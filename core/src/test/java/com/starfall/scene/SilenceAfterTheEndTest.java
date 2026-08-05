package com.starfall.scene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.starfall.game.Arena;
import com.starfall.game.ArenaSetup;
import com.starfall.game.Direction;
import com.starfall.game.Enemy;
import com.starfall.game.EnemyKind;
import com.starfall.game.Grid;
import com.starfall.game.Tile;
import com.starfall.sim.Playout;

import java.util.List;
import java.util.Random;

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

    /** Première coupe de la ligne de capture qui est en vague 4 sans être finie. */
    private static final int ENTERS_FOURTH_WAVE = 42;

    /** Graine qui finit une partie de vague 4 avec une invocation encore annoncée. */
    private static final int WITNESS_SEED = 568;

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
    void aWonGameWithALoadedQueueFallsSilentToo() {
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
        // Le jumeau de previewTop, qui alimente l'infobulle de râtelier, porte la même garde et
        // n'avait aucun témoin côté victoire : le passer à isDefeat() laissait 500 tests verts.
        for (Tile tile : Tile.values()) {
            assertEquals(null, won.preview(tile),
                    "le preavis de « " + tile.label() + " » promet encore apres la victoire");
        }
        // Et l'infobulle de râtelier, éprouvée seulement sur une défaite jusqu'ici.
        for (int slot = 0; slot < won.rack().tiles().size(); slot++) {
            List<String> rackLines = HudText.infoLines(won, slot, -1, -1, null);
            assertTrue(rackLines.stream().anyMatch(line -> line.contains("PARTIE FINIE")),
                    "l'infobulle de ratelier promet encore apres la victoire : " + rackLines);
        }

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
        // Toujours pas d'assertion sur « INVOCATION » ici : la vitrine se joue en vague 1, où le
        // souverain n'existe pas. Voir la partie perdue en vague 4 juste en dessous, et surtout ce
        // que sa mesure a fini par établir.
    }

    /**
     * Une partie <b>perdue en vague 4</b>, souverain debout.
     *
     * <p>Le commentaire d'à côté disait depuis deux reviews : « ce qui manque est une partie perdue
     * en vague 4, et elle n'existe dans aucun scénario ». La première moitié était vraie, la seconde
     * était une <b>supposition</b> — elle existe, il suffisait de la chercher.
     */
    @Test
    @DisplayName("Une partie perdue en vague 4 ne promet rien non plus")
    void aGameLostInTheFourthWavePromisesNothingEither() {
        Arena arena = lostInTheFourthWave();

        assertTrue(arena.isDefeat(), "cette partie devait etre perdue");
        assertEquals(4, arena.wave(), "cette partie devait se perdre en vague 4");
        assertTrue(arena.enemies().stream().anyMatch(e -> e.kind() == EnemyKind.SOUVERAIN),
                "le souverain devait rester debout : sans lui, cet etat ne vaut pas mieux que la"
                        + " vitrine en vague 1");

        String banner = HudText.banner(arena);
        assertTrue(banner.contains("VAGUE 4"), "le bandeau devrait situer la vague : " + banner);
        assertFalse(banner.contains("MENACE"),
                "le bandeau annonce une menace alors que la partie est perdue : " + banner);

        // Et le souverain lui-meme, survole : son infobulle ne doit plus annoncer d'intention.
        int bossCell = -1;
        for (Enemy enemy : arena.enemies()) {
            if (enemy.kind() == EnemyKind.SOUVERAIN) {
                bossCell = arena.grid().indexOf(enemy);
            }
        }
        List<String> lines = HudText.infoLines(arena, -1, -1, bossCell, null);
        assertTrue(lines.stream().anyMatch(line -> line.contains("PARTIE FINIE")),
                "l'infobulle du souverain devrait dire que la partie est finie : " + lines);
    }

    /**
     * Une partie finie <b>pendant qu'une invocation est annoncée</b> : le témoin qui manquait, et
     * dont j'avais écrit qu'il ne pouvait pas exister.
     *
     * <h2>L'affirmation structurelle qui est tombée, la troisième</h2>
     *
     * <p>J'avais conclu, chiffres à l'appui, que cet état était <b>inatteignable</b> : « sur six
     * cents défaites en vague 4, le souverain a zéro invocation restante dans les six cents ; son
     * budget vaut un et il est toujours déjà dépensé ». Une review indépendante l'a réfuté en
     * balayant une population que je n'avais pas regardée — des parties <em>démarrées</em> à chaque
     * vague, et non la seule ligne de capture prolongée. Résultat : <b>quatre fins de partie sur
     * 3 200</b> portent une invocation vivante. Reproduit ici.
     *
     * <p>Le mécanisme, une fois le contre-exemple en main, est simple et j'aurais dû le voir :
     * {@code spendSummon} est appelé à l'<em>exécution</em>, et la phase ennemie termine
     * <b>toujours</b> par {@code announceIntentions}, même quand le héros vient de mourir. Il suffit
     * donc qu'un <em>autre</em> ennemi porte le coup fatal pendant que le souverain, à distance et
     * en phase impaire, a encore son budget : il ré-annonce une invocation après la fin.
     *
     * <p>La leçon est celle que ce projet a déjà payée deux fois, et c'est la troisième :
     * <b>une démonstration ne vaut que pour la population qu'elle a examinée.</b> J'avais cherché le
     * contre-exemple — mais dans un seul couloir, en prolongeant la ligne de capture, là où la
     * défaite arrive toujours tard et le budget toujours dépensé. Le fil-piège que j'avais posé
     * « au cas où le réglage changerait » regardait cette même partie unique : il était vert
     * pendant que l'état existait déjà.
     */
    @Test
    @DisplayName("Le bandeau tait l'invocation annoncée après la fin")
    void theBannerSilencesASummonAnnouncedAfterTheEnd() {
        Arena arena = overWithALiveSummon();

        assertTrue(arena.isOver(), "cette partie devait etre finie");
        // La premisse du temoin. Si elle tombe, ce n'est pas l'assertion suivante qu'il faut
        // croire : c'est ce test qui a cesse de temoigner, et il doit le dire lui-meme.
        assertTrue(arena.anySummonAnnounced(),
                "aucune invocation annoncee sur cette partie finie : le temoin a derive et"
                        + " l'assertion ci-dessous serait vraie pour rien, exactement comme la"
                        + " version qu'elle remplace");

        String banner = HudText.banner(arena);
        assertFalse(banner.contains("INVOCATION"),
                "une invocation est annoncee ET la partie est finie : le bandeau la repete alors"
                        + " que plus rien ne sera joue. " + banner);
    }

    /**
     * La graine qui produit une fin de partie avec une invocation encore annoncée.
     *
     * <p>Quatre couples (vague, graine) le font sur les 3 200 balayés ; celui-ci est fixé pour que
     * le test soit reproductible. Le tirage de largeur consomme le même flux aléatoire que le jeu
     * — c'est ce qui rend la partie déterministe, et c'est pourquoi il n'est pas sorti de la boucle.
     */
    private static Arena overWithALiveSummon() {
        Random random = new Random(WITNESS_SEED);
        Arena arena = ArenaSetup.trainingArena(
                Grid.MIN_WIDTH + random.nextInt(Grid.MAX_WIDTH - Grid.MIN_WIDTH + 1),
                Arena.WAVE_COUNT);
        playUntilOver(arena, random);
        return arena;
    }

    /** Du jeu quelconque, les quatre gestes, jusqu'à la fin ou jusqu'à épuisement du budget. */
    private static void playUntilOver(Arena arena, Random random) {
        for (int step = 0; step < 200 && !arena.isOver(); step++) {
            switch (random.nextInt(4)) {
                case 0 -> arena.step(random.nextBoolean() ? Direction.LEFT : Direction.RIGHT);
                case 1 -> arena.swapWithTarget();
                case 2 -> arena.queueTile(
                        arena.rack().tiles().get(random.nextInt(arena.rack().tiles().size())));
                default -> arena.unleash();
            }
        }
    }

    /**
     * La ligne de capture jusqu'à son entrée en vague 4, puis du jeu quelconque jusqu'à la mort.
     *
     * <p>La coupe 42 est la première où la ligne gagnante est en vague 4 sans être finie : elle est
     * vérifiée ici plutôt que supposée, parce qu'un scénario qui dérive déplacerait cette frontière
     * en silence — c'est déjà arrivé deux fois à ce même scénario.
     */
    private static Arena lostInTheFourthWave() {
        Arena arena = ArenaSetup.trainingArena(9);
        CaptureScript.SCENARIO.replayInto(arena, ENTERS_FOURTH_WAVE);
        assertTrue(!arena.isOver() && arena.wave() == 4,
                "la coupe " + ENTERS_FOURTH_WAVE + " de la ligne de capture n'est plus une vague 4"
                        + " en cours : vague " + arena.wave() + ", finie " + arena.isOver());

        playUntilOver(arena, new Random(0));
        return arena;
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
