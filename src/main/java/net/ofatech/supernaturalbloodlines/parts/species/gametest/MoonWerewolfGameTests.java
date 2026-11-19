package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.werewolves.api.WReference;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.AwakeningRules;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.core.moon.MoonRule;
import net.ofatech.supernaturalbloodlines.core.moon.SleepRules.Mode;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.moon.AwakeningQueue;
import net.ofatech.supernaturalbloodlines.parts.species.moon.DelayedAwakening;
import net.ofatech.supernaturalbloodlines.parts.species.pack.GeneHandler;
import net.ofatech.supernaturalbloodlines.shared.supernatural.WorldNowHook;

/** R141 (the werewolf mode) and R142 (delayed awakening) with a real werewolf and a real kill. Werewolves only. */
@PrefixGameTestTemplate(false)
public final class MoonWerewolfGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final WolfState GENE = WolfState.NONE.toBuilder().geneRolled(true).latentGene(true).build();
    private static final long DAY = 24000L * 24; // phase 0

    private MoonWerewolfGameTests() {
    }

    private static ServerPlayer carrier(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().wolf(GENE).build());
        return p;
    }

    private static void killVillager(GameTestHelper helper, ServerPlayer killer) {
        Villager v = helper.spawn(EntityType.VILLAGER, new BlockPos(1, 1, 1));
        NeoForge.EVENT_BUS.post(new LivingDeathEvent((LivingEntity) v, killer.damageSources().playerAttack(killer)));
    }

    private static boolean werewolf(ServerPlayer p) {
        return VampirismAPI.factionPlayerHandler(p).getCurrentFaction() == WReference.WEREWOLF_FACTION;
    }

    // ---- R141 ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aWerewolfCannotSleepOnAFullMoonNightInWerewolvesMode(GameTestHelper helper) {
        ServerPlayer wolf = TestPlayers.createMortal(helper);
        ServerPlayer human = TestPlayers.createMortal(helper);
        ServerLevel level = helper.getLevel();
        long saved = level.getDayTime();
        SpeciesConfig.overrideFullMoonSleep(Mode.WEREWOLVES);
        try {
            VampirismAPI.factionPlayerHandler(wolf).setFactionAndLevel(WReference.WEREWOLF_FACTION, 1);
            level.setDayTime(DAY + 13000);
            CanPlayerSleepEvent w = new CanPlayerSleepEvent(wolf, BlockPos.ZERO, null);
            CanPlayerSleepEvent h = new CanPlayerSleepEvent(human, BlockPos.ZERO, null);
            NeoForge.EVENT_BUS.post(w);
            NeoForge.EVENT_BUS.post(h);
            helper.assertTrue(w.getProblem() == Player.BedSleepingProblem.OTHER_PROBLEM, "the werewolf was allowed to sleep");
            helper.assertTrue(h.getProblem() == null, "the human was refused");
            WorldNowHook.Flags fw = WorldNowHook.of(wolf);
            WorldNowHook.Flags fh = WorldNowHook.of(human);
            helper.assertTrue(fw.sleepRule() && !fh.sleepRule(), "the hub flag says " + fw + " / " + fh);
        } finally {
            SpeciesConfig.overrideFullMoonSleep(null);
            level.setDayTime(saved);
            VampirismAPI.factionPlayerHandler(wolf).leaveFaction(true);
        }
        helper.succeed();
    }

    // ---- R142 ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theKillQueuesTheAwakeningInsteadOfDoingIt(GameTestHelper helper) {
        ServerPlayer p = carrier(helper);
        ServerLevel level = helper.getLevel();
        long saved = level.getDayTime();
        SpeciesConfig.overrideDelayedAwakeningNights(1);
        try {
            level.setDayTime(1000);
            killVillager(helper, p);
            helper.assertTrue(BloodlineAccess.get(p).wolf().lineage() == WolfState.Lineage.NONE && !werewolf(p), "the kill awakened them at once");
            helper.assertTrue(AwakeningQueue.pending(p), "nothing was queued");
            helper.assertTrue(AwakeningQueue.of(p).queuedAt() == level.getGameTime(), "the game time of the kill was not kept");
            helper.assertTrue(AwakeningQueue.dueOf(p) == MoonRule.NIGHT_START, "due at " + AwakeningQueue.dueOf(p) + ", not the first dusk");
            helper.assertTrue(WorldNowHook.of(p).awakeningPending(), "the hub flag did not show it");
        } finally {
            SpeciesConfig.overrideDelayedAwakeningNights(null);
            level.setDayTime(saved);
            BloodlineAccess.set(p, BloodlineState.NONE);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void itWaitsForDuskAndThenAwakens(GameTestHelper helper) {
        ServerPlayer p = carrier(helper);
        ServerLevel level = helper.getLevel();
        long saved = level.getDayTime();
        SpeciesConfig.overrideDelayedAwakeningNights(1);
        try {
            level.setDayTime(1000);
            killVillager(helper, p);
            level.setDayTime(MoonRule.NIGHT_START - 1);
            DelayedAwakening.check(p);
            helper.assertTrue(AwakeningQueue.pending(p) && !werewolf(p), "it awakened before dusk");
            level.setDayTime(MoonRule.NIGHT_START);
            DelayedAwakening.check(p);
            helper.assertTrue(werewolf(p) && BloodlineAccess.get(p).wolf().lineage() == WolfState.Lineage.BORN, "it did not awaken at dusk: " + BloodlineAccess.get(p).wolf());
            helper.assertTrue(!AwakeningQueue.pending(p), "the queue was not cleared");
        } finally {
            SpeciesConfig.overrideDelayedAwakeningNights(null);
            level.setDayTime(saved);
            VampirismAPI.factionPlayerHandler(p).leaveFaction(true);
            BloodlineAccess.set(p, BloodlineState.NONE);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void zeroNightsAwakensAtOnce(GameTestHelper helper) {
        ServerPlayer p = carrier(helper);
        SpeciesConfig.overrideDelayedAwakeningNights(0);
        try {
            killVillager(helper, p);
            helper.assertTrue(werewolf(p) && !AwakeningQueue.pending(p), "0 nights did not awaken at once");
        } finally {
            SpeciesConfig.overrideDelayedAwakeningNights(null);
            VampirismAPI.factionPlayerHandler(p).leaveFaction(true);
            BloodlineAccess.set(p, BloodlineState.NONE);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSecondKillDoesNotMoveTheDueTime(GameTestHelper helper) {
        ServerPlayer p = carrier(helper);
        ServerLevel level = helper.getLevel();
        long saved = level.getDayTime();
        SpeciesConfig.overrideDelayedAwakeningNights(1);
        try {
            level.setDayTime(1000);
            killVillager(helper, p);
            long due = AwakeningQueue.dueOf(p);
            level.setDayTime(5000);
            killVillager(helper, p);
            helper.assertTrue(AwakeningQueue.dueOf(p) == due, "the due time moved from " + due + " to " + AwakeningQueue.dueOf(p));
        } finally {
            SpeciesConfig.overrideDelayedAwakeningNights(null);
            level.setDayTime(saved);
            BloodlineAccess.set(p, BloodlineState.NONE);
        }
        helper.succeed();
    }

    /** doDaylightCycle=false: the day time never reaches the dusk, so the game time since the kill has to do it. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aFrozenClockStillAwakensAfterTheNights(GameTestHelper helper) {
        ServerPlayer p = carrier(helper);
        ServerLevel level = helper.getLevel();
        long saved = level.getDayTime();
        SpeciesConfig.overrideDelayedAwakeningNights(1);
        try {
            level.setDayTime(1000);
            killVillager(helper, p);
            long game = level.getGameTime();
            AwakeningQueue.set(p, new AwakeningRules.Pending(AwakeningQueue.dueOf(p), game - 24000L + 1)); // one tick short of a day, the clock still at 1000
            DelayedAwakening.check(p);
            helper.assertTrue(AwakeningQueue.pending(p) && !werewolf(p), "it awakened a tick early on a frozen clock");
            AwakeningQueue.set(p, new AwakeningRules.Pending(AwakeningQueue.dueOf(p), game - 24000L));
            DelayedAwakening.check(p);
            helper.assertTrue(werewolf(p) && !AwakeningQueue.pending(p), "a frozen clock never awakened it");
        } finally {
            SpeciesConfig.overrideDelayedAwakeningNights(null);
            level.setDayTime(saved);
            VampirismAPI.factionPlayerHandler(p).leaveFaction(true);
            BloodlineAccess.set(p, BloodlineState.NONE);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aKillWithoutTheGeneQueuesNothing(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        SpeciesConfig.overrideDelayedAwakeningNights(1);
        try {
            killVillager(helper, p);
            helper.assertTrue(!AwakeningQueue.pending(p), "someone with no gene was queued");
        } finally {
            SpeciesConfig.overrideDelayedAwakeningNights(null);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void joiningAnotherFactionCancelsIt(GameTestHelper helper) {
        ServerPlayer p = carrier(helper);
        ServerLevel level = helper.getLevel();
        long saved = level.getDayTime();
        SpeciesConfig.overrideDelayedAwakeningNights(1);
        try {
            level.setDayTime(1000);
            killVillager(helper, p);
            VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1);
            level.setDayTime(MoonRule.NIGHT_START + 100);
            DelayedAwakening.check(p);
            helper.assertTrue(!AwakeningQueue.pending(p), "the queue stayed");
            helper.assertTrue(!werewolf(p) && BloodlineAccess.get(p).wolf().lineage() == WolfState.Lineage.NONE, "a vampire was awakened as a wolf");
        } finally {
            SpeciesConfig.overrideDelayedAwakeningNights(null);
            level.setDayTime(saved);
            VampirismAPI.factionPlayerHandler(p).leaveFaction(true);
            BloodlineAccess.set(p, BloodlineState.NONE);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void turningTheSettingOffReleasesAWaitingAwakening(GameTestHelper helper) {
        ServerPlayer p = carrier(helper);
        ServerLevel level = helper.getLevel();
        long saved = level.getDayTime();
        try {
            level.setDayTime(1000);
            SpeciesConfig.overrideDelayedAwakeningNights(2);
            killVillager(helper, p);
            helper.assertTrue(AwakeningQueue.dueOf(p) == AwakeningRules.due(1000, 2), "two nights was not due after two dusks");
            SpeciesConfig.overrideDelayedAwakeningNights(0);
            DelayedAwakening.check(p);
            helper.assertTrue(werewolf(p), "the awakening stayed queued with the setting at 0");
        } finally {
            SpeciesConfig.overrideDelayedAwakeningNights(null);
            level.setDayTime(saved);
            VampirismAPI.factionPlayerHandler(p).leaveFaction(true);
            BloodlineAccess.set(p, BloodlineState.NONE);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theQueueIsKeptThroughDeath(GameTestHelper helper) {
        ServerPlayer before = carrier(helper);
        ServerPlayer after = TestPlayers.createMortal(helper);
        before.setData(SpeciesRegistries.PENDING_AWAKENING, new AwakeningRules.Pending(12786L, 77L));
        after.restoreFrom(before, false);
        helper.assertTrue(after.getData(SpeciesRegistries.PENDING_AWAKENING).equals(new AwakeningRules.Pending(12786L, 77L)), "the new body forgot the queue: " + after.getData(SpeciesRegistries.PENDING_AWAKENING));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aRiteOfLineageIsNotDelayed(GameTestHelper helper) {
        ServerPlayer p = carrier(helper);
        SpeciesConfig.overrideDelayedAwakeningNights(1);
        try {
            String refused = GeneHandler.awakenHuman(p, PackEvent.RITE_OF_LINEAGE_AWAKEN);
            helper.assertTrue(refused == null && werewolf(p), "the rite was delayed or refused: " + refused);
        } finally {
            SpeciesConfig.overrideDelayedAwakeningNights(null);
            VampirismAPI.factionPlayerHandler(p).leaveFaction(true);
            BloodlineAccess.set(p, BloodlineState.NONE);
        }
        helper.succeed();
    }
}
