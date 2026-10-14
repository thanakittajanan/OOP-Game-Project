package com.Group6.mygame;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

/** Level selection: Level 1 (The Drain), Level 2 (Greenhouse Pipes), Level 3 (The Furnace). */
public class LevelSelectScreen extends BaseScreen {
    private static final String[] LEVEL_NAMES = { "The Drain", "Greenhouse Pipes", "The Furnace" };

    public LevelSelectScreen(Main game) {
        super(game);
    }

    @Override
    protected void buildUi() {
        addCoverBackground(game.level1Texture);

        Table root = new Table();
        root.setFillParent(true);
        root.center().pad(40f);

        root.add(new Label("Select Level", skin, "title")).padBottom(70f).row();

        for (int i = 0; i < LevelScreen.LEVEL_COUNT; i++) {
            final int levelNumber = i + 1;
            TextButton button = new TextButton("Level " + levelNumber + ": " + LEVEL_NAMES[i], skin);
            root.add(button).width(460f).height(68f).padBottom(20f).row();
            button.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    openLevel(levelNumber);
                }
            });
        }

        stage.addActor(root);

        addBackButton(new Runnable() {
            @Override
            public void run() {
                switchTo(new MainMenuScreen(game));
            }
        });

        addKeyboardShortcuts(
            new Runnable() {
                @Override
                public void run() {
                    openLevel(1);
                }
            },
            new Runnable() {
                @Override
                public void run() {
                    switchTo(new MainMenuScreen(game));
                }
            }
        );
    }

    private void openLevel(int levelNumber) {
        switchTo(new LevelScreen(game, levelNumber));
    }
}
