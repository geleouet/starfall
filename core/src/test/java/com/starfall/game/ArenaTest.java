package com.starfall.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests des règles du combat : déplacement, orientation, capacité spéciale. */
class ArenaTest {

    private record Pawn(String label, long id) implements Occupant {
        // Le numero est un COMPOSANT, et non un « return Identities.next() » dans
        // l'accesseur : ce dernier rendrait un numero neuf a chaque lecture, soit
        // l'exact contraire de ce qu'un numero d'identite promet.
        Pawn(String label) {
            this(label, Identities.next());
        }

        @Override
        public String spriteName() {
            return "enemy/sabreur";
        }
    }

    private static Occupant pawn(String label) {
        return new Pawn(label);
    }

    @Nested
    @DisplayName("Mise en place")
    class Setup {

        @Test
        @DisplayName("Le héros démarre au milieu et regarde à droite")
        void theHeroStartsInTheMiddleFacingRight() {
            Arena arena = new Arena(9);

            assertEquals(4, arena.heroCell());
            assertEquals(Direction.RIGHT, arena.hero().facing());
            assertSame(arena.hero(), arena.grid().occupantAt(4));
        }

        @Test
        @DisplayName("Le milieu d'une grille paire penche à droite, et c'est stable")
        void theMiddleOfAnEvenGridIsWellDefined() {
            // Grille de 6 : indices 0..5, milieux 2 et 3 ; on prend 3, donc a droite.
            assertEquals(3, new Arena(6).heroCell());
            assertEquals(5, new Arena(10).heroCell());
        }
    }

    /**
     * Le prédicat et le geste disent la même chose, sur de vraies parties.
     *
     * <p>{@code canStep} a été extrait pour que l'énumération de l'instrument de mesure et le
     * surlignage de la scène cessent de recopier la règle. Deux consommateurs qui s'accordent sur
     * un prédicat faux s'accordent quand même : ce qu'il faut garder, c'est que le prédicat dise
     * exactement ce que {@link Arena#step} fera.
     *
     * <p>Et le test compte ce qu'il a vu. Un accord observé seulement sur des « oui » ne dit rien :
     * il faut avoir rencontré les deux réponses.
     */
    @Test
    @DisplayName("canStep prédit exactement ce que step fait")
    void canStepPredictsExactlyWhatStepDoes() {
        int allowed = 0;
        int refused = 0;

        for (int wave = 1; wave <= Arena.WAVE_COUNT; wave++) {
            for (int seed = 0; seed < 60; seed++) {
                java.util.Random random = new java.util.Random(seed);
                Arena arena = ArenaSetup.trainingArena(
                        Grid.MIN_WIDTH + random.nextInt(Grid.MAX_WIDTH - Grid.MIN_WIDTH + 1), wave);

                for (int turn = 0; turn < 40; turn++) {
                    Direction direction = random.nextBoolean() ? Direction.LEFT : Direction.RIGHT;
                    boolean predicted = arena.canStep(direction);
                    ActionResult result = arena.step(direction);
                    assertEquals(predicted, result != ActionResult.BLOCKED,
                            "vague " + wave + " graine " + seed + " tour " + turn + " : canStep dit "
                                    + predicted + " et step rend " + result);
                    if (predicted) {
                        allowed++;
                    } else {
                        refused++;
                    }
                }
            }
        }

        assertTrue(allowed > 0, "aucun pas accepte : l'accord n'a ete observe que sur des refus");
        assertTrue(refused > 0,
                "aucun pas refuse : l'accord n'a ete observe que sur des « oui », et un predicat"
                        + " qui dirait toujours vrai passerait ce test");
    }

    @Nested
    @DisplayName("Orientation et déplacement")
    class Movement {

        @Test
        @DisplayName("Aller vers là où on regarde : on avance")
        void steppingForwardMoves() {
            Arena arena = new Arena(9);
            int start = arena.heroCell();

            assertEquals(ActionResult.MOVED, arena.step(Direction.RIGHT));
            assertEquals(start + 1, arena.heroCell());
        }

        @Test
        @DisplayName("Aller vers là où on ne regarde pas : on se retourne, sans bouger")
        void steppingBackwardsTurnsWithoutMoving() {
            Arena arena = new Arena(9);
            int start = arena.heroCell();

            assertEquals(ActionResult.TURNED, arena.step(Direction.LEFT));
            assertEquals(Direction.LEFT, arena.hero().facing());
            assertEquals(start, arena.heroCell(), "le demi-tour ne deplace pas");

            assertEquals(ActionResult.MOVED, arena.step(Direction.LEFT));
            assertEquals(start - 1, arena.heroCell());
        }

        @Test
        @DisplayName("Le bord de la grille bloque, sans faire sortir le héros")
        void theEdgeBlocks() {
            Arena arena = new Arena(5, 4);

            assertEquals(ActionResult.BLOCKED, arena.step(Direction.RIGHT));
            assertEquals(4, arena.heroCell());
            assertTrue(arena.grid().contains(arena.heroCell()));
        }

        @Test
        @DisplayName("Un occupant bloque aussi")
        void anOccupantBlocks() {
            Arena arena = new Arena(9);
            arena.grid().place(arena.heroCell() + 1, pawn("mur"));

            assertEquals(ActionResult.BLOCKED, arena.step(Direction.RIGHT));
            assertEquals(4, arena.heroCell());
        }

        @Test
        @DisplayName("Le héros ne quitte jamais la grille, quoi qu'on tape")
        void theHeroNeverLeavesTheGrid() {
            Arena arena = new Arena(5);

            // Une rafale d'entrees dans tous les sens ne doit jamais produire une position invalide.
            Direction[] script = {
                    Direction.LEFT, Direction.LEFT, Direction.LEFT, Direction.LEFT, Direction.LEFT,
                    Direction.RIGHT, Direction.RIGHT, Direction.RIGHT, Direction.RIGHT,
                    Direction.RIGHT, Direction.RIGHT, Direction.RIGHT, Direction.LEFT,
            };
            for (Direction direction : script) {
                arena.step(direction);
                int cell = arena.heroCell();
                assertTrue(arena.grid().contains(cell), "position invalide : " + cell);
                assertSame(arena.hero(), arena.grid().occupantAt(cell));
            }
        }
    }

    @Nested
    @DisplayName("Capacité spéciale : échange de place")
    class SwapAbility {

        @Test
        @DisplayName("La Vagabonde échange sa place avec le premier occupant devant elle")
        void theHeroSwapsWithTheFirstOccupantAhead() {
            Arena arena = new Arena(9);
            Occupant target = pawn("cible");
            arena.grid().place(7, target);

            assertEquals(7, arena.swapTarget());
            assertEquals(ActionResult.SWAPPED, arena.swapWithTarget());

            assertEquals(7, arena.heroCell());
            assertSame(target, arena.grid().occupantAt(4), "la cible prend la place du heros");
        }

        @Test
        @DisplayName("L'échange traverse la ligne : c'est le premier devant qui compte, pas le plus loin")
        void theSwapTakesTheNearestTarget() {
            Arena arena = new Arena(9);
            arena.grid().place(6, pawn("proche"));
            arena.grid().place(8, pawn("loin"));

            assertEquals(6, arena.swapTarget());
            arena.swapWithTarget();
            assertEquals(6, arena.heroCell());
        }

        @Test
        @DisplayName("L'orientation choisit la cible")
        void facingSelectsTheTarget() {
            Arena arena = new Arena(9);
            arena.grid().place(1, pawn("derriere"));
            arena.grid().place(7, pawn("devant"));

            assertEquals(7, arena.swapTarget());
            arena.step(Direction.LEFT); // demi-tour
            assertEquals(1, arena.swapTarget());
        }

        @Test
        @DisplayName("Sans cible, la capacité le dit et ne coûte rien")
        void withoutATargetTheAbilitySaysSo() {
            Arena arena = new Arena(9);

            assertEquals(-1, arena.swapTarget());
            assertEquals(ActionResult.NO_TARGET, arena.swapWithTarget());
            assertEquals(4, arena.heroCell());
            assertEquals(0, arena.turnsTaken(), "une action qui echoue ne coute pas de tour");
        }

        @Test
        @DisplayName("Échanger deux fois de suite ramène tout le monde à sa place")
        void swappingTwiceIsIdentity() {
            Arena arena = new Arena(9);
            Occupant target = pawn("cible");
            arena.grid().place(7, target);

            arena.swapWithTarget();
            // Le heros regarde toujours a droite, mais la cible est maintenant derriere lui.
            arena.step(Direction.LEFT);
            arena.swapWithTarget();

            assertEquals(4, arena.heroCell());
            assertSame(target, arena.grid().occupantAt(7));
        }
    }

    @Nested
    @DisplayName("Clic sur une case")
    class Clicking {

        @Test
        @DisplayName("Cliquer devant soi avance d'une seule case")
        void clickingAheadMovesExactlyOneCell() {
            Arena arena = new Arena(15, 0);
            arena.hero().face(Direction.RIGHT);

            // Un clic a l'autre bout ne doit pas enchainer dix tours d'un coup.
            assertEquals(ActionResult.MOVED, arena.clickOn(14));
            assertEquals(1, arena.heroCell());
        }

        @Test
        @DisplayName("Cliquer derrière soi fait d'abord se retourner, et ça coûte un tour")
        void clickingBehindTurnsFirst() {
            Arena arena = new Arena(9);

            assertEquals(ActionResult.TURNED, arena.clickOn(0));
            assertEquals(Direction.LEFT, arena.hero().facing());
            assertEquals(4, arena.heroCell());
            // Sans cette ligne, le demi-tour a la souris etait gratuit alors que le meme geste au
            // clavier coutait un tour - ce qui rendait la tuile volte-face sans valeur.
            assertEquals(1, arena.turnsTaken());

            assertEquals(ActionResult.MOVED, arena.clickOn(0));
            assertEquals(3, arena.heroCell());
            assertEquals(2, arena.turnsTaken());
        }

        @Test
        @DisplayName("Souris et clavier facturent le même nombre de tours")
        void bothInputsChargeTheSameNumberOfTurns() {
            // C'est la monnaie du jeu : deux chemins qui menent au meme etat doivent couter pareil,
            // sinon l'un des deux est strictement meilleur et le choix d'entree n'en est plus un.
            for (int target = 0; target < 9; target++) {
                Arena byKey = new Arena(9);
                Arena byMouse = new Arena(9);
                if (target == byKey.heroCell()) {
                    continue;
                }
                Direction direction = Direction.towards(byKey.heroCell(), target);

                byMouse.clickOn(target);
                byKey.step(direction);

                assertEquals(byKey.heroCell(), byMouse.heroCell(), "case, cible " + target);
                assertEquals(byKey.hero().facing(), byMouse.hero().facing(), "regard, cible " + target);
                assertEquals(byKey.turnsTaken(), byMouse.turnsTaken(), "tours, cible " + target);
            }
        }

        @Test
        @DisplayName("Cliquer sur la cible de la capacité déclenche l'échange")
        void clickingTheSwapTargetTriggersTheAbility() {
            Arena arena = new Arena(9);
            Occupant target = pawn("cible");
            arena.grid().place(7, target);

            assertEquals(ActionResult.SWAPPED, arena.clickOn(7));
            assertEquals(7, arena.heroCell());
            assertSame(target, arena.grid().occupantAt(4));
        }

        @Test
        @DisplayName("Cliquer sur sa propre case ou hors grille ne fait rien")
        void clickingNowhereDoesNothing() {
            Arena arena = new Arena(9);

            assertEquals(ActionResult.BLOCKED, arena.clickOn(4));
            assertEquals(ActionResult.BLOCKED, arena.clickOn(-1));
            assertEquals(ActionResult.BLOCKED, arena.clickOn(9));
            assertEquals(4, arena.heroCell());
        }

        @Test
        @DisplayName("Une même intention se joue au clavier comme à la souris")
        void thesameIntentIsReachableWithEitherInput() {
            // Les deux entrees sont de plein droit : « avancer d'une case puis echanger » doit se
            // jouer avec l'une comme avec l'autre, et donner le meme etat.
            Arena byKeyboard = new Arena(9);
            Arena byMouse = new Arena(9);
            byKeyboard.grid().place(7, pawn("cible"));
            byMouse.grid().place(7, pawn("cible"));

            byKeyboard.step(Direction.RIGHT);
            byKeyboard.swapWithTarget();

            byMouse.clickOn(5);
            byMouse.clickOn(7);

            assertEquals(byKeyboard.heroCell(), byMouse.heroCell());
            assertEquals(byKeyboard.hero().facing(), byMouse.hero().facing());
            assertEquals(byKeyboard.grid().occupiedCells(), byMouse.grid().occupiedCells());
            assertEquals(byKeyboard.turnsTaken(), byMouse.turnsTaken(), "meme cout en tours");
        }

        @Test
        @DisplayName("Clic et flèche ne sont pas interchangeables sur une case occupée, et c'est voulu")
        void clickingAnOccupiedNeighbourIsNotTheSameAsSteppingIntoIt() {
            // Il serait faux d'affirmer que les deux entrees font toujours la meme chose : sur un
            // voisin occupe, la fleche se cogne alors que le clic designe une intention sans
            // ambiguite - « je veux aller la » - et la seule facon d'y aller est l'echange.
            Arena byKey = new Arena(9);
            Arena byClick = new Arena(9);
            byKey.grid().place(5, pawn("voisin"));
            byClick.grid().place(5, pawn("voisin"));

            assertEquals(ActionResult.BLOCKED, byKey.step(Direction.RIGHT));
            assertEquals(4, byKey.heroCell());

            assertEquals(ActionResult.SWAPPED, byClick.clickOn(5));
            assertEquals(5, byClick.heroCell());
        }
    }

    @Nested
    @DisplayName("Garde-fous d'API")
    class Guards {

        @Test
        @DisplayName("Une direction nulle est refusée au lieu de corrompre l'orientation")
        void aNullDirectionIsRejected() {
            // Direction.towards peut rendre null ; l'accepter posait une orientation nulle en
            // silence, et le premier calcul de cible suivant partait en NullPointerException.
            Arena arena = new Arena(9);

            assertThrows(IllegalArgumentException.class, () -> arena.step(null));
            assertEquals(Direction.RIGHT, arena.hero().facing(), "l'orientation doit etre intacte");
            assertEquals(-1, arena.swapTarget(), "et l'arene doit rester utilisable");
        }
    }

    @Nested
    @DisplayName("Mise en place d'une vague")
    class WaveSetup {

        @ParameterizedTest(name = "grille de {0} cases")
        @ValueSource(ints = {5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15})
        @DisplayName("Il y a toujours un ennemi de chaque côté du héros")
        void thereIsAlwaysAnEnemyOnBothSides(int gridWidth) {
            // Sans ca, la moitie des mecaniques d'orientation ne serait jamais mise a l'epreuve.
            Arena arena = ArenaSetup.trainingArena(gridWidth);
            int hero = arena.heroCell();

            assertTrue(arena.grid().firstOccupied(hero, Direction.LEFT) >= 0,
                    "aucun ennemi a gauche sur une grille de " + gridWidth);
            assertTrue(arena.grid().firstOccupied(hero, Direction.RIGHT) >= 0,
                    "aucun ennemi a droite sur une grille de " + gridWidth);
        }

        @ParameterizedTest(name = "grille de {0} cases")
        @ValueSource(ints = {5, 7, 9, 15})
        @DisplayName("La vague ne recouvre jamais le héros, et chaque ennemi est une instance à lui")
        void theWaveNeverOverlapsTheHero(int gridWidth) {
            Arena arena = ArenaSetup.trainingArena(gridWidth);

            assertSame(arena.hero(), arena.grid().occupantAt(arena.heroCell()));
            assertTrue(arena.enemies().size() >= 2,
                    "vague de " + arena.enemies().size() + " sur une grille de " + gridWidth);
            assertEquals(arena.enemies().size(), new java.util.HashSet<>(arena.enemies()).size(),
                    "deux cases partagent la meme instance d'ennemi");
        }

        @Test
        @DisplayName("Chaque ennemi a annoncé son intention avant le premier geste du joueur")
        void everyEnemyHasAnnouncedBeforeThePlayerMoves() {
            // Tout l'interet du telegraphe : on doit pouvoir lire la menace avant d'agir.
            Arena arena = ArenaSetup.trainingArena(9);

            assertEquals(0, arena.turnsTaken());
            for (Enemy enemy : arena.enemies()) {
                assertTrue(enemy.intention() != null, enemy.label() + " n'a rien annonce");
            }
        }
    }
}
