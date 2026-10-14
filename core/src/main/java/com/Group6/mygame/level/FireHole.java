package com.Group6.mygame.level;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

/** One tile of a burning gap in the floor. The tile map draws the embers; this class adds moving flames. */
public class FireHole extends Hazard {
    private float time;
    private final float seed;

    public FireHole(LevelArt art, float tileX, float tileY, float tileSize) {
        super(art, tileX, tileY, tileSize, tileSize);
        seed = tileX * 0.013f;
    }

    @Override
    public boolean touches(Rectangle cat) {
        return cat.overlaps(bounds);
    }

    @Override
    public float heatRate() {
        return 45f;
    }

    @Override
    public Effect getEffect() {
        return Effect.RESPAWN;
    }

    @Override
    public void update(float delta) {
        time += delta;
    }

    @Override
    public void render(SpriteBatch batch) {
        float top = bounds.y + bounds.height * 0.75f;
        art.draw(batch, art.glow, 1f, 0.55f, 0.2f, 0.45f, bounds.x - 40f, top - 50f, bounds.width + 80f, 110f);
        for (int k = 0; k < 3; k++) {
            float h = 34f + 26f * (0.5f + 0.5f * (float) Math.sin(time * 9f + seed * 100f + k * 2.1f));
            float x = bounds.x + 4f + k * 19f;
            if (k == 1) {
                art.draw(batch, art.flame, 1f, 0.82f, 0.4f, 1f, x, top - 10f, 20f, h);
            } else {
                art.draw(batch, art.flame, 1f, 0.54f, 0.24f, 1f, x, top - 10f, 20f, h);
            }
        }
    }
}
