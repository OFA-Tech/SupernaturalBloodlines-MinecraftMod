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

    /** What a player who can bite has: Bite in slot 1, offered. */
    private static final Loadout BITER = Loadout.DEFAULT.withBiteOffered();

    @Test
    void aNewPlayerHasNothingInAnySlot() {
        for (int i = 0; i < Loadout.SLOTS; i++) {
            assertEquals(Optional.empty(), Loadout.DEFAULT.get(i), "slot " + (i + 1));
        }
        assertFalse(Loadout.DEFAULT.biteOffered());
    }

    @Test
    void offeringBiteFillsAnEmptySlotOneOnceAndNeverAgain() {
        assertEquals(Optional.of(Loadout.BITE), BITER.get(0));
        for (int i = 1; i < Loadout.SLOTS; i++) {
            assertEquals(Optional.empty(), BITER.get(i));
        }
        assertTrue(BITER.biteOffered());
        Loadout cleared = BITER.with(0, null);
        assertEquals(cleared, cleared.withBiteOffered(), "a cleared Bite is not put back");
    }

    @Test
    void offeringBiteNeverOverwritesASlotThePlayerSet() {
        Loadout set = Loadout.DEFAULT.with(0, JUMP).withBiteOffered();
        assertEquals(Optional.of(JUMP), set.get(0));
        assertTrue(set.biteOffered(), "it is spent, so it is not put in a slot the player empties later");
        assertEquals(Optional.empty(), set.with(0, null).withBiteOffered().get(0));
    }

    @Test
    void aSaveFromBeforeTheFlagKeepsAClearedBiteCleared() {
        JsonObject tag = new JsonObject();
        com.google.gson.JsonArray slots = new com.google.gson.JsonArray();
        for (int i = 0; i < Loadout.SLOTS; i++) {
            slots.add("");
        }
        tag.add("slots", slots);
        Loadout old = Loadout.CODEC.parse(JsonOps.INSTANCE, tag).getOrThrow(IllegalStateException::new);
        assertTrue(old.biteOffered());
        assertEquals(Optional.empty(), old.withBiteOffered().get(0));
    }

    @Test
    void roundTripKeepsEverySlot() {
        Loadout l = BITER.with(3, JUMP).with(0, null);
        assertEquals(l, roundTrip(l));
        assertEquals(Optional.of(JUMP), roundTrip(l).get(3));
        assertEquals(Optional.empty(), roundTrip(l).get(0));
    }

    @Test
    void aClearedLoadoutStaysClearedAfterTheRoundTrip() {
        Loadout cleared = BITER.with(0, null);
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
        Loadout a = BITER;
        Loadout b = a.with(1, Loadout.BITE);
        assertEquals(Optional.of(Loadout.BITE), b.get(1));
        assertEquals(Optional.empty(), a.get(1), "the original is unchanged");
        assertTrue(b.contains(Loadout.BITE));
        assertFalse(BITER.with(0, null).contains(Loadout.BITE));
    }

    @Test
    void theFallbackFillsEmptySlotsTwoToSixInOrderAndSkipsWhatIsThere() {
        ResourceLocation a = ResourceLocation.parse("vampirism:a");
        ResourceLocation b = ResourceLocation.parse("vampirism:b");
        ResourceLocation c = ResourceLocation.parse("vampirism:c");
        Loadout l = BITER.with(2, b).withFallback(List.of(a, b, c));
        assertEquals(Optional.of(Loadout.BITE), l.get(0), "slot 1 is left alone");
        assertEquals(Optional.of(a), l.get(1));
        assertEquals(Optional.of(b), l.get(2), "a slot the player filled is kept");
        assertEquals(Optional.of(c), l.get(3), "an action already in the list is not repeated");
        assertEquals(Optional.empty(), l.get(4));
    }

    @Test
    void theFallbackNeverTouchesAClearedSlotOne() {
        Loadout l = BITER.with(0, null).withFallback(List.of(ResourceLocation.parse("vampirism:a")));
        assertEquals(Optional.empty(), l.get(0));
        assertEquals(Optional.of(ResourceLocation.parse("vampirism:a")), l.get(1));
    }

    @Test
    void theFallbackWithNothingUnlockedChangesNothing() {
        assertEquals(BITER, BITER.withFallback(List.of()));
    }

    @Test
    void theFallbackFillsAtMostFiveSlots() {
        List<ResourceLocation> many = java.util.stream.IntStream.range(0, 9).mapToObj(i -> ResourceLocation.parse("vampirism:a" + i)).toList();
        Loadout l = BITER.withFallback(many);
        assertEquals(Optional.of(many.get(4)), l.get(5));
    }

    @Test
    void aSlotOutOfRangeIsRefused() {
        org.junit.jupiter.api.Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Loadout.DEFAULT.get(6));
        org.junit.jupiter.api.Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Loadout.DEFAULT.with(-1, JUMP));
        assertFalse(Loadout.validSlot(6));
        assertTrue(Loadout.validSlot(5));
    }

    private static final ResourceLocation ROAR = ResourceLocation.parse("supernaturalbloodlines:alpha_roar");

    @Test
    void anOfferedActionGoesInTheFirstEmptySlotAndBiteKeepsSlotOne() {
        Loadout l = Loadout.DEFAULT.withBiteOffered().withActionOffered(ROAR);
        assertEquals(Optional.of(Loadout.BITE), l.get(0));
        assertEquals(Optional.of(ROAR), l.get(1), "the next empty slot");
        assertEquals(Optional.of(ROAR), Loadout.DEFAULT.withActionOffered(ROAR).get(0), "without Bite it takes slot 1");
    }

    @Test
    void anOfferedActionNeverOverwritesAndIsOfferedOnce() {
        Loadout full = Loadout.DEFAULT;
        for (int i = 0; i < Loadout.SLOTS; i++) {
            full = full.with(i, JUMP);
        }
        Loadout after = full.withActionOffered(ROAR);
        assertEquals(full.slots(), after.slots(), "no room: nothing is overwritten");
        assertTrue(after.offered().contains(ROAR), "and it is spent");
        assertEquals(after.with(2, null), after.with(2, null).withActionOffered(ROAR), "a spent offer is not repeated into a slot the player cleared");
    }

    @Test
    void theOfferedSetSurvivesTheSaveAndAnOldSaveWithTheBiteFlagStillWorks() {
        Loadout l = Loadout.DEFAULT.withBiteOffered().withActionOffered(ROAR).with(1, null);
        assertEquals(l, roundTrip(l));
        JsonObject tag = new JsonObject();
        com.google.gson.JsonArray slots = new com.google.gson.JsonArray();
        for (int i = 0; i < Loadout.SLOTS; i++) {
            slots.add("");
        }
        tag.add("slots", slots);
        tag.addProperty("bite_offered", false);
        Loadout old = Loadout.CODEC.parse(JsonOps.INSTANCE, tag).getOrThrow(IllegalStateException::new);
        assertFalse(old.biteOffered());
        assertEquals(Optional.of(Loadout.BITE), old.withBiteOffered().get(0));
    }
}
