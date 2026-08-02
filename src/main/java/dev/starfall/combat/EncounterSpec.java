package dev.starfall.combat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Everything needed to open one encounter, and nothing that changes during it.
 *
 * <p>Fully declarative and fully validated at construction, so an encounter that
 * builds is an encounter that can be played: the lane is a legal length, no two
 * bodies start on the same tile, everyone is on the board, and the wave respects
 * {@link Lane#maxSimultaneousEnemies()}.
 *
 * @param lane        the Fold of the World
 * @param hero        which Night Pilgrim, and therefore which movement verb
 * @param heroHp      placeholder hit points
 * @param heroTile    where the hero opens
 * @param heroFacing  which way the hero opens
 * @param loadout     the hand of tiles
 * @param enemies     the Charted Shadows, in placement order
 * @param seed        fixes composition; the fight itself is deterministic regardless
 */
public record EncounterSpec(Lane lane, Hero hero, int heroHp, int heroTile, Facing heroFacing,
                            List<Tile> loadout, List<Placement> enemies, long seed) {

    /**
     * One Charted Shadow, placed.
     *
     * @param extraTraits traits fitted on top of the archetype's own. The elite
     *                    mechanism of the source, reduced to its one useful form:
     *                    it lets an encounter ask "what if this one were Quick"
     *                    without inventing a sixth archetype.
     */
    public record Placement(EnemyArchetype archetype, int tile, Facing facing, Set<Trait> extraTraits) {

        public Placement {
            extraTraits = Collections.unmodifiableSet(extraTraits.isEmpty()
                    ? EnumSet.noneOf(Trait.class)
                    : EnumSet.copyOf(extraTraits));
        }

        public static Placement of(EnemyArchetype archetype, int tile, Facing facing) {
            return new Placement(archetype, tile, facing, Set.of());
        }

        public Placement with(Trait... traits) {
            EnumSet<Trait> t = EnumSet.noneOf(Trait.class);
            t.addAll(extraTraits);
            Collections.addAll(t, traits);
            return new Placement(archetype, tile, facing, t);
        }
    }

    public EncounterSpec {
        loadout = List.copyOf(loadout);
        enemies = List.copyOf(enemies);
        if (loadout.isEmpty()) {
            throw new IllegalArgumentException("the hero needs at least one tile");
        }
        if (heroHp <= 0) {
            throw new IllegalArgumentException("heroHp must be positive: " + heroHp);
        }
        if (!lane.contains(heroTile)) {
            throw new IllegalArgumentException("hero starts off the lane at " + heroTile);
        }
        if (enemies.size() > lane.maxSimultaneousEnemies()) {
            throw new IllegalArgumentException("a lane of " + lane.length() + " holds at most "
                    + lane.maxSimultaneousEnemies() + " Charted Shadows, got " + enemies.size());
        }
        List<Integer> taken = new ArrayList<>();
        taken.add(heroTile);
        for (Placement p : enemies) {
            if (!lane.contains(p.tile())) {
                throw new IllegalArgumentException(p.archetype() + " starts off the lane at " + p.tile());
            }
            if (taken.contains(p.tile())) {
                throw new IllegalArgumentException("two bodies start on tile " + p.tile());
            }
            taken.add(p.tile());
        }
    }

    /** A builder, because encounters are written by hand far more often than generated. */
    public static Builder builder(int laneLength, Hero hero) {
        return new Builder(laneLength, hero);
    }

    /**
     * A wave laid out from a seed: the one place the encounter layer is allowed to
     * be random. Enemies are drawn from the five archetypes and placed on distinct
     * tiles in the half of the lane away from the hero, so the fight always opens
     * with an approach rather than with someone already inside your guard.
     *
     * <p>Deterministic in the seed, and that is tested. Composition being seeded
     * and resolution being not is the split that keeps the review loop honest: you
     * can regenerate the same board a thousand times and it plays the same way.
     */
    public static EncounterSpec wave(int laneLength, Hero hero, int enemyCount, long seed) {
        Lane lane = new Lane(laneLength);
        if (enemyCount < 0 || enemyCount > lane.maxSimultaneousEnemies()) {
            throw new IllegalArgumentException("a lane of " + laneLength + " holds at most "
                    + lane.maxSimultaneousEnemies() + " Charted Shadows, asked for " + enemyCount);
        }
        Rng rng = new Rng(seed);
        int heroTile = 0;
        List<Integer> free = new ArrayList<>();
        for (int t = Math.max(1, laneLength / 2); t < laneLength; t++) {
            free.add(t);
        }
        List<Placement> out = new ArrayList<>();
        EnemyArchetype[] pool = EnemyArchetype.values();
        for (int i = 0; i < enemyCount; i++) {
            if (free.isEmpty()) {
                break;
            }
            EnemyArchetype a = pool[rng.nextInt(pool.length)];
            int tile = free.remove(rng.nextInt(free.size()));
            out.add(Placement.of(a, tile, Facing.LEFT));
        }
        out.sort((a, b) -> Integer.compare(a.tile(), b.tile()));
        return new EncounterSpec(lane, hero, Hero.DEFAULT_HP, heroTile, Facing.RIGHT,
                Loadout.starting().tiles(), out, seed);
    }

    public static final class Builder {
        private final Lane lane;
        private final Hero hero;
        private int heroHp = Hero.DEFAULT_HP;
        private int heroTile = 0;
        private Facing heroFacing = Facing.RIGHT;
        private List<Tile> loadout = Loadout.starting().tiles();
        private final List<Placement> enemies = new ArrayList<>();
        private long seed = 0L;

        private Builder(int laneLength, Hero hero) {
            this.lane = new Lane(laneLength);
            this.hero = hero;
        }

        public Builder heroAt(int tile, Facing facing) {
            this.heroTile = tile;
            this.heroFacing = facing;
            return this;
        }

        public Builder heroHp(int hp) {
            this.heroHp = hp;
            return this;
        }

        public Builder loadout(Tile... tiles) {
            this.loadout = List.of(tiles);
            return this;
        }

        public Builder loadout(List<Tile> tiles) {
            this.loadout = List.copyOf(tiles);
            return this;
        }

        public Builder enemy(EnemyArchetype archetype, int tile, Facing facing) {
            enemies.add(Placement.of(archetype, tile, facing));
            return this;
        }

        public Builder enemy(Placement placement) {
            enemies.add(placement);
            return this;
        }

        public Builder seed(long seed) {
            this.seed = seed;
            return this;
        }

        public EncounterSpec build() {
            return new EncounterSpec(lane, hero, heroHp, heroTile, heroFacing, loadout, enemies, seed);
        }
    }
}
