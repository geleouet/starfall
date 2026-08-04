package com.starfall.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.starfall.game.Arena;
import com.starfall.game.ArenaSetup;
import com.starfall.game.Grid;
import com.starfall.game.Tile;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * L'instrument de mesure doit être aussi fiable que ce qu'il mesure.
 *
 * <p>Un bilan d'équilibrage faux est pire qu'aucun bilan : il donne des chiffres, donc il inspire
 * confiance, donc on règle le jeu dessus. Ces tests portent sur l'instrument lui-même — l'énumération
 * des gestes, la reproductibilité du rejeu, l'absence d'enlisement — et non sur les valeurs qu'il
 * produit, qui sont l'affaire du réglage.
 */
class PlayoutTest {

    /**
     * Le rejeu est la fondation de tout : essayer un geste consiste à rejouer la partie depuis son
     * début. S'il n'était pas déterministe, chaque évaluation porterait sur un autre jeu.
     */
    @Test
    @DisplayName("Rejouer la même histoire redonne exactement le même état")
    void replayingTheSameHistoryGivesTheSameState() {
        for (int width : new int[]{Grid.MIN_WIDTH, 9, Grid.MAX_WIDTH}) {
            List<String> history = new ArrayList<>(List.of(
                    "poser " + Tile.THRUST.label(),
                    "poser " + Tile.STRIKE.label(),
                    "pas droite",
                    "salve",
                    "pas gauche"));

            String reference = signature(Playout.replay(width, 1, history));
            for (int repeat = 0; repeat < 4; repeat++) {
                assertEquals(reference, signature(Playout.replay(width, 1, history)),
                        "grille de " + width);
            }
        }
    }

    /**
     * Chaque geste énuméré doit être réellement jouable. Proposer à une politique des gestes que le
     * jeu refuse ferait compter des tours qui n'existent pas.
     */
    @Test
    @DisplayName("Tout geste énuméré est accepté par le jeu")
    void everyEnumeratedMoveIsAccepted() {
        for (int seed = 0; seed < 60; seed++) {
            Arena arena = ArenaSetup.trainingArena(9, 1);
            List<String> history = new ArrayList<>();
            java.util.Random random = new java.util.Random(seed);

            for (int step = 0; step < 25 && !arena.isOver(); step++) {
                List<Move> moves = Move.legal(arena);
                assertTrue(!moves.isEmpty(), "aucun geste legal, graine " + seed);

                Move move = moves.get(random.nextInt(moves.size()));
                // Le rejeu retrouve le geste par son libellé : s'il ne le retrouvait pas, toute
                // évaluation partirait d'un état faux sans que rien ne le signale.
                history.add(move.label());
                Playout.replay(9, 1, history);
                move.applyTo(arena);
            }
        }
    }

    /**
     * Aucune partie ne doit s'enliser <b>par la faute de l'instrument</b>.
     *
     * <p>C'est le test qui a le plus servi. La politique réfléchie a trouvé deux fois de suite un
     * moyen de ne jamais faire avancer l'horloge : d'abord en remuant sa file indéfiniment — poser
     * et reprendre sont gratuits — puis, une fois cela interdit, en répétant un <b>pas bloqué</b>,
     * qui n'est ni une pose ni une exécution gratuite mais ne coûte pas de tour non plus. Rien dans
     * les règles du jeu n'oblige le joueur à agir : c'est une propriété du jeu, pas un défaut de
     * l'instrument, mais la mesure doit en tenir compte.
     */
    @Test
    @DisplayName("Une partie mesurée finit toujours par consommer des tours")
    void ameasuredGameAlwaysBurnsTurns() {
        for (Policy policy : List.of(Policy.random(), Policy.direct(), Policy.thoughtful())) {
            for (int seed = 0; seed < 8; seed++) {
                Playout.Outcome outcome = Playout.play(policy, 9, 1, seed);
                assertTrue(outcome.turns() > 0,
                        policy.name() + " graine " + seed + " : aucun tour consomme");
                // Une partie qui atteint le plafond de gestes sans avoir consommé au moins un tour
                // tous les six gestes tourne en rond.
                assertTrue(outcome.turns() * 6 >= outcome.actions(),
                        policy.name() + " graine " + seed + " : " + outcome.actions()
                                + " gestes pour seulement " + outcome.turns() + " tours");
            }
        }
    }

    /**
     * Le plancher : si le hasard s'en sortait, le jeu ne demanderait rien. C'est la seule assertion
     * d'équilibre que cet instrument porte lui-même — le reste est du réglage, qui bouge.
     */
    @Test
    @DisplayName("Le hasard ne gagne jamais : il y a bien un jeu")
    void randomPlayNeverWins() {
        BalanceReport report = BalanceReport.measure(Policy.random(), 9, 1, 60);
        assertEquals(0, report.wins(), "le hasard gagne : le jeu ne demande rien");
        assertTrue(report.averageTurns() > 3,
                "le hasard meurt trop vite pour que la mesure dise quoi que ce soit : "
                        + report.averageTurns());
    }

    /**
     * Une partie enlisée n'est pas une partie, et le bilan ne doit pas la compter comme telle.
     *
     * <p>La review du jalon d'équilibrage l'a montré net : le garde-fou anti-enlisement
     * n'empêche pas de tourner en rond, il le <em>cadence</em> — cinq gestes gratuits, un geste
     * forcé, jusqu'au plafond. Une politique qui s'enlisait systématiquement affichait donc
     * « 78 tours survécus », ce qui se lit comme de la résistance alors que c'est le
     * garde-fou qu'on mesure. Sur la grille la plus large, 92 % des « parties » de la politique de
     * plafond n'en étaient pas.
     */
    @Test
    @DisplayName("Les parties enlisées sortent des moyennes et sont annoncées")
    void stalledGamesLeaveTheAveragesAndAreAnnounced() {
        BalanceReport report = BalanceReport.measure(Policy.thoughtful(), 15, 1, 12);

        assertTrue(report.stalled() >= 0 && report.stalled() <= report.games());
        assertTrue(report.wins() <= report.games() - report.stalled(),
                "plus de victoires que de parties reellement jouees : "
                        + report.wins() + " pour " + (report.games() - report.stalled()));
        if (report.stalled() > 0) {
            assertTrue(report.line().contains("ENLISÉES"),
                    "le bilan tait " + report.stalled() + " partie(s) enlisee(s) : " + report.line());
        }
        // « 0,0 » quand personne ne gagne se lirait comme « on gagne a zero point de vie ».
        if (report.wins() == 0) {
            assertTrue(report.line().contains("s.o."),
                    "une marge de vie sans objet doit se dire, pas s'ecrire zero : " + report.line());
        }
    }

    @Test
    @DisplayName("Le bilan se lit et se reproduit")
    void theReportIsReadableAndReproducible() {
        BalanceReport first = BalanceReport.measure(Policy.direct(), 9, 1, 20);
        BalanceReport again = BalanceReport.measure(Policy.direct(), 9, 1, 20);

        assertEquals(first, again, "mêmes graines, mêmes chiffres");
        assertTrue(first.line().contains("grille 9"), first.line());
        assertTrue(first.winRate() >= 0 && first.winRate() <= 1);
    }

    private static String signature(Arena arena) {
        StringBuilder state = new StringBuilder();
        for (int cell = 0; cell < arena.grid().width(); cell++) {
            var occupant = arena.grid().occupantAt(cell);
            state.append(occupant == null ? '.'
                    : occupant == arena.hero() ? 'H' : occupant.label().charAt(0));
        }
        return state.append('|').append(arena.turnsTaken())
                .append('|').append(arena.hero().health())
                .append('|').append(arena.wave())
                .append('|').append(arena.queue().size()).toString();
    }
}
