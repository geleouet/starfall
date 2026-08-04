package com.starfall.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

/**
 * Primitives de dessin au pixel-monde : rectangles pleins, contours d'un pixel, lignes de
 * Bresenham, sprites.
 *
 * <p>Tout passe par une texture blanche de 1x1 teintée à la volée, donc par le même {@link
 * SpriteBatch} que les sprites : aucun changement d'état GL entre un décor et une figure, et surtout
 * aucune rastérisation de ligne par le pilote — c'est nous qui posons chaque pixel, ce qui est la
 * seule façon d'obtenir le même résultat sur toutes les machines.
 */
public final class PixelPainter implements Disposable {

    private final SpriteBatch batch;
    private final Texture white;

    public PixelPainter(SpriteBatch batch) {
        this.batch = batch;
        this.white = solidTexture();
    }

    /** Texture blanche 1x1, pour les rares cas qui dessinent hors de ces primitives. */
    public Texture white() {
        return white;
    }

    public void color(Color color) {
        batch.setColor(color);
    }

    /** Rectangle plein. */
    public void fill(int x, int y, int width, int height, Color color) {
        batch.setColor(color);
        batch.draw(white, x, y, width, height);
    }

    /** Contour de rectangle large d'un pixel-monde. */
    public void outline(int x, int y, int width, int height, Color color) {
        if (width < 1 || height < 1) {
            return;
        }
        batch.setColor(color);
        batch.draw(white, x, y, width, 1);
        if (height < 2) {
            return; // un rectangle d'une seule ligne est déjà entièrement dessiné
        }
        batch.draw(white, x, y + height - 1, width, 1);
        if (height > 2) {
            batch.draw(white, x, y + 1, 1, height - 2);
            batch.draw(white, x + width - 1, y + 1, 1, height - 2);
        }
    }

    /** Ligne de Bresenham tracée un pixel-monde à la fois. */
    public void line(int x0, int y0, int x1, int y1, Color color) {
        batch.setColor(color);
        int dx = Math.abs(x1 - x0);
        int dy = -Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx + dy;
        int x = x0;
        int y = y0;
        while (true) {
            batch.draw(white, x, y, 1, 1);
            if (x == x1 && y == y1) {
                break;
            }
            int e2 = 2 * err;
            if (e2 >= dy) {
                err += dy;
                x += sx;
            }
            if (e2 <= dx) {
                err += dx;
                y += sy;
            }
        }
    }

    /** Dessine un sprite, coin bas-gauche en coordonnées monde, sans teinte. */
    public void sprite(TextureRegion region, int x, int y) {
        batch.setColor(Color.WHITE);
        batch.draw(region, x, y, region.getRegionWidth(), region.getRegionHeight());
    }

    /**
     * Dessine un sprite en le retournant horizontalement.
     *
     * <p>Le retournement se fait sur la région, pas sur les coordonnées : la position du coin reste
     * entière, donc le sprite retourné tombe exactement sur les mêmes pixels que le sprite normal.
     */
    public void spriteFlipped(TextureRegion region, int x, int y) {
        batch.setColor(Color.WHITE);
        batch.draw(region,
                x + region.getRegionWidth(), y,
                -region.getRegionWidth(), region.getRegionHeight());
    }

    private static Texture solidTexture() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pixmap.dispose();
        return texture;
    }

    @Override
    public void dispose() {
        white.dispose();
    }
}
