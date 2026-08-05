package com.starfall.scene;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.starfall.game.Arena;
import com.starfall.game.ArenaSetup;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Les planches-contact doivent montrer ce qu'elles légendent.
 *
 * <h2>Pourquoi ce test existe</h2>
 *
 * <p>C'est la règle que le projet s'est donnée après le jalon de la résolution du combat, et elle a
 * été enfreinte deux fois depuis — parce que rien ne la vérifiait.
 *
 * <p>La première fois, les légendes portaient encore le texte de deux jalons plus tôt : le commit
 * n'avait échangé que les chemins d'images. La seconde, plus grave, au jalon d'équilibrage : le
 * scénario écrit pour l'ancienne économie s'est mis à mourir en vague 1 au tour 7, sans qu'une seule
 * tuile ne se pose à côté d'une autre. <b>Douze images livrées pour un jalon intitulé « la file a
 * enfin une économie », où l'on ne voyait jamais de salve.</b> Aucune assertion n'a bronché, parce
 * qu'il n'y en avait aucune.
 *
 * <p>Ce test ne juge pas de l'esthétique — il vérifie que le scénario <b>atteint les états qu'il
 * prétend montrer</b>. Il n'a besoin d'aucun contexte graphique : le scénario ne touche qu'à
 * l'arène, ce qui est précisément la raison pour laquelle il a pu quitter la scène.
 */
class CaptureScriptTest {

    /** La largeur par défaut du mode capture, celle des planches livrées. */
    private static final int GRID = 9;

    private static Arena after(int actions) {
        Arena arena = ArenaSetup.trainingArena(GRID);
        CaptureScript.SCENARIO.replayInto(arena, actions);
        return arena;
    }

    /**
     * Le scénario doit <b>charger</b> : une file qui ne dépasse jamais une tuile ne montre rien de
     * l'économie du jeu.
     *
     * <h2>Pourquoi le seuil est à deux et non à cinq</h2>
     *
     * <p>Il devrait être à cinq — c'est la capacité de la file, et le jalon d'équilibrage s'appelle
     * « file de 5 ». Il est à deux parce que c'est ce que le jeu <b>permet</b> aujourd'hui, et le
     * constater ici vaut mieux que l'espérer : la version à trois tuiles de ce scénario finissait à
     * <b>un point de vie sur huit</b>, et l'assertion de survie l'a refusée.
     *
     * <p>C'est la même mesure que la review du jalon a faite de son côté : sur douze configurations,
     * six meurent avant la quatrième tuile, et aucune ligne gagnante ne remplit jamais la file. Le
     * seuil de ce test est donc un <b>relevé</b>, pas un objectif. Il est consigné comme question
     * ouverte au tableau de bord — rendre le chargement survivable est une décision de game design,
     * et le jour où elle sera prise, ce nombre devra monter.
     */
    @Test
    @DisplayName("Le scénario charge une file de plusieurs tuiles")
    void theScriptLoadsAQueueOfSeveralTiles() {
        int deepest = 0;
        for (int step = 0; step <= CaptureScript.ACTIONS.size(); step++) {
            deepest = Math.max(deepest, after(step).queue().size());
        }

        assertTrue(deepest >= 2,
                "la file du scenario ne depasse jamais " + deepest + " tuile(s) : les planches ne"
                        + " montreraient pas ce que la file apporte");
    }

    /**
     * Et il doit <b>lâcher</b> : une salve de plusieurs tuiles pour un seul tour est la promesse
     * centrale du jeu, et c'est ce que les planches doivent donner à voir.
     */
    @Test
    @DisplayName("Le scénario lance une salve de plusieurs tuiles pour un seul tour")
    void theScriptFiresAMultiTileVolley() {
        boolean found = false;
        for (int step = 0; step < CaptureScript.ACTIONS.size(); step++) {
            Arena before = after(step);
            int queued = before.queue().size();
            int turns = before.turnsTaken();
            if (queued < 2) {
                continue;
            }
            Arena andAfter = after(step + 1);
            if (andAfter.queue().isEmpty() && andAfter.turnsTaken() <= turns + 1) {
                found = true;
                break;
            }
        }

        assertTrue(found, "aucune salve de plusieurs tuiles dans le scenario : les planches ne"
                + " montreraient pas le decalage qui fait tout le jeu");
    }

    /**
     * Le scénario ne doit pas se terminer par une défaite.
     *
     * <p>C'est exactement ce qui s'est produit au jalon d'équilibrage : les dernières images
     * affichaient <code>PARTIE PERDUE</code> et <code>VAGUE 1/4</code>. Ce n'est pas qu'une question
     * de fierté — une planche qui montre une défaite précoce dit au relecteur que le jeu est
     * injouable, ce qui était faux.
     */
    @Test
    @DisplayName("Le scénario ne meurt pas avant la fin")
    void theScriptDoesNotDieBeforeTheEnd() {
        Arena finished = after(CaptureScript.ACTIONS.size());

        assertTrue(!finished.isDefeat(),
                "le scenario de capture meurt au tour " + finished.turnsTaken()
                        + " en vague " + finished.wave()
                        + " : les planches livrees montreraient une defaite");
        assertTrue(finished.hero().health() >= 2,
                "le scenario finit a " + finished.hero().health() + " point(s) de vie : trop juste"
                        + " pour que les planches montrent un jeu qui se joue");
    }

    /**
     * Et il doit faire basculer une vague : c'est la seule façon de montrer sur une planche que le
     * combat progresse plutôt que de tourner en rond.
     */
    @Test
    @DisplayName("Le scénario fait basculer au moins une vague")
    void theScriptFlipsAtLeastOneWave() {
        Arena finished = after(CaptureScript.ACTIONS.size());

        assertTrue(finished.wave() >= 2,
                "le scenario reste en vague " + finished.wave()
                        + " : aucune planche ne montrerait l'enchainement des vagues");
    }

    /**
     * Les tableaux de survol doivent couvrir toutes les images qu'on capture, sinon les dernières
     * planches perdent silencieusement leurs infobulles et leurs repères de portée.
     */
    @Test
    @DisplayName("Le survol scénarisé couvre tout le scénario")
    void theScriptedHoverCoversTheWholeScript() {
        int frames = CaptureScript.ACTIONS.size() + 1;

        // La MEME borne pour les deux tableaux. La version precedente en demandait « frames - 1 »
        // au ratelier et « frames » aux cases : deux exigences differentes pour deux tableaux qui
        // couvrent les memes images, et la premiere avait ete relachee d'exactement un pour
        // accepter que la derniere planche perde son survol. Le test benissait ce qu'il pretendait
        // interdire.
        assertTrue(CaptureScript.HOVERED_RACK_SLOT.length >= frames,
                "le survol du ratelier s'arrete a l'image "
                        + CaptureScript.HOVERED_RACK_SLOT.length + " pour " + frames + " images");
        // Le troisième tableau était sans borne, et c'est le défaut que le commentaire ci-dessus
        // décrit mot pour mot : tronqué à douze entrées, les deux survols de file disparaissaient
        // et la suite restait verte, parce que queueSlotAt rend -1 hors bornes. Une perte
        // silencieuse est pire qu'une erreur.
        assertTrue(CaptureScript.HOVERED_QUEUE_SLOT.length >= frames,
                "le tableau des survols de file ne couvre que "
                        + CaptureScript.HOVERED_QUEUE_SLOT.length + " images pour " + frames);
        assertTrue(CaptureScript.HOVERED_CELL.length >= frames,
                "le survol des cases s'arrete a l'image "
                        + CaptureScript.HOVERED_CELL.length + " pour " + frames + " images");

        // Et surtout : une case survolee doit porter quelqu'un. Le seul survol de plateau du
        // scenario visait une case VIDE, donc aucune planche livree ne montrait l'infobulle
        // d'ennemi - la seule raison d'etre de ce tableau. Verifier des bornes ne suffit pas.
        for (int frame = 0; frame < CaptureScript.HOVERED_CELL.length; frame++) {
            int cell = CaptureScript.cellAt(frame);
            if (cell < 0) {
                continue;
            }
            assertTrue(after(frame).grid().occupantAt(cell) instanceof com.starfall.game.Enemy,
                    "image " + frame + " : la case survolee " + (cell + 1)
                            + " ne porte aucun ennemi, l'infobulle ne montrera rien");
        }

        // Un emplacement de file survolé doit être OCCUPÉ à cette image-là. Le tableau des cases
        // avait exactement ce défaut : il désignait une case vide, si bien qu'aucune planche ne
        // montrait l'infobulle d'ennemi alors que c'était sa seule raison d'être. Un survol qui ne
        // survole rien ne se voit pas sur la planche, et ne se voit pas non plus dans le tableau.
        for (int frame = 0; frame < CaptureScript.HOVERED_QUEUE_SLOT.length; frame++) {
            int slot = CaptureScript.HOVERED_QUEUE_SLOT[frame];
            if (slot < 0) {
                continue;
            }
            int size = after(frame).queue().size();
            assertTrue(slot < size, "image " + frame + " : la file compte " + size
                    + " tuile(s), le survol designe l'emplacement " + slot);
        }

        for (int slot : CaptureScript.HOVERED_RACK_SLOT) {
            assertTrue(slot >= -1 && slot < 6, "emplacement de ratelier hors bornes : " + slot);
        }
        for (int cell : CaptureScript.HOVERED_CELL) {
            assertTrue(cell >= -1 && cell < GRID, "case survolee hors grille : " + cell);
        }
    }
}
