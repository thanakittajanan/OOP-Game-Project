# Gas form + Level 4 (prototype)

## Rules
- **Locked** until the star near the end of Level 3 ("The Furnace", hangs 2 tiles up before the exit - jump for it) is collected. Saved in libGDX Preferences (`GasProgress`). **F4** toggles the lock for testing.
- **E** turns the cat into gas (needs >= 10% heat), **E** again condenses it. **W A S D** fly. No gravity.
- **One progress bar (HeatMeter, top-left).** Flying drains a full bar in 11 s. Fire refills it: `Brazier` (60/s, harmless, reaches 3 tiles up), burning `FlameVent` and `FireHole` (45/s; a cloud is warmed by them instead of hurt).
- Bar empty -> the cloud condenses where it is. Over a pit = a fall; inside rock = back to the checkpoint (-1 HP).
- A cloud passes every solid tile and the floor, and is ignored by spikes, saws and monsters. It is stopped only by the map edge and **sealed doors** (`S`).
- **Frost pipe** (`I`): touching it empties the bar at once. **Fan**: pushes a cloud; thin gas is pushed up to 2x harder. **Valve**: only a cloud above 20% heat can turn it; it opens every sealed door.
- Pressing E inside rock is refused ("No room to condense").
- The thin cloud is drawn paler and bluer: the sprite is the second readout of the bar.

## Level 4 - The Vent Works
1. Warm up: charge at a brazier, pass a wall.
2. Heat relay: three 18-tile gaps over spikes; one bar crosses two, not three -> refuel on the island braziers.
3. Dangerous passage: 11-tile wall with frost pipes, gaps alternate floor / ceiling (pipes also run through the floor rows, so you cannot drift underneath).
4. Valve: a fan, a chamber with a valve in a 1-tile cell (too narrow for the cat), a sealed door. Turn the valve, leave the chamber, condense on the walkway, walk through the door.
Every checkpoint has a brazier next to it, so a respawn never strands the player without heat.

## Files
New: `HeatMeter`, `GasState`, `GasProgress`, `level/Brazier`, `level/FrostPipe`, `level/Fan`, `assets/Level_4.jpg`.
Changed: `CatForm` (canFly/passesWalls), `GasForm`, `MorphState`, `Player`, `Hazard` (heatRate/freezesGas/wind hooks), `LevelData/Map/World/Art/Theme`, `LevelScreen` (heat bar, F4), `LevelSelectScreen`.

## Tuning knobs
`HeatMeter.FULL_FLIGHT_SECONDS`, `HeatMeter.THIN_FRACTION`, `GasForm.FLY_SPEED`, `Brazier.HEAT_PER_SECOND`, `Fan.WIND_SPEED`. Level 4 distances are written against FLY_SPEED x FULL_FLIGHT_SECONDS; change one and re-check the relay gaps.
