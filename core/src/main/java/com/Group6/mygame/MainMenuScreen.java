package com.Group6.mygame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

/** Home page: Level_1.jpg cover background, title, main buttons and the team roster. */
public class MainMenuScreen extends BaseScreen {

    public MainMenuScreen(Main game) {
        super(game);
    }

    @Override
    protected void buildUi() {
        addCoverBackground(game.level1Texture);

        Table root = new Table();
        root.setFillParent(true);
        root.center().pad(40f);

        root.add(new Label("Polymorphic Cat", skin, "title")).padBottom(80f).row();

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

        startButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                switchTo(new LevelSelectScreen(game));
            }
        });
        quitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Gdx.app.exit();
            }
        });

        addKeyboardShortcuts(
            new Runnable() {
                @Override
                public void run() {
                    switchTo(new LevelSelectScreen(game));
                }
            },
            new Runnable() {
                @Override
                public void run() {
                    Gdx.app.exit();
                }
            }
        );
    }
}
