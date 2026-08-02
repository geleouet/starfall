# System 1 — ink material revision 3, step 1 of 2

Supersedes `docs/system1-shader-fixes.md`. Applies to
`src/main/resources/shaders/` and `dev.starfall.render` (excluding
`PaperBackground`) only.

Written from the independent review of pass 3. That review's architectural verdict is
**HYBRID, staged** — and this document is **step 1 only**. Do not attempt step 2. Bundling
the two changes is exactly what made pass 3's result unattributable, and repeating it is
the one failure mode this document exists to prevent.

---

## The situation

Pass 3 replaced per-fragment ink evaluation with a merged coverage buffer plus a
full-screen resolve. It killed the periodic banding — measured chest-scanline amplitude
fell 20.3 to 3.6 — and it broke the image:

- Three axis-aligned rectangular bars now run through the torso. These are **ribbon rails
  printing directly**. The merge uses max-blending on a topmost-wins basis, so each ribbon
  *replaces* its neighbour's value inside its own quad, and the quad's rail prints as a
  hard step. Pass 2 had no such artefact precisely because per-ribbon alpha compositing
  *averaged* neighbours.
- The silhouette lost all articulation: no neck, no shoulder line, no findable sword arm.
- Edges became smooth thresholded-gaussian lobes with round holes and round detached dots,
  where pass 2 had angular torn shards with direction.

**And the banding fix was never architectural.** Pass 2's finest fleck octave ran at a
4.7 px period and its dry-brush streak octave at 2.5 px, neither with a time term. The
2.5 px octave already violated this project's own written hard floor in STYLE.md §3b.1.
Correcting the frequencies was the fix; the architecture was not needed for it.

**No backup of pass 2's `ink_skin.frag` exists** — it was overwritten before this project
was under version control. Its behaviour must be rebuilt from description, not restored.
A git repository now exists; commit before and after significant changes.

---

## The cut line

The reviewer's most useful finding, and the thing that makes this tractable:

> Almost everything pass 3 improved lives in `ink_skin.frag`, the material-space side.
> Almost everything it broke lives in the merge rule and `ink_resolve.frag`, the
> screen-space side.

Cut there.

---

## Step 1 — required work

### 1. The full-resolution material-space field becomes the dominant edge cutter again

The fray threshold, the fleck islands and the edge feather are all evaluated
**per-fragment at full resolution in material space**, as pass 2 did. The quarter-res
gaussian ladder must not shape the outline.

Concretely, pass 3 displaces the boundary by ±13-44 px using a `coarse` term derived from
the quarter-res field. That is why the silhouette is shaped like a quarter-res field. In
step 1, that displacement goes to **zero** — the outline comes from the full-resolution
field alone.

### 2. Stop the merge replacing value and stain

Every straight line inside the pass-3 figure comes from this one rule. If ribbons still
composite at all in step 1, accumulate **coverage-weighted and normalise**; do not use
max/topmost-wins on the material channels. Max-blending belongs on coverage only, if
anywhere.

Note the review's judgement that this is not a cosmetic defect: aperiodic, zero-frequency,
full-amplitude, hard-edged, axis-aligned bars are decisively worse than the periodic
low-amplitude banding they replaced. A viewer with no context calls pass 2's chest
"brushy cloth" and pass 3's torso "a bug".

### 3. Restore torn, directional fleck cutting

Detached marks must be **shards with direction**, not peaks of a high-pass residual —
residual peaks are always round, which is why pass 3's flecks are dots. Cut at full
resolution, with pass 3's corrected frequencies.

### 4. Carry forward, verbatim, everything pass 3 got right

All of these live on the material-space side and survive this change. Losing any of them
is a regression:

- **The corrected octave frequencies** — marks at 3.10 / 7.30 / 15.50 / 24.00 cycles per
  unit with anisotropy capped at 1.6:1, dry brush at 11.0 / 19.0. This is the real banding
  fix and it satisfies §3b.1's hard floor.
- **The directional-smear noise** that replaced the rotating stroke frame. The old frame's
  noise argument cycled dozens of times per turn, which is what produced the head's radial
  fringe.
- **Two-dimensional stain gating**, so stains form isolated blooms rather than per-row
  horizontal stripes.
- **The stronger stain amount.** The hue is right; only its boundary was broken by the
  merge.
- **The blade glow sheath** — a genuine §5 requirement that pass 2 did not satisfy.

### 5. Author dissolve to near zero on neck, skull and the sword arm's inner contour

The same principle STYLE.md §4b.1 already applies to the face: a fray band sized for the
haori hem, applied uniformly, deletes every small feature in the figure. Paper must be
visible on both sides of the neck, as it is in every one of the eight references and in
pass 2.

### 6. Fix the blade trail's compositing

Currently a large wedge with two dead-straight edges, written as cool pale grey at low
alpha. Alpha-blended over warm cream paper it composites *darker and cooler* than the
ground, so it reads as a dirty smudge rather than as light — and straight edges are
permitted to nothing but the blade itself.

- Composite **additively or with screen**, not alpha-blended grey.
- Taper both rails to zero alpha so no straight edge survives.
- Widen and lengthen it enough that the arc it already computes becomes visible at the
  0.218 s capture step. The angle-about-the-base interpolation is the correct
  construction; the ribbon is simply too narrow and short to expose the curve.

### 7. Bring back the dry-brush tooth

At the corrected 9-19 px scale, once item 2 means region boundaries no longer print.
STYLE.md §3.3 and §3.4 are System 1's own requirements and are not deferred to System 3c.
Pass 3's torso interior is a near-flat dark field with no streak structure and no pooling.

Note the connection worth understanding: **the corrugation and the tooth were the same
term.** Pass 3 removed both instead of retuning one. The goal is visible tooth without
periodic ripple, which the corrected frequencies now make possible.

---

## Explicitly NOT in step 1

- **Do not reintroduce the coverage buffer as an outline source.** Retaining it as
  dead code behind a flag is fine; using it to shape the silhouette is not.
- **Do not implement the wide bleed yet.** It is genuinely worth keeping and pass 2 cannot
  produce it, but it is step 2. Step 1 may ship with pass 2's tighter halo.
- Nothing in `dev.starfall.rig`, `dev.starfall.anim`, `dev.starfall.capture`, or
  `dev.starfall.render.PaperBackground`. A rig agent is revising the first two and the
  last.

---

## Verification

```
./gw test
./gw capture -Pscene=rig-swing -Pout=out/captures/s1-p5-swing -Pframes=12 -Pcols=4
./gw capture -Pscene=rig-bindpose -Pout=out/captures/s1-p5-bind -Pframes=4 -Pcols=4
```

Judge from **individual full-resolution frames**, and compare directly against both
`out/captures/s1-p2-swing/frame_007.png` and `out/captures/s1-p3-swing/frame_007.png`.

Confirm all of:
- **No straight line anywhere inside the silhouette.** This is the pass/fail gate.
- Edges read as torn, angular and directional rather than as smooth lobes; detached marks
  are shards, not dots.
- A horizontal scanline through the chest shows no periodic ripple — the pass-3 gain must
  be held, not traded away.
- Paper is visible on both sides of the neck; the shoulder line and sword arm are findable
  as silhouette.
- Visible dry-brush tooth inside the body.
- The blade trail carries no straight edge and reads as light rather than as a smudge.

Protect, and verify by measurement: ink black at `#161A22` with no pure black, paper warm
and never white, and material-anchored noise that does not swim under deformation.
