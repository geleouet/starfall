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
     * @param grid       plateau
     * @param enemy      ennemi qui décide
     * @param enemyCell  sa case
     * @param heroCell   case du héros
     * @param reserved   cases qu'une intention déjà annoncée réclame, et où personne ne doit se poser
     * @param phaseIndex numéro de la phase <b>à venir</b> : le souverain y lit son rythme
     */
    static Intention decide(Grid grid, Enemy enemy, int enemyCell, int heroCell,
                            boolean[] reserved, int phaseIndex) {
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
        if (kind.summons() > 0) {
            return decideSovereign(grid, enemy, enemyCell, heroCell, toward, distance, reserved,
                    phaseIndex);
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
     * Le souverain : trois réponses au lieu d'une, et c'est la distance qui choisit.
     *
     * <p>Au contact il frappe, une fois, pour un point : c'est sa forme la moins dangereuse. Dès
     * qu'on s'écarte, il alterne — une phase il <b>invoque</b> derrière le héros, la suivante il
     * <b>fonce</b> et repousse. Contre lui, s'éloigner n'est donc pas se mettre à l'abri : c'est
     * lui laisser remplir le plateau, puis se faire ramener au contact.
     *
     * <p>Le rythme n'est pas un ornement, c'est ce qui rend la rencontre jouable. La première
     * version laissait la seule distance décider — invocation au loin, ruée à deux cases — et comme
     * la ruée <em>repousse</em> le héros à deux cases, elle recréait exactement les conditions
     * d'une nouvelle ruée : le souverain fonçait à chaque tour, n'invoquait jamais, et sa mécanique
     * vedette était injouable. C'est le défaut que le jalon précédent avait payé sur la table des
     * vagues, sous une autre forme.
     *
     * <p>Quand il ne lui reste plus d'invocation, il ne fait plus que foncer : le compte à rebours
     * est aussi ce qui rend la fin de la rencontre lisible. Un boss qui annoncerait une invocation
     * impossible mentirait, et l'annonce est un engagement.
     */
    private static Intention decideSovereign(Grid grid, Enemy enemy, int enemyCell, int heroCell,
                                             Direction toward, int distance, boolean[] reserved,
                                             int phaseIndex) {
        if (distance <= effectiveRange(enemy)) {
            return Intention.attack(heroCell);
        }
        // Une phase sur deux il invoque, l'autre il fonce. Le rythme est la seule chose qui rende
        // la rencontre jouable : la premiere version faisait dependre son choix de la seule
        // distance, et comme la ruee repousse le heros a deux cases, elle recreait exactement les
        // conditions d'une nouvelle ruee. Le souverain fonçait a chaque tour, n'invoquait jamais,
        // et sa mecanique vedette etait injouable - le defaut que M7 avait paye sur les vagues.
        if (enemy.summonsLeft() > 0 && phaseIndex % 2 == 1) {
            int cell = summonCell(grid, heroCell, toward, reserved);
            if (cell >= 0) {
                return Intention.summon(cell);
            }
        }
        if (hasClearLine(grid, enemyCell, heroCell)) {
            return Intention.rush(heroCell);
        }
        return approach(grid, enemy, enemyCell, heroCell, toward, reserved);
    }

    /**
     * Où l'invocation apparaîtra : sur une case <b>adjacente au héros</b>, de préférence derrière
     * lui.
     *
     * <p>L'adjacence n'est pas une préférence esthétique, c'est <b>l'invariant</b> qui rend la
     * mécanique jouable. Une invocation est annoncée un tour à l'avance pour qu'on puisse la
     * refuser en occupant la case ; si la case n'est pas à un geste du héros, l'annonce n'est plus
     * une décision, c'est un compte à rebours.
     *
     * <p>La première version se rabattait sur la case <em>derrière le souverain</em> quand celle
     * derrière le héros était prise. Mesuré : <b>56 % des invocations de la vague livrée</b>
     * tombaient là, et 57 % des annonces étaient irréfusables — le boss en travers du chemin. Pire,
     * les sbires y naissaient coincés derrière lui : <b>47 % d'entre eux annonçaient « attend »</b>,
     * dont 93 % pour cette raison. Le souverain ne prenait pas en tenaille, il fabriquait une file
     * d'attente. Le joueur payait une invocation qu'il ne pouvait pas refuser pour un ennemi qui ne
     * faisait rien.
     *
     * <p>Faute de case adjacente libre, on n'invoque pas du tout et le souverain fonce. Renoncer
     * vaut mieux qu'annoncer ce qui ne se conteste pas.
     *
     * @return la case, ou {@code -1} si aucune ne convient
     */
    private static int summonCell(Grid grid, int heroCell, Direction toward, boolean[] reserved) {
        // Derrière le héros d'abord : c'est la tenaille, et c'est le côté qui l'oblige à tourner
        // le dos au souverain pour la refuser.
        int behind = heroCell + toward.step();
        if (grid.isFree(behind) && !isReserved(reserved, behind)) {
            return behind;
        }
        // Sinon devant lui, entre le héros et le souverain : le sbire y bouche la ruée du boss,
        // ce qui est un autre marché, mais il reste à un geste.
        int front = heroCell - toward.step();
        if (grid.isFree(front) && !isReserved(reserved, front)) {
            return front;
        }
        return -1;
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
