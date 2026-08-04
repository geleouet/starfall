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
 * A tiny built-in bitmap font, generated at runtime - no external asset files.
 *
 * <p>Glyphs are 5x7 pixels inside an 11 pixel tall cell that reserves two rows above for French
 * accents and two rows below for descenders. Everything is drawn on the pixel grid at an integer
 * scale, so the overlay text stays as crisp as the rest of the scene.
 *
 * <p>Uppercase only: incoming text is upper-cased, which keeps the glyph table small while still
 * covering accented French.
 */
public final class PixelFont implements Disposable {

    /** Width of a glyph, in font pixels. */
    public static final int GLYPH_WIDTH = 5;
    /** Height of a glyph cell (accent zone + body + descender zone), in font pixels. */
    public static final int CELL_HEIGHT = 11;
    /** Horizontal step between two characters, in font pixels. */
    public static final int ADVANCE = 6;
    /** Vertical step between two lines, in font pixels. */
    public static final int LINE_HEIGHT = 13;

    private static final int BODY_TOP = 2;   // row of the cell where the 5x7 body starts
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

        // Accented French capitals, composed from a base glyph plus a mark.
        accented('É', 'E', ACUTE);          // E acute
        accented('È', 'E', GRAVE);          // E grave
        accented('Ê', 'E', CIRCUMFLEX);     // E circumflex
        accented('Ë', 'E', DIAERESIS);      // E diaeresis
        accented('À', 'A', GRAVE);          // A grave
        accented('Â', 'A', CIRCUMFLEX);     // A circumflex
        accented('Î', 'I', CIRCUMFLEX);     // I circumflex
        accented('Ï', 'I', DIAERESIS);      // I diaeresis
        accented('Ô', 'O', CIRCUMFLEX);     // O circumflex
        accented('Ù', 'U', GRAVE);          // U grave
        accented('Û', 'U', CIRCUMFLEX);     // U circumflex
        accented('Ü', 'U', DIAERESIS);      // U diaeresis
        cedilla('Ç', 'C');                  // C cedilla
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
     * Draws a single line of text.
     *
     * @param x     left edge, in the batch's current units (should be a whole pixel)
     * @param yTop  top edge of the glyph cell, in the batch's current units
     * @param scale integer magnification
     */
    public void draw(SpriteBatch batch, CharSequence text, float x, float yTop, int scale) {
        String upper = text.toString().toUpperCase(Locale.FRENCH);
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

    /** Width in pixels a line of text will occupy at the given scale. */
    public int width(CharSequence text, int scale) {
        int length = text.length();
        return length == 0 ? 0 : (length * ADVANCE - 1) * scale;
    }

    @Override
    public void dispose() {
        texture.dispose();
    }

    // ---------------------------------------------------------------- glyph table helpers

    /** Registers a glyph from 7 body rows, optionally followed by up to 2 descender rows. */
    private static void glyph(char character, String... rows) {
        String[] cell = blankCell();
        for (int i = 0; i < rows.length; i++) {
            int target = BODY_TOP + i; // rows past the body spill into the descender zone
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
