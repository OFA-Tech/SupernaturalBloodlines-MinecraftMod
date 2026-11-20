package net.ofatech.supernaturalbloodlines.core.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.ofatech.supernaturalbloodlines.core.boss.BossRules.Event;
import org.junit.jupiter.api.Test;

class BossRulesTest {
    @Test
    void unseenIsSpawnedIntoActive() {
        assertEquals(BossState.ACTIVE, BossRules.next(BossState.UNSEEN, Event.SPAWNED));
    }

    @Test
    void activeIsDesiccatedIntoOpenAndOpenReturnsToActive() {
        assertEquals(BossState.OPEN, BossRules.next(BossState.ACTIVE, Event.DESICCATED));
        assertEquals(BossState.ACTIVE, BossRules.next(BossState.OPEN, Event.RETURNED));
    }

    @Test
    void anActiveOrOpenBossThatDiesIsDeadForGood() {
        assertEquals(BossState.DEAD, BossRules.next(BossState.ACTIVE, Event.KILLED));
        assertEquals(BossState.DEAD, BossRules.next(BossState.OPEN, Event.KILLED));
    }

    @Test
    void aBossWhoseEntityIsGoneIsLostAndMayBeBroughtBack() {
        assertEquals(BossState.LOST, BossRules.next(BossState.ACTIVE, Event.LOST));
        assertEquals(BossState.LOST, BossRules.next(BossState.OPEN, Event.LOST));
        assertEquals(BossState.ACTIVE, BossRules.next(BossState.LOST, Event.SPAWNED));
    }

    @Test
    void deadStaysDeadWhateverHappens() {
        for (Event e : Event.values()) {
            if (e != Event.FORGET) {
                assertEquals(BossState.DEAD, BossRules.next(BossState.DEAD, e), e.name());
            }
        }
    }

    @Test
    void forgetResetsEveryState() {
        for (BossState s : BossState.values()) {
            assertEquals(BossState.UNSEEN, BossRules.next(s, Event.FORGET), s.name());
        }
    }

    @Test
    void anEventThatMeansNothingHereChangesNothing() {
        assertEquals(BossState.UNSEEN, BossRules.next(BossState.UNSEEN, Event.KILLED));
        assertEquals(BossState.ACTIVE, BossRules.next(BossState.ACTIVE, Event.RETURNED));
        assertEquals(BossState.OPEN, BossRules.next(BossState.OPEN, Event.DESICCATED));
    }
}
