package com.Group6.mygame.level;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

/**
 * A fire basin that recharges the heat bar.
 */
public class Brazier extends Hazard {
    public static final float HEAT_PER_SECOND = 60f;
    public static final float REACH_TILES = 3f;

    private float time;

    /** @param tileX,tileY bottom-left of the tile the basin stands on top of the floor in */
    public Brazier(LevelArt art, float tileX, float tileY, float tileSize) {
        super(art, tileX, tileY, tileSize, tileSize * REACH_TILES);
    }

    @Override
    public boolean touches(Rectangle cat) {
        return false;
    }

    @Override
    public float heatRate() {
        return HEAT_PER_SECOND;
    }

    @Override
    public Effect getEffect() {
        return Effect.KNOCKBACK;
    }

    @Override
    public void update(float delta) {
        time += delta;
    }

    @Override
    public void render(SpriteBatch batch) {
        float tile = bounds.width;
        // faint column of warm air: this is the area that recharges, so the player can see its reach
        art.draw(batch, art.glow, 1f, 0.6f, 0.25f, 0.28f, bounds.x - 50f, bounds.y - 10f, bounds.width + 100f, bounds.height + 40f);
        // basin
        art.draw(batch, art.white, 0.28f, 0.2f, 0.2f, 1f, bounds.x + 8f, bounds.y, tile - 16f, 16f);
        art.draw(batch, art.white, 0.42f, 0.3f, 0.26f, 1f, bounds.x + 4f, bounds.y + 12f, tile - 8f, 8f);
        // flames
        for (int k = 0; k < 3; k++) {
            float h = 30f + 22f * (0.5f + 0.5f * (float) Math.sin(time * 9f + k * 2.1f + bounds.x));
            float x = bounds.x + 8f + k * 16f;
            if (k == 1) {
                art.draw(batch, art.flame, 1f, 0.85f, 0.4f, 1f, x, bounds.y + 18f, 18f, h + 8f);
            } else {
                art.draw(batch, art.flame, 1f, 0.54f, 0.24f, 1f, x, bounds.y + 18f, 18f, h);
            }
        }
    }
}
