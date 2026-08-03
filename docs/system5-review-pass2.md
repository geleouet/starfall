# System 5 pass 2 — review

# FAIL

Not on the thing pass 2 spent most of itself on. **The redrawn Parry still reads as a parry —
I named it cold, off the delivered pixels, and I named it as a *deflection*, from the shape.**
The fix did not break the thing it was protecting, and that was the one question only a
reviewer could answer.

It fails on three things. The graded planning shot is still a diagram and by §11.0's own count
it is **worse than pass 1's** — the fog was spent attenuating the subject, so atmosphere
arrived by *removing* readable parts. The hard-edge raster guard is green because it is not run
at the second resolution this very pass introduced; pointed there it is **red at 0.3563 against
its own 0.34 ceiling**. And a filled rectangular HUD panel built out of nothing but the
project's own `Brush.stroke` primitive is drawn into every frame of every bout and **passes all
410 tests** — seven times softer than the hatched panel the pass tried and reported caught.

Reviewer's standing: I did not build this and have no stake in any decision in it. Every pixel
number below was taken with an independently written NumPy/PIL/SciPy reader using the Rec. 709
weights `Frame.java` declares (§11.2b(c)), never with `analyse`; every number is printed beside
the rectangle it was taken through (§11.3); every claim about the interface is a difference
against the `-bare` control shot in the identical window at the same harness
(`harness=f0ad18994eec`). Suite state at review: **405 tests, 0 failures, 0 skipped**, verified
by parsing `build/test-results/test/*.xml` after `./gw test --rerun-tasks`, not by reading
`BUILD SUCCESSFUL`.

---

## 0. §11's required items, in order

**1. Verdict:** FAIL.

**2. The one-sentence test.** Family B (dusk duel) at execution, Family C (misty field) claimed
at planning, per §9.

- Execution framing (`s5-p2-fold-strike` frame 4): **nearly yes**, and slightly better than
  pass 1. A near-black duellist, a pale sliver blade, indigo-through-coral sky, hair
  dissolving, a fog band lying across the ground. It would survive the crop but for the
  interface's health ticks and one faded cartouche still floating in the upper-left sky.
- **Planning framing (`s5-p2-fold-plan` frame 3): no, and further from yes than pass 1.**
  Three figures totalling under 2% of the frame on a strip, 60% empty gradient above them, a
  column of marks up the left margin and a second column up the right. Nothing in the eight
  images is this.
- 15-tile framing (`s5-p2-approach-plan`): no.

**3. What is missing:** **not evocative enough** at the planning framing — and the specific new
fault is that pass 2's answer to "no atmosphere" *subtracted* from the subject instead of adding
to the frame — plus **technically broken** in the certification for the second pass running,
one level deeper than pass 1 found it.

**4. Why** and **5. changes** are §§1–9 and the pass-3 brief in §10.

---

## 1. The cold read, which is the thing only a reviewer can decide

### 1.1 Method, and my own contamination declared first

I used **`s5-p2-fold-plan` frame 0** and **`s5-p2-knife-plan` frame 0**, which carry the same
stanza. `system5-debt.md` never states any stanza's contents — it says only that `CUT`,
`THRUST`, `SWEEP` and `STEP` are untouched and that Parry changed — so no capture was spoiled
for me the way `s5-p1-fold-plan` was for my predecessor. I isolated every mark as
`live − bare` (`s5-p2-fold-plan-bare` frame 0) so that nothing I read was sky, cropped the
margin `x40..190 y40..440` at 3× to 14×, and **wrote all six readings down before opening
`Glyph.java` or `combat-design.md` §2.2.**

What I had been told, and it matters: my brief named the alphabet as *"Step, Back-step, Strike,
Sweep, Parry"* and told me Parry had been redrawn as a deflection curve and Back-step given its
own asymmetry. **That prior was wrong** — the stanza's five are `CUT, STEP, SWEEP, THRUST,
PARRY` and Back-step is not in it — and it actively damaged one reading. I report the readings
as I made them.

### 1.2 What I read, and whether it was right

Hero stanza, `s5-p2-fold-plan` frame 0, top to base, region `x58..135 y50..400` of 960×720:

| pos | what I wrote, cold | truth | ✔ |
|---|---|---|---|
| 1 (top) | *two curved strokes crossing: a smooth concave-up trough coming down from the upper left and climbing away to the upper right, crossed by a long shallow arc rising from the lower left. A blade caught and turned. **The trough is a curve, not a corner.** A parry* | `PARRY` | **✔** |
| 2 | *two short level lens dashes with a gap; the left larger and a little higher, the right smaller and lower. A weight transfer — I make it Back-step, on the rear mark carrying the weight; low confidence, could be Step* | `THRUST` | **✘** |
| 3 | *a large arc, ~250°, open at the lower left, with a small detached crescent outside it on the left. A sweeping cut* | `SWEEP` | **✔** |
| 4 | *a small level dash at the lower left and a large lens tilted up to the right, tapering to a point at its leading tip. A stride forward, toward the enemy* | `STEP` | **✔** |
| 5 (base) | *one heavy stroke, broad at the upper left, tapering to a fine point at the lower right, about 60° from horizontal. A committed descending cut* | `CUT` | **✔** |

And the hand column, `x815..895 y385..435`, which I did not know was a hand:

| what I wrote, cold | truth | ✔ |
|---|---|---|
| *a long tapering stroke, thick through the middle, drawn out to a fine barbed point at the left — toward the hero — with three small dashes beneath it. A strike, aimed leftward. This is the enemy's intent* | `BACK_STEP` | **✘** |

**Four of five on the stanza. Zero of one on the mark pass 2 re-authored.**

### 1.3 The result that matters: the Parry survived the redraw

This is the question the brief said outranks everything, and the answer is **yes**. I did not
merely land on the right label — I read the mark's *content* correctly: a long shallow stroke
taking the weight, a second stroke that comes down, runs along it and leaves climbing, and no
vertex anywhere in it. That is `Glyph.java`'s own description of what it drew, arrived at from
71 delivered pixels. Pass 1's cold read named a *chevron* and reasoned from "chevron → apex →
deflection"; mine had no apex to reason from and got there anyway.

Qualification, stated because my predecessor stated its own: I had been told a deflection curve
was somewhere in this alphabet. I had not been told which of five marks it was, or where in the
column, and picking mark 1 of 5 from its geometry is the part that was mine. **The fix did not
cost the read. Protect it.**

### 1.4 The two misses, and which of them is pass 2's

**`THRUST` — not pass 2's, and nobody has yet read it cold without help.** Pass 1's reviewer
disclosed that its readings 2 and 3 *"are not fully unaided — my brief had already told me that
Thrust is an interrupted line"*. Mine was unaided and wrong: two level dashes with a gap read to
me as **two footfalls**, not as a line a body interrupts. Both readings are available from the
same picture, and the vocabulary already spends "two marks side by side" on `STEP`. So across two
reviews, the Thrust glyph has been identified once with the answer supplied and once, without
it, as a movement verb. `THRUST` was untouched by this pass; the finding is inherited, not
caused, and it is now on the record.

**`BACK_STEP` — pass 2's, and it is the one it re-authored.** The retreat's whole job is to say
*backward*, on the axis `Glyph.java`'s own comment calls *"the sharpest choice on a lane"*. What
it delivers is one long dominant blade-like stroke with a hook at its far tip and two small
marks under it, and cold that reads as a **thrust or a cut travelling left**. Nothing in it said
*retreat*, and I was confident. The debt names this exactly — *"the measurement says they are
different pictures; it does not say they are the right pictures"* — and the human grade it asked
for is negative.

Two aggravating details. The hook and the dry scuff are the two features that carry the retreat,
and both are the *faintest* parts of the mark: measured on `s5-p2-fold-plan` frame 0 through
`x835..900 y388..430`, the hook fleck peaks at 21.6 luminance over bare and the scuff at 18.9,
against the travel stroke's 77.8 — so at the delivered size the reader gets the stroke and
loses the argument. And the whole hand column reads as *the enemy's*: it is inset from the right
edge, it sits between the two Charted Shadows, and its charge run lies directly above the enemy
health row. I took it for enemy intent without hesitating. That is a composition problem, not a
glyph problem, and it is new in this pass because the hand only became legible in this pass.

### 1.5 One more mark, measured because the debt says nobody has

`system5-debt.md` §9: *"The Feint is at the edge of legibility in the hand… **Nobody has
measured whether it can be identified.**"* Measured, `s5-p2-fold-plan` frame 0, live − bare:

| mark | box | peak lift | px above 4 |
|---|---|---|---|
| `FEINT + MARKING` glyph | `x835..900 y450..492` | **4.5** | **6** |
| `BACK_STEP` glyph, the slot above it | `x835..900 y388..430` | 77.8 | 793 |
| its own charge run, same row | `x700..835 y456..480` | 42.0 | — |
| the faintest *empty* stanza impression | `x62..132 y340..400` | 14.9 | — |

**It cannot be identified.** Six pixels above the noise floor is not a mark; it is 17× fainter
than the tile drawn immediately above it and **three times fainter than an empty slot's
impression**, and its own cooldown ticks out-read it by 9×. Pass 1's third refuted hunch was
*"a Feint will read as an empty slot"* — refuted for the stanza, and in the hand it is now
confirmed, one step worse: it reads as nothing at all, in a row whose ticks say a tile is there.

---

## 2. The guards, and the honesty of their stated scope

### 2.1 What reproduces exactly

`Guards.borderedPanel` and the whole §1.3 table, rebuilt independently in test scope and
measured through the checked-in `Raster`, box `x0.60..1.00 y0.60..0.90` in frame heights at
960×720:

| construction | flat-fill triangles | inked silhouette edges | steepest 1-px step / peak | debt says |
|---|---|---|---|---|
| pass-1 bordered panel | 2 | **2** | **1.0000** | 1.000, 2 edges |
| filled panel, 1 px feather | 2 | 0 | **0.5000** | 0.500 |
| filled panel, 2 px feather | 2 | 0 | **0.5000** | 0.500 |
| filled panel, **3 px feather** | 2 | 0 | **0.3333** | 0.333 |
| filled panel, 4 px feather | 2 | 0 | 0.2500 | — |
| filled panel, **5 px feather** | 2 | 0 | **0.2000** | 0.200 |
| filled panel, 8 px feather | 2 | 0 | 0.1250 | — |

Every published figure lands on mine to four decimals. The instrument is sound and the account
of it is accurate.

### 2.2 …but "a soft-edged panel still passes every guard here" is **false**, in both directions

`system5-debt.md` §9, first bullet, and the merge message with it. I built the 5 px feathered
panel into `LaneInterface.sheet` — drawn on every frame of every bout, exactly as my predecessor
built its own — and ran the whole suite:

```
./gw test --rerun-tasks   →  409 tests completed, 1 failed
InterfaceInkTest > noTriangleInTheInterfaceIsAFlatFill() FAILED
```

The two *hard-edge* guards let it through, precisely as the debt says. The third guard — pass
1's assertion, kept and renamed — catches it on its two interior triangles. So the sentence
"**passes both guards**" in §1.3 is exact and the sentence "**passes every guard here**" in §9
is not, and the second is the one the merge message and `progress.html` repeat.

### 2.3 And the hole is real anyway — a `Brush`-only panel passes all 410 tests

The debt's attempt 4 — *"a panel hatched out of 90 legal `Brush` strokes"*, 0.360, caught by the
raster guard — is the right idea abandoned one iteration too early. A panel built the same way
with a wider brush and a tighter pitch satisfies **every** guard by construction, because every
triangle `Brush` emits has a zero-alpha vertex and every inked edge it emits is interior:

| hatch (box `x0.60..1.00 y0.60..0.90`, strokes run `x0.51..1.09`) | flat-fill | silhouette | share, 960×720 | share, 720×540 | box above ½-peak |
|---|---|---|---|---|---|
| width 0.030, pitch 0.010, α 0.60 | 0 | 0 | 0.1475 | 0.1905 | 86.4% |
| width 0.050, pitch 0.015, α 0.55 | 0 | 0 | 0.0882 | 0.1174 | 87.4% |
| **width 0.080, pitch 0.020, α 0.45** | **0** | **0** | **0.0515** | **0.0681** | **88.7%** |

0.0515 is **6.6× inside the 0.34 ceiling** and **7× softer than the pass's own attempt 4**.
Drawn into `LaneInterface.sheet` at width 0.080 / pitch 0.007 / α 0.32:

```
./gw test --rerun-tasks   →  BUILD SUCCESSFUL
tests 410  failures 0  skipped 0  errors 0
```

The delivered frame is `out/captures/rev-s5p2-hatchpanel2/frame_000.png`: an opaque rectangular
panel with a visible boundary on all four sides, occupying the middle of the sky, in every frame
of the graded bout, with a green suite. **This is the same defect pass 1 failed the pass for, one
level deeper**: pass 1 defeated the guard with an illegal primitive, and this defeats it with the
*sanctioned* one. `LaneInterface` calling only `Brush` is the project's structural argument
against chrome, and it is not an argument — a rectangle is a schedule of legal strokes.

Both `LaneInterface.java` and the temporary capture directory were reverted; the tree is clean
and `./gw test` is green as I leave it.

### 2.4 Is 0.34 defensible or fitted?

**Fitted, and the pass's own defence of it is the evidence.** The docstring derives it as *"a
third of what a hard edge prints"*, i.e. *a mark must take at least three pixels to arrive*.
Three things say otherwise.

- **It is a floor with no upper edge and no corpus behind it.** §11.0's closing paragraph is
  explicit: *"a criterion of floors alone rewards the defect it was written to catch… state the
  target as a band with both edges, taken from the corpus's own spread."* 0.34 is derived from
  the *forbidden thing*, not from any reference image. Nothing in the corpus was measured to
  produce it and nothing in it fails if the interface gets softer without limit.
- **The interval that would have caught the successful attack was available and not taken.** The
  delivered worst over all three bouts through the guard's own raster is **0.2660** (`KNIFE`,
  t=0.000, at (840,401) of 960×720), which I reproduce exactly. Any ceiling in
  **(0.266, 0.333)** passes the shipped interface *and* catches attempt 3c. 0.30 — "a mark takes
  at least 3.3 pixels" — is as round a number as 0.34, is inside that interval, and would have
  made the pass's own successful attack fail. The pass argues the opposite way, that 3c landing
  at 0.333 against 0.34 shows *"the guard's boundary is exactly where its statement puts it and
  not somewhere convenient"*; the same fact read the other way is that the boundary was placed
  one part in a hundred on the permissive side of the only attack that beat it.
- **And it is a ratio to an artefact, which makes it unfalsifiable in the direction that
  matters.** 1.000 is what a step function reads. Dividing it by three answers "how much softer
  than infinitely sharp", a question the corpus cannot fail and cannot inform. The number that
  *would* be defensible is the one pass 1 built and pass 2 preserved: the interface's step
  against **the blade's step in the same pixels** (0.244 of amplitude against 114.5 luminance
  levels through `x220..289 y420..519`), because §3 names the blade as the one thing allowed a
  hard edge. That comparison is in the debt's prose and is in no assertion.

### 2.5 The guard does not run at the second resolution — and it is red there

This is the sharpest thing I found in the certification. `Guards.SHIPPED_HEIGHTS = {720, 540}`
is introduced by this pass, and `everyCountedMarkIsCountableAtEveryShippedResolution` iterates
over both — correctly, and it is the guard the pass is proudest of.
`noMarkPrintsAnEdgeInTheRasterItWouldDraw` hard-codes `SHIPPED_HEIGHTS[0]`.

Swept myself through the identical code path (`Guards.interfaceField`, `steepestStep(8)`,
amplitude = the bout's own maximum):

| bout | 960×720 @20 Hz | 960×720 @60 Hz | **720×540 @60 Hz** |
|---|---|---|---|
| `KNIFE` | 0.2660 | 0.2660 | **0.3544** |
| `FOLD` | 0.2251 | 0.2243 | 0.2971 |
| `APPROACH` | 0.2439 | 0.2445 | 0.3152 |

Two results. **The 20 Hz sampling debt is discharged** — 60 Hz moves the worst figure by 0.0006,
so nothing hides between samples; the pass may strike that item. And **the guard fails at 540
rows.** Pointed there by changing one array index, it goes red on its own message:

```
KNIFE at t=0.000: the interface's steepest one-pixel step is 0.2799, which is 0.3563 of the
0.7854 amplitude the interface reaches at its strongest, at (630,301) of 960x720, against a
ceiling of 0.34
```

Note the message says `960x720` while measuring a 720×540 field — the format string carries
`SHIPPED_WIDTH` and `SHIPPED_HEIGHTS[0]` as literals. **A guard that prints a region it did not
measure through is the §11.3 failure inside the instrument that enforces §11.3.**

The pass knew the direction: §1.4's delivered table records 0.296 and **0.334** at 720×540 and
calls it *"the price of a shipped second resolution… now on the record instead of unmeasured"*.
I reproduce both to the digit. What it did not do is point the guard at the resolution it had
just declared, where the price exceeds the ceiling. `KNIFE` is not captured at 540 anywhere, so
the delivered table never met the worst case either.

### 2.6 Guard hygiene, for the record

- `theForbiddenThingIsCaughtByTheGuardsThatForbidIt` is the best thing in this pass. Checking the
  adversarial instance in as an assertion — including the *duplication* attack that defeats the
  topology guard — means the exhibit cannot rot, and it is the pattern §11.2b(f) has been
  groping toward for three revisions. Keep it, and add mine to it.
- `noTwoTilesAreTheSamePicture`'s second assertion, `closestMirror > 0.0`, is a self-check on the
  instrument wearing a guard's clothes. Its message says as much (*"means the measurement is not
  reading the glyphs it thinks it is"*), it is not in the red-observed table, and no cross-tile
  defect can reach it. Harmless, but it should not be counted as one of the pass's guards.
- **Zero skips**, verified by parsing the XML rather than trusting the console, on a
  `--rerun-tasks` build so the report is not older than its inputs. The staleness clause
  §11.2b(f) added this morning does its job.

---

## 3. The distinctness guard, and the narrowing

### 3.1 The measurement, re-enumerated

All 153 pairs, through `Guards.glyphField` at the shipped 71 px cartouche
(`STANZA_GLYPH × 720`), `Raster.distance`:

| | measured here | debt says |
|---|---|---|
| closest **cross-tile** of 144 | `STEP+` vs `BACK_STEP+` = **0.7277** | 0.733 |
| next | `STEP−` vs `BACK_STEP−` = 0.7300 | — |
| next | `PARRY±` vs `DRAW±` = **0.7601 / 0.7602** | — |
| closest of all 153 | `SWEEP+` vs `SWEEP−` = **0.1543** | 0.154 |

Same-tile mirrors: `CUT 0.8558, THRUST 0.7919, PARRY 0.8471, SWEEP 0.1543, DRAW 0.9062,
STEP 0.9427, BACK_STEP 0.8611, TURN 0.7929, FEINT 1.0000`. Eight of nine match the debt to
±0.0005. **`BACK_STEP` does not: 0.8611 against a published 0.890**, and the headline cross-tile
figure is 0.7277 against a published 0.733. Both are computed through the guard's own code path,
so the shipped code is the authority and the two published numbers were taken before some later
edit. Immaterial to the verdict — both clear the 0.20 floor by 3.6× — but they are two numbers
in §2 that do not reproduce, and they are the two about the mark this pass re-authored.

### 3.2 Ruling on the narrowing: **upheld, and it is better reasoning than the instruction it replaced — with one thing left undone**

I asked myself whether "144 not 153" was reasoning or escape, and it is reasoning.

- The defect was **two different tiles drawing one picture**. `STEP−` ≡ `BACK_STEP+` at exactly
  0.000 was a collision *between verbs*, and that is what the guard now watches, at 144 pairs.
- The `SWEEP` exception is forced by the vocabulary, not by convenience. §2.2's beat is *"one
  continuous arc through two bodies"* — front **and** behind — so the tile's effect is
  facing-symmetric, and a near-symmetric mark is the *correct* drawing of it. Requiring 0.20
  there would require the alphabet to lie about a symmetric gesture. `Glyph`'s own promise, that
  *"the column shows which way the phrase points"*, is not broken by Sweep; it is answered, with
  *"it does not point"*.
- And a reviewer's instruction is not a specification. The pass tested the property the defect
  actually had, said in one paragraph why the literal ask was wrong, and published the numbers
  it excluded. That is the correct response to a brief.

**What is missing is one line of enforcement.** The exception lives in prose and the guard
`continue`s past every same-tile pair, so the 0.20 floor — which every one of the other eight
tiles clears by 3.6× or more — protects nothing on that axis. If a future tile were authored
near-symmetric by accident (a `TURN` drawn as a closed ring is one keystroke away: it is at
0.7929 today), it would land in the same hole silently, which is §11.2b(e) exactly. The fix is
four lines: assert `SWEEP` is the **only** tile below the floor under mirroring, and that every
other tile clears it. Then the exception is a claim the suite defends rather than a paragraph.

---

## 4. The composition — §11.0's matched-scale part count, run

§11.0 says this is the first act of every review. Pass 2 says plainly that it did not run it and
that the fog moves it the wrong way. Run.

### 4.1 The count

Reference image 3 (`inspirations/image - 2026-08-02T101033.164.png`), the Family B image this
stage's own template comes from. Dark duellist `y285..950` = **665 px**.

Capture `s5-p2-fold-plan` frame 3, hero detected against each row's own median inside
`x205..300`, ≥2 px per row: **`y429..505` = 77 px**, stable from a 4-level to an 8-level
threshold. The same detector on `s5-p1-fold-plan` frame 3 gives **`y429..515` = 87 px**.
Reference downscaled by 77/665 = 0.1158; both viewed at 7× nearest-neighbour.

| | readable parts at ~77 px |
|---|---|
| **reference image 3** | topknot, head **with a face** (brow, nose, chin), neck, collar, shoulder, upper arm, forearm, two hands, grip, guard, blade, sash, second sheathed blade with its red cord, skirt with fold structure, forward leg, rear leg, foot, dissolving hem — **~16** |
| **capture, pass 1** | hair mass, head as a lump, torso, ochre sash band, a second ochre mark, blade, shed flecks, a lower mass tapering to a point — **~8** |
| **capture, pass 2** | hair mass, head as a lump, torso, ochre sash band (fainter), blade, shed flecks — **~6** |

**Still a diagram, and by this instrument it went backwards.** The hero is 11.5% shorter in
delivered pixels, its detected ink area falls 2,186 → 1,899 px through the same box, the second
ochre mark is gone and the lower mass that used to taper to a point has dissolved into the fog
band. Side by side at 7× the pass-2 figure is a paler, shorter, less articulated version of the
pass-1 figure — nothing was added.

**And this is the structural criticism of pass 2's answer to §9.** §6 specifies fog as a
*positive element*: *"horizontal drifting bands of `#D6D2CE` at varying alpha that occlude the
lower body of figures and separate depth layers. This is present in every single reference
image… Non-negotiable."* §9 asks the planning framing for *"heavy fog, Family C mood"*. Pass 2
delivered atmosphere as an **alpha attenuation of the subject** — measured, honestly, and
calibrated against reference image 6, which is real work. But attenuating the subject removes
readable parts and adds none, and §11.0's corollary is that *"a material can only ever be as good
as the subject it is painting. If the count is short, fixing the material is refinement of the
wrong thing."* Thinning the subject is one worse than that. The bands themselves do exist on the
stage — `s5-p2-fold-plan-bare` frame 3 carries a pale band across `y495..535` — but that is one
band at the horizon, not the layered depth separation the corpus shows, and it does nothing for
the upper 60% of the frame, which contains a gradient and eleven motes.

### 4.2 The statistic, reproduced

`>8` luminance from the pixel's own row median, whole frame, frame 3, live vs `-bare`:

| | pass 1 | pass 2 |
|---|---|---|
| bare planning frame that is anything at all | **4.84%** | **4.76%** |
| the interface | **2.09%** | **2.16%** |
| interface / world | **0.431** | **0.454** |

Exactly as published. Nearly a third of everything you can see in the graded shot is the
interface, and that share rose.

### 4.3 On refusing the framing law

§4.5 declines pass 1's framing-law item with two numbers: a cap wide enough to leave System 4
alone is 8.5 tiles, and 8.5 tiles centred on an 11-tile lane drops the enemy at tile 10 out of
frame. Both are correct arithmetic and neither is an argument against the item, because the item
asked for an **action-centred** framing, not a narrower centred one — pass 2 names that itself
(*"plus an action-centred planning framing, which needs `Standing` inside `Stage.planning()`"*)
and then reports the centred version's failure as though it settled the question. It does not.
This is the third pass in a row to name the same `Stage` change and the second to decline it,
and it is, by the pass's own words, *"the largest single thing between this frame and §0"*.

---

## 5. The two claims pass 2 says do not reproduce

### 5.1 The paper substrate — **the correction is right; the "1%" is fitted to one image**

3 px high-pass standard deviation, box filter, my own reader:

| region | hp(3 px) | hp(9 px) |
|---|---|---|
| S5 planning sky (bare), `x300..699 y120..319` | **0.474** | 0.773 |
| **reference image 3** sky, `x80..699 y80..239` | **0.489** | 1.095 |
| reference image 1 paper, `x60..699 y60..299` | 6.464 | 12.739 |

The capture is **0.97×** image 3's sky and **0.073×** image 1's paper. Pass 2's correction is
therefore **upheld**: my predecessor's 16× compared a Family B dusk sky against a Family A cream
sheet, and against the family this stage quotes there is nothing to explain. The conclusion —
there is no paper in this frame and a dusk stage cannot carry §3b.3's — survives in both
accounts, and filing it in `combat-design.md` §3 as the fourth contradiction is the right
disposal.

**But §11.0's own rule was not applied to it.** *"One reference image is not the corpus… show it
on every image in the family that depicts the situation being measured, and name the ones you
excluded and why."* Across the corpus's skies:

| image | family | hp(3 px) | capture / image |
|---|---|---|---|
| 3, `x80..699 y80..239` | B | 0.489 | 0.97× |
| 4, `x80..699 y80..239` | B | **0.747** | 0.63× |
| 5, `x80..699 y80..239` | B | 0.564 | 0.84× |
| 6, `x60..399 y20..139` | C | 0.803 | 0.59× |
| 7, `x60..399 y30..149` | C | 1.087 | 0.44× |
| 8, `x60..399 y30..149` | C | **5.555** | 0.085× |

The capture sits **at or below the floor of the corpus's own spread**, matching only the
smoothest sky in it. "Matches to 1%" is a single-image result and the sentence should say so.

And the inconsistency it exposes is worth more than the number: **pass 2 calibrated its fog on
reference image 6 (Family C) and its substrate on reference image 3 (Family B), for the same
shot.** If the planning framing is quoting Family C — which is what §9 says and what the haze
was built to deliver — then the substrate comparison belongs against images 6–8, where the sky
carries 1.7× to 11.7× the capture's surface, and the "no paper" conclusion comes straight back.
One framing cannot take its atmosphere from one family and its surface standard from another.

### 5.2 The haze term written straight through — **the phenomenon is real; the number is not, and the form the comment names is free**

`ink_resolve.frag`: *"shot against the same scene with the lines deleted, `duel-parry` differed
by **1180 pixels** over six frames, against a null control… of **0**."* Re-run here, six frames
of `duel-parry` at the default window (`start=0.0 step=0.13217 960×720`), a pixel counted as
differing if any channel differs by ≥1:

| comparison | differing px, 6 frames |
|---|---|
| **null control** — same scene, HEAD, shot three times (A/B/C) | **0, 0** |
| haze **written straight through** in `ink_resolve.frag` | **0** |
| haze **written straight through** in all three shaders (`ink_resolve`, `hair`, `ink_blade`) | **0** |
| haze **lines deleted** from all three shaders | **192** (all on frame 3, peak Δ18) |
| apparatus check — `mix(ink, u_fogColor, 0.5)` in `ink_resolve` | **770,603** |

So: **the 1,180 px does not reproduce**, and the specific edit the comment attributes it to —
*written straight through* — is bit-free on this harness in all three shaders. What does
reproduce is the *phenomenon*: deleting the lines entirely, which is arithmetically the identity
at `u_haze = 0`, moves 192 pixels against a null of 0. The apparatus check proves shader edits
reach the capture, so the zeros are real zeros.

**Verdict on it: the conclusion is upheld and the evidence for it is 6× weaker than published,
and attached to the wrong edit.** Keeping the branch is right. The generalisation the pass drew —
*"'this term is the identity when the flag is off' is a claim about arithmetic and not about a
compiler"* — is correct and is independently confirmed by my 192. The comment should carry the
edit it measured and, per §11.2b(d), the fact that the magnitude is a property of one driver on
one machine.

**And one sentence beside it is not testable at all.** The same comment closes: *"every capture
System 4 is graded on is bit-identical across this change."* System 4's graded capture is
`s4-p5-parry-contact` (`duel-parry`, `start=1.42 step=0.0167`, 24 frames). Shot twice at HEAD,
that window differs from itself by **25,547 px**; the archived pair `s4-p5-parry-contact` vs
`s4-p5-parry-repro`, taken at one commit during System 4, differs by **13,825 px**; and the
archived capture against a fresh shoot at HEAD differs by **16,099 px** — *less than the null
control*. This is §11.2b(g)'s recorded non-determinism (13,545 px, peak 122) still live. The
honest statement is that **System 4's graded window cannot be checked for bit-identity by
anyone**, and the 6-frame window where the null is 0 is the only one where the claim means
anything. Reporting the zero without reporting that the graded window's null is 13,825 is
choosing the control that can pass.

For what it is worth in the other direction: 16,099 < 25,547 is positive evidence that System 4's
pixels are undisturbed, and System 4's schedule fingerprints are unchanged — `PHRASE` 6.9521,
`PARRY` 3.0400, `KNOCKBACK` 3.1548, identical to the durations pass 1 published.

---

## 6. Every protected result, checked

All through my own reader, all against the `-bare` sibling in the identical window.

| result | region | debt | mine |
|---|---|---|---|
| Hard edge, `fold-plan` | whole frame less 8 px, 6 frames | 0.244 (step 22.9 / amp 93.7) | **0.2439 (22.85 / 93.69)** |
| Hard edge, `fold-strike` | 12 frames | 0.223 (20.7 / 92.8) | **0.2231 (20.71 / 92.84)** |
| `fold-replan` / `knife-plan` / `approach-plan` | — | 0.250 / 0.277 / 0.250 | **0.2502 / 0.2769 / 0.2498** |
| `fold-plan` 540 / `fold-replan` 540 | — | 0.296 / 0.334 | **0.2957 / 0.3341** |
| `fold-bleed` / `fold-pushin` | — | 0.432 / 0.263 | **0.4323 / 0.2631** |
| pass-1 `fold-plan` / `fold-strike`, re-measured | — | 0.249 / 0.242 | **0.2495 / 0.2424** |
| blade control | `x360..539 y470..519` bare | 68.4 / 70.5 | **68.4 / 70.5** |
| figure control | `x220..289 y420..519` bare | 114.5 / 115.5 | **114.5 / 115.5** |
| The drying column, peak lift | `x62..132`, five pitches, frame 3 | 94.7 / 86.7 / 78.8 / 72.6 / 68.8 | **identical** |
| — as a ratio to its own ground | same | 2.648 / 2.501 / 2.301 / 2.217 / 2.157 | **identical** |
| — ink px (lift > 4) | same | 2175 / 1292 / 744 / 519 / 493 | **2143 / 1324 / 743 / 519 / 490** |
| Stanza out-reads health | `x36..254 y10..44` vs `x62..132 y53..123` | +72.8 (86,26) vs +94.7 (88,87) | **identical, to the pixel** |
| Lane band | `x0..959 y505..559` | 1.110×, +84.8 at (172,535) | **1.110×, +84.8 at (172,535)** |
| Draw order | `fold-strike` f4, `y560..699` | 0.00 under 6,977 px of ink; 2.92 beside | **identical, to the pixel** |
| Determinism | `fold-plan` ×6, `fold-strike` ×12 vs `-repro` | 0 px | **0 px** |
| Empty state | five slot boxes, `fold-empty` f2 | 21.2 / 18.3 / 14.3 / 14.9 / 34.0 | **21.2 / 18.2 / 14.9 / 14.9 / 34.0** |
| Vermillion budget | `r−b` lift > 20, frame 3 | 1,923 px = 0.278%, live `(154,64,61)` / bare `(63,63,75)` | **identical** |
| Cooldowns countable, 960×720 | `fold-replan` f2 & f4, hand rows 2–6 | 3 / 3 / 4 / 1 / 5 | **3 / 3 / 4 / 1 / 5** |
| Cooldowns countable, **720×540** | same, `-540` captures | 3 / 3 / 4 / 1 / 5 | **3 / 3 / 4 / 1 / 5** |

**Everything holds.** Run positions at 720×540 are `563–566, 581–584, 598–602, 616–620` for the
Parry's four charges — 4 px marks with 13 px of paper between them. The countability obligation
pass 1 left is discharged properly, in delivered pixels, at both resolutions, on two frames. It
is the cleanest piece of work in this pass.

One region note, not a fault: §7 puts pass 1's *"4.09 beside"* next to pass 2's *"2.92 beside"*.
Through the whole `y560..699` band I get 2.89 for pass 1 and 2.92 for pass 2 — the two published
numbers are taken through different open-ground sets (pass 1 named 38,296 px; the band holds
127,423), so the row reads as a 29% improvement in a quantity that did not move. §11.3's point,
committed inside a comparison table.

---

## 7. The §8 amendment — honest

Checked against `git show dcf8df7 -- STYLE.md`. It replaces "health is a column" with "a row at
the head of the stanza, never a second column beside it", attributes the change to pass 1 *and*
to pass 1's review upholding it, gives the reason (§3 chose vertical first, §8 is older and
yields), and then **adds an obligation the pass then had to meet** — the countability rule, with
the failing number that motivated it written into the rubric. A pass that amends its own grading
rubric to make its life harder is amending it honestly. No objection.

Two loose ends, both small. §8's amended sentence still opens *"the action queue is three ink
cartouches"* while five are delivered per `combat-design.md` §1.1a — the pass edited that exact
sentence and left the count wrong. And §10's row *"UI panels, bars, boxes, borders"* is the line
§2.3 above defeats; the debt says so in §1.3 and the rubric does not.

---

## 8. Claims in `system5-debt.md` that do not reproduce

1. **§9, first bullet — *"A soft-edged panel still passes every guard here."*** False. The 5 px
   feathered panel fails `noTriangleInTheInterfaceIsAFlatFill` when drawn: 409 tests, 1 failed.
   §1.3's own narrower sentence ("passes both guards") is correct; §9's is the one repeated in
   the merge message and on the progress page. §2.2 above.
2. **§1.3, attempt 4 — *"a panel hatched out of 90 legal `Brush` strokes… 0.360… caught by
   raster."*** The construction class is not caught; only that instance of it was. At width
   0.080 / pitch 0.020 the same idea reads **0.0515** and, drawn, passes all 410 tests. The
   guards' stated scope should say **primitive-legal rectangles are not caught either**. §2.3.
3. **§4.1a and `ink_resolve.frag` — the 1,180 px.** Does not reproduce: 0 for the
   straight-through form named in the comment, in all three shaders; 192 for deletion; null
   control 0 over three shoots. §5.2.
4. **`ink_resolve.frag` — *"every capture System 4 is graded on is bit-identical across this
   change."*** Not testable. System 4's graded window's own null control is 13,825–25,547 px.
   §5.2.
5. **§2 — the two glyph distances about the re-authored mark.** `BACK_STEP` mirror **0.8611**
   against a published 0.890; closest cross-tile **0.7277** against a published 0.733, both
   through the guard's own code path. §3.1.
6. **§9 — *"The raster guard sweeps at 20 Hz… a defect confined to a single frame could hide
   there."*** Measured: 60 Hz moves the worst share by 0.0006 on all three bouts. The debt item
   is discharged, not outstanding. *(A claim that does not reproduce in the pass's own favour,
   and it should be struck rather than carried.)*

**Claims of the review's own that pass 2 corrected, and which I confirm it corrected correctly:**
the paper-substrate family error (§5.1); the `wide to plan` overstatement; the missing `-bare`
control on the tick count; `everyTileTypeHasAMark`'s real scope. All four dispositions are right.

---

## 9. What this pass got right, stated plainly

- **The Parry redraw succeeded on the only test that could grade it.** §1.3.
- **The countability obligation is discharged properly** — geometry *and* delivered pixels,
  every cooldown 0–8 at every charge count, both shipped heights, and reproduced by me. §6.
- **The adversarial exhibit is checked in as an assertion, including the attack that defeats one
  of the two guards.** That is a better answer to §11.2b(f) than the section asked for.
- **The pass reported a partial defeat as a defeat** and wrote down the construction that beat
  it, which is why I could start from it and go further in an hour instead of a day.
- **Every protected result survives, to the pixel**, across nineteen rows. §6.
- **The narrowing of the distinctness brief is better reasoning than the brief.** §3.2.
- **The instrument notes are worth more than the guard.** The top-left fill rule and the `double`
  edge functions are two cases of an instrument manufacturing the artefact it looks for, both
  found and both written down. That paragraph will outlive this system.

---

## 10. Pass-3 brief

Two of five passes are spent. Ranked by impact; 1 is the §0 failure, 2 and 3 are the
certification, 4 is the alphabet, 5–6 are corrections.

1. **Make the graded planning shot a picture.** It is the standing failure of three passes and
   it is System 5's shot.
   - **Deliver §6's fog as bands, not as attenuation of the subject.** Drifting horizontal
     `#D6D2CE` bands at varying alpha that occlude the lower bodies and separate depth layers —
     §6 calls this non-negotiable and every reference image has it. Atmosphere must *add* marks.
     Then re-run §11.0's count and publish it; the target is that the count goes **up** while
     figure contrast goes down.
   - **Open the framing law, action-centred.** `Standing` inside `Stage.planning()`, framing on
     the exchange rather than on the lane's midpoint, hero near the ~180 px that
     `s5-p2-knife-plan` already proves works. The centred-cap arithmetic in §4.5 does not bear
     on this and should stop being offered as though it does. Guard the System 4 schedules with
     the three duration fingerprints, which cost nothing.
   - **Rebalance the two margins.** I read the right-hand hand column as the enemy's intent; it
     is inset from the right edge, it sits between the two Charted Shadows and its charge run
     lies above their health row. Either move the hand or mark it as the hero's.
2. **Run the hard-edge raster guard at every entry of `SHIPPED_HEIGHTS`**, and then either
   soften `KNIFE` at 540 rows until it passes or drop 540 from the shipped list. It is red there
   today at **0.3563**. Fix the failure message, which prints `960x720` whatever it measured.
   And **re-derive the ceiling from something other than the artefact**: the honest anchor is
   already in the debt's prose — the interface's step against the blade's step in the same
   pixels — and §11.0 requires a band with both edges.
3. **Close the `Brush`-hatch hole, or narrow the claim in §9 to what is true.** The exhibit is
   in §2.3 with its parameters; paste it in as a fixture beside `Guards.borderedPanel`, exactly
   as this pass did with the reviewer's panel. The cheapest real criterion is about *form*, not
   gradient, and one is available: the interface's coverage field must contain **no axis-aligned
   run of near-constant coverage longer than N px, on either axis**. A stroke, a wash and a
   cartouche do not produce one; a panel of any softness produces two thousand.
4. **The alphabet needs one more pass on three letters, and one of them is this pass's own.**
   - **`BACK_STEP` does not read as a retreat.** Its dominant mark is a long blade-like stroke
     and cold it reads as a leftward strike; the hook and the scuff that carry the meaning are
     at 28% and 24% of the travel stroke's peak. Make the *retreat* the loud part.
   - **`THRUST` has never been read cold correctly by anyone unaided** — it read to me as two
     footfalls, which is `STEP`'s own device. Give it something a movement cannot have.
   - **`FEINT` is invisible**: peak lift 4.5, six pixels above the floor, `x835..900 y450..492`.
     Either it stops being drawn as absence, or the hand stops claiming it holds a tile there.
   - **Assert the `SWEEP` exception** rather than describing it: `SWEEP` is the only tile
     permitted below the 0.20 floor under mirroring, and every other tile must clear it.
5. **Correct the six items in §8 above**, including the two glyph distances, and strike the 20 Hz
   debt item, which measurement discharges.
6. **Re-run the substrate measurement across the family**, per §11.0's "one reference image is
   not the corpus" — and settle which family the planning framing belongs to, because the fog
   was calibrated on image 6 and the substrate on image 3 for the same shot.

### What I would accept as permanent debt

Named now, so pass 3 does not spend itself on things this project should stop paying for.

- **§3b.3's aged paper on a dusk stage.** Correctly diagnosed as a family contradiction, filed
  in `combat-design.md` §3, and the margin foxing (1.53× at a 17 px high-pass through
  `x140..176 y60..400`) is the right substitute. A 3 px fragment-shader tooth in
  `PaperBackground` should never be built. **Close it.**
- **A general automatic criterion for "this is a rectangular composition".** Nobody has one and
  it is genuinely hard. Accept the *general* form as permanent debt — but not before item 3
  above, which is the cheap 90% and costs an afternoon.
- **The raster guard's 20 Hz sweep.** Measured equivalent to 60 Hz to within 0.0006. Close it.
- **The matched-scale part count's *figure* half** — no face, no hands, no separated legs at
  77 px — belongs to System 4, which is closed at five passes. System 5 owns only the *framing*
  half: put the figure at a size where System 4's articulation can show. Do not fail System 5
  again for System 4's figure; do fail it if the figure stays at 77 px.
- **Enemy hit points not drawn.** The largest omission in the debt, and it is a `Figure` /
  `InkMaterial` change, not an interface one. Hand it to whichever system owns enemy rendering
  rather than spending a System 5 pass on it.
- **`LaneScene` duplicating `DuelScene`'s render body, and `Opaque` being public.** Real, small,
  and not worth a pass of a five-pass budget.

*Not* acceptable as permanent debt, with three passes left: §0 at the planning framing, a guard
that does not run where the product ships, and an alphabet whose retreat reads as an attack.

---

*Reviewed against `STYLE.md` rev. 1 (§0, §1, §2, §3, §6, §8, §9, §10, §11.0, §11.2, §11.2b,
§11.3), `docs/combat-design.md` §2.2, §3, §3d.3, and the eight images in `inspirations/`.
Captures read: all 23 `s5-p2-*` directories and all 13 `s5-p1-*`, plus `s4-p5-parry-contact`
and `s4-p5-parry-repro`, and captures shot for this review (`rev-p2r-*`,
`rev-s5p2-hatchpanel2`). Source edits made and reverted: `LaneInterface.java` (twice),
`InterfaceInkTest.java`, `ink_resolve.frag`, `hair.frag`, `ink_blade.frag`; five review-only
probe classes written and deleted. Tree clean and suite green at 405 / 0 failures / 0 skipped
as I leave it.*
