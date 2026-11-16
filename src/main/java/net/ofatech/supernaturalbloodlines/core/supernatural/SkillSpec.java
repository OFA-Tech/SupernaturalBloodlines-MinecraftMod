package net.ofatech.supernaturalbloodlines.core.supernatural;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * One skill of the catalogue (a file {@code supernatural/skill/<id>.json}). The kinds are fixed in code, the numbers come from the JSON.
 * No game classes.
 *
 * @param id           the catalogue id, such as {@code S-VAM-4}
 * @param cost         skill points; 0 only for a granted-only skill
 * @param grantedOnly  never bought: another rule grants it (S-WOL-8, phase 12d)
 * @param kinds        what the skill does: an attribute modifier, a number a rule reads, a Tensura skill, or Vampirism blood
 * @param attr         the modifier of an ATTR skill
 * @param tensuraSkill the Tensura skill a TENSURA skill grants, as a resource location
 * @param vampirismSkill the Vampirism skill a VAMPIRISM skill enables for its owner (Vampirism's own skill, with its own effect), as a resource location
 * @param params       the numbers RULE and BLOOD skills read through {@code LadderQuery.value}
 */
public record SkillSpec(String id, String name, int cost, boolean grantedOnly, Set<Kind> kinds, @Nullable Attr attr,
                        @Nullable String tensuraSkill, @Nullable String vampirismSkill, Map<String, Double> params) {
    public enum Kind {
        ATTR, RULE, TENSURA, BLOOD, VAMPIRISM;

        public static Kind byId(String id) {
            for (Kind k : values()) {
                if (k.name().toLowerCase(Locale.ROOT).equals(id)) {
                    return k;
                }
            }
            throw new IllegalArgumentException(id);
        }
    }

    /** When an ATTR modifier is on. It is re-checked every 200 ticks, so it lags a change of the sky by up to 10 seconds. */
    public enum When {
        ALWAYS, NIGHT, FULL_MOON_NIGHT;

        /** Werewolves' own full-moon window (its Helper.isFullMoon): after 12786 and before 23216 on the phase-0 day. */
        private static final long MOON_START = 12786;
        private static final long MOON_END = 23216;

        public static When byId(String id) {
            for (When w : values()) {
                if (w.name().toLowerCase(Locale.ROOT).equals(id)) {
                    return w;
                }
            }
            throw new IllegalArgumentException(id);
        }

        /** Dimensions with a fixed time (the Nether, the End) have no night and no moon. */
        public boolean holds(boolean hasFixedTime, int moonPhase, long dayTime) {
            long timeOfDay = Math.floorMod(dayTime, 24000L);
            return switch (this) {
                case ALWAYS -> true;
                case NIGHT -> !hasFixedTime && timeOfDay >= 13000 && timeOfDay < 23000;
                case FULL_MOON_NIGHT -> !hasFixedTime && moonPhase == 0 && timeOfDay > MOON_START && timeOfDay < MOON_END;
            };
        }
    }

    public static final Set<String> OPERATIONS = Set.of("add_value", "add_multiplied_base", "add_multiplied_total");

    /** @param attribute a resource location such as {@code minecraft:generic.max_health}; the part resolves it */
    public record Attr(String attribute, String operation, double amount, When when) {
    }

    public SkillSpec {
        kinds = Set.copyOf(kinds);
        params = Map.copyOf(params);
    }

    /** The mod a skill needs to exist at all: a TENSURA skill is hidden without Tensura. */
    @Nullable
    public String mod() {
        return kinds.contains(Kind.TENSURA) ? "tensura" : null; // a VAMPIRISM skill needs no check: the supernatural part itself needs Vampirism
    }

    /** The number the JSON gives for {@code key}, or {@code fallback} when it names none. */
    public double param(String key, double fallback) {
        return params.getOrDefault(key, fallback);
    }
}
