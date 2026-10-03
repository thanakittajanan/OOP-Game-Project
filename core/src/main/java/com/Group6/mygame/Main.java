package com.Group6.mygame;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.Texture;
import org.w3c.dom.Text;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends Game {
    // Load asset files first to be able to use.
    Texture physicalCatStandTexture;
    Texture physicalCatWalk1Texture;
    Texture physicalCatWalk2Texture;
    Texture physicalCatUpwardJump1Texture;
    Texture physicalCatDownwardJump2Texture;
    Texture currentPhysicalCatTexture;
    Texture ozflow1Texture;
    Texture ozflow2Texture;
    Texture ozflow3Texture;


    @Override
    public void create() {
        physicalCatStandTexture = new Texture("physical-stand-default.png");
        physicalCatWalk1Texture = new Texture("physical-walk-01.png");
        physicalCatWalk2Texture = new Texture("physical-walk-02.png");
        physicalCatUpwardJump1Texture = new Texture("physical-upward-jump-01.png");
        physicalCatDownwardJump2Texture = new Texture("physical-downward-jump-02.png");
        ozflow1Texture = new Texture("ozflow1.png");
        ozflow2Texture = new Texture("ozflow2.png");
        ozflow3Texture = new Texture("ozflow3.png");

        //default starting texture
        currentPhysicalCatTexture = physicalCatStandTexture;


        setScreen(new FirstScreen(this));
    }

    @Override
    public void dispose() {
        super.dispose();
        physicalCatWalk1Texture.dispose();

    }
}
