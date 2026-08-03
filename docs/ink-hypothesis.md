# The ink hypothesis — stains, gradients, and whether ink should be a verb

**What this is.** An experiment, not a pass. The owner asked: *do the stains and gradients
not harm the aesthetics and the readability? Shouldn't we have something more classical,
and keep the ink effect more for the movements?* This document tests that against the
corpus and against a working prototype, shot as a matched pair against the current build.
Nothing here was integrated; `STYLE.md`, `MEASUREMENT.md`, `combat-design.md` and
`STORY.md` are untouched, and every rule change proposed is quoted before/after in §7.

**Scope note.** The brief named `docs/system3b-debt.md` and `docs/system3b-review-pass1.md`
as reading; neither exists in this worktree (checked by glob and by grep across `docs/`).
The `s3b-p1-*` captures exist and were used.

---

## 1. Verdict, one paragraph

**The owner is partly right — right about our figure, wrong about the corpus's mechanism,
and the change buys real legibility but not the part count.** The corpus does *not* keep
its figures classical at rest and spend ink only on movement: image 1's *standing* figure
detonates its cloak into ink clouds, and at matched scale the family-B silhouettes measure
*at least as broken* as our capture's (mask-level breakage 37–51% of perimeter against our
36.5%; contour tortuosity 1.49–1.62 against our 1.44 — §3, regions there). What the corpus
actually does is **structure** the breakup: trailing side > leading side in five of six
duellists, hem and hair loudest, and the face, hands, grips and fittings *never* break at
any scale — its clean edges are **soft-but-whole**, its broken edges are peripheral. Our
delivered figure instead applies a uniform grime — dissolve floors on far limbs, boundary
wander scaled to strip width everywhere, interior tooth holes, scattered mid-size stains —
which breaks the silhouette **isotropically**, and that is what shreds the body into the
"jumble of disconnected dark flaps" the audit named. The prototype that pulls
fragmentation back to where the corpus breaks (hems, trailing edges, hits) while keeping
every edge soft restores the corpus's front/back gradient (leading 1.15 vs trailing 1.54),
transforms the small at-rest figure from a debris cloud into a person
(`inkhyp-sidebyside-stand.png`), and adds about **+3 readable parts** at the duel framing
(12–14 against the current 9–12) — but the reference resolves ~24–25, and the remaining
~10 parts are **mesh** (hands, grip, guard, face in profile, feet, fold structure — debt
D1), which no ink setting can supply. Adopt the structural half of the hypothesis; do not
expect it to close the count.

---

## 2. What was run

All captures at 960×720, `harness=f0ad18994eec` (same as every `s3b-p1-*` and `s4-p5-*`
capture, so cross-comparisons are in scope per §11.2b(d)). The classic variant is a
shader-side switch (`ink_skin.frag` `u_classic`, wired `-Pclassic=1` → system property
`starfall.inkClassic` → `InkSkinnedRenderer.CLASSIC`); it is deliberately **not** a
`CaptureSpec` field because `CaptureSpec` is in the `HarnessId` apparatus digest and the
variant is subject, not apparatus. Each classic capture dir carries a `variant.txt` saying
so, since `capture.txt` cannot record it.

| capture | what |
|---|---|
| `inkhyp-parry-current` | `duel-parry`, frames 24, start 1.42, step 0.0167 — shot **before** any edit; the baseline |
| `inkhyp-parry-bare` | same, **after** the edit, variant off — the null control |
| `inkhyp-parry-classic` | variant on, first iteration (band 0.09) — superseded, kept for the §6 cost-1 measurement |
| `inkhyp-parry-classic2` | variant on, feather band widened to 0.15 — **the graded classic capture** |
| `inkhyp-stand-current` / `-classic` / `-classic2` | `duel-parry`, start 0.0, 6 frames at 0.08 s — the figures at rest, small |
| `inkhyp-knockback-current` / `-classic` | `duel-knockback`, 24 frames — proves ink-as-verb survives |
| `inkhyp-sidebyside-hero.png`, `-foe.png`, `-stand.png` | matched-scale sheets: reference / current / classic |

**The control was run and it holds.** `analyse diff inkhyp-parry-current
inkhyp-parry-bare`: **21 of 24 frames bit-identical**; the three differing frames total
5,269 px, all inside `x513..646 y364..475` — the shed-fleck/clash box that System 4 debt
§7.1 records as run-to-run non-deterministic at exactly this box and mechanism (a discrete
visibility cull on a float-accumulated clock). No silhouette pixel differs, so the switch
is inert when off. Every classic term sits behind `if (u_classic > 0.0)` — the
`ink_resolve.frag` `u_haze` pattern — because System 4 §4.4 measured that even a
`mix(x, y, 0.0)` rewrite is not bit-exact on this driver.

Reference figures at matched scale: `ref3-matched-378.png` as delivered; images 4 and 5
downscaled by the same 612/1088 factor (one family, one framing; the resulting figure
boxes are printed with every number below).

---

## 3. Question 1 — where does the corpus actually break its edges?

Two instruments, both run on all three family-B images, both duellists, and on the
capture pair. Ink convention throughout: Rec.709 luminance below 0.60 × the row-local
background (median of the frame's outer 70 columns), the System 4 reader convention.

**(a) Mask-level breakage** — fraction of the closed silhouette's boundary where a
radius-4 closing had to bridge gaps (≥3 bridged px within radius 4). Zones: hem = lowest
25% of the figure box; leading/trailing split at the ink centroid by facing.

| figure (region) | all | hem | trailing | leading |
|---|---|---|---|---|
| ref3 dark `(10,130,260,560)` on ref3-matched | 47.9% | 52.2% | 37.2% | 53.5% |
| ref3 pale `(250,130,460,560)` | 51.0% | 63.6% | 50.7% | 45.6% |
| ref4 dark `(30,130,250,560)` ×0.5625 | 43.6% | 47.2% | 41.4% | 42.1% |
| ref4 pale `(250,130,440,560)` ×0.5625 | 39.5% | 43.6% | 32.7% | 42.9% |
| ref5 dark `(40,150,260,560)` ×0.5625 | 38.9% | 41.1% | 32.2% | 42.6% |
| ref5 pale `(260,150,450,560)` ×0.5625 | 37.2% | 35.7% | 43.0% | 32.3% |
| **hero f011 current** `(240,260,500,660)` | 36.5% | 43.6% | 37.1% | 30.3% |
| **hero f011 classic2** (same box) | 23.1% | 28.6% | 23.4% | 17.9% |

**(b) Contour tortuosity** — boundary length of the lightly-closed mask over the boundary
length of the same mask gaussian-smoothed at σ=6 px. 1.0 = smooth contour; high = torn
flaps. Same regions.

| figure | all | hem | trailing | leading |
|---|---|---|---|---|
| ref3 dark | 1.53 | 1.35 | **1.81** | 1.51 |
| ref3 pale | 1.50 | 1.46 | **1.71** | 1.39 |
| ref4 dark | 1.57 | 1.50 | **1.70** | 1.54 |
| ref4 pale | 1.50 | 1.45 | **1.64** | 1.42 |
| ref5 dark | 1.49 | 1.35 | 1.50 | **1.59** |
| ref5 pale | 1.62 | 1.61 | **1.74** | 1.51 |
| hero current | 1.44 | 1.40 | 1.46 | 1.45 |
| hero classic2 | 1.34 | 1.29 | **1.54** | 1.15 |

**Findings, in order of how much they surprised me.**

1. **The naive form of the hypothesis is false: the corpus breaks *more* of its perimeter
   than we do, everywhere, including leading edges.** Both metrics say so on all six
   reference figures. §3's "the silhouette breaks" is not what the corpus lacks.
2. **But the corpus's breakage has a *grain* and ours does not.** Trailing > leading in
   five of six reference figures (ratio ~1.1–1.3× on tortuosity); the current capture is
   dead flat (1.46 vs 1.45). The corpus's leading-edge breakage is *soft feathering and
   fine speckle* — a wet contour crossing the ink threshold over 5–15 px — while ours is
   mid-scale (5–30 px) torn flaps distributed isotropically. The metrics partly conflate
   the two (caveat below), which is exactly why the reference scores "broken" on edges
   the eye reads as whole: **a soft edge and a broken edge are different instruments, and
   §3 currently owns only one word for both.**
3. **The classic prototype restores the grain** (trailing 1.54 / leading 1.15) **but
   overshoots the total** (1.34 against the corpus's 1.49–1.62). The corpus's overall
   wildness comes from things the prototype does not add: curling smoke streamers off the
   silhouettes (proposals B6), splash at the feet, thrown hair mass. Cleaning the body
   without adding the smoke leaves the figure *quieter than the corpus*.

**Metric caveats, per §11.2b(g).** Neither statistic's null case was run against a
synthetic; both would read ~0 on a hard-edged rectangle by construction — read that
phrase as MEASUREMENT.md instructs. The breakage metric counts threshold speckle on soft
gradients as "broken", so it overstates the reference; the tortuosity metric is the more
honest of the two for "torn flaps". They are diagnostics supporting the eye here, not
acceptances, and the side-by-side sheets are the primary exhibit.

---

## 4. Question 2 — what do the stains do to readability?

**On the dusk stage the corpus barely stains at all.** Warm-in-ink census (r−b > 30
inside the ink mask, so the coral sky is excluded), same regions as §3:

| figure | warm px as % of ink px | regions ≥ 8 px |
|---|---|---|
| ref3 dark | **0.33%** | 3 (largest 11 px) |
| ref4 dark | **0.20%** | 2 |
| ref5 dark | **0.35%** | 6 (largest 22 px) |
| ref3 pale | 1.78% | 8 (largest 279 px — the red sash cord) |
| ref4 pale | 4.69% | 17 |
| ref5 pale | 3.54% | 15 |

At the saturated end (r−b > 90, past the sky's own ~50–60) the entire family-B corpus
carries **one** mark bigger than 30 px: ref3 pale's 226 px sash cord — **a fitting**. The
big ochre thigh bloom lives in family A, on cream, on a figure five times this delivered
size. So at the duel framing on the dusk stage, the reference's answer to "interior
modelling" is *value* — folds, collar, the pale face patch, the hands — plus warm
**fittings**, and essentially zero garment blooms.

Our delivered hero (box `(240,260,500,660)`, f011) carries ~10 warm regions; they read at
1× as strips and patches scattered over shoulder, sash and skirt — torn-camouflage, not
shibori — and each one **interrupts a part instead of naming one**. The exceptions are
the fittings (obi band, scabbard), which are the two most readable parts on the figure —
the stain channel's *fitting* regime is doing exactly what the corpus does, and the
*bloom* regime is doing what the corpus does not. The classic variant consolidates blooms
(10 → 5 regions, larger and coherent) which helps the hero, but on the pale foe it
produced one big warm mass — see §6, "what it made worse": on this stage the honest
setting for garment blooms is close to **zero**, not "fewer and bigger".

---

## 5. Question 3 — the gradients

The gradients divide cleanly into one that earns its place and two that take from the
figure.

- **The sky ramp helps.** It is the corpus's own colour script (System 4's measured
  match), and a near-black figure against the coral band is the highest-contrast thing
  this project draws. None of the figure-legibility damage traces to it.
- **The ground smear/fog band takes.** In every `inkhyp` frame both figures lose their
  feet and shins into the blurred ground band; the reference keeps feet, ankles and a
  ground splash *inside* its equally dark ground (ref3 dark: near foot, far foot and
  splash all read at matched scale). That is 2–3 parts per duellist paid to atmosphere,
  the exact mechanism §9 already bans ("atmosphere may not be paid for out of the
  subject", measured at −2 parts by System 5 pass 2). The fix is the corpus's: dark
  *marks* (feet, splash, grass strokes) standing on the smear, not attenuation of the
  figure into it.
- **The empty gradient composition flattens.** `inkhyp-stand-current/frame_000.png` is
  the audit's finding restated: two small debris-clouds in the corner of an 85%-empty
  ramp. The owner has since ruled the planning frame a map (§9), so this is recorded, not
  re-litigated — but note that the classic variant is worth *more* at this framing than
  at the duel framing, because a 180 px figure has no pixels to spare for grime: the
  standing sheet is the largest single visible difference this experiment produced.

---

## 6. Question 4 — does the corpus reserve anything for motion?

**Not exclusively — image 1's standing figure is the counterexample.** Its cloak and hem
detonate at rest; the ink language in the corpus is a property of the **periphery**, not
of the **moment**. But motion *concentrates* it, visibly: images 3/4/5 shed their densest
smoke off the sides that just moved, the thrown hair is the loudest single gesture in
every duel frame, and the clash carries its own vocabulary (star, embers) while the
blades stay hard. And the *core* never joins in, moving or still — face, hands, grips,
fittings stay whole in every image at every scale.

So the owner's intuition maps onto the corpus as: **fragmentation lives at the periphery
always, and grows with motion; the core is classical always.** The prototype implements
exactly that split: authored dissolve below ~0.35 (the body, the far-limb "contrast
floors", the limb tips) buys softness only; above it (hems, trailing rails, sleeve
openings) the full detonation remains; and `dissolveBias` — the hit-reaction channel —
adds *before* the remap, so a struck body still sheds. Verified on the knockback pair:
`inkhyp-knockback-classic` still streams hair ahead of the carry and sheds flecks
mid-carry (frames 10–18), on a body that now reads as a body.

---

## 7. Question 5 — the part count, and what actually decides it

Counted at 1× on the matched-scale sheets (`inkhyp-sidebyside-hero.png`: ref3-matched
crop `(0,120,250,580)`, capture crops `(240,240,520,700)` of f011; figure height 378 rows
in all three panels). Zoom was used only to confirm a mark's identity, never to find it.

| | parts | what resolves |
|---|---|---|
| **reference 3, dark duellist** | **~24** | topknot; hair wisps; face profile (brow/nose/chin); jaw shadow; collar; shoulder; upper arm; forearm; upper hand; lower hand; grip; blade; scabbard at hip; sash wrap; sash tail; hip mass; near hakama panel; fold lines; far panel; near shin; near foot; far leg/foot; back smoke plume; ground splash |
| **current hero (f011)** | **9–12** | hair mass; shoulder flap (disconnected); upper-arm flag; forearm band; ochre sash strip; second strip; scabbard stick; skirt flaps (1–2); leg columns (2). No face, no hand, no grip, no guard, no foot, no fold; nothing connects |
| **classic2 hero (f011)** | **12–14** | hair mass; neck/shoulder line (now continuous); torso distinct from skirt; upper arm and forearm as one bent limb (2); sash band; sash knot bump; scabbard; skirt mass; fold hint; near leg; far leg; trailing sleeve mass; hem fray tail. Still no face, hand, grip, guard, or foot |

**The change is worth about +3 parts, and the honest mechanism is connection, not
addition**: parts that existed as disconnected flaps (arm, sash, skirt, legs) become
nameable because the silhouette between them survives. What it cannot add is the ~10
parts that do not exist in the mesh — the hand+grip+guard cluster, the face in profile,
feet with ground contact, real fold structure. Those are debt D1, seven systems old,
never on a work order, and they are why the count cannot reach §11.4's floor of 18 by any
ink setting. **The hypothesis buys legibility; the fittings pass buys the count.** If the
two are ever traded against each other, the fittings pass wins.

---

## 8. The prototype, honestly

**What it is.** ~40 gated lines in `ink_skin.frag` (uniform `u_classic`), one uniform
upload in `InkSkinnedRenderer.draw`, one flag in `build.gradle`'s capture task. Behind
the gate: (1) a remapped fray driver `dFray = dissolve * smoothstep(0.10, 0.42,
dissolve)` feeding the fray width, boundary wander, interior-tear threshold, shard and
speck gates — so low authored dissolve is solid and hems keep everything; (2) the
solid-passage fray floor cut from `0.22·halfPx+1.5` (≈15 px on the haori) to
`min(0.12·halfPx+1.5, 9)` and wander from `0.16·halfPx+2.2` to `min(0.08·halfPx+1.2, 5)`;
(3) the dry-brush tooth keeps its value job but opens the sheet at 0.35× its authority;
(4) garment stain blooms gated on a coarser field at a higher knee (fittings untouched);
(5) the coverage feather on solid passages *widened* (band 0.09 → 0.15), because clean
must not mean hard.

**What it looks like.** At the duel framing: the hero becomes one connected figure —
shoulder line runs into torso into hip, the sash reads as a band with a knot, the skirt
is a mass with a fold hint instead of four flaps, and the trailing haori still frays and
sheds. At the standing framing it is the difference between a debris cloud and a person
(`inkhyp-sidebyside-stand.png`, viewed at 2×, stated). The knockback still reads as a
carry with cloth thrown ahead and flecks shed. The one-sentence test moves closer to
*yes* on every frame I shot.

**What it made worse, named.**

1. **Edges got harder before they got softer.** First iteration: share of single-pixel
   luminance steps > 60 in the hero box went 0.047% → 0.135% (reference: 0.010%; region
   `(240,260,500,660)`, f011, horizontal steps). Widening the band recovered it to
   0.070% — still 7× the corpus. The corpus's clean edge is a 5–15 px wet gradient; a
   third iteration should spend the reclaimed fray budget on feather width, not on none.
2. **The foe's ochre bloom got bigger.** Consolidating blooms on a pale base produced one
   large warm mass (visible in `inkhyp-sidebyside-foe.png`) where the corpus's pale
   duellist carries a 226 px cord and nothing else. The classic stain restructure aimed
   at image 1 when the stage is images 3–5; the right dusk-stage setting for garment
   blooms is ~0, warmth concentrated at fittings. One constant, not yet changed.
3. **The pale duellist's wrong register is now louder.** A solid bright mass reads more
   wrong than a shredded one. Debt item 14 (sign and hue vs the corpus) becomes the
   loudest fault in the frame the moment the noise around it is cleaned. It should be
   decided (proposals B4) before or with any adoption.
4. **The figure is now quieter than the corpus** (tortuosity 1.34 vs 1.49–1.62). Cleaning
   subtracted; the corpus's remaining wildness — curling smoke streamers, feet splash,
   escapee garment wisps — is additive work (B6) the prototype does not contain.
5. **Detached chips are still chips.** Fewer and better clustered, but nothing curls;
   the corpus's figures smoke. Same B6 gap.

---

## 9. Rule changes this would imply, quoted

**STYLE.md §3, the headline and item 1.** Before:

> **Nothing in this game has a hard edge except the blades.**
> …
> 1. **Edge dissolve.** Every skinned garment surface carries a per-vertex `dissolve`
>    weight, 0 at the body core rising to 1 at hems, sleeve ends, and trailing edges. The
>    fragment shader thresholds a multi-octave noise field against this weight so the
>    silhouette breaks into discrete brush flecks before vanishing.

After:

> **Nothing in this game has a hard edge except the blades — and nothing but the hems,
> the hair, the trailing edges and a struck body has a broken one.** Soft and broken are
> different instruments and the corpus never confuses them: every contour is feathered
> (a wet 5–15 px gradient at matched scale), but fragmentation — flecks, shards,
> splatter, torn boundary — lives only at the periphery, loudest on the trailing side
> (measured: trailing > leading in five of six family-B duellists), and never touches a
> face, a hand, a grip or a fitting at any scale.
>
> 1. **Edge dissolve.** Every skinned garment surface carries a per-vertex `dissolve`
>    weight, 0 at the body core rising to 1 at hems, sleeve ends, and trailing edges.
>    Below ~0.35 the weight buys *softness only* — feather and halo, never
>    fragmentation; a far limb recedes by value, not by tearing. Above it, the fragment
>    shader thresholds a multi-octave noise field against the weight so the silhouette
>    breaks into discrete brush flecks before vanishing. A hit reaction
>    (`dissolveBias`, §7.3) may push any passage over the knee — fragmentation is
>    always available as a *verb*.

**STYLE.md §3b.0 (shibori blooms), appended.** Before (closing sentence):

> The existing stain system should be reinterpreted this way rather than kept alongside.

After (append):

> **And on the family-B stage, blooms are a family-A instrument.** Measured across
> images 3, 4 and 5, both duellists (regions in docs/ink-hypothesis.md §4): the dark
> duellist's ink is at most 0.35% warm, and the only saturated warm mark in the family
> bigger than 30 px is a sash cord — a fitting. At duel framing on the dusk stage,
> garment blooms are near zero and warmth concentrates at the fittings, whose entire job
> is to be reliably warm. Spend the shibori vocabulary at family-A framings and on the
> push-in, not as default surface treatment on a 378-row silhouette.

**No change to §7.3** — the prototype depends on it as written; the hit vocabulary *is*
the "ink for movements" half of the hypothesis and it already exists.

**Debt-note correction rather than a rule:** `SamuraiRig`'s far-limb "contrast floor"
(dissolve 0.08–0.10) was a value device implemented as fragmentation; system1-debt
already ruled for small marks that "a contrast floor is a value device on a large soft
mass and a delete key on a small hard one." Under the classic knee it becomes inert as
fray, which is the correct reading; far-side recession should move to value/halo
entirely.

---

## 10. Cost to adopt

- **The switch itself: already built.** ~40 gated shader lines, one uniform, one gradle
  flag. Making it the default is one constant; deleting the old path after a review pass
  is cheap. Off-state proven inert (§2 control).
- **One more tuning iteration owed before default-on** (medium-cheap, a day): feather
  width toward the corpus's 5–15 px gradient (close the residual 0.070% vs 0.010% step
  share); dusk-stage garment blooms to ~0; re-sweep the family-A scenes (`s1-*` cream
  captures) since image 1 legitimately detonates more at rest than family B and the
  variant may want to be stage-scoped or garment-scoped there.
- **Decisions it forces upward:** the pale duellist's register (B4) becomes urgent — the
  prototype makes debt 14 the loudest thing in the frame. Recommend deciding B4 first or
  simultaneously.
- **What it does not buy, priced honestly:** the part count stays ~12–14 against a floor
  of 18. The fittings pass (proposals B1, mesh authoring: hand+grip+guard, second blade,
  sash knot, folds, feet) is structural and remains the highest-impact item in the
  project; this change is its complement (clean silhouette makes new fittings readable),
  not its substitute. The smoke wisps (B6, medium) are the additive half of matching the
  corpus's edge wildness and should ride the same future pass.
- **Guard/measurement impact:** harness unchanged (`f0ad18994eec`), so no comparison is
  voided; but any future absolute statistic tuned on current pictures (e.g. a repointed
  `DuellistValueTest`) should be taken after the variant decision, not before.

## 11. Test suite

`./gw test --rerun-tasks`: **405 tests, 403 pass, 2 fail — both pre-existing worktree
artefacts, not this change.** `CorpusTest.theHairCaptureContainsNoReversalAtAll` fails
with `no frame_NNN.png files in out\captures\s3-p1-hair` and
`CorpusTest.theArrivalChainReproduces` reads NaN from `s3-p1-reversal`: git publishes
only `contact-sheet.png` for the first and only `frame_000.png` for the second, so on any
clean worktree these two cannot run — the exact failure MEASUREMENT.md §11.2b(f) names
("never let a guard depend on an artefact the repository does not publish"), here failing
loudly rather than skipping. Neither test touches the renderer, the shader or the capture
task; the failing reads are of files this experiment never wrote.

---

*Captures: `out/captures/inkhyp-*` with `capture.txt` manifests and `variant.txt` notes.
Measurement scripts: `tools/inkhyp_measure.py` (perimeter breakage + stain census),
`tools/inkhyp_tortuosity.py`, `tools/inkhyp_stains.py` (loud-warm census); every number
above is printed beside its region and scale factor.*
