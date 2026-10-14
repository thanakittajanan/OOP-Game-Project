package com.Group6.mygame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Disposable;

/**
 * One animation clip: its frames, plus the one measurement per frame that the drawing code needs to
 * keep the cat's feet on the floor.
 *
 * <p>The frames were exported one file each and the artwork inside a canvas is rarely flush with the
 * edges: the transparent rows under the cat range from 4 to 63 px across the jump clip. Drawing the
 * canvas bottom on the hitbox bottom would make the cat float by exactly that much, so at load time
 * this class scans the alpha channel once and remembers how many transparent rows sit under the
 * artwork ({@link #bottomGap(int)}).
 *
 * <p>The scan is why this class opens the files itself instead of taking ready-made {@link Texture}s:
 * once a texture has been uploaded its pixels are gone from the CPU side.
 *
 * <p>Frames are scaled by <b>canvas</b> height, never by the artwork's own height. The canvases in a
 * clip are all the same size by construction, so that holds the cat's size rock steady from frame to
 * frame, and lets the pose move around inside the canvas the way the artist drew it - a tucked jump
 * frame really is shorter than a stretched one, and a mid-melt frame really is flatter. Normalising
 * each frame by its own artwork height instead would cancel exactly that, and worse, it would make
 * the drawn size jump wherever the artwork's height is not monotonic (the melt frames go 212, 217,
 * 162, 183, 132, 117 source pixels).
 */
public final class SpriteFrames implements Disposable {
    /** Alpha above this counts as artwork; low enough to keep soft antialiased edges. */
    private static final int ALPHA_THRESHOLD = 8;

    private final Texture[] frames;
    private final int[] bottomGaps;

    private SpriteFrames(Texture[] frames, int[] bottomGaps) {
        this.frames = frames;
        this.bottomGaps = bottomGaps;
    }

    /** Loads every path as one clip. The returned object owns the textures and must be disposed. */
    public static SpriteFrames load(String... paths) {
        Texture[] frames = new Texture[paths.length];
        int[] gaps = new int[paths.length];
        for (int i = 0; i < paths.length; i++) {
            frames[i] = new Texture(paths[i]);
            gaps[i] = measureBottomGap(paths[i]);
        }
        return new SpriteFrames(frames, gaps);
    }

    public int size() {
        return frames.length;
    }

    /** Frame {@code index}, wrapping around so animations can just keep counting up. */
    public Texture get(int index) {
        return frames[Math.floorMod(index, frames.length)];
    }

    /** Transparent rows under the artwork, in source pixels; multiply by {@link #scale} to get world units. */
    public int bottomGap(int index) {
        return bottomGaps[Math.floorMod(index, frames.length)];
    }

    /**
     * How much to shrink or stretch this clip's frames to draw them {@code targetHeight} world units
     * tall. The caller multiplies <em>both</em> the frame's width and its {@link #bottomGap} by this,
     * which keeps the artwork's feet on the hitbox bottom at any size.
     */
    public float scale(float targetHeight) {
        return targetHeight / frames[0].getHeight();
    }

    /** Counts the fully transparent rows between the bottom edge of the canvas and the first visible one. */
    private static int measureBottomGap(String path) {
        Pixmap pixmap = new Pixmap(Gdx.files.internal(path));
        try {
            int gap = 0;
            for (int y = pixmap.getHeight() - 1; y >= 0; y--) {
                if (rowHasArtwork(pixmap, y)) break;
                gap++;
            }
            return gap;
        } finally {
            pixmap.dispose();
        }
    }

    private static boolean rowHasArtwork(Pixmap pixmap, int y) {
        for (int x = 0; x < pixmap.getWidth(); x++) {
            if ((pixmap.getPixel(x, y) & 0xFF) > ALPHA_THRESHOLD) return true;   // RGBA8888: alpha is the low byte
        }
        return false;
    }

    @Override
    public void dispose() {
        for (Texture frame : frames) frame.dispose();
    }
}
