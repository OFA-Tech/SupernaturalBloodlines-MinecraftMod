package net.ofatech.supernaturalbloodlines.core.rite;

import static net.ofatech.supernaturalbloodlines.core.rite.PerformerRule.Pick.FOUND;
import static net.ofatech.supernaturalbloodlines.core.rite.PerformerRule.Pick.NONE;
import static net.ofatech.supernaturalbloodlines.core.rite.PerformerRule.Pick.TARGET;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PerformerRuleTest {
    @Test
    void aPerformerWhoIsInTheLevelIsUsed() {
        assertEquals(FOUND, PerformerRule.pick(true, false));
        assertEquals(FOUND, PerformerRule.pick(true, true));
    }

    @Test
    void aPerformerWhoLeftIsStoodForOnlyByThemselves() {
        assertEquals(TARGET, PerformerRule.pick(false, true));
        assertEquals(NONE, PerformerRule.pick(false, false));
    }
}