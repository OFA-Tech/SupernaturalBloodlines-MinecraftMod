package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.clean;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.human;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.join;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.online;

import java.util.List;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.network.HallucinationPayload;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers.TalkingDuelist;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.group.brotherhood.BrotherhoodRegistry;
import net.ofatech.supernaturalbloodlines.parts.species.hunter.FiveRoster;
import net.ofatech.supernaturalbloodlines.parts.species.hunter.HunterCurse;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;

/** Plan 20d Task 9 (R295): the Hunter's Curse reaches every brother and ends after 48 hours. Deaths are posted by hand, the clock is pinned. */
@PrefixGameTestTemplate(false)
public final class BrotherCurseGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final long HOUR = 3_600_000L;
    private static final long T0 = 1_800_000_000_000L;
    private static final long[] CLOCK = {T0};

    private BrotherCurseGameTests() {
    }

    private static void at(long millis) {
        CLOCK[0] = millis;
        PackIndex.overrideClock(() -> CLOCK[0]);
    }

    /** leader (a plain hunter), a plain hunter brother, a plain human brother; the killer is prey outside the group. */
    private record Bh(TalkingDuelist leader, TalkingDuelist hunter, TalkingDuelist human, ServerPlayer vampire) {
        ServerPlayer[] all() {
            return new ServerPlayer[] {leader, hunter, human, vampire};
        }
    }

    private static Bh bh(GameTestHelper helper) {
        at(T0);
        TalkingDuelist leader = GroupTestKit.hunter(helper, 1.5);
        TalkingDuelist hunter = GroupTestKit.hunter(helper, 2.5);
        TalkingDuelist human = human(helper, 3.5);
        ServerPlayer vampire = HunterMarkGameTests.vampire(helper);
        online(leader, hunter, human, vampire);
        HunterMarkGameTests.online(leader, hunter, human, vampire);
        if (!join(leader, hunter) || !join(leader, human)) {
            throw new AssertionError("no brotherhood");
        }
        return new Bh(leader, hunter, human, vampire);
    }

    private static void finish(GameTestHelper helper, Bh g, ServerPlayer... extra) {
        HunterMarkGameTests.clearHooks();
        HallucinationPayload.overrideSpy(null);
        FiveRoster r = FiveRoster.get(helper.getLevel());
        GroupIndex groups = GroupIndex.get(helper.getLevel());
        List<ServerPlayer> everyone = new java.util.ArrayList<>(List.of(g.all()));
        everyone.addAll(List.of(extra));
        for (ServerPlayer p : everyone) {
            HunterCurse.onLogout(p);
            r.setCursed(p.getUUID(), false);
            r.setLiftedCurse(p.getUUID(), false);
            groups.clearCurse(p.getUUID());
        }
        clean(helper, everyone.toArray(new ServerPlayer[0]));
        // leaving the hunters as a Potential owes another one: settled only after the clean-up, so nothing is left for the next test
        for (ServerPlayer p : everyone) {
            r.setPotential(p.getUUID(), false);
            r.remove(p.getUUID());
            r.clearPendingPotentialChoice(p.getUUID());
        }
    }

    private static void kill(ServerPlayer killer, LivingEntity victim) {
        HunterMarkGameTests.kill(killer, victim);
    }

    private static boolean cursed(ServerPlayer p) {
        return BloodlineAccess.get(p).hunterCurse();
    }

    private static OptionalLong end(GameTestHelper helper, ServerPlayer p) {
        return GroupIndex.get(helper.getLevel()).curseUntil(p.getUUID());
    }

    // ---- who is cursed ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aVampireWhoKillsAPlainHunterBrotherIsCursed(GameTestHelper helper) {
        Bh g = bh(helper);
        try {
            kill(g.vampire(), g.hunter());
            helper.assertTrue(cursed(g.vampire()), "the killer is not cursed");
            helper.assertTrue(FiveRoster.get(helper.getLevel()).cursed().contains(g.vampire().getUUID()), "the roster does not hold the killer");
            helper.assertTrue(HunterCurse.nextDue(g.vampire()).isPresent(), "no hallucination timer");
            helper.assertTrue(end(helper, g.vampire()).orElse(-1) == T0 + 48 * HOUR, "the end is " + end(helper, g.vampire()));
        } finally {
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aVampireWhoKillsAPotentialBrotherIsCursed(GameTestHelper helper) {
        Bh g = bh(helper);
        try {
            BloodlineAccess.set(g.hunter(), BloodlineState.NONE.toBuilder().species(Species.POTENTIAL).build());
            kill(g.vampire(), g.hunter());
            helper.assertTrue(cursed(g.vampire()) && end(helper, g.vampire()).orElse(-1) == T0 + 48 * HOUR, "a Potential's death did not curse with an end");
        } finally {
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aVampireWhoKillsAPlainHumanBrotherIsCursed(GameTestHelper helper) {
        Bh g = bh(helper);
        try {
            kill(g.vampire(), g.human());
            helper.assertTrue(cursed(g.vampire()) && end(helper, g.vampire()).isPresent(), "a plain human brother's death did not curse");
        } finally {
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aVampireWhoKillsAMarkedBrotherIsCursedByTheOldPathWithNoTimer(GameTestHelper helper) {
        Bh g = bh(helper);
        try {
            BloodlineAccess.set(g.hunter(), HunterMarkGameTests.state(Species.HUNTER_OF_THE_FIVE, 3));
            kill(g.vampire(), g.hunter());
            helper.assertTrue(cursed(g.vampire()), "the old path did not curse");
            helper.assertTrue(end(helper, g.vampire()).isEmpty(), "a marked hunter's curse got a timed end: " + end(helper, g.vampire()));
            helper.assertTrue(BloodlineAccess.get(g.hunter()).species() != Species.HUNTER_OF_THE_FIVE, "the victim kept the mark");
        } finally {
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSecondKillChangesNothing(GameTestHelper helper) {
        Bh g = bh(helper);
        try {
            kill(g.vampire(), g.hunter());
            long due = HunterCurse.nextDue(g.vampire()).orElse(-1);
            long until = end(helper, g.vampire()).orElse(-1);
            at(T0 + 5 * HOUR);
            kill(g.vampire(), g.human());
            helper.assertTrue(HunterCurse.nextDue(g.vampire()).orElse(-2) == due, "the hallucination timer moved");
            helper.assertTrue(end(helper, g.vampire()).orElse(-2) == until, "the end moved from " + until + " to " + end(helper, g.vampire()));
            helper.assertTrue(FiveRoster.get(helper.getLevel()).cursed().stream().filter(g.vampire().getUUID()::equals).count() == 1, "the roster holds the killer twice");
        } finally {
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aMarkedKillAfterATimedCurseDropsTheTimedEnd(GameTestHelper helper) {
        Bh g = bh(helper);
        try {
            kill(g.vampire(), g.human());
            helper.assertTrue(end(helper, g.vampire()).isPresent(), "setup: no timed end");
            BloodlineAccess.set(g.hunter(), HunterMarkGameTests.state(Species.HUNTER_OF_THE_FIVE, 3));
            kill(g.vampire(), g.hunter());
            helper.assertTrue(cursed(g.vampire()) && end(helper, g.vampire()).isEmpty(), "a marked hunter's curse kept a timed end: " + end(helper, g.vampire()));
            at(T0 + 49 * HOUR);
            BrotherhoodRegistry.sweep(helper.getLevel().getServer());
            helper.assertTrue(cursed(g.vampire()), "the cheap timed curse ended the curse a marked hunter's death brought");
        } finally {
            finish(helper, g);
        }
        helper.succeed();
    }

    // ---- the end ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theCurseEndsAfterFortyEightHours(GameTestHelper helper) {
        Bh g = bh(helper);
        try {
            kill(g.vampire(), g.human());
            at(T0 + 48 * HOUR - 1);
            BrotherhoodRegistry.sweep(helper.getLevel().getServer());
            helper.assertTrue(cursed(g.vampire()) && end(helper, g.vampire()).isPresent(), "the curse ended at 47 h 59 m");
            at(T0 + 48 * HOUR);
            BrotherhoodRegistry.sweep(helper.getLevel().getServer());
            helper.assertTrue(!cursed(g.vampire()), "the curse did not end at 48 h");
            helper.assertTrue(end(helper, g.vampire()).isEmpty() && !FiveRoster.get(helper.getLevel()).cursed().contains(g.vampire().getUUID()), "a row stayed");
            helper.assertTrue(HunterCurse.nextDue(g.vampire()).isEmpty(), "the hallucination timer stayed");
        } finally {
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void itEndsAtLoginWhenItEndedWhileOffline(GameTestHelper helper) {
        Bh g = bh(helper);
        try {
            kill(g.vampire(), g.human());
            online(g.leader(), g.hunter(), g.human());
            HunterMarkGameTests.online(g.leader(), g.hunter(), g.human());
            at(T0 + 49 * HOUR);
            BrotherhoodRegistry.sweep(helper.getLevel().getServer());
            helper.assertTrue(FiveRoster.get(helper.getLevel()).liftedCurses().contains(g.vampire().getUUID()), "the offline killer is not marked to be freed");
            helper.assertTrue(end(helper, g.vampire()).isEmpty(), "the row stayed");
            online(g.leader(), g.hunter(), g.human(), g.vampire());
            HunterMarkGameTests.online(g.leader(), g.hunter(), g.human(), g.vampire());
            HunterCurse.onLogin(g.vampire());
            helper.assertTrue(!cursed(g.vampire()), "the login did not free the killer");
        } finally {
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anAwakeningEndsItAtOnceAndClearsTheRows(GameTestHelper helper) {
        Bh g = bh(helper);
        ServerPlayer other = HunterMarkGameTests.vampire(helper);
        try {
            kill(g.vampire(), g.human());
            kill(other, g.hunter());
            helper.assertTrue(end(helper, g.vampire()).isPresent() && end(helper, other).isPresent(), "setup: no rows");
            HunterMarkGameTests.online(g.leader(), g.hunter(), g.human(), g.vampire(), other);
            HunterCurse.liftAll(helper.getLevel().getServer());
            helper.assertTrue(!cursed(g.vampire()) && !cursed(other), "an awakening left a curse");
            helper.assertTrue(end(helper, g.vampire()).isEmpty() && end(helper, other).isEmpty(), "an awakening left a row");
        } finally {
            HunterMarkGameTests.reset(other);
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anAwakeningClearsTheRowOfSomeoneWhoIsAway(GameTestHelper helper) {
        Bh g = bh(helper);
        try {
            kill(g.vampire(), g.human());
            HunterMarkGameTests.online(g.leader(), g.hunter(), g.human());
            HunterCurse.liftAll(helper.getLevel().getServer());
            helper.assertTrue(end(helper, g.vampire()).isEmpty(), "the row of an offline killer stayed after an awakening");
            helper.assertTrue(FiveRoster.get(helper.getLevel()).liftedCurses().contains(g.vampire().getUUID()), "the offline killer is not freed at login");
        } finally {
            finish(helper, g);
        }
        helper.succeed();
    }

    // ---- who is not ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aHumanKillerIsNotCursed(GameTestHelper helper) {
        Bh g = bh(helper);
        ServerPlayer human = HunterMarkGameTests.human(helper);
        try {
            kill(human, g.human());
            helper.assertTrue(!cursed(human) && end(helper, human).isEmpty(), "a human killer was cursed");
        } finally {
            HunterMarkGameTests.reset(human);
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aWitchKillerIsNotCursed(GameTestHelper helper) {
        Bh g = bh(helper);
        ServerPlayer witch = HunterMarkGameTests.vampire(helper);
        try {
            BloodlineAccess.set(witch, BloodlineState.NONE.toBuilder().species(Species.WITCH).build());
            kill(witch, g.human());
            helper.assertTrue(!cursed(witch) && end(helper, witch).isEmpty(), "a witch killer was cursed");
        } finally {
            HunterMarkGameTests.reset(witch);
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aKillByTheWorldCursesNobody(GameTestHelper helper) {
        Bh g = bh(helper);
        try {
            NeoForge.EVENT_BUS.post(new LivingDeathEvent(g.human(), g.human().damageSources().fall()));
            helper.assertTrue(!cursed(g.vampire()) && !cursed(g.leader()) && GroupIndex.get(helper.getLevel()).curses().isEmpty(), "the world's kill cursed somebody");
        } finally {
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aKillByANonPreyCursesNobody(GameTestHelper helper) {
        Bh g = bh(helper);
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 1, 3));
        try {
            NeoForge.EVENT_BUS.post(new LivingDeathEvent(g.human(), g.human().damageSources().mobAttack(zombie)));
            helper.assertTrue(GroupIndex.get(helper.getLevel()).curses().isEmpty() && !cursed(g.leader()), "a zombie's kill cursed somebody");
        } finally {
            zombie.discard();
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPreyMemberWhoKillsHisOwnBrotherIsCursed(GameTestHelper helper) {
        Bh g = bh(helper);
        TalkingDuelist prey = GroupTestKit.vampire(helper, 4.5);
        online(g.leader(), g.hunter(), g.human(), prey);
        try {
            helper.assertTrue(join(g.leader(), prey), "a vampire could not join");
            kill(prey, g.human());
            helper.assertTrue(cursed(prey) && end(helper, prey).isPresent(), "a prey member who killed his own brother was spared");
        } finally {
            finish(helper, g, prey);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aMemberWhoDoesNotFitIsNoBrother(GameTestHelper helper) {
        Bh g = bh(helper);
        TalkingDuelist member = GroupTestKit.vampire(helper, 4.5);
        online(g.leader(), g.hunter(), g.human(), member, g.vampire());
        try {
            helper.assertTrue(join(g.leader(), member), "a vampire could not join");
            kill(g.vampire(), member);
            helper.assertTrue(!cursed(g.vampire()) && end(helper, g.vampire()).isEmpty(), "the death of a vampire member cursed somebody");
        } finally {
            finish(helper, g, member);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aVictimInNoGroupIsNoBrother(GameTestHelper helper) {
        Bh g = bh(helper);
        ServerPlayer loner = HunterMarkGameTests.huntNone(helper);
        try {
            kill(g.vampire(), loner);
            helper.assertTrue(!cursed(g.vampire()), "the death of somebody in no group cursed the killer");
        } finally {
            HunterMarkGameTests.reset(loner);
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPackMemberVictimIsNoBrother(GameTestHelper helper) {
        Bh g = bh(helper);
        TalkingDuelist hybrid = human(helper, 4.5);
        TalkingDuelist mate = human(helper, 5.5);
        online(g.leader(), g.hunter(), g.human(), g.vampire(), hybrid, mate);
        GroupsConfig.override("open_founding", true);
        try {
            BloodlineAccess.set(hybrid, BloodlineState.NONE.toBuilder().species(Species.HYBRID).build());
            helper.assertTrue(join(hybrid, mate), "no pack");
            kill(g.vampire(), mate);
            helper.assertTrue(!cursed(g.vampire()), "a pack member's death cursed the killer");
        } finally {
            finish(helper, g, hybrid, mate);
        }
        helper.succeed();
    }

    // ---- the switches ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void curseExtendsOffMeansOnlyTheOldPath(GameTestHelper helper) {
        Bh g = bh(helper);
        try {
            GroupsConfig.override("curse_extends_enabled", false);
            kill(g.vampire(), g.human());
            helper.assertTrue(!cursed(g.vampire()), "the extension ran with its switch off");
            BloodlineAccess.set(g.hunter(), HunterMarkGameTests.state(Species.HUNTER_OF_THE_FIVE, 2));
            kill(g.vampire(), g.hunter());
            helper.assertTrue(cursed(g.vampire()) && end(helper, g.vampire()).isEmpty(), "the old path stopped with the switch off");
        } finally {
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void brotherhoodOffMeansOnlyTheOldPath(GameTestHelper helper) {
        Bh g = bh(helper);
        try {
            GroupsConfig.override("brotherhood_enabled", false);
            kill(g.vampire(), g.human());
            helper.assertTrue(!cursed(g.vampire()), "the extension ran with brotherhoods off");
        } finally {
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theExpiryRunsWithBothSwitchesOff(GameTestHelper helper) {
        Bh g = bh(helper);
        try {
            kill(g.vampire(), g.human());
            GroupsConfig.override("curse_extends_enabled", false);
            GroupsConfig.override("brotherhood_enabled", false);
            at(T0 + 48 * HOUR);
            BrotherhoodRegistry.sweep(helper.getLevel().getServer());
            helper.assertTrue(!cursed(g.vampire()) && end(helper, g.vampire()).isEmpty(), "the curse kept running with the switches off");
        } finally {
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCuredKillerLosesTheRow(GameTestHelper helper) {
        Bh g = bh(helper);
        try {
            kill(g.vampire(), g.human());
            BloodlineAccess.applyEvent(g.vampire(), net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineEvent.HUNTER_CURSE_LIFTED,
                    net.ofatech.supernaturalbloodlines.parts.species.SpeciesContext.of(g.vampire(), null));
            helper.assertTrue(!cursed(g.vampire()) && end(helper, g.vampire()).isPresent(), "setup: the flag is gone but the row should wait for the sweep");
            BrotherhoodRegistry.sweep(helper.getLevel().getServer());
            helper.assertTrue(end(helper, g.vampire()).isEmpty(), "the row of a cured killer stayed");
        } finally {
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theHallucinationsAreTheExistingOnes(GameTestHelper helper) {
        Bh g = bh(helper);
        int[] sent = {0};
        HallucinationPayload.overrideSpy((p, kind) -> sent[0]++);
        try {
            kill(g.vampire(), g.human());
            long due = HunterCurse.nextDue(g.vampire()).orElseThrow();
            HunterCurse.tick(helper.getLevel().getServer(), due - 1);
            helper.assertTrue(sent[0] == 0, "a hallucination came early");
            HunterCurse.tick(helper.getLevel().getServer(), due);
            helper.assertTrue(sent[0] == 1, "no hallucination at the timer: " + sent[0]);
        } finally {
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCancelledDeathCursesNobody(GameTestHelper helper) {
        Bh g = bh(helper);
        Consumer<LivingDeathEvent> cancel = e -> e.setCanceled(true);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, LivingDeathEvent.class, cancel);
        try {
            NeoForge.EVENT_BUS.post(new LivingDeathEvent(g.human(), g.human().damageSources().source(DamageTypes.GENERIC_KILL, g.vampire())));
            helper.assertTrue(!cursed(g.vampire()), "a cancelled death cursed the killer");
        } finally {
            NeoForge.EVENT_BUS.unregister(cancel);
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aKillerWhoLogsOutKeepsTheRowAndTheEnd(GameTestHelper helper) {
        Bh g = bh(helper);
        try {
            kill(g.vampire(), g.human());
            long until = end(helper, g.vampire()).orElse(-1);
            HunterCurse.onLogout(g.vampire());
            online(g.leader(), g.hunter(), g.human());
            at(T0 + HOUR);
            helper.assertTrue(cursed(g.vampire()) && end(helper, g.vampire()).orElse(-2) == until, "the row or the flag was lost at logout");
            online(g.leader(), g.hunter(), g.human(), g.vampire());
            HunterCurse.onLogin(g.vampire());
            helper.assertTrue(HunterCurse.nextDue(g.vampire()).isPresent() && end(helper, g.vampire()).orElse(-2) == until, "login lost the timer or the end");
        } finally {
            finish(helper, g);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theRowsOfOtherPlayersAreNeverTouchedByAnotherLift(GameTestHelper helper) {
        Bh g = bh(helper);
        UUID stranger = UUID.randomUUID();
        try {
            GroupIndex.get(helper.getLevel()).setCurse(stranger, T0 + HOUR);
            HunterCurse.liftOne(g.leader());
            helper.assertTrue(GroupIndex.get(helper.getLevel()).curseUntil(stranger).isPresent(), "another player's row was cleared by a lift");
        } finally {
            GroupIndex.get(helper.getLevel()).clearCurse(stranger);
            finish(helper, g);
        }
        helper.succeed();
    }

    // ---- round 2 ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aVampireWhoKillsABrotherWhoAttackedFirstIsNotCursed(GameTestHelper helper) {
        Bh g = bh(helper);
        TalkingDuelist vampire = GroupTestKit.vampire(helper, 4.5);
        online(g.leader(), g.hunter(), g.human(), vampire);
        HunterMarkGameTests.online(g.leader(), g.hunter(), g.human(), vampire);
        try {
            vampire.hurt(g.human().damageSources().playerAttack(g.human()), 1f);
            helper.assertTrue(vampire.getLastHurtByMob() == g.human(), "setup: the brother's hit did not land");
            kill(vampire, g.human());
            helper.assertTrue(!cursed(vampire) && end(helper, vampire).isEmpty(), "a vampire who killed a brother in self-defence was cursed");
        } finally {
            finish(helper, g, vampire);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theKillerHearsTheCurseStartAndLift(GameTestHelper helper) {
        Bh g = bh(helper);
        TalkingDuelist killer = GroupTestKit.vampire(helper, 4.5);
        online(g.leader(), g.hunter(), g.human(), killer);
        HunterMarkGameTests.online(g.leader(), g.hunter(), g.human(), killer);
        try {
            kill(killer, g.human());
            var start = killer.message("supernaturalbloodlines.brotherhood.curse_start");
            helper.assertTrue(start != null && List.of(((net.minecraft.network.chat.contents.TranslatableContents) start.getContents()).getArgs()).equals(List.of(48)),
                    "the start line: " + start);
            at(T0 + 48 * HOUR);
            BrotherhoodRegistry.sweep(helper.getLevel().getServer());
            helper.assertTrue(!cursed(killer) && killer.received("supernaturalbloodlines.brotherhood.curse_lifted"), "no line when the curse lifted");
        } finally {
            finish(helper, g, killer);
        }
        helper.succeed();
    }
}