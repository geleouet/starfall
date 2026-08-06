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
    /**
     * Ce qui est <b>absent</b> : une tuile indisponible, un point de vie perdu.
     *
     * <p>Elle a porté un troisième sens pendant un temps — « cette tuile va rater » — et c'était
     * une faute de la même famille que le vert Free-Play : trois messages sur une couleur bâtie
     * pour n'en porter qu'un. Pire, elle le portait mal : à {@code 0x3a3a44} sur le fond de la
     * fosse, le repère qui annonce la promesse-titre du jalon faisait un rectangle de trois pixels
     * quasi invisible. Un coup qui ne portera pas se dit maintenant par une <b>forme</b> — une
     * croix — et non par une extinction.
     */
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

    /**
     * Or : ce qui part <b>&agrave; la fin de ce tour</b>, quoi que fasse le joueur.
     *
     * <p>Une couleur &agrave; elle, et non une nuance de la menace. Rouge veut dire
     * &laquo; annonc&eacute; &raquo; ; l'or veut dire &laquo; annonc&eacute; ET imminent &raquo;, et
     * entre les deux il y a un tour de r&eacute;pit &mdash; c'est-&agrave;-dire toute la
     * d&eacute;cision.
     */
    public static final Color IMMINENT = new Color(0xffd24aff);

    /** L'or en creux : l'autre temps du battement. Voir {@link #IMMINENT}. */
    public static final Color IMMINENT_DIM = new Color(0xa8792cff);

    /**
     * Ce que <b>ma</b> prochaine tuile va faire : portée, case visée, trajectoire d'une poussée.
     *
     * <p>Un violet, et le choix compte : le rouge appartient à ce qui va m'arriver, le violet à ce
     * que je vais provoquer. Les deux repères tombent souvent sur la même case — une poussée qui
     * répond à une charge, par exemple — et le joueur doit lire d'un coup laquelle des deux
     * intentions il regarde.
     */
    public static final Color PREVIEW = new Color(0xb47dffff);

    // ------------------------------------------------------------------ surfaces

    /**
     * Fond des panneaux d'interface : infobulles, aide, bannière de fin.
     *
     * <p>Ce n'est pas un signal mais une <b>surface</b>, et la distinction a des conséquences
     * mesurables : un signal doit se distinguer des couleurs de la palette d'art, une surface doit
     * se distinguer de tous les signaux, sinon le texte posé dessus disparaît. Le test applique
     * bien deux règles différentes, plutôt qu'une règle unique qui aurait interdit tout fond
     * sombre — les noirs de la palette sont des noirs, et un panneau doit pouvoir être sombre.
     */
    public static final Color PANEL = new Color(0x141024f0);
    /** Bord d'un panneau : assez clair pour poser le panneau sur la scène, jamais lu comme un signal. */
    public static final Color PANEL_EDGE = new Color(0x6a5a8cff);
    /** Texte d'interface. */
    public static final Color TEXT = new Color(0xffffffff);
    /** Texte secondaire : ce qui précise sans commander le regard. */
    public static final Color TEXT_DIM = new Color(0xa8b0d8ff);

    // ------------------------------------------------------------------ fonds de la scène
    //
    // Ces trois bandes vivaient en constantes privées de la scène, donc aucun test ne pouvait
    // mesurer un signal contre le fond sur lequel il est réellement dessiné. C'est ainsi qu'un
    // repère est resté illisible : il était bien distinct de toutes les autres couleurs
    // d'interface, et se confondait avec le décor.

    /** Le ciel, au-dessus du mur. */
    public static final Color SKY = new Color(0x141a2eff);
    /** Le mur derrière la grille : le fond des figures, de leurs points de vie et de leurs intentions. */
    public static final Color WALL = new Color(0x1c2440ff);
    /** La fosse sous les dalles : le fond de toutes les bandes de repères, de la file et du râtelier. */
    public static final Color PIT = new Color(0x0c101eff);
}
