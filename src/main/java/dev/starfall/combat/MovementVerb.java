package dev.starfall.combat;

/**
 * A hero's signature: what happens when they advance into an occupied tile.
 *
 * <p><b>Why this is an interface and not two branches in the Step tile.</b>
 * combat-design.md 2.1 says the two verbs are "the point of the design", chosen
 * so the interaction layer is proved generic rather than fitted to one case. A
 * verb implemented as {@code if (hero == WARDEN) ... else ...} is fitted to two
 * cases by construction. So the verb is a first-class object with one entry
 * point, the engine knows nothing about push or swap specifically, and the
 * third verb -- pass-through, throw-backward, rotate-front-and-back -- is a new
 * implementation of this interface and no engine change at all.
 *
 * <p>A verb is asked to resolve <em>the contact</em>, not the move. The caller
 * has already established that the mover wants the occupied tile; the verb
 * decides what two bodies meeting there does, and reports whether the mover ends
 * up standing in it.
 *
 * <p>Verbs are stateless singletons and must stay that way: {@link CombatEngine}
 * previews by cloning state and replaying, and a verb holding state would not be
 * cloned.
 */
public sealed interface MovementVerb {

    /** Collision damage dealt to each of two braced bodies. */
    int COLLISION_DAMAGE = 1;

    /** Name used in {@link CombatEvent.VerbRefused}, so the stream is readable. */
    String verbName();

    /**
     * Resolve two bodies meeting on one tile.
     *
     * @param direction the direction the mover is travelling, i.e. its facing
     * @return true if {@code mover} now stands on the tile {@code occupant} held
     */
    boolean resolveContact(Context ctx, Combatant mover, Combatant occupant, Facing direction);

    /**
     * The slice of the board a verb is allowed to touch. Kept this narrow on
     * purpose: a verb may look at the lane, move bodies, deal damage and speak,
     * and nothing else. It cannot end turns, spend tiles or declare intents.
     */
    interface Context {
        boolean inLane(int tile);

        /** The body standing on {@code tile}, or {@code null}. */
        Combatant at(int tile);

        /**
         * Set a body down on a tile and say so. Deliberately does <em>not</em>
         * check occupancy: a swap needs two bodies to cross, which is momentarily
         * illegal and is the verb's business to get right.
         */
        void move(Combatant who, int toTile, CombatEvent.MoveReason reason);

        void damage(Combatant target, int amount, CombatEvent.HitSource source, Combatant from);

        void emit(CombatEvent event);

        /**
         * The instant, in {@link Phases#WHOLE} parts of the beat now resolving, at
         * which this contact lands.
         *
         * <p>A verb is asked to resolve the contact, so it is the verb that names
         * where on the two bodies it happens -- and the <em>when</em> has to come
         * from the beat around it, because a verb fires inside a Step beat whose
         * shape it does not choose.
         */
        int contactAt();
    }

    /** The Warden. */
    MovementVerb PUSH = new Push();

    /** The Pilgrim. */
    MovementVerb SWAP = new Swap();

    /**
     * The Warden's verb: a shoulder-and-hilt check. The occupant gives ground one
     * tile; if it has nowhere to give, both bodies take the impact.
     *
     * <p>Two rules combat-design.md leaves open, resolved here:
     * <ul>
     *   <li><b>"No room behind" covers both a lane edge and another body.</b> The
     *       design only names the edge case, but a body braced against its own
     *       ally is the same brace and should read the same. It does not chain --
     *       one shove moves one body, so a queue of enemies is a wall.</li>
     *   <li><b>A collision that kills the occupant still does not let the pusher
     *       through.</b> The beat is the brace, not the follow-through; the Warden
     *       is stopped at the moment of impact and the tile clears behind the
     *       event, not during it.</li>
     * </ul>
     */
    record Push() implements MovementVerb {

        @Override
        public String verbName() {
            return "push";
        }

        @Override
        public boolean resolveContact(Context ctx, Combatant mover, Combatant occupant, Facing direction) {
            int held = occupant.tile();
            if (occupant.hasTrait(Trait.UNYIELDING)) {
                ctx.emit(new CombatEvent.VerbRefused(mover.id(), occupant.id(), verbName(),
                        CombatEvent.RefusalReason.UNYIELDING));
                return false;
            }
            Meeting meeting = check(ctx, mover, occupant, direction);
            int behind = held + direction.step();
            boolean offLane = !ctx.inLane(behind);
            boolean blocked = !offLane && ctx.at(behind) != null;
            if (offLane || blocked) {
                ctx.emit(new CombatEvent.Shoved(mover.id(), occupant.id(), held, held, false, meeting));
                ctx.emit(new CombatEvent.Collided(mover.id(), occupant.id(), held, COLLISION_DAMAGE,
                        offLane ? CombatEvent.CollisionCause.LANE_EDGE : CombatEvent.CollisionCause.BODY_BEHIND));
                ctx.damage(occupant, COLLISION_DAMAGE, CombatEvent.HitSource.COLLISION, mover);
                ctx.damage(mover, COLLISION_DAMAGE, CombatEvent.HitSource.COLLISION, occupant);
                return false;
            }
            ctx.emit(new CombatEvent.Shoved(mover.id(), occupant.id(), held, behind, true, meeting));
            ctx.move(occupant, behind, CombatEvent.MoveReason.SHOVED);
            ctx.move(mover, held, CombatEvent.MoveReason.STEP);
            return true;
        }

        /**
         * Shoulder or hilt, decided by how the occupant is turned.
         *
         * <p>combat-design.md 2.1 names the beat "a shoulder-and-hilt check" and
         * leaves which of the two open. The board answers it: a body squared up at
         * you is met chest to chest and high, and a body with its back turned is
         * driven forward with the grip end, lower and heavier. Those are two
         * different drawings and the engine already knows which one it is, so it
         * says so rather than letting the animation layer pick one and use it for
         * both. Contact is <em>sustained</em> either way, per 2.1 -- which is
         * carried by the Step beat's wide contact span, not by this point.
         */
        private static Meeting check(Context ctx, Combatant mover, Combatant occupant, Facing direction) {
            boolean backTurned = occupant.facing() == direction;
            ContactPoint onMover = ContactPoint.leading(mover,
                    backTurned ? ContactPoint.Part.HILT : ContactPoint.Part.SHOULDER,
                    backTurned ? ContactPoint.Height.MIDDLE : ContactPoint.Height.HIGH);
            ContactPoint onOccupant = ContactPoint.on(occupant, mover,
                    backTurned ? ContactPoint.Part.BACK : ContactPoint.Part.TORSO,
                    backTurned ? ContactPoint.Height.MIDDLE : ContactPoint.Height.HIGH);
            return new Meeting(onMover, onOccupant, ctx.contactAt());
        }
    }

    /**
     * The Pilgrim's verb: two figures exchanging places within one beat, sleeves
     * and hair crossing, each trailing into the space the other left.
     *
     * <p>Swap never runs out of room -- the two bodies trade, so the destination
     * is vacated by definition -- which makes {@link Trait#UNYIELDING} its
     * <em>only</em> refusal, and therefore the Bulwark's only answer to the
     * Pilgrim. That asymmetry is intended: push has two failure modes and swap has
     * one, so the two heroes meet the same wall in different shapes.
     */
    record Swap() implements MovementVerb {

        @Override
        public String verbName() {
            return "swap";
        }

        @Override
        public boolean resolveContact(Context ctx, Combatant mover, Combatant occupant, Facing direction) {
            if (occupant.hasTrait(Trait.UNYIELDING)) {
                ctx.emit(new CombatEvent.VerbRefused(mover.id(), occupant.id(), verbName(),
                        CombatEvent.RefusalReason.UNYIELDING));
                return false;
            }
            int here = mover.tile();
            int there = occupant.tile();
            // Sleeve on sleeve, mid-height, as the two figures cross. The only
            // contact in the game the animation layer must draw with no impact at
            // all -- combat-design.md 2.1 calls it an interpenetration -- so it is
            // named as a meeting like any other and carries Force.NONE on the Moveds
            // that follow.
            Meeting meeting = new Meeting(
                    ContactPoint.leading(mover, ContactPoint.Part.ARM, ContactPoint.Height.MIDDLE),
                    ContactPoint.on(occupant, mover, ContactPoint.Part.ARM, ContactPoint.Height.MIDDLE),
                    ctx.contactAt());
            ctx.emit(new CombatEvent.Swapped(mover.id(), occupant.id(), here, there, meeting));
            ctx.move(occupant, here, CombatEvent.MoveReason.SWAPPED);
            ctx.move(mover, there, CombatEvent.MoveReason.SWAPPED);
            return true;
        }
    }
}
