package com.starfall.sim;

import com.starfall.game.Arena;
import com.starfall.game.ArenaSetup;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Une partie jouée par une machine, du début à la fin.
 *
 * <h2>Pourquoi cet outil existe</h2>
 *
 * <p>Le jalon d'équilibrage est le seul où l'on ne peut rien prouver en lisant le code : un nombre
 * n'est ni juste ni faux, il est trop grand ou trop petit <em>pour quelqu'un qui joue</em>. Tant
 * qu'on n'a personne qui joue, équilibrer c'est deviner — et les reviews précédentes ont montré ce
 * que valent les impressions : la tranche a été mesurée à zéro victoire sur trois cents parties
 * alors que je la croyais serrée.
 *
 * <p>L'outil est donc du code livré, testé, et atteignable depuis la ligne de commande
 * ({@code --simulate N}) : un relecteur peut refaire mes mesures au lieu de me croire.
 *
 * <h2>Rejouer plutôt que copier</h2>
 *
 * <p>Une arène ne sait pas se dupliquer, et on ne le lui apprendra pas pour les besoins d'un outil
 * de mesure : un instantané d'état est exactement le genre de chose qui se désynchronise, et une
 * capacité ajoutée par commodité finit toujours par servir ailleurs. Essayer un geste consiste donc
 * à <b>rejouer la partie depuis son début</b>. C'est quadratique et parfaitement supportable : une
 * partie fait quelques dizaines de gestes.
 *
 * <p>L'histoire est faite de <b>libellés</b> et non d'indices. Un libellé est stable — « poser
 * estoc » désigne le même geste quel que soit le nombre de gestes légaux du moment — là où un
 * indice change de sens dès qu'une tuile entre en recharge. C'est aussi une trace lisible : une
 * partie simulée se relit.
 */
public final class Playout {

    /**
     * Ce qu'une partie a produit.
     *
     * @param won         vrai si toutes les vagues sont tombées
     * @param turns       tours consommés — la vraie monnaie du jeu
     * @param actions     gestes joués, gratuits compris
     * @param health      points de vie restants ; à la victoire, c'est la marge
     * @param waveReached dernière vague atteinte
     */
    public record Outcome(boolean won, int turns, int actions, int health, int waveReached) {
    }

    /** Au-delà, on considère la partie enlisée : aucune partie honnête ne dure autant. */
    public static final int MAX_ACTIONS = 400;

    /**
     * Gestes gratuits d'affilée au-delà desquels on force la machine à agir.
     *
     * <p>Il a fallu l'ajouter, et ce que ça révèle vaut mieux que le correctif : <b>rien dans le jeu
     * n'oblige le joueur à consommer un tour</b>. Poser et reprendre une tuile sont gratuits, les
     * ennemis n'avancent que sur un tour consommé, donc ne rien faire est <em>strictement sûr</em>,
     * indéfiniment. La politique réfléchie l'a trouvé toute seule dès la première mesure : elle a
     * passé les quatre cents gestes autorisés à remuer sa file sans jamais jouer un tour, dans
     * <b>100 parties sur 100</b>.
     *
     * <p>Ce n'est pas un défaut de l'instrument, c'est une propriété du jeu — un humain ne s'enlise
     * pas parce qu'il veut gagner, pas parce que les règles l'en empêchent. Cinq permet de remplir
     * une file de cinq ; au-delà, la machine doit agir.
     */
    public static final int MAX_IDLE_MOVES = 5;

    private Playout() {
    }

    public static Outcome play(Policy policy, int gridWidth, int startWave, long seed) {
        Random random = new Random(seed);
        List<String> history = new ArrayList<>();
        Arena arena = ArenaSetup.trainingArena(gridWidth, startWave);
        int idle = 0;

        while (!arena.isOver() && history.size() < MAX_ACTIONS) {
            List<String> soFar = List.copyOf(history);
            List<Move> candidates = candidates(arena, idle, gridWidth, startWave, soFar);
            if (candidates.isEmpty()) {
                break;
            }
            Policy.Trial trial = index -> {
                Arena copy = replay(gridWidth, startWave, soFar);
                apply(copy, candidates.get(index).label());
                return copy;
            };

            Move move = candidates.get(policy.choose(arena, candidates, trial, random));
            int turnsBefore = arena.turnsTaken();
            move.applyTo(arena);
            history.add(move.label());
            // On compte les gestes qui n'ont PAS fait avancer l'horloge, en le constatant plutôt
            // qu'en le prédisant : une tuile qui rate ne coûte pas de tour non plus, et un pas
            // bloqué non plus. Prédire aurait demandé de redire la règle une deuxième fois.
            idle = arena.turnsTaken() > turnsBefore ? 0 : idle + 1;
        }

        return new Outcome(arena.isVictory(), arena.turnsTaken(), history.size(),
                arena.hero().health(), arena.wave());
    }

    /**
     * Gestes proposés à la politique : tous, sauf quand la machine tourne en rond.
     *
     * <p>Le filtre demande « ce geste fait-il avancer l'horloge ? » en l'<b>essayant</b>, et pas en
     * regardant l'étiquette du geste. La version qui se fiait à l'étiquette a été battue à plate
     * couture : la politique s'est mise à répéter un <em>pas bloqué</em>, qui n'est ni une pose ni
     * une exécution gratuite mais ne consomme pas de tour non plus. Elle a trouvé le seul geste que
     * mon filtre laissait passer et qui ne coûtait rien, et elle l'a joué quatre cents fois.
     *
     * <p>Deux fois de suite, la machine a exploité une faille du filtre plutôt que de jouer. C'est
     * la meilleure démonstration de ce que vaut cet instrument : elle cherche les trous, sans idée
     * préconçue sur ce que le jeu est censé être.
     */
    private static List<Move> candidates(Arena arena, int idle, int gridWidth, int startWave,
                                         List<String> history) {
        List<Move> moves = Move.legal(arena);
        if (idle < MAX_IDLE_MOVES) {
            return moves;
        }
        List<Move> acting = new ArrayList<>();
        for (Move move : moves) {
            Arena copy = replay(gridWidth, startWave, history);
            int before = copy.turnsTaken();
            apply(copy, move.label());
            if (copy.turnsTaken() > before) {
                acting.add(move);
            }
        }
        return acting.isEmpty() ? moves : acting;
    }

    /** Rejoue une partie en réappliquant une histoire de gestes, désignés par leur libellé. */
    public static Arena replay(int gridWidth, int startWave, List<String> history) {
        Arena arena = ArenaSetup.trainingArena(gridWidth, startWave);
        for (String label : history) {
            apply(arena, label);
        }
        return arena;
    }

    private static void apply(Arena arena, String label) {
        for (Move move : Move.legal(arena)) {
            if (move.label().equals(label)) {
                move.applyTo(arena);
                return;
            }
        }
        throw new IllegalStateException("geste « " + label + " » introuvable au rejeu");
    }
}
