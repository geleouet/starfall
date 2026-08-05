package com.starfall.scene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.starfall.game.ActionResult;
import com.starfall.game.Arena;
import com.starfall.game.ArenaSetup;
import com.starfall.game.Enemy;
import com.starfall.game.TilePreview;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Ce que la vitrine promet de montrer, elle doit le montrer.
 *
 * <h2>Pourquoi ce fichier existe</h2>
 *
 * <p>{@link ShowcaseScript} n'a aucune valeur en soi : sa seule raison d'être est d'amener quatre
 * états d'interface sur une planche de référence. Si sa ligne dérive — un geste de plus, un
 * équilibrage qui change — elle peut cesser de les atteindre <b>sans que rien ne le dise</b> : les
 * planches changeraient, on les adopterait, et la couverture serait perdue en silence.
 *
 * <p>C'est le mode de défaillance le plus courant de ce projet, et il a un nom : une garde qui garde
 * autre chose que ce qu'elle annonce. Ces assertions épinglent ce que la vitrine doit rencontrer.
 */
class ShowcaseScriptTest {

    private static Arena after(int frames) {
        Arena arena = ArenaSetup.trainingArena(9, 1);
        ShowcaseScript.SCENARIO.replayInto(arena, frames);
        return arena;
    }

    /**
     * La ligne perd, et c'est tout l'objet : la bannière de défaite n'est atteignable autrement.
     */
    @Test
    @DisplayName("La vitrine se termine par une défaite")
    void theShowcaseEndsInDefeat() {
        Arena finished = after(ShowcaseScript.ACTIONS.size());

        assertTrue(finished.isDefeat(), "la vitrine doit perdre : etat final vie "
                + finished.hero().health() + ", partie finie " + finished.isOver());
        assertTrue(finished.isOver(), "une defaite doit terminer la partie");
    }

    /**
     * La poussée qui <b>bute</b>, c'est-à-dire l'issue que le jalon décrit comme « la plus rentable
     * du jeu et la moins calculable d'un coup d'œil ». Aucune planche ne la montrait.
     */
    @Test
    @DisplayName("La vitrine montre une poussée qui bute")
    void theShowcaseShowsACollidingPush() {
        TilePreview top = after(ShowcaseScript.COLLISION_FRAME).previewTop();

        assertTrue(top != null, "aucun preavis de sommet a l'image "
                + ShowcaseScript.COLLISION_FRAME);
        assertEquals(ActionResult.COLLIDED, top.outcome(),
                "l'image " + ShowcaseScript.COLLISION_FRAME + " devait montrer une poussee qui bute");
    }

    /**
     * Une case survolée <b>qu'un clic ne peut pas atteindre</b>.
     *
     * <p>C'est l'objection de M4 : le surlignage s'allumait sur des cases inatteignables, donc le
     * pointeur promettait une action qui ne venait pas. Le filtre existe depuis, mais les seuls
     * survols de la ligne gagnante visent des cases cliquables — le retirer ne changeait aucune
     * planche. Il fallait survoler une case <em>refusée</em> pour que la différence se voie.
     */
    @Test
    @DisplayName("La vitrine survole une case qu'un clic ne peut pas atteindre")
    void theShowcaseHoversACellNoClickCanReach() {
        int frame = ShowcaseScript.COLLISION_FRAME;
        int cell = ShowcaseScript.SCENARIO.cellAt(frame);
        assertTrue(cell >= 0, "l'image " + frame + " ne survole aucune case");

        Arena arena = after(frame);
        assertTrue(arena.grid().occupantAt(cell) instanceof Enemy,
                "la case survolee doit porter un ennemi, sinon l'infobulle ne montre rien");
        assertTrue(!arena.clickable(cell),
                "la case survolee est cliquable : le filtre du surlignage reste sans temoin");
    }

    /** La reprise d'une tuile, seul geste gratuit du jeu, et le seul état d'action jamais rendu. */
    @Test
    @DisplayName("La vitrine reprend une tuile")
    void theShowcaseTakesATileBack() {
        Arena arena = ArenaSetup.trainingArena(9, 1);
        ActionResult last = ShowcaseScript.SCENARIO.replayInto(arena, ShowcaseScript.UNQUEUE_FRAME);

        assertEquals(ActionResult.UNQUEUED, last,
                "l'image " + ShowcaseScript.UNQUEUE_FRAME + " devait suivre une reprise");
    }

    /**
     * Les tableaux de survol couvrent toutes les images, comme ceux de la ligne gagnante.
     *
     * <p>La borne manquait sur l'un des trois tableaux de l'autre scénario, et le tronquer faisait
     * disparaître ses survols en silence. La même garde vaut ici.
     */
    @Test
    @DisplayName("Les tableaux de survol de la vitrine couvrent toutes ses images")
    void theShowcaseHoverTablesCoverEveryFrame() {
        int frames = ShowcaseScript.ACTIONS.size() + 1;

        assertTrue(ShowcaseScript.HOVERED_RACK_SLOT.length >= frames,
                "survols de ratelier : " + ShowcaseScript.HOVERED_RACK_SLOT.length + " pour "
                        + frames + " images");
        assertTrue(ShowcaseScript.HOVERED_QUEUE_SLOT.length >= frames,
                "survols de file : " + ShowcaseScript.HOVERED_QUEUE_SLOT.length + " pour "
                        + frames + " images");
        assertTrue(ShowcaseScript.HOVERED_CELL.length >= frames,
                "survols de plateau : " + ShowcaseScript.HOVERED_CELL.length + " pour "
                        + frames + " images");
    }

    /** Et la résolution par nom de scène rend bien ce scénario-ci, pas la ligne gagnante. */
    @Test
    @DisplayName("Le nom de scène de la vitrine désigne son scénario")
    void theSceneNameResolvesToTheShowcase() {
        assertEquals(ShowcaseScript.ACTIONS.size(),
                CaptureScenario.forScene(ShowcaseScript.SCENE_NAME).size());
        assertEquals(CaptureScript.ACTIONS.size(), CaptureScenario.forScene("arena").size());
    }
}
