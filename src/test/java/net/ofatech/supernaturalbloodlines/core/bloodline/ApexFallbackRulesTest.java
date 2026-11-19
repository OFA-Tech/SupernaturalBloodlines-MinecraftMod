package net.ofatech.supernaturalbloodlines.core.bloodline;

import static net.ofatech.supernaturalbloodlines.core.bloodline.ApexFallbackRules.Action.NONE;
import static net.ofatech.supernaturalbloodlines.core.bloodline.ApexFallbackRules.Action.RESTORE;
import static net.ofatech.supernaturalbloodlines.core.bloodline.ApexFallbackRules.Action.REVERT;
import static net.ofatech.supernaturalbloodlines.core.bloodline.ApexFallbackRules.APEX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank;
import org.junit.jupiter.api.Test;

class ApexFallbackRulesTest {
    private static final String BEFORE = "tensura:human_saint";

    @Test
    void losingTheSparkOnTheApexRaceGoesBackAndGettingItBackReturns() {
        assertEquals(REVERT, ApexFallbackRules.decide(true, false, APEX, BEFORE), "stolen, given away or cured");
        assertEquals(RESTORE, ApexFallbackRules.decide(false, true, BEFORE, BEFORE), "the spark is back");
    }

    @Test
    void onlyAChangeCountsAndOnlyOnTheRightRace() {
        assertEquals(NONE, ApexFallbackRules.decide(false, false, APEX, BEFORE), "a Beta who never had a spark stays on the race they evolved to");
        assertEquals(NONE, ApexFallbackRules.decide(true, true, APEX, BEFORE));
        assertEquals(NONE, ApexFallbackRules.decide(true, false, BEFORE, BEFORE), "already back");
        assertEquals(NONE, ApexFallbackRules.decide(false, true, "tensura:elf", BEFORE), "evolved elsewhere meanwhile: not sent anywhere");
        assertEquals(NONE, ApexFallbackRules.decide(false, true, BEFORE, ""), "nothing recorded: nothing to come back to");
    }

    @Test
    void whoHoldsASpark() {
        assertTrue(ApexFallbackRules.holds(Rank.ALPHA, true) && ApexFallbackRules.holds(Rank.TRUE_ALPHA, true));
        assertFalse(ApexFallbackRules.holds(Rank.TRUE_ALPHA, false), "a dormant True Alpha holds nothing now");
        assertFalse(ApexFallbackRules.holds(Rank.BETA, true));
    }

    @Test
    void theRaceBeforeIsRecordedOnEnteringTheApexAndForgottenOnAnyOtherChange() {
        assertEquals(BEFORE, ApexFallbackRules.recorded(BEFORE, APEX, ""));
        assertEquals(BEFORE, ApexFallbackRules.recorded(APEX, APEX, BEFORE), "a repeat keeps it");
        assertEquals(BEFORE, ApexFallbackRules.recorded(null, APEX, BEFORE), "no old race keeps what was there");
        assertEquals("", ApexFallbackRules.recorded(BEFORE, "tensura:elf", BEFORE), "evolved on by their own choice");
        assertEquals(ApexFallbackRules.DEFAULT_BEFORE, ApexFallbackRules.target(""), "a legacy apex wolf goes to the default");
        assertEquals(BEFORE, ApexFallbackRules.target(BEFORE));
    }
}
