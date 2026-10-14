package com.Group6.mygame.level;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

/**
 * Simple shapes for traps and monsters, drawn into textures at start-up with {@link Pixmap}.
 * They are placeholders: swap them for real sprites later without touching the trap logic.
 *
 * <p>Every shape is white, so it can be tinted with {@code batch.setColor(...)}.
 */
public class LevelArt implements Disposable {
    public final TextureRegion white;
    public final TextureRegion circle;
    public final TextureRegion triangleUp;
    public final TextureRegion triangleDown;
    public final TextureRegion saw;
    public final TextureRegion flame;
    public final TextureRegion glow;
    public final TextureRegion star;
    public final LevelTheme theme;

    private final Array<Texture> textures = new Array<>();

    public LevelArt(LevelTheme theme) {
        this.theme = theme;

        Pixmap p = new Pixmap(4, 4, Pixmap.Format.RGBA8888);
        p.setColor(Color.WHITE);
        p.fill();
        white = keep(p);

        p = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        p.setColor(Color.WHITE);
        p.fillCircle(32, 32, 31);
        circle = keep(p);

        // Pixmap y goes DOWN, so "up" means the point is at row 0.
        p = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        p.setColor(Color.WHITE);
        p.fillTriangle(0, 63, 32, 0, 63, 63);
        triangleUp = keep(p);

        p = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        p.setColor(Color.WHITE);
        p.fillTriangle(0, 0, 63, 0, 32, 63);
        triangleDown = keep(p);

        p = new Pixmap(96, 96, Pixmap.Format.RGBA8888);
        p.setColor(Color.WHITE);
        p.fillCircle(48, 48, 36);
        int teeth = 10;
        for (int i = 0; i < teeth; i++) {
            double a = i * Math.PI * 2 / teeth;
            double b = a + Math.PI / teeth;
            p.fillTriangle(48 + (int) (Math.cos(a) * 34), 48 + (int) (Math.sin(a) * 34),
                48 + (int) (Math.cos(b) * 47), 48 + (int) (Math.sin(b) * 47),
                48 + (int) (Math.cos(a + 2 * Math.PI / teeth) * 34), 48 + (int) (Math.sin(a + 2 * Math.PI / teeth) * 34));
        }
        p.setColor(0.35f, 0.38f, 0.45f, 1f);
        p.fillCircle(48, 48, 12);
        saw = keep(p);

        p = new Pixmap(32, 64, Pixmap.Format.RGBA8888);
        p.setColor(Color.WHITE);
        p.fillCircle(16, 48, 15);
        p.fillTriangle(1, 48, 31, 48, 16, 0);
        flame = keep(p);

        p = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                float d = (float) Math.sqrt((x - 31.5) * (x - 31.5) + (y - 31.5) * (y - 31.5)) / 32f;
                float a = Math.max(0f, 1f - d);
                p.setColor(1f, 1f, 1f, a * a);
                p.drawPixel(x, y);
            }
        }
        glow = keep(p);

        // five-pointed star: ten triangles fanned from the centre, alternating outer and inner points
        p = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        p.setColor(Color.WHITE);
        int cx = 32, cy = 34;
        for (int i = 0; i < 10; i++) {
            double a1 = -Math.PI / 2 + i * Math.PI / 5;
            double a2 = a1 + Math.PI / 5;
            double r1 = (i % 2 == 0) ? 30 : 12;
            double r2 = (i % 2 == 0) ? 12 : 30;
            p.fillTriangle(cx, cy,
                (int) Math.round(cx + Math.cos(a1) * r1), (int) Math.round(cy + Math.sin(a1) * r1),
                (int) Math.round(cx + Math.cos(a2) * r2), (int) Math.round(cy + Math.sin(a2) * r2));
        }
        star = keep(p);
    }

    private TextureRegion keep(Pixmap pixmap) {
        Texture t = new Texture(pixmap);
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        textures.add(t);
        return new TextureRegion(t);
    }

    /** Draws a tinted region, then resets the batch colour so nothing else gets tinted by mistake. */
    public void draw(SpriteBatch batch, TextureRegion region, Color color, float x, float y, float w, float h) {
        batch.setColor(color);
        batch.draw(region, x, y, w, h);
        batch.setColor(Color.WHITE);
    }

    public void draw(SpriteBatch batch, TextureRegion region, float r, float g, float b, float a,
                     float x, float y, float w, float h) {
        batch.setColor(r, g, b, a);
        batch.draw(region, x, y, w, h);
        batch.setColor(Color.WHITE);
    }

    @Override
    public void dispose() {
        for (int i = 0; i < textures.size; i++) textures.get(i).dispose();
        textures.clear();
    }
}
