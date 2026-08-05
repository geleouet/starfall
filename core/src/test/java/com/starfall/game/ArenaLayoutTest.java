package com.starfall.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.starfall.StarfallGame;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests du cadrage. C'est l'affirmation de M4 qui se vérifie le moins bien à l'œil : une case
 * coupée au bord d'une grille de 15 ne se verrait que sur une capture, et seulement si on pense à
 * la prendre à cette largeur-là.
 */
class ArenaLayoutTest {

    private static final int CENTRE = StarfallGame.MIN_WORLD_WIDTH / 2;

    private static ArenaLayout layout(int gridWidth) {
        return new ArenaLayout(gridWidth, CENTRE);
    }

    @ParameterizedTest(name = "grille de {0} cases")
    @ValueSource(ints = {5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15})
    @DisplayName("Toute grille tient entièrement dans la zone garantie")
    void everyGridFitsInsideTheGuaranteedArea(int gridWidth) {
        ArenaLayout layout = layout(gridWidth);

        assertTrue(layout.left() >= 0,
                () -> "la grille de " + gridWidth + " deborde a gauche : " + layout.left());
        assertTrue(layout.right() <= StarfallGame.MIN_WORLD_WIDTH,
                () -> "la grille de " + gridWidth + " deborde a droite : " + layout.right());
    }

    @ParameterizedTest(name = "grille de {0} cases")
    @ValueSource(ints = {5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15})
    @DisplayName("La grille est centrée, et sur des pixels entiers")
    void theGridIsCentredOnWholePixels(int gridWidth) {
        ArenaLayout layout = layout(gridWidth);

        // Un demi-pixel de decentrage ferait scintiller toute l'arene des que la camera bouge.
        assertEquals(CENTRE, (layout.left() + layout.right()) / 2);
        assertEquals(layout.left() + layout.pixelWidth(), layout.right());
        assertEquals(CENTRE - layout.left(), layout.right() - CENTRE, "centrage asymetrique");
    }

    @ParameterizedTest(name = "grille de {0} cases")
    @ValueSource(ints = {5, 9, 15})
    @DisplayName("Les cases s'abutent sans trou ni recouvrement")
    void cellsTileWithoutGapOrOverlap(int gridWidth) {
        ArenaLayout layout = layout(gridWidth);

        for (int i = 0; i < gridWidth - 1; i++) {
            assertEquals(layout.cellLeft(i) + ArenaLayout.CELL_WIDTH, layout.cellLeft(i + 1),
                    "trou ou recouvrement entre les cases " + i + " et " + (i + 1));
        }
        assertEquals(layout.left(), layout.cellLeft(0));
        assertEquals(layout.right(), layout.cellLeft(gridWidth - 1) + ArenaLayout.CELL_WIDTH);
    }

    @ParameterizedTest(name = "grille de {0} cases")
    @ValueSource(ints = {5, 9, 15})
    @DisplayName("Une figure est centrée dans sa case et n'en sort pas")
    void figuresSitCentredInsideTheirCell(int gridWidth) {
        ArenaLayout layout = layout(gridWidth);

        for (int i = 0; i < gridWidth; i++) {
            int left = layout.figureLeft(i);
            assertTrue(left >= layout.cellLeft(i), "figure debordant a gauche de la case " + i);
            assertTrue(left + ArenaLayout.FIGURE_WIDTH <= layout.cellLeft(i) + ArenaLayout.CELL_WIDTH,
                    "figure debordant a droite de la case " + i);
            assertEquals(left - layout.cellLeft(i),
                    layout.cellLeft(i) + ArenaLayout.CELL_WIDTH - (left + ArenaLayout.FIGURE_WIDTH),
                    "figure non centree dans la case " + i);
        }
    }

    @ParameterizedTest(name = "grille de {0} cases")
    @ValueSource(ints = {5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15})
    @DisplayName("Le pointage retrouve exactement la case sous le curseur")
    void pickingFindsTheCellUnderTheCursor(int gridWidth) {
        ArenaLayout layout = layout(gridWidth);

        for (int i = 0; i < gridWidth; i++) {
            assertEquals(i, layout.cellAt(layout.cellLeft(i)), "bord gauche de la case " + i);
            assertEquals(i, layout.cellAt(layout.cellLeft(i) + ArenaLayout.CELL_WIDTH - 0.5f),
                    "bord droit de la case " + i);
            assertEquals(i, layout.cellAt(layout.cellLeft(i) + ArenaLayout.CELL_WIDTH / 2f),
                    "milieu de la case " + i);
        }
    }

    @Test
    @DisplayName("Pointer en dehors de la grille ne désigne aucune case")
    void pickingOutsideTheGridSelectsNothing() {
        ArenaLayout layout = layout(9);

        // Une division entiere aurait renvoye 0 juste a gauche du bord, et donc surligne la
        // premiere case alors que le curseur n'est pas dessus.
        assertEquals(-1, layout.cellAt(layout.left() - 0.5f));
        assertEquals(-1, layout.cellAt(layout.left() - 20f));
        assertEquals(-1, layout.cellAt(layout.right()));
        assertEquals(-1, layout.cellAt(layout.right() + 40f));
        assertEquals(-1, layout.cellAt(-1000f));
    }

    /**
     * Le pointage à la souris a une bande verticale, et c'est indispensable : sans elle, toute la
     * colonne d'une case était cliquable du bas de la fenêtre jusqu'en haut, donc un clic sur le
     * bandeau d'interface traversait et déplaçait le héros.
     */
    @Test
    @DisplayName("Le pointage souris ignore ce qui est hors de la bande de jeu")
    void pickingIgnoresWhatIsOutsideThePlayBand() {
        ArenaLayout layout = layout(9);
        float insideX = layout.cellLeft(3) + 10f;

        assertEquals(3, layout.cellAt(insideX, ArenaLayout.GROUND_Y + 4f), "sur la dalle");
        assertEquals(3, layout.cellAt(insideX, ArenaLayout.FIGURE_Y + 16f), "sur la figure");
        assertEquals(3, layout.cellAt(insideX, ArenaLayout.PICK_BOTTOM), "bord bas inclus");
        assertEquals(3, layout.cellAt(insideX, ArenaLayout.PICK_TOP), "bord haut inclus");

        assertEquals(-1, layout.cellAt(insideX, ArenaLayout.PICK_BOTTOM - 1f), "sous la bande");
        assertEquals(-1, layout.cellAt(insideX, ArenaLayout.PICK_TOP + 1f), "au-dessus de la bande");
        assertEquals(-1, layout.cellAt(insideX, 0f), "tout en bas de la fenetre");
        assertEquals(-1, layout.cellAt(insideX, 175f), "dans le bandeau d'interface");
    }

    @Test
    @DisplayName("La bande de pointage couvre bien les figures et les dalles")
    void thePlayBandCoversWhatThePlayerSees() {
        assertTrue(ArenaLayout.PICK_BOTTOM < ArenaLayout.GROUND_Y);
        assertTrue(ArenaLayout.PICK_TOP >= ArenaLayout.FIGURE_Y + ArenaLayout.FIGURE_HEIGHT);
        assertTrue(ArenaLayout.PICK_BOTTOM >= 0);
    }

    /**
     * La version précédente de ce test assénait {@code assertEquals(CENTRE, layout(width, CENTRE)
     * .cameraTargetX())} — c'est-à-dire {@code CENTRE == CENTRE}. Elle ne prouvait rien, et surtout
     * elle ne prouvait pas ce qui compte : que la cible de caméra est <b>au milieu de la grille</b>,
     * quel que soit l'endroit où celle-ci est posée.
     */
    @Test
    @DisplayName("La cible de caméra tombe au milieu de la grille, où qu'elle soit posée")
    void theCameraTargetIsTheGridMidpoint() {
        for (int width = Grid.MIN_WIDTH; width <= Grid.MAX_WIDTH; width++) {
            for (int centre : new int[]{CENTRE, 0, 40, 500, -120}) {
                ArenaLayout layout = new ArenaLayout(width, centre);

                assertEquals((layout.left() + layout.right()) / 2, layout.cameraTargetX(),
                        "grille de " + width + " posee en " + centre);
            }
        }
    }

    @Test
    @DisplayName("Cadrée sur sa cible, toute grille tient dans la zone garantie")
    void framedOnItsTargetEveryGridFitsInTheGuaranteedArea() {
        // Le lien réel entre la caméra et la grille : on part de la cible de caméra, on en déduit
        // la fenêtre garantie autour d'elle, et on vérifie que la grille y tient. C'est ce que la
        // scène fait à l'exécution.
        for (int w = Grid.MIN_WIDTH; w <= Grid.MAX_WIDTH; w++) {
            final int width = w;
            ArenaLayout layout = new ArenaLayout(width, CENTRE);
            int target = layout.cameraTargetX();
            final int safeLeft = target - StarfallGame.MIN_WORLD_WIDTH / 2;
            final int safeRight = target + StarfallGame.MIN_WORLD_WIDTH / 2;

            assertTrue(layout.left() >= safeLeft,
                    () -> "grille de " + width + " : bord gauche " + layout.left() + " < " + safeLeft);
            assertTrue(layout.right() <= safeRight,
                    () -> "grille de " + width + " : bord droit " + layout.right() + " > " + safeRight);
        }
    }

    @Test
    @DisplayName("Une largeur de grille hors bornes est refusée")
    void illegalGridWidthsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> layout(4));
        assertThrows(IllegalArgumentException.class, () -> layout(16));
    }

    @Test
    @DisplayName("Les figures tiennent au-dessus du sol, sous le bandeau d'interface")
    void figuresSitBetweenTheGroundAndTheOverlay() {
        assertEquals(ArenaLayout.GROUND_Y + ArenaLayout.GROUND_HEIGHT, ArenaLayout.FIGURE_Y);
        assertTrue(ArenaLayout.FIGURE_Y + ArenaLayout.FIGURE_HEIGHT <= StarfallGame.PATTERN_TOP,
                "les figures montent sous le bandeau : "
                        + (ArenaLayout.FIGURE_Y + ArenaLayout.FIGURE_HEIGHT));
        assertTrue(ArenaLayout.GROUND_Y >= 0);
    }

    /**
     * Les bandes de repères sous les dalles ne se recouvrent pas.
     *
     * <p>Elles portent trois messages différents au même endroit de l'écran : où je suis et où je
     * regarde, ce que la touche d'échange viserait, et ce que la tuile du sommet fera. Deux d'entre
     * elles au même pixel, ce serait un trait de liaison doré et une trajectoire de poussée
     * violette de même longueur superposés — deux phrases lues comme une seule.
     */
    @Test
    @DisplayName("Les bandes de repères sous les dalles ne se recouvrent pas")
    void theMarkBandsBelowTheGroundDoNotOverlap() {
        assertTrue(ArenaLayout.PREVIEW_Y + ArenaLayout.PREVIEW_HEIGHT <= ArenaLayout.MARK_Y,
                "la bande des portees mord sur celle des reperes tactiques : "
                        + (ArenaLayout.PREVIEW_Y + ArenaLayout.PREVIEW_HEIGHT)
                        + " > " + ArenaLayout.MARK_Y);
        // On compare la bande REELLEMENT occupee - pointes comprises - et non la seule epaisseur
        // du trait. La version precedente comparait MARK_HEIGHT, soit deux pixels, alors que les
        // pointes en occupaient huit et enjambaient les deux bandes voisines. Un test de bandes
        // qui ignore la moitie de ce qui est dessine ne garde rien.
        assertTrue(ArenaLayout.MARK_TOP <= ArenaLayout.GROUND_Y,
                "les reperes tactiques, pointes comprises, montent a " + ArenaLayout.MARK_TOP
                        + " et mordent sur les dalles qui commencent a " + ArenaLayout.GROUND_Y);
        assertTrue(ArenaLayout.MARK_TIP_HEIGHT >= ArenaLayout.MARK_HEIGHT,
                "une pointe plus fine que son propre trait ne se verrait pas");
        assertTrue(ArenaLayout.PREVIEW_Y >= HudLayout.RACK_Y + HudLayout.TILE_SIZE,
                "la bande des portees mord sur le ratelier");
    }

    /**
     * La pointe du héros et celle de sa cible ne partagent aucune colonne.
     *
     * <p>C'est l'assertion d'une ligne qui manquait, et son absence a coûté un cycle entier. Quand
     * la cible d'échange est <b>adjacente</b> — le cas le plus courant —, les deux pointes
     * occupaient exactement les mêmes quatre colonnes, et le héros étant dessiné après, il écrasait
     * l'autre. On ne voyait pas « deux flèches qui se font face » mais un losange bicolore :
     * précisément l'ambiguïté que ce repère venait lever.
     *
     * <p>Le contrôle existant comparait des <em>constantes de bande</em> entre elles ; il ne savait
     * rien de ce qui était dessiné dedans. Celui-ci compare les colonnes réellement occupées, ce qui
     * ne demande aucun contexte graphique puisque c'est de l'arithmétique.
     */
    @Test
    @DisplayName("Les deux pointes tactiques ne partagent jamais une colonne")
    void thetwoTacticalTipsNeverShareAColumn() {
        for (int heroCell = 0; heroCell < Grid.MAX_WIDTH; heroCell++) {
            for (int step : new int[]{-1, 1}) {
                int target = heroCell + step;
                if (target < 0 || target >= Grid.MAX_WIDTH) {
                    continue;
                }
                ArenaLayout layout = new ArenaLayout(Grid.MAX_WIDTH, CENTRE);

                // La pointe du héros s'affine vers ce qu'il regarde, celle de la cible vers lui.
                int[] hero = ArenaLayout.markTipColumns(layout.cellLeft(heroCell), step);
                int[] cible = ArenaLayout.markTipColumns(layout.cellLeft(target), -step);

                java.util.Set<Integer> shared = new java.util.TreeSet<>();
                for (int a : hero) {
                    for (int b : cible) {
                        if (a == b) {
                            shared.add(a);
                        }
                    }
                }
                assertTrue(shared.isEmpty(), "heros en " + (heroCell + 1) + " regardant "
                        + (step > 0 ? "a droite" : "a gauche") + ", cible adjacente : colonnes"
                        + " partagees " + shared);
            }
        }
    }

    /**
     * Et chaque pointe reste dans sa propre case : c'est ce qui garantit le test ci-dessus pour
     * toute distance, pas seulement pour l'adjacence.
     */
    @Test
    @DisplayName("Une pointe ne sort jamais de sa case")
    void amarkTipNeverLeavesItsCell() {
        ArenaLayout layout = new ArenaLayout(Grid.MAX_WIDTH, CENTRE);
        for (int cell = 0; cell < Grid.MAX_WIDTH; cell++) {
            for (int step : new int[]{-1, 1}) {
                int left = layout.cellLeft(cell);
                for (int column : ArenaLayout.markTipColumns(left, step)) {
                    assertTrue(column >= left && column < left + ArenaLayout.CELL_WIDTH,
                            "case " + (cell + 1) + " : la pointe occupe la colonne " + column
                                    + ", hors de [" + left + ", "
                                    + (left + ArenaLayout.CELL_WIDTH) + "[");
                }
            }
        }
    }
}
