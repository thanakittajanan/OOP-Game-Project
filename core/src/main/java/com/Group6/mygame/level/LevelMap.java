package com.Group6.mygame.level;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/**
 * Builds one level from {@link LevelData}: collision boxes, bars, vines, traps, monsters, checkpoints,
 * spawn and exit. It also draws the tiles.
 *
 * <p>Coordinates: LevelData counts rows from the TOP. libGDX uses y-up, so here tile (0, 0) is the
 * BOTTOM-left tile and {@code y = ROWS - 1 - row}.
 */
public class LevelMap {
    private final LevelData data;
    private final LevelArt art;
    private final int widthInTiles, heightInTiles;
    private final float tileSize;
    private final char[][] tiles;         // [x][y], y-up
    private final boolean[][] solidGrid;  // [x][y] true where a tile always blocks (solid, vine, vent)

    private final Array<Rectangle> terrainSolids = new Array<>();   // merged into long strips (fewer boxes, no seams)
    private final Array<Rectangle> liquidPassables = new Array<>(); // bars and grates: solid for the standing cat only
    private final Array<Rectangle> vines = new Array<>();           // climbable walls
    private final Array<Hazard> hazards = new Array<>();
    private final Array<CrumbleBlock> crumbles = new Array<>();
    private final Array<Monster> monsters = new Array<>();
    private final Array<Rectangle> checkpoints = new Array<>();
    private final Vector2 spawn = new Vector2();
    private final Rectangle exit = new Rectangle();

    /**
     * @param levelNumber 1, 2 or 3
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

        for (int row = 0; row < heightInTiles; row++) {
            for (int x = 0; x < widthInTiles; x++) {
                tiles[x][heightInTiles - 1 - row] = data.grid[row][x];
            }
        }
        readTiles(cat);
        mergeSolids();
        readPlacements(cat, sink);
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
                        art.draw(batch, art.white, th.tile, px, py, s, s);
                        if (openAbove) art.draw(batch, art.white, th.edge, px, py + s - 6f, s, 6f);
                        break;
                    case 'H':
                        art.draw(batch, art.white, th.vineDark, px, py, s, s);
                        art.draw(batch, art.triangleUp, th.vine, px + 16f, py + 8f, 32f, 18f);
                        art.draw(batch, art.triangleUp, th.vine, px + 16f, py + 36f, 32f, 18f);
                        break;
                    case '|':
                        for (int k = 0; k < 3; k++) art.draw(batch, art.white, th.bars, px + 10f + k * 18f, py, 7f, s);
                        if (y + 1 >= heightInTiles || tiles[x][y + 1] != '|') art.draw(batch, art.white, th.bars, px, py + s - 6f, s, 6f);
                        break;
                    case '^':
                        for (int k = 0; k < 4; k++) art.draw(batch, art.triangleUp, th.hazard, px + k * 16f, py, 16f, 38f);
                        break;
                    case 'L':
                        art.draw(batch, art.white, 0.76f, 0.22f, 0.1f, 1f, px, py, s, 20f);
                        art.draw(batch, art.white, 1f, 0.42f, 0.17f, 1f, px, py + 20f, s, 28f);
                        art.draw(batch, art.white, 1f, 0.82f, 0.4f, 1f, px, py + 40f, s, 8f);
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
    public Array<Hazard> getHazards() { return hazards; }
    public Array<CrumbleBlock> getCrumbles() { return crumbles; }
    public Array<Monster> getMonsters() { return monsters; }
    public Array<Rectangle> getCheckpoints() { return checkpoints; }
    /** Bottom-centre of the start tile. */
    public Vector2 getSpawn() { return spawn; }
    public Rectangle getExit() { return exit; }
}
