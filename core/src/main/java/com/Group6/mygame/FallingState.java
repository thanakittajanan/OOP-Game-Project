package com.Group6.mygame;

/** The cat is in the air and moving down (after the top of a jump, or after walking off a ledge). */
public class FallingState implements PlayerState {
    @Override
    public void enter(Player p) {
    }

    @Override
    public void update(Player p, float delta) {
        p.applyAirControl(delta);
        p.applyGravity(delta);
        p.moveAndCollide(delta);

        if (p.isOnGround()) {
            p.changeState(p.groundedState);
        }
    }

    @Override
    public void exit(Player p) {
    }
}
