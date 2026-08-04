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

        for (int seed = 0; seed < games; seed++) {
            Playout.Outcome outcome = Playout.play(policy, gridWidth, startWave, seed);
            turns += outcome.turns();
            waves += outcome.waveReached();
            if (outcome.won()) {
                wins++;
                healthOnWin += outcome.health();
            }
            if (outcome.actions() >= Playout.MAX_ACTIONS) {
                stalled++;
            }
        }

        return new BalanceReport(policy.name(), gridWidth, startWave, games, wins,
                turns / (double) games,
                wins == 0 ? 0 : healthOnWin / (double) wins,
                waves / (double) games,
                stalled);
    }

    /** Une ligne de tableau, en français, lisible dans un terminal. */
    public String line() {
        return String.format("  %-9s grille %-3d vague %d : %3d/%3d gagnées (%3.0f %%)"
                        + "  tours %5.1f  marge de vie %4.1f  vague atteinte %4.2f%s",
                policy, gridWidth, startWave, wins, games, winRate() * 100,
                averageTurns, averageHealthOnWin, averageWaveReached,
                stalled > 0 ? "  ENLISÉES " + stalled : "");
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
