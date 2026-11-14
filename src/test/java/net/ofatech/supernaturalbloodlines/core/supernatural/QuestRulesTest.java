package net.ofatech.supernaturalbloodlines.core.supernatural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class QuestRulesTest {
    private static JsonElement json(String s) {
        return JsonParser.parseString(s);
    }

    private static final String QUEST = "{\"id\":\"Q-X\",\"name\":\"X\",\"tasks\":["
            + "{\"id\":\"Q-Xa\",\"detector\":\"kill\",\"category\":\"vampire\",\"n\":3},"
            + "{\"id\":\"Q-Xb\",\"detector\":\"signal\",\"signal\":\"s\",\"n\":2,\"part\":\"species\"},"
            + "{\"id\":\"Q-Xc\",\"detector\":\"feed\",\"n\":2}]}";

    private static SkillTable table(java.util.function.Predicate<String> parts) {
        return SkillTable.parse(Map.of("s-x", json("{\"id\":\"S-X\",\"cost\":1,\"kinds\":[\"rule\"]}")),
                Map.of("vampire", json("{\"skills\":[{\"skill\":\"S-X\",\"rung\":2,\"quest\":\"Q-X\"}]}")),
                Map.of("q-x", json(QUEST)), mod -> true, parts).table();
    }

    private static final List<SupernaturalRace> VAMPIRE = List.of(SupernaturalRace.VAMPIRE);

    private static SupernaturalState at(int rung) {
        return SupernaturalState.NONE.toBuilder().rung(SupernaturalRace.VAMPIRE, rung).build();
    }

    private static TaskEvent kill() {
        return TaskEvent.of(TaskSpec.Detector.KILL, 1, "category", "vampire");
    }

    @Test
    void aQuestOpensWhenTheTreesRungIsReached() {
        SkillTable t = table(p -> true);
        assertEquals(List.of(), QuestRules.open(t, at(1), VAMPIRE));
        assertEquals(List.of("Q-X"), QuestRules.open(t, at(2), VAMPIRE).stream().map(QuestSpec::id).toList());
        assertEquals(at(1), QuestRules.apply(t, at(1), VAMPIRE, kill()), "an event counted before the quest opened");
        assertEquals(List.of(), QuestRules.open(t, at(5).toBuilder().quests(Set.of("Q-X")).build(), VAMPIRE), "a finished quest opened again");
        assertEquals(List.of(), QuestRules.open(t, at(5), List.of(SupernaturalRace.WITCH)), "another race's quest opened");
    }

    @Test
    void everyTaskMustBeDoneAndTheRecordsGoWhenItIsFinished() {
        SkillTable t = table(p -> true);
        SupernaturalState s = at(2);
        for (int i = 0; i < 3; i++) {
            s = QuestRules.apply(t, s, VAMPIRE, kill());
        }
        assertTrue(s.done().contains("Q-Xa") && s.quests().isEmpty(), "one task done is not the quest");
        s = QuestRules.apply(t, s, VAMPIRE, TaskEvent.of(TaskSpec.Detector.SIGNAL, 5, "signal", "s"));
        s = QuestRules.apply(t, s, VAMPIRE, TaskEvent.of(TaskSpec.Detector.FEED, 1));
        assertEquals(1L, s.counters().get("Q-Xc"));
        s = QuestRules.apply(t, s, VAMPIRE, TaskEvent.of(TaskSpec.Detector.FEED, 1));
        assertEquals(Set.of("Q-X"), s.quests());
        assertTrue(s.done().isEmpty() && s.counters().isEmpty(), "task records stayed: " + s.done() + s.counters());
        assertEquals(s, QuestRules.apply(t, s, VAMPIRE, kill()), "a finished quest counted again");
        assertEquals(0, s.sp(), "a quest pays nothing");
    }

    @Test
    void aTaskWhosePartIsOffIsDroppedAndAQuestWithNoTasksFinishesAtOnce() {
        SkillTable t = table(p -> !p.equals("species"));
        SupernaturalState s = at(2);
        for (int i = 0; i < 3; i++) {
            s = QuestRules.apply(t, s, VAMPIRE, kill());
        }
        s = QuestRules.apply(t, s, VAMPIRE, TaskEvent.of(TaskSpec.Detector.FEED, 2));
        assertEquals(Set.of("Q-X"), s.quests(), "the quest needs a signal nobody can fire");
        SkillTable empty = SkillTable.parse(Map.of("s-x", json("{\"id\":\"S-X\",\"cost\":1,\"kinds\":[\"rule\"]}")),
                Map.of("vampire", json("{\"skills\":[{\"skill\":\"S-X\",\"rung\":2,\"quest\":\"Q-Y\"}]}")),
                Map.of("q-y", json("{\"id\":\"Q-Y\",\"tasks\":[{\"id\":\"Q-Ya\",\"detector\":\"feed\",\"part\":\"gone\"}]}")), mod -> true, p -> !p.equals("gone")).table();
        assertEquals(Set.of("Q-Y"), QuestRules.settle(empty, at(2), VAMPIRE).quests());
    }

    @Test
    void aGhoulRecordsNothing() {
        SkillTable t = table(p -> true);
        List<SupernaturalRace> ghoul = List.of(SupernaturalRace.GHOUL, SupernaturalRace.VAMPIRE);
        assertEquals(at(2), QuestRules.apply(t, at(2), ghoul, kill()));
        assertEquals(at(2), QuestRules.settle(t, at(2), ghoul));
    }

    @Test
    void aBadQuestIsRejectedAlone() {
        String[] bad = {"{\"id\":\"Q-Z\",\"tasks\":[]}", "{\"id\":\"Q-Z\"}",
                "{\"id\":\"Q-Z\",\"tasks\":[{\"id\":\"a\",\"any_of\":[{\"id\":\"b\",\"detector\":\"feed\"}]}]}",
                "{\"id\":\"Q-Z\",\"tasks\":[{\"id\":\"a\",\"detector\":\"feed\",\"total\":true}]}",
                "{\"id\":\"Q-Z\",\"tasks\":[{\"id\":\"a\",\"detector\":\"nope\"}]}",
                "{\"id\":\"Q-Z\",\"tasks\":[{\"id\":\"a\",\"detector\":\"feed\"},{\"id\":\"a\",\"detector\":\"feed\"}]}",
                "{\"id\":\"Q-X\",\"tasks\":[{\"id\":\"z\",\"detector\":\"feed\"}]}"};
        for (String b : bad) {
            SkillTable.Parsed p = SkillTable.parse(Map.of(), Map.of(), Map.of("good", json(QUEST), "bad", json(b)), m -> true, x -> true);
            assertEquals(1, p.rejected().size(), b + " -> " + p.rejected());
            assertTrue(p.table().quest("Q-X").isPresent(), "the good quest was lost with " + b);
            assertFalse(p.table().quest("Q-Z").isPresent());
        }
    }
}