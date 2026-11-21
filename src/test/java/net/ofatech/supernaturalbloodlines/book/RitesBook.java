package net.ofatech.supernaturalbloodlines.book;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.ofatech.supernaturalbloodlines.core.rite.RiteAdvancements;
import net.ofatech.supernaturalbloodlines.core.rite.RiteUnlockTable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Writes this mod's chapter of Enchanted's book (a Modopedia book): one entry per rite file, built from the rite JSON (its circle) and the
 * {@code hub.rites.desc.<rite>.<n>} lang lines (what it does, who, the cost), so the book and the rite data cannot disagree.
 * Output is committed under {@link #BOOK}; {@code RitesBookTest} fails when the committed files are not what this makes.
 * Regenerate with {@code RITES_BOOK_WRITE=1 ./gradlew test --tests '*RitesBookTest'}.
 */
final class RitesBook {
    static final String NS = "supernaturalbloodlines";
    static final String BOOK = "src/main/resources/assets/supernaturalbloodlines/modopedia/books/art_of_witchcraft/en_us";
    static final String RITES = "src/main/resources/data/supernaturalbloodlines/enchanted/circle_magic/rite";
    /** The unlock advancements (generated with the book: {@code known/<rite>} and {@code sealed/<rite>}). */
    static final String ADV = "src/main/resources/data/supernaturalbloodlines/advancement/rites";
    static final String HINT = "supernaturalbloodlines.rites.hint.";
    static final String SEALED_TEXT = "supernaturalbloodlines.rites.sealed";
    static final String LANG = "src/main/resources/assets/supernaturalbloodlines/lang/en_us.json";
    static final String DESC = "supernaturalbloodlines.hub.rites.desc.";

    /** What fits on a Modopedia text page under its header (100 x 135 px of text): the longest a page's text may be. */
    static final int PAGE_CHARS = 230;

    static final String TOP = NS + "/rites";
    static final String INTRO = NS + "/intro";

    /** The chalks and shapes Enchanted has a circle picture for (circle_magic_shapes/&lt;shape&gt;_&lt;chalk&gt;.png). */
    static final List<String> SHAPES = List.of("small_circle", "medium_circle", "large_circle");
    static final Map<String, String> CHALKS = Map.of("ritual_chalk", "ritual chalk", "nether_chalk", "infernal chalk", "otherwhere_chalk", "otherwhere chalk");

    record Cat(String id, String title, String icon, String landing, List<String> rites) {
    }

    static final List<Cat> CATS = List.of(
            new Cat("vampires", "Vampires and Hunters", "minecraft:redstone",
                    "Rites of blood: making an Original, washing vampirism out, bottling an Original's blood, blood stones for altars and marking a Hunter of the Five.",
                    List.of("immortality", "cleansing_blood", "original_blood", "blood_offering", "rite_of_the_five")),
            new Cat("werewolves", "Werewolves", "minecraft:bone",
                    "Rites of the moon and the wolf: the gene, the hybrids, the silver cure, the Alpha Trial and the brothers of the pack.",
                    List.of("rite_of_lineage_reveal", "rite_of_lineage_awaken", "rite_of_the_moon", "silver_moon", "hybrid_blood", "werewolf_brother", "alpha_trial")),
            new Cat("witches", "Witchcraft", "minecraft:amethyst_shard",
                    "Rites a witch works, or that call witchcraft awake: ancestral calling, hiding, linking, revealing genes, cutting a sire bond and drawing doppelg\u00e4nger blood.",
                    List.of("ancestral_calling", "rite_of_revelation", "doppelganger_blood", "rite_of_severance", "rite_of_cloaking", "rite_of_linking")),
            new Cat("lines", "Lines of the Originals", "minecraft:white_dye",
                    "Rites that free someone from the line of their Original, and the rite that makes The Cure.",
                    List.of("rite_of_the_broken_line", "rite_of_the_imprisoned_original", "rite_of_the_cure")),
            new Cat("artifacts", "Wards and Tools", "minecraft:compass",
                    "Rites that make rings and tools: the Sun Ring, the Moon Ring, the Resurrection Ring, the Vampire Compass and the Hearing Device.",
                    List.of("ward_of_daylight", "ward_of_the_moon", "ward_of_return", "vampire_compass", "hearing_device")),
            new Cat("tensura", "Tensura", "minecraft:soul_sand",
                    "Rites that need Tensura: two curses, an offering of magicules and the purification of the undead.",
                    List.of("curse_magicule_drain", "curse_skill_seal", "magicule_offering", "purification", "purification_holy_water")));

    /** The book entry's icon. Only {@code minecraft:} items: an icon from a mod that is not there would drop the whole entry. */
    static final Map<String, String> ICON = Map.ofEntries(
            Map.entry("alpha_trial", "minecraft:bone"), Map.entry("ancestral_calling", "minecraft:amethyst_shard"),
            Map.entry("blood_offering", "minecraft:glass_bottle"), Map.entry("cleansing_blood", "minecraft:golden_apple"),
            Map.entry("curse_magicule_drain", "minecraft:soul_sand"), Map.entry("curse_skill_seal", "minecraft:soul_sand"),
            Map.entry("doppelganger_blood", "minecraft:diamond"), Map.entry("hearing_device", "minecraft:note_block"),
            Map.entry("hybrid_blood", "minecraft:glass_bottle"), Map.entry("immortality", "minecraft:golden_apple"),
            Map.entry("magicule_offering", "minecraft:amethyst_shard"), Map.entry("original_blood", "minecraft:glass_bottle"),
            Map.entry("purification", "minecraft:gold_ingot"), Map.entry("purification_holy_water", "minecraft:gold_ingot"),
            Map.entry("rite_of_cloaking", "minecraft:phantom_membrane"), Map.entry("rite_of_lineage_awaken", "minecraft:bone"),
            Map.entry("rite_of_lineage_reveal", "minecraft:paper"), Map.entry("rite_of_linking", "minecraft:lead"),
            Map.entry("rite_of_revelation", "minecraft:spyglass"), Map.entry("rite_of_severance", "minecraft:shears"),
            Map.entry("rite_of_the_broken_line", "minecraft:chain"), Map.entry("rite_of_the_cure", "minecraft:enchanted_golden_apple"),
            Map.entry("rite_of_the_five", "minecraft:candle"), Map.entry("rite_of_the_imprisoned_original", "minecraft:iron_bars"),
            Map.entry("rite_of_the_moon", "minecraft:clock"), Map.entry("silver_moon", "minecraft:iron_ingot"),
            Map.entry("vampire_compass", "minecraft:compass"), Map.entry("ward_of_daylight", "minecraft:sunflower"),
            Map.entry("ward_of_return", "minecraft:totem_of_undying"), Map.entry("ward_of_the_moon", "minecraft:amethyst_shard"),
            Map.entry("werewolf_brother", "minecraft:beef"));

    /** The items a rite makes: looking one up in the book opens the rite. */
    static final Map<String, String> MAKES = Map.of(
            "ward_of_daylight", NS + ":sun_ring", "ward_of_the_moon", NS + ":moon_ring", "ward_of_return", NS + ":resurrection_ring",
            "vampire_compass", NS + ":vampire_compass", "hearing_device", NS + ":hearing_device", "rite_of_the_cure", NS + ":the_cure",
            "blood_offering", NS + ":blood_stone", "magicule_offering", NS + ":condensed_magicule");

    /**
     * The width in pixels of a string in Minecraft's default font (the book draws its headers in it, centred in a 100 pixel line): most characters are 6
     * wide with their gap, the narrow ones less. "Rite of the Moon" is 81, as it measures on screen.
     */
    static int width(String s) {
        int w = 0;
        for (char c : s.toCharArray()) {
            w += switch (c) {
                case ' ' -> 4;
                case '!', ',', '.', ':', ';', 'i', '|' -> 2;
                case '\'', 'l', '`' -> 3;
                case 'I', '[', ']', 't' -> 4;
                case '"', '(', ')', '*', '<', '>', 'f', 'k', '{', '}' -> 5;
                case '@', '~' -> 7;
                default -> 6;
            };
        }
        return w;
    }

    /** The widest header that stays inside the book's line (Modopedia's default line width is 100). */
    static final int HEADER_WIDTH = 98;

    /** A shorter header for the rites whose name does not fit; the entry's title (in the lists) is always the whole name. */
    static final Map<String, String> SHORT = Map.ofEntries(
            Map.entry("purification_holy_water", "Holy Purification"), Map.entry("rite_of_the_imprisoned_original", "Imprisoned Original"),
            Map.entry("vampire_compass", "Vampire Compass"), Map.entry("doppelganger_blood", "Doppelg\u00e4nger"),
            Map.entry("hearing_device", "Hearing Device"), Map.entry("rite_of_lineage_awaken", "Lineage: Awaken"),
            Map.entry("rite_of_lineage_reveal", "Lineage: Reveal"), Map.entry("magicule_offering", "Magicule Offering"),
            Map.entry("ancestral_calling", "Ancestral Calling"), Map.entry("curse_magicule_drain", "Magicule Drain"),
            Map.entry("rite_of_the_broken_line", "The Broken Line"), Map.entry("cleansing_blood", "Cleansing Blood"),
            Map.entry("silver_moon", "The Silver Moon"), Map.entry("original_blood", "Original's Blood"),
            Map.entry("blood_offering", "Blood Offering"), Map.entry("hybrid_blood", "Hybrid's Blood"));

    static String header(String path, String name) {
        return width(name) <= HEADER_WIDTH ? name : SHORT.getOrDefault(path, name);
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private RitesBook() {
    }

    /** The rite file names, without {@code .json}. */
    static List<String> rites(Path root) throws IOException {
        try (Stream<Path> files = Files.list(root.resolve(RITES))) {
            return files.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".json")).map(n -> n.substring(0, n.length() - 5)).sorted().toList();
        }
    }

    static JsonObject lang(Path root) throws IOException {
        return JsonParser.parseString(Files.readString(root.resolve(LANG))).getAsJsonObject();
    }

    static JsonObject rite(Path root, String path) throws IOException {
        return JsonParser.parseString(Files.readString(root.resolve(RITES).resolve(path + ".json"))).getAsJsonObject();
    }

    /** The {@code hub.rites.desc} lines of a rite, in order, until the first gap. */
    static List<String> desc(JsonObject lang, String path) {
        List<String> out = new ArrayList<>();
        for (int i = 1; lang.has(DESC + path + "." + i); i++) {
            out.add(lang.get(DESC + path + "." + i).getAsString());
        }
        return out;
    }

    /** "medium circle, infernal chalk" for each circle of the rite file, joined with "and". */
    static String circleLine(JsonObject rite) {
        List<String> circles = new ArrayList<>();
        for (var e : rite.getAsJsonObject("shapes").entrySet()) {
            String shape = e.getKey().substring(e.getKey().indexOf(':') + 1).replace('_', ' ');
            String chalk = e.getValue().getAsString().substring(e.getValue().getAsString().indexOf(':') + 1);
            circles.add(shape + ", " + CHALKS.getOrDefault(chalk, chalk));
        }
        return "Circle: " + String.join(" and ", circles) + ".";
    }

    /** The text pages of an entry: the parts joined with a blank line, a new page when the next part would not fit. */
    static List<String> textPages(String path, List<String> parts) {
        List<String> pages = new ArrayList<>();
        StringBuilder page = new StringBuilder();
        for (String part : parts) {
            if (part.length() > PAGE_CHARS) {
                throw new IllegalStateException(path + ": a line of " + part.length() + " characters does not fit a book page (" + PAGE_CHARS + "): " + part);
            }
            if (page.length() > 0 && page.length() + 2 + part.length() > PAGE_CHARS) {
                pages.add(page.toString());
                page.setLength(0);
            }
            page.append(page.length() > 0 ? "\n\n" : "").append(part);
        }
        pages.add(page.toString());
        return pages;
    }

    private static JsonObject component(String template, String key, String value) {
        JsonObject c = new JsonObject();
        c.addProperty("template", template);
        c.addProperty(key, value);
        return c;
    }

    private static JsonObject page(JsonObject... components) {
        JsonArray list = new JsonArray();
        for (JsonObject c : components) {
            list.add(c);
        }
        JsonObject page = new JsonObject();
        page.add("components", list);
        return page;
    }

    private static JsonObject icon(String id) {
        JsonObject o = new JsonObject();
        o.addProperty("count", 1);
        o.addProperty("id", id);
        return o;
    }

    static JsonObject entry(Path root, JsonObject lang, String path) throws IOException {
        String name = lang.get("rite." + NS + "." + path).getAsString();
        String header = header(path, name);
        String circle = circleLine(rite(root, path));
        List<String> text = desc(lang, path);
        // the circle line first; when the next line does not fit beside it the first page would be only that line, so it goes (the picture and the text say it)
        List<String> parts = new ArrayList<>();
        parts.add(circle);
        parts.addAll(text);
        List<String> texts = textPages(path, parts);
        if (texts.get(0).equals(circle)) {
            texts = textPages(path, text);
        }
        JsonObject entry = new JsonObject();
        entry.addProperty("title", name);
        entry.add("icon", icon(ICON.get(path)));
        JsonArray assigned = new JsonArray();
        RiteUnlockTable.Gate gate = RiteUnlockTable.GATED.get(path);
        // the study key opens an entry from an item without asking for its advancement: a rite that lore teaches must not be reachable that way
        if (MAKES.containsKey(path) && (gate == null || gate.lore().isEmpty())) {
            assigned.add(MAKES.get(path));
        }
        entry.add("assigned_items", assigned);
        if (gate != null) {
            entry.addProperty("advancement", RiteAdvancements.known(path));
        }
        JsonArray pages = new JsonArray();
        for (String page : texts) {
            JsonObject c = component("modopedia:page/headered_text", "text", page);
            c.addProperty("header", header);
            pages.add(page(c));
        }
        JsonObject rite = component(NS + ":page/rite", "rite", NS + ":" + path);
        rite.addProperty("header", header);
        pages.add(page(rite));
        entry.add("pages", pages);
        return entry;
    }

    /** The "???" entry that stands for a gated rite while it is sealed: a hint, and nothing of the rite (no name, circle or item). */
    static JsonObject sealedEntry(JsonObject lang, String path) {
        RiteUnlockTable.Gate gate = RiteUnlockTable.GATED.get(path);
        JsonObject entry = new JsonObject();
        entry.addProperty("title", "???");
        entry.add("icon", icon("minecraft:honeycomb"));
        entry.add("assigned_items", new JsonArray());
        entry.addProperty("advancement", RiteAdvancements.sealed(path));
        JsonObject text = component("modopedia:page/headered_text", "text",
                lang.get(HINT + gate.hint()).getAsString() + "\n\n" + lang.get(SEALED_TEXT).getAsString());
        text.addProperty("header", "???");
        JsonArray pages = new JsonArray();
        pages.add(page(text));
        entry.add("pages", pages);
        return entry;
    }

    static String advancementJson() {
        JsonObject criterion = new JsonObject();
        criterion.addProperty("trigger", "minecraft:impossible");
        JsonObject criteria = new JsonObject();
        criteria.add(RiteAdvancements.CRITERION, criterion);
        JsonObject adv = new JsonObject();
        adv.add("criteria", criteria);
        return GSON.toJson(adv) + "\n";
    }

    private static JsonObject category(String title, String icon, String landing, List<String> children, List<String> entries, int sort, boolean front) {
        JsonObject c = new JsonObject();
        JsonArray kids = new JsonArray();
        children.forEach(kids::add);
        JsonArray ents = new JsonArray();
        entries.forEach(ents::add);
        c.add("children", kids);
        if (!front) {
            c.addProperty("display_on_front_page", false);
        }
        c.add("entries", ents);
        c.add("icon", icon(icon));
        c.addProperty("landing_text", landing);
        if (front) {
            c.addProperty("sort_num", sort);
        }
        c.addProperty("title", title);
        return c;
    }

    static JsonObject intro() {
        String link = "$(b)$(el:circle_magic/tutorial/performing_rites)Performing Rites$(/el)$(/b)";
        JsonObject entry = new JsonObject();
        entry.addProperty("title", "Supernatural Rites");
        entry.add("icon", icon("minecraft:ender_eye"));
        entry.add("assigned_items", new JsonArray());
        JsonArray pages = new JsonArray();
        JsonObject one = component("modopedia:page/headered_text", "text",
                "These rites come with Supernatural Bloodlines and work like all circle magic: draw the golden heart glyph and the circle, put the items in it and activate it.\n\nThe basics: " + link + ".");
        one.addProperty("header", "Supernatural Rites");
        pages.add(page(one));
        JsonObject two = component("modopedia:page/headered_text", "text",
                "Many of these rites act on whoever stands on the heart glyph in the centre. When nobody does, they act on whoever performs the rite.\n\nA rite a server has switched off shows a note in place of its circle.");
        two.addProperty("header", "On the Centre");
        pages.add(page(two));
        entry.add("pages", pages);
        return entry;
    }

    /** Every generated file, by path relative to the project root. */
    static Map<String, String> generate(Path root) throws IOException {
        JsonObject lang = lang(root);
        Map<String, String> out = new TreeMap<>();
        List<String> all = rites(root);
        List<String> listed = new ArrayList<>();
        List<String> kids = new ArrayList<>();
        for (Cat cat : CATS) {
            String id = TOP + "/" + cat.id();
            kids.add(id);
            List<String> entries = new ArrayList<>();
            for (String path : cat.rites()) {
                // a gated rite is two entries side by side: the "???" one while it is sealed, its own once it is known (one of them shows)
                if (RiteUnlockTable.isGated(path)) {
                    entries.add(RiteAdvancements.sealedEntry(path));
                }
                entries.add(TOP + "/" + path);
                listed.add(path);
            }
            out.put(BOOK + "/categories/" + id + ".json", GSON.toJson(category(cat.title(), cat.icon(), cat.landing(), List.of(), entries, 0, false)) + "\n");
        }
        out.put(BOOK + "/categories/" + TOP + ".json", GSON.toJson(category("Supernatural Bloodlines", "minecraft:ender_eye",
                "The rites of Supernatural Bloodlines: vampires, werewolves, witches, the lines of the Originals and the things they make.\n\nThey are performed like any other circle magic.",
                kids, List.of(INTRO), 6, true)) + "\n");
        out.put(BOOK + "/entries/" + INTRO + ".json", GSON.toJson(intro()) + "\n");
        for (String path : all) {
            if (!listed.contains(path)) {
                throw new IllegalStateException(path + " is in no category of RitesBook.CATS");
            }
            out.put(BOOK + "/entries/" + TOP + "/" + path + ".json", GSON.toJson(entry(root, lang, path)) + "\n");
            if (RiteUnlockTable.isGated(path)) {
                out.put(BOOK + "/entries/" + RiteAdvancements.sealedEntry(path) + ".json", GSON.toJson(sealedEntry(lang, path)) + "\n");
                out.put(ADV + "/known/" + path + ".json", advancementJson());
                out.put(ADV + "/sealed/" + path + ".json", advancementJson());
            }
        }
        return out;
    }

    static Map<String, String> onDisk(Path root) throws IOException {
        Map<String, String> out = new LinkedHashMap<>();
        for (String dir : List.of(BOOK, ADV)) {
            Path base = root.resolve(dir);
            if (!Files.isDirectory(base)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(base)) {
                for (Path p : files.filter(Files::isRegularFile).toList()) {
                    out.put(root.relativize(p).toString().replace('\\', '/'), Files.readString(p).replace("\r\n", "\n"));
                }
            }
        }
        return out;
    }
}
