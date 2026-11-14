package net.ofatech.supernaturalbloodlines.core.supernatural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SupernaturalStateTest {
    private static SupernaturalState decode(String json) {
        return SupernaturalState.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
    }

    private static SupernaturalState full() {
        return new SupernaturalState(1, Map.of("vampire", 3, "witch", 1), Map.of("vampire", 100L), 4, 9,
                Set.of("L-VAM-1a", "L-VAM-1b"), Set.of("L-VAM-1a"), Map.of("L-VAM-2b", 5L), Set.of("S-VAM-1"),
                Set.of("Q-VAM-6"), Set.of("tensura:drain"), List.of("layer", "rank"));
    }

    @Test
    void roundTripsThroughCodec() {
        SupernaturalState s = full();
        JsonElement json = SupernaturalState.CODEC.encodeStart(JsonOps.INSTANCE, s).getOrThrow();
        assertEquals(s, SupernaturalState.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    @Test
    void emptyTagLoadsNone() {
        assertEquals(SupernaturalState.NONE, decode("{}"));
    }

    @Test
    void noneRoundTrips() {
        JsonElement json = SupernaturalState.CODEC.encodeStart(JsonOps.INSTANCE, SupernaturalState.NONE).getOrThrow();
        assertEquals(SupernaturalState.NONE, SupernaturalState.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    @Test
    void aBadFieldFallsBackToItsDefaultAndTheRestLoads() {
        SupernaturalState s = decode("{\"sp\":\"lots\",\"skills\":[\"S-VAM-1\"]}");
        assertEquals(0, s.sp());
        assertEquals(Set.of("S-VAM-1"), s.skills());
    }

    @Test
    void clientViewDropsTheServerOnlyFields() {
        SupernaturalState view = full().clientView();
        assertTrue(view.granted().isEmpty());
        assertTrue(view.backfill().isEmpty());
        assertEquals(0, view.schema());
        // everything the hub shows stays
        assertEquals(full().rungs(), view.rungs());
        assertEquals(full().skills(), view.skills());
        assertEquals(4, view.sp());
        assertEquals(full().counters(), view.counters());
    }

    @Test
    void withTrackingOfTakesOnlyTheTracking() {
        SupernaturalState s = SupernaturalState.NONE.toBuilder().sp(7).build().withTrackingOf(full());
        assertEquals(Set.of("tensura:drain"), s.granted());
        assertEquals(List.of("layer", "rank"), s.backfill());
        assertEquals(7, s.sp());
        assertEquals(0, s.schema());
    }

    @Test
    void rungDefaultsToZeroAndBuilderSetsOne() {
        assertEquals(0, SupernaturalState.NONE.rung(SupernaturalRace.VAMPIRE));
        assertEquals(2, SupernaturalState.NONE.toBuilder().rung(SupernaturalRace.VAMPIRE, 2).build().rung(SupernaturalRace.VAMPIRE));
    }
}
