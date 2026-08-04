package com.starfall.art;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests de l'index d'atlas, le contrat entre l'étape de build et le jeu. */
class AtlasIndexTest {

    private static SpriteSource sprite(String name, int width, int height) {
        return new SpriteSource(name, width, height, new int[width * height]);
    }

    private static AtlasIndex sampleIndex() {
        return AtlasIndex.of(AtlasLayout.pack(List.of(
                sprite("hero/idle", 16, 32),
                sprite("tile/slash", 16, 16),
                sprite("ground/plain", 16, 8)), 64));
    }

    @Test
    @DisplayName("Ce qui est écrit est relu à l'identique")
    void writingThenReadingGivesTheSameIndex() {
        AtlasIndex written = sampleIndex();

        AtlasIndex read = AtlasIndex.parse("relu", Arrays.asList(written.render().split("\\R")));

        assertEquals(written.width(), read.width());
        assertEquals(written.height(), read.height());
        assertEquals(written.names(), read.names());
        for (String name : written.names()) {
            assertEquals(written.region(name), read.region(name), name);
        }
    }

    @Test
    @DisplayName("Un sprite inconnu est signalé, pas silencieusement absent")
    void anUnknownRegionIsReported() {
        AtlasIndex index = sampleIndex();

        ArtFormatException error = assertThrows(ArtFormatException.class, () -> index.region("hero/attack"));
        assertTrue(error.getMessage().contains("hero/attack"), error.getMessage());
        assertTrue(error.getMessage().contains("hero/idle"), "le message doit lister les noms connus");
    }

    @Test
    @DisplayName("Une taille d'atlas manquante est refusée")
    void aMissingAtlasLineIsRejected() {
        assertThrows(ArtFormatException.class,
                () -> AtlasIndex.parse("index", List.of("hero/idle 1 1 16 32")));
    }

    @Test
    @DisplayName("Un index sans région est refusé")
    void anIndexWithoutRegionsIsRejected() {
        assertThrows(ArtFormatException.class, () -> AtlasIndex.parse("index", List.of("atlas 64 64")));
    }

    @Test
    @DisplayName("Une région en double est refusée")
    void aDuplicateRegionIsRejected() {
        ArtFormatException error = assertThrows(ArtFormatException.class,
                () -> AtlasIndex.parse("index", List.of(
                        "atlas 64 64",
                        "hero/idle 1 1 16 32",
                        "hero/idle 20 1 16 32")));

        assertTrue(error.getMessage().startsWith("index:3:"), error.getMessage());
    }

    @Test
    @DisplayName("Une ligne mal formée est refusée avec sa ligne")
    void aMalformedLineIsRejected() {
        for (String bad : List.of("hero/idle 1 1 16", "hero/idle 1 1 16 32 4", "hero/idle a 1 16 32")) {
            ArtFormatException error = assertThrows(ArtFormatException.class,
                    () -> AtlasIndex.parse("index", List.of("atlas 64 64", bad)),
                    "aurait du refuser : " + bad);
            assertTrue(error.getMessage().startsWith("index:2:"), error.getMessage());
        }
    }

    @Test
    @DisplayName("Commentaires et lignes vides sont ignorés")
    void commentsAndBlankLinesAreIgnored() {
        AtlasIndex index = AtlasIndex.parse("index", List.of(
                "# entete",
                "",
                "atlas 64 64",
                "hero/idle 1 1 16 32"));

        assertEquals(64, index.width());
        assertEquals(List.of("hero/idle"), index.names());
    }
}
