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
            helper.assertTrue(RipHeartAction.REFUSED.equals(key(RipHeartAction.INSTANCE.trigger(user, z))), "a strong mob was not refused");
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
            helper.assertTrue(RipHeartAction.REFUSED.equals(key(RipHeartAction.INSTANCE.trigger(userC, original))), "a healthy Original was not refused");
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
    public static void aSnappedMobLiesDownThenGetsItsAiBack(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        TestPlayers.TalkingDuelist user = vampire(helper, "S-VAM-9");
        TestPlayers.TalkingDuelist user2 = vampire(helper, "S-VAM-9");
        Zombie thinking = zombie(helper, 20.0f);
        Zombie dumb = zombie(helper, 20.0f);
        thinking.setNoAi(false);
        thinking.setData(SpeciesRegistries.ORIGINAL_ROOT, java.util.UUID.randomUUID().toString());
        dumb.setNoAi(true);
        dumb.setData(SpeciesRegistries.ORIGINAL_ROOT, java.util.UUID.randomUUID().toString());
        try {
            helper.assertTrue(NeckSnapAction.INSTANCE.trigger(user, thinking) == null && NeckSnapAction.INSTANCE.trigger(user2, dumb) == null, "refused");
            helper.assertTrue(thinking.hasEffect(SpeciesRegistries.SNAPPED) && thinking.isNoAi(), "the mob was not snapped");
            helper.assertTrue(thinking.getHealth() == 20.0f, "the snap itself cost the mob health: " + thinking.getHealth());
            boolean hit = thinking.hurt(helper.getLevel().damageSources().generic(), 3.0f);
            helper.assertTrue(!hit && thinking.getHealth() == 20.0f, "a snapped mob was hurt: hit " + hit + ", health " + thinking.getHealth() + ", effect " + thinking.hasEffect(SpeciesRegistries.SNAPPED));
            thinking.removeEffect(SpeciesRegistries.SNAPPED);
            helper.assertTrue(!thinking.isNoAi(), "the mob did not get its AI back");
            dumb.removeEffect(SpeciesRegistries.SNAPPED);
            helper.assertTrue(dumb.isNoAi(), "a mob that had no AI was given some");
        } finally {
            thinking.discard();
            dumb.discard();
            clean(user, user2);
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
        var type = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.fromNamespaceAndPath(NS, "klaus"));
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
}
