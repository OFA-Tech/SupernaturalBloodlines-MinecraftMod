package net.ofatech.supernaturalbloodlines.parts.supernatural.gametest;

import com.mojang.authlib.GameProfile;
import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import io.netty.buffer.Unpooled;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.supernatural.SupernaturalConfig;
import net.ofatech.supernaturalbloodlines.parts.supernatural.SupernaturalConfig.Tweak;
import net.ofatech.supernaturalbloodlines.parts.supernatural.tweaks.TweakSync;
import net.ofatech.supernaturalbloodlines.parts.supernatural.tweaks.VampirismTweaks;
import net.ofatech.supernaturalbloodlines.shared.admin.AdminEntries;
import org.jetbrains.annotations.Nullable;

/** The four Vampirism tweaks (R213): each does nothing while off, and each does its one thing while on. */
@PrefixGameTestTemplate(false)
public final class TweaksGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final String VAMPIRISM_PLAYER = "de.teamlapen.vampirism.entity.player.vampire.VampirePlayer";

    private TweaksGameTests() {}

    private static ServerPlayer vampire(GameTestHelper helper, int level) {
        ServerPlayer p = TestPlayers.createDuelist(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, level), "could not make a vampire");
        return p;
    }

    private static ServerPlayer vampire(GameTestHelper helper) {
        return vampire(helper, 1);
    }

    private static void clear() {
        for (Tweak t : Tweak.values()) {
            SupernaturalConfig.overrideTweak(t, null);
        }
        SupernaturalConfig.clearPushed();
    }

    // ---- the four switches ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void everyTweakIsOffByDefaultAndHasAnAdminRow(GameTestHelper helper) {
        for (Tweak t : Tweak.values()) {
            helper.assertTrue(!SupernaturalConfig.localTweak(t), t.key + " is on by default");
            helper.assertTrue(AdminEntries.ALL.stream().anyMatch(e -> e.dottedPath().equals(t.key)), t.key + " has no Admin row");
        }
        helper.assertTrue(AdminEntries.ALL.stream().filter(e -> e.dottedPath().equals(Tweak.CLEANSING_ALTAR_DISABLED.key)).allMatch(e -> e.clientCopy()),
                "the altar row does not copy to clients");
        helper.succeed();
    }

    // ---- sunlight ----

    /** Midday, clear sky, the player in the open: Vampirism itself says they are in the sun. Returns what puts the day time and the weather back. */
    private static Runnable sunlit(GameTestHelper helper, ServerPlayer p, long dayTime) {
        ServerLevel level = helper.getLevel();
        ServerLevelData data = (ServerLevelData) level.getLevelData();
        long time = level.getDayTime();
        int clear = data.getClearWeatherTime();
        int rain = data.getRainTime();
        boolean raining = data.isRaining();
        boolean thundering = data.isThundering();
        level.setDayTime(dayTime);
        level.setWeatherParameters(6000, 0, false, false);
        Vec3 at = helper.absoluteVec(new Vec3(2.5, 1.0, 2.5));
        p.setPos(at); // not teleportTo: that would leave a vampire in the level's player list, where the next test's compass finds it
        p.clearFire();
        p.removeAllEffects();
        return () -> {
            level.setDayTime(time);
            level.setWeatherParameters(clear, rain, raining, thundering);
        };
    }

    private static boolean sunDamage(ServerPlayer p) {
        return VampirismAPI.vampirePlayer(p).isGettingSundamage(p.level(), true);
    }

    /**
     * Vampirism's own update, run until it would hurt: 100 ticks in the sun, then the tick (5 of 40) it hurts on. A fake player never ticks, so this is
     * the real code with the clock moved by hand. Returns the health lost.
     */
    private static float vampirismSunCycle(ServerPlayer p) {
        try {
            Class<?> type = Class.forName(VAMPIRISM_PLAYER);
            Object vp = type.getMethod("get", Player.class).invoke(null, p);
            Method update = type.getMethod("onUpdate");
            float before = p.getHealth();
            for (int i = 0; i < 100; i++) {
                p.tickCount = 0;
                update.invoke(vp);
            }
            p.tickCount = 5;
            update.invoke(vp);
            return before - p.getHealth();
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static int topLevel() {
        return VReference.VAMPIRE_FACTION.getHighestReachableLevel();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void sunDamageSetsAVampireOnFireWhenOn(GameTestHelper helper) {
        ServerPlayer p = vampire(helper, topLevel());
        Runnable back = sunlit(helper, p, 6000);
        try {
            SupernaturalConfig.overrideTweak(Tweak.SUNLIGHT_IGNITES_VAMPIRES, true);
            helper.assertTrue(sunDamage(p), "setup: Vampirism does not count this as sunlight");
            float lost = vampirismSunCycle(p);
            helper.assertTrue(lost > 0, "setup: Vampirism's own sun damage did not land");
            helper.assertTrue(p.isOnFire(), "Vampirism's sun damage landed and the vampire did not catch fire");
        } finally {
            clear();
            back.run();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void sunDamageWithTheSwitchOffSetsNothingOnFire(GameTestHelper helper) {
        ServerPlayer p = vampire(helper, topLevel());
        Runnable back = sunlit(helper, p, 6000);
        try {
            helper.assertTrue(sunDamage(p), "setup: Vampirism does not count this as sunlight");
            helper.assertTrue(vampirismSunCycle(p) > 0, "setup: Vampirism's own sun damage did not land");
            helper.assertTrue(!p.isOnFire(), "the switch is off and the vampire burned");
        } finally {
            back.run();
        }
        helper.succeed();
    }

    /** Vampirism spares the first levels (its level gate): no sun damage, so no fire either, whatever the switch says. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aLowLevelVampireTheSunDoesNotHurtDoesNotBurn(GameTestHelper helper) {
        ServerPlayer p = vampire(helper, 1);
        Runnable back = sunlit(helper, p, 6000);
        try {
            SupernaturalConfig.overrideTweak(Tweak.SUNLIGHT_IGNITES_VAMPIRES, true);
            helper.assertTrue(sunDamage(p), "setup: Vampirism does not count this as sunlight");
            helper.assertTrue(vampirismSunCycle(p) == 0, "setup: a level 1 vampire was hurt by the sun (Vampirism's level gate changed?)");
            helper.assertTrue(!p.isOnFire(), "a low level vampire the sun does not hurt burned");
        } finally {
            clear();
            back.run();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void sunRingWearerDoesNotBurn(GameTestHelper helper) {
        ServerPlayer p = vampire(helper, topLevel());
        Runnable back = sunlit(helper, p, 6000);
        try {
            SupernaturalConfig.overrideTweak(Tweak.SUNLIGHT_IGNITES_VAMPIRES, true);
            p.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.getHolder(VampirismTweaks.SUNSCREEN).orElseThrow(), 200, 4, true, false));
            helper.assertTrue(sunDamage(p), "setup: not in the sun");
            helper.assertTrue(vampirismSunCycle(p) == 0, "setup: a Sun Ring wearer was hurt by the sun");
            helper.assertTrue(!p.isOnFire(), "a Sun Ring wearer burned");
        } finally {
            clear();
            back.run();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void vampireInTheShadeDoesNotBurn(GameTestHelper helper) {
        ServerPlayer p = vampire(helper, topLevel());
        Runnable back = sunlit(helper, p, 18000);
        try {
            SupernaturalConfig.overrideTweak(Tweak.SUNLIGHT_IGNITES_VAMPIRES, true);
            helper.assertTrue(!sunDamage(p), "setup: the sun is out at midnight");
            helper.assertTrue(vampirismSunCycle(p) == 0 && !p.isOnFire(), "a vampire at night burned");
        } finally {
            clear();
            back.run();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void creativeVampireDoesNotBurn(GameTestHelper helper) {
        ServerPlayer p = vampire(helper, topLevel());
        Runnable back = sunlit(helper, p, 6000);
        try {
            p.setGameMode(GameType.CREATIVE);
            SupernaturalConfig.overrideTweak(Tweak.SUNLIGHT_IGNITES_VAMPIRES, true);
            vampirismSunCycle(p);
            helper.assertTrue(!p.isOnFire(), "a creative vampire burned");
        } finally {
            clear();
            p.setGameMode(GameType.SURVIVAL);
            back.run();
        }
        helper.succeed();
    }

    /** Only Vampirism's sun damage sets fire: any other hurt (and the fire itself) leaves a vampire as it is. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void otherDamageDoesNotSetAnyoneOnFire(GameTestHelper helper) {
        ServerPlayer p = vampire(helper, topLevel());
        try {
            SupernaturalConfig.overrideTweak(Tweak.SUNLIGHT_IGNITES_VAMPIRES, true);
            p.clearFire();
            p.hurt(helper.getLevel().damageSources().generic(), 1f);
            helper.assertTrue(p.getHealth() < p.getMaxHealth() && !p.isOnFire(), "ordinary damage set a vampire on fire");
        } finally {
            clear();
        }
        helper.succeed();
    }

    // ---- the cleansing altar ----

    private static PlayerInteractEvent.RightClickBlock click(GameTestHelper helper, ServerPlayer p, BlockPos rel) {
        BlockPos abs = helper.absolutePos(rel);
        PlayerInteractEvent.RightClickBlock e = new PlayerInteractEvent.RightClickBlock(p, InteractionHand.MAIN_HAND, abs,
                new BlockHitResult(Vec3.atCenterOf(abs), net.minecraft.core.Direction.UP, abs, false));
        NeoForge.EVENT_BUS.post(e);
        return e;
    }

    private static void place(GameTestHelper helper, BlockPos rel, BlockState state) {
        helper.getLevel().setBlockAndUpdate(helper.absolutePos(rel), state);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void altarClickIsCancelledWhenOn(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        BlockPos at = new BlockPos(1, 1, 1);
        try {
            place(helper, at, BuiltInRegistries.BLOCK.get(VampirismTweaks.ALTAR).defaultBlockState());
            helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(VampirismTweaks.ALTAR), "setup: Vampirism has no cleansing altar");
            SupernaturalConfig.overrideTweak(Tweak.CLEANSING_ALTAR_DISABLED, true);
            PlayerInteractEvent.RightClickBlock e = click(helper, p, at);
            helper.assertTrue(e.isCanceled() && e.getCancellationResult() == InteractionResult.FAIL, "the altar click was not refused");
        } finally {
            clear();
            place(helper, at, Blocks.AIR.defaultBlockState());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void altarWorksWhenOff(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        BlockPos at = new BlockPos(1, 1, 1);
        try {
            place(helper, at, BuiltInRegistries.BLOCK.get(VampirismTweaks.ALTAR).defaultBlockState());
            PlayerInteractEvent.RightClickBlock e = click(helper, p, at);
            helper.assertTrue(!e.isCanceled(), "the altar click was refused with the switch off");
        } finally {
            place(helper, at, Blocks.AIR.defaultBlockState());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anotherBlockIsNotAffected(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        BlockPos at = new BlockPos(1, 1, 1);
        try {
            SupernaturalConfig.overrideTweak(Tweak.CLEANSING_ALTAR_DISABLED, true);
            for (BlockState other : new BlockState[] {Blocks.STONE.defaultBlockState(), medChair()}) {
                place(helper, at, other);
                helper.assertTrue(!click(helper, p, at).isCanceled(), other + " was refused");
            }
        } finally {
            clear();
            place(helper, at, Blocks.AIR.defaultBlockState());
        }
        helper.succeed();
    }

    /** Vampirism's med chair, which sits next to the altar in its own set; the stone stands in when it is not there. */
    private static BlockState medChair() {
        return BuiltInRegistries.BLOCK.getOptional(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("vampirism", "med_chair"))
                .orElse(Blocks.STONE).defaultBlockState();
    }

    /**
     * Vampirism's own REVERT_BACK packet, handled by its own handler as the server would (the packet type and handler are not in the API jar, so by reflection).
     * Its handler kills a player who is not in hardcore when they leave, so the player of a test that leaves is dead after it.
     */
    private static void sendRevertBack(ServerPlayer p) {
        try {
            Class<?> handlerType = Class.forName("de.teamlapen.vampirism.server.ServerPayloadHandler");
            Class<?> packetType = Class.forName("de.teamlapen.vampirism.network.ServerboundSimpleInputEvent");
            Class<?> eventType = Class.forName("de.teamlapen.vampirism.network.ServerboundSimpleInputEvent$Event");
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object revert = Enum.valueOf((Class<Enum>) eventType, "REVERT_BACK");
            Object packet = packetType.getConstructor(eventType).newInstance(revert);
            IPayloadContext ctx = (IPayloadContext) Proxy.newProxyInstance(IPayloadContext.class.getClassLoader(), new Class<?>[] {IPayloadContext.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "player" -> p;
                        case "enqueueWork" -> {
                            if (args[0] instanceof Runnable r) {
                                r.run();
                            } else {
                                ((Supplier<?>) args[0]).get();
                            }
                            yield CompletableFuture.completedFuture(null);
                        }
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
            handlerType.getMethod("handleSimpleInputEvent", packetType, IPayloadContext.class).invoke(handlerType.getMethod("getInstance").invoke(null), packet, ctx);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static boolean isVampire(ServerPlayer p) {
        return VampirismAPI.factionPlayerHandler(p).getCurrentFaction() == VReference.VAMPIRE_FACTION;
    }

    /** Without our mod (or on the old protocol) the client shows the altar anyway and sends the packet: the server says no, and the player still dies. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void theAltarPacketIsRefusedByTheServerWhenOn(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        try {
            SupernaturalConfig.overrideTweak(Tweak.CLEANSING_ALTAR_DISABLED, true);
            sendRevertBack(p);
            helper.assertTrue(isVampire(p), "a client that never heard of the switch left the vampire faction through the altar");
        } finally {
            clear();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theAltarPacketWorksWhenOff(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        sendRevertBack(p);
        helper.assertTrue(!isVampire(p), "the switch is off and the packet did not make them leave");
        helper.succeed();
    }

    /** Vampirism's med chair sends the same packet with its menu open: that way out stays, as does everything that is not the packet. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void theMedChairAndOtherWaysOutStayWhenOn(GameTestHelper helper) {
        ServerPlayer chair = vampire(helper);
        ServerPlayer other = vampire(helper);
        try {
            SupernaturalConfig.overrideTweak(Tweak.CLEANSING_ALTAR_DISABLED, true);
            Class<?> menuType = Class.forName("de.teamlapen.vampirism.inventory.RevertBackMenu");
            chair.getInventory().setItem(chair.getInventory().selected, new ItemStack(Items.STICK));
            chair.containerMenu = (net.minecraft.world.inventory.AbstractContainerMenu) menuType.getConstructor(int.class, net.minecraft.world.entity.player.Inventory.class)
                    .newInstance(1, chair.getInventory());
            sendRevertBack(chair);
            helper.assertTrue(!isVampire(chair), "the med chair's revert was refused");

            helper.assertTrue(VampirismAPI.factionPlayerHandler(other).setFactionAndLevel(null, 0) && !isVampire(other),
                    "leaving the faction another way (a command, a rite) was refused");
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        } finally {
            clear();
            chair.containerMenu = chair.inventoryMenu;
        }
        helper.succeed();
    }

    // ---- the client copy ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void flagReachesTheClientCopy(GameTestHelper helper) {
        try {
            SupernaturalConfig.overrideTweak(Tweak.CLEANSING_ALTAR_DISABLED, true);
            TweakSync.Payload sent = TweakSync.Payload.ofLocal();
            helper.assertTrue(sent.altarDisabled(), "the server's own value was not sent");
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            TweakSync.Payload.STREAM_CODEC.encode(buf, sent);
            TweakSync.Payload got = TweakSync.Payload.STREAM_CODEC.decode(buf);
            helper.assertTrue(got.equals(sent) && buf.readableBytes() == 0, "the payload does not survive the wire");

            // the receiving side (here the same process, with its own file off): the server's value wins until it leaves
            SupernaturalConfig.overrideTweak(Tweak.CLEANSING_ALTAR_DISABLED, false);
            helper.assertTrue(!SupernaturalConfig.tweak(Tweak.CLEANSING_ALTAR_DISABLED), "setup: the client file is on");
            SupernaturalConfig.applyPushedAltar(got.altarDisabled());
            helper.assertTrue(SupernaturalConfig.tweak(Tweak.CLEANSING_ALTAR_DISABLED), "the pushed value was not used");
            helper.assertTrue(!TweakSync.Payload.ofLocal().altarDisabled(), "a client would send what it was pushed, not its own file");
            SupernaturalConfig.clearPushed();
            helper.assertTrue(!SupernaturalConfig.tweak(Tweak.CLEANSING_ALTAR_DISABLED), "leaving the world did not bring the local file back");
            // only the altar is ever pushed
            SupernaturalConfig.applyPushedAltar(true);
            helper.assertTrue(!SupernaturalConfig.tweak(Tweak.SUNLIGHT_IGNITES_VAMPIRES), "the push reached another tweak");
        } finally {
            clear();
        }
        helper.succeed();
    }

    /** A fake player with an open connection that has our channel, and the packets it was sent. */
    private static final class Wired extends FakePlayer {
        final List<Packet<?>> sent = new ArrayList<>();

        Wired(ServerLevel level) {
            super(level, new GameProfile(UUID.randomUUID(), "wired"));
            Connection open = new Connection(PacketFlow.CLIENTBOUND) {
                @Override
                public boolean isConnected() {
                    return true;
                }

                @Override
                public void setListenerForServerboundHandshake(net.minecraft.network.PacketListener listener) {}
            };
            // its constructor puts itself on this player
            new ServerGamePacketListenerImpl(level.getServer(), open, this, CommonListenerCookie.createInitial(getGameProfile(), false)) {
                @Override
                public boolean hasChannel(CustomPacketPayload.Type<?> type) {
                    return true;
                }

                @Override
                public void send(Packet<?> packet) {
                    sent.add(packet);
                }

                @Override
                public void send(Packet<?> packet, @Nullable PacketSendListener listener) {
                    sent.add(packet);
                }
            };
        }
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theSyncSkipsAFakePlayerAndReachesOneWithTheChannel(GameTestHelper helper) {
        ServerPlayer fake = TestPlayers.createDuelist(helper);
        helper.assertTrue(!TweakSync.send(fake), "a fake player (no open connection, no channel) was sent the tweak");
        Wired wired = new Wired(helper.getLevel());
        helper.assertTrue(TweakSync.send(wired) && !wired.sent.isEmpty(), "a player with the channel was not sent the tweak: " + wired.sent);
        helper.succeed();
    }

    // ---- mob targeting ----

    private static Mob spawn(GameTestHelper helper, EntityType<? extends Mob> type) {
        return (Mob) helper.spawn(type, 1, 1, 1);
    }

    private static int tweakGoals(Mob mob, Tweak t) {
        int n = 0;
        for (WrappedGoal g : mob.targetSelector.getAvailableGoals()) {
            if (g.getGoal() instanceof VampirismTweaks.TweakGoal tg && tg.tweak == t) {
                n++;
            }
        }
        return n;
    }

    private static VampirismTweaks.TweakGoal goal(Mob mob, Tweak t) {
        for (WrappedGoal g : mob.targetSelector.getAvailableGoals()) {
            if (g.getGoal() instanceof VampirismTweaks.TweakGoal tg && tg.tweak == t) {
                return tg;
            }
        }
        return null;
    }

    /** Whether any of the mob's own (not ours) goals aimed at players would take {@code target}, by the goal's own targeting conditions. */
    @SuppressWarnings("unchecked")
    private static boolean othersSelect(Mob mob, LivingEntity target) {
        try {
            Field type = NearestAttackableTargetGoal.class.getDeclaredField("targetType");
            Field conditions = NearestAttackableTargetGoal.class.getDeclaredField("targetConditions");
            type.setAccessible(true);
            conditions.setAccessible(true);
            for (WrappedGoal g : mob.targetSelector.getAvailableGoals()) {
                if (g.getGoal() instanceof NearestAttackableTargetGoal<?> n && !(n instanceof VampirismTweaks.TweakGoal) && type.get(n) == Player.class
                        && ((TargetingConditions) conditions.get(n)).test(mob, target)) {
                    return true;
                }
            }
            return false;
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /** The goal looks for a target one tick in ten, so it gets plenty of tries. */
    private static boolean everUses(VampirismTweaks.TweakGoal g) {
        for (int i = 0; i < 300; i++) {
            if (g.canUse()) {
                return true;
            }
        }
        return false;
    }

    /** The empty structure has a barrier ceiling at head height, which a mob's line of sight cannot cross: take it away. Returns what puts it back. */
    static Runnable openCeiling(GameTestHelper helper) {
        java.util.Map<BlockPos, BlockState> old = new java.util.HashMap<>();
        for (int x = 0; x <= 5; x++) {
            for (int y = 2; y <= 3; y++) {
                for (int z = 0; z <= 5; z++) {
                    BlockPos at = new BlockPos(x, y, z);
                    BlockState was = helper.getBlockState(at);
                    if (!was.isAir()) {
                        old.put(at, was);
                        place(helper, at, Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
        return () -> old.forEach((at, state) -> place(helper, at, state));
    }

    static String why(Mob mob, ServerPlayer p) {
        return "difficulty " + mob.level().getDifficulty() + ", canAttack " + mob.canAttack(p) + ", seenAsEnemy " + p.canBeSeenAsEnemy() + ", creative " + p.isCreative()
                + ", sight " + mob.getSensing().hasLineOfSight(p) + ", dist " + mob.distanceTo(p) + ", inList " + mob.level().players().contains(p)
                + ", nearest " + mob.level().getNearestPlayer(mob, 20) + ", mobAt " + mob.position() + " eye " + mob.getEyeY() + ", playerAt " + p.position() + " eye " + p.getEyeY()
                + ", clip " + mob.level().clip(new net.minecraft.world.level.ClipContext(new Vec3(mob.getX(), mob.getEyeY(), mob.getZ()), new Vec3(p.getX(), p.getEyeY(), p.getZ()),
                        net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, mob)).getType()
                + " at " + mob.level().clip(new net.minecraft.world.level.ClipContext(new Vec3(mob.getX(), mob.getEyeY(), mob.getZ()), new Vec3(p.getX(), p.getEyeY(), p.getZ()),
                        net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, mob)).getLocation()
                + " mobBlock " + mob.level().getBlockState(mob.blockPosition()) + " headBlock " + mob.level().getBlockState(BlockPos.containing(mob.getX(), mob.getEyeY(), mob.getZ()))
                + " lineEnd " + mob.level().getBlockState(BlockPos.containing(p.getX(), p.getEyeY(), p.getZ()));
    }

    private static final List<EntityType<? extends Mob>> ZOMBIES = List.of(EntityType.ZOMBIE, EntityType.HUSK, EntityType.DROWNED, EntityType.ZOMBIE_VILLAGER);
    private static final List<EntityType<? extends Mob>> SKELETONS = List.of(EntityType.SKELETON, EntityType.STRAY);

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theExactZombieAndSkeletonTypesGetTheGoalWhateverTheSwitch(GameTestHelper helper) {
        ServerPlayer vampire = vampire(helper);
        ServerPlayer human = TestPlayers.createDuelist(helper);
        try {
            Mob before = spawn(helper, EntityType.ZOMBIE);
            helper.assertTrue(!othersSelect(before, vampire), "setup: the zombie's own goals already take a vampire (Vampirism's zombieIgnoreVampire is off?)");
            before.discard();
            for (EntityType<? extends Mob> type : java.util.stream.Stream.concat(ZOMBIES.stream(), SKELETONS.stream()).toList()) {
                Mob mob = spawn(helper, type);
                VampirismTweaks.TweakGoal g = goal(mob, Tweak.HOSTILE_MOBS_ATTACK_VAMPIRES);
                helper.assertTrue(g != null && tweakGoals(mob, Tweak.HOSTILE_MOBS_ATTACK_VAMPIRES) == 1, type + ": no single goal (it is always added, the switch is asked at use)");
                helper.assertTrue(g.picks(vampire), type + ": the goal does not pick a vampire");
                helper.assertTrue(!g.picks(human), type + ": the goal picks a human (it is only for vampires)");
                helper.assertTrue(SKELETONS.contains(type) == (goal(mob, Tweak.SKELETONS_ATTACK_WEREWOLVES) != null), type + ": wrong werewolf goal");
                mob.discard();
            }
        } finally {
            clear();
        }
        helper.succeed();
    }

    /** A zombified piglin is a Zombie and a wither skeleton or a bogged an AbstractSkeleton, but they are other families: nothing is added to them. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void otherFamiliesAreNotTouched(GameTestHelper helper) {
        try {
            SupernaturalConfig.overrideTweak(Tweak.HOSTILE_MOBS_ATTACK_VAMPIRES, true);
            SupernaturalConfig.overrideTweak(Tweak.SKELETONS_ATTACK_WEREWOLVES, true);
            for (EntityType<? extends Mob> type : List.of(EntityType.ZOMBIFIED_PIGLIN, EntityType.WITHER_SKELETON, EntityType.BOGGED)) {
                Mob mob = spawn(helper, type);
                helper.assertTrue(mob.targetSelector.getAvailableGoals().stream().noneMatch(w -> w.getGoal() instanceof VampirismTweaks.TweakGoal), type + " got a tweak goal");
                mob.discard();
            }
        } finally {
            clear();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void creepersAndOthersAreNotTouched(GameTestHelper helper) {
        try {
            SupernaturalConfig.overrideTweak(Tweak.HOSTILE_MOBS_ATTACK_VAMPIRES, true);
            SupernaturalConfig.overrideTweak(Tweak.SKELETONS_ATTACK_WEREWOLVES, true);
            for (EntityType<? extends Mob> type : List.of(EntityType.CREEPER, EntityType.SPIDER, EntityType.COW, EntityType.IRON_GOLEM)) {
                Mob mob = spawn(helper, type);
                helper.assertTrue(mob.targetSelector.getAvailableGoals().stream().noneMatch(w -> w.getGoal() instanceof VampirismTweaks.TweakGoal), type + " got a tweak goal");
                mob.discard();
            }
            Mob zombie = spawn(helper, EntityType.ZOMBIE);
            helper.assertTrue(tweakGoals(zombie, Tweak.SKELETONS_ATTACK_WEREWOLVES) == 0, "a zombie got the skeleton's werewolf goal");
            zombie.discard();
        } finally {
            clear();
        }
        helper.succeed();
    }

    /** The switch is asked at every use, so flipping it works on mobs that are already loaded, in both directions. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void flippingTheSwitchWorksOnALoadedMobBothWays(GameTestHelper helper) {
        ServerPlayer vampire = vampire(helper);
        // a fake player is not in the level's list the goal searches; put it there only for this one call, a little away from where the mob spawns
        vampire.setPos(helper.absoluteVec(new Vec3(3.5, 1, 1.5)));
        helper.getLevel().players().add(vampire);
        Runnable ceiling = openCeiling(helper);
        try {
            for (EntityType<? extends Mob> type : List.of(EntityType.ZOMBIE, EntityType.SKELETON)) {
                Mob mob = spawn(helper, type);
                VampirismTweaks.TweakGoal g = goal(mob, Tweak.HOSTILE_MOBS_ATTACK_VAMPIRES);
                helper.assertTrue(!everUses(g), type + ": the goal was used with the switch off");
                SupernaturalConfig.overrideTweak(Tweak.HOSTILE_MOBS_ATTACK_VAMPIRES, true);
                helper.assertTrue(everUses(g), type + ": turning the switch on did not reach a mob that was already loaded: " + why(mob, vampire));
                SupernaturalConfig.overrideTweak(Tweak.HOSTILE_MOBS_ATTACK_VAMPIRES, false);
                helper.assertTrue(!everUses(g), type + ": turning the switch off did not stop a loaded mob");
                mob.discard();
            }
        } finally {
            ceiling.run();
            helper.getLevel().players().remove(vampire);
            clear();
        }
        helper.succeed();
    }
}
