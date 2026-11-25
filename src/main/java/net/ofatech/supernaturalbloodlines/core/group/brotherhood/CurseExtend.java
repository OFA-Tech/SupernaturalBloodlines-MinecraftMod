package net.ofatech.supernaturalbloodlines.core.group.brotherhood;

import java.util.OptionalLong;

/** R295: the 48-hour end of a curse an unmarked brother's death brings. A marked brother's death keeps the old path, which owns its end. */
public final class CurseExtend {
    private CurseExtend() {
    }

    public static OptionalLong endAt(boolean killerAlreadyCursed, boolean victimWasMarked, long now, int hours) {
        if (killerAlreadyCursed || victimWasMarked) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(now + hours * 3_600_000L);
    }

    public static boolean expired(long until, long now) {
        return now >= until;
    }
}