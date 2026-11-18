package net.ofatech.supernaturalbloodlines.shared.admin;

/** When the Admin page asks the server for a fresh snapshot: when it has none or was away, and never more than once per {@link #GAP_MS}, however often it is drawn. Pure. */
public final class RequestPace {
    public static final long GAP_MS = 1500;

    private RequestPace() {}

    public static boolean due(long lastSent, long lastRender, boolean haveSnapshot, long now) {
        return now - lastSent >= GAP_MS && (!haveSnapshot || now - lastRender > GAP_MS);
    }
}

