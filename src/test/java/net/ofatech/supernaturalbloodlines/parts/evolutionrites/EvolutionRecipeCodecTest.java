package net.ofatech.supernaturalbloodlines.parts.evolutionrites;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Set;
import net.ofatech.supernaturalbloodlines.core.evolution.EvolutionRecipe;
import net.ofatech.supernaturalbloodlines.core.evolution.GrantForm;
import net.ofatech.supernaturalbloodlines.core.evolution.Tier;
import net.ofatech.supernaturalbloodlines.core.rite.RiteKind;
import net.ofatech.supernaturalbloodlines.core.evolution.EvolutionRecipeCodec;
import org.junit.jupiter.api.Test;

class EvolutionRecipeCodecTest {
    private static com.mojang.serialization.DataResult<EvolutionRecipe> decode(String json) {
        return EvolutionRecipeCodec.RECIPE.codec().parse(JsonOps.INSTANCE, JsonParser.parseString(json));
    }

    private static final String FULL = """
            { "recipe": "x", "tier": "unique", "grants": "tensura:great_sage", "grant_form": "default", "consumes": ["tensura:sage"],
              "needs": [ { "skill": "tensura:sage", "mastered": true }, { "skill": "tensura:degenerate" } ],
              "requires_mod": "tensura", "probe": "own.sage_great_sage", "subject_kinds": ["vampire", "WITCH"], "ep_cost": 5.5 }
            """;

    @Test
    void aFullBlockDecodes() {
        var r = decode(FULL).getOrThrow();
        assertEquals("x", r.id());
        assertEquals(Tier.UNIQUE, r.tier());
        assertEquals(GrantForm.DEFAULT, r.grantForm());
        assertEquals(List.of("tensura:sage"), r.consumes());
        assertEquals(2, r.needs().size());
        assertTrue(r.needs().get(0).mastered());
        assertEquals(false, r.needs().get(1).mastered());
        assertEquals(Set.of(RiteKind.VAMPIRE, RiteKind.WITCH), r.subjectKinds());
        assertEquals(5.5, r.epCost());
    }

    @Test
    void theOptionalFieldsDefault() {
        var r = decode("{ \"recipe\": \"x\", \"tier\": \"extra\", \"grants\": \"a:b\", \"requires_mod\": \"tensura\", \"probe\": \"own.sage_great_sage\" }").getOrThrow();
        assertEquals(GrantForm.DEFAULT, r.grantForm());
        assertTrue(r.consumes().isEmpty() && r.needs().isEmpty() && r.subjectKinds().isEmpty());
        assertEquals(0.0, r.epCost());
    }

    @Test
    void aMissingFieldIsAnError() {
        assertTrue(decode("{ \"tier\": \"unique\", \"grants\": \"a:b\", \"requires_mod\": \"tensura\", \"probe\": \"p\" }").error().isPresent(), "no recipe id");
        assertTrue(decode("{ \"recipe\": \"x\", \"tier\": \"unique\", \"requires_mod\": \"tensura\", \"probe\": \"p\" }").error().isPresent(), "no grants");
    }

    @Test
    void anUnknownTierKindOrFormIsAnErrorAndCommonIsNotAnEvolutionTier() {
        String base = "\"recipe\": \"x\", \"grants\": \"a:b\", \"requires_mod\": \"tensura\", \"probe\": \"p\"";
        assertTrue(decode("{ " + base + ", \"tier\": \"mythic\" }").error().isPresent());
        assertTrue(decode("{ " + base + ", \"tier\": \"common\" }").error().isPresent(), "a Common skill is only for the Rite of Mastery");
        assertTrue(decode("{ " + base + ", \"tier\": \"unique\", \"subject_kinds\": [\"dragon\"] }").error().isPresent());
        assertTrue(decode("{ " + base + ", \"tier\": \"unique\", \"grant_form\": \"free\" }").error().isEmpty(), "FREE arrived with 19b");
        assertTrue(decode("{ " + base + ", \"tier\": \"unique\", \"grant_form\": \"permanent\" }").error().isPresent(), "PERMANENT arrives with 19c");
    }

    @Test
    void theMasteryBlockTakesAKnownTier() {
        for (Tier t : Tier.values()) {
            var r = EvolutionRecipeCodec.MASTERY_TIER.codec().parse(JsonOps.INSTANCE, JsonParser.parseString("{ \"tier\": \"" + t.id() + "\" }"));
            assertEquals(t, r.getOrThrow());
        }
        assertTrue(EvolutionRecipeCodec.MASTERY_TIER.codec().parse(JsonOps.INSTANCE, JsonParser.parseString("{ \"tier\": \"mythic\" }")).error().isPresent());
    }
}
