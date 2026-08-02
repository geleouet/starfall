package dev.starfall.combat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * The whole board, at one instant.
 *
 * <p>Read-only from outside the package. The engine mutates it; the UI, the
 * animation layer and the tests read it. Everything here is a fact about
 * <em>this</em> fight -- design objects like {@link Tile} and
 * {@link EnemyArchetype} live elsewhere and are shared.
 *
 * <p>{@link #copy()} is a full deep copy including the random source, which is
 * what makes {@link CombatEngine#preview} a genuine dry run rather than an
 * approximation of one.
 */
public final class CombatState {

    private final Lane lane;
    private final List<Combatant> combatants = new ArrayList<>();
    private final Loadout loadout;
    private final InkStanza stanza;
    private final int heroId;
    private Rng rng;
    private int turn = 1;
    private EncounterOutcome outcome = EncounterOutcome.ONGOING;

    CombatState(Lane lane, Loadout loadout, InkStanza stanza, int heroId, Rng rng) {
        this.lane = lane;
        this.loadout = loadout;
        this.stanza = stanza;
        this.heroId = heroId;
        this.rng = rng;
    }

    public Lane lane() {
        return lane;
    }

    public int turn() {
        return turn;
    }

    public EncounterOutcome outcome() {
        return outcome;
    }

    public Loadout loadout() {
        return loadout;
    }

    /** The Ink Stanza, top-first: {@code stanza().slots().get(0)} resolves next. */
    public InkStanza stanza() {
        return stanza;
    }

    public Rng rng() {
        return rng;
    }

    /** Every body ever placed, living or not, in creation order with the hero first. */
    public List<Combatant> all() {
        return Collections.unmodifiableList(combatants);
    }

    public Combatant hero() {
        return byId(heroId);
    }

    public Combatant byId(int id) {
        for (Combatant c : combatants) {
            if (c.id() == id) {
                return c;
            }
        }
        return null;
    }

    /**
     * Living Charted Shadows in <b>board order</b>, which is ascending tile index.
     * This is the order they resolve in (combat-design.md 1.3) and it is a total
     * order because one tile holds one body -- so the enemy phase needs no
     * tie-break and therefore no randomness.
     */
    public List<Combatant> enemies() {
        List<Combatant> out = new ArrayList<>();
        for (Combatant c : combatants) {
            if (!c.isHero() && c.alive()) {
                out.add(c);
            }
        }
        out.sort(Comparator.comparingInt(Combatant::tile));
        return Collections.unmodifiableList(out);
    }

    /** The living body standing on {@code tile}, or {@code null}. */
    public Combatant at(int tile) {
        for (Combatant c : combatants) {
            if (c.alive() && c.tile() == tile) {
                return c;
            }
        }
        return null;
    }

    public boolean occupied(int tile) {
        return at(tile) != null;
    }

    /**
     * Every standing Strikethrough, in board order -- what the UI washes in
     * vermillion this turn. A Quick body never appears here, because it does not
     * telegraph, and the absence is the mechanic.
     */
    public List<Telegraph> strikethroughs() {
        List<Telegraph> out = new ArrayList<>();
        for (Combatant e : enemies()) {
            if (e.intent() != null) {
                out.add(new Telegraph(e.id(), e.tile(), e.intent()));
            }
        }
        return Collections.unmodifiableList(out);
    }

    /** One published intent, ready to paint. */
    public record Telegraph(int entity, int fromTile, Intent intent) {
    }

    /** Union of every threatened tile, ascending. The wash, as a set of tiles. */
    public List<Integer> threatenedTiles() {
        List<Integer> out = new ArrayList<>();
        for (Telegraph t : strikethroughs()) {
            if (t.intent().isAttack()) {
                for (int tile : t.intent().threatened()) {
                    if (!out.contains(tile)) {
                        out.add(tile);
                    }
                }
            }
        }
        Collections.sort(out);
        return Collections.unmodifiableList(out);
    }

    void add(Combatant c) {
        combatants.add(c);
    }

    void advanceTurn() {
        turn++;
    }

    void outcome(EncounterOutcome o) {
        this.outcome = o;
    }

    /** A deep copy. Nothing is shared with the original except immutable design objects. */
    public CombatState copy() {
        CombatState c = new CombatState(lane, loadout.copy(), stanza.copy(), heroId, rng.copy());
        for (Combatant k : combatants) {
            c.combatants.add(k.copy());
        }
        c.turn = turn;
        c.outcome = outcome;
        return c;
    }

    /**
     * A canonical rendering of the entire board. Two states with equal
     * fingerprints are equal in every way the rules can observe, which makes this
     * the assertion of choice for "the preview did not touch anything" and for
     * seeded-replay tests.
     */
    public String fingerprint() {
        StringBuilder sb = new StringBuilder();
        sb.append("lane=").append(lane.length())
                .append(" turn=").append(turn)
                .append(" outcome=").append(outcome)
                .append(" rng=").append(rng.seedState())
                .append('\n');
        for (Combatant c : combatants) {
            sb.append("  ").append(c.fingerprint()).append('\n');
        }
        sb.append("  hand ").append(loadout.fingerprint()).append('\n');
        sb.append("  stanza ").append(stanza).append('\n');
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int t = 0; t < lane.length(); t++) {
            Combatant c = at(t);
            sb.append('[').append(c == null ? "." : c.isHero() ? "@" : c.name().charAt(0)).append(']');
        }
        return sb.toString();
    }
}
