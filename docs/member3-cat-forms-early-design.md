# Polymorphic Cat: Cat Forms (README for Member 3)

## Goal
The cat changes its matter form with the **E** key:

**Solid → Liquid → Gas → Solid → ...**

| Form | Main power |
|---|---|
| **Solid** | Normal movement. Can press heavy switches. |
| **Liquid** | Passes through normal solid walls. |
| **Gas** | Flies across long gaps and tall cliffs (time limited). |

## Who does what

| Part | Owner |
|---|---|
| Physics values, wall-skipping collision, flying movement, `FlyingState` | Member 1 |
| `CatForm` classes, E key, textures, sounds, HUD | Member 3 (you) |
| Map tiles and objects that react to forms | Member 2 |

Think of it like outfits: you choose the outfit, and Member 1's physics reacts to what the outfit says.

## What Member 1 provides (fields and methods on `Player`)
These are changeable values, never hardcoded:

- `gravity`, `maxSpeed`, `acceleration`, `friction`, `jumpSpeed`
- `hitboxWidth`, `hitboxHeight`
- `canPassThroughWalls` (collision skips passable walls when true)
- `canFly`, `flySpeed`, `maxFlyTime` (Gas form)
- `requestFormChange(CatForm next)` (Member 1 may refuse, for example if the cat is inside a wall)

## What you build

### 1. The forms
```java
public interface CatForm {
    void apply(Player p);    // set physics values for this form
    String textureName();    // asset name, e.g. "cat_liquid"
}
```

```java
public class LiquidForm implements CatForm {
    public void apply(Player p) {
        p.canPassThroughWalls = true;
        p.canFly = false;
        p.setHitboxSize(20, 16);   // smaller body
        p.maxSpeed = 120;          // slower
    }
    public String textureName() { return "cat_liquid"; }
}

public class GasForm implements CatForm {
    public void apply(Player p) {
        p.canPassThroughWalls = false;
        p.canFly = true;
        p.maxFlyTime = 3f;         // seconds of flight
        p.flySpeed = 100;
        p.maxSpeed = 100;
    }
    public String textureName() { return "cat_gas"; }
}
```

Every form must **reset what the previous form changed** (for example, `SolidForm.apply` sets `canPassThroughWalls = false` and `canFly = false`). Otherwise powers carry over by mistake.

### 2. Form behavior (starting values, tune later)

| | Solid | Liquid | Gas |
|---|---|---|---|
| Walls | Blocked | Passes normal walls | Blocked |
| Metal walls | Blocked | **Blocked** | Blocked |
| Gravity | Normal | Normal | Off while flying |
| Speed | Normal | Slower | Slow |
| Hitbox | Normal | Smaller | Normal |
| Special | Presses heavy switches | Squeezes into gaps | W/S fly up and down, about 3 s |

### 3. Press E (Command pattern)
```java
public class ChangeFormCommand {
    public void execute(Player p) {
        p.requestFormChange(p.getFormCycle().next());
    }
}
```
The input code only calls the command. If we rebind the key later, the cat code does not change.

### 4. Textures and sounds (AssetManager)
- Load `cat_solid`, `cat_liquid`, `cat_gas` through the shared `GameAssets` singleton.
- When the form changes, swap the sprite and play a transform sound.

### 5. Event (Observer pattern)
When the form really changes, fire a `FormChanged` event. The texture swap, sound, and HUD icon all listen to it. Nobody has to edit anyone else's code.

### 6. HUD
- Show an icon of the current form.
- Show a **fly meter** (a bar) for Gas, so players see how much flight time is left.

## Rules we must agree on
1. **Switching inside a wall:** if the cat is Liquid inside a wall and presses E, we either block the switch or push the cat out to the nearest free spot. **Decision: ____**
2. **Wall types:** normal walls are passable by Liquid. Metal walls are never passable. Member 2 gives Member 1 two lists: `solids` and `metalSolids`.
3. **Hitbox change:** when the hitbox shrinks or grows, keep the cat's **feet** in the same place so it does not fall through the floor.
4. **Gas flight limit:** about 3 seconds. The timer refills when the cat touches the ground or switches back to Solid. Without a limit, Gas skips every obstacle.
5. **Switch cooldown:** optional, about 0.5 seconds, to stop players spamming E.
6. **Gas ending mid-air:** when the fly timer runs out, the cat starts falling (Member 1's `FallingState`).

## Build order
1. Solid and Liquid first.
2. Add Gas after those two work.
3. Add effects, sounds, and HUD polish last.

## Testing checklist
- [ ] E cycles through all forms in order
- [ ] Texture matches the form
- [ ] Liquid passes normal walls but not metal walls
- [ ] Gas flies with W/S and falls when the timer ends
- [ ] Fly timer refills on the ground
- [ ] Switching back to Solid inside a wall does not trap the cat
- [ ] Hitbox change does not drop the cat through the floor
- [ ] Each form resets the previous form's powers
