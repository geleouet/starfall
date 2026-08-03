# System 3b — faces — review of pass 1

**Reviewer:** independent; did not build this system and has no stake in any prior decision.
**Subject:** the worktree at `4bdde59-dirty`, harness `f0ad18994eec`, captures `s3b-p1-*`.
**Instrument:** an independently written Python/numpy reader (§11.2b(c) — it crosses an
*implementation* boundary, not a call site), plus my own eyes on the frames.
Every number below is printed beside its rectangle (§11.3).

---

# 1. VERDICT: **FAIL**

---

## 2. The one-sentence test (§0)

**No.** Frame 11 of `s3b-p1-parry-contact` could not be cropped out of reference images 3, 4
or 5 — the family this scene is explicitly built to match (Family B, dusk duel) — and the
head is now the reason, where before it was merely absent.

Two different failures, one per duellist:

- **The pale duellist's face is a decal.** At matched scale it is a flat, stair-edged pale
  shard beside the clash bloom. Its face plane reads **1.36× the local sky luminance**;
  reference image 3's two faces read **0.25×** and **0.31×**. In the corpus the face is one of
  the *darkest* things on the figure, found by a socket and a two-pixel break of light. Here
  it is one of the *brightest* things in the frame. The builder's own §5.2 named this
  mechanism correctly and then only half-removed it: bareface 1.62× → delivered 1.36×.
- **The hero's face is a carved polygon mask.** It reads at intimate framing, which is real
  progress over a featureless lobe, but it is built from straight-line facets, a hard-edged
  black rectangle for the open mouth (which detaches from the jaw into open sky on frames
  16–17 and 22–23), and a bright white dot for an eye with no dark iris behind it. §3's
  "nothing in this game has a hard edge except the blades" is violated at exactly the place
  §4b.1 removed the dissolve that would otherwise have hidden it.

The builder says its first attempt read as a decal and was reworked. **The rework cannot be
verified**: no capture of the first attempt is on disk, so only the end state is gradeable.
The end state still reads as a decal on the pale duellist.

**What is missing, in the rubric's vocabulary: *too literal*, and on the pale duellist *technically broken*.**
The references find a face with a handful of soft marks. This pass *draws a face* — a nose
quad, a mouth quad, a specular quad, a skin polygon — and every one of them is a discrete
hard-edged object. It is a diagram of a face rendered into a painting.

---

## 3. My own matched-scale part count (§11.0)

**Regions, so this is checkable.** Capture figure span **378 rows, crown 299** — the
project's own `ParryWindowTest` derivation. Reference image 3 scaled 378/672 = 0.5625, i.e.
the tracked `out/captures/ref3-matched-378.png` (468×612).

| panel | file | crop |
|---|---|---|
| reference, dark duellist | `ref3-matched-378.png` | `x10..249 y150..549` |
| reference, pale duellist | `ref3-matched-378.png` | `x250..489 y150..549` |
| capture, hero | `s3b-p1-parry-contact/frame_011.png` | `x295..534 y285..684` |
| capture, foe | `s3b-p1-parry-contact/frame_011.png` | `x520..759 y275..674` |

**Reference, dark duellist — 27 readable parts:** topknot; loose crown wisps; hair mass /
hairline; forehead + brow ridge with coral rim; eye socket; nose; lip + moustache; chin +
beard; jaw/neck shadow; haori collar; shoulder; upper arm; forearm; rear hand on grip; front
hand on grip; tsuka; tsuba; blade; obi; sheathed second blade with its saya line; hakama fold
strokes; front leg; rear leg; front foot; rear foot; ground smear/splash; sleeve ink cloud.

**Reference, pale duellist — 24**, the same list minus the second sheathed blade, one foot
and the ground splash.

**Capture, hero — 14:** hair lobe; thin trailing wisps; skin patch; brow–nose–chin light
break; eye specular; mouth block; jaw/neck value change; shoulder/arm mass; forearm/fist
mass; ochre sash; second ochre stain; sheathed-sword diagonal; skirt mass; hem smoke.

**Capture, foe — 11:** streaming hair; hair wisps; pale head shard; ochre profile rim; sash;
scabbard diagonal; blade; torso mass; near leg; far leg; foot.

> **Hero 14 / 27 = 0.52. Foe 11 / 24 = 0.46.**

The builder's own count (hero ≈13, foe ≈10–11) **reproduces**, and I confirm the movement
from System 4's 9. But two qualifications matter more than the arithmetic:

1. **Three of the seven new marks are marks the eye finds and misreads.** The specular is not
   read as an eye (no dark anchor); the black block is not read as a mouth (it is a rectangle
   hanging off the chin); the pale shard is not read as a face at matched scale at all. §11.0
   counts "a distinct mark there", so I have counted them — but a mark that reads as the wrong
   thing has not moved §0, and the brief's own warning ("a face that adds parts on a
   spreadsheet but not to the eye has not moved §0") is the right one here.
2. **The gap is still roughly half**, and the largest remaining items are exactly the ones
   System 4's debt named: both hands, the grip, the guard, the feet, the collar, the fold
   lines. Faces were "the largest single item" on that list and they have delivered about a
   fifth of the shortfall.

---

## 4. The finding I most want on record: the face's *value* is a paper-ground value

Face-plane luminance against the local sky, both measured on the same frame, regions printed.

| subject | face box | face mean L | sky box | sky mean L | **face / sky** |
|---|---|---|---|---|---|
| ref3 **dark** duellist | `x156..170 y196..214` | 25.7 | `x190..210 y165..185` | 103.6 | **0.25** |
| ref3 **pale** duellist | `x296..310 y200..220` | 30.0 | `x270..288 y170..190` | 95.6 | **0.31** |
| capture **hero** | `x412..434 y296..322` | 48.6 | `x460..480 y295..320` | 87.1 | **0.56** |
| capture hero, *bareface* | same | 30.6 | same | 87.1 | 0.35 |
| capture **foe** | `x580..604 y292..320` | 118.3 | `x540..560 y290..315` | 87.3 | **1.36** |
| capture foe, *bareface* | same | 141.5 | same | 87.3 | 1.62 |
| capture foe, **planning framing** (`s3b-p1-wide/frame_000`) | `x205..249 y518..551` | 88.1 | `x300..339 y500..539` | 73.3 | **1.20** |

Two consequences.

**(a) On the hero, the face made the head lighter.** 0.35 → 0.56, i.e. *away* from the corpus's
0.25. §2.2's amendment excuses a figure that reads too pale because `#161A22` (L 25.7) is a
floor and the bareface head is already sitting on it (box min = 25.7 exactly). It does not
excuse adding 18 luminance points on top.

**(b) This is §2.2's own defect, one section later, unnoticed — and that is a correction §4b
owes.** §4b.2's skin palette was sampled from family D, which is faces on *cream paper*.
`SKIN_BASE #E3D2BB` is L 210; even `SKIN_DEEP #5E5C68` is L 94, **3.7× the ink floor**. Used
against Family B's dusk sky it produces exactly what §2.2 already diagnosed for `#161A22`: a
paper-ground absolute applied where a ratio-to-ground is needed. §2.2 says "the next pass to
touch the palette owes that fraction, measured, with the region recorded."

> **Measured, here, for the face: family B paints the face plane at 0.25–0.31× of its own
> local sky luminance, on both duellists, and the break of light peaks at 0.59–1.22× as a
> band 2–3 px wide. That is the fraction §4b.2 is missing, and it is the single most
> load-bearing number in this review.**

---

## 5. The instrument

### 5.1 The pinning is real, and I verified it across an implementation boundary

My reader is written from the *definition* (|ΔL| > 8 between pixels `base` apart, contiguous
run ≥ 6 along one row or column, Rec.709 luma), not from `Facets.java`.

On the tracked `out/captures/s4-p4-parry-contact/frame_011.png`, rect `x610..690 y360..420`
(81×61), base 1, unmasked:

```
V 26 longest 48 | H 19 longest 17 | 9.11 per 1000 px
```

**Exact on all five of audit C2's numbers.** Blade-masked on the same box: **4.86** (the debt
says 4.9 ✓). The whole `s3b` §2.1 table also reproduces to the digit through the derived head
box `x568..653 y283..368` (86×86), blade-masked, base 1: delivered **5.68/20**, bareface
**4.60/18**, inherited **4.60/18**; and `ref3-matched-378.png` box `120,150,86,86` unmasked
**6.76/22**. The pinning claim is honest and the instrument is the best-built artefact in
this pass.

### 5.2 My third attack — **it SUCCEEDS, by a mechanism neither prior attack touches**

Both of the builder's successful attacks soften the **riser** (they attack the 8-level step
threshold). Mine attacks the **run**: `minRun = 6` is an unguarded parameter.

**The exhibit.** A hard-edged, axis-aligned block lattice, cell 12 px, three value levels 26
apart — steps far above the threshold, boundaries a single pixel wide — with **every boundary
carrying a ±2-px sawtooth of period 5 px**. Files:
`atk_a0p5.png` (control, no jitter) and `atk_a2p5.png` (the attack), 132×132.

| | base 1 | base 2 | base 3 |
|---|---|---|---|
| plain lattice (control) | 1.15/1000, **longest 132** | 2.30, longest 132 | 3.44, longest 132 |
| **+ ±2-px jitter, period 5** | **2.64/1000, longest 7** | **7.58/1000, longest 7** | 14.23, longest 132 |

At 5× the jittered field is unmistakably a hard axis-aligned block lattice — arguably worse
than the control, because the sawtooth adds aliasing. It passes
`theFoeHeadRegionIsNoLongerTheLattice`'s ceilings (≤ 8.0 per 1000, longest ≤ 30) at **both**
bases the tool prints, with room, and it **scores better than the delivered face (5.68) and
better than every corpus reading (6.76–10.55)**.

The general rule: **any boundary jitter of amplitude ≥ base and period < minRun defeats the
instrument at that base.** Widening the base — the debt's own proposed remedy for its 3-px
hole — does not touch this; raising the jitter amplitude tracks it for free.

### 5.3 The scope is **not** honestly stated everywhere it is used

`FacetsTest`'s own javadoc says, in terms: *"this is why `analyse facets` prints both, and why
**no guard in this project may quote the 1-px number alone**."*

Every guard in this project quotes the 1-px number alone:

- `FaceWindowTest.theFoeHeadRegionIsNoLongerTheLattice` — `Facets.measure(frame, box, 8, 6, **1**, true)`
- `FaceWindowTest.theCorpusPassesThisGuard` — base **1**
- `FaceWindowTest.theInheritedLatticeWouldFailThisGuard` — base **1**
- `FacetsTest.theAuditsNumbersReproduceOnTheTrackedFrame` — base **1**
- every row of `system3b-debt.md` §2.1 and §2.2

**No assertion anywhere requires an evader to beat base 2.** The instrument's documentation is
honest; the guards built on it are not, and they contradict the documentation by name.

### 5.4 And the delivered artefact itself sits in the gap

Derived head box `x568..653 y283..368`, blade-masked, per 1000 px:

| | base 1 | base 2 | base 3 |
|---|---|---|---|
| delivered | **5.68** | **20.82** | **36.91** |
| bareface | 4.60 | 16.90 | 32.99 |
| corpus (ref3 dark, `120,150,86,86`) | 6.76 | 17.58 | 25.55 |

The delivered face is **below** the corpus at base 1 and **above** it at bases 2 and 3. That
is the signature of edges quantised over 2–3 px — the very class the builder's own successful
exhibits describe. The pass's headline sentence, *"the delivered face sits in the corpus's own
register"*, is a base-1-only claim, and does not survive the two other bases the tool prints
on every invocation.

---

## 6. Guard spot-checks

### 6.1 `theCorpusPassesThisGuard` — **the criterion is fitted to one head in one image** (§11.0)

The guard validates the acceptance on image 3's **dark** duellist alone. Its javadoc names the
exclusion of images 4 and 5: *"their duellists' heads at matched scale sit against the clash
bloom and the whole-frame red wash respectively, so a blade-unmasked facet reading there
measures the light, not the face."*

I matched-scaled images 4 and 5 to the same 378-px figure height (0.5642 and 0.6311, figure
spans measured on the left-figure ink band) and read an 86×86 box centred on each duellist's
head — the same box class the guard uses, and verified visually to sit on the head and not on
the bloom (`corpus_boxes.png`).

| duellist | per 1000, unmasked | blade-masked | longest | vs the guard (≤8.0, ≤30) |
|---|---|---|---|---|
| ref3 dark — *the one the guard uses* | 6.76 | 6.76 | 22 | pass |
| **ref3 pale — the same image** | **8.92** | 8.92 | **32** | **fails both** |
| img4 dark | **10.55** | 10.55 | 17 | **fails density** |
| img4 pale | **8.92** | 8.65 | 25 | **fails density** |
| img5 dark | **9.87** | 9.87 | 20 | **fails density** |
| img5 pale | 7.30 | 7.30 | 16 | pass |

**Four of six family-B duellist heads fail the density ceiling; one fails the run ceiling.**
Blade-masking moves the numbers by at most 0.27, so the stated exclusion reason is refutable
in one command — and it cannot cover **ref3's own pale duellist**, which lives in the very
image the guard reads and was simply never measured. This is §11.0's amendment verbatim
("show it on *every* image in the family that depicts the situation being measured, and name
the ones you excluded and why"), committed by the pass that quotes §11.0 in that method's own
javadoc.

**And the ceiling is one-sided, which §11.0 forbids in terms.** The scale runs:

```
bareface control (no face at all)  4.60
delivered face                     5.68
corpus                             6.76 … 10.55
```

**The guard's best possible score is achieved by drawing nothing.** §11.0: *"a criterion of
floors alone rewards the defect it was written to catch… state the target as a band with both
edges, taken from the corpus's own spread."* Stated as a band — **density 6.8–10.6, longest
16–32** — the delivered face is **below** the band, i.e. under-marked. Confirmed on skin-only
boxes of the same class: hero `x405..452 y286..336` reads **2.86**/1000, foe
`x568..604 y285..335` reads **3.71**, against ref3's own faces at **9.00** (`x145..184 y182..231`)
and **7.50** (`x290..329 y190..239`). *The corpus carries two to three times the delivered
face's mark density.* Which is also what the picture says.

### 6.2 `bothDuellistsLookAtEachOtherThroughTheContact` — **vacuous; I broke it and it stayed green**

The assertion is `assertTrue(f.face().gazeX() > 0.25f)` — gaze *forward in the figure's own
frame*. It never references the opponent's position. It cannot distinguish "the schedule's
gaze anchor drives the eye" from "the eye always stares straight ahead".

**Adversarial instance actually run (§11.2b(f)).** I replaced the body of `Figure.gazeAt` with
`face.gazeToward(1f, 0f)` — the schedule's anchor discarded entirely, both figures hard-coded
to stare forward — and ran the suite:

```
WITH GAZE ANCHOR SABOTAGED: tests 427  skipped 0  failures 0  errors 0
```

**Nothing in the project witnesses the pass's headline finding.** (File restored; suite
re-run with `--rerun-tasks`: 427/0/0, `check-progress.mjs` OK.) The debt lists this guard in
neither its observed-red table (A–E) nor its "known-answer rather than red-observed" list —
it is the one assertion in the pass with no stated epistemic status, and it is the one
carrying the pass's most-advertised claim.

**Against the capture, not only the test.** `FaceRig.write` does
`pose.set("eye", 0.009f * gazeX, …)`. The skull is 0.15 world radius and delivers 66–68 px at
the parry's intimate framing, so **0.009 world units is ≤ 2 px of eye translation at the
closest shot the game has**, and ≤ 0.75 px at planning framing. On the pale duellist the eye is
a single ~2-px black dot; there is nothing in the delivered pixels from which a gaze could be
read. The plumbing is real and correctly wired — that part of §5.4's correction to
`Director.poseChannel` stands — but "the gaze now drives the eye" is not observable in the
capture and not guarded in the suite.

### 6.3 `theFaceDetailResolvesOnPushInAndOnlyThere` — guards a function, not a picture

It asserts only on `DuelScene.detailFade(headPx)`, a pure function; it never reads a pixel.
The fade curve is genuinely smooth (no 0.5-px step exceeds 0.06) and the ink marks do fade —
verified on pixels: `s3b-p1-wide` vs `-bareface` frame 0 differ by 1,849 px confined to
`x131..264 y516..566`, reproducing the debt exactly. But the *skin field* is not faded, and
that is where the wide-framing problem lives: at planning framing the pale duellist's head is
a white blob at **1.20× the sky** (live) against 1.24× (bareface). §4b.0's rule is satisfied
for the marks and unaddressed for the field.

### 6.4 The ones that hold

`theInheritedLatticeWouldFailThisGuard` is a genuinely good pattern — the red run checked in
as a permanent exhibit. Guards A–E were observed red with the messages quoted; I re-ran the
suite and confirm **427 tests, 0 skipped, 0 failures**, `tools/check-progress.mjs` OK, and the
graded frame `s3b-p1-parry-contact/frame_011.png`, `ref3-matched-378.png` and
`s4-p4-parry-contact/frame_011.png` are all git-tracked, so nothing fails open.

One scope note that costs nothing to state: `theFoeHeadRegionIsNoLongerTheLattice` reads a
**checked-in PNG**, so it witnesses the *delivered artefact*, not the *renderer*. A future
pass can regress the face and the guard stays green until someone re-captures and re-commits.

---

## 7. Determinism — **a claim that does not reproduce**

§2.5 characterises the live pair as *"run-to-run noise of **max channel delta 1** — invisible"*,
and §6 item 4 disposes of it as *"±1-LSB head-region non-determinism… Invisible… → nobody;
obey it."*

Measured over all 24 frames, `s3b-p1-parry-contact` vs `s3b-p1-parry-repro`:

- identical **2 of 24**, **11,445** px total — reproduces exactly.
- **max channel delta 78**, on frame 16, over **1,539 px** in `x366..467 y260..359` — the hero
  head box.
- frame 22: delta **69**, 1,765 px. Frames 8/10/15/20/21/18/13/14/12/11 carry deltas of
  21/20/16/17/17/15/12/11/12/14.
- **Only 6 of the 22 differing frames are at delta ≤ 2.**
- The amplified delta map on frame 16 puts the difference on **the specular, the profile rail
  and the mouth block** — the face's own marks, not a blur-resolve dither.
- The **graded frame itself** (11) is not reproducible: 288 px, delta 14, at the hero head.

The bareface pair reproduces as stated (17/24, 26,546 px, the pre-existing fleck class).

**Judgement: as characterised, this poisons future comparisons and must not be accepted.** A
78-level change over 1.5k pixels on the face's own marks is a *visible* run-to-run change and
a shimmer source on the one object the eye is designed to lock onto. The pass did the right
thing by §11.2b(g) — it shot the dynamic control that the static null cannot express — and
then summarised it by a number that is wrong by roughly 78×, which is how a real measurement
becomes a false reassurance. It is acceptable as permanent debt only once re-characterised
(max delta, pixel count, and the box) **and** bounded by an assertion, or bisected.

---

## 8. Ruling on the three corrections to inherited documents (§5)

### §5.1 — **half upheld, and it is the most valuable half; the other half is over-claimed**

**Upheld, independently and exactly.** On the audit's own tracked frame, box
`x610..690 y360..420`: **9.11 unmasked → 4.86 blade-masked**, and the five longest vertical
runs (48, 43, 43, 31, 28) all sit at **x631..637** — one narrow column band, which is the
blade. So the 44–50 px runs audit C2 counted *are the blade's own §5-licensed edge*, and
**roughly half the audit's headline is steel**. An audited number three systems inherited is
wrong in the way the debt says. This is the best thing in the pass.

**Over-claimed: "the audit box does not contain the face at all."**

That is true of *this* staging and I confirm it independently: contact-minus-bareface on frame
11 differs over exactly `x371..657 y260..359` — the delivered face touches **no pixel at or
below y = 360**, and the audit's box starts at y = 360.

But **the staging moved between the audit's capture and this one, and the debt does not say
so.** Same scene, same `-Pstart/-Pstep/-Pframes/-Pw/-Ph`, same harness:

| capture | code | first figure ink row | foe head lobe |
|---|---|---|---|
| `s4-p4-parry-contact/frame_011` | pre-3b, older commit | y = **320** | ≈ y335..390 |
| `s3b-p1-parry-inherited/frame_011` | pre-3b, this commit | y = **266** | ≈ y283..370 |

A ~54 px shift, present *before* 3b touched anything (`-inherited` and `-bareface` agree to
616 px). **On the audit's own frame the box clips the lower half of the pale head lobe.** The
audit drew its box on a head; the head then moved. Written as it is, §5.1 reads as an error in
the audit that is really a staging drift — and that matters, because the correction is being
handed to 3c as a redirection of where to look.

> **Corrected correction for the record:** *audit C2's box was on the lower half of the foe's
> head on the audit's own frame; on the current staging the same box sits on chest, sleeve and
> blade. Roughly half its unmasked reading is the blade, on both stagings. The residual is
> garment strips and hair-mass erosion, as §5.1 says.*

### §5.2 — **upheld as a diagnosis; only half delivered as a fix**

The bareface capture shows precisely what §5.2 describes: a flat, value-less pale lobe where
the pale duellist's skull is — garment colour on a skull, flattened by a garment compensation.
The code change (a scalp cap in its own skin merge group) is consistent with the claim. But
the *effect* survives: the head still reads **1.36×** the local sky at the parry framing and
**1.20×** at planning framing, against the corpus's 0.25–0.31×. The mechanism was found; the
picture was not fixed.

### §5.3 — **upheld exactly, with one convention still un-named**

The C2 face row reproduces to the digit on tracked pixels, and naming the blade-unmasked
convention is a genuine improvement — §11.3 applied to a convention rather than a rectangle,
which is the right generalisation.

One caveat the debt does not carry. Its §2.1 table compares **blade-masked** delivered rows
against an **unmasked** corpus row. On the delivered frame the mask covers **1,244 of 7,396 px
(16.8%)** of the derived head box, and the density denominator stays at the full area; on the
corpus box the mask is a no-op (6.76 either way, no cool-bright pixels). The two columns are
therefore not measured the same way. Under the corpus's own convention the delivered box reads
**10.41 per 1000, longest 24** (bareface 9.06/24). Quote masked-vs-masked and report the
masked area, or the comparison is convention-dependent by 1.8×.

### §5.4 — taken as read; `SimTimingTest` had already pinned the true bone count.

---

## 9. §4b compliance, line by line

| clause | verdict | evidence |
|---|---|---|
| **4b.1** face exempt from the ink dissolve | **holds** | no fleck breakup on the skin field in any of 24 frames; the beard is the one deliberate exception, correctly |
| **4b.2** cool grey-violet shadow, brown unreachable | **holds by construction** | `skin.deep = SKIN_DEEP`; measured hero face is neutral-to-cool (G−(R+B)/2 = −5.9) |
| **4b.2** palette *values* | **fails** | paper-ground values against a dusk sky; §4 above |
| **4b.3** contour as one continuous flowing line | **fails** | the profile is a chain of straight facets on both duellists (`hero_grid.png`, `foe_grid.png`); the foe's contour stair-steps |
| **4b.3** brow line, heavy ink stroke | hero partial; **foe absent** | no dark brow mark on the pale duellist on any frame |
| **4b.3** one eye | **fails on both, in opposite directions** | see below |
| **4b.3** jaw/neck wedge | holds (hero), weak (foe) | |
| **4b.3** gaze direction | **not observable** | ≤2 px of eye translation at the closest framing |
| **4b.4** *"if only two pixels survive they must be dark iris + specular"* | **fails, inverted** | hero: a white dot at `(441,302)` with **no dark iris**, sitting on the brow contour. foe: a 2-px black dot at `(595,313)` — at **nostril height, on the silhouette edge** — with no specular, no lash, no socket. Every family-B and family-D reference reads the eye as a **dark** mark |
| **4b.4** specular offset "toward the light", non-centred | **static** | it does not move across the 24-frame window |
| **4b.5** marks | debt, accepted | freckles ride the stain granulation; the scar is lip-toned |
| **4b.6** expression driven by combat state | **holds, and is the best-built thing here** | `Stance`-keyed condition table, no clips; first-order channels; settles staggered 0.30/0.40/0.44/0.56 s; guard A observed red |
| **4b.6** expression must not pop | holds numerically; **its printed form does** | the jaw channel prints as a hard black quad that detaches from the jaw into open sky (`x438..443 y325..333`, frame 17) |
| **4b.6** seeded variety, genuine not jitter | **unproven** | 400-seed *parameter* spread only; no capture shows two generated faces (debt item 8) |
| **4b.7** cel-shaded / flat-fill skin | **violated on both** | hero: flat polygon wedges whose facet boundaries change frame to frame. foe: one flat field, stair-stepped contour |
| **4b.7** both eyes in profile | not violated | |
| **4b.7** pure white sclera | not violated | |
| **4b.0** no shimmer at wide framing | **marks yes, field no** | ink marks fade correctly; the skin field does not, and the pale head is a white blob at 1.20× sky |
| **§3 / §10** nothing hard-edged except blades | **violated at the face** | mouth quad, skin facets, stair-stepped skin contour |

---

## 10. Pass-2 brief, ranked by impact

One pass of five is spent. In order.

1. **Re-derive the face's value from the ground.** Target **face-plane / local-sky ≈ 0.25–0.31**,
   the measured family-B band (§4 above). One number; it is the single change that would move
   §0 furthest, and it fixes the pale duellist at both framings. Write the fraction into
   STYLE.md §4b.2 the way §2.2 demands for its own floor, with the regions from §4 of this
   review. **This is the pass's headline job.**
2. **Invert the eye.** Author the **socket first**: a dark wedge 2–3 px across, set *behind*
   the brow ridge and *above* nostril height, heavier lash on the upper lid. The specular is
   the last thing added and the first thing to die. §4b.4's degradation order, not its inverse.
   On the pale duellist the socket is the whole difference between a face and a shard.
3. **Remove the hard edges the face introduced.** The mouth quad, the polygon facets in the
   skin field, the stair-stepped skin contour. §4b.1 exempts the face from the *dissolve*; it
   does not license aliasing. A 1-px feathered alpha rim on the skin field and on every ink
   mark — which `lightSpeck` already does correctly for the specular — costs nothing.
4. **Make the criterion a band, on the whole family.** `theCorpusPassesThisGuard` must read all
   six family-B duellist heads (numbers and boxes are in §6.1) and the acceptance must have
   both edges: **density 6.8–10.6, longest run 16–32**. As written the guard is maximised by
   the bareface control, and the delivered face is under the band.
5. **Close the run hole or narrow the claim.** Add my ±2-px/period-5 jitter exhibit to
   `FacetsTest` as the third checked-in adversarial success, and either extend the run
   detector across ±1 lateral steps (with its own null: a genuinely wandering ink edge must
   not be convicted) or state at every use site that the scope is *"no straight, un-jittered,
   near-single-pixel axis-aligned lattice"*. **And assert base 2 alongside base 1**, which
   `FacetsTest`'s javadoc already requires and no guard does.
6. **Make the gaze guard mean something.** Assert the eye's world aim against the *opponent's
   head position*, not the sign of a local-frame float — the sabotage in §6.2 must go red.
   Then either widen the gaze's visual range beyond 2 px at the closest shot, or stop claiming
   it drives the picture.
7. **Re-characterise and bound the head-region non-determinism.** Not "±1 LSB": max 78 over
   1,539 px on the face's own marks. Bisect it, or add an assertion that bounds the per-frame
   head-box delta, before any future pass tries a before/after at a head.
8. **Ship a face gallery capture.** Six generated foes at push-in scale in one frame. §4b.6's
   variety claim currently exists only in parameter space.

---

## 11. What I would accept as permanent debt

**Accept, if named at every use site:**

- The ear, the warm-black iris colour, discrete freckle placement, the vermillion scar (debt
  item 6). Genuinely below §4b.3's own hierarchy of what reads at this scale.
- Faces absent from Family A scenes, `SimScene`, `RigSwingScene` and `LaneScene` (item 7).
  §4b.0's decision names the duel; System 5's one-line adoption is a fair hand-off.
- The garment/hair residual in the audit's box (item 1) → 3c. Better diagnosed now than the
  audit had it.
- Per-frame coverage of the lattice guard (item 2) — one frame is sufficient once the
  criterion is a band with both edges.
- **Both** instrument holes — the builder's 3-px riser and my run-jitter — *provided* the
  narrowed scope is stated at every use site and the instrument never carries an acceptance
  alone. That is the honest state of the tool and it is fine to ship it that way.
- The generator's missing `hairMass` axis and the hairline (item 5) — a hair pass, not a face
  pass.

**Do not accept:**

- **The determinism item as characterised.** The number is wrong by ~78× and the disposition
  ("nobody; obey it") depends on it.
- **The single-image, one-sided criterion.** §11.0 rules it out by name, and the guard's best
  score belongs to the absent face.
- **The gaze guard.** A checked-in vacuous test is worse than no test — it certifies the
  defect and persuades the next reviewer to stop looking (§11.2b(f)).

---

## 12. Claims in `docs/system3b-debt.md` that do not reproduce

Collected, because this is the artefact the project values most.

1. **§2.5 / §6.4 — "max channel delta 1", "±1-LSB", "invisible".** Measured max **78** over
   1,539 px at the hero head on frame 16; 69 on frame 22; only 6 of 22 differing frames at
   ≤ 2. (§7 above.)
2. **§5.1 — "the audit box does not contain the face at all."** True of this staging; the
   figures moved ~54 px between the audit's capture and this pass's baseline, and on the
   audit's own frame the box clips the lower half of the head. The half about the blade is
   solid and valuable. (§8 above.)
3. **§2.1 — "the delivered face sits in the corpus's own register."** A base-1-only claim.
   At base 2 the delivered box reads 20.82 against the corpus's 17.58; at base 3, 36.91 against
   25.55. And the corpus's *base-1 register* is 6.8–10.6, so the delivered 5.68 sits **below**
   it, not in it. (§5.4, §6.1.)
4. **`FacetsTest` javadoc vs `FaceWindowTest` — a documented rule the same pass violates.**
   *"No guard in this project may quote the 1-px number alone"* — and all three guards do.
   (§5.3.)
5. **§1.6 / §4 — the gaze "proved by a headless test".** The test passes with the schedule's
   gaze anchor thrown away; I ran it. (§6.2.)
6. **§2.1's table mixes blade-masked delivered rows with an unmasked corpus row**, and the
   mask removes 16.8% of the delivered box while the denominator does not move. Under one
   convention the delivered box reads 10.41, not 5.68. (§8, §5.3.)

---

*Reviewed against STYLE.md §0, §1, §2.2, §3, §4b (all clauses), §10, §11.0, §11.2b(c)(f)(g),
§11.3, and reference images 3, 4, 5 plus `inspirations/faces/`. Every number above was taken
with a reader written for this review, not with `Facets.java`, and every one is printed with
its rectangle.*
