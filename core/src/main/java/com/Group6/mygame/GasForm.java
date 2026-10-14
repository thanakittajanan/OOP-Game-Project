package com.Group6.mygame;

/**
 * The cat puffed out into a cloud. Fourth form, after {@link SolidForm}, {@link LiquidForm} and
 * {@link ProneForm}.
 *
 * <p>A cloud floats in every direction ({@link GasState}), drifts through walls and floors, and is
 * immune to spikes, saws and monsters. It pays for that with the {@link HeatMeter}: the bar drains
 * while it flies, fire refills it, and at zero the cloud condenses back into a cat wherever it is
 * (inside a wall that is fatal, over a pit it is a fall). Cold air condenses it at once.
 *
 * <p>It is locked until the star at the end of level 3 is collected ({@link GasProgress}).
 */
public class GasForm implements CatForm {
    /**
     * The drawn height of the gas artwork, not a designed number: the frames were cut so that the
     * standing pose they start from comes out at exactly {@code SolidForm.HEIGHT}, which lands the
     * finished cloud at 104.4 px - see {@code Player.GAS_DRAW_HEIGHT}.
     */
    public static final float HEIGHT = 104f;

    public static final float WALK_SPEED = 200f;
    public static final float RUN_SPEED = 260f;

    /** Top speed in the air, in any direction. Level 4 distances are measured in seconds of this. */
    public static final float FLY_SPEED = 230f;
    public static final float FLY_ACCEL = 1400f;

    @Override
    public String getName() {
        return "gas";
    }

    @Override
    public float height() {
        return HEIGHT;
    }

    @Override
    public float walkSpeed() {
        return WALK_SPEED;
    }

    @Override
    public float runSpeed() {
        return RUN_SPEED;
    }

    /** Nothing to push off: a cloud flies instead (W / S). */
    @Override
    public boolean canJump() {
        return false;
    }

    /** Bars are just more wall to a cloud; {@link #passesWalls()} covers them. */
    @Override
    public boolean passesBars() {
        return false;
    }

    @Override
    public boolean canFly() {
        return true;
    }

    @Override
    public boolean passesWalls() {
        return true;
    }
}
