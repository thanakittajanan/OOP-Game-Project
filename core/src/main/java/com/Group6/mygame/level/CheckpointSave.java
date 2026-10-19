package com.Group6.mygame.level;

public final class CheckpointSave {
    private static int level = -1;
    private static float x, y;
    private static boolean[] reached = new boolean[0];
    private static boolean valveOpen;
    private static int plateMask;      // which pressure-plate pairs are latched down, one bit each
    private static int coreMask;       // which greenhouse cores have been lifted, one bit each

    private CheckpointSave() {
    }

    public static void store(int levelNumber, float respawnX, float respawnY, boolean[] flags, boolean valve) {
        level = levelNumber;
        x = respawnX;
        y = respawnY;
        reached = flags.clone();
        valveOpen = valve;
    }

    /**
     * The rest of what a death must not undo. Kept separate from {@link #store} because it changes on
     * its own schedule - a plate goes down, a core is lifted - and not only when a flag is touched.
     * A gate that shut itself again behind the cat would be a dead end, so this has to outlive it.
     *
     * @param plates   one bit per pressure-plate pair, set once that pair is latched down
     * @param coreMask one bit per greenhouse core already lifted
     */
    public static void setProgress(int plates, int coreMask) {
        plateMask = plates;
        CheckpointSave.coreMask = coreMask;
    }

    public static boolean has(int levelNumber) {
        return level == levelNumber;
    }

    public static float x() { return x; }
    public static float y() { return y; }
    public static boolean[] reached() { return reached; }
    public static boolean valveOpen() { return valveOpen; }
    public static int plateMask() { return plateMask; }
    public static int coreMask() { return coreMask; }

    public static void clear() {
        level = -1;
        valveOpen = false;
        plateMask = 0;
        coreMask = 0;
    }
}
