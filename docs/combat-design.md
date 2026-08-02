# Combat design — Starfall

Scope decision: **one excellent encounter first.** A single fight on the lane,
complete — LIFO action queue, cooldowns, intent telegraphs, enemy traits, statuses,
combos. No regions, no shops, no skills, no days, no NG+. The roguelike structure layers
on top later without breaking anything.

Source: `shogun-details.md` is a retro-spec of *Shogun Showdown* (Roboatino, 2024). **We
borrow its systems and write our own content.** Its mechanics are design ideas worth
learning from; its 42 named tiles, 14 named bosses and 13 named regions are someone
else's game, and much of that content was designed for a rendering style that is not ours.

---

## 0. The filter — the only rule that decides what goes in

> **A mechanic earns its place if it produces a beat of choreography.**

The brief's stated top priority is procedural animation of *interactions between
characters* — parries, weapon contacts, impact reactions. So mechanics that make two
bodies touch are the point of this game, and abstract stat modifiers are overhead.

| Earns its place | Does not |
|---|---|
| Parry, deflection, blade bind | Flat damage modifiers |
| Push, pull, throw, swap | Pure cooldown arithmetic |
| Passing through an occupant | Ranged chip damage with no contact |
| Being carried backward by a blow | Numeric aura buffs |
| Guarding another body | Off-screen effects |

This is also why the *hero identity* insight from the source material is the best thing in
it: each hero's signature is a **movement verb** — swap, push, pass through, throw
backward, rotate front-and-back. Every one of those is a two-body contact event. That is
free animation design.

---

## 1. Borrowed systems, stated precisely

These are taken as-is because they are good and they serve the filter.

### 1.1 The action queue

- Up to **5 tiles** in the queue. The source uses 3; we deliberately go longer, for the
  reasons in 1.1a below.
- **Adding a tile costs a turn.** This is the whole tension: every tile you bank is a turn
  you spend exposed to telegraphed attacks.
- **Reordering and removing are free.** The queue can be debugged in place as the board
  changes.
- Execution is **all-or-nothing**: once triggered the whole sequence resolves without
  interruption.
- **Execution is LIFO** — last tile added resolves first.

**Why LIFO matters here more than it did in the source.** Building a queue is writing a
choreography backwards: the last intention becomes the first gesture. And an uninterrupted
multi-tile sequence is a *phrase*, not a hit — which is exactly the multi-beat material
STYLE.md §7 needs. A single strike gives the animation system one impact to make poetic; a
five-tile execution gives it a sentence with clauses.

### 1.1a Why the queue is 5 rather than 3

**The animation argument.** Queue length *is* phrase length. This project's whole reason to
exist is procedural interaction choreography, and a longer queue is directly more of it —
five linked beats resolving without interruption, each one flowing out of the last, is a
qualitatively different thing to animate than three. It is the cheapest possible way to buy
the animation system more material.

**It costs nothing in tension, because the price scales with it.** Each tile still costs a
turn, so a full queue is **five turns of standing exposed** to telegraphed attacks. The
all-or-nothing rule then bites much harder: a five-tile queue spent on a board that moved
underneath you is a five-turn loss. That is better drama than a three-turn one, not worse
balance.

**And it pairs with lane length (1.6), which is the real reason for the number.** Long lanes
give you an approach — several turns of closing distance where you are not yet in danger.
That is exactly the window in which a big queue gets banked. So:

| Lane | Queue you can realistically bank | The fight that produces |
|---|---|---|
| 5-7 | 1-2 | React, strike, react. No time to compose. |
| 9-11 | 2-3 | Partial phrases; the source's feel. |
| 13-15 | 4-5 | Bank the whole phrase during the approach, then spend it in one exchange. |

**Lane length and queue size are the same tension seen from two ends**, which is why raising
one without the other would have been wrong. On a short lane the five slots are simply
unreachable, and that is the point — the board tells you how long a sentence you are allowed
to write.

Two consequences to watch when tuning:
- **Combo rewards need retuning.** A five-tile execution can clear far more than a
  three-tile one, so combo-driven economy scales faster than the source's.
- **The UI has to carry five cartouches with unmistakable LIFO ordering** and no chrome
  (STYLE.md §8). Three was already the hard part of that problem; five makes reading order
  the primary UI design constraint rather than a detail.

Per-hero queue size is a natural later dial — a heavier hero with fewer, larger slots
against a fleeter one with more — but is deliberately not used yet, because the two heroes
already differ by their movement verb and one variable at a time is enough.

### 1.2 Cooldowns

- Each tile carries **charges, 0 to 8**, recovering 1 per turn.
- **Using a tile resets its cooldown whether it hits or not.** Missing is punished twice,
  which pushes toward precision rather than spam.

### 1.3 Turn structure and facing

- Player acts; then all enemies resolve in board order.
- **Facing is a resource.** The hero faces left or right, most tiles act in the facing
  direction, and turning costs a turn unless a tile grants it free.
- Enemies **telegraph intent one turn ahead**. Per STYLE.md §8 this is the one place
  vermillion is spent freely — a thin wash over threatened tiles, arriving by wet bleed
  rather than by fade.

### 1.4 Statuses

Renamed to fit the world; mechanics unchanged.

| Name | Effect | Ink reading |
|---|---|---|
| **Seeping** | 1 damage per turn for 3 turns | Ink bleeds outward from the wound and keeps spreading |
| **Stillness** | Immobilised 3 turns | The figure's pigment dries; motion damping raised hard |
| **Marked** | Next hit taken deals double | A vermillion seal blooms on the body |
| **Guard** | Negates the next attack | A held brushstroke standing between body and blade |

### 1.5 Combos and enchantments

- Killing more than one enemy in a single execution is a **combo**, and combos are the
  economy's engine.
- A tile may carry **one** enchantment: shockwave, perfect strike, seeping, stillness,
  double strike, marking, or **free-play** (adding it costs no turn, at a cooldown
  penalty).

### 1.6 Lane length is a design parameter, not a constant

**The lane runs from 5 to 15 tiles**, chosen per encounter. The source varies 5/7/9 for the
same reason; we extend the top end.

This is the single strongest composition dial in the game, because length changes what the
fight *is*:

| Length | What the fight becomes |
|---|---|
| **5-7** | A knife fight. Everything is already in reach, so tactics are about *ordering* — what resolves first, and what you are exposed to while banking a tile. Closest to the source's balance. |
| **9-11** | Middle game. Closing distance costs turns but is not the whole problem. Ranged and mobility start to pay. |
| **13-15** | The approach. Several turns of closing before contact, retreat is genuinely viable, and ranged attacks and dashes dominate unless the enemy mix pushes forward. |

Two things follow, and both must be designed rather than tolerated:

- **Numbers do not transpose from the source**, which is balanced for the short end only.
  On a long lane a melee-only enemy can spend several turns simply walking, so waves need
  mixed approach speeds or the board reads as empty. The Charger trait exists partly to
  solve this.
- **Maximum simultaneous enemies scales with length.** A 15-tile lane with three enemies
  is a corridor; a 5-tile lane with three enemies is a crisis.

**What the long end buys** is the *approach* — the slow closing of distance that makes the
first contact land. That is exactly what the camera plan of STYLE.md §9 needs, and it means
long lanes and the push-in are the same design idea seen from two directions.

**Camera consequence.** The planning framing must fit the lane, so a 5-tile lane is already
near-intimate while a 15-tile lane is genuinely wide. The push-in is therefore a *small*
move on short lanes and a *large* one on long lanes — which is correct, because a short
lane has no approach to dramatise and a long one has nothing but. The camera should derive
its wide framing from lane length rather than using a fixed value.

---

## 2. Original content

### 2.1 Two heroes, two contrasting verbs

Chosen so the interaction layer is proved **generic** rather than fitted to one case.

**The Warden — verb: *push*.**
Advancing into an occupied tile shoves the occupant back one tile. If there is no room
behind them, both take collision damage.
*Choreography:* a shoulder-and-hilt check. Two bodies meeting, one giving ground, cloth
compressing between them. Contact is sustained, not instantaneous.

**The Pilgrim — verb: *swap*.**
Advancing into an occupied tile exchanges places with the occupant.
*Choreography:* two figures passing through each other within one beat — sleeves and hair
crossing, each trailing into the space the other has left. This is the harder animation
problem of the two and the better test of the system.

The two verbs are deliberately opposite in what they demand: push is a *collision* with
resistance and recoil; swap is an *interpenetration* with no impact at all. If both read
as poetic, the interaction layer is genuinely general.

### 2.2 Tiles

Small, and every one is a contact event. Numbers are placeholders to be tuned on a
lane whose length varies from 5 to 15, not inherited.

| Tile | Effect | Choreography beat |
|---|---|---|
| **Cut** | 1 damage, adjacent, facing | The baseline stroke |
| **Thrust** | Pierces 2 tiles ahead | Blade passing through a body, not stopping at it |
| **Parry** | Reactive: if attacked this turn, deflect and counter | **The signature beat.** Blade-on-blade, a deflection curve rather than a collision, per STYLE.md §7.2 |
| **Sweep** | Hits front and behind | One continuous arc through two bodies |
| **Draw** | Pulls the target one tile toward you | Contact at distance — a line of force between two figures |
| **Step / Back-step** | Move one tile | Weight transfer |
| **Turn** | Reverse facing | The whole body winding around; cloth and hair last to arrive |
| **Feint** | Free-play reposition, cooldown penalty | Motion with no contact — the negative space that makes contact read |

### 2.3 Enemy traits

Taken from the source; they are compact and they generate distinct silhouettes of
behaviour.

- **Aggressive** — closes after attacking (others give ground). Either way the step is
  declared and walked on the *following* turn, never in the same instant as the blade — §3d.1.
- **Quick** — attacks as soon as a tile enters its queue, without waiting a turn.
- **Explosive** — bursts on death, damaging adjacent tiles. In our idiom, a bloom of ink
  thrown across the neighbours.
- **Charger** — always moves the maximum distance available.
- **Unyielding** — cannot be pushed, pulled or turned. The direct counter to both heroes'
  verbs, and therefore the most interesting enemy in the set.

### 2.4 Enemies for the first encounter

Five is enough to make one fight tactical. Each is defined by how it *forces contact*.

| Enemy | HP | Trait | Forces you to |
|---|---|---|---|
| **Wisp** | 3 | — | Nothing. The baseline the others are read against |
| **Reacher** | 4 | — | Respect 2 tiles of reach, so closing is not free |
| **Runner** | 4 | Charger | React to sudden distance collapse — the extreme-motion test case. Quick is a variant trait, see §3d.2 |
| **Warden** | 1 | Explosive | Choose *where* it dies, not just whether |
| **Bulwark** | 5 | Unyielding, Aggressive | Solve a problem without your movement verb |

The Bulwark exists specifically to deny the hero's signature verb, because a mechanic is
only interesting once something refuses it.

---

## 3. UI

STYLE.md §8 governs, and it is restrictive by design: no chrome, no panels, no bars, no
borders. Specifically here:

- The **queue is a vertical column**, not a horizontal bar, and new tiles **enter at the
  top**. This is the single most important UI decision in the game and it is settled.

  Vertical is the only orientation that does not fight LIFO. A horizontal bar read
  left-to-right implies the leftmost tile resolves first, which is the opposite of what
  happens, so it forces the rule to be *explained* and re-explained. A column reads top-down
  as "what happens next", which is the natural metaphor for a stack. And inserting at the top
  makes the rule disappear entirely: what you wrote last is at the top and goes first, so the
  player never learns "LIFO", they just read downward. Appending at the bottom would put the
  first-to-resolve tile at the bottom and reintroduce the whole problem.

  Three supporting reasons. The lane is horizontal and long, so a horizontal bar sits
  *parallel* to it — two horizontal rows of similarly sized elements read as one confusable
  thing, whereas a column is orthogonal to the board and can never be mistaken for it. The
  camera glides horizontally toward the exchange on execution (§9), and a side column stays
  out of that path. And it is thematically correct: `STORY.md` names the queue the **Ink
  Stanza**, and a stanza is read downward on a hanging scroll — a horizontal hand of cards
  reads as a deckbuilder hotbar, which is precisely the generic UI STYLE.md §8 forbids.
- **Cooldowns** are strokes that dry out and refill, not numeric counters.
- **Health** is a column of ink strokes that dry and fade.
- **Intent telegraphs** are vermillion washes over threatened tiles, arriving by bleed.
- The **lane** is a row of faint wash marks that only intensify near relevant tiles.
- Everything sits on the aged-paper substrate of STYLE.md §3b.3, so the interface is marks
  on the same sheet the figures are painted on rather than a layer above it.

---

## 3b. Setting and vocabulary

`STORY.md` sets the world: *The Atlas of Extinguished Dreams*. The player is the **Night
Pilgrim**; enemies are **Charted Shadows**; the lane is the **Fold of the World**; the
action queue is the **Ink Stanza**; an intent telegraph is a **Strikethrough**.

**This is a re-skin, not a redesign.** `STORY.md` §2 retires the samurai framing because it
anchored the world to one specific culture, but it keeps the entire visual language — warm
paper, edge dissolve, the blade as the only hard edge, dream motes, impact as an ink bloom
rather than a shockwave. So nothing about the geometry, the mechanics or the rubric changes.
What changes is naming and fittings:

| Was | Is |
|---|---|
| tsuba (guard) | star-guard |
| tsuka (grip) | grip |
| daisho / saya | traveller's scabbard, second sheathed blade |
| obi | sash |
| haori | outer robe, mantle |
| hakama | split robe |
| katana | star-blade |
| hamon | temper line |

The reference corpus stays ground truth for **material, value, atmosphere and motion**, and
stops being ground truth for **iconography** — see STYLE.md's preamble and §11.0. The
matched-scale test counts *readable parts*, not Japanese garment parts: a guard is a guard
whether it is a tsuba or a star-forged crossbar; what matters is that the eye finds a mark
there.

The two heroes keep their verbs and are renamed to fit: **the Warden** pushes, **the
Pilgrim** swaps.

---

## 3c. On the generated maquettes

`tmp/maquettes/` holds AI-generated mockups. **They are loose inspiration and are explicitly
not faithful to the target render.** They are not composition targets, not UI targets, and
not material ground truth. The eight paintings in `inspirations/` remain the only ground
truth, per STYLE.md's preamble.

One observation from them is worth keeping, because it arrived independently: their hair is
drawn as a **solid black mass with one-to-two-pixel wisps escaping it, and nothing in
between** — which is exactly the bimodal structure the System 3 review measured in the
references and made the single named cause of that pass's failure. Two unrelated routes to
the same finding.

---

## 3d. Corrections found by building the engine

The rules engine was implemented headless with 96 tests before any UI existed. That
surfaced things reading the design could not.

### 3d.1 Melee could never land — the retreat was glued to the strike

**The symptom, found as a non-terminating test loop.** A hero alternating bank / execute
could **never** land a melee hit on a non-Aggressive enemy. Not "often missed" — never
connected, deterministically, forever. Twenty of forty-five starting configurations were
infinite.

**The first diagnosis was wrong, and the first fix was never implemented.** It was recorded
here as "enemies give ground only when wounded". That resolution was written into this
document and never into the engine, so the defect stayed live the whole time while the
document said it was fixed. Two lessons, both now standing rules: **a resolution written
into a design document is not an implementation**, and a claim in a doc that no test
enforces will drift from the code silently.

**The real cause is a parity lock.** The retreat was executed *in the same step as the
strike*, which made the enemy's cycle exactly two beats:

```
[advance, declaring ATTACK] → [strike + retreat, declaring ADVANCE] → …
```

The hero's cadence is also even — banking *n* tiles then executing is period *n+1*, and any
bank/execute rhythm lands on the same parity. Two cycles of the same parity **phase-lock**.
No amount of tuning damage or hit points can unstick that; only a change of *period* can.

**The fix: unglue the retreat from the strike.** The body strikes, *stays*, and withdraws on
the following step, with the withdraw declared like any other intent:

```
[advance, declaring ATTACK] → [strike, declaring WITHDRAW] → [withdraw, declaring ADVANCE] → …
```

Measured afterwards: first contact is bounded by **distance + 4** turns, worst case 11, from
every starting phase. No configuration is ever "never".

**The load-bearing property is the contact window, not the period.** Unglued, the body
stands in contact across **two consecutive hero turns**. A period-2 cadence cannot miss a
window of two whatever its phase — that is a guarantee. "Period 3 and period 2 are coprime"
is a coincidence of this particular pair, and reasoning from it invites a false
generalisation: a *three*-beat hero cadence (bank, bank, execute) still locks against a
three-beat enemy on one residue class in three, measured at no contact in 60 turns.

**And that residual lock is correct, because the player chose it.** One `hold` re-phases and
the same cadence connects on turn 4. `inspiration.md` names tactical patience as a dominant
strategy; this is what makes it mechanically real. The fix does not abolish phase-locking —
it moves it from something the rules do to the player into something the player does to
themselves and can undo.

**The invariant, in the form that actually bites.** The obvious wording — "every threatening
enemy must spend at least one complete hero turn in the position it threatens from" — was
*already satisfied by the broken engine*: the body arrived, the hero got a turn with it
standing there, and only then did it strike and vanish. The version that fails on the old
code and passes on the new is:

> **A body that resolves a telegraphed attack must still be standing where it struck when
> the hero's next turn begins.**

A telegraph is a promise that the player can act on the information. If the body leaves the
position it threatened from within the step in which it resolves, the telegraph announced a
threat but not a target. This is swept over every archetype, every hero cadence and both
hands.

**Aggressive bodies keep closing in**, and the close-in became a declared step for the same
reason — it is the same lie told forward instead of backward. But only where there is a tile
to close into: at reach 1 that tile is the one the hero occupies, so the Bulwark's cadence
is unchanged and it still strikes every single turn. The honest cost is that an Aggressive
body which whiffs against a retreating hero now falls one turn behind instead of keeping
pace.

### 3d.2 Quick has no base archetype, and that is correct

This section previously claimed the Runner carried Charger **and** Quick. **That claim was
written here and never into the code** — the same divergence as §3d.1, found the same way.

Trying to make the code match it failed twice, informatively. Quick removes the advance
telegraph; its mechanic is the *absence* of a strikethrough. All five base bodies are the
fixtures that **demonstrate** telegraph behaviour, so giving Quick to any of them deletes
the demonstration: on the Runner, no non-Quick Charger is left to show destination
recomputation; on the Reacher, three telegraph tests lose their subject. Both were tried and
both broke the suite.

There is also a design reason to keep it off the Charger specifically: **Quick should hide
*when*, not *where*.** The Reacher's two threatened tiles are fixed and knowable, so
withholding only the timing is frightening but fair. A body that crosses the whole lane
*and* strikes unannounced hides position and timing at once, which defeats the information
channel the entire tactical design rests on.

**Quick is a variant trait**, applied on top via a placement override, which is what the
source material does with it. It is swept through the §3d.1 invariant on all five
archetypes.

### 3d.3 Open tuning items — recorded, not resolved

Both need a playable fight to settle and neither should be guessed at now.

- **Cooldowns 0-8 recovering 1/turn are nearly inert at a 5-slot queue.** Banking a full
  stanza already costs 5 turns, so anything under about cooldown 5 is back before you want
  it, and only enchanted tiles feel the system. The scale probably wants compressing, or
  recovery slowing. This is the same problem as §1.1a's note that combo rewards need
  retuning for the longer queue, seen on the other axis.
- **Facing is a resource for the hero only.** Enemies re-face free every turn while the hero
  pays a whole turn or a Turn tile. Making an enemy spend its step to turn would give
  flanking real value and hand the Pilgrim's swap a second payoff beyond the one-turn disarm
  it already has.

### 3d.4 Rules the design left implicit, resolved in the engine

| Question | Resolution and why |
|---|---|
| Does cooldown gate banking or execution? | **Banking**, and a banked tile does not recover — otherwise a 5-turn stanza silently pre-charges itself and banking's cost leaks away. |
| "No room behind" — only the lane edge? | **Also another body**, and it does not chain: a column of enemies is a wall, not dominoes. |
| Push that kills the occupant — does the pusher follow through? | **No.** The beat is the brace, not the follow-through. |
| Does Unyielding refuse *swap*? | **Yes** — §2.4 says it denies **both** heroes their verb. It refuses external displacement and external turning, and does **not** refuse damage: the blade lands, the haul does not. |
| Is a telegraph pinned to tiles or to the body? | **To the body.** Tile-pinning would make one cooldown-0 Step a full disarm and would gut the Bulwark's refusal. Divergence is announced explicitly. |
| Does a phrase stop when the board is clear? | **No**, and every tile is spent even if the hero dies mid-phrase. All-or-nothing on the *cost*, not only the effect — which is what makes over-committing cost something. |
| Guard versus Marked ordering | **Guard first.** Marked doubles the next hit *taken*, and a negated attack was not taken; the other order silently eats a seal for free. |
| Do Guard and Marked apply to Seeping? | **No.** A raised brushstroke stands between body and blade; a wound already bleeding is behind it. |
| Do status durations stack? | **Refresh.** Stacking makes a status tile scale with queue length, which is arithmetic rather than choreography. |
| Maximum simultaneous enemies | `max(2, length/2)` — invented, since the design gave only the intent. Keeps density roughly constant across a 5-to-15 lane. |

### 3d.5 What the engine deliberately does not provide

The event stream is **ordinal**: turns and tiles, no world positions and no durations.
Mapping that to space and time is the animation layer's job and it does not exist yet. Also
missing, and all of it needed by System 4:

- **Intra-beat phases.** A strike is a single instant; a parry needs a *contact moment* to
  synchronise two skeletons to, and a wind-up / contact / recovery split to blend IK weight
  across.
- **Contact points.** A shove says two bodies met, not whether at shoulder or hilt; a blade
  meeting gives no crossing point. IK targets need both.
- **Overlap hints.** Nothing says "beat 3 may begin before beat 2 settles" — which is
  precisely what makes five beats read as one phrase rather than five.
- **Death staging.** Death is instantaneous; an ink dissolve wants a duration and a
  direction.
- **A camera focus hint.** Which tile is the subject of a beat is derivable but never stated.

---

## 4. Explicitly out of scope for now

Regions, branching paths, shops, skills, consumables, days and NG+, elite variants,
corrupted variants, boss roster, and the 42-tile catalogue. All are additive and none of
them are needed to prove the thing this project is actually about.
