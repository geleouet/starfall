package dev.starfall.combat;

/**
 * The five behavioural traits of a Charted Shadow (combat-design.md 2.3).
 *
 * <p>Each one exists because it changes the <em>silhouette of behaviour</em>, not
 * because it changes a number: an Aggressive body walks into you after it hits,
 * a Charger collapses the whole distance at once, an Explosive one throws a
 * bloom of ink across its neighbours as it goes. All four are beats.
 * {@link #UNYIELDING} is the odd one out and the most important: it is the
 * absence of a beat, the shove that moves nothing.
 */
public enum Trait {
    /** Closes one tile after attacking. Bodies without it give ground instead. */
    AGGRESSIVE,
    /**
     * Declares and resolves in the same turn, so its Strikethrough never gets a
     * turn of warning. In the source this is "attacks as soon as a tile enters its
     * queue"; here, where enemy intent is a single declared action rather than a
     * queue, the faithful translation is "no advance telegraph".
     */
    QUICK,
    /** Blooms on death, damaging both neighbouring tiles. */
    EXPLOSIVE,
    /** Always moves the maximum distance available rather than one tile. */
    CHARGER,
    /**
     * Cannot be pushed, pulled, swapped with, or turned by anything but itself.
     * The direct refusal of both heroes' movement verbs.
     */
    UNYIELDING
}
