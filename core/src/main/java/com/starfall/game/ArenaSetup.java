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
        return trainingArena(gridWidth, 1);
    }

    /**
     * Même chose, mais en démarrant à une vague donnée.
     *
     * <p>La rencontre finale est la quatrième : l'atteindre demande d'en gagner trois. Une mécanique
     * qu'on ne peut ni capturer ni faire relire sans jouer une partie complète est une mécanique
     * qu'on ne relit pas.
     */
    public static Arena trainingArena(int gridWidth, int startWave) {
        Arena arena = new Arena(gridWidth);
        arena.enableWaves();
        arena.startAtWave(startWave);
        // On repose la question à l'arène plutôt que de réutiliser l'argument : c'est elle qui
        // borne, et borner à deux endroits de deux façons différentes est ce qui laissait
        // « vague 0 » passer le garde-fou puis sortir de la table.
        WaveTable.spawn(arena, arena.wave());
        // Les intentions doivent être visibles avant même le premier geste du joueur : c'est tout
        // l'intérêt du télégraphe.
        arena.announceIntentions();
        return arena;
    }
}
