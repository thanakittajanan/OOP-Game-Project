package com.Group6.mygame;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Rectangle;

/**
 * Root of everything that lives in the game world: the cat, hazards and monsters.
 *
 * <p>It owns the two things every object needs - where it is ({@link #position}, bottom-left
 * corner) and the box that collides ({@link #bounds}) - and declares the two calls the game loop
 * makes on every object, {@link #update(float)} and {@link #render(SpriteBatch)}. Each subclass
 * decides what those mean; callers only ever see a {@code GameObject} (polymorphism).
 */
public abstract class GameObject {
    protected final Vector2 position;
    protected final Rectangle bounds;

    public GameObject(float x, float y, float width, float height) {
        this.position = new Vector2(x, y);
        this.bounds = new Rectangle(x, y, width, height);
    }

    /** Advances the object by {@code delta} seconds. */
    public abstract void update(float delta);

    /** Draws the object. The caller has already called {@code batch.begin()}. */
    public abstract void render(SpriteBatch batch);

    public Vector2 getPosition() {
        return position;
    }

    public Rectangle getBounds() {
        return bounds;
    }
}
