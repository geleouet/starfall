package com.starfall.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests des ennemis et de leur télégraphe.
 *
 * <p>La propriété qui compte plus que toutes les autres : <b>ce qui est annoncé est ce qui est
 * joué</b>. Si un ennemi recalculait sa cible au moment de frapper, le télégraphe serait un
 * mensonge, l'esquive n'existerait pas, et le jeu n'aurait plus rien à lire.
 */
class EnemyTest {

    /** Arène nue : aucune vague posée, on place exactement ce qu'on veut observer. */
    private static Arena bare(int width, int heroCell) {
        return new Arena(width, heroCell);
    }

    private static Enemy place(Arena arena, int cell, EnemyKind kind, Trait... traits) {
        Enemy enemy = new Enemy(kind, traits);
        arena.grid().place(cell, enemy);
        arena.announceIntentions();
        return enemy;
    }

    /** Fait passer un tour sans que le héros ne bouge ni ne frappe. */
    private static void passTurn(Arena arena) {
        // Un demi-tour consomme un tour sans changer la position : c'est le moyen le plus neutre
        // de laisser les ennemis jouer.
        arena.step(arena.hero().facing().opposite());
    }

    @Nested
    @DisplayName("Le télégraphe est une promesse")
    class TelegraphIsAPromise {

        @Test
        @DisplayName("L'ennemi frappe la case annoncée, même si le héros s'est écarté")
        void theEnemyStrikesTheAnnouncedCellEvenIfTheHeroLeft() {
            Arena arena = bare(9, 4);
            Enemy sabreur = place(arena, 5, EnemyKind.SABREUR);

            assertEquals(Intention.Kind.ATTACK, sabreur.intention().kind());
            assertEquals(4, sabreur.intention().targetCell(), "il annonce la case du heros");

            // Le heros se retourne puis s'ecarte : deux tours, donc deux phases ennemies.
            arena.step(Direction.LEFT);   // demi-tour : la frappe annoncee porte
            assertEquals(1, arena.heroHits());

            arena.step(Direction.LEFT);   // il quitte enfin la case 4
            assertEquals(3, arena.heroCell());
        }

        @Test
        @DisplayName("Quitter la case menacée suffit à esquiver")
        void steppingOutOfTheThreatenedCellDodges() {
            Arena arena = bare(9, 4);
            place(arena, 6, EnemyKind.ARCHER);

            assertTrue(arena.isThreatened(4), "l'archer a portee 3 menace deja le heros");

            // Le heros regarde a droite ; un pas a droite le rapproche mais quitte la case 4.
            arena.step(Direction.RIGHT);

            assertEquals(5, arena.heroCell());
            assertEquals(0, arena.heroHits(), "la fleche est partie sur la case 4, desormais vide");
        }

        @Test
        @DisplayName("Rester sur la case menacée coûte un coup")
        void stayingOnTheThreatenedCellCosts() {
            Arena arena = bare(9, 4);
            place(arena, 6, EnemyKind.ARCHER);

            passTurn(arena); // demi-tour : on ne bouge pas de case

            assertEquals(4, arena.heroCell());
            assertEquals(1, arena.heroHits());
        }

        @Test
        @DisplayName("La case menacée est signalée avant que le joueur n'agisse")
        void theThreatenedCellIsKnownBeforeThePlayerActs() {
            Arena arena = bare(9, 4);
            place(arena, 5, EnemyKind.SABREUR);

            assertEquals(0, arena.turnsTaken());
            assertTrue(arena.isThreatened(4));
            assertFalse(arena.isThreatened(3));
            assertFalse(arena.isThreatened(6));
        }
    }

    @Nested
    @DisplayName("Archétypes")
    class Archetypes {

        @Test
        @DisplayName("Le sabreur avance tant qu'il n'est pas au contact, puis frappe")
        void theSabreurClosesInThenStrikes() {
            Arena arena = bare(9, 0);
            Enemy sabreur = place(arena, 4, EnemyKind.SABREUR);

            assertEquals(Intention.Kind.ADVANCE, sabreur.intention().kind());
            passTurn(arena);
            assertEquals(3, arena.grid().indexOf(sabreur));

            passTurn(arena);
            assertEquals(2, arena.grid().indexOf(sabreur));
            passTurn(arena);
            assertEquals(1, arena.grid().indexOf(sabreur));
            assertEquals(Intention.Kind.ATTACK, sabreur.intention().kind());

            int hitsBefore = arena.heroHits();
            passTurn(arena);
            assertEquals(hitsBefore + 1, arena.heroHits());
        }

        @Test
        @DisplayName("L'archer frappe de loin et recule si on le colle")
        void theArcherShootsFarAndBacksAwayWhenAdjacent() {
            Arena far = bare(9, 0);
            Enemy archer = place(far, 3, EnemyKind.ARCHER);
            assertEquals(Intention.Kind.ATTACK, archer.intention().kind(), "portee 3");

            Arena close = bare(9, 0);
            Enemy cornered = place(close, 1, EnemyKind.ARCHER);
            assertEquals(Intention.Kind.ADVANCE, cornered.intention().kind());
            assertEquals(2, cornered.intention().targetCell(), "il recule, il n'avance pas");
        }

        @Test
        @DisplayName("Le lancier prend son élan, puis charge la case annoncée")
        void theLancierWindsUpThenCharges() {
            Arena arena = bare(9, 0);
            Enemy lancier = place(arena, 5, EnemyKind.LANCIER);

            assertEquals(Intention.Kind.WIND_UP, lancier.intention().kind());
            passTurn(arena);
            assertEquals(5, arena.grid().indexOf(lancier), "prendre son elan ne deplace pas");
            assertEquals(Intention.Kind.CHARGE, lancier.intention().kind());
            assertEquals(0, lancier.intention().targetCell());

            passTurn(arena);
            assertEquals(1, arena.grid().indexOf(lancier), "il vient au contact");
            assertEquals(1, arena.heroHits());
        }

        @Test
        @DisplayName("Le colosse n'agit qu'une phase sur deux")
        void theColosseActsEveryOtherPhase() {
            Arena arena = bare(9, 0);
            Enemy colosse = place(arena, 4, EnemyKind.COLOSSE);

            passTurn(arena);
            int afterFirst = arena.grid().indexOf(colosse);
            passTurn(arena);
            int afterSecond = arena.grid().indexOf(colosse);
            passTurn(arena);
            int afterThird = arena.grid().indexOf(colosse);

            assertNotEquals(4, afterFirst, "il agit a la premiere phase");
            assertEquals(afterFirst, afterSecond, "puis il passe son tour");
            assertNotEquals(afterSecond, afterThird, "puis il repart");
        }
    }

    @Nested
    @DisplayName("Traits")
    class Traits {

        @Test
        @DisplayName("Un ennemi rapide avance de deux cases en une seule intention")
        void aFastEnemyActsTwice() {
            Arena normal = bare(15, 0);
            Enemy slow = place(normal, 10, EnemyKind.SABREUR);
            passTurn(normal);

            Arena fast = bare(15, 0);
            Enemy quick = place(fast, 10, EnemyKind.SABREUR, Trait.RAPIDE);
            passTurn(fast);

            assertEquals(9, normal.grid().indexOf(slow));
            assertEquals(8, fast.grid().indexOf(quick),
                    "une seule intention annoncee, mais de deux cases");
        }

        @Test
        @DisplayName("Un ennemi rapide frappe deux fois quand il frappe")
        void aFastEnemyStrikesTwice() {
            Arena arena = bare(9, 4);
            place(arena, 5, EnemyKind.SABREUR, Trait.RAPIDE);

            passTurn(arena);

            assertEquals(2, arena.heroHits(), "une frappe, deux coups");
        }

        @Test
        @DisplayName("Un ennemi agressif frappe une case plus loin que son archétype")
        void anAggressiveEnemyReachesOneCellFurther() {
            Arena normal = bare(9, 4);
            Enemy plain = place(normal, 6, EnemyKind.SABREUR);
            assertEquals(Intention.Kind.ADVANCE, plain.intention().kind(), "portee 1 : trop loin");

            Arena aggressive = bare(9, 4);
            Enemy reaching = place(aggressive, 6, EnemyKind.SABREUR, Trait.AGRESSIF);
            assertEquals(Intention.Kind.ATTACK, reaching.intention().kind(), "portee 2");
            assertEquals(4, reaching.intention().targetCell());
        }

        @Test
        @DisplayName("Un fonceur comble toute la distance d'un coup")
        void aChargerClosesTheWholeGap() {
            Arena arena = bare(15, 0);
            Enemy fonceur = place(arena, 12, EnemyKind.SABREUR, Trait.FONCEUR);

            assertEquals(1, fonceur.intention().targetCell(), "il vient droit au contact");
            passTurn(arena);
            assertEquals(1, arena.grid().indexOf(fonceur));
        }

        @Test
        @DisplayName("Un fonceur ne traverse pas ses camarades")
        void aChargerDoesNotWalkThroughItsFriends() {
            Arena arena = bare(15, 0);
            arena.grid().place(6, new Enemy(EnemyKind.COLOSSE));
            Enemy fonceur = place(arena, 12, EnemyKind.SABREUR, Trait.FONCEUR);

            assertEquals(11, fonceur.intention().targetCell(), "il se replie sur un pas simple");
        }

        @Test
        @DisplayName("Un explosif emporte ses voisins, et la chaîne se propage")
        void anExplosiveTakesItsNeighboursWithIt() {
            Arena arena = bare(15, 0);
            // Des archetypes legers : une explosion inflige deux points, donc elle les emporte et
            // la chaine se propage. Un colosse a pleine sante l'arreterait, et c'est voulu.
            Enemy first = new Enemy(EnemyKind.SABREUR, Trait.EXPLOSIF);
            Enemy second = new Enemy(EnemyKind.SABREUR, Trait.EXPLOSIF);
            Enemy third = new Enemy(EnemyKind.SABREUR);
            arena.grid().place(5, first);
            arena.grid().place(6, second);
            arena.grid().place(7, third);
            arena.announceIntentions();

            arena.kill(5);

            assertTrue(arena.grid().isFree(5));
            assertTrue(arena.grid().isFree(6), "le voisin explosif part aussi");
            assertTrue(arena.grid().isFree(7), "et la chaine emporte le suivant");
        }

        @Test
        @DisplayName("Une explosion au contact du héros le touche")
        void anExplosionNextToTheHeroHitsHim() {
            Arena arena = bare(9, 4);
            arena.grid().place(5, new Enemy(EnemyKind.SABREUR, Trait.EXPLOSIF));
            arena.announceIntentions();

            arena.kill(5);

            assertEquals(1, arena.heroHits());
            assertEquals(Hero.MAX_HEALTH - Arena.EXPLOSION_DAMAGE, arena.hero().health());
        }
    }

    @Nested
    @DisplayName("Cohérence de la phase ennemie")
    class PhaseConsistency {

        @Test
        @DisplayName("Les ennemis ne se traversent pas et ne se superposent jamais")
        void enemiesNeverOverlap() {
            java.util.Map<Intention.Kind, Integer> observed =
                    new java.util.EnumMap<>(Intention.Kind.class);

            // Toutes les vagues, pas seulement la premiere. Ce test dit « ne se TRAVERSENT pas »,
            // et les deux seuls mouvements qui traversent plusieurs cases - la charge du lancier et
            // la ruee du souverain - n'existent qu'a partir de la vague 2. Mesure du montage
            // precedent, qui ne jouait que des pas et des echanges : cinq cents parties, aucune ne
            // depassait la vague 1, deux especes sur cinq observees, zero ruee, zero invocation.
            // Il gardait la superposition de deux archetypes qui avancent d'une case.
            for (int startWave = 1; startWave <= Arena.WAVE_COUNT; startWave++) {
                for (int seed = 0; seed < 200; seed++) {
                    Random random = new Random(seed);
                    Arena arena = ArenaSetup.trainingArena(
                            Grid.MIN_WIDTH + random.nextInt(Grid.MAX_WIDTH - Grid.MIN_WIDTH + 1),
                            startWave);

                    for (int step = 0; step < 60; step++) {
                        for (Enemy enemy : arena.enemies()) {
                            observed.merge(enemy.intention().kind(), 1, Integer::sum);
                        }
                        if (random.nextBoolean()) {
                            arena.step(random.nextBoolean() ? Direction.LEFT : Direction.RIGHT);
                        } else {
                            arena.swapWithTarget();
                        }

                        java.util.Set<Occupant> seen = new java.util.HashSet<>();
                        for (int cell : arena.grid().occupiedCells()) {
                            assertTrue(seen.add(arena.grid().occupantAt(cell)),
                                    "vague " + startWave + " graine " + seed
                                            + " : un occupant est sur deux cases");
                        }
                        assertSame(arena.hero(), arena.grid().occupantAt(arena.heroCell()));
                    }
                }
            }

            // Et l'echantillon dit ce qu'il a vu. Sans ces deux lignes, le montage pourrait
            // retomber en vague 1 sans que personne ne s'en apercoive - c'est exactement ainsi que
            // le precedent avait vecu.
            assertTrue(observed.getOrDefault(Intention.Kind.CHARGE, 0) > 0,
                    "aucune charge observee : le mouvement qui traverse le plus de cases n'est pas"
                            + " dans l'echantillon, et c'est celui que ce test existe pour garder."
                            + " Intentions vues : " + observed);
            assertTrue(observed.getOrDefault(Intention.Kind.RUSH, 0) > 0,
                    "aucune ruee observee : le second mouvement traversant manque a l'echantillon."
                            + " Intentions vues : " + observed);
        }

        @Test
        @DisplayName("Un ennemi tué en cours de phase ne joue pas")
        void anEnemyKilledMidPhaseDoesNotAct() {
            Arena arena = bare(9, 4);
            // Charger d'abord, sur un plateau vide : poser coute un tour, et ce test porte sur la
            // phase ou l'ennemi meurt, pas sur celle d'avant.
            arena.queueTile(Tile.STRIKE);
            place(arena, 5, EnemyKind.SABREUR);

            arena.unleash(); // le tue, et consomme le tour

            assertEquals(0, arena.heroHits(), "un mort ne frappe pas");
            assertTrue(arena.enemies().isEmpty());
        }

        @Test
        @DisplayName("La phase ennemie est déterministe : mêmes gestes, même partie")
        void theEnemyPhaseIsDeterministic() {
            // L'ordre d'activation est fixe, de gauche a droite : deux parties identiques doivent
            // produire exactement le meme plateau.
            String first = playScript();
            for (int repeat = 0; repeat < 5; repeat++) {
                assertEquals(first, playScript(), "la partie n'est pas reproductible");
            }
        }

        private String playScript() {
            Arena arena = ArenaSetup.trainingArena(11);
            Direction[] script = {Direction.RIGHT, Direction.RIGHT, Direction.LEFT,
                    Direction.LEFT, Direction.RIGHT, Direction.RIGHT};
            for (Direction direction : script) {
                arena.step(direction);
            }
            StringBuilder state = new StringBuilder();
            for (int cell = 0; cell < arena.grid().width(); cell++) {
                Occupant occupant = arena.grid().occupantAt(cell);
                state.append(occupant == null ? "."
                        : occupant == arena.hero() ? "H" : occupant.label().charAt(0));
            }
            return state.append('|').append(arena.heroHits())
                    .append('|').append(arena.turnsTaken()).toString();
        }
    }
}
