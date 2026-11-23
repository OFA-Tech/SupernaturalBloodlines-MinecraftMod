package net.ofatech.supernaturalbloodlines.core.wheel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class TensuraKindsTest {
    private static ResourceLocation rl(String s) {
        return ResourceLocation.parse(s);
    }

    @Test
    void overridesParseAndRejectUnknownKinds() {
        TensuraKinds k = TensuraKinds.parse("{\"overrides\": {\"tensura:coercion\": \"INSTANT\", \"tensura:x\": \"HELD\"}}");
        assertEquals(Optional.of(PowerKind.INSTANT), k.override(rl("tensura:coercion")));
        assertEquals(Optional.of(PowerKind.HELD), k.override(rl("tensura:x")));
        assertEquals(Optional.empty(), k.override(rl("tensura:other")));
        assertThrows(IllegalArgumentException.class, () -> TensuraKinds.parse("{\"overrides\": {\"tensura:coercion\": \"WHENEVER\"}}"));
        assertThrows(IllegalArgumentException.class, () -> TensuraKinds.parse("{\"overrides\": {\"Not Valid\": \"HELD\"}}"));
        assertThrows(IllegalArgumentException.class, () -> TensuraKinds.parse("not json"));
    }

    @Test
    void chainsParseAndRejectAnIdInTwoChains() {
        assertThrows(IllegalArgumentException.class, () -> TensuraKinds.parse("{\"chains\": [[\"a:x\", \"a:y\"], [\"a:y\", \"a:z\"]]}"));
        assertThrows(IllegalArgumentException.class, () -> TensuraKinds.parse("{\"chains\": [[\"a:x\", \"a:x\"]]}"));
    }

    @Test
    void onlyTheHighestOwnedTierIsShown() {
        TensuraKinds k = TensuraKinds.parse("{\"chains\": [[\"a:low\", \"a:mid\", \"a:high\"]]}");
        assertTrue(k.hiddenByHigherTier(rl("a:low"), Set.of(rl("a:low"), rl("a:mid"))));
        assertTrue(k.hiddenByHigherTier(rl("a:low"), Set.of(rl("a:low"), rl("a:high"))));
        assertFalse(k.hiddenByHigherTier(rl("a:mid"), Set.of(rl("a:low"), rl("a:mid"))));
        assertFalse(k.hiddenByHigherTier(rl("a:high"), Set.of(rl("a:low"), rl("a:mid"), rl("a:high"))));
        assertFalse(k.hiddenByHigherTier(rl("a:unrelated"), Set.of(rl("a:high"))));
    }

    @Test
    void theShippedFileParses() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/data/supernaturalbloodlines/wheel/tensura_kinds.json")) {
            TensuraKinds k = TensuraKinds.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            assertTrue(k.hiddenByHigherTier(rl("tensura:self_regeneration"), Set.of(rl("tensura:self_regeneration"), rl("tensura:infinite_regeneration"))));
        }
    }
}
