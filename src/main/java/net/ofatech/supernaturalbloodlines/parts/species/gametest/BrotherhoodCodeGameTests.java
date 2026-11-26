package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.clean;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.human;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.join;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.key;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.online;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers.TalkingDuelist;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupGate;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupLead;
import net.ofatech.supernaturalbloodlines.parts.species.group.Groups;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.group.brotherhood.BrotherhoodArms;
import net.ofatech.supernaturalbloodlines.parts.species.group.brotherhood.BrotherhoodRegistry;
import net.ofatech.supernaturalbloodlines.parts.species.group.brotherhood.Codes;
import net.ofatech.supernaturalbloodlines.parts.species.hunter.MarkTracker;
import net.ofatech.supernaturalbloodlines.parts.species.pack.Innocence;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackAbsence;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackCommand;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.pack.SparkHandler;

/** Plan 20d Task 8 (R294): the Code. The clock is pinned, kills are posted by hand (never a tick loop). */
@PrefixGameTestTemplate(false)
public final class BrotherhoodCodeGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final long HOUR = 3_600_000L;
    private static final long DAY = 24 * HOUR;
    private static final long T0 = 1_800_000_000_000L;
    private static final String BH = "supernaturalbloodlines.brotherhood.";
    private static final long[] CLOCK = {T0};

    private BrotherhoodCodeGameTests() {
    }

    private record Bh(TalkingDuelist leader, TalkingDuelist a, TalkingDuelist b) {
        ServerPlayer[] all() {
            return new ServerPlayer[] {leader, a, b};
        }
    }

    private static BloodlineState hof(int kills) {
        return BloodlineState.NONE.toBuilder().species(Species.HUNTER_OF_THE_FIVE).markKills(kills).build();
    }

    private static void at(long millis) {
        CLOCK[0] = millis;
        PackIndex.overrideClock(() -> CLOCK[0]);
    }

    /** A hunter-led brotherhood of two plain humans; the leader has {@code leaderKills} (so nobody is passed by a smaller mark) and the clock is pinned at T0. */
    private static Bh bh(GameTestHelper helper, int leaderKills) {
        at(T0);
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
        for (ServerPlayer p : g.all()) {
            groups.clearBreaker(p.getUUID());
        }
        List<ServerPlayer> everyone = new ArrayList<>(List.of(g.all()));
        everyone.addAll(List.of(extra));
        for (ServerPlayer p : everyone) {
            groups.clearBreaker(p.getUUID());
        }
        HunterMarkGameTests.clearHooks();
        clean(helper, everyone.toArray(new ServerPlayer[0]));
    }

    private static Villager villager(GameTestHelper helper) {
        return helper.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(2, 1, 2));
    }

    private static void kill(ServerPlayer killer, LivingEntity victim) {
        HunterMarkGameTests.kill(killer, victim);
    }

    private static GroupIndex groups(GameTestHelper helper) {
        return GroupIndex.get(helper.getLevel());
    }

    private static boolean breaker(GameTestHelper helper, ServerPlayer p) {
        return Codes.isBreaker(helper.getLevel().getServer(), p.getUUID());
    }

    private static int strikes(GameTestHelper helper, ServerPlayer p) {
        return groups(helper).breakerOf(p.getUUID()).map(r -> r.strikes().size()).orElse(0);
    }

    private static int mark(ServerPlayer p) {
        return BloodlineAccess.get(p).markKills();
    }

    private static UUID group(GameTestHelper helper, ServerPlayer leader) {
        return groups(helper).existingGroupOf(leader.getUUID()).orElseThrow();
    }

    private static boolean in(GameTestHelper helper, ServerPlayer p) {
        return PackIndex.get(helper.getLevel()).alphaOf(p.getUUID()).isPresent();
    }

    /** Three breaks by {@code killer}, one a day apart, each on another villager. */
    private static void threeBreaks(GameTestHelper helper, ServerPlayer killer) {
        for (int i = 0; i < 3; i++) {
            at(T0 + i * DAY);
            Villager v = villager(helper);
            kill(killer, v);
            v.discard();
        }
    }

    // ---- what is a break ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aVillagerKillCostsTheMarkAndMakesABreaker(GameTestHelper helper) {
        Bh g = bh(helper, 20);
        Villager v = villager(helper);
        try {
            BloodlineAccess.set(g.a(), hof(12));
            kill(g.a(), v);
            helper.assertTrue(mark(g.a()) == 9, "the mark is " + mark(g.a()));
            helper.assertTrue(breaker(helper, g.a()) && strikes(helper, g.a()) == 1, "no breaker or strike");
            helper.assertTrue(groups(helper).breakerOf(g.a().getUUID()).orElseThrow().until() == T0 + DAY, "the day is not 24 hours");
            helper.assertTrue(BloodlineAccess.get(g.a()).species() == Species.HUNTER_OF_THE_FIVE, "the species changed");
        } finally {
            v.discard();
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aKillOfAPlayerWhoDidNotFightIsABreak(GameTestHelper helper) {
        Bh g = bh(helper, 20);
        ServerPlayer victim = HunterMarkGameTests.human(helper);
        try {
            kill(g.a(), victim);
            helper.assertTrue(breaker(helper, g.a()), "a player who did not fight is not a break");
        } finally {
            HunterMarkGameTests.reset(victim);
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aKillOfAWitchWhoDidNotFightIsABreak(GameTestHelper helper) {
        Bh g = bh(helper, 20);
        ServerPlayer witch = HunterMarkGameTests.human(helper);
        try {
            BloodlineAccess.set(witch, BloodlineState.NONE.toBuilder().species(Species.WITCH).build());
            kill(g.a(), witch);
            helper.assertTrue(breaker(helper, g.a()), "a witch who did not fight is not a break");
        } finally {
            HunterMarkGameTests.reset(witch);
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aKillOfAPlayerWhoFoughtIsNot(GameTestHelper helper) {
        Bh g = bh(helper, 20);
        ServerPlayer recent = HunterMarkGameTests.human(helper);
        ServerPlayer old = HunterMarkGameTests.human(helper);
        try {
            recent.setLastHurtMob(g.a());
            recent.tickCount += 100;
            kill(g.a(), recent);
            helper.assertTrue(!breaker(helper, g.a()), "a player who hit the killer 100 ticks ago was judged innocent");
            old.setLastHurtMob(g.a());
            old.tickCount += 300;
            kill(g.a(), old);
            helper.assertTrue(breaker(helper, g.a()), "a player who hit the killer 300 ticks ago was not innocent");
        } finally {
            HunterMarkGameTests.reset(recent, old);
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPreyKillIsNeverABreak(GameTestHelper helper) {
        Bh g = bh(helper, 20);
        ServerPlayer prey = HunterMarkGameTests.vampire(helper);
        try {
            BloodlineAccess.set(g.a(), hof(12));
            kill(g.a(), prey);
            helper.assertTrue(!breaker(helper, g.a()) && mark(g.a()) == 13, "a prey kill: breaker=" + breaker(helper, g.a()) + " mark=" + mark(g.a()));
        } finally {
            HunterMarkGameTests.reset(prey);
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aHostileMobAndAnAnimalAreNeverABreak(GameTestHelper helper) {
        Bh g = bh(helper, 20);
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 1, 3));
        LivingEntity cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(3, 1, 2));
        try {
            kill(g.a(), zombie);
            kill(g.a(), cow);
            helper.assertTrue(!breaker(helper, g.a()) && strikes(helper, g.a()) == 0, "a mob or an animal broke the Code");
        } finally {
            zombie.discard();
            cow.discard();
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBrotherKillingABrotherWhoDidNotFightIsABreak(GameTestHelper helper) {
        Bh g = bh(helper, 20);
        try {
            kill(g.a(), g.b());
            helper.assertTrue(breaker(helper, g.a()), "a brother killed a brother and it was no break");
        } finally {
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void penaltyIsAQuarterWithAMinimumOfOne(GameTestHelper helper) {
        Bh g = bh(helper, 20);
        try {
            int[][] rows = {{20, 15}, {4, 3}, {3, 2}, {1, 0}};
            for (int[] r : rows) {
                BloodlineAccess.set(g.a(), hof(r[0]));
                groups(helper).clearBreaker(g.a().getUUID());
                Codes.resetLedger();
                Villager v = villager(helper);
                kill(g.a(), v);
                v.discard();
                helper.assertTrue(mark(g.a()) == r[1], r[0] + " kills left " + mark(g.a()) + ", expected " + r[1]);
            }
        } finally {
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPlainHumanLosesNoMarkButIsStillABreaker(GameTestHelper helper) {
        Bh g = bh(helper, 20);
        Villager v = villager(helper);
        try {
            kill(g.a(), v);
            helper.assertTrue(mark(g.a()) == 0 && BloodlineAccess.get(g.a()).species() == Species.NONE, "a plain human's state changed");
            helper.assertTrue(breaker(helper, g.a()), "a plain human is not a Code-breaker");
        } finally {
            v.discard();
            finish(helper, g);
        }
        helper.succeed();
    }

    // ---- the day, the strikes, the expulsion ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aRepeatBreakRestartsTheDay(GameTestHelper helper) {
        Bh g = bh(helper, 20);
        Villager one = villager(helper);
        Villager two = villager(helper);
        try {
            kill(g.a(), one);
            at(T0 + 10 * HOUR);
            kill(g.a(), two);
            helper.assertTrue(groups(helper).breakerOf(g.a().getUUID()).orElseThrow().until() == T0 + 10 * HOUR + DAY, "the day did not restart");
            helper.assertTrue(strikes(helper, g.a()) == 2, "strikes " + strikes(helper, g.a()));
        } finally {
            one.discard();
            two.discard();
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void twoBreaksOnOneVictimWithinAMinuteCountOnce(GameTestHelper helper) {
        Bh g = bh(helper, 20);
        Villager v = villager(helper);
        try {
            BloodlineAccess.set(g.a(), hof(12));
            kill(g.a(), v);
            at(T0 + 30_000);
            kill(g.a(), v);
            helper.assertTrue(strikes(helper, g.a()) == 1 && mark(g.a()) == 9, "the second kill counted: strikes " + strikes(helper, g.a()) + " mark " + mark(g.a()));
        } finally {
            v.discard();
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theThirdBreakWithinSevenDaysExpels(GameTestHelper helper) {
        Bh g = bh(helper, 20);
        try {
            UUID group = group(helper, g.leader());
            threeBreaks(helper, g.a());
            helper.assertTrue(!in(helper, g.a()), "the third break did not expel");
            helper.assertTrue(g.a().received(BH + "broke_code"), "the member was not told why");
            helper.assertTrue(groups(helper).banUntil(group.toString(), g.a().getUUID()) == T0 + 2 * DAY + 7 * DAY, "the bar is " + groups(helper).banUntil(group.toString(), g.a().getUUID()));
            helper.assertTrue(strikes(helper, g.a()) == 0, "the strikes were kept: " + strikes(helper, g.a()));
            helper.assertTrue(g.b().received(BH + "code_expelled") && g.leader().received(BH + "code_expelled"), "the brotherhood was not told");
            helper.assertTrue(groups(helper).rankOf(g.a().getUUID(), GroupType.BROTHERHOOD) == net.ofatech.supernaturalbloodlines.core.group.GroupRank.NONE, "a rank row stayed");
        } finally {
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void twoBreaksDoNotExpel(GameTestHelper helper) {
        Bh g = bh(helper, 20);
        Villager one = villager(helper);
        Villager two = villager(helper);
        try {
            kill(g.a(), one);
            at(T0 + DAY);
            kill(g.a(), two);
            helper.assertTrue(in(helper, g.a()) && strikes(helper, g.a()) == 2, "two breaks expelled or lost a strike");
        } finally {
            one.discard();
            two.discard();
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theOldestBreakLeavesTheWindow(GameTestHelper helper) {
        Bh g = bh(helper, 20);
        Villager one = villager(helper);
        Villager two = villager(helper);
        Villager three = villager(helper);
        try {
            kill(g.a(), one);
            at(T0 + 3 * DAY);
            kill(g.a(), two);
            at(T0 + 7 * DAY + 1);
            kill(g.a(), three);
            helper.assertTrue(in(helper, g.a()) && strikes(helper, g.a()) == 2, "a break from seven days and a tick ago still counted: " + strikes(helper, g.a()));
        } finally {
            one.discard();
            two.discard();
            three.discard();
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theThirdBreakJustInsideTheWindowExpels(GameTestHelper helper) {
        Bh g = bh(helper, 20);
        Villager one = villager(helper);
        Villager two = villager(helper);
        Villager three = villager(helper);
        try {
            kill(g.a(), one);
            at(T0 + 3 * DAY);
            kill(g.a(), two);
            at(T0 + 7 * DAY - 1);
            kill(g.a(), three);
            helper.assertTrue(!in(helper, g.a()), "a break just inside the window did not count");
        } finally {
            one.discard();
            two.discard();
            three.discard();
            finish(helper, g);
        }
        helper.succeed();
    }

    // ---- the bar ----

    private static void expel(GameTestHelper helper, Bh g) {
        threeBreaks(helper, g.a());
        if (in(helper, g.a())) {
            throw new AssertionError("setup: not expelled");
        }
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anExpelledPlayerCannotRejoinThatBrotherhood(GameTestHelper helper) {
        Bh g = bh(helper, 20);
        TalkingDuelist outsider = human(helper, 4.5);
        online(g.leader(), g.a(), g.b(), outsider);
        try {
            helper.assertTrue(PackCommand.invite(g.leader(), outsider) == null, "setup: the invite was refused");
            String token = PackCommand.requests().tokenOf(g.leader().getUUID(), outsider.getUUID()).orElseThrow();
            groups(helper).ban(group(helper, g.leader()).toString(), outsider.getUUID(), PackIndex.now() + 7 * DAY);
            Component refusal = PackCommand.accept(outsider, token);
            helper.assertTrue(key(refusal).equals("supernaturalbloodlines.group.refused.brotherhood"), "the accept was " + key(refusal));
            helper.assertTrue(!key(refusal).contains("exile") && !key(refusal).contains("code") && !PackIndex.get(helper.getLevel()).inPack(outsider.getUUID()), "the refusal names the bar or the player joined");
        } finally {
            finish(helper, g, outsider);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theLeaderInvitingABarredPlayerHearsABrotherhoodLine(GameTestHelper helper) {
        Bh g = bh(helper, 20);
        try {
            expel(helper, g);
            Component line = PackCommand.invite(g.leader(), g.a());
            helper.assertTrue(key(line).equals(BH + "err.exiled_still"), "the line is " + key(line));
            helper.assertTrue(!key(line).contains("clan"), "the line says clan");
            helper.assertTrue(PackCommand.requests().latestFor(g.a().getUUID(), helper.getLevel().getGameTime()).isEmpty(), "an invitation was sent anyway");
        } finally {
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anExpelledPlayerMayJoinAnotherBrotherhoodOrAnyOtherGroup(GameTestHelper helper) {
        Bh g = bh(helper, 20);
        TalkingDuelist other = GroupTestKit.hunter(helper, 4.5);
        TalkingDuelist wicca = GroupTestKit.witch(helper, 5.5);
        TalkingDuelist otherMate = GroupTestKit.human(helper, 6.5);
        TalkingDuelist wiccaMate = GroupTestKit.human(helper, 7.5);
        online(g.leader(), g.a(), g.b(), other, wicca, otherMate, wiccaMate);
        try {
            // both groups exist before the expulsion, so a bar written to every group would reach them
            helper.assertTrue(join(other, otherMate) && join(wicca, wiccaMate), "no other groups");
            expel(helper, g);
            online(g.leader(), g.a(), g.b(), other, wicca, otherMate, wiccaMate);
            helper.assertTrue(join(other, g.a()) && Groups.typeOf(g.a()) == GroupType.BROTHERHOOD, "could not join another brotherhood");
            helper.assertTrue(PackCommand.leave(g.a()) == null && join(wicca, g.a()) && Groups.typeOf(g.a()) == GroupType.COVEN, "could not join a coven");
        } finally {
            finish(helper, g, other, wicca, otherMate, wiccaMate);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theBarEndsAfterTheDays(GameTestHelper helper) {
        Bh g = bh(helper, 20);
        try {
            expel(helper, g);
            UUID group = group(helper, g.leader());
            long until = groups(helper).banUntil(group.toString(), g.a().getUUID());
            at(until - 1);
            helper.assertTrue(GroupGate.exiled(helper.getLevel().getServer(), group, g.a().getUUID()), "the bar ended early");
            at(until);
            helper.assertTrue(!GroupGate.exiled(helper.getLevel().getServer(), group, g.a().getUUID()), "the bar did not end");
            helper.assertTrue(PackCommand.invite(g.leader(), g.a()) == null, "the invite is still refused after the days");
        } finally {
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theBarFollowsTheLeadMoving(GameTestHelper helper) {
        Bh g = bh(helper, 20);
        try {
            expel(helper, g);
            UUID group = group(helper, g.leader());
            GroupLead.handOver(helper.getLevel().getServer(), PackIndex.get(helper.getLevel()), g.leader().getUUID(), g.b().getUUID(), GroupType.BROTHERHOOD);
            helper.assertTrue(group(helper, g.b()).equals(group), "the group id did not follow the lead");
            helper.assertTrue(GroupGate.exiled(helper.getLevel().getServer(), group, g.a().getUUID()), "the bar was lost when the lead moved");
            helper.assertTrue(key(PackCommand.invite(g.b(), g.a())).equals(BH + "err.exiled_still"), "the new leader could invite the barred player");
        } finally {
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theLeaderIsNeverExpelled(GameTestHelper helper) {
        Bh g = bh(helper, 20);
        try {
            threeBreaks(helper, g.leader());
            helper.assertTrue(PackIndex.get(helper.getLevel()).isLeader(g.leader().getUUID()) && in(helper, g.a()), "the leader was put out or lost the lead");
            helper.assertTrue(strikes(helper, g.leader()) == 0, "the strikes were kept: " + strikes(helper, g.leader()));
            helper.assertTrue(groups(helper).breakerOf(g.leader().getUUID()).orElseThrow().until() == T0 + 2 * DAY + DAY, "no fresh breaker day");
            helper.assertTrue(g.a().received(BH + "code_leader"), "the brotherhood was not told");
            helper.assertTrue(groups(helper).banUntil(group(helper, g.leader()).toString(), g.leader().getUUID()) == 0, "the leader was barred");
        } finally {
            finish(helper, g);
        }
        helper.succeed();
    }

    // ---- what a breaker loses ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBreakerCannotTakeTheLeadOrInherit(GameTestHelper helper) {
        Bh g = bh(helper, 5);
        Villager v = villager(helper);
        try {
            BloodlineAccess.set(g.a(), hof(4));
            kill(g.a(), v);
            helper.assertTrue(breaker(helper, g.a()), "setup: not a breaker");
            BloodlineAccess.set(g.a(), hof(9));
            BrotherhoodRegistry.sweep(helper.getLevel().getServer());
            helper.assertTrue(PackIndex.get(helper.getLevel()).isLeader(g.leader().getUUID()), "a Code-breaker took the lead");
            online(g.a(), g.b());
            PackIndex.get(helper.getLevel()).setLastSeen(g.leader().getUUID(), T0 - 8 * DAY);
            helper.assertTrue(PackAbsence.check(helper.getLevel().getServer(), T0) == 1, "the absent leader was not deposed");
            helper.assertTrue(PackIndex.get(helper.getLevel()).isLeader(g.b().getUUID()), "a Code-breaker inherited: " + PackIndex.get(helper.getLevel()).leaderOf(g.a().getUUID()));
        } finally {
            v.discard();
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBreakerGetsNoArmsAndNoSight(GameTestHelper helper) {
        Bh g = bh(helper, 20);
        Villager v = villager(helper);
        try {
            List<ServerPlayer> list = List.of(g.leader(), g.a(), g.b());
            BrotherhoodArms.overridePlayers(() -> list);
            BrotherhoodArms.refresh(helper.getLevel().getServer());
            helper.assertTrue(g.a().getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getModifier(BrotherhoodArms.MODIFIER_ID) != null, "setup: no bonus before the break");
            kill(g.a(), v);
            BrotherhoodArms.refresh(helper.getLevel().getServer());
            helper.assertTrue(g.a().getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getModifier(BrotherhoodArms.MODIFIER_ID) == null, "a breaker kept the bonus");
            helper.assertTrue(BrotherhoodArms.brothersNear(g.leader()) == 1, "a breaker still gives it: " + BrotherhoodArms.brothersNear(g.leader()));
        } finally {
            v.discard();
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theBreakerStatusEndsAfterTheHours(GameTestHelper helper) {
        Bh g = bh(helper, 20);
        Villager v = villager(helper);
        try {
            kill(g.a(), v);
            at(T0 + DAY - 1);
            helper.assertTrue(breaker(helper, g.a()), "the day ended early");
            at(T0 + DAY);
            helper.assertTrue(!breaker(helper, g.a()), "the day did not end exactly at its end");
        } finally {
            v.discard();
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aLogoutKeepsTheTimer(GameTestHelper helper) {
        Bh g = bh(helper, 20);
        Villager v = villager(helper);
        try {
            kill(g.a(), v);
            long until = groups(helper).breakerOf(g.a().getUUID()).orElseThrow().until();
            online(g.leader(), g.b());
            at(T0 + 2 * HOUR);
            helper.assertTrue(breaker(helper, g.a()) && groups(helper).breakerOf(g.a().getUUID()).orElseThrow().until() == until, "an offline breaker lost the timer");
        } finally {
            v.discard();
            finish(helper, g);
        }
        helper.succeed();
    }

    // ---- who is judged ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aMemberWhoDoesNotFitIsNeverJudged(GameTestHelper helper) {
        Bh g = bh(helper, 20);
        TalkingDuelist witch = GroupTestKit.witch(helper, 4.5);
        online(g.leader(), g.a(), g.b(), witch);
        Villager v = villager(helper);
        try {
            helper.assertTrue(join(g.leader(), witch), "a witch could not join");
            kill(witch, v);
            helper.assertTrue(!breaker(helper, witch) && strikes(helper, witch) == 0, "a witch member was judged");
        } finally {
            v.discard();
            finish(helper, g, witch);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aVampireMemberIsNeverJudged(GameTestHelper helper) {
        Bh g = bh(helper, 20);
        TalkingDuelist vampire = GroupTestKit.vampire(helper, 4.5);
        online(g.leader(), g.a(), g.b(), vampire);
        Villager v = villager(helper);
        try {
            helper.assertTrue(join(g.leader(), vampire), "a vampire could not join");
            kill(vampire, v);
            helper.assertTrue(!breaker(helper, vampire), "a vampire member was judged");
        } finally {
            v.discard();
            finish(helper, g, vampire);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aNonMemberIsNeverJudged(GameTestHelper helper) {
        Bh g = bh(helper, 20);
        TalkingDuelist stranger = human(helper, 4.5);
        online(g.leader(), g.a(), g.b(), stranger);
        Villager v = villager(helper);
        try {
            kill(stranger, v);
            helper.assertTrue(!breaker(helper, stranger) && strikes(helper, stranger) == 0, "somebody in no group was judged");
        } finally {
            v.discard();
            finish(helper, g, stranger);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPackMemberIsNeverJudged(GameTestHelper helper) {
        Bh g = bh(helper, 20);
        TalkingDuelist hybrid = human(helper, 4.5);
        TalkingDuelist mate = human(helper, 5.5);
        online(g.leader(), g.a(), g.b(), hybrid, mate);
        GroupsConfig.override("open_founding", true);
        Villager v = villager(helper);
        try {
            BloodlineAccess.set(hybrid, BloodlineState.NONE.toBuilder().species(Species.HYBRID).build());
            helper.assertTrue(join(hybrid, mate) && Groups.typeOf(mate) == GroupType.PACK, "no pack");
            kill(mate, v);
            helper.assertTrue(!breaker(helper, mate), "a pack member was judged");
        } finally {
            v.discard();
            finish(helper, g, hybrid, mate);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void codeOffJudgesNobody(GameTestHelper helper) {
        Bh g = bh(helper, 20);
        Villager v = villager(helper);
        try {
            GroupsConfig.override("code_enabled", false);
            BloodlineAccess.set(g.a(), hof(12));
            kill(g.a(), v);
            helper.assertTrue(!breaker(helper, g.a()) && mark(g.a()) == 12, "the Code judged with code_enabled off");
        } finally {
            v.discard();
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void brotherhoodOffJudgesNobody(GameTestHelper helper) {
        Bh g = bh(helper, 20);
        Villager v = villager(helper);
        try {
            GroupsConfig.override("brotherhood_enabled", false);
            kill(g.a(), v);
            helper.assertTrue(!breaker(helper, g.a()), "the Code judged with brotherhoods off");
        } finally {
            v.discard();
            finish(helper, g);
        }
        helper.succeed();
    }

    // ---- attribution ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aKillCreditWithNoSourceIsNotJudged(GameTestHelper helper) {
        Bh g = bh(helper, 20);
        Villager v = villager(helper);
        try {
            v.setLastHurtByPlayer(g.a());
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new LivingDeathEvent(v, v.damageSources().genericKill()));
            helper.assertTrue(!breaker(helper, g.a()), "kill credit with no killing blow was judged (round 2: only the killing blow counts)");
        } finally {
            v.discard();
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCancelledDeathIsNotJudged(GameTestHelper helper) {
        Bh g = bh(helper, 20);
        Villager v = villager(helper);
        Consumer<LivingDeathEvent> cancel = e -> e.setCanceled(true);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, LivingDeathEvent.class, cancel);
        try {
            NeoForge.EVENT_BUS.post(new LivingDeathEvent(v, v.damageSources().source(DamageTypes.GENERIC_KILL, g.a())));
            helper.assertTrue(!breaker(helper, g.a()), "a cancelled death was judged");
        } finally {
            NeoForge.EVENT_BUS.unregister(cancel);
            v.discard();
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aLeaderWhoLosesAQuarterCanBePassed(GameTestHelper helper) {
        Bh g = bh(helper, 8);
        Villager v = villager(helper);
        try {
            BloodlineAccess.set(g.a(), hof(7));
            kill(g.leader(), v);
            helper.assertTrue(mark(g.leader()) == 6, "the leader's mark is " + mark(g.leader()));
            helper.assertTrue(PackIndex.get(helper.getLevel()).isLeader(g.a().getUUID()), "the lead did not move to the brother with 7 against 6");
        } finally {
            v.discard();
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theLineToTheBrotherhoodNamesTheBreakerAndNoNumber(GameTestHelper helper) {
        Bh g = bh(helper, 20);
        Villager v = villager(helper);
        try {
            online(g.a());
            BloodlineAccess.set(g.a(), hof(12));
            kill(g.a(), v);
            List<String[]> waiting = PackIndex.get(helper.getLevel()).takeNotices(g.leader().getUUID());
            String[] line = waiting.stream().filter(n -> n[0].equals(BH + "code_broken")).findFirst().orElseThrow();
            helper.assertTrue(line[1].equals(g.a().getGameProfile().getName()), "the line names " + line[1]);
            helper.assertTrue(line[1].chars().noneMatch(Character::isDigit) || g.a().getGameProfile().getName().chars().anyMatch(Character::isDigit), "the line carries a number: " + line[1]);
            String text = Component.translatable(line[0], line[1]).getString();
            helper.assertTrue(!text.matches(".*\\d.*") || g.a().getGameProfile().getName().chars().anyMatch(Character::isDigit), "the words carry a number: " + text);
        } finally {
            v.discard();
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theWerewolfInnocentKillStillWorks(GameTestHelper helper) {
        ServerPlayer killer = HunterMarkGameTests.human(helper);
        Villager v = villager(helper);
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 1, 3));
        try {
            for (LivingEntity victim : List.of(v, zombie, killer)) {
                helper.assertTrue(SparkHandler.isInnocent(killer, victim) == Innocence.of(killer, victim), "SparkHandler and Innocence disagree");
            }
            helper.assertTrue(Innocence.of(killer, v) && !Innocence.of(killer, zombie), "the table moved");
        } finally {
            v.discard();
            zombie.discard();
            HunterMarkGameTests.reset(killer);
        }
        helper.succeed();
    }

    private static final List<String> FEED = new ArrayList<>();
    private static boolean fed;

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theMarkFeedReachesEveryListener(GameTestHelper helper) {
        if (!fed) {
            fed = true;
            MarkTracker.onDeathResolved(r -> FEED.add("first"));
            MarkTracker.onDeathResolved(r -> {
                FEED.add("second");
                if (FEED.contains("throw")) {
                    throw new IllegalStateException("a hook that fails");
                }
            });
            MarkTracker.onDeathResolved(r -> FEED.add("third"));
        }
        Bh g = bh(helper, 20);
        Villager v = villager(helper);
        try {
            FEED.clear();
            FEED.add("throw");
            kill(g.a(), v);
            helper.assertTrue(FEED.equals(List.of("throw", "first", "second", "third")), "the hooks saw " + FEED);
            helper.assertTrue(breaker(helper, g.a()), "a hook that threw stopped the Code");
        } finally {
            FEED.clear();
            v.discard();
            finish(helper, g);
        }
        helper.succeed();
    }

    // ---- round 2: who was fighting, and who did the killing ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aVictimWhoHitTheKillerThenAChickenWasStillFighting(GameTestHelper helper) {
        Bh g = bh(helper, 20);
        ServerPlayer victim = HunterMarkGameTests.human(helper);
        LivingEntity chicken = helper.spawnWithNoFreeWill(EntityType.CHICKEN, new BlockPos(3, 1, 3));
        try {
            g.a().hurt(victim.damageSources().playerAttack(victim), 1f);
            helper.assertTrue(g.a().getLastHurtByMob() == victim, "setup: the hit did not land");
            victim.setLastHurtMob(chicken);
            kill(g.a(), victim);
            helper.assertTrue(!breaker(helper, g.a()), "a victim who hit the killer and then a chicken counted as innocent");
        } finally {
            chicken.discard();
            HunterMarkGameTests.reset(victim);
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aVictimWhoHurtTheKillerWithAPotionWasFighting(GameTestHelper helper) {
        Bh g = bh(helper, 20);
        ServerPlayer victim = HunterMarkGameTests.human(helper);
        try {
            g.a().hurt(victim.damageSources().indirectMagic(victim, victim), 1f);
            helper.assertTrue(g.a().getLastHurtByMob() == victim, "setup: the potion did not land");
            kill(g.a(), victim);
            helper.assertTrue(!breaker(helper, g.a()), "a victim who hurt the killer with a potion counted as innocent");
        } finally {
            HunterMarkGameTests.reset(victim);
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aKillerWhoIsOnlyCreditedIsNotJudged(GameTestHelper helper) {
        Bh g = bh(helper, 20);
        ServerPlayer victim = HunterMarkGameTests.human(helper);
        try {
            victim.setLastHurtByPlayer(g.a());
            NeoForge.EVENT_BUS.post(new LivingDeathEvent(victim, victim.damageSources().lava()));
            helper.assertTrue(!breaker(helper, g.a()), "a brother credited with a death in lava broke the Code");
        } finally {
            HunterMarkGameTests.reset(victim);
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aLeaderPassedByTheMarkLossOfTheThirdBreakIsNotExpelled(GameTestHelper helper) {
        Bh g = bh(helper, 20);
        try {
            for (int i = 0; i < 2; i++) {
                at(T0 + i * DAY);
                Villager v = villager(helper);
                kill(g.leader(), v);
                v.discard();
            }
            int afterTwo = mark(g.leader());
            BloodlineAccess.set(g.a(), hof(afterTwo - 1));
            helper.assertTrue(PackIndex.get(helper.getLevel()).isLeader(g.leader().getUUID()), "setup: the lead moved too early");
            at(T0 + 2 * DAY);
            Villager v = villager(helper);
            kill(g.leader(), v);
            v.discard();
            helper.assertTrue(mark(g.leader()) < afterTwo - 1, "setup: the third break did not drop the leader below the brother: " + mark(g.leader()));
            helper.assertTrue(in(helper, g.leader()), "the leader was expelled after being passed by the mark loss of the same break");
            helper.assertTrue(!GroupGate.exiled(helper.getLevel().getServer(), group(helper, g.a()), g.leader().getUUID()), "the old leader is barred");
            helper.assertTrue(strikes(helper, g.leader()) == 0 && breaker(helper, g.leader()), "the leader's third break is not the leader's rule");
        } finally {
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theBreakerHearsTheMarkTheDayAndTheStrikes(GameTestHelper helper) {
        Bh g = bh(helper, 20);
        Villager v = villager(helper);
        Villager w = villager(helper);
        try {
            BloodlineAccess.set(g.a(), hof(12));
            kill(g.a(), v);
            Component marked = g.a().message(BH + "code_you");
            helper.assertTrue(marked != null && args(marked).equals(List.of(9, 24, 1, 3)), "the marked breaker's line: " + marked);
            kill(g.b(), w);
            Component plain = g.b().message(BH + "code_you.unmarked");
            helper.assertTrue(plain != null && args(plain).equals(List.of(24, 1, 3)), "the plain breaker's line: " + plain);
        } finally {
            v.discard();
            w.discard();
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theExpelledHearTheDays(GameTestHelper helper) {
        Bh g = bh(helper, 20);
        try {
            expel(helper, g);
            Component days = g.a().message(BH + "code_out_days");
            helper.assertTrue(days != null && args(days).equals(List.of(7)), "no days in the expulsion: " + days);
        } finally {
            finish(helper, g);
        }
        helper.succeed();
    }

    private static List<Object> args(Component c) {
        return List.of(((net.minecraft.network.chat.contents.TranslatableContents) c.getContents()).getArgs());
    }
}