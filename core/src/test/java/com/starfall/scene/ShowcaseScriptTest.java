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
        // Pas de « et la partie est finie » : isOver() rend « victoire || defaite », donc
        // l'assertion serait impliquee par la precedente. Ce qui compte, et qui peut echouer, c'est
        // que la file soit encore GARNIE et que son sommet soit survole -- le seul etat ou le
        // retour anticipe de hoveringTheTop se voit, et celui qui m'a fait prendre une regression
        // de rendu pour du code mort.
        assertTrue(!finished.queue().isEmpty(),
                "la file doit rester garnie a la mort, elle contient " + finished.queue().size());
        assertEquals(0, ShowcaseScript.SCENARIO.queueSlotAt(ShowcaseScript.DEATH_FRAME),
                "le sommet de la file doit etre survole a l'image de la mort");
        assertEquals(ShowcaseScript.DEATH_FRAME, ShowcaseScript.ACTIONS.size(),
                "l'image de la mort doit etre la derniere du scenario");
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

    /*
     * ------------------------------------------------------------------------------------------
     * Ici vivait « Les tableaux de survol de la vitrine couvrent toutes ses images », copié sur la
     * garde équivalente de la ligne gagnante. IL ÉTAIT VRAI PAR CONSTRUCTION : les trois tableaux
     * sont déclarés « new int[ACTIONS.size() + 1] », donc leur longueur EST celle qu'on assertait,
     * calculée depuis la même expression, dans le même fichier.
     *
     * La garde d'origine, elle, porte : chez CaptureScript les tableaux sont des littéraux écrits à
     * la main, et les tronquer fait disparaître des survols en silence. Prouvé des deux côtés en
     * ajoutant un geste à chaque scénario -- la garde de la ligne gagnante rougit, celle-ci non.
     *
     * J'avais recopié la FORME d'un contrôle dans un fichier où la grandeur n'a pas de sens :
     * exactement ce qui était arrivé au « test de marge de faisceau » deux reviews plus tôt.
     * Retiré plutôt que rafistolé.
     * ------------------------------------------------------------------------------------------
     */

    /**
     * Un nom de scène inconnu est <b>refusé</b>, il ne retombe pas sur la ligne gagnante.
     *
     * <p>La première version rendait la ligne gagnante pour tout ce qui n'était pas la vitrine —
     * y compris {@code "calibration"}, {@code "banane"} et {@code null}. Son voisin
     * {@code StarfallGame.sceneFor} fait l'inverse et son javadoc revendique le principe : « un
     * ajout de scène oublié ici échoue bruyamment plutôt que d'afficher la mauvaise ». Deux tables
     * de noms, deux disciplines opposées ; c'est celle qui devinait qui avait tort.
     */
    @Test
    @DisplayName("Un nom de scène sans scénario est refusé")
    void anUnknownSceneHasNoScenario() {
        for (String unknown : new String[]{"calibration", "banane", "", null}) {
            org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                    () -> CaptureScenario.forScene(unknown),
                    "aurait du refuser la scene « " + unknown + " »");
        }
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
