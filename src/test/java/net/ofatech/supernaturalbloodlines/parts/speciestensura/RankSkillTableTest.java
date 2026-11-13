package net.ofatech.supernaturalbloodlines.parts.speciestensura;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class RankSkillTableTest {
    @Test
    void defaultsMatchTheDesign() {
        RankSkillTable t = RankSkillTable.parse(RankSkillTable.DEFAULTS).table();
        assertEquals(List.of("tensura:beast_transformation", "tensura:godwolf_sense"), t.skillsFor(RankSkillTable.ALPHA));
        assertTrue(t.skillsFor(RankSkillTable.TRUE_ALPHA).containsAll(t.skillsFor(RankSkillTable.ALPHA)));
        assertTrue(t.skillsFor(RankSkillTable.TRUE_ALPHA).contains("tensura:commander"));
        assertEquals(List.of("tensura:godwolf_sense"), t.skillsFor(RankSkillTable.BORN_LEVEL5));
    }

    @Test
    void configOverridesOneKeyAndAddsTheNamespace() {
        RankSkillTable.Parsed p = RankSkillTable.parse(List.of("alpha=charm, tensura:drain"));
        assertEquals(List.of("tensura:charm", "tensura:drain"), p.table().skillsFor(RankSkillTable.ALPHA));
        assertTrue(p.rejected().isEmpty());
        assertTrue(p.table().skillsFor(RankSkillTable.TRUE_ALPHA).contains("tensura:commander"));
    }

    @Test
    void emptyListIsAllowed() {
        assertTrue(RankSkillTable.parse(List.of("ALPHA=")).table().skillsFor(RankSkillTable.ALPHA).isEmpty());
    }

    @Test
    void badEntriesAreRejectedAndTheDefaultStays() {
        RankSkillTable.Parsed p = RankSkillTable.parse(List.of("OMEGA=drain", "ALPHA=bad id", "nonsense", "=drain"));
        assertEquals(4, p.rejected().size());
        assertEquals(List.of("tensura:beast_transformation", "tensura:godwolf_sense"), p.table().skillsFor(RankSkillTable.ALPHA));
    }
}
