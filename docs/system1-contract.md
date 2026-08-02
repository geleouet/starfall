# System 1 contract — skeleton, skinning, ink material

Two implementations are built in parallel against this contract. **Neither side may
change anything in this document without the change being reflected here first.**
If you believe the contract is wrong, say so in your final report rather than
silently deviating — a silent deviation breaks the other half of the system.

Package root: `dev.starfall`

---

## A. Vertex format — the hard boundary

Skinned meshes are built with exactly this `VertexAttributes` layout, in this order:

```java
new VertexAttributes(
    VertexAttribute.Position(),          // a_position   : vec3 (z always 0)
    VertexAttribute.TexCoords(0),        // a_texCoord0  : vec2  material-space UV
    VertexAttribute.ColorUnpacked(),     // a_color      : vec4  per-vertex tint
    VertexAttribute.BoneWeight(0),       // a_boneWeight0: vec2 (boneIndex, weight)
    VertexAttribute.BoneWeight(1),       // a_boneWeight1: vec2
    VertexAttribute.BoneWeight(2),       // a_boneWeight2: vec2
    VertexAttribute.BoneWeight(3))       // a_boneWeight3: vec2
```

That is **17 floats per vertex**: position 3 + uv 2 + color 4 + bone weights 4x2.
Derive it in code with `mesh.getVertexSize() / 4`; do not hardcode 17 in two places.

Rules:
- Up to **4 bone influences** per vertex. Unused slots must be `(0f, 0f)` — index 0,
  weight 0 — never a garbage index with zero weight.
- Weights for a vertex **must sum to 1.0** (normalise at build time).
- `a_color` carries per-vertex material data, not literal colour. See section C.
- `a_texCoord0` is **material-space**, stable under deformation. This is what makes
  the ink noise stick to the cloth instead of swimming across it (STYLE.md §3.5).

## B. Bone matrix uniform

```glsl
const int MAX_BONES = 32;
uniform mat4 u_bones[MAX_BONES];   // skinning matrices, already = globalPose * inverseBind
uniform mat4 u_projTrans;          // camera
```

Uploaded with `ShaderProgram#setUniformMatrix4fv("u_bones", floats, 0, boneCount * 16)`.
2D transforms live in the upper-left 2x2 plus the translation column; z is untouched.
**32 bones is a hard cap** — chosen to stay inside the GLES 3.0 guaranteed minimum of
256 vertex uniform vectors (32 mat4 = 128 vec4), per the mobile-safe decision.

## C. Per-vertex material channel (`a_color`)

Not a colour. Four independent scalars in 0..1 driving the ink material:

| Channel | Name | Meaning |
|---|---|---|
| `r` | **dissolve** | 0 at the body core, 1 at hems / sleeve ends / trailing edges. Drives edge break-up. STYLE.md §3.1. |
| `g` | **wetness** | How much pigment pools here. Raises local darkness and bleed radius. High at folds and lower hems. STYLE.md §3.4. |
| `b` | **stainMask** | Where ochre-rust underpainting shows through. Sparse, blotchy. STYLE.md §2.1 OCHRE. |
| `a` | **flowU** | Direction of the brush stroke at this vertex, encoded as `angle / TWO_PI` in 0..1. Drives dry-brush streak orientation so streaks run along the limb, not screen-aligned. STYLE.md §3.3. |

## D. Java API — implemented by side A

```java
package dev.starfall.anim;

public final class Bone {
    public final String name;
    public final int index;
    public final Bone parent;          // null for root
    // Local bind pose
    public float bindX, bindY, bindRotDeg, bindScaleX, bindScaleY;
    // Animated local transform (what animation writes to)
    public float x, y, rotDeg, scaleX, scaleY;
    public void resetToBind();
}

public final class Skeleton {
    public Skeleton(List<Bone> bonesInParentFirstOrder);
    public int boneCount();
    public Bone bone(int index);
    public Bone bone(String name);
    /** Recomputes global transforms from local ones. Parent-first, single pass. */
    public void updateWorldTransforms();
    /** Fills dst with boneCount*16 floats: globalPose * inverseBind, column-major mat4. */
    public void fillSkinningMatrices(float[] dst);
    /** World-space position of a bone's origin, after updateWorldTransforms(). */
    public Vector2 worldPosition(int boneIndex, Vector2 out);
    public float worldRotationDeg(int boneIndex);
}

public final class SkinnedMesh {
    public Mesh mesh();          // built with the layout in section A
    public int vertexCount();
    public void dispose();
}
```

`fillSkinningMatrices` must produce matrices that map **bind-space vertex positions**
to **posed world positions**. Inverse bind matrices are computed once at construction.

## E. Rig + mesh builder — implemented by side A

```java
package dev.starfall.rig;

public final class SamuraiRig {
    public static SamuraiRig build();        // skeleton + skinned meshes, procedural
    public Skeleton skeleton();
    public SkinnedMesh mesh();               // body + all cloth
    public SkinnedMesh bladeMesh();          // the blade alone, same skeleton
    public void applyPose(Pose pose);        // writes local transforms
}
```

Bone hierarchy (parent-first order, names are load-bearing — side B may reference them):

```
root
└─ hips
   ├─ spine        ─ chest ─ neck ─ head
   │                 ├─ shoulderL ─ upperArmL ─ forearmL ─ handL ─ blade
   │                 └─ shoulderR ─ upperArmR ─ forearmR ─ handR
   ├─ thighL ─ shinL ─ footL
   ├─ thighR ─ shinR ─ footR
   └─ (skirt/haori bones added by System 3, reserve indices)
```

Keep total bones ≤ 24 so System 3 can add cloth bones inside the 32 cap.

Mesh regions to build, each a skinned quad-strip or fan, with `a_color` authored per
section C:
- **haori** (outer robe) — the big shape. Must extend well below the legs and have a
  wide dissolve gradient at the hem: `dissolve` should reach 1.0 over roughly the
  bottom 35% of the garment, not just the last row of vertices. This is the single
  most important mesh in the whole system.
- **sleeves** — wide, hanging, heavy dissolve at the openings.
- **hakama** (trousers) — wide-legged, dissolve at ankles.
- **torso/limbs** — narrow, low dissolve, mostly hidden by cloth.
- **head** — simple; face detail is not in scope for System 1.

Revision 3 adds the six regions below. **They are not decoration.** The list
above is what the contract shipped with, and because a hand, a grip, a guard, a
sash and the worn swords were never on it, no pass ever built them; the pass-5
review recorded a figure that resolved "a torso, a topknot and a hairline white
scratch" against a reference resolving fourteen parts at the same figure height
(STYLE.md §11.0). No later system supplies any of these — IK moves an arm it
does not create. This is mesh authoring and nothing else.

All six are small, and small means `dissolve` at or near **zero**: `frayPx` is an
absolute pixel width, so a hem-sized fray band does not soften a 26 px mark, it
deletes it.

- **hand** — the fist on the grip, authored on the *blade's* axis so it actually
  sits on the tsuka, skinned to `handL` so a wrist IK solver carries it.
- **tsuka** and **tsuba** — two distinct masses, not one lozenge. The guard is
  the widest mark in the cluster and the last to survive downscaling.
- **obi** — the sash. Three rails (dark / warm / dark), because what makes it
  read is the sandwich, not the outline. It is also the only thing separating
  the shoulder-heavy upper mass from the hakama.
- **saya** and a **second sheathed sword** crossing behind the hip, skinned to
  `hips`. Their trailing ends must finish *outside* the garment silhouette in
  open paper; that is where they do their silhouette work.

Value, not outline, is what makes the grip cluster read, and this is the part
that took five passes to find. A hand and a hilt mesh existed from revision 3
onward, correctly placed and drawn last, and the review still found nothing
between cloth and steel — because they were authored at the same wetness as the
sleeve around them, and the whole cloth path resolves into a ~30-level ramp
between `INK_INDIGO` and `INK_BLACK`. Reading outward the cluster must step
sleeve → hand (warm) → tsuka (near-black) → tsuba (black) → steel (near-white).
The warm step comes from `stainMask` authored above ~0.5, which `ink_skin.frag`
treats as a *fitting* rather than a dye bloom: reliable gate, muted amount.

- **blade** — the one hard-edged mesh: `dissolve = 0` everywhere. Built into a
  **separate** `SkinnedMesh` returned by `bladeMesh()`, because it is the only part of
  the figure drawn with a different material (near-white, `emissive = true`) and
  `InkMaterial` applies per draw call. Same skeleton, weighted to the `blade` bone.

Silhouette reference: images 1 and 2 in `inspirations/`. Profile/three-quarter,
shoulders wide, garment flaring outward and downward into ink.

## F. Rendering — implemented by side B

```java
package dev.starfall.render;

public final class InkSkinnedRenderer {
    public InkSkinnedRenderer();
    public void begin(Matrix4 projTrans, float timeSeconds);
    public void draw(SkinnedMesh mesh, Skeleton skeleton, InkMaterial material);
    public void end();
    public void dispose();
}

public final class InkMaterial {
    public Color base       = Palette.INK_INDIGO;
    public Color deep       = Palette.INK_BLACK;   // where wetness pools
    public Color stain      = Palette.OCHRE;
    public float dissolveBias   = 0f;   // +ve dissolves more (used for hit reactions later)
    public float bleedRadius    = 1f;
    public float paperGrain     = 1f;
    public boolean emissive     = false; // true for blades
}
```

Side B also owns:
- `dev.starfall.render.PaperBackground` — the warm paper / dusk sky ground with
  fog bands, per STYLE.md §2 and §6. Needed so captures are judged in context and
  not against a void. API is exactly:
  ```java
  public final class PaperBackground {
      public PaperBackground();
      public void render(Matrix4 projTrans, float timeSeconds);
      public void dispose();
  }
  ```
  Revision 2: fog cannot occlude the figure from a background pass drawn before it,
  which STYLE.md §6 requires. The fog bands are therefore also evaluated inside
  `ink_skin.frag` from world position, sharing constants through
  `dev.starfall.render.Atmosphere` and a `u_fogBands` uniform, so the figure fades
  into the same mist the paper shows. This is the sanctioned approach; do not add a
  separate foreground pass.
- The GLSL in `src/main/resources/shaders/`: `ink_skin.vert`, `ink_skin.frag`.

Shader requirements are STYLE.md §3 in full. The non-negotiables:
1. Edge dissolve thresholding multi-octave noise against the `dissolve` channel, with
   a **soft** smoothstep band (~0.12) so flecks are feathered, never aliased.
2. A wider, softer, low-alpha wet-bleed halo **outside** the dissolve band.
3. Dry-brush streaks aligned to `flowU`, not screen-aligned.
4. Value pooling driven by `wetness`.
5. Noise sampled in **material space** — either bind-space vertex position or a
   *metric* material UV — so it does not swim under deformation. Revision 2:
   originally this said `a_texCoord0`, but that attribute is authored per-region
   0..1, so its metric scale differs by ~30x between the haori hem and a wrist
   sliver; sampling noise there makes fleck size vary by that factor and stretches
   every mark. Bind-space position is used instead. `a_texCoord0` remains the
   source for boundary distance and along-strip gradients.
6. Slow temporal evolution of the dissolve pattern (`u_time`), full turnover over
   several seconds.

GLSL version: `#version 300 es` compatible constructs, or GLSL 120 with no features
beyond GLES 3.0. No compute, no derivatives beyond `fwidth`, no texture arrays.
Procedural noise only — no texture assets.

## G. Scenes — the capture surface

Both sides register scenes in `SceneRegistry`. Required for review:

| Scene name | What it must show |
|---|---|
| `rig-bindpose` | The samurai in bind pose, full figure, paper background. Static. |
| `rig-swing` | A 2.4s overhead sword swing: long anticipation, smooth release, long follow-through, per STYLE.md §7.1 (roughly 40/15/45). |
| `rig-bones` | Debug overlay: bone segments and joints drawn over a ghosted mesh. For technical verification only, not aesthetic review. |

`rig-swing` is the scene the reviewer grades. It must be authored so that the
contact sheet reads as a **trajectory**, not a set of poses.

---

## H. Build and capture

```bash
./gw capture -Pscene=rig-swing -Pout=out/captures/s1-p1 -Pframes=12 -Pcols=4 -Pw=960 -Ph=540
```

`./gw` pins the build to JDK 21; do not invoke Gradle directly, and do not change
`JAVA_HOME`. The command must exit 0 and print `CAPTURE_SHEET=` before you report done.
