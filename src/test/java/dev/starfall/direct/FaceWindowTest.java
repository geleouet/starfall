package dev.starfall.direct;

import dev.starfall.analysis.Facets;
import dev.starfall.analysis.Frame;
import dev.starfall.analysis.Rect;
import dev.starfall.stage.Directive;
import dev.starfall.stage.Framing;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * System 3b's published-pixel guards, on the graded frame of
 * {@code s3b-p2-parry-contact} — force-added, because a guard that depends on
 * an artefact the repository does not publish silently skips on every machine
 * but its author's (STYLE.md 11.2b(f), audit C13).
 *
 * <h2>Pass 2: the criterion is a band, on the whole family, at both bases</h2>
 *
 * <p>The pass-1 guard was refuted on all three counts it is now built from
 * (review §6.1, §5.3): it read ONE corpus head where four of six family-B heads
 * failed its ceiling; it had no floor, so its best possible score belonged to
 * drawing no face at all; and it quoted the 1-px base alone, which
 * {@code FacetsTest}'s own javadoc forbids by name.
 *
 * <p><b>The corpus band, measured through recorded boxes</b> (86x86 head boxes
 * on the tracked matched-scale references, licensed-light-masked, |dL|>8
 * run>=6): base-1 density 6.8..9.6 per 1000 px, base-1 longest max(V,H) 17..28,
 * base-2 density 14.6..23.4. {@link #theCorpusPassesThisGuard} iterates all six
 * heads — §11.0's "show it on every image in the family", and 11.2b(f)'s
 * "enumerate the axis, do not index it".
 *
 * <h2>What the delivered floors are, and where the density comes from</h2>
 *
 * <p>The delivered heads are asserted against BOTH edges: density must exceed
 * the same head's own bareface control by a real margin (the face must add
 * marks — the floor the review's decisive finding demanded) and stay under the
 * corpus ceiling; runs must stay under the lattice's. The floor became
 * reachable when the pass did what this javadoc's earlier revision said it
 * would need: <b>the ink floor was re-derived as a fraction of the ground</b>
 * (STYLE.md 2.2 as amended; the measured family-B fraction, 0.12-0.14 of
 * local sky, lives in {@code Palette.INK_BLACK_DUSK} and the clamp in
 * {@code ink_resolve.frag}), which put a register BELOW the face plane back
 * into the material's reach. The delivered density is coverage-edged marks on
 * grounds that contrast — strokes at L 12-16 on washes at 33-40 and on the
 * lit break, the contour line against sky, the moustache and lip parting on
 * the lit lip — because the resolve's own anti-seam averaging blurs any
 * value-channel step to 2-3 px, so interior wetness structure counts at base
 * 2 only, by design. The remaining distance to the corpus's absolute 6.8-9.6
 * is the un-marked HAIR mass (bareface heads read 3.0-3.9 against corpus
 * heads' 6.8+ before any face is drawn) — the hair pass's item, named in
 * docs/system3b-debt.md.
 *
 * <h2>Scope of the facet instrument, restated at the use site</h2>
 *
 * <p>Two evasion classes are proven and checked in as tests: risers spread
 * over 3 px ({@code FacetsTest.aThreePixelStaircase...}) and boundary jitter of
 * amplitude >= base with period < minRun ({@code
 * FacetsTest.theReviewersJitteredLattice...}, the reviewer's own successful
 * attack). The honest claim is therefore: <em>no near-single-pixel,
 * straight-or-lightly-jittered axis-aligned lattice</em> — with the band's
 * density FLOOR carrying the defence against the jitter class, which lands at
 * 2.6/1000 (base 1) and 7.6 (base 2), below every floor here.
 */
class FaceWindowTest {

    private static final double START = 1.42;
    private static final double STEP = 0.0167;
    private static final int W = 960;
    private static final int H = 720;
    private static final double EYE = 0.44;
    private static final int FRAME = 11;

    static final File CONTACT = new File("out/captures/s3b-p2-parry-contact/frame_011.png");
    static final File BAREFACE = new File("out/captures/s3b-p2-parry-bareface/frame_011.png");

    /** A duellist's head box on the graded frame, from the scene's own arithmetic. */
    static Rect headBox(Rehearsal r, boolean hero) {
        double t = START + FRAME * STEP;
        Rehearsal.Frame f = r.at(t);
        Rehearsal.Body b = f.body(hero ? r.staged().hero() : r.staged().enemy());
        Framing framing = r.schedule().framingAt(t);
        double worldW = Director.stretchTiles(framing.widthTiles());
        double worldH = worldW * H / (double) W;
        double centreX = Director.stretch(framing.centreTile() * dev.starfall.stage.Stage.TILE_WIDTH);
        double yMin = worldH * EYE - worldH / 2;
        double xMin = centreX - worldW / 2;
        double px = (b.head().x - xMin) / worldW * W;
        double py = (1 - (b.head().y - yMin) / worldH) * H;
        int half = (int) Math.round(0.15 * 1.3 / worldW * W);
        return new Rect((int) Math.round(px) - half, (int) Math.round(py) - half, 2 * half, 2 * half);
    }

    /**
     * Both duellists' head regions, delivered vs bareface, both bases, both
     * edges. The axis (which duellist) is enumerated, not indexed — the pass-1
     * guard read the foe alone.
     */
    @Test
    void theHeadRegionsAreNoLongerTheLattice() throws Exception {
        Rehearsal r = new Rehearsal(Duel.Kind.PARRY);
        r.play();
        Frame live = Frame.load(CONTACT);
        Frame bare = Frame.load(BAREFACE);
        for (boolean hero : new boolean[] {true, false}) {
            String who = hero ? "hero" : "foe";
            Rect box = headBox(r, hero);
            Facets.Reading b1 = Facets.measure(live, box, 8, 6, 1, true);
            Facets.Reading b2 = Facets.measure(live, box, 8, 6, 2, true);
            Facets.Reading c1 = Facets.measure(bare, box, 8, 6, 1, true);
            Facets.Reading c2 = Facets.measure(bare, box, 8, 6, 2, true);
            System.out.println("faceWindow " + who + ": base1 " + b1.per1000() + "/1000 longest "
                    + Math.max(b1.vLongest(), b1.hLongest()) + ", base2 " + b2.per1000()
                    + "/1000, bareface " + c1.per1000() + "/" + c2.per1000()
                    + " through " + b1.rect().describe());

            // The run ceiling: the lattice's signature. Corpus 17..28, audit
            // lattice 44..62. Ceiling-only by design — the density floor below
            // is what stops "no face" from being the best score, and a run
            // floor would not (the bareface control clears 16 on its own).
            assertTrue(Math.max(b1.vLongest(), b1.hLongest()) <= 30,
                    who + ": a straight axis-aligned run of " + Math.max(b1.vLongest(), b1.hLongest())
                            + " px at base 1 in " + b1.rect().describe()
                            + " — the audit's lattice ran 44-62, the corpus 17-28");

            // Density, both edges, both bases. Floor: the face must add real
            // mark density over its own absent-subject control (11.2b(g)).
            // Ceiling: the corpus's own top plus its measurement spread.
            assertTrue(b1.per1000() >= c1.per1000() + 1.0,
                    who + ": base-1 density " + b1.per1000() + " vs bareface " + c1.per1000()
                            + " — a face that adds no marks is the absent face the review's"
                            + " 'best score belongs to drawing nothing' finding forbids");
            assertTrue(b1.per1000() <= 10.6,
                    who + ": base-1 density " + b1.per1000() + " above the corpus band top 9.6+1.0");
            assertTrue(b2.per1000() >= c2.per1000() + 1.0,
                    who + ": base-2 density " + b2.per1000() + " vs bareface " + c2.per1000()
                            + " — FacetsTest's javadoc forbids quoting base 1 alone, and so does this guard");
            assertTrue(b2.per1000() <= 26.0,
                    who + ": base-2 density " + b2.per1000() + " above the corpus band top 23.4+2.6");
        }
    }

    /**
     * §11.0's other half, as the review prescribed it: the criterion shown to
     * pass on <b>every</b> head in the family, in the suite. The pass-1 version
     * read image 3's dark duellist alone and excluded images 4 and 5 with a
     * reason the review refuted in one command (blade-masking moves their
     * readings by at most 0.27); it also never measured ref3's own pale
     * duellist, which failed its ceiling. All six now run, through the recorded
     * boxes, with the extended licensed-light mask (see {@link Facets}).
     */
    @Test
    void theCorpusPassesThisGuard() throws Exception {
        record Head(String name, String file, Rect box) {
        }
        List<Head> heads = List.of(
                new Head("ref3 dark", "ref3-matched-378.png", new Rect(120, 150, 86, 86)),
                new Head("ref3 pale", "ref3-matched-378.png", new Rect(270, 157, 86, 86)),
                new Head("ref4 dark", "ref4-matched-378.png", new Rect(117, 157, 86, 86)),
                new Head("ref4 pale", "ref4-matched-378.png", new Rect(267, 152, 86, 86)),
                new Head("ref5 dark", "ref5-matched-378.png", new Rect(127, 192, 86, 86)),
                new Head("ref5 pale", "ref5-matched-378.png", new Rect(332, 192, 86, 86)));
        for (Head h : heads) {
            Frame f = Frame.load(new File("out/captures/" + h.file()));
            Facets.Reading b1 = Facets.measure(f, h.box(), 8, 6, 1, true);
            Facets.Reading b2 = Facets.measure(f, h.box(), 8, 6, 2, true);
            System.out.println("corpus " + h.name() + ": base1 " + b1.per1000() + " longest "
                    + Math.max(b1.vLongest(), b1.hLongest()) + ", base2 " + b2.per1000()
                    + " through " + b1.rect().describe());
            assertTrue(b1.per1000() >= 6.3 && b1.per1000() <= 10.1,
                    h.name() + " reads " + b1.per1000() + "/1000 at base 1 through " + b1.rect().describe()
                            + " — outside the family's own 6.8..9.6 spread; box or band is wrong");
            assertTrue(Math.max(b1.vLongest(), b1.hLongest()) <= 30,
                    h.name() + " longest run " + Math.max(b1.vLongest(), b1.hLongest()));
            assertTrue(b2.per1000() >= 13.5 && b2.per1000() <= 24.5,
                    h.name() + " reads " + b2.per1000() + "/1000 at base 2 — outside 14.6..23.4");
        }
    }

    /**
     * The red run, checked in: the same reader on the inherited picture fails
     * the run ceiling and the base-1 ceiling. Unmasked and at base 1, exactly
     * as the audit measured it, so the mask extension cannot soften it.
     */
    @Test
    void theInheritedLatticeWouldFailThisGuard() throws Exception {
        Frame frame = Frame.load(new File("out/captures/s4-p4-parry-contact/frame_011.png"));
        Facets.Reading r = Facets.measure(frame, Rect.ofCorners(610, 360, 690, 420), 8, 6, 1, false);
        assertTrue(r.vLongest() > 30 && r.per1000() > 6.0,
                "the inherited frame was supposed to exhibit the lattice this guard forbids; "
                        + "it reads " + r.per1000() + "/1000, longest " + r.vLongest());
    }

    /**
     * STYLE.md 4b.0: detail resolves on push-in and only there. Unchanged from
     * pass 1 (the fade curve held); the FIELD half of 4b.0 — which pass 1 left
     * unaddressed and the review failed as "the pale head is a white blob at
     * 1.20x sky" — is guarded on pixels in {@code FaceValueTest}.
     */
    @Test
    void theFaceDetailResolvesOnPushInAndOnlyThere() {
        Rehearsal parry = new Rehearsal(Duel.Kind.PARRY);
        Framing intimate = parry.schedule().framingAt(START + FRAME * STEP);
        float intimateHead = headPx(intimate);
        assertEquals(1f, DuelScene.detailFade(intimateHead), 1e-4,
                "the graded parry framing delivers a " + intimateHead
                        + " px head; the face must be fully resolved there");

        Rehearsal phrase = new Rehearsal(Duel.Kind.PHRASE);
        Framing wide = phrase.schedule().framingAt(0.0);
        float wideHead = headPx(wide);
        assertTrue(DuelScene.detailFade(wideHead) < 0.20f,
                "the planning framing delivers a " + wideHead + " px head and the face's ink is "
                        + "still at " + DuelScene.detailFade(wideHead)
                        + " coverage; 4b.0 wants a suggestion, not a face");

        for (float px = 10f; px < 80f; px += 0.5f) {
            float step = Math.abs(DuelScene.detailFade(px + 0.5f) - DuelScene.detailFade(px));
            assertTrue(step < 0.06f, "the fade steps " + step + " at " + px
                    + " px — a pop, on a channel that inherits the no-cut rule");
        }
    }

    private static float headPx(Framing f) {
        double worldW = Director.stretchTiles(f.widthTiles());
        return (float) (2f * dev.starfall.rig.SamuraiRig.SKULL_RADIUS * W / worldW);
    }

    /**
     * The gaze guard, rebuilt TWICE. The pass-1 version was vacuous (review
     * §6.2: {@code gazeX() > 0.25f} cannot distinguish "the schedule's anchor
     * drives the eye" from "the eye stares forward"). The first pass-2
     * rebuild claimed a red observation this pass could not reproduce: under
     * the review's exact sabotage ({@code Figure.gazeAt} replaced by
     * {@code face.gazeToward(1f, 0f)}) it stayed GREEN, measured on this
     * worktree — because it sampled only the END of the score, where the
     * horizontal expectation saturates at clamp() = 1.0 (indistinguishable
     * from the constant stare) and the final anchor's vertical component
     * happens to sit inside the 0.08 tolerance. A guard that samples one
     * state can be satisfied by a constant equal to that state.
     *
     * <p>So this version probes MID-SCHEDULE: for every gaze-carrying
     * {@code PoseChange} it replays the score to anchor-time + 0.8 s (six
     * gaze taus: settled, STYLE.md 7) and compares both channels of the
     * delivered {@code FaceRig} against expectations computed from the
     * schedule's own anchor data — never from {@code Figure.gazeAt}, whose
     * behaviour is under test (11.2b(c)). And it asserts its OWN
     * discriminating power: across the probes, at least one expectation must
     * differ from the forward stare by more than twice the tolerance, so if
     * the schedule ever degenerates to all-saturated anchors this guard says
     * so instead of silently certifying anything.
     *
     * <p><b>Observed red against the sabotage above, this version:</b>
     * <i>"body 0 at t=0.8: gazeX 1.0 vs the schedule's -0.3071025013923645"</i>
     * — mid-schedule even the horizontal channel discriminates, because the
     * early anchors sit close enough that the mapping is unsaturated. The
     * run is quoted in docs/system3b-debt.md; restored and re-run green.
     */
    @Test
    void bothDuellistsLookAtEachOtherThroughTheContact() {
        Rehearsal probe = new Rehearsal(Duel.Kind.PARRY);
        Duel.Staged staged = probe.staged();
        double duration = probe.schedule().duration();
        // Probe instants: settled under each gaze-carrying anchor.
        java.util.TreeSet<Double> times = new java.util.TreeSet<>();
        for (Directive.PoseChange p : probe.schedule().of(Directive.PoseChange.class)) {
            if (p.gaze() == null) {
                continue;
            }
            double t = Math.min(p.at() + 0.80, duration);
            times.add(Math.round(t * 60.0) / 60.0);
        }
        assertTrue(!times.isEmpty(), "no gaze-carrying pose changes in the schedule");

        double maxOffAxis = 0;
        int settled = 0;
        for (double t : times) {
            Rehearsal r = new Rehearsal(Duel.Kind.PARRY);
            r.play(t, 60.0);
            Rehearsal.Frame frame = r.frames().get(r.frames().size() - 1);
            for (Figure fig : r.director().figures()) {
                int body = fig.body();
                Directive.PoseChange active = null;
                for (Directive.PoseChange p : r.schedule().of(Directive.PoseChange.class)) {
                    if (p.body() == body && p.at() <= t + 1e-6) {
                        active = p;
                    }
                }
                if (active == null || active.gaze() == null || t - active.at() < 0.66) {
                    continue;
                }
                settled++;
                Rehearsal.Body me = frame.body(body);
                double facing = body == staged.hero() ? 1 : -1;
                double dirX = clamp((Director.stretch(active.gaze()) - me.head().x) / 0.9 * facing);
                double dirY = clamp((active.gaze().y() - me.head().y) / 0.9);
                maxOffAxis = Math.max(maxOffAxis,
                        Math.max(Math.abs(dirX - 1.0), Math.abs(dirY)));
                dev.starfall.rig.FaceRig face = fig.face();
                assertEquals(dirX, face.gazeX(), 0.08,
                        "body " + body + " at t=" + t + ": gazeX " + face.gazeX()
                                + " vs the schedule's " + dirX
                                + " — the anchor no longer drives the eye");
                assertEquals(dirY, face.gazeY(), 0.08,
                        "body " + body + " at t=" + t + ": gazeY " + face.gazeY()
                                + " vs the schedule's " + dirY
                                + " — the anchor no longer drives the eye");
                // The anchor is ABOUT the opponent (4b.6: "who this character
                // is about to act on"): within a tile of the other head.
                Rehearsal.Body other = frame.body(body == staged.hero() ? staged.enemy() : staged.hero());
                assertTrue(Math.abs(Director.stretch(active.gaze()) - other.head().x) < 1.3,
                        "body " + body + "'s gaze anchor x=" + Director.stretch(active.gaze())
                                + " is nowhere near the opponent's head at " + other.head().x);
            }
        }
        assertTrue(settled >= 2, "only " + settled + " settled gaze samples across the score");
        assertTrue(maxOffAxis > 0.16,
                "every sampled expectation sits within " + maxOffAxis
                        + " of the constant forward stare — this guard has no discriminating"
                        + " power against review 6.2's sabotage and must not count as a guard");
    }

    private static Figure figOf(Rehearsal r, int body) {
        for (Figure f : r.director().figures()) {
            if (f.body() == body) {
                return f;
            }
        }
        throw new AssertionError("no figure " + body);
    }

    private static double clamp(double v) {
        return Math.max(-1, Math.min(1, v));
    }

    /**
     * Review §6.2's second half: the gaze must be <b>visible</b>, not merely
     * plumbed. Pass 1 wrote {@code 0.009f * gazeX}, which is under 2 px of eye
     * travel at the closest shot the game delivers; the review's verdict was
     * "not observable in the capture". The write scale is now 0.013, and this
     * holds the floor in delivered pixels-per-gaze-swing at the parry's own
     * framing: a full left-to-right gaze moves the eye by more than 2 px, and
     * under 8 (4b's "an expression bigger than three pixels is a cartoon"
     * bounds a HALF swing).
     */
    @Test
    void theGazeMovesTheEyeByMoreThanTwoPixels() {
        Rehearsal parry = new Rehearsal(Duel.Kind.PARRY);
        Framing intimate = parry.schedule().framingAt(START + FRAME * STEP);
        double worldW = Director.stretchTiles(intimate.widthTiles());
        double pxPerWorld = W / worldW;

        dev.starfall.rig.FaceRig left = new dev.starfall.rig.FaceRig();
        left.gazeToward(-1f, 0f);
        left.snap();
        dev.starfall.rig.FaceRig right = new dev.starfall.rig.FaceRig();
        right.gazeToward(1f, 0f);
        right.snap();
        dev.starfall.anim.Pose pl = new dev.starfall.anim.Pose();
        dev.starfall.anim.Pose pr = new dev.starfall.anim.Pose();
        left.write(pl);
        right.write(pr);
        double swingPx = (pr.get("eye").dx - pl.get("eye").dx) * pxPerWorld;
        assertTrue(swingPx > 2.0,
                "a full gaze swing moves the eye " + swingPx + " px at the parry framing — "
                        + "the review measured pass 1's 2 px as invisible");
        assertTrue(swingPx < 8.0, "a " + swingPx + " px eye swing is a cartoon");
    }

    /** 4b.4 read at this scale: no lid, no light — a closed eye must kill the specular. */
    @Test
    void theSpecularDiesWithTheLidAndWithTheDistance() {
        assertEquals(0f, DuelScene.specularAlpha(1f, 0.06f), 1e-6, "closed lid, no speck");
        assertEquals(0f, DuelScene.specularAlpha(0f, 1.15f), 1e-6, "planning framing, no speck");
        assertTrue(DuelScene.specularAlpha(1f, 1f) > 0.4f, "open eye at push-in carries the speck");
    }

    /**
     * The eye, inverted the way the review prescribed: <b>socket first,
     * specular last</b>. Pass 1 shipped "a white dot with no dark iris behind
     * it" — a specular on a lit field. On delivered pixels, the eye
     * neighbourhood must now be no brighter than the face plane on average
     * (the socket is a shadow), while still carrying the one bright point
     * (the specular), so the two surviving pixels of 4b.4's degradation are
     * dark-anchor-plus-light in that order.
     */
    @Test
    void theEyeIsASocketBeforeItIsASpecular() throws IOException {
        Rehearsal r = new Rehearsal(Duel.Kind.PARRY);
        r.play();
        Frame live = Frame.load(CONTACT);
        double t = START + FRAME * STEP;
        for (boolean hero : new boolean[] {true, false}) {
            Rehearsal.Body b = r.at(t).body(hero ? r.staged().hero() : r.staged().enemy());
            Framing framing = r.schedule().framingAt(t);
            double worldW = Director.stretchTiles(framing.widthTiles());
            double worldH = worldW * H / (double) W;
            double centreX = Director.stretch(framing.centreTile() * dev.starfall.stage.Stage.TILE_WIDTH);
            double yMin = worldH * EYE - worldH / 2;
            double xMin = centreX - worldW / 2;
            int ex = (int) Math.round((b.eye().x - xMin) / worldW * W);
            int ey = (int) Math.round((1 - (b.eye().y - yMin) / worldH) * H);
            Rect eyeBox = new Rect(ex - 5, ey - 5, 11, 11);
            // The cheek behind the eye — which is toward the skull, so the
            // offset follows the facing. The pass-1 version used ex-14 for
            // both duellists, which on the left-facing foe put the "plane"
            // in front of the face where it read part sky: a ceiling of
            // 1.35x sky is no socket assertion at all, and the foe's side
            // of this guard was vacuously green the whole time it was red
            // on the hero.
            Rect plane = hero ? new Rect(ex - 14, ey - 3, 10, 12)
                    : new Rect(ex + 5, ey - 3, 10, 12);
            double eyeMean = mean(live, eyeBox);
            double planeMean = mean(live, plane);
            double eyeMax = max(live, eyeBox);
            String who = hero ? "hero" : "foe";
            System.out.println("eye " + who + ": mean " + eyeMean + " max " + eyeMax
                    + " plane " + planeMean + " through " + eyeBox.describe() + " / " + plane.describe());
            assertTrue(eyeMean <= planeMean * 1.35,
                    who + ": the eye region (" + eyeMean + " through " + eyeBox.describe()
                            + ") is brighter than the face plane (" + planeMean
                            + ") — that is pass 1's white shard, not a socket");
            assertTrue(eyeMax >= planeMean * 1.35,
                    who + ": no specular found — brightest eye pixel " + eyeMax
                            + " against plane " + planeMean);
        }
    }

    /**
     * Review item 7: the head-region run-to-run noise, re-characterised and
     * BOUNDED, because pass 1 recorded it as "±1 LSB / invisible" and the
     * review measured max 78 over 1,539 px — a false reassurance that would
     * have poisoned every future before/after at a head.
     *
     * <p>Re-characterised on this pass's own final pair (same command, same
     * commit, twice): 0 of 24 frames bit-identical, 19,768 px total, per
     * frame up to 2,816 px at max channel delta 99, confined to the two
     * head regions; on the graded frame, 1,900 px at max 33. The un-faced
     * control pair is 21/24 identical, so the cost is the face's (plus the
     * pre-existing fleck class). Not bisected — the suspects are still the
     * two quarter-res blur/resolve passes per figure — but no longer
     * mischaracterised, and bounded here on the tracked pair so a future
     * pass reading a one-frame head delta bigger than this KNOWS it is
     * signal. The acceptance metrics themselves must agree across the pair
     * by more than the guards' own margins, which is what makes
     * {@link #theHeadRegionsAreNoLongerTheLattice} a property of the code
     * rather than of one lucky roll: this pass's first delivery read 5.00
     * against its twin's 4.60 — a criterion sitting inside its apparatus's
     * noise — and was reworked until both rolls clear the floor.
     */
    @Test
    void theHeadNoiseIsBoundedAcrossReruns() throws IOException {
        Frame a = Frame.load(CONTACT);
        Frame b = Frame.load(new File("out/captures/s3b-p2-parry-repro/frame_011.png"));
        int differing = 0;
        double maxDelta = 0;
        for (int y = 0; y < 720; y++) {
            for (int x = 0; x < 960; x++) {
                double d = Math.abs(a.lum(x, y) - b.lum(x, y));
                if (d > 0) {
                    differing++;
                    maxDelta = Math.max(maxDelta, d);
                }
            }
        }
        assertTrue(differing <= 6000,
                differing + " px differ between the graded frame and its same-command twin — "
                        + "three times the characterised class; a real change is hiding in the noise record");
        assertTrue(maxDelta <= 120, "max luma delta " + maxDelta + " across the rerun pair");

        Rehearsal r = new Rehearsal(Duel.Kind.PARRY);
        r.play();
        for (boolean hero : new boolean[] {true, false}) {
            Rect box = headBox(r, hero);
            Facets.Reading a1 = Facets.measure(a, box, 8, 6, 1, true);
            Facets.Reading b1 = Facets.measure(b, box, 8, 6, 1, true);
            Facets.Reading a2 = Facets.measure(a, box, 8, 6, 2, true);
            Facets.Reading b2 = Facets.measure(b, box, 8, 6, 2, true);
            String who = hero ? "hero" : "foe";
            assertTrue(Math.abs(a1.per1000() - b1.per1000()) <= 0.55,
                    who + ": base-1 facet density differs by "
                            + Math.abs(a1.per1000() - b1.per1000())
                            + " across a same-command pair — the acceptance is noise");
            assertTrue(Math.abs(a2.per1000() - b2.per1000()) <= 1.5,
                    who + ": base-2 facet density differs by "
                            + Math.abs(a2.per1000() - b2.per1000()) + " across a same-command pair");
        }
    }

    private static double mean(Frame f, Rect r) {
        Rect c = r.clamp(f);
        double s = 0;
        int n = 0;
        for (int y = c.y; y <= c.y1(); y++) {
            for (int x = c.x; x <= c.x1(); x++) {
                s += f.lum(x, y);
                n++;
            }
        }
        return s / Math.max(1, n);
    }

    private static double max(Frame f, Rect r) {
        Rect c = r.clamp(f);
        double m = 0;
        for (int y = c.y; y <= c.y1(); y++) {
            for (int x = c.x; x <= c.x1(); x++) {
                m = Math.max(m, f.lum(x, y));
            }
        }
        return m;
    }
}
