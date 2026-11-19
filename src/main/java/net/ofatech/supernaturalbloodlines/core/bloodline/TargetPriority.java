package net.ofatech.supernaturalbloodlines.core.bloodline;

import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * What an NPC werewolf hunts first (R144): a vampire, then a hunter, then another player, then prey. A friend (its own pack's Guard, an ally, a
 * recruit's owner) is never a target. Pure: the entities are whatever {@code T} the caller has.
 */
public final class TargetPriority {
    /** Best first. */
    public enum Tier { VAMPIRE, HUNTER, PLAYER, PREY }

    /** Something the wolf can see. {@code friend} excludes it. */
    public record Candidate<T>(T who, Tier tier, double distSq, boolean friend) {
    }

    private TargetPriority() {
    }

    /** The best candidate: the best tier, the nearest of it; null when there is none (or only friends). */
    @Nullable
    public static <T> Candidate<T> pick(List<Candidate<T>> seen) {
        Candidate<T> best = null;
        for (Candidate<T> c : seen) {
            if (c.friend()) {
                continue;
            }
            if (best == null || c.tier().ordinal() < best.tier().ordinal()
                    || (c.tier() == best.tier() && c.distSq() < best.distSq())) {
                best = c;
            }
        }
        return best;
    }

    /**
     * Whether the wolf drops what it is hunting for {@code better}: only a better tier does it, and only when the current one is gone, out of reach
     * or a friend. An equal or worse one never takes it over, so the pack does not flicker between two targets.
     */
    public static boolean switchTo(@Nullable Tier current, boolean currentValid, @Nullable Tier better) {
        if (better == null) {
            return false;
        }
        if (current == null || !currentValid) {
            return true;
        }
        return better.ordinal() < current.ordinal();
    }
}
