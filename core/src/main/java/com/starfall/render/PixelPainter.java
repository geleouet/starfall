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
        sprite(region, x, y, false, Color.WHITE);
    }

    /**
     * Dessine un sprite, retourné ou non, teinté ou non.
     *
     * <p>Les trois formes courtes ci-dessus s'y ramènent toutes. Le retournement et la teinte
     * étaient auparavant deux méthodes qui ne se combinaient pas — il n'existait aucun moyen de
     * dessiner une figure à la fois tournée vers la gauche et en train de s'effacer, ce que le
     * déroulé d'une salve demande à chaque ennemi qui tombe. Les écrire séparément aurait donné
     * quatre méthodes pour une seule règle de retournement, donc quatre endroits où elle pouvait
     * diverger.
     *
     * <p>La teinte porte aussi l'<b>opacité</b> : son canal alpha traverse jusqu'au batch, qui
     * mélange. C'est ce qui permet à une figure de disparaître en fondu plutôt que d'être retirée
     * d'une image à l'autre.
     */
    public void sprite(TextureRegion region, int x, int y, boolean flip, Color tint) {
        batch.setColor(tint);
        if (flip) {
            // Le retournement se fait sur la région, pas sur les coordonnées : la position du coin
            // reste entière, donc le sprite retourné tombe exactement sur les mêmes pixels que le
            // sprite normal.
            batch.draw(region, x + region.getRegionWidth(), y,
                    -region.getRegionWidth(), region.getRegionHeight());
        } else {
            batch.draw(region, x, y, region.getRegionWidth(), region.getRegionHeight());
        }
        batch.setColor(Color.WHITE);
    }

    /**
     * Dessine un sprite teinté.
     *
     * <p>Sert à éteindre une tuile indisponible sans la faire disparaître : le joueur doit
     * continuer à voir <em>quelle</em> tuile lui manque, pas seulement qu'il lui en manque une.
     */
    public void spriteTinted(TextureRegion region, int x, int y, Color tint) {
        sprite(region, x, y, false, tint);
    }

    /** Dessine un sprite en le retournant horizontalement. */
    public void spriteFlipped(TextureRegion region, int x, int y) {
        sprite(region, x, y, true, Color.WHITE);
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
