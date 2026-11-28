package net.ofatech.supernaturalbloodlines.parts.speciestensura;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for HumanLikeSet admin mutations. The dynamic set itself requires a running server
 * (GameTests cover that); these tests verify the add/remove/invalidate contract in isolation.
 */
class HumanLikeSetTest {
    @AfterEach
    void cleanup() {
        HumanLikeSet.adminRemove("testmod:test_race");
        HumanLikeSet.adminRemove("testmod:another_race");
    }

    @Test
    void adminAddMakesRaceAppearInCurrentSet() {
        HumanLikeSet.adminAdd("testmod:test_race");
        assertTrue(HumanLikeSet.current().contains("testmod:test_race"));
    }

    @Test
    void adminRemoveTakesRaceOutOfCurrentSet() {
        HumanLikeSet.adminAdd("testmod:test_race");
        HumanLikeSet.adminRemove("testmod:test_race");
        assertFalse(HumanLikeSet.current().contains("testmod:test_race"));
    }

    @Test
    void invalidateForcesRebuildOnNextAccess() {
        HumanLikeSet.adminAdd("testmod:test_race");
        var first = HumanLikeSet.current();
        HumanLikeSet.invalidate();
        var second = HumanLikeSet.current();
        assertTrue(second.contains("testmod:test_race"));
        assertNotSame(first, second);
    }

    @Test
    void adminAddOverridesPreviousRemove() {
        HumanLikeSet.adminAdd("testmod:test_race");
        HumanLikeSet.adminRemove("testmod:test_race");
        HumanLikeSet.adminAdd("testmod:test_race");
        assertTrue(HumanLikeSet.current().contains("testmod:test_race"));
    }
}