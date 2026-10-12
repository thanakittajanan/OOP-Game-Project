package com.Group6.mygame;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.Texture;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends Game {
    // Virtual resolution (16:9) shared by every screen; all sizes/positions are in these world units.
    public static final float WORLD_WIDTH = 1920f;
    public static final float WORLD_HEIGHT = 1080f;

    // The cat's animation clips. Each group is one clip, loaded as a whole so that Player can treat
    // "walk" as an array instead of a handful of separate textures. See SpriteFrames for the sizing.
    SpriteFrames catStandFrames;
    SpriteFrames catWalkFrames;
    SpriteFrames catJumpFrames;
    SpriteFrames catLiquidWalkFrames;
    SpriteFrames catMorphFrames;
    Texture level1Texture;

    @Override
    public void create() {
        // One clip per animation. The frames in a clip share a canvas size, which is what lets Player
        // hold the cat's size steady while the pose moves inside it - see SpriteFrames.
        catStandFrames = SpriteFrames.load("physical-stand-default.png");
        catWalkFrames = SpriteFrames.load(
            "physical-walk-01.png", "physical-walk-02.png", "physical-walk-03.png",
            "physical-walk-04.png", "physical-walk-05.png");
        // Frames 0-2 are the rise, 3-5 the fall - see Player.JUMP_RISE_FRAMES.
        catJumpFrames = SpriteFrames.load(
            "physical-jump-01.png", "physical-jump-02.png", "physical-jump-03.png",
            "physical-jump-04.png", "physical-jump-05.png", "physical-jump-06.png");
        catLiquidWalkFrames = SpriteFrames.load(
            "liquid-walk-01.png", "liquid-walk-02.png", "liquid-walk-03.png",
            "liquid-walk-04.png", "liquid-walk-05.png");
        catMorphFrames = SpriteFrames.load(
            "liquid-morph-01.png", "liquid-morph-02.png", "liquid-morph-03.png",
            "liquid-morph-04.png", "liquid-morph-05.png", "liquid-morph-06.png");

        level1Texture = new Texture("Level_1.jpg");
        level1Texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear); // smoother 0.5x downscale

        setScreen(new MainMenuScreen(this));
    }

    /** Builds a Player at the given spawn position, using the animation clips loaded in create(). */
    Player createPlayer(float x, float y) {
        return new Player(x, y, catStandFrames, catWalkFrames, catJumpFrames,
            catLiquidWalkFrames, catMorphFrames);
    }

    @Override
    public void dispose() {
        // Game.setScreen()/Game.dispose() only call hide(), so screens are never disposed automatically.
        if (screen != null) screen.dispose();

        catStandFrames.dispose();
        catWalkFrames.dispose();
        catJumpFrames.dispose();
        catLiquidWalkFrames.dispose();
        catMorphFrames.dispose();
        level1Texture.dispose();
    }
}
