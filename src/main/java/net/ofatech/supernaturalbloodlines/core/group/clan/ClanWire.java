package net.ofatech.supernaturalbloodlines.core.group.clan;

/** The size bounds of the clan view's codec and its tests. A decode over a bound fails that one packet cleanly. */
public final class ClanWire {
    public static final int MAX_NAME = 32;
    /** No clan counts more mates than the group size ever allows. */
    public static final int MAX_COUNT = 100;
    public static final int MAX_DAYS = 3650;
    public static final int MAX_SECONDS = 24 * 3600 * 400;
    public static final int MAX_PERCENT = 100;

    private ClanWire() {
    }

    public static String clip(String s) {
        return s.length() <= MAX_NAME ? s : s.substring(0, MAX_NAME);
    }
}
