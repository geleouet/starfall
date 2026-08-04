package com.starfall.sim;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.starfall.game.Arena;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Le garde-fou qui manquait : <b>le jeu doit rester gagnable</b>.
 *
 * <h2>Pourquoi</h2>
 *
 * <p>C'était le trou le plus dangereux relevé par la review du jalon d'équilibrage. Toute la suite
 * de tests vérifie que les règles font ce qu'elles disent ; <em>aucune</em> ne vérifiait qu'il
 * existe encore un chemin vers la victoire. Un point de vie retiré, un ennemi de plus dans une
 * vague, un coût de recharge relevé — et la tranche devenait impossible sans qu'une seule assertion
 * ne bronche. Le bilan d'équilibrage lui-même ne l'aurait pas vu : ses trois politiques rendent zéro
 * victoire, donc zéro reste zéro.
 *
 * <h2>Une recherche, pas une partie enregistrée</h2>
 *
 * <p>Enregistrer une suite de gestes gagnante aurait été plus rapide, et aurait crié au loup à
 * chaque changement : la moindre retouche au comportement d'un ennemi invalide une ligne écrite à la
 * main, pour une raison qui n'est pas « le jeu est devenu impossible ». Une recherche, elle, échoue
 * pour la seule raison qui nous intéresse — <b>il n'existe plus de chemin</b>.
 *
 * <p>C'est un faisceau et non une exploration exhaustive : il prouve qu'une victoire existe, jamais
 * qu'elle n'existe pas. Un échec doit donc se lire « la recherche n'a pas trouvé », et la première
 * chose à faire alors est d'élargir le faisceau avant d'accuser l'équilibrage. Le message le dit.
 */
class WinnabilityTest {

    /** Largeur du faisceau. Assez pour trouver, assez peu pour rester une poignée de secondes. */
    private static final int BEAM = 140;
    /** Profondeur maximale en gestes, gratuits compris. Une tranche gagnée en fait moins de 80. */
    private static final int MAX_DEPTH = 110;

    /**
     * Une ligne de jeu en cours d'exploration.
     *
     * @param moves   les gestes joués, par leur libellé — la même monnaie que {@link Playout}
     * @param score   l'évaluation de l'état atteint
     */
    private record Line(List<String> moves, int score) {
    }

    /**
     * Cherche une victoire.
     *
     * @return la suite de gestes gagnante, ou {@code null} si le faisceau n'a rien trouvé
     */
    private static List<String> findAWin(int gridWidth, int startWave) {
        List<Line> beam = new ArrayList<>();
        beam.add(new Line(List.of(), 0));

        for (int depth = 0; depth < MAX_DEPTH; depth++) {
            List<Line> next = new ArrayList<>();
            // Deux lignes qui aboutissent au même plateau sont la même ligne : dédupliquer sur
            // l'état, et non sur la suite de gestes, est ce qui tue les cycles de « pose puis
            // reprends » sans avoir à les interdire.
            Set<String> seen = new LinkedHashSet<>();

            for (Line line : beam) {
                Arena arena = Playout.replay(gridWidth, startWave, line.moves());
                if (arena.isOver()) {
                    continue;
                }
                for (Move move : Move.legal(arena)) {
                    List<String> extended = new ArrayList<>(line.moves());
                    extended.add(move.label());

                    Arena after = Playout.replay(gridWidth, startWave, extended);
                    if (after.isVictory()) {
                        return extended;
                    }
                    if (after.isDefeat() || !seen.add(signature(after))) {
                        continue;
                    }
                    next.add(new Line(extended, Policy.score(after)));
                }
            }

            if (next.isEmpty()) {
                return null;
            }
            next.sort((a, b) -> b.score() - a.score());
            beam = next.subList(0, Math.min(BEAM, next.size()));
        }
        return null;
    }

    /** Deux plateaux identiques ont la même signature. Le détail compte : il décide de l'élagage. */
    private static String signature(Arena arena) {
        StringBuilder state = new StringBuilder();
        for (int cell = 0; cell < arena.grid().width(); cell++) {
            var occupant = arena.grid().occupantAt(cell);
            if (occupant == null) {
                state.append('.');
            } else if (occupant == arena.hero()) {
                state.append('H');
            } else {
                state.append(occupant.label().charAt(0));
            }
        }
        state.append('|').append(arena.hero().health())
                .append('|').append(arena.hero().facing())
                .append('|').append(arena.wave())
                .append('|').append(arena.queue().fromOldest());
        for (var tile : arena.rack().tiles()) {
            state.append('|').append(arena.rack().missingPoints(tile));
        }
        return state.toString();
    }

    /**
     * La rencontre finale doit rester gagnable. C'est la plus dure, donc la plus susceptible de
     * basculer du mauvais côté quand un nombre bouge.
     */
    @Test
    @DisplayName("La rencontre du souverain reste gagnable")
    void theSovereignEncounterStaysWinnable() {
        List<String> win = findAWin(9, Arena.WAVE_COUNT);

        assertTrue(win != null,
                "aucun chemin vers la victoire trouve sur la rencontre finale.\n"
                        + "  Un faisceau prouve qu'une victoire existe, jamais qu'elle n'existe pas :\n"
                        + "  elargir BEAM avant d'accuser l'equilibrage. Si un faisceau large ne\n"
                        + "  trouve toujours rien, c'est la tranche qui est devenue impossible.");
        assertTrue(win.size() <= MAX_DEPTH, "victoire trouvee en " + win.size() + " gestes");
    }

    /**
     * La tranche <b>entière</b>, de la première vague à la dernière.
     *
     * <p>Partir de la vague 1 et atteindre la victoire veut dire que les quatre vagues sont tombées :
     * {@code settle} ne déclare la victoire que lorsque le terrain se vide alors qu'il n'y a plus de
     * vague suivante. C'est donc le garde-fou le plus fort du projet — il tient en une assertion ce
     * que la review du jalon d'équilibrage a mis une recherche en faisceau de 6 000 nœuds à établir.
     */
    @Test
    @DisplayName("La tranche entière, de la première vague à la dernière, reste gagnable")
    void theWholeSliceStaysWinnable() {
        List<String> win = findAWin(9, 1);

        assertTrue(win != null, "aucun chemin vers la victoire trouve depuis la premiere vague");

        Arena finished = Playout.replay(9, 1, win);
        assertTrue(finished.isVictory(), "la ligne trouvee doit vraiment gagner, rejeu a l'appui");
        assertTrue(finished.wave() == Arena.WAVE_COUNT,
                "la victoire doit venir de la derniere vague, obtenu " + finished.wave());
        // La marge est ce qui distingue « gagnable » de « jouable ». On ne fixe pas un seuil serré
        // — l'équilibrage bougera — mais gagner à zéro point de vie signalerait un jeu au fil du
        // rasoir, et c'est le genre de bascule qu'on veut voir arriver.
        assertTrue(finished.hero().health() >= 1,
                "victoire obtenue a " + finished.hero().health() + " point de vie");
    }
}
