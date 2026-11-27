package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.clean;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.human;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.join;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.key;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.online;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.vampire;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.witch;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.group.GroupRank;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers.TalkingDuelist;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupCommand;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupGate;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupLead;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupSwitcher;
import net.ofatech.supernaturalbloodlines.parts.species.group.Groups;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.group.clan.ClanDiscipline;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackCommand;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;

/** Plan 20c Task 4 (R270, R276): Night Walkers and Day Walkers, promotion, demotion, the clock, and exile. Runs with or without Werewolves. */
@PrefixGameTestTemplate(false)
public final class ClanRankGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final long DAY = 24L * 60 * 60 * 1000;
    private static final String ERR = "supernaturalbloodlines.clan.err.";
    private static final String CLAN = "supernaturalbloodlines.clan.";

    private ClanRankGameTests() {
    }

    private record Clan(TalkingDuelist leader, TalkingDuelist a, TalkingDuelist b) {
        TalkingDuelist[] all() {
            return new TalkingDuelist[] {leader, a, b};
        }
    }

    private static Clan clan(GameTestHelper helper) {
        TalkingDuelist l = vampire(helper, 1.5);
        TalkingDuelist a = vampire(helper, 2.5);
        TalkingDuelist b = vampire(helper, 3.5);
        online(l, a, b);
        helper.assertTrue(join(l, a) && join(l, b) && Groups.typeOf(a) == GroupType.CLAN, "no clan formed");
        return new Clan(l, a, b);
    }

    private static GroupIndex groups(GameTestHelper helper) {
        return GroupIndex.get(helper.getLevel());
    }

    private static UUID groupOf(GameTestHelper helper, TalkingDuelist leader) {
        return groups(helper).existingGroupOf(leader.getUUID()).orElseThrow();
    }

    private static GroupRank rank(GameTestHelper helper, TalkingDuelist p) {
        return groups(helper).rankOf(p.getUUID(), GroupType.CLAN);
    }

    private static String name(TalkingDuelist p) {
        return p.getUUID().toString(); // a test player is in no player list, so the server knows no name for it: the commands take the id too
    }

    private static MinecraftServer server(GameTestHelper helper) {
        return helper.getLevel().getServer();
    }

    private static void said(GameTestHelper helper, Component c, String key, String why) {
        helper.assertTrue(key.equals(key(c)), why + ": " + key(c));
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aNewMemberIsANightWalker(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            helper.assertTrue(rank(helper, c.a()) == GroupRank.NONE && rank(helper, c.b()) == GroupRank.NONE, "a new member holds a rank");
        } finally {
            clean(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void theLeaderPromotesAndDemotes(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            helper.assertTrue(ClanDiscipline.promote(c.leader(), name(c.a())) == null, "the promotion was refused");
            helper.assertTrue(rank(helper, c.a()) == GroupRank.DAY_WALKER, "no Day Walker");
            helper.assertTrue(c.a().received(CLAN + "promoted") && c.leader().received(CLAN + "promoted_ok"), "nobody was told");
            helper.assertTrue(ClanDiscipline.demote(c.leader(), name(c.a())) == null, "the demotion was refused");
            helper.assertTrue(rank(helper, c.a()) == GroupRank.NONE, "still a Day Walker");
            helper.assertTrue(groups(helper).cooldownUntil(groupOf(helper, c.leader()), "nodw:" + c.a().getUUID()) == Long.MAX_VALUE, "no mark that keeps the clock from promoting again");
            helper.assertTrue(c.a().received(CLAN + "demoted") && c.leader().received(CLAN + "demoted_ok"), "nobody was told of the demotion");
        } finally {
            clean(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aNightWalkerCannotPromote(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            said(helper, ClanDiscipline.promote(c.a(), name(c.b())), ERR + "leader_only", "a Night Walker promoted");
            said(helper, ClanDiscipline.demote(c.a(), name(c.b())), ERR + "leader_only", "a Night Walker demoted");
            helper.assertTrue(rank(helper, c.b()) == GroupRank.NONE, "the target changed");
        } finally {
            clean(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aDayWalkerCannotPromote(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            groups(helper).setRank(c.a().getUUID(), GroupRank.DAY_WALKER);
            said(helper, ClanDiscipline.promote(c.a(), name(c.b())), ERR + "leader_only", "a Day Walker promoted");
            said(helper, ClanDiscipline.exile(c.a(), name(c.b())), ERR + "leader_only", "a Day Walker exiled");
            helper.assertTrue(rank(helper, c.b()) == GroupRank.NONE && PackIndex.get(helper.getLevel()).inPack(c.b().getUUID()), "the target changed");
        } finally {
            clean(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void theLeaderCannotBeTargeted(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            said(helper, ClanDiscipline.promote(c.leader(), name(c.leader())), ERR + "leader_target", "the leader was promoted");
            said(helper, ClanDiscipline.demote(c.leader(), name(c.leader())), ERR + "leader_target", "the leader was demoted");
            said(helper, ClanDiscipline.exile(c.leader(), name(c.leader())), ERR + "leader_target", "the leader was exiled");
            helper.assertTrue(rank(helper, c.leader()) == GroupRank.NONE, "the leader holds a stored rank");
        } finally {
            clean(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void theLeaderCannotExileThemselves(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            said(helper, ClanDiscipline.exile(c.leader(), c.leader().getUUID().toString()), ERR + "leader_target", "the leader exiled themselves by id");
            helper.assertTrue(PackIndex.get(helper.getLevel()).betasOf(c.leader().getUUID()).size() == 2, "somebody left");
        } finally {
            clean(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void anOutsiderCannotBeTargeted(GameTestHelper helper) {
        Clan c = clan(helper);
        TalkingDuelist stranger = human(helper, 4.5);
        online(c.leader(), c.a(), c.b(), stranger);
        try {
            said(helper, ClanDiscipline.promote(c.leader(), name(stranger)), ERR + "not_member", "an outsider was promoted");
            said(helper, ClanDiscipline.exile(c.leader(), name(stranger)), ERR + "not_member", "an outsider was exiled");
            said(helper, ClanDiscipline.promote(c.leader(), "nobody-by-this-name"), ERR + "not_member", "a made-up name");
        } finally {
            clean(helper, c.leader(), c.a(), c.b(), stranger);
        }
        helper.succeed();
    }

    private static TalkingDuelist[] group(GameTestHelper helper, TalkingDuelist leader, TalkingDuelist member, GroupType type) {
        online(leader, member);
        if (type == GroupType.PACK) {
            PackIndex.get(helper.getLevel()).join(leader.getUUID(), member.getUUID());
            groups(helper).ensure(leader.getUUID(), GroupType.PACK, PackIndex.now());
        } else {
            helper.assertTrue(join(leader, member), "no " + type);
        }
        return new TalkingDuelist[] {leader, member};
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void promoteInAPackAnswersWrongType(GameTestHelper helper) {
        TalkingDuelist[] g = group(helper, human(helper, 1.5), human(helper, 2.5), GroupType.PACK);
        try {
            said(helper, ClanDiscipline.promote(g[0], name(g[1])), "supernaturalbloodlines.group.wrong_type.pack", "a pack leader promoted");
            said(helper, ClanDiscipline.exile(g[0], name(g[1])), "supernaturalbloodlines.group.wrong_type.pack", "a pack leader exiled");
            helper.assertTrue(PackIndex.get(helper.getLevel()).inPack(g[1].getUUID()), "the member left");
        } finally {
            clean(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void promoteInACovenAnswersWrongType(GameTestHelper helper) {
        TalkingDuelist[] g = group(helper, witch(helper, 1.5), human(helper, 2.5), GroupType.COVEN);
        try {
            said(helper, ClanDiscipline.promote(g[0], name(g[1])), "supernaturalbloodlines.group.wrong_type.coven", "a Regent promoted");
            said(helper, ClanDiscipline.demote(g[0], name(g[1])), "supernaturalbloodlines.group.wrong_type.coven", "a Regent demoted");
        } finally {
            clean(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void promoteWhenClansAreOffAnswersGroupOff(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            GroupsConfig.override("clans_enabled", false);
            said(helper, ClanDiscipline.promote(c.leader(), name(c.a())), "supernaturalbloodlines.group.group_off.clan", "promoted with clans off");
            said(helper, ClanDiscipline.exile(c.leader(), name(c.a())), "supernaturalbloodlines.group.group_off.clan", "exiled with clans off");
            helper.assertTrue(rank(helper, c.a()) == GroupRank.NONE && PackIndex.get(helper.getLevel()).inPack(c.a().getUUID()), "something changed");
        } finally {
            clean(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void autoPromotionAfterTheDays(GameTestHelper helper) {
        long[] now = {1_000_000_000L};
        PackIndex.overrideClock(() -> now[0]);
        Clan c = clan(helper);
        try {
            now[0] += 14 * DAY - 1;
            ClanDiscipline.sweep(server(helper));
            helper.assertTrue(rank(helper, c.a()) == GroupRank.NONE, "promoted a millisecond early");
            now[0] += 1;
            ClanDiscipline.sweep(server(helper));
            helper.assertTrue(rank(helper, c.a()) == GroupRank.DAY_WALKER && rank(helper, c.b()) == GroupRank.DAY_WALKER, "not promoted after the days");
            helper.assertTrue(rank(helper, c.leader()) == GroupRank.NONE, "the leader got a rank");
            helper.assertTrue(c.a().received(CLAN + "auto_promoted"), "the member was not told");
        } finally {
            clean(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aMemberWhoDoesNotFitAClanIsNeverPromoted(GameTestHelper helper) {
        long[] now = {1_000_000_000L};
        PackIndex.overrideClock(() -> now[0]);
        Clan c = clan(helper);
        TalkingDuelist h = human(helper, 6.5);
        online(c.leader(), c.a(), c.b(), h);
        try {
            helper.assertTrue(join(c.leader(), h), "setup: the human joined");
            said(helper, ClanDiscipline.promote(c.leader(), name(h)), ERR + "cannot_promote", "a human was promoted by the head");
            helper.assertTrue(rank(helper, h) == GroupRank.NONE, "a human holds a rank");
            now[0] += 15 * DAY;
            ClanDiscipline.sweep(server(helper));
            helper.assertTrue(rank(helper, h) == GroupRank.NONE, "the clock promoted a human");
            helper.assertTrue(rank(helper, c.a()) == GroupRank.DAY_WALKER, "the clock did not promote a vampire");
        } finally {
            clean(helper, c.leader(), c.a(), c.b(), h);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void fitsIsTheClanKindRuleOfTheTypeDerivation(GameTestHelper helper) {
        TalkingDuelist p = vampire(helper, 1.5);
        try {
            for (net.ofatech.supernaturalbloodlines.core.bloodline.Species s : net.ofatech.supernaturalbloodlines.core.bloodline.Species.values()) {
                net.ofatech.supernaturalbloodlines.core.BloodlineAccess.set(p, net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState.NONE.toBuilder().species(s).build());
                boolean clanKind = net.ofatech.supernaturalbloodlines.core.group.GroupTypes.of(new net.ofatech.supernaturalbloodlines.core.group.GroupTypes.Leader(s, true, false, false, true)) == GroupType.CLAN;
                helper.assertTrue(net.ofatech.supernaturalbloodlines.parts.species.group.clan.ClanScope.fits(p) == clanKind, "a vampire of species " + s + (clanKind ? " should fit" : " should not fit") + " a clan");
            }
        } finally {
            net.ofatech.supernaturalbloodlines.core.BloodlineAccess.set(p, net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState.NONE);
            clean(helper, p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void dayWalkerIsOneRuleForEveryPlace(GameTestHelper helper) {
        Clan c = clan(helper);
        TalkingDuelist h = human(helper, 6.5);
        TalkingDuelist stranger = vampire(helper, 7.5);
        online(c.leader(), c.a(), c.b(), h, stranger);
        try {
            helper.assertTrue(join(c.leader(), h), "setup: the human joined");
            var level = helper.getLevel();
            groups(helper).setRank(c.a().getUUID(), GroupRank.DAY_WALKER);
            groups(helper).setRank(h.getUUID(), GroupRank.DAY_WALKER); // a rank a member who does not fit holds anyway (set by an older build, or by a kind that changed)
            groups(helper).setRank(stranger.getUUID(), GroupRank.DAY_WALKER);
            helper.assertTrue(net.ofatech.supernaturalbloodlines.parts.species.group.clan.ClanScope.dayWalker(level, c.leader().getUUID()), "the head is not a Day Walker for the rules");
            helper.assertTrue(net.ofatech.supernaturalbloodlines.parts.species.group.clan.ClanScope.dayWalker(level, c.a().getUUID()), "a vampire Day Walker is not one");
            helper.assertTrue(!net.ofatech.supernaturalbloodlines.parts.species.group.clan.ClanScope.dayWalker(level, c.b().getUUID()), "a Night Walker is one");
            helper.assertTrue(!net.ofatech.supernaturalbloodlines.parts.species.group.clan.ClanScope.dayWalker(level, h.getUUID()), "a human with a stored rank is one");
            helper.assertTrue(!net.ofatech.supernaturalbloodlines.parts.species.group.clan.ClanScope.dayWalker(level, stranger.getUUID()), "a vampire of no clan is one");
        } finally {
            clean(helper, c.leader(), c.a(), c.b(), h, stranger);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void autoPromotionIsOffAtZero(GameTestHelper helper) {
        long[] now = {1_000_000_000L};
        PackIndex.overrideClock(() -> now[0]);
        Clan c = clan(helper);
        try {
            GroupsConfig.override("daywalker_days", 0);
            now[0] += 1000 * DAY;
            ClanDiscipline.sweep(server(helper));
            helper.assertTrue(rank(helper, c.a()) == GroupRank.NONE, "promoted with the clock off");
        } finally {
            clean(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aDemotedMemberIsNotAutoPromotedAgain(GameTestHelper helper) {
        long[] now = {1_000_000_000L};
        PackIndex.overrideClock(() -> now[0]);
        Clan c = clan(helper);
        try {
            now[0] += 15 * DAY;
            ClanDiscipline.sweep(server(helper));
            helper.assertTrue(rank(helper, c.a()) == GroupRank.DAY_WALKER, "setup: not promoted");
            helper.assertTrue(ClanDiscipline.demote(c.leader(), name(c.a())) == null, "demotion refused");
            now[0] += 100 * DAY;
            ClanDiscipline.sweep(server(helper));
            helper.assertTrue(rank(helper, c.a()) == GroupRank.NONE, "the clock promoted a demoted member again");
            helper.assertTrue(ClanDiscipline.promote(c.leader(), name(c.a())) == null && rank(helper, c.a()) == GroupRank.DAY_WALKER, "the leader cannot promote them again");
        } finally {
            clean(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void thePromotedAreToldAndAnOfflineOneAtLogin(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            online(c.leader(), c.b());
            helper.assertTrue(ClanDiscipline.promote(c.leader(), name(c.a())) == null, "refused");
            helper.assertTrue(PackIndex.get(helper.getLevel()).takeNotices(c.a().getUUID()).stream().anyMatch(n -> n[0].equals(CLAN + "promoted")), "an offline member has no notice waiting");
            online(c.leader(), c.a(), c.b());
            helper.assertTrue(ClanDiscipline.promote(c.leader(), name(c.b())) == null && c.b().received(CLAN + "promoted"), "an online member was not told");
        } finally {
            clean(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void leavingClearsTheRankAndTheMark(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            UUID group = groupOf(helper, c.leader());
            ClanDiscipline.promote(c.leader(), name(c.a()));
            ClanDiscipline.demote(c.leader(), name(c.a()));
            ClanDiscipline.promote(c.leader(), name(c.b()));
            helper.assertTrue(groups(helper).cooldownUntil(group, "nodw:" + c.a().getUUID()) != 0, "setup: no mark");
            helper.assertTrue(PackCommand.leave(c.a()) == null && PackCommand.leave(c.b()) == null, "leaving was refused");
            helper.assertTrue(rank(helper, c.b()) == GroupRank.NONE && groups(helper).cooldownUntil(group, "nodw:" + c.a().getUUID()) == 0, "the rank or the mark stayed");
        } finally {
            clean(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void joiningAnotherClanStartsAsANightWalker(GameTestHelper helper) {
        Clan c = clan(helper);
        TalkingDuelist other = vampire(helper, 5.5);
        online(c.leader(), c.a(), c.b(), other);
        try {
            ClanDiscipline.promote(c.leader(), name(c.a()));
            helper.assertTrue(PackCommand.leave(c.a()) == null && join(other, c.a()), "could not move to the other clan");
            helper.assertTrue(rank(helper, c.a()) == GroupRank.NONE, "the rank came along");
        } finally {
            clean(helper, c.leader(), c.a(), c.b(), other);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void exileRemovesBarsAndTellsTheMember(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            UUID group = groupOf(helper, c.leader());
            helper.assertTrue(ClanDiscipline.exile(c.leader(), name(c.a())) == null, "refused");
            helper.assertTrue(!PackIndex.get(helper.getLevel()).inPack(c.a().getUUID()), "still in the clan");
            helper.assertTrue(GroupGate.exiled(server(helper), group, c.a().getUUID()) && !GroupGate.exiled(server(helper), group, c.b().getUUID()), "the ban is on the wrong people");
            helper.assertTrue(c.a().received(CLAN + "exiled") && c.leader().received(CLAN + "exiled_ok"), "nobody was told");
            helper.assertTrue(groups(helper).banUntil(group.toString(), c.a().getUUID()) > PackIndex.now(), "no ban row under the group id");
        } finally {
            clean(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void exileClearsTheDayWalkerRank(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            ClanDiscipline.promote(c.leader(), name(c.a()));
            ClanDiscipline.exile(c.leader(), name(c.a()));
            helper.assertTrue(rank(helper, c.a()) == GroupRank.NONE, "an exiled member kept DAY_WALKER");
        } finally {
            clean(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void anExiledPlayerIsToldAtLoginWhenOffline(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            online(c.leader(), c.b());
            helper.assertTrue(ClanDiscipline.exile(c.leader(), name(c.a())) == null, "refused");
            helper.assertTrue(PackIndex.get(helper.getLevel()).takeNotices(c.a().getUUID()).stream().anyMatch(n -> n[0].equals(CLAN + "exiled")), "no notice waits for the offline member");
        } finally {
            clean(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void theLeaderInvitingAnExiledPlayerHearsExiledStill(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            ClanDiscipline.exile(c.leader(), name(c.a()));
            Component still = PackCommand.invite(c.leader(), c.a());
            said(helper, still, ERR + "exiled_still", "the invite was not refused with the leader's own line");
            helper.assertTrue("7".equals(((net.minecraft.network.chat.contents.TranslatableContents) still.getContents()).getArgs()[1].toString()), "the refusal does not say how many days are left: " + still);
            helper.assertTrue(PackCommand.requests().latestFor(c.a().getUUID(), helper.getLevel().getGameTime()).isEmpty(), "an invitation was sent anyway");
        } finally {
            clean(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void anExiledPlayerCannotRejoinThatClan(GameTestHelper helper) {
        Clan c = clan(helper);
        TalkingDuelist outsider = human(helper, 4.5);
        online(c.leader(), c.a(), c.b(), outsider);
        try {
            helper.assertTrue(PackCommand.invite(c.leader(), outsider) == null, "setup: the invite was refused");
            String token = PackCommand.requests().tokenOf(c.leader().getUUID(), outsider.getUUID()).orElseThrow();
            groups(helper).ban(groupOf(helper, c.leader()).toString(), outsider.getUUID(), PackIndex.now() + 7 * DAY);
            Component refusal = PackCommand.accept(outsider, token);
            said(helper, refusal, "supernaturalbloodlines.group.refused.clan", "the exiled player's accept was not the generic refusal");
            helper.assertTrue(!key(refusal).contains("exile") && !PackIndex.get(helper.getLevel()).inPack(outsider.getUUID()), "the refusal names the ban or the player joined");
        } finally {
            clean(helper, c.leader(), c.a(), c.b(), outsider);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void exileIsBarredOnlyFromThatClan(GameTestHelper helper) {
        Clan c = clan(helper);
        TalkingDuelist otherClan = vampire(helper, 5.5);
        TalkingDuelist wicca = witch(helper, 6.5);
        online(c.leader(), c.a(), c.b(), otherClan, wicca);
        try {
            ClanDiscipline.exile(c.leader(), name(c.a()));
            helper.assertTrue(join(otherClan, c.a()) && Groups.typeOf(c.a()) == GroupType.CLAN, "could not join another clan");
            helper.assertTrue(PackCommand.leave(c.a()) == null && join(wicca, c.a()) && Groups.typeOf(c.a()) == GroupType.COVEN, "could not join a coven");
        } finally {
            clean(helper, c.leader(), c.a(), c.b(), otherClan, wicca);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void exileSurvivesTheLeadMoving(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            UUID group = groupOf(helper, c.leader());
            ClanDiscipline.exile(c.leader(), name(c.a()));
            GroupLead.handOver(server(helper), PackIndex.get(helper.getLevel()), c.leader().getUUID(), c.b().getUUID(), GroupType.CLAN);
            helper.assertTrue(groupOf(helper, c.b()).equals(group), "the group id did not follow the lead");
            helper.assertTrue(GroupGate.exiled(server(helper), group, c.a().getUUID()), "the ban was lost when the lead moved");
            said(helper, PackCommand.invite(c.b(), c.a()), ERR + "exiled_still", "the new leader could invite the exiled player");
        } finally {
            clean(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aLeaderWhoWouldBringAnExiledMemberIsRefused(GameTestHelper helper) {
        Clan c = clan(helper);
        TalkingDuelist rival = vampire(helper, 5.5);
        TalkingDuelist follower = human(helper, 6.5);
        online(c.leader(), c.a(), c.b(), rival, follower);
        try {
            helper.assertTrue(join(rival, follower), "setup: no second clan");
            helper.assertTrue(PackCommand.invite(c.leader(), rival) == null, "setup: the leader's invite to the rival was refused");
            String token = PackCommand.requests().tokenOf(c.leader().getUUID(), rival.getUUID()).orElseThrow();
            // the rival's follower is exiled from the first clan after the invitation went out: the rival would bring them
            groups(helper).ban(groupOf(helper, c.leader()).toString(), follower.getUUID(), PackIndex.now() + 7 * DAY);
            Component refusal = PackCommand.accept(rival, token);
            said(helper, refusal, "supernaturalbloodlines.group.refused.clan", "a leader bringing an exiled member was not refused generically");
            helper.assertTrue(PackIndex.get(helper.getLevel()).leaderOf(follower.getUUID()).equals(rival.getUUID()), "the clans merged");
        } finally {
            clean(helper, c.leader(), c.a(), c.b(), rival, follower);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void theHeadHearsWhoIsBroughtAndWhoIsExiled(GameTestHelper helper) {
        Clan c = clan(helper);
        TalkingDuelist rival = vampire(helper, 5.5);
        TalkingDuelist follower = human(helper, 6.5);
        online(c.leader(), c.a(), c.b(), rival, follower);
        try {
            helper.assertTrue(join(rival, follower), "setup: no second clan");
            groups(helper).ban(groupOf(helper, c.leader()).toString(), follower.getUUID(), PackIndex.now() + 7 * DAY);
            Component refusal = PackCommand.invite(c.leader(), rival);
            said(helper, refusal, ERR + "exiled_brings", "the head was not told who the rival brings");
            var args = ((net.minecraft.network.chat.contents.TranslatableContents) refusal.getContents()).getArgs();
            helper.assertTrue(args.length == 2 && args[0].toString().contains(rival.getUUID().toString().substring(0, 8)) && args[1].toString().contains(follower.getUUID().toString().substring(0, 8)), "the refusal does not name both: " + java.util.Arrays.toString(args));
            helper.assertTrue(PackCommand.requests().latestFor(rival.getUUID(), helper.getLevel().getGameTime()).isEmpty(), "an invitation was sent anyway");
        } finally {
            clean(helper, c.leader(), c.a(), c.b(), rival, follower);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void theExiledPlayerHearsTheDaysAndTheEnd(GameTestHelper helper) {
        long[] now = {2_000_000_000L};
        PackIndex.overrideClock(() -> now[0]);
        Clan c = clan(helper);
        try {
            helper.assertTrue(ClanDiscipline.exile(c.leader(), name(c.a())) == null, "refused");
            Component days = c.a().message(CLAN + "exiled_for");
            Component told = c.a().message(CLAN + "exiled_until");
            helper.assertTrue(days != null && told != null, "the exiled player was not told the days and the end");
            helper.assertTrue("7".equals(((net.minecraft.network.chat.contents.TranslatableContents) days.getContents()).getArgs()[0].toString()), "the days are their own argument: " + days);
            String arg = ((net.minecraft.network.chat.contents.TranslatableContents) told.getContents()).getArgs()[0].toString();
            helper.assertTrue(arg.contains(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(java.time.ZoneOffset.UTC).format(java.time.Instant.ofEpochMilli(now[0] + 7 * DAY))) && arg.endsWith("UTC"), "the end is " + arg);
            GroupsConfig.override("exile_days", 1);
            helper.assertTrue(ClanDiscipline.exile(c.leader(), name(c.b())) == null, "the second exile was refused");
            helper.assertTrue(c.b().message(CLAN + "exiled_for.one") != null && c.b().message(CLAN + "exiled_for") == null, "one day is not said as one day");
            helper.assertTrue(c.leader().message(CLAN + "exiled_ok.one") != null, "the head was not told in the singular");
        } finally {
            clean(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void anEndedExileRowIsForgottenBySweepEvenWithCovensOff(GameTestHelper helper) {
        long[] now = {2_000_000_000L};
        PackIndex.overrideClock(() -> now[0]);
        Clan c = clan(helper);
        try {
            UUID group = groupOf(helper, c.leader());
            ClanDiscipline.exile(c.leader(), name(c.a()));
            helper.assertTrue(groups(helper).banUntil(group.toString(), c.a().getUUID()) > 0, "setup: no ban row");
            now[0] += 8 * DAY;
            ClanDiscipline.sweep(server(helper));
            helper.assertTrue(groups(helper).banUntil(group.toString(), c.a().getUUID()) == 0, "an ended exile row is still in the index");
        } finally {
            clean(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void exileEndsAfterTheDays(GameTestHelper helper) {
        long[] now = {2_000_000_000L};
        PackIndex.overrideClock(() -> now[0]);
        Clan c = clan(helper);
        try {
            UUID group = groupOf(helper, c.leader());
            ClanDiscipline.exile(c.leader(), name(c.a()));
            now[0] += 7 * DAY - 1;
            helper.assertTrue(GroupGate.exiled(server(helper), group, c.a().getUUID()), "the ban ended early");
            now[0] += 1;
            helper.assertTrue(!GroupGate.exiled(server(helper), group, c.a().getUUID()), "the ban did not end");
            helper.assertTrue(join(c.leader(), c.a()), "the exiled player could not return when the days were over");
        } finally {
            clean(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void kickHasNoBan(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            UUID group = groupOf(helper, c.leader());
            helper.assertTrue(PackCommand.kickNamed(c.leader(), name(c.a())) == null, "the kick was refused");
            helper.assertTrue(!GroupGate.exiled(server(helper), group, c.a().getUUID()) && join(c.leader(), c.a()), "a kick barred the member");
        } finally {
            clean(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void membersMayAlwaysLeave(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            UUID group = groupOf(helper, c.leader());
            ClanDiscipline.promote(c.leader(), name(c.a()));
            helper.assertTrue(PackCommand.leave(c.a()) == null, "a Day Walker could not leave");
            helper.assertTrue(PackCommand.leave(c.b()) == null, "a Night Walker could not leave");
            helper.assertTrue(!GroupGate.exiled(server(helper), group, c.a().getUUID()), "leaving left a ban");
        } finally {
            clean(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 200)
    public static void aTypeSwitchKeepsRanksAndBans(GameTestHelper helper) {
        Clan c = clan(helper);
        TalkingDuelist stranger = human(helper, 4.5);
        online(c.leader(), c.a(), c.b(), stranger);
        try {
            UUID group = groupOf(helper, c.leader());
            ClanDiscipline.promote(c.leader(), name(c.a()));
            groups(helper).ban(group.toString(), stranger.getUUID(), PackIndex.now() + 7 * DAY);
            de.teamlapen.vampirism.api.VampirismAPI.factionPlayerHandler(c.leader()).leaveFaction(false);
            GroupSwitcher.refresh(c.leader());
            helper.assertTrue(Groups.typeOf(c.a()) != GroupType.CLAN, "the group did not stop being a clan");
            helper.assertTrue(groups(helper).rankOf(c.a().getUUID(), GroupType.COVEN) == GroupRank.NONE, "a dormant clan rank shows in the coven");
            de.teamlapen.vampirism.api.VampirismAPI.factionPlayerHandler(c.leader()).setFactionAndLevel(de.teamlapen.vampirism.api.VReference.VAMPIRE_FACTION, 1);
            GroupSwitcher.refresh(c.leader());
            helper.assertTrue(Groups.typeOf(c.a()) == GroupType.CLAN, "the group did not come back as a clan");
            helper.assertTrue(rank(helper, c.a()) == GroupRank.DAY_WALKER, "the Day Walker row did not come back");
            helper.assertTrue(GroupGate.exiled(server(helper), group, stranger.getUUID()), "the ban did not survive");
        } finally {
            clean(helper, c.leader(), c.a(), c.b(), stranger);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void theCommandsAreOnTheGroupTreeAndAliased(GameTestHelper helper) {
        CommandDispatcher<CommandSourceStack> d = new CommandDispatcher<>();
        GroupCommand.register(d);
        CommandSourceStack source = helper.getLevel().getServer().createCommandSourceStack();
        for (String line : new String[] {"group promote Bob", "clan promote Bob", "group demote Bob", "clan demote Bob", "group exile Bob", "coven exile Bob", "sb group exile Bob"}) {
            ParseResults<CommandSourceStack> parsed = d.parse(line, source);
            helper.assertTrue(!parsed.getReader().canRead() && parsed.getExceptions().isEmpty() && parsed.getContext().getLastChild().getCommand() != null, "'" + line + "' does not parse to a command");
        }
        helper.succeed();
    }
}
