package com.Group6.mygame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;

/** Home page: the game's wallpaper with Start and Quit buttons. */
public class MainMenuScreen extends BaseScreen {
    public MainMenuScreen(Main game) {
        super(game);
    }

    @Override
    protected void buildUi() {
        Audio.playMenuMusic();
        addCoverBackground(game.getMenuBackground());

        Table root = new Table();
        root.setFillParent(true);
        root.center().pad(60f);

        TextButton startButton = new TextButton("Start Game", skin);
        root.add(startButton).width(260f).height(60f).padBottom(24f).row();

        TextButton quitButton = new TextButton("Quit", skin);
        root.add(quitButton).width(260f).height(60f).row();

        stage.addActor(root);

        Table roster = new Table();
        roster.setFillParent(true);
        roster.bottom().padBottom(40f);
        roster.add(new Label("Team 6", skin, "roster-header")).padBottom(6f).row();
        roster.add(new Label("Pete | Shiro | Joseph | Patrik", skin, "roster"));
        stage.addActor(roster);

        startButton.addListener(onClick(this::openLevelSelect));
        quitButton.addListener(onClick(Gdx.app::exit));

        addKeyboardShortcuts(this::openLevelSelect, Gdx.app::exit);
    }

    private void openLevelSelect() {
        switchTo(new LevelSelectScreen(game));
    }
}
