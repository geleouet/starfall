package com.starfall.scene;

import com.starfall.game.ActionResult;
import com.starfall.game.Arena;
import com.starfall.game.Direction;

import java.util.List;
import java.util.function.Function;

/**
 * Un <b>pas, puis la riposte</b> : le geste le plus fréquent du jeu, et la réponse qu'il paie.
 *
 * <h2>Pourquoi cet écran existe</h2>
 *
 * <p>Deux règles neuves n'avaient aucun témoin, et l'écran de salve ne pouvait pas le leur donner.
 *
 * <p>La première est que <b>le geste du joueur se déroule, fût-il seul</b>. L'ancienne règle disait
 * qu'une action d'un seul temps ne se déroule pas — vrai quand un déroulé n'était qu'une suite
 * d'images figées, faux depuis que le mouvement est continu. Le pas, geste le plus fréquent du jeu,
 * était devenu le seul à ne pas bouger.
 *
 * <p>La seconde est que <b>la riposte ennemie forme un temps à elle</b>. La phase ennemie se joue
 * après que le modèle a consigné ses temps, si bien que le déroulé s'achevait sur le plateau
 * d'avant la riposte et que la scène, en se reposant, basculait d'un coup sur l'état vrai. C'est
 * exactement l'illisibilité que ce déroulé devait supprimer, repoussée à la fin.
 *
 * <p>La ligne de l'écran de salve ne pouvait montrer ni l'une ni l'autre : sa dernière tuile est une
 * volte-face, qui ne déplace personne, et l'ennemi qui lui survit exécute une intention qui ne le
 * déplace pas non plus. Mesuré, pas supposé — le déroulé de cette salve ne compte que deux temps
 * retenus sur trois enregistrés.
 *
 * <h2>Ce que la ligne fait</h2>
 *
 * <p>Un pas vers la droite, et rien d'autre. Le héros glisse d'une case ; l'ennemi le plus proche
 * répond en avançant d'une. Deux temps, deux mouvements, et l'ordre entre eux <em>est</em> la
 * lecture : voir qui bouge d'abord et qui répond ensuite.
 */
public final class RiposteScript {

    /** Nom de scène qui sélectionne ce scénario. */
    public static final String SCENE_NAME = "riposte";

    /** Un pas, et rien d'autre. Ce qui suit n'est plus du joueur. */
    public static final List<Function<Arena, ActionResult>> ACTIONS = List.of(
            a -> a.step(Direction.RIGHT));

    /** Première image qui montre un temps du déroulé. */
    public static final int FIRST_BEAT_FRAME = ACTIONS.size();

    /**
     * Les instants saisis : le rang du temps, puis où il en est.
     *
     * <p>Chaque temps est pris deux fois, à mi-course et une fois posé. La mi-course est ce qui
     * porte la démonstration — à la fin d'un temps, l'écran montre exactement ce qu'il montrait
     * quand ce jeu n'animait rien.
     */
    public static final float[][] MOMENTS = {
            {1f, 0.50f},
            {1f, 1f},
            {2f, 0.50f},
            {2f, 1f},
    };

    private RiposteScript() {
    }

    /** Le scénario, prêt à être joué par la scène d'arène. */
    public static final CaptureScenario SCENARIO = new CaptureScenario() {

        @Override
        public String sceneName() {
            return SCENE_NAME;
        }

        @Override
        public List<Function<Arena, ActionResult>> actions() {
            return ACTIONS;
        }

        @Override
        public int lastFrame() {
            return ACTIONS.size() + MOMENTS.length;
        }

        @Override
        public int playbackBeatAt(int frameIndex) {
            float[] moment = momentAt(frameIndex);
            return moment == null ? 0 : (int) moment[0];
        }

        @Override
        public float playbackProgressAt(int frameIndex) {
            float[] moment = momentAt(frameIndex);
            return moment == null ? 1f : moment[1];
        }

        /** L'instant saisi par cette image, ou {@code null} si elle montre un état au repos. */
        private float[] momentAt(int frameIndex) {
            int rank = frameIndex - FIRST_BEAT_FRAME - 1;
            return rank >= 0 && rank < MOMENTS.length ? MOMENTS[rank] : null;
        }

        @Override
        public int rackSlotAt(int frameIndex) {
            return -1;
        }

        @Override
        public int queueSlotAt(int frameIndex) {
            return -1;
        }

        @Override
        public int cellAt(int frameIndex) {
            return -1;
        }
    };
}
