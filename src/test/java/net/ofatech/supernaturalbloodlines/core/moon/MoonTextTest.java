package net.ofatech.supernaturalbloodlines.core.moon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import net.ofatech.supernaturalbloodlines.SourceTree;
import org.junit.jupiter.api.Test;

/**
 * R358: what a full moon, a lunar eclipse and a Moonstone are is said the same way wherever a player reads about a rite (its description in the hub and
 * the book, and its refusal), and what it says is what the code does ({@link MoonRule}).
 */
class MoonTextTest {
    static final String MOON_LINE = "Needs a full moon: the moon must be full, between dusk and dawn, in the Overworld. An eclipse counts too. "
            + "The hub's moon calendar shows when the next one rises.";
    static final String ECLIPSE_LINE = "An eclipse happens on some full-moon nights (the server's chance). It lasts until dawn, and an eclipse also counts "
            + "as a full moon for every rite that needs one.";
    static final String WEREWOLVES_LINE = "This rite needs the Werewolves mod.";
    private static final String DESC = "supernaturalbloodlines.hub.rites.desc.";
    private static final Path RITES = SourceTree.root().resolve("src/main/resources/data/supernaturalbloodlines/enchanted/circle_magic/rite");
    private static final Path MOONSTONE = SourceTree.root().resolve("src/main/resources/data/supernaturalbloodlines/recipe/moonstone.json");
    /** The rites whose code reads the lunar eclipse. */
    private static final Set<String> ECLIPSE_RITES = Set.of("rite_of_the_broken_line", "rite_of_the_imprisoned_original", "mastery_ultimate", "evolve_blood_sovereign", "evolve_raphael");
    /** The refusals that tell a player the night is wrong: a short line on the action bar, the whole of it in chat ({@code .detail}). */
    private static final List<String> MOON_REFUSALS = List.copyOf(net.ofatech.supernaturalbloodlines.shared.rite.RiteTell.FULL_MOON);
    private static final List<String> ECLIPSE_REFUSALS = List.copyOf(net.ofatech.supernaturalbloodlines.shared.rite.RiteTell.ECLIPSE);

    private static JsonObject lang() throws IOException {
        return JsonParser.parseString(SourceTree.read("src/main/resources/assets/supernaturalbloodlines/lang/en_us.json")).getAsJsonObject();
    }

    private static List<String> rites() throws IOException {
        try (Stream<Path> files = Files.list(RITES)) {
            return files.map(p -> p.getFileName().toString().replace(".json", "")).sorted().toList();
        }
    }

    private static JsonObject rite(String path) throws IOException {
        return JsonParser.parseString(Files.readString(RITES.resolve(path + ".json"))).getAsJsonObject();
    }

    private static List<String> desc(JsonObject lang, String path) {
        List<String> out = new ArrayList<>();
        for (int i = 1; lang.has(DESC + path + "." + i); i++) {
            out.add(lang.get(DESC + path + "." + i).getAsString());
        }
        return out;
    }

    /** The file says so, or the rite is an evolution or mastery rite below Ultimate ({@code Tier.night()}: Ultimate wants the eclipse). */
    private static boolean needsFullMoon(JsonObject rite) {
        JsonObject f = rite.getAsJsonObject("factory");
        if (f.has("requires_full_moon") && f.get("requires_full_moon").getAsBoolean()) {
            return true;
        }
        String type = f.get("type").getAsString();
        return (type.endsWith(":evolution") || type.endsWith(":mastery")) && f.has("tier") && !f.get("tier").getAsString().equals("ultimate");
    }

    private static boolean needsWerewolves(JsonObject rite) {
        if (!rite.has("neoforge:conditions")) {
            return false;
        }
        for (JsonElement c : rite.getAsJsonArray("neoforge:conditions")) {
            JsonObject o = c.getAsJsonObject();
            if (o.has("modid") && o.get("modid").getAsString().equals("werewolves")) {
                return true;
            }
        }
        return false;
    }

    private static boolean takesMoonstone(JsonObject rite) {
        for (JsonElement i : rite.getAsJsonArray("items")) {
            if (i.getAsJsonObject().get("id").getAsString().equals("supernaturalbloodlines:moonstone")) {
                return true;
            }
        }
        return false;
    }

    /** "an amethyst shard and a silver ingot", from the recipe file itself. */
    static String moonstoneLine() throws IOException {
        JsonObject recipe = JsonParser.parseString(Files.readString(MOONSTONE)).getAsJsonObject();
        List<String> parts = new ArrayList<>();
        for (JsonElement e : recipe.getAsJsonArray("ingredients")) {
            String id = e.getAsJsonObject().get("item").getAsString();
            String name = id.substring(id.indexOf(':') + 1).replace('_', ' ');
            String from = id.startsWith("minecraft:") ? "" : " (from " + Character.toUpperCase(id.charAt(0)) + id.substring(1, id.indexOf(':')) + ")";
            parts.add(("aeiou".indexOf(name.charAt(0)) >= 0 ? "an " : "a ") + name + from);
        }
        return "Moonstone: craft it from " + String.join(" and ", parts) + ", in any order.";
    }

    @Test
    void theNumbersInTheLangAreMoonRules() throws IOException {
        assertEquals(12786, MoonRule.NIGHT_START, "the texts say dusk; the code's dusk moved: say where");
        assertEquals(23216, MoonRule.NIGHT_END, "the texts say dawn; the code's dawn moved: say where");
        assertTrue(MoonRule.isFullMoon(false, 0) && !MoonRule.isFullMoon(true, 0), "the texts say phase 0 and the Overworld");
        JsonObject lang = lang();
        for (String key : MOON_REFUSALS) {
            String text = lang.get(key + ".detail").getAsString();
            for (String must : List.of("full moon", "dusk", "dawn", "Overworld", "eclipse", "moon calendar")) {
                assertTrue(text.contains(must), key + ".detail does not say " + must + ": " + text);
            }
            assertTrue(lang.get(key).getAsString().endsWith("(see chat)."), key + ": the action bar line is the short one");
        }
        for (String key : ECLIPSE_REFUSALS) {
            assertTrue(lang.get(key + ".detail").getAsString().endsWith(ECLIPSE_LINE), key + ".detail does not say how an eclipse happens");
            assertTrue(lang.get(key).getAsString().endsWith("(see chat)."), key + ": the action bar line is the short one");
        }
        assertTrue(lang.get("supernaturalbloodlines.rite.full_moon_in").getAsString().contains("%s"), "the days to the next full moon");
        for (String k : lang.keySet()) {
            assertTrue(!lang.get(k).getAsString().contains("phase 0"), k + " says phase 0: say \"when the moon is full\"");
        }
    }

    @Test
    void everyMoonRiteSaysItAndPointsToTheMoonCalendar() throws IOException {
        JsonObject lang = lang();
        int moon = 0;
        for (String path : rites()) {
            boolean flag = needsFullMoon(rite(path));
            moon += flag ? 1 : 0;
            assertEquals(flag, desc(lang, path).contains(MOON_LINE), path + ": the rite file and the moon line disagree");
        }
        assertEquals(16, moon, "the rites that need a full moon: seven by their file, nine evolution and mastery rites by their tier");
    }

    @Test
    void everyEclipseRiteSaysHowEclipsesHappen() throws IOException {
        JsonObject lang = lang();
        for (String path : rites()) {
            assertEquals(ECLIPSE_RITES.contains(path), desc(lang, path).contains(ECLIPSE_LINE), path);
        }
    }

    @Test
    void everyMoonstoneRiteStatesTheRecipe() throws IOException {
        JsonObject lang = lang();
        String line = moonstoneLine();
        int seen = 0;
        for (String path : rites()) {
            boolean takes = takesMoonstone(rite(path));
            seen += takes ? 1 : 0;
            assertEquals(takes, desc(lang, path).contains(line), path + ": the Moonstone line, which must be \"" + line + "\"");
        }
        assertEquals(4, seen, "the rites that take a Moonstone");
    }

    @Test
    void werewolvesRequiredIsSaidWhereTrue() throws IOException {
        JsonObject lang = lang();
        for (String path : rites()) {
            List<String> lines = desc(lang, path);
            assertEquals(needsWerewolves(rite(path)), lines.contains(WEREWOLVES_LINE), path + ": the rite file and the Werewolves line disagree");
            assertTrue(lines.stream().noneMatch(l -> !l.equals(WEREWOLVES_LINE) && l.contains("Werewolves must be installed")), path + " says it the old way too");
        }
    }

    @Test
    void noTextSaysFullMoonWithoutTheWindow() throws IOException {
        JsonObject lang = lang();
        for (String path : rites()) {
            List<String> lines = desc(lang, path);
            // a rite that asks for a full-moon night (not one that only names the full-moon shift, like the Ward of the Moon)
            boolean mentions = lines.stream().anyMatch(l -> !l.equals(ECLIPSE_LINE) && l.toLowerCase().matches(".*full[- ]moon night.*"));
            if (mentions) {
                assertTrue(lines.contains(MOON_LINE), path + " talks of the full moon without saying what one is");
            }
        }
    }

    @Test
    void theMoonstoneLineIsReadFromTheRecipe() throws IOException {
        assertEquals("Moonstone: craft it from an amethyst shard and a silver ingot (from Werewolves), in any order.", moonstoneLine());
    }
}
