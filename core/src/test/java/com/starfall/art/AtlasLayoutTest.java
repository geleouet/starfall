package com.starfall.art;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests du rangement des sprites dans l'atlas. */
class AtlasLayoutTest {

    private static SpriteSource sprite(String name, int width, int height) {
        return new SpriteSource(name, width, height, new int[width * height]);
    }

    private static List<SpriteSource> sampleSprites() {
        return new ArrayList<>(List.of(
                sprite("hero/idle", 16, 32),
                sprite("enemy/melee", 16, 32),
                sprite("tile/slash", 16, 16),
                sprite("tile/empty", 16, 16),
                sprite("ground/plain", 16, 8)));
    }

    @Test
    @DisplayName("Aucun sprite ne chevauche un autre")
    void placementsNeverOverlap() {
        AtlasLayout layout = AtlasLayout.pack(sampleSprites(), 64);

        List<AtlasLayout.Placement> placements = layout.placements();
        for (int i = 0; i < placements.size(); i++) {
            for (int j = i + 1; j < placements.size(); j++) {
                assertTrue(!placements.get(i).overlaps(placements.get(j)),
                        placements.get(i) + " chevauche " + placements.get(j));
            }
        }
    }

    @Test
    @DisplayName("Tout tient dans les dimensions annoncées, marge comprise")
    void everythingFitsInsideTheAtlas() {
        AtlasLayout layout = AtlasLayout.pack(sampleSprites(), 64);

        for (AtlasLayout.Placement placement : layout.placements()) {
            assertTrue(placement.x() >= AtlasLayout.PADDING, () -> "marge gauche manquante : " + placement);
            assertTrue(placement.y() >= AtlasLayout.PADDING, () -> "marge haute manquante : " + placement);
            assertTrue(placement.right() + AtlasLayout.PADDING <= layout.width(),
                    () -> placement + " deborde a droite de " + layout);
            assertTrue(placement.bottom() + AtlasLayout.PADDING <= layout.height(),
                    () -> placement + " deborde en bas de " + layout);
        }
    }

    @Test
    @DisplayName("Chaque sprite est présent une fois et garde sa taille")
    void everySpriteIsPlacedOnceAtItsSize() {
        AtlasLayout layout = AtlasLayout.pack(sampleSprites(), 512);

        assertEquals(5, layout.placements().size());
        for (AtlasLayout.Placement placement : layout.placements()) {
            SpriteSource expected = sampleSprites().stream()
                    .filter(s -> s.name().equals(placement.name()))
                    .findFirst()
                    .orElseThrow();
            assertEquals(expected.width(), placement.width());
            assertEquals(expected.height(), placement.height());
        }
    }

    /**
     * La propriété qui compte le plus pour la boucle de review : mêmes sources, même atlas. Sans
     * cela chaque build ferait bouger les captures et un vrai changement visuel se noierait dans le
     * bruit.
     */
    @Test
    @DisplayName("Le rangement ne dépend pas de l'ordre d'entrée")
    void packingIsDeterministicWhateverTheInputOrder() {
        AtlasLayout reference = AtlasLayout.pack(sampleSprites(), 64);

        for (int seed = 0; seed < 20; seed++) {
            List<SpriteSource> shuffled = sampleSprites();
            Collections.rotate(shuffled, seed);
            Collections.reverse(shuffled.subList(0, 1 + seed % shuffled.size()));

            AtlasLayout other = AtlasLayout.pack(shuffled, 64);
            assertEquals(reference.placements(), other.placements(), "ordre d'entree " + seed);
            assertEquals(reference.width(), other.width());
            assertEquals(reference.height(), other.height());
        }
    }

    @Test
    @DisplayName("Les dimensions sont des puissances de deux")
    void dimensionsArePowersOfTwo() {
        AtlasLayout layout = AtlasLayout.pack(sampleSprites(), 64);

        assertEquals(0, layout.width() & (layout.width() - 1), "largeur " + layout.width());
        assertEquals(0, layout.height() & (layout.height() - 1), "hauteur " + layout.height());
    }

    @Test
    @DisplayName("Un sprite plus large que l'atlas est refusé plutôt que tronqué")
    void aSpriteWiderThanTheAtlasIsRejected() {
        ArtFormatException error = assertThrows(ArtFormatException.class,
                () -> AtlasLayout.pack(List.of(sprite("huge/thing", 100, 8)), 64));

        assertTrue(error.getMessage().contains("huge/thing"), error.getMessage());
    }

    @Test
    @DisplayName("Une liste vide est refusée")
    void anEmptyListIsRejected() {
        assertThrows(ArtFormatException.class, () -> AtlasLayout.pack(List.of(), 64));
    }

    @Test
    @DisplayName("Le passage à l'étagère suivante se fait quand la ligne est pleine")
    void spritesWrapOntoTheNextShelf() {
        // Quatre sprites de 16 px plus leur marge font 72 px : le quatrieme ne tient pas sur 64.
        AtlasLayout layout = AtlasLayout.pack(sampleSprites(), 64);

        long distinctRows = layout.placements().stream().map(AtlasLayout.Placement::y).distinct().count();
        assertTrue(distinctRows >= 2, "tout est reste sur une seule etagere : " + layout.placements());
    }
}
