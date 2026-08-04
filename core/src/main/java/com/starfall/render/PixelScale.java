package com.starfall.render;

/**
 * Mathématiques pures, sans GL, derrière {@link PixelViewport}.
 *
 * <p>À partir d'une taille de fenêtre en pixels écran et d'une zone minimale qui doit toujours
 * rester visible (exprimée en pixels-monde, c'est-à-dire la résolution à laquelle les sprites sont
 * dessinés), cette classe calcule :
 *
 * <ul>
 *   <li>le plus grand facteur de zoom <em>entier</em> auquel la zone minimale tient encore ;</li>
 *   <li>la <b>zone sûre</b> : les pixels-monde entièrement visibles à l'écran, jamais rognés. Elle
 *       est toujours au moins aussi grande que la zone minimale demandée ;</li>
 *   <li>la zone à dessiner, qui déborde de la zone sûre d'une colonne et d'une ligne de chaque côté
 *       quand la fenêtre ne tombe pas juste, afin de remplir la gouttière restante ;</li>
 *   <li>le rectangle de viewport GL correspondant.</li>
 * </ul>
 *
 * <h2>Pourquoi une zone sûre distincte de la zone dessinée</h2>
 *
 * <p>Une fenêtre dont la taille n'est pas un multiple du zoom laisse un reliquat de moins d'un
 * pixel-monde. Le remplir en agrandissant simplement le monde d'une colonne puis en centrant le
 * tout revient à couper en deux la colonne de chaque bord — et rien ne garantit alors que la
 * colonne coupée soit en dehors de la zone garantie. C'était le cas par exemple en 1281x720 :
 * la colonne monde x=0, c'est-à-dire le bord même de la zone garantie, n'était affichée que sur
 * 2 pixels écran au lieu de 4.
 *
 * <p>La zone sûre lève l'ambiguïté : elle est posée sur la grille des pixels écran à un décalage
 * positif, donc <b>entièrement</b> dans la fenêtre, et les colonnes de débordement qui bouchent les
 * gouttières sont ajoutées <em>en dehors</em> d'elle. Tout ce qui compte pour le jeu tient dans la
 * zone sûre ; le débordement ne sert qu'à ce qu'aucun pixel de la fenêtre ne reste noir.
 *
 * <p>Zoom et origine étant tous deux entiers, chaque pixel-monde couvre exactement {@code scale}
 * pixels écran et tombe pile sur la grille écran.
 */
public final class PixelScale {

    /**
     * Borne haute des dimensions acceptées, en pixels. Sert uniquement à garder toute
     * l'arithmétique loin du débordement entier : aucune fenêtre ni aucun monde réel n'en approche.
     */
    public static final int MAX_DIMENSION = 1 << 20;

    /** Nombre de pixels écran par pixel-monde. Toujours >= 1. */
    public final int scale;

    /** Largeur de la zone sûre, en pixels-monde entièrement visibles. */
    public final int safeWorldWidth;
    /** Hauteur de la zone sûre, en pixels-monde entièrement visibles. */
    public final int safeWorldHeight;

    /** Largeur de la zone dessinée, en pixels-monde. Vaut la zone sûre, plus le débordement. */
    public final int worldWidth;
    /** Hauteur de la zone dessinée, en pixels-monde. Vaut la zone sûre, plus le débordement. */
    public final int worldHeight;

    /** Colonnes de débordement à gauche de la zone sûre : 0 ou 1. Autant à droite. */
    public final int bleedX;
    /** Lignes de débordement sous la zone sûre : 0 ou 1. Autant au-dessus. */
    public final int bleedY;

    /** Origine X du viewport GL, en pixels écran. Nulle ou négative (débordement). */
    public final int screenX;
    /** Origine Y du viewport GL, en pixels écran. Nulle ou négative (débordement). */
    public final int screenY;
    /** Largeur du viewport GL, en pixels écran. Vaut {@code worldWidth * scale}. */
    public final int screenWidth;
    /** Hauteur du viewport GL, en pixels écran. Vaut {@code worldHeight * scale}. */
    public final int screenHeight;

    private PixelScale(int scale,
                       int safeWorldWidth, int safeWorldHeight,
                       int worldWidth, int worldHeight,
                       int bleedX, int bleedY,
                       int screenX, int screenY, int screenWidth, int screenHeight) {
        this.scale = scale;
        this.safeWorldWidth = safeWorldWidth;
        this.safeWorldHeight = safeWorldHeight;
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.bleedX = bleedX;
        this.bleedY = bleedY;
        this.screenX = screenX;
        this.screenY = screenY;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    /**
     * Calcule la disposition pixel-perfect d'une fenêtre.
     *
     * @param windowWidth    largeur de la fenêtre en pixels écran (ramenée dans [1, MAX_DIMENSION])
     * @param windowHeight   hauteur de la fenêtre en pixels écran (ramenée dans [1, MAX_DIMENSION])
     * @param minWorldWidth  largeur de monde toujours visible, en pixels-monde (au moins 1)
     * @param minWorldHeight hauteur de monde toujours visible, en pixels-monde (au moins 1)
     */
    public static PixelScale compute(int windowWidth, int windowHeight,
                                     int minWorldWidth, int minWorldHeight) {
        int w = clampDimension(windowWidth);
        int h = clampDimension(windowHeight);
        int minW = clampDimension(minWorldWidth);
        int minH = clampDimension(minWorldHeight);

        // Plus grand zoom entier auquel la zone garantie tient encore sur les deux axes.
        // Jamais sous 1 : sur une fenêtre plus petite que la zone garantie on reste en 1:1 et on
        // montre moins de monde, plutôt que de produire des pixels fractionnaires (scintillants).
        int scale = Math.max(1, Math.min(w / minW, h / minH));

        Axis x = Axis.solve(w, scale);
        Axis y = Axis.solve(h, scale);

        return new PixelScale(scale,
                x.safeWorld, y.safeWorld,
                x.world, y.world,
                x.bleed, y.bleed,
                x.screenOrigin, y.screenOrigin,
                x.world * scale, y.world * scale);
    }

    /**
     * Résolution d'un seul axe. Les deux axes sont indépendants une fois le zoom choisi, ce qui
     * évite de dupliquer six lignes d'arithmétique délicate.
     */
    private record Axis(int safeWorld, int world, int bleed, int screenOrigin) {

        static Axis solve(int windowSize, int scale) {
            // Pixels-monde qui tiennent entièrement dans la fenêtre. Par construction du zoom,
            // c'est toujours au moins la taille minimale demandée (sauf fenêtre sous-dimensionnée,
            // où le zoom a été remonté de force à 1 et où l'on montre simplement moins de monde).
            int safeWorld = windowSize / scale;
            int leftover = windowSize - safeWorld * scale; // dans [0, scale[

            if (leftover == 0) {
                // La fenêtre tombe juste : rien à boucher, la zone sûre est le monde entier.
                return new Axis(safeWorld, safeWorld, 0, 0);
            }

            // La zone sûre est posée à un décalage positif, donc entièrement dans la fenêtre ;
            // une colonne de débordement de chaque côté couvre les deux gouttières restantes.
            int offset = leftover / 2;
            return new Axis(safeWorld, safeWorld + 2, 1, offset - scale);
        }
    }

    private static int clampDimension(int value) {
        return Math.min(MAX_DIMENSION, Math.max(1, value));
    }

    @Override
    public String toString() {
        return "PixelScale{scale=" + scale
                + ", safe=" + safeWorldWidth + "x" + safeWorldHeight
                + ", drawn=" + worldWidth + "x" + worldHeight
                + ", viewport=" + screenWidth + "x" + screenHeight
                + " @ " + screenX + "," + screenY + '}';
    }
}
