package com.Group6.mygame.level;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

/** One tile of floor spikes. The picture comes from the tile map; this class only does the damage. */
public class SpikeStrip extends Hazard {

    public SpikeStrip(LevelArt art, float tileX, float tileY, float tileSize) {
        // Only the lower 60% of the tile is sharp, and the sides are trimmed so brushing past is fair.
        super(art, tileX + 6f, tileY, tileSize - 12f, tileSize * 0.6f);
    }

    @Override
    public boolean touches(Rectangle cat) {
        return cat.overlaps(bounds);
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
