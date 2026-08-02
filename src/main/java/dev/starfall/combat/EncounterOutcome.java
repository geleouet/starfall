package dev.starfall.combat;

/** How the one encounter stands. */
public enum EncounterOutcome {
    ONGOING,
    /** Every Charted Shadow is off the Fold. */
    VICTORY,
    /** The Night Pilgrim's ink ran out. */
    DEFEAT;

    public boolean over() {
        return this != ONGOING;
    }
}
