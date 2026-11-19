package net.ofatech.supernaturalbloodlines.core.bloodline;

import net.ofatech.supernaturalbloodlines.core.bloodline.NpcBond.Order;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcBond.Source;

/**
 * The rules of the pack's NPC members (R148, R149): who may be recruited, what a member weighs toward the pack bonus, what it does when its owner is
 * away, who may give it orders. Pure tables, one test per row.
 */
public final class NpcRules {
    /** A wild wolf has been beaten, not lured, when it is at or under this share of its health. */
    public static final float BEATEN = 0.5f;

    /** The pack bonus' step and cap (R94); a test pins them to {@code PackBonus}. */
    public static final double STEP = 0.05;
    public static final int CAP = 5;

    public enum Refusal { NONE, OFF, NOT_WEREWOLF, NOT_A_WOLF, ALPHA, TRIAL_BOSS, ALREADY_BONDED, NOT_BEATEN, OWNER_CAP, PACK_CAP }

    private NpcRules() {
    }

    /**
     * Whether {@code recruiter} may take this mob into their pack with the Werewolf Brother rite. Checked in this order, so the message says the first
     * thing that is wrong.
     *
     * @param healthFraction the mob's health over its maximum
     * @param ownerCount     NPC members the recruiter has now (of every source that counts against the cap)
     * @param packCount      NPC members the recruiter's whole pack has now
     */
    public static Refusal recruit(boolean enabled, boolean recruiterWerewolf, boolean wolfMob, boolean alpha, boolean trialBoss, boolean bonded,
                                  float healthFraction, int ownerCount, int ownerMax, int packCount, int packMax) {
        if (!enabled) {
            return Refusal.OFF;
        }
        if (!recruiterWerewolf) {
            return Refusal.NOT_WEREWOLF;
        }
        if (!wolfMob) {
            return Refusal.NOT_A_WOLF;
        }
        if (trialBoss) {
            return Refusal.TRIAL_BOSS;
        }
        if (alpha) {
            return Refusal.ALPHA;
        }
        if (bonded) {
            return Refusal.ALREADY_BONDED;
        }
        if (healthFraction > BEATEN) {
            return Refusal.NOT_BEATEN;
        }
        if (ownerCount >= Math.max(0, ownerMax)) {
            return Refusal.OWNER_CAP;
        }
        if (packCount >= Math.max(0, packMax)) {
            return Refusal.PACK_CAP;
        }
        return Refusal.NONE;
    }

    /** What one member weighs toward the pack bonus: a Brother {@code brother}, a Tensura subordinate {@code tensura}, a Wolf Spirit nothing. */
    public static double weight(Source source, double brother, double tensura) {
        return switch (source) {
            case BROTHER -> Math.max(0, brother);
            case TENSURA -> Math.max(0, tensura);
            case SPIRIT -> 0;
        };
    }

    /**
     * The pack bonus in steps of 5% (R94 with NPC members; the numbers are the pack bonus' own). Players count as before: the raw count against {@code minNeeded}, the bitten ones twice in the sum.
     * NPC members count by weight in both, but their share of the sum is capped at {@code share} of the cap ({@link PackBonus#CAP} steps), so no lone Alpha
     * farms the bonus from wolves.
     *
     * @return the bonus as a fraction (0.05 per step), 0 below the minimum
     */
    public static double bonus(int rawPlayers, int doubledPlayers, double npcWeight, int minNeeded, double share) {
        double npc = Math.max(0, npcWeight);
        if (rawPlayers + npc < minNeeded) {
            return 0;
        }
        double cappedNpc = Math.min(npc, Math.max(0, Math.min(1, share)) * CAP);
        double steps = Math.min(CAP, doubledPlayers + cappedNpc);
        return Math.round(STEP * steps * 1000) / 1000.0;
    }

    /**
     * Whether a Brother goes on a hunting trip now: the switch is on, it has Hunt on, it is not following (it rests or guards, so it has a post to come
     * back to), it is night, the time since its last trip is up, and it is not in a fight.
     */
    public static boolean huntDue(boolean enabled, boolean hunt, Order effective, boolean night, long ticksSinceLast, long periodTicks, boolean fighting) {
        return enabled && hunt && effective != Order.FOLLOW && night && ticksSinceLast >= periodTicks && !fighting;
    }

    /** What a member really does now: its owner offline or dead means it rests where it stands; the bond itself is kept. */
    public static Order effectiveOrder(Order order, boolean ownerOnline, boolean ownerAlive) {
        return ownerOnline && ownerAlive ? order : Order.REST;
    }

    /** Who may give a member orders (or release it): its owner, or the leader of the pack the owner is in. */
    public static boolean mayOrder(boolean sendsAsOwner, boolean sendsAsLeaderOfOwnersPack) {
        return sendsAsOwner || sendsAsLeaderOfOwnersPack;
    }

    /** A member the owner can still be told about: of a source whose orders are ours (a Tensura subordinate keeps Tensura's). */
    public static boolean hasOurOrders(Source source) {
        return source == Source.BROTHER;
    }

    /**
     * Whether a Tensura subordinate is a member of a pack now (R149): the bridge is on, it is tamed, its Tensura owner is a player in a pack or a Brother of
     * one (one step only: the subordinate of a subordinate never joins), and the pack has room unless it is a member already (a member is never put out
     * for the cap).
     */
    public static boolean tensuraJoins(boolean enabled, boolean tame, boolean ownerInPack, boolean ownerIsBrotherOfPackMember, boolean alreadyMember,
                                       int countInPack, int max) {
        if (!enabled || !tame || !(ownerInPack || ownerIsBrotherOfPackMember)) {
            return false;
        }
        return alreadyMember || countInPack < Math.max(0, max);
    }

    /** Only a player takes a spark (R148): the credit of a pack member (a Brother, a subordinate) never reaches the owner or anyone. */
    public static boolean mayTakeSpark(boolean killerIsPlayer, boolean killerIsMember) {
        return killerIsPlayer && !killerIsMember;
    }
}