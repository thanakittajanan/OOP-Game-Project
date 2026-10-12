# Member 2 — Levels, Map & Hazards (Shiro)

Part of **Polymorphic Cat** (package `com.Group6.mygame`). This covers the three levels, the traps, the monsters, the camera, and how they connect to Member 1's physics and Member 3's health and forms. Everything is plain Java: the levels are written in `LevelData.java`, so no map files are needed.

## How to play the levels

Run `lwjgl3:run`, then **Start Game → Select Level**. All three levels are unlocked.

| Key | Action |
|---|---|
| `A` / `D` | Move |
| `W` | Jump. Hold `W` against a vine to climb it |
| `S` | Crouch. **Crouching counts as liquid for now**: it passes bars, grates and 1-tile gaps |
| `Left Shift` | Run |
| `F3` | Debug view (solid boxes, bars, hitboxes) |
| `Esc` | Back to level select |

## The levels

| Level | Size | New things |
|---|---|---|
| 1 · The Drain | 92 × 16 tiles | low pipe, bar gate, fire holes, crumbling floor, falling spikes, Tier 1 Blob |
| 2 · Greenhouse Pipes | 110 × 16 | vines, floor grate, saw blades, Tier 2 Hopper |
| 3 · The Furnace | 130 × 16 | flame vents, Tier 3 Spitter and fireballs |

Tiles are 64 px. The maps are sized for Member 1's cat (90 × 90 standing, 90 × 55 crouching, jump about 139 px):

- A 1-tile-high gap (64 px) only fits the crouching / liquid cat.
- No ledge is more than 2 tiles (128 px) above where you stand.
- Pits are at least 2 tiles wide, because the cat is 1.4 tiles wide.

**If Member 1 changes jump height or the hitbox size, tell me.** The gaps and ledges are measured against these numbers.

## Files

```
core/src/main/java/com/Group6/mygame/
├── LevelScreen.java          plays a level: camera, background, tile renderer, HUD, next level / restart
├── LevelSelectScreen.java    three level buttons
└── level/
    ├── LevelData.java        the three levels, written as character grids + placed objects
    ├── LevelMap.java         builds solids, bars, vines, hazards, monsters, checkpoints from LevelData; draws the tiles
    ├── LevelWorld.java       runs a level each frame: collisions, damage, respawn, checkpoints, exit
    ├── LevelArt.java         placeholder shapes for traps and monsters (made with Pixmap)
    ├── LevelTheme.java       colours for each level
    ├── Hazard.java           abstract base for every trap
    ├── SpikeStrip.java  FireHole.java  FlameVent.java  FallingSpike.java  SawBlade.java  Fireball.java
    ├── CrumbleBlock.java     floor that breaks 0.6 s after you stand on it, grows back after 3 s
    ├── Monster.java          abstract base for monsters (patrol, hitbox, tier pips)
    └── Blob.java  Hopper.java  Spitter.java
```

## OOP design

```
GameObject (Member 1)
├── DynamicEntity → Player            (Member 1)
├── Hazard        → SpikeStrip, FireHole, FlameVent, FallingSpike, SawBlade, Fireball
├── Monster       → Blob, Hopper, Spitter
└── CrumbleBlock
```

- **Inheritance and polymorphism.** `LevelWorld` keeps one `Array<Hazard>` and one `Array<Monster>`. It calls `hazard.touches(cat)` and `monster.update(delta)` without knowing which kind each one is.
- **Abstraction.** `Hazard.getEffect()` says what a hit does: `RESPAWN` (back to the checkpoint) or `KNOCKBACK` (pushed away).
- **Factory method.** `Monster.create(tier, ...)` turns the `tier` number from the map into the right class.
- **Small state machine.** `FallingSpike` goes IDLE → SHAKING → FALLING → GONE.
- **Interface callback.** `Monster.HazardSink` lets a Spitter add fireballs without knowing about `LevelWorld`.

## Editing a level

Open `level/LevelData.java`. Each level is a method (`theDrain()`, `greenhousePipes()`, `theFurnace()`) that builds a 16-row grid. Rows and columns count from the **top-left**.

| Helper | What it does |
|---|---|
| `solid(c0, r0, c1, r1)` | solid block |
| `rect(c0, r0, c1, r1, ch)` | fill with any tile: `H` vine, `\|` bars/grate, `^` spikes, `~` crumbling floor |
| `pit(c0, c1, '^' or 'L')` | hole in the floor with spikes or fire at the bottom |
| `vent(c)` | flame vent in the floor |
| `spawn`, `checkpoint`, `exit`, `fallingSpike` `(c, r)` | put it in the tile just above the floor (or just under the ceiling for falling spikes) |
| `monster(c, r, range, tier)` | tier 1, 2 or 3; range = patrol distance each way, in tiles |
| `saw(c, r, dx, dy, period, phase)` | travels dx/dy tiles (dy positive = down), period in seconds, phase 0–1 |

## Monsters

| Tier | Class | Behaviour | How to get past |
|---|---|---|---|
| 1 | `Blob` | Walks its patrol at 120 px/s | Jump over it |
| 2 | `Hopper` | Hops 150 px high every 1.8 s, only moves while in the air | Jump it while it is down, or run under it mid-hop |
| 3 | `Spitter` | When the cat is in front of it within 12 tiles, its mouth glows for 0.5 s, then it spits a fireball. Reloads in 2 s | Crouch / liquid: fireballs fly 75 px up, over the 55 px cat. Then jump the Spitter |

Every monster, saw and fireball costs 1 HP and knocks the cat back. Spikes, fire holes, flame vents and falling spikes cost 1 HP and send the cat to the last checkpoint.

## What I need from the other members

**Member 1 (physics)**
- I only use public `Player` methods: `update`, `render`, `setSolids`, `getBounds`, `getPosition`, `getVelocity`, `isOnGround`, `isCrouching`. No physics code was changed.
- **Vine climbing is a stand-in** in `LevelWorld.applyVineClimb()`: it sets the upward speed while `W` is held against a vine. A real `ClimbingState` would be cleaner. When you add one, delete my method.
- `setSolids` gets a list that `LevelWorld` refills every frame. Broken floor disappears from it, and bars and grates are left out while the cat is liquid.

**Member 3 (forms, health, UI)**
- Liquid is checked in **one place**: `LevelWorld.isLiquid()`, which uses `isCrouching()` for now. When `LiquidForm` exists, change that one line.
- Bars and grates are in `LevelMap.getLiquidPassables()`. This is the "normal walls" list from your README; nothing is metal yet.
- Damage goes through your `player.takeDamage(1)`. I added a 1.2 s cooldown in `LevelWorld` so one trap cannot hit every frame. Replace it with your invincibility frames when they are ready.
- With **3 HP** the longer levels are hard. I suggest 5 HP; your call.
- Game over currently restarts the level after a short message. Hook your death screen into `LevelScreen.handleLevelEnd()`.

## Known gaps

- Trap and monster graphics are simple placeholder shapes. Swap in sprites inside each class's `render()`.
- The background image stays still behind the level (no parallax yet).
- The code was checked against the libGDX API but not run on a computer yet. Play each level once and report anything that blocks you.
