package com.starfall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.starfall.game.Grid;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests de l'analyse de la ligne de commande.
 *
 * <p>C'est du Java pur, sans GL, et toute la boucle de review du projet en dépend : une option mal
 * orthographiée qui passe inaperçue produit le mauvais résultat en sortie 0.
 */
class LaunchOptionsTest {

    private static LaunchOptions parse(String... args) {
        return LaunchOptions.parse(args);
    }

    @Nested
    @DisplayName("Valeurs par défaut")
    class Defaults {

        @Test
        void noArgumentsMeansPlayableWindow() {
            LaunchOptions options = parse();

            assertNull(options.screenshotDir);
            assertFalse(options.isScreenshotMode());
            assertFalse(options.helpRequested);
            assertEquals(LaunchOptions.DEFAULT_WIDTH, options.width);
            assertEquals(LaunchOptions.DEFAULT_HEIGHT, options.height);
            assertEquals(LaunchOptions.DEFAULT_FRAMES, options.frames);
        }

        @Test
        void nullArgumentsAreTolerated() {
            LaunchOptions options = LaunchOptions.parse(null);

            assertFalse(options.isScreenshotMode());
            assertEquals(LaunchOptions.DEFAULT_WIDTH, options.width);
        }
    }

    @Nested
    @DisplayName("Analyse nominale")
    class HappyPath {

        @Test
        void parsesEveryOption() {
            LaunchOptions options = parse("--screenshot", "captures/m1", "--size", "1000x543",
                    "--frames", "3", "--scene", "calibration", "--grid", "15");

            assertTrue(options.isScreenshotMode());
            assertEquals("captures/m1", options.screenshotDir);
            assertEquals(1000, options.width);
            assertEquals(543, options.height);
            assertEquals(3, options.frames);
            assertEquals("calibration", options.scene);
            assertEquals(15, options.gridWidth);
        }

        @Test
        @DisplayName("Le nom de scène est accepté quelle que soit la casse")
        void theSceneNameIsCaseInsensitive() {
            assertEquals("arena", parse("--scene", "ARENA").scene);
            assertEquals("calibration", parse("--scene", "Calibration").scene);
        }

        @Test
        @DisplayName("Le X de la taille est accepté en majuscule comme en minuscule")
        void sizeSeparatorIsCaseInsensitive() {
            assertEquals(800, parse("--size", "800X450").width);
            assertEquals(450, parse("--size", "800x450").height);
        }

        @Test
        void helpIsFlaggedRatherThanLaunchingTheGame() {
            // L'aide affichait autrefois son texte puis ouvrait quand même une fenêtre.
            assertTrue(parse("--help").helpRequested);
            assertTrue(parse("-h").helpRequested);
        }
    }

    @Nested
    @DisplayName("Analyse stricte")
    class Strictness {

        @Test
        @DisplayName("Une option inconnue est fatale, pas ignorée")
        void unknownOptionIsFatal() {
            // « --frame » au lieu de « --frames » produisait 2 images au lieu de 5, en sortie 0.
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> parse("--screenshot", "out", "--frame", "5"));
            assertTrue(error.getMessage().contains("--frame"));
        }

        @Test
        @DisplayName("Une valeur manquante est fatale")
        void missingValueIsFatal() {
            assertThrows(IllegalArgumentException.class, () -> parse("--screenshot"));
            assertThrows(IllegalArgumentException.class, () -> parse("--size"));
            assertThrows(IllegalArgumentException.class, () -> parse("--frames"));
        }

        @Test
        @DisplayName("Une option ne peut pas servir de valeur à une autre option")
        void anOptionIsNeverSwallowedAsAValue() {
            // Sinon « --screenshot --size 800x450 » créait un dossier nommé littéralement « --size ».
            assertThrows(IllegalArgumentException.class, () -> parse("--screenshot", "--size", "800x450"));
        }

        @ParameterizedTest(name = "--size {0}")
        @ValueSource(strings = {"1280", "x720", "1280x", "1280x720x2", "axb", "1280*720", "", "1280x-720"})
        void malformedSizeIsRejected(String value) {
            assertThrows(IllegalArgumentException.class, () -> parse("--size", value));
        }

        @ParameterizedTest(name = "--frames {0}")
        @ValueSource(strings = {"0", "-1", "deux", "2.5", ""})
        void invalidFrameCountIsRejected(String value) {
            // « --frames 0 » était silencieusement remonté à 1 : une demande absurde valait succès.
            assertThrows(IllegalArgumentException.class, () -> parse("--frames", value));
        }

        @Test
        @DisplayName("Une taille nulle ou négative est refusée au lieu d'être remontée à 1")
        void nonPositiveSizeIsRejected() {
            assertThrows(IllegalArgumentException.class, () -> parse("--size", "0x720"));
            assertThrows(IllegalArgumentException.class, () -> parse("--size", "1280x0"));
        }

        @ParameterizedTest(name = "--grid {0}")
        @ValueSource(strings = {"4", "16", "0", "-3", "100", "neuf", "9.5", ""})
        @DisplayName("Une largeur de grille hors bornes est fatale à l'analyse, pas au démarrage")
        void anOutOfRangeGridWidthIsRejectedAtParseTime(String value) {
            // Ne verifier que la forme laissait « --grid 20 » ouvrir la fenetre puis planter dans le
            // constructeur de Grid, avec le code 1 (« plantage ») au lieu de 2 (« ligne de commande
            // invalide ») - exactement le faux positif que l'analyse stricte doit proscrire.
            assertThrows(IllegalArgumentException.class, () -> parse("--grid", value),
                    "aurait du refuser : " + value);
        }

        @ParameterizedTest(name = "--grid {0}")
        @ValueSource(ints = {5, 9, 15})
        @DisplayName("Les largeurs de grille légales passent")
        void legalGridWidthsAreAccepted(int value) {
            assertEquals(value, parse("--grid", String.valueOf(value)).gridWidth);
        }

        @ParameterizedTest(name = "--scene {0}")
        @ValueSource(strings = {"arene", "menu", "combat", "", "arena2"})
        @DisplayName("Un nom de scène inconnu est refusé")
        void anUnknownSceneIsRejected(String value) {
            assertThrows(IllegalArgumentException.class, () -> parse("--scene", value),
                    "aurait du refuser : " + value);
        }

        @Test
        @DisplayName("Les bornes annoncées par l'aide sont celles que le jeu applique")
        void theUsageTextMatchesTheRealBounds() {
            // Une aide qui annonce d'autres bornes que celles appliquees est un piege a elle seule.
            assertTrue(LaunchOptions.usage().contains(Grid.MIN_WIDTH + " à " + Grid.MAX_WIDTH),
                    LaunchOptions.usage());
        }
    }
}
