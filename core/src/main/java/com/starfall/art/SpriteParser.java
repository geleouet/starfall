package com.starfall.art;

import java.util.ArrayList;
import java.util.List;

/**
 * Analyse un fichier {@code .sprite} : une ou plusieurs grilles de caractères indexées sur la
 * {@link Palette}.
 *
 * <pre>
 *   # les lignes commençant par # sont des commentaires
 *   &#64;sprite hero/idle
 *   ....KK....
 *   ...KbbK...
 *
 *   &#64;sprite hero/hit
 *   ....RR....
 * </pre>
 *
 * <p>L'analyse est stricte, et volontairement : ces fichiers sont écrits et relus à la main entre
 * deux passes de review. Une ligne plus courte que les autres, un code couleur inexistant ou un nom
 * en double sont des erreurs signalées avec le fichier et la ligne — jamais un sprite tordu qui
 * s'affiche quand même.
 */
public final class SpriteParser {

    private static final String SPRITE_DIRECTIVE = "@sprite";

    /**
     * Une ligne de pixels et l'endroit d'où elle vient. Le numéro est porté par la ligne plutôt que
     * recalculé à partir de la directive : commentaires et lignes vides peuvent s'intercaler dans
     * une grille, et un message d'erreur qui pointe la mauvaise ligne ne vaut pas mieux que pas de
     * message du tout.
     */
    private record Row(String text, int lineNumber) {
    }

    private SpriteParser() {
    }

    /**
     * @param source  nom affiché dans les messages d'erreur (typiquement le chemin du fichier)
     * @param lines   contenu du fichier, une entrée par ligne
     * @param palette palette maître servant à résoudre les codes
     */
    public static List<SpriteSource> parse(String source, List<String> lines, Palette palette) {
        List<SpriteSource> sprites = new ArrayList<>();

        String currentName = null;
        int currentNameLine = 0;
        List<Row> currentRows = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            int lineNumber = i + 1;
            String line = stripComment(lines.get(i)).stripTrailing();
            if (line.isBlank()) {
                continue;
            }

            if (line.stripLeading().startsWith(SPRITE_DIRECTIVE)) {
                if (currentName != null) {
                    sprites.add(build(source, currentName, currentNameLine, currentRows, palette));
                    currentRows = new ArrayList<>();
                }
                currentName = parseName(source, lineNumber, line.strip());
                currentNameLine = lineNumber;
                continue;
            }

            if (currentName == null) {
                throw new ArtFormatException(source, lineNumber,
                        "grille de pixels avant toute directive « " + SPRITE_DIRECTIVE + " <nom> »");
            }
            if (line.stripLeading().startsWith("@")) {
                throw new ArtFormatException(source, lineNumber,
                        "directive inconnue « " + line.strip().split("\\s+")[0] + " »");
            }

            // Le décalage à gauche fait partie du dessin : on ne retire que les espaces de fin.
            currentRows.add(new Row(line, lineNumber));
        }

        if (currentName != null) {
            sprites.add(build(source, currentName, currentNameLine, currentRows, palette));
        }
        if (sprites.isEmpty()) {
            throw new ArtFormatException(source + " : aucun sprite déclaré");
        }
        return sprites;
    }

    private static String parseName(String source, int lineNumber, String line) {
        String[] fields = line.split("\\s+");
        if (fields.length != 2) {
            throw new ArtFormatException(source, lineNumber,
                    "« " + SPRITE_DIRECTIVE + " » attend exactement un nom, reçu " + (fields.length - 1) + " mot(s)");
        }
        String name = fields[1];
        if (!name.matches("[a-z0-9]+(/[a-z0-9]+)*")) {
            throw new ArtFormatException(source, lineNumber,
                    "nom de sprite invalide « " + name + " » : minuscules, chiffres et « / » uniquement");
        }
        return name;
    }

    private static SpriteSource build(String source, String name, int nameLine,
                                      List<Row> rows, Palette palette) {
        if (rows.isEmpty()) {
            throw new ArtFormatException(source, nameLine,
                    "le sprite « " + name + " » n'a aucune ligne de pixels");
        }

        int width = rows.get(0).text().length();
        for (Row row : rows) {
            if (row.text().length() != width) {
                throw new ArtFormatException(source, row.lineNumber(),
                        "le sprite « " + name + " » a une ligne de " + row.text().length()
                                + " caractères alors que la première en fait " + width);
            }
        }

        int height = rows.size();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            Row row = rows.get(y);
            for (int x = 0; x < width; x++) {
                char code = row.text().charAt(x);
                if (!palette.hasCode(code)) {
                    throw new ArtFormatException(source, row.lineNumber(),
                            "colonne " + (x + 1) + " : code couleur inconnu « " + code + " »");
                }
                pixels[y * width + x] = palette.color(code, source, row.lineNumber());
            }
        }
        return new SpriteSource(name, width, height, pixels);
    }

    private static String stripComment(String line) {
        int hash = line.indexOf('#');
        return hash < 0 ? line : line.substring(0, hash);
    }
}
