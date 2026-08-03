package dev.starfall.ui;

import com.badlogic.gdx.graphics.Color;
import dev.starfall.art.Palette;

/**
 * The interface, composed: the Fold of the World drawn as ground, and the Ink
 * Stanza written up the margin of the same sheet.
 *
 * <h2>The problem this class exists to solve</h2>
 *
 * <p>STYLE.md 8 removes every element a tactical game normally reads from --
 * panels, bars, borders, boxes, icons in frames, typeset labels -- and STYLE.md 10
 * fails a pass on sight of any of them. What is left has to be legible enough to
 * choose a five-tile phrase from. Four devices do that work here and each of them
 * is a mark rather than a widget:
 *
 * <ol>
 *   <li><b>The glyph is the gesture.</b> {@link Glyph} draws each tile as the
 *       stroke it produces, so the column can be read without a label and STYLE.md
 *       8's "type is the last resort" costs nothing. There is no text anywhere in
 *       this class.</li>
 *   <li><b>Wetness carries the reading order.</b> Fresh ink is opaque; ink that
 *       has been waiting is thinner and more broken. The top of the column is the
 *       wettest mark, which is the newest, which is the next to resolve -- so
 *       "last in, first out" is a fact about pigment rather than a rule to be
 *       taught.</li>
 *   <li><b>The column drains rather than moving.</b> See {@link Look.Written}: the
 *       column is anchored at its base and grows upward, so nothing ever slides,
 *       and a resolving mark is drunk in place while the ones below it wait.</li>
 *   <li><b>Everything recedes with the push-in.</b> The interface's presence is a
 *       function of {@link Readout#intimacy(double)} and nothing else, so it thins
 *       out as the camera closes on the exchange the way an inscription gives way
 *       to the painting. A constant-opacity layer through a camera move is the
 *       single most reliable signature of a HUD, and this cannot have one.</li>
 * </ol>
 *
 * <h2>Two spaces, and why the column is in the second one</h2>
 *
 * <p>{@link #ground} draws in world units, because the lane <em>is</em> ground:
 * it scales, translates and is occluded exactly as the dirt the figures stand on.
 * {@link #sheet} draws in units of frame height, because a column that scaled with
 * a 5.2x push-in would leave the frame during the move it exists to survive.
 *
 * <p>That makes the column the one thing here that does not live in the world, and
 * two things stop it becoming an overlay. It is drawn <b>before the figures</b>,
 * so a body walking into the margin passes in front of it -- nothing a duellist
 * occludes reads as a HUD. And it carries a bounded <b>parallax</b>: the sheet
 * drifts against the camera at a small fraction of its motion, which is STYLE.md
 * 9's "subtle parallax between fog layers, figures and ground" with the margin
 * as the furthest layer of all.
 */
public final class LaneInterface {

    // -- the sheet, in units of frame height -----------------------------------

    /** The margin the stanza is written down, in frame heights from the left edge. */
    public static final float COLUMN_X = 0.135f;

    /**
     * And the margin the hand waits in, measured from the <em>right</em> edge.
     *
     * <p>Two margins rather than one column, and the reason is composition rather
     * than room. The written stanza and the unwritten hand are different kinds of
     * statement -- one is a commitment, the other an inventory -- and stacking them
     * on one axis makes the reader scan past five marks to reach the five that say
     * what is still possible. Flanking the picture instead leaves the middle third
     * clear, which is where combat-design.md 3 requires the camera's horizontal
     * glide to happen and where the exchange itself lands. Neither column can be
     * confused with the lane, because both are orthogonal to it.
     *
     * <p><b>Moved outward from 0.135 after the pass-2 review read this column as the
     * enemy's.</b> Cold, off the delivered pixels, the reviewer wrote of the hand's
     * loudest mark: <i>"a strike, aimed leftward. This is the enemy's intent"</i>,
     * and named three causes -- the column is inset from the edge, it stands among
     * the Charted Shadows, and its charge run lies where an enemy health row would.
     * Four things answer those and this constant is only one: the charge run is now
     * drawn <b>under its own tile</b> rather than reaching from it toward the
     * picture ({@link #held}), both margins carry the same seal at their head
     * ({@link #seal}), and {@code Stage.planning(Standing)} no longer leaves the
     * bodies against the right edge.
     *
     * <p>The number itself is set by the run rather than by taste: a centred run at
     * the largest cooldown the engine can carry is {@code 8 * TICK_PITCH} wide, and
     * the column has to sit far enough in that the run's outer end stays on the
     * sheet at <em>both</em> shipped heights. 0.125 is the smallest inset that does,
     * with about six pixels to spare at 720 rows and five at 540 -- and the run's
     * inner end now stops at 0.84 of the frame's width, where pass 2's five-charge
     * run reached 0.76 and touched the picture.
     */
    public static final float HAND_INSET = 0.125f;

    /** The foot of the column. Marks are written upward from here and never move. */
    public static final float STANZA_BASE_Y = 0.490f;

    /** One line of the stanza. */
    public static final float STANZA_PITCH = 0.0970f;

    /** The side of a cartouche's unit square. */
    public static final float STANZA_GLYPH = 0.098f;

    /** The top of the hand. */
    public static final float HAND_TOP_Y = 0.920f;

    /**
     * One line of the hand.
     *
     * <p>Widened with {@link #HAND_GLYPH} because the hand now holds seven tiles
     * rather than five -- see {@code Bout}, and the 1.016x the pass-1 review measured
     * through the right third of the graded frame.
     */
    public static final float HAND_PITCH = 0.0940f;

    /**
     * The side of a held tile's cartouche.
     *
     * <p>Larger than pass 1's 0.072, for a measured reason rather than a
     * compositional one: a glyph in the hand was drawn at 72% of the side the same
     * glyph gets in the stanza, so every stroke in it was 28% narrower in pixels and
     * therefore 28% steeper at its rim -- and delivered, the steepest single-pixel
     * step anywhere in the interface was in the hand column, not in the stanza. It
     * also gives the right third of the graded frame something to do.
     */
    public static final float HAND_GLYPH = 0.080f;

    /**
     * How wide one charge mark is drawn, as a share of the frame height.
     *
     * <h2>A counted mark has to be countable at every resolution the game ships</h2>
     *
     * <p>combat-design.md 1.2 counts a cooldown in <b>charges</b>, and pass 1 drew
     * them as a discrete run because a discrete quantity should be a discrete
     * picture. It then shipped a run whose pitch was 1.7 tick widths, which at
     * 960x720 delivered <b>three separable marks for a cooldown of four</b>. The
     * pass-1 review reproduced that and named what makes it serious: a Parry at two
     * charges of four printed two ochre marks and one legible ghost, so a player
     * counts <i>"two of three"</i>. That is not a missing tick, it is a <b>wrong
     * number</b>, on the resource that decides whether a tile can be banked at all.
     *
     * <p>The obligation the review left is narrow and is entirely inside this layer:
     * <i>"a mark that is counted must be countable at every resolution the game
     * ships, or it must stop being a count."</i> So the pitch is <b>derived from the
     * width</b> rather than chosen beside it -- {@link #TICK_PITCH} is
     * {@value #TICK_SPACING} tick widths, which leaves more paper between two marks
     * than either mark occupies -- and the run is asserted separable at every entry
     * of {@code Guards.SHIPPED_HEIGHTS}, for every cooldown the engine can produce
     * and every charge count inside it.
     *
     * <p>The second half of the defect was the ghosts. A spent charge was drawn at
     * {@code dryness} 0.48, so the dry-brush breakup ate whole marks and the ones it
     * left were shorter than the gap between them. A ghost is now separated from a
     * live charge by <em>value and hue</em> -- pale paper at 0.34 against ochre at
     * 0.90, a 2.6x difference -- and not by being broken, because a mark the noise
     * can eat cannot be part of a count.
     */
    public static final float TICK_WIDTH = 0.0130f;

    /** Tick widths between two charge marks. */
    public static final float TICK_SPACING = 2.6f;

    public static final float TICK_PITCH = TICK_WIDTH * TICK_SPACING;

    /**
     * How far under its own cartouche a charge run is drawn, in frame heights.
     *
     * <p>In the paper between the tile it counts for and the next one down:
     * {@link #HAND_PITCH} less {@link #HAND_GLYPH} leaves 0.014 of it, and the run
     * lives there. That is the whole of "this count belongs to that mark".
     */
    public static final float TICK_ROW = 0.0470f;

    /** Half the length of one charge mark. Bounded by the paper between two cartouches. */
    public static final float TICK_HALF_LENGTH = 0.0060f;

    /**
     * How loud a live charge is, and how loud its ghost.
     *
     * <h2>Lowered from 0.74, and the reason is where the run now sits</h2>
     *
     * <p>{@code Stage.planning(Standing)} tightened the planning shot from 12.5 tiles
     * to 6.5, which doubled the figures and moved the ground plane <em>up</em> the
     * frame -- so the foot of the hand column, which was over the sky at pass 2's
     * framing, is now over the near ground. Delivered luminance is coverage times the
     * contrast between the pigment and whatever is under it, and ochre at 176 over a
     * ground at 30 is half again the contrast the same mark had over a sky at 60. The
     * mark did not get harder; it moved somewhere darker. Measured, the steepest
     * delivered step on the graded frame went 0.244 to 0.286 with nothing about the
     * run changed.
     *
     * <p>0.62 puts it back at 0.239. The pair keeps its ratio -- 2.48x, against 2.47x
     * before -- because that ratio is what separates a live charge from a spent one
     * and {@code everyCountedMarkIsCountableAtEveryShippedResolution} is asserted on
     * it at both shipped heights.
     */
    public static final float CHARGE_ALPHA = 0.62f;

    public static final float CHARGE_GHOST = 0.32f;

    /** The Night Pilgrim's remaining strokes, at the head of the stanza. */
    public static final float HEALTH_Y = 0.963f;

    public static final float HEALTH_X = 0.055f;

    /** One life stroke, and the same countability rule the charge marks are under. */
    public static final float HEALTH_WIDTH = 0.0125f;

    public static final float HEALTH_PITCH = HEALTH_WIDTH * TICK_SPACING;

    /**
     * How loud a life stroke is.
     *
     * <p><b>Lowered from 0.90 because of what the pass-1 review measured, and the
     * reasoning is compositional rather than numerical.</b> Delivered, the health row
     * read at a peak lift of <b>+99.6</b> over its own ground and the freshest mark
     * in the stanza at <b>+94.8</b>: the loudest thing in the margin was the one
     * element the player is not composing with. Health is a state you <em>check</em>;
     * the stanza is the thing you are <em>building</em>, and it should out-read it.
     * Guarded rather than asserted in prose -- see the interface's own tests.
     */
    public static final float HEALTH_ALPHA = 0.66f;

    /** How much of itself the interface gives up at the intimate framing. */
    public static final float RECESSION = 0.62f;

    /**
     * And how much the mark now resolving gives up, which is much less. The gap
     * between these two numbers is the whole of "the currently-resolving beat is
     * distinguishable from the queued ones" at execution framing.
     */
    public static final float RESOLVING_RECESSION = 0.18f;

    // -- the ground, in world units --------------------------------------------

    /**
     * How far below the ground plane the lane's wash lies, as a fraction of the
     * frame's own height.
     *
     * <h2>Why this is a share of the frame and not a world height</h2>
     *
     * <p>Measured down an empty column of the delivered planning frame, the Family
     * B stage has no room at the ground plane: the horizon band runs at luminance
     * 100 to row 486, collapses to 50 by row 498 and to a flat 30 by row 540. The
     * ground plane sits inside that collapse, on the steepest gradient in the
     * picture and under the mist wisps of STYLE.md 6 as well. A mark there loses
     * either way round -- pale, it is another wisp; dark, it is another shadow.
     *
     * <p>The flat band <em>below</em> the feet is the near ground, it is the
     * largest even field in the frame, and it is where reference image 3 puts its
     * own ground smear. That is where the lane is drawn.
     *
     * <p>It cannot be a fixed world height, because the framing law of STYLE.md 9
     * changes the frame's world height by 3.9x between the planning shot and the
     * intimate one: any constant that clears the horizon when wide is off the
     * bottom of the sheet when close. A share of the frame keeps the wash the same
     * distance under the feet in the picture, which is where a painter would put
     * it and the only place it is legible at both ends of the push-in.
     */
    public static final float GROUND_DROP = 0.024f;

    /**
     * Half-height of a tile's own pool, as a share of the frame.
     *
     * <p>Much flatter than it is wide, because a wash lying on the ground is seen
     * edge-on: a pool as tall as it is broad reads as a disc floating in front of
     * the picture rather than as pigment sitting in the paper.
     */
    public static final float TILE_RY = 0.0095f;

    /** Half-width of a tile's pool, as a fraction of the tile pitch. */
    public static final float TILE_RX = 0.30f;

    /** A tile the written stanza reaches: wider, so a contiguous reach reads as one ground. */
    public static final float REACH_RX = 0.52f;

    public static final float REACH_RY = 0.0215f;

    // -- the Shadows' strokes, in world x and shares of the frame ---------------

    /**
     * One of a Charted Shadow's strokes, as a share of the frame height.
     *
     * <h2>The grammar, and why it cannot be confused with the two things it rhymes
     * with</h2>
     *
     * <p>STYLE.md 8 draws health as "a row of ink strokes... drying and fading as it
     * drops", and warns by name that a second vertical run of similar marks beside
     * the stanza is a confusable pair. So the Shadow's row is the same grammar with
     * three different anchorings at once: it is <b>horizontal</b> where the stanza is
     * vertical; it is <b>pinned to the body it counts</b>, in world x, where the
     * hero's row lives at the head of the margin; and it is smaller than either. It
     * moves with the body and the camera, which no margin mark ever does -- and that
     * motion is the strongest of the three signals, because it is the one a cold
     * reader cannot miss.
     *
     * <p>Sized as a share of the frame height, like {@link #STRIKE_WIDTH}, because
     * the framing law changes the frame's world height 3.9x across the push-in: a
     * count authored in world units would be sub-pixel at the planning shot or a
     * banner at the intimate one. A share of the frame is the same number of pixels
     * at every framing, which is what a <em>count</em> has to be
     * (combat-design.md 3: separable at every resolution the game ships, or not a
     * count at all).
     */
    public static final float SHADOW_WIDTH = 0.0100f;

    /** Same spacing law as the charge run: more paper between marks than mark. */
    public static final float SHADOW_PITCH = SHADOW_WIDTH * TICK_SPACING;

    /** Half the length of one stroke, in frame heights. */
    public static final float SHADOW_HALF_LENGTH = 0.0155f;

    /**
     * How far above the ground plane the row sits, in world units: above the
     * figure's head, in the air the fog bands leave clear. {@code SamuraiRig}'s
     * figure stands 1.70 tall and its topknot and hair ride a little higher.
     */
    public static final float SHADOW_Y = 2.02f;

    /**
     * How loud a living stroke is. Quieter than the hero's {@link #HEALTH_ALPHA}
     * for the same reason the health row is quieter than the stanza: the enemy's
     * state is a thing you check, and it stands inside the picture, where every
     * unit of alpha costs more than it does in the margin.
     */
    public static final float SHADOW_ALPHA = 0.60f;

    /** And a spent one: a dry ghost, same statement as the hero's lost strokes. */
    public static final float SHADOW_GHOST = 0.24f;

    /** Where a Strikethrough is drawn: just under the feet, above the lane's own wash. */
    public static final float STRIKE_DROP = 0.014f;

    /** How thick a Strikethrough is drawn, as a share of the frame. */
    public static final float STRIKE_WIDTH = 0.019f;

    private LaneInterface() {
    }

    /**
     * The Fold of the World, drawn as ground rather than as a board.
     *
     * <p>combat-design.md 3 and STYLE.md 8 both give the same instruction -- "a row
     * of faint wash marks that only intensify near relevant tiles" -- and the
     * operative word is <em>row</em>, not grid. So there is one pool per tile and
     * the pools of adjacent relevant tiles are wide enough to run together, which
     * means a contiguous reach prints as one continuous ground and an isolated tile
     * prints as one mark. Nothing is ever bounded, so nothing can be counted as a
     * cell.
     *
     * @param spread      world units per tile, after the lane has been stretched
     * @param frameHeight how much world the frame holds vertically, which is what
     *                    the wash's own geometry is a share of
     */
    public static void ground(Brush.Sink sink, Look look, double spread, double frameHeight) {
        float fade = 1f - 0.35f * (float) look.intimacy();
        float y = (float) (-GROUND_DROP * frameHeight);
        float ry = (float) (TILE_RY * frameHeight);
        for (int tile = 0; tile < look.laneLength(); tile++) {
            float x = (float) (tile * spread);
            float seed = 7.3f + tile * 2.9f;
            // The baseline: wash pooled on the near ground, one pool per tile.
            // Pale, because this band is the darkest even field in the frame and
            // pigment lifted off it is the only mark that reads there.
            Brush.wash(sink, x, y, (float) (TILE_RX * spread), ry,
                    0.20f * fade, Palette.PAPER_COOL, seed, 0.50f);
        }
        for (int tile : look.reached()) {
            float x = (float) (tile * spread);
            // The written stanza's own footprint, taken from the engine's Focus
            // spans rather than re-derived: the tiles this phrase would happen over.
            // Wider than a tile's own pool, so a contiguous reach runs together into
            // one ground and an isolated tile stays one mark.
            Brush.wash(sink, x, y, (float) (REACH_RX * spread), (float) (REACH_RY * frameHeight),
                    0.44f * fade, Palette.CLOTH_PALE, 31.7f + tile * 5.1f, 0.45f);
        }
        for (int tile : look.threatened()) {
            strikethrough(sink, (float) (tile * spread), (float) (0.48 * spread),
                    (float) (-STRIKE_DROP * frameHeight), (float) (STRIKE_WIDTH * frameHeight),
                    (float) look.bleed(), 0.74f * fade, 41.3f + tile * 8.7f);
        }
    }

    /**
     * One Charted Shadow's strokes, over the body that carries them.
     *
     * <p>Drawn in the same pass as {@link #ground} -- world x, sizes in shares of
     * the frame -- and <b>before the figures</b>, like everything else in the
     * interface, so the marks live behind the ink of the world rather than on a
     * layer above it. The row is centred on the body, each stroke leans the way
     * the hero's health strokes lean, spent strokes stay as dry ghosts, and the
     * whole row recedes with the push-in and dries away with the body's own
     * dissolve.
     *
     * <p>No vermillion: the row states a fact, not a threat, and guard B owns
     * that colour.
     *
     * @param x           the body's world x, from the figure the scene is drawing
     * @param frameHeight how much world the frame holds vertically
     */
    public static void shadow(Brush.Sink sink, Look.Shadow s, float x, float frameHeight,
                              double intimacy) {
        float fade = (1f - RECESSION * (float) intimacy) * (1f - (float) s.dying());
        if (fade <= 0.01f || s.maxHp() <= 0) {
            return;
        }
        float seed = 67.3f + s.body() * 9.1f;
        float pitch = SHADOW_PITCH * frameHeight;
        float first = x - (s.maxHp() - 1) * pitch * 0.5f;
        for (int i = 0; i < s.maxHp(); i++) {
            boolean wet = i < s.hp();
            tick(sink, first + i * pitch, SHADOW_Y,
                    SHADOW_HALF_LENGTH * frameHeight, 0.0030f * frameHeight,
                    SHADOW_WIDTH * frameHeight,
                    (wet ? SHADOW_ALPHA : SHADOW_GHOST) * fade,
                    wet ? Palette.CLOTH_PALE : Palette.PAPER_COOL,
                    seed + i * 2.3f, wet ? 0f : 0.30f);
        }
    }

    /**
     * combat-design.md 3b's Strikethrough: the one place STYLE.md 8 spends
     * vermillion freely.
     *
     * <p>Drawn as its own name -- a stroke through the tile, the way a word is
     * struck out -- and it arrives "by wet bleed rather than by fade": two half
     * strokes wicking outward from a seed at the tile's centre, so the pigment
     * spreads into the paper instead of the whole mark growing an opacity. Nothing
     * about it is a highlight rectangle, and there is at most one per threatened
     * tile, which is what keeps it inside STYLE.md 2.2's "at most a few small marks
     * per frame".
     */
    private static void strikethrough(Brush.Sink sink, float x, float half, float y, float width,
                                      float bleed, float alpha, float seed) {
        if (bleed <= 0.02f) {
            return;
        }
        // <b>The damp the wick runs out of.</b> Pass 1 drew the arrival as two half
        // strokes and nothing else, and early in the bleed that is a mark six pixels
        // long carrying most of a stroke's ink -- the brush's pressure taper, which is
        // in the parameter of the stroke and not in pixels, then lands the whole of
        // its cap inside a single pixel. Measured through the interface's own raster
        // that was the steepest step anything in this layer printed, and it was
        // printed by the one mark STYLE.md 8 says must arrive "by pigment spreading
        // into place". A seed of wash under the strokes is what a drop on wet paper
        // actually is, and it gives the wick a ground to rise off instead of a hard
        // start.
        Brush.wash(sink, x, y, half * (0.22f + 0.55f * bleed), width * 0.95f,
                alpha * 0.55f, Palette.VERMILLION, seed + 0.6f, 0.55f);
        // And the pigment at the front of a bleed is thin. The wick reaches full
        // strength once it has somewhere to be: without this the first frames of the
        // arrival are a stroke two pixels long carrying a full load of ink, and a
        // brush's pressure taper is in the parameter of the stroke rather than in
        // pixels, so the whole of its cap lands inside one pixel. This is not the
        // fade STYLE.md 8 forbids -- the mark's <em>extent</em> is what changes over
        // the whole 0.6 s, and this term is spent inside the first fifth of it.
        float front = smoothstep(0.0f, 0.30f, bleed);
        for (int side = -1; side <= 1; side += 2) {
            int n = 5;
            float[] xs = new float[n];
            float[] ys = new float[n];
            for (int i = 0; i < n; i++) {
                float u = i / (float) (n - 1) * bleed;
                xs[i] = x + side * half * u;
                // Not level: a struck-out word is drawn by a hand, and a hand tilts.
                ys[i] = y + side * width * 0.45f * u;
            }
            Brush.stroke(sink, xs, ys, width * (0.62f + 0.38f * bleed), alpha * front,
                    Palette.VERMILLION, seed + side, 0.06f);
        }
    }

    /**
     * The margin of the sheet: health, the Ink Stanza, and the hand still unwritten.
     *
     * <p>Coordinates are in <b>frame heights</b>, so {@code x} runs 0 to the frame's
     * aspect ratio and {@code y} runs 0 to 1, and a length means the same thing on
     * both axes however the sheet is cut.
     *
     * @param parallax how far the margin has drifted with the camera, in frame heights
     */
    public static void sheet(Brush.Sink sink, Look look, float aspect, float parallax) {
        float x = COLUMN_X + parallax;
        float hx = aspect - HAND_INSET + parallax;
        float fade = 1f - RECESSION * (float) look.intimacy();

        foxing(sink, x, 0.108f, 0.975f, 0.240f, 12.7f, fade);
        foxing(sink, hx, 0.100f, 0.965f, 0.300f, 60.1f, fade * 0.85f);
        runnel(sink, x, 0.975f, 0.240f, 0.108f, 3.1f, fade);
        runnel(sink, hx, 0.965f, 0.520f, 0.086f, 8.9f, fade * 0.72f);
        seal(sink, SEAL_X, SEAL_Y, 5.9f, fade);
        seal(sink, hx, SEAL_Y, 5.9f, fade);
        health(sink, look, fade);

        // Where the nib goes next: the impression left at every line of the column
        // that has nothing written on it. This is what makes an empty stanza a
        // readable state rather than an absence -- the sheet still shows that there
        // is a stanza, how long it can be, and which line the next mark lands on.
        int written = look.stanza().size();
        for (int h = written; h < dev.starfall.combat.InkStanza.CAPACITY; h++) {
            float y = STANZA_BASE_Y + h * STANZA_PITCH;
            Brush.wash(sink, x, y, STANZA_GLYPH * 0.44f, STANZA_GLYPH * 0.13f,
                    (h == written ? 0.26f : 0.11f) * fade, Palette.PAPER_COOL,
                    91.3f + h * 4.7f, 0.7f);
        }

        column(sink, look, parallax);

        for (int i = 0; i < look.hand().size(); i++) {
            held(sink, hx, look.hand().get(i), i, look.heroStep(), fade);
        }
    }

    /**
     * The written cartouches alone, without the sheet around them.
     *
     * <p>Separated so that a test can measure what one mark draws rather than what
     * the margin draws -- the reading order of the column is a claim about where
     * marks land, and it should be checkable against the geometry the renderer was
     * handed rather than against the model that produced it.
     */
    public static void column(Brush.Sink sink, Look look, float parallax) {
        float fade = 1f - RECESSION * (float) look.intimacy();
        for (Look.Written w : look.stanza()) {
            cartouche(sink, COLUMN_X + parallax, look, w, fade);
        }
    }

    /**
     * The wash the column is written down: one long, very pale, broken vertical
     * stroke.
     *
     * <p>It does two jobs and neither of them is decoration. It gives the marks a
     * ground of their own so they are not floating in the sky, which is what a
     * margin does on a scroll. And its pigment runs downward, which is the
     * direction the column is read and resolved in -- the arrow that is not an
     * arrow.
     */
    private static void runnel(Brush.Sink sink, float x, float from, float to, float width,
                               float seed, float fade) {
        int n = 26;
        float[] xs = new float[n];
        float[] ys = new float[n];
        for (int i = 0; i < n; i++) {
            float u = i / (float) (n - 1);
            ys[i] = from - u * (from - to);
            xs[i] = x + (Brush.hash(seed, i, 13) - 0.5f) * 0.014f;
        }
        Brush.stroke(sink, xs, ys, width, 0.115f * fade, Palette.PAPER_COOL, seed, 0.32f);
    }

    /**
     * The sheet the margin is written on: foxing.
     *
     * <h2>The substrate, and the contradiction it half is</h2>
     *
     * <p>combat-design.md 3's last bullet requires that <i>"everything sits on the
     * aged-paper substrate of STYLE.md 3b.3, so the interface is marks on the same
     * sheet the figures are painted on rather than a layer above it"</i>, and the
     * pass-1 review found no paper: high-pass standard deviation <b>0.813</b> in the
     * planning sky against reference image 1's paper at <b>12.853</b>.
     *
     * <p>Half of that gap is a comparison across families and this pass measured it
     * again to say so: image 1 is a Family A cream sheet at luminance ~230, and the
     * same statistic on <b>reference image 3's own sky -- the Family B stage this
     * scene is quoting -- is 1.144</b>, which the delivered frame is within a factor
     * of 1.4 of. A dusk sky is not a sheet of paper and cannot be given 3b.3's tooth
     * without becoming one. That correction is filed in {@code docs/system5-debt.md}
     * rather than quietly resolved.
     *
     * <p>The other half is real, and it is this. The <em>margin</em> is where the
     * interface claims to be marks on a sheet, and until now the sheet under them was
     * one pale runnel; delivered, the review saw the right-hand marks read as
     * <i>"pale scratches floating in the sky beside the bokeh motes"</i>. So the two
     * margins carry 3b.3's aged paper in the one form a wash can carry it -- foxing:
     * small irregular stains, some cool and some ochre, at a scale a long way above
     * the pixel grid so nothing here can shimmer (3b.1). It is surface rather than
     * value: the marks it puts on the sheet are individually fainter than the
     * faintest empty-slot impression, and what it changes is the <em>tooth</em>,
     * which is the quantity the review measured.
     */
    private static void foxing(Brush.Sink sink, float x, float halfWidth, float from, float to,
                              float seed, float fade) {
        int marks = 34;
        for (int i = 0; i < marks; i++) {
            float u = Brush.hash(seed, i, 2);
            float v = Brush.hash(seed, i, 5);
            float s = Brush.hash(seed, i, 9);
            float cx = x + (v - 0.5f) * 2f * halfWidth;
            float cy = to + (from - to) * u;
            float r = 0.0055f + 0.0125f * s;
            boolean warm = Brush.hash(seed, i, 17) > 0.62f;
            Brush.wash(sink, cx, cy, r * (0.8f + 0.7f * v), r * (0.55f + 0.5f * s),
                    (warm ? 0.075f : 0.055f) * fade,
                    warm ? Palette.OCHRE : Palette.PAPER_COOL, seed + i * 1.7f, 0.85f);
        }
    }

    /** Where the author's chop sits: at the head of each margin, on both sides. */
    public static final float SEAL_X = 0.030f;

    public static final float SEAL_Y = 0.963f;

    /**
     * The chop at the head of a margin: <b>whose sheet this is</b>.
     *
     * <h2>What it is answering</h2>
     *
     * <p>The pass-2 review read the right-hand column cold as <i>"the enemy's
     * intent"</i>, and was confident. That is a composition finding rather than a
     * glyph one -- nothing in a mark says who owns it, and a column of marks
     * standing beside two Charted Shadows is theirs by position. The device a
     * hanging scroll uses for exactly this question is the seal: one chop, in one
     * pigment, at the head of the writing. Here there are two margins, so the
     * <em>same</em> chop is drawn at the head of both, at the same height and in the
     * same ink -- one sheet, one hand, both columns the player's.
     *
     * <p>Ochre and not vermillion, deliberately. STYLE.md 2.1 lists seals under
     * vermillion's own uses, but STYLE.md 8 spends vermillion on enemy intent
     * telegraphs and the interface's guard asserts that every vermillion vertex
     * lies over a threatened tile. A seal in the margin would either break that
     * guard or dilute the one colour in the game that means danger; ochre is the
     * palette's other warm note, is already the hand's own pigment for a live
     * charge, and reads as a stamp rather than as a threat.
     *
     * <p>Two overlapping washes with torn rims rather than one, so the mark is a
     * pressed blot and not a disc -- STYLE.md 10 fails a pass on sight of anything
     * that reads as a shape primitive.
     */
    private static void seal(Brush.Sink sink, float x, float y, float seed, float fade) {
        Brush.wash(sink, x, y, 0.0155f, 0.0170f, 0.46f * fade, Palette.OCHRE, seed, 0.80f);
        Brush.wash(sink, x + 0.0022f, y - 0.0016f, 0.0098f, 0.0112f, 0.34f * fade,
                Palette.OCHRE_PALE, seed + 1.4f, 0.90f);
    }

    /**
     * STYLE.md 8's health: ink strokes that dry out and fade as it drops.
     *
     * <p>The lost strokes stay on the sheet as dry ghosts rather than being rubbed
     * out, which costs nothing and says two things instead of one -- how much is
     * left, and how much there ever was. It sits at the head of the column because
     * the stanza already owns the vertical axis and a second vertical run beside it
     * is precisely the confusable pair combat-design.md 3 warns about.
     */
    private static void health(Brush.Sink sink, Look look, float fade) {
        for (int i = 0; i < look.maxHealth(); i++) {
            boolean alive = i < look.health();
            tick(sink, HEALTH_X + i * HEALTH_PITCH, HEALTH_Y, 0.020f, 0.0038f, HEALTH_WIDTH,
                    (alive ? HEALTH_ALPHA : 0.26f) * fade,
                    alive ? Palette.CLOTH_PALE : Palette.PAPER_COOL,
                    17.9f + i * 2.3f, 0f);
        }
    }

    /**
     * One short countable stroke: a life, or a charge.
     *
     * <p>Five points rather than the two a tick needs, because a brush's dry-brush
     * breakup is sampled along the path and a two-point stroke has no interior to
     * break. A live mark is drawn wet -- {@code dryness} zero -- so that a tick
     * counted by the player can never be a tick the noise happened to eat.
     */
    private static void tick(Brush.Sink sink, float x, float y, float halfLength, float lean,
                             float width, float alpha, Color ink, float seed, float dryness) {
        int n = 3;
        float[] xs = new float[n];
        float[] ys = new float[n];
        for (int i = 0; i < n; i++) {
            float u = i / (float) (n - 1);
            xs[i] = x + lean * (1f - 2f * u);
            ys[i] = y + halfLength * (1f - 2f * u);
        }
        Brush.stroke(sink, xs, ys, width, alpha, ink, seed, dryness);
    }

    /**
     * One written cartouche: the tile's own gesture, at the wetness of its ink.
     *
     * <h2>What a beat drinking a mark looks like</h2>
     *
     * <p>Not a fade and not a highlight. The mark <b>floods</b>: it goes to full
     * ink and stays there for most of its beat, so it is the strongest thing in the
     * column while it is resolving; then, in the last part of the beat, the pigment
     * spreads and breaks up; then it dries off the sheet over
     * {@link Readout#DRY_SECONDS}. Only the third stage is a fade, and by then the
     * mark has already said what it had to say.
     *
     * <p>The first version faded from the instant the beat opened, which made the
     * resolving mark the <em>weakest</em> in the column -- distinguishable, and
     * distinguishable in a way that reads as "this one has been cancelled".
     */
    private static void cartouche(Brush.Sink sink, float x, Look look, Look.Written w, float fade) {
        boolean resolving = w.spent() > 0.0 && w.dried() <= 0.0;
        float ownFade = resolving
                ? 1f - RESOLVING_RECESSION * (float) look.intimacy()
                : fade;
        float y = STANZA_BASE_Y + w.height() * STANZA_PITCH;

        float wet = (float) w.wetness();
        float dried = (float) w.dried();
        // Only the tail of the beat spreads the mark, so a clause that is resolving
        // is at full ink for most of the time it is resolving.
        float drink = smoothstep(0.45f, 1f, (float) w.spent());
        float alpha = (0.45f + 0.50f * wet) * (1f - 0.45f * drink) * (1f - dried) * ownFade;
        float dryness = 0.22f - 0.19f * wet + 0.50f * drink + 0.30f * dried;
        float scale = STANZA_GLYPH * (1f + 0.20f * drink + 0.10f * dried);
        Color ink = Palette.CLOTH_PALE;

        if (resolving) {
            // The wet bleed of a mark being taken: pigment wicking out of the
            // gesture into the paper around it. Under the glyph, so the strokes
            // stay the mark and this stays its damp.
            Brush.wash(sink, x, y, scale * 0.66f, scale * 0.34f,
                    0.17f * (1f - drink) * ownFade, Palette.CLOTH_PALE, w.seed() + 2.2f, 0.5f);
        }

        for (Glyph.Stroke s : Glyph.of(w.type(), look.heroStep())) {
            place(sink, s, x, y, scale, alpha * s.weight(), ink, w.seed(), dryness);
        }
        if (w.enchantment() != null) {
            Brush.wash(sink, x - scale * 0.42f, y + scale * 0.42f, scale * 0.11f, scale * 0.090f,
                    0.60f * ownFade * (1f - dried), Palette.OCHRE_PALE, w.seed() + 0.7f, 0.6f);
        }
    }

    private static float smoothstep(float a, float b, float v) {
        float t = Math.max(0f, Math.min(1f, (v - a) / (b - a)));
        return t * t * (3f - 2f * t);
    }

    /**
     * One tile still on the sheet, with its charges beside it.
     *
     * <h2>Charges are marks, and a banked tile has none</h2>
     *
     * <p>combat-design.md 1.2 counts a cooldown in <b>charges</b>, which is a
     * discrete quantity, so it is drawn as a discrete run of ticks and never as a
     * length: a tile at four of six is four marks and two ghosts, not two-thirds of
     * anything. A tile whose cooldown is zero draws no ticks at all, and the absence
     * is the correct statement -- there is nothing to wait for.
     *
     * <p>A banked tile is drawn as its own impression only. Its ink has moved up
     * into the column, which is the literal relationship {@link Look.Held#banked()}
     * describes, and showing the same glyph twice would make the two columns two
     * readouts instead of one.
     */
    private static void held(Brush.Sink sink, float x, Look.Held h, int index, int step, float fade) {
        float y = HAND_TOP_Y - index * HAND_PITCH;
        float seed = 53.1f + index * 6.7f;
        boolean ready = !h.banked() && h.charges() >= h.cooldown();
        float alpha = (h.banked() ? 0.17f : ready ? 0.66f : 0.34f) * fade;
        Color ink = ready ? Palette.CLOTH_PALE : Palette.PAPER_COOL;
        float dryness = h.banked() ? 0.66f : ready ? 0.08f : 0.40f;
        for (Glyph.Stroke s : Glyph.of(h.type(), step)) {
            place(sink, s, x, y, HAND_GLYPH, alpha * s.weight(), ink, seed, dryness);
        }
        if (h.banked()) {
            // No charge marks at all, and the absence is the rule: Loadout's own
            // note records that "a banked tile does not recover -- it is off the
            // sheet and in the hand". A tile that showed charges while it sat in the
            // column would be drawing a recovery that is not happening.
            return;
        }
        // <b>The run is drawn under its own tile, centred on it, and both halves of
        // that are measured.</b>
        //
        // Pass 2 ran it sideways out of the cartouche and into the picture. The
        // pass-2 review read the result as an enemy health row -- a long horizontal
        // run of equal marks standing on its own beside two Charted Shadows is a
        // bar, whatever it was drawn for -- and that alone would be a matter of
        // taste. What settles it is a delivered-pixel measurement taken after
        // {@code Stage.planning(Standing)} tightened the planning shot: with the run
        // beside the tile, a Parry at four charges printed its two spent ghosts at
        // {@code (794,340)} and {@code (770,340)} of {@code s5-p3-fold-replan}, and
        // the live frame is <b>bit-identical to the bare control</b> at the first of
        // them. The interface is drawn before the figures on purpose, so a run long
        // enough to reach into the picture is a run a Charted Shadow stands on. A
        // count that a body can erase is not a count.
        //
        // Centred under its own mark, the widest run the engine can produce --
        // eight charges -- spans {@code 8 * TICK_PITCH} around the column and never
        // leaves the margin at either shipped height. It also cannot detach from the
        // tile it belongs to, which is the review's finding answered rather than
        // argued with.
        float runY = y - TICK_ROW;
        float first = x - (h.cooldown() - 1) * TICK_PITCH * 0.5f;
        for (int c = 0; c < h.cooldown(); c++) {
            boolean got = c < h.charges();
            tick(sink, first + c * TICK_PITCH, runY, TICK_HALF_LENGTH, 0.0018f, TICK_WIDTH,
                    (got ? CHARGE_ALPHA : CHARGE_GHOST) * fade,
                    got ? Palette.OCHRE_PALE : Palette.PAPER_COOL, seed + c, 0f);
        }
    }

    /**
     * One stroke of a glyph, placed on the sheet.
     *
     * <p><b>The two drynesses are combined by taking the drier, not by adding.</b>
     * A stroke's own {@code dryness} says what the gesture is -- only the Feint and
     * the Back-step's scuff are anything but zero -- and the {@code state} dryness
     * says how the sheet is holding it: fresh, waiting, or banked. They are two
     * statements about one brush, and summing them saturates. Pass 2 summed them,
     * and a Feint sitting in a hand at state dryness 0.40 came out at the 0.92 clamp
     * and delivered a peak lift of 4.5 over bare -- a tile the sheet claims to hold
     * and the eye cannot find. Taking the maximum changes nothing for the seven
     * glyphs whose own dryness is zero.
     */
    private static void place(Brush.Sink sink, Glyph.Stroke s, float cx, float cy, float scale,
                              float alpha, Color ink, float seed, float state) {
        float[] xs = new float[s.xs().length];
        float[] ys = new float[s.ys().length];
        for (int i = 0; i < xs.length; i++) {
            xs[i] = cx + s.xs()[i] * scale;
            ys[i] = cy + s.ys()[i] * scale;
        }
        float dryness = Math.max(state, s.dryness());
        Brush.stroke(sink, xs, ys, s.width() * scale, alpha, ink, seed,
                Math.max(0f, Math.min(0.92f, dryness)));
    }
}
