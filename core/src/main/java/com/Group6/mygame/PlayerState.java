package com.Group6.mygame;

/**
 * State Pattern: one object per way the cat can be moving through the world.
 *
 * <p>{@link Player} keeps a reference to the current state and delegates to it, so the same call
 * ({@code state.update(this, delta)}) behaves differently depending on which state object is stored.
 * That is polymorphism: no big if/else chain on "is the cat in the air?".
 *
 * <pre>
 *                  jump pressed
 *    GroundedState ─────────────→ JumpingState
 *       ↑   ↘ walked off ledge        │ velocity.y &lt;= 0
 *       │       ↘                     ↓
 *       └──────── landed ──────── FallingState
 * </pre>
 */
public interface PlayerState {
    /** Called once when the player switches into this state. */
    void enter(Player p);

    /** Called every frame while this state is active: input, gravity, movement and transitions. */
    void update(Player p, float delta);

    /** Called once when the player switches out of this state. */
    void exit(Player p);
}
