package com.starfall.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.starfall.scene.HudText;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Annoncé ne veut pas dire imminent.
 *
 * <h2>Ce que ce test garde</h2>
 *
 * <p>Une intention dit ce qu'un ennemi fera <b>à sa prochaine activation</b>. Elle ne dit pas si
 * cette activation est celle qui vient. Le colosse ne joue qu'une phase sur deux, et un ennemi
 * étourdi en saute une : tous deux portent une menace qui ne tombera pas ce tour-ci.
 *
 * <p>L'écran distingue désormais les deux — cadre rouge pour « annoncé », cadre doré battant pour
 * « ça part à la fin de ce tour, quoi que tu fasses ». Toute la valeur de cette distinction tient
 * à {@link Arena#willAct(Enemy)}, et une couleur ne peut pas garder un prédicat : si celui-ci se
 * mettait à répondre vrai pour tout le monde, l'écran redeviendrait uniforme sans qu'aucune image
 * ne change de forme, seulement de teinte, et personne ne le verrait.
 *
 * <p>C'est aussi <b>la pire des deux erreurs possibles</b> qui est gardée ici. Lire « j'ai un tour
 * de répit » quand on n'en a pas coûte une partie ; lire l'inverse ne coûte qu'une prudence
 * inutile.
 */
class ImminenceTest {

    @Test
    @DisplayName("Le colosse n'est imminent qu'une phase sur deux")
    void theColossusIsImminentEveryOtherPhase() {
        Arena arena = new Arena(11, 1);
        Enemy colossus = new Enemy(EnemyKind.COLOSSE);
        Enemy sabreur = new Enemy(EnemyKind.SABREUR);
        arena.grid().place(7, colossus);
        arena.grid().place(9, sabreur);
        arena.announceIntentions();

        boolean first = arena.willAct(colossus);
        assertTrue(arena.willAct(sabreur),
                "le sabreur joue a toutes les phases : sans lui, l'alternance ci-dessous pourrait"
                        + " venir d'un compteur global et non de l'archetype");

        // Un tour passe. Le pas est le geste le moins intrusif : il ne tue personne, donc il ne
        // peut pas faire disparaitre le sujet de la mesure.
        arena.step(arena.hero().facing());

        assertFalse(first == arena.willAct(colossus),
                "le colosse est reste dans le meme etat d'une phase a l'autre : il n'agit qu'une"
                        + " fois sur deux, et c'est cette alternance que le cadre dore annonce");
        assertTrue(arena.willAct(sabreur),
                "le sabreur, lui, ne doit jamais cesser d'etre imminent");
    }

    @Test
    @DisplayName("Un ennemi étourdi annonce sans être imminent")
    void aStunnedEnemyAnnouncesWithoutBeingImminent() {
        Arena arena = new Arena(11, 1);
        Enemy sabreur = new Enemy(EnemyKind.SABREUR);
        arena.grid().place(8, sabreur);
        arena.announceIntentions();

        assertTrue(arena.willAct(sabreur), "la premisse : il etait imminent avant d'etre etourdi");

        sabreur.setStunned(true);

        assertFalse(arena.willAct(sabreur),
                "un ennemi etourdi saute son activation : son intention reste affichee, mais elle"
                        + " ne tombe pas ce tour-ci, et confondre les deux fait lire un danger"
                        + " immediat la ou il y a un tour de repit");
    }

    /**
     * Le bandeau annonce ce que le h&eacute;ros perd, pas combien d'ennemis frappent.
     *
     * <p>La plaque pos&eacute;e au-dessus d'un ennemi annonce son prix en points. Le bandeau, lui,
     * comptait les <em>coups</em> : un lancier seul affichait « 2 » sur sa plaque et « MENACE 1 »
     * en haut de l'&eacute;cran. Deux nombres pour la m&ecirc;me question, dans deux unit&eacute;s,
     * dont un que le joueur lit d'un coup d'&oelig;il pour d&eacute;cider s'il reste.
     *
     * <p>Ce test ne vaut que parce que les deux comptes <b>diff&egrave;rent</b> sur le montage
     * choisi : la pr&eacute;misse est ass&eacute;r&eacute;e, faute de quoi il serait vert avec
     * l'ancienne version comme avec la nouvelle.
     */
    @Test
    @DisplayName("Le bandeau annonce des points, pas un nombre d'assaillants")
    void theBannerAnnouncesPointsRatherThanAttackers() {
        Arena arena = new Arena(11, 4);
        arena.grid().place(5, new Enemy(EnemyKind.LANCIER));
        arena.announceIntentions();

        int blows = arena.threatCount(arena.heroCell());
        int points = arena.threatDamage(arena.heroCell());

        // La premisse : sur un montage ou les deux comptes coincident, ce test passerait quelle
        // que soit l'unite choisie, et ne distinguerait donc rien.
        assertTrue(points > blows,
                "montage invalide : " + blows + " coup(s) pour " + points + " point(s), les deux"
                        + " comptes coincident et ce test ne departage plus les deux unites");

        String banner = HudText.banner(arena, true);
        assertTrue(banner.contains("MENACE " + points),
                "le bandeau dit « " + banner + " » : il devrait annoncer " + points + " points,"
                        + " le prix de ce qui vient, et non " + blows + " assaillant(s)");
    }

    /**
     * La file d&rsquo;un ennemi : ce qu&rsquo;il tient, et non seulement ce qu&rsquo;il fait.
     *
     * <p>Le lancier en tient deux &mdash; l&rsquo;&eacute;lan, puis la charge &mdash; et cette suite
     * existait dans le mod&egrave;le depuis M6, cach&eacute;e dans un bool&eacute;en. Rien &agrave;
     * l&rsquo;&eacute;cran ne la montrait : on voyait « il se charge », jamais « il se charge
     * <em>pour une charge</em> », et le joueur apprenait ce qui venait en le prenant.
     */
    @Test
    @DisplayName("Un lancier qui prend son élan tient deux actions, les autres une seule")
    void aWindingLancerHoldsTwoActions() {
        Arena arena = new Arena(11, 1);
        Enemy lancer = new Enemy(EnemyKind.LANCIER);
        Enemy sabreur = new Enemy(EnemyKind.SABREUR);
        arena.grid().place(6, lancer);
        // DERRIERE le lancier : place entre lui et le heros, il lui bouchait la ligne et le
        // lancier avancait au lieu de prendre son elan. Un montage qui ne produit pas l'etat
        // qu'il pretend eprouver ne prouve rien - la premisse ci-dessous le dit desormais.
        arena.grid().place(9, sabreur);
        arena.announceIntentions();

        assertEquals(Intention.Kind.WIND_UP, lancer.intention().kind(),
                "montage invalide : le lancier devait prendre son elan");
        assertEquals(2, lancer.plan().size(),
                "l'elan est le premier temps d'une suite de deux : la charge qui suit est promise"
                        + " sans condition, et ne rien en dire la laisse invisible");
        assertEquals(Intention.Kind.CHARGE, lancer.plan().get(1).kind(),
                "ce qui suit un elan est une charge, et rien d'autre");

        // La contre-epreuve. Sans elle, une file qui rendrait toujours deux entrees passerait.
        assertEquals(1, sabreur.plan().size(),
                "un sabreur ne tient qu'une action : " + sabreur.plan());
    }

    /**
     * Ce que la file promet est ce que le cerveau joue.
     *
     * <h2>Pourquoi ce test existe</h2>
     *
     * <p>La r&egrave;gle &laquo; apr&egrave;s un &eacute;lan vient une charge &raquo; est
     * &eacute;crite &agrave; <b>deux endroits</b> : dans {@code Enemy.plan()}, qui la montre au
     * joueur, et dans {@code EnemyBrain}, qui la joue. Ce projet a pay&eacute; neuf fois pour
     * savoir ce qu&rsquo;il advient d&rsquo;une r&egrave;gle &eacute;crite &agrave; deux endroits ;
     * ici le prix serait plus lourd qu&rsquo;ailleurs, parce que la carte pos&eacute;e au-dessus de
     * l&rsquo;ennemi est une <b>promesse</b>, et qu&rsquo;une promesse d&eacute;mentie est
     * exactement ce que le t&eacute;l&eacute;graphe interdit.
     *
     * <p>Les deux &eacute;critures ne peuvent pas &ecirc;tre fusionn&eacute;es sans faire remonter
     * le cerveau dans la vue. Elles sont donc <b>confront&eacute;es</b> : on annonce, on laisse la
     * phase se jouer, et on v&eacute;rifie que ce qui arrive est ce qui avait &eacute;t&eacute;
     * montr&eacute;.
     */
    @Test
    @DisplayName("Ce que la file annonce après l'élan est bien ce qui vient")
    void whatThePlanPromisesIsWhatTheBrainPlays() {
        Arena arena = new Arena(11, 1);
        Enemy lancer = new Enemy(EnemyKind.LANCIER);
        arena.grid().place(6, lancer);
        arena.announceIntentions();

        assertEquals(Intention.Kind.WIND_UP, lancer.intention().kind(),
                "montage invalide : le lancier devait prendre son elan");
        Intention.Kind promised = lancer.plan().get(1).kind();

        // Un demi-tour : il consomme un tour sans deplacer le heros, donc la phase ennemie se joue
        // et le lancier passe de l'elan a ce qui suit.
        arena.step(arena.hero().facing().opposite());

        assertEquals(promised, lancer.intention().kind(),
                "la file annoncait " + promised + " et le cerveau joue "
                        + lancer.intention().kind() + " : la carte posee au-dessus de l'ennemi est"
                        + " une promesse, et le telegraphe interdit qu'une promesse soit dementie");
    }

    /**
     * Une file qui se remplit ne menace personne.
     *
     * <h2>La sur-promesse, et pourquoi elle co&ucirc;te plus cher que l&rsquo;inverse</h2>
     *
     * <p>Un colosse met deux tours &agrave; remplir sa file. Pendant le premier, il tient
     * d&eacute;j&agrave; une action &mdash; mais elle ne part pas. La compter comme une menace
     * ferait <b>fuir le joueur d&rsquo;une case o&ugrave; rien ne tombe</b>, et lui co&ucirc;terait
     * un tour pour rien ; ce projet appelle cela sur-promettre, et c&rsquo;est la faute que le
     * t&eacute;l&eacute;graphe existe pour interdire.
     *
     * <p>Le test tient les deux moiti&eacute;s : rien tant qu&rsquo;elle se remplit, <b>tout</b>
     * d&egrave;s qu&rsquo;elle est pleine. La seconde est la contre-&eacute;preuve de la
     * premi&egrave;re &mdash; un compteur qui rendrait toujours z&eacute;ro satisferait l&rsquo;une
     * sans rien garder.
     */
    @Test
    @DisplayName("Une file qui se remplit ne menace rien ; pleine, elle menace tout")
    void aFillingPlanThreatensNothingAndAFullOneThreatensAll() {
        Arena arena = new Arena(11, 4);
        Enemy colossus = new Enemy(EnemyKind.COLOSSE);
        arena.grid().place(5, colossus);
        arena.announceIntentions();

        assertTrue(EnemyKind.COLOSSE.planSize() > 1,
                "montage invalide : sans file de plusieurs actions, il n'y a pas de remplissage"
                        + " a eprouver");
        assertFalse(colossus.isPlanFull(), "il devait n'avoir rempli qu'a moitie");
        assertEquals(0, arena.threatDamage(arena.heroCell()),
                "une file qui se remplit annonce deja un coup, mais il ne part pas ce tour-ci :"
                        + " le compter ferait fuir le joueur d'une case ou rien ne tombe");

        // Un tour passe : la file se remplit, et ce qu'elle tient part maintenant.
        arena.step(arena.hero().facing().opposite());

        assertTrue(colossus.isPlanFull(), "sa file devait etre pleine apres un tour de plus");
        assertEquals(colossus.queued().size() * colossus.announcedDamage(),
                arena.threatDamage(arena.heroCell()),
                "pleine, la file menace de TOUT ce qu'elle tient : " + colossus.queued());
    }

    /**
     * Un ennemi d&eacute;cide depuis la case o&ugrave; sa propre file l&rsquo;aura men&eacute;.
     *
     * <h2>Ce que la mesure a montr&eacute;, et ce que ce test emp&ecirc;che de reperdre</h2>
     *
     * <p>Le colosse remplissait sa file <b>deux fois avec la m&ecirc;me action</b> : chaque
     * d&eacute;cision partait de sa case actuelle, en ignorant ce que la pr&eacute;c&eacute;dente
     * allait lui faire faire. Mesur&eacute; : ses deux coups visaient la m&ecirc;me case 94 fois
     * sur 100. Une file de deux copies n&rsquo;est pas une file, c&rsquo;est un coup coup&eacute;
     * en deux.
     *
     * <p>Depuis qu&rsquo;il projette sa position, 18 % des l&acirc;chers tiennent deux actions de
     * <b>natures diff&eacute;rentes</b>, et pr&egrave;s d&rsquo;une centaine sur onze cents sont
     * exactement &laquo; avancer, puis frapper &raquo;. Ce test tient la m&eacute;canique sur le
     * cas le plus net : un colosse &agrave; deux cases du h&eacute;ros doit annoncer un pas
     * <em>puis</em> une frappe, et non deux pas.
     */
    @Test
    @DisplayName("Un colosse a deux cases annonce un pas, puis la frappe qui suit")
    void aColossusTwoCellsAwayQueuesAStepThenTheBlow() {
        Arena arena = new Arena(11, 4);
        Enemy colossus = new Enemy(EnemyKind.COLOSSE);
        arena.grid().place(6, colossus);
        arena.announceIntentions();
        arena.announceIntentions();

        assertTrue(colossus.isPlanFull(), "montage invalide : sa file devait se remplir");
        assertEquals(Intention.Kind.ADVANCE, colossus.queued().get(0).kind(),
                "a deux cases, il commence par s'approcher : " + colossus.queued());
        assertEquals(Intention.Kind.ATTACK, colossus.queued().get(1).kind(),
                "puis il frappe DEPUIS la case ou son premier pas l'aura mene ; sans cette"
                        + " projection il annoncerait deux pas, et sa file ne dirait qu'une chose"
                        + " dite deux fois : " + colossus.queued());
    }
}
