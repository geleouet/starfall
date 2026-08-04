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
