package dev.starfall.ui;

import dev.starfall.combat.CombatEvent;
import dev.starfall.combat.CombatState;
import dev.starfall.combat.Focus;
import dev.starfall.combat.InkStanza;
import dev.starfall.combat.Resolution;
import dev.starfall.stage.Schedule;
import dev.starfall.stage.ScheduledBeat;
import dev.starfall.stage.Stage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The interface's own timeline: what is written on the sheet at any second of a
 * played encounter.
 *
 * <h2>Why this exists rather than reading the engine every frame</h2>
 *
 * <p>The combat engine is a turn machine and the interface is a continuous
 * picture. A board state is a fact about a turn; a drying cartouche is a fact
 * about a second. Between the two sits exactly one mapping -- the same one
 * {@code Scheduler} makes for the bodies -- and combat-design.md 3d.5 is explicit
 * that it belongs on this side of the line: <i>"the moment the engine learns what
 * a second is, the reproducibility the whole review loop depends on becomes a
 * rendering concern."</i>
 *
 * <p>So the engine plays the encounter, the scheduler places the beats, and this
 * class records where the two agree: a mark is written when a command banks it and
 * drunk by the beat that resolves it. Nothing here decides anything about the
 * fight.
 *
 * <h2>The reading order is not re-derived, it is carried</h2>
 *
 * <p>{@link InkStanza} returns its slots top-first and says why: <i>"nothing
 * anywhere reverses a list, which is exactly the property that stops the rule from
 * having to be explained."</i> This class holds to that -- {@link Look#stanza()}
 * is top-first, the beats spend it in that order, and the only arithmetic applied
 * is the one that turns a slot index into the fixed height of {@link Look.Written},
 * which is the base-anchored column of that record's own note.
 */
public final class Readout {

    /** How long a spent cartouche takes to dry off the sheet, in seconds. */
    public static final double DRY_SECONDS = 0.55;

    /** How long fresh ink takes to spread into place. STYLE.md 8's 0.4-0.7 s. */
    public static final double WET_SECONDS = 0.55;

    /** How long a Strikethrough takes to wick across the tiles it threatens. */
    public static final double BLEED_SECONDS = 0.60;

    /** Wetness of the topmost queued mark: the newest ink on the sheet. */
    public static final double QUEUED_WETNESS = 0.92;

    /** How much drier each line below it is. */
    public static final double QUEUED_DRYING = 0.13;

    /** One board state, standing from {@code at} until the next one. */
    private record Chapter(double at, CombatState state, List<Integer> reached) {
    }

    /** One cartouche and its whole life, in seconds. */
    private record Mark(Look.Written shape, double writtenAt, double resolveFrom, double resolveTo) {
    }

    private final Stage stage;
    private final Schedule schedule;
    private final List<Chapter> chapters;
    private final List<Mark> marks;
    private final double planningWidth;

    private Readout(Stage stage, Schedule schedule, List<Chapter> chapters, List<Mark> marks) {
        this.stage = stage;
        this.schedule = schedule;
        this.chapters = List.copyOf(chapters);
        this.marks = List.copyOf(marks);
        this.planningWidth = widestOf(schedule, stage);
    }

    /**
     * The widest shot this score actually holds.
     *
     * <p>Read off the schedule rather than from {@code stage.planning()}, because
     * since {@link Stage#planning(Standing)} the two are different questions: the
     * lane's own framing is what decides the <em>duration</em> of a push-in, and the
     * shot the camera really opens on is the exchange's. {@link #intimacy(double)}
     * is "how far in from the widest shot", so it has to be normalised by the shot
     * that exists -- otherwise the interface arrives at the planning framing already
     * two thirds receded, which is a recession with no camera move under it.
     */
    private static double widestOf(Schedule schedule, Stage stage) {
        double widest = 0.0;
        for (dev.starfall.stage.Directive.CameraKey k : schedule.camera()) {
            widest = Math.max(widest, Math.max(k.from().widthTiles(), k.to().widthTiles()));
        }
        return widest > 0.0 ? widest : stage.planning().widthTiles();
    }

    public static Builder builder(Stage stage) {
        return new Builder(stage);
    }

    public Schedule schedule() {
        return schedule;
    }

    /** What the sheet shows at {@code t}. */
    public Look at(double t) {
        Chapter c = chapterAt(t);
        CombatState s = c.state();

        List<Look.Written> stanza = new ArrayList<>();
        List<Double> arriving = new ArrayList<>();
        for (Mark m : marks) {
            if (t < m.writtenAt() || t >= m.resolveTo() + DRY_SECONDS) {
                continue;
            }
            double spent = m.resolveFrom() >= Double.MAX_VALUE ? 0.0
                    : clamp01((t - m.resolveFrom()) / Math.max(1e-6, m.resolveTo() - m.resolveFrom()));
            double dried = t <= m.resolveTo() ? 0.0
                    : clamp01((t - m.resolveTo()) / DRY_SECONDS);
            stanza.add(new Look.Written(m.shape().type(), m.shape().enchantment(), m.shape().height(),
                    wetness(t, m, spent, dried), spent, dried, m.shape().seed()));
            // How much of the mark's arrival is still running: 1 at the instant it
            // is written, 0 once the pigment has settled. STYLE.md 8's 0.4-0.7 s.
            arriving.add(1.0 - clamp01((t - m.writtenAt()) / WET_SECONDS));
        }
        // Top first, which is the order the beats spend them and the order the
        // column is read downward. The comparator is the only place the height of
        // Look.Written is turned back into a reading order, and it is a descent
        // because the column is anchored at its base -- see Look.Written.
        List<Look.Written> unsorted = List.copyOf(stanza);
        stanza.sort((a, b) -> Integer.compare(b.height(), a.height()));

        // <b>And the column dries downward.</b> A queued mark's wetness falls with
        // its reading position, so the newest tile -- which is the topmost, and the
        // next to resolve -- is the wettest ink on the sheet and the one written
        // first is the driest.
        //
        // This is what makes combat-design.md 3's argument true of the picture
        // rather than only of the ordering: "what you wrote last is at the top and
        // goes first, so the player never learns LIFO, they just read downward". A
        // column of five equally wet marks tells the player the order but not the
        // reason, and until this the delivered planning shot was exactly that --
        // every tile of the opening stanza is banked before the score starts, so
        // they all carried the same age and the device demonstrated nothing.
        //
        // Ranked rather than timed on purpose. {@code InkStanza.Slot} carries
        // {@code bankedOnTurn} "so the UI can age the ink", and a turn is the unit
        // the queue is built in; ageing in seconds instead would make a player who
        // stared at the board lose the gradient without anything having changed.
        for (int i = 0; i < stanza.size(); i++) {
            Look.Written w = stanza.get(i);
            if (w.spent() > 0.0 || w.dried() > 0.0) {
                continue;
            }
            double ranked = QUEUED_WETNESS - QUEUED_DRYING * i;
            // Fresh ink starts at full and settles onto its rank, so a tile just
            // banked arrives by spreading rather than by appearing at its resting
            // value -- and the settle is continuous, so nothing steps.
            double arrive = arriving.get(unsorted.indexOf(w));
            stanza.set(i, new Look.Written(w.type(), w.enchantment(), w.height(),
                    ranked + (1.0 - ranked) * arrive, w.spent(), w.dried(), w.seed()));
        }

        List<Look.Held> hand = new ArrayList<>();
        for (int i = 0; i < s.loadout().size(); i++) {
            hand.add(new Look.Held(s.loadout().tile(i).type(), s.loadout().tile(i).enchantment(),
                    s.loadout().charges(i), s.loadout().tile(i).cooldown(), s.loadout().banked(i)));
        }

        return new Look(t, Collections.unmodifiableList(stanza), Collections.unmodifiableList(hand),
                s.hero().hp(), s.hero().maxHp(), s.lane().length(),
                s.hero().tile(), s.hero().facing().step(),
                c.reached(), s.threatenedTiles(),
                clamp01((t - c.at()) / BLEED_SECONDS), intimacy(t));
    }

    /**
     * How far the camera has pushed in, 0 at the planning framing and 1 at the
     * intimate one.
     *
     * <p><b>The interface's own recession is driven by this and by nothing
     * else</b>, and that is the point. STYLE.md 9 makes the camera a continuous
     * function of eased keys and {@code Schedule.cameraIsContinuous} asserts that
     * the keys join, so a quantity derived from the framing width inherits the
     * continuity: the interface cannot pop, by the same theorem that says the
     * camera cannot cut. Deriving it instead from "is a beat running" -- the
     * obvious way -- would be a boolean that steps 0 to 1 inside one frame, which
     * is a pop composited over a glide.
     */
    public double intimacy(double t) {
        double w = schedule.framingAt(t).widthTiles();
        double span = planningWidth - Stage.INTIMACY_TILES;
        if (span <= 1e-9) {
            return 1.0;
        }
        return clamp01((planningWidth - w) / span);
    }

    /**
     * How much air the camera is looking through, 0 at the intimate framing and 1 at
     * a shot as wide as the Fold's own middle game.
     *
     * <h2>Why distance costs value, and why it is a width and not an intimacy</h2>
     *
     * <p>STYLE.md 9 asks the planning framing for <i>"the full lane readable, figures
     * small, heavy fog, Family C mood"</i>. The pass-1 review delivered the first two
     * and measured the absence of the last two: <b>the 86 px hero held 32.5 luminance
     * of contrast against its local sky where the 280 px hero held 38.4</b> -- 85% of
     * the contrast at a third of the size, which is no atmosphere at all. Family C is
     * <i>"background figures desaturate and half-dissolve into mist"</i>, and the
     * review's verdict on the wide shot was that it is 95% smooth gradient because
     * the gradient does nothing.
     *
     * <p>It is <b>not</b> {@code 1 - intimacy}. Intimacy is normalised per lane, so a
     * five-tile knife fight -- whose planning framing is 6.50 tiles and whose figures
     * are already ~180 px -- would carry the same haze as a fifteen-tile approach at
     * 16.50. Air does not know how long the lane is. So this is a function of the
     * framing width itself, full at {@link #HAZE_FULL_TILES}, which is the middle
     * game's own planning framing: the knife fight lands at 0.35 of it and the
     * approach is saturated.
     *
     * <p>Continuous, for the reason {@link #intimacy(double)} gives: it is derived
     * from the framing width, so it inherits {@code Schedule.cameraIsContinuous} and
     * cannot step.
     */
    public double haze(double t) {
        double w = schedule.framingAt(t).widthTiles();
        return clamp01((w - Stage.INTIMACY_TILES) / (HAZE_FULL_TILES - Stage.INTIMACY_TILES));
    }

    /** The framing width at which the air is as thick as it gets: the middle game's wide shot. */
    public static final double HAZE_FULL_TILES = 12.5;

    private double wetness(double t, Mark m, double spent, double dried) {
        if (dried > 0.0) {
            return Math.max(0.0, 1.0 - dried);
        }
        if (spent > 0.0) {
            // The beat is drinking this mark: it is the one wet thing in the column.
            return 1.0;
        }
        // Fresh ink spreads into place and holds at full for its first half-second,
        // which is STYLE.md 8's "elements arrive by pigment spreading into place
        // (0.4-0.7 s)". Anything past that is ranked by the loop in at(), which is
        // where the column's own gradient is set.
        double age = t - m.writtenAt();
        return age < WET_SECONDS ? 1.0 : QUEUED_WETNESS;
    }

    private Chapter chapterAt(double t) {
        Chapter best = chapters.get(0);
        for (Chapter c : chapters) {
            if (c.at() > t + 1e-9) {
                break;
            }
            best = c;
        }
        return best;
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : v > 1 ? 1 : v;
    }

    /** Every tile the beats of {@code resolution} happen over -- the phrase's footprint. */
    public static List<Integer> footprint(Resolution resolution) {
        List<Integer> out = new ArrayList<>();
        for (CombatEvent.BeatBegan b : resolution.of(CombatEvent.BeatBegan.class)) {
            Focus f = b.focus();
            for (int tile = f.fromTile(); tile <= f.toTile(); tile++) {
                if (!out.contains(tile)) {
                    out.add(tile);
                }
            }
        }
        Collections.sort(out);
        return List.copyOf(out);
    }

    // -- building --------------------------------------------------------------

    /**
     * Records the sheet as an encounter is played.
     *
     * <p>Used by the scene that stages the encounter, beside the calls that feed
     * the same events to the {@code Scheduler}, so the two timelines are built from
     * one play-through rather than from two.
     */
    public static final class Builder {

        private final Stage stage;
        private final List<Chapter> chapters = new ArrayList<>();
        private final List<Mark> pending = new ArrayList<>();
        private final List<Mark> done = new ArrayList<>();
        private float seed = 1.7f;

        private Builder(Stage stage) {
            this.stage = stage;
        }

        /** The board from {@code at} until the next chapter. */
        public Builder board(double at, CombatState state, List<Integer> reached) {
            chapters.add(new Chapter(at, state.copy(), List.copyOf(reached)));
            return this;
        }

        /**
         * A tile banked: one more mark written above the last, at the top of the
         * column.
         *
         * <p>The height is the count of marks already standing, which is the
         * banking order and therefore fixed for the life of the mark -- see
         * {@link Look.Written}.
         */
        public Builder bank(double at, dev.starfall.combat.Tile tile) {
            seed += 3.1f;
            pending.add(new Mark(new Look.Written(tile.type(), tile.enchantment(), pending.size(),
                    1.0, 0.0, 0.0, seed), at, Double.MAX_VALUE, Double.MAX_VALUE));
            return this;
        }

        /**
         * The stanza resolves: the beats of {@code beats} drink the marks from the
         * top of the column downward.
         *
         * <p>{@code beats} is the run of {@link ScheduledBeat.Bracket#STANZA} beats
         * the scheduler placed for this execution, in order. The engine emits them
         * top-first because {@link InkStanza#slots()} is top-first, so beat
         * <i>i</i> is the mark at height {@code size - 1 - i} and no list is
         * reversed anywhere.
         */
        public Builder resolve(List<ScheduledBeat> beats) {
            int size = pending.size();
            for (int i = 0; i < beats.size() && i < size; i++) {
                ScheduledBeat b = beats.get(i);
                int height = size - 1 - i;
                for (int k = 0; k < pending.size(); k++) {
                    Mark m = pending.get(k);
                    if (m.shape().height() == height) {
                        pending.set(k, new Mark(m.shape(), m.writtenAt(), b.start(), b.contactEnd()));
                        break;
                    }
                }
            }
            done.addAll(pending);
            pending.clear();
            return this;
        }

        public Readout build(Schedule schedule) {
            if (chapters.isEmpty()) {
                throw new IllegalStateException("a readout needs at least one board");
            }
            List<Mark> all = new ArrayList<>(done);
            all.addAll(pending);
            return new Readout(stage, schedule, chapters, all);
        }
    }
}
