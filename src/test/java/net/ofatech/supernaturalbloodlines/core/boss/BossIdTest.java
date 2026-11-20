package net.ofatech.supernaturalbloodlines.core.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.core.bloodline.NpcOriginal;
import org.junit.jupiter.api.Test;

class BossIdTest {
    @Test
    void nineBosses() {
        assertEquals(9, BossId.values().length);
    }

    @Test
    void theSixVampireOriginalsAreNpcRootsTheOtherThreeAreNot() {
        for (BossId b : BossId.values()) {
            boolean vampire = switch (b) {
                case MIKAEL, KLAUS, ELIJAH, KOL, REBEKAH, FINN -> true;
                default -> false;
            };
            assertEquals(vampire, b.npcOriginal().isPresent(), b.id());
        }
    }

    @Test
    void anNpcRootHasTheUuidOfItsOriginal() {
        assertEquals(NpcOriginal.MIKAEL.uuid(), BossId.MIKAEL.npcOriginal().orElseThrow().uuid());
    }

    @Test
    void onlyMikaelKlausElijahAndRebekahHaveLandedInThisBuild() {
        for (BossId b : BossId.values()) {
            assertEquals(b == BossId.MIKAEL || b == BossId.KLAUS || b == BossId.ELIJAH || b == BossId.REBEKAH, b.landed(), b.id());
        }
    }

    @Test
    void foundById() {
        assertEquals(BossId.PETER_HALE, BossId.byId("peter_hale").orElseThrow());
        assertTrue(BossId.byId("nobody").isEmpty());
    }
}
