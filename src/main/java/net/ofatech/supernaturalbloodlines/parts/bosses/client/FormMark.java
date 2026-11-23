package net.ofatech.supernaturalbloodlines.parts.bosses.client;

import java.util.Locale;

/** The mark a token carries for the form of its boss, so the form is not told by colour alone: the first letter of the form's word, in capitals. Pure. */
public final class FormMark {
    private FormMark() {
    }

    public static String of(String word) {
        if (word == null || word.isBlank()) {
            return "";
        }
        String s = word.strip();
        return new String(Character.toChars(s.codePointAt(0))).toUpperCase(Locale.ROOT);
    }
}
