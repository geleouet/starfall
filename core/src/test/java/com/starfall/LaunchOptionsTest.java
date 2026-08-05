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

        /**
         * {@code --from} décale la première image du <b>scénario</b>, pas le compte d'images écrites.
         *
         * <p>L'option existe pour garder la bannière de fin sans versionner les 88 images d'une
         * partie complète. Elle a été testée dès son ajout, parce qu'une review a montré ce que
         * coûte une option de ligne de commande non testée : {@code --grid} et {@code --scene} ne
         * l'étaient pas, et c'est ce qui a laissé passer un défaut de code de sortie.
         */
        @Test
        void parsesTheFirstFrame() {
            LaunchOptions options = parse("--screenshot", "captures/fin", "--frames", "8",
                    "--from", "26");

            assertEquals(8, options.frames);
            assertEquals(26, options.firstFrame);
        }

        /** Zéro est le défaut, donc zéro doit être accepté : refuser sa propre valeur par défaut
         * serait une drôle de validation. */
        @Test
        void acceptsZeroAsFirstFrame() {
            assertEquals(0, parse("--screenshot", "captures/fin", "--from", "0").firstFrame);
            assertEquals(0, parse("--screenshot", "captures/fin").firstFrame);
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

        /**
         * Demander une image que le scénario n'a pas est une erreur de ligne de commande.
         *
         * <p>Sans ce contrôle, {@code --from 200} écrivait trois PNG rigoureusement identiques et
         * sortait en 0. Le projet avait déjà eu ce défaut sous la forme d'un {@code --frames N} qui
         * écrivait N copies de la même image ; {@code --from} rouvrait la même porte, et une review
         * l'a mesuré : trois fichiers de 8917 octets chacun.
         */
        @Test
        @DisplayName("Une première image au-delà du scénario est refusée")
        void aFirstFrameBeyondTheScriptIsRejected() {
            int last = com.starfall.scene.CaptureScript.ACTIONS.size();
            // La dernière image du scénario est celle qui suit le dernier geste : elle est légale.
            assertEquals(last, parse("--screenshot", "captures/fin", "--from", String.valueOf(last),
                    "--frames", "1").firstFrame);

            assertThrows(IllegalArgumentException.class,
                    () -> parse("--screenshot", "captures/fin", "--from", String.valueOf(last + 1),
                            "--frames", "1"),
                    "une image au-dela du scenario aurait du etre refusee");
            assertThrows(IllegalArgumentException.class,
                    () -> parse("--screenshot", "captures/fin", "--from", String.valueOf(last),
                            "--frames", "2"),
                    "deux images depuis la derniere auraient du etre refusees");
        }

        /**
         * La borne de {@code --from} vient du scénario de <b>cette</b> scène, pas d'un scénario
         * unique.
         *
         * <p>Le commentaire du code l'affirmait ; rien ne le tenait. Une review l'a montré en
         * remplaçant la résolution par scène par la longueur de la ligne gagnante : les 477 tests
         * restaient verts, et {@code --scene showcase --from 12} redevenait accepté — c'est-à-dire
         * que le défaut « des images identiques écrites en silence » se rouvrait pour la vitrine.
         *
         * <p>Les deux scénarios n'ont pas la même longueur, et c'est tout ce qu'il faut pour que ce
         * test morde.
         */
        @Test
        @DisplayName("La borne de --from suit le scénario de la scène demandée")
        void theFirstFrameBoundFollowsTheSceneScenario() {
            int showcase = com.starfall.scene.ShowcaseScript.ACTIONS.size();
            int winning = com.starfall.scene.CaptureScript.ACTIONS.size();
            assertTrue(showcase < winning,
                    "les deux scenarios ont la meme longueur : ce test ne mord plus");

            // Ce que la ligne gagnante accepte, la vitrine doit le refuser.
            assertEquals(showcase + 1, parse("--screenshot", "captures/x", "--scene", "arena",
                    "--from", String.valueOf(showcase + 1), "--frames", "1").firstFrame);
            assertThrows(IllegalArgumentException.class,
                    () -> parse("--screenshot", "captures/x", "--scene", "showcase",
                            "--from", String.valueOf(showcase + 1), "--frames", "1"),
                    "la vitrine ne compte que " + showcase + " gestes");
        }

        /**
         * {@code --help} sort avant tout refus, quelles que soient les options qui l'accompagnent.
         *
         * <p>Le refus de la vitrine hors capture s'était interposé : « --help --scene showcase »
         * rendait le code 2 et l'usage sur la sortie d'erreur, alors que l'option est documentée
         * « affiche l'aide et quitte ». Une option qui explique le programme ne peut pas être
         * refusée par le programme.
         */
        @Test
        @DisplayName("--help sort avant tout refus, quoi qu'il l'accompagne")
        void helpEscapesEveryRefusal() {
            assertTrue(parse("--help", "--scene", "showcase").helpRequested);
            assertTrue(parse("--scene", "showcase", "--help").helpRequested);
            assertTrue(parse("--help", "--from", "9999", "--screenshot", "x").helpRequested);
        }

        /**
         * Et une simulation n'ouvre aucune fenêtre : le refus de scène y est sans objet, et le
         * poser reviendrait à refuser une commande qui marche.
         */
        @Test
        @DisplayName("--simulate échappe au refus de scène")
        void simulationEscapesTheSceneRefusal() {
            assertEquals(3, parse("--simulate", "3", "--scene", "showcase").simulations);
        }

        /** Et la mire, qui n'a pas de scénario du tout, n'est pas bornée. */
        @Test
        @DisplayName("La mire de calibration échappe à la borne, faute de scénario")
        void theCalibrationSceneHasNoScriptBound() {
            assertEquals(500, parse("--screenshot", "captures/x", "--scene", "calibration",
                    "--from", "500", "--frames", "1").firstFrame);
        }

        @ParameterizedTest(name = "--from {0}")
        @ValueSource(strings = {"-1", "-80", "quatre", "8.5", ""})
        @DisplayName("Une première image négative ou informe est refusée")
        void anInvalidFirstFrameIsRejected(String value) {
            assertThrows(IllegalArgumentException.class, () -> parse("--from", value),
                    "aurait du refuser : " + value);
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
