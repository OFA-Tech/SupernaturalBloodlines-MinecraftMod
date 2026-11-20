package net.ofatech.supernaturalbloodlines.core.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class LinkRulesTest {
    @Test
    void exactlyTwoCreaturesNeitherAnOriginalNorLinkedAlreadyMakeALink() {
        assertEquals(Optional.empty(), LinkRules.refusal(2, false, false));
        assertEquals(Optional.of(LinkRules.NEEDS_TWO), LinkRules.refusal(0, false, false));
        assertEquals(Optional.of(LinkRules.NEEDS_TWO), LinkRules.refusal(1, false, false));
        assertEquals(Optional.of(LinkRules.NEEDS_TWO), LinkRules.refusal(3, false, false));
    }

    @Test
    void anOriginalAndAnAlreadyLinkedCreatureGetTheSameAnswerSoARefusalNeverNamesAnOriginal() {
        assertEquals(Optional.of(LinkRules.WONT_HOLD), LinkRules.refusal(2, true, false));
        assertEquals(Optional.of(LinkRules.WONT_HOLD), LinkRules.refusal(2, false, true));
        assertEquals(Optional.of(LinkRules.WONT_HOLD), LinkRules.refusal(2, true, true));
        assertEquals(Optional.of(LinkRules.NEEDS_TWO), LinkRules.refusal(3, true, true), "the count is checked first");
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
    void aLinkRunsUntilItsTickAndNotOnePast() {
        assertTrue(LinkRules.active(100, 99));
        assertFalse(LinkRules.active(100, 100));
    }
}