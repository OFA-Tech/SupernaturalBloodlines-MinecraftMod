package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class SettingsNotesTest {
    @Test
    void reduceMotionIsAShortLabelWithItsOwnMutedLine() throws Exception {
        try (InputStream in = SettingsNotesTest.class.getResourceAsStream("/assets/supernaturalbloodlines/lang/en_us.json")) {
            JsonObject lang = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals("Reduce motion", lang.get("supernaturalbloodlines.hub.settings.key.reduce_motion").getAsString());
            assertEquals("No slides, pulses or flourishes.", lang.get("supernaturalbloodlines.hub.settings.note.reduce_motion").getAsString());
        }
    }
}