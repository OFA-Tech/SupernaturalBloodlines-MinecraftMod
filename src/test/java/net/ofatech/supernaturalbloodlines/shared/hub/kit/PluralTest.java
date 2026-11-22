package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class PluralTest {
    @Test
    void exactlyOneTakesTheSingularKey() {
        assertEquals("a.one", Plural.key("a", 1));
        assertEquals("a", Plural.key("a", 0));
        assertEquals("a", Plural.key("a", 2));
        assertEquals("a", Plural.key("a", 11));
        assertEquals("a", Plural.key("a", -1));
    }

    @Test
    void everyCountedSkillPointLineHasItsSingularTwinAndItReadsSingular() throws Exception {
        try (InputStream in = PluralTest.class.getResourceAsStream("/assets/supernaturalbloodlines/lang/en_us.json")) {
            assertNotNull(in);
            JsonObject lang = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (String base : Plural.SKILL_POINT_LINES) {
                String many = lang.get(base).getAsString();
                String one = lang.get(base + ".one").getAsString();
                assertEquals(many.replace("skill points", "skill point"), one, base);
            }
        }
    }
}