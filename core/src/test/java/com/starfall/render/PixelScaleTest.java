package com.starfall.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Tests de l'arithmétique du viewport pixel-perfect. Aucun contexte GL requis. */
class PixelScaleTest {

    private static final int MIN_W = 320;
    private static final int MIN_H = 180;

    private static PixelScale at(int windowWidth, int windowHeight) {
        return PixelScale.compute(windowWidth, windowHeight, MIN_W, MIN_H);
    }

    /**
     * L'invariant central, et celui qui manquait : la zone sûre doit tenir <b>entièrement</b> dans
     * les pixels de la fenêtre. Les anciens tests ne vérifiaient que {@code worldWidth >= 320},
     * c'est-à-dire que le monde <em>calculé</em> contenait la zone garantie — jamais qu'elle était
     * réellement affichée en entier. C'est par ce trou que passait le défaut de la version
     * précédente, où le bord gauche de la zone garantie n'était rendu qu'à moitié en 1281x720.
     */
    private static void assertSafeAreaFullyOnScreen(PixelScale s, int windowWidth, int windowHeight) {
        int safeScreenX = s.screenX + s.bleedX * s.scale;
        int safeScreenY = s.screenY + s.bleedY * s.scale;

        assertTrue(safeScreenX >= 0,
                () -> "la zone sûre déborde à gauche : " + s + " en " + windowWidth + "x" + windowHeight);
        assertTrue(safeScreenY >= 0,
                () -> "la zone sûre déborde en bas : " + s + " en " + windowWidth + "x" + windowHeight);
        assertTrue(safeScreenX + s.safeWorldWidth * s.scale <= windowWidth,
                () -> "la zone sûre déborde à droite : " + s + " en " + windowWidth + "x" + windowHeight);
        assertTrue(safeScreenY + s.safeWorldHeight * s.scale <= windowHeight,
                () -> "la zone sûre déborde en haut : " + s + " en " + windowWidth + "x" + windowHeight);
    }

    @Nested
    @DisplayName("Multiples exacts de la taille de monde minimale")
    class ExactMultiples {

        @ParameterizedTest(name = "{0}x{1} -> échelle {2}")
        @CsvSource({
                "320,180,1",
                "640,360,2",
                "960,540,3",
                "1280,720,4",
                "1600,900,5",
                "1920,1080,6",
                "3840,2160,12",
        })
        void keepsExactlyTheMinimumAreaAndFillsTheWindow(int width, int height, int expectedScale) {
            PixelScale s = at(width, height);

            assertEquals(expectedScale, s.scale);
            assertEquals(MIN_W, s.safeWorldWidth, "aucun élargissement nécessaire sur un multiple exact");
            assertEquals(MIN_H, s.safeWorldHeight, "aucun élargissement nécessaire sur un multiple exact");
            assertEquals(0, s.bleedX, "un ajustement exact n'a aucune gouttière à boucher");
            assertEquals(0, s.bleedY, "un ajustement exact n'a aucune gouttière à boucher");
            assertEquals(MIN_W, s.worldWidth);
            assertEquals(MIN_H, s.worldHeight);
            assertEquals(width, s.screenWidth);
            assertEquals(height, s.screenHeight);
            assertEquals(0, s.screenX);
            assertEquals(0, s.screenY);
            assertSafeAreaFullyOnScreen(s, width, height);
        }
    }

    @Nested
    @DisplayName("Fenêtres qui ne sont pas un multiple de la taille de monde minimale")
    class NonMultiples {

        @Test
        @DisplayName("800x450 - échelle 2, le monde s'élargit à 400x225 sans débordement")
        void expandsRatherThanLetterboxes() {
            PixelScale s = at(800, 450);

            assertEquals(2, s.scale);
            assertEquals(400, s.safeWorldWidth);
            assertEquals(225, s.safeWorldHeight);
            assertEquals(0, s.bleedX);
            assertEquals(0, s.bleedY);
            assertEquals(800, s.screenWidth);
            assertEquals(450, s.screenHeight);
            assertEquals(0, s.screenX);
            assertEquals(0, s.screenY);
            assertSafeAreaFullyOnScreen(s, 800, 450);
        }

        @Test
        @DisplayName("1000x543 - taille bâtarde : zone sûre entière, gouttière bouchée par le débordement")
        void awkwardSizeKeepsTheSafeAreaIntact() {
            PixelScale s = at(1000, 543);

            assertEquals(3, s.scale);
            assertEquals(333, s.safeWorldWidth);   // floor(1000 / 3)
            assertEquals(181, s.safeWorldHeight);  // 543 / 3, exact
            assertEquals(1, s.bleedX, "1 px de reliquat horizontal : une colonne de débordement de chaque côté");
            assertEquals(0, s.bleedY, "la hauteur tombe juste");
            assertEquals(335, s.worldWidth);
            assertEquals(181, s.worldHeight);
            assertEquals(-3, s.screenX);
            assertEquals(0, s.screenY);
            assertSafeAreaFullyOnScreen(s, 1000, 543);
        }

        @Test
        @DisplayName("1281x720 - la régression : le bord de la zone garantie n'est plus coupé")
        void theGuaranteedEdgeIsNoLongerClipped() {
            PixelScale s = at(1281, 720);

            assertEquals(4, s.scale);
            assertEquals(320, s.safeWorldWidth, "320 colonnes entières tiennent dans 1281 px à l'échelle 4");
            assertEquals(180, s.safeWorldHeight);
            assertEquals(1, s.bleedX);
            assertEquals(0, s.bleedY);

            // La zone sûre commence pile au pixel écran 0 et se termine à 1280 : les 320 colonnes
            // occupent 4 px chacune, y compris la toute première. C'est exactement ce qui était faux.
            assertEquals(0, s.screenX + s.bleedX * s.scale);
            assertEquals(1280, s.safeWorldWidth * s.scale);
            assertSafeAreaFullyOnScreen(s, 1281, 720);
        }

        @Test
        @DisplayName("1281x721 - reliquat sur les deux axes en même temps")
        void leftoverOnBothAxes() {
            PixelScale s = at(1281, 721);

            assertEquals(4, s.scale);
            assertEquals(1, s.bleedX);
            assertEquals(1, s.bleedY);
            assertEquals(320, s.safeWorldWidth);
            assertEquals(180, s.safeWorldHeight);
            assertSafeAreaFullyOnScreen(s, 1281, 721);
        }

        @Test
        @DisplayName("L'échelle ne devient jamais fractionnaire : 1279x719 reste à 3, pas 3,99")
        void neverUsesAFractionalScale() {
            PixelScale s = at(1279, 719);

            assertEquals(3, s.scale);
            assertTrue(s.safeWorldWidth >= MIN_W);
            assertTrue(s.safeWorldHeight >= MIN_H);
            assertSafeAreaFullyOnScreen(s, 1279, 719);
        }
    }

    @Nested
    @DisplayName("Très petites fenêtres")
    class SmallWindows {

        @ParameterizedTest(name = "{0}x{1}")
        @CsvSource({
                "319,179",
                "160,90",
                "100,50",
                "16,16",
                "1,1",
        })
        void scaleNeverDropsBelowOne(int width, int height) {
            PixelScale s = at(width, height);

            assertEquals(1, s.scale, "sous le minimum, on montre moins de monde plutôt que de rétrécir les pixels");
            assertEquals(width, s.safeWorldWidth);
            assertEquals(height, s.safeWorldHeight);
            assertEquals(width, s.screenWidth);
            assertEquals(height, s.screenHeight);
            assertEquals(0, s.screenX);
            assertEquals(0, s.screenY);
            assertSafeAreaFullyOnScreen(s, width, height);
        }

        @ParameterizedTest(name = "{0}x{1}")
        @CsvSource({"0,0", "-100,-100", "0,720"})
        void degenerateSizesAreClampedInsteadOfDividingByZero(int width, int height) {
            PixelScale s = at(width, height);

            assertEquals(1, s.scale);
            assertTrue(s.safeWorldWidth >= 1);
            assertTrue(s.safeWorldHeight >= 1);
        }

        @Test
        @DisplayName("Un monde minimal de 1x1 est légal")
        void supportsATinyMinimumWorld() {
            PixelScale s = PixelScale.compute(100, 100, 1, 1);

            assertEquals(100, s.scale);
            assertEquals(1, s.safeWorldWidth);
            assertEquals(1, s.safeWorldHeight);
            assertSafeAreaFullyOnScreen(s, 100, 100);
        }

        @Test
        @DisplayName("Un monde minimal de 1x1 sur une fenêtre bâtarde garde sa zone sûre entière")
        void tinyMinimumWorldOnAnAwkwardWindow() {
            PixelScale s = PixelScale.compute(101, 100, 1, 1);

            assertEquals(100, s.scale);
            assertEquals(1, s.safeWorldWidth);
            assertEquals(1, s.bleedX);
            assertSafeAreaFullyOnScreen(s, 101, 100);
        }
    }

    @Nested
    @DisplayName("Ratios extrêmes")
    class ExtremeAspectRatios {

        @Test
        @DisplayName("Ultra-large - c'est la hauteur qui limite")
        void ultraWideExpandsHorizontally() {
            PixelScale s = at(3840, 200);

            assertEquals(1, s.scale, "la hauteur n'autorise qu'une échelle de 1");
            assertEquals(3840, s.safeWorldWidth, "la largeur en trop montre plus de monde");
            assertEquals(200, s.safeWorldHeight);
            assertSafeAreaFullyOnScreen(s, 3840, 200);
        }

        @Test
        @DisplayName("Ultra-haut - c'est la largeur qui limite")
        void ultraTallExpandsVertically() {
            PixelScale s = at(400, 2000);

            assertEquals(1, s.scale);
            assertEquals(400, s.safeWorldWidth);
            assertEquals(2000, s.safeWorldHeight);
            assertSafeAreaFullyOnScreen(s, 400, 2000);
        }

        @Test
        @DisplayName("Une fenêtre large à forte échelle garde des pixels carrés sur les deux axes")
        void wideWindowKeepsASingleScaleForBothAxes() {
            PixelScale s = at(5120, 1440);

            assertEquals(8, s.scale, "min(5120/320, 1440/180) = min(16, 8)");
            assertEquals(640, s.safeWorldWidth);
            assertEquals(180, s.safeWorldHeight);
            assertSafeAreaFullyOnScreen(s, 5120, 1440);
        }

        @Test
        @DisplayName("Une fenêtre en forme de boîte aux lettres ne produit jamais de bandes noires")
        void noLetterboxing() {
            PixelScale s = at(2560, 600);

            assertTrue(s.screenWidth >= 2560, "le viewport couvre toute la largeur de la fenêtre");
            assertTrue(s.screenHeight >= 600, "le viewport couvre toute la hauteur de la fenêtre");
        }
    }

    @Nested
    @DisplayName("Invariants sur un balayage exhaustif des tailles de fenêtre")
    class Invariants {

        /**
         * Balayage au pas de 1 sur les deux axes. L'ancien test avançait de 7 en largeur et de 11 en
         * hauteur, ce qui ne visitait ni 1281 ni 1281x721 — les deux tailles qui révélaient le
         * défaut. Un invariant qui n'est vrai qu'aux tailles échantillonnées ne prouve rien.
         */
        @Test
        void holdForEveryWindowSizeInRange() {
            for (int w = 1; w <= 2000; w++) {
                for (int h = 1; h <= 1200; h++) {
                    PixelScale s = at(w, h);

                    assertTrue(s.scale >= 1, "l'échelle reste entière et positive");

                    // Le monde est un nombre entier de pixels-monde et le viewport un multiple exact.
                    assertEquals(s.worldWidth * s.scale, s.screenWidth);
                    assertEquals(s.worldHeight * s.scale, s.screenHeight);

                    // Pas de bandes noires : le viewport couvre toujours la fenêtre...
                    assertTrue(s.screenX <= 0 && s.screenY <= 0);
                    assertTrue(s.screenX + s.screenWidth >= w);
                    assertTrue(s.screenY + s.screenHeight >= h);

                    // ...et le débordement ne dépasse jamais une colonne / une ligne par bord.
                    assertTrue(s.bleedX == 0 || s.bleedX == 1);
                    assertTrue(s.bleedY == 0 || s.bleedY == 1);
                    assertEquals(s.safeWorldWidth + 2 * s.bleedX, s.worldWidth);
                    assertEquals(s.safeWorldHeight + 2 * s.bleedY, s.worldHeight);

                    // L'affirmation du projet, vérifiée en pixels écran et non en pixels calculés.
                    assertSafeAreaFullyOnScreen(s, w, h);

                    // La zone garantie est honorée dès que la fenêtre est assez grande pour elle.
                    if (w >= MIN_W && h >= MIN_H) {
                        assertTrue(s.safeWorldWidth >= MIN_W);
                        assertTrue(s.safeWorldHeight >= MIN_H);
                    }
                }
            }
        }

        /**
         * À échelle constante, élargir la fenêtre montre plus de monde. Franchir un palier d'échelle
         * est le seul cas où la zone sûre rétrécit : le zoom augmente d'un cran, donc on voit moins
         * de monde mais plus gros. C'est voulu — et c'est précisément pour cela que la monotonie ne
         * peut être affirmée qu'<em>à l'intérieur</em> d'un palier.
         */
        @Test
        @DisplayName("À échelle constante, agrandir la fenêtre ne réduit jamais la zone sûre")
        void safeAreaGrowsMonotonicallyWithinAScaleStep() {
            int previousScale = -1;
            int previousWidth = 0;
            for (int w = MIN_W; w <= 4000; w++) {
                PixelScale s = at(w, 1080);
                if (s.scale == previousScale) {
                    assertTrue(s.safeWorldWidth >= previousWidth,
                            "la zone sûre a rétréci à échelle constante en passant à " + w + " px : " + s);
                }
                previousScale = s.scale;
                previousWidth = s.safeWorldWidth;
            }
        }

        /**
         * Le corollaire qui compte réellement au franchissement d'un palier : quoi qu'il arrive à la
         * zone sûre, elle ne descend jamais sous la zone garantie.
         */
        @Test
        @DisplayName("Franchir un palier d'échelle ne fait jamais passer sous la zone garantie")
        void crossingAScaleStepNeverBreachesTheGuaranteedArea() {
            for (int w = MIN_W; w <= 4000; w++) {
                PixelScale s = at(w, 1080);
                assertTrue(s.safeWorldWidth >= MIN_W, () -> "zone sûre sous le minimum : " + s);
            }
            for (int h = MIN_H; h <= 2400; h++) {
                PixelScale s = at(3840, h);
                assertTrue(s.safeWorldHeight >= MIN_H, () -> "zone sûre sous le minimum : " + s);
            }
        }

        @Test
        @DisplayName("Les tailles absurdes sont bornées au lieu de déborder en entier négatif")
        void absurdSizesAreClampedInsteadOfOverflowing() {
            for (int[] size : new int[][]{
                    {Integer.MAX_VALUE, Integer.MAX_VALUE},
                    {Integer.MAX_VALUE, 1080},
                    {1920, Integer.MAX_VALUE},
            }) {
                PixelScale s = PixelScale.compute(size[0], size[1], MIN_W, MIN_H);

                assertTrue(s.scale >= 1, () -> "échelle négative : " + s);
                assertTrue(s.worldWidth >= 1, () -> "largeur de monde négative : " + s);
                assertTrue(s.worldHeight >= 1, () -> "hauteur de monde négative : " + s);
                assertTrue(s.screenWidth >= 1, () -> "largeur de viewport négative : " + s);
                assertTrue(s.screenHeight >= 1, () -> "hauteur de viewport négative : " + s);
                assertEquals(s.worldWidth * s.scale, s.screenWidth);
                assertEquals(s.worldHeight * s.scale, s.screenHeight);
            }

            PixelScale huge = PixelScale.compute(4000, 2400, Integer.MAX_VALUE, Integer.MAX_VALUE);
            assertEquals(1, huge.scale, "un monde minimal absurde retombe à l'échelle 1");
            assertTrue(huge.worldWidth >= 1);
            assertTrue(huge.worldHeight >= 1);
        }
    }

    /**
     * Les résolutions d'écran réelles, c'est-à-dire ce que la touche « plein écran » donne au
     * viewport.
     *
     * <h2>Pourquoi elles méritent leur propre test</h2>
     *
     * <p>Le plein écran est signalé depuis le premier jalon comme « implémenté mais jamais testé
     * automatiquement », et pour une bonne raison : on ne peut pas ouvrir de fenêtre dans un test.
     * Mais ce que <code>F11</code> fait réellement tient en une ligne — il passe au viewport la
     * résolution du moniteur — et cette partie-là, elle, est du calcul pur.
     *
     * <p>Le balayage exhaustif s'arrête à 2000x1200, ce qui couvre le 1080p et rien au-delà. Or les
     * résolutions qui comptent aujourd'hui sont précisément celles qu'il ne visite pas : le 1440p,
     * l'ultra-large 21:9, le 4K. Un test qui s'arrête juste avant les tailles où le jeu passera le
     * plus clair de son temps en plein écran ne dit pas grand-chose.
     */
    @Nested
    @DisplayName("Résolutions d'écran réelles, celles que donne le plein écran")
    class FullscreenResolutions {

        /** Ce qu'un moniteur rend vraiment, du portable au 4K, ratios exotiques compris. */
        private static final int[][] DISPLAY_MODES = {
                {1280, 720},   {1366, 768},   {1440, 900},   {1600, 900},
                {1680, 1050},  {1920, 1080},  {1920, 1200},  {2048, 1152},
                {2560, 1080},  // 21:9
                {2560, 1440},  {2560, 1600},
                {3440, 1440},  // 21:9 large
                {3840, 1080},  // double écran 32:9
                {3840, 2160},  // 4K
                {5120, 1440},  // super ultra-large
        };

        @Test
        @DisplayName("Aucune résolution d'écran ne rogne la zone garantie")
        void noDisplayModeEverClipsTheGuaranteedArea() {
            for (int[] mode : DISPLAY_MODES) {
                int w = mode[0];
                int h = mode[1];
                PixelScale s = at(w, h);
                String where = w + "x" + h;

                assertTrue(s.scale >= 1, where + " : echelle non entiere ou nulle");
                assertTrue(s.safeWorldWidth >= MIN_W,
                        where + " : zone garantie retrecie a " + s.safeWorldWidth + " de large");
                assertTrue(s.safeWorldHeight >= MIN_H,
                        where + " : zone garantie retrecie a " + s.safeWorldHeight + " de haut");
                assertSafeAreaFullyOnScreen(s, w, h);

                // Pas de bandes noires, et pas plus d'une colonne de débordement par bord.
                assertTrue(s.screenX <= 0 && s.screenY <= 0, where + " : bandes noires");
                assertTrue(s.screenX + s.screenWidth >= w, where + " : le viewport ne couvre pas");
                assertTrue(s.screenY + s.screenHeight >= h, where + " : le viewport ne couvre pas");
                assertTrue(s.bleedX <= 1 && s.bleedY <= 1, where + " : debordement de plus d'un pixel");
            }
        }

        /**
         * Un pixel-monde doit rester carré : c'est la promesse du projet depuis le premier jalon, et
         * les ratios exotiques — 32:9, 21:9 — sont exactement ce qui la mettrait en défaut si
         * l'échelle était calculée par axe.
         */
        @Test
        @DisplayName("Le pixel reste carré même en 32:9")
        void thePixelStaysSquareEvenOnExtremeRatios() {
            for (int[] mode : DISPLAY_MODES) {
                PixelScale s = at(mode[0], mode[1]);
                String where = mode[0] + "x" + mode[1];

                assertEquals(s.worldWidth * s.scale, s.screenWidth, where + " : pixel non carre en x");
                assertEquals(s.worldHeight * s.scale, s.screenHeight, where + " : pixel non carre en y");
            }
        }

        /**
         * Passer en plein écran ne doit jamais <em>réduire</em> ce qu'on voit : le moniteur est plus
         * grand que la fenêtre, donc la zone garantie doit au moins tenir aussi bien.
         */
        @Test
        @DisplayName("Le plein écran ne montre jamais moins que la fenêtre qu'il remplace")
        void goingFullscreenNeverShowsLessThanTheWindow() {
            PixelScale windowed = at(1280, 720);
            for (int[] mode : DISPLAY_MODES) {
                if (mode[0] < 1280 || mode[1] < 720) {
                    continue; // un moniteur plus petit que la fenêtre n'est pas le cas qu'on décrit
                }
                PixelScale full = at(mode[0], mode[1]);
                String where = mode[0] + "x" + mode[1];

                assertTrue(full.safeWorldWidth >= windowed.safeWorldWidth,
                        where + " : la zone garantie retrecit en passant en plein ecran ("
                                + windowed.safeWorldWidth + " -> " + full.safeWorldWidth + ")");
                assertTrue(full.safeWorldHeight >= windowed.safeWorldHeight,
                        where + " : la zone garantie retrecit en hauteur ("
                                + windowed.safeWorldHeight + " -> " + full.safeWorldHeight + ")");
            }
        }
    }
}
