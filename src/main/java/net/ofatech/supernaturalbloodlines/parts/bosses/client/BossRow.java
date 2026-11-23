package net.ofatech.supernaturalbloodlines.parts.bosses.client;

import java.util.ArrayList;
import java.util.List;
import net.ofatech.supernaturalbloodlines.core.hud.TrackerAnchor;

/**
 * Where everything of the boss row goes. Our bars take one row, one card tall, at the top centre, whatever the number of bosses: the focus boss is the full card and
 * every other boss a small token in the same row, left and right of the card and kept balanced. When the width cannot hold them all the card narrows (down to
 * {@link #NARROWEST_CARD}) and what is still too many collapses into a "+N" token. All x are screen x; y is the row's top. Pure.
 */
public final class BossRow {
    /** A token: the portrait plate (34) and a 2 px health ring round it. */
    public static final int TOKEN = 38;
    public static final int TOKEN_GAP = 2;
    private static final int SLOT = TOKEN + TOKEN_GAP;
    public static final int SCREEN_MARGIN = 4;
    /** The row stands this far from the left edge when another mod's status corner (Tensura, Vampirism) is there. */
    public static final int STATUS_CLEAR = TrackerAnchor.CORNER_W + 4;
    /** A card beside the corner is not worth making narrower than this screen leaves room for. */
    public static final int MIN_SCREEN_FOR_CORNER = 170;
    public static final int NARROWEST_CARD = 200;
    /** The token's top is this far under the row's top: it is centred on the card. */
    public static final int TOKEN_DY = (BossBarLayout.CARD_H - TOKEN) / 2;
    /** The slot's index for the "+N" token. */
    public static final int PLUS = -1;

    /** {@code index}: the position in the list of tokens, or {@link #PLUS}. */
    public record Slot(int x, int index) {
    }

    /** @param hidden how many tokens are not drawn (they are inside the "+N" token, which is the last slot, or in no slot at all when there is no room even for it) */
    public record Plan(int cardW, int cardX, List<Slot> slots, int hidden) {
    }

    private BossRow() {
    }

    /** How far from the left edge the row may start. */
    public static int leftClear(int guiW, boolean statusCorner) {
        return statusCorner && guiW - STATUS_CLEAR - SCREEN_MARGIN >= MIN_SCREEN_FOR_CORNER ? STATUS_CLEAR : SCREEN_MARGIN;
    }

    /** The widest the card may be on this screen. */
    public static int widest(int guiW, boolean statusCorner) {
        return Math.min(BossBarLayout.CARD_W, guiW - leftClear(guiW, statusCorner) - SCREEN_MARGIN);
    }

    /**
     * @param tokens how many bosses besides the focus there are
     * @return the card and the tokens: the card is the widest that lets every token have a slot, never under {@link #NARROWEST_CARD}
     */
    public static Plan plan(int guiW, boolean statusCorner, int tokens) {
        int left = leftClear(guiW, statusCorner);
        int widest = widest(guiW, statusCorner);
        int floor = Math.min(NARROWEST_CARD, widest);
        Plan best = null;
        for (int w = widest; best == null || w >= floor; w -= 10) {
            best = at(guiW, left, Math.max(w, floor), tokens);
            if (best.hidden() == 0 || w <= floor) {
                break;
            }
        }
        return best;
    }

    private static Plan at(int guiW, int left, int w, int tokens) {
        int x = Math.max((guiW - w) / 2, left);
        int rightCap = Math.max(0, (guiW - SCREEN_MARGIN - (x + w)) / SLOT);
        int leftCap = Math.max(0, (x - left) / SLOT);
        int cap = rightCap + leftCap;
        boolean plus = tokens > cap && cap >= 1;
        int shown = tokens <= cap ? tokens : Math.max(0, cap - 1);
        int slots = shown + (plus ? 1 : 0);
        List<Slot> out = new ArrayList<>(slots);
        int r = 0;
        int l = 0;
        for (int k = 0; k < slots; k++) {
            boolean right = (r <= l && r < rightCap) || l >= leftCap;
            int sx = right ? x + w + TOKEN_GAP + r++ * SLOT : x - SLOT - l++ * SLOT;
            out.add(new Slot(sx, plus && k == slots - 1 ? PLUS : k));
        }
        return new Plan(w, x, List.copyOf(out), tokens - shown);
    }

    /** The slot under a point, or null. {@code rowY}: the row's top. */
    public static Slot slotAt(Plan plan, int rowY, double mx, double my) {
        if (my < rowY + TOKEN_DY || my >= rowY + TOKEN_DY + TOKEN) {
            return null;
        }
        for (Slot s : plan.slots()) {
            if (mx >= s.x() && mx < s.x() + TOKEN) {
                return s;
            }
        }
        return null;
    }
}
