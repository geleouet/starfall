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

    /**
     * Bande où s'affichent les glyphes d'intention, au-dessus des têtes.
     *
     * <p>Ces hauteurs vivent ici et non en constantes cachées dans le rendu, pour la même raison
     * qu'en M5 : elles doivent être confrontables au budget du bandeau d'interface sans contexte
     * graphique. Codées en dur dans la scène, elles ne l'étaient pas — et le bandeau effaçait les
     * glyphes à certaines tailles de fenêtre, exactement le défaut que M5 avait déjà corrigé
     * ailleurs.
     */
    public static final int INTENT_Y = FIGURE_Y + FIGURE_HEIGHT + 6;
    public static final int INTENT_HEIGHT = 6;
    public static final int INTENT_TOP = INTENT_Y + INTENT_HEIGHT;

    /**
     * Bande des points de vie, juste au-dessus des têtes.
     *
     * <p>Elle a sa propre hauteur, et ce n'est pas un détail : les pastilles étaient d'abord posées
     * sous les figures, c'est-à-dire exactement sur la ligne des barres de menace. Deux
     * informations vitales au même pixel, chacune effaçant l'autre selon l'ordre de dessin.
     */
    public static final int HEALTH_Y = FIGURE_Y + FIGURE_HEIGHT + 1;
    public static final int HEALTH_HEIGHT = 2;

    /**
     * Bande des repères tactiques : position du héros, son regard, et le lien vers sa cible
     * d'échange.
     *
     * <p>Elle vivait en constante privée dans la scène, ce qui la rendait invérifiable — or c'est
     * précisément le genre de valeur qui doit être confrontable aux autres bandes, puisque le seul
     * défaut qu'elle puisse avoir est d'en recouvrir une.
     */
    public static final int MARK_Y = GROUND_Y - 4;
    public static final int MARK_HEIGHT = 2;

    /**
     * Hauteur des <b>pointes</b> posées sur cette bande — celle du héros, celle de sa cible.
     *
     * <p>Elle manquait, et son absence a fait mentir le test de bandes pendant six jalons : il
     * comparait {@code MARK_Y + MARK_HEIGHT} à la bande voisine, c'est-à-dire deux pixels, alors
     * que les pointes en occupaient huit et enjambaient à la fois la bande des portées en dessous
     * et la première ligne des dalles au-dessus. Le test vérifiait des <em>constantes</em>, pas le
     * dessin.
     *
     * <p>Les pointes poussent maintenant vers le haut depuis la ligne des repères, sans jamais
     * descendre : la bande va de {@link #MARK_Y} à {@link #MARK_TOP}, et rien n'en sort.
     */
    public static final int MARK_TIP_HEIGHT = 4;
    /** Bord supérieur de la bande des repères tactiques, pointes comprises. */
    public static final int MARK_TOP = MARK_Y + MARK_TIP_HEIGHT;

    /** Retrait latéral d'un repère tactique dans sa case, de chaque côté. */
    public static final int MARK_INSET = 2;

    /**
     * Colonnes qu'occupe la pointe d'un repère tactique, en pixels-monde.
     *
     * <p>Elle vit ici, et pas dans le rendu, pour une raison que la review a rendue indiscutable :
     * la pointe du héros et celle de sa cible se sont recouvertes <b>au pixel près</b> pendant tout
     * un cycle, sans qu'aucun test ne puisse le voir. Le seul contrôle existant comparait des
     * <em>constantes de bande</em> entre elles — il ne savait rien de ce qui était dessiné dedans.
     *
     * <p>Le correctif a été vérifié à la main, en relisant les pixels d'une capture. C'est mieux que
     * rien et moins bien qu'un test : une mesure faite une fois ne garde rien. La géométrie est de
     * l'arithmétique pure, elle n'a jamais eu besoin d'un contexte graphique.
     *
     * @param cellLeft bord gauche de la case, en pixels-monde
     * @param toward   sens vers lequel la pointe s'affine : {@code -1} vers la gauche, {@code +1} vers
     *                 la droite
     * @return les colonnes occupées, de la plus fine à la plus épaisse
     */
    public static int[] markTipColumns(int cellLeft, int toward) {
        int left = cellLeft + MARK_INSET;
        int width = CELL_WIDTH - 2 * MARK_INSET;
        // La pointe est posée sur le bord de la barre situé du côté où elle s'affine, et elle
        // s'épaissit vers l'intérieur de la case. C'est ce « vers l'intérieur » qui garantit qu'elle
        // ne sort jamais de sa propre case, donc qu'elle ne peut pas rencontrer celle du voisin.
        int tip = toward < 0 ? left : left + width - 1;
        int[] columns = new int[MARK_TIP_HEIGHT];
        for (int i = 0; i < columns.length; i++) {
            columns[i] = tip - toward * i;
        }
        return columns;
    }

    /**
     * Bande des repères de portée : ce que la prochaine tuile fera, sous les dalles.
     *
     * <p>Elle a sa propre hauteur, quatre pixels sous celle des repères tactiques. Les y mêler
     * aurait fait se recouvrir le trait de liaison de la capacité d'échange et la trajectoire d'une
     * poussée — deux flèches de même longueur au même endroit, dont l'une décrit ce que fait la
     * touche E et l'autre ce que fait la tuile du sommet.
     */
    public static final int PREVIEW_Y = GROUND_Y - 8;
    public static final int PREVIEW_HEIGHT = 3;

    /**
     * Barres de menace : une par coup qui tombera sur la case.
     *
     * <h2>Pourquoi c'est calculé et non codé en dur</h2>
     *
     * <p>Le rendu dessinait trois barres au maximum, en s'arrêtant net au-delà. Quatre coups
     * annoncés sur une case s'affichaient donc « trois », et la vague 3 — sabreur rapide, archer
     * agressif, lancier fonceur, colosse — peut en produire cinq. Le héros a cinq points de vie :
     * la différence entre « trois » et « cinq » est la différence entre rester et mourir.
     *
     * <p>C'était le télégraphe qui <b>sous-promettait</b>, dans l'exact miroir du défaut que le
     * jalon précédent avait fait corriger côté modèle. Le pas se resserre donc pour que le compte
     * reste exact, et {@link com.starfall.scene.HudText} écrit en plus le nombre.
     *
     * @return pas horizontal entre deux barres, en pixels-monde
     */
    public static int threatBarPitch(int blows) {
        int usable = CELL_WIDTH - 4;
        return blows <= 3 ? 6 : Math.max(2, usable / blows);
    }

    /** Largeur d'une barre de menace, déduite du pas. */
    public static int threatBarWidth(int blows) {
        return Math.max(1, threatBarPitch(blows) - 2);
    }

    /** Nombre de barres réellement dessinables sur une case sans déborder. */
    public static int threatBarsDrawn(int blows) {
        int usable = CELL_WIDTH - 4;
        int pitch = threatBarPitch(blows);
        return Math.min(blows, 1 + (usable - threatBarWidth(blows)) / pitch);
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
