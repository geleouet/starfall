package com.starfall.scene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.starfall.game.ActionResult;
import com.starfall.game.Arena;
import com.starfall.game.Direction;
import com.starfall.game.Tile;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Le déroulé d'une action : ce qu'il montre, et surtout ce qu'il ne montre pas.
 *
 * <p>Ces règles vivent ici plutôt que dans le garde-fou d'images pour la raison que ce projet a
 * payée plusieurs fois : une règle gardée seulement par une planche n'est gardée qu'à la main.
 */
class PlaybackTest {

    /**
     * Un temps ou une figure se tient sur la case dite.
     *
     * <p>Le plateau ne jouait AUCUN role dans ce fichier : ces temps portaient tous un plateau
     * vide, parce qu'il ne gardait que la machine du deroule - quand il court, dans quel ordre,
     * quand il finit. La machine depend desormais des plateaux : un temps ou rien ne bouge ne
     * prend pas de place. Des plateaux vides bougeraient tous pareil, c'est-a-dire pas du tout,
     * et ce fichier aurait garde une machine qui ne demarre jamais en croyant garder l'ordre de
     * ses temps.
     */
    private static Arena.Beat beat(Tile tile, int cell) {
        return new Arena.Beat(tile, ActionResult.STRUCK, cell, board(cell), List.of());
    }

    /** Un plateau d'une seule figure, posee sur cette case. */
    private static List<Arena.Figure> board(int cell) {
        return List.of(new Arena.Figure(7L, cell, "enemy/sabreur", Direction.LEFT, 3, 3,
                false, false));
    }

    /**
     * Demarre un deroule sans lui donner d'etat final a rattraper.
     *
     * <p>Le plateau etabli est celui du dernier temps : ces tests-ci gardent la machine, pas le
     * temps final qui repare la riposte. Lui passer autre chose ajouterait un temps de plus a
     * chaque cas et ferait mentir tous les comptes.
     */
    private static void start(Playback playback, List<Arena.Beat> beats) {
        playback.start(beats, List.of(),
                beats.isEmpty() ? List.of() : beats.get(beats.size() - 1).board());
    }

    /**
     * La regle qui a remplace « une action d'un seul temps ne se deroule pas ».
     *
     * <p>L'ancienne avait ete ecrite quand un deroule n'etait qu'une suite d'images figees : avec
     * un seul temps, il n'y avait rien a egrener. Le mouvement est continu depuis, et cet argument
     * ne couvre plus rien - un temps unique a un trajet. Il couvrait meme mal : le PAS, geste le
     * plus frequent du jeu, tombait dedans et restait le seul a ne pas bouger.
     */
    @Test
    @DisplayName("Un temps où quelque chose bouge se déroule, fût-il seul")
    void aLoneBeatStillUnfoldsWhenSomethingMoves() {
        Playback playback = new Playback();

        start(playback, List.of(beat(Tile.STRIKE, 3)));

        assertTrue(playback.isRunning(),
                "un temps unique a un trajet depuis que le mouvement est continu : c'est le cas du"
                        + " pas, et il n'y a aucune raison de le laisser sauter");
        assertEquals(1, playback.step());
        assertEquals(1, playback.total());
    }

    @Test
    @DisplayName("Un temps sans tuile où rien ne bouge ne prend pas de place")
    void aTilelessBeatWhereNothingMovesTakesNoTime() {
        Playback playback = new Playback();
        Arena.Beat still = new Arena.Beat(null, null, -1, board(3), List.of());

        // Le plateau d'ouverture EST celui du temps : le geste n'a deplace personne.
        playback.start(List.of(still), still.board(), still.board());

        assertFalse(playback.isRunning(),
                "un geste qui ne deplace rien - se retourner, poser une tuile - n'a rien a montrer :"
                        + " attendre ajouterait de la latence sans ajouter de lecture");
        assertNull(playback.current());
        assertEquals(0, playback.step());
    }

    /**
     * Une tuile garde son temps même quand le plateau n'en garde aucune trace.
     *
     * <p>La première version de la règle écartait tout temps immobile, tuile comprise. La
     * volte-face ne déplace personne : elle partait, le panneau ne la nommait jamais, et le joueur
     * voyait sa salve de trois tuiles compter deux temps. Le défaut a été vu <b>sur une planche</b>,
     * où le compteur affichait « SALVE 1/2 » après trois tuiles lâchées. Le panneau qui nomme un
     * coup <em>est</em> l'information, autant que le mouvement quand il y en a un.
     */
    @Test
    @DisplayName("Une tuile immobile garde quand même son temps")
    void aTileKeepsItsBeatEvenWhenNothingMoves() {
        Playback playback = new Playback();
        Arena.Beat pivot = beat(Tile.PIVOT, 3);

        playback.start(List.of(pivot), pivot.board(), pivot.board());

        assertTrue(playback.isRunning(),
                "la volte-face a joue : sans son temps, elle ne serait nommee nulle part");
        assertEquals(Tile.PIVOT, playback.current().tile());
    }

    /**
     * Le temps final, celui qui manquait.
     *
     * <p>La phase ennemie se joue APRES que les temps ont ete enregistres, et la vague suivante
     * apparait apres elle. Le deroule s'achevait donc sur le plateau d'avant la riposte, et la
     * scene basculait d'un coup sur l'etat vrai : l'illisibilite que ce deroule devait supprimer
     * avait ete repoussee a la fin.
     */
    @Test
    @DisplayName("Ce qu'aucune tuile n'explique forme un dernier temps")
    void whatNoTileExplainsBecomesALastBeat() {
        Playback playback = new Playback();
        Arena.Beat fired = beat(Tile.STRIKE, 3);

        // Le plateau etabli differe de celui du dernier temps : les ennemis ont riposté.
        playback.start(List.of(fired), List.of(), board(6));

        assertEquals(2, playback.total(),
                "la riposte ennemie doit former un temps a elle : sans lui, l'ecran saute des"
                        + " ennemis d'avant a ceux d'apres sans que rien ne montre le trajet");
        playback.advance(Playback.BEAT_SECONDS);
        assertNull(playback.current().tile(),
                "le temps final ne porte aucune tuile : ce n'est pas le joueur qui l'a joue");
    }

    /**
     * Le pas de temps d'une image, borné.
     *
     * <p>La scène calcule son pas par différence avec l'image précédente. À la <b>première</b>, il
     * n'y en a pas eu : le pas valait tout le temps écoulé depuis le démarrage. Et lorsqu'une image
     * arrive <b>en retard</b> — fenêtre déplacée, point d'arrêt, ramassage de miettes — il vaut
     * plusieurs secondes. Dans les deux cas, un pas non borné fait avaler le déroulé entier par
     * l'image même qui devait le montrer.
     *
     * <p>Ce défaut était <em>latent</em> : signalé trois contrôles de suite, jamais atteint, parce
     * qu'aucun déroulé ne court à la première image. Il l'aurait été le jour où quelque chose en
     * aurait lancé un plus tôt. On ne garde pas un piège en pariant sur ce qui ne l'atteint pas
     * encore.
     */
    @Test
    @DisplayName("Une image en retard ralentit le déroulé, elle ne l'avale pas")
    void aLateFrameSlowsTheUnfoldingInsteadOfSwallowingIt() {
        assertEquals(Playback.BEAT_SECONDS, Playback.frameStep(3f), 1e-6f,
                "trois secondes d'un coup emporteraient tout un deroule : le pas se borne a un"
                        + " temps, de sorte que l'animation ralentit au lieu de sauter");
        assertEquals(0f, Playback.frameStep(-42f), 1e-6f,
                "le pas de la premiere image se calcule contre une image qui n'a pas eu lieu");
        assertEquals(0.01f, Playback.frameStep(0.01f), 1e-6f,
                "un pas ordinaire traverse sans etre touche, sinon la borne changerait la vitesse"
                        + " de tout le jeu au lieu de rattraper les accidents");
    }

    @Test
    @DisplayName("Une action vide ne se déroule pas non plus")
    void anEmptyActionDoesNotUnfoldEither() {
        Playback playback = new Playback();
        start(playback, List.of());
        assertFalse(playback.isRunning());
    }

    @Test
    @DisplayName("Une salve montre ses temps dans l'ordre, un par durée")
    void aVolleyShowsItsBeatsInOrder() {
        Arena.Beat first = beat(Tile.STRIKE, 3);
        Arena.Beat second = beat(Tile.THRUST, 4);
        Arena.Beat third = beat(Tile.PUSH, 5);
        Playback playback = new Playback();

        start(playback, List.of(first, second, third));

        assertTrue(playback.isRunning(), "trois temps : il y a de quoi derouler");
        assertSame(first, playback.current());
        assertEquals(1, playback.step());
        assertEquals(3, playback.total());

        playback.advance(Playback.BEAT_SECONDS);
        assertSame(second, playback.current(), "le deuxieme temps vient apres le premier");
        assertEquals(2, playback.step());

        playback.advance(Playback.BEAT_SECONDS);
        assertSame(third, playback.current());

        playback.advance(Playback.BEAT_SECONDS);
        assertFalse(playback.isRunning(), "apres le dernier temps, le deroule est fini");
        assertNull(playback.current());
    }

    @Test
    @DisplayName("Le déroulé finit, quelle que soit la taille des pas de temps")
    void theUnfoldingAlwaysEnds() {
        // La contrainte inscrite au tableau de bord : le garde-fou d'images capture des etats AU
        // REPOS, donc un deroule qui ne finirait pas - ou qui dependrait du temps reel ecoule -
        // rendrait les captures impossibles. Il ne connait aucune horloge : il n'avance que par ce
        // qu'on lui donne, et il finit toujours.
        for (float step : new float[] {0.001f, 0.05f, Playback.BEAT_SECONDS, 3f, 100f}) {
            Playback playback = new Playback();
            start(playback, List.of(beat(Tile.STRIKE, 1), beat(Tile.THRUST, 2),
                    beat(Tile.PUSH, 3), beat(Tile.DASH, 4), beat(Tile.PIVOT, 5)));

            int guard = 0;
            while (playback.isRunning() && guard++ < 1_000_000) {
                playback.advance(step);
            }

            assertFalse(playback.isRunning(),
                    "un deroule ne finit pas avec des pas de " + step + " s");
            assertTrue(guard < 1_000_000, "il a fallu un million de pas de " + step + " s");
        }
    }

    /**
     * La position posée, celle dont vivent les planches de mi-course.
     *
     * <p>Le mode capture ne peut pas avancer par accumulation jusqu'à « la fin du temps 2 » :
     * avancer d'exactement une durée de temps bascule sur le suivant, donc la dernière fraction
     * serait toujours hors d'atteinte. {@code seek} pose la position au lieu de l'atteindre, et
     * c'est ce qui rend une planche prise à 35 % d'un temps reproductible.
     *
     * <p>Un {@code seek} faux ne casserait rien de visible : il déplacerait <em>silencieusement</em>
     * ce que chaque planche de mi-course montre, et l'on accepterait des images en croyant regarder
     * un autre instant que celui qu'on regarde.
     */
    @Test
    @DisplayName("Se placer sur un instant donné y place vraiment, bornes comprises")
    void seekingPutsThePlaybackExactlyThere() {
        Playback playback = new Playback();
        start(playback, List.of(beat(Tile.STRIKE, 1), beat(Tile.THRUST, 2),
                beat(Tile.PUSH, 3)));

        playback.seek(2, 0.35f);
        assertEquals(2, playback.step(), "le rang demande est celui qui s'affiche");
        assertEquals(0.35f, playback.progress(), 1e-6f, "la fraction demandee est celle qui court");
        assertTrue(playback.isRunning(), "se placer au milieu ne termine pas le deroule");

        // La fin d'un temps est ATTEIGNABLE, et c'est tout l'interet : c'est elle que rendaient les
        // planches d'avant l'animation, donc c'est par elle qu'elles restent conformes.
        playback.seek(3, 1f);
        assertEquals(3, playback.step());
        assertEquals(1f, playback.progress(), 1e-6f);
        assertTrue(playback.isRunning(), "la fin du dernier temps s'affiche encore : elle EST une"
                + " image, et non l'apres-deroule");

        playback.seek(99, 4f);
        assertEquals(3, playback.step(), "un rang trop grand se rabat sur le dernier temps");
        assertEquals(1f, playback.progress(), 1e-6f, "une fraction trop grande se rabat sur 1");

        playback.seek(-5, -2f);
        assertEquals(1, playback.step(), "un rang trop petit se rabat sur le premier temps");
        assertEquals(0f, playback.progress(), 1e-6f, "une fraction negative se rabat sur 0");
    }

    @Test
    @DisplayName("Un pas de temps très long ne saute pas le déroulé à moitié")
    void oneHugeStepEndsItCleanly() {
        Playback playback = new Playback();
        start(playback, List.of(beat(Tile.STRIKE, 1), beat(Tile.THRUST, 2)));

        playback.advance(1000f);

        assertFalse(playback.isRunning());
        assertNull(playback.current(), "aucun temps ne doit rester affiche apres la fin");
        assertEquals(0, playback.total());
    }
}
