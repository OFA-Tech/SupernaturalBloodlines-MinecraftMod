package net.ofatech.supernaturalbloodlines.core.bloodline;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.ofatech.supernaturalbloodlines.core.bloodline.PackSkillRules.Refusal;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank;
import org.junit.jupiter.api.Test;

/** R146, row by row: who may surge and call spirits, and when. */
class PackSkillRulesTest {
    private static final long CD = PackSkillRules.SURGE_COOLDOWN_TICKS;

    @Test
    void aWerewolfWithAPackMaySurge() {
        assertEquals(Refusal.NONE, PackSkillRules.surge(true, true, true, false, true, 1000, -1, CD));
    }

    @Test
    void aSurgeNeedsTheSwitchAWerewolfAndAPack() {
        assertEquals(Refusal.OFF, PackSkillRules.surge(false, true, true, false, true, 1000, -1, CD));
        assertEquals(Refusal.NOT_WEREWOLF, PackSkillRules.surge(true, false, true, false, true, 1000, -1, CD));
        assertEquals(Refusal.NO_PACK, PackSkillRules.surge(true, true, false, false, true, 1000, -1, CD));
    }

    @Test
    void anEclipseDeadOrSpectatingAndTheCooldownRefuseBoth() {
        assertEquals(Refusal.ECLIPSE, PackSkillRules.surge(true, true, true, true, true, 1000, -1, CD));
        assertEquals(Refusal.UNAVAILABLE, PackSkillRules.surge(true, true, true, false, false, 1000, -1, CD));
        assertEquals(Refusal.COOLDOWN, PackSkillRules.surge(true, true, true, false, true, 1000 + CD - 1, 1000, CD));
        assertEquals(Refusal.NONE, PackSkillRules.surge(true, true, true, false, true, 1000 + CD, 1000, CD));
    }

    @Test
    void aFirstUseHasNoCooldownEvenAtTickZero() {
        assertEquals(Refusal.NONE, PackSkillRules.surge(true, true, true, false, true, 0, -1, CD));
        assertEquals(Refusal.COOLDOWN, PackSkillRules.surge(true, true, true, false, true, 5, 0, CD), "a use at tick 0 is a use");
    }

    @Test
    void onlyAnAlphaOrATrueAlphaCallsSpirits() {
        long cd = PackSkillRules.SPIRITS_COOLDOWN_TICKS;
        assertEquals(Refusal.NONE, PackSkillRules.spirits(true, true, Rank.ALPHA, false, true, 10, -1, cd));
        assertEquals(Refusal.NONE, PackSkillRules.spirits(true, true, Rank.TRUE_ALPHA, false, true, 10, -1, cd));
        assertEquals(Refusal.NOT_ALPHA, PackSkillRules.spirits(true, true, Rank.BETA, false, true, 10, -1, cd));
        assertEquals(Refusal.NOT_ALPHA, PackSkillRules.spirits(true, true, Rank.OMEGA, false, true, 10, -1, cd));
        assertEquals(Refusal.NOT_WEREWOLF, PackSkillRules.spirits(true, false, Rank.ALPHA, false, true, 10, -1, cd));
        assertEquals(Refusal.OFF, PackSkillRules.spirits(false, true, Rank.ALPHA, false, true, 10, -1, cd));
        assertEquals(Refusal.ECLIPSE, PackSkillRules.spirits(true, true, Rank.ALPHA, true, true, 10, -1, cd));
        assertEquals(Refusal.COOLDOWN, PackSkillRules.spirits(true, true, Rank.ALPHA, false, true, 10 + cd - 1, 10, cd));
    }

    @Test
    void theSecondsLeftRoundUp() {
        assertEquals(240, PackSkillRules.secondsLeft(0, 0, CD));
        assertEquals(1, PackSkillRules.secondsLeft(CD - 1, 0, CD));
        assertEquals(0, PackSkillRules.secondsLeft(CD, 0, CD));
        assertEquals(0, PackSkillRules.secondsLeft(CD * 3, 0, CD));
    }
}