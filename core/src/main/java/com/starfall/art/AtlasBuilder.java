package com.starfall.art;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnmappableCharacterException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

/**
 * Étape de build : convertit les sources texte de {@code art/} en un atlas PNG et son index.
 *
 * <p>C'est la moitié « outil » du pipeline verrouillé au cadrage. Les sources restent la vérité —
 * versionnables, relisables, modifiables entre deux passes de review — et le PNG n'est qu'un produit
 * dérivé, régénéré à chaque build et jamais édité à la main.
 *
 * <p>Cette classe utilise {@code java.awt} et {@code ImageIO}, jamais libGDX : elle tourne au build,
 * hors de tout contexte graphique.
 *
 * <p>La sortie est <b>déterministe</b> : mêmes sources, même PNG octet pour octet. La boucle de
 * review compare des captures, donc un atlas qui bougerait à chaque build rendrait tout diff
 * d'image illisible.
 */
public final class AtlasBuilder {

    /** Largeur maximale de l'atlas, en pixels. */
    public static final int MAX_ATLAS_WIDTH = 512;
    /**
     * Hauteur maximale de l'atlas, en pixels. Volontairement bien en dessous de la taille de
     * texture minimale garantie par OpenGL (2048), pour échouer au build plutôt qu'au chargement.
     */
    public static final int MAX_ATLAS_HEIGHT = 1024;

    public static final String PALETTE_FILE = "palette.txt";
    public static final String SPRITE_EXTENSION = ".sprite";
    public static final String ATLAS_IMAGE = "starfall-atlas.png";
    public static final String ATLAS_INDEX = "starfall-atlas.txt";

    private AtlasBuilder() {
    }

    /**
     * @param args {@code <dossier source> <dossier de sortie>}
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("usage : AtlasBuilder <dossier art> <dossier de sortie>");
            System.exit(2);
            return;
        }
        try {
            Result result = build(Path.of(args[0]), Path.of(args[1]));
            System.out.println("[atlas] " + result.spriteCount + " sprite(s), "
                    + result.width + "x" + result.height + " -> " + result.image);
        } catch (ArtFormatException e) {
            // Une faute de frappe dans une source d'art doit arrêter le build avec un message net,
            // pas une trace d'exception à décoder.
            System.err.println("[atlas] " + e.getMessage());
            System.exit(1);
        } catch (UncheckedIOException | IOException e) {
            System.err.println("[atlas] " + e.getMessage());
            System.exit(1);
        }
    }

    /** Ce que le build a produit, pour affichage et pour les tests. */
    public record Result(Path image, Path index, int spriteCount, int width, int height) {
    }

    public static Result build(Path artDir, Path outputDir) throws IOException {
        Palette palette = readPalette(artDir.resolve(PALETTE_FILE));
        List<SpriteSource> sprites = readSprites(artDir, palette);

        AtlasLayout layout = AtlasLayout.pack(sprites, MAX_ATLAS_WIDTH, MAX_ATLAS_HEIGHT);
        AtlasIndex index = AtlasIndex.of(layout);

        Map<String, SpriteSource> byName = new HashMap<>();
        for (SpriteSource sprite : sprites) {
            byName.put(sprite.name(), sprite);
        }

        BufferedImage image = new BufferedImage(layout.width(), layout.height(), BufferedImage.TYPE_INT_ARGB);
        for (AtlasLayout.Placement placement : layout.placements()) {
            SpriteSource sprite = byName.get(placement.name());
            for (int y = 0; y < sprite.height(); y++) {
                for (int x = 0; x < sprite.width(); x++) {
                    image.setRGB(placement.x() + x, placement.y() + y, toArgb(sprite.pixel(x, y)));
                }
            }
        }

        Files.createDirectories(outputDir);
        Path imagePath = outputDir.resolve(ATLAS_IMAGE);
        Path indexPath = outputDir.resolve(ATLAS_INDEX);
        ImageIO.write(image, "png", imagePath.toFile());
        Files.writeString(indexPath, index.render(), StandardCharsets.UTF_8);

        return new Result(imagePath, indexPath, sprites.size(), layout.width(), layout.height());
    }

    private static Palette readPalette(Path file) {
        if (!Files.isRegularFile(file)) {
            throw new ArtFormatException("palette introuvable : " + file);
        }
        return Palette.parse(file.toString(), readLines(file));
    }

    /**
     * Lit un fichier source en UTF-8, en nommant le fichier fautif si la lecture échoue.
     *
     * <p>Un {@code .sprite} enregistré dans un autre encodage produisait auparavant
     * « echec d'ecriture : Input length = 1 » — un message qui désigne la mauvaise opération et ne
     * dit pas quel fichier reprendre.
     */
    private static List<String> readLines(Path file) {
        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (MalformedInputException | UnmappableCharacterException e) {
            throw new ArtFormatException(file + " : le fichier n'est pas de l'UTF-8 valide"
                    + " (le ré-enregistrer en UTF-8)");
        } catch (IOException e) {
            throw new ArtFormatException(file + " : lecture impossible - " + e.getMessage());
        }
    }

    private static List<SpriteSource> readSprites(Path artDir, Palette palette) throws IOException {
        List<Path> files;
        try (Stream<Path> walk = Files.walk(artDir)) {
            files = walk.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(SPRITE_EXTENSION))
                    // Tri sur le chemin : l'ordre de parcours du système de fichiers n'est pas
                    // garanti, et la sortie doit rester identique d'une machine à l'autre.
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        }
        if (files.isEmpty()) {
            throw new ArtFormatException("aucun fichier " + SPRITE_EXTENSION + " sous " + artDir);
        }

        List<SpriteSource> sprites = new ArrayList<>();
        Map<String, String> seenIn = new HashMap<>();
        for (Path file : files) {
            String source = file.toString();
            for (SpriteSource sprite : SpriteParser.parse(source, readLines(file), palette)) {
                String previous = seenIn.put(sprite.name(), source);
                if (previous != null) {
                    throw new ArtFormatException("le sprite « " + sprite.name()
                            + " » est défini deux fois : " + previous + " et " + source);
                }
                sprites.add(sprite);
            }
        }
        return sprites;
    }

    /** Passe de {@code 0xRRGGBBAA}, la convention des sources, à l'ARGB attendu par AWT. */
    private static int toArgb(int rgba) {
        return (rgba >>> 8) | (rgba << 24);
    }
}
