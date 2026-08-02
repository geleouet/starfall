# System 1 — rig revision 3

Addendum to `docs/system1-contract.md` and `docs/system1-rig-fixes.md`. Applies to
`dev.starfall.rig`, `dev.starfall.anim` and `dev.starfall.render.PaperBackground` only.

Written from the independent review of pass 2
(`out/captures/s1-p2-swing/`). Every item below is backed by a pixel measurement from
that capture, not by taste.

Revision 2 fixed real things — the profile, the blade geometry, the whole-body swing
envelope, the grounding. This revision is about the four faults that stand between the
current build and a pass, plus three that stand between a pass and something that could
be cropped into reference image 2.

---

## 1. Give the figure shoulders — the single highest-impact fix in System 1

**Measured:** in `frame_011.png` the silhouette is a cone. Shoulder line (y≈175) is
**~55 px wide**. Hem (y≈420) is **~185 px wide**, widening monotonically all the way
down. Shoulder span is therefore about **0.3x** hip span.

**Reference:** every samurai in references 1, 2, 3, 4 and 5 is the exact reverse. Sode
and haori sleeves make the shoulder the widest horizontal in the figure, the obi pinches
at the waist, and the hakama flares again below. Reference 2's shoulder span is roughly
**1.6x** its hip span.

**Why it matters more than anything else:** this, not the head, is why the figure reads
as a cloaked wraith. A monk's cassock, a Nazgûl, a Dementor — all cone silhouettes. The
wraith read cannot be fixed by fixing the head while the shoulder mass is absent. This
one change converts a wraith into a samurai and is worth more than the next three items
combined.

**Required:** widen the shoulder and upper-arm mass until the shoulder line is the widest
horizontal in the figure; pinch the waist at the obi; let the hakama flare again below
it. Target shoulder:hip ≈ 1.6:1. Revision 2 already authors the haori as independent
front/back contours, so this is a change to the shoulder row's width, not a rewrite.

## 2. Weld the head, the arm and the blade to the body

**Measured, all in pass 2:** in `frame_007.png` there is clean paper between the head
blob's lower edge and the top of the neck wedge. In `frame_011.png` at approximately
(520,175) a pale notch of paper separates the top of the sleeve from the shoulder. In
`frame_007.png` at approximately (618,290) the blade begins in open paper roughly **8 px
clear of the sleeve end** — no hand, no tsuba, no tsuka.

**Reference:** images 3, 4 and 5 are near-black silhouettes and they *still* resolve the
hand and the sword guard, because that join is what tells you a person is holding the
thing. They spend their entire interior-modelling budget there.

**Required:** close all three paper gaps. Draw a hand and a tsuba mass — a solid dark
lozenge is sufficient and is what the references do. The figure currently reads as a
floating line beside a floating sleeve beside a floating ball.

## 3. Fix the leg alpha — the legs exist, they are compositing wrong

**Correction to a previous assumption:** the legs are NOT missing. In `frame_007.png` at
4x there is a clear leg column from hip to foot with a knee break and a foot wedge. The
fault is that it renders as a **pale, translucent, hard-faceted ghost tube in front of a
darker skirt, with a bright hard vertical seam down its centre** (visible in
`frame_011.png`). That is an alpha-sorting or double-blend fault, and it costs twice: the
legs do not read as anatomy, *and* the seam reads as a polygon edge, which STYLE.md §10
fails on sight.

**Required:** sort or merge the leg geometry into the skirt so the lower body composites
as one dark wet mass rather than translucent tubes over cloth.

## 4. Invert the ink gravity

**Measured:** down the column x=440 in `frame_007.png`, the mid-torso sits at luminance
~60 and the hem region fades to **~137-160** before it dies. Ink is pooling at the chest
and thinning at the floor.

**Reference:** references 1 and 2 dissolve the lower body into ink smoke but the *value*
stays dark and wet down there — the hem is the darkest, heaviest part of the wash.
STYLE.md §3.4 explicitly requires ink to be darkest where it collects, including at the
bottom of hanging garments.

**Required:** author `wetness` so the value gradient runs the other way — lightest at the
chest, pooling to near `#161A22` immediately above the fray line. Revision 2 moved in
this direction but the measurement says it has not landed.

## 5. Invert the ground and mist

**Measured:** vertical column at x=250 in `frame_007.png`, clean background:
`y=330 -> 231,223,207` (R−B = 24), `y=450 -> 215,208,196` (R−B = 19),
`y=530 -> 176,173,167` (R−B = 9). The band gets **55 luminance units darker and loses
60% of its warmth** as it descends.

**Reference:** in references 6, 7 and 8, and in the lower third of 1 and 2, the mist is
the **lightest and warmest** region in the frame. It is what the figures dissolve *into* —
an emitter, not a subtractive haze. The current treatment is a neutral-grey subtractive
gradient, which is the definition of a muddy fog bank and is why it reads as smoke or
dirt rather than luminous morning air.

**Required:**
- The mist must be **lighter and warmer** than the paper, not darker and greyer.
- Break it into **2-4 horizontal drifting bands at varying alpha**. It is currently one
  monotonic vertical ramp, so nothing separates depth layers. STYLE.md §6 asks for bands.
- The topmost band must **occlude the figure's lower third**, not sit behind it. §6 calls
  fog occlusion of the lower body non-negotiable, and currently the lowest figure ink at
  x=440 is y≈445 while the band is a smooth gradient below and behind it — there is clean
  paper between hem and haze.
- Add a few **grass strokes drawn over the hem**. Reference 2 draws grass strokes across
  the figure's legs; that single trick does more for grounding than any amount of haze.

## 6. Reshape the swing envelope inside the timing it now has

**What is now right and must not regress:** measured ink-centroid displacement per frame
gives wind-up ≈45%, cut ≈9%, follow-through ≈45%. Against the 40/15/45 target that is a
genuine hit and a real improvement — pass 1 had four pixel-identical dead frames.

**Three faults inside that envelope:**
- **The anticipation is the fastest part of the clip.** Blade tip travels ~400 px in the
  first 0.44 s: f0 (746,461) → f1 (391,442) → f2 (347,143). STYLE.md §7.1 requires
  anticipation to be the *slow* part. Slow it down.
- **The apex leaves the frame.** At f3, f4 and f5 the blade is entirely outside the top of
  frame, so 18% of the clip has an invisible weapon. Keep the apex in frame.
- **The follow-through is an elastic rebound, not a settle.** Blade tip after the cut:
  f6 (809,450) → f7 (856,351) → f8 (843,359) → f9 (778,437) → f10 (719,484) →
  f11 (689,498). The blade cuts down, **bounces back up 100 px**, sinks past its landing
  point, and is still descending at the final frame — a 40% overshoot on a 240 px blade.
  STYLE.md §7.2: "no mechanical overshoot-and-return". This is the spring, visible.
  Replace it with a monotonic decelerating settle that comes to rest *before* the last
  frame.

Note the body centroid moves only 10 px across f8-f11 while the arm is still bouncing —
the body arrives before the sword. That is overlapping action inverted; §7.1 wants cloth
trailing the body, not the body trailing the weapon.

## 7. Rebuild the head as a directional mass

The uniform 360-degree radial fringe and the interior speckle are the shader agent's to
fix. The **silhouette** is yours: a solid opaque crown, a clean jaw-side interruption, and
a distinct topknot lobe offset from the skull. Full strand simulation is System 3; a
directional silhouette is not, and the head is currently the only high-frequency object in
an otherwise low-frequency image, which is the only reason the eye lands there.

## 8. Raise the mote chroma (in `PaperBackground`)

Frame-wide maximum saturation is **0.23**. The jewel motes are present in geometry but
blended to invisibility; spec `#5FD8E8` is 0.59 saturation and `#E06BA8` is 0.52. Push
them to spec at a dozen tiny points and change nothing else — STYLE.md §2.2 wants
desaturated mid-values with saturation reserved for the extremes.

---

## Do not touch

`src/main/resources/shaders/*`, `dev.starfall.render.InkSkinnedRenderer`,
`dev.starfall.render.InkMaterial`. The shader agent is revising the compositing path, the
blade material, the dissolve octaves and the ochre chroma in parallel.

## Protect verbatim — confirmed correct by measurement across two reviews

- **Material-anchored noise.** Hem flecks translate with the body rather than swimming.
  Called the requirement most builds fail.
- **Value floor and ceiling.** Ink black measures exactly `22,26,34` against a `#161A22`
  spec; paper `226,217,202`, warm, never white.
- **Blade taper and curve.** 6 px to 1 px over 210 px, converging to a true point, with a
  progressive bow. The most reference-accurate object in the build. Do not touch the
  blade's geometry.
- **The sword-arm sleeve** in `frame_006.png` is the one part of the figure with a
  convincing wash — tapered, soft-edged, with dry-brush interior variation. Whatever
  produces that sleeve is what the torso should be produced by.

## Verification

```
./gw test
./gw capture -Pscene=rig-swing -Pout=out/captures/s1-p4-swing -Pframes=12 -Pcols=4
./gw capture -Pscene=rig-bindpose -Pout=out/captures/s1-p4-bind -Pframes=4 -Pcols=4
```

Judge from individual full-resolution frames. Confirm: the shoulder line is the widest
horizontal in the figure; no paper gap at neck, shoulder or hand; a visible hand and
guard; the lower body is one dark mass with no bright vertical seam; ink is darkest just
above the hem; mist is lighter and warmer than paper and crosses the figure's lower third;
the blade apex stays in frame and the follow-through settles without rebounding.
