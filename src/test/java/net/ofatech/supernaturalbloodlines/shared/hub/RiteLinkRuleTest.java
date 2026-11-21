package net.ofatech.supernaturalbloodlines.shared.hub;

import static net.ofatech.supernaturalbloodlines.shared.hub.RiteLinkRule.Open.ENTRY;
import static net.ofatech.supernaturalbloodlines.shared.hub.RiteLinkRule.Open.NEED_BOOK;
import static net.ofatech.supernaturalbloodlines.shared.hub.RiteLinkRule.Open.SEALED;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RiteLinkRuleTest {
    @Test
    void withoutTheBookNothingOpens() {
        assertEquals(NEED_BOOK, RiteLinkRule.decide(false, false, true));
        assertEquals(NEED_BOOK, RiteLinkRule.decide(false, true, false));
        assertEquals(NEED_BOOK, RiteLinkRule.decide(false, true, true));
    }

    @Test
    void aKnownOrOpenRiteOpensItsEntry() {
        assertEquals(ENTRY, RiteLinkRule.decide(true, false, true));
        assertEquals(ENTRY, RiteLinkRule.decide(true, false, false));
        assertEquals(ENTRY, RiteLinkRule.decide(true, true, true));
    }

    @Test
    void aRiteNotLearnedOpensTheSealedEntry() {
        assertEquals(SEALED, RiteLinkRule.decide(true, true, false));
    }
}
