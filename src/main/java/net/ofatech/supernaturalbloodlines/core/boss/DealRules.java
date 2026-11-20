package net.ofatech.supernaturalbloodlines.core.boss;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcOriginal;
import org.jetbrains.annotations.Nullable;

/**
 * R167: Elijah's deal. He gives a vial of the blood of one NPC Original (never his own) for an oath that lasts {@code elijah_oath_days}: not to harm
 * or desiccate any NPC Original other than Mikael, and not to attack Elijah. Breaking it turns him hostile for good and the deal is gone. A player
 * is offered one deal, ever. Pure rules; the game side keeps the oath in {@code BossIndex}.
 */
public final class DealRules {
    /** A player who has no oath record: never dealt with Elijah. */
    public static final long NEVER = 0L;
    /** The record of a player who broke the oath: it never binds, and they are never offered again. */
    public static final long BROKEN = -1L;

    private DealRules() {
    }

    /** The day an oath sworn today runs out (it holds through that day's end: sworn on day 10 for 7 days, it holds through day 16). */
    public static long sworn(long today, int days) {
        return today + Math.max(1, days);
    }

    /** Whether the oath still binds: sworn (not {@link #NEVER}) and not past its last day. A broken oath is removed from the record, so it never binds again. */
    public static boolean binding(long until, long today) {
        return until != NEVER && today < until;
    }

    /** A player is offered the deal once, ever: only one with no record at all. */
    public static boolean offers(long until, boolean elijahHostile, boolean elijahOpen) {
        return until == NEVER && !elijahHostile && !elijahOpen;
    }

    /** The victim of a blow, or of a desiccation: the oath is broken when the victim is Elijah or an NPC Original other than Mikael. */
    public static boolean breaks(BossId victim) {
        return victim == BossId.ELIJAH || victim != BossId.MIKAEL && victim.npcOriginal().isPresent();
    }

    /** {@code broken(oath, event)}: the oath is broken by this victim only while it binds. */
    public static boolean broken(long until, long today, BossId victim) {
        return binding(until, today) && breaks(victim);
    }

    /**
     * What the offer lists, in order: every NPC Original but Elijah, except the one whose line the player is in when they have not discovered it
     * ({@code ownRoot} not equal to {@code knownRoot}): the vial would name it.
     */
    public static List<NpcOriginal> options(@Nullable UUID ownRoot, @Nullable UUID knownRoot, boolean originalsAlwaysVisible) {
        List<NpcOriginal> out = new ArrayList<>();
        for (NpcOriginal o : NpcOriginal.values()) {
            if (o == NpcOriginal.ELIJAH) {
                continue;
            }
            boolean own = o.uuid().equals(ownRoot);
            if (own && !originalsAlwaysVisible && !o.uuid().equals(knownRoot)) {
                continue;
            }
            out.add(o);
        }
        return out;
    }
}