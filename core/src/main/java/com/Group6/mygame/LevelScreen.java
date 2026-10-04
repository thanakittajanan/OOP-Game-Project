package com.Group6.mygame;

import com.badlogic.gdx.graphics.Texture;

/** Placeholder level screen: level background image + playable cat. Traps/terrain come later. */
public class LevelScreen extends BaseScreen {
    private final Texture backgroundTexture;
    private Player player;

    public LevelScreen(Main game, Texture backgroundTexture) {
        super(game);
        this.backgroundTexture = backgroundTexture;
    }

    @Override
    protected void buildUi() {
        addCoverBackground(backgroundTexture);

        // Draw order: background, then the cat, then the UI (back button).
        player = game.createPlayer(200f, 200f); // spawns a bit above the floor so it drops in
        stage.addActor(new PlayerActor(player));

        addBackButton(new Runnable() {
            @Override
            public void run() {
                backToLevelSelect();
            }
        });

        addKeyboardShortcuts(null, new Runnable() {
            @Override
            public void run() {
                backToLevelSelect();
            }
        });
    }

    private void backToLevelSelect() {
        switchTo(new LevelSelectScreen(game));
    }
}
