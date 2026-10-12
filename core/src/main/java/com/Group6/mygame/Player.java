package com.Group6.mygame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * The cat. Hierarchy: GameObject -> DynamicEntity (velocity, gravity, collision) -> Player (input, form, states, animation).
 *
 * <p>Each frame: {@link #update(float)} decides which {@link CatForm} the cat is in, lets the current
 * {@link PlayerState} handle input + physics, then picks the picture.
 *
 * <p>Two patterns meet here. The <b>State Pattern</b> owns what the cat is <em>doing</em> (grounded,
 * jumping, falling, morphing); the <b>Strategy Pattern</b> owns what the cat <em>is</em> ({@link CatForm}).
 * Keeping them apart is what lets a puddle jump-less cat still use the same falling state, and a
 * morphing cat still collide like any other entity.
 */
public class Player extends DynamicEntity {
    // ===================== Feel: tuning constants (pixels / second, pixels / second²) ===================== //
    public static final float GROUND_ACCEL = 2000f;      // speeding up on the ground
    public static final float GROUND_FRICTION = 2400f;   // slowing down on the ground with no key held
    public static final float AIR_ACCEL = 1200f;         // steering in the air
    public static final float AIR_FRICTION = 400f;       // very little drag in the air, so jumps keep momentum

    public static final float JUMP_SPEED = 500f;         // initial upward speed (~139 px high)
    public static final float JUMP_CUT_SPEED = 200f;     // upward speed is cut to this when the jump key is released early

    /** Hitbox width - the same in every form, so only the height changes. */
    private static final float BODY_WIDTH = 90f;

    // Animation timing (seconds per frame). The walk and run clips are the same frames, played faster.
    private static final float WALK_FRAME_TIME = 0.10f;
    private static final float RUN_FRAME_TIME = 0.06f;
    private static final float LIQUID_FRAME_TIME = 0.09f;
    private static final float JUMP_FRAME_TIME = 0.08f;
    private static final float MOVING_SPEED_THRESHOLD = 10f;   // below this the cat counts as standing still

    /** Where the jump clip splits: frames [0,3) while rising, frames [3,6) while falling. */
    private static final int JUMP_RISE_FRAMES = 3;

    /**
     * The drawn height the melt clip starts/ends at on the <em>liquid</em> side, instead of
     * {@link LiquidForm#HEIGHT}.
     *
     * <p>Frames are scaled by canvas height, and the two liquid clips do not waste the same amount of
     * canvas: the melt frames sit in a 328x227 canvas, the swim frames in a 410x183 one. Ending the
     * melt at 40 would therefore hand over a puddle two thirds the size of the one the cat becomes a
     * frame later. 65 is the canvas height at which the last melt frame's artwork lines up with
     * {@code liquid-walk-01}, so the hand-off is invisible. The solid end needs no such correction:
     * the stand frame fills its canvas corner to corner.
     */
    private static final float MORPH_LIQUID_HEIGHT = 65f;

    // Which clip is playing; a switch resets animTimer so every animation starts on frame 0.
    private static final int CLIP_STAND = 0;
    private static final int CLIP_WALK = 1;
    private static final int CLIP_RUN = 2;
    private static final int CLIP_JUMP_RISE = 3;
    private static final int CLIP_JUMP_FALL = 4;
    private static final int CLIP_LIQUID = 5;
    private static final int CLIP_MORPH = 6;

    private int health = 3;

    private final Sprite catSprite;
    private final SpriteFrames standFrames, walkFrames, jumpFrames, liquidWalkFrames, morphFrames;

    // Strategy Pattern: exactly one form is active at a time. The two are stateless, so one of each
    // is shared by every player and compared by identity.
    private final CatForm solidForm = new SolidForm();
    private final CatForm liquidForm = new LiquidForm();
    private CatForm form = solidForm;

    // Input / stance (refreshed every frame)
    private boolean running = false;
    private boolean facingLeft = false;
    private boolean moving = false;
    private float moveInput = 0f;       // -1 = left, 0 = none, +1 = right
    private float airSpeed = SolidForm.WALK_SPEED;   // top horizontal speed in the air = the speed we had at takeoff
    private int clip = -1;
    private float animTimer = 0;

    // State Pattern: the states hold no data, so each player just owns one of each.
    final PlayerState groundedState = new GroundedState();
    final PlayerState jumpingState = new JumpingState();
    final PlayerState fallingState = new FallingState();
    final MorphState morphState = new MorphState();
    private PlayerState state;

    public Player(float x, float y, SpriteFrames stand, SpriteFrames walk, SpriteFrames jump,
                  SpriteFrames liquidWalk, SpriteFrames morph) {
        super(x, y, BODY_WIDTH, SolidForm.HEIGHT);
        standFrames = stand;
        walkFrames = walk;
        jumpFrames = jump;
        liquidWalkFrames = liquidWalk;
        morphFrames = morph;

        catSprite = new Sprite(stand.get(0));
        catSprite.setPosition(x, y);

        // Start in the air: if the spawn point is above the floor the cat drops in,
        // and if it is already on the floor the first frame lands it.
        changeState(fallingState);
    }

    @Override
    public void update(float delta) {
        delta = Math.min(delta, 1 / 30f); // cap: a lag spike must not teleport the cat through the floor

        updateForm();
        updateStance();                   // before the state: ground control needs this frame's run flag
        state.update(this, delta);        // input, gravity, movement and state changes happen in the state
        updateAnimation(delta);
    }

    @Override
    public void render(SpriteBatch batch) {
        // The screen calls batch.begin()/end(), so we only draw here
        catSprite.draw(batch);
    }

    // ===================== State Pattern ===================== //

    /** Switches to another state (calls exit on the old one and enter on the new one). */
    void changeState(PlayerState next) {
        if (state == next) return;
        if (state != null) state.exit(this);
        state = next;
        state.enter(this);
    }

    /** Name of the current state, for the debug view. */
    public String getStateName() {
        return state.getClass().getSimpleName();
    }

    // ===================== Form (Strategy Pattern) ===================== //

    public CatForm getForm() {
        return form;
    }

    /**
     * Switches the hitbox to another form. Called by {@link MorphState#enter}; the height changes from
     * the top, so the feet ({@code position.y}) stay where they are and only the head moves.
     */
    void setForm(CatForm next) {
        form = next;
        bounds.setSize(BODY_WIDTH, next.height());
        syncBounds();
    }

    /**
     * Hold S to melt into liquid, release to set back into a cat. Ignored while a morph is playing.
     *
     * <p>Setting back only happens when there is room: the {@code isBlocked} test asks whether the
     * <em>tall</em> box would fit at the cat's feet. A cat that releases S under a low ceiling
     * therefore stays a puddle until it flows out, instead of growing into the wall above it.
     */
    private void updateForm() {
        if (state == morphState) return;

        boolean wantsLiquid = Gdx.input.isKeyPressed(Input.Keys.S);
        if (wantsLiquid && form != liquidForm) {
            startMorph(liquidForm);
        } else if (!wantsLiquid && form != solidForm
            && !isBlocked(position.x, position.y, BODY_WIDTH, solidForm.height())) {
            startMorph(solidForm);
        }
    }

    private void startMorph(CatForm target) {
        morphState.begin(form, target);
        changeState(morphState);
    }

    // ===================== Input helpers (used by the states) ===================== //

    /** Jump key. A form with nothing to push off ({@link CatForm#canJump()}) simply reports "no". */
    boolean isJumpPressed() {
        return form.canJump() && Gdx.input.isKeyJustPressed(Input.Keys.W);
    }

    /** Is the jump key still down - used to cut a jump short. Also false for a form that cannot jump. */
    boolean isJumpHeld() {
        return form.canJump() && Gdx.input.isKeyPressed(Input.Keys.W);
    }

    /** Reads A/D (D wins if both are held) and remembers which way the cat faces. */
    private float readMoveInput() {
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            facingLeft = false;
            moveInput = 1f;
        } else if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            facingLeft = true;
            moveInput = -1f;
        } else {
            moveInput = 0f;
        }
        return moveInput;
    }

    /** Left/right movement on the ground: walk or run speed for the current form, with acceleration and friction. */
    void applyGroundControl(float delta) {
        accelerateX(readMoveInput(), getGroundMaxSpeed(), GROUND_ACCEL, GROUND_FRICTION, delta);
    }

    /** Left/right steering in the air: top speed is the takeoff speed. */
    void applyAirControl(float delta) {
        accelerateX(readMoveInput(), airSpeed, AIR_ACCEL, AIR_FRICTION, delta);
    }

    /** Speeds up toward input * maxSpeed, or slows to a stop with friction when no key is held. */
    private void accelerateX(float input, float maxSpeed, float accel, float friction, float delta) {
        if (input == 0f) {
            velocity.x = approach(velocity.x, 0f, friction * delta);
        } else {
            if (Math.signum(velocity.x) == -input) accel += friction;   // turning around: brake and accelerate at once
            velocity.x = approach(velocity.x, input * maxSpeed, accel * delta);
        }
    }

    /** Moves {@code value} toward {@code target} by at most {@code maxChange} (never overshoots). */
    private static float approach(float value, float target, float maxChange) {
        if (value < target) return Math.min(value + maxChange, target);
        return Math.max(value - maxChange, target);
    }

    /** Top horizontal speed on the ground right now. The form decides the numbers, Shift decides which one. */
    private float getGroundMaxSpeed() {
        return running ? form.runSpeed() : form.walkSpeed();
    }

    /** Launches the cat upward and remembers the takeoff speed for air control. */
    void startJump() {
        velocity.y = JUMP_SPEED;
        airSpeed = getGroundMaxSpeed();      // 400 if running, 200 if walking, 340 if a running puddle
        onGround = false;
    }

    /**
     * Refreshes the stance flags. Only "running" is left: the hitbox height used to be worked out here
     * too, but that now belongs to the form, and the crouch stance it had is gone - holding S melts the
     * cat into a 40 px puddle, which is shorter than the old 55 px crouch and passes bars as well.
     */
    private void updateStance() {
        running = onGround && Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT);
    }

    // ===================== Animation ===================== //

    private void updateAnimation(float delta) {
        moving = moveInput != 0f || Math.abs(velocity.x) > MOVING_SPEED_THRESHOLD;

        int next = pickClip();
        if (next != clip) {          // a new animation always starts on its first frame
            clip = next;
            animTimer = 0f;
        }
        animTimer += delta;

        switch (clip) {
            case CLIP_MORPH:
                drawMorphFrame();
                break;
            case CLIP_LIQUID:
                drawFrame(liquidWalkFrames, moving ? elapsedFrames(LIQUID_FRAME_TIME) : 0, liquidForm.height());
                break;
            case CLIP_JUMP_RISE:
                drawFrame(jumpFrames, elapsedFrames(JUMP_FRAME_TIME) % JUMP_RISE_FRAMES, solidForm.height());
                break;
            case CLIP_JUMP_FALL:
                drawFrame(jumpFrames,
                    JUMP_RISE_FRAMES + elapsedFrames(JUMP_FRAME_TIME) % (jumpFrames.size() - JUMP_RISE_FRAMES),
                    solidForm.height());
                break;
            case CLIP_RUN:
                drawFrame(walkFrames, elapsedFrames(RUN_FRAME_TIME), solidForm.height());
                break;
            case CLIP_WALK:
                drawFrame(walkFrames, elapsedFrames(WALK_FRAME_TIME), solidForm.height());
                break;
            default:
                drawFrame(standFrames, 0, solidForm.height());
        }
    }

    /** Which animation the cat's situation calls for this frame. */
    private int pickClip() {
        if (state == morphState) return CLIP_MORPH;
        if (form == liquidForm) return CLIP_LIQUID;
        if (!onGround) return velocity.y > 0f ? CLIP_JUMP_RISE : CLIP_JUMP_FALL;
        if (!moving) return CLIP_STAND;
        return running ? CLIP_RUN : CLIP_WALK;
    }

    /** Frames of the current clip that have gone by; {@link SpriteFrames#get(int)} wraps it into range. */
    private int elapsedFrames(float secondsPerFrame) {
        return (int) (animTimer / secondsPerFrame);
    }

    /**
     * The melt clip is special twice over. It plays backwards when the cat sets back into solid, and
     * the height it is drawn at slides between the two forms, so the change reads as a melt rather
     * than as a cut between a 90 px cat and a 40 px puddle.
     *
     * <p>That height is the drawing height, not the hitbox height - see {@link #MORPH_LIQUID_HEIGHT}
     * for why the liquid end is 65 rather than {@link LiquidForm#HEIGHT}.
     */
    private void drawMorphFrame() {
        float progress = morphState.progress();
        int last = morphFrames.size() - 1;
        int index = Math.min((int) (progress * morphFrames.size()), last);
        if (morphState.getTo() == solidForm) index = last - index;   // solidifying = the same frames reversed

        float from = drawnHeightOf(morphState.getFrom());
        float to = drawnHeightOf(morphState.getTo());
        drawFrame(morphFrames, index, from + (to - from) * progress);
    }

    /** How tall a form's artwork is drawn - the same as its hitbox, except at the melting end. */
    private float drawnHeightOf(CatForm target) {
        return target == liquidForm ? MORPH_LIQUID_HEIGHT : target.height();
    }

    /**
     * Draws one frame so the cat is always the same size and always standing on the floor.
     *
     * <p>Both rules come from measurements {@link SpriteFrames} took at load time, and both are needed:
     * the picture is scaled so it is {@code targetHeight} tall, and it is shifted down by the
     * transparent rows under the artwork so the cat's <em>feet</em>, not the canvas edge, land on
     * {@code position.y}. The frames were exported on canvases with 4-63 px of padding under the cat,
     * so without the second rule it visibly floats on some frames and not others.
     *
     * <p>The picture is centred on the hitbox and is wider than it: the stand frame comes out 129 px
     * wide against a 90 px box. That overhang is deliberate - the hitbox is what the world collides
     * with, the artwork is what the player sees, and a cat squeezed into its own collision box does
     * not look like a cat.
     */
    private void drawFrame(SpriteFrames frames, int index, float targetHeight) {
        Texture picture = frames.get(index);
        float scale = frames.scale(targetHeight);
        float width = picture.getWidth() * scale;
        float height = picture.getHeight() * scale;

        catSprite.setTexture(picture);
        catSprite.setSize(width, height);
        catSprite.setPosition(position.x + BODY_WIDTH / 2f - width / 2f,   // centred on the hitbox
                              position.y - frames.bottomGap(index) * scale);
        catSprite.setFlip(facingLeft, false);
    }

    // ===================== Interface called for member 2 (trap damage) ===================== //

    public void takeDamage(int damage) {
        this.health -= damage;
        System.out.println("Player took damage! Current HP: " + health);
    }

    public int getHealth() {
        return health;
    }
}
