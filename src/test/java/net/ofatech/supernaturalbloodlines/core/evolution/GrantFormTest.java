package net.ofatech.supernaturalbloodlines.core.evolution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GrantFormTest {
    @Test
    void freeAndDefaultParseAndPermanentIsNotYet() {
        assertEquals(Optional.of(GrantForm.FREE), GrantForm.parse("free"));
        assertEquals(Optional.of(GrantForm.DEFAULT), GrantForm.parse("default"));
        assertTrue(GrantForm.parse("permanent").isEmpty(), "permanent is 19c");
        assertTrue(GrantForm.parse("").isEmpty());
    }

    @Test
    void aRecipeFileWithTheFreeFormDecodesToIt() {
        JsonObject factory = new JsonObject();
        factory.addProperty("recipe", "x");
        factory.addProperty("tier", "ultimate");
        factory.addProperty("grants", "tensura:great_sage");
        factory.addProperty("grant_form", "free");
        factory.addProperty("requires_mod", "tensura");
        factory.addProperty("probe", "own.sage_great_sage");
        EvolutionRecipe recipe = EvolutionRecipeCodec.RECIPE.codec().parse(JsonOps.INSTANCE, factory).getOrThrow();
        assertEquals(GrantForm.FREE, recipe.grantForm());
    }

    @Test
    void aRecipeWithoutAFormIsTheDefaultOne() {
        JsonObject factory = new JsonObject();
        factory.addProperty("recipe", "x");
        factory.addProperty("tier", "unique");
        factory.addProperty("grants", "tensura:great_sage");
        factory.addProperty("requires_mod", "tensura");
        factory.addProperty("probe", "own.sage_great_sage");
        assertEquals(GrantForm.DEFAULT, EvolutionRecipeCodec.RECIPE.codec().parse(JsonOps.INSTANCE, factory).getOrThrow().grantForm());
    }

    @Test
    void anUnknownFormIsAnError() {
        JsonObject factory = new JsonObject();
        factory.addProperty("recipe", "x");
        factory.addProperty("tier", "unique");
        factory.addProperty("grants", "tensura:great_sage");
        factory.addProperty("grant_form", "forever");
        factory.addProperty("requires_mod", "tensura");
        factory.addProperty("probe", "own.sage_great_sage");
        assertTrue(EvolutionRecipeCodec.RECIPE.codec().parse(JsonOps.INSTANCE, factory).error().isPresent());
    }
}
