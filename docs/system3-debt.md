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
