package com.starfall.scene;

import com.starfall.game.ActionResult;
import com.starfall.game.Arena;

import java.util.List;
import java.util.function.Function;

/**
 * Un scénario de capture : une suite de gestes, et ce qui est survolé à chaque image.
 *
 * <h2>Pourquoi il en faut plus d'un</h2>
 *
 * <p>Le scénario principal est une <b>ligne gagnante</b> : il traverse les quatre vagues et finit
 * sur la bannière de victoire. C'est ce qu'il faut pour garder le jeu tel qu'on le joue — mais un
 * jeu qu'on joue bien ne montre jamais ce qui arrive quand on joue mal, ni les cas rares.
 *
 * <p>Plusieurs états leur ont échappé, tous relevés par des reviews : la <b>bannière de
 * défaite</b>, le mur d'une <b>poussée qui bute</b>, le filtre qui empêche de surligner une case
 * qu'un clic ne peut pas atteindre, la <b>reprise</b> d'une tuile — seul geste gratuit du jeu — et
 * la <b>fin de partie</b> avec une file encore garnie, sous ses deux formes de survol. Les allonger dans la
 * ligne gagnante était impossible : elle se termine par une victoire, et rien ne se joue après.
 *
 * <p>D'où cette abstraction, et une seconde implémentation — {@link ShowcaseScript} — dont le seul
 * métier est d'exhiber ce que la bonne ligne ne rencontre pas.
 */
public interface CaptureScenario {

    /** Le nom de scène qui sélectionne ce scénario. */
    String sceneName();

    /** Les gestes, dans l'ordre. L'image {@code n} montre l'état après les {@code n} premiers. */
    List<Function<Arena, ActionResult>> actions();

    /** Tuile du râtelier survolée à cette image, ou {@code -1}. */
    int rackSlotAt(int frameIndex);

    /** Emplacement de la file survolé à cette image, ou {@code -1}. */
    int queueSlotAt(int frameIndex);

    /** Case du plateau survolée à cette image, ou {@code -1}. */
    int cellAt(int frameIndex);

    /**
     * Rang du temps de déroulé à montrer sur cette image, à partir de 1, ou {@code 0} pour aucun.
     *
     * <p>Par défaut, aucun : une planche est un <b>état au repos</b>, et c'est ce qui la rend
     * reproductible. Un scénario peut pourtant vouloir capturer le déroulé lui-même — et il le
     * faut, parce que sans cela la règle qui fait taire les calques du repos pendant l'animation
     * <em>n'a aucun témoin</em>. Le mode capture ne déroulant jamais, les planches ne peuvent pas
     * l'attraper, et ce projet a appris ce que vaut un garde-fou qu'on croit sur parole.
     *
     * <p>Le déroulé reste déterministe : la scène l'avance d'un nombre <b>entier</b> de temps, pas
     * d'une durée écoulée. Une image montre le temps {@code n}, jamais un entre-deux.
     */
    default int playbackBeatAt(int frameIndex) {
        float[] moment = momentAt(frameIndex);
        return moment == null ? 0 : (int) moment[0];
    }

    /**
     * O&ugrave; en est ce temps-l&agrave;, de 0 &mdash; il commence &mdash; &agrave; 1 &mdash; il
     * est jou&eacute;.
     *
     * <p>Un temps ne saute plus d'une case &agrave; l'autre : il se d&eacute;roule, et une planche
     * peut donc le saisir <b>&agrave; mi-course</b>. C'est le seul moyen de garder par l'image ce
     * qu'une animation fait, puisqu'une planche prise &agrave; la fin de chaque temps montre
     * exactement ce que montrait le jeu quand il n'animait rien.
     *
     * <p>La valeur par d&eacute;faut est <b>1</b>, et ce choix n'est pas neutre : il fait que tous
     * les sc&eacute;narios &eacute;crits avant l'animation continuent de rendre, au pixel
     * pr&egrave;s, ce qu'ils rendaient. Un d&eacute;faut &agrave; z&eacute;ro aurait fait montrer
     * &agrave; chaque planche l'&eacute;tat d'AVANT le temps qu'elle annonce &mdash; un
     * d&eacute;calage d'un temps sur quatre-vingt-quinze planches, qu'il aurait fallu accepter sans
     * le comprendre.
     *
     * <p>La position est <b>pos&eacute;e</b> et non atteinte par accumulation : voir
     * {@link Playback#seek(int, float)}.
     */
    default float playbackProgressAt(int frameIndex) {
        float[] moment = momentAt(frameIndex);
        return moment == null ? 1f : moment[1];
    }

    /**
     * L'instant saisi par cette image, ou {@code null} si elle montre un &eacute;tat au repos.
     *
     * <p>Les images des gestes viennent d'abord, celles des instants ensuite. Le rejeu de la
     * sc&egrave;ne applique tous les gestes d&egrave;s que l'image d&eacute;passe leur nombre, si
     * bien que ces images-l&agrave; partent toutes du m&ecirc;me &eacute;tat final et ne
     * diff&egrave;rent que par l'instant montr&eacute; : c'est ce qui les rend reproductibles.
     */
    private float[] momentAt(int frameIndex) {
        float[][] moments = moments();
        int rank = frameIndex - size() - 1;
        return rank >= 0 && rank < moments.length ? moments[rank] : null;
    }

    /**
     * Numéro de la <b>dernière image</b> que ce scénario sait rendre.
     *
     * <p>Par défaut le nombre de gestes : l'image {@code n} montre l'état après les {@code n}
     * premiers, et rien au-delà. Mais un scénario qui <em>déroule</em> continue après son dernier
     * geste — ses images suivantes montrent les temps de la résolution, pas des gestes de plus.
     * La borne vient donc du scénario, et non d'un calcul fait chez l'appelant : c'est la même
     * raison qui avait fait descendre les longueurs ici plutôt que dans {@code LaunchOptions}.
     */
    default int lastFrame() {
        return size() + moments().length;
    }

    /**
     * Les <b>instants</b> saisis apr&egrave;s les gestes : le rang du temps, puis o&ugrave; il en
     * est.
     *
     * <p>Un sc&eacute;nario qui d&eacute;clare des instants gagne, sans rien &eacute;crire de plus,
     * la borne de ses images et les deux r&eacute;ponses qui vont avec &mdash; quel temps montrer,
     * et &agrave; quelle fraction de son d&eacute;roulement. Vide par d&eacute;faut : un
     * sc&eacute;nario qui ne d&eacute;clare rien montre des &eacute;tats au repos, comme avant que
     * ce jeu sache animer quoi que ce soit.
     *
     * <p>Cette r&egrave;gle a &eacute;t&eacute; &eacute;crite <b>deux fois</b> avant d'atterrir ici,
     * une fois par &eacute;cran de d&eacute;roul&eacute;, et les deux copies &eacute;taient
     * d&eacute;j&agrave; identiques au caract&egrave;re pr&egrave;s. Ce projet sait ce qu'il advient
     * d'une r&egrave;gle &eacute;crite &agrave; deux endroits ; il l'a pay&eacute; neuf fois. Le
     * troisi&egrave;me &eacute;cran de d&eacute;roul&eacute; n'aura qu'un tableau &agrave;
     * d&eacute;clarer.
     */
    default float[][] moments() {
        return new float[0][];
    }

    /** Nombre de gestes. La dernière image porte le numéro {@code size()}. */
    default int size() {
        return actions().size();
    }

    /** Rejoue les {@code count} premiers gestes sur une arène. */
    default ActionResult replayInto(Arena arena, int count) {
        ActionResult last = null;
        List<Function<Arena, ActionResult>> gestures = actions();
        for (int i = 0; i < count && i < gestures.size(); i++) {
            last = gestures.get(i).apply(arena);
        }
        return last;
    }

    /**
     * Le scénario d'une scène.
     *
     * <p>La résolution vit ici plutôt que dans la scène : c'est la seule règle qui lie un nom de
     * ligne de commande à un scénario, et la borne de {@code --from} en a besoin autant que le
     * rendu. Écrite à deux endroits, elle aurait divergé — ce projet en a l'habitude.
     */
    static CaptureScenario forScene(String scene) {
        if (ShowcaseScript.SCENE_NAME.equals(scene)) {
            return ShowcaseScript.SCENARIO;
        }
        if (RiposteScript.SCENE_NAME.equals(scene)) {
            return RiposteScript.SCENARIO;
        }
        if (SalvoScript.SCENE_NAME.equals(scene)) {
            return SalvoScript.SCENARIO;
        }
        if (CaptureScript.SCENE_NAME.equals(scene)) {
            return CaptureScript.SCENARIO;
        }
        // Elle rendait la ligne gagnante pour TOUT le reste, y compris « calibration », « banane »
        // et null. Son voisin StarfallGame.sceneFor fait l'inverse et son javadoc revendique le
        // principe : « un ajout de scène oublié ici échoue bruyamment plutôt que d'afficher la
        // mauvaise ». Deux tables de noms, deux disciplines opposées : c'est celle qui devine qui
        // avait tort.
        throw new IllegalArgumentException(
                "aucun scénario de capture pour la scène « " + scene + " »");
    }
}
