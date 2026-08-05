package com.starfall.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.starfall.game.Arena;
import com.starfall.game.ActionResult;
import com.starfall.game.ArenaSetup;
import com.starfall.game.Grid;
import com.starfall.game.Tile;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * L'instrument de mesure doit être aussi fiable que ce qu'il mesure.
 *
 * <p>Un bilan d'équilibrage faux est pire qu'aucun bilan : il donne des chiffres, donc il inspire
 * confiance, donc on règle le jeu dessus. Ces tests portent sur l'instrument lui-même — l'énumération
 * des gestes, la reproductibilité du rejeu, l'absence d'enlisement — et non sur les valeurs qu'il
 * produit, qui sont l'affaire du réglage.
 */
class PlayoutTest {

    /**
     * Le rejeu est la fondation de tout : essayer un geste consiste à rejouer la partie depuis son
     * début. S'il n'était pas déterministe, chaque évaluation porterait sur un autre jeu.
     */
    @Test
    @DisplayName("Rejouer la même histoire redonne exactement le même état")
    void replayingTheSameHistoryGivesTheSameState() {
        for (int width : new int[]{Grid.MIN_WIDTH, 9, Grid.MAX_WIDTH}) {
            // « pas gauche » et non « pas droite » : le heros regarde deja a droite au depart et
            // la case y est occupee, si bien que ce script rejouait depuis toujours un geste que le
            // jeu REFUSE - sur les trois largeurs, mesure. Il passait parce que l'enumeration
            // l'offrait quand meme et que le determinisme tient aussi pour un geste sans effet.
            // Un pas vers la gauche est un demi-tour, donc toujours accepte.
            List<String> history = new ArrayList<>(List.of(
                    "poser " + Tile.THRUST.label(),
                    "poser " + Tile.STRIKE.label(),
                    "pas gauche",
                    "salve",
                    "pas gauche"));

            String reference = signature(Playout.replay(width, 1, history));
            for (int repeat = 0; repeat < 4; repeat++) {
                assertEquals(reference, signature(Playout.replay(width, 1, history)),
                        "grille de " + width);
            }
        }
    }

    /**
     * Le refus <b>propre à chaque geste</b>, et pas un ensemble global.
     *
     * <p>Ma première version mettait {@code NO_TARGET} dans une liste unique de refus, et elle a
     * aussitôt accusé « salve » à tort : pour une salve, {@code NO_TARGET} n'est pas un refus, c'est
     * le <em>résultat</em> de sa dernière tuile — le tir est bien parti, il n'a rien trouvé. Le même
     * mot dit « je refuse » pour un échange et « c'était vide » pour une salve.
     *
     * <p>C'est le motif que ce projet appelle <i>un instrument incapable de porter sa conclusion</i>.
     * La question n'est pas « ce résultat est-il un refus quelque part », mais « ce geste-ci a-t-il
     * été refusé pour la raison que l'énumération devait écarter ».
     */
    private static java.util.Set<ActionResult> refusalsFor(String label) {
        if (label.startsWith("pas ")) {
            return java.util.EnumSet.of(ActionResult.BLOCKED);
        }
        if (label.startsWith("poser ")) {
            return java.util.EnumSet.of(ActionResult.QUEUE_FULL, ActionResult.NOT_READY);
        }
        if (label.startsWith("reprendre ")) {
            return java.util.EnumSet.of(ActionResult.BLOCKED);
        }
        if (label.equals("échange")) {
            return java.util.EnumSet.of(ActionResult.NO_TARGET);
        }
        if (label.equals("salve")) {
            return java.util.EnumSet.of(ActionResult.EMPTY_QUEUE);
        }
        throw new IllegalArgumentException("geste non classe : « " + label + " ». Ajoute-le ici"
                + " plutot que de le laisser tomber dans un fourre-tout : c'est exactement ce qui"
                + " avait rendu cette garde vide pour toute une famille de gestes.");
    }

    @Test
    @DisplayName("L'énumération des poses est exactement ce que l'arène accepterait")
    void theEnumerationOfPlacementsIsExactlyWhatTheArenaWouldAccept() {
        for (int wave = 1; wave <= Arena.WAVE_COUNT; wave++) {
            for (int seed = 0; seed < 40; seed++) {
                Arena arena = ArenaSetup.trainingArena(9, wave);
                java.util.Random random = new java.util.Random(seed);

                for (int step = 0; step < 25; step++) {
                    // Une seule enumeration par pas : la version precedente appelait Move.legal
                    // deux fois, une pour lire les libelles et une pour jouer. Move.legal est pure
                    // et l'arene ne bouge pas entre les deux, donc rien ne se cachait la - mais
                    // deux appels la ou un suffit invitent le lecteur a se demander lequel fait foi.
                    List<Move> moves = Move.legal(arena);
                    List<String> labels = moves.stream().map(Move::label).toList();
                    for (Tile tile : arena.rack().tiles()) {
                        boolean enumerated = labels.contains("poser " + tile.label());
                        assertEquals(arena.canQueue(tile), enumerated,
                                "vague " + wave + " graine " + seed + " : « poser " + tile.label()
                                        + " » est " + (enumerated ? "present dans" : "absent de")
                                        + " l'enumeration alors que canQueue rend "
                                        + arena.canQueue(tile));
                    }
                    if (moves.isEmpty()) {
                        break;
                    }
                    moves.get(random.nextInt(moves.size())).applyTo(arena);
                }
            }
        }
    }

    /**
     * Chaque geste énuméré doit être réellement jouable. Proposer à une politique des gestes que le
     * jeu refuse ferait compter des tours qui n'existent pas.
     */
    @Test
    @DisplayName("Tout geste énuméré est accepté par le jeu")
    void everyEnumeratedMoveIsAccepted() {
        for (int seed = 0; seed < 60; seed++) {
            Arena arena = ArenaSetup.trainingArena(9, 1);
            List<String> history = new ArrayList<>();
            java.util.Random random = new java.util.Random(seed);

            for (int step = 0; step < 25 && !arena.isOver(); step++) {
                List<Move> moves = Move.legal(arena);
                assertTrue(!moves.isEmpty(), "aucun geste legal, graine " + seed);

                Move move = moves.get(random.nextInt(moves.size()));
                // Le rejeu retrouve le geste par son libellé : s'il ne le retrouvait pas, toute
                // évaluation partirait d'un état faux sans que rien ne le signale.
                history.add(move.label());
                Playout.replay(9, 1, history);

                // Le titre de ce test dit « accepte par le jeu », et rien ne l'assertait : applyTo
                // rend un ActionResult que personne ne regardait. Or un geste enumere puis refuse
                // est exactement ce que legal() existe pour empecher — « les mesures compteraient
                // des tours qui n'existent pas », dit son propre javadoc. Trouve en corrigeant le
                // quatrieme endroit ou la regle etait recopiee.
                ActionResult result = move.applyTo(arena);
                assertTrue(!refusalsFor(move.label()).contains(result),
                        "graine " + seed + " : « " + move.label() + " » etait enumere comme legal"
                                + " et le jeu rend " + result);
                // Pour « reprendre », l'assertion ci-dessus ne peut pas echouer : unqueueAt ne
                // connait que deux sorties, et l'enumeration ne propose ce geste que lorsque
                // l'autre est certaine. Corriger le classement de cette famille - ce que le commit
                // precedent a fait - ne l'a donc pas rendue falsifiable. On asserte le resultat
                // POSITIF, qui lui peut echouer.
                if (move.label().startsWith("reprendre ")) {
                    assertEquals(ActionResult.UNQUEUED, result,
                            "graine " + seed + " : « " + move.label() + " » etait enumere et rend "
                                    + result + " au lieu de reprendre la tuile");
                }
            }
        }
    }

    /**
     * Aucune partie ne doit s'enliser <b>par la faute de l'instrument</b>.
     *
     * <p>C'est le test qui a le plus servi. La politique réfléchie a trouvé deux fois de suite un
     * moyen de ne jamais faire avancer l'horloge : d'abord en remuant sa file indéfiniment — poser
     * et reprendre sont gratuits — puis, une fois cela interdit, en répétant un <b>pas bloqué</b>,
     * qui n'est ni une pose ni une exécution gratuite mais ne coûte pas de tour non plus. Rien dans
     * les règles du jeu n'oblige le joueur à agir : c'est une propriété du jeu, pas un défaut de
     * l'instrument, mais la mesure doit en tenir compte.
     */
    @Test
    @DisplayName("Une partie mesurée finit toujours par consommer des tours")
    void ameasuredGameAlwaysBurnsTurns() {
        for (Policy policy : List.of(Policy.random(), Policy.direct(), Policy.thoughtful())) {
            for (int seed = 0; seed < 8; seed++) {
                Playout.Outcome outcome = Playout.play(policy, 9, 1, seed);
                assertTrue(outcome.turns() > 0,
                        policy.name() + " graine " + seed + " : aucun tour consomme");
                // Une partie qui atteint le plafond de gestes sans avoir consommé au moins un tour
                // tous les six gestes tourne en rond.
                assertTrue(outcome.turns() * 6 >= outcome.actions(),
                        policy.name() + " graine " + seed + " : " + outcome.actions()
                                + " gestes pour seulement " + outcome.turns() + " tours");
            }
        }
    }

    /**
     * Le plancher : si le hasard s'en sortait, le jeu ne demanderait rien. C'est la seule assertion
     * d'équilibre que cet instrument porte lui-même — le reste est du réglage, qui bouge.
     */
    @Test
    @DisplayName("Le hasard ne gagne jamais : il y a bien un jeu")
    void randomPlayNeverWins() {
        BalanceReport report = BalanceReport.measure(Policy.random(), 9, 1, 60);
        assertEquals(0, report.wins(), "le hasard gagne : le jeu ne demande rien");
        assertTrue(report.averageTurns() > 3,
                "le hasard meurt trop vite pour que la mesure dise quoi que ce soit : "
                        + report.averageTurns());
    }

    /**
     * Une partie enlisée n'est pas une partie, et le bilan ne doit pas la compter comme telle.
     *
     * <p>La review du jalon d'équilibrage l'a montré net : le garde-fou anti-enlisement
     * n'empêche pas de tourner en rond, il le <em>cadence</em> — cinq gestes gratuits, un geste
     * forcé, jusqu'au plafond. Une politique qui s'enlisait systématiquement affichait donc
     * « 78 tours survécus », ce qui se lit comme de la résistance alors que c'est le
     * garde-fou qu'on mesure. Sur la grille la plus large, 92 % des « parties » de la politique de
     * plafond n'en étaient pas.
     */
    @Test
    @DisplayName("Les parties enlisées sortent des moyennes et sont annoncées")
    void stalledGamesLeaveTheAveragesAndAreAnnounced() {
        // Configuration redecoupee apres que l'enumeration a cesse d'offrir les pas contre un
        // mur : la politique ne gaspille plus ces tours, s'enlise beaucoup moins, et l'ancienne
        // (largeur 15, 12 parties) est tombee a ZERO enlisement - le test ne mesurait plus rien,
        // et il le disait lui-meme. Mesure : largeur 6 donne 2 enlisements sur 12 parties et 5 sur
        // 40. On prend la seconde, pour une marge franche plutot qu'un fil.
        BalanceReport report = BalanceReport.measure(Policy.thoughtful(), 6, 1, 40);

        // On EXIGE que le cas annoncé soit visité, au lieu de le vérifier sous un « if ». Les deux
        // assertions qui portaient quelque chose étaient conditionnelles : un rééquilibrage qui
        // ferait gagner la politique une seule fois aurait désactivé la moitié du test en silence,
        // et « stalled >= 0 » ne dit rien du tout.
        assertTrue(report.stalled() > 0,
                "aucune partie enlisee sur cette configuration : le test ne mesure plus rien."
                        + " Si la politique s'est ameliorée, choisir une configuration ou elle"
                        + " s'enlise encore, ou retirer ce test.");
        assertTrue(report.wins() <= report.games() - report.stalled(),
                "plus de victoires que de parties reellement jouees : "
                        + report.wins() + " pour " + (report.games() - report.stalled()));
        assertTrue(report.line().contains("ENLISÉES"),
                "le bilan tait " + report.stalled() + " partie(s) enlisee(s) : " + report.line());
        assertEquals(0, report.wins(), "montage invalide : cette politique ne doit pas gagner ici");
        // « 0,0 » quand personne ne gagne se lirait comme « on gagne a zero point de vie ».
        assertTrue(report.line().contains("s.o."),
                "une marge de vie sans objet doit se dire, pas s'ecrire zero : " + report.line());
    }

    @Test
    @DisplayName("Le bilan se lit et se reproduit")
    void theReportIsReadableAndReproducible() {
        BalanceReport first = BalanceReport.measure(Policy.direct(), 9, 1, 20);
        BalanceReport again = BalanceReport.measure(Policy.direct(), 9, 1, 20);

        assertEquals(first, again, "mêmes graines, mêmes chiffres");
        assertTrue(first.line().contains("grille 9"), first.line());
        assertTrue(first.winRate() >= 0 && first.winRate() <= 1);
    }

    private static String signature(Arena arena) {
        StringBuilder state = new StringBuilder();
        for (int cell = 0; cell < arena.grid().width(); cell++) {
            var occupant = arena.grid().occupantAt(cell);
            state.append(occupant == null ? '.'
                    : occupant == arena.hero() ? 'H' : occupant.label().charAt(0));
        }
        return state.append('|').append(arena.turnsTaken())
                .append('|').append(arena.hero().health())
                .append('|').append(arena.wave())
                .append('|').append(arena.queue().size()).toString();
    }
}
