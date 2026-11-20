package net.ofatech.supernaturalbloodlines.core.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SpawnRulesTest {
    @Test
    void anUnseenEnabledBossWithItsModsMaySpawn() {
        assertTrue(SpawnRules.refusal(new BossFacts(true, true, BossState.UNSEEN, false)).isEmpty());
    }

    @Test
    void aLostBossMayBeBroughtBack() {
        assertTrue(SpawnRules.refusal(new BossFacts(true, true, BossState.LOST, false)).isEmpty());
    }

    @Test
    void aSwitchedOffBossIsRefused() {
        assertEquals(SpawnRules.DISABLED, SpawnRules.refusal(new BossFacts(false, true, BossState.UNSEEN, false)).orElseThrow());
    }

    @Test
    void aBossWhoseModIsMissingIsRefused() {
        assertEquals(SpawnRules.NEEDS_MOD, SpawnRules.refusal(new BossFacts(true, false, BossState.UNSEEN, false)).orElseThrow());
    }

    @Test
    void aDeadBossIsRefused() {
        assertEquals(SpawnRules.DEAD, SpawnRules.refusal(new BossFacts(true, true, BossState.DEAD, false)).orElseThrow());
    }

    @Test
    void aBossThatIsOutThereOrDownIsRefused() {
        assertEquals(SpawnRules.PRESENT, SpawnRules.refusal(new BossFacts(true, true, BossState.ACTIVE, false)).orElseThrow());
        assertEquals(SpawnRules.PRESENT, SpawnRules.refusal(new BossFacts(true, true, BossState.OPEN, false)).orElseThrow());
    }

    @Test
    void aCoolingDirectorRefuses() {
        assertEquals(SpawnRules.COOLING, SpawnRules.refusal(new BossFacts(true, true, BossState.UNSEEN, true)).orElseThrow());
    }

    @Test
    void theReasonsComeInTheDocumentedOrder() {
        assertEquals(SpawnRules.DISABLED, SpawnRules.refusal(new BossFacts(false, false, BossState.DEAD, true)).orElseThrow());
        assertEquals(SpawnRules.NEEDS_MOD, SpawnRules.refusal(new BossFacts(true, false, BossState.DEAD, true)).orElseThrow());
        assertEquals(SpawnRules.DEAD, SpawnRules.refusal(new BossFacts(true, true, BossState.DEAD, true)).orElseThrow());
    }
}
