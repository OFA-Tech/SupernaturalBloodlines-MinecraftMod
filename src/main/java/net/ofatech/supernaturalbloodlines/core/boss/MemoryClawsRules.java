package net.ofatech.supernaturalbloodlines.core.boss;

/**
 * R183: the Memory Claws (Peter's reward). Used on another <b>player</b> they give the user a <b>snapshot</b> of that player's sire and genes as that player knew
 * them at that moment (written to the user's own record, never the pack's; a later discovery of the target never reaches them, a second use takes a new snapshot). What the
 * user reads on that player's Profile page is what the discovery rules already show: the sire as the Bloodline page shows it (a name, "none", or "?" for an NPC
 * Original or a creature, so no line is named), and the genes the player had discovered themselves. Nothing is revealed to the target, to the user's own genes, or to
 * anybody's line, and no gene is discovered by it.
 *
 * <p>It is a way of prying, so the target must <b>agree on purpose</b>: sneak, with a lead in the main hand, <b>looking at the user</b> (the look within about 25
 * degrees of the user's eyes, and a clear line to them), where PvP is on, and the user must be allowed to hurt them (no teams, no claims). Consent is asked before
 * anything hidden is read, so a refusal tells nothing about the target and costs nothing, and every refusal says the same words. A target that is no player is refused with
 * its own line (what it is is plain to see), the claws never work on the user themselves, and where PvP is off they say so (that is the server's, not the target's).
 * A use spends one of {@link #USES} and starts a cooldown of {@link #COOLDOWN_TICKS}, which the item keeps itself.
 */
public final class MemoryClawsRules {
    public static final int USES = 12;
    public static final int COOLDOWN_TICKS = 20 * 30;
    /** The target's look and the direction to the user's eyes must agree to this much (the cosine, about 25 degrees). */
    public static final double FACING_DOT = 0.9;
    public static final String REFUSED = "item.supernaturalbloodlines.memory_claws.refused";
    public static final String NOT_THIS = "item.supernaturalbloodlines.memory_claws.not_this";
    public static final String SELF = "item.supernaturalbloodlines.memory_claws.self";
    public static final String PVP_OFF = "item.supernaturalbloodlines.memory_claws.pvp_off";
    public static final String WAIT = "item.supernaturalbloodlines.memory_claws.wait";
    public static final String USAGE = "item.supernaturalbloodlines.memory_claws.usage";

    public enum Target { PLAYER, OTHER }

    public enum Verdict { OK, NOT_THIS, SELF, PVP_OFF, REFUSED }

    private MemoryClawsRules() {
    }

    /**
     * What happens when the claws are used on {@code target}. {@code consent} is whether the target agreed ({@link LinkRules#consents}), {@code facing} whether they
     * look at the user ({@link #facing}) and {@code mayHarm} whether the user may hurt them; those are only asked of a player, and only after what the target is, whether it
     * is the user, and whether PvP is on at all.
     */
    public static Verdict verdict(Target target, boolean self, boolean pvpAllowed, boolean consent, boolean facing, boolean mayHarm) {
        if (target == Target.OTHER) {
            return Verdict.NOT_THIS;
        }
        if (self) {
            return Verdict.SELF;
        }
        if (!pvpAllowed) {
            return Verdict.PVP_OFF;
        }
        return consent && facing && mayHarm ? Verdict.OK : Verdict.REFUSED;
    }

    /** Whether the target looks at the user: the cosine of the angle between their look and the way to the user's eyes is over {@link #FACING_DOT}, and they can see them. */
    public static boolean facing(double lookDotToUser, boolean lineOfSight) {
        return lineOfSight && lookDotToUser > FACING_DOT;
    }

    /** The message key of a verdict that is a refusal; null for {@link Verdict#OK}. */
    public static String message(Verdict verdict) {
        return switch (verdict) {
            case OK -> null;
            case NOT_THIS -> NOT_THIS;
            case SELF -> SELF;
            case PVP_OFF -> PVP_OFF;
            case REFUSED -> REFUSED;
        };
    }

    /** Whether a use is paid for: only the one that worked. */
    public static boolean spends(Verdict verdict) {
        return verdict == Verdict.OK;
    }

    /** The whole seconds left of a cooldown that has {@code remainingFraction} (0 to 1) of {@link #COOLDOWN_TICKS} to run; at least one while any is left. */
    public static int secondsLeft(float remainingFraction) {
        return remainingFraction <= 0F ? 0 : Math.max(1, (int) Math.ceil(remainingFraction * COOLDOWN_TICKS / 20.0));
    }
}