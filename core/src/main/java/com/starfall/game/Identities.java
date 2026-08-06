package com.starfall.game;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Un numéro unique par occupant, pour toute sa vie.
 *
 * <h2>Pourquoi le modèle porte ceci, alors qu'il ne s'en sert pas</h2>
 *
 * <p>C'est une concession assumée à la vue, et la seule. Le déroulé d'une salve montre le plateau
 * d'un temps puis celui du suivant ; pour <b>animer</b> le passage de l'un à l'autre, il faut savoir
 * que le sabreur de la case 4 est <em>le même</em> que celui de la case 3 juste avant, et non un
 * autre sabreur. Sans réponse à cette question, on ne peut que faire clignoter des figures d'une
 * case à l'autre — ce qu'on faisait.
 *
 * <p>Rapprocher les figures par leur sprite serait suffisant tant qu'aucun plateau ne porte deux
 * ennemis identiques. Le nôtre en porte : une vague de deux sabreurs, une poussée qui décale l'un
 * dans la case que l'autre vient de quitter, et le rapprochement devient un tirage au sort. Un
 * numéro tranche là où une ressemblance hésite.
 *
 * <h2>Ce que ce numéro n'est pas</h2>
 *
 * <p>Il n'est <b>ni ordonné, ni affiché, ni reproductible d'une exécution à l'autre</b>. Deux
 * parties identiques donneront des numéros différents, et c'est sans conséquence parce que rien ne
 * dépend de leur <em>valeur</em> : on ne s'en sert que pour comparer deux figures entre elles. Cette
 * affirmation est fragile — elle serait fausse au premier tri, au premier parcours de {@code
 * HashMap}, au premier numéro peint sur un écran — alors elle est <b>gardée par un test</b> qui
 * rejoue la même salve dans deux arènes distinctes, aux numéros donc différents, et exige la même
 * chorégraphie au pixel près.
 */
final class Identities {

    private static final AtomicLong NEXT = new AtomicLong(1);

    private Identities() {
    }

    /** Le numéro suivant. Jamais zéro : zéro reste disponible pour dire « personne ». */
    static long next() {
        return NEXT.getAndIncrement();
    }
}
