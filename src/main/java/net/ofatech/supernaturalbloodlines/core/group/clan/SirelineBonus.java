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

    /** The top of the line: walks {@code sireOf} up until there is no sire, a cycle closes or {@code depthCap} steps are made; answers the last id reached. */
    public static UUID rootOf(UUID id, Function<UUID, Optional<UUID>> sireOf, int depthCap) {
        Set<UUID> seen = new HashSet<>();
        UUID current = id;
        seen.add(current);
        for (int steps = 0; steps < depthCap; steps++) {
            Optional<UUID> up = sireOf.apply(current);
            if (up.isEmpty() || !seen.add(up.get())) {
                return current;
            }
            current = up.get();
        }
        return current;
    }

    public static boolean sameLine(UUID a, UUID b, Function<UUID, Optional<UUID>> sireOf, int depthCap) {
        return rootOf(a, sireOf, depthCap).equals(rootOf(b, sireOf, depthCap));
    }
}