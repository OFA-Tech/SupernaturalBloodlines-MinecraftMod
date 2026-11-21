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

    private static net.ofatech.supernaturalbloodlines.core.supernatural.TaskSpec task(net.ofatech.supernaturalbloodlines.core.supernatural.TaskSpec.Detector d, java.util.Map<String, String> args) {
        return new net.ofatech.supernaturalbloodlines.core.supernatural.TaskSpec("T", d, args, 1, null, null, false, java.util.List.of());
    }

    @Test
    void aTaskThatNamesOneOfOurRitesLinksToIt() {
        var rite = net.ofatech.supernaturalbloodlines.core.supernatural.TaskSpec.Detector.RITE;
        assertEquals("ward_of_return", RiteLinkRule.riteOf(task(rite, java.util.Map.of("rite", "supernaturalbloodlines:ward_of_return", "role", "performer"))));
        assertEquals("hybrid_blood", RiteLinkRule.riteOf(task(rite, java.util.Map.of("rite", "supernaturalbloodlines:hybrid_blood", "role", "target"))));
    }

    @Test
    void anAnyRiteAnotherModsRiteOrAnotherTaskLinksToNothing() {
        var rite = net.ofatech.supernaturalbloodlines.core.supernatural.TaskSpec.Detector.RITE;
        org.junit.jupiter.api.Assertions.assertNull(RiteLinkRule.riteOf(task(rite, java.util.Map.of("rite", "any"))));
        org.junit.jupiter.api.Assertions.assertNull(RiteLinkRule.riteOf(task(rite, java.util.Map.of("rite", "enchanted:create_item"))));
        org.junit.jupiter.api.Assertions.assertNull(RiteLinkRule.riteOf(task(net.ofatech.supernaturalbloodlines.core.supernatural.TaskSpec.Detector.FEED, java.util.Map.of("rite", "supernaturalbloodlines:ward_of_return"))));
    }
}
