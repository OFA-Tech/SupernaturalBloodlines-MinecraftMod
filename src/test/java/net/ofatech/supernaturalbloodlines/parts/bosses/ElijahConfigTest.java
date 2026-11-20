package net.ofatech.supernaturalbloodlines.parts.bosses;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The defaults the plan decided (open question 5, 17d): 5% a day, a 7-day oath, a half-health Elixir, a 60-second delay for Rebekah. */
class ElijahConfigTest {
    @Test
    void theDefaultsAreTheOnesTheOwnerAccepted() {
        assertEquals(0.05, BossesConfig.elijahChance());
        assertEquals(7, BossesConfig.elijahOathDays());
        assertEquals(0.5, BossesConfig.elixirHealFraction());
        assertEquals(60, BossesConfig.rebekahDelaySeconds());
    }

    @Test
    void anOverrideWinsAndClearsAgain() {
        BossesConfig.override("elijah_oath_days", 2);
        assertEquals(2, BossesConfig.elijahOathDays());
        BossesConfig.override("elijah_oath_days", null);
        assertEquals(7, BossesConfig.elijahOathDays());
    }
}
