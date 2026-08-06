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
 * <p>Un tour est la seule monnaie du jeu : c'est lui qui fait avancer les ennemis et recharger les
 * tuiles. Le consomment : un déplacement, un demi-tour, un échange de place, <b>poser une tuile</b>,
 * et une <b>salve</b> dont au moins une tuile porte. Ne le consomment pas : reprendre une tuile,
 * poser une tuile Free-Play, une salve qui n'en contient que des Free-Play, et <b>toute action qui
 * échoue</b>. Une action bloquée qui coûterait un tour serait la pire des punitions : celle qu'on ne
 * comprend pas.

 * <p>Charger est donc le moment vulnérable et la salve la récompense — voir {@link #unleash()}.
 */
public final class Arena {

    private final Grid grid;
    private final Hero hero;
    private final TileRack rack;
    private final ActionQueue queue = new ActionQueue();

    /**
     * Dégâts d'une explosion. Deux points : de quoi emporter d'un coup les archétypes légers et
     * n'entamer que les lourds. C'est ce qui rend la chaîne d'explosions possible sans la rendre
     * automatique — un colosse à pleine santé l'arrête.
     */
    public static final int EXPLOSION_DAMAGE = 2;
    /** Dégâts d'une collision. Un point, comme une frappe : le mur vaut une lame, pas davantage. */
    public static final int COLLISION_DAMAGE = 1;
    /**
     * Points de vie rendus au passage d'une vague.
     *
     * <p>Deux depuis le jalon d'équilibrage, et la review a montré que c'est <b>trop</b> : sur la
     * meilleure ligne mesurée, le héros encaisse trois coups sur toute la tranche pour six points
     * rendus. Le répit est intégralement absorbé, il ne contraint plus rien.
     *
     * <p>Il reste en l'état parce que le nombre qu'il faudrait bouger n'est pas celui-là : la même
     * mesure a montré que <b>charger la file n'est pas survivable</b> — six configurations sur
     * douze meurent avant la quatrième tuile — et qu'aucune ligne gagnante ne remplit jamais les
     * cinq emplacements. Baisser le répit rendrait le jeu plus dur sans rendre sa mécanique vedette
     * plus accessible. La question est consignée au tableau de bord.
     */
    public static final int WAVE_RESPITE = 2;

    private int turnsTaken;
    /** Numéro de la phase ennemie, qui sert aux archétypes n'agissant qu'une phase sur deux. */
    private int phaseIndex;
    private int heroHits;
    /** Morts provoquées par l'action en cours : c'est la base du combo. */
    private int killsThisAction;
    private int lastCombo;
    private int bestCombo;
    private int wave = 1;
    private boolean victory;
    /** Vrai quand cette arène joue une campagne de vagues, et non un plateau isolé. */
    private boolean wavesEnabled;

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

    // ------------------------------------------------------------------ combos et vagues

    /** Ennemis tombés du dernier geste du joueur. Deux ou plus, c'est un combo. */
    public int lastCombo() {
        return lastCombo;
    }

    /** Meilleur enchaînement de la partie. */
    public int bestCombo() {
        return bestCombo;
    }

    /** Numéro de la vague en cours, à partir de 1. */
    public int wave() {
        return wave;
    }

    public int waveCount() {
        return WaveTable.count();
    }

    public boolean isVictory() {
        return victory;
    }

    /**
     * Enclenche l'enchaînement des vagues.
     *
     * <p>Une arène est un plateau de combat ; la campagne de vagues est une couche au-dessus. Sans
     * cette distinction, tout plateau sans ennemi — un plateau d'essai, une situation construite à
     * la main — déclenchait une vague dès la première action, ce qui rendait toute mise en place
     * ciblée impossible.
     */
    void enableWaves() {
        this.wavesEnabled = true;
    }

    /**
     * Démarre la partie à une vague donnée, plutôt qu'à la première.
     *
     * <p>Sert à aller voir la fin sans jouer le début : la rencontre du souverain est la quatrième,
     * et ni une capture reproductible ni un relecteur ne peuvent gagner trois vagues d'abord.
     */
    void startAtWave(int startWave) {
        this.wave = Math.max(1, Math.min(startWave, WaveTable.count()));
    }

    /**
     * Nombre de vagues d'une tranche.
     *
     * <p>Public pour que la ligne de commande puisse borner son option, et <b>dérivé</b> de la table
     * des vagues plutôt que recopié : un nombre écrit à deux endroits finit toujours par n'en
     * décrire qu'un seul.
     */
    public static final int WAVE_COUNT = WaveTable.count();

    public boolean isDefeat() {
        return hero.health() == 0;
    }

    /** Vrai quand la partie est finie, gagnée ou perdue. */
    public boolean isOver() {
        return victory || isDefeat();
    }

    /**
     * Clôt une action du joueur : compte le combo, enchaîne la vague si le terrain est vide.
     *
     * <p>Un seul point de passage, appelé par chaque action, plutôt qu'un appel dispersé dans chaque
     * tuile — la même raison qui a fait centraliser la consommation de tour, et la même leçon : une
     * règle écrite à plusieurs endroits finit par diverger.
     *
     * @return le résultat à afficher, éventuellement remplacé par un événement plus important
     */
    /**
     * Un <b>temps</b> de la résolution : une tuile a joué, voici ce qu'elle a fait et où.
     *
     * <p>Une salve de cinq tuiles se résout dans un seul tour, et le modèle la résout d'un bloc :
     * l'écran passe de l'avant à l'après sans que rien ne montre l'enchaînement. C'est le geste
     * central du jeu — celui pour lequel la file existe — et c'est le seul qu'on ne peut pas lire.
     *
     * <p>Ces temps sont ce que la scène animera. Le modèle reste instantané, ce qui est
     * délibéré : tout ce qui dépend de lui — les cinq cent dix tests, la recherche de jouabilité,
     * les mesures d'équilibrage — continue de voir une résolution atomique. C'est la <em>vue</em>
     * qui prend du retard sur l'état, jamais l'inverse.
     *
     * @param tile   la tuile qui a joué
     * @param result ce qu'elle a produit
     * @param cell   la case visée, ou {@code -1} quand la tuile n'en vise aucune
     */
    public record Beat(Tile tile, ActionResult result, int cell, List<Figure> board,
                      List<Tile> queued) {
    }

    /*
     * CE QUE CE JOURNAL COUTE, ET POURQUOI IL EST PAYE PARTOUT.
     *
     * Chaque tuile d'une salve photographie le plateau et la file. La recherche par faisceau et
     * les fuzz rejouent des dizaines de milliers de salves sans que personne n'anime jamais rien :
     * ils paient donc pour une donnee qu'eux seuls ne liront pas. Mesure, suite complete relancee
     * en force : 19,57 s avec les instantanes, 17,01 s sans - environ 15 %.
     *
     * Ce n'est pas rendu conditionnel, et c'est un choix. Un drapeau « enregistre ou non » creerait
     * DEUX chemins d'execution : celui que les cinq cent dix-huit tests eprouvent, et celui que le
     * joueur voit. Ce projet vient precisement de payer cette lecon - deduplication qui vide un
     * contrat, regle recopiee qui diverge - et quinze pour cent d'une suite de vingt secondes
     * achetent moins qu'un chemin unique.
     *
     * L'autre economie possible serait de ne rien enregistrer quand la salve n'a qu'une tuile,
     * puisque le deroule ne montre alors rien. Elle est ecartee pour la meme raison : la regle
     * « moins de deux temps, pas de deroule » vit dans Playback, et la recopier ici la ferait
     * diverger le jour ou l'une des deux bougerait.
     */

    /**
     * Une figure sur le plateau, telle qu'il faut la dessiner — et rien de plus.
     *
     * <p>Assez pour que la scène rende un plateau <em>passé</em> : la case, le sprite, l'orientation,
     * la santé, et de quoi distinguer le héros d'un ennemi explosif. Pas de référence vers
     * l'occupant vivant, et c'est tout l'objet : un instantané qui pointerait vers l'objet réel
     * montrerait l'état d'aujourd'hui sous prétexte de montrer celui d'hier.
     */
    public record Figure(long id, int cell, String sprite, Direction facing, int health,
                         int maxHealth, boolean hero, boolean explosive) {
    }

    /**
     * Le plateau tel qu'il est <b>maintenant</b>, sous une forme que la scène peut garder.
     *
     * <p>C'est ce qui permet au déroulé de faire reculer les figures dans le temps : sans lui, le
     * plateau affiche l'état final pendant que le déroulé égrène les cases, et l'on voit l'ordre
     * des coups sans voir leurs effets.
     */
    public List<Figure> snapshot() {
        List<Figure> figures = new ArrayList<>();
        for (int cell : grid.occupiedCells()) {
            Occupant occupant = grid.occupantAt(cell);
            if (occupant == hero) {
                figures.add(new Figure(hero.id(), cell, hero.spriteName(), hero.facing(),
                        hero.health(), Hero.MAX_HEALTH, true, false));
            } else if (occupant instanceof Enemy enemy) {
                figures.add(new Figure(enemy.id(), cell, enemy.spriteName(), enemy.facing(),
                        enemy.health(), enemy.maxHealth(), false, enemy.has(Trait.EXPLOSIF)));
            } else {
                figures.add(new Figure(occupant.id(), cell, occupant.spriteName(),
                        Direction.LEFT, 0, 0, false, false));
            }
        }
        return List.copyOf(figures);
    }

    /**
     * Les temps de la <b>dernière action du joueur</b>, dans l'ordre où ils se sont produits.
     *
     * <p>Vide pour un geste qui n'en compte qu'un — un pas, un échange : il n'y a rien à
     * dérouler. Une salve en compte autant que de tuiles parties, et c'est là que la lecture se
     * perd aujourd'hui.
     */
    public List<Beat> beats() {
        return List.copyOf(beats);
    }

    /**
     * Le plateau tel qu'il était juste AVANT la dernière action du joueur.
     *
     * <p>Chaque temps porte l'état qui <em>suit</em> la tuile qui vient de jouer. Le premier n'a
     * donc rien à quoi se comparer, et sans ce plateau d'ouverture la première tuile d'une salve
     * apparaîtrait déjà résolue pendant que les suivantes s'animent : le coup le plus important
     * de la salve serait le seul qu'on ne verrait pas partir.
     *
     * <p>Il coûte un instantané par action, là où les temps en coûtent un par tuile. La mesure
     * qui accompagne {@link #beats()} vaut donc a fortiori pour lui.
     */
    public List<Figure> opening() {
        return opening;
    }

    private final List<Beat> beats = new ArrayList<>();

    /** Le plateau tel qu'il était juste avant l'action en cours. Voir {@link #opening()}. */
    private List<Figure> opening = List.of();

    /** Ouvre une action du joueur : c'est ici que le compteur de morts repart de zéro. */
    private void beginAction() {
        killsThisAction = 0;
        beats.clear();
        // Le plateau AVANT que quoi que ce soit ne bouge. Les temps disent chacun l'état qui suit
        // la tuile qui vient de jouer ; il manquait celui qui précède la première, sans quoi le
        // premier temps d'une salve n'a rien à quoi se comparer et sa figure apparaît déjà arrivée.
        opening = snapshot();
    }

    private ActionResult settle(ActionResult result) {
        lastCombo = killsThisAction;
        bestCombo = Math.max(bestCombo, lastCombo);
        killsThisAction = 0;

        if (isDefeat()) {
            return ActionResult.DEFEAT;
        }
        if (wavesEnabled && enemiesLeftToRight().isEmpty()) {
            if (wave >= WaveTable.count()) {
                victory = true;
                return ActionResult.VICTORY;
            }
            wave++;
            hero.heal(WAVE_RESPITE);
            WaveTable.spawn(this, wave);
            announceIntentions();
        }
        // Un combo mérite d'être nommé : c'est la récompense du placement, et rien d'autre à
        // l'écran ne la désigne.
        return lastCombo >= 2 ? ActionResult.COMBO : result;
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
            // L'ordre des deux tests compte. Un étourdissement promet de faire perdre « sa prochaine
            // activation » : le consommer sur une phase où l'ennemi se reposait de toute façon
            // reviendrait à ne rien lui coûter, et c'est précisément le colosse — le seul à trois
            // points de vie — que le joueur est censé gérer en poussant plutôt qu'en tuant.
            if (!enemy.kind().actsThisPhase(phaseIndex)) {
                continue;
            }
            if (enemy.isStunned()) {
                enemy.setStunned(false);
                continue;
            }
            execute(enemy);
        }
        resolvePendingPush();
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
        List<Enemy> enemies = enemiesLeftToRight();

        // Deux passes, et l'ordre est le fond du sujet. Les charges s'annoncent d'abord, puis leurs
        // couloirs sont réservés : un allié qui s'y posait interceptait la charge, et le coup
        // annoncé au joueur ne tombait jamais. Une intention est un engagement — les autres
        // ennemis doivent le respecter, sans quoi le télégraphe sur-promet.
        boolean[] reserved = new boolean[grid.width()];
        for (Enemy enemy : enemies) {
            if (!willAct(enemy)) {
                enemy.announce(Intention.of(Intention.Kind.WAIT));
            }
        }
        // Les charges d'abord, les invocations ensuite, le reste en dernier. L'ordre des deux
        // premières passes compte : un lancier en élan tient sa promesse quoi qu'il arrive et ne
        // consulte donc aucune réservation. Si une invocation s'annonçait avant lui, elle pouvait
        // choisir une case au milieu de son couloir, et le sbire qui s'y levait interceptait la
        // charge — le coup promis ne tombait jamais. C'est mot pour mot la faute que le jalon
        // précédent a payée avec un allié, transposée à un ennemi qui n'existait pas encore.
        announceGroundClaims(enemies, heroCell, reserved, Enemy::isWindingUp);
        announceGroundClaims(enemies, heroCell, reserved, e -> e.kind().summons() > 0);

        for (Enemy enemy : enemies) {
            if (!willAct(enemy) || claimsGround(enemy)) {
                continue;
            }
            enemy.announce(EnemyBrain.decide(grid, enemy, grid.indexOf(enemy), heroCell,
                    reserved, phaseIndex));
        }
    }

    /** Fait annoncer les ennemis d'une catégorie, et marque le terrain qu'ils réclament. */
    private void announceGroundClaims(List<Enemy> enemies, int heroCell, boolean[] reserved,
                                      java.util.function.Predicate<Enemy> selector) {
        for (Enemy enemy : enemies) {
            if (!willAct(enemy) || !selector.test(enemy)) {
                continue;
            }
            Intention claim = EnemyBrain.decide(grid, enemy, grid.indexOf(enemy), heroCell,
                    reserved, phaseIndex);
            enemy.announce(claim);
            reserve(reserved, grid.indexOf(enemy), claim);
        }
    }

    /**
     * Vrai si cet ennemi annonce quelque chose qui <b>réclame du terrain</b> : un couloir de charge,
     * ou la case où son invocation apparaîtra.
     *
     * <p>Ceux-là s'annoncent d'abord, et leur emprise est ensuite interdite aux autres. Sans cela un
     * allié pouvait se poser dans le couloir d'une charge — le coup promis ne tombait jamais — ou
     * sur la case d'une invocation, qui échouait alors sans que le joueur y soit pour rien. Dans les
     * deux cas, l'annonce cesse d'être un engagement.
     */
    private static boolean claimsGround(Enemy enemy) {
        return enemy.isWindingUp() || enemy.kind().summons() > 0;
    }

    /** Vrai si l'ennemi jouera réellement à la phase à venir. */
    private boolean willAct(Enemy enemy) {
        return !enemy.isStunned() && enemy.kind().actsThisPhase(phaseIndex);
    }

    /** Marque le terrain qu'une intention réclame : un couloir de charge, ou une case d'invocation. */
    private static void reserve(boolean[] reserved, int from, Intention intention) {
        if (intention.kind() == Intention.Kind.SUMMON) {
            mark(reserved, intention.targetCell());
            return;
        }
        if (intention.kind() != Intention.Kind.CHARGE && intention.kind() != Intention.Kind.RUSH) {
            return;
        }
        int step = Integer.signum(intention.targetCell() - from);
        if (step == 0) {
            return;
        }
        for (int cell = from; cell != intention.targetCell(); cell += step) {
            mark(reserved, cell);
        }
    }

    private static void mark(boolean[] reserved, int cell) {
        if (cell >= 0 && cell < reserved.length) {
            reserved[cell] = true;
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
            case RUSH -> rush(enemy, cell, intention.targetCell());
            case SUMMON -> summon(enemy, intention.targetCell());
            case WAIT -> {
            }
        }
    }

    /**
     * La ruée du souverain : il vient au contact, frappe une fois, et <b>repousse</b>.
     *
     * <p>Elle réutilise le déplacement de la charge — donc elle s'arrête au premier obstacle et ne
     * frappe pas si on s'interpose, comme toute charge. Ce qu'elle ajoute est le déplacement subi :
     * dans un jeu où l'on gagne en plaçant, être déplacé sans l'avoir choisi coûte souvent plus
     * cher que le point de vie.
     *
     * <p><b>La poussée ne blesse pas</b>, même contre un mur, et c'est une contrainte du télégraphe
     * plus qu'un choix d'équilibre : un dégât conditionnel à la géométrie ne peut pas être annoncé
     * un tour à l'avance sans risquer de sur-promettre, et le nombre de coups affiché doit rester
     * exactement le nombre de coups reçus.
     */
    private void rush(Enemy enemy, int from, int target) {
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
        if (cell != landing || grid.occupantAt(target) == null) {
            return; // interceptée, ou la cible s'est écartée : c'est ainsi qu'on esquive
        }
        // Autant de coups que la charge en porterait : le compteur de menaces les compte, donc
        // les oublier ferait sous-promettre le télégraphe dès qu'un souverain porterait un trait.
        for (int blow = 0; blow < enemy.strikesPerAttack(); blow++) {
            strike(target);
        }
        // La poussée est différée à la fin de la phase — voir pendingPush.
        pendingPushFrom = target;
        pendingPushStep = step;
    }

    /**
     * Poussée en attente, appliquée quand <b>toute</b> la phase ennemie a été résolue.
     *
     * <p>Appliquée sur-le-champ, elle sortait le héros d'une case que d'autres ennemis avaient
     * annoncé vouloir frapper, et leurs coups partaient dans le vide : le bandeau affichait deux
     * coups, un seul tombait, et la différence tenait à l'ordre d'exécution — invisible. Le
     * télégraphe sous-promettait.
     *
     * <p>Différer la poussée règle le cas sans rien retirer au joueur : il est déplacé de la même
     * case vers la même case, simplement après que tout ce qui était annoncé a eu lieu. C'est aussi
     * ce qui se raconte le mieux — la mêlée se résout, puis le souverain vous jette.
     */
    private int pendingPushFrom = -1;
    private int pendingPushStep;

    private void resolvePendingPush() {
        int from = pendingPushFrom;
        int step = pendingPushStep;
        pendingPushFrom = -1;
        if (from < 0 || grid.occupantAt(from) == null || !grid.isFree(from + step)) {
            return;
        }
        grid.move(from, from + step);
    }

    /**
     * L'invocation : un sabreur apparaît sur la case annoncée.
     *
     * <p>Rien si la case n'est plus libre — et c'est la réponse du joueur, pas un raté : la case est
     * montrée un tour à l'avance, s'y placer refuse l'invocation. Le coût est réel, puisque s'y
     * placer signifie souvent tourner le dos au souverain.
     *
     * <p>L'invocation est consommée <b>dans tous les cas</b>. Une invocation refusée est une
     * invocation perdue, sans quoi refuser ne ferait que retarder, et retarder n'est pas décider.
     */
    private void summon(Enemy enemy, int cell) {
        enemy.spendSummon();
        if (!grid.isFree(cell)) {
            return;
        }
        Enemy minion = new Enemy(EnemyKind.SABREUR);
        minion.summonedBy(enemy);
        grid.place(cell, minion);
        minion.announce(Intention.of(Intention.Kind.WAIT));
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
            // Interceptée : elle s'arrête là et ne frappe pas — mais elle garde son élan, sans quoi
            // s'interposer coûterait à l'ennemi bien plus que le tour qu'il vient de perdre.
            return;
        }
        enemy.setWindingUp(false);
        for (int blow = 0; blow < enemy.strikesPerAttack(); blow++) {
            strike(target);
        }
    }

    /** Résout une frappe ennemie sur une case. */
    private void strike(int cell) {
        if (grid.occupantAt(cell) == hero) {
            heroHits++;
            hero.damage(1);
        }
    }

    /**
     * Inflige des dégâts à ce qui occupe une case, et tue si les points de vie tombent à zéro.
     *
     * <p>Tout ce qui blesse passe par ici, et tout ce qui tue passe par {@link #kill} : c'est ce qui
     * garantit qu'une mort déclenche toujours son explosion et compte toujours dans le combo, quel
     * que soit ce qui l'a provoquée — une tuile, une collision, ou une autre explosion.
     */
    void damage(int cell, int amount) {
        Occupant victim = grid.occupantAt(cell);
        if (victim == null) {
            return;
        }
        if (victim == hero) {
            heroHits++;
            hero.damage(amount);
        } else if (victim instanceof Enemy enemy) {
            if (enemy.damage(amount)) {
                kill(cell);
            }
        } else {
            // Un occupant sans points de vie — décor, obstacle — tombe du premier coup. L'alternative
            // serait qu'il absorbe les frappes en silence, ce qui ferait mentir le résultat rendu.
            kill(cell);
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
        if (victim instanceof Enemy enemy) {
            killsThisAction++;
            if (enemy.has(Trait.EXPLOSIF)) {
                explode(cell);
            }
            dismissSummonsOf(enemy);
        }
    }

    /**
     * Les invocations d'un chef abattu <b>disparaissent</b>.
     *
     * <p>Règle systémique de la source, et elle n'était pas ici : « les invocations disparaissent
     * instantanément à la mort du leader, économisant ainsi des dizaines de tours d'action
     * potentiellement mortels ». Sans elle, la seule ligne jouable contre le souverain est de tout
     * nettoyer ; avec elle, ignorer les sbires pour saturer le chef en devient une seconde, et
     * c'est cette alternative qui fait la rencontre.
     *
     * <p>Elles <b>disparaissent</b>, elles ne meurent pas : ni explosion, ni combo. Créditer ces
     * retraits inverserait l'incitation que la règle installe — on gagnerait à laisser vivre les
     * sbires pour les encaisser tous d'un coup, alors que le sens de la règle est qu'ils cessent
     * simplement de compter. C'est aussi pourquoi on passe par {@code grid.clear} et non par
     * {@link #kill}, qui déclencherait les deux.
     *
     * <p>Appelé après l'explosion : un chef explosif blesse d'abord ses voisins, puis emporte les
     * siens. L'ordre inverse ferait disparaître des sbires que l'explosion aurait dû toucher.
     */
    private void dismissSummonsOf(Enemy leader) {
        for (int cell = 0; cell < grid.width(); cell++) {
            if (grid.occupantAt(cell) instanceof Enemy minion && minion.summoner() == leader) {
                grid.clear(cell);
            }
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
            if (grid.occupantAt(neighbour) != null) {
                damage(neighbour, EXPLOSION_DAMAGE);
            }
        }
    }

    /**
     * Décide ce que ferait une poussée, sans la faire.
     *
     * <p>Une poussée qui aboutit ne fait que déplacer. Une poussée qui <b>butte</b> — sur un bord ou
     * sur quelqu'un — blesse le poussé, blesse ce qu'il a percuté, et l'étourdit. C'est ce qui fait
     * d'une tuile de placement une tuile de dégâts quand on l'utilise bien : le mur devient une
     * arme, et la position de l'ennemi compte autant que ses points de vie.
     *
     * <p>La décision est isolée de son exécution parce que l'interface doit pouvoir la montrer au
     * joueur <em>avant</em> qu'il valide. Le cas de la collision est justement celui qu'un joueur ne
     * peut pas calculer d'un coup d'œil au bord de la grille — et c'est le plus rentable.
     */
    TilePreview planShove(Tile tile, int from, Direction direction) {
        if (grid.occupantAt(from) == null) {
            return TilePreview.inert(tile, ActionResult.NO_TARGET, direction, from);
        }
        int landing = from + direction.step();
        // La case d'arrivée peut être hors grille : c'est le mur, et le préavis doit le dire.
        return new TilePreview(tile,
                grid.isFree(landing) ? ActionResult.PUSHED : ActionResult.COLLIDED,
                TilePreview.Kind.PUSH, direction, from, landing);
    }

    /** Réalise une poussée déjà décidée. Ne décide de rien. */
    ActionResult applyShove(TilePreview plan) {
        if (!plan.connects()) {
            return plan.outcome();
        }
        int from = plan.aim();
        int landing = plan.landing();
        if (plan.outcome() == ActionResult.PUSHED) {
            grid.move(from, landing);
            return ActionResult.PUSHED;
        }

        // Collision : le poussé encaisse, et ce qu'il percute aussi s'il y a quelqu'un.
        if (grid.contains(landing)) {
            damage(landing, COLLISION_DAMAGE);
        }
        damage(from, COLLISION_DAMAGE);
        if (grid.occupantAt(from) instanceof Enemy enemy) {
            stun(enemy);
        }
        return ActionResult.COLLIDED;
    }

    /**
     * Étourdit un ennemi : il passera sa prochaine activation.
     *
     * <p>Son intention est <b>immédiatement</b> remplacée par « attend ». C'est indispensable :
     * l'étourdissement est provoqué par le joueur pendant son propre tour, donc l'affichage doit
     * cesser de promettre un coup qui ne partira plus. Le télégraphe reste vrai à chaque instant, et
     * pas seulement au début du tour.
     */
    void stun(Enemy enemy) {
        enemy.setStunned(true);
        enemy.announce(Intention.of(Intention.Kind.WAIT));
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

    /**
     * Vrai si une invocation annoncée fera apparaître quelqu'un sur cette case.
     *
     * <p>Séparé du compteur de coups, et pas par commodité : rien ne <em>tombe</em> sur cette case,
     * donc l'y mêler ferait mentir le nombre que le bandeau affiche et que le joueur lit pour
     * décider s'il reste. Deux dangers différents, deux comptes différents, deux formes différentes
     * au sol.
     */
    public boolean summonsAt(int cell) {
        for (Enemy enemy : enemiesLeftToRight()) {
            if (enemy.intention().kind() == Intention.Kind.SUMMON
                    && enemy.intention().targetCell() == cell) {
                return true;
            }
        }
        return false;
    }

    /** Vrai si au moins une invocation est annoncée quelque part sur le plateau. */
    public boolean anySummonAnnounced() {
        for (Enemy enemy : enemiesLeftToRight()) {
            if (enemy.intention().kind() == Intention.Kind.SUMMON) {
                return true;
            }
        }
        return false;
    }

    /**
     * Case où une ruée annoncée déplacera l'occupant de {@code cell}, ou {@code -1}.
     *
     * <p>Elle existe pour l'affichage, et elle comble un vrai trou : le joueur voyait qu'il allait
     * être poussé, jamais où il allait atterrir — alors que c'est là tout le coût de la ruée. Être
     * déplacé d'une case ne veut pas dire la même chose selon qu'on tombe au milieu du plateau ou
     * contre un mur, entre deux sbires.
     */
    public int announcedPushTo(int cell) {
        for (Enemy enemy : enemiesLeftToRight()) {
            Intention intention = enemy.intention();
            if (intention.kind() != Intention.Kind.RUSH || intention.targetCell() != cell) {
                continue;
            }
            int step = Integer.signum(intention.targetCell() - grid.indexOf(enemy));
            int destination = cell + step;
            return grid.contains(destination) ? destination : -1;
        }
        return -1;
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
        ActionResult refusal = refusalToStep(direction);
        if (refusal != null) {
            return refusal;
        }
        beginAction();
        if (hero.facing() != direction) {
            hero.face(direction);
            consumeTurn();
            return settle(ActionResult.TURNED);
        }

        int from = heroCell();
        grid.move(from, from + direction.step());
        consumeTurn();
        return settle(ActionResult.MOVED);
    }

    /**
     * Ce pas ferait-il quelque chose ?
     *
     * <p>Même motif que {@link #canQueue} : la question se pose <b>ici</b> et ne se recopie pas.
     * L'énumération de l'instrument de mesure proposait « pas droite » contre un mur — le jeu
     * répondait {@code BLOCKED}, aucun tour n'était consommé, et la politique avait dépensé son
     * geste pour rien. Son javadoc promettait pourtant qu'« on n'énumère pas les gestes que le jeu
     * refuserait de toute façon », et le test qui le garde s'appelle « tout geste énuméré est
     * accepté » sans avoir jamais regardé la réponse du jeu.
     *
     * <p>Un demi-tour passe toujours : il change l'orientation et consomme un tour, mur ou pas.
     */
    public boolean canStep(Direction direction) {
        return refusalToStep(direction) == null;
    }

    /** Le refus qu'opposerait {@link #step}, ou {@code null} si le geste passerait. */
    private ActionResult refusalToStep(Direction direction) {
        if (isOver()) {
            return ActionResult.BLOCKED;
        }
        if (hero.facing() != direction) {
            return null;
        }
        return grid.isFree(heroCell() + direction.step()) ? null : ActionResult.BLOCKED;
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
        if (isOver()) {
            return ActionResult.BLOCKED;
        }
        beginAction();
        int target = swapTarget();
        if (target < 0) {
            return ActionResult.NO_TARGET;
        }
        grid.swap(heroCell(), target);
        consumeTurn();
        return settle(ActionResult.SWAPPED);
    }

    // ------------------------------------------------------------------ file d'actions

    /**
     * Pose une tuile du râtelier sur la file, en <b>consommant un tour</b> — sauf si elle est
     * Free-Play.
     *
     * @return {@link ActionResult#QUEUED}, ou {@link ActionResult#QUEUE_FULL} / {@link
     *         ActionResult#NOT_READY} si le geste est impossible
     */
    /**
     * Une touche peut-elle poser cette tuile <b>maintenant</b> ?
     *
     * <p>Elle existe pour la même raison que {@link #clickable(int)} : l'interface ne doit promettre
     * que ce qu'elle tient, et la seule façon de ne pas mentir est de <b>poser la question au même
     * endroit</b> que celui qui exécute. La scène recopiait la règle et n'en connaissait que deux
     * tiers — elle testait la fin de partie et la recharge, jamais la <b>file pleine</b>. Elle
     * peignait donc la portée d'une tuile rechargée dans la couleur qui veut dire « ce que je vais
     * provoquer », sur un état où {@code queueTile} rend {@code QUEUE_FULL}.
     *
     * <p>C'était le sixième « symétrique » de la même famille, trouvé dans le commit qui en fermait
     * un cinquième, à seize lignes de la correction. Une règle recopiée finit toujours par diverger
     * de l'originale ; celle-ci n'est plus recopiée.
     */
    public boolean canQueue(Tile tile) {
        return refusalToQueue(tile) == null;
    }

    /** Le refus qu'opposerait {@link #queueTile}, ou {@code null} si le geste passerait. */
    private ActionResult refusalToQueue(Tile tile) {
        if (isOver()) {
            return ActionResult.BLOCKED;
        }
        if (queue.isFull()) {
            return ActionResult.QUEUE_FULL;
        }
        if (!rack.isReady(tile)) {
            return ActionResult.NOT_READY;
        }
        return null;
    }

    public ActionResult queueTile(Tile tile) {
        ActionResult refusal = refusalToQueue(tile);
        if (refusal != null) {
            return refusal;
        }
        beginAction();
        rack.take(tile);
        queue.push(tile);
        // C'est ICI que le temps passe désormais. Charger est le moment vulnérable : plus on
        // prépare, plus on encaisse avant de frapper. Une tuile Free-Play se glisse dans la file
        // sans rien coûter — le rôle de la soupape a suivi le coût, qui a changé de place.
        if (!tile.isFreePlay()) {
            consumeTurn();
        }
        return settle(ActionResult.QUEUED);
    }

    /**
     * Reprend une tuile de la file, désignée par sa position d'affichage — de la plus ancienne à la
     * plus récente. <b>Gratuit</b>, et la tuile ne part pas en recharge : elle n'a pas servi.
     *
     * <p>Poser coûte un tour ; reprendre n'en rend pas un. C'est le seul geste resté gratuit : le
     * joueur paie son engagement, pas le fait de changer d'avis sur une tuile qui n'a rien fait.
     *
     * <p><b>Il y a pourtant quelque chose à y gagner, et il vaut mieux le dire.</b> Poser puis
     * reprendre coûte un tour, ne dépense aucune tuile, ne déplace ni ne retourne le héros, et fait
     * avancer toutes les recharges d'un cran : c'est un <em>tour de passe</em> parfaitement neutre,
     * strictement meilleur que le demi-tour, qui lui retourne le héros. C'est le seul moyen
     * d'attendre sans bouger, donc un vrai outil tactique — la review l'a trouvé alors que le
     * javadoc affirmait le contraire. Il est laissé en place : sur un plateau où l'on gagne en
     * plaçant, savoir laisser passer un tour est une décision, pas une faille.
     */
    public ActionResult unqueueAt(int indexFromOldest) {
        // Ce geste NE demande PAS a canUnqueue, et c'est delibere. Il l'a fait un temps - la
        // deduplication paraissait evidente - et cela rendait le test de contrat entre les deux
        // rigoureusement vide dans un sens : « canUnqueue dit oui » et « unqueueAt ne rend pas
        // BLOCKED » etaient la MEME expression evaluee deux fois. Mesure : un canUnqueue rendu
        // trop restrictif (interdisant de reprendre la derniere tuile d'une file pleine) laissait
        // les 508 tests verts, y compris celui ecrit pour l'epingler.
        //
        // Deux implementations independantes qui doivent s'accorder valent mieux qu'une seule
        // consultee deux fois. C'est aussi ce que font step et queueTile : leur predicat et leur
        // geste passent par un refus PARTAGE, mais le predicat public n'est pas dans le chemin du
        // geste - muter le predicat seul les fait diverger, donc rougir.
        if (isOver()) {
            return ActionResult.BLOCKED;
        }
        Tile removed = queue.removeAt(indexFromOldest);
        if (removed == null) {
            return ActionResult.BLOCKED;
        }
        rack.giveBack(removed);
        return ActionResult.UNQUEUED;
    }

    /**
     * Ce retrait ferait-il quelque chose ?
     *
     * <p>Troisième de la famille, après {@link #canQueue} et {@link #canStep}, et le dernier geste
     * du joueur qui n'avait pas la sienne. Sa règle était énoncée à deux endroits hors de l'arène —
     * l'énumération de l'instrument de mesure et le survol de la scène — sous la forme réduite
     * « la partie n'est pas finie ».
     *
     * <p><b>Et cette forme réduite y était correcte</b>, ce qu'une review a mesuré après coup : les
     * deux sites sont enfermés dans un contexte qui garantit déjà l'emplacement — boucle bornée par
     * la taille de la file d'un côté, tuile survolée nécessairement présente de l'autre. J'avais
     * écrit ici qu'ils « ne connaissaient qu'un de ses deux refus », en laissant entendre une
     * divergence latente. Il n'y en avait pas. Poser la question une seule fois reste préférable —
     * un contexte qui garantit aujourd'hui peut cesser de garantir demain — mais l'affirmation était
     * plus large que la mesure.
     *
     * <p>Un seul refus visible de l'extérieur, {@code BLOCKED}, pour deux causes : c'est
     * volontaire, l'interface n'a pas à distinguer « la partie est finie » de « il n'y a rien
     * là ».
     */
    public boolean canUnqueue(int indexFromOldest) {
        return !isOver() && indexFromOldest >= 0 && indexFromOldest < queue.size();
    }

    /**
     * Lance la <b>salve</b> : toute la file part, du sommet vers le bas, pour un seul tour.
     *
     * <h2>Le décalage, enfin</h2>
     *
     * <p>C'est le geste qui donne son sens à la file, et il a manqué neuf jalons. Tant que poser
     * était gratuit et qu'exécuter coûtait un tour par tuile, cinq tuiles coûtaient cinq tours —
     * exactement comme cinq actions jouées une à une. La file n'était qu'une contrainte d'ordre
     * sans contrepartie, et aucun enchaînement ne pouvait en sortir puisque chaque exécution était
     * une action à elle seule.
     *
     * <p>Maintenant, <b>charger est le risque et lâcher est la récompense</b> : on paie un tour par
     * tuile posée, en encaissant tout ce que les ennemis annoncent pendant ce temps, puis les cinq
     * effets tombent alors qu'ils n'avancent que d'un cran. C'est aussi ce qui rend les
     * enchaînements possibles — toute la salve est <em>une seule action</em>, donc plusieurs morts
     * y comptent comme un combo.
     *
     * <p>Une salve dont <b>rien</b> ne porte ne coûte pas de tour : les tuiles sont dépensées et
     * partent en recharge, ce qui est déjà le prix de les avoir jouées, mais on ne fait pas payer
     * deux fois une décision déjà punie. C'est la règle de M5, transposée à la salve entière.
     *
     * <h2>Écart assumé avec la source</h2>
     *
     * <p>Shogun Showdown décrit l'exécution comme un processus « <b>tout-ou-rien</b> » qui
     * « s'enchaîne sans interruption », et il en fait un <em>risque de conception</em> : vider le
     * plateau déclenche aussitôt la vague suivante, si bien qu'« un joueur ayant vidé sa file et ses
     * cooldowns pour un combo spectaculaire peut se retrouver démuni ». La salve qui continue dans
     * le vide fait partie de la punition.
     *
     * <p>Ici elle s'arrête, et les tuiles restantes demeurent dans la file. Le choix est délibéré et
     * il a sa raison — la ligne d'annonce ne décrit que la <em>première</em> tuile, donc le
     * gaspillage serait invisible au moment de décider, ce qui en ferait une punition non
     * télégraphiée. Le télégraphe est l'invariant central de ce projet ; il l'emporte sur la
     * fidélité au détail. Mais c'est bien un écart, et le nommer vaut mieux que de le laisser
     * ressembler à un oubli.
     */
    public ActionResult unleash() {
        if (isOver()) {
            return ActionResult.BLOCKED;
        }
        beginAction();
        if (queue.isEmpty()) {
            return ActionResult.EMPTY_QUEUE;
        }

        boolean costly = false;
        ActionResult last = ActionResult.EMPTY_QUEUE;
        int fired = 0;
        // « Le terrain s'est vidé » n'a de sens que s'il y avait quelqu'un. Sur un plateau d'essai
        // sans ennemi, la salve doit partir en entier comme n'importe où ailleurs.
        boolean hadEnemies = !enemiesLeftToRight().isEmpty();
        while (!queue.isEmpty()) {
            Tile tile = queue.pop();
            // La case se lit AVANT l'exécution : après, la tuile a déjà déplacé le héros, et le
            // préavis décrirait le coup SUIVANT au lieu de celui qui vient de partir.
            //
            // La visée d'abord, l'arrivée ensuite. Une tuile qui frappe dans le VIDE garde sa case
            // visée — le préavis la conserve même quand rien ne s'y trouve — et c'est ce qu'il faut
            // montrer : voir l'estoc balayer une case déserte explique au joueur pourquoi rien ne
            // s'est produit. Les tuiles de mouvement, elles, n'ont pas de visée : c'est leur case
            // d'arrivée qui porte le temps, et c'est là qu'il y a le plus à voir.
            TilePreview preview = tile.preview(this);
            int cell = preview.aim() >= 0 ? preview.aim() : preview.landing();
            ActionResult result = tile.applyTo(this);
            // Le plateau APRES le coup : c'est l'effet de cette tuile-ci qu'il faut montrer, pas
            // l'état qui la précédait. Le premier temps montre donc déjà quelque chose, et le
            // dernier montre l'état final — celui que la scène retrouvera de toute façon au repos.
            // La file telle qu'elle est APRES ce coup : elle se vide tuile par tuile sous les
            // yeux, au lieu de se vider d'un bloc pendant que la salve s'égrène. C'est le même
            // décalage que celui des figures, sur l'autre moitié de l'écran.
            beats.add(new Beat(tile, result, cell, snapshot(), queue.fromOldest()));
            rack.giveBackSpent(tile);
            fired++;
            last = result;
            boolean connected = result != ActionResult.NO_TARGET && result != ActionResult.BLOCKED;
            // La soupape a suivi le coût. Free-Play voulait dire « s'exécute sans consommer de
            // tour » quand l'exécution était le moment payant ; maintenant que c'est le chargement,
            // une tuile Free-Play se pose gratuitement — et une salve qui ne contient qu'elles ne
            // coûte rien non plus. Se repositionner sans offrir un tour reste possible, ce qui
            // était toute la raison d'être de ces deux tuiles.
            costly |= connected && !tile.isFreePlay();
            if (isDefeat()) {
                break; // une salve ne continue pas après la mort de celle qui la lance
            }
            if (hadEnemies && enemiesLeftToRight().isEmpty()) {
                // Ni sur un plateau vide. Les tuiles suivantes partaient dans le vide, étaient
                // dépensées et mises en recharge sans que rien ne prévienne — la ligne d'annonce
                // ne décrit que la première. Le joueur payait un gaspillage qu'il ne pouvait pas
                // voir venir. Ce qui reste dans la file y reste, et servira à la vague suivante.
                break;
            }
        }

        if (costly) {
            consumeTurn();
        }
        // Une seule tuile partie garde son propre résultat : « salve » pour un coup unique se
        // lirait comme une grandiloquence, et le joueur veut savoir ce que sa tuile a fait.
        return settle(fired > 1 ? ActionResult.UNLEASHED : last);
    }

    /**
     * Ce que ferait la tuile du sommet si on l'exécutait maintenant, ou {@code null} si la file est
     * vide.
     *
     * <p>C'est la réponse à la seule question que le joueur se pose en regardant sa file :
     * <i>qu'est-ce qui part si j'appuie ?</i> Elle est calculée par le code qui exécute, pas par un
     * code parallèle qui le décrit — voir {@link TilePreview}.
     *
     * <p>Elle vaut {@code null} quand la partie est finie, comme les six portes d'action : promettre
     * un coup qu'aucune touche ne peut plus déclencher serait une promesse en trop.
     */
    public TilePreview previewTop() {
        Tile top = queue.top();
        if (top == null || isOver()) {
            return null;
        }
        return top.preview(this);
    }

    /** Ce que ferait une tuile donnée si elle partait maintenant. Sert à l'infobulle du râtelier. */
    public TilePreview preview(Tile tile) {
        return isOver() ? null : tile.preview(this);
    }

    /** Case que la capacité viserait maintenant, ou {@code -1}. Sert aussi à la télégraphier. */
    public int swapTarget() {
        // Aucune cible quand la partie est finie, et c'est le même principe que previewTop : ne pas
        // désigner ce qu'aucune touche ne peut plus atteindre. swapWithTarget rend déjà BLOCKED et
        // clickable rend déjà faux ; seule cette méthode continuait de nommer une case, si bien que
        // la scène éteignait le surlignage ET peignait par-dessus la pointe qui dit « celui-ci
        // vient à toi ». Une règle vraie à trois endroits sur quatre est une règle fausse.
        if (isOver()) {
            return -1;
        }
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
        // Le demi-tour et le pas sont délégués à step() plutôt que réécrits ici. La version
        // précédente dupliquait la logique de demi-tour, et c'est exactement ce qui a laissé le
        // clic échapper à la comptabilité des tours : se retourner coûtait un tour au clavier et
        // rien à la souris. Une règle écrite à deux endroits finit toujours par diverger.
        return switch (planClick(cell)) {
            case NOTHING -> ActionResult.BLOCKED;
            // Déjà tourné vers la cible de la capacité : l'échange est le geste attendu.
            case SWAP -> swapWithTarget();
            case TURN, STEP -> step(Direction.towards(heroCell(), cell));
        };
    }

    /**
     * Vrai si un clic sur cette case produirait quelque chose.
     *
     * <p>Elle existe pour l'interface, et pour une raison précise : le surlignage de survol
     * s'allumait sur <b>toutes</b> les cases du plateau, y compris celles qu'un clic ne pouvait pas
     * atteindre — derrière un ennemi, ou n'importe où une fois la partie finie. Le pointeur
     * promettait une action qui ne venait pas. L'objection date de M4 et traînait depuis quatre
     * jalons faute d'un endroit où poser la réponse.
     *
     * <p>Elle interroge le <em>même</em> plan que {@link #clickOn}, sans l'exécuter : c'est la seule
     * façon d'être sûr que ce qui s'allume est ce qui répond.
     */
    public boolean clickable(int cell) {
        return planClick(cell) != ClickPlan.NOTHING;
    }

    /** Ce qu'un clic sur une case déclencherait. */
    private enum ClickPlan { NOTHING, TURN, STEP, SWAP }

    private ClickPlan planClick(int cell) {
        if (isOver() || !grid.contains(cell) || cell == heroCell()) {
            return ClickPlan.NOTHING;
        }
        Direction direction = Direction.towards(heroCell(), cell);
        if (hero.facing() != direction) {
            return ClickPlan.TURN; // se retourner réussit toujours
        }
        if (cell == swapTarget()) {
            return ClickPlan.SWAP;
        }
        // Déjà tourné dans la bonne direction : il ne reste qu'un pas. La question « ce pas
        // ferait-il quelque chose » se pose à canStep et ne se recopie pas — elle l'était ici, à
        // l'identique, et c'était le jumeau de l'énumération de l'instrument de mesure. Ce projet
        // a laissé sept fois un correctif sans son symétrique ; celui-ci part avec.
        return canStep(direction) ? ClickPlan.STEP : ClickPlan.NOTHING;
    }
}
