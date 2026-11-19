package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.ofatech.supernaturalbloodlines.core.moon.MoonCalendar;
import org.junit.jupiter.api.Test;

/** "Narration for every target" for the moon calendar: every line it draws, every tooltip and every night it can name has words. */
class MoonLangTest {
    private static JsonObject lang() throws Exception {
        try (var in = new InputStreamReader(MoonLangTest.class.getResourceAsStream("/assets/supernaturalbloodlines/lang/en_us.json"), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(in).getAsJsonObject();
        }
    }

    @Test
    void everyKeyOfTheMoonCalendarHasWords() throws Exception {
        JsonObject lang = lang();
        List<String> keys = new ArrayList<>(List.of("title", "info", "now", "legend", "day_today", "day_tomorrow", "day_in", "tonight", "no_sleep", "stirs",
                "full_moons", "full_today", "full_in", "events"));
        for (int phase = 0; phase < 8; phase++) {
            keys.add("phase." + phase);
        }
        for (MoonCalendar.Night n : MoonCalendar.Night.values()) {
            keys.add("night." + n.name().toLowerCase(Locale.ROOT));
        }
        List<String> missing = new ArrayList<>();
        for (String k : keys) {
            if (!lang.has(MoonPage.KEY + k)) {
                missing.add(MoonPage.KEY + k);
            }
        }
        for (String k : List.of("supernaturalbloodlines.hub.home.open_moon", "supernaturalbloodlines.hub.path.calendar_open", "supernaturalbloodlines.moon.no_sleep",
                "supernaturalbloodlines.awaken.stirs", "supernaturalbloodlines.awaken.stirs_later")) {
            if (!lang.has(k)) {
                missing.add(k);
            }
        }
        assertTrue(missing.isEmpty(), "no words for: " + missing);
    }
}
