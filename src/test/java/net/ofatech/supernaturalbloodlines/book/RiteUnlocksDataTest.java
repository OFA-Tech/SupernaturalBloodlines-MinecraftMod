package net.ofatech.supernaturalbloodlines.book;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.rite.RiteAdvancements;
import net.ofatech.supernaturalbloodlines.core.rite.RiteKinds;
import net.ofatech.supernaturalbloodlines.core.rite.RiteUnlockTable;
import org.junit.jupiter.api.Test;

/** The unlock table against the data it governs: the rite files, the ladders, the lang and the book (plan rites-unlock). */
class RiteUnlocksDataTest {
    private static final Path ROOT = root();

    private static Path root() {
        Path base = Path.of("").toAbsolutePath();
        while (!Files.isDirectory(base.resolve("src/main/java"))) {
            base = base.getParent();
        }
        return base;
    }

    @Test
    void everyRiteFileIsOpenOrGatedExactlyOnce() throws IOException {
        Set<String> table = new HashSet<>(RiteUnlockTable.OPEN);
        table.addAll(RiteUnlockTable.GATED.keySet());
        assertEquals(RiteUnlockTable.OPEN.size() + RiteUnlockTable.GATED.size(), table.size(), "a rite is both open and gated");
        assertEquals(RitesBook.rites(ROOT).stream().sorted().toList(), table.stream().sorted().toList(), "the rite files and RiteUnlockTable differ");
    }

    /** The races whose ladders ask for a particular rite to be performed, and what that race is: every other race must ask for none. */
    private static final Map<String, Species> PERFORMERS = Map.of("witch", Species.WITCH, "siphoner", Species.SIPHONER, "tribrid", Species.TRIBRID);

    private static void performed(JsonElement node, List<String> out) {
        if (node.isJsonArray()) {
            node.getAsJsonArray().forEach(e -> performed(e, out));
        } else if (node.isJsonObject()) {
            JsonObject o = node.getAsJsonObject();
            if (o.has("detector") && o.get("detector").getAsString().equals("rite") && o.has("role") && o.get("role").getAsString().equals("performer")
                    && !o.get("rite").getAsString().equals("any")) {
                String id = o.get("rite").getAsString();
                out.add(id.substring(id.indexOf(':') + 1));
            }
            o.entrySet().forEach(e -> performed(e.getValue(), out));
        }
    }

    private static List<String> ladderPerformed(String race) throws IOException {
        try (InputStream in = RiteUnlocksDataTest.class.getResourceAsStream("/data/supernaturalbloodlines/supernatural/ladder/" + race + ".json")) {
            List<String> out = new ArrayList<>();
            performed(JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)), out);
            return out;
        }
    }

    /** A ladder task that asks a race to perform a rite is a quest nobody could finish if the race did not know the rite. */
    @Test
    void everyRiteALadderAsksToPerformIsKnownToThatRace() throws IOException {
        for (var race : PERFORMERS.entrySet()) {
            var kinds = RiteKinds.of(race.getValue(), false, false, false);
            List<String> asked = ladderPerformed(race.getKey());
            assertFalse(asked.isEmpty(), race.getKey() + " asks for no particular rite: drop it from PERFORMERS");
            for (String rite : asked) {
                assertTrue(RiteUnlockTable.knows(rite, kinds, Set.of()), race.getKey() + " is asked to perform " + rite + " but never learns it");
            }
        }
    }

    @Test
    void noOtherLadderAsksForAParticularRiteToBePerformed() throws IOException {
        try (var files = Files.list(ROOT.resolve("src/main/resources/data/supernaturalbloodlines/supernatural/ladder"))) {
            for (Path p : files.toList()) {
                String race = p.getFileName().toString().replace(".json", "");
                if (!PERFORMERS.containsKey(race)) {
                    assertEquals(List.of(), ladderPerformed(race), race + " asks for a particular rite: add it to PERFORMERS so the rule covers it");
                }
            }
        }
    }

    @Test
    void theHintsAreBuiltFromTheLangAndNameNoRite() throws IOException {
        JsonObject lang = RitesBook.lang(ROOT);
        assertTrue(lang.has(RitesBook.SEALED_TEXT));
        for (String rite : RiteUnlockTable.GATED.keySet()) {
            String hint = RitesBook.hint(lang, rite).toLowerCase();
            String label = RitesBook.label(lang, rite).toLowerCase();
            for (String other : RiteUnlockTable.GATED.keySet()) {
                String name = lang.get("rite." + RitesBook.NS + "." + other).getAsString().toLowerCase();
                assertFalse(hint.contains(name) || label.contains(name), rite + ": the hint or the label gives away " + other);
            }
            assertTrue(label.startsWith("??? \u00b7 "), rite + ": " + label);
            // the list line is one line of the book: a label that is longer than this wraps or is cut
            assertTrue(label.length() <= RitesBook.LABEL_CHARS, rite + ": the label is " + label.length() + " characters: " + label);
        }
    }

    @Test
    void aKindHintSaysBecomingThatKind() throws IOException {
        JsonObject lang = RitesBook.lang(ROOT);
        assertEquals("Unlocked by becoming a werewolf.", RitesBook.hint(lang, "silver_moon"));
        assertEquals("Unlocked by becoming an Original.", RitesBook.hint(lang, "original_blood"));
        assertEquals("Unlocked by becoming a vampire or a witch.", RitesBook.hint(lang, "ward_of_daylight"));
    }

    @Test
    void aRiteWithTwoRoutesSaysBoth() throws IOException {
        JsonObject lang = RitesBook.lang(ROOT);
        String cure = RitesBook.hint(lang, "rite_of_the_cure");
        assertTrue(cure.contains("becoming a witch") && cure.contains("Elijah's Letter"), cure);
        String severance = RitesBook.hint(lang, "rite_of_severance");
        assertTrue(severance.contains("a siphoner") && severance.contains("a tribrid") && severance.contains("Esther's Grimoire"), severance);
        String label = RitesBook.label(lang, "rite_of_the_cure");
        assertTrue(label.contains("witch") && label.contains("Elijah"), label);
    }

    @Test
    void aLootHintNamesTheItemAndThePlaces() throws IOException {
        JsonObject lang = RitesBook.lang(ROOT);
        for (String rite : List.of("immortality", "curse_skill_seal")) {
            String hint = RitesBook.hint(lang, rite);
            assertTrue(hint.contains("Torn Page"), hint);
            assertTrue(hint.contains("ancient cities") && hint.contains("woodland mansions") && hint.contains("stronghold libraries"), hint);
        }
        assertTrue(RitesBook.label(lang, "immortality").contains("Torn Page"), RitesBook.label(lang, "immortality"));
    }

    /** The places the hint names are the chests the loot modifier puts the page in. */
    @Test
    void theLootPlacesTheHintNamesAreTheOnesTheModifierUses() throws IOException {
        String mod = Files.readString(ROOT.resolve("src/main/resources/data/supernaturalbloodlines/loot_modifiers/old_rites_in_ancient_places.json"));
        for (String table : List.of("chests/ancient_city", "chests/woodland_mansion", "chests/stronghold_library", "chests/hale_house")) {
            assertTrue(mod.contains(table), table);
        }
        assertTrue(mod.contains("part_enabled") && mod.contains("species"), "the page is an item of the species part: the modifier needs the part");
        assertTrue(Files.readString(ROOT.resolve("src/main/resources/data/supernaturalbloodlines/loot_modifiers/curses_in_ancient_places.json")).contains("\"part\": \"species\""),
                "the curses' modifier needs the species part too");
    }

    @Test
    void everyGatedRiteHasBothEntriesAndBothAdvancements() {
        for (String rite : RiteUnlockTable.GATED.keySet()) {
            for (String file : List.of(RitesBook.BOOK + "/entries/" + RiteAdvancements.entry(rite) + ".json", RitesBook.BOOK + "/entries/" + RiteAdvancements.sealedEntry(rite) + ".json",
                    RitesBook.ADV + "/known/" + rite + ".json", RitesBook.ADV + "/sealed/" + rite + ".json")) {
                assertTrue(Files.isRegularFile(ROOT.resolve(file)), file + " is missing");
            }
        }
    }

    private static JsonObject read(String file) throws IOException {
        return JsonParser.parseString(Files.readString(ROOT.resolve(file))).getAsJsonObject();
    }

    @Test
    void theRealEntryAsksForKnownAndTheSealedOneForSealed() throws IOException {
        for (String rite : RiteUnlockTable.GATED.keySet()) {
            assertEquals(RiteAdvancements.known(rite), read(RitesBook.BOOK + "/entries/" + RiteAdvancements.entry(rite) + ".json").get("advancement").getAsString(), rite);
            assertEquals(RiteAdvancements.sealed(rite), read(RitesBook.BOOK + "/entries/" + RiteAdvancements.sealedEntry(rite) + ".json").get("advancement").getAsString(), rite);
        }
        for (String rite : RiteUnlockTable.OPEN) {
            assertFalse(read(RitesBook.BOOK + "/entries/" + RiteAdvancements.entry(rite) + ".json").has("advancement"), rite + " is open and must not be locked");
        }
    }

    /** The study key opens an entry from an item without asking for the advancement, so a gated rite must not list the item it makes. */
    @Test
    void noGatedEntryIsOpenedByStudyingItsItem() throws IOException {
        for (String rite : RiteUnlockTable.GATED.keySet()) {
            JsonArray assigned = read(RitesBook.BOOK + "/entries/" + RiteAdvancements.entry(rite) + ".json").getAsJsonArray("assigned_items");
            assertEquals(0, assigned.size(), rite + " is gated and must not be studied from an item");
        }
    }

    @Test
    void aSealedEntryShowsNothingOfTheRite() throws IOException {
        JsonObject lang = RitesBook.lang(ROOT);
        for (String rite : RiteUnlockTable.GATED.keySet()) {
            JsonObject sealed = read(RitesBook.BOOK + "/entries/" + RiteAdvancements.sealedEntry(rite) + ".json");
            assertTrue(sealed.get("title").getAsString().startsWith("??? \u00b7 "), rite + ": " + sealed.get("title"));
            sealed.remove("advancement"); // the id of the advancement is not shown
            String text = sealed.toString().toLowerCase();
            assertFalse(text.contains("page/rite"), rite + ": a circle in the sealed entry");
            assertFalse(text.contains(lang.get("rite." + RitesBook.NS + "." + rite).getAsString().toLowerCase()), rite + ": the sealed entry names the rite");
            assertFalse(text.contains(rite), rite + ": the sealed entry carries the rite id");
            assertEquals(1, sealed.getAsJsonArray("pages").size());
        }
    }

    /** The two entries of a rite are in the same category, the sealed one first (the list shows only one of them). */
    @Test
    void theSealedEntryIsBesideTheRealOne() throws IOException {
        for (String rite : RiteUnlockTable.GATED.keySet()) {
            boolean found = false;
            try (var files = Files.list(ROOT.resolve(RitesBook.BOOK + "/categories/" + RitesBook.TOP))) {
                for (Path p : files.toList()) {
                    List<String> entries = new ArrayList<>();
                    JsonParser.parseString(Files.readString(p)).getAsJsonObject().getAsJsonArray("entries").forEach(e -> entries.add(e.getAsString()));
                    int real = entries.indexOf(RiteAdvancements.entry(rite));
                    if (real >= 0) {
                        assertEquals(real - 1, entries.indexOf(RiteAdvancements.sealedEntry(rite)), rite + ": the sealed entry is not just before the real one");
                        found = true;
                    }
                }
            }
            assertTrue(found, rite + " is in no category");
        }
    }

    @Test
    void theUnlockAdvancementsAreDisplaylessImpossibleRoots() throws IOException {
        for (String rite : RiteUnlockTable.GATED.keySet()) {
            for (String kind : List.of("known", "sealed")) {
                JsonObject adv = read(RitesBook.ADV + "/" + kind + "/" + rite + ".json");
                // no display (it would show a tab, a toast and a chat line) and no parent (a root is sent to the client exactly when it is done)
                assertFalse(adv.has("display") || adv.has("parent") || adv.has("rewards"), rite + " " + kind);
                assertEquals(List.of(List.of(RiteAdvancements.CRITERION)), adv.getAsJsonArray("requirements").asList().stream().map(a -> a.getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList()).toList(),
                        rite + " " + kind + ": without a requirement the advancement can never be done");
                assertEquals("minecraft:impossible", adv.getAsJsonObject("criteria").getAsJsonObject(RiteAdvancements.CRITERION).get("trigger").getAsString());
            }
        }
    }

    @Test
    void theItemsTooltipsSayWhatReadingThemTeachesAndThatTheyAreKept() throws IOException {
        JsonObject lang = RitesBook.lang(ROOT);
        String esther = lang.get("item.supernaturalbloodlines.esther_grimoire.tooltip").getAsString();
        assertTrue(esther.contains("teaches the Linking Spell and the Rite of Severance") && esther.contains("not used up"), esther);
        for (String key : List.of("item.supernaturalbloodlines.kol_grimoire.tooltip", "item.supernaturalbloodlines.kol_grimoire.tooltip_read")) {
            String kol = lang.get(key).getAsString();
            assertTrue(kol.contains("teaches the Rite of Cloaking"), key + ": " + kol);
        }
        String page = lang.get("item.supernaturalbloodlines.rite_page.tooltip").getAsString();
        assertTrue(page.contains("teaches %s") && page.contains("not used up"), page);
        assertTrue(lang.get("item.supernaturalbloodlines.rite_page.tooltip_blank").getAsString().contains("teaches nothing"));
    }

    @Test
    void noTextSendsAnyoneToARitesPage() throws IOException {
        JsonObject lang = RitesBook.lang(ROOT);
        for (var e : lang.entrySet()) {
            String text = e.getValue().getAsString().toLowerCase();
            assertFalse(text.contains("rites page") || text.contains("rites tab"), e.getKey() + " still says Rites page: " + e.getValue().getAsString());
        }
    }

    @Test
    void theGuideChapterSaysWhereTheRitesAreAndHowToGetTheBook() throws IOException {
        String body = RitesBook.lang(ROOT).get("supernaturalbloodlines.hub.guide.rites.body").getAsString();
        for (String needed : List.of("The Art of Witchcraft", "Supernatural Bloodlines", "???", "grimoire", "performing", "E seal", "belladonna")) {
            assertTrue(body.contains(needed), "the Rites chapter does not say \"" + needed + "\": " + body);
        }
    }

    @Test
    void theBookRefusalSaysToCraftIt() throws IOException {
        JsonObject lang = RitesBook.lang(ROOT);
        String text = lang.get("supernaturalbloodlines.hub.launcher.needs_book").getAsString();
        assertTrue(text.startsWith("You need The Art of Witchcraft") && text.contains("Craft it") && text.contains("belladonna"), text);
        assertEquals("Open this rite in The Art of Witchcraft, then draw its circle at an altar.", lang.get("supernaturalbloodlines.hub.hint.rite").getAsString());
        assertTrue(lang.get("supernaturalbloodlines.rites.sealed_tip").getAsString().startsWith("Sealed in the book"));
    }

    @Test
    void theCategoryBlurbsNameNoSealedRite() throws IOException {
        JsonObject lang = RitesBook.lang(ROOT);
        for (var cat : RitesBook.CATS) {
            String blurb = cat.landing().toLowerCase();
            for (String rite : RiteUnlockTable.GATED.keySet()) {
                String name = lang.get("rite." + RitesBook.NS + "." + rite).getAsString().toLowerCase();
                assertFalse(blurb.contains(name), cat.id() + ": the blurb names " + rite);
            }
        }
    }
}
