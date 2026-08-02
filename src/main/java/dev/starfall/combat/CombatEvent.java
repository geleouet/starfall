package dev.starfall.combat;

import java.util.List;

/**
 * The beat vocabulary. Everything the rules engine does, it says.
 *
 * <p><b>Why an event stream at all.</b> combat-design.md 0 filters mechanics by
 * whether they produce a beat of choreography, and System 4 is the reason this
 * project exists. A rules engine that only mutated state would leave the
 * animation layer to <em>infer</em> what happened by diffing boards, and the
 * interesting cases are exactly the ones a diff cannot see: a shove that moved
 * nobody, a blade that met another blade, a stroke that went through empty air.
 * Those are non-events in the state and the best beats in the game.
 *
 * <p><b>The layering rule.</b> {@link Moved} is the atomic fact -- a body left
 * one tile and arrived at another, and why. The <em>relational</em> events
 * ({@link Shoved}, {@link Swapped}, {@link Drawn}, {@link Collided},
 * {@link BladeMet}, {@link VerbRefused}) name the two-body relationship and are
 * emitted <b>before</b> the {@link Moved} events they cause. So the animation
 * layer reads the relational event to choose the choreography -- shoulder-check,
 * interpenetration, line of force -- and reads the {@link Moved} events that
 * follow for the trajectories. Neither is redundant: a swap is two Moveds that
 * must be animated as one interlocked gesture, and a refused push is a relational
 * event with no Moved at all.
 *
 * <p><b>The staging layer.</b> Sitting across all three of the above, and not
 * beside them, is what System 4 needs and could not derive: {@link Phases} on
 * every beat (the wind-up / contact / recovery split STYLE.md 7.1 specifies),
 * {@link Overlap} on the seam between beats, {@link Focus} naming the subject,
 * {@link Meeting} and {@link ContactPoint} naming <em>where</em> two bodies
 * touch, {@link Force} saying how hard a body is travelling, and
 * {@link Dissolve} giving a death a duration and a direction. Every one of those
 * rides on an event that already existed, because the alternative -- a parallel
 * stream of hints -- would have to be re-synchronised with the beats it
 * describes, and a synchronisation that can drift will.
 *
 * <p>All of it stays <b>ordinal and unit-free</b>: proportions of a beat whose
 * length the engine never learns, tiles, sides relative to a body's own facing,
 * and a four-step force scale. The renderer owns every mapping to space and
 * time, which is what keeps two runs of one seed producing {@code equals} event
 * lists.
 *
 * <p><b>What is deliberately not here.</b> Cooldown recovery emits nothing.
 * combat-design.md 0 lists "pure cooldown arithmetic" under mechanics that do not
 * earn their place, and a charge ticking up is a mark on the sheet, not a body
 * doing something. {@link TileSpent} and {@link TileRefunded} do exist, because
 * those are the moments a stroke dries out or refills and the UI needs the
 * instant.
 *
 * <p>Entities are referred to by integer id throughout rather than by reference,
 * so a stream stays meaningful after the state it describes has moved on -- which
 * is precisely the situation the animation layer is in, several frames behind.
 *
 * <p>Every event is a record, so two runs of the same seeded encounter produce
 * event lists that are {@code equals}. That is the determinism contract, and it
 * is a far stronger check than comparing final board states.
 */
public sealed interface CombatEvent {

    // -- shared vocabulary ----------------------------------------------------

    /** Why a body moved. The animation layer picks a gait from this. */
    enum MoveReason {
        /** The hero's Step tile, or a plain advance into empty space. */
        STEP,
        /** The hero's Back-step. Weight going the wrong way. */
        BACK_STEP,
        /** The Feint. Motion with no contact -- the negative space. */
        FEINT,
        /** Carried backward by the Warden's shoulder-and-hilt check. */
        SHOVED,
        /** Half of a Pilgrim swap: two figures passing through each other. */
        SWAPPED,
        /** Hauled in by the Draw. A line of force between two figures. */
        DRAWN,
        /** An enemy closing one tile. */
        ADVANCE,
        /** A Charger collapsing the whole distance at once. */
        CHARGE,
        /** A non-Aggressive body giving ground, on the turn after it struck. */
        GAVE_GROUND,
        /** An Aggressive body walking in, on the turn after it struck. */
        CLOSED_IN
    }

    /** Why a move did not happen. */
    enum BlockReason {
        /** The Fold of the World ends there. */
        LANE_EDGE,
        /** Another body is standing in it. */
        OCCUPIED,
        /** The mover's pigment has dried. */
        IMMOBILISED,
        /** There was nothing in range to act on. */
        NO_TARGET
    }

    /** Why a body turned. */
    enum TurnReason {
        /** The player spent a whole turn on it. Facing is a resource. */
        COMMAND,
        /** A Turn tile inside a phrase. */
        TILE,
        /** Spun by a Draw. */
        HAULED,
        /** An enemy squaring up to the hero of its own accord. */
        OWN_CHOICE
    }

    /** Why a verb was refused. Currently one answer, and that is the point of the Bulwark. */
    enum RefusalReason {
        UNYIELDING
    }

    /** What the shoved body was braced against. */
    enum CollisionCause {
        /** The lane ran out behind them. */
        LANE_EDGE,
        /** Another body was standing behind them. */
        BODY_BEHIND
    }

    /** Where damage came from. Guard negates attacks; it does not negate a wound already bleeding. */
    enum HitSource {
        /** A blade, from either side. */
        BLADE,
        /** A parry's answer. */
        COUNTER,
        /** Two braced bodies. */
        COLLISION,
        /** An Explosive death thrown across the neighbours. */
        BLOOM,
        /** The Shockwave enchantment catching the tiles either side. */
        SHOCKWAVE,
        /** A Seeping tick. The only source {@link Status#GUARD} and {@link Status#MARKED} ignore. */
        SEEPING;

        /** True for everything a raised guard can stand in front of. */
        public boolean isAttack() {
            return this != SEEPING;
        }
    }

    // -- structure: the shape of a turn and of a phrase ------------------------

    /** @param enemyIds ids of the Charted Shadows on the board at the opening, in board order */
    record EncounterBegan(int laneLength, Hero hero, int heroId, List<Integer> enemyIds) implements CombatEvent {
    }

    record TurnBegan(int turn) implements CombatEvent {
    }

    record TurnEnded(int turn) implements CombatEvent {
    }

    /**
     * An execution starts. Everything between this and {@link PhraseEnded} is one
     * uninterrupted sentence of movement -- the multi-beat material of STYLE.md 7.
     */
    record PhraseBegan(int turn, int tiles) implements CombatEvent {
    }

    record PhraseEnded(int turn, int kills) implements CombatEvent {
    }

    /**
     * One clause of the phrase, with everything the animation layer needs to stage
     * it before a single atomic event has been emitted.
     *
     * <p>The staging is declared <em>up front</em> on purpose. A camera that
     * learns its subject from the {@code Hit} has already failed to glide
     * (STYLE.md 9), and an overlap hint that arrives after the beat has played is
     * not a hint. So the shape, the seam and the subject are all known at the top
     * of the beat, and only the outcome arrives during it.
     *
     * @param index   counted from the top of the stanza, so 0 resolves first
     * @param phases  the wind-up / contact / recovery split of STYLE.md 7.1
     * @param overlap how far into the previous beat this one may begin
     * @param focus   whose beat it is, and the run of tiles it happens over
     */
    record BeatBegan(int index, Tile tile, int actor, Phases phases, Overlap overlap, Focus focus)
            implements CombatEvent {
    }

    record EnemyPhaseBegan(int turn) implements CombatEvent {
    }

    /**
     * One clause of the enemy phase: a Charted Shadow resolving its Strikethrough.
     *
     * <p>Symmetric with {@link BeatBegan}, and it exists for the same reason.
     * STYLE.md 7 does not grade the hero's motion and the enemy's by different
     * standards, and a lane with three bodies acting in board order is as much a
     * phrase as a five-tile stanza is. Without this the enemy phase was a run of
     * atomic events with no shape, no seams and no subject, which would have made
     * exactly half the fight unstageable.
     *
     * <p>Separate from {@link BeatBegan} rather than sharing it, because a beat
     * spent on a {@link Tile} and a beat spent on an {@link Intent} are different
     * facts and folding them together would mean a nullable tile on every beat in
     * the game.
     *
     * @param index counted within this enemy phase, in board order
     * @param kind  what the body committed to, one turn ago or -- if it is Quick --
     *              in this instant
     */
    record EnemyBeatBegan(int index, Intent.Kind kind, int actor, Phases phases, Overlap overlap, Focus focus)
            implements CombatEvent {
    }

    record EnemyPhaseEnded(int turn) implements CombatEvent {
    }

    record EncounterEnded(EncounterOutcome outcome, int turn) implements CombatEvent {
    }

    // -- the stanza ------------------------------------------------------------

    /** @param costTurn false only for a free-play tile, which is the whole value of the enchantment */
    record TileQueued(int loadoutIndex, Tile tile, int slot, boolean costTurn) implements CombatEvent {
    }

    record TileUnqueued(int loadoutIndex, Tile tile, int slot) implements CombatEvent {
    }

    record TileReordered(int fromSlot, int toSlot) implements CombatEvent {
    }

    /** The stroke dries: charges to zero, whether the tile hit or not. */
    record TileSpent(int loadoutIndex, Tile tile) implements CombatEvent {
    }

    /** Perfect Strike: the stroke refills instead. */
    record TileRefunded(int loadoutIndex, Tile tile) implements CombatEvent {
    }

    // -- bodies ----------------------------------------------------------------

    /**
     * @param force how hard the body is travelling, which is not the same question
     *              as how far. See {@link Force}.
     */
    record Moved(int entity, int fromTile, int toTile, MoveReason reason, Force force) implements CombatEvent {

        /** Signed tile delta: negative toward the near end of the Fold. */
        public int delta() {
            return toTile - fromTile;
        }
    }

    record MoveBlocked(int entity, int fromTile, int intoTile, BlockReason reason) implements CombatEvent {
    }

    record Turned(int entity, Facing from, Facing to, TurnReason reason) implements CombatEvent {
    }

    /**
     * The Warden's verb. Emitted whether or not the body actually gave ground, so
     * the animation layer always gets the contact even when the state does not move.
     *
     * <p>combat-design.md 2.1 calls it "a shoulder-and-hilt check", and which of
     * the two it is depends on how the occupant is turned: a body squared up takes
     * a shoulder in the chest, a body with its back turned takes the hilt. The
     * {@link Meeting} says which, so the IK targets differ instead of the animation
     * layer picking one and using it for both.
     *
     * @param gaveGround false when the shove was braced; a {@link Collided} follows
     */
    record Shoved(int pusher, int shoved, int fromTile, int toTile, boolean gaveGround, Meeting meeting)
            implements CombatEvent {
    }

    /**
     * The Pilgrim's verb. Two figures passing through each other within one beat.
     *
     * <p>The {@link Meeting} is sleeve on sleeve -- combat-design.md 2.1's
     * "sleeves and hair crossing" -- and it is the one contact in the game with no
     * force behind it at all.
     */
    record Swapped(int a, int b, int aFrom, int bFrom, Meeting meeting) implements CombatEvent {
    }

    /**
     * The Draw's line of force.
     *
     * @param toTile equals fromTile when the pull had nowhere to go
     */
    record Drawn(int puller, int pulled, int fromTile, int toTile, Meeting meeting) implements CombatEvent {
    }

    /**
     * The mechanic refusing to happen. The Bulwark exists to produce this event,
     * and it is a beat in its own right: a body thrown against something rooted.
     */
    record VerbRefused(int actor, int target, String verb, RefusalReason reason) implements CombatEvent {
    }

    /** Two braced bodies. Both take {@code damage}; the {@link Hit}s follow. */
    record Collided(int a, int b, int atTile, int damage, CollisionCause cause) implements CombatEvent {
    }

    // -- blades ----------------------------------------------------------------

    /**
     * A Strikethrough. What this Charted Shadow will do next turn, published a
     * turn early so the UI can wash the threatened tiles in vermillion.
     */
    record IntentDeclared(int entity, Intent intent) implements CombatEvent {
    }

    /** The board moved under a declared intent, so it resolves somewhere else than it was drawn. */
    record IntentRetargeted(int entity, List<Integer> declared, List<Integer> actual) implements CombatEvent {
    }

    /**
     * The stroke leaves the body, over these tiles, whether or not it finds anything.
     *
     * @param at the instant within the enclosing beat at which this stroke arrives,
     *           in {@link Phases#WHOLE} parts. Normally the beat's own
     *           {@link Phases#contactStart()}; a Double Strike puts two strokes in
     *           one beat and they land at different instants, because STYLE.md 10
     *           fails a pass on sight of two things peaking together.
     */
    record Swung(int attacker, List<Integer> tiles, int at) implements CombatEvent {
    }

    /** The stroke found nothing. Still a beat, and still spends the tile. */
    record Whiffed(int attacker, List<Integer> tiles) implements CombatEvent {
    }

    /**
     * @param doubled true when {@link Status#MARKED} was consumed to double it
     * @param landed  where on the struck body it landed, which is where STYLE.md
     *                7.3's bloom of ink spreads from. Null for
     *                {@link HitSource#SEEPING} only: nothing touched the body, the
     *                wound was already there.
     */
    record Hit(int attacker, int target, int amount, int hpAfter, HitSource source, boolean doubled,
               ContactPoint landed) implements CombatEvent {
    }

    /**
     * Blade on blade. A deflection curve, not a collision -- STYLE.md 7.2's
     * signature beat, and the reason {@link Meeting} exists.
     *
     * <p>The meeting names the crossing twice, once in each body's own frame, and
     * gives both the same instant. That single shared number is what two skeletons
     * synchronise on: without it each would solve its own arc and the blades would
     * cross on different frames, which is the failure STYLE.md 7.2 describes as a
     * collision rather than a deflection.
     */
    record BladeMet(int defender, int attacker, Meeting meeting) implements CombatEvent {
    }

    /** A plain guard absorbing a blow, with no answer to it. */
    record GuardHeld(int defender, int attacker, Meeting meeting) implements CombatEvent {
    }

    /** The parry's answer, following a {@link BladeMet}. */
    record Countered(int defender, int attacker, int damage) implements CombatEvent {
    }

    /** @param dissolve how the ink goes: a direction, a force, and a length in beats */
    record Died(int entity, int atTile, int killer, Dissolve dissolve) implements CombatEvent {
    }

    /** An Explosive death: a bloom of ink thrown across the neighbouring tiles. */
    record Bloomed(int entity, int atTile, int damage, List<Integer> tiles) implements CombatEvent {
    }

    /** More than one kill in a single execution. The economy's engine, and the phrase's cadence. */
    record ComboLanded(int count, List<Integer> victims) implements CombatEvent {
    }

    // -- statuses --------------------------------------------------------------

    record StatusApplied(int entity, Status status, int turns) implements CombatEvent {
    }

    record StatusTicked(int entity, Status status, int damage, int remaining) implements CombatEvent {
    }

    record StatusExpired(int entity, Status status) implements CombatEvent {
    }

    /** A flag status being cashed in: Guard standing in front of a blow, Marked doubling one. */
    record StatusConsumed(int entity, Status status) implements CombatEvent {
    }

    /** A body whose pigment has dried, missing its step. */
    record Immobilised(int entity, int remaining) implements CombatEvent {
    }
}
