package com.starfall.scene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.starfall.StarfallGame;
import com.starfall.game.ActionResult;
import com.starfall.game.Arena;
import com.starfall.game.ArenaLayout;
import com.starfall.game.ArenaSetup;
import com.starfall.game.Direction;
import com.starfall.game.Grid;
import com.starfall.game.HudLayout;
import com.starfall.game.Tile;
import com.starfall.render.PixelFont;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Deux façons dont un libellé d'interface échoue sans que personne ne s'en aperçoive.
 *
 * <ol>
 *   <li><b>Un caractère que la police ne connaît pas.</b> {@code PixelFont.draw} saute en silence
 *       ce qu'elle ne sait pas dessiner : « PRÊTE » s'afficherait « PRTE » sans le moindre signal,
 *       et le jeu est intégralement en français accentué.</li>
 *   <li><b>Une ligne trop longue.</b> Elle sort de la zone garantie, donc elle disparaît sur
 *       certaines tailles de fenêtre et pas sur d'autres — jamais sur celle où l'on développe.</li>
 * </ol>
 *
 * <p>Le test engendre <em>toutes</em> les phrases que l'interface peut produire, sur des milliers
 * d'états de partie tirés au hasard, et les mesure une par une. C'est faisable parce que
 * {@link HudText} est pur : c'était la raison de le séparer du rendu.
 */
class HudTextTest {

    private static final int SEEDS = 120;
    private static final int ACTIONS_PER_SEED = 40;

    /**
     * Largeur utile d'une ligne de panneau, en pixels-monde : la bande d'interface, moins la marge
     * gauche du panneau et ses deux marges intérieures.
     *
     * <p>Elle se mesure sur {@link StarfallGame#MIN_WORLD_WIDTH} et non sur la zone garantie du
     * viewport, parce que c'est là que l'interface est ancrée : cette bande de 320 px-monde est la
     * seule qui accompagne le plateau quelle que soit la forme de la fenêtre.
     */
    private static final int PANEL_TEXT_WIDTH =
            StarfallGame.MIN_WORLD_WIDTH - 8 - 2 * HudLayout.PANEL_PADDING;

    /** Largeur utile du bandeau : la bande d'interface moins ses deux marges. */
    private static final int BANNER_WIDTH = StarfallGame.MIN_WORLD_WIDTH - 8;

    /** Le corpus complet des phrases que l'interface peut produire. */
    private record Corpus(Set<String> panel, Set<String> banner) {
        Set<String> all() {
            Set<String> everything = new LinkedHashSet<>(panel);
            everything.addAll(banner);
            return everything;
        }
    }

    /**
     * Toutes les phrases possibles d'un état donné.
     *
     * <p>On balaie tous les survols possibles — chaque tuile du râtelier, chaque emplacement de
     * file, <b>chaque case du plateau</b> — parce que c'est exactement ce que le joueur fera avec
     * sa souris, et que le panneau change de contenu à chaque fois.
     */
    private static void collect(Arena arena, Set<String> panel, Set<String> banner) {
        banner.add(HudText.banner(arena));
        for (ActionResult result : ActionResult.values()) {
            for (int combo : new int[]{0, 1, 3}) {
                panel.add(HudText.lastAction(result, combo));
                panel.addAll(HudText.infoLines(arena, -1, -1, -1, result));
            }
        }
        for (int slot = 0; slot < arena.rack().tiles().size(); slot++) {
            panel.addAll(HudText.infoLines(arena, slot, -1, -1, null));
        }
        for (int slot = 0; slot < arena.queue().size(); slot++) {
            panel.addAll(HudText.infoLines(arena, -1, slot, -1, null));
        }
        for (int cell = 0; cell < arena.grid().width(); cell++) {
            panel.addAll(HudText.infoLines(arena, -1, -1, cell, null));
        }
        if (arena.isOver()) {
            panel.addAll(HudText.outcome(arena));
        }
    }

    private static Corpus gather() {
        Set<String> panel = new LinkedHashSet<>(HudText.help());
        Set<String> banner = new LinkedHashSet<>();

        for (int seed = 0; seed < SEEDS; seed++) {
            Random random = new Random(seed);
            Arena arena = ArenaSetup.trainingArena(
                    Grid.MIN_WIDTH + random.nextInt(Grid.MAX_WIDTH - Grid.MIN_WIDTH + 1));

            for (int action = 0; action < ACTIONS_PER_SEED; action++) {
                collect(arena, panel, banner);
                switch (random.nextInt(4)) {
                    case 0 -> arena.step(random.nextBoolean() ? Direction.LEFT : Direction.RIGHT);
                    case 1 -> arena.unleash();
                    case 2 -> arena.swapWithTarget();
                    default -> {
                        List<Tile> rack = arena.rack().tiles();
                        arena.queueTile(rack.get(random.nextInt(rack.size())));
                    }
                }
            }
            collect(arena, panel, banner);
        }
        return new Corpus(panel, banner);
    }

    @Test
    @DisplayName("Toutes les phrases de l'interface sont dessinables par la police")
    void everyInterfaceLineIsDrawable() {
        List<String> failures = new ArrayList<>();

        for (String line : gather().all()) {
            int missing = PixelFont.firstUndrawableCharacter(line);
            if (missing >= 0) {
                failures.add("« " + line + " » contient le caractere U+"
                        + Integer.toHexString(missing).toUpperCase() + " ('" + (char) missing + "')");
            }
        }

        assertTrue(failures.isEmpty(), "la police sauterait ces caracteres en silence :\n  "
                + String.join("\n  ", failures));
    }

    @Test
    @DisplayName("Aucune phrase de panneau ne déborde de la bande d'interface")
    void noPanelLineOverflowsTheInterfaceBand() {
        List<String> failures = new ArrayList<>();

        for (String line : gather().panel()) {
            int width = PixelFont.widthOf(line, 1);
            if (width > PANEL_TEXT_WIDTH) {
                failures.add("« " + line + " » fait " + width + " px pour " + PANEL_TEXT_WIDTH
                        + " disponibles");
            }
        }

        assertTrue(failures.isEmpty(), "ces lignes seraient tronquees :\n  "
                + String.join("\n  ", failures));
    }

    /**
     * Le bandeau partage sa ligne avec le rappel de la touche d'aide, posé à droite. On mesure donc
     * la somme, plus un espace de séparation : c'est le cas où deux textes qui tiennent chacun se
     * chevauchent quand même.
     *
     * <p>Le compteur de tours est poussé à quatre chiffres à la main. Les corpus tirés au hasard
     * s'arrêtent vers la quarantième action, si bien que la marge réelle du bandeau tenait à des
     * parties courtes — elle a d'ailleurs été mesurée à deux pixels avant que l'enchaînement ne
     * quitte cette ligne.
     */
    @Test
    @DisplayName("Le bandeau et le rappel d'aide ne se chevauchent jamais")
    void theBannerAndTheHelpHintNeverOverlap() {
        int hint = PixelFont.widthOf(HudText.HELP_HINT, 1);

        List<String> failures = new ArrayList<>();
        Set<String> lines = gather().banner();
        lines.add(longestPlausibleBanner());

        for (String line : lines) {
            int total = PixelFont.widthOf(line, 1) + 6 + hint;
            if (total > BANNER_WIDTH) {
                failures.add("« " + line + " » + rappel = " + total + " px pour " + BANNER_WIDTH);
            }
        }

        assertTrue(failures.isEmpty(), "le bandeau deborde :\n  " + String.join("\n  ", failures));
    }

    /** Une partie interminable, tous les compteurs au plus large, et la case du héros sous le feu. */
    private static String longestPlausibleBanner() {
        Arena arena = new Arena(9, 4);
        while (arena.turnsTaken() < 1000) {
            arena.step(arena.hero().facing().opposite());
        }
        String line = HudText.banner(arena);
        // Le bandeau ne porte pas la menace quand personne ne vise le héros : on la simule à la
        // valeur la plus large que la vague 3 puisse produire.
        return line + "  MENACE 99";
    }

    /**
     * Le panneau d'information grandit vers le bas depuis un bord fixe. S'il descend trop, il
     * recouvre les glyphes d'intention — c'est-à-dire qu'une infobulle cacherait le télégraphe,
     * exactement le genre d'occultation que les jalons précédents ont payée deux fois.
     */
    @Test
    @DisplayName("Le panneau d'information ne mord jamais sur les glyphes d'intention")
    void theInfoPanelNeverCoversTheIntentionGlyphs() {
        assertTrue(HudLayout.infoPanelBottom(HudLayout.MAX_INFO_LINES) > ArenaLayout.INTENT_TOP,
                "le panneau descend jusqu'a " + HudLayout.infoPanelBottom(HudLayout.MAX_INFO_LINES)
                        + ", les glyphes montent a " + ArenaLayout.INTENT_TOP);
        assertTrue(HudLayout.INFO_TOP + 1 <= HudLayout.BANNER_BOTTOM,
                "le panneau d'information mord sur le bandeau d'etat");
        assertTrue(HudLayout.BANNER_TOP <= StarfallGame.MIN_WORLD_HEIGHT,
                "le bandeau d'etat sort de la zone garantie");
    }

    /**
     * Le panneau d'information ne dépasse jamais le nombre de lignes que la géométrie autorise.
     *
     * <p>C'est la contrainte qui a décidé de la forme des infobulles : la ligne d'annonce garde la
     * première place — sans quoi survoler le râtelier cachait ce qui allait partir — donc il ne
     * reste que deux lignes pour décrire une tuile, et l'état de recharge a dû rejoindre l'en-tête.
     */
    @Test
    @DisplayName("Le panneau d'information tient toujours dans le nombre de lignes prévu")
    void theInfoPanelAlwaysFitsTheAllowedLineCount() {
        for (int seed = 0; seed < 40; seed++) {
            Random random = new Random(seed);
            Arena arena = ArenaSetup.trainingArena(
                    Grid.MIN_WIDTH + random.nextInt(Grid.MAX_WIDTH - Grid.MIN_WIDTH + 1));

            for (int action = 0; action < 30; action++) {
                for (int slot = -1; slot < arena.rack().tiles().size(); slot++) {
                    assertTrue(HudText.infoLines(arena, slot, -1, -1, ActionResult.STRUCK).size()
                                    <= HudLayout.MAX_INFO_LINES,
                            "infobulle de ratelier trop haute, graine " + seed);
                }
                for (int cell = 0; cell < arena.grid().width(); cell++) {
                    assertTrue(HudText.infoLines(arena, -1, -1, cell, ActionResult.STRUCK).size()
                                    <= HudLayout.MAX_INFO_LINES,
                            "infobulle d'ennemi trop haute, graine " + seed);
                }
                arena.queueTile(arena.rack().tiles().get(random.nextInt(6)));
                arena.step(random.nextBoolean() ? Direction.LEFT : Direction.RIGHT);
            }
        }
    }

    /**
     * La ligne d'annonce ne quitte jamais la première place du panneau.
     *
     * <p>Le défaut qu'elle corrige : survoler une tuile du râtelier remplaçait tout le panneau par
     * l'infobulle. Or l'instant qui suit une pose est celui où le curseur se trouve sur le râtelier
     * <em>et</em> où le joueur veut savoir ce que sa nouvelle tuile va faire — l'interface lui
     * cachait exactement ce qu'il venait de demander, pendant que la touche d'exécution restait
     * active.
     */
    @Test
    @DisplayName("La ligne d'annonce reste visible quoi qu'on survole")
    void theAnnounceLineSurvivesEveryHover() {
        Arena arena = ArenaSetup.trainingArena(9);
        arena.queueTile(Tile.THRUST);
        String announce = HudText.announce(arena);

        for (int slot = 0; slot < arena.rack().tiles().size(); slot++) {
            assertEquals(announce, HudText.infoLines(arena, slot, -1, -1, null).get(0));
        }
        assertEquals(announce, HudText.infoLines(arena, -1, 0, -1, null).get(0));
        for (int cell = 0; cell < arena.grid().width(); cell++) {
            assertEquals(announce, HudText.infoLines(arena, -1, -1, cell, null).get(0));
        }
    }

    /**
     * Une partie finie ne promet plus rien, <b>y compris dans les infobulles</b>.
     *
     * <p>Le préavis avait appris la règle et pas les infobulles : après une défaite, le râtelier
     * annonçait encore « PRÊTE » et la file « CLIC POUR LA REPRENDRE », alors que les six portes
     * d'action renvoient toutes {@code BLOCKED}.
     */
    @Test
    @DisplayName("Les infobulles se taisent quand la partie est finie")
    void tooltipsFallSilentOnceTheGameIsOver() {
        // On joue au hasard jusqu'a la defaite : le jeu s'en charge tout seul, la review l'a
        // mesure a 0 victoire sur 300 parties aleatoires.
        Random random = new Random(3);
        Arena arena = ArenaSetup.trainingArena(9);
        arena.queueTile(Tile.STRIKE);
        while (!arena.isOver() && arena.turnsTaken() < 400) {
            arena.step(random.nextBoolean() ? Direction.LEFT : Direction.RIGHT);
        }
        assertTrue(arena.isOver(), "la partie doit s'etre terminee");

        for (Tile tile : arena.rack().tiles()) {
            String head = HudText.rackHead(arena, tile);
            assertTrue(head.contains("PARTIE FINIE"),
                    "l'infobulle de " + tile + " promet encore quelque chose : " + head);
        }
        assertEquals("PARTIE PERDUE", HudText.announce(arena));
    }

    /**
     * Le panneau d'aide tient entre le haut du râtelier et le bas du bandeau.
     *
     * <p>Centré à mi-hauteur du monde, il recouvrait treize des seize pixels du râtelier —
     * c'est-à-dire les six tuiles que sa deuxième ligne numérote.
     */
    @Test
    @DisplayName("L'aide ne recouvre ni le râtelier qu'elle explique ni le bandeau d'état")
    void theHelpPanelClearsTheRackAndTheBanner() {
        int height = HudLayout.panelHeight(HudText.help().size());
        int top = HudLayout.HELP_CENTRE_Y + height / 2;
        int bottom = top - height;

        assertTrue(bottom >= HudLayout.RACK_Y + HudLayout.TILE_SIZE,
                "l'aide descend a " + bottom + " et recouvre le ratelier, qui monte a "
                        + (HudLayout.RACK_Y + HudLayout.TILE_SIZE));
        assertTrue(top <= HudLayout.BANNER_BOTTOM,
                "l'aide monte a " + top + " et recouvre le bandeau, qui descend a "
                        + HudLayout.BANNER_BOTTOM);
    }


    /**
     * {@code hoveringTheTop} : trois branches, aucune testée jusqu'ici.
     *
     * <p>Elle est pourtant pure et vit dans la classe dont le javadoc revendique de tout éprouver à
     * ce niveau. Son absence de test n'est pas anodine : c'est elle qui décide si le préavis du
     * sommet reste plein quand on le survole, et c'est autour d'elle que deux régressions de rendu
     * se sont jouées — une suppression à tort, puis un symétrique laissé ouvert.
     */
    @Test
    @DisplayName("Survoler le sommet : les trois branches")
    void hoveringTheTopHasThreeBranches() {
        Arena arena = ArenaSetup.trainingArena(9, 1);
        arena.queueTile(Tile.STRIKE);
        arena.queueTile(Tile.PUSH);

        // Le râtelier est prioritaire : tant qu'une tuile y est survolée, la file ne compte pas.
        assertFalse(HudText.hoveringTheTop(arena, 0, 1),
                "un survol de ratelier ne peut pas etre un survol du sommet");

        // Aucun emplacement de file survolé.
        assertFalse(HudText.hoveringTheTop(arena, -1, -1), "rien n'est survole");

        // Le sommet est le DERNIER posé, pas le premier affiché : c'est tout le contre-intuitif de
        // la file, et l'inverser rendrait « plein » un préavis qui doit rester creux.
        assertTrue(HudText.hoveringTheTop(arena, -1, arena.queue().size() - 1),
                "le dernier emplacement est le sommet");
        assertFalse(HudText.hoveringTheTop(arena, -1, 0),
                "le premier emplacement n'est pas le sommet quand la file en compte deux");

        // Et sur une file d'une seule tuile, l'unique emplacement EST le sommet.
        Arena single = ArenaSetup.trainingArena(9, 1);
        single.queueTile(Tile.STRIKE);
        assertTrue(HudText.hoveringTheTop(single, -1, 0),
                "l'unique emplacement d'une file d'une tuile est le sommet");
    }
}
