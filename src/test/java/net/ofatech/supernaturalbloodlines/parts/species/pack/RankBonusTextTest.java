package net.ofatech.supernaturalbloodlines.parts.species.pack;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RankBonusTextTest {
    @Test
    void theBonusIsWordedLikeTheMockup() {
        assertEquals("+20% attack, +4 max health", RankEffects.bonusText(0.20, 4));
        assertEquals("+24% attack, +4.8 max health", RankEffects.bonusText(0.24, 4.8));
    }

    @Test
    void theScaleStacksTheTrueAlphaAndTheSparks() {
        assertEquals(1.0, RankEffects.alphaScale(net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank.ALPHA, -1, 5), 1e-9);
        assertEquals(1.1, RankEffects.alphaScale(net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank.TRUE_ALPHA, -1, 5), 1e-9);
        assertEquals(1.2, RankEffects.alphaScale(net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank.ALPHA, 3, 5), 1e-9);
    }
}

