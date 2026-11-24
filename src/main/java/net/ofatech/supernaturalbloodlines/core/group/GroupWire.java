package net.ofatech.supernaturalbloodlines.core.group;

/** The size bounds the group view's codec and its tests share. A decode over a bound fails that one packet cleanly. */
public final class GroupWire {
    public static final int MAX_MEMBERS = 64;
    public static final int MAX_NAME = 32;
    public static final int MAX_RANK = 16;

    private GroupWire() {
    }
}
