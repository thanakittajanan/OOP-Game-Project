package com.Group6.mygame;

/**
 * Strategy Pattern: the cat's body - how big it is and what it can do.
 *
 * <p>{@link Player} holds one form at a time and asks it for every number that depends on the shape,
 * instead of testing "am I liquid?" in each method. A third shape later (gas? ice?) is one new class,
 * not an edit to the player.
 *
 * <p>{@link com.Group6.mygame.level.LevelWorld} only needs {@link #passesBars()}: it decides whether
 * bars and grates go into the collision list this frame.
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
}
