package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import static net.ofatech.supernaturalbloodlines.parts.species.gametest.CovenCostGameTests.circleAt;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.CovenCostGameTests.tidy;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.human;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.join;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.online;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.witch;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.group.GroupRank;
import net.ofatech.supernaturalbloodlines.core.group.circle.Consent;
import net.ofatech.supernaturalbloodlines.core.group.coven.CovenView;
import net.ofatech.supernaturalbloodlines.core.group.coven.VoteKind;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers.TalkingDuelist;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.CovenCostGameTests.Circle;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.group.circle.CircleLink;
import net.ofatech.supernaturalbloodlines.parts.species.group.coven.CovenGovernance;
import net.ofatech.supernaturalbloodlines.parts.species.group.coven.CovenViews;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackAccess;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;

/** Plan 20b Task 11: the coven view carries the viewer's own meter, consent and ballot, and the names of the viewer's own coven only. */
@PrefixGameTestTemplate(false)
public final class CovenViewGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private CovenViewGameTests() {
    }

    private static String nameOf(GameTestHelper helper, UUID id) {
        String n = PackAccess.nameOf(helper.getLevel().getServer(), id);
        return n != null ? n : id.toString().substring(0, 8);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void eachMemberGetsTheirOwnMeterAndConsent(GameTestHelper helper) {
        Circle c = circleAt(helper, new BlockPos(8, 1, 8), 3);
        try {
            GroupIndex groups = GroupIndex.get(helper.getLevel());
            long now = PackIndex.now();
            groups.setChannel(c.anchor().getUUID(), 1000, now, 3000);
            groups.setChannel(c.others().get(0).getUUID(), 2000, now, 3000);
            groups.setConsent(c.anchor().getUUID(), Consent.NOSEBLEED);
            groups.setConsent(c.others().get(0).getUUID(), Consent.DAMAGE);
            CovenView a = CovenViews.of(c.anchor());
            CovenView b = CovenViews.of(c.others().get(0));
            CovenView z = CovenViews.of(c.others().get(1));
            helper.assertTrue(a.meter() == 1000 && b.meter() == 2000 && z.meter() == 3000, "meters " + a.meter() + ", " + b.meter() + ", " + z.meter());
            helper.assertTrue(a.consent() == Consent.NOSEBLEED && b.consent() == Consent.DAMAGE && z.consent() == Consent.NONE, "consents are not each their own");
            helper.assertTrue(a.circle() == CovenView.Circle.ANCHOR && b.circle() == CovenView.Circle.JOINED, "circle roles: " + a.circle() + " " + b.circle());
            helper.assertTrue(a.size() == 3 && a.tier() == 1 && a.regenSeconds() > 0 && b.regenSeconds() < a.regenSeconds(), "size/tier/regen: " + a.size() + " " + a.tier() + " " + a.regenSeconds());
            helper.assertTrue(a.circleNames().size() == 3 && a.anchorName().equals(nameOf(helper, c.anchor().getUUID())), "the circle's names are wrong: " + a.circleNames());
        } finally {
            tidy(helper, c.array());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void onlyTheViewersOwnCovenIsCarried(GameTestHelper helper) {
        Circle mine = circleAt(helper, new BlockPos(8, 1, 8), 2);
        TalkingDuelist otherLeader = witch(helper, 12);
        TalkingDuelist otherMember = human(helper, 13);
        try {
            online(new ServerPlayer[] {mine.anchor(), mine.others().get(0), otherLeader, otherMember});
            helper.assertTrue(join(otherLeader, otherMember), "the second coven did not form");
            CovenView v = CovenViews.of(mine.anchor());
            List<String> all = new ArrayList<>(v.circleNames());
            all.add(v.anchorName());
            for (UUID stranger : List.of(otherLeader.getUUID(), otherMember.getUUID())) {
                helper.assertTrue(!all.contains(nameOf(helper, stranger)), "another coven's name is in the view");
            }
            helper.assertTrue(v.size() == 2, "the size counts another coven: " + v.size());
        } finally {
            tidy(helper, otherLeader, otherMember);
            tidy(helper, mine.array());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aNonWitchViewerStillGetsTheirCard(GameTestHelper helper) {
        TalkingDuelist leader = witch(helper, 1.5);
        TalkingDuelist member = human(helper, 2.5);
        online(leader, member);
        try {
            helper.assertTrue(join(leader, member), "the human could not join");
            CovenView v = CovenViews.of(member);
            helper.assertTrue(v.size() == 2 && v.meterCap() == (int) Math.round(GroupsConfig.channelCap() * GroupsConfig.channelOtherFactor()), "the human's meter cap is " + v.meterCap());
            helper.assertTrue(v.meter() == v.meterCap() && v.circle() == CovenView.Circle.NONE && v.vote().isEmpty(), "a fresh member: " + v);
            helper.assertTrue(CovenViews.of(human(helper, 5)).equals(CovenView.NONE), "somebody with no coven got a card");
        } finally {
            tidy(helper, leader, member);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theVoteShowsCountsAndTheViewersOwnBallotOnly(GameTestHelper helper) {
        TalkingDuelist regent = witch(helper, 1.5);
        TalkingDuelist e1 = human(helper, 2.5);
        TalkingDuelist e2 = human(helper, 3.5);
        TalkingDuelist target = human(helper, 4.5);
        online(regent, e1, e2, target);
        try {
            for (TalkingDuelist m : List.of(e1, e2, target)) {
                helper.assertTrue(join(regent, m), "could not join");
            }
            GroupIndex groups = GroupIndex.get(helper.getLevel());
            groups.setRank(e1.getUUID(), GroupRank.ELDER);
            groups.setRank(e2.getUUID(), GroupRank.ELDER);
            helper.assertTrue(CovenGovernance.shun(regent, target.getUUID().toString()) == null, "the shun vote was not opened");
            CovenView r = CovenViews.of(regent);
            helper.assertTrue(r.vote().isPresent() && r.vote().get().kind() == VoteKind.SHUN, "no shun vote in the view");
            helper.assertTrue(r.vote().get().electors() == 3 && r.vote().get().yes() == 1 && r.vote().get().no() == 0 && r.vote().get().mine() == CovenView.Ballot.YES, "regent: " + r.vote());
            CovenView a = CovenViews.of(e1);
            helper.assertTrue(a.vote().get().yes() == 1 && a.vote().get().mine() == CovenView.Ballot.NONE, "an elder who has not voted sees the regent's ballot: " + a.vote());
            helper.assertTrue(CovenGovernance.vote(e1, false, "shun") == null, "the elder could not vote");
            CovenView after = CovenViews.of(regent);
            helper.assertTrue(after.vote().get().no() == 1 && after.vote().get().mine() == CovenView.Ballot.YES, "the count did not move: " + after.vote());
            CovenView plain = CovenViews.of(target);
            helper.assertTrue(plain.vote().isPresent() && plain.vote().get().mine() == CovenView.Ballot.NONE && plain.vote().get().subjectName().equals(nameOf(helper, target.getUUID())),
                    "the member the vote is about does not see the counts: " + plain.vote());
            helper.assertTrue(r.vote().get().hoursLeft() > 0 && r.vote().get().hoursLeft() <= GroupsConfig.voteHours(), "hours left " + r.vote().get().hoursLeft());
        } finally {
            tidy(helper, regent, e1, e2, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theViewShowsTheFirstVoteTheViewerHasNotAnswered(GameTestHelper helper) {
        TalkingDuelist regent = witch(helper, 1.5);
        TalkingDuelist e1 = human(helper, 2.5);
        TalkingDuelist e2 = human(helper, 3.5);
        TalkingDuelist a = human(helper, 4.5);
        TalkingDuelist b = human(helper, 5.5);
        online(regent, e1, e2, a, b);
        try {
            for (TalkingDuelist m : List.of(e1, e2, a, b)) {
                helper.assertTrue(join(regent, m), "could not join");
            }
            GroupIndex groups = GroupIndex.get(helper.getLevel());
            groups.setRank(e1.getUUID(), GroupRank.ELDER);
            groups.setRank(e2.getUUID(), GroupRank.ELDER);
            GroupsConfig.override("elder_min_days", 0);
            GroupsConfig.override("members_per_elder", 1);
            helper.assertTrue(CovenGovernance.elderAdd(regent, a.getUUID().toString()) == null, "the first vote did not open");
            helper.assertTrue(CovenGovernance.shun(regent, b.getUUID().toString()) == null, "the second vote did not open");
            helper.assertTrue(CovenViews.of(e1).vote().get().kind() == VoteKind.ELDER_ADD, "an Elder who has answered nothing should see the older vote first");
            helper.assertTrue(CovenGovernance.vote(e1, true, "elder_add") == null, "the Elder could not answer");
            var shown = CovenViews.of(e1).vote().get();
            helper.assertTrue(shown.kind() == VoteKind.SHUN && shown.mine() == CovenView.Ballot.NONE, "the view still shows the vote already answered: " + shown);
            helper.assertTrue(CovenGovernance.vote(e1, true, "shun") == null, "the Elder could not answer the second");
            helper.assertTrue(CovenViews.of(e1).vote().get().kind() == VoteKind.ELDER_ADD, "with everything answered the older vote is shown");
        } finally {
            GroupsConfig.override("elder_min_days", null);
            GroupsConfig.override("members_per_elder", null);
            tidy(helper, regent, e1, e2, a, b);
        }
        helper.succeed();
    }
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aShunnedPlayerSeesTheDaysLeft(GameTestHelper helper) {
        TalkingDuelist p = human(helper, 1.5);
        try {
            GroupIndex.get(helper.getLevel()).ban(CovenGovernance.SHUN_SCOPE, p.getUUID(), PackIndex.now() + 3L * 24 * 60 * 60 * 1000 - 1000);
            helper.assertTrue(CovenViews.of(p).shunnedDaysLeft() == 3, "days left: " + CovenViews.of(p).shunnedDaysLeft());
            GroupIndex.get(helper.getLevel()).ban(CovenGovernance.SHUN_SCOPE, p.getUUID(), 0);
            helper.assertTrue(CovenViews.of(p).shunnedDaysLeft() == 0, "an ended ban still shows");
        } finally {
            GroupIndex.get(helper.getLevel()).ban(CovenGovernance.SHUN_SCOPE, p.getUUID(), 0);
            tidy(helper, p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCalledMemberSeesTheCallAndTheShareIsTheWorstCaseUntilARiteLies(GameTestHelper helper) {
        Circle c = circleAt(helper, new BlockPos(8, 1, 8), 2);
        TalkingDuelist third = human(helper, 3);
        try {
            online(c.anchor(), c.others().get(0), third);
            helper.assertTrue(join(c.anchor(), third), "the third did not join the coven");
            CircleLink.calls().open(c.anchor().getUUID(), third.getUUID(), helper.getLevel().getGameTime());
            CovenView v = CovenViews.of(third);
            helper.assertTrue(v.circle() == CovenView.Circle.CALLED && v.anchorName().equals(nameOf(helper, c.anchor().getUUID())), "the call is not in the view: " + v.circle());
            CovenView a = CovenViews.of(c.anchor());
            helper.assertTrue(!a.shareKnown() && a.sharePower() > 0 && a.shareEp() == 0, "share before a rite lies: " + a.sharePower() + " " + a.shareEp() + " " + a.shareKnown());
            helper.assertTrue(a.sharePower() >= CovenViews.of(c.others().get(0)).sharePower(), "the anchor takes the remainder first");
        } finally {
            CircleLink.calls().forget(third.getUUID());
            tidy(helper, third);
            tidy(helper, c.array());
        }
        helper.succeed();
    }
}