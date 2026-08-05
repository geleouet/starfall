package com.starfall.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests de la résolution du combat : dégâts, collisions, combos, vagues et fin de partie. */
class CombatTest {

    private static Arena bare(int width, int heroCell) {
        return new Arena(width, heroCell);
    }

    private static Enemy place(Arena arena, int cell, EnemyKind kind, Trait... traits) {
        Enemy enemy = new Enemy(kind, traits);
        arena.grid().place(cell, enemy);
        arena.announceIntentions();
        return enemy;
    }

    @Nested
    @DisplayName("Points de vie")
    class Health {

        @Test
        @DisplayName("Le héros perd un point par coup encaissé et tombe à zéro")
        void theHeroLosesOnePointPerBlow() {
            Arena arena = bare(9, 4);
            place(arena, 5, EnemyKind.SABREUR);

            int expected = Hero.MAX_HEALTH;
            while (expected > 0) {
                arena.step(arena.hero().facing().opposite());
                expected--;
                assertEquals(expected, arena.hero().health());
            }
            assertTrue(arena.isDefeat());
        }

        @Test
        @DisplayName("Une partie perdue n'accepte plus d'action")
        void aLostGameAcceptsNothingMore() {
            Arena arena = bare(9, 4);
            place(arena, 5, EnemyKind.SABREUR, Trait.RAPIDE);

            for (int i = 0; i < 10; i++) {
                arena.step(arena.hero().facing().opposite());
            }

            assertTrue(arena.isDefeat());

            // TOUTES les portes, pas seulement celle du deplacement : seul step() etait garde, et
            // un echange de place faisait repasser le temps et deplacait un heros mort.
            int turns = arena.turnsTaken();
            int cell = arena.heroCell();
            assertEquals(ActionResult.BLOCKED, arena.step(Direction.LEFT), "deplacement");
            assertEquals(ActionResult.BLOCKED, arena.swapWithTarget(), "echange de place");
            assertEquals(ActionResult.BLOCKED, arena.clickOn(0), "clic");
            assertEquals(ActionResult.BLOCKED, arena.unleash(), "execution de tuile");
            assertEquals(ActionResult.BLOCKED, arena.queueTile(Tile.STRIKE), "pose de tuile");
            assertEquals(ActionResult.BLOCKED, arena.unqueueAt(0), "reprise de tuile");

            assertEquals(turns, arena.turnsTaken(), "le temps ne doit plus passer");
            assertEquals(cell, arena.heroCell(), "et le heros mort ne doit plus bouger");
        }

        @Test
        @DisplayName("Les points de vie suivent la lenteur de l'archétype")
        void healthFollowsSlowness() {
            // Ce qui est difficile a tuer doit etre facile a eviter, sans quoi il n'y a pas de
            // decision a prendre.
            assertTrue(EnemyKind.COLOSSE.health() > EnemyKind.SABREUR.health());
            assertFalse(EnemyKind.COLOSSE.actsThisPhase(1));
            assertTrue(EnemyKind.SABREUR.actsThisPhase(1));
        }
    }

    @Nested
    @DisplayName("Collisions")
    class Collisions {

        @Test
        @DisplayName("Une poussée qui aboutit déplace sans blesser")
        void aPushThatLandsOnlyMoves() {
            Arena arena = bare(9, 4);
            Enemy victim = place(arena, 5, EnemyKind.COLOSSE);

            arena.queueTile(Tile.PUSH);
            assertEquals(ActionResult.PUSHED, arena.unleash());

            assertEquals(victim.maxHealth(), victim.health(), "aucun degat sans collision");
            assertEquals(6, arena.grid().indexOf(victim));
        }

        @Test
        @DisplayName("Une poussée sur un autre occupant blesse les deux")
        void aPushIntoSomeoneHurtsBoth() {
            Arena arena = bare(9, 4);
            Enemy pushed = place(arena, 5, EnemyKind.COLOSSE);
            Enemy wall = place(arena, 6, EnemyKind.COLOSSE);

            arena.queueTile(Tile.PUSH);
            assertEquals(ActionResult.COLLIDED, arena.unleash());

            assertEquals(pushed.maxHealth() - Arena.COLLISION_DAMAGE, pushed.health());
            assertEquals(wall.maxHealth() - Arena.COLLISION_DAMAGE, wall.health());
            assertEquals(5, arena.grid().indexOf(pushed), "personne n'a bouge");
        }

        @Test
        @DisplayName("Une poussée peut tuer, et la mort compte comme n'importe quelle autre")
        void aPushCanKill() {
            Arena arena = bare(9, 4);
            // Deux archers : le seul archetype a UN point de vie depuis l'axe des degats, donc
            // le seul que la collision - qui retire un point - puisse tuer.
            place(arena, 5, EnemyKind.ARCHER);
            place(arena, 6, EnemyKind.ARCHER);

            arena.queueTile(Tile.PUSH);
            arena.unleash();

            assertTrue(arena.grid().isFree(5), "le pousse est mort");
            assertTrue(arena.grid().isFree(6), "et celui qu'il a percute aussi");
            assertEquals(2, arena.lastCombo(), "deux morts d'un seul geste");
        }
    }

    @Nested
    @DisplayName("Combos")
    class Combos {

        @Test
        @DisplayName("Une seule mort n'est pas un combo")
        void oneKillIsNotACombo() {
            Arena arena = bare(9, 4);
            // A deux cases, et a l'estoc : depuis l'axe des degats, une frappe n'abat plus rien
            // d'un coup. Trois degats contre les deux points du sabreur, la mort est certaine.
            place(arena, 7, EnemyKind.SABREUR);

            arena.queueTile(Tile.THRUST);
            assertEquals(ActionResult.STRUCK, arena.unleash());
            assertEquals(1, arena.lastCombo());
        }

        @Test
        @DisplayName("Une explosion en chaîne compte pour un seul combo")
        void anExplosiveChainIsOneCombo() {
            Arena arena = bare(15, 0);
            arena.grid().place(5, new Enemy(EnemyKind.SABREUR, Trait.EXPLOSIF));
            arena.grid().place(6, new Enemy(EnemyKind.SABREUR, Trait.EXPLOSIF));
            arena.grid().place(7, new Enemy(EnemyKind.SABREUR));
            arena.announceIntentions();

            arena.kill(5);

            assertTrue(arena.enemies().isEmpty(), "la chaine a tout emporte");
        }

        @Test
        @DisplayName("Le meilleur combo de la partie est retenu")
        void theBestComboIsKept() {
            Arena arena = bare(9, 4);
            place(arena, 5, EnemyKind.ARCHER);
            place(arena, 6, EnemyKind.ARCHER);

            arena.queueTile(Tile.PUSH);
            arena.unleash();
            assertEquals(2, arena.bestCombo());

            // Une action suivante sans mort ne doit pas effacer le record.
            arena.step(Direction.LEFT);
            assertEquals(0, arena.lastCombo());
            assertEquals(2, arena.bestCombo());
        }
    }

    @Nested
    @DisplayName("Vagues")
    class Waves {

        @Test
        @DisplayName("Le terrain vidé fait venir la vague suivante")
        void clearingTheBoardBringsTheNextWave() {
            Arena arena = ArenaSetup.trainingArena(11);
            assertEquals(1, arena.wave());
            int firstWaveSize = arena.enemies().size();

            clearBoard(arena);

            assertEquals(2, arena.wave());
            assertTrue(arena.enemies().size() >= firstWaveSize,
                    "une vague ne doit pas etre plus facile que la precedente");
        }

        @Test
        @DisplayName("Passer une vague rend des points de vie")
        void clearingAWaveGivesSomeRespite() {
            Arena arena = ArenaSetup.trainingArena(11);
            // On encaisse d'abord, sinon le repit ne se verrait pas.
            while (arena.hero().health() > 2) {
                arena.step(arena.hero().facing().opposite());
            }
            int before = arena.hero().health();

            clearBoard(arena);

            // Sans Math.min, et le retrait est le fond de la correction. L'attendu recopiait la
            // formule de heal() - « min(MAX_HEALTH, sante + soin) » - ce qui donnait a
            // l'assertion l'air d'eprouver le plafond. Elle ne le pouvait pas : la boucle
            // ci-dessus descend le heros a deux points quand le maximum en vaut huit, si bien
            // que le min ne serrait JAMAIS. Le titre promettait « sans depasser le maximum » ;
            // il ne le promet plus, et le plafond a son propre test juste en dessous, celui-la
            // sur un etat ou il mord.
            assertEquals(before + Arena.WAVE_RESPITE, arena.hero().health());
        }

        @Test
        @DisplayName("Le répit ne dépasse jamais le maximum")
        void theRespiteNeverOverflows() {
            Arena arena = ArenaSetup.trainingArena(11);
            assertEquals(Hero.MAX_HEALTH, arena.hero().health());

            clearBoard(arena);

            assertEquals(Hero.MAX_HEALTH, arena.hero().health());
        }

        @Test
        @DisplayName("La dernière vague vidée donne la victoire")
        void clearingTheLastWaveWins() {
            Arena arena = ArenaSetup.trainingArena(11);

            for (int wave = 1; wave <= arena.waveCount(); wave++) {
                assertEquals(wave, arena.wave());
                clearBoard(arena);
            }

            assertTrue(arena.isVictory());
            assertEquals(ActionResult.BLOCKED, arena.step(Direction.LEFT),
                    "une partie gagnee n'accepte plus d'action");
        }

        @Test
        @DisplayName("Une vague arrive entière, à toutes les largeurs de grille")
        void aWaveAlwaysArrivesComplete() {
            // Abandonner en silence les ennemis qui ne trouvaient pas de case signifiait qu'une
            // grille de 5 ne rencontrait jamais ni lancier ni colosse explosif : la mecanique
            // vedette du jalon y etait injouable, et la grille la plus etroite etait la plus facile.
            for (int width = Grid.MIN_WIDTH; width <= Grid.MAX_WIDTH; width++) {
                Arena arena = ArenaSetup.trainingArena(width);
                java.util.Set<EnemyKind> seen = new java.util.HashSet<>();

                for (int wave = 1; wave <= arena.waveCount(); wave++) {
                    // On compare a l'effectif declare de la vague, et non a une formule. La version
                    // precedente attendait « N+1 ennemis a la vague N », ce qui etait vrai par
                    // coincidence sur les trois premieres et se lisait comme une regle : la vague
                    // du souverain n'en compte que deux, puisqu'elle fabrique ses renforts.
                    assertTrue(arena.enemies().size() >= WaveTable.size(wave),
                            "vague " + wave + " amputee sur une grille de " + width
                                    + " : " + arena.enemies().size() + " ennemis pour "
                                    + WaveTable.size(wave) + " attendus");
                    for (Enemy enemy : arena.enemies()) {
                        seen.add(enemy.kind());
                    }
                    clearBoard(arena);
                }

                assertTrue(seen.containsAll(java.util.List.of(EnemyKind.values())),
                        "archetypes jamais rencontres sur une grille de " + width + " : "
                                + java.util.Arrays.stream(EnemyKind.values())
                                        .filter(k -> !seen.contains(k)).toList());
            }
        }

        @Test
        @DisplayName("Une vague neuve arrive des deux côtés et jamais au contact")
        void aFreshWaveArrivesOnBothSidesAndNeverAdjacent() {
            for (int width = Grid.MIN_WIDTH; width <= Grid.MAX_WIDTH; width++) {
                Arena arena = ArenaSetup.trainingArena(width);
                int hero = arena.heroCell();

                // Le contact reste un dernier recours, jamais le cas nominal : sur une grille qui
                // a la place, personne ne doit apparaitre colle au heros.
                if (width >= 7) {
                    assertTrue(arena.grid().isFree(hero - 1) || !arena.grid().contains(hero - 1),
                            "un ennemi au contact des le depart, grille de " + width);
                    assertTrue(arena.grid().isFree(hero + 1) || !arena.grid().contains(hero + 1),
                            "un ennemi au contact des le depart, grille de " + width);
                }
                assertTrue(arena.grid().firstOccupied(hero, Direction.LEFT) >= 0
                                || arena.grid().firstOccupied(hero, Direction.RIGHT) >= 0,
                        "vague vide sur une grille de " + width);
            }
        }

        @Test
        @DisplayName("Chaque ennemi d'une vague neuve a déjà annoncé son intention")
        void everyFreshEnemyHasAlreadyAnnounced() {
            Arena arena = ArenaSetup.trainingArena(11);
            clearBoard(arena);

            for (Enemy enemy : arena.enemies()) {
                assertTrue(enemy.intention() != null, enemy.label() + " n'a rien annonce");
            }
        }

        /** Tue tout ce qui est sur le plateau, sans passer par le jeu. */
        private void clearBoard(Arena arena) {
            for (int cell : arena.grid().occupiedCells()) {
                if (arena.grid().occupantAt(cell) instanceof Enemy) {
                    arena.kill(cell);
                }
            }
            // La vague suivante n'arrive qu'au terme d'une action du joueur.
            arena.step(arena.hero().facing().opposite());
        }
    }

    @Nested
    @DisplayName("Invariants sous jeu aléatoire")
    class Invariants {

        @Test
        @DisplayName("Les points de vie restent dans leurs bornes, et la partie se termine")
        void healthStaysInBoundsAndTheGameEnds() {
            int ended = 0;
            int longest = 0;

            for (int seed = 0; seed < 200; seed++) {
                Random random = new Random(seed);
                Arena arena = ArenaSetup.trainingArena(
                        Grid.MIN_WIDTH + random.nextInt(Grid.MAX_WIDTH - Grid.MIN_WIDTH + 1));

                int step = 0;
                for (; step < 300 && !arena.isOver(); step++) {
                    switch (random.nextInt(4)) {
                        case 0 -> arena.step(random.nextBoolean() ? Direction.LEFT : Direction.RIGHT);
                        case 1 -> arena.swapWithTarget();
                        case 2 -> arena.queueTile(
                                arena.rack().tiles().get(random.nextInt(arena.rack().tiles().size())));
                        default -> arena.unleash();
                    }

                    assertTrue(arena.hero().health() >= 0 && arena.hero().health() <= Hero.MAX_HEALTH,
                            "graine " + seed + " : sante du heros " + arena.hero().health());
                    for (Enemy enemy : arena.enemies()) {
                        assertTrue(enemy.health() > 0 && enemy.health() <= enemy.maxHealth(),
                                "graine " + seed + " : " + enemy.label() + " a " + enemy.health()
                                        + " points de vie");
                    }
                    assertTrue(arena.wave() >= 1 && arena.wave() <= arena.waveCount(),
                            "graine " + seed + " : vague " + arena.wave());
                }

                if (arena.isOver()) {
                    ended++;
                }
                longest = Math.max(longest, step);
            }

            // La seconde moitie du titre, qui n'etait affirmee nulle part. Le test bornait la sante
            // et la vague, et s'arretait la : une partie qui aurait tourne en rond indefiniment
            // l'aurait laisse vert, puisque la boucle sort d'elle-meme au bout de 300 pas. Mesure :
            // les 200 se terminent, et la plus longue prend 107 pas - une marge de pres de trois.
            // Le maximum est asserte lui aussi, parce qu'un budget qu'on frole n'est plus un
            // budget : le jour ou une partie s'approchera de 300, c'est ici qu'on le saura, et non
            // le jour ou une graine le depassera en silence.
            assertEquals(200, ended,
                    "seules " + ended + " parties sur 200 se terminent en 300 pas : le titre de ce"
                            + " test promet qu'elles se terminent toutes");
            assertTrue(longest < 200,
                    "la partie la plus longue prend " + longest + " pas sur un budget de 300 : la"
                            + " marge s'est refermee, et ce test finira par mentir");
        }
    }
}
