package com.starfall.game;

/**
 * Mise en place d'une vague d'entraînement.
 *
 * <p>Vit ici, et non dans la scène de rendu, pour une raison précise : la disposition doit rester
 * sensée à toutes les largeurs de grille, et c'est vérifiable sans le moindre contexte graphique.
 */
public final class ArenaSetup {

    /**
     * Ordre dans lequel les archétypes sont posés, avec leurs traits.
     *
     * <p>Le sabreur est rapide et l'archer nu : la paire la plus instructive à lire côte à côte,
     * puisque l'un veut qu'on s'éloigne et l'autre qu'on s'approche. Le colosse est explosif, ce qui
     * en fait autant une menace qu'une occasion.
     */
    private static final Enemy[] WAVE = {
            new Enemy(EnemyKind.SABREUR, Trait.RAPIDE),
            new Enemy(EnemyKind.ARCHER),
            new Enemy(EnemyKind.LANCIER),
            new Enemy(EnemyKind.COLOSSE, Trait.EXPLOSIF),
    };

    private ArenaSetup() {
    }

    /**
     * Construit une arène avec le héros au milieu et une vague de part et d'autre.
     *
     * <p>Garantie : il y a toujours au moins un ennemi de chaque côté du héros, quelle que soit la
     * largeur — sans quoi la moitié des mécaniques d'orientation ne serait jamais mise à l'épreuve.
     */
    public static Arena trainingArena(int gridWidth) {
        Arena arena = new Arena(gridWidth);
        int hero = arena.heroCell();

        int[] wanted = {hero + 2, hero - 2, hero + 4, hero - 4, gridWidth - 1, 0};
        int placed = 0;
        for (int cell : wanted) {
            if (placed == WAVE.length) {
                break;
            }
            if (arena.grid().isFree(cell)) {
                arena.grid().place(cell, freshEnemy(placed));
                placed++;
            }
        }

        // Les intentions doivent être visibles avant même le premier geste du joueur : c'est tout
        // l'intérêt du télégraphe.
        arena.announceIntentions();
        return arena;
    }

    /** Un ennemi neuf : le tableau {@code WAVE} ne sert que de patron, jamais d'instance partagée. */
    private static Enemy freshEnemy(int index) {
        Enemy pattern = WAVE[index];
        return new Enemy(pattern.kind(), pattern.traits().toArray(new Trait[0]));
    }
}
