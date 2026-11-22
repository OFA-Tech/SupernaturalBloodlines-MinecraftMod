package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.centre;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.clearDropped;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.dropped;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.item;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.place;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.remove;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.start;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.type;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.RecordingPlayer;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.item.AugustineSyringeItem;

/** Phase 18c Task 5 (R201, R202): the Rite of Augustine and Wes's syringe. */
@PrefixGameTestTemplate(false)
public final class AugustineMakingGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private AugustineMakingGameTests() {
    }

    private static List<ItemStack> ingredients() {
        return List.of(new ItemStack(item("vampirism:garlic"), 4), new ItemStack(item("vampirism:holy_water_bottle_normal")),
                new ItemStack(item("vampirism:human_heart")), new ItemStack(Items.GOLDEN_APPLE));
    }

    private static boolean noRite(GameTestHelper helper) {
        if (type(helper, "augustine") == null) {
            helper.succeed(); // the rite's data needs Enchanted and the species part
            return true;
        }
        return false;
    }

    private static void vampire(GameTestHelper helper, ServerPlayer p) {
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 3), "could not make a vampire");
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void riteDataLoads(GameTestHelper helper) {
        if (noRite(helper)) {
            return;
        }
        var type = type(helper, "augustine");
        helper.assertTrue(type.getPower() == 16000, "power is " + type.getPower());
        helper.assertTrue(type.getItems().size() == 4, "the ingredients: " + type.getItems());
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theRiteMakesTheCircleCentreAnAugustine(GameTestHelper helper) {
        if (noRite(helper)) {
            return;
        }
        RecordingPlayer caster = RecordingPlayer.create(helper);
        RecordingPlayer patient = RecordingPlayer.create(helper);
        caster.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(centre(helper)).add(5, 1, 0));
        helper.getLevel().addNewPlayer(caster);
        place(helper, patient);
        try {
            vampire(helper, caster);
            vampire(helper, patient);
            clearDropped(helper);
            start(helper, "augustine", caster, ingredients());
            helper.assertTrue(BloodlineAccess.get(patient).species() == Species.AUGUSTINE, "the one on the centre is " + BloodlineAccess.get(patient).species());
            helper.assertTrue(BloodlineAccess.get(caster).species() == Species.NONE, "the caster, standing apart, changed");
            helper.assertTrue(VampirismAPI.factionPlayerHandler(patient).getCurrentFaction() == VReference.VAMPIRE_FACTION, "an Augustine left the vampires");
            helper.assertTrue(dropped(helper, "vampirism:garlic") == 0, "a good rite gave the ingredients back");
        } finally {
            remove(helper, caster);
            remove(helper, patient);
            BloodlineAccess.set(caster, BloodlineState.NONE);
            BloodlineAccess.set(patient, BloodlineState.NONE);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theRiteRefusesAWitchVampireAGhoulAndAnyoneWhoIsNotAVampire(GameTestHelper helper) {
        if (noRite(helper)) {
            return;
        }
        RecordingPlayer p = RecordingPlayer.create(helper);
        place(helper, p);
        try {
            // a witch layer: the existing refusal path, ingredients returned
            vampire(helper, p);
            BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().witchLayer(true).build());
            clearDropped(helper);
            start(helper, "augustine", p, ingredients());
            helper.assertTrue(BloodlineAccess.get(p).species() == Species.NONE && BloodlineAccess.get(p).witchLayer(), "a witch vampire became " + BloodlineAccess.get(p).species());
            helper.assertTrue(dropped(helper, "vampirism:garlic") == 4, "the ingredients were not given back: " + dropped(helper, "vampirism:garlic"));
            clearDropped(helper);
            // a hybrid is not "plain"
            BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(Species.HYBRID).build());
            start(helper, "augustine", p, ingredients());
            helper.assertTrue(BloodlineAccess.get(p).species() == Species.HYBRID, "a hybrid became " + BloodlineAccess.get(p).species());
            helper.assertTrue(dropped(helper, "vampirism:garlic") == 4, "the hybrid's ingredients were not given back");
            clearDropped(helper);
            // a Ghoul
            BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().ghoul(true).build());
            start(helper, "augustine", p, ingredients());
            helper.assertTrue(BloodlineAccess.get(p).species() == Species.NONE, "a Ghoul became " + BloodlineAccess.get(p).species());
            helper.assertTrue(dropped(helper, "vampirism:garlic") == 4, "the Ghoul's ingredients were not given back");
            clearDropped(helper);
            // a human
            BloodlineAccess.set(p, BloodlineState.NONE);
            VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(null, 0);
            start(helper, "augustine", p, ingredients());
            helper.assertTrue(BloodlineAccess.get(p).species() == Species.NONE, "a human became " + BloodlineAccess.get(p).species());
            helper.assertTrue(dropped(helper, "vampirism:garlic") == 4, "the human's ingredients were not given back");
        } finally {
            clearDropped(helper);
            remove(helper, p);
            BloodlineAccess.set(p, BloodlineState.NONE);
        }
        helper.succeed();
    }

    private static ItemStack syringe(int n) {
        return new ItemStack(SpeciesRegistries.AUGUSTINE_SYRINGE.get(), n);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theSyringeMakesAPlainVampireAnAugustine(GameTestHelper helper) {
        TestPlayers.TalkingDuelist p = TestPlayers.createTalkingDuelist(helper);
        try {
            vampire(helper, p);
            p.setShiftKeyDown(true);
            ItemStack stack = syringe(2);
            p.setItemInHand(InteractionHand.MAIN_HAND, stack);
            stack.getItem().use(helper.getLevel(), p, InteractionHand.MAIN_HAND);
            helper.assertTrue(BloodlineAccess.get(p).species() == Species.AUGUSTINE, "the species is " + BloodlineAccess.get(p).species());
            helper.assertTrue(stack.getCount() == 1, "one syringe was not used up: " + stack.getCount());
            helper.assertTrue(p.received(AugustineSyringeItem.DONE), "no word that it worked");
            // a second use does nothing and keeps the syringe
            p.removeEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE);
            stack.getItem().use(helper.getLevel(), p, InteractionHand.MAIN_HAND);
            helper.assertTrue(stack.getCount() == 1 && p.receivedOnActionBar(AugustineSyringeItem.NOTHING), "an Augustine used a second syringe: " + stack.getCount());
        } finally {
            p.setShiftKeyDown(false);
            BloodlineAccess.set(p, BloodlineState.NONE);
        }
        helper.succeed();
    }

    /** Whatever the reason, the same one line, and the syringe is kept. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void theSyringeSaysNothingHappenedWhateverTheReason(GameTestHelper helper) {
        for (int reason = 0; reason < 6; reason++) {
            TestPlayers.TalkingDuelist p = TestPlayers.createTalkingDuelist(helper);
            try {
                switch (reason) {
                    case 0 -> BloodlineAccess.set(p, BloodlineState.NONE); // a human
                    case 1 -> {
                        vampire(helper, p);
                        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(Species.ORIGINAL).build());
                    }
                    case 2 -> {
                        vampire(helper, p);
                        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(Species.HERETIC).build());
                    }
                    case 3 -> {
                        vampire(helper, p);
                        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().ghoul(true).build());
                    }
                    case 4 -> {
                        vampire(helper, p);
                        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().witchLayer(true).build());
                    }
                    default -> {
                        vampire(helper, p);
                        p.setShiftKeyDown(false); // a plain vampire who does not sneak
                    }
                }
                p.setShiftKeyDown(reason != 5);
                ItemStack stack = syringe(1);
                stack.getItem().use(helper.getLevel(), p, InteractionHand.MAIN_HAND);
                helper.assertTrue(stack.getCount() == 1, "reason " + reason + ": the syringe was used up");
                helper.assertTrue(p.receivedOnActionBar(AugustineSyringeItem.NOTHING), "reason " + reason + ": not the one line");
                helper.assertTrue(BloodlineAccess.get(p).species() != Species.AUGUSTINE, "reason " + reason + ": became an Augustine");
            } finally {
                p.setShiftKeyDown(false);
                BloodlineAccess.set(p, BloodlineState.NONE);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theSyringeOnAnotherPlayerNeedsTheirSneak(GameTestHelper helper) {
        TestPlayers.TalkingDuelist user = TestPlayers.createTalkingDuelist(helper);
        TestPlayers.TalkingDuelist target = TestPlayers.createTalkingDuelist(helper);
        try {
            vampire(helper, target);
            ItemStack stack = syringe(1);
            user.setItemInHand(InteractionHand.MAIN_HAND, stack);
            target.setShiftKeyDown(false);
            stack.getItem().interactLivingEntity(stack, user, target, InteractionHand.MAIN_HAND);
            helper.assertTrue(BloodlineAccess.get(target).species() == Species.NONE && stack.getCount() == 1, "an unwilling vampire was injected");
            helper.assertTrue(user.receivedOnActionBar(AugustineSyringeItem.NOTHING), "no line for the one who refused");
            target.setShiftKeyDown(true);
            stack.getItem().interactLivingEntity(stack, user, target, InteractionHand.MAIN_HAND);
            helper.assertTrue(BloodlineAccess.get(target).species() == Species.AUGUSTINE, "a consenting vampire was not injected");
            helper.assertTrue(stack.isEmpty(), "the syringe was not used up: " + stack.getCount());
            helper.assertTrue(target.received(AugustineSyringeItem.DONE), "the target was not told");
        } finally {
            target.setShiftKeyDown(false);
            BloodlineAccess.set(target, BloodlineState.NONE);
        }
        helper.succeed();
    }
}
