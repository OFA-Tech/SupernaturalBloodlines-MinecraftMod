package net.ofatech.supernaturalbloodlines.core.evolution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class GrantFormTest {
    @Test
    void everyFormParses() {
        assertEquals(Optional.of(GrantForm.FREE), GrantForm.parse("free"));
        assertEquals(Optional.of(GrantForm.DEFAULT), GrantForm.parse("default"));
        assertEquals(Optional.of(GrantForm.PERMANENT), GrantForm.parse("permanent"));
        assertTrue(GrantForm.parse("").isEmpty());
    }

    @Test
    void onlyPermanentNamesARemoveTime() {
        assertEquals(OptionalInt.of(-1), GrantForm.PERMANENT.removeTime(), "-1 is Tensura's permanent skill, as Ascension's altar learns it");
        assertTrue(GrantForm.DEFAULT.removeTime().isEmpty(), "the default form is Tensura's own two-argument learn");
        assertTrue(GrantForm.FREE.removeTime().isEmpty(), "the free form learns through its own gifted instance");
    }

    @Test
    void everyFormRoundTripsThroughItsName() {
        for (GrantForm f : GrantForm.values()) {
            assertEquals(f, GrantForm.parse(f.name().toLowerCase(Locale.ROOT)).orElseThrow());
        }
        assertTrue(GrantForm.parse("temporary").isEmpty());
    }

    private static JsonObject factory(String tier) {
        JsonObject factory = new JsonObject();
        factory.addProperty("recipe", "x");
        factory.addProperty("tier", tier);
        factory.addProperty("grants", "tensura:great_sage");
        factory.addProperty("requires_mod", "tensura");
        factory.addProperty("probe", "own.sage_great_sage");
        return factory;
    }

    @Test
    void aRecipeFileWithTheFreeFormDecodesToIt() {
        JsonObject factory = factory("ultimate");
        factory.addProperty("grant_form", "free");
        assertEquals(GrantForm.FREE, EvolutionRecipeCodec.RECIPE.codec().parse(JsonOps.INSTANCE, factory).getOrThrow().grantForm());
    }

    @Test
    void aRecipeWithoutAFormIsTheDefaultOne() {
        assertEquals(GrantForm.DEFAULT, EvolutionRecipeCodec.RECIPE.codec().parse(JsonOps.INSTANCE, factory("unique")).getOrThrow().grantForm());
    }

    @Test
    void anUnknownFormIsAnError() {
        JsonObject factory = factory("unique");
        factory.addProperty("grant_form", "forever");
        assertTrue(EvolutionRecipeCodec.RECIPE.codec().parse(JsonOps.INSTANCE, factory).error().isPresent());
    }
}
