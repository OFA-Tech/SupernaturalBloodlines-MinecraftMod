package net.ofatech.supernaturalbloodlines.core.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LinkRulesTest {
    @Test
    void exactlyTwoCreaturesWithNothingWrongMakeALink() {
        assertFalse(LinkRules.refuses(2, false, false, false, false));
    }

    @Test
    void everyReasonRefusesAndTheCountMustBeExactlyTwo() {
        assertTrue(LinkRules.refuses(0, false, false, false, false), "none");
        assertTrue(LinkRules.refuses(1, false, false, false, false), "one");
        assertTrue(LinkRules.refuses(3, false, false, false, false), "three");
        assertTrue(LinkRules.refuses(2, true, false, false, false), "an Original");
        assertTrue(LinkRules.refuses(2, false, true, false, false), "a boss");
        assertTrue(LinkRules.refuses(2, false, false, true, false), "already linked");
        assertTrue(LinkRules.refuses(2, false, false, false, true), "a player who does not agree");
    }

    @Test
    void thereIsOneMessageForAllOfThemAndAnotherForAPerformerWhoIsNotAWitchType() {
        assertEquals("supernaturalbloodlines.refusal.link_wont_hold", LinkRules.WONT_HOLD);
        assertTrue(!LinkRules.WONT_HOLD.equals(LinkRules.NOT_WITCH));
    }

    @Test
    void aPlayerAgreesByPerformingOrBySneakingWithALeadWhereTheyMayFight() {
        assertTrue(LinkRules.consents(true, false, false, false), "the performer always agrees");
        assertTrue(LinkRules.consents(true, true, true, true));
        assertTrue(LinkRules.consents(false, true, true, true), "sneaking with a lead, PvP on");
        assertFalse(LinkRules.consents(false, true, false, true), "sneaking alone is an accident, not a yes");
        assertFalse(LinkRules.consents(false, false, true, true), "holding a lead and standing there is not agreeing");
        assertFalse(LinkRules.consents(false, true, true, false), "PvP is off: no other player is linked");
        assertFalse(LinkRules.consents(false, false, false, false));
    }

    @Test
    void theShareIsAFractionOfTheDamageOnlyAtDepthZero() {
        assertEquals(2.0F, LinkRules.share(4F, 0, 0.5), 1e-6);
        assertEquals(0F, LinkRules.share(4F, 1, 0.5), "a share is never shared again");
        assertEquals(0F, LinkRules.share(4F, 2, 0.5));
        assertEquals(0F, LinkRules.share(0F, 0, 0.5));
        assertEquals(0F, LinkRules.share(-3F, 0, 0.5));
    }

    @Test
    void theFractionIsClampedBetweenNothingAndAll() {
        assertEquals(4F, LinkRules.share(4F, 0, 3.0), 1e-6, "more than all is all");
        assertEquals(0F, LinkRules.share(4F, 0, -1.0), 1e-6);
        assertEquals(1F, LinkRules.share(4F, 0, 0.25), 1e-6);
    }

    @Test
    void whatAPartnerOwesAddsUpAndStopsAtTheCap() {
        assertEquals(3F, LinkRules.addPending(0F, 3F));
        assertEquals(5F, LinkRules.addPending(3F, 2F));
        assertEquals(LinkRules.MAX_PENDING, LinkRules.addPending(19F, 50F));
        assertEquals(LinkRules.MAX_PENDING, LinkRules.addPending(LinkRules.MAX_PENDING, 1F));
        assertEquals(2F, LinkRules.addPending(-4F, 2F), "a bad stored value does not subtract");
        assertEquals(3F, LinkRules.addPending(3F, -9F), "a negative share owes nothing");
    }

    @Test
    void aCreatureThatLoadsTakesAtMostHalfItsMaximumHealth() {
        assertEquals(5F, LinkRules.settleCap(20F, 10F), "capped at half");
        assertEquals(3F, LinkRules.settleCap(3F, 10F), "less than half is all of it");
        assertEquals(5F, LinkRules.settleCap(5F, 10F), "exactly half");
        assertEquals(0F, LinkRules.settleCap(-2F, 10F));
        assertEquals(0F, LinkRules.settleCap(8F, 0F));
    }

    @Test
    void aLinkRunsUntilItsTickAndNotOnePast() {
        assertTrue(LinkRules.active(100, 99));
        assertFalse(LinkRules.active(100, 100));
    }
}