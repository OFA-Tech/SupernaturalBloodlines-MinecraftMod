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

    private static MemoryClawsRules.Verdict v(boolean pvp, boolean consent, boolean facing, boolean mayHarm) {
        return MemoryClawsRules.verdict(PLAYER, false, pvp, consent, facing, mayHarm);
    }

    @Test
    void aPlayerWhoConsentsFacesTheUserAndMayBeHurtIsReadWherePvpIsOn() {
        assertEquals(MemoryClawsRules.Verdict.OK, v(true, true, true, true));
    }

    @Test
    void everyOneOfTheThreeThingsTheTargetMustDoOrBeIsNeededAndEachRefusalSaysTheSame() {
        assertEquals(MemoryClawsRules.Verdict.REFUSED, v(true, false, true, true), "no consent");
        assertEquals(MemoryClawsRules.Verdict.REFUSED, v(true, true, false, true), "looking away or behind a wall");
        assertEquals(MemoryClawsRules.Verdict.REFUSED, v(true, true, true, false), "the user may not hurt them");
        assertEquals(MemoryClawsRules.Verdict.REFUSED, v(true, false, false, false));
    }

    @Test
    void whereNoPlayerMayFightTheClawsSayTheyCannotWorkHereWhateverTheTargetDid() {
        assertEquals(MemoryClawsRules.Verdict.PVP_OFF, v(false, true, true, true));
        assertEquals(MemoryClawsRules.Verdict.PVP_OFF, v(false, false, false, false), "it is the server's, so it tells nothing about the target");
    }

    @Test
    void whatTheTargetIsAndWhetherItIsTheUserComeBeforeAnythingIsAskedOfTheTarget() {
        assertEquals(MemoryClawsRules.Verdict.NOT_THIS, MemoryClawsRules.verdict(MemoryClawsRules.Target.OTHER, false, true, true, true, true));
        assertEquals(MemoryClawsRules.Verdict.NOT_THIS, MemoryClawsRules.verdict(MemoryClawsRules.Target.OTHER, true, false, false, false, false));
        assertEquals(MemoryClawsRules.Verdict.SELF, MemoryClawsRules.verdict(PLAYER, true, true, true, true, true), "never on themselves");
        assertEquals(MemoryClawsRules.Verdict.SELF, MemoryClawsRules.verdict(PLAYER, true, false, false, false, false));
    }

    @Test
    void everyRefusalHasAMessageAndOnlyAWorkingUseIsPaidFor() {
        assertNull(MemoryClawsRules.message(MemoryClawsRules.Verdict.OK));
        assertEquals(MemoryClawsRules.REFUSED, MemoryClawsRules.message(MemoryClawsRules.Verdict.REFUSED));
        assertEquals(MemoryClawsRules.NOT_THIS, MemoryClawsRules.message(MemoryClawsRules.Verdict.NOT_THIS));
        assertEquals(MemoryClawsRules.SELF, MemoryClawsRules.message(MemoryClawsRules.Verdict.SELF));
        assertEquals(MemoryClawsRules.PVP_OFF, MemoryClawsRules.message(MemoryClawsRules.Verdict.PVP_OFF));
        assertTrue(MemoryClawsRules.spends(MemoryClawsRules.Verdict.OK));
        for (var v : MemoryClawsRules.Verdict.values()) {
            if (v != MemoryClawsRules.Verdict.OK) {
                assertFalse(MemoryClawsRules.spends(v), v + " must cost nothing");
            }
        }
    }

    @Test
    void theTargetFacesTheUserOnlyWithinAboutTwentyFiveDegreesAndWithAClearLine() {
        assertTrue(MemoryClawsRules.facing(1.0, true));
        assertTrue(MemoryClawsRules.facing(0.91, true));
        assertFalse(MemoryClawsRules.facing(0.9, true), "the cosine must be over 0.9");
        assertFalse(MemoryClawsRules.facing(-1.0, true), "back turned");
        assertFalse(MemoryClawsRules.facing(1.0, false), "facing them through a wall");
    }

    @Test
    void theWaitIsTheSecondsLeftRoundedUpAndNeverZeroWhileAnyIsLeft() {
        assertEquals(30, MemoryClawsRules.secondsLeft(1.0F));
        assertEquals(15, MemoryClawsRules.secondsLeft(0.5F));
        assertEquals(1, MemoryClawsRules.secondsLeft(0.001F), "a sliver left is still one second");
        assertEquals(2, MemoryClawsRules.secondsLeft(0.04F), "1.2 seconds rounds up");
        assertEquals(1, MemoryClawsRules.secondsLeft(0.03F), "0.9 seconds is one");
        assertEquals(0, MemoryClawsRules.secondsLeft(0F));
        assertEquals(0, MemoryClawsRules.secondsLeft(-1F));
    }

    @Test
    void theClawsTeachTheSireAndTheGenesAndNothingElseSoNoLineIsEverNamed() {
        assertEquals(java.util.EnumSet.of(Fact.SIRE, Fact.GENES), Knowledge.learnedBy(Knowledge.Ability.MEMORY_CLAWS));
        assertFalse(Knowledge.learnedBy(Knowledge.Ability.MEMORY_CLAWS).contains(Fact.LINE));
    }
}
