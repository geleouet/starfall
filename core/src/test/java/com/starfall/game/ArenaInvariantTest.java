package com.starfall.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Invariants du combat, vérifiés après <b>chaque</b> action d'une longue séquence aléatoire.
 *
 * <p>Les autres tests vérifient chacun une règle sur un cas choisi. Celui-ci vérifie que les règles
 * tiennent <em>ensemble</em>, sur des enchaînements que personne n'a pensé à écrire — c'est le seul
 * moyen d'attraper une interaction entre deux mécaniques correctes prises séparément.
 *
 * <p>Chaque invariant est ici parce qu'il est <b>annoncé quelque part</b> : dans une javadoc, dans
 * le tableau de bord ou dans un message de commit. Aucun n'était verrouillé avant.
 */
class ArenaInvariantTest {

    private static final int SEEDS = 400;
    private static final int ACTIONS_PER_SEED = 200;

    @Test
    @DisplayName("Les invariants tiennent sur 80 000 actions aléatoires")
    void invariantsHoldUnderRandomPlay() {
        for (int seed = 0; seed < SEEDS; seed++) {
            Random random = new Random(seed);
            int gridWidth = Grid.MIN_WIDTH + random.nextInt(Grid.MAX_WIDTH - Grid.MIN_WIDTH + 1);
            Arena arena = ArenaSetup.trainingArena(gridWidth);

            checkAll(arena, seed, -1, "depart");
            for (int step = 0; step < ACTIONS_PER_SEED; step++) {
                int turnsBefore = arena.turnsTaken();
                applyRandomAction(arena, random);
                checkAll(arena, seed, step, "apres action");
                checkTimeMovesForward(arena, turnsBefore, seed, step);
            }
        }
    }

    private static void applyRandomAction(Arena arena, Random random) {
        List<Tile> tiles = arena.rack().tiles();
        switch (random.nextInt(6)) {
            case 0 -> arena.step(random.nextBoolean() ? Direction.LEFT : Direction.RIGHT);
            case 1 -> arena.swapWithTarget();
            case 2 -> arena.clickOn(random.nextInt(arena.grid().width() + 4) - 2);
            case 3 -> arena.queueTile(tiles.get(random.nextInt(tiles.size())));
            case 4 -> arena.unqueueAt(random.nextInt(ActionQueue.CAPACITY + 1) - 1);
            default -> arena.executeTop();
        }
    }

    /** Le temps ne recule jamais, et n'avance jamais de plus d'un tour par action. */
    private static void checkTimeMovesForward(Arena arena, int before, int seed, int step) {
        int delta = arena.turnsTaken() - before;
        if (delta < 0 || delta > 1) {
            fail("graine " + seed + " action " + step + " : le compteur de tours a bouge de " + delta);
        }
    }

    private static void checkAll(Arena arena, int seed, int step, String when) {
        String where = "graine " + seed + " action " + step + " (" + when + ") : ";

        // Le héros est sur la grille, et une seule fois.
        int heroCell = arena.heroCell();
        assertTrue(arena.grid().contains(heroCell), where + "heros hors grille : " + heroCell);
        int heroCount = 0;
        for (int cell = 0; cell < arena.grid().width(); cell++) {
            if (arena.grid().occupantAt(cell) == arena.hero()) {
                heroCount++;
            }
        }
        assertEquals(1, heroCount, where + "le heros apparait " + heroCount + " fois");

        // Aucun occupant n'apparaît deux fois sur la grille.
        Set<Occupant> seen = new HashSet<>();
        for (int cell : arena.grid().occupiedCells()) {
            assertTrue(seen.add(arena.grid().occupantAt(cell)),
                    where + "un occupant est sur deux cases a la fois");
        }

        // La file ne déborde pas, et ne porte jamais deux fois la même tuile.
        List<Tile> queued = arena.queue().fromOldest();
        assertTrue(queued.size() <= ActionQueue.CAPACITY,
                where + "file de " + queued.size() + " tuiles");
        assertEquals(queued.size(), new HashSet<>(queued).size(),
                where + "une tuile est deux fois sur la file : " + queued);

        // L'invariant central : chaque tuile est au râtelier OU sur la file, jamais les deux,
        // jamais nulle part.
        for (Tile tile : arena.rack().tiles()) {
            boolean inRack = arena.rack().holds(tile);
            boolean inQueue = queued.contains(tile);
            assertTrue(inRack ^ inQueue,
                    where + tile.label() + " : rateleir=" + inRack + " file=" + inQueue);
        }

        // Les points de recharge restent dans leurs bornes, et une tuile posée n'en accumule pas.
        for (Tile tile : arena.rack().tiles()) {
            int missing = arena.rack().missingPoints(tile);
            assertTrue(missing >= 0 && missing <= tile.rechargeCost(),
                    where + tile.label() + " a " + missing + " points restants");
            if (missing > 0) {
                assertTrue(arena.rack().holds(tile),
                        where + tile.label() + " se recharge alors qu'elle est sur la file");
            }
        }
    }

    /**
     * L'autre sens de la règle : un tour ne se consomme que si quelque chose a réellement changé.
     * Sans cela, une action bloquée ferait avancer le temps — la punition qu'on ne comprend pas.
     */
    @Test
    @DisplayName("Aucun tour n'est consommé sans que l'état du jeu change")
    void noTurnPassesWithoutSomethingHappening() {
        for (int seed = 0; seed < SEEDS; seed++) {
            Random random = new Random(seed);
            Arena arena = ArenaSetup.trainingArena(9);

            for (int step = 0; step < ACTIONS_PER_SEED; step++) {
                String before = snapshot(arena);
                int turnsBefore = arena.turnsTaken();
                applyRandomAction(arena, random);

                if (arena.turnsTaken() > turnsBefore) {
                    assertTrue(!snapshot(arena).equals(before),
                            "graine " + seed + " action " + step
                                    + " : un tour a ete consomme sans que rien ne change - " + before);
                }
            }
        }
    }

    /** État visible du jeu, hors compteur de tours et hors recharges. */
    private static String snapshot(Arena arena) {
        StringBuilder builder = new StringBuilder();
        for (int cell = 0; cell < arena.grid().width(); cell++) {
            Occupant occupant = arena.grid().occupantAt(cell);
            builder.append(occupant == null ? "." : occupant == arena.hero() ? "H" : "x");
        }
        return builder.append('|').append(arena.hero().facing())
                .append('|').append(arena.queue().fromOldest()).toString();
    }
}
