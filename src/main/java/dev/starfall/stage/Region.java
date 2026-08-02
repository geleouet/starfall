package dev.starfall.stage;

import dev.starfall.combat.Force;

/**
 * The simulated surfaces an impulse can be aimed at, each with the anchor it
 * trails and how far behind that anchor it runs.
 *
 * <p><b>Why the staging layer drives the cloth and hair at all.</b>
 * {@code docs/system3-debt.md} records the consequence of it not doing so:
 * "System 4 must express impact through <em>how cloth trails</em>, because 7.1
 * bans freeze and shake -- and it has nothing to express it with." STYLE.md 7.1
 * bans hitstop, screen shake and impact freeze outright, and 7.3 replaces them
 * with ink and with "a change in how cloth trails". That change has to be
 * commanded by something that knows a blow landed, and the simulation does not.
 *
 * <p><b>The lag numbers are STYLE.md 7.1's, with their anchors attached.</b>
 * "Cloth trails the body by ~4-8 frames, hair tips by ~8-14" -- at 60 Hz that is
 * 0.067-0.133 s and 0.133-0.233 s. The hem is given 6 frames <em>behind the
 * hips</em>; the sleeve 5 frames <em>behind the wrist</em>, which is itself 0.27 s
 * behind the hips, so the sleeve's net lag behind the pelvis is 0.35 s. Those two
 * readings differ by 3.5x, which is precisely the factor 7.1 warns about, and
 * {@link #netLagBehindHips()} exists so both numbers are printable and neither can
 * be quoted alone.
 */
public enum Region {

    /**
     * The skirt and hem of the outer robe. Trails the pelvis, because that is what
     * it hangs from and what carries it through a knockback.
     */
    CLOTH_HEM(LagAnchor.HIPS, 6.0 / 60.0),

    /**
     * The sleeve of the sword arm. Trails the wrist -- STYLE.md 7.1's own example
     * of the anchor that changes the answer.
     */
    CLOTH_SLEEVE(LagAnchor.WRIST, 5.0 / 60.0),

    /**
     * The hair. STYLE.md 4: "hair leads and lags the head. On a direction change,
     * tips should still be travelling the old way for a noticeable beat." 11 frames
     * sits in the middle of 7.1's 8-14 band, and {@code system3-debt.md} verified
     * 7-9 frames behind the hips in delivered pixels, so this is the number that
     * layer already produces rather than a new one.
     */
    HAIR(LagAnchor.HEAD, 11.0 / 60.0);

    private final LagAnchor anchor;
    private final double lag;

    Region(LagAnchor anchor, double lag) {
        this.anchor = anchor;
        this.lag = lag;
    }

    /** What this region trails. Never optional -- see {@link LagAnchor}. */
    public LagAnchor anchor() {
        return anchor;
    }

    /** Seconds behind {@link #anchor()}. This is the number STYLE.md 7.1 quotes. */
    public double lag() {
        return lag;
    }

    /**
     * Seconds behind the hips: this region's own lag plus however far its anchor
     * already is. The other defensible reading of the same behaviour, printed
     * beside the first so a review can check which one a claim used.
     */
    public double netLagBehindHips() {
        return lag + anchor.behindHips();
    }

    /**
     * How hard an impulse of {@code force} hits this region, 0 to 1.
     *
     * <p>The scale is {@link Force}'s own -- drive, not speed -- so a shove really
     * does move the cloth less than a stride does. STYLE.md 7.2 asks for exactly
     * that inversion and {@code Force}'s own note explains it: "a launch is the
     * failure mode; a deliberate stride has more muscle behind it than being
     * carried does."
     *
     * <p>Hair takes more of any given force than cloth does, because it is lighter
     * and because STYLE.md 4 wants the escapees to move dramatically. That is a
     * mass ratio, not a second aesthetic rule.
     */
    public double magnitude(Force force) {
        double drive = switch (force) {
            case NONE -> 0.10;
            case DRIFT -> 0.38;
            case DRIVE -> 0.62;
            case HEADLONG -> 1.00;
        };
        double mass = this == HAIR ? 1.25 : this == CLOTH_SLEEVE ? 1.05 : 0.90;
        return Math.min(1.0, drive * mass);
    }
}
