package net.ofatech.supernaturalbloodlines.core.rite;

/**
 * The names of what the unlock is built from (plan {@code rites-unlock}, section 1): per gated rite two displayless advancements, {@code known} and
 * {@code sealed}, and two book entries, the rite's own and a "???" one. Exactly one of the two advancements is done for a player. Pure.
 */
public final class RiteAdvancements {
    public static final String MODID = "supernaturalbloodlines";
    /** The one criterion of every unlock advancement ({@code minecraft:impossible}: only the server awards it). */
    public static final String CRITERION = "unlock";

    private RiteAdvancements() {
    }

    /** {@code supernaturalbloodlines:rites/known/<rite>}: the player has learned the rite. */
    public static String known(String rite) {
        return MODID + ":rites/known/" + rite;
    }

    /** {@code supernaturalbloodlines:rites/sealed/<rite>}: the player has not, so the book shows the "???" entry. */
    public static String sealed(String rite) {
        return MODID + ":rites/sealed/" + rite;
    }

    /** The rite's own entry in the book ({@code entries/<this>}). */
    public static String entry(String rite) {
        return MODID + "/rites/" + rite;
    }

    /** The "???" entry that stands for it while it is sealed. */
    public static String sealedEntry(String rite) {
        return MODID + "/sealed/" + rite;
    }

    /** The rite an advancement id belongs to, or null when it is not one of ours. */
    public static String riteOf(String advancementId) {
        for (String prefix : new String[] {MODID + ":rites/known/", MODID + ":rites/sealed/"}) {
            if (advancementId.startsWith(prefix)) {
                return advancementId.substring(prefix.length());
            }
        }
        return null;
    }
}
