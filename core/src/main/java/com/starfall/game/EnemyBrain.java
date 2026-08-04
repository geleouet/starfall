package com.starfall.game;

/**
 * Décide ce qu'un ennemi annoncera pour sa prochaine activation.
 *
 * <p>Séparé de l'exécution, et sans effet de bord sur la grille : une décision doit pouvoir être
 * rejouée et vérifiée sans faire avancer la partie. C'est aussi ce qui permet de tester le
 * télégraphe pour ce qu'il est — une <em>promesse</em> — indépendamment de sa tenue.
 */
final class EnemyBrain {

    private EnemyBrain() {
    }

    /**
     * @param grid      plateau
     * @param enemy     ennemi qui décide
     * @param enemyCell sa case
     * @param heroCell  case du héros
     */
    static Intention decide(Grid grid, Enemy enemy, int enemyCell, int heroCell) {
        Direction toward = Direction.towards(enemyCell, heroCell);
        if (toward == null) {
            // Le héros ne peut pas être sur la case de l'ennemi ; par prudence, on n'invente rien.
            return Intention.of(Intention.Kind.WAIT);
        }
        enemy.face(toward);

        int distance = Math.abs(heroCell - enemyCell);
        EnemyKind kind = enemy.kind();

        // Le lancier tient sa promesse : s'il a pris son élan, il charge, quoi qu'il arrive.
        if (enemy.isWindingUp()) {
            return Intention.charge(heroCell);
        }
        if (kind.retreatsWhenAdjacent() && distance == 1) {
            int back = enemyCell - toward.step();
            return grid.isFree(back) ? Intention.advance(back) : Intention.attack(heroCell);
        }
        if (distance <= effectiveRange(enemy) && hasClearLine(grid, enemyCell, heroCell)) {
            return Intention.attack(heroCell);
        }
        if (kind.windsUp() && distance >= 2 && hasClearLine(grid, enemyCell, heroCell)) {
            return Intention.of(Intention.Kind.WIND_UP);
        }
        return approach(grid, enemy, enemyCell, heroCell, toward);
    }

    /**
     * Choix du déplacement, traits compris.
     *
     * <p>Un fonceur comble toute la distance d'un coup ; un rapide avance de deux cases au lieu
     * d'une. Les deux se replient sur un pas simple si le chemin n'est pas libre — un ennemi qui
     * traverserait ses camarades serait illisible.
     */
    private static Intention approach(Grid grid, Enemy enemy, int enemyCell, int heroCell,
                                      Direction toward) {
        int contactCell = heroCell - toward.step() * effectiveRange(enemy);
        if (enemy.has(Trait.FONCEUR) && contactCell != enemyCell
                && isPathClear(grid, enemyCell, contactCell)) {
            return Intention.advance(contactCell);
        }

        int steps = enemy.has(Trait.RAPIDE) ? 2 : 1;
        for (int distance = steps; distance >= 1; distance--) {
            int destination = enemyCell + toward.step() * distance;
            if (destination != heroCell && isPathClear(grid, enemyCell, destination)) {
                return Intention.advance(destination);
            }
        }
        return Intention.of(Intention.Kind.WAIT);
    }

    /** Portée réellement atteinte, trait agressif compris. */
    static int effectiveRange(Enemy enemy) {
        return enemy.kind().range() + (enemy.has(Trait.AGRESSIF) ? 1 : 0);
    }

    /** Vrai si toutes les cases strictement entre les deux bornes sont libres, bornes comprises. */
    private static boolean isPathClear(Grid grid, int from, int to) {
        if (!grid.contains(to)) {
            return false;
        }
        int step = Integer.signum(to - from);
        for (int cell = from + step; cell != to + step; cell += step) {
            if (!grid.isFree(cell)) {
                return false;
            }
        }
        return true;
    }

    /** Vrai si rien ne s'interpose entre l'ennemi et sa cible — les tirs ne traversent personne. */
    private static boolean hasClearLine(Grid grid, int from, int to) {
        int step = Integer.signum(to - from);
        for (int cell = from + step; cell != to; cell += step) {
            if (!grid.isFree(cell)) {
                return false;
            }
        }
        return true;
    }
}
