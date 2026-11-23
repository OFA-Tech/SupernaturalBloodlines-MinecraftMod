package net.ofatech.supernaturalbloodlines.parts.bosses.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.ofatech.supernaturalbloodlines.parts.bosses.client.BossNarration.Entry;
import net.ofatech.supernaturalbloodlines.parts.bosses.client.BossNarration.Line;
import org.junit.jupiter.api.Test;

class BossNarrationTest {
    private static final String FORM = "supernaturalbloodlines.boss.bar.form.shifting";

    @Test
    void theFocusIsNamedFirstWithItsHealthAndFormAndTheOthersAreListedWithTheirs() {
        List<Line> lines = BossNarration.row(new Entry("Klaus", 82, FORM), List.of(new Entry("Deucalion", 55, null), new Entry("Mikael", 100, null)));
        assertEquals(2, lines.size());
        assertEquals(BossNarration.KEY + "focus_form", lines.get(0).key());
        assertEquals(List.of("Klaus", 82, FORM), lines.get(0).args());
        assertEquals(BossNarration.KEY + "others", lines.get(1).key());
        assertEquals(List.of(2, "Deucalion 55%, Mikael 100%"), lines.get(1).args(), "every other boss, in the order of the row");
    }

    @Test
    void aBossWithoutFormsHasNoFormInItsLineAndALoneBossHasNoSecondLine() {
        List<Line> lines = BossNarration.row(new Entry("Mikael", 40, null), List.of());
        assertEquals(1, lines.size());
        assertEquals(BossNarration.KEY + "focus_alone", lines.get(0).key());
        assertEquals(List.of("Mikael", 40), lines.get(0).args());
    }

    @Test
    void everyBossIsNamedEvenWhenTheRowHidesSomeInAPlusToken() {
        List<Entry> eight = new java.util.ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            eight.add(new Entry("Boss" + i, i * 10, null));
        }
        Line others = BossNarration.row(new Entry("Focus", 100, null), eight).get(1);
        assertEquals(8, others.args().get(0));
        for (int i = 1; i <= 8; i++) {
            assertTrue(((String) others.args().get(1)).contains("Boss" + i + " " + i * 10 + "%"), "Boss" + i + " is missing: " + others.args().get(1));
        }
    }

    @Test
    void thePlusTokensTooltipListsTheHiddenBossesOneLineEach() {
        List<Line> lines = BossNarration.hidden(List.of(new Entry("Kol", 30, null), new Entry("Peter", 60, FORM)));
        assertEquals(2, lines.size());
        assertEquals(BossNarration.KEY + "token", lines.get(0).key());
        assertEquals(List.of("Kol", 30), lines.get(0).args());
        assertEquals(BossNarration.KEY + "token_form", lines.get(1).key());
        assertEquals(List.of("Peter", 60, FORM), lines.get(1).args());
    }

    @Test
    void thePercentIsRoundedUpAndHeldInRangeSoALivingBossNeverReadsZero() {
        assertEquals(1, BossNarration.percent(1, 2000));
        assertEquals(82, BossNarration.percent(1640, 2000));
        assertEquals(82, BossNarration.percent(1639, 2000), "81.95 reads as 82, the way the card's number is rounded up");
        assertEquals(2, BossNarration.percent(21, 2000), "1.05 is 2, not 1");
        assertEquals(100, BossNarration.percent(2000, 2000));
        assertEquals(100, BossNarration.percent(9000, 2000));
        assertEquals(0, BossNarration.percent(0, 2000));
        assertEquals(0, BossNarration.percent(5, 0), "no maximum");
        assertEquals(100, new Entry("x", 400, null).percent());
        assertEquals(0, new Entry("x", -4, null).percent());
    }
}
