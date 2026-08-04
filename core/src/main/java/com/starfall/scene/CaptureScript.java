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

    /**
     * Les gestes joués, dans l'ordre.
     *
     * <p>Ce qu'ils doivent montrer, et ce que le test vérifie : une file <b>chargée sous le feu</b>
     * — chaque pose coûte un tour et les ennemis se rapprochent pendant ce temps — puis une
     * <b>salve</b> qui fait tomber plusieurs effets d'un coup, puis une <b>vague qui bascule</b>.
     * C'est toute l'économie du jeu en onze gestes.
     */
    public static final List<Function<Arena, ActionResult>> ACTIONS = List.of(
            // Une ouverture qui tient : elle franchit la première vague à pleine santé en quatre
            // tours. Elle a été trouvée par la recherche du garde-fou de jouabilité, pas écrite à
            // la main — la version manuelle mourait au tour 9 sans avoir fait basculer une vague,
            // et c'est le test de ce fichier qui l'a dit.
            a -> a.queueTile(Tile.SIDESTEP),  // Free-Play : se charge sans rien coûter
            a -> a.unleash(),                 // salve entièrement Free-Play : gratuite aussi
            a -> a.queueTile(Tile.THRUST),    // le premier tour dépensé, et le premier encaissé
            a -> a.queueTile(Tile.PIVOT),
            a -> a.unleash(),                 // deux effets, un seul tour
            a -> a.queueTile(Tile.STRIKE),
            a -> a.queueTile(Tile.PIVOT),
            a -> a.unleash(),                 // la vague bascule quand le terrain se vide
            // Puis on charge sous le feu de la vague suivante, et on lâche : c'est l'image que le
            // jalon d'équilibrage devait montrer et ne montrait pas. Deux tuiles et pas trois —
            // la troisième coûtait quatre points de vie de plus, et le test de ce fichier a refusé
            // une planche qui finissait à un point de vie sur huit.
            a -> a.queueTile(Tile.THRUST),
            a -> a.queueTile(Tile.PUSH),
            a -> a.unleash());

    /**
     * Tuile du râtelier survolée à chaque image, ou {@code -1}.
     *
     * <p>L'ordre du râtelier est celui de {@code ArenaSetup} : frappe, estoc, poussée, élan, pas de
     * côté, volte-face. Le survol est scénarisé parce que sans lui, ni infobulle ni repère de portée
     * n'apparaîtrait sur <em>aucune</em> planche — et une planche qui ne montre pas ce qu'elle
     * légende vaut moins que pas de planche.
     */
    public static final int[] HOVERED_RACK_SLOT = {
            -1,  // image 0 : l'aide, telle qu'un joueur la découvre
            1,   // l'estoc et sa portée 2
            -1,  // rien de survolé : le préavis résolu du sommet
            2,   // la poussée
            3,   // l'élan, dont la portée dépend du terrain
            -1,
            4,   // le pas de côté, tuile Free-Play
            -1,
            -1,
            -1,  // le préavis résolu d'une salve
            5,   // la volte-face, qui ne vise aucune case
    };

    /**
     * Case du plateau survolée à chaque image, ou {@code -1}.
     *
     * <p>Elle sert à montrer l'infobulle d'ennemi, qui existe pour la raison exacte qui a fait
     * écrire celles des tuiles : la portée de l'archer et celle du trait agressif n'étaient écrites
     * nulle part, et on les apprenait en prenant des coups.
     */
    public static final int[] HOVERED_CELL = {
            -1, -1, -1, -1, -1, 2, -1, -1, -1, -1, -1, -1,
    };

    /** Rejoue les {@code count} premiers gestes sur une arène. */
    public static ActionResult replayInto(Arena arena, int count) {
        ActionResult last = null;
        for (int i = 0; i < count && i < ACTIONS.size(); i++) {
            last = ACTIONS.get(i).apply(arena);
        }
        return last;
    }

    /** Tuile survolée à cette image, ou {@code -1}. */
    public static int rackSlotAt(int frameIndex) {
        return frameIndex >= 0 && frameIndex < HOVERED_RACK_SLOT.length
                ? HOVERED_RACK_SLOT[frameIndex] : -1;
    }

    /** Case survolée à cette image, ou {@code -1}. */
    public static int cellAt(int frameIndex) {
        return frameIndex >= 0 && frameIndex < HOVERED_CELL.length ? HOVERED_CELL[frameIndex] : -1;
    }
}
