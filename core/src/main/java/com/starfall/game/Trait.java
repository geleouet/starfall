package com.starfall.game;

/**
 * Un trait modifie le comportement d'un ennemi sans changer son archétype.
 *
 * <p>Un même sabreur devient une autre menace selon qu'il est rapide ou explosif : c'est ce qui
 * permet de composer beaucoup de situations avec peu d'archétypes. Les traits sont <b>visibles</b>
 * sur l'ennemi — un trait caché serait un piège, pas une lecture.
 *
 * <h2>Un trait ne double jamais une activation</h2>
 *
 * <p>Il aurait été naturel de faire agir un ennemi rapide « deux fois ». Ce serait incompatible
 * avec le télégraphe : la deuxième action ne pourrait pas être annoncée sans être recalculée, et le
 * joueur lirait une promesse qui ne couvre que la moitié de ce qui va se passer. Les traits
 * amplifient donc l'intention <em>annoncée</em> — deux cases au lieu d'une, deux coups au lieu d'un
 * — et ce qui est montré reste exactement ce qui sera joué.
 */
public enum Trait {

    /** Avance de deux cases au lieu d'une, et frappe deux fois au lieu d'une. */
    RAPIDE("rapide"),
    /** À sa mort, frappe les deux cases voisines — et la chaîne se propage. */
    EXPLOSIF("explosif"),
    /** Frappe une case plus loin que son archétype ne le permet. */
    AGRESSIF("agressif"),
    /** Comble d'un coup toute la distance quand sa cible est en ligne, au lieu d'avancer d'un pas. */
    FONCEUR("fonceur");

    private final String label;

    Trait(String label) {
        this.label = label;
    }

    /** Libellé affichable, en français. */
    public String label() {
        return label;
    }
}
