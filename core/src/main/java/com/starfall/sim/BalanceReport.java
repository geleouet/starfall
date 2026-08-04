package com.starfall.sim;

import com.starfall.game.Arena;
import com.starfall.game.Grid;

import java.util.ArrayList;
import java.util.List;

/**
 * Le bilan chiffré d'une politique sur un lot de parties.
 *
 * <p>C'est la sortie de l'instrument : ce qu'on lit pour décider si un nombre du jeu est trop grand
 * ou trop petit. Elle est reproductible — mêmes graines, mêmes chiffres — donc un relecteur peut la
 * refaire au lieu de me croire.
 */
public record BalanceReport(String policy, int gridWidth, int startWave, int games,
                            int wins, double averageTurns, double averageHealthOnWin,
                            double averageWaveReached, int stalled) {

    /*
     * Les parties enlisées sont exclues des moyennes, et la review a montré pourquoi : le garde-fou
     * anti-enlisement n'empêche pas de tourner en rond, il le cadence — cinq gestes gratuits, un
     * geste forcé, jusqu'au plafond. Une politique qui s'enlise systématiquement affichait donc
     * « 78 tours survécus », ce qui se lit comme de la résistance alors que c'est le garde-fou qu'on
     * mesure. Sur la grille 15, 92 % des « parties » de la politique de plafond n'étaient pas des
     * parties.
     */

    /** Proportion de parties gagnées, de 0 à 1. */
    public double winRate() {
        return games == 0 ? 0 : wins / (double) games;
    }

    public static BalanceReport measure(Policy policy, int gridWidth, int startWave, int games) {
        int wins = 0;
        int stalled = 0;
        long turns = 0;
        long healthOnWin = 0;
        long waves = 0;

        int played = 0;
        for (int seed = 0; seed < games; seed++) {
            Playout.Outcome outcome = Playout.play(policy, gridWidth, startWave, seed);
            if (outcome.actions() >= Playout.MAX_ACTIONS) {
                stalled++;
                continue; // enlisée : elle mesure le garde-fou, pas le jeu
            }
            played++;
            turns += outcome.turns();
            waves += outcome.waveReached();
            if (outcome.won()) {
                wins++;
                healthOnWin += outcome.health();
            }
        }

        return new BalanceReport(policy.name(), gridWidth, startWave, games, wins,
                played == 0 ? 0 : turns / (double) played,
                wins == 0 ? -1 : healthOnWin / (double) wins,
                played == 0 ? 0 : waves / (double) played,
                stalled);
    }

    /** Une ligne de tableau, en français, lisible dans un terminal. */
    public String line() {
        return String.format("  %-9s grille %-3d vague %d : %3d/%3d gagnées (%3.0f %%)"
                        + "  tours %5.1f  marge de vie %s  vague atteinte %4.2f%s",
                policy, gridWidth, startWave, wins, games, winRate() * 100,
                averageTurns,
                // « 0,0 » quand personne ne gagne se lit comme « on gagne à zéro point de vie ».
                averageHealthOnWin < 0 ? " s.o." : String.format("%4.1f", averageHealthOnWin),
                averageWaveReached,
                stalled > 0 ? "  ENLISÉES " + stalled + "/" + games + " (hors moyennes)" : "");
    }

    /**
     * Le bilan complet : les trois politiques, la tranche entière et la rencontre finale seule, sur
     * les trois largeurs qui comptent.
     *
     * <p>La grille la plus étroite et la plus large sont là parce que le jeu prétend se jouer sur
     * les deux, et parce que la review de M7 a montré qu'une largeur pouvait être secrètement plus
     * facile que l'autre.
     */
    public static List<BalanceReport> full(int games) {
        List<BalanceReport> reports = new ArrayList<>();
        List<Policy> policies = List.of(Policy.random(), Policy.direct(), Policy.thoughtful());
        for (Policy policy : policies) {
            for (int width : new int[]{Grid.MIN_WIDTH, 9, Grid.MAX_WIDTH}) {
                reports.add(measure(policy, width, 1, games));
            }
            reports.add(measure(policy, 11, Arena.WAVE_COUNT, games));
        }
        return reports;
    }
}
