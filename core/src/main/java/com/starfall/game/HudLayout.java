package com.starfall.game;

/**
 * Géométrie de la file d'actions et du râtelier, en pixels-monde.
 *
 * <p>Pure et sans GL, comme {@link ArenaLayout}, pour la même raison : un emplacement mal placé ou
 * une zone cliquable décalée d'un pixel ne se verraient qu'à l'usage.
 *
 * <p>Les deux rangées vivent <b>sous</b> le plateau, dans la bande laissée libre par
 * {@link ArenaLayout#GROUND_Y}. Le joueur doit voir la grille et sa file d'un même regard : c'est en
 * comparant les deux qu'il décide.
 */
public final class HudLayout {

    /** Côté d'une tuile, en pixels-monde. */
    public static final int TILE_SIZE = 16;
    /** Espace entre deux emplacements. */
    public static final int TILE_GAP = 2;

    /** Bord bas de la rangée de la file. */
    public static final int QUEUE_Y = 4;
    /** Bord bas de la rangée du râtelier. */
    public static final int RACK_Y = 32;

    /**
     * Bandes réservées aux repères, en pixels-monde.
     *
     * <p>Elles vivent ici plutôt qu'en constantes cachées dans le rendu, et pour une raison
     * apprise à la dure : le repère « prochaine tuile » et les repères du râtelier occupaient la
     * même bande de quatre pixels, si bien que le second effaçait le premier — et rien ne pouvait
     * l'attraper, puisque ces hauteurs n'étaient nulle part vérifiables sans contexte graphique.
     */
    public static final int QUEUE_MARK_BOTTOM = QUEUE_Y - 3;
    public static final int QUEUE_MARK_TOP = QUEUE_Y + TILE_SIZE + 4;
    public static final int RACK_MARK_BOTTOM = RACK_Y - 4;
    public static final int RACK_MARK_TOP = RACK_Y + TILE_SIZE;

    // ------------------------------------------------------------------ bandes de texte
    //
    // Hauteur d'une cellule de texte : 11 px-monde, celle de la police du jeu. Les bandes sont
    // déclarées par leur bord SUPÉRIEUR, parce que c'est ce que la police attend — une conversion
    // recopiée à chaque appel finit par se tromper de sens une fois.

    /** Hauteur d'une ligne de texte, cellule comprise. */
    public static final int TEXT_CELL = 11;
    /** Pas vertical entre deux lignes d'un panneau. */
    public static final int TEXT_STEP = 13;
    /** Marge intérieure des panneaux. */
    public static final int PANEL_PADDING = 3;

    /**
     * Bord supérieur du bandeau d'état, en haut de la zone garantie.
     *
     * <p>Il est ancré sur la <b>zone garantie</b> et non sur la zone dessinée : sur une fenêtre
     * large, le décor déborde des 320 px-monde promis, et un bandeau collé au bord de la fenêtre
     * s'éloignerait du plateau au lieu de l'accompagner.
     */
    public static final int BANNER_TOP = 178;
    /** Bord inférieur du bandeau, pour vérifier qu'il ne mord sur rien. */
    public static final int BANNER_BOTTOM = BANNER_TOP - TEXT_CELL;

    /**
     * Bord supérieur du panneau d'information : ce que fera la tuile du sommet, ou l'infobulle de
     * la tuile survolée.
     *
     * <p>Les deux partagent la même bande, et c'est délibéré. Une infobulle qui suit le curseur
     * oblige l'œil à la chercher et recouvre justement ce que le joueur essayait de regarder ;
     * ancrée à un endroit fixe, au-dessus du plateau, elle se lit sans quitter la scène des yeux —
     * et elle ne cache jamais une figure ni une case menacée.
     *
     * <p>Le panneau <b>grandit vers le bas</b> depuis ce bord. Son plancher est donc ce qui limite
     * le nombre de lignes, et {@link #MAX_INFO_LINES} le fixe une fois pour toutes.
     */
    public static final int INFO_TOP = 166;

    /**
     * Nombre maximal de lignes du panneau d'information.
     *
     * <p>Au-delà, le panneau mordrait sur les glyphes d'intention — c'est-à-dire qu'une infobulle
     * cacherait le télégraphe, exactement le genre d'occultation que les jalons précédents ont
     * payée deux fois. {@code HudLayoutTest} vérifie que la valeur tient encore si l'on touche aux
     * hauteurs.
     */
    public static final int MAX_INFO_LINES = 3;

    /** Hauteur d'un panneau de {@code lines} lignes, marges comprises. */
    public static int panelHeight(int lines) {
        return lines * TEXT_STEP + 2 * PANEL_PADDING;
    }

    /** Bord inférieur du panneau d'information pour un nombre de lignes donné. */
    public static int infoPanelBottom(int lines) {
        return INFO_TOP - panelHeight(lines);
    }

    /**
     * Ordonnée autour de laquelle le panneau d'aide est centré.
     *
     * <p>Volontairement <b>plus haute</b> que le milieu du monde. Centrée sur 90, l'aide recouvrait
     * treize des seize pixels du râtelier — c'est-à-dire les six tuiles que sa deuxième ligne
     * numérote (« 1 À 6 : POSER UNE TUILE »). Un panneau qui cache ce qu'il explique explique mal.
     *
     * <p>Elle couvre encore le plateau, et c'est assumé : l'aide est modale, un geste la referme
     * sans rien déclencher. {@code HudTextTest} vérifie qu'elle tient entre le haut du râtelier et
     * le bas du bandeau d'état.
     */
    public static final int HELP_CENTRE_Y = 105;

    private final int worldCentre;
    private final int rackSize;

    /**
     * @param worldCentre abscisse autour de laquelle les deux rangées sont centrées
     * @param rackSize    nombre de tuiles que possède le héros
     */
    public HudLayout(int worldCentre, int rackSize) {
        if (rackSize < 1) {
            throw new IllegalArgumentException("Râtelier vide : " + rackSize);
        }
        this.worldCentre = worldCentre;
        this.rackSize = rackSize;
    }

    /** Largeur totale d'une rangée de {@code count} emplacements. */
    public static int rowWidth(int count) {
        return count * TILE_SIZE + (count - 1) * TILE_GAP;
    }

    private int rowLeft(int count) {
        // Le pas vaut 18, donc la demi-largeur d'une rangée peut tomber sur un demi-pixel. On
        // arrondit vers le bas une bonne fois : un centrage fractionnaire ferait vibrer toute la
        // rangée dès que la caméra bouge.
        return worldCentre - rowWidth(count) / 2;
    }

    /** Bord gauche du n-ième emplacement de la file, compté depuis la plus ancienne tuile. */
    public int queueSlotX(int index) {
        return rowLeft(ActionQueue.CAPACITY) + index * (TILE_SIZE + TILE_GAP);
    }

    /** Bord gauche de la n-ième tuile du râtelier. */
    public int rackSlotX(int index) {
        return rowLeft(rackSize) + index * (TILE_SIZE + TILE_GAP);
    }

    public int rackSize() {
        return rackSize;
    }

    /** Emplacement de file sous un point du monde, ou {@code -1}. */
    public int queueSlotAt(float worldX, float worldY) {
        return slotAt(worldX, worldY, QUEUE_Y, ActionQueue.CAPACITY, rowLeft(ActionQueue.CAPACITY));
    }

    /** Tuile du râtelier sous un point du monde, ou {@code -1}. */
    public int rackSlotAt(float worldX, float worldY) {
        return slotAt(worldX, worldY, RACK_Y, rackSize, rowLeft(rackSize));
    }

    /**
     * Le pointage ignore les gouttières entre deux emplacements : cliquer entre deux tuiles ne doit
     * pas en désigner une au hasard. Un geste imprécis ne fait rien, plutôt que de faire autre
     * chose que ce que le joueur voulait.
     */
    private static int slotAt(float worldX, float worldY, int rowY, int count, int left) {
        if (worldY < rowY || worldY > rowY + TILE_SIZE) {
            return -1;
        }
        float offset = worldX - left;
        if (offset < 0) {
            return -1;
        }
        int step = TILE_SIZE + TILE_GAP;
        int index = (int) (offset / step);
        if (index >= count) {
            return -1;
        }
        return (offset - index * step) <= TILE_SIZE ? index : -1;
    }
}
