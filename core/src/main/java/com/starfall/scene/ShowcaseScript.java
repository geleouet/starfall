package com.starfall.scene;

import com.starfall.game.ActionResult;
import com.starfall.game.Arena;
import com.starfall.game.Direction;
import com.starfall.game.Tile;

import java.util.List;
import java.util.function.Function;

/**
 * La <b>vitrine</b> : une partie perdue, jouée pour montrer ce qu'une partie gagnée ne montre pas.
 *
 * <h2>Ce qu'elle exhibe, et pourquoi elle existe</h2>
 *
 * <p>Des reviews ont mesuré cinq états d'interface qu'aucune des planches de référence ne traversait,
 * chacun prouvé absent par mutation — on pouvait les casser sans qu'un seul test ni une seule
 * planche ne bronche :
 *
 * <ul>
 *   <li>la <b>bannière de défaite</b>. La ligne principale gagne, donc elle ne la voit jamais ;</li>
 *   <li>le <b>mur d'une poussée qui bute</b> — l'issue {@code COLLIDED}, que le javadoc du préavis
 *       appelle « le cas le plus rentable du jeu et le moins calculable d'un coup d'œil » ;</li>
 *   <li>le filtre qui empêche de <b>surligner une case qu'un clic ne peut pas atteindre</b> : les
 *       seuls survols de plateau de la ligne principale visent des cases cliquables, si bien que
 *       retirer le filtre ne changeait rien. C'est l'objection de M4, revenue sans témoin ;</li>
 *   <li>la <b>reprise</b> d'une tuile, seul geste gratuit du jeu ;</li>
 *   <li>et la <b>partie finie avec une tuile encore sur la file</b>, survolée. C'est l'état qui
 *       m'a fait prendre une vraie régression pour du code mort : {@code previewTop} rend
 *       {@code null} quand la partie est finie, donc le retour anticipé de
 *       {@code hoveringTheTop} est le seul à empêcher une portée de réapparaître sur fond nu
 *       après la mort. Aucune planche ne l'atteignait ; je l'ai retiré en croyant démontrer qu'il
 *       ne servait à rien, et la review a mesuré 544 pixels de différence.</li>
 * </ul>
 *
 * <p>Rien de tout cela ne pouvait être ajouté à la ligne gagnante : elle se termine par une
 * victoire, et rien ne se joue après. Allonger par le milieu aurait décalé les soixante-et-onze
 * planches suivantes, c'est-à-dire rendu leur relecture impraticable.
 *
 * <h2>La ligne</h2>
 *
 * <p>Les gestes viennent d'une recherche, comme ceux de la ligne gagnante : les premiers amènent la
 * poussée qui bute puis la reprennent, les suivants mènent à la défaite la plus courte depuis cet
 * état, et le dernier est refusé — il rejoue l'état terminal pour le survoler autrement. Le héros
 * meurt, et c'est le but. Le compte de gestes n'est pas écrit ici : il a déjà été faux une fois, et
 * {@code ACTIONS.size()} le dit sans se tromper.
 */
public final class ShowcaseScript {

    /** Nom de scène qui la sélectionne, côté ligne de commande. */
    public static final String SCENE_NAME = "showcase";

    /**
     * L'image où la poussée bute, où un ennemi <b>non cliquable</b> est survolé, et où la file
     * contient la tuile qui sera reprise à l'image suivante. Trois des cinq états d'un coup.
     */
    public static final int COLLISION_FRAME = 4;

    /** L'image qui suit la reprise : le compte rendu dit « reprise ». */
    public static final int UNQUEUE_FRAME = 5;

    /**
     * Les <b>trois</b> images terminales, et pourquoi il en faut trois.
     *
     * <p>« Après la mort, rien ne se promet » n'est pas une règle mais trois, et aucune image ne
     * peut les montrer ensemble :
     *
     * <ul>
     *   <li>{@link #DEATH_FRAME} — un emplacement de file survolé qui <b>n'est pas</b> le sommet.
     *       C'est la moitié du correctif qui n'avait aucun témoin : la vitrine mourait avec une
     *       seule tuile, donc l'emplacement 0 était toujours le sommet et ce chemin n'était jamais
     *       atteint. Elle meurt maintenant avec deux ;</li>
     *   <li>{@link #TOP_FRAME} — le <b>sommet</b> survolé, qui passe par un autre chemin du code :
     *       {@code hoveringTheTop} sort avant même de regarder la disponibilité ;</li>
     *   <li>{@link #RACK_FRAME} — une tuile du <b>râtelier</b> survolée, ce que {@code
     *       hoveringTheTop} exclut par construction.</li>
     * </ul>
     *
     * <p>Les deux derniers gestes sont <b>refusés</b> par l'arène, la partie étant finie : c'est le
     * seul moyen de rendre trois fois le même état terminal avec trois survols différents. Un geste
     * sans effet dans un scénario de capture est normalement un défaut ; ici c'est l'instrument, et
     * un test exige qu'ils soient bien refusés.
     */
    public static final int DEATH_FRAME = 11;

    /** L'état terminal, sommet de la file survolé. */
    public static final int TOP_FRAME = 12;

    /** L'état terminal, râtelier survolé. */
    public static final int RACK_FRAME = 13;

    public static final List<Function<Arena, ActionResult>> ACTIONS = List.of(
            a -> a.step(Direction.RIGHT),
            a -> a.swapWithTarget(),
            a -> a.step(Direction.LEFT),
            // Le sabreur est maintenant dos au mur : la poussée annoncée BUTE, et le préavis doit
            // le dire par une forme et non par un mot.
            a -> a.queueTile(Tile.PUSH),
            a -> a.unqueueAt(0),
            // On recharge DEUX tuiles avant de mourir : la file doit être garnie à plus d'un à la
            // dernière image, sans quoi l'emplacement survolé serait toujours le sommet et la
            // moitié « file » de la règle resterait sans témoin.
            a -> a.queueTile(Tile.STRIKE),
            a -> a.queueTile(Tile.THRUST),
            // Puis on meurt, ce qui est le seul moyen de voir la bannière de défaite.
            a -> a.step(Direction.RIGHT),
            a -> a.step(Direction.LEFT),
            a -> a.step(Direction.RIGHT),
            a -> a.step(Direction.LEFT),
            // Refusés : la partie est finie. Voir TOP_FRAME et RACK_FRAME.
            a -> a.step(Direction.RIGHT),
            a -> a.step(Direction.LEFT));

    /**
     * Un seul survol de râtelier, sur la dernière image terminale : c'est là que se voit la règle
     * « une tuile qu'aucune touche ne peut poser ne se dessine pas dans la couleur du préavis ».
     * Ailleurs il recouvrirait d'une infobulle les états que cette vitrine existe pour montrer.
     */
    public static final int[] HOVERED_RACK_SLOT = new int[ACTIONS.size() + 1];

    /**
     * Deux survols de file, sur deux images terminales : l'un sur un emplacement qui <b>n'est
     * pas</b> le sommet, l'autre sur le sommet. Les deux passent par des chemins de code
     * différents, et c'est pour cela qu'il en faut deux.
     */
    public static final int[] HOVERED_QUEUE_SLOT = new int[ACTIONS.size() + 1];

    /**
     * Le survol de plateau, et c'est ici qu'il porte : la case survolée à l'image {@link
     * #COLLISION_FRAME} porte un ennemi <b>qu'un clic ne peut pas atteindre</b>. Sans elle, retirer
     * le filtre {@code clickable} du surlignage ne changeait aucune planche.
     */
    public static final int[] HOVERED_CELL = new int[ACTIONS.size() + 1];

    static {
        java.util.Arrays.fill(HOVERED_RACK_SLOT, -1);
        java.util.Arrays.fill(HOVERED_QUEUE_SLOT, -1);
        java.util.Arrays.fill(HOVERED_CELL, -1);
        HOVERED_CELL[COLLISION_FRAME] = 4;
        HOVERED_QUEUE_SLOT[DEATH_FRAME] = 0;   // la plus ancienne : PAS le sommet
        HOVERED_QUEUE_SLOT[TOP_FRAME] = 1;     // le sommet
        // La poussee : rechargee a l'etat terminal ET de portee non vide. Les deux comptent --
        // une tuile depensee serait deja dessinee en creux, et une portee vide ne dessinerait rien.
        HOVERED_RACK_SLOT[RACK_FRAME] = 2;
    }

    /** La vue « scénario » de cette vitrine. */
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
        public int rackSlotAt(int frameIndex) {
            return at(HOVERED_RACK_SLOT, frameIndex);
        }

        @Override
        public int queueSlotAt(int frameIndex) {
            return at(HOVERED_QUEUE_SLOT, frameIndex);
        }

        @Override
        public int cellAt(int frameIndex) {
            return at(HOVERED_CELL, frameIndex);
        }
    };

    private static int at(int[] table, int frameIndex) {
        return frameIndex >= 0 && frameIndex < table.length ? table[frameIndex] : -1;
    }

    private ShowcaseScript() {
    }
}
