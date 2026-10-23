package com.Group6.mygame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

/**
 * The cat: the one object the person plays.
 *
 * <p>It is built from two design patterns, so this class does not need big if/else chains:
 * <ul>
 *   <li><b>State pattern</b> - how the cat is <em>moving</em> ({@link GroundedState},
 *       {@link JumpingState}, {@link FallingState}, {@link MorphState}, {@link GasState}). The
 *       current {@link PlayerState} runs the frame; see {@link PlayerState} for the diagram.</li>
 *   <li><b>Strategy pattern</b> - what the cat's <em>body</em> is ({@link CatForm}: solid, liquid,
 *       prone, gas). Speeds, height and abilities are read from the current form.</li>
 * </ul>
 *
 * <p>Physics (gravity, AABB collision) comes from {@link DynamicEntity}; this class adds input,
 * form switching, health and the sprite animation.
 */
public class Player extends DynamicEntity {
    public static final float GROUND_ACCEL = 2000f;
    public static final float GROUND_FRICTION = 2400f;
    public static final float AIR_ACCEL = 1200f;
    public static final float AIR_FRICTION = 400f;

    public static final float JUMP_SPEED = 500f;
    public static final float JUMP_CUT_SPEED = 200f;

    /** Hitbox width, the same in every form (only the height changes). */
    private static final float BODY_WIDTH = 90f;

    // Seconds each animation frame stays on screen.
    private static final float WALK_FRAME_TIME = 0.10f;
    private static final float RUN_FRAME_TIME = 0.06f;
    private static final float LIQUID_FRAME_TIME = 0.09f;
    private static final float PRONE_FRAME_TIME = 0.22f;   // a two-pose shuffle: keep it lazy
    private static final float GAS_FRAME_TIME = 0.14f;     // a drift, so slower than a walk

    /** Below this horizontal speed the cat counts as standing still. */
    private static final float MOVING_SPEED_THRESHOLD = 10f;

    /** Where the jump clip splits: frames [0,3) while rising, frames [3,6) while falling. */
    private static final int JUMP_RISE_FRAMES = 3;

    /** Height the picture is drawn at while melting; the liquid hitbox is shorter than the drawing. */
    private static final float MORPH_LIQUID_HEIGHT = 65f;

    /** Drawn height of the lying-down clip (the hitbox is {@link ProneForm#HEIGHT}). */
    private static final float PRONE_DRAW_HEIGHT = 90f;

    /** The two crawl poses inside the prone clip; the cat alternates between them while it moves. */
    private static final int PRONE_CRAWL_FROM = 5;
    private static final int PRONE_CRAWL_TO = 4;

    /** Drawn height of the cloud; see {@link GasForm#HEIGHT} for why the hitbox differs. */
    private static final float GAS_DRAW_HEIGHT = 106.5f;

    /** Which animation clip is playing. A switch resets {@code animTimer} so every clip starts on frame 0. */
    private enum Clip {
        STAND, WALK, RUN, JUMP_RISE, JUMP_FALL, LIQUID, MORPH, PRONE, GAS
    }

    private int health = 3;

    // ----- gas form ----- //
    /** The heat bar. A new Player (a new level) starts cold: heat has to be found in the level. */
    private final HeatMeter heat = new HeatMeter();
    /** What the solid cat would collide with, set by LevelWorld; lets a cloud check it has room to set. */
    private Array<Rectangle> fullSolids = new Array<>();
    private final Rectangle fitProbe = new Rectangle();
    /** Wind this frame (px/s), set by LevelWorld from the fans the cloud is inside. */
    private float windX, windY;
    /** One-line message for the player ("too cold", "no room"); LevelWorld shows and clears it. */
    private String hint;
    /** Set when the cloud had to condense inside solid rock; LevelWorld then sends the cat back. */
    private boolean condensedInWall;

    private final Sprite catSprite;
    private final SpriteFrames standFrames, walkFrames, jumpFrames, liquidWalkFrames, morphFrames, proneFrames;
    private final SpriteFrames gasMorphFrames, gasFloatFrames;

    // Strategy pattern: one object per body; `form` points at the one in use.
    private final CatForm solidForm = new SolidForm();
    private final CatForm liquidForm = new LiquidForm();
    private final CatForm proneForm = new ProneForm();
    private final CatForm gasForm = new GasForm();
    private CatForm form = solidForm;

    private boolean wantsLiquid = false;
    private boolean wantsGas = false;

    // Input / stance (refreshed every frame)
    private boolean running = false;
    private boolean facingLeft = false;
    private boolean moving = false;
    private float moveInput = 0f;       // -1 = left, 0 = none, +1 = right
    private float airSpeed = SolidForm.WALK_SPEED;   // top horizontal speed in the air = the speed we had at takeoff
    private Clip clip;                  // null until the first frame
    private float animTimer = 0;

    // State Pattern: the states hold no data, so each player just owns one of each.
    private final PlayerState groundedState = new GroundedState();
    private final PlayerState jumpingState = new JumpingState();
    private final PlayerState fallingState = new FallingState();
    private final MorphState morphState = new MorphState();
    private final PlayerState gasState = new GasState();
    private PlayerState state;

    /**
     * @param x          spawn x (bottom-left corner of the hitbox)
     * @param y          spawn y
     * @param animations the shared animation clips, loaded once by {@link Main}
     */
    public Player(float x, float y, CatAnimations animations) {
        super(x, y, BODY_WIDTH, SolidForm.HEIGHT);
        standFrames = animations.stand();
        walkFrames = animations.walk();
        jumpFrames = animations.jump();
        liquidWalkFrames = animations.liquidWalk();
        morphFrames = animations.morph();
        proneFrames = animations.prone();
        gasMorphFrames = animations.gasMorph();
        gasFloatFrames = animations.gasFloat();

        catSprite = new Sprite(standFrames.get(0));
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
    private void changeState(PlayerState next) {
        if (state == next) return;
        if (state != null) state.exit(this);
        state = next;
        state.enter(this);
    }

    // Transitions the states ask for. States say WHERE to go; only the Player knows which object that is.

    void toGrounded() {
        changeState(groundedState);
    }

    void toJumping() {
        changeState(jumpingState);
    }

    void toFalling() {
        changeState(fallingState);
    }

    void toGas() {
        changeState(gasState);
    }

    /** Name of the current state, for the debug view. */
    public String getStateName() {
        return state.getClass().getSimpleName();
    }

    // ===================== Form (Strategy Pattern) ===================== //

    public CatForm getForm() {
        return form;
    }

    /** Swaps the body: resizes the hitbox to the new form. Called by {@link MorphState#enter}. */
    void setForm(CatForm next) {
        if (form.canFly() && !next.canFly()) velocity.scl(0.25f);   // a cloud's drift must not launch the cat
        form = next;
        bounds.setSize(BODY_WIDTH, next.height());
        syncBounds();
    }

    /**
     * Reads Q / E / S, decides which form the cat wants to be in and starts a morph if that differs
     * from the current one. Refuses to grow into a space the taller body would not fit.
     */
    private void updateForm() {
        if (state == morphState) return;

        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            wantsLiquid = !wantsLiquid;
            wantsGas = false;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            if (wantsGas) {
                wantsGas = false;
            } else if (!GasProgress.isUnlocked()) {
                hint = "Gas is locked - find the star at the end of The Furnace";
            } else if (!heat.canStart()) {
                hint = "Too cold to puff into gas - warm up at a fire";
            } else {
                wantsGas = true;
                wantsLiquid = false;
            }
        }

        boolean forced = false;
        if (form.canFly() && heat.isEmpty()) {
            wantsGas = false;
            forced = true;
        }

        CatForm want = solidForm;
        if (wantsGas) {
            want = gasForm;
        } else if (wantsLiquid) {
            want = liquidForm;
        } else if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            want = proneForm;
        }

        if (want != solidForm && form != solidForm && want != form) want = solidForm;

        if (want == form) return;

        if (form.canFly()) {
            if (!fitsAsCat(want.height())) {
                if (forced) {
                    condensedInWall = true;
                } else {
                    wantsGas = true;
                    hint = "No room to condense here - fly out of the wall first";
                    return;
                }
            }
        } else if (want.height() > form.height()
            && isBlocked(position.x, position.y, BODY_WIDTH, want.height())) {
            // Shrinking never needs a check; only growing into a space the tall box does not fit.
            return;
        }

        startMorph(want);
    }

    /** Would a cat of this height fit here, against everything a solid cat collides with? */
    private boolean fitsAsCat(float height) {
        fitProbe.set(position.x + 0.01f, position.y + 0.01f, BODY_WIDTH - 0.02f, height - 0.02f);
        for (int i = 0; i < fullSolids.size; i++) {
            if (fitProbe.overlaps(fullSolids.get(i))) return false;
        }
        return true;
    }

    private void startMorph(CatForm target) {
        // Only the puddle has a sound, and a water drop under a cat lying down would be wrong.
        if (target == liquidForm) Audio.morph();
        morphState.begin(form, target);
        changeState(morphState);
    }

    // ===================== Input helpers (used by the states) ===================== //

    boolean isJumpPressed() {
        return form.canJump() && Gdx.input.isKeyJustPressed(Input.Keys.SPACE);
    }

    boolean isJumpHeld() {
        return form.canJump() && Gdx.input.isKeyPressed(Input.Keys.SPACE);
    }

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

    void applyAirControl(float delta) {
        accelerateX(readMoveInput(), airSpeed, AIR_ACCEL, AIR_FRICTION, delta);
    }

    /** Free flight for the cloud: WASD steers, wind from fans adds to it (more so as the heat runs low). */
    void applyGasControl(float delta) {
        float ix = readMoveInput();
        float iy = 0f;
        if (Gdx.input.isKeyPressed(Input.Keys.W)) iy += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) iy -= 1f;
        float length = (float) Math.sqrt(ix * ix + iy * iy);
        if (length > 0f) {
            ix /= length;
            iy /= length;
        }
        float thinness = 1f + (1f - heat.fraction());
        float targetX = ix * GasForm.FLY_SPEED + windX * thinness;
        float targetY = iy * GasForm.FLY_SPEED + windY * thinness;
        velocity.x = approach(velocity.x, targetX, GasForm.FLY_ACCEL * delta);
        velocity.y = approach(velocity.y, targetY, GasForm.FLY_ACCEL * delta);
    }

    /** Speeds up toward {@code input * maxSpeed}, or slows to a stop with friction when no key is held. */
    private void accelerateX(float input, float maxSpeed, float accel, float friction, float delta) {
        if (input == 0f) {
            velocity.x = approach(velocity.x, 0f, friction * delta);
        } else {
            if (Math.signum(velocity.x) == -input) accel += friction;   // turning around: brake and accelerate at once
            velocity.x = approach(velocity.x, input * maxSpeed, accel * delta);
        }
    }

    private static float approach(float value, float target, float maxChange) {
        if (value < target) return Math.min(value + maxChange, target);
        return Math.max(value - maxChange, target);
    }

    private float getGroundMaxSpeed() {
        return running ? form.runSpeed() : form.walkSpeed();
    }

    void startJump() {
        Audio.jump();
        velocity.y = JUMP_SPEED;
        airSpeed = getGroundMaxSpeed();      // 400 if running, 200 if walking, 340 if a running puddle
        onGround = false;
    }

    /** Refreshes the stance flags that ground control needs. */
    private void updateStance() {
        running = onGround && Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT);
    }

    // ===================== Animation ===================== //

    private void updateAnimation(float delta) {
        moving = moveInput != 0f || Math.abs(velocity.x) > MOVING_SPEED_THRESHOLD;

        Clip next = pickClip();
        if (next != clip) {          // a new animation always starts on its first frame
            clip = next;
            animTimer = 0f;
        }
        animTimer += delta;

        switch (clip) {
            case MORPH:
                drawMorphFrame();
                break;
            case LIQUID:
                drawFrame(liquidWalkFrames, moving ? elapsedFrames(LIQUID_FRAME_TIME) : 0, liquidForm.height());
                break;
            case JUMP_RISE:
                drawFrame(jumpFrames, clipFrame(1f - velocity.y / JUMP_SPEED, 0, JUMP_RISE_FRAMES), solidForm.height());
                break;
            case JUMP_FALL:
                drawFrame(jumpFrames,
                    clipFrame(-velocity.y / JUMP_SPEED, JUMP_RISE_FRAMES, jumpFrames.size()), solidForm.height());
                break;
            case PRONE:
                drawFrame(proneFrames, proneCrawlFrame(), PRONE_DRAW_HEIGHT);
                break;
            case GAS:
                drawFrame(gasFloatFrames, elapsedFrames(GAS_FRAME_TIME), GAS_DRAW_HEIGHT, 0);
                break;
            case RUN:
                drawFrame(walkFrames, elapsedFrames(RUN_FRAME_TIME), solidForm.height());
                break;
            case WALK:
                drawFrame(walkFrames, elapsedFrames(WALK_FRAME_TIME), solidForm.height());
                break;
            default:
                drawFrame(standFrames, 0, solidForm.height());
        }

        // A cooling cloud thins out and turns blue: the picture is the second readout of the heat bar.
        float warmth = heat.fraction();
        if (form == gasForm && state != morphState) {
            catSprite.setColor(0.65f + 0.35f * warmth, 0.82f + 0.18f * warmth, 1f, 0.45f + 0.55f * warmth);
        } else {
            catSprite.setColor(1f, 1f, 1f, 1f);
        }
    }

    /** Which animation the cat's situation calls for this frame. */
    private Clip pickClip() {
        if (state == morphState) return Clip.MORPH;
        if (form == gasForm) return Clip.GAS;
        if (form == liquidForm) return Clip.LIQUID;
        if (form == proneForm) return Clip.PRONE;
        if (!onGround) return velocity.y > 0f ? Clip.JUMP_RISE : Clip.JUMP_FALL;
        if (!moving) return Clip.STAND;
        return running ? Clip.RUN : Clip.WALK;
    }

    /** The lying-down cat holds one pose when still and alternates two while it crawls. */
    private int proneCrawlFrame() {
        if (!moving) return PRONE_CRAWL_FROM;
        return elapsedFrames(PRONE_FRAME_TIME) % 2 == 0 ? PRONE_CRAWL_FROM : PRONE_CRAWL_TO;
    }

    private int elapsedFrames(float secondsPerFrame) {
        return (int) (animTimer / secondsPerFrame);
    }
    /** Maps {@code progress} (0..1) onto the frames {@code [from, to)} of a clip. */
    private static int clipFrame(float progress, int from, int to) {
        float p = Math.max(0f, Math.min(progress, 1f));
        int step = Math.min((int) (p * (to - from)), to - from - 1);
        return from + step;
    }
    /** Draws the current morph frame, growing or shrinking the picture from one form's height to the other's. */
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

    /** Which clip plays the morph between the two forms: gas, liquid, or the lying-down one. */
    private SpriteFrames morphClip() {
        CatForm from = morphState.getFrom(), to = morphState.getTo();
        if (from == gasForm || to == gasForm) return gasMorphFrames;
        return (from == liquidForm || to == liquidForm) ? morphFrames : proneFrames;
    }

    /** Height the picture is drawn at for a form. This is the drawing height, not the hitbox height. */
    private float drawnHeightOf(CatForm target) {
        if (target == liquidForm) return MORPH_LIQUID_HEIGHT;
        if (target == proneForm) return PRONE_DRAW_HEIGHT;
        if (target == gasForm) return GAS_DRAW_HEIGHT;
        return target.height();
    }

    /** Draws frame {@code index} of a clip, standing on the hitbox bottom, {@code targetHeight} tall. */
    private void drawFrame(SpriteFrames frames, int index, float targetHeight) {
        drawFrame(frames, index, targetHeight, frames.bottomGap(index));
    }

    /** As above, with an explicit count of transparent rows to sink below the hitbox bottom. */
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

    // ===================== Gas form: what LevelWorld reads and sets ===================== //

    public HeatMeter getHeat() {
        return heat;
    }

    /** The solid-cat collision list; only used to check a cloud has room to condense. */
    public void setFullSolids(Array<Rectangle> solids) {
        this.fullSolids = (solids != null) ? solids : new Array<Rectangle>();
    }

    public void setWind(float x, float y) {
        windX = x;
        windY = y;
    }

    public String pollHint() {
        String h = hint;
        hint = null;
        return h;
    }

    public boolean pollCondensedInWall() {
        boolean b = condensedInWall;
        condensedInWall = false;
        return b;
    }

    // ===================== Health ===================== //

    /** Removes {@code damage} hit points. Called by {@code LevelWorld} when a hazard touches the cat. */
    public void takeDamage(int damage) {
        this.health -= damage;
    }

    public int getHealth() {
        return health;
    }
}
