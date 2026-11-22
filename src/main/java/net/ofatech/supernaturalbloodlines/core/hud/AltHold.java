package net.ofatech.supernaturalbloodlines.core.hud;

/**
 * Alt shows each slot's name and state beside it at once; the full text (mastery, what the skill does, why it is locked, in a panel above the column) comes
 * after Alt has been held for a second, so a quick look is never covered by a wall of text. Pure.
 */
public final class AltHold {
    /** One second. */
    public static final int DETAIL_TICKS = 20;

    private AltHold() {
    }

    /** @param since the tick Alt went down, or a negative number when it is up */
    public static boolean detailed(long since, long now) {
        return since >= 0 && now - since >= DETAIL_TICKS;
    }

    /** The tick Alt went down: kept while it stays down, forgotten when it comes up. */
    public static long since(long previous, boolean altDown, long now) {
        if (!altDown) {
            return -1;
        }
        return previous >= 0 ? previous : now;
    }
}