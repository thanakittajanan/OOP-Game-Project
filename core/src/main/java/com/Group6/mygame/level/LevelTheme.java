package com.Group6.mygame.level;

import com.badlogic.gdx.graphics.Color;

/** Colours for one level: the tiles and the traps. Level 1 is blue, level 2 green, level 3 red. */
public final class LevelTheme {
    public final Color tile;       // solid blocks
    public final Color edge;       // light top edge of blocks
    public final Color vine;       // vine leaves
    public final Color vineDark;   // vine wall
    public final Color bars;       // bars and grates
    public final Color hazard;     // spikes, monster horns
    public final Color accent;     // checkpoint flags

    private LevelTheme(String tile, String edge, String vine, String vineDark, String bars, String hazard, String accent) {
        this.tile = Color.valueOf(tile);
        this.edge = Color.valueOf(edge);
        this.vine = Color.valueOf(vine);
        this.vineDark = Color.valueOf(vineDark);
        this.bars = Color.valueOf(bars);
        this.hazard = Color.valueOf(hazard);
        this.accent = Color.valueOf(accent);
    }

    /** levelNumber is 1, 2 or 3. */
    public static LevelTheme forLevel(int levelNumber) {
        switch (levelNumber) {
            case 2:
                return new LevelTheme("4fbf86", "93e8bb", "86e2ab", "3c9b68", "b8f2d2", "f2876f", "8fdcf2");
            case 3:
                return new LevelTheme("d85c46", "ffa48f", "f0a070", "b4643c", "ffc9b8", "ffd166", "ffd166");
            default:
                return new LevelTheme("6f97e6", "a9c4f7", "8fb0f0", "3f62a8", "b9d0ff", "f2876f", "8fdcf2");
        }
    }
}
