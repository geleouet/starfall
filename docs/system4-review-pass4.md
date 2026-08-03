# System 4 — pass 4 review (independent)

**VERDICT: FAIL.**

One pass remains after this one. This review is written to be spent in one pass, and the
ranked brief in §7 is four items long with the first two named as the ones that must land
if only two fit. §8 is what I would ship as permanent debt, and it is deliberately long —
closing System 4 with that list recorded is the right outcome and the reason the cap
exists.

Everything below was measured by me, on the delivered pixels, with an independently
written NumPy/SciPy/PIL reader (`rev4_reader.py`, `rev4_corridor.py`, `rev4_cont.py`) that
shares no code with `src/main/java`. Every number carries its rectangle (§11.3). The
suite was green before I touched it (388 tests, 0 skipped, 0 failures) and is green after.

**Apparatus in scope (§11.2b(c), (a)).** My reader was calibrated against a synthetic card
before it was pointed at anything: a graded dusk-like field with two cool-bright bars whose
nearest pixel centres are 41 px apart, drawn **only in rows 200–339 of a 720-row frame**.
It reads 41.00 px and finds 0 mask pixels in rows 360–719. No y-flip, no bleed, no
off-by-one. It then reproduced pass 4's `blades` minima to four decimals on four separate
captures and pass 4's corpus corridor readings to the pixel, so where it disagrees below,
the disagreement is not the instrument.

---

## 1. The three adjudications

### 1.1 `LANE_SPREAD = 1.35` no longer destroys the parry — **UPHELD, and it goes further
than pass 4 claimed**

Independent reader, cool-bright components (`L > 212`, `b − r > −6`), 8-connected,
normalised by the 329-row span `0,348,960,329`, minimum over the 24 frames of each capture:

| capture | blade nagasa | min two-cloud separation | at frame |
|---|---|---|---|
| `rev-p3-spread135` (pass 3's probe) | 0.40 | **0.1298** | 20 |
| `rev-p3-spread120` | 0.40 | 0.0510 | 9 |
| `s4-p3-parry-contact` (1.55) | 0.40 | 0.0317 | 9 |
| `rev-p4-spread135` | 0.55 | **0.0068** | 9 |
| `s4-p4-parry-contact` (1.55, shipped) | 0.55 | 0.0182 | 11 |

Pass 4's 0.0068 reproduces exactly. The pass-3 review's 0.0903 was taken through the
406-row span; through the corrected 329-row span my reader puts pass 3's 1.35 probe at
0.1298. Either way the ruling is the same: **with a 0.55 nagasa the blades meet across a
1.35 lane, and they meet more tightly than they do across the shipped 1.55.** Half of the
pass-3 refusal is void, exactly as pass 4 says.

**But the other half — the one pass 4 kept — does not survive.** §6.1's table gives
"merged frames: 8 of 24 at 1.35, 1 of 24 at 1.55", and the sentence it draws is *"The
instruction not to lower `LANE_SPREAD` stands — for the merge, not for the blades."* Run
consistently, with the project's own checked-in tool:

```
./gw analyse -Pargs="corridor out/captures/rev-p4-spread135      --profile --span 0,348,960,329"
   -> 0 of 24 frames pass every band; 1 are one mass
./gw analyse -Pargs="corridor out/captures/s4-p4-parry-contact  --profile --span 0,348,960,329"
   -> 0 of 24 frames pass every band; 1 are one mass
```

and without `--span`, also consistently:

| `LANE_SPREAD` | one-mass frames, **with** `--span` | one-mass frames, **without** |
|---|---|---|
| 1.55 (shipped) | 1 of 24 | **17 of 24** |
| 1.35 (probe) | 1 of 24 | **8 of 24** |

My independent reader agrees: 1 of 24 at 1.35, 1 of 24 at 1.55 (span-cropped, factor 0.85,
3×3 opening, `BODY_SHARE = 0.10`).

**The "8 against 1" compares the 1.35 probe's un-spanned reading against the shipped
setting's spanned reading.** Under either reader taken consistently, 1.35 is **equal or
twice better** on merge, not eight times worse. Pass 4's own Commands section closes with
*"Give `--span` on every Family B capture. Without it … every ratio in this document is
wrong by 30–50%"*, and §6.1 is the one place in the document that breaks it.

So: **both halves of the refusal are void.** At 1.35 the blades meet tighter, the ink
merges no more often, the corridor is 0 of 24 either way, and side by side at frame 11 the
1.35 staging is the better picture — the crossing sits between the two figures instead of
crowding the foe against the right edge. Lowering `LANE_SPREAD` is the top structural item
in §7.

### 1.2 Reference image 5 sets no corridor floor — **UPHELD in its conclusion, WRONG in its
stated reason**

Span-cropped, factor 0.85, 3×3 opening, 8-connected, `BODY_SHARE = 0.10`:

| image | span | components ≥ 10% of span ink | largest | torso |
|---|---|---|---|---|
| 3 | `y283..955` | 2 (53.4% / 44.9%) | 144,417 px | 0.0149 |
| 4 | `y255..930` | 2 (49.4% / 48.9%) | 139,484 px | 0.0118 |
| 5 | `y255..930` | **1 (99.3%)** | 250,879 px | — no corridor exists |

Pass 4's image 3 and image 4 rows reproduce to four decimals (my `head` on image 3 reads
0.0936 against their 0.0847 — a one-row span difference, and both sit inside the adopted
0.084..0.162 band). **Image 5 is one mass and cannot set a floor.** The exclusion is
correct, it is asserted rather than described (`CorridorProfileTest`, which I broke and
watched go red — §5), and it is *not* self-serving: including image 5 could only drop the
`torso` floor to 0.000, and it is the `torso` **ceiling** of 0.015 that fails this capture.
The exclusion makes the criterion harder for pass 4, not easier.

**The stated reason is not right, though, and it matters because §11.0 asks for the reason
and not just the name.** `CorridorProfile.FAMILY_B`'s note says image 5's duellists are one
component *"at every threshold tried"*, and the debt says the 0.0000 was measured *"between
a duellist and the ground smear"*. Sweeping the ink factor with the span crop already in
place:

| factor | 0.40 | 0.45 | 0.50 | 0.55 | 0.60 | 0.85 | 0.95 |
|---|---|---|---|---|---|---|---|
| bodies ≥ 10% | **2** | **2** | **2** | 1 | 1 | 1 | 1 |
| torso | **0.0000** | **0.0000** | **0.0000** | — | — | — | — |

At factor 0.40–0.50, **span-cropped**, image 5 resolves two genuine duellists and its torso
corridor reads exactly 0.0000 — the number pass 3 quoted, from the two bodies, not from the
ground. So pass 3's substantive point survives in a weaker form: *the corpus does contain a
two-duellist painting whose torso corridor is literally zero, because their hilts touch.*
Pass 4 disposed of that by moving the threshold, not by refuting it. The conclusion stands;
the note is overstated and should be corrected to say **"at the operating factor 0.85, and
at every factor at which the two masses separate the corridor is 0.0000, so it sets no
floor either way."**

### 1.3 The pass-3 review's span was 23% too tall — **UPHELD**

Measured on `s4-p4-parry-contact-debug` frame 11, the same window and harness as the graded
capture: the drawn ground line occupies **rows 676–677** (960 of 960 columns darker than
paper at both rows; the next full-width row is 698, the frame's own baseline rule). The
four foot markers sit at `y628..642` and `y644..658`. Row 719 — what pass 3 used as "feet"
— is **43 rows below the drawn ground**. Pass 3's `y314..719` is h 406 against a true 329;
406/329 = **1.234**.

`ParryWindowTest.theGradedParryWindowPutsAFigureHeightAt329Rows` pins the same span through
`Schedule.framingAt` and `Stage.FIGURE_HEIGHT`, so the constant cannot rot. It is a
known-answer test and pass 4 labels it as one; my debug-frame reading is the independent
path to the same answer (§11.2b(c)).

**Numbers in the record that are wrong by this factor**, all of them *understating* gaps by
19%: every band fraction in the pass-3 review and in `docs/system4-debt.md`'s pass-3
columns; pass 3's published blade separation 0.0264 (→ 0.0344 through 329); the pass-3
review's `LANE_SPREAD` sweep figure 0.0903 (→ 0.1115, and 0.1298 on my reader); and the
pass-3 review's base-separation figure of 0.793 figure heights at the skirt.

**And that last one breaks pass 4's own §6 argument.** §6 quotes the review's 0.79
uncorrected and reasons: *"The measured skirt separation of 0.79 is already less than the
stand separation, because the garments lean inward — so standing the figures up would move
it the wrong way."* Measured by me, widest clear column run between the two body centroids,
side centroids per band, span-cropped:

| | sash separation | skirt separation |
|---|---|---|
| corpus, image 3 | 0.633 | 0.614 |
| corpus, image 4 | 0.595 | 0.582 |
| `s4-p3` f11 through the **406**-row span | 0.700 | 0.720 |
| `s4-p3` f11 through the **329**-row span | 0.873 | 0.804 |
| **`s4-p4` f11 through the 329-row span** | **0.856** | **1.048** |

(My corpus rows reproduce pass 4's §6 table to ±0.002, so the corpus reader is sound.)

Through pass 4's own corrected span, its own capture stands its duellists **1.048** figure
heights apart at the skirt — **above** the 0.912 stand separation, not below it. The
garments lean *outward* at the new blade length, because the longer `REACH_TO_CROSSING`
pushes both fists back. The premise of the sentence is inverted. Its conclusion —
*`LANE_SPREAD` would have to be about 1.05, and no stance change reaches 0.62 from 0.91* —
survives and is in fact strengthened: the delivered skirt separation is **1.7× the
corpus's**, not 1.3×.

---

## 2. §0, the one-sentence test, at matched scale

Reference image 3 downscaled by 329/672 so its figure height matches the capture's, beside
`s4-p4-parry-contact` frame 11 at native scale, both cropped to the same figure box plus
context.

**Answer: no.** Not for `s4-p4-parry-contact` frame 11, and not for
`s4-p4-phrase-60hz` frames 120–360. The nearest thing to a yes in either capture is
`s4-p4-phrase-60hz` frames 0 and 417 — the planning framings — where the sky, the ground
haze, the motes and two tiny figures do read like a crop of Family C, and where the figures
are too small to expose anything. That is a real gain and it should be said plainly: this
is the first pass in the project's history whose colour script belongs to the corpus.
Against Family B, which is what the parry is trying to match, the answer is still no, and
the reasons are specific.

### 2.1 The part count (§11.0)

At 329 px of figure height, reference image 3 resolves, per duellist: topknot, hair wisps
trailing, brow, eye, nose, lip, chin as one contour, jaw, ear, neck, kimono collar,
shoulder, upper arm, forearm, wrist, **both hands on the grip**, tsuka, tsuba, blade,
obi with its knot, a second sheathed blade with saya and kojiri, hakama panels with fold
lines, two legs, two bare feet, and ink-smear ground contact. Call it 25.

At the same scale the capture resolves, for the hero: a hair scribble, a head lump with a
pale-pink patch where a face would be, a shoulder, one arm, a fist, a hilt, a blade, an
ochre sash, an undifferentiated lower mass. Nine. For the foe: a black spiked hair mass, a
white amoeba where a face would be, a shoulder, an arm, a hilt, a blade stub, an ochre
sash, a leg, a boot. Nine. **Neither figure resolves a face, a hand, a guard, a second
blade, a fold, or a foot on the ground.** This is roughly a third of the reference's count,
which is where System 1 was five passes ago. §11.0's corollary applies: the material can
only be as good as the subject.

### 2.2 The clash — the specific thing a critic attacks, and it is worse than "sharp"

At 4× on frame 11, crop `x500..760 y250..450`:

- The rays are **flat-shaded polygon wedges** with straight aliased edges and hard
  terminating points, not a bloom with soft rays. Quantified: single-pixel luminance steps
  inside that box, against reference image 3's clash box `x300..560 y330..530` **downscaled
  to matched scale**:

  | | p99 |∆L| | max |∆L| | share > 60 | share > 100 |
  |---|---|---|---|---|
  | ref 3 clash, matched scale | 44 | 76 | 0.195% | **0.000%** |
  | `s4-p4` f11 clash | **52** | **134** | **0.506%** | **0.021%** |

  2.6× the reference's share of hard steps, a maximum step 1.8× the reference's, and
  pixels stepping over 100 where the reference has none. §5 asks for "4–6 long **soft**
  rays"; §10 fails on sight of "hard-edged sprites / visible polygon silhouettes" and
  "symmetric, uniform particle bursts". This is both.

- **The cluster of squares is literal.** Blobs in `x530..600 y290..400`, thresholded at
  8 L above the local median:

  | blob | size | bbox fill | aspect |
  |---|---|---|---|
  | at (555,343) | 7×7 | **0.98** | 1.00 |
  | at (577,343) | 3×4 | **1.00** | 0.75 |
  | at (562,349) | 7×8 | 0.89 | 0.88 |
  | at (570,349) | 8×8 | 0.78 | 1.00 |
  | at (549,353) | 8×8 | 0.81 | 1.00 |

  Filled axis-aligned quadrilaterals, all within 1 px of the same size, 60 px from the
  focal point of the frame. §3's failure signature list names "flecks that are all the same
  size"; §10 fails on sight of hard-edged sprites. This is the single most damaging object
  in the picture because of where it sits.

### 2.3 The pale duellist does not read as pale — it reads as transparent

Pass 4's §3 table reproduces **exactly** on my reader (threshold-free median over row
background, same rectangles):

| | dark | pale | ratio |
|---|---|---|---|
| ref image 3 `x205..285 y415..520` / `x555..635` | 0.128 (L 12.1) | 0.419 (L 39.8) | 3.28× |
| ref image 4 | 0.115 | 0.372 | 3.22× |
| ref image 5 | 0.134 | 1.245 | 9.32× |
| `s4-p4` f11 `x385..465 y415..478` / `x645..720 y425..488` | 0.314 (L 27.9) | 1.304 (L 116.4) | **4.15×** |

The arithmetic is right and the ratio is inside the corpus's spread. **The picture it
describes is not the corpus's picture.** In images 3 and 4 *both* duellists are silhouettes
darker than their ground — 0.128 and 0.419 — and "pale" means *less dark*. In the capture
one figure is 0.314 of its ground and the other is **1.304**, i.e. brighter than the sky it
stands in. The foe is not a pale silhouette; it is a hole in the sky. You can see the
gradient through its torso in `s4-p4-phrase-60hz` frame 240.

Two further things about that number:

- **Chroma.** Reference image 3's pale duellist medians `#1F283C`, hue 221°, sat 0.48 — the
  cool grey-indigo §2.1 calls `Cloth pale`. The capture's published box medians `#8D6E69`,
  **hue 8°** — warm. It is not a garment colour; it is horizon coral read through a
  translucent figure plus an ochre wash. §2.2's warm/cool opposition is satisfied by the
  hero and violated by the foe, which is warm-on-warm against the coral band.
- **Box sensitivity (§11.3).** Defensible alternative boxes inside the same figure read
  0.599 (`x610..690 y500..570`), 0.772 (`x660..740 y490..560`), 0.972 (`x640..700
  y470..530`), 1.035 (`x600..760 y420..600`, whole body) and 1.338 (`x630..700 y400..440`).
  The published 1.304 sits at the top of its own figure's range. The rectangle is honest
  about *being inside the figure* — pass 4 is right that the review's wide boxes were
  three-quarters sky — but it lands on the one ochre patch on the foe, and a 2.2× spread
  across defensible boxes is exactly the situation §11.3 was written for.

**None of this is chargeable to §2.2's conceded floor.** The dark duellist at L 27.9 sitting
2.5× too light against its sky is the rubric's fault and I am not counting it. The pale
duellist crossing to the wrong side of its ground is a different defect and it is the pass's.

### 2.4 The trail is a halo, and it is measurable

Local-background residual `L − uniform_filter(L, 61)` above 1.0, restricted to the sky
window `x200..800 y130..320` on frame 6 so no figure ink is included:

- one connected ridge, 10,721 px, box `x228..591 y143..319` — **364 px wide = 1.11 figure
  heights**
- best-fit circle: centre (430,305), **radius 162 px = 0.49 figure heights**, median radial
  residual 13.4 px
- **angular extent 177°** — a full half-circle, drawn in empty sky where no blade is

§5's trail is "a smooth ribbon following the blade's **swept path**, brightest at the
leading edge", fading over ~0.4 s. This is a ring. Pass 4 fixed its *visibility* and its
*taper* and names the extent as unfixed; I agree with the diagnosis and disagree that it is
low priority. In the phrase capture it is the second most conspicuous object in the frame
after the blades, on frames 120, 240 and 300.

There is no moon in the scene (`grep -rn moon src/main` returns only two comments about
this exact artefact) and the clash halo has no hard edge — its radial profile from (628,332)
decays smoothly from L 80 at r=60 to a flat 65 by r=100. **What reads as a moon disc is the
trail arc.** The defect is geometric, not photometric.

### 2.5 The ground third is fog where the corpus's is ink

Lower third of the figure span plus 30 rows, matched scale:

| | p01 | median | p99 | sd | p99−p01 |
|---|---|---|---|---|---|
| ref image 3, rows 358..497 (matched) | **11** | **31** | 102 | **24.6** | 91 |
| `s4-p4` parry f11, rows 568..706 | 27 | 48 | 95 | 18.0 | 68 |
| `s4-p4` phrase f240, rows 568..706 | 27 | 55 | 108 | 22.2 | 81 |

The p01 of 27 is §2.2's floor doing exactly what STYLE.md now admits it does wrong, and I
am not charging that. **The 27% deficit in standard deviation is not a floor problem** —
you can reach sd 24.6 while never going below 25.7 by adding marks, not by darkening. What
is missing is structure: the corpus's bottom third is splatter flecks, drips, wet blooms and
near-black strokes with the coral sky punching through gaps. The capture's is a soft
airbrushed gradient with grass strokes at roughly 2% contrast, into which the hero's robe
simply fades without terminating. §6's "the floor is not a floor, it is an ink smear with
grass strokes and wet blooms" is not delivered, and §3's "the bottom third of nearly every
figure is not a figure at all — it is ink smoke" is delivered as *fade*, not as *fray*.

This is why both figures read as floating, and it is the largest single readable-part gap
at matched scale after the faces.

### 2.6 The debt's own ground-band number is slightly mis-stated, in a way that matters

§1 says the sky is "15–25 levels too light below world y +0.3 (56–60 against 27–39)".
Row-background profile, median of the outer 70 columns, world y from the 329-row span:

| world y | ref image 3 | `s4-p4` f11 |
|---|---|---|
| +0.23 | 44.4 | **46.6** |
| +0.11 | ~38 | 54.5 |
| 0.00 | 36.3 | 59.7 |
| −0.07 | 19.6 | 58.7 |
| −0.13 | 28.5 | 52.0 |

At +0.3 to +0.2 the capture **matches** the corpus. The defect starts below +0.15, and it
is not "too light" so much as **the wrong sign of gradient**: the corpus keeps darkening
toward the frame bottom and the capture *brightens* to its local maximum at world y −0.04.
The bottom of every Family B image is its darkest region; the bottom of this frame is
brighter than its own horizon-adjacent band. That is a more actionable statement than
"15–25 levels" and it points at the mist strength low down, which is where pass 4 already
says the remaining term is.

### 2.7 Where the picture is genuinely good, and it should be recorded

- The **sky ramp** from world y +0.5 upward is within a few levels of image 3 and it is the
  first time the project's warm/cool opposition has been the right way round. That is a
  real, large, correctly-diagnosed win and it was deferred twice as cosmetic.
- The **blade** in the phrase capture (frame 240, upper left) is the single best object this
  project has drawn: a thin pale sliver, faintly cool, gently curved, with a soft
  directional glow and a taper. It is a corpus object.
- **Saturation discipline** holds. My spot checks agree with §1: the capture is broader and
  flatter in chroma than image 3 and does not reach it anywhere. Not a §10 hit.
- The **motes** and the fog banding read as Family C and belong.

---

## 3. The corridor — verified, and it should be declared debt

**Verified, with my own reader and with the project's:** `0 of 24 frames pass every band`
for `s4-p2-parry-contact`, `s4-p3-parry-contact`, `s4-p4-parry-contact` **and**
`rev-p4-spread135` alike, span-cropped at `0,348,960,329`.

Band medians over the contact window (frames 6–23), my reader, beside the fixed-0.60
second reading:

| band | p3 @0.85 | p4 @0.85 | p3 @0.60 | p4 @0.60 |
|---|---|---|---|---|
| head | 0.0851 | 0.1474 | 0.1292 | 0.2264 |
| torso | 0.0213 | 0.2173 | 0.2447 | 0.2523 |
| sash | 0.3906 | 0.3283 | 0.4498 | 0.4453 |
| skirt | 0.4058 | 0.2340 | 0.4103 | **0.4149** |
| feet | 0.5258 | 0.1125 | 0.5760 | 0.2644 |

Pass 4's headline reproduces at 0.85 and **collapses at 0.60, harder than pass 4 itself
reported**: the debt says `skirt` improves 22% at the fixed threshold; I read
0.4103 → 0.4149, i.e. **no geometric improvement at all, a shade worse**. `feet` genuinely
moves (0.5760 → 0.2644). `torso` at the fixed threshold barely moves (0.2447 → 0.2523),
which means pass 3's apparently-fine 0.0213 was itself photometric and the two bodies have
been ~0.25 apart at the hands through all three passes.

**The fixed-threshold second reading is the most valuable thing pass 4 built**, precisely
because the first thing it caught was pass 4, and the second thing it caught was pass 3's
number. It should be kept.

**The band criterion itself should be declared debt and receive nothing further.** Its
record: three passes have spent budget on it; the corpus passes it; the capture has never
passed a single frame of it in any pass; it has never once discriminated between two
settings of the project (0/24 at 1.35, at 1.55, at pass 2, at pass 3); its `sash` band is a
6% window resting on two paintings; its `head` and `feet` bands are reader-unstable by pass
4's own account and by mine (my `head` on image 3 differs from pass 4's by 10% on a
one-row span change). It is a correctly-built instrument measuring a property the renderer
cannot currently express, and continuing to tune against it is refinement of the wrong
thing (§11.0's corollary). Keep it running as a **diagnostic printout**; delete it as an
acceptance; spend nothing on the numbers it prints.

---

## 4. Protected results

### 4.1 Phrase continuity — the replacement reader is **not sound**, and 192× should be
withdrawn

I re-implemented the described reader — row-local ink at 0.60 × row background, 3×3
opening, largest 8-connected component **taller than it is wide**, silhouette area-averaged
into 64×64 over its own bounding box, mean absolute difference of consecutive grids. It
reproduces pass 4's pass-3 row **to five decimal places on all four statistics**:

| | steps | min | p05 | median | max |
|---|---|---|---|---|---|
| `s4-p3-phrase-60hz`, pass 4's table | 417 | 0.00466 | 0.01055 | 0.06395 | 0.34135 |
| `s4-p3-phrase-60hz`, my reader | 417 | **0.00466** | **0.01055** | **0.06395** | **0.34135** |
| `s4-p4-phrase-60hz`, pass 4's table | 417 | **0.01056** | 0.01879 | 0.07269 | 0.38999 |
| `s4-p4-phrase-60hz`, my reader | 417 | **0.00391** | 0.01825 | 0.05815 | 0.47713 |

The reader is the same instrument — it agrees exactly on one capture. **It does not agree
on the other, and the disagreement is 2.7× in the direction that flatters the pass.**

It is not the capture. I re-shot `duel-phrase` with the identical command
(`out/captures/rev4-phrase-repro2`, 170 frames, start 0, step 0.0167): frames 0, 50, 100,
150, 152, 156, 157, 158, 160, 165 and 169 are **bit-identical** across runs; frame 154
differs by 1,695 px, max delta 54. The pixels I measured are the pixels pass 4 measured.

**And the reader should not be used at all, because it does not know which figure it is
looking at.** Sampling every 8th frame of the 418, the tracked component ranges from
**1,464 px to 19,951 px** and is under 25% of the frame's ink on **23 of 53** sampled
frames, as low as 4%. At the step where I read the global minimum — step 156 — it has
latched onto a **2,352 px blob at `x632..691 y457..549`**, a 60×93 fragment, not the hero.
This is a two-figure frame read with no figure named, which §11.3 forbids by name: *"the
tool must refuse when the ink resolves into two components each above some share of the
total and no figure has been named. A silent wrong answer is worse than a refusal."* The
sentence *"the hero's silhouette never comes to rest"* is not supported by an instrument
that cannot say whose silhouette it has.

**What survives.** The old absolute-threshold reader really is void on a dusk sky, and pass
4 is right to have replaced it. The *conclusion* also survives on my numbers: 29 of 417
steps below 0.02, longest consecutive run **5 steps = 0.083 s** — which is pass 4's own
figure, reproduced. The figure does not come to rest. **Withdraw the 192×** — it is
min-over-static-control, and a static control's noise floor of 0.00005 is a measure of PNG
encoding, not of a figure at rest (§11.2b(g): the null must be able to express the
property). Quote the run-length instead; it is box-free, reader-stable and it reproduces.

### 4.2 Held breath — **HOLDS.** Guard B observed red under my hands, §5.

### 4.3 Blade trail — **holds as continuity, fails as shape.** Single smooth arc, no kink,
no strobing; and a 177° ring a figure height across (§2.4).

### 4.4 Blades meeting — **HOLDS.** My reader: min two-cloud separation 0.0182 at frame 11,
identical to the tool. Frames 9 and 10 are one cool-bright component; the union of pass 3's
two clouds at frame 9 covered the same box, so this is a merge of the same two objects, as
pass 4 says. Two caveats the record should carry: the "second blade cloud" at frame 11 is
**8 × 88 px, 290 px of area**, and across frames 12–23 it is 78–248 px. The tool is scoring
the separation between a blade and a *fragment*. And the `--max 0.02` acceptance prints
PASS at frame 9 on a merge, by convention — pass 4 says so, and it should be said in the
tool's own output, not only in the debt.

### 4.5 Bloom on the steel — **HOLDS.** Ember counts reproduce exactly: **5 / 3 / 2 / 2 / 2**
blobs ≥ 4 px on frames 9/11/13/15/17 (`r − b ≥ 40`, `L ≥ 150`) against §5's 8–20.

### 4.6 §7.0.1 — the pelvis

Not re-measured by pass 4 and not re-measured by me; nothing touched the mechanism, and the
fix is a directive that translates and rotates a body. I accept the declaration. It is
permanent System 4 debt and the honest framing is: **System 4 has never satisfied §7.0.1's
first positive, in four passes, and closing System 4 means closing it with that unpaid.**
That is a legitimate outcome — the item belongs to whoever owns the directive vocabulary,
not to a visual pass — but it should be recorded as a *System 4 verdict*, not as a to-do.

---

## 5. Guard spot-checks

Five of the six broken by hand at this commit, each message read from the JUnit XML, suite
restored green (388 tests, 0 skipped, 0 failures) afterwards.

| # | break | result |
|---|---|---|
| A | `Scheduler.CLASH_SPAN` 0.42 → 0.90 | **RED**, message verbatim: *"PARRY: the clash that starts at t=1.568 is still drawn at t=1.6468 with the two blades 6.2% of a figure height apart…"* |
| B | `Timing.HELD_BREATH_SECONDS` 0.25 → 0.12 | **RED**, verbatim: *"PHRASE: the held breath runs 0.125 s per ramp over 5 ramp(s)…"* |
| C | `torso` floor 0.011 → 0.014 | **RED**, verbatim, naming image 4 at 0.011834… against 0.014..0.015 |
| D | `skirt` ceiling 0.102 → 1.000 | **RED**, and it fails on `head` at 0.4050 MISS high rather than on `skirt`; the message interpolates the *broken* ceiling ("four times the corpus's own ceiling of 1.0"). Cosmetic, but the message names a band it did not fail on. |
| F | `FIXED_FACTOR` 0.60 → 0.85 | **RED**, verbatim: *"the fixed reading … agrees with the first on every band; that makes it decorative."* |

**Guard E is vacuous outside the author's working directory, and it is the one carrying the
pass's headline result.** `DuellistValueTest` reads
`out/captures/s4-p4-parry-contact/frame_011.png` behind `Assumptions.assumeTrue`. That file
is matched by `.gitignore`'s `out/**/frame_*.png` and **is not force-added** — `git ls-files`
shows only `capture.txt`, `contact-sheet.png` and `frame_010.png` for that directory.
Renaming the file and re-running:

```
<testsuite name="dev.starfall.analysis.DuellistValueTest" tests="4" skipped="2" failures="0">
```

`theTwoDuellistsAreTellableApartInDeliveredPixels` and
`thePaleDuellistStillPoolsToTheFloorBelowTheSash` **silently skip**. So
`DELIVERED_FLOOR = 4.00`, described as "a ratchet, set just under what pass 4 delivers so a
regression fails the build", does not exist on a clean clone, and neither does the protected
pass-2 skirt-pooling result. The pass-4 red observation for guard E — pointing it at
`s4-p3-parry-contact/frame_011.png` — is also not reproducible from the repository, because
that file is untracked too.

§11.2b(f)'s own words apply: *"a checked-in test is the strongest evidence this project
produces, so a vacuous one is worse than none."* This is a five-minute fix — `git add -f`
the two frames, or point the assertions at `frame_010.png`, which **is** tracked — and it
should be done in pass 5 regardless of what else is.

**A missing guard, and §11.2b(e) names the rule pass 4 broke.** `analyse corridor` runs
happily without `--span` and returns a wholly different answer (17 of 24 one-mass instead
of 1). The discipline is written into the debt's Commands section and into nothing that can
enforce it — *"a discipline written into a document but not into the tool that reads it is
documentation, not a guard"* — and the document that quotes that rule is the document whose
§6.1 breaks it. `analyse corridor` should **refuse** a Family B capture with no `--span`,
the way `track` refuses without an anchor and `drape --control` refuses four ways.

---

## 6. What is missing, in the vocabulary §11 requires

**Technically broken**, in the specific §10 sense of *fail on sight*, and **not evocative
enough** everywhere else.

The technically-broken part is small and local and it is at the focal point: filled
axis-aligned 7×7 squares 60 px from the clash, and a clash star built from flat polygon
wedges with aliased straight edges. §10 lists both and instructs a reviewer to fail on
sight, and this review does.

The not-evocative-enough part is the ground and the heads. At matched scale the capture
resolves about a third of the reference's readable parts, no face on either figure, no
hand, no fold, no foot, and a bottom third that is an airbrushed gradient where the corpus
has the busiest, darkest, wettest passage in the painting.

Why it matters against the references: Family B's picture is *two silhouettes standing in
ink*, and this is *two translucent shapes hovering in fog*. The sky is now right, which is
what makes the rest legible as a gap — three passes on cream paper could not have shown it.

---

## 7. The pass-5 brief — ranked, one pass long

**Items 1 and 2 are the two that must land if only two fit.** Items 3 and 4 are ranked
below them and named as debt if they do not.

### 1. Kill the two hard edges at the focal point. *(smallest, largest §0 return)*

- The shed flecks near the clash are **filled axis-aligned quadrilaterals**, 7×7 and 8×8,
  bbox-fill 0.78–1.00, all within a pixel of the same size. Route them through the dissolve
  §3 already implements for hems — soft threshold band, varied scale, rotated — or stop
  drawing them. Evidence: frame 11, `x530..600 y290..400`.
- The clash star's rays are **flat polygon wedges**. Replace with a soft radial falloff plus
  4–6 rays of *unequal* length and soft, non-linear edges. Target: no single-pixel step over
  100 anywhere in the clash box, and the share over 60 at or below the reference's 0.195% at
  matched scale.

These two are the only §10 fail-on-sight items in the frame and they sit inside 60 px of
each other at the point the whole composition directs the eye to.

### 2. Lower `LANE_SPREAD` to 1.35 and re-shoot the parry and the phrase.

The refusal is void (§1.1). At 1.35 the blades meet at 0.0068 against 0.0182, the ink merges
on 1 of 24 frames either way (8 vs 17 under the un-spanned reader — 1.35 is *twice better*),
the corridor is 0 of 24 either way, and the staging is visibly better: the crossing sits
between the two figures instead of crowding the foe against the frame edge. It is also the
only lever that touches the base separation, which is 1.048 figure heights at the skirt
against the corpus's 0.582–0.614 (§1.3).

`Director.LANE_SPREAD`'s own note says the structural answer is `TILE_WIDTH` against
`FIGURE_HEIGHT` or `BODY_HALF`. **Do not attempt that in pass 5** — it is a `Stage` change
with combat-design consequences. Change the constant, re-shoot, and hand the structural
form on. And **do not spend pass 5 on `FIST_DROP`** chasing the torso band; see §3.

### 3. Give the ground third its ink back.

Not a floor change — §2.2's floor is conceded and the delivered p01 of 27 obeys it. A
*structure* change: splatter flecks, drips, wet blooms and near-black strokes in the bottom
third, and grass strokes at more than 2% contrast. Target the reference's dispersion rather
than its floor: **sd 24.6 and p99−p01 = 91 through the lower third at matched scale**,
against the delivered 18.0 and 68. This is what makes both figures stop floating and it is
the largest readable-part deficit after the faces, which belong to 3b.

Second-order and cheap while in there: the mist's own strength below world y +0.15, so the
ground band stops *brightening* toward the frame bottom (§2.6).

### 4. Cap the trail's angular sweep.

Measured: 177° of arc, radius 0.49 figure heights, 364 px = 1.11 figure heights wide, in
empty sky. Pass 4 has already named the mechanism — cap accumulated angular sweep rather
than `TRAIL_SECONDS`. A ~60–80° cap would leave a smear and remove the ring. It is the
second most conspicuous object in the phrase capture.

### Free, and it should be done whatever else is not

- `git add -f out/captures/s4-p4-parry-contact/frame_011.png` (and the pass-3 frame the
  guard-E red observation used), or repoint `DuellistValueTest` at the tracked
  `frame_010.png`. Two assertions currently skip on every machine but one.
- Make `analyse corridor` **refuse** a capture with no `--span`.
- Correct `CorridorProfile.FAMILY_B`'s exclusion note: image 5 is one mass **at the
  operating factor**, and at the factors where it splits its torso corridor is 0.0000, so it
  sets no floor either way (§1.2).
- Correct `docs/system4-debt.md` §6.1's merged-frames row and the sentence it supports, and
  §6's "the garments lean inward" (§1.1, §1.3).

### Explicitly not in the brief

**Nothing on the corridor band criterion. Nothing on the value ratio.** Both are inside or
demonstrably indifferent, and both have consumed budget for three passes without
discriminating between two settings of the project. See §8.

---

## 8. What I would ship as permanent System 4 debt

Recorded, not fixed. This list is the deliverable of the cap.

1. **§7.0.1 — the pelvis has exactly 0.0000 figure heights of horizontal motion, four
   passes running.** Hip/hand path ratio 1.1% (parry), 0.6% (phrase) — pass 4's figures,
   carried forward, not re-measured here; System 2 was failed at
   1.5%. System 4's motion is **merely correct, not poetic**, and it closes that way. The
   fix is a directive that translates and rotates a body and belongs to the directive
   vocabulary, not to a visual pass.
2. **The corridor band criterion.** Sound instrument, corpus passes it, capture has never
   passed a frame of it in any pass, never discriminated between settings, two-image `sash`
   band, reader-unstable `head` and `feet`. Keep the printout as a diagnostic; delete the
   acceptance; spend nothing further. **Keep the fixed-0.60 second reading** — it is the one
   piece of this apparatus that has ever caught anything.
3. **The chain still arrives together.** Hips and shoulder on one 1/120 s sample,
   elbow/hand/tip on another. §10's last row, §7.0.3. Untouched this pass.
4. **The embers: 2–5 blobs against §5's 8–20.** Reproduced exactly.
5. **The blade trail's extent**, if item 4 of the brief does not fit.
6. **The foe's stub blade**: 0.211 of a figure height against the hero's 0.420, and its
   cool-bright cloud is an 8×88 px sliver on the graded frame. Needs a per-figure rig
   parameter.
7. **The foe's lower garment**: 0.300 of a figure height against the hero's 0.581 and the
   corpus's 0.495 — the pass-3 review's measurement, carried forward, not re-verified here.
   Same rig parameter as item 6.
8. **The knockback scene**, never shot on the dusk stage and never measured.
9. **The planning composition**: two ~85 px figures in the bottom-left of an 85%-empty
   frame. It is now a beautiful 85%-empty frame, which is not the same as fixed — though it
   is the closest thing in either capture to a §0 yes, so the urgency is lower than it was.
10. **§7.3's ink bloom** never appears in a delivered frame; `Duel.Kind` needs a fourth
    entry resolving a `Hit`.
11. **Faces.** Neither figure resolves one at 329 px. That is System 3b's and it is the
    single largest matched-scale part-count gap; it should be handed over with the count
    (9 parts against the reference's 25) attached.
12. **Capture non-determinism**, characterised further and downgraded: the phrase scene is
    bit-identical across two runs on 11 of 12 sampled frames (1,695 px, max delta 54, on
    one), and pass 4 measured 983 px on one frame of the parry against pass 3's 13,545 on
    four. It is real, it is small, and any absolute pixel claim about an individual fleck
    near a clash remains unreproducible. Not worth further budget.
13. **The pale duellist is brighter than its own ground (1.304) where the corpus's is darker
    (0.372–0.419) and cool where the capture's is warm.** The *ratio* gate is met; the
    *sign* is wrong. If item 3 of the brief lands, this may partly come with it; otherwise
    it ships.
14. **`Duellists.blades`' `L > 212` is an absolute pixel statistic** (§11.2b(d)) and
    happens to work only because cream paper is warm enough to fail the `b − r > −6` test.
    It is valid within the `f0ad18994eec` harness and void across a harness change. Worth a
    sentence in the tool's own help text.

---

## 9. Closing

Pass 4 did the hardest and most useful thing anyone has done to this project: it drew the
sky the rubric had specified since revision 1, which had been deferred twice as cosmetic,
and in doing so it exposed three structural defects — the trail, the bloom's white point,
and the value statistic — that were invisible on cream and had been mis-graded for three
passes. Its instruments are better than its predecessors': the threshold-free value
statistic, the fixed-threshold second reading and the two-sided band are all real
improvements, and five of its six guards go red under an outsider's hands with the exact
messages claimed.

It fails on three counts. The picture still answers §0 with no, and the two objects that
make that answer loudest — a polygonal starburst and a cluster of literal squares — sit at
the focal point and are cheap. Its refusal of the one structural instruction it was given
rests on a comparison that crosses spans, and measured consistently the instruction it
refused is the right one. And its headline continuity number does not reproduce from its own
capture, through a reader that cannot say which figure it is measuring.

One pass remains. Spend it on items 1 and 2, take 3 if it fits, and close System 4 with §8
recorded.
