package net.ofatech.supernaturalbloodlines.core.blood;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import org.junit.jupiter.api.Test;

class BloodKindTest {
    @Test
    void speciesMapToTheirOwnKindAndEveryOtherIsCommon() {
        assertEquals(BloodKind.COMMON_VAMPIRE, BloodKind.of(Species.NONE));
        assertEquals(BloodKind.COMMON_VAMPIRE, BloodKind.of(Species.WITCH));
        assertEquals(BloodKind.COMMON_VAMPIRE, BloodKind.of(Species.SIPHONER));
        assertEquals(BloodKind.HERETIC, BloodKind.of(Species.HERETIC));
        assertEquals(BloodKind.ORIGINAL, BloodKind.of(Species.ORIGINAL));
        assertEquals(BloodKind.ORIGINAL_HYBRID, BloodKind.of(Species.ORIGINAL_HYBRID));
        assertEquals(BloodKind.HYBRID, BloodKind.of(Species.HYBRID));
        assertEquals(BloodKind.TRIBRID, BloodKind.of(Species.TRIBRID));
    }

    @Test
    void anUnstampedOrUnknownBottleIsCommonVampireBlood() {
        assertEquals(BloodKind.COMMON_VAMPIRE, BloodKind.parse(null));
        assertEquals(BloodKind.COMMON_VAMPIRE, BloodKind.parse(""));
        assertEquals(BloodKind.COMMON_VAMPIRE, BloodKind.parse("NONE"));
        assertEquals(BloodKind.COMMON_VAMPIRE, BloodKind.parse("bogus"));
        assertEquals(BloodKind.ORIGINAL, BloodKind.parse("ORIGINAL"));
    }

    @Test
    void speciesNameRoundTripsThroughParse() {
        for (BloodKind k : BloodKind.values()) {
            assertEquals(k, BloodKind.parse(k.speciesName()));
        }
        assertEquals("NONE", BloodKind.COMMON_VAMPIRE.speciesName());
    }

    @Test
    void onlyAnOriginalsBloodNamesTheSire() {
        for (BloodKind k : BloodKind.values()) {
            assertEquals(k == BloodKind.ORIGINAL || k == BloodKind.ORIGINAL_HYBRID, k.namesSire(), k.name());
        }
    }

    @Test
    void everyKindButCommonIsSecret() {
        assertFalse(BloodKind.COMMON_VAMPIRE.secret());
        for (BloodKind k : BloodKind.values()) {
            if (k != BloodKind.COMMON_VAMPIRE) {
                assertTrue(k.secret(), k.name());
            }
        }
    }
}
