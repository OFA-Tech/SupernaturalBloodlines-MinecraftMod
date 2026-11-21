package net.ofatech.supernaturalbloodlines.shared.hub;

/**
 * What a link from the hub to a rite does (plan {@code rites-unlock}): the rites live in Enchanted's book, which the player must carry; a rite they have
 * not learned opens its "???" entry, not its own. Pure.
 */
public final class RiteLinkRule {
    public enum Open { NEED_BOOK, ENTRY, SEALED }

    private RiteLinkRule() {
    }

    /**
     * @param hasBook The Art of Witchcraft is in the player's inventory (Modopedia's own rule, the one its study feature uses)
     * @param gated   the rite is one that has to be learned
     * @param known   the player has learned it
     */
    public static Open decide(boolean hasBook, boolean gated, boolean known) {
        if (!hasBook) {
            return Open.NEED_BOOK;
        }
        return gated && !known ? Open.SEALED : Open.ENTRY;
    }
}
