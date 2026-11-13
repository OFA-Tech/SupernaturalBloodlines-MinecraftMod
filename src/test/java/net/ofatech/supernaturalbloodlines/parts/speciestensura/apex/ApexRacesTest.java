package net.ofatech.supernaturalbloodlines.parts.speciestensura.apex;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
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
    void everyApexSpeciesHasRace() {
        Set<String> ids = new HashSet<>();
        for (Species s : Species.values()) {
            boolean apexSpecies = s != Species.NONE && s != Species.POTENTIAL;
            assertEquals(apexSpecies, ApexSpec.forSpecies(s).isPresent(), s.name());
            ApexSpec.forSpecies(s).ifPresent(spec -> ids.add(spec.id()));
        }
        assertEquals(7, ids.size());
        assertEquals(ids, new HashSet<>(ApexSpec.ALL.stream().map(ApexSpec::id).toList()));
        assertEquals("primordial_vampire", ApexSpec.forSpecies(Species.ORIGINAL).orElseThrow().id());
        assertSame(ApexSpec.forSpecies(Species.SIPHONER).orElseThrow(), ApexSpec.forSpecies(Species.HERETIC).orElseThrow());
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
        assertEquals(5, undead.size());
    }
}
