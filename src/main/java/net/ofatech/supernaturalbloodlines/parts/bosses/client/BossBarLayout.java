package net.ofatech.supernaturalbloodlines.parts.bosses.client;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.function.ToIntFunction;
import net.ofatech.supernaturalbloodlines.core.hud.TrackerAnchor;

/**
 * Where everything of a boss bar goes (the approved card of the style guide): a 250 x 46 card with the portrait plate on the left, the name plate over the
 * health bar, and under the bar the health in numbers on the left and the form chips on the right; cards stack with a 6 px gap. Every coordinate is relative to the
 * card's top left corner. Pure, so the client computes a card's {@link Geometry} once and not every frame.
 */
public final class BossBarLayout {
    public static final int CARD_W = 250;
    public static final int CARD_H = 46;
    public static final int GAP = 6;
    /** The screen keeps this much clear of a card's sides. */
    public static final int SCREEN_MARGIN = 4;
    /** The portrait plate's body, and the portrait inside it. */
    public static final int PLATE = 34;
    public static final int ART = 24;
    public static final int NAME_H = 13;
    public static final int TRACK_H = 9;
    public static final int FILL_H = 7;
    public static final int CHIP_H = 10;
    /** A chip is its text and this much (3 px each side). */
    public static final int CHIP_PAD = 6;
    public static final int CHIP_GAP = 2;
    /** The stack takes at most this share of the screen's height (three tenths and a half: 3/5), like vanilla's limit but wide enough for three cards. */
    private static final int SHARE_NUM = 3;
    private static final int SHARE_DEN = 5;
    /** The card's left edge to the name plate and the bar: the portrait plate and its air. */
    private static final int BODY_X = 45;

    /** Where text starts on its picture: the name 8 px in and 3 down on its plate, a chip's word 2 down from the top of the chip's body. */
    public static final int NAME_TEXT_X = 8;
    public static final int NAME_TEXT_Y = 3;
    public static final int CHIP_TEXT_Y = 2;
    /** The default font: capitals and digits cover rows 0 to 6 of their 8 px cell, a descender (j, g, y) and a mark over a letter reach row 7 and row 0. */
    public static final int CAP_TOP = 0;
    public static final int CAP_BOTTOM = 6;
    public static final int DESCENDER_BOTTOM = 7;

    private BossBarLayout() {
    }

    /** Every place inside a card. {@code chipX}/{@code chipW}: width 0 is a chip that is not drawn. */
    public record Geometry(int cardW, int plateX, int plateY, int artX, int artY, int nameX, int nameY, int nameW, int trackX, int trackY, int trackW,
                           int fillX, int fillY, int fillMaxW, int[] tickX, int hpX, int hpY, int hpMaxW, int chipY, int[] chipX, int[] chipW) {
    }

    /** Another mod's status corner (Tensura's and Vampirism's bars, top left) is this wide, with a gap; the stack stands to its right when the screen has room. */
    public static final int STATUS_CLEAR = TrackerAnchor.CORNER_W + 4;
    /** Narrower than this a card is not worth making: it then stays centred and may reach over the corner. */
    public static final int MIN_CARD_W = 170;
    /** Another mod's bar draws its name 9 px above itself: it needs this much under the last card (two pixels of air). */
    public static final int VANILLA_HEAD = 11;

    /** How far from the left edge the stack may start: clear of the status corner when it is there and the screen is wide enough for a card beside it. */
    public static int leftClear(int guiW, boolean statusCorner) {
        return statusCorner && guiW - STATUS_CLEAR - SCREEN_MARGIN >= MIN_CARD_W ? STATUS_CLEAR : SCREEN_MARGIN;
    }

    public static int cardWidth(int guiW, boolean statusCorner) {
        return Math.min(CARD_W, guiW - leftClear(guiW, statusCorner) - SCREEN_MARGIN);
    }

    /** Centred, but never nearer the left edge than {@link #leftClear}. */
    public static int cardX(int guiW, int cardW, boolean statusCorner) {
        return Math.max((guiW - cardW) / 2, leftClear(guiW, statusCorner));
    }

    public static int cardWidth(int guiW) {
        return cardWidth(guiW, false);
    }

    public static int cardX(int guiW, int cardW) {
        return cardX(guiW, cardW, false);
    }

    /** What the first card tells vanilla it took, for {@code bars} cards: the next bar of another mod starts under the last card with room for its name above it. */
    public static int increment(int bars) {
        return bars <= 0 ? 0 : (bars - 1) * (CARD_H + GAP) + CARD_H + VANILLA_HEAD;
    }

    public static int cardY(int base, int index) {
        return base + index * (CARD_H + GAP);
    }

    /** What {@code bars} cards take going down, with the gap after the last: the increment that puts another mod's vanilla bar under them. */
    public static int stackHeight(int bars) {
        return bars <= 0 ? 0 : bars * (CARD_H + GAP);
    }

    public static int bottom(int base, int bars) {
        return base + stackHeight(bars);
    }

    /** How many cards fit in the top share of a screen {@code guiH} high when the stack starts at {@code base}; one at least. */
    public static int barsThatFit(int guiH, int base) {
        return Math.max(1, (guiH * SHARE_NUM / SHARE_DEN - base + GAP) / (CARD_H + GAP));
    }

    /**
     * The card for a width and the widths of the chip texts ({@code active}: the lit chip). When the chips do not all fit on the name plate's width, only the lit
     * one is drawn, so a chip never leaves the plate and never covers the health.
     */
    public static Geometry geometry(int cardW, int[] chipTextW, int active) {
        int nameW = cardW - BODY_X - 5;
        int[] chipX = new int[chipTextW.length];
        int[] chipW = new int[chipTextW.length];
        int right = BODY_X + nameW - 4;
        int total = 0;
        for (int w : chipTextW) {
            total += w + CHIP_PAD + CHIP_GAP;
        }
        total = Math.max(0, total - CHIP_GAP);
        // the health text keeps at least a short number's room: chips that would take it all are cut down to the lit one
        boolean all = total <= nameW - 4 - MIN_HEALTH_W;
        int left = right;
        for (int i = chipTextW.length - 1; i >= 0; i--) {
            boolean shown = all || i == active;
            int w = shown ? chipTextW[i] + CHIP_PAD : 0;
            chipW[i] = w;
            chipX[i] = right - w;
            if (shown) {
                left = chipX[i];
                right = chipX[i] - CHIP_GAP;
            }
        }
        int hpX = BODY_X + 2;
        int hpMaxW = Math.max(0, (chipTextW.length == 0 ? BODY_X + nameW : left - HP_GAP) - hpX);
        int trackX = BODY_X;
        int[] tickX = {trackX + Math.round((nameW) * 0.25f), trackX + Math.round(nameW * 0.5f), trackX + Math.round(nameW * 0.75f)};
        return new Geometry(cardW, 5, 6, 10, 11, BODY_X, 5, nameW, trackX, 21, nameW, trackX + 1, 22, nameW - 2, tickX, hpX, 33, hpMaxW, 32, chipX, chipW);
    }

    /** The health text keeps at least this much width before chips are cut down to the lit one. */
    public static final int MIN_HEALTH_W = 36;
    /** The air between the end of the health text and the first chip: with the default font "1,640 / 2,000" (66 px) just fits beside three chips on the approved card. */
    public static final int HP_GAP = 2;

    /** The width of a fill for a fraction of the bar: none for no health, a sliver for any, all of it for full. */
    public static int fillWidth(int maxW, float progress) {
        if (!(progress > 0f)) {
            return 0;
        }
        return Math.min(maxW, Math.max(1, Math.round(maxW * Math.min(1f, progress))));
    }

    /** The health as it is written: both numbers, or just the current one when the room is short. */
    public record Health(String full, String compact) {
    }

    public static Health health(int hp, int max) {
        NumberFormat f = NumberFormat.getIntegerInstance(Locale.ROOT);
        String now = f.format(hp);
        return new Health(now + " / " + f.format(max), now);
    }

    /** The longest form of the health that fits {@code maxW}, or empty. */
    public static String healthText(Health h, int maxW, ToIntFunction<String> measure) {
        if (measure.applyAsInt(h.full()) <= maxW) {
            return h.full();
        }
        return measure.applyAsInt(h.compact()) <= maxW ? h.compact() : "";
    }
}
