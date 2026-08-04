package com.starfall.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests de la file d'actions.
 *
 * <p>L'essentiel tient en une phrase : <b>on exécute à l'envers de l'ordre où l'on a posé</b>. Tout
 * le reste du jeu s'appuie là-dessus, et une inversion silencieuse ne se verrait qu'à l'usage.
 */
class ActionQueueTest {

    /** Occupant inerte : ces tests portent sur la file, pas sur le comportement des ennemis. */
    private record Pawn(String label) implements Occupant {
        @Override
        public String spriteName() {
            return "enemy/sabreur";
        }
    }

    private static Arena arena() {
        return new Arena(9);
    }

    @Nested
    @DisplayName("Ordre dernier entré, premier sorti")
    class LastInFirstOut {

        @Test
        @DisplayName("La dernière posée est la première exécutée")
        void theLastQueuedIsTheFirstExecuted() {
            Arena arena = arena();
            arena.queueTile(Tile.STRIKE);
            arena.queueTile(Tile.PUSH);
            arena.queueTile(Tile.DASH);

            assertEquals(Tile.DASH, arena.queue().top());
        }

        @Test
        @DisplayName("L'affichage lit la file dans l'ordre de pose, donc à l'envers de l'exécution")
        void theDisplayOrderIsTheReverseOfTheExecutionOrder() {
            Arena arena = arena();
            arena.queueTile(Tile.STRIKE);
            arena.queueTile(Tile.PUSH);

            assertEquals(List.of(Tile.STRIKE, Tile.PUSH), arena.queue().fromOldest());
            assertEquals(Tile.PUSH, arena.queue().top());
        }

        @Test
        @DisplayName("Vider la file la vide dans l'ordre inverse de la pose")
        void emptyingTheQueueUnwindsIt() {
            Arena arena = new Arena(15, 0);
            arena.queueTile(Tile.STRIKE);
            arena.queueTile(Tile.PUSH);
            arena.queueTile(Tile.DASH);

            arena.executeTop();
            assertEquals(Tile.PUSH, arena.queue().top());
            arena.executeTop();
            assertEquals(Tile.STRIKE, arena.queue().top());
            arena.executeTop();
            assertTrue(arena.queue().isEmpty());
        }
    }

    @Nested
    @DisplayName("Capacité")
    class Capacity {

        @Test
        @DisplayName("La file en porte cinq, pas trois")
        void theQueueHoldsFive() {
            assertEquals(5, ActionQueue.CAPACITY);
        }

        @Test
        @DisplayName("Le héros possède plus de tuiles que la file n'a d'emplacements")
        void theHeroOwnsMoreTilesThanTheQueueHolds() {
            // Sinon la file ne pourrait jamais etre pleine : une tuile est soit au rateleir, soit
            // sur la file, jamais les deux.
            assertTrue(Tile.values().length > ActionQueue.CAPACITY,
                    Tile.values().length + " tuiles pour " + ActionQueue.CAPACITY + " emplacements");
        }

        @Test
        @DisplayName("Une sixième pose est refusée sans rien casser")
        void aSixthTileIsRefused() {
            Arena arena = new Arena(15, 7);
            Tile[] tiles = Tile.values();
            for (int i = 0; i < ActionQueue.CAPACITY; i++) {
                assertEquals(ActionResult.QUEUED, arena.queueTile(tiles[i]));
            }

            assertTrue(arena.queue().isFull());
            assertEquals(ActionResult.QUEUE_FULL, arena.queueTile(tiles[ActionQueue.CAPACITY]));

            assertEquals(ActionQueue.CAPACITY, arena.queue().size(), "la file n'a pas deborde");
            assertTrue(arena.rack().holds(tiles[ActionQueue.CAPACITY]),
                    "la tuile refusee doit rester au rateleir");
            assertEquals(0, arena.turnsTaken(), "un refus ne consomme rien");
        }
    }

    @Nested
    @DisplayName("Poser et reprendre sont gratuits")
    class FreeHandling {

        @Test
        @DisplayName("Poser une tuile ne consomme pas de tour")
        void queueingCostsNoTurn() {
            Arena arena = arena();
            int before = arena.turnsTaken();

            assertEquals(ActionResult.QUEUED, arena.queueTile(Tile.STRIKE));
            assertEquals(ActionResult.QUEUED, arena.queueTile(Tile.PUSH));

            assertEquals(before, arena.turnsTaken(), "preparer la file ne doit jamais punir");
        }

        @Test
        @DisplayName("Reprendre une tuile ne consomme pas de tour et ne la met pas en recharge")
        void unqueueingCostsNothingAtAll() {
            Arena arena = arena();
            arena.queueTile(Tile.STRIKE);
            int before = arena.turnsTaken();

            assertEquals(ActionResult.UNQUEUED, arena.unqueueAt(0));

            assertEquals(before, arena.turnsTaken());
            assertTrue(arena.rack().isReady(Tile.STRIKE), "la tuile n'a pas servi : elle reste prete");
            assertTrue(arena.queue().isEmpty());
        }

        @Test
        @DisplayName("On peut reprendre une tuile du milieu de la file")
        void aTileCanBeTakenBackFromTheMiddle() {
            Arena arena = arena();
            arena.queueTile(Tile.STRIKE);
            arena.queueTile(Tile.PUSH);
            arena.queueTile(Tile.DASH);

            assertEquals(ActionResult.UNQUEUED, arena.unqueueAt(1));

            assertEquals(List.of(Tile.STRIKE, Tile.DASH), arena.queue().fromOldest());
            assertEquals(Tile.DASH, arena.queue().top(), "le sommet ne doit pas avoir bouge");
        }

        @Test
        @DisplayName("Reprendre une position qui n'existe pas ne fait rien")
        void takingBackNothingDoesNothing() {
            Arena arena = arena();
            arena.queueTile(Tile.STRIKE);

            assertEquals(ActionResult.BLOCKED, arena.unqueueAt(3));
            assertEquals(ActionResult.BLOCKED, arena.unqueueAt(-1));
            assertEquals(1, arena.queue().size());
        }

        @Test
        @DisplayName("Une tuile posée quitte le râtelier et ne peut pas être posée deux fois")
        void aQueuedTileLeavesTheRack() {
            Arena arena = arena();
            arena.queueTile(Tile.STRIKE);

            assertFalse(arena.rack().holds(Tile.STRIKE));
            assertEquals(ActionResult.NOT_READY, arena.queueTile(Tile.STRIKE));
            assertEquals(1, arena.queue().size());
        }
    }

    @Nested
    @DisplayName("Recharges")
    class Recharge {

        @Test
        @DisplayName("Une tuile exécutée part en recharge pour son coût")
        void anExecutedTileGoesOnCooldown() {
            Arena arena = new Arena(15, 0);
            arena.queueTile(Tile.DASH);
            arena.executeTop();

            // L'execution consomme un tour, qui accorde deja un point de recharge.
            assertEquals(Tile.DASH.rechargeCost() - 1, arena.rack().missingPoints(Tile.DASH));
            assertFalse(arena.rack().isReady(Tile.DASH));
        }

        @Test
        @DisplayName("Chaque tour consommé donne un point à toutes les tuiles en recharge")
        void everyTurnGivesOnePointToEveryRechargingTile() {
            Arena arena = new Arena(15, 0);
            arena.queueTile(Tile.STRIKE);
            arena.queueTile(Tile.DASH);
            arena.executeTop();  // DASH : le heros charge, un tour consomme
            arena.executeTop();  // STRIKE : personne devant, aucun tour consomme

            int dashBefore = arena.rack().missingPoints(Tile.DASH);
            int strikeBefore = arena.rack().missingPoints(Tile.STRIKE);
            arena.step(arena.hero().facing().opposite()); // demi-tour : un tour

            assertEquals(dashBefore - 1, arena.rack().missingPoints(Tile.DASH));
            assertEquals(strikeBefore - 1, arena.rack().missingPoints(Tile.STRIKE));
        }

        @Test
        @DisplayName("Une tuile en recharge ne peut pas être posée")
        void aRechargingTileCannotBeQueued() {
            Arena arena = new Arena(15, 0);
            arena.queueTile(Tile.DASH);
            arena.executeTop();

            assertEquals(ActionResult.NOT_READY, arena.queueTile(Tile.DASH));
        }

        @Test
        @DisplayName("Après assez de tours, la tuile revient")
        void afterEnoughTurnsTheTileComesBack() {
            Arena arena = new Arena(15, 0);
            arena.queueTile(Tile.DASH);
            arena.executeTop();

            int guard = 0;
            while (!arena.rack().isReady(Tile.DASH) && guard++ < 20) {
                arena.step(arena.hero().facing().opposite());
            }

            assertTrue(arena.rack().isReady(Tile.DASH), "la tuile n'est jamais revenue");
            assertEquals(ActionResult.QUEUED, arena.queueTile(Tile.DASH));
        }

        @Test
        @DisplayName("Poser et reprendre en boucle ne fait avancer aucune recharge")
        void shufflingTheQueueNeverAdvancesTime() {
            Arena arena = arena();
            arena.queueTile(Tile.STRIKE);
            arena.executeTop();
            int missing = arena.rack().missingPoints(Tile.STRIKE);

            for (int i = 0; i < 50; i++) {
                arena.queueTile(Tile.PUSH);
                arena.unqueueAt(0);
            }

            assertEquals(missing, arena.rack().missingPoints(Tile.STRIKE),
                    "remuer la file ne doit pas faire passer le temps");
        }
    }

    @Nested
    @DisplayName("Free-Play")
    class FreePlay {

        @Test
        @DisplayName("Une tuile Free-Play ne consomme pas de tour")
        void aFreePlayTileCostsNoTurn() {
            Arena arena = arena();
            arena.queueTile(Tile.SIDESTEP);
            int before = arena.turnsTaken();

            assertEquals(ActionResult.MOVED, arena.executeTop());

            assertEquals(before, arena.turnsTaken());
            assertTrue(Tile.SIDESTEP.isFreePlay());
        }

        @Test
        @DisplayName("Mais elle part quand même en recharge")
        void butItStillGoesOnCooldown() {
            Arena arena = arena();
            arena.queueTile(Tile.SIDESTEP);
            arena.executeTop();

            assertFalse(arena.rack().isReady(Tile.SIDESTEP));
            assertEquals(Tile.SIDESTEP.rechargeCost(), arena.rack().missingPoints(Tile.SIDESTEP),
                    "aucun tour consomme, donc aucun point accorde");
        }

        @Test
        @DisplayName("La volte-face offre un demi-tour gratuit, que le clavier fait payer")
        void thePivotGivesAFreeTurnAround() {
            // C'est le sel de la regle d'orientation du projet : se retourner coute normalement un
            // tour, donc une tuile qui l'offre a une vraie valeur tactique.
            Arena byKey = new Arena(9);
            Arena byTile = new Arena(9);

            byKey.step(byKey.hero().facing().opposite());
            byTile.queueTile(Tile.PIVOT);
            byTile.executeTop();

            assertEquals(byKey.hero().facing(), byTile.hero().facing(), "meme resultat");
            assertEquals(1, byKey.turnsTaken(), "au clavier, le demi-tour coute un tour");
            assertEquals(0, byTile.turnsTaken(), "par la tuile, il est gratuit");
        }

        @Test
        @DisplayName("Le pas de côté recule sans faire pivoter le héros")
        void theSidestepMovesBackWithoutTurning() {
            Arena arena = arena();
            int start = arena.heroCell();
            Direction facing = arena.hero().facing();

            arena.queueTile(Tile.SIDESTEP);
            arena.executeTop();

            assertEquals(start - facing.step(), arena.heroCell());
            assertEquals(facing, arena.hero().facing(), "reculer ne fait pas se retourner");
        }
    }

    @Nested
    @DisplayName("Exécution")
    class Execution {

        @Test
        @DisplayName("Exécuter une file vide le dit au lieu de ne rien faire")
        void executingAnEmptyQueueSaysSo() {
            Arena arena = arena();

            assertEquals(ActionResult.EMPTY_QUEUE, arena.executeTop());
            assertEquals(0, arena.turnsTaken());
        }

        @Test
        @DisplayName("Une tuile dont l'effet échoue est consommée mais ne coûte pas de tour")
        void aTileThatMissesIsSpentButFree() {
            Arena arena = arena();
            arena.queueTile(Tile.STRIKE); // personne devant
            int before = arena.turnsTaken();

            assertEquals(ActionResult.NO_TARGET, arena.executeTop());

            assertTrue(arena.queue().isEmpty(), "la tuile est bien consommee");
            assertFalse(arena.rack().isReady(Tile.STRIKE), "et bien mise en recharge");
            assertEquals(before, arena.turnsTaken(), "mais elle ne fait pas payer deux fois");
        }

        @Test
        @DisplayName("La frappe retire l'occupant juste devant")
        void theStrikeRemovesTheOccupantAhead() {
            Arena arena = arena();
            int target = arena.heroCell() + 1;
            arena.grid().place(target, new Pawn("cible"));

            arena.queueTile(Tile.STRIKE);
            assertEquals(ActionResult.STRUCK, arena.executeTop());

            assertTrue(arena.grid().isFree(target));
        }

        @Test
        @DisplayName("La poussée déplace la cible d'une case, sans la retirer")
        void thePushMovesTheTargetOneCell() {
            Arena arena = arena();
            int target = arena.heroCell() + 1;
            Occupant victim = new Pawn("cible");
            arena.grid().place(target, victim);

            arena.queueTile(Tile.PUSH);
            assertEquals(ActionResult.PUSHED, arena.executeTop());

            assertTrue(arena.grid().isFree(target));
            assertSame(victim, arena.grid().occupantAt(target + 1));
        }

        @Test
        @DisplayName("Une poussée contre un mur ou un autre occupant est bloquée")
        void aPushIntoSomethingIsBlocked() {
            Arena arena = new Arena(9, 7);
            arena.grid().place(8, new Pawn("au bord"));

            arena.queueTile(Tile.PUSH);
            assertEquals(ActionResult.BLOCKED, arena.executeTop());
            assertNull(arena.grid().occupantAt(9));
        }

        @Test
        @DisplayName("L'élan charge jusqu'au premier obstacle")
        void theDashRunsUntilBlocked() {
            Arena arena = new Arena(15, 0);
            arena.grid().place(6, new Pawn("obstacle"));

            arena.queueTile(Tile.DASH);
            assertEquals(ActionResult.DASHED, arena.executeTop());

            assertEquals(5, arena.heroCell(), "arrete juste avant l'obstacle");
        }

        @Test
        @DisplayName("Un élan sans place disponible est bloqué")
        void aDashWithNowhereToGoIsBlocked() {
            Arena arena = new Arena(9, 8);

            arena.queueTile(Tile.DASH);
            assertEquals(ActionResult.BLOCKED, arena.executeTop());
            assertEquals(8, arena.heroCell());
        }
    }
}
