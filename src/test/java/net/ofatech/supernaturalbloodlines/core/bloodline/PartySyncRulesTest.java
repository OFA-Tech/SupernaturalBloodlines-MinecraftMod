package net.ofatech.supernaturalbloodlines.core.bloodline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.ofatech.supernaturalbloodlines.core.bloodline.PartySyncRules.Party;
import net.ofatech.supernaturalbloodlines.core.bloodline.PartySyncRules.Plan;
import org.junit.jupiter.api.Test;

class PartySyncRulesTest {
    private static final UUID LEADER = new UUID(0, 1);
    private static final UUID ALPHA2 = new UUID(0, 2);
    private static final UUID BETA = new UUID(0, 3);
    private static final UUID MATE = new UUID(0, 4);
    private static final Set<UUID> PACK = Set.of(LEADER, ALPHA2, BETA, MATE);
    private static final Set<UUID> ALPHAS = Set.of(LEADER, ALPHA2);

    @Test
    void aPackWithNoPartyGetsOneAndEveryoneIsAddedTheOtherAlphasAsAdmins() {
        Plan p = PartySyncRules.plan(LEADER, PACK, ALPHAS, null, Set.of(), true);
        assertTrue(p.create());
        assertEquals(Boolean.TRUE, p.add().get(ALPHA2));
        assertEquals(Boolean.FALSE, p.add().get(BETA));
        assertEquals(Boolean.FALSE, p.add().get(MATE));
    }

    @Test
    void aLonePackLeaderMakesNoParty() {
        assertFalse(PartySyncRules.plan(LEADER, Set.of(LEADER), ALPHAS, null, Set.of(), true).create());
    }

    @Test
    void aPartyTheOwnerDisbandedIsNotMadeAgainUntilTheRosterChanges() {
        Plan quiet = PartySyncRules.plan(LEADER, PACK, ALPHAS, null, Set.of(BETA, MATE), false);
        assertFalse(quiet.create());
        assertEquals(Set.of(LEADER), quiet.ours(), "the mirror says: ended");
        assertFalse(PartySyncRules.plan(LEADER, PACK, ALPHAS, null, quiet.ours(), false).create(), "and stays quiet on the next pass");
        assertTrue(PartySyncRules.plan(LEADER, PACK, ALPHAS, null, quiet.ours(), true).create(), "until the roster changes");
        assertTrue(PartySyncRules.plan(LEADER, PACK, ALPHAS, null, Set.of(), false).create(), "a pack that never had one gets it");
    }

    @Test
    void aMemberWhoLeftThePartyLeavesThePack() {
        Party party = new Party(LEADER, Set.of(LEADER, ALPHA2, MATE));
        Plan p = PartySyncRules.plan(LEADER, PACK, ALPHAS, party, Set.of(ALPHA2, BETA, MATE), false);
        assertEquals(Set.of(BETA), p.leavePack());
        assertFalse(p.ours().contains(BETA));
        assertTrue(p.add().isEmpty(), "and the Beta is not added again");
    }

    @Test
    void aMemberWhoLeftThePackLeavesTheParty() {
        Party party = new Party(LEADER, Set.of(LEADER, ALPHA2, BETA, MATE));
        Plan p = PartySyncRules.plan(LEADER, Set.of(LEADER, ALPHA2, MATE), ALPHAS, party, Set.of(ALPHA2, BETA, MATE), false);
        assertEquals(Set.of(BETA), p.removeFrom());
        assertFalse(p.ours().contains(BETA));
    }

    @Test
    void aNewPackMemberIsAddedAndMirrored() {
        Party party = new Party(LEADER, Set.of(LEADER));
        Plan p = PartySyncRules.plan(LEADER, PACK, ALPHAS, party, Set.of(), true);
        assertEquals(Map.of(ALPHA2, true, BETA, false, MATE, false), p.add());
        assertEquals(Set.of(ALPHA2, BETA, MATE), p.ours());
    }

    @Test
    void anOwnerWhoLeftThePackDisbandsTheirPartyAndThePackGetsItsOwn() {
        Party party = new Party(new UUID(9, 9), Set.of(new UUID(9, 9), BETA));
        Plan p = PartySyncRules.plan(LEADER, PACK, ALPHAS, party, Set.of(BETA), false);
        assertTrue(p.disband());
        assertTrue(p.create());
    }

    @Test
    void aPartyMemberWeNeverAddedIsLeftAlone() {
        Party party = new Party(LEADER, Set.of(LEADER, new UUID(7, 7)));
        Plan p = PartySyncRules.plan(LEADER, PACK, ALPHAS, party, Set.of(), false);
        assertTrue(p.removeFrom().isEmpty());
        assertTrue(p.leavePack().isEmpty());
    }

    @Test
    void theFoundingOwnerStaysEvenWhenAnotherAlphaLeadsNow() {
        Party party = new Party(LEADER, Set.of(LEADER, ALPHA2));
        Plan p = PartySyncRules.plan(ALPHA2, PACK, ALPHAS, party, Set.of(ALPHA2), false);
        assertFalse(p.create() || p.disband());
        assertTrue(p.leavePack().isEmpty());
    }
}
