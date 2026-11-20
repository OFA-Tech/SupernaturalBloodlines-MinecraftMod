package net.ofatech.supernaturalbloodlines.core.boss;

/** R159: a hint to a player (the action-bar "white oak only works on an Original who is down") is not repeated within {@link #GAP} ticks. */
public final class NoticeRules {
    public static final long GAP = 60L;

    private NoticeRules() {
    }

    public static boolean due(long lastSent, long now) {
        return lastSent < 0 || now - lastSent >= GAP || now < lastSent;
    }
}
