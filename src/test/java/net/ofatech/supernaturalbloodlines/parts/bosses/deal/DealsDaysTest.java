package net.ofatech.supernaturalbloodlines.parts.bosses.deal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;

class DealsDaysTest {
    private static String key(long n) {
        return ((TranslatableContents) Deals.days(n).getContents()).getKey();
    }

    @Test
    void oneDayIsSingularAndEveryOtherNumberIsPlural() {
        assertEquals("supernaturalbloodlines.boss.days.one", key(1));
        assertEquals("supernaturalbloodlines.boss.days.other", key(0));
        assertEquals("supernaturalbloodlines.boss.days.other", key(2));
        assertEquals("supernaturalbloodlines.boss.days.other", key(7));
    }

    @Test
    void theOfferListsTheSameIdsForEverybodyAndTheAnswerIsOneOfThem() {
        assertEquals(java.util.List.of("mikael", "finn", "kol", "rebekah", "klaus"), Deals.ids());
    }
}
