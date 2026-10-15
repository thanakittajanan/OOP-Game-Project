package com.Group6.mygame.level;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/**
 * Builds one level from {@link LevelData}: collision boxes, bars, vines, traps, monsters, checkpoints,
 * gates, spawn and exit. It also draws the tiles.
 *
 * <p>Coordinates: LevelData counts rows from the TOP. libGDX uses y-up, so here tile (0, 0) is the
 * BOTTOM-left tile and {@code y = ROWS - 1 - row}.
 */
public class LevelMap {

    /**
     * The two plate/gate pairs, A and B. Each pair is independent, which is what lets a level teach the
     * mechanism with one and then set a harder question with the other; a single global "gate open"
     * flag would have the first plate in the level open the last door.
     */
    public static final int PAIR_A = 0, PAIR_B = 1, PLATE_PAIRS = 2;

    /** How tall the part of a plate tile that counts as "stood on" is, measured up from its floor. */
    public static final float PLATE_TRIGGER_HEIGHT = 14f;

    /** Decor kinds, so {@link Decor#kind} is not a bare number at the call sites. */
    public static final int DECOR_HANGVINE = 0, DECOR_GLOWPLANT = 1, DECOR_PIPE = 2;

    private final LevelData data;
    private final LevelArt art;
    private final int widthInTiles, heightInTiles;
    private final float tileSize;
    private final char[][] tiles;         // [x][y], y-up
    private final boolean[][] solidGrid;  // [x][y] true where a tile always blocks (solid, vine, vent, frost)
    private final int[] topSolidRow;      // [x] highest solid row in that column, or -1 if none

    private final Array<Rectangle> terrainSolids = new Array<>();   // merged into long strips (fewer boxes, no seams)
    private final Array<Rectangle> liquidPassables = new Array<>(); // bars and grates: solid for the standing cat only
    private final Array<Rectangle> vines = new Array<>();           // climbable walls
    private final Array<Rectangle> sealedDoors = new Array<>();     // stop the cat AND the gas cloud, until a valve is turned
    private final Array<Rectangle> boundarySolids = new Array<>();  // the four edges of the map: the only "walls" a cloud respects
    private Rectangle valve;                                        // null in levels without one
    private boolean valveOpen;
    private Rectangle star;                                         // null in levels without one
    private final Array<Hazard> hazards = new Array<>();
    private final Array<CrumbleBlock> crumbles = new Array<>();
    private final Array<Monster> monsters = new Array<>();
    private final Array<Rectangle> checkpoints = new Array<>();
    private final Array<Hint> hints = new Array<>();
    private final Vector2 spawn = new Vector2();
    private final Rectangle exit = new Rectangle();

    private final Array<Rectangle> cores = new Array<>();           // greenhouse cores (level 2's collectible)
    private final Array<Decor> decor = new Array<>();               // painted scenery, in no collision list
    private final Array<Plate> plates = new Array<>();              // pressure plates
    private final Array<GateColumn> gateColumns = new Array<>();    // gates, as whole doors, for drawing
    private final Array<Rectangle> gatesA = new Array<>();          // shut gates held by the 'P' plates
    private final Array<Rectangle> gatesB = new Array<>();          // shut gates held by the 'p' plates
    private final boolean[] gateOpen = new boolean[PLATE_PAIRS];

    /** A line of teaching text that appears once, when the cat gets past {@link #x}. */
    public static final class Hint {
        public final float x;
        public final String text;

        Hint(float x, String text) {
            this.x = x;
            this.text = text;
        }
    }

    /**
     * A pressure plate: the box the cat has to be standing in, and which gate pair it holds open.
     *
     * <p>The box is a thin slab at the foot of the tile rather than the whole tile, so the plate reads
     * as something underfoot: a cat that jumps over it clears it, and a cat that walks on it presses it.
     */
    public static final class Plate {
        public final Rectangle bounds;
        public final int group;

        Plate(Rectangle bounds, int group) {
            this.bounds = bounds;
            this.group = group;
        }
    }

    /**
     * A whole gate - one column's worth of contiguous gate tiles - so it can be drawn as a single door
     * instead of a stack of tiles.
     */
    public static final class GateColumn {
        public final int group;
        public final float centerX;
        public final float footY;
        public final float height;

        GateColumn(int group, float centerX, float footY, float height) {
            this.group = group;
            this.centerX = centerX;
            this.footY = footY;
            this.height = height;
        }
    }

    /**
     * A piece of scenery. It is drawn behind the cat and it is in no collision list at all, which is
     * the point: a hanging vine that could hurt the cat would be a trap pretending to be decoration.
     */
    public static final class Decor {
        public final int kind;
        public final int variant;        // pipe fitting index; unused by the plants
        public final float centerX;
        public final float anchorY;      // the ceiling it hangs from, or the floor it stands on
        public final boolean hangs;

        Decor(int kind, int variant, float centerX, float anchorY, boolean hangs) {
            this.kind = kind;
            this.variant = variant;
            this.centerX = centerX;
            this.anchorY = anchorY;
            this.hangs = hangs;
        }
    }

    /**
     * @param levelNumber 1 to 4
     * @param cat         the cat's hitbox; traps and monsters keep this reference to see where the cat is
     * @param sink        where monsters put new hazards (fireballs)
     */
    public LevelMap(int levelNumber, LevelArt art, Rectangle cat, Monster.HazardSink sink) {
        this.data = LevelData.create(levelNumber);
        this.art = art;
        this.widthInTiles = data.width;
        this.heightInTiles = LevelData.ROWS;
        this.tileSize = LevelData.TILE_SIZE;
        this.tiles = new char[widthInTiles][heightInTiles];
        this.solidGrid = new boolean[widthInTiles][heightInTiles];
        this.topSolidRow = new int[widthInTiles];

        for (int row = 0; row < heightInTiles; row++) {
            for (int x = 0; x < widthInTiles; x++) {
                tiles[x][heightInTiles - 1 - row] = data.grid[row][x];
            }
        }
        readTiles(cat);
        findColumnTops();
        findGateColumns();
        mergeSolids();
        readPlacements(cat, sink);
    }

    /**
     * Records the highest solid tile in each column. The real artwork is a cross-section - a lit top
     * edge, then the beam, then brick - so how far a tile sits below its column's top decides which
     * slice of it to draw. See {@link SewerArt#drawPlatform} and {@link GreenhouseArt#drawPlatform}.
     */
    private void findColumnTops() {
        for (int x = 0; x < widthInTiles; x++) {
            int top = -1;
            for (int y = heightInTiles - 1; y >= 0; y--) {
                if (solidGrid[x][y]) { top = y; break; }
            }
            topSolidRow[x] = top;
        }
    }

    /**
     * Measures each gate's vertical run of tiles, so the shutter can be drawn as one door instead of a
     * stack of tiles. The collision boxes are built per tile in {@link #readTiles}; this is only about
     * the picture.
     */
    private void findGateColumns() {
        for (int x = 0; x < widthInTiles; x++) {
            int y = 0;
            while (y < heightInTiles) {
                char c = tiles[x][y];
                int pair = c == 'G' ? PAIR_A : c == 'g' ? PAIR_B : -1;
                if (pair < 0) { y++; continue; }
                int start = y;
                while (y < heightInTiles && tiles[x][y] == c) y++;
                gateColumns.add(new GateColumn(pair, x * tileSize + tileSize / 2f,
                    start * tileSize, (y - start) * tileSize));
            }
        }
    }

    // ------------------------------------------------------------------ tiles

    private void readTiles(Rectangle cat) {
        int ventIndex = 0;
        for (int y = 0; y < heightInTiles; y++) {
            for (int x = 0; x < widthInTiles; x++) {
                float px = x * tileSize, py = y * tileSize;
                switch (tiles[x][y]) {
                    case '#':
                        solidGrid[x][y] = true;
                        break;
                    case 'H':
                        solidGrid[x][y] = true;
                        vines.add(new Rectangle(px, py, tileSize, tileSize));
                        break;
                    case '|':
                        liquidPassables.add(new Rectangle(px, py, tileSize, tileSize));
                        break;
                    case '^':
                        hazards.add(new SpikeStrip(art, px, py, tileSize));
                        break;
                    case 'L':
                        hazards.add(new FireHole(art, px, py, tileSize));
                        break;
                    case 'F':
                        solidGrid[x][y] = true;
                        hazards.add(new FlameVent(art, px, py, tileSize, flameHeightAbove(x, y), ventIndex++));
                        break;
                    case 'I':
                        solidGrid[x][y] = true;
                        hazards.add(new FrostPipe(art, px, py, tileSize));
                        break;
                    case 'S':
                        // The lowest tile of a door column reaches down to the bottom of the map, or a
                        // cloud would drift under the door through the floor.
                        boolean lowest = y == 0 || tiles[x][y - 1] != 'S';
                        sealedDoors.add(new Rectangle(px, lowest ? 0f : py, tileSize, lowest ? py + tileSize : tileSize));
                        break;
                    case 'P':
                    case 'p':
                        // Not solid and not a hazard: a plate is something you stand in, and the whole
                        // of its effect is on the gate pair it holds open.
                        plates.add(new Plate(
                            new Rectangle(px, py, tileSize, PLATE_TRIGGER_HEIGHT),
                            tiles[x][y] == 'P' ? PAIR_A : PAIR_B));
                        break;
                    case 'G':
                    case 'g':
                        // Same trick as the sealed doors, for the same reason: the lowest tile of the
                        // column reaches down to the bottom of the map. Without it a gas cloud could
                        // drift under a shut gate through the floor rows - and a cat that unlocked the
                        // gas form on an earlier run can come back to this level with it.
                        boolean floorOfGate = y == 0 || tiles[x][y - 1] != tiles[x][y];
                        gateRects(tiles[x][y] == 'G' ? PAIR_A : PAIR_B).add(new Rectangle(
                            px, floorOfGate ? 0f : py, tileSize, floorOfGate ? py + tileSize : tileSize));
                        break;
                    case '~':
                        crumbles.add(new CrumbleBlock(art, px, py, tileSize, cat));
                        break;
                    default:
                        break;
                }
            }
        }
    }

    /** Flames reach 2 tiles up, or less if a solid tile is in the way (for example the low "hot crawl"). */
    private float flameHeightAbove(int x, int y) {
        int free = 0;
        for (int k = 1; k <= 2 && y + k < heightInTiles; k++) {
            char above = tiles[x][y + k];
            if (above == '#' || above == 'H') break;
            free++;
        }
        return Math.max(1, free) * tileSize;
    }

    /** Joins neighbouring solid tiles in a row into one long box: fewer boxes and no seams to snag on. */
    private void mergeSolids() {
        for (int y = 0; y < heightInTiles; y++) {
            int x = 0;
            while (x < widthInTiles) {
                if (!solidGrid[x][y]) { x++; continue; }
                int start = x;
                while (x < widthInTiles && solidGrid[x][y]) x++;
                terrainSolids.add(new Rectangle(start * tileSize, y * tileSize, (x - start) * tileSize, tileSize));
            }
        }
        // Invisible walls just outside the map so the cat cannot leave it.
        float h = heightInTiles * tileSize;
        terrainSolids.add(new Rectangle(-100f, 0f, 100f, h + 400f));
        terrainSolids.add(new Rectangle(getWidth(), 0f, 100f, h + 400f));

        // A gas cloud flies through all of the above, so it needs its own edges: all four sides.
        float w = getWidth();
        boundarySolids.add(new Rectangle(-100f, -400f, 100f, h + 800f));
        boundarySolids.add(new Rectangle(w, -400f, 100f, h + 800f));
        boundarySolids.add(new Rectangle(-100f, -100f, w + 200f, 100f));
        boundarySolids.add(new Rectangle(-100f, h, w + 200f, 100f));
    }

    // ------------------------------------------------------------------ placed objects

    private void readPlacements(Rectangle cat, Monster.HazardSink sink) {
        boolean hasSpawn = false;
        for (int i = 0; i < data.placements.size; i++) {
            LevelData.Placement p = data.placements.get(i);
            float left = p.col * tileSize;
            float cx = left + tileSize / 2f;                               // centre of the tile
            float bottom = (heightInTiles - 1 - p.row) * tileSize;         // bottom of the tile = the floor

            switch (p.type) {
                case "spawn":
                    spawn.set(cx, bottom);
                    hasSpawn = true;
                    break;
                case "checkpoint":
                    checkpoints.add(new Rectangle(left, bottom, tileSize, tileSize * 2f));
                    break;
                case "exit":
                    exit.set(left + 8f, bottom, tileSize - 16f, tileSize * 2f);
                    break;
                case "monster":
                    monsters.add(Monster.create(p.tier, art, this, cx, bottom, p.range * tileSize, cat, sink));
                    break;
                case "saw":
                    float cy = bottom + tileSize / 2f;
                    hazards.add(new SawBlade(art, cx, cy, cx + p.dx * tileSize, cy - p.dy * tileSize, p.period, p.phase));
                    break;
                case "fallingSpike":
                    hazards.add(new FallingSpike(art, this, cx, bottom + tileSize, cat));
                    break;
                case "brazier":
                    hazards.add(new Brazier(art, left, bottom, tileSize));
                    break;
                case "fan":
                    // (col, row) is the zone's top-left tile; the zone grows right and down
                    float fanBottom = (heightInTiles - (p.row + p.h)) * tileSize;
                    hazards.add(new Fan(art, left, fanBottom, p.w * tileSize, p.h * tileSize, p.dx, p.dy));
                    break;
                case "valve":
                    valve = new Rectangle(left, bottom, tileSize, tileSize);
                    break;
                case "star":
                    star = new Rectangle(left + 6f, bottom + 6f, tileSize - 12f, tileSize - 12f);
                    break;
                case "core":
                    // Level 2's collectible, in the same footing as the gas star so it looks and
                    // feels like one - but it unlocks nothing. See LevelWorld#updatePickups.
                    cores.add(new Rectangle(left + 6f, bottom + 6f, tileSize - 12f, tileSize - 12f));
                    break;
                case "hangvine":
                    decor.add(new Decor(DECOR_HANGVINE, 0, cx, bottom + tileSize, true));
                    break;
                case "glowplant":
                    decor.add(new Decor(DECOR_GLOWPLANT, 0, cx, bottom, false));
                    break;
                case "pipe":
                    decor.add(new Decor(DECOR_PIPE, p.tier, cx, bottom, false));
                    break;
                case "hint":
                    if (p.text != null) hints.add(new Hint(left, p.text));
                    break;
                default:
                    break;
            }
        }
        if (!hasSpawn) spawn.set(2.5f * tileSize, tileSize * 2f);
    }

    // ------------------------------------------------------------------ drawing

    /** Draws the tiles between {@code left} and {@code right} (world x). Call between batch.begin() and end(). */
    public void render(SpriteBatch batch, float left, float right) {
        int x0 = Math.max(0, (int) Math.floor(left / tileSize) - 1);
        int x1 = Math.min(widthInTiles - 1, (int) Math.ceil(right / tileSize) + 1);
        LevelTheme th = art.theme;
        float s = tileSize;
        for (int x = x0; x <= x1; x++) {
            for (int y = 0; y < heightInTiles; y++) {
                char c = tiles[x][y];
                float px = x * s, py = y * s;
                boolean openAbove = y + 1 >= heightInTiles || !solidGrid[x][y + 1];
                switch (c) {
                    case '#':
                        if (art.hasSewerArt()) {
                            // Depth below the top of this column picks the slice: lit edge, beam,
                            // truss, then brick. Purely a drawing choice - the collision boxes above
                            // are built from solidGrid and do not move.
                            art.sewer.drawPlatform(batch, topSolidRow[x] - y, px, py, s);
                        } else if (art.hasGreenhouseArt()) {
                            // Level 2's sheet holds loose blocks rather than a seamless section, so the
                            // joints between them would show the background through solid ground. An
                            // opaque rectangle underneath turns those joints into panel seams.
                            art.draw(batch, art.white, GreenhouseArt.BACKING, px, py, s, s);
                            art.greenhouse.drawPlatform(batch, topSolidRow[x] - y, px, py, s);
                        } else {
                            art.draw(batch, art.white, th.tile, px, py, s, s);
                            if (openAbove) art.draw(batch, art.white, th.edge, px, py + s - 6f, s, 6f);
                        }
                        break;
                    case 'H':
                        if (art.hasGreenhouseArt()) {
                            art.greenhouse.drawVine(batch, topSolidRow[x] - y, px, py, s);
                        } else {
                            art.draw(batch, art.white, th.vineDark, px, py, s, s);
                            art.draw(batch, art.triangleUp, th.vine, px + 16f, py + 8f, 32f, 18f);
                            art.draw(batch, art.triangleUp, th.vine, px + 16f, py + 36f, 32f, 18f);
                        }
                        break;
                    case '|':
                        if (art.hasGreenhouseArt()) {
                            // Level 2 is drawn over pale mist, and the mint bars level 1 uses sink into
                            // it. A dark recess behind them does the work instead - which also happens
                            // to be the honest picture: a hole with bars across it.
                            art.draw(batch, art.white, 0.04f, 0.09f, 0.10f, 0.62f, px, py, s, s);
                            for (int k = 0; k < 4; k++) {
                                art.draw(batch, art.white, th.bars, px + 5f + k * 15f, py, 9f, s);
                            }
                            if (y + 1 >= heightInTiles || tiles[x][y + 1] != '|') {
                                art.draw(batch, art.white, th.bars, px, py + s - 7f, s, 7f);
                            }
                        } else {
                            for (int k = 0; k < 3; k++) art.draw(batch, art.white, th.bars, px + 10f + k * 18f, py, 7f, s);
                            if (y + 1 >= heightInTiles || tiles[x][y + 1] != '|') art.draw(batch, art.white, th.bars, px, py + s - 6f, s, 6f);
                        }
                        break;
                    case '^':
                        if (art.hasSewerArt()) {
                            art.sewer.drawSpike(batch, px, py, s);
                        } else if (art.hasGreenhouseArt()) {
                            // A spike in the greenhouse is a plant: tall, glowing, and growing up out
                            // of the trench. The damage box is untouched, so this is only a new face.
                            art.greenhouse.drawSpike(batch, px, py, s);
                        } else {
                            for (int k = 0; k < 4; k++) art.draw(batch, art.triangleUp, th.hazard, px + k * 16f, py, 16f, 38f);
                        }
                        break;
                    case 'L':
                        art.draw(batch, art.white, 0.76f, 0.22f, 0.1f, 1f, px, py, s, 20f);
                        art.draw(batch, art.white, 1f, 0.42f, 0.17f, 1f, px, py + 20f, s, 28f);
                        art.draw(batch, art.white, 1f, 0.82f, 0.4f, 1f, px, py + 40f, s, 8f);
                        break;
                    case 'S':
                        if (valveOpen) break;                                   // the door is gone
                        art.draw(batch, art.white, 0.55f, 0.95f, 1f, 0.55f, px, py, s, s);
                        art.draw(batch, art.white, 0.85f, 1f, 1f, 1f, px, py, 5f, s);
                        art.draw(batch, art.white, 0.85f, 1f, 1f, 1f, px + s - 5f, py, 5f, s);
                        art.draw(batch, art.white, 0.85f, 1f, 1f, 0.9f, px + 22f, py + 6f, 6f, s - 12f);
                        break;
                    case 'I':
                        // frost pipe: pale blue with a bright ring, clearly not ordinary rock
                        art.draw(batch, art.white, 0.62f, 0.86f, 0.98f, 1f, px, py, s, s);
                        art.draw(batch, art.white, 0.95f, 1f, 1f, 1f, px + 8f, py + 8f, s - 16f, s - 16f);
                        art.draw(batch, art.white, 0.35f, 0.66f, 0.92f, 1f, px + 20f, py + 20f, s - 40f, s - 40f);
                        break;
                    case 'F':
                        art.draw(batch, art.white, 0.16f, 0.08f, 0.07f, 1f, px, py, s, s);
                        for (int k = 0; k < 3; k++) art.draw(batch, art.white, 1f, 0.62f, 0.26f, 1f, px + 12f + k * 16f, py + 36f, 6f, 20f);
                        art.draw(batch, art.white, th.edge, px, py + s - 4f, s, 4f);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    // ------------------------------------------------------------------ valve and sealed doors

    /** The valve has been turned: every sealed door disappears, for the cat and the cloud alike. */
    public void openSealedDoors() {
        valveOpen = true;
        sealedDoors.clear();
    }

    public boolean isValveOpen() {
        return valveOpen;
    }

    // ------------------------------------------------------------------ plates and gates

    /**
     * A plate of this pair has been held down: its gates stop existing, for the cat and the cloud
     * alike. Latched rather than momentary - the boxes are thrown away, so nothing that happens later
     * can shut the door again on a cat that walked through it.
     */
    public void openGates(int pair) {
        gateOpen[pair] = true;
        gateRects(pair).clear();
    }

    /** After a game over: put back the pairs that were already latched down. */
    public void restoreGates(int mask) {
        for (int pair = 0; pair < PLATE_PAIRS; pair++) {
            if ((mask & (1 << pair)) != 0) openGates(pair);
        }
    }

    /** Which pairs are latched down, as a bit per pair - what {@link CheckpointSave} remembers. */
    public int getGateMask() {
        int mask = 0;
        for (int pair = 0; pair < PLATE_PAIRS; pair++) {
            if (gateOpen[pair]) mask |= 1 << pair;
        }
        return mask;
    }

    public boolean isGateOpen(int pair) {
        return gateOpen[pair];
    }

    private Array<Rectangle> gateRects(int pair) {
        return pair == PAIR_B ? gatesB : gatesA;
    }

    // ------------------------------------------------------------------ queries

    /** True if (x, y) in world pixels is inside a tile that always blocks (solid, vine or vent). */
    public boolean isSolidAt(float x, float y) {
        int tx = (int) Math.floor(x / tileSize), ty = (int) Math.floor(y / tileSize);
        if (tx < 0 || ty < 0 || tx >= widthInTiles || ty >= heightInTiles) return false;
        return solidGrid[tx][ty];
    }

    public String getTitle() { return data.title; }
    public float getTileSize() { return tileSize; }
    public float getWidth() { return widthInTiles * tileSize; }
    public float getHeight() { return heightInTiles * tileSize; }
    public Array<Rectangle> getTerrainSolids() { return terrainSolids; }
    public Array<Rectangle> getLiquidPassables() { return liquidPassables; }
    public Array<Rectangle> getVines() { return vines; }
    /** Sealed doors that are still shut (empty once the valve is turned). */
    public Array<Rectangle> getSealedDoors() { return sealedDoors; }
    /** The gates of one plate pair that are still shut (empty once that pair is latched down). */
    public Array<Rectangle> getGates(int pair) { return gateRects(pair); }
    /** Every gate, shut or not, for drawing. */
    public Array<GateColumn> getGateColumns() { return gateColumns; }
    public Array<Plate> getPlates() { return plates; }
    /** Greenhouse cores not yet lifted. */
    public Array<Rectangle> getCores() { return cores; }
    /** Painted scenery: drawn behind the cat, in no collision list. */
    public Array<Decor> getDecor() { return decor; }
    /** The four edges of the map, the only things a gas cloud cannot pass. */
    public Array<Rectangle> getBoundarySolids() { return boundarySolids; }
    public Rectangle getValve() { return valve; }
    public Rectangle getStar() { return star; }
    public Array<Hazard> getHazards() { return hazards; }
    public Array<CrumbleBlock> getCrumbles() { return crumbles; }
    public Array<Monster> getMonsters() { return monsters; }
    public Array<Rectangle> getCheckpoints() { return checkpoints; }
    /** Teaching lines, each with the x the cat has to pass for it to appear. */
    public Array<Hint> getHints() { return hints; }
    /** Bottom-centre of the start tile. */
    public Vector2 getSpawn() { return spawn; }
    public Rectangle getExit() { return exit; }
}
