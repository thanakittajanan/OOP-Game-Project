package com.Group6.mygame;

import com.badlogic.gdx.utils.Disposable;

/**
 * Every animation clip the cat needs, loaded once and shared by all {@link Player} instances.
 *
 * <p>A new {@code Player} is built for each level, but the textures are expensive to load, so
 * {@link Main} creates one {@code CatAnimations} at start-up and hands it to every player. Bundling
 * the clips here keeps {@code Main} and the {@code Player} constructor from carrying eight separate
 * {@link SpriteFrames} fields and parameters.
 *
 * <p>The frames inside a clip share a canvas size, which is what lets {@code Player} hold the cat's
 * size steady while the pose moves inside it - see {@link SpriteFrames}.
 */
public final class CatAnimations implements Disposable {
    private final SpriteFrames stand;
    private final SpriteFrames walk;
    private final SpriteFrames jump;
    private final SpriteFrames liquidWalk;
    private final SpriteFrames morph;
    private final SpriteFrames prone;
    private final SpriteFrames gasMorph;
    private final SpriteFrames gasFloat;

    private CatAnimations(SpriteFrames stand, SpriteFrames walk, SpriteFrames jump, SpriteFrames liquidWalk,
                          SpriteFrames morph, SpriteFrames prone, SpriteFrames gasMorph, SpriteFrames gasFloat) {
        this.stand = stand;
        this.walk = walk;
        this.jump = jump;
        this.liquidWalk = liquidWalk;
        this.morph = morph;
        this.prone = prone;
        this.gasMorph = gasMorph;
        this.gasFloat = gasFloat;
    }

    /** Loads every clip from the assets folder. The result owns the textures and must be disposed. */
    public static CatAnimations load() {
        return new CatAnimations(
            SpriteFrames.load("physical-stand-default.png"),
            SpriteFrames.load(
                "physical-walk-01.png", "physical-walk-02.png", "physical-walk-03.png",
                "physical-walk-04.png", "physical-walk-05.png"),
            // Frames 0-2 are the rise, 3-5 the fall - see Player.JUMP_RISE_FRAMES.
            SpriteFrames.load(
                "physical-jump-01.png", "physical-jump-02.png", "physical-jump-03.png",
                "physical-jump-04.png", "physical-jump-05.png", "physical-jump-06.png"),
            SpriteFrames.load(
                "liquid-walk-01.png", "liquid-walk-02.png", "liquid-walk-03.png",
                "liquid-walk-04.png", "liquid-walk-05.png"),
            SpriteFrames.load(
                "liquid-morph-01.png", "liquid-morph-02.png", "liquid-morph-03.png",
                "liquid-morph-04.png", "liquid-morph-05.png", "liquid-morph-06.png"),
            // One stem for both prone states: frames 0-3 lie the cat down, 4-5 are the crawl - see
            // Player.PRONE_CRAWL_FROM for why the crawl is drawn out of the same clip.
            SpriteFrames.load(
                "physical-prone-01.png", "physical-prone-02.png", "physical-prone-03.png",
                "physical-prone-04.png", "physical-prone-05.png", "physical-prone-06.png"),
            // Two gas clips, cut onto one canvas so a single draw height spans the hand-off: the morph
            // starts on the cat and ends on the cloud, the drift picks up from that same cloud.
            SpriteFrames.load(
                "gas-morph-01.png", "gas-morph-02.png", "gas-morph-03.png",
                "gas-morph-04.png", "gas-morph-05.png", "gas-morph-06.png"),
            // Five, not six: the sheet's last pose is its first one again, drawn to close the loop.
            SpriteFrames.load(
                "gas-float-01.png", "gas-float-02.png", "gas-float-03.png",
                "gas-float-04.png", "gas-float-05.png"));
    }

    public SpriteFrames stand() {
        return stand;
    }

    public SpriteFrames walk() {
        return walk;
    }

    public SpriteFrames jump() {
        return jump;
    }

    public SpriteFrames liquidWalk() {
        return liquidWalk;
    }

    public SpriteFrames morph() {
        return morph;
    }

    public SpriteFrames prone() {
        return prone;
    }

    public SpriteFrames gasMorph() {
        return gasMorph;
    }

    public SpriteFrames gasFloat() {
        return gasFloat;
    }

    @Override
    public void dispose() {
        stand.dispose();
        walk.dispose();
        jump.dispose();
        liquidWalk.dispose();
        morph.dispose();
        prone.dispose();
        gasMorph.dispose();
        gasFloat.dispose();
    }
}
