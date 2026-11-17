package net.ofatech.supernaturalbloodlines.core.bloodline;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.jetbrains.annotations.Nullable;

/** The pure rules of Original bloodlines (R120 to R127). Roots are UUIDs: a player Original's own, or an {@link NpcOriginal}'s fixed one. */
public final class LineRules {
    public static final String REFUSAL_PREFIX = "supernaturalbloodlines.refusal.line_";

    private LineRules() {
    }

    public enum State { ORIGINAL, NOT_VAMPIRE, SIRE_UNKNOWN, IN_LINE, FREE }

    public static State state(boolean original, boolean vampire, boolean free, boolean hasRoot) {
        if (original) {
            return State.ORIGINAL;
        }
        if (!vampire) {
            return State.NOT_VAMPIRE;
        }
        if (free) {
            return State.FREE;
        }
        return hasRoot ? State.IN_LINE : State.SIRE_UNKNOWN;
    }

    /** R121: a child of an Original gets that Original, a child of a member gets the member's root, a child of a lineless sire gets none. */
    public static Optional<UUID> inheritedRoot(UUID sire, boolean sireIsOriginal, Optional<UUID> sireRoot) {
        return sireIsOriginal ? Optional.of(sire) : sireRoot;
    }

    /** R123 */
    public static boolean showsRoot(@Nullable UUID known, UUID root, boolean always) {
        return always || root.equals(known);
    }

    /** {@code name=weight} entries; a bad entry is skipped and a negative weight counts as 0. */
    public static Map<NpcOriginal, Integer> parseWeights(List<String> entries) {
        Map<NpcOriginal, Integer> out = new EnumMap<>(NpcOriginal.class);
        for (String e : entries) {
            int eq = e.indexOf('=');
            if (eq < 0) {
                continue;
            }
            Optional<NpcOriginal> o = NpcOriginal.byId(e.substring(0, eq).trim().toLowerCase());
            if (o.isEmpty()) {
                continue;
            }
            try {
                out.put(o.get(), Math.max(0, Integer.parseInt(e.substring(eq + 1).trim())));
            } catch (NumberFormatException ignored) {
                // a bad entry is skipped
            }
        }
        return out;
    }

    /** A weighted pick among the living NPC roots; {@code roll} is in [0,1). Empty when every weight is 0 or every weighted root is dead. */
    public static Optional<NpcOriginal> pickRoot(Map<NpcOriginal, Integer> weights, Set<NpcOriginal> dead, double roll) {
        long sum = 0;
        for (NpcOriginal o : NpcOriginal.values()) {
            sum += weight(weights, dead, o);
        }
        if (sum <= 0) {
            return Optional.empty();
        }
        double target = roll * sum;
        double acc = 0;
        NpcOriginal last = null;
        for (NpcOriginal o : NpcOriginal.values()) {
            int w = weight(weights, dead, o);
            if (w <= 0) {
                continue;
            }
            last = o;
            acc += w;
            if (target < acc) {
                return Optional.of(o);
            }
        }
        return Optional.ofNullable(last);
    }

    private static int weight(Map<NpcOriginal, Integer> weights, Set<NpcOriginal> dead, NpcOriginal o) {
        return dead.contains(o) ? 0 : Math.max(0, weights.getOrDefault(o, 0));
    }

    public record BloodPair(UUID original, UUID target) {
    }

    /** The first ordered pair of distinct sources where the second belongs to the first's line (the first is its root). */
    public static Optional<BloodPair> pairBloods(List<UUID> sources, Function<UUID, Optional<UUID>> rootOf) {
        for (UUID a : sources) {
            for (UUID b : sources) {
                if (!a.equals(b) && rootOf.apply(b).filter(a::equals).isPresent()) {
                    return Optional.of(new BloodPair(a, b));
                }
            }
        }
        return Optional.empty();
    }

    public record BrokenLineFacts(boolean witchType, boolean eclipse, int bloodCount, Optional<BloodPair> pair,
                                  boolean targetOnline, boolean targetIsOriginal) {
    }

    /** R125: the first failing check, as a full lang key. */
    public static Optional<String> brokenLineRefusal(BrokenLineFacts f) {
        if (!f.witchType()) {
            return refusal("not_witch");
        }
        if (!f.eclipse()) {
            return refusal("no_eclipse");
        }
        if (f.bloodCount() < 2) {
            return refusal("no_blood");
        }
        if (f.pair().isEmpty()) {
            return refusal("wrong_blood");
        }
        if (!f.targetOnline()) {
            return refusal("target_away");
        }
        if (f.targetIsOriginal()) {
            return refusal("target_original");
        }
        return Optional.empty();
    }

    public record ImprisonedFacts(boolean witchType, boolean eclipse, Optional<UUID> targetBlood, boolean targetOnline,
                                  boolean targetIsOriginal, Optional<UUID> targetRoot, Optional<UUID> trappedRoot,
                                  boolean ringClosed) {
    }

    /** R126: the first failing check, as a full lang key. */
    public static Optional<String> imprisonedRefusal(ImprisonedFacts f) {
        if (!f.witchType()) {
            return refusal("not_witch");
        }
        if (!f.eclipse()) {
            return refusal("no_eclipse");
        }
        if (f.targetBlood().isEmpty()) {
            return refusal("no_blood");
        }
        if (!f.targetOnline()) {
            return refusal("target_away");
        }
        if (f.targetIsOriginal()) {
            return refusal("target_original");
        }
        if (f.trappedRoot().isEmpty()) {
            return refusal("nobody_trapped");
        }
        if (!f.trappedRoot().equals(f.targetRoot())) {
            return refusal("wrong_original");
        }
        if (!f.ringClosed()) {
            return refusal("ring_open");
        }
        return Optional.empty();
    }

    private static Optional<String> refusal(String key) {
        return Optional.of(REFUSAL_PREFIX + key);
    }
}
