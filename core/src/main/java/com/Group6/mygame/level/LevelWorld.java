package com.Group6.mygame.level;

import com.Group6.mygame.GasProgress;
import com.Group6.mygame.Player;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

/**
 * Runs one level each frame: the cat, traps, monsters, checkpoints and the exit.
 * {@code LevelScreen} owns the camera and drawing order; this class owns the rules.
 *
 * <p>Order of one frame ({@link #update(float)}):
 * <ol>
 *   <li>Decide what the cat collides with (liquid cat ignores bars and grates, broken floor is gone,
 *       a gas cloud ignores everything except sealed doors and the map edge).</li>
 *   <li>Vine climbing (temporary, see {@link #applyVineClimb()}) and the wind a cloud is standing in.</li>
 *   <li>{@code player.update(delta)}: Member 1's physics and states move the cat.</li>
 *   <li>Traps and monsters move, then we check if anything touches the cat. A cloud is warmed by
 *       fire, condensed by cold air and ignored by everything else.</li>
 *   <li>The star, the valve, checkpoints and the exit.</li>
 * </ol>
 */
public class LevelWorld implements Disposable {

    public enum Status { PLAYING, CLEARED, DEAD }

    /** Seconds the cat cannot be hurt again after a hit (Member 3 may replace this with the real i-frames). */
    public static final float HURT_COOLDOWN = 1.2f;
    public static final float KNOCKBACK_X = 350f;
    public static final float KNOCKBACK_Y = 320f;
    public static final float CLIMB_SPEED = 230f;
    public static final float CLIMB_TOP_BOOST = 380f;

    private final int levelNumber;
    private final Player player;
    private final LevelArt art;
    private final LevelMap map;

    private final Array<Rectangle> activeSolids = new Array<>();
    private final Array<Rectangle> catSolids = new Array<>();    // what a solid cat would hit, even while the player is a cloud
    private final Rectangle gasCore = new Rectangle();           // the small middle of a cloud: what cold air and valves test against
    private boolean starTaken;
    private final Array<Hazard> spawned = new Array<>();    // fireballs created during this frame
    private final boolean[] checkpointReached;
    private float respawnX, respawnY;                        // bottom-centre of the respawn point
    private float hurtCooldown;
    private float time;
    private Status status = Status.PLAYING;
    private String message = "";
    private float messageTime;

    public LevelWorld(int levelNumber, Player player) {
        this(levelNumber, player, false);
    }

    /** @param restoreCheckpoint true after a game over: start from the last checkpoint reached */
    public LevelWorld(int levelNumber, Player player, boolean restoreCheckpoint) {
        this.levelNumber = levelNumber;
        this.player = player;
        this.art = new LevelArt(LevelTheme.forLevel(levelNumber));
        this.map = new LevelMap(levelNumber, art, player.getBounds(), new Monster.HazardSink() {
            @Override
            public void add(Hazard hazard) {
                spawned.add(hazard);
            }
        });
        checkpointReached = new boolean[map.getCheckpoints().size];
        respawnX = map.getSpawn().x;
        respawnY = map.getSpawn().y;
        if (restoreCheckpoint && CheckpointSave.has(levelNumber)
            && CheckpointSave.reached().length == checkpointReached.length) {
            System.arraycopy(CheckpointSave.reached(), 0, checkpointReached, 0, checkpointReached.length);
            respawnX = CheckpointSave.x();
            respawnY = CheckpointSave.y();
            if (CheckpointSave.valveOpen()) map.openSealedDoors();
        } else {
            CheckpointSave.clear();
        }
        moveCatTo(respawnX, respawnY);
        player.setSolids(activeSolids);   // the same list object is refilled every frame
        player.setFullSolids(catSolids);
        starTaken = GasProgress.isUnlocked();   // already earned: the star is not drawn again
        rebuildSolids();
    }

    // ================================================================== update

    public void update(float delta) {
        delta = Math.min(delta, 1 / 30f);
        time += delta;
        if (messageTime > 0f) messageTime -= delta;
        if (status != Status.PLAYING) return;

        rebuildSolids();
        applyVineClimb();
        applyWind();
        player.update(delta);

        Rectangle cat = player.getBounds();
        String hint = player.pollHint();
        if (hint != null) show(hint);
        if (player.pollCondensedInWall()) sendBack("Condensed inside the wall!");
        for (int i = 0; i < map.getCrumbles().size; i++) {
            CrumbleBlock c = map.getCrumbles().get(i);
            c.setCatOnGround(player.isOnGround());
            c.update(delta);
        }

        updateHazards(delta, cat);
        updateMonsters(delta, cat);
        updatePickups(cat);
        if (hurtCooldown > 0f) hurtCooldown -= delta;
        if (cat.y < -150f) {                     // fell out of the map (only possible while invulnerable over a pit)
            if (hurtCooldown > 0f) moveCatTo(respawnX, respawnY);
            else hurt(Hazard.Effect.RESPAWN, cat.x);
        }

        checkCheckpoints(cat);
        if (status == Status.PLAYING && cat.overlaps(map.getExit())) {
            status = Status.CLEARED;
            show(levelNumber < LevelData.LEVEL_COUNT ? "Level cleared!" : "You escaped!");
        }
    }

    /**
     * Bars and grates only stop the solid cat; the liquid form flows through them.
     *
     * <p>The second half is a safety net, not a gameplay rule: a cat that is <em>already</em>
     * overlapping a bar keeps counting as liquid for as long as it is in there. Without it, a cat that
     * released S inside a grate would set back into a 90 px cat embedded in a wall.
     */
    private boolean isLiquid() {
        return player.getForm().passesBars()
            || overlapsAny(player.getBounds(), map.getLiquidPassables());
    }

    private boolean isGas() {
        return player.getForm().canFly();
    }

    /**
     * Fills the two collision lists for this frame.
     *
     * <p>{@code catSolids} is what a solid cat would collide with; the player uses it only to check a
     * cloud has room to condense. {@code activeSolids} is what the cat collides with <em>now</em>: for
     * a cloud that is only the sealed doors and the map edge, because everything else is drifted through.
     */
    private void rebuildSolids() {
        catSolids.clear();
        catSolids.addAll(map.getTerrainSolids());
        for (int i = 0; i < map.getCrumbles().size; i++) {
            CrumbleBlock c = map.getCrumbles().get(i);
            if (c.isSolid()) catSolids.add(c.getBounds());
        }
        catSolids.addAll(map.getSealedDoors());
        catSolids.addAll(map.getLiquidPassables());

        activeSolids.clear();
        if (isGas()) {
            activeSolids.addAll(map.getBoundarySolids());
            activeSolids.addAll(map.getSealedDoors());
            return;
        }
        activeSolids.addAll(map.getTerrainSolids());
        for (int i = 0; i < map.getCrumbles().size; i++) {
            CrumbleBlock c = map.getCrumbles().get(i);
            if (c.isSolid()) activeSolids.add(c.getBounds());
        }
        activeSolids.addAll(map.getSealedDoors());
        if (!isLiquid()) activeSolids.addAll(map.getLiquidPassables());
    }

    /**
     * Hold W while touching a vine to climb it. At the top the cat gets a small hop so it can step onto
     * the ledge (hold A or D toward the vine).
     *
     * <p>TODO(Member 1): this is a stand-in. A proper ClimbingState in the State Pattern would be cleaner.
     */
    private void applyVineClimb() {
        if (!Gdx.input.isKeyPressed(Input.Keys.W) || isLiquid() || isGas()) return;   // W flies a cloud up
        Rectangle cat = player.getBounds();
        for (int i = 0; i < map.getVines().size; i++) {
            Rectangle v = map.getVines().get(i);
            boolean touching = cat.x < v.x + v.width + 4f && cat.x + cat.width > v.x - 4f
                && cat.y < v.y + v.height && cat.y + cat.height > v.y;
            if (!touching) continue;
            float vineTop = topOfVineColumn(v);
            if (cat.y >= vineTop - 2f) continue;                  // already above the vine
            float vy = cat.y > vineTop - 40f ? CLIMB_TOP_BOOST : CLIMB_SPEED;
            if (player.getVelocity().y < vy) player.getVelocity().y = vy;
            return;
        }
    }

    /** Vines are stored per tile; find the top of the column this tile belongs to. */
    private float topOfVineColumn(Rectangle tile) {
        float top = tile.y + tile.height;
        boolean grew = true;
        while (grew) {
            grew = false;
            for (int i = 0; i < map.getVines().size; i++) {
                Rectangle v = map.getVines().get(i);
                if (v.x == tile.x && v.y == top) {
                    top += v.height;
                    grew = true;
                }
            }
        }
        return top;
    }

    private void updateHazards(float delta, Rectangle cat) {
        Array<Hazard> hazards = map.getHazards();
        boolean gas = isGas();
        if (gas) updateGasCore();
        for (int i = 0; i < hazards.size; i++) {
            Hazard h = hazards.get(i);
            h.update(delta);
            if (status != Status.PLAYING) continue;

            if (h.heatRate() > 0f && cat.overlaps(h.getBounds())) {
                player.getHeat().add(h.heatRate() * delta);        // fire feeds the heat bar
            }
            if (gas) {
                if (h.freezesGas() && gasCore.overlaps(h.getBounds())) freezeCloud();
                continue;                                          // nothing else can hurt a cloud
            }
            if (h.touches(cat)) {
                h.onHitCat();
                hurt(h.getEffect(), h.getCenterX());
            }
        }
        // add new fireballs, drop finished ones (backwards loop so removing is safe)
        hazards.addAll(spawned);
        spawned.clear();
        for (int i = hazards.size - 1; i >= 0; i--) {
            if (hazards.get(i).isFinished()) hazards.removeIndex(i);
        }
    }

    private void updateMonsters(float delta, Rectangle cat) {
        for (int i = 0; i < map.getMonsters().size; i++) {
            Monster m = map.getMonsters().get(i);
            m.update(delta);
            if (status == Status.PLAYING && !isGas() && cat.overlaps(m.getHitbox())) {
                hurt(Hazard.Effect.KNOCKBACK, m.getCenterX());
            }
        }
    }

    private void updateGasCore() {
        Rectangle b = player.getBounds();
        gasCore.set(b.x + b.width / 2f - 18f, b.y + b.height / 2f - 18f, 36f, 36f);
    }

    /** Fans: sum the wind of every fan the middle of the cloud is inside. Zero for anything but a cloud. */
    private void applyWind() {
        float wx = 0f, wy = 0f;
        if (isGas()) {
            updateGasCore();
            Array<Hazard> hazards = map.getHazards();
            for (int i = 0; i < hazards.size; i++) {
                Hazard h = hazards.get(i);
                if ((h.windX() != 0f || h.windY() != 0f) && gasCore.overlaps(h.getBounds())) {
                    wx += h.windX();
                    wy += h.windY();
                }
            }
        }
        player.setWind(wx, wy);
    }

    /** Cold air: the heat is gone at once; Player then condenses the cloud on its next update. */
    private void freezeCloud() {
        if (player.getHeat().isEmpty()) return;
        player.getHeat().empty();
        show("Frozen! Cold air condensed the gas");
    }

    /** A cloud that set inside rock: back to the checkpoint, with the heat gone. */
    private void sendBack(String why) {
        show(why);
        player.getHeat().empty();
        if (hurtCooldown > 0f) moveCatTo(respawnX, respawnY);
        else hurt(Hazard.Effect.RESPAWN, player.getBounds().x);
    }

    /** The star (any form can take it) and the valve (only a warm enough cloud can turn it). */
    private void updatePickups(Rectangle cat) {
        Rectangle star = map.getStar();
        if (!starTaken && star != null && cat.overlaps(star)) {
            starTaken = true;
            GasProgress.unlock();
            show("Star collected! Gas form unlocked - press E");
        }

        Rectangle valve = map.getValve();
        if (valve != null && !map.isValveOpen() && isGas()) {
            updateGasCore();
            if (gasCore.overlaps(valve)) {
                if (player.getHeat().isThin()) {
                    show("The gas is too thin to turn the valve - warm up first");
                } else {
                    map.openSealedDoors();
                    if (CheckpointSave.has(levelNumber)) {
                        CheckpointSave.store(levelNumber, respawnX, respawnY, checkpointReached, true);
                    }
                    show("Valve opened! The sealed door is gone");
                }
            }
        }
    }

    private void checkCheckpoints(Rectangle cat) {
        Array<Rectangle> cps = map.getCheckpoints();
        for (int i = 0; i < cps.size; i++) {
            if (!checkpointReached[i] && cat.overlaps(cps.get(i))) {
                checkpointReached[i] = true;
                respawnX = cps.get(i).x + cps.get(i).width / 2f;
                respawnY = cps.get(i).y;
                CheckpointSave.store(levelNumber, respawnX, respawnY, checkpointReached, map.isValveOpen());
                show("Checkpoint");
            }
        }
    }

    // ================================================================== damage

    /** The cat loses 1 HP, then is either sent back to the checkpoint or knocked away. */
    private void hurt(Hazard.Effect effect, float sourceX) {
        if (hurtCooldown > 0f) return;
        hurtCooldown = HURT_COOLDOWN;
        player.takeDamage(1);                    // Member 3's health system
        if (player.getHealth() <= 0) {
            status = Status.DEAD;
            show("Game over");
            return;
        }
        if (effect == Hazard.Effect.RESPAWN) {
            moveCatTo(respawnX, respawnY);
        } else {
            float catCenter = player.getBounds().x + player.getBounds().width / 2f;
            float away = catCenter < sourceX ? -1f : 1f;
            player.getVelocity().set(away * KNOCKBACK_X, KNOCKBACK_Y);
        }
    }

    private void moveCatTo(float centerX, float feetY) {
        Rectangle b = player.getBounds();
        float x = centerX - b.width / 2f;
        player.getPosition().set(x, feetY + 1f);
        b.setPosition(x, feetY + 1f);
        player.getVelocity().set(0f, 0f);
    }

    /** Shows a short message on screen (used by LevelScreen for the level-start notice). */
    public void announce(String text) {
        show(text);
    }

    private void show(String text) {
        message = text;
        messageTime = 1.5f;
    }

    private static boolean overlapsAny(Rectangle r, Array<Rectangle> list) {
        for (int i = 0; i < list.size; i++) if (r.overlaps(list.get(i))) return true;
        return false;
    }

    // ================================================================== drawing

    /** Draws the tiles between world x {@code left} and {@code right}. Call between batch.begin() and batch.end(). */
    public void renderTiles(SpriteBatch batch, float left, float right) {
        map.render(batch, left, right);
    }

    /** Draws checkpoints, the exit, traps and monsters. Call between batch.begin() and batch.end(). */
    public void renderBehindCat(SpriteBatch batch) {
        for (int i = 0; i < map.getCheckpoints().size; i++) drawCheckpoint(batch, map.getCheckpoints().get(i), checkpointReached[i]);
        drawExit(batch, map.getExit());
        drawStar(batch);
        drawValve(batch);
        for (int i = 0; i < map.getCrumbles().size; i++) map.getCrumbles().get(i).render(batch);
        for (int i = 0; i < map.getHazards().size; i++) map.getHazards().get(i).render(batch);
        for (int i = 0; i < map.getMonsters().size; i++) map.getMonsters().get(i).render(batch);
    }

    /** The cat blinks while it cannot be hurt. */
    public void renderCat(SpriteBatch batch) {
        boolean blinkOff = hurtCooldown > 0f && ((int) (time * 14f)) % 2 == 0;
        if (!blinkOff) player.render(batch);
    }

    private void drawCheckpoint(SpriteBatch batch, Rectangle cp, boolean reached) {
        float poleX = cp.x + cp.width / 2f - 3f;
        art.draw(batch, art.white, 1f, 1f, 1f, 0.6f, poleX, cp.y, 6f, 110f);
        if (reached) {
            art.draw(batch, art.glow, art.theme.accent.r, art.theme.accent.g, art.theme.accent.b, 0.5f, poleX - 40f, cp.y + 60f, 90f, 80f);
            art.draw(batch, art.white, art.theme.accent, poleX + 6f, cp.y + 80f, 36f, 26f);
        } else {
            art.draw(batch, art.white, 1f, 1f, 1f, 0.35f, poleX + 6f, cp.y + 80f, 36f, 26f);
        }
    }

    private void drawStar(SpriteBatch batch) {
        Rectangle st = map.getStar();
        if (starTaken || st == null) return;
        float bob = (float) Math.sin(time * 3f) * 6f;
        art.draw(batch, art.glow, 1f, 0.85f, 0.3f, 0.55f, st.x - 40f, st.y - 40f + bob, st.width + 80f, st.height + 80f);
        art.draw(batch, art.star, 1f, 0.86f, 0.25f, 1f, st.x, st.y + bob, st.width, st.height);
        art.draw(batch, art.star, 1f, 0.97f, 0.7f, 1f, st.x + 12f, st.y + 12f + bob, st.width - 24f, st.height - 24f);
    }

    private void drawValve(SpriteBatch batch) {
        Rectangle v = map.getValve();
        if (v == null) return;
        boolean open = map.isValveOpen();
        float r = open ? 0.4f : 1f, g = open ? 0.95f : 0.45f, b = open ? 0.55f : 0.3f;
        art.draw(batch, art.glow, r, g, b, 0.5f, v.x - 30f, v.y - 30f, v.width + 60f, v.height + 60f);
        art.draw(batch, art.circle, r, g, b, 1f, v.x + 8f, v.y + 8f, v.width - 16f, v.height - 16f);
        art.draw(batch, art.circle, 0.12f, 0.1f, 0.2f, 1f, v.x + 20f, v.y + 20f, v.width - 40f, v.height - 40f);
        art.draw(batch, art.white, 1f, 1f, 1f, 1f, v.x + v.width / 2f - 3f, v.y + 10f, 6f, v.height - 20f);
        art.draw(batch, art.white, 1f, 1f, 1f, 1f, v.x + 10f, v.y + v.height / 2f - 3f, v.width - 20f, 6f);
    }

    private void drawExit(SpriteBatch batch, Rectangle e) {
        art.draw(batch, art.glow, 1f, 1f, 1f, 0.35f, e.x - 70f, e.y - 30f, e.width + 140f, e.height + 60f);
        art.draw(batch, art.white, 1f, 1f, 1f, 0.15f, e.x, e.y, e.width, e.height);
        float t = 4f;
        art.draw(batch, art.white, 1f, 1f, 1f, 1f, e.x, e.y, t, e.height);
        art.draw(batch, art.white, 1f, 1f, 1f, 1f, e.x + e.width - t, e.y, t, e.height);
        art.draw(batch, art.white, 1f, 1f, 1f, 1f, e.x, e.y + e.height - t, e.width, t);
        art.draw(batch, art.circle, 1f, 1f, 1f, 1f, e.x + e.width - 16f, e.y + e.height / 2f - 4f, 8f, 8f);
    }

    // ================================================================== getters

    public LevelMap getMap() { return map; }
    public Status getStatus() { return status; }
    public int getLevelNumber() { return levelNumber; }
    /** Short on-screen message ("Checkpoint", "Game over"...), or "" when none is showing. */
    public String getMessage() { return messageTime > 0f ? message : ""; }

    @Override
    public void dispose() {
        art.dispose();
    }
}
