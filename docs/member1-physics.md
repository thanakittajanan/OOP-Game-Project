# Member 1 — Physics, Core Movement & Mechanics (Pete)

Part of **Polymorphic Cat**, a 2D platformer built with Java and libGDX (package `com.Group6.mygame`).

This document covers everything owned by Member 1: how the cat moves, how it collides with the world, and how the other members can plug their work into it.

---

## 1. Responsibilities

| Area | Goal |
|---|---|
| Core mechanics | Walking, running, crouching, jumping, variable jump height, gravity, acceleration |
| Collision | Player vs. ground, walls, ceilings and ramps with no clipping or sticking |
| Base architecture | Abstract classes `GameObject` and `DynamicEntity` |
| OOP pattern | **State Pattern** for player physics states (Grounded, Jumping, Falling) |

---

## 2. Current Status

Legend: ✅ done · 🔧 in progress · ⬜ planned

| Task | Status | Notes |
|---|---|---|
| `GameObject` abstract base class | ✅ | `position`, `bounds`, abstract `update()` / `render()` |
| `DynamicEntity` (velocity, gravity, `onGround`) | ✅ | Also owns the collision code, so any moving thing can reuse it |
| Walk / run / crouch | ✅ | Speeds are reached with acceleration, not instantly |
| Jump + gravity | ✅ | Real collision now; the temporary `groundY` floor is gone |
| Frame-rate safe movement | ✅ | `delta` is capped at 1/30 s and movement is split into 4 px sub-steps |
| Hitbox follows crouching | ✅ | Hitbox height switches between standing and crouching |
| Sprite drawn at real size, centred on hitbox | ✅ | No stretching |
| Debug view (solids, green hitbox, state text) | ✅ | Drawn in `LevelScreen`, toggled by `DEBUG` |
| AABB collision: floor | ✅ | Landing sets `onGround` |
| AABB collision: walls and ceilings | ✅ | Walls stop the cat cleanly; head bumps cancel upward speed |
| Ramps / stairs (step-up) | ✅ | Steps up to 12 px are climbed and descended without jumping |
| Crouch headroom | ✅ | The cat stays crouched under a low ceiling until it can stand |
| Acceleration and friction | ✅ | Separate ground and air values |
| Variable jump height | ✅ | Release `W` early for a short hop |
| State Pattern (Grounded / Jumping / Falling) | ✅ | `PlayerState` interface + three state classes |

> Update this table as each task is finished.

---

## 3. Controls

| Key | Action |
|---|---|
| `A` / `D` | Move left / right |
| `W` | Jump (tap = short hop once variable jump is added) |
| `S` | Crouch (slower, smaller hitbox) |
| `Left Shift` | Run (while on the ground) |

Top speeds: walk `200`, run `400`, crouch `80` (pixels per second). The cat speeds up and slows down smoothly. In the air you can still steer, but the top speed is the one you had at takeoff (so a crouch-jump stays slow and a running jump stays fast).

---

## 4. Files

```
core/src/main/java/com/Group6/mygame/
├── GameObject.java     abstract base: position + hitbox (bounds)
├── DynamicEntity.java  abstract: velocity, gravity, onGround, AABB collision, step-up
├── Player.java         input, tuning constants, state switching, animation
├── PlayerState.java    interface for the State Pattern (enter / update / exit)
├── GroundedState.java  standing / walking / running / crouching
├── JumpingState.java   moving upward after a jump (variable jump height)
├── FallingState.java   in the air and moving down
├── PlayerActor.java    bridge that lets Player live inside a scene2d Stage
└── LevelScreen.java    test level: placeholder solids + debug drawing
```

### Class hierarchy

```
GameObject            position, bounds (everything in the world)
    └── DynamicEntity velocity, gravity, onGround, collision (things that move)
            └── Player input, states, animation
```

A spike or wall is a `GameObject` but not a `DynamicEntity`, because it never moves.

---

## 5. How the Game Loop Reaches the Player

```
LevelScreen.render(delta)
  └─ BaseScreen.render → stage.act(delta)
        └─ PlayerActor.act(delta)
              └─ Player.update(delta)
                    ├─ updateStance()                 crouch / run, hitbox height
                    ├─ state.update(this, delta)      ← input, gravity, movement, collision
                    └─ updateAnimation(delta)         pick the picture
```

`delta` is the time in seconds since the last frame. All movement is multiplied by `delta` so the game runs at the same speed on every computer. `Player.update` caps it:

```java
delta = Math.min(delta, 1 / 30f);
```

This prevents a lag spike from moving the cat hundreds of pixels in one frame and skipping through the floor.

---

## 6. Coordinates and Hitbox

- World size is **1920 × 1080** (`Main.WORLD_WIDTH`, `Main.WORLD_HEIGHT`).
- **(0, 0) is the bottom-left** and **y goes up**. Gravity is therefore negative and jumping is positive.
- A `Rectangle` is `(x, y, width, height)`, where `y` is its **bottom** edge.
- The cat's hitbox is `bounds`. It is deliberately **smaller than the picture** so the game feels fair.

| State | Hitbox (width × height) |
|---|---|
| Standing | 90 × 90 |
| Crouching | 90 × 55 |

The sprite is centred on the hitbox horizontally, with its feet on the hitbox bottom.

---

## 7. Design (as implemented)

### 7.1 Collision (AABB, one axis at a time) — `DynamicEntity.moveAndCollide`

1. Move on the **X** axis, then push the cat out of any block it overlaps.
2. Move on the **Y** axis, then push the cat out of any block it overlaps.
3. A downward collision means the cat **landed** (`onGround = true`). An upward collision means it **hit its head** (`hitCeiling`, upward speed set to 0). A sideways collision sets `hitWall` and sets horizontal speed to 0, so the cat can never stick to a wall.

Movement is split into small sub-steps (at most `MAX_STEP` = 4 px each) so fast movement cannot tunnel through thin platforms.

**Steps and ramps:** if the cat is on the ground and walks into a block whose top is at most `STEP_HEIGHT` = 12 px above its feet, and there is room above it, the cat is lifted onto it. When walking down such a step the cat is snapped down to the ground, so it does not switch to `FallingState` for a small step. Anything higher than 12 px is a wall, and a drop of more than 12 px is a real fall (`FallingState`).

**Crouch headroom:** when `S` is released, `Player` checks whether a standing hitbox would overlap a solid. If it would, the cat stays crouched until it can stand.

Always loop over solids with an indexed `for` loop. libGDX `Array` iterators cannot be nested.

### 7.2 State Pattern

```
                 jump pressed
   GroundedState ─────────────→ JumpingState
      ↑   ↘ walked off ledge        │ velocity.y <= 0
      │       ↘                     ↓
      └──────── landed ──────── FallingState
```

Each state implements one interface:

```java
public interface PlayerState {
    void enter(Player p);
    void update(Player p, float delta);
    void exit(Player p);
}
```

`Player` keeps a reference to the current state and delegates to it, so the same call (`state.update(this, delta)`) behaves differently depending on which state object is stored. This is polymorphism.

| State | What it does each frame | Leaves when |
|---|---|---|
| `GroundedState` | ground acceleration / friction, jump start, gravity, collision | jump pressed → `JumpingState`; no ground under the feet → `FallingState` |
| `JumpingState` | air steering, **jump cut** if `W` is released early, gravity, collision | landed → `GroundedState`; `velocity.y <= 0` (top of jump or head bump) → `FallingState` |
| `FallingState` | air steering, gravity, collision | landed → `GroundedState` |

`Player.changeState(next)` calls `exit` on the old state and `enter` on the new one. The cat starts in `FallingState`, so a spawn point above the floor just drops it in.

### 7.3 Feel (tuning constants)

All are public named constants in `Player` (pixels per second, or per second²):

| Constant | Value | Meaning |
|---|---|---|
| `WALK_SPEED` / `RUN_SPEED` / `CROUCH_SPEED` | 200 / 400 / 80 | top ground speeds |
| `GROUND_ACCEL` | 2000 | speeding up on the ground |
| `GROUND_FRICTION` | 2400 | slowing down on the ground when no key is held |
| `AIR_ACCEL` | 1200 | steering in the air |
| `AIR_FRICTION` | 400 | slowing down in the air (low, so jumps keep momentum) |
| `JUMP_SPEED` | 500 | upward speed at takeoff |
| `JUMP_CUT_SPEED` | 200 | upward speed is cut down to this when `W` is released early |

`GRAVITY` (−900) lives in `DynamicEntity`. A full jump reaches about 135 px; a quick tap gives a hop of about 30 px.

---

## 8. Integration Guide for Other Members

### Member 2 (Level & Map)

Give the player the solid rectangles from the Tiled collision layer:

```java
player.setSolids(solids);          // Array<Rectangle>
```

To check whether the player touches a trap, portal or finish line:

```java
if (spike.getBounds().overlaps(player.getBounds())) {
    player.takeDamage(1);
}
```

Spawn the player through `game.createPlayer(x, y)`. Spawn it in **empty space**, slightly above the floor so it drops in. Do not spawn it overlapping a solid block: the collision code pushes the cat out of blocks, and a spawn inside several blocks can push it somewhere unexpected.

`setSolids` keeps a reference to your array, so adding rectangles to it later also works. Call `setSolids` again when a new level is loaded. Every solid must be a plain `Rectangle` (no slopes); build ramps from steps of 12 px or less.

### Member 3 (Abilities, States & UI)

- Health lives in `Player` (`getHealth()`, `takeDamage(int)`).
- Movement states (`GroundedState`, `JumpingState`, `FallingState`) are only about moving through the world. The Liquid / Solid form morphing is a separate pattern and does not need to touch them. Useful hooks: `player.getVelocity()`, `player.isOnGround()`, `player.isCrouching()`, `player.getStateName()`, and the public tuning constants in `Player`.
- Hitbox size is controlled by the `BODY_WIDTH`, `STAND_HEIGHT` and `CROUCH_HEIGHT` constants in `Player`. If a transformation changes the collision bounds, change them through the player rather than editing `bounds` from outside.
- Textures are passed into the `Player` constructor today. When the `AssetManager` is ready, agree on replacing the long constructor with one that reads from it.

---

## 9. Debug Tools

In `LevelScreen`, `DEBUG = true` draws:

- **Grey rectangles**: every solid block the cat can collide with.
- **Green outline**: the cat's real hitbox.
- **Text (top-left)**: current state (`GroundedState` / `JumpingState` / `FallingState`), `onGround`, `crouching`, and the cat's speeds `vx` / `vy`.

Use it whenever collision looks wrong. The game decides collisions from the green box, not from the picture.

---

## 10. Manual Test Checklist

The test level now has: a floating platform (jump under it), a 3-step staircase (10 px per step), a low ceiling (70 px gap, only fits crouched), a tall wall at the right, and solid walls at both screen edges.

- [ ] Walk, run and crouch; speeds feel different.
- [ ] Hold `S`: the green box gets shorter.
- [ ] Walk left and right: the picture stays centred in the green box.
- [ ] Jump: the cat lands exactly on the floor top, with no sinking.
- [ ] Lag test: drag the window while jumping; the cat never falls through the floor.
- [ ] Jump under the platform: the cat bumps its head.
- [ ] Run into a wall at full speed: the cat stops cleanly and does not stick.
- [ ] Walk up small steps without jumping.
- [ ] Crouch under a low platform and release `S`: the cat stays crouched until there is headroom.
- [ ] Walk off a ledge: the state changes to `FallingState`.

---

## 11. Known Issues and TODO

- Spawning the cat **inside** a solid is not supported (see section 8).
- Only rectangles collide, so there are no real slopes: ramps are built from steps of 12 px or less.
- There is no run animation yet; running alternates the two jump pictures.
- The `Player` constructor takes 8 textures; replace with `AssetManager` access. The `ozflow3` texture is accepted but not used yet.
- Nice-to-have, not built: coyote time (jump just after leaving a ledge) and jump buffering (jump pressed just before landing).

---

## 12. Naming Conventions (team rules)

- Classes: `PascalCase`
- Methods and variables: `camelCase`
- Constants: `UPPER_SNAKE_CASE` (e.g. `GRAVITY`, `JUMP_SPEED`)
- Package: `com.Group6.mygame`
