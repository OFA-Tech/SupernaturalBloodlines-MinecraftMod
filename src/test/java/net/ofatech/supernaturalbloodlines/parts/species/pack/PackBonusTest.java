package net.ofatech.supernaturalbloodlines.parts.species.pack;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.ofatech.supernaturalbloodlines.parts.species.witch.WitchAltarBonus;
import org.junit.jupiter.api.Test;

class PackBonusTest {
    @Test
    void nothingBelowThreeRawBetasHoweverManyAreDoubled() {
        assertEquals(0, PackBonus.bonusFor(0, 0));
        assertEquals(0, PackBonus.bonusFor(2, 4));
    }

    @Test
    void fivePercentPerCountedBetaCappedAtTwentyFive() {
        assertEquals(0.15, PackBonus.bonusFor(3, 3), 1e-9);
        assertEquals(0.20, PackBonus.bonusFor(3, 4), 1e-9);
        assertEquals(0.25, PackBonus.bonusFor(3, 5), 1e-9);
        assertEquals(0.25, PackBonus.bonusFor(10, 20), 1e-9);
    }

    @Test
    void emissaryCostRoundsUpAndIsNeverFree() {
        assertEquals(900, WitchAltarBonus.scaledEmissaryCost(1000));
        assertEquals(1, WitchAltarBonus.scaledEmissaryCost(1));
        assertEquals(0, WitchAltarBonus.scaledEmissaryCost(0));
    }
}
