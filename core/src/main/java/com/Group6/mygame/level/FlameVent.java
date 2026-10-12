package com.Group6.mygame.level;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

/**
 * A vent in the floor that shoots flames upward on a fixed rhythm.
 * Cycle: 0.9 s burning, 0.9 s off, then 0.4 s of warning flicker before it burns again.
 * Neighbouring vents are offset by 0.7 s so they are out of sync.
 */
public class FlameVent extends Hazard {
    public static final float CYCLE = 2.2f;
    public static final float BURN_TIME = 0.9f;
    public static final float WARN_FROM = 1.8f;

    private final float offset;
    private float time;

    /**
     * @param flameHeight how high the flame reaches (2 tiles normally, less under a low ceiling)
     * @param index       which vent this is in the level, used to put vents out of sync
     */
    public FlameVent(LevelArt art, float tileX, float tileY, float tileSize, float flameHeight, int index) {
        super(art, tileX + 10f, tileY + tileSize, tileSize - 20f, flameHeight);
        this.offset = index * 0.7f;
    }

    private float phase() {
        return (time + offset) % CYCLE;
    }

    public boolean isBurning() {
        return phase() < BURN_TIME;
    }

    public boolean isWarning() {
        return phase() > WARN_FROM;
    }

    @Override
    public boolean touches(Rectangle cat) {
        return isBurning() && cat.overlaps(bounds);
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
        if (isBurning()) {
            float flicker = 0.85f + 0.15f * (float) Math.sin(time * 30f);
            art.draw(batch, art.glow, 1f, 0.6f, 0.25f, 0.5f, bounds.x - 40f, bounds.y - 20f, bounds.width + 80f, bounds.height + 60f);
            art.draw(batch, art.flame, 1f, 0.54f, 0.24f, flicker, bounds.x, bounds.y, bounds.width, bounds.height);
            art.draw(batch, art.flame, 1f, 0.85f, 0.4f, 1f, bounds.x + bounds.width * 0.2f, bounds.y,
                bounds.width * 0.6f, bounds.height * 0.7f * flicker);
        } else if (isWarning()) {
            float a = 0.35f + 0.25f * (float) Math.sin(time * 40f);
            art.draw(batch, art.white, 1f, 0.67f, 0.31f, a, bounds.x + 6f, bounds.y, bounds.width - 12f, 12f);
        }
    }
}
