package com.Group6.mygame;

/**
 * The cat is changing form: melting into liquid, or setting back into a cat.
 *
 * <p>This is a state rather than a timer flag on {@link Player} because movement input has to be
 * swallowed while the change plays. {@link GroundedState#update} reads A/D every frame, so a flag
 * would be ignored the moment the cat is back on the ground. Here the input is simply never read,
 * and whatever momentum the cat had carries it through the morph.
 *
 * <p>The hitbox switches to the new form immediately, in {@link #enter} - that is what lets the cat
 * slide into a grate the instant it presses S, and what stops it from standing back up somewhere it
 * does not fit (the player checks the headroom before calling). The <em>picture</em> is drawn at an
 * interpolated height instead, so the change still reads as a melt rather than a hard cut.
 *
 * <p>Gravity and collision keep running the whole time, so morphing in mid-air is allowed and just
 * falls differently.
 */
public class MorphState implements PlayerState {
    /** 6 morph frames at 0.06 s each. */
    public static final float DURATION = 0.36f;

    private float elapsed;
    private CatForm from;
    private CatForm to;

    /** Records the two ends of the morph. Called before {@link Player#changeState}. */
    void begin(CatForm from, CatForm to) {
        this.from = from;
        this.to = to;
        this.elapsed = 0f;
    }

    @Override
    public void enter(Player p) {
        p.setForm(to);
    }

    @Override
    public void update(Player p, float delta) {
        elapsed += delta;
        p.applyGravity(delta);
        p.moveAndCollide(delta);

        if (elapsed >= DURATION) {
            p.changeState(p.isOnGround() ? p.groundedState : p.fallingState);
        }
    }

    @Override
    public void exit(Player p) {
    }

    /** 0 on the first frame, 1 on the last. Drives the morph animation and the drawn height. */
    float progress() {
        return Math.min(elapsed / DURATION, 1f);
    }

    CatForm getFrom() {
        return from;
    }

    CatForm getTo() {
        return to;
    }
}
