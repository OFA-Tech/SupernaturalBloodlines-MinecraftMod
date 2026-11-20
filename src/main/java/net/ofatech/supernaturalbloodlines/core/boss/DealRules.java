package net.ofatech.supernaturalbloodlines.core.boss;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcOriginal;

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

    /** Whether the oath still binds: sworn (not {@link #NEVER}) and not past its last day. A broken oath is recorded as {@link #BROKEN}, which never binds. */
    public static boolean binding(long until, long today) {
        return until != NEVER && today < until;
    }

    /** A player is offered the deal once, ever: only one with no record at all. */
    public static boolean offers(long until, boolean elijahHostile, boolean elijahOpen) {
        return until == NEVER && !elijahHostile && !elijahOpen;
    }

    /** The victim of a blow, or of a desiccation: the oath is broken when the victim is an NPC Original other than Mikael (Elijah is one: striking him breaks it too). */
    public static boolean breaks(BossId victim) {
        return victim != BossId.MIKAEL && victim.npcOriginal().isPresent();
    }

    /** {@code broken(oath, event)}: the oath is broken by this victim only while it binds. */
    public static boolean broken(long until, long today, BossId victim) {
        return binding(until, today) && breaks(victim);
    }

    /**
     * Whether a blow counts against the oath of {@code owner}: their own always does; a blow of something they tame (or a minion) only when the Original
     * was not already after them, because then the animal is defending its owner and the owner has done nothing.
     */
    public static boolean counts(boolean byOwnerThemselves, boolean originalWasAfterOwner) {
        return byOwnerThemselves || !originalWasAfterOwner;
    }

    /**
     * What the offer lists, in order: every NPC Original but Elijah, the same for everybody. Leaving one out for a player would tell them, by the gap,
     * which line they are in before they have discovered it. Picking their own line's Original is allowed: it gives a vial with a name on it, nothing more.
     */
    public static List<NpcOriginal> options() {
        List<NpcOriginal> out = new ArrayList<>();
        for (NpcOriginal o : NpcOriginal.values()) {
            if (o != NpcOriginal.ELIJAH) {
                out.add(o);
            }
        }
        return out;
    }

    /** The Original this id names, when the offer lists it. */
    public static Optional<NpcOriginal> choose(String id) {
        return options().stream().filter(o -> o.id().equals(id)).findFirst();
    }
}
