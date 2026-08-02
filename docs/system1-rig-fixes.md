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
- Length roughly **0.75 of the figure's total height**, following the reference
  images where the katana is a long, thin, dominant diagonal.
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

## Verification

The `rig-bones` debug capture must show, across its twelve cells: a clearly
side-facing figure, an asymmetric shoulder-heavy silhouette, a long blade tracing a
continuous readable arc, and visible motion in hips, spine, head and legs — not just
the arm. Look at the sheet. Do not report done on a sheet you have not read.
