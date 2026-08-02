package dev.starfall.stage;

import dev.starfall.combat.CombatEvent;
import dev.starfall.combat.Facing;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * Where every body is and which way it is turned, tracked forward from the event
 * stream.
 *
 * <p><b>Why the staging layer keeps its own copy.</b> The brief is explicit: do
 * not reach back into the engine for state, the event stream is the interface.
 * That is the right rule -- a schedule is built once and consumed several frames
 * later, and a scheduler that queried {@code CombatState} would be reading a board
 * that has moved on, which is the exact failure {@code CombatEvent}'s own note
 * describes ("a stream stays meaningful after the state it describes has moved
 * on"). So the board is reconstructed from {@link CombatEvent.Moved} and
 * {@link CombatEvent.Turned}, which are the only two events that change it.
 *
 * <p><b>The one thing the stream does not supply.</b> {@link CombatEvent.EncounterBegan}
 * names the lane length, the hero and the ids of the Charted Shadows, and does not
 * say where any of them is standing or which way it faces. There is no other event
 * that states an opening position -- {@code Moved} states a departure, and a body
 * that never moves never appears in one. So the opening board has to be handed in.
 * That is a genuine gap in the stream rather than a design choice here, and it is
 * reported as one: a {@code List<Standing.Body>} on {@code EncounterBegan} would
 * close it and would keep the stream entirely ordinal, since a tile and a facing
 * are exactly the units the engine already speaks.
 *
 * <p>Deterministic by construction: bodies are held in a {@link TreeMap} keyed by
 * id, so {@link #bodies()} is in ascending id order on every run.
 */
public final class Standing {

    /** One body's opening position. Tiles and facings, which is all the engine has. */
    public record Body(int id, int tile, Facing facing) {
    }

    private final TreeMap<Integer, Integer> tiles = new TreeMap<>();
    private final TreeMap<Integer, Facing> facings = new TreeMap<>();

    private Standing() {
    }

    public static Standing opening(List<Body> bodies) {
        Standing s = new Standing();
        for (Body b : bodies) {
            s.tiles.put(b.id(), b.tile());
            s.facings.put(b.id(), b.facing());
        }
        return s;
    }

    public static Standing opening(Body... bodies) {
        return opening(List.of(bodies));
    }

    public Standing copy() {
        Standing s = new Standing();
        s.tiles.putAll(tiles);
        s.facings.putAll(facings);
        return s;
    }

    /** Every body known, in ascending id order. Never a hash order. */
    public List<Body> bodies() {
        List<Body> out = new ArrayList<>(tiles.size());
        for (var e : tiles.entrySet()) {
            out.add(new Body(e.getKey(), e.getValue(), facings.getOrDefault(e.getKey(), Facing.RIGHT)));
        }
        return List.copyOf(out);
    }

    /**
     * The tile {@code id} stands on. Unknown bodies read as tile 0 rather than
     * throwing: a schedule with one anchor in the wrong place is a bug worth
     * seeing on screen, and a schedule that cannot be built at all hides which
     * event introduced the body.
     */
    public int tile(int id) {
        Integer t = tiles.get(id);
        return t == null ? 0 : t;
    }

    public Facing facing(int id) {
        Facing f = facings.get(id);
        return f == null ? Facing.RIGHT : f;
    }

    /** Note a body somewhere the stream has just put it. */
    public void place(int id, int tile) {
        tiles.put(id, tile);
    }

    public void turn(int id, Facing facing) {
        facings.put(id, facing);
    }

    /** Applies whatever an event says about the board, and ignores everything else. */
    public void apply(CombatEvent event) {
        if (event instanceof CombatEvent.Moved m) {
            tiles.putIfAbsent(m.entity(), m.fromTile());
            tiles.put(m.entity(), m.toTile());
        } else if (event instanceof CombatEvent.Turned t) {
            facings.putIfAbsent(t.entity(), t.from());
            facings.put(t.entity(), t.to());
        }
    }
}
