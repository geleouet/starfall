# System 5 — standing debt

**Status: pass 2 shipped, answering the pass-1 review's FAIL. Not self-declared a pass.**
A reviewer must never grade work it produced, and this document grades nothing; it records
what was built, what was measured, what was corrected in pass 1's own account, and what is
still owed.

Every capture quoted is `s5-p2-*`, shot at `harness=f0ad18994eec` — the hash that produced
every `s4-*`, `rev-p*` and `s5-p1-*` capture, so comparisons back to pass 1 and to System 4
are in scope per §11.2b(d). Every number about the interface is a **difference against a
`-bare` control shot at the same harness in the same window**, and is printed beside the
rectangle it was taken through (§11.3). Every pixel number was taken with an independently
written NumPy/PIL reader using the Rec. 709 weights `Frame.java` declares (§11.2b(c)); where
a pass-1 figure is quoted for comparison it was **re-measured through that same reader on the
pass-1 captures**, so the two columns are one instrument.

Suite: **404 tests green, 2 skipped** — both `DuellistValueTest`'s, which are System 4's and
are fixed upstream by `tools/check-progress.mjs`.

---

## 0. What the review failed this pass on, and what happened to each

| # | The review's finding | Status |
|---|---|---|
| 1 | Guard A's stated scope is false; a bordered HUD panel passes the whole suite | **Fixed, and the scope is now itself tested.** §1 |
| 2 | `Glyph.of(STEP,-1)` is vertex-identical to `Glyph.of(BACK_STEP,+1)`; nothing watches distinctness | **Fixed.** 16 of 18 shapes → 18 of 18; closest cross-tile pair 0.000 → **0.733**. §2 |
| 3 | The Parry glyph draws the collision its own rubric forbids | **Redrawn as a deflection over a span.** §3 |
| 4 | The graded frame is a diagram on a nice background; no fog, no substrate | **Partly.** Fog delivered and calibrated on the corpus; substrate corrected rather than delivered; the framing law was not opened. §4 |
| 5 | The health row is the loudest mark in the margin | **Fixed:** +99.6 → **+72.8**, against the stanza's +94.7. §5.1 |
| 6 | Five claims in this document do not reproduce | **All five corrected, and a sixth of the review's own does not reproduce.** §6 |

The three design challenges were upheld and are implemented: the base-anchored column is
unchanged and its wording is now in `combat-design.md` §3; health is a row and `STYLE.md` §8
says so; and the cooldown obligation — *countable at every shipped resolution or not a count*
— is discharged in §5.2 and recorded in `combat-design.md` §3d.3.

---

## 1. The hard-edge guard, which was the FAIL

### 1.1 What pass 1 asserted, and what it claimed

```java
assertTrue(a == 0f || b == 0f || c == 0f, …)
```

> *"every triangle a brush emits has at least one vertex at exactly zero alpha… **A panel, a
> bar, a border, a rounded rectangle or an icon in a frame all fail it on the first triangle,
> whatever they are shaded with.**"*

The sentence was false and the review proved it in the only way it could be proved: by
building the forbidden thing. A triangle `(0, α, α)` satisfies the assertion and prints a hard
edge along its `α–α` side.

### 1.2 Three assertions now, and each says only what it tests

**`noTriangleInTheInterfaceIsAFlatFill`** — pass 1's assertion, kept, renamed, and with the
false sentence deleted. It is a true and useful invariant of `Brush` and it is **not** a
hard-edge certificate.

**`noMarkPutsInkOnASilhouetteEdge`** — the property the old name promised. *No edge of the
emitted mesh that belongs to a single triangle carries ink at both of its ends.* Interior
edges may be as dark as they like; it is the **outline** that must be on paper. That is what
actually makes `Brush` edgeless: a stroke's two rims sit on zero either side of the spine, so
every inked edge it emits is shared with the triangle beside it, and a wash is a fan whose rim
is on zero. Swept over all three bouts at 60 Hz.

**`noMarkPrintsAnEdgeInTheRasterItWouldDraw`** — the same rule measured where an edge exists.
`Raster` (test scope) composites the emitted triangles at 960×720 with the renderer's own
premultiplied over, and the statistic is the review's own: **the steepest single-pixel step as
a fraction of the interface's own amplitude.** Ceiling **0.34**, derived rather than chosen —
see §1.3. Delivered worst over all three bouts at 20 Hz: **0.266 / 0.225 / 0.244** (`KNIFE`, `FOLD`, `APPROACH`).

Two things about the instrument, both found by it manufacturing the artefact it looks for:

- **The fill rule matters.** Without the top-left rule a pixel exactly on a shared edge is
  composited twice, and two coats of a 0.30 wash print a 0.51 line one pixel wide.
- **The edge functions have to be in `double`.** A reach pool at the intimate framing is a fan
  of 28 slivers 127 px long and 15 px tall; in `float` the tests on adjacent slivers disagree
  by an ulp often enough to leave **single-pixel holes**, and one such hole read 0.15 between
  neighbours at 0.39 and was the steepest "edge" in the interface.

### 1.3 The adversarial instance, per §11.2b(f) — **and it partly succeeded**

*"Try to build the thing the guard forbids while satisfying it. If you succeed, the guard's
scope is the finding and the name is a lie."* Four attempts, all measured through the raster
the guard uses, box `x0.60..1.00 y0.60..0.90` in frame heights:

| attempt | inked silhouette edges | steepest 1-px step / peak | caught by |
|---|---|---|---|
| 1. the reviewer's panel, as written | **2** | **1.000** | both |
| 2. the same panel, **every triangle drawn twice** | **0** | **1.000** | raster only |
| 3a. filled panel, 1 px feather | 0 | 0.500 | raster |
| 3b. filled panel, 2 px feather | 0 | 0.500 | raster |
| 3c. filled panel, 3 px feather | 0 | 0.333 | **neither** (0.333 vs a 0.34 ceiling) |
| 3d. filled panel, 5 px feather | 0 | **0.200** | **neither** |
| 4. a panel hatched out of 90 legal `Brush` strokes | 0 | 0.360 | raster |

**Attempt 2 defeats the topology guard outright** and is checked in inside
`theForbiddenThingIsCaughtByTheGuardsThatForbidIt`, so that deleting the raster guard can never
look safe. Drawing every triangle twice makes every boundary edge appear an even number of
times; the mesh looks closed and the picture is unchanged.

**Attempts 3c and 3d succeed against both guards, and that is the finding.** A filled
rectangle whose four sides fade to zero over four or more pixels passes everything here. So
the scope of these two guards, stated exactly:

> They forbid a **printed boundary** — any construction, however meshed, that delivers more
> than a third of its amplitude in a single pixel. They do **not** forbid a rectangular
> *composition* whose edges are feathered. `STYLE.md` §10's "UI panels, bars, boxes, borders"
> is a ban on a **shape**, and nothing in this project tests for a shape.

Pass 1's device 3 conflated the two and that conflation is what made its sentence false.
Catching a soft-edged panel needs a criterion about form, not about gradient, and **nobody has
one. Unpaid, with the exhibit above written down so the next pass does not have to rediscover
it.**

The ceiling of 0.34 is a third of what a hard edge reads through the same instrument (1.000,
measured, not assumed), which is the same as saying *a mark must take at least three pixels to
arrive*. Attempt 3c lands at 0.333 — one part in a hundred inside the ceiling — so the guard's
boundary is exactly where its statement puts it and not somewhere convenient.

### 1.4 And in delivered pixels, which is where §3's rule lives

`D = luminance(live) − luminance(bare)`, largest single-pixel `max|∂D|` in either axis, as a
fraction of that capture's peak `|D|`, over **every frame of every capture that has a
control**, ignoring an 8 px frame border for the reason the pass-1 review gave (a pool the
viewport cuts is not a printed boundary).

| capture | frames | worst step / amplitude | where |
|---|---|---|---|
| `s5-p2-fold-plan` | 6 | **0.244** | frame 0, step 22.9, amp 93.7 |
| `s5-p2-fold-strike` | 12 | **0.223** | frame 0, step 20.7, amp 92.8 |
| `s5-p2-fold-pushin` (@0.0167) | 48 | **0.263** | frame 32, step 15.8, amp 60.0 |
| `s5-p2-fold-replan` | 5 | 0.250 | frame 3, step 23.8, amp 95.0 |
| `s5-p2-fold-empty` | 4 | 0.286 | frame 0, step 23.6, amp 82.7 |
| `s5-p2-knife-plan` | 4 | 0.277 | frame 0, step 26.7, amp 96.5 |
| `s5-p2-approach-plan` | 4 | 0.250 | frame 2, step 23.8, amp 95.2 |
| `s5-p2-fold-plan` **at 720×540** | 6 | 0.296 | frame 0, step 28.1, amp 94.9 |
| `s5-p2-fold-replan` **at 720×540** | 5 | **0.334** | frame 4, step 30.7, amp 91.9 |
| `s5-p2-fold-bleed` (@0.0167) | 36 | **0.432** | frame 35, step 15.4, amp **35.7** |

Controls in the same pixels, on the **bare** frame (`fold-plan` frame 3):

| control region | worst 1-px step |
|---|---|
| blades and figures, `x360..539 y470..519` | 68.4 / 70.5 |
| left figure, `x220..289 y420..519` | **114.5 / 115.5** |

**The pass-1 result is preserved and slightly improved on the graded captures** — 0.244 and
0.223 against pass 1's 0.249 and 0.242, re-measured through my reader on the pass-1 captures
as 0.249 and 0.242 exactly. The interface's steepest transition takes four pixels; the figure
beside it steps five times harder.

**Two rows are worse than pass 1's and both are new measurements rather than regressions.**

- **720×540 costs softness.** Every mark is 25% narrower in pixels at 540 rows, so every rim is
  25% steeper: 0.244 → 0.296 on the graded frame and 0.250 → 0.334 on the replan. That is the
  price of a shipped second resolution and it is now on the record instead of unmeasured.
- **`fold-bleed` at 0.432 is a normalisation artefact and the absolute number says so.** That
  window sits at the intimate framing where the interface has given up most of its presence,
  so the frame's own amplitude is 35.7 where the planning frame's is 93.7. The *absolute*
  step, 15.4 luminance levels, is the **smallest** of any capture in the table. Against the
  amplitude the interface reaches at its strongest it is **0.164**. The checked-in raster guard
  normalises against the bout's strongest amplitude for exactly this reason and says so in its
  own docstring; this table keeps the review's per-capture form so the two are comparable.

**The blade controls fell** — 81.6/129.4 in pass 1 to 68.4/114.5 here — because the distance
haze of §4.1 softens the figures at the planning framing, which is what it is for. The ratio
that matters is unchanged in kind: the interface's steepest step is **5.0×** softer than the
softest blade in the same frame (pass 1: 5.2×).

---

## 2. Two tiles, two pictures

`Glyph.of(STEP, -1)` was **vertex-identical** to `Glyph.of(BACK_STEP, +1)`: 16 distinct shapes
for 18 `(tile, facing)` pairs, and the two that merged were forward and back on a lane.

**`BACK_STEP` is re-authored** rather than mirrored. Pass 1's note — *"retreat has no verb, so
the mark is the Step's own gesture read the other way"* — is the bug stated as a design
principle. The retreat now has its own asymmetry, taken from what a retreating body does: the
front foot does not lift and land, it **drags**. Three marks rather than two — the heel
travelling back and settling, the weight still standing where it was, and a dry scuff under
the travel. Nothing in the Step has a hook and nothing in it is dry.

**`noTwoTilesAreTheSamePicture`** guards it. Each mark is rasterised alone at the shipped 71 px
cartouche and compared as a *picture*: `Σ|a−b| / Σ max(a,b)`, which is 0 for identical marks and
1 for marks sharing no paper. Floor 0.20.

| | pass 1 | pass 2 |
|---|---|---|
| closest **cross-tile** pair, of 144 comparisons | **0.000** (`STEP-` vs `BACK_STEP+`) | **0.733** (`STEP+` vs `BACK_STEP+`) |

**Where this is narrower than the review asked, and the measurement that made it narrower.**
The review asked for distinctness across all 18 pairs. Measured, the same-tile mirror distances
are `CUT 0.856, THRUST 0.792, PARRY 0.847, SWEEP 0.154, DRAW 0.906, STEP 0.943, BACK_STEP 0.890,
TURN 0.793, FEINT 1.000`. **Sweep is the exception and it is correct**: *"one continuous arc
through two bodies"* hits the tile in front and the tile behind, its mark is a near-symmetric
230° arc, and requiring it to differ under mirroring would require the alphabet to lie about a
symmetric gesture. So the assertion is on the property the collision actually broke — two
*different* tiles never draw one picture, at any pair of facings — and the same-tile numbers
are recorded here instead of asserted.

A note the distinctness measurement produced on its own: mean absolute difference over the
cartouche box, the obvious statistic, is useless here — a Feint and its own mirror, which are
opposite diagonals, measured **0.0037** apart on it, because a sparse mark resembles every
other sparse mark when the denominator is the box.

---

## 3. The Parry, which is the signature beat

`combat-design.md` §2.2 asks for *"a deflection curve rather than a collision"*, and §3d's
closing note spends a paragraph on why reading the meeting as a point *"collapses the
deflection to a point, which is the collision the whole document exists to forbid."* Pass 1
drew two straight lines meeting at a sharp apex.

The mark is now two Catmull-Rom curves, and what settles its shape is `STYLE.md` §7.1's own
sentence about the beat: *"contact is a span rather than an instant."* So the meeting is a
**span** and the redirection is a **curve** —

- the defending blade is a long shallow arc that rolls as it takes the weight and never
  changes direction;
- the attack comes down steeply from above, runs alongside it across the middle third of the
  mark at a small fixed gap, and leaves climbing.

There is no vertex anywhere in it. Delivered at the shipped cartouche it reads as two blades
sliding past each other and parting, which is the beat.

**The cold read was protected by construction and then checked.** Of the five glyphs the
pass-1 reviewer read correctly cold, only Parry changed; `CUT`, `THRUST`, `SWEEP` and `STEP`
are untouched, and `BACK_STEP` — which did change — is not one of the five and appears in the
graded stanza only in Act III. The new Parry was rendered at the shipped 71 px side and at the
delivered planning framing and inspected before it was accepted. **Whether it still reads as
*Parry* to someone who has not been told is a human judgement and only a reviewer can make
it**; §11.2b(g)'s word for anything else here is "nobody has measured this".

`DRAW` and `TURN` were also touched, for a measured reason rather than a design one: at their
authored widths the stroke's own half-width was wider than the radius of its hook, so the
ribbon folded across itself and the two coats printed a bright core with a step at its
boundary — **0.528 of amplitude** through the raster, the steepest thing the layer drew. Both
hooks are opened and both ribbons narrowed. A brush lifts as it turns.

---

## 4. The composition

### 4.1 Fog, and it is calibrated on the corpus rather than by eye

`STYLE.md` §9 asks the planning framing for *"the full lane readable, figures small, heavy fog,
Family C mood"*, and pass 1 delivered the first two only. A single dial now attenuates the
figures with the **framing width** — `Readout.haze`, full at 12.5 tiles, so a five-tile knife
fight lands at 0.35 of it and a fifteen-tile approach is saturated. It is a function of the
framing width alone, so it inherits `Schedule.cameraIsContinuous` and cannot step; the
recession guard now sweeps it at 60 Hz beside the interface's own recession.

**It is an alpha term first and a tint second, and that ordering is a measurement.** Mixing a
near-black figure toward `Fog #D6D2CE` on a *dusk* stage does not dissolve it, it **inverts**
it: at a mix of 0.45 the ink passes through the sky's own value and comes out brighter than the
sky. What dissolves a dark figure into a dark sky is letting the sky through it.

**The strength was set by measuring the corpus, and the first setting was twice too strong.**
Reference image 6, the Family C image the planning framing is quoting:

| | contrast (sky − darkest 3%) | ratio |
|---|---|---|
| foreground figure, top panel `x420..660 y200..520` vs sky `x100..380 y30..150` | 180.2 | 23.62× |
| background figure, top panel `x200..295 y180..395` vs local `x100..190 y200..320` | 166.0 | 5.69× |
| foreground figure, bottom panel `x260..600 y700..1050` | 147.6 | 19.61× |
| background figure, bottom panel `x95..190 y800..1010` | 97.7 | 3.29× |

So a Family C background figure keeps **66–92% of the foreground's absolute contrast** while
its value **ratio collapses to a fifth**. Those are compatible there because that ground is
luminance 188: lifting the ink from 8 to 35 costs a fifth of the ratio and a twelfth of the
contrast. **On a dusk stage the ground is luminance 59 and they are not compatible** — the
ratio collapse the corpus shows would erase the figure. The absolute retention is what is
matched; the ratio is recorded as a thing a dark ground cannot deliver, which is the same
finding `STYLE.md` §2.2's own note about the value floor already carries in the other
direction.

Delivered, hero box `x225..270 y429..514` against local sky `x300..699 y380..420` on the bare
control, and execution box `x340..470 y300..660` against `x700..940 y120..300`:

| | contrast at the planning framing | at the execution framing | wide keeps |
|---|---|---|---|
| pass 1 | 33.4 (ratio 2.28×) | 33.8 (2.27×) | **99%** |
| pass 2 | **21.0** (ratio 1.55×) | 31.9 (2.11×) | **66%** |

66% is the bottom of the corpus's own band. Going wide now costs value the way distance does.

### 4.1a A term that multiplies out to the identity is not free, and this cost a day to find

`u_haze` is zero on every scene but the lane. Written straight through, the two lines it
drives are the identity at zero — `mix(x, y, 0.0)` is `x` and `alpha * (1.0 - 0.0)` is `alpha`,
exactly, in IEEE arithmetic. **They are still not free.** Shot against the same scene with the
lines deleted, `duel-parry` differed by **1,180 pixels over six frames**; the null control for
that comparison — the same scene shot twice at one commit, which §11.2b(g) requires and which
this pass ran before believing anything — is **0**. The driver recompiles the expression tree
around the dead code.

Behind a branch on the uniform, the same comparison is **0 of 691,200 on every frame**. So
System 4's captures are bit-identical across this change, and they would not have been if the
obvious form of the code had shipped. The general shape of it is worth more than the instance:
**"this term is the identity when the flag is off" is a claim about arithmetic and not about a
compiler**, and on this project it is exactly the kind of sentence §11.2b(g) says to read as
*nobody has measured this*.

### 4.2 The substrate: corrected, not delivered — and the review's own number does not survive

`combat-design.md` §3 requires the interface to sit on §3b.3's aged paper. The review measured
the planning sky's 3 px high-pass sd against **reference image 1** — a Family A *cream sheet* —
and reported 16× less surface.

Re-measured through my own reader (absolute values differ from the review's because the kernels
differ; the ratios are the claim):

| region | high-pass sd |
|---|---|
| S5 planning sky (bare), `x300..699 y120..319` | **0.476** |
| **reference image 3 sky**, `x80..699 y80..239` | **0.481** |
| reference image 1 paper, `x60..699 y60..299` | 6.524 |

**Against the sky of the family this stage is quoting, the capture matches to 1%.** The 13×
gap is between a dusk sky and a Family A paper ground, which are different surfaces in
different families. That correction is now in `combat-design.md` §3 as the fourth
contradiction, filed rather than resolved quietly — which is the half of this the pass owed and
did not file.

The other half was real. The margin's marks read as *"pale scratches floating in the sky"*, so
both margins now carry **foxing**: irregular stains, some cool and some ochre, at a scale far
above the pixel grid so §3b.1's anti-shimmer rule is untouched. Measured through a mark-free
band of the left margin, `x140..176 y60..400`, live over bare:

| high-pass kernel | pass 2 live / bare | pass 1 live / bare |
|---|---|---|
| 3 px | 0.452 / 0.440 = **1.03×** | 0.440 / 0.440 = 1.00× |
| 9 px | 0.531 / 0.475 = **1.12×** | 1.00× |
| 17 px | 0.746 / 0.487 = **1.53×** | 1.00× |

**It is stain structure and not tooth, and the numbers say so plainly.** A wash primitive can
carry a stain; a 3 px tooth in the margin needs a noise field in the fragment shader, which is
`PaperBackground`'s to give and is **unpaid**.

### 4.3 The hand, and the right third of the graded frame

Pass 1's bouts held exactly `InkStanza.CAPACITY` tiles, so filling the stanza emptied the hand
and the review measured the graded shot's right third at **1.016×** its control. Every bout
now holds **seven** tiles: the same five, banked, in the same order — because they are the
stanza the reviewer read cold and nothing was allowed to disturb them — plus two that stay on
the sheet with their charges on them.

Box `x790..919 y30..359` (the review's own) and `x640..919 y28..500` (the whole hand, which the
larger glyph and pitch now reach):

| box | pass 1 | pass 2 |
|---|---|---|
| `x790..919 y30..359` mean ratio | 1.016× | 1.020× |
| `x640..919 y28..500` mean ratio | 1.005× | 1.010× |
| `x640..919 y28..500` **peak lift** | **+21.3** | **+77.8** at (865,404) |
| `x640..919 y28..500` ink px (lift > 4) | 4764 | 6685 |

**The mean ratio barely moves and that is the honest number**: the box is mostly sky and a
ratio over it measures the sky. What changed is that the right third now carries a mark at
**3.65× the strength** it did, because there is a live tile in it rather than five ghosts.

The two extra tiles differ by lane and between the three bouts they cover **Back-step, Feint,
Draw and Turn** — the four letters of the alphabet the review found had never been through the
renderer — plus one enchanted tile in each of `FOLD` and `APPROACH`, so an enchantment mark is
drawn for the first time as well.

### 4.4 What the composition statistic says, including the part that got worse

`>8` luminance from the pixel's own row median, the review's statistic:

| | pass 1 | pass 2 |
|---|---|---|
| bare **planning** frame that is anything at all | 4.84% | **4.76%** |
| bare **execution** frame | 16.61% | **14.71%** |
| the interface | 2.09% | **2.16%** |
| interface / world | 0.43 | **0.45** |

**This got slightly worse and the reason is the fog.** Attenuating the figures is exactly what
§9 asked for and it removes pixels from a statistic that counts *deviation*, so the two
requirements pull against each other: the frame is more atmospheric and, by this measure, less
featured. The interface's own share rose 0.07 points, from the foxing and the larger hand.

**§11.0's matched-scale part count was not re-run, and the fog makes it worse rather than
better.** The figures are System 4's, unchanged; the haze removes contrast from them at the
planning framing by design. The review declined to fail System 5 for the count and this pass
has no answer to it either. **Unpaid, and now with a term in it that this pass added.**

### 4.5 The framing law was measured and deliberately not opened

The review's second composition bullet asks for a framing law that keeps the figure near the
~180 px it reaches on the five-tile lane, *"even at the cost of not showing the whole lane."*
This pass did not do it, for two reasons and both are numbers rather than reluctance.

- **Capping `Stage.planning()` drops a combatant out of the planning shot.** A cap wide enough
  to leave System 4's 5- and 7-tile lanes untouched is 8.5 tiles; on the 11-tile lane the
  bodies open at tiles 2, 6 and 10 and a shot 8.5 tiles wide centred on the lane's middle holds
  0.75 to 9.25. The enemy at 10 is off-frame in the one shot the player plans in.
- **Anything below that cap changes System 4's schedules**, and the pass-1 review's own
  protected result is three byte-identical schedule fingerprints showing that System 5 did not
  disturb them.

The clean fix is the one `Director.LANE_SPREAD` already names in its own docstring — *"either
`TILE_WIDTH` rises against `FIGURE_HEIGHT`, or `BODY_HALF` grows to the width the rig actually
has and the lane spacing follows it"* — plus an **action-centred** planning framing, which
needs `Standing` inside `Stage.planning()`. That is a `Stage` change and it is the third pass
in a row to name it. **Unpaid, and it is the largest single thing between this frame and §0.**

---

## 5. The two free items, and the counted marks

### 5.1 The margin's loudest mark is now the one the player is composing with

`health row x36..254 y10..44` against `stanza top x62..132 y53..123`, live over bare,
`s5-p2-fold-plan` frame 3:

| | pass 1 | pass 2 |
|---|---|---|
| health row, peak lift | **+99.6** | **+72.8** at (86,26) |
| freshest mark in the stanza, peak lift | +94.8 | **+94.7** at (88,87) |

Guarded, not asserted in prose: `theStanzaOutReadsTheHealthRow` measures both peaks on the
sheet the renderer would draw.

### 5.2 A counted mark is countable at every shipped resolution

The review refused the debt's prescription — *"a rendering constraint at one resolution is not
a reason to change a mechanic"* — and left a narrower obligation entirely inside this layer:
**a mark that is counted must be countable at every resolution the game ships, or stop being a
count.** Two runs are counts: a tile's charges, and the Night Pilgrim's health.

Both were wrong. A charge run's pitch was 1.7 tick widths and its ghosts were drawn at
`dryness` 0.48, so the dry-brush breakup ate whole marks. Measured over the whole state space
before the fix, a cooldown of 3 at 1 charge read **2**, a cooldown of 4 at 2 charges read
**3** — the misread the review names as worse than an omission.

Now: pitch is **derived** from the mark's own width (2.6 widths, so there is more paper between
two marks than either occupies), and a ghost is separated from a live charge by **value and
hue** — pale paper at 0.30 against ochre at 0.74 — rather than by being broken, because a mark
the noise can eat cannot be part of a count. Health is under the same rule.

`everyCountedMarkIsCountableAtEveryShippedResolution` asserts it over every cooldown 0–8 at
every charge count inside it, and every tally 0–max at three maxima, at **both** shipped
heights.

**And it is proved in delivered pixels at a second resolution**, which is what the review asked
for. `s5-p2-fold-replan` frames 2 and 4, hand rows at `HAND_TOP_Y − i·HAND_PITCH`, runs read
across the densest row of each band, live minus bare:

| hand row | tile | cooldown | runs at 960×720 | runs at 720×540 |
|---|---|---|---|---|
| 0, 1 | CUT, STEP | 0 | none drawn | none drawn |
| 2 | SWEEP | 3 | **3** | **3** |
| 3 | THRUST | 3 | **3** | **3** |
| 4 | **PARRY** | **4** | **4** | **4** |
| 5 | BACK_STEP | 1 | **1** | **1** |
| 6 | FEINT + MARKING | 5 | **5** | **5** |

The row the review convicted — a Parry at cooldown 4 reading "three" — now reads four, at both
resolutions, on both frames.

---

## 6. Claims that do not reproduce

### 6.1 The five the review found in this document — all corrected

1. **Guard A's stated scope.** False, as the review proved. §1.
2. **§4.4's *"'wide to plan' never happened"*.** Overstated by half. Reverted, each bout has
   **two** qualifying planning gaps and only the **first** is missed; the second reaches the
   full planning framing in all three bouts. The defect costs one planning gap of two per
   score, not all of them. It also moved `KNIFE`'s duration, 20.1462 → 21.3722 s.
3. **§3.2's execution diff range.** The minimum reproduced; the maximum did not. The review's
   reader gives **49,030 px (7.093%)** under "any channel differs" and 45,153 under "luminance
   differs by ≥1"; no threshold yields 46,999. And this document contradicted itself on the
   planning minimum — §1.6 said 33,998 and §3.2 said 34,128 for the same quantity. §1.6's low
   end was right. **Both are superseded by §7 below, measured afresh on the pass-2 captures.**
4. **§5's *"asserted to… survive mirroring"*.** `everyTileTypeHasAMark` asserts that mirroring
   preserves the **stroke count**, and nothing asserted that a mark survives mirroring *as
   itself*. For `STEP`/`BACK_STEP` it provably did not. §2.
5. **§4.5's tick count quoted without a control**, against this document's own §7 instruction.
   `s5-p1-fold-replan` had no `-bare` sibling. Every capture in §8 below has one, including
   `s5-p2-fold-replan` and both 720×540 captures.

### 6.2 And one of the review's own does not reproduce as framed

**The paper-substrate finding.** *"Sky high-pass sd is 0.813 against reference image 1's
12.853… 16× less surface than the corpus's paper ground."* The two regions are a **dusk sky**
and a **Family A cream sheet**, in different families. Against the sky of reference image 3 —
the image this stage's own template comes from, and which the review measured in the same table
— the capture matches to **1%** (0.476 against 0.481 through my reader; 0.813 against 1.144
through the review's, which is 1.4×, not 16×). The conclusion "there is no paper in the frame"
is right; the multiplier is a family error, and the correct statement is that a Family B dusk
stage cannot carry §3b.3's paper at all. §4.2.

**Claims of the review's that reproduce exactly**, recorded because a reply that only lists
disagreements is not a measurement: the lane band 1.109× and +84.8 at (172,535) — mine 1.110×
and +84.8 at (172,535); the whole §1.1 wetness table to the pixel; the health row's 2.79×; the
bit-reproducibility at 0 differing pixels; the draw-order result at 0.00 under body ink; the
hand column at 1.016×; and the 0.242/0.249 hard-edge figures, all re-measured on the pass-1
captures through my own reader.

---

## 7. Every protected result, re-measured

| result | pass 1 | pass 2 |
|---|---|---|
| **Hard edge**, `fold-plan` / `fold-strike` | 0.249 / 0.242 | **0.244 / 0.223** |
| — against the softest blade in the same frame | 81.6 | 68.4 (haze), ratio 5.0× vs 5.2× |
| **The column, top to base** (peak lift, box `x62..132`) | 94.8 / 85.6 / 78.8 / 72.6 / 68.8 | **94.7 / 86.7 / 78.8 / 72.6 / 68.8** |
| — as a ratio to its own ground | 2.643 / 2.481 / 2.301 / 2.239 / 2.157 | **2.648 / 2.501 / 2.301 / 2.217 / 2.157** |
| — ink px (lift > 4), which is the size half of the gradient | 2320/1537/832/536/401 | **2175/1292/744/519/493** |
| **Lane band** `x0..959 y505..559` | 1.109×, +84.8 at (172,535) | **1.110×, +84.8 at (172,535)** |
| **Draw order**, `fold-strike` frame 4, band `y560..699` | 0.00 under 7,694 px of body ink; 4.09 beside | **0.00 under 6,977 px; 2.92 beside** |
| **Bit-reproducibility** (`-repro`, 6 + 12 frames) | 0 differing px | **0 differing px** |
| **Recession**, mean \|delta\| over the changed set | 15.76 → 9.96 | **10.35 → 7.02**; share 6.96% → 8.35% |
| **Empty state**, five slot impressions | 20.9/16.8/14.1/12.0/35.3 | **21.2/18.3/14.3/14.9/34.0** |
| **Camera fix** | S4 fingerprints byte-identical | unchanged — `Stage` and `Scheduler` are untouched, and `duel-parry` is **bit-identical**, 0 px over 6 frames, against a null control of 0. §4.1a |

Two of these moved and both are consequences of changes made on purpose:

- **`ink px` at reading position 5 rose from 401 to 493**, which flattens the *area* half of the
  LIFO gradient from 5.8× to 4.4×. The review's caution stands: the perceived gradient is
  partly size and area is alphabet-dependent. The **peak** statistic, which is the one the
  guard is on, is unchanged to the tenth.
- **Mean |delta| fell across the board** (15.76 → 10.35 at the planning framing) because the
  interface's loudest elements — health and the charge ticks — were deliberately quietened, and
  because the changed-pixel *set* grew by 40% with the foxing, which adds many very faint
  pixels to the denominator. The recession's *shape* — thinner as the camera closes — is
  unchanged and is what the claim is about.

---

## 8. The captures, and how to reproduce them

Every live capture has a `-bare` sibling in the identical window, per this document's own §7
rule, which pass 1 broke once.

```
./gw capture -Pscene=lane-fold      -Pout=out/captures/s5-p2-fold-plan     -Pframes=6  -Pcols=3 -Pstart=0.4  -Pstep=0.5    -Pw=960 -Ph=720
./gw capture -Pscene=lane-fold-bare -Pout=out/captures/s5-p2-fold-plan-bare        (identical window)
./gw capture -Pscene=lane-fold      -Pout=out/captures/s5-p2-fold-plan-repro       (identical window)
./gw capture -Pscene=lane-fold      -Pout=out/captures/s5-p2-fold-strike   -Pframes=12 -Pcols=4 -Pstart=3.3  -Pstep=0.42   -Pw=960 -Ph=720
./gw capture -Pscene=lane-fold-bare -Pout=out/captures/s5-p2-fold-strike-bare      (identical window)
./gw capture -Pscene=lane-fold      -Pout=out/captures/s5-p2-fold-strike-repro     (identical window)
./gw capture -Pscene=lane-fold      -Pout=out/captures/s5-p2-fold-pushin   -Pframes=48 -Pcols=8 -Pstart=2.95 -Pstep=0.0167 -Pw=960 -Ph=720
./gw capture -Pscene=lane-fold-bare -Pout=out/captures/s5-p2-fold-pushin-bare      (identical window)
./gw capture -Pscene=lane-fold      -Pout=out/captures/s5-p2-fold-bleed    -Pframes=36 -Pcols=6 -Pstart=8.60 -Pstep=0.0167 -Pw=960 -Ph=720
./gw capture -Pscene=lane-fold-bare -Pout=out/captures/s5-p2-fold-bleed-bare       (identical window)
./gw capture -Pscene=lane-fold      -Pout=out/captures/s5-p2-fold-empty    -Pframes=4  -Pcols=2 -Pstart=11.3 -Pstep=0.35   -Pw=960 -Ph=720
./gw capture -Pscene=lane-fold-bare -Pout=out/captures/s5-p2-fold-empty-bare       (identical window)
./gw capture -Pscene=lane-fold      -Pout=out/captures/s5-p2-fold-replan   -Pframes=5  -Pcols=5 -Pstart=15.8 -Pstep=0.35   -Pw=960 -Ph=720
./gw capture -Pscene=lane-fold-bare -Pout=out/captures/s5-p2-fold-replan-bare      (identical window)
./gw capture -Pscene=lane-fold      -Pout=out/captures/s5-p2-fold-score    -Pframes=44 -Pcols=4 -Pstart=0.4  -Pstep=0.5    -Pw=960 -Ph=720
./gw capture -Pscene=lane-knife     -Pout=out/captures/s5-p2-knife-plan    -Pframes=4  -Pcols=2 -Pstart=0.6  -Pstep=0.7    -Pw=960 -Ph=720
./gw capture -Pscene=lane-knife-bare    -Pout=out/captures/s5-p2-knife-plan-bare    (identical window)
./gw capture -Pscene=lane-approach      -Pout=out/captures/s5-p2-approach-plan      (same shape as knife)
./gw capture -Pscene=lane-approach-bare -Pout=out/captures/s5-p2-approach-plan-bare (identical window)

# the second shipped resolution, which is where the countability claim is proved
./gw capture -Pscene=lane-fold      -Pout=out/captures/s5-p2-fold-plan-540   -Pframes=6 -Pcols=3 -Pstart=0.4  -Pstep=0.5  -Pw=720 -Ph=540
./gw capture -Pscene=lane-fold-bare -Pout=out/captures/s5-p2-fold-plan-540-bare     (identical window)
./gw capture -Pscene=lane-fold      -Pout=out/captures/s5-p2-fold-replan-540 -Pframes=5 -Pcols=5 -Pstart=15.8 -Pstep=0.35 -Pw=720 -Ph=540
./gw capture -Pscene=lane-fold-bare -Pout=out/captures/s5-p2-fold-replan-540-bare   (identical window)

./gw test --tests '*StanzaColumnTest*'
./gw test --tests '*InterfaceInkTest*'
```

**Quote a `-bare` control beside every number about the interface.** Without it a figure of
"the mark reads at luminance 133" is a measurement of the dusk sky, which varies by 45 levels
across the column's own height.

The graded scene is **`lane-fold`**. `lane-knife` and `lane-approach` exist to show that the
push-in and the haze derive from lane length and framing width and that nothing has been tuned
through them.

---

## 9. What this pass did not do, with the number beside it

Carried forward from pass 1 where still true, and new items marked.

- **Enemy hit points are still not drawn at all.** The largest omission; *"will this phrase
  kill it"* is the combo question and `combat-design.md` §1.5 makes combos the economy's
  engine. The idea worth trying is still that a wounded Charted Shadow's own **ink runs
  thinner**, which is a `Figure`/`InkMaterial` change. **Unpaid.**
- **A soft-edged panel still passes every guard here.** §1.3, with the exact construction and
  its number (0.200 at a 5 px feather). Catching it needs a criterion about *shape*. **New,
  and it is the honest cost of fixing the FAIL.** *(§11.2b(f) asks for the attempt; the attempt
  succeeded, so the scope is the finding.)*
- **The framing law is untouched.** §4.5, with the two numbers that decided it. **Unpaid, and
  it is the largest thing between this frame and §0.**
- **§11.0's matched-scale part count was not run**, and the fog moves it the wrong way. §4.4.
- **A 3 px paper tooth in the margin is not delivered**, only stain structure at 1.53× over
  17 px. §4.2. It is a `PaperBackground` question. **Unpaid.**
- **The Feint is at the edge of legibility in the hand.** Its mark is *"motion with no
  contact"* drawn at `dryness` 0.66, which is the tile, and delivered at the 59 px hand
  cartouche it is very nearly nothing. That may be right and it may be a tile a player cannot
  find. **Nobody has measured whether it can be identified.** *(New.)*
- **The vermillion budget doubled**, from 0.141% to **0.278%** of the frame at the planning
  framing (1,923 px, strongest live `RGB(154,64,61)` against bare `RGB(63,63,75)`) and 0.391% to
  0.517% at the execution framing. The cause is the seed of wash the Strikethrough's arrival is
  now drawn from, which exists because the bare wick printed the steepest step in the layer
  early in its bleed. Still *"a few small marks per frame"* by a wide margin and still tied to
  threatened tiles by guard B — but it is twice what it was and that is worth a reviewer's
  attention.
- **The Strikethrough's arrival is now captured at 60 Hz** (`s5-p2-fold-bleed`, 36 frames at
  0.0167 s) and **nobody has read it as an arrival**. The frames exist; no statistic was taken
  through them beyond §1.4's hard-edge sweep, so `STYLE.md` §8's *"elements arrive by pigment
  spreading into place (0.4–0.7 s)"* remains **unverified in delivered frames**. *(Half-paid:
  the capture the review asked for exists; the measurement does not.)*
- **A spent mark's drying has still never been captured at a true frame rate.**
- **`DRAW`, `TURN`, `BACK_STEP`, `FEINT` and one enchantment mark are now on screen** in the
  hand columns of the three bouts, and `BACK_STEP` also reaches the written column in Act III
  of `FOLD`. **No pixel statistic was taken through any of them.**
- **Nothing was measured about `knife` and `approach` beyond their camera arithmetic and the
  hard-edge sweep.**
- **`LaneScene` still duplicates about sixty lines of `DuelScene`'s render body**, and it has
  now drifted: the haze uniforms are set here and not there. Deliberate, and the drift the pass-1
  note predicted has begun.
- **`Opaque` is still public** to work around a one-line `CaptureApp` bug that is still there.
- **The raster guard sweeps at 20 Hz, not 60.** Rasterising 960×720 is three orders of
  magnitude dearer than reading vertices. Every distinct state of the interface lasts far
  longer than 50 ms — the shortest thing in it is a 0.60 s wick — but a defect confined to a
  single frame could hide there, where the two vertex guards could not. *(New, and stated in
  the guard's own docstring.)*
- **The raster guard's headroom is 22%** on the graded bouts (0.266 worst against a 0.34
  ceiling). It is meant to be tight; it will also fire on a change that is only cosmetic.
- **Whether the redrawn Parry still reads as a parry to someone who has not been told** is the
  one thing here only a human can grade, and the same is true of Back-step versus Step. The
  measurement says they are different pictures; it does not say they are the *right* pictures.

---

## 10. Every guard, and how it was observed red

§11.2b(f): no assertion counts as a guard until it has been observed red — and, since today,
a guard carrying a broad claim also owes the adversarial instance.

| # | guard | broken by | red |
|---|---|---|---|
| A | `noTriangleInTheInterfaceIsAFlatFill` | `Brush.stroke`'s left rim given the spine's alpha | pass 1, and the scope is now stated in what it tests |
| A2 | `noMarkPutsInkOnASilhouetteEdge` | pointed at `Guards.borderedPanel`: **2 inked silhouette edges**, asserted in `theForbiddenThingIsCaughtByTheGuardsThatForbidIt` | ✔, and its **defeat** is asserted too (§1.3, attempt 2) |
| A3 | `noMarkPrintsAnEdgeInTheRasterItWouldDraw` | the same panel reads **1.000** of its amplitude in one pixel; also observed red during authoring on the Draw glyph's folded ribbon (0.528) and on the Strikethrough's early wick (0.418) | ✔ |
| B | `vermillionIsSpentOnlyOnTheTilesTheEngineSaysAreThreatened` | the lane's base pools drawn in `VERMILLION` | pass 1 |
| C | `theInterfaceRecedesContinuouslyBecauseTheCameraDoes` | `Readout.intimacy` driven off "is a beat running"; now also sweeps `Readout.haze` | pass 1 |
| D | `theCameraReachesTheWideFramingInsideThePlanningGap` | `Scheduler.returnWide(idleAt)` reverted | pass 1, and re-verified by the review |
| E | `theColumnReadsDownwardInTheOrderTheEngineResolves` | the height comparator reversed | pass 1 |
| F | `theMarkThatResolvesFirstIsDrawnHighestOnTheSheet` | comparator, and `STANZA_PITCH` negated | pass 1 |
| G | `theResolvingClauseIsTheStrongestMarkInTheColumn` | the flood reverted to a fade | pass 1 |
| H | `theColumnDriesDownwardFromTheMarkThatResolvesNext` | `QUEUED_DRYING` 0.13 → 0.00 | pass 1 |
| I | `anEmptyStanzaStillPrintsEveryLineOfItsColumn` | the empty-slot loop bounded at `written` | pass 1 |
| J | `noTwoTilesAreTheSamePicture` | **observed red on pass 1's own alphabet**: `STEP-` vs `BACK_STEP+` measured **0.000** | ✔ |
| K | `everyCountedMarkIsCountableAtEveryShippedResolution` | **observed red on pass 1's own geometry**: cooldown 3 at 1 charge read 2 runs, cooldown 4 at 2 charges read 3 | ✔ |
| L | `theStanzaOutReadsTheHealthRow` | **observed red on pass 1's own values**: health at alpha 0.90 out-peaked the stanza | ✔ |

Known-answer rather than red-observed, and labelled as such: `everyTileTypeHasAMark`.
