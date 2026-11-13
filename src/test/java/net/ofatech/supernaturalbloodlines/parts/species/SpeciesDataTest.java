package net.ofatech.supernaturalbloodlines.parts.species;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class SpeciesDataTest {
    @Test
    void countsEachOriginalOnce() {
        SpeciesData d = new SpeciesData();
        UUID a = UUID.randomUUID();
        d.setOriginal(a, true);
        d.setOriginal(a, true);
        d.setOriginal(UUID.randomUUID(), true);
        assertEquals(2, d.count());
        d.setOriginal(a, false);
        assertEquals(1, d.count());
        d.setOriginal(a, false);
        assertEquals(1, d.count());
    }

    @Test
    void survivesSaveAndLoad() {
        SpeciesData d = new SpeciesData();
        UUID a = UUID.randomUUID();
        d.setOriginal(a, true);
        d.setOriginal(UUID.randomUUID(), true);
        SpeciesData back = SpeciesData.load(d.save(new CompoundTag(), null), null);
        assertEquals(2, back.count());
        back.setOriginal(a, false);
        assertEquals(1, back.count());
    }
}
