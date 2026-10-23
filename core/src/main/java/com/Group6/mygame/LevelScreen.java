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
 * Plays one level (1 to 4). The levels themselves are written in {@code level/LevelData.java}.
 *
 * <p>Two cameras: the world camera scrolls sideways to follow the cat; the UI stage (back button, HUD)
 * stays fixed on screen. Draw order: background image → tiles → traps and monsters → cat → UI.
 *
 * <p>Press F3 to show the debug view (solid boxes, bars, hitboxes). Press F4 to lock / unlock the
 * gas form (developer shortcut, so level 4 can be tested without replaying level 3).
 */
public class LevelScreen extends BaseScreen {
    public static final int LEVEL_COUNT = LevelData.LEVEL_COUNT;
    private static final float END_DELAY = 1.6f;        // seconds the "cleared" / "game over" message shows
    private static final float CAMERA_LOOK_AHEAD = 0f;

    private static boolean debug = false;

    private final int levelNumber;
    private final boolean restoreCheckpoint;   // true after a game over: start from the last flag
    private Texture backgroundTexture;         // levels 2-4: one flat picture, drawn camera-locked
    private ParallaxBackground parallax;       // level 1: two layers that slide at different speeds
    private Player player;
    private LevelWorld world;

    private OrthographicCamera camera;
    private FitViewport worldViewport;
    private SpriteBatch batch;
    private ShapeRenderer shapes;

    // The heat bar (top left, under the back button). Drawn with the shape renderer in UI coordinates.
    private static final float BAR_X = 40f, BAR_Y = 905f, BAR_W = 380f, BAR_H = 26f;

    private Label hudLabel;
    private Label heatLabel;
    private Label messageLabel;
    private float endTimer;

    public LevelScreen(Main game, int levelNumber) {
        this(game, levelNumber, false);
    }

    public LevelScreen(Main game, int levelNumber, boolean restoreCheckpoint) {
        super(game);
        this.restoreCheckpoint = restoreCheckpoint;
        this.levelNumber = Math.max(1, Math.min(LEVEL_COUNT, levelNumber));
    }

    @Override
    protected void buildUi() {
        Audio.playLevelMusic(levelNumber);      // only level 1 has a track so far

        // Levels 1 and 2 have layered art; the rest keep their single flat picture. The far layer moves
        // slower than the midground in both, which is the whole point of a parallax pair.
        if (levelNumber == 1) {
            parallax = new ParallaxBackground(
                "Misty Industrial Sewer Depths-level 1 long shot.png", 0.15f,
                "Futuristic Sewer Parallax Midground.png", 0.4f);
        } else if (levelNumber == 2) {
            parallax = new ParallaxBackground(
                "FAR BACKGROUND for Level 2.png", 0.15f,
                "MIDGROUND asset for Level 2.png", 0.4f);
        } else {
            backgroundTexture = new Texture("Level_" + levelNumber + ".jpg");
            backgroundTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }

        camera = new OrthographicCamera();
        worldViewport = new FitViewport(Main.WORLD_WIDTH, Main.WORLD_HEIGHT, camera);
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();

        player = game.createPlayer(0f, 0f);       // LevelWorld moves it to the map's spawn point
        world = new LevelWorld(levelNumber, player, restoreCheckpoint);
        if (levelNumber == LEVEL_COUNT && !GasProgress.isUnlocked()) {
            world.announce("This level needs the gas form - collect the star at the end of The Furnace");
        }

        // HUD: level name and HP, top-right
        Table hud = new Table();
        hud.setFillParent(true);
        hud.top().right().pad(28f);
        hudLabel = new Label("", skin);
        hud.add(hudLabel);
        stage.addActor(hud);

        // heat bar caption, under the back button
        Table heat = new Table();
        heat.setFillParent(true);
        heat.top().left().padTop(100f).padLeft(BAR_X);
        heatLabel = new Label("", skin);
        heat.add(heatLabel);
        stage.addActor(heat);

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
        help.add(new Label("A / D move    Space jump, climb vines    hold S = lie prone (low gaps)"
            + "    Q = liquid on/off (bars, grates)    E = gas on/off (then W A S D fly)    Shift run"
            + "    F3 debug    F4 gas lock (dev)    Esc back",
            skin, "roster"));
        stage.addActor(help);

        addBackButton(this::backToLevelSelect);
        addKeyboardShortcuts(null, this::backToLevelSelect);
    }

    @Override
    public void render(float delta) {
        world.update(delta);
        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) debug = !debug;
        if (Gdx.input.isKeyJustPressed(Input.Keys.F4)) {
            GasProgress.set(!GasProgress.isUnlocked());
            world.announce(GasProgress.isUnlocked() ? "Gas form unlocked (dev)" : "Gas form locked (dev)");
        }
        if (handleLevelEnd(delta)) return;        // switched screen: this one is disposed now

        followCat();

        ScreenUtils.clear(0.02f, 0.03f, 0.08f, 1f);
        worldViewport.apply();
        batch.setProjectionMatrix(camera.combined);

        // 1. background fills the view
        float left = camera.position.x - Main.WORLD_WIDTH / 2f;
        batch.setColor(Color.WHITE);
        batch.begin();
        if (parallax != null) {
            parallax.render(batch, camera.position.x, camera.position.y);   // moves slower than the cat
        } else {
            float bottom = camera.position.y - Main.WORLD_HEIGHT / 2f;
            batch.draw(backgroundTexture, left, bottom, Main.WORLD_WIDTH, Main.WORLD_HEIGHT);
        }

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
        heatLabel.setText(GasProgress.isUnlocked()
            ? "HEAT  " + Math.round(player.getHeat().fraction() * 100f) + "%"
            : "GAS LOCKED  (star at the end of The Furnace)");
        stage.getViewport().apply();
        if (GasProgress.isUnlocked()) drawHeatBar();
        stage.act(delta);
        stage.draw();
    }

    /**
     * The single progress bar. Orange when hot, blue as it cools; the tick marks the "thin" line below
     * which a cloud cannot turn a valve. Drawn before the stage so the label sits on top of nothing.
     */
    private void drawHeatBar() {
        float f = player.getHeat().fraction();
        shapes.setProjectionMatrix(stage.getCamera().combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.05f, 0.07f, 0.14f, 1f);
        shapes.rect(BAR_X - 4f, BAR_Y - 4f, BAR_W + 8f, BAR_H + 8f);
        shapes.setColor(0.18f, 0.22f, 0.34f, 1f);
        shapes.rect(BAR_X, BAR_Y, BAR_W, BAR_H);
        shapes.setColor(0.35f + 0.65f * f, 0.65f - 0.15f * f, 1f - 0.8f * f, 1f);
        shapes.rect(BAR_X, BAR_Y, BAR_W * f, BAR_H);
        shapes.setColor(1f, 1f, 1f, 1f);
        shapes.rect(BAR_X + BAR_W * HeatMeter.THIN_FRACTION - 1f, BAR_Y - 4f, 3f, BAR_H + 8f);
        shapes.end();
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
            switchTo(new LevelScreen(game, levelNumber, true));   // back to the last checkpoint
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
        // Gates and plates get their own colours: they are the two things in a level whose collision
        // changes while it is running, so seeing them move is the point of looking.
        shapes.setColor(Color.MAGENTA);
        for (int pair = 0; pair < LevelMap.PLATE_PAIRS; pair++) drawBoxes(map.getGates(pair));
        shapes.setColor(Color.PINK);
        for (int i = 0; i < map.getPlates().size; i++) {
            Rectangle r = map.getPlates().get(i).bounds;
            shapes.rect(r.x, r.y, r.width, r.height);
        }
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
        if (parallax != null) { parallax.dispose(); parallax = null; }
    }
}
