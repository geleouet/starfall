package dev.starfall.stage;

/**
 * What a trailing thing trails.
 *
 * <p>STYLE.md 7.1, in the paragraph added after a review could not falsify a lag
 * claim: <i>"Name the anchor, or the number means nothing. A hem trails the hips;
 * a sleeve trails the wrist, which is itself already far behind the hips because
 * it hangs off an IK chain carrying its own settle. Both readings are defensible
 * and they differ by a factor of three, so a lag figure quoted without its anchor
 * is unfalsifiable. State it every time."</i>
 *
 * <p>This enum is that instruction turned into a type. No impulse can be emitted
 * without one, so no lag in this layer can be quoted without its anchor.
 */
public enum LagAnchor {

    /** The pelvis. The earliest thing in the body to arrive, and the natural datum. */
    HIPS(Timing.ARRIVAL_HIPS),

    /** The skull. What hair hangs off. */
    HEAD(Timing.ARRIVAL_HEAD),

    /** The end of the sword arm. What a sleeve hangs off, and 0.27 s behind the hips. */
    WRIST(Timing.ARRIVAL_WRIST);

    private final double arrival;

    LagAnchor(double arrival) {
        this.arrival = arrival;
    }

    /** When this anchor itself comes to rest, in seconds. */
    public double arrival() {
        return arrival;
    }

    /** How far behind the hips this anchor already is before anything hangs off it. */
    public double behindHips() {
        return arrival - HIPS.arrival();
    }
}
