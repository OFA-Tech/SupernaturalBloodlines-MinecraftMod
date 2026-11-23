package net.ofatech.supernaturalbloodlines.parts.bosses.director;

/** How often a player is told of their hits on one boss: a flurry of blows is one message every half second, not one packet per blow. Pure. */
public final class HitThrottle {
    public static final long MIN_GAP = 10;
    public static final long NEVER = Long.MIN_VALUE;

    private HitThrottle() {
    }

    /** Whether to send now, when the last one was sent at {@code last} ({@link #NEVER} for none). A clock that went back (a new world) sends. */
    public static boolean allow(long last, long now) {
        return last == NEVER || now < last || now - last >= MIN_GAP;
    }
}
