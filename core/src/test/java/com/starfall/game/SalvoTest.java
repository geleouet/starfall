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
        int volleyHeads = 0;

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

                if (arena.queue().size() >= 2) {
                    volleyHeads++;
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
        // Le nom dit « tete de SALVE » : sur une file d'une seule tuile, previewTop et applyTo
        // decrivent le meme coup isole, et le test ne dit plus rien de la salve. Il faut donc
        // savoir qu'on a bien joue des tetes de file d'au moins deux tuiles.
        assertTrue(volleyHeads > 0,
                "aucune tete de file d'au moins deux tuiles jouee : ce test n'a eprouve que des"
                        + " tuiles isolees, pas ce que son nom annonce");
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
        arena.grid().place(5, new Enemy(EnemyKind.ARCHER)); // un point de vie
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
        // A DEUX cases, et c'est l'estoc qui videra le plateau : depuis l'axe des degats, une
        // frappe n'abat plus un sabreur d'un coup.
        arena.grid().place(arena.heroCell() + 4 * arena.hero().facing().step(),
                new Enemy(EnemyKind.SABREUR));
        arena.announceIntentions();

        // L'ordre est inverse de l'intuition : la file part du DERNIER pose. C'est donc l'estoc
        // qui frappe en premier et vide le plateau, et la frappe qui doit traverser la vague.
        arena.queueTile(Tile.STRIKE);   // vise la case d'a cote : ne portera pas
        arena.queueTile(Tile.THRUST);   // part la premiere et vide le plateau
        int waveBefore = arena.wave();

        arena.unleash();

        assertEquals(waveBefore + 1, arena.wave(),
                "le terrain vide devait faire venir la vague suivante");
        assertEquals(1, arena.queue().size(),
                "la frappe devait traverser la vague, pas etre gaspillee : file "
                        + arena.queue().fromOldest());
        assertEquals(Tile.STRIKE, arena.queue().fromOldest().get(0),
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

    /**
     * <b>La salve raconte son déroulement, temps par temps.</b>
     *
     * <p>C'est ce qui manque pour rendre le geste central lisible : cinq tuiles se résolvent dans
     * un seul tour, et l'écran passe de l'avant à l'après sans montrer l'enchaînement. Le modèle
     * reste instantané — délibérément — mais il consigne désormais ce qu'il a fait, dans l'ordre,
     * pour que la vue puisse prendre du retard sur lui.
     *
     * <p>Trois moitiés, et chacune peut échouer seule : l'<b>ordre</b> doit être celui de
     * l'exécution, du sommet vers le bas ; la <b>case</b> de chaque temps doit être celle que la
     * tuile visait, lue avant l'exécution puisqu'après elle ne décrit plus rien ; et un geste
     * simple ne doit produire <em>aucun</em> temps, sans quoi la scène animerait un pas.
     */
    @Test
    @DisplayName("La salve consigne ses temps, dans l'ordre et avec leur case")
    void theVolleyRecordsItsBeatsInOrder() {
        Arena arena = new Arena(11, 1);
        arena.grid().place(3, new Enemy(EnemyKind.SABREUR));
        arena.announceIntentions();

        arena.queueTile(Tile.THRUST);   // posee en premier, donc jouee en DERNIER
        arena.queueTile(Tile.STRIKE);
        assertTrue(arena.beats().isEmpty(), "poser n'est pas un deroulement");

        arena.unleash();
        List<Arena.Beat> beats = arena.beats();

        assertEquals(2, beats.size(), "deux tuiles parties, deux temps : " + beats);
        assertEquals(Tile.STRIKE, beats.get(0).tile(),
                "la file part du DERNIER pose : la frappe d'abord");
        assertEquals(Tile.THRUST, beats.get(1).tile(), "puis l'estoc");
        assertEquals(arena.heroCell() + 1, beats.get(0).cell(),
                "la frappe vise la case juste devant");
        assertEquals(arena.heroCell() + 2, beats.get(1).cell(),
                "l'estoc vise la deuxieme, et cette case se lit AVANT l'execution");

        // Et un geste simple ne raconte rien : sans cela, la scene animerait un pas.
        arena.step(arena.hero().facing().opposite());
        assertTrue(arena.beats().isEmpty(),
                "un demi-tour a produit des temps : " + arena.beats());

        // LA MOITIE QUI MORD. Les deux assertions de case ci-dessus ne distinguent rien : ni la
        // frappe ni l'estoc ne deplacent le heros, si bien que la case visee est la meme avant et
        // apres l'execution - mesure, la mutation « lire la case APRES » les laissait vertes. Il
        // faut une tuile qui BOUGE, dont la case d'arrivee change des qu'elle a joue.
        Arena moving = new Arena(11, 5);
        moving.announceIntentions();
        int before = moving.heroCell();
        Direction back = moving.hero().facing().opposite();
        moving.queueTile(Tile.SIDESTEP);
        moving.unleash();

        assertEquals(1, moving.beats().size(), "une tuile partie, un temps");
        assertEquals(before + back.step(), moving.beats().get(0).cell(),
                "le temps doit porter la case d'ARRIVEE calculee avant le pas ; lue apres, elle"
                        + " designerait la case suivante, celle du pas d'apres");
        assertEquals(before + back.step(), moving.heroCell(),
                "et le heros y est bien : le montage ne vaut que si le pas a eu lieu");
    }

    /**
     * <b>Chaque temps porte le plateau tel qu'il était à ce moment-là.</b>
     *
     * <p>C'est ce qui permet aux figures de reculer dans le temps. Sans instantané, le plateau
     * affiche l'état <em>final</em> pendant que le déroulé égrène les cases : on voit l'ordre des
     * coups sans voir leurs effets, ce qui est la moitié de la lecture.
     *
     * <p>La mise en scène est choisie pour que les deux temps <b>diffèrent</b> : la poussée déplace
     * le sabreur, l'estoc l'abat ensuite. Un instantané qui pointerait vers les objets vivants —
     * ou qui serait pris après coup — montrerait un plateau déjà vide au premier temps, et le
     * déroulé serait un diaporama de la même image.
     */
    @Test
    @DisplayName("Chaque temps porte le plateau de son instant")
    void everyBeatCarriesTheBoardOfItsMoment() {
        Arena arena = new Arena(11, 0);
        arena.grid().place(1, new Enemy(EnemyKind.SABREUR));
        arena.announceIntentions();

        arena.queueTile(Tile.THRUST);   // posee en premier, donc jouee en DERNIER
        arena.queueTile(Tile.PUSH);     // jouee en premier : elle repousse
        arena.unleash();

        List<Arena.Beat> beats = arena.beats();
        assertEquals(2, beats.size(), "deux tuiles parties : " + beats);

        long standingAfterPush = beats.get(0).board().stream().filter(f -> !f.hero()).count();
        assertEquals(1, standingAfterPush,
                "au premier temps le sabreur tient encore sa case : " + beats.get(0).board());
        assertEquals(2, beats.get(0).board().stream().filter(f -> !f.hero()).findFirst()
                .orElseThrow().cell(), "et la poussee l'a bien deplace de 1 vers 2");

        long standingAfterThrust = beats.get(1).board().stream().filter(f -> !f.hero()).count();
        assertEquals(0, standingAfterThrust,
                "au second temps l'estoc l'a abattu : " + beats.get(1).board());

        // ET LA FILE SE VIDE AU MEME RYTHME. Sans cela elle apparait videe d'un bloc pendant que
        // les tuiles partent une a une : l'autre moitie de l'ecran raconterait une autre histoire
        // que le plateau, et l'on perdrait de vue laquelle vient de jouer.
        assertEquals(1, beats.get(0).queued().size(),
                "apres la poussee il reste l'estoc dans la file : " + beats.get(0).queued());
        assertEquals(Tile.THRUST, beats.get(0).queued().get(0));
        assertTrue(beats.get(1).queued().isEmpty(),
                "apres le dernier coup la file est vide : " + beats.get(1).queued());

        // Et le heros figure dans les deux, sans quoi le plateau anime serait borgne.
        assertTrue(beats.get(0).board().stream().anyMatch(Arena.Figure::hero),
                "le heros manque au premier temps");
        assertTrue(beats.get(1).board().stream().anyMatch(Arena.Figure::hero),
                "le heros manque au second temps");
    }
}
