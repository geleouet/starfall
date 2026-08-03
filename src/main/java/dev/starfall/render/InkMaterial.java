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

    /**
     * What a saturated stain's thinned rim pushes toward. {@code OCHRE_PALE} for
     * every garment — the value the resolve had hard-coded until System 3b — and
     * the lip tone for skin, where a rust rim would read as a wound. Resolve-stage,
     * so a material changing it ends the merge group like {@link #base} does.
     */
    public Color stainPale = Palette.OCHRE_PALE;

    /**
     * Multiplies the coverage this material's fragments write, 0..1. 1 is every
     * capture shot before System 3b, bit-for-bit (the shader takes the multiply
     * only under a branch on the uniform).
     *
     * <p>This is STYLE.md 3b.1's "detail resolves on push-in" for <em>authored
     * marks</em> rather than noise octaves: an octave fades itself out via
     * {@code octaveFade} as its period approaches the pixel grid, but a brow
     * stroke or an eye is geometry and cannot — so the scene fades it, continuously,
     * from the same camera width that drives everything else. At the planning
     * framing a face is a suggestion (the skin field's value structure), not a set
     * of 2 px marks shimmering on a 20 px head.
     */
    public float covScale = 1f;

    /**
     * Minimum feathered rim, in pixels, on every boundary this material prints.
     * 0 for every garment — cloth edges are shaped by the fray band and must stay
     * torn, not soft — and every capture shot before System 3b pass 2 is
     * bit-identical at 0 (the shader takes the multiply only under a branch).
     *
     * <p>This exists for the face. STYLE.md 4b.1 exempts skin from the <em>ink
     * dissolve</em>; the pass-1 review's finding is that it grants no licence to
     * alias: at dissolve 0 the fray threshold sweeps its whole range in well
     * under a pixel of a small mark, so the mouth quad and the profile facets
     * printed as hard-edged objects — §3 violated at exactly the place 4b.1
     * removed the dissolve that would have hidden it. A feather is the soft
     * alpha rim {@code lightSpeck} already gives the specular, applied to
     * geometry: coverage ramps over this many pixels of the strip's own
     * material-space boundary distance, independent of the noise field.
     */
    public float feather = 0f;

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

    /**
     * Bind-space height of the sash: where this garment's colourway changes.
     *
     * <h2>The per-region colour channel, in the cheapest form that is still one</h2>
     *
     * <p>{@code docs/system4-debt.md} has recorded for two passes that image 3 "has a
     * colourway that <em>changes</em> at the sash, and {@code InkMaterial} has one base
     * colour per draw call", and that the same missing channel blocks STYLE.md §7.3's
     * "the dissolve threshold pushed <em>locally</em>". A full per-vertex colour channel
     * is a second render target and a mesh format change. This is not that.
     *
     * <p>What it is: the material pass already varies value per fragment through
     * {@code pool}, and it already samples everything at the vertex's <b>bind-space</b>
     * position so nothing swims (§3.5). A bind-space height therefore names a band of
     * the garment exactly as well as a per-vertex weight would, for the one split the
     * corpus actually shows — pale kimono above, near-black hakama below. The line is
     * in the same space as the noise it modulates, so it deforms with the skin and is
     * invisible to every {@code fwidth} in the shader.
     *
     * <p>What it is not: it cannot make the hakama a <em>different hue</em> from the
     * kimono, only a different value. That remains open and is still the argument for
     * the real channel.
     */
    public float sashHeight = 0f;

    /**
     * Where the sash lift's licence ENDS: above this bind-space height the
     * pooling multiplier returns to 1. Defaults far above any figure, so every
     * capture shot before System 3b pass 2 is bit-identical.
     *
     * <p>System 3b pass 2, closing the rest of debt §5.2. The lift exists to
     * keep the pale <em>kimono</em> pale, and "everything above the sash line"
     * included the skull: the head lobe, the topknot lobe and the jaw wedge all
     * print in the cloth material, so the pale duellist wore its haori colour
     * as a skull (§5.2), a white crescent for a topknot (found at the planning
     * framing, s3b-p2-wide-try frame 0) and a washed-out jaw shadow (review
     * §9's "weak (foe)" row). A garment compensation has no business on a
     * head: the licence now stops at the collar line, and the skull's own
     * pooling — authored wet, toward {@code deep = INK_BLACK} — prints the
     * near-black head every family-B reference paints.
     */
    public float sashTop = 99f;

    /**
     * What the pooling above {@link #sashHeight} is multiplied by. 1.0 changes nothing.
     *
     * <h2>Why this number exists and what it is measured against</h2>
     *
     * <p>Reference image 3 is a dark duellist against a pale one and the separation is
     * enormous: median ink luminance as a fraction of the frame's own ground level is
     * <b>0.17 for the dark figure's torso and 0.56 for the pale one's — 3.3x apart</b>,
     * unmistakable at any scale. System 4 pass 2 pooled the pale figure to the ink floor
     * (correctly, for the skirt: the corpus reads 1.16x there and the capture 1.29x) and
     * did not compensate above the sash, and the capture landed at <b>1.51x</b>, where
     * "you cannot tell which duellist is the pale one".
     *
     * <p>So the pale figure keeps {@code INK_BLACK} as its pooling colour below the sash
     * and lifts above it. Graded on delivered pixels by {@code DuellistValueTest}, not
     * on this field — a base colour is not a picture, and asserting on one is what let
     * {@code DirectorTest.bothFiguresAreVisuallyDistinguishable} pass through the
     * regression.
     */
    public float sashLift = 1f;

    /** Configures this material as the blade material of STYLE.md 5. */
    public InkMaterial asBlade() {
        base = Palette.BLADE;
        deep = Palette.BLADE;
        emissive = true;
        dissolveBias = 0f;
        return this;
    }
}
