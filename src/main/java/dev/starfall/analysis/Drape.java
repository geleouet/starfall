package dev.starfall.analysis;

import java.util.List;

/**
 * Drape excursion: how far a hanging panel gets <em>away from</em> the body it hangs on.
 *
 * <p>This is STYLE.md §7.1's cloth criterion, and it exists because the statistic it
 * replaced could not be measured through any rectangle on this figure. The history is worth
 * carrying with the code, because three passes were spent inside it:
 *
 * <ul>
 *   <li>Passes 1 and 2 graded cloth by a <em>reversal-time lag</em> read off the particle,
 *       and the picture never showed it.</li>
 *   <li>Pass 3 built {@code SceneProbe}, proved the particle lag had doubled while the
 *       delivered pixels moved by 0.02 frames, and then proved <em>why</em>: with the cloth
 *       welded rigid the graded box still read +0.34 frames of the +0.87 it was scoring, so
 *       the statistic's whole dynamic range there was half a frame.</li>
 *   <li>§7.1's own answer: <i>"a panel answers a body translation with a rotation about its
 *       pin, and registration fits one translation per box, so the panel's silhouette prints
 *       as a picture of the body's velocity — and velocity leads position by a quarter
 *       period."</i> A correctly trailing hem therefore reports a <em>negative</em> lag. The
 *       phase statistic is ill-posed as a form, not merely mis-boxed.</li>
 * </ul>
 *
 * <p>So cloth is graded by displacement instead. {@code D(t) = pos(cloth) − pos(anchor)}
 * along one axis, referenced to its own value at the first frame so it is an excursion and
 * not an offset. A garment welded to the hips scores zero by construction, which is the
 * property the phase statistic did not have.
 *
 * <h2>The three gates</h2>
 *
 * <ol>
 *   <li><b>Amplitude.</b> {@code peak|D| ≥ 1.5 ×} the anchor's own peak-to-peak travel in
 *       the window. Cloth that answers a body translation with less than the translation is
 *       not draping, it is being carried.</li>
 *   <li><b>Return.</b> After the anchor reverses, {@code |D|} must fall to ≤25% of its peak
 *       within 0.15–0.25 s, with at most one sign change exceeding 20% of peak. This is
 *       §7.2's "at most one soft return", asked on the relative signal, where — unlike on
 *       the absolute one — it is well posed.</li>
 *   <li><b>Rigid control.</b> The same measurement with {@code --clamp cloth} must score
 *       ≤0.15×. Without it gate 1 is unfalsifiable: §7.1 records a rigidly skinned torso box
 *       manufacturing +2.32 frames of lag with no simulation in the scene at all.</li>
 * </ol>
 *
 * <p>Box-invariance was the other reason to prefer it: moving the rectangle 6 px changes the
 * excursion by about 0.01 px, where the phase statistic moved by whole frames.
 */
public final class Drape {

    /** Gate 1: peak |D| as a multiple of the anchor's travel. */
    public static final double AMPLITUDE_GATE = 1.5;
    /** Gate 2: |D| must fall to this fraction of its peak. */
    public static final double RETURN_FRACTION = 0.25;
    /** Gate 2: a sign change smaller than this fraction of peak is not a return. */
    public static final double SIGN_CHANGE_FRACTION = 0.20;
    /** Gate 2, seconds after the anchor's reversal. */
    public static final double RETURN_MIN_SECONDS = 0.15;
    public static final double RETURN_MAX_SECONDS = 0.25;
    /** Gate 3: a rigid control may score no more than this. */
    public static final double CONTROL_GATE = 0.15;

    public final Track cloth;
    public final Track anchor;
    public final double fps;

    /** {@code pos(cloth) − pos(anchor)}, referenced to frame 0. */
    public final double[] d;
    /** Peak |D| in px, and the frame it happens on. */
    public final double peak;
    public final int peakFrame;
    /** The anchor's peak-to-peak travel along the same axis, in px. */
    public final double anchorTravel;
    /** Frame of the anchor's dominant velocity reversal, or NaN. */
    public final double anchorReversal;
    /** Frame at which |D| first falls to {@link #RETURN_FRACTION} of peak after the reversal, or NaN. */
    public final double returnFrame;
    /** Sign changes of D after the anchor's reversal whose following excursion exceeds 20% of peak. */
    public final int signChanges;
    /** True when |D| falls without rising again up to the first sign change — §7.2's "monotone out". */
    public final boolean monotoneReturn;
    /** Largest |D| after the first sign change, as a fraction of peak. §7.2's one soft return. */
    public final double overshootFraction;

    private Drape(Track cloth, Track anchor, double fps) {
        this.cloth = cloth;
        this.anchor = anchor;
        this.fps = fps;
        int n = Math.min(cloth.pos.length, anchor.pos.length);
        this.d = new double[n];
        double d0 = cloth.pos[0] - anchor.pos[0];
        for (int i = 0; i < n; i++) {
            d[i] = (cloth.pos[i] - anchor.pos[i]) - d0;
        }
        int pf = 0;
        for (int i = 0; i < n; i++) {
            if (Math.abs(d[i]) > Math.abs(d[pf])) {
                pf = i;
            }
        }
        this.peakFrame = pf;
        this.peak = Math.abs(d[pf]);

        double lo = Double.MAX_VALUE;
        double hi = -Double.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            lo = Math.min(lo, anchor.pos[i]);
            hi = Math.max(hi, anchor.pos[i]);
        }
        this.anchorTravel = hi - lo;

        Track.Crossing dom = anchor.dominantCrossing();
        this.anchorReversal = dom == null ? Double.NaN : dom.frame();

        // The return is measured from the peak that follows the anchor's reversal, because
        // that is the excursion the reversal caused. Before the reversal the panel is being
        // dragged, and how far it got is gate 1's business, not gate 2's.
        int from = Double.isNaN(anchorReversal) ? pf : Math.max(pf, (int) Math.ceil(anchorReversal));
        int lateFrame = from;
        for (int i = from; i < n; i++) {
            if (Math.abs(d[i]) > Math.abs(d[lateFrame])) {
                lateFrame = i;
            }
        }
        double latePeak = Math.abs(d[lateFrame]);
        double rf = Double.NaN;
        for (int i = lateFrame; i < n; i++) {
            if (Math.abs(d[i]) <= RETURN_FRACTION * latePeak) {
                rf = i;
                break;
            }
        }
        this.returnFrame = rf;

        // §7.2 asks for "at most one soft return", and the two halves of that phrase are
        // measured separately. Monotone until the signal crosses zero -- a hem that stalls
        // and comes back out on the way home is ringing. Then, once it has crossed, how far
        // it goes on the far side, which is the "soft" half: a crossing is allowed, an
        // excursion past 20% of peak on the other side is the mechanical rebound §10 fails
        // on sight. Requiring |D| itself to be monotone would forbid the crossing outright
        // and is a stricter rule than the one written down.
        double sign0 = Math.signum(d[lateFrame]);
        int firstCross = -1;
        boolean mono = true;
        for (int i = lateFrame + 1; i < n; i++) {
            if (sign0 != 0 && Math.signum(d[i]) == -sign0 && Math.abs(d[i]) > 1e-9) {
                firstCross = i;
                break;
            }
            if (Math.abs(d[i]) > Math.abs(d[i - 1]) + 1e-9) {
                mono = false;
                break;
            }
        }
        this.monotoneReturn = mono;

        double over = 0;
        int changes = 0;
        double sign = 0;
        for (int i = lateFrame; i < n; i++) {
            double v = d[i];
            if (firstCross >= 0 && i >= firstCross) {
                over = Math.max(over, Math.abs(v));
            }
            if (Math.abs(v) < 1e-9) {
                continue;
            }
            double s = Math.signum(v);
            if (sign != 0 && s != sign) {
                changes++;
            }
            sign = s;
        }
        this.overshootFraction = latePeak <= 1e-9 ? 0 : over / latePeak;
        this.signChanges = changes;
    }

    public static Drape measure(Track cloth, Track anchor, double fps) {
        return new Drape(cloth, anchor, fps);
    }

    /** Gate 1's number: peak excursion as a multiple of the anchor's own travel. */
    public double ratio() {
        return anchorTravel <= 1e-9 ? Double.NaN : peak / anchorTravel;
    }

    public boolean gate1() {
        return ratio() >= AMPLITUDE_GATE;
    }

    /** Seconds between the anchor's reversal and |D| falling to a quarter of peak, or NaN. */
    public double returnSeconds() {
        if (Double.isNaN(returnFrame) || Double.isNaN(anchorReversal) || fps <= 0) {
            return Double.NaN;
        }
        return (returnFrame - anchorReversal) / fps;
    }

    public boolean gate2() {
        double s = returnSeconds();
        return !Double.isNaN(s) && s >= RETURN_MIN_SECONDS && s <= RETURN_MAX_SECONDS
                && signChanges <= 1 && overshootFraction <= SIGN_CHANGE_FRACTION && monotoneReturn;
    }

    public String describe() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("drape excursion  D(t) = pos(%s) - pos(%s), axis=%s(%.2f,%.2f), method=%s%n",
                cloth.name, anchor.name, cloth.axis, cloth.axisX, cloth.axisY, cloth.method));
        sb.append(String.format("  cloth  %-12s %s%n", cloth.name, cloth.rect.describe()));
        sb.append(String.format("  anchor %-12s %s%n", anchor.name, anchor.rect.describe()));
        sb.append("  D    ");
        for (double v : d) {
            sb.append(String.format("%+.2f ", v));
        }
        sb.append('\n');
        sb.append(String.format("  peak |D| %.2f px at frame %d;  anchor travel %.2f px%n",
                peak, peakFrame, anchorTravel));
        sb.append(String.format("  GATE 1 amplitude   %.2fx  (need >= %.2fx)  -> %s%n",
                ratio(), AMPLITUDE_GATE, gate1() ? "PASS" : "FAIL"));
        if (Double.isNaN(anchorReversal)) {
            sb.append("  GATE 2 return      anchor has no reversal above its gate in this window -> NOT MEASURABLE\n");
        } else {
            sb.append(String.format("  GATE 2 return      anchor reverses at frame %.2f; |D| <= %.0f%% of peak at frame %s"
                            + " = %.3f s later; %d sign change(s) over %.0f%% of peak; %s%n",
                    anchorReversal, RETURN_FRACTION * 100,
                    Double.isNaN(returnFrame) ? "never" : String.format("%.0f", returnFrame),
                    returnSeconds(), signChanges, SIGN_CHANGE_FRACTION * 100,
                    monotoneReturn ? "monotone out" : "NOT monotone out"));
            sb.append(String.format("                     overshoot past zero %.0f%% of peak (limit %.0f%%)%n",
                    overshootFraction * 100, SIGN_CHANGE_FRACTION * 100));
            sb.append(String.format("                     need %.2f-%.2f s and <= 1 sign change -> %s%n",
                    RETURN_MIN_SECONDS, RETURN_MAX_SECONDS, gate2() ? "PASS" : "FAIL"));
        }
        return sb.toString();
    }

    /** Gate 3, printed against a control run of the same window with {@code --clamp cloth}. */
    public static String describeControl(Drape live, Drape control) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("  GATE 3 rigid control  %.2fx  (need <= %.2fx)  -> %s%n",
                control.ratio(), CONTROL_GATE, control.ratio() <= CONTROL_GATE ? "PASS" : "FAIL"));
        sb.append(String.format("                        control peak |D| %.2f px against live %.2f px"
                        + " -- the statistic's dynamic range on this box is %.2f px%n",
                control.peak, live.peak, live.peak - control.peak));
        return sb.toString();
    }

    /** Frames at which either track's registration hit its search window. */
    public List<Integer> clipped() {
        return cloth.clipped.isEmpty() ? anchor.clipped : cloth.clipped;
    }
}
