package com.starfall.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests de la grille linéaire. Java pur, aucun contexte GL. */
class GridTest {

    /** Occupant inerte, juste de quoi remplir une case. */
    private record Pawn(String label, long id) implements Occupant {
        // Le numero est un COMPOSANT, et non un « return Identities.next() » dans
        // l'accesseur : ce dernier rendrait un numero neuf a chaque lecture, soit
        // l'exact contraire de ce qu'un numero d'identite promet.
        Pawn(String label) {
            this(label, Identities.next());
        }

        @Override
        public String spriteName() {
            return "enemy/melee";
        }
    }

    private static Occupant pawn(String name) {
        return new Pawn(name);
    }

    @ParameterizedTest(name = "largeur {0}")
    @ValueSource(ints = {5, 6, 9, 14, 15})
    @DisplayName("Toute largeur entre 5 et 15 est acceptée")
    void everyLegalWidthIsAccepted(int width) {
        assertEquals(width, new Grid(width).width());
    }

    @ParameterizedTest(name = "largeur {0}")
    @ValueSource(ints = {4, 16, 0, -1, 100})
    @DisplayName("Une largeur hors bornes est refusée")
    void illegalWidthsAreRejected(int width) {
        assertThrows(IllegalArgumentException.class, () -> new Grid(width));
    }

    @Test
    @DisplayName("Une case neuve est libre et vide")
    void aFreshGridIsEmpty() {
        Grid grid = new Grid(7);

        for (int i = 0; i < 7; i++) {
            assertTrue(grid.isFree(i));
            assertNull(grid.occupantAt(i));
        }
        assertEquals(List.of(), grid.occupiedCells());
    }

    @Test
    @DisplayName("Hors grille n'est pas « libre » : c'est un mur")
    void outsideTheGridIsNotFree() {
        Grid grid = new Grid(5);

        // La distinction compte : un deplacement doit etre bloque par un bord aussi surement que
        // par un occupant, et confondre les deux ferait sortir le heros de la grille.
        assertFalse(grid.isFree(-1));
        assertFalse(grid.isFree(5));
        assertFalse(grid.contains(-1));
        assertFalse(grid.contains(5));
        assertNull(grid.occupantAt(-1));
        assertNull(grid.occupantAt(5));
    }

    @Test
    @DisplayName("Poser puis retrouver un occupant")
    void placeThenFind() {
        Grid grid = new Grid(9);
        Occupant a = pawn("a");

        grid.place(3, a);

        assertSame(a, grid.occupantAt(3));
        assertEquals(3, grid.indexOf(a));
        assertFalse(grid.isFree(3));
        assertEquals(List.of(3), grid.occupiedCells());
    }

    @Test
    @DisplayName("Deux occupants ne peuvent pas partager une case")
    void aCellHoldsAtMostOneOccupant() {
        Grid grid = new Grid(9);
        grid.place(3, pawn("a"));

        assertThrows(IllegalArgumentException.class, () -> grid.place(3, pawn("b")));
    }

    @Test
    @DisplayName("Un même occupant ne peut pas être posé deux fois")
    void anOccupantCannotBeInTwoPlaces() {
        Grid grid = new Grid(9);
        Occupant a = pawn("a");
        grid.place(3, a);

        assertThrows(IllegalArgumentException.class, () -> grid.place(5, a));
    }

    @Test
    @DisplayName("Poser hors grille est refusé")
    void placingOutsideIsRejected() {
        Grid grid = new Grid(5);

        assertThrows(IllegalArgumentException.class, () -> grid.place(-1, pawn("a")));
        assertThrows(IllegalArgumentException.class, () -> grid.place(5, pawn("a")));
    }

    @Test
    @DisplayName("Déplacer vers une case libre")
    void moveToAFreeCell() {
        Grid grid = new Grid(9);
        Occupant a = pawn("a");
        grid.place(3, a);

        grid.move(3, 4);

        assertTrue(grid.isFree(3));
        assertSame(a, grid.occupantAt(4));
    }

    @Test
    @DisplayName("Déplacer vers une case prise ou hors grille est refusé")
    void moveToABlockedCellIsRejected() {
        Grid grid = new Grid(5);
        grid.place(2, pawn("a"));
        grid.place(3, pawn("b"));

        assertThrows(IllegalArgumentException.class, () -> grid.move(2, 3));
        assertThrows(IllegalArgumentException.class, () -> grid.move(2, -1));
        assertThrows(IllegalArgumentException.class, () -> grid.move(2, 5));
        assertThrows(IllegalArgumentException.class, () -> grid.move(0, 1), "case de depart vide");
    }

    @Test
    @DisplayName("L'échange marche entre deux occupants comme avec une case vide")
    void swapWorksBothWays() {
        Grid grid = new Grid(9);
        Occupant a = pawn("a");
        Occupant b = pawn("b");
        grid.place(1, a);
        grid.place(6, b);

        grid.swap(1, 6);
        assertSame(b, grid.occupantAt(1));
        assertSame(a, grid.occupantAt(6));

        grid.swap(6, 8); // 8 est vide
        assertTrue(grid.isFree(6));
        assertSame(a, grid.occupantAt(8));
    }

    @Test
    @DisplayName("Un échange hors grille est refusé")
    void swapOutsideIsRejected() {
        Grid grid = new Grid(5);
        grid.place(0, pawn("a"));

        assertThrows(IllegalArgumentException.class, () -> grid.swap(0, 5));
        assertThrows(IllegalArgumentException.class, () -> grid.swap(-1, 0));
    }

    @Test
    @DisplayName("La visée trouve le premier occupant, pas le plus lointain")
    void aimingFindsTheNearestOccupant() {
        Grid grid = new Grid(9);
        grid.place(2, pawn("proche"));
        grid.place(6, pawn("loin"));

        assertEquals(6, grid.firstOccupied(4, Direction.RIGHT));
        assertEquals(2, grid.firstOccupied(4, Direction.LEFT));
    }

    @Test
    @DisplayName("La visée ne compte pas la case de départ")
    void aimingSkipsTheStartingCell() {
        Grid grid = new Grid(9);
        grid.place(4, pawn("moi"));
        grid.place(7, pawn("cible"));

        assertEquals(7, grid.firstOccupied(4, Direction.RIGHT));
    }

    @Test
    @DisplayName("La visée dans le vide renvoie -1")
    void aimingIntoNothingReturnsMinusOne() {
        Grid grid = new Grid(9);
        grid.place(4, pawn("moi"));

        assertEquals(-1, grid.firstOccupied(4, Direction.RIGHT));
        assertEquals(-1, grid.firstOccupied(4, Direction.LEFT));
        assertEquals(-1, grid.firstOccupied(0, Direction.LEFT), "depuis le bord");
        assertEquals(-1, grid.firstOccupied(8, Direction.RIGHT), "depuis le bord");
    }

    @Test
    @DisplayName("Vider une case rend ce qui s'y trouvait")
    void clearingReturnsWhatWasThere() {
        Grid grid = new Grid(5);
        Occupant a = pawn("a");
        grid.place(2, a);

        assertSame(a, grid.clear(2));
        assertTrue(grid.isFree(2));
        assertNull(grid.clear(2));
        assertNull(grid.clear(99));
    }

    @Test
    @DisplayName("Les cases occupées sont rendues de gauche à droite")
    void occupiedCellsAreOrdered() {
        Grid grid = new Grid(9);
        grid.place(7, pawn("c"));
        grid.place(1, pawn("a"));
        grid.place(4, pawn("b"));

        assertEquals(List.of(1, 4, 7), grid.occupiedCells());
    }
}
