package com.starfall.game;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * La file d'actions : jusqu'à {@value #CAPACITY} tuiles, exécutées <b>dernier entré, premier
 * sorti</b>.
 *
 * <p>C'est le seul endroit du jeu où l'ordre compte autant. Poser trois tuiles puis les exécuter les
 * joue à l'envers de l'ordre de pose — une contrainte qui a l'air arbitraire jusqu'à ce qu'on
 * réalise qu'elle transforme chaque préparation en plan lu à rebours.
 *
 * <p>La file en porte <b>cinq</b> et non trois comme la référence : c'est l'écart mécanique
 * délibéré du projet, et il change beaucoup l'économie des tours — plafond de combo bien plus haut,
 * mais aussi bien plus de tours passés à charger la file en étant vulnérable.
 *
 * <p>Poser et reprendre sont <b>gratuits</b> : ni l'un ni l'autre ne consomme de tour.
 */
public final class ActionQueue {

    public static final int CAPACITY = 5;

    /** Sommet = dernière posée = prochaine exécutée. */
    private final Deque<Tile> stack = new ArrayDeque<>();

    public int size() {
        return stack.size();
    }

    public boolean isEmpty() {
        return stack.isEmpty();
    }

    public boolean isFull() {
        return stack.size() >= CAPACITY;
    }

    /** Tuile qui sera exécutée ensuite, ou {@code null} si la file est vide. */
    public Tile top() {
        return stack.peek();
    }

    /**
     * Contenu de la file, <b>de la plus ancienne à la plus récente</b>.
     *
     * <p>C'est l'ordre d'affichage naturel — on lit la file dans l'ordre où on l'a construite — et
     * donc l'inverse de l'ordre d'exécution. L'interface doit rendre ça évident ; le modèle, lui,
     * se contente d'être explicite sur le sens.
     */
    public List<Tile> fromOldest() {
        List<Tile> tiles = new ArrayList<>(stack);
        java.util.Collections.reverse(tiles);
        return tiles;
    }

    void push(Tile tile) {
        if (isFull()) {
            throw new IllegalStateException("La file porte déjà " + CAPACITY + " tuiles");
        }
        stack.push(tile);
    }

    /** Retire et rend la tuile du sommet. */
    Tile pop() {
        if (stack.isEmpty()) {
            throw new IllegalStateException("File vide");
        }
        return stack.pop();
    }

    /**
     * Retire la tuile à une position donnée, comptée <b>depuis la plus ancienne</b>, comme
     * l'affichage. Rend la tuile retirée, ou {@code null} si la position n'existe pas.
     */
    Tile removeAt(int indexFromOldest) {
        List<Tile> tiles = fromOldest();
        if (indexFromOldest < 0 || indexFromOldest >= tiles.size()) {
            return null;
        }
        Tile removed = tiles.remove(indexFromOldest);
        stack.clear();
        for (Tile tile : tiles) {
            stack.push(tile);
        }
        return removed;
    }

    @Override
    public String toString() {
        return "ActionQueue" + fromOldest() + " (sommet : " + (isEmpty() ? "-" : top().label()) + ")";
    }
}
