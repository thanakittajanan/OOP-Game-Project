package com.Group6.mygame.level;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

/**
 * The real artwork for level 2, cut out of the sheets that ship in {@code assets/}. Levels 1 and 3-4
 * keep the coloured shapes from {@link LevelArt}; this class is only built when the level wants it.
 *
 * <p>Like {@link SewerArt}, every rectangle below was <em>measured</em> from the file with an alpha
 * scan, not guessed: these sheets hold sprites on a transparent field, and the numbers are their ink
 * bounds.
 *
 * <p>Sheets and what is taken from them:
 * <ul>
 *   <li>{@code Greenhouse Pipes.png} (1024x1024) - twenty-four separate slabs and pipe fittings laid
 *       out in six columns and four rows. The block rows are <em>not</em> a cross-section like level
 *       1's platform sheet: they are individual blocks with rounded corners and a cast shadow, so
 *       stacking them leaves the background showing through the joints. Two things fix that, and both
 *       were checked against a mock-up before this was written - each band is cropped inward to cut
 *       its own outline away, and {@link LevelMap} lays an opaque {@link #BACKING} rectangle under
 *       every solid tile first. What is left reads as riveted metal panelling rather than floating
 *       slabs. The fourth row is pipe fittings, and it is used as decoration.</li>
 *   <li>{@code Sci-Fi Moss Gate Asset Sheet.png} (1774x887) - three objects. The low mossy pad on the
 *       left is the pressure plate, the riveted frame with bars filling its opening is the shut gate,
 *       and the arch with the bars drawn up out of the way is the open one. Those two states are
 *       exactly what the plate has to communicate, so they are used as drawn.</li>
 *   <li>{@code Three Stylized Vines on Transparency.png} (1774x887) - three plants. The twisted thorn
 *       column becomes the vine wall ({@code 'H'}), the leafy strand becomes a hanging decoration, and
 *       the glowing crystal plant is what a spike tile looks like in this level.</li>
 *   <li>{@code industrial checkpoint.png} and {@code industrial sewer exit door.png} - the same two
 *       objects level 1 uses, at the same measured rectangles, so a checkpoint and an exit read the
 *       same in every level.</li>
 * </ul>
 *
 * <p>Every sheet is loaded defensively: a file that is missing or unreadable flips {@link #isReady()}
 * off and makes every draw call a no-op, so the level falls back to the coloured shapes it used
 * before. The art is an upgrade, not a dependency.
 */
public class GreenhouseArt implements Disposable {
    private static final String PIPES_PNG = "Greenhouse Pipes.png";
    private static final String GATE_PNG = "Sci-Fi Moss Gate Asset Sheet.png";
    private static final String VINES_PNG = "Three Stylized Vines on Transparency.png";
    private static final String CHECKPOINT_PNG = "industrial checkpoint.png";
    private static final String EXIT_PNG = "industrial sewer exit door.png";

    // ---------------------------------------------------------------- measured rectangles

    /**
     * The lit top surface of a column: the mossy slab with grass along its top edge.
     *
     * <p>Measured at (13, 95, 154x173), then cropped 10% off each side and 22% off the bottom - the
     * bottom fifth of every block in this sheet is its own cast shadow, and leaving it in paints a dark
     * band between one tile and the next.
     */
    private static final int SURFACE_X = 28, SURFACE_Y = 95, SURFACE_W = 124, SURFACE_H = 135;

    /** One tile down: a mossy block, measured (352, 513, 150x180) and cropped the same way. */
    private static final int MOSS_X = 367, MOSS_Y = 527, MOSS_W = 120, MOSS_H = 126;

    /**
     * Everything deeper: the plain riveted slab, measured (352, 119, 151x147).
     *
     * <p>This one was picked by drawing the level's own plateau with each candidate and looking at it,
     * and the runner-up is worth naming because it is the obvious choice and it looks wrong: the plain
     * block further down the sheet (522, 544) carries a bright moss tuft that lands in the middle of
     * every tile, and repeated nine tiles deep it reads as wallpaper. This slab's repeating feature is
     * a riveted seam, which reads as a stacked steel beam - which is what the wall is.
     */
    private static final int STONE_X = 367, STONE_Y = 131, STONE_W = 121, STONE_H = 103;

    private static final int[][] PIPES = {
        {187, 743, 145, 158},   // elbow
        {514, 738, 165, 192},   // four-way cross
        {704, 738, 117, 192},   // valve wheel
        {867, 738, 125, 192},   // round vent
    };

    /** The thorn column, measured 199x839, cut into tile-sized slices down its length. */
    private static final int THORN_X = 786, THORN_Y = 27, THORN_W = 199, THORN_H = 839;

    /** The leafy strand, hung from ceilings and platform edges. Never collides. */
    private static final int HANG_X = 123, HANG_Y = 26, HANG_W = 390, HANG_H = 816;

    /** The glowing crystal plant. Drawn wherever a spike tile is, so nothing else has to change. */
    private static final int CRYSTAL_X = 1261, CRYSTAL_Y = 127, CRYSTAL_W = 415, CRYSTAL_H = 733;

    /** The low mossy pad, measured (92, 626, 436x100) - 4.36:1, so it is stretched to be readable. */
    private static final int PLATE_X = 92, PLATE_Y = 626, PLATE_W = 436, PLATE_H = 100;
    /** How tall the pad is drawn while it is waiting, and how far its top stands above the floor. */
    private static final float PLATE_DRAW_H = 26f, PLATE_LIP = 4f;
    /** And once it is held down: shorter, and sunk so its top sits just under the floor line. */
    private static final float PLATE_DOWN_H = 15f, PLATE_DOWN_SINK = 3f;

    /** The shut gate (bars filling the opening) and the open one (bars drawn up out of the way). */
    private static final int SHUT_X = 635, SHUT_Y = 233, SHUT_W = 465, SHUT_H = 496;
    private static final int OPEN_X = 1232, OPEN_Y = 169, OPEN_W = 456, OPEN_H = 561;

    /** Checkpoint and exit: the same objects and rectangles {@link SewerArt} uses for level 1. */
    private static final int CHECKPOINT_X = 252, CHECKPOINT_Y = 44, CHECKPOINT_W = 530, CHECKPOINT_H = 905;
    private static final int EXIT_X = 73, EXIT_Y = 84, EXIT_W = 885, EXIT_H = 824;

    public static final float CHECKPOINT_ASPECT = CHECKPOINT_W / (float) CHECKPOINT_H;
    public static final float EXIT_ASPECT = EXIT_W / (float) EXIT_H;

    /** One slice per tile down the thorn column; a taller vine reuses the lowest slice. */
    private static final int VINE_SLICES = 13;

    /**
     * What sits behind every solid tile, under the panel art. Level 1's sheet was a seamless
     * cross-section; this one is loose blocks, so without a backing the gaps between them show the
     * background through what is supposed to be solid rock.
     */
    public static final Color BACKING = new Color(0.10f, 0.157f, 0.165f, 1f);

    /** The dim tint a checkpoint wears until it has been touched. */
    private static final Color CHECKPOINT_DIM = new Color(0.30f, 0.33f, 0.42f, 1f);

    /**
     * The glow under a pressure plate: cold teal while it is waiting, bright mint once the weight is
     * on it. The pad itself is small and dark, so this is what actually tells the player it is there.
     */
    public static final Color PLATE_LIT = new Color(0.42f, 0.95f, 0.62f, 1f);
    public static final Color PLATE_WAITING = new Color(0.16f, 0.52f, 0.42f, 1f);

    // ---------------------------------------------------------------- state

    private final Array<Texture> textures = new Array<>();
    private boolean ready = true;

    private final TextureRegion[] bands = new TextureRegion[3];
    private final TextureRegion[] pipes = new TextureRegion[PIPES.length];
    private final TextureRegion[] vineSlices = new TextureRegion[VINE_SLICES];
    private final TextureRegion hang, crystal, plate, shut, open, checkpoint, exitDoor;
    private final TextureRegion[] slimeFrames = new TextureRegion[SLIME_X.length];
    private final TextureRegion[] slimeFlipped = new TextureRegion[SLIME_X.length];

    public GreenhouseArt() {
        Texture pipeSheet = load(PIPES_PNG);
        Texture gateSheet = load(GATE_PNG);
        Texture vineSheet = load(VINES_PNG);
        Texture checkpointSheet = load(CHECKPOINT_PNG);
        Texture exitSheet = load(EXIT_PNG);
        Texture slimeSheet = load(SLIME_PNG);

        bands[0] = region(pipeSheet, SURFACE_X, SURFACE_Y, SURFACE_W, SURFACE_H);
        bands[1] = region(pipeSheet, MOSS_X, MOSS_Y, MOSS_W, MOSS_H);
        bands[2] = region(pipeSheet, STONE_X, STONE_Y, STONE_W, STONE_H);
        for (int i = 0; i < PIPES.length; i++) {
            int[] r = PIPES[i];
            pipes[i] = region(pipeSheet, r[0], r[1], r[2], r[3]);
        }

        plate = region(gateSheet, PLATE_X, PLATE_Y, PLATE_W, PLATE_H);
        shut = region(gateSheet, SHUT_X, SHUT_Y, SHUT_W, SHUT_H);
        open = region(gateSheet, OPEN_X, OPEN_Y, OPEN_W, OPEN_H);

        hang = region(vineSheet, HANG_X, HANG_Y, HANG_W, HANG_H);
        crystal = region(vineSheet, CRYSTAL_X, CRYSTAL_Y, CRYSTAL_W, CRYSTAL_H);
        int slice = (int) LevelData.TILE_SIZE;
        for (int i = 0; i < VINE_SLICES; i++) {
            // The column is 839 px tall, so the last slice would run off the bottom; back it up.
            int y = Math.min(THORN_Y + i * slice, THORN_Y + THORN_H - slice);
            vineSlices[i] = region(vineSheet, THORN_X, y, THORN_W, slice);
        }

        checkpoint = region(checkpointSheet, CHECKPOINT_X, CHECKPOINT_Y, CHECKPOINT_W, CHECKPOINT_H);
        exitDoor = region(exitSheet, EXIT_X, EXIT_Y, EXIT_W, EXIT_H);

        for (int i = 0; i < slimeFrames.length; i++) {
            slimeFrames[i] = region(slimeSheet, SLIME_X[i], SLIME_Y, SLIME_W[i], SLIME_H);
            if (slimeFrames[i] != null) {
                slimeFlipped[i] = new TextureRegion(slimeFrames[i]);
                slimeFlipped[i].flip(true, false);      // the art faces right
            }
        }
    }

    private TextureRegion region(Texture sheet, int x, int y, int w, int h) {
        if (sheet == null) return null;
        return new TextureRegion(sheet, x, y, w, h);
    }

    /**
     * Loads a sheet, once. Unlike {@link SewerArt}, a failure here is survivable by design: it flips
     * {@link #isReady()} off and every draw call becomes a no-op, so the level drops back to coloured
     * shapes instead of refusing to open. These art files are not tracked by git yet, and a level that
     * will not load because a picture is missing is worse than a level made of rectangles.
     */
    private Texture load(String png) {
        try {
            Texture t = new Texture(png);
            t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            t.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
            textures.add(t);
            return t;
        } catch (Exception e) {
            ready = false;
            return null;
        }
    }

    /** False when a sheet was missing: the level then draws itself with {@link LevelTheme} shapes. */
    public boolean isReady() {
        return ready;
    }

    // ---------------------------------------------------------------- terrain

    /**
     * One tile of terrain.
     *
     * @param depth how many tiles below the top of its column this tile sits; 0 is the lit surface.
     *              {@link LevelMap} derives it from {@code topSolidRow}, exactly as for level 1, so a
     *              column always shows moss at the top and bare steel further down.
     */
    public void drawPlatform(SpriteBatch batch, int depth, float x, float y, float size) {
        if (!ready) return;
        int d = depth <= 0 ? 0 : depth == 1 ? 1 : 2;
        batch.setColor(Color.WHITE);
        batch.draw(bands[d], x, y, size, size);
    }

    // ---------------------------------------------------------------- traps and props

    /**
     * A spike tile, drawn as the glowing crystal plant: one tile wide and about 113 tall,
     * bottom-aligned, so it grows up out of the trench it stands in.
     *
     * <p>The damage box is untouched - still {@code SpikeStrip}'s 52x38 at the foot of the tile - so
     * the picture is deliberately a little bigger than the hitbox, which forgives the player rather
     * than cheating them. Swapping the art changes nothing about how a spike behaves.
     */
    public void drawSpike(SpriteBatch batch, float x, float y, float size) {
        if (!ready) return;
        float h = size * (CRYSTAL_H / (float) CRYSTAL_W);
        batch.setColor(Color.WHITE);
        batch.draw(crystal, x, y, size, h);
    }

    /**
     * One tile of a vine wall - the thorn column, cut into tiles.
     *
     * <p>Slicing rather than scaling is the whole point: the column is 199 px wide and 839 tall, so
     * squeezing a whole tile's worth of it into 64x64 turns it into a flat green bar. Taking a 64 px
     * slice and narrowing only its width leaves the twist and the spines intact, and because
     * neighbouring tiles take neighbouring slices the column runs unbroken from the ground to its tip.
     *
     * @param depth tiles below the top of the vine column; 0 is the growing tip.
     */
    public void drawVine(SpriteBatch batch, int depth, float x, float y, float size) {
        if (!ready) return;
        int slice = depth < 0 ? 0 : Math.min(depth, VINE_SLICES - 1);
        batch.setColor(Color.WHITE);
        batch.draw(vineSlices[slice], x, y, size, size);
    }

    /**
     * A whole gate, drawn once per column rather than once per tile: the shutter is one object, and
     * scaling each half of it separately would put a seam through the middle of the door.
     *
     * <p>Scaled to the doorway's height and centred on it. The frame comes out a little wider than the
     * single tile of collision, which is what a gate embedded in a wall should look like - the
     * collision is the opening, the picture is the opening plus its frame.
     *
     * @param footY the bottom of the doorway, i.e. the floor the gate stands on
     */
    public void drawGate(SpriteBatch batch, float centerX, float footY, float height, boolean isOpen) {
        if (!ready) return;
        TextureRegion g = isOpen ? open : shut;
        float w = height * (g.getRegionWidth() / (float) g.getRegionHeight());
        batch.setColor(Color.WHITE);
        batch.draw(g, centerX - w / 2f, footY, w, height);
    }

    /**
     * The pressure plate, sitting in the floor so the cat's feet rest on it rather than in it.
     *
     * <p>{@code pressed} is the whole point of drawing it in two states: a plate that is held down has
     * to <em>look</em> held down. Waiting, it stands {@link #PLATE_LIP} proud of the floor at its full
     * height; pressed, it is shorter and sunk a few pixels below the line, so the pad visibly drops
     * away under the cat's weight and stays dropped. That drop is what the player reads as "I did
     * that", long after the flash has faded.
     *
     * <p>The sheet's pad is 4.36:1, which at one tile wide is 15 px tall and all but invisible against
     * the ground, so it is stretched to {@link #PLATE_DRAW_H} while waiting; the shape is a slab with a
     * green top and a dark recess, and losing a little of its proportion costs nothing next to being
     * able to see it. {@link LevelWorld} adds the glow and the burst.
     */
    public void drawPlate(SpriteBatch batch, float x, float floorY, float size, boolean pressed) {
        if (!ready) return;
        batch.setColor(Color.WHITE);
        if (pressed) {
            batch.draw(plate, x, floorY - PLATE_DOWN_SINK - PLATE_DOWN_H + PLATE_LIP, size, PLATE_DOWN_H);
        } else {
            batch.draw(plate, x, floorY + PLATE_LIP - PLATE_DRAW_H, size, PLATE_DRAW_H);
        }
    }

    /** A leafy strand hung from a ceiling: decoration only, in no collision list. */
    public void drawHangingVine(SpriteBatch batch, float centerX, float topY, float height) {
        if (!ready) return;
        float w = height * (HANG_W / (float) HANG_H);
        batch.setColor(Color.WHITE);
        batch.draw(hang, centerX - w / 2f, topY - height, w, height);
    }

    /** A cluster of glowing crystals, for the greenhouse core. Decoration only. */
    public void drawGlowPlant(SpriteBatch batch, float centerX, float footY, float height) {
        if (!ready) return;
        float w = height * (CRYSTAL_W / (float) CRYSTAL_H);
        batch.setColor(Color.WHITE);
        batch.draw(crystal, centerX - w / 2f, footY, w, height);
    }

    /**
     * A pipe fitting, standing on {@code footY} at its own aspect so a valve wheel stays round.
     *
     * @param kind index into the sheet's pipe row; out-of-range values fall back to the elbow
     */
    public void drawPipe(SpriteBatch batch, int kind, float centerX, float footY, float height) {
        if (!ready) return;
        int k = kind < 0 || kind >= pipes.length ? 0 : kind;
        TextureRegion t = pipes[k];
        float w = height * (t.getRegionWidth() / (float) t.getRegionHeight());
        batch.setColor(Color.WHITE);
        batch.draw(t, centerX - w / 2f, footY, w, height);
    }

    // ---------------------------------------------------------------- the slime

    private static final String SLIME_PNG = "Cute Dark Slime Sprite Sheet.png";

    /** The walk cycle: one shared top and height, six different widths. Same row level 1 uses. */
    private static final int SLIME_Y = 371, SLIME_H = 131;
    private static final int[] SLIME_X = {93, 321, 562, 802, 1047, 1293};
    private static final int[] SLIME_W = {150, 148, 153, 154, 156, 140};

    /**
     * One frame of the slime walk cycle, bottom-aligned on {@code feetY} and centred on {@code centerX}.
     *
     * <p>Level 2 has its own copy of this rather than reaching into {@link SewerArt}, because only one
     * of the two art modules is ever built. It is the same object at the same rectangles, so a slime
     * looks the same in both levels - which matters more than the handful of duplicated numbers, since
     * a monster the player already knows how to read should not change costume between levels.
     *
     * @param flip true when the slime is walking left (the art faces right)
     */
    public void drawSlime(SpriteBatch batch, float centerX, float feetY, float height, int frame, boolean flip) {
        if (!ready) return;
        int f = frame < 0 ? 0 : frame % SLIME_X.length;
        float w = height * (SLIME_W[f] / (float) SLIME_H);
        batch.setColor(Color.WHITE);
        batch.draw(flip ? slimeFlipped[f] : slimeFrames[f], centerX - w / 2f, feetY, w, height);
    }

    // ---------------------------------------------------------------- checkpoint and exit

    /**
     * The checkpoint post, base at ({@code x}, {@code y}). Same object and same treatment as level 1:
     * {@code dim} is the "not reached yet" state, so touching it is the whole feedback the player gets.
     */
    public void drawCheckpoint(SpriteBatch batch, float x, float y, float w, float h, boolean dim) {
        if (!ready) return;
        batch.setColor(dim ? CHECKPOINT_DIM : Color.WHITE);
        batch.draw(checkpoint, x, y, w, h);
        batch.setColor(Color.WHITE);
    }

    /** The exit archway, base at ({@code x}, {@code y}). */
    public void drawExit(SpriteBatch batch, float x, float y, float w, float h) {
        if (!ready) return;
        batch.setColor(Color.WHITE);
        batch.draw(exitDoor, x, y, w, h);
    }

    @Override
    public void dispose() {
        for (int i = 0; i < textures.size; i++) textures.get(i).dispose();
        textures.clear();
    }
}
