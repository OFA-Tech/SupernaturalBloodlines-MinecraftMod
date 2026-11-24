package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import net.ofatech.supernaturalbloodlines.core.group.GroupView;
import net.ofatech.supernaturalbloodlines.core.group.circle.Consent;
import net.ofatech.supernaturalbloodlines.core.group.coven.VoteKind;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupTextTest;
import org.junit.jupiter.api.Test;

/** The coven's words: every key the cards draw exists, and the coven never says "pack" (it is not one). */
class CovenTextTest {
    private static final String H = "supernaturalbloodlines.hub.coven.";

    private static List<String> keys() {
        List<String> k = new ArrayList<>();
        for (String s : List.of("tier", "tier.tip", "shunned", "circle_title", "circle_names", "meter", "meter_full", "meter_refill", "meter.tip", "consent", "share", "share_worst",
                "share_unknown", "votes_title", "no_vote", "vote_counts", "vote_mine.yes", "vote_mine.no", "vote_electors", "vote_yes", "vote_no", "elders_title",
                "elder_add", "elder_add.what", "elder_remove", "elder_remove.what", "shun", "shun.what", "shun.body", "call_regent", "call_regent.what", "call_regent.body",
                "link_open", "link_close", "link_join", "link_leave", "f.names", "f.channel", "f.channel_value", "f.share")) {
            k.add(H + s);
        }
        for (int i = 0; i <= 3; i++) {
            k.add(H + "tier." + i);
        }
        for (String c : List.of("none", "anchor", "joined", "called")) {
            k.add(H + "circle." + c);
            k.add(H + "circle_hint." + c);
        }
        for (Consent c : Consent.values()) {
            k.add(H + "consent." + c.id());
            k.add(H + "consent." + c.id() + ".tip");
        }
        for (VoteKind v : VoteKind.values()) {
            k.add(H + "vote." + v.id());
        }
        for (String s : List.of("join", "yes", "no")) {
            k.add("supernaturalbloodlines.hub.home.answer." + s);
        }
        for (String s : List.of("elder_add", "elder_remove", "shun", "call_regent")) {
            k.add("supernaturalbloodlines.hub.profile.action." + s);
            k.add("supernaturalbloodlines.hub.profile.action." + s + ".what");
        }
        k.add("supernaturalbloodlines.hub.profile.confirm.shun");
        k.add("supernaturalbloodlines.hub.profile.confirm.call_regent");
        return k;
    }

    @Test
    void everyCovenKeyTheCardsDrawExists() throws Exception {
        JsonObject lang = GroupTextTest.lang();
        List<String> missing = new ArrayList<>();
        for (String k : keys()) {
            if (!lang.has(k)) {
                missing.add(k);
            }
        }
        assertTrue(missing.isEmpty(), "coven keys missing from the lang file: " + missing);
    }

    @Test
    void noCovenWordIsACopyOfThePacksText() throws Exception {
        JsonObject lang = GroupTextTest.lang();
        List<String> bad = new ArrayList<>();
        for (String k : lang.keySet()) {
            if ((k.startsWith(H) || k.startsWith("supernaturalbloodlines.hub.group.") && k.endsWith(".coven") || k.startsWith("supernaturalbloodlines.rite.grand.")
                    || k.startsWith("supernaturalbloodlines.coven.")) && lang.get(k).getAsString().toLowerCase().contains("pack")) {
                bad.add(k);
            }
        }
        assertTrue(bad.isEmpty(), "a coven text names a pack: " + bad);
    }

    @Test
    void theRegentAppointsAndRemovesElders() {
        assertEquals(List.of("elder_add", "shun"), ids(CovenCards.actionsFor(GroupView.LEADER, "member", false, true)));
        assertEquals(List.of("elder_remove", "shun"), ids(CovenCards.actionsFor(GroupView.LEADER, "elder", false, true)));
    }

    @Test
    void anElderMayShunAndMayCallForARegentButNotAppoint() {
        assertEquals(List.of("shun"), ids(CovenCards.actionsFor(GroupView.ELDER, "member", false, true)));
        assertEquals(List.of("shun", "call_regent"), ids(CovenCards.actionsFor(GroupView.ELDER, "elder", false, true)));
    }

    @Test
    void aMemberSelfTheRegentAnAllyOrNoIdGetsNothing() {
        assertTrue(CovenCards.actionsFor(GroupView.MEMBER, "member", false, true).isEmpty());
        assertTrue(CovenCards.actionsFor(GroupView.LEADER, "member", true, true).isEmpty());
        assertTrue(CovenCards.actionsFor(GroupView.LEADER, "leader", false, true).isEmpty());
        assertTrue(CovenCards.actionsFor(GroupView.LEADER, "ally", false, true).isEmpty());
        assertTrue(CovenCards.actionsFor(GroupView.LEADER, "member", false, false).isEmpty());
        assertTrue(CovenCards.actionsFor(GroupView.ELDER, "leader", false, true).isEmpty());
    }

    @Test
    void shunningAndCallingAskFirst() {
        assertTrue(CovenCards.actionsFor(GroupView.ELDER, "elder", false, true).stream().allMatch(CovenCards.Act::confirm));
        assertTrue(CovenCards.actionsFor(GroupView.LEADER, "member", false, true).stream().filter(a -> a.id().equals("elder_add")).noneMatch(CovenCards.Act::confirm));
    }

    @Test
    void theCardsAreForACovenOnly() {
        for (net.ofatech.supernaturalbloodlines.core.group.GroupType type : net.ofatech.supernaturalbloodlines.core.group.GroupType.values()) {
            GroupView v = new GroupView(type, true, GroupView.MEMBER, "x", 0, List.of());
            assertEquals(type == net.ofatech.supernaturalbloodlines.core.group.GroupType.COVEN, CovenCards.applies(v), type.name());
        }
        assertTrue(!CovenCards.applies(new GroupView(net.ofatech.supernaturalbloodlines.core.group.GroupType.COVEN, false, GroupView.NOT_IN, "", 0, List.of())));
    }

    @Test
    void theConsentButtonSendsALevelTheCommandParsesBack() {
        for (Consent c : Consent.values()) {
            assertEquals(c, Consent.parse(CovenCards.consentArg(c)));
            assertEquals(c.id(), CovenCards.consentArg(c));
        }
    }

    @Test
    void aCircleCallCarriesItsTokenAndAVoteDoesNot() {
        var call = HomePage.answers(net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests.Kind.CIRCLE_CALL);
        assertEquals(1, call.size());
        assertEquals("C3", call.get(0).rowId());
        assertTrue(call.get(0).usesToken());
        var vote = HomePage.answers(net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests.Kind.GROUP_VOTE);
        assertEquals(List.of("C9", "C10"), vote.stream().map(HomePage.Answer::rowId).toList());
        assertTrue(vote.stream().noneMatch(HomePage.Answer::usesToken));
    }

    private static List<String> ids(List<CovenCards.Act> acts) {
        return acts.stream().map(CovenCards.Act::id).toList();
    }
}