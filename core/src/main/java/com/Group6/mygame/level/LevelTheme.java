package com.Group6.mygame.level;

import com.badlogic.gdx.graphics.Color;

/** Colours for one level: the tiles and the traps. Level 1 is blue, level 2 green, level 3 red, level 4 violet. */
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

    /** levelNumber is 1 to 4. */
    public static LevelTheme forLevel(int levelNumber) {
        switch (levelNumber) {
            case 2:
                // The greenhouse: deep emerald and dark teal over steel, with a neon mint accent. The
                // bars stay pale on purpose - "mint" is how every level says "a puddle fits through
                // here", and the cat should not have to relearn that in level 2. The hazard colour
                // stays warm, because a warm spike against a green world is the one thing that cannot
                // be mistaken for scenery.
                return new LevelTheme("256b55", "4fb98a", "3f8f5f", "1c4a38", "a8e6c8", "f2876f", "5ff0c8");
            case 3:
                return new LevelTheme("d85c46", "ffa48f", "f0a070", "b4643c", "ffc9b8", "ffd166", "ffd166");
            case 4:
                return new LevelTheme("7a6fd0", "bdb4f7", "9aa8f0", "4b4596", "cfd6ff", "ff8fb0", "9ff0ff");
            default:
                return new LevelTheme("6f97e6", "a9c4f7", "8fb0f0", "3f62a8", "b9d0ff", "f2876f", "8fdcf2");
        }
    }
}
