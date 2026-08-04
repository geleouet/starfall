package com.starfall.art;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Placement des sprites dans l'atlas. Arithmétique pure, sans image ni fichier.
 *
 * <p>Rangement en étagères : les sprites sont triés par hauteur décroissante puis par nom, et posés
 * de gauche à droite ; quand la ligne est pleine on passe à l'étagère suivante. C'est loin d'être le
 * rangement le plus dense, mais il est court à lire, et avec quelques dizaines de sprites la place
 * perdue n'a aucune importance.
 *
 * <p><b>Le tri est ce qui rend le résultat déterministe.</b> Deux exécutions sur les mêmes sources
 * doivent produire un atlas identique octet pour octet : sans cela chaque build ferait bouger les
 * captures de review, et un vrai changement visuel se noierait dans le bruit.
 */
public final class AtlasLayout {

    /**
     * Marge transparente autour de chaque sprite. Le rendu se fait au filtre « plus proche voisin »
     * et à une échelle entière, donc rien ne devrait baver ; cette marge est une assurance bon
     * marché contre un pixel emprunté au voisin en cas d'erreur d'arrondi de coordonnées.
     */
    public static final int PADDING = 1;

    /** Emplacement d'un sprite dans l'atlas, en pixels, origine en haut à gauche. */
    public record Placement(String name, int x, int y, int width, int height) {

        public int right() {
            return x + width;
        }

        public int bottom() {
            return y + height;
        }

        public boolean overlaps(Placement other) {
            return x < other.right() && other.x < right()
                    && y < other.bottom() && other.y < bottom();
        }
    }

    private final List<Placement> placements;
    private final int width;
    private final int height;

    private AtlasLayout(List<Placement> placements, int width, int height) {
        this.placements = List.copyOf(placements);
        this.width = width;
        this.height = height;
    }

    /**
     * Range les sprites.
     *
     * @param sprites  sprites à placer, dans n'importe quel ordre
     * @param maxWidth largeur maximale de l'atlas, en pixels
     */
    public static AtlasLayout pack(List<SpriteSource> sprites, int maxWidth) {
        if (sprites.isEmpty()) {
            throw new ArtFormatException("aucun sprite à ranger dans l'atlas");
        }

        List<SpriteSource> sorted = new ArrayList<>(sprites);
        sorted.sort(Comparator.comparingInt(SpriteSource::height).reversed()
                .thenComparing(SpriteSource::name));

        for (SpriteSource sprite : sorted) {
            int needed = sprite.width() + 2 * PADDING;
            if (needed > maxWidth) {
                throw new ArtFormatException("le sprite « " + sprite.name() + " » fait "
                        + sprite.width() + " px de large, ce qui dépasse la largeur d'atlas de " + maxWidth + " px");
            }
        }

        List<Placement> placements = new ArrayList<>();
        int cursorX = 0;
        int shelfY = 0;
        int shelfHeight = 0;
        int usedWidth = 0;

        for (SpriteSource sprite : sorted) {
            int boxWidth = sprite.width() + 2 * PADDING;
            int boxHeight = sprite.height() + 2 * PADDING;

            if (cursorX + boxWidth > maxWidth) {
                shelfY += shelfHeight;
                shelfHeight = 0;
                cursorX = 0;
            }

            placements.add(new Placement(sprite.name(),
                    cursorX + PADDING, shelfY + PADDING,
                    sprite.width(), sprite.height()));

            cursorX += boxWidth;
            shelfHeight = Math.max(shelfHeight, boxHeight);
            usedWidth = Math.max(usedWidth, cursorX);
        }

        return new AtlasLayout(placements, nextPowerOfTwo(usedWidth), nextPowerOfTwo(shelfY + shelfHeight));
    }

    /** Emplacements, dans l'ordre déterministe du rangement. */
    public List<Placement> placements() {
        return placements;
    }

    /** Largeur de l'atlas, arrondie à la puissance de deux supérieure. */
    public int width() {
        return width;
    }

    /** Hauteur de l'atlas, arrondie à la puissance de deux supérieure. */
    public int height() {
        return height;
    }

    /**
     * Les dimensions sont arrondies à une puissance de deux : ce n'est plus obligatoire sur les
     * machines visées, mais c'est gratuit et cela évite les surprises sur les pilotes anciens.
     */
    private static int nextPowerOfTwo(int value) {
        int result = 1;
        while (result < value) {
            result <<= 1;
        }
        return result;
    }

    @Override
    public String toString() {
        return "AtlasLayout{" + placements.size() + " sprites, " + width + "x" + height + "}";
    }
}
