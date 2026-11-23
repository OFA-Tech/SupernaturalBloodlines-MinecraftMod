package net.ofatech.supernaturalbloodlines.parts.bosses.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import net.ofatech.supernaturalbloodlines.parts.bosses.client.BossRow.Plan;
import net.ofatech.supernaturalbloodlines.parts.bosses.client.BossRow.Slot;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import org.junit.jupiter.api.Test;

class BossRowTest {
    private static final int[] SCREENS = {960, 640, 480, 427, 320};

    private static Rect token(Slot s) {
        return new Rect(s.x(), BossRow.TOKEN_DY, BossRow.TOKEN, BossRow.TOKEN);
    }

    @Test
    void theCardIsTheApprovedSizeWhereThereIsRoomAndCentredWithoutTheCorner() {
        for (int guiW : new int[] {960, 640, 480}) {
            Plan p = BossRow.plan(guiW, false, 0);
            assertEquals(250, p.cardW(), guiW + "");
            assertEquals((guiW - 250) / 2, p.cardX(), guiW + ": centred");
            assertTrue(p.slots().isEmpty());
            assertEquals(0, p.hidden());
        }
        assertEquals(224, BossRow.plan(248, false, 0).cardW(), "a narrow screen leaves the margin on each side");
    }

    @Test
    void theRowStaysClearOfTheStatusCornerAtEveryScreenWidthThatHasRoomForACardBesideIt() {
        for (int guiW : new int[] {960, 640, 480, 427, 360}) {
            for (int tokens = 0; tokens <= 9; tokens++) {
                Plan p = BossRow.plan(guiW, true, tokens);
                int left = Math.min(p.cardX(), p.slots().stream().mapToInt(Slot::x).min().orElse(p.cardX()));
                assertTrue(left >= net.ofatech.supernaturalbloodlines.core.hud.TrackerAnchor.CORNER_W + 4, guiW + "/" + tokens + ": the row starts at " + left + ", inside the corner");
            }
        }
        assertEquals(BossRow.STATUS_CLEAR, BossRow.leftClear(480, true));
        assertEquals(BossRow.SCREEN_MARGIN, BossRow.leftClear(480, false));
        assertEquals(BossRow.SCREEN_MARGIN, BossRow.leftClear(300, true), "a screen too narrow for a card beside the corner centres it and reaches over it");
    }

    @Test
    void everyTokenIsOnTheScreenInTheCardsRowAndClearOfTheCardAndOfEachOther() {
        for (int guiW : SCREENS) {
            for (boolean corner : new boolean[] {false, true}) {
                for (int tokens = 0; tokens <= 9; tokens++) {
                    Plan p = BossRow.plan(guiW, corner, tokens);
                    Rect card = new Rect(p.cardX(), 0, p.cardW(), BossBarLayout.CARD_H);
                    assertTrue(card.x() >= BossRow.SCREEN_MARGIN && card.right() <= guiW - BossRow.SCREEN_MARGIN, guiW + "/" + corner + "/" + tokens + ": card " + card);
                    List<Rect> seen = new ArrayList<>();
                    for (Slot s : p.slots()) {
                        Rect t = token(s);
                        String at = guiW + "/" + corner + "/" + tokens + ": token at " + s.x();
                        assertTrue(t.x() >= BossRow.SCREEN_MARGIN && t.right() <= guiW - BossRow.SCREEN_MARGIN, at + " is off the screen");
                        assertFalse(t.overlaps(card), at + " is on the card");
                        for (Rect other : seen) {
                            assertFalse(t.overlaps(other), at + " is on another token");
                        }
                        seen.add(t);
                        assertTrue(t.y() >= 0 && t.bottom() <= BossBarLayout.CARD_H, at + " is outside the card's row");
                    }
                }
            }
        }
    }

    @Test
    void theLastTokenAndThePlusTokenKeepTheScreenMarginOnTheRightAsTheCardsDoOnTheLeft() {
        assertTrue(BossRow.SCREEN_MARGIN >= 12, "a margin a token plate and its ring can show: " + BossRow.SCREEN_MARGIN);
        for (int guiW : SCREENS) {
            for (boolean corner : new boolean[] {false, true}) {
                for (int tokens = 1; tokens <= 12; tokens++) {
                    Plan p = BossRow.plan(guiW, corner, tokens);
                    int right = p.cardX() + p.cardW();
                    int left = p.cardX();
                    for (Slot s : p.slots()) {
                        right = Math.max(right, s.x() + BossRow.TOKEN);
                        left = Math.min(left, s.x());
                    }
                    String at = guiW + "/" + corner + "/" + tokens;
                    assertTrue(guiW - right >= BossRow.SCREEN_MARGIN, at + ": the row ends " + (guiW - right) + " px from the right edge");
                    assertTrue(left >= BossRow.leftClear(guiW, corner), at + ": the row starts " + left + ", inside the clearance");
                }
            }
        }
    }

    @Test
    void tokensAreBalancedLeftAndRightWhereBothSidesHaveRoom() {
        for (int tokens = 1; tokens <= 6; tokens++) {
            Plan p = BossRow.plan(960, false, tokens);
            int left = 0;
            int right = 0;
            for (Slot s : p.slots()) {
                if (s.x() < p.cardX()) {
                    left++;
                } else {
                    right++;
                }
            }
            assertEquals(tokens, left + right);
            assertTrue(Math.abs(left - right) <= 1, tokens + " tokens: " + left + " left, " + right + " right");
        }
    }

    @Test
    void theFirstTokenIsRightNextToTheCardAndTheOthersFollowOutwards() {
        Plan p = BossRow.plan(960, false, 4);
        List<Slot> s = p.slots();
        assertEquals(p.cardX() + p.cardW() + BossRow.TOKEN_GAP, s.get(0).x(), "the first on the right");
        assertEquals(p.cardX() - BossRow.TOKEN - BossRow.TOKEN_GAP, s.get(1).x(), "the second on the left");
        assertEquals(s.get(0).x() + BossRow.TOKEN + BossRow.TOKEN_GAP, s.get(2).x(), "the third beside the first");
        for (int i = 0; i < 4; i++) {
            assertEquals(i, s.get(i).index());
        }
    }

    @Test
    void nineBossesFitAsEightTokensWhereTheScreenIsWideAndNothingIsHidden() {
        Plan p = BossRow.plan(960, false, 8);
        assertEquals(8, p.slots().size());
        assertEquals(0, p.hidden());
        assertEquals(250, p.cardW());
        assertTrue(p.slots().stream().noneMatch(s -> s.index() == BossRow.PLUS));
    }

    @Test
    void whatDoesNotFitCollapsesIntoOnePlusTokenThatIsTheLastSlotAndTheCountAddsUp() {
        for (int guiW : new int[] {480, 427, 320}) {
            for (boolean corner : new boolean[] {false, true}) {
                for (int tokens = 1; tokens <= 12; tokens++) {
                    Plan p = BossRow.plan(guiW, corner, tokens);
                    long plus = p.slots().stream().filter(s -> s.index() == BossRow.PLUS).count();
                    long real = p.slots().size() - plus;
                    String at = guiW + "/" + corner + "/" + tokens;
                    assertTrue(plus <= 1, at + ": more than one +N token");
                    assertEquals(tokens, real + p.hidden(), at + ": every token is drawn or counted");
                    if (plus == 1) {
                        assertEquals(BossRow.PLUS, p.slots().get(p.slots().size() - 1).index(), at + ": +N is the last slot");
                        assertTrue(p.hidden() >= 2, at + ": a +N for one token would hold less than the token");
                    } else if (p.hidden() > 0) {
                        assertTrue(p.slots().isEmpty(), at + ": hidden tokens with room left for a +N token");
                    }
                    for (int i = 0; i < real; i++) {
                        assertEquals(i, p.slots().get(i).index(), at);
                    }
                }
            }
        }
    }

    @Test
    void theCardNarrowsToHoldTokensBeforeAnyIsCollapsedAndNeverBelowTheNarrowestCard() {
        // 480 wide with the corner: a 250 card leaves room for one token, a narrower one for two
        assertEquals(1, BossRow.plan(480, true, 1).slots().size());
        Plan two = BossRow.plan(480, true, 2);
        assertEquals(0, two.hidden(), "two tokens fit once the card is narrower");
        assertTrue(two.cardW() < 250 && two.cardW() >= BossRow.NARROWEST_CARD, "" + two.cardW());
        Plan many = BossRow.plan(480, true, 9);
        assertEquals(BossRow.NARROWEST_CARD, many.cardW(), "when nothing helps the card is as narrow as it goes");
        assertTrue(many.hidden() > 0);
        assertEquals(250, BossRow.plan(480, false, 2).cardW(), "no need to narrow it where the tokens fit");
    }

    @Test
    void aTokenIsFoundUnderThePointerAndOnlyInsideItsSquare() {
        Plan p = BossRow.plan(960, false, 2);
        int rowY = 12;
        Slot s0 = p.slots().get(0);
        assertEquals(s0, BossRow.slotAt(p, rowY, s0.x() + 3, rowY + BossRow.TOKEN_DY + 3));
        assertNull(BossRow.slotAt(p, rowY, s0.x() - 1, rowY + BossRow.TOKEN_DY + 3), "left of it");
        assertNull(BossRow.slotAt(p, rowY, s0.x() + BossRow.TOKEN, rowY + BossRow.TOKEN_DY + 3), "right of it");
        assertNull(BossRow.slotAt(p, rowY, s0.x() + 3, rowY + BossRow.TOKEN_DY - 1), "above it");
        assertNull(BossRow.slotAt(p, rowY, s0.x() + 3, rowY + BossRow.TOKEN_DY + BossRow.TOKEN), "under it");
        assertNull(BossRow.slotAt(p, rowY, p.cardX() + 20, rowY + 20), "on the card");
    }
}
