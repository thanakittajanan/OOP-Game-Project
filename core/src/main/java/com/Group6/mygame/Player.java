package com.Group6.mygame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;


public class Player extends DynamicEntity {
    public static final float GROUND_ACCEL = 2000f;
    public static final float GROUND_FRICTION = 2400f;
    public static final float AIR_ACCEL = 1200f;
    public static final float AIR_FRICTION = 400f;

    public static final float JUMP_SPEED = 500f;
    public static final float JUMP_CUT_SPEED = 200f;

    private static final float BODY_WIDTH = 90f;


    private static final float WALK_FRAME_TIME = 0.10f;
    private static final float RUN_FRAME_TIME = 0.06f;
    private static final float LIQUID_FRAME_TIME = 0.09f;
    private static final float PRONE_FRAME_TIME = 0.22f;   // a two-pose shuffle: keep it lazy
    private static final float GAS_FRAME_TIME = 0.14f;     // a drift, so slower than a walk
    private static final float MOVING_SPEED_THRESHOLD = 10f;   // below this the cat counts as standing still


    /** Where the jump clip splits: frames [0,3) while rising, frames [3,6) while falling. */
    private static final int JUMP_RISE_FRAMES = 3;

    private static final float MORPH_LIQUID_HEIGHT = 65f;

    /**
     * The height the prone clip's <em>canvas</em> is drawn at - deliberately not {@link ProneForm#HEIGHT}.
     * The drawn height and the hitbox are two different numbers here, exactly as they are at the liquid
     * end of a melt (see {@link #MORPH_LIQUID_HEIGHT}).
     *
     * <p>The six prone frames were cut so that the standing one's ink fills the 216 px canvas. Drawing
     * that canvas at the solid cat's 90 therefore makes the first lie-down frame come out 90 px tall -
     * the same as {@code physical-stand-default} - so pressing S does not make the cat jump in size.
     * The flat last frame lands at 53.8 px on its own, only because the artwork inside the canvas is
     * flatter, and that is the number {@link ProneForm#HEIGHT} is set to.
     */
    private static final float PRONE_DRAW_HEIGHT = 90f;

    /**
     * The two crawl poses inside the prone clip. That clip is one 6-frame stem: 0-3 lie the cat down,
     * 4-5 are the poses it shuffles between once it is flat. The crawl starts on 5 because 5 is the
     * frame the lie-down ends on - starting to crawl must not flick the cat back up a pose.
     */
    private static final int PRONE_CRAWL_FROM = 5;
    private static final int PRONE_CRAWL_TO = 4;

    /**
     * The height the gas clips' <em>canvas</em> is drawn at - both the morph and the drift, because
     * the two were cut onto one 342 x 252 canvas so that a single number keeps them the same size
     * across the hand-off.
     *
     * <p>Chosen so that the morph's first frame comes out at exactly the solid cat's 90 px: the cat
     * presses E and puffs out, it does not jump in size. The finished cloud therefore lands at
     * 104.4 px, and that is {@link GasForm#HEIGHT}.
     */
    private static final float GAS_DRAW_HEIGHT = 106.5f;

    // Which clip is playing; a switch resets animTimer so every animation starts on frame 0.
    private static final int CLIP_STAND = 0;
    private static final int CLIP_WALK = 1;
    private static final int CLIP_RUN = 2;
    private static final int CLIP_JUMP_RISE = 3;
    private static final int CLIP_JUMP_FALL = 4;
    private static final int CLIP_LIQUID = 5;
    private static final int CLIP_MORPH = 6;
    private static final int CLIP_PRONE = 7;
    private static final int CLIP_GAS = 8;

    private int health = 3;

    private final Sprite catSprite;
    private final SpriteFrames standFrames, walkFrames, jumpFrames, liquidWalkFrames, morphFrames, proneFrames;
    private final SpriteFrames gasMorphFrames, gasFloatFrames;


    private final CatForm solidForm = new SolidForm();
    private final CatForm liquidForm = new LiquidForm();
    private final CatForm proneForm = new ProneForm();
    private final CatForm gasForm = new GasForm();
    private CatForm form = solidForm;

    /**
     * The player's wish, not the cat's body: Q and E flip these and {@link #updateForm} morphs the
     * cat to match. They are fields rather than key tests because both keys are toggles - the puddle
     * stays until Q is pressed again, the cloud until E is - which is also what keeps the form keys
     * from fighting over the cat. Turning one on turns the other off; S is the only hold.
     */
    private boolean wantsLiquid = false;
    private boolean wantsGas = false;

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
                  SpriteFrames liquidWalk, SpriteFrames morph, SpriteFrames prone,
                  SpriteFrames gasMorph, SpriteFrames gasFloat) {
        super(x, y, BODY_WIDTH, SolidForm.HEIGHT);
        standFrames = stand;
        walkFrames = walk;
        jumpFrames = jump;
        liquidWalkFrames = liquidWalk;
        morphFrames = morph;
        proneFrames = prone;
        gasMorphFrames = gasMorph;
        gasFloatFrames = gasFloat;

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
     * Reads the three form keys: Q toggles the puddle, E toggles the cloud, S held lies the cat down.
     * Ignored while a morph is playing.
     *
     * <p>Two keys are toggles and one is a hold, which is what stops them fighting: the toggles win
     * outright and S is read only when neither is on, so a cat that wants to crawl presses Q or E
     * back into a cat first. Turning a toggle on turns the other off.
     *
     * <p>Growing only happens where there is room: the {@code isBlocked} test asks whether the taller
     * box would fit at the cat's feet, so a cat whose player lets go of S under a low ceiling stays
     * prone until it crawls out rather than standing up into the ceiling. Because the wish is re-read
     * every frame, it stands up by itself the moment it is clear.
     */
    private void updateForm() {
        if (state == morphState) return;

        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            wantsLiquid = !wantsLiquid;
            wantsGas = false;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            wantsGas = !wantsGas;
            wantsLiquid = false;
        }

        CatForm want = solidForm;
        if (wantsGas) {
            want = gasForm;
        } else if (wantsLiquid) {
            want = liquidForm;
        } else if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            want = proneForm;
        }

        // The only morphs ever drawn start or end on the cat: "into a puddle", "onto the floor",
        // "puffed out". So a morph asked for between two other forms goes through the cat - a puddle
        // told to become gas plays the melt backwards and then the gas clip rather than inventing a
        // puddle-to-gas transition that does not exist. This is also why S needs no guard here.
        if (want != solidForm && form != solidForm && want != form) want = solidForm;

        if (want == form) return;
        // Shrinking never needs a check; only growing into a space the tall box does not fit.
        if (want.height() > form.height()
            && isBlocked(position.x, position.y, BODY_WIDTH, want.height())) return;

        startMorph(want);
    }

    private void startMorph(CatForm target) {
        morphState.begin(form, target);
        changeState(morphState);
    }

    // ===================== Input helpers (used by the states) ===================== //

    /** Jump key. A form with nothing to push off ({@link CatForm#canJump()}) simply reports "no". */
    boolean isJumpPressed() {
        return form.canJump() && Gdx.input.isKeyJustPressed(Input.Keys.SPACE);
    }

    /** Is the jump key still down - used to cut a jump short. Also false for a form that cannot jump. */
    boolean isJumpHeld() {
        return form.canJump() && Gdx.input.isKeyPressed(Input.Keys.SPACE);
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

    //Refreshes the stance flags

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
                drawFrame(jumpFrames, clipFrame(1f - velocity.y / JUMP_SPEED, 0 , JUMP_RISE_FRAMES), solidForm.height());
                break;
            case CLIP_JUMP_FALL:
                drawFrame(jumpFrames,
                    clipFrame(-velocity.y / JUMP_SPEED, JUMP_RISE_FRAMES, jumpFrames.size()), solidForm.height());
                break;
            case CLIP_PRONE:
                drawFrame(proneFrames, proneCrawlFrame(), PRONE_DRAW_HEIGHT);
                break;
            case CLIP_GAS:
                // Always cycling, moving or not: a cloud that freezes mid-puff when the player lets
                // go of D looks broken. It is also the one clip drawn by its canvas bottom - see the
                // four-argument drawFrame.
                drawFrame(gasFloatFrames, elapsedFrames(GAS_FRAME_TIME), GAS_DRAW_HEIGHT, 0);
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
        if (form == gasForm) return CLIP_GAS;
        if (form == liquidForm) return CLIP_LIQUID;
        if (form == proneForm) return CLIP_PRONE;
        if (!onGround) return velocity.y > 0f ? CLIP_JUMP_RISE : CLIP_JUMP_FALL;
        if (!moving) return CLIP_STAND;
        return running ? CLIP_RUN : CLIP_WALK;
    }

    /**
     * Which frame of the prone clip to draw. Standing still, the cat holds the pose the lie-down ended
     * on; crawling, it shuffles between the clip's last two frames and no further, so that
     * {@link #elapsedFrames} counts the <em>crawl</em> and not the whole six-frame stem.
     */
    private int proneCrawlFrame() {
        if (!moving) return PRONE_CRAWL_FROM;
        return elapsedFrames(PRONE_FRAME_TIME) % 2 == 0 ? PRONE_CRAWL_FROM : PRONE_CRAWL_TO;
    }

    /** Frames of the current clip that have gone by; {@link SpriteFrames#get(int)} wraps it into range. */
    private int elapsedFrames(float secondsPerFrame) {
        return (int) (animTimer / secondsPerFrame);
    }
    //
    private static int clipFrame(float progress, int from, int to) {
        float p = Math.max(0f, Math.min(progress, 1f));
        int step = Math.min((int) (p * (to - from)), to - from - 1);
        return from + step;
    }
    //That height is the drawing height, not the hitbox height
    private void drawMorphFrame() {
        float progress = morphState.progress();
        SpriteFrames clip = morphClip();
        int last = clip.size() - 1;
        int index = Math.min((int) (progress * clip.size()), last);
        if (morphState.getTo() == solidForm) index = last - index;   // solidifying = the same frames reversed

        float from = drawnHeightOf(morphState.getFrom());
        float to = drawnHeightOf(morphState.getTo());
        drawFrame(clip, index, from + (to - from) * progress);
    }

    /**
     * Which clip the running morph is played with. Each clip is drawn as "the cat becoming X", so the
     * one that ends in a puddle uses the melt frames, the one that ends flat on the floor uses the
     * prone frames and the one that ends as a cloud uses the gas frames; each is played backwards for
     * the way back - see {@link #drawMorphFrame}. Only one of the two ends can be a non-cat form, so
     * the choice is never ambiguous.
     */
    private SpriteFrames morphClip() {
        CatForm from = morphState.getFrom(), to = morphState.getTo();
        if (from == gasForm || to == gasForm) return gasMorphFrames;
        return (from == liquidForm || to == liquidForm) ? morphFrames : proneFrames;
    }

    /** How tall a form's artwork is drawn - its hitbox, except where the art says otherwise. */
    private float drawnHeightOf(CatForm target) {
        if (target == liquidForm) return MORPH_LIQUID_HEIGHT;
        if (target == proneForm) return PRONE_DRAW_HEIGHT;
        if (target == gasForm) return GAS_DRAW_HEIGHT;
        return target.height();
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
        drawFrame(frames, index, targetHeight, frames.bottomGap(index));
    }

    /**
     * As above, but the caller says how much of the transparent padding under the artwork to cancel.
     *
     * <p>Every clip cancels all of it, which is what puts the feet on {@code position.y}. The gas
     * drift is the exception and passes 0, pinning the canvas bottom instead: its frames were cut
     * with the artist's own layout intact, so the padding under each pose is not a mistake to
     * correct but the drift itself. Cancelling it would leave a cloud that puffs and squashes on the
     * spot, which is exactly what the four lines of arithmetic in {@link SpriteFrames} are there to
     * prevent everywhere else.
     */
    private void drawFrame(SpriteFrames frames, int index, float targetHeight, int bottomGap) {
        Texture picture = frames.get(index);
        float scale = frames.scale(targetHeight);
        float width = picture.getWidth() * scale;
        float height = picture.getHeight() * scale;

        catSprite.setTexture(picture);
        catSprite.setSize(width, height);
        catSprite.setPosition(position.x + BODY_WIDTH / 2f - width / 2f,   // centred on the hitbox
                              position.y - bottomGap * scale);
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
