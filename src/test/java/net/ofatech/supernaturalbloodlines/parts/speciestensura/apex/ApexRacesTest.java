package net.ofatech.supernaturalbloodlines.parts.speciestensura.apex;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.HumanLikeLine;
import org.junit.jupiter.api.Test;

class ApexRacesTest {
    private static JsonObject resource(String path) throws IOException {
        try (var in = ApexRacesTest.class.getResourceAsStream(path)) {
            assertNotNull(in, path + " is missing");
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static Set<String> tagValues(String name) throws IOException {
        JsonObject tag = resource("/data/tensura/tags/manascore_race/races/" + name + ".json");
        assertFalse(tag.get("replace").getAsBoolean(), name + " must merge into Tensura's tag");
        Set<String> out = new HashSet<>();
        JsonArray values = tag.getAsJsonArray("values");
        values.forEach(v -> {
            // optional entries: a tag file of ours must not break the tag when an id is not registered
            assertFalse(v.getAsJsonObject().get("required").getAsBoolean(), v + " must be optional");
            out.add(v.getAsJsonObject().get("id").getAsString());
        });
        return out;
    }

    @Test
    void everyApexRaceHasRace() {
        Set<String> ids = new HashSet<>();
        for (SupernaturalRace r : SupernaturalRace.values()) {
            boolean apex = r != SupernaturalRace.HUMAN && r != SupernaturalRace.GHOUL;
            assertEquals(apex, ApexSpec.forRace(r).isPresent(), r.name());
            ApexSpec.forRace(r).ifPresent(spec -> ids.add(spec.id()));
        }
        assertEquals(ApexSpec.ALL.size(), ids.size());
        assertEquals(ids, new HashSet<>(ApexSpec.ALL.stream().map(ApexSpec::id).toList()));
        assertEquals("primordial_vampire", ApexSpec.forRace(SupernaturalRace.ORIGINAL).orElseThrow().id());
        assertSame(ApexSpec.forRace(SupernaturalRace.SIPHONER).orElseThrow(), ApexSpec.forRace(SupernaturalRace.HERETIC).orElseThrow());
    }

    @Test
    void thePlainRacesHaveTheirOwnApex() {
        assertEquals(9, ApexSpec.ALL.size());
        assertSame(ApexSpec.ELDER_VAMPIRE, ApexSpec.forRace(SupernaturalRace.VAMPIRE).orElseThrow());
        assertSame(ApexSpec.ALPHA_PRIMEVAL, ApexSpec.forRace(SupernaturalRace.WEREWOLF).orElseThrow());
        assertEquals(900, ApexSpec.ELDER_VAMPIRE.health());
        assertEquals(List.of("tensura:self_regeneration"), ApexSpec.ELDER_VAMPIRE.intrinsicSkills());
        assertEquals(List.of("tensura:godwolf_sense"), ApexSpec.ALPHA_PRIMEVAL.intrinsicSkills());
        assertEquals(1000, ApexSpec.ALPHA_PRIMEVAL.health());
        assertEquals(25, ApexSpec.ALPHA_PRIMEVAL.attack());
        assertTrue(ApexSpec.ELDER_VAMPIRE.undead());
        assertFalse(ApexSpec.ALPHA_PRIMEVAL.undead(), "R103: werewolves can't be undead");
    }

    @Test
    void everyApexRaceHasOneSignatureSkillAndNoNextEvolution() {
        for (ApexSpec spec : ApexSpec.ALL) {
            assertEquals(1, spec.intrinsicSkills().size(), spec.id());
            assertTrue(spec.intrinsicSkills().get(0).startsWith("tensura:"), spec.id());
        }
    }

    @Test
    void langHasEveryApexRace() throws IOException {
        JsonObject lang = resource("/assets/supernaturalbloodlines/lang/en_us.json");
        for (ApexSpec spec : ApexSpec.ALL) {
            assertTrue(lang.has("supernaturalbloodlines.race." + spec.id()), spec.id() + " has no name");
            assertTrue(lang.has("supernaturalbloodlines.race." + spec.id() + ".description"), spec.id() + " has no description");
        }
    }

    @Test
    void bitableTagHoldsEveryApexRace() throws IOException {
        JsonObject tag = resource("/data/supernaturalbloodlines/tags/manascore_race/races/bitable.json");
        assertFalse(tag.get("replace").getAsBoolean());
        Set<String> ids = new HashSet<>();
        tag.getAsJsonArray("values").forEach(v -> ids.add(v.getAsJsonObject().get("id").getAsString()));
        assertEquals(new HashSet<>(ApexSpec.ALL.stream().map(s -> "supernaturalbloodlines:" + s.id()).toList()), ids);
    }

    @Test
    void epRequirementIsTwoMillion() {
        assertEquals(2_000_000, ApexRace.EP_REQUIREMENT);
    }

    @Test
    void humanLikeTagHoldsEveryApexRaceAndUndeadTagTheVampireTypes() throws IOException {
        Set<String> humanLike = tagValues("human_like");
        Set<String> undead = tagValues("undead");
        for (ApexSpec spec : ApexSpec.ALL) {
            String id = "supernaturalbloodlines:" + spec.id();
            assertTrue(humanLike.contains(id), id + " is not human_like");
            assertEquals(spec.undead(), undead.contains(id), id + " undead tag");
            assertTrue(HumanLikeLine.isEvolvedMember(id), id + " is not a human-like line member");
            assertFalse(HumanLikeLine.isSaintOrHigher(id), id + " must not be offered an apex race");
        }
        assertEquals(ApexSpec.ALL.stream().filter(ApexSpec::undead).count(), undead.size());
        assertFalse(undead.contains("supernaturalbloodlines:void_siphon"), "a siphoner is not undead");
        assertFalse(ApexSpec.VOID_SIPHON.undead());
    }
}
