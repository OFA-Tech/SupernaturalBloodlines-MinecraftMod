package net.ofatech.supernaturalbloodlines.parts.artifacts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** The item art rework swapped the compass for a loader model; it must still be 32 needle frames chosen by the {@code angle} predicate. */
class CompassModelTest {
    private static final String ASSETS = "/assets/supernaturalbloodlines/";

    private static JsonObject json(String path) throws Exception {
        var in = CompassModelTest.class.getResourceAsStream(ASSETS + path);
        assertNotNull(in, "missing " + path);
        try (var r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(r).getAsJsonObject();
        }
    }

    @Test
    void thirtyTwoFramesChosenByAnIncreasingAngleThatWrapsBackToTheFirst() throws Exception {
        JsonArray overrides = json("models/item/vampire_compass.json").getAsJsonArray("overrides");
        assertEquals(33, overrides.size(), "32 frames plus the wrap-around entry");
        double last = -1;
        for (int i = 0; i < overrides.size(); i++) {
            double angle = overrides.get(i).getAsJsonObject().getAsJsonObject("predicate").get("angle").getAsDouble();
            assertTrue(angle > last, "angle must increase at override " + i);
            last = angle;
        }
        String model = overrides.get(32).getAsJsonObject().get("model").getAsString();
        assertEquals("supernaturalbloodlines:item/vampire_compass_00", model, "the last angle wraps to frame 0");
    }

    @Test
    void everyFrameModelAndItsSpriteShip() throws Exception {
        JsonArray overrides = json("models/item/vampire_compass.json").getAsJsonArray("overrides");
        for (int i = 0; i < overrides.size(); i++) {
            String id = overrides.get(i).getAsJsonObject().get("model").getAsString().substring("supernaturalbloodlines:".length());
            JsonObject frame = json("models/" + id + ".json");
            String sprite = frame.getAsJsonObject("perspectives").getAsJsonObject("gui").getAsJsonObject("textures").get("layer0").getAsString();
            assertNotNull(CompassModelTest.class.getResource(ASSETS + "textures/" + sprite.substring("supernaturalbloodlines:".length()) + ".png"),
                    "frame " + i + " has no sprite " + sprite);
        }
    }
}
