package net.ofatech.supernaturalbloodlines.shared.supernatural;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class HubRequestsVisibleTest {
    private static final UUID A = new UUID(1, 1);
    private static final HubRequests.Request LIVE = new HubRequests.Request(HubRequests.Kind.SIRE_CALL, A, "Klaus", "aaaaaaaa", 600);
    private static final HubRequests.Request GONE = new HubRequests.Request(HubRequests.Kind.PACK_INVITE, A, "Klaus", "bbbbbbbb", 0);
    private static final HubRequests.Request DUEL = new HubRequests.Request(HubRequests.Kind.DUEL, A, "Klaus", "on", 600);

    @Test
    void anExpiredCardIsNotShownEvenIfTheServerStillListsIt() {
        // the server sends the ticks left once; the client counts down, so a card can run out before the next push
        assertEquals(List.of(LIVE), HubRequests.visible(List.of(LIVE, GONE), Set.of(), r -> r.ticksLeft()));
        assertEquals(List.of(), HubRequests.visible(List.of(LIVE), Set.of(), r -> 0));
    }

    @Test
    void aHiddenCardIsOutOfViewButCounted() {
        Set<String> hidden = Set.of(HubRequests.key(LIVE));
        assertEquals(List.of(), HubRequests.visible(List.of(LIVE), hidden, r -> r.ticksLeft()));
        assertEquals(1, HubRequests.hiddenCount(List.of(LIVE, GONE, DUEL), hidden, r -> r.ticksLeft()));
        assertEquals(0, HubRequests.hiddenCount(List.of(GONE), Set.of(HubRequests.key(GONE)), r -> r.ticksLeft()), "an expired hidden card is not offered back");
    }

    @Test
    void aHidingEndsWithItsRequestSoANewChallengeFromTheSamePersonShowsAgain() {
        HubRequests.Request challenge = new HubRequests.Request(HubRequests.Kind.CHALLENGE, A, "Klaus", "", 600);
        Set<String> hidden = Set.of(HubRequests.key(challenge), HubRequests.key(LIVE));
        // the challenge is gone from what the server lists; the call is still there
        assertEquals(Set.of(HubRequests.key(LIVE)), HubRequests.stillHidden(hidden, List.of(LIVE)));
        assertEquals(Set.of(), HubRequests.stillHidden(hidden, List.of()));
        assertEquals(List.of(challenge), HubRequests.visible(List.of(challenge), HubRequests.stillHidden(hidden, List.of()), r -> r.ticksLeft()));
    }

    @Test
    void aDuelIsNeverARequest() {
        assertEquals(List.of(), HubRequests.visible(List.of(DUEL), Set.of(), r -> r.ticksLeft()));
    }
}

