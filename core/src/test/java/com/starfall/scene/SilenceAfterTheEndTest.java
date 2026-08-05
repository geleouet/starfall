package com.starfall.scene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.starfall.game.Arena;
import com.starfall.game.ArenaSetup;
import com.starfall.game.Enemy;
import com.starfall.game.Tile;
import com.starfall.sim.Playout;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * <b>Après la fin de partie, le jeu ne promet plus rien.</b> Toutes les règles, au même endroit.
 *
 * <h2>Pourquoi ce fichier existe</h2>
 *
 * <p>Ce n'est pas une règle mais une famille, et elle a été fermée <b>porte par porte, sur quatre
 * reviews</b> : la portée d'une tuile survolée, puis le sommet de la file, puis le râtelier, puis
 * les bandes de menace et la cible d'échange, puis enfin le glyphe d'intention — qui était peint sur
 * les planches mêmes construites pour prouver qu'aucune promesse ne survit.
 *
 * <p>La cause de cette répétition n'est pas l'inattention : c'est que <b>le seul témoin de ces
 * règles était un garde-fou d'image</b>, hors de {@code gradlew test}, qui demande un écran et se
 * lance à la main. Une review l'a dit sans détour : quatre de ces règles vivent dans du code
 * <em>pur et testable</em> et n'avaient aucun test JUnit — on pouvait les casser en gardant 493
 * tests verts. Le tableau de bord écrit lui-même qu'« une référence qu'on adopte sans regarder est
 * pire qu'aucune référence » ; c'est exactement ce qui s'est produit.
 *
 * <p>Ce fichier rassemble ce qui peut être éprouvé sans écran, pour que la famille entière ait un
 * témoin dans la suite ordinaire. Ce qui relève du dessin reste gardé par l'image — mais plus
 * <em>seulement</em> par elle.
 */
class SilenceAfterTheEndTest {

    /**
     * Une arène perdue, avec une file garnie et des ennemis qui avaient annoncé quelque chose.
     *
     * <p>C'est la ligne de la <b>vitrine</b>, et ce n'est pas de la paresse : elle est déjà tenue
     * par ses propres assertions — elle doit perdre, mourir avec au moins deux tuiles en file — et
     * la réutiliser lie ces règles à l'état exact que les planches de référence montrent. Deux
     * témoins du même instant, l'un sans écran, l'autre avec.
     */
    private static Arena defeated() {
        Arena arena = ArenaSetup.trainingArena(9, 1);
        ShowcaseScript.SCENARIO.replayInto(arena, ShowcaseScript.DEATH_FRAME);
        return arena;
    }

    /**
     * Une arène <b>gagnée dont la file est encore garnie</b>.
     *
     * <h3>Comment j'ai failli supprimer ce test</h3>
     *
     * <p>Je l'avais écrit, éprouvé par mutation, jugé vide et retiré — en démontrant qu'à la
     * victoire « le plateau est vide et la file l'est aussi », donc que la branche « victoire » de
     * ces gardes serait <b>inobservable par construction</b>.
     *
     * <p><b>C'était faux</b>, et le contre-exemple était dans {@code Arena.unleash} : quand une
     * salve vide le terrain, la boucle <b>sort</b> et « ce qui reste dans la file y reste ». À la
     * dernière vague, cela veut dire qu'elle reste <em>après la victoire</em>. Une review l'a
     * relevé, et une recherche sur de vraies parties le confirme — ligne ci-dessous, quatorze
     * gestes, victoire avec un estoc encore chargé.
     *
     * <p>Ma mutation ne portait que sur {@code swapTarget}, la <b>seule</b> des quatre méthodes où
     * la conclusion tenait : le plateau, lui, est bien vide à la victoire. J'ai généralisé d'un
     * instrument à quatre règles — deuxième fois que je supprime du code sur une démonstration
     * juste mais incomplète, après celle qui oubliait le cas de la fin de partie.
     *
     * <p>La leçon est la même à chaque fois, et elle mérite d'être écrite ici plutôt que dans un
     * journal : <b>une démonstration ne vaut que pour ce qu'elle a examiné</b>. « Inobservable par
     * construction » est une affirmation forte ; elle demande qu'on ait cherché le contre-exemple,
     * pas seulement qu'on n'en ait pas vu.
     */
    private static Arena victoriousWithALoadedQueue() {
        Arena arena = Playout.replay(5, Arena.WAVE_COUNT, List.of(
                "pas droite", "échange", "poser frappe", "poser poussée", "poser volte-face",
                "salve", "échange", "poser frappe", "poser poussée", "poser volte-face", "salve",
                "poser estoc", "poser frappe", "salve"));
        return arena;
    }

    @Test
    @DisplayName("Une partie GAGNÉE dont la file est garnie se tait aussi")
    void awonGameWithALoadedQueueFallsSilentToo() {
        Arena won = victoriousWithALoadedQueue();

        assertTrue(won.isVictory(), "la ligne devait gagner");
        assertTrue(!won.queue().isEmpty(),
                "la file devait rester garnie : sans cela ce test ne garde rien, et c'est"
                        + " exactement l'erreur qui l'avait fait supprimer");

        assertEquals(null, won.previewTop(),
                "le preavis du sommet promet encore un resultat apres la victoire, alors qu'une"
                        + " tuile attend bien dans la file");

        // Par infoLines et non par queueHead : le correctif n'est pas la branche « over », c'est
        // le CÂBLAGE qui lui passe arena.isOver(). Une review l'a montré sur le jumeau — un test
        // qui écrit « true » à la main éprouve la branche et jamais le fil qui l'alimente.
        for (int slot = 0; slot < won.queue().size(); slot++) {
            List<String> lines = HudText.infoLines(won, -1, slot, -1, null);
            assertTrue(lines.stream().anyMatch(line -> line.contains("PARTIE FINIE")),
                    "l'infobulle de file promet encore apres la victoire : " + lines);
        }
    }

    @Test
    @DisplayName("Une partie finie ne désigne plus de cible d'échange")
    void aFinishedGameNamesNoSwapTarget() {
        Arena arena = defeated();

        assertTrue(arena.isOver(), "l'arene devait etre finie");
        assertEquals(-1, arena.swapTarget(),
                "swapTarget designe encore la case " + arena.swapTarget() + " alors qu'aucun"
                        + " echange n'est possible : clickable rend faux et swapWithTarget rend"
                        + " BLOCKED, cette methode doit dire la meme chose");
    }

    @Test
    @DisplayName("Le bandeau n'annonce plus de menace ni d'invocation")
    void theBannerAnnouncesNoMoreThreat() {
        String banner = HudText.banner(defeated());

        assertFalse(banner.contains("MENACE"),
                "le bandeau annonce encore une menace sur une partie finie : " + banner);
        // Pas d'assertion sur « INVOCATION » : la vitrine se joue en vague 1, où aucune
        // invocation n'est jamais annoncée — le souverain n'apparaît qu'en vague 4. L'assertion
        // aurait été vraie par construction, et une review l'a mesuré : retirer la garde
        // correspondante laissait la suite verte. Ce qui manque est une partie PERDUE en vague 4,
        // et elle n'existe dans aucun scénario ; c'est écrit ici plutôt que masqué par un vert.
    }

    @Test
    @DisplayName("L'infobulle de file ne dit plus quelle tuile partira")
    void theQueueTooltipNamesNoNextTile() {
        Arena arena = defeated();
        List<Tile> queued = arena.queue().fromOldest();
        assertTrue(queued.size() >= 2, "la file devait rester garnie, elle contient " + queued);

        for (int slot = 0; slot < queued.size(); slot++) {
            List<String> lines = HudText.infoLines(arena, -1, slot, -1, null);
            assertFalse(lines.stream().anyMatch(line -> line.contains("SOMMET")),
                    "l'infobulle dit encore quelle tuile part la premiere : " + lines);
            assertTrue(lines.stream().anyMatch(line -> line.contains("PARTIE FINIE")),
                    "l'infobulle devrait dire que la partie est finie : " + lines);
        }
    }

    @Test
    @DisplayName("L'infobulle d'ennemi n'annonce plus d'intention")
    void theEnemyTooltipAnnouncesNoIntention() {
        Arena arena = defeated();
        assertTrue(!arena.enemies().isEmpty(), "il devait rester des ennemis");

        // Par infoLines, en survolant réellement la case de l'ennemi : c'est le câblage qui était
        // aveugle, pas la branche. Remettre « false » au point d'appel laissait 498 tests verts ET
        // 84 planches conformes — aucun témoin des deux côtés, ce que la review a mesuré.
        for (Enemy enemy : arena.enemies()) {
            int cell = arena.grid().indexOf(enemy);
            List<String> lines = HudText.infoLines(arena, -1, -1, cell, null);
            assertTrue(lines.contains("PARTIE FINIE"),
                    "l'infobulle d'ennemi annonce encore une intention qui ne sera jamais jouee : "
                            + lines);
        }
    }

    /**
     * Et le préavis se tait déjà — c'est la première porte de la famille, fermée bien avant les
     * autres. Elle est répétée ici pour que la famille se lise d'un seul endroit.
     */
    @Test
    @DisplayName("Le préavis du sommet se tait, comme il le faisait déjà")
    void theTopPreviewStaysSilent() {
        Arena arena = defeated();

        assertEquals(null, arena.previewTop(),
                "le preavis du sommet promet encore un resultat sur une partie finie");
    }
}
