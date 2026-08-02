package dev.starfall.rig;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import dev.starfall.anim.Skeleton;
import dev.starfall.sim.HairSim;

/**
 * The Night Pilgrim's hair: which strand goes where on this particular skull.
 *
 * <h2>The topknot the review asked for, built rather than bugged</h2>
 *
 * <p>An earlier review recorded that a defect -- "a fringe of uniform radial
 * spokes around the head", produced when the head lobe was a triangle fan and
 * the shader's boundary dissolve ate each wedge back to a spike -- was reading at
 * this framing as <b>a wind-blown topknot</b>, and called it "the single most
 * reference-accurate feature in any capture so far". The bug was correctly
 * fixed, and the aesthetic loss was real. This is the work order to get the read
 * back honestly, and the difference between the two is worth stating precisely,
 * because it is the whole design:
 *
 * <ul>
 *   <li>The bug was <b>radial</b>: every spoke pointed away from the skull
 *       centre, including forward across the face, which is why it also read as
 *       a dandelion. Here every strand's rest direction points <b>back</b>,
 *       within about 35 degrees of the breeze, whatever part of the skull it
 *       grows from. Roots fan; directions do not.</li>
 *   <li>The bug was <b>uniform</b> -- one length, one width, one spacing, and
 *       STYLE.md 10 fails a pass on sight of uniform hair motion. Here length
 *       runs 0.17 to 0.78 world units (a tenth of body height to nearly half),
 *       root width 0.0045 to 0.016, damping 0.19 s to 0.46 s, and every strand
 *       carries its own turbulence phase.</li>
 *   <li>The bug was <b>static</b>. These lag the head, and the whole point of
 *       section 4 is that they lag it by different amounts.</li>
 *   <li>The bug had <b>no taper</b>: a spoke is a wedge that stops. These narrow
 *       nonlinearly to sub-pixel, which is the mark the corpus actually makes.</li>
 * </ul>
 *
 * <p>What is kept from the bug is the one thing that made it work: <b>a lot of
 * separate thin dark marks leaving the crown into open paper</b>. Twenty-three
 * strands is a lot at a 330 px figure, and the dense group around the topknot is
 * deliberately the one that reads first.
 *
 * <h2>The three groups</h2>
 *
 * <ol>
 *   <li><b>Scalp mass</b> (4). Short, very wide, overlapping: the "dark mass
 *       near the scalp" of section 4, made of the same strands as everything
 *       else and merely drawn thick.</li>
 *   <li><b>Topknot spray</b> (7). Short to medium, rooted round the tied mass,
 *       the densest cluster and the one that carries the wind-blown read.</li>
 *   <li><b>Nape fall</b> (8). Longer, rooted down the back of the skull: the
 *       "long, thin, curling wisps that trail far into open air".</li>
 *   <li><b>Escapees</b> (4). Section 4's own term. Half the gravity, half again the wind, three times the shape-recovery time, nearly double
 *       the damping time and four times the curl -- so they float, lag
 *       dramatically, and curl far out into open air. Images 6, 7 and 8 are full
 *       of them and they are what sells the dream.</li>
 * </ol>
 *
 * <p>No strand is rooted forward of 55 degrees on the skull. STYLE.md 4b.1 makes
 * the hairline "a hard wet edge, not a blend" and exempts the face from the ink
 * dissolve entirely; a strand crossing the brow would put the one dissolving
 * material in the figure straight over the one surface that must not dissolve.
 */
public final class SamuraiHair {

    private SamuraiHair() {
    }

    /**
     * @param bindSkeleton must be at bind pose. Strand roots are read off it once.
     */
    public static HairSim build(Skeleton bindSkeleton) {
        HairSim hair = new HairSim(bindSkeleton, "head");

        Vector2 head = bindSkeleton.worldPosition(bindSkeleton.bone("head").index, new Vector2());
        float cx = head.x + SamuraiRig.HEAD_LOBE_DX;
        float cy = head.y + SamuraiRig.HEAD_LOBE_DY;
        float kx = cx + MathUtils.cosDeg(SamuraiRig.TOPKNOT_ANGLE_DEG) * SamuraiRig.TOPKNOT_DIST;
        float ky = cy + MathUtils.sinDeg(SamuraiRig.TOPKNOT_ANGLE_DEG) * SamuraiRig.TOPKNOT_DIST;

        // Well inside the drawn skull -- 0.72 of it, not 0.92 -- and the
        // difference is the whole first capture. The topknot lobe sits 0.146
        // units up and back from the skull centre with its own radius of 0.072,
        // so a strand rooted on it starts as little as 0.084 from that centre:
        // at 0.92 the collider swallowed the entire tied mass and pushed every
        // strand in it radially outward. The result was a fan of straight spokes
        // above the head -- the *precise* failure this class exists to avoid,
        // reproduced from a completely different cause, and a good argument for
        // looking at the pixels before believing any amount of reasoning.
        //
        // At 0.70 the collider is only what it should be: a stop that keeps a
        // long strand from swinging through the middle of the skull on a hard
        // reversal. Every root in every group sits outside it -- the closest,
        // the scalp mass at 0.78, clears it by 1.6 px -- so it can never push a
        // strand outward at the point where a push would fan the bundle.
        hair.collider(cx, cy, SamuraiRig.SKULL_RADIUS * 0.70f);

        // -- group 1: the topknot spray ------------------------------------
        //
        // Curl is budgeted as *total turn along the strand*, not as degrees per
        // joint, because degrees per joint means nothing without knowing how
        // many there are. The first capture authored 5.5-12.3 degrees on eight
        // joints and 9.5-16.7 on twelve, i.e. up to 100 and 190 degrees of total
        // turn, and a strand that turns through 190 degrees has curled back over
        // the face it grows out of -- one escapee left the crown and crossed the
        // brow, which is precisely what STYLE.md 4b.1 forbids. The budget is now
        // about 43 degrees for the spray, 52 for the nape and 103 for the
        // escapees: a quarter turn for the bundle and rather more than a quarter
        // for the strands whose job is to curl.
        //
        // Roots fan 208 degrees around the tied mass; rest directions fan only
        // 52. That gap is the entire difference between hair and a dandelion.
        float[] knotRootDeg = {56f, 96f, 132f, 168f, 202f, 232f, 256f};
        float[] knotDirDeg = {178f, 186f, 193f, 200f, 208f, 217f, 226f};
        float[] knotLen = {0.215f, 0.295f, 0.345f, 0.365f, 0.310f, 0.245f, 0.190f};
        float[] knotWidth = {0.0112f, 0.0134f, 0.0150f, 0.0158f, 0.0138f, 0.0118f, 0.0098f};
        for (int i = 0; i < knotRootDeg.length; i++) {
            HairSim.Spec s = new HairSim.Spec();
            s.rootX = kx + MathUtils.cosDeg(knotRootDeg[i]) * SamuraiRig.TOPKNOT_RADIUS * 0.86f;
            s.rootY = ky + MathUtils.sinDeg(knotRootDeg[i]) * SamuraiRig.TOPKNOT_RADIUS * 0.86f;
            s.restDirDeg = knotDirDeg[i];
            s.length = knotLen[i];
            s.particles = 9;
            // Alternating sign, so neighbouring strands in the spray curl apart
            // rather than nesting into one thick comma.
            s.curlDeg = (i % 2 == 0 ? 1f : -1f) * (3.4f + 2.0f * (i % 3));
            s.bendTau = 0.013f;
            s.dragTau = 0.145f + 0.016f * i;
            s.turbulence = 0.30f + 0.12f * (i % 4);
            s.turbFreq = 0.37f + 0.055f * i;
            s.turbPhase = 1.13f * i;
            s.rootHalfWidth = knotWidth[i];
            s.valueBias = 0.10f + 0.04f * (i % 3);
            s.rootAlpha = 0.96f;
            s.seed = 1000 + i;
            hair.addStrand(s);
        }

        // -- group 2: the scalp mass ---------------------------------------
        //
        // STYLE.md 4 describes hair as "a dark mass near the scalp resolving
        // into long, thin, curling wisps", and the first capture had only the
        // second half of that: fifteen separate curves radiating from one point,
        // which reads as a spider rather than as hair. These four are short,
        // three times the width of anything else, and overlap each other -- at
        // the root they are 7 px apart and 17 px wide, so they merge into one
        // opaque mass and only separate where the taper has taken half their
        // width away. The mass is not a different material from the wisps; it is
        // the same strands drawn thick, which is what a bundle of hair is.
        float[] massRootDeg = {156f, 182f, 208f, 232f};
        float[] massDirDeg = {198f, 206f, 215f, 224f};
        float[] massLen = {0.250f, 0.300f, 0.270f, 0.215f};
        float[] massWidth = {0.0400f, 0.0455f, 0.0415f, 0.0345f};
        for (int i = 0; i < massRootDeg.length; i++) {
            HairSim.Spec s = new HairSim.Spec();
            s.rootX = cx + MathUtils.cosDeg(massRootDeg[i]) * SamuraiRig.SKULL_RADIUS * 0.78f;
            s.rootY = cy + MathUtils.sinDeg(massRootDeg[i]) * SamuraiRig.SKULL_RADIUS * 0.78f;
            s.restDirDeg = massDirDeg[i];
            s.length = massLen[i];
            s.particles = 8;
            s.curlDeg = (i % 2 == 0 ? -1f : 1f) * 4.0f;
            s.curlGrowth = 0.25f;
            s.bendTau = 0.009f;
            s.dragTau = 0.13f + 0.014f * i;
            s.turbulence = 0.10f;
            s.turbFreq = 0.24f + 0.04f * i;
            s.turbPhase = 2.2f * i + 0.4f;
            s.rootHalfWidth = massWidth[i];
            s.valueBias = 0.02f;
            s.rootAlpha = 1f;
            s.seed = 500 + i;
            hair.addStrand(s);
        }

        // -- group 3: the nape fall ----------------------------------------
        float[] napeRootDeg = {144f, 162f, 178f, 194f, 210f, 226f, 240f, 253f};
        float[] napeDirDeg = {194f, 199f, 204f, 210f, 216f, 222f, 229f, 236f};
        float[] napeLen = {0.400f, 0.510f, 0.590f, 0.625f, 0.540f, 0.450f, 0.370f, 0.300f};
        float[] napeWidth = {0.0232f, 0.0268f, 0.0292f, 0.0300f, 0.0276f, 0.0243f, 0.0208f, 0.0176f};
        for (int i = 0; i < napeRootDeg.length; i++) {
            HairSim.Spec s = new HairSim.Spec();
            s.rootX = cx + MathUtils.cosDeg(napeRootDeg[i]) * SamuraiRig.SKULL_RADIUS * 0.86f;
            s.rootY = cy + MathUtils.sinDeg(napeRootDeg[i]) * SamuraiRig.SKULL_RADIUS * 0.86f;
            s.restDirDeg = napeDirDeg[i];
            s.length = napeLen[i];
            s.particles = 11;
            s.curlDeg = (i % 2 == 0 ? -1f : 1f) * (3.0f + 1.5f * (i % 4));
            s.bendTau = 0.016f;
            s.dragTau = 0.175f + 0.016f * i;
            s.turbulence = 0.24f + 0.10f * (i % 3);
            s.turbFreq = 0.29f + 0.048f * i;
            s.turbPhase = 0.71f * i + 2.4f;
            s.rootHalfWidth = napeWidth[i];
            s.valueBias = 0.12f + 0.05f * (i % 4);
            s.rootAlpha = 0.98f;
            s.seed = 2000 + i;
            hair.addStrand(s);
        }

        // -- group 4: the escapees -----------------------------------------
        float[] escRootDeg = {120f, 146f, 228f, 246f};
        float[] escFromKnot = {1f, 1f, 0f, 0f};
        float[] escDirDeg = {168f, 180f, 214f, 224f};
        float[] escLen = {0.640f, 0.790f, 0.710f, 0.560f};
        // Wider and darker than the first pass had them, and the correction is
        // the debt documents' oldest lesson applied to the one part of the hair
        // STYLE.md 4 says "sells the dreamlike quality". Authored at a 0.9 px
        // half-width and lifted most of the way to INDIGO_MID, they were a
        // faint grey scratch that did not survive the capture at all -- exactly
        // the way the hand and the hilt sat invisible for five passes. A mark
        // that is meant to read has to be *authored* to read.
        float[] escWidth = {0.0082f, 0.0092f, 0.0086f, 0.0074f};
        for (int i = 0; i < escRootDeg.length; i++) {
            HairSim.Spec s = new HairSim.Spec();
            boolean fromKnot = escFromKnot[i] > 0.5f;
            float ox = fromKnot ? kx : cx;
            float oy = fromKnot ? ky : cy;
            float r = fromKnot ? SamuraiRig.TOPKNOT_RADIUS * 0.9f : SamuraiRig.SKULL_RADIUS * 0.9f;
            s.rootX = ox + MathUtils.cosDeg(escRootDeg[i]) * r;
            s.rootY = oy + MathUtils.sinDeg(escRootDeg[i]) * r;
            s.restDirDeg = escDirDeg[i];
            s.length = escLen[i];
            s.particles = 13;
            // Four times the spray's curl and one sign per strand, so each
            // escapee describes a genuine arc rather than a wobble. This is the
            // "curl" half of section 4's escapee clause; the lag half is below.
            s.curlDeg = (i % 2 == 0 ? 1f : -1f) * (6.5f + 1.4f * i);
            // Nearly all of the bend in the last third: these are the strands
            // that "curl far out into open air", and a curl is a tightening
            // radius, not a constant one.
            s.curlGrowth = 0.75f;
            s.bendTau = 0.030f;
            s.dragTau = 0.31f + 0.024f * i;
            s.gravityScale = 0.50f;
            s.windScale = 1.90f;
            s.turbulence = 0.85f + 0.18f * i;
            s.turbFreq = 0.23f + 0.037f * i;
            s.turbPhase = 1.97f * i + 0.6f;
            s.rootHalfWidth = escWidth[i];
            // Lighter than the rest of the bundle. These are the strands that
            // spend most of their length in open paper, and at INK_BLACK a
            // 2 px hairline 150 levels below the ground reads as a scratch
            // rather than as hair; lifted toward INDIGO_MID it reads as a mark
            // made by a brush that was nearly out of ink.
            s.valueBias = 0.30f;
            s.rootAlpha = 0.95f;
            s.seed = 3000 + i;
            s.escapee = true;
            hair.addStrand(s);
        }

        return hair;
    }
}
