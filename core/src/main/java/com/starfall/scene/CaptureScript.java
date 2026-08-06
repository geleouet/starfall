package com.starfall.scene;

import com.starfall.game.ActionResult;
import com.starfall.game.Arena;
import com.starfall.game.Direction;
import com.starfall.game.Tile;

import java.util.List;
import java.util.function.Function;

/**
 * La partie que joue le mode capture, une action par image.
 *
 * <h2>Pourquoi elle a quitté la scène</h2>
 *
 * <p>Les entrées sont coupées en capture : sans scénario, les images d'une série seraient toutes
 * identiques. Rejouer une courte partie les rend différentes <em>et</em> utiles — les planches
 * montrent le jeu à l'œuvre au lieu de trois copies de la position de départ.
 *
 * <p>Elle vivait dans {@code ArenaScene}, donc derrière un contexte graphique, donc hors de portée
 * de tout test. Et elle a régressé exactement là : au jalon d'équilibrage, le scénario écrit pour
 * l'ancienne économie s'est mis à mourir en vague 1 au tour 7, sans qu'une seule tuile ne se pose à
 * côté d'une autre. Douze images livrées pour un jalon intitulé « la file a enfin une économie », où
 * l'on ne voyait jamais de salve. <b>Aucune assertion n'a bronché</b>, parce qu'il n'y en avait
 * aucune à broncher.
 *
 * <p>Le scénario ne touche qu'à l'{@link Arena} : rien n'obligeait à le garder derrière GL. Il est
 * donc ici, et {@code CaptureScriptTest} vérifie qu'il montre encore ce qu'il prétend montrer.
 */
public final class CaptureScript {

    private CaptureScript() {
    }

    /** Temps de la salve finale : deux tuiles chargées, donc deux temps. */
    public static final int WINNING_BEATS = 2;

    /**
     * Les gestes joués, dans l'ordre.
     *
     * <p>Ce qu'ils doivent montrer, et ce que le test vérifie : une file <b>chargée sous le feu</b>
     * — chaque pose coûte un tour et les ennemis se rapprochent pendant ce temps — puis une
     * <b>salve</b> qui fait tomber plusieurs effets d'un coup, puis une <b>vague qui bascule</b>.
     * C'est toute l'économie du jeu, puis la tranche entière jusqu'à la victoire. Le compte de
     * gestes n'est pas écrit ici : il a été faux deux fois — « onze » alors qu'il y en avait 27,
     * puis 87 — et {@code ACTIONS.size()} le dit sans jamais se tromper.
     */
    public static final List<Function<Arena, ActionResult>> ACTIONS = List.of(
            a -> a.step(Direction.RIGHT),
            a -> a.swapWithTarget(),
            a -> a.queueTile(Tile.STRIKE),
            a -> a.queueTile(Tile.PUSH),
            a -> a.queueTile(Tile.PIVOT),
            a -> a.unleash(),
            a -> a.queueTile(Tile.THRUST),
            a -> a.queueTile(Tile.SIDESTEP),
            a -> a.unleash(),
            a -> a.queueTile(Tile.PUSH),
            a -> a.queueTile(Tile.DASH),
            a -> a.unleash(),
            a -> a.queueTile(Tile.THRUST),
            a -> a.unleash(),
            a -> a.swapWithTarget(),
            a -> a.swapWithTarget(),
            a -> a.swapWithTarget(),
            a -> a.queueTile(Tile.PUSH),
            a -> a.queueTile(Tile.PIVOT),
            a -> a.unleash(),
            a -> a.queueTile(Tile.THRUST),
            a -> a.unleash(),
            a -> a.queueTile(Tile.PUSH),
            a -> a.unleash(),
            a -> a.queueTile(Tile.STRIKE),
            a -> a.unleash(),
            a -> a.queueTile(Tile.PUSH),
            a -> a.queueTile(Tile.THRUST),
            a -> a.unleash(),
            a -> a.queueTile(Tile.STRIKE),
            a -> a.swapWithTarget(),
            a -> a.unleash(),
            a -> a.step(Direction.RIGHT),
            a -> a.queueTile(Tile.THRUST),
            a -> a.unleash());

    /** Emplacement de râtelier survolé à chaque image, ou -1. Une entrée par image. */
    public static final int[] HOVERED_RACK_SLOT = {
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
    };

    /** Case de plateau survolée, ou -1. Chacune porte un ennemi : un survol qui ne survole rien ne se voit ni sur la planche, ni dans ce tableau. */
    public static final int[] HOVERED_CELL = {
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
    };

    /** Emplacement de file survolé, ou -1. Chacun est occupé à son image. */
    public static final int[] HOVERED_QUEUE_SLOT = {
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
    };

    /** Nom de scène qui sélectionne cette ligne, côté ligne de commande. */
    public static final String SCENE_NAME = "arena";

    /** La vue « scénario » de la ligne gagnante. Voir {@link CaptureScenario}. */
    public static final CaptureScenario SCENARIO = new CaptureScenario() {
        @Override
        public String sceneName() {
            return SCENE_NAME;
        }

        @Override
        public java.util.List<java.util.function.Function<Arena, ActionResult>> actions() {
            return ACTIONS;
        }

        /**
         * Deux temps de plus, après le dernier geste : ceux de la salve qui <b>gagne</b>.
         *
         * <p>La bannière de fin appartient à l'état au repos — sans cette garde, « VICTOIRE » se
         * peignait dès la première image du déroulé, donc AVANT le coup qui l'a gagnée. Cette
         * règle-là n'avait aucun témoin : l'écran de la salve ordinaire ne gagne rien. Ces deux
         * images le donnent, et c'est la seule ligne du projet qui gagne.
         */
        @Override
        public int lastFrame() {
            return ACTIONS.size() + WINNING_BEATS;
        }

        @Override
        public int playbackBeatAt(int frameIndex) {
            return frameIndex <= ACTIONS.size() ? 0 : frameIndex - ACTIONS.size();
        }

        @Override
        public int rackSlotAt(int frameIndex) {
            return CaptureScript.rackSlotAt(frameIndex);
        }

        @Override
        public int queueSlotAt(int frameIndex) {
            return CaptureScript.queueSlotAt(frameIndex);
        }

        @Override
        public int cellAt(int frameIndex) {
            return CaptureScript.cellAt(frameIndex);
        }
    };

    /** Tuile survolée à cette image, ou {@code -1}. */
    public static int rackSlotAt(int frameIndex) {
        return frameIndex >= 0 && frameIndex < HOVERED_RACK_SLOT.length
                ? HOVERED_RACK_SLOT[frameIndex] : -1;
    }

    /** Emplacement de la file survolé à cette image, ou {@code -1}. */
    public static int queueSlotAt(int frameIndex) {
        return frameIndex >= 0 && frameIndex < HOVERED_QUEUE_SLOT.length
                ? HOVERED_QUEUE_SLOT[frameIndex] : -1;
    }

    /** Case survolée à cette image, ou {@code -1}. */
    public static int cellAt(int frameIndex) {
        return frameIndex >= 0 && frameIndex < HOVERED_CELL.length ? HOVERED_CELL[frameIndex] : -1;
    }
}
