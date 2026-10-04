package com.Group6.mygame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * The cat. Hierarchy: GameObject -> DynamicEntity (velocity, gravity, collision) -> Player (input, states, animation).
 *
 * <p>Each frame: {@link #update(float)} works out crouch/run, lets the current {@link PlayerState}
 * handle input + physics, then picks the picture.
 */
public class Player extends DynamicEntity {
    // ===================== Feel: tuning constants (pixels / second, pixels / second²) ===================== //
    public static final float WALK_SPEED = 200f;
    public static final float RUN_SPEED = 400f;
    public static final float CROUCH_SPEED = 80f;

    public static final float GROUND_ACCEL = 2000f;      // speeding up on the ground
    public static final float GROUND_FRICTION = 2400f;   // slowing down on the ground with no key held
    public static final float AIR_ACCEL = 1200f;         // steering in the air
    public static final float AIR_FRICTION = 400f;       // very little drag in the air, so jumps keep momentum

    public static final float JUMP_SPEED = 500f;         // initial upward speed
    public static final float JUMP_CUT_SPEED = 200f;     // upward speed is cut to this when the jump key is released early

    // Hitbox sizes
    private static final float BODY_WIDTH = 90f;
    private static final float STAND_HEIGHT = 90f;
    private static final float CROUCH_HEIGHT = 55f;

    // Animation timing (seconds per frame)
    private static final float WALK_FRAME_TIME = 0.15f;
    private static final float CROUCH_FRAME_TIME = 0.25f;
    private static final float RUN_FRAME_TIME = 0.35f;
    private static final float MOVING_SPEED_THRESHOLD = 10f;   // below this the cat counts as standing still

    private int health = 3;

    private final Sprite catSprite;
    private final Texture pStandTexture, pWalk1Texture, pWalk2Texture, pUpwardTexture, pDownwardTexture,
        ozflow1Texture, ozflow2Texture;

    // Input / stance (refreshed every frame)
    private boolean crouching = false;
    private boolean running = false;
    private boolean facingLeft = false;
    private float moveInput = 0f;       // -1 = left, 0 = none, +1 = right
    private float airSpeed = WALK_SPEED;   // top horizontal speed in the air = the speed we had at takeoff
    private float animTimer = 0;

    // State Pattern: the states hold no data, so each player just owns one of each.
    final PlayerState groundedState = new GroundedState();
    final PlayerState jumpingState = new JumpingState();
    final PlayerState fallingState = new FallingState();
    private PlayerState state;

    public Player(float x, float y, Texture pStand, Texture pWalk1, Texture pWalk2,
                  Texture pUpward, Texture pDownward, Texture ozflow1, Texture ozflow2, Texture ozflow3)
    {
        super(x, y, BODY_WIDTH, STAND_HEIGHT);
        pStandTexture = pStand;
        pWalk1Texture = pWalk1;
        pWalk2Texture = pWalk2;
        pUpwardTexture = pUpward;
        pDownwardTexture = pDownward;
        ozflow1Texture = ozflow1;
        ozflow2Texture = ozflow2;
        // ozflow3 is accepted but not used yet (kept so Main.createPlayer() and FirstScreen don't change)

        catSprite = new Sprite(pStand);
        catSprite.setPosition(x, y);

        // Start in the air: if the spawn point is above the floor the cat drops in,
        // and if it is already on the floor the first frame lands it.
        changeState(fallingState);
    }

    @Override
    public void update(float delta) {
        delta = Math.min(delta, 1 / 30f); // cap: a lag spike must not teleport the cat through the floor

        updateStance();
        state.update(this, delta);        // input, gravity, movement and state changes happen in the state
        updateAnimation(delta);

        // Sprite is centred on the hitbox horizontally, feet on the hitbox bottom
        catSprite.setPosition(position.x + BODY_WIDTH / 2f - catSprite.getWidth() / 2f, position.y);
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

    // ===================== Input helpers (used by the states) ===================== //

    boolean isJumpPressed() { return Gdx.input.isKeyJustPressed(Input.Keys.W); }
    boolean isJumpHeld() { return Gdx.input.isKeyPressed(Input.Keys.W); }

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

    /** Left/right movement on the ground: walk, run or crouch speed, with ground acceleration and friction. */
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

    /** Top horizontal speed on the ground right now (run / crouch / walk). */
    private float getGroundMaxSpeed() {
        if (running && !crouching) return RUN_SPEED;
        if (crouching && !running) return CROUCH_SPEED;
        return WALK_SPEED;
    }

    /** Launches the cat upward and remembers the takeoff speed for air control. */
    void startJump() {
        velocity.y = JUMP_SPEED;
        airSpeed = getGroundMaxSpeed();      // 400 if running, 200 if walking, 80 if crouching
        onGround = false;
    }

    // ===================== Stance (hitbox height) ===================== //

    /** Works out crouching/running and resizes the hitbox. */
    private void updateStance() {
        boolean wantsCrouch = onGround && Gdx.input.isKeyPressed(Input.Keys.S);
        boolean mustStayCrouched = crouching && !wantsCrouch
            && isBlocked(position.x, position.y, BODY_WIDTH, STAND_HEIGHT);   // no headroom to stand up

        crouching = wantsCrouch || mustStayCrouched;
        running = onGround && Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT);

        // Height changes from the top: the feet (position.y) stay where they are.
        bounds.setSize(BODY_WIDTH, crouching ? CROUCH_HEIGHT : STAND_HEIGHT);
    }

    // ===================== Animation ===================== //

    private void updateAnimation(float delta) {
        boolean moving = moveInput != 0f || Math.abs(velocity.x) > MOVING_SPEED_THRESHOLD;

        if (crouching) {
            animTimer += delta;
            boolean useFirstFrame = ((int) (animTimer / CROUCH_FRAME_TIME)) % 2 == 0;
            catSprite.setTexture(useFirstFrame ? ozflow1Texture : ozflow2Texture);
        } else if (running && moving) {
            // no run sprites yet: alternate the two jump pictures
            animTimer += delta;
            boolean useFirstFrame = ((int) (animTimer / RUN_FRAME_TIME)) % 2 == 0;
            catSprite.setTexture(useFirstFrame ? pUpwardTexture : pDownwardTexture);
        } else if (!onGround) {
            catSprite.setTexture(velocity.y > 0 ? pUpwardTexture : pDownwardTexture);
        } else if (moving) {
            animTimer += delta;
            boolean useFirstFrame = ((int) (animTimer / WALK_FRAME_TIME)) % 2 == 0;
            catSprite.setTexture(useFirstFrame ? pWalk1Texture : pWalk2Texture);
        } else {
            animTimer = 0;
            catSprite.setTexture(pStandTexture);
        }

        // Use each picture's real size (no stretching)
        catSprite.setSize(catSprite.getTexture().getWidth(),
                          catSprite.getTexture().getHeight());
        catSprite.setFlip(facingLeft, false);
    }

    public boolean isCrouching() { return crouching; }

    // Interface called for member 2 (trap damage)
    public void takeDamage(int damage) {
        this.health -= damage;
        System.out.println("Player took damage! Current HP: " + health);
    }

    public int getHealth() { return health; }
}
