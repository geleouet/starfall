package dev.starfall.combat;

/**
 * The status block carried by one body.
 *
 * <p>Two of the four are durations and two are single-use flags, which is a real
 * asymmetry in combat-design.md 1.4 rather than a modelling shortcut: Seeping and
 * Stillness are measured in turns and tick down in upkeep, while Marked and Guard
 * wait, however long it takes, for the next hit.
 *
 * <p>Durations <b>refresh rather than stack</b>: re-applying Seeping to a body
 * that already has 1 turn left leaves it with 3, not 4. Stacking would make a
 * status-enchanted tile scale with queue length, which is arithmetic rather than
 * choreography.
 *
 * <p>Mutators are package-private. The engine owns transitions; everything
 * outside the package reads.
 */
public final class Statuses {

    /** Turns a freshly applied duration status lasts. */
    public static final int DURATION = 3;

    private int seeping;
    private int stillness;
    private boolean marked;
    private boolean guard;
    private boolean counterArmed;

    public int seeping() {
        return seeping;
    }

    public int stillness() {
        return stillness;
    }

    public boolean marked() {
        return marked;
    }

    public boolean guard() {
        return guard;
    }

    /**
     * True when the standing {@link Status#GUARD} came from a Parry rather than
     * from a plain shield, so absorbing an attack answers it with a counter. This
     * is what turns Guard from a negation into blade-on-blade -- the signature
     * beat of STYLE.md 7.2.
     */
    public boolean counterArmed() {
        return counterArmed;
    }

    public boolean has(Status s) {
        return switch (s) {
            case SEEPING -> seeping > 0;
            case STILLNESS -> stillness > 0;
            case MARKED -> marked;
            case GUARD -> guard;
        };
    }

    /** Remaining turns, or 1 for the flag statuses when set, 0 when not. */
    public int remaining(Status s) {
        return switch (s) {
            case SEEPING -> seeping;
            case STILLNESS -> stillness;
            case MARKED -> marked ? 1 : 0;
            case GUARD -> guard ? 1 : 0;
        };
    }

    void apply(Status s, int turns) {
        switch (s) {
            case SEEPING -> seeping = Math.max(seeping, turns);
            case STILLNESS -> stillness = Math.max(stillness, turns);
            case MARKED -> marked = true;
            case GUARD -> guard = true;
        }
    }

    void armCounter() {
        counterArmed = true;
    }

    void clear(Status s) {
        switch (s) {
            case SEEPING -> seeping = 0;
            case STILLNESS -> stillness = 0;
            case MARKED -> marked = false;
            case GUARD -> {
                guard = false;
                counterArmed = false;
            }
        }
    }

    /** Decrements a duration and reports whether it just ran out. */
    boolean tickDown(Status s) {
        switch (s) {
            case SEEPING -> {
                if (seeping > 0 && --seeping == 0) {
                    return true;
                }
            }
            case STILLNESS -> {
                if (stillness > 0 && --stillness == 0) {
                    return true;
                }
            }
            default -> {
            }
        }
        return false;
    }

    void copyFrom(Statuses other) {
        this.seeping = other.seeping;
        this.stillness = other.stillness;
        this.marked = other.marked;
        this.guard = other.guard;
        this.counterArmed = other.counterArmed;
    }

    Statuses copy() {
        Statuses c = new Statuses();
        c.copyFrom(this);
        return c;
    }

    @Override
    public String toString() {
        return "seep=" + seeping + ",still=" + stillness + ",mark=" + marked
                + ",guard=" + guard + (counterArmed ? "+counter" : "");
    }
}
