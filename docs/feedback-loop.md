# The feedback loop — capture, measure, drive

Written for an agent picking this up cold. Everything here works from a shell today. The MCP
server at the end is the same thing wrapped for a session that has restarted.

Three commands cover most of it:

```bash
./gw capture -Pscene=sim-extreme -Pframes=24 -Pcols=6 -Pstart=1.95 -Pstep=0.0167 -Pout=out/captures/s4-p1-parry
./gw analyse -Pargs="report out/captures/s4-p1-parry"
./gw analyse -Pargs="track out/captures/s4-p1-parry --anchor hips --fps 60 --method register"
```

Never invoke gradle directly. `./gw` pins JDK 21; the machine's `JAVA_HOME` points at a JDK the
pinned Gradle cannot run on.

---

## 1. Capture

`./gw capture` renders a scene offscreen at a fixed timestep and writes `frame_NNN.png` plus a
labelled `contact-sheet.png`. It is deterministic: the same arguments produce the same bytes on
any machine, which is what makes iteration-to-iteration comparison mean anything.

| flag | meaning |
|---|---|
| `-Pscene` | a registered scene name — see below |
| `-Pframes` `-Pcols` | frame count and contact-sheet columns |
| `-Pstart` | seconds after the scene's warmup at which frame 0 is taken |
| `-Pstep` | **seconds between frames — set this** |
| `-Pclamp=cloth` | weld every cloth chain to bind — the rigid control of §7.1 |
| `-Pw` `-Ph` `-Plabel` `-Pout` | render size, sheet title, output directory |

Every capture writes a `capture.txt` beside its frames: the command that reproduces it, the
scene, window and substep, `clamp=`/`clothRigid=`, and — since System 3 pass 5 — `commit=` and
`harness=`. `harness` is a digest of the *capture path's* compiled bytecode and nothing else, so
it changes when the apparatus changes and not when a shader or a rig does. STYLE.md §11.2b(d):
an absolute pixel statistic is valid only against the harness that produced it.

To list the scenes without starting anything, ask for one that does not exist — the registry
prints the full set in the error:

```bash
./gw capture -Pscene=list 2>&1 | grep "Known scenes"
```

With the debug API running, `node tools/sfctl.mjs scenes` does the same thing politely.

### The cadence rule, which is not optional

**Anything about timing must be captured at a true frame rate.** STYLE.md §11.2. Without
`-Pstep` the harness spreads `-Pframes` across the whole scene, which for a 3.6 s scene samples
every 0.327 s — while §7.1 specifies overlapping action in the 0.067–0.133 s band. Every lag the
motion systems are required to produce is then shorter than one delivered sample, and a review
literally cannot see whether the chain staggers or arrives as a unit.

> Pass 3 escaped §10's last row only because the sampling was too coarse to convict it. That is
> not the same as complying.

So: `-Pstep=0.0167` with a `-Pstart` aimed at the beat that matters — the reversal, the impact,
the settle. 24 frames then covers 0.38 s at 60 Hz. Contact sheets label the captured window, not
the scene duration.

### Debug overlays

Most scenes have a `-debug` sibling with the same driver and the same frames, drawn so targets,
poles, particles and constraints are visible. Ship both. The graded capture cannot show them and
most of what goes wrong is a question about something the graded capture hides.

---

## 2. Measure — `dev.starfall.analysis`

```bash
./gw analyse                                     # usage
./gw analyse -Pargs="report out/captures/X"      # the standard battery
```

Every command takes a capture directory or a single PNG, and `--json` for machine-readable
output. Coordinates are image coordinates: origin top left, +y down.

| command | question it answers |
|---|---|
| `report` | everything at once: paper, figure, values, coverage and bands by region, periodic artefacts, per-frame change |
| `figure` | where the figure is and how tall; with `--reference`, the §11.0 matched-scale factor |
| `regions` | resolve the region set against this frame; `--emit` writes it to edit and commit |
| `coverage` | ink coverage in a region, and that region's share of the figure's ink |
| `bands` | mean ink luminance top to bottom — the ink-gravity test |
| `track` | centroid / registration tracking, velocity, reversals, and **lag against a named anchor** |
| `autocorr` | periodic artefacts on a high-passed band, lags 4–200 px |
| `edge` | paper→core distance and wet-bleed halo width across a silhouette |
| `marks` | mark-width runs on cuts through a region, and whether they are bimodal |
| `values` | floor against `#161A22`, ceiling against paper |
| `diff` | two frames or two directories, pixel statistics plus md5 |
| `timing` | read a headless timing series and report arrivals in samples and seconds |

### Regions — the thing that makes a number checkable

A pixel measurement without its rectangle cannot be reproduced or refuted. Every number in
`docs/system1-debt.md`, `system2-debt.md` and `system3-debt.md` was taken through a rectangle
those documents do not record, and boxes exist in `s3-p1-reversal` that give the recorded 23%
hair coverage *and* boxes that give 42%. This is the same failure STYLE.md §7.1 already named
for lag anchors, one level down.

So regions are named, checked in, and printed beside every number.

```bash
--region hips                            # from the loaded region set
--region hemtip=489,419,31,36            # absolute pixels
--region hair=fig:-0.06,-0.01,0.70,0.30  # fractions of the detected figure box  <- prefer this
--region band=img:0,0.82,1,0.14          # fractions of the frame
--regions docs/regions.json              # a whole set
```

`docs/regions.json` holds the default set for the samurai figure, expressed as fractions of the
figure box so it transfers between captures at different scales and positions. It is a starting
point, not ground truth: **when the rig changes shape, re-check it and commit the change.** The
point is not that these boxes are right, it is that whatever a review used is written down.

`./gw analyse -Pargs="regions <dir>"` prints the resolved rectangles for the frame in front of
you. Do that before quoting anything.

### Lag needs its anchor — enforced, not documented

`track` **refuses to run without `--anchor`.** From §7.1:

> A hem trails the *hips*; a sleeve trails the *wrist*, which is itself already far behind the
> hips because it hangs off an IK chain carrying its own settle. Both readings are defensible
> and they differ by a factor of three, so a lag figure quoted without its anchor is
> unfalsifiable. State it every time.

There is no default, in the CLI or in `Arrivals`. A tool that made the anchor optional would
make the unfalsifiable version the easy one to produce.

Two further defaults exist for the same reason:

- **The dominant reversal, not the first.** A hair bundle in an ambient breeze reverses several
  times before the body does anything; comparing first crossings puts the hair ahead of the
  hips. `--reversal first|dominant|N` to override.
- **Only reversals turning the same way as the anchor's.** Otherwise a chain mixes the turn
  *into* an impulse with the turn *out of* it, and the sleeve appears to lead the hips by four
  frames when it in fact trails them by eleven. `--any-direction` to override.

Useful options: `--method centroid|register` (centroid for soft masses that change shape,
registration for marks that keep it), `--axis x|y|principal`, `--smooth N`, `--fps 60` to get
lags in seconds, `--gate` for the noise floor.

### Timing claims need a headless measurement

§7.1: *any timing claim ships with a headless measurement, and that measurement must drive the
same scene the capture runs rather than re-enacting it.* A previous pass reported peak frames at
126/132/139/146/147/150 for a window that covered frames 111–134.

```bash
./gw timing -Pscene=sim-extreme -Pstart=1.0 -Pduration=0.4 -Prate=240 -Pout=out/timing/run.json
./gw analyse -Pargs="timing out/timing/run.json --anchor hips --smooth 2"
```

`./gw timing` builds the scene through the same `SceneRegistry`, warms it up the same way,
advances it through the same `SceneClock.SUBSTEP`, and renders through the same offscreen path
as `./gw capture`. It only samples more often and writes numbers instead of PNGs. Sample index
and capture frame index are the same clock, so a measurement made this way cannot report a peak
outside its own window. Region rectangles are resolved once and written into the output file.

A scene may also implement `dev.starfall.capture.SceneProbe` to report its own simulation state;
when it does, `timing` records that alongside the pixel measurement. That is how you tell "the
particle moved but the picture did not" from "nothing moved" — a distinction `system3-debt.md`
turns on.

**`SimScene` implements it** (`dev.starfall.rig.SimProbe`), so `sim-sway`, `sim-extreme` and
`sim-impulse` all report the hips and head bones, every particle of the back cloth rail, the
front and sleeve tips, and the longest wisp's tip — in the image pixels the capture writes,
through the scene's own camera. `analyse timing` folds them into the same arrival chain as the
pixel regions, **tagged `sim:`**, and prints the box each particle swept beside it:

```
  skirtHigh      trails hips       by   +0.87 frames      <- the picture
  sim:back1      trails hips       by   +3.95 frames      <- the particle inside that box
```

Those two lines are the whole reason the interface exists. A `sim:` row is *not* a pixel
measurement and must never be quoted as one — STYLE.md grades the picture. Pass 3 used exactly
this pair to show that the cloth solver's lag had doubled while the delivered pixels had not
moved, and then to find out why (the graded box was mostly obi, thigh and scabbard).

The complementary control is worth knowing because it is cheap: **capture the same window with
the thing you are measuring switched off.** `-Pclamp=cloth` welds every cloth chain to its bind
pose, and if the statistic through your box barely changes, your box is not measuring cloth. On
`skirtHigh` that control reads +0.34 of the +0.87 frames.

```bash
./gw capture -Pscene=sim-sway -Pout=out/captures/x       -Pframes=90 -Pcols=10 -Pstart=1.0 -Pstep=0.0167
./gw capture -Pscene=sim-sway -Pout=out/captures/x-rigid -Pframes=90 -Pcols=10 -Pstart=1.0 -Pstep=0.0167 -Pclamp=cloth
./gw analyse -Pargs="drape out/captures/x --region skirtBack --region hips --anchor hips \
                     --control out/captures/x-rigid --axis x --fps 60"
```

**`--control` is checked, not trusted.** It refuses a directory with no `capture.txt`, one whose
`clamp` is not `cloth`, one whose scene / start / step / frames / size differ from the live
capture's, and one shot through a different `harness=` — a manifest with no `harness=` line
counts as its own (older) harness rather than as a wildcard, so a capture from before the
harness fix cannot be scored against one from after it. `--allow-harness-drift` waives only the
last, and has to be typed. STYLE.md §11.2b(d) and (e); `ControlGuardTest` asserts all five.

### The reach gate — does this particle paint anything?

`analyse timing` prints STYLE.md §7.1's one surviving scalar gate under the arrival chain, and
returns non-zero when it fails:

```
STYLE.md 7.1 reach gate  -- a particle paints when the darkest pixel within its
  paint radius is <= 122.4, the midpoint between paper 219.0 and the #161A22 ink floor 25.73.
  back4      x437..446 y448..452 (10x5)  paints on 0.0% of samples, darkest 142.2  <- PAINTS NOTHING
  2 of 8 simulated cloth particles paint nothing (gate: 0)  -> FAIL
```

The threshold is the midpoint between paper and the ink floor, not the 0.85×paper ink
threshold, because a wet halo measures as "ink" at 0.85×paper while reading as empty — and it
is not the figure's bounding box either, because a particle can sit well inside that rectangle
while hanging in open paper beside the skirt. Which is what `back4` did for four passes.

---

## 3. The debug API

A loopback HTTP server, **off unless `--debug-server` is passed**, so it never runs in a normal
build. Running `DebugLauncher` without the flag exits with an explanation.

```bash
./gw debugServer -Pport=7671 -Pscene=sim-extreme            # hidden
./gw debugServer -Pport=7671 -Pscene=sim-extreme -Pvisible  # watch it work
```

Simulated time never advances on its own. The render loop only drains the command queue and
redraws the current state, so the state a command sees is the state the previous command left,
and two identical command sequences produce identical pixels. A capture taken through the API is
**bit-identical** to `./gw capture` with the same scene, start and step — verified, and the
response tells you the exact command that reproduces it.

| endpoint | body |
|---|---|
| `GET /health` `GET /scenes` `GET /state` | — |
| `POST /scene` | `{"name": "sim-extreme"}` |
| `POST /time` | `{"t": 1.95}` absolute simulated seconds |
| `POST /step` | `{"frames": 4, "dt": 0.0167}` |
| `POST /camera` | `{"x":420,"y":90,"w":260,"h":260,"outW":520}` or `{"reset":true}` |
| `POST /event` | `{"name":"knockback","args":{"dir":"left"}}` |
| `POST /frame` | `{"out":"out/debug/one.png"}` |
| `POST /capture` | `{"out":"out/captures/x","frames":24,"step":0.0167,"start":1.95,"cols":6}` |
| `POST /measure` | `{"regions":["hair","hips"]}` — measures the live frame, writes nothing |
| `POST /regions` | `{"file":"docs/regions.json"}` |
| `POST /analyse` | `{"args":["report","out/captures/x"]}` — the analysis CLI in the server's JVM |
| `POST /shutdown` | — |

Two design notes worth knowing:

- **The camera is a crop, not a projection change.** Scenes do not expose a camera, and framing
  the capture on a zone of interest must never alter the thing being measured.
- **Seeking backwards rebuilds the scene and replays from warmup.** Scenes are forward-only, and
  a seek that merely stopped updating would leave a Verlet solver in a state no capture can
  reproduce.

Scenes that are time-scripted rather than event-driven — which is all of them today — report
`/event` as unsupported and list nothing under `events`. Drive those with `/time` and `/step`.
To make a scene event-driven, implement `dev.starfall.capture.SceneEvents`.

---

## 4. The CLI client — `tools/sfctl.mjs`

Node, no dependencies, no restart. This is the half of the loop that works today.

```bash
node tools/sfctl.mjs scenes
node tools/sfctl.mjs scene sim-extreme
node tools/sfctl.mjs seek 1.95
node tools/sfctl.mjs step --frames 4 --dt 0.0167
node tools/sfctl.mjs camera --x 420 --y 90 --w 260 --h 260 --out-w 520 --out-h 520
node tools/sfctl.mjs measure --region hair --region hips
node tools/sfctl.mjs capture out/captures/s4-p1-parry --frames 24 --step 0.0167 --start 1.95 --cols 6
node tools/sfctl.mjs analyse track out/captures/s4-p1-parry --anchor hips --fps 60
node tools/sfctl.mjs shutdown
```

`--port` (default 7671, or `$STARFALL_DEBUG_PORT`), `--json` for the raw response. Anything
`sfctl` does, `curl` does; anything either does, `./gw` does without a server at all.

---

## 5. The MCP server — needs a restart

`mcp/starfall-mcp.mjs`, registered in `.mcp.json`, exposing `list_scenes`, `get_state`,
`load_scene`, `set_time`, `step`, `set_camera`, `trigger_animation`, `capture_frame`,
`capture_sequence`, `measure_frame` and `analyse_capture`. Each tool is one HTTP call to the
endpoint above; there is no logic in it.

**Claude Code loads MCP servers at startup, so these tools do not exist until the session
restarts.** That is the only thing in this document that needs one. Everything else — capture,
analysis, timing, the debug API, `sfctl` — works in the session it was written in.

The server starts `./gw debugServer` on demand, so the first tool call after a cold start can
take up to a minute. Set `STARFALL_AUTOSTART=0` to manage the server yourself, which is what you
want if you would like to watch the window.

It speaks MCP over stdio directly rather than through `@modelcontextprotocol/sdk`, so there is
nothing to `npm install`. Same reasoning as the JDK-only JSON in `dev.starfall.analysis`: the
whole loop must be runnable with no network fetch.

---

## 6. What a review should actually do

In this order, because the first step has repeatedly invalidated the rest:

1. **`analyse figure <capture> --reference <inspiration.png>`.** Downscale the reference to the
   same figure height, put them side by side, and count readable parts on each. STYLE.md §11.0.
   Five System 1 passes refined how ink behaves at the edge of a shape before anyone asked
   whether there were enough shapes.
2. **`analyse regions <capture>`** and, if you are going to quote a number, commit the region
   set you used.
3. **`analyse report <capture>`** for the still-frame battery.
4. **`analyse track <capture> --anchor <region>`** for motion, on a capture taken with
   `-Pstep=0.0167`. A timing claim from a coarse capture is unfalsifiable.
5. **`./gw timing` plus `analyse timing`** for anything you are going to state as a lag.
6. **`analyse diff`** against the previous pass's captures for the regression claims, and quote
   the md5 when you claim something is unchanged.

A reviewer must never grade work it produced, and must look at the captured pixels rather than
the source when judging aesthetics.

---

## 7. Known limits of these tools

Stated plainly, because a measurement tool that oversells itself is worse than none.

- **Region boxes are judgement, not measurement.** The tool guarantees you know which box a
  number came from; it does not tell you the box was the right one. Coverage and ink-share
  figures move by a factor of two across defensible boxes for the same body part. Reversal
  frames are far more robust — they moved under half a frame across box variations that moved
  coverage from 23% to 42%.
- **Registration is a single translation per region per step.** It has nothing to say about
  rotation, and it will report nonsense for a region whose contents change shape faster than
  they move. Use `--method centroid` for soft masses, and check the clipping warning.
- **`analyse figure` finds the largest connected ink component.** If a capture ever renders two
  figures, this measures whichever one is bigger.
- **The halo threshold sits at twice the paper's own noise.** The paper has visible tooth and
  oscillates about four levels; a flatter threshold reports a 12 px halo in front of a mark that
  has none.
- **`monotoneSpeed` is strict.** Sub-pixel jitter breaks it on almost any real capture. Use the
  peak-speed step to say "this window is pure acceleration": if the peak is at the last delivered
  step, nothing has arrived yet.
