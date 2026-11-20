package net.ofatech.supernaturalbloodlines.core.boss;

import java.util.Optional;

/**
 * R177: the Linking Spell. Exactly two living creatures stand in the circle, neither an Original (NPC or player); for {@code link_seconds} part of the damage
 * one takes ({@code link_damage_share}) is dealt to the other, never again for that damage (no loops). It ends when either dies or leaves the dimension.
 * A partner that is not loaded takes the damage when it loads, up to {@link #MAX_PENDING}. Pure.
 */
public final class LinkRules {
    public static final String NEEDS_TWO = "supernaturalbloodlines.refusal.link_needs_two";
    /** One answer for "an Original is in the circle" and "one of them is linked already": the first would say who is an Original. */
    public static final String WONT_HOLD = "supernaturalbloodlines.refusal.link_wont_hold";
    public static final float MAX_PENDING = 20.0F;

    private LinkRules() {
    }

    /** The first reason the circle cannot link, as a lang key. */
    public static Optional<String> refusal(int creatures, boolean anyOriginal, boolean anyLinked) {
        if (creatures != 2) {
            return Optional.of(NEEDS_TWO);
        }
        return anyOriginal || anyLinked ? Optional.of(WONT_HOLD) : Optional.empty();
    }

    /** What the partner takes of {@code damage}: the share at depth 0, nothing for damage that is itself a share (depth 1 or more). */
    public static float share(float damage, int depth, double fraction) {
        if (depth >= 1 || damage <= 0F) {
            return 0F;
        }
        return (float) (damage * Math.max(0.0, Math.min(1.0, fraction)));
    }

    public static float addPending(float pending, float damage) {
        return Math.min(MAX_PENDING, Math.max(0F, pending) + Math.max(0F, damage));
    }

    public static boolean active(long until, long now) {
        return until > now;
    }
}
