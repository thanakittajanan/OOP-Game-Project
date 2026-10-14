package com.Group6.mygame;

/**
 * The cat is a cloud: no gravity, free flight in every direction, and the heat bar draining.
 *
 * <p>This is the fourth movement state next to {@link GroundedState}, {@link JumpingState} and
 * {@link FallingState}. It is a state rather than an {@code if (gas)} inside them because nothing
 * about the other three applies: there is no ground to be on and no jump to cut short.
 *
 * <p>Leaving it is not decided here. {@link Player#updateForm} condenses the cloud (E pressed, bar
 * empty, cold air) and the {@link MorphState} that plays hands the cat back to the ground or to the air.
 */
public class GasState implements PlayerState {
    @Override
    public void enter(Player p) {
    }

    @Override
    public void update(Player p, float delta) {
        p.getHeat().drain(delta);
        p.applyGasControl(delta);
        p.moveAndCollide(delta);          // no applyGravity: a cloud does not fall
    }

    @Override
    public void exit(Player p) {
    }
}
