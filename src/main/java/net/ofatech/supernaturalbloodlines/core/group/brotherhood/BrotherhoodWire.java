package net.ofatech.supernaturalbloodlines.core.group.brotherhood;

/** The size bounds of the brotherhood view's codec and its tests. A decode over a bound fails that one packet cleanly. */
public final class BrotherhoodWire {
    public static final int MAX_MARK = 20;
    public static final int MAX_STRIKES = 100;
    /** The longest breaker day the config allows: 720 hours. */
    public static final int MAX_SECONDS = 720 * 3600;
    public static final int MAX_COUNT = 100;
    public static final int MAX_PERCENT = 100;
    /** The longest strike window the config allows. */
    public static final int MAX_DAYS = 365;

    private BrotherhoodWire() {
    }

    public static int clamp(long v, int max) {
        return (int) Math.max(0, Math.min(max, v));
    }
}