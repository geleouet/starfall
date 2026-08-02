package dev.starfall.art;

import com.badlogic.gdx.graphics.Color;

/**
 * The Starfall palette. Mirrors STYLE.md section 2.1 exactly — that document is
 * the source of truth and this class is its code-side transcription. Do not add
 * a colour here that is not in STYLE.md; add it to STYLE.md first.
 *
 * <p>All values are immutable. Copy before mutating: {@code new Color(Palette.INK_INDIGO)}.
 */
public final class Palette {

    // -- Paper ------------------------------------------------------------
    public static final Color PAPER_WARM = rgb(0xEDE4D3);
    public static final Color PAPER_COOL = rgb(0xC8C2B8);

    // -- Dusk sky ---------------------------------------------------------
    public static final Color SKY_ZENITH = rgb(0x2B3A5C);
    public static final Color SKY_MID = rgb(0x5B4A6E);
    public static final Color SKY_HORIZON = rgb(0xD9736B);
    public static final Color SKY_HORIZON_HOT = rgb(0xE8907E);

    // -- Ink --------------------------------------------------------------
    /** Never use {@link Color#BLACK}. Ink is blue-black. */
    public static final Color INK_BLACK = rgb(0x161A22);
    public static final Color INK_INDIGO = rgb(0x2C3A4F);
    public static final Color INK_SLATE = rgb(0x4A5A6B);
    public static final Color CLOTH_PALE = rgb(0xB9BEC0);

    // STYLE.md 3b.2. The saturated pool of a shibori bloom and the body of dyed
    // cloth; also the two values System 3's hair ramps between, because hair has
    // to sit a full ramp *below* the garment it crosses and a long way above
    // nothing at all where it trails into open paper.
    public static final Color INDIGO_DEEP = rgb(0x1F2A3D);
    public static final Color INDIGO_MID = rgb(0x3E5470);

    // -- Accents ----------------------------------------------------------
    public static final Color OCHRE = rgb(0xB5793A);
    public static final Color OCHRE_PALE = rgb(0xD8A86A);
    /** Spend sparingly. Vermillion must always carry meaning. */
    public static final Color VERMILLION = rgb(0xC8382E);

    // -- Light ------------------------------------------------------------
    public static final Color BLADE = rgb(0xEAF2F8);
    public static final Color CLASH_CORE = rgb(0xFFF6E2);
    public static final Color EMBER = rgb(0xFF9A4D);

    // -- Atmosphere -------------------------------------------------------
    public static final Color MOTE_CYAN = rgb(0x5FD8E8);
    public static final Color MOTE_MAGENTA = rgb(0xE06BA8);
    public static final Color FOG = rgb(0xD6D2CE);

    private Palette() {
    }

    private static Color rgb(int hex) {
        return new Color(
                ((hex >> 16) & 0xFF) / 255f,
                ((hex >> 8) & 0xFF) / 255f,
                (hex & 0xFF) / 255f,
                1f);
    }

    /** Returns a mutable copy of {@code c} with alpha {@code a}. */
    public static Color alpha(Color c, float a) {
        Color out = new Color(c);
        out.a = a;
        return out;
    }
}
