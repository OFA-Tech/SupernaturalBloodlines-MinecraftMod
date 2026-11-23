package net.ofatech.supernaturalbloodlines.core.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import org.junit.jupiter.api.Test;

class DaggerRulesTest {
    @Test
    void bothDaggersLodgeInAnOriginal() {
        assertTrue(DaggerRules.lodges(false, Species.ORIGINAL));
        assertTrue(DaggerRules.lodges(true, Species.ORIGINAL));
    }

    @Test
    void onlyTheCursedDaggerLodgesInAnOriginalHybrid() {
        assertFalse(DaggerRules.lodges(false, Species.ORIGINAL_HYBRID));
        assertTrue(DaggerRules.lodges(true, Species.ORIGINAL_HYBRID));
    }

    @Test
    void noDaggerLodgesInAnyoneElse() {
        for (Species s : Species.values()) {
            if (s != Species.ORIGINAL && s != Species.ORIGINAL_HYBRID) {
                assertFalse(DaggerRules.lodges(false, s), "plain dagger, " + s);
                assertFalse(DaggerRules.lodges(true, s), "cursed dagger, " + s);
            }
        }
    }

    @Test
    void theCursedDaggerIsNeverWeakerThanThePlainOne() {
        for (Species s : Species.values()) {
            assertTrue(!DaggerRules.lodges(false, s) || DaggerRules.lodges(true, s), s.toString());
        }
        assertEquals(2, java.util.Arrays.stream(Species.values()).filter(s -> DaggerRules.lodges(true, s)).count());
    }
}
