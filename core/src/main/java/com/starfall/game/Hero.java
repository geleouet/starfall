package com.starfall.game;

/**
 * Le héros joueur — archétype « Vagabonde » : il ne frappe pas fort, il se replace.
 *
 * <p>Le héros ne connaît pas sa position : elle appartient à la {@link Grid}. Il ne porte que ce qui
 * est vraiment à lui, c'est-à-dire son orientation.
 */
public final class Hero implements Occupant {

    private Direction facing = Direction.RIGHT;

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
