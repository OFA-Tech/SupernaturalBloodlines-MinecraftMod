package net.ofatech.supernaturalbloodlines.parts.species.sireline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.Test;

class InfectionSireTest {
    @Test
    void survivesTheCodec() {
        InfectionSire in = new InfectionSire(UUID.randomUUID(), "Ann", 123456L);
        var tag = InfectionSire.CODEC.encodeStart(NbtOps.INSTANCE, in).getOrThrow();
        assertEquals(in, InfectionSire.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow());
    }

    @Test
    void countsForExactlyUntilItExpires() {
        InfectionSire in = new InfectionSire(UUID.randomUUID(), "Ann", 100);
        assertFalse(in.expired(99));
        assertTrue(in.expired(100));
    }
}
