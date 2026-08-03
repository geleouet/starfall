package dev.starfall.stage;

import dev.starfall.combat.ContactPoint;
import dev.starfall.combat.Focus;
import dev.starfall.combat.Lane;
import dev.starfall.combat.Meeting;

/**
 * The map from the engine's ordinals to space: tiles to world units, the three
 * contact heights to a height on a figure, and lane length to a camera framing.
 *
 * <p>Immutable and lane-scoped, because the only spatial parameter the engine has
 * is lane length and it does not change during an encounter.
 *
 * <h2>The camera law, which is the part with a rubric behind it</h2>
 *
 * <p>combat-design.md 1.6, verbatim: <i>"The planning framing must fit the lane, so
 * a 5-tile lane is already near-intimate while a 15-tile lane is genuinely wide.
 * The push-in is therefore a small move on short lanes and a large one on long
 * lanes -- which is correct, because a short lane has no approach to dramatise and
 * a long one has nothing but. The camera should derive its wide framing from lane
 * length rather than using a fixed value."</i>
 *
 * <p>So there are two independent framings and only one of them scales:
 *
 * <ul>
 *   <li>{@link #planning()} is the whole lane plus a margin of paper. It grows
 *       linearly with lane length, from 6.5 tiles at the short end to 16.5 at the
 *       long one.</li>
 *   <li>{@link #execution(Focus)} is the intimacy of reference images 3, 4 and 5 --
 *       two figures large in frame with the blades crossing near centre -- and it
 *       is the <em>same shot</em> on every lane, because a duel does not get less
 *       intimate because the corridor behind it is longer. It widens only when the
 *       beat itself spans more tiles than the intimate shot holds, which is the
 *       Runner case combat-design.md 3d.5 asks for.</li>
 * </ul>
 *
 * <p>The push-in is the ratio between them, and it falls out: 2.0x on a five-tile
 * lane, 5.2x on a fifteen-tile one. Nothing had to be tuned to make the move small
 * on a knife fight and large on an approach; deriving the wide shot from the lane
 * and the close shot from the exchange does it on its own.
 */
public final class Stage {

    /** One tile of the Fold of the World, in world units. */
    public static final double TILE_WIDTH = 1.0;

    /**
     * Standing figure height, matched to {@code SamuraiRig}: feet on the ground
     * plane, hips at 0.98, head at about 1.7.
     */
    public static final double FIGURE_HEIGHT = 1.70;

    /** {@link ContactPoint.Height#HIGH}: the head and shoulder line. */
    public static final double Y_HIGH = 1.45;

    /** {@link ContactPoint.Height#MIDDLE}: chest to hip. */
    public static final double Y_MIDDLE = 1.00;

    /** {@link ContactPoint.Height#LOW}: hem and ground. */
    public static final double Y_LOW = 0.20;

    /** The pelvis. */
    public static final double Y_HIP = 0.98;

    /** The skull. */
    public static final double Y_HEAD = 1.62;

    /** Half a body's width: how far off its own tile centre a contact on it sits. */
    public static final double BODY_HALF = 0.28;

    /** How far in front of itself a body holds its blade at guard. */
    public static final double GUARD_REACH = 0.46;

    /** How far apart the feet are planted. */
    public static final double STANCE_WIDTH = 0.26;

    /** Paper beyond the last tile that the planning framing still holds, in tiles. */
    public static final double EDGE_MARGIN = 0.75;

    /**
     * The intimacy of reference images 3, 4 and 5, in tiles. Two figures and their
     * crossed blades, and nothing else. STYLE.md 4b.0 makes this load-bearing --
     * "the camera push-in of 9 is now load-bearing. It is the only moment the face
     * is legible at all, so the execution framing must get genuinely close" -- and
     * 3b.1 gives it a second job, since the cloth's fine octaves only resolve as the
     * camera approaches.
     */
    public static final double INTIMACY_TILES = 3.2;

    /** Paper either side of a beat's own span, when the beat is wider than the intimate shot. */
    public static final double EXEC_MARGIN = 0.60;

    private final int laneLength;

    public Stage(int laneLength) {
        if (laneLength < Lane.MIN_LENGTH || laneLength > Lane.MAX_LENGTH) {
            throw new IllegalArgumentException("lane length must be within "
                    + Lane.MIN_LENGTH + ".." + Lane.MAX_LENGTH + ": " + laneLength);
        }
        this.laneLength = laneLength;
    }

    public static Stage of(Lane lane) {
        return new Stage(lane.length());
    }

    public int laneLength() {
        return laneLength;
    }

    // -- space -----------------------------------------------------------------

    /** World x of the centre of a (possibly fractional) tile. */
    public double tileX(double tile) {
        return tile * TILE_WIDTH;
    }

    /** World y of one of the engine's three contact heights. */
    public double heightY(ContactPoint.Height h) {
        return switch (h) {
            case HIGH -> Y_HIGH;
            case MIDDLE -> Y_MIDDLE;
            case LOW -> Y_LOW;
        };
    }

    /**
     * A {@link ContactPoint} placed in the world.
     *
     * <p>The side is the part that needs the board. {@link ContactPoint.Side} is
     * relative to the touched body's <em>own</em> facing -- "two bodies squared up
     * on a lane face opposite ways, so a single world-space side would be the
     * leading side of one and the trailing side of the other" -- so turning it into
     * a signed offset needs to know which way that body is turned, and that is the
     * one fact only {@link Standing} has.
     */
    public Anchor contact(ContactPoint cp, Standing standing) {
        int tile = standing.tile(cp.body());
        int step = standing.facing(cp.body()).step();
        double side = cp.side() == ContactPoint.Side.LEADING ? +1.0 : -1.0;
        double reach = cp.part() == ContactPoint.Part.BLADE ? GUARD_REACH : BODY_HALF;
        return new Anchor(cp.body(), Anchor.Site.CONTACT,
                tileX(tile) + step * side * reach, heightY(cp.height()));
    }

    /**
     * The single point in space a {@link Meeting}'s two named halves describe.
     *
     * <h2>Reconciling two names into one point is this layer's job, not the
     * engine's and not the rig's</h2>
     *
     * <p>{@link ContactPoint}'s own note: <i>"the renderer owns the mapping from
     * that to a point in space, and two renderers at different figure scales can
     * both honour it."</i> {@link Meeting}'s: <i>"a parry is two skeletons that
     * have to agree on one point in space."</i> Put together, the two halves are
     * two <em>descriptions</em> of one crossing and it is here that they become
     * one coordinate -- the midpoint of the two, which for two bodies squared up
     * on adjacent tiles is the exact middle of the gap between them, and stays
     * the exact middle however far apart the lane is drawn.
     *
     * <p>Pass 1 shipped both halves separately into two IK chains. They resolved
     * 0.08 world units apart, which is correct and was never the problem: what
     * went wrong is that each half was handed to a chain whose effector is the
     * <b>fist</b>, so the point two <em>hands</em> agreed on was 0.47 units short
     * of where either <em>blade</em> was. The site is now
     * {@link Anchor.Site#CROSSING} and it means what it says.
     */
    public Anchor crossing(Meeting meeting, Standing standing) {
        Anchor a = contact(meeting.onActor(), standing);
        Anchor b = contact(meeting.onTarget(), standing);
        return new Anchor(Anchor.NO_BODY, Anchor.Site.CROSSING,
                0.5 * (a.x() + b.x()), 0.5 * (a.y() + b.y()));
    }

    /** Where {@code body} holds its blade when it is not swinging it. */
    public Anchor guard(int body, Standing standing) {
        int step = standing.facing(body).step();
        return new Anchor(body, Anchor.Site.GUARD,
                tileX(standing.tile(body)) + step * GUARD_REACH, Y_MIDDLE);
    }

    /**
     * The loaded rest of the hand. Slightly nearer the body than the guard and a
     * little lower -- {@code system2-debt.md} E1 asks for a rest pose that already
     * reads as a body under load rather than a symmetric neutral.
     */
    public Anchor rest(int body, Standing standing) {
        int step = standing.facing(body).step();
        return new Anchor(body, Anchor.Site.REST,
                tileX(standing.tile(body)) + step * GUARD_REACH * 0.55, Y_MIDDLE - 0.16);
    }

    /**
     * Where the hand gathers during a wind-up: back over the trailing side and
     * high. STYLE.md 7.1 wants the anticipation long, and a gesture that gathers
     * nowhere has nothing to be long about.
     */
    public Anchor gather(int body, Standing standing) {
        int step = standing.facing(body).step();
        return new Anchor(body, Anchor.Site.REST,
                tileX(standing.tile(body)) - step * BODY_HALF * 0.65, Y_HIGH - 0.06);
    }

    /** The pelvis, leaning {@code lean} tiles toward whatever the beat is about. */
    public Anchor hipLeaning(int body, Standing standing, double towardTile, double lean) {
        double x = tileX(standing.tile(body));
        double dir = towardTile > standing.tile(body) ? 1.0 : towardTile < standing.tile(body) ? -1.0
                : standing.facing(body).step();
        return new Anchor(body, Anchor.Site.HIP, x + dir * lean, Y_HIP);
    }

    public Anchor hip(int body, Standing standing) {
        return new Anchor(body, Anchor.Site.HIP, tileX(standing.tile(body)), Y_HIP);
    }

    public Anchor head(int body, Standing standing) {
        return new Anchor(body, Anchor.Site.HEAD, tileX(standing.tile(body)), Y_HEAD);
    }

    /** The near foot's plant. The feet are the one thing that must not slide -- see {@code system2-debt.md}. */
    public Anchor footLead(int body, Standing standing) {
        int step = standing.facing(body).step();
        return new Anchor(body, Anchor.Site.FOOT_LEAD,
                tileX(standing.tile(body)) + step * STANCE_WIDTH * 0.5, 0.0);
    }

    public Anchor footTrail(int body, Standing standing) {
        int step = standing.facing(body).step();
        return new Anchor(body, Anchor.Site.FOOT_TRAIL,
                tileX(standing.tile(body)) - step * STANCE_WIDTH * 0.5, 0.0);
    }

    /** A point on the lane with no body attached: a bloom, a camera target, a whiffed stroke. */
    public Anchor onTile(double tile, ContactPoint.Height height) {
        return new Anchor(Anchor.NO_BODY, Anchor.Site.TILE, tileX(tile), heightY(height));
    }

    /**
     * Where {@code body}'s hand goes to reach {@code towardTile} at {@code height}:
     * the far edge of that tile from where the body stands, so a stroke into an
     * empty tile still travels somewhere real.
     */
    public Anchor reach(int body, double towardTile, ContactPoint.Height height, Standing standing) {
        double from = standing.tile(body);
        double sign = towardTile >= from ? +1.0 : -1.0;
        double x = tileX(towardTile) - sign * BODY_HALF;
        // Never let a stroke aimed at the body's own tile collapse onto the body.
        if (Math.abs(x - tileX(from)) < GUARD_REACH) {
            x = tileX(from) + standing.facing(body).step() * GUARD_REACH;
        }
        return new Anchor(body, Anchor.Site.CONTACT, x, heightY(height));
    }

    /** The gaze point: the head height of whatever this beat is about. */
    public Anchor gaze(int body, double towardTile) {
        return new Anchor(body, Anchor.Site.GAZE, tileX(towardTile), Y_HEAD);
    }

    /** Which way, along the lane, a blow from {@code fromTile} onto {@code toTile} travels. */
    public static double alongLane(int fromTile, int toTile) {
        return toTile == fromTile ? 0.0 : Math.signum((double) (toTile - fromTile));
    }

    // -- camera ----------------------------------------------------------------

    /**
     * STYLE.md 9's planning framing: "the full lane readable, figures small, heavy
     * fog, Family C mood." Derived from lane length, per combat-design.md 1.6.
     */
    public Framing planning() {
        return new Framing((laneLength - 1) / 2.0, laneLength - 1 + 2 * EDGE_MARGIN + TILE_WIDTH);
    }

    /**
     * STYLE.md 9's execution framing, for one beat: "figures large, blades crossing
     * near frame centre."
     *
     * <p>Width comes from the exchange rather than from the lane, so it is the same
     * intimate shot on every lane <em>unless</em> the beat itself is wider than the
     * intimate shot can hold -- a Runner collapsing twelve tiles, which
     * combat-design.md 3d.5 names as the case a single tile could not drive. The
     * centre is clamped so the frame never leaves the paper, which is what stops a
     * beat at the lane edge from framing half a sheet of nothing.
     */
    public Framing execution(Focus focus) {
        double width = Math.max(INTIMACY_TILES, focus.span() + 2 * EXEC_MARGIN);
        return clamp(new Framing((focus.fromTile() + focus.toTile()) / 2.0, width));
    }

    /** Keeps a framing inside the paper the planning shot covers. */
    public Framing clamp(Framing f) {
        Framing plan = planning();
        double half = f.widthTiles() / 2.0;
        double lo = plan.left() + half;
        double hi = plan.right() - half;
        double centre = lo > hi ? plan.centreTile() : Math.max(lo, Math.min(hi, f.centreTile()));
        return new Framing(centre, f.widthTiles());
    }

    /**
     * How large the push-in is on this lane: the ratio between the widest shot and
     * the most intimate one. 2.0 at five tiles, 5.2 at fifteen.
     */
    public double pushIn() {
        return planning().widthTiles() / INTIMACY_TILES;
    }

    /** The push-in on the shortest legal lane, which is the reference STYLE.md 9 describes. */
    public static final double SHORTEST_PUSH_IN =
            (Lane.MIN_LENGTH - 1 + 2 * EDGE_MARGIN + TILE_WIDTH) / INTIMACY_TILES;

    /**
     * How long the push-in takes on this lane.
     *
     * <p><b>An ambiguity in STYLE.md 9, resolved here and worth recording.</b> 9
     * gives the push-in a fixed duration -- "glides toward the exchange over ~0.5 s"
     * -- while combat-design.md 1.6 requires the push-in to be a <em>small</em> move
     * on a short lane and a <em>large</em> one on a long one. Held together
     * literally, the same half second covers a 2.0x zoom on a five-tile lane and a
     * 5.2x zoom on a fifteen-tile one, and the second is fast enough to read as the
     * snap 9's own "never cut" forbids: it crosses more than thirteen tiles of
     * framing in half a second.
     *
     * <p>The reading taken is that 9's number describes the shot it was written
     * about -- the intimate exchange at the short end -- and the duration scales
     * with the square root of how much further the camera has to come. A five-tile
     * lane keeps 0.50 s exactly; a fifteen-tile lane takes 0.80 s, which is still a
     * glide and still shorter than its own return. combat-design.md 1.6 is the
     * tie-breaker: "a long lane has nothing but the approach", so the approach is
     * allowed to take its time.
     */
    public double pushInSeconds() {
        return Timing.PUSH_IN * glideScale();
    }

    /** And the return, which STYLE.md 9 requires to stay slower than the push-in. */
    public double returnSeconds() {
        return Timing.RETURN * glideScale();
    }

    private double glideScale() {
        return Math.max(1.0, Math.sqrt(pushIn() / SHORTEST_PUSH_IN));
    }

    // Lane length is the whole of a stage's identity, so two stages cut for the same
    // lane are the same stage -- which is what lets a Schedule be compared with
    // equals rather than only through its fingerprint.

    @Override
    public boolean equals(Object o) {
        return o instanceof Stage s && s.laneLength == laneLength;
    }

    @Override
    public int hashCode() {
        return laneLength;
    }

    @Override
    public String toString() {
        return "stage[lane " + laneLength + "]";
    }
}
