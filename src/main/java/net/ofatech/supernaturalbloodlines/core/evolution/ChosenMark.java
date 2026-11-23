package net.ofatech.supernaturalbloodlines.core.evolution;

import org.jetbrains.annotations.Nullable;

/**
 * What the client knows of the skill chosen for the Rite of Mastery: only what the server has said. The hub marks a choice after the server's answer to it
 * arrives in chat, never at the click (the server may refuse), so the mark follows these four lines. Pure.
 */
public final class ChosenMark {
    public static final String CHOSEN = "supernaturalbloodlines.mastery.chosen";
    public static final String CHOSEN_IS = "supernaturalbloodlines.mastery.chosen_is";
    public static final String CLEARED = "supernaturalbloodlines.mastery.cleared";
    public static final String NONE = "supernaturalbloodlines.mastery.none";

    private ChosenMark() {
    }

    /** The mark after a system message with this key and first argument (null when it has none): the new choice, no choice, or the old one unchanged. */
    @Nullable
    public static String after(@Nullable String current, String key, @Nullable String firstArg) {
        return switch (key) {
            case CHOSEN, CHOSEN_IS -> firstArg != null ? firstArg : current;
            case CLEARED, NONE -> null;
            default -> current;
        };
    }
}