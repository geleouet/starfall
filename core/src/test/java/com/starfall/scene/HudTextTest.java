package com.starfall.scene;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.starfall.StarfallGame;
import com.starfall.game.ActionResult;
import com.starfall.game.Arena;
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
     * Largeur utile d'une ligne de panneau, en pixels-monde : la zone garantie, moins la marge
     * gauche du panneau et ses deux marges intérieures.
     */
    private static final int PANEL_TEXT_WIDTH =
            StarfallGame.MIN_WORLD_WIDTH - 8 - 2 * HudLayout.PANEL_PADDING;

    /** Largeur utile du bandeau : la zone garantie moins ses deux marges. */
    private static final int BANNER_WIDTH = StarfallGame.MIN_WORLD_WIDTH - 8;

    /** Toutes les phrases possibles d'un état de partie donné, avec le nom de leur usage. */
    private static void collect(Arena arena, Set<String> panel, Set<String> banner) {
        banner.add(HudText.banner(arena));
        panel.add(HudText.announce(arena));
        for (ActionResult result : ActionResult.values()) {
            panel.add(HudText.lastAction(result));
        }
        for (Tile tile : arena.rack().tiles()) {
            panel.addAll(HudText.rackTooltip(arena, tile));
        }
        List<Tile> queued = arena.queue().fromOldest();
        for (int index = 0; index < queued.size(); index++) {
            panel.addAll(HudText.queueTooltip(queued, index));
        }
        if (arena.isOver()) {
            panel.addAll(HudText.outcome(arena));
        }
    }

    /** Le corpus complet des phrases que l'interface peut produire. */
    private record Corpus(Set<String> panel, Set<String> banner) {
        Set<String> all() {
            Set<String> everything = new LinkedHashSet<>(panel);
            everything.addAll(banner);
            return everything;
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
                    case 1 -> arena.executeTop();
                    case 2 -> arena.swapWithTarget();
                    default -> {
                        List<Tile> rack = arena.rack().tiles();
                        arena.queueTile(rack.get(random.nextInt(rack.size())));
                    }
                }
            }
            collect(arena, panel, banner);
        }
        // Le bandeau se mesure à part : il partage sa ligne avec le rappel de la touche d'aide.
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
    @DisplayName("Aucune phrase de panneau ne déborde de la zone garantie")
    void noPanelLineOverflowsTheGuaranteedArea() {
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
     */
    @Test
    @DisplayName("Le bandeau et le rappel d'aide ne se chevauchent jamais")
    void theBannerAndTheHelpHintNeverOverlap() {
        int hint = PixelFont.widthOf(HudText.HELP_HINT, 1);

        List<String> failures = new ArrayList<>();
        for (String line : gather().banner()) {
            int total = PixelFont.widthOf(line, 1) + 6 + hint;
            if (total > BANNER_WIDTH) {
                failures.add("« " + line + " » + rappel = " + total + " px pour " + BANNER_WIDTH);
            }
        }

        assertTrue(failures.isEmpty(), "le bandeau deborde :\n  " + String.join("\n  ", failures));
    }

    /**
     * Le panneau d'information grandit vers le bas depuis un bord fixe. S'il descend trop, il
     * recouvre les glyphes d'intention — c'est-à-dire qu'une infobulle cacherait le télégraphe,
     * exactement le genre d'occultation que les jalons précédents ont payée deux fois.
     */
    @Test
    @DisplayName("Le panneau d'information ne mord jamais sur les glyphes d'intention")
    void theInfoPanelNeverCoversTheIntentionGlyphs() {
        assertTrue(HudLayout.infoPanelBottom(HudLayout.MAX_INFO_LINES)
                        > com.starfall.game.ArenaLayout.INTENT_TOP,
                "le panneau descend jusqu'a " + HudLayout.infoPanelBottom(HudLayout.MAX_INFO_LINES)
                        + ", les glyphes montent a " + com.starfall.game.ArenaLayout.INTENT_TOP);
        assertTrue(HudLayout.INFO_TOP + 1 <= HudLayout.BANNER_BOTTOM,
                "le panneau d'information mord sur le bandeau d'etat");
        assertTrue(HudLayout.BANNER_TOP <= StarfallGame.MIN_WORLD_HEIGHT,
                "le bandeau d'etat sort de la zone garantie");
    }

    /**
     * Toutes les infobulles font le même nombre de lignes, et ce nombre est celui que la géométrie
     * autorise. Une quatrième ligne ajoutée un jour sans y penser sortirait du panneau.
     */
    @Test
    @DisplayName("Les infobulles tiennent dans le nombre de lignes prévu")
    void tooltipsFitTheAllowedLineCount() {
        Arena arena = ArenaSetup.trainingArena(9);
        arena.queueTile(Tile.STRIKE);
        arena.queueTile(Tile.PUSH);

        for (Tile tile : arena.rack().tiles()) {
            assertTrue(HudText.rackTooltip(arena, tile).size() <= HudLayout.MAX_INFO_LINES,
                    "infobulle de " + tile + " trop haute");
        }
        List<Tile> queued = arena.queue().fromOldest();
        for (int index = 0; index < queued.size(); index++) {
            assertTrue(HudText.queueTooltip(queued, index).size() <= HudLayout.MAX_INFO_LINES,
                    "infobulle de file " + index + " trop haute");
        }
        assertTrue(HudText.outcome(arena).size() <= HudLayout.MAX_INFO_LINES);
    }
}
