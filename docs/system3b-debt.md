# System 3b — faces — the closing record of pass 1

**Status: delivered, one pass.** This document is the record of what System 3b built,
what it measured, what it did not do, and what in the inherited documents did not
survive measurement. Written in the format of `docs/system4-debt.md`, for the same
readers: the reviewer of this pass, System 3c, and System 5.

Every capture quoted is `s3b-p1-*`, shot at `commit=4bdde59-dirty`,
`harness=f0ad18994eec` — **the same harness hash as every `s4-p2-*` through `s4-p5-*`
capture**, so comparisons back to System 4 are in scope per STYLE.md §11.2b(d). The
rig, the shaders and the face system are the subject and changed; the apparatus did
not. Every pixel number below is printed beside its rectangle (§11.3).

---

## 0. The spans and the boxes, load-bearing for every number below

- The parry window is unchanged (`-Pstart=1.42 -Pstep=0.0167 -Pframes=24`, 960×720),
  and `ParryWindowTest` still derives **378 rows, crown 299** from the scene.
- **The face box is derived, not drawn**: `FaceWindowTest.foeHeadBox()` computes the
  foe's head from `Rehearsal` plus the schedule's own framing arithmetic — the same
  independent path `ParryWindowTest` uses for the span — and on frame 11 it resolves
  to **`x568..653 y283..368` (86×86)**. A guard whose rectangle is pinned to where the
  head *is* cannot drift to where the defect isn't.
- The audit's box `x610..690 y360..420` is kept for continuity everywhere the audit's
  numbers are re-taken.

---

## 1. What System 3b delivers

**The instrument first, because the brief ordered it first.**

1. **`analyse facets` — the straight-edge instrument of audit C2, checked in.**
   `Facets.java` counts straight axis-aligned luminance edges (|ΔL| > 8, run ≥ 6 px)
   per 1000 px through an explicit `--rect` (it refuses without one, §11.3), at a 1-px
   base (the audit's definition) and a 2-px base (the anti-evasion reading), with an
   optional `--mask-blade` (cool-bright L > 130, b − r > 4, dilated 1 px — §2.1's own
   mask, because §5 licenses exactly one hard edge and the parry crosses every
   interesting box). Proven against known answers on synthetic frames
   (`FacetsTest`: a six-block grid counts exactly 12 V + 12 H; a 2-level/px ramp
   counts zero), and **pinned to the audit's own numbers on the tracked
   `s4-p4-parry-contact/frame_011.png`: V 26 longest 48, H 19 longest 17, 9.1 per
   1000 px — exact on all five.** The definition cannot drift silently.

2. **A face.** Both duellists now carry STYLE.md §4b's five profile elements, built
   as two new meshes per rig in two new merge groups:
   - `faceMesh()` — the skin field, drawn with the §4b.2 palette (`SKIN_*`, `BLUSH`,
     `LIP`, added to `Palette` from the table STYLE.md already carries). `deep` IS
     the cool grey-violet, so §4b.7's brown shadow is unreachable by construction.
     Four radial rails inside the profile contour: a **narrow break of light** along
     brow–nose–chin (2–3 px at the intimate framing), everything inward pooling
     toward shadow, a socket that sinks with age, a scalp cap covering the whole
     skull. `dissolve` is 0 on every skin vertex — §4b.1.
   - `faceInkMesh()` — brow stroke (weight from the generator), lash-and-iris on the
     eye bone, nostril, lip parting, and a beard mass when the generator grew one
     (the beard carries dissolve on its free rim: it is hair, not skin).
   - The **specular** is a soft screen-blended fan through the renderer's glow path
     (`lightSpeck`), warm `#FFF6E2`, off-centre toward the light, killed by a closed
     lid and by distance. It is the one §4b.4 dot and it cannot print a hard edge —
     its rim is authored at zero alpha.
3. **The profile contour now carries identity.** `FaceParams` reshapes the
   brow/nose/lip/chin radius table (nose group and jaw group scaled as ratios to the
   hero's own parameters, so the hero's contour is bit-identical to the table three
   rig passes tuned). §4b.6's "an old face is a different silhouette" is geometry:
   age sinks the socket and softens the jaw (`FaceGenTest.anOldFaceIsADifferentSilhouette`).
4. **The four channels, damped.** `FaceRig`: brow, eyelid (the eye bone's scaleY,
   collapsing toward the upper lash line where the bone's origin sits), jaw, gaze —
   first-order settles staggered at 0.30 / 0.40 / 0.44 / 0.56 s across §4b.6's own
   band (`FaceRigTest` measures the settles by integration, not by reading the taus
   back). Zero overshoot by construction and by test.
5. **Expression is combat state.** The targets come from the `Stance` channel the
   scheduler already raises from engine events — a condition table exactly like
   `Stances`' trunk shapes (GATHERING draws the brow down and narrows the eye;
   YIELDING closes the eye and opens the jaw; SLACK is §4b.6's death row). No clips.
6. **The gaze channel is finally honoured.** The Scheduler has authored a gaze anchor
   on every `PoseChange` since System 4, and `Director.poseChannel`'s own javadoc
   recorded not honouring it as "a gap worth naming". It now resolves the anchor
   through the same lane stretch as every other anchor and hands it to the face.
   `FaceWindowTest.bothDuellistsLookAtEachOtherThroughTheContact` proves both gazes
   point at the opponent through the parry, headless, on the director the captures run.
7. **Variety from a seed; the hero authored.** `FaceParams.generate(seed)` spans
   family D's range (measured over 400 seeds: age 0.00–1.00, brow weight spread
   0.65, ~45% beards, ~30% scars, marks placed asymmetrically off the seed); the
   foe's face is keyed on its body id so every capture reproduces. The hero is
   `FaceParams.hero()`, constants, pinned by test.
8. **Detail resolves on push-in** (§4b.0 / §3b.1 for authored marks).
   `InkMaterial.covScale` fades the face-ink group's coverage with the delivered
   head size — 0 at ≤ 26 px of head, 1 at ≥ 44, smoothstep between, continuous in
   the camera width so it inherits the schedule's no-cut guarantee.
   `FaceWindowTest.theFaceDetailResolvesOnPushInAndOnlyThere`: the parry's intimate
   framing delivers a 67 px head at fade 1.0; the phrase's planning framing delivers
   25.1 px at fade 0.0; no 0.5-px step of the fade exceeds 0.06.
9. **427 tests, 0 skipped, 0 failures**, and the two `CorpusTest` failures this
   worktree inherited on a clean checkout are paid: `s3-p1-hair` and
   `s3-p1-reversal` frames are now force-added (audit C13's last two open items).

---

## 2. The numbers, before and after

### 2.1 The face region (the head, where the face actually is)

`analyse facets`, blade-masked, base 1 px, through the derived head box
`x568..653 y283..368` on frame 11 of the parry window:

| capture | per 1000 px | longest run |
|---|---|---|
| `s3b-p1-parry-inherited` (pre-3b code, same harness) | 4.6 | 18 |
| `s3b-p1-parry-bareface` (3b code, face not drawn) | 4.6 | 18 |
| **`s3b-p1-parry-contact` (delivered)** | **5.7** | **20** |
| reference image 3's own dark-duellist head, matched scale (`ref3-matched-378.png`, box 120,150,86,86, unmasked) | **6.8** | **22** |
| the audit's lattice (C2, their box, their frame) | 9.1–11.5 | **44–62** |

**The delivered face sits in the corpus's own register** — a real face *adds*
straight-edge density (the corpus's adds more than mine), and what separates a face
from the lattice is the run length: 20–22 against 44–62. `FaceWindowTest` holds both
edges of that band, and `theCorpusPassesThisGuard` keeps the reference's side of it in
the suite (§11.0 — and the criterion **was** moved by running it on the corpus first:
a 6.0 density ceiling failed image 3's own face and was widened to 8.0 with the run
ceiling carrying the discrimination).

### 2.2 The audit's box, and what was actually in it

The audit's `x610..690 y360..420` on the current staging's frame 11 reads **10.1
unmasked / 7.5 blade-masked** before this pass and **identically after**, because —
measured by disabling passes one at a time (`STARFALL_SKIP` probes, since removed) —
its content is: **the blade (V-runs of 44–50 px at x614–637, licensed by §5)**, the
pale figure's **garment** strip boundaries, and the **hair-mass** erode steps. The
foe's head on frame 11 is at y283–368, above that box. See §5.1–5.3 for what this
does to the inherited documents. The garment and hair-boundary residual (4.9–7.5
masked against a sky of 0.2–1.0) **is real and goes to 3c with the corrected
mechanism**: it is pale-strip cov edges and hair-mass erosion against a pale ground,
not skull facets and not a face.

### 2.3 The delivered face, visually

- `s3b-p1-parry-contact/frame_011.png` (published) beside
  `s3b-p1-parry-bareface/frame_011.png` (published): the bareface hero head is a
  featureless dark lobe; the live one resolves forehead, a warm-lit brow ridge, a
  deep-set eye with one specular, the nose's break of light, an open mouth under
  effort (BRACED holds jaw 0.30), chin, jaw shadow. The foe reads a bowed grey-violet
  head with the same vocabulary in the pale colourway.
- At the planning framing (`s3b-p1-wide` vs `s3b-p1-wide-bareface`, frame 0): the two
  captures differ by **1,849 px confined to `x131..264 y516..566`** — the skin
  field's suggestion only; the ink marks are faded out. §4b.0's wide-framing rule is
  delivered as a feature, not an apology.

### 2.4 The matched-scale part count (§11.0), author's own count

Sheet: reference image 3 at 378/672 beside `s3b-p1-parry-contact` frame 11 (the
reviewer must re-take this — §11.0 wants the count from someone who did not produce
the work; this row is the author's claim, not the standing measure):

- **Hero:** hair mass, trailing wisps, **face patch with brow–nose–chin light break**,
  **eye + specular**, jaw/neck shadow, shoulder, upper arm, forearm/fist, blade, ochre
  sash, sheathed-sword diagonal, skirt mass, hem smoke ≈ **13** (was 9, with no face).
- **Foe:** streaming hair, **face patch + eye**, shoulder, arm, blade, sash, skirt,
  near leg, far leg, foot ≈ **10–11** (was 9, with no face).
- The remaining gap to the corpus's ~25 is named and it is not 3b's: **both hands on
  the grip, tsuba, fold lines, feet on the ground, collar** — mesh-authoring items
  handed to 3c by System 4's own debt (items 4's garment half, 7), plus the ear
  (§4b.3 calls it a bonus; not built, see §6).

### 2.5 Determinism (§11.2b(g)), and what the face costs there

Same command, same commit, twice:

| pair | frames bit-identical | px changed | character |
|---|---|---|---|
| `s3b-p1-parry-bareface` / `-bareface-repro` | 17 of 24 | 26,546 | the fleck/star class System 4 recorded, unchanged |
| **`s3b-p1-parry-contact` / `-repro`** | **2 of 24** | 11,445 | flecks on the same frames, **plus ±1-LSB noise at the hero-head box (~`x400..480 y270..360`) on most frames** |

**The face rendering adds run-to-run noise of max channel delta 1** — invisible, and
bit-identity at the head is gone all the same (the exact class of §4's `u_dusk`
finding). Not bisected; the suspects are the two extra quarter-res blur/resolve passes
per figure quantising borderline 8-bit values. Consequence, stated as System 4 stated
its own: **no absolute single-pixel claim at a head on any one frame**; the facet
readings used here are stable across the pair (5.68 vs 5.7 per 1000). → §6, not done.

---

## 3. Contract amendments, named loudly

1. **`MAX_BONES` 32 → 36** (`ink_skin.vert`, `InkSkinnedRenderer`,
   `SimTimingTest.boneBudgetHoldsUnderTheGlesCap` updated). The skeleton was already
   at **31** bones — `SamuraiRig`'s own javadoc said 28, which was stale before 3b
   arrived (21 body + **10** cloth: five back, three front, two sleeve) — and the
   three face bones (brow, eye, jaw) take it to **34**. 36 mat4 = 144 vec4 against
   the GLES 3.0 guaranteed 256 the original cap was derived from. Two slots remain
   for 3c's far sleeve / far hakama.
2. **`ink_skin.frag` gains `u_covScale`**, branched at 1.0 exactly as the haze term
   is, for the recorded reason: multiplying by 1.0 is the IEEE identity but the
   recompile is not bit-innocent. Every pre-3b scene renders with it at 1.0.
3. **`InkMaterial.stainPale` is now a material field** (was the hard-coded
   `OCHRE_PALE` in the resolve) and participates in the merge-group key — skin needs
   a lip-toned pale rim where garments need rust.
4. **Face bones exist in the headless skeleton too**, so `Rehearsal` carries the
   face channels and every gaze/expression claim is testable without GL.

---

## 4. Every guard, and the proof it was observed red

All five were broken by hand **in this pass**, watched fail with the message quoted,
restored, and the suite re-run green (427/0/0).

| # | guard | broken by | printed |
|---|---|---|---|
| A | `FaceRigTest.everyChannelSettlesInsideTheBand` | `TAU_JAW` 0.14 → 0.50 | *"jaw settles in 1.9583333333333333 s, outside STYLE.md 4b.6's 0.3-0.6 s band"* |
| B | `FaceWindowTest.theFoeHeadRegionIsNoLongerTheLattice` | density ceiling 6.0 → 2.0 | *"5.6787452677122765 straight edges per 1000 px through x568..653 y283..368 (86x86) (blade-masked)…"* |
| C | `FaceWindowTest.theFaceDetailResolvesOnPushInAndOnlyThere` | fade knee 26 px → 6 px | *"the planning framing delivers a 25.09804 px head and the face's ink is still at 1.0 coverage; 4b.0 wants a suggestion, not a face"* |
| D | `FaceGenTest.theGeneratorSpansTheFamily` | `age = r.nextFloat()` → `0.5f` | *"age spans 0.5..0.5"* |
| E | `FacetsTest.aKnownBlockGridIsCountedExactly` (+2 siblings) | the instrument's step condition `>` → `<` | *"six blocks, two vertical boundaries each ==> expected: <12> but was: <111>"* |

Additionally, **the red run for guard B is checked in as a permanent test**:
`theInheritedLatticeWouldFailThisGuard` runs the guard's reader on the tracked
inherited frame and asserts it fails both ceilings (9.1 per 1000, longest 48) — the
failure mode re-exhibits on every build instead of living in a log.

**The adversarial exhibits (§11.2b(f), the newest clause), and one attempt SUCCEEDED:**

- *Attempted:* build an axis-aligned block lattice that satisfies the facet
  instrument. **Succeeded twice, and both successes are checked in as tests.**
  `aTwoPixelStaircaseEvadesTheOnePixelBase`: risers of 7 levels over 2 px are
  invisible at base 1 (caught at base 2 — which is why the tool prints both).
  `aThreePixelStaircaseOfFourLevelRisersEvadesBothBases`: a 12-level lattice spread
  over three 4-level risers passes **both** bases. **So the guard's honest scope is:
  no near-single-pixel axis-aligned lattice.** A 3-px-soft 12-level grid would pass
  it and still read faintly as blocks at 8×. Widening the base needs its own null
  first (a genuinely soft 6-px ink edge must not convict every hem) — recorded as
  open, not silently claimed closed.
- *Attempted:* fool the head-box guard by having the face somewhere other than where
  the box is. Could not: the box is derived from the schedule and skeleton, the same
  arithmetic the camera uses; moving the face moves the box.
- *Known hole, named rather than found the hard way:* the guard reads **frame 11
  only**. A lattice appearing exclusively on other frames of the window would pass
  it. The window's other frames were spot-read at 4.6–7.5 through the same box class
  during this pass, but no per-frame assertion exists. → §6.

Known-answer rather than red-observed, and labelled as such:
`theAuditsNumbersReproduceOnTheTrackedFrame` (pins the instrument to the audit),
`theCorpusPassesThisGuard` (pins the criterion to the reference),
`theSpecularDiesWithTheLidAndWithTheDistance`, `theHeroFaceIsPinned`.

---

## 5. Claims in the inherited documents that do not reproduce

Every pass of this project has found at least one. This pass found three, and they
matter because two of them redirect 3c.

### 5.1 The audit box does not contain the face, and about half its headline number is the blade

Audit C2 titles its region *"foe's face `x610..690 y360..420`"* and hands 3b **11.5
straight edges per 1000 px** there. Measured on this pass's captures at the same
harness: the foe's **head** on frame 11 spans y283..368 — the audit's box sits below
it, on the **chest and sleeve**, and the parry's blade crosses it. Disabling passes
one at a time: skip-blade drops the box from 10.1 to 7.5 per 1000 and the longest
V-run from 50 to 21 (the 44–50 px runs the audit counted are **the blade's own
licensed edge**); skip-hair *raises* it to 11.7 with an H-run of 62 (hair was
covering part of the artefact — and the audit's own H-longest of 62 appears exactly
when hair is removed, suggesting their frame's hair simply lay elsewhere); skip-fx
changes nothing. On the tracked `s4-p4` frame the same box blade-masks from 9.1
down to **4.9**. The defect the audit found is real — 4.9–7.5 masked against a sky
of 0.2–1.0 — but it is **garment strips and hair-mass erosion, partly obscured by
whichever way the hair blew, plus the blade**, and its magnitude unmasked is roughly
half steel. **3c should chase the pale figure's garment boundaries and the hair-mass
erode, not a skull.**

### 5.2 Debt item 4's "pale face patch" was never a face — it was the haori colour on the skull

The record says the foe's *"head and skull print flat-shaded polygon facets"* and the
audit corrected that to a sampled lattice. Both descriptions miss the mechanism this
pass hit when it painted over it: **the head lobes were drawn in the CLOTH merge
group**, so the pale duellist's skull rendered in `CLOTH_PALE` — and its own
`sashLift = 0.35` then washed the head's pooling flat (the lift applies to everything
above the sash line, which includes the whole skull). The "pale face patch against
the hair mass" was the pale figure's **garment colour on its head, value-flattened by
a garment compensation**. The fix was not to soften anything: it was to give the head
its own material (the skin group), which is what §4b wanted anyway.

### 5.3 The audit's C2 straight-edge table is confirmed — with its convention now named

The four rows I could re-take on tracked pixels reproduce exactly (s4-p4 face box
26/48/19/17/9.1; flecks/sky/torso rows to the decimal on the re-shot window). What no
document recorded: **the metric was blade-unmasked**, and in a parry window the blade
is in the box. `analyse facets` prints both readings so this convention can never be
silent again (§11.3, applied to a convention rather than a rectangle).

### 5.4 Smaller corrections

- `SamuraiRig`'s "21 body bones plus System 3's seven cloth bones is 28, four spare"
  — the cloth chains add **ten** bones, the skeleton stood at **31**, and there was
  **one** spare, not four. `SimTimingTest` had silently pinned the truth (31) while
  the javadoc said 28.
- `Director.poseChannel`'s claim that gaze needed "a mechanism no layer below System
  4 provides" was true when written; the schedule side, however, was already
  complete — every `PoseChange` carried a resolved gaze anchor. The whole channel
  cost eleven lines to honour.

---

## 6. What System 3b does not deliver (permanent debt of this pass)

1. **The garment/hair residual in the audit's box.** 7.5 per 1000 blade-masked on the
   live frame, unchanged by this pass; mechanism corrected in §5.1. → **3c**.
2. **Per-frame coverage of the lattice guard.** Frame 11 only; the other 23 frames
   are spot-read, not asserted. → **the next pass to touch the head**.
3. **The facet instrument's 3-px hole.** The succeeded adversarial exhibit
   (§4): a lattice softened over 3 px passes both bases. Closing it needs a wider
   base with its own null (a 6-px soft hem edge must read clean). → **whoever next
   needs the instrument to carry an acceptance alone; today it never does — the
   run-length ceiling and the corpus row carry the guard with it.**
4. **±1-LSB head-region non-determinism**, unbisected (§2.5). Invisible; breaks
   bit-identity on ~20 more frames of a 24-frame pair. → **nobody; obey it** (no
   absolute single-pixel claims at a head), unless a bit-identity control is ever
   needed there again.
5. **Hair does not know about the face.** The generator has no hair-mass axis
   (§4b.6 lists one), the hairline is wherever the hair sim's roots blow, and at the
   wide framing the scalp cap sometimes reads as a pale patch where the corpus's
   hairline would cover it. `SamuraiHair` was deliberately not touched. → **3c or a
   dedicated hair pass**, with the generator's `hairMass`/`facialHair` axes wired in.
6. **The iris is ink-black, not `IRIS` warm-black**; freckle specks ride the stain
   gate's granulation rather than being placed as §4b.5's discrete varied dots; the
   scar prints lip/blush-toned (skin group) rather than vermillion-ochre; there is
   no ear. All are push-in bonuses under §4b.3's own hierarchy. → **a later face
   pass, if any**; none of them blocks §0.
7. **Faces exist in the duel scenes only.** `SimScene`, `RigSwingScene` and the
   Family A corpus scenes draw `rig.mesh()` alone, so a Family A capture still shows
   the pre-3b head. The System 5 `LaneScene` likewise does not draw the face meshes
   yet — **the combat view the decision names is the duel; wiring `LaneScene` is
   System 5's one-line adoption**, and its planning framing already sits below the
   fade knee. One subject change does reach every scene that builds a pale figure:
   `Figure.pale` now generates its `FaceParams`, and the generator reshapes the
   **profile contour** in the body mesh — so the pale figure's head silhouette is no
   longer bit-identical to pre-3b captures even where the face meshes are not drawn.
   The hero's contour is bit-identical (its parameters reproduce the authored table
   exactly, by construction). → **System 5**.
8. **No face gallery.** Variety is proven by parameter spread (400 seeds) and by the
   one generated foe on screen; no capture shows a row of generated faces at
   push-in scale. → **the reviewer may ask for one; it is a scene, not a system.**
9. **The knockback capture** (`s3b-p1-knockback`) is the first ever shot on the dusk
   stage — System 4's debt item 11 said it never had been. It exists and shows the
   YIELDING/CARRIED expressions in motion; nothing in it is measured. → **System 5**,
   as item 11 already says.

---

## 7. What is right and must not regress

- **The face is exempt from the dissolve** (§4b.1): every skin vertex is authored at
  dissolve 0, and the beard is the one deliberate exception (it is hair).
- **The brown shadow is unreachable**: skin `deep` is the grey-violet, per material,
  not per tuning.
- **The expression never snaps**: first-order channels, staggered settles measured in
  suite; the specular dies with the lid.
- **The wide framing has a suggestion of a face and nothing to shimmer**: fade 0 at
  ≤ 26 px of head, and the live/bareface wide pair differs only in the skin field.
- **The hero's contour is the authored table, bit-exact**, and a generated face is a
  reshaping of the same line — identity lives in the silhouette, per §4b.3.

## Commands

```
./gw capture -Pscene=duel-parry          -Pout=out/captures/s3b-p1-parry-contact \
             -Pframes=24 -Pcols=6 -Pstart=1.42 -Pstep=0.0167 -Pw=960 -Ph=720
./gw capture -Pscene=duel-parry          -Pout=out/captures/s3b-p1-parry-repro      (identical: the dynamic control)
./gw capture -Pscene=duel-parry-bareface -Pout=out/captures/s3b-p1-parry-bareface   (same window: the absent-subject control)
./gw capture -Pscene=duel-parry-bareface -Pout=out/captures/s3b-p1-parry-bareface-repro
./gw capture -Pscene=duel-phrase          -Pout=out/captures/s3b-p1-wide -Pframes=6 -Pcols=3 -Pstart=0.0 -Pstep=0.30 -Pw=960 -Ph=720
./gw capture -Pscene=duel-phrase-bareface -Pout=out/captures/s3b-p1-wide-bareface   (same window)
./gw capture -Pscene=duel-knockback       -Pout=out/captures/s3b-p1-knockback -Pframes=24 -Pcols=6 -Pw=960 -Ph=720

./gw analyse -Pargs="facets out/captures/s3b-p1-parry-contact --frame 11 --rect 568,283,86,86 --mask-blade"
./gw analyse -Pargs="facets out/captures/ref3-matched-378.png --rect 120,150,86,86"
./gw analyse -Pargs="diff out/captures/s3b-p1-parry-contact out/captures/s3b-p1-parry-repro"
./gw test
```

`s3b-p1-parry-inherited` is kept on disk: it is the pre-3b code shot at this harness,
and it is the control that makes §2's before/after a measurement of this pass rather
than of a harness. `ref3-matched-378.png` (reference image 3 at 378/672, Lanczos) is
tracked because `theCorpusPassesThisGuard` reads it.
