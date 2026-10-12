package com.Group6.mygame.level;

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
 *   <li>Decide what the cat collides with (liquid cat ignores bars and grates, broken floor is gone).</li>
 *   <li>Vine climbing (temporary, see {@link #applyVineClimb()}).</li>
 *   <li>{@code player.update(delta)}: Member 1's physics and states move the cat.</li>
 *   <li>Traps and monsters move, then we check if anything touches the cat.</li>
 *   <li>Checkpoints and the exit.</li>
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
    private final Array<Hazard> spawned = new Array<>();    // fireballs created during this frame
    private final boolean[] checkpointReached;
    private float respawnX, respawnY;                        // bottom-centre of the respawn point
    private float hurtCooldown;
    private float time;
    private Status status = Status.PLAYING;
    private String message = "";
    private float messageTime;

    public LevelWorld(int levelNumber, Player player) {
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
        moveCatTo(respawnX, respawnY);
        player.setSolids(activeSolids);   // the same list object is refilled every frame
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
        player.update(delta);

        Rectangle cat = player.getBounds();
        for (int i = 0; i < map.getCrumbles().size; i++) {
            CrumbleBlock c = map.getCrumbles().get(i);
            c.setCatOnGround(player.isOnGround());
            c.update(delta);
        }

        updateHazards(delta, cat);
        updateMonsters(delta, cat);
        if (hurtCooldown > 0f) hurtCooldown -= delta;
        if (cat.y < -150f) {                     // fell out of the map (only possible while invulnerable over a pit)
            if (hurtCooldown > 0f) moveCatTo(respawnX, respawnY);
            else hurt(Hazard.Effect.RESPAWN, cat.x);
        }

        checkCheckpoints(cat);
        if (status == Status.PLAYING && cat.overlaps(map.getExit())) {
            status = Status.CLEARED;
            show(levelNumber < 3 ? "Level cleared!" : "You escaped!");
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

    private void rebuildSolids() {
        activeSolids.clear();
        activeSolids.addAll(map.getTerrainSolids());
        for (int i = 0; i < map.getCrumbles().size; i++) {
            CrumbleBlock c = map.getCrumbles().get(i);
            if (c.isSolid()) activeSolids.add(c.getBounds());
        }
        if (!isLiquid()) activeSolids.addAll(map.getLiquidPassables());
    }

    /**
     * Hold W while touching a vine to climb it. At the top the cat gets a small hop so it can step onto
     * the ledge (hold A or D toward the vine).
     *
     * <p>TODO(Member 1): this is a stand-in. A proper ClimbingState in the State Pattern would be cleaner.
     */
    private void applyVineClimb() {
        if (!Gdx.input.isKeyPressed(Input.Keys.W) || isLiquid()) return;
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
        for (int i = 0; i < hazards.size; i++) {
            Hazard h = hazards.get(i);
            h.update(delta);
            if (status == Status.PLAYING && h.touches(cat)) {
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
            if (status == Status.PLAYING && cat.overlaps(m.getHitbox())) {
                hurt(Hazard.Effect.KNOCKBACK, m.getCenterX());
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
