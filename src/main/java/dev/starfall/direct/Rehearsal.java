package dev.starfall.direct;

import com.badlogic.gdx.math.Vector2;
import dev.starfall.capture.SceneClock;
import dev.starfall.rig.SamuraiRig;
import dev.starfall.stage.Directive;
import dev.starfall.stage.Schedule;

import java.util.ArrayList;
import java.util.List;

/**
 * A duel played through the whole animation stack with no GL context: the same
 * {@link Schedule}, the same {@link Director}, the same rig, the same IK, the
 * same simulation and the same substep the capture uses -- everything except the
 * two mesh uploads and the shaders.
 *
 * <h2>Why this exists, which is the whole of System 4 pass 2's method</h2>
 *
 * <p>Pass 1 shipped a parry in which <b>the two blades never came closer than
 * 21% of a body height</b>, and a clash bloom that fired two frames after the
 * closest approach, 36 px from the attacker's own grip. Every test the pass had
 * passed, because every one of them was written against the <em>schedule</em>:
 * the directives were correct, the two named halves of the {@code Meeting} were
 * 0.08 world units apart, and the arm chain reached its target. What no test
 * could see was that the target names the <em>fist</em> and the blade hangs a
 * further 0.10 out of the fist at 45 degrees, so two fists a finger's width apart
 * put two blades most of a metre apart.
 *
 * <p>STYLE.md 11.2b(e): <i>"a discipline written into a document but not into the
 * tool that reads it is documentation, not a guard."</i> The discipline missing
 * here was "assert the picture, not the intent", and the tool it needed was a way
 * to ask where the blade actually is without shooting a capture. That is this
 * class. It costs nothing -- {@link SamuraiRig#headless()} is the whole trick --
 * and it turns "the blades meet" from a claim in a pass log into an assertion in
 * {@code RehearsalTest}.
 *
 * <p><b>It is not a substitute for pixels and must never be quoted as one.</b>
 * It knows where the geometry is; it does not know what the material paints, what
 * the halo does to a silhouette, or where the camera is looking. Anything about
 * ink, value, corridors of paper or silhouette merging is still graded from a
 * capture. What this grades is kinematics, which is exactly the class of claim
 * pass 1 got wrong.
 */
public final class Rehearsal {

    /**
     * One frame of a rehearsal: the clock, where the load-bearing geometry is, and
     * <b>the coordinate the renderer would be handed for a {@code CROSSING} mark.</b>
     *
     * <p>That last field exists because of STYLE.md 11.2b(f), which was written about
     * this file's own test:
     *
     * <blockquote>A guard must be shown to fail… the guard for a <b>drawn</b> mark
     * reads the coordinate the renderer was handed ({@code director.lastCrossing}),
     * not a coordinate recomputed from the geometry the renderer was supposed to have
     * used. 11.2b(c) applied one level in: a test that shares its input with the code
     * under test is a bit-identity check wearing an assertion's clothes.</blockquote>
     *
     * <p>{@link #mark} is read straight off {@link Director#lastCrossing}, which is the
     * same field {@code Director.renderInk} substitutes for the directive's own origin
     * on a {@code CROSSING} anchor. Nothing recomputes it here.
     */
    public record Frame(double t, double wall, double timeScale,
                        Body hero, Body foe, Vector2 mark) {

        /** The smaller of the two blade-to-blade distances that matter. */
        public double bladeGap() {
            return segmentDistance(hero.bladeRoot, hero.bladeTip, foe.bladeRoot, foe.bladeTip);
        }

        /** The same, as a fraction of the figure's own height. STYLE.md 11.3. */
        public double bladeGapFraction() {
            return bladeGap() / SamuraiRig.FIGURE_HEIGHT;
        }

        /**
         * How far the drawn mark is from one body's blade <em>segment</em>, as a
         * fraction of figure height.
         *
         * <p>The point and the segment come from two different places on purpose:
         * the point is what {@code Director} handed the renderer, the segment is
         * where the skeleton put the blade. Pass 2's version took both from the same
         * blade and returned 0.0 for every input.
         */
        public double markToBlade(boolean heroSide) {
            Body b = heroSide ? hero : foe;
            return segmentDistance(mark, mark, b.bladeRoot, b.bladeTip) / SamuraiRig.FIGURE_HEIGHT;
        }

        /**
         * True when the mark lies between the two blades rather than beyond one of
         * them: the closest point of each blade to the mark must be on the far side
         * of the mark from the other blade's, to within a hair.
         *
         * <p>STYLE.md 5's clash is "at the point of contact", and a light welded to
         * one blade with the other 0.11-0.19 of a figure height away is the defect the
         * pass-2 review measured frame by frame. "Near both" does not say it; a bloom
         * sitting on a crossing is <em>between</em> them.
         */
        public boolean markIsBetweenTheBlades() {
            Vector2 h = nearestOn(hero.bladeRoot, hero.bladeTip, mark);
            Vector2 f = nearestOn(foe.bladeRoot, foe.bladeTip, mark);
            double ax = h.x - mark.x, ay = h.y - mark.y;
            double bx = f.x - mark.x, by = f.y - mark.y;
            double la = Math.sqrt(ax * ax + ay * ay);
            double lb = Math.sqrt(bx * bx + by * by);
            if (la < 1e-4 || lb < 1e-4) {
                return true;
            }
            // Opposing directions: the dot product of the two unit vectors is negative
            // when the mark is between, positive when both blades are the same way.
            return (ax * bx + ay * by) / (la * lb) < 0.2;
        }

        public Body body(int id) {
            return hero.id == id ? hero : foe.id == id ? foe : null;
        }
    }

    private static Vector2 nearestOn(Vector2 a, Vector2 b, Vector2 p) {
        double vx = b.x - a.x;
        double vy = b.y - a.y;
        double len2 = vx * vx + vy * vy;
        double u = len2 < 1e-12 ? 0 : ((p.x - a.x) * vx + (p.y - a.y) * vy) / len2;
        u = Math.max(0, Math.min(1, u));
        return new Vector2((float) (a.x + u * vx), (float) (a.y + u * vy));
    }

    /**
     * One figure's world geometry at one instant, plus what the sword arm was
     * <em>asked</em> for.
     *
     * <p>The asked-for half is not decoration. {@code IkChain.weight} blends
     * <em>local rotations</em>, not effector positions, so a chain at 0.85 with a
     * reachable target still leaves a residual -- and a residual the size of a
     * blade is the difference between two blades crossing and two blades missing.
     * Recording the target beside the result is STYLE.md 11.3's "print the
     * rectangle with the number" applied to kinematics.
     */
    public record Body(int id, Vector2 stand, Vector2 hips, Vector2 head, Vector2 hand,
                       Vector2 bladeRoot, Vector2 bladeCross, Vector2 bladeTip,
                       Vector2 armTarget, double armWeight, double armResidual, double bladeWeight,
                       double bladeLocalDeg, double bladeWorldDeg, double handWorldDeg,
                       Vector2 shoulder, Vector2 elbow, Vector2 pole,
                       /* System 3b pass 2: the eye bone's world position. The pass-1 gaze
                          guard was vacuous partly because nothing headless could say where
                          the EYE is, only where the head is — and the face-value guard
                          needs the same point to place its boxes on a bowed head. */
                       Vector2 eye) {
    }

    private final Duel.Kind kind;
    private final Duel.Staged staged;
    private final Director director;
    private final List<Frame> frames = new ArrayList<>();

    public Rehearsal(Duel.Kind kind) {
        this.kind = kind;
        this.staged = Duel.of(kind);
        this.director = new Director(staged.schedule(), Duel.headlessFiguresFor(staged));
        for (Figure f : director.figures()) {
            f.sim().wind(Director.BREEZE_X, Director.BREEZE_Y);
        }
        director.start();
        // The same pre-roll DuelScene does, for the same reason: a garment that has
        // not found the breeze yet is answering two things at once.
        int steps = Math.round(0.8f / SceneClock.SUBSTEP);
        for (int i = 0; i < steps; i++) {
            for (Figure f : director.figures()) {
                f.simulate(SceneClock.SUBSTEP, 0f);
            }
        }
    }

    public Duel.Kind kind() {
        return kind;
    }

    public Duel.Staged staged() {
        return staged;
    }

    public Schedule schedule() {
        return staged.schedule();
    }

    /**
     * Plays the whole score at {@code rate} samples of wall time per second and
     * records every frame.
     *
     * <p>Wall time rather than schedule time, deliberately: STYLE.md 7.3's held
     * breath makes the two differ, and the instrument that measured it -- "the
     * debug schedule cursor read at uniform wall-clock steps" -- only works
     * because the sampling is uniform in the clock the viewer experiences.
     */
    public List<Frame> play(double seconds, double rate) {
        frames.clear();
        float dt = (float) (1.0 / rate);
        double wall = 0;
        record(wall);
        while (wall < seconds) {
            // The capture's own integration, restated rather than shared because
            // SceneClock.advance takes a Scene and this drives a Director directly.
            // Same substep, same rounding: a rehearsal that integrated differently
            // from the capture would be measuring a different beat.
            int steps = Math.round(dt / SceneClock.SUBSTEP);
            for (int i = 0; i < steps; i++) {
                director.advance(SceneClock.SUBSTEP);
            }
            wall += dt;
            record(wall);
        }
        return List.copyOf(frames);
    }

    /** The whole score, at the capture's own 60 Hz. */
    public List<Frame> play() {
        return play(schedule().duration(), 60.0);
    }

    public List<Frame> frames() {
        return List.copyOf(frames);
    }

    public Director director() {
        return director;
    }

    /** The frame nearest a schedule instant. Contact instants are quoted in schedule seconds. */
    public Frame at(double scheduleSeconds) {
        Frame best = null;
        double bestGap = Double.MAX_VALUE;
        for (Frame f : frames) {
            double gap = Math.abs(f.t() - scheduleSeconds);
            if (gap < bestGap) {
                bestGap = gap;
                best = f;
            }
        }
        return best;
    }

    /** Every frame whose schedule time falls inside {@code [from, to]}. */
    public List<Frame> between(double from, double to) {
        List<Frame> out = new ArrayList<>();
        for (Frame f : frames) {
            if (f.t() >= from - 1e-9 && f.t() <= to + 1e-9) {
                out.add(f);
            }
        }
        return out;
    }

    private void record(double wall) {
        Body hero = null;
        Body foe = null;
        for (Figure f : director.figures()) {
            Body b = read(f);
            if (f.body() == staged.hero()) {
                hero = b;
            } else {
                foe = b;
            }
        }
        frames.add(new Frame(director.time(), wall, director.timeScale(), hero, foe,
                director.lastCrossing(new Vector2())));
    }

    private static Body read(Figure f) {
        var arm = f.ik().swordArm();
        return new Body(f.body(),
                new Vector2((float) f.standX(), 0f),
                f.where("hips", new Vector2()).cpy(),
                f.where("head", new Vector2()).cpy(),
                f.where("handL", new Vector2()).cpy(),
                f.bladeAt(0f, new Vector2()).cpy(),
                f.bladeCrossing(new Vector2()).cpy(),
                f.bladeAt(1f, new Vector2()).cpy(),
                new Vector2(arm.targetX(), arm.targetY()),
                arm.weight(), arm.bendSide(), f.ik().blade().weight(),
                f.ik().blade().bone().rotDeg,
                f.skeleton().worldRotationDeg(f.skeleton().bone("blade").index),
                f.skeleton().worldRotationDeg(f.skeleton().bone("handL").index),
                f.where("upperArmL", new Vector2()).cpy(),
                f.where("forearmL", new Vector2()).cpy(),
                new Vector2(arm.poleX(), arm.poleY()),
                f.where("eye", new Vector2()).cpy());
    }

    // -- geometry --------------------------------------------------------------

    /** Minimum distance between two line segments, which is what "the blades met" means. */
    public static double segmentDistance(Vector2 p1, Vector2 p2, Vector2 q1, Vector2 q2) {
        if (intersects(p1, p2, q1, q2)) {
            return 0.0;
        }
        return Math.min(
                Math.min(pointToSegment(p1, q1, q2), pointToSegment(p2, q1, q2)),
                Math.min(pointToSegment(q1, p1, p2), pointToSegment(q2, p1, p2)));
    }

    private static boolean intersects(Vector2 p1, Vector2 p2, Vector2 q1, Vector2 q2) {
        double d1 = cross(q1, q2, p1);
        double d2 = cross(q1, q2, p2);
        double d3 = cross(p1, p2, q1);
        double d4 = cross(p1, p2, q2);
        return ((d1 > 0) != (d2 > 0)) && ((d3 > 0) != (d4 > 0));
    }

    private static double cross(Vector2 a, Vector2 b, Vector2 c) {
        return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
    }

    private static double pointToSegment(Vector2 p, Vector2 a, Vector2 b) {
        double vx = b.x - a.x;
        double vy = b.y - a.y;
        double len2 = vx * vx + vy * vy;
        double u = len2 < 1e-12 ? 0 : ((p.x - a.x) * vx + (p.y - a.y) * vy) / len2;
        u = Math.max(0, Math.min(1, u));
        double dx = p.x - (a.x + u * vx);
        double dy = p.y - (a.y + u * vy);
        return Math.sqrt(dx * dx + dy * dy);
    }

    /** Every CLASH mark in this score, with the instant it is at its brightest. */
    public List<Directive.Ink> clashes() {
        List<Directive.Ink> out = new ArrayList<>();
        for (Directive.Ink d : schedule().of(Directive.Ink.class)) {
            if (d.kind() == Directive.InkKind.CLASH) {
                out.add(d);
            }
        }
        return out;
    }
}
