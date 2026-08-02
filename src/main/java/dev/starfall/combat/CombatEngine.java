package dev.starfall.combat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The rules of one encounter on the Fold of the World. Headless: turns, tiles and
 * bodies, and not one pixel, second or colour.
 *
 * <h2>How it is used</h2>
 * <pre>{@code
 * CombatEngine engine = CombatEngine.create(spec);
 * engine.state().strikethroughs();          // what the UI washes in vermillion
 * Resolution dry = engine.previewExecution(); // what would happen, mutating nothing
 * Resolution real = engine.apply(Command.execute());
 * for (CombatEvent beat : real.events()) { ... }  // hand the phrase to System 4
 * }</pre>
 *
 * <h2>Three properties the rest of the project leans on</h2>
 * <ul>
 *   <li><b>It speaks.</b> Every rule that fires emits a {@link CombatEvent}, in
 *       order, including the rules that fire and change nothing -- a refused
 *       shove, a stroke through empty air. See {@link CombatEvent} for why the
 *       vocabulary is shaped the way it is.</li>
 *   <li><b>It is deterministic.</b> Nothing in resolution rolls a die. Enemies
 *       resolve in board order, which is a total order because one tile holds one
 *       body, so there is never even a tie to break. The same spec replayed with
 *       the same commands emits an {@code equals} event list.</li>
 *   <li><b>It can be asked without being told.</b> {@link #preview} clones the
 *       whole state, replays the command on the clone and hands back both the
 *       beats and the resulting board, leaving the live state untouched down to
 *       the last charge.</li>
 * </ul>
 *
 * <h2>The turn</h2>
 * A turn-costing command runs: {@code TurnBegan} - the player's action - the
 * enemy phase in board order - upkeep (statuses tick, charges recover, the next
 * turn's Strikethroughs are declared) - {@code TurnEnded}. Free commands
 * (correcting the stanza, and banking a free-play tile) sit outside that frame
 * entirely, which is exactly what "free" means here: the board does not move.
 */
public final class CombatEngine {

    /** Damage a Parry answers with when it deflects. */
    public static final int COUNTER_DAMAGE = 1;

    /** Damage the Shockwave enchantment throws onto each tile beside the one struck. */
    public static final int SHOCKWAVE_DAMAGE = 1;

    private final CombatState state;
    private final MovementVerb verb;
    private final MovementVerb.Context verbContext = new VerbContext();
    private final List<CombatEvent> history = new ArrayList<>();

    private List<CombatEvent> current = new ArrayList<>();
    private List<CombatEvent> opening = List.of();

    /** Non-null only while a phrase is resolving; collects the kills that make a combo. */
    private List<Integer> phraseVictims;

    /** Set when a hit within the current beat finished a body exactly. Perfect Strike reads it. */
    private boolean beatFinishedExactly;

    // -- staging: the shape of the beat now resolving --------------------------
    //
    // All four are transient within one command. They are reset at the top of every
    // bracket (a phrase, an enemy phase), never read across one, and never copied by
    // CombatState -- which is what keeps previewExecution() byte-identical to an
    // apply(): a preview builds a fresh engine over a cloned state, so it starts from
    // the same blank staging the real run does.

    /** The shape of the beat now resolving. */
    private Phases beatPhases = Phases.STRIKE;

    /** How many strokes this beat resolves into: two only under Double Strike. */
    private int strokesInBeat = 1;

    /** Which of those strokes is resolving, so two contacts never land together. */
    private int strokeIndex;

    /** What the previous beat in this bracket did, for {@link Overlap}. */
    private BeatMark previousBeat;

    /** Counts beats within the current enemy phase, in board order. */
    private int enemyBeatIndex;

    private CombatEngine(CombatState state, MovementVerb verb) {
        this.state = state;
        this.verb = verb;
    }

    // -- construction ----------------------------------------------------------

    public static CombatEngine create(EncounterSpec spec) {
        Loadout loadout = Loadout.of(spec.loadout());
        CombatState state = new CombatState(spec.lane(), loadout, new InkStanza(), 0, new Rng(spec.seed()));
        state.add(Combatant.hero(0, spec.hero(), spec.heroHp(), spec.heroTile(), spec.heroFacing()));
        int id = 1;
        for (EncounterSpec.Placement p : spec.enemies()) {
            state.add(Combatant.enemy(id++, p.archetype(), p.tile(), p.facing(), p.extraTraits()));
        }
        CombatEngine engine = new CombatEngine(state, spec.hero().verb());
        engine.begin();
        return engine;
    }

    private void begin() {
        current = new ArrayList<>();
        List<Integer> ids = new ArrayList<>();
        for (Combatant e : state.enemies()) {
            ids.add(e.id());
        }
        // state.all() is the hero followed by the Charted Shadows in creation order,
        // which is ascending id -- ids are handed out in exactly that sequence in
        // create() -- so this needs no sort to honour the field's own contract.
        List<CombatEvent.EncounterBegan.Position> board = new ArrayList<>();
        for (Combatant c : state.all()) {
            board.add(new CombatEvent.EncounterBegan.Position(c.id(), c.tile(), c.facing()));
        }
        emit(new CombatEvent.EncounterBegan(state.lane().length(), state.hero().hero(), state.hero().id(), ids,
                board));
        declareIntents();
        checkOutcome();
        history.addAll(current);
        opening = List.copyOf(current);
        current = new ArrayList<>();
    }

    // -- reading ---------------------------------------------------------------

    public CombatState state() {
        return state;
    }

    /** The hero's movement verb: push for the Warden, swap for the Pilgrim. */
    public MovementVerb verb() {
        return verb;
    }

    /** Every beat since the encounter opened, in order. */
    public List<CombatEvent> history() {
        return Collections.unmodifiableList(history);
    }

    /** The beats emitted while setting the board up, including the first Strikethroughs. */
    public List<CombatEvent> opening() {
        return opening;
    }

    // -- validation ------------------------------------------------------------

    /**
     * Why {@code cmd} cannot be issued, or {@code null} if it can. Returning the
     * reason rather than a boolean is deliberate: the UI has to be able to say
     * <em>why</em> a cartouche is refusing to be written, and re-deriving that
     * from a false is how two implementations of the same rule appear.
     */
    public String reject(Command cmd) {
        if (state.outcome().over()) {
            return "the encounter is over";
        }
        Loadout hand = state.loadout();
        InkStanza stanza = state.stanza();
        return switch (cmd) {
            case Command.Add a -> {
                int i = a.loadoutIndex();
                if (i < 0 || i >= hand.size()) {
                    yield "no such tile in hand: " + i;
                }
                if (hand.banked(i)) {
                    yield hand.tile(i) + " is already in the Ink Stanza";
                }
                if (stanza.isFull()) {
                    yield "the Ink Stanza already holds " + InkStanza.CAPACITY + " tiles";
                }
                if (!hand.ready(i)) {
                    yield hand.tile(i) + " is still drying: " + hand.charges(i) + "/" + hand.tile(i).cooldown();
                }
                yield null;
            }
            case Command.Remove r -> outOfStanza(r.slot(), stanza);
            case Command.Reorder r -> {
                String bad = outOfStanza(r.fromSlot(), stanza);
                yield bad != null ? bad : outOfStanza(r.toSlot(), stanza);
            }
            case Command.Execute ignored -> stanza.isEmpty() ? "the Ink Stanza is empty" : null;
            case Command.TurnAround ignored -> null;
            case Command.Hold ignored -> null;
        };
    }

    private static String outOfStanza(int slot, InkStanza stanza) {
        return slot < 0 || slot >= stanza.size() ? "no such stanza slot: " + slot : null;
    }

    public boolean can(Command cmd) {
        return reject(cmd) == null;
    }

    /** True when {@code cmd} would let the board move, i.e. when it spends a turn. */
    public boolean costsTurn(Command cmd) {
        return switch (cmd) {
            case Command.Add a -> !state.loadout().tile(a.loadoutIndex()).freePlay();
            case Command.Remove ignored -> false;
            case Command.Reorder ignored -> false;
            default -> true;
        };
    }

    // -- issuing ---------------------------------------------------------------

    /**
     * Issue a command. Throws {@link IllegalStateException} with the text of
     * {@link #reject} if it is not legal -- an illegal command is a bug in the
     * caller, not a game state, and swallowing it would let a UI quietly desync
     * from the rules.
     */
    public Resolution apply(Command cmd) {
        String bad = reject(cmd);
        if (bad != null) {
            throw new IllegalStateException(bad);
        }
        int turn = state.turn();
        current = new ArrayList<>();
        resetStaging();
        switch (cmd) {
            case Command.Remove r -> {
                InkStanza.Slot s = state.stanza().remove(r.slot());
                state.loadout().unbank(s.loadoutIndex());
                emit(new CombatEvent.TileUnqueued(s.loadoutIndex(), s.tile(), r.slot()));
            }
            case Command.Reorder r -> {
                state.stanza().reorder(r.fromSlot(), r.toSlot());
                emit(new CombatEvent.TileReordered(r.fromSlot(), r.toSlot()));
            }
            case Command.Add a -> {
                boolean free = state.loadout().tile(a.loadoutIndex()).freePlay();
                if (free) {
                    bank(a.loadoutIndex(), false);
                } else {
                    runTurn(() -> bank(a.loadoutIndex(), true));
                }
            }
            case Command.Execute ignored -> runTurn(this::resolvePhrase);
            case Command.TurnAround ignored -> runTurn(() -> {
                Combatant h = state.hero();
                turnBody(h, h.facing().opposite(), CombatEvent.TurnReason.COMMAND);
            });
            case Command.Hold ignored -> runTurn(() -> {
            });
        }
        List<CombatEvent> produced = current;
        current = new ArrayList<>();
        history.addAll(produced);
        return new Resolution(turn, produced, state);
    }

    /**
     * What {@code cmd} would do, without doing it. Runs on a full deep copy, so
     * the returned {@link Resolution#state()} is a detached board the caller may
     * inspect freely and the live state is byte-for-byte untouched.
     */
    public Resolution preview(Command cmd) {
        CombatEngine ghost = new CombatEngine(state.copy(), verb);
        Resolution r = ghost.apply(cmd);
        return new Resolution(r.turn(), r.events(), ghost.state);
    }

    /** "What happens if I execute this stanza right now." */
    public Resolution previewExecution() {
        return preview(Command.execute());
    }

    // -- the turn --------------------------------------------------------------

    private void runTurn(Runnable playerAction) {
        emit(new CombatEvent.TurnBegan(state.turn()));
        playerAction.run();
        checkOutcome();
        if (!state.outcome().over()) {
            enemyPhase();
        }
        if (!state.outcome().over()) {
            upkeep();
        }
        emit(new CombatEvent.TurnEnded(state.turn()));
        state.advanceTurn();
    }

    private void bank(int loadoutIndex, boolean costTurn) {
        Tile tile = state.loadout().tile(loadoutIndex);
        int slot = state.stanza().push(loadoutIndex, tile, state.turn());
        state.loadout().bank(loadoutIndex);
        emit(new CombatEvent.TileQueued(loadoutIndex, tile, slot, costTurn));
    }

    // -- the phrase ------------------------------------------------------------

    /**
     * All-or-nothing, top slot first.
     *
     * <p><b>Two readings of "all-or-nothing" that combat-design.md leaves open,
     * settled here.</b> First, a phrase does not stop when it runs out of targets:
     * if beat two clears the board, beats three to five still swing, still whiff
     * and still spend their charges. STORY.md 1.2.3 is explicit -- "si la
     * trajectoire manque sa cible, le Pèlerin poursuit son tracé jusqu'au bout de
     * son encre" -- and it is also the better rule, because it makes an
     * over-committed stanza cost something. Second, every banked tile is spent
     * even if the hero dies partway through the phrase: the cost is all-or-nothing
     * too, not just the effect.
     */
    private void resolvePhrase() {
        List<InkStanza.Slot> slots = List.copyOf(state.stanza().slots());
        emit(new CombatEvent.PhraseBegan(state.turn(), slots.size()));
        phraseVictims = new ArrayList<>();
        previousBeat = null;
        int beat = 0;
        for (InkStanza.Slot slot : slots) {
            Combatant hero = state.hero();
            BeatMark mark = BeatMark.of(hero, slot.tile().type(), state.lane());
            Phases shape = Phases.of(slot.tile().type());
            emit(new CombatEvent.BeatBegan(beat++, slot.tile(), hero.id(), shape,
                    overlapAfter(mark), mark.focus()));
            previousBeat = mark;
            beatPhases = shape;
            beatFinishedExactly = false;
            if (hero.alive()) {
                resolveBeat(hero, slot.tile());
            }
            state.loadout().spend(slot.loadoutIndex());
            emit(new CombatEvent.TileSpent(slot.loadoutIndex(), slot.tile()));
            if (beatFinishedExactly && slot.tile().has(Enchantment.PERFECT_STRIKE)) {
                state.loadout().refill(slot.loadoutIndex());
                emit(new CombatEvent.TileRefunded(slot.loadoutIndex(), slot.tile()));
            }
        }
        state.stanza().clear();
        List<Integer> victims = phraseVictims;
        phraseVictims = null;
        if (victims.size() >= 2) {
            emit(new CombatEvent.ComboLanded(victims.size(), List.copyOf(victims)));
        }
        emit(new CombatEvent.PhraseEnded(state.turn(), victims.size()));
    }

    private void resolveBeat(Combatant hero, Tile tile) {
        int repeats = tile.has(Enchantment.DOUBLE_STRIKE) && tile.type().strike() ? 2 : 1;
        strokesInBeat = repeats;
        for (int i = 0; i < repeats && hero.alive() && !state.outcome().over(); i++) {
            strokeIndex = i;
            resolveTile(hero, tile);
        }
    }

    private void resolveTile(Combatant hero, Tile tile) {
        int here = hero.tile();
        int d = hero.facing().step();
        switch (tile.type()) {
            case CUT -> strike(hero, tile, List.of(here + d));
            case THRUST -> strike(hero, tile, List.of(here + d, here + 2 * d));
            case SWEEP -> strike(hero, tile, List.of(here + d, here - d));
            case PARRY -> {
                hero.statuses().apply(Status.GUARD, 1);
                hero.statuses().armCounter();
                emit(new CombatEvent.StatusApplied(hero.id(), Status.GUARD, 1));
            }
            case DRAW -> draw(hero, tile);
            case STEP -> advance(hero, hero.facing(), CombatEvent.MoveReason.STEP, true);
            case BACK_STEP -> advance(hero, hero.facing().opposite(), CombatEvent.MoveReason.BACK_STEP, false);
            case FEINT -> advance(hero, hero.facing(), CombatEvent.MoveReason.FEINT, false);
            case TURN -> turnBody(hero, hero.facing().opposite(), CombatEvent.TurnReason.TILE);
        }
    }

    /** A stroke over a set of tiles that hits <em>every</em> body it crosses. */
    private void strike(Combatant attacker, Tile tile, List<Integer> rawTiles) {
        List<Integer> tiles = onLane(rawTiles);
        emit(new CombatEvent.Swung(attacker.id(), tiles, contactAt()));
        boolean touched = false;
        for (int t : tiles) {
            Combatant target = state.at(t);
            if (target == null || target == attacker) {
                continue;
            }
            touched = true;
            int dealt = damage(target, tile.damage(), CombatEvent.HitSource.BLADE, attacker);
            if (dealt > 0) {
                onHit(tile, target, attacker, t);
            }
        }
        if (!touched) {
            emit(new CombatEvent.Whiffed(attacker.id(), tiles));
        }
    }

    /**
     * Contact at distance: strike the first body ahead and haul it one tile in.
     *
     * <p>The pull is a displacement, so {@link Trait#UNYIELDING} refuses it -- but
     * the blade still lands first. That split is deliberate and is the general
     * rule for the trait: Unyielding refuses to be <em>moved</em>, not to be
     * <em>touched</em>.
     */
    private void draw(Combatant hero, Tile tile) {
        int d = hero.facing().step();
        List<Integer> line = new ArrayList<>();
        for (int k = 1; k <= TileType.DRAW_RANGE; k++) {
            int t = hero.tile() + k * d;
            if (state.lane().contains(t)) {
                line.add(t);
            }
        }
        emit(new CombatEvent.Swung(hero.id(), List.copyOf(line), contactAt()));
        Combatant target = firstOccupant(line);
        if (target == null) {
            emit(new CombatEvent.Whiffed(hero.id(), List.copyOf(line)));
            return;
        }
        int held = target.tile();
        int dealt = damage(target, tile.damage(), CombatEvent.HitSource.BLADE, hero);
        if (dealt > 0) {
            onHit(tile, target, hero, held);
        }
        if (!target.alive()) {
            return;
        }
        if (target.hasTrait(Trait.UNYIELDING)) {
            emit(new CombatEvent.VerbRefused(hero.id(), target.id(), "draw",
                    CombatEvent.RefusalReason.UNYIELDING));
            return;
        }
        int dest = target.tile() - d;
        // The line of force: the hero's blade, held out on its leading side, hooked
        // into the target's body. combat-design.md 2.2 calls the Draw "contact at
        // distance", which is exactly what an IK chain cannot guess from tile
        // indices -- the blade is the far end of the reach and the torso is the near
        // end of the haul.
        Meeting hook = new Meeting(
                ContactPoint.leading(hero, ContactPoint.Part.BLADE, ContactPoint.Height.MIDDLE),
                ContactPoint.on(target, hero, ContactPoint.Part.TORSO, ContactPoint.Height.MIDDLE),
                contactAt());
        if (!state.lane().contains(dest) || state.occupied(dest)) {
            emit(new CombatEvent.Drawn(hero.id(), target.id(), target.tile(), target.tile(), hook));
            emit(new CombatEvent.MoveBlocked(target.id(), target.tile(), dest,
                    state.lane().contains(dest) ? CombatEvent.BlockReason.OCCUPIED
                            : CombatEvent.BlockReason.LANE_EDGE));
            return;
        }
        emit(new CombatEvent.Drawn(hero.id(), target.id(), target.tile(), dest, hook));
        moveTo(target, dest, CombatEvent.MoveReason.DRAWN);
        turnBody(target, Facing.toward(target.tile(), hero.tile()), CombatEvent.TurnReason.HAULED);
    }

    /**
     * One tile of travel.
     *
     * @param useVerb true when walking into a body triggers the hero's verb.
     *                Forward motion does; a Back-step does not, because retreat is
     *                not an advance, and neither does a Feint, whose whole point
     *                is motion with no contact.
     */
    private void advance(Combatant mover, Facing direction, CombatEvent.MoveReason reason, boolean useVerb) {
        int dest = mover.tile() + direction.step();
        if (!state.lane().contains(dest)) {
            emit(new CombatEvent.MoveBlocked(mover.id(), mover.tile(), dest, CombatEvent.BlockReason.LANE_EDGE));
            return;
        }
        Combatant occupant = state.at(dest);
        if (occupant == null) {
            moveTo(mover, dest, reason);
            return;
        }
        if (!useVerb) {
            emit(new CombatEvent.MoveBlocked(mover.id(), mover.tile(), dest, CombatEvent.BlockReason.OCCUPIED));
            return;
        }
        verb.resolveContact(verbContext, mover, occupant, direction);
    }

    // -- damage ----------------------------------------------------------------

    /**
     * Apply damage and say so. Returns the damage that actually landed, which is
     * zero when a guard stood in front of it.
     *
     * <p>Order of the two flag statuses is fixed and matters: <b>Guard resolves
     * before Marked</b>. A body that is both guarded and marked spends the guard
     * and keeps the mark, because Marked doubles the next hit <em>taken</em> and a
     * negated attack was not taken. The other order would let a single guard
     * silently eat the mark, which is strictly worse for the player and reads as a
     * bug at the table.
     *
     * <p>Neither flag applies to {@link CombatEvent.HitSource#SEEPING}: a raised
     * brushstroke stands between body and blade, and a wound that is already
     * bleeding is behind it.
     */
    private int damage(Combatant target, int amount, CombatEvent.HitSource source, Combatant from) {
        if (!target.alive() || amount <= 0) {
            return 0;
        }
        int fromId = from == null ? -1 : from.id();
        if (source.isAttack() && target.statuses().guard()) {
            boolean counter = target.statuses().counterArmed() && from != null && from.alive();
            Meeting crossing = crossing(target, from);
            if (counter) {
                emit(new CombatEvent.BladeMet(target.id(), fromId, crossing));
            } else {
                emit(new CombatEvent.GuardHeld(target.id(), fromId, crossing));
            }
            target.statuses().clear(Status.GUARD);
            emit(new CombatEvent.StatusConsumed(target.id(), Status.GUARD));
            if (counter) {
                emit(new CombatEvent.Countered(target.id(), fromId, COUNTER_DAMAGE));
                damage(from, COUNTER_DAMAGE, CombatEvent.HitSource.COUNTER, target);
            }
            return 0;
        }
        boolean doubled = false;
        if (source.isAttack() && target.statuses().marked()) {
            doubled = true;
            target.statuses().clear(Status.MARKED);
            emit(new CombatEvent.StatusConsumed(target.id(), Status.MARKED));
            amount *= 2;
        }
        int before = target.hp();
        if (amount == before) {
            beatFinishedExactly = true;
        }
        target.hp(before - amount);
        emit(new CombatEvent.Hit(fromId, target.id(), amount, Math.max(0, target.hp()), source, doubled,
                landing(target, from, source)));
        if (target.hp() <= 0) {
            kill(target, from, source);
        }
        return amount;
    }

    /**
     * Where two blades cross, named once in each body's frame.
     *
     * <p>Both points share a height, because they are one physical event; they may
     * differ in {@link ContactPoint.Side}, because a guard raised by a body facing
     * the wrong way catches the blade behind it, and that is a different drawing
     * from a squared-up deflection. The height comes from how far the stroke
     * travelled: adjacent it comes down, from reach two it comes in flat.
     */
    private Meeting crossing(Combatant defender, Combatant attacker) {
        if (attacker == null) {
            return new Meeting(
                    ContactPoint.leading(defender, ContactPoint.Part.BLADE, ContactPoint.Height.HIGH),
                    ContactPoint.leading(defender, ContactPoint.Part.BLADE, ContactPoint.Height.HIGH),
                    contactAt());
        }
        ContactPoint.Height height =
                ContactPoint.heightForReach(Math.abs(attacker.tile() - defender.tile()));
        return new Meeting(
                ContactPoint.leading(attacker, ContactPoint.Part.BLADE, height),
                ContactPoint.on(defender, attacker, ContactPoint.Part.BLADE, height),
                contactAt());
    }

    /**
     * Where a blow landed on the body that took it -- STYLE.md 7.3's bloom of ink
     * needs a place to spread from, and a stain that always blooms from the same
     * spot is a decal.
     *
     * <p>A Seeping tick has no point at all, and the null says so: nothing touched
     * the body, the wound was already open. That is the same distinction
     * {@link CombatEvent.HitSource#isAttack()} draws for Guard and Marked, seen
     * from the animation side.
     */
    private static ContactPoint landing(Combatant target, Combatant from, CombatEvent.HitSource source) {
        if (source == CombatEvent.HitSource.SEEPING || from == null) {
            return null;
        }
        int distance = Math.abs(from.tile() - target.tile());
        return switch (source) {
            case BLOOM, SHOCKWAVE ->
                // Ink thrown across a tile arrives at the hem, not at the chest.
                    ContactPoint.on(target, from, ContactPoint.Part.TORSO, ContactPoint.Height.LOW);
            case COLLISION ->
                    ContactPoint.on(target, from, ContactPoint.Part.TORSO, ContactPoint.Height.MIDDLE);
            default ->
                    ContactPoint.on(target, from, ContactPoint.Part.TORSO,
                            ContactPoint.heightForReach(distance));
        };
    }

    private void kill(Combatant target, Combatant from, CombatEvent.HitSource source) {
        int where = target.tile();
        Dissolve dissolve = Dissolve.of(target, from, source);
        target.kill();
        emit(new CombatEvent.Died(target.id(), where, from == null ? -1 : from.id(), dissolve));
        if (phraseVictims != null && !target.isHero()) {
            phraseVictims.add(target.id());
        }
        if (target.hasTrait(Trait.EXPLOSIVE)) {
            List<Integer> tiles = onLane(List.of(where - 1, where + 1));
            emit(new CombatEvent.Bloomed(target.id(), where, EnemyArchetype.BLOOM_DAMAGE, tiles));
            for (int t : tiles) {
                Combatant victim = state.at(t);
                if (victim != null) {
                    damage(victim, EnemyArchetype.BLOOM_DAMAGE, CombatEvent.HitSource.BLOOM, target);
                }
            }
        }
    }

    private void onHit(Tile tile, Combatant target, Combatant attacker, int targetTile) {
        Enchantment e = tile.enchantment();
        if (e == null) {
            return;
        }
        switch (e) {
            case SHOCKWAVE -> {
                for (int t : onLane(List.of(targetTile - 1, targetTile + 1))) {
                    Combatant splash = state.at(t);
                    if (splash != null && splash != attacker) {
                        damage(splash, SHOCKWAVE_DAMAGE, CombatEvent.HitSource.SHOCKWAVE, attacker);
                    }
                }
            }
            case SEEPING -> applyStatus(target, Status.SEEPING, Statuses.DURATION);
            case STILLNESS -> applyStatus(target, Status.STILLNESS, Statuses.DURATION);
            case MARKING -> applyStatus(target, Status.MARKED, 1);
            default -> {
            }
        }
    }

    private void applyStatus(Combatant target, Status status, int turns) {
        if (!target.alive()) {
            return;
        }
        target.statuses().apply(status, turns);
        emit(new CombatEvent.StatusApplied(target.id(), status, turns));
    }

    // -- the enemy phase -------------------------------------------------------

    private void enemyPhase() {
        emit(new CombatEvent.EnemyPhaseBegan(state.turn()));
        previousBeat = null;
        enemyBeatIndex = 0;
        for (Combatant e : state.enemies()) {
            if (state.outcome().over()) {
                break;
            }
            enemyStep(e);
        }
        emit(new CombatEvent.EnemyPhaseEnded(state.turn()));
        checkOutcome();
    }

    private void enemyStep(Combatant e) {
        if (!e.alive()) {
            return;
        }
        if (e.statuses().stillness() > 0) {
            // A body whose pigment has dried still occupies a beat -- 1.4 calls for
            // motion damping raised hard, which is a thing to draw, not an absence of
            // one. It gets the shape of a held breath and a focus on itself.
            beginEnemyBeat(e, Intent.Kind.HOLD, e.tile(), e.tile());
            emit(new CombatEvent.Immobilised(e.id(), e.statuses().stillness()));
            return;
        }
        if (e.hasTrait(Trait.QUICK)) {
            declareIntent(e);
        }
        Intent intent = e.intent();
        if (intent == null) {
            return;
        }
        e.intent(null);
        beginEnemyBeat(e, intent.kind(), e.tile(), reachOf(e, intent));
        switch (intent.kind()) {
            case ATTACK -> enemyAttack(e, intent);
            case ADVANCE -> enemyAdvance(e, intent);
            case WITHDRAW -> enemyReposition(e, intent, false);
            case CLOSE_IN -> enemyReposition(e, intent, true);
            case HOLD -> {
            }
        }
    }

    /**
     * A declared blade resolving.
     *
     * <p>The stroke stops at the <em>first</em> body in the line, whoever it
     * belongs to. Standing an ally in front of a Reacher therefore eats the blow,
     * which is a way to kill Charted Shadows without touching them and one of the
     * better things a push or a swap can set up.
     *
     * <p><b>The body strikes and stays.</b> The footwork that follows -- giving
     * ground, or walking in if it is Aggressive -- is not executed here. It is
     * recorded as a debt ({@link Combatant#owesStep()}), declared at upkeep like
     * everything else, and walked on the following turn. Two reasons, and the
     * second is the general one.
     *
     * <p>First, gluing the step to the strike made the cycle
     * <em>advance, strike-and-vanish</em> -- period two, against a hero whose
     * cadences are also even, and two cycles of the same parity phase-lock rather
     * than drift. A hero alternating bank and execute could then never land a Cut
     * on a Wisp: not rarely, never, from half the starting phases. Unglued, the
     * cycle is three beats and the body stands adjacent for two consecutive hero
     * turns instead of one, so any cadence of period two has an execution inside
     * the window whatever its phase. combat-design.md 3d.1 has the full account.
     *
     * <p>Second, and this is the invariant the fix encodes: <b>a Strikethrough is
     * a promise the player can act on.</b> A body that leaves the tile it
     * threatened from in the same instant it resolves has announced a threat but
     * never offered a target. Every threatening body owes the player one complete
     * turn standing where it struck.
     */
    private void enemyAttack(Combatant e, Intent declared) {
        List<Integer> now = threatTiles(e, e.facing(), e.reach());
        if (!now.equals(declared.threatened())) {
            emit(new CombatEvent.IntentRetargeted(e.id(), declared.threatened(), now));
        }
        emit(new CombatEvent.Swung(e.id(), now, contactAt()));
        Combatant victim = firstOccupant(now);
        if (victim == null) {
            emit(new CombatEvent.Whiffed(e.id(), now));
        } else {
            damage(victim, declared.damage(), CombatEvent.HitSource.BLADE, e);
        }
        if (!e.alive() || state.outcome().over()) {
            return;
        }
        e.owesStep(true);
    }

    /** The footwork a body declared after striking, one turn late and on purpose. */
    private void enemyReposition(Combatant e, Intent declared, boolean closing) {
        Combatant hero = state.hero();
        Facing toHero = Facing.toward(e.tile(), hero.tile());
        Facing walk = closing ? toHero : toHero.opposite();
        int dest = e.tile() + walk.step();
        if (dest != declared.destination()) {
            emit(new CombatEvent.IntentRetargeted(e.id(), declared.threatened(), List.of(dest)));
        }
        oneStep(e, walk, closing ? CombatEvent.MoveReason.CLOSED_IN : CombatEvent.MoveReason.GAVE_GROUND);
    }

    private void enemyAdvance(Combatant e, Intent declared) {
        int dest = walkTarget(e);
        if (dest == e.tile()) {
            int wanted = e.tile() + e.facing().step();
            emit(new CombatEvent.MoveBlocked(e.id(), e.tile(), wanted,
                    state.lane().contains(wanted) ? CombatEvent.BlockReason.OCCUPIED
                            : CombatEvent.BlockReason.LANE_EDGE));
            return;
        }
        if (dest != declared.destination()) {
            emit(new CombatEvent.IntentRetargeted(e.id(), declared.threatened(), List.of(dest)));
        }
        moveTo(e, dest, e.hasTrait(Trait.CHARGER) ? CombatEvent.MoveReason.CHARGE
                : CombatEvent.MoveReason.ADVANCE);
    }

    /** How far this body would walk: one tile, or every open tile for a Charger. */
    private int walkTarget(Combatant e) {
        int d = e.facing().step();
        int max = e.hasTrait(Trait.CHARGER) ? state.lane().length() : 1;
        int dest = e.tile();
        for (int k = 0; k < max; k++) {
            int next = dest + d;
            if (!state.lane().contains(next) || state.occupied(next)) {
                break;
            }
            dest = next;
        }
        return dest;
    }

    private void oneStep(Combatant e, Facing direction, CombatEvent.MoveReason reason) {
        int dest = e.tile() + direction.step();
        if (!state.lane().contains(dest)) {
            emit(new CombatEvent.MoveBlocked(e.id(), e.tile(), dest, CombatEvent.BlockReason.LANE_EDGE));
            return;
        }
        if (state.occupied(dest)) {
            emit(new CombatEvent.MoveBlocked(e.id(), e.tile(), dest, CombatEvent.BlockReason.OCCUPIED));
            return;
        }
        moveTo(e, dest, reason);
    }

    // -- upkeep ----------------------------------------------------------------

    private void upkeep() {
        for (Combatant c : List.copyOf(state.all())) {
            if (c.alive()) {
                tickStatuses(c);
            }
        }
        checkOutcome();
        if (state.outcome().over()) {
            return;
        }
        state.loadout().recover();
        declareIntents();
    }

    private void tickStatuses(Combatant c) {
        if (c.statuses().seeping() > 0) {
            damage(c, 1, CombatEvent.HitSource.SEEPING, null);
            boolean expired = c.statuses().tickDown(Status.SEEPING);
            emit(new CombatEvent.StatusTicked(c.id(), Status.SEEPING, 1, c.statuses().seeping()));
            if (expired) {
                emit(new CombatEvent.StatusExpired(c.id(), Status.SEEPING));
            }
        }
        if (c.alive() && c.statuses().stillness() > 0) {
            boolean expired = c.statuses().tickDown(Status.STILLNESS);
            emit(new CombatEvent.StatusTicked(c.id(), Status.STILLNESS, 0, c.statuses().stillness()));
            if (expired) {
                emit(new CombatEvent.StatusExpired(c.id(), Status.STILLNESS));
            }
        }
    }

    // -- intent ----------------------------------------------------------------

    private void declareIntents() {
        for (Combatant e : state.enemies()) {
            if (!e.hasTrait(Trait.QUICK)) {
                declareIntent(e);
            }
        }
    }

    /**
     * Publish one Strikethrough.
     *
     * <p>A Charted Shadow squares up to the hero for free, whereas the hero pays a
     * whole turn to turn around. That asymmetry is inherited from the source and
     * is the one rule here I would change first -- see the note in the package
     * report -- but it is faithful, so it is what is implemented.
     */
    private void declareIntent(Combatant e) {
        Combatant hero = state.hero();
        if (!hero.alive()) {
            return;
        }
        Facing want = Facing.toward(e.tile(), hero.tile());
        if (e.facing() != want) {
            emit(new CombatEvent.Turned(e.id(), e.facing(), want, CombatEvent.TurnReason.OWN_CHOICE));
            e.facing(want);
        }
        Intent intent = owedStep(e, want);
        if (intent == null) {
            int distance = Math.abs(hero.tile() - e.tile());
            if (distance >= 1 && distance <= e.reach()) {
                intent = Intent.attack(want, threatTiles(e, want, e.reach()), e.damage(), e.tile());
            } else {
                intent = Intent.advance(want, walkTarget(e));
            }
        }
        e.intent(intent);
        emit(new CombatEvent.IntentDeclared(e.id(), intent));
    }

    /**
     * The step a body owes after a strike, or {@code null} when it owes none.
     *
     * <p>Aggressive bodies walk in, everything else gives ground -- the trait is
     * unchanged, only its timing is. <b>Aggressive keeps its pressure exactly.</b>
     * A body that has nowhere to put the step -- the lane ran out, or the hero is
     * standing in the tile, which at reach 1 is every single turn of a Bulwark's
     * life -- owes nothing, declares nothing, and simply squares up and strikes
     * again. So the archetype that walks into you still attacks every turn, and
     * the only case where the close-in becomes a declared beat of its own is the
     * one where it is a real displacement the player deserves to see coming: a
     * reach-2 Aggressive body stepping from the far edge of its threat into the
     * near one.
     *
     * <p>The debt is discharged either way. A body that could not step has tried
     * and failed, which is a turn spent, not a turn owed.
     */
    private Intent owedStep(Combatant e, Facing toHero) {
        if (!e.owesStep()) {
            return null;
        }
        e.owesStep(false);
        boolean closing = e.hasTrait(Trait.AGGRESSIVE);
        int dest = e.tile() + (closing ? toHero : toHero.opposite()).step();
        if (!state.lane().contains(dest) || state.occupied(dest)) {
            return null;
        }
        return closing ? Intent.closeIn(toHero, dest) : Intent.withdraw(toHero, dest);
    }

    private List<Integer> threatTiles(Combatant e, Facing facing, int reach) {
        List<Integer> out = new ArrayList<>();
        for (int k = 1; k <= reach; k++) {
            int t = e.tile() + k * facing.step();
            if (state.lane().contains(t)) {
                out.add(t);
            }
        }
        return List.copyOf(out);
    }

    // -- shared helpers --------------------------------------------------------

    private void moveTo(Combatant who, int dest, CombatEvent.MoveReason reason) {
        emit(new CombatEvent.Moved(who.id(), who.tile(), dest, reason, Force.of(reason, dest - who.tile())));
        who.tile(dest);
    }

    // -- staging ---------------------------------------------------------------

    /**
     * Blank the staging at the top of every command.
     *
     * <p><b>This is a preview-purity rule, not tidiness.</b> {@link #preview}
     * builds a fresh engine over a cloned board and replays one command on it, so
     * the ghost starts from blank staging while the live engine would otherwise be
     * carrying whatever the previous turn left behind. Every field here is set
     * before it is read on any path currently reachable -- a contact only happens
     * inside a beat, and every beat opens by setting all of them. But the paths
     * that come <em>close</em> to breaking that are real and unobvious: a contact
     * can be emitted during upkeep, outside any beat at all, when an Explosive body
     * dies of a Seeping tick and throws ink at a hero still holding a guard. One
     * more rule of that shape and the live engine would read a stale beat while the
     * ghost read a fresh one, and the two would report different contact instants
     * for an identical board -- a divergence no board comparison could see.
     *
     * <p>So this makes the property <b>structural rather than incidental</b>.
     * Preview purity is the thing the whole UI leans on, and it should not depend
     * on nobody ever adding a rule that speaks outside a beat.
     *
     * <p>The default is {@link Phases#BREATH} because a contact outside any beat
     * is exactly that: a body doing nothing that something happened to.
     */
    private void resetStaging() {
        beatPhases = Phases.BREATH;
        strokesInBeat = 1;
        strokeIndex = 0;
        previousBeat = null;
        enemyBeatIndex = 0;
    }

    /** The instant, within the beat now resolving, at which this stroke's contact lands. */
    private int contactAt() {
        return beatPhases.contactAt(strokeIndex, strokesInBeat);
    }

    /** Open a beat for a Charted Shadow and stage it exactly as a stanza beat is staged. */
    private void beginEnemyBeat(Combatant body, Intent.Kind kind, int from, int to) {
        BeatMark mark = BeatMark.of(body, kind, from, to, state.lane());
        Phases shape = Phases.of(kind);
        emit(new CombatEvent.EnemyBeatBegan(enemyBeatIndex++, kind, body.id(), shape,
                overlapAfter(mark), mark.focus()));
        previousBeat = mark;
        beatPhases = shape;
        strokesInBeat = 1;
        strokeIndex = 0;
    }

    /** The far end of what a declared intent is about to do. */
    private int reachOf(Combatant e, Intent intent) {
        return switch (intent.kind()) {
            case ATTACK -> e.tile() + e.reach() * e.facing().step();
            case ADVANCE -> walkTarget(e);
            case WITHDRAW, CLOSE_IN -> {
                Facing toHero = Facing.toward(e.tile(), state.hero().tile());
                yield e.tile() + (intent.kind() == Intent.Kind.CLOSE_IN ? toHero : toHero.opposite()).step();
            }
            case HOLD -> e.tile();
        };
    }

    /**
     * Whether this beat may begin before the last one has settled, and what says so.
     *
     * <p><b>The rule, in one line: a beat may lean into the previous beat's
     * follow-through unless its own geometry is that beat's output.</b> Three ways
     * that happens, in the order they are checked, because the earlier ones are the
     * more absolute:
     *
     * <ol>
     *   <li>The previous beat is still deciding where this body <em>stands</em>. A
     *       Step may be blocked, may shove, may swap; a Charted Shadow's walk may
     *       be refused. Nothing this body does next has ground to start from.</li>
     *   <li>The previous beat is still turning it. A stroke begun before the body
     *       came round is aimed at the old facing.</li>
     *   <li>The previous beat is still moving a body this one is about to read, and
     *       their tile spans touch -- a Draw hauling a target into the tile the next
     *       stroke is written for, or a Charted Shadow walking through the corridor
     *       the next one wants. This is the one that needs the footprints: two
     *       bodies at opposite ends of a fifteen-tile lane are not waiting on each
     *       other and should not be drawn as if they were.</li>
     * </ol>
     *
     * <p>A beat that reads none of that may overlap. The same body continuing gets
     * less room than two unrelated bodies do, because one body cannot be in two
     * poses at once and two bodies can -- but neither gets all of it, since
     * STYLE.md 10 fails a pass on sight of things peaking together whether or not
     * anything caused it.
     *
     * <p>Note what is deliberately <em>not</em> a dependency: a stroke that kills.
     * Beat three genuinely resolves differently when beat two cleared its target --
     * that is the whole of {@code PhraseTest} -- but the <em>gesture</em> does not.
     * The body winds up and cuts from the same place into the same tiles whether
     * anything is standing there, and combat-design.md's rule that a phrase keeps
     * resolving through an empty board is what guarantees it. Treating a kill as a
     * dependency would forbid every overlap in the game and this field would mean
     * nothing.
     */
    private Overlap overlapAfter(BeatMark now) {
        BeatMark prev = previousBeat;
        if (prev == null) {
            return Overlap.firstBeat();
        }
        boolean sameBody = prev.actor() == now.actor();
        if (sameBody && prev.movesActor()) {
            return Overlap.awaitingFooting();
        }
        if (sameBody && prev.turnsActor()) {
            return Overlap.awaitingFacing();
        }
        if (now.readsBoard() && (prev.movesOthers() || prev.movesActor()) && prev.touches(now)) {
            return Overlap.awaitingBoard();
        }
        return sameBody ? Overlap.continues() : Overlap.unrelated();
    }

    /**
     * What one beat is about to do, in the only terms {@link Overlap} needs: whose
     * it is, which tiles it touches, and which of the three dependencies it creates
     * or reads.
     */
    private record BeatMark(int actor, int from, int to, boolean movesActor, boolean turnsActor,
                            boolean movesOthers, boolean readsBoard, Focus focus) {

        static BeatMark of(Combatant hero, TileType type, Lane lane) {
            int h = hero.tile();
            int d = hero.facing().step();
            int far = switch (type) {
                case CUT, STEP, FEINT -> h + d;
                case THRUST -> h + 2 * d;
                case DRAW -> h + TileType.DRAW_RANGE * d;
                case BACK_STEP -> h - d;
                case PARRY, TURN -> h;
                // The one continuous arc through the tile in front and the tile
                // behind: the camera has to hold both ends of it.
                case SWEEP -> h + d;
            };
            int near = type == TileType.SWEEP ? h - d : h;
            boolean moves = type == TileType.STEP || type == TileType.BACK_STEP || type == TileType.FEINT;
            boolean reads = type == TileType.CUT || type == TileType.THRUST || type == TileType.SWEEP
                    || type == TileType.DRAW || type == TileType.STEP;
            return new BeatMark(hero.id(), Math.min(near, far), Math.max(near, far), moves,
                    type == TileType.TURN, type == TileType.DRAW, reads,
                    Focus.of(hero, near, far, lane));
        }

        static BeatMark of(Combatant body, Intent.Kind kind, int from, int to, Lane lane) {
            boolean moves = kind == Intent.Kind.ADVANCE || kind == Intent.Kind.WITHDRAW
                    || kind == Intent.Kind.CLOSE_IN;
            return new BeatMark(body.id(), Math.min(from, to), Math.max(from, to), moves, false, false,
                    kind != Intent.Kind.HOLD, Focus.of(body, from, to, lane));
        }

        boolean touches(BeatMark other) {
            return from <= other.to() && other.from() <= to;
        }
    }

    private void turnBody(Combatant who, Facing to, CombatEvent.TurnReason reason) {
        if (who.facing() == to) {
            return;
        }
        emit(new CombatEvent.Turned(who.id(), who.facing(), to, reason));
        who.facing(to);
    }

    private Combatant firstOccupant(List<Integer> tiles) {
        for (int t : tiles) {
            Combatant c = state.at(t);
            if (c != null) {
                return c;
            }
        }
        return null;
    }

    private List<Integer> onLane(List<Integer> tiles) {
        List<Integer> out = new ArrayList<>(tiles.size());
        for (int t : tiles) {
            if (state.lane().contains(t)) {
                out.add(t);
            }
        }
        return List.copyOf(out);
    }

    private void checkOutcome() {
        if (state.outcome().over()) {
            return;
        }
        if (!state.hero().alive()) {
            state.outcome(EncounterOutcome.DEFEAT);
            emit(new CombatEvent.EncounterEnded(EncounterOutcome.DEFEAT, state.turn()));
            return;
        }
        for (Combatant c : state.all()) {
            if (!c.isHero() && c.alive()) {
                return;
            }
        }
        state.outcome(EncounterOutcome.VICTORY);
        emit(new CombatEvent.EncounterEnded(EncounterOutcome.VICTORY, state.turn()));
    }

    private void emit(CombatEvent event) {
        current.add(event);
    }

    /** The narrow window {@link MovementVerb} implementations act through. */
    private final class VerbContext implements MovementVerb.Context {

        @Override
        public boolean inLane(int tile) {
            return state.lane().contains(tile);
        }

        @Override
        public Combatant at(int tile) {
            return state.at(tile);
        }

        @Override
        public void move(Combatant who, int toTile, CombatEvent.MoveReason reason) {
            moveTo(who, toTile, reason);
        }

        @Override
        public void damage(Combatant target, int amount, CombatEvent.HitSource source, Combatant from) {
            CombatEngine.this.damage(target, amount, source, from);
        }

        @Override
        public void emit(CombatEvent event) {
            CombatEngine.this.emit(event);
        }

        @Override
        public int contactAt() {
            return CombatEngine.this.contactAt();
        }
    }
}
