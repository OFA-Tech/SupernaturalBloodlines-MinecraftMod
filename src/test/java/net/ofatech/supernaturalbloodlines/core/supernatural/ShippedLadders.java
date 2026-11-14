package net.ofatech.supernaturalbloodlines.core.supernatural;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/** The ladder JSON that ships in the jar, for tests. */
final class ShippedLadders {
    private static final String DIR = "/data/supernaturalbloodlines/supernatural/ladder/";

    private ShippedLadders() {
    }

    static Map<String, JsonElement> files() {
        Map<String, JsonElement> out = new HashMap<>();
        for (SupernaturalRace race : SupernaturalRace.values()) {
            try (InputStream in = ShippedLadders.class.getResourceAsStream(DIR + race.id() + ".json")) {
                if (in == null) {
                    throw new IllegalStateException("no ladder file for " + race.id());
                }
                out.put(race.id(), JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return out;
    }

    static LadderTable load(Predicate<String> modLoaded) {
        LadderTable.Parsed parsed = LadderTable.parse(files(), modLoaded);
        if (!parsed.rejected().isEmpty()) {
            throw new IllegalStateException("shipped ladders rejected: " + parsed.rejected());
        }
        return parsed.table();
    }

    static LadderTable all() {
        return load(mod -> true);
    }
}
