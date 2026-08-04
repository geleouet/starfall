package com.starfall.game;

/**
 * Les vagues successives d'une tranche de jeu.
 *
 * <p>Trois vagues, comme le périmètre le prévoit, et une progression qui n'est pas seulement
 * « plus d'ennemis » : chaque vague pose une question que la précédente ne posait pas.
 *
 * <ol>
 *   <li>un sabreur et un archer — apprendre que l'un veut qu'on s'éloigne et l'autre qu'on
 *       s'approche ;</li>
 *   <li>on ajoute le lancier, dont la menace est décalée d'un tour ;</li>
 *   <li>on ajoute le colosse explosif, qui vaut autant comme arme que comme obstacle.</li>
 * </ol>
 */
final class WaveTable {

    /** Composition de chaque vague. Ces instances ne servent que de patrons ; on en fait des copies. */
    private static final Enemy[][] WAVES = {
            {
                    new Enemy(EnemyKind.SABREUR),
                    new Enemy(EnemyKind.ARCHER),
            },
            {
                    new Enemy(EnemyKind.SABREUR, Trait.RAPIDE),
                    new Enemy(EnemyKind.ARCHER),
                    new Enemy(EnemyKind.LANCIER),
            },
            {
                    new Enemy(EnemyKind.SABREUR, Trait.RAPIDE),
                    new Enemy(EnemyKind.ARCHER, Trait.AGRESSIF),
                    new Enemy(EnemyKind.LANCIER, Trait.FONCEUR),
                    new Enemy(EnemyKind.COLOSSE, Trait.EXPLOSIF),
            },
    };

    private WaveTable() {
    }

    static int count() {
        return WAVES.length;
    }

    /**
     * Pose la vague demandée autour du héros.
     *
     * <p>Les ennemis sont répartis <b>de part et d'autre</b>, en alternant les côtés : une vague qui
     * arriverait toute du même côté ne poserait aucune question d'orientation, alors que c'est l'une
     * des mécaniques verrouillées du jeu.
     *
     * <p>Personne n'apparaît au contact : la première distance essayée est de deux cases, pour que
     * le joueur ait toujours au moins un tour pour lire la nouvelle vague avant de la subir.
     */
    static void spawn(Arena arena, int wave) {
        Enemy[] pattern = WAVES[Math.min(wave, WAVES.length) - 1];
        Grid grid = arena.grid();
        int hero = arena.heroCell();
        int width = grid.width();

        int placed = 0;
        for (int distance = 2; distance < width && placed < pattern.length; distance++) {
            for (int side : new int[]{1, -1}) {
                if (placed == pattern.length) {
                    break;
                }
                int cell = hero + side * distance;
                if (grid.isFree(cell)) {
                    Enemy model = pattern[placed];
                    grid.place(cell, new Enemy(model.kind(), model.traits().toArray(new Trait[0])));
                    placed++;
                }
            }
        }
    }
}
