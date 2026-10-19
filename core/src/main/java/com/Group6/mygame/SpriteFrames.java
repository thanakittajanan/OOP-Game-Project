package com.Group6.mygame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Disposable;


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
