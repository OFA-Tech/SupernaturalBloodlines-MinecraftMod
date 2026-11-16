package net.ofatech.supernaturalbloodlines.core.supernatural;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ladder progress, skill points and skills of a player (the {@code supernaturalbloodlines:supernatural} attachment).
 * Pure data: the rules live in {@link LadderRules}. Maps are keyed by {@link SupernaturalRace#id()}, so a race this
 * version does not know is kept and ignored.
 *
 * @param schema     0 = never migrated, 1 = migrated (server only)
 * @param rungs      highest rung reached per race (a high-water mark, kept when the race changes)
 * @param raceSince  game time the player first became that race
 * @param sp         unspent skill points, one pool for every race
 * @param spEarned   lifetime skill points
 * @param done       ids of the leaf tasks whose condition is met
 * @param claimed    ids of the tasks whose reward was paid
 * @param counters   progress of counting tasks of an active rung; dropped when the task is done
 * @param lifetime   counts that run from the moment the player has a race, whatever the rung (the "in total" tasks), by {@code TaskSpec#totalKey}
 * @param skills     our skills bought
 * @param quests     skill quests finished
 * @param granted    Tensura skill and attribute ids our skills granted; only these are ever taken back (server only)
 * @param vampirismGranted Vampirism skill ids our skills enabled for the player; only these are ever taken back, a skill the player had or bought is never in it (server only)
 * @param backfill   what the migration gave (server only)
 */
public record SupernaturalState(int schema, Map<String, Integer> rungs, Map<String, Long> raceSince, int sp, int spEarned,
                                Set<String> done, Set<String> claimed, Map<String, Long> counters, Map<String, Long> lifetime, Set<String> skills,
                                Set<String> quests, Set<String> granted, Set<String> vampirismGranted, List<String> backfill) {

    public static final SupernaturalState NONE = new SupernaturalState(0, Map.of(), Map.of(), 0, 0, Set.of(), Set.of(),
            Map.of(), Map.of(), Set.of(), Set.of(), Set.of(), Set.of(), List.of());

    /**
     * A map that keeps every entry that decodes and logs the ones that do not, so one bad value (a hand-edited file, an old tag) cannot
     * silently wipe the whole map: with the plain map codec one bad entry fails the field and the lenient field falls back to empty.
     */
    private static <V> Codec<Map<String, V>> lenientMap(Codec<V> value, String field) {
        Codec<Map<String, V>> strict = Codec.unboundedMap(Codec.STRING, value);
        Codec<Map<String, Dynamic<?>>> raw = Codec.unboundedMap(Codec.STRING, Codec.PASSTHROUGH);
        return Codec.of(strict, new Decoder<Map<String, V>>() {
            @Override
            public <T> DataResult<Pair<Map<String, V>, T>> decode(DynamicOps<T> ops, T input) {
                return raw.decode(ops, input).map(decoded -> {
                    Map<String, V> out = new HashMap<>();
                    decoded.getFirst().forEach((key, entry) -> entry(value, entry).resultOrPartial(
                            err -> LOG.warn("Supernatural state: dropped {}[{}]: {}", field, key, err)).ifPresent(v -> out.put(key, v)));
                    return Pair.of(out, decoded.getSecond());
                });
            }
        });
    }

    private static <V, T> DataResult<V> entry(Codec<V> value, Dynamic<T> d) {
        return value.parse(d);
    }

    private static List<String> sorted(Set<String> s) {
        List<String> l = new ArrayList<>(s);
        Collections.sort(l);
        return l;
    }

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("supernaturalbloodlines");

    public static final Codec<SupernaturalState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.lenientOptionalFieldOf("schema", 0).forGetter(SupernaturalState::schema),
            lenientMap(Codec.INT, "rungs").lenientOptionalFieldOf("rungs", Map.of()).forGetter(SupernaturalState::rungs),
            lenientMap(Codec.LONG, "race_since").lenientOptionalFieldOf("race_since", Map.of()).forGetter(SupernaturalState::raceSince),
            Codec.INT.lenientOptionalFieldOf("sp", 0).forGetter(SupernaturalState::sp),
            Codec.INT.lenientOptionalFieldOf("sp_earned", 0).forGetter(SupernaturalState::spEarned),
            Codec.STRING.listOf().lenientOptionalFieldOf("done", List.of()).forGetter(s -> sorted(s.done())),
            Codec.STRING.listOf().lenientOptionalFieldOf("claimed", List.of()).forGetter(s -> sorted(s.claimed())),
            lenientMap(Codec.LONG, "counters").lenientOptionalFieldOf("counters", Map.of()).forGetter(SupernaturalState::counters),
            lenientMap(Codec.LONG, "lifetime").lenientOptionalFieldOf("lifetime", Map.of()).forGetter(SupernaturalState::lifetime),
            Codec.STRING.listOf().lenientOptionalFieldOf("skills", List.of()).forGetter(s -> sorted(s.skills())),
            Codec.STRING.listOf().lenientOptionalFieldOf("quests", List.of()).forGetter(s -> sorted(s.quests())),
            Codec.STRING.listOf().lenientOptionalFieldOf("granted", List.of()).forGetter(s -> sorted(s.granted())),
            Codec.STRING.listOf().lenientOptionalFieldOf("vampirism_granted", List.of()).forGetter(s -> sorted(s.vampirismGranted())),
            Codec.STRING.listOf().lenientOptionalFieldOf("backfill", List.of()).forGetter(SupernaturalState::backfill)
    ).apply(i, (schema, rungs, since, sp, earned, done, claimed, counters, lifetime, skills, quests, granted, vampirismGranted, backfill) ->
            new SupernaturalState(schema, rungs, since, sp, earned, new HashSet<>(done), new HashSet<>(claimed), counters, lifetime,
                    new HashSet<>(skills), new HashSet<>(quests), new HashSet<>(granted), new HashSet<>(vampirismGranted), backfill)));

    public SupernaturalState {
        rungs = Map.copyOf(rungs);
        raceSince = Map.copyOf(raceSince);
        counters = Map.copyOf(counters);
        lifetime = Map.copyOf(lifetime);
        done = Set.copyOf(done);
        claimed = Set.copyOf(claimed);
        skills = Set.copyOf(skills);
        quests = Set.copyOf(quests);
        granted = Set.copyOf(granted);
        vampirismGranted = Set.copyOf(vampirismGranted);
        backfill = List.copyOf(backfill);
    }

    /** The highest rung reached on that race's ladder; 0 (the race itself) before any. */
    public int rung(SupernaturalRace race) {
        return rungs.getOrDefault(race.id(), 0);
    }

    /** This state with the server-only tracking ({@code granted}, {@code vampirismGranted}, {@code backfill}) taken from {@code source}. */
    public SupernaturalState withTrackingOf(SupernaturalState source) {
        return toBuilder().granted(source.granted()).vampirismGranted(source.vampirismGranted()).backfill(source.backfill()).build();
    }

    /** What the owning client may know: no server-only tracking and no schema. */
    public SupernaturalState clientView() {
        return withTrackingOf(NONE).toBuilder().schema(0).build();
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static final class Builder {
        private int schema;
        private Map<String, Integer> rungs;
        private Map<String, Long> raceSince;
        private int sp;
        private int spEarned;
        private Set<String> done;
        private Set<String> claimed;
        private Map<String, Long> counters;
        private Map<String, Long> lifetime;
        private Set<String> skills;
        private Set<String> quests;
        private Set<String> granted;
        private Set<String> vampirismGranted;
        private List<String> backfill;

        private Builder(SupernaturalState s) {
            schema = s.schema;
            rungs = s.rungs;
            raceSince = s.raceSince;
            sp = s.sp;
            spEarned = s.spEarned;
            done = s.done;
            claimed = s.claimed;
            counters = s.counters;
            lifetime = s.lifetime;
            skills = s.skills;
            quests = s.quests;
            granted = s.granted;
            vampirismGranted = s.vampirismGranted;
            backfill = s.backfill;
        }

        public Builder schema(int v) { schema = v; return this; }
        public Builder rungs(Map<String, Integer> v) { rungs = v; return this; }
        public Builder raceSince(Map<String, Long> v) { raceSince = v; return this; }
        public Builder sp(int v) { sp = v; return this; }
        public Builder spEarned(int v) { spEarned = v; return this; }
        public Builder done(Set<String> v) { done = v; return this; }
        public Builder claimed(Set<String> v) { claimed = v; return this; }
        public Builder counters(Map<String, Long> v) { counters = v; return this; }
        public Builder lifetime(Map<String, Long> v) { lifetime = v; return this; }
        public Builder skills(Set<String> v) { skills = v; return this; }
        public Builder quests(Set<String> v) { quests = v; return this; }
        public Builder granted(Set<String> v) { granted = v; return this; }
        public Builder vampirismGranted(Set<String> v) { vampirismGranted = v; return this; }
        public Builder backfill(List<String> v) { backfill = v; return this; }

        public Builder rung(SupernaturalRace race, int rung) {
            Map<String, Integer> m = new HashMap<>(rungs);
            m.put(race.id(), rung);
            rungs = m;
            return this;
        }

        public SupernaturalState build() {
            return new SupernaturalState(schema, rungs, raceSince, sp, spEarned, done, claimed, counters, lifetime, skills, quests,
                    granted, vampirismGranted, backfill);
        }
    }
}
