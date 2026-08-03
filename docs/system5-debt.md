# System 5 — standing debt

**Status: pass 3 shipped, answering the pass-2 review's FAIL. Not self-declared a pass.**
A reviewer must never grade work it produced, and this document grades nothing; it records
what was built, what was measured, what was corrected in pass 2's own account, and what is
still owed.

Every capture quoted is `s5-p3-*`, shot at the harness the tree is at. Every number about the
interface is a **difference against a `-bare` control shot in the same window at the same
harness** (§11.2b(g)), and is printed beside the rectangle it was taken through (§11.3). Every
pixel number was taken with an independently written NumPy/PIL reader using the Rec. 709
weights `Frame.java` declares (§11.2b(c)); where a pass-2 figure is quoted for comparison it
was **re-measured through that same reader on the pass-2 captures**, so the two columns are
one instrument.

Suite: **413 tests, 0 failures, 0 skipped**, verified by parsing
`build/test-results/test/*.xml` after `./gw test --rerun-tasks`.

**A framing decision arrived mid-pass and is recorded here because it changes how this work
should be read.** The owner has ruled that the planning view is **a map, not a picture** —
maximum tactical readability, deliberately soberer aesthetic — so the pass-2 review's verdict
*"a diagram on a nice background"* is no longer a failure at that framing. What survives from
its item 1, and is what this pass acted on, is the half about **legibility**: the fog was spent
as alpha attenuation of the subject, which removed marks from the figure and added none. A map
whose pieces are harder to read is a worse map.

---

## 0. What the review failed this pass on, and what happened to each

| # | The review's finding | Status |
|---|---|---|
| 1 | The graded planning shot went backwards: ~8 readable parts → ~6, hero 87 px → 77 px, because the fog was spent attenuating the subject | **Answered.** Fog is bands and adds marks; the subject-attenuation term is halved; the framing law is open and the hero is **194 px**. §1 |
| 2 | The raster guard hard-codes `SHIPPED_HEIGHTS[0]` and is red at 540 (0.3563 vs 0.34); its message prints `960x720` whatever it measured; the ceiling is fitted, not derived | **Fixed, all three.** It enumerates the axis, reports its own parameters, and the ceiling is a corpus-derived **band with both edges**. §2 |
| 3 | A panel hatched from only legal `Brush.stroke`s reads 0.0515 and passes all 410 tests | **Closed by a criterion about form**, with the exhibit checked in. §3 |
| 4 | `BACK_STEP` reads as an attack; `THRUST` reads as two footfalls; `FEINT` is invisible at peak lift 4.5; the `SWEEP` exception lives in prose | **All four addressed.** §4 |
| 5 | Six claims in this document do not reproduce | **All six corrected or struck.** §5 |
| 6 | The substrate was calibrated on one image; the fog on another family | **Re-run across the family, and the two families are now assigned by question.** §6 |

---

## 1. The composition, which was the standing failure

### 1.1 The framing law, opened

`Stage.planning(Standing)` is new and it is the item three reviews in a row have named and two
have declined. The planning framing is now taken from **the exchange** — the hero and the
Charted Shadow nearest it — with the same margin the lane law already spends, floored at
`Stage.SHORTEST_PLANNING` (the five-tile lane's own planning framing, 6.5 tiles) and capped at
the lane's own framing so it can only ever tighten a shot.

| bout | lane's own framing | the shot it now plans in | figure share | px of 720 |
|---|---|---|---|---|
| `KNIFE` (5 tiles) | 6.50 | **6.50**, centred on tile 2.00 | 0.2250 | 162 |
| `FOLD` (11 tiles) | 12.50 | **6.50**, centred on tile 3.00 | 0.2250 | 162 |
| `APPROACH` (15 tiles) | 16.50 | **6.50**, centred on tile 4.00 | 0.2250 | 162 |

The floor is the one number in the law not forced by the board, and it is derived rather than
chosen: it is `planning()` evaluated at `Lane.MIN_LENGTH`. Checked against the corpus — the
smallest figure any reference image asks to carry meaning is a Family C background figure at
about **0.22** of the frame height (image 7, top panel, ~120 px of a 544-px panel), and the
Family B duellists run 0.55 to 0.65. At 12.5 tiles the hero was 0.107 of the frame, *outside*
the corpus's own spread on the low side. `theGradedBoutsPlanAtASizeTheCorpusAsksAFigureToCarry`
asserts it and prints the numbers above.

**Delivered.** Hero detected against each row's own median inside `x290..380`, `y180..515`, ≥2 px
per row, at an 8-level threshold: **`y322..515` = 194 px**. The same detector on
`s5-p2-fold-plan` frame 3 through `x205..300` gives **87 px**. That is **2.23×**. (My reader
reads pass 2 at 87 px where the pass-2 review's reader read 77; the two differ on how much of
the dissolving hem they keep, and the ratio is quoted through one instrument on both.)

**What it does not change, and this was the constraint.** `pushIn()`, `pushInSeconds()` and
`returnSeconds()` still read `planning()` — the *lane's* framing — so the duration law of §9 is
a property of the lane and every schedule the project has fingerprinted keeps its length.
`systemFoursSchedulesKeepTheirDurationsAcrossTheFramingLaw` pins all three: `PHRASE` 6.9521 s,
`PARRY` 3.0400 s, `KNOCKBACK` 3.1548 s. It was **observed red** during authoring by pointing
`pushIn()` at the new framing, which takes `PHRASE` to 6.8410 and `KNOCKBACK` to 3.0932.

**⚠ It does change two of System 4's *shots*, and this is the item to merge carefully.**
`duel-parry` is on a five-tile lane and is **untouched** (6.50 tiles, centre 2.00, identical).
`duel-phrase` and `duel-knockback` are on seven-tile lanes and their *wide* framing tightens
from 8.50 tiles to 6.50 — the figures in those two scenes' planning shots are about 31% larger
than in the archived `s4-*` captures. Nothing about their timing, their beats or their
fingerprints moves. System 4 is closed at five passes and nothing re-grades those frames, but
any comparison against an archived `s4-*` capture that includes a wide shot is now void by
§11.2b(d) and should be re-shot.

### 1.2 Fog as bands, not as attenuation of the subject

§6's first bullet is the only line in the rubric marked *non-negotiable*, and pass 2 answered
it with an alpha term on the **figures**. `Fog.java` delivers it as the section asks: two banks
of drifting horizontal `#D6D2CE` bands, six per bank, stratified across the frame so none of it
is empty, each band a string of eleven washes whose rims sit on zero alpha — so a bank of fog
cannot print an edge at any alpha. `FAR` is drawn behind the bodies (depth separation);
`NEAR` in front of them and below hem height (occlusion of the lower body), tapered to nothing
over the outer 17% of the frame's width so it never veils either margin's readout.

It is **world, not interface**: the `-bare` control carries it too, so every `live − bare`
number in this document still measures the interface.

The subject-attenuation term survives at half strength (`LaneScene.SUBJECT_HAZE = 0.5`) as §6's
second bullet — depth desaturation on a body seen through fog — and the tighter framing lowers
`Readout.haze` at the planning shot from 1.00 to 0.355 on its own, so the figure keeps far more
of its contrast than it did.

### 1.3 The composition statistic

`>8` luminance from the pixel's own row median, whole frame, frame 3, live vs `-bare`:

| | pass 1 | pass 2 | **pass 3** |
|---|---|---|---|
| bare **planning** frame that is anything at all | 4.84% | 4.76% | **11.79%** |
| bare **execution** frame | 16.61% | 14.71% | **17.26%** |
| the interface | 2.09% | 2.16% | **2.99%** |
| interface / world, planning | 0.431 | 0.454 | **0.254** |

The planning frame carries **2.48×** the deviation it did, and the interface's share of it fell
by 44%. Two thirds of that world number is the fog bands and one third is the figures being
twice the size.

### 1.4 §11.0's matched-scale part count, re-run

Reference image 3 downscaled by 194/665 so its dark duellist matches the delivered hero, both
viewed at 5× nearest-neighbour.

| | readable parts at ~194 px |
|---|---|
| **reference image 3** | topknot, hair mass, head **with a face** (brow, nose, chin), neck, collar, shoulder, upper arm, forearm, hand, grip, sash with its knot, second blade with red cord, skirt with fold structure, forward leg, dissolving hem, ground smear — **~16** |
| capture, pass 1 | hair mass, head as a lump, torso, ochre sash band, a second ochre mark, blade, shed flecks, a lower mass tapering to a point — **~8** |
| capture, pass 2 | hair mass, head as a lump, torso, ochre sash band (fainter), blade, shed flecks — **~6** |
| **capture, pass 3** | hair mass, streaming hair wisps, head as a lump, shoulder, upper arm, forearm mass, torso, ochre sash band, second ochre mark below it, skirt mass, forward foot, blade, shed flecks — **~13** |

**~6 → ~13 against the reference's ~16.** What is still missing is the *figure* half the
pass-2 review assigned to System 4 as permanent debt — no face, no separated hands, no two
legs — and this pass did not touch the rig. What System 5 owed was the framing half, and at
194 px System 4's articulation now has room to show.

### 1.5 The hand column, which the reviewer read as the enemy's

Four changes, and one of them is a measurement rather than a composition argument.

1. **The charge run is drawn under its own tile, centred on it** (`LaneInterface.held`). Pass 2
   ran it sideways out of the cartouche and into the picture. Cold, the reviewer read the
   result as an enemy health row. What settles it is a delivered-pixel measurement taken after
   the framing tightened: with the run beside the tile, a Parry at four charges printed its two
   spent ghosts at `(794,340)` and `(770,340)` of `s5-p3-fold-replan` frame 2 — and the live
   frame is **bit-identical to the bare control at the first of them** (both `RGB(33,37,48)`,
   luminance 36.9). The interface is drawn before the figures on purpose, so a run long enough
   to reach into the picture is a run a Charted Shadow stands on. *A count a body can erase is
   not a count.* Centred, the widest run the engine can make — eight charges — never leaves the
   margin at either shipped height.
2. **Both margins carry the same seal at their head** (`LaneInterface.seal`): one ochre chop,
   same mark, same pigment, same height, one at each end of the sheet's head. One sheet, one
   author, both columns the player's. Ochre and not vermillion, because guard B asserts every
   vermillion vertex lies over a threatened tile and a seal in the margin would either break it
   or dilute the one colour that means danger.
3. **`HAND_INSET` 0.135 → 0.125**, set by the run rather than by taste: the smallest inset at
   which a centred eight-charge run stays on the sheet at both shipped heights.
4. **The framing law no longer leaves the bodies against the right edge.**

**Whether it now reads as the hero's is a cold read and only a reviewer can make it.**

---

## 2. The hard-edge guard: where it runs, what it says, and where its number comes from

### 2.1 It runs at every shipped height now

`noMarkPrintsAnEdgeInTheRasterItWouldDraw` iterates `Guards.SHIPPED_HEIGHTS` instead of
indexing `[0]`, and its failure message reports the width and height it actually measured
through. §11.2b(f)'s fourth clause, applied: *enumerate the axis, do not index it*.

Delivered through the guard's own raster, worst over the whole bout, printed by the test's own
control line:

| bout | 960×720 | 720×540 |
|---|---|---|
| `KNIFE` | 0.2441 at (848,398) | **0.2825** at (641,357) |
| `FOLD` | 0.2157 at (63,102) | 0.2603 at (267,393) |
| `APPROACH` | 0.2158 at (63,102) | 0.2829 at (652,348) |

Pointed at 540 with pass 2's geometry the guard read **0.3563**, which is the review's number
reproduced. Three geometry changes brought it down and each was found by the guard going red:
the charge tick is drawn with three points rather than five (a five-point stroke lands its
pressure over a quarter of a very short mark, which at 540 rows is two pixels); it is wider
(0.0105 → 0.0130 of the frame height) and quieter (0.74 → 0.62); and the `BACK_STEP` hook was
opened and narrowed, because at a half-width wider than the radius of its own turn a ribbon
folds across itself and the two coats print a bright core — measured at **0.5479** of amplitude
in one pixel, the same failure the `DRAW` glyph's own note already records.

### 2.2 The ceiling is a band with both edges, taken from the corpus

The old ceiling was 0.34, *"a third of what a hard edge prints"*. The review convicted it on
three counts and all three are right: it is a ratio to an **artefact** (1.000 is what a step
function reads, a question no reference image can inform or fail); it is a **floor with no
upper edge**, which §11.0 forbids by name; and it was **fitted**, sitting one part in a hundred
on the permissive side of the only construction that beat it.

Measured on the Family B reference images — 3, 4 and 5, which §1 calls *"the primary template
for the game screen"* — with the same statistic the guard uses (steepest single-pixel step
anywhere in the picture, over the picture's own amplitude, Rec. 709 luminance):

| | region | steepest 1-px step / amplitude |
|---|---|---|
| image 3, matched to a 194 px figure | whole frame less 2 px | **0.3542** (on a blade) |
| image 3, native 832×1088 | whole frame less 2 px | 0.3771 (on a blade, at (577,303)) |
| image 4, native | same | 0.3986 (at (432,461)) |
| image 5, native | same | 0.4352 (at (441,461)) |
| image 3 sky | `x40..790 y40..200` | 0.1101 |
| image 4 sky | same | 0.1223 |
| image 5 sky | same | **0.1075** |
| image 3 garment and ground, no blade | `x40..790 y740..1080` | 0.3743 |
| image 4, same | same | 0.2581 |
| image 5, same | same | 0.3579 |

- **Ceiling 0.354** — the *softest blade edge the corpus contains*, at the matched scale §11.0
  requires. §3 and §5 allow exactly one hard-edged object; the ceiling says no mark in the
  interface may arrive as abruptly as that one.
- **Floor 0.107** — the softest region the same three images contain, their skies. An interface
  below that is softer than the corpus's own sky, which is to say it has stopped being marks
  and become weather. This is the edge the old criterion did not have, and it is what stops
  "make it softer" being a free move. The guard asserts it on the bout's own worst step.

**What this ceiling does not do, stated plainly.** It does not catch a feathered rectangle
(attempt 3c reads 0.333) or the `Brush`-only hatch (0.0518), and it never could: both are
*softer* than the interface. That defect is about form and §3 is its guard.

### 2.3 And in delivered pixels

`D = luminance(live) − luminance(bare)`, largest single-pixel `max|∂D|` in either axis, over
every frame of every capture with a control, ignoring an 8 px frame border.

| capture | frames | worst / that capture's own amplitude | worst / the interface's strongest amplitude | where |
|---|---|---|---|---|
| `s5-p3-fold-plan` | 6 | **0.2380** (21.64 / 90.91) | 0.2380 | frame 5, (133,26) |
| `s5-p3-fold-strike` | 12 | 0.3368 (22.04 / 65.44) | **0.2424** | frame 7, (648,681) |
| `s5-p3-fold-replan` | 5 | 0.2591 (21.44 / 82.78) | 0.2359 | frame 1, (853,399) |
| `s5-p3-fold-empty` | 4 | 0.3499 (25.09 / 71.70) | 0.2760 | frame 3, (857,388) |
| `s5-p3-fold-pushin` (@0.0167) | 48 | 0.2910 (28.02 / 96.27) | 0.3082 | frame 13, (858,387) |
| `s5-p3-fold-bleed` (@0.0167) | 36 | 0.3874 (14.94 / **38.56**) | **0.1643** | frame 35, (869,387) |
| `s5-p3-knife-plan` | 4 | 0.2486 (22.71 / 91.34) | 0.2498 | frame 0, (855,476) |
| `s5-p3-approach-plan` | 4 | 0.2378 (21.62 / 90.91) | 0.2378 | frame 3, (943,497) |
| `s5-p3-fold-plan` **at 720×540** | 6 | 0.3108 (27.85 / 89.62) | 0.3108 | frame 1, (30,19) |
| `s5-p3-fold-replan` **at 720×540** | 5 | 0.3460 (27.78 / 80.28) | 0.3100 | frame 1, (30,19) |
| `s5-p3-knife-plan` **at 720×540** | 4 | 0.3198 (29.21 / 91.34) | 0.3260 | frame 0, (641,357) |

The graded frame is **0.2380** against pass 2's 0.244 and pass 1's 0.249. Every capture is
inside the corpus band on the right-hand column, which is the form the checked-in guard uses
and the one `fold-bleed` exists to justify: that window sits at the intimate framing where the
interface has given up most of its presence, so its own amplitude is 38.56 where the planning
frame's is 90.91, and its *absolute* step, 14.94 levels, is the smallest in the table.

**One honest complication, new this pass.** The guard measures **coverage** and this table
measures **luminance**, and the two stopped being the same ordering when the framing tightened:
a pale mark over the dark near-ground converts a given coverage step into a larger luminance
step than the same mark over the sky. Pass 2's docstring claimed the coverage measurement was
*"therefore stricter than the delivered measurement"*; that was true when every mark sat on the
sky and it is no longer true in general. Both are published above.

---

## 3. The `Brush`-hatch hole, closed by a criterion about form

`Guards.hatchPanel` is the pass-2 reviewer's construction checked in at its own parameters —
strokes of width 0.080 at a pitch of 0.020, running `x0.51..1.09` across `x0.60..1.00
y0.60..0.90` in frame heights. Reproduced: **0.0518** of its amplitude in one pixel (the review
published 0.0515), zero flat-fill triangles, zero inked silhouette edges. It passes all three
hard-edge guards, and `theBrushOnlyHatchIsCaughtByTheFormGuardAndByNothingElse` asserts that it
does, because that is the finding and deleting it would leave the fix without its defect.

The criterion that catches it is `Raster.inkBlock`: **the largest square of solid ink the
picture contains**, computed as the largest `min(horizontal run, vertical run)` over every
pixel. `noMarkHoldsAFlatRunAcrossTheSheet` caps it at one whole cartouche
(`STANZA_GLYPH` = 0.098 of the frame height: 71 px at 720 rows, 53 at 540), swept over every
state of every bout at both shipped heights.

| | largest inked block |
|---|---|
| the interface, `KNIFE` / `FOLD` / `APPROACH` at 960×720 | 24 / 24 / 25 px |
| the same at 720×540 | 19 / 19 / 19 px |
| **ceiling** | **71 px / 53 px** |
| the pass-1 bordered panel | 216 px |
| **the pass-2 `Brush`-only hatch** | **247 px** |

### 3.1 Two things the review got right about this, and one it did not

The review named the criterion as *"no axis-aligned run of near-constant coverage longer than
N px, on either axis"*. Built and measured, that literal form fails twice.

- **On one axis it convicts the interface's own lane.** A wash lying on the ground is seen
  edge-on: measured, `KNIFE` at t=8.100 holds **129 px** of near-constant coverage across
  `(47,522)` and about thirty vertically. That is a wash, which §8 asks for by name.
- **"Near-constant" is the wrong predicate**, because a hatch ripples at its stroke pitch. At a
  band of 0.06 of amplitude the review's own exhibit measures **23 px against the interface's
  13** — a margin of 1.8×, which a hatch tuned to ripple a little harder closes entirely.
  Thickness has no such dial: the same hatch reads **247 px against the interface's 27**.

So the criterion shipped is about **thickness in both axes at once**, which is the same idea
one level firmer: a filled region has an interior, a stroke is a ridge and a wash is a lens.
**What the review got right** is that the answer is about form and not about gradient, and that
it costs an afternoon.

**Scope, stated:** it catches a filled region *thicker than a mark*. A filled rectangle smaller
than one cartouche passes it, and nobody has a general detector of rectangular composition.
That remains permanent debt, as the review said it should be.

---

## 4. The alphabet

**Three of nine marks changed, and a reviewer's cold read is the only instrument that can grade
them. `CUT`, `PARRY`, `SWEEP`, `STEP`, `DRAW` and `TURN` are untouched.** `PARRY` in particular
— the protected result — is byte-identical to the mark the pass-2 review read correctly and
named as a deflection from its shape alone.

### 4.1 `BACK_STEP`: the retreat is the loud part now

The pass-2 reviewer read it cold as *"a strike, aimed leftward. This is the enemy's intent."*
The diagnosis is in the pressure profile rather than in the drawing: a `Brush` stroke lands
heavy and thins along its travel, so a stroke authored from `x=+0.46` to `x=−0.50` prints a
heavy head on the right and a fine barbed point on the left, which is exactly what a blade
travelling leftward looks like. The two marks that carried the meaning measured 21.6 and 18.9
of lift against the travel stroke's 77.8.

The mark is now authored **from the rear**: the heel lands at the back, catches, and hooks, so
the brush's own pressure puts the mass behind the body and thins the trace toward the enemy.
Measured on the isolated 71 px cartouche, alpha-weighted:

| | peak at | centroid x (box 0..70) |
|---|---|---|
| `STEP` | x=47 | 38.6 |
| `BACK_STEP`, pass 3 | **x=5** | **19.2** |

The two movement verbs now put their mass at opposite ends of the tile. Delivered in the hand,
`s5-p3-fold-plan` frame 3 through `x835..905 y369..423`: peak lift **47.6** at (858,388), which
is above the tile's centre line — the hook.

### 4.2 `THRUST`: something a movement cannot have

Read cold by two reviewers: once correctly with the answer supplied, once — unaided — as *"two
short level lens dashes with a gap… a weight transfer"*, which is `STEP`'s own device. The
brief was *"give it something a movement cannot have"*, and a movement cannot have **a body in
it**. The beat's own sentence is now drawn literally: a long level stroke, a steep stroke
across it, and the line continuing past rather than stopping at it. Nothing else in the
alphabet has one stroke crossing another at a steep angle; `PARRY`'s two run *alongside* each
other for a third of the mark, which is the span that makes it a deflection.

Its mirror distance falls 0.7919 → **0.6639**, because the crossing stroke is near-symmetric.
Still 3.3× the floor.

### 4.3 `FEINT`: it stopped being drawn as absence

Measured by the review at peak lift **4.5** with six pixels above the noise floor — three times
fainter than an *empty* slot's impression. Two causes, both fixed.

- The mark was one stroke at `dryness` 0.58, and `LaneInterface.place` **added** the sheet's own
  state dryness to it, so a Feint in a hand at state 0.40 came out at the 0.92 clamp. The two
  are statements about one brush and are now combined by taking the drier of them, which
  changes nothing for the seven glyphs whose own dryness is zero.
- The mark is now a gesture *and the gesture it did not make*: one committed stroke and, beside
  it, the same stroke offset and dry. The ink is present; what says "no contact" is that the
  second mark never lands.

Delivered in the hand, `s5-p3-fold-plan` frame 3 through `x835..905 y437..491`: peak lift
**57.4** at (877,469), against the faintest empty-slot impression on `s5-p3-fold-empty` frame 2
of **13.9**. That is 12.8× pass 2's 4.5, and 4.1× the impression it used to be three times
fainter than.

### 4.4 The `SWEEP` exception is asserted rather than described

`sweepIsTheOnlyTilePermittedToBeItsOwnMirror`: Sweep is below the 0.20 floor under mirroring
and **every other tile is above it**, both halves failing if the alphabet moves. The review's
point was §11.2b(e) exactly — the exception lived in prose while the guard `continue`d past the
axis it excluded, and a `TURN` drawn as a closed ring is one keystroke away.

Same-tile mirror distances at the shipped 71 px cartouche, printed by the test:

`CUT 0.8558  THRUST 0.6639  PARRY 0.8471  SWEEP 0.1543  DRAW 0.9062  STEP 0.9427
BACK_STEP 0.9405  TURN 0.7929  FEINT 0.9960`

Closest cross-tile pair of 144: **`PARRY` facing left and `DRAW` facing left = 0.7601**, against
a floor of 0.20. Pass 2's closest was 0.7277 (`STEP+` / `BACK_STEP+`); re-authoring the retreat
moved it off the bottom of the table.

---

## 5. The six claims the review found, corrected

1. **§9's *"a soft-edged panel still passes every guard here"*.** False as written: the 5 px
   feathered panel fails `noTriangleInTheInterfaceIsAFlatFill` on its two interior triangles
   when drawn. §1.3's narrower sentence — "passes both *hard-edge* guards" — was the correct
   one. **Superseded anyway**: as of §3 above, a filled region of any softness is caught by the
   form guard.
2. **§1.3, attempt 4 — the hatch "caught by raster".** The *construction class* was not caught,
   only that instance of it. At width 0.080 / pitch 0.020 it reads **0.0518** and passes every
   hard-edge guard. Recorded correctly in §3, with the exhibit checked in.
3. **§4.1a and `ink_resolve.frag` — the 1,180 px.** Does not reproduce. The review measured 0
   for the straight-through form the comment names, in all three shaders, against a null of 0
   over three shoots; 192 px for *deletion*. **Not re-measured this pass** — the shader files are
   being worked on in a parallel worktree and this pass did not touch them. The comment still
   carries the wrong number and the wrong edit. **Unpaid, and it belongs to whoever merges the
   shaders.**
4. **`ink_resolve.frag` — "every capture System 4 is graded on is bit-identical across this
   change".** Not testable: System 4's graded window's own null control is 13,825–25,547 px.
   The honest statement is that only the 6-frame window where the null is 0 can carry a
   bit-identity claim at all. Same disposal as above: **the comment is in a shared file this
   pass did not touch.**
5. **§2's two glyph distances.** Both were stale. Re-measured through the guard's own code path
   on pass 2's alphabet, `BACK_STEP`'s mirror was **0.8611** against a published 0.890 and the
   closest cross-tile pair **0.7277** against a published 0.733. Both marks have since been
   re-authored; §4.4 carries the current table.
6. **§9's 20 Hz sweep.** **Struck.** The review measured 60 Hz to move the worst share by
   0.0006 on all three bouts; the debt item is discharged, not outstanding. The guard still
   samples at 20 Hz and its docstring still says so, which is the honest form: the *rate* is a
   cost decision, and the claim that a defect could hide between samples is the one that was
   refuted.

---

## 6. The substrate, across the family

Pass 2 reported *"against the sky of the family this stage is quoting, the capture matches to
1%"*, from one image. §11.0: *"one reference image is not the corpus."* Measured through one
reader, 3 px high-pass standard deviation:

| image | family | box | hp(3 px) | pass 2 capture / image | **pass 3 capture / image** |
|---|---|---|---|---|---|
| 3 | B | `x80..699 y80..239` | 0.4924 | 0.96× | **1.14×** |
| 4 | B | same | 0.7482 | 0.63× | **0.75×** |
| 5 | B | same | 0.5657 | 0.84× | **0.99×** |
| 6 | C | `x60..399 y20..139` | 0.8059 | 0.59× | 0.70× |
| 7 | C | `x60..399 y30..149` | 1.1453 | 0.41× | 0.49× |
| 8 | C | `x60..399 y30..149` | 5.5014 | 0.086× | 0.10× |
| capture (bare), pass 2 | — | `x300..699 y120..319` | 0.4743 | — | — |
| **capture (bare), pass 3** | — | same | **0.5612** | — | — |

Pass 2's capture sat **at or below the floor** of the corpus's own spread and matched only the
smoothest sky in it. Pass 3's sits **inside Family B's band**, and the surface came from the fog
bands rather than from anything added to the sky.

**Which family the planning framing belongs to is now settled and filed in `combat-design.md`
§3.** §1 already divides the labour: Family B governs the stage and the colour script and is
*"the primary template for the game screen"*; Family C governs *"atmosphere, depth/fog,
background figures, the planning phase"*. So the **surface** standard is Family B's and the
**air** standard is Family C's — one shot, two families, one for each question. That is not an
inconsistency once it is written down, and neither pass had written it down.

---

## 7. Every protected result, re-measured

All through one reader, all against the `-bare` sibling in the identical window.

| result | region | pass 2 | **pass 3** |
|---|---|---|---|
| Hard edge, `fold-plan` | whole frame less 8 px, 6 frames | 0.244 | **0.2380** (21.64 / 90.91) |
| Hard edge, `fold-strike` | 12 frames | 0.223 | 0.3368 per capture, **0.2424** against the interface's strongest |
| Determinism | `fold-plan` ×6, `fold-strike` ×12 vs `-repro` | 0 px | **0 px** |
| Stanza out-reads health | `x36..254 y10..44` vs `x62..132 y53..123` | +72.8 vs +94.7 | **+72.4 at (86,26) vs +90.6 at (100,88)** |
| The drying column, peak lift | `x62..132`, five pitches, frame 3 | 94.7 / 86.7 / 78.8 / 72.6 / 68.8 | **90.6 / 88.4 / 77.8 / 67.2 / 50.7** |
| — ink px (lift > 4) | same | 2175 / 1292 / 744 / 519 / 493 | **1996 / 1294 / 564 / 463 / 394** |
| Lane band | `x0..959 y505..559` | 1.110×, +84.8 at (172,535) | **1.157×, +86.8 at (615,536)** |
| Draw order | `fold-strike` f4, `y560..699` | 0.00 under body ink | **not reproduced — see §8** |
| Empty state | five slot boxes, `fold-empty` f2 | 21.2 / 18.3 / 14.3 / 14.9 / 34.0 | **24.9 / 13.9 / 14.3 / 17.8 / 20.0** |
| Vermillion budget | `r−b` lift > 20, frame 3 | 1,923 px = 0.278% | **3,484 px = 0.504%**, live `(150,66,64)` / bare `(51,54,65)` at (340,527) |
| Cooldowns countable, 960×720 | `fold-replan` f2 & f4, hand rows 2–6 | 3 / 3 / 4 / 1 / 5 | **3 / 3 / 4 / 1 / 5** |
| Cooldowns countable, **720×540** | same, `-540` captures | 3 / 3 / 4 / 1 / 5 | **3 / 3 / 4 / 1 / 5** |

Three of these moved and each is a consequence of something changed on purpose.

- **The column's base mark fell from 68.8 to 50.7.** The tighter framing lifted the horizon, so
  the foot of the stanza now sits over the bright coral band rather than over the dark sky, and
  a pale mark on a bright ground lifts less. The gradient is still monotone top to base, which
  is what the guard is on, and it is *steeper* than it was.
- **The lane band rose to 1.157×.** More lane is in frame at 6.5 tiles than at 12.5.
- **The vermillion budget doubled again**, 0.278% → 0.504% of the frame. The Strikethroughs are
  the same marks over the same tiles; the frame is half as wide, so they cover twice the share.
  Guard B still holds — every vermillion vertex lies within half a tile of a tile the engine
  says is threatened — but 0.5% of a frame is worth a reviewer's attention and §2.2's *"a few
  small marks per frame"* is being leaned on.

---

## 8. What this pass did not do, with the number beside it

- **The draw-order result is not reproduced this pass, and the reason is the framing.** Pass 2
  measured 0.00 of `|live − bare|` under 6,977 px of body ink in `s5-p3-fold-strike` frame 4,
  band `y560..699`. At the new framing that band is no longer under the bodies at that frame —
  the camera is elsewhere — and every dark-pixel mask I tried selected the near ground rather
  than a figure, reading 17–27 of mean delta because the *lane's own wash* is drawn there. The
  property is unchanged in the code (`LaneScene` draws the interface before the figures, one
  line, unmoved) and §1.5 above contains a delivered instance of it working — a charge tick
  drawn under a Charted Shadow is bit-identical to the bare frame. **But the statistic the
  review protected has not been re-measured in the form it was published in. Unpaid.**
- **A cooldown of 0 draws no ticks, and an independent reader can still find one there.**
  Reading `s5-p3-fold-replan` at hand row 1 (`STEP`, cooldown 0) my reader returns 1 run: it is
  the `STEP` glyph's own lower rim, 5 px above the tick row, inside a ±4 px band. The suite's
  guard differences against a control and correctly returns 0; a player cannot. The margin
  between a cartouche and the run beneath it is **0.014 of the frame height, 10 px at 720 and
  7 at 540**, and that is thin. **Measured and recorded rather than fixed.**
- **The `ink_resolve.frag` comment still carries the 1,180 px and the untestable bit-identity
  sentence.** Both are review findings 3 and 4 and both are in a **shared shader file** being
  worked on in a parallel worktree. Not touched. **Unpaid.**
- **Enemy hit points are still not drawn at all.** The largest omission. A `Figure`/
  `InkMaterial` change. **Unpaid, and the pass-2 review assigned it elsewhere.**
- **The general shape criterion.** §3 catches a filled region thicker than a mark. A filled
  rectangle smaller than one cartouche passes it, and no one has a general detector of
  rectangular composition. **Accepted as permanent debt.**
- **The hand's live tiles read at about half the stanza's top mark** — 47.6 and 57.4 against
  90.6 — and most of the hand is banked ghosts at alpha 0.17 in the planning shot. Under the
  owner's new *map* reading, where legibility of the available actions is the point of this
  frame, a reviewer may want them louder. Raising them costs the hard-edge numbers of §2.
  **Measured, not decided.**
- **A 3 px paper tooth in the margin is still not delivered**, only stain structure. It is a
  `PaperBackground` question and that file is in the parallel worktree's area. **Unpaid.**
- **Nobody has read the Strikethrough's arrival as an arrival.** `s5-p3-fold-bleed` exists at
  60 Hz; no statistic was taken through it beyond §2.3's sweep. **Half-paid, as before.**
- **A spent mark's drying has still never been captured at a true frame rate.**
- **`LaneScene` still duplicates about sixty lines of `DuelScene`'s render body**, and it has
  drifted further: the fog banks are set here and not there. Deliberate. **Accepted as
  permanent debt by the pass-2 review.**
- **`Opaque` is still public** to work around a one-line `CaptureApp` bug that is still there.
- **The raster and form guards sweep at 20 Hz and 10 Hz.** Rasterising 960×720 is three orders
  of magnitude dearer than reading vertices, and the form guard needs two passes because its
  thresholds are shares of an amplitude the sweep has to find first. Both rates are in their
  own docstrings. The 20 Hz *claim* was discharged by the pass-2 review's own 60 Hz measurement
  (§5.6); the 10 Hz one is new and unmeasured.
- **Whether the re-authored `BACK_STEP`, `THRUST` and `FEINT` read as what they are** is the one
  thing only a reviewer can grade, and this pass spent cold-read budget on three of nine marks.

---

## 9. The captures, and how to reproduce them

Every live capture has a `-bare` sibling in the identical window.

```
./gw capture -Pscene=lane-fold      -Pout=out/captures/s5-p3-fold-plan     -Pframes=6  -Pcols=3 -Pstart=0.4  -Pstep=0.5    -Pw=960 -Ph=720
./gw capture -Pscene=lane-fold-bare -Pout=out/captures/s5-p3-fold-plan-bare         (identical window)
./gw capture -Pscene=lane-fold      -Pout=out/captures/s5-p3-fold-plan-repro        (identical window)
./gw capture -Pscene=lane-fold      -Pout=out/captures/s5-p3-fold-strike   -Pframes=12 -Pcols=4 -Pstart=3.3  -Pstep=0.42   -Pw=960 -Ph=720
./gw capture -Pscene=lane-fold-bare -Pout=out/captures/s5-p3-fold-strike-bare       (identical window)
./gw capture -Pscene=lane-fold      -Pout=out/captures/s5-p3-fold-strike-repro      (identical window)
./gw capture -Pscene=lane-fold      -Pout=out/captures/s5-p3-fold-replan   -Pframes=5  -Pcols=5 -Pstart=15.8 -Pstep=0.35   -Pw=960 -Ph=720
./gw capture -Pscene=lane-fold-bare -Pout=out/captures/s5-p3-fold-replan-bare       (identical window)
./gw capture -Pscene=lane-fold      -Pout=out/captures/s5-p3-fold-empty    -Pframes=4  -Pcols=2 -Pstart=11.3 -Pstep=0.35   -Pw=960 -Ph=720
./gw capture -Pscene=lane-fold-bare -Pout=out/captures/s5-p3-fold-empty-bare        (identical window)
./gw capture -Pscene=lane-fold      -Pout=out/captures/s5-p3-fold-pushin   -Pframes=48 -Pcols=8 -Pstart=2.95 -Pstep=0.0167 -Pw=960 -Ph=720
./gw capture -Pscene=lane-fold-bare -Pout=out/captures/s5-p3-fold-pushin-bare       (identical window)
./gw capture -Pscene=lane-fold      -Pout=out/captures/s5-p3-fold-bleed    -Pframes=36 -Pcols=6 -Pstart=8.60 -Pstep=0.0167 -Pw=960 -Ph=720
./gw capture -Pscene=lane-fold-bare -Pout=out/captures/s5-p3-fold-bleed-bare        (identical window)
./gw capture -Pscene=lane-knife     -Pout=out/captures/s5-p3-knife-plan    -Pframes=4  -Pcols=2 -Pstart=0.6  -Pstep=0.7    -Pw=960 -Ph=720
./gw capture -Pscene=lane-knife-bare    -Pout=out/captures/s5-p3-knife-plan-bare    (identical window)
./gw capture -Pscene=lane-approach      -Pout=out/captures/s5-p3-approach-plan      (same shape as knife)
./gw capture -Pscene=lane-approach-bare -Pout=out/captures/s5-p3-approach-plan-bare (identical window)

# the second shipped resolution, which is where the countability claim is proved
./gw capture -Pscene=lane-fold      -Pout=out/captures/s5-p3-fold-plan-540   -Pframes=6 -Pcols=3 -Pstart=0.4  -Pstep=0.5  -Pw=720 -Ph=540
./gw capture -Pscene=lane-fold-bare -Pout=out/captures/s5-p3-fold-plan-540-bare     (identical window)
./gw capture -Pscene=lane-fold      -Pout=out/captures/s5-p3-fold-replan-540 -Pframes=5 -Pcols=5 -Pstart=15.8 -Pstep=0.35 -Pw=720 -Ph=540
./gw capture -Pscene=lane-fold-bare -Pout=out/captures/s5-p3-fold-replan-540-bare   (identical window)
./gw capture -Pscene=lane-knife     -Pout=out/captures/s5-p3-knife-plan-540   -Pframes=4 -Pcols=2 -Pstart=0.6 -Pstep=0.7 -Pw=720 -Ph=540
./gw capture -Pscene=lane-knife-bare -Pout=out/captures/s5-p3-knife-plan-540-bare   (identical window)

./gw test --rerun-tasks
```

**Quote a `-bare` control beside every number about the interface.** The graded scene is
**`lane-fold`**.

---

## 10. Every guard, and how it was observed red

§11.2b(f): no assertion counts as a guard until it has been observed red — and a guard carrying
a broad claim owes the adversarial instance, and must run in every configuration the product
ships in.

| # | guard | broken by | red |
|---|---|---|---|
| A | `noTriangleInTheInterfaceIsAFlatFill` | `Brush.stroke`'s left rim given the spine's alpha | pass 1 |
| A2 | `noMarkPutsInkOnASilhouetteEdge` | pointed at `Guards.borderedPanel`: 2 inked silhouette edges | pass 2, and its **defeat** is asserted too |
| A3 | `noMarkPrintsAnEdgeInTheRasterItWouldDraw` | the same panel reads 1.000; **red this pass at 720×540 on pass 2's geometry (0.3563), and again on the re-authored `BACK_STEP` hook folding across itself (0.5479)** | ✔, at **both** shipped heights |
| A4 | `noMarkHoldsAFlatRunAcrossTheSheet` | **red on the pass-2 reviewer's own `Brush`-only hatch: 247 px against a 71 px ceiling**, and on the bordered panel at 216 px | ✔ *(new)* |
| B | `vermillionIsSpentOnlyOnTheTilesTheEngineSaysAreThreatened` | the lane's base pools drawn in `VERMILLION` | pass 1 |
| C | `theInterfaceRecedesContinuouslyBecauseTheCameraDoes` | `Readout.intimacy` driven off "is a beat running" | pass 1 |
| D | `theCameraReachesTheWideFramingInsideThePlanningGap` | `Scheduler.returnWide(idleAt)` reverted | pass 1; **red this pass** when the framing law landed and the guard still compared against the lane's framing |
| E | `theColumnReadsDownwardInTheOrderTheEngineResolves` | the height comparator reversed | pass 1 |
| F | `theMarkThatResolvesFirstIsDrawnHighestOnTheSheet` | comparator, and `STANZA_PITCH` negated | pass 1 |
| G | `theResolvingClauseIsTheStrongestMarkInTheColumn` | the flood reverted to a fade | pass 1 |
| H | `theColumnDriesDownwardFromTheMarkThatResolvesNext` | `QUEUED_DRYING` 0.13 → 0.00 | pass 1 |
| I | `anEmptyStanzaStillPrintsEveryLineOfItsColumn` | the empty-slot loop bounded at `written` | pass 1 |
| J | `noTwoTilesAreTheSamePicture` | observed red on pass 1's alphabet: `STEP−` vs `BACK_STEP+` at 0.000 | ✔ |
| J2 | `sweepIsTheOnlyTilePermittedToBeItsOwnMirror` | pointed at `TURN` with its spiral closed into a ring | ✔ *(new)* |
| K | `everyCountedMarkIsCountableAtEveryShippedResolution` | observed red on pass 1's geometry | ✔ |
| K2 | `everyChargeRunIsCountableOnTheSheetTheBoutDraws` | **written because an independent delivered reading disagreed with K**; red on the tick row placed inside the cartouche above it | ✔ *(new)* |
| L | `theStanzaOutReadsTheHealthRow` | observed red on pass 1's values | ✔ |
| M | `systemFoursSchedulesKeepTheirDurationsAcrossTheFramingLaw` | **red by pointing `pushIn()` at the new framing: `PHRASE` 6.9521 → 6.8410, `KNOCKBACK` 3.1548 → 3.0932** | ✔ *(new)* |
| N | `thePlanningFramingIsInsideItsOwnBandOnEveryLaneAndEveryExchange` | enumerated over every lane length and every two-body placement | ✔ *(new)* |
| O | `theGradedBoutsPlanAtASizeTheCorpusAsksAFigureToCarry` | **red on pass 2's framing: 0.107 of the frame against a corpus floor of 0.20** | ✔ *(new)* |

Known-answer rather than red-observed, and labelled as such: `everyTileTypeHasAMark`.
`theForbiddenThingIsCaughtByTheGuardsThatForbidIt` and
`theBrushOnlyHatchIsCaughtByTheFormGuardAndByNothingElse` are **exhibits**, not guards: they
assert what the two attacks do, including the parts they *defeat*, so that the findings cannot
rot.
