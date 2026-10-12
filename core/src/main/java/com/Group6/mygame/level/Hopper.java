package com.Group6.mygame.level;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

/**
 * Tier 2. Hops forward every {@link #CYCLE} seconds, rising {@link #HOP_HEIGHT} px.
 * It only moves while it is in the air. Get past it by jumping over it while it is on the ground,
 * or by running (Shift) under it at the top of a hop.
 */
public class Hopper extends Monster {
    public static final float AIR_SPEED = 180f;
    public static final float HOP_HEIGHT = 150f;
    public static final float AIR_TIME = 0.9f;
    public static final float CYCLE = 1.8f;

    public Hopper(LevelArt art, float centerX, float groundY, float range, Rectangle cat) {
        super(art, centerX, groundY, 48f, 44f, range, cat);
    }

    @Override
    public int getTier() {
        return 2;
    }

    @Override
    public void update(float delta) {
        time += delta;
        float t = time % CYCLE;
        if (t < AIR_TIME) {
            airHeight = HOP_HEIGHT * (float) Math.sin(Math.PI * t / AIR_TIME);
            patrol(AIR_SPEED, delta);
        } else {
            airHeight = 0f;
            patrol(0f, delta);
        }
    }

    @Override
    public void render(SpriteBatch batch) {
        float x = bounds.x, y = bounds.y, w = bounds.width, h = bounds.height;
        boolean inAir = airHeight > 2f;
        // legs
        float legSpread = inAir ? 2f : 8f;
        art.draw(batch, art.white, 0.71f, 0.55f, 0.95f, 1f, x + 8f - legSpread, y - (inAir ? 10f : 2f), 5f, 14f);
        art.draw(batch, art.white, 0.71f, 0.55f, 0.95f, 1f, x + w - 13f + legSpread, y - (inAir ? 10f : 2f), 5f, 14f);
        // body
        art.draw(batch, art.circle, 0.11f, 0.06f, 0.19f, 1f, x, y, w, h);
        // antenna
        art.draw(batch, art.white, 0.71f, 0.55f, 0.95f, 1f, x + w / 2f - 1f, y + h - 4f, 3f, 14f);
        art.draw(batch, art.circle, 0.71f, 0.55f, 0.95f, 1f, x + w / 2f - 4f + direction * 3f, y + h + 8f, 8f, 8f);
        // eyes
        float ex = x + w / 2f + direction * 6f;
        art.draw(batch, art.circle, 1f, 1f, 1f, 1f, ex - 12f, y + h / 2f - 2f, 10f, 10f);
        art.draw(batch, art.circle, 1f, 1f, 1f, 1f, ex + 2f, y + h / 2f - 2f, 10f, 10f);
        drawCommon(batch);
    }
}
