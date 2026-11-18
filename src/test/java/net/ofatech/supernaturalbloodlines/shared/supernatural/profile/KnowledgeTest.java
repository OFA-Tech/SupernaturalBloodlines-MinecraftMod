package net.ofatech.supernaturalbloodlines.shared.supernatural.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The knowledge table of the hub IA (3.7), written out again row by row as the document has it: a cell that changes in the code turns a test red. */
class KnowledgeTest {
    // columns: NAME SPECIES RANK LEVEL PACK PACK_DAYS PRESENCE SIRE LINE GENES ANCHOR BONUS
    private static final Map<Relation, String> DOC = new EnumMap<>(Relation.class);

    static {
        DOC.put(Relation.SELF,             "K K K K K K K K D D K K");
        DOC.put(Relation.SAME_PACK,        "K K K K K K K K D D K K");
        DOC.put(Relation.ALLY_LEADER,      "K K K ? K K K ? ? ? ? ?");
        DOC.put(Relation.DIRECT_SIRE,      "K K ? K D D K K K ? ? ?");
        DOC.put(Relation.DIRECT_SIRED,     "K K ? K ? ? K K K ? ? ?");
        DOC.put(Relation.OTHER_IN_LINE,    "K K ? ? ? ? ? ? ? ? ? ?");
        DOC.put(Relation.NPC_CREATURE,     "K K ? ? ? ? ? ? ? ? ? ?");
        DOC.put(Relation.NPC_ANCESTOR,     "D ? ? ? ? ? ? ? ? ? ? ?");
        DOC.put(Relation.STRANGER,         "? ? ? ? ? ? ? ? ? ? ? ?");
        DOC.put(Relation.STRANGER_OFFLINE, "? ? ? ? ? ? ? ? ? ? ? ?");
        DOC.put(Relation.OP_VIEW,          "K K K K K K K K K K K K");
    }

    private static Visibility cell(String letter) {
        return switch (letter) {
            case "K" -> Visibility.KNOWN;
            case "D" -> Visibility.DISCOVERED;
            default -> Visibility.UNKNOWN;
        };
    }

    @Test
    void everyRowOfTheTableIsThereAndEveryCellMatchesTheDocument() {
        assertEquals(EnumSet.allOf(Relation.class), DOC.keySet(), "a relation with no row in the document");
        for (Relation r : Relation.values()) {
            String[] row = DOC.get(r).split(" ");
            assertEquals(Fact.values().length, row.length, r + " has the wrong number of cells");
            for (Fact f : Fact.values()) {
                assertEquals(cell(row[f.ordinal()]), Knowledge.visible(r, f), r + " / " + f);
            }
        }
    }

    @Test
    void theSelfRowKeepsOnlyTheLineAndTheGenesBehindDiscovery() {
        for (Fact f : Fact.values()) {
            boolean secret = f == Fact.LINE || f == Fact.GENES;
            assertEquals(secret ? Visibility.DISCOVERED : Visibility.KNOWN, Knowledge.visible(Relation.SELF, f), f.name());
        }
    }

    @Test
    void aStrangerKnowsNothing() {
        // the client already has a stranger's name and online dot from its own player list, so the table has no exception to make
        for (Fact f : Fact.values()) {
            assertEquals(Visibility.UNKNOWN, Knowledge.visible(Relation.STRANGER, f), f.name());
        }
    }

    @Test
    void whatEachAbilityTeaches() {
        assertEquals(EnumSet.of(Fact.LINE, Fact.GENES), Knowledge.learnedBy(Knowledge.Ability.REVELATION));
        assertEquals(EnumSet.of(Fact.GENES), Knowledge.learnedBy(Knowledge.Ability.LINEAGE_REVEAL));
        assertEquals(EnumSet.of(Fact.GENES), Knowledge.learnedBy(Knowledge.Ability.DOPPELGANGER_TAGLOCK));
        assertEquals(EnumSet.of(Fact.SIRE, Fact.GENES), Knowledge.learnedBy(Knowledge.Ability.MEMORY_CLAWS));
        for (Knowledge.Ability a : Knowledge.Ability.values()) {
            assertTrue(!Knowledge.learnedBy(a).contains(Fact.SPECIES) && !Knowledge.learnedBy(a).contains(Fact.NAME), a + " must not teach identity");
        }
    }
}

