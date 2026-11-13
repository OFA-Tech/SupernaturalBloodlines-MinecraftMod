package net.ofatech.supernaturalbloodlines.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.EnumSet;
import java.util.List;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PartEnabledConditionTest {
    @AfterEach
    void reset() {
        EnabledParts.set(List.of());
    }

    @Test
    void decodesPartKey() {
        JsonElement json = JsonParser.parseString("{\"part\": \"enchantedTensura\"}");
        PartEnabledCondition c = PartEnabledCondition.CODEC.codec().parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals("enchantedTensura", c.part());
    }

    @Test
    void missingPartKeyDoesNotDecode() {
        assertTrue(PartEnabledCondition.CODEC.codec().parse(JsonOps.INSTANCE, JsonParser.parseString("{}")).error().isPresent());
    }

    @Test
    void encodesBackToTheSameJson() {
        JsonElement json = PartEnabledCondition.CODEC.codec().encodeStart(JsonOps.INSTANCE, new PartEnabledCondition("species")).getOrThrow();
        assertEquals(JsonParser.parseString("{\"part\": \"species\"}"), json);
    }

    @Test
    void trueOnlyForEnabledParts() {
        EnabledParts.set(EnumSet.of(Part.VAMPIRISM_TENSURA));
        ICondition.IContext ctx = ICondition.IContext.EMPTY;
        assertTrue(new PartEnabledCondition("vampirismTensura").test(ctx));
        assertFalse(new PartEnabledCondition("enchantedTensura").test(ctx));
    }

    @Test
    void everythingIsOffBeforeTheConstructorRuns() {
        for (Part p : Part.values()) {
            assertFalse(EnabledParts.isEnabled(p.configKey()), p.toString());
        }
    }

    @Test
    void everyPartKeyIsKnown() {
        EnabledParts.set(EnumSet.allOf(Part.class));
        for (Part p : Part.values()) {
            assertTrue(EnabledParts.isEnabled(p.configKey()), p.toString());
        }
    }

    @Test
    void unknownPartIsFalseEvenWhenAllPartsAreOn() {
        EnabledParts.set(EnumSet.allOf(Part.class));
        assertFalse(EnabledParts.isEnabled("noSuchPart"));
        assertFalse(EnabledParts.isEnabled("noSuchPart"));
    }
}
