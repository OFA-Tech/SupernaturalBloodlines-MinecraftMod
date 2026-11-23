package net.ofatech.supernaturalbloodlines.core.wheel.overlap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerRef;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerSource;
import org.junit.jupiter.api.Test;

class OverlapTableTest {
    static OverlapTable seed() {
        return parseResource("/wheel/overlaps-seed.json");
    }

    static OverlapTable production() {
        return parseResource("/data/supernaturalbloodlines/wheel/overlaps.json");
    }

    static OverlapTable parseResource(String path) {
        try (var in = OverlapTableTest.class.getResourceAsStream(path)) {
            return OverlapTable.parse(JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static PowerRef ref(String key) {
        return PowerRef.parse(key).orElseThrow();
    }

    private static JsonObject json(String s) {
        return JsonParser.parseString(s).getAsJsonObject();
    }

    private static String group(String id, String confidence, int main, String... members) {
        return "{\"id\":\"" + id + "\",\"confidence\":\"" + confidence + "\",\"main\":" + main + ",\"members\":[\"" + String.join("\",\"", members) + "\"]}";
    }

    private static JsonObject file(String... groups) {
        return json("{\"format\":1,\"groups\":[" + String.join(",", groups) + "]}");
    }

    private static final String A = "vampirism/vampirism:a";
    private static final String B = "werewolves/werewolves:b";
    private static final String C = "tensura/tensura:c";

    @Test
    void seedFileParses() {
        OverlapTable t = seed();
        OverlapGroup m01 = t.groups().get(0);
        assertEquals("invisibility", m01.id());
        assertEquals("M01", m01.research());
        assertEquals(0, m01.main());
        assertEquals(ref("vampirism/vampirism:vampire_invisibility"), m01.mainMember());
        assertTrue(m01.members().contains(ref("infinity/infinity:reality.invisibility")));
        assertTrue(m01.members().contains(ref("tensura/tensura:invisible")));
        OverlapGroup m06 = t.groups().get(1);
        assertEquals(List.of(ref("vampirism/vampirism:vampire_rage"), ref("werewolves/werewolves:rage")), m06.members());
        assertEquals(1, t.chains().size());
    }

    @Test
    void looseGroupRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> OverlapTable.parse(file(group("g", "loose", 0, A, B))));
        assertTrue(e.getMessage().contains("g"), e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> OverlapTable.parse(file(group("g", "", 0, A, B))));
    }

    @Test
    void sameRefInTwoGroupsIsAllowed() {
        OverlapTable t = OverlapTable.parse(file(group("one", "close", 0, A, C), group("two", "close", 0, B, C)));
        assertEquals(2, t.groupsOf(ref(C)).size());
        assertEquals(List.of("one", "two"), t.groupsOf(ref(C)).stream().map(OverlapGroup::id).toList());
    }

    @Test
    void sameRefTwiceInOneGroupRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> OverlapTable.parse(file(group("g", "same", 0, A, A))));
        assertTrue(e.getMessage().contains("g"));
    }

    @Test
    void mainOutOfRangeRejected() {
        assertThrows(IllegalArgumentException.class, () -> OverlapTable.parse(file(group("g", "same", 2, A, B))));
        assertThrows(IllegalArgumentException.class, () -> OverlapTable.parse(file(group("g", "same", -1, A, B))));
    }

    @Test
    void singleMemberRejected() {
        assertThrows(IllegalArgumentException.class, () -> OverlapTable.parse(file(group("g", "same", 0, A))));
    }

    @Test
    void unknownSourceRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> OverlapTable.parse(file(group("g", "same", 0, A, "someothermod/x:y"))));
        assertTrue(e.getMessage().contains("g"));
    }

    @Test
    void idInTwoChainsRejected() {
        assertThrows(IllegalArgumentException.class, () -> OverlapTable.parse(json(
                "{\"format\":1,\"chains\":[[\"tensura/tensura:a\",\"tensura/tensura:b\"],[\"tensura/tensura:b\",\"tensura/tensura:c\"]],\"groups\":[]}")));
    }

    @Test
    void wrongFormatRejected() {
        assertThrows(IllegalArgumentException.class, () -> OverlapTable.parse(json("{\"format\":2,\"groups\":[]}")));
    }

    @Test
    void duplicateGroupIdRejected() {
        assertThrows(IllegalArgumentException.class, () -> OverlapTable.parse(file(group("g", "same", 0, A, B), group("g", "same", 0, A, C))));
    }

    @Test
    void aTierOfAChainIsItsChainWhereGroupsAreMatched() {
        OverlapTable t = seed();
        PowerRef infinite = ref("tensura/tensura:infinite_regeneration");
        assertEquals(List.of("regeneration"), t.groupsOf(infinite).stream().map(OverlapGroup::id).toList());
        assertEquals(0, t.sideOf(t.groups().get(2), ref("vampirism/vampirism:regen")));
        assertEquals(1, t.sideOf(t.groups().get(2), infinite));
    }

    // ---- the production table against the research ----

    private static final Pattern BACKTICKED = Pattern.compile("`([a-z0-9_]+):([a-z0-9_./\\-]+)`");

    /** The mod prefixes of the research table that are Tensura addons (their skills are Tensura skills: source TENSURA) and the ones with their own source. */
    private static final Map<String, PowerSource> KNOWN_PREFIXES = Map.ofEntries(Map.entry("vampirism", PowerSource.VAMPIRISM), Map.entry("werewolves", PowerSource.WEREWOLVES),
            Map.entry("tensura", PowerSource.TENSURA), Map.entry("infinity", PowerSource.INFINITY), Map.entry("greatsage", PowerSource.GREAT_SAGE),
            Map.entry("supernaturalbloodlines", PowerSource.BLOODLINE), Map.entry("ascension", PowerSource.TENSURA), Map.entry("horizons", PowerSource.TENSURA),
            Map.entry("tensurahorizons", PowerSource.TENSURA), Map.entry("foxablazeultimate", PowerSource.TENSURA), Map.entry("mysticism", PowerSource.TENSURA),
            Map.entry("stextras", PowerSource.TENSURA), Map.entry("tensura_kumodesu", PowerSource.TENSURA), Map.entry("kumodesu", PowerSource.TENSURA),
            Map.entry("ascensioncore", PowerSource.TENSURA), Map.entry("minecraft", PowerSource.TENSURA));

    private static String research() {
        Path root = Path.of("").toAbsolutePath();
        while (!Files.isDirectory(root.resolve("src/main/java"))) {
            root = root.getParent();
        }
        Path p = root.resolve("docs/superpowers/research/skill-overlaps.md");
        try {
            return Files.readString(p, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("cannot read " + p.toAbsolutePath(), e);
        }
    }

    @Test
    void everyIdInTheResearchTableUsesAKnownSource() {
        Matcher m = BACKTICKED.matcher(research());
        int checked = 0;
        Set<String> unknown = new HashSet<>();
        while (m.find()) {
            String prefix = m.group(1);
            // lang and config style words (`key.x`, `mod:id` in prose) are not ids: only the colon form with a known-looking path is checked
            if (!KNOWN_PREFIXES.containsKey(prefix) && prefix.matches("[a-z_]+") && m.group(2).matches("[a-z_.]+") && looksLikePowerId(prefix)) {
                unknown.add(prefix);
            }
            checked++;
        }
        assertTrue(checked > 40, "the research table was not read: " + checked);
        assertTrue(unknown.isEmpty(), "research ids with a prefix that maps to no source: " + unknown);
    }

    /** The research uses `mod:id` only for powers and for a few other things (`key.x`, `Mod:Id` prose); a prefix is a power's mod when the research says so in a table row. */
    private static boolean looksLikePowerId(String prefix) {
        return !prefix.equals("key") && !prefix.equals("keys") && !prefix.equals("tensuraslots") && !prefix.equals("tag") && !prefix.equals("data") && !prefix.equals("mod");
    }

    /** The shipped groups: id, research row, main member. A typo in the data file fails here. */
    private static final Map<String, String> SHIPPED_MAINS = Map.ofEntries(
            Map.entry("invisibility", "vampirism/vampirism:vampire_invisibility"),
            Map.entry("night_vision", "vampirism/vampirism:vision"),
            Map.entry("bat_form", "vampirism/vampirism:bat"),
            Map.entry("flight", "tensura/tensura:gravity_flight"),
            Map.entry("regeneration", "vampirism/vampirism:regen"),
            Map.entry("rage", "vampirism/vampirism:vampire_rage"),
            Map.entry("fear", "werewolves/werewolves:fear"),
            Map.entry("teleport", "vampirism/vampirism:teleport"),
            Map.entry("sense", "werewolves/werewolves:sense"),
            Map.entry("appraisal", "tensura/tensura:analytical_appraisal"),
            Map.entry("bite", "bloodline/supernaturalbloodlines:bite"));

    @Test
    void shippedGroupsAreExactlyTheAcceptedRows() {
        OverlapTable t = production();
        assertEquals(SHIPPED_MAINS.size(), t.groups().size(), "groups: " + t.groups().stream().map(OverlapGroup::id).toList());
        for (OverlapGroup g : t.groups()) {
            assertEquals(ref(SHIPPED_MAINS.get(g.id())), g.mainMember(), g.id());
        }
        // rows M01 to M08, M10, M11, M12 (M09 keeps both sides): the research ids of the groups
        assertEquals(List.of("M01", "M02", "M03", "M04", "M05", "M06", "M07", "M08", "M10", "M11", "M12"), t.groups().stream().map(OverlapGroup::research).toList());
    }

    @Test
    void noInfinityPowerIsAMember() {
        for (OverlapGroup g : production().groups()) {
            for (PowerRef r : g.members()) {
                assertTrue(r.source() != PowerSource.INFINITY, g.id() + " has an Infinity member: " + r.key() + " (a stone power is a mode of its stone, owner 2026-11-22)");
            }
        }
    }

    @Test
    void theLeftOutPowersAreNotMembers() {
        Set<String> out = Set.of("tensura/tensura:mortal_fear", "tensura/tensura:hero_haki", "tensura/tensura:demon_lord_haki", "tensura/tensura:sacred_haki", "tensura/tensura:traveler",
                "tensura/tensura:suppressor", "tensura/ascension:blood_blade", "vampirism/vampirism:dark_blood_projectile");
        for (OverlapGroup g : production().groups()) {
            for (PowerRef r : g.members()) {
                assertTrue(!out.contains(r.key()), g.id() + " has " + r.key() + ", which the owner left out");
            }
        }
    }

    @Test
    void raceAbilityIsInBatFormAndBite() {
        OverlapTable t = production();
        PowerRef race = ref("tensura/tensura:race_ability");
        assertEquals(List.of("bat_form", "bite"), t.groupsOf(race).stream().map(OverlapGroup::id).toList());
    }

    @Test
    void everyShippedMemberIsNamedInTheResearch() {
        String text = research();
        Set<String> named = new HashSet<>();
        Matcher m = BACKTICKED.matcher(text);
        while (m.find()) {
            named.add(m.group(1) + ":" + m.group(2));
        }
        // ids the wheel gives that the research names another way: the vision key, the race ability, our bite, and the horizons prefix
        Set<String> alias = Set.of("vampirism:vision", "tensura:race_ability", "supernaturalbloodlines:bite", "greatsage:scan");
        for (OverlapGroup g : production().groups()) {
            for (PowerRef r : g.members()) {
                String id = r.id().toString().replace("tensurahorizons:", "horizons:");
                assertTrue(named.contains(id) || alias.contains(id) || named.contains(r.id().toString()), g.id() + ": " + r.id() + " is not in the research table");
            }
        }
    }

    @Test
    void theChainsAreTheTensuraKindsChains() {
        OverlapTable t = production();
        assertEquals(1, t.chains().size());
        assertEquals(List.of(ref("tensura/tensura:self_regeneration"), ref("tensura/tensura:ultraspeed_regeneration"), ref("tensura/tensura:infinite_regeneration")), t.chains().get(0));
    }
}
