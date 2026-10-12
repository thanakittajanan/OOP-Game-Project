package com.Group6.mygame.level;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

/** A spinning saw that slides back and forth between two points, easing at each end. */
public class SawBlade extends Hazard {
    public static final float RADIUS = 40f;

    private final float startX, startY, endX, endY;
    private final float period, phase;
    private float time;
    private float centerX, centerY;

    /** Points are the saw's centre at each end of its track, in world pixels. */
    public SawBlade(LevelArt art, float startX, float startY, float endX, float endY, float period, float phase) {
        super(art, startX - RADIUS, startY - RADIUS, RADIUS * 2f, RADIUS * 2f);
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.period = period;
        this.phase = phase;
        update(0f);
    }

    @Override
    public void update(float delta) {
        time += delta;
        float k = (1f - (float) Math.cos(2.0 * Math.PI * (time / period + phase))) / 2f;
        centerX = startX + (endX - startX) * k;
        centerY = startY + (endY - startY) * k;
        bounds.setPosition(centerX - RADIUS, centerY - RADIUS);
        position.set(bounds.x, bounds.y);
    }

    @Override
    public boolean touches(Rectangle cat) {
        // circle vs box: closest point of the box to the circle centre
        float nx = Math.max(cat.x, Math.min(centerX, cat.x + cat.width));
        float ny = Math.max(cat.y, Math.min(centerY, cat.y + cat.height));
        float r = RADIUS - 6f;
        return (nx - centerX) * (nx - centerX) + (ny - centerY) * (ny - centerY) < r * r;
    }

    @Override
    public float getCenterX() {
        return centerX;
    }

    @Override
    public Effect getEffect() {
        return Effect.KNOCKBACK;
    }

    @Override
    public void render(SpriteBatch batch) {
        // dotted track
        float len = (float) Math.hypot(endX - startX, endY - startY);
        int dots = Math.max(1, (int) (len / 16f));
        for (int i = 0; i <= dots; i++) {
            float t = i / (float) dots;
            art.draw(batch, art.white, 1f, 1f, 1f, 0.22f,
                startX + (endX - startX) * t - 3f, startY + (endY - startY) * t - 3f, 6f, 6f);
        }
        float size = RADIUS * 2.4f;
        batch.setColor(0.85f, 0.87f, 0.91f, 1f);
        batch.draw(art.saw, centerX - size / 2f, centerY - size / 2f, size / 2f, size / 2f, size, size, 1f, 1f, time * -520f);
        batch.setColor(1f, 1f, 1f, 1f);
    }
}
