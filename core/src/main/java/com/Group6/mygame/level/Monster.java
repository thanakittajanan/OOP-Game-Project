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
    protected final float groundY;       // feet height
    protected final Rectangle cat;       // the cat's hitbox (shared object)

    private final float minX, maxX;      // left edge of the hitbox stays between these
    private int direction = -1;          // -1 = left, +1 = right
    private float time;
    /** Height above the ground right now (only Hoppers leave the ground). */
    private float airHeight;
    /** The map, so a ground walker can look at the floor ahead of it. Null if it never needs to. */
    private final LevelMap map;
    /**
     * True for monsters that walk on their feet and should turn at a ledge rather than step off it.
     *
     * <p>Deliberately opt-in and only set by {@link Blob}. A Hopper calls {@code patrol(0, delta)} while
     * it is on the ground, so flipping its direction there would change which way it launches itself -
     * that would quietly re-route the Hoppers already placed in levels 2 and 3.
     */
    private final boolean edgeAware;

    private final Rectangle hitbox = new Rectangle();

    /**
     * @param centerX    spawn position (centre of the monster)
     * @param groundY    y of the floor it stands on
     * @param range      patrol distance in each direction from the spawn point, in pixels
     */
    protected Monster(LevelArt art, float centerX, float groundY, float width, float height, float range, Rectangle cat) {
        this(art, null, false, centerX, groundY, width, height, range, cat);
    }

    /**
     * Same as above, for a ground walker that looks at the floor ahead of it.
     *
     * @param map       the level, used to find the edge of a platform (may be null)
     * @param edgeAware true to turn around at a ledge instead of stepping off it
     */
    protected Monster(LevelArt art, LevelMap map, boolean edgeAware, float centerX, float groundY,
                      float width, float height, float range, Rectangle cat) {
        super(centerX - width / 2f, groundY, width, height);
        this.art = art;
        this.map = map;
        this.edgeAware = edgeAware;
        this.groundY = groundY;
        this.cat = cat;
        this.minX = centerX - range - width / 2f;
        this.maxX = centerX + range - width / 2f;
        this.time = (float) Math.random() * 2f;   // monsters of the same tier do not move in lockstep
    }

    /** 1, 2 or 3. */
    public abstract int getTier();

    /** -1 while facing left, +1 while facing right. */
    protected int getDirection() {
        return direction;
    }

    /** Turns to face the cat; the Spitter does this before it fires. */
    protected void faceCat() {
        direction = catCenterX() < getCenterX() ? -1 : 1;
    }

    /** Seconds this monster has been alive (plus a random head start), for animation and timing. */
    protected float getTime() {
        return time;
    }

    protected void advanceTime(float delta) {
        time += delta;
    }

    /** Height above the ground right now. */
    protected float getAirHeight() {
        return airHeight;
    }

    protected void setAirHeight(float airHeight) {
        this.airHeight = airHeight;
    }

    /** Moves along the patrol and turns around at either end, or at the edge of a platform. */
    protected void patrol(float speed, float delta) {
        float next = bounds.x + direction * speed * delta;
        if (next < minX) {
            next = minX;
            direction = 1;
        } else if (next > maxX) {
            next = maxX;
            direction = -1;
        } else if (edgeAware && airHeight <= 0f && !groundAhead(next)) {
            next = bounds.x;                 // stop short of the drop and go back the other way
            direction = -direction;
        }
        bounds.x = next;
        bounds.y = groundY + airHeight;
        position.set(bounds.x, bounds.y);
    }

    /**
     * True if there is floor just beyond the monster's leading foot. The patrol ends are authored per
     * monster, so this is a backstop for the case where a range is set too wide for the platform - it
     * keeps a monster from walking off a ledge into a pit or through into the gap above one.
     */
    private boolean groundAhead(float nextX) {
        if (map == null) return true;
        float footX = direction > 0 ? nextX + bounds.width + 2f : nextX - 2f;
        return map.isSolidAt(footX, groundY - 4f);
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
            default: return new Blob(art, map, centerX, groundY, range, cat);
        }
    }

    /** Lets a monster add new hazards (fireballs) to the level without knowing about LevelWorld. */
    public interface HazardSink {
        void add(Hazard hazard);
    }
}
