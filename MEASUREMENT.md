# MEASUREMENT.md — how this project proves things about pictures

**This file is the other half of the grading rubric.** `STYLE.md` says what the game must
look like; this file says what counts as evidence that it does. Both bind every pass and
every review, and where they disagree, `STYLE.md` wins on *what* and this file wins on
*how*.

**Why it is a separate file.** It grew inside `STYLE.md` and reached roughly forty percent
of it by line count — with §11.2b(f) alone running to about a hundred and fifteen lines of
guard epistemology without a word about ink in it. An audit found the consequence, and it
was not cosmetic: the rubric's *falsifiable* clauses decide PASS/FAIL mechanically, so they
became the work orders, while the two things that actually decide whether a frame is a
picture — how much figure resolves, and what the composition is when nobody is striking —
headed no brief in seven systems. Splitting the apparatus out is meant to stop the
measurement doctrine from crowding out the thing being measured. **Nothing here was deleted
and nothing was softened**; every clause below was earned by a real defect, most of them
defects nobody suspected until an instrument found them.

Section numbers are unchanged, so every existing reference of the form "§11.2b(f)" still
resolves — it now resolves here. `STYLE.md` §0.1 and §11 point at this file.

---

## 11. Review protocol

### 11.0 The matched-scale comparison — do this first, before anything else

**Downscale a reference image so its figure is the same height in pixels as the figure in
the capture, and put them side by side.** Then count what each one gives you.

This takes two minutes and it must be the first act of every review, before any shader
analysis, any zoom, any measurement.

It was introduced after five System 1 passes had been spent refining how ink behaves at
the edge of a shape, while nobody had asked whether there were **enough shapes**. The
matched-scale test answered that instantly: at a 330 px figure height, reference image 1
still resolves a head with a face, a shoulder, a weapon arm, a hand, a grip, a guard, a
second sheathed blade across the back, a sash, a hip, two legs, two feet and a loud ochre
bloom over the thigh. The capture at identical scale resolved a torso, a topknot and a
hairline white scratch — roughly a third of the readable parts.

**Count readable parts, not cultural fittings.** The list above is a count of *how much
figure survives at this scale*, and that is the only thing the test measures. A guard is
a guard whether it is a tsuba or a star-forged crossbar; what matters is that the eye
finds a distinct mark there. Never fail a capture for lacking a specific Japanese
garment part — fail it for resolving fewer parts than the reference does.

Four passes of material analysis never surfaced that. One matched-scale comparison did.

**Corollary:** a material can only ever be as good as the subject it is painting. If the
count is short, fixing the material is refinement of the wrong thing.

**And run the criterion on the reference, not only the capture.** A threshold justified by
the corpus must be *shown to pass on the corpus*, in the same command, before it is allowed
to fail anything. System 4 set "the corridor between two bodies is never below 0.06 of a
figure height" from an eyeballed reading of reference image 3, and two passes then chased
it. Measured, that image's clear whole-frame column is **0.015** — and, like the capture,
**0.000** over the full figure height, because the duellists' hands nearly touch. The
acceptance the project was failing against was one its own ground truth fails by 4×.

The failure is not the wrong number, it is the wrong *shape* of number. A single scalar
across the whole figure is decided by the tightest band, which for two duellists is always
the hands — the one place they are supposed to be close. What separates the reference from
the capture is the **profile**: reference image 3 pinches at the hands and opens at sash,
skirt and feet, while the capture does the opposite, standing too far apart at the base and
leaning in at the top. So state such criteria **per band, with the reference's own profile
as the target**, and let the assertion that the reference passes live in the test suite
beside the one that the capture must.

**One reference image is not the corpus.** The paragraph above was written after two passes
chased a threshold image 3 fails, and the pass that answered it built exactly the tool asked
for — and ran it on image 3 alone. Pointed at the rest of the family, **image 4 fails three
bands and image 5 fails four, reading `torso = 0.0000`: the identical number the capture was
being failed for.** So the rule is not "show it on the reference", it is **show it on every
image in the family that depicts the situation being measured, and name the ones you
excluded and why.** A criterion validated against a single image is fitted to that image;
that is the whole failure this section exists to prevent, committed one level up by the
section itself.

**And a criterion of floors alone rewards the defect it was written to catch.** The corridor
profile has minimums and no maximums, so the highest score in the sweep — 21 of 24 bands
passing — belongs to the setting that pushes the skirt gap to **4.19× the corpus** and
destroys the parry entirely. A one-sided criterion does not measure resemblance to the
reference, it measures distance in one direction, and something will always score well by
running away. **State the target as a band with both edges**, taken from the corpus's own
spread, and make the sweep report where the score peaks — if the peak sits at a setting you
would refuse on sight, the criterion is wrong and not the setting.

### 11.2 Capture cadence — anything about timing must be captured at a true frame rate

A review once could not grade a pass's central claim at all, because the capture sampled
every 0.327 s while §7.1 specifies overlapping action in the 0.067-0.133 s band. **Every
lag the motion systems are required to produce was shorter than one delivered sample.** The
verdict at the time: *"it escapes the everything-peaks-together rule only because the
sampling is too coarse to convict it. That is not the same as complying."*

Use `-Pstart` and `-Pstep` to capture a short window at a true frame rate:

```
./gw capture -Pscene=<name> -Pframes=24 -Pcols=6 -Pstart=1.95 -Pstep=0.0167
```

24 frames over 0.38 s at 60 Hz, aimed at the beat that matters — the reversal, the impact,
the settle. Contact sheets label the *captured window*, not the scene duration.

**Any claim about lag or stagger made from a coarse capture is unfalsifiable**, and per
§7.1 a capture is not sufficient either: timing claims also ship with a headless
measurement.

### 11.2b The apparatus is upstream of every other rule here

Everything else in §11 governs the **subject**. Nothing governed the **apparatus** — and a
harness bug corrupted every frame this project captured, through roughly a dozen reviews,
until a change of framing exposed it. The flip in the capture path composited instead of
assigning, so each frame carried its own vertical mirror: **76-79% of pixels affected.** It
survived because every scene until then centred a single figure vertically, so the ghost
landed on the figure it came from.

**(a) A calibration card, asserted in the test suite.** Render a synthetic frame *through
the capture path* whose correct measurement is known analytically — a paper field and a
solid rectangle of known position, size and value, deliberately **asymmetric in every axis
the pipeline touches**. The flip bug dies to one assertion: *a figure drawn only in the top
third must leave the bottom two thirds at paper*. The same card catches gamma drift, y-flips,
paper-level estimation and off-by-one region resolution. It is the cheapest item in this
document and it would have caught the bug on day one.

**(b) Capture the subject where it cannot hide the artefact — once per system.** Off-centre,
off-frame, doubled, or absent. **Anything symmetric about an axis of the subject cannot be
tested on the subject.** One two-figure capture in an unfamiliar aspect ratio found in a
single frame what a dozen reviews did not.

**(c) Bit-identity across paths that share code proves nothing.** Two capture paths were
celebrated as bit-identical; they agreed because they shared the buggy function. A
cross-check must cross an **implementation** boundary, not a call site — against a known
answer, or an independently written reader.

**(d) Prefer differences to absolutes.** Every claim that survived the re-capture was
*relative* — registration lag, drape excursion, before/after through matched rectangles.
Every claim that died was *absolute* — coverage percentages, luminance standard deviations,
the brightest interior pixel, mark-run counts. **An absolute pixel statistic is valid only
against the harness that produced it**, so `capture.txt` records `commit=` and `harness=`,
and any comparison spanning two harness versions is void by default rather than by
discovery.

**(e) A discipline written into a document but not into the tool that reads it is
documentation, not a guard.** `track` *refuses* to run without an anchor, and anchors
stopped being a problem. `drape` writes the control flag into the manifest and never checks
it — and a reviewer proved in one command that it will call a live capture a rigid control.
Audit every rule in §7.1 and §11.3 for which of the two it is.

**Closed for `drape` in System 3 pass 5, and the shape of the fix is the reusable part.**
`--control` now *refuses*, four ways: a directory with no `capture.txt`; one whose `clamp` is
not `cloth`; one whose scene, start, step, frames or size differ from the live capture's; and
one shot through a different `harness`, where a manifest with no `harness=` line counts as a
harness version of its own rather than as a wildcard. Each refusal is asserted in a test, not
described in a comment, because that is the difference this paragraph is about.

**(f) A guard must be shown to fail.** System 4 pass 2 shipped
`everyClashIsDrawnWhereTwoBladesActuallyAre`, whose failure message names precisely the
defect the previous review found — *a light asserting an event the picture does not
contain* — and which **cannot fail**. Its helper takes the mark from `bladeCross()`, a
point on a blade, and measures it against **that same blade's own segment**: three
collinear points, distance identically `0.0`, asserted against a `0.10` ceiling. Two
reviews and a commit message cited it as proof.

The mechanism is worth naming because it is not carelessness. A guard is written by the
person who just fixed the bug, from *inside* the fixed model of the world, and reaches for
the value nearest to hand — which is, of course, the one the fix just made correct. A
test that consumes the fix's own output cannot witness the fix.

So: **no assertion counts as a guard until it has been observed red.** Break the thing it
watches — invert a sign, offset a target, revert the fix — and see the test fail with the
message it was written to print. If that is impractical, the assertion must at minimum
cross an *independent path* to its subject: the guard for a **drawn** mark reads the
coordinate the renderer was handed (`director.lastCrossing`), not a coordinate recomputed
from the geometry the renderer was supposed to have used. §11.2b(c) applied one level in:
**a test that shares its input with the code under test is a bit-identity check wearing an
assertion's clothes.**

And the corollary that costs the most to learn late: a checked-in test is the strongest
evidence this project produces, so a **vacuous** one is worse than none. It does not merely
fail to catch the defect — it certifies it, and it persuades the next reviewer to stop
looking.

**A guard that skips is a guard that fails open, and it is the harder one to see.** The
tautological guard at least *ran*. Three assertions in this project read a capture frame
that `.gitignore` excludes, each wrapped in `assumeTrue(file.isFile())` — so on the author's
machine they pass, and on a clean clone they **silently skip** and the results they certify
do not exist for anyone else. One of them was a pass's headline value result. This survived
a review that checked the guard's *logic*, because the logic was fine.

**And a guard whose exit code the pipeline discards has not run.** Recorded because the
author of this section then did it: `check-progress.mjs` was invoked as
`node tools/check-progress.mjs | tail -3 && git commit && git push`, and a pipe takes the
exit status of its *last* command. The checker printed two FAILs in red, `tail` returned 0,
and the commit and push proceeded over the top of them. Nothing was damaged, and nothing
would have warned anybody if it had been. **Run a guard as itself, never through a pipe,
and never as anything but the first link of a `&&` chain** — every convenience that
reformats its output also throws away its verdict.

Two things follow. **Never let a guard depend on an artefact the repository does not
publish** — if an assertion needs a frame, that frame is source and must be force-added,
whatever the ignore rules say about its neighbours. And **`skipped="0"` on the machine that
wrote the test proves nothing**, because the file is on that machine's disk; the only honest
check is whether version control hands it to someone else. `tools/check-progress.mjs` now
refuses on any `out/captures/**/frame_*.png` referenced from `src/` that git does not track,
and was observed red before being believed.

The general form, which is worth more than either instance: **an assertion has three
outcomes, not two, and the third one is silent.** Any mechanism that can turn a test into a
no-op — an assumption, a conditional skip, an empty parameter set, a tag filter, a `@Disabled`
someone meant to remove — belongs in the review, because a suite reports it as success.

**And observing a guard red proves a path to failure exists — not that the guard covers
what it claims.** This is the version that survives the other two, and it was found the only
way it could be: adversarially.

System 5's interface guard asserts that every emitted triangle has a vertex at alpha zero,
and its documentation says a panel, a bar or a border *"fails it on the first triangle"*. It
was broken by hand and it went red, so it had been observed red. A reviewer then wrote a
triangle of the form `(0, α, α)` — which satisfies the assertion and **prints a hard edge
along the α–α side** — and with it drew **a filled, bordered HUD panel that passes the guard
and all 401 tests.** The assertion was true, the break was genuine, and the claim was still
false, because the claim was about *every* way to print an edge and the assertion covered
one.

Being red once tells you the assertion is reachable. It tells you nothing about the gap
between what the assertion tests and what its name promises, and that gap is where defects
live — the guard is written from the same mental model as the code, so it inherits exactly
the blind spots that produced the bug.

**The rule paid for itself on its first use, which is the strongest thing that can be said
for it.** The skip-detector written *in response to* the paragraph above — count what the
suite declined to run, rather than regex-match paths, so that composed paths and assumptions
are covered alike — was then attacked as this section requires. **The first attempt
defeated it.** Capture frames are not declared gradle inputs, so hiding one leaves
`./gw test` reporting UP-TO-DATE, and the checker read a *stale* report that still said zero
skips. Green build, green checker, six tests never run. The fix is a second clause — a
report older than its inputs is not a report — and both routes are now exhibited: staleness
refused by name, and a genuine skip caught with its count. **Had the guard merely been
observed red, it would have shipped with the hole in it.**

**And a guard that does not run in every configuration the product ships is not a guard for
the ones it skips.** This is the fourth distinct failure and the least like the others,
because nothing is wrong with the assertion at all.

System 5's rasteriser guard is correct, was observed red, and survived an adversarial
attempt. It also reads `SHIPPED_HEIGHTS[0]`. Pointed at the second entry — **540 rows, a
resolution that same pass added** — it goes red immediately: 0.3563 against a ceiling of
0.34. And its failure message prints `960x720` whatever it actually measured, so even a
reader watching it fail would have been told the wrong thing.

The first three rules all ask *is this assertion any good?*. This one asks a question none
of them reach: **where does it run?** A guard is a claim about the product, and the product
is every configuration it ships in — every resolution, every scene, every lane length, every
figure. Testing one and naming the file after the claim is a sampling decision disguised as
a proof.

So: **enumerate the axis, do not index it.** If a constant lists the shipped configurations,
the guard iterates the list; if it cannot, it names in its own message the single case it
checked, so nobody mistakes a sample for the set. And a failure message must report the
parameters it actually ran with rather than the ones it was written against — a message that
lies about its own inputs turns a red run into a wrong diagnosis.

**So a guard that carries a broad claim owes a second exhibit: the adversarial instance.**
Try to build the thing the guard forbids *while satisfying it*. If you succeed, the guard's
scope is the finding and the name is a lie; narrow the name or widen the test. If you
genuinely cannot, say what you tried — that attempt is the evidence, and it is worth more
than the red run. Three rules deep, the pattern is one thing: **every claim about a guard
must itself be tested, and each level of that has cost a system a pass.**

**(g) The control must exercise the property it certifies.** Every review since the harness
bug has opened by proving its apparatus in scope against `rig-bindpose`, a static null, and
reporting *0 of 691,200 pixels differ*. That sentence was read as "captures are
reproducible". It only ever established that **a scene with nothing moving in it renders the
same twice**. Shooting the graded scene twice at one commit on one harness — which no review
had done, because the null had appeared to settle the question — gives **13,545 differing
pixels, peak delta 122, on four frames including the graded one**. Every before/after in
this project rests on a determinism nobody had measured where it could vary.

A null case answers exactly the question its own content can pose. A static frame certifies
the *readback path*; only a moving one can certify the *simulation*. So state what each
control establishes in the words of its content, and when a claim needs a property the
control cannot express, shoot the control that can.

**And generalise the control from the subject to the instrument.** §7.1 learned "run the
control" about cloth. Stated generally: **before a number is allowed to decide anything,
someone must say what it would read if the thing being measured were absent — and then
produce that case.** The phrase *"by construction"* has now destroyed two acceptance
criteria, one bug diagnosis and one test. Read it, always, as **nobody has measured this**.

### 11.3 Record the region, or the measurement is unfalsifiable

**Every number must be printed beside the rectangle it was taken through.**

This was learned by building the analysis tooling and pointing it at the project's own
records. Two findings that had decided real work turned out not to hold:

- "Hair-region coverage 23%, hair as 6.0% of the figure's ink" — the region box was never
  written down, and defensible "hair region" boxes on that very frame give coverage from
  **3.6% to 59.3%** and share from **1.0% to 24.2%**. The recorded pair is reachable; so is
  almost anything else. The band-luminance profile that proved the ink gravity was inverted
  has the same problem: the *shape* reproduces strongly, but the five quoted numbers were
  taken through an unrecorded column.
- "A tight hem-tip box registers 0.00 px across all 23 inter-frame steps" is a
  **quantisation artefact**. With sub-pixel registration that box moves 0.04-0.44 px per
  step and accumulates 5.52 px of path. Every step being under half a pixel, integer-only
  registration *necessarily* reports exactly 0.00 at every step — the figure measures the
  ruler, not the cloth.

This is the same failure §7.1 already named for lag anchors, one level down. So:

- Region sets are **checked in** (`docs/regions.json`), not improvised per review.
- The measuring tool prints the rectangle with the number, and refuses to track without an
  explicit anchor.
- Sub-pixel registration, because integer registration silently manufactures zeros.

**A measurement whose region is not recorded is an anecdote.** It may still be true. It
cannot be checked, and it must not be used to fail a pass.

**On any scene with a camera move, a pixel number is quoted normalised or not at all.** §9
requires the camera to glide during the beat and never be still; §7.1 requires timing measured
in delivered pixels. Those two pull against each other, and the resolution is normalisation:
every pixel statistic on a moving-camera scene is a **ratio to the figure's own span**, or to
a **control shot at the same harness**, or it is void. Measured on one phrase, the hero's
bounding-box height ranged **4.08×** across the scene — a raw pixel lag taken across that
window is measuring the lens.

**A region set that resolves against a detected figure box must refuse a frame it cannot
resolve.** On a two-figure frame the detected box spans *both* bodies, so `head` lands on the
wrong figure's hair and `torso` straddles the gap — and the tooling reported *"hem trails hips
by +6.52 frames"*, **a plausible number inside §7.1's own band**, from boxes that were
nonsense. It only looked sane because the boxes were resolved against frame 0, the one frame
where the bodies happened to be apart. **A silent wrong answer is worse than a refusal**: the
tool must refuse when the ink resolves into two components each above some share of the total
and no figure has been named.

---

Each review pass must produce, in this order:

1. **Verdict:** PASS or FAIL.
2. **The one-sentence test** answered explicitly, with reference to which family (A/B/C) the
   capture was trying to match.
3. **What is missing**, phrased as one of: *too literal*, *too harsh*, *not evocative enough*,
   *drifting generic*, *technically broken* — with the specific visual evidence in the capture
   that supports it.
4. **Why** that matters against the references.
5. **Concrete, actionable changes** ranked by impact.

A reviewer must never grade work it produced. A reviewer must look at the actual captured
pixels, not the source code, when judging aesthetics.

