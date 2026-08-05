package com.starfall.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.starfall.game.ActionQueue;
import com.starfall.game.Arena;
import com.starfall.game.ArenaSetup;
import com.starfall.game.Enemy;
import com.starfall.game.Tile;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Ce que coûte une salve pleine, et si la file peut seulement être remplie.
 *
 * <h2>Ce que ce fichier a cessé d'affirmer</h2>
 *
 * <p>Une première version prétendait établir un fait de <b>game design</b> : « à budget de six
 * tours, la meilleure ligne n'emploie jamais plus de trois tuiles d'un coup ». La review l'a
 * démontée, et le vice était méthodologique et non de réglage.
 *
 * <p>Une recherche par faisceau prouve qu'un chemin <b>existe</b> ; elle ne borne jamais l'optimum.
 * {@code WinnabilityTest} l'assume explicitement — « il prouve qu'une victoire existe, jamais
 * qu'elle n'existe pas ». Le même instrument était ici retourné pour asserter une propriété
 * <em>de</em> l'optimum, ce qu'il ne peut structurellement pas faire : il ne peut dire que « je n'ai
 * pas trouvé mieux avec ce réglage ». Et le réglage décidait : le relecteur a mesuré un plafond de 3
 * à faisceau 400, <b>4</b> à 600, 3 à 800, <b>4</b> de 1000 à 2000, 3 à 5000 — non monotone. Pire, à
 * score strictement égal, deux réglages rendent des salves de tailles différentes : la valeur
 * mesurée n'était pas une propriété du jeu mais l'ordre de découverte du faisceau.
 *
 * <p>Le fil-piège ne se déclenchait pas non plus. En rendant le chargement gratuit — l'économie
 * d'avant M10 — quinze tests devenaient rouges et celui-ci restait vert, parce qu'il n'interrogeait
 * qu'une configuration sur les douze dont il tirait sa conclusion.
 *
 * <h2>Ce qu'il affirme maintenant</h2>
 *
 * <p>Deux faits seulement, tous deux reproductibles et insensibles au réglage :
 *
 * <ul>
 *   <li>le <b>coût exact</b> d'une salve pleine, qui est de l'arithmétique et non une recherche ;</li>
 *   <li>la file <b>peut</b> être remplie dans chaque configuration — une affirmation d'existence,
 *       c'est-à-dire précisément ce qu'un faisceau sait établir.</li>
 * </ul>
 */
class SalvoEconomyTest {

    /**
     * Le coût d'une salve pleine, et il n'est pas celui que j'annonçais.
     *
     * <p>J'avais écrit « six tours, exactement ce que coûte une salve pleine — cinq de chargement et
     * un pour lâcher ». C'est faux dans les deux sens. Poser une tuile <b>Free-Play</b> ne consomme
     * aucun tour, et il n'y a que <b>quatre</b> tuiles non-Free-Play pour cinq emplacements : une
     * file pleine en contient donc <em>forcément</em> au moins une gratuite. Le chargement coûte 3 ou
     * 4 tours, jamais 5.
     *
     * <p>Ce n'est pas un détail : ce chiffre était le budget sur lequel toute la comparaison
     * « charger contre frapper » était bâtie. Une salve pleine est un tiers moins chère que je ne le
     * disais, ce qui affaiblit d'autant l'argument « cinq tours sans frapper ».
     */
    @Test
    @DisplayName("Une salve pleine coûte 4 ou 5 tours, jamais 6")
    void afullSalvoCostsFourOrFiveTurnsNeverSix() {
        List<Tile> free = new ArrayList<>();
        List<Tile> costly = new ArrayList<>();
        for (Tile tile : Tile.values()) {
            (tile.isFreePlay() ? free : costly).add(tile);
        }
        assertEquals(2, free.size(), "le compte de tuiles Free-Play a change : " + free);
        assertTrue(costly.size() < ActionQueue.CAPACITY,
                "il y a desormais assez de tuiles payantes pour remplir la file sans gratuite,"
                        + " donc le cout maximal a change : " + costly);

        // Le pire des cas : on charge d'abord tout ce qui coûte, et la file se complète avec une
        // seule gratuite. C'est le chargement le plus cher qui existe.
        Arena arena = ArenaSetup.trainingArena(9, 1);
        int before = arena.turnsTaken();
        List<Tile> loaded = new ArrayList<>();
        for (Tile tile : costly) {
            arena.queueTile(tile);
            loaded.add(tile);
        }
        arena.queueTile(free.get(0));
        loaded.add(free.get(0));

        assertTrue(arena.queue().isFull(), "la file devait etre pleine, elle contient " + loaded);
        int loading = arena.turnsTaken() - before;
        assertEquals(costly.size(), loading,
                "le chargement le plus cher devrait couter un tour par tuile payante");
        assertEquals(4, loading, "le chargement le plus cher vaut " + loading + " tours");

        // Et le meilleur des cas : les deux gratuites entrent dans la file.
        Arena cheapest = ArenaSetup.trainingArena(9, 1);
        int start = cheapest.turnsTaken();
        for (Tile tile : free) {
            cheapest.queueTile(tile);
        }
        for (int i = 0; i < ActionQueue.CAPACITY - free.size(); i++) {
            cheapest.queueTile(costly.get(i));
        }
        assertTrue(cheapest.queue().isFull(), "la file devait etre pleine");
        assertEquals(3, cheapest.turnsTaken() - start,
                "le chargement le moins cher vaut " + (cheapest.turnsTaken() - start) + " tours");
    }

    // ------------------------------------------------------------------ la file peut être remplie

    private static final int BEAM = 300;

    /**
     * Borne de gestes : <b>reprendre</b> une tuile et poser une <b>Free-Play</b> ne consomment aucun
     * tour, si bien qu'une ligne peut s'allonger indéfiniment. Sans borne, la recherche ne s'arrête
     * pas — c'est arrivé.
     */
    private static final int MAX_MOVES = 20;

    private static String signature(Arena arena) {
        StringBuilder state = new StringBuilder();
        state.append(arena.hero().health()).append('|').append(arena.hero().facing())
                .append('|').append(arena.wave()).append('|').append(arena.turnsTaken())
                .append('|').append(arena.queue().fromOldest());
        for (Enemy enemy : arena.enemies()) {
            state.append(';').append(arena.grid().indexOf(enemy)).append(',').append(enemy.kind())
                    .append(',').append(enemy.health()).append(',').append(enemy.intention());
        }
        return state.toString();
    }

    /** Le plus grand nombre de tuiles qu'on ait su mettre dans la file en restant en vie. */
    private static int deepestQueue(int width, int startWave) {
        return deepestQueue(width, startWave, BEAM);
    }

    private static int deepestQueue(int width, int startWave, int beamWidth) {
        List<List<String>> beam = new ArrayList<>();
        beam.add(new ArrayList<>());
        int deepest = 0;

        // La mémoire est refaite à chaque étage, et c'est un correctif : partagée sur toute la
        // recherche, elle marquait « vu » des états admis puis coupés par le faisceau, qui
        // devenaient définitivement inatteignables. Le relecteur l'a mesuré — à faisceau égal, la
        // mémoire globale faisait perdre un point de vie au champion. Ce n'est pas de la
        // déduplication, c'est de l'oubli.
        for (int move = 0; move < MAX_MOVES && deepest < ActionQueue.CAPACITY; move++) {
            List<List<String>> next = new ArrayList<>();
            Set<String> seen = new LinkedHashSet<>();
            for (List<String> line : beam) {
                Arena arena = Playout.replay(width, startWave, line);
                if (arena.isOver()) {
                    continue;
                }
                for (Move candidate : Move.legal(arena)) {
                    if (candidate.label().equals("salve")) {
                        continue; // on cherche à remplir, pas à vider
                    }
                    List<String> extended = new ArrayList<>(line);
                    extended.add(candidate.label());
                    Arena after = Playout.replay(width, startWave, extended);
                    if (after.isDefeat() || !seen.add(signature(after))) {
                        continue;
                    }
                    deepest = Math.max(deepest, after.queue().size());
                    next.add(extended);
                }
            }
            if (next.isEmpty()) {
                break;
            }
            // On garde les lignes les plus chargées, puis les plus saines : c'est tout l'objectif.
            next.sort((first, second) -> {
                Arena a = Playout.replay(width, startWave, first);
                Arena b = Playout.replay(width, startWave, second);
                int byQueue = Integer.compare(b.queue().size(), a.queue().size());
                return byQueue != 0 ? byQueue
                        : Integer.compare(b.hero().health(), a.hero().health());
            });
            beam = next.size() > beamWidth
                    ? new ArrayList<>(next.subList(0, beamWidth)) : next;
        }
        return deepest;
    }

    /**
     * Et la largeur du faisceau n'est pas posée au bord de sa propre limite.
     *
     * <p>C'est le contrôle que la review a relevé comme manquant, et il a une histoire : ce projet a
     * déjà eu un faisceau réglé à 1,75× son seuil de rupture, si bien que la moindre retouche
     * d'heuristique le faisait virer au rouge pour une raison qui n'était pas « le jeu est devenu
     * impossible ». {@code WinnabilityTest} porte le même contrôle depuis.
     *
     * <p>La marge est donc <b>mesurée et non supposée</b> : un faisceau de moitié remplit encore.
     */
    @Test
    @DisplayName("Un faisceau deux fois plus étroit remplit encore la file")
    void ahalvedBeamStillFillsTheQueue() {
        assertEquals(ActionQueue.CAPACITY, deepestQueue(9, Arena.WAVE_COUNT, BEAM / 2),
                "un faisceau de " + (BEAM / 2) + " ne remplit plus la file sur la rencontre finale :"
                        + " le garde-fou est pose trop pres de sa limite");
    }

    /**
     * La file <b>peut</b> être remplie, partout.
     *
     * <p>C'est une affirmation d'<b>existence</b>, donc exactement ce qu'une recherche par faisceau
     * sait établir : trouver un chemin le prouve. C'est aussi la seule chose que l'ancienne version
     * de ce fichier disait de vrai, et elle réfute ce que le tableau de bord affirmait depuis M10 —
     * « sur douze configurations, six meurent avant la quatrième tuile ». Cette mesure-là chargeait
     * naïvement ; en jouant <em>pour</em> remplir, on remplit partout.
     *
     * <p>Reste que remplir n'est pas remplir <i>utilement</i> : ce test ne dit rien de ce que la
     * salve vaut ensuite, et c'est volontaire. La question du rendement est ouverte et attend une
     * décision de game design, pas une mesure de plus.
     */
    @Test
    @DisplayName("La file de 5 peut être remplie dans chaque configuration")
    void thequeueCanBeFilledInEveryConfiguration() {
        List<String> stuck = new ArrayList<>();
        for (int width : new int[]{5, 9, 15}) {
            for (int wave = 1; wave <= Arena.WAVE_COUNT; wave++) {
                int deepest = deepestQueue(width, wave);
                if (deepest < ActionQueue.CAPACITY) {
                    stuck.add("largeur " + width + " vague " + wave + " : " + deepest + " tuiles");
                }
            }
        }
        assertTrue(stuck.isEmpty(),
                "la file ne se remplit plus partout : " + stuck + System.lineSeparator()
                        + "  Un faisceau ne trouve pas toujours ce qui existe : elargir BEAM avant"
                        + " de conclure que le remplissage est devenu impossible.");
    }
}
