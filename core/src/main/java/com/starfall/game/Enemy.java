package com.starfall.game;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Un ennemi : un {@link EnemyKind archétype}, des {@link Trait traits}, une orientation, et
 * l'{@link Intention} qu'il a annoncée.
 *
 * <p>Comme le héros, il ne connaît pas sa position : elle appartient à la {@link Grid}.
 */
public final class Enemy implements Occupant {

    private final long id = Identities.next();

    private final EnemyKind kind;
    private final Set<Trait> traits;

    @Override
    public long id() {
        return id;
    }

    private int health;
    /** Étourdi : il passera sa prochaine activation. */
    private boolean stunned;

    private Direction facing = Direction.LEFT;
    private Intention intention = Intention.of(Intention.Kind.WAIT);
    /** Vrai quand un lancier a pris son élan et chargera à sa prochaine activation. */
    private boolean windingUp;
    /** Invocations qu'il lui reste. Zéro pour tout le monde sauf le souverain. */
    private int summonsLeft;
    /** Celui qui l'a fait apparaître, ou {@code null} s'il est arrivé avec sa vague. */
    private Enemy summoner;

    public Enemy(EnemyKind kind, Trait... traits) {
        this.kind = kind;
        this.health = kind.health();
        this.summonsLeft = kind.summons();
        this.traits = traits.length == 0
                ? Collections.emptySet()
                : Collections.unmodifiableSet(EnumSet.of(traits[0], traits));
    }

    public EnemyKind kind() {
        return kind;
    }

    /**
     * Celui qui l'a fait apparaître, ou {@code null} s'il est arrivé avec sa vague.
     *
     * <p>La source fait de ce lien une règle systémique : « les invocations disparaissent
     * instantanément à la mort du leader, économisant ainsi des dizaines de tours d'action
     * potentiellement mortels ». C'est ce qui rend viable la stratégie « ignorer les sbires et
     * saturer le chef » — sans ce lien, la seule ligne jouable est de tout nettoyer, et la
     * rencontre perd un de ses deux axes.
     */
    Enemy summoner() {
        return summoner;
    }

    void summonedBy(Enemy leader) {
        this.summoner = leader;
    }

    public Set<Trait> traits() {
        return traits;
    }

    public boolean has(Trait trait) {
        return traits.contains(trait);
    }

    public Direction facing() {
        return facing;
    }

    void face(Direction direction) {
        this.facing = direction;
    }

    /** Ce que cet ennemi fera à sa prochaine activation. Visible par le joueur. */
    public Intention intention() {
        return intention;
    }

    void announce(Intention intention) {
        this.intention = intention;
    }

    boolean isWindingUp() {
        return windingUp;
    }

    void setWindingUp(boolean windingUp) {
        this.windingUp = windingUp;
    }

    /** Points de vie restants. À zéro, l'ennemi meurt. */
    public int health() {
        return health;
    }

    public int maxHealth() {
        return kind.health();
    }

    /** Retire des points de vie. Renvoie vrai si l'ennemi vient de tomber. */
    boolean damage(int amount) {
        health = Math.max(0, health - amount);
        return health == 0;
    }

    /** Vrai s'il passera sa prochaine activation. */
    public boolean isStunned() {
        return stunned;
    }

    void setStunned(boolean stunned) {
        this.stunned = stunned;
    }

    /** Nombre de coups portés par une frappe. Un ennemi rapide en porte deux. */
    int strikesPerAttack() {
        return has(Trait.RAPIDE) ? 2 : 1;
    }

    /**
     * Distance à laquelle cet ennemi frappe, trait <i>agressif</i> compris.
     *
     * <p>Publique parce que l'interface doit pouvoir la dire. M8 donne une infobulle aux tuiles au
     * motif que « l'estoc porte à deux cases et rien ne le disait » ; c'était mot pour mot vrai de
     * l'archer, du lancier et du trait agressif — sauf qu'on l'apprenait en prenant des coups
     * plutôt qu'en gâchant des tuiles. Elle délègue au cerveau plutôt que de recalculer : la portée
     * affichée est celle qui décide de l'intention.
     */
    public int reach() {
        return EnemyBrain.effectiveRange(this);
    }

    /** Nombre de coups qu'une de ses frappes portera. Sert à l'infobulle. */
    public int blowsPerAttack() {
        return strikesPerAttack();
    }

    /**
     * Ce que cet ennemi a mis dans sa file, du plus proche au plus lointain.
     *
     * <p>Un ennemi n'annonce aujourd'hui qu'<b>une</b> action &agrave; la fois &mdash; sauf le
     * lancier, qui en tient deux : il prend son &eacute;lan, puis il charge. Cette suite existait
     * dans le mod&egrave;le depuis M6, cach&eacute;e dans un bool&eacute;en, et rien &agrave;
     * l'&eacute;cran ne la montrait : on voyait « il se charge », jamais « il se charge <em>pour
     * une charge</em> ». Le joueur apprenait ce qui venait en le prenant.
     *
     * <p>La rendre lisible demande de la <b>nommer</b>, et c'est ce que fait cette liste. Elle en
     * contient une pour tout le monde et deux pour un lancier qui prend son &eacute;lan ; c'est le
     * premier pas d'une file ennemie plus large, et elle est d&eacute;j&agrave; exacte pour ce
     * qu'elle couvre.
     */
    public List<Intention> plan() {
        if (intention.kind() == Intention.Kind.WIND_UP) {
            // Ce qui suit l'elan est connu d'avance et sans condition : le lancier « tient sa
            // promesse, quoi qu'il arrive ». La case visee, elle, ne l'est pas - elle sera
            // recalculee au moment de charger - donc on annonce la NATURE sans promettre l'endroit.
            return List.of(intention, Intention.of(Intention.Kind.CHARGE));
        }
        return List.of(intention);
    }

    /** Points de vie qu'un seul de ses coups retire. */
    public int blowDamage() {
        return kind.damage();
    }

    /**
     * Points de vie que sa frappe annoncée retirera <b>en tout</b>.
     *
     * <p>Le produit des deux axes : combien de coups, et combien chacun coûte. « Deux coups » et
     * « deux points » ne sont la même chose que chez le sabreur.
     */
    public int announcedDamage() {
        return kind.damage() * strikesPerAttack();
    }

    /** Invocations restantes. L'interface les affiche : un compte à rebours se joue autrement. */
    public int summonsLeft() {
        return summonsLeft;
    }

    /** Consomme une invocation. Appelé à l'<b>exécution</b>, jamais à la décision. */
    void spendSummon() {
        summonsLeft--;
    }

    @Override
    public String spriteName() {
        return kind.spriteName();
    }

    @Override
    public String label() {
        if (traits.isEmpty()) {
            return kind.label();
        }
        StringBuilder builder = new StringBuilder(kind.label());
        for (Trait trait : traits) {
            builder.append(' ').append(trait.label());
        }
        return builder.toString();
    }

    @Override
    public String toString() {
        return "Enemy{" + label() + ", " + intention + "}";
    }
}
