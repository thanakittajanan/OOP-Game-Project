<<<<<<< HEAD
# Polymorphic Cat

A 2D platformer game built with Java and libGDX, inspired by *Cats are Liquid*. Designed as an Object-Oriented Programming (OOP) course project demonstrating modular design, state patterns, and physics interactions.

---

## Project Overview

**Polymorphic Cat** is a 2D puzzle-platformer where players control a morphing cat navigating through challenging obstacles, traps, and platforming puzzles. The core mechanic revolves around switching between different physical states to alter movement speeds, jump physics, and collision bounds.

---

##  Tech Stack & Tools

* **Language:** Java (JDK 17)
* **Framework:** libGDX 1.14.2
* **Build Tool:** Gradle
* **Level Design:** Tiled Map Editor (`.tmx`)
* **Version Control:** Git & GitHub

---

##  Team Division

### Member 1: Physics & Movement（pete)
1. Player input handling (A/D movement, Space bar jump).
 2.Gravity calculations and velocity handling.
 3.Terrain AABB collision detection and wall-stick prevention.

### Member 2: Level & Map System(shiro)
 1.Parsing Tiled `.tmx` maps using `TmxMapLoader`.
 2.Rendering level environments via `OrthogonalTiledMapRenderer`.
 3.Camera tracking and object layer hazard processing (`Spike`, `Portal`).

###  Member 3: Systems, Abilities & UI (Joseph - Lead)
1.Base project structure initialization, Gradle setup, and Git workflow.
2.Core State Pattern for cat form morphing (Liquid form / Solid form).
3.Health system implementation (`takeDamage`), invincibility frames, and death logic.
4.Screen navigation (`GameScreen`) and HUD UI drawing.

---

##  Completed Setup

- [x] Base libGDX desktop project initialized.
- [x] Abstract base class `GameObject.java` defined.
- [x] Player subclass `Player.java` created with basic interface signatures (`takeDamage`, `getPosition`, `getHealth`).
- [x] Basic game rendering loop (`GameScreen.java`) configured.
- [x] Git repository set up with main and feature branches.

---

## Code Naming Conventions

Classes: `PascalCase` (e.g., `GameObject`, `Player`)
Methods / Variables: `camelCase` (e.g., `takeDamage()`, `currentHealth`)
Constants:`UPPER_SNAKE_CASE` (e.g., `MAX_HEALTH`, `GRAVITY`)
Package Path: `com.Group6.mygame`

A [libGDX](https://libgdx.com/) project generated with [gdx-liftoff](https://github.com/libgdx/gdx-liftoff).

This project was generated with a template including simple application launchers and a main class extending `Game` that sets the first screen.

## Platforms

- `core`: Main module with the application logic shared by all platforms.
- `lwjgl3`: Primary desktop platform using LWJGL3; was called 'desktop' in older docs.

## Gradle

This project uses [Gradle](https://gradle.org/) to manage dependencies.
The Gradle wrapper was included, so you can run Gradle tasks using `gradlew.bat` or `./gradlew` commands.
Useful Gradle tasks and flags:

- `--continue`: when using this flag, errors will not stop the tasks from running.
- `--daemon`: thanks to this flag, Gradle daemon will be used to run chosen tasks.
- `--offline`: when using this flag, cached dependency archives will be used.
- `--refresh-dependencies`: this flag forces validation of all dependencies. Useful for snapshot versions.
- `build`: builds sources and archives of every project.
- `cleanEclipse`: removes Eclipse project data.
- `cleanIdea`: removes IntelliJ project data.
- `clean`: removes `build` folders, which store compiled classes and built archives.
- `eclipse`: generates Eclipse project data.
- `idea`: generates IntelliJ project data.
- `lwjgl3:jar`: builds application's runnable jar, which can be found at `lwjgl3/build/libs`.
- `lwjgl3:run`: starts the application.
- `test`: runs unit tests (if any).

Note that most tasks that are not specific to a single project can be run with `name:` prefix, where the `name` should be replaced with the ID of a specific project.
For example, `core:clean` removes `build` folder only from the `core` project.
=======
This is GAME project for OOP lecture
>>>>>>> 7418314dff91e9c5258eb74f707e03aaa8907694
