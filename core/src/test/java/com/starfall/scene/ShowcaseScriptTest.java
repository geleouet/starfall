package com.starfall.scene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.starfall.game.ActionResult;
import com.starfall.game.Arena;
import com.starfall.game.ArenaSetup;
import com.starfall.game.Enemy;
import com.starfall.game.Tile;
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
        Arena finished = after(ShowcaseScript.DEATH_FRAME);

        assertTrue(finished.isDefeat(), "la vitrine doit perdre : etat final vie "
                + finished.hero().health() + ", partie finie " + finished.isOver());
        // Pas de « et la partie est finie » : isOver() rend « victoire || defaite », donc
        // l'assertion serait impliquee par la precedente. Ce qui compte, et qui peut echouer, c'est
        // que la file soit encore GARNIE et que son sommet soit survole -- le seul etat ou le
        // retour anticipe de hoveringTheTop se voit, et celui qui m'a fait prendre une regression
        // de rendu pour du code mort.
        assertTrue(!finished.queue().isEmpty(),
                "la file doit rester garnie a la mort, elle contient " + finished.queue().size());
    }

    /**
     * <b>Le sommet</b> de la file est survolé à l'image de la mort, et c'est la conjonction qui
     * compte.
     *
     * <p>La première version assertait trois choses séparément — la file garnie, l'emplacement 0
     * survolé, la mort en dernière image — sans jamais leur conjonction, c'est-à-dire la seule
     * propriété qui rend la planche utile. Une review l'a montré en ajoutant <b>un geste</b> au
     * scénario : la file contenait alors deux tuiles, l'emplacement 0 cessait d'être le sommet, les
     * quatre assertions passaient, et l'on pouvait de nouveau retirer le retour anticipé sans qu'un
     * test ni une planche ne bronche.
     *
     * <p>Le remède est de demander à la fonction elle-même plutôt que de recopier sa définition.
     */
    @Test
    @DisplayName("Les deux formes de survol de file sont montrées sur l'état terminal")
    void bothQueueHoversAreShownOnTheTerminalState() {
        Arena dead = after(ShowcaseScript.DEATH_FRAME);
        int rack = ShowcaseScript.SCENARIO.rackSlotAt(ShowcaseScript.DEATH_FRAME);
        int queue = ShowcaseScript.SCENARIO.queueSlotAt(ShowcaseScript.DEATH_FRAME);

        assertTrue(!HudText.hoveringTheTop(dead, rack, queue),
                "l'image de la mort doit survoler un emplacement qui N'EST PAS le sommet : c'est la"
                        + " moitie du correctif qui n'avait aucun temoin. Ratelier " + rack
                        + ", file " + queue + ", taille " + dead.queue().size());
        assertTrue(queue >= 0, "aucun emplacement de file survole a l'image de la mort");
        assertTrue(dead.queue().size() >= 2,
                "la file doit compter au moins deux tuiles, sinon l'emplacement 0 EST le sommet"
                        + " et ce chemin n'est jamais atteint");

        int topQueue = ShowcaseScript.SCENARIO.queueSlotAt(ShowcaseScript.TOP_FRAME);
        assertTrue(HudText.hoveringTheTop(after(ShowcaseScript.TOP_FRAME),
                        ShowcaseScript.SCENARIO.rackSlotAt(ShowcaseScript.TOP_FRAME), topQueue),
                "l'image du sommet doit bien survoler le sommet : file " + topQueue);
    }

    /**
     * Et l'image d'après montre le <b>râtelier</b> survolé sur le même état terminal.
     *
     * <p>Deux règles se cachent derrière « après la mort, rien ne se promet », et une seule image ne
     * peut pas les montrer toutes deux : {@code hoveringTheTop} exige qu'aucune tuile du râtelier ne
     * soit survolée, tandis que la disponibilité se lit sur le râtelier. La tuile survolée doit être
     * <b>rechargée</b>, sans quoi elle serait déjà dessinée en creux et le défaut resterait invisible.
     */
    @Test
    @DisplayName("Le râtelier est survolé sur l'état terminal, et la tuile y est rechargée")
    void theRackIsHoveredOnTheTerminalState() {
        Arena dead = after(ShowcaseScript.RACK_FRAME);
        int slot = ShowcaseScript.SCENARIO.rackSlotAt(ShowcaseScript.RACK_FRAME);

        assertTrue(dead.isOver(), "l'image d'apres la mort doit rester terminale");
        assertTrue(slot >= 0, "aucune tuile du ratelier survolee a l'image "
                + ShowcaseScript.RACK_FRAME);

        Tile hovered = dead.rack().tiles().get(slot);
        assertTrue(dead.rack().isReady(hovered),
                "la tuile survolee « " + hovered.label() + " » n'est pas rechargee : le defaut de"
                        + " disponibilite resterait invisible");
        assertTrue(hovered.reach().length > 0,
                "la tuile survolee « " + hovered.label() + " » a une portee VIDE : rien ne serait"
                        + " dessine, et le defaut resterait invisible pour une seconde raison");
        assertEquals(ShowcaseScript.RACK_FRAME, ShowcaseScript.ACTIONS.size(),
                "l'etat terminal survole doit etre la derniere image du scenario");

        // Les deux derniers gestes DOIVENT etre refuses : c'est toute la premisse des images
        // terminales, et rien ne l'assertait. Si l'arene cessait de les refuser, les trois images
        // cesseraient de porter le meme etat et seul verifyRender le dirait -- qu'un acceptRender
        // distrait enterinerait.
        for (int frame : new int[]{ShowcaseScript.TOP_FRAME, ShowcaseScript.RACK_FRAME}) {
            Arena before = after(frame - 1);
            assertEquals(com.starfall.game.ActionResult.BLOCKED,
                    ShowcaseScript.ACTIONS.get(frame - 1).apply(before),
                    "le geste menant a l'image " + frame + " doit etre refuse");
        }
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

    /**
     * La vitrine n'existe qu'en <b>mode capture</b>, et la demander autrement est refusé.
     *
     * <p>Son scénario n'est rejoué que sous {@code --screenshot} : sans lui, {@code --scene
     * showcase} ouvrait une partie ordinaire tout en prétendant montrer autre chose. Le tableau de
     * bord l'a annoncée jouable pendant un commit entier, ce qui était faux — une review l'a
     * relevé. Une option qui ment vaut mieux refusée qu'expliquée.
     */
    @Test
    @DisplayName("La vitrine sans mode capture est refusée")
    void theShowcaseIsRefusedOutsideCaptureMode() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> com.starfall.LaunchOptions.parse(
                        new String[]{"--scene", ShowcaseScript.SCENE_NAME}),
                "la vitrine sans --screenshot aurait du etre refusee");

        // Et avec la capture, elle passe : le refus vise l'usage trompeur, pas la scene.
        assertEquals(ShowcaseScript.SCENE_NAME, com.starfall.LaunchOptions.parse(new String[]{
                "--screenshot", "captures/x", "--scene", ShowcaseScript.SCENE_NAME}).scene);
    }

    /** Et le nom que la scène rend est celui de son scénario, pas « arena » pour tout le monde. */
    @Test
    @DisplayName("La scène rend le nom de son scénario")
    void theSceneReportsItsOwnName() {
        assertEquals(ShowcaseScript.SCENE_NAME, new ArenaScene(ShowcaseScript.SCENARIO).name());
        assertEquals(CaptureScript.SCENE_NAME, new ArenaScene().name());
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
