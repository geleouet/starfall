package com.starfall.scene;

import com.starfall.game.ActionResult;
import com.starfall.game.Arena;
import com.starfall.game.Enemy;
import com.starfall.game.Hero;
import com.starfall.game.HudLayout;
import com.starfall.game.Occupant;
import com.starfall.game.Tile;
import com.starfall.game.TilePreview;

import java.util.ArrayList;
import java.util.List;

/**
 * Tout ce que l'interface <b>dit</b>, en français, séparé de l'endroit où elle le dessine.
 *
 * <h2>Pourquoi c'est une classe à part</h2>
 *
 * <p>Un libellé d'interface peut échouer de deux façons qu'aucun coup d'œil ne rattrape :
 *
 * <ul>
 *   <li><b>Un caractère que la police ne connaît pas.</b> {@code PixelFont.draw} saute en silence
 *       ce qu'elle ne sait pas dessiner : « PRÊTE » s'afficherait « PRTE » sans le moindre signal,
 *       et le jeu est intégralement en français accentué.</li>
 *   <li><b>Une ligne trop longue.</b> Elle sort de la zone garantie, donc elle disparaît sur
 *       certaines tailles de fenêtre et pas sur d'autres — jamais sur celle où l'on développe.</li>
 * </ul>
 *
 * <p>Tant que ces phrases vivaient au milieu du code de rendu, les vérifier demandait un contexte
 * graphique, c'est-à-dire ne les vérifiait pas. Ici, elles sont pures : {@code HudTextTest} les
 * engendre toutes sur des milliers d'états de partie et mesure chacune. Le choix de ce qui
 * s'affiche — {@link #infoLines} — est ici pour la même raison.
 *
 * <h2>Les cases n'ont pas de numéro</h2>
 *
 * <p>L'interface a d'abord parlé en numéros de case : « FRAPPE CASE 12 ». Aucune dalle n'étant
 * numérotée à l'écran, lire cette phrase sur une grille de quinze demandait de compter douze
 * dalles depuis le bord. Elle parle donc en <b>relatif</b> — « la case devant », « la 2e case
 * devant » — parce que c'est ainsi qu'on raisonne sur une ligne, et parce que le repère violet
 * sous la dalle dit déjà <em>où</em> avec une précision qu'aucun numéro n'ajoute.
 */
public final class HudText {

    private HudText() {
    }

    /**
     * Bandeau d'état.
     *
     * <p>Il ne porte que ce que la scène ne montre pas déjà, et une chose qu'elle montre mal : le
     * <b>nombre de coups</b> qui tomberont sur la case du héros. Les barres sur la dalle le disent
     * aussi, mais un nombre écrit ne peut pas saturer — et c'est le chiffre le plus décisif de
     * l'écran quand le héros a cinq points de vie.
     *
     * <p>Deux choses en sont parties. L'<b>enchaînement</b> d'abord : c'est une récompense, pas une
     * donnée de décision, et sa place ici mettait la largeur du bandeau à deux pixels de la
     * rupture. Le <b>compteur de tours</b> ensuite, pour faire de la place au mot « invocation » —
     * et parce qu'il ne sert à rien pour décider : le rythme des archétypes lents se lit sur leurs
     * intentions annoncées, jamais sur une parité qu'il faudrait calculer. Il reste au bilan de fin
     * de partie, qui est sa vraie place.
     *
     * <p>Le mot « invocation » y est en revanche indispensable : c'est le seul danger que le
     * compteur de coups ne peut pas dire, puisque rien ne tombe sur la case.
     */
    public static String banner(Arena arena) {
        StringBuilder line = new StringBuilder();
        line.append("VAGUE ").append(arena.wave()).append('/').append(arena.waveCount())
                .append("  PV ").append(arena.hero().health()).append('/').append(Hero.MAX_HEALTH);
        // Rien à annoncer quand plus rien ne sera joué : « MENACE 2 » s'affichait au-dessus de
        // « PARTIE PERDUE ».
        int threat = arena.isOver() ? 0 : arena.threatCount(arena.heroCell());
        if (threat > 0) {
            line.append("  MENACE ").append(threat);
        }
        if (!arena.isOver() && arena.anySummonAnnounced()) {
            line.append("  INVOCATION");
        }
        return line.toString();
    }

    /**
     * Les lignes du panneau d'information, dans l'ordre.
     *
     * <p>La <b>ligne d'annonce est toujours la première</b>, et c'est un correctif : le survol du
     * râtelier la remplaçait entièrement par l'infobulle. Or l'instant qui suit la pose d'une tuile
     * est celui où le curseur se trouve sur le râtelier <em>et</em> où le joueur veut savoir ce que
     * sa nouvelle tuile va faire. L'interface lui cachait exactement ce qu'il venait de demander,
     * pendant que la touche d'exécution restait active.
     *
     * <p>Jamais plus de {@link HudLayout#MAX_INFO_LINES} lignes : au-delà, le panneau mordrait sur
     * les glyphes d'intention, c'est-à-dire qu'une infobulle cacherait le télégraphe.
     *
     * @param rackSlot  tuile du râtelier survolée, ou {@code -1}
     * @param queueSlot emplacement de file survolé, ou {@code -1}
     * @param cell      case du plateau survolée, ou {@code -1}
     * @param last      résultat de la dernière action, ou {@code null}
     */
    public static List<String> infoLines(Arena arena, int rackSlot, int queueSlot, int cell,
                                         ActionResult last) {
        List<String> lines = new ArrayList<>(HudLayout.MAX_INFO_LINES);
        lines.add(announce(arena));

        Tile hovered = hoveredTile(arena, rackSlot, queueSlot);
        if (hovered != null) {
            lines.add(rackSlot >= 0
                    ? rackHead(arena, hovered)
                    : queueHead(arena.queue().fromOldest(), queueSlot, arena.isOver()));
            lines.add(hovered.effect().toUpperCase());
            return lines;
        }
        if (arena.grid().occupantAt(cell) instanceof Enemy enemy) {
            lines.add(enemyHead(enemy));
            lines.add(enemyDetail(enemy, arena.isOver()));
            return lines;
        }
        if (last != null) {
            lines.add(lastAction(last, arena.lastCombo()));
        }
        return lines;
    }

    /** Tuile survolée, du râtelier ou de la file, ou {@code null}. Le râtelier a la priorité. */
    public static Tile hoveredTile(Arena arena, int rackSlot, int queueSlot) {
        List<Tile> rack = arena.rack().tiles();
        if (rackSlot >= 0 && rackSlot < rack.size()) {
            return rack.get(rackSlot);
        }
        List<Tile> queued = arena.queue().fromOldest();
        if (queueSlot >= 0 && queueSlot < queued.size()) {
            return queued.get(queueSlot);
        }
        return null;
    }

    /**
     * Vrai si la tuile survolée est celle qui partira, donc si son préavis a le droit d'annoncer un
     * résultat.
     *
     * <p>Survoler l'emplacement du sommet faisait passer son propre préavis de plein à creux : la
     * forme disait « ceci pourrait atteindre » à propos de la tuile qui allait partir.
     *
     * <p><b>Elle a été retirée une fois, à tort.</b> J'avais démontré que le retour anticipé qu'elle
     * garde ne pouvait rien changer à l'image, en raisonnant sur les couleurs : la portée statique
     * dessine un contour là où le préavis résolu a déjà rempli le même rectangle dans la même
     * couleur. Le raisonnement était juste — et incomplet. Il supposait qu'un préavis résolu existe,
     * or {@link Arena#previewTop()} rend <b>{@code null} quand la partie est finie</b>, précisément
     * pour ne pas promettre un coup qu'aucune touche ne peut plus déclencher. Sans ce retour, une
     * portée réapparaît alors sur fond nu, après la mort : 544 pixels mesurés par la review.
     *
     * <p>La leçon vaut mieux que le correctif : <b>une démonstration n'est pas une mesure</b>. Le
     * « rendu identique à l'octet » que j'avais présenté comme preuve ne prouvait rien, puisque
     * aucune planche du projet n'atteignait l'état « partie finie, file non vide ». La vitrine
     * l'atteint maintenant.
     */
    public static boolean hoveringTheTop(Arena arena, int rackSlot, int queueSlot) {
        return rackSlot < 0 && queueSlot >= 0 && queueSlot == arena.queue().size() - 1;
    }

    /**
     * La ligne la plus lue de l'écran : ce qui partira si le joueur appuie sur espace.
     *
     * <p>Elle est composée à partir du <b>préavis</b>, c'est-à-dire de l'objet même que l'exécution
     * consommera — jamais d'une relecture parallèle des règles. Voir {@link TilePreview}.
     */
    public static String announce(Arena arena) {
        if (arena.isOver()) {
            return arena.isVictory() ? "PARTIE GAGNÉE" : "PARTIE PERDUE";
        }
        TilePreview preview = arena.previewTop();
        if (preview == null) {
            return "FILE VIDE - POSEZ UNE TUILE (1-6 OU CLIC)";
        }
        // Le préavis ne décrit que la PREMIÈRE tuile de la salve, et il ne peut pas faire mieux :
        // ce que la deuxième trouvera devant elle dépend de ce que la première aura fait, et le
        // jeu ne sait pas se dupliquer pour le calculer. Annoncer toute la salve demanderait de
        // deviner — exactement ce que ce projet interdit depuis M6. Le compte des tuiles dit le
        // reste : le joueur sait combien part, et il voit ce que la première fera.
        // Une seule tuile n'est pas une salve, et le modèle le dit déjà : {@code unleash} renvoie
        // le résultat propre de la tuile plutôt que « salve », au motif que ce mot « pour un coup
        // unique se lirait comme une grandiloquence ». L'affichage disait l'inverse dans le même
        // jalon — deux décisions opposées sur la même question.
        int queued = arena.queue().size();
        String head = queued > 1
                ? "SALVE " + queued + " - " + preview.tile().label().toUpperCase()
                : "PRÊTE : " + preview.tile().label().toUpperCase();
        return head + " - " + effectOf(arena, preview);
    }

    /**
     * Ce que le préavis annonce, en un complément.
     *
     * <p>Un <em>complément</em> et non une phrase : il se lit derrière le nom de la tuile, qui porte
     * déjà le verbe. La première version répétait celui-ci et donnait « SOMMET : FRAPPE - FRAPPE
     * CASE 5 » — une planche-contact l'a montré avant qu'un joueur ne le lise.
     */
    public static String effectOf(Arena arena, TilePreview preview) {
        return switch (preview.kind()) {
            case HIT -> "SUR " + ahead(arena, preview.aim());
            case PUSH -> preview.outcome() == ActionResult.PUSHED
                    ? "RECULE D'UNE CASE"
                    : "BUTE SUR " + obstacle(arena, preview.landing()) + " : CHOC";
            case MOVE -> preview.outcome() == ActionResult.DASHED
                    ? "COURSE DE " + distance(arena, preview.landing()) + " CASES"
                    : "RECUL D'UNE CASE";
            case TURN -> "REGARD À " + preview.direction().label().toUpperCase();
            case NONE -> preview.outcome() == ActionResult.NO_TARGET
                    ? empty(arena, preview.aim()) + " - TUILE PERDUE"
                    : "BLOQUÉ - TUILE PERDUE";
        };
    }

    /** Retour sur l'action qui vient d'être jouée. */
    public static String lastAction(ActionResult result, int combo) {
        // Une poussée qui butte et une poussée qui aboutit déplacent toutes les deux quelqu'un
        // d'une case : sans retour écrit, le joueur ne sait pas laquelle des deux il vient de jouer.
        String line = "DERNIÈRE : " + result.label().toUpperCase();
        if (result == ActionResult.COLLIDED) {
            return line + " - ENNEMI ÉTOURDI";
        }
        if (combo > 1) {
            return line + " - ENCHAÎNEMENT " + combo;
        }
        return line;
    }

    /**
     * Première ligne d'une infobulle de râtelier : nom, portée, gratuité, <b>et disponibilité</b>.
     *
     * <p>Les quatre sur une ligne parce que la ligne d'annonce a la priorité sur la première du
     * panneau. C'est aussi la ligne qui dit qu'un point de recharge vaut un tour à attendre : le
     * râtelier montrait bien des points ocre à compter, sans jamais dire ce qu'ils comptaient.
     */
    public static String rackHead(Arena arena, Tile tile) {
        return head(tile) + "   " + rackState(arena, tile);
    }

    private static String rackState(Arena arena, Tile tile) {
        if (arena.isOver()) {
            // Le préavis se tait quand la partie est finie ; l'infobulle disait encore « PRÊTE »
            // pour une tuile qu'aucune touche ne pouvait plus poser.
            return "PARTIE FINIE";
        }
        int missing = arena.rack().missingPoints(tile);
        if (missing > 0) {
            return "EN RECHARGE : " + missing;
        }
        if (!arena.rack().holds(tile)) {
            return "SUR LA FILE";
        }
        // Troisième endroit où la même question se pose, et le seul qui l'ignorait encore : une
        // tuile rechargée sur une FILE PLEINE ne se pose pas davantage qu'une tuile en recharge, et
        // l'infobulle annonçait pourtant son coût comme si elle était disponible. Deux corrections
        // avaient déjà porté sur cette règle — la couleur de la portée, le halo de survol — et
        // c'est en la posant à l'arène plutôt qu'en la recopiant qu'on cesse d'en oublier un.
        if (!arena.canQueue(tile)) {
            return "FILE PLEINE";
        }
        return "RECHARGE " + tile.rechargeCost();
    }

    /**
     * Première ligne d'une infobulle de file : la même, plus la place dans l'ordre d'exécution.
     *
     * <p>« SOMMET » veut dire « c'est celle-là qui part la première ». Quand la partie est finie,
     * aucune ne part : le mot devient une promesse en trop, exactement comme le râtelier qui dit
     * « PARTIE FINIE » au lieu d'annoncer une portée. L'asymétrie avait été introduite sans qu'on
     * la voie — le râtelier avait reçu le traitement, pas la file.
     */
    public static String queueHead(List<Tile> queued, int index, boolean over) {
        String place = over ? "PARTIE FINIE"
                : index == queued.size() - 1
                        ? "SOMMET"
                        : "POSITION " + (index + 1) + "/" + queued.size();
        return head(queued.get(index)) + "   " + place;
    }

    private static String head(Tile tile) {
        return tile.label().toUpperCase() + "   " + tile.reachLabel().toUpperCase()
                + (tile.isFreePlay() ? "   GRATUITE" : "");
    }

    /**
     * Infobulle d'un ennemi survolé.
     *
     * <p>Elle existe pour la raison exacte qui a fait écrire celles des tuiles : « l'estoc porte à
     * deux cases et rien ne le disait — le joueur devait l'apprendre en gâchant des tuiles. » C'est
     * mot pour mot vrai de l'archer, du lancier et du trait agressif, sauf qu'on l'apprenait en
     * prenant des coups.
     */
    public static String enemyHead(Enemy enemy) {
        return enemy.label().toUpperCase() + "   PORTÉE " + enemy.reach()
                + "   PV " + enemy.health() + "/" + enemy.maxHealth();
    }

    /** Deuxième ligne : ce qu'il fait maintenant, et ce qui le rend particulier. */
    public static String enemyDetail(Enemy enemy, boolean over) {
        // Une intention est une annonce : elle dit ce que la phase ennemie va faire. Après la fin de
        // partie il n'y en aura plus, et l'infobulle disait encore « FRAPPE » pendant que le râtelier
        // disait « PARTIE FINIE ». Le glyphe et le texte se sont tus ensemble, cette fois.
        if (over) {
            return "PARTIE FINIE";
        }
        StringBuilder line = new StringBuilder(enemy.intention().kind().label().toUpperCase());
        if (enemy.blowsPerAttack() > 1) {
            line.append(" - ").append(enemy.blowsPerAttack()).append(" COUPS PAR FRAPPE");
        }
        if (enemy.summonsLeft() > 0) {
            // Le compte à rebours des invocations se joue autrement qu'une menace ordinaire : deux
            // invocations refusées, et le souverain n'est plus qu'un ennemi de plus.
            line.append(" - ").append(enemy.summonsLeft()).append(" INVOCATIONS");
        }
        if (enemy.has(com.starfall.game.Trait.EXPLOSIF)) {
            line.append(" - EXPLOSE EN MOURANT");
        }
        return line.toString();
    }

    /** Bannière de fin de partie. Ne vaut la peine d'être appelée que si la partie est finie. */
    public static List<String> outcome(Arena arena) {
        if (arena.isVictory()) {
            return List.of("VICTOIRE",
                    "LES " + arena.waveCount() + " VAGUES SONT TOMBÉES - RECORD " + arena.bestCombo());
        }
        return List.of("DÉFAITE", "LA VAGABONDE EST TOMBÉE AU TOUR " + arena.turnsTaken());
    }

    /**
     * L'aide : les commandes, en français.
     *
     * <p>Elle est ouverte au départ et se referme au premier geste. Un jeu au tour par tour dont
     * les commandes ne sont écrites nulle part n'est pas jouable ; un jeu qui les affiche en
     * permanence gaspille la moitié de son écran.
     */
    public static List<String> help() {
        return List.of(
                "AIDE - STARFALL",
                "FLÈCHES OU A/D : SE TOURNER, PUIS AVANCER",
                "1 À 6 : CHARGER UNE TUILE - UN TOUR CHACUNE",
                "ESPACE : LANCER LA SALVE - TOUTE LA FILE, UN TOUR",
                "RETOUR ARRIÈRE OU CLIC SUR LA FILE : REPRENDRE",
                "E : ÉCHANGER DE PLACE AVEC LA CIBLE VISÉE",
                "SURVOL : PORTÉE ET DÉTAIL   CLIC : TOUT",
                "F1 FERMER   F11 PLEIN ÉCRAN   ÉCHAP QUITTER");
    }

    /**
     * Rappel de la touche d'aide, dans le coin du bandeau.
     *
     * <p>Le même texte que l'aide soit ouverte ou non, et c'est un choix de largeur autant que de
     * clarté : « F1 FERMER » est plus long de deux caractères, ce qui suffisait à faire chevaucher
     * le bandeau et le rappel dans les parties longues — le test de largeur l'a attrapé avant que
     * ça n'arrive. La touche bascule ; le libellé nomme ce qu'elle ouvre.
     */
    public static final String HELP_HINT = "F1 AIDE";

    // ---------------------------------------------------------------- désignation relative

    /** « LA CASE DEVANT », « LA 2E CASE DEVANT »… selon la distance et le sens du regard. */
    private static String ahead(Arena arena, int cell) {
        int offset = Math.abs(cell - arena.heroCell());
        if (offset <= 1) {
            return "LA CASE DEVANT";
        }
        return "LA " + offset + "E CASE DEVANT";
    }

    /** « RIEN DEVANT », « RIEN À 2 CASES » : la forme courte, pour la ligne d'annonce. */
    private static String empty(Arena arena, int cell) {
        int offset = distance(arena, cell);
        return offset <= 1 ? "RIEN DEVANT" : "RIEN À " + offset + " CASES";
    }

    private static int distance(Arena arena, int cell) {
        return Math.abs(cell - arena.heroCell());
    }

    /**
     * Ce sur quoi une poussée butte : le mur, ou le voisin.
     *
     * <p>L'archétype seul, sans ses traits : « BUTE SUR LE COLOSSE EXPLOSIF : CHOC » derrière le
     * préfixe de la ligne d'annonce arrivait à cinq pixels de la largeur garantie. Les traits sont
     * dits par l'infobulle de l'ennemi, qui a la place de les écrire.
     */
    private static String obstacle(Arena arena, int landing) {
        Occupant blocker = arena.grid().contains(landing) ? arena.grid().occupantAt(landing) : null;
        if (blocker == null) {
            return "LE MUR";
        }
        if (!(blocker instanceof Enemy enemy)) {
            return blocker.label().toUpperCase();
        }
        // « LE ARCHER » : l'article élidé n'est pas un raffinement, c'est la différence entre une
        // interface écrite en français et une interface qui concatène des chaînes.
        String label = enemy.kind().label().toUpperCase();
        return "AEIOUÉÈÊ".indexOf(label.charAt(0)) >= 0 ? "L'" + label : "LE " + label;
    }
}
