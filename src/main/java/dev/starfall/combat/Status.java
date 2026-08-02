package dev.starfall.combat;

/** The four statuses of combat-design.md 1.4. */
public enum Status {
    /** 1 damage per turn for 3 turns. Ink bleeding outward from the wound. */
    SEEPING,
    /** Immobilised 3 turns. The figure's pigment dries. */
    STILLNESS,
    /** The next hit taken deals double. A vermillion seal on the body. */
    MARKED,
    /** Negates the next attack. A held brushstroke standing between body and blade. */
    GUARD
}
