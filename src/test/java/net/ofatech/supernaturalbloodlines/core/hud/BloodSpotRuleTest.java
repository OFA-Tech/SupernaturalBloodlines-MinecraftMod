package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.core.hud.BloodSpotRule.Show;
import org.junit.jupiter.api.Test;

class BloodSpotRuleTest {
    @Test
    void aVampireWithTensuraSeesBloodWhereTheFoodIs() {
        assertEquals(Show.BLOOD_IN_FOOD_SPOT, BloodSpotRule.decide(true, true, true, true, true));
    }

    @Test
    void withoutTensuraVampirismsOwnBarStays() {
        assertEquals(Show.VAMPIRISM_BAR, BloodSpotRule.decide(false, true, true, true, true));
    }

    @Test
    void aNonVampireKeepsTensurasFoodAndHasNoBloodBar() {
        Show s = BloodSpotRule.decide(true, true, true, false, true);
        assertEquals(Show.TENSURA_FOOD, s);
        assertFalse(s.hidesVampirismBar());
        assertFalse(s.hidesTensuraFood());
    }

    @Test
    void theSettingOffRestoresBothOriginals() {
        Show s = BloodSpotRule.decide(true, true, false, true, true);
        assertEquals(Show.VAMPIRISM_BAR, s);
        assertFalse(s.hidesTensuraFood());
        assertFalse(s.hidesVampirismBar());
    }

    @Test
    void nothingIsHiddenUntilTheRendererIsReady() {
        // the private hooks of Tensura could not be reached: the player must still see a blood value and a food value
        Show s = BloodSpotRule.decide(true, true, true, true, false);
        assertEquals(Show.VAMPIRISM_BAR, s);
        assertFalse(s.hidesVampirismBar());
        assertFalse(s.hidesTensuraFood());
    }

    @Test
    void withoutVampirismThereIsNoBloodAndTensuraKeepsItsFood() {
        assertEquals(Show.TENSURA_FOOD, BloodSpotRule.decide(true, false, true, true, true));
        assertEquals(Show.VAMPIRISM_BAR, BloodSpotRule.decide(false, false, true, false, true));
    }

    @Test
    void onlyTheBloodInFoodSpotHidesAnything() {
        assertTrue(Show.BLOOD_IN_FOOD_SPOT.hidesVampirismBar());
        assertTrue(Show.BLOOD_IN_FOOD_SPOT.hidesTensuraFood());
        assertFalse(Show.VAMPIRISM_BAR.hidesVampirismBar());
        assertFalse(Show.TENSURA_FOOD.hidesTensuraFood());
    }
}
