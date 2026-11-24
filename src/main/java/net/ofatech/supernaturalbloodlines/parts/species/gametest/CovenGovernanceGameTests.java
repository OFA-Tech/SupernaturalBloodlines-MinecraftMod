package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.clean;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.human;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.join;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.key;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.online;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.witch;

import com.mojang.brigadier.CommandDispatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.group.GroupRank;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.core.group.coven.VoteKind;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers.TalkingDuelist;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupCommand;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupSwitcher;
import net.ofatech.supernaturalbloodlines.parts.species.group.Groups;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.group.coven.CovenGovernance;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackCommand;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests;

/** Plan 20b Task 8 (R250, R251, R259 to R261): Elders, votes, shunning and the call for a new Regent. The Regent and the Elders are witches, so a new Regent still leads a coven. */
@PrefixGameTestTemplate(false)
public final class CovenGovernanceGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final List<String> KEYS = List.of("elder_min_days", "members_per_elder", "vote_hours", "election_cooldown_days", "shun_days");

    private CovenGovernanceGameTests() {
    }

    private record Coven(TalkingDuelist regent, List<TalkingDuelist> elders, List<TalkingDuelist> members) {
        ServerPlayer[] all() {
            List<ServerPlayer> out = new ArrayList<>();
            out.add(regent);
            out.addAll(elders);
            out.addAll(members);
            return out.toArray(new ServerPlayer[0]);
        }
    }

    /** A coven of witches (Regent, {@code elders} Elders) and {@code members} humans; the ranks are written, not voted. */
    private static Coven coven(GameTestHelper helper, int elders, int members) {
        GroupsConfig.override("elder_min_days", 0);
        GroupsConfig.override("members_per_elder", 1);
        TalkingDuelist regent = witch(helper, 1.0);
        List<TalkingDuelist> es = new ArrayList<>();
        List<TalkingDuelist> ms = new ArrayList<>();
        for (int i = 0; i < elders; i++) {
            es.add(witch(helper, 1.5 + 0.2 * i));
        }
        for (int i = 0; i < members; i++) {
            ms.add(human(helper, 3.0 + 0.2 * i));
        }
        Coven c = new Coven(regent, es, ms);
        online(c.all());
        for (ServerPlayer p : c.all()) {
            if (p != regent) {
                helper.assertTrue(join(regent, p), "the coven did not take everyone");
            }
        }
        for (TalkingDuelist e : es) {
            GroupIndex.get(helper.getLevel()).setRank(e.getUUID(), GroupRank.ELDER);
        }
        return c;
    }

    private static void tidy(GameTestHelper helper, Coven c) {
        KEYS.forEach(k -> GroupsConfig.override(k, null));
        clean(helper, c.all());
    }

    private static GroupIndex groups(GameTestHelper helper) {
        return GroupIndex.get(helper.getLevel());
    }

    private static UUID groupOf(GameTestHelper helper, Coven c) {
        return groups(helper).existingGroupOf(c.regent().getUUID()).orElseThrow();
    }

    /** A fake player is not in the player list, so the pack index cannot name it: the commands take the full id as well. */
    private static String name(ServerPlayer p) {
        return p.getUUID().toString();
    }

    private static boolean isElder(GameTestHelper helper, ServerPlayer p) {
        return groups(helper).rankOf(p.getUUID(), GroupType.COVEN) == GroupRank.ELDER;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theRegentAppointsTheFirstElderAlone(GameTestHelper helper) {
        Coven c = coven(helper, 0, 2);
        GroupsConfig.override("members_per_elder", 4);
        try {
            helper.assertTrue(CovenGovernance.elderAdd(c.regent(), name(c.members().get(0))) == null, "the Regent could not appoint");
            helper.assertTrue(isElder(helper, c.members().get(0)), "no Elder was made");
            helper.assertTrue(groups(helper).openVotes(groupOf(helper, c)).isEmpty(), "a vote was opened for an electorate of two");
            var second = CovenGovernance.elderAdd(c.regent(), name(c.members().get(1)));
            helper.assertTrue(second != null && key(second).equals("supernaturalbloodlines.coven.err.elder_cap"), "a coven of three has one seat: " + key(second));
            var notRegent = CovenGovernance.elderAdd(c.members().get(1), name(c.members().get(1)));
            helper.assertTrue(notRegent != null && key(notRegent).equals("supernaturalbloodlines.coven.err.regent_only"), "a member appointed: " + key(notRegent));
            helper.assertTrue(CovenGovernance.elderRemove(c.regent(), name(c.members().get(0))) == null && !isElder(helper, c.members().get(0)), "the Elder was not removed");
        } finally {
            tidy(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void withEldersAMajorityIsNeeded(GameTestHelper helper) {
        Coven c = coven(helper, 2, 1);
        try {
            String target = name(c.members().get(0));
            helper.assertTrue(CovenGovernance.elderAdd(c.regent(), target) == null, "the vote did not open");
            helper.assertTrue(!isElder(helper, c.members().get(0)), "the Regent alone made an Elder in an electorate of three");
            helper.assertTrue(groups(helper).openVotes(groupOf(helper, c)).size() == 1, "no vote is open");
            helper.assertTrue(CovenGovernance.vote(c.elders().get(0), true) == null, "an Elder could not vote");
            helper.assertTrue(!isElder(helper, c.members().get(0)), "the vote ended before everyone voted");
            helper.assertTrue(CovenGovernance.vote(c.elders().get(1), false) == null, "the second Elder could not vote");
            helper.assertTrue(isElder(helper, c.members().get(0)), "two of three is a majority");
            helper.assertTrue(groups(helper).openVotes(groupOf(helper, c)).isEmpty(), "the vote stayed open");
            var outsider = CovenGovernance.vote(c.members().get(0), true);
            helper.assertTrue(outsider != null, "somebody who is not an elector voted");
        } finally {
            tidy(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aTieOrNoVotesFailAtTheTimeLimit(GameTestHelper helper) {
        Coven c = coven(helper, 2, 1);
        long[] clock = {40_000_000L};
        PackIndex.overrideClock(() -> clock[0]);
        try {
            CovenGovernance.elderAdd(c.regent(), name(c.members().get(0)));
            clock[0] += GroupsConfig.voteHours() * 3_600_000L + 1;
            CovenGovernance.sweep(helper.getLevel().getServer());
            helper.assertTrue(!isElder(helper, c.members().get(0)), "a vote with one yes of three passed at the time limit");
            helper.assertTrue(groups(helper).openVotes(groupOf(helper, c)).isEmpty(), "the failed vote stayed open");
        } finally {
            tidy(helper, c);
            PackIndex.overrideClock(null);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anElderNeedsTheDaysInTheCoven(GameTestHelper helper) {
        long[] clock = {50_000_000L};
        PackIndex.overrideClock(() -> clock[0]);
        Coven c = coven(helper, 0, 2);
        try {
            GroupsConfig.override("elder_min_days", 3);
            var early = CovenGovernance.elderAdd(c.regent(), name(c.members().get(0)));
            helper.assertTrue(early != null && key(early).equals("supernaturalbloodlines.coven.err.elder_days"), "an Elder after no days: " + key(early));
            clock[0] += 3L * 24 * 3_600_000L;
            helper.assertTrue(CovenGovernance.elderAdd(c.regent(), name(c.members().get(0))) == null && isElder(helper, c.members().get(0)), "an Elder after three days was refused");
        } finally {
            tidy(helper, c);
            PackIndex.overrideClock(null);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aVoteSurvivesARestart(GameTestHelper helper) {
        Coven c = coven(helper, 2, 1);
        try {
            CovenGovernance.elderAdd(c.regent(), name(c.members().get(0)));
            GroupIndex reloaded = GroupIndex.load(groups(helper).save(new CompoundTag(), null), null);
            var votes = reloaded.openVotes(groupOf(helper, c));
            helper.assertTrue(votes.size() == 1 && votes.get(0).kind == VoteKind.ELDER_ADD && votes.get(0).subject.equals(c.members().get(0).getUUID()), "the vote is " + votes.size());
            helper.assertTrue(votes.get(0).ballots.get(c.regent().getUUID()) == Boolean.TRUE && votes.get(0).caller.equals(c.regent().getUUID()), "the ballots were lost");
        } finally {
            tidy(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aDoubleBallotChangesNothing(GameTestHelper helper) {
        Coven c = coven(helper, 2, 1);
        try {
            CovenGovernance.elderAdd(c.regent(), name(c.members().get(0)));
            var vote = groups(helper).openVotes(groupOf(helper, c)).get(0);
            helper.assertTrue(CovenGovernance.vote(c.elders().get(0), true) == null, "the first ballot");
            var again = CovenGovernance.vote(c.elders().get(0), false);
            helper.assertTrue(again != null && key(again).equals("supernaturalbloodlines.coven.err.no_vote"), "a second ballot was taken: " + key(again));
            helper.assertTrue(!groups(helper).ballot(vote, c.elders().get(0).getUUID(), false), "the index took a second ballot");
            helper.assertTrue(vote.yes() == 2 && vote.no() == 0, "the count changed: " + vote.yes() + "/" + vote.no());
            var again2 = CovenGovernance.vote(c.regent(), false);
            helper.assertTrue(again2 != null && vote.yes() == 2, "the caller changed their ballot");
        } finally {
            tidy(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aVoteWhoseSubjectLeftIsCancelled(GameTestHelper helper) {
        Coven c = coven(helper, 2, 1);
        try {
            CovenGovernance.elderAdd(c.regent(), name(c.members().get(0)));
            helper.assertTrue(PackCommand.leave(c.members().get(0)) == null, "the subject could not leave");
            CovenGovernance.sweep(helper.getLevel().getServer());
            helper.assertTrue(groups(helper).openVotes(groupOf(helper, c)).isEmpty(), "the vote about somebody who left is still open");
            helper.assertTrue(CovenGovernance.vote(c.elders().get(0), true) != null, "a ballot was taken on a cancelled vote");
            helper.assertTrue(!isElder(helper, c.members().get(0)), "the one who left became an Elder");
        } finally {
            tidy(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aTypeSwitchCancelsTheOpenVotesAndKeepsTheElderRanks(GameTestHelper helper) {
        Coven c = coven(helper, 2, 1);
        try {
            CovenGovernance.elderAdd(c.regent(), name(c.members().get(0)));
            UUID group = groupOf(helper, c);
            BloodlineAccess.set(c.regent(), BloodlineState.NONE.toBuilder().species(Species.HYBRID).build());
            GroupSwitcher.refresh(c.regent());
            helper.assertTrue(Groups.typeOf(c.regent()) != GroupType.COVEN, "the group did not switch type");
            helper.assertTrue(groups(helper).openVotes(group).isEmpty(), "a vote survived the switch");
            helper.assertTrue(groups(helper).rankOf(c.elders().get(0).getUUID(), Groups.typeOf(c.regent())) == GroupRank.NONE, "an Elder rank shows in another type");
            BloodlineAccess.set(c.regent(), BloodlineState.NONE.toBuilder().species(Species.WITCH).witchLayer(true).build());
            GroupSwitcher.refresh(c.regent());
            helper.assertTrue(Groups.typeOf(c.regent()) == GroupType.COVEN, "the group did not come back");
            helper.assertTrue(isElder(helper, c.elders().get(0)) && isElder(helper, c.elders().get(1)), "the Elder ranks did not come back");
            helper.assertTrue(groups(helper).openVotes(group).isEmpty(), "the cancelled vote came back");
        } finally {
            tidy(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aShunRemovesTheMemberAndBarsEveryCoven(GameTestHelper helper) {
        Coven c = coven(helper, 0, 2);
        TalkingDuelist other = witch(helper, 6.0);
        online(append(c.all(), other));
        try {
            TalkingDuelist target = c.members().get(0);
            helper.assertTrue(CovenGovernance.shun(c.regent(), name(target)) == null, "the Regent could not shun");
            helper.assertTrue(!PackIndex.get(helper.getLevel()).inPack(target.getUUID()), "the shunned one is still in the coven");
            helper.assertTrue(!isElder(helper, target), "a rank survived the shunning");
            helper.assertTrue(target.received("supernaturalbloodlines.coven.shunned"), "the shunned one was not told");
            helper.assertTrue(c.members().get(1).received("supernaturalbloodlines.coven.shun_done"), "the coven was not told");
            helper.assertTrue(groups(helper).banUntil(CovenGovernance.SHUN_SCOPE, target.getUUID()) > PackIndex.now(), "no ban was written");
            var regent = CovenGovernance.shun(c.regent(), name(c.regent()));
            helper.assertTrue(regent != null, "the Regent shunned themself");
        } finally {
            tidy(helper, c);
            clean(helper, other);
        }
        helper.succeed();
    }

    private static ServerPlayer[] append(ServerPlayer[] all, ServerPlayer extra) {
        ServerPlayer[] out = java.util.Arrays.copyOf(all, all.length + 1);
        out[all.length] = extra;
        return out;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aShunnedWitchCannotJoinAnyCoven(GameTestHelper helper) {
        long[] clock = {60_000_000L};
        PackIndex.overrideClock(() -> clock[0]);
        Coven c = coven(helper, 0, 1);
        TalkingDuelist otherRegent = witch(helper, 6.0);
        online(append(c.all(), otherRegent));
        try {
            TalkingDuelist target = c.members().get(0);
            helper.assertTrue(CovenGovernance.shun(c.regent(), name(target)) == null, "no shunning");
            var invite = PackCommand.invite(otherRegent, target);
            helper.assertTrue(invite != null && key(invite).equals("supernaturalbloodlines.group.target_shunned.coven"), "another coven could invite a shunned witch: " + key(invite));
            String old = PackCommand.requests().open(otherRegent.getUUID(), target.getUUID(), helper.getLevel().getGameTime());
            var accept = PackCommand.accept(target, old);
            helper.assertTrue(accept != null && key(accept).equals("supernaturalbloodlines.group.shunned.coven"), "an old token was accepted: " + key(accept));
            helper.assertTrue(!PackIndex.get(helper.getLevel()).inPack(target.getUUID()), "the shunned one joined");
            clock[0] += GroupsConfig.shunDays() * 24L * 3_600_000L + 1;
            helper.assertTrue(join(otherRegent, target), "the ban did not end after shun_days");
        } finally {
            tidy(helper, c);
            clean(helper, otherRegent);
            PackIndex.overrideClock(null);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aShunnedWitchKeepsTheirMagic(GameTestHelper helper) {
        Coven c = coven(helper, 0, 1);
        TalkingDuelist target = witch(helper, 6.0);
        online(append(c.all(), target));
        try {
            helper.assertTrue(join(c.regent(), target), "no join");
            BloodlineState before = BloodlineAccess.get(target);
            helper.assertTrue(CovenGovernance.shun(c.regent(), name(target)) == null, "no shunning");
            helper.assertTrue(BloodlineAccess.get(target).equals(before) && BloodlineAccess.get(target).species() == Species.WITCH, "the shunning touched the witch's state");
        } finally {
            tidy(helper, c);
            clean(helper, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theRegentCallIsRoutedFromChallenge(GameTestHelper helper) {
        Coven c = coven(helper, 2, 0);
        try {
            CommandDispatcher<CommandSourceStack> d = new CommandDispatcher<>();
            d.register(GroupCommand.tree("group"));
            TalkingDuelist caller = c.elders().get(0);
            TalkingDuelist nominee = c.elders().get(1);
            try {
                d.execute("group challenge " + name(nominee), caller.createCommandSourceStack());
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
                throw new IllegalStateException(e);
            }
            var votes = groups(helper).openVotes(groupOf(helper, c));
            helper.assertTrue(votes.size() == 1 && votes.get(0).kind == VoteKind.REGENT && votes.get(0).subject.equals(nominee.getUUID()), "challenge did not open the election: " + votes.size());
            var asRegent = CovenGovernance.callElection(c.regent(), name(nominee));
            helper.assertTrue(asRegent != null && key(asRegent).equals("supernaturalbloodlines.coven.err.elder_only"), "the Regent called for themself out of office: " + key(asRegent));
        } finally {
            tidy(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPassedElectionMovesTheLeadAndKeepsTheOldRegentAsAnElder(GameTestHelper helper) {
        Coven c = coven(helper, 2, 1);
        try {
            TalkingDuelist nominee = c.elders().get(1);
            helper.assertTrue(CovenGovernance.callElection(c.elders().get(0), name(nominee)) == null, "no election");
            helper.assertTrue(CovenGovernance.vote(nominee, true) == null && CovenGovernance.vote(c.regent(), false) == null, "the ballots were refused");
            PackIndex packs = PackIndex.get(helper.getLevel());
            helper.assertTrue(packs.isLeader(nominee.getUUID()) && !packs.isLeader(c.regent().getUUID()), "the lead did not move");
            helper.assertTrue(isElder(helper, c.regent()), "the old Regent is not an Elder");
            helper.assertTrue(!isElder(helper, nominee), "the new Regent still holds an Elder rank");
            helper.assertTrue(packs.betasOf(nominee.getUUID()).contains(c.regent().getUUID()) && packs.betasOf(nominee.getUUID()).contains(c.members().get(0).getUUID()), "the members did not follow");
            helper.assertTrue(Groups.typeOf(nominee) == GroupType.COVEN, "the group is " + Groups.typeOf(nominee));
        } finally {
            tidy(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void oneElectionPerCooldown(GameTestHelper helper) {
        long[] clock = {70_000_000L};
        PackIndex.overrideClock(() -> clock[0]);
        Coven c = coven(helper, 2, 0);
        try {
            TalkingDuelist nominee = c.elders().get(1);
            CovenGovernance.callElection(c.elders().get(0), name(nominee));
            CovenGovernance.vote(nominee, true);
            CovenGovernance.vote(c.regent(), false);
            helper.assertTrue(PackIndex.get(helper.getLevel()).isLeader(nominee.getUUID()), "the first election did not pass");
            var again = CovenGovernance.callElection(c.elders().get(0), name(c.regent()));
            helper.assertTrue(again != null && key(again).equals("supernaturalbloodlines.coven.err.election_cooldown"), "a second election in the cooldown: " + key(again));
            clock[0] += GroupsConfig.electionCooldownDays() * 24L * 3_600_000L + 1;
            helper.assertTrue(CovenGovernance.callElection(c.elders().get(0), name(c.regent())) == null, "no election after the cooldown");
        } finally {
            tidy(helper, c);
            PackIndex.overrideClock(null);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void noElectionWithAnElectorateUnderThree(GameTestHelper helper) {
        Coven c = coven(helper, 1, 1);
        try {
            var refused = CovenGovernance.callElection(c.elders().get(0), name(c.elders().get(0)));
            helper.assertTrue(refused != null && key(refused).equals("supernaturalbloodlines.coven.err.too_few_electors"), "an election with two electors: " + key(refused));
            helper.assertTrue(CovenGovernance.shun(c.elders().get(0), name(c.members().get(0))) != null, "an Elder shunned alone while the Regent decides");
        } finally {
            tidy(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aGroupVoteCardOpensForEachElectorWhoHasNotVoted(GameTestHelper helper) {
        Coven c = coven(helper, 2, 1);
        try {
            CovenGovernance.elderAdd(c.regent(), name(c.members().get(0)));
            helper.assertTrue(cards(c.regent()) == 0, "the caller has a card for their own vote");
            helper.assertTrue(cards(c.elders().get(0)) == 1 && cards(c.elders().get(1)) == 1, "the electors have no card");
            helper.assertTrue(cards(c.members().get(0)) == 0, "a plain member has a card");
            CovenGovernance.vote(c.elders().get(0), true);
            helper.assertTrue(cards(c.elders().get(0)) == 0 && cards(c.elders().get(1)) == 1, "a card is for somebody who has not voted");
            CovenGovernance.vote(c.elders().get(1), true);
            helper.assertTrue(cards(c.elders().get(0)) == 0 && cards(c.elders().get(1)) == 0, "the cards stayed after the vote ended");
        } finally {
            tidy(helper, c);
        }
        helper.succeed();
    }

    private static long cards(ServerPlayer p) {
        return HubRequests.of(p).stream().filter(r -> r.kind() == HubRequests.Kind.GROUP_VOTE && r.ticksLeft() > 0).count();
    }
}