package net.ofatech.supernaturalbloodlines.parts.supernatural.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.player.vampire.IDrinkBloodContext;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.EnabledParts;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalState;
import net.ofatech.supernaturalbloodlines.core.supernatural.TaskEvent;
import net.ofatech.supernaturalbloodlines.core.supernatural.TaskSpec.Detector;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.supernatural.task.TaskTracker;
import net.ofatech.supernaturalbloodlines.shared.supernatural.TaskSignals;

/** The detectors on real players: an event in, a task done or counted in the attachment. */
@PrefixGameTestTemplate(false)
public final class DetectorGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final String RITE = NS + ":";

    private DetectorGameTests() {}

    private static SupernaturalState state(ServerPlayer p) {
        return SupernaturalAccess.get(p);
    }

    private static void vampireAt(GameTestHelper helper, ServerPlayer p, int level) {
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, level), "could not make a level " + level + " vampire");
    }

    private static void rung(ServerPlayer p, SupernaturalRace race, int rung) {
        SupernaturalAccess.set(p, state(p).toBuilder().rung(race, rung).build());
    }

    @SuppressWarnings("unchecked")
    private static Mob vampireMob(GameTestHelper helper) {
        return helper.spawnWithNoFreeWill((EntityType<Mob>) BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("vampirism:vampire")), new BlockPos(1, 1, 1));
    }

    private static void drinkFrom(ServerPlayer drinker, LivingEntity source) {
        VampirismAPI.vampirePlayer(drinker).drinkBlood(2, 0.1f, new IDrinkBloodContext() {
            @Override
            public Optional<LivingEntity> getEntity() {
                return Optional.of(source);
            }

            @Override
            public Optional<ItemStack> getStack() {
                return Optional.empty();
            }

            @Override
            public Optional<BlockState> getBlockState() {
                return Optional.empty();
            }

            @Override
            public Optional<BlockPos> getBlockPos() {
                return Optional.empty();
            }
        });
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void levelStepCompletesTask(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        vampireAt(helper, p, 1);
        helper.assertTrue(!state(p).done().contains("L-VAM-1a"), "level 1 completed the level 2 step");
        vampireAt(helper, p, 2);
        helper.assertTrue(state(p).done().contains("L-VAM-1a"), "level 2 did not complete L-VAM-1a: " + state(p).done());
        helper.assertTrue(state(p).raceSince().containsKey("vampire"), "the race was not stamped");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void levelReachedEarlyCountsOnActivation(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        vampireAt(helper, p, 5);
        helper.assertTrue(state(p).done().contains("L-VAM-1a") && !state(p).done().contains("L-VAM-2a"), "setup: " + state(p).done());
        rung(p, SupernaturalRace.VAMPIRE, 1);
        helper.assertTrue(state(p).done().contains("L-VAM-2a"), "the level 4 step was not found done when its rung became active: " + state(p).done());
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void humanLikeFeedCounts(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        vampireAt(helper, p, 1);
        drinkFrom(p, EntityType.VILLAGER.create(helper.getLevel()));
        helper.assertTrue(state(p).counters().getOrDefault("L-VAM-1b", 0L) == 1, "a villager's blood was not counted: " + state(p).counters());
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void nonHumanFeedIgnored(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        vampireAt(helper, p, 1);
        drinkFrom(p, EntityType.COW.create(helper.getLevel()));
        helper.assertTrue(state(p).counters().isEmpty(), "a cow's blood was counted: " + state(p).counters());
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void riteSignalCompletesTask(GameTestHelper helper) {
        ServerPlayer witch = TestPlayers.create(helper);
        ServerPlayer other = TestPlayers.create(helper);
        BloodlineAccess.set(witch, BloodlineState.NONE.toBuilder().species(Species.WITCH).build());
        TaskSignals.rite(witch, other, RITE + "ancestral_calling");
        helper.assertTrue(state(witch).counters().getOrDefault("L-WIT-1a", 0L) == 1, "the performer was not credited: " + state(witch).counters());
        helper.assertTrue(state(other).counters().isEmpty() && state(other).done().isEmpty(), "the target was credited as a performer");
        // the Rite of Lineage (Reveal) on someone else is the witch's L-WIT-1b, but only with Werewolves (it needs the gene)
        if (ModList.get().isLoaded("werewolves")) {
            TaskSignals.rite(witch, other, RITE + "rite_of_lineage_reveal");
            helper.assertTrue(state(witch).done().contains("L-WIT-1b"), "the reveal did not complete L-WIT-1b: " + state(witch).done());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aRiteOnYourselfIsBothRoles(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(Species.WITCH).build());
        TaskSignals.rite(p, p, RITE + "ancestral_calling");
        helper.assertTrue(state(p).counters().getOrDefault("L-WIT-1a", 0L) == 1, "performing on yourself did not count");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void compelSignalCounts(GameTestHelper helper) {
        if (!EnabledParts.isEnabled("species")) {
            helper.succeed(); // the task is dropped without the species part (LadderDataGameTests)
            return;
        }
        ServerPlayer p = TestPlayers.create(helper);
        vampireAt(helper, p, 1);
        rung(p, SupernaturalRace.VAMPIRE, 2);
        TaskSignals.fire(p, "compel", 2);
        helper.assertTrue(state(p).counters().getOrDefault("L-VAM-3b", 0L) == 2, "compelling was not counted: " + state(p).counters());
        TaskSignals.fire(p, "compel", 1);
        helper.assertTrue(state(p).done().contains("L-VAM-3b") && state(p).counters().isEmpty(), "three compulsions did not finish the task");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void venomHitCounts(GameTestHelper helper) {
        if (!ModList.get().isLoaded("werewolves")) {
            helper.succeed();
            return;
        }
        ServerPlayer p = TestPlayers.create(helper);
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(Species.HYBRID).build());
        rung(p, SupernaturalRace.HYBRID, 2);
        TaskSignals.fire(p, "venom_hit", 3);
        helper.assertTrue(state(p).done().contains("L-HYB-3a"), "three venom hits did not complete L-HYB-3a: " + state(p).done() + state(p).counters());
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void ghoulRecordsNothing(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        vampireAt(helper, p, 1);
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().ghoul(true).build());
        drinkFrom(p, EntityType.VILLAGER.create(helper.getLevel()));
        TaskTracker.emit(p, TaskEvent.of(Detector.LEVEL, 14, "faction", "vampirism:vampire"));
        TaskTracker.periodic(p);
        SupernaturalState s = state(p);
        helper.assertTrue(s.done().isEmpty() && s.counters().isEmpty() && s.lifetime().isEmpty(), "a Ghoul progressed: " + s.done() + s.counters());
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void countingOnlyWhileActive(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        vampireAt(helper, p, 1);
        TaskTracker.emit(p, TaskEvent.of(Detector.KILL, 1, "category", "hunter")); // L-VAM-4b is three rungs ahead
        helper.assertTrue(state(p).counters().isEmpty(), "a kill counted before its rung was active: " + state(p).counters());
        rung(p, SupernaturalRace.VAMPIRE, 3);
        TaskTracker.emit(p, TaskEvent.of(Detector.KILL, 1, "category", "hunter"));
        helper.assertTrue(state(p).counters().getOrDefault("L-VAM-4b", 0L) == 1, "a kill did not count once its rung was active");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void killOfPreyCounts(GameTestHelper helper) {
        if (!EnabledParts.isEnabled("vampirismEnchanted")) {
            helper.succeed();
            return;
        }
        ServerPlayer hunter = TestPlayers.create(helper);
        // a mob: a vampire player is put into Vampirism's dying state, the death is cancelled, and nothing was killed
        LivingEntity victim = vampireMob(helper);
        // a human's L-HUM-2b (kill prey) is on the second rung
        rung(hunter, SupernaturalRace.HUMAN, 1);
        NeoForge.EVENT_BUS.post(new LivingDeathEvent(victim, hunter.damageSources().playerAttack(hunter)));
        helper.assertTrue(state(hunter).counters().getOrDefault("L-HUM-2b", 0L) == 1, "killing a vampire was not a prey kill: " + state(hunter).counters());
        // a kill by something that is not a player counts for nobody
        NeoForge.EVENT_BUS.post(new LivingDeathEvent(victim, victim.damageSources().generic()));
        helper.assertTrue(state(hunter).counters().get("L-HUM-2b") == 1, "a kill nobody made counted");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void vervainTeaCounts(GameTestHelper helper) {
        if (!EnabledParts.isEnabled("artifacts")) {
            helper.succeed();
            return;
        }
        ServerPlayer p = TestPlayers.create(helper);
        ItemStack tea = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(NS + ":vervain_tea")));
        NeoForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Finish(p, tea, 0, tea));
        helper.assertTrue(state(p).done().contains("L-HUM-1a"), "drinking vervain tea did not complete L-HUM-1a: " + state(p).done());
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void survivingAHitFromPreyCounts(GameTestHelper helper) {
        if (!EnabledParts.isEnabled("vampirismEnchanted")) {
            helper.succeed();
            return;
        }
        LivingEntity attacker = vampireMob(helper);
        ServerPlayer victim = TestPlayers.createVulnerable(helper);
        helper.assertTrue(victim.hurt(victim.damageSources().mobAttack(attacker), 1.0F), "setup: the hit did not land");
        helper.assertTrue(state(victim).done().contains("L-HUM-1b"), "surviving a vampire's hit did not complete L-HUM-1b: " + state(victim).done());
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void stateTasksAreReadOffTheBloodlineState(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(Species.SIPHONER).siphonedPower(999).build());
        rung(p, SupernaturalRace.SIPHONER, 2);
        helper.assertTrue(!state(p).done().contains("L-SIP-3a"), "999 power completed the 1,000 step");
        BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().siphonedPower(1000).build());
        TaskTracker.recheck(p);
        helper.assertTrue(state(p).done().contains("L-SIP-3a"), "1,000 siphoned power did not complete L-SIP-3a: " + state(p).done());
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void daysAsARaceAreReadFromTheStamp(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        vampireAt(helper, p, 1);
        rung(p, SupernaturalRace.VAMPIRE, 4);
        TaskTracker.periodic(p);
        helper.assertTrue(!state(p).done().contains("L-VAM-5b"), "no days had passed");
        SupernaturalAccess.set(p, state(p).toBuilder().raceSince(Map.of("vampire", -31L * 24_000L)).build());
        TaskTracker.periodic(p);
        helper.assertTrue(state(p).done().contains("L-VAM-5b"), "31 days as a vampire did not complete L-VAM-5b: " + state(p).done());
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void inTotalCountsStartAtOnceAndSurviveARaceChange(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(Species.SIPHONER).build());
        TaskSignals.fire(p, "siphon_power", 1500);
        TaskSignals.fire(p, "siphon_power", 1500);
        helper.assertTrue(state(p).lifetime().getOrDefault("siphoner/signal?signal=siphon_power", 0L) == 3000, "the lifetime count is " + state(p).lifetime());
        helper.assertTrue(state(p).done().contains("L-SIP-1a"), "3,000 siphoned did not complete the 2,000 step");
        // the count belongs to the race, not to the rung: it is still there after the species changes and back
        BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().species(Species.NONE).build());
        BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().species(Species.SIPHONER).build());
        helper.assertTrue(state(p).lifetime().getOrDefault("siphoner/signal?signal=siphon_power", 0L) == 3000, "the lifetime count was lost");
        helper.succeed();
    }
}
