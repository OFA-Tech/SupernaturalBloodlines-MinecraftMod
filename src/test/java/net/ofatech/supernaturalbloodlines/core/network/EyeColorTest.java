package net.ofatech.supernaturalbloodlines.core.network;

import static net.ofatech.supernaturalbloodlines.core.network.EyeColorPayload.Color.BLUE;
import static net.ofatech.supernaturalbloodlines.core.network.EyeColorPayload.Color.GOLD;
import static net.ofatech.supernaturalbloodlines.core.network.EyeColorPayload.Color.RED;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Lineage;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank;
import org.junit.jupiter.api.Test;

class EyeColorTest {
    private static WolfState wolf(Rank r, boolean innocent) {
        return WolfState.NONE.toBuilder().lineage(Lineage.BITTEN).rank(r).killedInnocent(innocent).build();
    }

    @Test
    void goldForBetasAndOmegasRedForAlphasBlueAfterAnInnocentKill() {
        assertEquals(GOLD, EyeColorPayload.Color.of(wolf(Rank.OMEGA, false)));
        assertEquals(GOLD, EyeColorPayload.Color.of(wolf(Rank.BETA, false)));
        assertEquals(RED, EyeColorPayload.Color.of(wolf(Rank.ALPHA, false)));
        assertEquals(RED, EyeColorPayload.Color.of(wolf(Rank.TRUE_ALPHA, false)));
        assertEquals(BLUE, EyeColorPayload.Color.of(wolf(Rank.OMEGA, true)));
        assertEquals(BLUE, EyeColorPayload.Color.of(wolf(Rank.BETA, true)));
        // an Alpha keeps the red of the spark; the blue only decides who may become a True Alpha
        assertEquals(RED, EyeColorPayload.Color.of(wolf(Rank.ALPHA, true)));
        assertEquals(GOLD, EyeColorPayload.Color.of(WolfState.NONE));
    }
}
