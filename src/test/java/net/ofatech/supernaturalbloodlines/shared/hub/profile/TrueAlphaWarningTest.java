package net.ofatech.supernaturalbloodlines.shared.hub.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import org.junit.jupiter.api.Test;

class TrueAlphaWarningTest {
    @Test
    void onlyABetaOrOmegaWithAnOpenPathIsWarned() {
        for (WolfState.Rank r : WolfState.Rank.values()) {
            boolean beta = r == WolfState.Rank.BETA || r == WolfState.Rank.OMEGA;
            assertEquals(beta, TrueAlphaWarning.applies(r, true), r.name());
            assertEquals(false, TrueAlphaWarning.applies(r, false), r.name() + " whose path is closed");
        }
    }
}

