package com.starfall.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
            assertEquals(1, healthBefore - arena.hero().health(),
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
     * Le télégraphe complet, sur la rencontre du souverain : ce qui est annoncé sur la case du héros
     * est exactement ce qu'il encaisse. Le fuzz est le seul moyen de couvrir les combinaisons de
     * ruées, d'invocations et de sbires qui s'ajoutent.
     */
    @Test
    @DisplayName("Le télégraphe tient face au souverain et à ses invocations")
    void theTelegraphHoldsAgainstTheSovereign() {
        List<String> failures = new ArrayList<>();

        for (int seed = 0; seed < 400 && failures.size() <= 5; seed++) {
            Random random = new Random(seed);
            int width = Grid.MIN_WIDTH + random.nextInt(Grid.MAX_WIDTH - Grid.MIN_WIDTH + 1);
            Arena arena = new Arena(width, width / 2);
            arena.grid().place(width - 1, new Enemy(EnemyKind.SOUVERAIN));
            arena.announceIntentions();

            for (int turn = 0; turn < 50 && !arena.isOver(); turn++) {
                int announced = arena.threatCount(arena.heroCell());
                int hitsBefore = arena.heroHits();
                int turnsBefore = arena.turnsTaken();
                int heroBefore = arena.heroCell();

                arena.step(random.nextBoolean() ? Direction.LEFT : Direction.RIGHT);
                if (arena.turnsTaken() == turnsBefore) {
                    continue;
                }
                // Un tour qui deplace le heros le sort peut-etre de la case annoncee : on ne
                // compare que lorsqu'il n'a pas bouge, comme le fait le test du telegraphe.
                if (arena.heroCell() != heroBefore) {
                    continue;
                }
                int taken = arena.heroHits() - hitsBefore;
                if (taken > announced) {
                    failures.add("graine " + seed + " tour " + turn + " : " + announced
                            + " coup(s) annonce(s), " + taken + " encaisse(s)");
                }
            }
        }

        assertTrue(failures.isEmpty(), "le telegraphe ment :\n  " + String.join("\n  ", failures));
    }

    /**
     * Une invocation annoncée réserve sa case : aucun allié ne doit pouvoir s'y poser. Sans cela
     * l'annonce échouerait sans que le joueur y soit pour rien — la même faute que l'allié qui
     * interceptait une charge, transposée à l'invocation.
     */
    @Test
    @DisplayName("Aucun allié ne se pose sur la case d'une invocation annoncée")
    void noAllyStepsOntoAnAnnouncedSummonCell() {
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
                            assertTrue(other.intention().targetCell() != cell,
                                    "graine " + seed + " tour " + turn + " : " + other.label()
                                            + " vise la case reservee " + (cell + 1));
                        }
                    }
                }
            }
        }
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
}
