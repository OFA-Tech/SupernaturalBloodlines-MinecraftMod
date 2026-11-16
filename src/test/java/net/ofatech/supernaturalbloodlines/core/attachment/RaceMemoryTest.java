package net.ofatech.supernaturalbloodlines.core.attachment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class RaceMemoryTest {
    @Test
    void aCureRestoresTheRaceWeReplaced() {
        RaceMemory m = RaceMemory.NONE.afterChange("tensura:human", "tensura:beastfolk");
        assertEquals(Optional.of("tensura:human"), m.restore("tensura:beastfolk"));
    }

    @Test
    void aChainOfOurChangesKeepsTheRaceFromBeforeTheFirst() {
        RaceMemory m = RaceMemory.NONE.afterChange("tensura:human", "tensura:beastfolk").afterChange("tensura:beastfolk", "tensura:beast_lord")
                .afterChange("tensura:beast_lord", "tensura:spirit_beast");
        assertEquals(Optional.of("tensura:human"), m.restore("tensura:spirit_beast"));
    }

    @Test
    void aRaceThePlayerEvolvedToThemselvesIsLeftAlone() {
        RaceMemory m = RaceMemory.NONE.afterChange("tensura:human", "tensura:beastfolk");
        assertEquals(Optional.empty(), m.restore("tensura:divine_beast"));
        assertEquals(Optional.empty(), m.restore(null));
    }

    @Test
    void aPlayerWhoChangedAndWasMovedAgainRecordsTheNewStart() {
        RaceMemory m = RaceMemory.NONE.afterChange("tensura:human", "tensura:beastfolk").afterChange("tensura:elf", "tensura:beastfolk");
        assertEquals(Optional.of("tensura:elf"), m.restore("tensura:beastfolk"));
    }

    @Test
    void nothingRecordedRestoresNothing() {
        assertEquals(Optional.empty(), RaceMemory.NONE.restore("tensura:human"));
    }
}
