package com.starfall.render;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.starfall.art.ArtFormatException;
import com.starfall.art.AtlasIndex;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Le garde-fou qui empêche l'atlas et son index de se désynchroniser.
 *
 * <p>Il existait depuis le pipeline d'art et n'avait <b>jamais</b> été vérifié : il vivait au milieu
 * du chargement, entre une texture GL et un fichier, donc hors de portée sans écran. Un garde-fou
 * qu'on ne peut pas tester est un garde-fou qu'on croit sur parole.
 *
 * <p>Ce qu'il attrape : une image régénérée qui change de taille pendant que l'index livré reste
 * celui d'avant. Toutes les régions seraient décalées, et le jeu afficherait des morceaux de sprites
 * voisins — un défaut qui se lit comme un bug de rendu et qu'on cherche du mauvais côté.
 */
class SpriteAtlasTest {

    @Test
    @DisplayName("Une image et un index qui s'accordent passent sans bruit")
    void amatchingPairPassesQuietly() {
        assertDoesNotThrow(() -> SpriteAtlas.requireSameSize(256, 64, 256, 64));
    }

    /**
     * Les quatre façons de se désaccorder, et pas seulement la première : un contrôle écrit avec un
     * {@code ||} peut très bien n'en couvrir qu'une moitié.
     */
    @Test
    @DisplayName("Le moindre désaccord de taille est refusé, et nommé")
    void anyMismatchIsRefusedAndNamed() {
        int[][] mismatches = {{255, 64}, {257, 64}, {256, 63}, {256, 65}};
        for (int[] size : mismatches) {
            ArtFormatException thrown = assertThrows(ArtFormatException.class,
                    () -> SpriteAtlas.requireSameSize(size[0], size[1], 256, 64),
                    "image " + size[0] + "x" + size[1] + " acceptee contre un index 256x64");

            String message = thrown.getMessage();
            assertTrue(message.contains(size[0] + "x" + size[1]),
                    "le message tait la taille de l'image : " + message);
            assertTrue(message.contains("256x64"),
                    "le message tait la taille annoncee par l'index : " + message);
        }
    }

    /**
     * Et le couple réellement livré, vérifié <b>sans contexte graphique</b>.
     *
     * <p>Un en-tête PNG commence par une signature de huit octets, puis un bloc {@code IHDR} dont
     * les huit octets utiles sont la largeur et la hauteur en gros-boutiste. C'est tout ce qu'il faut
     * pour confronter l'image livrée à l'index livré, et ça ne demande ni écran ni décodeur.
     *
     * <p>C'est le test doré qui manquait entre {@code art/} et le jeu : le précédent vérifiait que
     * l'index déclare tous les sprites attendus, celui-ci vérifie que l'image sous cet index est bien
     * celle que l'index décrit.
     */
    @Test
    @DisplayName("L'atlas livré et son index livré s'accordent, sans ouvrir d'écran")
    void theShippedAtlasAndItsShippedIndexAgree() throws IOException {
        Path generated = locateGenerated();
        Path image = generated.resolve("starfall-atlas.png");
        Path indexFile = generated.resolve("starfall-atlas.txt");

        assertTrue(Files.isRegularFile(image), "atlas introuvable : " + image);
        AtlasIndex index = AtlasIndex.parse(indexFile.toString(),
                Files.readAllLines(indexFile, StandardCharsets.UTF_8));

        int[] size = readPngSize(Files.readAllBytes(image));
        assertDoesNotThrow(() -> SpriteAtlas.requireSameSize(size[0], size[1],
                        index.width(), index.height()),
                "l'image livree et l'index livre ne parlent pas de la meme chose");

        // Et toutes les régions doivent tenir dedans : l'index le vérifie déjà à l'analyse, mais il
        // le vérifie contre la taille qu'il DÉCLARE. Ici on le confronte à la taille réelle.
        for (String name : index.names()) {
            var placement = index.region(name);
            assertTrue(placement.x() + placement.width() <= size[0]
                            && placement.y() + placement.height() <= size[1],
                    "la region « " + name + " » deborde de l'image reelle "
                            + size[0] + "x" + size[1]);
        }
    }

    /** Largeur et hauteur d'un PNG, lues dans son bloc {@code IHDR}. */
    private static int[] readPngSize(byte[] png) {
        assertTrue(png.length > 24, "fichier trop court pour etre un PNG");
        assertEquals((byte) 0x89, png[0], "signature PNG absente");
        assertEquals('P', png[1]);
        assertEquals('N', png[2]);
        assertEquals('G', png[3]);
        return new int[]{readInt(png, 16), readInt(png, 20)};
    }

    private static int readInt(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24)
                | ((bytes[offset + 1] & 0xFF) << 16)
                | ((bytes[offset + 2] & 0xFF) << 8)
                | (bytes[offset + 3] & 0xFF);
    }

    private static Path locateGenerated() {
        Path candidate = Path.of("").toAbsolutePath();
        for (int depth = 0; depth < 5 && candidate != null; depth++) {
            Path generated = candidate.resolve("assets/generated");
            if (Files.isDirectory(generated)) {
                return generated;
            }
            candidate = candidate.getParent();
        }
        throw new AssertionError("dossier des ressources generees introuvable depuis "
                + Path.of("").toAbsolutePath());
    }
}
