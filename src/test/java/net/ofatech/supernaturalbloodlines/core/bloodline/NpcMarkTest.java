package net.ofatech.supernaturalbloodlines.core.bloodline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NpcMarkTest {
    @Test
    void aWildMarkSurvivesTheCodec() {
        NpcMark m = NpcMark.wild(7, 24);
        assertEquals(m, NpcMark.CODEC.parse(JsonOps.INSTANCE, NpcMark.CODEC.encodeStart(JsonOps.INSTANCE, m).getOrThrow()).getOrThrow());
    }

    @Test
    void aSpiritMarkKeepsItsOwnerAndItsEnd() {
        UUID owner = UUID.randomUUID();
        NpcMark m = NpcMark.spirit(owner, 99_000);
        NpcMark back = NpcMark.CODEC.parse(JsonOps.INSTANCE, NpcMark.CODEC.encodeStart(JsonOps.INSTANCE, m).getOrThrow()).getOrThrow();
        assertEquals(owner, back.owner());
        assertEquals(99_000, back.expires());
        assertEquals(NpcMark.Kind.SPIRIT, back.kind());
    }

    @Test
    void everyFieldIsOptional() {
        NpcMark back = NpcMark.CODEC.parse(JsonOps.INSTANCE, new JsonObject()).getOrThrow();
        assertEquals(NpcMark.Kind.WILD, back.kind());
        assertNull(back.owner());
        assertEquals(0, back.packId());
    }
}