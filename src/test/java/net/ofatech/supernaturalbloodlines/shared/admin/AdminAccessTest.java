package net.ofatech.supernaturalbloodlines.shared.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AdminAccessTest {
    @Test
    void belowLevelTwoIsRefused() {
        for (int level : new int[] {-1, 0, 1}) {
            assertFalse(AdminAccess.allowed(level), "level " + level);
        }
    }

    @Test
    void levelTwoAndUpIsAllowed() {
        assertEquals(2, AdminAccess.OP_LEVEL);
        for (int level : new int[] {2, 3, 4}) {
            assertTrue(AdminAccess.allowed(level), "level " + level);
        }
    }
}
