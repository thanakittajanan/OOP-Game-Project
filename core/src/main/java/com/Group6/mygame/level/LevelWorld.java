package com.Group6.mygame.level;

import com.Group6.mygame.Audio;
import com.Group6.mygame.GasProgress;
import com.Group6.mygame.Player;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
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

    /** How big the level 2 scenery is drawn. Nothing here is collidable, so these are eyeballed. */
    private static final float HANGVINE_HEIGHT = 200f;
    private static final float GLOWPLANT_HEIGHT = 110f;
    private static final float PIPE_HEIGHT = 150f;

    /**
     * How long a pressure plate stays lit up after it slams down. A plate opens its gate the same frame
     * the cat's weight lands on it, and the gate is always somewhere off screen, so without a burst of
     * light at the plate itself the player has nothing to tell them it worked - the level just quietly
     * changes state ahead of them. That is not a puzzle, that is a coin flip.
     */
    private static final float PLATE_FLASH_TIME = 0.75f;

    private final int levelNumber;
    private final Player player;
    private final LevelArt art;
    private final LevelMap map;

    private final Array<Rectangle> activeSolids = new Array<>();
    private final Array<Rectangle> catSolids = new Array<>();    // what a solid cat would hit, even while the player is a cloud
    private final Rectangle gasCore = new Rectangle();           // the small middle of a cloud: what cold air and valves test against
    private boolean starTaken;
    private final boolean[] coreTaken;                       // greenhouse cores, one flag each
    private int coresLifted;
    private final float[] plateFlash;                        // seconds left of the "it went down" burst
    private final Array<Hazard> spawned = new Array<>();    // fireballs created during this frame
    private final boolean[] checkpointReached;
    private final boolean[] hintFired;                       // each teaching line shows once per attempt
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
        // Levels 1 and 2 draw their ground and traps from PNG sheets; 3 and 4 keep the coloured shapes.
        this.art = new LevelArt(LevelTheme.forLevel(levelNumber), levelNumber);
        this.map = new LevelMap(levelNumber, art, player.getBounds(), hazard -> spawned.add(hazard));
        checkpointReached = new boolean[map.getCheckpoints().size];
        hintFired = new boolean[map.getHints().size];
        coreTaken = new boolean[map.getCores().size];
        plateFlash = new float[map.getPlates().size];
        respawnX = map.getSpawn().x;
        respawnY = map.getSpawn().y;
        if (restoreCheckpoint && CheckpointSave.has(levelNumber)
            && CheckpointSave.reached().length == checkpointReached.length) {
            System.arraycopy(CheckpointSave.reached(), 0, checkpointReached, 0, checkpointReached.length);
            respawnX = CheckpointSave.x();
            respawnY = CheckpointSave.y();
            if (CheckpointSave.valveOpen()) map.openSealedDoors();
            // A latched plate is part of the level's state, not of the attempt: without this, dying
            // past a gate would send the cat back to a flag on the far side of a door that had just
            // shut itself again, with no plate within reach.
            map.restoreGates(CheckpointSave.plateMask());
            int cores = CheckpointSave.coreMask();
            for (int i = 0; i < coreTaken.length; i++) {
                if ((cores & (1 << i)) != 0) {
                    coreTaken[i] = true;
                    coresLifted++;
                }
            }
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
        for (int i = 0; i < plateFlash.length; i++) {              // the plate's "it went down" burst
            if (plateFlash[i] > 0f) plateFlash[i] -= delta;
        }
        if (cat.y < -150f) {                     // fell out of the map (only possible while invulnerable over a pit)
            if (hurtCooldown > 0f) moveCatTo(respawnX, respawnY);
            else hurt(Hazard.Effect.RESPAWN, cat.x);
        }

        checkHints(cat);
        checkCheckpoints(cat);
        if (status == Status.PLAYING && cat.overlaps(map.getExit())) {
            status = Status.CLEARED;
            Audio.levelComplete();
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
        addGates(catSolids);
        catSolids.addAll(map.getLiquidPassables());

        activeSolids.clear();
        if (isGas()) {
            activeSolids.addAll(map.getBoundarySolids());
            activeSolids.addAll(map.getSealedDoors());
            // A shut gate stops a cloud too. Gas is earned late and by another level, but the cat can
            // come back here with it, and a plate puzzle you can float over is not a puzzle.
            addGates(activeSolids);
            return;
        }
        activeSolids.addAll(map.getTerrainSolids());
        for (int i = 0; i < map.getCrumbles().size; i++) {
            CrumbleBlock c = map.getCrumbles().get(i);
            if (c.isSolid()) activeSolids.add(c.getBounds());
        }
        activeSolids.addAll(map.getSealedDoors());
        addGates(activeSolids);
        if (!isLiquid()) activeSolids.addAll(map.getLiquidPassables());
    }

    /** Every gate still shut, of both pairs. An opened pair has already thrown its boxes away. */
    private void addGates(Array<Rectangle> into) {
        for (int pair = 0; pair < LevelMap.PLATE_PAIRS; pair++) into.addAll(map.getGates(pair));
    }

    /**
     * Hold W while touching a vine to climb it. At the top the cat gets a small hop so it can step onto
     * the ledge (hold A or D toward the vine).
     *
     * <p>Design note: climbing is handled here rather than as a {@code ClimbingState} because it needs the
     * level's vine list. Moving it into the State pattern is the natural next refactor.
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

    /** The star, the valve (gas only), the pressure plates and the greenhouse cores. */
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
                    persist();
                    show("Valve opened! The sealed door is gone");
                }
            }
        }

        updatePlates(cat);
        updateCores(cat);
    }

    /**
     * Pressure plates. A plate answers to weight and to standing, and to nothing else, so the two ways
     * to get it wrong are both legible: jumping across it clears it, and flowing over it as a puddle
     * does not press it - which is the one the level wants the player to notice, so it says so.
     *
     * <p>Once a pair is latched it stays latched for the rest of the level. The gate is not a door the
     * cat has to keep open, it is a lock that has been opened.
     */
    private void updatePlates(Rectangle cat) {
        Array<LevelMap.Plate> plates = map.getPlates();
        for (int i = 0; i < plates.size; i++) {
            LevelMap.Plate plate = plates.get(i);
            if (map.isGateOpen(plate.group)) continue;               // already latched down
            if (!cat.overlaps(plate.bounds) || !player.isOnGround()) continue;

            if (!player.getForm().isHeavy()) {
                // Said every frame it is stood on, not once: the cat is doing the wrong thing right
                // now, and the line should stay up for as long as that is true.
                show("Too light - only the solid cat can press this");
                continue;
            }
            map.openGates(plate.group);
            plateFlash[i] = PLATE_FLASH_TIME;
            Audio.gate();
            persist();
            show("The plate slams down - a bulkhead grinds open somewhere ahead");
        }
    }

    /**
     * Greenhouse cores. Taken by any form, exactly like the star, and they unlock nothing: the gas
     * form is still earned only by the star at the end of The Furnace. They exist to be found.
     */
    private void updateCores(Rectangle cat) {
        Array<Rectangle> cores = map.getCores();
        for (int i = 0; i < cores.size; i++) {
            if (coreTaken[i] || !cat.overlaps(cores.get(i))) continue;
            coreTaken[i] = true;
            coresLifted++;
            Audio.core();
            persist();
            show("Greenhouse core " + coresLifted + " / " + cores.size);
        }
    }

    /**
     * Writes the level's progress somewhere a game over cannot reach it: the flags reached, the
     * respawn point, and the two things that change on their own schedule - which plates are latched
     * down and which cores have been lifted. Called whenever any of those change, not only when a flag
     * is touched, because every gate in the level is one death away from stranding the cat.
     */
    private void persist() {
        CheckpointSave.store(levelNumber, respawnX, respawnY, checkpointReached, map.isValveOpen());
        CheckpointSave.setProgress(map.getGateMask(), coreMask());
    }

    private int coreMask() {
        int mask = 0;
        for (int i = 0; i < coreTaken.length; i++) {
            if (coreTaken[i]) mask |= 1 << i;
        }
        return mask;
    }

    /**
     * Shows each teaching line once, as soon as the cat's nose gets past its column. A hint that comes
     * due while something else is already on the bar - "Checkpoint", say - waits its turn instead of
     * cutting in; the column test stays true, so it still arrives a moment later.
     */
    private void checkHints(Rectangle cat) {
        if (messageTime > 0f) return;
        Array<LevelMap.Hint> hints = map.getHints();
        for (int i = 0; i < hints.size; i++) {
            if (!hintFired[i] && cat.x + cat.width > hints.get(i).x) {
                hintFired[i] = true;
                show(hints.get(i).text);
                return;
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
                persist();
                Audio.checkpoint();
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
            Audio.gameOver();
            show("Game over");
            return;
        }
        Audio.hurt();
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

    /**
     * Draws checkpoints, the exit, the level's machinery, traps and monsters, in that order - scenery
     * first, then anything the cat has to read, then anything that can hurt it. Call between
     * {@code batch.begin()} and {@code batch.end()}.
     */
    public void renderBehindCat(SpriteBatch batch) {
        for (int i = 0; i < map.getCheckpoints().size; i++) drawCheckpoint(batch, map.getCheckpoints().get(i), checkpointReached[i]);
        drawExit(batch, map.getExit());
        drawDecor(batch);
        drawGates(batch);
        drawPlates(batch);
        drawCores(batch);
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
        if (art.hasSewerArt() || art.hasGreenhouseArt()) {
            // The post is as tall as its 1x2 tile box and centred on it, so its base stands on the floor.
            float h = cp.height;
            float w = h * art.checkpointAspect();
            float x = cp.x + cp.width / 2f - w / 2f;
            if (reached) {
                art.draw(batch, art.glow, art.theme.accent.r, art.theme.accent.g, art.theme.accent.b,
                    0.6f, x - w * 0.5f, cp.y - 10f, w * 2f, h * 1.15f);
            }
            if (art.hasSewerArt()) art.sewer.drawCheckpoint(batch, x, cp.y, w, h, !reached);
            else art.greenhouse.drawCheckpoint(batch, x, cp.y, w, h, !reached);
            return;
        }

        float poleX = cp.x + cp.width / 2f - 3f;
        art.draw(batch, art.white, 1f, 1f, 1f, 0.6f, poleX, cp.y, 6f, 110f);
        if (reached) {
            art.draw(batch, art.glow, art.theme.accent.r, art.theme.accent.g, art.theme.accent.b, 0.5f, poleX - 40f, cp.y + 60f, 90f, 80f);
            art.draw(batch, art.white, art.theme.accent, poleX + 6f, cp.y + 80f, 36f, 26f);
        } else {
            art.draw(batch, art.white, 1f, 1f, 1f, 0.35f, poleX + 6f, cp.y + 80f, 36f, 26f);
        }
    }

    // ================================================================== the greenhouse machinery

    /**
     * Painted scenery: hanging vines, glowing crystal, pipe fittings. None of it is in a collision
     * list, which is the whole point - a decorative vine that could hurt the cat would be a trap
     * wearing a costume.
     */
    private void drawDecor(SpriteBatch batch) {
        if (!art.hasGreenhouseArt()) return;
        Array<LevelMap.Decor> decor = map.getDecor();
        for (int i = 0; i < decor.size; i++) {
            LevelMap.Decor d = decor.get(i);
            if (d.kind == LevelMap.DECOR_GLOWPLANT) {
                // A soft bloom under the crystal, so the core of the greenhouse reads as lit.
                art.draw(batch, art.glow, 0.30f, 0.95f, 0.70f, 0.30f,
                    d.centerX - GLOWPLANT_HEIGHT * 0.8f, d.anchorY - GLOWPLANT_HEIGHT * 0.35f,
                    GLOWPLANT_HEIGHT * 1.6f, GLOWPLANT_HEIGHT * 1.5f);
                art.greenhouse.drawGlowPlant(batch, d.centerX, d.anchorY, GLOWPLANT_HEIGHT);
            } else if (d.kind == LevelMap.DECOR_HANGVINE) {
                art.greenhouse.drawHangingVine(batch, d.centerX, d.anchorY, HANGVINE_HEIGHT);
            } else {
                art.greenhouse.drawPipe(batch, d.variant, d.centerX, d.anchorY, PIPE_HEIGHT);
            }
        }
    }

    /**
     * The gates, one drawing per column, in the state the pair is in. A shut gate shows its bars
     * filling the opening; an open one shows the same frame with the bars drawn up out of the way.
     *
     * <p>The two states are the same frame in the same colours, though, and at a glance they are easy
     * to confuse - so each one also gets a lamp that cannot be misread: warm amber while the door is
     * locked, cool mint once it is open, plus a glow in the opening itself. A plate normally opens its
     * gate within a couple of screen-tiles of itself, so the lamp going from amber to mint is the
     * thing that actually connects "I stood on that" to "that door opened".
     */
    private void drawGates(SpriteBatch batch) {
        Array<LevelMap.GateColumn> columns = map.getGateColumns();
        for (int i = 0; i < columns.size; i++) {
            LevelMap.GateColumn g = columns.get(i);
            boolean open = map.isGateOpen(g.group);
            float lampY = g.footY + g.height - 26f;

            if (art.hasGreenhouseArt()) {
                art.greenhouse.drawGate(batch, g.centerX, g.footY, g.height, open);
            } else {
                // Shape stand-in, for when the sheets are missing.
                float x = g.centerX - 32f;
                Color c = open ? art.theme.accent : art.theme.bars;
                art.draw(batch, art.white, c.r, c.g, c.b, open ? 0.28f : 1f, x, g.footY, 64f, g.height);
                if (!open) {
                    for (int k = 0; k < 3; k++) {
                        art.draw(batch, art.white, c.r * 0.55f, c.g * 0.55f, c.b * 0.55f, 1f,
                            x + 8f + k * 18f, g.footY, 8f, g.height);
                    }
                }
            }

            if (open) {
                // Cool and steady: the door is open, nothing is being asked of the player.
                art.draw(batch, art.glow, 0.45f, 1f, 0.75f, 0.42f,
                    g.centerX - 70f, g.footY - 10f, 140f, g.height + 20f);
                art.draw(batch, art.circle, 0.55f, 1f, 0.80f, 1f, g.centerX - 7f, lampY, 14f, 14f);
            } else {
                // Warm and pulsing: something is shut and wants attention.
                float pulse = 0.55f + 0.45f * (float) Math.sin(time * 4.5f);
                art.draw(batch, art.glow, 1f, 0.55f, 0.35f, 0.30f + 0.25f * pulse,
                    g.centerX - 34f, lampY - 24f, 68f, 68f);
                art.draw(batch, art.circle, 1f, 0.52f, 0.32f, 1f, g.centerX - 7f, lampY, 14f, 14f);
            }
        }
    }

    /**
     * The plates. Three things are being said here, and each needs its own channel.
     *
     * <ul>
     *   <li><b>"Stand here."</b> The pad itself, its mint lip, and a slow breathing glow, so an unlit
     *       plate on an otherwise flat floor is findable before the cat knows what a plate is.</li>
     *   <li><b>"It went down - now."</b> A burst: a flash and a shockwave running out along the floor,
     *       for {@link #PLATE_FLASH_TIME}. This is the one that matters, and it is loud on purpose. The
     *       plate opens its gate on the very frame the weight lands, and the gate is off screen, so
     *       without this the level changes state silently and the player cannot tell whether they
     *       did anything at all.</li>
     *   <li><b>"This one is already done."</b> The pad drawn sunk and flush instead of proud, with a
     *       steady bright rim, so coming back to a plate you have already pressed reads as pressed.</li>
     * </ul>
     *
     * <p>The colour is a near-white mint, deliberately stronger than it looks like it needs to be:
     * level 2 is played against teal mist, and the first version - a teal glow at low alpha, which is
     * the colour a greenhouse plate obviously "should" be - was invisible against it.
     */
    private void drawPlates(SpriteBatch batch) {
        Array<LevelMap.Plate> plates = map.getPlates();
        for (int i = 0; i < plates.size; i++) {
            LevelMap.Plate p = plates.get(i);
            Rectangle b = p.bounds;
            float cx = b.x + b.width / 2f;
            boolean pressed = map.isGateOpen(p.group);

            // The waiting glow breathes, so the eye is drawn to it without it looking "already on".
            float breathe = 0.5f + 0.5f * (float) Math.sin(time * 2.2f);
            float r = pressed ? 0.70f : 0.62f;
            float g = 1f;
            float bl = pressed ? 0.78f : 0.88f;
            float idle = pressed ? 0.62f : 0.26f + 0.22f * breathe;
            art.draw(batch, art.glow, r, g, bl, idle, b.x - 52f, b.y - 74f, b.width + 104f, 168f);

            if (art.hasGreenhouseArt()) {
                art.greenhouse.drawPlate(batch, b.x, b.y, b.width, pressed);
            } else {
                // Shape stand-in: a proud bar gives way to a flush one.
                float h = pressed ? 7f : 12f;
                art.draw(batch, art.white, r, g, bl, pressed ? 0.95f : 0.55f, b.x, b.y, b.width, h);
            }
            art.draw(batch, art.white, r, g, bl, pressed ? 0.95f : 0.6f + 0.3f * breathe,
                b.x + 6f, b.y + (pressed ? 0f : 3f), b.width - 12f, 4f);

            float flash = plateFlash[i];
            if (flash > 0f) {
                float age = 1f - flash / PLATE_FLASH_TIME;      // 0 at the moment it goes down
                float fade = flash / PLATE_FLASH_TIME;          // 1 -> 0
                // A flash over the plate...
                art.draw(batch, art.glow, 1f, 1f, 1f, 0.9f * fade,
                    cx - 130f - 190f * age, b.y - 110f - 70f * age,
                    260f + 380f * age, 260f + 140f * age);
                // ...and a shockwave running out along the floor, which is the part that reads as
                // "something mechanical just moved" rather than "a light came on".
                float half = 26f + 300f * age;
                art.draw(batch, art.white, 1f, 1f, 1f, 0.95f * fade,
                    cx - half, b.y + 1f, half * 2f, 5f);
                art.draw(batch, art.white, 1f, 1f, 1f, 0.55f * fade,
                    cx - half * 0.7f, b.y + 8f, half * 1.4f, 3f);
            }
        }
    }

    /**
     * The greenhouse cores. Deliberately the same five-pointed shape, the same bob and the same halo
     * as the gas star, so it reads as a collectible at a glance - but a cold emerald rather than the
     * star's gold. This one unlocks nothing, and it must not look like it does.
     */
    private void drawCores(SpriteBatch batch) {
        for (int i = 0; i < map.getCores().size; i++) {
            if (coreTaken[i]) continue;
            Rectangle c = map.getCores().get(i);
            float bob = (float) Math.sin(time * 3f + i) * 5f;
            art.draw(batch, art.glow, 0.35f, 1f, 0.75f, 0.45f,
                c.x - 30f, c.y - 30f + bob, c.width + 60f, c.height + 60f);
            art.draw(batch, art.star, 0.09f, 0.58f, 0.42f, 1f, c.x, c.y + bob, c.width, c.height);
            art.draw(batch, art.star, 0.55f, 1f, 0.82f, 1f, c.x + 11f, c.y + 11f + bob, c.width - 22f, c.height - 22f);
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
        if (art.hasSewerArt() || art.hasGreenhouseArt()) {
            // The arch is wider and taller than the trigger box, so centre it rather than matching its
            // corners; the top step is 3 tiles wide so the feet still land on stone.
            float h = e.height * 1.25f;
            float w = h * art.exitAspect();
            float x = e.x + e.width / 2f - w / 2f;
            art.draw(batch, art.glow, art.theme.accent.r, art.theme.accent.g, art.theme.accent.b,
                0.3f, x - w * 0.12f, e.y - 18f, w * 1.24f, h * 1.14f);
            if (art.hasSewerArt()) art.sewer.drawExit(batch, x, e.y, w, h);
            else art.greenhouse.drawExit(batch, x, e.y, w, h);
            return;
        }

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
