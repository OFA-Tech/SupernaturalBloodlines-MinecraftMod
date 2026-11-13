package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IFactionPlayerHandler;
import de.teamlapen.werewolves.api.WReference;
import java.util.UUID;
import net.favouriteless.enchanted.common.enchanted.circle_magic.RiteType;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.species.OutcomeExecutor;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.hybrid.TransitionHandler;
import net.ofatech.supernaturalbloodlines.parts.species.item.OriginalBloodEffect;
import net.ofatech.supernaturalbloodlines.parts.species.item.UnstableEffect;

import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.type;

/** The hybrid transition (R40, R45, R64). Registered only when Werewolves is loaded, so nothing here links it without. */
@PrefixGameTestTemplate(false)
public final class HybridGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private HybridGameTests() {
    }

    private static IFactionPlayerHandler handler(ServerPlayer p) {
        return VampirismAPI.factionPlayerHandler(p);
    }

    /** A werewolf that can die like a real player. */
    private static ServerPlayer wolf(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        helper.assertTrue(handler(p).setFactionAndLevel(WReference.WEREWOLF_FACTION, 1), "could not make a werewolf");
        return p;
    }

    private static void drinkOriginalBlood(GameTestHelper helper, ServerPlayer p, UUID source) {
        ItemStack bottle = new ItemStack(SpeciesRegistries.ORIGINAL_BLOOD.get());
        bottle.set(SpeciesRegistries.BLOOD_SOURCE_COMPONENT.get(), source);
        p.setItemInHand(InteractionHand.MAIN_HAND, bottle.finishUsingItem(helper.getLevel(), p));
    }

    private static void killByAnything(GameTestHelper helper, ServerPlayer p) {
        p.hurt(helper.getLevel().damageSources().generic(), 1000f);
    }

    private static BloodlineState inTransition(UUID sire, long left) {
        return BloodlineState.NONE.toBuilder().inTransition(true).pendingSire(sire).transitionTicksLeft(left).build();
    }

    private static void reset(ServerPlayer p) {
        BloodlineAccess.set(p, BloodlineState.NONE);
    }

    // ---- data ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void hybridDataLoads(GameTestHelper helper) {
        RiteType rite = type(helper, "doppelganger_blood");
        helper.assertTrue(rite != null && rite.getPower() == 6000, "doppelganger_blood rite missing or wrong power");
        helper.assertTrue(rite.getItems().size() == 3, "doppelganger_blood has " + rite.getItems().size() + " ingredients");
        helper.assertTrue(rite.getItems().stream().anyMatch(s -> s.is(Items.DIAMOND) && s.getCount() == 2), "needs 2 diamonds");
        helper.assertTrue(rite.getItems().stream().anyMatch(s -> s.is(Items.AMETHYST_SHARD)), "needs an amethyst shard");
        ItemStack bloodBottle = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM
                .get(ResourceLocation.parse("vampirism:blood_bottle")));
        helper.assertTrue(bloodBottle.is(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(NS, "blood_drinks"))),
                "Vampirism's blood bottle is not tagged blood_drinks");
        helper.succeed();
    }

    // ---- death ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void werewolfDyingWithOriginalBloodEntersTransition(GameTestHelper helper) {
        ServerPlayer p = wolf(helper);
        UUID original = UUID.randomUUID();
        try {
            drinkOriginalBlood(helper, p, original);
            killByAnything(helper, p);
            BloodlineState s = BloodlineAccess.get(p);
            helper.assertTrue(s.inTransition() && !s.unstable(), "not in a stable transition: " + s);
            helper.assertTrue(original.equals(s.pendingSire()), "pending sire is " + s.pendingSire());
            helper.assertTrue(s.transitionTicksLeft() == SpeciesConfig.transitionTicks(), "ticks left " + s.transitionTicksLeft());
            helper.assertTrue(s.species() == Species.NONE, "species is " + s.species());
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void otherDeathsStartNoTransition(GameTestHelper helper) {
        ServerPlayer noEffect = wolf(helper);
        ServerPlayer human = TestPlayers.createMortal(helper);
        ServerPlayer failed = wolf(helper);
        try {
            killByAnything(helper, noEffect);
            helper.assertTrue(!BloodlineAccess.get(noEffect).inTransition(), "a werewolf without the blood started a transition");
            human.addEffect(new MobEffectInstance(SpeciesRegistries.ORIGINAL_BLOOD_EFFECT, OriginalBloodEffect.DURATION_TICKS));
            killByAnything(helper, human);
            helper.assertTrue(!BloodlineAccess.get(human).inTransition(), "a human started a transition");
            failed.addEffect(new MobEffectInstance(SpeciesRegistries.ORIGINAL_BLOOD_EFFECT, OriginalBloodEffect.DURATION_TICKS));
            failed.hurt(failed.damageSources().source(OutcomeExecutor.FAILED_TRANSITION), 1000f);
            helper.assertTrue(!BloodlineAccess.get(failed).inTransition(), "a failed transition restarted itself");
        } finally {
            reset(noEffect);
            reset(human);
            reset(failed);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void secondDoseAfterDeathChangesNothing(GameTestHelper helper) {
        ServerPlayer p = wolf(helper);
        UUID first = UUID.randomUUID();
        try {
            BloodlineAccess.set(p, inTransition(first, 5000));
            drinkOriginalBlood(helper, p, UUID.randomUUID());
            BloodlineState s = BloodlineAccess.get(p);
            helper.assertTrue(s.inTransition() && !s.unstable() && first.equals(s.pendingSire()) && s.transitionTicksLeft() == 5000,
                    "a second dose changed the transition: " + s);
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void lastOriginalBloodIsSire(GameTestHelper helper) {
        ServerPlayer p = wolf(helper);
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        try {
            drinkOriginalBlood(helper, p, a);
            drinkOriginalBlood(helper, p, b);
            killByAnything(helper, p);
            helper.assertTrue(b.equals(BloodlineAccess.get(p).pendingSire()), "the sire is " + BloodlineAccess.get(p).pendingSire() + ", not the last blood " + b);
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    // ---- drinking ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void doppelgangerBloodCompletesHybrid(GameTestHelper helper) {
        ServerPlayer p = wolf(helper);
        UUID sire = UUID.randomUUID();
        try {
            BloodlineAccess.set(p, inTransition(sire, 9000));
            ItemStack bottle = new ItemStack(SpeciesRegistries.DOPPELGANGER_BLOOD.get());
            helper.assertTrue(bottle.use(helper.getLevel(), p, InteractionHand.MAIN_HAND).getResult().consumesAction(), "cannot start drinking in a transition");
            p.setItemInHand(InteractionHand.MAIN_HAND, bottle.finishUsingItem(helper.getLevel(), p));
            BloodlineState s = BloodlineAccess.get(p);
            helper.assertTrue(s.species() == Species.HYBRID, "species is " + s.species());
            helper.assertTrue(sire.equals(s.sire()), "sire is " + s.sire());
            helper.assertTrue(!s.inTransition() && s.pendingSire() == null && s.transitionTicksLeft() == 0, "transition left behind: " + s);
            helper.assertTrue(p.getMainHandItem().is(Items.GLASS_BOTTLE), "the empty bottle was not given back");
            helper.assertTrue(handler(p).getCurrentFaction() == WReference.WEREWOLF_FACTION, "left the werewolf faction");
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void doppelgangerBloodNeedsATransition(GameTestHelper helper) {
        ServerPlayer p = wolf(helper);
        ItemStack bottle = new ItemStack(SpeciesRegistries.DOPPELGANGER_BLOOD.get());
        helper.assertTrue(!bottle.use(helper.getLevel(), p, InteractionHand.MAIN_HAND).getResult().consumesAction(), "drinkable outside a transition");
        BloodlineAccess.set(p, inTransition(UUID.randomUUID(), 100).toBuilder().unstable(true).build());
        helper.assertTrue(!bottle.use(helper.getLevel(), p, InteractionHand.MAIN_HAND).getResult().consumesAction(), "drinkable while unstable");
        reset(p);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void otherBloodMakesUnstableThenKills(GameTestHelper helper) {
        ServerPlayer p = wolf(helper);
        try {
            BloodlineAccess.set(p, inTransition(UUID.randomUUID(), 9000));
            TransitionHandler.drankOtherBlood(p);
            BloodlineState s = BloodlineAccess.get(p);
            helper.assertTrue(s.inTransition() && s.unstable(), "not unstable: " + s);
            MobEffectInstance effect = p.getEffect(SpeciesRegistries.UNSTABLE_EFFECT);
            helper.assertTrue(effect != null && effect.getDuration() == UnstableEffect.DURATION_TICKS && UnstableEffect.DURATION_TICKS == 1200,
                    "unstable effect is " + effect);
            helper.assertTrue(!p.isDeadOrDying(), "dead before the effect ended");
            TransitionHandler.secondPassed(p);
            helper.assertTrue(!p.isDeadOrDying(), "killed while still unstable");
            p.removeEffect(SpeciesRegistries.UNSTABLE_EFFECT);
            TransitionHandler.secondPassed(p);
            helper.assertTrue(p.isDeadOrDying(), "survived the unstable transition");
            helper.assertTrue(p.getLastDamageSource() != null && p.getLastDamageSource().is(OutcomeExecutor.FAILED_TRANSITION),
                    "died of " + p.getLastDamageSource());
            s = BloodlineAccess.get(p);
            helper.assertTrue(!s.inTransition() && !s.unstable() && s.species() == Species.NONE, "stayed a werewolf expected, state is " + s);
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void otherBloodOutsideATransitionDoesNothing(GameTestHelper helper) {
        ServerPlayer p = wolf(helper);
        TransitionHandler.drankOtherBlood(p);
        helper.assertTrue(BloodlineAccess.get(p).equals(BloodlineState.NONE) && !p.hasEffect(SpeciesRegistries.UNSTABLE_EFFECT),
                "blood outside a transition changed something: " + BloodlineAccess.get(p));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void unstableEffectIgnoresMilk(GameTestHelper helper) {
        ServerPlayer p = wolf(helper);
        p.addEffect(new MobEffectInstance(SpeciesRegistries.UNSTABLE_EFFECT, UnstableEffect.DURATION_TICKS));
        p.removeEffectsCuredBy(net.neoforged.neoforge.common.EffectCures.MILK);
        helper.assertTrue(p.hasEffect(SpeciesRegistries.UNSTABLE_EFFECT), "milk cured the unstable effect");
        helper.succeed();
    }

    // ---- the timer ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void transitionTimesOut(GameTestHelper helper) {
        ServerPlayer p = wolf(helper);
        try {
            BloodlineAccess.set(p, inTransition(UUID.randomUUID(), 60));
            TransitionHandler.secondPassed(p);
            TransitionHandler.secondPassed(p);
            helper.assertTrue(BloodlineAccess.get(p).transitionTicksLeft() == 20 && !p.isDeadOrDying(), "state is " + BloodlineAccess.get(p));
            TransitionHandler.secondPassed(p);
            helper.assertTrue(p.isDeadOrDying(), "survived the timeout");
            helper.assertTrue(p.getLastDamageSource() != null && p.getLastDamageSource().is(OutcomeExecutor.FAILED_TRANSITION),
                    "died of " + p.getLastDamageSource());
            BloodlineState s = BloodlineAccess.get(p);
            helper.assertTrue(!s.inTransition() && s.species() == Species.NONE && s.transitionTicksLeft() == 0, "state is " + s);
            helper.assertTrue(handler(p).getCurrentFaction() == WReference.WEREWOLF_FACTION, "no longer a werewolf");
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void timerOnlyRunsDuringATransition(GameTestHelper helper) {
        ServerPlayer p = wolf(helper);
        TransitionHandler.secondPassed(p);
        helper.assertTrue(BloodlineAccess.get(p).equals(BloodlineState.NONE) && !p.isDeadOrDying(), "state is " + BloodlineAccess.get(p));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void transitionTimerSurvivesRelog(GameTestHelper helper) {
        ServerPlayer p = wolf(helper);
        ServerPlayer reloaded = TestPlayers.create(helper);
        UUID sire = UUID.randomUUID();
        try {
            BloodlineAccess.set(p, inTransition(sire, SpeciesConfig.transitionTicks()));
            for (int i = 0; i < 10; i++) {
                TransitionHandler.secondPassed(p);
            }
            long expected = SpeciesConfig.transitionTicks() - 200;
            CompoundTag saved = p.saveWithoutId(new CompoundTag());
            reloaded.load(saved);
            BloodlineState s = BloodlineAccess.get(reloaded);
            helper.assertTrue(s.inTransition() && s.transitionTicksLeft() == expected && sire.equals(s.pendingSire()),
                    "the relogged state is " + s + ", expected " + expected + " ticks left");
            TransitionHandler.secondPassed(reloaded);
            helper.assertTrue(BloodlineAccess.get(reloaded).transitionTicksLeft() == expected - 20, "the timer did not resume");
        } finally {
            reset(p);
            reset(reloaded);
        }
        helper.succeed();
    }
}
