package net.ofatech.supernaturalbloodlines.core.supernatural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SkillTableTest {
    private static JsonElement json(String s) {
        return JsonParser.parseString(s);
    }

    private static Map<String, JsonElement> skills(String... idAndJson) {
        Map<String, JsonElement> m = new LinkedHashMap<>();
        for (int i = 0; i < idAndJson.length; i += 2) {
            m.put(idAndJson[i], json(idAndJson[i + 1]));
        }
        return m;
    }

    private static final String HEALTH = "{\"id\":\"S-A\",\"name\":\"A\",\"cost\":2,\"kinds\":[\"attr\"],"
            + "\"attribute\":{\"id\":\"minecraft:generic.max_health\",\"operation\":\"add_value\",\"amount\":2}}";
    private static final String RULE = "{\"id\":\"S-B\",\"name\":\"B\",\"cost\":1,\"kinds\":[\"rule\"],\"params\":{\"range\":6}}";
    private static final String TENSURA = "{\"id\":\"S-C\",\"name\":\"C\",\"cost\":3,\"kinds\":[\"tensura\"],\"tensura_skill\":\"tensura:blood_mist\"}";

    private static SkillTable.Parsed parse(Map<String, JsonElement> skills, Map<String, JsonElement> trees) {
        return SkillTable.parse(skills, trees, mod -> true);
    }

    @Test
    void parsesSkillsAndTrees() {
        SkillTable.Parsed p = parse(skills("s-a", HEALTH, "s-b", RULE, "s-c", TENSURA), Map.of("vampire",
                json("{\"skills\":[{\"skill\":\"S-A\",\"rung\":1},{\"skill\":\"S-B\",\"rung\":2,\"after\":\"S-A\",\"quest\":\"Q-X\"}]}")));
        assertEquals(List.of(), p.rejected());
        SkillSpec a = p.table().skill("S-A").orElseThrow();
        assertEquals(2, a.cost());
        assertEquals("minecraft:generic.max_health", a.attr().attribute());
        assertEquals(SkillSpec.When.ALWAYS, a.attr().when());
        assertEquals(6.0, p.table().skill("S-B").orElseThrow().param("range", 0));
        assertEquals("tensura:blood_mist", p.table().skill("S-C").orElseThrow().tensuraSkill());
        SkillTree.Entry b = p.table().tree(SupernaturalRace.VAMPIRE).entry("S-B").orElseThrow();
        assertEquals(2, b.rung());
        assertEquals("S-A", b.after());
        assertEquals("Q-X", b.quest());
        assertTrue(p.table().tree(SupernaturalRace.WITCH).entries().isEmpty());
    }

    @Test
    void aBadSkillIsRejectedAloneWithOneLine() {
        String[][] bad = {
                {"{\"id\":\"S-X\",\"cost\":1,\"kinds\":[\"magic\"]}", "unknown kind"},
                {"{\"id\":\"S-X\",\"cost\":0,\"kinds\":[\"rule\"]}", "cost"},
                {"{\"id\":\"S-X\",\"cost\":1,\"kinds\":[]}", "kinds"},
                {"{\"id\":\"S-X\",\"cost\":1,\"kinds\":[\"attr\"]}", "attribute"},
                {"{\"id\":\"S-X\",\"cost\":1,\"kinds\":[\"attr\"],\"attribute\":{\"id\":\"a:b\",\"operation\":\"mul\",\"amount\":1}}", "operation"},
                {"{\"id\":\"S-X\",\"cost\":1,\"kinds\":[\"attr\"],\"attribute\":{\"id\":\"a:b\",\"operation\":\"add_value\",\"amount\":1,\"when\":\"noon\"}}", "when"},
                {"{\"id\":\"S-X\",\"cost\":1,\"kinds\":[\"tensura\"]}", "tensura_skill"},
                {"{\"id\":\"S-X\",\"cost\":1,\"kinds\":[\"rule\"],\"params\":{\"n\":\"six\"}}", "number"},
                {"{\"id\":\"S-X\",\"cost\":1,\"kinds\":[\"rule\"],\"params\":{\"n\":1e999}}", "number"},
                {"{\"cost\":1,\"kinds\":[\"rule\"]}", "id"},
                {"{\"id\":\"S-X\",\"cost\":1,\"granted_only\":true,\"kinds\":[\"rule\"]}", "granted"},
                {"[]", "not an object"},
        };
        for (String[] b : bad) {
            SkillTable.Parsed p = parse(skills("bad", b[0], "s-b", RULE), Map.of());
            assertEquals(1, p.rejected().size(), b[0] + " -> " + p.rejected());
            assertTrue(p.rejected().get(0).contains(b[1]), b[0] + " -> " + p.rejected());
            assertTrue(p.table().skill("S-B").isPresent(), "the good skill was lost with " + b[0]);
        }
    }

    @Test
    void aGrantedOnlySkillPaysNothingAndIsNeverInATree() {
        String granted = "{\"id\":\"S-G\",\"name\":\"G\",\"cost\":0,\"granted_only\":true,\"kinds\":[\"rule\"]}";
        SkillTable.Parsed p = parse(skills("s-g", granted), Map.of());
        assertEquals(List.of(), p.rejected());
        assertTrue(p.table().skill("S-G").orElseThrow().grantedOnly());
        SkillTable.Parsed listed = parse(skills("s-g", granted), Map.of("witch", json("{\"skills\":[{\"skill\":\"S-G\",\"rung\":1}]}")));
        assertEquals(1, listed.rejected().size());
        assertTrue(listed.table().tree(SupernaturalRace.WITCH).entries().isEmpty());
    }

    @Test
    void duplicateSkillIdsAreRejected() {
        SkillTable.Parsed p = parse(skills("one", RULE, "two", RULE), Map.of());
        assertEquals(1, p.rejected().size());
        assertTrue(p.rejected().get(0).contains("duplicate"));
        assertTrue(p.table().skill("S-B").isPresent());
    }

    @Test
    void aBadTreeIsRejectedWhole() {
        Map<String, JsonElement> s = skills("s-a", HEALTH, "s-b", RULE);
        String[] bad = {
                "{\"skills\":[{\"skill\":\"S-NOPE\",\"rung\":1}]}",
                "{\"skills\":[{\"skill\":\"S-A\",\"rung\":1},{\"skill\":\"S-A\",\"rung\":2}]}",
                "{\"skills\":[{\"skill\":\"S-A\",\"rung\":0}]}",
                "{\"skills\":[{\"skill\":\"S-A\"}]}",
                "{\"skills\":[{\"skill\":\"S-A\",\"rung\":1,\"after\":\"S-B\"},{\"skill\":\"S-B\",\"rung\":1}]}", // after must come first
                "{\"skills\":[{\"skill\":\"S-A\",\"rung\":1,\"after\":\"S-A\"}]}",
                "{\"skills\":[{\"skill\":\"S-A\",\"rung\":1,\"after\":\"S-Z\"}]}",
                "{}",
        };
        for (String t : bad) {
            SkillTable.Parsed p = parse(s, Map.of("vampire", json(t), "witch", json("{\"skills\":[{\"skill\":\"S-A\",\"rung\":1}]}")));
            assertEquals(1, p.rejected().size(), t + " -> " + p.rejected());
            assertTrue(p.table().tree(SupernaturalRace.VAMPIRE).entries().isEmpty(), t);
            assertEquals(1, p.table().tree(SupernaturalRace.WITCH).entries().size());
        }
        assertEquals(1, parse(s, Map.of("dragon", json("{\"skills\":[]}"))).rejected().size());
    }

    @Test
    void withoutTensuraTheSkillAndWhatNeedsItAreHidden() {
        Map<String, JsonElement> s = skills("s-a", HEALTH, "s-c", TENSURA);
        Map<String, JsonElement> t = Map.of("vampire", json("{\"skills\":[{\"skill\":\"S-C\",\"rung\":1},"
                + "{\"skill\":\"S-A\",\"rung\":2,\"after\":\"S-C\"}]}"));
        SkillTable.Parsed without = SkillTable.parse(s, t, mod -> !mod.equals("tensura"));
        assertEquals(List.of(), without.rejected());
        assertFalse(without.table().skill("S-C").isPresent());
        assertTrue(without.table().skill("S-A").isPresent());
        assertTrue(without.table().tree(SupernaturalRace.VAMPIRE).entries().isEmpty(), "a skill after a hidden one stays reachable");
        assertEquals(2, parse(s, t).table().tree(SupernaturalRace.VAMPIRE).entries().size());
    }
}
