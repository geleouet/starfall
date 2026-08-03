package dev.starfall.direct;

import dev.starfall.analysis.Frame;
import dev.starfall.analysis.Rect;
import dev.starfall.rig.SamuraiRig;
import dev.starfall.stage.Framing;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * STYLE.md 4b.2 as amended by the pass-1 review: <b>the binding value of a face
 * is a ratio</b> — family B paints the face plane at <b>0.25–0.31 of its own
 * local sky luminance</b>, measured on all three family-B images and both
 * duellists. The hexes of 4b.2's table govern only relative structure within
 * the face. Pass 1 obeyed the table and delivered a pale duellist's face at
 * 1.36x its sky: one of the brightest things in the frame, where the corpus
 * paints the face as one of the darkest things on the figure.
 *
 * <h2>The corpus band, measured, regions recorded (STYLE.md 11.3)</h2>
 *
 * <p>Face-plane mean over local-sky mean, Rec.709 luma, on the tracked
 * matched-scale references:
 *
 * <pre>
 *   ref3 dark  x156..170 y196..214 / sky x190..210 y165..185  = 0.248
 *   ref3 pale  x296..310 y200..220 / sky x270..288 y170..190  = 0.313
 *   ref4 dark  x152..166 y192..208 / sky x85..105  y120..140  = 0.289
 *   ref4 pale  x289..305 y182..208 / sky x390..410 y140..160  = 0.304
 *   ref5 dark  x162..180 y228..252 / sky x150..170 y150..170  = 0.309
 *   ref5 pale  x352..368 y230..254 / sky x450..470 y180..200  = 0.303
 * </pre>
 *
 * <p>So the band is <b>[0.248, 0.313]</b>, and {@link #theCorpusFacePlanesSitInTheBand}
 * keeps the corpus's side of it in the suite — §11.0's rule that the whole
 * family passes the criterion before it is allowed to fail anything, on all six
 * heads, not one.
 *
 * <h2>What the delivered assertion covers, exactly</h2>
 *
 * <p>The boxes are derived from the scene's own arithmetic ({@link Rehearsal}
 * plus the schedule's framing), not drawn by eye, so they track the heads on
 * every frame. Two framings are asserted, because the review failed both: the
 * parry's intimate shot and the phrase's planning shot (where pass 1's pale
 * head was "a white blob at 1.20x the sky").
 *
 * <p><b>The clash-light convention, named (§11.3):</b> on the contact frame the
 * clash bloom of §7.3 — licensed light — lifts everything within ~30 px of the
 * crossing, and the foe's face fights at that range: through the bloom the
 * foe's plane box reads 1.374, numerically the same as pass 1's 1.36 decal and
 * mechanically its opposite (glow over dark skin, not pale skin). A contact-
 * frame ceiling therefore cannot discriminate and is not asserted; the band is
 * asserted on the window's bloom-free frames (measured sweep: foe 0.309-0.323
 * on frames 0/16/17/22, against 0.44-1.37 through the clash on frames 4-12 and
 * 19-21), and the red half of the exhibit is the same reader on pass 1's own
 * tracked graded frame, where the foe reads above 1.0 on the SAME bloom-free
 * arithmetic — see {@link #thePassOneDecalFailsThisBand}. An assertion that
 * demanded 0.31 through a licensed light would be failing §7.3 for obeying
 * §7.3.
 */
class FaceValueTest {

    private static final double START = 1.42;
    private static final double STEP = 0.0167;
    private static final int W = 960;
    private static final int H = 720;
    private static final double EYE = 0.44;

    /** The corpus band, from the six readings in the class doc. */
    static final double BAND_LO = 0.248;
    static final double BAND_HI = 0.313;

    private record Boxes(Rect face, Rect sky) {
    }

    /**
     * Face-plane and local-sky boxes for {@code body} at schedule time {@code t},
     * in the pixels of a {@code duel-parry} capture frame. The plane box sits on
     * the cheek/socket plane (between the eye and the jaw, inside the contour);
     * the sky box sits half a head above and forward, which on this staging is
     * always open sky.
     */
    private static Boxes boxesAt(Rehearsal r, int body, double t) {
        Rehearsal.Frame f = r.at(t);
        Rehearsal.Body b = f.body(body);
        double facing = body == r.staged().hero() ? 1 : -1;

        // The head's world orientation, recovered from the head->eye vector
        // against its bind-pose value, so the plane box rides a bowed head
        // instead of sliding off it onto the bloom (which is exactly what a
        // world-constant offset did on the foe: GUARD_RAISED bows the head).
        double bindX = SamuraiRig.HEAD_LOBE_DX + SamuraiRig.EYE_BIND_DX;
        double bindY = SamuraiRig.HEAD_LOBE_DY + SamuraiRig.EYE_BIND_DY;
        double ex = (b.eye().x - b.head().x) * facing;
        double ey = b.eye().y - b.head().y;
        double rot = Math.atan2(ey, ex) - Math.atan2(bindY, bindX);

        // The cheek/socket plane, in the figure's own frame: behind the eye,
        // a shade below it — the same station the corpus boxes sit on.
        double lx = -0.080;
        double ly = -0.020;
        double ofx = lx * Math.cos(rot) - ly * Math.sin(rot);
        double ofy = lx * Math.sin(rot) + ly * Math.cos(rot);
        double planeX = b.eye().x + facing * ofx;
        double planeY = b.eye().y + ofy;
        double skyX = b.head().x + facing * 0.30;
        double skyY = b.head().y + 0.16;

        Framing framing = r.schedule().framingAt(t);
        double worldW = Director.stretchTiles(framing.widthTiles());
        double worldH = worldW * H / (double) W;
        double centreX = Director.stretch(framing.centreTile() * dev.starfall.stage.Stage.TILE_WIDTH);
        double xMin = centreX - worldW / 2;
        double yMin = worldH * EYE - worldH / 2;
        double scale = W / worldW;

        Rect face = rectAround(planeX, planeY, 0.028, 0.036, xMin, yMin, worldH, scale);
        Rect sky = rectAround(skyX, skyY, 0.048, 0.048, xMin, yMin, worldH, scale);
        return new Boxes(face, sky);
    }

    private static Rect rectAround(double wx, double wy, double halfW, double halfH,
                                   double xMin, double yMin, double worldH, double scale) {
        int x0 = (int) Math.round((wx - halfW - xMin) * scale);
        int x1 = (int) Math.round((wx + halfW - xMin) * scale);
        int y0 = (int) Math.round((1 - (wy + halfH - yMin) / worldH) * H);
        int y1 = (int) Math.round((1 - (wy - halfH - yMin) / worldH) * H);
        return Rect.ofCorners(x0, y0, x1, y1);
    }

    private static String px(Rehearsal r, double t, double wx, double wy) {
        Framing framing = r.schedule().framingAt(t);
        double worldW = Director.stretchTiles(framing.widthTiles());
        double worldH = worldW * H / (double) W;
        double centreX = Director.stretch(framing.centreTile() * dev.starfall.stage.Stage.TILE_WIDTH);
        double xMin = centreX - worldW / 2;
        double yMin = worldH * EYE - worldH / 2;
        return "(" + Math.round((wx - xMin) / worldW * W) + ","
                + Math.round((1 - (wy - yMin) / worldH) * H) + ")";
    }

    private static double mean(Frame f, Rect r) {
        double sum = 0;
        int n = 0;
        for (int y = r.y; y <= r.y1(); y++) {
            for (int x = r.x; x <= r.x1(); x++) {
                sum += f.lum(x, y);
                n++;
            }
        }
        return sum / Math.max(1, n);
    }

    /** §11.0: the criterion, run on every head in the family, in the suite. */
    @Test
    void theCorpusFacePlanesSitInTheBand() throws IOException {
        record Head(String name, String file, Rect face, Rect sky) {
        }
        List<Head> heads = List.of(
                new Head("ref3 dark", "ref3-matched-378.png",
                        Rect.ofCorners(156, 196, 170, 214), Rect.ofCorners(190, 165, 210, 185)),
                new Head("ref3 pale", "ref3-matched-378.png",
                        Rect.ofCorners(296, 200, 310, 220), Rect.ofCorners(270, 170, 288, 190)),
                new Head("ref4 dark", "ref4-matched-378.png",
                        Rect.ofCorners(152, 192, 166, 208), Rect.ofCorners(85, 120, 105, 140)),
                new Head("ref4 pale", "ref4-matched-378.png",
                        Rect.ofCorners(289, 182, 305, 208), Rect.ofCorners(390, 140, 410, 160)),
                new Head("ref5 dark", "ref5-matched-378.png",
                        Rect.ofCorners(162, 228, 180, 252), Rect.ofCorners(150, 150, 170, 170)),
                new Head("ref5 pale", "ref5-matched-378.png",
                        Rect.ofCorners(352, 230, 368, 254), Rect.ofCorners(450, 180, 470, 200)));
        for (Head h : heads) {
            Frame f = Frame.load(new File("out/captures/" + h.file()));
            double ratio = mean(f, h.face()) / mean(f, h.sky());
            System.out.println("corpus " + h.name() + ": " + ratio + " through " + h.face().describe());
            assertTrue(ratio >= BAND_LO - 0.01 && ratio <= BAND_HI + 0.01,
                    h.name() + " reads " + ratio + " through " + h.face().describe()
                            + " — outside the 0.25..0.31 band 4b.2 quotes; either the band or the box is wrong");
        }
    }

    /**
     * The delivered face planes sit in the corpus band, at the intimate
     * framing, on bloom-free frames, both duellists — review item 1, on
     * pixels. Boxes are derived from the scene's own arithmetic and the
     * tolerance is ±0.02 against the corpus's [0.248, 0.313] (the derived
     * boxes are not the corpus's hand boxes; the corpus test holds the
     * tighter ±0.01 on its own). Frames 4-12 and 19-21 carry the clash bloom
     * across the foe (licensed, §7.3) and frame 23's foe head drops against
     * the horizon wash — excluded and named per §11.0.
     */
    @Test
    void theDeliveredFacePlanesSitInTheBand() throws IOException {
        Rehearsal r = new Rehearsal(Duel.Kind.PARRY);
        r.play();
        int[] frames = {0, 16, 17, 22};
        for (int fr : frames) {
            Frame f = Frame.load(new File(String.format(
                    "out/captures/s3b-p2-parry-contact/frame_%03d.png", fr)));
            double t = START + fr * STEP;
            for (boolean hero : new boolean[] {true, false}) {
                int body = hero ? r.staged().hero() : r.staged().enemy();
                Boxes b = boxesAt(r, body, t);
                double ratio = mean(f, b.face()) / mean(f, b.sky());
                assertTrue(ratio >= BAND_LO - 0.02 && ratio <= BAND_HI + 0.02,
                        (hero ? "hero" : "foe") + " frame " + fr + ": face/sky " + ratio
                                + " through " + b.face().describe() + " / " + b.sky().describe()
                                + " — outside the corpus's 0.25-0.31 band (4b.2 as amended); "
                                + "pass 1 delivered 0.56 and 1.36 here");
            }
        }
    }

    /**
     * The red half of the exhibit, permanent: the same reader on pass 1's own
     * tracked graded capture fails the band on both duellists — the foe by 4x
     * (the decal the review's §0 named). Uses a bloom-free frame of that
     * window (17), so the licensed clash light cannot be blamed.
     */
    @Test
    void thePassOneDecalFailsThisBand() throws IOException {
        Rehearsal r = new Rehearsal(Duel.Kind.PARRY);
        r.play();
        Frame f = Frame.load(new File("out/captures/s3b-p1-parry-contact/frame_017.png"));
        double t = START + 17 * STEP;
        Boxes foe = boxesAt(r, r.staged().enemy(), t);
        double foeRatio = mean(f, foe.face()) / mean(f, foe.sky());
        assertTrue(foeRatio > BAND_HI + 0.02,
                "pass 1's foe face was supposed to exhibit the decal this band forbids; it reads "
                        + foeRatio + " through " + foe.face().describe());
        Boxes hero = boxesAt(r, r.staged().hero(), t);
        double heroRatio = mean(f, hero.face()) / mean(f, hero.sky());
        assertTrue(heroRatio > BAND_HI + 0.02,
                "pass 1's hero face read 0.56x sky in the review; here " + heroRatio
                        + " through " + hero.face().describe());
    }

    /**
     * STYLE.md 4b.0's field half, the piece review §6.3 failed: at the
     * planning framing the pale duellist's head must not be a white blob. On
     * pass 1's wide capture the head box held nothing darker than the sky
     * (5th percentile 72.4 = 0.99x sky, max 167.5 = 2.28x — the blob). The
     * delivered head keeps a genuine dark-face register at 25 px of head and
     * adds nothing brighter than the bareface control. Boxes are the
     * review's own (§4, planning framing row).
     */
    @Test
    void theWideFramingHeadIsNotAWhiteBlob() throws IOException {
        Frame live = Frame.load(new File("out/captures/s3b-p2-wide/frame_000.png"));
        Frame bare = Frame.load(new File("out/captures/s3b-p2-wide-bareface/frame_000.png"));
        Rect head = Rect.ofCorners(205, 518, 249, 551);
        Rect sky = Rect.ofCorners(300, 500, 339, 539);
        double liveP05 = percentile(live, head, 5);
        double bareP05 = percentile(bare, head, 5);
        double skyMean = mean(live, sky);
        assertTrue(liveP05 <= 0.45 * skyMean,
                "the foe head's darkest register at planning framing is " + liveP05 + " against sky "
                        + skyMean + " through " + head.describe()
                        + " — no dark face register; pass 1's blob read p05 at 0.99x sky");
        assertTrue(liveP05 < bareP05 - 10,
                "the face must darken the wide-framing head against its own bareface control: "
                        + liveP05 + " vs " + bareP05);
        assertTrue(max(live, head) <= max(bare, head) + 2,
                "the face added a brighter pixel (" + max(live, head) + ") than the bareface head's "
                        + max(bare, head) + " at planning framing — 4b.0's field rule");
        assertTrue(mean(live, head) <= mean(bare, head),
                "the skin field lightened the wide-framing head: " + mean(live, head)
                        + " vs bareface " + mean(bare, head));
    }

    private static double percentile(Frame f, Rect r, double p) {
        java.util.ArrayList<Double> v = new java.util.ArrayList<>();
        for (int y = r.y; y <= r.y1(); y++) {
            for (int x = r.x; x <= r.x1(); x++) {
                v.add(f.lum(x, y));
            }
        }
        java.util.Collections.sort(v);
        return v.get((int) Math.min(v.size() - 1, Math.round(p / 100.0 * (v.size() - 1))));
    }

    private static double max(Frame f, Rect r) {
        double m = 0;
        for (int y = r.y; y <= r.y1(); y++) {
            for (int x = r.x; x <= r.x1(); x++) {
                m = Math.max(m, f.lum(x, y));
            }
        }
        return m;
    }

    /**
     * Diagnostic sweep: every frame of the parry window, both duellists,
     * against the delivered capture (which the acceptance tests read, so it
     * is force-added and exists on every clone — a diagnostic that silently
     * returns on a missing file is a skip wearing a print's clothes, and
     * this one did exactly that while pointed at an untracked iteration
     * directory, s3b-p2-try9).
     */
    @Test
    void printTheParryWindowRatios() throws IOException {
        Rehearsal r = new Rehearsal(Duel.Kind.PARRY);
        r.play();
        for (int fr = 0; fr < 24; fr++) {
            File file = new File(String.format("out/captures/s3b-p2-parry-contact/frame_%03d.png", fr));
            if (!file.isFile()) {
                continue;
            }
            Frame f = Frame.load(file);
            double t = START + fr * STEP;
            Boxes hero = boxesAt(r, r.staged().hero(), t);
            Boxes foe = boxesAt(r, r.staged().enemy(), t);
            Rehearsal.Body hb = r.at(t).body(r.staged().hero());
            Rehearsal.Body fb = r.at(t).body(r.staged().enemy());
            System.out.printf("f%02d hero %.3f (%s / %s)  foe %.3f (%s / %s)  heroEye %s foeEye %s heroHead %s foeHead %s%n", fr,
                    mean(f, hero.face()) / mean(f, hero.sky()),
                    hero.face().describe(), hero.sky().describe(),
                    mean(f, foe.face()) / mean(f, foe.sky()),
                    foe.face().describe(), foe.sky().describe(),
                    px(r, t, hb.eye().x, hb.eye().y), px(r, t, fb.eye().x, fb.eye().y),
                    px(r, t, hb.head().x, hb.head().y), px(r, t, fb.head().x, fb.head().y));
        }
    }
}
