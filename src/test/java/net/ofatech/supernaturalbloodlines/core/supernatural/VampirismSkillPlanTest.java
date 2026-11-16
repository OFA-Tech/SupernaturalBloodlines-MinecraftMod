package net.ofatech.supernaturalbloodlines.core.supernatural;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import org.junit.jupiter.api.Test;

/** Which Vampirism skills we enable, keep and take back: only ever the ones we enabled ourselves. */
class VampirismSkillPlanTest {
    private static final String SKILL = "vampirism:less_sundamage";

    @Test
    void aWantedSkillThePlayerLacksIsEnabledAndRecorded() {
        VampirismSkillPlan.Result r = VampirismSkillPlan.plan(Set.of(SKILL), Set.of(), id -> false);
        assertEquals(Set.of(SKILL), r.enable());
        assertEquals(Set.of(SKILL), r.tracked());
        assertEquals(Set.of(), r.disable());
    }

    @Test
    void aSkillThePlayerAlreadyHadIsNeverRecordedOrTakenBack() {
        VampirismSkillPlan.Result own = VampirismSkillPlan.plan(Set.of(SKILL), Set.of(), id -> true);
        assertEquals(Set.of(), own.enable());
        assertEquals(Set.of(), own.tracked());
        VampirismSkillPlan.Result lost = VampirismSkillPlan.plan(Set.of(), own.tracked(), id -> true);
        assertEquals(Set.of(), lost.disable(), "the player's own skill must stay when the ladder skill goes");
    }

    @Test
    void ourSkillIsTakenBackWhenTheLadderSkillIsNotActiveAnyMore() {
        VampirismSkillPlan.Result r = VampirismSkillPlan.plan(Set.of(), Set.of(SKILL), id -> true);
        assertEquals(Set.of(SKILL), r.disable());
        assertEquals(Set.of(), r.tracked());
    }

    @Test
    void aResetOfOursIsEnabledAgainAndStaysRecorded() {
        VampirismSkillPlan.Result r = VampirismSkillPlan.plan(Set.of(SKILL), Set.of(SKILL), id -> false);
        assertEquals(Set.of(SKILL), r.enable());
        assertEquals(Set.of(SKILL), r.tracked());
    }

    @Test
    void aSkillAlreadyGoneIsForgottenWithoutDisablingAnything() {
        VampirismSkillPlan.Result r = VampirismSkillPlan.plan(Set.of(), Set.of(SKILL), id -> false);
        assertEquals(Set.of(), r.disable());
        assertEquals(Set.of(), r.tracked());
    }

    @Test
    void anOwnedSkillStaysRecordedWhileItIsStillWanted() {
        VampirismSkillPlan.Result r = VampirismSkillPlan.plan(Set.of(SKILL), Set.of(SKILL), id -> true);
        assertEquals(Set.of(), r.enable());
        assertEquals(Set.of(), r.disable());
        assertEquals(Set.of(SKILL), r.tracked());
    }
}
