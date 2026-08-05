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
     * Les colonnes qu'occupe un repère, telles que le jeu les demande.
     *
     * <p>Le mot important est « telles que le jeu les demande » : la version précédente de ces tests
     * fournissait <em>elle-même</em> les sens {@code +1} et {@code -1}, donc elle ne savait rien de
     * ceux que la scène passait réellement. La review l'a démontré en inversant les deux appels dans
     * {@code ArenaScene} : héros regardant derrière lui, cible fuyant le héros — et les 448 tests
     * restaient verts. Le sens est maintenant déduit dans {@link ArenaLayout} à partir des cases, et
     * c'est cette déduction-là qui est éprouvée ici.
     */
    private static java.util.Set<Integer> columnsOf(java.util.List<ArenaLayout.MarkShape> shapes) {
        java.util.Set<Integer> columns = new java.util.TreeSet<>();
        for (ArenaLayout.MarkShape shape : shapes) {
            for (int x = shape.x(); x < shape.x() + shape.width(); x++) {
                columns.add(x);
            }
        }
        return columns;
    }

    /** Les colonnes de la seule pointe, c'est-à-dire tout sauf la barre. */
    private static java.util.Set<Integer> tipColumnsOf(java.util.List<ArenaLayout.MarkShape> shapes) {
        return columnsOf(shapes.subList(1, shapes.size()));
    }

    /**
     * La pointe du héros et celle de sa cible ne partagent aucune colonne.
     *
     * <p>C'est l'assertion qui manquait, et son absence a coûté un cycle entier. Quand la cible
     * d'échange est <b>adjacente</b> — le cas le plus courant —, les deux pointes occupaient
     * exactement les mêmes quatre colonnes, et le héros étant dessiné après, il écrasait l'autre. On
     * ne voyait pas « deux flèches qui se font face » mais un losange bicolore : précisément
     * l'ambiguïté que ce repère venait lever.
     *
     * <p>Le balayage porte sur <b>toutes les cases et les deux orientations</b>, mais il faut être
     * honnête sur ce qu'il vaut : la géométrie étant invariante par translation, les cases n'ajoutent
     * pas de cas distincts — elles vérifient seulement que le calcul reste ancré sur la bonne case.
     * Ce que ce test garde réellement de neuf, c'est la <b>déduction du sens à partir des cases</b>.
     */
    @Test
    @DisplayName("Les deux pointes tactiques ne partagent jamais une colonne")
    void thetwoTacticalTipsNeverShareAColumn() {
        ArenaLayout layout = new ArenaLayout(Grid.MAX_WIDTH, CENTRE);
        for (int heroCell = 0; heroCell < Grid.MAX_WIDTH; heroCell++) {
            for (Direction facing : Direction.values()) {
                int target = heroCell + facing.step();
                if (target < 0 || target >= Grid.MAX_WIDTH) {
                    continue;
                }
                java.util.Set<Integer> hero = tipColumnsOf(layout.heroMark(heroCell, facing));
                java.util.Set<Integer> cible = tipColumnsOf(layout.targetMark(target, heroCell));

                java.util.Set<Integer> shared = new java.util.TreeSet<>(hero);
                shared.retainAll(cible);
                assertTrue(shared.isEmpty(), "heros en " + (heroCell + 1) + " regardant " + facing
                        + ", cible adjacente : colonnes partagees " + shared);
            }
        }
    }

    /**
     * Et chaque repère reste dans sa propre case : c'est ce qui étend le test ci-dessus à
     * <b>toute</b> distance, et pas seulement à l'adjacence. Deux cases distinctes ayant des plages
     * de colonnes disjointes par construction, « rien ne sort de sa case » entraîne « rien ne se
     * rencontre ».
     */
    @Test
    @DisplayName("Un repère ne déborde jamais de sa case")
    void amarkNeverLeavesItsCell() {
        ArenaLayout layout = new ArenaLayout(Grid.MAX_WIDTH, CENTRE);
        for (int cell = 0; cell < Grid.MAX_WIDTH; cell++) {
            int left = layout.cellLeft(cell);
            int right = left + ArenaLayout.CELL_WIDTH;
            for (Direction facing : Direction.values()) {
                for (int column : columnsOf(layout.heroMark(cell, facing))) {
                    assertTrue(column >= left && column < right,
                            "case " + (cell + 1) + " : le repere occupe la colonne " + column
                                    + ", hors de [" + left + ", " + right + "[");
                }
            }
        }
    }

    /**
     * La pointe tient dans sa propre barre.
     *
     * <p>C'est le contrôle qui manquait au retrait. {@code CELL_INSET} existait mais n'était employé
     * nulle part : les huit sites de dessin gardaient {@code + 2} et {@code - 4} en dur, si bien que
     * la review a pu porter la constante à 7 sans qu'un test bronche — la pointe se détachait de sa
     * barre de cinq pixels et flottait à côté, seule sur la ligne.
     *
     * <p>« La pointe est incluse dans la barre » est une propriété qu'aucune valeur de retrait ne
     * peut plus contredire en silence, puisque les deux la lisent désormais au même endroit. Ce test
     * garde donc l'unicité de la règle autant que sa valeur.
     */
    @Test
    @DisplayName("La pointe d'un repère tient dans sa barre")
    void themarkTipFitsInsideItsOwnBar() {
        ArenaLayout layout = new ArenaLayout(Grid.MAX_WIDTH, CENTRE);
        for (int cell = 0; cell < Grid.MAX_WIDTH; cell++) {
            for (Direction facing : Direction.values()) {
                var shapes = layout.heroMark(cell, facing);
                ArenaLayout.MarkShape bar = shapes.get(0);
                for (int column : tipColumnsOf(shapes)) {
                    assertTrue(bar.coversColumn(column),
                            "case " + (cell + 1) + ", " + facing + " : la pointe occupe la colonne "
                                    + column + ", hors de sa barre [" + bar.x() + ", "
                                    + (bar.x() + bar.width()) + "[");
                }
            }
        }
    }

    /** La colonne la plus fine d'un repère, c'est-à-dire le bout de la pointe. */
    private static int fineColumnOf(java.util.List<ArenaLayout.MarkShape> shapes) {
        for (ArenaLayout.MarkShape shape : shapes) {
            if (shape.height() == 1) {
                return shape.x();
            }
        }
        throw new AssertionError("aucun rectangle d'un pixel de haut : le repere n'a pas de pointe");
    }

    /**
     * <b>Les deux pointes se font face.</b> C'est la promesse du repère, et c'était le vrai trou.
     *
     * <p>La disjonction des colonnes ne dit rien du <em>sens</em> : deux pointes qui se tournent le
     * dos sont exactement aussi disjointes que deux pointes qui se regardent, puisqu'aucune ne sort
     * de sa case. J'ai vérifié la chose plutôt que de la supposer — en inversant la déduction du sens
     * de la cible, puis celle du héros, la suite entière restait verte. Le test précédent gardait la
     * <i>séparation</i> et laissait passer l'<i>inversion</i>, qui est le défaut de lecture le plus
     * grave des deux : un troc affiché à l'envers dit au joueur le contraire de ce qui va arriver.
     *
     * <p>La propriété est énoncée <b>sans signe</b>, pour ne pas recopier le calcul qu'elle
     * éprouve : de toutes les extrémités des deux barres, les deux bouts de pointe doivent être la
     * paire la plus rapprochée. Une pointe retournée s'éloigne de l'autre, et ça se voit.
     */
    @Test
    @DisplayName("Les deux pointes se font face, et pas dos à dos")
    void thetwoTipsFaceEachOther() {
        ArenaLayout layout = new ArenaLayout(Grid.MAX_WIDTH, CENTRE);
        for (int heroCell = 0; heroCell < Grid.MAX_WIDTH; heroCell++) {
            for (Direction facing : Direction.values()) {
                for (int distance = 1; distance < Grid.MAX_WIDTH; distance++) {
                    int target = heroCell + facing.step() * distance;
                    if (target < 0 || target >= Grid.MAX_WIDTH) {
                        continue;
                    }
                    var hero = layout.heroMark(heroCell, facing);
                    var cible = layout.targetMark(target, heroCell);

                    int gap = Math.abs(fineColumnOf(hero) - fineColumnOf(cible));
                    int nearest = nearestEnds(hero.get(0), cible.get(0));
                    assertEquals(nearest, gap, "heros en " + (heroCell + 1) + " regardant " + facing
                            + ", cible en " + (target + 1) + " : les pointes sont distantes de " + gap
                            + " alors que les barres se rapprochent jusqu'a " + nearest
                            + " -- au moins une pointe est tournee du mauvais cote");
                }
            }
        }
    }

    /** Le plus petit écart entre une extrémité de la première barre et une de la seconde. */
    private static int nearestEnds(ArenaLayout.MarkShape first, ArenaLayout.MarkShape second) {
        int[] a = {first.x(), first.x() + first.width() - 1};
        int[] b = {second.x(), second.x() + second.width() - 1};
        int best = Integer.MAX_VALUE;
        for (int x : a) {
            for (int y : b) {
                best = Math.min(best, Math.abs(x - y));
            }
        }
        return best;
    }

    /**
     * Et le héros ne peut pas être sa propre cible d'échange.
     *
     * <p>Sans cette garde, {@code targetMark} recevrait un sens nul et rendrait une pointe dégénérée
     * — quatre rectangles empilés sur une seule colonne — au lieu de signaler l'erreur. C'est la
     * seule entrée du calcul qui n'a pas de forme correcte.
     */
    @Test
    @DisplayName("Une cible d'échange confondue avec le héros est refusée")
    void aswapTargetOnTheHeroCellIsRefused() {
        ArenaLayout layout = new ArenaLayout(Grid.MAX_WIDTH, CENTRE);
        assertThrows(IllegalArgumentException.class, () -> layout.targetMark(3, 3));
    }
}
