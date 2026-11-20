package net.ofatech.supernaturalbloodlines.core.boss;

/**
 * R182: the Memory Claws (Peter's reward). Used on another <b>player</b> they teach the user two facts about that player, for good: their sire and their genes
 * ({@code Knowledge.Ability.MEMORY_CLAWS}, written into the user's own profile knowledge, never into their pack's pool). What the user then reads on that player's Profile
 * page is what the discovery rules already say of them: the sire as the Bloodline page shows it (a name, "none", or "?" for an NPC Original or a creature, so no line
 * is named), and the genes the player has <b>discovered themselves</b> (a gene nobody has found stays unknown). Nothing is revealed to the target, to the user's own
 * genes, or to anybody's line, and no gene is discovered by it.
 *
 * <p>It is a way of prying, so it follows the Linking Spell: the target consents by sneaking with a lead in the main hand ({@link LinkRules#consents}), and only where PvP
 * is on and the user may harm them (no teams). Consent is asked before anything hidden is read, so a refusal tells nothing about the target, and it costs nothing; the
 * target is told that the claws were used on them. A target that is no player is refused with its own line (what it is is plain to see), and the claws never work on
 * the user themselves (finding their own genes is the rite's job). A use spends one of {@link #USES} and starts a cooldown of {@link #COOLDOWN_TICKS}.
 */
public final class MemoryClawsRules {
    public static final int USES = 12;
    public static final int COOLDOWN_TICKS = 20 * 30;
    public static final String REFUSED = "item.supernaturalbloodlines.memory_claws.refused";
    public static final String NOT_THIS = "item.supernaturalbloodlines.memory_claws.not_this";
    public static final String SELF = "item.supernaturalbloodlines.memory_claws.self";

    public enum Target { PLAYER, OTHER }

    public enum Verdict { OK, NOT_THIS, SELF, REFUSED }

    private MemoryClawsRules() {
    }

    /**
     * What happens when the claws are used on {@code target}. {@code consent} is whether the target agreed ({@link LinkRules#consents}) and {@code mayHarm} whether the
     * user may hurt them; both are only asked of a player, and only after what the target is and whether it is the user.
     */
    public static Verdict verdict(Target target, boolean self, boolean consent, boolean mayHarm) {
        if (target == Target.OTHER) {
            return Verdict.NOT_THIS;
        }
        if (self) {
            return Verdict.SELF;
        }
        return consent && mayHarm ? Verdict.OK : Verdict.REFUSED;
    }

    /** The message key of a verdict that is a refusal; null for {@link Verdict#OK}. */
    public static String message(Verdict verdict) {
        return switch (verdict) {
            case OK -> null;
            case NOT_THIS -> NOT_THIS;
            case SELF -> SELF;
            case REFUSED -> REFUSED;
        };
    }

    /** Whether a use is paid for: only the one that worked. */
    public static boolean spends(Verdict verdict) {
        return verdict == Verdict.OK;
    }
}
