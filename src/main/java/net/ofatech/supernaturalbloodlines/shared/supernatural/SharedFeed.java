package net.ofatech.supernaturalbloodlines.shared.supernatural;

/**
 * Whether the drink in progress is a clan's share of somebody else's feed. A listener that must not act on a share (it rescales, rewards or counts a
 * feed) returns when {@link #active()}. Lives in {@code shared} so a part that cannot see the species part (the artifacts) can still read it; the default is "not a share".
 */
public final class SharedFeed {
    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private SharedFeed() {
    }

    public static boolean active() {
        return ACTIVE.get();
    }

    public static void run(Runnable share) {
        boolean was = ACTIVE.get();
        ACTIVE.set(Boolean.TRUE);
        try {
            share.run();
        } finally {
            ACTIVE.set(was);
        }
    }
}
