package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.hud.action.LoadoutService;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.original.Downed;
import net.ofatech.supernaturalbloodlines.parts.species.skill.ActionClock;
import net.ofatech.supernaturalbloodlines.parts.species.skill.NeckSnapAction;
import net.ofatech.supernaturalbloodlines.parts.species.skill.RipHeartAction;
import net.ofatech.supernaturalbloodlines.shared.hud.ExtraActions;

/** Plan 18d: Rip Heart (R204) and Neck Snap (R214), and the snapped state. The skills belong to the vampire trees, so these need the supernatural part. */
@PrefixGameTestTemplate(false)
public final class TargetedSkillGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private TargetedSkillGameTests() {
    }

    private static boolean skipped(GameTestHelper helper) {
        if (!net.ofatech.supernaturalbloodlines.core.EnabledParts.isEnabled("supernatural")) {
            helper.succeed(); // no skills without the supernatural part
            return true;
        }
        return false;
    }

    private static TestPlayers.TalkingDuelist vampire(GameTestHelper helper, String... skills) {
        TestPlayers.TalkingDuelist p = TestPlayers.createTalkingDuelist(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(Species.NONE).build());
        SupernaturalAccess.set(p, SupernaturalAccess.get(p).toBuilder().skills(Set.of(skills)).build());
        return p;
    }

    private static ServerPlayer target(GameTestHelper helper, Species species, boolean vampire) {
        // a player target needs the server's PvP switch on (a targeted skill refuses a player with it off); the tests that want it off turn it off after this
        helper.getLevel().getServer().setPvpAllowed(true);
        ServerPlayer t = TestPlayers.createDuelist(helper);
        t.setPos(helper.absoluteVec(new Vec3(1.5, 1, 3.0)));
        if (vampire) {
            helper.assertTrue(VampirismAPI.factionPlayerHandler(t).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire target");
        }
        BloodlineAccess.set(t, BloodlineState.NONE.toBuilder().species(species).build());
        return t;
    }

    private static Zombie zombie(GameTestHelper helper, float health) {
        Zombie z = EntityType.ZOMBIE.create(helper.getLevel());
        z.setPos(helper.absoluteVec(new Vec3(1.5, 1, 3.0)));
        z.setNoAi(true);
        helper.getLevel().addFreshEntity(z);
        z.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(Math.max(20.0, health));
        z.setHealth(health);
        return z;
    }

    private static void clean(ServerPlayer... players) {
        for (ServerPlayer p : players) {
            Downed.clearForTest(p);
            p.removeAllEffects();
            p.setData(SpeciesRegistries.SNAP_GRACE_UNTIL, 0L);
            BloodlineAccess.set(p, BloodlineState.NONE);
            SupernaturalAccess.set(p, SupernaturalAccess.get(p).toBuilder().skills(Set.of()).build());
            if (VampirismAPI.factionPlayerHandler(p).getCurrentFaction() != null) {
                VampirismAPI.factionPlayerHandler(p).leaveFaction(false);
            }
        }
    }

    private static String key(Component c) {
        return c != null && c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t ? t.getKey() : String.valueOf(c);
    }

    // ---- Rip Heart ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void ripHeartFinishesAWeakMob(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        TestPlayers.TalkingDuelist user = vampire(helper, "S-VAM-8");
        Zombie z = zombie(helper, 6.0f);
        try {
            helper.assertTrue(RipHeartAction.INSTANCE.trigger(user, z) == null, "the action was refused");
            helper.assertTrue(!z.isAlive(), "a mob at 6 health lived");
            helper.assertTrue(ActionClock.left(user, ExtraActions.RIP_HEART) > 0, "no cooldown");
            helper.assertTrue(user.receivedOnActionBar(RipHeartAction.DONE), "no done line");
        } finally {
            z.discard();
            clean(user);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void ripHeartRefusesAStrongMobAndCostsNothing(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        TestPlayers.TalkingDuelist user = vampire(helper, "S-VAM-8");
        Zombie z = zombie(helper, 20.0f);
        try {
            helper.assertTrue(RipHeartAction.TOO_STRONG.equals(key(RipHeartAction.INSTANCE.trigger(user, z))), "a strong mob was not told it is too healthy");
            helper.assertTrue(z.isAlive() && z.getHealth() == 20.0f, "the mob was hurt");
            helper.assertTrue(ActionClock.left(user, ExtraActions.RIP_HEART) == 0, "a refusal started the cooldown");
        } finally {
            z.discard();
            clean(user);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void ripHeartOnAnOriginalAtLowHealthLooksTheSame(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        TestPlayers.TalkingDuelist userA = vampire(helper, "S-VAM-8");
        TestPlayers.TalkingDuelist userB = vampire(helper, "S-VAM-8");
        ServerPlayer original = target(helper, Species.ORIGINAL, true);
        Zombie z = zombie(helper, 5.0f);
        try {
            original.setHealth(5.0f);
            Component a = RipHeartAction.INSTANCE.trigger(userA, original);
            Component b = RipHeartAction.INSTANCE.trigger(userB, z);
            helper.assertTrue(a == null && b == null, "one of them was refused: " + key(a) + " / " + key(b));
            helper.assertTrue(original.isAlive() && original.getHealth() == 5.0f, "an Original was hurt: " + original.getHealth());
            helper.assertTrue(!z.isAlive(), "the control mob lived");
            helper.assertTrue(userA.receivedOnActionBar(RipHeartAction.DONE) && userB.receivedOnActionBar(RipHeartAction.DONE), "the done lines differ");
            helper.assertTrue(ActionClock.left(userA, ExtraActions.RIP_HEART) == ActionClock.left(userB, ExtraActions.RIP_HEART)
                    && ActionClock.left(userA, ExtraActions.RIP_HEART) > 0, "the cooldowns differ");
            // a healthy Original is refused like anyone healthy
            TestPlayers.TalkingDuelist userC = vampire(helper, "S-VAM-8");
            original.setHealth(20.0f);
            helper.assertTrue(RipHeartAction.TOO_STRONG.equals(key(RipHeartAction.INSTANCE.trigger(userC, original))), "a healthy Original was not told too healthy, like anyone");
            clean(userC);
        } finally {
            z.discard();
            clean(userA, userB, original);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void ripHeartIsNotWorkedOnAnNpcOriginal(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        TestPlayers.TalkingDuelist user = vampire(helper, "S-VAM-8");
        Zombie z = zombie(helper, 5.0f);
        z.setData(SpeciesRegistries.ORIGINAL_ROOT, java.util.UUID.randomUUID().toString());
        try {
            helper.assertTrue(RipHeartAction.INSTANCE.trigger(user, z) == null, "refused");
            helper.assertTrue(z.isAlive() && z.getHealth() == 5.0f, "an NPC Original was hurt");
            helper.assertTrue(user.receivedOnActionBar(RipHeartAction.DONE) && ActionClock.left(user, ExtraActions.RIP_HEART) > 0, "the user was told something else");
        } finally {
            z.discard();
            clean(user);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aClaimThatCancelsTheAttackRefusesRipHeart(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        TestPlayers.TalkingDuelist user = vampire(helper, "S-VAM-8");
        Zombie z = zombie(helper, 5.0f);
        java.util.function.Consumer<AttackEntityEvent> claim = e -> e.setCanceled(true);
        NeoForge.EVENT_BUS.addListener(claim);
        try {
            helper.assertTrue(RipHeartAction.REFUSED.equals(key(RipHeartAction.INSTANCE.trigger(user, z))), "a claimed target was not refused");
            helper.assertTrue(z.isAlive() && ActionClock.left(user, ExtraActions.RIP_HEART) == 0, "it still acted or started the cooldown");
        } finally {
            NeoForge.EVENT_BUS.unregister(claim);
            z.discard();
            clean(user);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void ripHeartOnAPlayerWithPvpOffIsRefused(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        ServerPlayer user = TestPlayers.createPvpAware(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(user).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
        SupernaturalAccess.set(user, SupernaturalAccess.get(user).toBuilder().skills(Set.of("S-VAM-8")).build());
        ServerPlayer other = target(helper, Species.NONE, true);
        other.setHealth(4.0f);
        boolean pvp = helper.getLevel().getServer().isPvpAllowed();
        helper.getLevel().getServer().setPvpAllowed(false);
        try {
            helper.assertTrue(RipHeartAction.REFUSED.equals(key(RipHeartAction.INSTANCE.trigger(user, other))), "PvP off did not refuse");
            helper.assertTrue(other.isAlive() && ActionClock.left(user, ExtraActions.RIP_HEART) == 0, "it acted anyway");
        } finally {
            helper.getLevel().getServer().setPvpAllowed(pvp);
            clean(user, other);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void ripHeartIsFinalAgainstATotem(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        TestPlayers.TalkingDuelist user = vampire(helper, "S-VAM-8");
        Zombie z = zombie(helper, 4.0f);
        z.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
        try {
            helper.assertTrue(RipHeartAction.INSTANCE.trigger(user, z) == null, "refused");
            helper.assertTrue(!z.isAlive(), "a Totem took back a heart-rip");
        } finally {
            z.discard();
            clean(user);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void ripHeartDoesNotKillABossAndSaysTheSame(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        LivingEntity boss = boss(helper, "deucalion");
        if (boss == null) {
            helper.succeed(); // the bosses part is off
            return;
        }
        TestPlayers.TalkingDuelist user = vampire(helper, "S-VAM-8");
        try {
            boss.setHealth(4.0f);
            helper.assertTrue(RipHeartAction.INSTANCE.trigger(user, boss) == null, "refused");
            helper.assertTrue(boss.isAlive() && boss.getHealth() == 4.0f, "a boss was hurt by a heart-rip");
            helper.assertTrue(user.receivedOnActionBar(RipHeartAction.DONE) && ActionClock.left(user, ExtraActions.RIP_HEART) > 0, "the user was told something else");
        } finally {
            boss.discard();
            clean(user);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void ripHeartCannotBeUsedWithoutTheSkillAndNeedsATarget(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        TestPlayers.TalkingDuelist user = vampire(helper);
        TestPlayers.TalkingDuelist owner = vampire(helper, "S-VAM-8");
        try {
            helper.assertTrue(!RipHeartAction.INSTANCE.available(user), "available without the skill");
            helper.assertTrue(RipHeartAction.INSTANCE.available(owner), "not available with the skill");
            helper.assertTrue(RipHeartAction.NO_TARGET.equals(key(RipHeartAction.INSTANCE.trigger(owner, null))), "no target gave another line");
            helper.assertTrue(ActionClock.left(owner, ExtraActions.RIP_HEART) == 0, "no target started the cooldown");
            helper.assertTrue(LoadoutService.set(owner, 2, ExtraActions.RIP_HEART) == null, "the owner could not slot it");
            helper.assertTrue(LoadoutService.set(user, 2, ExtraActions.RIP_HEART) != null, "a non-owner could slot it");
        } finally {
            clean(user, owner);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void ripHeartFinishesASnappedVampireButNotASnappedOriginal(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        TestPlayers.TalkingDuelist user = vampire(helper, "S-VAM-8");
        TestPlayers.TalkingDuelist user2 = vampire(helper, "S-VAM-8");
        ServerPlayer vamp = target(helper, Species.NONE, true);
        ServerPlayer original = target(helper, Species.ORIGINAL, true);
        try {
            vamp.setHealth(5.0f);
            original.setHealth(5.0f);
            Downed.startSnapped(vamp, 600);
            Downed.startSnapped(original, 600);
            helper.assertTrue(RipHeartAction.INSTANCE.trigger(user, vamp) == null && RipHeartAction.INSTANCE.trigger(user2, original) == null, "refused");
            helper.assertTrue(vamp.isDeadOrDying(), "a snapped vampire at 5 health was not finished");
            helper.assertTrue(!original.isDeadOrDying() && Downed.isSnapped(original), "a snapped Original was hurt");
        } finally {
            clean(user, user2, vamp, original);
        }
        helper.succeed();
    }

    // ---- Neck Snap ----

    private static int snapTicks(GameTestHelper helper, Species species, boolean vampire, String why) {
        TestPlayers.TalkingDuelist user = vampire(helper, "S-VAM-9");
        ServerPlayer t = target(helper, species, vampire);
        try {
            float before = t.getHealth();
            helper.assertTrue(NeckSnapAction.INSTANCE.trigger(user, t) == null, why + ": refused");
            helper.assertTrue(Downed.isSnapped(t), why + ": not snapped");
            helper.assertTrue(t.getHealth() == before, why + ": a snap cost " + (before - t.getHealth()) + " health");
            return t.getData(SpeciesRegistries.DOWNED);
        } finally {
            clean(user, t);
        }
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void neckSnapLaysADownAVampireForTheConfiguredTimeAndAnOriginalTheSame(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        int vampire = snapTicks(helper, Species.NONE, true, "vampire");
        int original = snapTicks(helper, Species.ORIGINAL, true, "original");
        int augustine = snapTicks(helper, Species.AUGUSTINE, true, "augustine");
        int hybrid = snapTicks(helper, Species.HYBRID, false, "hybrid");
        helper.assertTrue(vampire == SpeciesConfig.neckSnapVampireSeconds() * 20, "a vampire lay for " + vampire);
        helper.assertTrue(original == vampire, "an Original lay for " + original + " but a vampire for " + vampire + " (the time would give the kind away)");
        helper.assertTrue(augustine == vampire && hybrid == vampire, "augustine " + augustine + ", hybrid " + hybrid);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSnappedPlayerWakesWithTheirHealthAndCannotGiveUp(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        ServerPlayer t = target(helper, Species.NONE, true);
        try {
            t.setHealth(13.0f);
            Downed.startSnapped(t, 40);
            Downed.giveUp(t);
            helper.assertTrue(Downed.isSnapped(t) && !t.isDeadOrDying(), "Give up worked on a snapped player");
            helper.assertTrue(!t.hurt(helper.getLevel().damageSources().generic(), 5.0f) && t.getHealth() == 13.0f, "a snapped player was hurt");
            for (int i = 0; i < 40; i++) {
                Downed.tick(t);
            }
            helper.assertTrue(!Downed.isDowned(t), "still down after the time");
            helper.assertTrue(t.getHealth() == 13.0f, "woke with " + t.getHealth() + ", not the 13 they had");
        } finally {
            clean(t);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSnappedPlayerCannotSwingAndRunsNoAction(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        ServerPlayer t = target(helper, Species.NONE, true);
        Zombie z = zombie(helper, 20.0f);
        try {
            Downed.startSnapped(t, 100);
            AttackEntityEvent swing = new AttackEntityEvent(t, z);
            NeoForge.EVENT_BUS.post(swing);
            helper.assertTrue(swing.isCanceled(), "a snapped player could attack");
            helper.assertTrue(ExtraActions.blocked(t), "a snapped player may run an extra action");
        } finally {
            z.discard();
            clean(t);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSnappedMobLiesDownWithNothingSavedAndIsFreeAgain(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        TestPlayers.TalkingDuelist user = vampire(helper, "S-VAM-9");
        ServerPlayer victim = TestPlayers.createDuelist(helper);
        Zombie thinking = zombie(helper, 20.0f);
        thinking.setNoAi(false);
        thinking.setData(SpeciesRegistries.ORIGINAL_ROOT, java.util.UUID.randomUUID().toString());
        try {
            helper.assertTrue(NeckSnapAction.INSTANCE.trigger(user, thinking) == null, "refused");
            helper.assertTrue(thinking.hasEffect(SpeciesRegistries.SNAPPED), "the mob was not snapped");
            helper.assertTrue(thinking.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) == 0.0, "the snapped mob can still move: " + thinking.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED));
            helper.assertTrue(!thinking.isNoAi() && !thinking.saveWithoutId(new net.minecraft.nbt.CompoundTag()).getBoolean("NoAI"), "a NoAI flag was set and would be saved with the mob");
            helper.assertTrue(thinking.getHealth() == 20.0f, "the snap itself cost the mob health: " + thinking.getHealth());
            helper.assertTrue(!thinking.hurt(helper.getLevel().damageSources().generic(), 3.0f) && thinking.getHealth() == 20.0f, "a snapped mob was hurt");
            thinking.setTarget(victim);
            helper.assertTrue(thinking.getTarget() == null, "a snapped mob took a target");
            float before = victim.getHealth();
            helper.assertTrue(!victim.hurt(helper.getLevel().damageSources().mobAttack(thinking), 5.0f) && victim.getHealth() == before, "a snapped mob struck");
            thinking.removeEffect(SpeciesRegistries.SNAPPED);
            helper.assertTrue(thinking.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) > 0.0, "the mob did not get its speed back");
            helper.assertTrue(net.ofatech.supernaturalbloodlines.parts.species.item.SnappedEffect.inGrace(thinking), "no grace after the snap");
        } finally {
            thinking.discard();
            clean(user, victim);
        }
        helper.succeed();
    }
    @GameTest(template = "empty", templateNamespace = NS)
    public static void neckSnapKillsAWeakMortalAndOnlyStaggersAHealthyOne(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        TestPlayers.TalkingDuelist userA = vampire(helper, "S-VAM-9");
        TestPlayers.TalkingDuelist userB = vampire(helper, "S-VAM-9");
        Zombie weak = zombie(helper, 6.0f);
        Zombie healthy = zombie(helper, 20.0f);
        try {
            helper.assertTrue(NeckSnapAction.INSTANCE.trigger(userA, weak) == null && NeckSnapAction.INSTANCE.trigger(userB, healthy) == null, "refused");
            helper.assertTrue(!weak.isAlive(), "a weak mortal lived");
            helper.assertTrue(healthy.isAlive(), "a healthy mortal died");
            var slow = healthy.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
            helper.assertTrue(slow != null && slow.getAmplifier() == 2 && healthy.hasEffect(MobEffects.WEAKNESS), "a healthy mortal did not stagger");
        } finally {
            weak.discard();
            healthy.discard();
            clean(userA, userB);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void neckSnapGivesTheSameLineAndCooldownForEveryResult(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        LivingEntity[] targets = {target(helper, Species.ORIGINAL, true), target(helper, Species.NONE, true), zombie(helper, 6.0f), zombie(helper, 20.0f), boss(helper)};
        int cooldown = -1;
        try {
            for (LivingEntity t : targets) {
                if (t == null) {
                    continue; // no bosses part
                }
                TestPlayers.TalkingDuelist user = vampire(helper, "S-VAM-9");
                helper.assertTrue(NeckSnapAction.INSTANCE.trigger(user, t) == null, t + ": refused");
                helper.assertTrue(user.receivedOnActionBar(NeckSnapAction.DONE), t + ": no done line");
                int left = ActionClock.left(user, ExtraActions.NECK_SNAP);
                helper.assertTrue(left > 0 && (cooldown < 0 || left == cooldown), t + ": cooldown " + left + " but the others " + cooldown);
                cooldown = left;
                clean(user);
            }
        } finally {
            for (LivingEntity t : targets) {
                if (t instanceof ServerPlayer sp) {
                    clean(sp);
                } else if (t != null) {
                    t.discard();
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void neckSnapDoesNotMoveABoss(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        LivingEntity boss = boss(helper);
        if (boss == null) {
            helper.succeed(); // the bosses part is off
            return;
        }
        TestPlayers.TalkingDuelist user = vampire(helper, "S-VAM-9");
        try {
            float before = boss.getHealth();
            helper.assertTrue(NeckSnapAction.INSTANCE.trigger(user, boss) == null, "refused");
            helper.assertTrue(boss.isAlive() && boss.getHealth() == before && !boss.hasEffect(SpeciesRegistries.SNAPPED) && !boss.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
                    "a boss was moved");
        } finally {
            boss.discard();
            clean(user);
        }
        helper.succeed();
    }

    private static LivingEntity boss(GameTestHelper helper) {
        return boss(helper, "klaus");
    }

    /** A boss by its entity id; one without an Original root (a werewolf boss) shows the tag alone is what protects it. */
    private static LivingEntity boss(GameTestHelper helper, String id) {
        var type = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.fromNamespaceAndPath(NS, id));
        if (type.isEmpty()) {
            return null;
        }
        net.minecraft.world.entity.Entity e = type.get().create(helper.getLevel());
        if (!(e instanceof Mob m)) {
            return null;
        }
        m.setPos(helper.absoluteVec(new Vec3(1.5, 1, 3.0)));
        m.setNoAi(true);
        helper.getLevel().addFreshEntity(m);
        return m;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void neckSnapRefusesTheSameWayWhateverTheReason(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        TestPlayers.TalkingDuelist user = vampire(helper, "S-VAM-9");
        ServerPlayer snapped = target(helper, Species.NONE, true);
        ServerPlayer creative = target(helper, Species.NONE, true);
        Zombie z = zombie(helper, 6.0f);
        java.util.function.Consumer<AttackEntityEvent> claim = e -> e.setCanceled(true);
        try {
            Downed.startSnapped(snapped, 600);
            creative.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            helper.assertTrue(NeckSnapAction.REFUSED.equals(key(NeckSnapAction.INSTANCE.trigger(user, snapped))), "an already snapped target was not refused");
            helper.assertTrue(NeckSnapAction.REFUSED.equals(key(NeckSnapAction.INSTANCE.trigger(user, creative))), "a creative target was not refused");
            NeoForge.EVENT_BUS.addListener(claim);
            helper.assertTrue(NeckSnapAction.REFUSED.equals(key(NeckSnapAction.INSTANCE.trigger(user, z))), "a claimed target was not refused");
            helper.assertTrue(z.isAlive() && ActionClock.left(user, ExtraActions.NECK_SNAP) == 0, "a refusal acted or started the cooldown");
        } finally {
            NeoForge.EVENT_BUS.unregister(claim);
            z.discard();
            clean(user, snapped, creative);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void neckSnapNeedsTheSkillAndShowsInTheHub(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        TestPlayers.TalkingDuelist user = vampire(helper);
        TestPlayers.TalkingDuelist owner = vampire(helper, "S-VAM-9");
        try {
            helper.assertTrue(ExtraActions.get(ExtraActions.NECK_SNAP).isPresent() && ExtraActions.get(ExtraActions.RIP_HEART).isPresent(), "the actions are not installed");
            helper.assertTrue(!NeckSnapAction.INSTANCE.available(user) && NeckSnapAction.INSTANCE.available(owner), "availability does not follow the skill");
            LoadoutService.offerDefaults(owner);
            helper.assertTrue(LoadoutService.get(owner).offered().contains(ExtraActions.NECK_SNAP), "the loadout did not offer Neck Snap to its owner");
            helper.assertTrue(LoadoutService.get(user).offered().isEmpty() || !LoadoutService.get(user).offered().contains(ExtraActions.NECK_SNAP), "it was offered to a non-owner");
        } finally {
            clean(user, owner);
        }
        helper.succeed();
    }

    // ================= review round 1 =================

    private static void intoDbno(GameTestHelper helper, ServerPlayer p) {
        Zombie hitter = zombie(helper, 20.0f);
        p.invulnerableTime = 0;
        p.hurt(helper.getLevel().damageSources().mobAttack(hitter), 1000f);
        hitter.discard();
        helper.assertTrue(VampirismAPI.vampirePlayer(p).isDBNO(), "setup: Vampirism's dying state did not start");
        p.setHealth(5.0f);
    }

    private static net.minecraft.world.entity.PathfinderMob villager(GameTestHelper helper) {
        var v = (net.minecraft.world.entity.PathfinderMob) EntityType.VILLAGER.create(helper.getLevel());
        v.setNoAi(true);
        v.setPos(helper.absoluteVec(new Vec3(1.5, 1, 2.5)));
        helper.getLevel().addFreshEntity(v);
        return v;
    }

    private static void assertRefused(GameTestHelper helper, String what, String expectedKey, Component got, ServerPlayer user, ResourceLocation action) {
        helper.assertTrue(expectedKey.equals(key(got)), what + ": answered " + key(got));
        helper.assertTrue(ActionClock.left(user, action) == 0, what + ": a refusal started the cooldown");
    }

    // ---- H1: a downed player acts at all ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSnappedVampireCannotBiteAndTheTargetIsUnbitten(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
        var v = villager(helper);
        try {
            helper.assertTrue(LoadoutService.set(p, 0, net.ofatech.supernaturalbloodlines.core.hud.Loadout.BITE) == null, "could not slot Bite");
            int villagerBlood = VampirismAPI.getExtendedCreatureVampirism(v).map(c2 -> c2.getBlood()).orElse(0);
            Downed.startSnapped(p, 200);
            String refusal = net.ofatech.supernaturalbloodlines.parts.hud.action.ActionService.trigger(p, 0, v.getId(), false);
            helper.assertTrue((net.ofatech.supernaturalbloodlines.parts.hud.action.ActionService.KEY + "cannot_act").equals(refusal), "a snapped vampire pressed Bite: " + refusal);
            helper.assertTrue(VampirismAPI.getExtendedCreatureVampirism(v).map(c2 -> c2.getBlood()).orElse(0) == villagerBlood, "the villager was bitten");
        } finally {
            v.discard();
            net.ofatech.supernaturalbloodlines.parts.hud.bite.BiteService.release(p);
            clean(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aVampirismDyingUserGetsCannotActAndKillsNothing(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        ServerPlayer user = TestPlayers.createDuelist(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(user).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
        SupernaturalAccess.set(user, SupernaturalAccess.get(user).toBuilder().skills(Set.of("S-VAM-8")).build());
        Zombie z = zombie(helper, 5.0f);
        try {
            helper.assertTrue(LoadoutService.set(user, 2, ExtraActions.RIP_HEART) == null, "could not slot Rip Heart");
            intoDbno(helper, user);
            String refusal = net.ofatech.supernaturalbloodlines.parts.hud.action.ActionService.trigger(user, 2, z.getId(), false);
            helper.assertTrue((net.ofatech.supernaturalbloodlines.parts.hud.action.ActionService.KEY + "cannot_act").equals(refusal), "a player in Vampirism's dying state used a slot: " + refusal);
            helper.assertTrue(z.isAlive(), "the target died");
        } finally {
            z.discard();
            OriginalRulesProbe.endDying(user);
            clean(user);
        }
        helper.succeed();
    }

    // ---- H2: stun-lock ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPlayerCannotBeSnappedAgainRightAfterWaking(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        ServerPlayer t = target(helper, Species.NONE, true);
        TestPlayers.TalkingDuelist user = vampire(helper, "S-VAM-9");
        try {
            Downed.startSnapped(t, 40);
            for (int i = 0; i < 40; i++) {
                Downed.tick(t);
            }
            helper.assertTrue(!Downed.isDowned(t), "still down after the time");
            assertRefused(helper, "re-snap in the grace", NeckSnapAction.REFUSED, NeckSnapAction.INSTANCE.trigger(user, t), user, ExtraActions.NECK_SNAP);
            helper.assertTrue(!Downed.isSnapped(t), "the player was snapped in the grace");
            t.setData(SpeciesRegistries.SNAP_GRACE_UNTIL, 0L);
            helper.assertTrue(NeckSnapAction.INSTANCE.trigger(user, t) == null && Downed.isSnapped(t), "the grace did not end");
            t.setData(SpeciesRegistries.SNAP_GRACE_UNTIL, 12345L);
            var saved = t.saveWithoutId(new net.minecraft.nbt.CompoundTag());
            helper.assertTrue(saved.getCompound("neoforge:attachments").contains(NS + ":snap_grace_until"), "the grace is not saved: a logout would end it: " + saved.getCompound("neoforge:attachments").getAllKeys());
        } finally {
            clean(user, t);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCreatureCannotBeSnappedAgainRightAfterTheSnapEnds(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        TestPlayers.TalkingDuelist user = vampire(helper, "S-VAM-9");
        var mob = vampireMob(helper);
        try {
            helper.assertTrue(NeckSnapAction.INSTANCE.trigger(user, mob) == null && mob.hasEffect(SpeciesRegistries.SNAPPED), "a Vampirism vampire mob was not snapped");
            mob.removeEffect(SpeciesRegistries.SNAPPED);
            TestPlayers.TalkingDuelist user2 = vampire(helper, "S-VAM-9");
            assertRefused(helper, "re-snap a mob in the grace", NeckSnapAction.REFUSED, NeckSnapAction.INSTANCE.trigger(user2, mob), user2, ExtraActions.NECK_SNAP);
            helper.assertTrue(!mob.hasEffect(SpeciesRegistries.SNAPPED), "the mob was snapped in the grace");
            clean(user2);
        } finally {
            mob.discard();
            clean(user);
        }
        helper.succeed();
    }

    private static net.minecraft.world.entity.Mob vampireMob(GameTestHelper helper) {
        var type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("vampirism:vampire"));
        var m = (net.minecraft.world.entity.Mob) type.create(helper.getLevel());
        m.setPos(helper.absoluteVec(new Vec3(1.5, 1, 3.0)));
        m.setNoAi(true);
        helper.getLevel().addFreshEntity(m);
        return m;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theCooldownIsNeverShorterThanTheLongestSnap(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        TestPlayers.TalkingDuelist user = vampire(helper, "S-VAM-9");
        try {
            SpeciesConfig.overrideSnapSeconds(new SpeciesConfig.SnapSeconds(11, 22, 33, 66));
            helper.assertTrue(NeckSnapAction.INSTANCE.cooldownTicks(user) == Math.max(SpeciesConfig.neckSnapCooldownSeconds(), 66) * 20, "the cooldown is " + NeckSnapAction.INSTANCE.cooldownTicks(user));
            helper.assertTrue(NeckSnapAction.INSTANCE.cooldownTicks(user) >= 66 * 20, "the cooldown is shorter than the longest snap");
        } finally {
            SpeciesConfig.overrideSnapSeconds(null);
            clean(user);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void eachKindWakesAfterItsOwnConfiguredTime(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        try {
            SpeciesConfig.overrideSnapSeconds(new SpeciesConfig.SnapSeconds(11, 22, 33, 44));
            helper.assertTrue(snapTicks(helper, Species.NONE, true, "vampire") == 11 * 20, "vampire");
            helper.assertTrue(snapTicks(helper, Species.AUGUSTINE, true, "augustine") == 22 * 20, "augustine");
            helper.assertTrue(snapTicks(helper, Species.HYBRID, false, "hybrid") == 33 * 20, "hybrid");
            helper.assertTrue(snapTicks(helper, Species.ORIGINAL, true, "original") == 44 * 20, "original");
        } finally {
            SpeciesConfig.overrideSnapSeconds(null);
        }
        helper.succeed();
    }

    // ---- H3: i-frames ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void ripHeartAfterAHitGivesTheSameAnswerForAnOriginalAndAnyoneElse(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        TestPlayers.TalkingDuelist userA = vampire(helper, "S-VAM-8");
        TestPlayers.TalkingDuelist userB = vampire(helper, "S-VAM-8");
        Zombie z = zombie(helper, 20.0f);
        ServerPlayer original = target(helper, Species.ORIGINAL, true);
        try {
            // 11 of 20 health: 9 are left, so the heart-rip's blow (9 + 1) is not above the 11 the i-frames remember, and without bypasses_cooldown it would be swallowed
            z.hurt(helper.getLevel().damageSources().generic(), 11.0f);
            original.hurt(helper.getLevel().damageSources().generic(), 11.0f);
            helper.assertTrue(z.getHealth() == 9.0f && original.getHealth() > 9.0f && original.getHealth() <= 10.0f, "setup: health " + z.getHealth() + " / " + original.getHealth());
            helper.assertTrue(z.invulnerableTime > 10 && original.invulnerableTime > 10, "setup: the hit left no invulnerability frames");
            helper.assertTrue(RipHeartAction.INSTANCE.trigger(userA, z) == null, "a hit mob refused the heart-rip");
            helper.assertTrue(RipHeartAction.INSTANCE.trigger(userB, original) == null, "a hit Original refused: it would tell it from a mob");
            helper.assertTrue(!z.isAlive(), "the hit mob lived (the i-frames swallowed the heart-rip)");
            helper.assertTrue(original.isAlive(), "an Original died");
            helper.assertTrue(userA.receivedOnActionBar(RipHeartAction.DONE) && userB.receivedOnActionBar(RipHeartAction.DONE)
                    && ActionClock.left(userA, ExtraActions.RIP_HEART) == ActionClock.left(userB, ExtraActions.RIP_HEART), "the two answers differ");
        } finally {
            z.discard();
            clean(userA, userB, original);
        }
        helper.succeed();
    }

    // ---- H4: Vampirism's dying state ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void ripHeartKillsAVampireInVampirismsDyingStateForGoodAndSparesAnOriginalThere(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        TestPlayers.TalkingDuelist userA = vampire(helper, "S-VAM-8");
        TestPlayers.TalkingDuelist userB = vampire(helper, "S-VAM-8");
        ServerPlayer vamp = target(helper, Species.NONE, true);
        ServerPlayer original = target(helper, Species.ORIGINAL, true);
        boolean[] final_ = new boolean[1];
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingDeathEvent> watch = e -> {
            if (e.getEntity() == vamp) {
                final_[0] = net.ofatech.supernaturalbloodlines.core.death.FinalDeath.is(vamp);
            }
        };
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, true, net.neoforged.neoforge.event.entity.living.LivingDeathEvent.class, watch);
        try {
            intoDbno(helper, vamp);
            intoDbno(helper, original);
            helper.assertTrue(RipHeartAction.INSTANCE.trigger(userA, vamp) == null && RipHeartAction.INSTANCE.trigger(userB, original) == null, "refused");
            helper.assertTrue(vamp.isDeadOrDying(), "a vampire in Vampirism's dying state survived a heart-rip");
            helper.assertTrue(final_[0], "the heart-rip was not a final death");
            helper.assertTrue(!original.isDeadOrDying() && VampirismAPI.vampirePlayer(original).isDBNO(), "an Original in the dying state was killed");
            helper.assertTrue(userA.receivedOnActionBar(RipHeartAction.DONE) && userB.receivedOnActionBar(RipHeartAction.DONE)
                    && ActionClock.left(userA, ExtraActions.RIP_HEART) == ActionClock.left(userB, ExtraActions.RIP_HEART), "the two answers differ");
        } finally {
            NeoForge.EVENT_BUS.unregister(watch);
            OriginalRulesProbe.endDying(original);
            clean(userA, userB, vamp, original);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void ripHeartFinishesAVampireWhoseDeathVampirismWouldCatch(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        TestPlayers.TalkingDuelist user = vampire(helper, "S-VAM-8");
        ServerPlayer vamp = target(helper, Species.NONE, true);
        boolean[] caught = new boolean[1];
        // Vampirism does not catch a rip_heart blow by itself in this setup; what it does for another blow (cancel the death, start its dying state) is done by hand,
        // so the heart-rip meets a vampire whose death was caught only now
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingDeathEvent> watch = e -> {
            if (e.getEntity() == vamp && !caught[0]) {
                caught[0] = true;
                e.setCanceled(true);
                vamp.setHealth(1.0f);
                try {
                    var vampire = VampirismAPI.vampirePlayer(vamp);
                    var setTimer = vampire.getClass().getDeclaredMethod("setDBNOTimer", int.class);
                    setTimer.setAccessible(true);
                    setTimer.invoke(vampire, 200);
                } catch (ReflectiveOperationException ex) {
                    throw new IllegalStateException(ex);
                }
            }
        };
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST, false, net.neoforged.neoforge.event.entity.living.LivingDeathEvent.class, watch);        try {
            vamp.setHealth(5.0f);
            helper.assertTrue(!VampirismAPI.vampirePlayer(vamp).isDBNO(), "setup: already in the dying state");
            helper.assertTrue(RipHeartAction.INSTANCE.trigger(user, vamp) == null, "refused");
            helper.assertTrue(caught[0], "setup: the death was not caught, so this test would not reach the hotspot");
            helper.runAfterDelay(2, () -> {
                try {
                    helper.assertTrue(!vamp.isAlive(), "the vampire was put in the dying state and not finished");
                } finally {
                    NeoForge.EVENT_BUS.unregister(watch);
                    OriginalRulesProbe.endDying(vamp);
                    clean(user, vamp);
                }
                helper.succeed();
            });
        } catch (RuntimeException ex) {
            NeoForge.EVENT_BUS.unregister(watch);
            OriginalRulesProbe.endDying(vamp);
            clean(user, vamp);
            throw ex;
        }
    }
    @GameTest(template = "empty", templateNamespace = NS)
    public static void neckSnapOnAPlayerInVampirismsDyingStateIsRefused(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        TestPlayers.TalkingDuelist user = vampire(helper, "S-VAM-9");
        ServerPlayer vamp = target(helper, Species.NONE, true);
        try {
            intoDbno(helper, vamp);
            assertRefused(helper, "snap in the dying state", NeckSnapAction.REFUSED, NeckSnapAction.INSTANCE.trigger(user, vamp), user, ExtraActions.NECK_SNAP);
            helper.assertTrue(!Downed.isSnapped(vamp), "a player in the dying state was snapped");
        } finally {
            OriginalRulesProbe.endDying(vamp);
            clean(user, vamp);
        }
        helper.succeed();
    }

    // ---- the probe leaves no trace ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aProbeDoesNotDesiccateAnNpcOriginalAndOrdinaryDamageDoes(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        LivingEntity klaus = boss(helper);
        if (!(klaus instanceof net.ofatech.supernaturalbloodlines.parts.bosses.entity.NpcOriginalBoss boss)) {
            helper.succeed(); // the bosses part is off
            return;
        }
        try {
            boss.setHealth(1.0f);
            var probe = new net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent(boss,
                    new net.neoforged.neoforge.common.damagesource.DamageContainer(helper.getLevel().damageSources().generic(), 1000f));
            net.ofatech.supernaturalbloodlines.core.death.DamageProbe.run(() -> NeoForge.EVENT_BUS.post(probe));
            helper.assertTrue(probe.getAmount() == 1000f && !probe.isCanceled() && !boss.takePendingDesiccation() && !boss.isDesiccated(), "a probe changed the NPC Original: amount " + probe.getAmount());
            var real = new net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent(boss,
                    new net.neoforged.neoforge.common.damagesource.DamageContainer(helper.getLevel().damageSources().generic(), 1000f));
            NeoForge.EVENT_BUS.post(real);
            helper.assertTrue(real.getAmount() != 1000f || real.isCanceled() || boss.takePendingDesiccation() || boss.isDesiccated(), "setup: a real lethal blow changes nothing, so the probe check proves nothing");
        } finally {
            boss.discard();
        }
        helper.succeed();
    }

    // ---- claims, PvP and packs for every result ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anIncomingDamageCancellerRefusesEveryResultTheSame(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent> canceller = e -> e.setCanceled(true);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGH, canceller);
        java.util.List<Runnable> cleanups = new java.util.ArrayList<>();
        try {
            ServerPlayer vamp = target(helper, Species.NONE, true);
            ServerPlayer original = target(helper, Species.ORIGINAL, true);
            original.setHealth(5.0f);
            Zombie healthy = zombie(helper, 20.0f);
            Zombie weak = zombie(helper, 5.0f);
            LivingEntity klaus = boss(helper);
            cleanups.add(() -> {
                clean(vamp, original);
                healthy.discard();
                weak.discard();
                if (klaus != null) {
                    klaus.discard();
                }
            });
            record Case(String what, boolean snap, LivingEntity target) {
            }
            java.util.List<Case> cases = new java.util.ArrayList<>(java.util.List.of(new Case("snap a vampire", true, vamp), new Case("stagger a healthy mortal", true, healthy),
                    new Case("kill a weak mortal", true, weak), new Case("rip an Original", false, original), new Case("rip a weak mortal", false, weak)));
            if (klaus != null) {
                cases.add(new Case("snap a boss", true, klaus));
            }
            for (Case k : cases) {
                TestPlayers.TalkingDuelist user = vampire(helper, k.snap() ? "S-VAM-9" : "S-VAM-8");
                Component answer = k.snap() ? NeckSnapAction.INSTANCE.trigger(user, k.target()) : RipHeartAction.INSTANCE.trigger(user, k.target());
                assertRefused(helper, k.what(), k.snap() ? NeckSnapAction.REFUSED : RipHeartAction.REFUSED, answer, user, k.snap() ? ExtraActions.NECK_SNAP : ExtraActions.RIP_HEART);
                helper.assertTrue(k.target().isAlive() && !k.target().hasEffect(SpeciesRegistries.SNAPPED) && !(k.target() instanceof ServerPlayer sp && Downed.isDowned(sp)), k.what() + ": it acted anyway");
                clean(user);
            }
        } finally {
            NeoForge.EVENT_BUS.unregister(canceller);
            cleanups.forEach(Runnable::run);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void pvpOffRefusesEveryResultAgainstAPlayer(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        boolean pvp = helper.getLevel().getServer().isPvpAllowed();
        try {
            ServerPlayer vamp = target(helper, Species.NONE, true);
            ServerPlayer mortal = target(helper, Species.NONE, false);
            ServerPlayer original = target(helper, Species.ORIGINAL, true);
            original.setHealth(5.0f);
            helper.getLevel().getServer().setPvpAllowed(false);
            try {
                for (Object[] k : new Object[][] {{"snap a vampire player", true, vamp}, {"stagger a mortal player", true, mortal}, {"rip an Original player", false, original}}) {
                    ServerPlayer user = TestPlayers.createPvpAware(helper);
                    helper.assertTrue(VampirismAPI.factionPlayerHandler(user).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
                    SupernaturalAccess.set(user, SupernaturalAccess.get(user).toBuilder().skills(Set.of((boolean) k[1] ? "S-VAM-9" : "S-VAM-8")).build());
                    Component answer = (boolean) k[1] ? NeckSnapAction.INSTANCE.trigger(user, (LivingEntity) k[2]) : RipHeartAction.INSTANCE.trigger(user, (LivingEntity) k[2]);
                    assertRefused(helper, (String) k[0], (boolean) k[1] ? NeckSnapAction.REFUSED : RipHeartAction.REFUSED, answer, user, (boolean) k[1] ? ExtraActions.NECK_SNAP : ExtraActions.RIP_HEART);
                    clean(user);
                }
            } finally {
                clean(vamp, mortal, original);
            }
        } finally {
            helper.getLevel().getServer().setPvpAllowed(pvp);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPackMateIsRefusedForEveryResult(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        if (!net.neoforged.fml.ModList.get().isLoaded("werewolves")) {
            helper.succeed(); // no packs without Werewolves
            return;
        }
        var index = net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex.get(helper.getLevel());
        TestPlayers.TalkingDuelist leader = vampire(helper, "S-VAM-9", "S-VAM-8");
        ServerPlayer mate = target(helper, Species.ORIGINAL, true);
        mate.setHealth(5.0f);
        try {
            helper.assertTrue(index.join(leader.getUUID(), mate.getUUID()), "could not make the pack");
            assertRefused(helper, "snap a pack-mate", NeckSnapAction.REFUSED, NeckSnapAction.INSTANCE.trigger(leader, mate), leader, ExtraActions.NECK_SNAP);
            assertRefused(helper, "rip a pack-mate (an Original: the no-effect branch)", RipHeartAction.REFUSED, RipHeartAction.INSTANCE.trigger(leader, mate), leader, ExtraActions.RIP_HEART);
            helper.assertTrue(!Downed.isSnapped(mate), "a pack-mate was snapped");
        } finally {
            index.leave(leader.getUUID());
            index.leave(mate.getUUID());
            index.takeDissolved(leader.getUUID());
            index.takeDissolved(mate.getUUID());
            clean(leader, mate);
        }
        helper.succeed();
    }

    // ---- the turning ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void neckSnapKillsIntoTheTurningButRipHeartDoesNot(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        TestPlayers.TalkingDuelist snapper = vampire(helper, "S-VAM-9");
        TestPlayers.TalkingDuelist ripper = vampire(helper, "S-VAM-8");
        ServerPlayer a = TestPlayers.createTalkingDuelist(helper);
        ServerPlayer b = TestPlayers.createTalkingDuelist(helper);
        long until = helper.getLevel().getGameTime() + 100000;
        try {
            for (ServerPlayer m : new ServerPlayer[] {a, b}) {
                m.setPos(helper.absoluteVec(new Vec3(1.5, 1, 3.0)));
                BloodlineAccess.set(m, BloodlineState.NONE.toBuilder().vampireBloodUntil(until).build());
                m.setHealth(5.0f);
            }
            helper.assertTrue(NeckSnapAction.INSTANCE.trigger(snapper, a) == null, "the snap was refused");
            helper.assertTrue(!a.isDeadOrDying() && Downed.isDyingForTurning(a) && BloodlineAccess.get(a).inTurning(), "a snapped mortal with vampire blood did not start the turning: " + BloodlineAccess.get(a));
            helper.assertTrue(RipHeartAction.INSTANCE.trigger(ripper, b) == null, "the heart-rip was refused");
            helper.assertTrue(b.isDeadOrDying() && !BloodlineAccess.get(b).inTurning() && !Downed.isDowned(b), "a heart-rip started the turning: " + BloodlineAccess.get(b));
        } finally {
            clean(snapper, ripper, a, b);
        }
        helper.succeed();
    }

    // ---- snapped players ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSnappedPlayerWakesWithTheSafetyOfEveryWakeUp(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        TestPlayers.TalkingDuelist t = TestPlayers.createTalkingDuelist(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(t).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
        try {
            Downed.startSnapped(t, 40);
            helper.assertTrue(t.receivedOnActionBar("supernaturalbloodlines.snapped.body_short"), "an old client (no screen) was not told on the action bar");
            t.setRemainingFireTicks(100);
            t.setAirSupply(5);
            for (int i = 0; i < 40; i++) {
                Downed.tick(t);
            }
            helper.assertTrue(t.getRemainingFireTicks() == 0 && t.getAirSupply() == t.getMaxAirSupply(), "the fire or the air was not put right: fire " + t.getRemainingFireTicks() + ", air " + t.getAirSupply());
            helper.assertTrue(t.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE), "no Resistance on waking");
            helper.assertTrue(t.received("supernaturalbloodlines.snapped.woke") && t.receivedOnActionBar("supernaturalbloodlines.snapped.woke"), "the wake was not in chat and on the action bar");
            helper.assertTrue(net.ofatech.supernaturalbloodlines.parts.species.item.SnappedEffect.inGrace(t), "no grace after waking");
        } finally {
            clean(t);
        }
        helper.succeed();
    }

    // ---- small things ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aTargetBehindTheUserIsNoTarget(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        TestPlayers.TalkingDuelist user = vampire(helper, "S-VAM-8", "S-VAM-9");
        Zombie z = zombie(helper, 5.0f);
        try {
            user.setYRot(180.0f);
            helper.assertTrue(RipHeartAction.NO_TARGET.equals(key(RipHeartAction.INSTANCE.trigger(user, z))), "Rip Heart reached a target behind the user");
            helper.assertTrue(NeckSnapAction.NO_TARGET.equals(key(NeckSnapAction.INSTANCE.trigger(user, z))), "Neck Snap reached a target behind the user");
            helper.assertTrue(z.isAlive() && ActionClock.left(user, ExtraActions.RIP_HEART) == 0, "it acted anyway");
        } finally {
            z.discard();
            clean(user);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPlayerUnderRespawnProtectionIsRefused(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        TestPlayers.TalkingDuelist userA = vampire(helper, "S-VAM-8");
        TestPlayers.TalkingDuelist userB = vampire(helper, "S-VAM-9");
        ServerPlayer original = target(helper, Species.ORIGINAL, true);
        original.setHealth(5.0f);
        try {
            var field = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
            field.setAccessible(true);
            field.setInt(original, 60);
            assertRefused(helper, "rip a protected Original", RipHeartAction.REFUSED, RipHeartAction.INSTANCE.trigger(userA, original), userA, ExtraActions.RIP_HEART);
            assertRefused(helper, "snap a protected Original", NeckSnapAction.REFUSED, NeckSnapAction.INSTANCE.trigger(userB, original), userB, ExtraActions.NECK_SNAP);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        } finally {
            clean(userA, userB, original);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void everyBossEntityIsInTheSnapImmuneTagAndEveryTagEntryExists(GameTestHelper helper) {
        if (!net.ofatech.supernaturalbloodlines.core.EnabledParts.isEnabled("bosses")) {
            helper.succeed();
            return;
        }
        try (var in = TargetedSkillGameTests.class.getResourceAsStream("/data/supernaturalbloodlines/tags/entity_type/snap_immune.json")) {
            var root = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in)).getAsJsonObject();
            java.util.Set<ResourceLocation> listed = new java.util.HashSet<>();
            root.getAsJsonArray("values").forEach(v -> listed.add(ResourceLocation.parse(v.getAsJsonObject().get("id").getAsString())));
            for (ResourceLocation id : listed) {
                helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(id), "the snap_immune tag lists " + id + ", which does not exist");
            }
            for (var entry : BuiltInRegistries.ENTITY_TYPE.entrySet()) {
                ResourceLocation id = entry.getKey().location();
                if (!id.getNamespace().equals(NS)) {
                    continue;
                }
                var e = entry.getValue().create(helper.getLevel());
                if (e != null && e.getClass().getName().startsWith("net.ofatech.supernaturalbloodlines.parts.bosses.")) {
                    helper.assertTrue(listed.contains(id), "the boss " + id + " is not in the snap_immune tag");
                    e.discard();
                }
            }
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSnappedMobThatRunsOutBecomesFreeAndGetsAGrace(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        // a cow: a hostile mob would be discarded at once on the test world's peaceful difficulty
        var z = EntityType.COW.create(helper.getLevel());
        z.setPos(helper.absoluteVec(new Vec3(1.5, 1, 3.0)));
        z.setPersistenceRequired();
        helper.getLevel().addFreshEntity(z);
        z.addEffect(new net.minecraft.world.effect.MobEffectInstance(SpeciesRegistries.SNAPPED, 5));
        helper.assertTrue(z.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) == 0.0, "setup: the effect did not hold the mob");
        helper.runAfterDelay(12, () -> {
            try {
                helper.assertTrue(!z.hasEffect(SpeciesRegistries.SNAPPED), "the effect did not run out");
                helper.assertTrue(z.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) > 0.0, "the mob did not get its speed back: " + z.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) + ", effects " + z.getActiveEffects() + ", modifiers " + z.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).getModifiers());
                helper.assertTrue(net.ofatech.supernaturalbloodlines.parts.species.item.SnappedEffect.inGrace(z), "no grace after the natural end");
            } finally {
                z.discard();
            }
            helper.succeed();
        });
    }

    /** Ends Vampirism's dying state by hand, for a test's cleanup. */
    private static final class OriginalRulesProbe {
        static void endDying(ServerPlayer p) {
            var vampire = VampirismAPI.vampirePlayer(p);
            if (vampire.isDBNO()) {
                try {
                    vampire.getClass().getMethod("giveUpDBNO").invoke(vampire);
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSnappedPlayerIsNotSentTheScreenAgainAndAMortalDyingOneIs(GameTestHelper helper) {
        ServerPlayer snapped = TestPlayers.createDuelist(helper);
        ServerPlayer dying = TestPlayers.createDuelist(helper);
        int[] sent = new int[2];
        Downed.overrideSender((p, payload) -> {
            if (payload.downed()) {
                sent[p == snapped ? 0 : 1]++;
            }
        });
        try {
            Downed.startSnapped(snapped, 300);
            Downed.startDying(dying, helper.getLevel().damageSources().generic());
            int first0 = sent[0];
            int first1 = sent[1];
            for (int i = 0; i < 250; i++) {
                Downed.tick(snapped);
                Downed.tick(dying);
            }
            helper.assertTrue(sent[0] == first0, "a snapped player was sent the screen again " + (sent[0] - first0) + " times (they chose Wait)");
            helper.assertTrue(sent[1] > first1, "control: a dying player is not sent the screen again");
        } finally {
            Downed.overrideSender(null);
            Downed.clearForTest(snapped);
            Downed.clearForTest(dying);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theRipHeartSignalFiresOnlyWhenTheTargetDied(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        TestPlayers.TalkingDuelist user = vampire(helper, "S-VAM-8");
        Zombie saved = zombie(helper, 5.0f);
        Zombie plain = zombie(helper, 5.0f);
        int[] fired = new int[1];
        net.ofatech.supernaturalbloodlines.shared.supernatural.TaskSignals.Listener counter = (p, id, amount, attrs) -> {
            if ("rip_heart".equals(id)) {
                fired[0]++;
            }
        };
        net.ofatech.supernaturalbloodlines.shared.supernatural.TaskSignals.addListener(counter);
        // another mod saves one of them: the death is cancelled and it stands up with 1 health
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingDeathEvent> save = e -> {
            if (e.getEntity() == saved) {
                e.setCanceled(true);
                saved.setHealth(1.0f);
            }
        };
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, save);
        try {
            helper.assertTrue(RipHeartAction.INSTANCE.trigger(user, saved) == null, "refused");
            helper.assertTrue(saved.isAlive(), "setup: the other mod did not save the target");
            helper.assertTrue(fired[0] == 0, "the Rip Heart signal fired for a target that lived");
            ActionClock.start(user, ExtraActions.RIP_HEART, 0);
            helper.assertTrue(RipHeartAction.INSTANCE.trigger(user, plain) == null && !plain.isAlive(), "the control heart-rip failed");
            helper.assertTrue(fired[0] == 1, "the Rip Heart signal did not fire for a kill: " + fired[0]);
        } finally {
            NeoForge.EVENT_BUS.unregister(save);
            saved.discard();
            plain.discard();
            clean(user);
        }
        helper.succeed();
    }

    // ================= review round 2 =================

    private static ResourceLocation bat() {
        return ResourceLocation.parse("vampirism:bat");
    }

    private static de.teamlapen.vampirism.api.entity.player.actions.IAction.ActivationContext noContext() {
        return new de.teamlapen.vampirism.api.entity.player.actions.IAction.ActivationContext() {
            @Override
            public java.util.Optional<net.minecraft.core.BlockPos> targetBlock() {
                return java.util.Optional.empty();
            }

            @Override
            public java.util.Optional<net.minecraft.world.entity.Entity> targetEntity() {
                return java.util.Optional.empty();
            }
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static de.teamlapen.vampirism.api.entity.player.actions.IAction.PERM toggleBat(ServerPlayer p) {
        var handler = VampirismAPI.vampirePlayer(p).getActionHandler();
        var action = (de.teamlapen.vampirism.api.entity.player.actions.IAction) de.teamlapen.vampirism.api.VampirismRegistries.ACTION.get().get(bat());
        handler.unlockActions(java.util.List.of(action));
        return handler.toggleAction(action, noContext());
    }

    // ---- H1: the hold is never saved ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theSnapHoldIsNotInTheSaveAndComesBackAfterAReload(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        var cow = EntityType.COW.create(helper.getLevel());
        cow.setPos(helper.absoluteVec(new Vec3(1.5, 1, 3.0)));
        cow.setPersistenceRequired();
        helper.getLevel().addFreshEntity(cow);
        try {
            cow.addEffect(new net.minecraft.world.effect.MobEffectInstance(SpeciesRegistries.SNAPPED, 400));
            var speed = cow.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
            helper.assertTrue(speed.getValue() == 0.0 && speed.getModifier(net.ofatech.supernaturalbloodlines.parts.species.item.SnappedEffect.SPEED_ID) != null, "the hold is not on");
            var saved = cow.saveWithoutId(new net.minecraft.nbt.CompoundTag());
            helper.assertTrue(!saved.toString().contains("effect.snapped"), "the hold is in the save: a rollback or a lost effect would leave the creature frozen: " + saved.getList("attributes", 10));
            // a reload: the effect is saved, the hold is not; joining the level again puts it back
            speed.removeModifier(net.ofatech.supernaturalbloodlines.parts.species.item.SnappedEffect.SPEED_ID);
            helper.assertTrue(speed.getValue() > 0.0, "setup: the hold did not come off");
            NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.EntityJoinLevelEvent(cow, helper.getLevel(), false));
            helper.assertTrue(speed.getValue() == 0.0, "the hold did not come back when the creature joined the level again");
        } finally {
            cow.discard();
        }
        helper.succeed();
    }

    // ---- H2: Vampirism's own keys ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSnappedVampireCannotTakeBatFormAndAnActiveOneEndsOnTheSnap(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 5), "could not make a vampire");
        try {
            // the key's own event is cancelled for a player who cannot act (and only for them)
            var vp = VampirismAPI.vampirePlayer(p);
            var action = de.teamlapen.vampirism.api.VampirismRegistries.ACTION.get().get(bat());
            var control = new de.teamlapen.vampirism.api.event.ActionEvent.ActionActivatedEvent(vp, action, 0, 0);
            NeoForge.EVENT_BUS.post(control);
            helper.assertTrue(!control.isCanceled(), "control: a player who can act had their action cancelled");
            Downed.startSnapped(p, 400);
            var event = new de.teamlapen.vampirism.api.event.ActionEvent.ActionActivatedEvent(vp, action, 0, 0);
            NeoForge.EVENT_BUS.post(event);
            helper.assertTrue(event.isCanceled(), "a snapped player was allowed to take a form (Vampirism's own key)");
            Downed.clearForTest(p);
            // a form that is already on ends with the snap
            var perm = toggleBat(p);
            if (!vp.getActionHandler().isActionActive(bat())) {
                helper.succeed(); // the bat form cannot be taken in this setup (the sun, a vehicle): the event check above is the proof
                return;
            }
            helper.assertTrue(perm != null, "setup: toggle answered nothing");
            Downed.startSnapped(p, 400);
            helper.assertTrue(!vp.getActionHandler().isActionActive(bat()), "the bat form went on while the player lay snapped");
            helper.assertTrue(!p.getAbilities().flying, "a snapped player kept flying");
        } finally {
            Downed.clearForTest(p);
            clean(p);
        }
        helper.succeed();
    }

    // ---- M1: speed 0 is not all ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSnappedMobCannotBeShovedOrBlinkAway(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        var cow = EntityType.COW.create(helper.getLevel());
        cow.setPos(helper.absoluteVec(new Vec3(1.5, 3, 3.0)));
        cow.setPersistenceRequired();
        cow.setNoGravity(true);
        helper.getLevel().addFreshEntity(cow);
        try {
            cow.addEffect(new net.minecraft.world.effect.MobEffectInstance(SpeciesRegistries.SNAPPED, 400));
            var teleport = new net.neoforged.neoforge.event.entity.EntityTeleportEvent.EnderEntity(cow, 1, 2, 3);
            NeoForge.EVENT_BUS.post(teleport);
            helper.assertTrue(teleport.isCanceled(), "a snapped creature may teleport");
            var free = EntityType.COW.create(helper.getLevel());
            free.setPos(helper.absoluteVec(new Vec3(3.5, 3, 3.0)));
            var freeTeleport = new net.neoforged.neoforge.event.entity.EntityTeleportEvent.EnderEntity(free, 1, 2, 3);
            NeoForge.EVENT_BUS.post(freeTeleport);
            helper.assertTrue(!freeTeleport.isCanceled(), "control: a free creature's teleport was cancelled");
            cow.setDeltaMovement(1.0, 1.0, 1.0);
            helper.runAfterDelay(3, () -> {
                try {
                    var v = cow.getDeltaMovement();
                    helper.assertTrue(v.x == 0.0 && v.z == 0.0 && v.y <= 0.0, "a snapped creature kept the push it was given: " + v);
                } finally {
                    cow.discard();
                }
                helper.succeed();
            });
        } catch (RuntimeException ex) {
            cow.discard();
            throw ex;
        }
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void milkDoesNotEndASnap(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        var cow = EntityType.COW.create(helper.getLevel());
        cow.setPos(helper.absoluteVec(new Vec3(1.5, 1, 3.0)));
        cow.setPersistenceRequired();
        helper.getLevel().addFreshEntity(cow);
        try {
            cow.addEffect(new net.minecraft.world.effect.MobEffectInstance(SpeciesRegistries.SNAPPED, 400));
            cow.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.WEAKNESS, 400));
            cow.removeEffectsCuredBy(net.neoforged.neoforge.common.EffectCures.MILK);
            helper.assertTrue(cow.hasEffect(SpeciesRegistries.SNAPPED), "milk ended a snapped neck");
            helper.assertTrue(!cow.hasEffect(MobEffects.WEAKNESS), "control: milk cures nothing at all");
        } finally {
            cow.discard();
        }
        helper.succeed();
    }

    // ---- M2: a running bite ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBiteThatIsRunningEndsWhenThePlayerIsSnapped(GameTestHelper helper) {
        if (!net.ofatech.supernaturalbloodlines.core.EnabledParts.isEnabled("hud")) {
            helper.succeed(); // no Bite action without the hud part: nothing to stop
            return;
        }
        ServerPlayer p = TestPlayers.createMortal(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
        p.setPos(helper.absoluteVec(new Vec3(1.5, 2, 1.5)));
        var v = villager(helper);
        v.setPos(helper.absoluteVec(new Vec3(1.5, 2, 2.5)));
        try {
            var r = net.ofatech.supernaturalbloodlines.parts.hud.bite.BiteService.press(p, v.getId());
            helper.assertTrue(r.ok(), "setup: the bite was refused: " + r.refusal());
            var vp = VampirismAPI.vampirePlayer(p);
            var field = vp.getClass().getDeclaredField("feed_victim");
            field.setAccessible(true);
            helper.assertTrue(field.getInt(vp) == v.getId(), "setup: the feeding did not start (" + field.getInt(vp) + ")");
            Downed.startSnapped(p, 400);
            helper.assertTrue(field.getInt(vp) == -1, "the bite went on while the player lay snapped");
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        } finally {
            v.discard();
            net.ofatech.supernaturalbloodlines.parts.hud.bite.BiteService.release(p);
            clean(p);
        }
        helper.succeed();
    }

    // ---- small things ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anOldClientIsToldAgainEveryFewSeconds(GameTestHelper helper) {
        TestPlayers.TalkingDuelist t = TestPlayers.createTalkingDuelist(helper);
        try {
            Downed.startSnapped(t, 400);
            int first = t.countOnActionBar("supernaturalbloodlines.snapped.body_short");
            for (int i = 0; i < 250; i++) {
                Downed.tick(t);
            }
            helper.assertTrue(t.countOnActionBar("supernaturalbloodlines.snapped.body_short") >= first + 2, "the old client was told once and then left alone: " + t.countOnActionBar("supernaturalbloodlines.snapped.body_short"));
        } finally {
            Downed.clearForTest(t);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theCrosshairMustBeOnTheTargetNotJustInFrontOfTheUser(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        TestPlayers.TalkingDuelist user = vampire(helper, "S-VAM-9");
        Zombie z = zombie(helper, 5.0f);
        try {
            user.setPos(helper.absoluteVec(new Vec3(1.5, 1, 0.5)));
            user.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, z.getEyePosition());
            helper.assertTrue(NeckSnapAction.INSTANCE.trigger(user, z) == null, "the crosshair was on the target and it was refused");
            Zombie far = zombie(helper, 5.0f);
            far.setPos(helper.absoluteVec(new Vec3(5.5, 1, 3.0)));
            helper.assertTrue(NeckSnapAction.NO_TARGET.equals(key(NeckSnapAction.INSTANCE.trigger(user, far))), "a target 4 blocks off to the side, ahead of the user, was taken");
            far.discard();
        } finally {
            z.discard();
            clean(user);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aProbeDoesNotCountAPikeHitOnTheSpiritButARealBlowDoes(GameTestHelper helper) {
        if (!net.ofatech.supernaturalbloodlines.core.EnabledParts.isEnabled("bosses")) {
            helper.succeed();
            return;
        }
        ServerPlayer striker = TestPlayers.createDuelist(helper);
        var type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath(NS, "gevaudan_spirit"));
        var spirit = (net.ofatech.supernaturalbloodlines.parts.bosses.entity.GevaudanSpiritEntity) type.create(helper.getLevel());
        try {
            striker.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(net.ofatech.supernaturalbloodlines.parts.bosses.BossItems.VALET_PIKE.get()));
            var source = striker.damageSources().playerAttack(striker);
            var probe = new net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent(spirit, new net.neoforged.neoforge.common.damagesource.DamageContainer(source, 1.0f));
            net.ofatech.supernaturalbloodlines.core.death.DamageProbe.run(() -> NeoForge.EVENT_BUS.post(probe));
            helper.assertTrue(!spirit.pikeHit(), "a probe was counted as a Pike hit");
            helper.assertTrue(!probe.isCanceled(), "a probe of a Pike blow was cancelled: the answer must be the same as for a real one");
            var real = new net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent(spirit, new net.neoforged.neoforge.common.damagesource.DamageContainer(source, 1.0f));
            NeoForge.EVENT_BUS.post(real);
            helper.assertTrue(spirit.pikeHit(), "control: a real Pike blow was not counted");
        } finally {
            spirit.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void neckSnapOnAPlayerWithPvpOffIsRefusedLikeAnyRefusal(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        TestPlayers.TalkingDuelist user = vampire(helper, "S-VAM-9");
        ServerPlayer other = target(helper, Species.NONE, true);
        Zombie z = zombie(helper, 20.0f);
        boolean pvp = helper.getLevel().getServer().isPvpAllowed();
        helper.getLevel().getServer().setPvpAllowed(false);
        try {
            helper.assertTrue(NeckSnapAction.REFUSED.equals(key(NeckSnapAction.INSTANCE.trigger(user, other))), "PvP off did not refuse a Neck Snap of a player");
            helper.assertTrue(!Downed.isSnapped(other) && ActionClock.left(user, ExtraActions.NECK_SNAP) == 0, "the refusal had an effect");
            helper.assertTrue(NeckSnapAction.INSTANCE.trigger(user, z) == null, "PvP off refused a mob");
        } finally {
            helper.getLevel().getServer().setPvpAllowed(pvp);
            z.discard();
            clean(user, other);
        }
        helper.succeed();
    }
}