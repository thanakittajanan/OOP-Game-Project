package com.Group6.mygame;

/**
 * The cat lying flat on the floor: the middle rung between the standing cat and the puddle.
 *
 * <p>{@link #HEIGHT} is where the form earns its place. A tile is 64 px ({@code LevelData.TILE_SIZE}),
 * so at 55 the cat fits through any one-tile gap in the levels - and it is the number the levels were
 * designed around anyway ("90 x 90 standing, 90 x 55 crouching" in {@code LevelData}). It does
 * <em>not</em> slip through bars, so lying down is not a second way to solve a liquid puzzle.
 *
 * <p>55 is also what the artwork measures, which is the point: the lie-down frames are drawn with
 * their canvas scaled to the standing cat's 90, so the flat pose's ink comes out 53.8 px tall. The
 * hitbox is therefore within a pixel of the cat you can see, and the rule the form is meant to follow
 * - "if the gap looks like it fits, it fits" - actually holds. A hitbox much shorter than the drawing
 * would let the cat crawl through gaps its own back is visibly scraping.
 *
 * <p>The trade is {@link #canJump()}: a cat flat on the floor has nothing to push off, so the way out
 * of a low tunnel is to let go of S and stand up first. The crawl is slower than a walk and Shift
 * does nothing, so lying down is never the fast way to cross open ground.
 */
public class ProneForm implements CatForm {
    public static final float HEIGHT = 55f;

    /** The crawl, top speed. One number: {@link #runSpeed()} returns the same, so Shift does nothing. */
    public static final float CRAWL_SPEED = 140f;

    @Override
    public String getName() {
        return "prone";
    }

    @Override
    public float height() {
        return HEIGHT;
    }

    @Override
    public float walkSpeed() {
        return CRAWL_SPEED;
    }

    /** You cannot sprint while flat on the floor. */
    @Override
    public float runSpeed() {
        return CRAWL_SPEED;
    }

    @Override
    public boolean canJump() {
        return false;
    }

    @Override
    public boolean passesBars() {
        return false;
    }

    /**
     * A cat lying down is still a cat: same body, same weight. Pressing a plate flat on the floor is
     * in fact the pose it is most natural in.
     */
    @Override
    public boolean isHeavy() {
        return true;
    }
}
