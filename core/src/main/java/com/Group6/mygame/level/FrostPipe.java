package com.Group6.mygame.level;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

/**
 * One tile of a cold-air pipe buried in a wall. Solid rock to the cat (the tile is also in the solid
 * grid), invisible to the cat's health, and deadly to a gas cloud: a cloud that touches it
 * condenses on the spot, and inside a wall that means being sent back to the checkpoint.
 *
 * <p>The picture comes from the tile map; this class only supplies the box and the rule.
 */
public class FrostPipe extends Hazard {
    public FrostPipe(LevelArt art, float tileX, float tileY, float tileSize) {
        super(art, tileX, tileY, tileSize, tileSize);
    }

    @Override
    public boolean touches(Rectangle cat) {
        return false;
    }

    @Override
    public boolean freezesGas() {
        return true;
    }

    @Override
    public Effect getEffect() {
        return Effect.RESPAWN;
    }

    @Override
    public void update(float delta) {
    }

    @Override
    public void render(SpriteBatch batch) {
        // drawn by the tile map
    }
}
