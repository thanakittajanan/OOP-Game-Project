package com.Group6.mygame.level;

import com.Group6.mygame.GameObject;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

/**
 * Base class for every monster. A monster walks back and forth between {@code minX} and {@code maxX}
 * (its patrol) and hurts the cat on contact. The three tiers override {@link #update(float)} to
 * behave differently:
 *
 * <pre>
 *   GameObject
 *     └── Monster
 *           ├── Blob     (tier 1) walks its patrol
 *           ├── Hopper   (tier 2) hops forward, only moves while in the air
 *           └── Spitter  (tier 3) stops and spits fireballs when it sees the cat
 * </pre>
 *
 * Every monster costs 1 HP on contact and knocks the cat back.
 */
public abstract class Monster extends GameObject {
    protected final LevelArt art;
    protected final float minX, maxX;    // left edge of the hitbox stays between these
    protected final float groundY;       // feet height
    protected final Rectangle cat;       // the cat's hitbox (shared object)
    protected int direction = -1;        // -1 = left, +1 = right
    protected float time;
    /** Height above the ground right now (only Hoppers leave the ground). */
    protected float airHeight;

    private final Rectangle hitbox = new Rectangle();

    /**
     * @param centerX    spawn position (centre of the monster)
     * @param groundY    y of the floor it stands on
     * @param range      patrol distance in each direction from the spawn point, in pixels
     */
    protected Monster(LevelArt art, float centerX, float groundY, float width, float height, float range, Rectangle cat) {
        super(centerX - width / 2f, groundY, width, height);
        this.art = art;
        this.groundY = groundY;
        this.cat = cat;
        this.minX = centerX - range - width / 2f;
        this.maxX = centerX + range - width / 2f;
        this.time = (float) Math.random() * 2f;   // monsters of the same tier do not move in lockstep
    }

    /** 1, 2 or 3. */
    public abstract int getTier();

    /** Moves along the patrol and turns around at either end. */
    protected void patrol(float speed, float delta) {
        float next = bounds.x + direction * speed * delta;
        if (next < minX) {
            next = minX;
            direction = 1;
        } else if (next > maxX) {
            next = maxX;
            direction = -1;
        }
        bounds.x = next;
        bounds.y = groundY + airHeight;
        position.set(bounds.x, bounds.y);
    }

    /** Slightly smaller than the picture so brushing past a monster is fair. */
    public Rectangle getHitbox() {
        return hitbox.set(bounds.x + 6f, bounds.y + 4f, bounds.width - 12f, bounds.height - 6f);
    }

    public float getCenterX() {
        return bounds.x + bounds.width / 2f;
    }

    protected float catCenterX() {
        return cat.x + cat.width / 2f;
    }

    /** Draws the shadow (when in the air) and the tier pips above the head. Subclasses draw the body. */
    protected void drawCommon(SpriteBatch batch) {
        if (airHeight > 4f) {
            float w = bounds.width * Math.max(0.4f, 1f - airHeight / 300f);
            art.draw(batch, art.circle, 0f, 0f, 0f, 0.35f, getCenterX() - w / 2f, groundY - 4f, w, 10f);
        }
        Color pip = tierColor(getTier());
        float y = bounds.y + bounds.height + 14f;
        for (int k = 0; k < getTier(); k++) {
            float x = getCenterX() + (k - (getTier() - 1) / 2f) * 14f;
            art.draw(batch, art.circle, pip, x - 4f, y, 8f, 8f);
        }
    }

    public static Color tierColor(int tier) {
        switch (tier) {
            case 2: return Color.valueOf("b48cf2");
            case 3: return Color.valueOf("ffb057");
            default: return Color.valueOf("c9d3e3");
        }
    }

    /** Creates the right monster class for a tier number read from the map. */
    public static Monster create(int tier, LevelArt art, LevelMap map, float centerX, float groundY, float range,
                                 Rectangle cat, HazardSink sink) {
        switch (tier) {
            case 2: return new Hopper(art, centerX, groundY, range, cat);
            case 3: return new Spitter(art, map, centerX, groundY, range, cat, sink);
            default: return new Blob(art, centerX, groundY, range, cat);
        }
    }

    /** Lets a monster add new hazards (fireballs) to the level without knowing about LevelWorld. */
    public interface HazardSink {
        void add(Hazard hazard);
    }
}
