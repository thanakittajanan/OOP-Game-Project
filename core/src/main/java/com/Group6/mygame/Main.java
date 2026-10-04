package com.Group6.mygame;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.Texture;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends Game {
    // Virtual resolution (16:9) shared by every screen; all sizes/positions are in these world units.
    public static final float WORLD_WIDTH = 1920f;
    public static final float WORLD_HEIGHT = 1080f;

    // Load asset files first to be able to use.
    Texture physicalCatStandTexture;
    Texture physicalCatWalk1Texture;
    Texture physicalCatWalk2Texture;
    Texture physicalCatUpwardJump1Texture;
    Texture physicalCatDownwardJump2Texture;
    Texture currentPhysicalCatTexture;
    Texture ozflow1Texture;
    Texture ozflow2Texture;
    Texture ozflow3Texture;
    Texture level1Texture;


    @Override
    public void create() {
        physicalCatStandTexture = new Texture("physical-stand-default.png");
        physicalCatWalk1Texture = new Texture("physical-walk-01.png");
        physicalCatWalk2Texture = new Texture("physical-walk-02.png");
        physicalCatUpwardJump1Texture = new Texture("physical-upward-jump-01.png");
        physicalCatDownwardJump2Texture = new Texture("physical-downward-jump-02.png");
        ozflow1Texture = new Texture("ozflow1.png");
        ozflow2Texture = new Texture("ozflow2.png");
        ozflow3Texture = new Texture("ozflow3.png");
        level1Texture = new Texture("Level_1.jpg");
        level1Texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear); // smoother 0.5x downscale

        //default starting texture
        currentPhysicalCatTexture = physicalCatStandTexture;


        setScreen(new MainMenuScreen(this));
    }

    /** Builds a Player at the given spawn position, using the textures loaded in create(). */
    Player createPlayer(float x, float y) {
        return new Player(x, y,
            physicalCatStandTexture,
            physicalCatWalk1Texture,
            physicalCatWalk2Texture,
            physicalCatUpwardJump1Texture,
            physicalCatDownwardJump2Texture,
            ozflow1Texture,
            ozflow2Texture,
            ozflow3Texture);
    }

    @Override
    public void dispose() {
        // Game.setScreen()/Game.dispose() only call hide(), so screens are never disposed automatically.
        if (screen != null) screen.dispose();

        physicalCatStandTexture.dispose();
        physicalCatWalk1Texture.dispose();
        physicalCatWalk2Texture.dispose();
        physicalCatUpwardJump1Texture.dispose();
        physicalCatDownwardJump2Texture.dispose();
        ozflow1Texture.dispose();
        ozflow2Texture.dispose();
        ozflow3Texture.dispose();
        level1Texture.dispose();
        // currentPhysicalCatTexture aliases physicalCatStandTexture - do not dispose it twice.
    }
}
