package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.EnabledParts;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.hud.action.ActionService;
import net.ofatech.supernaturalbloodlines.parts.hud.action.HudTargets;
import net.ofatech.supernaturalbloodlines.parts.hud.action.LoadoutService;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.original.Downed;
import net.ofatech.supernaturalbloodlines.parts.species.skill.ActionClock;
import net.ofatech.supernaturalbloodlines.parts.species.skill.HybridLeapAction;
import net.ofatech.supernaturalbloodlines.shared.hud.ExtraActions;

/**
 * Plan 18e: Hybrid Leap (R212). The server side of a slot action that throws the player at a target. A fake player is not moved by the server's physics, so the flight is
 * driven by hand: the test puts the leaper near the target and ticks the flight. The skill is in the Hybrid and Original hybrid trees, so these need the supernatural part.
 */
@PrefixGameTestTemplate(false)
public final class HybridLeapGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final ResourceLocation LEAP = ExtraActions.HYBRID_LEAP;

    private HybridLeapGameTests() {
    }

    private static boolean skipped(GameTestHelper helper) {
        if (!EnabledParts.isEnabled("supernatural")) {
            helper.succeed(); // no skills without the supernatural part
            return true;
        }
        return false;
    }

    private static TestPlayers.TalkingDuelist hybrid(GameTestHelper helper, Species species, String... skills) {
        TestPlayers.TalkingDuelist p = TestPlayers.createTalkingDuelist(helper);
        p.setPos(helper.absoluteVec(new Vec3(1.5, 1, 0.5)));
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(species).build());
        SupernaturalAccess.set(p, SupernaturalAccess.get(p).toBuilder().skills(Set.of(skills)).build());
        return p;
    }

    private static TestPlayers.TalkingDuelist hybrid(GameTestHelper helper) {
        return hybrid(helper, Species.HYBRID, "S-HYB-3");
    }

    private static Zombie zombieAt(GameTestHelper helper, double z) {
        Zombie zombie = EntityType.ZOMBIE.create(helper.getLevel());
        zombie.setPos(helper.absoluteVec(new Vec3(1.5, 1, z)));
        zombie.setNoAi(true);
        helper.getLevel().addFreshEntity(zombie);
        return zombie;
    }

    private static void look(ServerPlayer p, LivingEntity target) {
        p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
    }

    private static String key(Component c) {
        return c != null && c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t ? t.getKey() : String.valueOf(c);
    }

    private static void clean(ServerPlayer... players) {
        HybridLeapAction.reset();
        for (ServerPlayer p : players) {
            Downed.clearForTest(p);
            p.removeAllEffects();
            p.getAbilities().flying = false;
            p.setData(SpeciesRegistries.SNAP_GRACE_UNTIL, 0L);
            BloodlineAccess.set(p, BloodlineState.NONE);
            SupernaturalAccess.set(p, SupernaturalAccess.get(p).toBuilder().skills(Set.of()).build());
        }
    }

    /** The flight reaches the target: the leaper is put where the leap would bring them, and the flight is ticked. */
    private static void arriveAt(ServerPlayer p, LivingEntity target) {
        p.setPos(target.position().add(0.0, 0.0, -1.5));
        look(p, target);
        HybridLeapAction.tick(p);
    }

    // ---- the leap ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void leapMovesTowardsTheTargetAndHitsOnceWithWeakness(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        TestPlayers.TalkingDuelist p = hybrid(helper);
        Zombie z = zombieAt(helper, 6.5);
        try {
            look(p, z);
            helper.assertTrue(HybridLeapAction.INSTANCE.trigger(p, z) == null, "the leap was refused");
            Vec3 v = p.getDeltaMovement();
            helper.assertTrue(v.z > 0.3 && v.y >= 0.2 && Math.abs(v.x) < 1.0E-6, "the push does not point at the target: " + v);
            helper.assertTrue(HybridLeapAction.inFlight(p) && ActionClock.left(p, LEAP) > 0, "no flight or no cooldown");
            helper.assertTrue(z.getHealth() == 20.0f, "the blow landed before the flight reached the target");
            arriveAt(p, z);
            helper.assertTrue(z.getHealth() == 20.0f - (float) SpeciesConfig.hybridLeapDamage(), "the blow did " + (20.0f - z.getHealth()));
            var weak = z.getEffect(MobEffects.WEAKNESS);
            helper.assertTrue(weak != null && weak.getDuration() > 0, "no Weakness");
            float after = z.getHealth();
            z.invulnerableTime = 0;
            HybridLeapAction.tick(p);
            HybridLeapAction.tick(p);
            helper.assertTrue(z.getHealth() == after, "a second blow in the same flight: " + z.getHealth());
        } finally {
            z.discard();
            clean(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anOriginalHybridHasItAndAWerewolfFormIsNotNeeded(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        TestPlayers.TalkingDuelist p = hybrid(helper, Species.ORIGINAL_HYBRID, "S-HYB-3");
        Zombie z = zombieAt(helper, 5.5);
        try {
            helper.assertTrue(HybridLeapAction.INSTANCE.available(p), "an Original hybrid does not have the action");
            look(p, z);
            helper.assertTrue(HybridLeapAction.INSTANCE.trigger(p, z) == null, "refused without a werewolf form");
        } finally {
            z.discard();
            clean(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void leapNeedsTheSkillAndTheTreeThatListsIt(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        TestPlayers.TalkingDuelist without = hybrid(helper, Species.HYBRID);
        TestPlayers.TalkingDuelist tribrid = hybrid(helper, Species.TRIBRID, "S-HYB-3");
        TestPlayers.TalkingDuelist owner = hybrid(helper);
        try {
            helper.assertTrue(!HybridLeapAction.INSTANCE.available(without), "available without the skill");
            helper.assertTrue(!HybridLeapAction.INSTANCE.available(tribrid), "a tribrid's tree does not list it");
            helper.assertTrue(HybridLeapAction.INSTANCE.available(owner), "not available with the skill");
            helper.assertTrue(LoadoutService.set(without, 2, LEAP) != null, "a player without it could slot it");
            helper.assertTrue(LoadoutService.set(owner, 2, LEAP) == null, "the owner could not slot it");
            LoadoutService.offerDefaults(owner);
            helper.assertTrue(LoadoutService.get(owner).offered().contains(LEAP), "the hub's loadout did not offer it to its owner");
        } finally {
            clean(without, tribrid, owner);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void leapIsRefusedBeyondTheRangeWithoutACooldown(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        TestPlayers.TalkingDuelist p = hybrid(helper);
        Zombie far = zombieAt(helper, 0.5 + SpeciesConfig.hybridLeapRange() + 1.0);
        Zombie edge = zombieAt(helper, 0.5 + SpeciesConfig.hybridLeapRange() - 0.1);
        try {
            look(p, far);
            helper.assertTrue(HybridLeapAction.TOO_FAR.equals(key(HybridLeapAction.INSTANCE.trigger(p, far))), "a target past the range was not refused as too far");
            helper.assertTrue(ActionClock.left(p, LEAP) == 0 && !HybridLeapAction.inFlight(p), "a refusal started the cooldown or a flight");
            look(p, edge);
            helper.assertTrue(HybridLeapAction.INSTANCE.trigger(p, edge) == null, "a target inside the range was refused");
        } finally {
            far.discard();
            edge.discard();
            clean(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aTargetBehindTheLeaperIsNoTargetAndTheCooldownHolds(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        TestPlayers.TalkingDuelist p = hybrid(helper);
        Zombie z = zombieAt(helper, 6.5);
        try {
            p.setYRot(180.0f);
            helper.assertTrue(HybridLeapAction.NO_TARGET.equals(key(HybridLeapAction.INSTANCE.trigger(p, z))), "a target behind the leaper was taken");
            helper.assertTrue(HybridLeapAction.NO_TARGET.equals(key(HybridLeapAction.INSTANCE.trigger(p, null))), "no target gave another answer");
            look(p, z);
            helper.assertTrue(HybridLeapAction.INSTANCE.trigger(p, z) == null, "the leap was refused");
            helper.assertTrue(HybridLeapAction.CANNOT_ACT.equals(key(HybridLeapAction.INSTANCE.trigger(p, z))), "a second leap in the air was not refused");
            net.ofatech.supernaturalbloodlines.shared.hud.ExtraActions.stop(p);
            helper.assertTrue(!HybridLeapAction.inFlight(p), "the flight was not ended");
            helper.assertTrue(HybridLeapAction.COOLDOWN.equals(key(HybridLeapAction.INSTANCE.trigger(p, z))), "the cooldown did not hold");
        } finally {
            z.discard();
            clean(p);
        }
        helper.succeed();
    }

    // ---- who cannot leap ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aDownedOrSnappedOrFlyingPlayerCannotLeap(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        TestPlayers.TalkingDuelist p = hybrid(helper);
        Zombie z = zombieAt(helper, 6.5);
        try {
            look(p, z);
            Downed.startSnapped(p, 200);
            helper.assertTrue(HybridLeapAction.CANNOT_ACT.equals(key(HybridLeapAction.INSTANCE.trigger(p, z))), "a snapped player leapt");
            Downed.clearForTest(p);
            p.getAbilities().flying = true;
            helper.assertTrue(HybridLeapAction.CANNOT_ACT.equals(key(HybridLeapAction.INSTANCE.trigger(p, z))), "a flying player leapt");
            p.getAbilities().flying = false;
            helper.assertTrue(HybridLeapAction.INSTANCE.trigger(p, z) == null, "control: a player who can act was refused");
        } finally {
            z.discard();
            clean(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSlotPressForADownedPlayerIsRefusedByTheService(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        TestPlayers.TalkingDuelist p = hybrid(helper);
        Zombie z = zombieAt(helper, 6.5);
        try {
            helper.assertTrue(LoadoutService.set(p, 2, LEAP) == null, "could not slot the leap");
            look(p, z);
            Downed.startSnapped(p, 200);
            helper.assertTrue((ActionService.KEY + "cannot_act").equals(ActionService.trigger(p, 2, z.getId(), false)), "a snapped player pressed the slot");
            helper.assertTrue(!HybridLeapAction.inFlight(p), "a flight began");
        } finally {
            z.discard();
            clean(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSlotNamesATargetFartherThanTheBiteReach(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        TestPlayers.TalkingDuelist p = hybrid(helper);
        Zombie z = zombieAt(helper, 10.5);
        try {
            helper.assertTrue(HudTargets.resolve(p, z.getId()).isEmpty(), "setup: the bite reach already names a target 10 blocks away");
            helper.assertTrue(LoadoutService.set(p, 2, LEAP) == null, "could not slot the leap");
            look(p, z);
            helper.assertTrue(ActionService.trigger(p, 2, z.getId(), false) == null, "the slot press was refused: the leap does not reach past the bite reach");
            helper.assertTrue(HybridLeapAction.inFlight(p), "no flight began");
            helper.assertTrue(HudTargets.resolve(p, z.getId(), 5.0).isEmpty() && HudTargets.resolve(p, z.getId(), 12.0).isPresent(), "HudTargets does not honour the reach it is given");
        } finally {
            z.discard();
            clean(p);
        }
        helper.succeed();
    }

    // ---- the blow ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void noBlowLandsThroughAWallAndOneLandsOnceTheWayIsClear(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        TestPlayers.TalkingDuelist p = hybrid(helper);
        Zombie z = zombieAt(helper, 6.5);
        BlockPos wall = helper.absolutePos(new BlockPos(1, 1, 5));
        try {
            look(p, z);
            helper.assertTrue(HybridLeapAction.INSTANCE.trigger(p, z) == null, "the leap was refused");
            for (int dy = -1; dy < 4; dy++) {
                for (int dx = -2; dx <= 2; dx++) {
                    helper.getLevel().setBlockAndUpdate(wall.offset(dx, dy, 0), Blocks.STONE.defaultBlockState());
                }
            }
            p.setPos(helper.absoluteVec(new Vec3(1.5, 1, 4.0)));
            p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, z.getEyePosition());
            HybridLeapAction.tick(p);
            helper.assertTrue(z.getHealth() == 20.0f, "a blow landed through a wall");
            for (int dy = -1; dy < 4; dy++) {
                for (int dx = -2; dx <= 2; dx++) {
                    helper.getLevel().setBlockAndUpdate(wall.offset(dx, dy, 0), Blocks.AIR.defaultBlockState());
                }
            }
            HybridLeapAction.tick(p);
            helper.assertTrue(z.getHealth() < 20.0f, "the blow did not land once the way was clear");
        } finally {
            for (int dy = -1; dy < 4; dy++) {
                for (int dx = -2; dx <= 2; dx++) {
                    helper.getLevel().setBlockAndUpdate(wall.offset(dx, dy, 0), Blocks.AIR.defaultBlockState());
                }
            }
            z.discard();
            clean(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPlayerPutOutOfActionMidFlightDealsNoBlow(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        TestPlayers.TalkingDuelist p = hybrid(helper);
        Zombie z = zombieAt(helper, 6.5);
        try {
            look(p, z);
            helper.assertTrue(HybridLeapAction.INSTANCE.trigger(p, z) == null, "the leap was refused");
            Downed.startSnapped(p, 200);
            helper.assertTrue(!HybridLeapAction.inFlight(p), "the flight went on for a snapped player");
            arriveAt(p, z);
            helper.assertTrue(z.getHealth() == 20.0f, "a snapped player struck a blow");
        } finally {
            z.discard();
            clean(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aClaimOrPvpThatCancelsTheDamageRefusesTheLeapAndNoWeaknessFollowsALateCancel(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        TestPlayers.TalkingDuelist p = hybrid(helper);
        Zombie z = zombieAt(helper, 6.5);
        java.util.function.Consumer<LivingIncomingDamageEvent> claim = e -> e.setCanceled(true);
        try {
            look(p, z);
            NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, claim);
            helper.assertTrue(HybridLeapAction.REFUSED.equals(key(HybridLeapAction.INSTANCE.trigger(p, z))), "a protected target was not refused");
            helper.assertTrue(ActionClock.left(p, LEAP) == 0 && !HybridLeapAction.inFlight(p), "the refusal started a flight or the cooldown");
            NeoForge.EVENT_BUS.unregister(claim);
            // a claim that only closes after the leap is thrown: the blow is cancelled and no Weakness comes of it
            helper.assertTrue(HybridLeapAction.INSTANCE.trigger(p, z) == null, "the leap was refused");
            NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, claim);
            arriveAt(p, z);
            helper.assertTrue(z.getHealth() == 20.0f && !z.hasEffect(MobEffects.WEAKNESS), "a cancelled blow still weakened the target");
        } finally {
            NeoForge.EVENT_BUS.unregister(claim);
            z.discard();
            clean(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void everyRefusalIsTheSameWhateverTheTargetIs(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        ServerPlayer original = TestPlayers.createDuelist(helper);
        ServerPlayer creativeVampire = TestPlayers.createDuelist(helper);
        ServerPlayer snapped = TestPlayers.createDuelist(helper);
        Zombie weak = zombieAt(helper, 6.5);
        java.util.List<LivingEntity> targets = java.util.List.of(original, creativeVampire, snapped, weak);
        try {
            for (ServerPlayer t : new ServerPlayer[] {original, creativeVampire, snapped}) {
                t.setPos(helper.absoluteVec(new Vec3(1.5, 1, 6.5)));
            }
            BloodlineAccess.set(original, BloodlineState.NONE.toBuilder().species(Species.ORIGINAL).build());
            creativeVampire.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            Downed.startSnapped(snapped, 600);
            weak.setHealth(1.0f);
            java.util.function.Consumer<LivingIncomingDamageEvent> claim = e -> e.setCanceled(true);
            for (LivingEntity t : targets) {
                TestPlayers.TalkingDuelist p = hybrid(helper);
                look(p, t);
                String answer;
                if (t == original || t == weak) {
                    // control: these are fair targets, and a claim refuses them like anyone
                    NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, claim);
                    try {
                        answer = key(HybridLeapAction.INSTANCE.trigger(p, t));
                    } finally {
                        NeoForge.EVENT_BUS.unregister(claim);
                    }
                } else {
                    answer = key(HybridLeapAction.INSTANCE.trigger(p, t));
                }
                helper.assertTrue(HybridLeapAction.REFUSED.equals(answer), t + " got " + answer + " instead of the one refusal");
                helper.assertTrue(ActionClock.left(p, LEAP) == 0 && !HybridLeapAction.inFlight(p), t + ": a refusal had an effect");
                clean(p);
            }
        } finally {
            weak.discard();
            clean(original, creativeVampire, snapped);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anOriginalIsHurtLikeAnyoneAndTheGraceOfASnapDoesNotProtect(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        TestPlayers.TalkingDuelist p = hybrid(helper);
        ServerPlayer original = TestPlayers.createDuelist(helper);
        original.setPos(helper.absoluteVec(new Vec3(1.5, 1, 6.5)));
        BloodlineAccess.set(original, BloodlineState.NONE.toBuilder().species(Species.ORIGINAL).build());
        boolean pvp = helper.getLevel().getServer().isPvpAllowed();
        helper.getLevel().getServer().setPvpAllowed(true);
        try {
            original.setData(SpeciesRegistries.SNAP_GRACE_UNTIL, helper.getLevel().getGameTime() + 1000L);
            look(p, original);
            helper.assertTrue(HybridLeapAction.INSTANCE.trigger(p, original) == null, "an Original in the grace after a snap was refused");
            float before = original.getHealth();
            arriveAt(p, original);
            helper.assertTrue(original.getHealth() < before && original.isAlive(), "an Original was not hurt like anyone (or died of it): " + original.getHealth());
            helper.assertTrue(original.hasEffect(MobEffects.WEAKNESS), "no Weakness on an Original");
        } finally {
            helper.getLevel().getServer().setPvpAllowed(pvp);
            clean(p, original);
        }
        helper.succeed();
    }

    // ---- the landing ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theFallOfALeapIsEasedForAWhileButOnlyForTheFirstBlocks(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        TestPlayers.TalkingDuelist p = hybrid(helper);
        Zombie z = zombieAt(helper, 6.5);
        try {
            LivingFallEvent normal = new LivingFallEvent(p, 12.0f, 1.0f);
            NeoForge.EVENT_BUS.post(normal);
            helper.assertTrue(normal.getDistance() == 12.0f, "control: a fall with no leap was changed to " + normal.getDistance());
            look(p, z);
            helper.assertTrue(HybridLeapAction.INSTANCE.trigger(p, z) == null, "the leap was refused");
            LivingFallEvent inFlight = new LivingFallEvent(p, 12.0f, 1.0f);
            NeoForge.EVENT_BUS.post(inFlight);
            helper.assertTrue(inFlight.getDistance() == 4.0f, "a landing in flight: " + inFlight.getDistance());
            LivingFallEvent small = new LivingFallEvent(p, 5.0f, 1.0f);
            NeoForge.EVENT_BUS.post(small);
            helper.assertTrue(small.getDistance() == 0.0f, "a short fall after a leap hurt: " + small.getDistance());
            net.ofatech.supernaturalbloodlines.shared.hud.ExtraActions.stop(p);
            LivingFallEvent after = new LivingFallEvent(p, 100.0f, 1.0f);
            NeoForge.EVENT_BUS.post(after);
            helper.assertTrue(after.getDistance() == 92.0f, "a leap off a cliff does not make any fall safe: " + after.getDistance());
            HybridLeapAction.reset();
            LivingFallEvent later = new LivingFallEvent(p, 12.0f, 1.0f);
            NeoForge.EVENT_BUS.post(later);
            helper.assertTrue(later.getDistance() == 12.0f, "the ease outlived its time: " + later.getDistance());
        } finally {
            z.discard();
            clean(p);
        }
        helper.succeed();
    }
}
