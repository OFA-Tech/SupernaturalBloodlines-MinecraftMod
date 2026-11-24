package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import static net.ofatech.supernaturalbloodlines.parts.species.gametest.WitchGameTests.altar;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.WitchGameTests.chalk;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.WitchGameTests.set;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import net.favouriteless.enchanted.api.circle_magic.RiteFactory;
import net.favouriteless.enchanted.common.blocks.entity.AltarBlockEntity;
import net.favouriteless.enchanted.common.blocks.entity.GoldChalkBlockEntity;
import net.favouriteless.enchanted.common.enchanted.circle_magic.RiteManager;
import net.favouriteless.enchanted.common.enchanted.circle_magic.RiteType;
import net.favouriteless.enchanted.common.enchanted.circle_magic.RiteWeatherRequirement;
import net.favouriteless.enchanted.common.enchanted.circle_magic.rites.Rite;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.mixin.SpeciesMixinPlugin;
import net.ofatech.supernaturalbloodlines.parts.species.witch.RiteCost;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport;

/**
 * Plan 20b Task 0: what the circle link rests on, proved with a real gold chalk and a throwaway rite type whose factory writes down what happens.
 * Every proof drives the chalk's own {@code serverTick} on a tick divisible by 20 (the only one it acts on) and never waits on {@code tickCount}.
 */
@PrefixGameTestTemplate(false)
public final class CovenHookGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final String BATCH = "covens_hooks";

    private CovenHookGameTests() {
    }

    /** A rite type whose rite writes {@code onStart} and {@code onTick} into {@code log}; {@code persistent} keeps it in the manager. */
    private record Probe(List<String> log, AtomicReference<Rite> made, boolean persistent) implements RiteFactory {
        @Override
        public Rite create(Rite.BaseRiteParams base, Rite.RiteParams params) {
            Rite rite = new ProbeRite(base, params, log, persistent);
            made.set(rite);
            return rite;
        }

        @Override
        public ResourceLocation id() {
            return ResourceLocation.fromNamespaceAndPath(NS, "coven_probe");
        }

        RiteType type(int power, int tickPower) {
            return new RiteType(List.of(), Map.of(), List.of(), power, tickPower, RiteWeatherRequirement.NONE, List.of(0, Level.TICKS_PER_DAY), List.of(), this);
        }
    }

    private static final class ProbeRite extends Rite {
        private final List<String> log;
        private final boolean persistent;

        ProbeRite(BaseRiteParams base, RiteParams params, List<String> log, boolean persistent) {
            super(base, params);
            this.log = log;
            this.persistent = persistent;
        }

        @Override
        protected boolean onStart(RiteParams params) {
            log.add("onStart");
            return persistent;
        }

        @Override
        protected boolean onTick(RiteParams params) {
            log.add("onTick");
            return persistent;
        }
    }

    private static boolean skip(GameTestHelper helper) {
        if (!ModList.get().isLoaded("enchanted")) {
            helper.succeed();
            return true;
        }
        helper.assertTrue(SpeciesMixinPlugin.riteCostHooked(), "RiteCostMixin did not land in GoldChalkBlockEntity");
        return false;
    }

    private static boolean flag(GoldChalkBlockEntity chalk, String name) {
        try {
            Field f = GoldChalkBlockEntity.class.getDeclaredField(name);
            f.setAccessible(true);
            return f.getBoolean(chalk);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Sets the chalk up as if a player had laid the offering and clicked, then runs its real tick. */
    private static void startThroughChalk(GameTestHelper helper, BlockPos pos, GoldChalkBlockEntity chalk, RiteType type) {
        ServerLevel level = helper.getLevel();
        set(chalk, "type", type);
        set(chalk, "itemsToConsume", new ArrayList<ItemStack>());
        set(chalk, "entitiesToConsume", new ArrayList<net.minecraft.world.entity.EntityType<?>>());
        set(chalk, "isInitialising", true);
        GoldChalkBlockEntity.serverTick(level, pos, level.getBlockState(pos), chalk);
    }

    /** Runs {@code body} once, on the first tick divisible by 20; a failure inside it is rethrown on every later tick, so it cannot pass by being retried. */
    private static void onAlignedTick(GameTestHelper helper, Runnable body) {
        boolean[] ran = {false};
        RuntimeException[] failure = {null};
        helper.succeedWhen(() -> {
            if (failure[0] != null) {
                throw failure[0];
            }
            if (!ran[0]) {
                helper.assertTrue(helper.getLevel().getGameTime() % 20 == 0, "waiting for a tick divisible by 20");
                ran[0] = true;
                try {
                    body.run();
                } catch (RuntimeException e) {
                    failure[0] = e;
                    throw e;
                }
            }
        });
    }

    private static List<String> traced(Runnable run) {
        List<String> seen = new ArrayList<>();
        Consumer<String> before = RiteCost.trace;
        RiteCost.trace = seen::add;
        try {
            run.run();
        } finally {
            RiteCost.trace = before;
        }
        return seen;
    }

    @GameTest(template = "empty", templateNamespace = NS, batch = BATCH, timeoutTicks = 100)
    public static void beginIsCalledOncePerStart(GameTestHelper helper) {
        if (skip(helper)) {
            return;
        }
        ServerLevel level = helper.getLevel();
        BlockPos rich = helper.absolutePos(new BlockPos(0, 200, 4));
        BlockPos poor = helper.absolutePos(new BlockPos(8, 200, 4));
        AltarBlockEntity richAltar = altar(helper, rich, 5000);
        AltarBlockEntity poorAltar = altar(helper, poor, 0);
        BlockPos chalkRich = rich.offset(0, 0, -2);
        BlockPos chalkPoor = poor.offset(0, 0, -2);
        GoldChalkBlockEntity paying = chalk(helper, chalkRich, UUID.randomUUID(), rich);
        GoldChalkBlockEntity broke = chalk(helper, chalkPoor, UUID.randomUUID(), poor);
        Probe probe = new Probe(new ArrayList<>(), new AtomicReference<>(), false);
        onAlignedTick(helper, () -> {
            set(richAltar, "power", 5000.0);
            set(poorAltar, "power", 0.0);
            List<String> started = traced(() -> startThroughChalk(helper, chalkRich, paying, probe.type(1000, 0)));
            helper.assertTrue(started.stream().filter(s -> s.startsWith("begin:")).count() == 1, "begin ran " + started);
            helper.assertTrue(started.contains("commit") && probe.made().get() != null, "the paying altar saw " + started);
            SupernaturalBloodlines.LOGGER.info("[covens task 0] a start through a paying altar: {}", started);
            probe.made().set(null);
            List<String> cancelled = traced(() -> startThroughChalk(helper, chalkPoor, broke, probe.type(1000, 0)));
            helper.assertTrue(probe.made().get() == null, "the rite was created although the altar could not pay");
            helper.assertTrue(cancelled.equals(List.of("begin:1000", "discard")), "an altar that cannot pay saw " + cancelled);
            SupernaturalBloodlines.LOGGER.info("[covens task 0] a start the altar cannot pay: {}", cancelled);
        });
    }

    @GameTest(template = "empty", templateNamespace = NS, batch = BATCH, timeoutTicks = 100)
    public static void commitRunsRightAfterStartAndBeforeTheFirstTick(GameTestHelper helper) {
        if (skip(helper)) {
            return;
        }
        ServerLevel level = helper.getLevel();
        BlockPos base = helper.absolutePos(new BlockPos(0, 200, 4));
        AltarBlockEntity altarEntity = altar(helper, base, 5000);
        BlockPos chalkPos = base.offset(0, 0, -2);
        GoldChalkBlockEntity chalk = chalk(helper, chalkPos, UUID.randomUUID(), base);
        List<String> log = new ArrayList<>();
        Probe probe = new Probe(log, new AtomicReference<>(), true);
        onAlignedTick(helper, () -> {
            set(altarEntity, "power", 5000.0);
            List<String> order = new ArrayList<>();
            Consumer<String> before = RiteCost.trace;
            RiteCost.trace = s -> {
                order.add(s);
                log.add(s);
            };
            try {
                startThroughChalk(helper, chalkPos, chalk, probe.type(1000, 10));
                RiteManager.tick(level);
            } finally {
                RiteCost.trace = before;
            }
            Rite rite = probe.made().get();
            try {
                helper.assertTrue(rite != null && RiteManager.getRiteAt(level, chalkPos) == rite, "the persistent rite is not running");
                List<String> mine = log.stream().filter(s -> !s.startsWith("tick-begin")).toList();
                helper.assertTrue(mine.stream().filter(s -> !s.equals("discard")).toList().equals(List.of("begin:1000", "onStart", "commit", "onTick")), "the order was " + log);
                SupernaturalBloodlines.LOGGER.info("[covens task 0] order for a persistent rite: {}", log);
            } finally {
                if (rite != null) {
                    rite.stop();
                    RiteManager.removeRite(level, rite);
                }
            }
        });
    }

    @GameTest(template = "empty", templateNamespace = NS, batch = BATCH, timeoutTicks = 200)
    public static void aRunningRiteCanBeStoppedFromOutside(GameTestHelper helper) {
        if (skip(helper)) {
            return;
        }
        ServerLevel level = helper.getLevel();
        BlockPos base = helper.absolutePos(new BlockPos(0, 200, 4));
        AltarBlockEntity altarPower = altar(helper, base, 20000);
        BlockPos chalkA = base.offset(0, 0, -2);
        BlockPos chalkB = base.offset(4, 0, -2);
        GoldChalkBlockEntity a = chalk(helper, chalkA, UUID.randomUUID(), base);
        GoldChalkBlockEntity b = chalk(helper, chalkB, UUID.randomUUID(), base);
        ServerPlayer mortal = TestPlayers.createMortal(helper);
        Queue<Rite> queue = new ConcurrentLinkedQueue<>();
        boolean[] live = {true};
        Consumer<ServerTickEvent.Post> drain = e -> {
            Rite r;
            while (live[0] && (r = queue.poll()) != null) {
                r.stop();
                RiteManager.removeRite(r.getLevel(), r);
            }
        };
        AtomicReference<Rite> fromDeath = new AtomicReference<>();
        Consumer<LivingDeathEvent> onDeath = e -> {
            if (live[0] && e.getEntity() == mortal && fromDeath.get() != null) {
                queue.add(fromDeath.get());
            }
        };
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, false, ServerTickEvent.Post.class, drain);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, false, LivingDeathEvent.class, onDeath);
        Probe probeA = new Probe(new ArrayList<>(), new AtomicReference<>(), true);
        Probe probeB = new Probe(new ArrayList<>(), new AtomicReference<>(), true);
        int[] stage = {0};
        helper.succeedWhen(() -> {
            {
                if (stage[0] == 0) {
                    helper.assertTrue(level.getGameTime() % 20 == 0, "waiting for a tick divisible by 20");
                    set(altarPower, "power", 20000.0);
                    startThroughChalk(helper, chalkA, a, probeA.type(1000, 0));
                    startThroughChalk(helper, chalkB, b, probeB.type(1000, 0));
                    helper.assertTrue(probeA.made().get() != null && probeB.made().get() != null, "a persistent rite did not start (altar power "
                            + WitchGameTests.power(altarPower) + ")");
                    helper.assertTrue(RiteManager.getRiteAt(level, chalkA) == probeA.made().get() && RiteManager.getRiteAt(level, chalkB) == probeB.made().get(),
                            "the two persistent rites are not running");
                    queue.add(probeA.made().get());
                    fromDeath.set(probeB.made().get());
                    stage[0] = 1;
                    helper.assertTrue(false, "queued the stop of the first rite; waiting for the end of the tick");
                }
                if (stage[0] == 1) {
                    helper.assertTrue(RiteManager.getRiteAt(level, chalkA) == null, "the first rite is still running after the queue was drained");
                    helper.assertTrue(!flag(a, "isExecuting"), "the first chalk is still attached to its rite");
                    helper.assertTrue(RiteManager.getRiteAt(level, chalkB) != null, "the second rite stopped without being asked (altar power " + WitchGameTests.power(altarPower) + ", first rite log " + probeA.log() + ", second " + probeB.log() + ")");
                    mortal.setHealth(1);
                    mortal.die(mortal.damageSources().genericKill());
                    stage[0] = 2;
                    helper.assertTrue(false, "the anchor died; waiting for the queued stop");
                }
                helper.assertTrue(RiteManager.getRiteAt(level, chalkB) == null, "the second rite is still running after a death event queued its stop");
                helper.assertTrue(!flag(b, "isExecuting"), "the second chalk is still attached to its rite");
                live[0] = false;
                level.removePlayerImmediately(mortal, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
                SupernaturalBloodlines.LOGGER.info("[covens task 0] a persistent rite was stopped from a queue drained in ServerTickEvent.Post, and from a LivingDeathEvent handler's queue");
            }
        });
    }

    @GameTest(template = "empty", templateNamespace = NS, batch = BATCH)
    public static void aThrowawayRiteCanBeCreatedFromItsType(GameTestHelper helper) {
        if (skip(helper)) {
            return;
        }
        RiteType type = RiteTestSupport.type(helper, "mastery_ultimate");
        if (type == null) {
            SupernaturalBloodlines.LOGGER.info("[covens task 0] mastery_ultimate is not loaded (the evolution rites part is off): nothing to create");
            helper.succeed();
            return;
        }
        BlockPos pos = RiteTestSupport.centre(helper);
        ServerLevel level = helper.getLevel();
        int before = RiteTestSupport.dropped(helper, "minecraft:nether_star");
        Rite rite = type.create(level, pos);
        helper.assertTrue(rite != null, "no rite came out of create(level, pos)");
        helper.assertTrue(RiteManager.getRiteAt(level, pos) == null, "the throwaway rite was added to the manager");
        helper.assertTrue(RiteTestSupport.dropped(helper, "minecraft:nether_star") == before, "creating a rite moved items");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, batch = BATCH, timeoutTicks = 100)
    public static void aZeroPowerRiteStillReachesBegin(GameTestHelper helper) {
        if (skip(helper)) {
            return;
        }
        ServerLevel level = helper.getLevel();
        BlockPos chalkPos = helper.absolutePos(new BlockPos(0, 200, 4));
        GoldChalkBlockEntity chalk = chalk(helper, chalkPos, UUID.randomUUID());
        Probe probe = new Probe(new ArrayList<>(), new AtomicReference<>(), false);
        onAlignedTick(helper, () -> {
            List<String> seen = traced(() -> startThroughChalk(helper, chalkPos, chalk, probe.type(0, 0)));
            helper.assertTrue(seen.contains("begin:0") && seen.contains("commit"), "a rite with power 0 saw " + seen);
            helper.assertTrue(probe.log().contains("onStart"), "the zero-power rite did not start: " + probe.log());
            SupernaturalBloodlines.LOGGER.info("[covens task 0] a rite with power 0: {}", seen);
        });
    }
}
