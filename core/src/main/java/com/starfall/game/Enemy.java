package com.starfall.game;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Un ennemi : un {@link EnemyKind archétype}, des {@link Trait traits}, une orientation, et
 * l'{@link Intention} qu'il a annoncée.
 *
 * <p>Comme le héros, il ne connaît pas sa position : elle appartient à la {@link Grid}.
 */
public final class Enemy implements Occupant {

    private final EnemyKind kind;
    private final Set<Trait> traits;

    private Direction facing = Direction.LEFT;
    private Intention intention = Intention.of(Intention.Kind.WAIT);
    /** Vrai quand un lancier a pris son élan et chargera à sa prochaine activation. */
    private boolean windingUp;

    public Enemy(EnemyKind kind, Trait... traits) {
        this.kind = kind;
        this.traits = traits.length == 0
                ? Collections.emptySet()
                : Collections.unmodifiableSet(EnumSet.of(traits[0], traits));
    }

    public EnemyKind kind() {
        return kind;
    }

    public Set<Trait> traits() {
        return traits;
    }

    public boolean has(Trait trait) {
        return traits.contains(trait);
    }

    public Direction facing() {
        return facing;
    }

    void face(Direction direction) {
        this.facing = direction;
    }

    /** Ce que cet ennemi fera à sa prochaine activation. Visible par le joueur. */
    public Intention intention() {
        return intention;
    }

    void announce(Intention intention) {
        this.intention = intention;
    }

    boolean isWindingUp() {
        return windingUp;
    }

    void setWindingUp(boolean windingUp) {
        this.windingUp = windingUp;
    }

    /** Nombre de coups portés par une frappe. Un ennemi rapide en porte deux. */
    int strikesPerAttack() {
        return has(Trait.RAPIDE) ? 2 : 1;
    }

    @Override
    public String spriteName() {
        return kind.spriteName();
    }

    @Override
    public String label() {
        if (traits.isEmpty()) {
            return kind.label();
        }
        StringBuilder builder = new StringBuilder(kind.label());
        for (Trait trait : traits) {
            builder.append(' ').append(trait.label());
        }
        return builder.toString();
    }

    @Override
    public String toString() {
        return "Enemy{" + label() + ", " + intention + "}";
    }
}
