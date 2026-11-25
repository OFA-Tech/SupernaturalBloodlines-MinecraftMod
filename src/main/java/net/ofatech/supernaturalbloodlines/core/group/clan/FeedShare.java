package net.ofatech.supernaturalbloodlines.core.group.clan;

/** R274: what each clanmate gets from a feed: a percent of the amount, at least 1 while there is blood, never above the max. */
public final class FeedShare {
    private FeedShare() {
    }

    public static int of(int amount, int percent, int max) {
        if (amount <= 0) {
            return 0;
        }
        long share = (long) amount * percent / 100;
        return (int) Math.min(max, Math.max(1, share));
    }
}