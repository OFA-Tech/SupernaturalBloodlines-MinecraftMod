package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.clean;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.human;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.join;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.online;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.group.GroupView;
import net.ofatech.supernaturalbloodlines.core.group.brotherhood.BrotherhoodView;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers.TalkingDuelist;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex.BreakerRow;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupViews;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.group.brotherhood.BrotherhoodArms;
import net.ofatech.supernaturalbloodlines.parts.species.group.brotherhood.BrotherhoodLead;
import net.ofatech.supernaturalbloodlines.parts.species.group.brotherhood.BrotherhoodViews;
import net.ofatech.supernaturalbloodlines.parts.species.group.brotherhood.Codes;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.BrotherhoodViewPayload;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.LadderNet;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests;

/** Plan 20d Task 11 (R297): what the hub is told about the viewer's own brotherhood, and when. Marks are set by hand, the clock is pinned. */
@PrefixGameTestTemplate(false)
public final class BrotherhoodViewGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final long T0 = 1_800_000_000_000L;
    private static final long[] CLOCK = {T0};
    private static final List<UUID> PUSHED = new ArrayList<>();
    private static boolean hooked;

    private BrotherhoodViewGameTests() {
    }

    private static void at(long millis) {
        CLOCK[0] = millis;
        PackIndex.overrideClock(() -> CLOCK[0]);
    }

    private static BloodlineState hof(int kills) {
        return BloodlineState.NONE.toBuilder().species(Species.HUNTER_OF_THE_FIVE).markKills(kills).build();
    }

    private record Bh(TalkingDuelist leader, TalkingDuelist a, TalkingDuelist b) {
        ServerPlayer[] all() {
            return new ServerPlayer[] {leader, a, b};
        }
    }

    private static Bh bh(GameTestHelper helper, int leaderKills) {
        at(T0);
        if (!hooked) {
            hooked = true;
            HubRequests.onChanged(p -> PUSHED.add(p.getUUID()));
        }
        TalkingDuelist leader = GroupTestKit.hunter(helper, 1.5);
        TalkingDuelist a = human(helper, 2.5);
        TalkingDuelist b = human(helper, 3.5);
        online(leader, a, b);
        if (!join(leader, a) || !join(leader, b)) {
            throw new AssertionError("no brotherhood");
        }
        BloodlineAccess.set(leader, hof(leaderKills));
        return new Bh(leader, a, b);
    }

    private static void finish(GameTestHelper helper, Bh g, ServerPlayer... extra) {
        GroupIndex groups = GroupIndex.get(helper.getLevel());
        List<ServerPlayer> all = new ArrayList<>(List.of(g.all()));
        all.addAll(List.of(extra));
        for (ServerPlayer p : all) {
            groups.clearBreaker(p.getUUID());
            groups.clearMark(p.getUUID());
        }
        BrotherhoodArms.overridePlayers(null);
        PUSHED.clear();
        clean(helper, all.toArray(new ServerPlayer[0]));
    }

    // ---- what the view says ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aNonBrotherViewerGetsNone(GameTestHelper helper) {
        Bh g = bh(helper, 5);
        TalkingDuelist stranger = human(helper, 4.5);
        try {
            helper.assertTrue(BrotherhoodViews.of(stranger).equals(BrotherhoodView.NONE), "somebody in no group got " + BrotherhoodViews.of(stranger));
        } finally {
            finish(helper, g, stranger);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aViewerWhoDoesNotFitGetsFitsFalseAndNoPerkNumbers(GameTestHelper helper) {
        Bh g = bh(helper, 5);
        TalkingDuelist vampire = GroupTestKit.vampire(helper, 4.5);
        online(g.leader(), g.a(), g.b(), vampire);
        try {
            helper.assertTrue(join(g.leader(), vampire), "a vampire could not join");
            BloodlineAccess.set(vampire, BloodlineState.NONE.toBuilder().markKills(9).build());
            BrotherhoodView v = BrotherhoodViews.of(vampire);
            helper.assertTrue(v.inBrotherhood() && !v.fits(), "in/fits: " + v);
            helper.assertTrue(v.ownMark() == 0 && v.leaderMark() == 0 && v.armsBrothers() == 0 && v.armsPercent() == 0 && v.strikes() == 0 && v.strikeLimit() == 0 && !v.sightOn() && !v.codeOn() && !v.leadByMark() && !v.anyHasMax(),
                    "a viewer who does not fit got perk numbers: " + v);
        } finally {
            finish(helper, g, vampire);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void onlyTheViewersOwnBrotherhoodIsCarried(GameTestHelper helper) {
        Bh g = bh(helper, 7);
        TalkingDuelist other = GroupTestKit.hunter(helper, 4.5);
        TalkingDuelist otherMember = human(helper, 5.5);
        online(g.leader(), g.a(), g.b(), other, otherMember);
        try {
            helper.assertTrue(join(other, otherMember), "no second brotherhood");
            BloodlineAccess.set(other, hof(20));
            BrotherhoodView v = BrotherhoodViews.of(g.a());
            helper.assertTrue(v.leaderMark() == 7 && !v.anyHasMax(), "the other brotherhood's marks leaked: " + v);
            BrotherhoodView w = BrotherhoodViews.of(otherMember);
            helper.assertTrue(w.leaderMark() == 20 && w.anyHasMax(), "the second brotherhood's own view: " + w);
        } finally {
            finish(helper, g, other, otherMember);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theLeadersMarkIsTheRecordedOne(GameTestHelper helper) {
        Bh g = bh(helper, 2);
        try {
            online(g.a(), g.b());
            GroupIndex.get(helper.getLevel()).setMark(g.leader().getUUID(), 12);
            BrotherhoodView v = BrotherhoodViews.of(g.a());
            helper.assertTrue(v.leaderMark() == 12, "the leader's mark is " + v.leaderMark() + ", not the recorded 12");
        } finally {
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anotherBrothersMarkIsNeverCarried(GameTestHelper helper) {
        Bh g = bh(helper, 12);
        try {
            BloodlineAccess.set(g.a(), hof(3));
            BloodlineAccess.set(g.b(), hof(9));
            BrotherhoodView a = BrotherhoodViews.of(g.a());
            BrotherhoodView b = BrotherhoodViews.of(g.b());
            helper.assertTrue(a.ownMark() == 3 && b.ownMark() == 9 && a.leaderMark() == 12 && b.leaderMark() == 12, "a " + a + " b " + b);
            helper.assertTrue(java.util.Arrays.stream(BrotherhoodView.class.getRecordComponents()).map(c -> c.getName()).filter(n -> n.toLowerCase(java.util.Locale.ROOT).contains("mark") || n.toLowerCase(java.util.Locale.ROOT).contains("kill")).sorted().toList().equals(List.of("leadByMark", "leaderMark", "ownMark")), "a field of another brother's kills");
            helper.assertTrue(!a.anyHasMax() && !b.anyHasMax(), "a brother's 9 made anyHasMax");
        } finally {
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anyHasMaxComesFromTheRecord(GameTestHelper helper) {
        Bh g = bh(helper, 5);
        try {
            online(g.leader(), g.a());
            GroupIndex.get(helper.getLevel()).setMark(g.b().getUUID(), 20);
            helper.assertTrue(BrotherhoodViews.of(g.a()).anyHasMax(), "a brother away with 20 recorded did not make the map whole");
            GroupIndex.get(helper.getLevel()).setMark(g.b().getUUID(), 19);
            helper.assertTrue(!BrotherhoodViews.of(g.a()).anyHasMax(), "19 made the map whole");
        } finally {
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theBreakerTimeComesFromTheClock(GameTestHelper helper) {
        Bh g = bh(helper, 5);
        try {
            GroupIndex.get(helper.getLevel()).setBreaker(g.a().getUUID(), new BreakerRow(T0 + 3_600_000L, List.of(T0 - 1_000L)));
            BrotherhoodView v = BrotherhoodViews.of(g.a());
            helper.assertTrue(v.breakerSecondsLeft() == 3600 && v.breaker() && v.strikes() == 1 && v.strikeLimit() == 3, "view " + v);
            at(T0 + 1_800_000L);
            helper.assertTrue(BrotherhoodViews.of(g.a()).breakerSecondsLeft() == 1800, "half an hour later: " + BrotherhoodViews.of(g.a()).breakerSecondsLeft());
            at(T0 + 3_600_000L);
            helper.assertTrue(BrotherhoodViews.of(g.a()).breakerSecondsLeft() == 0 && !Codes.isBreaker(helper.getLevel().getServer(), g.a().getUUID()), "the day did not end");
        } finally {
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void perksOffCarriesNoNumbers(GameTestHelper helper) {
        Bh g = bh(helper, 5);
        try {
            BloodlineAccess.set(g.a(), hof(4));
            GroupsConfig.override("brotherhood_enabled", false);
            BrotherhoodView v = BrotherhoodViews.of(g.a());
            helper.assertTrue(v.inBrotherhood() && !v.perksOn() && v.ownMark() == 0 && v.armsPercent() == 0 && v.armsBrothers() == 0 && !v.sightOn() && !v.codeOn(), "perks off still carry numbers: " + v);
        } finally {
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theViewMatchesTheLiveBonus(GameTestHelper helper) {
        Bh g = bh(helper, 5);
        try {
            List<ServerPlayer> list = List.of(g.leader(), g.a(), g.b());
            BrotherhoodArms.overridePlayers(() -> list);
            BrotherhoodArms.refresh(helper.getLevel().getServer());
            for (ServerPlayer p : list) {
                var m = p.getAttribute(Attributes.ATTACK_DAMAGE).getModifier(BrotherhoodArms.MODIFIER_ID);
                BrotherhoodView v = BrotherhoodViews.of(p);
                helper.assertTrue(m != null && v.armsPercent() == Math.round(m.amount() * 100) && v.armsBrothers() == 2, "view " + v + " modifier " + m);
            }
        } finally {
            finish(helper, g);
        }
        helper.succeed();
    }

    // ---- when it is pushed ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aMarkChangePushesTheViewToTheMember(GameTestHelper helper) {
        Bh g = bh(helper, 12);
        try {
            PUSHED.clear();
            BloodlineAccess.set(g.a(), hof(3));
            helper.assertTrue(PUSHED.contains(g.a().getUUID()) && PUSHED.contains(g.b().getUUID()), "the brothers' pages were not refreshed: " + PUSHED.size());
        } finally {
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aStrikeAndABreakerPushTheView(GameTestHelper helper) {
        Bh g = bh(helper, 12);
        Villager v = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(2, 1, 2));
        try {
            PUSHED.clear();
            HunterMarkGameTests.kill(g.a(), v);
            helper.assertTrue(PUSHED.contains(g.a().getUUID()) && PUSHED.contains(g.leader().getUUID()), "a break did not refresh the pages");
            helper.assertTrue(BrotherhoodViews.of(g.a()).strikes() == 1 && BrotherhoodViews.of(g.a()).breaker(), "the view does not show the break");
        } finally {
            v.discard();
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aLeadChangePushesTheViewToEveryMember(GameTestHelper helper) {
        Bh g = bh(helper, 2);
        try {
            BloodlineAccess.set(g.a(), hof(0));
            // the higher mark is recorded while lead by mark is off, so the mark's own push happens before the pages are watched
            GroupsConfig.override("lead_by_mark", false);
            BloodlineAccess.set(g.b(), hof(5));
            GroupsConfig.override("lead_by_mark", null);
            PUSHED.clear();
            BrotherhoodLead.check(helper.getLevel().getServer(), g.b().getUUID());
            helper.assertTrue(PackIndex.get(helper.getLevel()).isLeader(g.b().getUUID()), "setup: the lead did not move");
            long leaders = PUSHED.stream().filter(g.leader().getUUID()::equals).count();
            helper.assertTrue(PUSHED.contains(g.a().getUUID()) && PUSHED.contains(g.b().getUUID()) && leaders >= 1, "the lead change did not refresh every page");
        } finally {
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPotentialRowIsLabelledPotential(GameTestHelper helper) {
        Bh g = bh(helper, 12);
        try {
            BloodlineAccess.set(g.a(), BloodlineState.NONE.toBuilder().species(Species.POTENTIAL).build());
            BloodlineAccess.set(g.b(), hof(3));
            GroupView view = GroupViews.of(g.leader());
            helper.assertTrue(view.members().stream().anyMatch(r -> r.rank().equals("potential")), "no Potential row: " + view.members());
            helper.assertTrue(view.members().stream().filter(r -> r.rank().equals("member")).count() == 1, "a Hunter of the Five is not just a Brother");
            helper.assertTrue(view.members().stream().filter(r -> r.rank().equals("potential")).count() == 1, "more than one Potential row");
        } finally {
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aGroupOfAnotherTypeGetsNone(GameTestHelper helper) {
        TalkingDuelist witch = GroupTestKit.witch(helper, 1.5);
        TalkingDuelist member = human(helper, 2.5);
        online(witch, member);
        try {
            helper.assertTrue(join(witch, member), "no coven");
            helper.assertTrue(BrotherhoodViews.of(witch).equals(BrotherhoodView.NONE) && BrotherhoodViews.of(member).equals(BrotherhoodView.NONE), "a coven got a brotherhood view");
        } finally {
            PackIndex.overrideClock(null);
            clean(helper, witch, member);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theViewReachesAClientThroughLadderNet(GameTestHelper helper) {
        Bh g = bh(helper, 5);
        try {
            BrotherhoodView v = BrotherhoodViews.of(g.a());
            io.netty.buffer.ByteBuf buf = io.netty.buffer.Unpooled.buffer();
            BrotherhoodViewPayload.STREAM_CODEC.encode(buf, new BrotherhoodViewPayload(v));
            helper.assertTrue(BrotherhoodViewPayload.STREAM_CODEC.decode(buf).view().equals(v), "the live view does not survive the codec");
            LadderNet.sendRequests(g.a());
            helper.assertTrue(g.a().connection == null || !g.a().connection.hasChannel(BrotherhoodViewPayload.TYPE), "a fake player's connection has the optional channel");
        } finally {
            finish(helper, g);
        }
        helper.succeed();
    }
}
