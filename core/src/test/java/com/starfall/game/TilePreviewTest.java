package com.starfall.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Le test central de M8, et le pendant exact de {@code TelegraphTest}.
 *
 * <p>Le télégraphe promet que <b>ce que l'ennemi annonce est ce qu'il joue</b>. Le préavis promet la
 * même chose de l'autre côté du plateau : <b>ce que l'interface annonce de ma tuile est ce que ma
 * tuile fera</b>. Les deux promesses ont la même valeur — un jeu tactique se joue entièrement sur ce
 * que le joueur croit savoir de la position — et méritent donc la même sévérité.
 *
 * <p>La méthode est celle qui a fini par marcher : on ne vérifie pas des cas construits à la main,
 * on joue des milliers d'actions au hasard et, avant chacune, on relève ce que l'interface aurait
 * affiché. Un affichage de portée qui ment est pire que pas d'affichage du tout : il fait prendre
 * des décisions fausses avec confiance.
 */
class TilePreviewTest {

    private static final int SEEDS = 400;
    private static final int ACTIONS_PER_SEED = 60;

    private static Arena randomArena(Random random) {
        return ArenaSetup.trainingArena(
                Grid.MIN_WIDTH + random.nextInt(Grid.MAX_WIDTH - Grid.MIN_WIDTH + 1));
    }

    /**
     * L'invariant du jalon. Le résultat annoncé est le résultat joué — à ceci près que le jeu a le
     * droit de le <em>promouvoir</em> quand une frappe annoncée tue, enchaîne ou termine la partie.
     * Ce ne sont pas des démentis : ce sont des conséquences du succès annoncé, et la liste de ce
     * qui compte comme telle vit dans {@link TilePreview#isConsequence}, pas ici — un test qui
     * recopierait le code ne contrôlerait rien.
     */
    @Test
    @DisplayName("Le résultat annoncé est le résultat joué, ou l'une de ses conséquences")
    void theAnnouncedOutcomeIsTheOutcomePlayed() {
        List<String> failures = new ArrayList<>();

        for (int seed = 0; seed < SEEDS && failures.size() <= 5; seed++) {
            Random random = new Random(seed);
            Arena arena = randomArena(random);

            for (int action = 0; action < ACTIONS_PER_SEED && !arena.isOver(); action++) {
                if (arena.queue().isEmpty() || random.nextInt(3) > 0) {
                    List<Tile> rack = arena.rack().tiles();
                    arena.queueTile(rack.get(random.nextInt(rack.size())));
                    continue;
                }

                TilePreview preview = arena.previewTop();
                boolean single = arena.queue().size() == 1;
                ActionResult result = arena.unleash();

                // Le préavis ne décrit que la PREMIÈRE tuile de la salve, et il ne peut pas faire
                // mieux : ce que la deuxième trouvera devant elle dépend de ce que la première aura
                // fait. On ne confronte donc l'annonce au résultat que lorsqu'une seule tuile part.
                if (!single || result == preview.outcome()) {
                    continue;
                }
                if (!TilePreview.isConsequence(result)) {
                    failures.add("graine " + seed + " : " + preview.tile()
                            + " annoncait " + preview.outcome() + ", a produit " + result);
                } else if (!preview.connects()) {
                    // Une tuile annoncée comme ratée ne peut pas tuer : si elle le fait, c'est que
                    // l'annonce et l'exécution ne parlent pas de la même chose.
                    failures.add("graine " + seed + " : " + preview.tile()
                            + " annoncait " + preview.outcome() + " et a tout de meme produit "
                            + result);
                }
            }
        }

        assertTrue(failures.isEmpty(), "le preavis ment :\n  " + String.join("\n  ", failures));
    }

    /**
     * Le résultat ne suffit pas : encore faut-il que la case annoncée soit celle qui bouge.
     *
     * <p>Un préavis qui dirait « frappe case 7 » en frappant la 6 renverrait le même
     * {@code ActionResult}. C'est exactement le défaut qu'un joueur ne comprendrait pas — la tuile a
     * l'air de marcher, mais pas sur ce qu'il visait.
     *
     * <p>L'effet est mesuré sur {@code applyTo} et non sur {@code executeTop}, et ce n'est pas un
     * raccourci : {@code executeTop} consomme un tour, donc les ennemis rejouent aussitôt et
     * l'ennemi qu'on vient de pousser a déjà bougé quand on relève sa case. La première version de
     * ce test mesurait cela et accusait le préavis d'une erreur qui n'était pas la sienne. Le
     * préavis promet l'<b>effet direct</b> de la tuile ; c'est donc l'effet direct qu'il faut
     * regarder.
     */
    @Test
    @DisplayName("La case annoncée est celle qui subit l'effet")
    void theAnnouncedCellIsTheCellAffected() {
        List<String> failures = new ArrayList<>();

        for (int seed = 0; seed < SEEDS && failures.size() <= 5; seed++) {
            Random random = new Random(seed);
            Arena arena = randomArena(random);

            for (int action = 0; action < ACTIONS_PER_SEED && !arena.isOver(); action++) {
                Tile tile = Tile.values()[random.nextInt(Tile.values().length)];

                TilePreview preview = tile.preview(arena);
                Occupant[] before = snapshot(arena);
                int aimHealth = healthAt(arena, preview.aim());
                Direction facingBefore = arena.hero().facing();

                tile.applyTo(arena);
                failures.addAll(verify(seed, arena, preview, before, aimHealth, facingBefore));

                // On laisse la partie avancer entre deux mesures, pour que les positions testées
                // ne soient pas toujours celles du début de vague.
                arena.step(random.nextBoolean() ? Direction.LEFT : Direction.RIGHT);
            }
        }

        assertTrue(failures.isEmpty(), "le preavis vise a cote :\n  " + String.join("\n  ", failures));
    }

    private static List<String> verify(int seed, Arena arena, TilePreview preview,
                                       Occupant[] before, int aimHealth, Direction facingBefore) {
        List<String> failures = new ArrayList<>();
        String where = "graine " + seed + " : " + preview.tile() + " " + preview.kind();

        switch (preview.kind()) {
            case HIT -> {
                Occupant victim = before[preview.aim()];
                boolean hurt = arena.grid().indexOf(victim) < 0
                        || (victim instanceof Enemy enemy && enemy.health() < aimHealth);
                if (!hurt) {
                    failures.add(where + " : la case " + (preview.aim() + 1) + " n'a rien subi");
                }
            }
            case MOVE -> {
                if (arena.heroCell() != preview.landing()) {
                    failures.add(where + " : arrivee annoncee en " + (preview.landing() + 1)
                            + ", heros en " + (arena.heroCell() + 1));
                }
            }
            case PUSH -> {
                Occupant pushed = before[preview.aim()];
                int landed = arena.grid().indexOf(pushed);
                if (preview.outcome() == ActionResult.PUSHED && landed != preview.landing()) {
                    failures.add(where + " : arrivee annoncee en " + (preview.landing() + 1)
                            + ", pousse en " + (landed + 1));
                }
                // Une poussée qui butte ne déplace personne : le poussé reste sur place, ou meurt.
                if (preview.outcome() == ActionResult.COLLIDED && landed >= 0
                        && landed != preview.aim()) {
                    failures.add(where + " : choc annonce, mais le pousse a bouge en " + (landed + 1));
                }
            }
            case TURN -> {
                if (arena.hero().facing() != preview.direction()) {
                    failures.add(where + " : regard annonce vers " + preview.direction()
                            + ", obtenu " + arena.hero().facing());
                }
            }
            // Une tuile annoncée sans effet ne doit rien changer du tout : ni le plateau, ni
            // l'orientation. Elle est dépensée, et c'est tout ce qu'elle fait.
            case NONE -> {
                if (!sameOccupants(before, snapshot(arena))) {
                    failures.add(where + " : annoncee sans effet, mais le plateau a change");
                }
                if (arena.hero().facing() != facingBefore) {
                    failures.add(where + " : annoncee sans effet, mais le heros s'est retourne");
                }
            }
        }
        return failures;
    }

    /**
     * Un préavis se calcule à chaque image ; s'il faisait avancer la partie, l'interface la
     * jouerait toute seule. Le risque n'est pas théorique : le préavis est écrit avec les mêmes
     * primitives que l'exécution, à un {@code grid.move} près.
     */
    @Test
    @DisplayName("Calculer un préavis ne change rien à la partie")
    void computingAPreviewChangesNothing() {
        for (int seed = 0; seed < 60; seed++) {
            Random random = new Random(seed);
            Arena arena = randomArena(random);
            for (int i = 0; i < 5; i++) {
                List<Tile> rack = arena.rack().tiles();
                arena.queueTile(rack.get(random.nextInt(rack.size())));
            }

            String reference = signature(arena);
            for (int repeat = 0; repeat < 50; repeat++) {
                arena.previewTop();
                for (Tile tile : Tile.values()) {
                    arena.preview(tile);
                }
            }
            assertEquals(reference, signature(arena), "graine " + seed);
        }
    }

    /**
     * La portée statique — celle que le râtelier montre au survol — et la portée résolue — celle du
     * sommet de la file — sont deux représentations de la même règle. Elles doivent se recouper,
     * sans quoi l'infobulle promettrait une portée que la tuile n'utilise pas.
     */
    @Test
    @DisplayName("La portée affichée au râtelier est celle que la tuile vise vraiment")
    void theAdvertisedReachIsTheReachActuallyAimedAt() {
        for (int seed = 0; seed < 120; seed++) {
            Random random = new Random(seed);
            Arena arena = randomArena(random);
            for (int turn = 0; turn < 12 && !arena.isOver(); turn++) {
                for (Tile tile : Tile.values()) {
                    int[] reach = tile.reach();
                    if (reach.length == 0) {
                        continue; // portée variable ou tuile qui ne vise rien
                    }
                    TilePreview preview = arena.preview(tile);
                    int aimed = preview.kind() == TilePreview.Kind.MOVE
                            ? preview.landing() : preview.aim();
                    if (aimed < 0) {
                        continue;
                    }
                    int offset = (aimed - arena.heroCell()) / arena.hero().facing().step();
                    boolean advertised = false;
                    for (int candidate : reach) {
                        advertised |= candidate == offset;
                    }
                    assertTrue(advertised, "graine " + seed + " : " + tile + " annonce la portee "
                            + java.util.Arrays.toString(reach) + " mais vise le decalage " + offset);
                }
                arena.step(random.nextBoolean() ? Direction.LEFT : Direction.RIGHT);
            }
        }
    }

    /**
     * La partie finie, l'interface ne doit plus rien promettre : aucune touche ne peut plus rien
     * déclencher. C'est le prolongement direct du défaut relevé au jalon précédent, où six portes
     * d'action sur sept étaient gardées et la septième laissait rejouer une partie perdue.
     */
    @Test
    @DisplayName("Une partie finie n'annonce plus rien")
    void afinishedGameAnnouncesNothing() {
        Arena arena = new Arena(9, 4);
        arena.queueTile(Tile.STRIKE);
        assertTrue(arena.previewTop() != null, "une partie en cours doit annoncer sa tuile");

        while (!arena.isOver() && arena.turnsTaken() < 200) {
            arena.damage(arena.heroCell(), 1);
            arena.step(Direction.LEFT);
        }

        assertTrue(arena.isOver(), "la partie doit s'etre terminee");
        assertEquals(null, arena.previewTop(), "une partie finie ne promet plus de coup");
        assertEquals(null, arena.preview(Tile.STRIKE), "ni pour une tuile du ratelier");
    }

    // ------------------------------------------------------------------ outils

    private static Occupant[] snapshot(Arena arena) {
        Occupant[] cells = new Occupant[arena.grid().width()];
        for (int cell = 0; cell < cells.length; cell++) {
            cells[cell] = arena.grid().occupantAt(cell);
        }
        return cells;
    }

    private static boolean sameOccupants(Occupant[] a, Occupant[] b) {
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    private static int healthAt(Arena arena, int cell) {
        return arena.grid().occupantAt(cell) instanceof Enemy enemy ? enemy.health() : -1;
    }

    private static String signature(Arena arena) {
        StringBuilder state = new StringBuilder();
        for (int cell = 0; cell < arena.grid().width(); cell++) {
            Occupant occupant = arena.grid().occupantAt(cell);
            state.append(occupant == null ? '.'
                    : occupant == arena.hero() ? 'H' : occupant.label().charAt(0));
        }
        return state.append('|').append(arena.turnsTaken())
                .append('|').append(arena.hero().health())
                .append('|').append(arena.hero().facing())
                .append('|').append(arena.queue().size()).toString();
    }
}
