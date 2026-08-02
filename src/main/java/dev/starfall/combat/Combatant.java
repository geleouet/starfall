package dev.starfall.combat;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * One body on the Fold of the World -- the Night Pilgrim or a Charted Shadow.
 *
 * <p>Deliberately one class for both sides. Everything the engine does to a body
 * it can do to either: both can be shoved, marked, guarded, killed and turned,
 * and the moment heroes and enemies are separate types those rules start
 * acquiring "for enemies only" clauses that will be wrong the first time a tile
 * pulls a hero.
 *
 * <p>Mutators are package-private: the engine owns transitions, and the UI and
 * the animation layer read. Identity is the {@code id}, which is stable for the
 * life of the encounter and is what {@link CombatEvent} refers to bodies by.
 */
public final class Combatant {

    private final int id;
    private final String name;
    private final Hero hero;
    private final EnemyArchetype archetype;
    private final int maxHp;
    private final int reach;
    private final int damage;
    private final Set<Trait> traits;
    private final Statuses statuses;

    private int tile;
    private Facing facing;
    private int hp;
    private boolean alive = true;
    private Intent intent;
    private boolean owesStep;

    private Combatant(int id, String name, Hero hero, EnemyArchetype archetype, int maxHp, int reach, int damage,
                      Set<Trait> traits, int tile, Facing facing) {
        this.id = id;
        this.name = name;
        this.hero = hero;
        this.archetype = archetype;
        this.maxHp = maxHp;
        this.hp = maxHp;
        this.reach = reach;
        this.damage = damage;
        this.traits = traits;
        this.statuses = new Statuses();
        this.tile = tile;
        this.facing = facing;
    }

    static Combatant hero(int id, Hero kind, int hp, int tile, Facing facing) {
        return new Combatant(id, kind.name(), kind, null, hp, 1, 0,
                Collections.unmodifiableSet(EnumSet.noneOf(Trait.class)), tile, facing);
    }

    static Combatant enemy(int id, EnemyArchetype archetype, int tile, Facing facing, Set<Trait> extraTraits) {
        EnumSet<Trait> t = EnumSet.noneOf(Trait.class);
        t.addAll(archetype.traits());
        t.addAll(extraTraits);
        return new Combatant(id, archetype.displayName(), null, archetype, archetype.hp(), archetype.reach(),
                archetype.damage(), Collections.unmodifiableSet(t), tile, facing);
    }

    public int id() {
        return id;
    }

    public String name() {
        return name;
    }

    public boolean isHero() {
        return hero != null;
    }

    /** The hero kind, or {@code null} for a Charted Shadow. */
    public Hero hero() {
        return hero;
    }

    /** The archetype, or {@code null} for the hero. */
    public EnemyArchetype archetype() {
        return archetype;
    }

    public int tile() {
        return tile;
    }

    public Facing facing() {
        return facing;
    }

    public int hp() {
        return hp;
    }

    public int maxHp() {
        return maxHp;
    }

    /** How many tiles ahead this body threatens. */
    public int reach() {
        return reach;
    }

    public int damage() {
        return damage;
    }

    public boolean alive() {
        return alive;
    }

    public Set<Trait> traits() {
        return traits;
    }

    public boolean hasTrait(Trait t) {
        return traits.contains(t);
    }

    public Statuses statuses() {
        return statuses;
    }

    /** The declared Strikethrough, or {@code null} when nothing is telegraphed. */
    public Intent intent() {
        return intent;
    }

    /**
     * True between resolving a blade and declaring the footwork that follows it.
     *
     * <p>The engine's memory that this body has just struck and has not yet
     * stepped. It is set as the blade lands and spent by the next declaration,
     * which turns it into a {@link Intent.Kind#WITHDRAW} or a
     * {@link Intent.Kind#CLOSE_IN}. Keeping the step out of the strike is what
     * gives the player a turn to answer the body where it struck from -- see
     * {@code CombatEngine.enemyAttack} and combat-design.md 3d.1.
     */
    public boolean owesStep() {
        return owesStep;
    }

    void tile(int t) {
        this.tile = t;
    }

    void facing(Facing f) {
        this.facing = f;
    }

    void hp(int v) {
        this.hp = v;
    }

    void kill() {
        this.alive = false;
        this.hp = 0;
        this.intent = null;
        this.owesStep = false;
    }

    void intent(Intent i) {
        this.intent = i;
    }

    void owesStep(boolean v) {
        this.owesStep = v;
    }

    Combatant copy() {
        Combatant c = new Combatant(id, name, hero, archetype, maxHp, reach, damage, traits, tile, facing);
        c.hp = hp;
        c.alive = alive;
        c.intent = intent;
        c.owesStep = owesStep;
        c.statuses.copyFrom(statuses);
        return c;
    }

    String fingerprint() {
        return id + ":" + name + "@" + tile + facing.step() + " hp=" + hp + (alive ? "" : "!")
                + (owesStep ? " owes-step" : "") + " [" + statuses + "] " + intent;
    }

    @Override
    public String toString() {
        return name + "#" + id + "@" + tile;
    }
}
