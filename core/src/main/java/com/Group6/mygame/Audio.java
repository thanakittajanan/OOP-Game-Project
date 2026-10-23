package com.Group6.mygame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;

/**
 * Every sound the game makes, in one place. Screens and game objects call the static methods; nothing
 * else holds a {@link Music} or {@link Sound}.
 *
 * <p>Two kinds of clip, loaded differently. The two long backing tracks are {@code Music}, which
 * streams from disk instead of sitting in memory - the menu one alone is three and a half minutes.
 * Everything else is a {@code Sound}: decoded up front so it can fire the moment it is asked for.
 *
 * <p>Music is deliberately <em>not</em> owned by a screen. {@code BaseScreen.switchTo} disposes the
 * outgoing screen, so a track stored there would be cut off the instant the player moved on. Instead
 * the current track is remembered by {@link #playing}, which also stops the menu music restarting
 * from the top when the player walks from the main menu into the level list.
 *
 * <p>Nothing here is required for the game to run. Every file is loaded inside a try/catch and a
 * failure is logged and ignored, so one unreadable file cannot stop the game from starting - it just
 * goes quiet. That matters because the assets folder is hand-managed.
 */
public final class Audio {
    /** Music sits well under the effects, so a checkpoint never gets swallowed by the backing track. */
    private static final float MUSIC_VOLUME = 0.40f;
    private static final float SFX_VOLUME = 0.75f;

    private static final String MENU_TRACK = "background (1).mp3";
    private static final String LEVEL_TRACK = "mixkit-game-level-music-689.wav";

    private static final String CLICK_FILE = "mixkit-flute-alert-2307.wav";
    private static final String JUMP_FILE = "jump.mp3";
    private static final String HURT_FILE = "mixkit-creature-cry-of-hurt-2208.wav";
    private static final String CHECKPOINT_FILE = "mixkit-flute-alert-2307.wav";
    private static final String CLEAR_FILE = "mixkit-game-level-completed-2059.wav";
    private static final String GAME_OVER_FILE = "mixkit-player-losing-or-failing-2042.wav";
    private static final String MORPH_FILE = "waterdrop.mp3";
    private static final String GATE_FILE = "mixkit-game-experience-level-increased-2062.wav";
    private static final String CORE_FILE = "sparkle.mp3";

    private static Music menuMusic;
    private static Music levelMusic;

    private static Sound clickSound;
    private static Sound jumpSound;
    private static Sound hurtSound;
    private static Sound checkpointSound;
    private static Sound clearSound;
    private static Sound gameOverSound;
    private static Sound morphSound;
    private static Sound gateSound;
    private static Sound coreSound;

    /** Which track is running, so re-entering the same kind of screen does not restart it. */
    private static String playing;

    private Audio() {
    }

    // ================================================================== loading

    /** Call once from {@code Main.create()}, after the audio backend exists. */
    public static void init() {
        menuMusic = music(MENU_TRACK);
        levelMusic = music(LEVEL_TRACK);

        clickSound = sound(CLICK_FILE);
        jumpSound = sound(JUMP_FILE);
        hurtSound = sound(HURT_FILE);
        checkpointSound = sound(CHECKPOINT_FILE);
        clearSound = sound(CLEAR_FILE);
        gameOverSound = sound(GAME_OVER_FILE);
        morphSound = sound(MORPH_FILE);
        gateSound = sound(GATE_FILE);
        coreSound = sound(CORE_FILE);
    }

    private static Music music(String file) {
        try {
            return Gdx.audio.newMusic(Gdx.files.internal(file));
        } catch (Exception e) {
            Gdx.app.error("Audio", "could not load " + file + " - carrying on without it", e);
            return null;
        }
    }

    private static Sound sound(String file) {
        try {
            return Gdx.audio.newSound(Gdx.files.internal(file));
        } catch (Exception e) {
            Gdx.app.error("Audio", "could not load " + file + " - carrying on without it", e);
            return null;
        }
    }

    // ================================================================== music

    /** The backing track for the menus. Continues across the main menu and the level list. */
    public static void playMenuMusic() {
        switchTo(menuMusic, "menu");
    }

    /**
     * The backing track for playing a level. Only level 1 has one so far; anywhere else stops the
     * music rather than leaving the previous level's track running underneath.
     */
    public static void playLevelMusic(int levelNumber) {
        if (levelNumber == 1) {
            switchTo(levelMusic, "level1");
        } else {
            stopMusic();
        }
    }

    private static void switchTo(Music track, String tag) {
        if (track == null) {                    // the file would not load: go quiet rather than leave
            stopMusic();                        // whatever was playing running under the wrong screen
            return;
        }
        if (tag.equals(playing)) return;        // already running: let it play across the screen change
        stopMusic();
        track.setLooping(true);
        track.setVolume(MUSIC_VOLUME);
        track.play();
        playing = tag;
    }

    public static void stopMusic() {
        if (menuMusic != null) menuMusic.stop();
        if (levelMusic != null) levelMusic.stop();
        playing = null;
    }

    // ================================================================== effects

    /** A menu or in-game button was pressed. */
    public static void click() {
        play(clickSound);
    }

    public static void jump() {
        play(jumpSound);
    }

    /** The cat lost a heart but is still playing. */
    public static void hurt() {
        play(hurtSound);
    }

    public static void checkpoint() {
        play(checkpointSound);
    }

    /** The cat reached the exit. */
    public static void levelComplete() {
        play(clearSound);
    }

    public static void gameOver() {
        play(gameOverSound);
    }

    /** The cat changed form - currently fired for the puddle. */
    public static void morph() {
        play(morphSound);
    }

    /** A pressure plate went down and a bulkhead gate ground open. */
    public static void gate() {
        play(gateSound);
    }

    /** A greenhouse core was lifted. */
    public static void core() {
        play(coreSound);
    }

    private static void play(Sound s) {
        if (s != null) s.play(SFX_VOLUME);
    }

    // ================================================================== teardown

    public static void dispose() {
        stopMusic();
        dispose(menuMusic);
        dispose(levelMusic);
        dispose(clickSound);
        dispose(jumpSound);
        dispose(hurtSound);
        dispose(checkpointSound);
        dispose(clearSound);
        dispose(gameOverSound);
        dispose(morphSound);
        dispose(gateSound);
        dispose(coreSound);
        menuMusic = null;
        levelMusic = null;
        clickSound = jumpSound = hurtSound = checkpointSound = clearSound = gameOverSound = morphSound = null;
    }

    private static void dispose(Music m) {
        if (m != null) m.dispose();
    }

    private static void dispose(Sound s) {
        if (s != null) s.dispose();
    }
}
