package net.ofatech.supernaturalbloodlines.parts.supernatural.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import io.netty.buffer.Unpooled;
import java.lang.reflect.Field;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.supernatural.SupernaturalConfig;
import net.ofatech.supernaturalbloodlines.parts.supernatural.SupernaturalConfig.Tweak;
import net.ofatech.supernaturalbloodlines.parts.supernatural.tweaks.TweakSync;
import net.ofatech.supernaturalbloodlines.parts.supernatural.tweaks.VampirismTweaks;
import net.ofatech.supernaturalbloodlines.shared.admin.AdminEntries;

/** The four Vampirism tweaks (R213): each does nothing while off, and each does its one thing while on. */
@PrefixGameTestTemplate(false)
public final class TweaksGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private TweaksGameTests() {}

    private static ServerPlayer vampire(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createDuelist(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
        return p;
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

    /** Midday, clear sky, the player in the open: Vampirism itself says they are in the sun (checked before the tweak is asked). */
    private static void sunlit(GameTestHelper helper, ServerPlayer p) {
        helper.getLevel().setDayTime(6000);
        helper.getLevel().setWeatherParameters(6000, 0, false, false);
        Vec3 at = helper.absoluteVec(new Vec3(2.5, 1.0, 2.5));
        p.teleportTo(helper.getLevel(), at.x, at.y, at.z, java.util.Set.of(), 0f, 0f);
        p.clearFire();
        p.removeAllEffects();
    }

    private static boolean sunDamage(ServerPlayer p) {
        return VampirismAPI.vampirePlayer(p).isGettingSundamage(p.level(), true);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void sunlitVampireBurnsWhenOn(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        try {
            sunlit(helper, p);
            helper.assertTrue(sunDamage(p), "setup: Vampirism does not count this as sunlight");
            SupernaturalConfig.overrideTweak(Tweak.SUNLIGHT_IGNITES_VAMPIRES, true);
            helper.assertTrue(VampirismTweaks.igniteIfSunlit(p) && p.isOnFire(), "a sunlit vampire did not catch fire");
        } finally {
            clear();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void sunlightOffByDefaultSetsNothingOnFire(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        sunlit(helper, p);
        helper.assertTrue(sunDamage(p), "setup: Vampirism does not count this as sunlight");
        helper.assertTrue(!VampirismTweaks.igniteIfSunlit(p) && !p.isOnFire(), "the switch is off and the vampire burned");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void sunRingWearerDoesNotBurn(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        try {
            sunlit(helper, p);
            SupernaturalConfig.overrideTweak(Tweak.SUNLIGHT_IGNITES_VAMPIRES, true);
            p.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.getHolder(VampirismTweaks.SUNSCREEN).orElseThrow(), 200, 4, true, false));
            helper.assertTrue(sunDamage(p), "setup: not in the sun");
            helper.assertTrue(!VampirismTweaks.igniteIfSunlit(p) && !p.isOnFire(), "a Sun Ring wearer burned");
        } finally {
            clear();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void vampireInTheShadeDoesNotBurn(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        try {
            sunlit(helper, p);
            helper.getLevel().setDayTime(18000);
            SupernaturalConfig.overrideTweak(Tweak.SUNLIGHT_IGNITES_VAMPIRES, true);
            helper.assertTrue(!sunDamage(p), "setup: the sun is out at midnight");
            helper.assertTrue(!VampirismTweaks.igniteIfSunlit(p) && !p.isOnFire(), "a vampire at night burned");
        } finally {
            clear();
            helper.getLevel().setDayTime(6000);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void humanDoesNotBurn(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createDuelist(helper);
        try {
            sunlit(helper, p);
            SupernaturalConfig.overrideTweak(Tweak.SUNLIGHT_IGNITES_VAMPIRES, true);
            helper.assertTrue(!VampirismTweaks.igniteIfSunlit(p) && !p.isOnFire(), "a human burned");
        } finally {
            clear();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void creativeVampireDoesNotBurn(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        try {
            sunlit(helper, p);
            p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            SupernaturalConfig.overrideTweak(Tweak.SUNLIGHT_IGNITES_VAMPIRES, true);
            helper.assertTrue(!VampirismTweaks.igniteIfSunlit(p) && !p.isOnFire(), "a creative vampire burned");
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

    @GameTest(template = "empty", templateNamespace = NS)
    public static void zombieGetsTheGoalWhenOn(GameTestHelper helper) {
        ServerPlayer vampire = vampire(helper);
        ServerPlayer human = TestPlayers.createDuelist(helper);
        try {
            Mob before = spawn(helper, EntityType.ZOMBIE);
            helper.assertTrue(tweakGoals(before, Tweak.HOSTILE_MOBS_ATTACK_VAMPIRES) == 0, "a goal was added with the switch off");
            helper.assertTrue(!othersSelect(before, vampire), "setup: the zombie's own goals already take a vampire (Vampirism's zombieIgnoreVampire is off?)");
            before.discard();

            SupernaturalConfig.overrideTweak(Tweak.HOSTILE_MOBS_ATTACK_VAMPIRES, true);
            for (EntityType<? extends Mob> type : java.util.List.of(EntityType.ZOMBIE, EntityType.HUSK, EntityType.DROWNED, EntityType.ZOMBIE_VILLAGER,
                    EntityType.SKELETON, EntityType.STRAY)) {
                Mob mob = spawn(helper, type);
                VampirismTweaks.TweakGoal g = goal(mob, Tweak.HOSTILE_MOBS_ATTACK_VAMPIRES);
                helper.assertTrue(g != null && tweakGoals(mob, Tweak.HOSTILE_MOBS_ATTACK_VAMPIRES) == 1, type + ": no single added goal");
                helper.assertTrue(g.picks(vampire), type + ": the added goal does not pick a vampire");
                helper.assertTrue(!g.picks(human), type + ": the added goal picks a human (it is only for vampires)");
                helper.assertTrue(!(mob.targetSelector.getAvailableGoals().stream().anyMatch(w -> w.getGoal() instanceof VampirismTweaks.TweakGoal t
                        && t.tweak == Tweak.SKELETONS_ATTACK_WEREWOLVES)), type + ": got the werewolf goal with that switch off");
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
            for (EntityType<? extends Mob> type : java.util.List.of(EntityType.CREEPER, EntityType.SPIDER, EntityType.COW, EntityType.IRON_GOLEM)) {
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

    @GameTest(template = "empty", templateNamespace = NS)
    public static void offByDefaultAddsNothing(GameTestHelper helper) {
        for (EntityType<? extends Mob> type : java.util.List.of(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.STRAY, EntityType.HUSK)) {
            Mob mob = spawn(helper, type);
            helper.assertTrue(mob.targetSelector.getAvailableGoals().stream().noneMatch(w -> w.getGoal() instanceof VampirismTweaks.TweakGoal), type + " got a tweak goal by default");
            mob.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aMobAlreadyInTheLevelIsUntouched(GameTestHelper helper) {
        try {
            Mob already = spawn(helper, EntityType.ZOMBIE);
            SupernaturalConfig.overrideTweak(Tweak.HOSTILE_MOBS_ATTACK_VAMPIRES, true);
            helper.assertTrue(tweakGoals(already, Tweak.HOSTILE_MOBS_ATTACK_VAMPIRES) == 0, "a loaded mob changed when the switch flipped");
            Mob later = spawn(helper, EntityType.ZOMBIE);
            helper.assertTrue(tweakGoals(later, Tweak.HOSTILE_MOBS_ATTACK_VAMPIRES) == 1, "a mob that joined after the flip did not change");
            SupernaturalConfig.overrideTweak(Tweak.HOSTILE_MOBS_ATTACK_VAMPIRES, false);
            helper.assertTrue(tweakGoals(later, Tweak.HOSTILE_MOBS_ATTACK_VAMPIRES) == 1, "turning it off took a goal from a mob that has it (R213: it keeps it until it reloads)");
            Mob after = spawn(helper, EntityType.ZOMBIE);
            helper.assertTrue(tweakGoals(after, Tweak.HOSTILE_MOBS_ATTACK_VAMPIRES) == 0, "a mob that joined after turning it off got the goal");
            already.discard();
            later.discard();
            after.discard();
        } finally {
            clear();
        }
        helper.succeed();
    }
}
