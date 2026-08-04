package com.starfall.game;

/**
 * Les deux seules directions d'une grille linéaire.
 *
 * <p>La grille du jeu est une ligne unique : tout — déplacement, orientation, portée d'attaque,
 * poussée — se ramène à un pas de {@code -1} ou {@code +1}. C'est ce qui rend une position tactique
 * lisible d'un coup d'œil, et l'énumération existe pour que ce soit impossible d'écrire un {@code
 * +1} au lieu d'un {@code -1} sans que ça se voie.
 */
public enum Direction {

    LEFT(-1, "gauche"),
    RIGHT(1, "droite");

    private final int step;
    private final String label;

    Direction(int step, String label) {
        this.step = step;
        this.label = label;
    }

    /** Déplacement d'un pas sur l'axe des cases : -1 ou +1. */
    public int step() {
        return step;
    }

    /** Nom affichable, en français comme tout ce que voit le joueur. */
    public String label() {
        return label;
    }

    /** Direction à prendre pour aller de {@code from} vers {@code to}, ou {@code null} si égales. */
    public static Direction towards(int from, int to) {
        if (to == from) {
            return null;
        }
        return to < from ? LEFT : RIGHT;
    }
}
