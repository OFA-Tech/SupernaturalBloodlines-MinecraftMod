package net.ofatech.supernaturalbloodlines.parts.evolutionrites;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import net.ofatech.supernaturalbloodlines.core.evolution.EvolutionRecipe;
import net.ofatech.supernaturalbloodlines.core.evolution.Tier;
import net.ofatech.supernaturalbloodlines.core.rite.RiteAdvancements;
import net.ofatech.supernaturalbloodlines.core.rite.RiteUnlockTable;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.EvolutionRitesConfig;
import net.ofatech.supernaturalbloodlines.core.evolution.EvolutionRecipeCodec;
import org.junit.jupiter.api.Test;

/** The evolution and mastery rite files against the rules of plan 19 (spec E1 to E16, 10.3, 13): conditions, sacrifices, the unlock table, the book. */
class EvolutionRitesDataTest {
    private static final Path ROOT = root();
    private static final String RITES = "src/main/resources/data/supernaturalbloodlines/enchanted/circle_magic/rite";
    private static final String ADV = "src/main/resources/data/supernaturalbloodlines/advancement/rites";

    private static Path root() {
        Path base = Path.of("").toAbsolutePath();
        while (!Files.isDirectory(base.resolve("src/main/java"))) {
            base = base.getParent();
        }
        return base;
    }

    private static JsonObject read(Path p) throws IOException {
        return JsonParser.parseString(Files.readString(p)).getAsJsonObject();
    }

    private static List<String> mine() throws IOException {
        try (Stream<Path> files = Files.list(ROOT.resolve(RITES))) {
            return files.map(p -> p.getFileName().toString().replace(".json", "")).filter(n -> n.startsWith("evolve_") || n.startsWith("mastery_")).sorted().toList();
        }
    }

    private static Set<String> conditions(JsonObject rite, String type, String key) {
        Set<String> out = new HashSet<>();
        for (JsonElement e : rite.getAsJsonArray("neoforge:conditions")) {
            JsonObject c = e.getAsJsonObject();
            if (c.get("type").getAsString().equals(type)) {
                out.add(c.get(key).getAsString());
            }
        }
        return out;
    }

    @Test
    void thereAreTwelveRites() throws IOException {
        assertEquals(List.of("evolve_blood_sovereign", "evolve_butcher", "evolve_deus_sanguis", "evolve_great_mage", "evolve_great_sage", "evolve_imitator",
                "evolve_king_of_curses", "evolve_raphael", "mastery_common", "mastery_extra", "mastery_ultimate", "mastery_unique"), mine());
    }

    /** Plan 19d: the four Uniques of the other addons (spec 8.4): their addon, their probe (the addon's switch, or none), and only Butcher asks for a kind. */
    @Test
    void everyUniqueOfPlan19dNamesItsAddonItsProbeAndItsKinds() throws IOException {
        String a = "unique.road:com.simplygray.tensuraaddon.AddonSkillConfig#";
        Object[][] table = {
                {"great_mage", "ascension:great_mage", "ascension", a + "GREAT_MAGE_ENABLED", Set.of()},
                {"king_of_curses", "ascension:king_of_curses", "ascension", a + "KING_OF_CURSES_ENABLED", Set.of()},
                {"butcher", "mysticism:butcher", "mysticism", "unique.no_switch", Set.of(net.ofatech.supernaturalbloodlines.core.rite.RiteKind.WEREWOLF)},
                {"imitator", "stextras:imitator", "stextras", "unique.no_switch", Set.of()},
        };
        for (Object[] row : table) {
            JsonObject rite = read(ROOT.resolve(RITES).resolve("evolve_" + row[0] + ".json"));
            EvolutionRecipe r = EvolutionRecipeCodec.RECIPE.codec().parse(JsonOps.INSTANCE, rite.getAsJsonObject("factory")).getOrThrow();
            assertEquals(row[1], r.grants(), row[0].toString());
            assertEquals(row[2], r.requiresMod(), row[0].toString());
            assertEquals(row[3], r.probe(), row[0].toString());
            assertEquals(row[4], r.subjectKinds(), row[0].toString());
            assertEquals(Tier.UNIQUE, r.tier(), row[0].toString());
            assertEquals(net.ofatech.supernaturalbloodlines.core.evolution.GrantForm.DEFAULT, r.grantForm(), row[0].toString());
            assertTrue(r.consumes().isEmpty() && r.needs().isEmpty(), row[0] + " keeps nothing and needs nothing: a new road");
            assertTrue(r.epCost() > 0, row[0] + " has no sacrifice (E15)");
        }
    }

    /** Rebalanced retires these skills (its {@code retired} tag, read for 19d); no rite may grant one (D3). */
    @Test
    void noRecipeGrantsARetiredSkill() throws IOException {
        Set<String> retired = Set.of("foxablazeultimate:azathoth", "foxablazeultimate:beelzebub", "foxablazeultimate:beelzebuth", "foxablazeultimate:velgrynd",
                "beelzebub", "beelzebuth", "mysticism:embryo", "trnightmare:imitator");
        for (String name : mine()) {
            JsonObject factory = read(ROOT.resolve(RITES).resolve(name + ".json")).getAsJsonObject("factory");
            if (factory.has("grants")) {
                assertFalse(retired.contains(factory.get("grants").getAsString()), name + " grants a retired skill");
            }
        }
    }

    @Test
    void theRaphaelRecipeIsTheDormantUltimateThatNeedsGreatSage() throws IOException {
        JsonObject rite = read(ROOT.resolve(RITES).resolve("evolve_raphael.json"));
        assertTrue(conditions(rite, "neoforge:mod_loaded", "modid").contains("greatsage"), "the rite must exist only with Great Sage");
        EvolutionRecipe recipe = EvolutionRecipeCodec.RECIPE.codec().parse(JsonOps.INSTANCE, rite.getAsJsonObject("factory")).getOrThrow();
        assertEquals(Tier.ULTIMATE, recipe.tier());
        assertEquals("greatsage:raphael", recipe.grants());
        assertEquals(net.ofatech.supernaturalbloodlines.core.evolution.GrantForm.FREE, recipe.grantForm());
        assertEquals("greatsage", recipe.requiresMod());
        assertEquals("greatsage.raphael", recipe.probe());
        assertEquals(Set.of("tensura:great_sage", "tensura:degenerate"), new HashSet<>(recipe.consumes()));
        Set<String> needs = new HashSet<>();
        recipe.needs().forEach(n -> {
            needs.add(n.skill());
            assertTrue(n.mastered(), n.skill() + " must be mastered");
        });
        assertEquals(Set.of("tensura:great_sage", "tensura:degenerate"), needs);
        assertTrue(recipe.subjectKinds().isEmpty());
    }

    @Test
    void everyRiteIsInThePartAndAnAddonRecipeIsInItsAddon() throws IOException {
        for (String name : mine()) {
            JsonObject rite = read(ROOT.resolve(RITES).resolve(name + ".json"));
            assertTrue(conditions(rite, "supernaturalbloodlines:part_enabled", "part").contains("evolutionRites"), name + " is not conditioned on the part");
            JsonObject factory = rite.getAsJsonObject("factory");
            if (factory.get("type").getAsString().equals("supernaturalbloodlines:evolution")) {
                String mod = factory.get("requires_mod").getAsString();
                if (!mod.equals("tensura")) {
                    assertTrue(conditions(rite, "neoforge:mod_loaded", "modid").contains(mod), name + " is not conditioned on " + mod + ": without the addon its factory block could still decode and the rite exist");
                }
            }
        }
    }

    @Test
    void everyEvolutionFileDecodesAndFollowsTheRules() throws IOException {
        for (String name : mine()) {
            JsonObject factory = read(ROOT.resolve(RITES).resolve(name + ".json")).getAsJsonObject("factory");
            if (!factory.get("type").getAsString().equals("supernaturalbloodlines:evolution")) {
                continue;
            }
            EvolutionRecipe recipe = EvolutionRecipeCodec.RECIPE.codec().parse(JsonOps.INSTANCE, factory).getOrThrow();
            assertEquals(name.replace("evolve_", ""), recipe.id(), name + ": the recipe id is not the file's");
            // E8: a rite never consumes what it did not require
            Set<String> needed = new HashSet<>();
            recipe.needs().forEach(n -> needed.add(n.skill()));
            assertTrue(needed.containsAll(recipe.consumes()), name + " consumes a skill it does not require: " + recipe.consumes());
            // E15: a sacrifice: an EP cost, or the addon's own tribute: among the circle's items (Ascension's Ultimate Catalyst), or the skills an Ultimate
            // consumes (Raphael's: Great Sage's evolution forgets Great Sage and Degenerate)
            assertTrue(recipe.epCost() > 0 || hasTribute(read(ROOT.resolve(RITES).resolve(name + ".json")), recipe)
                    || recipe.tier() == Tier.ULTIMATE && !recipe.consumes().isEmpty(), name + " has no sacrifice (E15)");
            assertTrue(recipe.tier() != Tier.COMMON, name);
        }
    }

    @Test
    void theGreatSageRecipeIsWhatTheSpecSays() throws IOException {
        JsonObject factory = read(ROOT.resolve(RITES).resolve("evolve_great_sage.json")).getAsJsonObject("factory");
        EvolutionRecipe recipe = EvolutionRecipeCodec.RECIPE.codec().parse(JsonOps.INSTANCE, factory).getOrThrow();
        assertEquals(Tier.UNIQUE, recipe.tier());
        assertEquals("tensura:great_sage", recipe.grants());
        assertEquals(1, recipe.needs().size());
        assertEquals("tensura:sage", recipe.needs().get(0).skill());
        assertTrue(recipe.needs().get(0).mastered());
        assertTrue(recipe.consumes().isEmpty(), "Sage stays");
        assertEquals("own.sage_great_sage", recipe.probe());
    }

    @Test
    void theDeusSanguisRecipeIsAVampireRoadThatNeedsAscension() throws IOException {
        JsonObject rite = read(ROOT.resolve(RITES).resolve("evolve_deus_sanguis.json"));
        EvolutionRecipe recipe = EvolutionRecipeCodec.RECIPE.codec().parse(JsonOps.INSTANCE, rite.getAsJsonObject("factory")).getOrThrow();
        assertEquals("ascension:deus_sanguis", recipe.grants());
        assertEquals("ascension", recipe.requiresMod());
        assertTrue(recipe.probe().startsWith("unique.road:com.simplygray.tensuraaddon.AddonSkillConfig#DEUS_SANGUIS_ENABLED"));
        assertEquals(Set.of(net.ofatech.supernaturalbloodlines.core.rite.RiteKind.VAMPIRE), recipe.subjectKinds());
    }

    /** The addon's own tribute: one of the rite's items is of the recipe's mod and is a catalyst. */
    private static boolean hasTribute(JsonObject rite, EvolutionRecipe recipe) {
        for (JsonElement e : rite.getAsJsonArray("items")) {
            String id = e.getAsJsonObject().get("id").getAsString();
            if (id.startsWith(recipe.requiresMod() + ":") && id.endsWith("_catalyst")) {
                return true;
            }
        }
        return false;
    }

    @Test
    void theBloodSovereignRecipeIsWhatTheSpecSays() throws IOException {
        JsonObject rite = read(ROOT.resolve(RITES).resolve("evolve_blood_sovereign.json"));
        EvolutionRecipe recipe = EvolutionRecipeCodec.RECIPE.codec().parse(JsonOps.INSTANCE, rite.getAsJsonObject("factory")).getOrThrow();
        assertEquals("blood_sovereign", recipe.id());
        assertEquals(Tier.ULTIMATE, recipe.tier(), "an Ultimate: the lunar eclipse");
        assertEquals("ascension:blood_sovereign", recipe.grants());
        assertEquals(net.ofatech.supernaturalbloodlines.core.evolution.GrantForm.PERMANENT, recipe.grantForm(), "as the altar learns it");
        assertEquals(List.of("ascension:deus_sanguis"), recipe.consumes(), "the altar forgets the Unique, and only that one");
        assertEquals(1, recipe.needs().size());
        assertEquals("ascension:deus_sanguis", recipe.needs().get(0).skill());
        assertTrue(recipe.needs().get(0).mastered(), "the altar asks for the Unique mastered");
        assertEquals("ascension.awakening", recipe.probe());
        assertEquals("ascension", recipe.requiresMod());
        assertTrue(recipe.subjectKinds().isEmpty(), "the road asks for no race of ours (D5)");
        assertEquals(0.0, recipe.epCost(), "the tribute is the catalyst, not EP (D1)");
        assertTrue(hasTribute(rite, recipe), "the catalyst is among the circle's items");
        assertTrue(conditions(rite, "neoforge:mod_loaded", "modid").contains("ascension"));
        assertEquals(20000, rite.get("power").getAsInt());
    }

    /** E7: an Ultimate evolution is sealed until the player has read lore; every other evolution is known by kind. */
    @Test
    void anUltimateEvolutionIsLoreGatedAndTheOthersAreKindGated() throws IOException {
        for (String name : mine()) {
            JsonObject factory = read(ROOT.resolve(RITES).resolve(name + ".json")).getAsJsonObject("factory");
            if (!factory.get("type").getAsString().equals("supernaturalbloodlines:evolution")) {
                continue;
            }
            Tier tier = EvolutionRecipeCodec.RECIPE.codec().parse(JsonOps.INSTANCE, factory).getOrThrow().tier();
            RiteUnlockTable.Gate gate = RiteUnlockTable.GATED.get(name);
            if (tier == Tier.ULTIMATE) {
                assertTrue(gate.kinds().isEmpty() && !gate.lore().isEmpty(), name + ": an Ultimate rite unlocks only by lore (E7)");
            } else {
                assertFalse(gate.kinds().isEmpty(), name + ": a Unique rite unlocks by kind (E7)");
            }
        }
        assertEquals(java.util.Set.of(RiteUnlockTable.FINN), RiteUnlockTable.GATED.get("evolve_blood_sovereign").lore(), "Finn's Notes, the rites of the eclipse (Q4)");
    }

    @Test
    void theMasteryFilesNameTheKindsOnceAndTheConfigHoldsTheOwnersCounts() throws IOException {
        Object[][] table = {
                {"common", 6000, java.util.Map.of("minecraft:diamond_block", 5, "minecraft:echo_shard", 5)},
                {"extra", 9000, java.util.Map.of("minecraft:netherite_ingot", 5, "minecraft:echo_shard", 20)},
                {"unique", 12000, java.util.Map.of("minecraft:nether_star", 5, "minecraft:netherite_ingot", 5)},
                {"ultimate", 20000, java.util.Map.of("minecraft:nether_star", 5, "minecraft:dragon_breath", 5, "minecraft:netherite_ingot", 10)}};
        for (Object[] row : table) {
            JsonObject rite = read(ROOT.resolve(RITES).resolve("mastery_" + row[0] + ".json"));
            assertEquals("supernaturalbloodlines:mastery", rite.getAsJsonObject("factory").get("type").getAsString());
            assertEquals(row[0], rite.getAsJsonObject("factory").get("tier").getAsString());
            assertEquals(row[1], rite.get("power").getAsInt(), row[0] + " power");
            java.util.Set<String> kinds = new java.util.TreeSet<>();
            for (JsonElement e : rite.getAsJsonArray("items")) {
                assertEquals(1, e.getAsJsonObject().get("count").getAsInt(), row[0] + ": the file names a kind, the config says how many");
                kinds.add(e.getAsJsonObject().get("id").getAsString());
            }
            @SuppressWarnings("unchecked")
            java.util.Map<String, Integer> owners = (java.util.Map<String, Integer>) row[2];
            Tier tier = Tier.parse((String) row[0]).orElseThrow();
            java.util.Map<String, Integer> defaults = net.ofatech.supernaturalbloodlines.core.evolution.Sacrifice.parse(EvolutionRitesConfig.DEFAULT_SACRIFICE.get(tier));
            assertEquals(owners, defaults, row[0] + ": the default sacrifice is the owner's");
            assertEquals(new java.util.TreeSet<>(owners.keySet()), kinds, row[0] + ": the rite file and the default sacrifice name the same kinds, or the circle never starts the rite");
            assertFalse(kinds.stream().anyMatch(s -> s.contains("supernaturalbloodlines:")), String.valueOf(row[0]));
        }
    }
    @Test
    void theEclipseRitesAreExactlyTheUltimateOnes() throws IOException {
        assertEquals(Tier.Night.ECLIPSE, Tier.ULTIMATE.night());
        JsonObject rite = read(ROOT.resolve(RITES).resolve("mastery_ultimate.json"));
        assertEquals("ultimate", rite.getAsJsonObject("factory").get("tier").getAsString());
        assertEquals("ultimate", read(ROOT.resolve(RITES).resolve("evolve_raphael.json")).getAsJsonObject("factory").get("tier").getAsString());
        for (String other : List.of("common", "extra", "unique")) {
            assertEquals(Tier.Night.FULL_MOON, Tier.parse(other).orElseThrow().night());
        }
    }

    @Test
    void everyRiteHasATableRowAndBothAdvancementsThatCarryItsConditions() throws IOException {
        for (String name : mine()) {
            assertTrue(RiteUnlockTable.isGated(name), name + " is not in RiteUnlockTable.GATED: D5 says these start sealed in the book");
            RiteUnlockTable.Gate gate = RiteUnlockTable.GATED.get(name);
            assertFalse(gate.kinds().isEmpty() && gate.lore().isEmpty(), name + ": unlocked by kind (the owner's rule), or by lore for an Ultimate evolution (E7)");
            JsonObject rite = read(ROOT.resolve(RITES).resolve(name + ".json"));
            for (String kind : List.of("known", "sealed")) {
                JsonObject adv = read(ROOT.resolve(ADV).resolve(kind).resolve(name + ".json"));
                assertEquals(rite.getAsJsonArray("neoforge:conditions"), adv.getAsJsonArray("neoforge:conditions"),
                        name + " " + kind + ": the advancement must load exactly when the rite does, or a book shows an entry for a rite that is not there");
            }
        }
        assertEquals(RiteAdvancements.known("evolve_great_sage"), "supernaturalbloodlines:rites/known/evolve_great_sage");
    }

    @Test
    void everyKeyTheRitesSayExistsInTheLang() throws IOException {
        JsonObject lang = read(ROOT.resolve("src/main/resources/assets/supernaturalbloodlines/lang/en_us.json"));
        String p = "supernaturalbloodlines.";
        List<String> keys = new ArrayList<>(List.of(p + "refusal.evolution.closed", p + "refusal.evolution.not_witch", p + "refusal.evolution.already", p + "refusal.evolution.not_ready",
                p + "rite.needs_eclipse", p + "rite.needs_full_moon", p + "rite.sneak_to_evolve", p + "rite.sneak_to_master", p + "rite.evolution_done", p + "rite.evolution_done_caster",
                p + "rite.mastery_done", p + "rite.mastery_done_caster", p + "hub.admin.section.evolutionRites"));
        for (String r : List.of("no_choice", "not_held", "temporary", "already", "wrong_tier", "system", "retired", "no_ep", "cooldown", "vetoed")) {
            keys.add(p + "refusal.mastery." + r);
        }
        for (String name : mine()) {
            keys.add("rite." + p + name);
            keys.add(p + "hub.rites.desc." + name + ".1");
        }
        for (String key : keys) {
            assertTrue(lang.has(key), "lang is missing " + key);
        }
        JsonArray none = new JsonArray();
        assertEquals(0, none.size());
    }
}
