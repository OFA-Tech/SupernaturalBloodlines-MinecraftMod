package net.ofatech.supernaturalbloodlines.parts.bosses;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The defaults the plan decided (open question 6): 0.35 a full moon, players hunted, 90 seconds of warning. */
class KlausConfigTest {
    @Test
    void theDefaultsAreTheOnesTheOwnerAccepted() {
        assertEquals(0.35, BossesConfig.klausChance());
        assertTrue(BossesConfig.klausHuntsPlayers());
        assertEquals(90, BossesConfig.klausWarningSeconds());
    }

    @Test
    void anOverrideWinsAndClearsAgain() {
        BossesConfig.override("klaus_warning_seconds", 5);
        assertEquals(5, BossesConfig.klausWarningSeconds());
        BossesConfig.override("klaus_warning_seconds", null);
        assertEquals(90, BossesConfig.klausWarningSeconds());
    }
}
