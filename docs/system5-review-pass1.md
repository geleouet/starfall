# System 5 pass 1 — review

# FAIL

Not on the thing the builder was most afraid of. **The no-text interface is readable — I read
all five delivered glyphs correctly, cold, before opening `Glyph.java`.** It fails on three
other things, one of which is the most serious defect this review found anywhere: the guard
that certifies the pass's central material claim is blind to the entire class of shape it
names, and a bordered HUD panel drawn into `LaneInterface` passes the whole 401-test suite.

Reviewer's standing: I did not build this and have no stake in any decision in it. Every
number below was taken with an independently written NumPy/PIL reader (Rec. 709 weights, per
`Frame.java`), not with `analyse`, per §11.2b(c); every number is printed beside the rectangle
it was taken through, per §11.3; every claim about the interface is a difference against the
`-bare` control shot at the same harness (`harness=f0ad18994eec`, `commit=1564fcf-dirty`).

---

## 0. §11's required items, in order

**1. Verdict:** FAIL.

**2. The one-sentence test** — *could this frame be cropped out of one of the eight reference
images and not look out of place?* The pass is trying to match **Family B** (dusk duel) at the
execution framing and claims **Family C** (misty field) at the planning framing, per §9.

- Execution framing (`s5-p1-fold-strike` frame 4): **nearly yes.** Two figures at ~280 px,
  indigo-to-coral sky, blades reading pale, hair dissolving. It would survive the crop.
- **Planning framing (`s5-p1-fold-plan` frame 3): no.** This is the frame the interface is
  graded on and it is not a picture from the corpus. Nothing in the eight images is 95%
  smooth gradient with three 90 px figures on a strip.
- 15-tile framing (`s5-p1-approach-plan`): emphatically no.

**3. What is missing:** **not evocative enough** at the planning framing — the graded shot is a
diagram on a nice background, which is the builder's own phrase and it is the right one — and
**technically broken** in the certification, not in the delivered pixels.

**4. Why it matters** and **5. concrete changes** are §§1–8 and the pass-2 brief.

---

## 1. The central question: can a human tell the five actions apart?

### 1.1 The cold read

I could not use `s5-p1-fold-plan` for this: `system5-debt.md` §1.1 states its column top-to-base
as `[PARRY, THRUST, SWEEP, STEP, CUT]`, so by the time I reached the frames I had been told the
answer. I used **`s5-p1-knife-plan` frame 2**, whose stanza contents appear nowhere in the debt,
cropped `x40..149 y40..409` at 4×, and wrote the five readings down before opening `Glyph.java`:

| reading position | what I wrote, cold | `Glyph.java` says | correct? |
|---|---|---|---|
| 1 (top) | *a chevron; two strokes meeting at an apex; a deflection, so a parry* | `PARRY` | ✔ |
| 2 | *a broken horizontal line — a line interrupted rather than stopped* | `THRUST` | ✔ |
| 3 | *a large arc, nearly a circle, open at the lower left — a sweeping cut* | `SWEEP` | ✔ |
| 4 | *a long rising stroke with a short foot mark below it — two footfalls, a step* | `STEP` | ✔ |
| 5 (base) | *one steep falling diagonal — a single decisive cut* | `CUT` | ✔ |

**Five of five.** And the order read without being taught: the top mark is visibly the boldest
and the base mark visibly the faintest, so "read downward" is where the eye starts anyway.

**Honesty about my own coldness.** My brief had already told me that Sweep is a 230° arc, that
Thrust is an interrupted line and that Feint is a dry Step, so readings 2 and 3 are not fully
unaided. Readings 1, 4 and 5 were unaided, and reading 1 — chevron → *deflection* → Parry — is
the one that most surprised me, because I arrived at it from the shape alone.

**So the no-text argument survives.** I will not fail this pass on it, and the finding should be
recorded as strongly as the failures: the claim *"no test says a human can tell a Step from a
Sweep"* now has a human answer, and the answer is that they can.

### 1.2 …but the reading is only correct because the alphabet is the beat

Checked against `combat-design.md` §2.2's choreography column, four of the five marks *are* the
sentence in that column, and one is its opposite:

- **Sweep** — *"one continuous arc through two bodies"*. A 230° arc. The best mark in the set and
  the one I identified fastest.
- **Cut** — *"the baseline stroke"*. One decisive downstroke. Correct.
- **Step** — *"weight transfer"*, drawn as *"a short heavy mark where the weight leaves and a long
  one where it arrives"*. That is why I read "two footfalls" and not "an arc".
- **Thrust** — *"blade passing through a body, not stopping at it"*. The mark is what the thrust
  *leaves*, which is a correct and rather good idea. What the mark cannot say is *two tiles
  ahead*; the reach is not in the picture. Acceptable — §2.2 asked for the beat, not the rule.
- **Parry — the mark contradicts its own entry.** §2.2 says *"a deflection curve rather than a
  collision"*, and `§3d`'s closing note spends a paragraph establishing that reading the meeting
  as a point *"collapses the deflection to a point, which is the collision the whole document
  exists to forbid."* The glyph is `line(-0.46,-0.30 → 0.14,0.26)` and `line(0.48,-0.24 →
  0.02,0.34)`: **two straight segments meeting at a sharp apex.** A corner is what a collision
  looks like. The code comment defends it — *"neither of them stops there — both carry through"* —
  and delivered there is a ~4 px overshoot past the apex, but at the shipped 71 px cartouche the
  dominant read is a chevron, which is exactly what I wrote down. It is the only mark in the set
  that draws the thing its own design document forbids, and it is the **signature beat**.

### 1.3 The defect: two tiles have the same mark

`Glyph` authors right-facing and mirrors when the hero is turned, and the class doc gives the
reason: *"so the column shows which way the phrase points."* Mirroring maps one tile onto
another.

```
COLLISION: [STEP facing right, BACK_STEP facing left]
COLLISION: [STEP facing left,  BACK_STEP facing right]
distinct shapes: 16 of 18 (tile, facing) pairs
```

Verified by enumerating every `(TileType, facing)` pair and comparing the exact vertex arrays
`Brush` is handed: `Glyph.of(STEP, -1)` is **vertex-identical** to `Glyph.of(BACK_STEP, +1)`.
Not similar — identical.

Why this is not a nicety. Step and Back-step are the only two movement tiles on a linear lane,
and forward-versus-back is the single most consequential thing a player commits to. To
disambiguate the mark you must first read the hero's facing — and at the planning framing the
hero is **86 px tall and I could not tell which way it faces** (§3.2). So the one distinction
the mirroring exists to make is the one it destroys, in the one shot where you make it.

Nothing watches this. `everyTileTypeHasAMark` — labelled in §6 as known-answer rather than
red-observed, correctly — asserts that a mark *exists* and that mirroring preserves the *stroke
count*. It never asserts that two tiles are different pictures. That is the one property a
no-text alphabet cannot do without.

### 1.4 Three things I suspected and could not substantiate — recorded because they failed

§11.2b(g)'s discipline applied to a reviewer's hunches:

- **"Dryness is overloaded: it means both *this is a Feint* and *this resolves later*."** Measured:
  the queued-drying channel spans `dryness` 0.03 (top) to 0.13 (base), and `FEINT` adds a
  constant 0.58 on top, landing at 0.61–0.71. The two uses of the channel do not overlap and are
  not confusable. **Refuted.**
- **"A Feint will read as an empty slot."** Peak lift over the local row median, box `x61..130`,
  the five column boxes: written marks **92.7 / 82.5 / 78.1 / 70.8 / 69.0**
  (`s5-p1-fold-plan` frame 3) against empty impressions **20.9 / 16.8 / 14.1 / 12.0 / 35.3**
  (`s5-p1-fold-empty` frame 2). The strongest impression is half the faintest written mark, and
  the shapes differ (a flat ellipse against a stroke). **Refuted — the empty state works.**
- **"The LIFO gradient is confounded by the alphabet."** Rasterised all nine glyphs from the
  recorded geometry at *identical* wetness (`alpha 0.95, dryness 0.03`, 71 px box): peak alpha
  **0.745 / 0.746 / 0.748 / 0.751 / 0.745** for Sweep / Thrust / Parry / Cut / Step — a spread of
  **0.8%**. The peak statistic is flat across the vocabulary, so the delivered 2.64× → 2.16× fall
  is wetness and nothing else. **Refuted; see §4.**

---

## 2. §8 compliance, line by line

| §8 line | verdict | evidence |
|---|---|---|
| No chrome; no boxes, bevels, bars with borders, drop shadows, glassy gradients | **PASS in delivered pixels, NOT ENFORCED** | §2.1 and §5.1 |
| UI elements are brush marks **on the paper** | **FAIL** | §2.3 |
| Action queue as ink cartouches | **PASS** | §1.1; three cartouches became five per `§1.1a` |
| Health as a column of ink strokes | **DIVERGED, and the divergence is right** | §6, challenge 2 |
| Tile grid a row of faint wash marks intensifying near relevant tiles | **PASS** | lane band `x0..959 y505..559`, live L 43.73 vs bare 39.44 = 1.109×, peak lift +84.8 at (172,535) = 3.58× — reproduced exactly |
| Enemy telegraphs the one place vermillion is spent freely; wet-bleed arrival, not a fade | **PASS** | §2.4 |
| Type: a serif or brush face, never a UI sans | **PASS, vacuously** | there is no type |
| Transitions are washes and bleeds, never slides or pops | **PASS** | §2.5 |

### 2.1 "Nothing it draws can print an edge" — in delivered pixels, this holds

The brief asked for this in pixels, not geometry, because §3's first rule is the one this
project has failed most often. Method: `D = luminance(live) − luminance(bare)`, then the largest
single-pixel step `max|∂D|` in either axis, over **every frame of every capture that has a
control**, expressed as a fraction of that frame's peak `|D|` — a hard edge is a step that
consumes most of the amplitude in one pixel.

| capture | worst 1-px step | peak \|D\| | step / amplitude |
|---|---|---|---|
| `s5-p1-fold-plan` (6 frames) | 24.9 | 99.6 | **0.249** |
| `s5-p1-fold-strike` (12 frames) | 20.1 | 82.8 | **0.242** |
| `s5-p1-fold-pushin` (48 frames @0.0167) | 24.9 | 100.5 | **0.247** |

Controls, measured on the **bare** frame so they are the same harness and the same pixels:

| control region (bare, `fold-plan` frame 3) | worst 1-px step |
|---|---|
| blades and figures, `x360..539 y470..519` | **81.6 / 91.0** |
| left figure, `x220..289 y420..519` | **129.4 / 130.6** |

So the interface's steepest transition consumes a quarter of its own amplitude — a mark that
takes four-plus pixels to arrive — while the blades it is drawn beside step five times harder.
**Nothing the interface delivers prints an edge.** This is the first pass in the project to
demonstrate §3's first rule on its own output rather than assert it, and it deserves saying.

One qualification: at the execution framing the lane pools reach the frame border, `|D|` up to
26.9 on the left and right columns. That is a mark cut by the viewport, which the ground and
sky also are; it is not a printed boundary. Non-fatal.

### 2.2 …and the guard that certifies it is blind to what it names — **the finding of this review**

`InterfaceInkTest.nothingInTheInterfaceHasAHardEdge` asserts, per triangle:

```java
assertTrue(a == 0f || b == 0f || c == 0f, …)
```

Its javadoc claims the scope:

> *"every triangle a brush emits has at least one vertex at exactly zero alpha, so no triangle
> can print a boundary of its own. **A panel, a bar, a border, a rounded rectangle or an icon in
> a frame all fail it on the first triangle, whatever they are shaded with.**"*

That sentence is false. A triangle with vertex alphas `(0, α, α)` prints a hard edge along the
`α–α` side. A rectangle drawn as `(TL,TR,BR)` + `(TL,BR,BL)` with **only `TL` at alpha 0** gives
both triangles a zero corner and passes.

Demonstrated. I inserted into `LaneInterface.sheet` a filled rectangle spanning
`x 0.60..1.00, y 0.60..0.90` in frame heights, one corner at alpha 0 and the other three at
0.95 — a HUD panel with a hard border, drawn by the interface, in every frame of every bout:

```
./gw test --tests '*InterfaceInkTest*'   →  BUILD SUCCESSFUL
./gw test                                →  BUILD SUCCESSFUL   (401 tests)
```

**A bordered panel passes the entire suite.** The property `Brush` has is real — its two rims
sit on alpha 0 either side of the spine, so the `α–α` edge of every triangle it emits is
*interior* — but the assertion does not test that property. It tests a strictly weaker one that
`Brush` happens to satisfy.

This is §11.2b(f)'s named mechanism exactly, one turn further out than the section describes.
Guard A **was** observed red — I reproduced it: giving `Brush.stroke`'s left rim the spine's
alpha fires it with the message §6 quotes. Observed-red is necessary and it is not sufficient.
A guard can be red-able against the defect its author had in mind and blind to the class its
own docstring claims. And §11.2b(f)'s closing warning applies with full force: *"a vacuous one
is worse than none. It does not merely fail to catch the defect — it certifies it, and it
persuades the next reviewer to stop looking."* `system5-debt.md` §1.6 device 3 and §6 row A both
cite it as proof; the delivered pixels are clean, and the certificate is not.

### 2.3 "Brush marks on the paper" — the substrate is not delivered, and it is not declared

`combat-design.md` §3's final bullet: *"Everything sits on the aged-paper substrate of STYLE.md
§3b.3, so the interface is marks on the same sheet the figures are painted on rather than a
layer above it."*

Measured — standard deviation of a 3 px high-pass, which is what paper tooth is:

| region | high-pass sd |
|---|---|
| S5 planning sky, `x300..699 y120..319` (bare) | **0.813** |
| reference image 3 sky, `x80..699 y80..239` | 1.144 |
| reference image 1 paper, `x60..699 y60..299` | **12.853** |

There is no paper in the frame — **16× less surface than the corpus's paper ground**. The sky is
correct for Family B; the *substrate the interface is required to sit on does not exist on this
stage*. That is not a bug, it is a fourth contradiction between the two design documents, of
exactly the kind the pass raised three of. It is absent from `§4` (corrections) and from `§5`
(what this pass did not do). The pass owed this one and did not file it.

Delivered, this is visible: in `s5-p1-fold-empty` frame 2 the right-hand marks read as pale
scratches floating in the sky beside the bokeh motes. The `runnel` — *"a ground of their own so
they are not floating in the sky"* — is a good instinct and it is far too faint to do the job.

### 2.4 Vermillion, and the wet-bleed arrival

Strikethrough on `s5-p1-fold-plan` frame 3, pixels where the live−bare `r−b` lift exceeds 20:
**897 px, 0.130% of the frame**, strongest pixel live `RGB(148, 63, 61)` against bare
`RGB(63, 62, 74)`, lift `(+85, +1, −13)`. Reads as vermillion (`#C8382E`) laid thin over a dark
sky. §2.2's *"a few small marks per frame"* is met with a wide margin, and the debt's 0.141% is
within threshold choice of mine. Guard B, which ties every vermillion vertex to a tile the
engine says is threatened rather than to the numbers the interface drew with, is a properly
crossed guard.

### 2.5 "Never slides or pops" — the recession

Verified in two independent ways.

*In the schedule*: guard C's ceiling is derived rather than chosen — the steepest slope of
`Ease.SLOW_IN_SLOWER_OUT` over `Timing.CAMERA_MIN_MOVE × 60` frames = 0.1185. I broke it by
making `Readout.intimacy` return 1.0 inside a beat and 0.0 outside, and it fired:
`theInterfaceRecedesContinuouslyBecauseTheCameraDoes() FAILED`. The 1.0000-in-one-frame figure
is real.

*In delivered pixels*, over the changed set, mean `|channel delta|`:

| capture | share of frame | mean \|delta\| over changed px |
|---|---|---|
| `fold-plan` | 4.92–4.94% | 15.47 → 15.75 |
| `fold-pushin` (48 frames @0.0167) | 4.94 → 6.65% | 15.75 → 11.21, monotone |
| `fold-strike` | 5.76 → 7.09% | 13.46 → **9.96** |

The interface covers more of a close frame and is thinner everywhere it covers, exactly as §3.2
argues, and the argument is honest about giving both halves. Reproduced.

---

## 3. The composition, and §11.0's matched-scale part count

§11.0 says this is the first act of every review. The pass did not run it. Run.

### 3.1 The count

Reference image 3 (Family B, the stage's own template). Dark duellist spans `y285..950` = **665
px**. Capture: `s5-p1-fold-plan` frame 3, hero detected against the row median at 12 levels,
bounding box `x225..270 y429..514` = **86 px**. Reference downscaled by 86/665 = 0.129 and
cropped to its own duellist; both viewed at 5× nearest-neighbour.

| | readable parts at 86 px |
|---|---|
| **reference image 3** | topknot, head **with a face** (brow, nose, jaw), neck, collar, shoulder, upper arm, forearm, two hands, grip, guard, blade, sash, second sheathed blade with its red cord, skirt with fold structure, forward leg, rear leg, foot, dissolving hem — **~16** |
| **capture (planning framing)** | hair mass, head as a lump, torso, ochre sash band, a second ochre mark, blade, shed flecks, a lower mass tapering to a point — **~8** |

**Half.** No face, no hands, no grip, no guard, no second blade, no separated legs, no feet. This
is the same shortfall §11.0 was invented to expose, at the same order of magnitude, on the frame
this pass is graded on.

At the **execution** framing (270 px) the capture reaches ~13 against the reference's ~19 at the
same scale — a far better showing, and the reason the execution frame nearly passes §0.

**This is not System 5's to fix and I am not failing System 5 for it.** The figures are System
4's, unchanged, and §2.2's own note already owns the value-floor half of it — *"a figure that
reads too pale against a dark ground is a fault of this line and not of the pass reporting it."*
Confirmed: figure-to-sky contrast is **2.22×** (darkest 3% 26.7 against local sky 59.3, box
`x225..270 y429..514`) against reference image 3's **7.47×**. What *is* System 5's is that it
chose the planning framing as the shot its interface lives in, and that shot is the weak one.

### 3.2 Is the planning view a picture, or a diagram on a nice background?

**A diagram on a nice background.** The builder's own words, and they are correct. With numbers:

- **The planning frame is 95.2% featureless.** On the *bare* control, only **4.84%** of pixels
  differ from their own row's median by more than 8 luminance levels (2.34% by more than 15).
  Everything — three duellists, the horizon, the ground smear, every mote — is 4.84% of the
  frame. At the execution framing the same statistic is **16.61%**.
- **The interface is 43% as much visible content as the entire rest of the world.** Same >8
  criterion, live vs bare: interface **2.09%** against world **4.84%**. At the execution framing
  it is 0.17. When the camera is wide, nearly a third of what you can see is the UI.
- **§9's planning framing is not delivered as specified.** §9 asks for *"the full lane readable,
  figures small, heavy fog, Family C mood."* Full lane readable ✔, figures small ✔ — and there is
  **no fog and no Family C mood**. The 86 px hero holds 32.5 luminance of contrast against its
  local sky where the 280 px hero holds 38.4: the figure at a third of the size retains **85%**
  of its contrast. Atmospheric attenuation is essentially zero. Family C is *"background figures
  desaturate and half-dissolve into mist"*, and nothing here desaturates.
- **The 15-tile lane is worse.** `s5-p1-approach-plan` frame 2: figures ~65 px, three specks on a
  line, the entire upper-left quadrant held by the interface alone.
- **The hand column reads at 1.016× its control** in the graded shot. Reproduced. The builder
  calls it *"correct behaviour, bad picture"* and that is the right description: the right third
  of the graded frame does no work.

One impression of mine that the measurement refused, recorded per §11.2b(g): I wanted to write
*"the interface out-reads the fight."* It does not. Peak deviation from the row median —
stanza column `x61..130 y52..400` **91.8**, health row `x30..194 y8..47` **98.0**, all three
figures `x215..529 y420..529` **149.3** — the blades still hold the frame's strongest contrast in
all three lanes. The composition problem is not that the interface shouts; it is that it is the
only thing in half of the picture.

**The good frame is `s5-p1-knife-plan`.** At five tiles the figures are ~180 px, the lane band
reads as real ground, the Strikethroughs are two red streaks lying *on* that ground, and the
picture nearly works. The composition dial has a good setting and the graded scene is not it.

---

## 4. The LIFO legibility claim

**The statistic reproduces exactly.** `s5-p1-fold-plan` frame 3 against `-bare`, boxes `x61..130`
at the five column pitches, my own reader:

| reading position | tile | peak lift | at | ratio | debt says |
|---|---|---|---|---|---|
| 1 (top) | PARRY | +94.8 | (80,94) | **2.643×** | +94.8 at (80,94), 2.64× |
| 2 | THRUST | +85.6 | (76,155) | 2.481× | +85.6 at (76,155), 2.48× |
| 3 | SWEEP | +78.8 | (102,205) | 2.301× | +78.8 at (102,205), 2.30× |
| 4 | STEP | +72.6 | (103,304) | 2.239× | +72.6 at (103,304), 2.24× |
| 5 (base) | CUT | +68.8 | (98,366) | 2.157× | +68.8 at (98,366), 2.16× |

To the pixel and to the last decimal, monotone, and — per §1.4 above — **not confounded by the
alphabet**: at identical wetness the five glyphs' peak alphas span 0.745 to 0.751.

**Does a monotone statistic read as an order to a human eye?** Partly, and the honest answer is
more useful than a yes.

- **As a rank of five: no.** Adjacent pairs are 6%, 7%, 3%, 4% apart in peak lift. I cannot rank
  positions 2 against 3, or 4 against 5, by looking. Nobody can.
- **As a direction: yes.** Top versus base is 94.8 against 68.8 and the top mark is plainly the
  freshest thing in the column. That reads instantly, and I read it without being told.
- **And a direction is all the device has to carry**, because position does the ranking. The
  player needs to know *which way to read*, not to rank five marks by wetness. Judged against
  its actual job the device works, and §3's claim — *"the player never learns LIFO, they just
  read downward"* — is delivered.

Two cautions on how the claim is stated:

1. **The perceived gradient is partly size, not wetness.** Delivered ink area (`lift > 4`) over
   the same boxes falls **2320 / 1537 / 832 / 536 / 401** — a **5.8×** fall against the peak's
   1.22×. Peak is clean; the *impression* of the gradient is dominated by area, and area is
   heavily alphabet-dependent (at fixed wetness, Sweep 624 px against Thrust 318 px). If the
   player had banked Thrust at the top and Sweep at the base, the picture would look far less
   like a gradient than this one does. The device is on a knife edge that the delivered stanza
   happens to sit on the right side of.
2. **§1.1's flood table inherits that.** *"4.7× the ink area of the strongest queued mark"* —
   Sweep resolving at 2142 px against Step queued at 457. At identical wetness Sweep already
   carries 1.41× Step's area, so the like-for-like figure is **~3.3×**, not 4.7×. The claim
   survives comfortably; the number is inflated by the vocabulary the very next guard (G) was
   fixed to normalise out.

Guards G and H are, for what it is worth, the best-built pair in the table: both normalise each
cartouche by its own full-wetness ink, and §6's note that breaking H exposed the fault in G is
§11.2b(f)'s mechanism working as intended and is worth the project keeping.

---

## 5. Guard spot-checks — five broken, all five genuinely red

Baseline `./gw test` green (401 tests, 2 skipped — see §5.2). Each break applied alone, suite
restored and re-verified byte-identical afterwards.

| # | guard | how I broke it | result |
|---|---|---|---|
| A | `nothingInTheInterfaceHasAHardEdge` | `Brush.stroke`'s left rim given `alpha * p` instead of `0f` | **RED**, `InterfaceInkTest.java:53`, message as quoted in §6 |
| C | `theInterfaceRecedesContinuouslyBecauseTheCameraDoes` | `Readout.intimacy` returns 1.0 inside a beat, 0.0 outside | **RED** |
| D | `theCameraReachesTheWideFramingInsideThePlanningGap` | `returnWide(idleAt)` → `returnWide(cursor())`, i.e. the pre-System-5 code | **RED**, and *the only failure in the whole suite* |
| H | `theColumnDriesDownwardFromTheMarkThatResolvesNext` | `Readout.QUEUED_DRYING` 0.13 → 0.00 | **RED**, `StanzaColumnTest.java:217` |
| I | `anEmptyStanzaStillPrintsEveryLineOfItsColumn` | empty-slot loop bounded at `written` | **RED**, `StanzaColumnTest.java:251` |

All five failed with the message they were written to print, and each break fired only its own
guard. The §6 table is honest about what it did.

Two structural observations:

- **E and F cross properly.** E compares the interface's written order against the sequence of
  `CombatEvent.BeatBegan` the *engine* emitted — a genuinely independent path, not a recomputation
  from the numbers the renderer used. F reads `Recorder.meanY()`, the geometry the renderer was
  handed. Neither is a bit-identity check wearing an assertion's clothes.
- **A is red-able and blind** — §2.2. This is the review's headline. Being observed red does not
  establish a guard's *scope*, only that some path to it exists. §11.2b(f) should gain the
  corollary.

### 5.1 Does anything in the suite catch a HUD?

No. With a bordered filled panel drawn into `LaneInterface.sheet`, all 401 tests pass. §1.6's
device 3 — *"a panel, a bar, a border or an icon in a frame fails on the first triangle"* — is
the pass's own strongest non-HUD argument and it is unenforced.

### 5.2 Assertions that read artefacts git does not publish

**System 5's nine guards are clean.** `InterfaceInkTest` and `StanzaColumnTest` touch no files at
all; they run headless off `Bout`, `Readout` and `Recorder`. No System 5 assertion depends on a
capture.

**But two assertions in the suite are silently skipping right now**, which the brief asked me to
check for specifically:

```
SKIPPED dev.starfall.analysis.DuellistValueTest theTwoDuellistsAreTellableApartInDeliveredPixels()
SKIPPED dev.starfall.analysis.DuellistValueTest thePaleDuellistStillPoolsToTheFloorBelowTheSash()
```

Both gate on `Assumptions.assumeTrue(CAPTURE.isFile())` for
`out/captures/s4-p4-parry-contact/frame_011.png`. That path is matched by `.gitignore:22`
(`out/**/frame_*.png`) and is not force-added — `git ls-files` says *"Did you forget to
'git add'?"* — so it is absent from any clone **and absent from this disk**, where only
`frame_010.png` was published. The harness prints `BUILD SUCCESSFUL`. These are System 4's, not
System 5's, and they are exactly the defect the brief warned about, live and green today.

---

## 6. The camera bug — real, and the fix does not touch System 4

Verified by fingerprinting every schedule with the fix and with `returnWide(idleAt)` reverted to
`returnWide(cursor())`, and by sweeping every inter-beat gap at 60 Hz for the widest framing
reached.

| | with the fix | reverted |
|---|---|---|
| S4 `PHRASE` | `dur=6.9521 md5=1fbdec73a04c098cd38b5ac6694cb461` | **identical** |
| S4 `PARRY` | `dur=3.0400 md5=8b23d7496b6b82b52814ca7d95dce702` | **identical** |
| S4 `KNOCKBACK` | `dur=3.1548 md5=7b24e2ed64635c6ff5c8ae2bc467e03d` | **identical** |
| S5 `KNIFE` first gap 10.143–12.743 | widest **6.56** vs plan 6.50 | widest **3.20** — never wide |
| S5 `FOLD` first gap 10.143–12.743 | widest **12.50** vs plan 12.50 | widest **3.25** — never wide |
| S5 `APPROACH` first gap 10.143–12.743 | widest **16.50** vs plan 16.50 | widest **3.25** — never wide |

And the full suite on the reverted code fails **exactly one test**, guard D, with every System 4
assertion green — which is the defect's own claim about itself, independently demonstrated.

**The defect is real and the fix is correct. One claim about it does not reproduce.** §4.4 says
*"STYLE.md §9's 'wide to plan' therefore never happened"*, and the brief repeats it as *"never
happened in any score with more than one command."* Reverted, each bout has **two** qualifying
planning gaps and only the **first** is missed; the second reaches the full planning framing in
all three bouts. The bug costs one planning gap of two per score, not all of them. Also worth
recording: reverted, `KNIFE`'s duration moves 20.1462 → 21.3722 s, so the defect changed timing
as well as framing.

---

## 7. Claims in `system5-debt.md` that do not reproduce

The most valuable output of a review here. Five, in descending order of consequence.

1. **§6 row A / §1.6 device 3 — the guard's stated scope.** *"A panel, a bar, a border, a rounded
   rectangle or an icon in a frame all fail it on the first triangle, whatever they are shaded
   with."* **False.** A filled rectangle with one alpha-0 corner passes guard A and passes the
   whole suite. §2.2 above.

2. **§4.4 — *"'wide to plan' never happened."*** Overstated by half: one of two planning gaps per
   score. §6 above.

3. **§3.2 — the execution diff range `39,826–46,999 px`.** The minimum reproduces to the pixel;
   the **maximum does not**. My reader gives **49,030 px (7.093%)** under "any channel differs"
   and 45,153 under "luminance differs by ≥1" — no threshold I tried yields 46,999. The interface
   covers ~2,000 px more of the closest frame than reported. Separately, **the document
   contradicts itself on the planning minimum**: §1.6 says 33,998 and §3.2 says 34,128 for the
   same quantity. My reader: **33,998–34,158**. §1.6's low end is right.

4. **§5 — *"[Feint and Draw] are asserted to exist and to survive mirroring
   (`everyTileTypeHasAMark`)."*** The test asserts that mirroring preserves the **stroke count**.
   Nothing asserts a mark survives mirroring *as itself*, and for `STEP`/`BACK_STEP` it provably
   does not. §1.3 above.

5. **§4.5 — the cooldown-tick count is quoted without a control**, against the pass's own §7
   instruction: *"Quote a `-bare` control beside every number about the interface."*
   `s5-p1-fold-replan` has no `-bare` sibling, so the +5-luminance threshold is against the sky.
   The finding itself **does reproduce** (§8, challenge 3) and it is stronger than stated.

**And one undeclared divergence**, which belongs in §4 or §5 and is in neither: the aged-paper
substrate `combat-design.md` §3 requires is not in the frame — high-pass sd 0.813 against
reference image 1's paper at 12.853. §2.3 above.

**Claims I checked and which reproduce exactly**, recorded because a review that only lists
failures is not a measurement: the §1.1 wetness table to the pixel; the lane band 1.109× and
+84.8 at (172,535); the health row 2.79×; the §3.1 bit-reproducibility (**0 differing pixels over
6 planning and 12 execution frames**, including the moving window — better than any previous
pass in this project); the recession's 15.76 → 9.96; the first three rows of §1.5's pool-pitch
table (77.7/90.7/159.5 px, mine 79/89/157). I could not reproduce the last two rows of the
pool-pitch table — at a 3.2-tile framing only three pools remain on screen and my peak finder
splits them — and I have no grounds to dispute them; that is my apparatus, not their number.

**And one claim verified in pixels that the pass argued only structurally.** §1.6 device 1, *the
interface is drawn before the figures*. `s5-p1-fold-strike` frame 4, lane band `y560..699`:
under body ink (7,694 px, bare luminance more than 18 below the row median) the interface's
contribution is **mean 0.00, with 0.00% of those pixels above 4**; on open ground beside them
(38,296 px) it is mean 4.09 with 14.59% above 4. The figures occlude the interface completely.
Same test on `fold-plan` frame 3: 0.00 under ink, 4.41 on open ground. **The strongest of the
four non-HUD devices, and it holds.**

---

## 8. The three design-document challenges

### Challenge 1 — `combat-design.md` §3 "new tiles enter at the top" vs `STYLE.md` §8 "never slides"

**UPHELD.** The contradiction is real: with a fixed set of five lines and the newest written into
the top one, every existing mark moves down the sheet on every bank — five slides per stanza,
which §8 forbids by name. Neither document says which end is nailed down, so neither is wrong;
they are jointly under-specified.

The resolution is correct and it **preserves the owner's chosen orientation exactly**: the
column is vertical, the newest mark is topmost, the topmost resolves first, and the player reads
downward. Nothing about what the owner saw and chose changes. What changes is that the column is
anchored at its **foot** and grows upward, so no mark ever moves. Verified in the picture, not
only in the model: guard F measures the alpha-weighted centre of each cartouche drawn alone and
asserts the column descends as it is read, and it fires when either the comparator or
`STANZA_PITCH` is inverted.

Adopt the proposed wording into `combat-design.md` §3. **One amendment**: the wording should also
say what the *unwritten* lines are, because base-anchoring makes the column's length carry
information — the five impressions are how a player sees how many more tiles they may bank, and
guard I exists to protect that. Suggested addition:

> The lines not yet written carry the impression the nib leaves — fainter than any mark, and
> strongest on the line the next tile will land on — so the column always states its own capacity
> and where the next mark goes.

### Challenge 2 — §8's "health is a column of ink strokes"

**UPHELD, with one correction to the delivery.** The argument is sound and it is §3's own
argument rotated: with the stanza owning the vertical axis, a second vertical run of similar
marks beside it is precisely the confusable pair §3 warns about. §8 was written before §3 chose
vertical, so §8's line is the older of the two and should yield.

Delivered as a row at the head of the stanza it reads correctly — cold, I read the ten short
strokes at `x30..194 y8..47` as a tally, never as a queue, and the reading of the column was
unaffected. Measured: live L 65.31 against bare 55.35, **peak lift +99.6 = 2.79×**. Reproduced.

**The correction**: at +99.6 the health row is the **strongest mark the interface draws** —
louder than the top of the stanza at +94.8. The loudest thing in the margin is the one element
the player is *not* composing. Health is a state you check; the stanza is the thing you are
building, and it should out-read it. Rebalance so the stanza's freshest mark is the margin's
peak.

Amend `STYLE.md` §8 to: *"health is a row of ink strokes at the head of the stanza, drying and
fading as it drops, and never a second column beside it."* Divergence announced rather than
quiet is exactly right and the pass did that.

### Challenge 3 — §3d.3's cooldown compression

**UPHELD on the evidence, and the finding is worse than stated. The prescription is refused.**

Reproduced independently. `s5-p1-fold-replan` frame 2, hand rows at pitch 66 from `y30`, ticks
read over `x782..839` at +5 luminance over the row's own local median (`x700..789`):

| row | tick runs found | separable by eye at 4× | should be |
|---|---|---|---|
| 2 (SWEEP, cd 3) | 3 | 3 (1 ghost + 2 ochre) | 3 |
| 3 (THRUST, cd 3) | 3 | 3 | 3 |
| **4 (PARRY, cd 4)** | 4 — but one is a 1-px sliver at `x=839`, the region's own edge | **3** | **4** |

**It is not a lost tick, it is a wrong number.** A Parry at 2 charges of 4 delivers two ochre
marks and one legible ghost. A player counts **"two of three"** — they do not perceive an
omission, they perceive a different cooldown. That is a misread, not an under-read, and a
misread of the resource that decides whether a tile is bankable is worse than the debt argues.

**What I uphold**: the visual system cannot carry a 0–8 countable-mark scale at 960×720, and
this is a second and independent route to §3d.3's conclusion, arriving from the drawing side.
Record it in `combat-design.md` §3d.3 as such.

**What I refuse**: compressing the cooldown scale is not the drawing layer's prescription to
make. §3d.3 already says both its items need a playable fight; a rendering constraint at one
resolution is not a reason to change a mechanic, and `system5-debt.md` §5 concedes the interface
*"has never been shot at any resolution but 960×720"*. The obligation that falls on pass 2 is
narrower and entirely within its own layer: **a mark that is counted must be countable at every
resolution the game ships, or it must stop being a count.** Either widen the tick pitch (12.2 px
is under two tick widths) and give the ghost ticks enough separation from the sky to survive a
+5 reader, or replace the run with a mark whose *state* rather than whose *cardinality* is read.
Then shoot it at a second resolution and prove it.

---

## 9. Pass-2 brief

Ranked by impact. Items 1 and 2 are the FAIL; 3 is the picture; 4–6 are debts this pass opened.

1. **Make guard A test the property, not a proxy for it.** The property is *no triangle has an
   edge whose two endpoints both carry ink and which is not shared with another triangle at the
   same alphas* — i.e. no **silhouette** edge carries ink. Assert that, and prove it observed red
   against the bordered panel in §2.2, whose exact construction is written down there so pass 2
   can paste it in as a fixture. Do not delete `Brush`'s one-vertex-at-zero property; it is a
   true and useful invariant. Delete the sentence claiming it catches panels, or make it true.
   Then re-run the §2.1 delivered-pixel measurement — steepest 1-px step as a fraction of
   amplitude, against the blade in the same frame — and check it in as a test, because that
   measurement is the one that actually convicted nothing and it should be the guard of record.

2. **No two tiles may be the same picture.** Add the assertion that `Glyph.of(t, s)` is distinct
   for all 18 `(tile, facing)` pairs — the enumeration in §1.3 is the whole test — and observe it
   red on today's code. Then re-author `BACK_STEP` so it is not `STEP` mirrored. The design note
   *"retreat has no verb, so the mark is the Step's own gesture read the other way"* is elegant
   and it is the bug; give the retreat its own asymmetry (the foot mark that *arrives* rather
   than the one that leaves, a drag, a heel). And **redraw `PARRY`** as the deflection curve
   `combat-design.md` §2.2 asks for: the two strokes should meet on a curve and part again, not
   turn a corner. The signature beat should not draw the collision the rubric forbids.

3. **Make the planning framing a picture.** This is the §0 failure and it is a `Stage` question,
   as the pass correctly says — but it is System 5's shot and pass 2 owns it.
   - **Deliver §9's "heavy fog, Family C mood."** Today the 86 px figure keeps 85% of the
     contrast the 280 px figure has. Attenuate the figures toward `Fog #D6D2CE` as a function of
     framing width, so that going wide costs contrast the way distance does. This is one dial and
     it is the cheapest large change available: it fixes "95% smooth gradient" by making the
     gradient *do* something, and it is the section of the corpus this framing was always meant
     to be quoting.
   - **Reconsider `EDGE_MARGIN` / `INTIMACY_TILES`.** At 11 and 15 tiles the figures are 86 and
     ~65 px. `s5-p1-knife-plan` shows the composition works at ~180 px; find the framing law that
     keeps the figure near that on every lane, even at the cost of not showing the whole lane.
   - **Give the sheet a substrate, or file the contradiction.** §2.3. If a Family B dusk stage
     cannot carry `§3b.3`'s paper, say so in `combat-design.md` §3 as the fourth correction and
     replace it with something the stage *can* carry — the `runnel` is the right idea at three
     times its present strength.
   - **Shoot the graded planning frame with a full hand.** The right third at 1.016× is a third of
     the frame doing nothing. Stage a bout where the stanza is full *and* tiles remain charged.

4. **Fix the two silently skipped assertions** (§5.2): `git add -f
   out/captures/s4-p4-parry-contact/frame_011.png`, or repoint them at a published frame, or make
   them refuse rather than assume. §11.2b(e) is explicit that a discipline the tool does not
   enforce is documentation; an `assumeTrue` on an unpublishable path is worse — it is a green
   tick for an assertion nobody has run since the day it was written.

5. **Correct `system5-debt.md`** on the five items in §7, and add the paper-substrate divergence
   to §5. §4.4's "never happened" should read "the first planning gap of every score"; §3.2's
   execution maximum should be re-measured; §1.6 and §3.2 should agree on the planning minimum.

6. **Two things nobody has measured yet** and which pass 2 should not inherit unexamined: the
   Strikethrough's wet-bleed arrival and a spent mark's drying have never been captured at
   `-Pstep=0.0167`, so §8's *"elements arrive by pigment spreading into place (0.4–0.7 s)"* is
   unverified in delivered frames; and `FEINT`, `DRAW`, `TURN`, `BACK_STEP` and every enchantment
   mark have never appeared in a capture. Four of the nine letters of the alphabet have never
   been through the renderer. Stage a bout that banks them.

---

## 10. What this pass got right, stated plainly

A FAIL verdict should not bury this, because most of it is new to the project.

- **The no-text interface is readable.** Five of five, cold, including the order. That was the
  open question and it now has an answer.
- **§3's first rule is demonstrated on delivered output for the first time.** Steepest 1-px step
  0.242–0.249 of amplitude across 66 frames, against blades at 81–130 in the same pixels.
- **The scenes are bit-reproducible, including the moving execution window** — 0 differing pixels
  over 18 frames. §11.2b(g) asked for the control that can express the property and this is the
  first pass to shoot it.
- **The interface is genuinely occluded by the figures**, proved in pixels over 7,694 px of body
  ink at exactly 0.00 delta.
- **A real staging defect was found, fixed, and shown not to disturb System 4** — three
  byte-identical fingerprints.
- **Five guards broken by a reviewer, five genuinely red**, each firing only its own assertion.
- **Three design-document contradictions raised rather than quietly resolved**, all three upheld.

The pass did not declare itself a pass, and it was right not to.

---

*Reviewed against `STYLE.md` rev. 1 (§0, §2, §3, §8, §9, §10, §11.0, §11.2, §11.2b, §11.3),
`docs/combat-design.md` §2.2, §3, §3b, §3d, and the eight images in `inspirations/`.
Captures read: all 13 `s5-p1-*` directories. Suite state at review: 401 tests, green, 2 skipped.*
