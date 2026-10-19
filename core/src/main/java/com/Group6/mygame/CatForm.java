package com.Group6.mygame;

/**
 * Strategy Pattern: the cat's body - how big it is and what it can do.
 *
 * <p>{@link Player} holds one form at a time and asks it for every number that depends on the shape,
 * instead of testing "am I liquid?" in each method. A third shape later (gas? ice?) is one new class,
 * not an edit to the player.
 *
 * <p>{@link com.Group6.mygame.level.LevelWorld} only needs {@link #passesBars()} and {@link #isHeavy()}:
 * the first decides whether bars and grates go into the collision list this frame, the second whether
 * a pressure plate counts as stepped on.
 */
public interface CatForm {
    /** Name for the debug overlay. */
    String getName();

    /** Hitbox height in pixels. The width is the same in every form, so only this changes. */
    float height();

    /** Top horizontal speed while walking (Shift not held). */
    float walkSpeed();

    /** Top horizontal speed while running (Shift held). */
    float runSpeed();

    /** False for a form with nothing to push off, i.e. one that cannot jump. */
    boolean canJump();

    /** True if the form slips through bars and grates (the {@code '|'} tiles). */
    boolean passesBars();

    /**
     * True if the form has enough weight behind it to hold a pressure plate down (the {@code 'P'} and
     * {@code 'p'} tiles). The real cat and the cat lying flat do; a puddle and a cloud do not.
     *
     * <p>This is what makes a plate a <em>solid-form</em> puzzle rather than a thing any shape can
     * trip over, and it asks the same question {@link #canJump()} does - "is there a body here?" -
     * in the one place where the answer matters again.
     */
    default boolean isHeavy() {
        return false;
    }

    /** True for a form that floats freely (no gravity, W/S move it up and down). Only the cloud. */
    default boolean canFly() {
        return false;
    }

    /**
     * True if the form drifts through ordinary walls and floors. Sealed doors and the edge of the
     * map still stop it - see {@code LevelWorld#rebuildSolids}.
     */
    default boolean passesWalls() {
        return false;
    }
}
