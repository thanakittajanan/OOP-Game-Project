package com.Group6.mygame.level;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

/**
 * Tier 3. Patrols slowly. When the cat is in front of it at the same height and within
 * {@link #SIGHT_RANGE}, it stops, its mouth glows for {@link #WINDUP} seconds, then it spits a
 * {@link Fireball}. It reloads in {@link #RELOAD} seconds.
 *
 * <p>Fireballs fly at {@link #FIREBALL_HEIGHT} above the floor: they hit the standing cat (90 px tall)
 * and pass over the crouching / liquid cat (55 px tall). So melting is a way to dodge.
 */
public class Spitter extends Monster {
    public static final float SPEED = 70f;
    public static final float SIGHT_RANGE = 12 * 64f;
    public static final float SIGHT_HEIGHT = 100f;
    public static final float WINDUP = 0.5f;
    public static final float RELOAD = 2f;
    public static final float FIREBALL_HEIGHT = 75f;

    private final LevelMap map;
    private final HazardSink sink;
    private float windup;
    private float reload = 1f;

    public Spitter(LevelArt art, LevelMap map, float centerX, float groundY, float range, Rectangle cat, HazardSink sink) {
        super(art, centerX, groundY, 64f, 72f, range, cat);
        this.map = map;
        this.sink = sink;
    }

    @Override
    public int getTier() {
        return 3;
    }

    private boolean canSeeCat() {
        return Math.abs(catCenterX() - getCenterX()) < SIGHT_RANGE
            && Math.abs(cat.y - groundY) < SIGHT_HEIGHT;
    }

    @Override
    public void update(float delta) {
        time += delta;
        reload -= delta;

        if (windup > 0f) {                       // mouth is glowing: stand still, then fire
            windup -= delta;
            if (windup <= 0f) {
                sink.add(new Fireball(art, map, getCenterX() + direction * 34f, groundY + FIREBALL_HEIGHT, direction));
                reload = RELOAD;
            }
            patrol(0f, delta);
        } else if (canSeeCat()) {                 // turn to face the cat and start the wind-up when reloaded
            direction = catCenterX() < getCenterX() ? -1 : 1;
            if (reload <= 0f) windup = WINDUP;
            patrol(0f, delta);
        } else {
            patrol(SPEED, delta);
        }
    }

    @Override
    public void render(SpriteBatch batch) {
        float x = bounds.x, y = bounds.y, w = bounds.width, h = bounds.height;
        // horns
        art.draw(batch, art.triangleUp, art.theme.hazard, x + 2f, y + h - 6f, 16f, 22f);
        art.draw(batch, art.triangleUp, art.theme.hazard, x + w - 18f, y + h - 6f, 16f, 22f);
        // body
        art.draw(batch, art.white, 0.07f, 0.03f, 0.03f, 1f, x, y, w, h);
        // eyes
        float ex = x + w / 2f + direction * 8f;
        art.draw(batch, art.white, 1f, 0.82f, 0.4f, 1f, ex - 18f, y + 40f, 10f, 7f);
        art.draw(batch, art.white, 1f, 0.82f, 0.4f, 1f, ex + 8f, y + 40f, 10f, 7f);
        // mouth: dim normally, bright while winding up
        float mouthX = direction > 0 ? x + w - 28f : x + 4f;
        float mouthY = y + FIREBALL_HEIGHT - 6f;   // the mouth sits where the fireball comes out
        if (windup > 0f) {
            art.draw(batch, art.glow, 1f, 0.7f, 0.35f, 0.8f, mouthX - 30f, mouthY - 30f, 84f, 74f);
            art.draw(batch, art.white, 1f, 0.62f, 0.26f, 1f, mouthX, mouthY, 24f, 12f);
        } else {
            art.draw(batch, art.white, 1f, 0.62f, 0.26f, 0.4f, mouthX, mouthY, 24f, 12f);
        }
        drawCommon(batch);
    }
}
