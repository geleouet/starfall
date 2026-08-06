package com.starfall.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Le souverain ajoute deux verbes au jeu — la <b>ruée</b> et l'<b>invocation</b> — et chacun ouvre
 * une nouvelle façon pour le télégraphe de mentir.
 *
 * <p>Une invocation annonce une case ; si quelqu'un d'autre s'y pose, elle échoue et l'annonce
 * devient fausse. Une ruée annonce un coup <em>et</em> un déplacement ; si le déplacement ajoutait
 * un dégât dans certaines géométries, le compte des coups affiché cesserait d'être le compte des
 * coups reçus. Les deux défauts sont ceux que les jalons précédents ont payés, transposés.
 */
class SovereignTest {

    /** Un souverain seul, à distance choisie du héros. */
    private static Arena arena(int width, int heroCell, int bossCell) {
        Arena arena = new Arena(width, heroCell);
        arena.grid().place(bossCell, new Enemy(EnemyKind.SOUVERAIN));
        arena.announceIntentions();
        return arena;
    }

    /**
     * Un souverain qui annonce une invocation.
     *
     * <p>Il alterne une phase sur deux, donc le montage doit avancer d'une phase si la première
     * tombe sur une ruée. Écrire « il invoque quand il est loin » ne suffirait pas : ce serait
     * décrire l'ancienne version, celle qui n'invoquait jamais.
     */
    private static Arena summoning(int width, int heroCell, int bossCell) {
        Arena arena = arena(width, heroCell, bossCell);
        if (sovereignIn(arena).intention().kind() != Intention.Kind.SUMMON) {
            arena.step(arena.hero().facing().opposite());
        }
        assertEquals(Intention.Kind.SUMMON, sovereignIn(arena).intention().kind(),
                "montage invalide : le souverain doit annoncer une invocation");
        return arena;
    }

    private static Enemy sovereignIn(Arena arena) {
        for (Enemy enemy : arena.enemies()) {
            if (enemy.kind() == EnemyKind.SOUVERAIN) {
                return enemy;
            }
        }
        return null;
    }

    /**
     * La grammaire de l'archétype : au contact il frappe, sinon il alterne invocation et ruée.
     *
     * <p>L'alternance est ce qui rend la rencontre jouable, et elle a été trouvée en jouant : sans
     * elle, la ruée repoussait le héros à deux cases, ce qui recréait les conditions d'une nouvelle
     * ruée. Le souverain fonçait à chaque tour et n'invoquait <em>jamais</em>.
     */
    @Test
    @DisplayName("Au contact il frappe ; sinon il alterne invocation et ruée")
    void theSovereignStrikesUpCloseAndAlternatesOtherwise() {
        assertEquals(Intention.Kind.ATTACK, arena(11, 5, 6).enemies().get(0).intention().kind());

        Arena arena = arena(11, 5, 9);
        Enemy boss = sovereignIn(arena);
        List<Intention.Kind> seen = new ArrayList<>();
        for (int turn = 0; turn < 6 && !arena.isOver(); turn++) {
            seen.add(boss.intention().kind());
            arena.step(arena.hero().facing().opposite());
        }

        assertTrue(seen.contains(Intention.Kind.SUMMON),
                "il doit invoquer au moins une fois : " + seen);
        assertTrue(seen.contains(Intention.Kind.RUSH),
                "il doit foncer au moins une fois : " + seen);
    }

    @Test
    @DisplayName("L'invocation apparaît derrière le héros, du côté opposé au souverain")
    void theSummonAppearsBehindTheHero() {
        Arena arena = summoning(11, 5, 8);
        Enemy boss = sovereignIn(arena);
        int hero = arena.heroCell();
        int away = Integer.signum(hero - arena.grid().indexOf(boss));
        int expected = hero + away;

        assertEquals(expected, boss.intention().targetCell(),
                "l'invocation doit tomber derriere le heros, du cote oppose au souverain");
        assertTrue(arena.summonsAt(expected), "la case doit etre marquee au sol");
        assertTrue(!arena.isThreatened(expected), "une invocation ne fait tomber aucun coup");
        assertEquals(0, arena.threatCount(expected),
                "elle ne doit pas compter dans les coups annonces");
    }

    /**
     * La contrepartie du télégraphe : une case annoncée un tour à l'avance doit pouvoir être
     * refusée. Et refuser coûte l'invocation au souverain, sans quoi refuser ne ferait que retarder.
     */
    @Test
    @DisplayName("Occuper la case annoncée refuse l'invocation, et la consomme")
    void standingOnTheAnnouncedCellDeniesTheSummon() {
        // Le heros regarde deja du bon cote : il n'a qu'UN tour entre l'annonce et l'execution,
        // donc il doit pouvoir se poser sur la case en un seul geste.
        Arena arena = summoning(11, 5, 8);
        arena.hero().face(Direction.LEFT);

        Enemy boss = sovereignIn(arena);
        int cell = boss.intention().targetCell();
        int before = boss.summonsLeft();

        arena.step(Direction.LEFT); // il se pose sur la case annoncee

        assertEquals(cell, arena.heroCell(), "le heros doit occuper la case annoncee");
        assertEquals(before - 1, boss.summonsLeft(), "l'invocation refusee est perdue");
    }

    @Test
    @DisplayName("Une invocation laissée libre fait bien apparaître un ennemi")
    void anUncontestedSummonBringsSomeoneIn() {
        Arena arena = summoning(11, 5, 8);
        int before = arena.enemies().size();

        arena.step(arena.hero().facing().opposite()); // demi-tour : un tour passe

        assertEquals(before + 1, arena.enemies().size(), "personne n'est apparu");
    }

    /**
     * Le budget est un compte à rebours, pas une source.
     *
     * <p>Une invocation sans plafond ne pose aucune question : elle transforme une rencontre en
     * course contre la montre, où la seule réponse est de tuer vite — le contraire d'un jeu de
     * placement. Épuisé, le souverain redevient un ennemi de plus, et c'est ce qui rend la fin de
     * la rencontre lisible.
     */
    @Test
    @DisplayName("Budget épuisé, le souverain n'annonce plus d'invocation")
    void theSovereignStopsSummoningOnceItsBudgetIsSpent() {
        int budget = EnemyKind.SOUVERAIN.summons();
        assertTrue(budget > 0 && budget <= 3,
                "un budget hors de toute mesure ne pose plus de question : " + budget);

        Arena arena = summoning(15, 7, 12);
        Enemy boss = sovereignIn(arena);
        // On depense le budget a la main : ce test porte sur l'epuisement, pas sur la poursuite.
        for (int i = 0; i < budget; i++) {
            boss.spendSummon();
        }
        arena.announceIntentions();

        assertEquals(0, boss.summonsLeft());
        assertTrue(boss.intention().kind() != Intention.Kind.SUMMON,
                "budget epuise : il ne doit plus rien annoncer de tel, obtenu "
                        + boss.intention().kind());
    }

    /**
     * La ruée est une charge : elle s'arrête au premier obstacle et ne frappe pas si l'on
     * s'interpose. C'est la contrepartie que M7 avait fait ajouter aux charges du lancier, et elle
     * doit valoir pour le boss aussi.
     */
    @Test
    @DisplayName("Une ruée interceptée s'arrête et ne frappe pas")
    void aBlockedRushStopsAndDoesNotStrike() {
        Arena arena = new Arena(11, 4);
        Enemy boss = new Enemy(EnemyKind.SOUVERAIN);
        arena.grid().place(6, boss);
        arena.announceIntentions();
        assertEquals(Intention.Kind.RUSH, boss.intention().kind());

        arena.grid().place(5, new Enemy(EnemyKind.SABREUR));
        int before = arena.heroHits();

        arena.step(Direction.LEFT); // demi-tour : la ruee part

        assertEquals(before, arena.heroHits(), "une ruee interceptee ne frappe pas");
        assertEquals(6, arena.grid().indexOf(boss), "elle s'arrete avant l'obstacle");
    }

    /**
     * L'invariant central du jalon, et celui qui a décidé de la règle : <b>la poussée de la ruée ne
     * blesse pas</b>, même contre un mur.
     *
     * <p>Un dégât conditionnel à la géométrie ne peut pas être annoncé un tour à l'avance sans
     * risquer de sur-promettre — le joueur peut toujours quitter le mur entre l'annonce et
     * l'exécution. Le compte des coups affiché resterait alors supérieur au compte des coups reçus,
     * ce qui est exactement le mensonge que les deux jalons précédents ont coûté à éteindre.
     */
    @Test
    @DisplayName("La ruée déplace mais ne blesse qu'une fois, mur ou pas")
    void theRushDisplacesButOnlyEverStrikesOnce() {
        for (int heroCell : new int[]{0, 1, 5}) {
            Arena arena = new Arena(11, heroCell);
            Enemy boss = new Enemy(EnemyKind.SOUVERAIN);
            arena.grid().place(heroCell + 2, boss);
            arena.announceIntentions();
            assertEquals(Intention.Kind.RUSH, boss.intention().kind(),
                    "montage invalide pour un heros en " + (heroCell + 1));

            int announced = arena.threatCount(arena.heroCell());
            int healthBefore = arena.hero().health();
            arena.step(arena.hero().facing().opposite());

            assertEquals(1, announced, "une ruee annonce un coup et un seul");
            // Un COUP, et le prix de ce coup. Les deux etaient le meme nombre tant que tout coup
            // ennemi retirait un point ; mesurer des coups en points de vie ferait desormais lire
            // « deux blessures » la ou il y a une frappe qui en coute deux.
            assertEquals(boss.announcedDamage(), healthBefore - arena.hero().health(),
                    "heros en " + (heroCell + 1) + " : la ruee a coute plus que le coup annonce");
        }
    }

    @Test
    @DisplayName("La ruée repousse le héros quand la place est libre")
    void theRushPushesTheHeroWhenThereIsRoom() {
        Arena arena = new Arena(11, 5);
        Enemy boss = new Enemy(EnemyKind.SOUVERAIN);
        arena.grid().place(3, boss);
        arena.announceIntentions();
        assertEquals(Intention.Kind.RUSH, boss.intention().kind());

        arena.step(arena.hero().facing().opposite());

        assertEquals(6, arena.heroCell(), "le heros doit avoir ete repousse d'une case");
        assertEquals(4, arena.grid().indexOf(boss),
                "le souverain doit s'etre arrete au contact, pas sur la case du heros");
    }

    /**
     * Le télégraphe complet sur la rencontre : ce qui est annoncé sur la case du héros est
     * <b>exactement</b> ce qu'il encaisse.
     *
     * <p>La première version de ce test sautait les tours où le héros avait changé de case — donc
     * précisément tous ceux où la ruée s'était déclenchée, puisqu'elle <em>pousse</em>. Elle ne
     * mesurait plus que les tours sans ruée, c'est-à-dire tout sauf la mécanique vedette du jalon.
     * Elle ne vérifiait pas non plus l'égalité, seulement l'absence de sur-promesse — alors que les
     * jalons précédents ont été refusés pour des <b>sous</b>-promesses.
     *
     * <p>C'est réparable parce que la poussée est désormais différée à la fin de la phase : les
     * coups annoncés tombent tous sur la case annoncée, et le déplacement vient après. Le demi-tour
     * reste la seule action qui consomme un tour sans que le héros bouge de son propre fait.
     */
    @Test
    @DisplayName("Le télégraphe tient exactement face au souverain et à ses invocations")
    void theTelegraphHoldsExactlyAgainstTheSovereign() {
        List<String> failures = new ArrayList<>();

        for (int seed = 0; seed < 400 && failures.size() <= 5; seed++) {
            Random random = new Random(seed);
            int width = Grid.MIN_WIDTH + random.nextInt(Grid.MAX_WIDTH - Grid.MIN_WIDTH + 1);
            Arena arena = ArenaSetup.trainingArena(width, Arena.WAVE_COUNT);

            for (int turn = 0; turn < 50 && !arena.isOver(); turn++) {
                int announced = arena.threatCount(arena.heroCell());
                int hitsBefore = arena.heroHits();
                int turnsBefore = arena.turnsTaken();

                arena.step(arena.hero().facing().opposite()); // demi-tour : il ne bouge pas
                if (arena.turnsTaken() == turnsBefore) {
                    continue;
                }
                int taken = arena.heroHits() - hitsBefore;
                if (taken != announced) {
                    failures.add("graine " + seed + " tour " + turn + " : " + announced
                            + " coup(s) annonce(s), " + taken + " encaisse(s)");
                }
            }
        }

        assertTrue(failures.isEmpty(), "le telegraphe ment :\n  " + String.join("\n  ", failures));
    }

    /**
     * Le cas que la poussée différée corrige : un souverain <b>à gauche</b> d'un autre ennemi qui
     * vise la même case.
     *
     * <p>Appliquée sur-le-champ, la poussée sortait le héros de la case avant que l'ennemi suivant
     * ne frappe : le bandeau annonçait deux coups, un seul tombait, et la différence tenait à
     * l'ordre d'exécution — invisible. Le miroir exact du même montage en donnait deux. Le même
     * chiffre affiché, deux résultats.
     */
    @Test
    @DisplayName("La poussée de la ruée ne fait pas rater les coups annoncés après elle")
    void theRushPushDoesNotMakeLaterAnnouncedBlowsMiss() {
        for (boolean sovereignFirst : new boolean[]{true, false}) {
            Arena arena = new Arena(11, 5);
            int left = sovereignFirst ? 3 : 7;
            int right = sovereignFirst ? 7 : 3;
            arena.grid().place(left, new Enemy(EnemyKind.SOUVERAIN));
            arena.grid().place(right, new Enemy(EnemyKind.ARCHER));
            arena.announceIntentions();

            int announced = arena.threatCount(arena.heroCell());
            // Le prix annonce se lit AVANT le geste, comme le compte de coups : apres, les ennemis
            // ont deja annonce le tour suivant et le nombre ne decrit plus le coup qu'on mesure.
            int announcedDamage = arena.threatDamage(arena.heroCell());
            int before = arena.hero().health();
            arena.step(arena.hero().facing().opposite());

            assertEquals(2, announced, "montage invalide : deux coups doivent etre annonces");
            assertEquals(announcedDamage, before - arena.hero().health(),
                    "souverain " + (sovereignFirst ? "a gauche" : "a droite")
                            + " : le meme chiffre affiche doit donner le meme resultat");
        }
    }

    /**
     * L'invariant qui rend l'invocation jouable : la case annoncée est <b>adjacente au héros</b>.
     *
     * <p>Elle est annoncée un tour à l'avance pour qu'on puisse la refuser en l'occupant. Si elle
     * n'est pas à un geste, l'annonce n'est plus une décision, c'est un compte à rebours. La
     * première version se rabattait derrière le souverain quand la case derrière le héros était
     * prise : mesuré, 56 % des invocations de la vague livrée tombaient là, et 57 % des annonces
     * étaient irréfusables — le boss en travers du chemin.
     */
    @Test
    @DisplayName("Toute invocation annoncée est adjacente au héros, donc refusable")
    void everyAnnouncedSummonSitsNextToTheHero() {
        List<String> failures = new ArrayList<>();
        int seen = 0;

        for (int seed = 0; seed < 400 && failures.size() <= 5; seed++) {
            Random random = new Random(seed);
            int width = Grid.MIN_WIDTH + random.nextInt(Grid.MAX_WIDTH - Grid.MIN_WIDTH + 1);
            Arena arena = ArenaSetup.trainingArena(width, Arena.WAVE_COUNT);

            for (int turn = 0; turn < 40 && !arena.isOver(); turn++) {
                for (Enemy enemy : arena.enemies()) {
                    if (enemy.intention().kind() != Intention.Kind.SUMMON) {
                        continue;
                    }
                    seen++;
                    int distance = Math.abs(enemy.intention().targetCell() - arena.heroCell());
                    if (distance != 1) {
                        failures.add("graine " + seed + " tour " + turn + " : invocation en "
                                + (enemy.intention().targetCell() + 1) + ", heros en "
                                + (arena.heroCell() + 1) + " (distance " + distance + ")");
                    }
                }
                arena.step(random.nextBoolean() ? Direction.LEFT : Direction.RIGHT);
            }
        }

        assertTrue(failures.isEmpty(),
                "invocations irrefusables :\n  " + String.join("\n  ", failures));
        assertTrue(seen > 200, "le fuzz n'a vu que " + seen + " invocations : trop peu pour prouver"
                + " quoi que ce soit sur le cas que ce test protege");
    }

    /**
     * Le <b>repli</b> : quand la case derrière le héros est prise, l'invocation tombe devant lui,
     * entre le héros et le souverain. Toujours adjacente, donc toujours refusable.
     *
     * <h3>Ce test n'exécutait aucune assertion</h3>
     *
     * <p>Sa première version posait deux ennemis à la main puis enfermait son assertion dans un
     * {@code if (kind == SUMMON)} — et la mise en scène rendait cette condition <b>impossible</b> :
     * le souverain, en ligne dégagée, annonçait une ruée, arrivait au contact, et annonçait dès lors
     * une attaque pour toujours. Une review l'a prouvé en remplaçant le corps du {@code if} par un
     * échec : jamais atteint. Puis, en cassant l'invariant lui-même, le test restait vert.
     *
     * <p>Deux causes, et toutes deux instructives. Le souverain n'invoque qu'<b>une phase sur
     * deux</b> — un montage figé ne tombe pas dessus par hasard. Et la case occupée pour forcer le
     * repli l'était du mauvais côté : {@code toward} part du <em>souverain</em>, pas du héros.
     *
     * <p>Un {@code if} dans un test est un aveu : ou bien la situation est garantie et il est
     * inutile, ou bien elle ne l'est pas et il faut le dire. Cette version-ci <b>cherche</b> le
     * repli sur de vraies parties, comme son voisin, et échoue si elle ne le voit jamais.
     */
    @Test
    @DisplayName("Le repli devant le héros reste adjacent, donc refusable")
    void theFallbackInFrontOfTheHeroStaysAdjacent() {
        List<String> failures = new ArrayList<>();
        int fallbacks = 0;

        for (int seed = 0; seed < 400 && failures.size() <= 5; seed++) {
            Random random = new Random(seed);
            int width = Grid.MIN_WIDTH + random.nextInt(Grid.MAX_WIDTH - Grid.MIN_WIDTH + 1);
            Arena arena = ArenaSetup.trainingArena(width, Arena.WAVE_COUNT);

            for (int turn = 0; turn < 40 && !arena.isOver(); turn++) {
                for (Enemy enemy : arena.enemies()) {
                    if (enemy.kind() != EnemyKind.SOUVERAIN
                            || enemy.intention().kind() != Intention.Kind.SUMMON) {
                        continue;
                    }
                    int hero = arena.heroCell();
                    int target = enemy.intention().targetCell();
                    int sovereign = arena.grid().indexOf(enemy);
                    // Le repli, c'est l'invocation posée DU CÔTÉ du souverain : la case derrière le
                    // héros, à l'opposé, était prise.
                    if (Integer.signum(target - hero) != Integer.signum(sovereign - hero)) {
                        continue;
                    }
                    fallbacks++;
                    if (Math.abs(target - hero) != 1) {
                        failures.add("graine " + seed + " tour " + turn + " : repli en "
                                + (target + 1) + ", heros en " + (hero + 1) + " (distance "
                                + Math.abs(target - hero) + ")");
                    }
                }
                arena.step(random.nextBoolean() ? Direction.LEFT : Direction.RIGHT);
            }
        }

        assertTrue(failures.isEmpty(), "replis irrefusables :\n  " + String.join("\n  ", failures));
        assertTrue(fallbacks > 0,
                "aucun repli observé en 400 parties : ce test ne garde rien, et c'est exactement"
                        + " le defaut qu'il a deja eu");
    }

    /**
     * Un sbire invoqué doit <b>faire quelque chose</b>.
     *
     * <p>Né derrière le souverain, il se retrouvait coincé derrière lui sur une grille linéaire :
     * mesuré, 47 % des sbires annonçaient « attend », dont 93 % pour cette raison. Le souverain ne
     * prenait pas en tenaille, il fabriquait une file d'attente — et le joueur payait une invocation
     * qu'il ne pouvait pas refuser pour un ennemi inerte.
     */
    @Test
    @DisplayName("Un sbire invoqué n'est pas inerte")
    void aSummonedMinionIsNotInert() {
        int waiting = 0;
        int total = 0;

        for (int seed = 0; seed < 200; seed++) {
            Random random = new Random(seed);
            Arena arena = ArenaSetup.trainingArena(11, Arena.WAVE_COUNT);

            for (int turn = 0; turn < 30 && !arena.isOver(); turn++) {
                arena.step(random.nextBoolean() ? Direction.LEFT : Direction.RIGHT);
                for (Enemy enemy : arena.enemies()) {
                    // Les sbires sont des sabreurs ; l'escorte de la vague est un archer.
                    if (enemy.kind() != EnemyKind.SABREUR) {
                        continue;
                    }
                    total++;
                    if (enemy.intention().kind() == Intention.Kind.WAIT) {
                        waiting++;
                    }
                }
            }
        }

        assertTrue(total > 100, "montage invalide : " + total + " observations de sbire");
        assertTrue(waiting * 4 < total, "trop de sbires inertes : " + waiting + " sur " + total
                + " annoncent « attend »");
    }

    /**
     * La ruée doit porter autant de coups qu'une charge, parce que le compteur de menaces les
     * compte. Elle n'en portait qu'un : latent tant que le souverain n'a pas de trait, mais le
     * jalon suivant est celui de l'équilibrage, et la table des vagues est une donnée.
     */
    @Test
    @DisplayName("Une ruée rapide porte les deux coups qu'elle annonce")
    void aFastRushLandsBothAnnouncedBlows() {
        Arena arena = new Arena(11, 5);
        arena.grid().place(7, new Enemy(EnemyKind.SOUVERAIN, Trait.RAPIDE));
        arena.announceIntentions();

        int announced = arena.threatCount(arena.heroCell());
        int before = arena.heroHits();
        arena.step(arena.hero().facing().opposite());

        assertEquals(2, announced, "un souverain rapide doit annoncer deux coups");
        assertEquals(announced, arena.heroHits() - before, "et les porter tous les deux");
    }

    /**
     * Une invocation ne doit jamais se poser dans le couloir d'une charge annoncée.
     *
     * <p>Un lancier en élan tient sa promesse quoi qu'il arrive et ne consulte donc aucune
     * réservation : c'est aux autres de lui laisser la place. Si l'invocation s'annonçait avant lui,
     * elle pouvait choisir une case au milieu de son couloir — et le sbire qui s'y levait
     * interceptait la charge. Mot pour mot la faute que le jalon précédent a payée avec un allié.
     */
    @Test
    @DisplayName("Aucune invocation ne se pose dans le couloir d'une charge annoncée")
    void noSummonLandsInsideAnAnnouncedChargeCorridor() {
        for (int bossCell = 0; bossCell <= 2; bossCell++) {
            Arena arena = new Arena(11, 4);
            arena.grid().place(bossCell, new Enemy(EnemyKind.SOUVERAIN));
            arena.grid().place(8, new Enemy(EnemyKind.LANCIER));
            arena.announceIntentions();

            for (int turn = 0; turn < 6 && !arena.isOver(); turn++) {
                for (Enemy charger : arena.enemies()) {
                    if (charger.intention().kind() != Intention.Kind.CHARGE) {
                        continue;
                    }
                    int from = arena.grid().indexOf(charger);
                    int to = charger.intention().targetCell();
                    int step = Integer.signum(to - from);
                    for (Enemy summoner : arena.enemies()) {
                        if (summoner.intention().kind() != Intention.Kind.SUMMON) {
                            continue;
                        }
                        int cell = summoner.intention().targetCell();
                        for (int inside = from + step; inside != to; inside += step) {
                            assertTrue(cell != inside,
                                    "boss en " + (bossCell + 1) + " tour " + turn
                                            + " : invocation en " + (cell + 1)
                                            + ", dans le couloir de charge " + (from + 1)
                                            + " vers " + (to + 1));
                        }
                    }
                }
                arena.hero().face(Direction.LEFT);
                arena.step(Direction.RIGHT);
            }
        }
    }

    /** Deux souverains ne doivent jamais viser la même case : le code les autorise. */
    @Test
    @DisplayName("Deux souverains n'invoquent jamais sur la même case")
    void twoSovereignsNeverSummonOntoTheSameCell() {
        int rivalries = 0;

        for (int seed = 0; seed < 200; seed++) {
            Random random = new Random(seed);
            // Les deux souverains du MEME cote, le heros loin. Le montage precedent les posait
            // de part et d'autre : ils fonçaient au premier tour, arrivaient au contact, et
            // attaquaient jusqu'a la mort du heros. Mesure sur deux cents parties : ATTACK et RUSH
            // seulement, et pas UNE SEULE invocation. Le test nomme d'apres la collision de deux
            // invocations n'en avait donc jamais vu une. Du meme cote et a distance, les deux visent
            // la meme case derriere le heros : c'est ce que la reservation doit departager.
            Arena arena = new Arena(15, 11);
            arena.grid().place(0, new Enemy(EnemyKind.SOUVERAIN));
            arena.grid().place(1, new Enemy(EnemyKind.SOUVERAIN));
            arena.announceIntentions();

            for (int turn = 0; turn < 20 && !arena.isOver(); turn++) {
                List<Integer> cells = new ArrayList<>();
                for (Enemy enemy : arena.enemies()) {
                    if (enemy.intention().kind() == Intention.Kind.SUMMON) {
                        cells.add(enemy.intention().targetCell());
                    }
                }
                if (cells.size() >= 2) {
                    rivalries++;
                }
                assertEquals(cells.size(), new java.util.HashSet<>(cells).size(),
                        "graine " + seed + " tour " + turn + " : deux invocations sur " + cells);
                arena.step(random.nextBoolean() ? Direction.LEFT : Direction.RIGHT);
            }
        }

        // Sans ce compte, l'assertion ci-dessus est vraie pour rien : une liste de zero ou une
        // cellule n'a jamais de doublon. Elle ne garde quelque chose que les tours ou les DEUX
        // souverains annoncent une invocation, et rien ne disait qu'un tel tour existait.
        assertTrue(rivalries > 0,
                "aucun tour ou les deux souverains invoquent en meme temps : l'assertion de"
                        + " doublon n'a jamais rien compare, et casser la reservation la"
                        + " laisserait verte");
    }

    /**
     * Une vague de départ hors bornes ne doit pas sortir de la table.
     *
     * <p>{@code startAtWave} bornait, et la ligne suivante rappelait l'argument brut : deux façons
     * de borner à deux lignes d'intervalle, dont une seule marchait.
     */
    @Test
    @DisplayName("Une vague de départ hors bornes est ramenée dans la table")
    void anOutOfRangeStartingWaveIsClamped() {
        for (int wave : new int[]{-3, 0, 1, Arena.WAVE_COUNT, Arena.WAVE_COUNT + 5}) {
            Arena arena = ArenaSetup.trainingArena(11, wave);
            assertTrue(arena.wave() >= 1 && arena.wave() <= Arena.WAVE_COUNT,
                    "vague " + wave + " a donne " + arena.wave());
            assertTrue(!arena.enemies().isEmpty(), "vague " + wave + " : personne n'est arrive");
        }
    }

    /**
     * Une invocation annoncée réserve sa case : aucun allié ne doit pouvoir s'y poser. Sans cela
     * l'annonce échouerait sans que le joueur y soit pour rien — la même faute que l'allié qui
     * interceptait une charge, transposée à l'invocation.
     */
    @Test
    @DisplayName("Aucun allié ne se pose sur la case d'une invocation annoncée")
    void noAllyStepsOntoAnAnnouncedSummonCell() {
        int pairs = 0;

        for (int seed = 0; seed < 300; seed++) {
            Random random = new Random(seed);
            int width = Grid.MIN_WIDTH + random.nextInt(Grid.MAX_WIDTH - Grid.MIN_WIDTH + 1);
            Arena arena = new Arena(width, width / 2);
            arena.grid().place(0, new Enemy(EnemyKind.SABREUR));
            arena.grid().place(width - 1, new Enemy(EnemyKind.SOUVERAIN));
            arena.announceIntentions();

            for (int turn = 0; turn < 30 && !arena.isOver(); turn++) {
                arena.step(random.nextBoolean() ? Direction.LEFT : Direction.RIGHT);

                for (Enemy summoner : arena.enemies()) {
                    if (summoner.intention().kind() != Intention.Kind.SUMMON) {
                        continue;
                    }
                    int cell = summoner.intention().targetCell();
                    for (Enemy other : arena.enemies()) {
                        if (other != summoner
                                && other.intention().kind() == Intention.Kind.ADVANCE) {
                            pairs++;
                            assertTrue(other.intention().targetCell() != cell,
                                    "graine " + seed + " tour " + turn + " : " + other.label()
                                            + " vise la case reservee " + (cell + 1));
                        }
                    }
                }
            }
        }

        // Meme raison qu'a cote : l'assertion ne s'execute que pour un couple « invocateur qui
        // annonce, camarade qui avance ». Si le montage n'en produit aucun, elle garde le vide.
        assertTrue(pairs > 0,
                "aucun camarade en approche pendant qu'une invocation est annoncee : la"
                        + " reservation n'a jamais ete mise a l'epreuve");
    }

    /** La rencontre est la dernière vague : on doit pouvoir y arriver, et la victoire doit exister. */
    @Test
    @DisplayName("Le souverain ferme la tranche")
    void theSovereignClosesTheSlice() {
        Arena arena = ArenaSetup.trainingArena(11);
        assertEquals(4, arena.waveCount(), "la tranche doit compter quatre vagues");

        // On pose directement la derniere vague : l'enchainement des vagues a ses propres tests,
        // celui-ci ne porte que sur sa composition.
        Arena last = new Arena(15, 7);
        WaveTable.spawn(last, last.waveCount());
        boolean sovereignPresent = last.enemies().stream()
                .anyMatch(e -> e.kind() == EnemyKind.SOUVERAIN);
        assertTrue(sovereignPresent, "la derniere vague doit amener le souverain");
        assertTrue(last.enemies().size() >= 2, "il ne doit pas venir tout a fait seul");
    }

    /**
     * Les invocations d'un chef abattu disparaissent avec lui.
     *
     * <p>Règle systémique de la source, absente d'ici jusqu'à cette passe sur les captures :
     * « les invocations disparaissent instantanément à la mort du leader, économisant ainsi des
     * dizaines de tours d'action potentiellement mortels ». Elle porte un axe de jeu entier —
     * ignorer les sbires pour saturer le chef — qui n'existait pas sans elle.
     *
     * <p>Le test épingle aussi le <b>symétrique</b>, et c'est la moitié qui compte le plus ici :
     * un ennemi arrivé avec sa vague ne doit <em>pas</em> disparaître. Une implémentation qui
     * viderait le plateau satisferait la première moitié et détruirait le jeu.
     */
    @Test
    @DisplayName("Les invocations disparaissent avec leur chef, les autres restent")
    void summonsVanishWithTheirLeaderButTheOthersStay() {
        Arena arena = new Arena(11, 5);
        Enemy sovereign = new Enemy(EnemyKind.SOUVERAIN);
        arena.grid().place(1, sovereign);
        Enemy bystander = new Enemy(EnemyKind.SABREUR);
        arena.grid().place(9, bystander);
        arena.announceIntentions();

        // On laisse le souverain invoquer : une phase sur deux, donc au plus quelques tours.
        int summoned = -1;
        for (int turn = 0; turn < 12 && summoned < 0; turn++) {
            int announced = -1;
            for (Enemy enemy : arena.enemies()) {
                if (enemy.intention().kind() == Intention.Kind.SUMMON) {
                    // La case est lue AVANT le pas : la phase ennemie execute l'intention puis en
                    // reannonce une neuve, si bien qu'apres coup on lirait la suivante.
                    announced = enemy.intention().targetCell();
                }
            }
            arena.step(arena.hero().facing().opposite());
            if (announced >= 0) {
                summoned = announced;
            }
        }
        assertTrue(summoned >= 0, "le souverain n'a jamais invoque : ce test ne garde rien");
        assertTrue(arena.grid().occupantAt(summoned) instanceof Enemy,
                "l'invocation devait se lever en " + (summoned + 1));

        arena.kill(arena.grid().indexOf(sovereign));

        assertTrue(arena.grid().isFree(summoned),
                "le sbire invoque est encore en " + (summoned + 1) + " apres la mort de son chef :"
                        + " la seule ligne jouable redevient « tout nettoyer »");
        assertSame(bystander, arena.grid().occupantAt(arena.grid().indexOf(bystander)),
                "un ennemi arrive avec sa vague a disparu lui aussi : la regle emporte plus que"
                        + " les invocations, ce qui viderait le plateau");
    }
}
