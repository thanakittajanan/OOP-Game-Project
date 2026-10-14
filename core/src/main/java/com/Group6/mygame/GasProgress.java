package com.Group6.mygame;

import com.badlogic.gdx.Gdx;

/**
 * Whether the player has earned the gas form yet.
 *
 * <p>The cat gets it by collecting the star at the end of level 3. That has to outlive a single
 * {@link LevelScreen} (a new Player is built for every level) and a restart of the game, so it is
 * kept in libGDX {@code Preferences} rather than in the Player. It is a tiny static holder on
 * purpose: one flag, read in two places, written in two places.
 */
public final class GasProgress {
    private static final String FILE = "polymorphic-cat";
    private static final String KEY = "gasUnlocked";

    private static boolean loaded;
    private static boolean unlocked;

    private GasProgress() {
    }

    public static boolean isUnlocked() {
        load();
        return unlocked;
    }

    public static void unlock() {
        set(true);
    }

    /** Also used by the F4 developer toggle, so level 4 can be tested without replaying level 3. */
    public static void set(boolean value) {
        load();
        unlocked = value;
        try {
            com.badlogic.gdx.Preferences prefs = Gdx.app.getPreferences(FILE);
            prefs.putBoolean(KEY, value);
            prefs.flush();
        } catch (RuntimeException e) {
            // no preferences available (headless test): the flag simply lives in memory
        }
    }

    private static void load() {
        if (loaded) return;
        loaded = true;
        try {
            unlocked = Gdx.app.getPreferences(FILE).getBoolean(KEY, false);
        } catch (RuntimeException e) {
            unlocked = false;
        }
    }
}
