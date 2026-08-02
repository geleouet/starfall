package dev.starfall.combat;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * The five Charted Shadows of the first encounter (combat-design.md 2.4).
 *
 * <p>Each is defined by how it <em>forces contact</em>, not by a stat line. Read
 * the table that way: the Wisp is the baseline the others are read against, the
 * Reacher makes closing cost something, the Runner collapses distance in one
 * gesture, the Warden makes you choose where it dies, and the Bulwark takes your
 * verb away.
 *
 * <p>Damage figures are placeholders. Reach is not -- a Reacher with reach 1 is a
 * Wisp, and the whole point of the set is five different distances at which two
 * bodies touch.
 */
public enum EnemyArchetype {

    // On Quick, which deliberately has no base archetype here.
    //
    // Quick removes the advance telegraph -- its mechanic is the *absence* of a
    // strikethrough. All five bodies below are the fixtures that demonstrate telegraph
    // behaviour, so giving Quick to any of them deletes the demonstration: put it on the
    // Runner and no non-Quick Charger is left to show destination recomputation; put it on
    // the Reacher and three telegraph tests lose their subject. Both were tried and both
    // broke the suite.
    //
    // It is a *variant* trait, applied on top via a placement override, which is also what
    // the source material does with it. docs/combat-design.md once claimed the Runner
    // carried it; that claim was written into the design document and never into the code,
    // and the code was right.

    /** The baseline. Three hit points, one tile of reach, nothing else. */
    WISP("Wisp", 3, 1, 1),

    /** Two tiles of reach, so walking up to it is already inside the threat. */
    REACHER("Reacher", 4, 2, 1),

    /** Charger. Crosses the whole open lane in one move -- the extreme-motion case. */
    RUNNER("Runner", 4, 1, 2, Trait.CHARGER),

    /** Explosive, one hit point. You do not choose whether it dies, only where. */
    WARDEN("Warden", 1, 1, 1, Trait.EXPLOSIVE),

    /** Unyielding and Aggressive. Refuses your verb and then walks into you. */
    BULWARK("Bulwark", 5, 1, 2, Trait.UNYIELDING, Trait.AGGRESSIVE);

    /** Damage an Explosive body throws onto each neighbouring tile as it goes. */
    public static final int BLOOM_DAMAGE = 2;

    private final String displayName;
    private final int hp;
    private final int reach;
    private final int damage;
    private final Set<Trait> traits;

    EnemyArchetype(String displayName, int hp, int reach, int damage, Trait... traits) {
        this.displayName = displayName;
        this.hp = hp;
        this.reach = reach;
        this.damage = damage;
        EnumSet<Trait> set = EnumSet.noneOf(Trait.class);
        Collections.addAll(set, traits);
        this.traits = Collections.unmodifiableSet(set);
    }

    public String displayName() {
        return displayName;
    }

    public int hp() {
        return hp;
    }

    /** How many tiles ahead of itself it threatens. */
    public int reach() {
        return reach;
    }

    public int damage() {
        return damage;
    }

    public Set<Trait> traits() {
        return traits;
    }

    public boolean has(Trait t) {
        return traits.contains(t);
    }
}
