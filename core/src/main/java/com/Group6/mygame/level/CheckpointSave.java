package com.Group6.mygame.level;

/**
 * Remembers the last checkpoint of the level being played, so that dying (HP 0) restarts the level
 * from that flag instead of from the very beginning.
 *
 * <p>A dead cat means a brand-new {@code LevelScreen} and {@code LevelWorld}, so the checkpoint has to
 * live outside them. It is kept in memory only: it survives a game over, but not closing the game, and
 * it is cleared whenever a level is entered fresh (level select, next level) or cleared.
 */
public final class CheckpointSave {
    private static int level = -1;
    private static float x, y;
    private static boolean[] reached = new boolean[0];
    private static boolean valveOpen;

    private CheckpointSave() {
    }

    public static void store(int levelNumber, float respawnX, float respawnY, boolean[] flags, boolean valve) {
        level = levelNumber;
        x = respawnX;
        y = respawnY;
        reached = flags.clone();
        valveOpen = valve;
    }

    public static boolean has(int levelNumber) {
        return level == levelNumber;
    }

    public static float x() { return x; }
    public static float y() { return y; }
    public static boolean[] reached() { return reached; }
    public static boolean valveOpen() { return valveOpen; }

    public static void clear() {
        level = -1;
    }
}
