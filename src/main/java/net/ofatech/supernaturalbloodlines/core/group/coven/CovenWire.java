package net.ofatech.supernaturalbloodlines.core.group.coven;

/** The size bounds of the coven view's codec and its tests. A decode over a bound fails that one packet cleanly. */
public final class CovenWire {
    /** A circle holds at most thirteen (the anchor and twelve). */
    public static final int MAX_CIRCLE = 13;
    public static final int MAX_NAME = 32;

    private CovenWire() {
    }
}