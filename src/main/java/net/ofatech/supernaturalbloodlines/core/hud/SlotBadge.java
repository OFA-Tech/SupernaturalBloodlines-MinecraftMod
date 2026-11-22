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

    /** The key badge: a round wax seal at the bottom right, a dark plate under the letter at the bottom, or a tab hanging off the top left. */
    public static Box key(Look look, Box slot, int labelW) {
        return key(look, slot, labelW, 0);
    }

    /** {@link #key(Look, Box, int)} standing {@code lift} px higher, to clear the mastery bar along the bottom edge (the Tensura theme's tab is at the top and does not move). */
    public static Box key(Look look, Box slot, int labelW, int lift) {
        int bw = badgeWidth(labelW);
        return switch (look) {
            // a wax seal at the bottom right; lifted, its lower edge is what clears the bar
            case GRIMOIRE -> new Box(slot.right() - bw - 1, slot.bottom() - lift - 12, bw, BADGE_H);
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

    /** {@link #seconds(Look, Box, int)} standing {@code lift} px higher where it is at the bottom (the Tensura theme), to clear the mastery bar. */
    public static Box seconds(Look look, Box slot, int textW, int lift) {
        int x = slot.x() + (HudLayout.SLOT - textW) / 2;
        return new Box(x, look == Look.TENSURA ? slot.bottom() - 10 - lift : slot.y() + 2, textW, TEXT_H);
    }

    /** The mark of Tensura mode: a small diamond on the top right corner, mostly outside the tile, above where the seconds stand. */
    public static Box tensuraMark(Box slot) {
        return new Box(slot.right() - 3, slot.y() - 5, MARK, MARK);
    }

    /** A cooldown of 100 seconds or more is shown in minutes, so it never grows wider than two digits and a letter. */
    public static String timeLabel(int seconds) {
        return seconds < 100 ? seconds + "s" : Math.min(99, (seconds + 59) / 60) + "m";
    }
}
