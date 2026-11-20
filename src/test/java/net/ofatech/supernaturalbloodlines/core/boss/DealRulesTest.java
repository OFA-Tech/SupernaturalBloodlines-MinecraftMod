package net.ofatech.supernaturalbloodlines.core.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
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
    void theOptionsAreEveryNpcOriginalButElijahTheSameForEverybody() {
        List<NpcOriginal> all = DealRules.options();
        assertEquals(List.of(NpcOriginal.MIKAEL, NpcOriginal.FINN, NpcOriginal.KOL, NpcOriginal.REBEKAH, NpcOriginal.KLAUS), all);
        assertFalse(all.contains(NpcOriginal.ELIJAH), "never his own blood");
        assertTrue(all.contains(NpcOriginal.KLAUS), "leaving one out would tell a player which line they are in");
    }

    @Test
    void anAnswerIsAnIdThatIsOnTheOfferAndNothingElse() {
        assertEquals(NpcOriginal.KOL, DealRules.choose("kol").orElseThrow());
        assertTrue(DealRules.choose("elijah").isEmpty(), "his own blood is not on offer");
        assertTrue(DealRules.choose("").isEmpty());
        assertTrue(DealRules.choose("nobody").isEmpty());
        assertTrue(DealRules.choose("KOL").isEmpty(), "the id is exact");
    }
}