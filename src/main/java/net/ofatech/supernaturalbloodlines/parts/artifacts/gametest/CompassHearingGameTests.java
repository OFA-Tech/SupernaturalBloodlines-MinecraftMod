package net.ofatech.supernaturalbloodlines.parts.artifacts.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.artifacts.ArtifactItems;

/** The Vampire Compass and the Hearing Device. Vampires are Vampirism's own (faction vampire), so no other part is needed. */
@PrefixGameTestTemplate(false)
public final class CompassHearingGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private CompassHearingGameTests() {
    }

    // ---- helpers ----

    @SuppressWarnings("unchecked")
    private static Mob vampireMob(GameTestHelper helper, BlockPos at) {
        return helper.spawnWithNoFreeWill((EntityType<Mob>) BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("vampirism:vampire")), at);
    }

    private static ServerPlayer vampirePlayer(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
        return p;
    }

    // a duelist: the hearing device only stuns players its user could hit
    private static ServerPlayer holder(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createDuelist(helper);
        p.setPos(helper.absoluteVec(new Vec3(1.5, 1, 1.5)));
        return p;
    }

    /** A FakePlayer is not in the level, so no area query finds it: add it like a connected player (and remove it when done). */
    private static void join(GameTestHelper helper, ServerPlayer p) {
        helper.getLevel().addNewPlayer(p);
    }

    private static void leave(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            helper.getLevel().removePlayerImmediately(p, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        }
    }

    private static Optional<GlobalPos> target(ItemStack compass) {
        LodestoneTracker t = compass.get(DataComponents.LODESTONE_TRACKER);
        return t == null ? null : t.target();
    }

    private static boolean hasEffect(LivingEntity e, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int amplifier) {
        MobEffectInstance i = e.getEffect(effect);
        return i != null && i.getAmplifier() == amplifier && i.getDuration() > 95 && i.getDuration() <= 100;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void compassAndDeviceRitesLoad(GameTestHelper helper) {
        var compass = net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.type(helper, "vampire_compass");
        var device = net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.type(helper, "hearing_device");
        helper.assertTrue(compass != null && compass.getPower() == 2000, "vampire_compass rite is missing or has the wrong power");
        helper.assertTrue(device != null && device.getPower() == 3000, "hearing_device rite is missing or has the wrong power");
        helper.succeed();
    }

    // ---- compass ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void compassPointsToNearestVampire(GameTestHelper helper) {
        ServerPlayer p = holder(helper);
        ItemStack compass = new ItemStack(ArtifactItems.VAMPIRE_COMPASS.get());
        Mob far = vampireMob(helper, new BlockPos(13, 1, 13));
        Mob near = vampireMob(helper, new BlockPos(6, 1, 1));
        helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 2));
        helper.spawn(EntityType.VILLAGER, new BlockPos(3, 1, 1));
        ServerPlayer human = holder(helper);
        human.setPos(helper.absoluteVec(new Vec3(2.5, 1, 1.5)));

        net.ofatech.supernaturalbloodlines.parts.artifacts.item.VampireCompassItem.update(compass, helper.getLevel(), p);
        Optional<GlobalPos> t = target(compass);
        helper.assertTrue(t != null && t.isPresent(), "the compass has no target: " + t);
        helper.assertTrue(t.get().pos().equals(near.blockPosition()), "points at " + t.get().pos() + ", the nearest vampire is at " + near.blockPosition());
        helper.assertTrue(t.get().dimension().equals(helper.getLevel().dimension()), "wrong dimension " + t.get().dimension());
        helper.assertTrue(!compass.get(DataComponents.LODESTONE_TRACKER).tracked(), "it would look for a lodestone");

        // the nearest dies: the next one is chosen
        near.discard();
        net.ofatech.supernaturalbloodlines.parts.artifacts.item.VampireCompassItem.update(compass, helper.getLevel(), p);
        helper.assertTrue(target(compass).isPresent() && target(compass).get().pos().equals(far.blockPosition()), "did not move on to the next vampire: " + target(compass));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void compassSpinsWithNoVampireInRange(GameTestHelper helper) {
        ServerPlayer p = holder(helper);
        ItemStack compass = new ItemStack(ArtifactItems.VAMPIRE_COMPASS.get());
        helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 2));
        net.ofatech.supernaturalbloodlines.parts.artifacts.item.VampireCompassItem.update(compass, helper.getLevel(), p);
        helper.assertTrue(target(compass) != null && target(compass).isEmpty(), "nothing in range must leave an empty tracker (spinning): " + target(compass));

        // a vampire is found, then the holder walks out of range: the tracker is cleared again
        Mob vamp = vampireMob(helper, new BlockPos(6, 1, 1));
        net.ofatech.supernaturalbloodlines.parts.artifacts.item.VampireCompassItem.update(compass, helper.getLevel(), p);
        helper.assertTrue(target(compass).isPresent(), "setup: the vampire was not found");
        p.setPos(helper.absoluteVec(new Vec3(1.5 + 70, 1, 1.5)));
        net.ofatech.supernaturalbloodlines.parts.artifacts.item.VampireCompassItem.update(compass, helper.getLevel(), p);
        helper.assertTrue(target(compass).isEmpty(), "a vampire 70 blocks away is in range: " + target(compass));

        // 60 blocks is in range
        p.setPos(vamp.position().add(60, 0, 0));
        net.ofatech.supernaturalbloodlines.parts.artifacts.item.VampireCompassItem.update(compass, helper.getLevel(), p);
        helper.assertTrue(target(compass).isPresent(), "a vampire 60 blocks away is out of range");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void compassIgnoresItsHolderAndUpdatesOncePerSecond(GameTestHelper helper) {
        ServerPlayer vamp = vampirePlayer(helper);
        vamp.setPos(helper.absoluteVec(new Vec3(1.5, 1, 1.5)));
        ItemStack compass = new ItemStack(ArtifactItems.VAMPIRE_COMPASS.get());
        net.ofatech.supernaturalbloodlines.parts.artifacts.item.VampireCompassItem.update(compass, helper.getLevel(), vamp);
        helper.assertTrue(target(compass) != null && target(compass).isEmpty(), "a vampire holder points at themself: " + target(compass));

        // inventoryTick: only on the second
        ServerPlayer p = holder(helper);
        vampireMob(helper, new BlockPos(6, 1, 1));
        ItemStack held = new ItemStack(ArtifactItems.VAMPIRE_COMPASS.get());
        p.tickCount = 7;
        held.inventoryTick(helper.getLevel(), p, 0, true);
        helper.assertTrue(held.get(DataComponents.LODESTONE_TRACKER) == null, "updated off the 20 tick beat");
        p.tickCount = 40;
        held.inventoryTick(helper.getLevel(), p, 0, true);
        helper.assertTrue(held.get(DataComponents.LODESTONE_TRACKER) != null && target(held).isPresent(), "not updated on the 20 tick beat");
        helper.succeed();
    }

    // ---- hearing device ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void hearingDeviceStunsSupernatural(GameTestHelper helper) {
        ServerPlayer user = holder(helper);
        Mob near = vampireMob(helper, new BlockPos(4, 1, 1));
        Mob far = vampireMob(helper, new BlockPos(12, 1, 1));
        ServerPlayer vampPlayer = vampirePlayer(helper);
        vampPlayer.setPos(helper.absoluteVec(new Vec3(1.5, 1, 4.5)));
        ServerPlayer farPlayer = vampirePlayer(helper);
        farPlayer.setPos(helper.absoluteVec(new Vec3(1.5, 1, 1.5 + 8)));
        ItemStack device = new ItemStack(ArtifactItems.HEARING_DEVICE.get());
        user.setItemInHand(InteractionHand.MAIN_HAND, device);
        join(helper, user);
        join(helper, vampPlayer);
        join(helper, farPlayer);
        try {
            device.use(helper.getLevel(), user, InteractionHand.MAIN_HAND);
            checkStunned(helper, user, near, vampPlayer, far, farPlayer);
        } finally {
            leave(helper, user, vampPlayer, farPlayer);
        }
        helper.succeed();
    }

    private static void checkStunned(GameTestHelper helper, ServerPlayer user, Mob near, ServerPlayer vampPlayer, Mob far, ServerPlayer farPlayer) {
        for (LivingEntity e : new LivingEntity[] {near, vampPlayer}) {
            helper.assertTrue(hasEffect(e, MobEffects.MOVEMENT_SLOWDOWN, 4), e.getName().getString() + " is not slowed V for 100 ticks: " + e.getActiveEffects());
            helper.assertTrue(hasEffect(e, MobEffects.CONFUSION, 0), e.getName().getString() + " has no nausea for 100 ticks: " + e.getActiveEffects());
        }
        helper.assertTrue(!far.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && !far.hasEffect(MobEffects.CONFUSION), "a vampire 8 blocks away was stunned: " + far.getActiveEffects());
        helper.assertTrue(!farPlayer.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && !farPlayer.hasEffect(MobEffects.CONFUSION), "a vampire player 8 blocks away was stunned: " + farPlayer.getActiveEffects());
        helper.assertTrue(user.getActiveEffects().isEmpty(), "the user was affected: " + user.getActiveEffects());
    }

    /** Pinned by the plan's review focus. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void hearingDeviceIgnoresHumans(GameTestHelper helper) {
        ServerPlayer user = holder(helper);
        ServerPlayer otherPlayer = holder(helper);
        otherPlayer.setPos(helper.absoluteVec(new Vec3(2.5, 1, 1.5)));
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 2));
        Villager villager = helper.spawn(EntityType.VILLAGER, new BlockPos(3, 1, 1));
        zombie.setNoAi(true);
        villager.setNoAi(true);
        ItemStack device = new ItemStack(ArtifactItems.HEARING_DEVICE.get());
        user.setItemInHand(InteractionHand.MAIN_HAND, device);
        join(helper, user);
        join(helper, otherPlayer);
        try {
            device.use(helper.getLevel(), user, InteractionHand.MAIN_HAND);
            for (LivingEntity e : new LivingEntity[] {user, otherPlayer, zombie, villager}) {
                helper.assertTrue(e.getActiveEffects().isEmpty(), e.getName().getString() + " was affected: " + e.getActiveEffects());
            }
        } finally {
            leave(helper, user, otherPlayer);
        }
        helper.succeed();
    }

    /** With PvP off a player target is spared (a creative or spectating one always); mobs are not. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void hearingDeviceRespectsPvp(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        boolean pvp = server.isPvpAllowed();
        ServerPlayer user = TestPlayers.createPvpAware(helper);
        user.setPos(helper.absoluteVec(new Vec3(1.5, 1, 1.5)));
        ServerPlayer target = vampirePlayer(helper);
        target.setPos(helper.absoluteVec(new Vec3(2.5, 1, 1.5)));
        ServerPlayer creative = vampirePlayer(helper);
        creative.setPos(helper.absoluteVec(new Vec3(1.5, 1, 3.5)));
        creative.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        ServerPlayer spectator = vampirePlayer(helper);
        spectator.setPos(helper.absoluteVec(new Vec3(3.5, 1, 1.5)));
        spectator.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
        Mob mob = vampireMob(helper, new BlockPos(1, 1, 4));
        ItemStack device = new ItemStack(ArtifactItems.HEARING_DEVICE.get());
        user.setItemInHand(InteractionHand.MAIN_HAND, device);
        for (ServerPlayer p : new ServerPlayer[] {user, target, creative, spectator}) {
            join(helper, p);
        }
        try {
            server.setPvpAllowed(false);
            device.use(helper.getLevel(), user, InteractionHand.MAIN_HAND);
            helper.assertTrue(target.getActiveEffects().isEmpty(), "a vampire player was stunned with PvP off: " + target.getActiveEffects());
            helper.assertTrue(creative.getActiveEffects().isEmpty() && spectator.getActiveEffects().isEmpty(), "a creative or spectating player was stunned");
            helper.assertTrue(mob.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "a vampire mob was spared with PvP off");

            user.getCooldowns().removeCooldown(device.getItem());
            server.setPvpAllowed(true);
            device.use(helper.getLevel(), user, InteractionHand.MAIN_HAND);
            helper.assertTrue(hasEffect(target, MobEffects.MOVEMENT_SLOWDOWN, 4), "control: a vampire player was spared with PvP on: " + target.getActiveEffects());
            helper.assertTrue(creative.getActiveEffects().isEmpty() && spectator.getActiveEffects().isEmpty(), "a creative or spectating player was stunned with PvP on");
        } finally {
            server.setPvpAllowed(pvp);
            leave(helper, user, target, creative, spectator);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void compassIgnoresCreativeAndSpectatorVampires(GameTestHelper helper) {
        ServerPlayer p = holder(helper);
        ServerPlayer creative = vampirePlayer(helper);
        creative.setPos(helper.absoluteVec(new Vec3(2.5, 1, 1.5)));
        creative.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        ServerPlayer spectator = vampirePlayer(helper);
        spectator.setPos(helper.absoluteVec(new Vec3(3.5, 1, 1.5)));
        spectator.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
        Mob far = vampireMob(helper, new BlockPos(13, 1, 13));
        ItemStack compass = new ItemStack(ArtifactItems.VAMPIRE_COMPASS.get());
        join(helper, creative);
        join(helper, spectator);
        try {
            net.ofatech.supernaturalbloodlines.parts.artifacts.item.VampireCompassItem.update(compass, helper.getLevel(), p);
            helper.assertTrue(target(compass).isPresent() && target(compass).get().pos().equals(far.blockPosition()),
                    "the compass points at a creative or spectating vampire: " + target(compass));
        } finally {
            leave(helper, creative, spectator);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void hearingDeviceHasCooldown(GameTestHelper helper) {
        ServerPlayer user = holder(helper);
        Mob vamp = vampireMob(helper, new BlockPos(4, 1, 1));
        ItemStack device = new ItemStack(ArtifactItems.HEARING_DEVICE.get());
        user.setItemInHand(InteractionHand.MAIN_HAND, device);
        helper.assertTrue(!user.getCooldowns().isOnCooldown(device.getItem()), "setup: already cooling down");

        device.use(helper.getLevel(), user, InteractionHand.MAIN_HAND);
        helper.assertTrue(user.getCooldowns().isOnCooldown(device.getItem()), "no cooldown after use");
        helper.assertTrue(vamp.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "setup: nobody was stunned");

        // used again while cooling down (a modded call can skip the vanilla check): nothing happens
        vamp.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        vamp.removeEffect(MobEffects.CONFUSION);
        device.use(helper.getLevel(), user, InteractionHand.MAIN_HAND);
        helper.assertTrue(!vamp.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the device worked during its cooldown");

        // exactly 1200 ticks
        for (int i = 0; i < 1199; i++) {
            user.getCooldowns().tick();
        }
        helper.assertTrue(user.getCooldowns().isOnCooldown(device.getItem()), "the cooldown is shorter than 1200 ticks");
        user.getCooldowns().tick();
        helper.assertTrue(!user.getCooldowns().isOnCooldown(device.getItem()), "the cooldown is longer than 1200 ticks");
        device.use(helper.getLevel(), user, InteractionHand.MAIN_HAND);
        helper.assertTrue(vamp.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the device did not work after its cooldown");
        helper.succeed();
    }
}
