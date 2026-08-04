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

    private static List<NamedColor> hudColors() {
        return List.of(
                new NamedColor("QUEUE", HudColors.QUEUE),
                new NamedColor("SLOT_EMPTY", HudColors.SLOT_EMPTY),
                new NamedColor("SLOT_OUTLINE", HudColors.SLOT_OUTLINE),
                new NamedColor("DIMMED", HudColors.DIMMED),
                new NamedColor("RECHARGE", HudColors.RECHARGE),
                new NamedColor("HOVER", HudColors.HOVER));
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
        for (NamedColor hud : hudColors()) {
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
        List<NamedColor> colors = hudColors();
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
}
