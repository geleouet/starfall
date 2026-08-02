# System 1 — rig revision 2

Addendum to `docs/system1-contract.md`. Applies to `dev.starfall.rig` and
`dev.starfall.anim` only. Written after reviewing the revision-1 debug capture
`out/captures/s1-sideA-bones/contact-sheet.png`.

Revision 1 is structurally wrong in four ways. Each is a required fix, not a
suggestion.

---

## 1. The figure must be in profile, facing +X

**What is wrong:** the figure reads front-on. Both arms and both legs are visible
and symmetric, and the head is a frontal oval.

**Why it matters:** every reference image is profile or three-quarter, and the game
is a linear lane viewed from the side where facing left/right is a tactical resource
that costs a turn. A front-on figure cannot express facing at all.

**Required:**
- Bind pose is a **three-quarter profile facing +X** — the direction of the enemy.
- The near-side limbs (`upperArmL`/`forearmL`/`handL`, carrying the blade, and
  `thighL`/`shinL`/`footL`) sit forward and are drawn larger and darker.
- The far-side limbs are drawn **smaller, higher on screen, and lower contrast** —
  they read as behind the body, mostly occluded by the torso and haori.
- The head is a profile silhouette: brow, nose and chin as a single flowing contour,
  with the topknot mass sitting high and back. Reference images 1, 2 and 8.
- Facing is flipped by negating the root bone's `scaleX`, not by rebuilding the mesh.
  Verify the skinning matrices survive a negative scale without inverting the winding
  order — if they do not, flip the cull face instead.

## 2. The silhouette must be asymmetric and shoulder-heavy

**What is wrong:** the garment is a symmetric isoceles cone from the neck down. It
reads as a Christmas tree, not a samurai.

**Required, from reference images 1 and 2:**
- The widest point is at the **shoulders**, not the hem. The haori's shoulder line
  extends well past the body on both sides and has a hard-ish upper edge where the
  fabric is stretched over the shoulder, contrasting with the dissolving hem.
- The mass is **asymmetric**: fabric trails further behind the figure (-X) than in
  front, as though caught mid-motion. Roughly 1.4:1.
- A distinct **forward lean** of the spine, about 8-12 degrees, so the figure has
  intent even at rest.
- The hem is not a horizontal line. It should be ragged and rise noticeably at the
  front, exposing the lower legs, while trailing low behind.

## 3. The blade must read as a blade

**What is wrong:** the blade is nearly invisible — a dot rather than a sword. It is
the one element the eye is supposed to follow through the whole swing.

**Required:**
- ~~Length roughly **0.75 of the figure's total height**~~ — **superseded, see
  revision 3 below.**
- Thin: a few pixels at capture resolution. It must read as a sliver, not a plank.
- Slight curvature (sori), convex away from the edge. A perfectly straight quad
  reads as a stick.
- Split into its own `SkinnedMesh` returned by `bladeMesh()`, per the contract's
  revised section E, weighted entirely to the `blade` bone.

## 4. The swing must be a whole-body action

**What is wrong:** only the sword arm animates. Hips, spine, chest, head and both
legs are static across all twelve frames, so there is no overlapping action — which
STYLE.md §7.1 requires and §10 lists as a failing anti-pattern.

**Required — every one of these must be visibly animated in the contact sheet:**

| Bone group | Motion through the swing |
|---|---|
| `hips` | Weight shifts back during anticipation, forward through release, settles late. Small vertical drop at the deepest point of the cut. |
| `spine` / `chest` | Extends (arches back) during anticipation, flexes forward through release. This is the main engine of the cut, not the arm. |
| `neck` / `head` | Tracks the blade with a lag of roughly 6-10 frames. The head should still be turning after the arms have stopped. |
| far arm | Counter-rotates against the sword arm for balance. Never static. |
| `thighL/R`, `shinL/R` | Stance widens slightly through the cut; the rear leg extends as weight transfers. |
| `footL/R` | At minimum a heel lift on the rear foot. Feet must not be nailed to the ground for 2.4 seconds. |

**Phase offsets are the point.** Per STYLE.md §7.1, body, cloth and hair must never
peak on the same frame. Within the body alone: hips lead, spine follows, arms follow
the spine, head trails everything. Give each group its own small time offset rather
than driving them all from one curve.

**Timing profile stays** as contract section G specifies: ~40% anticipation,
~15% release, ~45% follow-through, slow-in and slower-out, terminal settle over
0.3-0.6s with no snap.

---

---

# Revision 3 — corrections to this document

Written after the pass-5 review closed System 1 at a FAIL. These are not new
requirements; they are places where **revision 2 above specified the wrong
thing**, and where five passes of faithful implementation therefore produced the
wrong result. See `docs/system1-debt.md` D1 and D2.

## 3a. The blade length in section 3 was wrong

"Roughly 0.75 of the figure's total height" is not a katana, it is a nodachi held
by a child. Measured in the bind pose the implementation of it rendered at about
90% of body height, ran off the right frame edge at a constant ~2 px, and never
resolved a point inside the frame.

**The number is 0.40 of the figure's height, heel to crown.** A katana's nagasa
is about 70 cm on a 170 cm swordsman. It is `BLADE_NAGASA_FRACTION` in
`SamuraiRig`, stated once.

Also required, and none of it was in revision 2:

- the blade **must terminate in a visible kissaki inside the frame**. It needs
  enough rows to resolve one — seven was two quads' worth of taper and the
  rasteriser dropped it;
- a real profile, not a triangle: gentle taper to the yokote at ~0.86, then a
  point over the last seventh;
- the sori stays, scaled with the length (~3% of nagasa);
- STYLE.md 5's **outer glow**, stronger along the ha and toward the kissaki, not
  a symmetric bloom;
- STYLE.md 3b.3's **hamon**, and it must be worth ~20 luminance levels across
  the line. On a seven-pixel blade a subtler one is not faint, it is absent;
- the blade takes the **same fog attenuation as everything else**. A weapon at
  full brightness inside a band that has erased the figure's own legs is not in
  the picture, it is on top of it. This applies to the sheath and the arc-trail
  too, which is why `Atmosphere` now evaluates the bands on the CPU.

## 3b. Contract section E's region list was incomplete

The list of mesh regions in `docs/system1-contract.md` section E — haori,
sleeves, hakama, torso/limbs, head, blade — is the whole reason debt item D1
exists. It never mentioned the hand, the grip, the guard, the sash or the worn
swords, so no pass ever had them on a work order, and the figure read as a dark
shape rather than as a samurai. **The contract's section E is amended; build
against that list, not this paragraph.**

One implementation note that cost a capture and generalises to every small part:
`frayPx` is `mix(0.22 * halfPx + 1.5, 34, dissolve^0.75)` and it is an
**absolute width in pixels**. A scabbard is 13 px across, so an authored
dissolve of 0.22 buys it a 12.6 px fray band against 6 px of half-width and the
object is deleted outright. Anything under about 30 px across is authored at
`dissolve = 0`; the shader's own floor is all the break-up it can afford.

## Verification

The `rig-bones` debug capture must show, across its twelve cells: a clearly
side-facing figure, an asymmetric shoulder-heavy silhouette, a long blade tracing a
continuous readable arc, and visible motion in hips, spine, head and legs — not just
the arm. Look at the sheet. Do not report done on a sheet you have not read.
