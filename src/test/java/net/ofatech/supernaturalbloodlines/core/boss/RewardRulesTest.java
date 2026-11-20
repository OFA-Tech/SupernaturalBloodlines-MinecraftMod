package net.ofatech.supernaturalbloodlines.core.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.core.boss.RewardRules.Defeat;
import net.ofatech.supernaturalbloodlines.core.boss.RewardRules.When;
import org.junit.jupiter.api.Test;

class RewardRulesTest {
    @Test
    void firstDefeatPaysOnDesiccation() {
        assertTrue(RewardRules.pays(When.FIRST_DEFEAT, Defeat.DESICCATED, false));
    }

    @Test
    void firstDefeatPaysOnDeath() {
        assertTrue(RewardRules.pays(When.FIRST_DEFEAT, Defeat.DIED, false));
    }

    @Test
    void deathOnlyPaysOnDeath() {
        assertFalse(RewardRules.pays(When.DEATH, Defeat.DESICCATED, false));
        assertTrue(RewardRules.pays(When.DEATH, Defeat.DIED, false));
    }

    @Test
    void nothingPaysTwice() {
        for (When w : When.values()) {
            for (Defeat d : Defeat.values()) {
                assertFalse(RewardRules.pays(w, d, true), w + " " + d);
            }
        }
    }

    @Test
    void theConfigWordsParseAndAnythingElseIsTheDefault() {
        assertEquals(When.DEATH, RewardRules.parse("death"));
        assertEquals(When.FIRST_DEFEAT, RewardRules.parse("first_defeat"));
        assertEquals(When.FIRST_DEFEAT, RewardRules.parse("garbage"));
    }
}
