package com.Group6.mygame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

/** Placeholder level screen: level background image + playable cat. Traps/terrain come later. */
public class LevelScreen extends BaseScreen {
    private static final boolean DEBUG = true;   // draws the solids + the cat's green hitbox

    private final Texture backgroundTexture;
    private Player player;

    // Step 2: everything the cat can collide with (Member 2's map loader will fill this later)
    private final Array<Rectangle> solids = new Array<>();
    private ShapeRenderer shapes;

    public LevelScreen(Main game, Texture backgroundTexture) {
        super(game);
        this.backgroundTexture = backgroundTexture;
    }

    @Override
    protected void buildUi() {
        addCoverBackground(backgroundTexture);

        shapes = new ShapeRenderer();
        buildTestLevel();

        // Draw order: background, then the cat, then the UI (back button).
        player = game.createPlayer(200f, 200f); // spawns a bit above the floor so it drops in
        player.setSolids(solids);               // the cat collides with everything in this list
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

    /** Temporary test blocks (world is 1920 x 1080, y goes UP, (0,0) is bottom-left). */
    private void buildTestLevel() {
        solids.clear();
        solids.add(new Rectangle(0, 0, Main.WORLD_WIDTH, 100));   // floor: its top edge is at y = 100
        solids.add(new Rectangle(700, 200, 300, 30));             // platform: cat (90 tall) fits under it, jump under it to bump the head

        // screen edges: keep the cat inside the world
        solids.add(new Rectangle(-100, 0, 100, Main.WORLD_HEIGHT));                    // left wall
        solids.add(new Rectangle(Main.WORLD_WIDTH, 0, 100, Main.WORLD_HEIGHT));        // right wall

        // staircase: three steps of 10 px each (the cat climbs steps up to 12 px without jumping)
        solids.add(new Rectangle(1050, 100, 60, 10));
        solids.add(new Rectangle(1110, 100, 60, 20));
        solids.add(new Rectangle(1170, 100, 60, 30));

        // low ceiling: 70 px gap, so the standing cat (90) does NOT fit but the crouching cat (55) does
        solids.add(new Rectangle(1330, 170, 250, 30));

        // tall wall: run into it at full speed, the cat must stop cleanly and not stick
        solids.add(new Rectangle(1750, 100, 40, 400));
    }

    @Override
    public void render(float delta) {
        super.render(delta);                       // background, cat, UI
        if (!DEBUG || shapes == null) return;

        shapes.setProjectionMatrix(stage.getViewport().getCamera().combined);

        // draw solid blocks
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(Color.GRAY);
        for (int i = 0; i < solids.size; i++) {    // indexed loop: libGDX Array iterators can't be nested
            Rectangle s = solids.get(i);
            shapes.rect(s.x, s.y, s.width, s.height);
        }
        shapes.end();

        // draw the cat's hitbox (green outline)
        Rectangle b = player.getBounds();
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(Color.GREEN);
        shapes.rect(b.x, b.y, b.width, b.height);
        shapes.end();

        // debug text: current state and speeds
        BitmapFont font = skin.getFont("default-font");
        Batch batch = stage.getBatch();
        batch.setProjectionMatrix(stage.getViewport().getCamera().combined);
        batch.begin();
        font.draw(batch, "State: " + player.getStateName()
            + "   onGround: " + player.isOnGround()
            + "   crouching: " + player.isCrouching(), 40f, 960f);
        font.draw(batch, "vx: " + Math.round(player.getVelocity().x)
            + "   vy: " + Math.round(player.getVelocity().y), 40f, 920f);
        batch.end();
    }

    private void backToLevelSelect() {
        switchTo(new LevelSelectScreen(game));
    }

    @Override
    public void dispose() {
        super.dispose();
        if (shapes != null) {
            shapes.dispose();
            shapes = null;
        }
    }
}
