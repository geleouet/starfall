package com.starfall.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests des règles du combat : déplacement, orientation, capacité spéciale. */
class ArenaTest {

    private record Pawn(String label) implements Occupant {
        @Override
        public String spriteName() {
            return "enemy/melee";
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
        @DisplayName("Le milieu d'une grille paire penche à gauche, et c'est stable")
        void theMiddleOfAnEvenGridIsWellDefined() {
            assertEquals(3, new Arena(6).heroCell());
            assertEquals(5, new Arena(10).heroCell());
        }
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
        @DisplayName("Sans cible, la capacité le dit au lieu de ne rien faire")
        void withoutATargetTheAbilitySaysSo() {
            Arena arena = new Arena(9);

            assertEquals(-1, arena.swapTarget());
            assertEquals(ActionResult.NO_TARGET, arena.swapWithTarget());
            assertEquals(4, arena.heroCell());
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
        @DisplayName("Cliquer derrière soi fait d'abord se retourner")
        void clickingBehindTurnsFirst() {
            Arena arena = new Arena(9);

            assertEquals(ActionResult.TURNED, arena.clickOn(0));
            assertEquals(Direction.LEFT, arena.hero().facing());
            assertEquals(4, arena.heroCell());

            assertEquals(ActionResult.MOVED, arena.clickOn(0));
            assertEquals(3, arena.heroCell());
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
        @DisplayName("Clavier et souris mènent au même état")
        void mouseAndKeyboardAgree() {
            // Les deux entrees sont de plein droit : elles doivent produire exactement le meme jeu.
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
        }
    }

    @Nested
    @DisplayName("Résultats d'action")
    class Results {

        @Test
        @DisplayName("Seules les actions qui changent l'état le déclarent")
        void onlyRealActionsChangeState() {
            assertTrue(ActionResult.MOVED.changedState());
            assertTrue(ActionResult.TURNED.changedState());
            assertTrue(ActionResult.SWAPPED.changedState());
            assertTrue(!ActionResult.BLOCKED.changedState());
            assertTrue(!ActionResult.NO_TARGET.changedState());
        }
    }
}
