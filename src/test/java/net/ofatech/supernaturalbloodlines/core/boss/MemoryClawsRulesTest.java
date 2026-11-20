package net.ofatech.supernaturalbloodlines.core.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.shared.supernatural.profile.Fact;
import net.ofatech.supernaturalbloodlines.shared.supernatural.profile.Knowledge;
import org.junit.jupiter.api.Test;

class MemoryClawsRulesTest {
    private static final MemoryClawsRules.Target PLAYER = MemoryClawsRules.Target.PLAYER;

    @Test
    void aPlayerWhoConsentsAndMayBeHurtIsRead() {
        assertEquals(MemoryClawsRules.Verdict.OK, MemoryClawsRules.verdict(PLAYER, false, true, true));
    }

    @Test
    void withoutConsentOrWithoutTheRightToHarmItIsRefusedWithOneAnswer() {
        assertEquals(MemoryClawsRules.Verdict.REFUSED, MemoryClawsRules.verdict(PLAYER, false, false, true));
        assertEquals(MemoryClawsRules.Verdict.REFUSED, MemoryClawsRules.verdict(PLAYER, false, true, false));
        assertEquals(MemoryClawsRules.Verdict.REFUSED, MemoryClawsRules.verdict(PLAYER, false, false, false));
    }

    @Test
    void whatTheTargetIsAndWhetherItIsTheUserComeBeforeConsentSoNothingIsAskedOfAnythingElse() {
        assertEquals(MemoryClawsRules.Verdict.NOT_THIS, MemoryClawsRules.verdict(MemoryClawsRules.Target.OTHER, false, true, true));
        assertEquals(MemoryClawsRules.Verdict.NOT_THIS, MemoryClawsRules.verdict(MemoryClawsRules.Target.OTHER, true, false, false));
        assertEquals(MemoryClawsRules.Verdict.SELF, MemoryClawsRules.verdict(PLAYER, true, true, true), "never on themselves");
        assertEquals(MemoryClawsRules.Verdict.SELF, MemoryClawsRules.verdict(PLAYER, true, false, false));
    }

    @Test
    void everyRefusalHasAMessageAndOnlyAWorkingUseIsPaidFor() {
        assertNull(MemoryClawsRules.message(MemoryClawsRules.Verdict.OK));
        assertEquals(MemoryClawsRules.REFUSED, MemoryClawsRules.message(MemoryClawsRules.Verdict.REFUSED));
        assertEquals(MemoryClawsRules.NOT_THIS, MemoryClawsRules.message(MemoryClawsRules.Verdict.NOT_THIS));
        assertEquals(MemoryClawsRules.SELF, MemoryClawsRules.message(MemoryClawsRules.Verdict.SELF));
        assertTrue(MemoryClawsRules.spends(MemoryClawsRules.Verdict.OK));
        for (var v : MemoryClawsRules.Verdict.values()) {
            if (v != MemoryClawsRules.Verdict.OK) {
                assertFalse(MemoryClawsRules.spends(v), v + " must cost nothing");
            }
        }
    }

    @Test
    void theClawsTeachTheSireAndTheGenesAndNothingElseSoNoLineIsEverNamed() {
        assertEquals(java.util.EnumSet.of(Fact.SIRE, Fact.GENES), Knowledge.learnedBy(Knowledge.Ability.MEMORY_CLAWS));
        assertFalse(Knowledge.learnedBy(Knowledge.Ability.MEMORY_CLAWS).contains(Fact.LINE));
    }

    @Test
    void theClawsHaveTwelveUsesAndAHalfMinuteBetweenThem() {
        assertEquals(12, MemoryClawsRules.USES);
        assertEquals(600, MemoryClawsRules.COOLDOWN_TICKS);
    }
}