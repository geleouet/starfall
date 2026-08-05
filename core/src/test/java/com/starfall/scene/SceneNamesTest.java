package com.starfall.scene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.starfall.LaunchOptions;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Le pont entre les <b>noms de scène</b> et les <b>scènes</b>.
 *
 * <h2>Pourquoi ce test existe</h2>
 *
 * <p>Trois tables citent les mêmes noms sans se parler : celle que la ligne de commande accepte
 * ({@link LaunchOptions#SCENES}), l'aiguillage qui instancie la scène, et la résolution du scénario
 * de capture. Elles ont déjà divergé : une scène rendait {@code "arena"} alors qu'elle jouait la
 * vitrine — « un nom qui ment est un nom qu'on finira par croire ».
 *
 * <p>Le correctif a fait porter son nom à chaque scène, mais rien ne <b>reliait</b> ces noms à la
 * table de la ligne de commande. Une review l'a relevé : « aucun test ne confronte {@code
 * Scene.name()} à {@code LaunchOptions.SCENES} ». C'est ce que fait celui-ci, dans les deux sens —
 * une scène qu'on ne peut pas demander est aussi inutile qu'un nom qu'on ne peut pas instancier.
 */
class SceneNamesTest {

    /** Les scènes que le jeu sait construire, dans l'ordre de leur nom. */
    private static List<Scene> everyScene() {
        List<Scene> scenes = new ArrayList<>();
        scenes.add(new ArenaScene());
        scenes.add(new ArenaScene(ShowcaseScript.SCENARIO));
        scenes.add(new CalibrationScene());
        return scenes;
    }

    @Test
    @DisplayName("Chaque scène porte un nom que la ligne de commande accepte")
    void everySceneHasACommandLineName() {
        for (Scene scene : everyScene()) {
            assertTrue(LaunchOptions.SCENES.contains(scene.name()),
                    "la scene rend le nom « " + scene.name() + " », absent de la table des scenes "
                            + LaunchOptions.SCENES);
        }
    }

    @Test
    @DisplayName("Chaque nom accepté désigne une scène différente")
    void everyAcceptedNameDesignatesADistinctScene() {
        List<String> names = new ArrayList<>();
        for (Scene scene : everyScene()) {
            names.add(scene.name());
        }

        assertEquals(LaunchOptions.SCENES.size(), names.size(),
                "la table des scenes en annonce " + LaunchOptions.SCENES.size()
                        + " et le jeu en construit " + names.size() + " : " + names);
        assertEquals(LaunchOptions.SCENES.size(), List.copyOf(new java.util.LinkedHashSet<>(names))
                .size(), "deux scenes portent le meme nom : " + names);
        assertTrue(names.containsAll(LaunchOptions.SCENES),
                "des noms acceptes ne designent aucune scene : " + LaunchOptions.SCENES
                        + " contre " + names);
    }

    /**
     * Et les deux scènes qui ont un scénario de capture se retrouvent par leur nom.
     *
     * <p>La mire n'en a pas — c'est la seule exception, et elle est explicite : {@code forScene} la
     * refuse, comme n'importe quel nom sans scénario.
     */
    @Test
    @DisplayName("Un nom de scène retrouve son scénario, ou est refusé")
    void aSceneNameFindsItsScenarioOrIsRefused() {
        assertEquals(CaptureScript.ACTIONS.size(),
                CaptureScenario.forScene(new ArenaScene().name()).size());
        assertEquals(ShowcaseScript.ACTIONS.size(),
                CaptureScenario.forScene(new ArenaScene(ShowcaseScript.SCENARIO).name()).size());

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> CaptureScenario.forScene(new CalibrationScene().name()),
                "la mire n'a pas de scenario de capture : forScene doit le dire");
    }
}
