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
     * C'est toute l'économie du jeu, puis la tranche entière jusqu'à la victoire. Le compte de
     * gestes n'est pas écrit ici : il a été faux deux fois — « onze » alors qu'il y en avait 27,
     * puis 87 — et {@code ACTIONS.size()} le dit sans jamais se tromper.
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
            a -> a.unleash(),
            // ------------------------------------------------------------------ la suite profonde
            // Les onze gestes ci-dessus s'arrêtent une action trop tôt, et une review l'a prouvé en
            // rendant la CHARGE visuellement indiscernable d'une frappe : les 463 tests et les 48
            // planches sont restés verts. L'image 11 montre le lancier qui « prend son élan » —
            // c'est-à-dire la promesse — et le script se terminait là, sans jamais montrer la tenue
            // de promesse. Aucune valeur de {@code --frames} ne pouvait le rattraper : il n'y avait
            // plus de geste à jouer.
            //
            // Ces quinze gestes ont été trouvés par une recherche, pas écrits à la main — la même
            // discipline que l'ouverture, et pour la même raison. Ils font paraître le glyphe de
            // charge, portent le compte de coups annoncés à QUATRE — au-delà du plafond de trois
            // que la bande de menace a dû apprendre à ne plus dépasser en silence — et arrivent à
            // la vague 3 avec cinq points de vie sur huit.
            //
            // Ils sont AJOUTÉS et non intercalés : les douze premières images ne bougent pas d'un
            // octet, donc les planches-contact datées des jalons restent ce qu'elles étaient.
            a -> a.step(Direction.RIGHT),
            a -> a.swapWithTarget(),
            a -> a.step(Direction.RIGHT),
            a -> a.queueTile(Tile.STRIKE),
            a -> a.step(Direction.RIGHT),
            a -> a.queueTile(Tile.PUSH),
            a -> a.queueTile(Tile.PIVOT),
            a -> a.queueTile(Tile.SIDESTEP),
            a -> a.unleash(),
            a -> a.queueTile(Tile.DASH),
            a -> a.swapWithTarget(),
            a -> a.swapWithTarget(),
            a -> a.queueTile(Tile.PIVOT),
            a -> a.unleash(),
            a -> a.swapWithTarget(),
            // ------------------------------------------------------------------------ la fin
            // Le scénario va désormais jusqu'à la VICTOIRE, et c'est encore un trou de review qui
            // l'a imposé : ni la bannière de victoire ni celle de défaite ne paraissaient sur une
            // seule planche. {@code drawOutcome} traversait toute la chaîne graphique sans témoin.
            //
            // Ces gestes viennent d'une recherche, comme les précédents, et le barème a été orienté
            // vers la SANTÉ plutôt que la vitesse : une première ligne gagnait en 41 gestes de moins
            // mais finissait à un point de vie sur huit. Une planche qui finit ainsi ne montre pas
            // une victoire, elle montre un miracle -- et le test de ce fichier refuse déjà une
            // ouverture qui se termine à un point de vie.
            //
            // Toujours ajoutés, jamais intercalés : les vingt-sept premières images ne bougent pas.
            a -> a.swapWithTarget(),
            a -> a.step(Direction.RIGHT),
            a -> a.queueTile(Tile.PUSH),
            a -> a.queueTile(Tile.PIVOT),
            a -> a.queueTile(Tile.SIDESTEP),
            a -> a.unleash(),
            a -> a.swapWithTarget(),
            a -> a.step(Direction.LEFT),
            a -> a.queueTile(Tile.PUSH),
            a -> a.queueTile(Tile.PIVOT),
            a -> a.swapWithTarget(),
            a -> a.unleash(),
            a -> a.queueTile(Tile.THRUST),
            a -> a.queueTile(Tile.SIDESTEP),
            a -> a.queueTile(Tile.STRIKE),
            a -> a.unleash(),
            a -> a.queueTile(Tile.PUSH),
            a -> a.queueTile(Tile.PIVOT),
            a -> a.swapWithTarget(),
            a -> a.swapWithTarget(),
            a -> a.swapWithTarget(),
            a -> a.swapWithTarget(),
            a -> a.unleash(),
            a -> a.swapWithTarget(),
            a -> a.swapWithTarget(),
            a -> a.step(Direction.LEFT),
            a -> a.queueTile(Tile.THRUST),
            a -> a.queueTile(Tile.PIVOT),
            a -> a.queueTile(Tile.SIDESTEP),
            a -> a.step(Direction.LEFT),
            a -> a.unleash(),
            a -> a.swapWithTarget(),
            a -> a.queueTile(Tile.PIVOT),
            a -> a.unleash(),
            a -> a.swapWithTarget(),
            a -> a.step(Direction.LEFT),
            a -> a.queueTile(Tile.THRUST),
            a -> a.queueTile(Tile.PIVOT),
            a -> a.queueTile(Tile.SIDESTEP),
            a -> a.step(Direction.LEFT),
            a -> a.unleash(),
            a -> a.swapWithTarget(),
            a -> a.queueTile(Tile.PIVOT),
            a -> a.unleash(),
            a -> a.swapWithTarget(),
            a -> a.step(Direction.LEFT),
            a -> a.queueTile(Tile.THRUST),
            a -> a.queueTile(Tile.PIVOT),
            a -> a.queueTile(Tile.SIDESTEP),
            a -> a.step(Direction.LEFT),
            a -> a.unleash(),
            a -> a.swapWithTarget(),
            a -> a.queueTile(Tile.PIVOT),
            a -> a.unleash(),
            a -> a.swapWithTarget(),
            a -> a.step(Direction.LEFT),
            a -> a.queueTile(Tile.PUSH),
            a -> a.queueTile(Tile.PIVOT),
            a -> a.queueTile(Tile.SIDESTEP),
            a -> a.step(Direction.LEFT),
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
            -1,  // image 5 : c'est une case du plateau qui est survolée, pas le râtelier
            4,   // le pas de côté, tuile Free-Play
            -1,
            -1,
            -1,  // le préavis résolu d'une salve
            5,   // la volte-face, qui ne vise aucune case
            -1,  // et la dernière image, que le tableau ne couvrait pas
            // La suite profonde. L'emplacement 0 — la frappe — n'était survolé sur AUCUNE image :
            // l'infobulle et la portée de la tuile la plus élémentaire du jeu ne paraissaient nulle
            // part. C'est réparé ici.
            0,   // la frappe, enfin montrée
            -1, -1,
            0,   // et une seconde fois, sur un terrain différent
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            // La fin de partie : rien n'est survolé. Ce que ces images viennent montrer, ce sont
            // les dernières vagues et la bannière de victoire, qui ne demandent aucun survol — et
            // un survol de râtelier sur la dernière image recouvrirait la bannière d'une infobulle.
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1,
    };

    /**
     * Case du plateau survolée à chaque image, ou {@code -1}.
     *
     * <p>Elle sert à montrer l'infobulle d'ennemi, qui existe pour la raison exacte qui a fait
     * écrire celles des tuiles : la portée de l'archer et celle du trait agressif n'étaient écrites
     * nulle part, et on les apprenait en prenant des coups.
     */
    public static final int[] HOVERED_CELL = {
            // L'image 5 visait la case 2, qui est VIDE à ce moment-là : aucune planche livrée ne
            // montrait donc d'infobulle d'ennemi, alors que c'est la seule raison d'être de ce
            // tableau. Le test qui prétendait le garder ne vérifiait que des bornes.
            -1, -1, -1, -1, -1, 5, -1, -1, -1, -1, -1, -1,
            // La suite profonde : les survols de plateau y sont laissés à -1. Le tableau est vérifié
            // image par image — une case survolée doit porter un ennemi à cet instant précis — et
            // rien n'oblige à en désigner une : ce que la suite profonde vient montrer, c'est le
            // glyphe de charge et la bande de menace à quatre coups, qui ne demandent aucun survol.
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            // La fin de partie : idem, aucun survol de plateau.
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1,
    };

    /**
     * Emplacement de la <b>file</b> survolé à chaque image, ou {@code -1}.
     *
     * <p>Il manquait, et une review l'a relevé : {@code hoveredQueueSlot} était forcé à {@code -1}
     * pendant les captures, si bien que toute la branche « tuile survolée dans la file » —
     * l'emplacement mis en avant, l'infobulle, le repère de portée, et le régime creux d'une tuile
     * qui n'est pas au sommet — ne paraissait sur <em>aucune</em> planche.
     *
     * <p>Deux images seulement, et elles sont choisies :
     * <ul>
     *   <li><b>19</b> : la file compte quatre tuiles et on survole la <em>plus ancienne</em>, donc
     *       pas celle qui partira la première. C'est le cas que l'interface doit rendre lisible,
     *       puisque la file s'exécute à l'envers de sa lecture ;</li>
     *   <li><b>16</b> : la file n'en compte qu'une, donc la survolée <em>est</em> le sommet — et
     *       cette tuile est une <b>frappe</b>, ce qui n'est pas un détail.</li>
     * </ul>
     *
     * <p><b>Une première version choisissait l'image 21, et elle ne gardait rien.</b> La file y
     * contenait un <b>élan</b>, dont la portée statique est <em>vide</em>. Une review l'a relevé —
     * et en cherchant à la remplacer par une image dont la tuile a une portée, on a trouvé pire :
     * le retour anticipé qu'elle prétendait garder <b>ne pouvait rien changer à aucune image</b>,
     * la couleur étant forcée à celle du préavis dès qu'un emplacement de file est survolé. Il a
     * été retiré ; voir {@code ArenaScene.drawReach}.
     *
     * <p>Ce qui reste gardé ici est réel et vérifié par mutation : l'emplacement mis en avant, et
     * la tuile survolée traitée comme disponible. Les deux ne sont vues que par l'image 19.
     */
    public static final int[] HOVERED_QUEUE_SLOT = {
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, -1, -1, 0,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1,
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
