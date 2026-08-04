package com.starfall.game;

/**
 * Mise en place de l'arène d'entraînement de M4.
 *
 * <p>Vit ici, et non dans la scène de rendu, pour une raison précise : la disposition doit rester
 * sensée à toutes les largeurs de grille, et c'est vérifiable sans le moindre contexte graphique.
 * Placée dans la scène, elle n'était couverte par aucun test — et à 5 cases elle ne posait qu'un
 * seul mannequin, du même côté que les autres, ce qui rendait la capacité d'échange impossible à
 * essayer vers la gauche.
 */
public final class ArenaSetup {

    private ArenaSetup() {
    }

    /**
     * Construit une arène avec le héros au milieu et des mannequins de part et d'autre.
     *
     * <p>Garantie : dès que la grille le permet — c'est-à-dire toujours, de 5 à 15 cases — il y a au
     * moins un mannequin de chaque côté du héros, pour que l'échange de place soit essayable dans
     * les deux sens.
     */
    public static Arena trainingArena(int gridWidth) {
        Arena arena = new Arena(gridWidth);
        int hero = arena.heroCell();

        // Un devant, un derrière, puis un au bord opposé s'il reste de la place.
        int[] wanted = {hero + 2, hero - 2, gridWidth - 1, 0};
        int placed = 0;
        for (int cell : wanted) {
            if (placed == 3) {
                break;
            }
            if (arena.grid().isFree(cell)) {
                arena.grid().place(cell, new TrainingDummy("mannequin " + (placed + 1)));
                placed++;
            }
        }
        return arena;
    }
}
