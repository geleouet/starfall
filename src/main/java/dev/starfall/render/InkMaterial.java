package dev.starfall.render;

import com.badlogic.gdx.graphics.Color;
import dev.starfall.art.Palette;

/**
 * Per-draw parameters of the ink material, exactly as declared in
 * docs/system1-contract.md section F. Deliberately a plain mutable struct: it is
 * set once per draw call and read straight into uniforms, and later systems
 * (hit reactions pushing {@link #dissolveBias}) will animate these fields
 * frame to frame.
 *
 * <p>The per-vertex side of the material -- dissolve, wetness, stain, flow --
 * lives in {@code a_color} and is authored by the rig (contract section C).
 * What is here is only what applies uniformly to a whole region.
 */
public final class InkMaterial {

    /** Dominant cloth tone. */
    public Color base = Palette.INK_INDIGO;

    /** Where wetness pools. Never {@link Color#BLACK} -- STYLE.md 2.2. */
    public Color deep = Palette.INK_BLACK;

    /** Ochre-rust underpainting showing through, gated by the stainMask channel. */
    public Color stain = Palette.OCHRE;

    /** Positive dissolves more. Reserved for hit reactions shedding flecks (STYLE.md 7.3). */
    public float dissolveBias = 0f;

    /** Scales the width and opacity of the wet-bleed halo outside the dissolve band. */
    public float bleedRadius = 1f;

    /** Scales how hard the paper tooth cuts into the wash. 0 gives a flat, flooded wash. */
    public float paperGrain = 1f;

    /** True for blades: hard-edged, near-white, no dissolve. The one exception in STYLE.md 3. */
    public boolean emissive = false;

    /**
     * Offset added to the material-space sampling point of the ink noise, so two
     * figures cut from the same rig are not painted with bit-identical ink.
     *
     * <p>System 4 puts a second figure on screen for the first time. Every noise
     * field in {@code ink_skin.frag} is sampled at the vertex's <em>bind-space</em>
     * position, deliberately, because that is what stops the pattern swimming
     * (STYLE.md 3.5) -- and it also means the duellist and its opponent get the
     * same torn hem, the same dry-brush streaks and the same flecks in the same
     * places. A constant offset in that same space breaks the repeat without
     * introducing a single screen-space term, which STYLE.md 10 fails on sight.
     *
     * <p>Zero for a lone figure, so every capture shot before System 4 is
     * bit-identical.
     */
    public float seedX = 0f;

    /** @see #seedX */
    public float seedY = 0f;

    /** Configures this material as the blade material of STYLE.md 5. */
    public InkMaterial asBlade() {
        base = Palette.BLADE;
        deep = Palette.BLADE;
        emissive = true;
        dissolveBias = 0f;
        return this;
    }
}
