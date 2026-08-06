package com.starfall.game;

/**
 * Le héros joueur — archétype « Vagabonde » : il ne frappe pas fort, il se replace.
 *
 * <p>Le héros ne connaît pas sa position : elle appartient à la {@link Grid}. Il ne porte que ce qui
 * est vraiment à lui, c'est-à-dire son orientation.
 */
public final class Hero implements Occupant {

    /**
     * Points de vie de départ.
     *
     * <p>Cinq, c'est-à-dire assez pour se tromper deux ou trois fois et assez peu pour que chaque
     * coup encaissé se sente. Le chiffre sera réaccordé au jalon d'équilibrage.
     */
    public static final int MAX_HEALTH = 8;

    private final long id = Identities.next();

    private int health = MAX_HEALTH;
    private Direction facing = Direction.RIGHT;

    @Override
    public long id() {
        return id;
    }

    /** Points de vie restants. À zéro, la partie est perdue. */
    public int health() {
        return health;
    }

    /** Rend des points de vie, sans dépasser le maximum. */
    void heal(int amount) {
        health = Math.min(MAX_HEALTH, health + amount);
    }

    /** Retire des points de vie. Renvoie vrai si le héros vient de tomber. */
    boolean damage(int amount) {
        health = Math.max(0, health - amount);
        return health == 0;
    }

    @Override
    public String spriteName() {
        return "hero/idle";
    }

    @Override
    public String label() {
        return "la Vagabonde";
    }

    /** Direction vers laquelle le héros regarde. Détermine sa portée et la cible de sa capacité. */
    public Direction facing() {
        return facing;
    }

    /** Change l'orientation. Réservé au paquet : seules les règles décident du sens du regard. */
    void face(Direction direction) {
        this.facing = direction;
    }

    @Override
    public String toString() {
        return "Hero{" + facing.label() + "}";
    }
}
