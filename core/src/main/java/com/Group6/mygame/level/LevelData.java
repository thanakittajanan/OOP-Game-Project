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
 *   P  pressure plate, pair A                    G  gate, shut until an A plate is held down
 *   p  pressure plate, pair B                    g  gate, shut until a B plate is held down
 * </pre>
 *
 * <p>A plate is not solid and does not hurt: it is a thin slab at the foot of its tile that only a
 * <em>heavy</em> form can push down ({@code CatForm.isHeavy()} - the cat and the cat lying flat, not
 * the puddle or the cloud). Once a pair is held down it stays down for the rest of the level, so a
 * gate can never shut again behind a cat that walked through it.
 *
 * <p>Gates and plates come in two independent pairs rather than one global mechanism. That is what
 * lets a level teach the idea with one plate and then set a harder question with the other; a single
 * flag would have the first plate in the level open the last door.
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

    /** Which fitting a {@code "pipe"} decoration is; the index is {@link GreenhouseArt}'s pipe row. */
    public static final int PIPE_ELBOW = 0, PIPE_CROSS = 1, PIPE_VALVE = 2, PIPE_VENT = 3;

    /** Something placed in the level. col/row are the tile just ABOVE the floor it stands on. */
    public static final class Placement {
        public final String type;   // spawn, checkpoint, exit, monster, saw, fallingSpike, hint, core, decor
        public final int col, row;
        public int tier, range;     // monster: tier and patrol range. decor "pipe": tier = fitting kind
        public int dx, dy;          // saw: travel in tiles (dy positive = down)
        public float period, phase; // saw
        public int w, h;            // fan: size of the air block in tiles (col,row = its top-left tile)
        public String text;         // hint: the line to show

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

    /** A greenhouse core: level 2's collectible. It is taken like the star and unlocks nothing. */
    private void core(int c, int r) { place("core", c, r); }

    /** Scenery only - a leafy strand hung from the ceiling of this tile. In no collision list. */
    private void hangvine(int c, int r) { place("hangvine", c, r); }

    /** Scenery only - a cluster of glowing crystals standing on the floor of this tile. */
    private void glowplant(int c, int r) { place("glowplant", c, r); }

    /** Scenery only - a pipe fitting standing on this tile. {@code kind} is one of the {@code PIPE_*}. */
    private void pipe(int c, int r, int kind) { place("pipe", c, r).tier = kind; }

    /** Pressure plate of pair A: not solid, and only a heavy form can push it down. */
    private void plate(int c, int r) { grid[r][c] = 'P'; }

    /** Pressure plate of pair B. */
    private void plateB(int c, int r) { grid[r][c] = 'p'; }

    /** Gate of pair A, filling rows {@code r0}..{@code r1} of one column. Shut until a plate goes down. */
    private void gate(int c, int r0, int r1) { rect(c, r0, c, r1, 'G'); }

    /** Gate of pair B. */
    private void gateB(int c, int r0, int r1) { rect(c, r0, c, r1, 'g'); }

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

    /**
     * A one-line teaching hint. Only the column matters: it appears the first time the cat gets its
     * nose past that tile, and never again. Keep them short - the message bar holds one line at a time.
     */
    private void hint(int c, int r, String text) {
        place("hint", c, r).text = text;
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

    /**
     * Level 1: five teaching zones in a row, one idea each. Tier 1 Blobs only - no fire, no saws, no
     * crumbling floor, and nothing that needs the gas form. Every hint is a single line, and each one
     * is placed at the column where the thing it talks about first shows up on screen.
     *
     * <pre>
     *   cols  1-15   Drain Entrance   flat and empty: somewhere safe to try A / D and Space
     *   cols 16-28   Broken Walkway   a small spike pit, a step, a wider one, then two steps up
     *   cols 29-43   Slime Nest       a raised platform with one Blob patrolling along it
     *   cols 44-54   Pressure Pipes   crawl under a low pipe (hold S), then melt through bars (Q)
     *   cols 55-62   Drain Exit       one last gap, the stairs, the exit door
     * </pre>
     *
     * <p>Distances are kept inside what the cat can actually do: the jump clears 138 px, so a step up
     * is never more than 2 tiles (128 px), and the gaps are 2 tiles wide, well under the 6 tiles it
     * covers at a run.
     */
    private static LevelData theDrain() {
        LevelData L = new LevelData("The Drain", 64);

        // 1. Drain Entrance --------------------------------------------------------------
        L.spawn(3, 13);
        L.hint(2, 13, "A / D move    Space jump");

        // 2. Broken Walkway --------------------------------------------------------------
        L.hint(16, 13, "Orange spikes hurt - jump the gap");
        L.pit(17, 18, '^');                                     // small gap first, spikes visible in it
        L.solid(21, 13, 22, 13);                                // a step, so the second gap is in the air
        L.pit(25, 26, '^');
        L.checkpoint(27, 13);
        L.solid(28, 13, 28, 13);                                // one tile up: the way onto the nest

        // 3. Slime Nest ------------------------------------------------------------------
        L.solid(29, 12, 43, 13);                                // the platform, 2 tiles above the floor
        L.monster(36, 11, 5, 1);                                // Blob, 320 px of patrol, edge-aware
        L.hint(31, 11, "The slime patrols - jump over it, or wait for it to turn");

        // 4. Pressure Pipes --------------------------------------------------------------
        // The cat walks off the nest on the right and lands back on the floor at col 44.
        L.checkpoint(45, 13);
        L.hint(46, 13, "Hold S to lie flat and crawl under the pipes");
        L.solid(47, 1, 49, 12);                                 // only row 13 is free: 1 tile of headroom
        // Cols 50-52 are deliberately left open. Growing back into the cat needs a clear 90 px-wide
        // gap, and prone-to-liquid is routed through the cat, so without this the player would be
        // stuck flat with no way to reach the bars. 3 tiles = 192 px.
        L.hint(52, 13, "Let go of S to stand, then Q - only liquid passes bars");
        L.solid(53, 0, 54, 11); L.rect(53, 12, 54, 13, '|');    // bar gate: prone cannot pass this

        // 5. Drain Exit ------------------------------------------------------------------
        L.hint(56, 13, "Find the exit");
        L.pit(57, 58, '^');
        L.solid(59, 13, 59, 13);                                // two low steps up to the door
        L.solid(60, 12, 62, 13);                                // 3 tiles wide: the arch feet fit on it
        L.exit(61, 11);
        return L;
    }

    /**
     * Level 2: the greenhouse. Four zones in a row, one lesson each, then all of them at once.
     *
     * <p>This is where the game first asks for something other than "walk right and jump". It teaches
     * the vine ({@code W}), lying flat ({@code S}), the puddle ({@code Q}) and the pressure plate, in
     * that order, and each one comes back a second time harder.
     *
     * <pre>
     *   cols  1-19   Greenhouse Entrance   flat ground, one blob, a spike trench, then a vine tower
     *   cols 20-40   The Pipe Run          a plateau reached only by climbing; crawl, then melt through
     *   cols 41-70   The Bulkhead          a pressure plate, and the gate it holds shut
     *   cols 71-122  The Greenhouse Core   melt through, set solid, press - then the run to the core
     * </pre>
     *
     * <p>Distances follow the budget level 1 was written against: a step up is never more than two
     * tiles (128 px, against a 138 px jump), and gaps are two to three tiles wide against the six or
     * so the cat covers at a run.
     *
     * <p>The shape of the level is doing real work, so it is worth saying plainly. The pipe run is a
     * plateau that is solid from its walking surface (row 5) all the way down to the floor, and the
     * ground before it is walled off by the vine tower. That is what makes the crawl and the grate
     * compulsory rather than a shortcut: there is no low road under the walkway to take instead.
     *
     * <p>Two rules the checkpoints follow, because a gate is the one thing in a level that can strand
     * a player: there is a flag on each side of every gate, so a respawn always lands either close
     * enough to the plate to press it again or already past the door; and the plate of a pair is never
     * further back than the flag before its gate.
     */
    private static LevelData greenhousePipes() {
        LevelData L = new LevelData("Greenhouse Pipes", 124);

        // ---- 1. Greenhouse Entrance: cols 1-19 -------------------------------------------
        L.spawn(3, 13);
        L.hint(2, 13, "Greenhouse Pipes - the sap still runs in the glass");
        L.core(6, 13);                                           // the first core, right on the path
        L.pipe(8, 13, PIPE_CROSS);
        L.hangvine(10, 9);
        L.monster(11, 13, 2, 1);                                 // Blob, well clear of the trench
        L.hint(9, 13, "The slime patrols - jump it, or wait for it to turn");
        L.pit(15, 17, '^');                                      // three wide: a running jump
        L.hint(18, 13, "Hold W while you touch a vine to climb it");
        L.rect(20, 5, 21, 13, 'H');                              // vine tower, top flush with the plateau

        // ---- 2. The Pipe Run: cols 22-40 --------------------------------------------------
        L.solid(22, 5, 34, 13);                                  // the plateau, floor to walking surface
        L.hint(23, 5, "Hold S to lie flat and crawl under the pipe");
        L.solid(24, 0, 26, 3);                                   // hanging pipe: row 4 is a one-tile crawl
        L.hangvine(29, 1);
        L.hint(28, 5, "Q = liquid - only a puddle gets through the grate");
        L.rect(31, 0, 31, 4, '|');                               // grate wall, filling the corridor
        L.pipe(23, 5, PIPE_VALVE);
        // Down the far side in two easy drops, with the second core on the way.
        L.solid(35, 8, 37, 8);
        L.core(36, 7);
        L.solid(38, 11, 40, 11);
        L.pipe(35, 13, PIPE_ELBOW);
        L.pipe(40, 13, PIPE_VENT);

        // ---- 3. The Bulkhead: cols 41-70 --------------------------------------------------
        L.checkpoint(42, 13);
        L.hint(44, 13, "Only the heavy solid cat can press the mossy plate");
        L.saw(46, 8, 0, 5, 2.6f, 0f);                            // sweeps down to head height
        L.plate(48, 13);
        L.solid(50, 0, 50, 11); L.gate(50, 12, 13);              // the wall, and the two-tile doorway in it
        L.hangvine(52, 9);
        L.glowplant(54, 13);
        L.checkpoint(53, 13);
        L.monster(58, 13, 2, 2);
        L.pipe(63, 13, PIPE_VALVE);

        // ---- 4. The Greenhouse Core: cols 71-122 -----------------------------------------
        L.checkpoint(71, 13);
        L.glowplant(73, 13);
        L.pipe(76, 13, PIPE_VENT);
        // The second pair, and the reason it is the second: the plate is on the far side of a grate,
        // so the cat has to arrive as a puddle and then set back into a cat to press it. Liquid to get
        // there, solid to use it - which is the whole level in one move.
        L.hint(78, 13, "Flow through the grate, then set solid on the plate");
        L.rect(80, 0, 80, 13, '|');                              // grate wall, ceiling to floor
        L.plateB(82, 13);
        L.hint(84, 13, "Something heavy shifted - the second bulkhead");
        L.monster(90, 13, 2, 2);
        L.solid(95, 0, 95, 11); L.gateB(95, 12, 13);             // the second gate
        L.checkpoint(93, 13);
        L.checkpoint(97, 13);
        // The third core, up a two-step stair so it is a jump and not a pixel test.
        L.solid(99, 13, 99, 13);
        L.solid(100, 12, 101, 12);
        L.core(100, 11);
        // Crumbling floor laid flush in the ground over spikes: walking pace is too slow, a run is not.
        L.hint(103, 13, "The old floor gives way - keep moving");
        L.rect(104, 14, 106, 14, '~'); L.rect(104, 15, 106, 15, '^');
        L.monster(109, 13, 2, 2);
        L.pit(113, 115, '^');
        L.glowplant(117, 13);
        // The last climb: a vine the cat has to use, because the platform is three tiles up.
        L.rect(118, 11, 118, 13, 'H');
        L.solid(119, 11, 121, 11);                               // the exit step, flush with the vine top
        L.glowplant(122, 13);
        L.exit(120, 10);
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
