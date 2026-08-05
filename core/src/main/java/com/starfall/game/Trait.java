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

    /**
     * Avance de deux cases au lieu d'une, et frappe deux fois au lieu d'une.
     *
     * <p><b>Écart assumé avec la source.</b> Chez Shogun Showdown, « rapide » veut dire « peut
     * déclarer son attaque dès qu'une tuile est ajoutée à sa file, sans attendre un tour ». Ce sens
     * suppose que <em>les ennemis ont une file</em>, ce que notre modèle n'a pas : ici un ennemi
     * annonce une intention et l'exécute à la phase suivante, sans pile intermédiaire. Le sens
     * d'origine n'a donc pas de traduction, et le nom a été réemployé pour l'idée voisine — agir
     * deux fois plus. C'est une déviation <b>forcée</b>, pas un oubli, et elle est écrite ici parce
     * qu'un lecteur du document d'inspiration la chercherait.
     */
    RAPIDE("rapide"),
    /**
     * À sa mort, frappe les deux cases voisines — et la chaîne se propage.
     *
     * <p>Conforme à la source, y compris le chiffre : deux dégâts aux unités adjacentes.
     */
    EXPLOSIF("explosif"),
    /**
     * Frappe une case plus loin que son archétype ne le permet.
     *
     * <p><b>Écart assumé avec la source.</b> Chez Shogun Showdown, « agressif » veut dire « se
     * rapproche du joueur après avoir attaqué », par opposition à un défaut où les ennemis
     * <em>reculent</em>. Ce défaut-là n'existe pas ici : seul l'archer recule, et c'est une
     * propriété de son archétype, pas une règle générale. Le trait d'origine n'aurait donc rien
     * distingué. Il désigne ici l'allonge, ce qui garde son rôle — rendre une menace plus pressante
     * — sans emprunter un sens que le modèle ne porte pas.
     */
    AGRESSIF("agressif"),
    /**
     * Comble d'un coup toute la distance quand sa cible est en ligne, au lieu d'avancer d'un pas.
     *
     * <p>Conforme à la source : « se déplace toujours le plus loin possible dans la direction
     * choisie ».
     */
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
