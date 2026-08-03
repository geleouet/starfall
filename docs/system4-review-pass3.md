# System 4 — independent review of pass 3

# FAIL

Pass 3 was set two charges and closed both in form: the corridor acceptance now runs on the
corpus, and the clash guard now goes red. It also refused half of its brief in writing, and
**the refusal was right** — I shot `LANE_SPREAD` and the pass-2 prescription is worse on
every axis. That is the best thing in the pass and I am correcting the record for it.

But the criterion that now decides everything **fails two of the three Family B reference
images**, one of them on the identical reading the capture is failed for. The corridor gain
the pass reports as 8→11 frames is **entirely photometric** — four of five bands are
bit-identical to pass 2 on all 24 frames. `§7.0.1` is still exactly `0.0000`, for the third
pass. And at matched scale the frame still cannot be cropped out of the corpus.

System 4 is at **pass 3 of 5**. At most two passes remain and §10 fail-on-sight rows are
still on screen.

---

## 0. The one-sentence test

> Could this frame be cropped out of one of the eight reference images and not look out of place?

**No**, for both graded captures.

### `s4-p3-parry-contact` — Family B (§1: "the primary template for the game screen")

Matched-scale sheet built first, per §11.0. Reference image 3 (`inspirations/image -
2026-08-02T101033.164.png`, 832×1088) downscaled by 0.603 so its left duellist's figure
height is 406 px — the hero's own height on frame 11 (span `y314..719`, from the largest
opened ink component). Reference cropped `x0..501 y150..599` after scaling; capture cropped
`x190..719 y290..739`.

Part count at that scale, per figure:

| | reference image 3 | `s4-p3` hero | `s4-p3` foe |
|---|---|---|---|
| readable parts | ~18 | 11 | 8 |

Reference, per figure: topknot, trailing hair strands, profile contour (brow-nose-lip-chin),
ear, neck, collar, shoulder, sleeve, forearm, two hands on a grip, tsuba, a long blade running
out of frame, obi, sageo, a second sheathed blade at the hip, pleated hakama, two legs, a bare
foot, ink smoke. Capture hero: hair mass, head blob, one cheek plane, shoulder, upper arm,
forearm, fist, tsuba bar, short blade, ochre sash, skirt, one foot. Capture foe: hair mass, a
pale torso patch, ochre sash, a black wing-shaped mass on its far side, a scabbard bar, a stub
of blade, a leg, a foot — **it still does not read as a body**, it reads as a spray of dark
marks around a pale blob.

Three things kill it before the part count, in the order the eye finds them:

1. **There is no tall X of two blades with a light in its fork.** This is the single most
   identifying feature of Family B and images 3, 4 and 5 all have it. Measured: in image 3 the
   crossed blades occupy one luminous cloud `x272..568 y317..456` at `L > 1.30 × row
   background` — a **0.486 of a figure height** diagonal, with steel visibly continuing to
   y≈130, i.e. about **0.50 figure heights of blade per duellist**. In the capture at frame 11
   the two cool-bright clouds (`b − r > −6`, `L > 212`, ≥120 px after a 2×2 opening) are 419 px
   in `x551..619 y306..400` (diagonal **0.286** of a 406 px figure) and 196 px in
   `x610..628 y339..399` (**0.154**). Their union spans **0.30** against the corpus's ≥0.49.
   One duellist has a short sword and the other has a stub, and they meet at hand height
   instead of crossing high.
2. **The stage is still not Family B.** Cream paper, a straw band, grass strokes. No dusk sky,
   no violet transition, no coral horizon. `Palette.SKY_ZENITH / SKY_MID / SKY_HORIZON /
   SKY_HORIZON_HOT` are still referenced by exactly one scene and it is `SmokeScene`. §2.2's
   warm/cool opposition is inverted across the whole frame — warm ground, warm blades, warm
   bloom. Unchanged since pass 1.
3. **§10 fail-on-sight rows are visible at 5×.** Crop `x500..720 y260..440` of frame 9 at 5×:
   the foe's pale head-and-shoulder mass is a set of **flat-shaded polygons with straight
   internal edges**, and the dark chips scattered around the contact are hard-edged
   quadrilaterals. "Hard-edged sprites / visible polygon silhouettes" and §3's opening line
   both apply on sight. Pass 3 records this as untouched; confirmed.

### `s4-p3-phrase-60hz` — Family C (planning framing)

Frame 000: the two figures are 136 px tall in a 720 px frame, pushed into the **bottom-left
corner**, with roughly 85% of the page empty (largest ink component `x105..172 y545..680`).
Family C (image 7, `101542.216`) puts figures at nearly full frame height, resolves faces,
and fills the frame with foliage, mist layers and saturated jewel motes. The capture has the
fog bands and the motes — those read — but it is a blank cream sheet with two specks on it,
not a fog-filled meadow. This is unchanged from the pass-2 review's reading and pass 3 did not
claim otherwise.

---

## 1. What is missing

**Not evocative enough**, with a specific structural cause: *the picture never draws the
event*. Every part is present somewhere — two figures, two blades, a warm light, ink smoke,
fog, motes — but the composition that makes Family B legible in a quarter of a second (two
upright ink silhouettes, a tall crossed X of pale steel, a star in the fork, against a hot
horizon) is never assembled. The bloom is three frames of a soft round smudge; the blades are
half length and meet at the hands; the sky is the wrong script.

Secondary: **technically broken** in the apparatus, in a new place — the parry capture is not
reproducible (§2.2), and the criterion that now gates the work fails its own corpus (§3).

---

## 2. Apparatus, before anything else (§11.2b)

### 2.1 In scope

- Every measurement below was taken with an **independently written reader** — NumPy/SciPy +
  PIL, 8-connected labelling, row-local background as median of the outer 70 columns, 3×3
  opening — not through `dev.starfall.analysis`. Where the checked-in tool is used it is
  named. Luminance uses the same Rec.709 weights `Frame.java` declares; that is a shared
  *definition*, not shared code.
- **Control first, per §11.2b(c).** `out/captures/s4-p3-null-static` vs
  `out/captures/rev-p2-null-static`: **0 of 16,588,800 pixels differ across 24 frames, max
  channel delta 0.** Both carry `harness=f0ad18994eec`, which also produced every `s4-p2-*`
  and every capture I shot for this review. Comparisons across pass 2, pass 3 and this review
  are in scope; nothing from `s4-p1-*` is quoted.
- Independent reader vs the checked-in `CorridorProfile`, on reference image 3, span
  `y283..955`, factor 0.85, strip 70: `torso` **10 px**, `sash` **62 px**, `skirt` **68 px**,
  `feet` **76 px** — identical integers. `head` 63 px (mine) vs 57 px (Java), a 10%
  disagreement; my left window edge lands at x283 and the tool's at x288. Pass 3 discloses
  this and it reproduces.
- `./gw test --rerun-tasks` — **BUILD SUCCESSFUL**, whole suite green, before and after every
  break in §6.

### 2.2 A new apparatus defect: the parry capture is not reproducible

Shot `duel-parry` twice at the same commit, same harness, same arguments
(`-Pframes=24 -Pstart=1.42 -Pstep=0.0167 -Pw=960 -Ph=720`) into `rev-p3-repro` and
`rev-p3-repro2`:

| pair | differing px of 16,588,800 | max channel delta | frames affected |
|---|---|---|---|
| `s4-p3-null-static` / `rev-p2-null-static` | **0** | 0 | none |
| `rev-p3-repro` / `rev-p3-repro2` | **13,545** | **122** | 11 (3,014 px), 14 (380), 19 (5,705), 20 (4,446) |
| `rev-p3-repro` / `s4-p3-parry-contact` | 1,408 | 74 | 9, 14 |

The differing pixels sit in boxes around the contact — frame 11 `x590..671 y364..463`, frame
19 `x443..617 y357..476` — i.e. in the clash flecks and smoke. **Frame 11 is the graded
frame.**

This does *not* move any number in this review: the bloom table and the corridor profile are
byte-for-byte identical between the two runs (§4.1, and frame 11's profile reads
`head 0.3559 torso 0.0000 sash 0.3759 skirt 0.3333` on both). The affected pixels are
mid-grey flecks, not the bright core and not the ink mask.

But it matters for the discipline. `s4-p3-null-static` is the control the project relies on to
declare the apparatus sound, and **it cannot witness this**: `rig-bindpose` is one static
figure with no clash and no particles. That is §11.2b(b) verbatim — *"capture the subject
where it cannot hide the artefact"* — and the null control the project has is one the artefact
hides in. Any future absolute pixel claim about an individual fleck or ember near a clash is
not reproducible, and nobody knew.

---

## 3. THE RULING ON PASS 3'S REFUSAL

Pass 3 built the per-band criterion its brief demanded and then refused both halves of the
prescription, in writing. I shot it.

### 3.1 The refusal is CORRECT, and my measurement is harsher than pass 3's

Four captures of `duel-parry`, identical window and harness, differing only in
`Director.LANE_SPREAD`. `rev-p3-spread120` / `rev-p3-spread135` / `s4-p3-parry-contact`
(1.55) / `rev-p3-spread175`. Measured with the project's own
`analyse corridor --profile`, which reports the reference's row on every run.

| `LANE_SPREAD` | frames passing every band (of 24) | frames that are one mass | min blade separation (`analyse blades`) |
|---|---|---|---|
| 1.20 | 10 | 3 | 0.0329 @ f9 |
| **1.35** (the brief's target) | **4** | **10** | **0.0903 @ f19** |
| 1.55 (shipped) | 11 | 2 | **0.0264 @ f9** |
| 1.75 | **21** | **0** | 0.1197 @ f8 |

**At 1.35 the two duellists merge into one ink mass on 10 of 24 frames** against the shipped
1.55's 2, only 4 frames pass every band against 11, and **the blades never come closer than
0.0903 of a figure height — the signature beat is destroyed.** Pass 2's brief would have been
a regression on every axis it named and on the one it was trying to protect. Pass 3 was right
to refuse it, and right to say so with a table rather than comply.

The pass-2 review's item 1 is hereby **withdrawn**. `LANE_SPREAD` should not be lowered
toward 1.35 on its own.

### 3.2 But the *reason* pass 3 gives is false, measured

> *"Lowering `LANE_SPREAD` scales every band by roughly the same factor."* — `system4-debt.md` §1.3

It does not. Band medians over the contact window (frames 6–23), as a multiple of reference
image 3's own reading, all through `analyse corridor --profile`, window between the two body
centroids, span auto-detected per frame and printed with every number:

| `LANE_SPREAD` | head | torso | sash | skirt | feet |
|---|---|---|---|---|---|
| 1.20 | 2.62× | **3.67×** | **0.17×** | 1.25× | 1.44× |
| 1.35 | 1.79× | **0.00×** | 2.74× | 2.28× | 1.92× |
| 1.55 | 1.95× | 0.92× | 3.35× | 3.21× | 1.57× |
| 1.75 | 2.04× | 3.58× | 3.28× | 4.19× | 1.51× |

`torso` runs 3.67 → 0.00 → 0.92 → 3.58 across a 46% change in one constant. `sash` moves
**16×** between 1.20 and 1.35. Nothing here is a scalar and nothing is monotonic.

The cause is in `Director` and is worth naming because it will bite pass 4 too:
`stretch(Anchor)` scales `CONTACT`, `CROSSING` and `TILE` anchors by `× LANE_SPREAD` while
body anchors keep their offsets from a moved tile. So the constant moves the **targets** as
well as the **bodies**, the arms saturate against them (pass 3's own §6: both arms are
0.6–0.75 world units from targets on a 0.56-unit arm), and the pose changes. `LANE_SPREAD` is
not a spacing parameter; it is a staging parameter, and no band responds to it linearly.

So: **right answer, wrong argument.** The correct statement of the refusal is not "one factor
cannot fix two bands", it is "1.35 collapses the pinch and merges the bodies — here is the
capture".

### 3.3 And the counter-prescription is half right, decomposed

Pass 3's counter-prescription is *widen the lower garment and shorten the reach*. I decomposed
the skirt-band corridor into its two causes — how far apart the bodies are, and how wide they
are — measured per body component (8-connected, ≥10% of frame ink), band `0.62..0.89` of the
figure span, reference `y700..881` of `y283..955`, capture `y566..674` of `y314..719`:

| | body centre separation | left body width | right body width | resulting gap |
|---|---|---|---|---|
| reference image 3 | 0.649 | 0.591 | 0.495 | 0.101 |
| `s4-p3` frame 11 | **0.793** | 0.581 | **0.300** | 0.327 |

(all as fractions of that image's own figure height; left body `x0..398` / `x214..450`, right
body `x469..802` / `x593..715`.)

Of the capture's 0.226 of excess skirt corridor, **0.144 (64%) is separation** — the bodies
stand 22% further apart at the base than the corpus — and **0.082 (36%) is narrower bodies,
all of it the foe**. The hero's skirt is 0.581 against the corpus's 0.591; it is already the
right width. The foe's is 0.300 against 0.495 — **61% of the corpus**.

And the separation is not uniform down the figure, which is the finding neither pass made.
Body-to-body gap per band, same instrument:

| band | reference image 3 | `s4-p3` f11 | ratio |
|---|---|---|---|
| torso | 0.0149 | **0.0074** | 0.50× |
| sash | 0.163 | 0.365 | 2.2× |
| skirt | 0.104 | 0.350 | 3.4× |
| feet | 0.131 | 0.495 | 3.8× |

**The corpus's duellists stand parallel; the capture's lean into each other.** The reference
holds a body separation of 0.58–0.66 figure heights at every band. The capture runs 0.63 at
the head, 0.70 at the torso, 0.78 at the sash, 0.79 at the skirt. The figures are pitched
forward from the waist, so the bases splay and the tops close — and moving the whole body
(`LANE_SPREAD`) cannot fix a *shape*.

So pass 3's headline claim — *3.2× too wide at the skirt, zero at the pinch, and lowering the
spread is the wrong move* — **reproduces and stands**. Its "widen the lower garment" is worth
about a third of the skirt error and applies only to the foe. Its "shorten the reach" is the
right half and is the enabler for everything else.

### 3.4 One more thing the criterion is blaming on the staging

At frame 11 the `torso` band reads 0.0000, but the two *bodies* are 3 px apart (0.0074 of a
figure height) where the corpus keeps 6 px at the same scale. The columns are closed by a
**567-px ink component at `x596..628 y382..419` — the clash's own shed flecks** — plus four
smaller fleck blobs. The corridor at the pinch is being closed by the effect, not by the
staging, and no staging change will open it while the flecks are drawn there.

---

## 4. THE CRITERION AUDIT — it is a real improvement and it is under-validated in three ways

### 4.1 (a) The floors derive from the sweep, not from the capture — but the corpus is three images and only one was run

The floors are image 3's readings rounded down: `head` 0.080 vs 0.0847–0.0936, `torso` 0.014
vs 0.0149, `sash` 0.085 vs 0.0921, `skirt` 0.095 vs 0.1010, `feet` 0.065 vs 0.1129. They are
not fitted to the capture — the capture's `feet` reads 0.155–0.183 and would clear any floor
up to 0.15, so the slack at `feet` is not there to let the capture through. The sweep is real
and `CorridorProfileTest.theReferenceProfileIsStableUnderItsOwnNuisanceParameters` asserts it
rather than describing it. Credit where due.

**But STYLE.md §1 makes Family B images 3, 4 *and* 5, and neither of the other two has ever
been run.** `analyse corridor --profile` takes `--reference` and `--reference-span`, so this
is a two-minute check. Run with the project's own tool, span `0,255,832,676`:

**Reference image 4** (`101128.842`) — same composition, blades crossed, star bloom, dark
duellist against pale:

```
  head   x210..588 y255..376  109 px = 0.1612  (floor 0.080)  pass
  torso  x210..588 y377..531    8 px = 0.0118  (floor 0.014)  MISS
  sash   x210..588 y532..673   66 px = 0.0976  (floor 0.085)  pass
  skirt  x210..588 y674..856   58 px = 0.0858  (floor 0.095)  MISS
  feet   x210..588 y857..930   30 px = 0.0444  (floor 0.065)  MISS
  reference passes its own floors: NO
```

**Reference image 5** (`101232.595`) — same composition again:

```
  head   x145..422 y255..376   54 px = 0.0799  (floor 0.080)  MISS
  torso  x145..422 y377..531    0 px = 0.0000  (floor 0.014)  MISS
  sash   x145..422 y532..673   33 px = 0.0488  (floor 0.085)  MISS
  skirt  x145..422 y674..856   49 px = 0.0725  (floor 0.095)  MISS
  feet   x145..422 y857..930   55 px = 0.0814  (floor 0.065)  pass
  reference passes its own floors: NO
```

**Reference image 5's `torso` corridor is 0.0000 — the identical reading the capture is
failed for on eight frames.** On my independently written reader image 5 does not even resolve
into two bodies: it is **one connected ink component in all 264 combinations** of eight figure
spans × eleven bottoms × three ink factors I swept, which `Profile.pass()` hard-fails as
"no corridor exists". Image 4 misses `torso` in every one of the same 264 combinations (best
reading 0.0129 against the 0.014 floor).

This is §11.0's own failure, reproduced one level up. The old scalar was refuted because the
corpus fails it by 4×; the replacement is calibrated on **one** painting and **two thirds of
the same family fail it**, one of them on the exact band and the exact number the capture is
being failed on. The tool's headline — *"the criterion, run on the corpus first"* — is true of
a hard-coded `CORPUS_DUEL` file, not of the corpus.

### 4.2 The criterion is one-sided, and it rewards the defect pass 3 itself identifies

`Reading.pass()` is `fraction >= floor`. There is no ceiling. So a capture cannot fail for
being **too wide** — which is precisely what pass 3 measured as the real error (3.2× at the
skirt) and precisely what the criterion is now being used to steer.

Demonstrated, not argued: `LANE_SPREAD = 1.75` scores **21 of 24 frames and 0 merged frames**
— nearly twice the shipped 11 — while pushing `skirt` to **4.19×** the corpus and taking the
minimum blade separation from 0.0264 to **0.1197**, i.e. the blades never meet at all. **The
criterion that now decides everything prefers a staging in which the signature beat does not
happen.** A reviewer or a pass optimising against it alone will walk straight into that.

A second one-sidedness: acceptance requires **24 of 24 frames** to pass every band. Nobody has
established what fraction of frames a corpus-like *sequence* would pass; the corpus is three
stills, and two of them fail as stills.

### 4.3 (b) Shared code path — defensible, but the cross-reader claim does not extend to the capture

The reference and the capture go through the same `CorridorProfile.measure`. On its own that is
what §11.0 asks for, and §11.2b(d) actively prefers differences to absolutes, so a common-mode
reader error largely cancels in a *capture ≥ reference-derived floor* comparison. I would not
call the shared path a §11.2b(c) violation.

What *is* a gap: pass 3's cross-reader check was run only on the **reference**, where the span
is given by hand. On the **capture** the span is auto-detected, and my reader and the tool
disagree materially:

| frame 11 | tool | independent reader |
|---|---|---|
| figure span | `y314..719`, h 406 | `y314..712`, h 399 |
| `feet` | 0.1773 | **0.2957** |
| `skirt` | 0.3276 | 0.3333 |
| `sash` | 0.3645 | 0.3759 |
| `torso` | 0.0000 | 0.0000 |

The span bottom is the hero's garment dissolving into the ground smear, so it lands on whether
a handful of near-threshold pixels survive the opening. Seven pixels of span move `feet` by
**67% of itself**. The tool's own javadoc warns about exactly this on the reference and hands
it a span; on the capture nothing does. `torso` and `skirt` — the load-bearing bands — are
reader-stable; `feet` and `head` are not, and `head` decides three of the thirteen failures on
a floor that sits inside the readers' own 10% disagreement.

Also worth recording: the capture's `feet` band is not the same anatomy as the reference's.
The reference span ends at the feet (`y955`) with smoke below it excluded; the capture span
ends where the ink dissolve stops. The 1.6×/2.6× `feet` comparison is between different
things.

### 4.4 (c) The 8→11 frame gain is not "partly" photometric. It is *entirely* photometric.

`system4-debt.md` §1.4 says *"Part of the 8→11 improvement is not geometry."* Measured, none
of it is.

I ran `analyse corridor --profile` on `s4-p2-parry-contact` and `s4-p3-parry-contact` and
differenced every band on every frame:

| band | frames whose reading changed between pass 2 and pass 3 | largest change |
|---|---|---|
| **skirt** | **0 of 24** | 0.0000 |
| **feet** | **0 of 24** | 0.0000 |
| head | 2 of 24 | 0.0025 |
| sash | 1 of 24 | 0.0023 |
| **torso** | **15 of 24** | 0.0246 |

Three frames flipped MISS → PASS — **15, 18 and 21 — and all three flipped on `torso` alone**
(0.0000 → 0.0246, 0.0000 → 0.0172, 0.0025 → 0.0148). The clear runs they gained start at
x615, x611 and x615, which is **inside the pale duellist's own silhouette** (the foe's body
spans `x593..810` at that band).

And the pixels in those runs did not move, they got brighter:

| frame, run | pass 2 min L / ink px | pass 3 min L / ink px |
|---|---|---|
| 15, `x615..624 y387..479` | 153.4 / 98 | 168.2 / 5 |
| 18, `x611..617 y387..479` | 111.0 / 134 | 135.6 / 120 |
| 21, `x615..620 y387..479` | 73.7 / 41 | 155.5 / 21 |

(threshold ≈180.8 over those rows; medians identical to within a level.)

**The corridor did not improve as geometry at all. The staging is bit-identical to pass 2 on
`skirt` and `feet` on all 24 frames.** What improved is that four to seven columns of the pale
duellist's torso stopped registering as ink. A threshold-based corridor got wider because a
body got paler — which is the mistake §11.2b keeps recording, committed by the pass that named
it and then reported the number anyway as "8 / 24 → 11 / 24".

---

## 5. The bloom

### 5.1 The delivered placement claim reproduces exactly

Pass-2's instrument re-implemented independently: warm-bright mask `L ≥ 240 and r − b ≥ 8`,
whole frame, centroid; minimum distance to each of the two largest cool-bright blade clouds
(`b − r > −6`, `L > 212`, ≥120 px after a 2×2 opening); figure height from the largest opened
ink component.

| frame | bloom px | centroid | to nearer blade | to second blade |
|---|---|---|---|---|
| 8 | — | no warm-bright pixels | | |
| 9 | 152 | (556,354) | **2.5 px = 0.006** | 62.4 px = **0.161** |
| 10 | 276 | (587,331) | **2.6 px = 0.007** | 35.7 px = 0.091 |
| 11 | 111 | (608,326) | **3.1 px = 0.008** | 12.9 px = 0.033 |
| 12 | 49 | (630,316) | 7.3 px = 0.019 | 57.1 px = 0.144 |
| 13–21 | 9–129 | — | 8–36 px | 0.16–0.60 |

Every number in `system4-debt.md` §3.3 reproduces to the pixel, on both re-captures. The core
is 2.5–3.1 px from the nearer blade on frames 9/10/11 against pass 2's 10.8–44.8. **That claim
is verified.** Frames 12+ are the embers, correctly labelled as such.

### 5.2 As a picture, it is not a star, and frame 9 is not a clash

Read at 3× (`x480..720 y270..450`, frames 8–12) and at 5× (`x500..720 y260..440`, frame 9):

- **Frame 9**: a soft warm-white smudge sitting *on the hero's blade, just above the tsuba*.
  The second blade is 0.161 of a figure height away and has not arrived. As a picture this is a
  highlight on one sword, not two swords meeting. The world-space guard is satisfied because
  `drawnCrossing` is the midpoint of the closest approach between two *geometric* segments,
  and the drawn steel tapers and stops short of the segment end. Pass 3 names this and it is
  visible; it is the reason the placement half of the guard must not be quoted as evidence.
- **Frame 10** is the largest and best: a soft round glow at the crossing with **one**
  horizontal streak.
- **There are no rays.** §5 asks for "4–6 long soft rays". I count at most two directions on
  the best frame, and the streak is shorter than the core is wide. At threshold 240 the core
  spans 31 px = **0.079** of a figure height; reference image 3's star core at matched scale
  (downscaled to a 392 px figure) spans 38 px = 0.097 at threshold 200 — comparable *core*,
  and the corpus then has six-plus rays running several times that far, which the capture has
  none of.
- **Embers: 3–5** strongly-warm blobs ≥4 px on frames 9/11/13/15/17 (5, 4, 3, 4, 4) against
  §5's 8–20. Some of those are Family C sky motes.

### 5.3 Is `CLASH_SPAN = 0.27` — three frames — too short? Yes, and pass 3's diagnosis is right

Three frames is 0.05 s. Nothing in a 60 Hz picture reads as a "soft star bloom" in 0.05 s;
§7.3 calls the beat a poetic one and §7.1 makes contact a *span*. The GUARD beat's contact
span is 0.168 s = 10 frames and §7.1 wants the blades to meet at its start and part at its
end, so the target is about **10 frames — 3.3× the delivered**.

Pass 3's causal claim is correct and I verified the mechanism: the light is bounded by the
meeting, the meeting is 0.066 s of a 0.168 s span (39%), and the guard genuinely enforces it —
see §6.1. Lengthening `CLASH_SPAN` without lengthening the bind would print the light over
frames where the blades are apart, which is the defect the guard exists to catch. **The bloom's
duration is not a separate item; it is item 4 wearing a different hat**, and pass 3 is right
to say so.

---

## 6. Guard spot-checks — five broken, five red, five with the message quoted

§11.2b(f) is the sharpest rule in the document and a ceremonial application of it would be
worse than none. I broke five of the six guards `system4-debt.md` §8 tabulates and read the
messages out of the JUnit report.

| # | guard | how I broke it | printed |
|---|---|---|---|
| A | `RehearsalTest.everyClashIsDrawnWhereTwoBladesActuallyAre` (gap half) | `Scheduler.CLASH_SPAN` 0.27 → 0.90 | *"PARRY: the clash that starts at t=1.568 is still drawn at t=1.6316 with the two blades 6.3% of a figure height apart. A bloom is an assertion that they are meeting; on this frame it is false."* (line 101) |
| B | same guard (placement half) | `drawnCrossing.y += 0.25f` after `closestPoints` | *"PARRY: at t=1.5833 the drawn clash mark (1.005,1.984) is 7.0% from the hero's blade and 13.8% from the foe's. It is sitting on a grip."* |
| C | `RehearsalTest.theHeldBreathIsAtSpecOnEveryScene` | `Timing.HELD_BREATH_SECONDS` 0.25 → 0.12 | *"PHRASE: the held breath runs 0.125 s per ramp over 5 ramp(s). STYLE.md 7.3 asks for ~0.25 s and Timing.HELD_BREATH_SECONDS is 0.12."* |
| D | `CorridorProfileTest.referenceImageThreePassesEveryFloor…` and `…IsStableUnderItsOwnNuisanceParameters` | `torso` floor 0.014 → 0.06 | *"STYLE.md 11.0: the corpus must pass the criterion the corpus set. Reference image 3's torso band reads 0.014858… against a floor of 0.06."* and *"reference band torso at factor 0.75 span x0..831 y283..955 reads 0.014858… against floor 0.06"* |
| E | `DuellistValueTest.theTwoDuellistsAreTellableApartInDeliveredPixels` | capture path → `s4-p2-parry-contact/frame_011.png` | *"Family B is a dark duellist against a pale one and this capture is a dark duellist against a slightly less dark one. The corpus reads 3.27x; this reads capture: dark 0.269 …, ratio 1.49x"* |

All five went red, all five printed the message they were written to print, and the suite was
restored green afterwards. **§11.2b(f) was applied honestly by pass 3.** This is the first pass
in System 4 whose guards I could not fault.

### 6.1 Which half is load-bearing: confirmed, and sharper than pass 3 states

Pass 3 says the placement assertions are "close to tautological". Reading
`Director.rememberCrossing`, they are **strictly entailed**: `drawnCrossing` is the midpoint of
the closest approach between the two blade segments, so `markToBlade` is exactly half the gap
and `markIsBetweenTheBlades` is true by construction. Given `bladeGapFraction() <= 0.02`, both
placement assertions are theorems. They cannot fail independently.

They are not worthless — spot-check B shows they fire when `lastCrossing` and the renderer
disagree, which is precisely the bug pass 3 says it shipped for twenty minutes and found *by
looking at pixels*. So they are a live regression trap for a **desynchronisation**, and they
are **no evidence at all about placement**. Pass 3's caveat is correct and should be kept
verbatim in the next debt document.

The load-bearing assertion is `bladeGapFraction() <= MET` on **every drawn frame**, and
spot-check A proves it binds `CLASH_SPAN`: raising the span to 0.90 fails it at t=1.6316 with
the blades 6.3% apart. That is a real guard and it is doing real work.

### 6.2 One quoted message that does not reproduce

`system4-debt.md` §8 quotes guard E as printing *"…ratio 1.54x"*. It prints **"ratio 1.49x"**,
with `dark 0.269` and `pale 0.402`, against the debt §2 table's `0.260 / 0.399 / 1.54×` through
the same rectangles and the same frame. The difference is a median convention (upper median vs
mid-average). Trivial in size, but it is a quoted transcript of a guard's output that is not
the guard's output, in the section whose entire purpose is that guards were observed.

---

## 7. The contrast finding — REPRODUCES, and its conclusion does not

### 7.1 The measurement, verified, and at matched scale

Median ink luminance over the frame's own row-local ground, independently implemented:

| | dark duellist | pale duellist | ratio |
|---|---|---|---|
| reference image 3, `x190..300 / x540..650, y400..540` | **0.133** | **0.436** | **3.27×** |
| **the same, downscaled to a 406 px figure** (LANCZOS ×0.603, rects scaled, strip scaled) | 0.133 | 0.437 | **3.28×** |
| capture pass 2 f11, `x300..470 y300..420 / x600..760 y340..460` | 0.260 | 0.400 | 1.54× |
| capture pass 3 f11, same rects | 0.245 | **0.521** | **2.12×** |
| capture pass 3 f11 skirt, `x330..470 / x620..760, y500..620` | 0.162 | 0.264 | 1.63× |

**The corpus's 3.27× survives matched-scale downscaling to the capture's own figure height
(3.28×), so it is not a resolution artefact.** §11.0's first act, applied to a value statistic.

Pass 3's two claims both hold:

- **The pale figure is already pale enough.** 0.521 against the corpus's 0.436 — it is
  *paler* than the corpus by 20%.
- **The whole remaining gap is the dark duellist**: 0.245 against 0.133, 1.8× too bright. Its
  torso median is **L = 53.0**, median RGB **(45, 54, 67)**, against `INK_INDIGO` `#2C3A4F` =
  (44, 58, 79), luma 56.5. The garment above the sash is printing its base tone. The rect's
  minimum is 25.7, exactly `INK_BLACK`'s luma, so §2.2's floor is respected and reached
  somewhere.

This reassigns the work and is the second-best finding in the pass.

### 7.2 But "this is a System 1 mesh change" is wrong, and one line disproves it

`system4-debt.md` §2: *"Closing this wants the mesh's authored wetness above the sash raised on
the dark figure, which is a System 1 change. That is the concrete next step and it is not a
guess."*

The pooling argument is right — `ink_skin.frag`'s value terms are gated on `wetness`, the
mantle is authored 0.03–0.20 wet, and `DARK_SASH_POOL = 1.70` multiplying a near-zero pool
bought three luminance levels. But pooling is not the only lever on value. **The base colour
is.** `Figure.dark` sets `f.cloth.base = Palette.INK_INDIGO`.

I changed that one line to `Palette.INK_BLACK` and re-shot the capture
(`out/captures/rev-p3-darkbase`, same window, same harness):

| | torso dark | torso pale | ratio | dark IQR / ground |
|---|---|---|---|---|
| `s4-p3-parry-contact` f11 | 0.245 | 0.521 | 2.12× | 0.277 |
| `rev-p3-darkbase` f11 | **0.202** | 0.522 | **2.58×** | 0.284 |

**One line in the material layer moves the ratio 2.12 → 2.58 — four times what
`DARK_SASH_POOL` bought — in the layer pass 3 declared exhausted**, and it does not flatten
the figure (interquartile spread unchanged, 0.277 → 0.284). Read at 1:1 it is a visible
improvement: the left duellist finally reads as a near-black silhouette and it is instantly
obvious which one is the pale one.

The pass's counter-argument — that reaching the corpus value "needs `dark` to saturate, which
would print the flat fill §3b.5's first row fails on sight" — is also refutable on the corpus's
own pixels. Reference image 3's dark duellist's torso ink distribution, `x190..300 y400..540`:
p05 0.113, p25 0.123, **median 0.128**, p75 0.185, p95 0.524 — an **interquartile spread of
0.063**, i.e. a tight near-black mass with a thin bright tail for the collar and face. The
capture's is **0.277, 4.4× wider**. §1 says Family B's bodies are "near-black ink silhouettes
with just enough interior modelling to find the face and hands"; at this framing the corpus
*is* nearly flat there and the capture is the over-modelled one. Darkening the dark figure
moves it **toward** the corpus's spread, not away.

The mesh-wetness route may still be the right long-term fix. It is not the *next* step and it
is not the only one, and the debt states it as though it were both.

---

## 8. §7.0 — the positive test, third pass unaddressed

Measured headless in world units against `Stage.FIGURE_HEIGHT = 1.70`, driving the same
`Rehearsal` the capture's director runs, over the whole score of every `Duel.Kind`. (Temporary
instrument, deleted after use; it dumps `hero().hips()/head()/shoulder()/elbow()/hand()/
bladeTip()` per frame to CSV.)

Motion of each joint **relative to the figure's own stand position**, as a fraction of figure
height:

| joint | PARRY relX | PHRASE relX | KNOCKBACK relX | y range (parry) | rel path (parry) |
|---|---|---|---|---|---|
| **hips** | **0.0000** | **0.0000** | **0.0000** | 0.0118 | 0.034 |
| shoulder | 0.0491 | 0.0624 | 0.0560 | 0.0423 | 0.181 |
| head | 0.0767 | 0.0836 | 0.0834 | 0.0266 | 0.204 |
| elbow | 0.3894 | 0.3787 | 0.1435 | 0.3560 | 2.165 |
| hand | 0.6383 | 0.6635 | 0.2415 | 0.6103 | 3.096 |
| blade tip | 1.3738 | 1.3864 | 0.6573 | 1.4916 | 7.171 |

**The pelvis's horizontal motion relative to its own stance is exactly 0.0000 figure heights in
all three scenes**, including the knockback, whose `standX` does travel 0.69 figure heights.
Hip path / hand path: **1.1% (parry), 0.6% (phrase)**. System 2 was failed at 1.5%. The
pass-2 review's measurement reproduces exactly; pass 3 did not touch it and says so.

**Nothing arrives at the same time — still violated.** Peak speeds in the strike window
(t 1.30–1.75, 60 Hz rehearsal):

| joint | peaks at | speed |
|---|---|---|
| hips | **1.5667** | 0.54 |
| shoulder | **1.5667** | 2.86 |
| elbow | **1.4000** | 6.03 |
| hand | **1.4000** | 9.75 |
| blade tip | **1.4000** | 18.20 |

Two clusters of exact simultaneity on single samples. §10's last row is a fail-on-sight row.

**Verdict on §7.0: merely correct, not poetic.** The chain has no origin; it starts at the
spine, and the hip is a weld.

---

## 9. Claims in `docs/system4-debt.md` that do not reproduce

1. **§1.4, the corridor gain:** *"Part of the 8→11 improvement is **not** geometry."* — **none
   of it is.** `skirt` and `feet` are identical on all 24 frames; `head` and `sash` move on
   three frames by ≤0.0025; all three flipped frames flipped on `torso` alone, at columns
   inside the pale duellist, on pixels that got brighter rather than moved (§4.4).
2. **§1.3, the mechanism of the refusal:** *"Lowering `LANE_SPREAD` scales every band by
   roughly the same factor."* — it does not. `torso` runs 3.67× → 0.00× → 0.92× → 3.58× and
   `sash` moves 16× over the sweep 1.20 / 1.35 / 1.55 / 1.75 (§3.2). The refusal's *conclusion*
   is right and the argument for it is not.
3. **§2, the contrast:** *"Closing this wants the mesh's authored wetness above the sash raised
   on the dark figure, which is a System 1 change. That is the concrete next step."* — one line
   in `Figure.dark` (`base = INK_BLACK`) moves the ratio 2.12 → 2.58, more than four times what
   `DARK_SASH_POOL` bought, with no change in interior spread (§7.2). And the "it would print a
   flat fill" counter-argument is refuted by the corpus, whose dark torso has an interquartile
   spread of 0.063 against the capture's 0.277.
4. **§1.1, the criterion:** *"The command measures the reference first, every time, and returns
   non-zero if the corpus does not pass its own floors. STYLE.md §11.0, put in the tool."* —
   it measures **one image** of a three-image family. Reference image 4 fails `torso`, `skirt`
   and `feet`; reference image 5 fails four of five bands with `torso` at **0.0000**, and on an
   independent reader is one connected mass in all 264 span/factor combinations tried (§4.1).
5. **§8, guard E's transcript:** the quoted message reads *"ratio 1.54x"*; the guard prints
   *"ratio 1.49x"* with `dark 0.269` (§6.2).
6. **New, and nobody's claim — an apparatus fact:** `duel-parry` is **not reproducible**.
   Two runs at the same commit and harness differ by 13,545 px, max channel delta 122, on
   frames 11, 14, 19 and 20 — including the graded frame. The `rig-bindpose` null control
   cannot see it (§2.2).

Items 2 and 3 mean two of pass 3's arguments are wrong while one of its conclusions is right.
Items 1 and 4 mean the two headline deliverables are weaker than reported: the corridor gain is
photometric and the criterion is calibrated on a third of its corpus.

---

## 10. Brief for pass 4, ranked ruthlessly

At most two passes remain. The smallest set that could make §0 answer **yes** is items 1–3.
Everything from item 6 down I would let ship as recorded debt.

**1. Draw the Family B stage.** `Palette.SKY_ZENITH / SKY_MID / SKY_HORIZON / SKY_HORIZON_HOT`
exist, are calibrated from the corpus, and are referenced by one scene and it is `SmokeScene`.
Until a duel is fought against a dusk sky, §0 cannot be answered yes for a Family B frame
however good the figures get, because §2.2's warm/cool opposition is inverted across the whole
image — warm ground, warm blades, warm bloom. This has been deferred as "item 7", then "item
6", for two passes; it is the single largest change in what the frame *looks like* per unit of
work remaining, and it is a background, not a rig. **Do it first, before anything else, and
re-shoot everything against it.**

**2. Give the duel the tall X.** Two long slivers of pale steel crossing well above the fists,
with the light in the fork. Measured target: about **0.50 of a figure height of blade per
duellist** and a crossed figure spanning ≥0.49; delivered is 0.286 / 0.154 and a union of 0.30
(§0). This one change discharges four separate open items at once:
   - it moves the crossing out of the `torso` band, which is what the corridor is failing on;
   - it lets the bind be a span rather than an instant (§7.1's meet-at-40 / part-at-55) —
     pass 3 already located the levers and recorded them at `Director.FIST_DROP`,
     `Stage.Y_MIDDLE` (1.00 puts the hands at 0.77, below the hips) and `Figure.BLADE_CROSSING`,
     and recorded that both arms are 0.6–0.75 world units from targets on a 0.56-unit arm;
   - a longer bind is the only thing that lets `CLASH_SPAN` grow from 3 frames toward the
     10-frame contact span without the guard correctly failing;
   - and shortening the reach is the enabler for closing the base separation in item 4.

   **Do not chase this by widening the lane.** 1.75 scores 21/24 on the corridor and takes the
   blades to 0.1197 apart.

**3. Make the dark duellist dark: one line, already measured.** `Figure.dark`'s
`f.cloth.base = Palette.INK_INDIGO` → `Palette.INK_BLACK` takes the delivered torso ratio from
2.12× to 2.58× against the corpus's 3.27×, with the interior spread unchanged
(`out/captures/rev-p3-darkbase`, frame 11). Then re-tune `DARK_SASH_POOL` and `PALE_SASH_LIFT`
*on top of* the new base rather than instead of it, and raise `DuellistValueTest`'s
`DELIVERED_FLOOR` from 1.95 as a ratchet. The mesh-wetness route stays open; it is not the
first move and pass 3's debt should stop saying it is.

**4. Fix the criterion before using it again, in three specific ways.**
   - **Run it on images 4 and 5.** `--reference` already exists. Either the floors move to what
     all three Family B images pass — `torso` ≤ 0.0115, `skirt` ≤ 0.082, `feet` ≤ 0.028, and
     image 5 needs the merge rule reconsidered because a corpus painting reads as one mass —
     or the criterion is honestly scoped to "image 3's staging" and stops being called the
     corpus. Put the assertion for all three in `CorridorProfileTest`, not the reviewer's head.
   - **Give it a ceiling.** It is a floors-only test and the capture's real error is being
     3.4× **too wide** at the skirt and 3.8× at the feet. As built it scores `LANE_SPREAD =
     1.75` — blades never meeting, skirt 4.19× the corpus — as nearly twice as good as the
     shipped staging. State the acceptance as a **band around the reference profile**, not a
     floor.
   - **Give the capture an explicit span**, as the reference already gets. Seven pixels of
     auto-detected span move `feet` by 67% of itself between two readers.
   - And when the corridor is next quoted as improved, **quote it at a fixed value threshold
     too**, so the next pass cannot buy corridor by lightening a figure.

**5. Close the base separation, correctly.** The bodies stand 0.793 figure heights apart at the
skirt against the corpus's 0.649, and the separation grows from 0.63 at the head to 0.79 at the
skirt where the corpus holds 0.58–0.66 at every band: **the figures lean into each other**.
That is a stance/pitch problem, not a lane-spacing one, which is why `LANE_SPREAD` cannot fix
it. Stand them up. Separately, the **foe's** lower garment is 0.300 of a figure height wide
against the corpus's 0.495 and the hero's 0.581 — widen the foe's only.

**6. §10's fail-on-sight rows, if there is budget.** The flat-shaded polygon facets on the
foe's head and shoulder, the hard-edged quadrilateral flecks, and the straight quad boundary
are all visible at 5× on frame 9 (`x500..720 y260..440`). §3's first line and §10's
polygon-silhouette row both apply on sight, and a reviewer is required to fail on sight of
them. This is the one item below the line I would argue about.

### What I would let ship as recorded debt

Say so explicitly in the next debt document, with the number beside it, rather than as
"dropped for budget":

- **§7.0.1, the pelvis at exactly 0.0000 for three passes.** It needs a directive that
  translates and rotates a body, which is architecture, and it will not be what makes §0
  answer yes. Record it as permanent System 4 debt with the measurement (hip/hand 1.1% parry,
  0.6% phrase, System 2 failed at 1.5%) and hand it to whoever owns the directive vocabulary.
  If pass 4 has room for a *cheap* half of it, spend it on §10's last row instead: hips and
  shoulder peak on one sample and elbow, hand and tip on another, and separating those is a
  settle-time edit, not a new directive.
- **The blade trail** — a near-closed dome a figure height across, no taper, 2.7% above paper.
  Cutting it is cosmetic and it has survived three reviews without hurting the answer to §0.
- **The embers** (3–5 against §5's 8–20) and the bloom's missing rays — both fold into item 2;
  a 3-frame light cannot carry rays anyway.
- **The corridor's `head` and `feet` bands** — reader-unstable, floors inside the readers'
  own disagreement, and neither decides the picture.
- **The planning framing's composition** (two 136 px figures in the bottom-left corner of an
  85%-empty page). It is a camera/lane framing question and it is not Family B.
- **The capture's non-determinism** (§2.2) — record it, and add a two-figure clash frame to
  whatever plays the role of the null control, because `rig-bindpose` provably cannot see it.

---

## 11. What pass 3 got right, recorded so it is not lost

- **It refused a wrong instruction with a table, and the refusal holds.** `LANE_SPREAD = 1.35`
  merges the bodies on 10 of 24 frames against 2, passes 4 bands-complete frames against 11,
  and takes the blades from 0.0264 to 0.0903 apart. Pass 2's item 1 is withdrawn. This is the
  behaviour the project should want from a builder and it should be said plainly.
- **Its profile diagnosis is right**: the capture is 3.2–3.4× the corpus at the skirt, 1.6–3.8×
  at the feet, and zero at the pinch, and it is the *shape* that is wrong, not a scale.
- **Its contrast diagnosis is right and reassigns the work**: the pale duellist is already
  paler than the corpus (0.521 vs 0.436) and the entire remaining gap is the dark one at 0.245
  vs 0.133, sitting on `INK_INDIGO`'s own luminance. Nobody had measured that.
- **Every guard I broke went red with the message it was written to print.** Five of five.
  §11.2b(f) was applied honestly, including the load-bearing one, which really does bind
  `CLASH_SPAN`.
- **The honest caveat on the placement half of the clash guard** is not only correct, it is
  understated — those two assertions are theorems given the gap assertion. Keep it.
- **The bloom placement fix is real**: 44.8 / 31.6 / 10.8 px → 2.5 / 2.6 / 3.1 px, verified
  independently on two re-captures.
- **The pass did not declare itself a pass**, and §7 "what I did not measure" is the most
  useful section in the document. Three of the four things it lists were the first things I
  measured, and two of them are where the findings were.
