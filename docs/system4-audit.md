# System 4 — audit of the closing record

**Scope.** This document audits `docs/system4-debt.md` **for accuracy only**. System 4 is closed
at pass 5 of 5; there is no pass 6, so nothing here is a prescription and there is no verdict.
`docs/system4-debt.md` is now a permanent inheritance document that Systems 3b, 3c and 5 will
build against without re-deriving its numbers. An error in it propagates into three systems that
can no longer see the evidence. That is the only thing this audit is about.

**I did not rewrite the record.** Everything below is a correction, a confirmation, or a flag.

**Apparatus.** Every number below was taken with an independently written NumPy/SciPy/PIL reader
that shares no code with `src/main/java` (8-connected labelling, row-local background as the
median of the outer 70 columns, 3×3 opening, Rec. 709 weights as `Frame.java` declares them).
Per §11.2b(a) it was calibrated first against a synthetic card rendered to a graded field with a
single asymmetric dark rectangle drawn **only in rows 90–229** of a 720-row frame: it reads the
bbox back as `y90..229 x137..276`, 19,600 px exactly, and **0 mask pixels in rows 240–719**. No
y-flip, no bleed, no off-by-one. Where the project's own tool is used it is named and its command
given. Every capture read carries `harness=f0ad18994eec`, so all comparisons are in scope per
§11.2b(d). No new captures were needed; nothing was shot.

---

## 1. Corrections — statements in the closing record that do not reproduce

Ordered by how much damage each does to an inheriting system.

### C1. Debt item 5 (→ **3c**): the trail is **0.69 figure heights**, not 0.82, and the "floor" that goes with it is wrong on the source

**Record says** (§2.4, item 5, §9): *"The blade trail is still an arc 0.82 figure heights
across"*; *"An angular cap cannot take the arc below ≈ 0.74 fh because the ribbon spans the
blade's whole length"*; *"the remaining fix is to draw the ribbon on the tip's swept path
(`InkSkinnedRenderer.rail`)"*.

**Three separate errors.**

**(a) The normalisation is forbidden by §11.3.** The raw widths reproduce exactly — largest
local-background residual ridge (`L − uniform_filter(L,61) > 1.0`), sky window `x100..860
y60..330`: `s4-p4-phrase-60hz` f108 = **432 px**, `s4-p5-phrase-60hz` f108 = **309 px**, f258 =
**331 px** (record: 432 / 309 / 325). But the record divides these by the *parry* captures' span
constants — 329 and 378 — on a **moving-camera phrase capture**. §11.3: *"every pixel statistic on
a moving-camera scene is a ratio to the figure's own span."* Measured, the figure's own span on
those frames is **398 px** (p4 f108), **450 px** (p5 f108) and **455 px** (p5 f258).

| | ridge width | record's fh | record's value | figure's own span | **corrected** |
|---|---|---|---|---|---|
| `s4-p4-phrase-60hz` f108 | 432 px | 329 | 1.31 fh | 398 px | **1.09 fh** |
| `s4-p5-phrase-60hz` f108 | 309 px | 378 | 0.82 fh | 450 px | **0.69 fh** |
| `s4-p5-phrase-60hz` f258 | 331 px | 378 | 0.86 fh | 455 px | **0.73 fh** |

The *relative* improvement survives (−37% either way). The **absolute number handed to 3c does
not: the delivered arc is 0.69 fh, not 0.82.**

**(b) The claimed floor is therefore already breached by the delivered picture.** The record says
an angular cap *"cannot take the arc below about 0.74 figure heights"* and that *"the delivered
0.82 is within 0.08 of that floor."* Correctly normalised the delivered arc is **0.69 — already
below the stated floor.** The floor is not a constraint on anything.

**(c) The prescribed fix is already implemented.** The floor is derived as *"0.55 fh of blade plus
0.19 fh of rails"*, on the premise that *"the ribbon spans the blade's whole length"*. It does
not. `InkSkinnedRenderer.drawTrail` declares `rail = {-0.28f, -0.20f, -0.12f, -0.05f, 0f, 0.04f}`
and `emitTrailRow` applies it as `float d = len + rail[j] * width` — i.e. **offsets in absolute
world units back from the tip**, spanning 0.32 units ≈ 0.19 fh. The method's own comment says so:
*"A ribbon following the tip, not the whole swept sector … Offsets are absolute world units back
from the tip."* The 0.55 fh of blade is not part of the ribbon. **3c would go looking for a `rail`
change that is already in place.** What actually sets the arc's width is the number of retained
poses — which the record's own §2.4 item 1 identifies correctly.

**(d) Convention unrecorded (§11.3).** The record does not say whether the blade's own cool-bright
mask is excluded from the ridge. It matters: on f258 the ridge is 331 px (0.73 fh) unmasked and
**248 px (0.55 fh)** with the blade masked. `InkSkinnedRenderer`'s own javadoc, quoting the same
measurement, says the mask *is* excluded — and reports **0.58 fh** for frame 108 where the record
reports 0.82. The source and the record disagree on the same constant, the same frame and the
same window.

### C2. Debt items 4 and 9 (→ **3b**, **3c**): the focal-point defect is mis-named, and it is a field of filled axis-aligned quadrilaterals

**Record says** (item 4): *"The figures' silhouettes still print flat-shaded polygon facets, on the
foe's head, shoulder and garment. Evidence: `s4-p5-parry-contact` frame 11, `x610..690 y360..420`
at 8×."* And §2.1 concludes of the paid item: *"**No filled quadrilateral survives.**"*

The region is recorded and it is the right region. **The description of what is in it is wrong,
and it matters because it points 3b at the wrong mechanism.** Read at 8×, that patch is a grid of
**axis-aligned filled rectangles** with straight vertical and horizontal boundaries, not
arbitrarily-angled mesh facets. Quantified — straight axis-aligned luminance edges (|∆L| > 8,
contiguous run ≥ 6 px along a single row or column), per 1000 px of box, `s4-p5-parry-contact`
frame 11:

| region | straight V-edges | longest | straight H-edges | longest | **per 1000 px** |
|---|---|---|---|---|---|
| **foe's face `x610..690 y360..420`** | 30 | **44 px** | 27 | **62 px** | **11.5** |
| shed flecks `x501..581 y265..391` (the box §2.1 certifies) | 2 | 10 | 8 | 14 | **1.0** |
| open sky `x760..900 y300..420` | 8 | 9 | 9 | 11 | **1.0** |
| hero torso `x360..470 y360..420` | 2 | 7 | 10 | 13 | **1.8** |
| foe's face, same box, on `s4-p4-parry-contact` | 26 | 48 | 19 | 17 | 9.1 |

The horizontal edges sit on a near-regular **4-pixel lattice** — y = 379, 383, 387, 391, 395, 399,
403. That is a quantised or nearest-sampled field (§3's *"the dissolve reading as 'dithering' or
'TV static'"*, §3b.5's *"Detail in screen space"*), **not** flat mesh shading. Two consequences:

- **§2.1's "No filled quadrilateral survives" is true only inside its own 80×126 box.** Thirty
  pixels to the right, at the point the composition directs the eye to, the straight-edge density
  is **11× higher than in the box pass 5 certified**, and it got *worse* per unit area than pass 4
  (9.1 → 11.5), consistent with §2.2's own note that the figure is 15% larger.
- **The bbox-fill statistic that certifies item 1(a) is structurally blind to this.** The blocks
  are contiguous, so they label as one 72×60 component at fill 0.34. A metric that scores this
  patch 0.34 cannot be used to say quadrilaterals are gone.

### C3. Debt item 7 (→ **3c**): both capture numbers are pass-3 readings through a span pass 4 proved 23% too tall

**Record says**: *"**The foe's lower garment** is 0.300 of a figure height against the hero's 0.581
and the corpus's 0.495."* No capture is named, no frame, no region (§11.3).

Those two capture figures are the pass-3 review's, taken through the `y314..719` 406-row span that
the pass-4 review proved was **1.234× too tall** — the pass-4 review lists item 7 explicitly as
*"the pass-3 review's measurement, carried forward, not re-verified here"*, and pass 5 did not
re-verify it either. Re-measured, skirt band 0.621–0.890 of the span, connected components ≥ 10%
of the band's ink, `s4-p5-parry-contact` frame 11 through the recorded `0,299,960,378`:

| | hero (left) | foe (right) |
|---|---|---|
| reference image 3 | 0.569 | **0.496** |
| record's claim | 0.581 | 0.300 |
| **`s4-p5-parry-contact` f11, delivered** | **0.743** | **0.333** |

The corpus figure reproduces (0.496 against the recorded 0.495). Both capture figures do not. And
the correction reverses the prescription: the record tells 3c the hero is near the corpus and only
the foe needs widening, but the delivered hero is **0.743 — 31% wider than the corpus's 0.569.**
Widening the foe alone would move the pair further from the corpus, not closer.

*Reservation, stated because §0 warns about exactly this:* on the dusk stage the ground smear
merges with the garment at the skirt band, so this band is hard to segment. That is precisely why
item 7 needed a recorded region and does not have one.

### C4. §0's span rule is wrong for `s4-p5-spread155`, which is the control the whole cost table rests on

**Record says** (§0, and repeated in Commands): *"**`--span 0,299,960,378` for `s4-p5-*`.**"*

`s4-p5-spread155` is an `s4-p5-*` capture, and it is framed at `LANE_SPREAD = 1.55`. Its ink crown
sits at **y320 — identical to `s4-p4-parry-contact`'s y320**, and 54 rows below
`s4-p5-parry-contact`'s y266. By §0's own arithmetic its figure is **329 rows, not 378**. Read
both ways, frame 11:

| span used | sash | skirt |
|---|---|---|
| `0,299,960,378` — what §0's rule instructs | 0.778 | **0.850** |
| `0,348,960,329` — its own | **0.854** | **1.032** |
| record's §2.2 table | 0.854 | 1.028 |

**The record's numbers are right; the rule that reproduces them is wrong.** Anyone applying §0
literally — which is what an inheriting system will do — gets the skirt separation wrong by 17%.
The same applies to §2.2's cost item 2 (`19.8 at 1.55`): I read **20.0** through the 329 span and
**23.4** through the 378 one. `s4-p5-spread155` must be read through `0,348,960,329`, and §0 says
the opposite.

### C5. §5's bloom caveat — the discrediting of pass 4's result does not reproduce

**Record says** (§5, Protected results): *"**Caveat, and it is a real one:** the same reader on
`s4-p4-parry-contact` through the pass-4 box reads 3.5 / 7.7 / 19.3 px, not the 0.4/0.5/0.7 the
pass-4 record claims for that capture. The delivered picture meets the claim; the pass-4 capture
does not, on this reader."*

I cannot reach 3.5 / 7.7 / 19.3 under any convention. Core `L ≥ 246.5` centroid to nearest pixel of
the nearer cool-bright cloud (`L > 212`, `b − r > −6`, ≥ 120 px after a 2×2 opening),
`s4-p4-parry-contact` frames 9 / 10 / 11:

| box / threshold | f9 | f10 | f11 |
|---|---|---|---|
| `x500..700 y230..450`, L ≥ 246.5 | **0.4** | **0.5** | **0.7** |
| `x500..760 y250..450` (pass-4 review's box), L ≥ 246.5 | 0.4 | 0.5 | 0.7 |
| same box, L ≥ 240 | 1.0 | 0.4 | 0.4 |
| same box, L ≥ 248 | 0.4 | 0.3 | 0.5 |
| centroid-to-cloud-**centroid** instead of nearest pixel | 36.8 | 5.7 | 17.0 |

**Pass 4's 0.4 / 0.5 / 0.7 reproduces exactly and is robust across thresholds and boxes.** The
record's caveat casts doubt on a predecessor's protected result, and the doubt is unfounded. A
future system reading §5 would wrongly discount pass 4's bloom measurement.

*(The pass-5 half of that row does reproduce: I read 1.0 / 0.5 / 0.4 px on frames 9/10/11 of
`s4-p5-parry-contact` — frames 10 and 11 exact, frame 9 a sub-pixel convention difference — with
peak luminance **253.1 / 254.1 / 253.8**, exactly as recorded.)*

### C6. §1 and §5: the "minimum two-cloud separation of 0.0059 at frame 9" is a merged blade mass against a 52-pixel fragment

The number reproduces from the project's own tool exactly:

```
./gw analyse -Pargs="blades out/captures/s4-p5-parry-contact --span 0,299,960,378 --max 0.02"
  -> minimum 0.0059 of a figure height at frame 9
```

But its frame-9 line reads:

```
9   2.2 px = 0.0059 ... clouds 858 px x541..671 y232..394 (131x163) / 52 px x672..689 y215..231 (18x17)
```

The **858 px component contains both blades and the bloom**; the "second cloud" is a **52 px,
18×17 fragment** whose corner sits 2.2 px from it. The pass-4 review already recorded this failure
mode — *"The tool is scoring the separation between a blade and a fragment"* — and the closing
record does not carry the caveat forward. Under the reviews' own ≥ 120 px convention my
independent reader gives **minimum 0.0132 at frame 10**, on both the shipped run and the repro.

**The conclusion holds** — the blades meet, and both readings are inside the 0.02 acceptance — but
**0.0059 should not be handed forward as the delivered separation.** The claim *"no frame is
scored 0.0000 by the merge convention"* is literally true and materially misleading: at frame 9 the
blades **are** merged into one component, and the score escapes 0.0000 only because a 52 px
fragment detached.

### C7. §2.2 point 4: the spanned merge count is **1 of 24**, not 0

**Record says**: *"The whole-column merge count is **0 of 24 either way**, spanned."* Run through
the project's own tool:

| capture | span | frames passing every band | **one mass** |
|---|---|---|---|
| `s4-p5-parry-contact` | `0,299,960,378` | 0 of 24 | **1** |
| `s4-p5-spread155` | `0,348,960,329` | 0 of 24 | **1** |
| `s4-p4-parry-contact` | `0,348,960,329` | 0 of 24 | **1** |
| `s4-p5-parry-contact` | `--allow-detected-span` | 0 of 24 | **1** |
| `s4-p4-parry-contact` | `--allow-detected-span` | 0 of 24 | **17** |

The un-spanned readings (1 and 17) reproduce exactly and the argument voiding pass 4's refusal
stands. The spanned figure is 1 of 24 either way, which is also what the pass-4 review reported.
"0 of 24 either way" is the one number in that paragraph that is wrong.

### C8. Debt item 15 contradicts §7.1 and the delivered pixels

**Item 15 says**: *"Capture non-determinism, characterised and now larger. §7.1: **9,101 px over 4
frames, max delta 213**, in the shed flecks and the star. **The graded frame is bit-identical
across runs.**"*

**§7.1 says**: 13,825 px over 5 frames, max 149, and *"**The graded frame is one of them**"*.

Measured, `s4-p5-parry-contact` against `s4-p5-parry-repro`, all 24 frames:

```
13,825 differing px of 16,588,800, max channel delta 149, 5 frames
  frame 11: 10,493 px  max 149  x517..697 y322..454
  frame 16:  1,420 px  max  48
  frame 21:  1,109 px  max 126
  frame 22:    801 px  max  77
  frame 23:      2 px  max   1
```

**§7.1 is right to the pixel; item 15 is stale, and its error is the dangerous direction.** It
tells an inheriting system the graded frame is stable when it is the single most unstable frame in
the capture — licensing exactly the absolute frame-11 claims §7.1 forbids. Item 15 is the entry
that will be read, because §8 is the section labelled *"the deliverable of the five-pass cap"*.

### C9. §4 item 4 / item 16: the `u_dusk` count is **97,795**, and "all in the lower frame" is false

Measured, `s4-p5-null-static` against `s4-p4-null-static`, 24 frames:

- **97,795 differing pixels** (record: 97,791, stated twice)
- max channel delta **6** ✓, **all 24 frames** affected ✓
- differing rows span **y20..719**. **16,518 px — 16.9% — lie above row 360.** The record's *"all
  in the lower frame"* is wrong.

The substantive conclusion is unaffected and reproduces: `s4-p5-null-static` against
`-repro` is **0 of 16,588,800**, so the readback path is sound, and the bit-identity control across
the shader edit is genuinely gone at an invisible amplitude.

### C10. §1: the crossed-X extent has no frame and no region, and the graded frame reads 0.370

**Record says**: *"the crossed X spans **0.49–0.53** of a figure height against the corpus's
0.487–0.655."* No frame, no capture, no threshold, no region (§11.3). Union bounding diagonal of
the cool-bright clouds (`L > 212`, `b − r > −6`, ≥ 120 px, 2×2 opening), `s4-p5-parry-contact`,
normalised by 378:

| frame | 6 | 7 | 8 | 9 | 10 | **11** | 12 | 13 | 14 | 15 | 16 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| union diagonal (fh) | 0.798 | 0.683 | 0.492 | 0.546 | 0.450 | **0.370** | 0.419 | 0.215 | 0.231 | 0.265 | 0.305 |

The quoted 0.49–0.53 is reachable only on frames 8–9. **On the graded frame it is 0.370 — below
the corpus's own stated lower bound of 0.487.** The claim is a two-frame slice presented as the
delivered figure.

### C11. Debt item 8 and §2.2 (→ **System 5**): the corpus band is taken from two of three Family B images

**Record says** (item 8, §9): *"reaching the corpus's **0.58–0.64**"*, and §2.2's table lists only
images 3 and 4. §11.0 is explicit: *"show it on **every** image in the family that depicts the
situation being measured, and name the ones you excluded and why."* Reference image 5 is a Family
B duel, it resolves two bodies cleanly at the sash and skirt bands at the operating factor 0.85,
and it is not named as excluded here:

| | sash | skirt |
|---|---|---|
| reference image 3 | 0.634 | 0.615 |
| reference image 4 | 0.595 | 0.583 |
| **reference image 5** | **0.679** | **0.648** |

Including it, the corpus skirt band is **0.583–0.648**, not 0.58–0.64, and the delivered 0.753 is
**1.16–1.29×** the corpus rather than a single "1.22×".

**And "would need about 1.05" is a linear extrapolation the project has already refuted.** The two
points System 4 actually shot give skirt separation 1.032 at spread 1.55 and 0.753 at 1.35 — a
1.37× change for a 1.148× change in the constant, i.e. roughly the **square** of the spread, not
linear. Extrapolated on the measured exponent, reaching image 3's 0.615 needs about **1.22**, not
1.05. The pass-3 review made the same point in different words: *"`LANE_SPREAD` is not a spacing
parameter; it is a staging parameter, and no band responds to it linearly."* Two points are a weak
basis for an exponent — but they are a sufficient basis for saying **1.05 is not a measured
number**, and System 5 should not plan against it.

### C12. Debt item 14 (→ **3c**): 1.304 is a pass-4 number; the delivered picture reads 1.227

The recorded box reproduces exactly on the capture it was taken from —
`s4-p4-parry-contact` frame 11, `x645..720 y425..488`: **ratio 1.309, median `#8D6E69`, hue 8°**
(record: 1.304, `#8D6E69`, hue 8°). But item 14 is handed to 3c as a live property of System 4's
delivery, and the box is recorded *"on the pass-4 span"*. The same box on the **delivered**
`s4-p5-parry-contact` frame 11 reads **1.227, `#8C6C68`, hue 7°**. The defect is real and persists;
the number is not the delivered one.

### C13. §1 and §6: "0 skipped" is true of this machine and the guard-that-fails-open defect is still live

The suite is green — I re-ran it from a clean task state: **389 tests, 0 skipped, 0 failures,
0 errors**, aggregated from the JUnit XML across all suites. `node tools/check-progress.mjs`
returns **OK**, and the three frames it finds referenced from `src/` are all git-tracked.

**But §11.2b(f)'s own test is not "does it skip here", it is "does version control hand it to
someone else" — and six assertions still fail that test.** `tools/check-progress.mjs` scans for
*literal* `out/captures/.../frame_NNN.png` strings (`/out\/captures\/[\w./-]*frame_\d+\.png/g`),
which is why it reports "captures read by code: 3". `CorpusTest` composes its paths from two
arguments — `frame("s1-p5-swing", "frame_011.png")`, `capture("s3-p1-hair").frame(0)` — so the
checker cannot see them, and each is wrapped in `Assumptions.assumeTrue`.

Five of those frames and one whole capture directory are **untracked**:

```
out/captures/s3-p1-bind-regress/frame_000.png   UNTRACKED
out/captures/s3-p1-ik-regress/frame_000.png     UNTRACKED
out/captures/s2-p3-gesture/frame_000.png        UNTRACKED
out/captures/s1-p5-swing/frame_011.png          UNTRACKED
out/captures/s1-p7-swing/frame_000.png          UNTRACKED
out/captures/s3-p1-hair/                        only contact-sheet.png is tracked
```

Verified the honest way, by moving them aside and re-running (files restored immediately
afterwards, suite re-confirmed green):

```
<testsuite name="dev.starfall.analysis.CorpusTest" tests="10" skipped="6" failures="0">
  inkGravityIsStillInvertedInPass5, theGroundBandHasNoPeriodicArtefact,
  theBindPoseIsStillBitIdentical, theHairCaptureContainsNoReversalAtAll,
  theValueRangeHoldsAtBothEnds, theIkRegressionDiffIsTheRecordedCount
```

**BUILD SUCCESSFUL, 6 of 10 silently skipped, and `check-progress.mjs` still says OK.** So §1's
*"389 tests, 0 skipped … and a progress checker that refuses on any capture frame referenced from
`src/` that git does not track"* overstates what the checker does: it refuses on any frame
referenced **as a literal string**. The class of defect §11.2b(f) names — *"an assertion has three
outcomes, not two, and the third one is silent"* — was closed in three places and remains open in
six.

### C14. Small internal inconsistencies, listed so they are not inherited as fact

- **§2.1** quotes *"against pass 4's **0.58**–0.98"* for bbox fills, while its own table in the same
  subsection lists 0.78 / 0.81 / 0.89 / 0.98 and the pass-4 review's lists 0.78–1.00. 0.58 appears
  in neither.
- **§2.1**'s pass-5 fleck box `x501..581 y265..391` is described as *"the review's box scaled
  378/329 about the clash core"*. Scaling `x530..600 y290..400` about the measured pass-4 core
  (631, 337) gives `x515..595 y283..409`; about the pass-5 core (610, 338) gives `x494..574
  y283..409`. The recorded rectangle is what matters and I used it — but the stated derivation does
  not produce it.
- **§2.1** reports *"11 blobs"* in that box and *"6 blobs ≥ 40 px"* (§7.1); my reader finds **16**
  and **5** respectively. The local-median convention is not recorded, and the blob decomposition
  is sensitive to it. Conclusions are unaffected (see F3).
- **§4** heads the section *"Pass 5 found four"* non-reproductions; §5 then introduces a fifth (the
  bloom caveat, C5), which is not listed in §4 and does not reproduce.

---

## 2. Confirmations — handed-forward numbers I re-measured and reproduced

These are load-bearing and checked. An inheriting system can act on them.

### To System 3b

- **The part count, re-taken at 378 rows as the record instructs.** I built the matched-scale sheet
  (reference image 3 downscaled 378/672 → 468×612, beside `s4-p5-parry-contact` frame 11 at native
  scale) and counted. Reference image 3 at a 378-row figure resolves, per duellist: topknot with a
  bound knot, trailing hair wisps, brow, eye, nose, lip and chin **as one continuous contour**,
  jaw, ear, neck, collar, shoulder, upper arm, forearm, wrist, **both hands on the grip**, tsuka,
  tsuba, blade, obi with knot, **a second sheathed blade with saya and kojiri**, hakama panels with
  fold lines, two legs, feet, and ink-smear ground contact — **about 25.** The capture resolves,
  for the hero: hair mass, head lump with no features, shoulder, upper arm, forearm, fist/hilt bar,
  blade, ochre sash, undifferentiated lower mass, a foot — **nine.** For the foe: hair mass, head
  with a blocky patch where a face would be, shoulder, arm, blade sliver, ochre sash, lower
  garment, leg, boot — **nine.** **Neither figure resolves a face, a hand, a guard, a second blade,
  a fold, or a foot on the ground.** The record's 9-against-25 **reproduces unchanged at the new
  scale.** One thing the record does not say: enlarging the figure made the foe's face patch read
  as the block lattice of C2, so at 378 rows it is a *legible artefact* rather than the "white
  amoeba" pass 4 described.
- **Debt item 4's region and its largest step.** `s4-p5-parry-contact` frame 9, blade-excluded
  maximum single-pixel step in the clash box: **120.1 at (635,361), rgb `[165 158 166]` → `[40 39
  50]`** — exact, to the pixel and to the channel. (See C2 for the naming correction.)

### To System 3c

- **Item 6, the embers.** `r − b ≥ 40`, `L ≥ 150`, ≥ 4 px, frames 9/11/13/15/17: **3 / 3 / 2 / 2 /
  4** on `s4-p5-parry-contact` and **identically on `s4-p5-parry-repro`**; **5 / 3 / 2 / 2 / 2** on
  `s4-p4-parry-contact`. Exact on all ten readings. Unchanged in four passes against §5's 8–20.
- **Item 14's corpus and pass-4 anchor** — `x645..720 y425..488` on the pass-4 span: ratio **1.309**,
  median `#8D6E69`, **hue 8°**. Exact. (Delivered-picture value corrected in C12.)
- **The record's own warning on item 14 is sound** and should be obeyed: defensible alternative
  boxes inside the same figure spread 2.2×, which is §11.3's situation exactly.

### To System 5

- **Item 8's delivered separations.** Widest-clear-column split constrained to the central half of
  the ink extent, side centroids per band, span-cropped, bands sash 0.411–0.621 / skirt 0.621–0.890:

  | | sash | skirt |
  |---|---|---|
  | reference image 3 | **0.634** (rec. 0.635) | **0.615** (rec. 0.615) |
  | reference image 4 | **0.595** (rec. 0.595) | **0.583** (rec. 0.583) |
  | `s4-p4-parry-contact` (1.55) | **0.854** (rec. 0.854) | **1.057** (rec. 1.052) |
  | `s4-p5-spread155` (1.55, own 329 span) | **0.854** (rec. 0.854) | **1.032** (rec. 1.028) |
  | **`s4-p5-parry-contact` (1.35, shipped)** | **0.748** (rec. 0.749) | **0.753** (rec. 0.750) |

  Identical on `s4-p5-parry-repro`. The **1.7× → 1.22× improvement against image 3 is real and
  reproduces.** (Corpus-band scope corrected in C11; span rule corrected in C4.)
- **Item 11, the knockback.** Confirmed: every knockback capture on disk (`rev-s4-knockback*`,
  `s4-p1-knockback*`, `s3-p3-knockback*`) is on **cream paper** — frame 0 row backgrounds 218 / 220
  / 225, top pixel `[226 218 202]` — against the dusk stage's 57 / 88 / 38 and `[46 57 84]`. **The
  knockback has never been shot on the dusk stage.**
- **Item 18, the planning framing.** `s4-p5-phrase-60hz` frame 0: ink is **1.1% of the frame**, the
  largest component is **4,705 px at `x112..177 y526..659`** — two figures 134 px tall in the
  bottom-left of a 960×720 frame. Qualitatively confirmed. (The "85%-empty" figure itself is
  descriptive, not measured — see U4.)

### The pass-5 items, verified in delivered pixels

- **Item 1(b), the rays — PAID, and the headline reproduces.** Single-pixel luminance steps, blade
  excluded (`L > 130, b − r > 4`, dilated 3×3), 260×200 box centred on each frame's own `L ≥ 246.5`
  core:

  | | p99 | max | share > 60 | **share > 100** |
  |---|---|---|---|---|
  | reference image 3, matched scale 378/672, `x103..363 y127..327` | **30.0** | **77.4** | **0.020%** | **0.000%** |
  | `s4-p4` f9 / f10 / f11 | 25.4 / 36.6 / 28.7 | **114.9 / 135.3 / 88.6** | **0.069 / 0.379 / 0.030%** | **0.0021 / 0.0572 / 0%** |
  | `s4-p5` f9 / f10 / f11 | 28.4 / 27.1 / 28.5 | **120.1 / 92.6 / 108.1** | 0.069 / 0.068 / 0.100% | **0.0011 / 0% / 0.0031%** |
  | `s4-p5-parry-repro` f11 | 26.8 | **88.6** | 0.041% | **0%** |

  The reference row is **exact on all four statistics**. Frame 10 goes from **0.0572% of steps over
  100 to none at all**, and its share over 60 from 0.379% to 0.068%. The frame-11 range across the
  two runs (88.6 / 0.041% / 0% and 108.1 / 0.100% / 0.0031%) reproduces, and frames 9 and 10 are
  bit-identical across runs, so the conclusion does not rest on the unstable frame. **Item 1(b) is
  paid.**

  *One qualification the record should have carried:* it says *"The review's target was … the share
  over 60 at or below 0.195%. The share is met on every frame."* True against the pass-4 review's
  number — but the **same document's own re-measurement of the reference puts it at 0.020%**, and
  the delivered 0.068–0.100% is **3.4–5× the reference's own share.** The target moved by 10×
  between passes and nobody said so.

- **Item 1(a), the flecks — PAID, and the load-bearing conclusion reproduces.** Through the
  recorded `x501..581 y265..391` on `s4-p5-parry-contact` frame 11, blobs 8 L above the local
  median, size class ≥ 40 px: my reader finds **5 blobs, fills 0.41–0.57, sizes 44–283 px, aspects
  0.25–0.90** (record: 6 blobs, 0.37–0.55, 41–209 px, 0.26–0.95), and **identically on
  `s4-p5-parry-repro`** (0.41–0.58). Against pass 4's 8×8 blobs at fill 0.78–0.81 in the same size
  class. **No filled quadrilateral survives in that box** — reproduced on both runs, which is what
  makes it the load-bearing half. (Scope correction in C2: the claim does not extend past the box.)

- **Non-reproduction (a) — the "max step 134" is the blade's own edge. CONFIRMED.** Maximum
  single-pixel step in the pass-4 clash box, blade **included**: **133.5 at (635,357)→(635,358)**,
  rgb `[101 103 105]` → `[231 237 244]`, **b − r = +13**, and both pixels are inside the dilated
  blade mask. The largest step in the pass-5 clash box, **175.6 at (611,309)→(612,309)**, `[237 242
  247]` → `[62 66 73]`, is **likewise a blade edge**. The record's location differs by two pixels
  (it cites (637,359)) — the same edge, the adjacent pair. **The correction is right: the ray defect
  was never carried by the maximum.**

- **Non-reproduction (b) — the ground target was unreachable under §2.2. CONFIRMED, exactly.**
  Lower third of the figure span + 30 rows, reference image 3 downscaled to matched scale, its own
  1.24 fh width:

  | | p01 | median | p99 | sd | range | **below floor 25.73** |
  |---|---|---|---|---|---|---|
  | reference image 3 @ 329 | 11.0 | 30.9 | 102.2 | **24.6** | 91.2 | **41.6%** |
  | … clamped at §2.2's floor | 25.7 | 30.9 | 102.2 | **21.0** | 76.5 | — |
  | reference image 3 @ 378 | 11.0 | 31.4 | 102.4 | 24.8 | 91.4 | 40.7% |
  | `s4-p4-parry-contact` f11, 1.24 fh crop | 26.7 | 39.1 | 98.1 | **20.4** | 71.4 | 0.1% |
  | `s4-p5-spread155` f11, 1.24 fh crop (329) | 26.7 | 39.2 | 97.9 | **20.0** | 71.1 | 0.1% |
  | **`s4-p5-parry-contact` f11, 1.24 fh crop** | **26.7** | **39.6** | **97.5** | **20.2** | **70.7** | 0.1% |

  **41.6% below the floor is exact. The clamped ceiling of sd 21.0 is exact. The delivered 20.2 is
  exact**, and identical on the repro. And the crossed-crop diagnosis holds: read across the full
  960 px the same band gives **sd 18.2** (pass-4 review: 18.0) against **20.4** through the
  reference's own 1.24 fh width. **The deficit really is 20.2 against a floor-limited 21.0.**

- **The sky ramp and the ground band — exact on all fourteen readings.** Outer-70-column row
  background, world y from each capture's own span:

  | world y | reference image 3 | `s4-p4` f11 | `s4-p5` f11 |
  |---|---|---|---|
  | +2.40 | **57.2** | 58.5 | **57.7** |
  | +1.10 | **97.5** | 91.0 | **91.9** |
  | +0.50 | **64.0** | 64.4 | **64.4** |
  | +0.15 | **40.2** | **55.1** | **52.9** |
  | 0.00 | **36.3** | **59.7** | **44.9** |
  | −0.10 | **17.5** | **59.3** | **38.2** |
  | −0.15 | **28.1** | **56.9** | **35.9** |

  Every bolded cell matches the record to the decimal. **The gradient sign really did invert**: pass
  4 rises 55.1 → 59.7 → 59.3 going down the frame; pass 5 falls 52.9 → 44.9 → 38.2, which is the
  corpus's direction. This is the best-evidenced claim in the document.

- **Item 2, the corridor.** **0 of 24 frames pass every band** at 1.35, at 1.55 and on the pass-4
  capture, spanned and un-spanned. Confirmed on the project's tool. (Merge count corrected in C7.)
- **§7.3, the refusal.** `analyse corridor` without `--span` refuses with the message the record
  quotes. Confirmed.
- **§7.4.** `--profile` runs the criterion on the whole of Family B first: **images 3 and 4 both
  pass the band**, and image 5 is **excluded and named** with the corrected reason. §11.0
  satisfied.
- **Determinism controls.** `s4-p5-null-static` / `-repro`: **0 of 16,588,800**. `s4-p4-parry-contact`
  / `-repro`: **983 px, max 19, one frame (18)**. Both exact.
- **Constants and guards.** `Director.LANE_SPREAD = 1.35`, `SamuraiRig.BLADE_NAGASA_FRACTION =
  0.55f`, `Timing.HELD_BREATH_SCALE = 0.85` / `HELD_BREATH_SECONDS = 0.25`,
  `DuellistValueTest.DELIVERED_FLOOR = 4.00`. All eight guards named in §6 exist in the suite.
- **Suite state.** 389 tests, 0 skipped, 0 failures, 0 errors. `check-progress.mjs` OK. (Both
  qualified by C13.)

---

## 3. Unverifiable — do not build on these

- **U1. The `u_dusk` bisection (§4, item 4).** *"a control adding five noise octaves … →
  bit-identical; the grass amplitude change alone → bit-identical; deleting the dusk smear line
  alone → bit-identical; collapsing one `mix(a, b, u_dusk)` into an `if/else` with the identical two
  arms → **2 pixels differ**."* Every one of these was a probe build. No probe capture survives on
  disk and no branch or commit is named. The **outcome** is confirmed (C9) and the conclusion
  — *"multiplying by zero is not the same as not compiling the term"* — is plausible and cheap to
  believe. **The four-step bisection that assigns the cause to `mix` cannot now be checked by
  anyone.** Treat it as a hypothesis, not a measured result.
- **U2. Non-reproduction (c), *"the stored poses are 0.5–0.7 rad apart"*.** This is a runtime
  property of the trail history, and nothing in the repository records it. `TRAIL_SWEEP = 0.60f` is
  confirmed in source and the javadoc restates the claim, but the javadoc is the same author's
  assertion, not an independent path (§11.2b(c)). **The pose-spacing figure is unverifiable from
  the delivered artefacts.** Note also that the source javadoc and the debt disagree about what the
  cap delivers on the same frame (0.58 fh vs 0.82 fh — see C1(d)), so at least one of the two is
  wrong and there is no third reading to break the tie.
- **U3. Guards A–F (§6).** The record is honest that pass 5 *"did **not** re-break them and the
  messages below are carried forward"*. The assertions all exist; **the red observations are pass
  4's and cannot be re-witnessed from this document.** §11.2b(f) is satisfied historically, not
  currently. Guards **G** and **H** are pass 5's own and their mechanisms are checkable — the §7.3
  refusal fires, and `ParryWindowTest.theGradedParryWindowPutsAFigureHeightAt378Rows` exists and
  pins the span through the same camera arithmetic.
- **U4. "an 85%-empty frame" (item 18).** No region, no threshold, no instrument. I can confirm the
  *composition* (U-confirmed above: 1.1% ink, figures 134 px tall in the bottom-left) but **the 85%
  figure is a description carried from the pass-2 and pass-3 reviews, not a measurement.** It
  should not be quoted as one.
- **U5. Item 3 (the pelvis, 0.0000) and item 10 (the chain arrives together).** The record states
  plainly that these were *"Not re-measured since pass 4"* / *"Untouched since pass 3"*. They rest
  on headless rehearsal instruments that were temporary and are described as deleted after use. I
  did not re-derive them and **nothing in the delivered captures can confirm or refute them.** The
  disclosure is honest; the numbers are inherited, not verified.
- **U6. Item 12, phrase continuity (*"longest run below 0.02 = 5 steps = 0.083 s"*).** Correctly
  labelled *"carried forward, **not re-measured**"*, from a reader the record itself says should
  not be run again. I did not re-run it either, for the same reason. **The conclusion may be true;
  it has no current instrument.**
- **U7. Anything about an individual fleck or ember near a clash on frame 11.** §7.1's own
  consequence, and I confirm it: frame 11 differs by 10,493 px across two runs of the same command
  at the same commit, max channel delta 149, inside `x517..697 y322..454`. This is the correct
  constraint and item 15 states its opposite (C8).
- **U8. Item 13 (`DuellistValueTest` certifies pass 4's picture).** Confirmed as *stated* —
  `DELIVERED_FLOOR = 4.00` and the assertion reads `out/captures/s4-p4-parry-contact/frame_011.png`,
  which is git-tracked. **The ratchet is real but it is a ratchet on a superseded staging**, exactly
  as the record says. Whether 4.00 is the right floor for the 378-row picture is not knowable
  without re-deriving the four rectangles, which nobody has done.

---

## 4. What System 4 actually delivers

**In thirty seconds, for someone starting System 3b tomorrow.** System 4 is the staging and
direction layer: it turns a combat resolution into a directed duel on the Family B dusk stage and
draws it. What it genuinely delivers, verified in delivered pixels: **a dusk sky that belongs to
the corpus** — the ramp matches reference image 3 to within a few luminance levels from world y
+0.5 upward, and the ground band now *darkens* toward the frame bottom (52.9 → 44.9 → 38.2) where
every previous pass brightened; **a parry in which the two blades actually meet**, inside the 0.02
acceptance on every frame of the contact window; **a clash mark whose marks no longer print as
filled squares** — the share of single-pixel steps over 100 in the clash box goes to zero on the
brightest frame; **a base separation cut from 1.7× the corpus's to about 1.2×**; and **389 green
tests with a real refusal in the analysis tool**. What it does **not** deliver, and this is the
part 3b inherits: **the figures are still only about nine readable parts each against the
reference's twenty-five at the same scale** — no face, no hand, no guard, no second blade, no fold,
no foot on the ground — and the enlargement to 378 rows has made the worst artefact in the frame
*more* legible, not less: where the foe's face should be there is a grid of axis-aligned filled
rectangles on a 4-pixel lattice, carrying eleven times the straight-edge density of the region pass
5 certified clean, sitting at the exact point the composition directs the eye to. **The subject is
the gap, not the material** (§11.0's corollary), and the single most useful thing 3b can do is put
a face there.

---

## 5. Reading order for the inheriting systems

If you read only one thing before acting on `docs/system4-debt.md`:

- **3b** — read **C2** before item 4, and **F (part count)** confirms item 9 unchanged at 378 rows.
- **3c** — read **C1** before item 5 (the fix is already in the code), **C3** before item 7 (both
  capture numbers are wrong and the prescription reverses), **C12** before item 14. Item 6 is
  confirmed exact.
- **System 5** — read **C11** before item 8 (the corpus band excludes image 5, and "1.05" is not a
  measured number), **C4** before using any `s4-p5-spread155` figure. Items 11 and 18 are confirmed.
- **Everyone** — read **C8**. Debt item 15 says the graded frame is stable across runs. It is the
  least stable frame in the capture, and §7.1 of the same document says so.
