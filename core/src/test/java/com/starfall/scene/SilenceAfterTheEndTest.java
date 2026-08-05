package com.starfall.scene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.starfall.game.Arena;
import com.starfall.game.ArenaSetup;
import com.starfall.game.Enemy;
import com.starfall.game.Tile;

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

    /*
     * ------------------------------------------------------------------------------------------
     * Ici vivait « Une partie GAGNÉE se tait autant qu'une partie perdue », écrit pour combler le
     * trou qu'une review avait nommé : tous les témoins « après la fin » étaient du côté de la
     * défaite.
     *
     * IL NE GARDAIT RIEN, et c'est mesuré : en remplaçant isOver() par isDefeat() dans
     * Arena.swapTarget -- c'est-à-dire en retirant la garde pour la moitié « victoire » -- la suite
     * restait entièrement verte.
     *
     * La raison est structurelle et vaut mieux que le test. À la victoire, le plateau est VIDE et
     * la file l'est aussi (sondé : 0 ennemi, file vide, menace 0, aucune invocation) -- la victoire
     * ne se déclare que lorsque le terrain se vide sur la dernière vague. Chacune des « silences »
     * y tient donc pour une raison qui n'a rien à voir avec la garde :
     *
     *   - swapTarget rend -1 parce qu'il n'y a personne à échanger ;
     *   - previewTop rend null parce que la file est vide ;
     *   - le bandeau ne dit ni MENACE ni INVOCATION parce qu'il n'y a plus d'ennemi pour annoncer ;
     *   - queueHead n'est appelé sur aucune tuile.
     *
     * La branche « victoire » de ces gardes est donc INOBSERVABLE par construction. Ce n'est pas un
     * trou de test : c'est un cas qui ne peut pas diverger. Le seul témoin possible de cette famille
     * est la défaite, et il est ci-dessous.
     *
     * Écrire ce test m'aurait donné quatre assertions vertes et zéro pouvoir de détection -- le
     * quatorzième garde-fou de ce genre. Il a été trouvé par mutation avant d'être commité, ce qui
     * est la première fois : les treize précédents l'ont été par une review, après coup.
     * ------------------------------------------------------------------------------------------
     */

    @Test
    @DisplayName("Une partie finie ne désigne plus de cible d'échange")
    void afinishedGameNamesNoSwapTarget() {
        Arena arena = defeated();

        assertTrue(arena.isOver(), "l'arene devait etre finie");
        assertEquals(-1, arena.swapTarget(),
                "swapTarget designe encore la case " + arena.swapTarget() + " alors qu'aucun"
                        + " echange n'est possible : clickable rend faux et swapWithTarget rend"
                        + " BLOCKED, cette methode doit dire la meme chose");
    }

    @Test
    @DisplayName("Le bandeau n'annonce plus de menace ni d'invocation")
    void thebannerAnnouncesNoMoreThreat() {
        String banner = HudText.banner(defeated());

        assertFalse(banner.contains("MENACE"),
                "le bandeau annonce encore une menace sur une partie finie : " + banner);
        assertFalse(banner.contains("INVOCATION"),
                "le bandeau annonce encore une invocation sur une partie finie : " + banner);
    }

    @Test
    @DisplayName("L'infobulle de file ne dit plus quelle tuile partira")
    void thequeueTooltipNamesNoNextTile() {
        Arena arena = defeated();
        List<Tile> queued = arena.queue().fromOldest();
        assertTrue(queued.size() >= 2, "la file devait rester garnie, elle contient " + queued);

        for (int slot = 0; slot < queued.size(); slot++) {
            String head = HudText.queueHead(queued, slot, true);
            assertFalse(head.contains("SOMMET"),
                    "l'infobulle dit encore quelle tuile part la premiere : " + head);
            assertTrue(head.contains("PARTIE FINIE"),
                    "l'infobulle devrait dire que la partie est finie : " + head);
        }
    }

    @Test
    @DisplayName("L'infobulle d'ennemi n'annonce plus d'intention")
    void theenemyTooltipAnnouncesNoIntention() {
        Arena arena = defeated();
        assertTrue(!arena.enemies().isEmpty(), "il devait rester des ennemis");

        for (Enemy enemy : arena.enemies()) {
            String detail = HudText.enemyDetail(enemy, true);
            assertEquals("PARTIE FINIE", detail,
                    "l'infobulle annonce encore « " + detail + " » pour un ennemi dont l'intention"
                            + " ne sera jamais jouee");
        }
    }

    /**
     * Et le préavis se tait déjà — c'est la première porte de la famille, fermée bien avant les
     * autres. Elle est répétée ici pour que la famille se lise d'un seul endroit.
     */
    @Test
    @DisplayName("Le préavis du sommet se tait, comme il le faisait déjà")
    void thetopPreviewStaysSilent() {
        Arena arena = defeated();

        assertEquals(null, arena.previewTop(),
                "le preavis du sommet promet encore un resultat sur une partie finie");
    }
}
