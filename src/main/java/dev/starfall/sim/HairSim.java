package dev.starfall.sim;

import com.badlogic.gdx.math.Vector2;
import dev.starfall.anim.Skeleton;

import java.util.ArrayList;
import java.util.List;

/**
 * Hair as a bundle of independent Verlet strands carried by one bone, per
 * STYLE.md 4. Not a cloth sheet, not a mesh: 12-24 strands of 8-14 particles,
 * each with its own length, mass, damping, rest curl and phase, so the bundle
 * reads as many hairs rather than one flapping flag.
 *
 * <h2>What is authored where</h2>
 *
 * <p>This class knows nothing about the samurai. Roots, rest directions, curl
 * and style are handed to {@link #addStrand} in <em>world-bind space</em> -- the
 * same space {@code SamuraiRig} authors its meshes in -- and converted once into
 * the carrier bone's frame. The layout itself lives in
 * {@code dev.starfall.rig.SamuraiHair}, because which strand goes where is a
 * statement about a particular skull.
 *
 * <h2>Why the rest direction is carried rather than fixed</h2>
 *
 * <p>A chain with only distance constraints hangs straight down. Every reference
 * image shows the opposite: hair lies <em>back</em> along the skull and streams
 * from it. So segment 0's rest direction is the authored one rotated by however
 * far the head has turned since bind, and every joint below it bends relative to
 * the segment above. Turning the head therefore re-aims the root instantly and
 * the tip only eventually -- which is STYLE.md 4's "hair leads and lags the head"
 * arriving out of the structure rather than out of a special case, and is why
 * the tips are still travelling the old way for a beat after a reversal.
 *
 * <h2>Escapees</h2>
 *
 * <p>STYLE.md 4 asks for "a few escapee strands with much lower mass that lag
 * dramatically and curl", and names them as what sells the dreamlike quality.
 * Low mass in a position-based solver is not a mass at all -- gravity is
 * mass-independent -- so it is expressed the way it actually reads: a smaller
 * {@code gravityScale} (they float rather than fall), a larger
 * {@code windScale} (the same breeze moves them much further), a longer
 * {@code dragTau} (they arrive late), a much lower bend stiffness (they hold no
 * shape of their own and take the longest to recover it) and a heavier rest curl.
 */
public final class HairSim {

    /**
     * One strand: its solver chain, where it is rooted on the carrier bone, and
     * everything the renderer needs to draw it as a tapered ribbon.
     */
    public static final class Strand {

        final VerletChain chain;
        /** Root position in the carrier bone's frame. */
        final float localX;
        final float localY;
        /** Rest direction of segment 0, relative to the carrier bone's bind rotation. */
        final float restDirLocalDeg;

        final float windScale;
        final float gravityScale;
        final float turbulence;
        final float turbPhase;
        final float turbFreq;

        /** Half-width at the root, in world units. The taper to the tip is the renderer's. */
        public final float rootHalfWidth;
        /** 0 at the strand's own root, 1 at its tip: how far along the value ramp this strand sits. Escapees start lighter. */
        public final float valueBias;
        /** Peak opacity at the root. */
        public final float rootAlpha;
        /** Per-strand seed for the material-space noise. Stable across runs. */
        public final float seed;
        public final boolean escapee;

        Strand(VerletChain chain, float localX, float localY, float restDirLocalDeg,
               float windScale, float gravityScale, float turbulence, float turbPhase, float turbFreq,
               float rootHalfWidth, float valueBias, float rootAlpha, float seed, boolean escapee) {
            this.chain = chain;
            this.localX = localX;
            this.localY = localY;
            this.restDirLocalDeg = restDirLocalDeg;
            this.windScale = windScale;
            this.gravityScale = gravityScale;
            this.turbulence = turbulence;
            this.turbPhase = turbPhase;
            this.turbFreq = turbFreq;
            this.rootHalfWidth = rootHalfWidth;
            this.valueBias = valueBias;
            this.rootAlpha = rootAlpha;
            this.seed = seed;
            this.escapee = escapee;
        }

        public int particleCount() {
            return chain.particleCount();
        }

        public float x(int i) {
            return chain.x(i);
        }

        public float y(int i) {
            return chain.y(i);
        }

        public float length() {
            return chain.totalRestLength();
        }

        /** Per-substep displacement of the tip. The quantity a lag measurement reads. */
        public float tipSpeed() {
            return chain.speedOf(chain.particleCount() - 1);
        }
    }

    /** How a strand is specified. All positions and angles are world-bind space. */
    public static final class Spec {
        public float rootX;
        public float rootY;
        /** Where the strand points at rest, in world-bind space. Degrees, 0 = +X. */
        public float restDirDeg;
        public float length;
        public int particles = 11;
        /** Degrees of bend per joint. Constant sign gives a curl; the sign itself is which way it curls. */
        public float curlDeg;
        /**
         * How much the curl grows toward the tip. 0 is a constant-curvature arc,
         * which reads as wire; 0.6 puts most of the bend in the last third, which
         * is the comma-shaped wisp the corpus actually draws.
         */
        public float curlGrowth = 0.45f;
        /** Shape-recovery time constant, seconds. Small is stiff and early; large is loose and late. */
        public float bendTau = 0.11f;
        public float dragTau = 0.24f;
        public float gravityScale = 1f;
        public float windScale = 1f;
        public float turbulence = 0.35f;
        public float turbFreq = 0.41f;
        public float turbPhase;
        public float rootHalfWidth = 0.010f;
        public float valueBias = 0f;
        public float rootAlpha = 0.95f;
        public boolean escapee;
        public int seed;
    }

    /**
     * Base gravity, and it is not 9.81. STYLE.md 7 opens with "motion is
     * choreography, not physics": at true gravity a 0.6-unit strand falls to
     * vertical in about a third of a second and the bundle reads as a wet mop.
     * The corpus's hair is wind-dominated and nearly weightless -- it drifts. So
     * gravity is a shaping term at roughly a sixth of scale and the wind and the
     * head's own motion carry the picture.
     */
    private static final float GRAVITY = 1.55f;

    private final Skeleton skeleton;
    private final int carrierIndex;
    private final float bindX;
    private final float bindY;
    private final float bindRotDeg;

    private final List<Strand> strands = new ArrayList<>();

    private float colliderLocalX;
    private float colliderLocalY;
    private float colliderRadius;

    private float windX;
    private float windY;
    private float time;
    private float turbulenceScale = 1f;

    private final Vector2 scratch = new Vector2();

    /**
     * @param bindSkeleton must be sitting at its bind pose: the carrier bone's
     *                     transform is read once here and every authored root is
     *                     expressed relative to it.
     */
    public HairSim(Skeleton bindSkeleton, String carrierBone) {
        this.skeleton = bindSkeleton;
        this.carrierIndex = bindSkeleton.bone(carrierBone).index;
        bindSkeleton.worldPosition(carrierIndex, scratch);
        this.bindX = scratch.x;
        this.bindY = scratch.y;
        this.bindRotDeg = bindSkeleton.worldRotationDeg(carrierIndex);
    }

    /** The skull, in world-bind space. Strands are pushed out of it so hair cannot sink through the head. */
    public HairSim collider(float worldBindX, float worldBindY, float radius) {
        float dx = worldBindX - bindX;
        float dy = worldBindY - bindY;
        float c = SimMath.cosDeg(-bindRotDeg);
        float s = SimMath.sinDeg(-bindRotDeg);
        this.colliderLocalX = dx * c - dy * s;
        this.colliderLocalY = dx * s + dy * c;
        this.colliderRadius = radius;
        for (Strand strand : strands) {
            strand.chain.collider(0f, 0f, radius);
        }
        return this;
    }

    public Strand addStrand(Spec spec) {
        int n = Math.max(2, spec.particles);
        VerletChain chain = new VerletChain(n);
        float segLen = spec.length / (n - 1);
        for (int i = 0; i < n - 1; i++) {
            // The recovery time grows toward the tip: the scalp end of a hair
            // holds its direction and the last third of it holds almost nothing.
            // This is the second source of the increasing lag down the strand,
            // alongside the one-joint-per-round propagation of the bend pass
            // itself, and it is why a tip lags further than a mid-strand
            // particle rather than merely as far.
            float t = i / (float) Math.max(1, n - 2);
            float tau = spec.bendTau * (1f + 2.6f * t * t);
            // The rest curl is neither constant nor smooth. A constant bend per
            // joint is a circular arc, and a circular arc reads as wire however
            // well it is tapered; the corpus's wisps grow their curvature toward
            // the tip and wander on the way. So the authored curl is scaled by a
            // tip-weighted ramp and then by a seeded per-joint jitter -- the same
            // brush wobbling, not a second strand.
            float ramp = (1f - spec.curlGrowth) + 2f * spec.curlGrowth * t;
            float jitter = 0.55f + 0.9f * SimMath.hash01(spec.seed * 977 + i * 31 + 5);
            chain.segment(i, segLen, spec.curlDeg * ramp * jitter, tau);
        }
        chain.dragTau(spec.dragTau).iterations(8).gravity(0f, -GRAVITY * spec.gravityScale);
        if (colliderRadius > 0f) {
            chain.collider(0f, 0f, colliderRadius);
        }

        float dx = spec.rootX - bindX;
        float dy = spec.rootY - bindY;
        float c = SimMath.cosDeg(-bindRotDeg);
        float s = SimMath.sinDeg(-bindRotDeg);
        Strand strand = new Strand(chain,
                dx * c - dy * s,
                dx * s + dy * c,
                spec.restDirDeg - bindRotDeg,
                spec.windScale, spec.gravityScale, spec.turbulence, spec.turbPhase, spec.turbFreq,
                spec.rootHalfWidth, spec.valueBias, spec.rootAlpha,
                SimMath.hash01(spec.seed) * 64f, spec.escapee);
        strands.add(strand);
        return strand;
    }

    public int strandCount() {
        return strands.size();
    }

    public Strand strand(int i) {
        return strands.get(i);
    }

    public List<Strand> strands() {
        return strands;
    }

    /**
     * Scales every strand's own turbulence. 1 is the shipped bundle; 0 leaves
     * only the shared breeze and the carrier bone's motion.
     *
     * <p>It exists to make STYLE.md 7.1's lag requirement <em>measurable</em>,
     * and the distinction is worth being precise about. "Cloth trails the body
     * by 4-8 frames and hair tips by 8-14" is a statement about the transfer
     * from the body to the tip. Per-strand turbulence is an independent input
     * added at the tip, and it is deliberately large -- 10 fails a pass on sight
     * of uniform hair motion, so the strands must not agree with each other.
     * With it running, a correlation between the head and a tip is measuring
     * mostly the air: the first attempt at this test reported the escapee tips
     * peaking three frames <em>before</em> the head they hang from. Zeroing it
     * for a measurement isolates the quantity the specification is actually
     * about, and does not change a single frame of any capture.
     */
    public HairSim turbulenceScale(float scale) {
        this.turbulenceScale = Math.max(0f, scale);
        return this;
    }

    /** Steady breeze, in world units per second squared. The scene drives this; gusts are part of the script. */
    public HairSim wind(float wx, float wy) {
        this.windX = wx;
        this.windY = wy;
        return this;
    }

    /**
     * Re-anchors every strand onto the carrier bone's current transform and
     * refreshes its per-strand wind. Call after the skeleton is final for this
     * frame -- animation, then IK, then this -- and before the solver steps.
     */
    public void refresh(float timeSeconds) {
        this.time = timeSeconds;
        skeleton.worldPosition(carrierIndex, scratch);
        float hx = scratch.x;
        float hy = scratch.y;
        float rot = skeleton.worldRotationDeg(carrierIndex);
        float c = SimMath.cosDeg(rot);
        float s = SimMath.sinDeg(rot);
        float turned = rot - bindRotDeg;

        float colX = hx + colliderLocalX * c - colliderLocalY * s;
        float colY = hy + colliderLocalX * s + colliderLocalY * c;

        for (int i = 0; i < strands.size(); i++) {
            Strand st = strands.get(i);
            float ax = hx + st.localX * c - st.localY * s;
            float ay = hy + st.localX * s + st.localY * c;
            st.chain.anchor(ax, ay);
            st.chain.rootDirDeg(st.restDirLocalDeg + rot);
            if (colliderRadius > 0f) {
                st.chain.collider(colX, colY, colliderRadius);
            }

            // Per-strand turbulence, so the bundle never moves in unison
            // (STYLE.md 10 fails a pass on sight of uniform hair motion). Two
            // incommensurate sine terms per axis with a per-strand phase: cheap,
            // deterministic, and with no shared period for the eye to lock onto.
            float amp = st.turbulence * turbulenceScale;
            float w = 2f * (float) Math.PI * st.turbFreq * timeSeconds + st.turbPhase;
            float tx = amp * (float) (Math.sin(w) + 0.5 * Math.sin(w * 2.13 + 1.7 * st.turbPhase));
            float ty = amp * 0.55f * (float) Math.sin(w * 1.37 + 2.3 * st.turbPhase);
            st.chain.wind((windX + tx) * st.windScale, (windY + ty) * st.windScale);
        }
        this.turnedLast = turned;
    }

    private float turnedLast;

    /** How far the carrier bone has turned from its bind rotation, for debug overlays. */
    public float carrierTurnDeg() {
        return turnedLast;
    }

    public float time() {
        return time;
    }

    /** Registers every strand with a solver. */
    public void register(VerletSolver solver) {
        for (Strand st : strands) {
            solver.add(st.chain);
        }
    }

    /** Lays every strand out along its rest shape from the current anchors, with no velocity. */
    public void reset() {
        for (Strand st : strands) {
            st.chain.reset();
        }
    }
}
