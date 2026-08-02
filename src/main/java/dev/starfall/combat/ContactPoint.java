package dev.starfall.combat;

/**
 * Where one body is touched, in the engine's own vocabulary.
 *
 * <p><b>The problem this solves.</b> {@code Shoved} said two bodies met and not
 * whether at the shoulder or at the hilt; {@code BladeMet} named a deflection
 * with no crossing point. An IK target needs both, and the animation layer
 * cannot recover either from tile indices -- a shove into a body that has its
 * back turned and a shove into one squared up at you are the same two tiles and
 * completely different drawings.
 *
 * <p><b>Why it is not a coordinate.</b> Nothing here is in world units, because
 * the engine has none and acquiring them would make the whole event stream a
 * rendering artefact. A contact is named the way a fight is named: which body,
 * what part of it, which side of that body relative to <em>its own</em> facing,
 * and how high. The renderer owns the mapping from that to a point in space, and
 * two renderers at different figure scales can both honour it.
 *
 * <p><b>Why the side is relative and not left/right.</b> Two bodies squared up
 * on a lane face opposite ways, so a single world-space side would be the
 * leading side of one and the trailing side of the other. Naming it relative to
 * each body's own facing is what lets both skeletons read the same
 * {@link Meeting} and each aim its own chain correctly.
 *
 * @param body   the entity touched, so a point stands alone as an IK target spec
 * @param part   what of it was touched
 * @param side   which side of that body, relative to the way it is facing
 * @param height how high up the figure
 */
public record ContactPoint(int body, ContactPoint.Part part, ContactPoint.Side side, ContactPoint.Height height) {

    /** What was touched. Only what the rules actually produce -- nothing speculative. */
    public enum Part {
        /**
         * The star-blade, edge or flat -- including a blade held across as a guard,
         * which is physically the same contact whether or not a counter was armed.
         */
        BLADE,
        /** The grip end, driven into a body. The hilt half of the Warden's check. */
        HILT,
        /** The shoulder half of the Warden's check. */
        SHOULDER,
        /** A sleeve, crossing another as two figures pass through each other. */
        ARM,
        /** Chest or flank. Where a blade lands on a body squared up to it. */
        TORSO,
        /** The same, from behind. */
        BACK
    }

    /** Which side of the touched body, relative to the way that body is facing. */
    public enum Side {
        /** The side it is turned toward. It saw this coming. */
        LEADING,
        /** The side behind it. It did not. */
        TRAILING
    }

    /**
     * How high. Three bands, because that is as much as a 20-30 pixel profile
     * figure (STYLE.md 4b.0) can express and more would be inventing precision the
     * rules do not have.
     */
    public enum Height {
        /** Head and shoulder line: a cut coming down, a parry taken high. */
        HIGH,
        /** Chest to hip: a thrust, a shoulder-check, a body passing another. */
        MIDDLE,
        /** Hem and ground: ink thrown across a tile by a bloom. */
        LOW
    }

    /**
     * A point on {@code body}, with the side worked out from where {@code from}
     * is standing. Bodies on the same tile -- which the board forbids, but
     * defensive code is cheaper than a lost afternoon -- read as leading.
     */
    static ContactPoint on(Combatant body, Combatant from, Part part, Height height) {
        Side side = Side.LEADING;
        if (from != null && from.tile() != body.tile()) {
            side = Facing.toward(body.tile(), from.tile()) == body.facing() ? Side.LEADING : Side.TRAILING;
        }
        return new ContactPoint(body.id(), part, side, height);
    }

    /** A point on the leading side of {@code body}: what an actor reaches out with. */
    static ContactPoint leading(Combatant body, Part part, Height height) {
        return new ContactPoint(body.id(), part, Side.LEADING, height);
    }

    /**
     * How high a blade arrives from {@code distance} tiles away. Adjacent, it
     * comes down; from reach two it comes in flat. This is the one thing on the
     * lane that genuinely varies a contact's height, and a Reacher's stroke
     * reading like a Wisp's would waste the only distance the design gives.
     */
    static Height heightForReach(int distance) {
        return distance <= 1 ? Height.HIGH : Height.MIDDLE;
    }

    @Override
    public String toString() {
        return "#" + body + " " + side + " " + part + " " + height;
    }
}
