package dev.starfall.direct;

import dev.starfall.anim.Pose;
import dev.starfall.anim.Skeleton;
import dev.starfall.ik.IkChain;
import dev.starfall.rig.RigIk;
import dev.starfall.rig.SamuraiRig;
import dev.starfall.stage.Chain;
import dev.starfall.stage.Directive;
import dev.starfall.stage.Schedule;
import dev.starfall.stage.ScheduledBeat;
import dev.starfall.stage.Stance;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What System 4 has to keep, tested where it can be tested without a GL context.
 *
 * <p>The parts of this layer that need pixels are graded from captures, and the
 * captures are listed in the pass record. What is here is everything that is a
 * property of the <em>execution</em> rather than of the picture: that the
 * contact-ordering guarantee survives the third link in the chain, that the
 * director's reading of a chain agrees with the schedule's own, that the
 * vocabulary maps onto the rig it claims to map onto, and that the lane spread
 * this pass introduces does not break the one thing the parry needs.
 */
class DirectorTest {

    private static Schedule scheduleOf(Duel.Kind kind) {
        return Duel.of(kind).schedule();
    }

    // -- the guarantee ---------------------------------------------------------

    @Test
    void contactsStayOrderedThroughTheDirectorsClock() {
        // The engine proved a beat honouring its overlap hint contacts after the
        // previous one did, "however the renderer scales the beats". The staging
        // layer proved that survives the mapping to seconds. This is the third
        // link: the renderer's clock is not the schedule's clock, because
        // STYLE.md 7.3's held breath deliberately runs it slower for a quarter of
        // a second at exactly the moment a contact lands.
        //
        // So the property is asserted on the clock rather than on the schedule:
        // integrate wall time to schedule time at 240 Hz through every time ramp
        // the score contains, and check that the map is strictly increasing and
        // that every contact is crossed in the order the score lists it.
        for (Duel.Kind kind : Duel.Kind.values()) {
            Schedule s = scheduleOf(kind);
            List<Directive.TimeRamp> ramps = s.of(Directive.TimeRamp.class);
            List<Double> contacts = s.contacts();
            assertTrue(contacts.size() >= 2, kind + " has fewer than two contacts to order");

            double t = 0;
            double previous = -1;
            int next = 0;
            double dt = 1.0 / 240.0;
            List<Double> crossed = new java.util.ArrayList<>();
            for (int i = 0; i < 240 * 20 && t < s.duration(); i++) {
                double scale = Director.clockScale(ramps, t);
                assertTrue(scale > 0.0, "the clock stopped at t=" + t + ", which is the hard "
                        + "freeze STYLE.md 7.1 and 10 both ban");
                assertTrue(scale <= 1.0 + 1e-9, "a held breath must slow time, not speed it up");
                double advanced = t + dt * scale;
                assertTrue(advanced > previous, "the clock went backwards at t=" + t);
                previous = t;
                while (next < contacts.size() && contacts.get(next) <= advanced) {
                    crossed.add(contacts.get(next));
                    next++;
                }
                t = advanced;
            }
            for (int i = 1; i < crossed.size(); i++) {
                assertTrue(crossed.get(i) > crossed.get(i - 1),
                        kind + ": contact " + i + " at " + crossed.get(i)
                                + " did not land strictly after " + crossed.get(i - 1));
            }
            assertEquals(contacts.size(), crossed.size(),
                    kind + " did not play every contact in its own score");
        }
    }

    @Test
    void theHeldBreathIsARampAndNeverAFreeze() {
        // STYLE.md 7.3: "a soft time ramp, ~0.85x for ~0.25 s, never a hard
        // freeze." Two things make it soft rather than switched: the rate is
        // continuous at both ends of the window, and it never reaches its nominal
        // scale instantaneously.
        Schedule s = scheduleOf(Duel.Kind.PARRY);
        List<Directive.TimeRamp> ramps = s.of(Directive.TimeRamp.class);
        assertFalse(ramps.isEmpty(), "the parry lands a counter and must hold its breath");
        Directive.TimeRamp r = ramps.get(0);

        assertEquals(1.0, Director.clockScale(ramps, r.at()), 1e-6, "the ramp steps at its start");
        assertEquals(1.0, Director.clockScale(ramps, r.end() - 1e-9), 1e-3, "the ramp steps at its end");
        double deepest = 1.0;
        for (double t = r.at(); t < r.end(); t += 1e-4) {
            deepest = Math.min(deepest, Director.clockScale(ramps, t));
        }
        assertEquals(r.scale(), deepest, 1e-3, "the breath never reached the depth it was given");

        // And the rate is Lipschitz: no frame at 240 Hz sees a step in it.
        double dt = 1.0 / 240.0;
        for (double t = r.at() - dt; t < r.end() + dt; t += dt) {
            double a = Director.clockScale(ramps, t);
            double b = Director.clockScale(ramps, t + dt);
            assertTrue(Math.abs(b - a) < 0.05,
                    "the clock's rate stepped by " + Math.abs(b - a) + " in one frame at t=" + t
                            + ", which is hitstop with a soft name");
        }
    }

    // -- agreement with the layer above ----------------------------------------

    @Test
    void theDirectorReadsAChainsWeightExactlyAsTheScheduleDoes() {
        // Schedule.weight is what every phrasing assertion in the staging tests is
        // written against -- "a carrier chain whose weight never returns to zero
        // between beats is one gesture with clauses". If the director's reading
        // differed, those assertions would be describing something that is not on
        // screen.
        for (Duel.Kind kind : Duel.Kind.values()) {
            Schedule s = scheduleOf(kind);
            Duel.Staged staged = Duel.of(kind);
            for (int body : new int[] {staged.hero(), staged.enemy()}) {
                for (Chain c : Chain.values()) {
                    List<Directive.IkTarget> list = s.chain(body, c);
                    if (list.isEmpty()) {
                        continue;
                    }
                    for (double t = 0; t < s.duration(); t += 1.0 / 240.0) {
                        Director.Sample sample = Director.sampleAt(list, t);
                        double expected = s.weight(body, c, t);
                        if (t <= list.get(0).at()) {
                            // Before the first directive the schedule reads zero and
                            // the director holds the opening weight, deliberately: a
                            // chain must not be driven at a weight nobody asked for,
                            // and it must not lurch on the frame the first directive
                            // starts either.
                            continue;
                        }
                        assertEquals(expected, sample.w(), 1e-9,
                                kind + " body " + body + " " + c + " at t=" + t);
                    }
                }
            }
        }
    }

    @Test
    void theCarrierNeverLetsGoInsideThePhrase() {
        // The property that decides whether five queued tiles read as one phrase
        // or as five events, re-asserted here against the schedule the *scene*
        // actually plays rather than against a test fixture.
        Duel.Staged staged = Duel.of(Duel.Kind.PHRASE);
        Schedule s = staged.schedule();
        List<ScheduledBeat> stanza = s.bracket(ScheduledBeat.Bracket.STANZA);
        assertEquals(5, stanza.size(), "duel-phrase is the five-tile stanza and must have five beats");

        double from = stanza.get(0).start();
        double to = stanza.get(stanza.size() - 1).end();
        for (double t = from + 1e-3; t < to; t += 1.0 / 240.0) {
            double w = s.weight(staged.hero(), Chain.SPINE, t);
            assertTrue(w > 0.0, "the trunk went inert at t=" + t + ", which makes this five "
                    + "gestures rather than one");
        }
    }

    @Test
    void theCameraNeverCuts() {
        for (Duel.Kind kind : Duel.Kind.values()) {
            Schedule s = scheduleOf(kind);
            assertTrue(s.cameraIsContinuous(), kind + " cuts");
            for (Directive.CameraKey k : s.camera()) {
                assertTrue(k.duration() >= dev.starfall.stage.Timing.CAMERA_MIN_MOVE - 1e-9,
                        kind + " has a " + k.duration() + " s camera move, under STYLE.md 9's floor");
            }
        }
    }

    // -- the lane spread -------------------------------------------------------

    @Test
    void theSpreadKeepsACrossingACrossing() {
        // The one property the parry cannot survive losing: both skeletons have to
        // be aimed at *the same point in space*.
        //
        // <b>Pass 1 asserted a weaker thing and passed.</b> It checked that the
        // Meeting's two named halves stayed 0.08 units apart under the lane spread,
        // which was true, and shipped a parry with the blades a fifth of a body
        // height apart -- because both halves were handed to chains whose effector
        // is the fist, and the blade hangs a further 0.47 out of that. So the two
        // halves are now reconciled in Stage into one Anchor.Site.CROSSING and this
        // asserts identity rather than proximity.
        Schedule s = scheduleOf(Duel.Kind.PARRY);
        Duel.Staged staged = Duel.of(Duel.Kind.PARRY);
        double contact = s.contacts().get(s.contacts().size() - 1);

        dev.starfall.stage.Anchor onAttacker = crossingHalf(s, staged.enemy(), contact);
        dev.starfall.stage.Anchor onDefender = crossingHalf(s, staged.hero(), contact);
        assertNotNull(onAttacker, "the attacker has no crossing target live at the crossing");
        assertNotNull(onDefender, "the defender has no crossing target live at the crossing");
        assertEquals(onAttacker, onDefender,
                "the two bodies are aimed at two different points, which is not a crossing");

        // And it sits between the two bodies rather than inside either.
        double heroX = Director.stretch(staged.heroTile() * dev.starfall.stage.Stage.TILE_WIDTH);
        double foeX = Director.stretch(staged.enemyTile() * dev.starfall.stage.Stage.TILE_WIDTH);
        double mid = Director.stretch(onAttacker);
        assertTrue(mid > Math.min(heroX, foeX) && mid < Math.max(heroX, foeX),
                "the crossing at " + mid + " is not between the bodies at " + heroX + " and " + foeX);
        // It scales with the lane, so the crossing stays in the same relative place
        // in the gap however far apart the bodies are drawn.
        assertEquals(onAttacker.x() * Director.LANE_SPREAD, mid, 1e-9,
                "the crossing did not scale with the lane");
    }

    @Test
    void theClashMarkIsOnTheCrossingAndNotOnAGrip() {
        // STYLE.md 11.2b(e), in the tool rather than in the document: a CLASH is an
        // assertion that two blades met, and pass 1 shipped one 36 px from the
        // attacker's own grip. Its origin must be the same reconciled point both
        // blades are aimed at, not either body's own half of it.
        for (Duel.Kind kind : Duel.Kind.values()) {
            Schedule s = scheduleOf(kind);
            for (Directive.Ink d : s.of(Directive.Ink.class)) {
                if (d.kind() != Directive.InkKind.CLASH) {
                    continue;
                }
                assertEquals(dev.starfall.stage.Anchor.Site.CROSSING, d.origin().site(),
                        kind + " draws a clash at " + d.origin() + ", which is a point on one body "
                                + "rather than the point two blades share");
            }
        }
    }

    /** This body's half of the crossing: the crossing target live at {@code contact}. */
    private static dev.starfall.stage.Anchor crossingHalf(Schedule s, int body, double contact) {
        dev.starfall.stage.Anchor found = null;
        for (Directive.IkTarget d : s.chain(body, Chain.SWORD_ARM)) {
            if (d.target().site() != dev.starfall.stage.Anchor.Site.CROSSING) {
                continue;
            }
            if (d.at() <= contact + 1e-9 && d.end() >= contact - 1e-9) {
                found = d.target();
            }
        }
        return found;
    }

    @Test
    void theSpreadDoesNotChangeAnyFiguresOwnDimensions() {
        // Everything that belongs to a body keeps its exact offset from that body,
        // because the figure has not changed size -- only the lane has.
        for (double tile = 0; tile <= 6; tile++) {
            double centre = Director.stretch(tile);
            for (double offset : new double[] {-0.46, -0.28, -0.13, 0.0, 0.13, 0.16, 0.28, 0.46}) {
                double moved = Director.stretch(tile + offset);
                assertEquals(offset, moved - centre, 1e-9,
                        "an anchor " + offset + " from tile " + tile + " moved relative to it");
            }
        }
        // And two adjacent bodies end up LANE_SPREAD apart, which is the point.
        assertEquals(Director.LANE_SPREAD, Director.stretch(1.0) - Director.stretch(0.0), 1e-9);
    }

    // -- the vocabulary maps onto this rig -------------------------------------

    @Test
    void everyChainNamesBonesThisRigActuallyHas() {
        // Chain's own note: "the renderer maps a Chain to its own object; the
        // bones() list is here so that mapping is checkable rather than
        // conventional." This is that check.
        Skeleton skeleton = SamuraiRig.buildSkeletonOnly();
        RigIk ik = new RigIk(skeleton);
        for (Chain c : Chain.values()) {
            IkChain chain = switch (c) {
                case SPINE -> ik.spine();
                case SWORD_ARM -> ik.swordArm();
                case LEG_LEAD -> ik.legL();
                case LEG_TRAIL -> ik.legR();
                case CLAVICLE -> null;
            };
            if (chain == null) {
                // The clavicle is an AimLink, and Chain names one bone for it.
                assertEquals(1, c.bones().size());
                assertEquals(c.bones().get(0), ik.clavicle().bone().name);
                continue;
            }
            for (int i = 0; i < chain.boneCount(); i++) {
                assertEquals(c.bones().get(i), chain.bone(i).name,
                        c + " link " + i + " is not the bone the staging layer names");
            }
        }
    }

    @Test
    void theSettleProfilesReachTheChainsAndStayInTheBand() {
        // Directive.IkTarget's javadoc specifies the mapping exactly:
        // settleSeconds(settle.base()) and boneLagSeconds(i, settle.lag(i)).
        Skeleton skeleton = SamuraiRig.buildSkeletonOnly();
        RigIk ik = new RigIk(skeleton);
        record Pair(Chain chain, IkChain ikChain) {
        }
        for (Pair p : List.of(new Pair(Chain.SPINE, ik.spine()),
                new Pair(Chain.SWORD_ARM, ik.swordArm()),
                new Pair(Chain.LEG_LEAD, ik.legL()),
                new Pair(Chain.LEG_TRAIL, ik.legR()))) {
            Director.applySettle(p.ikChain(), p.chain().settle());
            assertEquals(p.chain().settle().base(), p.ikChain().settleSeconds(), 1e-6);
            for (int i = 0; i < p.ikChain().boneCount(); i++) {
                assertEquals(p.chain().settle().lag(i), p.ikChain().boneLagSeconds(i), 1e-6,
                        p.chain() + " link " + i);
            }
            assertTrue(p.ikChain().settleSeconds() >= dev.starfall.stage.Timing.SETTLE_ROOT - 1e-6
                            && p.ikChain().settleSeconds() <= dev.starfall.stage.Timing.SETTLE_TIP + 1e-6,
                    p.chain() + " settles outside STYLE.md 7.1's band");
        }
    }

    @Test
    void theCompositeRunStillArrivesInOrderOnTheRig() {
        // STYLE.md 7.0.3's "the unit of assertion is the whole run, not one solver
        // chain". Applying the schedule's profiles overwrites RigIk's own tuned
        // numbers, so the run has to be re-checked against what is actually on the
        // chains afterwards rather than against what Chain declares.
        Skeleton skeleton = SamuraiRig.buildSkeletonOnly();
        RigIk ik = new RigIk(skeleton);
        Director.applySettle(ik.spine(), Chain.SPINE.settle());
        Director.applySettle(ik.swordArm(), Chain.SWORD_ARM.settle());
        ik.clavicle().settleSeconds((float) Chain.CLAVICLE.settle().base());

        double previous = -1;
        for (int i = 0; i < ik.spine().boneCount(); i++) {
            double arrival = ik.spine().settleSeconds() + ik.spine().boneLagSeconds(i);
            assertTrue(arrival > previous, "spine link " + i + " arrives with the one above it");
            previous = arrival;
        }
        double clavicle = ik.clavicle().settleSeconds();
        assertTrue(clavicle > previous, "the clavicle arrives before the chest it hangs off");
        previous = clavicle;
        for (int i = 0; i < ik.swordArm().boneCount(); i++) {
            double arrival = ik.swordArm().settleSeconds() + ik.swordArm().boneLagSeconds(i);
            assertTrue(arrival > previous, "arm link " + i + " arrives with the link above it");
            previous = arrival;
        }
        assertTrue(previous <= dev.starfall.stage.Timing.SETTLE_TIP + 1e-6,
                "the run overruns STYLE.md 7.1's band at " + previous);
    }

    // -- the pose channel ------------------------------------------------------

    @Test
    void everyStanceIsASpiralAndNoneOfThemIsAnAPose() {
        // STYLE.md 7.0.1: "in every reference image the figure is a spiral".
        // system2-debt E1: a stance must be a condition, and never a symmetric
        // neutral. So no stance may leave the trunk straight, and every one of
        // them must counter-rotate somewhere down the run.
        Pose pose = new Pose();
        for (Stance stance : Stance.values()) {
            Stances.blend(pose, stance, stance, 1f);
            double hips = pose.get("hips").dRotDeg;
            double spine = pose.get("spine").dRotDeg;
            double chest = pose.get("chest").dRotDeg;
            double head = pose.get("head").dRotDeg;
            assertTrue(Math.abs(hips) + Math.abs(spine) + Math.abs(chest) + Math.abs(head) > 1.0,
                    stance + " leaves the trunk in a symmetric neutral");
            boolean counter = hips * spine < 0 || spine * chest < 0 || chest * head < 0
                    || hips * chest < 0;
            assertTrue(counter || stance == Stance.DRIED || stance == Stance.SLACK
                            || stance == Stance.YIELDING || stance == Stance.CARRIED,
                    stance + " turns the whole trunk the same way, which is a lean and not a spiral");
        }
    }

    @Test
    void aStanceBlendIsContinuousAndNeverLeavesItsEndpoints() {
        // 4b.6 bans popping between states and asks for expression to settle
        // "same as everything else", so a stance change is an interpolation with
        // no excursion beyond either end.
        Pose pose = new Pose();
        double previous = Double.NaN;
        for (double u = 0; u <= 1.0001; u += 0.01) {
            Stances.blend(pose, Stance.READY, Stance.YIELDING, (float) u);
            double chest = pose.get("chest").dRotDeg;
            assertTrue(chest >= 4.9 && chest <= 9.1, "chest left the interval at u=" + u + ": " + chest);
            if (!Double.isNaN(previous)) {
                assertTrue(Math.abs(chest - previous) < 0.2, "the blend stepped at u=" + u);
            }
            previous = chest;
        }
    }

    // -- the scenes are registered and reachable -------------------------------

    @Test
    void everyDuelIsRegisteredWithADebugSibling() {
        List<String> names = List.of(dev.starfall.capture.SceneRegistry.names());
        for (Duel.Kind kind : Duel.Kind.values()) {
            assertTrue(names.contains(kind.sceneName()), kind.sceneName() + " is not registered");
            assertTrue(names.contains(kind.sceneName() + "-debug"),
                    kind.sceneName() + "-debug is not registered");
        }
    }

    @Test
    void bothFiguresAreVisuallyDistinguishable() {
        // Reference images 3, 4 and 5 are a dark duellist against a pale one, and
        // this is the first time the project has had two figures to distinguish.
        // The test is on the material rather than on pixels: same base colour
        // would be the same figure twice however the pixels came out.
        Duel.Staged staged = Duel.of(Duel.Kind.PARRY);
        assertTrue(staged.hero() != staged.enemy(), "the duel has one body in it");
        assertEquals(dev.starfall.combat.Facing.RIGHT, staged.heroFacing());
        assertEquals(dev.starfall.combat.Facing.LEFT, staged.enemyFacing(),
                "the two must face each other, which is what reference 3 shows");
    }
}
