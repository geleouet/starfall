# STYLE.md — Visual & Motion Bible

**This document is the grading rubric.** Every review pass judges captured output against
this file and against the eight reference images in `inspirations/`. When this document and
a reviewer's personal taste disagree, this document wins. When this document and the
reference images disagree, **the reference images win** and this document gets corrected.

Project codename: **Starfall**. A 2D turn-based tactical duel on a linear lane of 5 to 15
tiles, rendered as a half-remembered ink painting.

**Setting** (see `STORY.md`): *The Atlas of Extinguished Dreams*. The world is a
cosmo-atlas drawn in ink; the player is the **Night Pilgrim**, carrying a blade forged
from a fallen star, fighting **Charted Shadows** across the **Fold of the World**.

**On the reference corpus and the setting.** Every reference image is a Japanese feudal
painting, and the setting deliberately is not. That is not a contradiction — it is the
division of labour this document runs on:

- The references are ground truth for **material, value, atmosphere and motion**. They
  are what the whole rubric is calibrated on and what produced every useful diagnosis so
  far. Grade against them without hesitation.
- They are **not** ground truth for **iconography**. Do not require a tsuba, a daisho, a
  hakama or lamellar plate. The silhouette they teach — a robed figure whose garment
  dissolves into ink, carrying one hard-edged blade — is the target; their specific
  cultural fittings are not.

Where this document still names a Japanese garment part, read it as shorthand for the
shape, not as a requirement for the object.

---

## 0. The one-sentence test

> Could this frame be cropped out of one of the eight reference images and not look out of place?

If a reviewer cannot answer *yes*, the pass fails. Everything below exists to make that
answer yes.

---

## 1. Reference corpus

The eight images in `inspirations/` fall into three families. All three are in scope; they
govern different parts of the game.

### Family A — "Ink figure on paper" (images 1, 2)
Single samurai, three-quarter/profile, standing on cream paper. Near-monochrome: indigo and
charcoal ink washes with ochre-rust stains bleeding through the armour. Cloth explodes into
wet ink clouds at the extremities. The paper ground is visible everywhere and is *warm*, not
white.

**Governs:** character rendering, the ink material, garment edge behaviour, the idle/ready pose.

### Family B — "Dusk duel" (images 3, 4, 5)
Two swordsmen in profile, blades crossed, against a sky grading from deep indigo at the top
through violet to coral/salmon at the horizon. Bodies read as near-black ink silhouettes with
just enough interior modelling to find the face and hands. Blades are pale, near-white,
luminous. At the point of contact: a soft star-shaped light bloom and a scatter of small warm
embers. Smoke/ink wisps curl off the figures into the air. Ground is a dark ink smear with
grass strokes.

**Governs:** the combat stage, the parry/clash moment, lighting, the overall colour script of
a fight. **This is the primary template for the game screen.**

### Family C — "Misty field" (images 6, 7, 8)
Figures in a pale fog-filled meadow at dusk, pink/cream sky, cloth in cool greys and bruised
blues with warm underpainting. Background figures desaturate and half-dissolve into mist.
Tiny saturated jewel-coloured light motes (cyan, magenta, amber) float as bokeh. Extremely
soft, painterly, contemplative.

**Governs:** atmosphere, depth/fog, background figures, the planning phase, calm beats,
between-wave moments.

---

## 2. Colour

### 2.1 Palette

Anchors sampled from the references. Use these as attractors, not as a locked ramp — the
material should drift within a few degrees of hue, the way real pigment does.

| Role | Hex | Notes |
|---|---|---|
| Paper ground (warm) | `#EDE4D3` | Family A background. Never pure white. |
| Paper ground (cool shadow) | `#C8C2B8` | Where wash pools on paper. |
| Sky zenith (dusk) | `#2B3A5C` | Family B top of frame. |
| Sky mid (violet) | `#5B4A6E` | The transition band. |
| Sky horizon (coral) | `#D9736B` | Family B lower third. |
| Sky horizon (hot) | `#E8907E` | Only at the very horizon line. |
| Ink black | `#161A22` | Never `#000000`. Ink is blue-black. |
| Ink indigo | `#2C3A4F` | The dominant body/cloth tone. |
| Ink slate | `#4A5A6B` | Mid-value cloth, lit side. |
| Cloth pale | `#B9BEC0` | The white-kimono duellist; cool grey, not white. |
| Ochre stain | `#B5793A` | Rust/amber bleed in armour and cloth. |
| Ochre pale | `#D8A86A` | Highlight edge of a stain. |
| Vermillion accent | `#C8382E` | Sparingly: cords, seals, blood-as-ink, danger telegraphs. |
| Blade luminance | `#EAF2F8` | Near-white, faintly cool. |
| Clash bloom core | `#FFF6E2` | Warm white, only at contact. |
| Ember | `#FF9A4D` | Sparks off a clash. |
| Mote cyan | `#5FD8E8` | Bokeh motes, Family C. |
| Mote magenta | `#E06BA8` | Bokeh motes, Family C. |
| Fog | `#D6D2CE` | At 0.0–0.85 alpha, warm-neutral. |

### 2.2 Colour rules

- **Desaturate the mid-values, saturate only the extremes.** The references keep the bulk of
  the image in muted indigo/grey and reserve saturation for the horizon, the ochre stains,
  the vermillion accents, and the jewel motes. A frame where everything is colourful has
  failed.
- **Warm/cool opposition carries the mood.** Cool ink figure against warm ground, or dark
  figure against hot horizon. Never cool-on-cool or warm-on-warm across a whole frame.
- **No pure black, no pure white, ever.** Darkest is `#161A22`; brightest non-emissive is the
  paper ground. Only the clash bloom and blade specular may approach white.
- **Vermillion is a budget.** At most a few small marks per frame. It is the only genuinely
  loud colour and it must always mean something (danger, intent, blood, a seal).

---

## 3. The ink material — the single most important rule

**Nothing in this game has a hard edge except the blades.**

Look at how the garments terminate in the references: the hem of a haori does not end, it
*frays*, breaks into separate brush marks, bleeds into a wet cloud, and dissolves into the
ground. The bottom third of nearly every figure is not a figure at all — it is ink smoke.

Implementation requirements:

1. **Edge dissolve.** Every skinned garment surface carries a per-vertex `dissolve` weight,
   0 at the body core rising to 1 at hems, sleeve ends, and trailing edges. The fragment
   shader thresholds a multi-octave noise field against this weight so the silhouette breaks
   into discrete brush flecks before vanishing. The threshold must be **soft** (smoothstep
   band ~0.12 wide) so flecks have feathered edges, not aliased ones.
2. **Wet bleed.** Outside the dissolve band, a low-alpha halo of the garment colour extends
   several pixels further, blurred, like pigment wicking into damp paper. This halo must be
   *softer and larger* than the dissolve band, so the figure reads as sitting *in* the paper
   rather than on it.
3. **Dry-brush breakup.** Inside the solid body, the paper-tooth texture must show through in
   streaks aligned with the direction of the stroke (roughly along the limb/cloth flow
   direction, not screen-aligned). Coverage is not uniform — ink skips.
4. **Value pooling.** Ink is darkest where it collects: at folds, at the trailing edge of a
   wash, at the bottom of a hanging garment. A flat-shaded garment is wrong.

   **When the base colour already sits on the floor, pool by lifting everything else.**
   §2.2 forbids anything below `#161A22`, so a material authored *at* the floor — the hair
   mass is — cannot express "ink collects here" by darkening. It has nothing left to darken
   into. Three modulation terms each centred a little below 1.0 multiplied out to 0.744x
   and printed `#12151C`, which is how the floor was breached. The rule that replaces it:
   **every multiplier applied to an ink value has a minimum of exactly 1.0.** The pooling
   rail sits *on* the floor and everything else lifts off it. Contrast ratios are identical;
   only the anchor moves.
5. **The noise field must be anchored to the *material*, not to the screen.** If the noise
   swims across the cloth as the character moves, the whole illusion collapses into
   "shader effect over a moving sprite". Sample noise in a stable material-space UV that
   deforms with the skin.
6. **The dissolve pattern must evolve slowly over time even when static** — pigment settling,
   not a frozen texture. Very slow: a full pattern turnover should take several seconds.

**Failure signatures to watch for:** crisp polygon silhouettes; a visible mesh outline; the
dissolve reading as "dithering" or "TV static"; noise swimming independently of the cloth;
flecks that are all the same size; a hem that is a straight line.

**Learned the hard way — the frequency budget of §3b.1 applies to this section too.** The
periodic torso banding that failed the first two System 1 reviews was traced to a fleck
octave running at a 4.7 px period and a dry-brush streak octave at 2.5 px, neither with a
time term. It was diagnosed at the time as a compositing problem and fixed with an
architectural change, when a frequency change would very likely have sufficed. Before
concluding that any periodic artefact is structural, **measure the world-space period of
every octave contributing to it.** An octave whose period lands near the pixel grid will
produce regular ripple no matter how the surfaces are composited.

---

## 3b. Material classes and surface detail

Reference corpus: the three 4x4 grids in `inspirations/textures/`. Call this **family E**.

Nothing in this game is a flat fill. Family E is the answer to "what is inside the
silhouette" — §3 governs how a surface *ends*, this section governs what it *is*.

### 3b.0 One reframe that changes §3

Family E shows that what §3 calls "ochre stains" are really **shibori dye blooms** — not
grime deposited on cloth, but the internal structure of the cloth itself. Irregular dark
pools with hard wet boundaries and pale cream reserves, produced by binding and dipping.

This is a better model in two ways: it explains why the marks read as belonging to the
garment rather than sitting on it, and it is far more natural to generate procedurally
(wet fronts meeting and stopping) than arbitrary blotches. The existing stain system
should be reinterpreted this way rather than kept alongside.

### 3b.1 The frequency budget — the rule that makes this work or fail

The figure is roughly 140 px wide in a 960 px capture. Sashiko stitches and embroidery
are sub-pixel at that scale. Rendering them anyway produces shimmer, and shimmer is the
artefact class that has already failed two reviews.

**Detail resolves on push-in.** Each detail octave declares a world-space period, and
fades out as that period approaches the pixel grid:

- Compute the material-space footprint of a pixel with `fwidth` on the material
  coordinate.
- Fade an octave to zero over the range where its period falls from ~6 px to ~2 px.
- Never let an octave contribute below ~2 px period. No exceptions — this is the
  anti-shimmer guarantee, not a tuning parameter.

Consequences, and they are intended: at planning framing only the shibori blooms, the
pleat lines and broad value variation are visible; as the camera glides in for execution,
weave, stitch rows, rivets and lacing emerge. The cloth resolves as the camera
approaches, the way ink reveals itself as it dries. This gives the push-in of §9 a second
reason to exist beyond the face.

All detail is sampled in **material space**, under the same anchoring rule as §3.5. Detail
that swims over moving cloth fails on sight.

### 3b.2 Palette additions

Extends §2.1.

| Role | Hex | Notes |
|---|---|---|
| Indigo deep | `#1F2A3D` | The saturated pool of a shibori bloom. |
| Indigo mid | `#3E5470` | Body of dyed cloth. |
| Indigo pale | `#8FA0B4` | Where binding resisted the dye. |
| Linen | `#CFC3AC` | Undyed cloth, cord, sashiko ground. Warm, never grey. |
| Linen pale | `#E5DCC8` | Reserve areas and cord highlights. |
| Lacquer rust | `#7A4A38` | Armour plate warmth under black lacquer. |
| Brass | `#A8873F` | Bosses and fittings. |
| Brass patina | `#6E6A45` | Aged brass in shadow. |
| Wood weathered | `#6B5A4C` | Ground planks. |

Vermillion, ochre and the ink tones of §2.1 carry over unchanged. The coral/red washes in
family E are the existing `#C8382E` at low opacity, and remain a budget per §2.2.

### 3b.3 Material classes

Each class is a distinct shader path with its own detail octaves.

**Resist-dyed indigo cloth** — the core material and most of the on-screen surface (haori,
kimono). Wet-edged dye pools, cream reserves, subtle linen weave. Detail octaves: bloom
boundaries (always visible) → weave (push-in only).

**Pleated lower robe** — parallel vertical pleats with per-pleat value variation, deforming
with the skin. This is what will finally give the lower body legibility; both System 1
reviews called it an undifferentiated curtain. Pleat lines are a mid-frequency octave and
should stay visible at planning framing.

**Plated scale and brass** — rows of lacquered plates, rivets, ochre lacing, patinated
brass bosses. Carries the warrior read and a large part of the ochre loudness the last
review demanded. This is *regular* structure, so misalignment is instantly visible —
rivet rows must follow the plate rows, which must follow the body's curvature.

**Stitched linen and cordage** — beige linen with running-stitch grids, braided cord, and
frayed knots. Small on-screen area but high value contrast; these are the light warm
accents that break the indigo at the obi, the sageo and the braids.

**Star-blade steel** — pale steel with a visible **temper line**, the wavy boundary between
hard edge and softer spine. Present in all three grids. Since the blade is the object the eye
follows, the temper line is the one high-frequency detail that should remain faintly
readable even at planning framing.

**Weathered wood** — vertical grain, cracks, knots. The lane's ground plane.

**Aged paper** — stains, foxing, hairline cracks, deckled edge. The substrate for all UI
per §8, so that UI reads as marks on the same sheet the figures are painted on rather
than as a layer floating above it.

**Embroidered silk** — woven floral and constellation motifs with visible satin-stitch
direction, on coral ground. **Reserved for the hero and bosses only.** It is high
frequency on small surface, so it only survives on a character the player looks at for a
long time; on an ordinary enemy it would be sub-pixel noise.

### 3b.4 Scope

Textures apply to **characters, environment and UI alike**. Every reference image is a
single continuous surface — the figure, the ground and the paper are the same material
event at different densities. A textured character on an untextured ground would break
that, and a UI drawn on anything other than the same aged paper would read as a layer
plated on top.

The background may stay atmospheric and near-empty where §6 requires it; "textured" does
not mean "busy". Fog, wash and emptiness are themselves part of the material.

### 3b.5 Texture anti-patterns

| Anti-pattern | Why it fails |
|---|---|
| Any flat fill | Family E's whole point |
| Detail below ~2 px period | Shimmer; already the cause of two review failures |
| Detail in screen space | Swims over moving cloth; same failure as §3.5 |
| Tiling with a visible repeat | Reads as a texture asset, not a dyed cloth |
| Uniform, evenly spaced shibori blooms | Reads as polka dots; blooms are irregular and vary in scale |
| Rivet or stitch rows that ignore body curvature | Instantly exposes the flat mesh underneath |
| Embroidery on ordinary enemies | Sub-pixel noise; reserved for hero and bosses |
| Sharp, high-contrast weave at wide framing | Fights the silhouette the whole game is built on |

---

## 4. Hair

Hair in the references is **not a cloth sheet**. It is a bundle of individually drawn ink
strands: a dark mass near the scalp resolving into long, thin, curling wisps that trail far
into open air — sometimes half a body-length — tapering to a hairline point.

Requirements:

- Simulated as independent Verlet strands, not a mesh. 12–24 strands, 8–14 particles each.
- Rendered as **tapered ribbons**: wide and near-opaque at the root, narrowing to sub-pixel
  and near-transparent at the tip. The taper must be nonlinear (fast narrowing in the last
  third) so tips look like a brush lifting off the paper.
- Strand paths must be **smoothed** (Catmull-Rom through the particles) — a visible polyline
  kink is an instant fail.
- Strands must not move in unison. Per-strand variation in length, mass, damping, and a small
  phase-offset noise so the bundle reads as many hairs, not one flapping flag.
- A few "escapee" strands with much lower mass that lag dramatically and curl — these are what
  sell the dreamlike quality and are prominent in images 6, 7, 8.
- Hair leads and lags the head. On a direction change, tips should still be travelling the old
  way for a noticeable beat.

---

## 4b. Faces

Reference corpus: the twenty portraits in `inspirations/faces/`. Call this **family D**.

### 4b.0 The reinterpretation problem — read this first

Family D is **front-facing, tightly cropped**: the head fills a thousand pixels and both
eyes are fully resolved. The game is a linear lane viewed from the side, with the figure
in **profile** at roughly 68% of frame height, which puts the head at 20-30 pixels. These
two framings cannot coexist, and the decision is that **faces live only in the combat
view** — there is no separate portrait layer.

So family D is not a target to reproduce. It is a source for four things that *do*
survive the translation to a small profile head:

1. the **material** — carnation tones, wet edges, paper showing through;
2. the **value structure** — where the darks and the light fall on a head;
3. the **eye treatment** — the single highest-contrast object on the figure;
4. the **marks** — ochre stains, freckle specks, scars.

What does *not* transfer: frontal symmetry, both eyes, and any detail finer than a few
pixels. Do not chase it.

**Corollary:** the camera push-in of §9 is now load-bearing. It is the only moment the
face is legible at all, so the execution framing must get genuinely close.

### 4b.1 The face is exempt from the ink dissolve

**This is the most important rule in this section.** The dissolve of §3 exists to destroy
edges. A face is the one place in the frame where edges carry meaning, and applying the
dissolve to it would eat exactly the detail that makes family D worth referencing.

- `dissolve` is authored **0 across the entire face and skull**, with no rim boost.
- The face may still receive `wetness` pooling and `stainMask`, which are what tie it back
  to the ink material.
- The hairline is where the two treatments meet: hair dissolves and frays, skin does not.
  That boundary should be a **hard wet edge**, not a blend.

### 4b.2 Skin palette

Extends §2.1. Every one of these is desaturated relative to real skin — family D's faces
are pale, cool and papery, with warmth reserved for small areas.

| Role | Hex | Notes |
|---|---|---|
| Skin base | `#E3D2BB` | Cream-ochre. The lit plane of the face. |
| Skin mid | `#C9BCAC` | Turning form. |
| Skin shadow | `#8E8C99` | **Cool grey-violet.** Never a brown shadow. This opposition is what makes family D read as watercolour. |
| Skin deep | `#5E5C68` | Under the jaw, eye socket, nostril. |
| Blush | `#C98878` | Cheeks, nose tip, ear. Asymmetric, blotchy, wet-edged. |
| Lip | `#B5636B` | The warmest note on the face. |
| Sclera | `#D8D5CE` | **Never white.** |
| Iris | `#2A2620` | Near-black warm. |
| Specular | `#FFF6E2` | A single small dot. See 4b.4. |
| Brow / lash | `#161A22` | Ink black, same as §2.1. |

Ochre stain and vermillion from §2.1 carry over unchanged for skin marks.

### 4b.3 Profile construction

At profile, only five things read. Build these and nothing else:

- **The contour** — brow, nose, lip, chin as *one continuous flowing line*, per §4b of the
  rig spec. This single silhouette carries most of the character's identity.
- **The brow line** — a heavy ink stroke. The primary expression carrier at distance.
- **One eye**, in three-quarter foreshortening.
- **The jaw and neck join** — a dark wedge separating head from body.
- **The gaze direction.**

Everything else (nostril, ear, lip parting, cheekbone) is a bonus that appears only at
push-in framing.

### 4b.4 The eye

In every family D portrait the eye is the focal point, and it earns that by being the
**highest-contrast object in the frame** — near-black iris against pale sclera, ringed by
an ink lash line, with one small bright specular.

- Exactly **one** specular dot, and it must not be centred — offset toward the light.
- The lash line is heavier on the upper lid than the lower. Never a symmetric outline.
- The sclera is never pure white and is usually partly shadowed by the upper lid.
- At small scale, if only two pixels survive, they must be **dark iris + specular**. Author
  the eye so it degrades to that.

### 4b.5 Marks

Family D puts marks on nearly every face, and they are what stop the skin reading as a
flat fill:

- **Ochre stains** — the same vocabulary as the garment stains of §3, blooming through the
  skin. Sparse and irregular.
- **Freckle specks** — small dark flecks scattered across the cheekbone and nose bridge.
  Varied in size; never a regular grid.
- **Scars** — thin vermillion-ochre marks, one or two at most, placed asymmetrically.
  Several family D faces carry them and they read as history rather than damage.

### 4b.6 Dynamic behaviour

Faces are **reactive and varied**. Two independent systems.

**Expression** — driven by combat state, not played as canned clips. Four channels:

| Channel | Range | Carries |
|---|---|---|
| Brow | raised / neutral / drawn-down | Effort, focus, alarm |
| Eyelid | wide / neutral / narrowed / closed | Intensity, pain, death |
| Jaw | closed / set / open | Exertion, cry, slack on death |
| Gaze | target-tracked | Intent — who this character is about to act on |

Expression must obey §7's motion law: it blends and settles, it never snaps. A face that
pops between expressions is as wrong as a limb that snaps. Terminal settle 0.3-0.6 s,
same as everything else.

**Variety** — hero and bosses are authored; ordinary enemies come from a seeded generator
parameterised on age, build, brow weight, jaw shape, hair mass, facial hair, and marks.
Family D covers a young woman, an old woman, a bearded man and a middle-aged man, which
is the range the generator must span. An old face is not a young face with lines: it is a
different silhouette — sunken eye socket, softened jaw, different hair mass.

### 4b.7 Face anti-patterns

| Anti-pattern | Why it fails |
|---|---|
| The ink dissolve applied to the face | Destroys the only meaningful edges in the frame |
| Both eyes visible in profile | The single most common 2D-rig tell |
| Pure white sclera | Same reason as §2.2 — no pure white anywhere |
| Cel-shaded or flat-fill skin | Wrong medium; family D is wash with wet edges |
| Brown shadows on skin | Family D's shadows are cool grey-violet; brown reads as plastic |
| Symmetric blush / symmetric marks | Reads as makeup, not as pigment |
| Expressions that pop between states | Violates §7; expression settles like everything else |
| A regular grid of freckles | Reads as texture, not as skin |
| Anime-style huge eyes | Wrong register entirely; family D is painterly realism |

---

## 5. Blades

The **only** hard-edged, high-contrast element in the frame, and deliberately so — the blade
is the point of visual focus in every duel reference.

- Near-white `#EAF2F8`, faintly cool, with a soft outer glow of the same hue at low alpha.
- Thin. In the references the blade is a *sliver*. Resist making it chunky.
- The glow is **not** a symmetric bloom — it is stronger along the edge and at the tip, and it
  smears slightly along the direction of travel.
- **Motion:** a swung blade leaves a soft luminous arc-trail that fades over ~0.4 s. The trail
  is a smooth ribbon following the blade's swept path, brightest at the leading edge, and it
  must curve — a straight trail reads as a generic "slash VFX" and fails.
- **Clash:** a soft star bloom (`#FFF6E2` core) with 4–6 long soft rays, plus 8–20 warm
  embers `#FF9A4D` that drift *slowly* outward and upward and fade. Embers must not fly like
  sparks from a grinder — they float like paper ash.

---

## 6. Atmosphere & depth

- **Fog bands.** Horizontal drifting bands of `#D6D2CE` at varying alpha that occlude the
  lower body of figures and separate depth layers. This is present in every single reference
  image and is a major part of why they read as dreams. Non-negotiable.
- **Depth desaturation.** Background figures lose saturation and contrast and gain fog until
  they are barely more than a value shape (see the trailing figures in images 6, 7, 8).
- **Jewel motes.** Small out-of-focus coloured lights (cyan, magenta, amber) drifting slowly.
  Sparse — a dozen or two in frame. They belong to Family C but should appear at low density
  even during fights.
- **Wash blooms in the ground plane.** The "floor" is not a floor. It is an ink smear with
  grass strokes and wet blooms, fading to nothing at the edges of the frame.
- **Vignette by wash, not by black.** Darken frame edges with a warm-grey wash, never a black
  gradient.

---

## 7. Motion — the oneiric law

> Motion is choreography, not physics. Every movement is a brushstroke being drawn.

### 7.0 The positive test — §7.2 is necessary and nowhere near sufficient

**Everything in §7.2 is a negative.** No snapping, no overshoot, no stretching, no strobing.
A system can satisfy every one of them and still read as a machine, because a list of ways
to fail is not a recipe for succeeding. Add all the absences together and you get motion
that never does anything wrong, which is not the same as motion that does something.

This was learned from System 2's first visual pass, where a solver that provably never
flipped an elbow, never stretched a bone, never overshot and settled monotonically to the
pixel was still, correctly, failed. The measurement that exposed it: across four seconds
containing two teleports and two hard reversals, **the hip moved 2 px and the head 4 px
while the hand travelled 130 px.**

So alongside §7.2, every motion system is graded on three positives:

1. **Motion must have a source.** In every reference image the figure is a spiral — the hip
   turns before the shoulder, which turns before the elbow, which turns before the tip. The
   arm is never the subject; it is the end of a sentence the body started. Movement that
   simply begins at the effector reads instantly as a puppet, before any analysis.
   *Acceptance criterion: the body's own centroids must move measurably during any limb
   action. A frozen torso fails regardless of how good the limb is.*

2. **Effort must be visible.** The corpus's limbs are gravity-loaded and reluctant — elbows
   held between 90° and 130°, upper arms hanging near the torso axis, the *blade* doing the
   reaching. A limb that extends fully because a coordinate permitted it reads as a linkage.
   A limb that declines to straighten reads as a brushstroke.

3. **Nothing may arrive at the same time.** §10's last row bans everything peaking on the
   same frame, and it applies *within* a chain, not only between body and cloth. The wrist
   should arrive after the elbow; the blade tip should drift a beat after the hand has
   stopped. This is the cheapest poetry available and it is almost free — it requires only
   that settle times differ down the chain rather than being shared.

A reviewer should state explicitly whether motion is **poetic or merely correct**, and if
merely correct, say what would give it an origin.

### 7.1 Timing

- **Slow in, slower out.** Nothing arrives at rest abruptly. Terminal damping should let a
  limb settle over 0.3–0.6 s after the visible motion has "ended".
- **Anticipation is long, release is smooth, recovery is long.** A strike is roughly
  40% wind-up / 15% travel / 45% follow-through. Fighting-game timing (fast wind-up, hard
  freeze on impact) is the exact opposite and is forbidden.
- **No impact freeze. No screen shake. No hitstop.** These are the crunchiest tools in the
  game-feel box and every one of them is banned. Impact is expressed by *ink*: a bloom, a
  spray of flecks, a spreading stain, a change in how cloth trails.
- **Overlapping action.** Body, cloth, and hair must never peak on the same frame. Cloth
  trails the body by ~4–8 frames, hair tips by ~8–14.

  **Name the anchor, or the number means nothing.** A hem trails the *hips*; a sleeve trails
  the *wrist*, which is itself already far behind the hips because it hangs off an IK chain
  carrying its own settle. Both readings are defensible and they differ by a factor of
  three, so a lag figure quoted without its anchor is unfalsifiable. State it every time.

  **And a lag must be measured, not eyeballed.** Every lag this section specifies is a beat
  you *feel*; none of them is a thing you can point at in a contact sheet, at any capture
  cadence. Fixing the capture rate (§11.2) made lag gradeable by a *reviewer*; it did not
  make it visible. **Any timing claim ships with a headless measurement**, and that
  measurement must drive the same scene the capture runs rather than re-enacting it.

### 7.2 Extreme cases (this is where the aesthetic dies)

The brief is explicit that fast motion is where dreamlike quality is easiest to lose. On
direction snaps, impacts, parries and knockback, verify:

- **No snapping.** Verlet constraint solving must not visibly "pop" a strand straight. Use
  enough relaxation iterations and cap per-step correction.
- **No mechanical overshoot-and-return.** A spring that visibly oscillates twice and stops
  reads as a machine. Damping should be high enough that there is at most one soft return.

  **Measured against a disturbance, never against ambient motion.** Under a continuous
  driver — a breeze, per-strand turbulence — "one soft return" is not a well-posed question,
  and the naive test is actively misleading: strands appeared to ring two or three times,
  and *damping them harder made it worse*, which is the unmistakable signature of measuring
  the driver rather than a resonance. Kill the ambient input, apply one impulse, and count
  the returns.
- **No stretching artefacts.** Skinning must not candy-wrap at the elbow/shoulder on extreme
  angles.
- **Knockback is a drift, not a launch.** A struck figure should be *carried* backward like
  a sheet of silk caught in wind, arriving over ~0.8 s, cloth and hair streaming ahead of
  the body's arrival.

  **"Streaming ahead" is a displacement, not an arrival time.** Read as an arrival-time
  claim it is both false and unbuildable — a chain hanging off a body cannot beat that body
  to a destination, and a test asserting it will fail correctly. What the references show is
  hair and cloth *thrown out in front* along the direction of travel: measure the growing
  tip-to-root offset during the strike, not who gets there first.
- **Parry is a deflection curve, not a collision.** The blades should slide and redirect
  along a smooth arc; the defender's arm gives ground on an IK curve rather than stopping dead.
- **Fast motion should smear, not strobe.** A blade crossing the frame in 3 frames must leave
  a continuous luminous ribbon, not three discrete blade poses.

### 7.3 Hit reactions

Poetic beats, not physical impacts. The vocabulary:

- **A bloom of ink** spreading from the contact point through the garment, like a drop
  hitting wet paper.
- **A held breath** — a brief slowing of everything (a soft time ramp, ~0.85× for ~0.25 s),
  never a hard freeze.
- **A shedding of flecks** — the dissolve threshold pushed locally so the struck area sheds
  brush flecks that drift away.
- **A yielding** — the whole figure gives on a soft IK curve, spine bending, rather than
  recoiling on a hard offset.

---

## 8. UI

- **No chrome.** No boxes, no bevels, no bars with borders, no drop shadows, no gradients that
  look like glass. Nothing that reads as "widget".
- UI elements are **brush marks on the paper**: the action queue is three ink cartouches, the
  health is a column of ink strokes that dry out and fade as it drops, the tile grid is a row
  of faint wash marks that only intensify near relevant tiles.
- **Enemy intent telegraphs** are the one place vermillion is spent freely: a thin vermillion
  wash over threatened tiles, appearing with a wet-bleed animation rather than a fade-in.
- Type: a serif or brush-derived face, generously letterspaced, low contrast against the
  ground. Never a UI sans-serif.
- **Transitions** are washes and bleeds, never slides or pops. Elements arrive by pigment
  spreading into place (0.4–0.7 s) and leave by drying out and fading.

---

## 9. Camera

Per the chosen framing: **wide to plan, push in to strike.**

- **Planning framing:** the full lane readable, figures small, heavy fog, Family C
  mood. Slight slow drift — the camera is never perfectly still.
- **Execution framing:** on queue execution, the camera glides (never cuts) toward the
  exchange over ~0.5 s with a soft ease, ending near the intimacy of images 3/4/5 — figures
  large, blades crossing near frame centre.
- **Return** is slower than the push-in (~0.8 s).
- **Never cut. Never shake.** All camera movement is eased and continuous.
- A subtle parallax between fog layers, figures, and ground sells depth during the move.

---

## 10. Explicit anti-patterns

A reviewer should fail a pass on sight of any of these:

| Anti-pattern | Why it fails |
|---|---|
| Screen shake, hitstop, impact freeze | Crunchy fighting-game feel; the exact opposite of the brief |
| Hard-edged sprites / visible polygon silhouettes | Kills the ink material |
| Pure black or pure white | Flattens the painterly value range |
| Saturated colour across large areas | References are muted; saturation is an accent budget |
| Neon glow / bloom on everything | Drifts to generic indie-glow, away from ink |
| Symmetric, uniform particle bursts | Reads as a particle system, not as ink |
| Snappy springs, visible oscillation | Mechanical, not dreamlike |
| Uniform hair motion | Reads as a flag, not as hair |
| UI panels, bars, boxes, borders | Chrome; breaks the paper illusion |
| Screen-space noise that swims over moving surfaces | Exposes the shader, breaks material illusion |
| Straight-line slash VFX | Generic action game |
| Cel-shaded / anime flat fills | Wrong medium entirely — this is wash, not cel |
| Everything peaking on the same frame | No overlapping action; reads as rigid |

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

---

*Revision 1 — established before any code was written, from the eight reference images and
the two project briefs.*
