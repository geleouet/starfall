package com.starfall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.starfall.scene.Scene;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * L'aiguillage nom → scène, éprouvé <b>en l'appelant</b>.
 *
 * <h2>Pourquoi il vit ici et pas à côté des scènes</h2>
 *
 * <p>Un test existait déjà pour relier les noms de scène aux scènes — mais {@code sceneFor} était
 * {@code private static}, si bien qu'il ne pouvait pas l'appeler et <b>recopiait le switch à la
 * main</b>. Une review l'a montré : en faisant instancier la ligne gagnante sous le nom de la
 * vitrine, les 491 tests restaient verts et seul le garde-fou d'image le voyait.
 *
 * <p>Un pont qui recopie ce qu'il relie ne relie rien. Celui-ci appelle l'aiguillage réel, ce qui
 * demande d'être dans son paquet — c'est le prix, et il est petit.
 */
class SceneSwitchTest {

    @Test
    @DisplayName("Chaque nom accepté construit la scène qui porte ce nom")
    void everyAcceptedNameBuildsTheSceneThatBearsIt() {
        for (String name : LaunchOptions.SCENES) {
            Scene scene = StarfallGame.sceneFor(name);
            assertEquals(name, scene.name(),
                    "le nom « " + name + " » construit une scene qui se nomme « " + scene.name()
                            + " » : l'aiguillage et le nom ne disent pas la meme chose");
        }
    }

    /**
     * Et un nom inconnu échoue <b>bruyamment</b>, ce que le javadoc de l'aiguillage revendique :
     * « un ajout de scène oublié ici échoue bruyamment plutôt que d'afficher la mauvaise ».
     */
    @Test
    @DisplayName("Un nom de scène inconnu ne construit rien")
    void anUnknownNameBuildsNothing() {
        assertThrows(IllegalArgumentException.class, () -> StarfallGame.sceneFor("banane"));
        assertThrows(IllegalArgumentException.class, () -> StarfallGame.sceneFor(""));
    }
}
