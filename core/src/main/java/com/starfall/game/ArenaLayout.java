package com.starfall.game;

import java.util.ArrayList;
import java.util.List;

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
    /**
     * Demi-largeur de la plaque qui porte une intention. Voir {@code ArenaScene.drawPlaque}.
     *
     * <p>Elle vit ici, avec le reste de la g&eacute;om&eacute;trie, et non dans la sc&egrave;ne qui
     * la dessine : c'est {@link #INTENT_TOP} qui en d&eacute;pend, et une borne calcul&eacute;e loin
     * de ce qu'elle borne ne le borne pas longtemps.
     */
    public static final int PLAQUE_HALF = 7;

    /** Hauteur de la plaque : le glyphe, le nombre de d&eacute;g&acirc;ts, et l'air autour. */
    public static final int PLAQUE_HEIGHT = INTENT_HEIGHT + 10;

    /** &Eacute;cart entre la plaque de t&ecirc;te et la carte de ce qui suit dans la file. */
    public static final int PLAQUE_STACK_STEP = PLAQUE_HEIGHT + 2;

    /**
     * Le haut de <b>tout ce qui se dessine au-dessus d'un ennemi</b>.
     *
     * <h2>Ce que cette constante disait, et ce qu'elle dit maintenant</h2>
     *
     * <p>Elle valait {@code INTENT_Y + INTENT_HEIGHT}, c'est-&agrave;-dire le haut du <em>glyphe</em>
     * &mdash; exact tant qu'un glyphe nu flottait au-dessus des t&ecirc;tes. Depuis, le glyphe a
     * re&ccedil;u une <b>plaque</b>, la plaque un <b>nombre</b>, et la file une <b>carte de
     * plus</b> : ce qui est peint l&agrave;-haut monte dix-huit pixels plus loin que ce que la
     * borne annon&ccedil;ait.
     *
     * <p>Cela comptait, parce qu'un test s'en sert pour garantir que le panneau d'information ne
     * recouvre pas le t&eacute;l&eacute;graphe &mdash; l'occultation que les jalons pr&eacute;c&eacute;dents
     * ont pay&eacute;e deux fois. <b>Il gardait donc une fronti&egrave;re que le dessin avait
     * franchie sans lui</b>, et serait rest&eacute; vert jusqu'&agrave; ce que la carte du dessus
     * disparaisse sous le panneau. Un second test ass&eacute;rait m&ecirc;me l'ancienne formule,
     * ce qui la rendait vraie par construction : une tautologie ne garde rien.
     */
    public static final int INTENT_TOP = INTENT_Y - 2 + PLAQUE_HEIGHT;

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

    /**
     * Retrait latéral d'un ornement dessiné <em>dans</em> une case, de chaque côté.
     *
     * <p>Il vaut pour tout ce qui se pose à l'intérieur d'une case sans être la case : les repères
     * tactiques, la bande des portées, la croix d'un coup qui rate, les barres de menace. Le retrait
     * est ce qui empêche deux ornements de cases voisines de se toucher et de se lire comme un seul
     * trait continu.
     *
     * <p>La constante existait déjà, mais <b>elle n'était employée nulle part</b> : les sept sites de
     * dessin continuaient d'écrire {@code + 2} et {@code - 4} en dur. La review l'a démontré en la
     * portant à 7 sans qu'un seul test bronche — la pointe se détachait de sa propre barre de cinq
     * pixels et la suite restait verte. Une règle écrite à deux endroits finit toujours par diverger ;
     * celle-ci l'était à huit.
     */
    public static final int CELL_INSET = 2;

    /** Bord gauche de la zone utile d'une case, retrait compris, en pixels-monde. */
    public int insetLeft(int cell) {
        return cellLeft(cell) + CELL_INSET;
    }

    /** Largeur de la zone utile d'une case, les deux retraits ôtés. */
    public static int insetWidth() {
        return CELL_WIDTH - 2 * CELL_INSET;
    }

    /**
     * Un rectangle plein d'un repère tactique, en pixels-monde.
     *
     * <p>C'est la seule chose que le rendu reçoit désormais. Il ne calcule plus rien : ni retrait, ni
     * sens, ni hauteur de pointe. La raison est celle que la review a établie par mutation — en
     * inversant les deux sens dans la scène, « les deux flèches qui se font face » se retournaient
     * exactement, et les 448 tests restaient verts. Les tests gardaient l'arithmétique ; le câblage,
     * lui, n'était gardé par rien, parce que c'était la scène qui décidait du sens.
     */
    public record MarkShape(int x, int y, int width, int height) {

        /** Vrai si ce rectangle occupe la colonne donnée. */
        public boolean coversColumn(int column) {
            return column >= x && column < x + width;
        }
    }

    /**
     * Le repère du héros : un trait, terminé par une pointe du côté qu'il <b>regarde</b>.
     *
     * <p>Position et orientation sont dites par une seule forme plutôt que par deux symboles côte à
     * côte : séparés, ils se télescopaient avec le trait de liaison et l'œil devait démêler trois
     * signes au même endroit.
     */
    public List<MarkShape> heroMark(int heroCell, Direction facing) {
        return mark(heroCell, facing.step());
    }

    /**
     * Le repère de la cible d'échange : le même trait, mais la pointe tournée <b>vers le héros</b>.
     *
     * <p>Elle dit « celui-ci vient à toi », ce qui est exactement ce que fait un échange de place.
     * Avec la pointe du héros qui regarde la cible, les deux se lisent comme une seule phrase — deux
     * flèches qui se font face, c'est-à-dire un troc.
     *
     * <p>Le sens est déduit <b>ici</b> des deux cases, et non reçu du rendu. C'est tout l'objet du
     * correctif : tant que l'appelant fournissait le signe, il pouvait le fournir à l'envers.
     */
    public List<MarkShape> targetMark(int targetCell, int heroCell) {
        if (targetCell == heroCell) {
            throw new IllegalArgumentException(
                    "la cible d'echange et le heros ne peuvent pas occuper la case " + heroCell);
        }
        return mark(targetCell, Integer.signum(heroCell - targetCell));
    }

    /**
     * Le trait et sa pointe, pour une case et un sens.
     *
     * <p>La pointe est posée sur le bord de la barre situé du côté où elle s'affine, et elle
     * s'épaissit vers l'<b>intérieur</b> de la case. C'est ce « vers l'intérieur » qui garantit
     * qu'elle ne sort jamais de sa propre case, donc qu'elle ne peut pas rencontrer celle du voisin.
     *
     * <p>Posée à l'extérieur, en miroir, elle occupait exactement les mêmes quatre colonnes que celle
     * du héros dès que la cible était adjacente — le cas le plus courant d'une capacité d'échange. Le
     * héros étant dessiné après, il l'écrasait : on ne voyait pas deux flèches mais un losange
     * bicolore, précisément l'ambiguïté que ce repère existe pour lever.
     */
    private List<MarkShape> mark(int cell, int toward) {
        int left = insetLeft(cell);
        int width = insetWidth();
        List<MarkShape> shapes = new ArrayList<>(1 + MARK_TIP_HEIGHT);
        shapes.add(new MarkShape(left, MARK_Y, width, MARK_HEIGHT));

        int tip = toward < 0 ? left : left + width - 1;
        for (int i = 0; i < MARK_TIP_HEIGHT; i++) {
            shapes.add(new MarkShape(tip - toward * i, MARK_Y, 1, i + 1));
        }
        return List.copyOf(shapes);
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
