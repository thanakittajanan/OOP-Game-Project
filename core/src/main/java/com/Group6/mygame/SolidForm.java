package com.Group6.mygame;

/** The cat as a cat: full height, quick on its feet, jumps, and is stopped by bars and grates. */
public class SolidForm implements CatForm {
    public static final float HEIGHT = 90f;
    public static final float WALK_SPEED = 200f;
    public static final float RUN_SPEED = 400f;

    @Override
    public String getName() {
        return "solid";
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
        return true;
    }

    @Override
    public boolean passesBars() {
        return false;
    }

    @Override
    public boolean isHeavy() {
        return true;
    }
}
