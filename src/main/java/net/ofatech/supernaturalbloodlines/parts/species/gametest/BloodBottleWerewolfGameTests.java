package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.werewolves.api.WReference;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.blood.BloodBottleRules.Effect;
import net.ofatech.supernaturalbloodlines.core.blood.BloodKind;
import net.ofatech.supernaturalbloodlines.core.blood.BloodSource;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcOriginal;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.RecordingPlayer;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.item.BloodBottles;
import net.ofatech.supernaturalbloodlines.parts.species.item.BloodStamp;
import net.ofatech.supernaturalbloodlines.parts.species.item.OriginalBloodItem;

/** What a werewolf gets from a stamped bottle (R210). Needs Werewolves. {@code hybrid_transition_blood} is pinned to its default wherever a test depends on it. */
@PrefixGameTestTemplate(false)
public final class BloodBottleWerewolfGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final UUID MIKAEL = NpcOriginal.MIKAEL.uuid();
    private static final UUID KLAUS = NpcOriginal.KLAUS.uuid();

    private BloodBottleWerewolfGameTests() {
    }

    private static RecordingPlayer wolf(GameTestHelper helper) {
        RecordingPlayer p = RecordingPlayer.create(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(WReference.WEREWOLF_FACTION, 1), "could not make a werewolf");
        return p;
    }

    private static ItemStack bottle(BloodKind kind, UUID id) {
        ItemStack s = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("vampirism:vampire_blood_bottle")));
        BloodStamp.write(s, new BloodSource(id, "Source", false, kind));
        return s;
    }

    private static boolean click(ServerPlayer p) {
        var click = new PlayerInteractEvent.RightClickItem(p, InteractionHand.MAIN_HAND);
        NeoForge.EVENT_BUS.post(click);
        return click.isCanceled() && click.getCancellationResult().consumesAction();
    }

    private static void reset(ServerPlayer p) {
        BloodlineAccess.set(p, BloodlineState.NONE);
    }

    private static void inTransition(ServerPlayer p) {
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().inTransition(true).transitionTicksLeft(5000).build());
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void werewolfDrinksAStampedCommonBottleForNothing(GameTestHelper helper) {
        RecordingPlayer p = wolf(helper);
        try {
            p.setItemInHand(InteractionHand.MAIN_HAND, bottle(BloodKind.COMMON_VAMPIRE, UUID.randomUUID()));
            helper.assertTrue(click(p) && p.getMainHandItem().is(Items.GLASS_BOTTLE), "the bottle was not drunk");
            helper.assertTrue(p.receivedOnActionBar(BloodBottles.NO_EFFECT), "the werewolf was not told it did nothing");
            helper.assertTrue(!p.hasEffect(SpeciesRegistries.ORIGINAL_BLOOD_EFFECT) && BloodlineAccess.get(p).equals(BloodlineState.NONE), "it did something");
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("vampirism:vampire_blood_bottle"))));
            helper.assertTrue(click(p) && p.getMainHandItem().is(Items.GLASS_BOTTLE), "an unstamped bottle is not the same as a common one");
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void werewolfDrinksOriginalBloodOnlyWhenItStartsTheTransition(GameTestHelper helper) {
        SpeciesConfig.overrideTransitionBloodAnyOriginal(false);
        RecordingPlayer plain = wolf(helper);
        RecordingPlayer hybrid = wolf(helper);
        try {
            ItemStack plainOriginal = new ItemStack(SpeciesRegistries.ORIGINAL_BLOOD.get());
            BloodStamp.write(plainOriginal, new BloodSource(MIKAEL, "Mikael", false, BloodKind.ORIGINAL));
            plain.setItemInHand(InteractionHand.MAIN_HAND, plainOriginal);
            helper.assertTrue(!plainOriginal.use(helper.getLevel(), plain, InteractionHand.MAIN_HAND).getResult().consumesAction(), "a plain Original's blood started a drink");
            helper.assertTrue(plain.receivedOnActionBar(OriginalBloodItem.REFUSED) && plain.getMainHandItem() == plainOriginal, "the bottle was not kept and explained");

            ItemStack originalHybrid = new ItemStack(SpeciesRegistries.ORIGINAL_BLOOD.get());
            BloodStamp.write(originalHybrid, new BloodSource(KLAUS, "Klaus", true, BloodKind.ORIGINAL_HYBRID));
            hybrid.setItemInHand(InteractionHand.MAIN_HAND, originalHybrid);
            helper.assertTrue(originalHybrid.use(helper.getLevel(), hybrid, InteractionHand.MAIN_HAND).getResult().consumesAction(), "an Original hybrid's blood was refused");
            hybrid.setItemInHand(InteractionHand.MAIN_HAND, originalHybrid.finishUsingItem(helper.getLevel(), hybrid));
            helper.assertTrue(hybrid.getMainHandItem().is(Items.GLASS_BOTTLE) && KLAUS.equals(hybrid.getData(SpeciesRegistries.BLOOD_SOURCE)), "the blood was not drunk");
        } finally {
            SpeciesConfig.overrideTransitionBloodAnyOriginal(null);
            reset(plain);
            reset(hybrid);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aWerewolfRefusedAnOriginalKindVampirismBottleIsToldWhy(GameTestHelper helper) {
        SpeciesConfig.overrideTransitionBloodAnyOriginal(false);
        RecordingPlayer p = wolf(helper);
        try {
            ItemStack plainOriginal = bottle(BloodKind.ORIGINAL, MIKAEL);
            p.setItemInHand(InteractionHand.MAIN_HAND, plainOriginal);
            helper.assertTrue(!click(p) && p.getMainHandItem() == plainOriginal, "the bottle was used up");
            helper.assertTrue(p.receivedOnActionBar(OriginalBloodItem.REFUSED), "the werewolf was not told why: " + p.heard());
        } finally {
            SpeciesConfig.overrideTransitionBloodAnyOriginal(null);
            reset(p);
        }
        helper.succeed();
    }

    /** The bottle that saves a transition and the one that ruins it look alike ("Strange blood"); the rules tell them apart. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aWerewolfInTransitionIsSavedByAnAcceptedOriginalAndRuinedByAHybrid(GameTestHelper helper) {
        SpeciesConfig.overrideTransitionBloodAnyOriginal(false);
        RecordingPlayer saved = wolf(helper);
        RecordingPlayer ruined = wolf(helper);
        try {
            inTransition(saved);
            inTransition(ruined);
            ItemStack saving = bottle(BloodKind.ORIGINAL_HYBRID, KLAUS);
            ItemStack deadly = bottle(BloodKind.HYBRID, UUID.randomUUID());
            helper.assertTrue(BloodBottles.decide(saved, BloodBottles.sourceOf(helper.getLevel().getServer(), saving)) == Effect.TRANSITION_BLOOD, "an accepted Original's bottle does not hold the transition");
            helper.assertTrue(BloodBottles.decide(ruined, BloodBottles.sourceOf(helper.getLevel().getServer(), deadly)) == Effect.UNSTABLE, "a hybrid's bottle does not ruin the transition");
            saved.setItemInHand(InteractionHand.MAIN_HAND, saving);
            helper.assertTrue(click(saved) && !BloodlineAccess.get(saved).unstable() && KLAUS.equals(saved.getData(SpeciesRegistries.BLOOD_SOURCE)), "the saving bottle was not drunk as transition blood");
            ruined.setItemInHand(InteractionHand.MAIN_HAND, deadly);
            helper.assertTrue(click(ruined) && BloodlineAccess.get(ruined).unstable() && ruined.hasEffect(SpeciesRegistries.UNSTABLE_EFFECT), "the hybrid's bottle did not make the transition unstable");
        } finally {
            SpeciesConfig.overrideTransitionBloodAnyOriginal(null);
            reset(saved);
            reset(ruined);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void werewolfInTransitionGoesUnstableOnCommonBlood(GameTestHelper helper) {
        RecordingPlayer p = wolf(helper);
        try {
            inTransition(p);
            p.setItemInHand(InteractionHand.MAIN_HAND, bottle(BloodKind.COMMON_VAMPIRE, UUID.randomUUID()));
            helper.assertTrue(click(p), "the bottle was not drunk");
            helper.assertTrue(BloodlineAccess.get(p).unstable() && p.hasEffect(SpeciesRegistries.UNSTABLE_EFFECT), "other blood did not make the transition unstable: " + BloodlineAccess.get(p));
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void everyKindOfBloodReachesAWerewolfThroughItsDrinker(GameTestHelper helper) {
        SpeciesConfig.overrideTransitionBloodAnyOriginal(false);
        RecordingPlayer p = wolf(helper);
        RecordingPlayer inTransition = wolf(helper);
        try {
            inTransition(inTransition);
            helper.assertTrue(BloodBottles.drinkerOf(p) == net.ofatech.supernaturalbloodlines.core.blood.BloodBottleRules.Drinker.WEREWOLF, "a werewolf is not a werewolf drinker");
            helper.assertTrue(BloodBottles.drinkerOf(inTransition) == net.ofatech.supernaturalbloodlines.core.blood.BloodBottleRules.Drinker.WEREWOLF_IN_TRANSITION, "a werewolf in transition is not one");
            for (BloodKind k : new BloodKind[] {BloodKind.HYBRID, BloodKind.TRIBRID, BloodKind.HERETIC, BloodKind.COMMON_VAMPIRE}) {
                BloodSource s = new BloodSource(UUID.randomUUID(), "x", false, k);
                helper.assertTrue(BloodBottles.decide(p, s) == Effect.NO_EFFECT, k + ": a werewolf's drink is " + BloodBottles.decide(p, s));
                helper.assertTrue(BloodBottles.decide(inTransition, s) == Effect.UNSTABLE, k + ": a werewolf in transition's drink is " + BloodBottles.decide(inTransition, s));
            }
        } finally {
            SpeciesConfig.overrideTransitionBloodAnyOriginal(null);
            reset(p);
            reset(inTransition);
        }
        helper.succeed();
    }
}
