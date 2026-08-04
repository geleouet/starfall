package com.starfall.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Petite police bitmap intégrée, générée à l'exécution — aucun fichier d'asset externe.
 *
 * <p>Les glyphes font 5x7 pixels dans une cellule haute de 11 pixels, qui réserve deux lignes
 * au-dessus pour les accents français et deux lignes en dessous pour les jambages. Tout est dessiné
 * sur la grille de pixels à une échelle entière, donc le texte de l'interface reste aussi net que le
 * reste de la scène.
 *
 * <p>Capitales uniquement : le texte reçu est passé en majuscules, ce qui garde une table de
 * glyphes réduite tout en couvrant le français accentué.
 */
public final class PixelFont implements Disposable {

    /** Largeur d'un glyphe, en pixels de police. */
    public static final int GLYPH_WIDTH = 5;
    /** Hauteur d'une cellule (zone d'accents + corps + zone de jambages), en pixels de police. */
    public static final int CELL_HEIGHT = 11;
    /** Pas horizontal entre deux caractères, en pixels de police. */
    public static final int ADVANCE = 6;
    /** Pas vertical entre deux lignes, en pixels de police. */
    public static final int LINE_HEIGHT = 13;

    private static final int BODY_TOP = 2;   // ligne de la cellule où commence le corps 5x7
    private static final int BODY_ROWS = 7;

    private static final String[] ACUTE = {"...#.", "..#.."};
    private static final String[] GRAVE = {".#...", "..#.."};
    private static final String[] CIRCUMFLEX = {"..#..", ".#.#."};
    private static final String[] DIAERESIS = {".#.#.", "....."};
    private static final String[] CEDILLA = {"..#..", ".##.."};

    private static final Map<Character, String[]> GLYPHS = new LinkedHashMap<>();

    static {
        glyph('A', ".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#");
        glyph('B', "####.", "#...#", "#...#", "####.", "#...#", "#...#", "####.");
        glyph('C', ".###.", "#...#", "#....", "#....", "#....", "#...#", ".###.");
        glyph('D', "####.", "#...#", "#...#", "#...#", "#...#", "#...#", "####.");
        glyph('E', "#####", "#....", "#....", "####.", "#....", "#....", "#####");
        glyph('F', "#####", "#....", "#....", "####.", "#....", "#....", "#....");
        glyph('G', ".###.", "#...#", "#....", "#.###", "#...#", "#...#", ".###.");
        glyph('H', "#...#", "#...#", "#...#", "#####", "#...#", "#...#", "#...#");
        glyph('I', "#####", "..#..", "..#..", "..#..", "..#..", "..#..", "#####");
        glyph('J', "..###", "...#.", "...#.", "...#.", "...#.", "#..#.", ".##..");
        glyph('K', "#...#", "#..#.", "#.#..", "##...", "#.#..", "#..#.", "#...#");
        glyph('L', "#....", "#....", "#....", "#....", "#....", "#....", "#####");
        glyph('M', "#...#", "##.##", "#.#.#", "#...#", "#...#", "#...#", "#...#");
        glyph('N', "#...#", "##..#", "#.#.#", "#..##", "#...#", "#...#", "#...#");
        glyph('O', ".###.", "#...#", "#...#", "#...#", "#...#", "#...#", ".###.");
        glyph('P', "####.", "#...#", "#...#", "####.", "#....", "#....", "#....");
        glyph('Q', ".###.", "#...#", "#...#", "#...#", "#.#.#", "#..#.", ".##.#");
        glyph('R', "####.", "#...#", "#...#", "####.", "#.#..", "#..#.", "#...#");
        glyph('S', ".####", "#....", "#....", ".###.", "....#", "....#", "####.");
        glyph('T', "#####", "..#..", "..#..", "..#..", "..#..", "..#..", "..#..");
        glyph('U', "#...#", "#...#", "#...#", "#...#", "#...#", "#...#", ".###.");
        glyph('V', "#...#", "#...#", "#...#", "#...#", "#...#", ".#.#.", "..#..");
        glyph('W', "#...#", "#...#", "#...#", "#...#", "#.#.#", "##.##", "#...#");
        glyph('X', "#...#", "#...#", ".#.#.", "..#..", ".#.#.", "#...#", "#...#");
        glyph('Y', "#...#", "#...#", ".#.#.", "..#..", "..#..", "..#..", "..#..");
        glyph('Z', "#####", "....#", "...#.", "..#..", ".#...", "#....", "#####");

        glyph('0', ".###.", "#...#", "#..##", "#.#.#", "##..#", "#...#", ".###.");
        glyph('1', "..#..", ".##..", "..#..", "..#..", "..#..", "..#..", ".###.");
        glyph('2', ".###.", "#...#", "....#", "...#.", "..#..", ".#...", "#####");
        glyph('3', "#####", "...#.", "..##.", "....#", "....#", "#...#", ".###.");
        glyph('4', "...#.", "..##.", ".#.#.", "#..#.", "#####", "...#.", "...#.");
        glyph('5', "#####", "#....", "####.", "....#", "....#", "#...#", ".###.");
        glyph('6', "..##.", ".#...", "#....", "####.", "#...#", "#...#", ".###.");
        glyph('7', "#####", "....#", "...#.", "..#..", ".#...", ".#...", ".#...");
        glyph('8', ".###.", "#...#", "#...#", ".###.", "#...#", "#...#", ".###.");
        glyph('9', ".###.", "#...#", "#...#", ".####", "....#", "...#.", ".##..");

        glyph(' ', ".....", ".....", ".....", ".....", ".....", ".....", ".....");
        glyph(':', ".....", "..#..", "..#..", ".....", "..#..", "..#..", ".....");
        glyph('.', ".....", ".....", ".....", ".....", ".....", ".....", "..#..");
        glyph(',', ".....", ".....", ".....", ".....", ".....", "..#..", "..#..", ".#...");
        glyph(';', ".....", ".....", "..#..", ".....", ".....", "..#..", "..#..", ".#...");
        glyph('-', ".....", ".....", ".....", "#####", ".....", ".....", ".....");
        glyph('_', ".....", ".....", ".....", ".....", ".....", ".....", "#####");
        glyph('+', ".....", "..#..", "..#..", "#####", "..#..", "..#..", ".....");
        glyph('=', ".....", ".....", "#####", ".....", "#####", ".....", ".....");
        glyph('/', "....#", "....#", "...#.", "..#..", ".#...", "#....", "#....");
        glyph('\\', "#....", "#....", ".#...", "..#..", "...#.", "....#", "....#");
        glyph('(', "...#.", "..#..", ".#...", ".#...", ".#...", "..#..", "...#.");
        glyph(')', ".#...", "..#..", "...#.", "...#.", "...#.", "..#..", ".#...");
        glyph('[', "..###", "..#..", "..#..", "..#..", "..#..", "..#..", "..###");
        glyph(']', "###..", "..#..", "..#..", "..#..", "..#..", "..#..", "###..");
        glyph('!', "..#..", "..#..", "..#..", "..#..", "..#..", ".....", "..#..");
        glyph('?', ".###.", "#...#", "....#", "...#.", "..#..", ".....", "..#..");
        glyph('\'', "..#..", "..#..", ".....", ".....", ".....", ".....", ".....");
        glyph('"', ".#.#.", ".#.#.", ".....", ".....", ".....", ".....", ".....");
        glyph('%', "##..#", "##.#.", "...#.", "..#..", ".#...", ".#.##", "#..##");
        glyph('*', ".....", "#.#.#", ".###.", "#####", ".###.", "#.#.#", ".....");
        glyph('<', "...#.", "..#..", ".#...", "#....", ".#...", "..#..", "...#.");
        glyph('>', ".#...", "..#..", "...#.", "....#", "...#.", "..#..", ".#...");
        glyph('#', ".#.#.", ".#.#.", "#####", ".#.#.", "#####", ".#.#.", ".#.#.");

        // Capitales accentuées, composées d'un glyphe de base et d'une marque.
        accented('É', 'E', ACUTE);
        accented('È', 'E', GRAVE);
        accented('Ê', 'E', CIRCUMFLEX);
        accented('Ë', 'E', DIAERESIS);
        accented('À', 'A', GRAVE);
        accented('Â', 'A', CIRCUMFLEX);
        accented('Î', 'I', CIRCUMFLEX);
        accented('Ï', 'I', DIAERESIS);
        accented('Ô', 'O', CIRCUMFLEX);
        accented('Ù', 'U', GRAVE);
        accented('Û', 'U', CIRCUMFLEX);
        accented('Ü', 'U', DIAERESIS);
        cedilla('Ç', 'C');
    }

    private final Texture texture;
    private final Map<Character, Integer> columns = new LinkedHashMap<>();

    public PixelFont() {
        int count = GLYPHS.size();
        Pixmap pixmap = new Pixmap(count * GLYPH_WIDTH, CELL_HEIGHT, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        pixmap.setColor(1f, 1f, 1f, 0f);
        pixmap.fill();
        pixmap.setColor(Color.WHITE);

        int index = 0;
        for (Map.Entry<Character, String[]> entry : GLYPHS.entrySet()) {
            columns.put(entry.getKey(), index);
            String[] cell = entry.getValue();
            for (int row = 0; row < CELL_HEIGHT; row++) {
                String line = cell[row];
                for (int col = 0; col < GLYPH_WIDTH; col++) {
                    if (line.charAt(col) == '#') {
                        pixmap.drawPixel(index * GLYPH_WIDTH + col, row);
                    }
                }
            }
            index++;
        }

        texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pixmap.dispose();
    }

    /**
     * Dessine une ligne de texte.
     *
     * @param x     bord gauche, dans les unités courantes du batch (de préférence un pixel entier)
     * @param yTop  bord supérieur de la cellule, dans les unités courantes du batch
     * @param scale grossissement entier
     */
    public void draw(SpriteBatch batch, CharSequence text, float x, float yTop, int scale) {
        String upper = normalise(text);
        float cursor = x;
        float y = yTop - CELL_HEIGHT * scale;
        for (int i = 0; i < upper.length(); i++) {
            Integer column = columns.get(upper.charAt(i));
            if (column != null) {
                batch.draw(texture,
                        cursor, y,
                        GLYPH_WIDTH * scale, CELL_HEIGHT * scale,
                        column * GLYPH_WIDTH, 0,
                        GLYPH_WIDTH, CELL_HEIGHT,
                        false, false);
            }
            cursor += ADVANCE * scale;
        }
    }

    /** Largeur en pixels qu'occupera une ligne de texte à l'échelle donnée. */
    public int width(CharSequence text, int scale) {
        return widthOf(text, scale);
    }

    /**
     * Même calcul que {@link #width}, mais sans instance : la mesure ne dépend que de la table de
     * glyphes, ce qui la rend testable sans contexte GL.
     *
     * <p>La longueur est mesurée sur le texte <em>normalisé</em>, celui que {@link #draw} dessine
     * réellement — passer en majuscules peut changer le nombre de caractères pour certains
     * caractères Unicode, et une mesure faite sur le texte d'origine décalerait la mise en page.
     */
    public static int widthOf(CharSequence text, int scale) {
        int length = normalise(text).length();
        return length == 0 ? 0 : (length * ADVANCE - 1) * scale;
    }

    /** Vrai si un glyphe existe pour ce caractère exact, sans normalisation préalable. */
    public static boolean hasGlyph(char character) {
        return GLYPHS.containsKey(character);
    }

    /**
     * Premier caractère de {@code text} que {@link #draw} serait incapable de dessiner, ou
     * {@code -1} si la ligne entière est couverte. La normalisation est appliquée d'abord, comme au
     * rendu : un caractère inconnu est sauté en silence, donc mieux vaut pouvoir le détecter.
     */
    public static int firstUndrawableCharacter(CharSequence text) {
        String normalised = normalise(text);
        for (int i = 0; i < normalised.length(); i++) {
            if (!hasGlyph(normalised.charAt(i))) {
                return normalised.charAt(i);
            }
        }
        return -1;
    }

    private static String normalise(CharSequence text) {
        return text.toString().toUpperCase(Locale.FRENCH);
    }

    @Override
    public void dispose() {
        texture.dispose();
    }

    // ---------------------------------------------------------------- table de glyphes

    /** Enregistre un glyphe à partir de 7 lignes de corps, suivies d'au plus 2 lignes de jambage. */
    private static void glyph(char character, String... rows) {
        String[] cell = blankCell();
        for (int i = 0; i < rows.length; i++) {
            int target = BODY_TOP + i; // les lignes au-delà du corps débordent dans la zone de jambage
            if (target < CELL_HEIGHT) {
                cell[target] = rows[i];
            }
        }
        GLYPHS.put(character, cell);
    }

    private static void accented(char character, char base, String[] mark) {
        String[] cell = GLYPHS.get(base).clone();
        cell[0] = mark[0];
        cell[1] = mark[1];
        GLYPHS.put(character, cell);
    }

    private static void cedilla(char character, char base) {
        String[] cell = GLYPHS.get(base).clone();
        cell[BODY_TOP + BODY_ROWS] = CEDILLA[0];
        cell[BODY_TOP + BODY_ROWS + 1] = CEDILLA[1];
        GLYPHS.put(character, cell);
    }

    private static String[] blankCell() {
        String[] cell = new String[CELL_HEIGHT];
        for (int i = 0; i < CELL_HEIGHT; i++) {
            cell[i] = ".....";
        }
        return cell;
    }
}
