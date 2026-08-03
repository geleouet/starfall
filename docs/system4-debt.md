# System 4 — standing debt

**Status: pass 2 shipped, self-graded, and NOT self-declared a pass.** Two of the
four acceptance criteria this document set for pass 2 are met; two are improved by
large factors and still miss. Everything is measured, every measurement is a
checked-in command, and the misses are named below with their numbers rather than
described.

Every capture quoted here is `s4-p2-*`, shot at commit `3671fdb`+ on the harness
that produced them. Nothing from `s4-p1-*` is quoted: it carries the harness ghost.
Every pixel number is a ratio to the frame's own figure height, per §11.3.

---

## 1. The parry: the blades now meet, and the bloom is on the meeting

**Before (pass 1):** minimum blade-to-blade separation **98.4 px on a 462 px
figure = 0.213 of a figure height**, and the blades diverged monotonically after
t=1.537. The clash bloom fired two frames later, on the attacker's grip.

**After (pass 2):**

| instrument | before | after | acceptance |
|---|---|---|---|
| pixel, `analyse blades` | **0.213** | **0.0287** | ≤ 0.02 |
| geometry, `RehearsalTest` | 0.2105 | **0.0000** | ≤ 0.02 |

`./gw analyse -Pargs="blades out/captures/s4-p2-parry-contact --max 0.02"`

**A 7.4× improvement in delivered pixels, and it misses by 0.9 percentage points.**
It is a miss and it is reported as one. The two instruments disagree — the geometry
says the blade *segments* intersect for four consecutive frames, the pixels say the
two cool-bright clouds come no closer than 11.3 px — and the disagreement is real
rather than an artefact: the blades are drawn as tapering slivers with a soft glow
and a warm clash core between them, and the cool-bright test (correctly) refuses to
count the core, so the two clouds stop where the steel stops rather than where the
axis crosses. **The pixel number is the honest one** and it is the one quoted first.

### What was wrong, which was three things and not one

The review said "one aiming bug". It was one *symptom* with three causes, all of
which had to go:

1. **The target named a blade and the chain moved a fist.** `Meeting` names a
   crossing twice; both halves went straight into `Chain.SWORD_ARM`, whose effector
   is `handL`'s origin. The blade hangs `0.10` out of the fist at 45° and runs
   `0.68` further, so two fists a finger's width apart put two blades most of a
   metre apart. Fixed with `Anchor.Site.CROSSING`, `Stage.crossing`, and
   `Director.fist`, which places the hand back down the blade from the crossing.

2. **Nothing in the rig could point a sword.** The blade's world angle was the
   forearm's plus a constant. `RigIk.wrist` and `RigIk.blade` are two `AimLink`s
   that aim the *blade* — the wrist carries `AimLink.axisOffsetDeg`, new this pass,
   so a link can aim something other than its own axis. They take links 2 and 3 of
   the directive's own `Settle`, which `Chain.SWORD_ARM` has declared since System 4
   pass 1 ("the hand, and the blade tip hanging off it") and which nothing drove.

3. **The elbow was on the wrong side of the fist, and a pole could not fix it.**
   The forearm points from elbow to fist and the blade is the forearm plus 45°, so
   an elbow in *front* of the fist points the blade behind the figure — which is
   exactly the review's "drawn from a grip on the far side of its own torso,
   pointing down and away". `Figure.REST_POLE_X` was `+0.10`, copied from
   `SimSceneDriver` where it is right.

   **The finding worth carrying forward is why moving the pole was not enough.**
   `TwoBoneIk` picks its bend side by testing the pole against the *settled* aim,
   and the settle is 0.48 s while the hand crosses most of a body height — so
   through the whole approach the aim it tests against still points behind the
   shoulder. Measured: the pole sat 0.37 to the correct side for the entire contact
   span and `bendSide` never moved off −1.00. Even a flip that did commit takes
   `flipSeconds` = 0.60 s, longer than the beat. So `Figure.pinElbow` pins the side
   per facing and the pole keeps its other jobs.

### Two more staging errors found on the way, both of which had shipped

- **Two directives on one chain at one instant.** `stageBeat` emitted the actor's
  release at `contactStart` from the beat's `Focus`; `bladeMet` emitted a second
  target on the same chain at the same instant from the `Meeting`. The director
  resolved the tie by emission order and interpolated across a zero-length segment,
  so the parry's curve ran from a point the hand had never been at.
  `Scheduler.supersede` withdraws the less specific one.
- **The strike was driven with a settle profile built for a rest.** `IkChain`
  implements `settleSeconds` as the time constant of the filter chasing the target,
  so 0.48 s means the hand covers about a fifth of its journey in a 0.168 s contact
  span. `Scheduler.STRIKE` (0.30/0.33/0.36/0.39) is inside §7.1's band and strictly
  increasing, so §7.0.3 is unaffected; the recovery keeps the long profile.

### The bloom is honest, and there is a test that says so

`RehearsalTest.everyClashIsDrawnWhereTwoBladesActuallyAre` fails the build when a
`CLASH` directive is live on a frame whose two blades are more than 2% of a figure
height apart, or whose origin is a point on one body rather than the reconciled
crossing. That is §11.2b(e) put in the tool. `Director.renderInk` draws the mark at
the crossing the two aims actually used, so it cannot land on a grip by
construction.

### The instrument that made all of this possible

**`dev.starfall.direct.Rehearsal`** plays a whole duel through the real schedule,
director, rig, IK and simulation with **no GL context** — `SamuraiRig.headless()`
is the entire trick — and exposes each blade's world segment per frame. Pass 1's
tests were all green because all of them were written against the *schedule*. This
is the first thing in the project that can assert about the *picture* without
shooting a capture, and its first run reproduced the review's 0.2105 to within a
tenth of a percentage point by a completely different route.

**It is not a substitute for pixels and must not be quoted as one.** It knows where
the geometry is. It does not know what the material paints, what the halo does to a
silhouette, or where the camera is looking.

---

## 2. The corridor: opened, guarded, still zero at the pinch

**Acceptance:** never zero, never below 0.06 of a figure height. **Not met.**

| capture | frames | corridor = 0 | one connected blob | median |
|---|---|---|---|---|
| pass 1 parry contact | 24 | 67% | 25% | 0.000 |
| **pass 2 parry contact** | 24 | **75%** | **12.5%** | **0.000** |

`./gw analyse -Pargs="corridor out/captures/s4-p2-parry-contact --min 0.06"`

**The guard is checked in and tested** (`analyse corridor`, `Duellists.corridor`,
`DuellistsTest`, four cases including the null case where two touching bodies must
report "one mass" rather than "zero columns" — those are different claims). That was
the stated deliverable: *"mechanism is yours; the guard is the deliverable, not the
tuning."* The tuning is not done.

**What the guard reveals, which the pass-1 review could not see.** The corridor is
not zero because the bodies merge — the merge rate halved, 25% → 12.5% — it is zero
because **the two figures' arms overlap in x by about eight pixels at the bind.**
The printout says so directly: the left body's rightmost ink column is at 596-612
while the right body's leftmost is at 588-594. They are two separate ink components
with no clear column between them.

**That is a consequence of getting the parry right, and it is the tension this
document should hand forward.** Reference image 3 has the duellists' hands almost
touching — 0.12 of a figure height apart — and a clear corridor of 6-11% elsewhere,
because *its* figures are narrow at the pinch. This rig is not: `SamuraiRig`'s haori
rails run 0.64 units wide against a `Stage.BODY_HALF` of 0.56, plus §3.2's wet-bleed
halo. So on this rig "hands close enough to bind" and "a column of paper between the
bodies" are in direct conflict, and pass 2 chose the bind.

`LANE_SPREAD` moved 1.35 → 1.55, which bought the corridor five frames it did not
have and cost the figure height 462 → 394 px (still above the 330 px every
matched-scale comparison in this project has been run at). Pushing further was
tested at 1.70 and made the parry worse: the arms cannot reach a crossing that far
out, and the blades stopped meeting.

**The clean fix is the one pass 1 already named and neither pass has done:** it is in
`Stage`, not in the Director's mitigation. Either `TILE_WIDTH` rises against
`FIGURE_HEIGHT`, or `BODY_HALF` grows to the width the rig actually has and the lane
spacing follows it — and then the arms can bind without the bodies overlapping,
because the *hands* would be closer to the bodies' leading edges. A per-body facing
offset that narrows the silhouette during contact (`Stance.PASSING` already does
this for a swap) is the other candidate and is cheaper.

---

## 3. The region set refuses, and refuses in the right places

**Closed.** `RegionSet.Which` and `RegionSet.resolve(frame, paper, factor, which)`.

- A figure-relative region on a frame whose ink resolves into two components each
  holding ≥10% of the total **throws**, naming the fix (`--figure left|right`) and
  quoting the failure it prevents.
- Naming a figure on a one-body frame **also throws**: the mismatch runs both ways
  and is equally silent if allowed through.
- An **absolute-pixel** region is always answerable and is never refused — it names
  its own window and never touches the detected box. This was got wrong first and
  is worth recording: the refusal has to be scoped to the thing that is actually
  ambiguous, or it stops the tool measuring anything at all on a two-figure frame.
- Every single-figure capture in the corpus resolves bit-identically, asserted.

`analyse` grew `--figure left|right`, and `track`'s anchor goes through the same
refusal — which is what closes the specific defect the review found, since
*"hem trails hips by +6.52 frames"* came out of `track --anchor hips`.

**What it catches, concretely:** on `s4-p2-parry-contact` frame 11 the detected
figure box is `x213..612 y314..707` — 400 px wide, spanning the hero and most of the
foe. Any `fig:`-space region resolved against it is measuring a rectangle that
straddles the gap. It now says so instead of answering.

---

## 4. The second body has a value structure

**Closed on value, open on colourway.**

Skirt bands, `s4-p2-parry-contact` frame 11, paper 219.0, absolute rectangles:

| region | rect | mean ink luminance | as × paper |
|---|---|---|---|
| hero skirt | x330..470 y500..620 | 44.4 – 61.6 | 0.20 – 0.28 |
| **defender skirt** | **x620..760 y500..620** | **63.0 – 79.8** | **0.29 – 0.36** |

Against pass 1's defender skirt at **119.9 (0.55×) with a minimum of 87.8**, and
against the review's complaint that *"nothing in the defender's lower body ever
reaches below 0.40× paper"*. It does now, throughout.

The change is one line and the reasoning it replaces was wrong in an instructive
way. Pass 1 pooled the pale figure to `INK_SLATE` on the argument that *"a pale
garment that pooled to the floor value would not be a pale garment, it would be a
dark one with a pale rim"*. That sounds right. Reference image 3 refutes it: its
white-clad duellist is pale above the sash and **near-black below**, with a dark
collar and dark hair on the shoulder — the floor is reached on the same figure, in
the places ink collects.

**And it costs something, which this pass noticed and did not fix.** Family B's
composition is a *dark* duellist against a *pale* one, and pooling the pale figure
to the floor narrows that contrast. On `s4-p2-phrase-check` the two figures read as
much closer in value than they did — the second body is now a silhouette, which is
what §4 asked for, but it is no longer obviously the *other* colour.
`DirectorTest.bothFiguresAreVisuallyDistinguishable` did not catch it because it
asserts on the material's base colour, not on delivered pixels, and a test that
compares two figures' rendered value distributions is the guard this needs. The
right answer is almost certainly the per-region channel above — pale kimono, dark
hakama — rather than a compromise on the pooling colour, and a pass that reaches for
the compromise should measure both figures' value histograms first.

**What is still open:** image 3 has a colourway that *changes at the sash*, and
`InkMaterial` has one base colour per draw call. Pooling to the floor darkens the
folds and the hem of the whole garment; it does not make the hakama a different
colour from the kimono. Closing it wants a per-vertex or per-region colour channel —
**the same missing channel `Director.dissolve` reports for §7.3's "pushed
*locally*"**, which is now two requirements blocked on one mechanism and is the
strongest argument in this document for building it.

---

## 5. Still open, unpaid, and named here so the next pass reads them

Pass 2 spent its budget on items 1 to 4. These are the review's smaller items and
none of them was touched. They are listed with what is known rather than with an
excuse.

- **§7.3's ink bloom still does not appear in a delivered frame.** The vocabulary is
  built and dispatched; no scene lands a blow. `Duel.Kind` needs a fourth entry
  whose encounter resolves a `Hit` rather than a `BladeMet` or a `Shoved` —
  `ContactTest` has fixtures for it. This is the cheapest item on the list and it is
  the one the review put first among the smaller ones.
- **Knockback: the figures still converge.** Gap closes monotonically 169 px in
  0.535 s with a static camera. `Scheduler.shoved` emits a pose, a hem and a hair
  impulse for the shoved body and **no trunk target**, so nothing translates it; the
  only body motion in the beat is the pusher stepping in. `CombatEvent.Moved` does
  carry the shoved body's new tile in the knockback fixture — `Scheduler.moved`
  handles it — so the fix is to check whether the `Shoved` and the `Moved` are being
  scheduled against each other rather than to invent a displacement.
- **The blade trail is still 1.27 figure-heights of near-closed dome** and persists
  eight frames after the blade has left. It reads as a moon, not a stroke. Cut its
  extent and taper it: brightest at the leading edge, fading over the tail.
  `InkSkinnedRenderer` builds it from the blade's sampled poses.
- **The held breath is still 0.857× for ~0.12 s** against §7.3's ~0.25 s, and there
  is still none on the knockback. `Timing.HELD_BREATH_SECONDS` is the lever and the
  instrument to grade it exists (the debug schedule cursor at uniform wall-clock
  steps); `Rehearsal.Frame` now records `t`, `wall` and `timeScale` per frame, so
  this can become a test in the rehearsal rather than a capture reading.
- **The 0.317 s hole at frames 312-330 of the phrase** — hip and chest within 2 px,
  blade within 8 — is untouched. System 2's E1 applies: a held pose must still carry
  weight.
- **The four seams pass 1 named** are all still open: the trunk anchor at hip height
  against a chain ending at the neck; no directive translates a body; foot anchors on
  the ground against leg chains ending at the ankle; and the tile width, which is
  item 2 above and is still the most damaging.

---

## What passed in pass 1 and must not have regressed

Pass 2 changed `LANE_SPREAD`, the arm's elbow pole, the sword arm's settle during a
crossing, and the pale figure's pooling colour. **All four of those touch the phrase
and the knockback as well as the parry, and pass 2 did not re-measure them.** That is
a real gap in this pass's own grading and it is stated here rather than left to be
discovered:

- **The phrase's continuity result** (417 inter-frame steps, minimum 0.00229 against
  a static control's 0.00009) was not re-run. The elbow pole change in particular
  moves every frame of it.
- **The held breath** (0.857× for ~0.12 s) was not re-run.
- **The blade trail's smoothness** was not re-run, and the blade is now driven by
  two aim links it did not have, which is exactly the sort of change that could
  introduce a kink.

`s4-p2-null-static` is shot and on disk as the stationary control for whoever does
re-run them.

---

## Two things in the pass-2 brief that this pass thinks are wrong

**The brief asked for the corridor and the bind at the same time, and on this rig
they are in conflict.** Not a little: reference image 3 gets both because its figures
are narrow at the pinch and its duellists' hands nearly touch. This rig's garment is
wider than the body box the staging layer thinks it has. Any pass that treats
"open the corridor" as a tuning problem in the Director will trade it against the
parry, exactly as this one did. It is a `Stage` problem and it should be given to a
pass that owns `Stage`.

**"Segment both blades as connected components of cool bright pixels, then
point-to-point minimum" measures something slightly different from "the blades
meet".** It is a good instrument and it is the one checked in, but it reports the
distance between the *drawn steel*, and STYLE.md §5 requires the blade to be a
sliver with a soft glow — so two blades whose axes genuinely cross can measure
several pixels apart wherever the taper and the warm clash core eat the cool-bright
pixels between them. The 2% threshold is defensible; a reviewer applying it should
read a value between 2% and 4% as "check the geometry too" rather than as a fail,
and `Rehearsal` is now the way to check.

---

## Commands, so the next pass does not have to reconstruct them

```
./gw capture  -Pscene=duel-parry       -Pout=out/captures/s4-p2-parry-contact \
              -Pframes=24 -Pcols=6 -Pstart=1.42 -Pstep=0.0167 -Pw=960 -Ph=720
./gw capture  -Pscene=duel-parry-debug -Pout=out/captures/s4-p2-parry-contact-debug \
              -Pframes=24 -Pcols=6 -Pstart=1.42 -Pstep=0.0167 -Pw=960 -Ph=720
./gw capture  -Pscene=rig-bindpose     -Pout=out/captures/s4-p2-null-static \
              -Pframes=24 -Pcols=6 -Pstart=0.0 -Pstep=0.0167 -Pw=960 -Ph=720

./gw analyse  -Pargs="blades   out/captures/s4-p2-parry-contact --max 0.02"
./gw analyse  -Pargs="corridor out/captures/s4-p2-parry-contact --min 0.06"
./gw analyse  -Pargs="regions  out/captures/s4-p2-parry-contact --figure left"
./gw test --tests '*RehearsalTest*'
./gw test --tests '*DuellistsTest*'
```
