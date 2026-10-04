package com.Group6.mygame;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/**
 * A {@link GameObject} that moves: it has a velocity, feels gravity and collides with solid blocks.
 *
 * <p>Collision is AABB (axis-aligned boxes), resolved one axis at a time:
 * move on X and push out of any block, then move on Y and push out of any block.
 * A downward hit means we landed ({@link #onGround}); an upward hit means we bumped our head
 * ({@link #hitCeiling}); a sideways hit sets {@link #hitWall}.
 *
 * <p>Movement is cut into small sub-steps ({@link #MAX_STEP} px) so fast movement can never
 * tunnel through a thin platform. Small steps (up to {@link #STEP_HEIGHT} px) are climbed
 * automatically, which gives simple ramps and stairs.
 *
 * <p>Convention: {@code position} is the BOTTOM-LEFT corner of the hitbox, and y goes UP.
 */
public abstract class DynamicEntity extends GameObject {
    /** Pixels per second squared. Negative because y goes up. */
    public static final float GRAVITY = -900f;

    /** Longest distance moved in one collision sub-step (px). */
    protected static final float MAX_STEP = 4f;
    /** Highest step the entity climbs without jumping (px). */
    protected static final float STEP_HEIGHT = 12f;
    /** Touching edges is not an overlap; this also absorbs float rounding. */
    private static final float EPSILON = 0.001f;

    protected final Vector2 velocity = new Vector2();
    protected boolean onGround;
    protected boolean hitCeiling;
    protected boolean hitWall;

    private Array<Rectangle> solids = new Array<>();
    private final Rectangle probe = new Rectangle();   // reusable test box (avoids new objects every frame)

    public DynamicEntity(float x, float y, float width, float height) {
        super(x, y, width, height);
    }

    /** Gives the entity the blocks it must collide with (e.g. from the Tiled collision layer). */
    public void setSolids(Array<Rectangle> solids) {
        this.solids = (solids != null) ? solids : new Array<Rectangle>();
    }

    public Vector2 getVelocity() { return velocity; }
    public boolean isOnGround() { return onGround; }
    public boolean isHitCeiling() { return hitCeiling; }
    public boolean isHitWall() { return hitWall; }

    /** Pulls the entity down. Call once per frame before {@link #moveAndCollide(float)}. */
    protected void applyGravity(float delta) {
        velocity.y += GRAVITY * delta;
    }

    /**
     * Moves by {@code velocity * delta} and resolves collisions.
     * Updates {@code position}, {@code bounds}, {@code onGround}, {@code hitCeiling} and {@code hitWall}.
     */
    protected void moveAndCollide(float delta) {
        boolean wasOnGround = onGround;
        hitWall = false;
        hitCeiling = false;

        moveX(velocity.x * delta, wasOnGround);

        // Walking down a small step should not turn into a fall.
        if (wasOnGround && velocity.y <= 0f) {
            snapToGround();
        }

        onGround = false;           // set to true again only if the Y move lands on something
        moveY(velocity.y * delta);
    }

    // ------------------------------------------------------------------ X axis

    private void moveX(float dx, boolean canStep) {
        int steps = (int) Math.ceil(Math.abs(dx) / MAX_STEP);
        if (steps == 0) return;
        float stepDx = dx / steps;

        for (int i = 0; i < steps; i++) {
            position.x += stepDx;
            syncBounds();

            // indexed loop: libGDX Array iterators can't be nested
            for (int j = 0; j < solids.size; j++) {
                Rectangle s = solids.get(j);
                if (!intersects(bounds, s)) continue;

                if (canStep && tryStepUp(s)) continue;

                // A real wall: push out on the side we came from and stop moving sideways.
                if (stepDx > 0f) {
                    position.x = s.x - bounds.width;
                } else {
                    position.x = s.x + s.width;
                }
                velocity.x = 0f;
                hitWall = true;
                syncBounds();
            }
        }
    }

    /** If the block is a small step and there is room above it, climb onto it. */
    private boolean tryStepUp(Rectangle s) {
        float newY = s.y + s.height;
        float rise = newY - position.y;
        if (rise <= 0f || rise > STEP_HEIGHT) return false;
        if (isBlocked(position.x, newY, bounds.width, bounds.height)) return false;   // no headroom / wall behind
        position.y = newY;
        syncBounds();
        return true;
    }

    /** Moves down onto ground that is at most {@link #STEP_HEIGHT} below the feet (only if nothing is underfoot). */
    private void snapToGround() {
        float bestGap = Float.MAX_VALUE;
        for (int i = 0; i < solids.size; i++) {
            Rectangle s = solids.get(i);
            if (!overlapsHorizontally(s)) continue;
            float gap = position.y - (s.y + s.height);
            if (gap < -EPSILON) continue;                    // block is above our feet
            if (gap <= EPSILON) return;                      // already standing on something
            if (gap <= STEP_HEIGHT && gap < bestGap) bestGap = gap;
        }
        if (bestGap != Float.MAX_VALUE) {
            position.y -= bestGap;
            syncBounds();
        }
    }

    // ------------------------------------------------------------------ Y axis

    private void moveY(float dy) {
        int steps = (int) Math.ceil(Math.abs(dy) / MAX_STEP);
        if (steps == 0) return;
        float stepDy = dy / steps;

        for (int i = 0; i < steps; i++) {
            position.y += stepDy;
            syncBounds();

            for (int j = 0; j < solids.size; j++) {
                Rectangle s = solids.get(j);
                if (!intersects(bounds, s)) continue;

                if (stepDy < 0f) {            // falling: land on top of the block
                    position.y = s.y + s.height;
                    onGround = true;
                } else {                      // rising: bump the head on the underside
                    position.y = s.y - bounds.height;
                    hitCeiling = true;
                }
                velocity.y = 0f;
                syncBounds();
                return;                       // stop moving on Y for this frame
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    /** Copies the position into the hitbox (the hitbox size is controlled by the subclass). */
    protected void syncBounds() {
        bounds.setPosition(position.x, position.y);
    }

    /** True if a box at (x, y, width, height) would overlap any solid. */
    protected boolean isBlocked(float x, float y, float width, float height) {
        probe.set(x, y, width, height);
        for (int i = 0; i < solids.size; i++) {
            if (intersects(probe, solids.get(i))) return true;
        }
        return false;
    }

    private boolean overlapsHorizontally(Rectangle s) {
        return position.x < s.x + s.width - EPSILON && position.x + bounds.width > s.x + EPSILON;
    }

    private static boolean intersects(Rectangle a, Rectangle b) {
        return a.x < b.x + b.width - EPSILON && a.x + a.width > b.x + EPSILON
            && a.y < b.y + b.height - EPSILON && a.y + a.height > b.y + EPSILON;
    }
}
