package com.starfall.scene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.starfall.game.Arena;
import com.starfall.game.ArenaSetup;
import com.starfall.game.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Ce qui bouge entre deux instantanés, et où ça en est.
 *
 * <h2>Pourquoi ces tests-là, et pourquoi ici</h2>
 *
 * <p>L'animation d'un jeu se garde d'ordinaire à l'œil, et ce projet la garde aussi par l'image :
 * l'écran {@code arenaSalvo} saisit une salve à mi-course, et {@code verifyRender} le compare octet
 * à octet. Mais une planche est un <em>point</em> — elle dit qu'à 35 % du premier temps l'écran
 * ressemble à ceci, et rien de plus. Elle ne dit pas que le mouvement part de la bonne case, qu'il
 * arrive à l'autre, qu'il ne dépasse pas, ni qu'il revient. Ces propriétés-là sont continues, et
 * seul un test peut les parcourir.
 *
 * <p>Il y a une seconde raison, plus importante : <b>les visuels de ce jeu vont changer
 * entièrement</b>. Le jour où ils changeront, les quatre-vingt-dix-neuf planches seront à refaire
 * et ne prouveront plus rien de ce qu'elles prouvaient. Ces tests-ci, eux, ne connaissent ni sprite,
 * ni pixel, ni écran : ils survivront intacts. C'est tout l'objet de la séparation qu'ils gardent.
 */
class ChoreographyTest {

    /** Une figure quelconque, à la case dite. Le numéro est ce qui la rend reconnaissable. */
    private static Arena.Figure figure(long id, int cell, int health) {
        return new Arena.Figure(id, cell, "enemy/sabreur", Direction.LEFT, health, 3, false, false);
    }

    private static Arena.Figure hero(long id, int cell) {
        return new Arena.Figure(id, cell, "hero/idle", Direction.RIGHT, 8, 8, true, false);
    }

    /** Le placement de la figure portant ce numéro. */
    private static Choreography.Placement of(List<Choreography.Placement> placements, long id) {
        for (Choreography.Placement placement : placements) {
            if (placement.figure().id() == id) {
                return placement;
            }
        }
        throw new AssertionError("aucun placement pour la figure " + id + " dans " + placements);
    }

    /** Où se trouve vraiment une figure, en cases : c'est ce que la scène va convertir en pixels. */
    private static float position(Choreography.Placement placement) {
        return placement.fromCell()
                + (placement.toCell() - placement.fromCell()) * placement.slide();
    }

    @Nested
    @DisplayName("Les deux bouts")
    class Ends {

        @Test
        @DisplayName("Au repos, chaque figure est exactement sur sa case")
        void restingPlacesEveryFigureOnItsOwnCell() {
            List<Arena.Figure> board = List.of(hero(1, 2), figure(2, 5, 3));

            for (Choreography.Placement placement : Choreography.resting(board)) {
                assertEquals(placement.figure().cell(), position(placement), 1e-6f,
                        "une figure au repos doit etre sur sa case, sans quoi le plateau immobile"
                                + " ne serait plus dessine comme il l'etait");
                assertEquals(1f, placement.fade(), 1e-6f, "une figure au repos est opaque");
                assertEquals(0f, placement.lift(), 1e-6f, "une figure au repos ne flotte pas");
            }
        }

        /**
         * L'invariant qui autorise les planches d'avant l'animation à ne pas bouger.
         *
         * <p>Quatre-vingt-quinze planches saisissaient la fin d'un temps ; la fraction par défaut
         * d'un scénario de capture vaut donc 1. Si à {@code t = 1} la chorégraphie ne rendait pas
         * <em>exactement</em> le plateau d'après, ces planches auraient toutes changé — et leur
         * conformité au premier rendu, qui est la meilleure preuve que cette refonte n'a rien cassé,
         * n'aurait rien voulu dire.
         */
        @Test
        @DisplayName("À la fin d'un temps, le plateau est exactement celui d'après")
        void theEndOfABeatIsExactlyTheBoardThatFollows() {
            List<Arena.Figure> before = List.of(hero(1, 2), figure(2, 4, 3), figure(3, 6, 1));
            List<Arena.Figure> after = List.of(hero(1, 3), figure(2, 4, 1));

            List<Choreography.Placement> placements = Choreography.at(before, after, 6, 1f);

            assertEquals(3f, position(of(placements, 1)), 1e-6f, "le heros a fini son glissement");
            assertEquals(4f, position(of(placements, 2)), 1e-6f, "le blesse a fini de tressaillir");
            assertEquals(0f, of(placements, 3).fade(), 1e-6f, "le mort a fini de disparaitre");
            for (Choreography.Placement placement : placements) {
                if (placement.figure().id() == 3) {
                    // Le mort a fini de s'affaisser, et son affaissement ne se voit pas puisqu'il
                    // ne se voit plus lui-meme. Lui demander de finir droit n'aurait aucun sens.
                    continue;
                }
                assertEquals(1f, placement.fade(), 1e-6f, "les vivants sont opaques a la fin");
                assertEquals(0f, placement.lift(), 1e-6f,
                        "un vivant qui flotte encore a la fin d'un temps flotterait aussi au repos,"
                                + " puisque c'est la meme image");
            }
        }

        @Test
        @DisplayName("Au début d'un temps, le plateau est exactement celui d'avant")
        void theStartOfABeatIsExactlyTheBoardThatPrecedes() {
            List<Arena.Figure> before = List.of(hero(1, 2), figure(2, 4, 3), figure(3, 6, 1));
            List<Arena.Figure> after = List.of(hero(1, 3), figure(2, 4, 1));

            List<Choreography.Placement> placements = Choreography.at(before, after, 6, 0f);

            for (Arena.Figure was : before) {
                assertEquals(was.cell(), position(of(placements, was.id())), 1e-6f,
                        "au premier instant d'un temps, rien n'a encore bouge : sinon le joueur"
                                + " verrait le resultat avant le geste");
                assertEquals(1f, of(placements, was.id()).fade(), 1e-6f,
                        "personne n'a encore commence a disparaitre");
            }
        }
    }

    @Nested
    @DisplayName("Les mouvements")
    class Movements {

        @Test
        @DisplayName("Un déplacement va d'une case à l'autre sans jamais reculer ni dépasser")
        void aMoveGoesFromOneCellToTheOtherWithoutOvershooting() {
            List<Arena.Figure> before = List.of(hero(1, 2));
            List<Arena.Figure> after = List.of(hero(1, 5));

            float previous = -1f;
            for (int step = 0; step <= 20; step++) {
                float t = step / 20f;
                float at = position(of(Choreography.at(before, after, -1, t), 1));

                assertTrue(at >= 2f - 1e-6f && at <= 5f + 1e-6f,
                        "a t=" + t + " le heros est en " + at + " : hors du segment 2 -> 5, il"
                                + " passerait par une case qu'il ne traverse pas");
                assertTrue(at >= previous - 1e-6f,
                        "a t=" + t + " le heros recule de " + previous + " a " + at + " : un"
                                + " deplacement qui revient en arriere se lit comme une hesitation");
                previous = at;
            }
        }

        /**
         * La fente parcourt la même distance quelle que soit la portée.
         *
         * <p>Elle valait d'abord une fraction du chemin jusqu'à la cible, et l'image l'a démentie :
         * l'estoc porte à deux cases, donc la fente déposait le héros <em>au-delà</em> de la case
         * intermédiaire — celle que l'estoc ne frappe pas. Ce test tient la correction.
         */
        @Test
        @DisplayName("Une fente porte aussi loin de près que de loin, et revient")
        void aLungeReachesTheSameDistanceWhateverTheRange() {
            List<Arena.Figure> board = List.of(hero(1, 3));

            float close = position(of(Choreography.at(board, board, 4, Choreography.IMPACT), 1));
            float far = position(of(Choreography.at(board, board, 5, Choreography.IMPACT), 1));

            assertEquals(3f + Choreography.LUNGE_CELLS, close, 1e-5f,
                    "une fente vers la case voisine avance de " + Choreography.LUNGE_CELLS
                            + " case");
            assertEquals(close, far, 1e-5f,
                    "une fente vers une cible deux fois plus loin avance deux fois plus : le heros"
                            + " se retrouve dans la case intermediaire, celle que l'estoc ne frappe"
                            + " justement pas");

            assertEquals(3f, position(of(Choreography.at(board, board, 5, 0f), 1)), 1e-6f,
                    "une fente part de sa case");
            assertEquals(3f, position(of(Choreography.at(board, board, 5, 1f), 1)), 1e-6f,
                    "une fente revient a sa case : celui qui frappe ne se deplace pas");
        }

        @Test
        @DisplayName("Un élan glisse au lieu de se fendre, sans faire les deux")
        void aDashGlidesInsteadOfLunging() {
            // La case visee par un elan EST sa case d'arrivee. Sans garde, la figure glisserait
            // vers elle et se fendrait vers elle : deux fois le meme mouvement.
            List<Arena.Figure> before = List.of(hero(1, 2));
            List<Arena.Figure> after = List.of(hero(1, 4));

            Choreography.Placement placement = of(Choreography.at(before, after, 4, 0.5f), 1);

            assertEquals(2, placement.fromCell(), "l'elan part de la case quittee");
            assertEquals(4, placement.toCell(), "l'elan va jusqu'a sa case d'arrivee");
            assertTrue(placement.lift() > 0f,
                    "un glissement se souleve ; une fente non, et c'est ce qui les distingue");
        }

        @Test
        @DisplayName("Celui qui encaisse recule en s'éloignant de qui l'a frappé")
        void theOneWhoTakesAHitRecoilsAwayFromTheHero() {
            List<Arena.Figure> beforeRight = List.of(hero(1, 3), figure(2, 5, 3));
            List<Arena.Figure> afterRight = List.of(hero(1, 3), figure(2, 5, 1));
            float right = position(of(Choreography.at(beforeRight, afterRight, 5, 0.7f), 2));
            assertTrue(right > 5f,
                    "un ennemi a la DROITE du heros recule vers la droite, or il est en " + right);

            List<Arena.Figure> beforeLeft = List.of(hero(1, 6), figure(2, 4, 3));
            List<Arena.Figure> afterLeft = List.of(hero(1, 6), figure(2, 4, 1));
            float left = position(of(Choreography.at(beforeLeft, afterLeft, 4, 0.7f), 2));
            assertTrue(left < 4f,
                    "un ennemi a la GAUCHE du heros recule vers la gauche, or il est en " + left);
        }

        /**
         * Encaisser sans savoir d'où vient le coup.
         *
         * <p>Reculer demande de savoir <em>de quoi</em>. Une tuile le dit : sa case visée est la
         * direction du coup. La <b>riposte ennemie</b> ne le dit pas — plusieurs ennemis frappent,
         * des deux côtés, et l'agrégat ne désigne aucune case. Choisir un sens serait inventer une
         * information que le modèle n'a pas, dans un jeu dont toute la tension tient à ce que
         * l'annoncé et le joué coïncident.
         *
         * <p>Le tressaillement devient donc vertical. C'est la branche qui joue <b>chaque fois que
         * le héros encaisse</b>, c'est-à-dire à l'événement de dégâts le plus fréquent du jeu — et
         * elle n'avait aucun témoin : les deux seuls cas sans case visée du corpus étaient un
         * glissement et une absence de changement, ni l'un ni l'autre blessé.
         */
        @Test
        @DisplayName("Sans case visée, celui qui encaisse tressaille sur place, jamais de côté")
        void withoutAnAimTheWoundedFlinchInPlace() {
            List<Arena.Figure> before = List.of(hero(1, 4), figure(2, 6, 3));
            List<Arena.Figure> after = List.of(new Arena.Figure(1, 4, "hero/idle",
                    Direction.RIGHT, 6, 8, true, false), figure(2, 6, 3));

            for (int step = 0; step <= 10; step++) {
                float t = step / 10f;
                Choreography.Placement hit = of(Choreography.at(before, after, -1, t), 1);

                assertEquals(4f, position(hit), 1e-6f,
                        "a t=" + t + " le heros a derive horizontalement : sans case visee, aucun"
                                + " sens n'est connu, et en choisir un inventerait la direction du"
                                + " coup");
                assertTrue(hit.lift() <= 0f,
                        "a t=" + t + " le tressaillement souleve au lieu d'enfoncer : encaisser"
                                + " ne fait pas bondir");
            }

            assertEquals(0f, of(Choreography.at(before, after, -1, 0f), 1).lift(), 1e-6f,
                    "rien avant le contact : montrer l'effet avant sa cause est le defaut que le"
                            + " moment du contact existe pour eviter");
            assertTrue(of(Choreography.at(before, after, -1, 0.5f), 1).lift() < 0f,
                    "apres le contact, le coup doit se VOIR : sans quoi seules les pastilles de"
                            + " points de vie changent, et rien ne dit quand");
            assertEquals(0f, of(Choreography.at(before, after, -1, 1f), 1).lift(), 1e-6f,
                    "et il faut se relever : un heros qui reste enfonce a la fin d'un temps"
                            + " resterait enfonce au repos, puisque c'est la meme image");
        }

        @Test
        @DisplayName("Avec une case visée, le tressaillement est horizontal et ne s'enfonce pas")
        void withAnAimTheFlinchIsHorizontalAndNeverSinks() {
            // Le complement du precedent : les deux branches doivent se distinguer, sans quoi un
            // test vert ne dirait pas laquelle a joue.
            List<Arena.Figure> before = List.of(hero(1, 3), figure(2, 5, 3));
            List<Arena.Figure> after = List.of(hero(1, 3), figure(2, 5, 1));

            Choreography.Placement hit = of(Choreography.at(before, after, 5, 0.5f), 2);

            assertTrue(position(hit) > 5f, "avec une case visee, le recul est horizontal");
            assertEquals(0f, hit.lift(), 1e-6f,
                    "et seulement horizontal : les deux formes de tressaillement se distinguent"
                            + " par ce qu'elles ne font pas autant que par ce qu'elles font");
        }

        @Test
        @DisplayName("Celui qui tombe reste entier jusqu'au contact, puis s'efface")
        void theOneWhoFallsStaysWholeUntilTheBlowLands() {
            List<Arena.Figure> before = List.of(hero(1, 3), figure(2, 4, 1));
            List<Arena.Figure> after = List.of(hero(1, 3));

            for (float t = 0f; t < Choreography.IMPACT; t += 0.05f) {
                assertEquals(1f, of(Choreography.at(before, after, 4, t), 2).fade(), 1e-6f,
                        "a t=" + t + ", avant le contact, la cible est encore entiere : la faire"
                                + " disparaitre plus tot montrerait l'effet avant sa cause");
            }
            assertTrue(of(Choreography.at(before, after, 4, 0.7f), 2).fade() < 1f,
                    "apres le contact, la cible s'efface");
        }

        @Test
        @DisplayName("Quand rien ne change, rien ne bouge")
        void nothingMovesWhenNothingChanges() {
            List<Arena.Figure> board = List.of(hero(1, 3), figure(2, 5, 3));

            for (int step = 0; step <= 10; step++) {
                for (Choreography.Placement placement : Choreography.at(board, board, -1,
                        step / 10f)) {
                    assertEquals(placement.figure().cell(), position(placement), 1e-6f,
                            "sans changement et sans cible, personne n'a de raison de bouger");
                }
            }
        }
    }

    @Nested
    @DisplayName("Ce dont la chorégraphie ne doit pas dépendre")
    class Independence {

        /**
         * Le numéro d'une figure sert à la reconnaître, jamais à la placer.
         *
         * <p>{@code Identities} l'affirme dans son javadoc : les numéros ne sont ni ordonnés, ni
         * reproductibles d'une exécution à l'autre. L'affirmation est fragile — elle deviendrait
         * fausse au premier tri par numéro, au premier parcours de {@code HashMap} dont l'ordre
         * dépend des clés, au premier numéro peint à l'écran. Aucune relecture ne garde durablement
         * une propriété pareille.
         *
         * <p>Ce test la garde. Deux arènes bâties séparément portent des numéros différents — le
         * compteur ne revient jamais en arrière — et jouent pourtant la même ligne. Toute
         * dépendance à la <em>valeur</em> d'un numéro ferait diverger les deux chorégraphies.
         */
        @Test
        @DisplayName("Deux parties identiques aux numéros différents se déroulent pareil")
        void identicalGamesWithDifferentIdsPlayTheSame() {
            List<List<String>> first = choreographyOfASalvo();
            List<List<String>> second = choreographyOfASalvo();

            assertNotEquals(idsOfASalvo(), idsOfASalvo(),
                    "les deux arenes portent les memes numeros : la demonstration ne demontrerait"
                            + " rien, puisque c'est justement leur difference qu'elle exploite");
            assertEquals(first, second,
                    "la choregraphie differe entre deux parties identiques : quelque chose depend"
                            + " de la VALEUR des numeros de figure, et non seulement de leur"
                            + " egalite");
        }

        /** Les numéros de figure d'une salve fraîche, dans l'ordre du plateau. */
        private List<Long> idsOfASalvo() {
            Arena arena = playASalvo();
            List<Long> ids = new ArrayList<>();
            for (Arena.Figure figure : arena.opening()) {
                ids.add(figure.id());
            }
            return ids;
        }

        /** Toute la chorégraphie d'une salve fraîche, réduite à ce qui se voit. */
        private List<List<String>> choreographyOfASalvo() {
            Arena arena = playASalvo();
            List<List<String>> everything = new ArrayList<>();
            List<Arena.Figure> before = arena.opening();

            for (Arena.Beat beat : arena.beats()) {
                for (int step = 0; step <= 8; step++) {
                    List<String> moment = new ArrayList<>();
                    for (Choreography.Placement placement
                            : Choreography.at(before, beat.board(), beat.cell(), step / 8f)) {
                        // Tout SAUF le numero : la sortie ne doit pas en dependre, donc la
                        // comparaison ne doit pas le regarder.
                        moment.add(placement.figure().sprite() + "@" + placement.figure().cell()
                                + " " + position(placement) + " " + placement.lift()
                                + " " + placement.fade());
                    }
                    everything.add(moment);
                }
                before = beat.board();
            }
            assertTrue(everything.size() > 1, "une salve d'un seul temps ne prouverait pas grand-chose");
            return everything;
        }

        /** La ligne exacte que l'écran de capture joue, pour que l'image et le test s'accordent. */
        private Arena playASalvo() {
            Arena arena = ArenaSetup.trainingArena(9);
            for (Function<Arena, ?> action : SalvoScript.ACTIONS) {
                action.apply(arena);
            }
            return arena;
        }
    }

    @Nested
    @DisplayName("Le point de passage unique")
    class SinglePath {

        /**
         * Le déroulé et le repos passent par la même porte.
         *
         * <p>La règle est écrite dans {@code Playback.placements} et vaut ce que vaut une règle
         * écrite : ce projet a payé neuf fois pour apprendre qu'une règle écrite à deux endroits
         * finit par diverger. Ici on vérifie que la porte est bien unique du côté du repos —
         * qu'elle rend les figures vivantes telles quelles, sans en perdre ni en inventer.
         */
        @Test
        @DisplayName("Au repos, le déroulé rend le plateau vivant, figure pour figure")
        void atRestThePlaybackHandsBackTheLivingBoard() {
            Arena arena = ArenaSetup.trainingArena(9);
            Playback playback = new Playback();

            List<Choreography.Placement> placements = playback.placements(arena.snapshot());

            assertEquals(arena.snapshot().size(), placements.size(),
                    "le repos ne doit ni perdre ni inventer de figure");
            for (int i = 0; i < placements.size(); i++) {
                // assertEquals, et non assertSame : sur des « long » emballes, assertSame ne
                // compare que des boites. Ce test etait vert lance seul - les petits numeros
                // partagent leurs boites - et rouge dans la suite complete, ou le compteur est
                // haut. Un instrument qui ne dit vrai que sur les petits nombres ne demontre rien.
                assertEquals(arena.snapshot().get(i).id(), placements.get(i).figure().id(),
                        "le repos doit rendre les figures dans l'ordre du plateau");
            }
        }
    }
}
