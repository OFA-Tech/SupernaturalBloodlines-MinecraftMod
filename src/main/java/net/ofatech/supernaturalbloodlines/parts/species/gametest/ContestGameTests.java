package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.clean;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.human;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.join;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.key;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.online;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.vampire;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.witch;

import com.mojang.brigadier.CommandDispatcher;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.group.GroupRank;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.core.group.clan.CharterState;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers.TalkingDuelist;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex.CharterRow;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex.ContestRow;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupLead;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupSwitcher;
import net.ofatech.supernaturalbloodlines.parts.species.group.Groups;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupCommand;
import net.ofatech.supernaturalbloodlines.parts.species.group.clan.CharterScan;
import net.ofatech.supernaturalbloodlines.parts.species.group.clan.Charters;
import net.ofatech.supernaturalbloodlines.parts.species.group.clan.Contests;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackCommand;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.shared.admin.AdminCommands;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests;

/** Plan 20c Task 6 (R272, R277): the Game of Kings. Day time and the wall clock are pinned; nothing here loops on a tick counter. */
@PrefixGameTestTemplate(false)
public final class ContestGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final long DAY = 24L * 60 * 60 * 1000;
    private static final long MIN = 60_000L;
    private static final String ERR = "supernaturalbloodlines.clan.err.";
    private static final String CLAN = "supernaturalbloodlines.clan.";

    private ContestGameTests() {
    }

    private record Clan(TalkingDuelist leader, TalkingDuelist a, TalkingDuelist b) {
        TalkingDuelist[] all() {
            return new TalkingDuelist[] {leader, a, b};
        }
    }

    private static final long[] DAYTIME = {17_000L};
    private static final long[] CLOCK = {20_000_000_000L};

    private static Clan ready(GameTestHelper helper, double x, boolean forge, boolean noMinimum) {
        DAYTIME[0] = 17_000L;
        CLOCK[0] = 20_000_000_000L;
        PackIndex.overrideClock(() -> CLOCK[0]);
        Contests.overrideDayTime(() -> DAYTIME[0]);
        if (noMinimum) {
            GroupsConfig.override("challenger_min_days", 0);
        }
        GroupsConfig.override("contest_min_minutes", 0);
        TalkingDuelist l = vampire(helper, x);
        TalkingDuelist a = vampire(helper, x + 1);
        TalkingDuelist b = vampire(helper, x + 2);
        online(l, a, b);
        helper.assertTrue(join(l, a) && join(l, b) && Groups.typeOf(a) == GroupType.CLAN, "no clan formed");
        Clan c = new Clan(l, a, b);
        groups(helper).setRank(a.getUUID(), GroupRank.DAY_WALKER);
        CharterScan.overridePlayers(() -> List.of(l, a, b));
        if (forge) {
            l.getInventory().add(new ItemStack(Items.WRITABLE_BOOK));
            l.getInventory().add(new ItemStack(Items.GOLD_BLOCK));
            helper.assertTrue(Charters.forge(l) == null, "setup: the forge was refused");
        }
        return c;
    }

    private static Clan ready(GameTestHelper helper) {
        return ready(helper, 1.5, true, true);
    }

    private static GroupIndex groups(GameTestHelper helper) {
        return GroupIndex.get(helper.getLevel());
    }

    private static MinecraftServer server(GameTestHelper helper) {
        return helper.getLevel().getServer();
    }

    private static UUID group(GameTestHelper helper, TalkingDuelist anyMember) {
        return groups(helper).existingGroupOf(PackIndex.get(helper.getLevel()).leaderOf(anyMember.getUUID())).orElseThrow();
    }

    private static Optional<ContestRow> contest(GameTestHelper helper, TalkingDuelist anyMember) {
        return groups(helper).contestOf(group(helper, anyMember));
    }

    private static UUID leaderOf(GameTestHelper helper, TalkingDuelist p) {
        return PackIndex.get(helper.getLevel()).leaderOf(p.getUUID());
    }

    private static void said(GameTestHelper helper, Component c, String key, String why) {
        helper.assertTrue(key.equals(key(c)), why + ": " + key(c));
    }

    private static boolean isCharter(ItemStack s) {
        return s.is(SpeciesRegistries.CLAN_CHARTER.get());
    }

    private static ItemStack takeCharter(TalkingDuelist p) {
        for (int i = 0; i < p.getInventory().items.size(); i++) {
            ItemStack s = p.getInventory().items.get(i);
            if (isCharter(s)) {
                p.getInventory().items.set(i, ItemStack.EMPTY);
                return s;
            }
        }
        return ItemStack.EMPTY;
    }

    private static void pass(TalkingDuelist from, TalkingDuelist to) {
        to.getInventory().add(takeCharter(from));
    }

    private static void done(GameTestHelper helper, TalkingDuelist... players) {
        for (ItemEntity e : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(helper.absoluteVec(new Vec3(4, 2, 4)), helper.absoluteVec(new Vec3(4, 2, 4))).inflate(30), e -> isCharter(e.getItem()))) {
            e.discard();
        }
        clean(helper, players);
    }

    private static void begin(GameTestHelper helper, Clan c, TalkingDuelist challenger) {
        helper.assertTrue(Contests.challenge(challenger) == null, "setup: the challenge was refused");
        helper.assertTrue(Contests.accept(c.leader()) == null, "setup: the accept was refused");
        helper.assertTrue(contest(helper, c.leader()).orElseThrow().phase() == ContestRow.Phase.RUNNING, "setup: not running");
    }

    // ---- challenge ----

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aDayWalkerChallengesAndTheLeaderIsAsked(GameTestHelper helper) {
        Clan c = ready(helper);
        try {
            helper.assertTrue(Contests.challenge(c.a()) == null, "refused");
            ContestRow row = contest(helper, c.leader()).orElseThrow();
            helper.assertTrue(row.phase() == ContestRow.Phase.PENDING && row.challenger().equals(c.a().getUUID()), "the row is " + row);
            List<HubRequests.Request> cards = HubRequests.of(c.leader()).stream().filter(r -> r.kind() == HubRequests.Kind.CHARTER_CONTEST).toList();
            helper.assertTrue(cards.size() == 1 && cards.get(0).ticksLeft() > 0 && cards.get(0).token().isEmpty(), "the card is " + cards);
            helper.assertTrue(c.leader().received(CLAN + "challenged") && c.a().received(CLAN + "challenged") && c.b().received(CLAN + "challenged"), "somebody was not told");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aNightWalkerCannotChallenge(GameTestHelper helper) {
        Clan c = ready(helper);
        try {
            said(helper, Contests.challenge(c.b()), ERR + "challenge_not_day_walker", "a Night Walker challenged");
            helper.assertTrue(contest(helper, c.leader()).isEmpty(), "a row was written");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void sixDaysInTheClanIsTooFew(GameTestHelper helper) {
        Clan c = ready(helper, 1.5, true, false);
        try {
            CLOCK[0] += 7 * DAY - 1;
            said(helper, Contests.challenge(c.a()), ERR + "challenge_too_new", "challenged one millisecond early");
            CLOCK[0] += 1;
            helper.assertTrue(Contests.challenge(c.a()) == null, "refused after the days");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void theLeaderCannotChallengeThemselves(GameTestHelper helper) {
        Clan c = ready(helper);
        try {
            said(helper, Contests.challenge(c.leader()), ERR + "challenge_leader", "the leader challenged");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void noCharterNoChallenge(GameTestHelper helper) {
        Clan c = ready(helper, 1.5, false, true);
        try {
            said(helper, Contests.challenge(c.a()), ERR + "challenge_no_charter", "challenged with no Charter");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aLostCharterCannotBeChallengedFor(GameTestHelper helper) {
        Clan c = ready(helper);
        try {
            UUID g = group(helper, c.leader());
            CharterRow r = groups(helper).charterOf(g).orElseThrow();
            groups(helper).setCharter(g, new CharterRow(r.id(), Optional.empty(), CharterState.LOST, r.forgedAt(), r.seenAt(), Optional.empty(), CLOCK[0]));
            said(helper, Contests.challenge(c.a()), ERR + "challenge_no_charter", "challenged for a lost Charter");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aChallengeNeedsThreeMembersAtThatMoment(GameTestHelper helper) {
        Clan c = ready(helper);
        try {
            helper.assertTrue(PackCommand.leave(c.b()) == null, "setup: leaving refused");
            said(helper, Contests.challenge(c.a()), ERR + "challenge_too_few", "a clan of two held a contest");
            helper.assertTrue(groups(helper).charterOf(group(helper, c.leader())).isPresent(), "the Charter went when the clan shrank");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void theLeaderMustBeOnline(GameTestHelper helper) {
        Clan c = ready(helper);
        try {
            online(c.a(), c.b());
            said(helper, Contests.challenge(c.a()), ERR + "challenge_leader_away", "challenged an absent head");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void oneContestPerSevenDays(GameTestHelper helper) {
        Clan c = ready(helper);
        try {
            begin(helper, c, c.a());
            DAYTIME[0] = 18_000L;
            Contests.tick(server(helper), DAYTIME[0]);
            helper.assertTrue(contest(helper, c.leader()).isEmpty(), "setup: the first contest did not end");
            CLOCK[0] += 7 * DAY - 1;
            Component refusal = Contests.challenge(c.a());
            said(helper, refusal, ERR + "challenge_cooldown", "a second contest inside the week");
            CLOCK[0] += 1;
            helper.assertTrue(Contests.challenge(c.a()) == null, "refused after the week");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aSecondChallengeWhilePendingIsRefused(GameTestHelper helper) {
        Clan c = ready(helper);
        try {
            groups(helper).setRank(c.b().getUUID(), GroupRank.DAY_WALKER);
            helper.assertTrue(Contests.challenge(c.a()) == null, "setup");
            said(helper, Contests.challenge(c.b()), ERR + "challenge_already", "a second pending challenge");
            helper.assertTrue(contest(helper, c.leader()).orElseThrow().challenger().equals(c.a().getUUID()), "the first was replaced");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void challengeInAPackAnswersWrongType(GameTestHelper helper) {
        TalkingDuelist l = human(helper, 1.5);
        TalkingDuelist a = human(helper, 2.5);
        online(l, a);
        try {
            PackIndex.get(helper.getLevel()).join(l.getUUID(), a.getUUID());
            groups(helper).ensure(l.getUUID(), GroupType.PACK, PackIndex.now());
            said(helper, Contests.challenge(a), "supernaturalbloodlines.group.wrong_type.pack", "a pack member challenged");
            said(helper, Contests.accept(l), "supernaturalbloodlines.group.wrong_type.pack", "a pack leader accepted");
        } finally {
            done(helper, l, a);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void challengeIsOffWithCharterDisabled(GameTestHelper helper) {
        Clan c = ready(helper);
        try {
            GroupsConfig.override("charter_enabled", false);
            said(helper, Contests.challenge(c.a()), ERR + "charter_off", "challenged with Charters off");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    // ---- start ----

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void theLeaderAcceptsAndTheContestRuns(GameTestHelper helper) {
        Clan c = ready(helper);
        try {
            begin(helper, c, c.a());
            ContestRow row = contest(helper, c.leader()).orElseThrow();
            helper.assertTrue(row.startedDayTime() == 17_000L && row.startedAt() == CLOCK[0], "the row is " + row);
            helper.assertTrue(c.a().received(CLAN + "contest_started") && c.b().received(CLAN + "contest_started"), "the clan was not told");
            helper.assertTrue(HubRequests.of(c.leader()).stream().noneMatch(r -> r.kind() == HubRequests.Kind.CHARTER_CONTEST), "the card stayed after the accept");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void theLeaderCannotDecline(GameTestHelper helper) {
        Clan c = ready(helper);
        try {
            helper.assertTrue(Contests.challenge(c.a()) == null, "setup");
            said(helper, Contests.decline(c.leader()), ERR + "no_decline", "the leader declined");
            helper.assertTrue(contest(helper, c.leader()).orElseThrow().phase() == ContestRow.Phase.PENDING, "the challenge went away");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void noAnswerStartsItAnywayAfterTheMinutes(GameTestHelper helper) {
        Clan c = ready(helper);
        try {
            helper.assertTrue(Contests.challenge(c.a()) == null, "setup");
            CLOCK[0] += 10 * MIN - 1;
            Contests.tick(server(helper), DAYTIME[0]);
            helper.assertTrue(contest(helper, c.leader()).orElseThrow().phase() == ContestRow.Phase.PENDING, "started early");
            CLOCK[0] += 1;
            Contests.tick(server(helper), DAYTIME[0]);
            helper.assertTrue(contest(helper, c.leader()).orElseThrow().phase() == ContestRow.Phase.RUNNING, "did not start by itself");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void noAnswerWithTheLeaderOfflineCancelsWithNoCooldown(GameTestHelper helper) {
        Clan c = ready(helper);
        try {
            helper.assertTrue(Contests.challenge(c.a()) == null, "setup");
            online(c.a(), c.b());
            CLOCK[0] += 10 * MIN;
            Contests.tick(server(helper), DAYTIME[0]);
            helper.assertTrue(contest(helper, c.leader()).isEmpty(), "the row stayed");
            helper.assertTrue(groups(helper).cooldownUntil(group(helper, c.leader()), "contest") == 0, "a cooldown was stamped");
            helper.assertTrue(c.a().received(CLAN + "contest_cancelled") && c.b().received(CLAN + "contest_cancelled"), "the clan was not told");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void theCooldownStartsAtTheStart(GameTestHelper helper) {
        Clan c = ready(helper);
        try {
            helper.assertTrue(Contests.challenge(c.a()) == null, "setup");
            helper.assertTrue(groups(helper).cooldownUntil(group(helper, c.leader()), "contest") == 0, "the cooldown started at the challenge");
            Contests.accept(c.leader());
            helper.assertTrue(groups(helper).cooldownUntil(group(helper, c.leader()), "contest") == CLOCK[0] + 7 * DAY, "no cooldown at the start");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aNonLeaderCannotAccept(GameTestHelper helper) {
        Clan c = ready(helper);
        try {
            helper.assertTrue(Contests.challenge(c.a()) == null, "setup");
            said(helper, Contests.accept(c.a()), ERR + "leader_only", "the challenger accepted their own challenge");
            said(helper, Contests.accept(c.b()), ERR + "leader_only", "a member accepted");
            helper.assertTrue(contest(helper, c.leader()).orElseThrow().phase() == ContestRow.Phase.PENDING, "it started");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aMemberWhoDoesNotFitCannotChallengeOrWinWithAStoredRank(GameTestHelper helper) {
        Clan c = ready(helper);
        TalkingDuelist h = human(helper, 6.5);
        online(c.leader(), c.a(), c.b(), h);
        try {
            helper.assertTrue(join(c.leader(), h), "setup: the human joined");
            groups(helper).setRank(h.getUUID(), GroupRank.DAY_WALKER);
            said(helper, Contests.challenge(h), ERR + "challenge_not_day_walker", "a human with a stored Day Walker rank challenged");
            begin(helper, c, c.a());
            pass(c.leader(), h);
            CharterScan.overridePlayers(() -> List.of(c.leader(), c.a(), c.b(), h));
            midnight(helper);
            helper.assertTrue(leaderOf(helper, c.a()).equals(c.leader().getUUID()), "a human with a stored rank took the lead");
        } finally {
            done(helper, c.leader(), c.a(), c.b(), h);
        }
        helper.succeed();
    }

    // ---- a contest cannot be cancelled by removing its two people ----

    private static String id(TalkingDuelist p) {
        return p.getUUID().toString();
    }

    private static void locked(GameTestHelper helper, Clan c, TalkingDuelist target, String phase) {
        said(helper, net.ofatech.supernaturalbloodlines.parts.species.group.clan.ClanDiscipline.demote(c.leader(), id(target)), ERR + "contest_running", phase + ": a demotion of a contest's person");
        said(helper, net.ofatech.supernaturalbloodlines.parts.species.group.clan.ClanDiscipline.exile(c.leader(), id(target)), ERR + "contest_running", phase + ": an exile of a contest's person");
        said(helper, PackCommand.kick(c.leader(), target.getUUID(), id(target)), ERR + "contest_running", phase + ": a kick of a contest's person");
        helper.assertTrue(PackIndex.get(helper.getLevel()).leaderOf(target.getUUID()).equals(c.leader().getUUID()), phase + ": the person left the clan");
        helper.assertTrue(groups(helper).banUntil(group(helper, c.leader()).toString(), target.getUUID()) == 0, phase + ": an exile row was written");
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void theChallengerCannotBeRemovedWhileItIsPendingOrRunning(GameTestHelper helper) {
        Clan c = ready(helper);
        try {
            helper.assertTrue(Contests.challenge(c.a()) == null, "setup: the challenge");
            locked(helper, c, c.a(), "pending");
            helper.assertTrue(Contests.accept(c.leader()) == null, "setup: the accept");
            locked(helper, c, c.a(), "running");
            helper.assertTrue(groups(helper).rankOf(c.a().getUUID(), GroupType.CLAN) == GroupRank.DAY_WALKER, "the rank went");
            helper.assertTrue(contest(helper, c.leader()).isPresent(), "the contest row went");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void theCharterHolderCannotBeRemovedWhileItIsPendingOrRunning(GameTestHelper helper) {
        Clan c = ready(helper);
        try {
            groups(helper).setRank(c.b().getUUID(), GroupRank.DAY_WALKER);
            pass(c.leader(), c.b());
            CharterScan.scanNow(server(helper));
            helper.assertTrue(groups(helper).charterOf(group(helper, c.leader())).orElseThrow().holder().equals(Optional.of(c.b().getUUID())), "setup: b holds it");
            helper.assertTrue(Contests.challenge(c.a()) == null, "setup: the challenge");
            locked(helper, c, c.b(), "pending");
            helper.assertTrue(Contests.accept(c.leader()) == null, "setup: the accept");
            locked(helper, c, c.b(), "running");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void anUninvolvedMemberCanStillBeDemotedExiledOrKickedDuringAContest(GameTestHelper helper) {
        Clan c = ready(helper);
        TalkingDuelist d = vampire(helper, 6.5);
        online(c.leader(), c.a(), c.b(), d);
        try {
            helper.assertTrue(join(c.leader(), d), "setup: the fourth joined");
            groups(helper).setRank(c.b().getUUID(), GroupRank.DAY_WALKER);
            begin(helper, c, c.a());
            helper.assertTrue(net.ofatech.supernaturalbloodlines.parts.species.group.clan.ClanDiscipline.demote(c.leader(), id(c.b())) == null, "an uninvolved Day Walker could not be demoted");
            helper.assertTrue(PackCommand.kick(c.leader(), c.b().getUUID(), id(c.b())) == null, "an uninvolved member could not be kicked");
            helper.assertTrue(net.ofatech.supernaturalbloodlines.parts.species.group.clan.ClanDiscipline.exile(c.leader(), id(d)) == null, "an uninvolved member could not be exiled");
        } finally {
            done(helper, c.leader(), c.a(), c.b(), d);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void theLockEndsWithTheContest(GameTestHelper helper) {
        Clan c = ready(helper);
        try {
            begin(helper, c, c.a());
            midnight(helper);
            helper.assertTrue(contest(helper, c.leader()).isEmpty(), "setup: still running");
            helper.assertTrue(net.ofatech.supernaturalbloodlines.parts.species.group.clan.ClanDiscipline.demote(c.leader(), id(c.a())) == null, "still locked after the contest");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    // ---- resolve ----

    private static void midnight(GameTestHelper helper) {
        DAYTIME[0] = 18_000L;
        Contests.tick(server(helper), DAYTIME[0]);
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void theOnlineDayWalkerHoldingItAtMidnightLeads(GameTestHelper helper) {
        Clan c = ready(helper);
        try {
            UUID group = group(helper, c.leader());
            CharterRow before = groups(helper).charterOf(group).orElseThrow();
            begin(helper, c, c.a());
            pass(c.leader(), c.a());
            DAYTIME[0] = 17_999L;
            Contests.tick(server(helper), DAYTIME[0]);
            helper.assertTrue(contest(helper, c.leader()).isPresent(), "it ended before midnight");
            midnight(helper);
            helper.assertTrue(leaderOf(helper, c.b()).equals(c.a().getUUID()) && leaderOf(helper, c.leader()).equals(c.a().getUUID()), "the lead did not move");
            helper.assertTrue(groups(helper).rankOf(c.leader().getUUID(), GroupType.CLAN) == GroupRank.DAY_WALKER, "the old head is not a Day Walker");
            helper.assertTrue(groups(helper).rankOf(c.a().getUUID(), GroupType.CLAN) == GroupRank.NONE, "the new head holds a rank row");
            helper.assertTrue(groups(helper).existingGroupOf(c.a().getUUID()).equals(Optional.of(group)), "the group id did not follow");
            CharterRow after = groups(helper).charterOf(group).orElseThrow();
            helper.assertTrue(after.id().equals(before.id()) && after.state() == CharterState.HELD && after.holder().equals(Optional.of(c.a().getUUID())), "the Charter row is " + after);
            helper.assertTrue(groups(helper).contestOf(group).isEmpty(), "the contest row stayed");
            helper.assertTrue(c.leader().received(CLAN + "contest_new_head") && c.a().received(CLAN + "contest_new_head") && c.b().received(CLAN + "contest_new_head"), "somebody was not told");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    private static void unchanged(GameTestHelper helper, Clan c, String why) {
        helper.assertTrue(leaderOf(helper, c.a()).equals(c.leader().getUUID()), why + ": the lead moved");
        helper.assertTrue(contest(helper, c.leader()).isEmpty(), why + ": the contest row stayed");
        helper.assertTrue(c.leader().received(CLAN + "contest_nochange"), why + ": the clan was not told");
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void theLeaderHoldingItKeepsTheLead(GameTestHelper helper) {
        Clan c = ready(helper);
        try {
            begin(helper, c, c.a());
            midnight(helper);
            unchanged(helper, c, "the head holds it");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aNightWalkerHoldingItChangesNothing(GameTestHelper helper) {
        Clan c = ready(helper);
        try {
            begin(helper, c, c.a());
            pass(c.leader(), c.b());
            midnight(helper);
            unchanged(helper, c, "a Night Walker holds it");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void anOfflineHolderChangesNothing(GameTestHelper helper) {
        Clan c = ready(helper);
        try {
            begin(helper, c, c.a());
            pass(c.leader(), c.a());
            CharterScan.scanNow(server(helper));
            online(c.leader(), c.b());
            CharterScan.overridePlayers(() -> List.of(c.leader(), c.b()));
            midnight(helper);
            unchanged(helper, c, "the holder is offline");
            CharterRow row = groups(helper).charterOf(group(helper, c.leader())).orElseThrow();
            helper.assertTrue(row.state() == CharterState.HELD && row.holder().equals(Optional.of(c.a().getUUID())), "the row is " + row);
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aCharterInAChestChangesNothing(GameTestHelper helper) {
        Clan c = ready(helper);
        BlockPos chestPos = helper.absolutePos(new BlockPos(3, 2, 3));
        try {
            begin(helper, c, c.a());
            pass(c.leader(), c.a());
            CharterScan.scanNow(server(helper));
            helper.assertTrue(groups(helper).charterOf(group(helper, c.leader())).orElseThrow().holder().equals(Optional.of(c.a().getUUID())), "setup: not held by the Day Walker");
            helper.getLevel().setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
            ((ChestBlockEntity) helper.getLevel().getBlockEntity(chestPos)).setItem(0, takeCharter(c.a()));
            midnight(helper);
            unchanged(helper, c, "the Charter is in a chest");
        } finally {
            helper.getLevel().setBlock(chestPos, Blocks.AIR.defaultBlockState(), 3);
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aDroppedCharterChangesNothing(GameTestHelper helper) {
        Clan c = ready(helper);
        try {
            begin(helper, c, c.a());
            pass(c.leader(), c.a());
            CharterScan.scanNow(server(helper));
            ItemStack stack = takeCharter(c.a());
            ItemEntity e = new ItemEntity(helper.getLevel(), c.a().getX(), c.a().getY(), c.a().getZ(), stack);
            e.setNoGravity(true);
            helper.getLevel().addFreshEntity(e);
            midnight(helper);
            unchanged(helper, c, "the Charter is on the ground");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aDeadHolderChangesNothing(GameTestHelper helper) {
        Clan c = ready(helper);
        try {
            begin(helper, c, c.a());
            pass(c.leader(), c.a());
            CharterScan.scanNow(server(helper));
            c.a().setHealth(0f);
            midnight(helper);
            unchanged(helper, c, "the holder is dead");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aHolderInAnotherDimensionCounts(GameTestHelper helper) {
        Clan c = ready(helper);
        try {
            begin(helper, c, c.a());
            pass(c.leader(), c.a());
            var nether = server(helper).getLevel(net.minecraft.world.level.Level.NETHER);
            helper.assertTrue(nether != null, "setup: no Nether");
            c.a().setServerLevel(nether);
            c.a().setPos(0.5, 70, 0.5);
            helper.assertTrue(c.a().level() == nether, "setup: the player did not change dimension");
            midnight(helper);
            helper.assertTrue(leaderOf(helper, c.b()).equals(c.a().getUUID()), "a holder in the Nether did not lead");
        } finally {
            c.a().setServerLevel(helper.getLevel());
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void theChallengerNeedNotBeTheHolder(GameTestHelper helper) {
        Clan c = ready(helper);
        try {
            groups(helper).setRank(c.b().getUUID(), GroupRank.DAY_WALKER);
            begin(helper, c, c.a());
            pass(c.leader(), c.b());
            midnight(helper);
            helper.assertTrue(leaderOf(helper, c.a()).equals(c.b().getUUID()), "the holder did not lead");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aHolderWhoLeftTheClanChangesNothing(GameTestHelper helper) {
        Clan c = ready(helper);
        try {
            begin(helper, c, c.a());
            pass(c.leader(), c.b());
            helper.assertTrue(PackCommand.leave(c.b()) == null, "setup: leaving refused");
            midnight(helper);
            helper.assertTrue(leaderOf(helper, c.a()).equals(c.leader().getUUID()), "the lead moved");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aLostCharterEndsTheContestWithNoChange(GameTestHelper helper) {
        Clan c = ready(helper);
        try {
            begin(helper, c, c.a());
            UUID g = group(helper, c.leader());
            CharterRow r = groups(helper).charterOf(g).orElseThrow();
            groups(helper).setCharter(g, new CharterRow(r.id(), Optional.empty(), CharterState.LOST, r.forgedAt(), r.seenAt(), Optional.empty(), CLOCK[0]));
            long cooldown = groups(helper).cooldownUntil(g, "contest");
            Contests.tick(server(helper), DAYTIME[0]);
            helper.assertTrue(contest(helper, c.leader()).isEmpty() && leaderOf(helper, c.a()).equals(c.leader().getUUID()), "the contest did not end quietly");
            helper.assertTrue(groups(helper).cooldownUntil(g, "contest") == cooldown, "the cooldown changed");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void theOldLeaderIsADayWalkerAndCanChallengeAgainAfterTheCooldown(GameTestHelper helper) {
        Clan c = ready(helper);
        try {
            begin(helper, c, c.a());
            pass(c.leader(), c.a());
            midnight(helper);
            helper.assertTrue(leaderOf(helper, c.leader()).equals(c.a().getUUID()), "setup: the lead did not move");
            said(helper, Contests.challenge(c.leader()), ERR + "challenge_cooldown", "the old head challenged inside the week");
            CLOCK[0] += 7 * DAY;
            helper.assertTrue(Contests.challenge(c.leader()) == null, "the old head could not challenge after the week");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    // ---- time ----

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aSleepingSkipAcrossMidnightResolves(GameTestHelper helper) {
        Clan c = ready(helper);
        try {
            DAYTIME[0] = 13_000L;
            begin(helper, c, c.a());
            pass(c.leader(), c.a());
            Contests.tick(server(helper), 24_000L);
            helper.assertTrue(contest(helper, c.leader()).isEmpty() && leaderOf(helper, c.b()).equals(c.a().getUUID()), "a skip over midnight did not resolve it");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aStalledClockEndsAfterTheMaxHours(GameTestHelper helper) {
        Clan c = ready(helper);
        try {
            DAYTIME[0] = 6_000L;
            begin(helper, c, c.a());
            CLOCK[0] += 60 * MIN - 1;
            Contests.tick(server(helper), 6_000L);
            helper.assertTrue(contest(helper, c.leader()).isPresent(), "ended before the hour");
            CLOCK[0] += 1;
            Contests.tick(server(helper), 6_000L);
            helper.assertTrue(contest(helper, c.leader()).isEmpty(), "a stopped clock never ended it");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aContestCannotEndBeforeItsMinimum(GameTestHelper helper) {
        Clan c = ready(helper);
        try {
            GroupsConfig.override("contest_min_minutes", 2);
            DAYTIME[0] = 17_990L;
            begin(helper, c, c.a());
            Contests.tick(server(helper), 18_000L);
            Contests.tick(server(helper), 41_999L);
            helper.assertTrue(contest(helper, c.leader()).isPresent(), "it ended on the midnight inside the minimum");
            Contests.tick(server(helper), 42_000L);
            helper.assertTrue(contest(helper, c.leader()).isEmpty(), "it did not end at the next midnight");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aTimeSetBackwardsDoesNotResolve(GameTestHelper helper) {
        Clan c = ready(helper);
        try {
            DAYTIME[0] = 30_000L;
            begin(helper, c, c.a());
            Contests.tick(server(helper), 5_000L);
            helper.assertTrue(contest(helper, c.leader()).isPresent(), "a step backwards resolved it");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    // ---- void ----

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void theLeaderLeavingMidContestDoesNotVoidIt(GameTestHelper helper) {
        Clan c = ready(helper);
        try {
            begin(helper, c, c.a());
            pass(c.leader(), c.a());
            online(c.a(), c.b());
            CharterScan.overridePlayers(() -> List.of(c.a(), c.b()));
            Contests.tick(server(helper), 17_500L);
            helper.assertTrue(contest(helper, c.leader()).isPresent(), "the head going offline voided it");
            midnight(helper);
            helper.assertTrue(leaderOf(helper, c.b()).equals(c.a().getUUID()), "it did not resolve with the head away");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void theChallengerLeavingDoesNotVoidIt(GameTestHelper helper) {
        Clan c = ready(helper);
        try {
            groups(helper).setRank(c.b().getUUID(), GroupRank.DAY_WALKER);
            begin(helper, c, c.a());
            pass(c.leader(), c.b());
            helper.assertTrue(PackCommand.leave(c.a()) == null, "setup: leaving refused");
            helper.assertTrue(contest(helper, c.leader()).isPresent(), "the challenger leaving voided it");
            midnight(helper);
            helper.assertTrue(leaderOf(helper, c.leader()).equals(c.b().getUUID()), "it did not resolve");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 200)
    public static void aTypeSwitchVoidsTheContestAndKeepsTheCharterDormant(GameTestHelper helper) {
        Clan c = ready(helper);
        try {
            begin(helper, c, c.a());
            UUID g = group(helper, c.leader());
            de.teamlapen.vampirism.api.VampirismAPI.factionPlayerHandler(c.leader()).leaveFaction(false);
            GroupSwitcher.refresh(c.leader());
            helper.assertTrue(groups(helper).contestOf(g).isEmpty(), "the contest survived the switch");
            helper.assertTrue(groups(helper).charterOf(g).isPresent(), "the Charter row went with the switch");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void clansOffVoidsIt(GameTestHelper helper) {
        Clan c = ready(helper);
        try {
            begin(helper, c, c.a());
            GroupsConfig.override("clans_enabled", false);
            Contests.tick(server(helper), DAYTIME[0]);
            helper.assertTrue(contest(helper, c.leader()).isEmpty() && groups(helper).charterOf(group(helper, c.leader())).isPresent(), "the contest stayed or the Charter went");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aLeaderChangeByAbsenceVoidsIt(GameTestHelper helper) {
        Clan c = ready(helper);
        try {
            begin(helper, c, c.a());
            UUID g = group(helper, c.leader());
            long cooldown = groups(helper).cooldownUntil(g, "contest");
            GroupLead.handOver(server(helper), PackIndex.get(helper.getLevel()), c.leader().getUUID(), c.b().getUUID(), GroupType.CLAN);
            helper.assertTrue(groups(helper).contestOf(g).isEmpty(), "the contest survived a lead change by another path");
            helper.assertTrue(groups(helper).cooldownUntil(g, "contest") == cooldown, "a new cooldown was stamped");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aDissolvedClanDropsTheRows(GameTestHelper helper) {
        Clan c = ready(helper);
        try {
            begin(helper, c, c.a());
            UUID g = group(helper, c.leader());
            groups(helper).drop(c.leader().getUUID());
            helper.assertTrue(groups(helper).contestOf(g).isEmpty() && groups(helper).charterOf(g).isEmpty(), "rows stayed");
            Contests.tick(server(helper), DAYTIME[0]);
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aRestartKeepsTheContest(GameTestHelper helper) {
        Clan c = ready(helper);
        try {
            DAYTIME[0] = 13_000L;
            begin(helper, c, c.a());
            UUID g = group(helper, c.leader());
            ContestRow before = groups(helper).contestOf(g).orElseThrow();
            GroupIndex back = GroupIndex.load(groups(helper).save(new CompoundTag(), helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
            helper.assertTrue(back.contestOf(g).equals(Optional.of(before)) && before.startedDayTime() == 13_000L, "the row did not survive a save: " + back.contestOf(g));
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 200)
    public static void twoClansHaveTwoContests(GameTestHelper helper) {
        Clan one = ready(helper, 1.5, true, true);
        TalkingDuelist l2 = vampire(helper, 6.5);
        TalkingDuelist a2 = vampire(helper, 7.5);
        TalkingDuelist b2 = vampire(helper, 8.5);
        try {
            online(one.leader(), one.a(), one.b(), l2, a2, b2);
            helper.assertTrue(join(l2, a2) && join(l2, b2), "setup: no second clan");
            groups(helper).setRank(a2.getUUID(), GroupRank.DAY_WALKER);
            CharterScan.overridePlayers(() -> List.of(one.leader(), one.a(), one.b(), l2, a2, b2));
            l2.getInventory().add(new ItemStack(Items.WRITABLE_BOOK));
            l2.getInventory().add(new ItemStack(Items.GOLD_BLOCK));
            helper.assertTrue(Charters.forge(l2) == null, "setup: the second forge was refused");
            begin(helper, one, one.a());
            helper.assertTrue(Contests.challenge(a2) == null && Contests.accept(l2) == null, "the second clan could not run its own contest");
            pass(one.leader(), one.a());
            midnight(helper);
            helper.assertTrue(leaderOf(helper, one.b()).equals(one.a().getUUID()), "the first clan did not resolve");
            helper.assertTrue(leaderOf(helper, b2).equals(l2.getUUID()), "the second clan's head was moved by the first's contest");
        } finally {
            done(helper, one.leader(), one.a(), one.b(), l2, a2, b2);
        }
        helper.succeed();
    }

    // ---- plumbing ----

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void theCardReachesTheLeaderOnly(GameTestHelper helper) {
        Clan c = ready(helper);
        try {
            helper.assertTrue(Contests.challenge(c.a()) == null, "setup");
            helper.assertTrue(HubRequests.of(c.leader()).stream().anyMatch(r -> r.kind() == HubRequests.Kind.CHARTER_CONTEST), "the leader has no card");
            helper.assertTrue(HubRequests.of(c.a()).stream().noneMatch(r -> r.kind() == HubRequests.Kind.CHARTER_CONTEST)
                    && HubRequests.of(c.b()).stream().noneMatch(r -> r.kind() == HubRequests.Kind.CHARTER_CONTEST), "a member has the card");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void theAcceptRowRunsGroupChallengeAccept(GameTestHelper helper) {
        helper.assertTrue(AdminCommands.command(AdminCommands.byId("C12").orElseThrow(), List.of()).equals(Optional.of("group challenge accept")), "C12 runs " + AdminCommands.command(AdminCommands.byId("C12").orElseThrow(), List.of()));
        helper.assertTrue(AdminCommands.command(AdminCommands.byId("C11").orElseThrow(), List.of()).equals(Optional.of("group challenge")), "C11 runs " + AdminCommands.command(AdminCommands.byId("C11").orElseThrow(), List.of()));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void theChallengeLiteralStillRoutesAPackToTheDuelAndACovenToTheRegentCall(GameTestHelper helper) {
        TalkingDuelist w = witch(helper, 1.5);
        TalkingDuelist wm = human(helper, 2.5);
        TalkingDuelist wm2 = human(helper, 3.5);
        online(w, wm, wm2);
        try {
            helper.assertTrue(join(w, wm) && join(w, wm2) && Groups.typeOf(wm) == GroupType.COVEN, "setup: no coven");
            CommandDispatcher<CommandSourceStack> d = new CommandDispatcher<>();
            GroupCommand.register(d);
            for (String line : new String[] {"group challenge accept", "group challenge decline", "group challenge"}) {
                try {
                    d.execute(line, w.createCommandSourceStack());
                } catch (com.mojang.brigadier.exceptions.CommandSyntaxException ignored) {
                    // a failure reply is the answer being read
                }
                helper.assertTrue(w.received("supernaturalbloodlines.group.wrong_type.coven"), "'" + line + "' did not answer wrong_type for a coven");
            }
        } finally {
            done(helper, w, wm, wm2);
        }
        helper.succeed();
    }
}
