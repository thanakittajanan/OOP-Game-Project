package com.Group6.mygame.level;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

/** Tier 1. Walks back and forth on its patrol. Get past it by jumping over it. */
public class Blob extends Monster {
    public static final float SPEED = 120f;

    public Blob(LevelArt art, float centerX, float groundY, float range, Rectangle cat) {
        super(art, centerX, groundY, 56f, 48f, range, cat);
    }

    @Override
    public int getTier() {
        return 1;
    }

    @Override
    public void update(float delta) {
        time += delta;
        patrol(SPEED, delta);
    }

    @Override
    public void render(SpriteBatch batch) {
        float x = bounds.x, y = bounds.y, w = bounds.width, h = bounds.height;
        float bob = (float) Math.sin(time * 6f) * 3f;
        // spikes on the back
        for (int k = 0; k < 3; k++) {
            art.draw(batch, art.triangleUp, art.theme.hazard, x + 6f + k * 15f, y + h - 8f + bob, 14f, 16f);
        }
        art.draw(batch, art.white, 0.05f, 0.04f, 0.07f, 1f, x, y, w, h - 4f + bob);
        art.draw(batch, art.circle, 0.05f, 0.04f, 0.07f, 1f, x - 6f, y, 20f, 20f);
        art.draw(batch, art.circle, 0.05f, 0.04f, 0.07f, 1f, x + w - 14f, y, 20f, 20f);
        float ex = x + w / 2f + direction * 7f;
        art.draw(batch, art.white, 1f, 1f, 1f, 1f, ex - 14f, y + h - 24f + bob, 9f, 9f);
        art.draw(batch, art.white, 1f, 1f, 1f, 1f, ex + 5f, y + h - 24f + bob, 9f, 9f);
        drawCommon(batch);
    }
}
