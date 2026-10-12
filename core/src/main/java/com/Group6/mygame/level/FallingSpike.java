package com.Group6.mygame.level;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

/**
 * A spike hanging from the ceiling. When the cat walks underneath it shakes for a moment, then drops.
 * It breaks on the floor and grows back after a short wait.
 *
 * <p>Small State Pattern of its own: IDLE → SHAKING → FALLING → GONE → IDLE.
 */
public class FallingSpike extends Hazard {
    private enum State { IDLE, SHAKING, FALLING, GONE }

    private static final float WIDTH = 40f;
    private static final float HEIGHT = 56f;
    private static final float TRIGGER_DISTANCE = 110f;   // how close (horizontally) the cat must be
    private static final float SHAKE_TIME = 0.3f;
    private static final float REGROW_TIME = 2.2f;
    private static final float GRAVITY = -1800f;

    private final LevelMap map;
    private final float homeY;
    private final Rectangle target;   // the cat's hitbox (shared object, always up to date)
    private State state = State.IDLE;
    private float timer;
    private float speed;

    /** {@code ceilingY} is the underside of the block the spike hangs from. */
    public FallingSpike(LevelArt art, LevelMap map, float centerX, float ceilingY, Rectangle target) {
        super(art, centerX - WIDTH / 2f, ceilingY - HEIGHT, WIDTH, HEIGHT);
        this.map = map;
        this.homeY = ceilingY - HEIGHT;
        this.target = target;
    }

    @Override
    public void update(float delta) {
        switch (state) {
            case IDLE:
                float catCenter = target.x + target.width / 2f;
                if (Math.abs(catCenter - getCenterX()) < TRIGGER_DISTANCE && target.y < bounds.y) {
                    state = State.SHAKING;
                    timer = SHAKE_TIME;
                }
                break;
            case SHAKING:
                timer -= delta;
                if (timer <= 0f) {
                    state = State.FALLING;
                    speed = 0f;
                }
                break;
            case FALLING:
                speed += GRAVITY * delta;
                bounds.y += speed * delta;
                if (bounds.y < 0f || map.isSolidAt(getCenterX(), bounds.y)) {
                    state = State.GONE;
                    timer = REGROW_TIME;
                }
                break;
            case GONE:
                timer -= delta;
                if (timer <= 0f) {
                    state = State.IDLE;
                    bounds.y = homeY;
                }
                break;
        }
        position.set(bounds.x, bounds.y);
    }

    @Override
    public boolean touches(Rectangle cat) {
        return state == State.FALLING && overlaps(cat, bounds.x + 8f, bounds.y, bounds.width - 16f, bounds.height);
    }

    @Override
    public Effect getEffect() {
        return Effect.RESPAWN;
    }

    @Override
    public void render(SpriteBatch batch) {
        art.draw(batch, art.white, 1f, 1f, 1f, 0.3f, bounds.x + 6f, homeY + HEIGHT - 4f, WIDTH - 12f, 4f);   // mark on the ceiling
        if (state == State.GONE) return;
        float jitter = state == State.SHAKING ? (float) (Math.random() - 0.5) * 5f : 0f;
        art.draw(batch, art.triangleDown, Color.valueOf("e9eef7"), bounds.x + jitter, bounds.y, bounds.width, bounds.height);
    }
}
