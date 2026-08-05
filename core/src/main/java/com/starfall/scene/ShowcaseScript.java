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
 * <p>Une review a mesuré quatre états d'interface qu'aucune des planches de référence ne traversait,
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
 * <p>Onze gestes, trouvés par recherche comme les autres. Les cinq premiers amènent la poussée qui
 * bute puis la reprennent ; les six derniers sont la défaite la plus courte depuis cet état. Le
 * héros meurt, et c'est le but.
 */
public final class ShowcaseScript {

    /** Nom de scène qui la sélectionne, côté ligne de commande. */
    public static final String SCENE_NAME = "showcase";

    /**
     * L'image où la poussée bute, où un ennemi <b>non cliquable</b> est survolé, et où la file
     * contient la tuile qui sera reprise à l'image suivante. Trois des quatre états d'un coup.
     */
    public static final int COLLISION_FRAME = 4;

    /** L'image qui suit la reprise : le compte rendu dit « reprise ». */
    public static final int UNQUEUE_FRAME = 5;

    /**
     * L'image de la mort, file <b>non vide</b> et sommet survolé.
     *
     * <p>La première version de cette vitrine mourait la file vide : elle montrait la bannière de
     * défaite, mais pas l'état où une tuile reste en attente d'une partie qui ne reprendra pas.
     */
    public static final int DEATH_FRAME = 10;

    public static final List<Function<Arena, ActionResult>> ACTIONS = List.of(
            a -> a.step(Direction.RIGHT),
            a -> a.swapWithTarget(),
            a -> a.step(Direction.LEFT),
            // Le sabreur est maintenant dos au mur : la poussée annoncée BUTE, et le préavis doit
            // le dire par une forme et non par un mot.
            a -> a.queueTile(Tile.PUSH),
            a -> a.unqueueAt(0),
            // On recharge avant de mourir : la file doit être GARNIE à la dernière image, sans quoi
            // l'état « partie finie, tuile en attente » reste hors de portée du garde-fou.
            a -> a.queueTile(Tile.STRIKE),
            // Puis on meurt, ce qui est le seul moyen de voir la bannière de défaite.
            a -> a.swapWithTarget(),
            a -> a.step(Direction.RIGHT),
            a -> a.step(Direction.LEFT),
            a -> a.step(Direction.RIGHT));

    /**
     * Aucun survol de râtelier : la ligne principale en montre déjà six, et en ajouter ici
     * recouvrirait d'une infobulle les états que cette vitrine existe pour montrer.
     */
    public static final int[] HOVERED_RACK_SLOT = new int[ACTIONS.size() + 1];

    /**
     * Un seul survol de file, et il porte sur la <b>dernière</b> image : la partie est finie et une
     * frappe attend encore. C'est là que le retour anticipé de {@code hoveringTheTop} se voit, et
     * nulle part ailleurs.
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
        HOVERED_QUEUE_SLOT[DEATH_FRAME] = 0;
    }

    /** La vue « scénario » de cette vitrine. */
    public static final CaptureScenario SCENARIO = new CaptureScenario() {
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
