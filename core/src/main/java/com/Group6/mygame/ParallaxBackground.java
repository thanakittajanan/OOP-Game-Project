package com.Group6.mygame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

/**
 * A two-layer scrolling background. Each layer slides past more slowly than the gameplay layer (the
 * far one slowest), which gives the level its sense of depth. A layer repeats forever by mirroring
 * its image, so the seam between copies never shows. Owns its textures; dispose it with the level.
 */
public class ParallaxBackground implements Disposable {
    private static final float VIEW_W = Main.WORLD_WIDTH;
    private static final float VIEW_H = Main.WORLD_HEIGHT;

    /** One image, drawn on forever by mirroring it. */
    private static final class Layer {
        final TextureRegion region;
        final float speed;      // how fast it slides past compared with the gameplay layer
        final float tileW;      // world width of one copy

        Layer(TextureRegion region, float speed, float tileW) {
            this.region = region;
            this.speed = speed;
            this.tileW = tileW;
        }
    }

    private final Array<Layer> layers = new Array<>();
    private final Array<Texture> textures = new Array<>();

    /**
     * @param farPng    the furthest image, drawn first
     * @param farSpeed  its speed, e.g. 0.15
     * @param midPng    the image in front of it
     * @param midSpeed  its speed, e.g. 0.4
     */
    public ParallaxBackground(String farPng, float farSpeed, String midPng, float midSpeed) {
        addLayer(farPng, farSpeed);
        addLayer(midPng, midSpeed);
    }

    private void addLayer(String png, float speed) {
        Texture t = new Texture(png);
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        textures.add(t);
        float tileW = Math.round(t.getWidth() * (VIEW_H / t.getHeight()));
        layers.add(new Layer(new TextureRegion(t), speed, tileW));
    }

    public void render(SpriteBatch batch, float cameraX, float cameraY) {
        float bottom = cameraY - VIEW_H / 2f;
        batch.setColor(Color.WHITE);
        for (int l = 0; l < layers.size; l++) {
            Layer layer = layers.get(l);
            float origin = Math.round(cameraX * (1f - layer.speed));
            float viewLeft = cameraX - VIEW_W / 2f;
            float viewRight = cameraX + VIEW_W / 2f;
            int first = (int) Math.floor((viewLeft - origin) / layer.tileW);
            int last = (int) Math.floor((viewRight - origin) / layer.tileW);
            for (int i = first; i <= last; i++) {
                float x = origin + i * layer.tileW;
                if ((i & 1) == 0) {
                    batch.draw(layer.region, x, bottom, layer.tileW, VIEW_H);
                } else {
                    // Negative width draws the copy mirrored, so the join has no step in it.
                    batch.draw(layer.region, x + layer.tileW, bottom, -layer.tileW, VIEW_H);
                }
            }
        }
        batch.setColor(Color.WHITE);
    }

    @Override
    public void dispose() {
        for (int i = 0; i < textures.size; i++) textures.get(i).dispose();
        textures.clear();
        layers.clear();
    }
}
