package net.ofatech.supernaturalbloodlines.core.group.clan;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/** R275: the attack bonus of clanmates of one sireline standing near each other. Only counts and percents leave this class. */
public final class SirelineBonus {
    private SirelineBonus() {
    }

    /** The bonus as a fraction (0.03 is +3%): {@code percentEach} for each mate, up to {@code maxPercent}. */
    public static double of(int mates, int percentEach, int maxPercent) {
        if (mates <= 0 || percentEach <= 0) {
            return 0;
        }
        return Math.min((long) mates * percentEach, maxPercent) / 100.0;
    }

    /**
     * The top of the line: walks {@code sireOf} up until there is no sire or a cycle closes. A line deeper than {@code depthCap} has no known root: it answers
     * empty, never the node where the walk was cut (two different long lines cut at the same depth must not look like one line).
     */
    public static Optional<UUID> rootOf(UUID id, Function<UUID, Optional<UUID>> sireOf, int depthCap) {
        Set<UUID> seen = new HashSet<>();
        UUID current = id;
        seen.add(current);
        for (int steps = 0; steps <= depthCap; steps++) {
            Optional<UUID> up = sireOf.apply(current);
            if (up.isEmpty() || !seen.add(up.get())) {
                return Optional.of(current);
            }
            current = up.get();
        }
        return Optional.empty();
    }

    /** Two known roots that are the same: a root that is unknown is the same as nothing, not even itself. */
    public static boolean same(Optional<UUID> a, Optional<UUID> b) {
        return a.isPresent() && a.equals(b);
    }
}
