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
