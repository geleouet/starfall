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
 * <p>L'essentiel tient en deux phrases. <b>On exécute à l'envers de l'ordre où l'on a posé</b> —
 * tout le reste du jeu s'appuie là-dessus, et une inversion silencieuse ne se verrait qu'à l'usage.
 * Et <b>charger coûte un tour par tuile, lâcher la salve en coûte un seul</b> : c'est l'économie qui
 * donne son sens à la file.
 *
 * <p>Cette seconde phrase est arrivée au jalon d'équilibrage, et elle a renversé la moitié de ces
 * tests. Jusque-là, poser était gratuit et chaque exécution coûtait un tour : cinq tuiles coûtaient
 * cinq tours, exactement comme cinq actions jouées une à une. La file n'était qu'une contrainte
 * d'ordre sans contrepartie, et aucun enchaînement ne pouvait en sortir puisque chaque exécution
 * était une action à elle seule — alors que le tableau de bord promettait depuis M5 « trois effets
 * pendant que les ennemis n'avancent que d'un cran ».
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
        @DisplayName("La salve part en entier, du sommet vers le bas")
        void theVolleyFiresTopDown() {
            Arena arena = new Arena(15, 0);
            arena.queueTile(Tile.STRIKE);
            arena.queueTile(Tile.PUSH);
            arena.queueTile(Tile.DASH);

            arena.unleash();

            assertTrue(arena.queue().isEmpty(), "la salve emporte toute la file");
        }

        /**
         * L'ordre se mesure par ses effets, pas par l'état de la file : depuis que la salve part en
         * entier, on ne peut plus l'observer au sommet entre deux coups.
         *
         * <p>Le montage sépare les deux ordres possibles : la volte-face avant l'élan renvoie le
         * héros à son point de départ, l'élan avant la volte-face l'envoie au bout.
         */
        @Test
        @DisplayName("Le dernier posé agit le premier, et ça change le résultat")
        void theLastQueuedActsFirstAndItShows() {
            Arena runThenTurn = new Arena(15, 0);
            runThenTurn.queueTile(Tile.PIVOT);   // posé en premier, donc joué en dernier
            runThenTurn.queueTile(Tile.DASH);
            runThenTurn.unleash();

            Arena turnThenRun = new Arena(15, 0);
            turnThenRun.queueTile(Tile.DASH);
            turnThenRun.queueTile(Tile.PIVOT);   // posé en dernier, donc joué en premier
            turnThenRun.unleash();

            assertEquals(14, runThenTurn.heroCell(), "l'elan a joue avant la volte-face");
            assertEquals(0, turnThenRun.heroCell(), "la volte-face a joue avant l'elan");
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
            int turns = arena.turnsTaken();
            assertEquals(ActionResult.QUEUE_FULL, arena.queueTile(tiles[ActionQueue.CAPACITY]));
            assertEquals(turns, arena.turnsTaken(), "un refus ne coute pas de tour");

            assertEquals(ActionQueue.CAPACITY, arena.queue().size(), "la file n'a pas deborde");
            assertTrue(arena.rack().holds(tiles[ActionQueue.CAPACITY]),
                    "la tuile refusee doit rester au rateleir");
        }
    }

    @Nested
    @DisplayName("Charger coûte, lâcher coûte une fois")
    class TurnEconomy {

        /**
         * Le cœur de l'économie : charger est le moment vulnérable.
         *
         * <p>C'est ce qui donne un prix à la file. Sans lui, cinq tuiles coûtaient cinq tours comme
         * cinq actions séparées et la file ne servait à rien.
         */
        @Test
        @DisplayName("Poser une tuile consomme un tour")
        void queueingCostsATurn() {
            Arena arena = arena();
            int before = arena.turnsTaken();

            assertEquals(ActionResult.QUEUED, arena.queueTile(Tile.STRIKE));
            assertEquals(ActionResult.QUEUED, arena.queueTile(Tile.PUSH));

            assertEquals(before + 2, arena.turnsTaken(), "charger est le moment vulnerable");
        }

        /**
         * Et la contrepartie : cinq effets pendant que les ennemis n'avancent que d'un cran. C'est
         * mot pour mot ce que le tableau de bord promettait depuis M5.
         */
        @Test
        @DisplayName("Une salve de cinq ne coûte qu'un tour")
        void aVolleyOfFiveCostsOneTurn() {
            Arena arena = new Arena(15, 7);
            arena.grid().place(8, new Pawn("cible"));
            for (Tile tile : new Tile[]{Tile.STRIKE, Tile.THRUST, Tile.PUSH, Tile.DASH, Tile.PIVOT}) {
                arena.queueTile(tile);
            }
            int before = arena.turnsTaken();

            arena.unleash();

            assertEquals(before + 1, arena.turnsTaken(),
                    "cinq effets doivent tomber pendant que les ennemis n'avancent que d'un cran");
            assertTrue(arena.queue().isEmpty());
        }

        /**
         * Une salve dont rien ne porte est déjà punie : les tuiles sont dépensées et partent en
         * recharge. On ne fait pas payer deux fois une décision déjà punie — c'est la règle de M5,
         * transposée à la salve entière.
         */
        @Test
        @DisplayName("Une salve qui ne porte sur rien ne coûte pas de tour")
        void avolleyThatConnectsWithNothingCostsNoTurn() {
            Arena arena = new Arena(15, 7);
            arena.queueTile(Tile.STRIKE);   // personne devant
            arena.queueTile(Tile.THRUST);   // personne a deux cases non plus
            int before = arena.turnsTaken();

            arena.unleash();

            assertEquals(before, arena.turnsTaken(), "rien n'a porte : rien de plus a payer");
            assertFalse(arena.rack().isReady(Tile.STRIKE), "mais les tuiles sont bien depensees");
        }

        /**
         * Reprendre reste le seul geste gratuit — mais il ne rend pas le tour dépensé.
         *
         * <p>Le joueur paie son engagement, pas le fait de changer d'avis sur une tuile qui n'a rien
         * fait. Il n'y a donc rien à gagner à remuer la file : juste de quoi ne pas être puni deux
         * fois pour une main qui a glissé.
         */
        @Test
        @DisplayName("Reprendre ne coûte rien, mais ne rend pas le tour dépensé")
        void unqueueingIsFreeButGivesNothingBack() {
            Arena arena = arena();
            arena.queueTile(Tile.STRIKE);
            int before = arena.turnsTaken();

            assertEquals(ActionResult.UNQUEUED, arena.unqueueAt(0));

            assertEquals(before, arena.turnsTaken(), "reprendre ne fait pas passer le temps");
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
            arena.unleash();

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
            arena.unleash();  // la salve entiere part, un tour consomme

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
            arena.unleash();

            assertEquals(ActionResult.NOT_READY, arena.queueTile(Tile.DASH));
        }

        @Test
        @DisplayName("Après assez de tours, la tuile revient")
        void afterEnoughTurnsTheTileComesBack() {
            Arena arena = new Arena(15, 0);
            arena.queueTile(Tile.DASH);
            arena.unleash();

            int guard = 0;
            while (!arena.rack().isReady(Tile.DASH) && guard++ < 20) {
                arena.step(arena.hero().facing().opposite());
            }

            assertTrue(arena.rack().isReady(Tile.DASH), "la tuile n'est jamais revenue");
            assertEquals(ActionResult.QUEUED, arena.queueTile(Tile.DASH));
        }

        /**
         * Reprendre en boucle ne fait rien avancer : c'est ce qui reste de la promesse « se préparer
         * ne doit jamais punir » une fois que poser est devenu payant.
         */
        @Test
        @DisplayName("Reprendre en boucle ne fait avancer aucune recharge")
        void takingBackNeverAdvancesTime() {
            Arena arena = arena();
            arena.queueTile(Tile.STRIKE);
            arena.unleash();
            int missing = arena.rack().missingPoints(Tile.STRIKE);
            int turns = arena.turnsTaken();

            arena.queueTile(Tile.PUSH);
            for (int i = 0; i < 50; i++) {
                arena.unqueueAt(0);
            }

            assertEquals(missing - 1, arena.rack().missingPoints(Tile.STRIKE),
                    "un seul tour a passe, celui de la pose");
            assertEquals(turns + 1, arena.turnsTaken(), "et un seul");
        }
    }

    @Nested
    @DisplayName("Free-Play")
    class FreePlay {

        /**
         * La soupape a suivi le coût.
         *
         * <p>Free-Play voulait dire « s'exécute sans consommer de tour » quand l'exécution était le
         * moment payant. Maintenant que c'est le chargement, une tuile Free-Play <b>se pose</b>
         * gratuitement — et une salve qui ne contient qu'elles ne coûte rien non plus. Se
         * repositionner sans offrir un tour reste possible, ce qui était toute leur raison d'être.
         */
        @Test
        @DisplayName("Poser une tuile Free-Play ne consomme pas de tour")
        void queueingAFreePlayTileCostsNoTurn() {
            Arena arena = arena();
            int before = arena.turnsTaken();

            assertEquals(ActionResult.QUEUED, arena.queueTile(Tile.SIDESTEP));

            assertEquals(before, arena.turnsTaken(), "la soupape se charge gratuitement");
            assertTrue(Tile.SIDESTEP.isFreePlay());
        }

        @Test
        @DisplayName("Une salve entièrement Free-Play ne coûte rien non plus")
        void anAllFreePlayVolleyCostsNothing() {
            Arena arena = arena();
            arena.queueTile(Tile.PIVOT);
            arena.queueTile(Tile.SIDESTEP);
            int before = arena.turnsTaken();

            arena.unleash();

            assertEquals(before, arena.turnsTaken(), "se repositionner ne doit rien offrir");
        }

        @Test
        @DisplayName("Mais une seule tuile payante suffit à faire coûter la salve")
        void oneCostlyTileMakesTheWholeVolleyCost() {
            Arena arena = new Arena(9, 4);
            arena.grid().place(5, new Pawn("cible"));
            arena.queueTile(Tile.PIVOT);
            arena.queueTile(Tile.STRIKE);
            int before = arena.turnsTaken();

            arena.unleash();

            assertEquals(before + 1, arena.turnsTaken());
        }

        @Test
        @DisplayName("Mais elle part quand même en recharge")
        void butItStillGoesOnCooldown() {
            Arena arena = arena();
            arena.queueTile(Tile.SIDESTEP);
            arena.unleash();

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
            byTile.unleash();

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
            arena.unleash();

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

            assertEquals(ActionResult.EMPTY_QUEUE, arena.unleash());
            assertEquals(0, arena.turnsTaken());
        }

        @Test
        @DisplayName("Une tuile dont l'effet échoue est consommée mais ne coûte pas de tour")
        void aTileThatMissesIsSpentButFree() {
            Arena arena = arena();
            arena.queueTile(Tile.STRIKE); // personne devant
            int before = arena.turnsTaken();

            assertEquals(ActionResult.NO_TARGET, arena.unleash());

            assertTrue(arena.queue().isEmpty(), "la tuile est bien consommee");
            assertFalse(arena.rack().isReady(Tile.STRIKE), "et bien mise en recharge");
            assertEquals(before, arena.turnsTaken(), "mais elle ne fait pas payer deux fois");
        }

        @Test
        @DisplayName("La frappe entame la cible sans la tuer si elle a plusieurs points de vie")
        void theStrikeDamagesWithoutKilling() {
            Arena arena = arena();
            int target = arena.heroCell() + 1;
            Enemy victim = new Enemy(EnemyKind.LANCIER); // deux points de vie
            arena.grid().place(target, victim);
            arena.announceIntentions();

            arena.queueTile(Tile.STRIKE);
            assertEquals(ActionResult.STRUCK, arena.unleash());

            assertEquals(1, victim.health(), "entame, pas tue");
            assertFalse(arena.grid().isFree(target), "il tient encore sa case");
        }

        @Test
        @DisplayName("Une cible à un seul point de vie tombe du premier coup")
        void aOneHitEnemyFallsAtOnce() {
            Arena arena = arena();
            int target = arena.heroCell() + 1;
            arena.grid().place(target, new Enemy(EnemyKind.SABREUR));
            arena.announceIntentions();

            arena.queueTile(Tile.STRIKE);
            assertEquals(ActionResult.STRUCK, arena.unleash());

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
            assertEquals(ActionResult.PUSHED, arena.unleash());

            assertTrue(arena.grid().isFree(target));
            assertSame(victim, arena.grid().occupantAt(target + 1));
        }

        @Test
        @DisplayName("Une poussée contre un mur blesse au lieu d'être bloquée")
        void aPushIntoAWallHurts() {
            // C'est la regle de M7 : une poussee qui aboutit deplace, une poussee qui butte blesse.
            // Le mur devient une arme, et la position de l'ennemi compte autant que sa sante.
            Arena arena = new Arena(9, 7);
            Enemy victim = new Enemy(EnemyKind.COLOSSE);
            arena.grid().place(8, victim);
            arena.announceIntentions();

            arena.queueTile(Tile.PUSH);
            assertEquals(ActionResult.COLLIDED, arena.unleash());

            assertEquals(victim.maxHealth() - Arena.COLLISION_DAMAGE, victim.health());
        }

        @Test
        @DisplayName("Une poussée contre un mur prive l'ennemi de son action")
        void aPushIntoAWallDeniesTheEnemyItsTurn() {
            // L'etourdissement est pose et consomme dans le meme geste : c'est tout son interet.
            // On ne peut donc pas l'observer apres coup, seulement constater qu'aucun coup n'est
            // parti alors qu'un coup etait annonce.
            Arena arena = new Arena(9, 7);
            // On charge AVANT de poser l'ennemi : depuis que charger consomme un tour, le faire
            // sous le feu offrirait a l'ennemi une frappe qui n'a rien a voir avec ce qu'on mesure.
            arena.queueTile(Tile.PUSH);

            Enemy victim = new Enemy(EnemyKind.SABREUR);
            arena.grid().place(8, victim);
            arena.announceIntentions();
            assertEquals(1, arena.threatCount(7), "le sabreur annonce bien une frappe");

            arena.unleash();

            assertEquals(0, arena.heroHits(), "l'ennemi etourdi n'a pas frappe");
        }

        @Test
        @DisplayName("L'élan charge jusqu'au premier obstacle")
        void theDashRunsUntilBlocked() {
            Arena arena = new Arena(15, 0);
            arena.grid().place(6, new Pawn("obstacle"));

            arena.queueTile(Tile.DASH);
            assertEquals(ActionResult.DASHED, arena.unleash());

            assertEquals(5, arena.heroCell(), "arrete juste avant l'obstacle");
        }

        @Test
        @DisplayName("Un élan sans place disponible est bloqué")
        void aDashWithNowhereToGoIsBlocked() {
            Arena arena = new Arena(9, 8);

            arena.queueTile(Tile.DASH);
            assertEquals(ActionResult.BLOCKED, arena.unleash());
            assertEquals(8, arena.heroCell());
        }
    }
}
