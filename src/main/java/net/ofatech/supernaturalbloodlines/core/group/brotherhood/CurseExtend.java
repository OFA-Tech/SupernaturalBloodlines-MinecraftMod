package net.ofatech.supernaturalbloodlines.core.group.brotherhood;

/** R295: the timed end of a curse an unmarked brother's death brings. A marked brother's death keeps the old path, which owns its end. */
public final class CurseExtend {
    private CurseExtend() {
    }

    public static long endAt(long now, int hours) {
        return now + hours * 3_600_000L;
    }

    public static boolean expired(long until, long now) {
        return now >= until;
    }
}
