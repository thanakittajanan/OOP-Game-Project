package com.Group6.mygame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

/** Shared scaffolding for the game's UI screens: stage, code-built skin, cover background and lifecycle. */
public abstract class BaseScreen implements Screen {
    // The default font is 15pt; these scales keep text readable in the 1920x1080 world.
    private static final float BODY_FONT_SCALE = 1.5f;
    private static final float TITLE_FONT_SCALE = 3f;

    protected final Main game;
    protected Stage stage;
    protected Skin skin;
    private boolean disposed;

    protected BaseScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new FitViewport(Main.WORLD_WIDTH, Main.WORLD_HEIGHT));
        skin = buildSkin();
        buildUi();
        Gdx.input.setInputProcessor(stage);
    }

    /** Subclasses add their widgets to the stage here. */
    protected abstract void buildUi();

    /** Adds a background image that covers the whole world: aspect preserved, overflow cropped. */
    protected void addCoverBackground(Texture texture) {
        Image background = new Image(texture);
        background.setFillParent(true);
        background.setScaling(Scaling.fill);
        background.setTouchable(Touchable.disabled);
        stage.addActor(background);
    }

    /** Adds a "< Back" button pinned to the top-left corner. */
    protected TextButton addBackButton(final Runnable action) {
        Table bar = new Table();
        bar.setFillParent(true);
        bar.top().left().pad(28f);
        TextButton backButton = new TextButton("< Back", skin);
        bar.add(backButton).width(180f).height(56f);
        stage.addActor(bar);

        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                action.run();
            }
        });
        return backButton;
    }

    /** Switches to another screen and releases this one (Game.setScreen never disposes screens). */
    protected void switchTo(Screen next) {
        if (disposed) return;
        game.setScreen(next);
        dispose();
    }

    /** Routes Enter/Space to onConfirm and Esc to onCancel (null = key does nothing). */
    protected void addKeyboardShortcuts(final Runnable onConfirm, final Runnable onCancel) {
        stage.getRoot().addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (onConfirm != null && (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE)) {
                    onConfirm.run();
                    return true;
                }
                if (onCancel != null && keycode == Input.Keys.ESCAPE) {
                    onCancel.run();
                    return true;
                }
                return false;
            }
        });
    }

    /** Builds the whole skin in code: a 1x1 white texture tinted into button states plus three default fonts. */
    private Skin buildSkin() {
        Skin skin = new Skin();

        // 1x1 white texture, stretched and tinted into every button state.
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        Texture whiteTexture = new Texture(pixmap);
        pixmap.dispose();
        skin.add("white", whiteTexture); // registered -> disposed together with the skin

        // Built-in 15pt font (ASCII only). Separate instances so each size is scaled independently.
        BitmapFont smallFont = new BitmapFont();
        BitmapFont bodyFont = new BitmapFont();
        BitmapFont titleFont = new BitmapFont();
        bodyFont.getData().setScale(BODY_FONT_SCALE);
        titleFont.getData().setScale(TITLE_FONT_SCALE);
        bodyFont.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        titleFont.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        skin.add("small-font", smallFont);
        skin.add("default-font", bodyFont);
        skin.add("title-font", titleFont);

        skin.add("default", new Label.LabelStyle(bodyFont, new Color(0.85f, 0.90f, 1f, 1f)));
        skin.add("title", new Label.LabelStyle(titleFont, Color.WHITE));
        skin.add("roster-header", new Label.LabelStyle(bodyFont, new Color(0.75f, 0.82f, 0.95f, 1f)));
        skin.add("roster", new Label.LabelStyle(smallFont, new Color(0.60f, 0.68f, 0.82f, 1f)));

        // Buttons: translucent navy -> lighter on hover -> bright blue pressed, matching the wallpaper.
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.up = skin.newDrawable("white", new Color(0.07f, 0.12f, 0.25f, 0.85f));
        buttonStyle.over = skin.newDrawable("white", new Color(0.15f, 0.26f, 0.50f, 0.95f));
        buttonStyle.down = skin.newDrawable("white", new Color(0.30f, 0.52f, 0.88f, 1f));
        buttonStyle.checked = buttonStyle.up; // buttons toggle checked on click; keep the resting look stable
        buttonStyle.disabled = skin.newDrawable("white", new Color(0.05f, 0.08f, 0.15f, 0.55f));
        buttonStyle.font = bodyFont;
        buttonStyle.fontColor = Color.WHITE;
        buttonStyle.overFontColor = new Color(0.85f, 0.92f, 1f, 1f);
        buttonStyle.disabledFontColor = new Color(0.45f, 0.52f, 0.65f, 1f);
        skin.add("default", buttonStyle);

        return skin;
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.02f, 0.03f, 0.08f, 1f); // dark navy, matches the wallpaper/letterbox bars
        stage.getViewport().apply();                // Stage.draw() does not apply the viewport itself
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) return;
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
        if (Gdx.input.getInputProcessor() == stage) Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        if (disposed) return;
        disposed = true;
        if (stage != null && Gdx.input.getInputProcessor() == stage) Gdx.input.setInputProcessor(null);
        if (stage != null) stage.dispose(); // disposes its own SpriteBatch
        if (skin != null) skin.dispose();   // disposes the white texture + all fonts (registered resources)
        // textures loaded by Main (e.g. level backgrounds) are owned by Main and must not be disposed here.
    }
}
