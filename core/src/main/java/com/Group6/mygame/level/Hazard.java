package com.Group6.mygame.level;

import com.Group6.mygame.GameObject;
import com.badlogic.gdx.math.Rectangle;

/**
 * Anything in the level that hurts the cat. Every trap extends this class, so {@link LevelWorld}
 * can check all of them with one loop: {@code if (hazard.touches(cat)) ...}. That is polymorphism:
 * a spike, a saw and a fireball each decide for themselves what "touching" means.
 *
 * <pre>
 *   GameObject
 *     └── Hazard ── SpikeStrip, FireHole, FlameVent, FallingSpike, SawBlade, Fireball
 * </pre>
 */
public abstract class Hazard extends GameObject {

    /** What happens to the cat after it loses 1 HP. */
    public enum Effect {
        /** Back to the last checkpoint (pits, spikes, flames, falling spikes). */
        RESPAWN,
        /** Pushed away from the hazard and keeps playing (saws, fireballs). */
        KNOCKBACK
    }

    protected final LevelArt art;

    protected Hazard(LevelArt art, float x, float y, float width, float height) {
        super(x, y, width, height);
        this.art = art;
    }

    /** True if this hazard is dangerous right now and overlaps the cat's hitbox. */
    public abstract boolean touches(Rectangle cat);

    public abstract Effect getEffect();

    /** Horizontal centre of the hazard, used to push the cat away on KNOCKBACK. */
    public float getCenterX() {
        return bounds.x + bounds.width / 2f;
    }

    /** Fireballs return true once they hit a wall; LevelWorld then removes them. */
    public boolean isFinished() {
        return false;
    }

    /** Called when this hazard has just hurt the cat (fireballs use it to disappear). */
    public void onHitCat() {
    }

    /** Small helper shared by the subclasses. */
    protected static boolean overlaps(Rectangle a, float x, float y, float w, float h) {
        return a.x < x + w && a.x + a.width > x && a.y < y + h && a.y + a.height > y;
    }
}
