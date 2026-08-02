# System 1 — standing debt

**Status: closed at the five-pass cap with a FAIL verdict.** This is the recorded debt,
not a request for another refinement pass. Anyone picking System 1 up later starts here.

Source: the final independent review of pass 5
(`out/captures/s1-p5-swing/`, `out/captures/s1-p5-bind/`).

Items are ranked. Each says whether it is genuinely System 1's or whether a later system
resolves it.

---

## The meta-finding, which matters more than any individual item

> Five passes were spent on how ink behaves at the edge of a shape, and zero on whether
> there are enough shapes.

At matched figure height (~330 px), reference image 1 resolves a head with a face, a
shoulder, a sword arm, a hand, a tsuka, a tsuba, a second sheathed sword across the back,
an obi, a hip, two legs, two feet, and a loud ochre bloom over the thigh. The pass-5
capture resolves a torso, a topknot and a hairline white scratch.

**A material can only be as good as the subject it paints.** Four passes refined a
material that was, by pass 4, already better than the figure underneath it. This is now
codified as STYLE.md §11.0 — the matched-scale comparison is the first act of every review
from System 2 onward.

---

## D1 — No hand, grip, guard, obi, scabbard or second sword

*Not evocative enough.* **System 1 defect. Highest impact. Never appeared on any pass's
work order.**

The blade emerges directly out of the mantle mass with nothing between cloth and steel
(`s1-p5-bind/frame_000.png` at ~(565, 290); same in swing frames 005, 007, 011).

In every Family A and B reference, the hand + tsuka + tsuba cluster is three small
high-contrast marks that survive to very small scale, and they are what tell the eye where
the body ends and the weapon begins. They are also what makes the figure read as *a
samurai* rather than *a dark shape*.

No later system supplies these. IK moves an arm it does not create; cloth dynamics animate
a sleeve that does not exist. This is mesh authoring and nothing else.

## D2 — The blade is about 2.2x too long, with no tip, taper, glow or hamon

*Technically broken*, shading into *drifting generic*. **System 1 defect** (rig proportion
plus blade material).

Measured in the **bind pose**, so this is the blade itself and not a trail artefact: it
runs ~380 px from mid-torso off the right frame edge against a 420 px figure — about
**90% of body height**, where a katana is ~40%. Constant ~2 px wide over its whole run.
Never terminates in a tip inside the frame. No outer glow, which §5 requires. No hamon,
which §3b.3 says must stay faintly readable even at planning framing because the blade is
the object the eye follows.

It also holds full brightness where it crosses the fog band that has erased the figure's
own legs — so the blade is not sitting in the same atmosphere as the rest of the picture.

Note this contradicts earlier reviews that praised the blade's taper. Those measured the
*swing* frames where the trail geometry overlays it. The bind pose is the honest test.

## D3 — The upper silhouette is a hard polygon cut with no wet bleed

*Technically broken.* **System 1 defect.** §10 fail-on-sight item.

`frame_011`, left shoulder, scanlines y=200 and y=230 across x≈405-425: paper 218, ink 124,
transition complete in **three pixels**, with stair-stepping visible at 7x. Paper holds
218-222 right up to the step, so §3.2's wet-bleed halo is **absent above the waist
entirely**. Identical in the bind pose.

The hem does this correctly. The top two-thirds of the figure receives essentially no ink
treatment beyond a slight feather.

**Fix instruction: make the shoulder look like the hem.** `frame_011` at roughly
(420-560, 350-450) is the best square inch in the capture — genuinely wet, dilute, cloudy,
with real internal value variation. That is the target for the whole figure.

## D4 — All break-up lives at 8-65 px; nothing at brush-hair scale

*Not evocative enough.* **System 1 defect. A tuning change, not an architectural one.**

The mark field runs octaves at 65 / 27 / 13 / 8.3 px and the fray wobble at 77 / 33 / 15 /
9 px. §3b.1's hard floor is **2 px**. There is a full octave and a half of unused headroom
between 8.3 px and 2 px, and that band is precisely where "ink fleck" lives.

The three detached flecks below the hem in `frame_011` (x≈440-465, y≈415-445) are smooth
ellipses all roughly 8x6 px — §3's own failure list names "flecks that are all the same
size". The matched-scale reference shows splatter from 1 px to 5 px plus hairline tendrils
shooting several body-widths out.

This is an over-correction from the shimmer failures of passes 1-2.

**Warning to whoever takes this on:** the last time a frequency artefact was diagnosed as
structural it cost two passes, and STYLE.md §3 now carries a postscript admitting it. This
is a number, not a design.

## D5 — The lower third is bleached where the references blacken it

*Not evocative enough*, inverted against reference. **System 1 / atmosphere-integration
defect.**

Mean ink luminance by band in `frame_011`: 65 at the chest, 52 at the waist, then 82, 132,
146 descending to the hem. Images 1 and 2 put their single **darkest** passage exactly
there — the ink cloud around the knees is the heaviest black in the whole painting.

Family C does bleach lower bodies into pale fog, but only for *background* figures: in
image 8 the foreground woman keeps a dark garment to the bottom of frame while the
receding figures dissolve. The hero here is being fogged at background depth.

The ink-gravity metric is now correct *within* the garment down to the hip. The picture is
still wrong below it, because the figure does not end in ink smoke — it ends in
**erasure**. System 3's cloth dynamics will animate the hem; they will not put darkness
back under it.

## D6 — The garment interior is flat-facet, not brushed

*Drifting generic.* **Partly deferred.**

At 3x the torso is a mosaic of large flat-value patches with straight borders; the pale
wedge at the right shoulder is identical across frames 08-11. No dry-brush streaking is
readable at 1x anywhere on the figure, despite §3.3 and despite the shader carrying a
working tooth term.

Root cause: garments are **2-rail quad strips**, so across the width of any garment there
are exactly two vertices and all cross-cloth variation is a linear interpolation between
them.

System 3c's pleated hakama and lamellar plate rows supply real cross-cloth structure and
will fix most of the flat-curtain read. The straight *facet boundaries* are System 1's and
will still print through underneath.

## D7 — The head reads as a topknot with no skull, neck or jaw

*Not evocative enough.* **Mostly deferred to System 3b, except the silhouette.**

`bind/frame_000` at 3x: a dark blob at ~(460-540, 40-110) on a pale vertical neck column
whose left boundary is a straight line. §4b.3 asks for brow-nose-lip-chin as one continuous
contour and a dark jaw-neck wedge; neither exists, so the figure reads at a glance as
headless with a hat.

Faces are System 3b. But the **contour** and the **jaw wedge** are silhouette, i.e. System
1 mesh, and they are why the head fails at a glance rather than merely lacking detail.

## D8 — The mist measures brighter than the paper ground

*Technically broken*, trivially. **Log only; do not act.**

Median luminance 223 at y≈360 against 217 at the top of frame, against §2.2's rule that
the brightest non-emissive element is the paper ground. Six levels out of 255.

The reviewer's explicit judgement: **the §6-over-§2.2 ruling was correct.** Fog appears in
all eight references and this document's own preamble says the references beat the
document. If anyone ever cares, amend §2.2 — not the renderer.

## D9 — The blade trail is below the threshold of perception

*Technically broken.* **System 1** (blade material).

In `frame_005` the trail lifts the paper by about **4 luminance levels** (213 to 217) and
its leading boundary is a **0.98-level** step. It is only findable by pushing contrast 8x.

The geometry is fine — at 8x boost the swept fan is a plausible shape. The failure is the
compositing operator: screen-blending over a 217/255 warm paper ground has essentially no
headroom, which is also why the carefully tapered rails vanished.

---

## What is right and must not regress

Any later refactor that breaks one of these is a regression, not a trade.

- **Material-space anchoring (§3.5).** Internal value patches and the ochre stain hold the
  same body-relative position through pose changes across swing frames 08-11. Nothing
  swims. Two earlier passes broke this. `ink_skin.frag`'s evaluation at `v_matPos` is
  load-bearing — **any refactor that reintroduces a screen-space measurement of the edge
  will resurrect the pass-3 failure wholesale.**
- **No periodic artefact.** Autocorrelation of the ground band shows no peak above 0.25 at
  any lag from 4 to 200 px; the torso shows no ripple at any zoom. The frequency-budget
  discipline works — it is merely set too coarse (D4).
- **Value range.** Darkest 0.5% of pixels = (28, 34, 44), right on `#161A22` and never
  below. Brightest non-emissive (246, 241, 208). No pure black or pure white in any frame.
- **Paper ground.** Warm, tooth visible, corner (225, 217, 201). Never white.
- **Fog band and jewel motes (§6).** Present, correctly hued, correctly sparse.
- **Ink gravity within each garment down to the hip** — 65 chest, 52 waist. D5 concerns
  what happens *below* the hip, not this gradient.
- **The hem passage** at roughly (420-560, 350-450) in `frame_011`. The best square inch in
  the capture and the target for the rest of the figure.
- **The swing envelope (§7.1).** Roughly 40/15/45 — long wind-up, short release, long
  settle. Nothing snaps, nothing oscillates, no freeze. Hard-won across three passes.

---

## Corrections from the pass-7 debt-paydown (D5, D7, D3, D4)

Measured against `out/captures/s1-p7-bind/` and `out/captures/s1-p7-swing/`, with
`out/captures/s1-p6-bind/frame_000.png` as the before. Three of the four items in
scope needed correcting, and one of them was materially wrong.

**D3's edge complaint was already discharged by pass 6 and should not be re-fought.**
D3 was recorded against pass 5, where the shoulder transition was three pixels with
paper holding 218-222 right up to the step. Swept across eighteen scanlines of the
upper silhouette on the p6 capture, the paper-to-ink transition is 1-76 px wide and
the halo runs 10-131 px before it. It is no longer a polygon cut and STYLE.md 3.2 is
not "absent above the waist entirely".

**D3's remaining half — "the interior, not the edge" — does not survive measurement
either, in the direction it was written.** The document names the hem passage at
(420-560, 350-450) as "the best square inch in the capture and the target for the
rest of the figure". Measured on strictly *interior* windows, with no edge or paper
in them, the p6 numbers are:

| window | mean | std | std below 9 px |
|---|---|---|---|
| shoulder mantle interior | 58.2 | 11.0 | 4.3 |
| skirt interior | 48.9 | **7.1** | **3.7** |

The hem interior is **flatter than the mantle interior**. What made that square inch
look like "genuinely wet, dilute, cloudy" is its frayed boundary and its fleck field
— which is D4's territory, not D3's — and the large std the whole-window measurement
reported was the ink/paper boundary running through the window. The mantle was still
worth improving and pass 7 does improve it (std 11.0 -> 12.0, fine-scale 4.3 -> 5.1,
mean unchanged at 58.1), but *anyone who chases the hem's interior as a target is
chasing a measurement artefact*.

**D4 was substantially discharged by pass 6 too.** The mark field runs octaves at
73/31/15/9.4/5.6/3.5 px, the fray wobble adds 5.0 and 3.2 px, and a separate splatter
cut reads only the two finest. Detached islands on the p6 capture measure 1.1 to 18.2
px equivalent diameter with twelve of twenty-eight at or below 2 px — a real size
distribution, not "smooth ellipses all roughly 8x6 px". The remaining headroom below
3.5 px is not usable at this framing: STYLE.md 3b.1's own fade takes an octave to zero
between 6 px and 2 px, so a 2.8 px octave arrives at 11% amplitude and a 2.3 px octave
at 1%. **The floor is not the binding constraint; the fade is.** Pass 7 therefore added
no octaves, which is the right answer to a warning that says "this is a number, not a
design".

**D5's largest single term was not in the shader or the mesh — it was the grass.**
`PaperBackground`'s overlay pass draws grass strokes *over* the figure at luminance 163
and up to 0.88 alpha, and they reached to world y=0.48, which is above the knee. Every
stroke that crossed a leg lifted near-black ink to about 150. The "146 descending to
the hem" in D5's own measurement is substantially grass painted on top of a figure that
was already dark underneath. The second largest term was the figure's fog response
(`alpha *= 1 - fog*0.20` with a mist that is *brighter* than the paper), and only the
third was the mesh. D5 named the mist and the mesh; it did not name the overlay.

**A plain bug, unrelated to any debt item.** `buildHakama` was authored with absolute
distances along the bone chain and called with the near leg's lengths for both legs.
The far leg's bones are 0.40/0.36 against the near leg's 0.44/0.40, so every row below
the far knee landed past where that leg actually is and the last one at world y=0.037
against a far sole at y=0.216 — the far leg rendered as a 40 px spike through its own
foot into the ground. It is now parameterised by fractions of the real bone lengths.

**The fray band is an absolute width in pixels, and this keeps biting.**
`sheathedSword` already documents it; the feet hit it again. A foot is ~20 px across,
so `halfPx` is 5 and a 0.09 dissolve — the same "contrast floor" the far leg and far
sleeve carry harmlessly — buys a 7.3 px fray band and deletes the object outright. A
contrast floor is a value device on a large soft mass and a delete key on a small hard
one. **Small hard marks get dissolve 0 and recede by value instead.**

---

## On contrast — a correction worth carrying forward

The pass-5 frame reads washed out compared with pass 2, and the intuitive fix — darken the
ink — is **wrong** and would push against the no-pure-black rule.

Measured: p5's darkest 0.5% is (28, 34, 44) against p2's (22, 26, 34), a six-level
difference that is invisible. Peak local figure-versus-paper contrast is 188 in p5 against
192 in p2 — identical within noise.

What actually changed is **ink coverage**. Sampled down the figure column, p2 covers 51-69%
of every band from chest to hem; p5 covers 30-53% and collapses to 22% by y400.

**The fix is coverage and mass, not value.**
