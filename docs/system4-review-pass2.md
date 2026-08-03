# System 4 — independent review of pass 2

# FAIL

Pass 1's closing line set the bar: *"Fix one aiming bug and one corridor, and this becomes
the first pass in the project that could answer §0 with a yes."* Pass 2 fixed the aiming bug
— genuinely, visibly, and by the right method. It did not fix the corridor, it confirmed a
value regression it named itself, and the guard it shipped for the clash bloom **cannot
fail**. The answer to §0 is still no.

System 4 is at **pass 2 of 5**.

---

## 0. The one-sentence test

> Could this frame be cropped out of one of the eight reference images and not look out of place?

**No.** The capture is trying to match **Family B** (duel, two figures, crossed blades, clash
bloom — §1 names it "the primary template for the game screen").

Matched-scale comparison, done first per §11.0. Reference image 3 (`inspirations/image -
2026-08-02T101033.164.png`, 832×1088) downscaled by 0.586 so its left duellist's figure
height is 394 px — the same as the hero's in `s4-p2-parry-contact` frame 11 — set beside the
capture cropped to `x250..830 y250..720`.

Three things kill it at that scale, in order of how fast the eye finds them:

1. **The stage is not Family B.** Cream paper ground, a pale straw band, grass strokes, and
   cyan/magenta bokeh. There is no dusk sky, no violet transition, no coral horizon.
   `Palette.SKY_ZENITH`, `SKY_MID`, `SKY_HORIZON`, `SKY_HORIZON_HOT` are defined in
   `art/Palette.java` and are referenced by exactly one scene in the repository —
   `capture/SmokeScene.java`. **Family B's colour script has never been drawn in a graded
   capture.** §2.2's "warm/cool opposition carries the mood" is inverted here: warm ground,
   cool-ish figures, warm blades, warm bloom — warm-on-warm across the whole frame, which
   §2.2 forbids by name.
2. **There is one value where there should be two.** See §5 below.
3. **Part count.** At 394 px, reference image 3 resolves, per figure: topknot, face in
   profile, neck, shoulder, sleeve, forearm, two hands on a grip, tsuba, blade, obi, a second
   sheathed blade at the hip, hakama with pleats, two legs, two feet, ink smoke off the
   shoulder — fourteen. The capture's hero resolves eight (hair mass, head blob, shoulder,
   arm, fist + a black tsuba bar, ochre sash, skirt, one foot, blade). The foe resolves five
   and **does not read as a body**: a hair mass, an ochre smear, a scabbard bar, a black
   wing-shaped mass on its far side, and a stub of blade. §11.0's corollary applies — the
   material cannot be better than the subject it is painting.

The planning framing is worse. `rev-p2-phrase-60hz` frame 000 is an 80%-empty cream page with
two ~92 px figures pushed into the bottom-left corner. It is not Family C (no fog-filled
meadow, no dissolving background figures); it is a blank sheet with two specks on it.

---

## 1. What is missing

**Not evocative enough**, with a secondary **technically broken** in the apparatus.

Every element the corpus asks for is now present somewhere in the frame — two figures, two
blades that cross, a soft warm star at the crossing, ink smoke, fog, motes. They do not add
up to a picture from the corpus, because the three things that make Family B legible at a
glance — *dark body / clear ground / pale body*, *two long blade slivers*, *a hot horizon
behind cool ink* — are all absent or collapsed. And the guard that is supposed to keep the
bloom honest is a tautology, which is the apparatus failure §11.2b exists to catch.

---

## 2. Apparatus check, before anything else (§11.2b)

- `out/captures/rev-p2-null-static` — `rig-bindpose`, 24 frames, start 0.0, step 0.0167,
  960×720, `commit=31e4f76-dirty`, `harness=f0ad18994eec`. **Bit-identical to
  `s4-p2-null-static`: 0 of 691,200 pixels differ, max channel delta 0.** The harness hash
  matches the one that produced every `s4-p2-*` capture, so comparisons across this review and
  pass 2's own captures are in scope. Nothing from `s4-p1-*` is quoted — those manifests carry
  no `harness=` line, which §11.2b(d) makes a harness version of its own.
- `out/captures/rev-p2-phrase-60hz` — `duel-phrase`, 418 frames, start 0.0, step 0.0167,
  960×720, same commit and harness. Shot for this review because pass 2 never re-shot the
  phrase.
- Every pixel statistic below was taken with an **independently written reader** (NumPy/SciPy,
  8-connected labelling, `0.85 × paper` ink threshold, 3×3 opening) rather than through
  `dev.starfall.analysis`, per §11.2b(c). Where the checked-in tool was used it is named.
- `./gw test` — **BUILD SUCCESSFUL**, whole suite green.

---

## 3. The three results pass 2 did not re-measure

Pass 2 states plainly that four of its changes touch the phrase and the knockback and that it
re-measured none of them. This was the first task of the review. **None of the three has
regressed. One of them is materially better than pass 2 believes.**

### 3.1 The phrase's continuity — HOLDS, at 44× the control

Instrument (pass 1's, re-implemented): segment the hero as the largest 8-connected component
with luminance < 95 after a 3×3 opening; resample its silhouette by area-average into a 64×64
grid spanning **its own bounding box**, so the statistic is scale- and translation-invariant
and the camera cancels; take the mean absolute difference of consecutive grids.

| capture | steps | min | p05 | median | max |
|---|---|---|---|---|---|
| `rev-p2-null-static` (static control) | 23 | 0.00000 | 0.00000 | 0.00002 | **0.00004** |
| `rev-p2-phrase-60hz` | 417 | **0.00197** | 0.00940 | 0.04513 | 0.30356 |

**The hero's silhouette never comes to rest at any of 417 consecutive 1/60 s steps across
6.95 s, at 44× the control's noise ceiling.** Pass 1 read 0.00229 against 0.00009; the
elbow-pole change, the STRIKE settle and the pooling change did not touch this.

Regions: the tracked component's box is recorded per frame — frame 0 `x119..169 y569..661`
(50×92, planning framing), frame 150 `x209..394 y339..677` (185×338), frame 300
`x358..531 y330..676` (173×346), frame 390 `x140..203 y494..659` (63×165). The component is
the hero on every sampled frame; the box shrinking at the ends is the camera, not a lost lock.

The 0.317 s hole pass 1 named is still there and is still the quietest moment of the phrase:
the global minimum sits at **step 328 (t = 5.48 s)**, inside pass 1's 312–330 window. The
longest run below 0.02 is **26 steps = 0.434 s starting at t = 4.91 s**.

### 3.2 The held breath — HOLDS, and `docs/system4-debt.md` is wrong about it

Headless measurement per §7.1, driving the same schedule/director/rig the capture drives:
`dev.starfall.direct.Rehearsal` played at **120 Hz of wall time** over the whole score of each
`Duel.Kind`, reading `Director.timeScale()` per sample. Instrument shipped as
`src/test/java/dev/starfall/direct/RevTimingDumpTest.java` (writes `out/review/rehearsal-*.csv`).

| scene | ramps | floor | full span below 1.0× | mean over the ramp | span at or below 0.90× |
|---|---|---|---|---|---|
| `duel-parry` | 1, at wall 1.583–1.833 | **0.850×** | **0.258 s** | 0.918× | 0.108 s |
| `duel-knockback` | 1, at wall 1.583–1.833 | **0.850×** | **0.258 s** | 0.918× | 0.108 s |
| `duel-phrase` | 5 (wall 0.467, 1.300, 3.383, 4.325, 5.467) | 0.850× | 0.258 s each | 0.918× | 0.108 s each |

§7.3 asks for "a soft time ramp, ~0.85× for ~0.25 s". The delivered ramp is **0.850× over
0.258 s**. `Timing.HELD_BREATH_SCALE = 0.85` and `HELD_BREATH_SECONDS = 0.25`, unchanged since
commit `367cd3b`, before pass 1.

**Two statements in `docs/system4-debt.md` §5 are false:**

- *"The held breath is still 0.857× for ~0.12 s against §7.3's ~0.25 s."* The ramp is 0.258 s
  end to end. The 0.12 s figure is the **plateau below ~0.90×** (I measure 0.108 s), which is
  what an eased ramp reaching 0.85 at its floor necessarily looks like. Reported as a shortfall
  against §7.3 it is a misreading of pass 1's own instrument, and it would send pass 3 to
  lengthen a constant that is already at spec.
- *"and there is still none on the knockback."* There is one, at the shove contact, **identical
  in floor, span and shape to the parry's**. Reproduce:
  `./gw test --tests '*RevTimingDumpTest*'` then read `out/review/rehearsal-duel-knockback.csv`
  column `timeScale` — 31 consecutive samples below 1.0 beginning at wall 1.583.

This is the most valuable thing in the pass and the pass does not know it has it.

### 3.3 The blade trail — no regression, no kink, and pass 1's complaint unfixed

Pass 2's stated worry was that two new aim links could introduce a kink. **They did not.**

Instrument: local-background residual, `L − uniform_filter(L, 61)`, on `s4-p2-parry-contact`
frames 2/6/10/14, crop `x250..760 y150..470`, amplified ×18 for viewing. The trail is a single
continuous smooth arc on every frame. No polyline kink, no strobing, no discrete blade poses.
§5's "must curve" and §7.2's "smear, not strobe" both still pass.

What has not changed:

- It is still a **near-closed dome**, running from the left of the hero's head, over the top of
  the frame and down past the foe — roughly a figure height across, and it is still visibly
  present at frame 22, i.e. **0.37 s after the blade has left it** (`Scheduler.TRAIL_SECONDS`
  = 0.40).
- It does not taper. §5 requires "brightest at the leading edge, fading over the tail"; the arc
  is even in value along its length.
- It is very faint: sampled on frame 6 at the apex, `(450,215)` reads luminance **225.3**
  against a paper of **222.0** — 1.5% above paper. Points along it at `(400,230)`, `(500,212)`,
  `(560,230)`, `(620,265)` read 223.2, 224.3, 221.5, 221.8. So the "moon" is both large and
  nearly invisible, which is the worst of both: it occupies the frame without reading as a
  stroke.

---

## 4. The parry

### 4.1 The headline number reproduces, and the fix is real

`./gw analyse -Pargs="blades out/captures/s4-p2-parry-contact --max 0.02"` reproduces exactly:
**minimum 0.0287 of a figure height at frame 11**, clouds `x551..621 y304..400` (421 px) and
`x610..628 y336..400` (177 px), on a 394 px figure. The headless geometry agrees:
`out/review/clashes-duel-parry.txt` reports `clash at=1.568 site=CROSSING bladeGapFraction=0.0`.

**And at viewing scale the blades meet.** Frame 11 at 1:1 shows a shallow X with a warm star in
the fork. Against pass 1's 0.213 — a head and shoulders of clear paper — this is a different
picture, and it was got by the right method: a headless rehearsal that can ask where the blade
is without shooting a capture. That instrument is the best engineering in the pass and it should
outlive System 4.

### 4.2 But the meeting is an instant, not a span

`analyse blades`, frame by frame across the contact window:

| frames | separation / figure height |
|---|---|
| 9 | 0.0320 |
| **11** | **0.0287** |
| 12 | 0.0809 |
| 10, 13–21 | 0.078 – 0.163 |

Two frames of the ten-frame contact span are inside 4% of a figure height. §7.1 is explicit
that "the middle span *is* the contact, and contact is a span rather than an instant… the
blades meet at 40, slide and redirect through the span, and part at 55". Delivered, they touch
at 40 and are 8–16% apart for the rest of it. The deflection is still being asserted by the
schedule rather than drawn.

### 4.3 The foe's blade barely exists

At frame 11 the foe's cool-bright cloud is **177 px in a 19×65 box**; the hero's is 421 px in
71×97. In the reference both blades are long conspicuous slivers running well past the crossing
into open sky, and §5 makes the blade "the point of visual focus in every duel reference". Here
one duellist has a sword and the other has a stub. Zoomed to 2.5× (`x570..850 y290..700`, frame
11) the foe's blade is a 19-px-wide streak that never leaves its own body ink.

---

## 5. The clash bloom

### 5.1 Where it actually lands, in delivered pixels

Region: warm-bright mask `luminance ≥ 248 and r − b ≥ 8`, whole frame; centroid of that mask;
minimum distance from the centroid to each of the two largest cool-bright blade clouds
(`b − r > −6`, `luminance > 212`, ≥120 px after a 2×2 opening). `s4-p2-parry-contact`,
figure height per frame from `analyse blades`.

| frame | bloom px | centroid | dist to blade A | dist to blade B |
|---|---|---|---|---|
| 9 | 25 | (631,324) | 54.3 px = **0.137** | 74.9 px = **0.190** |
| 10 | 56 | (631,322) | 34.7 px = **0.088** | 51.3 px = **0.130** |
| **11** | 105 | (620,327) | 14.0 px = 0.036 | 14.9 px = 0.038 |
| 12 | 93 | (614,333) | 8.2 px = 0.021 | 44.6 px = **0.111** |
| 13 | 128 | (608,337) | 2.0 px | 64.8 px = **0.161** |
| 14 | 66 | (602,344) | 2.8 px | 71.4 px = **0.175** |
| 15 | 31 | (599,353) | 1.1 px | 75.6 px = **0.185** |
| 16–19 | 17→2 | (592,360)→(582,364) | 63–75 px = **0.149–0.181** | 3.1–4.6 px |

**The bloom is drawn on eleven frames. On one of them is it within 4% of a figure height of
both blades.** On its first two frames it is in open paper, 0.09–0.19 of a figure height from
both. On the remaining eight it is welded to one blade and 0.11–0.19 from the other.

Pass 1's diagnosis was "a light that asserts an event the picture does not contain", with the
bloom 223 px from the defender's blade. That distance is now 45–76 px. **The defect is reduced
by a factor of three, not closed**, and the bloom still *begins* two frames before the meeting
— the same ordering error pass 1 named.

### 5.2 The test that is supposed to prevent this cannot fail

`RehearsalTest.everyClashIsDrawnWhereTwoBladesActuallyAre` has three assertions. The first
(`bladeGapFraction() <= 0.02`) is real and load-bearing. The second (`origin().site() ==
CROSSING`) is a schedule-level enum check — the same class of assertion that let pass 1 ship
green. **The third is a tautology.**

```java
private static double pointToSegment(Rehearsal.Frame f, boolean hero) {
    Rehearsal.Body b = hero ? f.hero() : f.foe();
    mark.set(b.bladeCross());
    return Rehearsal.segmentDistance(mark, mark, b.bladeRoot(), b.bladeTip()) / FIGURE_HEIGHT;
}
```

`bladeCross()` is `Figure.bladeAt(BLADE_CROSSING)` with `BLADE_CROSSING = 0.24f`;
`bladeRoot()` is `bladeAt(0f)` and `bladeTip()` is `bladeAt(1f)`; and
`Skeleton.worldAlong(bone, d)` returns `origin + d · unitAxis`. All three points are collinear
with `0 ≤ 0.24L ≤ L`, so **the mark is on the segment by construction and this function returns
exactly 0.0 for both bodies, always.** The assertion `toHero <= 0.10 && toFoe <= 0.10` is
`0.0 <= 0.10 && 0.0 <= 0.10`. Its failure message — *"the clash mark is X from the hero's blade
and Y from the foe's. It is sitting on a grip."* — describes a condition it is structurally
incapable of detecting.

It also never reads the mark that is drawn. `Director.lastCrossing(...)` is the drawn position;
the test computes `resolved` from it in `theResolvedCrossingStaysWhereTheScheduleStagedIt` and
then never uses the variable, asserting instead on `Director.stretch(clash.origin())`, a
schedule quantity.

So `docs/system4-debt.md`'s claim — *"the clash bloom now provably lands on the blade
intersection, with `RehearsalTest.everyClashIsDrawnWhereTwoBladesActuallyAre` failing the build
otherwise… That is §11.2b(e) put in the tool"* — is **half true**. The blade-gap half is a real
guard. The bloom-placement half is precisely the failure §11.2b(c) warns about, one level up: a
number compared against the object it was derived from. And the frame-by-frame table above is
what it would have caught.

### 5.3 Poetic beat or impact effect?

The bloom itself is on the right side of the line: soft warm-white core with four long soft
rays, no shake, no hitstop, no freeze, `timeScale` ramping to 0.85 instead. At threshold 240 it
spans `0.253` of a figure height against the reference's `0.095` at threshold 200 — larger in
ray extent, comparable in area (√area / figure height 0.062 vs 0.055). That is acceptable.

What is not:

- **The embers are not there.** §5 wants 8–20 warm `#FF9A4D` embers floating like paper ash.
  Counting strongly-warm blobs (`r − b > 60`, `r > 200`, `g < r − 40`, ≥4 px) gives **5, 4, 3,
  3, 3** on frames 9, 11, 13, 15, 17 — and some of those are the Family-C sky motes, not embers.
- **The shed flecks are axis-aligned squares.** At 8× on frame 13 (`x545..665 y300..420`) the
  pale flecks around the bloom are literal hard-edged rectangles, and the dark ones are
  diamonds. §3's first line — "Nothing in this game has a hard edge except the blades" — and
  §10's "symmetric, uniform particle bursts" both apply on sight.
- A **straight quad boundary** is visible crossing open paper below-right of the bloom on the
  same crop: a value step along a perfectly straight diagonal. That is a compositing edge, and
  §10 fails a pass on sight of a visible polygon silhouette.

---

## 6. The corridor — honest about the conflict, wrong about the cause, and chasing a number
that does not reproduce

### 6.1 The pass's numbers reproduce

My reader, `0.85 × paper`, 3×3 opening, ground band `y ≥ 655` excluded, widest run of zero-ink
columns between the two bodies' bounding-box centres:

- **zero on 19 of 24 frames** (pass 2 reports 75%; I get 79% with a slightly different ground
  cut), **median 0.000**;
- the two bodies are **one connected ink component on frames 13, 14, 15 — 3 of 24 = 12.5%**,
  exactly pass 2's figure.

### 6.2 The stated cause understates the overlap by an order of magnitude

Pass 2: *"it is zero because the two figures' arms overlap in x by about eight pixels at the
bind… the left body's rightmost ink column is at 596-612 while the right body's leftmost is at
588-594."*

Measured: hero's rightmost ink column minus foe's leftmost ink column, per frame, components
assigned by centroid side of x=480, ground band excluded.

| frame | 9 | 10 | 11 | 12 | 13–15 | 16 | 17 | 18 | 19 | 20 | 21 | 22 | 23 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| overlap px | 10 | 28 | 21 | 12 | *merged* | 120 | 20 | 14 | 33 | 89 | 77 | 25 | **8** |
| / figure height | .025 | .071 | .053 | .030 | — | .291 | .049 | .033 | .078 | .208 | .179 | .057 | .020 |

**"About eight pixels" is the value on one frame out of twenty.** The typical overlap through
the contact span is 20–120 px, 5–29% of a figure height, and on three frames the bodies are one
component. On frames 16, 19–22 the overlap is not a touch at all — no single row spans it — it
is the hero's *extended sword arm at chest height* projecting past the foe's *hem at ankle
height*. Two different heights, no contact, and the whole-column corridor statistic reads zero
anyway.

### 6.3 The acceptance number does not reproduce on the reference

Pass 1 set the acceptance from image 3: *"image 3's clear gap is 42 px at its narrowest (6.3%
of figure height)… It is never zero"* → *"the corridor must never be zero and must not fall
below 6% of figure height."* Pass 2's document repeats it as *"a clear corridor of 6-11%."*

Measured on `inspirations/image - 2026-08-02T101033.164.png`, figure height 672 px (left
duellist, head y283 → feet y955), ink = `L < k × row-sky`, row-sky = median of columns 0..70
and 760..832, 3×3 opening, widest zero-ink column run in `x250..600`:

| row band | widest clear run | / figure height |
|---|---|---|
| heads y283..400 | 57 px | 0.085 |
| torsos y400..560 | 10 px | **0.015** |
| hands/hilts y430..520 | 10 px | **0.015** |
| sashes y560..700 | 63 px | 0.094 |
| skirts y700..880 | 70 px | 0.104 |
| feet y880..960 | 48 px | 0.071 |
| **whole figure y283..955** | **10 px** | **0.015** |
| **whole frame column** | **0 px** | **0.000** |

Stable from `k = 0.60` to `k = 0.90` — 10 to 12 px throughout.

**Reference image 3 scores 0.015 on the whole-column corridor statistic, and 0.000 if the
column spans the frame.** Pass 1's per-band readings (8.5–10.4%) are right; the number it
turned into an acceptance threshold — 6% as a *whole-column minimum* — is roughly **4× stricter
than the corpus it was derived from, and the reference fails it.** The "42 px at its narrowest"
figure does not appear in any band I can find.

That is why pass 2's tuning went the way it did. `LANE_SPREAD` 1.35 → 1.55 bought five frames;
1.70 broke the parry. Of course it did — it was being pushed toward a target the corpus itself
cannot hit.

### 6.4 What is actually wrong with the staging

Same instrument applied to the capture, per band (`s4-p2-parry-contact`, x-window 400..700,
`0.85 × paper`, 3×3 opening), against the reference:

| band | reference | capture, contact span (frames 9–23) |
|---|---|---|
| heads | 0.085 | 0.037 – 0.385 |
| torso / hands | **0.015** | **0.000 on 14 of 15 frames** |
| sash | 0.094 | 0.085 – 0.425 |
| skirt | 0.104 | 0.127 – 0.344 |
| feet | 0.071 | **0.393 – 0.439** |
| whole column | 0.015 | 0.000 |

**The capture is wider than the reference in every band except the one that matters.** At the
feet it is five to six times wider. At the hands it is zero where the reference keeps 10 px of
sky. The two figures are standing too far apart at the base and leaning into each other at the
top — which is exactly what widening the lane produces, and it is the opposite of the corpus,
where the duellists stand close and the *bodies* stay narrow at the pinch.

So: the conflict pass 2 reports is real, and handing the staging layer forward is right. But the
diagnosis it hands forward — an 8 px arm overlap — is wrong by an order of magnitude, and the
target it hands forward is unreachable. **A pass 3 that takes this brief at face value will
widen the lane again and break the parry again.**

Against the aesthetic test — "two figures that read as one silhouette is a fail-on-sight" — the
honest answer is split. Below the sash the capture reads unambiguously as two figures. At chest
height, for 14 of 15 contact frames, it reads as one mass, and on frames 13–15 it *is* one mass.
That is a fail, but it is a local fail at the bind, not the whole-silhouette merge pass 1
described.

---

## 7. §7.0 — the positive test

Measured headless in world units (`SamuraiRig.FIGURE_HEIGHT = 1.70`), camera-free by
construction of the instrument rather than by assertion, from
`out/review/rehearsal-duel-parry.csv` and `rehearsal-duel-phrase.csv` at 120 Hz.

### 7.1 Does the motion have a source? **No.**

Motion of each joint **relative to the figure's own stand position**, as a fraction of figure
height:

| joint | `duel-parry` rel-x range | `duel-phrase` rel-x range | y range (parry) | rel path (parry) |
|---|---|---|---|---|
| **hips** | **0.0000** | **0.0000** | 0.0118 | 0.034 |
| shoulder | 0.0513 | 0.0624 | 0.0464 | 0.192 |
| head | 0.0856 | 0.0837 | 0.0274 | 0.225 |
| elbow | 0.389 | 0.379 | 0.358 | 2.184 |
| hand | 0.638 | 0.664 | 0.610 | 3.127 |
| blade tip | 1.374 | 1.387 | 1.498 | 7.250 |

**The hero's pelvis has exactly zero horizontal motion relative to its own stance, in both
scenes.** Every pixel of hip travel in the frame is the figure changing tile (`standX` 0 → 0.16,
0.094 of a figure height). Vertically it moves 0.0118 of a figure height — about **4.6 px on a
394 px figure** — while the hand travels 3.13 figure heights (≈1230 px) and the tip 7.25.

Hip path / hand path over the whole parry: **1.1%**. Over sliding 0.25 s windows in which the
hand is actually moving (path > 0.15 units), median hip/hand: **1.8% (parry), 0.5% (phrase)**.
**System 2 was failed at 1.5%.**

There is *some* source — the spine bends, so the shoulder gets 5–6% of a figure height and the
head 8.4–8.6%. But §7.0.1's model is "the hip turns before the shoulder"; here the hip is a
welded root and the chain starts at the spine. That is a puppet held by the ribcage.

**This flatly contradicts pass 1's accepted result** — *"hip excursion 0.216 of figure height,
chest 0.202, hip/blade 17.1%… camera-free by construction."* Those were pixel numbers taken
through boxes on a scene whose figure height ranges 4.08×, and §11.2b's own rule is to read
*"by construction"* as **nobody has measured this**. Two independently written instruments
disagree by more than 20×; the headless one is measuring the bone.

### 7.2 Is effort visible? **In the graded beat, yes.**

Elbow interior angle (shoulder–elbow–hand), whole parry: min 66.2°, p10 104.1°, median 146.4°,
p90 160.1°, max 177.2°. **Through the contact span t 1.40–1.75: min 102.3°, median 120.7°, max
162.5°** — inside §7.0.2's 90–130° for the beat that matters. The arm declines to straighten
where it counts. Two caveats: the elbow is above 165° for 6.8% of the score, and the upper arm
sits at a median **81°** from the torso axis, where §7.0.2 wants it "hanging near the torso
axis, the *blade* doing the reaching".

### 7.3 Does anything arrive at the same time? **Yes — three things, on the same sample.**

Peak-speed instants in the strike window (t 1.30–1.75), 120 Hz:

| joint | peaks at | speed |
|---|---|---|
| hips | **t = 1.575** | 2.06 u/s |
| shoulder | **t = 1.575** | 11.61 |
| elbow | **t = 1.575** | 13.13 |
| hand | **t = 1.400** | 17.25 |
| blade tip | **t = 1.400** | 31.20 |

Hip, shoulder and elbow peak on **one sample**; hand and tip on **one sample**. §10's last row
— "everything peaking on the same frame" — is a fail-on-sight row and this is two clusters of it.

Arrivals in the recovery (last time each joint's speed exceeds 1% of its own peak after the
contact): shoulder 2.771 s, elbow 2.779 s, hand 2.796 s — **a 0.025 s spread, 1.5 frames,
across the entire arm**. §7.0.3 requires the band to be *spent* across the chain, not shared;
`Scheduler.STRIKE` (0.30/0.33/0.36/0.39) does declare four different settles, and 1.5 frames is
what they deliver in the picture.

The one place it works: the blade tip is still moving at 1.35 u/s at t = 2.287 when the hand has
fallen to 0.15 u/s. **The tip does drift a beat after the hand has stopped** — §7.0.3's cheapest
poetry, and it is there.

**Verdict on §7.0: merely correct, not poetic.** What would give it an origin: a directive that
translates and rotates the pelvis. `docs/system4-debt.md` §5 already records that *nothing in
the directive vocabulary translates a body*; that gap is not a knockback problem, it is the
reason §7.0.1 fails.

---

## 8. The self-reported contrast regression — CONFIRMED, and quantified

Pass 2: *"pooling the pale figure to the floor narrows [Family B's dark-vs-pale contrast]… it
is no longer obviously the *other* colour."*

Median ink luminance as a fraction of the frame's own ground level (paper 222.0 for the
capture; local row-sky for the reference), ink = `L < 0.85 × ground`:

| | dark figure | pale figure | ratio |
|---|---|---|---|
| **reference image 3**, torso `x190..300 / x540..650, y400..540` | 12.4 = 0.17× | 40.8 = **0.56×** | **3.3×** |
| **capture** frame 11, torso `x300..470 y300..420 / x600..760 y340..460` | 60.6 = 0.27× | 91.5 = 0.41× | **1.51×** |
| reference, skirt `x150..330 / x520..700, y660..820` | 12.1 | 14.1 | 1.16× |
| capture, skirt `x330..470 / x620..760, y500..620` | 35.5 | 56.0 | 1.29× |

**Pass 2's skirt change is right and the reasoning behind it is right.** The reference's
white-clad duellist is near-black below the sash — 1.16× the dark figure — and the capture now
sits at 1.29×, close to the corpus. Pass 1's counter-argument was wrong and pass 2 refuted it
correctly.

**The regression is that nothing compensated above the sash.** The reference's pale duellist
reads at 0.56 of the sky's value against 0.17 for the dark one — 3.3× apart, unmistakable at
any scale. The capture is at 1.51×. In the matched-scale sheet, and in `s4-p2-phrase-check`
across all twelve frames, **you cannot tell which duellist is the pale one.** Family B's
composition is a dark duellist against a pale one; this is a dark duellist against a slightly
less dark one.

The debt's own prescription — a per-region colour channel, pale kimono over dark hakama, rather
than a compromise on the pooling colour — is correct and should be pass 3's second item.

---

## 9. Claims in `docs/system4-debt.md` that do not survive

1. **§5, held breath:** *"still 0.857× for ~0.12 s"* — it is 0.850× over 0.258 s (§3.2 above).
2. **§5, held breath:** *"there is still none on the knockback"* — there is one, identical
   (§3.2).
3. **§1, the bloom guard:** *"the clash bloom now provably lands on the blade intersection,
   with `RehearsalTest.everyClashIsDrawnWhereTwoBladesActuallyAre` failing the build
   otherwise"* — the bloom-placement half of that test is `0.0 <= 0.10` and cannot fail (§5.2).
   In delivered pixels the bloom is ≥0.088 of a figure height from both blades on 2 of the 11
   frames it is drawn on, and ≥0.11 from one blade on 8 more.
4. **§2, the corridor:** *"the two figures' arms overlap in x by about eight pixels at the
   bind"* — 8 px on 1 frame of 20; 20–120 px on the rest; one connected component on 3 (§6.2).
5. **§2, the corridor:** *"Reference image 3 has… a clear corridor of 6-11%"* used as the
   acceptance floor — image 3's whole-column corridor is 0.015 of a figure height and its
   whole-frame corridor is 0.000 (§6.3). The acceptance criterion is unreachable.
6. **Carried from pass 1 and repeated as accepted:** *"hip excursion 0.216 of figure height…
   hip/blade 17.1%"* — the headless rig says the pelvis's horizontal motion relative to its own
   stance is exactly 0.0000 figure heights, and the local hip/hand median is 1.8% (§7.1).

Items 1 and 2 mean pass 2 is *better* than it claims. Items 3–6 mean two of its guards and one
of its acceptance criteria are not load-bearing.

---

## 10. Brief for pass 3, ordered by what matters most

**1. Fix the corridor criterion before touching the corridor, and stop widening the lane.**
The whole-column corridor statistic is refuted by its own reference (0.015, and 0.000 over the
frame). Replace it with the **per-band** statistic in §6.4 and take the targets from image 3:
hands ≥ 0.015, sash ≥ 0.09, skirt ≥ 0.10, feet ≥ 0.07. Then read the capture's own row: it is
already 4–6× *wider* than the corpus at the feet and the skirt, and only fails at the hands.
So the move is the opposite of pass 2's: **bring `LANE_SPREAD` back down toward 1.35 and make
the bodies narrow at the pinch** — `SamuraiRig`'s haori rails at 0.64 against `Stage.BODY_HALF`
0.56 is the number to attack, plus a facing offset during contact (`Stance.PASSING` already has
the mechanism). Closer stances also mean the arms reach less, which helps §4.2 and §7.1 at the
same time. Add the null case the guard is missing: assert that the *reference itself* passes
whatever threshold you adopt.

**2. Give the pale figure back its value above the sash.** Build the per-region colour channel
the debt already identifies — it unblocks §7.3's local dissolve too. Target the reference's
**3.3×** torso ratio (pale duellist at ~0.55 of ground, dark at ~0.20); keep pass 2's skirt
pooling, which is correct at 1.29× against the corpus's 1.16×. Gate it with a test on the two
figures' **delivered pixel value histograms**, not on `InkMaterial`'s base colour —
`DirectorTest.bothFiguresAreVisuallyDistinguishable` is currently asserting the wrong thing.

**3. Make the bloom guard able to fail, then make the bloom honest.** Replace
`b.bladeCross()` with `director.lastCrossing(...)` — the point actually drawn — and assert over
**every frame on which the CLASH is drawn**, not the single frame nearest its instant. Then fix
what that guard will find: the bloom currently ignites two frames before the blades meet and
rides one blade for eight frames afterwards. It should ignite *at* the meeting and its centre
should stay between the two blades while they slide.

**4. Give the parry a span.** Two of ten contact frames inside 4% of a figure height is a
touch. §7.1 wants meet-at-40, slide, part-at-55. And give the defender a blade the eye can
find: 177 px of visible steel in a 19×65 box against the hero's 421 px is not two swordsmen.

**5. Give the motion a source.** The pelvis's horizontal motion relative to its own stance is
exactly zero in both scenes; hip/hand is 1.8% and 0.5%. Add the directive that translates and
rotates a body — the debt lists it as a seam, but it is the whole of §7.0.1. And stagger the
chain: hips, shoulder and elbow currently peak on the same 1/120 s sample, and shoulder, elbow
and wrist arrive 1.5 frames apart.

**6. The trail and the flecks.** Cut the dome's extent and taper it — brightest at the leading
edge, fading over the tail — and lift its value; at +3.3 luminance over paper it is a large
invisible object. Make the shed flecks stop being axis-aligned squares, kill the straight quad
boundary visible in open paper beside the bloom, and put §5's 8–20 warm `#FF9A4D` embers in
where there are currently 3–5 warm blobs.

**7. Then the stage.** `Palette.SKY_ZENITH / SKY_MID / SKY_HORIZON / SKY_HORIZON_HOT` are
referenced by one scene in the repository and it is `SmokeScene`. Until a duel is fought against
a dusk sky, §0 cannot be answered yes for a Family B frame however good the figures get, because
§2.2's warm/cool opposition is inverted across the whole image.

**8. Housekeeping.** Correct `docs/system4-debt.md` §5 on the held breath (it is at spec, on all
three scenes) so pass 3 does not spend budget on a solved item; fold
`src/test/java/dev/starfall/direct/RevTimingDumpTest.java` — this review's headless timing
instrument, left in place per §11.2b(e) — into `RehearsalTest` as an assertion on
`timeScale`'s floor and span, and delete it.

---

## 11. What pass 2 got right, recorded so it is not lost

- **The blades meet.** 0.213 → 0.0287 in delivered pixels, and at viewing scale frame 11 is a
  bind with a light in the fork. That is the single biggest visual improvement in System 4.
- **`Rehearsal` is the right instrument** and the first thing in the project that can assert
  about the picture without a capture. Its three-cause diagnosis of the aiming bug — target
  names a fist, nothing could aim a sword, the pole could not flip inside the beat — is the
  best debugging write-up in the repository.
- **`Scheduler.supersede`** — two directives on one chain at one instant resolved by emission
  order was a real bug and it is really fixed.
- **The region set refuses**, in both directions, and the absolute-pixel escape hatch is the
  right scope for the refusal.
- **The skirt pooling change is correct** and the argument it replaced was wrong; the capture
  now sits closer to the corpus on that ratio than the reasoning it overturned would have
  allowed.
- **The pass did not declare itself passed.** Two criteria met, two missed and named with
  numbers. That is the standard this project should keep.
