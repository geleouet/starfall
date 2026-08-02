# System 2 — standing debt

**Status: PASSED at pass 3.** This is what it owes anyway. Nothing here blocked the pass;
all of it is worth doing and some of it constrains System 4.

Source: the independent review of pass 3
(`out/captures/s2-p3-gesture/`, `s2-p3-extreme/`, `s2-p3-parry/`, `s2-p3-reach/`).

The verdict on the thing that mattered:

> `ik-gesture` is poetic. Not richly, not yet reliably, but genuinely — and for the right
> reason, which is that it now contains an idea about a body rather than a proof about a
> solver.

And the qualification that is the whole debt in one line:

> The poetry is an **event** rather than a **condition**. The hip moves during the gesture
> and returns. Images 1 and 2 are standing still and are more loaded than any frame here.

---

## E1 — The spiral should be authored into the bind pose, not only into the response

The figure shifts its weight and puts it back. Measured over `ik-gesture`: pelvis x ranges
5.3 px, thigh 3.5 px, upper spine 10.4 px on a 315 px figure. Torso lean goes +3.45° →
−3.52° → +4.19°, an 8° swing returning to within 0.7° of its start. `frame_000` and
`frame_011` are near-identical, near-symmetrical, and weightless.

References 1 and 2 are static paintings that contain more implied motion than any frame
here, because the *stance* carries it: hip loaded over one foot, shoulders counter-rotated
against the hip line, head turned down and away. The spiral is a permanent structural fact
of the pose before anything moves.

**Fix:** offset the pelvis over the weighted foot, counter-rotate the shoulder line against
the hip line, drop the head off-axis — so that the rest pose already reads as a body under
load and the working swing rides on top of a stance rather than oscillating about a
symmetric rest.

STYLE.md §7.0.1's "the body's own centroids must move measurably" is a **diagnostic for the
absence of that stance, not the cure for it**. Pass 3 satisfied the diagnostic.

## E2 — The grip-and-guard cluster folds into the torso silhouette at some poses

This is a binding concern and therefore System 2's, not System 1's.

The grip + guard cluster is the highest-contrast small mark on the figure and the only
thing that tells the eye where the body ends and the weapon begins. In
`s2-p3-gesture/frame_009.png` it sits clear against paper. In
`s2-p3-gesture/frame_006.png`, `s2-p3-extreme/frame_006.png` and `frame_010.png` the solver
folds the hand inside the torso outline and the cluster disappears into the mass.

**Fix:** a soft penalty when the wrist projects inside the body's own silhouette, so the
solver prefers solutions that keep the cluster clear against paper.

## E3 — Stagger the terminal settle down the chain

STYLE.md §7.0.3, and the review calls it "nearly free".

Measured in `s2-p3-extreme` frames 20-23, per-frame movement: head 0.3 / 0.0 / 0.0, chest
0.4 / 0.1 / 0.1, waist 0.5 / 0.0 / 0.1, hip 0.1 / 0.0 / 0.0, blade tip 3 / 1 / 0 px. The tip
outlasts the body by about one frame; everything else stops together. The settle is monotone
with no oscillation, which satisfies §7.2 — but the chain arrives as a unit, and §10's last
row fails a pass on sight for everything peaking on the same frame.

**Fix:** pelvis shortest settle, then spine, shoulder, elbow, wrist, tip, spread across
0.3-0.6 s, so the tip is still drifting perceptibly (>4 px at this figure scale, not 2) a
quarter second after the hips have stopped.

## E4 — Widen the hip's contribution at extreme reach

In `s2-p3-extreme` the body lean does real work — torso lean sweeps −0.6° → −16.0° → −4.0°
→ −16.7° as a smooth eased arc — but the pelvis band still only ranges 20 px.

**Fix:** let the pelvis carry more of an out-of-reach target before the arm is asked to, so
that "I cannot reach that" reads as the body straining rather than the arm giving up.

## E5 — Ship the stress case's debug overlay

`ik-gesture-debug` exists; **`ik-extreme-debug` does not**. So for the §7.2 case — the one
where targets teleport and reach 2.85× — there is no chain, no target and no pole visible,
and a reviewer cannot see where the hand went versus where it was asked to go. The easy case
got the instrumentation and the hard case did not.

---

## The exemption `ik-extreme` has, stated narrowly so it cannot be reused loosely

The implementer argued `ik-extreme` should stay a test rather than become a picture, since
its targets teleport and reach 2.85× and no reference contains that. The review's ruling:

- **Rejected as framed.** §7.2 exists precisely because fast motion is where the aesthetic
  dies, and STYLE.md nominates the extremes as where grading happens, not where it is
  suspended. "No reference contains a teleport" is not a defence, because System 5 will
  drive these chains with targets that move as fast as a parry, and every frame it produces
  is a frame a player looks at. **An unreferenced *stimulus* does not license an
  unreferenced *response*.**
- **Accepted narrowly.** `ik-extreme` is a stress rig and is not obliged to answer the
  one-sentence test *as a composition* — it is not staged, framed or lit to be a picture.
  It **is** obliged never to produce a frame violating §7.2 or §10, and on that standard it
  passes cleanly.

**The exemption is from composition, not from the aesthetic.** Recorded in this form
deliberately, because the looser version will otherwise be reused as cover.

---

## What must not regress — System 4 will drive these chains

- **Planted feet under a moving hip.** Foot markers hold to the sub-pixel across 3.6 s while
  the pelvis moves and the torso leans 8°. The moment feet slide to service a target, the
  weight shift becomes a translation and the whole source-of-motion fix evaporates.
- **The elbow floor.** 90-119° measured, and a refusal to straighten even at 2.85× reach.
  Any code treating "reach the point" as a hard constraint will straighten the arm and
  restore the linkage that failed pass 2.
- **The upper arm on the torso axis** (0-13° measured). This is what makes the blade rather
  than the arm do the reaching.
- **Monotone settle, no oscillation.**
- **Silhouette hardness flat across pose extremity.** Hard-edge pixel count measured
  3,366-4,396 and *does not grow* with pose extremity — maximum reach measures below rest.
  No candy-wrap, no tearing, interior voids under 0.8%.
- **The smooth trail at 316 px/frame tip speed.** It survives the fastest motion in the whole
  capture corpus. Do not let a re-parameterisation for combat reintroduce angle wrapping.
- **`TwoBoneIk`'s bend side as a continuous damped state with hysteresis, blended in angle
  space**; the **soft reach ceiling and fold floor**; the **unwrap-against-last-frame blend**
  in `IkChain.writeBack`; **critical damping**; and the **soft slew ceiling**. All carried
  forward from the pass-2 review and all still load-bearing.

---

## The methodological finding, which was fixed immediately

The review could not grade §7.0.3 at all, because **the capture cadence was too coarse to
resolve the thing pass 3 was built to do**. `ik-gesture` shipped 12 frames over 3.6 s
(0.327 s/frame) and `ik-extreme` 24 over 4 s (0.167 s/frame), while §7.1 specifies
overlapping action in the 4-8 frame band at 60 Hz — 0.067 to 0.133 s. *Every* lag the
motion systems are required to produce is shorter than one delivered sample.

> Pass 3 currently escapes §10's last row only because the sampling is too coarse to convict
> it. That is not the same as complying.

**Fixed in the harness:** `-Pstart` and `-Pstep` now capture a short window at a true frame
rate, e.g.
`./gw capture -Pscene=ik-gesture -Pframes=24 -Pcols=6 -Pstart=1.0 -Pstep=0.0167`
gives 24 frames over 0.38 s at 60 Hz. Contact sheets label the captured window rather than
the scene's full duration. **Any future review that grades timing must use this**, and any
claim about lag or stagger made from a coarse capture is unfalsifiable.
