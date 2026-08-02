package dev.starfall.combat;

/**
 * What a beat is about: whose gesture it is, and the run of tiles it happens
 * over.
 *
 * <p><b>Why the engine states something derivable.</b> combat-design.md 3d.5
 * lists this as missing and calls it "derivable but never stated", and STYLE.md 9
 * is the reason it has to be stated: on execution the camera glides toward the
 * exchange over about half a second, and something has to know what the exchange
 * <em>is</em> before the beat resolves. Deriving it from the atomic events means
 * deriving it from events that have not been emitted yet -- a Cut into empty air
 * produces a {@code Whiffed} and no target at all, and the camera still has to
 * have moved somewhere. So the subject is declared at the top of the beat,
 * alongside {@link Phases} and {@link Overlap}, which is the only place a camera
 * can read it in time to ease rather than cut.
 *
 * <p><b>Why a span and not a tile.</b> STYLE.md 9 also requires the wide framing
 * to be derived from lane length, and the push-in to be "a <em>small</em> move on
 * short lanes and a <em>large</em> one on long ones". A Runner collapsing twelve
 * tiles and a Wisp stepping one are the same subject and completely different
 * shots. The span is what tells them apart: the camera frames
 * {@code fromTile..toTile} and gets the right push-in for free.
 *
 * @param subject  the body whose beat this is
 * @param fromTile low end of the run of tiles the beat happens over, inclusive
 * @param toTile   high end, inclusive
 */
public record Focus(int subject, int fromTile, int toTile) {

    public Focus {
        if (toTile < fromTile) {
            int swap = fromTile;
            fromTile = toTile;
            toTile = swap;
        }
    }

    static Focus of(Combatant subject, int a, int b, Lane lane) {
        return new Focus(subject.id(), clamp(a, lane), clamp(b, lane));
    }

    private static int clamp(int tile, Lane lane) {
        return Math.max(0, Math.min(lane.last(), tile));
    }

    /** How many tiles the camera has to hold. One for a beat that goes nowhere. */
    public int span() {
        return toTile - fromTile + 1;
    }

    /** The tile to centre on. Rounds down, so it is stable under replay. */
    public int centre() {
        return fromTile + (toTile - fromTile) / 2;
    }

    @Override
    public String toString() {
        return "#" + subject + " over " + fromTile + ".." + toTile;
    }
}
