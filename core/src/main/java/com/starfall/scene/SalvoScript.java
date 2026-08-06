package com.starfall.scene;

import com.starfall.game.ActionResult;
import com.starfall.game.Arena;
import com.starfall.game.Tile;

import java.util.List;
import java.util.function.Function;

/**
 * La <b>salve qui se déroule</b> : une ligne courte, puis le lâcher, montré temps par temps.
 *
 * <h2>Pourquoi cet écran existe</h2>
 *
 * <p>L'animation avait une faiblesse que j'ai signalée avant qu'une review ne la trouve : la règle
 * qui fait <b>taire les calques du repos</b> pendant le déroulé — menaces, intentions, repère du
 * héros — n'avait <em>aucun témoin</em>. Le mode capture ne déroulait jamais, donc les 84 planches
 * ne pouvaient pas l'attraper ; sa seule défense était d'être écrite à un seul endroit. Ce projet
 * sait ce que vaut un garde-fou qu'on croit sur parole.
 *
 * <p>Cet écran le lui donne. Chaque image montre <b>un temps</b> du déroulé : les figures y sont
 * celles de cet instant-là, la file s'y vide tuile par tuile, et aucun calque du repos n'y paraît.
 * Casser l'une de ces trois règles change des pixels, et {@code verifyRender} le dit.
 *
 * <p>Il montre aussi, pour un lecteur humain, ce que le geste central du jeu donne à voir — ce que
 * ni le texte ni une planche au repos ne peuvent raconter.
 */
public final class SalvoScript {

    /** Nom de scène qui sélectionne ce scénario. */
    public static final String SCENE_NAME = "salvo";

    /**
     * Le chargement, puis le lâcher. Trois tuiles pour trois temps.
     *
     * <p>Volte-face et pas de côté sont <b>gratuites</b> : elles se posent sans offrir de tour aux
     * ennemis, ce qui garde la ligne courte et la met à l'abri d'un rééquilibrage. L'estoc, lui,
     * porte le coup — un déroulé dont aucun temps ne frappe ne montrerait pas grand-chose.
     */
    public static final List<Function<Arena, ActionResult>> ACTIONS = List.of(
            a -> a.queueTile(Tile.PIVOT),
            a -> a.queueTile(Tile.SIDESTEP),
            a -> a.queueTile(Tile.THRUST),
            a -> a.unleash());

    /** Première image qui montre un temps du déroulé. Les précédentes montrent le chargement. */
    public static final int FIRST_BEAT_FRAME = ACTIONS.size();

    /** Nombre de temps de la salve : autant que de tuiles chargées. */
    public static final int BEATS = 3;

    private SalvoScript() {
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
            // Les gestes, PUIS un temps par tuile de la salve. C'est tout l'objet de cet écran :
            // ses dernières images ne rejouent aucun geste de plus, elles égrènent la résolution.
            return ACTIONS.size() + BEATS;
        }

        @Override
        public int playbackBeatAt(int frameIndex) {
            // Les images du chargement montrent l'état au repos ; celles d'après égrènent les
            // temps. Le rejeu de la scène applique tous les gestes dès que l'image dépasse leur
            // nombre, si bien que ces images-là partent toutes du même état final et ne diffèrent
            // que par le temps montré. C'est ce qui les rend reproductibles.
            return frameIndex <= FIRST_BEAT_FRAME ? 0 : frameIndex - FIRST_BEAT_FRAME;
        }

        @Override
        public int rackSlotAt(int frameIndex) {
            // Un survol de râtelier PENDANT le déroulé : sans lui, la règle qui éteint les halos
            // n'aurait pas de témoin — les autres écrans ne survolent rien sur leurs images de
            // déroulé. Ici la souris reste posée sur la première tuile, et son halo doit rester
            // éteint tant que les entrées sont bloquées.
            return frameIndex > FIRST_BEAT_FRAME ? 0 : -1;
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
