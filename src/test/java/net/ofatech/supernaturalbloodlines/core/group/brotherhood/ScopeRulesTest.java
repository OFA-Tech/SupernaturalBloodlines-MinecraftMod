package net.ofatech.supernaturalbloodlines.core.group.brotherhood;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import org.junit.jupiter.api.Test;

class ScopeRulesTest {
    @Test
    void theTruthTable() {
        int trues = 0;
        for (boolean brotherhood : new boolean[] {false, true}) {
            for (boolean perks : new boolean[] {false, true}) {
                for (boolean fits : new boolean[] {false, true}) {
                    GroupType type = brotherhood ? GroupType.BROTHERHOOD : GroupType.COVEN;
                    boolean answer = ScopeRules.appliesTo(type, perks, fits);
                    assertEquals(brotherhood && perks && fits, answer, type + " " + perks + " " + fits);
                    trues += answer ? 1 : 0;
                }
            }
        }
        assertEquals(1, trues);
    }

    @Test
    void everyOtherTypeIsFalse() {
        for (GroupType t : new GroupType[] {GroupType.PACK, GroupType.CLAN, GroupType.COVEN}) {
            assertFalse(ScopeRules.appliesTo(t, true, true), t.name());
        }
    }
}