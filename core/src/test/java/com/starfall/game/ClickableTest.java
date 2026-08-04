package com.starfall.game;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Ce que le pointeur promet, le clic doit le tenir.
 *
 * <p>L'objection date de M4 et a traîné quatre jalons : le surlignage de survol s'allumait sur
 * <b>toutes</b> les cases du plateau, y compris celles qu'un clic ne pouvait pas atteindre — derrière
 * un occupant, ou n'importe où une fois la partie finie. Le pointeur promettait une action qui ne
 * venait pas, ce qui est la même faute que le télégraphe des ennemis a coûté deux jalons à éteindre,
 * simplement du côté du joueur.
 *
 * <p>Personne n'avait de test parce que {@code cellAt} et {@code clickOn} étaient testés chacun de
 * son côté, sans jamais être confrontés l'un à l'autre. Le voici.
 */
class ClickableTest {

    private static final int SEEDS = 300;
    private static final int ACTIONS_PER_SEED = 25;

    /** Rejoue une partie depuis sa graine jusqu'à une action donnée, pour retrouver l'état exact. */
    private static Arena replay(int seed, int width, int actions) {
        Random random = new Random(seed);
        Arena arena = ArenaSetup.trainingArena(width);
        for (int i = 0; i < actions; i++) {
            arena.step(random.nextBoolean() ? Direction.LEFT : Direction.RIGHT);
        }
        return arena;
    }

    /**
     * L'invariant : {@code clickable} est vrai si et seulement si un clic produit autre chose que
     * {@code BLOCKED}.
     *
     * <p>Chaque case est essayée sur une partie <b>rejouée</b> depuis sa graine : un clic modifie
     * l'arène, donc les mesurer à la suite sur la même instance ne dirait rien.
     */
    @Test
    @DisplayName("Une case s'allume si et seulement si un clic y fait quelque chose")
    void aCellLightsUpExactlyWhenAClickWouldDoSomething() {
        List<String> failures = new ArrayList<>();

        for (int seed = 0; seed < SEEDS && failures.size() <= 5; seed++) {
            int width = Grid.MIN_WIDTH + new Random(seed).nextInt(Grid.MAX_WIDTH - Grid.MIN_WIDTH + 1);

            for (int actions = 0; actions < ACTIONS_PER_SEED; actions++) {
                Arena reference = replay(seed, width, actions);
                for (int cell = 0; cell < width; cell++) {
                    boolean promised = reference.clickable(cell);
                    ActionResult result = replay(seed, width, actions).clickOn(cell);
                    boolean delivered = result != ActionResult.BLOCKED;

                    if (promised != delivered) {
                        failures.add("graine " + seed + " apres " + actions + " actions, case "
                                + (cell + 1) + " : promesse " + promised + ", resultat " + result);
                    }
                }
            }
        }

        assertTrue(failures.isEmpty(), "le pointeur ment :\n  " + String.join("\n  ", failures));
    }

    /** Le cas déterministe que la review a exhibé : cliquer derrière un occupant ne mène nulle part. */
    @Test
    @DisplayName("Une case située derrière un occupant ne s'allume pas")
    void aCellBehindAnOccupantStaysDark() {
        Arena arena = new Arena(9, 6);
        arena.hero().face(Direction.LEFT);
        arena.grid().place(5, new Enemy(EnemyKind.SABREUR));

        assertTrue(arena.clickable(5), "la case de l'occupant reste cliquable : c'est l'echange");
        for (int cell = 0; cell <= 4; cell++) {
            assertTrue(!arena.clickable(cell),
                    "la case " + (cell + 1) + " est derriere l'occupant et s'allume quand meme");
        }
    }

    @Test
    @DisplayName("Aucune case ne s'allume une fois la partie finie")
    void noCellLightsUpOnceTheGameIsOver() {
        Random random = new Random(3);
        Arena arena = ArenaSetup.trainingArena(9);
        while (!arena.isOver() && arena.turnsTaken() < 400) {
            arena.step(random.nextBoolean() ? Direction.LEFT : Direction.RIGHT);
        }
        assertTrue(arena.isOver(), "la partie doit s'etre terminee");

        for (int cell = 0; cell < arena.grid().width(); cell++) {
            assertTrue(!arena.clickable(cell),
                    "la case " + (cell + 1) + " s'allume encore apres la fin de la partie");
        }
    }
}
