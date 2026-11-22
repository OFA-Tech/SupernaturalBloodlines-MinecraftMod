package net.ofatech.supernaturalbloodlines.core.bloodline;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** R203: how much blood an Augustine takes from one drink of Vampirism's. Pure. */
public final class AugustineBloodRules {
    /** A bite of the same biter on the same victim within this many ticks is the same feed (the same number as Keen Fangs' window). */
    public static final long CHAIN_TICKS = 5 * 20;

    private AugustineBloodRules() {
    }

    /**
     * Blood an Augustine gets from one drink of Vampirism's {@code amount}: times {@code fromVampire} when the victim is a vampire, times {@code fromOther}
     * otherwise. Nothing stays nothing (a bite something refused is still refused). The product is rounded at random, {@code roll} being a uniform number in
     * [0, 1): a third of 1 is 1 in a third of the bites and 0 in the rest, so a factor of 1/3 is 1/3 on average and not a floor of 1.
     */
    public static int scale(int amount, boolean victimIsVampire, double fromVampire, double fromOther, double roll) {
        double factor = victimIsVampire ? fromVampire : fromOther;
        if (amount <= 0 || factor <= 0) {
            return 0;
        }
        double exact = amount * factor;
        long whole = (long) Math.floor(exact);
        long rounded = whole + (roll < exact - whole ? 1 : 0);
        return (int) Math.min(Integer.MAX_VALUE, rounded);
    }

    /** Garlic Ward: garlic's own effect is denied, and blindness only when it comes from standing in strong garlic (a blindness potion is not the ward's business). */
    public static boolean wardDenies(boolean garlicEffect, boolean blindness, boolean inStrongGarlic) {
        return garlicEffect || blindness && inStrongGarlic;
    }

    /** Which bites are the first of their feed: the extra of a bite on a vampire is paid once per feed, not once per bite. */
    public static final class FeedChain {
        private final Map<Key, Long> last = new HashMap<>();

        private record Key(UUID biter, UUID victim) {
        }

        /** True when this bite starts a feed: the same pair has not bitten within {@link #CHAIN_TICKS}. Every bite renews the window. */
        public boolean firstBite(UUID biter, UUID victim, long now) {
            last.values().removeIf(t -> now - t > CHAIN_TICKS);
            Long before = last.put(new Key(biter, victim), now);
            return before == null || now - before > CHAIN_TICKS;
        }

        /**
         * Whether this drink starts a feed (the x3 is paid): it must give something on a vampire, and be the first of its pair within the window. A drink that gave
         * nothing, or was on someone who is not a vampire, never touches the window (a refused bite must not start the feed that follows).
         */
        public boolean startsFeed(int given, boolean victimIsVampire, UUID biter, UUID victim, long now) {
            return given > 0 && victimIsVampire && firstBite(biter, victim, now);
        }

        public void clear() {
            last.clear();
        }
    }
}
