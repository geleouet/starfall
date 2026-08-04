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
     * <p>Provisoire, et assumé comme tel : mesurée sans répit, la tranche était gagnable mais pas
     * jouable — zéro victoire sur trois cents parties jouées au hasard, et un jeu quasi optimal
     * n'arrivait au bout qu'avec deux points de vie sur cinq. Rien ne soignait entre les vagues,
     * donc la deuxième arrivait avec ce qui restait de la première. Le réaccordage complet est
     * l'affaire du jalon d'équilibrage.
     */
    public static final int WAVE_RESPITE = 1;

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
    /** Ouvre une action du joueur : c'est ici que le compteur de morts repart de zéro. */
    private void beginAction() {
        killsThisAction = 0;
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
        if (isOver()) {
            return ActionResult.BLOCKED;
        }
        beginAction();
        if (hero.facing() != direction) {
            hero.face(direction);
            consumeTurn();
            return settle(ActionResult.TURNED);
        }

        int from = heroCell();
        int to = from + direction.step();
        if (!grid.isFree(to)) {
            return ActionResult.BLOCKED;
        }
        grid.move(from, to);
        consumeTurn();
        return settle(ActionResult.MOVED);
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
     * Pose une tuile du râtelier sur la file. <b>Gratuit</b> : aucun tour consommé.
     *
     * @return {@link ActionResult#QUEUED}, ou {@link ActionResult#QUEUE_FULL} / {@link
     *         ActionResult#NOT_READY} si le geste est impossible
     */
    public ActionResult queueTile(Tile tile) {
        if (isOver()) {
            return ActionResult.BLOCKED;
        }
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
     * Exécute la tuile du sommet — la dernière posée.
     *
     * <p>La tuile part en recharge et le tour est consommé, <b>sauf</b> si elle est Free-Play. Une
     * tuile dont l'effet échoue — personne à frapper, poussée bloquée — est tout de même consommée
     * et rechargée : c'est le prix de l'avoir jouée. En revanche elle ne consomme pas de tour, pour
     * ne pas faire payer deux fois une décision déjà punie.
     */
    public ActionResult executeTop() {
        if (isOver()) {
            return ActionResult.BLOCKED;
        }
        beginAction();
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
        return settle(result);
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
        // Déjà tourné dans la bonne direction : il ne reste qu'un pas, et il faut que la case
        // suivante soit libre. Cliquer au-delà d'un occupant ne mène nulle part.
        return grid.isFree(heroCell() + direction.step()) ? ClickPlan.STEP : ClickPlan.NOTHING;
    }
}
