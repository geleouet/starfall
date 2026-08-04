package com.starfall.sim;

import com.starfall.game.Arena;
import com.starfall.game.Enemy;
import com.starfall.game.Tile;
import com.starfall.game.TilePreview;

import java.util.List;
import java.util.Random;

/**
 * Une façon de jouer, pour mesurer le jeu plutôt que de l'imaginer.
 *
 * <h2>Pourquoi trois politiques et pas une</h2>
 *
 * <p>Un seul chiffre ne dit rien. « 40 % de victoires » n'a de sens qu'accompagné de <em>qui</em>
 * gagne : si le hasard gagne, le jeu ne demande rien ; si seule une recherche gagne, il demande
 * trop. C'est l'<b>écart</b> entre les trois politiques qui décrit la difficulté, pas la valeur
 * absolue de l'une d'elles.
 *
 * <ul>
 *   <li><b>hasard</b> — le plancher. S'il gagne souvent, il n'y a pas de jeu ;</li>
 *   <li><b>direct</b> — frapper dès qu'on peut, sans jamais anticiper. Le joueur qui découvre ;</li>
 *   <li><b>réfléchi</b> — un tour d'anticipation sur tous les gestes, l'évaluation lisant les
 *       intentions annoncées. Le plafond raisonnable.</li>
 * </ul>
 *
 * <p>La politique réfléchie <em>doit</em> lire les intentions : c'est l'information que le jeu
 * s'échine à afficher depuis M6, et une mesure qui l'ignorerait décrirait un autre jeu que celui
 * qu'on livre.
 */
public interface Policy {

    String name();

    /**
     * Choisit un geste.
     *
     * @param arena état courant, à consulter mais jamais à modifier
     * @param moves gestes légaux, jamais vide
     * @param trial permet d'essayer un geste sur une partie rejouée, sans toucher à celle-ci
     */
    int choose(Arena arena, List<Move> moves, Trial trial, Random random);

    /** Essaie un geste sans conséquence : rend l'état qu'il produirait. */
    @FunctionalInterface
    interface Trial {
        Arena after(int moveIndex);
    }

    static Policy random() {
        return new Policy() {
            @Override
            public String name() {
                return "hasard";
            }

            @Override
            public int choose(Arena arena, List<Move> moves, Trial trial, Random random) {
                return random.nextInt(moves.size());
            }
        };
    }

    /**
     * Le joueur direct : il exécute ce qui porte, sinon il charge une frappe, sinon il avance.
     *
     * <p>Il ne rejoue rien et ne regarde pas plus loin que son propre geste — mais il consulte le
     * <b>préavis</b>, parce que c'est affiché à l'écran et qu'un joueur qui découvre le jeu le lit.
     * Ne pas le lui donner mesurerait un joueur aveugle, pas un joueur naïf.
     */
    static Policy direct() {
        return new Policy() {
            @Override
            public String name() {
                return "direct";
            }

            @Override
            public int choose(Arena arena, List<Move> moves, Trial trial, Random random) {
                TilePreview top = arena.previewTop();
                for (int i = 0; i < moves.size(); i++) {
                    if (moves.get(i).label().equals("salve") && top != null && top.connects()) {
                        return i;
                    }
                }
                // Rien qui porte : on charge la frappe la plus courte disponible.
                for (Tile tile : List.of(Tile.STRIKE, Tile.THRUST, Tile.PUSH)) {
                    for (int i = 0; i < moves.size(); i++) {
                        if (moves.get(i).label().equals("poser " + tile.label())) {
                            return i;
                        }
                    }
                }
                // Sinon on marche vers l'ennemi le plus proche.
                int target = nearestEnemy(arena);
                String wanted = "pas " + (target < arena.heroCell() ? "gauche" : "droite");
                for (int i = 0; i < moves.size(); i++) {
                    if (moves.get(i).label().equals(wanted)) {
                        return i;
                    }
                }
                return 0;
            }
        };
    }

    /**
     * Le joueur réfléchi : il essaie chaque geste et garde le meilleur état obtenu.
     *
     * <p>Un seul tour d'anticipation, et c'est délibéré. Deux tours coûteraient cent fois plus pour
     * mesurer un joueur que personne n'est. Ce qui rend cette politique forte n'est pas sa
     * profondeur mais son <b>évaluation</b>, qui voit ce que le joueur voit.
     */
    static Policy thoughtful() {
        return new Policy() {
            @Override
            public String name() {
                return "réfléchi";
            }

            @Override
            public int choose(Arena arena, List<Move> moves, Trial trial, Random random) {
                List<Integer> best = new java.util.ArrayList<>();
                int bestScore = Integer.MIN_VALUE;
                for (int i = 0; i < moves.size(); i++) {
                    int score = score(trial.after(i));
                    // Un geste gratuit qui n'améliore rien fait tourner la partie en rond : on le
                    // départage à la baisse pour que la politique finisse toujours par agir.
                    if (moves.get(i).free()) {
                        score -= 1;
                    }
                    if (score > bestScore) {
                        bestScore = score;
                        best.clear();
                        best.add(i);
                    } else if (score == bestScore) {
                        best.add(i);
                    }
                }
                // Les ex aequo se départagent au hasard, et ce n'est pas cosmétique : sans cela la
                // politique est parfaitement déterministe, la position de départ l'est aussi, et
                // « cent parties » ne mesurent qu'une seule partie recopiée cent fois. Le premier
                // bilan produit rendait exactement le même chiffre pour les cent graines.
                return best.get(random.nextInt(best.size()));
            }
        };
    }

    private static int nearestEnemy(Arena arena) {
        int best = arena.heroCell();
        int bestDistance = Integer.MAX_VALUE;
        for (Enemy enemy : arena.enemies()) {
            int cell = arena.grid().indexOf(enemy);
            int distance = Math.abs(cell - arena.heroCell());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = cell;
            }
        }
        return best;
    }

    /**
     * Évaluation d'une position, du point de vue du joueur.
     *
     * <p>Les poids encodent ce que le jeu <em>dit</em> qui compte, mais ils encodent surtout une
     * chose qu'il a fallu apprendre de l'instrument : <b>un joueur accepte d'échanger</b>. La
     * première version valait la vie du héros huit fois un point de vie ennemi, si bien que la
     * politique refusait tout échange — et comme rien n'oblige à consommer un tour, elle a
     * simplement cessé de jouer, dans cent parties sur cent. Une évaluation qui ne veut jamais
     * perdre un point de vie ne décrit personne.
     *
     * <p>Tuer vaut donc plus cher qu'encaisser, et finir une vague vaut beaucoup plus que les deux.
     * La pression annoncée sur la case du héros reste lourdement pénalisée : c'est l'information que
     * le jeu s'échine à afficher depuis M6, et une politique qui l'ignorerait mesurerait un autre
     * jeu.
     */
    static int score(Arena arena) {
        if (arena.isVictory()) {
            return 1_000_000 + arena.hero().health() * 1000 - arena.turnsTaken();
        }
        if (arena.isDefeat()) {
            return -1_000_000 + arena.turnsTaken();
        }
        int enemyHealth = 0;
        int adjacent = 0;
        for (Enemy enemy : arena.enemies()) {
            enemyHealth += enemy.health();
            if (Math.abs(arena.grid().indexOf(enemy) - arena.heroCell()) <= 1) {
                adjacent++;
            }
        }
        // Une tuile posée sur la file reste disponible : la compter comme perdue faisait fuir la
        // politique de son propre système d'actions.
        int available = arena.queue().size();
        for (Tile tile : arena.rack().tiles()) {
            if (arena.rack().isReady(tile)) {
                available++;
            }
        }
        // Un coup en joue, c'est-à-dire un sommet de file qui porterait. C'est le terme qui manquait
        // et sans lequel la politique ne tuait jamais : la récompense d'une frappe est à trois
        // gestes — poser, se placer, exécuter — et chacun des deux premiers dégradait le score. Elle
        // a donc joué quatre-vingts tours à esquiver sans jamais lever une arme, ce qui est
        // parfaitement rationnel et ne ressemble à aucun joueur.
        TilePreview aimed = arena.previewTop();
        int loaded = aimed != null && aimed.connects() ? 1 : 0;

        return arena.hero().health() * 1500
                + arena.wave() * 20000
                - enemyHealth * 2500
                - arena.threatCount(arena.heroCell()) * 1200
                - adjacent * 100
                + available * 60
                + loaded * 2000
                - arena.turnsTaken() * 5;
    }
}
