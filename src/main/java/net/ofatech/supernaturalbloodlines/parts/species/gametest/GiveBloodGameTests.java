package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.death.DamageProbe;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.blood.GiveBloodAction;
import net.ofatech.supernaturalbloodlines.parts.species.blood.VampireBloodBuff;
import net.ofatech.supernaturalbloodlines.parts.species.item.BloodBottles;
import net.ofatech.supernaturalbloodlines.parts.species.item.BloodDrink;
import net.ofatech.supernaturalbloodlines.parts.species.line.Lines;
import net.ofatech.supernaturalbloodlines.parts.species.original.Downed;
import net.ofatech.supernaturalbloodlines.parts.species.skill.ActionClock;
import net.ofatech.supernaturalbloodlines.shared.hud.ExtraActions;

/** Plan 18f: Force-feed my blood (R208) and what the blood does while it lasts (R207). Needs no Werewolves: see {@code GiveBloodWerewolfGameTests}. */
@PrefixGameTestTemplate(false)
public final class GiveBloodGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final ExtraActions.Action ACTION = GiveBloodAction.INSTANCE;

    private GiveBloodGameTests() {
    }

    private static TestPlayers.TalkingDuelist vampire(GameTestHelper helper, Species species) {
        TestPlayers.TalkingDuelist p = TestPlayers.createTalkingDuelist(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(species).build());
        return p;
    }

    private static TestPlayers.TalkingDuelist human(GameTestHelper helper) {
        TestPlayers.TalkingDuelist t = TestPlayers.createTalkingDuelist(helper);
        t.setPos(helper.absoluteVec(new Vec3(1.5, 1, 3.0)));
        return t;
    }

    private static void clean(ServerPlayer... players) {
        for (ServerPlayer p : players) {
            Downed.clearForTest(p);
            p.removeAllEffects();
            p.setShiftKeyDown(false);
            p.setHealth(p.getMaxHealth());
            BloodlineAccess.set(p, BloodlineState.NONE);
            if (VampirismAPI.factionPlayerHandler(p).getCurrentFaction() != null) {
                VampirismAPI.factionPlayerHandler(p).leaveFaction(false);
            }
        }
    }

    private static String key(Component c) {
        return c != null && c.getContents() instanceof TranslatableContents t ? t.getKey() : String.valueOf(c);
    }

    private static boolean marked(ServerPlayer p) {
        return BloodlineAccess.get(p).vampireBloodUntil() > p.level().getGameTime();
    }

    private static void pvp(GameTestHelper helper, boolean on) {
        helper.getLevel().getServer().setPvpAllowed(on);
    }

    // ---- who may be fed ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void forceFeedMarksAnUnwillingHumanAtLowHealth(GameTestHelper helper) {
        TestPlayers.TalkingDuelist giver = vampire(helper, Species.NONE);
        TestPlayers.TalkingDuelist target = human(helper);
        pvp(helper, true);
        try {
            target.setHealth(target.getMaxHealth() * 0.25f);
            helper.assertTrue(ACTION.trigger(giver, target) == null, "the action was refused");
            helper.assertTrue(BloodlineAccess.get(target).vampireBloodUntil() == helper.getLevel().getGameTime() + SpeciesConfig.vampireBloodTicks(), "the blood did not mark them: " + BloodlineAccess.get(target));
            helper.assertTrue(giver.receivedOnActionBar(GiveBloodAction.DONE) && ActionClock.left(giver, ExtraActions.GIVE_BLOOD) == SpeciesConfig.giveBloodCooldownSeconds() * 20, "the giver was not told or has no cooldown");
            helper.assertTrue(target.received(GiveBloodAction.RECEIVED) && target.received(BloodBottles.MARKED), "the target was not told what is in them: " + target.keys());
            helper.assertTrue(target.message(GiveBloodAction.RECEIVED).getContents() instanceof TranslatableContents t && t.getArgs()[0] instanceof Component c && c.getString().equals(giver.getDisplayName().getString()),
                    "the target was not told who");
        } finally {
            pvp(helper, false);
            clean(giver, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void refusesWhenPvpIsOff(GameTestHelper helper) {
        TestPlayers.TalkingDuelist giver = vampire(helper, Species.NONE);
        TestPlayers.TalkingDuelist target = human(helper);
        pvp(helper, false);
        try {
            target.setHealth(1f);
            helper.assertTrue(GiveBloodAction.NOT_WILLING.equals(key(ACTION.trigger(giver, target))), "pvp off did not refuse");
            helper.assertTrue(!marked(target) && ActionClock.left(giver, ExtraActions.GIVE_BLOOD) == 0, "it acted or started the cooldown");
            Downed.startSnapped(target, 600);
            helper.assertTrue(GiveBloodAction.NOT_WILLING.equals(key(ACTION.trigger(giver, target))) && !marked(target), "a downed target was fed with pvp off");
        } finally {
            clean(giver, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void refusesAHealthyUnwillingTargetAndFeedsAtTheLimitExactly(GameTestHelper helper) {
        TestPlayers.TalkingDuelist giver = vampire(helper, Species.NONE);
        TestPlayers.TalkingDuelist target = human(helper);
        pvp(helper, true);
        try {
            helper.assertTrue(GiveBloodAction.NOT_WILLING.equals(key(ACTION.trigger(giver, target))) && !marked(target), "a healthy unwilling target was fed");
            target.setHealth((float) (target.getMaxHealth() * (SpeciesConfig.forceFeedHealthLimit() + 0.05)));
            helper.assertTrue(GiveBloodAction.NOT_WILLING.equals(key(ACTION.trigger(giver, target))) && !marked(target), "a target over the limit was fed");
            target.setHealth((float) (target.getMaxHealth() * SpeciesConfig.forceFeedHealthLimit()));
            helper.assertTrue(ACTION.trigger(giver, target) == null && marked(target), "a target at the limit exactly was not fed");
        } finally {
            pvp(helper, false);
            clean(giver, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aDownedUnwillingTargetIsFedAtFullHealth(GameTestHelper helper) {
        TestPlayers.TalkingDuelist giver = vampire(helper, Species.NONE);
        TestPlayers.TalkingDuelist target = human(helper);
        pvp(helper, true);
        try {
            Downed.startSnapped(target, 600);
            helper.assertTrue(Downed.isDowned(target), "the target is not down");
            helper.assertTrue(ACTION.trigger(giver, target) == null && marked(target), "a downed target was not fed");
        } finally {
            pvp(helper, false);
            clean(giver, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSneakingTargetAcceptsAtFullHealthWithPvpOff(GameTestHelper helper) {
        TestPlayers.TalkingDuelist giver = vampire(helper, Species.HYBRID);
        TestPlayers.TalkingDuelist target = human(helper);
        pvp(helper, false);
        try {
            target.setShiftKeyDown(true);
            helper.assertTrue(ACTION.trigger(giver, target) == null && marked(target), "a sneaking target did not accept");
        } finally {
            clean(giver, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void vervainBlocksEvenAWillingTarget(GameTestHelper helper) {
        if (!BuiltInRegistries.ITEM.containsKey(ResourceLocation.fromNamespaceAndPath(NS, "vervain"))) {
            helper.succeed(); // no artifacts part: nothing keeps vampire blood out
            return;
        }
        TestPlayers.TalkingDuelist giver = vampire(helper, Species.NONE);
        TestPlayers.TalkingDuelist target = human(helper);
        pvp(helper, true);
        try {
            target.setShiftKeyDown(true);
            target.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(NS, "vervain"))));
            helper.assertTrue(GiveBloodAction.VERVAIN.equals(key(ACTION.trigger(giver, target))), "vervain did not block a willing target");
            target.setHealth(1f);
            target.setShiftKeyDown(false);
            helper.assertTrue(GiveBloodAction.VERVAIN.equals(key(ACTION.trigger(giver, target))) && !marked(target), "vervain did not block an unwilling one");
            helper.assertTrue(ActionClock.left(giver, ExtraActions.GIVE_BLOOD) == 0, "a refusal started the cooldown");
            target.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            helper.assertTrue(ACTION.trigger(giver, target) == null && marked(target), "without vervain the same target was not fed");
        } finally {
            pvp(helper, false);
            clean(giver, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aClaimThatCancelsTheBlowOrTheClickBlocksIt(GameTestHelper helper) {
        TestPlayers.TalkingDuelist giver = vampire(helper, Species.NONE);
        TestPlayers.TalkingDuelist target = human(helper);
        pvp(helper, true);
        Consumer<AttackEntityEvent> attackClaim = e -> e.setCanceled(true);
        Consumer<PlayerInteractEvent.EntityInteract> clickClaim = e -> e.setCanceled(true);
        try {
            target.setHealth(1f);
            NeoForge.EVENT_BUS.addListener(attackClaim);
            helper.assertTrue(GiveBloodAction.REFUSED.equals(key(ACTION.trigger(giver, target))) && !marked(target), "a claim on the blow did not block an unwilling feed");
            NeoForge.EVENT_BUS.unregister(attackClaim);
            helper.assertTrue(ActionClock.left(giver, ExtraActions.GIVE_BLOOD) == 0, "a refusal started the cooldown");
            target.setShiftKeyDown(true);
            NeoForge.EVENT_BUS.addListener(clickClaim);
            helper.assertTrue(GiveBloodAction.REFUSED.equals(key(ACTION.trigger(giver, target))) && !marked(target), "a claim on the click did not block a willing feed");
            NeoForge.EVENT_BUS.unregister(clickClaim);
            helper.assertTrue(ACTION.trigger(giver, target) == null && marked(target), "without a claim the feed did not happen");
        } finally {
            NeoForge.EVENT_BUS.unregister(attackClaim);
            NeoForge.EVENT_BUS.unregister(clickClaim);
            pvp(helper, false);
            clean(giver, target);
        }
        helper.succeed();
    }

    /** A claim's question is a posted click: our own click listeners (a dagger pull, a siphon, a compulsion) must see the probe and stay out of it. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void theClaimQuestionIsMarkedAsAProbe(GameTestHelper helper) {
        TestPlayers.TalkingDuelist giver = vampire(helper, Species.NONE);
        TestPlayers.TalkingDuelist target = human(helper);
        boolean[] seen = new boolean[1];
        Consumer<PlayerInteractEvent.EntityInteract> watch = e -> seen[0] = DamageProbe.active();
        NeoForge.EVENT_BUS.addListener(watch);
        try {
            target.setShiftKeyDown(true);
            helper.assertTrue(ACTION.trigger(giver, target) == null, "the action was refused");
            helper.assertTrue(seen[0], "the click was not asked as a probe");
            helper.assertTrue(!DamageProbe.active(), "the probe is still on");
        } finally {
            NeoForge.EVENT_BUS.unregister(watch);
            clean(giver, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void selfAndMobAndCreativeAndNoTargetAreRefused(GameTestHelper helper) {
        TestPlayers.TalkingDuelist giver = vampire(helper, Species.NONE);
        TestPlayers.TalkingDuelist creative = human(helper);
        Zombie z = EntityType.ZOMBIE.create(helper.getLevel());
        z.setPos(helper.absoluteVec(new Vec3(1.5, 1, 3.0)));
        z.setNoAi(true);
        helper.getLevel().addFreshEntity(z);
        pvp(helper, true);
        try {
            helper.assertTrue(GiveBloodAction.SELF.equals(key(ACTION.trigger(giver, giver))), "self was not refused");
            helper.assertTrue(GiveBloodAction.NOT_A_PLAYER.equals(key(ACTION.trigger(giver, z))), "a mob was not refused");
            helper.assertTrue(GiveBloodAction.NO_TARGET.equals(key(ACTION.trigger(giver, null))), "no target was not said");
            creative.setShiftKeyDown(true);
            creative.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            helper.assertTrue(GiveBloodAction.REFUSED.equals(key(ACTION.trigger(giver, creative))) && !marked(creative), "a creative player was fed");
            helper.assertTrue(ActionClock.left(giver, ExtraActions.GIVE_BLOOD) == 0, "a refusal started the cooldown");
        } finally {
            creative.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            z.discard();
            pvp(helper, false);
            clean(giver, creative);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void everyVampireTypeCanGiveAndAHumanCannot(GameTestHelper helper) {
        TestPlayers.TalkingDuelist human = TestPlayers.createTalkingDuelist(helper);
        try {
            helper.assertTrue(!ACTION.available(human), "a human has the action");
            helper.assertTrue(GiveBloodAction.REFUSED.equals(key(ACTION.trigger(human, human))), "a human's trigger was not refused");
            for (Species s : new Species[] {Species.NONE, Species.HERETIC, Species.ORIGINAL, Species.ORIGINAL_HYBRID, Species.HYBRID, Species.TRIBRID, Species.AUGUSTINE}) {
                TestPlayers.TalkingDuelist v = vampire(helper, s);
                try {
                    helper.assertTrue(ACTION.available(v), s + " cannot give blood");
                } finally {
                    clean(v);
                }
            }
        } finally {
            clean(human);
        }
        helper.succeed();
    }

    // ---- no oracle ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theGiverHearsTheSameThingWhoeverTheTargetIs(GameTestHelper helper) {
        pvp(helper, true);
        TestPlayers.TalkingDuelist human = human(helper);
        TestPlayers.TalkingDuelist vampire = vampire(helper, Species.HYBRID);
        vampire.setPos(helper.absoluteVec(new Vec3(1.5, 1, 3.0)));
        TestPlayers.TalkingDuelist giverA = vampire(helper, Species.NONE);
        TestPlayers.TalkingDuelist giverB = vampire(helper, Species.NONE);
        try {
            human.setShiftKeyDown(true);
            vampire.setShiftKeyDown(true);
            helper.assertTrue(ACTION.trigger(giverA, human) == null && ACTION.trigger(giverB, vampire) == null, "one was refused");
            helper.assertTrue(marked(human) && !marked(vampire), "the blood did something to a vampire");
            helper.assertTrue(giverA.countOnActionBar(GiveBloodAction.DONE) == 1 && giverB.countOnActionBar(GiveBloodAction.DONE) == 1, "the done lines differ");
            helper.assertTrue(ActionClock.left(giverA, ExtraActions.GIVE_BLOOD) == ActionClock.left(giverB, ExtraActions.GIVE_BLOOD)
                    && ActionClock.left(giverA, ExtraActions.GIVE_BLOOD) > 0, "the cooldowns differ");
            helper.assertTrue(giverA.received() == giverB.received(), "the givers heard different amounts: " + giverA.received() + " and " + giverB.received());
        } finally {
            pvp(helper, false);
            clean(giverA, giverB, human, vampire);
        }
        helper.succeed();
    }

    // ---- the Original's blood ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anOriginalGiverNamesThePendingSireAndTheMessageUsesShownName(GameTestHelper helper) {
        pvp(helper, true);
        for (Species s : new Species[] {Species.ORIGINAL, Species.ORIGINAL_HYBRID}) {
            TestPlayers.TalkingDuelist giver = vampire(helper, s);
            TestPlayers.TalkingDuelist target = human(helper);
            try {
                target.setShiftKeyDown(true);
                helper.assertTrue(ACTION.trigger(giver, target) == null, s + ": the action was refused");
                helper.assertTrue(target.hasEffect(SpeciesRegistries.ORIGINAL_BLOOD_EFFECT) && giver.getUUID().equals(target.getData(SpeciesRegistries.BLOOD_SOURCE))
                        && s.name().equals(target.getData(SpeciesRegistries.BLOOD_SOURCE_SPECIES)), s + ": the blood is not the pending sire");
                Component said = target.message(BloodDrink.DRUNK_MARKED);
                helper.assertTrue(said != null && said.getContents() instanceof TranslatableContents t
                        && t.getArgs()[0] instanceof Component c && c.getString().equals(Lines.shownName(helper.getLevel().getServer(), target, giver.getUUID()).getString()),
                        s + ": the message does not name the source through shownName");
                target.hurt(helper.getLevel().damageSources().generic(), 1000f);
                helper.assertTrue(BloodlineAccess.get(target).inTurning() && giver.getUUID().equals(BloodlineAccess.get(target).pendingSire()), s + ": the pending sire is " + BloodlineAccess.get(target));
            } finally {
                clean(giver, target);
            }
        }
        pvp(helper, false);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPlainGiverNamesNoSire(GameTestHelper helper) {
        TestPlayers.TalkingDuelist giver = vampire(helper, Species.HYBRID);
        TestPlayers.TalkingDuelist target = human(helper);
        try {
            target.setShiftKeyDown(true);
            helper.assertTrue(ACTION.trigger(giver, target) == null && marked(target), "the action did not mark");
            helper.assertTrue(!target.hasEffect(SpeciesRegistries.ORIGINAL_BLOOD_EFFECT) && !target.received(BloodDrink.DRUNK) && !target.received(BloodDrink.DRUNK_MARKED), "a hybrid's blood named a sire");
        } finally {
            clean(giver, target);
        }
        helper.succeed();
    }

    // ---- the blood's gifts ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theBuffLastsWhileTheBloodDoes(GameTestHelper helper) {
        TestPlayers.TalkingDuelist p = human(helper);
        try {
            BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().vampireBloodUntil(helper.getLevel().getGameTime() + 1000).build());
            p.getFoodData().setExhaustion(1.0f);
            VampireBloodBuff.tick(p);
            var regen = p.getEffect(MobEffects.REGENERATION);
            helper.assertTrue(regen != null && regen.getAmplifier() == 1 && !regen.isVisible(), "no Regeneration II without particles: " + regen);
            helper.assertTrue(Math.abs(p.getFoodData().getExhaustionLevel() - 0.85f) < 0.001f, "exhaustion is " + p.getFoodData().getExhaustionLevel());
            p.removeAllEffects();
            p.getFoodData().setExhaustion(0.05f);
            VampireBloodBuff.tick(p);
            helper.assertTrue(p.getFoodData().getExhaustionLevel() == 0f, "exhaustion went below zero or was not eased");
            p.removeAllEffects();
            BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().vampireBloodUntil(helper.getLevel().getGameTime() - 1).build());
            p.getFoodData().setExhaustion(1.0f);
            VampireBloodBuff.tick(p);
            helper.assertTrue(!p.hasEffect(MobEffects.REGENERATION) && p.getFoodData().getExhaustionLevel() == 1.0f, "the buff outlived the blood");
            BloodlineAccess.set(p, BloodlineState.NONE);
            VampireBloodBuff.tick(p);
            helper.assertTrue(!p.hasEffect(MobEffects.REGENERATION), "someone with no blood got the buff");
        } finally {
            clean(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theBuffSwitchTurnsItOffAndABottleGivesIt(GameTestHelper helper) {
        TestPlayers.TalkingDuelist p = human(helper);
        try {
            SpeciesConfig.overrideVampireBloodBuff(false);
            BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().vampireBloodUntil(helper.getLevel().getGameTime() + 1000).build());
            VampireBloodBuff.tick(p);
            helper.assertTrue(!p.hasEffect(MobEffects.REGENERATION), "the switch did not turn the buff off");
            SpeciesConfig.overrideVampireBloodBuff(null);
            BloodlineAccess.set(p, BloodlineState.NONE);
            BloodBottles.apply(p, BloodBottles.UNSTAMPED); // a plain, unstamped bottle's blood
            VampireBloodBuff.tick(p);
            helper.assertTrue(p.hasEffect(MobEffects.REGENERATION), "the blood of a plain bottle gave no buff");
        } finally {
            SpeciesConfig.overrideVampireBloodBuff(null);
            clean(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theCooldownSurvivesDeath(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        ActionClock.start(p, ExtraActions.GIVE_BLOOD, 100);
        ServerPlayer reborn = TestPlayers.createMortal(helper);
        reborn.copyAttachmentsFrom(p, true);
        helper.assertTrue(ActionClock.left(reborn, ExtraActions.GIVE_BLOOD) == 100, "the reborn player's clock reads " + ActionClock.left(reborn, ExtraActions.GIVE_BLOOD));
        helper.assertTrue(ActionClock.ready(p, ExtraActions.BOTTLE_BLOOD), "one clock holds up another action");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSecondTryWaitsForTheCooldown(GameTestHelper helper) {
        TestPlayers.TalkingDuelist giver = vampire(helper, Species.NONE);
        TestPlayers.TalkingDuelist target = human(helper);
        try {
            target.setShiftKeyDown(true);
            helper.assertTrue(ACTION.trigger(giver, target) == null, "the first feed was refused");
            helper.assertTrue(GiveBloodAction.COOLDOWN.equals(key(ACTION.trigger(giver, target))), "a second feed came at once");
        } finally {
            clean(giver, target);
        }
        helper.succeed();
    }
}
