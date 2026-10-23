package com.Group6.mygame;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.Texture;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends Game {
    /** Virtual resolution (16:9) shared by every screen; all sizes and positions are in these world units. */
    public static final float WORLD_WIDTH = 1920f;
    public static final float WORLD_HEIGHT = 1080f;

    private CatAnimations catAnimations;
    private Texture menuBackground;

    @Override
    public void create() {
        catAnimations = CatAnimations.load();

        menuBackground = new Texture("Polymorphic Cat_ Neon Factory Adventure.png");
        menuBackground.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear); // smoother 0.5x downscale

        Audio.init();                             // the music and effects outlive every screen

        setScreen(new MainMenuScreen(this));
    }

    /** Builds a Player at the given spawn position, using the animation clips loaded in create(). */
    Player createPlayer(float x, float y) {
        return new Player(x, y, catAnimations);
    }

    /** The wallpaper behind the main menu and the level-select screen. Owned (and disposed) by Main. */
    Texture getMenuBackground() {
        return menuBackground;
    }

    @Override
    public void dispose() {
        // Game.setScreen()/Game.dispose() only call hide(), so screens are never disposed automatically.
        if (screen != null) screen.dispose();

        catAnimations.dispose();
        menuBackground.dispose();
        Audio.dispose();
    }
}
