package dev.starfall.rig;

import dev.starfall.anim.Pose;
import dev.starfall.stage.Stance;

/**
 * STYLE.md 4b.6's four expression channels — brow, eyelid, jaw, gaze — as damped
 * conditions on the three face bones.
 *
 * <h2>Reactive, not clipped</h2>
 *
 * <p>Targets come from combat state and nothing else: the expression table below
 * is keyed on {@link Stance}, which is the channel the scheduler already raises
 * from real engine events (a strike gathers, a parry braces, a hit yields, death
 * slackens), and the gaze target is the anchor the schedule already authors on
 * every {@code PoseChange} — the channel {@code Director#poseChannel} had recorded
 * as "a gap worth naming rather than papering over" since System 4. There are no
 * canned clips anywhere in this class.
 *
 * <h2>It settles, it never snaps</h2>
 *
 * <p>Every channel approaches its target first-order — {@code x += (t − x)·(1 −
 * e^(−dt/τ))} — which has zero overshoot and zero returns, the strongest form of
 * §7.2's "at most one soft return". The taus are staggered so the four channels
 * never arrive together (§7.0.3, §10's last row): lid 0.075 s, brow 0.10, gaze
 * 0.11, jaw 0.14 — first-order settle to 2% is ≈ 4τ, landing the four at 0.30 /
 * 0.40 / 0.44 / 0.56 s, spread across §4b.6's own 0.3–0.6 s band rather than
 * sitting on one point of it.
 */
public final class FaceRig {

    /** Settle-to-2% ≈ 4τ: 0.30 s — a lid is the fastest thing on a face. */
    public static final float TAU_LID = 0.075f;
    /** 0.40 s. */
    public static final float TAU_BROW = 0.10f;
    /** 0.44 s. */
    public static final float TAU_GAZE = 0.11f;
    /** 0.56 s — the jaw is the slowest, the heaviest thing on a face. */
    public static final float TAU_JAW = 0.14f;

    /** One stance's face: brow −1 raised .. +1 drawn-down; lid 0 closed .. 1.15 wide; jaw 0 closed .. 1 open. */
    public record Set(float brow, float lid, float jaw) {

        static Set lerp(Set a, Set b, float u) {
            return new Set(a.brow + (b.brow - a.brow) * u,
                    a.lid + (b.lid - a.lid) * u,
                    a.jaw + (b.jaw - a.jaw) * u);
        }
    }

    /**
     * The expression each stance holds. A condition table, exactly like
     * {@code Stances}' trunk shapes and for the same reason (system2-debt E1:
     * a held value, not an event).
     */
    public static Set of(Stance stance) {
        return switch (stance) {
            case READY -> new Set(0.10f, 1.00f, 0.05f);          // level, watchful
            case GATHERING -> new Set(0.70f, 0.55f, 0.15f);      // drawn down, narrowed: effort and focus
            case GUARD_RAISED -> new Set(0.45f, 1.10f, 0.10f);   // alarm under control: wide over a set jaw
            case BRACED -> new Set(0.85f, 0.45f, 0.30f);         // everything clenched into the contact
            case YIELDING -> new Set(-0.35f, 0.20f, 0.75f);      // pain: brow lifts, eye closes, jaw opens
            case CARRIED -> new Set(-0.60f, 1.15f, 0.65f);       // thrown: wide, open, helpless
            case PASSING -> new Set(0.25f, 0.80f, 0.05f);        // narrowed through the crossing
            case DRIED -> new Set(0.00f, 0.55f, 0.10f);          // the pigment dries; the face half-stills
            case SLACK -> new Set(-0.15f, 0.06f, 0.95f);         // going: closed, slack — 4b.6's death row
        };
    }

    private float brow, lid = 1f, jaw;
    private float browT = 0.10f, lidT = 1f, jawT = 0.05f;
    private float gazeX, gazeY;
    private float gazeXT, gazeYT;

    /** Blended stance target, same blend the trunk gets, so face and body read one condition. */
    public void express(Stance from, Stance to, float u) {
        Set s = Set.lerp(of(from), of(to), Math.max(0f, Math.min(1f, u)));
        browT = s.brow();
        lidT = s.lid();
        jawT = s.jaw();
    }

    /**
     * Where the eye is asked to look, as a unit-ish direction in the FIGURE's
     * frame (+x is the way the figure faces). The caller resolves the schedule's
     * gaze anchor against the head and hands in the direction; this class only
     * settles it.
     */
    public void gazeToward(float dirX, float dirY) {
        float mx = Math.max(-1f, Math.min(1f, dirX));
        float my = Math.max(-1f, Math.min(1f, dirY));
        gazeXT = mx;
        gazeYT = my;
    }

    /** First-order approach on every channel. Call once per frame with real dt. */
    public void update(float dt) {
        if (dt <= 0f) {
            return;
        }
        brow += (browT - brow) * gain(dt, TAU_BROW);
        lid += (lidT - lid) * gain(dt, TAU_LID);
        jaw += (jawT - jaw) * gain(dt, TAU_JAW);
        gazeX += (gazeXT - gazeX) * gain(dt, TAU_GAZE);
        gazeY += (gazeYT - gazeY) * gain(dt, TAU_GAZE);
    }

    /** Scene setup only: lands every channel on its target with no history. */
    public void snap() {
        brow = browT;
        lid = lidT;
        jaw = jawT;
        gazeX = gazeXT;
        gazeY = gazeYT;
    }

    private static float gain(float dt, float tau) {
        return 1f - (float) Math.exp(-dt / tau);
    }

    /**
     * Writes the three bones' deltas. Ranges are small on purpose — the whole
     * head is ~68 px at the intimate framing, so 0.012 world units is under
     * three pixels, and an expression bigger than that is a cartoon.
     *
     * <p>The eyelid is the eye bone's scaleY about its own origin, which the
     * skeleton authors AT the upper lash line: closing collapses sclera, lash
     * and iris up into the lid the way a lid actually comes down, and 4b.4's
     * "degrades to dark iris + specular" holds at every opening.
     */
    public void write(Pose pose) {
        pose.set("brow", -0.002f * brow, -0.009f * brow, -7f * brow);
        // 0.013, not pass 1's 0.009. The pass-1 review measured 0.009 world
        // units as <= 2 px of eye travel at the closest shot the game delivers
        // (66-68 px of skull), i.e. a gaze that was "not observable in the
        // capture". 0.013 is 2.9 px at that framing — about half the eye's own
        // length, the iris visibly crossing the sclera — and still under the
        // "an expression bigger than three pixels is a cartoon" ceiling above.
        // FaceWindowTest.theGazeMovesTheEyeByMoreThanTwoPixels holds the floor.
        pose.set("eye", 0.013f * gazeX, 0.006f * gazeY - 0.001f * (1.15f - lid), 0f,
                1f, Math.max(0.06f, lid));
        pose.set("jaw", 0f, -0.004f * jaw, -11f * jaw);
    }

    public float brow() {
        return brow;
    }

    public float lid() {
        return lid;
    }

    public float jaw() {
        return jaw;
    }

    public float gazeX() {
        return gazeX;
    }

    public float gazeY() {
        return gazeY;
    }
}
