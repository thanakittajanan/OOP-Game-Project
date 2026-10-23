package com.Group6.mygame.level;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

/**
 * The real artwork for level 1, cut out of the three sheets that ship in {@code assets/}. Levels 2-4
 * keep the coloured shapes from {@link LevelArt}; this class is only built when the level wants it.
 *
 * <p>Everything here is a sub-rectangle of one of three large sheets, so the sheets are loaded once and
 * the regions are views into them. The rectangles below were measured from the files, not guessed:
 * each sheet has transparent gutters between its sprites, and the numbers are the ink bounds.
 *
 * <p>Sheets and what is taken from them:
 * <ul>
 *   <li>{@code Industrial Sewer Platform Tile.png} (2172x724) - only rows 313..564 carry paint. That
 *       band is a vertical cross-section of a platform: a lit top edge, the beam face with rivets, a
 *       truss, then brick and pipes. It is cut into {@link #BAND_Y} bands, and the level draws band
 *       <em>d</em> for a tile that sits <em>d</em> tiles below the top of its column. The result is
 *       that any solid column shows a lit top edge and gets darker and more structural as it goes down.</li>
 *   <li>{@code Cartoon Spike Trap Tileset Sprite Sheet.png} (1774x887) - one compact floor trap,
 *       142x137, which is almost exactly square so it fills one 64 px tile without distortion.</li>
 *   <li>{@code Cute Dark Slime Sprite Sheet.png} (1536x1024) - the row labelled "walking animation
 *       (6 frames)" at y 371..501. All six frames share that top and height, so they are a proper
 *       cycle; the widths differ, which is the slime squashing as it walks. The captions printed on
 *       the sheet are baked into the pixels, so only this row is safe to cut - the rows below carry
 *       Chinese labels under each pose.</li>
 * </ul>
 */
public class SewerArt implements Disposable {
    private static final String PLATFORM_PNG = "Industrial Sewer Platform Tile.png";
    private static final String SPIKE_PNG = "Cartoon Spike Trap Tileset Sprite Sheet.png";
    private static final String SLIME_PNG = "Cute Dark Slime Sprite Sheet.png";
    private static final String CHECKPOINT_PNG = "industrial checkpoint.png";
    private static final String EXIT_PNG = "industrial sewer exit door.png";

    // ---------------------------------------------------------------- measured rectangles

    /** The platform sheet is 2172x724; rows 313..564 are the cross-section, and 313..564 is all of it. */
    private static final int PLATFORM_W = 2172;
    /** Top of each band, and its height. Contiguous: 313+64, +64, +64, +59 = 564. */
    private static final int[] BAND_Y = {313, 377, 441, 505};
    private static final int[] BAND_H = {64, 64, 64, 59};

    /** Floor trap: three spikes on a riveted base. Near square, so it maps onto one tile unchanged. */
    private static final int SPIKE_X = 203, SPIKE_Y = 716, SPIKE_W = 142, SPIKE_H = 137;

    /** The walk cycle: one shared top and height, six different widths. */
    public static final int SLIME_FRAMES = 6;
    private static final int SLIME_Y = 371, SLIME_H = 131;
    private static final int[] SLIME_X = {93, 321, 562, 802, 1047, 1293};
    private static final int[] SLIME_W = {150, 148, 153, 154, 156, 140};

    /** Both 1024x1024 files hold one object on a transparent field; these are its ink bounds. */
    private static final int CHECKPOINT_X = 252, CHECKPOINT_Y = 44, CHECKPOINT_W = 530, CHECKPOINT_H = 905;
    private static final int EXIT_X = 73, EXIT_Y = 84, EXIT_W = 885, EXIT_H = 824;

    /** Width / height of each, so a caller given one dimension can work out the other. */
    public static final float CHECKPOINT_ASPECT = CHECKPOINT_W / (float) CHECKPOINT_H;
    public static final float EXIT_ASPECT = EXIT_W / (float) EXIT_H;

    // ---------------------------------------------------------------- state

    private final Texture platformSheet;
    private final Texture spikeSheet;
    private final Texture slimeSheet;
    private final Texture checkpointSheet;
    private final Texture exitSheet;

    private final TextureRegion spike;
    private final TextureRegion checkpoint;
    private final TextureRegion exitDoor;
    private final TextureRegion[] slimeWalk = new TextureRegion[SLIME_FRAMES];
    private final TextureRegion[] slimeWalkFlipped = new TextureRegion[SLIME_FRAMES];

    private final Array<Texture> textures = new Array<>();

    public SewerArt() {
        platformSheet = load(PLATFORM_PNG);
        spikeSheet = load(SPIKE_PNG);
        slimeSheet = load(SLIME_PNG);
        checkpointSheet = load(CHECKPOINT_PNG);
        exitSheet = load(EXIT_PNG);

        spike = new TextureRegion(spikeSheet, SPIKE_X, SPIKE_Y, SPIKE_W, SPIKE_H);
        checkpoint = new TextureRegion(checkpointSheet, CHECKPOINT_X, CHECKPOINT_Y, CHECKPOINT_W, CHECKPOINT_H);
        exitDoor = new TextureRegion(exitSheet, EXIT_X, EXIT_Y, EXIT_W, EXIT_H);
        for (int i = 0; i < SLIME_FRAMES; i++) {
            slimeWalk[i] = new TextureRegion(slimeSheet, SLIME_X[i], SLIME_Y, SLIME_W[i], SLIME_H);
            slimeWalkFlipped[i] = new TextureRegion(slimeWalk[i]);
            slimeWalkFlipped[i].flip(true, false);      // the art faces right
        }
    }

    /**
     * The platform art has to run on across neighbouring tiles, so a tile near the right edge of the
     * sheet samples slightly past u = 1. Repeat wrap lets that continue into the left edge instead of
     * smearing the last column.
     */
    private Texture load(String png) {
        Texture t = new Texture(png);
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        t.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        textures.add(t);
        return t;
    }

    // ---------------------------------------------------------------- drawing

    /**
     * Draws one tile of the platform cross-section.
     *
     * @param depth how many tiles below the top of its column this tile is; 0 is the lit top surface.
     *              Anything deeper than the last band is drawn as brick.
     * @param x     the tile's left edge in world pixels. It also picks the horizontal sample point, so
     *              the beam pattern stays continuous from one tile to the next.
     */
    public void drawPlatform(SpriteBatch batch, int depth, float x, float y, float size) {
        int d = depth < 0 ? 0 : (depth >= BAND_Y.length ? BAND_Y.length - 1 : depth);
        float texH = platformSheet.getHeight();
        // Everything below the truss draws the same brick band, so a tall wall would come out as a grid
        // of identical squares. Sliding the sample sideways a little more on each row down staggers the
        // joints - which is what brickwork does anyway - and the wall stops looking stamped.
        float shift = d < 3 ? 0f : (depth - 3) * 37f;
        float u = ((x + shift) % PLATFORM_W) / (float) PLATFORM_W;
        float v = BAND_Y[d] / texH;
        float u2 = u + size / PLATFORM_W;
        float v2 = (BAND_Y[d] + BAND_H[d]) / texH;
        batch.setColor(Color.WHITE);
        // SpriteBatch only has this UV overload for a Texture, not for a TextureRegion.
        batch.draw(platformSheet, x, y, size, size, u, v, u2, v2);
    }

    /**
     * One floor spike trap, bottom-aligned in its tile. At a 64 px tile this comes out 64 x 62, so it
     * fills the tile; note the art stands a little taller than the 38 px damage box, which errs on the
     * side of forgiving the player.
     */
    public void drawSpike(SpriteBatch batch, float x, float y, float size) {
        float h = size * (SPIKE_H / (float) SPIKE_W);
        batch.setColor(Color.WHITE);
        batch.draw(spike, x, y, size, h);
    }

    /**
     * One frame of the slime walk cycle, bottom-aligned on {@code feetY} and centred on {@code centerX}.
     * Scaling by height is right for this sheet: the artist kept every frame the same height and let
     * the width change, so the width difference <em>is</em> the animation.
     *
     * @param height how tall to draw it; the width follows from the frame's own aspect.
     * @param flip   true when the slime is walking left (the art faces right).
     */
    public void drawSlime(SpriteBatch batch, float centerX, float feetY, float height, int frame, boolean flip) {
        int f = frame < 0 ? 0 : frame % SLIME_FRAMES;
        float w = height * (SLIME_W[f] / (float) SLIME_H);
        batch.setColor(Color.WHITE);
        batch.draw(flip ? slimeWalkFlipped[f] : slimeWalk[f], centerX - w / 2f, feetY, w, height);
    }

    /**
     * The checkpoint post, standing with its base at ({@code x}, {@code y}).
     *
     * <p>{@code dim} is the "not reached yet" state: the same picture multiplied down until the lamp
     * on top reads as switched off and the flag is a flat dark teal rather than glowing. It has to go
     * this dark - the flag is nearly saturated cyan to begin with, so a gentle tint still looks lit.
     * Touching it switches to full colour, which is the whole feedback the player gets.
     */
    public void drawCheckpoint(SpriteBatch batch, float x, float y, float w, float h, boolean dim) {
        if (dim) batch.setColor(0.30f, 0.33f, 0.42f, 1f);
        batch.draw(checkpoint, x, y, w, h);
        batch.setColor(Color.WHITE);
    }

    /** The exit archway, standing with its base at ({@code x}, {@code y}). */
    public void drawExit(SpriteBatch batch, float x, float y, float w, float h) {
        batch.setColor(Color.WHITE);
        batch.draw(exitDoor, x, y, w, h);
    }

    @Override
    public void dispose() {
        for (int i = 0; i < textures.size; i++) textures.get(i).dispose();
        textures.clear();
    }
}
