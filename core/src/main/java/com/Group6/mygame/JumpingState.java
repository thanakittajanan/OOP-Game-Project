package com.Group6.mygame;

/** The cat is moving upward after a jump. Letting go of the jump key early cuts the jump short. */
public class JumpingState implements PlayerState {
    @Override
    public void enter(Player p) {
    }

    @Override
    public void update(Player p, float delta) {
        p.applyAirControl(delta);

        // Variable jump height: released early -> throw away most of the upward speed.
        if (!p.isJumpHeld() && p.getVelocity().y > Player.JUMP_CUT_SPEED) {
            p.getVelocity().y = Player.JUMP_CUT_SPEED;
        }

        p.applyGravity(delta);
        p.moveAndCollide(delta);

        if (p.isOnGround()) {
            p.changeState(p.groundedState);
        } else if (p.getVelocity().y <= 0f) {   // top of the jump, or the head hit a ceiling (that sets y speed to 0)
            p.changeState(p.fallingState);
        }
    }

    @Override
    public void exit(Player p) {
    }
}
