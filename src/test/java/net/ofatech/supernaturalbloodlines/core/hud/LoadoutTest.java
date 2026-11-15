package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class LoadoutTest {
    private static final ResourceLocation JUMP = ResourceLocation.parse("vampirism:jump_boost");

    private static Loadout roundTrip(Loadout l) {
        var json = Loadout.CODEC.encodeStart(JsonOps.INSTANCE, l).getOrThrow(IllegalStateException::new);
        return Loadout.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(IllegalStateException::new);
    }

    @Test
    void aNewPlayerHasBiteInSlotOneAndNothingElse() {
        assertEquals(Optional.of(Loadout.BITE), Loadout.DEFAULT.get(0));
        for (int i = 1; i < Loadout.SLOTS; i++) {
            assertEquals(Optional.empty(), Loadout.DEFAULT.get(i));
        }
    }

    @Test
    void roundTripKeepsEverySlot() {
        Loadout l = Loadout.DEFAULT.with(3, JUMP).with(0, null);
        assertEquals(l, roundTrip(l));
        assertEquals(Optional.of(JUMP), roundTrip(l).get(3));
        assertEquals(Optional.empty(), roundTrip(l).get(0));
    }

    @Test
    void aClearedLoadoutStaysClearedAfterTheRoundTrip() {
        Loadout cleared = Loadout.DEFAULT.with(0, null);
        assertEquals(cleared, roundTrip(cleared), "Bite is never put back on its own");
    }

    @Test
    void anEmptyTagIsTheDefaultLoadout() {
        assertEquals(Loadout.DEFAULT, Loadout.CODEC.parse(JsonOps.INSTANCE, new JsonObject()).getOrThrow(IllegalStateException::new));
    }

    @Test
    void aShortOrLongListIsPaddedOrCut() {
        JsonObject tag = new JsonObject();
        com.google.gson.JsonArray two = new com.google.gson.JsonArray();
        two.add("");
        two.add("vampirism:teleport");
        tag.add("slots", two);
        Loadout l = Loadout.CODEC.parse(JsonOps.INSTANCE, tag).getOrThrow(IllegalStateException::new);
        assertEquals(Optional.of(ResourceLocation.parse("vampirism:teleport")), l.get(1));
        assertEquals(Optional.empty(), l.get(5));
        com.google.gson.JsonArray ten = new com.google.gson.JsonArray();
        for (int i = 0; i < 10; i++) {
            ten.add("vampirism:a" + i);
        }
        tag.add("slots", ten);
        assertEquals(Optional.of(ResourceLocation.parse("vampirism:a5")), Loadout.CODEC.parse(JsonOps.INSTANCE, tag).getOrThrow(IllegalStateException::new).get(5));
    }

    @Test
    void aBadIdIsAnEmptySlotNotACrash() {
        JsonObject tag = new JsonObject();
        com.google.gson.JsonArray slots = new com.google.gson.JsonArray();
        slots.add("NOT AN ID!");
        tag.add("slots", slots);
        assertEquals(Optional.empty(), Loadout.CODEC.parse(JsonOps.INSTANCE, tag).getOrThrow(IllegalStateException::new).get(0));
    }

    @Test
    void duplicatesAreAllowedAndWithIsImmutable() {
        Loadout a = Loadout.DEFAULT;
        Loadout b = a.with(1, Loadout.BITE);
        assertEquals(Optional.of(Loadout.BITE), b.get(1));
        assertEquals(Optional.empty(), a.get(1), "the original is unchanged");
        assertTrue(b.contains(Loadout.BITE));
        assertFalse(Loadout.DEFAULT.with(0, null).contains(Loadout.BITE));
    }

    @Test
    void theFallbackFillsEmptySlotsTwoToSixInOrderAndSkipsWhatIsThere() {
        ResourceLocation a = ResourceLocation.parse("vampirism:a");
        ResourceLocation b = ResourceLocation.parse("vampirism:b");
        ResourceLocation c = ResourceLocation.parse("vampirism:c");
        Loadout l = Loadout.DEFAULT.with(2, b).withFallback(List.of(a, b, c));
        assertEquals(Optional.of(Loadout.BITE), l.get(0), "slot 1 is left alone");
        assertEquals(Optional.of(a), l.get(1));
        assertEquals(Optional.of(b), l.get(2), "a slot the player filled is kept");
        assertEquals(Optional.of(c), l.get(3), "an action already in the list is not repeated");
        assertEquals(Optional.empty(), l.get(4));
    }

    @Test
    void theFallbackNeverTouchesAClearedSlotOne() {
        Loadout l = Loadout.DEFAULT.with(0, null).withFallback(List.of(ResourceLocation.parse("vampirism:a")));
        assertEquals(Optional.empty(), l.get(0));
        assertEquals(Optional.of(ResourceLocation.parse("vampirism:a")), l.get(1));
    }

    @Test
    void theFallbackWithNothingUnlockedChangesNothing() {
        assertEquals(Loadout.DEFAULT, Loadout.DEFAULT.withFallback(List.of()));
    }

    @Test
    void theFallbackFillsAtMostFiveSlots() {
        List<ResourceLocation> many = java.util.stream.IntStream.range(0, 9).mapToObj(i -> ResourceLocation.parse("vampirism:a" + i)).toList();
        Loadout l = Loadout.DEFAULT.withFallback(many);
        assertEquals(Optional.of(many.get(4)), l.get(5));
    }

    @Test
    void aSlotOutOfRangeIsRefused() {
        org.junit.jupiter.api.Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Loadout.DEFAULT.get(6));
        org.junit.jupiter.api.Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Loadout.DEFAULT.with(-1, JUMP));
        assertFalse(Loadout.validSlot(6));
        assertTrue(Loadout.validSlot(5));
    }
}
