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

    /**
     * Le défaut le plus grave que la review de M3 a trouvé : une région hors de l'image ne provoque
     * aucune erreur au rendu, elle fait simplement disparaître le sprite. Un index trafiqué faisait
     * ainsi disparaître le héros de la capture, en sortie 0 et sans un message.
     */
    @Test
    @DisplayName("Une région qui sort de l'atlas est refusée")
    void aRegionOutsideTheAtlasIsRejected() {
        for (String bad : List.of(
                "hero/idle 115 33 16 32",   // deborde a droite et en bas
                "hero/idle -5 1 16 32",     // hors bord gauche
                "hero/idle 1 -5 16 32",     // hors bord haut
                "hero/idle 120 1 16 32",    // deborde a droite seulement
                "hero/idle 1 40 16 32")) {  // deborde en bas seulement
            ArtFormatException error = assertThrows(ArtFormatException.class,
                    () -> AtlasIndex.parse("index", List.of("atlas 128 64", bad)),
                    "aurait du refuser : " + bad);
            assertTrue(error.getMessage().contains("en dehors de l'atlas"), error.getMessage());
        }
    }

    @Test
    @DisplayName("Une région de taille nulle ou négative est refusée")
    void aDegenerateRegionIsRejected() {
        for (String bad : List.of("a/one 1 1 0 0", "a/one 1 1 16 0", "a/one 1 1 -4 8")) {
            assertThrows(ArtFormatException.class,
                    () -> AtlasIndex.parse("index", List.of("atlas 128 64", bad)),
                    "aurait du refuser : " + bad);
        }
    }

    @Test
    @DisplayName("Deux régions qui se recouvrent sont refusées")
    void overlappingRegionsAreRejected() {
        ArtFormatException error = assertThrows(ArtFormatException.class,
                () -> AtlasIndex.parse("index", List.of(
                        "atlas 128 64",
                        "a/one 1 1 16 32",
                        "a/two 8 1 16 32")));

        assertTrue(error.getMessage().contains("recouvre"), error.getMessage());
    }

    @Test
    @DisplayName("Une seconde ligne « atlas » est refusée au lieu d'écraser la première")
    void aSecondAtlasLineIsRejected() {
        // Sans cela la derniere gagnait, et toutes les regions se retrouvaient validees contre une
        // taille qui n'etait pas celle de l'image.
        assertThrows(ArtFormatException.class,
                () -> AtlasIndex.parse("index", List.of(
                        "atlas 128 64",
                        "atlas 32 32",
                        "a/one 1 1 16 16")));
    }

    @Test
    @DisplayName("Une région déclarée avant la taille de l'atlas est refusée")
    void aRegionBeforeTheAtlasLineIsRejected() {
        assertThrows(ArtFormatException.class,
                () -> AtlasIndex.parse("index", List.of("a/one 1 1 16 16", "atlas 128 64")));
    }

    @Test
    @DisplayName("Tout ce que le générateur produit se relit sans erreur")
    void whateverThePackerProducesIsAccepted() {
        // Garde-fou des validations ci-dessus : elles doivent refuser les index corrompus sans
        // jamais refuser un index legitime.
        for (int count = 1; count <= 40; count++) {
            List<SpriteSource> sprites = new java.util.ArrayList<>();
            for (int i = 0; i < count; i++) {
                sprites.add(sprite("s/" + i, 1 + (i * 7) % 40, 1 + (i * 13) % 60));
            }
            AtlasIndex written = AtlasIndex.of(AtlasLayout.pack(sprites, 256, 1024));

            AtlasIndex read = AtlasIndex.parse("relu", Arrays.asList(written.render().split("\\R")));
            assertEquals(written.names().size(), read.names().size(), "avec " + count + " sprites");
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
