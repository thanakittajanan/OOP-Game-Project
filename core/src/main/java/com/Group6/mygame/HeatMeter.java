package com.Group6.mygame;

/**
 * The single progress bar behind the gas form: how much heat the cat is carrying.
 *
 * <p>Gas is only a cloud while it is warm. Flying drains the bar at a fixed rate, fire refills it
 * ({@link com.Group6.mygame.level.Hazard#heatRate()}), and when it hits zero the cloud condenses
 * back into a cat on the spot. The bar is deliberately one number: the whole mechanic is "how much
 * longer can I stay a cloud", and every other rule (thin gas is pushed around by wind, too thin to
 * turn a valve) is read off the same fraction.
 */
public class HeatMeter {
    public static final float MAX = 100f;

    /** Seconds of flight a full bar buys. Everything in level 4 is spaced against this number. */
    public static final float FULL_FLIGHT_SECONDS = 11f;
    public static final float DRAIN_PER_SECOND = MAX / FULL_FLIGHT_SECONDS;

    /** Below this the cat is too cold to puff into gas at all (stops E / instant-condense flicker). */
    public static final float MIN_TO_START = 10f;

    /** Below this fraction the cloud is "thin": it cannot turn a valve and wind pushes it harder. */
    public static final float THIN_FRACTION = 0.2f;

    private float heat;

    public float get() {
        return heat;
    }

    /** 0..1, what the HUD bar draws. */
    public float fraction() {
        return heat / MAX;
    }

    public boolean isEmpty() {
        return heat <= 0f;
    }

    public boolean canStart() {
        return heat >= MIN_TO_START;
    }

    public boolean isThin() {
        return fraction() < THIN_FRACTION;
    }

    public void add(float amount) {
        heat = Math.min(MAX, heat + amount);
    }

    /** Called once per frame while the cat is a cloud. */
    public void drain(float delta) {
        heat = Math.max(0f, heat - DRAIN_PER_SECOND * delta);
    }

    /** Cold air or a respawn: the heat is gone at once. */
    public void empty() {
        heat = 0f;
    }
}
