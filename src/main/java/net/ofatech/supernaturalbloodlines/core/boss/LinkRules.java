package net.ofatech.supernaturalbloodlines.core.boss;

/**
 * R177: the Linking Spell. A witch-type performer, exactly two living creatures in the circle, neither an NPC Original (a player Original may be linked, and consents like anyone), neither a boss, neither
 * linked already, and every player among them (but the performer, who is agreeing by working the rite) sneaking inside the circle with a lead in the main
 * hand to consent, and only where PvP is on. For {@code link_seconds} part of the damage one takes ({@code link_damage_share}) is dealt to the other, never again for that damage (no
 * loops). It ends when either dies or leaves the dimension.
 *
 * <p>Every reason the circle will not link is <b>one answer</b>, {@link #WONT_HOLD}, and every one costs the same (the lead, the amethyst and the altar's
 * power; the grimoire always comes back), so a refusal never tells a player who is a boss, or whether a creature was linked already. The
 * wrong-count case is explained in the Guide, not in the message. This is deliberate. A partner that is not loaded takes what it owes when it loads, but
 * only a creature that is not a player, and at most half its health. Pure.
 */
public final class LinkRules {
    /** The one plain answer for every refusal of the circle. */
    public static final String WONT_HOLD = "supernaturalbloodlines.refusal.link_wont_hold";
    /** The performer is not a witch type: this says nothing about the circle, so the ingredients are given back. */
    public static final String NOT_WITCH = "supernaturalbloodlines.refusal.link_not_witch";
    /** What can be stored for a partner that is not loaded. */
    public static final float MAX_PENDING = 20.0F;
    /** The most of its health a partner can take from what it owed, when it loads. */
    public static final float SETTLE_FRACTION = 0.5F;

    private LinkRules() {
    }

    /** Whether the circle refuses. Every reason is the same answer. */
    public static boolean refuses(int creatures, boolean anyNpcOriginal, boolean anyBoss, boolean anyLinked, boolean anyWithoutConsent) {
        return creatures != 2 || anyNpcOriginal || anyBoss || anyLinked || anyWithoutConsent;
    }

    /** A player in the circle consents when they are the performer, or sneak inside it holding a lead in the main hand (a deliberate act) and PvP is on. */
    public static boolean consents(boolean isPerformer, boolean sneaking, boolean holdingLead, boolean pvpAllowed) {
        return isPerformer || sneaking && holdingLead && pvpAllowed;
    }

    /** What the damage one linked creature takes passes to the other: the share at depth 0, nothing for damage that is itself a share. */
    public static float share(float damage, int depth, double fraction) {
        if (depth >= 1 || damage <= 0F) {
            return 0F;
        }
        return (float) (damage * Math.max(0.0, Math.min(1.0, fraction)));
    }

    public static float addPending(float pending, float damage) {
        return Math.min(MAX_PENDING, Math.max(0F, pending) + Math.max(0F, damage));
    }

    /** What a creature that loads takes of what it owed: never more than half its maximum health. */
    public static float settleCap(float owed, float maxHealth) {
        return Math.max(0F, Math.min(owed, maxHealth * SETTLE_FRACTION));
    }

    public static boolean active(long until, long now) {
        return until > now;
    }
}