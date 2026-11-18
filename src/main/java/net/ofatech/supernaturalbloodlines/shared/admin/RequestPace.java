package net.ofatech.supernaturalbloodlines.shared.admin;

/** When the Admin page asks the server for a fresh snapshot: when it has none or was away, and never more than once per {@link #GAP_MS}, however often it is drawn, and every {@link #REFRESH_MS} while it stays open. Pure. */
public final class RequestPace {
    public static final long GAP_MS = 1500;
    /** An open page with a snapshot still asks this often, so values another operator changed show up. */
    public static final long REFRESH_MS = 10_000;

    private RequestPace() {}

    public static boolean due(long lastSent, long lastRender, boolean haveSnapshot, long now) {
        return now - lastSent >= GAP_MS && (!haveSnapshot || now - lastRender > GAP_MS || now - lastSent >= REFRESH_MS);
    }
}

