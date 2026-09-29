package com.Group6.mygame;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class Player extends GameObject {
    private int health = 3;

    public Player(float x, float y) {
        super(x, y, 32, 32);
    }

    @Override
    public void update(float delta) {
        // Member 1 will fill in the keyboard movement and gravity logic here
        bounds.setPosition(position.x, position.y);
    }

    @Override
    public void render(SpriteBatch batch) {
        // Subsequent creation of cat stickers
    }

    // Interface called for member 2 (trap damage)
    public void takeDamage(int damage) {
        this.health -= damage;
        System.out.println("Player took damage! Current HP: " + health);
    }

    public int getHealth() { return health; }
}
