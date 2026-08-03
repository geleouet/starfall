// The ground the figure is painted on: Family A warm paper, per STYLE.md 2 and 6.
//
// This is not a backdrop, it is half the picture. In the reference paintings the
// paper is doing as much work as the figure -- cool wash clouds, ochre bleeds,
// mist bands lying across the lower body, a ground that is a smear rather than a
// floor. A flat cream fill would leave the samurai floating in a void and would
// make every judgement about the ink material meaningless.

#ifdef GL_ES
precision highp float;
#endif

varying vec2 v_worldPos;

uniform float u_time;
uniform vec2  u_frameMin;    // world-space rect of the visible frame
uniform vec2  u_frameSize;
uniform vec3  u_paperWarm;
uniform vec3  u_paperCool;
uniform vec3  u_ochre;
uniform vec3  u_inkIndigo;
uniform vec3  u_fogColor;
uniform vec3  u_moteCyan;
uniform vec3  u_moteMagenta;
uniform vec3  u_ember;
uniform vec3  u_fogBands[3];

// -- the Family B dusk sky (System 4 pass 4, item 1) -------------------------
//
// 0 draws the Family A cream sheet, bit for bit as it always has. 1 draws the
// Family B stage of STYLE.md 1: "a sky grading from deep indigo at the top
// through violet to coral/salmon at the horizon... Ground is a dark ink smear
// with grass strokes."
//
// The ramp is not eyeballed off the palette table. It is the corpus's own
// background, sampled as the median of the outer 70 columns of each row on all
// three Family B images and converted to world y through each image's own figure
// span (STYLE.md 11.3 -- the region is the outer strip, the normaliser is the
// figure height). Image 3, y283..955 = 1.70 world units:
//
//     world y 2.42  #313A56  L 58     world y 0.90  #955059  L 95
//     world y 1.87  #434663  L 72     world y 0.63  #8A4E56  L 91
//     world y 1.59  #65566A  L 91     world y 0.49  #3E3B46  L 60
//     world y 1.32  #735A6C  L 97     world y 0.35  #342F39  L 49
//     world y 1.04  #87545E  L 96     world y 0.08  #29232C  L 37
//
// Images 4 and 5 agree to within a few levels at every stop. Two things in it
// are not in the palette table and are worth naming rather than hiding:
//
//   * the corpus's coral is far darker than Palette.SKY_HORIZON. #D9736B is
//     luminance 136; the corpus's horizon band reads 95-100 at the frame edges
//     and 108 at its hottest, between the duellists. STYLE.md 2.1 says the
//     anchors are "attractors, not a locked ramp" and STYLE.md's own preamble
//     says the reference images win, so the hue is the palette's and the value
//     is the corpus's -- expressed as a mix toward SKY_ZENITH rather than as a
//     new hex, so nothing here invents a colour;
//   * the sky does not run to the bottom of the frame. Below world y ~ 0.6 all
//     three images turn to a dark blue-grey ground haze, which is why the ramp
//     has a fourth stop under the horizon rather than three.
uniform float u_dusk;
uniform vec3  u_skyZenith;
uniform vec3  u_skyMid;
uniform vec3  u_skyHorizon;
uniform vec3  u_skyHot;
uniform vec3  u_inkBlack;

float hash2(vec2 p) {
    vec3 q = fract(vec3(p.xyx) * 0.1031);
    q += dot(q, q.yzx + 33.33);
    return fract((q.x + q.y) * q.z);
}

float vnoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash2(i), hash2(i + vec2(1.0, 0.0)), f.x),
               mix(hash2(i + vec2(0.0, 1.0)), hash2(i + vec2(1.0, 1.0)), f.x), f.y);
}

float fbm(vec2 p) {
    float v = vnoise(p) * 0.55;
    v += vnoise(p * 2.1 + 7.3) * 0.27;
    v += vnoise(p * 4.3 - 3.1) * 0.12;
    v += vnoise(p * 9.7 + 11.0) * 0.06;
    return v;
}

/**
 * The measured ramp above, in world y, with the hot centre the corpus draws
 * between the two duellists.
 *
 * <p>Anchored to world y rather than to the frame, so the horizon does not slide
 * under the figures when the camera glides (STYLE.md 9 requires it to be moving
 * at all times, and 11.3 requires a pixel number on a moving-camera scene to be
 * normalised -- a sky pinned to uv.y would make every one of them meaningless).
 */
vec3 duskSky(vec2 w) {
    vec3 haze = mix(u_skyZenith, u_inkBlack, 0.42);   // #222C43, L 44
    vec3 hor  = mix(u_skyHorizon, u_skyZenith, 0.45); // #8A5964, L 101
    vec3 mid  = mix(u_skyMid, u_skyHorizon, 0.22);    // #765369, L  93
    vec3 c = mix(mid, u_skyZenith, smoothstep(1.45, 2.30, w.y));
    c = mix(hor, c, smoothstep(0.86, 1.45, w.y));
    c = mix(haze, c, smoothstep(0.34, 0.66, w.y));
    // The hot band. Reference image 3 reads #A55C61 (L 108) between the
    // duellists at world y 0.7-1.0 and #7E5763 / #9D4E51 (L 95-97) at the same
    // height 3 world units either side, so the coral is a lobe centred on the
    // exchange, not a stripe.
    float band = 1.0 - smoothstep(0.0, 0.62, abs(w.y - 0.88));
    float across = 1.0 - smoothstep(0.7, 3.0, abs(w.x));
    c = mix(c, mix(u_skyHot, u_skyZenith, 0.42), band * across * 0.60);
    // Wash, not a gradient: broad soft streaks so the sheet still reads as
    // pigment laid on paper rather than as a two-stop lerp (STYLE.md 3b.5's
    // first row bans flat fills, and a clean ramp is the same failure).
    float streak = fbm(vec2(w.x * 0.22, w.y * 1.05 + 3.0)) - 0.5;
    c *= 1.0 + streak * 0.16;
    return c;
}

void main() {
    vec2 w = v_worldPos;
    vec2 uv = (w - u_frameMin) / u_frameSize;
    float aspect = u_frameSize.x / u_frameSize.y;

    // Base sheet. Deliberately kept bright and warm nearly everywhere: in the
    // reference paintings most of the sheet is clean cream and the grey only
    // appears as distinct shapes. Grading the whole ground toward the cool tone
    // is what turned the first pass of this into a flat pewter fog.
    vec3 col = mix(u_paperWarm * 0.955, u_paperWarm, smoothstep(-0.10, 0.90, uv.y));

    // Cool wash clouds: read as separate shapes with clean paper between them,
    // so only the top of the noise range gets any pigment at all. Sampled
    // much wider than tall so the shapes drift out as horizontal streaks
    // rather than round blobs -- revision 2, this is what the fog bands
    // below are meant to look like and the two need to agree.
    vec2 drift = vec2(u_time * 0.012, u_time * 0.005);
    float cloud = fbm(vec2(w.x * 0.28, w.y * 0.95) + drift);
    col = mix(col, u_paperCool, smoothstep(0.56, 0.88, cloud) * 0.60);
    col = mix(col, mix(u_paperCool, u_inkIndigo, 0.45), smoothstep(0.70, 0.97, cloud) * 0.45);

    // Ochre bleeds low in the frame, off to the sides. Sparse and unsaturated
    // enough to stay inside the accent budget (STYLE.md 2.2).
    float ochreN = fbm(w * 1.15 + vec2(31.0, 7.0) - drift * 0.6);
    float ochreMask = smoothstep(0.62, 0.94, ochreN)
                    * smoothstep(0.75, 0.20, uv.y)
                    * smoothstep(0.14, 0.36, abs(uv.x - 0.5));
    col = mix(col, u_ochre, ochreMask * 0.42);

    // Family B. Everything above is the Family A sheet and is left computing
    // exactly what it always did, so u_dusk = 0 is bit-identical; the sky
    // replaces the sheet rather than tinting it, because a cream ground graded
    // toward indigo is a muddy ground and not a dusk.
    if (u_dusk > 0.5) {
        col = duskSky(w);
    }
    // Every wash below this line is authored against cream. On the dusk stage
    // each one is re-aimed at the tone that plays the same role there: the ink
    // smear, the contact pool, the grass and the vignette are all *darker* than
    // their ground on the corpus and all four of them would be *lighter* than
    // this one if they kept mixing toward u_paperCool.
    vec3 lowInk = mix(mix(u_inkIndigo, u_paperCool, 0.30), u_inkBlack, u_dusk);
    vec3 poolInk = mix(mix(u_inkIndigo, u_paperCool, 0.22), u_inkBlack, u_dusk);
    vec3 wetInk = mix(u_inkIndigo, u_inkBlack, u_dusk);

    // Ground: an ink smear with a ragged top edge and wet blooms, fading out
    // before it reaches either side of the frame. Not a floor (STYLE.md 6).
    float groundY = 0.075 + 0.045 * fbm(vec2(w.x * 1.6, 5.0));
    float ground = smoothstep(groundY + 0.11, groundY - 0.07, uv.y);
    // At dusk the smear is anchored to the *ground plane* rather than to the
    // bottom of the sheet, and this is a measurement rather than a tidy-up. In uv
    // the smear's top edge lands at world y = 0.465 at the intimate framing --
    // more than a quarter of the way up a 1.70 figure -- so it covers the feet
    // and the lower skirt, and every band statistic read through that region is
    // measuring the ground rather than the duellists. In reference images 3, 4
    // and 5 the smear begins *below* the feet (image 3: feet at y955, smear from
    // y960) and the figures stand clear of it. World y 0.06 is the sole line.
    float groundW = 0.06 + 0.05 * fbm(vec2(w.x * 1.6, 5.0));
    // <b>System 4 pass 5: steeper, and it starts lower.</b> The 0.42 above put the
    // smear's own transition across world y 0.50 down to -0.04, which is a soft
    // 220-row ramp -- an airbrush, and the pass-4 review named it as one. Reference
    // image 3's outer-column background falls from 49.0 at world y 0.00 to 16.7 at
    // -0.10: the corpus's ground band is a *step*, one tenth of a figure height
    // deep, not a gradient a third of a figure high. The smear now runs its whole
    // transition between world y 0.25 and -0.04, which also puts its top edge below
    // the feet where all three Family B images have it.
    // (the dusk smear's own edge is set inside the branch below)
    // And it reaches the frame edges, which the Family A smear deliberately does
    // not ("fading out before it reaches either side of the frame"). Reference
    // image 3's background at its own far left edge reads #171B26 at world
    // y -0.15 and #2D252F at +0.15: the dark band runs the full width. It has to,
    // or the row-local background the whole per-band criterion is built on reads
    // the sky where the picture has ground.
    ground *= mix(smoothstep(0.01, 0.26, uv.x) * smoothstep(0.01, 0.26, 1.0 - uv.x),
                  1.0, u_dusk);
    float bloom = smoothstep(0.42, 0.86, fbm(w * 2.6 + 19.0));
    if (u_dusk < 0.5) {
        // Family A, unchanged: a wash at 0.42-0.87 over cream, and it fades out
        // before it reaches either side of the frame.
        col = mix(col, lowInk, ground * (0.42 + 0.45 * bloom));
    } else {
        // -- System 4 pass 5, item 3: the smear is ragged, not solid ----------
        //
        // Measured, lower third of the figure span plus 30 rows, matched scale and
        // matched crop: reference image 3 reads sd 24.7 and p99-p01 = 91.4 through
        // that band, and 41.6% of its pixels are below STYLE.md 2.2's own floor of
        // luminance 25.73. Clamped at that floor -- which is what a pass obeying
        // 2.2 can reach -- the same band reads sd 21.0 and range 76.6, and pass 4
        // delivered 20.2 and 70.8. So the number in the pass-5 brief was not
        // reachable, and what is left to pay is not dispersion, it is the three
        // things a reader can name: an airbrushed ramp instead of a step, grass
        // strokes at 2% contrast, and a band that got *brighter* toward the frame
        // bottom where every Family B image gets darker.
        //
        // <b>And the marks go ABOVE the smear, not on it.</b> STYLE.md 3.4's rule
        // read one level out: "when the base colour already sits on the floor, pool
        // by lifting everything else... it has nothing left to darken into." Three
        // probes of this pass put splatter, drips and wet blooms *inside* the smear
        // and measured no change at all in the band, for exactly that reason -- the
        // smear is INK_BLACK, the marks are INK_BLACK, and a mark drawn on its own
        // colour is not a mark. What the corpus has in its bottom third is dark
        // marks standing on a ground that is not at the floor: image 3's row median
        // runs 56.7 at world y +0.05 and 15.8 at -0.15, and the marks are the
        // difference between the two.
        //
        // Every octave states its world-space period against STYLE.md 3b.1's 2 px
        // floor: the intimate framing puts 222 px on a world unit.
        //
        // <b>The whole dusk ground is inside this branch, and that is deliberate.</b>
        // Written as terms multiplied by u_dusk it is arithmetically identical on
        // cream -- and it was not identical in delivered pixels: the null control
        // moved 97,791 px over 24 frames at a maximum channel delta of 6, purely
        // from the driver recompiling a longer shader. Multiplying by zero is not
        // the same as not executing. STYLE.md 11.2b(g), one level down.

        // The smear proper: a step below the feet rather than a ramp through the
        // skirt. The pass-4 form put its transition across world y 0.50 down to
        // -0.04, a soft 220-row gradient; reference image 3's outer-column
        // background falls from 49.0 at world y 0.00 to 16.7 at -0.10.
        ground = smoothstep(groundW + 0.16, groundW - 0.12, w.y);
        // Holes, and they live at the smear's top edge rather than all through it:
        // a wash breaks up where it thins and is solid where it pooled, so a
        // uniform hole field only lifts the darkest rows -- measured, it did
        // exactly that and took the band's median the wrong way.
        float holes = 1.0 - 0.45
                    * smoothstep(0.44, 0.78, fbm(vec2(w.x * 2.3, w.y * 5.0) - 61.0))
                    * smoothstep(groundW - 0.06, groundW + 0.10, w.y);
        // The band the marks live in: from the smear's own edge up to about a
        // quarter of a figure height, which is the ankle. Clumped, and it stops
        // well short of the sash -- this is the ground's own smoke, not a second
        // horizon.
        float above = smoothstep(groundW - 0.02, groundW + 0.09, w.y)
                    * smoothstep(groundW + 0.52, groundW + 0.19, w.y);
        float fingers = smoothstep(0.56, 0.86, fbm(vec2(w.x * 1.5, w.y * 2.6) + 23.0))   // 148 px
                      * above;
        float drip = smoothstep(0.56, 0.90, vnoise(vec2(w.x * 26.0, w.y * 2.6 + 3.0)))   //  8.5 px
                   * smoothstep(groundW - 0.04, groundW + 0.07, w.y)
                   * smoothstep(groundW + 0.40, groundW + 0.10, w.y);
        float splat = smoothstep(0.74, 0.97, vnoise(vec2(w.x * 31.0, w.y * 33.0) + 77.0))//  7.2 px
                    * smoothstep(0.46, 0.86, vnoise(vec2(w.x * 4.5, w.y * 5.5) - 12.0))
                    * above;
        col = mix(col, lowInk, ground * holes * (0.80 + 0.19 * bloom));
        col = mix(col, wetInk, fingers * 0.92);
        col = mix(col, wetInk, drip * 0.80);
        col = mix(col, lowInk, splat * 0.95);
    }

    // Contact pool: a rounder, darker wash directly under the figure's feet,
    // distinct from the wide side-to-side ground smear above -- the rig
    // stands at world x=0, so this is centred there rather than on frame
    // centre, and it stays correct if the camera ever re-centres. Revision 2
    // grounding fix: without this the figure floats on blank paper with no
    // contact shadow at all (STYLE.md 6).
    vec2 poolCenter = vec2(0.0, 0.10);
    vec2 poolDelta = w - poolCenter;
    float poolNoise = fbm(w * 2.2 + 41.0);
    float poolDist = length(poolDelta * vec2(0.95, 1.9)) / (0.62 + 0.10 * poolNoise);
    float pool = smoothstep(1.0, 0.0, poolDist);
    // Half strength at dusk, and the reason is the criterion rather than the taste.
    // The pool is centred on world x = 0, which on this lane is the open ground
    // *between* the two duellists, and against a cream sheet it is a soft contact
    // shadow. Against the dark ground band it is the darkest thing in the lower
    // frame and it registers as ink: measured, it bridged the two bodies into one
    // connected component on 17 of the 24 graded frames, where reference image 3
    // keeps 0.044-0.113 of a figure height of clear ground between the feet.
    col = mix(col, poolInk, pool * mix(0.50, 0.22, u_dusk));
    // A second, smaller and wetter bloom slightly forward of the pool centre,
    // like pigment that kept spreading after the wash was laid down.
    float bloom2 = fbm(w * 3.4 + vec2(7.0, -11.0));
    float pool2Dist = length((w - vec2(0.16, 0.06)) * vec2(1.3, 2.3)) / 0.32;
    col = mix(col, wetInk, smoothstep(1.0, 0.0, pool2Dist) * smoothstep(0.5, 0.85, bloom2) * 0.35);

    // Grass strokes: thin, sparse, only just above the smear. Gated by a second
    // low-frequency mask so they come in clumps -- an even comb of strokes right
    // across the frame reads as a ruler, not as a field. Clumps are biased to
    // cluster near the contact pool so the grounding reads as a place, not a
    // strip of lawn running the width of the frame.
    float bladeMask = mix(smoothstep(groundY + 0.13, groundY + 0.02, uv.y)
                            * smoothstep(groundY - 0.04, groundY + 0.02, uv.y),
                          smoothstep(groundW + 0.20, groundW + 0.02, w.y)
                            * smoothstep(groundW - 0.06, groundW + 0.02, w.y), u_dusk);
    float clump = smoothstep(0.40, 0.78, vnoise(vec2(w.x * 1.3, 11.0)))
                * (0.55 + 0.45 * smoothstep(1.0, 0.0, abs(w.x) / 1.1));
    float grass = smoothstep(0.72, 0.95, vnoise(vec2(w.x * 38.0, w.y * 2.0)));
    // 0.34 on cream, 0.80 at dusk. The pass-4 review measured the delivered grass
    // at "roughly 2% contrast" against a corpus whose strokes are the darkest marks
    // in the frame, and a stroke that cannot be seen is not a stroke. The mask and
    // the clumping are unchanged; only the amplitude moves, and only at dusk.
    col = mix(col, lowInk, bladeMask * grass * clump * mix(0.34, 0.80, u_dusk)
                    * smoothstep(0.05, 0.28, uv.x) * smoothstep(0.05, 0.28, 1.0 - uv.x));

    // Fog bands. Same construction as ink_skin.frag so the mist the figure fades
    // into is the same mist that is drawn on the paper -- if these two drifted
    // apart the figure would fade into nothing at all.
    float fog = 0.0;
    for (int i = 0; i < 3; i++) {
        vec3 b = u_fogBands[i];
        float fi = float(i);
        float d = u_time * (0.055 + 0.031 * fi);
        float wobble = b.y * (0.30 * sin(w.x * (0.85 + 0.45 * fi) + d + fi * 2.1)
                            + 0.19 * sin(w.x * 2.43 - d * 0.7 + fi)
                            + 0.11 * sin(w.x * 5.11 + d * 1.6 - fi * 1.7));
        float dist = abs(w.y - (b.x + wobble)) / b.y;
        fog += b.z * (1.0 - smoothstep(0.0, 1.0, dist));
    }
    fog = clamp(fog, 0.0, 1.0);
    // Dead on every scene that ships, and worth a line rather than a deletion:
    // PaperBackground hands this pass NO_BANDS and draws the mist in its own
    // alpha-blended pass afterwards, so u_fogBands is all zeros here and this
    // term contributes nothing on either stage. System 4 pass 5 spent a probe
    // capture attenuating it before measuring that -- STYLE.md 11.2b(g), one
    // level down: state what a term would read if the thing it acts on were
    // absent, and here it is always absent. The attenuation that was wanted lives
    // in PaperBackground.MIST_FRAG, which is where the bands actually are.
    col = mix(col, mix(u_fogColor, u_paperWarm, 0.35), fog * mix(0.62, 0.34, u_dusk));

    // Jewel motes: out-of-focus, slow, and few (STYLE.md 6). Additive but tiny --
    // the moment these read as a particle system they have failed.
    for (int i = 0; i < 8; i++) {
        float fi = float(i);
        vec2 seed = vec2(fi * 7.13 + 1.0, fi * 3.71 + 2.0);
        vec2 c = vec2(hash2(seed), hash2(seed + 11.0));
        c.x += u_time * (0.003 + 0.004 * hash2(seed + 3.0));
        c.y += u_time * 0.0022;
        c = fract(c);
        float r = 0.022 + 0.030 * hash2(seed + 5.0);
        // Wide and very soft: a bokeh mote has no edge at all. Anything with a
        // disc boundary reads as dust on the lens rather than as a light.
        float g = pow(1.0 - smoothstep(0.0, r, length((uv - c) * vec2(aspect, 1.0))), 1.8);
        float k = hash2(seed + 17.0);
        vec3 mc = k < 0.34 ? u_moteCyan : (k < 0.67 ? u_moteMagenta : u_ember);
        // Tinted rather than brightened: additive motes on a cream ground wash out
        // to white.
        col = mix(col, mix(col, mc, 0.22), g);
    }

    // Vignette by wash, never by black (STYLE.md 6).
    float vig = smoothstep(0.55, 1.15, length((uv - 0.5) * vec2(1.05, 1.25)) * 1.6);
    col = mix(col, mix(u_paperCool * 0.94, u_inkBlack, u_dusk), vig * 0.26);

    // Paper tooth. Barely there, but it is the difference between a sheet of
    // paper and a gradient.
    float tooth = vnoise(w * 240.0) * 0.6 + vnoise(w * 90.0 + 5.0) * 0.4;
    col *= 0.975 + 0.05 * tooth;

    gl_FragColor = vec4(clamp(col, vec3(0.06), vec3(0.97)), 1.0);
}
