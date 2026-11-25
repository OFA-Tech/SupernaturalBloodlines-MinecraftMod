package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.clean;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.human;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.join;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.online;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.vampire;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.player.vampire.IDrinkBloodContext;
import de.teamlapen.vampirism.api.event.BloodDrinkEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.blood.BloodKind;
import net.ofatech.supernaturalbloodlines.core.blood.BloodSource;
import net.ofatech.supernaturalbloodlines.core.group.GroupRank;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers.TalkingDuelist;
import net.ofatech.supernaturalbloodlines.parts.species.augustine.AugustineRules;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.group.Groups;
import net.ofatech.supernaturalbloodlines.parts.species.item.BloodBottles;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismSupernatural;
import net.ofatech.supernaturalbloodlines.shared.supernatural.CircleQuery;

/**
 * Plan 20c Task 0: the hooks the clans rest on, proved before anything is built on them. Every proof says what it saw in the log
 * ({@code [clans task 0]}). Nothing here loops on {@code tickCount}: time is pinned or stepped by a sequence.
 */
@PrefixGameTestTemplate(false)
public final class ClanHookGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private ClanHookGameTests() {
    }

    private static void say(String line) {
        SupernaturalBloodlines.LOGGER.info("[clans task 0] {}", line);
    }

    private static ItemEntity drop(GameTestHelper helper, double x, double y, double z, ItemStack stack) {
        Vec3 at = helper.absoluteVec(new Vec3(x, y, z));
        ItemEntity item = new ItemEntity(helper.getLevel(), at.x, at.y, at.z, stack);
        item.setDeltaMovement(Vec3.ZERO);
        item.setNoGravity(true);
        item.setNoPickUpDelay();
        helper.getLevel().addFreshEntity(item);
        return item;
    }

    private static int count(ServerPlayer p, net.minecraft.world.item.Item item) {
        return p.getInventory().countItem(item);
    }

    private static IDrinkBloodContext from(net.minecraft.world.entity.LivingEntity victim) {
        return new IDrinkBloodContext() {
            @Override
            public Optional<net.minecraft.world.entity.LivingEntity> getEntity() {
                return Optional.of(victim);
            }

            @Override
            public Optional<ItemStack> getStack() {
                return Optional.empty();
            }

            @Override
            public Optional<net.minecraft.world.level.block.state.BlockState> getBlockState() {
                return Optional.empty();
            }

            @Override
            public Optional<BlockPos> getBlockPos() {
                return Optional.empty();
            }
        };
    }

    // ---- the pickup ----

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aPickupEventCanDenyAPlayer(GameTestHelper helper) {
        ServerPlayer denied = TestPlayers.create(helper);
        ServerPlayer allowed = TestPlayers.create(helper);
        ItemEntity item = drop(helper, 1.5, 1, 1.5, new ItemStack(Items.GOLD_NUGGET));
        boolean[] live = {true};
        Consumer<ItemEntityPickupEvent.Pre> deny = e -> {
            if (live[0] && e.getPlayer() == denied) {
                e.setCanPickup(TriState.FALSE);
            }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, ItemEntityPickupEvent.Pre.class, deny);
        try {
            item.playerTouch(denied);
            helper.assertTrue(!item.isRemoved() && item.getItem().getCount() == 1, "the denied player took it: removed=" + item.isRemoved() + " count=" + item.getItem().getCount());
            helper.assertTrue(count(denied, Items.GOLD_NUGGET) == 0, "the denied player has it in the inventory");
            item.playerTouch(allowed);
            helper.assertTrue(item.isRemoved() && count(allowed, Items.GOLD_NUGGET) == 1, "the other player could not take it: removed=" + item.isRemoved() + " has=" + count(allowed, Items.GOLD_NUGGET));
            say("a Pre event with TriState.FALSE kept the stack in the world for one player and let the other take it");
        } finally {
            live[0] = false;
            NeoForge.EVENT_BUS.unregister(deny);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aPickedUpStackEmptiesTheEntity(GameTestHelper helper) {
        ServerPlayer taker = TestPlayers.create(helper);
        ItemEntity item = drop(helper, 1.5, 1, 1.5, new ItemStack(Items.GOLD_NUGGET));
        List<String> seen = new ArrayList<>();
        boolean[] live = {true};
        Consumer<EntityLeaveLevelEvent> probe = e -> {
            if (live[0] && e.getEntity() == item) {
                seen.add(item.getRemovalReason() + "/empty=" + item.getItem().isEmpty());
            }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, EntityLeaveLevelEvent.class, probe);
        try {
            item.playerTouch(taker);
            helper.assertTrue(item.isRemoved(), "the entity is still in the world after a pick-up");
            helper.assertTrue(seen.size() == 1 && seen.get(0).endsWith("empty=true"), "the leave event saw " + seen);
            say("a pick-up leaves with " + seen + "; after it the entity's stack reads " + item.getItem());
        } finally {
            live[0] = false;
            NeoForge.EVENT_BUS.unregister(probe);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aHopperStillTakesAnItemEntity(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos hopperPos = helper.absolutePos(new BlockPos(3, 2, 3));
        level.setBlock(hopperPos, Blocks.HOPPER.defaultBlockState(), 3);
        HopperBlockEntity hopper = (HopperBlockEntity) level.getBlockEntity(hopperPos);
        helper.assertTrue(hopper != null, "no hopper block entity");
        ItemEntity item = drop(helper, 3.5, 3.2, 3.5, new ItemStack(Items.GOLD_NUGGET));
        item.setPos(hopperPos.getX() + 0.5, hopperPos.getY() + 1.2, hopperPos.getZ() + 0.5);
        boolean[] pickups = {false};
        boolean[] live = {true};
        Consumer<ItemEntityPickupEvent.Pre> probe = e -> {
            if (live[0]) {
                pickups[0] = true;
            }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, ItemEntityPickupEvent.Pre.class, probe);
        try {
            HopperBlockEntity.pushItemsTick(level, hopperPos, level.getBlockState(hopperPos), hopper);
            helper.assertTrue(item.isRemoved(), "the hopper did not take the entity");
            helper.assertTrue(hopper.countItem(Items.GOLD_NUGGET) == 1, "the hopper does not hold the nugget");
            helper.assertTrue(!pickups[0], "a hopper fired a pickup event");
            say("a hopper emptied a dropped stack with no pickup event: the hole the plan answers with STORED");
        } finally {
            live[0] = false;
            NeoForge.EVENT_BUS.unregister(probe);
            level.setBlock(hopperPos, Blocks.AIR.defaultBlockState(), 3);
        }
        helper.succeed();
    }

    // ---- the entity ----

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void anInvulnerableUnlimitedItemSurvivesLavaFireAndAnExplosion(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ItemEntity guarded = drop(helper, 1.5, 1, 1.5, new ItemStack(Items.GOLD_NUGGET));
        ItemEntity control = drop(helper, 6.5, 1, 6.5, new ItemStack(Items.GOLD_NUGGET));
        guarded.setInvulnerable(true);
        guarded.setUnlimitedLifetime();
        for (ItemEntity e : List.of(guarded, control)) {
            e.hurt(level.damageSources().lava(), 4f);
            e.hurt(level.damageSources().inFire(), 1f);
        }
        helper.assertTrue(!guarded.isRemoved(), "lava or fire destroyed the protected item");
        Vec3 g = guarded.position();
        Vec3 c = control.position();
        level.explode(null, g.x, g.y, g.z, 4f, Level.ExplosionInteraction.NONE);
        helper.assertTrue(!guarded.isRemoved(), "an explosion destroyed the protected item");
        level.explode(null, c.x, c.y, c.z, 4f, Level.ExplosionInteraction.NONE);
        helper.assertTrue(control.isRemoved(), "setup: the explosion did not destroy an ordinary item, so the proof proves nothing");
        CompoundTag saved = new CompoundTag();
        guarded.save(saved);
        helper.assertTrue(saved.getShort("Age") == -32768, "the saved Age is " + saved.getShort("Age"));
        Entity loaded = EntityType.create(saved, level).orElseThrow();
        helper.assertTrue(loaded instanceof ItemEntity, "the saved entity did not load as an item");
        ItemEntity reloaded = (ItemEntity) loaded;
        reloaded.setPos(g.x, g.y, g.z);
        reloaded.setNoGravity(true);
        level.addFreshEntity(reloaded);
        reloaded.tick();
        reloaded.tick();
        helper.assertTrue(!reloaded.isRemoved() && reloaded.isInvulnerable(), "the reloaded item is gone or hurtable: removed=" + reloaded.isRemoved() + " invulnerable=" + reloaded.isInvulnerable());
        ItemEntity old = drop(helper, 4.5, 1, 1.5, new ItemStack(Items.GOLD_NUGGET));
        CompoundTag aged = new CompoundTag();
        old.save(aged);
        aged.putShort("Age", (short) 5999);
        ItemEntity ordinary = (ItemEntity) EntityType.create(aged, level).orElseThrow();
        ordinary.setNoGravity(true);
        level.addFreshEntity(ordinary);
        ordinary.tick();
        ordinary.tick();
        helper.assertTrue(ordinary.isRemoved(), "setup: an ordinary item at Age 5999 survived two ticks, so the lifetime step proves nothing");
        guarded.discard();
        reloaded.discard();
        old.discard();
        say("invulnerable + setUnlimitedLifetime survived lava, fire and an explosion that destroyed the control, and a reload with its saved Age (-32768) twice");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aVoidFallLeavesWithTheStackAndAPickupLeavesItEmpty(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer taker = TestPlayers.create(helper);
        ItemEntity voided = drop(helper, 1.5, 1, 1.5, new ItemStack(Items.GOLD_NUGGET));
        ItemEntity killed = drop(helper, 2.5, 1, 1.5, new ItemStack(Items.GOLD_NUGGET));
        ItemEntity picked = drop(helper, 3.5, 1, 1.5, new ItemStack(Items.GOLD_NUGGET));
        List<ItemEntity> mine = List.of(voided, killed, picked);
        java.util.Map<ItemEntity, String> seen = new java.util.IdentityHashMap<>();
        boolean[] live = {true};
        Consumer<EntityLeaveLevelEvent> probe = e -> {
            if (live[0] && e.getEntity() instanceof ItemEntity item && mine.contains(item)) {
                seen.put(item, item.getRemovalReason() + "/destroy=" + item.getRemovalReason().shouldDestroy() + "/empty=" + item.getItem().isEmpty());
            }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, EntityLeaveLevelEvent.class, probe);
        try {
            voided.setPos(voided.getX(), level.getMinBuildHeight() - 100, voided.getZ());
            voided.checkBelowWorld();
            killed.kill();
            picked.playerTouch(taker);
            helper.assertTrue("DISCARDED/destroy=true/empty=false".equals(seen.get(voided)), "a void fall left as " + seen.get(voided));
            helper.assertTrue("KILLED/destroy=true/empty=false".equals(seen.get(killed)), "a kill left as " + seen.get(killed));
            helper.assertTrue("DISCARDED/destroy=true/empty=true".equals(seen.get(picked)), "a pick-up left as " + seen.get(picked));
            helper.assertTrue(!Entity.RemovalReason.UNLOADED_TO_CHUNK.shouldDestroy() && !Entity.RemovalReason.UNLOADED_WITH_PLAYER.shouldDestroy(), "an unload destroys");
            say("leave events: void " + seen.get(voided) + ", kill " + seen.get(killed) + ", pick-up " + seen.get(picked) + "; unloads do not destroy");
        } finally {
            live[0] = false;
            NeoForge.EVENT_BUS.unregister(probe);
        }
        helper.succeed();
    }

    // ---- midnight ----

    /** Whether some t with {@code prev < t <= now} has {@code t mod 24000 == 18000}. */
    private static boolean crossed(long prev, long now) {
        return now > prev && Math.floorDiv(now - 18000, 24000) > Math.floorDiv(prev - 18000, 24000);
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 400)
    public static void midnightIsTheCrossingOf18000(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel().getServer().overworld();
        long savedTime = overworld.getDayTime();
        boolean savedCycle = overworld.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT);
        List<long[]> steps = new ArrayList<>();
        long[] last = {Long.MIN_VALUE};
        boolean[] live = {true};
        Consumer<ServerTickEvent.Post> probe = e -> {
            if (live[0]) {
                long now = overworld.getDayTime();
                if (last[0] != Long.MIN_VALUE) {
                    steps.add(new long[] {last[0], now});
                }
                last[0] = now;
            }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, ServerTickEvent.Post.class, probe);
        Runnable restore = () -> {
            live[0] = false;
            NeoForge.EVENT_BUS.unregister(probe);
            overworld.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(savedCycle, helper.getLevel().getServer());
            overworld.setDayTime(savedTime);
        };
        java.util.function.Function<Runnable, Runnable> guard = body -> () -> {
            try {
                body.run();
            } catch (RuntimeException failure) {
                restore.run();
                throw failure;
            }
        };
        long[] crossings = {0};
        helper.startSequence()
                .thenExecute(guard.apply(() -> {
                    overworld.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(true, helper.getLevel().getServer());
                    overworld.setDayTime(17990);
                    steps.clear();
                    last[0] = Long.MIN_VALUE;
                }))
                .thenIdle(30)
                .thenExecute(guard.apply(() -> {
                    long hits = steps.stream().filter(s -> crossed(s[0], s[1])).count();
                    helper.assertTrue(hits == 1, "an ordinary run through 18000 crossed " + hits + " times: " + steps.stream().map(s -> s[0] + ">" + s[1]).limit(40).toList());
                    crossings[0] = hits;
                    long[] landing = steps.stream().filter(s -> crossed(s[0], s[1])).findFirst().orElseThrow();
                    helper.assertTrue(landing[1] == 18000 || landing[1] > 18000 && landing[0] < 18000, "the crossing step was " + landing[0] + ">" + landing[1]);
                    overworld.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, helper.getLevel().getServer());
                    overworld.setDayTime(13000);
                    steps.clear();
                }))
                .thenIdle(2)
                .thenExecute(guard.apply(() -> {
                    overworld.setDayTime(24000);
                }))
                .thenIdle(2)
                .thenExecute(guard.apply(() -> {
                    helper.assertTrue(steps.stream().anyMatch(s -> s[0] == 13000 && s[1] == 24000 && crossed(s[0], s[1])), "a sleeping-style jump 13000 to 24000 was not a crossing: " + steps.stream().map(s -> s[0] + ">" + s[1]).toList());
                    steps.clear();
                    overworld.setDayTime(5000);
                }))
                .thenIdle(2)
                .thenExecute(guard.apply(() -> {
                    helper.assertTrue(steps.stream().anyMatch(s -> s[0] == 24000 && s[1] == 5000) && steps.stream().noneMatch(s -> crossed(s[0], s[1])),
                            "a step backwards was counted: " + steps.stream().map(s -> s[0] + ">" + s[1]).toList());
                    steps.clear();
                }))
                .thenIdle(40)
                .thenExecute(guard.apply(() -> {
                    helper.assertTrue(overworld.getDayTime() == 5000 && steps.stream().noneMatch(s -> crossed(s[0], s[1])), "with doDaylightCycle off the time moved to " + overworld.getDayTime());
                    say("midnight: one crossing on an ordinary run (" + crossings[0] + "), a 13000>24000 jump crosses, 24000>5000 does not, a stopped cycle never moves");
                    restore.run();
                }))
                .thenSucceed();
    }

    // ---- the feed ----

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aSharedFeedRefiresTheEventWithAnEmptySource(GameTestHelper helper) {
        TalkingDuelist feeder = vampire(helper, 1.5);
        TalkingDuelist mate = vampire(helper, 2.5);
        TalkingDuelist victim = human(helper, 3.5);
        List<String> events = new ArrayList<>();
        boolean[] live = {true};
        boolean[] nested = {false};
        Consumer<BloodDrinkEvent.PlayerDrinkBloodEvent> probe = e -> {
            if (!live[0]) {
                return;
            }
            Entity drinker = e.getVampire().getRepresentingEntity();
            events.add((drinker == feeder ? "feeder" : drinker == mate ? "mate" : "other") + "/source=" + e.getBloodSource().getEntity().isPresent());
            if (drinker == feeder && e.getBloodSource().getEntity().isPresent() && !nested[0]) {
                nested[0] = true;
                VampirismAPI.vampirePlayer(mate).drinkBlood(1, 0f, IDrinkBloodContext.none());
            }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, BloodDrinkEvent.PlayerDrinkBloodEvent.class, probe);
        try {
            VampirismAPI.vampirePlayer(mate).useBlood(20, true);
            VampirismAPI.vampirePlayer(feeder).drinkBlood(4, 0f, from(victim));
            helper.assertTrue(events.equals(List.of("feeder/source=true", "mate/source=false")),
                    "one bite and one share fire exactly one event each, the share's with an empty source: " + events);
            events.clear();
            nested[0] = true;
            VampirismAPI.vampirePlayer(mate).drinkBlood(1, 0f, IDrinkBloodContext.none());
            helper.assertTrue(events.equals(List.of("mate/source=false")), "a bare share fired " + events);
            say("a bite fires one event, a drinkBlood on a mate fires one more with an empty source: " + events);
        } finally {
            live[0] = false;
            NeoForge.EVENT_BUS.unregister(probe);
            clean(helper, feeder, mate, victim);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aHumansVampirePlayerObjectIsNotEmpty(GameTestHelper helper) {
        TalkingDuelist person = human(helper, 1.5);
        try {
            helper.assertTrue(VampirismAPI.vampirePlayer(person) != null, "vampirePlayer(human) is null");
            helper.assertTrue(VampirismAPI.getVampirePlayer(person).isPresent(), "getVampirePlayer(human) is empty");
            helper.assertTrue(!VampirismSupernatural.isVampire(person), "a human reads as a vampire");
            say("vampirePlayer(human) is present while isVampire(human) is false: the mate test must be VampirismSupernatural.isVampire");
        } finally {
            clean(helper, person);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void anAugustineMateGetsARescaledShare(GameTestHelper helper) {
        TalkingDuelist mate = vampire(helper, 1.5);
        BloodlineAccess.set(mate, BloodlineState.NONE.toBuilder().species(Species.AUGUSTINE).build());
        AugustineRules.forgetFeedsForTest();
        try {
            VampirismAPI.vampirePlayer(mate).useBlood(20, true);
            int before = VampirismAPI.vampirePlayer(mate).getBloodStats().getBloodLevel();
            VampirismAPI.vampirePlayer(mate).drinkBlood(10, 0f, IDrinkBloodContext.none());
            int gained = VampirismAPI.vampirePlayer(mate).getBloodStats().getBloodLevel() - before;
            boolean noted = mate.getPersistentData().getBoolean("supernaturalbloodlines:augustine_diet_noted");
            helper.assertTrue(gained != 10, "an Augustine gained the full 10 from an empty source: the finding is wrong");
            helper.assertTrue(noted, "the diet note was not set");
            say("an Augustine mate drank 10 from an empty source and gained " + gained + " (diet note set=" + noted + "): the SharedFeed guard is needed in AugustineRules");
        } finally {
            AugustineRules.forgetFeedsForTest();
            mate.getPersistentData().remove("supernaturalbloodlines:augustine_diet_noted");
            clean(helper, mate);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void forcedFeedAndGiveBloodFireNoDrinkEventAtAll(GameTestHelper helper) {
        TalkingDuelist giver = vampire(helper, 1.5);
        TalkingDuelist receiver = vampire(helper, 2.5);
        int[] events = {0};
        boolean[] live = {true};
        Consumer<BloodDrinkEvent.PlayerDrinkBloodEvent> probe = e -> {
            if (live[0]) {
                events[0]++;
            }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, BloodDrinkEvent.PlayerDrinkBloodEvent.class, probe);
        try {
            BloodSource source = new BloodSource(giver.getUUID(), "giver", false, BloodKind.of(BloodlineAccess.get(giver).species()));
            BloodBottles.applyWithoutConsent(receiver, source);
            BloodBottles.apply(receiver, source);
            helper.assertTrue(events[0] == 0, "forced and given blood fired " + events[0] + " drink events");
            say("BloodBottles.apply / applyWithoutConsent (what GiveBloodAction and a forced feed end in) fire no PlayerDrinkBloodEvent: they cannot start a share");
        } finally {
            live[0] = false;
            NeoForge.EVENT_BUS.unregister(probe);
            clean(helper, giver, receiver);
        }
        helper.succeed();
    }

    // ---- the ring ----

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void theCovenRiteSeesAClanLeaderAsADayWalkerSincePlan20c(GameTestHelper helper) {
        TalkingDuelist leader = vampire(helper, 1.5);
        TalkingDuelist member = vampire(helper, 2.5);
        online(leader, member);
        try {
            helper.assertTrue(join(leader, member) && Groups.typeOf(member) == GroupType.CLAN, "no clan formed");
            ServerLevel level = helper.getLevel();
            helper.assertTrue(CircleQuery.dayWalker(level, leader.getUUID()), "a clan leader does not count as a Day Walker for the ring (owner answer 1 of plan 20c; it was false on 20b, which is the finding this fixed)");
            helper.assertTrue(!CircleQuery.dayWalker(level, member.getUUID()), "a fresh member counts as a Day Walker");
            GroupIndex.get(level).setRank(member.getUUID(), GroupRank.DAY_WALKER);
            helper.assertTrue(CircleQuery.dayWalker(level, member.getUUID()), "a member with DAY_WALKER is not one");
            say("CircleQuery.dayWalker: leader true (changed by Task 9; it was false on main, the finding), new member false, DAY_WALKER member true");
        } finally {
            clean(helper, leader, member);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void withClansOffTheRingRuleIsOffToo(GameTestHelper helper) {
        try {
            net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig.override("sun_ring_day_walker_only", true);
            helper.assertTrue(CircleQuery.switchOn("sun_ring_day_walker_only"), "setup: the rule is on");
            net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig.override("clans_enabled", false);
            helper.assertTrue(!CircleQuery.switchOn("sun_ring_day_walker_only"), "with clans off a ring could be forged by nobody: the rule must be off with them");
            net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig.override("clans_enabled", null);
            helper.assertTrue(CircleQuery.switchOn("sun_ring_day_walker_only"), "the rule did not come back");
        } finally {
            net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig.override("clans_enabled", null);
            net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig.override("sun_ring_day_walker_only", null);
        }
        helper.succeed();
    }

    // ---- the config ----

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aSavedNonDefaultIsKept(GameTestHelper helper) {
        ModConfigSpec spec = net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig.SPEC;
        CommentedConfig file = new TomlParser().parse("sun_ring_day_walker_only = false\n");
        spec.correct(file);
        helper.assertTrue(spec.isCorrect(file) && Boolean.FALSE.equals(file.get("sun_ring_day_walker_only")), "a stored false became " + file.get("sun_ring_day_walker_only"));
        CommentedConfig empty = new TomlParser().parse("");
        spec.correct(empty);
        helper.assertTrue(Boolean.TRUE.equals(empty.get("sun_ring_day_walker_only")), "a file with no value got " + empty.get("sun_ring_day_walker_only"));
        say("ModConfigSpec.correct keeps a stored false under a default of true and fills a missing key with true: the flip reaches new files only");
        helper.succeed();
    }
}