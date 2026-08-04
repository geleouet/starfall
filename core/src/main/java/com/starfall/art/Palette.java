package com.starfall.art;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Palette maître du jeu : la correspondance entre un caractère et une couleur.
 *
 * <p>C'est la moitié du format d'art « source texte » verrouillé au cadrage. Une grille de sprite
 * ne contient que des codes d'un caractère ; toutes les couleurs vivent ici, en un seul endroit.
 * Changer une teinte pour tout le jeu est donc une modification d'une ligne, relisable dans un
 * diff — ce qu'un PNG ne permet pas.
 *
 * <p>Format d'une ligne : {@code <code> <nom> <RRGGBB|RRGGBBAA>}. Les lignes vides et celles
 * commençant par {@code #} sont ignorées ; un commentaire peut aussi suivre une déclaration sur la
 * même ligne, la structure en trois champs levant l'ambiguïté.
 *
 * <h2>Caractères réservés</h2>
 *
 * <p>Trois caractères ne peuvent pas être des codes couleur, et le dire franchement vaut mieux que
 * de laisser une déclaration disparaître sans bruit :
 * <ul>
 *   <li>{@code .} — le transparent, qui ne se déclare pas ;</li>
 *   <li>{@code #} — l'introducteur de commentaire ; une ligne {@code # diese ff0000} serait lue
 *       comme un commentaire et la couleur n'existerait jamais ;</li>
 *   <li>{@code @} — l'introducteur de directive dans les fichiers {@code .sprite} ; une ligne de
 *       pixels commençant par lui serait lue comme une directive.</li>
 * </ul>
 */
public final class Palette {

    /** Code réservé au transparent. Jamais déclaré dans le fichier. */
    public static final char TRANSPARENT = '.';

    /** Caractères qui ne peuvent pas servir de code couleur, et pourquoi. */
    private static final String RESERVED = ".#@";

    private final Map<Character, Integer> colorsByCode;
    private final Map<Character, String> namesByCode;

    private Palette(Map<Character, Integer> colorsByCode, Map<Character, String> namesByCode) {
        this.colorsByCode = colorsByCode;
        this.namesByCode = namesByCode;
    }

    /**
     * Analyse une palette.
     *
     * @param source nom affiché dans les messages d'erreur (typiquement le chemin du fichier)
     * @param lines  contenu, une entrée par ligne
     */
    public static Palette parse(String source, List<String> lines) {
        Map<Character, Integer> colors = new LinkedHashMap<>();
        Map<Character, String> names = new LinkedHashMap<>();

        for (int i = 0; i < lines.size(); i++) {
            int lineNumber = i + 1;
            String line = stripComment(lines.get(i)).trim();
            if (line.isEmpty()) {
                continue;
            }

            String[] fields = line.split("\\s+");
            if (fields.length != 3) {
                throw new ArtFormatException(source, lineNumber,
                        "une couleur s'écrit « <code> <nom> <RRGGBB[AA]> », reçu " + fields.length + " champ(s)");
            }
            if (fields[0].length() != 1) {
                throw new ArtFormatException(source, lineNumber,
                        "le code couleur doit faire exactement un caractère, reçu « " + fields[0] + " »");
            }

            char code = fields[0].charAt(0);
            if (RESERVED.indexOf(code) >= 0) {
                throw new ArtFormatException(source, lineNumber,
                        "« " + code + " » est un caractère réservé et ne peut pas être un code couleur"
                                + " (réservés : " + RESERVED + ")");
            }
            if (colors.containsKey(code)) {
                throw new ArtFormatException(source, lineNumber,
                        "le code « " + code + " » est déjà défini (" + names.get(code) + ")");
            }

            colors.put(code, parseColor(source, lineNumber, fields[2]));
            names.put(code, fields[1]);
        }

        if (colors.isEmpty()) {
            throw new ArtFormatException(source + " : palette vide");
        }
        return new Palette(colors, names);
    }

    /**
     * Couleur RGBA empaquetée en {@code 0xRRGGBBAA}, ou 0 pour le transparent.
     *
     * @throws ArtFormatException si le code est inconnu
     */
    public int color(char code, String source, int lineNumber) {
        if (code == TRANSPARENT) {
            return 0;
        }
        Integer color = colorsByCode.get(code);
        if (color == null) {
            throw new ArtFormatException(source, lineNumber,
                    "code couleur inconnu « " + code + " » (codes connus : " + knownCodes() + ")");
        }
        return color;
    }

    public boolean hasCode(char code) {
        return code == TRANSPARENT || colorsByCode.containsKey(code);
    }

    /** Nom lisible d'un code, pour les messages. */
    public String name(char code) {
        return code == TRANSPARENT ? "transparent" : namesByCode.get(code);
    }

    public int size() {
        return colorsByCode.size();
    }

    /** Codes déclarés, dans l'ordre du fichier. */
    public List<Character> codes() {
        return new ArrayList<>(colorsByCode.keySet());
    }

    private String knownCodes() {
        StringBuilder builder = new StringBuilder().append(TRANSPARENT);
        for (char code : colorsByCode.keySet()) {
            builder.append(code);
        }
        return builder.toString();
    }

    private static int parseColor(String source, int lineNumber, String hex) {
        if (hex.length() != 6 && hex.length() != 8) {
            throw new ArtFormatException(source, lineNumber,
                    "une couleur s'écrit sur 6 ou 8 chiffres hexadécimaux, reçu « " + hex + " »");
        }
        // Contrôle caractère par caractère plutôt que de s'en remettre à Long.parseLong, qui
        // accepte un signe : « +ff000 » et « -f0000 » font bien six caractères et passeraient,
        // en donnant une couleur silencieusement fausse.
        for (int i = 0; i < hex.length(); i++) {
            if (Character.digit(hex.charAt(i), 16) < 0) {
                throw new ArtFormatException(source, lineNumber,
                        "« " + hex + " » contient « " + hex.charAt(i)
                                + " », qui n'est pas un chiffre hexadécimal");
            }
        }
        long value = Long.parseLong(hex, 16);
        // Une couleur sur 6 chiffres est opaque : on complète l'alpha plutôt que de le laisser à 0,
        // ce qui produirait un sprite invisible sans le moindre message.
        int rgba = hex.length() == 6 ? (int) ((value << 8) | 0xFF) : (int) value;
        return rgba;
    }

    private static String stripComment(String line) {
        int hash = line.indexOf('#');
        return hash < 0 ? line : line.substring(0, hash);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("Palette{");
        for (Map.Entry<Character, String> entry : namesByCode.entrySet()) {
            builder.append(entry.getKey()).append('=').append(entry.getValue()).append(' ');
        }
        return builder.append('}').toString().toLowerCase(Locale.ROOT);
    }
}
