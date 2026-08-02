# System 3 — pass 1 review record

**Status: FAILED at pass 1, one cause named.** Pass 2 is in progress against this document.

Source: the independent review of `out/captures/s3-p1-*`.

The verdict, which is the most compact statement of the problem:

> **The motion is poetic. The material is merely correct. The picture is neither yet.**

---

## The single named cause: the hair is unimodal

At matched scale the reference's hair is **bimodal** — a 30-55 px near-opaque mass, plus
1-2 px hairlines peeling off it, with **nothing in between**. Pass 1's hair is **unimodal at
5-11 px**: every strand drawn at the same width and the same edge hardness, which is the one
register that reads as neither mass nor wisp. It reads as **cord**.

| Measure (matched scale, 357 px figure) | Reference | Pass 1 | Target |
|---|---|---|---|
| Hair-region coverage, threshold 0.85 × paper | 60% | 23% | ~55% |
| Coverage at 0.6 × paper (core rather than halo) | 42% | 20% | — |
| Hair share of the figure's total ink | 15.8% | 6.0% | ~15% |
| Mark widths on vertical cuts | 1, 2, 1, 51 / 21, 33, 4 / 5, 55 | 8, 5, 18, 11 / 2, 6, 6 / 22, 8, 2 | bimodal |

**Do not fix this by strand count alone.** More strands at the current width closes the
coverage gap to grey mush *and* loses the wisps. The fix is a width and value split; count
follows from it.

Related value gap: the head mesh measures 26-45 luminance while the hair growing out of it
measures 78-88 median. In the reference the topknot and the hair mass are **the same value
and the same object**.

## The second cause: the hair is outside the ink material

Measured edge profile across a strand: paper to core in **one pixel**, core to paper in one
pixel, shoulders 1-3 px, **no wet-bleed halo at all**. The garment beside it transitions over
1-76 px behind a 10-131 px halo.

§3 opens with *"nothing in this game has a hard edge except the blades"* — and the one new
material this pass introduced is the only thing on the figure that has one. §3.2's stated
purpose is to make the figure sit *in* the paper rather than on it; the hair sits on it.

**This is why the bundle reads as cord: the value is fine, the boundary is not.**

## The third: cloth produces no readable mark

The entire cloth simulation is three chains of four particles, one pointing into empty space
beyond the garment silhouette. A tight hem-tip box registers **0.00 px across all 23
inter-frame steps**, and the skirt silhouette is the same shape through an entire knockback.

Three chains of four particles cannot bend. Cloth is half this system's title and is
currently a debug overlay. It also matters for what comes next: System 4 must express impact
through *how cloth trails*, because §7.1 bans freeze and shake — and it has nothing to
express it with.

## The fourth: the §7.2 gate captures do not gate anything

- The `s3-p1-extreme` 60 Hz window is **pure acceleration** — every tracked region's speed
  rises monotonically to the last delivered frame. No arrival, no settle, no return. So
  §7.2's "at most one soft return" and "arriving over ~0.8 s" have **no evidence at all**,
  and §11.2 says a coarse sheet cannot supply it.
- **The knockback drives in the same direction as the steady breeze, and the breeze is never
  switched off in any capture.** §7.2 now requires the overshoot test to be run against one
  impulse with ambient input killed — a rule added on this very implementer's recommendation,
  and then immediately violated by its own capture set.
- `s3-p1-hair` is billed "across a reversal" and contains **zero velocity sign changes** in
  any region across all 24 frames. The capture that actually earns the pass's timing result
  is `s3-p1-reversal`, which was listed third and shipped without a debug overlay.

---

## The topknot: structure and motion were separated, and only one was delivered

An earlier review called an *accidental* artefact "the single most reference-accurate feature
in any capture so far". At 6× in `s1-p2-bind/frame_000.png` it is a **near-black disc at ~90%
coverage with a hundred-odd sub-pixel spokes bristling off its rim** — core to hairline with
nothing in between, which is the reference's exact structure, arrived at by accident.

Pass 1's hair at the same zoom is six to eight fat grey worms with paper between them.

What pass 1 **recovered** is the dynamics the old artefact never had: it curls, it lags, it is
not radially symmetric — and the old one *was* radially symmetric, which §10 would eventually
have failed it for. What it **discarded** is precisely what the earlier review was praising:
density, and the mass-to-hairline gradient.

> The right target is the p2 artefact's *structure* driven by the p1 solver's *motion*, and
> this pass delivered one of the two.

---

## Claims refuted — measured against pixels

- **"Back hem 6 frames behind the hips"** — refuted as a visible fact. Both reverse at window
  frame 11.5 with near-identical velocity curves, and the hem-tip box moves 0.00 px. Whatever
  the particle does, the picture does not show it.
- **"Hair streaming 24 px ahead of the head"** — contaminated by the driver-versus-resonance
  trap. Measured 13.7 px, of which 10.1 accrues *before* the acceleration phase: that is the
  breeze. On the leftmost escapee the head-to-tip span actually **contracts** during
  acceleration.
- **Peak frames 126/132/139/146/147/150** — not corroborated. The delivered 60 Hz window
  covers roughly frames 111-134, so three fall outside it, and the claimed hip peak at 126 is
  not present (speed rises monotonically to the last frame). §7.1 requires the headless
  measurement to drive the same scene the capture runs; that is exactly the guarantee in
  question.
- **"`ik-gesture` unchanged"** — not demonstrated. The diff against `s2-p3-gesture` is 76,287
  pixels, consistent with System 1's pass-7 paydown which landed after that capture, but
  there is no post-p7 pre-S3 baseline to check against. Capture one before System 4 starts.
  Note also that neither regression scene renders hair, so no hair behaviour is exercised in
  the poses System 2 was graded on.

## Claims verified

- **The bind pose is bit-identical** to `s1-p7-bind` — md5 `ce533e77b1a1addefb4eda803bd11d5c`
  on both.
- **Nothing peaks on the same frame, and this is the pass's best result.** By inter-frame
  registration on `s3-p1-reversal`, velocity zero-crossings land at **hips 11.5 → head 13.5 →
  hair root 18.5 / hair mid 20.5 → sleeve+blade 22.5**. An 11-frame spread down the chain,
  visible in delivered pixels at a true frame rate. §10's last row is closed for hair, and
  hair lags the hips by 7-9 frames, inside §7.1's band.
- **No polyline kink anywhere**, checked at 14× on the thinnest escapee tips. §4's
  instant-fail is not tripped.
- **Per-strand variation** — six strands curling at six different radii and phases.
- Knockback half-travel ~0.42 s, consistent.
- No snap, no strobe, no double oscillation in any delivered window.

---

## Must not regress

- **The chain of arrivals** (hips 11.5 → head 13.5 → hair 18.5-20.5 → sleeve 22.5). The
  cheapest poetry in the project. Any re-parameterisation that shares a settle time down the
  chain resurrects §10's last row.
- **Smoothed Catmull-Rom paths** — impacts are where a kink would appear.
- **Per-strand variation.** A global wind term applied uniformly under knockback trips §10's
  "uniform hair motion".
- **The escapees**, rendered finer and fainter than the bundle. §4 says these sell the
  dreamlike quality and they do.
- **The bit-identical bind.**

## Risk handed forward to System 4

There is currently **one collider — the head**. Hair tips already terminate deep inside the
chest at rest, hidden only because the torso is dark. Knockback will throw them across the
torso and across the grip/guard cluster, which `docs/system2-debt.md` E2 already flags as the
figure's most fragile small mark.

---

# Pass 2 review — FAIL, one cause. Pass 3 in progress against this.

> **The hair is poetic. The cloth is correct in the solver and absent in the picture.
> The figure is now poetic from the collar up and inert from the sash down.**

## What pass 2 won, and must not be reopened

**The hair is the first thing in this project judged poetic in material and motion
together** — not because the histogram moved, but because at 4x through the knockback the
mass streams and folds back over the skull while three escapees curl away on a slower clock
and dissolve into sub-pixel. It has a source, it has effort, and nothing in it arrives at
the same time.

Verified, every rectangle recorded:

| | box | mid-band share | coverage @0.85 paper | ink share |
|---|---|---|---|---|
| pass 1 | x442..546 y108..189 | 38% | 47.7% | 9.5% |
| reference | x100..199 y0..79 | 12.5% | 57.1% | 8.5% |
| pass 2 | x455..559 y108..189 | **0%** | **66.1%** | **13.1%** |

And it **survives the extreme**, which is where §7.2 says the aesthetic dies: through the
knockback, bimodality 0.854-0.945 with the mid-band at 10-19%, i.e. sitting on the
reference's number throughout.

Edge profile: from a hard cliff to a **14-28 px ramp** with a near-black core at 33-49
behind it. Claimed 35-40; a real fix, oversold by about a third.

Matched-scale part count: pass 1 resolved roughly a third of the reference's parts, **pass 2
resolves roughly two thirds** (about 18 against 26), and the largest single contributor is
the hair.

§7.2 in dead air, measured against a disturbance for the first time: hair span 99.5 to 115.2
to 86.8 px, **monotone in and monotone out, one excursion, one return**. Cloth chain the
same. Body travel 90% by 0.75 s, 99% by 0.94 s, 0.9 px overshoot — §7.2's "arriving over
~0.8 s" met almost exactly. The value floor holds at exactly 25.73 on **every frame of all
nine** capture directories.

## The named cause: the cloth arrives with the body

Through `skirtHigh` = fig[0.15,0.55,0.70,0.13] to x470..603 y308..353 on
`s3-p2-fix-reversal` at 60 Hz, anchored on `hips`: the garment trails by **0.74 frames**
(register) and **1.02** (centroid), against §7.1's **4-8**. Through the `hem` box the two
methods disagree in *sign* with swings of 0.24 and 1.22 px/frame — the tooling reporting no
signal. Headless at 240 Hz on the same window: **no reversal above the 0.15 px/frame gate at
all**, hem travel 4.8 px against shoulder 30.2 px.

**Against pass 1 this is a regression in delivered pixels**: the identical `hem` box on the
same scene and beat registers 11.9 px of path at 0.97 px/frame peak in pass 1, and 6.1 px at
0.59 in pass 2.

## Why the pass could not see it — the instrument reads the wrong thing

`SimTimingTest` measures `back.x(back.particleCount()-1)` — **the particle, not the
picture**. Every cloth lag figure pass 2 stated is a reading of the simulation's own state.
Pass 1's finding was verbatim *"whatever the particle does, the picture does not show it"*,
and the verification instrument still reads the particle. `docs/feedback-loop.md` anticipates
this precisely and records that nothing implements it yet.

**Two passes in a row have now been lost to that confusion.** `SceneProbe` is one class and
it makes the whole cloth question falsifiable.

## The escalation: premise refuted, conclusion granted

The previous pass escalated the cloth question rather than acting unilaterally, which was
right. The review then **refuted its premise**: `buildHaori` assigns cloth bones to rows
whose authored dissolve is **0.0 — fully solid**, and the back rail spans the whole visible
skirt in screen terms. "The readable lower-body mass has no cloth chain at all" is not true
of the code as shipped.

**And granted the conclusion anyway**, on a better reason:

> A silhouette with a smooth continuous boundary cannot express a fold no matter how the
> bones underneath it rotate.

Coverage confirms it: `torso` x421..560 y191..270 is **97.0%** covered and `hips` **97.7%**,
where §3.3 says coverage is not uniform and ink skips.

**Ruling.** Pass 3 may change the base figure render and break the `s1-p7-bind`
bit-identity, for the specific purpose of giving the chain-driven rows an authored edge.
Four conditions: confined to those rows; `s1-p7-swing`, `ik-gesture` and `ik-extreme` stay
within a level or two; **a new bind baseline captured and its md5 recorded here in the same
commit**, so the guard is re-armed rather than removed; and the *cheap half proven first* —
sim parameters are bind-safe by construction, so the 4-8 frame lag must be reached on
delivered pixels before any mesh is touched.

## Also open

- **Hair tips terminate inside the body**: 36% buried at rest, **64% at the knockback
  peak**. The torso collider reduced this and did not close it, and it is *larger* under
  impulse than at rest — the opposite of what a collider buys. Handed forward as a System 4
  risk.
- **Capture hygiene, three defects.** `capture.txt` exists for no graded capture (the
  feature landed after those frames were shot). No `-debug` siblings for any graded window,
  so the reviewer shot its own in order to say anything about the chain at all.
  `s3-p2-fix-impulse-return` runs at **24.7 Hz**, not a true frame rate — and it is the
  capture whose job is to answer the settle question.
- **`sway-reversal` is named for a reversal it does not contain**: hips reverse at frame
  22.11 of 24 and the hair's reversal falls entirely outside the delivered window. Verbatim
  the defect pass 1 was pulled up on, in the capture named for the same beat.

## What must not regress, for System 4

Hair bimodality (mid-band 0-19% through a recorded box, at rest *and* through knockback);
hair coverage 57-66% and ink share about 13%; the hair edge (core 33-49, ramp 14-28 px); the
arrival chain headless (hips to head +0.067 s to hair-root +0.100 s at 240 Hz over
`sim-extreme` t=0.876-1.260); one soft return under one impulse in dead air, in all three
channels, with `sim-impulse` kept breezeless; knockback at 90% by 0.75 s and 99% by 0.94 s;
the value floor at 25.73 on every frame, with impact blooms lifting and never multiplying;
smoothed paths, per-strand variation and the escapees; no periodic artefact above 0.25.

---

# Pass 3 record — what was built, what was measured, and where the brief is wrong

**Every number below is printed beside the rectangle it was taken through (§11.3), and every
capture in `out/captures/s3-p3-*` carries a `capture.txt` with the command that reproduces
it.** The graded reversal window is `sim-extreme -Pstart=0.9 -Pstep=0.0167 -Pframes=24`,
recovered by md5 search and **bit-identical to `s3-p2-fix-reversal`** — so the before/after
below is the same scene, the same beat and the same pixels.

## 0. The new bind baseline — the guard is re-armed

The ruling authorised breaking `s1-p7-bind`'s bit-identity for the trailing edges. It is
broken, deliberately, and replaced:

| capture | md5 of `frame_000.png` |
|---|---|
| `s1-p7-bind` / `s3-p2-fix-bind-regress` (retired) | `ce533e77b1a1addefb4eda803bd11d5c` |
| **`s3-p3-bind-regress` (current baseline)** | **`2340bfc3234e3e1f19f4c17b040120fd`** |

Old to new: 23,211 of 518,400 pixels differ (4.48%), **mean absolute delta 1.22 levels**,
changed region x343..592 y95..452. The change is the skirt's trailing edge; head, face,
blade and torso are untouched.

Regression scenes, same measure: `ik-gesture` 15,057 px per frame (2.90%), **mean 0.72
levels**; `s1-p7-swing` about 21,000 px per frame (4.0-4.5%). Mean delta is inside "a level
or two" everywhere. Peak delta at the new frayed boundary is about 200 levels, because that
is what paper-to-ink is; a mesh change cannot be authorised and then be required to move no
pixel by more than two levels, so both numbers are recorded rather than one.

## 1. `SceneProbe` — implemented, and it is what settled the question

`dev.starfall.rig.SimProbe` plus `SimScene implements SceneProbe`, and `analyse timing` now
folds the probe series into the same arrival chain as the pixel regions, tagged `sim:`.
`docs/feedback-loop.md`'s "nothing implements it yet" is no longer true.

It answers the pass-1 question — *whatever the particle does, does the picture show it?* —
directly, because both are recorded through the same clock and the same camera:

    ./gw timing -Pscene=sim-extreme -Pstart=0.90 -Pduration=0.384 -Prate=240 \
                -Pregions=docs/regions.json -Pout=out/timing/x.json
    ./gw analyse -Pargs="timing out/timing/x.json --anchor hips --smooth 4 --axis x"

Probe points are the whole back rail (`back0`..`back5`), not just the tip, because *where
down the panel the motion starts* is the entire question. `back0` is pinned to the hips and
can only ever arrive with them; the tip is 1.15 units of chain below it.

Resolved through `docs/regions.json` on the graded window, `skirtHigh` is
**x466..602 y308..353**, and the probe puts `back1` (row 5) at **y=321** and `back2` (row 6)
at **y=360** — both inside that box. So the box does contain the chain's readable rows.

## 2. Step 1, the cheap half — done first, measured, and it does not work

Back rail: `dragTau` 0.095 to **0.120**, bend recovery 0.060 to **0.070**, swing limit 30 to
**40 degrees**. Bind-safe by construction.

| | particle, `sim:back1`, 240 Hz | delivered pixels, `skirtHigh` x470..603 y308..353, 60 Hz |
|---|---|---|
| pass 2 | +8.6 samples (2.1 frames) | **+0.72** register / **+1.02** centroid |
| step 1 alone, drag 0.17 / bend 0.14 | +14.6 samples (3.7 frames) | **+0.85** register / **+1.15** centroid |
| step 1 alone, drag 0.22 / bend 0.22 | +15.8 samples (4.0 frames) | **+0.87** register / **+1.19** centroid |

**The particle lag doubled and the picture did not move.** That is the pass-1 finding
reproduced under measurement for the first time, and it is why the probe had to exist before
anything else.

### The control that settles it

Capture the same window with the cloth chains' swing limit clamped to zero — a garment
rigidly welded to the hips, no simulation at all — and measure the same box:

| `skirtHigh` x469..603, `analyse track --anchor hips --axis x --method register` | lag |
|---|---|
| **cloth clamped rigid (control)** | **+0.34 frames** |
| pass 2 as shipped | +0.72 frames |
| pass 3 as shipped | +0.87 frames |

A rigid garment reads +0.34 frames through that rectangle. **The whole dynamic range of the
statistic on that box is about half a frame**, because the box is mostly things that are not
cloth: the obi, both thighs, and the katana scabbard, which `buildDaisho` runs from
(0.322, 1.176) to (-0.566, 0.638) — in pixels from (594,245) to (420,350), diagonally across
the upper left of the box. Registration fits **one translation to the whole rectangle**, so
a four-frame cloth signal occupying a sixth of the box's gradient energy prints as +0.6.

**This is the part of the brief that is wrong.** "Raise `dragTau` and lower bend stiffness
until `analyse track --anchor hips` reads 4-8 frames on `skirtHigh`" is not reachable by any
setting of those two numbers, and the reason is not the cloth. It is an instruction whose
stopping condition has no solution.

## 3. Two further reasons the 4-8 band cannot be met on that window

**(a) A first-order lag cannot exceed a quarter period.** The chain is deliberately
first-order — `VerletChain.bendPass` moves `prev` with `x` specifically so the bend relaxes
rather than rings, which is what keeps §7.2's "at most one soft return". A first-order lag
therefore tops out at 90 degrees of phase, and the graded window's beat is about 0.4 s, so
its ceiling is **0.1 s, i.e. 6 frames, at zero amplitude**. Eight frames on that beat
requires a resonance, which §7.2 bans by name. Anything inside the band is bought against
amplitude, and amplitude is what pass 1 was praised for and pass 2 failed on.

**(b) The onset statistic and the reversal statistic want different stiffness.** On
`sim-sway` — the *aesthetic* scene, the slow one — `SimTimingTest`'s onset delay reads the
bend time constant almost directly. Measured across a sweep, with the readable row
(particle 2) as the signal:

| back `dragTau` / bend | hem row-6 onset behind hips, `sim-sway` |
|---|---|
| 0.110 / 0.060 | 7 frames |
| **0.120 / 0.070 (shipped)** | **8 frames** |
| 0.140 / 0.085 | 9 frames — outside §7.1 |
| 0.170 / 0.110 | 11 frames |
| 0.220 / 0.240 | 16 frames |

So §7.1's band is already satisfied on the slow scene at pass 2's stiffness, and raising the
damping to chase the fast scene's number **breaks the slow scene's**. The shipped numbers sit
at the top of the band on `sim-sway`, the scene the one-sentence test is answered on, and
that is the trade taken.

**Correction to the test, and it is a correction to a measurement.** `SimTimingTest` recorded
the cloth as `back.x(particleCount()-1)`. At 1.15 units of lever a bend of **2.7 degrees**
cancels the whole of the hips' travel, so a hem that trails properly leaves its tip very
nearly stationary in world space and the onset statistic on it becomes a reading of noise:
with pass 3's damping the tip's onset lands **two frames before the hips'**. It now grades
particle 2, the row that carries readable ink and sits inside the graded box. The tip is kept
as a printed diagnostic.

## 4. Step 2, the mesh — proven necessary by section 2, and built

`SamuraiRig.MeshAuthor.trailingLeaf`. Three overlapping panels on the chain-driven rows, two
on the back rail and one on the front, each attached along the rail and bulging past it, each
terminating in its own frayed boundary. Three things are load-bearing:

- **The outer edge is the strip's own `u = 1` boundary**, so `ink_skin.frag`'s
  boundary-distance fray cuts it into flecks. The inner edge is at `u = 0.5`, interior, so
  the leaf melts into the solid mass instead of drawing a seam across it.
- **Authored fray now reaches rows 5-6**, where `dissolveB` was 0.0 flat. The rail underneath
  is untouched: the review is right that rows 5-6 are where the references put the heaviest
  black, so the mass stays and only the *edge* frays.
- **Each leaf is skinned one or two bones further down the chain than the row it sits on**, so
  a bend rotates them by different amounts, their boundaries cross, and the gaps between them
  are the fold.

Two failures found by looking at the pixels rather than at the code, both recorded in the
source:

1. The inner edge authored *on* the rail printed a **one-pixel hard line down the whole back
   of the skirt** — a `u = 0.5` interior boundary landing exactly where the main strip is
   fraying. Fixed by insetting it 0.85 of the leaf's own reach into the opaque mass.
2. The inner edge authored at dissolve 0 laid a solid panel over rows 7-8, where the rail is
   0.14 and 0.58 dissolved, and **filled in the ink smoke**. It now carries the rail's own
   dissolve.

Result on the graded window, `skirtHigh` x466..602 y308..353, `--axis x`: **+0.87 frames
register, +1.19 centroid** — unchanged by the mesh, exactly as section 2's control predicts.
`hem` x473..591 y382..441 improves to **+3.01 frames** and 6.6 px of registered path
(pass 2: 6.1 px). Coverage through `docs/regions.json`: `torso` 98.0 to 98.2%, `hips`
100.0 to 100.0%, `skirtHigh` 95.3 to 97.7%. **The interior skip §3.3 asks for is not
addressed by this work** — the leaves change the boundary, not the fill — and that is open.

**Was step 2 necessary?** Necessary for the *picture*, and it delivers what the ruling asked
for: at 4x the back of the skirt is now three overlapping arcs with separated brush marks
below them instead of one smooth boundary. Necessary for the *number*: no. Nothing moves that
number, per section 2's control.

## 5. Hair tips inside the body — closed

Pass 2 modelled the torso as two circles of radius 0.125 and 0.130 world units against a
garment measuring 0.58 units across at the ribs. Less than half the width of the body they
stand for, so a tip could clear every collider and still be drawn deep inside the figure —
and it got *worse* under the impulse, because the impulse is what throws the bundle across
the body.

`VerletChain.MAX_COLLIDERS` 4 to 6; the torso is now three circles read off the haori rails:
chest r 0.240, spine r 0.222, obi r 0.200.

New test `hairTipsDoNotTerminateInsideTheDrawnBody` measures tips against a capsule from the
hips to the neck of radius 0.20, **independent of the collider list**, so shrinking a
collider fails it:

| | at rest (t=0.90) | at the knockback peak (t=1.45) |
|---|---|---|
| pass 2 colliders | 20% | **45%** |
| pass 3 colliders | **0%** | **0%** |

## 6. Capture hygiene — all three fixed

- **`capture.txt` exists for every `s3-p3-*` capture.** It was already implemented; the
  pass-2 set predates it.
- **Debug siblings shipped**: `s3-p3-reversal-debug`, `s3-p3-sway-reversal-debug`,
  `s3-p3-impulse-debug`, `s3-p3-knockback-debug`, each the same scene, start and step as its
  graded twin.
- **Every capture is at a true frame rate**, `-Pstep=0.0167`, 60 Hz. Nothing is at 24.7 Hz.
- **`sway-reversal` re-aimed.** `sim-sway -Pstart=1.66 -Pframes=36 -Pstep=0.0167`: the hips
  reverse at frame **7.67 of 36** (pass 2: 22.11 of 24), hair-root at 8.55, sleeve at 13.38 —
  the chain is inside the delivered window instead of past its end.

## 7. Must-not-regress, re-measured

| item | recorded | pass 3 | box |
|---|---|---|---|
| hair coverage | 57-66% | **60.3%** | `hair` x426..563 y108..214 |
| hair ink share | about 13% | 19.4% by count | same box, which is wider than the review's |
| hair mid-band 5-16 px | 0-19% | **0%** | `hair-mid` x440..515 y125..199 |
| hair bimodality | bimodal | **0.905** (limit 0.556) | same box |
| head behind hips, headless 240 Hz | +0.067 s | **+0.066 s** | `sim-extreme` t=0.876-1.260 |
| value floor | 25.73 every frame | **25.73, floor respected** | whole frame, `s3-p3-reversal` |
| periodic artefact | none above 0.25 | **none** | `torso` |
| one soft return, dead air | 1 | **1** (`theSettleIsOneSoftReturnAndThenStillness`) | headless |
| knockback 90/99%/overshoot | 0.75 s / 0.94 s / 0.9 px | unchanged (`theKnockbackArrivesInOrder`) | headless |

`hair-root`'s +0.100 s could **not** be reproduced: through `docs/regions.json`'s `hair-root`
box that region has no dominant reversal in the t=0.876-1.260 window on either axis. The
review's box is not recorded, which is §11.3's own point turned back on the review.

## Open, handed to the next pass

1. **The graded rectangle.** `skirtHigh` measures the waist, and a rigid garment scores +0.34
   through it. A cloth lag graded on delivered pixels needs a box containing cloth and not
   the obi, the thighs and the katana. Either move it, or grade the cloth through the `hem`
   box (which reads +3.01 and is at least monotone in the right direction), or accept that
   §7.1's frame band is a *solver* statement and grade the picture on something else.
2. **§3.3's interior skip.** `hips` is 100.0% covered and `torso` 98.2%. The leaves gave the
   silhouette an edge and left the fill alone. That is a dry-brush and shader job, not a mesh
   one.
3. **The 32-bone cap.** 31 of 32 used. The front rail's pivot could not be raised to row 4
   without spending the last slot; it was tried and reverted, because moving it collapsed the
   skirt's width — the front panel blew back under the steady breeze — for no measurable
   gain, see section 2's control for why no weighting change can move that number.
4. **The principal-axis sign trap.** `analyse track --axis principal` chooses each region's
   axis independently and its sign is arbitrary, so a mesh change flipped `skirtHigh`'s axis
   relative to `hips` and the same-direction reversal filter then reported "no reversal in
   window" for a region that plainly reverses. Quote lag with `--axis x` on this figure, or
   fix the tool to align each region's axis to the anchor's.
