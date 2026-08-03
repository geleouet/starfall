# System 4 — standing debt

**Status: pass 3 shipped, self-graded, and NOT self-declared a pass.** System 4 is at
**pass 3 of 5**. The two charges the pass-2 review put first — a corridor acceptance
its own ground truth fails by 4×, and a clash guard that cannot fail — are both
closed, with the criterion asserted on the corpus and the guard observed red. The
things they were guarding are **not** closed: the capture misses the corridor profile
on 13 of 24 frames and the clash light is three frames long because the bind is.

Every capture quoted here is `s4-p3-*`. `s4-p3-null-static` is **bit-identical** to
`rev-p2-null-static` (24 of 24 frames, 0 of 691,200 pixels differ) and both carry
`harness=f0ad18994eec`, the hash that produced every `s4-p2-*` and `rev-p2-*` capture —
so comparisons with pass 2 are in scope per §11.2b(d). Nothing from `s4-p1-*` is quoted.
Every pixel number is a ratio to the frame's own figure height or to its own ground
level, per §11.3, and every one is printed beside its rectangle.

---

## 0. What the pass-2 review got right that this pass had to correct in itself

The review found three things nobody had measured. All three reproduce here, on
independently written readers:

- **Reference image 3 fails the 0.06 whole-column corridor acceptance.** Measured with
  the checked-in `CorridorProfile`: **0.0149** of a figure height through
  `x288..629 y283..955`. Two passes chased 0.06.
- **The held breath is at spec, on all three scenes.** 0.850× over ~0.25 s per ramp,
  parry, knockback and phrase. §5 of the pass-2 debt said otherwise and was wrong; that
  paragraph is deleted and replaced by an assertion (see §4 below).
- **`everyClashIsDrawnWhereTwoBladesActuallyAre` could not fail.** Confirmed by
  rebuilding it so it can, and watching it go red on the code it was written against.

---

## 1. The corridor criterion — CLOSED. The corridor itself — MISSED, with numbers.

### 1.1 The criterion, and it is asserted on the corpus in the same command

`dev.starfall.analysis.CorridorProfile` + `analyse corridor --profile`. Five bands of a
figure, each a fraction of its own height, each with a floor taken from reference image
3's own reading. The command measures **the reference first, every time**, and returns
non-zero if the corpus does not pass its own floors. STYLE.md §11.0, put in the tool.

Reference image 3, ink factor 0.85, row-local background, span `x0..831 y283..955`
(673 px), window between the two bodies' ink centroids `x288..x629`:

| band | rect | clear | / figure height | floor adopted |
|---|---|---|---|---|
| head | `x288..629 y283..403` | 57 px | 0.0847 | 0.080 |
| torso | `x288..629 y404..558` | 10 px | **0.0149** | 0.014 |
| sash | `x288..629 y559..699` | 62 px | 0.0921 | 0.085 |
| skirt | `x288..629 y700..881` | 68 px | 0.1010 | 0.095 |
| feet | `x288..629 y882..955` | 76 px | 0.1129 | 0.065 |
| **whole column** | same span | 10 px | **0.0149** | — (the criterion two passes chased was 0.06) |

Swept for stability across ink factor 0.60–0.90 and six figure spans: `torso`
0.0146–0.0178, `sash` 0.0894–0.0998, `skirt` 0.0991–0.1070 — those three carry the
acceptance. `head` runs 0.085–0.284 and `feet` 0.070–0.394 across the same sweep; both
floors are set at the **bottom** of their own sweep and both are reported as weak.
`CorridorProfileTest.theReferenceProfileIsStableUnderItsOwnNuisanceParameters` asserts
the sweep rather than describing it.

Two independent readers agree: a NumPy/SciPy implementation written first, and the
checked-in Java one, return the same integer pixel counts for `torso`, `sash`, `skirt`
and `feet` on the reference and differ by 6 px on `head` (§11.2b(c)).

### 1.2 The capture: 11 of 24 frames pass, against pass 2's 8

`./gw analyse -Pargs="corridor out/captures/s4-p3-parry-contact --profile"`

| | pass 2 | pass 3 |
|---|---|---|
| frames passing every band | 8 / 24 | **11 / 24** |
| frames that are one connected mass | 2 | **2** |
| frames whose `torso` band misses | 13 | **8** |
| `torso` worst | 0.0000 | **0.0000** |

**It is a miss and it is reported as one.** The reference's `torso` is 0.0149 and this
capture reads 0.0000 on six frames of the contact window. On a 400 px figure the whole
of the reference's pinch is **10 px of sky between two tsuba**, and the capture has
none.

### 1.3 The profile, which is the finding, and it contradicts the brief

Same instrument, `s4-p3-parry-contact` frame 11, against the reference:

| band | reference | capture frame 11 | capture, contact window |
|---|---|---|---|
| head | 0.0847 | 0.3498 | 0.037 – 0.392 |
| torso | **0.0149** | **0.0000** | 0.000 – 0.038 |
| sash | 0.0921 | 0.3645 | 0.064 – 0.368 |
| skirt | 0.1010 | 0.3276 | 0.316 – 0.342 |
| feet | 0.1129 | 0.1773 | 0.155 – 0.183 |

**The capture is 3.2× wider than the corpus at the skirt and 1.6× wider at the feet,
and zero at the pinch.** The pass-2 review's brief said to *"bring `LANE_SPREAD` back
down toward 1.35 and make the bodies narrow at the pinch — `SamuraiRig`'s haori rails at
0.64 against `Stage.BODY_HALF` 0.56 is the number to attack"*. Measured with the
criterion the same review asked for, **both halves of that prescription move the picture
away from the corpus:**

- Lowering `LANE_SPREAD` scales every band by roughly the same factor. `skirt` needs
  ×0.31 and `torso` needs ×∞ — no scalar exists that fixes both, and pass 1 shot 1.35
  and merged the bodies on 25% of frames against pass 2's 12.5%. `LANE_SPREAD` is
  **unchanged at 1.55** and the brief's instruction is refused with this table as the
  reason.
- Narrowing the garment widens `skirt` and `feet`, the two bands that are already 3×
  and 1.6× too wide. The bodies are not too wide; they are too far apart at the base
  and the arms meet in the middle at chest height.

The move the profile actually asks for is the opposite: **widen the lower garment and
shorten the reach.** Not done, and the reason is budget rather than doubt — it is a
`SamuraiRig` rail edit and a `Stage` change, and this pass spent its budget on the two
central charges.

### 1.4 A weakness in this pass's own criterion, named rather than left to be found

Part of the 8→11 improvement is **not** geometry. The pale duellist's value lift (§2)
raised the darkest pixel in the `torso` band's clear run at frame 10 from luminance
**34.9 to 137.4**, so ink that used to be below `0.85 × background` now survives the 3×3
opening as background. A threshold-based corridor gets wider when a figure gets paler,
whether or not anything moved. For a viewer that is a real improvement in separation —
which is what the statistic is a proxy for — but the number is not purely a distance,
and quoting it as one would be the mistake §11.2b keeps recording. **A pass that wants
to grade staging alone should re-run this against a fixed value.**

---

## 2. The pale duellist has its value back above the sash — improved 1.54× → 2.09×, misses 3.27×

**Acceptance: reference image 3's torso separation, 3.27×. Not met.**

The per-region colour channel two debt documents have asked for, built in the cheapest
form that is still one: `InkMaterial.sashHeight` / `sashLift`, a **bind-space** height
above which the pooled value is scaled, applied per figure as one more per-draw uniform
in `ink_skin.frag`. In bind space with the noise it modulates, so it deforms with the
skin and nothing swims (§3.5); `(0, 1)` is the default and every single-figure capture
in the corpus is bit-identical through it (`s4-p3-null-static`).

Median ink luminance over the frame's own ground, `CorridorProfile.medianInkOverGround`:

| | dark duellist | pale duellist | ratio |
|---|---|---|---|
| **reference image 3**, `x190..300 / x540..650, y400..540` | 0.127 | 0.415 | **3.27×** |
| capture, pass 2, `x300..470 y300..420 / x600..760 y340..460` | 0.260 | 0.399 | 1.54× |
| **capture, pass 3**, same rectangles | 0.245 | **0.513** | **2.09×** |
| reference, skirt `x150..330 / x520..700, y660..820` | — | — | 1.16× (review's number) |
| capture, pass 3, skirt `x330..470 / x620..760, y500..620` | 0.162 | 0.265 | 1.63× |

**Where the remaining gap is, and it is not where the review said.** The pale figure now
reads **0.513** of ground against the corpus's 0.415 — it is, if anything, paler than the
corpus. The whole of the remaining shortfall is that **the dark duellist is not dark**:
0.245 against the reference's 0.127. Its torso median is 53–56, which is *exactly*
`INK_INDIGO`'s own luminance — the garment above the sash is sitting on its base tone
with nothing pooled into it, because the mesh authors wetness 0.03–0.20 there and every
value term in `ink_skin.frag` is gated on wetness.

Pushing the dark figure's pooling (`DARK_SASH_POOL = 1.70`) bought **3 luminance levels**
— 0.260 → 0.245, ratio 1.99 → 2.09 — and the ceiling reachable by pooling alone is
`INK_BLACK` at 0.12, which needs `dark` to saturate, which would print the flat fill
§3b.5's first row fails on sight. **Closing this wants the mesh's authored wetness above
the sash raised on the dark figure, which is a System 1 change.** That is the concrete
next step and it is not a guess: the number that has to move is `HAORI`'s wetness rows,
not a colour.

The guard is `DuellistValueTest`, on **delivered pixels** — the review's own
prescription. `DirectorTest.bothFiguresAreVisuallyDistinguishable` still asserts on
`InkMaterial`'s base colour and is still not load-bearing; it was not deleted because it
is a cheap regression trap on a different thing, but **it must not be quoted as evidence
about the picture.**

---

## 3. The clash guard can fail, and the bloom is honest — but it is three frames long

### 3.1 The guard

`RehearsalTest.everyClashIsDrawnWhereTwoBladesActuallyAre`, rebuilt. Three holes closed:

1. The mark is **`Director.lastCrossing`**, recorded per frame by `Rehearsal.Frame.mark`
   — the coordinate `Director.renderInk` substitutes for a `CROSSING` origin — not a
   point recomputed from the blade it is being measured against.
2. **Every frame the mark is drawn on** is checked, not the one frame nearest the
   directive's instant.
3. The mark must be **between** the two blades, not merely near both.

**Observed red**, per §11.2b(f). Reverting `lastCrossing` and `renderInk` to the
aim-point construction and running `./gw test --tests '*RehearsalTest*'`:

```
PARRY: at t=1.5833 the drawn clash mark (1.305,1.839) is 12.6% from the hero's blade
and 18.3% from the foe's. It is sitting on a grip.
```

That is the message it was written to print, on the code it was written against, and it
also confirms in world units what the pass-2 review measured in pixels.

### 3.2 What the guard found, which was worse than "two frames early"

The `CLASH` directive ran `BLOOM_SECONDS * 0.7` = **0.63 s** against a GUARD beat whose
whole contact span is **0.168 s**, and against a bind that measured **0.066 s**. The
light asserted a meeting for **9.5× as long as the meeting lasted**.

And the mark was drawn at the point the two blades are *aimed* at — the apex built from
the two fists — which sits **0.10 to 0.27 world units** from either blade through the
whole bind, i.e. above both of them and above the figures' heads (`y = 1.84` on a 1.70
figure). `Director` now keeps that construction for the **aim** (built from the blades it
is a fixed point of its own output, and the pass-2 debt records that it chattered) and
resolves a separate `drawnCrossing` from the two blade *segments* for the **light**.

**One line of that fix shipped wrong for twenty minutes and the pixels caught it**:
`lastCrossing` returned the new point while `renderInk` still drew the old one, so the
guard and the renderer disagreed — §11.2b(f)'s failure mode in reverse. It was found by
cropping the capture at 3× and looking, not by any test. Recorded because it is the
argument for §11.2's "a reviewer must look at the actual captured pixels".

### 3.3 Delivered, and the miss

Warm-bright mask `luminance ≥ 248 and r − b ≥ 8`, whole frame, centroid, minimum
distance to each of the two largest cool-bright blade clouds (the pass-2 review's
instrument, re-implemented):

| frame | pass 2, px / dist to blade A / to blade B | pass 3, same |
|---|---|---|
| 9 | 185 px, 44.8 px = 0.113, 74.4 px = 0.188 | **152 px, 2.5 px = 0.006, 62.4 px = 0.158** |
| 10 | 228 px, 31.6 px = 0.080, 51.9 px = 0.131 | **276 px, 2.6 px = 0.007, 35.7 px = 0.090** |
| 11 | 394 px, 10.8 px = 0.027, 15.5 px = 0.039 | **111 px, 3.1 px = 0.008, 12.9 px = 0.033** |
| 12–21 | 358–513 px, welded to one blade, 0.11–0.19 from the other on 8 frames | **49, 11, 9 … px — embers, not a core** |

(threshold ≥240; at the review's ≥248 the star is one frame in pass 3 and eleven in
pass 2. Figure height 394–403 px.)

**The placement is fixed and the duration is a miss.** The core is now 2.5–3.1 px from
the nearer blade on every frame it is drawn, against 10.8–44.8 px; it is still **0.158 of
a figure height from the *second* blade on frame 9**, because the drawn steel tapers and
stops short of the geometric segment end, so "in the fork" in world units is not yet "in
the fork" in lit pixels. That is the same taper/glow disagreement the pass-2 debt records
between `analyse blades` and `Rehearsal`, and it is the reason the placement half of the
guard must not be quoted as evidence. `Scheduler.CLASH_SPAN = 0.27`
is the largest multiple of the contact span that keeps every drawn frame honest, and
0.27 × 0.168 s is **three frames at 60 Hz**, which is too short for §5's "soft star bloom
with 4–6 long soft rays". `InkFxRenderer.clash` was given a rise-and-fall envelope so it
ignites rather than appearing at full brightness — a pure decay is a snap-on, invisible
while the directive ran 0.63 s because every legible frame sat inside the first 6% of the
age curve — and it still peaks lower than pass 2's did.

The light cannot be longer than the meeting. **The meeting is 0.066 s of a 0.168 s
contact span, 39%.** That is item 4 below and the two are one defect.

---

## 4. The held breath — at spec, and now asserted rather than described

**Closed.** `RehearsalTest.theHeldBreathIsAtSpecOnEveryScene`, sampled at 120 Hz of wall
time over the whole score of every `Duel.Kind`:

| scene | ramps | floor | span per ramp |
|---|---|---|---|
| `duel-parry` | 1 | 0.850× | 0.25 s |
| `duel-knockback` | 1 | 0.850× | 0.25 s |
| `duel-phrase` | 5 | 0.850× | 0.25 s each |

**The pass-2 debt's §5 said "the held breath is still 0.857× for ~0.12 s… and there is
still none on the knockback". Both halves were false** and the review proved it; the
0.12 s was the plateau below 0.90×, which is what an eased ramp reaching its floor
necessarily looks like. The paragraph is deleted. Reported against §7.3 it would have
sent this pass to lengthen a constant that has been at spec since before pass 1.

Observed red: `Timing.HELD_BREATH_SECONDS = 0.12` gives
*"PHRASE: the held breath runs 0.125 s per ramp over 5 ramp(s)… asks for ~0.25 s"*.

The reviewer's temporary instrument `src/test/java/dev/starfall/direct/RevTimingDumpTest.java`
is **deleted**, its measurement folded into the assertion above.

---

## 5. Protected results, re-measured

Pass 2's failure to re-measure what it changed was itself a review finding. All three
results the pass-2 review verified are re-measured here on `s4-p3-*` captures.

### 5.1 Phrase continuity — HOLDS, 43× the control

Instrument re-implemented: hero = largest 8-connected component with luminance < 95
after a 3×3 opening; silhouette resampled by area-average into a 64×64 grid spanning
**its own bounding box**; mean absolute difference of consecutive grids.

| capture | steps | min | p05 | median | max |
|---|---|---|---|---|---|
| `s4-p3-null-static` (static control) | 23 | 0.00000 | 0.00000 | 0.00002 | **0.00005** |
| `s4-p3-phrase-60hz` | 417 | **0.00217** | 0.01025 | 0.04787 | 0.39392 |

The hero's silhouette never comes to rest at any of 417 consecutive 1/60 s steps across
6.95 s, at **43×** the control's noise ceiling (the review read 44×). Tracked component
boxes: frame 0 `x119..168 y562..660` (50×99), frame 150 `x209..393 y339..676` (185×338),
frame 300 `x358..530 y330..675` (173×346), frame 390 `x140..208 y494..658` (69×165).

The quiet hole is still the quietest moment and is still in the same place — global
minimum at **step 328, t = 5.48 s** — but the longest run below 0.02 is now **14 steps =
0.234 s** against the review's 26 steps = 0.434 s.

### 5.2 The held breath — HOLDS. See §4.

### 5.3 The blade trail — HOLDS, no kink, and unchanged in every way the review complained about

Local-background residual `L − uniform_filter(L, 61)` on `s4-p3-parry-contact` frames
2/6/10/14, crop `x250..760 y150..470`, amplified ×18. **A single continuous smooth arc on
every frame.** No polyline kink, no strobing, no discrete blade poses; §5's "must curve"
and §7.2's "smear, not strobe" both still pass.

And nothing else about it has moved, because item 6 was dropped:

- still a near-closed dome roughly a figure height across;
- still does not taper;
- still invisible. Frame 6 apex `(450,215)` reads **225.3** against a paper of **219.3** —
  2.7% above paper. Points at `(400,230)`, `(500,212)`, `(560,230)`, `(620,265)` read
  223.2, 224.3, 221.5, 221.8. Identical to the review's readings.

---

## 6. The parry: the blades still meet, marginally better, and the meeting is still an instant

| instrument | pass 1 | pass 2 | pass 3 | acceptance |
|---|---|---|---|---|
| pixel, `analyse blades` | 0.213 | 0.0287 | **0.0264** | ≤ 0.02 |
| geometry, `RehearsalTest` | 0.2105 | 0.0000 | 0.0000 | ≤ 0.02 |

`./gw analyse -Pargs="blades out/captures/s4-p3-parry-contact --max 0.02"` — minimum
**0.0264 of a 395 px figure at frame 9**, clouds `x551..594 y273..380` (389 px) and
`x570..644 y360..447` (386 px). Frame 11 reads 0.0287 with clouds of 422 px and 201 px,
bit-for-bit the pass-2 result on a 394 px figure. **A miss, by 0.6 percentage points.**

The defender's blade is present on every frame of the contact window and its cool-bright
cloud is 201 px at frame 11 against pass 2's 177 px. It is still a stub against the
hero's 422 px, and §4.3 of the review is not answered.

**The meeting is still an instant.** The two blade segments are within 2% of a figure
height from t = 1.550 to t = 1.616 — **0.066 s of a 0.168 s contact span, 39%.** §7.1
wants meet-at-40, slide, part-at-55, i.e. the whole span.

**A lever that lengthens it was found, measured and rejected** — recorded at
`Director.FIST_DROP` so the next pass does not spend the budget again. Placing the fist
target at the crossing's own height instead of a blade-length below it brings both hand
targets inside the arm's reach (the current target is 0.74 world units from a shoulder
whose arm reaches 0.56, so **both arms saturate and the bind is held by the blade aim
rather than by the hands**) and takes the bind from 0.066 s to **0.118 s, 1.8×**. It also
takes the delivered blade separation from 0.0287 to **0.0388** and makes the defender's
blade **disappear entirely on frames 11, 14, 15, 18 and 19**. The geometry preferred it
and the picture did not, and `Rehearsal` "is not a substitute for pixels".

The saturation is the real finding: **both arms are about 0.6–0.75 world units from the
targets they are given, on a 0.56-unit arm.** Any pass that wants the parry to have a
span has to fix that first, and the two candidate levers are the contact height
`Stage.Y_MIDDLE` (1.00, which puts the hands at 0.77 — below the hips) and
`Figure.BLADE_CROSSING`.

---

## 7. Not touched, and named so the next pass does not have to discover it

The pass-2 review's brief had eight items. Items **6** (the trail, the flecks, the quad
edge, the embers) and **7** (the dusk sky) were dropped deliberately and before anything
else, as the brief instructed. Item **5** (give the motion a source) was dropped for
budget.

- **§7.0.1 — the pelvis has no horizontal motion relative to its own stance, in both
  scenes.** The review measured exactly 0.0000 figure heights and a local hip/hand median
  of 1.8% (parry) and 0.5% (phrase); **System 2 was failed at 1.5%.** Nothing in this
  pass touched it. The fix the review names is a directive that translates and rotates a
  body, and the debt has listed that seam since pass 1.
- **The chain still arrives together.** Hips, shoulder and elbow peak on one 1/120 s
  sample; shoulder, elbow and wrist arrive 0.025 s apart. §10's last row is a
  fail-on-sight row.
- **The blade trail** is unchanged in extent, taper and value. §5.3 above has the numbers.
- **The shed flecks are still axis-aligned rectangles** and the straight quad boundary
  beside the bloom is still there. Both are visible at 3× on
  `s4-p3-parry-contact` frame 9, crop `x500..720 y260..440`. §3's first line and §10's
  polygon-silhouette row both apply on sight.
- **The embers are still 3–5 warm blobs** against §5's 8–20.
- **`Palette.SKY_ZENITH / SKY_MID / SKY_HORIZON / SKY_HORIZON_HOT` are still referenced
  by exactly one scene and it is `SmokeScene`.** Family B's colour script has never been
  drawn in a graded capture, and §2.2's warm/cool opposition is still inverted across the
  whole frame: warm ground, warm blades, warm bloom.
- **§7.3's ink bloom still does not appear in a delivered frame.** `Duel.Kind` needs a
  fourth entry resolving a `Hit`; `ContactTest` has the fixtures.
- **The knockback's figures still converge**, 169 px in 0.535 s with a static camera.
  Not re-measured this pass — the `Shoved`/`Moved` interaction is untouched.
- **The four seams pass 1 named** are all still open.

### What this pass did not measure

Stated plainly, per the standard the pass-2 review set:

- The **knockback and the phrase were not re-shot for the clash or the value change**
  beyond `s4-p3-phrase-60hz`. The phrase carries no `CLASH` directive, and the sash split
  applies to both figures in every scene, so `s4-p3-phrase-60hz` is the only evidence
  that the value change did not damage the planning framing — and it was measured for
  **continuity only**, not for value.
- **No matched-scale part count was run this pass.** §11.0's first act is the reviewer's
  and this pass did not pre-empt it, but that means the claim "the pale duellist now
  reads as a figure" rests on one 3× crop and a value ratio, not on a part count.
- **The corridor profile was not run on the phrase**, only on the parry window.
- **`analyse` has no command for the two-figure value ratio.** It exists only as
  `CorridorProfile.medianInkOverGround` and `DuellistValueTest`. A reviewer wanting it on
  another frame has to write a test.

---

## 8. Every guard this pass wrote, and the proof it was observed red

§11.2b(f): *no assertion counts as a guard until it has been observed red.*

| guard | broken by | message it printed |
|---|---|---|
| `RehearsalTest.everyClashIsDrawnWhereTwoBladesActuallyAre` | reverting `lastCrossing`/`renderInk` to the aim point | *"PARRY: at t=1.5833 the drawn clash mark (1.305,1.839) is 12.6% from the hero's blade and 18.3% from the foe's. It is sitting on a grip."* |
| `RehearsalTest.theHeldBreathIsAtSpecOnEveryScene` | `Timing.HELD_BREATH_SECONDS` 0.25 → 0.12 | *"PHRASE: the held breath runs 0.125 s per ramp over 5 ramp(s). STYLE.md 7.3 asks for ~0.25 s"* |
| `CorridorProfileTest.referenceImageThreePassesEveryFloor…` | `torso` floor 0.014 → 0.06 (the criterion two passes chased) | *"STYLE.md 11.0: the corpus must pass the criterion the corpus set. Reference image 3's torso band reads 0.0148… against a floor of 0.06."* |
| `CorridorProfileTest.theReferenceProfileIsStable…` | the same | *"reference band torso at factor 0.75 span x0..831 y283..955 reads 0.0148… against floor 0.06"* |
| `DuellistValueTest.theTwoDuellistsAreTellableApartInDeliveredPixels` | pointing it at `s4-p2-parry-contact/frame_011.png` | *"…The corpus reads 3.27x; this reads capture: dark 0.269 …, ratio 1.54x"* |
| `analyse corridor --profile` (exit code) | run on `s4-p2-parry-contact` and on `s4-p3-parry-contact` | both **FAIL**; 8 of 24 and 11 of 24 frames pass every band |

Two guards are known-answer rather than red-observed and are labelled as such:
`theProfileReturnsTheGapItIsToldToMeasure` (analytic gap, 119 px and 19 px on a 200 px
figure) and `theProfileSaysOneMassRatherThanZeroWhenTheBodiesTouch` (the null case).

**One honest caveat on the clash guard.** Now that `drawnCrossing` is built from the two
blade segments, the two *placement* assertions (`markToBlade`, `markIsBetweenTheBlades`)
are close to tautological — they are kept as regression traps and because they were
observed red on the pre-fix code, but **the load-bearing half is
`bladeGapFraction() <= MET` on every drawn frame**, which reads the two blades against
each other and has nothing to do with the mark. A reviewer should treat the placement
half as a trap, not as evidence, and should grade placement from the pixel table in §3.3.

---

## Commands, so the next pass does not have to reconstruct them

```
./gw capture  -Pscene=duel-parry       -Pout=out/captures/s4-p3-parry-contact \
              -Pframes=24 -Pcols=6 -Pstart=1.42 -Pstep=0.0167 -Pw=960 -Ph=720
./gw capture  -Pscene=duel-parry-debug -Pout=out/captures/s4-p3-parry-contact-debug \
              -Pframes=24 -Pcols=6 -Pstart=1.42 -Pstep=0.0167 -Pw=960 -Ph=720
./gw capture  -Pscene=rig-bindpose     -Pout=out/captures/s4-p3-null-static \
              -Pframes=24 -Pcols=6 -Pstart=0.0 -Pstep=0.0167 -Pw=960 -Ph=720
./gw capture  -Pscene=duel-phrase      -Pout=out/captures/s4-p3-phrase-60hz \
              -Pframes=418 -Pcols=22 -Pstart=0.0 -Pstep=0.0167 -Pw=960 -Ph=720

./gw analyse  -Pargs="blades   out/captures/s4-p3-parry-contact --max 0.02"
./gw analyse  -Pargs="corridor out/captures/s4-p3-parry-contact --profile"
./gw analyse  -Pargs="diff     out/captures/s4-p3-null-static out/captures/rev-p2-null-static"
./gw test --tests '*RehearsalTest*'
./gw test --tests '*CorridorProfileTest*'
./gw test --tests '*DuellistValueTest*'
```

`analyse corridor --min 0.06` — the whole-column form — is **deprecated as an
acceptance** and says so in its own help text. It still runs, because the two previous
passes' numbers are quoted against it.
