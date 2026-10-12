package com.Group6.mygame.level;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

/**
 * Spat by a {@link Spitter}. It flies straight at {@link Spitter#FIREBALL_HEIGHT} above the floor:
 * high enough to pass over the crouching (liquid) cat, low enough to hit the standing cat.
 * Bars and grates do not stop it, solid walls do.
 */
public class Fireball extends Hazard {
    public static final float RADIUS = 9f;
    public static final float SPEED = 450f;
    private static final float LIFETIME = 3f;

    private final LevelMap map;
    private final float velocityX;
    private float life = LIFETIME;
    private boolean finished;

    public Fireball(LevelArt art, LevelMap map, float centerX, float centerY, int direction) {
        super(art, centerX - RADIUS, centerY - RADIUS, RADIUS * 2f, RADIUS * 2f);
        this.map = map;
        this.velocityX = direction * SPEED;
    }

    @Override
    public void update(float delta) {
        bounds.x += velocityX * delta;
        position.set(bounds.x, bounds.y);
        life -= delta;
        if (life <= 0f || map.isSolidAt(getCenterX(), bounds.y + RADIUS)) finished = true;
    }

    @Override
    public boolean touches(Rectangle cat) {
        return !finished && cat.overlaps(bounds);
    }

    @Override
    public Effect getEffect() {
        return Effect.KNOCKBACK;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public void onHitCat() {
        finished = true;
    }

    @Override
    public void render(SpriteBatch batch) {
        float cx = getCenterX(), cy = bounds.y + RADIUS;
        art.draw(batch, art.glow, 1f, 0.7f, 0.3f, 0.9f, cx - 30f, cy - 30f, 60f, 60f);
        float trail = Math.signum(velocityX) * -26f;
        art.draw(batch, art.white, 1f, 0.55f, 0.24f, 0.5f, Math.min(cx, cx + trail), cy - 4f, Math.abs(trail), 8f);
        art.draw(batch, art.circle, 1f, 0.95f, 0.82f, 1f, cx - RADIUS, cy - RADIUS, RADIUS * 2f, RADIUS * 2f);
    }
}
