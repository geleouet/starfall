package com.starfall.art;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests de l'étape de build complète : des fichiers texte au PNG.
 *
 * <p>C'est le seul endroit qui vérifie que le pixel écrit dans la source se retrouve bien, à la
 * bonne place et de la bonne couleur, dans l'image produite. Les tests des autres classes ne
 * couvrent chacun qu'un maillon.
 */
class AtlasBuilderTest {

    private static final List<String> PALETTE = List.of(
            "# palette de test",
            "k noir 0d0b14",
            "w blanc f2ece0",
            "r rouge d4453f");

    private static void writeArt(Path artDir, String spriteFileName, List<String> spriteLines) throws IOException {
        Files.createDirectories(artDir.resolve("sprites"));
        Files.write(artDir.resolve(AtlasBuilder.PALETTE_FILE), PALETTE, StandardCharsets.UTF_8);
        Files.write(artDir.resolve("sprites").resolve(spriteFileName), spriteLines, StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("Chaque pixel de la source se retrouve à sa place dans le PNG")
    void everySourcePixelReachesTheImage(@TempDir Path dir) throws IOException {
        Path artDir = dir.resolve("art");
        writeArt(artDir, "test.sprite", List.of(
                "@sprite a/flag",
                "kw.",
                "rrr"));

        AtlasBuilder.Result result = AtlasBuilder.build(artDir, dir.resolve("out"));
        BufferedImage image = ImageIO.read(result.image().toFile());
        AtlasIndex index = AtlasIndex.parse("index",
                Files.readAllLines(result.index(), StandardCharsets.UTF_8));
        AtlasLayout.Placement placement = index.region("a/flag");

        assertEquals(3, placement.width());
        assertEquals(2, placement.height());

        // La ligne 0 de la source est la ligne du HAUT de l'image : le PNG et le fichier texte se
        // lisent dans le meme sens, ce qui rend une source relisable a cote de sa capture.
        assertEquals(0xFF0d0b14, image.getRGB(placement.x(), placement.y()));
        assertEquals(0xFFf2ece0, image.getRGB(placement.x() + 1, placement.y()));
        assertEquals(0x00000000, image.getRGB(placement.x() + 2, placement.y()), "le point doit rester transparent");
        assertEquals(0xFFd4453f, image.getRGB(placement.x(), placement.y() + 1));
        assertEquals(0xFFd4453f, image.getRGB(placement.x() + 2, placement.y() + 1));
    }

    @Test
    @DisplayName("La marge autour d'un sprite est transparente")
    void thePaddingAroundASpriteIsTransparent(@TempDir Path dir) throws IOException {
        Path artDir = dir.resolve("art");
        writeArt(artDir, "test.sprite", List.of("@sprite a/block", "kk", "kk"));

        AtlasBuilder.Result result = AtlasBuilder.build(artDir, dir.resolve("out"));
        BufferedImage image = ImageIO.read(result.image().toFile());
        AtlasIndex index = AtlasIndex.parse("index",
                Files.readAllLines(result.index(), StandardCharsets.UTF_8));
        AtlasLayout.Placement placement = index.region("a/block");

        for (int x = placement.x() - 1; x <= placement.right(); x++) {
            assertEquals(0, image.getRGB(x, placement.y() - 1), "marge haute en x=" + x);
            assertEquals(0, image.getRGB(x, placement.bottom()), "marge basse en x=" + x);
        }
        for (int y = placement.y() - 1; y <= placement.bottom(); y++) {
            assertEquals(0, image.getRGB(placement.x() - 1, y), "marge gauche en y=" + y);
            assertEquals(0, image.getRGB(placement.right(), y), "marge droite en y=" + y);
        }
    }

    /**
     * La propriété qui protège la boucle de review : deux builds sur les mêmes sources doivent
     * donner le même fichier, octet pour octet. Sinon chaque build ferait bouger les captures.
     */
    @Test
    @DisplayName("Deux builds identiques produisent le même octet")
    void theOutputIsByteForByteReproducible(@TempDir Path dir) throws IOException {
        Path artDir = dir.resolve("art");
        writeArt(artDir, "test.sprite", List.of(
                "@sprite b/second", "kw", "wk",
                "@sprite a/first", "rrrr", "rkkr", "rrrr"));

        AtlasBuilder.Result first = AtlasBuilder.build(artDir, dir.resolve("out1"));
        AtlasBuilder.Result second = AtlasBuilder.build(artDir, dir.resolve("out2"));

        assertArrayEquals(Files.readAllBytes(first.image()), Files.readAllBytes(second.image()),
                "le PNG a change entre deux builds identiques");
        assertEquals(Files.readString(first.index()), Files.readString(second.index()));
    }

    @Test
    @DisplayName("Un sprite défini dans deux fichiers est refusé")
    void aSpriteDefinedTwiceIsRejected(@TempDir Path dir) throws IOException {
        Path artDir = dir.resolve("art");
        writeArt(artDir, "one.sprite", List.of("@sprite a/dup", "kk"));
        Files.write(artDir.resolve("sprites").resolve("two.sprite"),
                List.of("@sprite a/dup", "ww"), StandardCharsets.UTF_8);

        ArtFormatException error = assertThrows(ArtFormatException.class,
                () -> AtlasBuilder.build(artDir, dir.resolve("out")));
        assertTrue(error.getMessage().contains("a/dup"), error.getMessage());
    }

    @Test
    @DisplayName("Une palette absente est signalée clairement")
    void aMissingPaletteIsReported(@TempDir Path dir) throws IOException {
        Path artDir = dir.resolve("art");
        Files.createDirectories(artDir.resolve("sprites"));
        Files.write(artDir.resolve("sprites").resolve("test.sprite"),
                List.of("@sprite a/one", "kk"), StandardCharsets.UTF_8);

        ArtFormatException error = assertThrows(ArtFormatException.class,
                () -> AtlasBuilder.build(artDir, dir.resolve("out")));
        assertTrue(error.getMessage().contains(AtlasBuilder.PALETTE_FILE), error.getMessage());
    }

    @Test
    @DisplayName("Un dossier sans aucun sprite est refusé")
    void anArtDirectoryWithoutSpritesIsRejected(@TempDir Path dir) throws IOException {
        Path artDir = dir.resolve("art");
        Files.createDirectories(artDir);
        Files.write(artDir.resolve(AtlasBuilder.PALETTE_FILE), PALETTE, StandardCharsets.UTF_8);

        assertThrows(ArtFormatException.class, () -> AtlasBuilder.build(artDir, dir.resolve("out")));
    }

    /**
     * Retrouve le dossier {@code art/} en remontant depuis le répertoire de travail.
     *
     * <p>La version précédente sortait en silence quand le dossier n'était pas là où elle
     * l'attendait : le test restait vert tout en ne vérifiant rien, selon le répertoire depuis
     * lequel on lançait Gradle.
     */
    private static Path locateArtDirectory() {
        Path candidate = Path.of("").toAbsolutePath();
        for (int depth = 0; depth < 5 && candidate != null; depth++) {
            Path art = candidate.resolve("art");
            if (Files.isDirectory(art) && Files.isRegularFile(art.resolve(AtlasBuilder.PALETTE_FILE))) {
                return art;
            }
            candidate = candidate.getParent();
        }
        throw new AssertionError("dossier art/ introuvable depuis " + Path.of("").toAbsolutePath()
                + " : ce test doit verifier les sources reelles, pas se taire");
    }

    @Test
    @DisplayName("Les sprites réels du jeu passent le pipeline")
    void theRealGameArtBuilds(@TempDir Path dir) throws IOException {
        AtlasBuilder.Result result = AtlasBuilder.build(locateArtDirectory(), dir.resolve("out"));

        assertTrue(result.spriteCount() >= 5, "sprites trouves : " + result.spriteCount());
        AtlasIndex index = AtlasIndex.parse("index",
                Files.readAllLines(result.index(), StandardCharsets.UTF_8));
        for (String expected : List.of("hero/idle", "enemy/melee", "tile/slash", "tile/empty", "ground/plain")) {
            assertTrue(index.contains(expected), "sprite manquant dans l'atlas : " + expected);
        }

        // Les personnages tiennent le gabarit verrouille au cadrage.
        assertEquals(16, index.region("hero/idle").width());
        assertEquals(32, index.region("hero/idle").height());
        assertEquals(16, index.region("enemy/melee").width());
        assertEquals(32, index.region("enemy/melee").height());

        // Et le heros n'est pas une silhouette vide : il doit rester des pixels opaques.
        BufferedImage image = ImageIO.read(result.image().toFile());
        AtlasLayout.Placement hero = index.region("hero/idle");
        int opaque = 0;
        for (int y = 0; y < hero.height(); y++) {
            for (int x = 0; x < hero.width(); x++) {
                if ((image.getRGB(hero.x() + x, hero.y() + y) >>> 24) == 0xFF) {
                    opaque++;
                }
            }
        }
        assertTrue(opaque > hero.width() * hero.height() / 4,
                "le heros n'a que " + opaque + " pixels opaques sur " + (hero.width() * hero.height()));
    }

    /**
     * Le test de reproductibilité enchaîne deux builds à quelques millisecondes d'intervalle : un
     * horodatage au format PNG ({@code tIME}, résolution d'une seconde) le passerait sans broncher.
     * L'invariant réel, c'est qu'aucune métadonnée variable ne soit écrite du tout.
     */
    @Test
    @DisplayName("Le PNG ne contient aucune métadonnée variable")
    void thePngCarriesNoVariableMetadata(@TempDir Path dir) throws IOException {
        Path artDir = dir.resolve("art");
        writeArt(artDir, "test.sprite", List.of("@sprite a/one", "kw", "wk"));

        AtlasBuilder.Result result = AtlasBuilder.build(artDir, dir.resolve("out"));
        List<String> chunks = pngChunkNames(Files.readAllBytes(result.image()));

        assertEquals(List.of("IHDR", "IDAT", "IEND"), chunks,
                "le PNG contient des blocs inattendus : " + chunks);
    }

    /** Liste les noms de blocs d'un PNG, dans l'ordre du fichier. */
    private static List<String> pngChunkNames(byte[] png) {
        List<String> names = new java.util.ArrayList<>();
        int offset = 8; // signature PNG
        while (offset + 8 <= png.length) {
            int length = ((png[offset] & 0xFF) << 24) | ((png[offset + 1] & 0xFF) << 16)
                    | ((png[offset + 2] & 0xFF) << 8) | (png[offset + 3] & 0xFF);
            names.add(new String(png, offset + 4, 4, java.nio.charset.StandardCharsets.US_ASCII));
            offset += 12 + length; // longueur + type + donnees + CRC
        }
        return names;
    }

    @Test
    @DisplayName("Un fichier source mal encodé nomme le fichier fautif")
    void aBadlyEncodedSourceNamesTheFile(@TempDir Path dir) throws IOException {
        Path artDir = dir.resolve("art");
        writeArt(artDir, "good.sprite", List.of("@sprite a/one", "kk"));
        // Octet 0xFF isole : invalide en UTF-8.
        Files.write(artDir.resolve("sprites").resolve("bad.sprite"),
                new byte[]{'@', 's', 'p', 'r', 'i', 't', 'e', ' ', 'a', '/', 'b', '\n', (byte) 0xFF});

        ArtFormatException error = assertThrows(ArtFormatException.class,
                () -> AtlasBuilder.build(artDir, dir.resolve("out")));

        assertTrue(error.getMessage().contains("bad.sprite"), error.getMessage());
        assertTrue(error.getMessage().contains("UTF-8"), error.getMessage());
    }

    @Test
    @DisplayName("Deux sources différentes donnent deux atlas différents")
    void differentSourcesGiveDifferentAtlases(@TempDir Path dir) throws IOException {
        // Garde-fou du test de reproductibilite : sans lui, un generateur qui ecrirait toujours la
        // meme image vide passerait « reproductible » haut la main.
        Path artA = dir.resolve("artA");
        Path artB = dir.resolve("artB");
        writeArt(artA, "test.sprite", List.of("@sprite a/one", "kk", "kk"));
        writeArt(artB, "test.sprite", List.of("@sprite a/one", "ww", "ww"));

        AtlasBuilder.Result a = AtlasBuilder.build(artA, dir.resolve("outA"));
        AtlasBuilder.Result b = AtlasBuilder.build(artB, dir.resolve("outB"));

        assertNotEquals(
                Arrays.hashCode(Files.readAllBytes(a.image())),
                Arrays.hashCode(Files.readAllBytes(b.image())));
    }
}
