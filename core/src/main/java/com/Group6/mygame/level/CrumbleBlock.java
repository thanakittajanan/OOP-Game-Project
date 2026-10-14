package com.Group6.mygame.level;

import com.Group6.mygame.GameObject;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

/**
 * One tile of crumbling floor. It is solid until the cat stands on it, then it shakes for
 * {@link #BREAK_DELAY} seconds and falls away. It grows back after {@link #REGROW_DELAY} seconds,
 * but only when the cat is not standing where it would reappear.
 */
public class CrumbleBlock extends GameObject {
    public static final float BREAK_DELAY = 0.6f;
    public static final float REGROW_DELAY = 3f;

    private final Rectangle cat;     // the cat's hitbox (shared object)
    private final LevelArt art;

    private float breakTimer = -1f;  // < 0 means "not triggered"
    private float regrowTimer;
    private boolean broken;
    private boolean catOnGround;

    public CrumbleBlock(LevelArt art, float x, float y, float tileSize, Rectangle cat) {
        super(x, y, tileSize, tileSize);
        this.art = art;
        this.cat = cat;
    }

    /** LevelWorld tells the block each frame whether the cat is standing on something. */
    public void setCatOnGround(boolean onGround) {
        this.catOnGround = onGround;
    }

    public boolean isSolid() {
        return !broken;
    }

    @Override
    public void update(float delta) {
        if (broken) {
            regrowTimer -= delta;
            if (regrowTimer <= 0f && !cat.overlaps(bounds)) {
                broken = false;
                breakTimer = -1f;
            }
            return;
        }
        if (breakTimer < 0f && isCatStandingOnTop()) {
            breakTimer = BREAK_DELAY;
        }
        if (breakTimer >= 0f) {
            breakTimer -= delta;
            if (breakTimer <= 0f) {
                broken = true;
                regrowTimer = REGROW_DELAY;
            }
        }
    }

    private boolean isCatStandingOnTop() {
        float top = bounds.y + bounds.height;
        boolean feetOnTop = Math.abs(cat.y - top) < 3f;
        boolean overlapX = cat.x < bounds.x + bounds.width && cat.x + cat.width > bounds.x;
        return catOnGround && feetOnTop && overlapX;
    }

    @Override
    public void render(SpriteBatch batch) {
        if (broken) return;
        float x = bounds.x, y = bounds.y, s = bounds.width;
        if (breakTimer >= 0f) x += (float) (Math.random() - 0.5) * 5f;   // shake before falling
        // a slab in the top part of the tile, with cracks
        art.draw(batch, art.white, art.theme.tile.r, art.theme.tile.g, art.theme.tile.b, 0.9f, x + 2f, y + 24f, s - 4f, 38f);
        art.draw(batch, art.white, art.theme.edge, x + 2f, y + 56f, s - 4f, 6f);
        art.draw(batch, art.white, 0.06f, 0.08f, 0.11f, 1f, x + 20f, y + 30f, 3f, 26f);
        art.draw(batch, art.white, 0.06f, 0.08f, 0.11f, 1f, x + 42f, y + 38f, 3f, 18f);
    }
}
