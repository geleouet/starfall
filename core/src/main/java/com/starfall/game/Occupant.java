package com.starfall.game;

/**
 * Ce qui peut occuper une case de la grille.
 *
 * <p>Volontairement minimal à ce stade : la grille n'a pas à savoir ce qu'est un héros ou un
 * ennemi, seulement qu'une case est prise et par quoi la dessiner. Les comportements arrivent avec
 * les ennemis (M6).
 */
public interface Occupant {

    /** Nom du sprite dans l'atlas, par exemple {@code hero/idle}. */
    String spriteName();

    /** Nom affichable, en français. */
    String label();

    /**
     * Numéro unique et stable, pour toute la vie de cet occupant.
     *
     * <p>Le modèle ne s'en sert jamais. Il existe pour que la vue puisse <b>suivre une figure</b>
     * d'un instantané au suivant et l'animer entre les deux : voir {@link Identities}.
     */
    long id();
}
