package com.starfall.scene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.starfall.game.ActionResult;
import com.starfall.game.Arena;
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

    private static Arena.Beat beat(Tile tile, int cell) {
        // Le plateau du temps ne joue aucun role ici : ce fichier garde la MACHINE du deroule -
        // quand il court, dans quel ordre, quand il finit - et pas ce qu'elle donne a dessiner.
        return new Arena.Beat(tile, ActionResult.STRUCK, cell, List.of(), List.of());
    }

    @Test
    @DisplayName("Une action d'un seul temps ne se déroule pas")
    void aSingleBeatActionDoesNotUnfold() {
        Playback playback = new Playback();

        playback.start(List.of(beat(Tile.STRIKE, 3)), List.of());

        assertFalse(playback.isRunning(),
                "un seul temps n'a rien a egrener : derouler ajouterait de la latence sans"
                        + " ajouter de lecture");
        assertNull(playback.current());
        assertEquals(0, playback.step());
    }

    @Test
    @DisplayName("Une action vide ne se déroule pas non plus")
    void anEmptyActionDoesNotUnfoldEither() {
        Playback playback = new Playback();
        playback.start(List.of(), List.of());
        assertFalse(playback.isRunning());
    }

    @Test
    @DisplayName("Une salve montre ses temps dans l'ordre, un par durée")
    void aVolleyShowsItsBeatsInOrder() {
        Arena.Beat first = beat(Tile.STRIKE, 3);
        Arena.Beat second = beat(Tile.THRUST, 4);
        Arena.Beat third = beat(Tile.PUSH, 5);
        Playback playback = new Playback();

        playback.start(List.of(first, second, third), List.of());

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
            playback.start(List.of(beat(Tile.STRIKE, 1), beat(Tile.THRUST, 2),
                    beat(Tile.PUSH, 3), beat(Tile.DASH, 4), beat(Tile.PIVOT, 5)), List.of());

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
        playback.start(List.of(beat(Tile.STRIKE, 1), beat(Tile.THRUST, 2),
                beat(Tile.PUSH, 3)), List.of());

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
        playback.start(List.of(beat(Tile.STRIKE, 1), beat(Tile.THRUST, 2)), List.of());

        playback.advance(1000f);

        assertFalse(playback.isRunning());
        assertNull(playback.current(), "aucun temps ne doit rester affiche apres la fin");
        assertEquals(0, playback.total());
    }
}
