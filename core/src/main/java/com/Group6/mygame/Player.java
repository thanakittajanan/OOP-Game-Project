package com.Group6.mygame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class Player extends GameObject {
    private int health = 3;
    private final Sprite catSprite;
    private final Texture pStandTexture, pWalk1Texture, pWalk2Texture, pUpwardTexture, pDownwardTexture,
        ozflow1Texture, ozflow2Texture, ozflow3Texture;

    //Cat attribute
    // Crouch
    private float crouchTimer = 0;
    private final float crouchFrameTime = 0.12f;   // how long ozflow1 shows before ozflow2

    // Hitbox sizes
    private static final float BODY_WIDTH = 139f;
    private static final float STAND_HEIGHT = 95f;
    private static final float CROUCH_HEIGHT = 50f;

    private float animTimer = 0;
    private final float frameTime = 0.15f;   // seconds per walk frame
    private boolean facingLeft = false;

    // Gravity
    float velocityY = 0;
    final float gravity = -900f;      // pixels/sec², pulls down
    final float jumpSpeed = 500f;     // pixels/sec, initial upward push
    final float groundY = 1f;         // y of the floor
    boolean onGround = true;


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
        ozflow3Texture = ozflow3;

        catSprite = new Sprite(pStand);
        catSprite.setSize(139, 95);      // same size as bounds
        catSprite.setPosition(x, y);
    }

    @Override
    public void update(float delta) {
        // Member 1: change position.x / position.y here (keyboard movement, gravity)
        delta = Gdx.graphics.getDeltaTime();
        boolean crouching = onGround && Gdx.input.isKeyPressed(Input.Keys.S);
        float speed = crouching ? 80f : 200f; // if crouching is true, speed is 80; otherwise it's 200." It's the same as:
        boolean moving = false;


        // =================== Left/Right Section ===================== //
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            position.x += speed * delta;
            moving = true;
            facingLeft = false;
        } else if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            position.x -= speed * delta;
            moving = true;
            facingLeft = true;
        }

        // ================= Jump Section =================== //
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && onGround && !crouching) {
            velocityY = jumpSpeed;
            onGround = false;
        }
        // Reduce VelocityY and PositionY
        velocityY += gravity * delta;
        position.y += velocityY * delta;
        // Land on the floor
        if (position.y <= groundY)
        {
            position.y = groundY;
            velocityY = 0;
            onGround = true;
        }

        // ============= Choose the texture Section =============== //
        if (!crouching) crouchTimer = 0;
        if (crouching) {
            crouchTimer += delta;
            animTimer += delta;
            boolean useFirstFrame = ((int) (animTimer / (frameTime + 0.1f) )) % 2 == 0;
            // ozflow1 first, then hold ozflow2 while S stays down
            catSprite.setTexture(crouchTimer < crouchFrameTime ? ozflow1Texture : ozflow2Texture);
            catSprite.setTexture(useFirstFrame ? ozflow1Texture : ozflow2Texture);
        } else if (!onGround && velocityY > 0) {
            catSprite.setTexture(pUpwardTexture);
        } else if (!onGround && velocityY < 0) {
            catSprite.setTexture(pDownwardTexture);
        } else if (moving) {
            animTimer += delta;
            boolean useFirstFrame = ((int) (animTimer / frameTime)) % 2 == 0;
            catSprite.setTexture(useFirstFrame ? pWalk1Texture : pWalk2Texture);
        } else {
            animTimer = 0;
            catSprite.setTexture(pStandTexture);
        }
        catSprite.setFlip(facingLeft, false);







        // Keep bounds and sprite in sync with position
        float h = crouching ? CROUCH_HEIGHT : STAND_HEIGHT ;
        bounds.setPosition(position.x, position.y);
        catSprite.setPosition(position.x, position.y);
    }



    @Override
    public void render(SpriteBatch batch) {
        // The screen calls batch.begin()/end(), so we only draw here
        catSprite.draw(batch);

    }



    // Interface called for member 2 (trap damage)
    public void takeDamage(int damage) {
        this.health -= damage;
        System.out.println("Player took damage! Current HP: " + health);
    }

    public int getHealth() { return health; }
}
