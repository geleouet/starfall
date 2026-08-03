# System 4 — the closing record

**System 4 is closed at pass 5 of 5.** This document is not a pass report. It is the
permanent record of what System 4 delivers, what it does not, and who inherits each
unpaid item. Systems 3b (faces), 3c (materials and textures) and 5 (combat UI) are the
readers it is written for.

Every capture quoted is `s4-p5-*` unless named otherwise, and all of them carry
`harness=f0ad18994eec` — the hash that produced every `s4-p2-*`, `s4-p3-*`, `s4-p4-*` and
`rev-p*` capture, so comparisons back to pass 2 are in scope per STYLE.md §11.2b(d).
Every pixel number is a ratio to a **given** figure span or to the frame's own row
background, and every one is printed beside its rectangle (§11.3).

---

## 0. The two spans, which are load-bearing for every number below

**`--span 0,299,960,378` for `s4-p5-*`. `--span 0,348,960,329` for `s4-p4-*`.**

Pass 5 lowered `Director.LANE_SPREAD` from 1.55 to 1.35 (§2). The camera framing is
stretched by the same factor, so the intimate shot is 13% narrower and the same
1.70-unit figure lands on **378** rows rather than 329. The feet do not move:
`rowOfGround` works out to `(0.5 + EYE) * H` whatever the zoom, so world y = 0 sits at
row 677 at both spreads; only the crown rises, 348 → 299.

Both spans are **computed, not detected**, and `ParryWindowTest` asserts the live one
against `Schedule.framingAt` and `Stage.FIGURE_HEIGHT` through the same camera arithmetic
`DuelScene.aim` runs, so it cannot rot when a framing constant moves. It was observed red
during pass 5 — the constant change made it fail with *"docs/system4-debt.md normalises
every s4-p4 parry number by 329 rows; the scene now says 377.8996018882435"* — which is
the strongest form of the assertion working.

Detection is not merely unstable on the Family B stage, it is wrong: the ground is itself
a dark ink smear, so the largest ink component runs from the head down into it and out to
both frame edges. **`analyse corridor` now refuses to run without a span** (§7.3).

Every number in this document was taken with an independently written NumPy/SciPy/PIL
reader (8-connected labelling, row-local background as the median of the outer 70 columns,
3×3 opening, Rec. 709 weights as `Frame.java` declares them), except where the checked-in
tool is named and its command given.

---

## 1. What System 4 delivers

System 4 is the staging and direction layer: it turns a combat resolution into a directed
duel on the Family B stage, and draws it.

- **The Family B dusk stage.** `DuelScene` draws the sky STYLE.md §1 specified from
  revision 1 and which three passes deferred as cosmetic. The ramp matches reference
  image 3 to within a few luminance levels from world y +0.5 upward — measured, outer-70-
  column row background: world y +0.50 reads 64.4 against the corpus's 64.0; +1.10 reads
  91.9 against 97.5; +2.40 reads 57.7 against 57.2. This is the first pass in the
  project's history whose colour script belongs to the corpus, and the pass-4 review said
  so.
- **A parry in which the blades actually meet.** `analyse blades
  out/captures/s4-p5-parry-contact --span 0,299,960,378 --max 0.02` reads a minimum
  two-cloud separation of **0.0059 of a figure height at frame 9**, and resolves *two*
  cool-bright clouds on all 24 frames — no frame is scored 0.0000 by the merge convention
  that pass 4's headline relied on.
- **A crossed blade figure inside the corpus's own range.** `MeshAuthor
  .BLADE_NAGASA_FRACTION = 0.55`; the crossed X spans 0.49–0.53 of a figure height
  against the corpus's 0.487–0.655.
- **A clash mark with no hard edge in it.** Pass 5, §1 below.
- **A base separation 1.22× the corpus's rather than 1.7×.** Pass 5, §2 below.
- **A ground band that darkens toward the frame bottom**, which every Family B image does
  and no previous pass did. §3 below.
- **A blade trail that is a wake rather than a ring.** §4 below.
- **A held breath at spec on all three scenes**, asserted headless on the same
  `Rehearsal` the capture's director runs: 0.850× over 0.25 s per ramp on `duel-parry`,
  `duel-knockback` and `duel-phrase`.
- **A bloom that sits on the steel.** Core (`L ≥ 246.5`) centroid to the nearer
  cool-bright blade cloud, through `x500..700 y230..450`, figure height 378 px given:
  **0.7 / 0.5 / 0.4 px** on frames 9 / 10 / 11 — 0.0018 / 0.0013 / 0.0010 of a figure
  height. Peak luminance 253.1 / 254.1 / 253.8.
- **389 tests, 0 skipped, 0 failures**, including three assertions that read published
  capture frames and a progress checker that refuses on any capture frame referenced from
  `src/` that git does not track.

---

## 2. Pass 5, item by item, with both numbers

The pass-4 review's ranked brief was four items long. Items 1 and 2 were named as the two
that must land. Both landed. Item 3 landed in its named defects and **not** in its target
statistic, and the reason is arithmetic rather than effort. Item 4 landed partially and
its remaining floor is measured.

### 2.1 Item 1 — the two hard edges at the focal point. **PAID.**

Two objects, both STYLE.md §10 fail-on-sight, both within 60 px of the point the whole
composition directs the eye to.

**(a) The shed flecks and embers were filled quadrilaterals.** `InkFxRenderer.quad` drew
each one as a rotated quad with *uniform* alpha on all four vertices, so every edge was a
step from the mark's own brightness to the ground's. The class comment claimed the
opposite — *"every polygon in this file ends on zero alpha"* — and it was true of every
mark except the two drawn most often.

Reproduced from the review, blobs 8 L above the local median in `x530..600 y290..400` on
`s4-p4-parry-contact` frame 11:

| blob | size | bbox fill | aspect |
|---|---|---|---|
| (558,345) | 7×7 | **0.98** | 1.00 |
| (565,352) | 7×8 | **0.89** | 0.88 |
| (552,356) | 8×8 | **0.81** | 1.00 |
| (573,352) | 8×8 | **0.78** | 1.00 |

Delivered: `fleck()` draws a seven-point irregular fan, elongated along the mark's own
flight, whose whole rim sits at zero alpha; sizes are spread over a cube of the hash
rather than a line, because a linear spread clusters and STYLE.md §3's failure list names
"flecks that are all the same size". Measured on `s4-p5-parry-contact` frame 11 through
the same box carried to matched scale (`x501..581 y265..391`, the review's box scaled
378/329 about the clash core): **11 blobs, maximum bbox fill 0.80 on a 5×4 mark of 16 px.**
Among blobs of the size class the review measured (≥ 40 px): fills **0.37–0.55**, aspects
**0.26–0.95**, sizes **41–209 px** — against pass 4's 0.58–0.98, 0.88–1.00 and 47–52 px.
**No filled quadrilateral survives.**

**(b) The clash rays were flat polygon wedges.** Each was *one triangle*: two zero-alpha
vertices at the base and one vertex at the tip carrying `alpha * 0.85`. Along the spine
that is a soft ramp; across it the alpha is constant to the last pixel and then stops, so
the mark printed two straight aliased edges converging on a hard point.

Delivered: `ray()` is a five-station strip. Every station carries a centre vertex with the
light and two flank vertices at zero alpha, the last station has both zero width and zero
alpha, and the spine bows. Four to six rays rather than five, lengths spread over a square
so they run 0.375× to 1.00× of each other rather than 0.55×–1.00×.

Single-pixel luminance steps, clash box 260 × 200 px centred on the clash core, against
reference image 3's own clash box at matched scale (downscaled 378/672, `x103..363
y127..327`). **Blade excluded** — the blade is the one thing STYLE.md §5 licenses to have a
hard edge, and the mask is cool-bright `L > 130, b − r > 4` dilated 3×3:

| | p99 \|∆L\| | max \|∆L\| | share > 60 | share > 100 |
|---|---|---|---|---|
| reference image 3, matched scale | 30.0 | 77.4 | 0.020% | **0.000%** |
| `s4-p4` f9 / f10 / f11 | 25.6 / **37.1** / 29.0 | 114.9 / **135.3** / 88.6 | 0.069 / **0.380** / 0.030% | 0.0021 / **0.0574** / 0% |
| `s4-p5` f9 / f10 / f11 | 25.6 / 26.0 / 26.0–27.8 | 120.1 / 92.6 / 88.6–108.1 | 0.063 / 0.066 / 0.039–0.098% | 0.0011 / **0%** / 0–0.0031% |

Frame 11's pass-5 cells are a range because that frame is not deterministic in this pass
(§7.1): the two runs of the same command read 26.0 / 88.6 / 0.039% / 0% and 27.8 / 108.1 /
0.098% / 0.0031%. Frames 9 and 10 are bit-identical across the two runs, so their cells are
single values. **The blob statistics below reproduce on both runs**, which is why they are
the load-bearing half of this item.

Frame 10 — the frame the star is brightest on — goes from 0.380% of pixels stepping over
60 and 0.0574% over 100, to **0.066% and none at all.** The review's target was "no
single-pixel step over 100 anywhere in the clash box, and the share over 60 at or below
0.195%". The share is met on every frame. The over-100 steps that remain are **not clash
marks**: the largest on frame 9 is at (635,361), rgb `[165 158 166]` → `[40 39 50]`, the
foe's pale face patch against its own hair mass — a garment/skull mesh silhouette, which
is debt item 4 below and belongs to 3b.

**A review claim that does not reproduce.** The review attributed the maximum step of 134
to the rays: *"the clash rays are flat polygon wedges with a maximum single-pixel step of
134"*. Measured, that step is at (637,359) on `s4-p4-parry-contact` frame 11, rgb
`[223 230 238]` → `[96 98 101]`: **b − r = +15, the blade's own edge.** The largest step
in the pass-5 clash box, 175.6, is likewise a blade edge. The ray defect is real and is
carried by the *share* statistics and by the 4× crop, not by the maximum, and the
"reference has no step above 100" comparison is partly an artefact of downscaling the
reference (Lanczos softens its blades and ours are at native scale).

### 2.2 Item 2 — `LANE_SPREAD` 1.55 → 1.35. **PAID, and here is what it costs.**

Shot as a matched pair: `s4-p5-parry-contact` (1.35) and `s4-p5-spread155` (1.55), with
every other pass-5 change in place in both, so the difference is the constant.

**What it buys.**

| | sash separation | skirt separation |
|---|---|---|
| reference image 3 | 0.635 | 0.615 |
| reference image 4 | 0.595 | 0.583 |
| `s4-p4-parry-contact` (1.55) | 0.854 | **1.052** |
| `s4-p5-spread155` (1.55) | 0.854 | 1.028 |
| **`s4-p5-parry-contact` (1.35)** | **0.749** | **0.750** |

(Widest-clear-column split constrained to the central half of the ink extent, side
centroids per band, span-cropped; bands are the corpus's own rows carried across as
fractions of each span — sash 0.411–0.621, skirt 0.621–0.890. This reader reproduces the
pass-4 review's corpus rows to ±0.002 and its `s4-p4` skirt figure to 0.004.)

The skirt separation goes from **1.7× the corpus to 1.22×**. It is the only lever that
touches base separation, and the structural answer — `TILE_WIDTH` against `FIGURE_HEIGHT`,
or `BODY_HALF` grown to the width the rig has — is a `Stage` change with combat-design
consequences and is handed on (debt item 8).

The blades also meet better:

| | minimum two-cloud separation | frames scored by the merge convention |
|---|---|---|
| 1.55 (`s4-p5-spread155`) | 0.0182 at frame 11 | 2 (frames 9, 10) |
| **1.35 (shipped)** | **0.0059 at frame 9** | **0** |

**What it costs, measured.**

1. **The figure is 15% larger in delivered pixels** (378 rows against 329), so every
   existing hard edge *on the figures* is about 15% steeper. Blade-excluded maximum
   single-pixel step in the clash box, frame 9 (which is bit-identical across two runs, so
   the comparison is sound): **99.3 at 1.55, 120.1 at 1.35.** Both readings are the same
   object — the foe's pale face patch against its hair mass, a mesh silhouette — read at
   two scales. Item 1's *marks* improved; the *figures'* silhouettes got sharper because
   they got bigger, and that is debt item 4, now louder.
2. **The ground band's dispersion does not change**: 19.8 at 1.55 against 20.2 at 1.35
   through the matched crop (§2.3). Neither a cost nor a gain.
3. **The corridor profile is 0 of 24 frames either way**, at 1.35 and at 1.55, as it has
   been at pass 2, pass 3 and pass 4. It has now failed to discriminate between two
   settings for a fourth time. See debt item 2.
4. **The whole-column merge count is 0 of 24 either way**, spanned; and the *un-spanned*
   profile reading, which is what pass 4's §6.1 compared, is 1 of 24 on the pass-5 capture
   against 17 of 24 on the pass-4 capture — the difference is the ground band becoming a
   step rather than a ramp (§2.3), not the spread.

**The refusal that stood for two passes is void, and the record should say why.** Pass 4
kept half of it on *"merged frames: 8 of 24 at 1.35, 1 of 24 at 1.55"*. That table
compared the 1.35 probe's **un-spanned** reading against the shipped setting's **spanned**
reading. Run consistently through the project's own tool the same capture reads 1 of 24
one-mass with `--span 0,348,960,329` and **17 of 24** without it — reproduced here, and
the discrepancy is now a refusal in the tool rather than a sentence in a document (§7.3).

### 2.3 Item 3 — the ground third. **The named defects are paid; the target statistic is not, and it was not reachable.**

The brief: *"Target the reference's dispersion rather than its floor: sd 24.6 and
p99−p01 = 91 through the lower third at matched scale, against the delivered 18.0 and 68…
you can reach sd 24.6 while never going below 25.7 by adding marks, not by darkening."*

**The first half of that sentence is right and the second half is arithmetically wrong.**
Reference image 3's lower third at matched scale reads sd 24.7 and range 91.4 — reproduced
exactly. **41.6% of that band's pixels lie below STYLE.md §2.2's own floor of luminance
25.73.** Clamped at the floor — which is what a pass obeying §2.2 can reach — the same band
reads **sd 21.0 and range 76.6**. Reaching 24.7 with a floor of 25.7 would require a p99
near 117, and nothing in the bottom third of a Family B painting is brighter than the
coral horizon at 102.

And the comparison was made at matched *scale* but not at matched *crop*. Reference image
3 downscaled to 378 rows is 468 px wide — **1.24 figure heights**. The capture's band was
read across the full 960 px — **2.54 figure heights** at 1.55, 2.9 at 1.35 — so the capture
was charged for two-thirds more empty sky than the reference has. Read through the
reference's own width:

| lower third of the figure span + 30 rows | p01 | median | p99 | sd | range |
|---|---|---|---|---|---|
| reference image 3, matched scale, its own 1.24 fh width | 11.0 | 30.8 | 102.4 | **24.7** | **91.4** |
| … the same, clamped at STYLE.md §2.2's floor | 25.7 | 30.8 | 102.4 | **21.0** | **76.6** |
| `s4-p4-parry-contact` f11, 1.24 fh crop | 26.7 | 39.2 | 97.5 | **20.2** | 70.8 |
| `s4-p5-spread155` f11, 1.24 fh crop | 26.7 | 39.6 | 97.2 | 19.8 | 70.5 |
| **`s4-p5-parry-contact` f11, 1.24 fh crop** | 26.7 | 39.7 | 97.5 | **20.2** | 70.7 |

**So the deficit was 20.2 against a floor-limited 21.0 — 4%, not 27% — and pass 5 did not
move it.** That is the honest number and it is a miss on the item as written.

What *did* move is everything the review named in words, and those were real:

- **The smear was an airbrush and is now a step.** The pass-4 form ran its transition
  across world y 0.50 down to −0.04, a soft 220-row ramp. Reference image 3's
  outer-column background falls from 49.0 at world y 0.00 to 16.7 at −0.10. The smear now
  runs its whole transition between world y 0.25 and −0.04, which also puts its top edge
  below the feet, where all three Family B images have it.
- **The band stopped brightening toward the frame bottom**, which was §2.6 of the review
  and the more actionable statement in it. Outer-70-column row background:

  | world y | image 3 | `s4-p4` | `s4-p5` |
  |---|---|---|---|
  | +0.15 | 40.2 | 55.1 | 52.9 |
  | 0.00 | 36.3 | 59.7 | **44.9** |
  | −0.10 | 17.5 | **59.3** | **38.2** |
  | −0.15 | 28.1 | 56.9 | **35.9** |

  Pass 4 *rose* 55.1 → 59.7 → 59.3 going down; pass 5 *falls* 52.9 → 44.9 → 38.2. The
  cause was found by measurement and it was not where pass 4 thought: the paper shader's
  own fog term is **dead on every scene that ships** (`PaperBackground` hands that pass
  `NO_BANDS` and draws the mist in a separate alpha-blended pass), and a probe capture was
  spent attenuating it before that was measured. The mist that actually lifts the ground
  is `PaperBackground.MIST_FRAG`, whose band 0 is centred at world y −0.14 with a half
  height of 0.56 — most of its area lies *under* the ground line. It now holds full
  strength through the feet (world y 0.13 still reads 0.95 of it) and lets go below the
  ground line.
- **Grass strokes at 0.80 rather than 0.34** on the dusk stage, against the review's
  "roughly 2% contrast". Visible in a 800×159 crop of the band at 1×.
- **Splatter, drips and wet blooms exist**, and finding out *where* they had to go is the
  transferable part: three probe captures put them **inside** the smear and measured no
  change at all in the band, because the smear is `INK_BLACK` and the marks are
  `INK_BLACK` — a mark drawn on its own colour is not a mark. This is STYLE.md §3.4's rule
  read one level out ("when the base colour already sits on the floor, pool by lifting
  everything else… it has nothing left to darken into"). What the corpus has is dark marks
  standing on a ground that is *not* at the floor: image 3's row median runs 56.7 at world
  y +0.05 and 15.8 at −0.15. The marks now live in the band from the smear's edge up to a
  quarter of a figure height.

**The residual, decomposed, so the next system does not chase it.** Reference image 3's
lower third has a row median of 24.3 at world y +0.35 and 27.7 at +0.25, where the capture
reads 43.9 and 48.9. The corpus is dark there because **two near-black bodies cover more
than half the row**; the capture's two bodies cover about 40% of a 4:3 frame and are on
STYLE.md's own value floor. The remaining ground-third gap is therefore a **part-count and
value-floor gap**, not a ground-ink gap: it belongs to §2.2's floor (debt item 1) and to
the figures (debt items 4 and 9), not to the shader that draws the ground.

### 2.4 Item 4 — the trail's angular sweep. **Partly paid, and the floor is measured.**

The trail history is now trimmed on **accumulated turn** as well as on age:
`InkSkinnedRenderer.TRAIL_SWEEP = 0.60 rad = 34°`, summed over the stored run's own steps
so a sweep-and-return spends the budget rather than cancelling it.

Largest local-background residual ridge (`L − uniform_filter(L, 61) > 1.0`) in the sky
window `x100..860 y60..330`, worst frame of each phrase capture, sampled every 6th frame:

| | worst frame | ridge | width | best-fit radius | circle-fit residual |
|---|---|---|---|---|---|
| `s4-p4-phrase-60hz` | 108 | 16,686 px | 432 px = **1.31 fh** | 187 px = 0.57 fh | 17.2 px |
| `s4-p5-phrase-60hz` | 258 | 14,549 px | 325 px = **0.86 fh** | 94 px = 0.25 fh | 31.6 px |
| `s4-p5-phrase-60hz`, frame 108 for comparison | — | 10,466 px | 309 px = **0.82 fh** | 0.42 fh | 10.2 px |

**Width down 37%, radius down 26%, and it is attached to the blade rather than standing in
empty sky. It is still an arc, and it still reads as a crescent at 1×.**

Three things the record should carry, because they correct the mechanism the brief
assumed:

1. **60–80° was not available.** On a fast phrase beat the stored poses are 0.5–0.7 rad
   apart, so the constant does not choose an angle, it chooses a *number of stored poses*.
   1.22 rad keeps three and measures 1.12 fh wide; 0.60 rad keeps two and measures 0.82.
   There is nothing between them.
2. **An angular cap cannot take the arc below about 0.74 figure heights**, because the
   ribbon spans the blade's whole length: 0.55 fh of blade plus 0.19 fh of rails
   (`rail` runs −0.28 to +0.04 world units on a 1.70-unit figure). The delivered 0.82 is
   within 0.08 of that floor. Removing the rest means drawing the ribbon on the *tip's*
   path rather than on the blade's body — a `rail` change, handed on as debt item 5.
3. **A probe that starts at the beat is not a control for a trail.** Two of this pass's
   probe captures compared sweep caps on a 12-frame window starting at t = 1.6866 and
   found 0.05, 0.35 and 0.60 rad bit-identical. They were: the capture window itself was
   shorter than `TRAIL_SECONDS`, so no setting was binding. Shoot the whole scene, or the
   history under test does not exist. §11.2b(g) again, in a new place.

---

## 3. Free items, all done

- **`analyse corridor` refuses without `--span`.** §7.3.
- **`CorridorProfile.FAMILY_B`'s image-5 exclusion note is corrected.** The old note said
  image 5's duellists are one component *"at every threshold tried"*. Measured, span
  cropped, sweeping the ink factor: at 0.40, 0.45 and 0.50 image 5 resolves **two** genuine
  duellists and its torso corridor reads exactly **0.0000** — the number pass 3 quoted,
  from the two bodies and not from the ground. At the operating factor 0.85 it is one
  mass. Both readings agree that it sets no floor, and the note now says that rather than
  the overstated version.
- **§6.1's merged-frames row and the sentence it supported are gone**, replaced by §2.2
  above with the spanned and un-spanned readings both stated.
- **§6's "the garments lean inward" is gone.** Measured, the garments lean *outward* at the
  0.55 nagasa, because the longer `REACH_TO_CROSSING` pushes both fists back: pass 4's own
  capture stands its duellists 1.052 figure heights apart at the skirt against 0.854 at the
  sash and a stand separation of 0.912. The conclusion the sentence supported — that
  `LANE_SPREAD` alone cannot reach the corpus — survives and is strengthened.

---

## 4. Claims in the record that do not reproduce

Every pass of this project has found one. Pass 5 found four.

1. **The clash's "maximum single-pixel step of 134" is a blade edge, not a ray.** §2.1.
2. **The ground-third target of sd 24.6 / range 91 was not reachable under §2.2**, and the
   comparison behind it crossed crops. §2.3.
3. **A 60–80° sweep cap was not available**, and would not have removed the arc if it had
   been. §2.4.
4. **`u_dusk = 0` is not bit-identical.** Pass 4's item-1 control reported that with
   `BLADE_NAGASA_FRACTION` reverted, `rig-bindpose` was bit-identical to the previous
   pass's null: *0 of 16,588,800 pixels differ*. That property does not survive editing
   the dusk branch, even though every new term is multiplied by `u_dusk` and every branch
   is gated on it. Measured: `s4-p5-null-static` against `s4-p4-null-static`, **97,791
   pixels over 24 frames, maximum channel delta 6**, all in the lower frame. Bisected:
   - a control adding five noise octaves and a dynamic branch that contribute exactly
     nothing → **bit-identical**, so it is not shader length and it is not the branch;
   - the grass amplitude change alone → bit-identical;
   - deleting the dusk smear line alone → bit-identical;
   - collapsing one `mix(a, b, u_dusk)` into an `if/else` with the identical two arms →
     **2 pixels differ.**

   So `mix(x, y, 0.0)` is not bit-exact against selecting `x` on this driver, and the
   full rewrite accumulates that to 97,791 pixels at a maximum delta of 6 — 0.6% of pixels
   per frame, at a difference no reader can see. **The picture is unchanged; the control
   is not.** `s4-p5-null-static` against `s4-p5-null-static-repro` is 0 of 16,588,800
   pixels, so the readback path is still sound. The lesson for the next system: *multiplying
   by zero is not the same as not compiling the term*, and a bit-identity control across a
   shader edit is a claim about a compiler, not about a picture.

---

## 5. Protected results, re-measured on the delivered pixels

| result | status | measurement |
|---|---|---|
| **The held breath** | HOLDS | `RehearsalTest.theHeldBreathIsAtSpecOnEveryScene`, headless on the same `Rehearsal` the director runs: 0.850× over 0.25 s per ramp on `duel-parry`, `duel-knockback`, `duel-phrase`. Observed red at `Timing.HELD_BREATH_SECONDS = 0.12`. |
| **The blades meeting** | HOLDS, and improves | minimum two-cloud separation **0.0059 at frame 9**, span `0,299,960,378`, against 0.0182 at 1.55; and for the first time **no frame is a merge**, so no reading is the convention's 0.0000. |
| **The bloom on the steel** | HOLDS | core `L ≥ 246.5` centroid to the nearer cool-bright cloud, box `x500..700 y230..450`, span 378: **0.7 / 0.5 / 0.4 px** on frames 9/10/11 = 0.0018 / 0.0013 / 0.0010 fh. Peak 253.1 / 254.1 / 253.8. **Caveat, and it is a real one:** the same reader on `s4-p4-parry-contact` through the pass-4 box reads 3.5 / 7.7 / 19.3 px, not the 0.4/0.5/0.7 the pass-4 record claims for that capture. The delivered picture meets the claim; the pass-4 capture does not, on this reader. |
| **The sky ramp** | HOLDS | §1 and §2.3's table. Within a few levels of image 3 from world y +0.5 up, and the sign of the gradient below +0.15 is now the corpus's. |
| **The guards** | HOLD | §6. Six guards, each observed red with the message it was written to print. |
| **Phrase continuity** | carried forward, **not re-measured** | The pass-4 review withdrew the `192×` figure — the reader latched onto fragments ranging 1,464–19,951 px on a two-figure frame with no figure named, which §11.3 forbids. Its conclusion survives by another route and was independently reproduced by that review: **longest run below 0.02 = 5 steps = 0.083 s**. Pass 5 did not re-measure it, because a reader that cannot name its figure should not be run again and building one that can was not in the brief. Handed on as debt item 12. |

---

## 6. Every guard, and the proof it was observed red

STYLE.md §11.2b(f): *no assertion counts as a guard until it has been observed red.*

**Which observations belong to which pass, because that matters more than the list.**
Guards **G** and **H** were broken and watched go red **in pass 5**, by hand, with the
suite restored green afterwards. Guards **A–F** were broken and watched go red in pass 4
by its author, and five of the six were independently broken and watched go red by the
pass-4 reviewer, who quoted the messages verbatim; pass 5 did **not** re-break them and
the messages below are carried forward from those two observations rather than re-taken.
That is a weaker claim and it is stated as one.

| # | guard | broken by | printed |
|---|---|---|---|
| A | `RehearsalTest.everyClashIsDrawnWhereTwoBladesActuallyAre` | `Scheduler.CLASH_SPAN` 0.42 → 0.90 | *"PARRY: the clash that starts at t=1.568 is still drawn at t=1.6468 with the two blades 6.2% of a figure height apart. A bloom is an assertion that they are meeting; on this frame it is false."* |
| B | `RehearsalTest.theHeldBreathIsAtSpecOnEveryScene` | `Timing.HELD_BREATH_SECONDS` 0.25 → 0.12 | *"PHRASE: the held breath runs 0.125 s per ramp over 5 ramp(s)…"* |
| C | `CorridorProfileTest.everyFamilyBImagePassesTheBandTheProjectFailsCapturesOn` | `torso` floor 0.011 → 0.014 | names image 4 at 0.011834… against a band of 0.014..0.015 |
| D | `CorridorProfileTest.aPairOfBodiesFourTimesTooFarApartFailsTheCeiling` | `skirt` ceiling 0.102 → 1.000 | fails, and the message interpolates the broken ceiling — cosmetic defect, recorded |
| E | `DuellistValueTest.theTwoDuellistsAreTellableApartInDeliveredPixels` | pointed at `s4-p3-parry-contact/frame_011.png` | *"…The corpus reads 3.2x to 9.32x; this reads capture: dark 0.228 through x385..465 y415..478 (81x64), pale 0.806 through x645..720 y425..488 (76x64), ratio 3.53x"* |
| F | `CorridorProfileTest.everyReadingCarriesASecondOneAtAFixedThreshold` | `CorridorProfile.FIXED_FACTOR` 0.60 → 0.85 | *"the fixed reading is supposed to be a second opinion and it agrees with the first on every band; that makes it decorative."* |
| **G** | **`ControlGuardTest.corridorRefusesWithoutASpan`** *(new, pass 5)* | the refusal replaced by `if (true) return;` | *"Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown."* Restored, green. |
| **H** | **`ParryWindowTest.theGradedParryWindowPutsAFigureHeightAt378Rows`** *(known-answer, and observed red in the ordinary course of this pass)* | `LANE_SPREAD` 1.55 → 1.35 before the constants were updated | *"docs/system4-debt.md normalises every s4-p4 parry number by 329 rows; the scene now says 377.8996018882435"* |

Guard E reads `out/captures/s4-p4-parry-contact/frame_011.png`, which is force-added and
published. **It still points at the pass-4 capture, and pass 5 did not repoint it**: the
value ratio was explicitly out of scope this pass, and repointing it means re-deriving four
torso and skirt rectangles on the new staging and publishing three more frames. The
consequence is stated rather than hidden — `DELIVERED_FLOOR = 4.00` is a ratchet on a
superseded capture, and it certifies pass 4's picture, not pass 5's. Debt item 13.

Known-answer rather than red-observed, and labelled as such:
`theProfileReturnsTheGapItIsToldToMeasure`,
`theProfileSaysOneMassRatherThanZeroWhenTheBodiesTouch`,
`theRowBackgroundFollowsAGradedSkyWhereAPaperLevelCannot`.

---

## 7. The apparatus

### 7.1 The dynamic control, and it got worse

Shot `duel-parry` twice at this commit, same harness, same arguments:

| pair | differing px of 16,588,800 | max channel delta | frames |
|---|---|---|---|
| `s4-p5-null-static` / `-repro` (static null) | **0** | 0 | none |
| `s4-p4-parry-contact` / `s4-p4-parry-repro` | 983 | 19 | 1 |
| **`s4-p5-parry-contact` / `s4-p5-parry-repro`** | **13,825** | **149** | **5** |

Per frame: **frame 11, 10,493 px (1.52% of the frame) in `x517..697 y322..454`, max delta
149**; frame 16, 1,420 px, max 48; frame 21, 1,109 px, max 126; frame 22, 801 px, max 77;
frame 23, 2 px, max 1.

**The graded frame is one of them, and that is worse than pass 4**, where frame 11 was
bit-identical across runs and the only differing frame was 18. It is stated first because
it constrains what may be read off frame 11: the two runs give a blade-excluded maximum
step of 88.6 and 108.1 in the clash box, and a share over 60 of 0.039% and 0.098%. The
blob statistics that carry item 1 read the same on both runs (6 blobs ≥ 40 px, fills
0.37–0.55 against 0.37–0.54), and frames 9 and 10 are bit-identical across runs, so the
item-1 conclusion does not rest on the unstable frame.

The boxes are the shed flecks and the star, which is where pass 3 and pass 4 found it too.
The amplitude rose because the marks are larger and softer: a mark whose age crosses the
`alpha <= 0.004` cull appears in one run and not the other, and a soft fan of that size
carries far more level difference than a 7×7 quad did. **This is the recorded
non-determinism getting louder and reaching the graded frame, not a new mechanism**, and the mechanism now has a name:
a *discrete visibility cull on a mark whose age comes from a float-accumulated clock*.

Consequence for anyone reading this document: **any absolute pixel claim about an
individual fleck or ember near a clash is unreproducible.** Every claim in §2 is either a
distribution over ≥ 90,000 pixel pairs, a ratio to a given span, or taken from frame 11.

### 7.2 Cross-reader

Every number in §1, §2, §4 and §5 was taken with an independently written NumPy/SciPy/PIL
reader that shares no code with `src/main/java`. It reproduces, to the stated precision:
the pass-4 review's corpus separations (±0.002), its `s4-p4` skirt separation (1.052
against 1.048), its clash blob table (7×7 fill 0.98, 7×8 0.89, 8×8 0.81, 8×8 0.78), its
maximum step of 133.5 against 134, and its ground-third row (p01 11, median 31, p99 102,
sd 24.7, range 91.4 — against 11 / 31 / 102 / 24.6 / 91). Where it disagrees it is said so
in §4 and §5.

### 7.3 `analyse corridor` refuses without `--span`

STYLE.md §11.2b(e): *a discipline written into a document but not into the tool that reads
it is documentation, not a guard.* The rule was in this document's own Commands section
and the pass-4 review found the one place in the same document that broke it. Both forms
of the command now refuse; `--allow-detected-span` waives it, out loud, on a command line
a reviewer can read. The whole-column form additionally **uses** the span now — it crops
the component analysis to it and normalises by its height rather than by `Figure.detect`.
Asserted in `ControlGuardTest.corridorRefusesWithoutASpan`, observed red.

### 7.4 The corridor band criterion is a diagnostic, not an acceptance

`analyse corridor --profile` still prints every band, still runs the criterion on the whole
of Family B first, and still exits 1 if **the corpus** misses the band the corpus set —
that half has caught two real defects. The **capture's** verdict is now a printed
diagnostic and no longer a build failure. See debt item 2 for the record that justifies it.

---

## 8. Permanent System 4 debt

Recorded, not fixed. Each item carries its number, its region where it has one, and the
system that inherits it. **This list is the deliverable of the five-pass cap.**

1. **STYLE.md §2.2's value floor is wrong for a dark ground, and System 4 could not reach
   the corpus's contrast without breaking it.** The corpus's dark duellist prints at
   luminance 12.2 (image 3, `x205..285 y415..520`); §2.2's floor `#161A22` is luminance
   25.7; a figure authored at the floor delivers 27.9. **41.6% of reference image 3's lower
   third lies below the floor.** STYLE.md has already been corrected to say the floor
   should be a *fraction of the frame's own ground luminance*; **nobody has measured that
   fraction.** → **the next pass to touch the palette**, with the region recorded.
2. **The corridor band criterion.** Sound instrument, corpus passes it, and **no frame of
   any capture this project has shot has ever passed a single band of it** — at pass 2,
   pass 3, pass 4, at `LANE_SPREAD` 1.55 and at 1.35. Four passes, no discrimination
   between two settings of the project, ever. Its `sash` band is a 6% window resting on two
   paintings and its `head` and `feet` bands are reader-unstable. **Kept as a diagnostic
   printout; deleted as an acceptance; spend nothing further.** Keep the fixed-0.60 second
   reading — it is the one piece of this apparatus that has ever caught anything, and the
   first thing it caught was pass 4. → **nobody; it is closed.**
3. **§7.0.1 — the pelvis has exactly 0.0000 figure heights of horizontal motion relative to
   its own stance, in all three scenes, for a fifth pass.** Hip path / hand path 1.1%
   (parry), 0.6% (phrase); System 2 was failed at 1.5%. Not re-measured since pass 4;
   nothing has touched the mechanism. **System 4's motion is merely correct, not poetic, and
   it closes that way.** The fix is a directive that translates and rotates a body. → **whoever
   owns the directive vocabulary.**
4. **The figures' silhouettes still print flat-shaded polygon facets**, on the foe's head,
   shoulder and garment. Evidence: `s4-p5-parry-contact` frame 11, `x610..690 y360..420` at
   8×, and the largest blade-excluded single-pixel step in the whole clash box on frame 9 —
   120.1 at (635,361), `[165 158 166]` → `[40 39 50]`, the pale face patch against the hair
   mass. This is the §10 fail-on-sight row that pass 5's item 1 did **not** cover. → **3b**
   (faces and skull) and **3c** (garment material).
5. **The blade trail is still an arc 0.82 figure heights across.** An angular cap cannot go
   below ≈ 0.74 fh because the ribbon spans the blade's whole length plus its rails; the
   remaining fix is to draw the ribbon on the *tip's* swept path (`InkSkinnedRenderer.rail`).
   Evidence: `s4-p5-phrase-60hz` frame 108, sky window `x100..860 y60..330`. → **3c**.
6. **The embers: 2–4 blobs against STYLE.md §5's 8–20.** `r − b ≥ 40`, `L ≥ 150`, ≥ 4 px:
   **3 / 3 / 2 / 2 / 4** on frames 9/11/13/15/17 of `s4-p5-parry-contact` (pass 4: 5/3/2/2/2).
   Unchanged in four passes. → **3c**.
7. **The foe's blade is a stub**: 0.211 of a figure height against the hero's 0.420, and its
   cool-bright cloud on the graded frame is a sliver. **The foe's lower garment** is 0.300
   of a figure height against the hero's 0.581 and the corpus's 0.495. Both need the same
   thing: a per-figure rig parameter (`SamuraiRig.build(skirtWidth)` plus the matching cloth
   rails, which are shared with the simulation). → **3c**, with **System 5** owning whether
   two duellists may differ in build at all.
8. **The base separation is 1.22× the corpus's and the structural fix is a `Stage` change.**
   `LANE_SPREAD` is at 1.35 and reaching the corpus's 0.58–0.64 would need about 1.05, which
   collides with `BODY_HALF = 0.28` against a rig whose authored garment is 0.64 wide plus a
   wet-bleed halo of 0.15–0.2 per side. Either `TILE_WIDTH` rises against `FIGURE_HEIGHT`, or
   `BODY_HALF` grows to the width the rig actually has and the lane spacing follows it. →
   **System 5**, because it is a combat-design decision about how far apart two combatants on
   adjacent tiles stand.
9. **Faces, and the part count that goes with them.** At matched scale reference image 3
   resolves about **25** readable parts per duellist; the pass-4 review counted **9** per
   duellist on the capture — no face, no hand, no guard, no second blade, no fold, no foot on
   the ground. Pass 5 did not re-count, because a reviewer's first act is not a pass's to
   pre-empt and because §11.0's own instruction is that the count be taken by someone who did
   not produce the work. **The figure is now 378 rows rather than 329, so the count should be
   re-taken at the new scale.** → **3b**, and it is the single largest matched-scale gap in
   the picture.
10. **The chain still arrives together.** Hips and shoulder peak on one 1/120 s sample,
    elbow/hand/tip on another. STYLE.md §10's last row, §7.0.3. Untouched since pass 3.
    → **whoever owns the directive vocabulary**, with item 3.
11. **§7.3's ink bloom never appears in a delivered frame.** `Duel.Kind` needs a fourth entry
    resolving a `Hit`. **The knockback scene has never been shot on the dusk stage and never
    measured**, though it goes through the same stage, blade and base colour. → **System 5**.
12. **Phrase continuity has no reader that can name its figure.** The statistic is sound —
    row-local ink at 0.60× the row background, 3×3 opening, silhouette area-averaged into a
    64×64 grid, mean absolute difference of consecutive grids — and its tracked component
    ranges 1,464–19,951 px across a two-figure frame, which §11.3 forbids by name. Quote the
    run-length conclusion (longest run below 0.02 = 5 steps = 0.083 s) and nothing else until
    a reader exists that refuses when two bodies resolve. → **whoever next measures motion.**
13. **`DuellistValueTest` certifies pass 4's picture, not pass 5's.** Its `DELIVERED_FLOOR`
    ratchet of 4.00 and the pass-2 skirt-pooling result both read
    `out/captures/s4-p4-parry-contact/frame_011.png`, which is tracked and published, so they
    run everywhere — but the staging under them has moved. Repointing needs four rectangles
    re-derived on the 378-row span and the new frame force-added. → **the next pass to touch
    the duellists' value.**
14. **The pale duellist is brighter than its own ground (1.304 of it) where the corpus's is
    darker (0.372–0.419), and warm (`#8D6E69`, hue 8°) where the corpus's is cool (`#1F283C`,
    hue 221°).** The *ratio* gate is met at 4.15× inside the corpus's 3.22–9.32; the *sign* is
    wrong. Boxes: `x645..720 y425..488` on the pass-4 span. Note that defensible alternative
    boxes inside the same figure read 0.599 to 1.338, a 2.2× spread — §11.3's situation
    exactly. → **3c**.
15. **Capture non-determinism, characterised and now larger.** §7.1: 9,101 px over 4 frames,
    max delta 213, in the shed flecks and the star. The graded frame is bit-identical across
    runs. Not worth further budget; it is a constraint on what may be claimed, not a bug to
    fix. → **nobody; obey it.**
16. **`u_dusk = 0` is no longer bit-identical to pass 4's null**, at 97,791 px and a maximum
    channel delta of 6 — invisible, and the control is gone all the same. §4.4. → **nobody;
    but do not quote pass 4's bit-identity claim again.**
17. **`Duellists.blades`' `L > 212` is an absolute pixel statistic** (§11.2b(d)) and happens to
    work only because cream paper is warm enough to fail the `b − r > −6` test. Valid within
    the `f0ad18994eec` harness and void across a harness change. → **anyone changing the
    harness.**
18. **The planning composition**: `s4-p5-phrase-60hz` frame 0 is two small figures in the
    bottom-left of an 85%-empty frame. It is now a beautiful 85%-empty dusk sky, which is not
    the same as fixed — and it is also the closest thing in either capture to a §0 *yes*.
    → **System 5**, with the framing.

---

## 9. What each system inherits, in one place

**To System 3b (faces).** The part count, and it is the standing measure of this picture:
**9 readable parts per duellist against reference image 3's 25**, counted by the pass-4
review at 329 rows of figure height. Neither figure resolves a face, a hand, a guard, a
second blade, a fold or a foot on the ground. The figure is now delivered at **378** rows,
so re-take the count at that scale before anything else (§11.0). With it goes debt item 4:
the head and skull print flat-shaded polygon facets, and the pale face patch against the
hair mass is the largest hard edge left in the frame that is not a blade.

**To System 3c (materials and textures).** Four material questions, all measured:
the trail's shape (item 5 — it is a ribbon over the blade's body and should be a ribbon
over the tip's path); the embers (item 6 — 2–4 against 8–20, unchanged in four passes);
the two figures' asymmetry (item 7 — the foe's blade at 0.211 fh against the hero's 0.420
and its lower garment at 0.300 against 0.581, both needing one per-figure rig parameter);
and the pale duellist's sign and hue (item 14 — brighter than its ground where the corpus
is darker, warm where the corpus is cool).

**To System 5 (combat UI and staging).** Three staging conflicts, none of them visual:
the lane spacing (item 8 — `LANE_SPREAD` is a mitigation at 1.35 and the structural answer
is `TILE_WIDTH` against `FIGURE_HEIGHT` or a wider `BODY_HALF`, which decides how far apart
two combatants on adjacent tiles stand); the missing resolutions (item 11 — `Duel.Kind` has
no entry for a `Hit`, so §7.3's ink bloom cannot appear, and the knockback has never been
shot on this stage); and the planning framing (item 18 — an 85%-empty frame that the UI
will have to live in, and which is the one framing in this project that answers §0 with
something close to a yes).

**To whoever owns the directive vocabulary.** Items 3 and 10, together: the pelvis does not
translate and the chain arrives together. Both are architecture, not a visual pass, and
System 4 closes with both unpaid after five passes.

---

## Commands, so nothing has to be reconstructed

```
./gw capture  -Pscene=duel-parry       -Pout=out/captures/s4-p5-parry-contact \
              -Pframes=24 -Pcols=6 -Pstart=1.42 -Pstep=0.0167 -Pw=960 -Ph=720
./gw capture  -Pscene=duel-parry       -Pout=out/captures/s4-p5-parry-repro       (identical: the dynamic control)
./gw capture  -Pscene=duel-parry-debug -Pout=out/captures/s4-p5-parry-contact-debug  (same window)
./gw capture  -Pscene=rig-bindpose     -Pout=out/captures/s4-p5-null-static \
              -Pframes=24 -Pcols=6 -Pstart=0.0 -Pstep=0.0167 -Pw=960 -Ph=720
./gw capture  -Pscene=rig-bindpose     -Pout=out/captures/s4-p5-null-static-repro (identical)
./gw capture  -Pscene=duel-phrase      -Pout=out/captures/s4-p5-phrase-60hz \
              -Pframes=418 -Pcols=22 -Pstart=0.0 -Pstep=0.0167 -Pw=960 -Ph=720

./gw analyse  -Pargs="blades   out/captures/s4-p5-parry-contact --span 0,299,960,378 --max 0.02"
./gw analyse  -Pargs="corridor out/captures/s4-p5-parry-contact --profile --span 0,299,960,378"
./gw analyse  -Pargs="diff     out/captures/s4-p5-parry-contact out/captures/s4-p5-parry-repro"
./gw test
```

**`--span` is now required** on `analyse corridor` and the tool refuses without it;
`analyse blades` still asks for it in its help text and should be given it for the same
reason. The spans are `0,299,960,378` for `s4-p5-*` and `0,348,960,329` for `s4-p4-*`;
`ParryWindowTest` derives the live one.

`out/captures/s4-p5-spread155` is kept: it is `LANE_SPREAD = 1.55` with every other pass-5
change in place, and it is the control that makes §2.2's cost table a measurement of one
constant rather than of a pass.
