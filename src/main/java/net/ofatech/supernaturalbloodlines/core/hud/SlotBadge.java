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

    private SlotBadge() {
    }

    /** The key badge: a round wax seal at the bottom right, a dark plate under the letter at the bottom, or a tab hanging off the top left. */
    public static Box key(Look look, Box slot, int labelW) {
        return key(look, slot, labelW, 0);
    }

    /** {@link #key(Look, Box, int)} standing {@code lift} px higher, to clear the mastery bar along the bottom edge (the Tensura theme's tab is at the top and does not move). */
    public static Box key(Look look, Box slot, int labelW, int lift) {
        return switch (look) {
            case GRIMOIRE -> {
                int bw = Math.max(11, labelW + 5);
                // lifted, the seal's lower edge is what clears the bar, however wide the key text makes it
                yield new Box(slot.right() - bw - 1, slot.bottom() - (lift == 0 ? 12 : lift + Math.max(12, bw + 1)), bw, bw);
            }
            case BLOOD_MOON -> new Box(slot.x() + (HudLayout.SLOT - labelW) / 2 - 2, slot.bottom() - 10 - lift, labelW + 4, 10);
            case TENSURA -> new Box(slot.x() - 2, slot.y() - 3, labelW + 4, 11);
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

    /** The mark of Tensura mode: a small diamond on the top right corner, mostly outside the tile. */
    public static Box tensuraMark(Box slot) {
        return new Box(slot.right() - 2, slot.y() - 3, 5, 5);
    }

    /** A cooldown of 100 seconds or more is shown in minutes, so it never grows wider than two digits and a letter. */
    public static String timeLabel(int seconds) {
        return seconds < 100 ? seconds + "s" : Math.min(99, (seconds + 59) / 60) + "m";
    }
}
