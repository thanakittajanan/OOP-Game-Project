package com.Group6.mygame;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

/** First screen of the application. Displayed after the application is created. */
public class FirstScreen implements Screen {
    private final Main game;
    private SpriteBatch batch;
    private FitViewport viewport;
    private Player player;

    public FirstScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        viewport = new FitViewport(640, 480);
        player = new Player(1, 1,
            game.physicalCatStandTexture,
            game.physicalCatWalk1Texture,
            game.physicalCatWalk2Texture,
            game.physicalCatUpwardJump1Texture,
            game.physicalCatDownwardJump2Texture,
            game.ozflow1Texture,
            game.ozflow2Texture,
            game.ozflow3Texture
        );
    }

    @Override
    public void render(float delta) {

        player.update(delta);

        ScreenUtils.clear(0, 0, 0, 1);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);

        batch.begin();
        player.render(batch);
        batch.end();
    }



    @Override
    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) return;
        viewport.update(width, height, true);
    }

    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }

    @Override
    public void dispose() {
        batch.dispose();
    }
}
