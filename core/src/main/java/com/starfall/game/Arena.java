package com.starfall.game;

import java.util.ArrayList;
import java.util.List;

/**
 * L'état d'un combat : une {@link Grid} et le {@link Hero} qui s'y trouve, plus les règles qui
 * relient les deux.
 *
 * <p>Toutes les actions du joueur passent par ici et renvoient un {@link ActionResult}. Aucune
 * n'est silencieuse : une action bloquée le dit, ce qui permettra à la file d'actions (M5) de
 * décider si un tour a été consommé, et à l'interface d'expliquer au joueur pourquoi rien n'a bougé.
 *
 * <h2>Règle d'orientation</h2>
 *
 * <p>Appuyer vers une direction que le héros ne regarde pas le fait <b>se retourner</b>, sans
 * avancer ; appuyer vers la direction qu'il regarde le fait avancer. L'orientation devient ainsi
 * une ressource tactique à part entière plutôt qu'un effet de bord du déplacement — c'est un écart
 * assumé par rapport à la référence, où l'on se retourne et avance dans le même geste, et il est
 * consigné comme tel dans le tableau de bord.
 *
 * <h2>Le tour, et ce qui le consomme</h2>
 *
 * <p>Un tour est la seule monnaie du jeu : c'est lui qui fait avancer les ennemis (M6) et recharger
 * les tuiles. Le consomment : un déplacement, un demi-tour, un échange de place, et l'exécution
 * d'une tuile ordinaire. Ne le consomment pas : poser ou reprendre une tuile, exécuter une tuile
 * Free-Play, et <b>toute action qui échoue</b>. Une action bloquée qui coûterait un tour serait la
 * pire des punitions : celle qu'on ne comprend pas.
 */
public final class Arena {

    private final Grid grid;
    private final Hero hero;
    private final TileRack rack;
    private final ActionQueue queue = new ActionQueue();

    private int turnsTaken;
    /** Numéro de la phase ennemie, qui sert aux archétypes n'agissant qu'une phase sur deux. */
    private int phaseIndex;
    private int heroHits;

    public Arena(int gridWidth) {
        this(gridWidth, gridWidth / 2);
    }

    public Arena(int gridWidth, int heroStart) {
        this(gridWidth, heroStart, new TileRack(Tile.values()));
    }

    public Arena(int gridWidth, int heroStart, TileRack rack) {
        this.grid = new Grid(gridWidth);
        this.hero = new Hero();
        this.rack = rack;
        grid.place(heroStart, hero);
    }

    public Grid grid() {
        return grid;
    }

    public Hero hero() {
        return hero;
    }

    public TileRack rack() {
        return rack;
    }

    public ActionQueue queue() {
        return queue;
    }

    /** Nombre de tours consommés depuis le début du combat. */
    public int turnsTaken() {
        return turnsTaken;
    }

    /**
     * Consomme un tour : c'est ici, et nulle part ailleurs, que le temps passe.
     *
     * <p>Tout centraliser dans une seule méthode est ce qui garantit qu'on ne pourra pas ajouter
     * une action qui fait avancer les ennemis en oubliant de recharger les tuiles, ou l'inverse.
     */
    private void consumeTurn() {
        turnsTaken++;
        rack.gainRechargePoint();
        enemyPhase();
    }

    // ------------------------------------------------------------------ phase ennemie

    /**
     * Les ennemis exécutent ce qu'ils avaient annoncé, puis annoncent la suite.
     *
     * <p>L'ordre est de gauche à droite, et il est fixé pour une raison : deux ennemis qui visent la
     * même case doivent produire le même résultat à chaque partie. Un ordre dépendant de
     * l'itération d'une structure interne rendrait le jeu subtilement non reproductible.
     *
     * <p>Exécuter d'abord, annoncer ensuite, est ce qui rend le télégraphe honnête : ce qui est
     * montré au joueur pendant qu'il réfléchit est exactement ce qui sera joué.
     */
    private void enemyPhase() {
        for (Enemy enemy : enemiesLeftToRight()) {
            if (!enemy.kind().actsThisPhase(phaseIndex)) {
                continue;
            }
            execute(enemy);
        }
        phaseIndex++;
        announceIntentions();
    }

    /** Ennemis présents sur la grille, de gauche à droite. */
    private List<Enemy> enemiesLeftToRight() {
        List<Enemy> enemies = new ArrayList<>();
        for (int cell : grid.occupiedCells()) {
            if (grid.occupantAt(cell) instanceof Enemy enemy) {
                enemies.add(enemy);
            }
        }
        return enemies;
    }

    /**
     * Fait recalculer son intention à chaque ennemi encore en vie.
     *
     * <p><b>Un ennemi qui n'agira pas à la phase à venir annonce qu'il attend.</b> Le colosse
     * n'agissant qu'une phase sur deux, il annonçait sinon une frappe qu'il ne portait pas : la case
     * du héros était cerclée de rouge, le glyphe montrait une attaque, et rien ne partait. C'est
     * exactement le mensonge que le télégraphe existe pour interdire — et il touchait un archétype
     * sur quatre.
     *
     * <p>{@code phaseIndex} a déjà été incrémenté quand cette méthode est appelée : il désigne donc
     * la phase <em>à venir</em>, celle que l'annonce doit décrire.
     */
    void announceIntentions() {
        int heroCell = heroCell();
        for (Enemy enemy : enemiesLeftToRight()) {
            if (!enemy.kind().actsThisPhase(phaseIndex)) {
                enemy.announce(Intention.of(Intention.Kind.WAIT));
                continue;
            }
            enemy.announce(EnemyBrain.decide(grid, enemy, grid.indexOf(enemy), heroCell));
        }
    }

    /**
     * Joue l'intention annoncée, sans la recalculer.
     *
     * <p>Si le joueur s'est écarté de la case visée, l'attaque part dans le vide — c'est exactement
     * ainsi qu'on esquive, et c'est pour cela que rien n'est recalculé ici.
     */
    private void execute(Enemy enemy) {
        int cell = grid.indexOf(enemy);
        if (cell < 0) {
            return; // tué entre-temps
        }
        Intention intention = enemy.intention();
        switch (intention.kind()) {
            case ADVANCE -> {
                if (grid.isFree(intention.targetCell())) {
                    grid.move(cell, intention.targetCell());
                }
            }
            case ATTACK -> {
                for (int blow = 0; blow < enemy.strikesPerAttack(); blow++) {
                    strike(intention.targetCell());
                }
            }
            case WIND_UP -> enemy.setWindingUp(true);
            case CHARGE -> charge(enemy, cell, intention.targetCell());
            case WAIT -> {
            }
        }
    }

    /**
     * Une charge avance case par case et <b>s'arrête au premier obstacle</b>.
     *
     * <p>La version précédente ne testait que la case d'arrivée : un lancier traversait allègrement
     * le héros et ses propres camarades pour aller se poser derrière eux. Outre l'absurdité, cela
     * contredisait la doctrine écrite pour le fonceur — « un ennemi qui traverserait ses camarades
     * serait illisible ».
     *
     * <p>Une charge interceptée ne frappe pas : <b>s'interposer l'arrête</b>. C'est la contrepartie
     * du télégraphe — il annonce une case un tour à l'avance, donc on doit pouvoir y répondre
     * autrement qu'en s'écartant.
     */
    private void charge(Enemy enemy, int from, int target) {
        enemy.setWindingUp(false);
        int step = Integer.signum(target - from);
        if (step == 0) {
            return;
        }
        int landing = target - step;
        int cell = from;
        while (cell != landing && grid.isFree(cell + step)) {
            grid.move(cell, cell + step);
            cell += step;
        }
        if (cell != landing) {
            return; // interceptée en chemin : la charge s'arrête là et ne frappe pas
        }
        for (int blow = 0; blow < enemy.strikesPerAttack(); blow++) {
            strike(target);
        }
    }

    /** Résout une frappe ennemie sur une case. */
    private void strike(int cell) {
        if (grid.occupantAt(cell) == hero) {
            heroHits++;
        }
    }

    /**
     * Retire un occupant de la grille, et déclenche ce que sa mort provoque.
     *
     * <p>Tout ce qui tue passe par ici. Vider directement la case fonctionnerait — et c'est ce que
     * faisaient les tuiles — mais un ennemi explosif n'exploserait jamais : un trait qui ne fait
     * rien est pire qu'un trait absent, puisqu'il est annoncé au joueur.
     */
    void kill(int cell) {
        Occupant victim = grid.clear(cell);
        if (victim instanceof Enemy enemy && enemy.has(Trait.EXPLOSIF)) {
            explode(cell);
        }
    }

    /**
     * Un explosif emporte ses deux voisins immédiats.
     *
     * <p>La récursion se termine parce que la victime est retirée <em>avant</em> que l'explosion ne
     * se propage : une chaîne d'explosifs se déclenche donc en cascade, chacun une seule fois. Cette
     * cascade est la forme la plus simple du combo, que la résolution du combat (M7) reprendra pour
     * en faire un enchaînement compté et récompensé.
     */
    private void explode(int centre) {
        for (int step : new int[]{-1, 1}) {
            int neighbour = centre + step;
            Occupant victim = grid.occupantAt(neighbour);
            if (victim == hero) {
                heroHits++;
            } else if (victim instanceof Enemy) {
                kill(neighbour);
            }
        }
    }

    /**
     * Nombre de fois où le héros a été touché.
     *
     * <p>Un simple compteur, et c'est assumé : les points de vie, les statuts et les combos sont
     * l'affaire de la résolution du combat (M7). Ce qui doit être juste dès maintenant, c'est
     * <em>quand</em> une frappe porte — donc le télégraphe et l'esquive.
     */
    public int heroHits() {
        return heroHits;
    }

    /** Ennemis encore en vie, de gauche à droite. */
    public List<Enemy> enemies() {
        return enemiesLeftToRight();
    }

    /** Vrai si une case est menacée par l'intention annoncée d'au moins un ennemi. */
    public boolean isThreatened(int cell) {
        return threatCount(cell) > 0;
    }

    /**
     * Nombre de coups qui tomberont sur une case si personne ne bouge.
     *
     * <p>Deux ennemis qui visent la même case, ou un ennemi rapide qui frappe deux fois, ne
     * produisaient qu'un seul cerclage : le joueur voyait « danger » sans voir « deux fois plus de
     * danger », et le compteur de coups reçus le surprenait après coup.
     */
    public int threatCount(int cell) {
        int count = 0;
        for (Enemy enemy : enemiesLeftToRight()) {
            if (enemy.intention().threatens(cell)) {
                count += enemy.strikesPerAttack();
            }
        }
        return count;
    }

    /** Case du héros. Toujours valide : le héros ne quitte jamais la grille. */
    public int heroCell() {
        return grid.indexOf(hero);
    }

    /**
     * Action de déplacement dans une direction.
     *
     * @return {@link ActionResult#TURNED} si le héros regardait ailleurs, {@link
     *         ActionResult#MOVED} s'il a avancé, {@link ActionResult#BLOCKED} si un bord ou un
     *         occupant l'en empêchait
     */
    public ActionResult step(Direction direction) {
        // La seule fabrique de Direction du projet, Direction.towards, peut rendre null quand les
        // deux cases sont les mêmes. Accepter null ici posait une orientation nulle sans rien dire,
        // et le premier calcul de cible suivant partait en NullPointerException.
        if (direction == null) {
            throw new IllegalArgumentException("Direction nulle : aucune action à jouer");
        }
        if (hero.facing() != direction) {
            hero.face(direction);
            consumeTurn();
            return ActionResult.TURNED;
        }

        int from = heroCell();
        int to = from + direction.step();
        if (!grid.isFree(to)) {
            return ActionResult.BLOCKED;
        }
        grid.move(from, to);
        consumeTurn();
        return ActionResult.MOVED;
    }

    /**
     * Capacité spéciale de la Vagabonde : elle échange sa place avec le premier occupant devant
     * elle, aussi loin soit-il. C'est ce qui lui permet de traverser une ligne ennemie au lieu de
     * la subir, et de laisser une cible là où elle était.
     *
     * @return {@link ActionResult#SWAPPED}, ou {@link ActionResult#NO_TARGET} si personne n'est
     *         devant
     */
    public ActionResult swapWithTarget() {
        int target = swapTarget();
        if (target < 0) {
            return ActionResult.NO_TARGET;
        }
        grid.swap(heroCell(), target);
        consumeTurn();
        return ActionResult.SWAPPED;
    }

    // ------------------------------------------------------------------ file d'actions

    /**
     * Pose une tuile du râtelier sur la file. <b>Gratuit</b> : aucun tour consommé.
     *
     * @return {@link ActionResult#QUEUED}, ou {@link ActionResult#QUEUE_FULL} / {@link
     *         ActionResult#NOT_READY} si le geste est impossible
     */
    public ActionResult queueTile(Tile tile) {
        if (queue.isFull()) {
            return ActionResult.QUEUE_FULL;
        }
        if (!rack.isReady(tile)) {
            return ActionResult.NOT_READY;
        }
        rack.take(tile);
        queue.push(tile);
        return ActionResult.QUEUED;
    }

    /**
     * Reprend une tuile de la file, désignée par sa position d'affichage — de la plus ancienne à la
     * plus récente. <b>Gratuit</b> aussi, et la tuile ne part pas en recharge : elle n'a pas servi.
     */
    public ActionResult unqueueAt(int indexFromOldest) {
        Tile removed = queue.removeAt(indexFromOldest);
        if (removed == null) {
            return ActionResult.BLOCKED;
        }
        rack.giveBack(removed);
        return ActionResult.UNQUEUED;
    }

    /**
     * Exécute la tuile du sommet — la dernière posée.
     *
     * <p>La tuile part en recharge et le tour est consommé, <b>sauf</b> si elle est Free-Play. Une
     * tuile dont l'effet échoue — personne à frapper, poussée bloquée — est tout de même consommée
     * et rechargée : c'est le prix de l'avoir jouée. En revanche elle ne consomme pas de tour, pour
     * ne pas faire payer deux fois une décision déjà punie.
     */
    public ActionResult executeTop() {
        if (queue.isEmpty()) {
            return ActionResult.EMPTY_QUEUE;
        }
        Tile tile = queue.pop();
        ActionResult result = tile.applyTo(this);
        rack.giveBackSpent(tile);

        boolean effective = result != ActionResult.NO_TARGET && result != ActionResult.BLOCKED;
        if (effective && !tile.isFreePlay()) {
            consumeTurn();
        }
        return result;
    }

    /** Case que la capacité viserait maintenant, ou {@code -1}. Sert aussi à la télégraphier. */
    public int swapTarget() {
        return grid.firstOccupied(heroCell(), hero.facing());
    }

    /**
     * Action correspondant à un clic sur une case : le héros fait le <b>seul</b> pas qui le
     * rapproche de cette case.
     *
     * <p>Un clic ne déclenche jamais plus d'une action. Sinon un clic à l'autre bout de la grille
     * enchaînerait plusieurs tours d'un coup, ce qui n'a aucun sens dans un jeu au tour par tour et
     * rendrait la souris moins précise que le clavier — alors que les deux doivent être de plein
     * droit.
     */
    public ActionResult clickOn(int cell) {
        if (!grid.contains(cell)) {
            return ActionResult.BLOCKED;
        }
        int from = heroCell();
        if (cell == from) {
            return ActionResult.BLOCKED;
        }

        Direction direction = Direction.towards(from, cell);
        // Le demi-tour et le pas sont délégués à step() plutôt que réécrits ici. La version
        // précédente dupliquait la logique de demi-tour, et c'est exactement ce qui a laissé le
        // clic échapper à la comptabilité des tours : se retourner coûtait un tour au clavier et
        // rien à la souris. Une règle écrite à deux endroits finit toujours par diverger.
        if (hero.facing() == direction && cell == swapTarget()) {
            // Déjà tourné vers la cible de la capacité : l'échange est le geste attendu.
            return swapWithTarget();
        }
        return step(direction);
    }
}
