package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The "1s forever" bug: ManasCore counts a skill cooldown in seconds, and the HUD divided it by 20 and rounded up. */
class SkillCooldownTest {
    @Test
    void aCooldownOfThirtySecondsReadsThirtyAndCountsDown() {
        assertEquals(30, SkillCooldown.seconds(30));
        assertEquals(29, SkillCooldown.seconds(29));
        assertEquals(21, SkillCooldown.seconds(21), "the old (n + 19) / 20 gave 2");
        assertEquals(1, SkillCooldown.seconds(1));
        assertEquals(0, SkillCooldown.seconds(0));
        assertEquals(0, SkillCooldown.seconds(-3));
    }

    @Test
    void everyValueFromTwentyDownIsDistinctSoTheNumberMoves() {
        java.util.Set<Integer> shown = new java.util.HashSet<>();
        for (int left = 20; left >= 1; left--) {
            shown.add(SkillCooldown.seconds(left));
        }
        assertEquals(20, shown.size(), "the old rule showed 1 for all of them");
    }

    @Test
    void theFillIsTheShareLeftOfTheLongestSeen() {
        assertEquals(1f, SkillCooldown.fraction(30, 30));
        assertEquals(0.5f, SkillCooldown.fraction(15, 30));
        assertEquals(0f, SkillCooldown.fraction(0, 30));
        assertEquals(0f, SkillCooldown.fraction(5, 0));
        assertEquals(1f, SkillCooldown.fraction(40, 30));
    }
}
