package com.starfall.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.starfall.scene.HudText;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Le télégraphe peut mentir dans les deux sens, et on n'avait couvert qu'un seul.
 *
 * <p>Les jalons précédents ont fait corriger le modèle pour qu'il ne <b>sur-promette</b> jamais : un
 * coup annoncé doit tomber. Personne n'avait regardé l'autre sens. Le rendu dessinait au plus trois
 * barres de menace et s'arrêtait net au-delà, si bien que quatre coups annoncés s'affichaient
 * « trois ». La vague 3 — sabreur rapide, archer agressif, lancier fonceur, colosse — peut en
 * produire cinq, et le héros n'a que cinq points de vie : « trois » quand il en tombe cinq, c'est la
 * différence entre rester et mourir.
 *
 * <p>Ce test vit dans le paquet du modèle et non dans celui de la scène, parce qu'il lui faut
 * construire un plateau à la main : {@code announceIntentions} n'est pas publique, et un tirage au
 * hasard n'atteint presque jamais quatre coups sur la même case — c'est justement pour cela que le
 * défaut avait survécu.
 */
class ThreatDisplayTest {

    /** Un plateau qui concentre quatre coups sur la case du héros. */
    private static Arena crowdedArena() {
        Arena arena = new Arena(9, 4);
        arena.grid().place(3, new Enemy(EnemyKind.SABREUR, Trait.RAPIDE));
        arena.grid().place(5, new Enemy(EnemyKind.SABREUR, Trait.RAPIDE));
        arena.announceIntentions();
        return arena;
    }

    @Test
    @DisplayName("Autant de barres de menace que de coups annoncés, sans plafond silencieux")
    void everyAnnouncedBlowGetsItsOwnBar() {
        List<String> failures = new ArrayList<>();
        for (int blows = 1; blows <= 8; blows++) {
            int drawn = ArenaLayout.threatBarsDrawn(blows);
            if (drawn != blows) {
                failures.add(blows + " coups annonces, " + drawn + " barres dessinees");
            }
            int span = 2 + (blows - 1) * ArenaLayout.threatBarPitch(blows)
                    + ArenaLayout.threatBarWidth(blows);
            if (span > ArenaLayout.CELL_WIDTH) {
                failures.add(blows + " coups debordent de la case : " + span + " px pour "
                        + ArenaLayout.CELL_WIDTH);
            }
            if (ArenaLayout.threatBarWidth(blows) < 1) {
                failures.add(blows + " coups donnent des barres de largeur nulle");
            }
        }
        assertTrue(failures.isEmpty(), "les barres de menace mentent :\n  "
                + String.join("\n  ", failures));
    }

    @Test
    @DisplayName("Quatre coups sur une case sont bien dessinés quatre fois")
    void fourBlowsOnOneCellAreDrawnFourTimes() {
        Arena arena = crowdedArena();
        int blows = arena.threatCount(arena.heroCell());

        assertTrue(blows >= 4, "le montage doit concentrer au moins quatre coups, obtenu " + blows);
        assertEquals(blows, ArenaLayout.threatBarsDrawn(blows),
                "le rendu s'arreterait avant d'avoir tout dit");
    }

    /**
     * Le filet de sécurité : même si les barres venaient un jour à saturer, le bandeau écrit le
     * nombre. Une case fait vingt pixels de large et le texte n'a pas cette limite.
     */
    @Test
    @DisplayName("Le bandeau écrit le nombre exact de coups à venir")
    void theBannerStatesTheExactNumberOfIncomingBlows() {
        Arena arena = crowdedArena();
        int blows = arena.threatCount(arena.heroCell());

        assertTrue(HudText.banner(arena).contains("MENACE " + blows),
                "le bandeau doit annoncer « MENACE " + blows + " » : " + HudText.banner(arena));

        Arena calm = new Arena(9, 4);
        assertEquals(0, calm.threatCount(calm.heroCell()));
        assertTrue(!HudText.banner(calm).contains("MENACE"),
                "un plateau vide ne doit annoncer aucune menace : " + HudText.banner(calm));
    }
}
