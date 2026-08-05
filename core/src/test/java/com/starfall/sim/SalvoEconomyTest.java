package com.starfall.sim;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.starfall.game.Arena;
import com.starfall.game.ArenaSetup;
import com.starfall.game.Enemy;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Le <b>plafond effectif</b> de la file : combien de tuiles la meilleure ligne emploie-t-elle d'un
 * seul coup ?
 *
 * <h2>Ce que ce fichier est, et ce qu'il n'est pas</h2>
 *
 * <p>Ce n'est <b>pas</b> un garde-fou : il n'énonce aucune propriété souhaitable. C'est un
 * <b>constat figé</b> — une mesure qu'on refuse de laisser vieillir dans un paragraphe de tableau de
 * bord, où personne ne verrait qu'elle a cessé d'être vraie.
 *
 * <p>Et c'est un <b>fil-piège</b> : il doit virer au rouge le jour où l'économie de la file change.
 * C'est le seul test du projet dont l'échec sera une bonne nouvelle.
 *
 * <h2>Ce qu'il a corrigé</h2>
 *
 * <p>Le tableau de bord affirmait que la file de cinq ne peut pas être remplie parce qu'on
 * <i>meurt</i> avant : « sur douze configurations largeur × vague, six meurent avant la quatrième
 * tuile ». C'est faux, et la mesure qui le disait chargeait naïvement. Une recherche dont l'objectif
 * est la profondeur remplit la file dans <b>les douze</b> configurations, pour <b>0 à 4 points de
 * vie sur 8</b> et 5 à 8 tours — gratuitement dans trois cas sur douze.
 *
 * <p>Le vrai obstacle est ailleurs, et il est économique. À budget de tours égal, remplir ne
 * rapporte pas : cinq tours passés à charger sont cinq tours sans frapper, et pendant ce temps deux
 * à quatre petites salves font davantage. Mesuré sur les douze configurations, la meilleure ligne
 * n'emploie <b>jamais plus de trois tuiles</b> d'un coup.
 *
 * <p>La distinction n'est pas académique : elle change entièrement la réponse. « On meurt en
 * chargeant » appelle une garde ou moins de pression ; « charger ne rapporte pas » appelle une
 * récompense qui croît avec la taille de la salve, ou une file plus courte qui promette ce qu'elle
 * tient. On aurait corrigé le mauvais problème.
 */
class SalvoEconomyTest {

    /**
     * Six tours : le coût d'une salve pleine sous l'économie actuelle — cinq tours de chargement et
     * un pour lâcher. C'est le budget qui rend la comparaison honnête, puisque c'est exactement ce
     * que la file de cinq demande.
     */
    private static final int BUDGET = 6;

    private static final int BEAM = 400;

    /**
     * Borne de gestes, et elle n'est pas cosmétique : <b>reprendre</b> une tuile et poser une tuile
     * <b>Free-Play</b> ne consomment aucun tour. Sans borne, une ligne peut donc s'allonger
     * indéfiniment sans jamais épuiser son budget — la recherche partait pour ne plus s'arrêter.
     */
    private static final int MAX_MOVES = 24;

    /** Le plafond constaté, sur les douze configurations mesurées. */
    private static final int OBSERVED_CEILING = 3;

    private record Line(List<String> moves, int biggestSalvo) { }

    private record Result(int removed, int heroHealth, int biggestSalvo, List<String> moves) { }

    private static int enemyHealth(Arena arena) {
        return arena.enemies().stream().mapToInt(Enemy::health).sum();
    }

    /** Une vague passée vaut mieux qu'un ennemi entamé, et le barème le dit plutôt que le sous-entendre. */
    private static int removedBy(Arena arena, int startingEnemyHealth) {
        return startingEnemyHealth - enemyHealth(arena) + (arena.wave() - 1) * 100;
    }

    private static long value(Arena arena, int startingEnemyHealth) {
        if (arena.isDefeat()) {
            return Long.MIN_VALUE / 4;
        }
        return removedBy(arena, startingEnemyHealth) * 1_000_000L
                + arena.hero().health() * 1_000L - arena.turnsTaken();
    }

    /**
     * La signature porte tout ce qui décide de la suite — points de vie, regard, vague, tour, file,
     * et chaque ennemi avec sa position, son archétype, ses points de vie et son intention.
     *
     * <p>Elle est complète à dessein : une signature qui jette l'intention annoncée confond deux
     * états que le joueur lit différemment, et le projet s'est déjà fait prendre — 781 collisions
     * mesurées sur une signature qui ne gardait que la première lettre de chaque occupant.
     */
    private static String signature(Arena arena) {
        StringBuilder state = new StringBuilder();
        state.append(arena.hero().health()).append('|').append(arena.hero().facing())
                .append('|').append(arena.wave()).append('|').append(arena.turnsTaken())
                .append('|').append(arena.queue().fromOldest());
        for (Enemy enemy : arena.enemies()) {
            state.append(';').append(arena.grid().indexOf(enemy)).append(',').append(enemy.kind())
                    .append(',').append(enemy.health()).append(',').append(enemy.intention());
        }
        return state.toString();
    }

    /** La meilleure ligne trouvée dans le budget, sans rien imposer sur la façon de jouer. */
    private static Result best(int width, int startWave) {
        Arena start = ArenaSetup.trainingArena(width, startWave);
        int startingEnemyHealth = enemyHealth(start);

        List<Line> beam = new ArrayList<>();
        beam.add(new Line(List.of(), 0));
        Result champion = new Result(0, start.hero().health(), 0, List.of());

        // Une seule mémoire pour toute la recherche, et non une par étage : les gestes gratuits
        // ramènent aux mêmes états d'un étage à l'autre, si bien qu'une mémoire par étage ne les
        // reconnaît jamais.
        Set<String> seen = new LinkedHashSet<>();
        for (int move = 0; move < MAX_MOVES; move++) {
            List<Line> next = new ArrayList<>();
            for (Line line : beam) {
                Arena arena = Playout.replay(width, startWave, line.moves());
                if (arena.isOver() || arena.turnsTaken() >= BUDGET) {
                    continue;
                }
                for (Move candidate : Move.legal(arena)) {
                    // La taille d'une salve se lit AVANT de la lâcher : après, la file est vide.
                    int salvo = candidate.label().equals("salve")
                            ? Math.max(line.biggestSalvo(), arena.queue().size())
                            : line.biggestSalvo();
                    List<String> extended = new ArrayList<>(line.moves());
                    extended.add(candidate.label());
                    Arena after = Playout.replay(width, startWave, extended);
                    if (after.isDefeat() || after.turnsTaken() > BUDGET) {
                        continue;
                    }
                    if (!seen.add(signature(after) + '|' + salvo)) {
                        continue;
                    }
                    next.add(new Line(extended, salvo));

                    int removed = removedBy(after, startingEnemyHealth);
                    if (removed > champion.removed()
                            || (removed == champion.removed()
                                && after.hero().health() > champion.heroHealth())) {
                        champion = new Result(removed, after.hero().health(), salvo, extended);
                    }
                }
            }
            if (next.isEmpty()) {
                break;
            }
            next.sort((first, second) -> Long.compare(
                    value(Playout.replay(width, startWave, second.moves()), startingEnemyHealth),
                    value(Playout.replay(width, startWave, first.moves()), startingEnemyHealth)));
            beam = next.size() > BEAM ? new ArrayList<>(next.subList(0, BEAM)) : next;
        }
        return champion;
    }

    @Test
    @DisplayName("Le plafond effectif de la file reste celui qui a été mesuré")
    void theEffectiveQueueCeilingIsStillWhatWasMeasured() {
        Result result = best(9, 1);

        assertTrue(result.removed() > 0,
                "la meilleure ligne ne fait rien du tout en " + BUDGET + " tours :"
                        + " ce n'est plus une mesure d'economie mais un jeu casse");

        assertTrue(result.biggestSalvo() <= OBSERVED_CEILING,
                "la meilleure ligne emploie desormais une salve de " + result.biggestSalvo()
                        + " tuiles, la ou le constat fige en donnait au plus " + OBSERVED_CEILING
                        + "." + System.lineSeparator()
                        + "  Si l'economie de la file vient d'etre changee, C'EST LA BONNE NOUVELLE"
                        + " attendue : relever OBSERVED_CEILING et remesurer les douze"
                        + " configurations." + System.lineSeparator()
                        + "  Ligne trouvee : " + result.moves());
    }
}
