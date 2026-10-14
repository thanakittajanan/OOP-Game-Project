package com.Group6.mygame;

/**
 * The cat puffed out into a cloud. Fourth form, after {@link SolidForm}, {@link LiquidForm} and
 * {@link ProneForm}.
 *
 * <p>PREVIEW BUILD. There is no flight here yet: E switches the cat to gas and it still walks,
 * falls and gets stopped by walls like anything else. Only the artwork and the hitbox are real.
 * The point of this version is to watch the gas clip play - if the frames read well, the movement
 * rules get designed around them, and if they do not, no level design was wasted on them.
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

    /** Nothing to push off yet. Giving a cloud a jump is a movement decision, not an art one. */
    @Override
    public boolean canJump() {
        return false;
    }

    /** A cloud is stopped by bars exactly like the cat. Only the puddle slips through them. */
    @Override
    public boolean passesBars() {
        return false;
    }
}
