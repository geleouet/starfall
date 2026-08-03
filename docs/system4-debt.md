# System 4 — standing debt

**Status: FAILED at pass 1.** Pass 2 is in progress against this document.

Source: the independent review of pass 1, which re-shot every capture on the fixed harness
before grading and used a `rig-bindpose` capture at the same harness as a stationary control
for every motion statistic.

The verdict, and it is split:

> **The phrase is poetic. The interaction is not merely correct — it is not correct. It is
> unstaged.** Poetry is not an available verdict on a beat that has not happened yet.

> Fix one aiming bug and one corridor, and this becomes the first pass in the project that
> could answer §0 with a yes.

---

## The pass: there is no parry in `duel-parry`

Blade-to-blade minimum separation, measured by segmenting both blades as connected
components of *cool bright* pixels (b−r > −6, luminance > 212 — paper is warm at r−b = +24,
so this separates blade from paper and from the warm clash core):

| frame | t | min separation |
|---|---|---|
| 06 | 1.520 | 101.6 px |
| **07** | **1.537** | **98.4 px** ← closest approach |
| 09 | 1.570 | 155.7 px |
| 15 | 1.670 | 184.4 px |

**The closest the blades ever come is 98.4 px on a 462 px figure — 21% of a body height, a
full head and shoulders of clear paper.** They approach, stop approaching at t=1.537, and
diverge monotonically thereafter.

**And the clash bloom fires two frames after closest approach, 0.05 s into the divergence.**
Its centroid at peak is **36 px from the attacker's own blade root and 223 px from the
defender's blade.** It is sitting on the attacker's grip.

**The staging did its job.** The debug overlay shows the two `Meeting` targets as discs 32 px
apart, exactly where the bloom lands, with leader lines into both bodies' chains. The
schedule names the point on each body correctly. **The defender's blade is not on the chain
that receives it** — it is drawn from a grip on the far side of its own torso, pointing down
and away, through the entire contact span.

So §7.2 is not mis-shaped, it is **unexercised**. There is no deflection curve because there
is no deflection, and no collision either. What there is is worse than both: **a light that
asserts an event the picture does not contain.** The eye goes to the brightest thing in the
frame and finds nothing there.

The pass's own claim — *"the blades meet, slide through the contact span, and part"* — is
false in delivered pixels. The *arm* does three-target work and the chain does move
continuously through the recovery, but **three targets through one chain is a description of
the solver, not of the picture.**

**Acceptance for pass 2:** minimum blade-to-blade separation ≤ **2% of figure height** for
the whole contact span, measured as above.

## The corridor: two bodies merge, worst exactly where it matters

Widest run of columns carrying zero ink between the two bodies, 0.85×paper threshold with a
3×3 opening, normalised by the frame's own figure height:

| capture | frames | zero corridor | single connected blob | median corridor |
|---|---|---|---|---|
| knockback | 37 | 0% | 0% | 0.114 |
| phrase | 98 | 20% | 4% | 0.098 |
| **parry contact** | **24** | **67%** | **25%** | **0.000** |

Through the contact span — **0.28 s, sixteen consecutive frames — there is not one column of
paper between the two figures.** At frames 06-11 they are literally one connected ink
component: 76,679 px, **78% of all ink in the frame.**

**The reference, measured at matched scale:** image 3's clear gap is **42 px at its narrowest
(6.3% of figure height)**, 8-11% at the hands, 126-244 px elsewhere. **It is never zero.**
Note this *corrects* pass 1's own phrasing — "a body-width of clear paper" overstates the
reference — but its conclusion was right and the capture is far outside it.

**This destroys the thing two figures were built for.** Two silhouettes that touch are one
silhouette. Family B works because the eye reads *body — gap — body* and then finds the
blades bridging the gap. Here it reads one mass with a light in it.

**Acceptance for pass 2:** a checked-in guard, tested in the harness against a two-figure
frame: **the corridor must never be zero and must not fall below 6% of figure height.**

## The second body is a stain, not a silhouette

Absolute rectangles, paper 217.5:

| region | rect | mean ink luminance | minimum |
|---|---|---|---|
| hero torso | x300..470 y300..420 | 90.2 (0.41× paper) | 25.7 |
| hero skirt | x300..470 y470..610 | 75.5 (0.35×) | 26.5 |
| defender torso | x600..760 y340..460 | 149.6 (0.69×) | 30.5 |
| **defender skirt** | **x590..760 y470..600** | **119.9 (0.55×)** | **87.8** |

**Nothing in the defender's lower body ever reaches below 0.40× paper.** Family B's law is
that *both* bodies read as near-black ink silhouettes with just enough interior modelling to
find the face and hands — and image 3's white-clad duellist obeys it: pale above the sash,
**near-black hakama below**, dark collar, dark hair on the shoulder. The capture's defender
is pale from hair to hem with an ochre belt and nothing else, resolving as three marks.

Nothing below System 4 specified a second-figure colourway; this is System 4's choice. And it
makes the corridor problem worse than it needed to be: **a body at 0.69× paper has no edge
with which to defend itself against a neighbour.**

## Knockback is not a carry — the figures converge

Edge-to-edge gap over the settled window, static camera:

| t | 0.567 | 0.701 | 0.834 | 0.968 | 1.102 |
|---|---|---|---|---|---|
| gap / hero height | 1.58 | 1.43 | 1.27 | 1.13 | 1.06 |

Monotone closure, 169 px in 0.535 s, continuing to 0.91 hero-heights by t=1.90. **There is no
frame in which the struck body moves away from the shover.** §7.2's "knockback is a drift
arriving over ~0.8 s" has no displacement to be a drift of.

The pass's tip-to-root offset measurements may well be correct — §7.2's own correction says
streaming ahead is a displacement, not an arrival — but **the body is the other half of that
sentence.** A garment thrown forward on a body being walked *towards* is not a knockback.

## §7.3's ink bloom does not exist in 552 delivered frames

The vocabulary is built and dispatched, and the debug timeline confirms six contact spans in
the phrase — drawn as **bars rather than ticks**, which is §7.1's "contact is a span" honoured
in the instrument itself, and that is good work.

But across 552 frames: **no ink bloom anywhere.** Dark-ink area tracks camera zoom smoothly
and never spikes; on the parry it *falls* through the contact. No warm bloom outside the
parry scene. **Three scenes, and not one puts an impact mark on the paper where an impact
happens.** Ship a scene that lands a blow.

The value floor held at 25.7 on every one of the 552 frames — blooms lift and never multiply.

---

## What passed, and must not regress

### The phrase — poetic, and the first real continuity result in the project

Instrument: segment the hero (largest connected component below luminance 95), resample its
silhouette into its own bounding box at 64×64, difference consecutive frames. **Scale- and
translation-invariant, so the camera cancels.**

- **Static control** (`rig-bindpose`, same harness, 24 frames): inter-frame change **max
  0.00009**.
- **The phrase, 417 inter-frame steps over 6.95 s: minimum 0.00229.**

**The hero's silhouette never comes to rest, at any step, in 6.95 seconds, at 25× the
control's noise ceiling.**

Hip participates, measured against the hero's *own planted foot* and normalised by its own
bounding box — camera-free by construction: hip excursion **0.216** of figure height, chest
**0.202**, hip/blade **17.1%** — which independently corroborates the pass's 17.7% by a
different route. System 2 was failed at 1.5%. Feet stay planted under a moving hip.

**Two qualifications that must be carried forward:**

- **The 17% is a whole-phrase aggregate inflated by re-planting between clauses.** Over
  sliding 0.25 s windows where the blade actually sweeps — which is what §7.0.1 asks about —
  the hip's median is **4.4% of the blade's** and the chest's 9.9%. Ten pixels of hip travel
  during a strike is visible and is not System 2's frozen torso, but "17.7%" read as "the
  body drives the limb" overstates it fourfold. **The local number is the honest one.**
- **One hole:** frames 312-330, **0.317 s**, the hero's hip and chest hold within 2 px and its
  blade within 8. The frame is not dead because the enemy moves, so this is not "five events"
  — but System 2's E1 applies: *the poetry is an event rather than a condition*, and a held
  pose must still carry weight.

### The held breath — the most tasteful thing in the codebase

Instrument, and it costs nothing: capture the debug overlay's schedule cursor at uniform
wall-clock steps; a soft time ramp shows up directly as a dip in schedule-seconds per frame.

Measured **0.857× sustained for ~0.12 s**, beginning 0.08 s after the clash. Control
(`duel-knockback`, same instrument): perfectly uniform, no dip — so the parry's dip is signal.
**§7.3 asks ~0.85× for ~0.25 s: the depth is exactly right and the clock never stops.** Read
0.12 s as a floor, since the eased shoulders sit inside the quantisation. Lengthen it, and
emit one on the knockback too.

### Also holding

- **Blade trail**: one connected smooth curved ribbon, no strobing, no kinks, at the fastest
  tip speed in the corpus. §5's "must curve" and §7.2's "smear, not strobe" both pass.
  *But* it occupies 1.27 figure-heights of near-closed dome and persists eight frames after
  the blade has left — it reads as a moon, not a stroke. Cut its extent and taper it.
- **Shed flecks**: localised to the contact box and correctly delayed, peaking 0.05 s after
  the bloom. About five flecks — a whisper. The Director is honest about why: `u_dissolveBias`
  is per-draw over a whole mesh, so §7.3's "pushed *locally*" cannot be honoured.
- No snapping: the two largest pose discontinuities are segmentation artefacts; the picture
  is continuous through both.

---

## The blocking gap: the region set fails **silently** on two-figure frames

`analyse regions` resolves `figure` to a box **spanning both bodies**, and then resolves
`head` onto the *defender's* hair, `torso` straddling both torsos and the gap.

**And it fails silently, which is worse than failing.** `analyse track ... --anchor hips` runs
happily and reports *"hem trails hips by +6.52 frames (+0.109 s)"* — **a number inside §7.1's
4-8 band, which a future pass would quote.** It is only not obviously garbage because the tool
resolves its boxes against **frame 0**, the one frame of that window where the bodies happen
to be separated. `analyse figure` reports height 462 on frame 0 and 478 on frame 10 of the
same directory and says nothing about why.

**Until this is fixed, no §7.1 statistic can be quoted on any System 4 scene**, which means
the next pass cannot grade its own overlap. `track` refuses without an anchor; it must also
**refuse when the detected ink resolves into two components each above some share of the
total and no figure has been named.** That is §11.2b(e) again, and it is one assertion.

## The other reported gaps — verified

Four are real and do not block: the trunk anchor at hip height against a chain ending at the
neck; no directive translates a body (does not block *today* because the pelvis is carried by
hand, but close it before the lane gets long); foot anchors on the ground against leg chains
ending at the ankle; and the tile width, which is quantified above and is the most damaging.

**And one that should become a rule:** a pixel timing claim is not separable from the camera
on any scene obeying §9. The hero's bounding-box height ranges **4.08×** across the phrase.
Every statistic in this review is either a ratio to the figure's own span or to a control at
the same harness, for exactly that reason.

## A process failure worth naming

**Systems 1, 2 and 3 each left a debt record. System 4 left two paragraphs in
`progress.html`.** Its log claimed "eight things the layers below failed to provide" and named
four; "six places the rubric is wrong" and named two.

**A gap reported where the next pass will not read it has not been reported.** Same class as
§11.2b(e): the discipline existed in someone's head rather than in the artefact. This document
is the fix, and it costs nothing.
