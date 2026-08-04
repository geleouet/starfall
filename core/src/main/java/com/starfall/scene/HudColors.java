package com.starfall.scene;

import com.badlogic.gdx.graphics.Color;

/**
 * Couleurs de l'interface — celles qui portent un <b>rôle</b>, par opposition à celles de la
 * palette d'art, qui portent une <b>famille</b>.
 *
 * <p>La distinction n'est pas cosmétique. Les tuiles Free-Play sont dessinées en vert, et « vert »
 * doit vouloir dire une seule chose : <i>ne consomme pas de tour</i>. Le repère de disponibilité du
 * râtelier utilisait exactement le même vert que la palette, si bien qu'au repos les six tuiles
 * portaient un trait vert : un joueur qui balayait du regard à la recherche du vert trouvait six
 * réponses au lieu de deux.
 *
 * <p>Ces constantes sont donc <b>volontairement tenues à l'écart des couleurs de la palette</b>, et
 * un test le vérifie en relisant {@code art/palette.txt}.
 */
public final class HudColors {

    private HudColors() {
    }

    /**
     * Ce qui concerne la file : son cadre, sa prochaine tuile, le rappel au râtelier.
     *
     * <p>Une crème chaude et non l'or de la palette : les tuiles de placement <em>sont</em> or, et
     * un repère de file en or à côté d'une icône or ne dit plus de quoi il parle.
     */
    public static final Color QUEUE = new Color(0xfff2c0ff);
    /** Emplacement libre de la file. */
    public static final Color SLOT_EMPTY = new Color(0x8f93a8ff);
    /** Contour d'un emplacement libre, pour qu'il reste comptable. */
    public static final Color SLOT_OUTLINE = new Color(0x4a5570ff);
    /** Extinction d'une tuile indisponible : éteinte, mais toujours identifiable. */
    public static final Color DIMMED = new Color(0x3a3a44ff);
    /** Points de recharge restants. Un orange vif, distinct de l'ocre des tuiles de placement. */
    public static final Color RECHARGE = new Color(0xff8a3cff);
    /** Survol de la souris. */
    public static final Color HOVER = new Color(0xd8f0ffff);

    /**
     * Case menacée par l'intention annoncée d'un ennemi, et tout ce qui annonce un coup.
     *
     * <p>Une seule couleur pour tout ce qui va faire mal, y compris la prise d'élan : le joueur n'a
     * pas à apprendre une nuance de rouge pour comprendre qu'il doit bouger.
     */
    public static final Color THREAT = new Color(0xff4a5aff);
}
