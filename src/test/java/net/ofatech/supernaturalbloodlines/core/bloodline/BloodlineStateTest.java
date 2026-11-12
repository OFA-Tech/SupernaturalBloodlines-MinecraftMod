package net.ofatech.supernaturalbloodlines.core.bloodline;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BloodlineStateTest {
    private static BloodlineState decode(String json) {
        return BloodlineState.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
    }

    @Test
    void roundTripsThroughCodec() {
        WolfState wolf = new WolfState(WolfState.Lineage.BITTEN, WolfState.Rank.BETA, true, true, true,
                UUID.fromString("00000000-0000-0000-0000-0000000000c3"), 777L, 12, true, Set.of("a", "b"));
        BloodlineState s = new BloodlineState(Species.TRIBRID, true, UUID.fromString("00000000-0000-0000-0000-0000000000a1"),
                true, true, 123L, UUID.fromString("00000000-0000-0000-0000-0000000000b2"), 50, 999L, 7, "x:y",
                4, true, 888L, true, wolf);

        JsonElement json = BloodlineState.CODEC.encodeStart(JsonOps.INSTANCE, s).getOrThrow();
        assertEquals(s, BloodlineState.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    @Test
    void noneRoundTrips() {
        JsonElement json = BloodlineState.CODEC.encodeStart(JsonOps.INSTANCE, BloodlineState.NONE).getOrThrow();
        assertEquals(BloodlineState.NONE, BloodlineState.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    @Test
    void unknownSpeciesDecodesToNone() {
        assertEquals(Species.NONE, decode("{\"species\":\"DRAGON\"}").species());
    }

    @Test
    void unknownWolfEnumsDecodeToNone() {
        WolfState w = decode("{\"wolf\":{\"lineage\":\"MOONCHILD\",\"rank\":\"EMPEROR\"}}").wolf();
        assertEquals(WolfState.Lineage.NONE, w.lineage());
        assertEquals(WolfState.Rank.NONE, w.rank());
    }

    @Test
    void missingFieldsDecodeToDefaults() {
        assertEquals(BloodlineState.NONE, decode("{}"));
        assertEquals(20, decode("{\"species\":\"HYBRID\"}").thirst());
        assertEquals(WolfState.NONE, decode("{\"species\":\"HYBRID\"}").wolf());
    }

    @Test
    void speciesChangeStampsTurnedAt() {
        Outcome o = BloodlineRules.apply(BloodlineState.NONE, BloodlineEvent.RITE_OF_IMMORTALITY,
                BloodlineContext.defaults().withGameTime(500));
        assertEquals(500, o.state().turnedAt());
    }
}
