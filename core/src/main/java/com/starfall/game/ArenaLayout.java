package com.starfall.game;

/**
 * Géométrie de l'arène : où tombe chaque case, en pixels-monde.
 *
 * <p>Arithmétique pure, sans GL, pour que le cadrage soit vérifiable par des tests plutôt que par
 * un coup d'œil sur une capture.
 *
 * <h2>Le cadrage selon la largeur</h2>
 *
 * <p>La grille fait de 5 à 15 cases, soit de 100 à 300 pixels-monde de large. La zone garantie du
 * viewport en fait 320 : <b>toute grille tient donc entièrement dans la zone garantie, quelle que
 * soit sa largeur et quelle que soit la taille de la fenêtre.</b> La caméra reste simplement centrée
 * sur le milieu de la grille, et une fenêtre plus large ne fait que montrer davantage de décor
 * autour — jamais moins de grille.
 *
 * <p>C'est la raison du choix d'une case de {@value #CELL_WIDTH} px : 15 cases y tiennent avec de la
 * marge, là où 24 px auraient débordé et obligé la caméra à couper une case ou à changer de zoom
 * selon la partie.
 */
public final class ArenaLayout {

    /** Largeur d'une case, en pixels-monde. Un personnage en fait 16 : il reste 2 px de chaque côté. */
    public static final int CELL_WIDTH = 20;
    /** Hauteur de la dalle de sol, en pixels-monde. */
    public static final int GROUND_HEIGHT = 8;
    /**
     * Bord bas des dalles de sol, en pixels-monde.
     *
     * <p>L'arène est haut placée pour laisser sous elle la bande où vivent la file d'actions et le
     * râtelier. Les deux doivent être visibles <em>en même temps</em> que le plateau : c'est en les
     * comparant du regard que le joueur décide.
     */
    public static final int GROUND_Y = 60;
    /** Bord bas des figures : elles posent les pieds sur la dalle. */
    public static final int FIGURE_Y = GROUND_Y + GROUND_HEIGHT;
    /** Largeur d'une figure, en pixels-monde. */
    public static final int FIGURE_WIDTH = 16;
    /** Hauteur d'une figure, en pixels-monde. */
    public static final int FIGURE_HEIGHT = 32;

    private final int gridWidth;
    private final int centreX;

    /**
     * @param gridWidth   nombre de cases
     * @param worldCentre abscisse du centre de la grille, en pixels-monde
     */
    public ArenaLayout(int gridWidth, int worldCentre) {
        if (gridWidth < Grid.MIN_WIDTH || gridWidth > Grid.MAX_WIDTH) {
            throw new IllegalArgumentException("Largeur de grille hors bornes : " + gridWidth);
        }
        this.gridWidth = gridWidth;
        this.centreX = worldCentre;
    }

    public int gridWidth() {
        return gridWidth;
    }

    /** Largeur totale de la grille, en pixels-monde. */
    public int pixelWidth() {
        return gridWidth * CELL_WIDTH;
    }

    /**
     * Bord gauche de la grille, en pixels-monde.
     *
     * <p>Entier par construction : la demi-largeur d'une case vaut {@value #CELL_WIDTH}/2, donc le
     * centrage ne produit jamais de demi-pixel — ce qui ferait scintiller toute l'arène.
     */
    public int left() {
        return centreX - gridWidth * (CELL_WIDTH / 2);
    }

    /** Bord droit (exclu) de la grille, en pixels-monde. */
    public int right() {
        return left() + pixelWidth();
    }

    /** Bord gauche d'une case, en pixels-monde. */
    public int cellLeft(int index) {
        return left() + index * CELL_WIDTH;
    }

    /** Bord gauche de la figure posée sur une case : centrée dans sa case. */
    public int figureLeft(int index) {
        return cellLeft(index) + (CELL_WIDTH - FIGURE_WIDTH) / 2;
    }

    /** Abscisse du centre de la grille, celle sur laquelle la caméra reste calée. */
    public int cameraTargetX() {
        return centreX;
    }

    /** Bord bas de la bande sensible au pointage : un peu sous les dalles, pour viser large. */
    public static final int PICK_BOTTOM = GROUND_Y - 8;
    /** Bord haut de la bande sensible : au-dessus des têtes, sans plus. */
    public static final int PICK_TOP = FIGURE_Y + FIGURE_HEIGHT + 4;

    /**
     * Case sous une abscisse du monde, ou {@code -1} en dehors de la grille.
     *
     * <p>Le calcul utilise un plancher plutôt qu'une division entière : à gauche du bord,
     * {@code -3 / 20} vaudrait 0 et désignerait la première case, alors que le curseur est hors de
     * la grille.
     */
    public int cellAt(float worldX) {
        float offset = worldX - left();
        if (offset < 0) {
            return -1;
        }
        int index = (int) (offset / CELL_WIDTH);
        return index < gridWidth ? index : -1;
    }

    /**
     * Case sous un point du monde, ou {@code -1} en dehors de la bande de jeu.
     *
     * <p>C'est la version à utiliser pour la souris. Ne tenir compte que de l'abscisse rendait
     * <b>toute</b> la colonne d'une case cliquable, du bas de la fenêtre jusqu'en haut : un clic
     * dans le ciel déplaçait le héros, et surtout un clic sur le bandeau d'interface le traversait.
     * La bande verticale reste généreuse — viser une case ne doit pas demander de la précision —
     * mais elle a un bord.
     */
    public int cellAt(float worldX, float worldY) {
        if (worldY < PICK_BOTTOM || worldY > PICK_TOP) {
            return -1;
        }
        return cellAt(worldX);
    }
}
