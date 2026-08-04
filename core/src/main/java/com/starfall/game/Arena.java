package com.starfall.game;

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
     * <p>Tout centraliser dans une seule méthode est ce qui garantit qu'on ne pourra pas ajouter en
     * M6 une action qui fait avancer les ennemis en oubliant de recharger les tuiles, ou l'inverse.
     */
    private void consumeTurn() {
        turnsTaken++;
        rack.gainRechargePoint();
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
        if (hero.facing() != direction) {
            hero.face(direction);
            return ActionResult.TURNED;
        }
        // Déjà tourné vers la case : si elle porte la cible de la capacité, l'échange est le geste
        // attendu ; sinon on avance d'un pas.
        if (cell == swapTarget()) {
            return swapWithTarget();
        }
        return step(direction);
    }
}
