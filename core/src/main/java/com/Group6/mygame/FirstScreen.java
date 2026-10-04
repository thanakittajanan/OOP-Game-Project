package com.Group6.mygame;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

/** First screen of the application. Displayed after the application is created. */
public class FirstScreen implements Screen {
    private final Main game;
    private SpriteBatch batch;
    private FitViewport viewport;
    private Player player;

    private final Array<Rectangle> solids = new Array<>();
    private ShapeRenderer shapes;

    public FirstScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        viewport = new FitViewport(Main.WORLD_WIDTH, Main.WORLD_HEIGHT);

        shapes = new ShapeRenderer();

        solids.clear();
        solids.add(new Rectangle(0, 0, Main.WORLD_WIDTH, 100));  // floor top is at y = 100
        solids.add(new Rectangle(700, 200, 300, 30));       // platform floats above it

        player = new Player(1, 100,
            game.physicalCatStandTexture,
            game.physicalCatWalk1Texture,
            game.physicalCatWalk2Texture,
            game.physicalCatUpwardJump1Texture,
            game.physicalCatDownwardJump2Texture,
            game.ozflow1Texture,
            game.ozflow2Texture,
            game.ozflow3Texture
        );
        player.setSolids(solids);   // without this the cat has nothing to stand on
    }

    @Override
    public void render(float delta) {

        player.update(delta);

        ScreenUtils.clear(0, 0, 0, 1);
        viewport.apply();
        shapes.setProjectionMatrix(viewport.getCamera().combined);
        batch.setProjectionMatrix(viewport.getCamera().combined);

        // draw solid block
        shapes.begin(ShapeRenderer.ShapeType.Filled); // start to draw
        shapes.setColor(Color.DARK_GRAY);
        for(int i = 0; i < solids.size; i++) { // number of solid in array
            Rectangle s = solids.get(i); // s represent solids in array by the index i
            shapes.rect(s.x, s.y, s.width, s.height); // draw according to the solid size
        }
        shapes.end();

        // draw cat
        batch.begin();
        player.render(batch);
        batch.end();

        // draw debug cat hit box
        Rectangle b = player.getBounds();
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(Color.GREEN);
        shapes.rect(b.x, b.y, b.width, b.height);
        shapes.end();
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
        shapes.dispose();
    }
}
