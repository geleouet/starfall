package com.starfall.game;

import java.util.ArrayList;
import java.util.List;

/**
 * La grille de combat : une ligne unique de cases, chacune vide ou occupée par un seul {@link
 * Occupant}.
 *
 * <p>La largeur est configurable de {@value #MIN_WIDTH} à {@value #MAX_WIDTH} cases, comme
 * verrouillé au cadrage. Une case ne peut jamais contenir deux occupants : c'est ce qui rend la
 * position lisible, et c'est aussi ce qui donne leur sens aux poussées et aux échanges de place.
 *
 * <p>La grille est la <b>seule</b> détentrice des positions. Un occupant ne connaît pas la sienne :
 * la demander se fait par {@link #indexOf}, ce qui coûte un parcours de quinze cases au plus et
 * supprime toute possibilité de désynchronisation entre deux sources de vérité.
 */
public final class Grid {

    public static final int MIN_WIDTH = 5;
    public static final int MAX_WIDTH = 15;

    private final Occupant[] cells;

    public Grid(int width) {
        if (width < MIN_WIDTH || width > MAX_WIDTH) {
            throw new IllegalArgumentException("La grille fait de " + MIN_WIDTH + " à " + MAX_WIDTH
                    + " cases, reçu " + width);
        }
        this.cells = new Occupant[width];
    }

    public int width() {
        return cells.length;
    }

    /** Vrai si l'indice désigne une case de la grille. */
    public boolean contains(int index) {
        return index >= 0 && index < cells.length;
    }

    /** Occupant d'une case, ou {@code null} si elle est vide ou hors grille. */
    public Occupant occupantAt(int index) {
        return contains(index) ? cells[index] : null;
    }

    /** Vrai si la case existe et est libre. Hors grille n'est pas « libre » : c'est un mur. */
    public boolean isFree(int index) {
        return contains(index) && cells[index] == null;
    }

    /**
     * Pose un occupant sur une case libre.
     *
     * @throws IllegalArgumentException si la case est hors grille ou déjà prise
     */
    public void place(int index, Occupant occupant) {
        if (occupant == null) {
            throw new IllegalArgumentException("Occupant nul : utiliser clear(int) pour vider une case");
        }
        if (!contains(index)) {
            throw new IllegalArgumentException("Case " + index + " hors d'une grille de " + width());
        }
        if (cells[index] != null) {
            throw new IllegalArgumentException("La case " + index + " est déjà occupée par "
                    + cells[index].label());
        }
        if (indexOf(occupant) >= 0) {
            throw new IllegalArgumentException(occupant.label() + " est déjà sur la grille");
        }
        cells[index] = occupant;
    }

    /** Vide une case et renvoie ce qui s'y trouvait, ou {@code null}. */
    public Occupant clear(int index) {
        if (!contains(index)) {
            return null;
        }
        Occupant previous = cells[index];
        cells[index] = null;
        return previous;
    }

    /** Position d'un occupant, ou {@code -1} s'il n'est pas sur la grille. */
    public int indexOf(Occupant occupant) {
        for (int i = 0; i < cells.length; i++) {
            if (cells[i] == occupant) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Déplace un occupant d'une case vers une autre, qui doit être libre.
     *
     * @throws IllegalArgumentException si la destination est hors grille ou occupée
     */
    public void move(int from, int to) {
        Occupant occupant = occupantAt(from);
        if (occupant == null) {
            throw new IllegalArgumentException("Aucun occupant sur la case " + from);
        }
        if (!isFree(to)) {
            throw new IllegalArgumentException("La case " + to + " n'est pas libre");
        }
        cells[from] = null;
        cells[to] = occupant;
    }

    /**
     * Échange le contenu de deux cases. Les deux peuvent être occupées, ou l'une vide : dans tous
     * les cas le résultat est un simple échange, ce qui évite un cas particulier à l'appelant.
     *
     * @throws IllegalArgumentException si une des cases est hors grille
     */
    public void swap(int a, int b) {
        if (!contains(a) || !contains(b)) {
            throw new IllegalArgumentException("Échange hors grille : " + a + " et " + b);
        }
        Occupant temporary = cells[a];
        cells[a] = cells[b];
        cells[b] = temporary;
    }

    /**
     * Première case occupée en partant de {@code from} (exclu) et en avançant dans {@code
     * direction}, ou {@code -1} s'il n'y a personne jusqu'au bord.
     *
     * <p>C'est la primitive de visée du jeu : portée d'attaque, cible d'un échange de place,
     * trajectoire d'un projectile s'y ramènent tous.
     */
    public int firstOccupied(int from, Direction direction) {
        for (int i = from + direction.step(); contains(i); i += direction.step()) {
            if (cells[i] != null) {
                return i;
            }
        }
        return -1;
    }

    /** Cases occupées, de gauche à droite. */
    public List<Integer> occupiedCells() {
        List<Integer> occupied = new ArrayList<>();
        for (int i = 0; i < cells.length; i++) {
            if (cells[i] != null) {
                occupied.add(i);
            }
        }
        return occupied;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("Grid[");
        for (Occupant cell : cells) {
            builder.append(cell == null ? '.' : '#');
        }
        return builder.append(']').toString();
    }
}
