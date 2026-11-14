package net.ofatech.supernaturalbloodlines.parts.species;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import java.util.UUID;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.VillagerDiscount;
import net.ofatech.supernaturalbloodlines.parts.species.witch.RiteCost;
import net.ofatech.supernaturalbloodlines.parts.species.witch.WitchAltarBonus;
import net.ofatech.supernaturalbloodlines.shared.werewolves.MoonControl;
import org.junit.jupiter.api.Test;

/** The numbers the skills of phase 12c Task 2 change, row by row. */
class SkillRuleMathTest {
    @Test
    void theRiteCostWithTheCastersSkills() {
        // base 100, no witch: Steady Circle x0.95, Ancestral Voice x0.5, both x0.475
        assertEquals(100, RiteCost.split(100, false, false, 0.9, 1.0, 1.0, 0).altarCost());
        assertEquals(95, RiteCost.split(100, false, false, 0.9, 0.95, 1.0, 0).altarCost());
        assertEquals(50, RiteCost.split(100, false, false, 0.9, 0.5, 1.0, 0).altarCost());
        assertEquals(48, RiteCost.split(100, false, false, 0.9, 0.95 * 0.5, 1.0, 0).altarCost());
        // on top of the witch discount (x0.75, rounded up) and the Emissary's (x0.9, or x0.8 with Emissary's Bond), each rounded up
        assertEquals(68, RiteCost.split(100, true, true, 0.9, 1.0, 1.0, 0).altarCost()); // 75 -> 68
        assertEquals(60, RiteCost.split(100, true, true, 0.8, 1.0, 1.0, 0).altarCost()); // 75 -> 60
        assertEquals(57, RiteCost.split(100, true, true, 0.8, 0.95, 1.0, 0).altarCost()); // 60 -> 57
        // the old signatures are the same as no skill
        assertEquals(RiteCost.split(100, true, true, 0), RiteCost.split(100, true, true, 0.9, 1.0, 1.0, 0));
        assertEquals(0, RiteCost.split(0, true, true, 0.8, 0.5, 0.9, 50).altarCost());
    }

    @Test
    void spentWiselyStretchesTheSiphonedPool() {
        RiteCost.Split plain = RiteCost.split(100, false, false, 0.9, 1.0, 1.0, 90);
        assertEquals(10, plain.altarCost());
        assertEquals(90, plain.siphoned());
        RiteCost.Split wise = RiteCost.split(100, false, false, 0.9, 1.0, 0.9, 90);
        assertEquals(0, wise.altarCost());
        assertEquals(90, wise.siphoned());
        RiteCost.Split half = RiteCost.split(100, false, false, 0.9, 1.0, 0.9, 45);
        assertEquals(50, half.altarCost());
        assertEquals(45, half.siphoned());
        RiteCost.Split rich = RiteCost.split(100, false, false, 0.9, 1.0, 0.9, 5000);
        assertEquals(0, rich.altarCost());
        assertEquals(90, rich.siphoned());
        assertEquals(0, RiteCost.split(100, false, false, 0.9, 1.0, 0.9, 0).siphoned());
    }

    @Test
    void theAltarRangeWithWideCircle() {
        assertEquals(18, WitchAltarBonus.scaledRange(12));
        assertEquals(21, WitchAltarBonus.scaledRange(12, 1.75));
        assertEquals(56, WitchAltarBonus.scaledRange(32, 1.75));
    }

    @Test
    void moonCalmSparesTheGivenShareOfNights() {
        UUID id = UUID.fromString("00000000-0000-0000-0000-00000000c0de");
        int quarter = 0;
        int never = 0;
        int always = 0;
        int nights = 4000;
        for (long day = 0; day < nights; day++) {
            quarter += MoonControl.sparedOn(id, day, 0.25) ? 1 : 0;
            never += MoonControl.sparedOn(id, day, 0) ? 1 : 0;
            always += MoonControl.sparedOn(id, day, 1) ? 1 : 0;
            assertEquals(MoonControl.sparedOn(id, day, 0.25), MoonControl.sparedOn(id, day, 0.25));
        }
        assertEquals(0, never);
        assertEquals(nights, always);
        assertTrue(quarter > nights * 0.22 && quarter < nights * 0.28, "spared " + quarter + " of " + nights);
        // it is not the born-wolf coin
        int same = 0;
        for (long day = 0; day < nights; day++) {
            same += MoonControl.sparedOn(id, day, 0.5) == MoonControl.bornSparedOn(id, day) ? 1 : 0;
        }
        assertTrue(same < nights * 0.6 && same > nights * 0.4, "the two coins agree on " + same + " of " + nights);
        assertFalse(MoonControl.sparedOn(id, 5, 0));
    }

    @Test
    void aSavedDiscountFromBeforeStillLoads() {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000042");
        // a record saved before "minor" existed has no such field
        var encoded = VillagerDiscount.CODEC.encodeStart(JsonOps.INSTANCE, new VillagerDiscount(id, 20, 24000L)).getOrThrow();
        encoded.getAsJsonObject().remove("minor");
        VillagerDiscount read = VillagerDiscount.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertEquals(new VillagerDiscount(id, 20, 0, 24000L), read);
        var withMinor = VillagerDiscount.CODEC.encodeStart(JsonOps.INSTANCE, new VillagerDiscount(id, 20, 25, 24000L)).getOrThrow();
        assertEquals(25, VillagerDiscount.CODEC.parse(JsonOps.INSTANCE, withMinor).getOrThrow().minor());
    }
}
