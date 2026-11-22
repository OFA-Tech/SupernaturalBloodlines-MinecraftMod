package net.ofatech.supernaturalbloodlines.core.hud;

import net.ofatech.supernaturalbloodlines.core.hud.HudLayout.Box;

/**
 * Where the small things on a slot stand, in each theme, so they can be tested not to cover each other: the key badge, the seconds of a cooldown and the
 * mark of Tensura mode. {@code SlotArt} draws exactly these boxes. The seconds stand opposite the badge: at the top for the two themes whose badge is at the
 * bottom (Grimoire's wax seal, Blood Moon's letter plate), at the bottom for the Tensura theme, whose key tab hangs off the top left. Pure.
 */
public final class SlotBadge {
    /** The hub themes, in the order of {@code HudStyle.Style}. */
    public enum Look { GRIMOIRE, BLOOD_MOON, TENSURA }

    /** A letter is 8 pixels tall; the font's digits are 6 wide and a letter 5 to 7. */
    public static final int TEXT_H = 8;
    public static final int CHAR_W = 6;
    /** The key badges of the kit: 11 x 11 for one character, 17 x 11 for two. */
    public static final int BADGE_H = 11;
    public static final int BADGE_W = 11;
    public static final int BADGE_WIDE = 17;
    /** The mark of Tensura mode: 7 x 7. */
    public static final int MARK = 7;

    /** The badge's width: the one-character picture, or the wide one. */
    public static int badgeWidth(int labelW) {
        return labelW <= CHAR_W ? BADGE_W : BADGE_WIDE;
    }

    private SlotBadge() {
    }

    /**
     * The look whose badge and seconds stand on a slot. The Blood Moon's plate sits at the bottom of a round slot; on the square slots of Tensura mode it would cover
     * half of the icon, so there it hangs off the top left like the Tensura theme's tab.
     */
    public static Look placement(Look look, boolean squareMode) {
        return look == Look.BLOOD_MOON && squareMode ? Look.TENSURA : look;
    }

    /** The key badge: a round wax seal at the bottom right, a dark plate under the letter at the bottom, or a tab hanging off the top left. */
    public static Box key(Look look, Box slot, int labelW) {
        return key(look, slot, labelW, 0);
    }

    /** {@link #key(Look, Box, int)} standing {@code lift} px higher, to clear the mastery bar along the bottom edge (the Tensura theme's tab is at the top and does not move). */
    public static Box key(Look look, Box slot, int labelW, int lift) {
        int bw = badgeWidth(labelW);
        return switch (look) {
            // a wax seal at the bottom right; lifted, it stands flush above the bar
            case GRIMOIRE -> new Box(slot.right() - bw - 1, slot.bottom() - lift - (lift > 0 ? BADGE_H : BADGE_H + 1), bw, BADGE_H);
            // a plate at the bottom, centred
            case BLOOD_MOON -> new Box(slot.x() + (HudLayout.SLOT - bw) / 2, slot.bottom() - BADGE_H - lift, bw, BADGE_H);
            // a tab hanging off the top left
            case TENSURA -> new Box(slot.x() - 2, slot.y() - 3, bw, BADGE_H);
        };
    }

    /** The cooldown's text, centred, away from the badge. */
    public static Box seconds(Look look, Box slot, int textW) {
        return seconds(look, slot, textW, 0);
    }

    /** What the seconds are drawn at when the badge is lifted over a mastery bar: at the top (Grimoire, Blood Moon) there is no room left for full size beside the badge. */
    public static final float SECONDS_SCALE = 0.75f;
    /** The rows of ink of a digit at that size (5 of the 7 it has at full size). */
    public static final int SECONDS_INK_SMALL = 5;

    /** The scale of the seconds' text: smaller only at the top of a slot whose badge is lifted. */
    public static float secondsScale(Look look, int lift) {
        return lift > 0 && look != Look.TENSURA ? SECONDS_SCALE : 1f;
    }

    /**
     * {@link #seconds(Look, Box, int)} for a slot with a mastery bar ({@code lift} > 0): the Tensura theme's stand at the bottom, above the bar; the other two at
     * the top, at {@link #SECONDS_SCALE} (pass the width at that scale), so they end above the badge, which is lifted by the bar.
     */
    public static Box seconds(Look look, Box slot, int textW, int lift) {
        int x = slot.x() + (HudLayout.SLOT - textW) / 2;
        if (look == Look.TENSURA) {
            return new Box(x, slot.bottom() - 10 - lift, textW, TEXT_H);
        }
        return lift > 0 ? new Box(x, slot.y() + 1, textW, SECONDS_INK_SMALL) : new Box(x, slot.y() + 2, textW, TEXT_H);
    }

    /** The mark of Tensura mode: a small diamond on the top right corner, mostly outside the tile, above where the seconds stand. */
    public static Box tensuraMark(Box slot) {
        return new Box(slot.right() - 3, slot.y() - 5, MARK, MARK);
    }

    /** A cooldown of 100 seconds or more is shown in minutes, so it never grows wider than two digits and a letter. */
    public static String timeLabel(int seconds) {
        return seconds < 100 ? seconds + "s" : Math.min(99, (seconds + 59) / 60) + "m";
    }

    /**
     * What the badge says for a key: one or two characters. A long key name is shortened to what the player knows it by: the initials of two words
     * ("Left Shift" is LS), the first letter and the number of a numbered key ("Mouse Button 4" is M4, "Keypad 1" is K1), else its first two characters.
     */
    public static String shortKey(String label) {
        String s = label.strip();
        if (s.length() <= 2) {
            return s;
        }
        String[] words = s.split("\\s+");
        if (words.length == 1) {
            return s.substring(0, 1).toUpperCase(java.util.Locale.ROOT) + s.substring(1, 2);
        }
        String last = words[words.length - 1];
        String initial = words[0].substring(0, 1).toUpperCase(java.util.Locale.ROOT);
        if (last.chars().allMatch(Character::isDigit)) {
            return initial + last.charAt(last.length() - 1);
        }
        return initial + last.substring(0, 1).toUpperCase(java.util.Locale.ROOT);
    }
}