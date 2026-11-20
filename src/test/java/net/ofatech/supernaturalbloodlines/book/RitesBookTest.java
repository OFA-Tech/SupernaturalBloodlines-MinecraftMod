package net.ofatech.supernaturalbloodlines.book;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Our chapter of Enchanted's book (Modopedia, {@code art_of_witchcraft}): one entry per rite, in one category, with the rite's own circle, and
 * the committed files are what {@link RitesBook} makes from the rite data and the lang lines.
 */
class RitesBookTest {
    private static final Path ROOT = projectRoot();

    /** The tests run in build/minecraft-junit: the sources are in the nearest parent that has src/main/java. */
    private static Path projectRoot() {
        Path base = Path.of("").toAbsolutePath();
        while (!Files.isDirectory(base.resolve("src/main/java"))) {
            base = base.getParent();
        }
        return base;
    }

    /** The rites that the code only lets work on a full-moon night: the rite file says so with {@code requires_full_moon}. */
    private static final Set<String> FULL_MOON_BY_CODE = Set.of("alpha_trial", "immortality", "rite_of_severance", "rite_of_the_moon", "silver_moon");

    /** The rites that need a lunar eclipse (the factory asks {@code LunarEclipse.active}). */
    private static final Set<String> ECLIPSE_BY_CODE = Set.of("rite_of_the_broken_line", "rite_of_the_imprisoned_original");

    private static JsonObject json(Path p) throws IOException {
        return JsonParser.parseString(Files.readString(p)).getAsJsonObject();
    }

    private static String all(String path) throws IOException {
        return String.join(" ", RitesBook.desc(RitesBook.lang(ROOT), path)).toLowerCase();
    }

    @Test
    void committedBookIsWhatTheGeneratorMakes() throws IOException {
        Map<String, String> made = RitesBook.generate(ROOT);
        if (System.getenv("RITES_BOOK_WRITE") != null) {
            Path dir = ROOT.resolve(RitesBook.BOOK);
            if (Files.isDirectory(dir)) {
                try (Stream<Path> old = Files.walk(dir)) {
                    for (Path p : old.sorted(java.util.Comparator.reverseOrder()).toList()) {
                        Files.delete(p);
                    }
                }
            }
            for (var e : made.entrySet()) {
                Path p = ROOT.resolve(e.getKey());
                Files.createDirectories(p.getParent());
                Files.writeString(p, e.getValue());
            }
        }
        Map<String, String> committed = RitesBook.onDisk(ROOT);
        assertEquals(made.keySet(), committed.keySet(), "the committed book files differ from the generated ones: RITES_BOOK_WRITE=1 ./gradlew test --tests '*RitesBookTest'");
        for (var e : made.entrySet()) {
            assertEquals(e.getValue(), committed.get(e.getKey()), e.getKey() + " is stale: RITES_BOOK_WRITE=1 ./gradlew test --tests '*RitesBookTest'");
        }
    }

    @Test
    void everyRiteIsInExactlyOneCategory() throws IOException {
        List<String> listed = RitesBook.CATS.stream().flatMap(c -> c.rites().stream()).toList();
        assertEquals(listed.size(), new HashSet<>(listed).size(), "a rite is in two categories");
        assertEquals(RitesBook.rites(ROOT), listed.stream().sorted().toList(), "the rite files and the categories differ");
    }

    @Test
    void everyCategoryAndEntryItNamesIsThere() throws IOException {
        Path book = ROOT.resolve(RitesBook.BOOK);
        List<String> categories = new ArrayList<>(List.of(RitesBook.TOP));
        int seen = 0;
        while (seen < categories.size()) {
            String id = categories.get(seen++);
            Path file = book.resolve("categories/" + id + ".json");
            assertTrue(Files.exists(file), "category " + id);
            JsonObject c = json(file);
            for (JsonElement child : c.getAsJsonArray("children")) {
                categories.add(child.getAsString());
            }
            for (JsonElement entry : c.getAsJsonArray("entries")) {
                assertTrue(Files.exists(book.resolve("entries/" + entry.getAsString() + ".json")), id + " lists a missing entry " + entry);
            }
        }
        assertEquals(1 + RitesBook.CATS.size(), categories.size());
    }

    @Test
    void everyEntryShowsItsOwnRite() throws IOException {
        for (String path : RitesBook.rites(ROOT)) {
            JsonObject entry = json(ROOT.resolve(RitesBook.BOOK).resolve("entries/" + RitesBook.TOP + "/" + path + ".json"));
            JsonArray pages = entry.getAsJsonArray("pages");
            JsonObject last = pages.get(pages.size() - 1).getAsJsonObject().getAsJsonArray("components").get(0).getAsJsonObject();
            assertEquals(RitesBook.NS + ":page/rite", last.get("template").getAsString(), path);
            assertEquals(RitesBook.NS + ":" + path, last.get("rite").getAsString(), path + " shows another rite's circle");
            assertTrue(entry.get("icon").getAsJsonObject().get("id").getAsString().startsWith("minecraft:"), path + ": a mod's icon would drop the entry when the mod is absent");
        }
    }

    @Test
    void onlyEnglishAndOnlyOurIds() throws IOException {
        Path books = ROOT.resolve("src/main/resources/assets/" + RitesBook.NS + "/modopedia/books/art_of_witchcraft");
        try (Stream<Path> langs = Files.list(books)) {
            assertEquals(List.of("en_us"), langs.map(p -> p.getFileName().toString()).sorted().toList(),
                    "a second language would hide Enchanted's own book (it only has en_us) for that language");
        }
        for (String file : RitesBook.onDisk(ROOT).keySet()) {
            String id = file.substring(file.indexOf("/en_us/") + 7).replaceFirst("^(categories|entries)/", "");
            assertTrue(id.startsWith(RitesBook.NS + "/"), file + " could replace one of Enchanted's own pages: ids are matched by path only");
        }
    }

    @Test
    void everyCircleIsOneEnchantedDraws() throws IOException {
        for (String path : RitesBook.rites(ROOT)) {
            JsonObject shapes = RitesBook.rite(ROOT, path).getAsJsonObject("shapes");
            assertFalse(shapes.isEmpty(), path + " has no circle");
            for (var e : shapes.entrySet()) {
                assertTrue(RitesBook.SHAPES.contains(e.getKey().replace("enchanted:", "")), path + ": " + e.getKey());
                assertTrue(RitesBook.CHALKS.containsKey(e.getValue().getAsString().replace("enchanted:", "")), path + ": " + e.getValue());
            }
        }
    }

    @Test
    void everyRiteIsDescribedAndNoLineIsLeftOver() throws IOException {
        JsonObject lang = RitesBook.lang(ROOT);
        List<String> rites = RitesBook.rites(ROOT);
        for (String path : rites) {
            List<String> lines = RitesBook.desc(lang, path);
            assertTrue(lines.size() >= 2, path + " needs what it does and who can perform it");
            lines.forEach(l -> assertTrue(l.length() <= RitesBook.PAGE_CHARS, path + ": a line is too long for a book page"));
        }
        for (String key : lang.keySet()) {
            if (key.startsWith(RitesBook.DESC)) {
                String rest = key.substring(RitesBook.DESC.length());
                String path = rest.substring(0, rest.lastIndexOf('.'));
                assertTrue(rites.contains(path), key + " describes a rite that is not there");
                int n = Integer.parseInt(rest.substring(rest.lastIndexOf('.') + 1));
                assertTrue(n == 1 || lang.has(RitesBook.DESC + path + "." + (n - 1)), key + " leaves a gap, so the lines after it are never shown");
            }
        }
    }

    @Test
    void theFullMoonIsInTheRiteFileAndInTheText() throws IOException {
        for (String path : RitesBook.rites(ROOT)) {
            JsonObject factory = RitesBook.rite(ROOT, path).getAsJsonObject("factory");
            boolean flag = factory.has("requires_full_moon") && factory.get("requires_full_moon").getAsBoolean();
            assertEquals(FULL_MOON_BY_CODE.contains(path), flag, path + ": the rite file and the code disagree about the full moon");
            assertEquals(flag, all(path).contains("full-moon night only"), path + ": the text and the rite file disagree about the full moon");
        }
    }

    @Test
    void theMoonRiteTextDoesNotTellTheCasterWhoCarriesTheGene() throws IOException {
        String moon = String.join(" ", RitesBook.desc(RitesBook.lang(ROOT), "rite_of_the_moon").subList(1, 3)).toLowerCase();
        assertFalse(moon.contains("gene"), "a refusal of this rite tells the caster nothing about the gene, so the text must not either");
        assertTrue(moon.contains("would change nothing"));
    }

    @Test
    void theEclipseIsInTheText() throws IOException {
        for (String path : RitesBook.rites(ROOT)) {
            assertEquals(ECLIPSE_BY_CODE.contains(path), all(path).contains("lunar eclipse"), path);
        }
    }

    @Test
    void aSacrificeAndAFreeRiteAreSaid() throws IOException {
        for (String path : RitesBook.rites(ROOT)) {
            JsonObject rite = RitesBook.rite(ROOT, path);
            assertEquals(rite.has("entities") && !rite.getAsJsonArray("entities").isEmpty(), all(path).contains("sacrifice"), path + ": the sacrifice");
            if (rite.get("power").getAsInt() == 0) {
                assertTrue(all(path).contains("no altar power"), path + " costs no altar power and must say what it costs instead");
            }
        }
    }

    @Test
    void noTextPageIsLongerThanABookPage() throws IOException {
        for (var e : RitesBook.onDisk(ROOT).entrySet()) {
            if (!e.getKey().contains("/entries/")) {
                continue;
            }
            for (JsonElement page : JsonParser.parseString(e.getValue()).getAsJsonObject().getAsJsonArray("pages")) {
                for (JsonElement c : page.getAsJsonObject().getAsJsonArray("components")) {
                    if (c.getAsJsonObject().has("text")) {
                        String shown = c.getAsJsonObject().get("text").getAsString().replaceAll("\\$\\([^)]*\\)", "");
                        assertTrue(shown.length() <= RitesBook.PAGE_CHARS, e.getKey() + ": " + shown.length() + " characters");
                    }
                }
            }
        }
    }

    @Test
    void anEntryHasTheCircleLineFromItsRiteFile() throws IOException {
        JsonObject curse = RitesBook.rite(ROOT, "curse_skill_seal");
        assertEquals("Circle: medium circle, infernal chalk.", RitesBook.circleLine(curse));
        assertEquals("Circle: large circle, ritual chalk.", RitesBook.circleLine(RitesBook.rite(ROOT, "rite_of_the_imprisoned_original")));
    }

    @Test
    void partsFillPagesInOrderAndOneThatCannotFitIsAnError() {
        String a = "a".repeat(130);
        String b = "b".repeat(100);
        String c = "c".repeat(60);
        assertEquals(List.of(a + "\n\n" + c, b), RitesBook.textPages("x", List.of(a, c, b)));
        assertEquals(List.of(a, b + "\n\n" + c), RitesBook.textPages("x", List.of(a, b, c)));
        boolean refused = false;
        try {
            RitesBook.textPages("x", List.of("z".repeat(RitesBook.PAGE_CHARS + 1)));
        } catch (IllegalStateException e) {
            refused = true;
        }
        assertTrue(refused);
    }
}
