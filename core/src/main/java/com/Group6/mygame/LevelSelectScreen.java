package com.Group6.mygame;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

/** Level selection: only Level 1 is playable so far, Level 2/3 are shown disabled. */
public class LevelSelectScreen extends BaseScreen {

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

        TextButton level1Button = new TextButton("Level 1", skin);
        root.add(level1Button).width(300f).height(68f).padBottom(20f).row();

        TextButton level2Button = new TextButton("Level 2", skin);
        level2Button.setDisabled(true);
        root.add(level2Button).width(300f).height(68f).padBottom(20f).row();

        TextButton level3Button = new TextButton("Level 3", skin);
        level3Button.setDisabled(true);
        root.add(level3Button).width(300f).height(68f).row();

        stage.addActor(root);

        addBackButton(new Runnable() {
            @Override
            public void run() {
                switchTo(new MainMenuScreen(game));
            }
        });

        level1Button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                openLevel1();
            }
        });

        addKeyboardShortcuts(
            new Runnable() {
                @Override
                public void run() {
                    openLevel1();
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

    private void openLevel1() {
        switchTo(new LevelScreen(game, game.level1Texture));
    }
}
