package com.starfall.game;

/**
 * Mannequin d'entraînement : un occupant strictement inerte.
 *
 * <p>Il n'existe que pour donner une cible à la capacité d'échange tant que les vrais ennemis
 * n'existent pas (M6). Il n'a ni comportement, ni points de vie, ni intention — et il emprunte
 * volontairement le sprite d'ennemi plutôt que d'en avoir un à lui : lui en dessiner un donnerait à
 * croire qu'il fait partie du jeu.
 */
public record TrainingDummy(String label) implements Occupant {

    @Override
    public String spriteName() {
        return "enemy/melee";
    }
}
