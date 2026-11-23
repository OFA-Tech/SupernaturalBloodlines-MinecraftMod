package net.ofatech.supernaturalbloodlines.parts.bosses.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Vanilla's {@code BossHealthOverlay.render} walks the bars in order from y = 12 and, after each one, adds its increment and stops once {@code y >= guiHeight / 3}.
 * This is that loop, with our row answering as the code does (the first of our bars takes the whole row's increment, the others none), and the table of what it draws.
 */
class VanillaBossLoopTest {
    private static final int[] GUI_HEIGHTS = {540, 360, 270}; // GUI scales 2, 3 and 4 on a 1920 x 1080 window
    private static final int VANILLA_INCREMENT = 19; // 10 + the font's line height
    private static final int START = 12;

    /** One bar of the overlay: ours (the {@code ourIndex}-th) or another mod's. */
    private record Bar(boolean ours, int ourIndex) {
    }

    private static List<Bar> bars(int before, int ours, int after) {
        List<Bar> out = new ArrayList<>();
        for (int i = 0; i < before; i++) {
            out.add(new Bar(false, -1));
        }
        for (int i = 0; i < ours; i++) {
            out.add(new Bar(true, i));
        }
        for (int i = 0; i < after; i++) {
            out.add(new Bar(false, -1));
        }
        return out;
    }

    /** The y of each bar that the loop posts an event for, null for one it never reaches. */
    private static Integer[] walk(int guiH, List<Bar> bars) {
        Integer[] y = new Integer[bars.size()];
        int j = START;
        for (int i = 0; i < bars.size(); i++) {
            y[i] = j;
            Bar b = bars.get(i);
            j += b.ours() ? (b.ourIndex() == 0 ? BossBarLayout.ROW_INCREMENT : 0) : VANILLA_INCREMENT;
            if (j >= guiH / 3) {
                break;
            }
        }
        return y;
    }

    @Test
    void theRowTakesOneCardOfHeightAndTheRoomForTheNextBarsNameWhateverTheNumberOfBosses() {
        assertEquals(BossBarLayout.CARD_H + BossBarLayout.VANILLA_HEAD, BossBarLayout.ROW_INCREMENT);
        assertEquals(57, BossBarLayout.ROW_INCREMENT);
        assertTrue(BossBarLayout.ROW_INCREMENT - 9 >= BossBarLayout.CARD_H + 2, "the next bar's name (9 px above it) clears the card by 2 px");
    }

    @Test
    void everyBossOfOursAndAnotherModsBarUnderThemAreDrawnAtEveryScaleWithOneToNineBosses() {
        for (int guiH : GUI_HEIGHTS) {
            for (int n = 1; n <= 9; n++) {
                List<Bar> bars = bars(0, n, 1);
                Integer[] y = walk(guiH, bars);
                assertEquals(START, y[0], "GUI " + guiH + ", " + n + " bosses: the first of ours, where the row is drawn, is not at the top");
                for (int i = 1; i < n; i++) {
                    assertEquals(START + BossBarLayout.ROW_INCREMENT, y[i], "GUI " + guiH + ", " + n + " bosses: ours #" + i + " is not posted (it would be drawn by vanilla)");
                }
                assertEquals(START + BossBarLayout.ROW_INCREMENT, y[n], "GUI " + guiH + ", " + n + " bosses: the other mod's bar is dropped or not under the row");
                assertTrue(y[n] >= START + BossBarLayout.CARD_H + BossBarLayout.VANILLA_HEAD - 9, "it starts under the card");
            }
        }
    }

    @Test
    void theTableOfWhereTheOtherModsBarIsAtEachScale() {
        // the row is 57 high, so the other bar is posted at y = 69: under a third of the screen at every scale (180, 120 and 90)
        for (int guiH : GUI_HEIGHTS) {
            assertTrue(START + BossBarLayout.ROW_INCREMENT < guiH / 3, "GUI " + guiH + ": the loop would stop before the other bar");
        }
    }

    @Test
    void theOldVerticalStackWasDroppedAtScaleThreeAndFourWhichIsWhyThereIsOneRow() {
        int stackOfTwo = START + 2 * 52 + 11; // two cards of 46 with 6 between, and the room for a name
        assertTrue(stackOfTwo >= 360 / 3 && stackOfTwo >= 270 / 3, "two stacked cards pushed the other bar past the third at scales 3 and 4");
    }

    @Test
    void anotherModsBarsThatComeFirstPushTheRowDownAndAFewTooManyOfThemTakeItBackToVanillasBars() {
        // ours follow three other bars: the row is posted at y = 12 + 3 x 19 = 69, still under a third of the smallest screen (the loop then stops, so the
        // other bars of ours are not posted, and nothing is left for vanilla to draw)
        Integer[] y = walk(270, bars(3, 4, 0));
        assertEquals(69, y[3], "the row is drawn under the three");
        // five of them first: the loop has stopped (y = 107 >= 90) before it reaches ours, so vanilla draws what is left, as it always did
        Integer[] late = walk(270, bars(5, 2, 0));
        assertEquals(null, late[5], "the loop never posts an event for ours");
        // but at scale 2 (a third of 540 is 180) five still leave ours in
        assertTrue(walk(540, bars(5, 2, 0))[5] != null);
    }
}
