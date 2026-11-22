package net.ofatech.supernaturalbloodlines.core.hud;

import static net.ofatech.supernaturalbloodlines.core.hud.RipHeartRules.Result.FINISH;
import static net.ofatech.supernaturalbloodlines.core.hud.RipHeartRules.Result.NO_EFFECT;
import static net.ofatech.supernaturalbloodlines.core.hud.RipHeartRules.Result.TOO_STRONG;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RipHeartRulesTest {
    @Test
    void theThresholdItselfIsStillWeakEnough() {
        assertEquals(FINISH, RipHeartRules.decide(10.0f, 10.0f, false));
        assertEquals(TOO_STRONG, RipHeartRules.decide(10.01f, 10.0f, false));
    }

    @Test
    void anOriginalAtLowHealthTakesNothingButTheUserCannotTell() {
        assertEquals(NO_EFFECT, RipHeartRules.decide(3.0f, 10.0f, true));
        assertEquals(NO_EFFECT, RipHeartRules.decide(10.0f, 10.0f, true));
    }

    @Test
    void aHealthyOriginalIsRefusedLikeAnyHealthyTarget() {
        assertEquals(TOO_STRONG, RipHeartRules.decide(50.0f, 10.0f, true));
        assertEquals(RipHeartRules.decide(50.0f, 10.0f, false), RipHeartRules.decide(50.0f, 10.0f, true));
    }

    @Test
    void theThresholdIsTheConfiguredOne() {
        assertEquals(FINISH, RipHeartRules.decide(20.0f, 20.0f, false));
        assertEquals(TOO_STRONG, RipHeartRules.decide(20.0f, 19.0f, false));
    }
}
