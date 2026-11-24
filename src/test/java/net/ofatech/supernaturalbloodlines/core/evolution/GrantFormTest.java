package net.ofatech.supernaturalbloodlines.core.evolution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class GrantFormTest {
    @Test
    void permanentParses() {
        assertEquals(GrantForm.PERMANENT, GrantForm.parse("permanent").orElseThrow());
    }

    @Test
    void onlyPermanentNamesARemoveTime() {
        assertEquals(OptionalInt.of(-1), GrantForm.PERMANENT.removeTime(), "-1 is Tensura's permanent skill, as Ascension's altar learns it");
        assertTrue(GrantForm.DEFAULT.removeTime().isEmpty(), "the default form is Tensura's own two-argument learn");
    }

    @Test
    void everyFormRoundTripsThroughItsName() {
        for (GrantForm f : GrantForm.values()) {
            assertEquals(f, GrantForm.parse(f.name().toLowerCase(Locale.ROOT)).orElseThrow());
        }
        assertTrue(GrantForm.parse("temporary").isEmpty());
    }
}
