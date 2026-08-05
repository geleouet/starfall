package com.starfall.sim;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.starfall.game.Arena;
import com.starfall.game.Enemy;
import com.starfall.game.Grid;
import com.starfall.game.Intention;
import com.starfall.game.Occupant;
import com.starfall.game.Tile;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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

    /**
     * Largeur du faisceau.
     *
     * <p>Elle était à 140, et la review a montré que c'était <b>1,75× le seuil de rupture</b> : à
     * 80 la recherche échouait déjà. Un garde-fou posé si près de sa propre limite vire au rouge
     * pour la première retouche venue de {@link Policy#score} — c'est-à-dire pour une raison qui
     * n'est pas « le jeu est devenu impossible », exactement le « crier au loup » que ce test
     * prétend éviter en préférant une recherche à une partie enregistrée.
     *
     * <p>La marge est désormais <b>mesurée et non supposée</b> : {@code searchFindsAWinWellAboveItsBreakingPoint}
     * vérifie qu'un faisceau de moitié trouve encore.
     */
    /** Retour a la ligne des messages d'echec, sans sequence d'echappement dans le source. */
    private static final String NL = System.lineSeparator();

    private static final int BEAM = 400;
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
        return findAWin(gridWidth, startWave, BEAM, MAX_DEPTH);
    }

    private static List<String> findAWin(int gridWidth, int startWave, int beamWidth, int maxDepth) {
        List<Line> beam = new ArrayList<>();
        beam.add(new Line(List.of(), 0));

        for (int depth = 0; depth < maxDepth; depth++) {
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
            beam = next.subList(0, Math.min(beamWidth, next.size()));
        }
        return null;
    }

    /**
     * Deux plateaux <b>réellement</b> identiques ont la même signature.
     *
     * <h2>Ce qu'elle a manqué, et pourquoi ça comptait</h2>
     *
     * <p>La première version se contentait de la <em>première lettre</em> du libellé de chaque
     * occupant, de la vie du héros et des recharges. Elle jetait donc les points de vie ennemis et
     * l'<b>intention annoncée</b> — les deux informations sur lesquelles tout le projet est bâti
     * depuis M6 — et confondait le sabreur avec le souverain, tous deux réduits à un {@code 's'},
     * dans la vague même où le souverain <em>invoque des sabreurs</em>.
     *
     * <p>Conséquence mesurée par la review : 781 collisions sur une seule recherche, dont un
     * souverain à 4 points de vie qui fonce fusionné avec un souverain à 5 qui invoque. Le test
     * passait <b>grâce à</b> cette perte d'information : avec une signature honnête, le même
     * faisceau de 140 échouait.
     *
     * <p>Le risque était unilatéral — une fusion ne peut qu'élaguer, jamais certifier une fausse
     * victoire, puisque celle-ci est constatée sur une arène rejouée. Mais élaguer des états
     * distincts rendait la recherche artificiellement rapide, et « moins d'une seconde » était un
     * chiffre obtenu en ne cherchant pas.
     */
    private static String signature(Arena arena) {
        StringBuilder state = new StringBuilder();
        for (int cell = 0; cell < arena.grid().width(); cell++) {
            Occupant occupant = arena.grid().occupantAt(cell);
            if (occupant == null) {
                state.append('.');
            } else if (occupant == arena.hero()) {
                state.append('H');
            } else if (occupant instanceof Enemy enemy) {
                // L'archétype par son rang, jamais par une initiale : sabreur et souverain
                // commencent tous deux par la même lettre.
                state.append(enemy.kind().ordinal()).append(':').append(enemy.health());
                Intention intention = enemy.intention();
                state.append(':').append(intention.kind().ordinal())
                        .append('@').append(intention.targetCell());
                if (enemy.isStunned()) {
                    state.append('!');
                }
            } else {
                state.append('?');
            }
            state.append(',');
        }
        state.append('|').append(arena.hero().health())
                .append('|').append(arena.hero().facing())
                .append('|').append(arena.wave())
                .append('|').append(arena.queue().fromOldest());
        for (Tile tile : arena.rack().tiles()) {
            state.append('|').append(arena.rack().missingPoints(tile));
        }
        return state.toString();
    }

    /**
     * La rencontre finale reste gagnable, sur <b>chacune</b> des largeurs jouables.
     *
     * <p>C'est la vague la plus dure, donc la plus susceptible de basculer du mauvais côté quand un
     * nombre bouge. Et la largeur n'est pas un détail de cadrage : elle change la distance à laquelle
     * le souverain peut charger, le nombre de cases où ses sbires peuvent naître, et la place que le
     * héros a pour s'extraire. Une rencontre équilibrée sur 9 cases peut devenir impossible sur 5 —
     * trop serré pour esquiver — ou sur 15 — trop long pour rattraper l'archer avant qu'il tire.
     *
     * <p><b>Le balayage est complet, et il l'est pour une raison précise.</b> Ce test n'a d'abord
     * tenu que la largeur 9, puis 5 et 15 ; la review a relevé que 7, 11 et 13 restaient sans
     * garde-fou <em>pendant que le tableau de bord annonçait une mesure sur six largeurs</em> — et
     * que le jeu en accepte onze, les paires comprises, que j'avais oubliées. Un écart entre ce
     * qu'on affirme et ce qu'on garde finit toujours par se payer. Onze recherches coûtent moins de
     * deux secondes ; il n'y avait aucune raison d'échantillonner.
     *
     * <p><b>Rien n'est affirmé sur la marge.</b> Une recherche en faisceau trouve <i>une</i>
     * victoire, jamais la meilleure : un seuil de points de vie mesurerait la qualité de mon
     * heuristique et non la difficulté du jeu. Le projet s'est déjà fait prendre à ce piège, et le
     * seuil avait été retiré plutôt que rabaissé. Ce qu'on garde ici est unilatéral et honnête : une
     * victoire <b>existe</b>, et elle ne demande pas un nombre absurde de gestes.
     */
    @ParameterizedTest(name = "largeur {0}")
    @DisplayName("La rencontre du souverain reste gagnable sur chaque largeur jouable")
    @MethodSource("everyPlayableWidth")
    void thesovereignEncounterStaysWinnableAtEveryWidth(int width) {
        List<String> win = findAWin(width, Arena.WAVE_COUNT);

        assertTrue(win != null, "aucun chemin vers la victoire sur la rencontre finale en largeur "
                + width + "." + NL
                + "  Un faisceau prouve qu'une victoire existe, jamais qu'elle n'existe pas :" + NL
                + "  elargir BEAM avant d'accuser l'equilibrage. Si un faisceau large ne trouve" + NL
                + "  toujours rien, c'est la rencontre qui est devenue impossible.");

        // Pas de « rejoue et verifie que ca gagne » : findAWin ne rend une ligne QUE lorsqu'elle
        // vient d'evaluer ce meme rejeu a vrai, et Playout.replay est pur. Reemettre l'appel serait
        // une assertion incapable d'echouer -- le defaut exact que ce fichier a deja eu a corriger
        // trois fois. Ce qui peut echouer, en revanche, c'est la LONGUEUR : une rencontre qui se
        // durcirait passerait de seize gestes a beaucoup plus sans cesser d'etre gagnable, et
        // personne ne le verrait.
        assertTrue(win.size() < MAX_DEPTH - 20, "victoire trouvee en largeur " + width + " apres "
                + win.size() + " gestes, trop pres du plafond de " + MAX_DEPTH);
    }

    /** Les onze largeurs que le jeu accepte, les paires comprises. */
    static java.util.stream.IntStream everyPlayableWidth() {
        return java.util.stream.IntStream.rangeClosed(Grid.MIN_WIDTH, Grid.MAX_WIDTH);
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

        // On rejoue la ligne geste par geste et on relève les vagues traversées. Affirmer
        // « finished.wave() == WAVE_COUNT » était une tautologie : settle ne déclare la victoire
        // que sous cette condition. Ce qui n'est PAS acquis, c'est que la ligne soit réellement
        // passée par toutes les vagues plutôt que d'en sauter une par un chemin qu'on n'a pas prévu.
        Set<Integer> traversed = new LinkedHashSet<>();
        for (int step = 0; step <= win.size(); step++) {
            traversed.add(Playout.replay(9, 1, win.subList(0, step)).wave());
        }
        for (int wave = 1; wave <= Arena.WAVE_COUNT; wave++) {
            assertTrue(traversed.contains(wave),
                    "la ligne gagnante n'est jamais passee par la vague " + wave
                            + " : vagues traversees " + traversed);
        }

        // Volontairement AUCUNE assertion sur la marge de vie, et c'est le fruit d'une erreur qu'il
        // vaut mieux consigner que taire.
        //
        // La version précédente exigeait « au moins 1 point de vie ». C'était une tautologie :
        // settle rend DEFEAT avant le bloc de victoire dès que la vie tombe à zéro, donc une
        // victoire implique déjà une vie non nulle. En la remplaçant par un seuil qui pouvait
        // tomber — trois points — le test est devenu rouge : cette recherche gagne à deux.
        //
        // Or ce n'est pas la marge du JEU, c'est celle de cette recherche. Elle trie sur
        // Policy.score, qui optimise la progression ; une évaluation orientée survie fait bien
        // mieux — la review du jalon d'équilibrage a mesuré des lignes gagnantes à huit points de
        // vie sur huit. Un seuil ici mesurerait donc la qualité de l'heuristique et rougirait à
        // chaque retouche de celle-ci, pour une raison qui n'a rien à voir avec la jouabilité.
        //
        // Ce test répond à « existe-t-il encore un chemin ». La marge est une mesure, pas un
        // garde-fou, et elle a sa place dans le bilan d'équilibrage.
    }

    /**
     * Le contrôle négatif : <b>cette recherche sait-elle échouer ?</b>
     *
     * <p>Le projet s'est donné cette règle en M6 — « un test qui ne casse pas sur le bug qu'il vise
     * ne vaut rien » — et l'a appliquée en réintroduisant le défaut du colosse pour voir le test
     * rougir. Le garde-fou le plus cher du lot n'avait jamais eu droit à ce traitement.
     *
     * <p>On lui donne donc une profondeur où aucune victoire n'est possible, et on exige qu'il le
     * dise. Sans cela, un {@code findAWin} qui rendrait n'importe quoi de non-{@code null} ferait
     * passer les deux autres tests sans jamais chercher.
     */
    @Test
    @DisplayName("La recherche sait rendre échec quand il n'y a rien à trouver")
    void theSearchKnowsHowToFail() {
        assertTrue(findAWin(9, 1, BEAM, 3) == null,
                "une victoire annoncee en trois gestes : la recherche ne cherche pas");
    }

    /**
     * La marge du faisceau, mesurée plutôt que supposée.
     *
     * <p>À 140, la review a montré que la recherche échouait dès 80 — soit une marge de 1,75×, assez
     * faible pour que la première retouche de l'évaluation fasse rougir le garde-fou pour une
     * mauvaise raison. On vérifie donc qu'un faisceau de <b>moitié</b> trouve encore.
     */
    @Test
    @DisplayName("La recherche trouve encore avec un faisceau deux fois plus étroit")
    void searchFindsAWinWellAboveItsBreakingPoint() {
        assertTrue(findAWin(9, 1, BEAM / 2, MAX_DEPTH) != null,
                "un faisceau de " + (BEAM / 2) + " ne trouve plus : le garde-fou est pose trop pres"
                        + " de sa limite, il rougira pour la mauvaise raison");
    }
}
