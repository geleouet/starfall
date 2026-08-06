package com.starfall.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Le seul test qui vérifie ce que le jalon promet vraiment : <b>ce qui est affiché est ce qui sera
 * joué</b>.
 *
 * <p>Il n'existait pas. Les autres tests vérifiaient les archétypes un par un et les invariants de
 * plateau, mais aucun ne confrontait l'annonce au résultat — si bien que le colosse a pu, une phase
 * sur deux, cercler de rouge une case qu'il ne frappait pas, sans qu'aucun test ne bronche. Un
 * archétype sur quatre mentait au joueur.
 *
 * <p>La méthode est simple et devrait le rester : avant chaque tour, on relève ce que l'interface
 * montrerait — le nombre de coups annoncés sur la case du héros — puis on joue le tour et on
 * compare aux coups réellement encaissés.
 */
class TelegraphTest {

    /** Obstacle inerte : un ennemi bougerait pendant la phase et fausserait la mesure. */
    private record Rock(String label, long id) implements Occupant {
        // Le numero est un COMPOSANT, et non un « return Identities.next() » dans
        // l'accesseur : ce dernier rendrait un numero neuf a chaque lecture, soit
        // l'exact contraire de ce qu'un numero d'identite promet.
        Rock(String label) {
            this(label, Identities.next());
        }

        @Override
        public String spriteName() {
            return "enemy/colosse";
        }
    }

    private static final int SEEDS = 300;
    private static final int TURNS_PER_SEED = 60;

    /**
     * Le t&eacute;l&eacute;graphe, en <b>points</b> et non plus seulement en coups.
     *
     * <p>Le test voisin garde la promesse centrale du jeu compt&eacute;e en <em>coups</em> : ce qui
     * est annonc&eacute; est exactement ce qui est jou&eacute;. C'&eacute;tait la promesse
     * enti&egrave;re tant que tout coup ennemi retirait un point.
     *
     * <p>Depuis l'axe de d&eacute;g&acirc;ts, ce n'est plus qu'une moiti&eacute;. La plaque
     * pos&eacute;e au-dessus de chaque ennemi affiche d&eacute;sormais un <b>nombre de points</b>,
     * et c'est sur lui que le joueur d&eacute;cide de rester ou de fuir. Un coup annonc&eacute;
     * juste qui retirerait deux points au lieu d'un tiendrait la promesse compt&eacute;e en coups
     * et la trahirait l&agrave; o&ugrave; elle se lit. Ce test-ci garde l'autre moiti&eacute;.
     */
    @Test
    @DisplayName("Les points annoncés sur la case du héros sont exactement ceux qu'il perd")
    void announcedDamageIsExactlyTheDamageTaken() {
        List<String> failures = new ArrayList<>();
        Set<Integer> announcedValues = new TreeSet<>();

        for (int startWave = 1; startWave <= Arena.WAVE_COUNT; startWave++) {
            for (int seed = 0; seed < SEEDS; seed++) {
                Random random = new Random(seed);
                Arena arena = ArenaSetup.trainingArena(
                        Grid.MIN_WIDTH + random.nextInt(Grid.MAX_WIDTH - Grid.MIN_WIDTH + 1),
                        startWave);

                for (int turn = 0; turn < TURNS_PER_SEED && !arena.isOver()
                        && !arena.enemies().isEmpty(); turn++) {
                    int heroCell = arena.heroCell();
                    int announced = arena.threatDamage(heroCell);
                    int healthBefore = arena.hero().health();
                    int turnsBefore = arena.turnsTaken();

                    // Le demi-tour, pour la meme raison que le test voisin : le seul geste qui
                    // consomme un tour sans deplacer le heros ni toucher a la grille. Il reste
                    // donc exactement sur la case dont on vient de lire le prix.
                    arena.step(arena.hero().facing().opposite());
                    if (arena.turnsTaken() == turnsBefore) {
                        continue;
                    }
                    // LE COUP QUI TUE NE SE MESURE PAS EN POINTS PERDUS. La sante se borne a zero :
                    // un heros a un point qui en encaisse deux n'en perd qu'un, et la promesse
                    // paraitrait trahie alors qu'elle est tenue. Le test voisin y echappe sans y
                    // penser parce qu'il compte des COUPS, que rien ne borne. Mesure : c'est la
                    // seule forme d'ecart que cet echantillon produisait, toutes a « pv 1 -> 0 ».
                    if (arena.hero().health() == 0) {
                        continue;
                    }
                    announcedValues.add(announced);

                    int lost = healthBefore - arena.hero().health();
                    if (lost != announced) {
                        failures.add("graine " + seed + " vague " + startWave + " tour " + turn
                                + " : " + announced + " point(s) annonce(s) sur la case "
                                + (heroCell + 1) + ", " + lost + " perdu(s), pv " + healthBefore
                                + " -> " + arena.hero().health());
                    }
                    if (failures.size() > 5) {
                        break;
                    }
                }
            }
        }

        assertTrue(failures.isEmpty(),
                "la plaque ment sur ce qu'elle coute :\n  " + String.join("\n  ", failures));

        // PREMIERE PREMISSE : l'echantillon n'est pas degenere. Si threatDamage rendait toujours
        // zero, ou si aucune case menacee n'etait jamais occupee, la comparaison ci-dessus serait
        // vraie sans rien avoir eprouve. Elle ne dit QUE cela - voir juste en dessous pourquoi la
        // formulation d'origine en promettait davantage.
        assertTrue(announcedValues.size() >= 3,
                "l'echantillon n'a vu que " + announcedValues + " comme prix annonces : il est"
                        + " degenere, et la comparaison ci-dessus ne porte plus sur grand-chose");

        // ET L'AXE LUI-MEME. L'assertion ci-dessus ne le garde PAS, contrairement a ce que sa
        // premiere version pretendait : ramener les cinq archetypes a un point laisse encore
        // plusieurs totaux distincts, parce qu'un ennemi rapide frappe deux fois et que deux
        // ennemis peuvent viser la meme case. Mesure : la mutation qui aplatit l'axe ne faisait
        // pas rougir ce fichier. Un titre qui promet plus que son corps n'assere est le defaut
        // que ce projet releve le plus souvent ; il vaut aussi pour les premisses.
        assertTrue(Arrays.stream(EnemyKind.values()).map(EnemyKind::damage).distinct().count() >= 2,
                "tous les archetypes retirent le meme nombre de points : l'axe de degats ennemi a"
                        + " ete aplati, et rien d'autre dans cette suite ne le dirait");
    }

    @Test
    @DisplayName("Les coups annoncés sur la case du héros sont exactement ceux qu'il encaisse")
    void announcedBlowsAreExactlyTheBlowsTaken() {
        List<String> failures = new ArrayList<>();
        Map<EnemyKind, Integer> sampled = new EnumMap<>(EnemyKind.class);
        Map<Trait, Integer> traits = new EnumMap<>(Trait.class);

        // Toutes les vagues. Le montage precedent ne demarrait qu'en vague 1 et, mesure sur cinq
        // cents parties, n'en sortait jamais : deux archetypes sur cinq, jamais de ruee, jamais
        // d'invocation, et la charge dans 0,2 % des observations. Or ce test porte sur la promesse
        // centrale du jeu - ce qui est annonce est exactement ce qui est joue - et il ne l'eprouvait
        // que sur les deux archetypes qui frappent d'une case.
        for (int startWave = 1; startWave <= Arena.WAVE_COUNT; startWave++) {
            for (int seed = 0; seed < SEEDS; seed++) {
                Random random = new Random(seed);
                Arena arena = ArenaSetup.trainingArena(
                        Grid.MIN_WIDTH + random.nextInt(Grid.MAX_WIDTH - Grid.MIN_WIDTH + 1),
                        startWave);

                // « !isOver() » et pas seulement « il reste des ennemis » : apres la mort du
                // heros le plateau est GELE, et cette boucle continuait d'y tourner jusqu'a
                // soixante tours en nourrissant le compteur de couverture.
                for (int turn = 0; turn < TURNS_PER_SEED && !arena.isOver()
                        && !arena.enemies().isEmpty(); turn++) {
                    // Les especes presentes AVANT le geste : ce sont elles qui ont annonce, et donc
                    // elles que la comparaison qui suit met a l'epreuve. On ne les compte qu'une
                    // fois le tour reellement consomme.
                    List<Enemy> facing = List.copyOf(arena.enemies());
                    int heroCell = arena.heroCell();
                    int announced = arena.threatCount(heroCell);
                    int hitsBefore = arena.heroHits();
                    int turnsBefore = arena.turnsTaken();

                    // Un demi-tour : la seule action qui consomme un tour sans déplacer le héros ni
                    // toucher à la grille. Le héros reste donc exactement sur la case menacée.
                    arena.step(arena.hero().facing().opposite());
                    if (arena.turnsTaken() == turnsBefore) {
                        continue; // aucun tour consommé : aucune phase ennemie à vérifier
                    }
                    // APRES le « continue », et c'est tout l'interet. Place avant, le compteur
                    // creditait des tours ou rien n'etait verifie - la seizieme review l'a mesure :
                    // 5 671 tours verifies contre 66 329 sautes, et des totaux de 300 x 60 tout
                    // ronds, signature d'un compteur qui tourne sur un plateau gele. Un compteur de
                    // couverture qui compte ce qui n'est pas eprouve ne mesure pas la couverture.
                    facing.forEach(enemy -> {
                        sampled.merge(enemy.kind(), 1, Integer::sum);
                        for (Trait trait : Trait.values()) {
                            if (enemy.has(trait)) {
                                traits.merge(trait, 1, Integer::sum);
                            }
                        }
                    });

                    int taken = arena.heroHits() - hitsBefore;
                    if (taken != announced) {
                        failures.add("graine " + seed + " tour " + turn + " : " + announced
                                + " coup(s) annonce(s) sur la case " + (heroCell + 1)
                                + ", " + taken + " encaisse(s)");
                    }
                    if (failures.size() > 5) {
                        break;
                    }
                }
            }
        }

        assertTrue(failures.isEmpty(), "le telegraphe ment :\n  " + String.join("\n  ", failures));
        // Et l'echantillon dit quels archetypes il a vus. Le telegraphe est la promesse que ce jeu
        // fait au joueur ; une promesse eprouvee sur deux especes sur cinq n'est eprouvee qu'a
        // deux cinquiemes, et rien ne le disait.
        for (EnemyKind kind : EnemyKind.values()) {
            assertTrue(sampled.getOrDefault(kind, 0) > 0,
                    "l'archetype " + kind + " n'apparait pas une seule fois dans l'echantillon :"
                            + " le telegraphe n'est pas eprouve contre lui. Vus : " + sampled);
        }
        // Le second axe : les traits. CE QUI SUIT EST UN CONTROLE DE COMPOSITION, PAS UNE
        // EPREUVE, et les distinguer est tout l'objet de cette version.
        //
        // J'avais d'abord ecrit « le telegraphe n'est pas eprouve contre ce trait » pour les
        // quatre, puis retire EXPLOSIF au motif que son unique effet est a la mort et que cet
        // echantillon ne tue jamais. Le critere etait juste ; je ne l'avais mesure que sur le
        // trait que je retirais. Une review l'a applique aux trois restants : neutraliser AGRESSIF,
        // puis FONCEUR, laisse les DIX tests de ce fichier verts. La raison est structurelle et
        // vaut pour tous - ce test compare l'annonce a la resolution, deux sorties du MEME cerveau,
        // et changer ce qu'un trait fait deplace les deux ensemble.
        //
        // Compter les traits ne dit donc pas que la promesse est eprouvee contre eux. Cela dit que
        // l'echantillon les CONTIENT, ce qui reste utile : un echantillon qui les perdrait
        // n'eprouverait plus rien du tout, et c'est exactement ce qui etait arrive aux archetypes.
        // Les effets, eux, sont gardes par EnemyTest > Traits, ou chacun des quatre rougit sous
        // mutation - EXPLOSIF compris, ce qui est aussi pourquoi il revient dans cette liste.
        for (Trait trait : Trait.values()) {
            assertTrue(traits.getOrDefault(trait, 0) > 0,
                    "le trait " + trait + " est absent de l'echantillon : les intentions comparees"
                            + " ici ne viennent plus que d'ennemis sans traits. Vus : " + traits);
        }
    }

    @Test
    @DisplayName("Aucun coup n'est encaissé sans avoir été annoncé, même en bougeant")
    void noBlowLandsWithoutHavingBeenAnnounced() {
        int blows = 0;
        int pushers = 0;

        for (int seed = 0; seed < SEEDS; seed++) {
            Random random = new Random(seed);
            Arena arena = ArenaSetup.trainingArena(
                    Grid.MIN_WIDTH + random.nextInt(Grid.MAX_WIDTH - Grid.MIN_WIDTH + 1));

            for (int turn = 0; turn < TURNS_PER_SEED; turn++) {
                // La premisse de tout ce qui suit, et elle n'etait ecrite nulle part : cet
                // instrument lit la menace de la case d'ARRIVEE. Elle n'est la case ou les coups
                // sont tombes que si RIEN n'a deplace le heros pendant la phase ennemie. Voir le
                // javadoc : mesure a l'appui, des qu'une ruee existe cette assertion accuse le jeu
                // a tort. On garde donc la premisse au lieu de l'esperer.
                for (Enemy enemy : arena.enemies()) {
                    if (enemy.intention().kind() != Intention.Kind.RUSH) {
                        continue;
                    }
                    pushers++;
                    // ICI et pas a la fin du test : l'assertion qu'elle protege se declenche
                    // dans la boucle, et elle accuserait le jeu AVANT qu'on ait pu expliquer
                    // pourquoi elle a tort. Une garde de premisse qui ne parle qu'apres coup
                    // laisse le mauvais diagnostic sortir en premier.
                    assertEquals(0, pushers,
                            "une ruee est annoncee dans l'echantillon. Cet instrument lit la case"
                                    + " d'ARRIVEE du heros, qui n'est celle ou les coups sont"
                                    + " tombes que si rien ne l'a deplace. Une ruee le pousse"
                                    + " APRES l'avoir frappe sur la case annoncee : l'assertion"
                                    + " suivante accuserait le jeu a tort. Ce n'est pas elle qu'il"
                                    + " faut relacher, c'est l'instrument qu'il faut refaire :"
                                    + " voir le javadoc, qui donne la trace du cas");
                }
                // On relève toutes les cases menacées AVANT d'agir, puis on agit librement.
                boolean[] threatened = new boolean[arena.grid().width()];
                for (int cell = 0; cell < threatened.length; cell++) {
                    threatened[cell] = arena.isThreatened(cell);
                }
                int hitsBefore = arena.heroHits();
                int turnsBefore = arena.turnsTaken();

                arena.step(random.nextBoolean() ? Direction.LEFT : Direction.RIGHT);
                if (arena.turnsTaken() == turnsBefore) {
                    continue;
                }

                if (arena.heroHits() > hitsBefore) {
                    blows++;
                    // Le héros a été touché : il devait se trouver sur une case annoncée, soit
                    // celle qu'il occupait avant de bouger, soit celle où il est arrivé.
                    assertTrue(threatened[arena.heroCell()],
                            "graine " + seed + " tour " + turn
                                    + " : coup encaisse sur la case " + (arena.heroCell() + 1)
                                    + ", qui n'etait pas annoncee");
                }
            }
        }

        // « Aucun coup ne tombe sans avoir ete annonce » se verifie coup par coup : zero coup
        // encaisse, zero verification. Le compte dit que la phrase a ete eprouvee, pas seulement
        // ecrite.
        assertTrue(blows > 0,
                "le heros n'a encaisse aucun coup en " + SEEDS + " parties : rien de ce que ce"
                        + " test affirme n'a ete verifie");
    }

    /**
     * L'archétype par lequel le défaut est passé. Un test dédié en plus du fuzz : quand une règle a
     * déjà été violée une fois, elle mérite d'être épinglée par son nom.
     */
    @Test
    @DisplayName("Le colosse n'annonce rien quand il n'agira pas")
    void theColosseAnnouncesNothingWhenItWillNotAct() {
        Arena arena = new Arena(9, 4);
        arena.grid().place(5, new Enemy(EnemyKind.COLOSSE));
        arena.announceIntentions();

        for (int turn = 0; turn < 8; turn++) {
            int announced = arena.threatCount(arena.heroCell());
            int before = arena.heroHits();

            arena.step(arena.hero().facing().opposite());

            assertEquals(announced, arena.heroHits() - before,
                    "tour " + turn + " : " + announced + " annonce(s), "
                            + (arena.heroHits() - before) + " encaisse(s)");
        }
        assertTrue(arena.heroHits() > 0, "le colosse doit tout de meme frapper de temps en temps");
    }

    /**
     * L'invariant par lequel M7 avait rouvert le défaut de M6 : une intention qui réclame une case
     * du couloir d'une charge déjà annoncée l'intercepte, si bien que le coup promis au joueur ne
     * tombe jamais. Le télégraphe sur-promet alors — on peut dépenser un tour pour esquiver un coup
     * qui n'allait pas partir.
     *
     * <h2>Ce que ce test affirmait, et ce qu'il gardait</h2>
     *
     * <p>Il portait sur du jeu aléatoire, qui <b>ne quitte jamais la vague 1</b> : mesuré, seuls le
     * sabreur et l'archer y paraissent, et aucune charge n'a jamais été annoncée en trois cents
     * parties. Le triple filtre — une charge, un camarade, un couloir non vide — n'a jamais été
     * franchi une seule fois. C'est la même racine que le corpus d'interface engendré par jeu
     * aléatoire : <b>l'échantillon exclut structurellement l'espèce qu'on prétend éprouver</b>.
     *
     * <p>Le test ne regardait par ailleurs que les intentions {@code ADVANCE}, alors que sa propre
     * phrase dit « aucune <em>autre intention</em> ». Les deux bras y sont désormais, et
     * <b>les deux portent</b> — c'est le second point de cette histoire, et il m'a coûté une
     * affirmation de trop.
     *
     * <h2>Ce que j'avais écrit ici, et qui était faux</h2>
     *
     * <p>J'avais écrit qu'une avance <em>ne peut pas</em> viser un couloir, « pour l'atteindre il
     * lui faudrait traverser le chargeur ou le héros, et {@code isPathClear} le lui interdit
     * déjà », et j'en avais conclu que ce bras était <b>vrai par construction</b>. C'est faux, et le
     * contre-exemple sort à la <b>troisième graine</b> de ce montage : une avance n'a pas besoin de
     * traverser qui que ce soit pour entrer dans un couloir, il lui suffit d'y être déjà voisine.
     * Réservation de couloir retirée, bras {@code ADVANCE} seul : « graine 3 tour 2 : sabreur
     * réclame la case 6, dans le couloir de charge 8 vers 5 ».
     *
     * <p>Ma mesure disait pourtant zéro. Elle disait zéro <b>sur un autre montage</b> — un
     * échantillon qui ne garantissait pas la présence conjointe des deux espèces — et j'ai
     * généralisé de celui-là à celui-ci. C'est, mot pour mot, la faute que ce projet a déjà payée
     * deux fois : <b>une démonstration ne vaut que pour la population qu'elle a examinée</b>.
     *
     * <p>L'invocation, elle, porte aussi : le souverain choisit sa case adjacente au héros sans
     * regarder les couloirs, et c'est là qu'il se pose au milieu d'une charge. Le test compte
     * désormais <b>chaque bras séparément</b> — les fusionner laissait le bras vivant masquer la
     * disparition de l'autre.
     */
    @Test
    @DisplayName("Aucune intention ne vise une case réservée par une charge annoncée")
    void noIntentionEverTargetsAnAnnouncedChargeCorridor() {
        int advanceCells = 0;
        int summonCells = 0;
        int giveUps = 0;

        for (int seed = 0; seed < SEEDS; seed++) {
            Random random = new Random(seed);
            int width = Grid.MIN_WIDTH + random.nextInt(Grid.MAX_WIDTH - Grid.MIN_WIDTH + 1);
            Arena arena = new Arena(width, random.nextInt(width));
            // L'espece qui charge et l'espece qui invoque, toutes deux absentes de la vague 1 : sans
            // elles ce test ne franchit jamais son premier filtre.
            giveUps += placeSomewhere(arena, EnemyKind.LANCIER, random) ? 0 : 1;
            giveUps += placeSomewhere(arena, EnemyKind.SOUVERAIN, random) ? 0 : 1;
            giveUps += placeSomewhere(arena, EnemyKind.SABREUR, random) ? 0 : 1;
            arena.announceIntentions();

            for (int turn = 0; turn < TURNS_PER_SEED && !arena.isOver(); turn++) {
                for (Enemy charger : arena.enemies()) {
                    Intention.Kind kind = charger.intention().kind();
                    if (kind != Intention.Kind.CHARGE && kind != Intention.Kind.RUSH) {
                        continue;
                    }
                    int from = arena.grid().indexOf(charger);
                    int to = charger.intention().targetCell();
                    int step = Integer.signum(to - from);
                    if (step == 0) {
                        continue;
                    }

                    for (Enemy other : arena.enemies()) {
                        Intention.Kind claim = other.intention().kind();
                        if (other == charger
                                || (claim != Intention.Kind.ADVANCE
                                        && claim != Intention.Kind.SUMMON)) {
                            continue;
                        }
                        int destination = other.intention().targetCell();
                        for (int cell = from; cell != to; cell += step) {
                            if (claim == Intention.Kind.ADVANCE) {
                                advanceCells++;
                            } else {
                                summonCells++;
                            }
                            assertTrue(destination != cell,
                                    "graine " + seed + " tour " + turn + " : " + other.label()
                                            + " reclame la case " + (destination + 1)
                                            + ", dans le couloir de charge " + (from + 1)
                                            + " vers " + (to + 1));
                        }
                    }
                }
                arena.step(random.nextBoolean() ? Direction.LEFT : Direction.RIGHT);
            }
        }

        // UN COMPTE PAR BRAS, et c'est tout l'interet. La version precedente les fusionnait : le
        // bras ADVANCE, quatre fois plus fourni, satisfaisait a lui seul le total, si bien que
        // retirer du montage l'espece qui invoque - une ligne - laissait la suite entierement
        // verte. Un compteur qui ne peut pas detecter la disparition de ce qu'il compte fabrique
        // une assurance au lieu d'en donner une.
        assertTrue(advanceCells > 0,
                "aucune case de couloir examinee face a une avance : le montage ne produit plus de"
                        + " charge annoncee avec un camarade en approche");
        assertTrue(summonCells > 0,
                "aucune case de couloir examinee face a une invocation : l'espece qui invoque a"
                        + " disparu du montage, et c'est le bras dont on a mesure qu'il porte le"
                        + " plus : reservation de couloir retiree, les deux bras produisent des"
                        + " violations et celui-ci nettement davantage - reproductible par cette"
                        + " mutation, contrairement au chiffre que ce message donnait avant");
        // Et le renoncement silencieux de placeSomewhere : six essais, puis rien, sans un mot.
        assertEquals(0, giveUps,
                giveUps + " ennemi(s) n'ont pas pu etre poses : le montage est plus pauvre qu'il"
                        + " n'en a l'air, et personne ne le disait");
    }

    /**
     * Pose un ennemi sur une case libre au hasard.
     *
     * @return {@code false} si les six essais ont échoué — un renoncement silencieux appauvrit le
     *     montage sans que rien ne le signale, et l'appelant en tient le compte
     */
    private static boolean placeSomewhere(Arena arena, EnemyKind kind, Random random) {
        // Depart au hasard, puis balayage complet : six tirages a l'aveugle renoncaient deux fois
        // sur neuf cents sur les grilles etroites, sans un mot. Ici on ne renonce que s'il n'y a
        // reellement plus une seule case libre, ce qui est une information et non un hasard.
        int width = arena.grid().width();
        int start = random.nextInt(width);
        for (int offset = 0; offset < width; offset++) {
            int cell = (start + offset) % width;
            if (arena.grid().isFree(cell)) {
                arena.grid().place(cell, new Enemy(kind));
                return true;
            }
        }
        return false;
    }

    @Test
    @DisplayName("Une charge interceptée s'arrête et ne frappe pas")
    void anInterceptedChargeStopsAndDoesNotStrike() {
        Arena arena = new Arena(15, 0);
        Enemy lancier = new Enemy(EnemyKind.LANCIER);
        arena.grid().place(14, lancier);
        arena.announceIntentions();

        arena.step(Direction.LEFT);  // demi-tour : le lancier prend son élan
        assertEquals(Intention.Kind.CHARGE, lancier.intention().kind());

        // On s'interpose : un occupant entre le lancier et sa cible.
        arena.grid().place(7, new Rock("obstacle"));
        int hitsBefore = arena.heroHits();

        arena.step(Direction.RIGHT); // demi-tour : la charge part

        assertEquals(hitsBefore, arena.heroHits(), "une charge interceptee ne frappe pas");
        assertEquals(8, arena.grid().indexOf(lancier), "elle s'arrete juste avant l'obstacle");
    }

    @Test
    @DisplayName("Une charge ne traverse jamais personne")
    void aChargeNeverWalksThroughAnyone() {
        for (int blocker = 2; blocker <= 12; blocker++) {
            Arena arena = new Arena(15, 0);
            Enemy lancier = new Enemy(EnemyKind.LANCIER);
            arena.grid().place(14, lancier);
            arena.grid().place(blocker, new Rock("obstacle"));
            arena.announceIntentions();

            arena.step(Direction.LEFT);
            arena.step(Direction.RIGHT);

            int landed = arena.grid().indexOf(lancier);
            assertTrue(landed > blocker,
                    "le lancier a traverse l'obstacle de la case " + (blocker + 1)
                            + " : il est en " + (landed + 1));
        }
    }

    @Test
    @DisplayName("Un lancier rapide porte bien deux coups en chargeant")
    void aFastLancierStrikesTwiceWhenCharging() {
        Arena arena = new Arena(9, 0);
        arena.grid().place(5, new Enemy(EnemyKind.LANCIER, Trait.RAPIDE));
        arena.announceIntentions();

        arena.step(Direction.LEFT);  // élan
        int announced = arena.threatCount(arena.heroCell());
        int before = arena.heroHits();
        arena.step(Direction.RIGHT); // charge

        assertEquals(2, announced, "deux coups doivent etre annonces");
        assertEquals(2, arena.heroHits() - before, "et deux coups doivent tomber");
    }

    @Test
    @DisplayName("Deux ennemis qui visent la même case annoncent deux coups")
    void twoEnemiesAimingAtTheSameCellAnnounceTwoBlows() {
        Arena arena = new Arena(9, 4);
        arena.grid().place(3, new Enemy(EnemyKind.SABREUR));
        arena.grid().place(5, new Enemy(EnemyKind.SABREUR));
        arena.announceIntentions();

        assertEquals(2, arena.threatCount(4));

        int before = arena.heroHits();
        arena.step(arena.hero().facing().opposite());
        assertEquals(2, arena.heroHits() - before);
    }

    @Test
    @DisplayName("Les glyphes d'intention tiennent sous le bandeau d'interface")
    void theIntentionGlyphsFitBelowTheOverlay() {
        // Le bandeau se borne au contenu déclaré par la scène ; si ce contenu oublie les glyphes,
        // il les recouvre — ce qui était le cas, à certaines tailles de fenêtre seulement.
        assertTrue(ArenaLayout.INTENT_Y > ArenaLayout.FIGURE_Y + ArenaLayout.FIGURE_HEIGHT,
                "les glyphes doivent etre au-dessus des tetes");
        assertTrue(ArenaLayout.INTENT_TOP < com.starfall.StarfallGame.MIN_WORLD_HEIGHT,
                "les glyphes doivent tenir dans la zone garantie");
        assertEquals(ArenaLayout.INTENT_Y + ArenaLayout.INTENT_HEIGHT, ArenaLayout.INTENT_TOP);

        // Les points de vie ont leur propre bande : ils etaient d'abord poses sur la ligne des
        // barres de menace, deux informations vitales au meme pixel.
        assertTrue(ArenaLayout.HEALTH_Y + ArenaLayout.HEALTH_HEIGHT <= ArenaLayout.INTENT_Y,
                "la bande de vie mord sur celle des intentions");
        assertTrue(ArenaLayout.HEALTH_Y > ArenaLayout.GROUND_Y + ArenaLayout.GROUND_HEIGHT,
                "la bande de vie mord sur les dalles et donc sur les barres de menace");
    }

    @Test
    @DisplayName("La partie reste reproductible")
    void theGameStaysReproducible() {
        for (int width = Grid.MIN_WIDTH; width <= Grid.MAX_WIDTH; width++) {
            String reference = play(width);
            for (int repeat = 0; repeat < 3; repeat++) {
                assertEquals(reference, play(width), "grille de " + width);
            }
        }
    }

    private static String play(int width) {
        Arena arena = ArenaSetup.trainingArena(width);
        Random random = new Random(7);
        for (int turn = 0; turn < 40; turn++) {
            arena.step(random.nextBoolean() ? Direction.LEFT : Direction.RIGHT);
        }
        StringBuilder state = new StringBuilder();
        for (int cell = 0; cell < arena.grid().width(); cell++) {
            Occupant occupant = arena.grid().occupantAt(cell);
            state.append(occupant == null ? '.'
                    : occupant == arena.hero() ? 'H' : occupant.label().charAt(0));
        }
        return state.append('|').append(arena.heroHits())
                .append('|').append(arena.turnsTaken()).toString();
    }
}
