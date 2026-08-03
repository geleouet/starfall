# System 4 — standing debt

**Status: pass 4 shipped, self-graded, and NOT self-declared a pass.** System 4 is at
**pass 4 of 5**. The pass-3 review's brief had five ranked items; items **1, 2, 3 and 4**
are paid, item **5 is not** and is decomposed below with both numbers. Two of the
review's own claims did not reproduce and are corrected here with captures.

Every capture quoted is `s4-p4-*` unless named otherwise. All of them carry
`harness=f0ad18994eec`, the hash that produced every `s4-p2-*`, `s4-p3-*` and `rev-p*`
capture, so comparisons back to pass 2 are in scope per §11.2b(d). Every pixel number is
a ratio to a **given** figure span or to the frame's own row background, and every one is
printed beside its rectangle (§11.3).

---

## 0. The span, which is new and is load-bearing for every number below

**`--span 0,348,960,329`.** Every parry measurement in this document normalises by a
329-row figure whose crown is at row 348 and whose feet are at row 676.

It is computed rather than detected, and `ParryWindowTest` asserts it against
`Schedule.framingAt` and `Stage.FIGURE_HEIGHT` through the same camera arithmetic
`DuelScene.aim` runs, so it cannot rot when a framing constant moves. Two reasons:

- **Detection is now wrong, not merely unstable.** On the Family B stage the ground is
  itself a dark ink smear (STYLE.md §1), so the largest ink component runs from the head
  down into it and out to both frame edges: on `s4-p4-parry-contact` frame 11 it spans
  `x17..699 y320..719`. `Figure.detect` returns figure heights of 219–226 px across the
  graded window against a true 329.
- **The pass-3 review's span was 23% too tall.** It used `y314..719`, h 406 — the ink
  top to the *frame bottom*. Row 719 is world y −0.22; the feet are at row 677. So every
  band fraction in the pass-3 record is about 0.81× what the same pixels read here, and
  **the comparisons in this document re-measure pass 2 and pass 3 through the corrected
  span rather than quoting their published numbers**. Where a published number is
  quoted, it is labelled with the span it was taken through.

---

## 1. Item 1 — the dusk sky. PAID.

`Palette.SKY_ZENITH / SKY_MID / SKY_HORIZON / SKY_HORIZON_HOT` were referenced by
exactly one scene and it was `SmokeScene`. Every graded duel this project has shot was
fought on Family A cream paper. `DuelScene` now draws the Family B stage:
`PaperBackground.dusk(true)`, a new `u_dusk` branch in `paper.frag`, and a three-stop
backdrop gradient in `ink_resolve.frag` so a figure's wet bleed wicks into the colour of
the sky it stands against rather than into a cream constant.

**The ramp is measured, not eyeballed.** Median of the outer 70 columns of each row on
all three Family B images, converted to world y through each image's own figure span.
Delivered against reference image 3, both read the same way:

| world y | image 3 | `s4-p4` frame 11 |
|---|---|---|
| +2.40 | `#2E3957` L 57.2 | `#2F3B58` L 58.5 |
| +2.00 | `#424660` L 71.5 | `#3F3F5C` L 65.1 |
| +1.70 | `#5C5268` L 85.7 | `#614A64` L 80.8 |
| +1.10 | `#7C5761` L 95.6 | `#78525D` L 90.9 |
| +0.90 | `#96505A` L 96.0 | `#79525B` L 90.9 |
| +0.70 | `#8E4D53` L 91.4 | `#77505A` L 89.0 |
| +0.50 | `#413B48` L 61.6 | `#4B3C4B` L 64.3 |
| +0.30 | `#383540` L 54.4 | `#2A2F40` L 47.5 |
| +0.15 | `#2D252F` L 39.4 | `#353745` L 56.0 |
| +0.00 | `#27222D` L 35.9 | `#3A3B49` L 60.2 |

The sky matches the corpus to within a few levels from world y +0.5 upward. **Below
+0.3 it is 15–25 levels too light and that is a miss**: the corpus's ground band runs
27–39 and this runs 56–60, because the fog bands of STYLE.md §6 sit on top of it. It was
attacked twice (the smear anchored to world y instead of uv, the mist lobes allowed to
reach zero at dusk) and both moved it by less than half of what is needed. The remaining
term is the mist's own strength low down, and touching it further starts trading against
§6's "non-negotiable".

**Two things in the palette table disagree with the corpus and the corpus wins**
(STYLE.md's own preamble): `SKY_HORIZON` `#D9736B` is luminance 136 and the corpus's
horizon band reads 95–100 at the frame edges and 108 at its hottest between the
duellists; and the sky does not run to the bottom of the frame — all three images turn to
a dark blue-grey ground haze below world y ≈ 0.6. Both are expressed as mixes toward
`SKY_ZENITH` so no colour is invented.

**Saturation, against STYLE.md §10's "saturated colour across large areas":** frame-wide
share above HSV saturation 0.40 is 39.1% for the capture and 27.0% for reference image 3,
but above 0.55 it is **0.01% against the corpus's 4.36%**, and the 99th percentile is
0.485 against 0.608. The capture is broader and flatter in chroma than the corpus and
does not reach it anywhere. Not a §10 hit.

**Every Family A scene is untouched — measured, not argued.** `u_dusk = 0` takes the same
branch the shader always took and `backdropStops(false)` is three copies of `PAPER_WARM`,
for which `mix(c, c, t)` is exactly `c` — but "by construction" is the phrase STYLE.md
§11.2b(g) says to read as *nobody has measured this*, so it was measured. With **only**
`BLADE_NAGASA_FRACTION` reverted to 0.40 and every other change of this pass in place,
`rig-bindpose` is **bit-identical to `s4-p3-null-static`: 24 of 24 frames, 0 of
16,588,800 pixels differ.** That covers the sky branch, the backdrop gradient in
`ink_resolve.frag`, the trail taper and its contrast scaling, and the clash's hot core in
one control. Kept as `out/captures/rev-p4-familyA-control`.

### 1.1 What the sky exposed, which is the interesting part

Three defects were invisible on cream and are structural, not cosmetic:

- **The blade trail.** Screen adds `src × (1 − dst)`, so the identical ribbon that sat
  2.7% above a 0.86 paper — recorded as *invisible*, as a fault, by three reviews — sits
  five times further above a 0.35 dusk sky. The first dusk capture printed it as a
  near-closed pale dome a figure height across: a moon, not a smear. Fixed two ways:
  the ribbon now **tapers** with age (`0.18 + 0.82·(1−age)²` of its width, nonlinear and
  fast in the last third, the same law STYLE.md §4 gives a hair tip), and its peak is
  scaled to hold a fixed multiple of the contrast it had on cream. It reads **1.050–1.070×
  its own row background** on frame 6 at `(450,215)`, `(400,230)`, `(500,212)`,
  `(560,230)`, `(620,265)` against 1.027× on paper.
- **The clash bloom stopped reaching white.** Same mechanism: `L ≥ 240` is a
  background-dependent test and the mark peaked at 213 over a dark sky. Warm-bright core
  pixels fell from 152/276/111 on frames 9/10/11 to **55 / 0 / 0**. STYLE.md §2.2 makes
  the clash bloom one of only two things allowed to approach white, so the innermost
  third of the disc is now drawn again at a saturating amplitude. Restored: see §4.
- **The two-figure value statistic changed meaning.** See §3.

---

## 2. Item 2 — the tall X. PAID, and the review's reasoning for it is HALF WRONG.

`MeshAuthor.BLADE_NAGASA_FRACTION` **0.40 → 0.55** of a figure height. The old number was
anatomy ("a katana's nagasa is about 70 cm on a 170 cm swordsman") and nobody had measured
the paintings. Measured, independently, on all three Family B images — crossed blades as
one cool-bright component, `L > 1.30 ×` the row's own background, 2×2 opening:

| | pixels | box | diagonal |
|---|---|---|---|
| image 3 | 14,778 | `x272..568 y317..456` | 327 px = **0.487** figure heights |
| image 4 | 19,414 | `x348..581 y117..444` | 402 px = **0.595** |
| image 5 | 16,992 | `x342..627 y122..460` | 442 px = **0.655** |

and image 3's left blade runs tsuba (390,465) to kissaki (560,135) = 379 px on a 672 px
figure = **0.56 of a figure height of steel per duellist**.

Delivered, same instrument, both captures normalised by the corrected 329-row span:

| | hero cloud | foe cloud | union |
|---|---|---|---|
| `s4-p3` frame 11 | 419 px, diag **0.353** | 196 px, diag **0.190** | **0.369** |
| `s4-p4` frame 11 | 655 px, diag **0.420** | 235 px, diag **0.211** | **0.526** |
| `s4-p4` frame 9 | one merged cloud of 522 px `x580..642 y294..444` | — | **0.493** |
| `s4-p4` frame 10 | 691 px + 261 px | | **0.492** |

**The crossed figure now spans 0.49–0.53 against the corpus's 0.487–0.655.** That is the
first time this project has hit a Family B composition statistic. The foe's blade is
still a stub relative to the hero's (0.211 against 0.420) and that is unpaid.

### 2.1 The review's argument for item 2 does not fully reproduce

The review's claim: *"this single change discharges the torso corridor, the bind span and
the bloom's three-frame limit at once."* Measured:

- **The bind span: TRUE.** Headless on the same `Rehearsal` the capture's director runs,
  the two blade segments are within 2% of a figure height from **t = 1.5500 to 1.6316 =
  0.0816 s**, against pass 3's 0.066 s — 49% of the 0.168 s contact span against 39%.
- **The bloom's three-frame limit: TRUE, and it was cashed.** `Scheduler.CLASH_SPAN`
  0.27 → **0.42**, found by moving the guard rather than by argument:
  `RehearsalTest.everyClashIsDrawnWhereTwoBladesActuallyAre` is green at 0.46 and red at
  0.50. 0.42 × 0.168 s = **4.2 frames at 60 Hz against 2.7**. Still far short of the ten
  §7.1 implies.
- **The torso corridor: FALSE, and it went the other way.** Longer blades mean a longer
  `REACH_TO_CROSSING` (0.263 → 0.324 world units), so the fist target is placed further
  back and lower from the crossing, the arms saturate harder, and the hands sit further
  apart. Median `torso` band over the contact window, corrected span: pass 3 **0.0198**,
  pass 4 **0.2173** — 1.33× the corpus's own reading to **14.58×**. At the graded frame 11
  it is fine (0.0122, inside the band), but across frames 16–23 the two bodies open to
  0.24–0.39 where the corpus holds 0.0149. **Item 2 bought the X at the cost of the
  pinch**, and the lever that would buy both back is `Director.FIST_DROP`, untouched at
  1.0 this pass.

---

## 3. Item 3 — `Figure.dark` base `INK_INDIGO` → `INK_BLACK`. PAID. And the statistic
that graded it had to be replaced.

One line, exactly as the review measured it. But the statistic every previous pass used —
`CorridorProfile.medianInkOverGround`, the median of pixels *darker than 0.85 ×* the row
background — is **ill-posed on the Family B stage**, and reports the change as a
catastrophe:

| through `x300..470 y300..420` / `x600..760 y340..460` | dark | pale | ratio |
|---|---|---|---|
| reference image 3, `x190..300` / `x540..650, y400..540` | 0.133 | 0.436 | **3.27×** |
| `s4-p3` frame 11 (cream) | 0.245 | 0.521 | 2.12× |
| `s4-p4` frame 11 (dusk) | 0.434 | 0.520 | **1.20×** |

The cause: the pale duellist is now **brighter than its own ground**, so only 3,671 px of
its torso box fall below the threshold against 6,884 on cream, and the statistic silently
changes subject to "the darkest quarter of the pale figure". Reference image 5's pale
duellist has the same property (it reads 1.245 of its sky), so this is a case the corpus
contains and the instrument could not express.

**Replacement: `CorridorProfile.medianOverGround`, threshold-free**, run on the corpus
first (§11.0) through boxes placed *inside* each torso — the review's wide boxes are
three-quarters sky and read 0.95 on every capture:

| | dark | pale | ratio | dark, absolute L |
|---|---|---|---|---|
| reference image 3 `x205..285 y415..520` / `x555..635` | 0.128 | 0.419 | **3.28×** | 12.2 |
| reference image 4 `x215..285 y395..505` / `x535..605` | 0.115 | 0.372 | **3.22×** | 11.2 |
| reference image 5 `x165..235 y395..505` / `x445..515` | 0.134 | 1.245 | **9.32×** | 13.2 |
| `s4-p2` frame 11 `x385..465 y415..478` / `x645..720 y425..488` | 0.249 | 0.749 | 3.00× | 53.0 |
| `s4-p3` frame 11, same | 0.229 | 0.808 | 3.53× | 48.7 |
| **`s4-p4` frame 11, same** | **0.314** | **1.304** | **4.15×** | **27.9** |

**The delivered ratio is inside the corpus's own spread for the first time (4.15 against
3.22–9.32)**, and interior spread is unchanged (dark IQR 0.150 on both p3 and p4).
`DuellistValueTest.DELIVERED_FLOOR` is a ratchet at 4.00.

**But the ratio is carried by the pale figure, and the dark one still misses.** It reads
0.314 of its own sky against the corpus's 0.115–0.134 — 2.5× too light. And here is why
that cannot be closed the way pass 3 assumed:

> **The corpus's dark duellist prints at luminance 12.2, and STYLE.md §2.2's own floor
> `#161A22` is luminance 25.7.** The delivered figure is at **27.9** — it is *on the
> floor*. The corpus breaks §2.2 by a factor of two to get its silhouette.

So the 3.27× target is unreachable while §2.2 stands, and it is *harder* on a dusk sky
than on cream: `INK_BLACK` over a 213-luminance paper is 0.12 of the ground, and over an
89-luminance sky it is 0.29. Pass 3's "the ceiling reachable by pooling alone is
`INK_BLACK`" was right about the ceiling and wrong about what that ceiling is worth. This
is a **STYLE.md question, not a shader question**, and it is handed on as one.

---

## 4. The bloom and the blades, re-measured. Both protected results HOLD.

### 4.1 The bloom — improved

Core = `L ≥ 246.5` inside `x520..700 y260..470` (the blade's own steel is luminance
240.7, so 240 is not a clean core threshold on this stage), centroid, minimum distance to
each of the two largest cool-bright clouds; figure height 329 px given.

| frame | `s4-p3` core / to nearer blade | `s4-p4` core / to nearer blade |
|---|---|---|
| 9 | 162 px, 2.7 px = **0.008** | 81 px, **0.4 px = 0.001** |
| 10 | 104 px, 0.4 px = 0.001 | 149 px, **0.5 px = 0.001** |
| 11 | 8 px, 2.4 px = 0.007 | 91 px, **0.7 px = 0.002** |
| 12 | 0 px | 4 px |

Peak luminance 254 / 255 / 253. On frames 9 and 10 there is **only one** cool-bright
cloud — the two blades' lit steel is one connected component — which is the strongest
available form of "the star is in the fork". At frame 11 the second cloud is 37.6 px =
0.114 away against pass 3's 0.034, on a frame where the foe's blade cloud is an 8×88 px
sliver.

**Embers: 5 / 3 / 2 / 2 / 2 strongly-warm blobs ≥ 4 px on frames 9/11/13/15/17**
(`r − b ≥ 40`, `L ≥ 150`) against §5's 8–20. Unpaid, unchanged from pass 3.

### 4.2 The blades meet — and the tool's `0.0000` is a merge, not a measurement

`./gw analyse -Pargs="blades out/captures/s4-p4-parry-contact --span 0,348,960,329 --max 0.02"`

- **Frames 9 and 10 are a single cool-bright component** (731 px `x559..650 y262..446`;
  1,046 px `x561..666 y281..444`). The tool scores that 0.0000 by convention and prints
  PASS. The union of pass 3's two clouds at frame 9 was `x551..644 y273..447`, so this is
  a genuine merge of the same two objects and not a blade disappearing.
- **Among frames where two clouds resolve, the minimum is 6.0 px = 0.0182 at frame 11**,
  against pass 3's 11.3 px = 0.0344 through the same span. That is the first time the
  delivered acceptance of ≤ 0.02 has been met by a two-cloud reading.

Pass 3's published `0.0264` and this document's `0.0344` are the same 11.3 px through a
406-row and a 329-row span.

---

## 5. Item 4 — the criterion. PAID, and it found three defects, one of them in the
review's own numbers.

`analyse corridor --profile` now runs on **all three** Family B images, states the
acceptance as a **band with both edges**, takes an explicit `--span`, and prints a
**second reading at a fixed ink threshold** beside every number.

### 5.1 The corpus, all of it

Measured at ink factor 0.85, each image on its own span, component analysis cropped to
that span:

| band | image 3 | image 4 | band adopted |
|---|---|---|---|
| head | 0.0847 | 0.1612 | 0.084 .. 0.162 |
| torso | 0.0149 | 0.0118 | 0.011 .. 0.015 |
| sash | 0.0921 | 0.0976 | 0.092 .. 0.098 |
| skirt | 0.1010 | 0.0858 | 0.085 .. 0.102 |
| feet | 0.1129 | 0.0444 | 0.044 .. 0.113 |

Floors are the corpus minimum rounded down and ceilings the maximum rounded up, both at
three decimals, **with no margin added in either direction** — a margin would be a number
nobody measured. `sash` is a 6% window on two samples and that is stated rather than
padded; what makes it usable is that the capture misses by factors of three to four.

### 5.2 Reference image 5 is EXCLUDED, and the reason corrects the review

The pass-3 review's central charge was that **image 5 reads `torso = 0.0000`, the
identical number the capture was failed for**. That reading reproduces exactly through
the pass-3 reader — and it is measured **between a duellist and the ground smear**.

Image 5's two duellists are *one connected ink component inside their own figure span*:
each carries a second sheathed blade whose scabbard crosses the gap at hip height, and
their hilts touch. Before the component analysis was cropped to the span, the two
"bodies" the tool found were one duellist and the ground band below the feet, and the
window was drawn between them. That is STYLE.md §11.3's silent wrong answer one level up,
and the checked-in reader and the review's independent NumPy reader now agree — the
review's own reader called image 5 one mass in all 264 combinations it swept, which is
what it is.

So image 5 sets no floor. The exclusion is named in `CorridorProfile.FAMILY_B`, printed
by the command, and **asserted** in `CorridorProfileTest`: if a reader change ever makes
image 5 resolve into two bodies, the test fails and the band has to be re-derived with it
in. §11.0's *"name the ones you excluded and why"*, held as a test rather than as a
sentence.

**Consequence for the record: the `torso` floor is 0.011, not 0.000.** The review's
argument that the capture could not be failed for `torso = 0.0000` because the corpus
does it too does not survive the corrected reader.

### 5.3 The delivered profile, and the ceiling doing its job

`0 of 24 frames pass every band` on **passes 2, 3 and 4 alike** through the corrected
span and the two-sided band. Pass 3's headline "8 / 24 → 11 / 24" was measured against a
floors-only criterion that the pass-3 review then showed prefers a staging in which the
blades never meet; against a criterion the corpus passes, none of the three passes has
ever put a frame inside the corpus's profile.

Merged frames: pass 2 **2**, pass 3 **2**, pass 4 **1**.

Band medians over the contact window (frames 6–23), as a multiple of image 3's own
reading, and **beside them the same median at the fixed 0.60 threshold**:

| band | p3 @0.85 | p4 @0.85 | p3 @0.60 | p4 @0.60 |
|---|---|---|---|---|
| head | 0.0836 (0.99×) | 0.1474 (1.74×) | 0.1292 | 0.2264 |
| torso | 0.0198 (1.33×) | 0.2173 (**14.58×**) | 0.2417 | 0.2523 |
| sash | 0.3905 (4.24×) | 0.3282 (3.56×) | 0.4498 | 0.4453 |
| skirt | 0.4058 (4.02×) | 0.1398 (**1.38×**) | 0.4103 | **0.3191** |
| feet | 0.5258 (4.66×) | 0.0760 (**0.67×**) | 0.5760 | **0.2188** |

**Read the last two columns before believing the third.** The headline collapse of
`skirt` from 4.02× to 1.38× and `feet` from 4.66× to 0.67× is **mostly photometric**: at
the fixed threshold `skirt` only improves 0.4103 → 0.3191 (22%) and `feet` 0.5760 →
0.2188 (62%). The dusk ground band and the low mist put marks into those bands that are
barely dark enough to count at 0.85 and not at 0.60. The second reading is the instrument
the review asked for, and the first thing it caught was this pass. **The skirt is still
about 3.2× the corpus as geometry.**

### 5.4 The whole-column scalar, for the record

Image 3 0.0149, image 4 0.0074, image 5 0.0000, against the 0.06 two passes chased.

---

## 6. Item 5 — the base separation. **NOT PAID**, and the review's decomposition of it
does not survive arithmetic.

The review: *"the bodies stand 0.793 figure heights apart at the skirt against the
corpus's 0.649 … the figures lean into each other. That is a stance/pitch problem, not a
lane-spacing one, which is why `LANE_SPREAD` cannot fix it."*

Measured on the corpus with an independently written reader (widest clear column run
splits the two bodies; centroid and extent per side, per band, as fractions of that
image's own figure height):

| | separation | left width | right width | gap |
|---|---|---|---|---|
| image 3, sash `y559..700` | 0.635 | 0.554 | 0.592 | 0.092 |
| image 3, skirt `y700..881` | 0.616 | 0.594 | 0.500 | 0.101 |
| image 4, sash `y532..674` | 0.596 | 0.495 | 0.582 | 0.098 |
| image 4, skirt `y674..856` | 0.583 | 0.572 | 0.575 | 0.086 |

**The corpus stands its duellists 0.58–0.64 figure heights apart. This lane stands them
`LANE_SPREAD × TILE_WIDTH / FIGURE_HEIGHT` = 1.55 / 1.70 = 0.912 apart, before any
stance.** The measured skirt separation of 0.79 is *already less* than the stand
separation, because the garments lean inward — so standing the figures up would move it
the wrong way, toward 0.91. No stance change can reach 0.62 from a stand separation of
0.91. To match the corpus, `LANE_SPREAD` would have to be about **1.05**, not 1.35.

That is a `Stage` finding, exactly as `Director.LANE_SPREAD`'s own note has said since
pass 2: *"either `TILE_WIDTH` rises against `FIGURE_HEIGHT`, or `BODY_HALF` grows to the
width the rig actually has and the lane spacing follows it."* It is the last untouched
structural cause of the corridor miss and it is handed to pass 5 as one.

**The half of item 5 that is real and unpaid: the foe's lower garment.** The review
measured 0.300 of a figure height against the corpus's 0.495 and the hero's 0.581. That
needs a per-figure rig parameter (`SamuraiRig.build(skirtWidth)` plus the matching cloth
rails, which are shared with the simulation) and it was not attempted — the budget went
to items 1–4. Worth roughly 0.10 of the 0.32 excess.

### 6.1 The review's `LANE_SPREAD` sweep does not reproduce with the long blade

The review shot 1.35 and reported *"the blades never come closer than 0.0903 of a figure
height — the signature beat is destroyed"*, and the brief formally withdrew pass 2's
instruction on that basis. Re-shot at this commit (`out/captures/rev-p4-spread135`, same
window, same harness, `LANE_SPREAD = 1.35`, everything else shipped):

| | min blade separation | merged frames | corridor |
|---|---|---|---|
| review's 1.35 (blade 0.40) | 0.0903 | 10 of 24 | 4 of 24 pass |
| **this 1.35 (blade 0.55)** | **0.0068** @ f9 | **8 of 24** | 0 of 24 pass |
| shipped 1.55 (blade 0.55) | 0.0182 @ f11, merged f9/f10 | **1 of 24** | 0 of 24 pass |

**A 0.55 nagasa reaches across a 1.35 lane and the parry survives it.** The half of the
refusal that still holds is the other one: 8 of 24 frames are one connected ink mass
against 1. The instruction not to lower `LANE_SPREAD` stands — for the merge, not for the
blades — and half the argument behind it is now void.

---

## 7. Protected results, re-measured

### 7.1 Phrase continuity — HOLDS, 192× the control

**The instrument had to change and both readers are reported.** Pass 3's reader took "the
largest 8-connected component with luminance < 95". On a dusk sky the whole upper frame is
below 95, and the largest dark component is the ground band. The replacement is row-local:
ink at **0.60 × the row's own background**, 3×3 opening, largest component **taller than
it is wide** (which is what separates a figure from the ground smear), silhouette
resampled by area-average into a 64×64 grid over its own bounding box, mean absolute
difference of consecutive grids.

| capture | steps | min | p05 | median | max |
|---|---|---|---|---|---|
| `s4-p4-null-static` (static control) | 23 | 0.00000 | 0.00000 | 0.00002 | **0.00005** |
| `s4-p3-phrase-60hz`, new reader | 417 | 0.00466 | 0.01055 | 0.06395 | 0.34135 |
| `s4-p4-phrase-60hz` | 417 | **0.01056** | 0.01879 | 0.07269 | 0.38999 |

The hero's silhouette never comes to rest at any of 417 consecutive 1/60 s steps, at
**192×** the control's noise ceiling (pass 3 on the same reader: 85×; pass 3's own debt
reported 43× on its absolute-threshold reader). Tracked boxes: frame 0 `x120..168
y562..645`, frame 150 `x237..393 y339..488`, frame 300 `x385..530 y330..482`, frame 390
`x140..208 y494..573`. Global minimum at step 256, t = 4.28 s. Longest run below 0.02:
**5 steps = 0.083 s** against pass 3's 14 steps = 0.234 s.

### 7.2 The held breath — HOLDS, at spec on all three scenes

`RehearsalTest.theHeldBreathIsAtSpecOnEveryScene`, unchanged, green: 0.850× over 0.25 s
per ramp on `duel-parry`, `duel-knockback` and `duel-phrase`. Observed red at
`Timing.HELD_BREATH_SECONDS = 0.12` — see §9.

### 7.3 The blade trail — HOLDS, no kink, and it is finally visible

Local-background residual `L − uniform_filter(L, 61)` on frames 2/6/10/14, crop
`x250..760 y150..470`, amplified ×18: **a single continuous smooth arc on every frame.**
No polyline kink, no strobing, no discrete blade poses. §5's "must curve" and §7.2's
"smear, not strobe" both still pass.

What changed: it now reads **1.050–1.070× its own row background** (frame 6 apex
`(450,215)` 62.6 against 58.6) where three passes recorded 1.027× above paper and called
it invisible. And it has a taper for the first time. It is **still a near-closed dome a
figure height across** — the shape is unfixed and is recorded as debt in §8.

---

## 8. What this pass did not do, with the number beside it

Named rather than dropped, per the standard the pass-2 review set.

- **§7.0.1 — the pelvis has exactly 0.0000 figure heights of horizontal motion relative
  to its own stance**, in all three scenes, for a fourth pass. Hip path / hand path 1.1%
  (parry), 0.6% (phrase); System 2 was failed at 1.5%. Not re-measured this pass; nothing
  touched the mechanism. The fix is a directive that translates and rotates a body, which
  is architecture. **Permanent System 4 debt**, handed to whoever owns the directive
  vocabulary.
- **The chain still arrives together.** Hips and shoulder peak on one 1/120 s sample,
  elbow/hand/tip on another. §10's last row. Not touched.
- **The blade trail's shape.** Still a near-closed dome roughly a figure height across.
  It is now tapered and visible; the *extent* is unfixed. The mechanism is that
  `TRAIL_SECONDS = 0.48` of history at a tip speed of 18 units/s is a very large arc, and
  a fix is a cap on accumulated angular sweep rather than on time.
- **The embers: 2–5 blobs ≥ 4 px against §5's 8–20.** Unchanged.
- **§10's fail-on-sight rows.** The flat-shaded polygon facets on the foe's head and
  shoulder and the hard-edged quadrilateral flecks are still there and still visible at
  5× on frame 9, crop `x500..720 y260..440`. Not touched.
- **The planning framing's composition.** `s4-p4-phrase-60hz` frame 0 is still two ~85 px
  figures in the bottom-left of an 85%-empty frame. It is now an 85%-empty *dusk sky*
  rather than an 85%-empty cream sheet, which is not the same as fixed.
- **The ground band is 15–25 luminance levels too light** below world y +0.3 (56–60
  against the corpus's 27–39). §1.
- **The foe's blade is a stub**: 0.211 of a figure height against the hero's 0.420.
- **The dark duellist is on STYLE.md's own value floor and still 2.5× too light against
  its sky.** §3. This needs §2.2 revisited, not a shader.
- **`Director.FIST_DROP` is untouched at 1.0** and is now the named lever for the torso
  regression of §2.1.
- **The knockback was not re-shot.** `Duel.Kind.KNOCKBACK` goes through the same dusk
  stage, the same blade and the same base colour, and none of it was measured.
- **§7.3's ink bloom still does not appear in a delivered frame.** `Duel.Kind` needs a
  fourth entry resolving a `Hit`.

### What was not measured at all

- The **matched-scale part count of §11.0** was not re-run this pass. The composition
  statistics (blade span, value ratio, corridor profile) all moved into the corpus's
  range, and a part count is the reviewer's first act; this pass did not pre-empt it.
- **The corridor profile was not run on the phrase**, only on the parry window.
- The **`head` and `feet` bands remain reader-unstable** and neither decides the picture.
- Nothing was measured about the **knockback**.

---

## 9. Every guard, and the proof it was observed red

§11.2b(f): *no assertion counts as a guard until it has been observed red.* Six, each
broken at the thing it watches, each message read out of the JUnit report, suite restored
green afterwards.

| # | guard | broken by | printed |
|---|---|---|---|
| A | `RehearsalTest.everyClashIsDrawnWhereTwoBladesActuallyAre` | `Scheduler.CLASH_SPAN` 0.42 → 0.90 | *"PARRY: the clash that starts at t=1.568 is still drawn at t=1.6468 with the two blades 6.2% of a figure height apart. A bloom is an assertion that they are meeting; on this frame it is false."* |
| B | `RehearsalTest.theHeldBreathIsAtSpecOnEveryScene` | `Timing.HELD_BREATH_SECONDS` 0.25 → 0.12 | *"PHRASE: the held breath runs 0.125 s per ramp over 5 ramp(s). STYLE.md 7.3 asks for ~0.25 s and Timing.HELD_BREATH_SECONDS is 0.12. (Span per ramp measured end to end: 1.040 s.)"* |
| C | `CorridorProfileTest.everyFamilyBImagePassesTheBandTheProjectFailsCapturesOn` | `torso` floor 0.011 → 0.014, the floor pass 3 shipped | *"STYLE.md 11.0: the corpus must pass the criterion the corpus set, and the corpus is the whole family that depicts the situation. image 4's torso band reads 0.011834319526627219 against a band of 0.014..0.015."* |
| D | `CorridorProfileTest.aPairOfBodiesFourTimesTooFarApartFailsTheCeiling` | `skirt` ceiling 0.102 → 1.000, i.e. floors only | *"a corridor of 0.405 of a figure height is four times the corpus's own ceiling of 1.0 and must fail. Under a floors-only criterion it scored a pass."* |
| E | `DuellistValueTest.theTwoDuellistsAreTellableApartInDeliveredPixels` | pointed at `s4-p3-parry-contact/frame_011.png`, the last cream capture | *"…The corpus reads 3.2x to 9.32x; this reads capture: dark 0.228 through x385..465 y415..478 (81x64), pale 0.806 through x645..720 y425..488 (76x64), ratio 3.53x"* |
| F | `CorridorProfileTest.everyReadingCarriesASecondOneAtAFixedThreshold` | `CorridorProfile.FIXED_FACTOR` 0.60 → 0.85 | *"the fixed reading is supposed to be a second opinion and it agrees with the first on every band; that makes it decorative."* |

The clash guard's **placement** half is still two theorems given the gap assertion —
`drawnCrossing` is the midpoint of the closest approach between the two blade segments,
so `markToBlade` is exactly half the gap by construction. Pass 3's caveat is kept
verbatim: **treat the placement half as a regression trap for a desynchronisation, never
as evidence about placement.** The load-bearing assertion is `bladeGapFraction() <= MET`
on every drawn frame, and spot-check A is what set `CLASH_SPAN` this pass.

Known-answer rather than red-observed, and labelled as such:
`theProfileReturnsTheGapItIsToldToMeasure`, `theProfileSaysOneMassRatherThanZeroWhenTheBodiesTouch`,
`theRowBackgroundFollowsAGradedSkyWhereAPaperLevelCannot`,
`ParryWindowTest.theGradedParryWindowPutsAFigureHeightAt329Rows`.

---

## 10. The apparatus (§11.2b)

### 10.1 The dynamic control, which is what §11.2b(g) asks for

Shot `duel-parry` twice at this commit, same harness, same arguments:

| pair | differing px of 16,588,800 | max channel delta | frames |
|---|---|---|---|
| `s4-p4-null-static` / `s4-p4-null-static-repro` (static) | **0** | 0 | none |
| `s4-p4-parry-contact` / `s4-p4-parry-repro` (**the graded scene**) | **983** (0.14% of one frame) | **19** | 1 (frame 18, box `x445..556 y373..487`) |

The non-determinism the pass-3 review discovered **reproduces and is not zero**, and it is
much smaller here: 983 px on one frame against 13,545 px on four frames with a peak delta
of 122, and **the graded frame 11 is bit-identical between the two runs this time**. The
differing pixels sit in the shed flecks and smoke around the contact, as before.

It is still not characterised as a *distribution* — two runs is two runs — and any
absolute pixel claim about an individual fleck near a clash remains unreproducible. The
static null control provably cannot witness it and is kept only for what it does
establish: that the readback path renders the same twice.

### 10.2 Cross-reader

Every number in §1, §2, §3, §6 and §7 was taken with an independently written
NumPy/SciPy/PIL reader — 8-connected labelling, row-local background as the median of the
outer 70 columns, 3×3 opening, Rec. 709 weights as `Frame.java` declares them. Where the
checked-in tool is used it is named and the command is given. The two agree on the corpus
readings to the integer pixel and disagree on nothing quoted here.

---

## Commands, so the next pass does not have to reconstruct them

```
./gw capture  -Pscene=duel-parry       -Pout=out/captures/s4-p4-parry-contact \
              -Pframes=24 -Pcols=6 -Pstart=1.42 -Pstep=0.0167 -Pw=960 -Ph=720
./gw capture  -Pscene=duel-parry       -Pout=out/captures/s4-p4-parry-repro   (identical: the dynamic control)
./gw capture  -Pscene=duel-parry-debug -Pout=out/captures/s4-p4-parry-contact-debug   (same window)
./gw capture  -Pscene=rig-bindpose     -Pout=out/captures/s4-p4-null-static \
              -Pframes=24 -Pcols=6 -Pstart=0.0 -Pstep=0.0167 -Pw=960 -Ph=720
./gw capture  -Pscene=rig-bindpose     -Pout=out/captures/s4-p4-null-static-repro     (identical)
./gw capture  -Pscene=duel-phrase      -Pout=out/captures/s4-p4-phrase-60hz \
              -Pframes=418 -Pcols=22 -Pstart=0.0 -Pstep=0.0167 -Pw=960 -Ph=720

./gw analyse  -Pargs="blades   out/captures/s4-p4-parry-contact --span 0,348,960,329 --max 0.02"
./gw analyse  -Pargs="corridor out/captures/s4-p4-parry-contact --profile --span 0,348,960,329"
./gw analyse  -Pargs="diff     out/captures/s4-p4-parry-contact out/captures/s4-p4-parry-repro"
./gw test --tests '*RehearsalTest*'
./gw test --tests '*CorridorProfileTest*'
./gw test --tests '*DuellistValueTest*'
./gw test --tests '*ParryWindowTest*'
```

**Give `--span` on every Family B capture.** Without it the detected figure box spans the
ground smear and both frame edges, and every ratio in this document is wrong by 30–50%.

`analyse corridor --min 0.06` — the whole-column form — remains **deprecated as an
acceptance** and says so in its own help text.

`out/captures/rev-p4-spread135` is kept: it is the `LANE_SPREAD = 1.35` probe of §6.1 and
it is the evidence that half of the review's refusal argument no longer holds.
