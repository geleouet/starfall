package dev.starfall.combat;

import java.util.List;

/**
 * What one command produced: the phrase of beats, and the board afterwards.
 *
 * <p>The same type comes back from {@link CombatEngine#apply} and
 * {@link CombatEngine#preview}, and that is on purpose -- a preview must be
 * indistinguishable from the real thing or it is not a preview. The only
 * difference is what {@link #state()} points at: {@code apply} returns the live
 * state, {@code preview} returns a detached copy that nothing else holds a
 * reference to.
 *
 * @param turn   the turn the command was issued on
 * @param events every beat, in order
 * @param state  the board after
 */
public record Resolution(int turn, List<CombatEvent> events, CombatState state) {

    public Resolution {
        events = List.copyOf(events);
    }

    /** Every event of the given type, in order. The normal way tests and the UI read a phrase. */
    public <E extends CombatEvent> List<E> of(Class<E> type) {
        return events.stream().filter(type::isInstance).map(type::cast).toList();
    }

    public <E extends CombatEvent> boolean any(Class<E> type) {
        return events.stream().anyMatch(type::isInstance);
    }

    public <E extends CombatEvent> E first(Class<E> type) {
        return events.stream().filter(type::isInstance).map(type::cast).findFirst().orElse(null);
    }

    public <E extends CombatEvent> int count(Class<E> type) {
        return (int) events.stream().filter(type::isInstance).count();
    }
}
