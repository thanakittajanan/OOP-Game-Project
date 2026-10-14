# Polymorphic Cat: Level, Map and Environment (README for Member 2)

## Goal
Load levels made in **Tiled**, create the objects inside them (spikes, platforms, switches, portals, finish line), and manage level flow (win, lose, reset, next stage).

Think of yourself as the **stage builder**. Member 1 built the referee (collision), Member 3 designs the cat's costumes (forms). You build the stage and everything on it.

## Who does what

| Part | Owner |
|---|---|
| Physics, collision code, `GameObject`, `DynamicEntity`, `Player` | Member 1 |
| `TmxMapLoader`, `InteractableObject` subclasses, factory, level flow | Member 2 (you) |
| Cat forms, E key, screens, HUD, sounds | Member 3 |

## 1. What Member 1 expects from you

### Solid blocks
- Give Member 1 an `Array<Rectangle>` of solid blocks.
- **Pixel units, not tile units.** A units mismatch is the most common bug.
- Read them from a Tiled layer named `solids`.

```java
Array<Rectangle> solids = mapLoader.getSolids();
Array<Rectangle> metalSolids = mapLoader.getMetalSolids();
player.update(delta, solids, metalSolids);
```

### Two wall types
| Type | Liquid cat | Tiled layer |
|---|---|---|
| Normal wall | Can pass through | `solids` |
| Metal wall | Cannot pass | `metalSolids` |

Without metal walls, Liquid could skip whole levels.

### Do not put traps in `solids`
If spikes are in the solid list, the cat will stand on them like a floor instead of getting hurt.

### Ramps
Member 1's step-up handles stairs up to **12 px**. Draw ramps as **small steps**, not slanted shapes. Collision only supports rectangles.

## 2. Interactable objects

Anything the cat touches but does not get blocked by.

```java
public abstract class InteractableObject extends GameObject {
    public abstract void onTouch(Player p);
}
```

| Object | Behavior |
|---|---|
| `Spikes` | `onTouch` calls `p.die()` |
| `Switch` | Toggles something (door, platform). Some need **Solid** form (heavy). |
| `Portal` | Moves the cat to a linked portal |
| `FinishLine` | Fires the level-complete event |
| `MovingPlatform` | A `DynamicEntity` (not an interactable). It moves between two points, and the cat rides it. |

Level loop:
```java
for (InteractableObject obj : interactables) {
    if (player.bounds.overlaps(obj.bounds)) obj.onTouch(player);
}
```

## 3. Factory Pattern (your required pattern)

Tiled objects have a `type` property. The factory reads it and builds the right class, so the loader never needs a giant `if/else`.

```java
public class ObjectFactory {
    public static GameObject create(MapObject mapObj) {
        String type = mapObj.getProperties().get("type", String.class);
        switch (type) {
            case "spikes": return new Spikes(mapObj);
            case "switch": return new Switch(mapObj);
            case "portal": return new Portal(mapObj);
            case "finish": return new FinishLine(mapObj);
            case "platform": return new MovingPlatform(mapObj);
            default: throw new IllegalArgumentException("Unknown type: " + type);
        }
    }
}
```

Adding a new object later means one new class and one new `case`.

## 4. Level flow

- **Win:** the cat touches `FinishLine`, fire a `LevelCompleted` event, then load the next level.
- **Lose:** the cat touches a trap or falls below the map, fire a `PlayerDied` event.
- **Reset:** call `player.respawn(x, y)` at the start point (or checkpoint) and reset switches and platforms.
- **Stage transition:** a stage has several levels (Stage 1 → Level 1-1, 1-2, 1-3).

Use events (Observer pattern) instead of calling Member 3's screens directly:

| Event | Member 2 does | Member 3 does | Member 1 does |
|---|---|---|---|
| `PlayerDied` | Reset level | Death sound, Game Over screen | `respawn()` |
| `LevelCompleted` | Load next level | Victory screen | Nothing |

## 5. Form-aware map ideas (our "original" feature)

| Obstacle | Solved by |
|---|---|
| Thick normal wall | **Liquid** passes through |
| Metal wall | Cannot pass, find another route |
| Wide pit or tall cliff | **Gas** flies over (about 3 s limit) |
| Heavy switch | **Solid** presses it |
| Vent | Lifts Gas upward |
| Narrow grate | Only **Liquid** fits (small hitbox) |

Design tips:
- Keep Gas gaps **shorter than the fly time allows**, or players get stuck.
- Do not let Liquid skip the whole level: put metal walls around the finish area.
- Build Solid and Liquid levels first. Add Gas levels later.

## Rules we must agree on
1. **Tile size in pixels:** ____
2. **Respawn:** start point or checkpoints? ____
3. **Invincibility after respawn:** yes/no, how long? ____
4. **Moving platforms:** how does Member 1 receive them? (suggestion: a separate `Array<DynamicEntity>` from your factory) ____
5. **Tiled layer names:** `solids`, `metalSolids`, `objects` (change only after telling everyone)

## Build order
1. Load a simple map and give Member 1 the `solids` list.
2. Add `Spikes` and `FinishLine`, so the basic loop (play, die, win) works.
3. Add the factory and the remaining objects (switch, portal, moving platform).
4. Add `metalSolids` and form-based puzzles.
5. Build real levels.

## Testing checklist
- [ ] Solids are in pixel units (cat stands exactly on tiles)
- [ ] Spikes hurt the cat and do not act like a floor
- [ ] Finish line loads the next level
- [ ] Falling out of the map triggers death and respawn
- [ ] Reset restores switches and platforms
- [ ] Cat rides moving platforms without sliding off
- [ ] Liquid passes `solids` but not `metalSolids`
- [ ] Unknown object type shows a clear error
