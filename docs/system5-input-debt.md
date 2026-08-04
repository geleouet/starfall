# System 5, the input pass — standing debt

**Status: shipped, not self-graded.** This file records what was built, what was
measured, what was played, and what is still owed. A reviewer must never grade
work it produced; nothing here is a verdict on the pictures.

Suite: **457 tests, 0 failures, 0 skipped** at `./gw test --rerun-tasks` (parse
`build/test-results/test/*.xml`, do not trust an UP-TO-DATE build). Every capture
quoted is `s5-input-*`, shot at the harness in its own `capture.txt`; every number
about the interface is a difference against a `-bare` sibling in the identical
window (§11.2b(g)), printed beside its rectangle (§11.3), through an independent
PIL/Rec.709 reader (§11.2b(c)).

---

## 0. What this pass is

The first pass in seven systems whose deliverable is not a rendering. It joins
the tested engine (`CombatEngine`), the tested ordinal→seconds mapping
(`Scheduler`), and the tested sheet (`Readout`/`LaneInterface`) to **a player**:

- `Session` — the live loop: commands on a wall clock, one accumulating
  timeline, a quiet-gate that makes all-or-nothing true in the picture.
- `Director.rescore` — the performance continues under a longer score; clock,
  carries, origins and fists survive the handover (guarded, red-observed).
- `PlayScene` — keyboard (digits bank, backspace un-banks LIFO, enter commits,
  T turns, H holds, space held = 3× fast-forward, F = 1.6× brisk toggle),
  `SceneEvents` for the debug API, and deterministic `Plays.Pilot`s for the
  capture harness. One `command()` gate for all three; nothing a pilot can do
  that a key cannot.
- **Enemy hit points drawn** — `Look.Shadow`, a horizontal row of the hero's
  own stroke grammar pinned above each body, spent strokes staying as ghosts,
  drying off with the body's dissolve. §8's confusable-pair warning is answered
  by anchoring, not by a new vocabulary: the row is horizontal (stanza is
  vertical), lives in the world (health lives in the margin), and moves with
  the body and the camera (no margin mark ever does).
- **Both endings reachable and staged.** `play-victory` and `play-defeat` are
  pilots playing through the same Session; defeat resolves as BP4 asked — the
  Pilgrim dissolves, the sheet dries off over ~2.8 s, and the last frames are a
  page with one pale figure on it. No card, no text.

## 1. Delivered numbers (all live − bare, regions beside)

| claim | capture | region | number |
|---|---|---|---|
| Bulwark's 5 strokes separable at 720 | `s5-input-fold-plan` f3 | `x712..812 y295..304`, densest row 299 | **5 runs**, peak Δ 57.8 |
| Reacher's 4 at 720 | same | `x575..660 y295..304`, row 299 | **4 runs**, peak Δ 56.6 |
| Bulwark's 5 at 540 | `s5-input-fold-plan-540` f3 | `x532..614 y219..228`, row 224 | **5 runs**, peak Δ 54.8 |
| Reacher's 4 at 540 | same | `x430..496 y219..228`, row 224 | **4 runs**, peak Δ 51.6 |
| The count drops at the blade's second | `s5-input-victory-blow` ×36 @0.0167 | `x540..640 y215..245` | flat 853.4 through f20 (t=13.884); **595.8 at f23 (t=13.934)**; staged wound ≈13.88 — delivered within 3 frames |
| The hero's last stroke dries at the blow | `s5-input-defeat-blow` ×36 @0.0167 | `x25..114 y15..41` | flat 7782.8 through t=9.217; **6001.5 at t=9.250**; staged blow ≈9.24 |

Countability is also guarded, not only measured: the shadow-row guard enumerates
both shipped heights × all five archetypes × every hp count × three framings
(`ShadowRowTest`), red-observed at pitch 0.8 widths ("a player would count 1" of
a Bulwark's 5) and red on its **first honest run** — see §3.

## 2. What playing found (the brief's real deliverable)

Three sessions were played turn by turn over the debug API — `play-fold` to a
Reacher kill at 2 hp remaining, `play-knife` six turns in, `play-approach`
through a full five-tile stanza — plus the two pilot fights. Everything below
divides into **measured** and **judgement**; only a human can settle the second
kind.

### 2.1 Tempo — chosen, then measured

**Chosen:** authored tempo 1.0 (the schedule's own seconds), hold-space
fast-forward at **3.0×**, a persistent 1.6× "brisk" toggle on F. Measured turn
costs at 1.0×: bank/hold **1.82–2.30 s**, one-tile execute **3.2–3.5 s**, the
full five-tile stanza + enemy phrase **6.97 s, uninterruptible** — the audit's
J3 number, now real. At 3× those become 0.6/1.1/2.3 s.

**The finding only playing could make:** camera moves are delayed, never
compressed (§9), and one turn emits ~1.66 s of camera (push-in 0.64 + return
1.02, nine-tile lane) against 1.12–2.2 s of beats — so a player who commands
the instant the score goes quiet banks ~0.5 s of camera debt per turn. Driven
at that cadence the victory fight's camera was **2.4 s behind by the third
command, and the phrase's push-in began at t=7.15 after the phrase's last
contact at 6.9** (camera keys dumped and recorded). At a human cadence the debt
never accumulates; the scripted pilots therefore take a 1.4 s breath
(`PlayScene.PILOT_BREATH`), and what a fast player is owed is fast-forward —
which scales clock and camera together — not a compressed glide.

**Judgement, human-owed:** input unlocks when the beats end, which is usually
~1 s before the return glide lands, so re-planning sometimes starts against a
still-intimate shot. Driving via API I found it mildly disorienting; whether it
reads as rhythm or as lag needs hands on keys. Also every enemy *walk* gets a
full push-in; at 1.0× the fight is stately verging on heavy. If an amendment is
wanted, the shape I would propose (report only — the rule is §9's):
*"Execution framing: on queue execution **and enemy attacks**, the camera
glides toward the exchange"* → adding *"an enemy beat that only repositions
takes a drift toward its span, not a push-in"* — but this re-frames graded
`lane-*` captures and must be its own decision.

### 2.2 §3d.3 — the cooldowns are not inert; they are still not choreography

The claim "anything under cooldown 5 is back before you want it" assumes
five-tile phrases. **Played, the dominant phrase length is 1–2 tiles** (fold and
knife both), and at that cadence cd 3–4 bound the fight exactly: my Thrust —
the only tile that reaches a Reacher — was refused mid-fold-fight at the very
turn the Reacher's cycle window opened, and Parry (cd 4) was unavailable when
the Bulwark arrived, which decided the endgame's shape. Cooldown 3 against the
Reacher's period-3 strike-stay-withdraw cycle is a genuine rhythm: you get one
reach-2 answer per cycle and you feel the window.

What stands of the §0 contradiction: the numbers now *do* something at real
phrase lengths, but what they do is still gating, not choreography — recovery
is felt as waiting, never seen as gesture. My judgement: keep the scale, spend
design on making recovery *visible in the world* rather than on retuning the
arithmetic. The economy item stays open; playing has moved it from "nearly
inert" to "binding at short phrases, invisible as motion".

### 2.3 §1.1a — the lane/queue pairing, confirmed with one sharp edge

The table in §1.1a is close to exactly right, played:

- **Knife (5):** no composition exists. Against a reach-2 Reacher there is
  *nowhere on the lane outside its threat*, so every banked tile costs a
  guaranteed stroke of ink; I spent 6 hp to deal 2. One-tile phrases of the
  right reach are the whole game, and the **Draw** — haul + wound, contact at
  distance — was the best-feeling tile in the hand. Sharp edge: the Reacher
  archetype dominates short lanes to the point of near-unfairness. Judgement:
  a knife encounter should not contain one, or the lane floor for Reachers is 7.
- **Fold (11):** natural phrases are 2–3 tiles; the five-slot ceiling was never
  reached and never missed.
- **Approach (15):** the full five banked during the walk-in for 1 hp — and the
  five-clause spend **mostly whiffed**, because the board's period-3 cycles
  moved under a phrase written five turns earlier. §1.1a called that "better
  drama"; it is, and it is also a hard lesson delivered exactly once per
  player. Five slots are interesting at 13–15 only, as designed.

### 2.4 Free facing and the combo economy — cannot be answered from shipped content

Played honestly: **facing never mattered** — every enemy in every shipped
encounter opens to the hero's right and re-faces free, so Turn was never worth
a turn and flanking never arose. **ComboLanded never fired** — no shipped
encounter can realistically lose two bodies to one phrase (Reacher 4 + Bulwark
5; the 1-hp Explosive Warden appears in no encounter). Both questions need an
encounter that ships a body behind the hero and a Warden/Wisp pair; until then
they are dormant content, not open questions play can settle.

## 3. Two defects this pass's own instruments caught in its own work

- **The ghost strokes were eaten by their own dryness.** First authoring gave
  spent shadow strokes dryness 0.30; the countability guard's first run printed
  "WISP at 0/3 … a player would count 2". Fixed by the charge run's own rule
  (ghosts differ by value and hue, never by breakup) — the guard's first red
  was a real defect, which is the strongest thing a guard can do.
- **The live loop's opening frame was the wrong shot.** With no camera keys,
  `Schedule.framingAt` falls back to the lane's framing: `play-fold` opened at
  12.5 tiles against every graded planning shot's 6.5, measured live
  (`intimacy (0, 12.5)`). Fixed by `Scheduler.openOnPlan()` — one drift key at
  t=0, which also satisfies "never perfectly still".

## 4. What this pass did not do, with numbers where they exist

- **No refusal feedback.** A refused command does nothing on screen — twice in
  one played fight I could not tell *why* a tile would not bank. The first was
  charges (Thrust at 0/3, confirmed once the probe carried engine truth); the
  second could not be diagnosed from the interface at all, which is the finding
  itself. The probe now carries engine truth (`turn`,
  `tile<i>` charges) so a driven session can see it; a **keyboard player
  cannot**. The fix is a drawing (a nib impression that fails to take ink) and
  it is the input loop's largest legibility debt.
- **Reorder is not bound.** The engine supports free reorder; the keyboard
  binds only LIFO un-bank (backspace), which is the queue's grammar. Unbound
  deliberately — reorder needs a selection affordance §8 makes hard, and
  nothing forced it this pass.
- **A body can stand on its own count.** The rows are drawn under the figures
  (as all interface is); a Shadow's own hair can cross its row at some walks.
  At the graded planning window it does not (the §1 table proves all 9 strokes),
  but no guard covers every walk frame. Same class as the erased-charge defect
  the hand column paid in pass 3; the countability guard covers geometry, not
  occlusion.
- **The margin guard enumerates the three shipped openings, not every
  reachable board.** A body walked to the extreme visible tile of a wide
  framing can carry its row into a margin band (KNIFE's Bulwark already enters
  the foxing by 0.031 fh while staying 0.019 fh clear of the glyphs — the
  guard's own red print). An edge taper was considered and rejected: it blanks
  a cornered body's count exactly when it is needed.
- **`play-*-rest` windows and `s5-input-victory-rest` carry no bare siblings**
  and therefore no numeric claims; they are pictures of the epilogue only.
- **The epilogue is minimal by intent.** Interface dries over 2.8 s after a
  0.9 s hold; the world stays. BP4's fuller staging (the terrain keeping a
  permanent stain, the star-blade's ink dispersing on defeat) is design work
  this pass did not attempt. Note played: the dissolve caps at bias 0.95, so a
  faint remnant of a dead body stays on the page — unplanned, and it reads as
  the stain BP4 asked for; a reviewer should say whether it survives.
- **One hand-driven session diverged from its own replay** (an extra Reacher
  exchange appeared that no scripted rerun — headless ×2 cadences, HTTP ×1 —
  reproduces; all scripted runs are identical to the beat). Suspected operator
  double-fire over HTTP. The probe now prints engine turn so a future session
  catches it in the act. Recorded because a claim of full live determinism
  would be stronger than the evidence.
- **Nobody with hands has played this.** Every feel claim above is an agent
  driving HTTP at chosen cadences; the keyboard path is code-identical
  (`PlayScene.command`) but untouched by human fingers. F5's playtest report
  (owner's audit) is still owed, and only it can grade the tempo choice.

## 5. Reproduce

```
./gw capture -Pscene=play-victory      -Pout=out/captures/s5-input-victory        -Pframes=12 -Pcols=4 -Pstart=0.5   -Pstep=1.6    -Pw=960 -Ph=720
./gw capture -Pscene=play-victory-bare -Pout=out/captures/s5-input-victory-bare   (identical window)
./gw capture -Pscene=play-victory      -Pout=out/captures/s5-input-victory-blow   -Pframes=36 -Pcols=6 -Pstart=13.55 -Pstep=0.0167 -Pw=960 -Ph=720
./gw capture -Pscene=play-victory-bare -Pout=out/captures/s5-input-victory-blow-bare (identical window)
./gw capture -Pscene=play-victory      -Pout=out/captures/s5-input-victory-rest   -Pframes=8  -Pcols=4 -Pstart=14.4  -Pstep=0.7    -Pw=960 -Ph=720
./gw capture -Pscene=play-defeat       -Pout=out/captures/s5-input-defeat         -Pframes=10 -Pcols=5 -Pstart=0.4   -Pstep=1.55   -Pw=960 -Ph=720
./gw capture -Pscene=play-defeat-bare  -Pout=out/captures/s5-input-defeat-bare    (identical window)
./gw capture -Pscene=play-defeat       -Pout=out/captures/s5-input-defeat-blow    -Pframes=36 -Pcols=6 -Pstart=9.05  -Pstep=0.0167 -Pw=960 -Ph=720
./gw capture -Pscene=play-defeat-bare  -Pout=out/captures/s5-input-defeat-blow-bare (identical window)
./gw capture -Pscene=play-defeat       -Pout=out/captures/s5-input-defeat-rest    -Pframes=8  -Pcols=4 -Pstart=9.6   -Pstep=0.8    -Pw=960 -Ph=720
./gw capture -Pscene=lane-fold         -Pout=out/captures/s5-input-fold-plan      -Pframes=6  -Pcols=3 -Pstart=0.4   -Pstep=0.5    -Pw=960 -Ph=720
./gw capture -Pscene=lane-fold-bare    -Pout=out/captures/s5-input-fold-plan-bare (identical window)
./gw capture -Pscene=lane-fold         -Pout=out/captures/s5-input-fold-plan-540  -Pframes=6  -Pcols=3 -Pstart=0.4   -Pstep=0.5    -Pw=720 -Ph=540
./gw capture -Pscene=lane-fold-bare    -Pout=out/captures/s5-input-fold-plan-540-bare (identical window)

./gw test --rerun-tasks
node tools/check-progress.mjs
```

To play: `./gw run --args="play-fold"` (digits bank, backspace un-banks, enter
commits, T turns, H holds, space fast-forwards, F brisk), or over HTTP:
`./gw debugServer -Pport=7671 -Pscene=play-fold` then `POST /event
{"name":"add","args":{"index":"3"}}` etc.

## 6. Every new guard, and how it was observed red

§11.2b(f) in all four clauses. "Broken by" was actually run, red actually seen.

| guard | broken by | note |
|---|---|---|
| `everyShadowsStrokeIsCountableAtEveryShippedResolution` | pitch 0.8 widths → "would count 1" of 5; **and red on first honest run** (ghost dryness ate a stroke) | enumerates heights × archetypes × hp × framings; message prints its own parameters |
| `theRowDriesWithTheBodyItCounts` | dying term negated (`1+dying`) | |
| `theRowNeverSpendsVermillion` | live strokes drawn in `VERMILLION` | |
| `theGradedOpeningsKeepEveryRowClearOfEveryCountedMark` | asserted against the foxing band: KNIFE Bulwark red at 1.139 vs 1.108 | scope stated: three shipped openings, off-frame bodies skipped by name |
| `aFightCanBeLostThroughTheInputLoop` | defeat spec at heroHp 999 → ONGOING | also asserts the hero's DISSOLVE staging |
| `aFightCanBeWonThroughTheInputLoop` | duellist's execute branch disabled → clock ran out | also asserts the row dries mid-dissolve and is gone after |
| `theStrokeDriesAtTheSecondTheBladeLands` | +0.5 s drift in `Readout.hpAt` | value asserted equal to the engine's own `hpAfter` |
| `unbankingIsLifoAndNoMarkEverMoves` | erase taken from the bottom of the column | |
| `commandsAreRefusedWhileTheScoreResolves` | quiet-gate clause deleted | |
| `noBeatIsStagedBeforeTheCommandThatCausedIt` | `notBefore` call deleted | |
| `rescoringMidPerformanceMovesNoBody` | clock copy deleted from `rescore` | the pelvis-carry copy could NOT be turned red (state round-trips through `standX`); recorded in the test so nobody mistakes that line for load-bearing |
| `aPlayedFightIsReproducible` | per-command random jitter on the beat floor | a `static final` jitter did *not* go red (one JVM, one value) — the second break attempt is the one that counts |
