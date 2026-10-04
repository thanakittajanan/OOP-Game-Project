package com.Group6.mygame;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;

/** Bridges the physics-driven {@link Player} into scene2d so it can live inside a Stage. */
public class PlayerActor extends Actor {
    private final Player player;

    public PlayerActor(Player player) {
        this.player = player;
    }

    @Override
    public void act(float delta) {
        player.update(delta);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        // The stage's batch is a SpriteBatch; Player draws its sprite directly in world coordinates.
        player.render((SpriteBatch) batch);
    }
}
