package com.starfall.scene;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.Color;
import com.starfall.art.Palette;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Une couleur d'interface ne doit jamais être une couleur de la palette d'art.
 *
 * <p>La règle affichée au joueur est « vert = ne consomme pas de tour ». Le repère de disponibilité
 * du râtelier utilisait exactement le vert de la palette : au repos, les six tuiles portaient donc
 * un trait vert, et un joueur cherchant le vert trouvait six réponses au lieu de deux.
 *
 * <p>Ce test relit {@code art/palette.txt} et refuse toute coïncidence. Trois lignes qui auraient
 * suffi à épingler le défaut.
 */
class HudColorsTest {

    private static Path locateArtDirectory() {
        Path candidate = Path.of("").toAbsolutePath();
        for (int depth = 0; depth < 5 && candidate != null; depth++) {
            Path art = candidate.resolve("art");
            if (Files.isRegularFile(art.resolve("palette.txt"))) {
                return art;
            }
            candidate = candidate.getParent();
        }
        throw new AssertionError("palette introuvable depuis " + Path.of("").toAbsolutePath());
    }

    private record NamedColor(String name, Color color) {
    }

    /**
     * Les couleurs de <b>signal</b> : celles qui veulent dire quelque chose.
     *
     * <p>{@code THREAT} manquait à cette liste, et c'est une omission qui aurait fini par coûter :
     * c'est la couleur la plus chargée de sens du jeu — tout ce qui va faire mal — et elle vivait à
     * côté du rouge vif de la palette sans que rien ne le vérifie.
     */
    private static List<NamedColor> signalColors() {
        return List.of(
                new NamedColor("QUEUE", HudColors.QUEUE),
                new NamedColor("SLOT_EMPTY", HudColors.SLOT_EMPTY),
                new NamedColor("SLOT_OUTLINE", HudColors.SLOT_OUTLINE),
                new NamedColor("DIMMED", HudColors.DIMMED),
                new NamedColor("RECHARGE", HudColors.RECHARGE),
                new NamedColor("HOVER", HudColors.HOVER),
                new NamedColor("THREAT", HudColors.THREAT),
                new NamedColor("PREVIEW", HudColors.PREVIEW));
    }

    /**
     * Les couleurs de <b>surface</b> : fonds et bords de panneaux, textes.
     *
     * <p>Elles suivent une règle différente, et pas par facilité. Une surface n'a pas à se
     * distinguer de la palette d'art — les noirs de la palette sont des noirs, et un panneau doit
     * pouvoir être sombre. Ce dont elle doit se distinguer, c'est de <em>tous les signaux</em>,
     * faute de quoi un repère posé sur un panneau y disparaît.
     */
    private static List<NamedColor> surfaceColors() {
        return List.of(
                new NamedColor("PANEL", HudColors.PANEL),
                new NamedColor("PANEL_EDGE", HudColors.PANEL_EDGE),
                new NamedColor("TEXT", HudColors.TEXT),
                new NamedColor("TEXT_DIM", HudColors.TEXT_DIM));
    }

    /**
     * Écart minimal exigé, en somme des différences par canal sur 255.
     *
     * <p>Une simple inégalité de bits ne prouverait rien : deux couleurs distantes d'une unité sont
     * indiscernables à l'œil et poseraient exactement le même problème. Le seuil est modeste — il
     * s'agit d'attraper la confusion, pas d'imposer un contraste.
     */
    private static final int MIN_DISTANCE = 40;

    private static int distance(int rgbaA, int rgbaB) {
        int dr = Math.abs(((rgbaA >>> 24) & 0xFF) - ((rgbaB >>> 24) & 0xFF));
        int dg = Math.abs(((rgbaA >>> 16) & 0xFF) - ((rgbaB >>> 16) & 0xFF));
        int db = Math.abs(((rgbaA >>> 8) & 0xFF) - ((rgbaB >>> 8) & 0xFF));
        return dr + dg + db;
    }

    @Test
    @DisplayName("Aucune couleur de rôle ne se confond avec une couleur de famille de la palette")
    void noRoleColourCollidesWithTheArtPalette() throws IOException {
        Path paletteFile = locateArtDirectory().resolve("palette.txt");
        Palette palette = Palette.parse(paletteFile.toString(),
                Files.readAllLines(paletteFile, StandardCharsets.UTF_8));

        List<String> collisions = new ArrayList<>();
        for (NamedColor hud : signalColors()) {
            int rgba = Color.rgba8888(hud.color());
            for (char code : palette.codes()) {
                int distance = distance(rgba, palette.color(code, paletteFile.toString(), 0));
                if (distance < MIN_DISTANCE) {
                    collisions.add(hud.name() + " est trop proche de « " + code + " » ("
                            + palette.name(code) + ", écart " + distance + ")");
                }
            }
        }

        assertTrue(collisions.isEmpty(), "collisions de couleurs : " + collisions);
    }

    @Test
    @DisplayName("Les couleurs de rôle se distinguent entre elles")
    void roleColoursDifferFromEachOther() {
        List<NamedColor> colors = signalColors();
        for (int i = 0; i < colors.size(); i++) {
            for (int j = i + 1; j < colors.size(); j++) {
                int distance = distance(Color.rgba8888(colors.get(i).color()),
                        Color.rgba8888(colors.get(j).color()));
                assertTrue(distance >= MIN_DISTANCE,
                        colors.get(i).name() + " et " + colors.get(j).name()
                                + " sont trop proches (écart " + distance + ")");
            }
        }
    }

    /**
     * Un signal posé sur un panneau doit rester lisible.
     *
     * <p>Le cas concret : la ligne d'annonce et les infobulles vivent sur un panneau opaque, et
     * c'est là que s'écrivent les mots qui portent les couleurs de rôle. Un fond choisi trop près
     * d'un signal ferait disparaître ce signal <em>seulement</em> quand il tombe sur un panneau —
     * un défaut qui n'apparaît que dans certaines situations de jeu.
     */
    @Test
    @DisplayName("Aucune surface de panneau ne se confond avec une couleur de signal")
    void noPanelSurfaceCollidesWithASignal() {
        List<String> collisions = new ArrayList<>();
        for (NamedColor surface : surfaceColors()) {
            for (NamedColor signal : signalColors()) {
                int distance = distance(Color.rgba8888(surface.color()),
                        Color.rgba8888(signal.color()));
                if (distance < MIN_DISTANCE) {
                    collisions.add(surface.name() + " est trop proche du signal " + signal.name()
                            + " (écart " + distance + ")");
                }
            }
        }
        assertTrue(collisions.isEmpty(), "collisions surface/signal : " + collisions);
    }

    /**
     * Un signal doit trancher sur le fond où il est <b>réellement</b> dessiné.
     *
     * <p>C'est le test qui manquait, et son absence a laissé passer un défaut visible : le repère
     * « ce coup ne portera pas » — la promesse-titre du jalon de l'interface — était peint en
     * {@code DIMMED} sur le fond de la fosse, soit un rectangle de trois pixels de haut à
     * {@code 0x3a3a44} sur {@code 0x0c101e}. Il était pourtant bien distinct de toutes les autres
     * couleurs d'interface et de toute la palette d'art : il se confondait avec le <em>décor</em>,
     * et rien ne regardait le décor.
     *
     * <p>Les trois bandes vivaient en constantes privées de la scène, donc hors de portée de tout
     * test. Elles ont déménagé dans {@link HudColors} pour cette seule raison.
     */
    @Test
    @DisplayName("Chaque signal tranche sur les fonds de la scène")
    void everySignalStandsOutAgainstTheSceneBackdrops() {
        List<NamedColor> backdrops = List.of(
                new NamedColor("SKY", HudColors.SKY),
                new NamedColor("WALL", HudColors.WALL),
                new NamedColor("PIT", HudColors.PIT));

        List<String> collisions = new ArrayList<>();
        for (NamedColor signal : signalColors()) {
            for (NamedColor backdrop : backdrops) {
                int distance = distance(Color.rgba8888(signal.color()),
                        Color.rgba8888(backdrop.color()));
                if (distance < MIN_DISTANCE) {
                    collisions.add(signal.name() + " disparait sur " + backdrop.name()
                            + " (écart " + distance + ")");
                }
            }
        }
        assertTrue(collisions.isEmpty(), "signaux illisibles : " + collisions);
    }

    /**
     * Le texte secondaire doit se distinguer du texte principal, sinon la hiérarchie qu'il est censé
     * porter n'existe pas.
     */
    @Test
    @DisplayName("Le texte secondaire se distingue du texte principal et du fond")
    void secondaryTextIsDistinguishable() {
        assertTrue(distance(Color.rgba8888(HudColors.TEXT), Color.rgba8888(HudColors.TEXT_DIM))
                        >= MIN_DISTANCE,
                "TEXT et TEXT_DIM sont trop proches");
        assertTrue(distance(Color.rgba8888(HudColors.TEXT_DIM), Color.rgba8888(HudColors.PANEL))
                        >= MIN_DISTANCE,
                "TEXT_DIM disparaitrait sur le fond des panneaux");
    }
}
