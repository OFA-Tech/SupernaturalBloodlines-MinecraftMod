package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.clean;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.human;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.join;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.online;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.vampire;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.witch;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.group.GroupRank;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.core.group.clan.CharterState;
import net.ofatech.supernaturalbloodlines.core.group.clan.ClanView;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers.TalkingDuelist;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex.CharterRow;
import net.ofatech.supernaturalbloodlines.parts.species.group.Groups;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.group.clan.CharterScan;
import net.ofatech.supernaturalbloodlines.parts.species.group.clan.Charters;
import net.ofatech.supernaturalbloodlines.parts.species.group.clan.ClanViews;
import net.ofatech.supernaturalbloodlines.parts.species.group.clan.Contests;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubClan;

/** Plan 20c Task 10 (R281): the clan view of each viewer is their own clan only, and a name is a member's name or nothing. */
@PrefixGameTestTemplate(false)
public final class ClanViewGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final long DAY = 24L * 60 * 60 * 1000;
    private static final long[] CLOCK = {40_000_000_000L};

    private ClanViewGameTests() {
    }

    private static String name(UUID id) {
        return "N" + id.toString().substring(0, 6);
    }

    private static TalkingDuelist[] clan(GameTestHelper helper, double x) {
        CLOCK[0] = 40_000_000_000L;
        PackIndex.overrideClock(() -> CLOCK[0]);
        ClanViews.overrideNames(ClanViewGameTests::name);
        TalkingDuelist l = vampire(helper, x);
        TalkingDuelist a = human(helper, x + 1);
        TalkingDuelist b = human(helper, x + 2);
        online(l, a, b);
        helper.assertTrue(join(l, a) && join(l, b) && Groups.typeOf(a) == GroupType.CLAN, "no clan formed");
        return new TalkingDuelist[] {l, a, b};
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aNonClanViewerGetsNone(GameTestHelper helper) {
        TalkingDuelist p = human(helper, 1.5);
        try {
            helper.assertTrue(ClanViews.of(p).equals(ClanView.NONE) && HubClan.of(p) != null, "a viewer in no group got " + ClanViews.of(p));
        } finally {
            clean(helper, p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aPackOrACovenViewerGetsNone(GameTestHelper helper) {
        TalkingDuelist w = witch(helper, 1.5);
        TalkingDuelist a = human(helper, 2.5);
        online(w, a);
        try {
            helper.assertTrue(join(w, a), "setup");
            helper.assertTrue(ClanViews.of(w).equals(ClanView.NONE) && ClanViews.of(a).equals(ClanView.NONE), "a coven member got a clan view");
        } finally {
            clean(helper, w, a);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void eachMemberGetsTheirOwnMineAndMayChallenge(GameTestHelper helper) {
        TalkingDuelist[] c = clan(helper, 1.5);
        try {
            GroupsConfig.override("challenger_min_days", 0);
            GroupIndex.get(helper.getLevel()).setRank(c[1].getUUID(), GroupRank.DAY_WALKER);
            CharterScan.overridePlayers(() -> List.of(c));
            c[0].getInventory().add(new ItemStack(Items.WRITABLE_BOOK));
            c[0].getInventory().add(new ItemStack(Items.GOLD_BLOCK));
            helper.assertTrue(Charters.forge(c[0]) == null, "setup: forge");
            ClanView head = ClanViews.of(c[0]);
            ClanView day = ClanViews.of(c[1]);
            ClanView night = ClanViews.of(c[2]);
            helper.assertTrue(head.charter().mine() && !day.charter().mine() && !night.charter().mine(), "mine is wrong: " + head.charter().mine() + day.charter().mine() + night.charter().mine());
            helper.assertTrue(head.charter().state() == ClanView.CHARTER_HELD && head.charter().holderName().equals(name(c[0].getUUID())), "the head's own Charter state or name is wrong: " + head.charter());
            helper.assertTrue(day.charter().holderName().equals(name(c[0].getUUID())) && night.charter().holderName().equals(name(c[0].getUUID())), "a clanmate does not read the holder's name");
            helper.assertTrue(day.contest().iMayChallenge() && !night.contest().iMayChallenge() && !head.contest().iMayChallenge(), "iMayChallenge is wrong");
            helper.assertTrue(night.autoPromoteDaysLeft() == 14 && day.autoPromoteDaysLeft() == -1 && head.autoPromoteDaysLeft() == -1, "the promotion clock: " + night.autoPromoteDaysLeft());
            CLOCK[0] += 10 * DAY;
            helper.assertTrue(ClanViews.of(c[2]).autoPromoteDaysLeft() == 4 && ClanViews.of(c[2]).daysInClan() == 10, "ten days in: " + ClanViews.of(c[2]));
            helper.assertTrue(Contests.challenge(c[1]) == null, "setup: challenge");
            ClanView asked = ClanViews.of(c[0]);
            helper.assertTrue(asked.contest().phase() == ClanView.CONTEST_PENDING && asked.contest().iMustAccept() && asked.contest().secondsLeft() > 0, "the head's card: " + asked.contest());
            helper.assertTrue(!ClanViews.of(c[1]).contest().iMustAccept() && !ClanViews.of(c[2]).contest().iMustAccept(), "a member must accept");
        } finally {
            CharterScan.overridePlayers(null);
            clean(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aHiddenOriginalLeaderIsOnlyAName(GameTestHelper helper) {
        TalkingDuelist[] c = clan(helper, 1.5);
        try {
            net.ofatech.supernaturalbloodlines.core.BloodlineAccess.set(c[0], net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState.NONE.toBuilder().species(net.ofatech.supernaturalbloodlines.core.bloodline.Species.ORIGINAL).build());
            for (TalkingDuelist p : c) {
                ClanView v = ClanViews.of(p);
                String text = v.toString().toLowerCase();
                helper.assertTrue(!text.contains("original") && !text.contains("species") && !text.contains(c[0].getUUID().toString()), "the view of " + p.getUUID() + " leaks: " + text);
            }
        } finally {
            clean(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aHolderWhoIsNotAMemberNeverReachesTheView(GameTestHelper helper) {
        TalkingDuelist[] c = clan(helper, 1.5);
        TalkingDuelist stranger = human(helper, 6.5);
        try {
            online(c[0], c[1], c[2], stranger);
            c[0].getInventory().add(new ItemStack(Items.WRITABLE_BOOK));
            c[0].getInventory().add(new ItemStack(Items.GOLD_BLOCK));
            helper.assertTrue(Charters.forge(c[0]) == null, "setup: forge");
            UUID g = GroupIndex.get(helper.getLevel()).existingGroupOf(c[0].getUUID()).orElseThrow();
            CharterRow r = GroupIndex.get(helper.getLevel()).charterOf(g).orElseThrow();
            GroupIndex.get(helper.getLevel()).setCharter(g, new CharterRow(r.id(), Optional.of(stranger.getUUID()), CharterState.HELD, r.forgedAt(), r.seenAt(), Optional.empty(), 0));
            for (TalkingDuelist p : c) {
                helper.assertTrue(ClanViews.of(p).charter().holderName().isEmpty(), "a stranger's name reached " + p.getUUID());
            }
            GroupIndex.get(helper.getLevel()).setCharter(g, new CharterRow(r.id(), Optional.of(c[1].getUUID()), CharterState.HELD, r.forgedAt(), r.seenAt(), Optional.empty(), 0));
            helper.assertTrue(ClanViews.of(c[2]).charter().holderName().equals(name(c[1].getUUID())), "a member holder's name is not shown");
        } finally {
            clean(helper, c[0], c[1], c[2], stranger);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aMemberWhoseNameIsUnknownIsShownAsNothingNeverAsAnId(GameTestHelper helper) {
        TalkingDuelist[] c = clan(helper, 1.5);
        try {
            ClanViews.overrideNames(id -> null);
            c[0].getInventory().add(new ItemStack(Items.WRITABLE_BOOK));
            c[0].getInventory().add(new ItemStack(Items.GOLD_BLOCK));
            helper.assertTrue(Charters.forge(c[0]) == null, "setup: forge");
            for (TalkingDuelist p : c) {
                helper.assertTrue(ClanViews.of(p).charter().holderName().isEmpty(), "an unknown name was shown as " + ClanViews.of(p).charter().holderName());
            }
        } finally {
            clean(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aViewerOutsideEveryClanSeesNothingWhileAClanExists(GameTestHelper helper) {
        TalkingDuelist[] c = clan(helper, 1.5);
        TalkingDuelist outsider = human(helper, 6.5);
        try {
            online(c[0], c[1], c[2], outsider);
            c[0].getInventory().add(new ItemStack(Items.WRITABLE_BOOK));
            c[0].getInventory().add(new ItemStack(Items.GOLD_BLOCK));
            helper.assertTrue(Charters.forge(c[0]) == null, "setup: forge");
            helper.assertTrue(ClanViews.of(outsider).equals(ClanView.NONE), "a viewer in no clan saw " + ClanViews.of(outsider));
        } finally {
            clean(helper, c[0], c[1], c[2], outsider);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void twoClansSeeOnlyTheirOwn(GameTestHelper helper) {
        TalkingDuelist[] one = clan(helper, 1.5);
        TalkingDuelist l2 = vampire(helper, 6.5);
        TalkingDuelist a2 = human(helper, 7.5);
        TalkingDuelist b2 = human(helper, 8.5);
        try {
            online(one[0], one[1], one[2], l2, a2, b2);
            helper.assertTrue(join(l2, a2) && join(l2, b2), "setup: no second clan");
            one[0].getInventory().add(new ItemStack(Items.WRITABLE_BOOK));
            one[0].getInventory().add(new ItemStack(Items.GOLD_BLOCK));
            helper.assertTrue(Charters.forge(one[0]) == null, "setup: forge");
            helper.assertTrue(ClanViews.of(l2).charter().state() == ClanView.CHARTER_NONE && ClanViews.of(a2).charter().state() == ClanView.CHARTER_NONE, "the other clan sees the first clan's Charter");
            helper.assertTrue(ClanViews.of(one[1]).charter().state() == ClanView.CHARTER_HELD, "a member does not see their own clan's Charter");
        } finally {
            clean(helper, one[0], one[1], one[2], l2, a2, b2);
        }
        helper.succeed();
    }
}
