package com.Group6.mygame;

import com.Group6.mygame.level.LevelData;
import com.Group6.mygame.level.LevelMap;
import com.Group6.mygame.level.LevelWorld;
import com.Group6.mygame.level.Monster;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

/**
 * Plays one level (1, 2 or 3). The levels themselves are written in {@code level/LevelData.java}.
 *
 * <p>Two cameras: the world camera scrolls sideways to follow the cat; the UI stage (back button, HUD)
 * stays fixed on screen. Draw order: background image → tiles → traps and monsters → cat → UI.
 *
 * <p>Press F3 to show the debug view (solid boxes, bars, hitboxes).
 */
public class LevelScreen extends BaseScreen {
    public static final int LEVEL_COUNT = LevelData.LEVEL_COUNT;
    private static final float END_DELAY = 1.6f;        // seconds the "cleared" / "game over" message shows
    private static final float CAMERA_LOOK_AHEAD = 0f;

    private static boolean debug = false;

    private final int levelNumber;
    private Texture backgroundTexture;
    private Player player;
    private LevelWorld world;

    private OrthographicCamera camera;
    private FitViewport worldViewport;
    private SpriteBatch batch;
    private ShapeRenderer shapes;

    private Label hudLabel;
    private Label messageLabel;
    private float endTimer;

    public LevelScreen(Main game, int levelNumber) {
        super(game);
        this.levelNumber = Math.max(1, Math.min(LEVEL_COUNT, levelNumber));
    }

    @Override
    protected void buildUi() {
        backgroundTexture = new Texture("Level_" + levelNumber + ".jpg");
        backgroundTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        camera = new OrthographicCamera();
        worldViewport = new FitViewport(Main.WORLD_WIDTH, Main.WORLD_HEIGHT, camera);
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();

        player = game.createPlayer(0f, 0f);       // LevelWorld moves it to the map's spawn point
        world = new LevelWorld(levelNumber, player);

        // HUD: level name and HP, top-right
        Table hud = new Table();
        hud.setFillParent(true);
        hud.top().right().pad(28f);
        hudLabel = new Label("", skin);
        hud.add(hudLabel);
        stage.addActor(hud);

        // big centre message: "Checkpoint", "Level cleared!", "Game over"
        Table centre = new Table();
        centre.setFillParent(true);
        centre.top().padTop(180f);
        messageLabel = new Label("", skin, "title");
        messageLabel.setAlignment(Align.center);
        centre.add(messageLabel);
        stage.addActor(centre);

        // controls reminder, bottom
        Table help = new Table();
        help.setFillParent(true);
        help.bottom().padBottom(18f);
        help.add(new Label("A / D move    W jump (not as liquid), climb vines    hold S = melt to liquid"
            + " (bars, grates, low gaps)    Shift run    F3 debug    Esc back", skin, "roster"));
        stage.addActor(help);

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

    @Override
    public void render(float delta) {
        world.update(delta);
        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) debug = !debug;
        if (handleLevelEnd(delta)) return;        // switched screen: this one is disposed now

        followCat();

        ScreenUtils.clear(0.02f, 0.03f, 0.08f, 1f);
        worldViewport.apply();
        batch.setProjectionMatrix(camera.combined);

        // 1. background fills the view and moves with the camera
        float left = camera.position.x - Main.WORLD_WIDTH / 2f;
        float bottom = camera.position.y - Main.WORLD_HEIGHT / 2f;
        batch.setColor(Color.WHITE);
        batch.begin();
        batch.draw(backgroundTexture, left, bottom, Main.WORLD_WIDTH, Main.WORLD_HEIGHT);

        // 2. tiles, then 3. traps, monsters, cat
        world.renderTiles(batch, left, left + Main.WORLD_WIDTH);
        world.renderBehindCat(batch);
        world.renderCat(batch);
        batch.end();

        if (debug) drawDebug();

        // 4. UI
        hudLabel.setText(world.getMap().getTitle() + "   |   Level " + levelNumber + " / " + LEVEL_COUNT
            + "   |   HP " + Math.max(0, player.getHealth())
            + "   |   " + player.getForm().getName() + " / " + player.getStateName());
        messageLabel.setText(world.getMessage());
        stage.getViewport().apply();
        stage.act(delta);
        stage.draw();
    }

    /** Camera follows the cat sideways and stops at the map edges. The map is 16 tiles (1024 px) tall. */
    private void followCat() {
        LevelMap map = world.getMap();
        Rectangle cat = player.getBounds();
        float halfW = Main.WORLD_WIDTH / 2f;
        float target = cat.x + cat.width / 2f + CAMERA_LOOK_AHEAD;
        float x = Math.max(halfW, Math.min(map.getWidth() - halfW, target));
        camera.position.set(Math.round(x), Main.WORLD_HEIGHT / 2f, 0f);   // whole pixels: no gaps between tiles
        camera.update();
    }

    /** After a short message, go to the next level, back to the level list, or restart after a game over. */
    private boolean handleLevelEnd(float delta) {
        if (world.getStatus() == LevelWorld.Status.PLAYING) return false;
        endTimer += delta;
        if (endTimer < END_DELAY) return false;

        if (world.getStatus() == LevelWorld.Status.DEAD) {
            switchTo(new LevelScreen(game, levelNumber));
        } else if (levelNumber < LEVEL_COUNT) {
            switchTo(new LevelScreen(game, levelNumber + 1));
        } else {
            switchTo(new LevelSelectScreen(game));
        }
        return true;
    }

    private void drawDebug() {
        LevelMap map = world.getMap();
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(Color.GRAY);
        drawBoxes(map.getTerrainSolids());
        shapes.setColor(Color.CYAN);
        drawBoxes(map.getLiquidPassables());
        shapes.setColor(Color.YELLOW);
        drawBoxes(map.getVines());
        shapes.setColor(Color.RED);
        for (int i = 0; i < map.getHazards().size; i++) {
            Rectangle r = map.getHazards().get(i).getBounds();
            shapes.rect(r.x, r.y, r.width, r.height);
        }
        shapes.setColor(Color.ORANGE);
        for (int i = 0; i < map.getMonsters().size; i++) {
            Monster m = map.getMonsters().get(i);
            Rectangle r = m.getHitbox();
            shapes.rect(r.x, r.y, r.width, r.height);
        }
        shapes.setColor(Color.GREEN);
        Rectangle b = player.getBounds();
        shapes.rect(b.x, b.y, b.width, b.height);
        shapes.end();
    }

    private void drawBoxes(Array<Rectangle> boxes) {
        for (int i = 0; i < boxes.size; i++) {
            Rectangle r = boxes.get(i);
            shapes.rect(r.x, r.y, r.width, r.height);
        }
    }

    private void backToLevelSelect() {
        switchTo(new LevelSelectScreen(game));
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        if (width <= 0 || height <= 0 || worldViewport == null) return;
        worldViewport.update(width, height, false);
    }

    @Override
    public void dispose() {
        super.dispose();
        if (world != null) { world.dispose(); world = null; }
        if (batch != null) { batch.dispose(); batch = null; }
        if (shapes != null) { shapes.dispose(); shapes = null; }
        if (backgroundTexture != null) { backgroundTexture.dispose(); backgroundTexture = null; }
    }
}
