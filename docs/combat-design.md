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

**The Ronin — verb: *push*.**
Advancing into an occupied tile shoves the occupant back one tile. If there is no room
behind them, both take collision damage.
*Choreography:* a shoulder-and-hilt check. Two bodies meeting, one giving ground, cloth
compressing between them. Contact is sustained, not instantaneous.

**The Wanderer — verb: *swap*.**
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

- **Aggressive** — closes after attacking (others give ground).
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
| **Ashigaru** | 3 | — | Nothing. The baseline the others are read against |
| **Lancer** | 4 | — | Respect 2 tiles of reach, so closing is not free |
| **Runner** | 4 | Charger | React to sudden distance collapse — the extreme-motion test case |
| **Warden** | 1 | Explosive | Choose *where* it dies, not just whether |
| **Bulwark** | 5 | Unyielding, Aggressive | Solve a problem without your movement verb |

The Bulwark exists specifically to deny the hero's signature verb, because a mechanic is
only interesting once something refuses it.

---

## 3. UI

STYLE.md §8 governs, and it is restrictive by design: no chrome, no panels, no bars, no
borders. Specifically here:

- The **queue** is three ink cartouches. Because execution is LIFO, the reading order must
  make that legible without a tutorial — the stack reads top-down as "what happens next".
- **Cooldowns** are strokes that dry out and refill, not numeric counters.
- **Health** is a column of ink strokes that dry and fade.
- **Intent telegraphs** are vermillion washes over threatened tiles, arriving by bleed.
- The **lane** is a row of faint wash marks that only intensify near relevant tiles.
- Everything sits on the aged-paper substrate of STYLE.md §3b.3, so the interface is marks
  on the same sheet the figures are painted on rather than a layer above it.

---

## 4. Explicitly out of scope for now

Regions, branching paths, shops, skills, consumables, days and NG+, elite variants,
corrupted variants, boss roster, and the 42-tile catalogue. All are additive and none of
them are needed to prove the thing this project is actually about.
