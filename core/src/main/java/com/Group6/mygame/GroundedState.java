package com.Group6.mygame;

/** The cat is standing on something: walk / run / crouch with ground acceleration and friction. */
public class GroundedState implements PlayerState {
    @Override
    public void enter(Player p) {
        // nothing to set up: landing keeps the horizontal speed, friction handles the rest
    }

    @Override
    public void update(Player p, float delta) {
        p.applyGroundControl(delta);

        boolean jumped = p.isJumpPressed();
        if (jumped) {
            p.startJump();
        }

        p.applyGravity(delta);
        p.moveAndCollide(delta);

        if (jumped) {
            p.changeState(p.jumpingState);
        } else if (!p.isOnGround()) {
            p.changeState(p.fallingState);      // walked off a ledge
        }
    }

    @Override
    public void exit(Player p) {
    }
}
