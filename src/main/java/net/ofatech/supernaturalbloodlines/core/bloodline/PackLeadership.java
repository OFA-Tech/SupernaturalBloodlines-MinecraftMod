package net.ofatech.supernaturalbloodlines.core.bloodline;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank;
import org.jetbrains.annotations.Nullable;

/**
 * Who succeeds the leader of a pack with more than one Alpha (plan 14). The strongest Alpha: a True Alpha before a plain Alpha,
 * then the higher Werewolves level; on a tie the first in the list (the order they joined). **Only a successor:** the order is
 * used when the leader leaves, loses the spark or gives it up, never when someone joins or levels up (a leader keeps the pack
 * until then, or until they lose a duel). Pure: no game classes, so a strength is one number the index can store for an offline player.
 */
public final class PackLeadership {
    /** Levels never reach this; the rank is kept above them in the packed number. */
    private static final int RANK_STEP = 1000;

    private PackLeadership() {
    }

    /** The packed strength of an Alpha of this rank and level: {@code rank * 1000 + level}. 0 for anyone who is not an Alpha. */
    public static int strength(Rank rank, int level) {
        int order = switch (rank) {
            case TRUE_ALPHA -> 2;
            case ALPHA -> 1;
            default -> 0;
        };
        return order == 0 ? 0 : order * RANK_STEP + Math.max(0, Math.min(level, RANK_STEP - 1));
    }

    public static boolean isAlpha(int strength) {
        return strength >= RANK_STEP;
    }

    /** A pack member with the strength the index last saw. */
    public record Candidate(UUID id, int strength) {
    }

    /**
     * @param candidates the leader and the members, in the order they joined (every one of them, not only the Alphas)
     * @param sitting    the current leader, if any
     * @return the Alpha who should lead, or empty when nobody in the list is an Alpha
     */
    public static Optional<UUID> pick(List<Candidate> candidates, @Nullable UUID sitting) {
        Candidate best = null;
        for (Candidate c : candidates) {
            if (!isAlpha(c.strength())) {
                continue;
            }
            if (best == null || c.strength() > best.strength() || (c.strength() == best.strength() && c.id().equals(sitting) && !best.id().equals(sitting))) {
                best = c;
            }
        }
        return Optional.ofNullable(best).map(Candidate::id);
    }
}
