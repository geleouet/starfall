package com.starfall.game;

/**
 * Mise en place d'un combat.
 *
 * <p>Vit ici, et non dans la scène de rendu, pour une raison précise : la disposition doit rester
 * sensée à toutes les largeurs de grille, et c'est vérifiable sans le moindre contexte graphique.
 */
public final class ArenaSetup {

    private ArenaSetup() {
    }

    /**
     * Construit une arène avec le héros au milieu et la première vague autour de lui.
     *
     * <p>Les vagues suivantes s'enchaînent toutes seules : c'est {@link Arena} qui les appelle quand
     * le terrain se vide.
     */
    public static Arena trainingArena(int gridWidth) {
        Arena arena = new Arena(gridWidth);
        arena.enableWaves();
        WaveTable.spawn(arena, 1);
        // Les intentions doivent être visibles avant même le premier geste du joueur : c'est tout
        // l'intérêt du télégraphe.
        arena.announceIntentions();
        return arena;
    }
}
