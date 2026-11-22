package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class TitleFontTest {
    private static JsonObject bitmap(String file) throws Exception {
        try (InputStream in = TitleFontTest.class.getResourceAsStream("/assets/supernaturalbloodlines/font/" + file)) {
            assertNotNull(in, file);
            JsonArray providers = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("providers");
            for (var p : providers) {
                if (p.getAsJsonObject().get("type").getAsString().equals("bitmap")) {
                    return p.getAsJsonObject();
                }
            }
            throw new AssertionError("no bitmap in " + file);
        }
    }

    @Test
    void theGlyphListIsTheBitmapOfBothFonts() throws Exception {
        for (String file : new String[] {"title.json", "title_gothic.json"}) {
            JsonObject b = bitmap(file);
            StringBuilder chars = new StringBuilder();
            b.getAsJsonArray("chars").forEach(row -> chars.append(row.getAsString()));
            for (char c : chars.toString().toCharArray()) {
                if (c != 0) {
                    assertTrue(TitleFont.CHARS.indexOf(c) >= 0, file + " has " + (int) c + " that CHARS lacks");
                }
            }
            for (char c : TitleFont.CHARS.toCharArray()) {
                assertTrue(c == ' ' || chars.indexOf(String.valueOf(c)) >= 0, file + " lacks " + (int) c);
            }
            assertEquals(TitleFont.HEIGHT, b.get("height").getAsInt());
        }
    }

    @Test
    void aTextWithAnyOtherCharacterIsLeftToTheNormalFont() {
        assertTrue(TitleFont.supports("Path of the Moon"));
        assertTrue(TitleFont.supports("Chroniques d\u00e9sol\u00e9es"));
        assertFalse(TitleFont.supports("\u7389\u5ea7"));
        assertFalse(TitleFont.supports("Steve\u00a7"));
        assertFalse(TitleFont.supports(""));
    }
}
