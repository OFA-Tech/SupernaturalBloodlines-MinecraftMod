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
import net.ofatech.supernaturalbloodlines.core.blood.BloodKind;
import net.ofatech.supernaturalbloodlines.core.blood.BloodSource;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.RecordingPlayer;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.item.BloodBottles;
import net.ofatech.supernaturalbloodlines.parts.species.item.BloodStamp;
import net.ofatech.supernaturalbloodlines.parts.species.item.OriginalBloodItem;

/** What a werewolf gets from a stamped bottle (R210). Needs Werewolves. */
@PrefixGameTestTemplate(false)
public final class BloodBottleWerewolfGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private BloodBottleWerewolfGameTests() {
    }

    private static RecordingPlayer wolf(GameTestHelper helper) {
        RecordingPlayer p = RecordingPlayer.create(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(WReference.WEREWOLF_FACTION, 1), "could not make a werewolf");
        return p;
    }

    private static ItemStack bottle(BloodKind kind) {
        ItemStack s = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("vampirism:vampire_blood_bottle")));
        BloodStamp.write(s, new BloodSource(UUID.randomUUID(), "Bob", false, kind));
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

    @GameTest(template = "empty", templateNamespace = NS)
    public static void werewolfDrinksAStampedCommonBottleForNothing(GameTestHelper helper) {
        RecordingPlayer p = wolf(helper);
        try {
            p.setItemInHand(InteractionHand.MAIN_HAND, bottle(BloodKind.COMMON_VAMPIRE));
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
        RecordingPlayer plain = wolf(helper);
        RecordingPlayer hybrid = wolf(helper);
        try {
            ItemStack plainOriginal = new ItemStack(SpeciesRegistries.ORIGINAL_BLOOD.get());
            BloodStamp.write(plainOriginal, new BloodSource(UUID.randomUUID(), "Elena", false, BloodKind.ORIGINAL));
            plain.setItemInHand(InteractionHand.MAIN_HAND, plainOriginal);
            helper.assertTrue(!plainOriginal.use(helper.getLevel(), plain, InteractionHand.MAIN_HAND).getResult().consumesAction(), "a plain Original's blood started a drink");
            helper.assertTrue(plain.receivedOnActionBar(OriginalBloodItem.REFUSED) && plain.getMainHandItem() == plainOriginal, "the bottle was not kept and explained");

            ItemStack originalHybrid = new ItemStack(SpeciesRegistries.ORIGINAL_BLOOD.get());
            UUID klaus = UUID.randomUUID();
            BloodStamp.write(originalHybrid, new BloodSource(klaus, "Klaus", false, BloodKind.ORIGINAL_HYBRID));
            hybrid.setItemInHand(InteractionHand.MAIN_HAND, originalHybrid);
            helper.assertTrue(originalHybrid.use(helper.getLevel(), hybrid, InteractionHand.MAIN_HAND).getResult().consumesAction(), "an Original hybrid's blood was refused");
            hybrid.setItemInHand(InteractionHand.MAIN_HAND, originalHybrid.finishUsingItem(helper.getLevel(), hybrid));
            helper.assertTrue(hybrid.getMainHandItem().is(Items.GLASS_BOTTLE) && klaus.equals(hybrid.getData(SpeciesRegistries.BLOOD_SOURCE)), "the blood was not drunk");
        } finally {
            reset(plain);
            reset(hybrid);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void werewolfInTransitionGoesUnstableOnCommonBlood(GameTestHelper helper) {
        RecordingPlayer p = wolf(helper);
        try {
            BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().inTransition(true).transitionTicksLeft(5000).build());
            p.setItemInHand(InteractionHand.MAIN_HAND, bottle(BloodKind.COMMON_VAMPIRE));
            helper.assertTrue(click(p), "the bottle was not drunk");
            helper.assertTrue(BloodlineAccess.get(p).unstable() && p.hasEffect(SpeciesRegistries.UNSTABLE_EFFECT), "other blood did not make the transition unstable: " + BloodlineAccess.get(p));
        } finally {
            reset(p);
        }
        helper.succeed();
    }
}
