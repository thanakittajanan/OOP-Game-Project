package com.Group6.mygame.level;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

/** Tier 1. A slime that walks its patrol and turns around at the edge of its platform. */
public class Blob extends Monster {
    public static final float SPEED = 120f;

    /** Seconds per frame of the walk cycle. Six frames at this rate is one full stride per 0.72 s. */
    private static final float FRAME_TIME = 0.12f;
    /** Drawn height in level 1, a little taller than the 48 px hitbox so it reads clearly. */
    private static final float DRAW_HEIGHT = 56f;

    public Blob(LevelArt art, LevelMap map, float centerX, float groundY, float range, Rectangle cat) {
        super(art, map, true, centerX, groundY, 56f, 48f, range, cat);
    }

    @Override
    public int getTier() {
        return 1;
    }

    @Override
    public void update(float delta) {
        advanceTime(delta);
        patrol(SPEED, delta);
    }

    @Override
    public void render(SpriteBatch batch) {
        if (art.hasSewerArt() || art.hasGreenhouseArt()) {
            // The sheet is a real cycle, so the width of each frame is the squash - nothing else moves.
            int frame = (int) (getTime() / FRAME_TIME);
            if (art.hasSewerArt()) art.sewer.drawSlime(batch, getCenterX(), bounds.y, DRAW_HEIGHT, frame, getDirection() > 0);
            else art.greenhouse.drawSlime(batch, getCenterX(), bounds.y, DRAW_HEIGHT, frame, getDirection() > 0);
            drawCommon(batch);
            return;
        }

        float x = bounds.x, y = bounds.y, w = bounds.width, h = bounds.height;
        float bob = (float) Math.sin(getTime() * 6f) * 3f;
        // spikes on the back
        for (int k = 0; k < 3; k++) {
            art.draw(batch, art.triangleUp, art.theme.hazard, x + 6f + k * 15f, y + h - 8f + bob, 14f, 16f);
        }
        art.draw(batch, art.white, 0.05f, 0.04f, 0.07f, 1f, x, y, w, h - 4f + bob);
        art.draw(batch, art.circle, 0.05f, 0.04f, 0.07f, 1f, x - 6f, y, 20f, 20f);
        art.draw(batch, art.circle, 0.05f, 0.04f, 0.07f, 1f, x + w - 14f, y, 20f, 20f);
        float ex = x + w / 2f + getDirection() * 7f;
        art.draw(batch, art.white, 1f, 1f, 1f, 1f, ex - 14f, y + h - 24f + bob, 9f, 9f);
        art.draw(batch, art.white, 1f, 1f, 1f, 1f, ex + 5f, y + h - 24f + bob, 9f, 9f);
        drawCommon(batch);
    }
}
