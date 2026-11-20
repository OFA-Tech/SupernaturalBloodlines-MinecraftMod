package net.ofatech.supernaturalbloodlines.core.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcOriginal;
import org.junit.jupiter.api.Test;

class DealRulesTest {
    @Test
    void theOathHoldsThroughItsLastDay() {
        long until = DealRules.sworn(10, 7);
        assertEquals(17, until);
        assertTrue(DealRules.binding(until, 10));
        assertTrue(DealRules.binding(until, 16));
        assertFalse(DealRules.binding(until, 17), "the oath ran out");
    }

    @Test
    void anOathIsAtLeastOneDay() {
        assertEquals(11, DealRules.sworn(10, 0));
        assertEquals(11, DealRules.sworn(10, -3));
    }

    @Test
    void nobodyWhoNeverDealtOrWhoBrokeItIsBound() {
        assertFalse(DealRules.binding(DealRules.NEVER, 0));
        assertFalse(DealRules.binding(DealRules.BROKEN, 0));
        assertFalse(DealRules.binding(DealRules.BROKEN, 1_000_000));
    }

    @Test
    void aPlayerIsOfferedTheDealOnceEver() {
        assertTrue(DealRules.offers(DealRules.NEVER, false, false));
        assertFalse(DealRules.offers(DealRules.sworn(10, 7), false, false), "an oath that binds");
        assertFalse(DealRules.offers(5, false, false), "an oath that ran out is still a deal had");
        assertFalse(DealRules.offers(DealRules.BROKEN, false, false), "a broken oath is never offered again");
    }

    @Test
    void noOfferFromAHostileOrFallenElijah() {
        assertFalse(DealRules.offers(DealRules.NEVER, true, false));
        assertFalse(DealRules.offers(DealRules.NEVER, false, true));
    }

    @Test
    void everyVictimItBreaksOnAndEveryOneItDoesNot() {
        for (BossId b : BossId.values()) {
            boolean expected = switch (b) {
                case ELIJAH, KLAUS, KOL, REBEKAH, FINN -> true;
                case MIKAEL, DEUCALION, PETER_HALE, BEAST_OF_GEVAUDAN -> false;
            };
            assertEquals(expected, DealRules.breaks(b), b.id());
        }
    }

    @Test
    void anOathIsBrokenOnlyWhileItBinds() {
        long until = DealRules.sworn(10, 7);
        assertTrue(DealRules.broken(until, 12, BossId.KLAUS));
        assertTrue(DealRules.broken(until, 12, BossId.ELIJAH), "attacking Elijah breaks it");
        assertFalse(DealRules.broken(until, 12, BossId.MIKAEL), "Mikael is the one exception");
        assertFalse(DealRules.broken(until, 17, BossId.KLAUS), "after it ran out");
        assertFalse(DealRules.broken(DealRules.NEVER, 12, BossId.KLAUS), "never sworn");
        assertFalse(DealRules.broken(DealRules.BROKEN, 12, BossId.KLAUS), "already broken");
    }

    @Test
    void theOptionsAreEveryNpcOriginalButElijahAndTheOwnUndiscoveredOne() {
        UUID klaus = NpcOriginal.KLAUS.uuid();
        List<NpcOriginal> all = DealRules.options(null, null, false);
        assertEquals(List.of(NpcOriginal.MIKAEL, NpcOriginal.FINN, NpcOriginal.KOL, NpcOriginal.REBEKAH, NpcOriginal.KLAUS), all);
        assertFalse(all.contains(NpcOriginal.ELIJAH), "never his own blood");
        assertFalse(DealRules.options(klaus, null, false).contains(NpcOriginal.KLAUS), "the vial would name the line they are in");
        assertTrue(DealRules.options(klaus, klaus, false).contains(NpcOriginal.KLAUS), "discovered, so no secret");
        assertTrue(DealRules.options(klaus, null, true).contains(NpcOriginal.KLAUS), "every Original is shown to everybody");
        assertEquals(all, DealRules.options(UUID.randomUUID(), null, false), "a line that is no NPC's takes nothing away");
    }
}