# Polymorphic Cat

A 2D puzzle-platformer built with Java and [libGDX](https://libgdx.com/), inspired by *Cats are Liquid*.
Built as an Object-Oriented Programming course project (Group 6): the cat morphs between **solid, prone,
liquid and gas** forms, and each form changes how it moves and which obstacles it can pass.

## Quick start

| I want to...                | Do this                                                                 |
|-----------------------------|-------------------------------------------------------------------------|
| Play the pre-built game     | `java -jar "Polymorphic Cat-1.0.0.jar"` (needs Java 17 or newer)        |
| Run from source             | `./gradlew lwjgl3:run` (Windows: `gradlew.bat lwjgl3:run`)              |
| Build a fresh runnable jar  | `./gradlew lwjgl3:jar` -> `lwjgl3/build/libs/Polymorphic Cat-1.0.0.jar` |

The `.jar` is self-contained: it bundles the game code, libGDX, the native libraries for Windows, macOS and
Linux, and every asset (images and sound).

## Controls

| Key            | Action                                              |
|----------------|-----------------------------------------------------|
| `A` / `D`      | Move                                                |
| `Space`        | Jump (solid form only); also climbs vines with `W`  |
| `Shift`        | Run                                                 |
| hold `S`       | Lie prone (fits through low gaps)                   |
| `Q`            | Liquid form on / off (slips through bars and grates)|
| `E`            | Gas form on / off, then `W A S D` to fly            |
| `Esc`          | Back to the level list                              |
| `F3` / `F4`    | Debug view / unlock gas form (developer shortcuts)  |

## Object-oriented design at a glance

The full explanation, with the class for every concept, is in **[docs/OOP_CONCEPTS.md](docs/OOP_CONCEPTS.md)**.

```
GameObject (abstract)                 PlayerState (interface)          CatForm (interface)
 +-- DynamicEntity (abstract)          +-- GroundedState                +-- SolidForm
 |    +-- Player                       +-- JumpingState                 +-- ProneForm
 +-- Hazard (abstract)                 +-- FallingState                 +-- LiquidForm
 |    +-- SpikeStrip, SawBlade, ...     +-- MorphState                   +-- GasForm
 +-- Monster (abstract)                +-- GasState
      +-- Blob, Hopper, Spitter
```

- **Abstraction / inheritance:** `GameObject` -> `DynamicEntity` -> `Player`; `Hazard` and `Monster` hierarchies.
- **Polymorphism:** the game loop calls `update`, `render` and `touches` on `GameObject` / `Hazard` without
  knowing the concrete type.
- **Encapsulation:** state is private; other classes use intent-revealing methods (`HeatMeter.drain`,
  `Monster.faceCat`, `Player.toGrounded`).
- **Design patterns:** State (`PlayerState`), Strategy (`CatForm`), Factory Method (`Monster.create`),
  Template Method (`BaseScreen`).

## Project layout

```
core/src/main/java/com/Group6/mygame/          game logic (shared by all platforms)
    Player, DynamicEntity, GameObject            the cat and its physics
    CatForm + Solid/Prone/Liquid/GasForm         Strategy pattern
    PlayerState + Grounded/Jumping/...State      State pattern
    BaseScreen + MainMenu/LevelSelect/LevelScreen   UI screens
core/src/main/java/com/Group6/mygame/level/    levels, hazards, monsters, tile map, art
lwjgl3/                                        desktop launcher and jar packaging
assets/                                        images, music and sound effects
docs/                                          design notes (OOP concepts, physics, gas form)
```

## Team

| Member | Area                                                           |
|--------|----------------------------------------------------------------|
| Pete   | Physics and movement, collision (`docs/member1-physics.md`)    |
| Shiro  | Level and map system, hazards                                  |
| Joseph | Cat forms (State/Strategy), health, screens and HUD (lead)     |
| Patrik | _(add your area here)_                                         |

## Coding conventions

- Classes `PascalCase`, methods and variables `camelCase`, constants `UPPER_SNAKE_CASE`.
- 4-space indentation, LF line endings, UTF-8 (enforced by `.editorconfig`).
- Every class has a Javadoc comment saying what it is for; non-obvious members explain *why*, not *what*.
- Fields are `private` unless a subclass genuinely needs them.
- Package path: `com.Group6.mygame`.

## Tech stack

Java 17, libGDX 1.14.2, Gradle (wrapper included), LWJGL3 desktop backend, Git and GitHub.
