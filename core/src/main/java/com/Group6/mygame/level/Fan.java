package com.Group6.mygame.level;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

/**
 * A block of moving air. Does nothing to a walking cat; blows a gas cloud inside its box along
 * (dirX, dirY) at {@link #WIND_SPEED} px/s, and thin gas is blown further (see
 * {@code Player#applyGasControl}). A cloud flying straight into it is slowed to a crawl; one that
 * goes around loses nothing but distance, which is the whole decision.
 */
public class Fan extends Hazard {
    public static final float WIND_SPEED = 110f;

    private final float dirX, dirY;
    private float time;

    public Fan(LevelArt art, float x, float y, float width, float height, float dirX, float dirY) {
        super(art, x, y, width, height);
        this.dirX = dirX;
        this.dirY = dirY;
    }

    @Override
    public boolean touches(Rectangle cat) {
        return false;
    }

    @Override
    public float windX() {
        return dirX * WIND_SPEED;
    }

    @Override
    public float windY() {
        return dirY * WIND_SPEED;
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
        art.draw(batch, art.white, 0.75f, 0.9f, 1f, 0.07f, bounds.x, bounds.y, bounds.width, bounds.height);
        // streaks sliding along the wind: a stripe per 40 px across the box, each looping on its own phase
        boolean horizontal = dirX != 0f;
        int lanes = (int) ((horizontal ? bounds.height : bounds.width) / 40f);
        for (int i = 0; i < lanes; i++) {
            float travel = horizontal ? bounds.width : bounds.height;
            float t = ((time * 0.7f + i * 0.37f) % 1f);
            float along = (dirX + dirY > 0f ? t : 1f - t) * (travel - 36f);
            float across = 12f + i * 40f;
            float a = 0.15f + 0.35f * (float) Math.sin(t * Math.PI);
            if (horizontal) {
                art.draw(batch, art.white, 0.85f, 0.95f, 1f, a, bounds.x + along, bounds.y + across, 36f, 4f);
            } else {
                art.draw(batch, art.white, 0.85f, 0.95f, 1f, a, bounds.x + across, bounds.y + along, 4f, 36f);
            }
        }
    }
}
