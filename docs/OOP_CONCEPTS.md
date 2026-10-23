# Object-Oriented Concepts in Polymorphic Cat

Where each OOP idea appears in the code, so you can point at it. Paths are under
`core/src/main/java/com/Group6/mygame/`.

## 1. Abstraction

An abstract class or interface says *what* something does and hides *how*.

| Type | What it promises | File |
|------|------------------|------|
| `GameObject` (abstract) | Every world object has a position, a hitbox, `update(delta)` and `render(batch)` | `GameObject.java` |
| `DynamicEntity` (abstract) | A `GameObject` that falls and collides; subclasses supply the behaviour | `DynamicEntity.java` |
| `Hazard` (abstract) | "Something that may hurt the cat": `touches(cat)`, `getEffect()` | `level/Hazard.java` |
| `Monster` (abstract) | A patrolling enemy with a tier | `level/Monster.java` |
| `PlayerState` (interface) | One way the cat can be moving: `enter`, `update`, `exit` | `PlayerState.java` |
| `CatForm` (interface) | One body the cat can have: height, speeds, abilities | `CatForm.java` |
| `Monster.HazardSink` (interface) | "Something that accepts new hazards", so a Spitter can fire without knowing `LevelWorld` | `level/Monster.java` |

## 2. Encapsulation

Data is private; other classes ask objects to *do* things rather than reaching in.

- `HeatMeter` keeps its value private and exposes behaviour: `add`, `drain`, `empty`, `isThin`, `fraction`.
- `Monster` keeps `direction`, `time`, `airHeight`, `minX` and `maxX` private. Subclasses use
  `getDirection()`, `faceCat()`, `advanceTime()` and `setAirHeight()`.
- `Player` keeps its five state objects private. States request a transition with `toGrounded()`,
  `toJumping()`, `toFalling()` or `toGas()`; only the player knows which object that means.
- `CatAnimations` hides eight `SpriteFrames` behind one `load()` / `dispose()` pair, so `Main` and the
  `Player` constructor no longer carry eight loose fields.
- `Audio`, `GasProgress` and `CheckpointSave` have private constructors and a small static interface.
- `GameObject.position` and `bounds` are `protected final`: subclasses can mutate the objects, but
  nobody can swap them for different ones.

## 3. Inheritance

```
GameObject
 +-- DynamicEntity
 |    +-- Player
 +-- Hazard
 |    +-- SpikeStrip, FireHole, FlameVent, FallingSpike, SawBlade, Fireball,
 |        Brazier, FrostPipe, Fan
 +-- CrumbleBlock
 +-- Monster
      +-- Blob (tier 1), Hopper (tier 2), Spitter (tier 3)

BaseScreen  (implements libGDX Screen)
 +-- MainMenuScreen, LevelSelectScreen, LevelScreen
```

Shared code lives once in the parent: collision and gravity in `DynamicEntity`, patrol logic in
`Monster.patrol`, the screen/stage/skin set-up in `BaseScreen`.

## 4. Polymorphism

**Runtime (overriding)**: the caller holds the parent type and the right child code runs.

| Call site | Parent type | Different behaviour in |
|-----------|-------------|------------------------|
| `state.update(this, delta)` in `Player.update` | `PlayerState` | `GroundedState`, `JumpingState`, `FallingState`, `MorphState`, `GasState` |
| `form.walkSpeed()`, `form.canJump()`, `form.height()` | `CatForm` | `SolidForm`, `ProneForm`, `LiquidForm`, `GasForm` |
| `hazard.touches(cat)` in `LevelWorld`, one loop for every trap | `Hazard` | a spike uses a box, a saw uses a circle, a vent only while burning |
| `monster.update(delta)` | `Monster` | Blob walks, Hopper hops, Spitter aims and fires |
| `object.render(batch)` | `GameObject` | each class draws itself |

`CatForm` also uses **default methods** (`isHeavy`, `canFly`, `passesWalls`): a new form only overrides
what makes it different.

**Compile time (overloading)**: `Monster` has two constructors, `LevelScreen` has two, and
`Player.drawFrame` has two forms.

## 5. Composition and design patterns

- **State pattern** - `PlayerState` and its five implementations. Replaces an `if (onGround) ... else if (jumping) ...` chain.
- **Strategy pattern** - `CatForm`. The player is *given* a body and asks it for numbers instead of testing "am I liquid?" everywhere.
- **Factory Method** - `Monster.create(tier, ...)` turns a number from the level data into the right subclass; `Main.createPlayer` builds a configured `Player`.
- **Template Method** - `BaseScreen.show()` runs the fixed set-up and calls the abstract `buildUi()`, which each screen fills in.
- **Dependency inversion** - `Spitter` depends on the small `HazardSink` interface, not on `LevelWorld`.
- **Composition ("has-a")** - `Player` has a `HeatMeter`, a set of `CatForm`s and a `CatAnimations`.

## 6. Open/Closed in practice

- A new cat form (ice, say) is **one new class** implementing `CatForm`. `Player` is not edited.
- A new trap is **one new subclass** of `Hazard`. `LevelWorld`'s loop is not edited.
- A new monster is one subclass of `Monster` plus one line in `Monster.create`.

## 7. Known trade-offs (be ready to discuss)

- `Hazard` carries optional hooks (`heatRate`, `freezesGas`, `windX`) that `Brazier`, `FrostPipe` and `Fan`
  use. Those three do not hurt the cat, so their `touches` returns `false`. A stricter design would split these
  into separate capability interfaces; the single base class was kept so `LevelWorld` can use one loop.
- `LevelWorld` and `LevelData` are large. Splitting `LevelWorld` into collision, hazard and checkpoint
  handlers is the natural next refactor, as is moving vine-climbing into a `ClimbingState`.
