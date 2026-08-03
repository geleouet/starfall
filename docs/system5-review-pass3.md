# System 5 pass 3 — review

# FAIL

**And the headline is true.** The framing law landed, every one of its four published numbers
reproduces, and through an instrument the pass did not use the improvement is *larger* than
claimed: the hero's largest connected dark mass goes from **79 px to 183 px** (2.32×) and from
**1,861 px of ink to 9,676** (5.20×). Three of the project's seven systems have closed at their
cap without a pass; this is the best single pass I have read here, and the longest-standing
failure in the project moved for the first time.

It fails on three things, and two of them are on the same frame the headline is about.

1. **A Charted Shadow is not in the graded 15-tile planning shot.** `s5-p3-approach-plan`
   frame 0 shows two figures; the board has three. The framing law aims at *the nearest*
   Shadow only, so on `APPROACH` the shot spans tiles 0.75..7.25 and the second Shadow stands
   at tile 8. Enumerated over every legal three-body board, **7,530 of 10,890 (69.1%) put at
   least one Charted Shadow outside the planning shot**, worst case missing by 8.75 tiles. The
   pass's own guard for this, `thePlanningFramingIsInsideItsOwnBandOnEveryLaneAndEveryExchange`,
   enumerates **two-body** boards while every bout the game ships has **three** — §11.2b(f)'s
   *enumerate the axis, do not index it*, committed by the pass that quotes it. Under §9 as the
   owner amended it, this is a failure of the first item on the owner's own list of what the
   map must show.
2. **The adversarial exhibit succeeds, and it is the pass's own fixture with one number
   changed.** `Guards.hatchPanel` at α **0.12** instead of 0.45 draws a pale filled rectangle
   into every frame of every bout — 423×221 px with a visible boundary on all four sides, mean
   lift **+10.46** and peak **+21.35** luminance over 80,315 delivered pixels — and **all 413
   tests pass**. `Raster.inkBlock` returns **0**, because the panel's peak coverage 0.1632 sits
   under `FLAT_INK × amplitude` = 0.25 × 0.7809 = 0.1952. The frame is
   `out/captures/rev-p3-faintpanel/frame_000.png`, force-added. This is the third consecutive
   pass whose anti-chrome guard falls to the reviewer, and the first where it falls to the
   guard's own checked-in exhibit.
3. **The floor of the "band with both edges" is not stated in the units it is compared
   against.** 0.107 is *(the corpus sky's steepest step) / (the whole picture's amplitude)*;
   the guard compares it against *(the interface's steepest step) / (the interface's own
   amplitude)*. Made consistent in either direction the shipped interface is **already below
   the corpus floor** — 0.2157 against the corpus sky's 0.2579 in region-own units, or 0.1038
   against 0.107 in picture-amplitude units. §11.0's *"state the target as a band with both
   edges"* is answered with one live edge.

Reviewer's standing: I did not build this and have no stake in any decision in it. Every pixel
number below was taken with an independently written NumPy/PIL/SciPy reader using the Rec. 709
weights `Frame.java` declares (§11.2b(c)), never with `analyse`; every number is printed beside
the rectangle it was taken through (§11.3); every claim about the interface is a difference
against the `-bare` control shot in the identical window at the same harness
(`harness=f0ad18994eec`). Suite state at review: **413 tests, 0 failures, 0 skipped, 0 errors**,
verified by parsing `build/test-results/test/*.xml` after `./gw test --rerun-tasks`, not by
reading `BUILD SUCCESSFUL`. Tree clean and suite green as I leave it.

---

## 0. §11's required items, in order

**1. Verdict:** FAIL.

**2. The one-sentence test.** Family B (dusk duel) at execution; the planning framing is a
**map** by the owner's decision and is graded on legibility, not on §0 (§9).

- Execution framing (`s5-p3-fold-strike` frame 10): **yes, for the first time.** A near-black
  duellist 441 px tall with a sash, folds, a leg and a foot; a pale sliver blade; indigo-through-
  coral sky; hair dissolving into wisps; a fog bank across the ground. Crop the left 60% of that
  frame and it belongs beside image 3. What still betrays it is the health row in the top-left
  corner and the Charted Shadows' pale ward, which is nothing in the corpus.
- Planning framing (`s5-p3-fold-plan` frame 3): **legible on `FOLD` and `KNIFE`, not legible on
  `APPROACH`** — one of two enemies is off-frame there, and 8 of 15 lane tiles with it. §5.
- 540 rows: the world reads; the **hand does not** — §5.3.

**3. What is missing:** **technically broken** in the certification for the third pass running
(§3, §4), and one **legibility regression** introduced by the pass's own headline change (§5).
Not *"not evocative enough"* — that finding is discharged.

**4. Why** and **5. changes** are §§1–9 and the pass-4 brief in §10.

---

## 1. The four framing numbers

All four reproduce. Two of them are quoted through an instrument that saturates, and I say so;
neither correction moves the verdict, and one of them moves in the pass's favour.

### 1.1 Hero 87 px → 194 px — **reproduced, and understated**

The debt's detector: rows inside `x290..380 y180..515` holding ≥2 px more than 8 levels from
that row's own median. Run here:

| capture | box | threshold sweep 4/6/8/12/16 | height |
|---|---|---|---|
| `s5-p3-fold-plan` f3 | `x290..380 y180..515` | y320/322/323/323/323 .. **515** | 196/**194**/193/193/193 |
| `s5-p2-fold-plan` f3 | `x205..300 y180..515` | y429/429/429/429/430 .. **515** | **87**/87/87/87/86 |

**Both readings terminate at y=515, which is the box's own floor.** The number is therefore
`516 − top` in both columns: it measures where the head is, not how tall the figure is. Opened
to `y120..719` the same detector runs to 673 and 628 respectively — it cannot find a foot in
either frame, because the ground band deviates from its own row median as much as the hem does.

So I measured it a second way, with an instrument that shares nothing with the first: the
**largest connected component of pixels ≥10 levels below their own row median**, 8-connected.

| | box searched | component | height | area |
|---|---|---|---|---|
| `s5-p1-fold-plan` f3 | `x195..310 y400..545` | `x227..270 y429..507` | **79 px** | 2,169 |
| `s5-p2-fold-plan` f3 | `x195..310 y400..545` | `x231..270 y429..507` | **79 px** | 1,861 |
| `s5-p3-fold-plan` f3 | `x255..430 y280..560` | `x280..371 y323..505` | **183 px** | 9,676 |

**2.32× in height and 5.20× in area.** The claim of 2.23× is conservative. Two side notes: the
pass-1→pass-2 shrink the last review published as 87→77 does *not* appear through this
instrument — both read 79 px, and what pass 2 actually lost was **ink** (2,169 → 1,861, −14%),
not height; and the pass-3 figure's true foot is at y≈505–510, so the 194 includes about 8 px
of nothing below it while pass 2's 87 does not. That asymmetry flatters pass 2, not pass 3.

### 1.2 Matched-scale part count ~6 → ~13 — **reproduced**

Reference image 3 (`inspirations/image - 2026-08-02T101033.164.png`), dark duellist `y285..950`
= 665 px, downscaled by 194/665 = 0.29173 (LANCZOS), both viewed at 5× nearest-neighbour.

| | readable parts at ~194 px |
|---|---|
| **reference image 3** | topknot, hair mass, brow, nose, mouth/chin, ear, neck, collar, shoulder, upper arm, forearm, two hands, grip, guard, blade, sash, sash knot, second sheathed blade with its cord, skirt, fold structure, forward leg, rear leg, foot, dissolving hem, ground smear — **~22** (the debt counts ~16; it is undercounting its own reference) |
| **capture, pass 3** | hair mass, streaming hair wisps, head as a lump, shoulder, upper arm, forearm mass, torso, ochre sash band, second ochre mark, third ochre wedge, skirt mass, forward foot, blade, shed flecks — **~13** |

**~13 confirmed.** The gap is exactly the list §11.4 names as owed — face, hands, grip, guard,
second blade, sash knot, rear leg — and none of it is System 5's. The framing half is paid.

### 1.3 Bare planning frame 4.76% → 11.79%, interface/world 0.454 → 0.254 — **reproduced to the digit**

`>8` luminance from the pixel's own row median, whole frame `x0..959 y0..719`, frame 3, live vs
`-bare`. Interface = the share of the frame where `|lum(live) − lum(bare)| > 8`.

| | pass 1 | pass 2 | pass 3 | debt |
|---|---|---|---|---|
| bare planning frame | **4.84%** | **4.76%** | **11.79%** | 4.84 / 4.76 / 11.79 ✔ |
| the interface | **2.09%** | **2.16%** | **2.99%** | 2.09 / 2.16 / 2.99 ✔ |
| interface / world | **0.431** | **0.454** | **0.254** | 0.431 / 0.454 / 0.254 ✔ |

Exact. **The interface's share of what you can see fell by 44% while its absolute presence rose
by 38%** — the world grew faster than the margin did, which is the right shape for this result.

One attribution in the same paragraph does not hold. The debt says *"two thirds of that world
number is the fog bands and one third is the figures being twice the size."* Split by column on
the bare frames, the +48,526 px gain is **+37,557 inside the bodies' columns `x260..849`** and
**+10,969 outside them** (`x0..259` and `x850..959`). Split by row, the sky `y0..299` gains only
1,277 px of the 48,526. The fog does reach the whole frame — it is in both live and bare, which
is the important part and is correct — but it is not two thirds of the number, and on the
evidence of the columns it is closer to a quarter.

---

## 2. The cold read

### 2.1 Contamination, declared before the readings

I was told which three marks changed, and `system5-debt.md` §4 states **how each was
re-authored**: the retreat drawn *from the rear* so the mass sits behind the body; the thrust
given *a body the blade passes through*; the feint drawn as *one committed stroke and beside it
the same stroke offset and dry*. I read all of that before I read a pixel, because the brief
told me to read the debt. **My readings of `BACK_STEP`, `THRUST` and `FEINT` are therefore
aided, and I do not claim otherwise.** What is *not* aided is which slot each occupies, and what
the delivered pixels actually contain — a negative reading is uncontaminated even when a
positive one is not, and I report both.

To keep as much independence as there was to keep, I read the stanza on
**`s5-p3-approach-plan` frame 0**, a capture whose contents `system5-debt.md` states nowhere,
isolated every mark as `live − bare` against `s5-p3-approach-plan-bare` frame 0, and cropped at
9×–11×. Readings written down before opening `Glyph.java`.

### 2.2 What I read, and whether it was right

Stanza, `s5-p3-approach-plan` frame 0, box `x50..145`, top to base:

| pos | box | what I wrote, cold | truth | ✔ |
|---|---|---|---|---|
| 1 | `y50..120` | *two long shallow curves; one arrives from the upper left descending, runs **alongside** the other for the middle third, and leaves climbing to the upper right. No corner anywhere in it. A blade caught and turned* | `PARRY` | **✔** |
| 2 | `y135..190` | *two short level lens dashes with a gap, left larger and higher; and a long steep slightly-curved stroke descending **through the gap between them and continuing past below**. A line driven through something* | `THRUST` | **✔** (aided) |
| 3 | `y190..250` | *a large ~230° arc opening down-left, with a small detached crescent inside it on the left. A sweep through both sides* | `SWEEP` | **✔** |
| 4 | `y280..335` | *a large lens tilted up to the right with a fine tail at its upper-right tip, mass at the right end, and a smaller level lens below-left of it. A stride to the right* | `STEP` | **✔** |
| 5 | `y345..400` | *one heavy stroke broad at the upper left tapering to a fine point at the lower right, ~55–60°, with a small level dash off its right side. A committed descending cut* | `CUT` | **✔** |

Hand, same capture, box `x835..915`:

| box | what I wrote, cold | truth | ✔ |
|---|---|---|---|
| `y382..418` | *a near-closed ring, open at the upper right, of even weight all the way round. Something that comes back to where it started* | `TURN` | **✔** (unaided — the debt never mentions this bout's hand, and `TURN` is claimed byte-identical) |

And the two re-authored hand marks, `s5-p3-fold-plan` frame 3:

| box | what I wrote, cold | truth | ✔ |
|---|---|---|---|
| `x830..915 y360..430` | *a stroke entering from the right, thinning as it goes; at the left end it turns down hard and hooks back on itself, and the mass and the brightness are all in that turn. A heel planted behind and catching* | `BACK_STEP` | **✔** (aided) |
| `x830..915 y432..500` | *two lens strokes on the same rising diagonal, the lower-left one heavy and the upper-right one slenderer and fainter, offset so they read as one gesture that steps and does not follow through. A false start* | `FEINT` | **✔** (aided) |

**Seven of seven, and — the thing that matters most — no confident wrong reading anywhere.**

### 2.3 The three re-authored marks, graded

- **`BACK_STEP` — fixed, and the fix is real.** Pass 2's mark was *"a strike, aimed leftward.
  This is the enemy's intent"* — confident and wrong on the one axis a lane spends a tile on.
  The delivered mark now has no long dominant blade-like stroke at all: it is a hook whose mass
  sits at the left end and whose trace thins to the right. What I could not do, and will not
  pretend I did, is arrive at the word *retreat* from the mark alone: **cold, the shape says
  "hook", and it says "backward" only once you have `STEP` beside it** putting its mass at the
  opposite end. Beside `STEP` the opposition is unmissable — the two movement verbs are now the
  same gesture reflected, which is what the tile pair is. That is a pass, not a triumph.
- **`THRUST` — fixed, and the code comment overstates what was drawn.** `Glyph.java` now says
  *"the long level stroke is the blade, the steep stroke across it is what it passes
  through."* Delivered at the 71 px cartouche it is not one long level stroke crossed by a
  second: it is **two short level dashes with a gap, and the steep stroke passes through the
  gap** rather than through either dash, grazing them at the edges. The device pass 2's reviewer
  read as *"two footfalls"* is still there, unaltered — a third stroke was added beside it. It
  works anyway, because a thing passing *between* two marks is not something a movement does
  either; but the sentence in the comment describes a picture the raster does not contain, and
  the next reader should not be told the crossing is on the dashes.
- **`FEINT` — fixed, and by a wide margin.** Delivered on `s5-p3-fold-plan` frame 3 through
  `x835..905 y437..491`: **peak lift 57.43 at (877,469)**, 396 px above a lift of 4, against
  pass 2's **4.50 at (861,487)** with **6 px** above the same floor through `x835..900
  y450..492`. The faintest empty-slot impression on `s5-p3-fold-empty` frame 2 is **13.9**. The
  mark that was three times fainter than an empty slot is now 4.1× louder than one. Both
  published numbers reproduce exactly.
  One qualification for the record: `Glyph.java` calls the result *"two parallel strokes on a
  rising diagonal"*. They are offset 0.156 along their own direction and 0.149 across it, so at
  the delivered size they read as **one gesture with a jog in it**, not as two parallel marks.
  That is a better feint than the comment describes and a worse description than the mark
  deserves.

### 2.4 The byte-identity claim — **spot-checked and true**

`git diff 5af69ff 3a1e4b5 -- src/main/java/dev/starfall/ui/Glyph.java` touches exactly three
`MARKS.put` entries: `THRUST`, `BACK_STEP`, `FEINT`. `CUT`, `PARRY`, `SWEEP`, `STEP`, `DRAW`
and `TURN` have no hunk against them. `PARRY` — the protected result — is byte-identical, and I
read it correctly from its shape for the second review running.

### 2.5 The hand column, and why I cannot grade it

Three of the four changes are delivered and I confirm them: the charge run is centred under its
own tile and its outer end stops on the sheet at both heights; an ochre seal of the same mark
and height stands at the head of **both** margins (`x≈30` and `x≈878` of 960, `y≈15`); the
bodies are no longer against the right edge. The fourth is `HAND_INSET`, which is arithmetic.

**But I was told the right margin is the hand before I looked at it, so I cannot deliver the
cold read this item needs**, and I will not manufacture one. What I can report is a fact that
did not change: on `s5-p3-fold-plan` frame 3 the rightmost Charted Shadow's ink reaches
x≈850 and the hand's marks occupy x≈835..905 — **they overlap**. The column is no longer
*between* the two Shadows; it is now immediately beside, and partly behind, the nearer one. The
seal is doing the whole job of saying whose sheet this is. A fourth reviewer should be given a
capture with nothing said about which margin is which.

---

## 3. The raster guard

### 3.1 It runs at both shipped heights, and the message no longer lies — **all three fixes confirmed**

The guard iterates `Guards.SHIPPED_HEIGHTS`. Its own control lines, from the suite:

| bout | 960×720 | 720×540 |
|---|---|---|
| `KNIFE` | 0.2441 at (848,398), amp 0.7928 | **0.2825** at (641,357), amp 0.7833 |
| `FOLD` | 0.2157 at (63,102), amp 0.7809 | 0.2603 at (267,393), amp 0.7862 |
| `APPROACH` | 0.2158 at (63,102), amp 0.7817 | **0.2829** at (652,348), amp 0.7870 |

Every figure matches the debt's §2.1. To confirm the 540 arm is live rather than merely
written, I set `EDGE_CEILING` to 0.270 — between the 720 worst and the 540 worst — and ran:

```
InterfaceInkTest > noMarkPrintsAnEdgeInTheRasterItWouldDraw() FAILED
KNIFE at t=0.000: the interface's steepest one-pixel step is 0.2188, which is 0.2793 of the
0.7833 amplitude the interface reaches at its strongest, at (641,357) of 720x540, against a
ceiling of 0.2700 ...
```

**`720x540`, correctly, with the coordinates it actually measured through.** Pass 2's finding —
a guard that prints a region it did not measure through, inside the instrument that enforces
§11.3 — is closed. Reverted; suite green.

One claim beside it does not hold. §2.1 says *"three geometry changes brought it down and each
was found by the guard going red"*, the third being the `BACK_STEP` hook. **Putting pass 2's
`BACK_STEP` geometry back changes the guard's readings by 0.0000** — 0.2441 / 0.2157 / 0.2158
at 720 and 0.2825 / 0.2603 / 0.2829 at 540, identical to four decimals at every bout and both
heights. The hook fix repaired a regression this pass introduced; it did not contribute to
bringing 0.3563 down. Two changes did.

### 3.2 The ceiling is real, is reproducible, and is not 0.34 in new clothes

Measured on the Family B images with my own reader — steepest single-pixel step in either axis
over the region's amplitude, Rec. 709, whole frame less a 2 px margin:

| | region | denominator | mine | debt |
|---|---|---|---|---|
| image 3, native 832×1088 | whole frame less 2 px | own | **0.3771** at (575,301) | 0.3771 at (577,303) ✔ |
| image 4, native | same | own | **0.3986** at (430,459) | 0.3986 at (432,461) ✔ |
| image 5, native | same | own | **0.4352** at (439,459) | 0.4352 at (441,461) ✔ |
| image 3 matched to a 194 px figure | same | own | **0.3541** at (110,132) | 0.3542 ✔ |
| image 3/4/5 sky | `x40..790 y40..200` | **whole picture's** | **0.1101 / 0.1223 / 0.1075** | 0.1101 / 0.1223 / 0.1075 ✔ |
| image 3/4/5 garment+ground | `x40..790 y740..1080` | **whole picture's** | **0.3743 / 0.2581 / 0.3579** | 0.3743 / 0.2581 / 0.3579 ✔ |

(The two-pixel offsets in the locations are my margin, not a disagreement.) **The derivation is
real, not fitted, and it is genuinely a different construction from 0.34** — it lands on the
matched-scale reading of the corpus's softest blade edge, and it lands there independently of
the interface.

Two things the docstring should carry that it does not. The matched-scale 0.354 is
**resampler-dependent**: LANCZOS 0.3541, BOX 0.3624, BICUBIC 0.3394, BILINEAR 0.2959, NEAREST
0.5251. The kernel is as much a part of the region as the rectangle is (§11.3), and it is not
recorded. And the sky and ground rows are normalised by the **whole picture's** amplitude while
the whole-frame row is normalised by its own — which is fine for the ceiling, whose region *is*
the whole picture, and fatal for the floor.

### 3.3 The floor does not constrain anything

`EDGE_FLOOR = 0.107` is asserted against `worstShare = worst step / the interface's own
amplitude`. The corpus number it is taken from is `sky's step / the whole picture's amplitude`.
Those are two different statistics. Make them the same, either way:

| | corpus (images 3/4/5) | shipped interface | verdict |
|---|---|---|---|
| region's step ÷ **that region's own** amplitude | sky **0.2750 / 0.2579 / 0.2813** | **0.2157** (`FOLD`, 960×720), 0.2158 (`APPROACH`) | interface is **below** the softest sky |
| region's step ÷ **the whole picture's** amplitude | sky 0.1101 / 0.1223 / **0.1075** | **0.1038** (21.64 delivered levels ÷ the graded frame's own 208.57, `s5-p3-fold-plan` f3 less 8 px) | interface is **below** the floor |
| the guard's mixed form | sky ÷ picture = 0.107 | interface ÷ interface = 0.2157 | passes by 2.0× |

Only the mixed form passes. **The lower edge of the "band with both edges" is 2.4× looser than
its own derivation**, and it is loose in exactly the direction §11.0 warns about: *"a criterion
of floors alone rewards the defect it was written to catch… something will always score well by
running away."* This is that sentence one level in — the pass built the missing edge and built
it in a unit that cannot bite. The ceiling is sound; the band is still a ceiling.

### 3.4 Delivered pixels — **the whole of §2.3 reproduces**

`D = lum(live) − lum(bare)`, largest single-pixel `|∂D|` in either axis over every frame,
ignoring an 8 px border. Every absolute step and every location matches:

| capture | worst \|∂D\| | at | debt |
|---|---|---|---|
| `s5-p3-fold-plan` (6) | **21.64** | f5 (133,26) | 21.64, f5 (133,26) ✔ |
| `s5-p3-fold-strike` (12) | **22.04** | f7 (648,681) | 22.04, f7 (648,681) ✔ |
| `s5-p3-fold-replan` (5) | **21.44** | f1 (853,399) | 21.44, f1 (853,399) ✔ |
| `s5-p3-fold-empty` (4) | **25.09** | f3 (857,388) | 25.09 ✔ |
| `s5-p3-fold-pushin` (48 @60 Hz) | **28.02** | f13 (858,387) | 28.02 ✔ |
| `s5-p3-fold-bleed` (36 @60 Hz) | **14.94** | f35 (869,387) | 14.94 ✔ |
| `s5-p3-knife-plan` (4) | **22.71** | f0 (855,476) | 22.71 ✔ |
| `s5-p3-approach-plan` (4) | **21.62** | f3 (943,497) | 21.62 ✔ |
| `s5-p3-fold-plan-540` (6) | **27.85** | f1 (30,19) | 27.85 ✔ |
| `s5-p3-fold-replan-540` (5) | **27.78** | f1 (30,19) | 27.78 ✔ |
| `s5-p3-knife-plan-540` (4) | **29.21** | f0 (641,357) | 29.21 ✔ |

The graded frame's 0.2380 is better than pass 2's 0.244 and pass 1's 0.249. The honest
complication the pass raises itself — coverage and luminance stopped agreeing in ordering once
the framing tightened — is correct and correctly published.

---

## 4. The form criterion, and the exhibit that beats it

### 4.1 What it does catch — reproduced

`Raster.inkBlock` is the largest `min(horizontal run, vertical run)` over every pixel above
`FLAT_INK × amplitude`. The suite's own control lines:

| | 960×720 | 720×540 |
|---|---|---|
| interface, `KNIFE` / `FOLD` / `APPROACH` | 24 / 24 / 25 px | 19 / 19 / 19 px |
| ceiling (one cartouche) | 71 px | 53 px |
| pass-1 bordered panel | **216 px** | — |
| pass-2 `Brush`-only hatch | **247 px** | — |

Both attacks are caught, both are checked in as exhibits, and
`theBrushOnlyHatchIsCaughtByTheFormGuardAndByNothingElse` asserts the *defeat* as well as the
catch. That is the right pattern and it is better than §11.2b(f) asks for.

Two notes on the instrument, neither fatal. `min(h_run, v_run)` at a pixel is not the side of
an inscribed square — a cross of two long thin strokes reports the length of the shorter arm
with no square present anywhere — so the statistic over-reports, which is the safe direction
for a ban but means the interface's own 24–25 px is an upper bound rather than a measurement.
And a run is broken by a **single** sub-floor pixel, which is the door §4.2 walks through.

### 4.2 §11.2b(f)'s adversarial instance — **it succeeds**

*"Try to build the thing the guard forbids while satisfying it."*

I did not need a new construction. `Guards.hatchPanel(sink, 0.080f, 0.020f, α)` — the pass's own
checked-in fixture — swept over α through the guard's own raster at 960×720 against the `FOLD`
amplitude of 0.7809:

| α | peak coverage | steepest step / amp | **inkBlock** | flat-fill tris | inked silhouettes |
|---|---|---|---|---|---|
| 0.450 (the fixture) | 0.5323 | 0.0353 | **239** | 0 | 0 |
| 0.200 | 0.2632 | 0.0169 | **183** | 0 | 0 |
| **0.120** | **0.1632** | **0.0103** | **0** | **0** | **0** |
| 0.080 | 0.1106 | 0.0070 | 0 | 0 | 0 |

At α ≤ 0.12 the panel's peak coverage falls under `FLAT_INK × amplitude` = 0.1952 and the form
guard **finds nothing at all**. Drawn into `LaneInterface.sheet` at α = 0.12, on every frame of
every bout:

```
./gw test --rerun-tasks   →  BUILD SUCCESSFUL
tests 413  failures 0  skipped 0  errors 0
```

Delivered, `out/captures/rev-p3-faintpanel/frame_000.png` against `s5-p3-fold-plan` frame 0 at
the identical window `-Pstart=0.4 -Pw=960 -Ph=720`: **80,315 pixels changed by more than 4
levels (11.6% of the frame)**; inside the panel's box `x378..800 y70..290` the mean lift is
**+10.46** and the peak **+21.35**; the steepest single-pixel step inside it is **3.72**, which
is a sixth of the interface's own worst. It is a pale rectangle in the sky with a visible
boundary on all four sides, and the suite has nothing to say about it.

**The finding is the guard's scope.** Its name — `noMarkHoldsAFlatRunAcrossTheSheet` — and
§10's new row both promise a property of *inked regions*, and "inked" is defined by a tunable
the attacker controls, because the denominator is the *bout's* amplitude and not the panel's.
A rectangle does not have to be dark to be chrome. Two cheap fixes, and they are not exclusive:
measure `inkBlock` at a ladder of floors (0.05, 0.10, 0.25 of amplitude) and cap the largest;
or normalise each connected region by **its own** peak rather than by the sheet's, which is
what makes a faint panel still a panel.

For the record, the pass's own stated scope — *"a filled rectangle smaller than one cartouche
would pass it"* — is honest and is **not** the hole I used. My panel is 423×221 px.

### 4.3 The first refutation of the brief — **upheld, and more strongly than the pass claimed**

Pass 3 says the review's literal criterion (*"no axis-aligned run of near-constant coverage
longer than N px, on either axis"*) convicts the interface's own lane wash and leaves a 1.8×
margin over the hatch. I built it independently — longest axis-aligned run over which
consecutive pixels differ by ≤ 0.06 of amplitude and coverage exceeds 0.02 — and swept every
state of every bout at 960×720:

| | longest near-constant run |
|---|---|
| interface, `KNIFE` | **944 px** horizontal at (8,533), t=4.000 |
| interface, `FOLD` | 876 px horizontal at (34,533), t=3.700 |
| interface, `APPROACH` | 567 px horizontal at (43,538), t=3.800 |
| the `Brush`-only hatch panel | **409 px** horizontal at (370,201) |

Under my reading of the same words the interface is **2.3× worse than the forbidden panel**.
The debt's own reading gives 129 px against 23 px. That the two readings differ by 7× is itself
the point: *"near-constant run"* is not a definition, and every way of making it one either
convicts a wash — which §8 asks for by name — or leaves a margin a hatch closes by rippling
harder. **Thickness in both axes is a better idea and the pass was right to refuse the
instruction.** This is the correct response to a brief, and it is the second time in this
system that refusing one has been the right call.

### 4.4 The second refutation — 87 px not 77 — **upheld, with the instrument's fault named**

One instrument applied to both is the right principle, the pass applied it, and it chose the
reading **less** flattering to itself: 87 makes the ratio 2.23× where 77 would have made it
2.52×. Upheld. What the pass did not notice is that the instrument saturates at its box floor
on both frames (§1.1), so the quantity is head position and not figure height. Through a
detector with no shared assumption the ratio is 2.32×. The refutation stands and the number
should be restated as *"2.2–2.3× on any instrument"*.

---

## 5. The map, graded on legibility (§9)

The owner's decision is the specification and I have not graded this frame on emptiness, on
sky fraction, or on whether it is a picture. §9's question, item by item, at 1×.

### 5.1 `FOLD` (11 tiles) and `KNIFE` (5 tiles) — legible

Both figures' positions: yes, all three bodies in frame. Threatened tiles: two vermillion
washes at `y≈515..528`, unmistakable. Reachable tiles: the pale lens marks along `y≈530..550`
separate cleanly — I count 7–8 across the frame. The enemy's telegraph: visible and warm
against a cool ground. The health row: **ten strokes, countable at a glance** through
`x36..254 y10..44`. This is a better map than pass 2's, because the pieces are bigger.

**What it costs, stated:** `FOLD` frames 6.5 of 11 tiles. **4.5 tiles of the lane — 41% of the
board — are off-frame**, where pass 2 showed all of it. On `KNIFE` nothing is lost.

### 5.2 `APPROACH` (15 tiles) — **not legible, and this is the pass's own regression**

`s5-p3-approach-plan` frame 0 contains **two figures**. The board has three: hero at tile 3,
Shadows at 5 and 8. The shot is 6.50 tiles spanning 0.75..7.25, so the Shadow at tile 8 is
outside it, and 8 of 15 lane tiles with it. Pass 2's `s5-p2-approach-plan` frame 0 shows all
three and the whole lane.

`Stage.planning(Standing)` takes the span from **the nearest** body only. Enumerated over every
legal board with a hero and two Shadows, lanes 5 to 15:

```
7,530 of 10,890 boards (69.1%) put at least one Charted Shadow outside the planning shot.
Worst: lane 15, hero@0, Shadows@1 and 14 -> shot -1.25..5.25 (6.50 tiles), missing by 8.75 tiles.
```

`Scheduler` then **holds** that framing for the whole score — correctly reasoned, and it means
the missing Shadow stays missing until it walks in.

The guard written for this, `thePlanningFramingIsInsideItsOwnBandOnEveryLaneAndEveryExchange`,
asserts `f.left() ≤ min(hero,enemy)` and `f.right() ≥ max(hero,enemy)` over **two-body** boards
on every lane length. Every bout the game ships has three bodies. **A guard that enumerates one
axis exhaustively and indexes another is still indexing** — this is §11.2b(f)'s fourth clause,
and the pass quotes that clause four lines above the assertion.

**And the fix is nearly free.** Widen the span to hold *every* body rather than the nearest:
on `APPROACH` that is |8−3| = 5 tiles, so `width = max(6.5, 5 + 2.5) = 7.5` and the hero sits at
**0.195 of the frame, 141 px of 720** — against 162 px today and 64 px under the lane's own
framing. Twenty-one pixels of figure for the whole board. That trade should not have been
made silently and I do not think the owner would have made it.

### 5.3 720×540 — the world reads, the hand does not

`s5-p3-fold-plan-540` frame 3: hero, both Shadows, blades, lane band, both vermillion washes,
health row — all legible. The **hand** is not: through `x560..719 y60..420` the only mark a
reader finds is the `BACK_STEP` hook and a row of faint ochre ticks. The debt measures why and
declines to decide (*"the hand's live tiles read at about half the stanza's top mark — 47.6 and
57.4 against 90.6 — and most of the hand is banked ghosts at alpha 0.17… a reviewer may want
them louder. Measured, not decided."*).

**Decided: louder.** Under the map doctrine the hand *is* the list of available actions, which
is half of what a planning frame is for. It cannot be the faintest thing on the sheet at the
resolution the game also ships. Note the direction of travel: `BACK_STEP`'s delivered peak fell
from **77.8 to 47.6** when it was re-authored — the retreat became correct and quiet at the same
time.

---

## 6. Every protected result, checked

| result | region | debt | mine |
|---|---|---|---|
| Hard edge, `fold-plan` | whole frame less 8 px, 6 frames | 21.64 / 90.91 = 0.2380 | **21.64 at f5 (133,26)** ✔ |
| Hard edge, `fold-strike` | 12 frames | 22.04, 0.2424 vs strongest | **22.04 at f7 (648,681)** ✔ |
| Determinism | `fold-plan` ×6, `fold-strike` ×12 vs `-repro` | 0 px | **0 px, peak delta 0** ✔ |
| Empty state | five slot boxes, `fold-empty` f2, `x62..132` | 24.9 / 13.9 / 14.3 / 17.8 / 20.0 | **identical** ✔ |
| Vermillion budget | `r−b` lift > 20, frame 3, whole frame | 3,484 px = 0.504%, live (150,66,64) / bare (51,54,65) at (340,527) | **3,484 px = 0.504%, identical colours and pixel** ✔ |
| — pass 2, same statistic | same | 1,923 px = 0.278% | **1,923 px = 0.278%** ✔ |
| Corpus ceiling and floor | see §3.2 | six figures | **all six exact** ✔ |
| Corpus matched-scale | 194 px figure | 0.3542 | **0.3541 (LANCZOS)** ✔ |
| Glyph mirrors | 71 px cartouche, guard's own path | eight of nine | **eight of nine** — `BACK_STEP` does not, §7 |
| System 4 durations | headless | 6.9521 / 3.0400 / 3.1548 | **6.9521 / 3.0400 / 3.1548** ✔ |

The determinism control deserves a word, because §11.2b(g) is about exactly this: both captures
re-shot here are **moving** scenes — fog drifting, camera drifting, marks drying — so 0 of
4,147,200 pixels differing over 18 frames certifies the simulation and not only the readback.
That is the control the section asks for, and it is clean.

---

## 7. Claims in `system5-debt.md` that do not reproduce

1. **§1.3, the *"bare execution frame"* row — three different frames presented as one.** The
   table's header says *"whole frame, frame 3"*. The planning row is frame 3 in all three
   columns and reproduces exactly. The execution row is not:

   | | f0 | f1 | f2 | **f3** | f4 | f5 | f6 | … |
   |---|---|---|---|---|---|---|---|---|
   | pass 1 | 9.82 | 16.48 | 17.83 | **17.26** | **16.61** | 14.73 | 14.74 | … |
   | pass 2 | 9.73 | 16.48 | 17.78 | **17.20** | 16.58 | 14.70 | **14.71** | … |
   | pass 3 | 15.71 | 17.14 | 17.85 | **17.26** | 16.73 | 15.50 | 15.85 | … |

   The published **16.61 / 14.71 / 17.26** is pass 1 frame **4**, pass 2 frame **6**, pass 3
   frame **3**. All three captures share `frames=12 start=3.3 step=0.42 window=4.62 size=960x720
   harness=f0ad18994eec`, so the indices are comparable and the row did not have to hop. **At a
   matched frame the execution frame reads 17.26 / 17.20 / 17.26 — it did not move at all.**
   §11.3 committed inside the pass's own headline table, and it flatters pass 3 by +17% on a
   quantity that changed by 0.3%.

2. **§4.4's glyph mirror table — `BACK_STEP 0.9405`.** The test that prints that table prints
   **0.9353**:
   `CUT 0.8558  THRUST 0.6639  PARRY 0.8471  SWEEP 0.1543  DRAW 0.9062  STEP 0.9427
   BACK_STEP 0.9353  TURN 0.7929  FEINT 0.9960`. Eight of nine match; the ninth is again the
   mark the pass re-authored, which is exactly what pass 2's review found in the same table for
   the same reason. Stale by a later edit. Immaterial to any verdict — it clears the 0.20 floor
   by 4.7× — but a published number the suite contradicts is the cheapest kind of rot.

3. **§2.1's *"three geometry changes brought it down."*** Restoring pass 2's `BACK_STEP`
   geometry moves the raster guard's readings by **0.0000** at every bout and both heights
   (§3.1). Two changes brought it down; the third repaired a regression this pass introduced.

4. **§1.3's *"two thirds of that world number is the fog bands and one third is the
   figures."*** Split by column on the bare frames, 77% of the +48,526 px gain is inside the
   bodies' columns `x260..849` and 23% outside them; split by row, the sky `y0..299` contributes
   1,277 px of 48,526 (§1.3). The attribution is not supported by the statistic it is attached
   to.

**Named misses, confirmed as declared:**

- **The draw-order result is genuinely not reproducible in its published form.** On
  `s5-p3-fold-strike` frame 4, band `y560..699`, a dark-pixel mask at 25 levels below the band's
  median selects **1,114 px on pass 2 and 0 px on pass 3** — the bodies are not there any more.
  Mean `|D|` over the whole band is 2.77 (pass 2) and 3.29 (pass 3), both dominated by the
  lane's own wash. The pass's account is exact and its substitute instance — a charge tick under
  a Charted Shadow reading bit-identical to the bare control — is the right kind of evidence.
  **Correctly declared unpaid.**
- **A cooldown-0 row still shows ink.** Reading `s5-p3-fold-replan` frames 2 and 4 through
  `x800..919` in a ±4 px band at the hand's second row, my reader returns runs at a peak lift of
  **9.3** where a cooldown of 0 should leave nothing. The suite's guard differences against a
  control and correctly returns 0; a player has no control. **Correctly declared.**
- **Vermillion 0.278% → 0.504%** of a frame half as wide: reproduced to the pixel. The marks are
  unchanged; the share doubled because the frame narrowed. §2.2's *"a few small marks per frame"*
  is being leaned on and the pass says so.

---

## 8. The cross-system consequence — verified

| | duration | framing at t=0 | lane's own `planning()` |
|---|---|---|---|
| `duel-phrase` (7 tiles) | **6.9521 s** | **6.5000 tiles centred 2.0000** | 8.5000 centred 3.0000 |
| `duel-parry` (5 tiles) | **3.0400 s** | **6.5000 tiles centred 2.0000** | 6.5000 centred 2.0000 |
| `duel-knockback` (7 tiles) | **3.1548 s** | **6.5000 tiles centred 2.5000** | 8.5000 centred 3.0000 |

- **`duel-parry` is untouched**: its lane's own framing already *was* the floor, so
  `planning(Standing)` returns the identical shot. Confirmed.
- **`duel-phrase` and `duel-knockback` moved 8.50 → 6.50 tiles.** The debt reports the width;
  it does not report that **the centre moved too** — 3.00 → 2.00 and 3.00 → 2.50. The figures in
  those shots are both larger *and* differently placed, so §11.2b(d)'s voiding is if anything
  understated. Any archived `s4-*` wide frame is void; correctly declared.
- **The pinning is real.** `systemFoursSchedulesKeepTheirDurationsAcrossTheFramingLaw` asserts
  all three fingerprints to 5e-5 and they hold at HEAD. The red-observation it claims — pointing
  `pushIn()` at the new framing takes `PHRASE` to 6.8410 — is consistent with the arithmetic:
  the duration law reads `planning()` and the new framing is narrower, so the push-in has less
  distance and gets less time. I did not re-break it; the assertion is not vacuous, because it
  compares against constants that share no code with the thing under test.

---

## 9. What this pass got right, stated plainly

- **The framing law is the largest single result this project has produced.** Three reviews
  named it and two declined it; this one built it, floored it on a number derived from the lane
  law rather than chosen, capped it so it can only tighten, and guarded both edges over every
  lane length. Hero 79 → 183 px, ink ×5.2, part count 6 → 13.
- **Fog as a positive element, in both live and bare**, so every `live − bare` number still
  measures the interface. That is the structurally correct answer to §6 and it is the reason
  the world statistic can be trusted at all.
- **All three re-authored marks read.** `FEINT` went from unfindable to 4.1× an empty slot's
  impression; `BACK_STEP`'s confident wrong reading is gone; `THRUST` no longer reads as a
  movement verb. Six marks byte-identical, verified by diff — that is the right way to spend a
  cold-read budget and I want it said, because the temptation is always to touch more.
- **The guard's ceiling is the first threshold in this system derived from the corpus rather
  than from the forbidden thing**, and every figure in the derivation reproduces exactly.
- **Both attacks are checked in as exhibits, including the one that defeats a guard.** The
  pattern pass 2 started is now three deep and it works: I could start from `Guards.hatchPanel`
  and have a working attack in one sweep instead of a day.
- **The pass refuted its own brief twice and was right both times**, and published the numbers
  that refute it.
- **It declared what it did not pay**, including one result it could not re-measure at all, and
  did not quietly substitute a different statistic for it.

---

## 10. Pass-4 brief

*Per §0.1, this brief opens with the three lines it is required to open with.*

**1. The current §0 answer.** *Execution framing:* yes — `s5-p3-fold-strike` frame 10 would
survive a crop out of image 3 but for the health row in the corner and the Charted Shadows' pale
ward. *Planning framing:* not applicable; it is a map and its answer is **legible on two of
three lanes and illegible on the third**, because an enemy is missing from it.

**2. The part-count target.** Duel framing, §11.4, ≥18 per duellist. Measured here at matched
scale on `s5-p3-fold-strike` frame 10: **hero ~18 at a generous count, Charted Shadow ~14**,
against reference image 3's ~22 at the same scale. **From 18 to 20 on the hero and from 14 to 18
on the Shadow**, and the fittings are named: *the near hand at the grip* (the hero's hilt is a
dark knot, not a hand), *the guard*, *the rear leg* (the hero shows one leg; the Shadow shows
two), *the sash knot*. The pass-2 review assigned the figure half to System 4 and §11.4 postdates
that; **if the owner holds §11.4 against System 5, this is a rig pass and it needs a rig owner.
If not, say so in §11.4 and stop counting it here.** What System 5 owes regardless is that it
never ran the count at the framing the floor applies to — the debt runs §11.0 only on the
planning frame, which §11.4 exempts by name.

**3. The one change visible at 1×.** **The second Charted Shadow appears in the 15-tile planning
shot.** Open `s5-p3-approach-plan` frame 0 today and count two figures; open it after pass 4 and
count three.

Ranked. Two of five passes remain.

1. **Frame every body, not the nearest one.** `Stage.planning(Standing)` takes its span from
   `min` and `max` over *all* bodies, still floored at `SHORTEST_PLANNING` and capped at the
   lane's. Cost on the graded bouts: `APPROACH` 6.50 → 7.50 tiles, hero 162 → 141 px, still
   2.2× pass 2's. Then **widen the existing guard to the boards the game ships**: enumerate
   three-body placements, not two, and assert every body is inside the shot. It is red today —
   69.1% of legal boards. This is the §0 item and it is an afternoon.
   *If the owner would rather keep the tighter shot than show the far Shadow, that is a decision
   and not a defect — but it has to be taken deliberately and written into §9, because as it
   stands the code has taken it silently.*
2. **Close the faint-panel hole, or narrow the claim in §10 to what is true.** The exhibit is
   `Guards.hatchPanel(sink, 0.080f, 0.020f, 0.12f)` and the frame is
   `out/captures/rev-p3-faintpanel/frame_000.png`; check both in as fixtures, exactly as this
   pass did with mine. The two cheap criteria are in §4.2: an `inkBlock` ladder over several ink
   floors, or normalising each connected region by its own peak. Whichever is chosen,
   **§10's new row must say that a region fainter than a quarter of the sheet's amplitude is not
   examined at all** — that sentence is the finding even if the guard is fixed.
3. **Restate the floor in the guard's own units, or delete it.** §3.3. Both self-consistent
   readings put the shipped interface below the corpus; a floor that only passes because its two
   sides are normalised differently is worse than no floor, because it certifies. If the honest
   corpus floor is 0.258, then either the interface has to get harder or the claim has to become
   *"the corpus's sky is harder than our interface and we accept that"* — in the rubric, in
   writing.
4. **Make the hand loud enough to plan from at 540 rows.** §5.3. The pass measured the case and
   handed the decision to a reviewer; the decision is *louder*. The cost is the hard-edge
   numbers of §2.3 and there is room — the shipped worst is 0.2829 against a ceiling of 0.354.
   Raise the live tiles toward the stanza's 90.6 and re-run both guards at both heights.
5. **Correct the four items in §7**, particularly the execution-frame row: publish it at a
   matched frame index and let it say that the execution frame did not move.
6. **Give the hand column one uncontaminated cold read.** Hand a reviewer a capture and say
   nothing about which margin is which. Two reviews have now been told, and the composition
   question pass 2 raised — *whose sheet is the right margin?* — is still unanswered by anyone
   who did not already know.
7. **Fix the two code comments that describe pictures the raster does not contain**:
   `THRUST`'s "steep stroke across the long level stroke" (it passes through the gap between two
   short dashes) and `FEINT`'s "two parallel strokes" (they are offset along their own direction
   as much as across it and read as one jogged gesture). §2.3.

### What I would accept as permanent debt

Named so pass 4 does not spend itself on it. I keep all six of pass 2's, and add:

- **The figure's own articulation — face, hands, grip, guard, second blade, sash knot, rear
  leg.** System 4 is closed at five passes and System 5 has now paid its half of §11.0 twice
  over. Do not fail System 5 for it again. **But it is now an acceptance in §11.4 with a number
  on it, so somebody has to own it; naming System 5 the owner by default is how it stayed
  unowned for seven systems.**
- **A general automatic detector of rectangular composition.** Still nobody has one. The
  ladder in item 2 is the cheap 90% and is not this.
- **`Raster.inkBlock` over-reporting on crossings.** `min(h,v)` is not an inscribed square. It
  errs safe and the interface's 24–25 px is an upper bound. Not worth a pass.
- **`commit=<sha>-dirty` cannot distinguish two passes.** `s5-p1-*` and `s5-p2-*` both record
  `commit=1564fcf-dirty`, so §11.2b(d)'s protection is blind between them. Real, and cheaper to
  live with than to fix.
- **Frames are not published.** The commit carries `capture.txt` and `contact-sheet.png` but no
  `frame_*.png` except where force-added. Every measurement in this document and in the debt is
  reproducible only on a machine that has the frames. `check-progress.mjs` guards the frames
  *tests* need; nothing guards the frames *reviews* need. Accept it, or force-add the graded
  frame of each capture — not the whole sweep.
- **`LaneScene` duplicating `DuelScene`'s render body, and `Opaque` being public.** Unchanged.

*Not* acceptable as permanent debt with two passes left: a planning shot that hides an enemy,
and an anti-chrome guard that a rectangle walks through by getting paler.

---

*Reviewed against `STYLE.md` rev. 1 (§0, §0.1, §1, §3, §6, §8, §9, §10, §11.4) and
`MEASUREMENT.md` (§11.0, §11.2, §11.2b(c)(d)(f)(g), §11.3), `docs/combat-design.md` §2.2, §3,
§3d, and the eight images in `inspirations/`. Captures read: all 24 `s5-p3-*` directories, all
19 `s5-p2-*`, `s5-p1-fold-plan`, `s5-p1-fold-strike`, and one shot for this review
(`rev-p3-faintpanel`, force-added as the exhibit). Source edits made and reverted:
`LaneInterface.java` (twice), `Glyph.java`, `InterfaceInkTest.java`; four review-only probe
classes written and deleted. Tree clean and suite green at 413 tests / 0 failures / 0 skipped /
0 errors as I leave it.*
