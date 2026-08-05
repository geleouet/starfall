package com.starfall.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Ce que la salve promet, et ce que la review a trouvé qu'elle ne tenait pas.
 *
 * <p>Le jalon d'équilibrage a déplacé le coût du tour de l'exécution vers le chargement. C'est une
 * modification du cœur du jeu, et elle a ouvert exactement les trous qu'on pouvait attendre : un
 * invariant vedette désactivé plutôt que resserré, une affirmation de javadoc devenue fausse, et un
 * comportement de bord — la salve qui continue sur un plateau vide — que personne ne regardait.
 */
class SalvoTest {

    /**
     * L'invariant de M8, resserré au lieu d'être court-circuité.
     *
     * <p>{@code TilePreviewTest} avait cessé de confronter l'annonce au résultat dès que la file
     * portait plus d'une tuile — c'est-à-dire précisément dans le cas que ce jalon introduit. Or
     * l'invariant atteignable existe et il est vrai par construction : <b>la première tuile d'une
     * salve part contre le plateau exactement tel qu'il était prévisualisé</b>, sans phase ennemie
     * intercalée. C'est tout ce que le préavis annonce, et c'est donc tout ce qu'il faut vérifier.
     */
    @Test
    @DisplayName("La première tuile d'une salve fait ce que le préavis annonçait")
    void theFirstTileOfAVolleyDoesWhatWasAnnounced() {
        List<String> failures = new ArrayList<>();

        for (int seed = 0; seed < 300 && failures.size() <= 5; seed++) {
            Random random = new Random(seed);
            int width = Grid.MIN_WIDTH + random.nextInt(Grid.MAX_WIDTH - Grid.MIN_WIDTH + 1);
            Arena arena = ArenaSetup.trainingArena(width);

            for (int action = 0; action < 40 && !arena.isOver(); action++) {
                if (arena.queue().size() < 2 && random.nextInt(3) > 0) {
                    List<Tile> rack = arena.rack().tiles();
                    arena.queueTile(rack.get(random.nextInt(rack.size())));
                    continue;
                }
                if (arena.queue().isEmpty()) {
                    arena.step(random.nextBoolean() ? Direction.LEFT : Direction.RIGHT);
                    continue;
                }

                Tile first = arena.queue().top();
                TilePreview preview = arena.previewTop();
                // On ne joue QUE la première tuile, avec le même chemin d'exécution que la salve.
                ActionResult played = first.applyTo(arena);

                if (played != preview.outcome()) {
                    failures.add("graine " + seed + " : " + first + " annoncait "
                            + preview.outcome() + ", a produit " + played);
                }
                arena.unleash();
            }
        }

        assertTrue(failures.isEmpty(), "le preavis ment sur la tete de salve :\n  "
                + String.join("\n  ", failures));
    }

    /**
     * Une salve ne continue pas sur un plateau vide.
     *
     * <p>Sans cette borne, les tuiles suivantes partaient dans le vide, étaient dépensées et mises
     * en recharge — et rien ne prévenait, puisque la ligne d'annonce ne décrit que la première. Le
     * joueur payait un gaspillage qu'il ne pouvait pas voir venir. Ce qui reste dans la file y reste
     * et servira à la vague suivante.
     */
    @Test
    @DisplayName("La salve s'arrête quand le terrain se vide")
    void theVolleyStopsWhenTheBoardEmpties() {
        Arena arena = new Arena(9, 4);
        arena.queueTile(Tile.THRUST);   // ne portera pas : elle vise a deux cases
        arena.queueTile(Tile.STRIKE);   // partira en premier et videra le plateau
        arena.grid().place(5, new Enemy(EnemyKind.SABREUR));
        arena.announceIntentions();

        arena.unleash();

        assertTrue(arena.enemies().isEmpty(), "la frappe a bien vide le plateau");
        assertEquals(1, arena.queue().size(), "l'estoc ne doit pas avoir ete gaspille");
        assertTrue(arena.rack().isReady(Tile.THRUST) || arena.queue().fromOldest().contains(Tile.THRUST),
                "et il ne doit pas etre parti en recharge");
    }

    /**
     * Et ce qui reste dans la file <b>traverse la vague</b>.
     *
     * <p>Le test ci-dessus se joue sur un plateau d'essai, sans campagne : il montre que les tuiles
     * ne sont pas gaspillées, jamais qu'elles survivent à l'arrivée de la vague suivante. Or c'est
     * cette moitié-là qui compte, et c'est elle qui m'a fait écrire une démonstration fausse : j'ai
     * affirmé qu'à la victoire la file est toujours vide, en oubliant que la salve <em>sort</em> de
     * sa boucle quand le terrain se vide. Sur la dernière vague, cela veut dire une victoire file
     * garnie — ce qu'une review a démontré et qu'un test tient désormais dans
     * {@code SilenceAfterTheEndTest}.
     *
     * <p>Celui-ci tient l'autre moitié : sur une vague qui n'est pas la dernière, les tuiles
     * restantes passent de l'autre côté avec le héros.
     */
    @Test
    @DisplayName("Ce qui reste dans la file traverse l'arrivée de la vague suivante")
    void whatStaysInTheQueueSurvivesTheNextWave() {
        Arena arena = ArenaSetup.trainingArena(9, 1);
        while (!arena.enemies().isEmpty()) {
            arena.grid().clear(arena.grid().indexOf(arena.enemies().get(0)));
        }
        arena.grid().place(arena.heroCell() + arena.hero().facing().step(),
                new Enemy(EnemyKind.SABREUR));
        arena.announceIntentions();

        arena.queueTile(Tile.THRUST);   // vise à deux cases : ne portera pas
        arena.queueTile(Tile.STRIKE);   // part la première et vide le plateau
        int waveBefore = arena.wave();

        arena.unleash();

        assertEquals(waveBefore + 1, arena.wave(),
                "le terrain vide devait faire venir la vague suivante");
        assertEquals(1, arena.queue().size(),
                "l'estoc devait traverser la vague, pas etre gaspille : file "
                        + arena.queue().fromOldest());
        assertEquals(Tile.THRUST, arena.queue().fromOldest().get(0),
                "et c'est bien celle qui n'avait pas porte");
    }

    /**
     * Poser puis reprendre est un <b>tour de passe</b>, et le javadoc affirmait le contraire.
     *
     * <p>« Il n'y a rien à y gagner », disait-il. En réalité : un tour dépensé, aucune tuile
     * consommée, position et orientation intactes, et toutes les recharges qui avancent d'un cran.
     * C'est strictement meilleur que le demi-tour, qui coûte le même prix mais retourne le héros.
     * C'est donc un vrai outil tactique, et le nommer vaut mieux que le nier — sur un plateau où
     * l'on gagne en plaçant, savoir laisser passer un tour est une décision.
     */
    @Test
    @DisplayName("Poser puis reprendre est un tour de passe neutre")
    void queueingThenTakingBackIsANeutralPassTurn() {
        Arena arena = new Arena(9, 4);
        arena.queueTile(Tile.STRIKE);
        arena.unleash();                       // met la frappe en recharge
        int missing = arena.rack().missingPoints(Tile.STRIKE);
        int cell = arena.heroCell();
        Direction facing = arena.hero().facing();
        int turns = arena.turnsTaken();

        arena.queueTile(Tile.THRUST);
        arena.unqueueAt(0);

        assertEquals(turns + 1, arena.turnsTaken(), "un tour, et un seul");
        assertEquals(missing - 1, arena.rack().missingPoints(Tile.STRIKE),
                "les recharges avancent : c'est tout l'interet du geste");
        assertTrue(arena.rack().isReady(Tile.THRUST), "aucune tuile depensee");
        assertEquals(cell, arena.heroCell(), "le heros n'a pas bouge");
        assertEquals(facing, arena.hero().facing(), "ni tourne, contrairement au demi-tour");
    }

    /**
     * Le cas mixte, celui qui décide de la règle : une salve contenant une tuile payante <b>qui ne
     * porte pas</b> et une tuile Free-Play <b>qui porte</b>.
     *
     * <p>Elle doit être gratuite, et l'énoncé le dit déjà : la salve coûte un tour si au moins une
     * tuile <em>porte</em> et n'est pas Free-Play. Une tuile payante qui rate n'est pas une raison
     * de payer — c'est la règle de M5, « une action qui échoue ne coûte pas de tour », et elle vaut
     * pour chaque tuile de la salve prise séparément.
     *
     * <p>Le cas n'était couvert par aucun test : il fallait le fuzz de la review pour l'atteindre,
     * et un fuzz qui ne trouve rien ne prouve rien tant que personne n'a écrit l'assertion.
     */
    @Test
    @DisplayName("Une salve où seules les tuiles Free-Play portent ne coûte rien")
    void avolleyWhereOnlyFreePlayTilesConnectCostsNothing() {
        Arena arena = new Arena(9, 4);
        // Personne devant : la frappe sera dépensée sans porter. La volte-face, elle, porte
        // toujours — se retourner ne peut pas rater.
        arena.queueTile(Tile.STRIKE);
        arena.queueTile(Tile.PIVOT);
        int before = arena.turnsTaken();

        arena.unleash();

        assertEquals(before, arena.turnsTaken(),
                "seule une tuile Free-Play a porte : la salve ne doit rien couter");
        assertTrue(!arena.rack().isReady(Tile.STRIKE),
                "la frappe est tout de meme depensee : c'est le prix de l'avoir jouee");
    }

    /**
     * Le coût d'une salve, énoncé comme une règle et vérifié comme telle : <b>N tours pour charger,
     * un pour lâcher</b>. C'est l'arithmétique que le joueur doit pouvoir faire de tête, et elle
     * n'était écrite nulle part.
     */
    @Test
    @DisplayName("Une salve de N coûte N+1 tours, une tuile isolée en coûte deux")
    void aVolleyOfNCostsNPlusOneTurns() {
        for (int size = 1; size <= 4; size++) {
            Arena arena = new Arena(15, 7);
            arena.grid().place(8, new Enemy(EnemyKind.COLOSSE));
            arena.announceIntentions();

            Tile[] loadout = {Tile.STRIKE, Tile.PUSH, Tile.THRUST, Tile.DASH};
            for (int i = 0; i < size; i++) {
                arena.queueTile(loadout[i]);
            }
            int afterLoading = arena.turnsTaken();
            arena.unleash();

            assertEquals(size, afterLoading, "charger " + size + " tuiles coute " + size + " tours");
            assertEquals(size + 1, arena.turnsTaken(),
                    "une salve de " + size + " doit couter " + (size + 1) + " tours en tout");
        }
    }
}
