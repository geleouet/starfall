package com.starfall.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Le seul test qui vérifie ce que le jalon promet vraiment : <b>ce qui est affiché est ce qui sera
 * joué</b>.
 *
 * <p>Il n'existait pas. Les autres tests vérifiaient les archétypes un par un et les invariants de
 * plateau, mais aucun ne confrontait l'annonce au résultat — si bien que le colosse a pu, une phase
 * sur deux, cercler de rouge une case qu'il ne frappait pas, sans qu'aucun test ne bronche. Un
 * archétype sur quatre mentait au joueur.
 *
 * <p>La méthode est simple et devrait le rester : avant chaque tour, on relève ce que l'interface
 * montrerait — le nombre de coups annoncés sur la case du héros — puis on joue le tour et on
 * compare aux coups réellement encaissés.
 */
class TelegraphTest {

    /** Obstacle inerte : un ennemi bougerait pendant la phase et fausserait la mesure. */
    private record Rock(String label) implements Occupant {
        @Override
        public String spriteName() {
            return "enemy/colosse";
        }
    }

    private static final int SEEDS = 300;
    private static final int TURNS_PER_SEED = 60;

    @Test
    @DisplayName("Les coups annoncés sur la case du héros sont exactement ceux qu'il encaisse")
    void announcedBlowsAreExactlyTheBlowsTaken() {
        List<String> failures = new ArrayList<>();

        for (int seed = 0; seed < SEEDS; seed++) {
            Random random = new Random(seed);
            Arena arena = ArenaSetup.trainingArena(
                    Grid.MIN_WIDTH + random.nextInt(Grid.MAX_WIDTH - Grid.MIN_WIDTH + 1));

            for (int turn = 0; turn < TURNS_PER_SEED && !arena.enemies().isEmpty(); turn++) {
                int heroCell = arena.heroCell();
                int announced = arena.threatCount(heroCell);
                int hitsBefore = arena.heroHits();
                int turnsBefore = arena.turnsTaken();

                // Un demi-tour : la seule action qui consomme un tour sans déplacer le héros ni
                // toucher à la grille. Le héros reste donc exactement sur la case menacée.
                arena.step(arena.hero().facing().opposite());
                if (arena.turnsTaken() == turnsBefore) {
                    continue; // aucun tour consommé : aucune phase ennemie à vérifier
                }

                int taken = arena.heroHits() - hitsBefore;
                if (taken != announced) {
                    failures.add("graine " + seed + " tour " + turn + " : " + announced
                            + " coup(s) annonce(s) sur la case " + (heroCell + 1)
                            + ", " + taken + " encaisse(s)");
                }
                if (failures.size() > 5) {
                    break;
                }
            }
        }

        assertTrue(failures.isEmpty(), "le telegraphe ment :\n  " + String.join("\n  ", failures));
    }

    @Test
    @DisplayName("Aucun coup n'est encaissé sans avoir été annoncé, même en bougeant")
    void noBlowLandsWithoutHavingBeenAnnounced() {
        for (int seed = 0; seed < SEEDS; seed++) {
            Random random = new Random(seed);
            Arena arena = ArenaSetup.trainingArena(
                    Grid.MIN_WIDTH + random.nextInt(Grid.MAX_WIDTH - Grid.MIN_WIDTH + 1));

            for (int turn = 0; turn < TURNS_PER_SEED; turn++) {
                // On relève toutes les cases menacées AVANT d'agir, puis on agit librement.
                boolean[] threatened = new boolean[arena.grid().width()];
                for (int cell = 0; cell < threatened.length; cell++) {
                    threatened[cell] = arena.isThreatened(cell);
                }
                int hitsBefore = arena.heroHits();
                int turnsBefore = arena.turnsTaken();

                arena.step(random.nextBoolean() ? Direction.LEFT : Direction.RIGHT);
                if (arena.turnsTaken() == turnsBefore) {
                    continue;
                }

                if (arena.heroHits() > hitsBefore) {
                    // Le héros a été touché : il devait se trouver sur une case annoncée, soit
                    // celle qu'il occupait avant de bouger, soit celle où il est arrivé.
                    assertTrue(threatened[arena.heroCell()],
                            "graine " + seed + " tour " + turn
                                    + " : coup encaisse sur la case " + (arena.heroCell() + 1)
                                    + ", qui n'etait pas annoncee");
                }
            }
        }
    }

    /**
     * L'archétype par lequel le défaut est passé. Un test dédié en plus du fuzz : quand une règle a
     * déjà été violée une fois, elle mérite d'être épinglée par son nom.
     */
    @Test
    @DisplayName("Le colosse n'annonce rien quand il n'agira pas")
    void theColosseAnnouncesNothingWhenItWillNotAct() {
        Arena arena = new Arena(9, 4);
        arena.grid().place(5, new Enemy(EnemyKind.COLOSSE));
        arena.announceIntentions();

        for (int turn = 0; turn < 8; turn++) {
            int announced = arena.threatCount(arena.heroCell());
            int before = arena.heroHits();

            arena.step(arena.hero().facing().opposite());

            assertEquals(announced, arena.heroHits() - before,
                    "tour " + turn + " : " + announced + " annonce(s), "
                            + (arena.heroHits() - before) + " encaisse(s)");
        }
        assertTrue(arena.heroHits() > 0, "le colosse doit tout de meme frapper de temps en temps");
    }

    /**
     * L'invariant par lequel M7 avait rouvert le défaut de M6 : un allié qui entre dans le couloir
     * d'une charge déjà annoncée l'intercepte, si bien que le coup promis au joueur ne tombe jamais.
     * Le télégraphe sur-promet alors — on peut dépenser un tour pour esquiver un coup qui n'allait
     * pas partir.
     *
     * <p>Le test porte sur la propriété plutôt que sur un cas construit à la main : une charge
     * annoncée <b>réserve</b> son couloir, et aucune autre intention ne doit désigner une case qui
     * s'y trouve.
     */
    @Test
    @DisplayName("Aucune intention ne vise une case réservée par une charge annoncée")
    void noIntentionEverTargetsAnAnnouncedChargeCorridor() {
        for (int seed = 0; seed < SEEDS; seed++) {
            Random random = new Random(seed);
            Arena arena = ArenaSetup.trainingArena(
                    Grid.MIN_WIDTH + random.nextInt(Grid.MAX_WIDTH - Grid.MIN_WIDTH + 1));

            for (int turn = 0; turn < TURNS_PER_SEED && !arena.isOver(); turn++) {
                arena.step(random.nextBoolean() ? Direction.LEFT : Direction.RIGHT);

                for (Enemy charger : arena.enemies()) {
                    if (charger.intention().kind() != Intention.Kind.CHARGE) {
                        continue;
                    }
                    int from = arena.grid().indexOf(charger);
                    int to = charger.intention().targetCell();
                    int step = Integer.signum(to - from);

                    for (Enemy other : arena.enemies()) {
                        if (other == charger || other.intention().kind() != Intention.Kind.ADVANCE) {
                            continue;
                        }
                        int destination = other.intention().targetCell();
                        for (int cell = from; cell != to; cell += step) {
                            assertTrue(destination != cell,
                                    "graine " + seed + " tour " + turn + " : " + other.label()
                                            + " veut se poser en " + (destination + 1)
                                            + ", dans le couloir de charge " + (from + 1)
                                            + " vers " + (to + 1));
                        }
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("Une charge interceptée s'arrête et ne frappe pas")
    void anInterceptedChargeStopsAndDoesNotStrike() {
        Arena arena = new Arena(15, 0);
        Enemy lancier = new Enemy(EnemyKind.LANCIER);
        arena.grid().place(14, lancier);
        arena.announceIntentions();

        arena.step(Direction.LEFT);  // demi-tour : le lancier prend son élan
        assertEquals(Intention.Kind.CHARGE, lancier.intention().kind());

        // On s'interpose : un occupant entre le lancier et sa cible.
        arena.grid().place(7, new Rock("obstacle"));
        int hitsBefore = arena.heroHits();

        arena.step(Direction.RIGHT); // demi-tour : la charge part

        assertEquals(hitsBefore, arena.heroHits(), "une charge interceptee ne frappe pas");
        assertEquals(8, arena.grid().indexOf(lancier), "elle s'arrete juste avant l'obstacle");
    }

    @Test
    @DisplayName("Une charge ne traverse jamais personne")
    void aChargeNeverWalksThroughAnyone() {
        for (int blocker = 2; blocker <= 12; blocker++) {
            Arena arena = new Arena(15, 0);
            Enemy lancier = new Enemy(EnemyKind.LANCIER);
            arena.grid().place(14, lancier);
            arena.grid().place(blocker, new Rock("obstacle"));
            arena.announceIntentions();

            arena.step(Direction.LEFT);
            arena.step(Direction.RIGHT);

            int landed = arena.grid().indexOf(lancier);
            assertTrue(landed > blocker,
                    "le lancier a traverse l'obstacle de la case " + (blocker + 1)
                            + " : il est en " + (landed + 1));
        }
    }

    @Test
    @DisplayName("Un lancier rapide porte bien deux coups en chargeant")
    void aFastLancierStrikesTwiceWhenCharging() {
        Arena arena = new Arena(9, 0);
        arena.grid().place(5, new Enemy(EnemyKind.LANCIER, Trait.RAPIDE));
        arena.announceIntentions();

        arena.step(Direction.LEFT);  // élan
        int announced = arena.threatCount(arena.heroCell());
        int before = arena.heroHits();
        arena.step(Direction.RIGHT); // charge

        assertEquals(2, announced, "deux coups doivent etre annonces");
        assertEquals(2, arena.heroHits() - before, "et deux coups doivent tomber");
    }

    @Test
    @DisplayName("Deux ennemis qui visent la même case annoncent deux coups")
    void twoEnemiesAimingAtTheSameCellAnnounceTwoBlows() {
        Arena arena = new Arena(9, 4);
        arena.grid().place(3, new Enemy(EnemyKind.SABREUR));
        arena.grid().place(5, new Enemy(EnemyKind.SABREUR));
        arena.announceIntentions();

        assertEquals(2, arena.threatCount(4));

        int before = arena.heroHits();
        arena.step(arena.hero().facing().opposite());
        assertEquals(2, arena.heroHits() - before);
    }

    @Test
    @DisplayName("Les glyphes d'intention tiennent sous le bandeau d'interface")
    void theIntentionGlyphsFitBelowTheOverlay() {
        // Le bandeau se borne au contenu déclaré par la scène ; si ce contenu oublie les glyphes,
        // il les recouvre — ce qui était le cas, à certaines tailles de fenêtre seulement.
        assertTrue(ArenaLayout.INTENT_Y > ArenaLayout.FIGURE_Y + ArenaLayout.FIGURE_HEIGHT,
                "les glyphes doivent etre au-dessus des tetes");
        assertTrue(ArenaLayout.INTENT_TOP < com.starfall.StarfallGame.MIN_WORLD_HEIGHT,
                "les glyphes doivent tenir dans la zone garantie");
        assertEquals(ArenaLayout.INTENT_Y + ArenaLayout.INTENT_HEIGHT, ArenaLayout.INTENT_TOP);

        // Les points de vie ont leur propre bande : ils etaient d'abord poses sur la ligne des
        // barres de menace, deux informations vitales au meme pixel.
        assertTrue(ArenaLayout.HEALTH_Y + ArenaLayout.HEALTH_HEIGHT <= ArenaLayout.INTENT_Y,
                "la bande de vie mord sur celle des intentions");
        assertTrue(ArenaLayout.HEALTH_Y > ArenaLayout.GROUND_Y + ArenaLayout.GROUND_HEIGHT,
                "la bande de vie mord sur les dalles et donc sur les barres de menace");
    }

    @Test
    @DisplayName("La partie reste reproductible")
    void theGameStaysReproducible() {
        for (int width = Grid.MIN_WIDTH; width <= Grid.MAX_WIDTH; width++) {
            String reference = play(width);
            for (int repeat = 0; repeat < 3; repeat++) {
                assertEquals(reference, play(width), "grille de " + width);
            }
        }
    }

    private static String play(int width) {
        Arena arena = ArenaSetup.trainingArena(width);
        Random random = new Random(7);
        for (int turn = 0; turn < 40; turn++) {
            arena.step(random.nextBoolean() ? Direction.LEFT : Direction.RIGHT);
        }
        StringBuilder state = new StringBuilder();
        for (int cell = 0; cell < arena.grid().width(); cell++) {
            Occupant occupant = arena.grid().occupantAt(cell);
            state.append(occupant == null ? '.'
                    : occupant == arena.hero() ? 'H' : occupant.label().charAt(0));
        }
        return state.append('|').append(arena.heroHits())
                .append('|').append(arena.turnsTaken()).toString();
    }
}
