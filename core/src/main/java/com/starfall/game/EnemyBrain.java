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
        return decide(grid, enemy, enemyCell, heroCell, new boolean[0]);
    }

    /**
     * @param reserved cases qu'une charge déjà annoncée traversera, et où personne ne doit se poser
     */
    static Intention decide(Grid grid, Enemy enemy, int enemyCell, int heroCell, boolean[] reserved) {
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
            return grid.isFree(back) && !isReserved(reserved, back)
                    ? Intention.advance(back)
                    : Intention.attack(heroCell);
        }
        if (distance <= effectiveRange(enemy) && hasClearLine(grid, enemyCell, heroCell)) {
            return Intention.attack(heroCell);
        }
        if (kind.windsUp() && distance >= 2 && hasClearLine(grid, enemyCell, heroCell)) {
            return Intention.of(Intention.Kind.WIND_UP);
        }
        return approach(grid, enemy, enemyCell, heroCell, toward, reserved);
    }

    /**
     * Vrai si une charge annoncée traversera cette case.
     *
     * <p>Une intention est un <b>engagement</b>, donc les autres ennemis doivent le respecter : un
     * allié qui se posait dans le couloir d'une charge l'interceptait, et le coup annoncé au joueur
     * ne tombait jamais. Le télégraphe sur-promettait — le joueur pouvait dépenser un tour pour
     * esquiver un coup qui n'allait pas partir.
     */
    private static boolean isReserved(boolean[] reserved, int cell) {
        return cell >= 0 && cell < reserved.length && reserved[cell];
    }

    /**
     * Choix du déplacement, traits compris.
     *
     * <p>Un fonceur comble toute la distance d'un coup ; un rapide avance de deux cases au lieu
     * d'une. Les deux se replient sur un pas simple si le chemin n'est pas libre — un ennemi qui
     * traverserait ses camarades serait illisible.
     */
    private static Intention approach(Grid grid, Enemy enemy, int enemyCell, int heroCell,
                                      Direction toward, boolean[] reserved) {
        int contactCell = heroCell - toward.step() * effectiveRange(enemy);
        if (enemy.has(Trait.FONCEUR) && contactCell != enemyCell
                && !isReserved(reserved, contactCell)
                && isPathClear(grid, enemyCell, contactCell)) {
            return Intention.advance(contactCell);
        }

        int steps = enemy.has(Trait.RAPIDE) ? 2 : 1;
        for (int distance = steps; distance >= 1; distance--) {
            int destination = enemyCell + toward.step() * distance;
            if (destination != heroCell && !isReserved(reserved, destination)
                    && isPathClear(grid, enemyCell, destination)) {
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
