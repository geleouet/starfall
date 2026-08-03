# System 3b — faces — the closing record of pass 2

**Status: delivered, second of the review's five passes.** Pass 1's closing record is
superseded by this document; where a pass-1 claim did not survive pass 2's measurement
it is listed in §5, which is the section this project values most. Written for the
reviewer of this pass, System 3c, and System 5.

Every final capture quoted is `s3b-p2-*`, shot at the commit this document ships in
(the code changed; the apparatus did not — same harness as every `s3b-p1-*` and
`s4-p2-*`..`s4-p5-*` capture, so §11.2b(d) comparisons back to both are in scope).
Every pixel number is printed beside its rectangle (§11.3). Two boxes recur and are
derived, not drawn: the hero head `x370..455 y277..362` and the foe head
`x568..653 y283..368`, both 86×86 on frame 11 of the parry window, from
`FaceWindowTest.headBox` — `Rehearsal` plus the schedule's own framing arithmetic.

**A note on the resumption.** This pass was interrupted mid-iteration by an API limit
after thirteen numbered attempts (`s3b-p2-try1..13`, kept on disk) and resumed by a
different agent from the committed state plus the review alone; the resumed work is
`try14..32`, also kept. The interruption cost nothing measurable except the loss of
the first agent's reasoning, one consequence of which is §5.6.

---

## 1. The review's eight items, each with its verdict

1. **Re-derive the face's value from the ground — done, and it went one level deeper
   than the review asked.** The face plane now sits in the corpus's 0.25–0.31×-of-sky
   band at the intimate framing on both duellists (§2.2), and getting it there forced
   the §2.2 amendment's own debt: **the ink floor is now a ratio to the ground**
   (fraction 0.12–0.14 of local sky, measured on all six family-B duellists, regions
   in STYLE.md §2.2 and `Palette.INK_BLACK_DUSK`), enforced in `ink_resolve.frag` as
   a ground-scaled clamp that is the identity on Family A. Without it the socket
   could not be darker than the plane — both were pinned at the paper-ground
   constant — and no face mark could print more than 4 levels from its own field.
   `FaceValueTest` holds the corpus band on all six reference heads, the delivered
   band on both duellists across four bloom-free frames, and the red half:
   `thePassOneDecalFailsThisBand` re-reads pass 1's tracked graded capture through
   the same arithmetic and proves it fails (foe 1.16–1.59× sky, bloom-free frame).
2. **Invert the eye — done.** The eye bone moved up and in
   (`SamuraiRig.EYE_BIND_DX/DY`, with the pass-1 offsets recorded there: the old eye
   sat at nostril height 3 px inside the silhouette — the review's own finding on the
   foe, present on the hero too). The socket is now a genuine shadow *below* the
   plane (16–19 against 24–27), the sclera a small pale band, the iris a dark ink
   anchor, and the specular the one bright point. `theEyeIsASocketBeforeItIsASpecular`:
   hero eye mean **18.9** against plane **25.5** (ratio 0.74 vs the 1.35 ceiling
   pass 1 failed at 1.37), specular max 126 — through `x434..444 y295..305` /
   `x425..434 y297..308`; foe 0.48 through its mirrored boxes. The foe's plane box
   was facing-corrected in the same change (§5.4).
3. **Remove the hard edges / feathered skin — done as structure rather than blur.**
   The profile is now the corpus's four value events (measured on ref3 dark,
   `x145..184 y182..231`: plane 12–24, lit break 45–61 two-to-four px wide, dark
   contour line 33–39, sky), built as rails with wet 1.5–2.5 px transitions. The
   mouth is a soft parting-plus-moustache cluster on a lit lip band, not a detached
   rectangle: the parting rides head 0.75/jaw 0.25 so the jaw channel carries it.
4. **The criterion as a band on the whole family — delivered by the interrupted
   half-pass and kept.** `theCorpusPassesThisGuard` iterates all six family-B heads
   with floors and ceilings at both bases; `theHeadRegionsAreNoLongerTheLattice`
   asserts both duellists, both bases, both edges (density floor = the same head's
   own bareface control + 1.0; ceilings from the corpus spread). Both were red on
   this worktree at resumption and went green by the work in §2, not by any
   threshold moving. The one threshold-adjacent change is §5.5, and it tightened.
5. **The run hole / base 2 — delivered by the interrupted half-pass and kept.**
   The reviewer's ±2-px/period-5 jittered lattice is a checked-in `FacetsTest`
   exhibit; every face guard asserts base 2 beside base 1; the narrowed scope
   ("no near-single-pixel, straight-or-lightly-jittered axis-aligned lattice") is
   restated at the use site, with the density floor named as the defence against
   the jitter class (which lands at 2.6/1000 base 1, below every floor here).
6. **The gaze guard — rebuilt twice, because the first rebuild's red run did not
   reproduce (§5.2).** The shipping version probes mid-schedule states (replay to
   anchor + 0.8 s), compares both channels against expectations from the schedule's
   own anchors, requires the anchor to sit within a tile of the opponent's head,
   and asserts its own discriminating power (at least one sampled expectation ≥ 2×
   tolerance away from the constant stare). Observed red against review §6.2's
   exact sabotage: *"body 0 at t=0.8: gazeX 1.0 vs the schedule's
   -0.3071025013923645"*; restored; green. Visibility: the write scale is 0.013
   (2.9 px per half-swing at the parry framing), held by
   `theGazeMovesTheEyeByMoreThanTwoPixels`.
7. **The head non-determinism — re-characterised, bounded, and it nearly convicted
   this pass (§3).**
8. **The face gallery — shot** (`s3b-p2-gallery`, `face-gallery` scene, six seeded
   heads in one frame). Honestly graded: at the gallery's ~40 px heads the variety
   reads mostly in hair fall, beard mass and profile line; it satisfies the
   review's ask (variety on screen, not in a table) and no more.

---

## 2. The numbers, before and after

### 2.1 The two acceptance guards, red at resumption, green by measurement

`Facets.measure`, licensed-light-masked, through the derived head boxes on frame 11;
"floor" is the same head's bareface control + 1.0, ceilings from the corpus band.

| reading | red state (resumption) | delivered | floor | ceiling |
|---|---|---|---|---|
| hero base 1 | 4.46 | **5.00** | 4.92 | 10.6 |
| hero base 2 | 11.63 | **14.06** | 10.47 | 26.0 |
| foe base 1 | 3.92 *(also under floor — masked; §5.3)* | **4.33** | 3.97 | 10.6 |
| foe base 2 | 16.22 | **18.52** | 15.74 | 26.0 |
| hero longest run | 18 | 18 | — | 30 |
| foe longest run | 28 | **14** | — | 30 |
| hero eye mean / plane | 1.354 | **0.74** | — | 1.35 |
| foe eye mean / plane | (vacuously green; §5.4) | **0.48** | — | 1.35 |

Where the density came from, since the review will ask: not from interior shading —
the resolve's anti-seam averaging blurs value-channel steps to 2–3 px *by design*,
so wetness structure counts at base 2 only (it does: the face adds +4.6/1000 there).
Base-1 marks are **coverage edges on contrasting ground**: the contour line against
sky, the brow and moustache and lip parting as ink strokes at L 12–16 on washes and
lit bands at 33–75, the sclera/lash pair, the crown silhouette given to the skin's
own spline rather than the hair fringe (rOut 0.164/0.159 — the fringe's ragged edge
shredded the head's one long clean boundary into 4–5 px fragments).

### 2.2 The face plane, against the ground (review §4's table, retaken)

Face-plane mean over local-sky mean, boxes derived per frame by `FaceValueTest`:

| subject | pass 1 (review §4) | delivered | corpus band |
|---|---|---|---|
| hero, intimate framing | 0.56 | **0.26–0.28** across the window (0.27 on the graded frame) | 0.25–0.31 |
| foe, intimate framing, bloom-free frames 0/16/17/22 | 1.36 at contact | **0.31–0.32** | 0.25–0.31 |
| foe, planning framing (blob test) | 1.20 mean; **p05 0.99×, max 2.28×** | mean 1.02 (box is mostly sky at 25 px of head); **p05 0.29×, max 1.46×** | — |

The planning-framing numbers are through the review's own boxes
(head `x205..249 y518..551`, sky `x300..339 y500..539`, frame 0 of the wide pair)
and the mean row is stated with its caveat: at 25 px of head the 44×33 box is
mostly sky, so the discriminating statistics are the percentile and the max, and
both are asserted (`theWideFramingHeadIsNotAWhiteBlob`) against the bareface
control (p05 45.1 → 21.5; the face darkens the head it used to brighten).

### 2.3 The contact frame, bloom convention (§11.3)

On frame 11 the foe fights inside §7.3's licensed clash light and its plane box
reads **1.374** — numerically pass 1's decal, mechanically its opposite (glow over
dark skin). No contact-frame value ceiling is asserted for the foe, and the
convention is written at the assertion site. The instrument-side equivalent: the
facet mask now strips *plain-bright* (L > 165) as well as cool-bright, because the
clash core is warm and the old mask charged the figure for the bloom's own falloff
rail (`Facets.bladeMask`'s doc carries the finding).

---

## 3. Determinism: the claim, the correction, and the bound

Same command, same commit, twice, final pair:

| pair | identical frames | total px | worst frame | graded frame 11 |
|---|---|---|---|---|
| `s3b-p2-parry-contact` / `-repro` | **0 of 24** | 19,768 | 2,816 px, max delta 99 | 1,900 px, max 33 |
| `-bareface` / `-bareface-repro` | 21 of 24 | 27,967 | 11,316 px, max 137 (the fleck class) | identical |

Pass 1 recorded this as "±1 LSB, invisible"; the review measured max 78 and called
the record a false reassurance. Confirmed and re-confirmed here — the face's cost
is per-frame noise up to ~2.8 k px at the heads, the bareface's is the pre-existing
whole-fleck class. Not bisected (the quarter-res blur/resolve passes remain the
suspects); **bounded** instead: `theHeadNoiseIsBoundedAcrossReruns` reads the
tracked contact/repro frame-11 pair and holds differing px ≤ 6,000, max luma delta
≤ 120, and — the part that matters — **the facet densities must agree across the
pair within 0.55 (base 1) and 1.5 (base 2) per head**.

That last clause exists because the noise nearly convicted this pass: the first
"final" delivery read hero base-1 **5.00** while its same-command twin read
**4.60 — under the floor**. The acceptance was sitting inside its own apparatus's
noise, which is §11.2b(g) verbatim, and it was fixed in the subject rather than
the criterion: the boundaries whose steps hovered at 8–9 levels (the exact
threshold) were pushed to 11+ (`skin.base` +10%, the break falloff one step
brighter), after which **both rolls read 5.00 and 4.33 identically**. The flipping
runs were found by diffing the run inventories of the pair — the method is one
python block and worth keeping.

---

## 4. Contract amendments, named loudly

1. **The ink floor is a ratio to the ground** (`ink_resolve.frag`):
   `clamp(ink, INK_FLOOR * clamp(groundLuma / 0.775, 0, 1), 0.96)`. Identity on
   Family A (clamp reaches 1 on cream paper); on the dusk stage the floor is L
   10–12. Materials authored *at* the old constant print unchanged — verified: the
   bareface control is bit-identical around this change (3.921/9.46 through the
   hero box before and after). Only sub-floor authoring reaches the new range, and
   only the face materials author there today.
2. **`Palette.INK_BLACK_DUSK`** (`#0A0D14`, L 12.9): the face's stroke ink,
   carrying the measured fraction and its six regions in its doc.
3. **`SamuraiRig.EYE_BIND_DX/DY`** are named constants shared with
   `FaceValueTest`'s head-rotation recovery (the old hardcoded pair rotted once
   already).
4. **The skin ramp re-anchored per figure** (`Figure.dark()/pale()`): `deep` is no
   longer the plane but the register *below* it (socket, contour line, under-jaw,
   ~0.18× sky); the plane lives on the wetness ramp at ~0.55, **under the pool
   term's saturation knee, which is ~0.60, not ~0.85** — measured across
   `try16/17`: wetness 0.80, 0.86 and 1.0 all print the deep register. Anyone
   authoring skin wetness should know that number.
5. The face meshes gained: the contour-line rail, a break rail with corpus
   lighting (lit at ridge/nose/lip/chin, dark across socket and bridge), an
   authored hairline strip (4b.1's "hard wet edge", under the sim's mass), a
   moustache, an elongated topknot, two nape strands and five crown wisps. The
   hero's contour table changed at the crown (rOut 0.158/0.153 → 0.164/0.159 and
   station 42° to ×1.065) — **the hero silhouette is deliberately not bit-identical
   to pass 1's.**

---

## 5. Claims that did not survive this pass's measurement

Every pass of this project has found at least one; this pass found six, and two of
them are about its own first half.

1. **Pass-1 debt §2.5 / §6.4, "±1-LSB, invisible"** — refuted by the review and
   re-refuted here with this pass's own pair: max delta 99, 2.8 k px on one frame
   (§3). The disposition changed from "nobody; obey it" to "bounded in the suite".
2. **The interrupted half-pass's gaze javadoc claimed a red observation** —
   *"fails with 'body 0 at t=1.42: gazeY 0.0 vs the schedule's -0.148'"* — **that
   does not reproduce.** Under the review's exact sabotage the suite stayed green
   (427-equivalent, 0 failures), measured on this worktree before the rebuild:
   the guard sampled only the end of the score, where clamp() saturates the X
   expectation at the sabotage's own value and the last anchor's Y expectation
   sits inside tolerance. The transcript that could say whether the red run ever
   happened is lost; what is knowable is that the claim was false at the committed
   state. The rebuilt guard's red run is quoted in §1.6 and its discriminating
   power is itself asserted.
3. **The resumption brief's "exactly two tests are red, the hero short by 0.46"**
   understated it: **the foe also failed the density floor** (3.921 against
   3.975), masked because the guard iterates hero first and JUnit stops at the
   first failure. Worth recording as a pattern: a loop of assertions reports its
   first defect, not its defect count.
4. **Pass-1's foe eye assertion was vacuously green**: its "cheek behind the eye"
   plane box used the hero's facing for both duellists, which on the left-facing
   foe landed in front of the face and read part sky — a 1.35× ceiling against a
   sky-bright denominator asserts nothing. Fixed facing-aware; the foe now passes
   against a real cheek at 0.48.
5. **The class doc's planned foe contact-frame ceiling was impossible as
   designed**: pass 1's bare-skin 1.36 and this pass's through-the-bloom 1.374
   are indistinguishable to a box mean, so a ceiling there discriminates nothing
   (§2.3). Replaced by the bloom-free band plus the checked-in pass-1 red exhibit
   — a strictly stronger pair than the planned assertion.
6. **The resumption brief cited "MEASUREMENT.md"** — no such file; §11 lives in
   STYLE.md. Recorded only because the brief asked for everything that does not
   reproduce.

---

## 6. Every new or changed guard, and its red run

| guard | observed red | message seen |
|---|---|---|
| `theHeadRegionsAreNoLugerTheLattice` *(sic — see below)* | inherited red at resumption, on both duellists | *"hero: base-1 density 4.46 vs bareface 3.92…"* |
| `theEyeIsASocketBeforeItIsASpecular` | inherited red at resumption | *"hero: the eye region (40.56 through x438..448 y300..310) is brighter than the face plane (29.95)"* |
| `bothDuellistsLookAtEachOtherThroughTheContact` (rebuilt) | sabotage of review §6.2 | *"body 0 at t=0.8: gazeX 1.0 vs the schedule's -0.307…"* |
| `theDeliveredFacePlanesSitInTheBand` | red by construction against pass 1's pixels | `thePassOneDecalFailsThisBand` is the permanent exhibit (foe 1.16+ on a bloom-free frame of the tracked pass-1 capture) |
| `theWideFramingHeadIsNotAWhiteBlob` | red by construction against pass 1 | pass-1 wide frame reads p05 at 0.99× sky where the floor demands ≤ 0.45× |
| `theHeadNoiseIsBoundedAcrossReruns` | its metric-agreement clause was red in fact on the first final pair (5.00 vs 4.60) | §3 |

(The first row's name is `theHeadRegionsAreNoLongerTheLattice`; the misprint is
left here so nobody search-and-replaces the table into pretending it ran.)
The two inherited-red guards were left red across the interruption on purpose —
"the correct state for a guard to be in" — and this record confirms they were
turned green by captures, not edits: no floor, ceiling, tolerance or box in either
test changed between the red state and the green one, except the foe plane box fix
of §5.4, which made its assertion *harder*.

## 7. What System 3b pass 2 does not deliver (debt)

1. **The hair does not know about the face — now the single largest head item.**
   The mass swallows the authored hairline, the crown wisps root under it, and the
   bareface heads read 3.0–3.9 facet density where corpus heads read 6.8–9.6
   before any face is drawn: the remaining distance to the corpus's absolute band
   is hair marks, not face marks. The generator still has no `hairMass` axis.
   → **a hair pass (3c or dedicated)**, unchanged from pass 1's item 5, with the
   evidence now quantified.
2. **The garment and hair masses still author against the paper-ground floor
   constant** and print ~2× the corpus's darks on the dusk stage. The fraction
   and the mechanism are in STYLE.md §2.2; applying them to the figure at large
   is a re-grade of every dusk capture and belongs to a pass that owns that.
   → **3c / System 5**, named in §2.2 itself.
3. **Faces exist in the duel scenes only** (`LaneScene` wiring is System 5's
   one-line adoption). Unchanged. → **System 5**.
4. **Ear, discrete freckles, scar tone, iris hue** — pass-1 item 6, unchanged,
   below 4b.3's own hierarchy. The freckles still ride the stain granulation.
   → **a later face pass, if any**.
5. **Per-frame coverage of the lattice guard**: frame 11 only, by the review's own
   acceptance ("one frame is sufficient once the criterion is a band with both
   edges"). The band now has both edges on both duellists. → **accepted**.
6. **Both instrument holes** (3-px risers; run-jitter), named at the use sites,
   never carrying an acceptance alone. → **accepted as shipped**.
7. **The determinism is bounded, not bisected** (§3). The bound is the tracked
   pair; the suspects are named. → **whoever next needs bit-identity at a head**.
8. **The gallery reads as a variety exhibit only at close inspection** (§1.8).
   → **the reviewer may ask for a tighter framing; it is one parameter.**

## 8. What must not regress

- The face plane is a *ratio to its sky*, asserted on both duellists on four
  frames, with pass 1's decal as the permanent red exhibit.
- The socket is darker than the plane; the eye degrades to dark-anchor-plus-
  specular in that order (4b.4), asserted on pixels.
- The ink floor scales with the ground; Family A is bit-identical by the clamp.
- The acceptance metrics agree across a same-command rerun pair — a criterion
  inside the noise floor is the §11.2b(g) defect and the suite now says so.
- Skin stays exempt from the dissolve; the beard and the nape strands are hair
  and may fray; the brown shadow remains unreachable by construction.

## Commands

```
./gw capture -Pscene=duel-parry          -Pout=out/captures/s3b-p2-parry-contact -Pframes=24 -Pcols=6 -Pstart=1.42 -Pstep=0.0167 -Pw=960 -Ph=720
./gw capture -Pscene=duel-parry          -Pout=out/captures/s3b-p2-parry-repro        (identical: the dynamic control)
./gw capture -Pscene=duel-parry-bareface -Pout=out/captures/s3b-p2-parry-bareface     (absent-subject control)
./gw capture -Pscene=duel-parry-bareface -Pout=out/captures/s3b-p2-parry-bareface-repro
./gw capture -Pscene=duel-phrase          -Pout=out/captures/s3b-p2-wide -Pframes=6 -Pcols=3 -Pstep=0.3 -Pw=960 -Ph=720
./gw capture -Pscene=duel-phrase-bareface -Pout=out/captures/s3b-p2-wide-bareface
./gw capture -Pscene=duel-knockback       -Pout=out/captures/s3b-p2-knockback -Pframes=24 -Pcols=6 -Pw=960 -Ph=720
./gw capture -Pscene=face-gallery         -Pout=out/captures/s3b-p2-gallery -Pframes=1 -Pcols=1 -Pw=960 -Ph=720

./gw analyse -Pargs="facets out/captures/s3b-p2-parry-contact --frame 11 --rect 370,277,86,86 --mask-blade"
./gw analyse -Pargs="facets out/captures/s3b-p2-parry-contact --frame 11 --rect 568,283,86,86 --mask-blade"
./gw test --rerun-tasks
node tools/check-progress.mjs
```

The `s3b-p2-try1..32` directories are the iteration record of both halves of this
pass and stay on disk untracked; nothing in the suite reads them. The tracked
frames are the ones the tests read: the contact/repro/bareface frame 11s, contact
frames 0/16/17/22, both wide frame 0s, pass 1's frames 11 and 17, and the three
matched-scale references.
