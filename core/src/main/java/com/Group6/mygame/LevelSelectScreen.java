package com.Group6.mygame;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;

/**
 * Level selection: Level 1 (The Drain), Level 2 (Greenhouse Pipes), Level 3 (The Furnace),
 * Level 4 (The Vent Works - needs the gas form from the star at the end of level 3).
 */
public class LevelSelectScreen extends BaseScreen {
    private static final String[] LEVEL_NAMES = { "The Drain", "Greenhouse Pipes", "The Furnace", "The Vent Works" };

    public LevelSelectScreen(Main game) {
        super(game);
    }

    @Override
    protected void buildUi() {
        Audio.playMenuMusic();          // already playing from the main menu: carries on, no restart
        addCoverBackground(game.getMenuBackground());

        Table root = new Table();
        root.setFillParent(true);
        root.center().pad(40f);

        root.add(new Label("Select Level", skin, "title")).padBottom(70f).row();

        for (int i = 0; i < LevelScreen.LEVEL_COUNT; i++) {
            final int levelNumber = i + 1;
            TextButton button = new TextButton("Level " + levelNumber + ": " + LEVEL_NAMES[i], skin);
            root.add(button).width(460f).height(40f).padBottom(20f).row();
            button.addListener(onClick(() -> openLevel(levelNumber)));
        }

        stage.addActor(root);

        addBackButton(this::backToMenu);
        addKeyboardShortcuts(() -> openLevel(1), this::backToMenu);
    }

    private void openLevel(int levelNumber) {
        switchTo(new LevelScreen(game, levelNumber));
    }

    private void backToMenu() {
        switchTo(new MainMenuScreen(game));
    }
}
