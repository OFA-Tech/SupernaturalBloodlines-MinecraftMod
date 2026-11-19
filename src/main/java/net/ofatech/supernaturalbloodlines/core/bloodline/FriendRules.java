package net.ofatech.supernaturalbloodlines.core.bloodline;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * Who never hurts whom (R144, plan 14 allies): two sides are friends when they are of the same pack or of allied packs. A side is named by the
 * leader of its pack (a lone player leads their own); a side with no pack at all (a wild NPC, a mob) has none and is nobody's friend. Pure.
 */
public final class FriendRules {
    private FriendRules() {
    }

    public static boolean friends(@Nullable UUID leaderA, @Nullable UUID leaderB, boolean packsAllied) {
        if (leaderA == null || leaderB == null) {
            return false;
        }
        return leaderA.equals(leaderB) || packsAllied;
    }
}
