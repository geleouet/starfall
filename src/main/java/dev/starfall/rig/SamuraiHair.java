package dev.starfall.rig;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import dev.starfall.anim.Skeleton;
import dev.starfall.sim.HairSim;

/**
 * The Night Pilgrim's hair: which lock goes where on this particular skull.
 *
 * <h2>Pass 2: the bundle is bimodal or it is cord</h2>
 *
 * <p>The pass-1 review measured the reference's hair at matched scale and found
 * it made of exactly two marks -- a 30-55 px near-opaque mass, and 1-2 px
 * hairlines peeling off it, <b>with nothing in between</b> -- while this file's
 * first version authored twenty-three strands across a continuum from 3 px to
 * 18 px, i.e. almost entirely inside the gap. The verdict:
 *
 * <blockquote>Pass 1's hair is unimodal at 5-11 px -- every strand drawn at the
 * same width and the same edge hardness -- which is the one register that reads
 * as neither mass nor wisp. It reads as <b>cord</b>.</blockquote>
 *
 * <p>The review is also explicit about the fix that does <em>not</em> work:
 * "Do not fix this by raising the strand count alone. 35 strands at the current
 * 5-11 px width gives a grey smear -- it closes the coverage gap and loses the
 * wisps as well. The fix is a <b>width and value split</b>; count follows from
 * it." So this file now authors two populations and never anything between them:
 *
 * <ul>
 *   <li><b>Six mass locks</b>, 20-33 px wide each, overlapping at the scalp so
 *       their union is a filled black plume 40-55 px across. They are at
 *       {@code INK_BLACK} and hold it across their whole first half. Pass 1
 *       measured the head mesh at 26-45 luminance and the hair growing out of it
 *       at 78-88 median -- a value cliff at the hairline -- where in the
 *       reference the topknot and the hair mass are the same value and the same
 *       object.</li>
 *   <li><b>Eighty-odd hairlines</b> at 1.0-1.9 px, fanned off the twenty
 *       simulated locks by {@code HairRenderer}, most of them shed from
 *       <em>inside</em> the mass so they read as escaping from it.</li>
 * </ul>
 *
 * <h2>The accident this is aiming at</h2>
 *
 * <p>An earlier review recorded that a defect -- "a fringe of uniform radial
 * spokes around the head", produced when the head lobe was a triangle fan and
 * the shader's boundary dissolve ate each wedge back to a spike -- was "the
 * single most reference-accurate feature in any capture so far":
 * {@code out/captures/s1-p2-bind/frame_000.png} is a near-black disc at ~90%
 * coverage with a hundred-odd sub-pixel spokes bristling off its rim. Core to
 * hairline with nothing in between: the reference's exact structure, arrived at
 * by accident, and thrown away when the bug was correctly fixed.
 *
 * <p>Pass 1 delivered the motion and discarded the structure. This is the work
 * order to get it back honestly, and the difference between this and the bug is
 * still the whole design:
 *
 * <ul>
 *   <li>The bug was <b>radial</b>: every spoke pointed away from the skull
 *       centre, including forward across the face, which is why it also read as
 *       a dandelion. Here every lock's rest direction points <b>back</b>, within
 *       about 35 degrees of the breeze, whatever part of the skull it grows
 *       from. Roots fan; directions do not.</li>
 *   <li>The bug was <b>uniform</b> -- one length, one width, one spacing, and
 *       STYLE.md 10 fails a pass on sight of uniform hair motion. Here length
 *       runs 0.24 to 0.80 world units, damping 0.13 s to 0.40 s, and every lock
 *       carries its own turbulence phase and its own hairline fan.</li>
 *   <li>The bug was <b>static</b>. These lag the head, and the whole point of
 *       section 4 is that they lag it by different amounts.</li>
 *   <li>The bug had <b>no mass</b>: a hundred spokes off a disc is only half the
 *       structure, and the half it is missing is the one the disc was
 *       accidentally supplying. Here the mass is authored.</li>
 * </ul>
 *
 * <p>No lock is rooted forward of 55 degrees on the skull. STYLE.md 4b.1 makes
 * the hairline "a hard wet edge, not a blend" and exempts the face from the ink
 * dissolve entirely; a strand crossing the brow would put the one dissolving
 * material in the figure straight over the one surface that must not dissolve.
 */
public final class SamuraiHair {

    private SamuraiHair() {
    }

    /**
     * One pixel at capture framing, in world units. The camera covers 4.91 world
     * units over 960 px, so a world unit is 195.6 px.
     *
     * <p>Hoisted to a named constant because every hairline width below is
     * authored against it: the review's finding is a statement in <em>pixels</em>
     * -- a 30-55 px mass and 1-2 px hairlines -- and converting by eye is how a
     * bundle ends up back in the 5-11 px band nobody meant to author.
     */
    private static final float PX = 1f / 195.6f;

    /**
     * @param bindSkeleton must be at bind pose. Lock roots are read off it once.
     */
    public static HairSim build(Skeleton bindSkeleton) {
        HairSim hair = new HairSim(bindSkeleton, "head");

        Vector2 head = bindSkeleton.worldPosition(bindSkeleton.bone("head").index, new Vector2());
        float cx = head.x + SamuraiRig.HEAD_LOBE_DX;
        float cy = head.y + SamuraiRig.HEAD_LOBE_DY;
        float kx = cx + MathUtils.cosDeg(SamuraiRig.TOPKNOT_ANGLE_DEG) * SamuraiRig.TOPKNOT_DIST;
        float ky = cy + MathUtils.sinDeg(SamuraiRig.TOPKNOT_ANGLE_DEG) * SamuraiRig.TOPKNOT_DIST;

        // The skull. Well inside the drawn head -- 0.70 of it, not 0.92 -- and
        // the difference is the whole first capture. The topknot lobe sits 0.146
        // units up and back from the skull centre with its own radius of 0.072,
        // so a lock rooted on it starts as little as 0.084 from that centre: at
        // 0.92 the collider swallowed the entire tied mass and pushed every lock
        // in it radially outward. The result was a fan of straight spokes above
        // the head -- the *precise* failure this class exists to avoid,
        // reproduced from a completely different cause, and a good argument for
        // looking at the pixels before believing any amount of reasoning.
        hair.collider(cx, cy, SamuraiRig.SKULL_RADIUS * 0.70f);

        // The torso, as two circles up the spine, and this is pass 2's answer to
        // a defect the pass-1 review found in the debug overlay rather than in
        // the picture: "cyan tips already terminate deep inside the chest at
        // rest; it is hidden only because the torso is dark."
        //
        // It is hidden today and it will not be tomorrow. System 4 throws hair
        // forward across the body and across the grip/guard cluster, which
        // docs/system2-debt.md E2 already names as the figure's most fragile
        // small mark -- "the only thing that tells the eye where the body ends
        // and the weapon begins". A hairline drawn through it is a hairline
        // drawn through the read.
        //
        // Two circles rather than one because a torso is not a sphere, and each
        // rides its own bone because the chest counter-rotates against the head
        // under every scene in the corpus: a torso circle carried by the skull
        // would swing away from the torso exactly when the figure turns.
        //
        // Pass 3 widens them and adds a third at the obi. Pass 2's pair were
        // 0.125 and 0.130 units of radius against a garment that measures 0.58
        // units across at the ribs and 0.47 at the hip -- less than half the
        // width of the body they stand for -- so a tip could clear every
        // collider and still be drawn deep inside the figure. Measured on paired
        // captures, 36% of tips sat inside the drawn torso at rest and 64% at
        // the knockback peak. A collider that helps less under an impulse than
        // at rest is a collider that is not where the body is.
        //
        // The radii below are read off the haori rails rather than chosen: the
        // rails give a half-width of 0.29 at the ribs, 0.26 at the waist and
        // 0.24 at the hip. They are set a little inside those, because the outer
        // tenth of the garment is dissolve and wet bleed rather than cloth, and
        // because a strand grazing the silhouette is the reference's own drawing
        // -- what must not happen is a strand vanishing into the solid core.
        hair.collider("chest", -0.015f, 1.335f, 0.240f);
        hair.collider("spine", -0.020f, 1.140f, 0.222f);
        hair.collider("hips", -0.030f, 0.960f, 0.200f);
        

        // -- group 1: the mass ---------------------------------------------
        //
        // Six locks rooted round the back of the skull at 0.76 of its radius --
        // six or seven pixels inside the drawn rim, so the plume grows out of the
        // head rather than beside it -- all pointing back within a 40 degree fan.
        // Each is 20-33 px across and they are 9-13 px apart at the root, so they
        // overlap into one filled shape and only separate where the taper has
        // taken half their width away.
        //
        // The mass is not a different material from the hairlines; it is the same
        // solver, the same shader and the same ink, drawn in the other register.
        // That is what a bundle of hair is.
        float[] massRootDeg = {132f, 158f, 184f, 210f, 236f, 200f};
        float[] massDirDeg = {186f, 196f, 205f, 214f, 224f, 190f};
        float[] massLen = {0.300f, 0.420f, 0.500f, 0.400f, 0.280f, 0.340f};
        // 20 to 33 px across. The review's mass band is 30-55 px and that is the
        // *union*: five of these overlap through the first third of their length.
        float[] massWidth = {0.052f, 0.070f, 0.084f, 0.072f, 0.055f, 0.060f};
        float[] massCurl = {-3.0f, 2.6f, -2.2f, 2.8f, -3.4f, 2.4f};
        for (int i = 0; i < massRootDeg.length; i++) {
            // The last one is rooted on the tied mass itself, because in the
            // reference the topknot and the hair are one object at one value and
            // pass 1 drew a light bundle emerging from a black lobe.
            boolean onKnot = i == 5;
            HairSim.Spec s = new HairSim.Spec();
            float ox = onKnot ? kx : cx;
            float oy = onKnot ? ky : cy;
            float r = onKnot ? SamuraiRig.TOPKNOT_RADIUS * 0.80f : SamuraiRig.SKULL_RADIUS * 0.76f;
            s.rootX = ox + MathUtils.cosDeg(massRootDeg[i]) * r;
            s.rootY = oy + MathUtils.sinDeg(massRootDeg[i]) * r;
            s.restDirDeg = massDirDeg[i];
            s.length = massLen[i];
            s.particles = 9;
            s.curlDeg = massCurl[i];
            s.curlGrowth = 0.30f;
            // The mass is the stiffest and quickest thing in the bundle: it is
            // the part of the hair that is nearly attached to the skull, and it
            // is what makes the arrival chain start at the hair *root* rather
            // than at a tip. The review verified that chain -- hips 11.5, head
            // 13.5, hair root 18.5, hair mid 20.5, sleeve 22.5 -- and named it
            // "the cheapest poetry in the project"; these two numbers are where
            // the root end of it lives.
            s.bendTau = 0.010f;
            s.dragTau = 0.130f + 0.014f * i;
            s.turbulence = 0.10f + 0.04f * (i % 3);
            s.turbFreq = 0.24f + 0.04f * i;
            s.turbPhase = 2.2f * i + 0.4f;
            s.rootHalfWidth = massWidth[i];
            s.valueBias = 0.02f + 0.02f * (i % 3);
            s.rootAlpha = 1f;
            s.seed = 500 + i;
            s.kind = HairSim.Kind.MASS;
            // Three hairlines each, shed from deep inside the plume. These are
            // the ones that read as escaping *from* the mass rather than as
            // starting beside it.
            s.hairlines = 3;
            s.hairlineSpread = 0.052f;
            s.hairlineHalfWidth = 0.62f * PX;
            hair.addStrand(s);
        }

        // -- group 2: the nape locks ---------------------------------------
        //
        // Long, loose, and drawn only as hairlines. This is the group pass 1
        // authored at 7-12 px, i.e. the middle of the band the review failed, and
        // the correction is not to thin the ribbon -- it is to stop drawing a
        // ribbon at all. A lock is a spine with six hairs fanned off it, and the
        // fan widens toward the tip, so the group reads as hair separating rather
        // than as a strap fraying.
        float[] napeRootDeg = {150f, 172f, 194f, 216f, 240f};
        float[] napeDirDeg = {197f, 203f, 211f, 219f, 230f};
        float[] napeLen = {0.430f, 0.560f, 0.640f, 0.520f, 0.380f};
        float[] napeSpread = {0.048f, 0.062f, 0.072f, 0.058f, 0.042f};
        for (int i = 0; i < napeRootDeg.length; i++) {
            HairSim.Spec s = new HairSim.Spec();
            s.rootX = cx + MathUtils.cosDeg(napeRootDeg[i]) * SamuraiRig.SKULL_RADIUS * 0.86f;
            s.rootY = cy + MathUtils.sinDeg(napeRootDeg[i]) * SamuraiRig.SKULL_RADIUS * 0.86f;
            s.restDirDeg = napeDirDeg[i];
            s.length = napeLen[i];
            s.particles = 11;
            // Curl is budgeted as *total turn along the lock*, not as degrees per
            // joint, because degrees per joint means nothing without knowing how
            // many there are. A lock that turns through 190 degrees has curled
            // back over the face it grows out of -- which one pass-1 escapee did,
            // and 4b.1 forbids exactly that. The budget is about 50 degrees here
            // and about 100 for the escapees: a quarter turn for the bundle and
            // rather more for the strands whose job is to curl.
            s.curlDeg = (i % 2 == 0 ? -1f : 1f) * (3.2f + 1.4f * (i % 3));
            s.curlGrowth = 0.50f;
            s.bendTau = 0.017f;
            s.dragTau = 0.185f + 0.020f * i;
            s.turbulence = 0.26f + 0.10f * (i % 3);
            s.turbFreq = 0.29f + 0.048f * i;
            s.turbPhase = 0.71f * i + 2.4f;
            s.rootHalfWidth = 0f;
            s.valueBias = 0.16f + 0.06f * (i % 4);
            s.rootAlpha = 0.96f;
            s.seed = 2000 + i;
            s.kind = HairSim.Kind.WISP;
            s.hairlines = 6;
            s.hairlineSpread = napeSpread[i];
            s.hairlineHalfWidth = 0.58f * PX;
            hair.addStrand(s);
        }

        // -- group 3: the crown locks --------------------------------------
        //
        // Rooted round the tied mass, shorter and quicker than the nape. Roots
        // fan 165 degrees around the knot; rest directions fan only 34. That gap
        // is the entire difference between hair and a dandelion.
        float[] knotRootDeg = {60f, 110f, 165f, 225f};
        float[] knotDirDeg = {182f, 191f, 202f, 216f};
        float[] knotLen = {0.240f, 0.330f, 0.370f, 0.270f};
        for (int i = 0; i < knotRootDeg.length; i++) {
            HairSim.Spec s = new HairSim.Spec();
            s.rootX = kx + MathUtils.cosDeg(knotRootDeg[i]) * SamuraiRig.TOPKNOT_RADIUS * 0.88f;
            s.rootY = ky + MathUtils.sinDeg(knotRootDeg[i]) * SamuraiRig.TOPKNOT_RADIUS * 0.88f;
            s.restDirDeg = knotDirDeg[i];
            s.length = knotLen[i];
            s.particles = 9;
            // Alternating sign, so neighbouring locks curl apart rather than
            // nesting into one thick comma.
            s.curlDeg = (i % 2 == 0 ? 1f : -1f) * (3.6f + 2.0f * (i % 3));
            s.curlGrowth = 0.45f;
            s.bendTau = 0.014f;
            s.dragTau = 0.150f + 0.018f * i;
            s.turbulence = 0.32f + 0.12f * (i % 4);
            s.turbFreq = 0.37f + 0.055f * i;
            s.turbPhase = 1.13f * i;
            s.rootHalfWidth = 0f;
            s.valueBias = 0.14f + 0.05f * (i % 3);
            s.rootAlpha = 0.94f;
            s.seed = 1000 + i;
            s.kind = HairSim.Kind.WISP;
            s.hairlines = 5;
            s.hairlineSpread = 0.038f + 0.010f * i;
            s.hairlineHalfWidth = 0.55f * PX;
            hair.addStrand(s);
        }

        // -- group 4: the escapees -----------------------------------------
        //
        // STYLE.md 4's own term, and the review verified them as one of the
        // pass's real achievements: "arcing and hooking, rendered finer and
        // fainter than the bundle". Half the gravity, nearly twice the wind,
        // three times the shape-recovery time and four times the curl -- so they
        // float, lag dramatically, and curl far out into open air.
        float[] escRootDeg = {120f, 146f, 228f, 246f, 196f};
        float[] escFromKnot = {1f, 1f, 0f, 0f, 0f};
        float[] escDirDeg = {170f, 181f, 214f, 224f, 206f};
        float[] escLen = {0.640f, 0.800f, 0.710f, 0.560f, 0.740f};
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
            s.curlDeg = (i % 2 == 0 ? 1f : -1f) * (6.5f + 1.2f * i);
            // Nearly all of the bend in the last third: these "curl far out into
            // open air", and a curl is a tightening radius, not a constant one.
            s.curlGrowth = 0.75f;
            s.bendTau = 0.030f;
            s.dragTau = 0.31f + 0.022f * i;
            s.gravityScale = 0.50f;
            s.windScale = 1.90f;
            s.turbulence = 0.85f + 0.16f * i;
            s.turbFreq = 0.23f + 0.037f * i;
            s.turbPhase = 1.97f * i + 0.6f;
            s.rootHalfWidth = 0f;
            // Lighter than the rest of the bundle: these spend nearly all of
            // their length in open paper, and lifted toward INDIGO_MID they read
            // as a mark made by a brush that was nearly out of ink. They are the
            // "near-transparent" half of the review's target, and the only part
            // of the hair allowed anywhere near the garment's own value, because
            // they are never anywhere near the garment.
            s.valueBias = 0.42f + 0.05f * i;
            s.rootAlpha = 0.90f;
            s.seed = 3000 + i;
            s.escapee = true;
            s.kind = HairSim.Kind.ESCAPEE;
            // Three each, tightly fanned: an escapee is one or two hairs that
            // have left the bundle, not a lock of them.
            s.hairlines = 3;
            s.hairlineSpread = 0.020f;
            s.hairlineHalfWidth = 0.52f * PX;
            hair.addStrand(s);
        }

        return hair;
    }
}
