package dev.starfall.ik;

/**
 * An angular limit on one joint, expressed in the same space as
 * {@link dev.starfall.anim.Bone#rotDeg}: degrees of the bone's own local
 * rotation, relative to its parent. So a constraint is authored by reading the
 * numbers already in the rig -- "forearmL sits at -10 at bind, allow -160 to 5"
 * -- with no separate coordinate convention to get wrong.
 *
 * <p><b>The limit is soft on purpose.</b> The obvious implementation is
 * {@code Math.max(lo, Math.min(hi, deg))}, and the obvious implementation is
 * banned here. A hard clamp is continuous but its derivative jumps from 1 to 0
 * at the bound, so a limb sweeping into its limit stops dead on one frame and
 * that reads as the mechanical harshness STYLE.md 7.2 rules out. Instead the
 * angle eases into the bound over a soft band and approaches it asymptotically:
 * the joint decelerates into its limit the way a real one does when the
 * ligament takes up.
 *
 * <p>Two properties are what make this safe to apply *inside* a solver rather
 * than as a post-pass, and both are pinned by tests:
 * <ul>
 *   <li>{@link #apply} is monotone and 1-Lipschitz, so it can never turn a
 *       continuous input into a discontinuous output -- feeding a solver's
 *       intermediate angles through it cannot introduce a pop that was not
 *       already there;</li>
 *   <li>it is the identity outside the soft band, so an inactive constraint
 *       costs nothing in accuracy -- a chain that is nowhere near its limits
 *       solves exactly as if it were unconstrained.</li>
 * </ul>
 *
 * <p>Ranges must lie within (-180, 180] and must not straddle the wrap point;
 * relative joint angles are measured by shortest arc, so a range spanning +-180
 * has no consistent meaning.
 */
public final class IkConstraint {

    private static final IkConstraint FREE = new IkConstraint();

    /** Default soft band as a fraction of the range. ~8% eases visibly without eating much travel. */
    private static final float DEFAULT_SOFTNESS = 0.08f;

    private final boolean free;
    private final float minDeg;
    private final float maxDeg;
    private final float softness;

    private IkConstraint() {
        this.free = true;
        this.minDeg = -180f;
        this.maxDeg = 180f;
        this.softness = 0f;
    }

    private IkConstraint(float minDeg, float maxDeg, float softness) {
        if (!(maxDeg > minDeg)) {
            throw new IllegalArgumentException("IkConstraint needs maxDeg > minDeg, got [" + minDeg + ", " + maxDeg + "]");
        }
        if (minDeg <= -180f || maxDeg > 180f) {
            throw new IllegalArgumentException(
                    "IkConstraint range must lie inside (-180, 180]; a range straddling the wrap point is ambiguous. Got ["
                            + minDeg + ", " + maxDeg + "]");
        }
        this.free = false;
        this.minDeg = minDeg;
        this.maxDeg = maxDeg;
        this.softness = IkMath.clamp(softness, 0f, 0.5f);
    }

    /** No limit. The default for every joint. */
    public static IkConstraint free() {
        return FREE;
    }

    public static IkConstraint range(float minDeg, float maxDeg) {
        return new IkConstraint(minDeg, maxDeg, DEFAULT_SOFTNESS);
    }

    /**
     * @param softness fraction of the total range spent easing into each bound,
     *                 clamped to [0, 0.5]. Zero degenerates to a hard clamp and
     *                 is offered only so a test can demonstrate why it is wrong.
     */
    public static IkConstraint range(float minDeg, float maxDeg, float softness) {
        return new IkConstraint(minDeg, maxDeg, softness);
    }

    /** A symmetric limit about a rest angle -- the usual way an elbow or a spine segment is authored. */
    public static IkConstraint around(float restDeg, float spreadDeg) {
        return range(restDeg - spreadDeg, restDeg + spreadDeg);
    }

    public boolean isFree() {
        return free;
    }

    public float minDeg() {
        return minDeg;
    }

    public float maxDeg() {
        return maxDeg;
    }

    /** Midpoint of the range; the angle a constrained joint relaxes toward when it has no other preference. */
    public float restDeg() {
        return free ? 0f : 0.5f * (minDeg + maxDeg);
    }

    /**
     * Maps a requested local angle to the nearest angle the joint will accept.
     * Monotone, 1-Lipschitz, identity in the interior, and strictly inside
     * [min, max] for every input.
     */
    public float apply(float deg) {
        if (free) {
            return deg;
        }
        float band = softness * (maxDeg - minDeg);
        if (band <= IkMath.EPS) {
            return IkMath.clamp(deg, minDeg, maxDeg);
        }
        // Two independent one-sided softenings rather than one blended function:
        // with band <= half the range the knees cannot cross, so each softening
        // sees the other's output already inside its own identity segment.
        float v = IkMath.softCeil(deg, maxDeg - band, maxDeg);
        return IkMath.softFloor(v, minDeg + band, minDeg);
    }

    @Override
    public String toString() {
        return free ? "IkConstraint[free]" : "IkConstraint[" + minDeg + ", " + maxDeg + " soft " + softness + "]";
    }
}
