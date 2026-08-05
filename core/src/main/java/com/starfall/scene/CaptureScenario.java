package com.starfall.scene;

import com.starfall.game.ActionResult;
import com.starfall.game.Arena;

import java.util.List;
import java.util.function.Function;

/**
 * Un scénario de capture : une suite de gestes, et ce qui est survolé à chaque image.
 *
 * <h2>Pourquoi il en faut plus d'un</h2>
 *
 * <p>Le scénario principal est une <b>ligne gagnante</b> : il traverse les quatre vagues et finit
 * sur la bannière de victoire. C'est ce qu'il faut pour garder le jeu tel qu'on le joue — mais un
 * jeu qu'on joue bien ne montre jamais ce qui arrive quand on joue mal, ni les cas rares.
 *
 * <p>Quatre états leur ont échappé, tous relevés par une review : la <b>bannière de défaite</b>, le
 * mur d'une <b>poussée qui bute</b>, le filtre qui empêche de surligner une case qu'un clic ne peut
 * pas atteindre, et la <b>reprise</b> d'une tuile — seul geste gratuit du jeu. Les allonger dans la
 * ligne gagnante était impossible : elle se termine par une victoire, et rien ne se joue après.
 *
 * <p>D'où cette abstraction, et une seconde implémentation — {@link ShowcaseScript} — dont le seul
 * métier est d'exhiber ce que la bonne ligne ne rencontre pas.
 */
public interface CaptureScenario {

    /** Les gestes, dans l'ordre. L'image {@code n} montre l'état après les {@code n} premiers. */
    List<Function<Arena, ActionResult>> actions();

    /** Tuile du râtelier survolée à cette image, ou {@code -1}. */
    int rackSlotAt(int frameIndex);

    /** Emplacement de la file survolé à cette image, ou {@code -1}. */
    int queueSlotAt(int frameIndex);

    /** Case du plateau survolée à cette image, ou {@code -1}. */
    int cellAt(int frameIndex);

    /** Nombre de gestes. La dernière image porte le numéro {@code size()}. */
    default int size() {
        return actions().size();
    }

    /** Rejoue les {@code count} premiers gestes sur une arène. */
    default ActionResult replayInto(Arena arena, int count) {
        ActionResult last = null;
        List<Function<Arena, ActionResult>> gestures = actions();
        for (int i = 0; i < count && i < gestures.size(); i++) {
            last = gestures.get(i).apply(arena);
        }
        return last;
    }

    /**
     * Le scénario d'une scène.
     *
     * <p>La résolution vit ici plutôt que dans la scène : c'est la seule règle qui lie un nom de
     * ligne de commande à un scénario, et la borne de {@code --from} en a besoin autant que le
     * rendu. Écrite à deux endroits, elle aurait divergé — ce projet en a l'habitude.
     */
    static CaptureScenario forScene(String scene) {
        if (ShowcaseScript.SCENE_NAME.equals(scene)) {
            return ShowcaseScript.SCENARIO;
        }
        if (CaptureScript.SCENE_NAME.equals(scene)) {
            return CaptureScript.SCENARIO;
        }
        // Elle rendait la ligne gagnante pour TOUT le reste, y compris « calibration », « banane »
        // et null. Son voisin StarfallGame.sceneFor fait l'inverse et son javadoc revendique le
        // principe : « un ajout de scène oublié ici échoue bruyamment plutôt que d'afficher la
        // mauvaise ». Deux tables de noms, deux disciplines opposées : c'est celle qui devine qui
        // avait tort.
        throw new IllegalArgumentException(
                "aucun scenario de capture pour la scene « " + scene + " »");
    }
}
