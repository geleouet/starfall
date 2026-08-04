package com.starfall.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.starfall.StarfallGame;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests de la géométrie de la file d'actions et du râtelier. */
class HudLayoutTest {

    private static final int CENTRE = StarfallGame.MIN_WORLD_WIDTH / 2;

    private static HudLayout hud() {
        return new HudLayout(CENTRE, Tile.values().length);
    }

    @Test
    @DisplayName("Les deux rangées tiennent dans la zone garantie")
    void bothRowsFitInsideTheGuaranteedArea() {
        HudLayout hud = hud();

        assertTrue(hud.queueSlotX(0) >= 0, "file debordant a gauche");
        assertTrue(hud.queueSlotX(ActionQueue.CAPACITY - 1) + HudLayout.TILE_SIZE
                <= StarfallGame.MIN_WORLD_WIDTH, "file debordant a droite");
        assertTrue(hud.rackSlotX(0) >= 0, "rateleir debordant a gauche");
        assertTrue(hud.rackSlotX(Tile.values().length - 1) + HudLayout.TILE_SIZE
                <= StarfallGame.MIN_WORLD_WIDTH, "rateleir debordant a droite");
    }

    @Test
    @DisplayName("Les rangées ne recouvrent ni le plateau ni l'une l'autre")
    void theRowsNeverOverlapTheBoardNorEachOther() {
        assertTrue(HudLayout.QUEUE_Y + HudLayout.TILE_SIZE < HudLayout.RACK_Y,
                "la file mord sur le rateleir");
        assertTrue(HudLayout.RACK_Y + HudLayout.TILE_SIZE < ArenaLayout.PICK_BOTTOM,
                "le rateleir mord sur la zone de pointage du plateau");
        assertTrue(HudLayout.QUEUE_Y >= 0);
    }

    @Test
    @DisplayName("Les emplacements s'alignent sans trou ni recouvrement")
    void slotsAreEvenlySpaced() {
        HudLayout hud = hud();
        int step = HudLayout.TILE_SIZE + HudLayout.TILE_GAP;

        for (int i = 0; i < ActionQueue.CAPACITY - 1; i++) {
            assertEquals(hud.queueSlotX(i) + step, hud.queueSlotX(i + 1));
        }
        for (int i = 0; i < Tile.values().length - 1; i++) {
            assertEquals(hud.rackSlotX(i) + step, hud.rackSlotX(i + 1));
        }
    }

    @ParameterizedTest(name = "{0} tuiles au râtelier")
    @ValueSource(ints = {1, 2, 4, 6, 8})
    @DisplayName("Le râtelier reste centré quelle que soit sa taille")
    void theRackStaysCentredWhateverItsSize(int rackSize) {
        HudLayout hud = new HudLayout(CENTRE, rackSize);
        int left = hud.rackSlotX(0);
        int right = hud.rackSlotX(rackSize - 1) + HudLayout.TILE_SIZE;

        // Un demi-pixel de decentrage ferait vibrer toute la rangee des que la camera bouge.
        assertEquals(HudLayout.rowWidth(rackSize), right - left);
        assertTrue(Math.abs((left + right) / 2 - CENTRE) <= 1, "rangee decentree de plus d'un pixel");
    }

    @Test
    @DisplayName("Le pointage retrouve chaque emplacement de la file")
    void pickingFindsEveryQueueSlot() {
        HudLayout hud = hud();

        for (int i = 0; i < ActionQueue.CAPACITY; i++) {
            int x = hud.queueSlotX(i);
            assertEquals(i, hud.queueSlotAt(x, HudLayout.QUEUE_Y), "coin bas-gauche de " + i);
            assertEquals(i, hud.queueSlotAt(x + HudLayout.TILE_SIZE / 2f,
                    HudLayout.QUEUE_Y + HudLayout.TILE_SIZE / 2f), "centre de " + i);
            assertEquals(i, hud.queueSlotAt(x + HudLayout.TILE_SIZE,
                    HudLayout.QUEUE_Y + HudLayout.TILE_SIZE), "coin haut-droit de " + i);
        }
    }

    @Test
    @DisplayName("Le pointage retrouve chaque tuile du râtelier")
    void pickingFindsEveryRackSlot() {
        HudLayout hud = hud();

        for (int i = 0; i < Tile.values().length; i++) {
            assertEquals(i, hud.rackSlotAt(hud.rackSlotX(i) + HudLayout.TILE_SIZE / 2f,
                    HudLayout.RACK_Y + HudLayout.TILE_SIZE / 2f));
        }
    }

    /**
     * Cliquer entre deux tuiles ne doit rien faire plutôt que d'en désigner une au hasard : le
     * geste imprécis est fréquent, et poser la mauvaise tuile est un vrai coût pour le joueur.
     */
    @Test
    @DisplayName("Cliquer dans la gouttière entre deux tuiles ne désigne rien")
    void clickingInTheGutterSelectsNothing() {
        HudLayout hud = hud();
        float gutter = hud.queueSlotX(0) + HudLayout.TILE_SIZE + 1;

        assertEquals(-1, hud.queueSlotAt(gutter, HudLayout.QUEUE_Y + 8f));
    }

    @Test
    @DisplayName("Le pointage ignore ce qui est en dehors des rangées")
    void pickingIgnoresEverythingElse() {
        HudLayout hud = hud();
        float insideX = hud.queueSlotX(2) + 8f;

        assertEquals(-1, hud.queueSlotAt(insideX, HudLayout.QUEUE_Y - 1f), "sous la rangee");
        assertEquals(-1, hud.queueSlotAt(insideX, HudLayout.QUEUE_Y + HudLayout.TILE_SIZE + 1f),
                "au-dessus de la rangee");
        assertEquals(-1, hud.queueSlotAt(hud.queueSlotX(0) - 1f, HudLayout.QUEUE_Y + 8f),
                "a gauche de la rangee");
        assertEquals(-1, hud.queueSlotAt(
                hud.queueSlotX(ActionQueue.CAPACITY - 1) + HudLayout.TILE_SIZE + 1f,
                HudLayout.QUEUE_Y + 8f), "a droite de la rangee");

        // Et surtout : la rangee de la file ne repond pas a la hauteur du rateleir, ni l'inverse.
        assertEquals(-1, hud.queueSlotAt(insideX, HudLayout.RACK_Y + 8f));
        assertEquals(-1, hud.rackSlotAt(insideX, HudLayout.QUEUE_Y + 8f));
    }

    @Test
    @DisplayName("Un râtelier vide est refusé")
    void anEmptyRackIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new HudLayout(CENTRE, 0));
    }
}
