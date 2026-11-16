package net.ofatech.supernaturalbloodlines.core.bloodline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void aPackWithNoRecordedPartyGetsOneForItsLeaderAndEveryoneElseIsAdded() {
        Plan p = PartySyncRules.plan(LEADER, PACK, null, false, Set.of(), false, false);
        assertTrue(p.create());
        assertEquals(Set.of(ALPHA2, BETA, MATE), p.add());
        assertFalse(p.abandon() || p.forget());
    }

    @Test
    void aLonePackLeaderMakesNoParty() {
        assertFalse(PartySyncRules.plan(LEADER, Set.of(LEADER), null, false, Set.of(), false, true).create());
    }

    @Test
    void aPartyThatWasEndedIsForgottenAndNotMadeAgainUntilTheRosterChanges() {
        Plan gone = PartySyncRules.plan(LEADER, PACK, null, true, Set.of(BETA), false, false);
        assertTrue(gone.forget());
        assertFalse(gone.create(), "the owner ended it: it does not come back by itself");
        assertFalse(PartySyncRules.plan(LEADER, PACK, null, false, Set.of(), true, false).create(), "and stays quiet while the roster is the same");
        assertTrue(PartySyncRules.plan(LEADER, PACK, null, false, Set.of(), true, true).create(), "until the roster changes");
    }

    @Test
    void aPlayerWhoLeftThePartyLeavesThePackButOnlyOneWeAdded() {
        Party party = new Party(LEADER, Set.of(LEADER, MATE));
        Plan p = PartySyncRules.plan(LEADER, PACK, party, true, Set.of(ALPHA2, BETA, MATE), false, false);
        assertEquals(Set.of(ALPHA2, BETA), p.leavePack());
        assertTrue(p.add().isEmpty(), "and nobody is added again");
        // a pack member we never added is not touched, whether they are in the party or not
        Plan q = PartySyncRules.plan(LEADER, PACK, new Party(LEADER, Set.of(LEADER)), true, Set.of(), false, false);
        assertTrue(q.leavePack().isEmpty());
        assertEquals(Set.of(ALPHA2, BETA, MATE), q.add(), "they are asked to join instead");
    }

    @Test
    void aPlayerWhoLeftThePackLeavesTheParty() {
        Party party = new Party(LEADER, Set.of(LEADER, ALPHA2, BETA, MATE));
        Plan p = PartySyncRules.plan(LEADER, Set.of(LEADER, ALPHA2, MATE), party, true, Set.of(ALPHA2, BETA, MATE), false, false);
        assertEquals(Set.of(BETA), p.removeFrom());
    }

    @Test
    void aPartyMemberNobodyAddedStaysEvenWhenTheyAreNotInThePack() {
        Party party = new Party(LEADER, Set.of(LEADER, new UUID(7, 7)));
        Plan p = PartySyncRules.plan(LEADER, PACK, party, true, Set.of(), false, false);
        assertTrue(p.removeFrom().isEmpty());
        assertTrue(p.leavePack().isEmpty());
    }

    @Test
    void whenTheOwnerIsNotTheLeaderTheOwnPeopleLeaveAndTheLeaderGetsANewParty() {
        Party party = new Party(LEADER, Set.of(LEADER, ALPHA2, BETA));
        Plan p = PartySyncRules.plan(ALPHA2, PACK, party, true, Set.of(ALPHA2, BETA), false, false);
        assertTrue(p.abandon() && p.create());
        assertEquals(Set.of(ALPHA2, BETA), p.removeFrom());
        assertFalse(p.removeFrom().contains(LEADER), "the old owner stays in their own party: it is left alone");
        assertEquals(Set.of(LEADER, BETA, MATE), p.add(), "the new party takes the whole pack but its own owner");
    }
}
