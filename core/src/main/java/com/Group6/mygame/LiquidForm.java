package com.Group6.mygame;

/**
 * The cat melted into a puddle.
 *
 * <p>At {@link #HEIGHT} it is flatter than the old 55 px crouch, so it still clears every one-tile
 * gap in the levels - and on top of that it slips through bars and grates and under the spitter's
 * fireballs. The trade is {@link #canJump()}: a puddle has nothing to push off, so crossing anything
 * wide or high means setting back into a cat first. That is the whole decision the form is for.
 *
 * <p>It flows faster than the cat walks and slower than the cat runs, so running is still the fast
 * way to travel where the ground is clear.
 */
public class LiquidForm implements CatForm {
    public static final float HEIGHT = 40f;
    public static final float WALK_SPEED = 260f;
    public static final float RUN_SPEED = 340f;

    @Override
    public String getName() {
        return "liquid";
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

    @Override
    public boolean canJump() {
        return false;
    }

    @Override
    public boolean passesBars() {
        return true;
    }
}
