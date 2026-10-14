package com.Group6.mygame.level;

import com.badlogic.gdx.utils.Array;

/**
 * The four levels, written as Java code. Each level is a grid of characters 16 rows tall, plus a list
 * of placed objects (spawn, checkpoints, exit, monsters, saws, falling spikes).
 *
 * <p>Rows and columns are counted from the TOP-LEFT here, like reading text. {@link LevelMap} turns them
 * into libGDX's y-up coordinates. One tile is {@link #TILE_SIZE} px.
 *
 * <pre>
 *   #  solid      H  vine (solid, climbable)   |  bars / grate (liquid passes)
 *   ^  spikes     L  fire hole                  F  flame vent (solid base)
 *   ~  crumbling floor                          .  empty
 *   I  frost pipe (solid, freezes gas)          S  sealed door (blocks even gas, valve opens it)
 * </pre>
 *
 * Gas cloud rule of thumb (level 4): a cloud flies through every solid tile, including the floor, so
 * anything meant to stop it - a frost pipe, a sealed door - must reach all the way down through rows
 * 14 and 15. {@link #sealed} does that by itself; {@link #frost} is told its rows.
 *
 * Design rules (Member 1's cat: 90 x 90 standing, 90 x 55 crouching, jump about 139 px):
 * a 1-tile gap only fits the crouching / liquid cat; ledges are at most 2 tiles up;
 * pits are at least 2 tiles wide because the cat is 1.4 tiles wide.
 */
public final class LevelData {
    public static final int ROWS = 16;
    public static final float TILE_SIZE = 64f;
    public static final int LEVEL_COUNT = 4;

    /** Something placed in the level. col/row are the tile just ABOVE the floor it stands on. */
    public static final class Placement {
        public final String type;   // spawn, checkpoint, exit, monster, saw, fallingSpike
        public final int col, row;
        public int tier, range;     // monster
        public int dx, dy;          // saw: travel in tiles (dy positive = down)
        public float period, phase; // saw
        public int w, h;            // fan: size of the air block in tiles (col,row = its top-left tile)

        Placement(String type, int col, int row) {
            this.type = type;
            this.col = col;
            this.row = row;
        }
    }

    public final String title;
    public final int width;
    /** grid[row][col], row 0 is the top. */
    public final char[][] grid;
    public final Array<Placement> placements = new Array<>();

    private LevelData(String title, int width) {
        this.title = title;
        this.width = width;
        this.grid = new char[ROWS][width];
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < width; c++) grid[r][c] = '.';
        }
        rect(0, 0, 0, ROWS - 1, '#');            // left wall
        rect(width - 1, 0, width - 1, ROWS - 1, '#'); // right wall
        rect(0, 14, width - 1, 15, '#');         // floor (2 tiles thick)
    }

    // ------------------------------------------------------------------ building helpers

    private void rect(int c0, int r0, int c1, int r1, char ch) {
        for (int r = r0; r <= r1; r++) {
            for (int c = c0; c <= c1; c++) grid[r][c] = ch;
        }
    }

    private void solid(int c0, int r0, int c1, int r1) { rect(c0, r0, c1, r1, '#'); }

    /** A hole in the floor with spikes ('^') or fire ('L') at the bottom. */
    private void pit(int c0, int c1, char bottom) {
        rect(c0, 14, c1, 14, '.');
        rect(c0, 15, c1, 15, bottom);
    }

    private void vent(int c) { grid[14][c] = 'F'; }

    /** Frost pipe: solid rock that condenses any gas cloud touching it. */
    private void frost(int c0, int r0, int c1, int r1) { rect(c0, r0, c1, r1, 'I'); }

    /** Sealed door: blocks the cat and the gas cloud until a valve is opened. */
    private void sealed(int c0, int r0, int c1, int r1) { rect(c0, r0, c1, r1, 'S'); }

    private Placement place(String type, int col, int row) {
        Placement p = new Placement(type, col, row);
        placements.add(p);
        return p;
    }

    private void spawn(int c, int r) { place("spawn", c, r); }
    private void checkpoint(int c, int r) { place("checkpoint", c, r); }
    private void exit(int c, int r) { place("exit", c, r); }
    private void fallingSpike(int c, int r) { place("fallingSpike", c, r); }
    /** Fire basin that recharges the heat bar; (c, r) is the tile it stands in. */
    private void brazier(int c, int r) { place("brazier", c, r); }
    /** The star that unlocks the gas form. */
    private void star(int c, int r) { place("star", c, r); }
    /** A valve a warm enough gas cloud can turn to open every sealed door. */
    private void valve(int c, int r) { place("valve", c, r); }

    /** Wind block, (c, r) is its top-left tile; dx/dy is the direction (-1, 0 or 1). */
    private void fan(int c, int r, int w, int h, int dx, int dy) {
        Placement p = place("fan", c, r);
        p.w = w;
        p.h = h;
        p.dx = dx;
        p.dy = dy;
    }

    private void monster(int c, int r, int range, int tier) {
        Placement p = place("monster", c, r);
        p.range = range;
        p.tier = tier;
    }

    private void saw(int c, int r, int dx, int dy, float period, float phase) {
        Placement p = place("saw", c, r);
        p.dx = dx;
        p.dy = dy;
        p.period = period;
        p.phase = phase;
    }

    // ------------------------------------------------------------------ the levels

    /** levelNumber is 1 to 4. */
    public static LevelData create(int levelNumber) {
        switch (levelNumber) {
            case 4: return ventWorks();
            case 2: return greenhousePipes();
            case 3: return theFurnace();
            default: return theDrain();
        }
    }

    /** Level 1: learn to move, jump and melt. Tier 1 Blobs only. */
    private static LevelData theDrain() {
        LevelData L = new LevelData("The Drain", 92);
        L.spawn(2, 13);
        L.solid(9, 13, 10, 13); L.solid(12, 12, 13, 13);        // two steps (1 and 2 tiles high)
        L.pit(16, 18, '^');                                      // spike pit, 3 wide
        L.solid(22, 1, 27, 12);                                  // low pipe: only row 13 free -> crouch
        L.checkpoint(30, 13);
        L.solid(33, 1, 34, 11); L.rect(33, 12, 34, 13, '|');     // bar gate: liquid / crouch only
        L.monster(40, 13, 3, 1);                                 // Blob
        L.pit(46, 54, 'L'); L.rect(46, 13, 54, 13, '~');         // crumbling bridge over a 9-wide fire hole
        L.checkpoint(57, 13);
        L.solid(59, 1, 67, 6);                                   // ceiling with falling spikes
        L.fallingSpike(60, 7); L.fallingSpike(63, 7); L.fallingSpike(66, 7);
        L.pit(70, 76, 'L'); L.solid(69, 12, 76, 12);             // guarded ledge over fire
        L.monster(73, 11, 2, 1);
        L.monster(80, 13, 2, 1);
        L.solid(84, 13, 85, 13); L.solid(86, 12, 87, 13); L.solid(88, 11, 90, 13);   // stairs
        L.exit(89, 10);
        return L;
    }

    /** Level 2: vines, grates, saws, Tier 2 Hoppers. */
    private static LevelData greenhousePipes() {
        LevelData L = new LevelData("Greenhouse Pipes", 110);
        L.spawn(2, 13);
        L.rect(6, 5, 7, 13, 'H');                                // vine tower
        L.solid(8, 5, 13, 5); L.solid(17, 5, 27, 5);             // walkway with a 3-tile gap
        L.rect(8, 13, 27, 13, '^');                              // spike bed below
        L.monster(22, 4, 3, 2);                                  // Hopper on the walkway
        L.solid(28, 5, 28, 13);
        L.rect(29, 5, 34, 5, '|');                               // floor grate: melt to drip through
        L.solid(35, 0, 37, 12);                                  // wall, crawl gap on row 13
        L.checkpoint(31, 13);
        L.checkpoint(39, 13);
        L.saw(43, 8, 0, 5, 2.4f, 0f);                            // saw gauntlet
        L.pit(45, 47, 'L');
        L.saw(49, 8, 0, 5, 2.4f, 0.5f);
        L.monster(52, 13, 1, 2);
        L.rect(57, 3, 58, 13, 'H');                              // second vine tower
        L.solid(59, 3, 62, 3); L.checkpoint(60, 2);
        L.rect(65, 5, 68, 5, '~');                               // platform run over spikes
        L.solid(72, 6, 76, 6); L.monster(75, 5, 1, 1);
        L.rect(80, 7, 82, 7, '~');
        L.solid(86, 7, 95, 7); L.monster(91, 6, 3, 2);
        L.rect(59, 13, 94, 13, '^');                             // long spike floor (95-96 is a safe landing)
        L.checkpoint(98, 13);
        L.solid(100, 0, 104, 7); L.fallingSpike(101, 8); L.fallingSpike(103, 8);
        L.pit(105, 106, 'L');
        L.exit(108, 13);
        return L;
    }

    /** Level 3: flame vents, Tier 3 Spitters, everything combined. */
    private static LevelData theFurnace() {
        LevelData L = new LevelData("The Furnace", 130);
        L.spawn(2, 13);
        L.vent(6); L.vent(10); L.vent(14);                       // vent row
        L.monster(12, 13, 1, 1);
        L.pit(17, 19, 'L');
        L.vent(22);
        L.solid(25, 1, 32, 12); L.vent(28);                      // hot crawl
        L.checkpoint(34, 13);
        L.solid(37, 1, 48, 11);                                  // Spitter corridor: too low to jump
        L.monster(53, 13, 1, 3);
        L.pit(57, 58, '^');
        L.checkpoint(61, 13);
        L.rect(64, 5, 65, 13, 'H');
        L.solid(66, 5, 80, 5); L.pit(66, 80, 'L'); L.monster(73, 4, 4, 2);   // bridge over fire
        L.solid(81, 0, 82, 0); L.rect(81, 1, 82, 4, '|'); L.solid(81, 5, 82, 5);   // bars
        L.rect(83, 5, 88, 5, '|');                               // grate
        L.solid(81, 6, 81, 13);
        L.solid(89, 0, 90, 12);                                  // wall, crawl gap on row 13
        L.saw(83, 13, 4, 0, 2.6f, 0f);                           // saw sweeping the room under the grate
        L.checkpoint(92, 13);
        L.vent(95); L.vent(99);
        L.monster(105, 13, 1, 3);
        L.checkpoint(109, 13);
        L.rect(111, 6, 112, 13, 'H');
        L.solid(113, 6, 127, 6); L.rect(117, 6, 119, 6, '~'); L.grid[6][122] = 'F';   // final bridge
        L.pit(113, 128, 'L');
        L.exit(125, 5);
        L.star(123, 3);                                          // hangs two tiles up: a jump on the way to the exit
        return L;
    }

    /**
     * Level 4 (PROTOTYPE): the gas cat. Four short lessons in a row, each one needs the cloud.
     *
     * <p>Distances are measured in seconds of flight: a full bar is {@link com.Group6.mygame.HeatMeter#FULL_FLIGHT_SECONDS}
     * seconds at {@link com.Group6.mygame.GasForm#FLY_SPEED} px/s, i.e. about 40 tiles.
     * <ol>
     *   <li>cols 0-20  "Warm up": charge at a brazier, puff, drift through a wall.</li>
     *   <li>cols 21-88 "Heat relay": three 18-tile gaps over spikes. One bar covers two of them,
     *       not three, so the cloud has to refuel at the two island braziers.</li>
     *   <li>cols 89-110 "Dangerous passage": an 11-tile wall with frost pipes inside it. The gaps in
     *       the pipes alternate between the floor and the ceiling, so the cloud has to weave.</li>
     *   <li>cols 111-137 "Valve": a fan, a hanging chamber with a valve in a 1-tile cell, and a
     *       sealed door. The cat cannot enter the cell, the cloud has to be warm enough to turn the
     *       valve, then condense on the walkway under the chamber and walk through the opened door.</li>
     * </ol>
     * Every checkpoint has a brazier beside it, so a respawn (which empties the bar) can never leave
     * the player stranded without heat.
     */
    private static LevelData ventWorks() {
        LevelData L = new LevelData("The Vent Works", 138);
        L.spawn(2, 13);

        // 1. Warm up: the wall is full height, so the cat cannot get past it on foot.
        L.brazier(5, 13);
        L.solid(12, 0, 13, 13);
        L.checkpoint(17, 13);
        L.brazier(19, 13);

        // 2. Heat relay: 18-tile gaps (1152 px = 5 s of flight) with 4-tile islands between them.
        L.pit(23, 40, '^');
        L.solid(41, 14, 44, 14); L.brazier(42, 13);
        L.pit(45, 62, '^');
        L.solid(63, 14, 66, 14); L.brazier(64, 13);
        L.pit(67, 84, '^');
        L.checkpoint(87, 13);
        L.brazier(90, 13);

        // 3. Dangerous passage: three frost pipes, gaps alternating low / high / low. A pipe
        //    column is frost from the ceiling or floor through the floor rows 14-15 as well, or the
        //    cloud would simply drift underneath the wall.
        L.solid(94, 0, 104, 13);
        L.frost(97, 0, 97, 8);    L.frost(97, 14, 97, 15);       // gap on rows 9-13
        L.frost(100, 5, 100, 15);                                // gap on rows 0-4
        L.frost(103, 0, 103, 8);  L.frost(103, 14, 103, 15);     // gap on rows 9-13

        // 4. Valve: fan in the way, hanging chamber, sealed door.
        L.checkpoint(108, 13);
        L.brazier(110, 13);
        L.fan(113, 3, 4, 11, -1, 0);                             // blows left, rows 3-13, cols 113-116
        L.solid(119, 1, 125, 8);                                 // chamber block over a 5-tile walkway
        L.rect(122, 4, 122, 5, '.');                             // 1x2 cell: too narrow for the cat
        L.valve(122, 5);
        L.sealed(128, 0, 129, 13);
        L.exit(133, 13);
        return L;
    }
}
