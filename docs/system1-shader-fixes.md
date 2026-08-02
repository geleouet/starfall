# System 1 — ink material revision 2

Addendum to `docs/system1-contract.md` section F. Applies to
`src/main/resources/shaders/` and `dev.starfall.render` only.

Written from the independent review of pass 1
(`out/captures/s1-sideB-swing/contact-sheet.png`). The material's *pigment* is right
and is not in question here — palette, value range, ochre bleed and the slow
non-swimming pattern evolution all passed and must survive this revision unchanged.
What follows are the four defects in how that pigment is applied to geometry.

---

## 1. Kill the vertical strip banding (highest impact)

**What is wrong:** a horizontal scanline through the chest oscillates in luminance by
20-40 levels at a regular 3-5 pixel period. It reads as a corrugated roof, or a fan of
hair cards. It is pixel-identical across all twelve swing frames and all six bind
frames, so it is not brushwork — brushwork evolves.

**Cause:** every garment ribbon runs its own dissolve, its own bleed and its own alpha
blend, and then composites on top of its neighbours. N overlapping narrow surfaces
each carrying an independent dissolve produce periodic ripple regardless of how good
the noise is. No amount of noise tuning fixes this.

**Required:** composite garment coverage into a single offscreen buffer first, then
apply the dissolve threshold, the dry-brush breakup and the wet bleed **once** over
the merged coverage.

Suggested shape — not binding, but the constraints are:
- Pass 1 renders all skinned garment geometry into an offscreen RGBA target, writing
  *material data* rather than final colour: coverage in one channel and the
  interpolated `dissolve` / `wetness` / `stainMask` / `flowU` channels alongside it.
  Use `max` blending or depth-free overwrite so overlapping ribbons do not accumulate
  alpha — accumulation is the bug.
- Pass 2 is a full-screen resolve that reads that target and applies the whole ink
  material, then composites onto the paper.
- The blade is drawn separately and is unaffected.

Mobile-safe constraint still applies: one extra full-screen target at capture
resolution is acceptable; a chain of blur passes at full resolution is not. If you
need a blur for the bleed, do it at half resolution.

## 2. Make the dissolve two-dimensional

**What is wrong:** dissolve is a ramp along the ribbon's length only. The hem frays
correctly, but the torso's left and right edges cut hard in three to four pixels. The
figure is therefore a hard-edged sprite with a frayed skirt attached — the exact
inverse of STYLE.md §3's central rule.

**Required:** drive dissolve from **distance to the silhouette** in material space, so
that sleeve openings, cuffs, shoulder crests, the trailing haori edge and the hem all
fray, while the trunk interior stays dense. With the merged coverage buffer from
item 1 this becomes tractable: a distance estimate can be derived from the merged
coverage field itself rather than from any single ribbon's UV.

Do **not** reduce the overall amount of dissolve. Measured, 26% of the figure sits
below luminance 77 and a solid core does exist in roughly the right quantity. The
defect is distribution, not quantity.

## 3. Widen the wet bleed

**What is wrong:** the halo extends only a few pixels past the dissolve band. STYLE.md
§3.2 requires it to be **softer and larger** than the dissolve band, which is what
makes a figure sit *in* the paper rather than on it.

**Required:** 3-5x the dissolve band width, at low alpha, with a much softer falloff.
Item 1 makes this possible — the previous attempt was blocked because the bleed
physically could not extend past the authored mesh, since garments were two-vertex-wide
strips with no room outside the rail. Reading from a merged coverage buffer removes
that constraint entirely.

## 4. De-uniform the flecks

**What is wrong:** at the hem, roughly six specks of near-identical size sit in a rough
horizontal row. At the blade tip, four to five round dots of monotonically decreasing
size trail in a straight line. Both are STYLE.md §10 anti-patterns — "symmetric,
uniform particle bursts", and for the blade specifically §5's "a straight trail reads
as a generic slash VFX and fails".

**Required:** vary fleck size by 3-4x, scatter them off the axis of the edge they came
from, and vary their alpha independently of their size.

---

## Also in scope

- **Dry brush (STYLE.md §3.3) is currently too weak.** Its amplitude was reduced twice
  during pass 1 because it read as venetian blinds over the figure. That symptom was
  almost certainly item 1's banding compounding it, not the dry brush itself. Once the
  merged coverage buffer is in, raise it back to a clearly visible level and re-judge.

- **Do not touch `PaperBackground`.** The rig agent revised it in the same pass for
  ground wash and fog bands.

---

## Verification

```
./gw capture -Pscene=rig-swing -Pout=out/captures/s1-p3-swing -Pframes=12 -Pcols=4
./gw capture -Pscene=rig-bindpose -Pout=out/captures/s1-p3-bind -Pframes=4 -Pcols=4
```

Judge from the individual full-resolution frames, not only the downscaled contact
sheet cells. Specifically confirm: a horizontal scanline through the chest no longer
shows periodic ripple; the torso's *side* edges fray as the hem does; the bleed halo
is visibly wider and softer than the fleck band; and hem flecks vary in size and are
not collinear.

Protect, verbatim: the palette and value range (no pure black, no pure white), the
ochre stain bleed, and the slow non-swimming evolution of the dissolve pattern.
