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
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.FactionRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.RiteTargets;

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
            patient.setShiftKeyDown(true); // their consent
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

    private static void makeKind(GameTestHelper helper, ServerPlayer p, int kind) {
        VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(null, 0);
        BloodlineAccess.set(p, BloodlineState.NONE);
        switch (kind) {
            case 0 -> { } // a human
            case 1 -> vampire(helper, p); // the one it works on: a plain vampire
            case 2 -> {
                vampire(helper, p);
                BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(Species.HYBRID).build());
            }
            case 3 -> {
                vampire(helper, p);
                BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(Species.ORIGINAL).build());
            }
            case 4 -> {
                vampire(helper, p);
                BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().ghoul(true).build());
            }
            case 5 -> {
                vampire(helper, p);
                BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(Species.AUGUSTINE).build());
            }
            case 6 -> {
                vampire(helper, p);
                BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().witchLayer(true).build());
            }
            case 9 -> {
                vampire(helper, p);
                BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(Species.HERETIC).build());
            }
            case 8 -> helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.HUNTER_FACTION, 1), "could not make a hunter");
            default -> BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(Species.WITCH).build());
        }
    }

    /**
     * The caster of a rite who is not the target hears one thing whatever the target is (human, hybrid, Original, Ghoul, an Augustine, a witch), and the target is
     * asked to sneak first. Only the one who works the rite on themselves, or whose target agreed AND is a plain vampire, gets anything done.
     */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void theCasterOfTheRiteLearnsNothingAboutWhoStandsOnTheCircle(GameTestHelper helper) {
        if (noRite(helper)) {
            return;
        }
        String first = null;
        for (int kind = 0; kind <= 7; kind++) {
            RecordingPlayer caster = RecordingPlayer.create(helper);
            RecordingPlayer target = RecordingPlayer.create(helper);
            caster.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(centre(helper)).add(5, 1, 0));
            helper.getLevel().addNewPlayer(caster);
            place(helper, target);
            try {
                makeKind(helper, target, kind);
                target.setShiftKeyDown(true);
                clearDropped(helper);
                start(helper, "augustine", caster, ingredients());
                if (kind == 1) {
                    helper.assertTrue(BloodlineAccess.get(target).species() == Species.AUGUSTINE, "the plain vampire who agreed was not made one");
                    helper.assertTrue(target.received(FactionRiteFactory.RITE_TAKES_YOU), "the one who agreed was not told the rite took them: " + target.heard());
                    continue;
                }
                helper.assertTrue(BloodlineAccess.get(target).species() != Species.AUGUSTINE || kind == 5, "kind " + kind + " became an Augustine");
                String heard = caster.heard();
                helper.assertTrue(heard.equals(FactionRiteFactory.NO_EFFECT), "kind " + kind + ": the caster heard " + heard);
                if (first == null) {
                    first = heard;
                }
                helper.assertTrue(heard.equals(first), "kind " + kind + ": the caster heard something else than for another kind");
                helper.assertTrue(dropped(helper, "vampirism:garlic") == 4, "kind " + kind + ": the ingredients were not given back");
            } finally {
                clearDropped(helper);
                remove(helper, caster);
                remove(helper, target);
                BloodlineAccess.set(target, BloodlineState.NONE);
            }
        }
        // and a target who does not sneak: the same one answer, nothing changes, and they are asked
        RecordingPlayer caster = RecordingPlayer.create(helper);
        RecordingPlayer target = RecordingPlayer.create(helper);
        caster.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(centre(helper)).add(5, 1, 0));
        helper.getLevel().addNewPlayer(caster);
        place(helper, target);
        try {
            vampire(helper, target);
            target.setShiftKeyDown(false);
            clearDropped(helper);
            start(helper, "augustine", caster, ingredients());
            helper.assertTrue(BloodlineAccess.get(target).species() == Species.NONE, "an unwilling vampire was made an Augustine");
            helper.assertTrue(caster.heard().equals(FactionRiteFactory.NO_EFFECT), "the caster heard " + caster.heard() + " of an unwilling target");
            helper.assertTrue(target.receivedOnActionBar(RiteTargets.SNEAK_TO_BE_MADE_AUGUSTINE), "the target was not asked to sneak");
            helper.assertTrue(dropped(helper, "vampirism:garlic") == 4, "the unwilling target's ingredients were not given back");
        } finally {
            clearDropped(helper);
            remove(helper, caster);
            remove(helper, target);
            BloodlineAccess.set(target, BloodlineState.NONE);
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

    private static void use(ItemStack stack, ServerPlayer user, ServerPlayer target) {
        stack.getItem().interactLivingEntity(stack, user, target, InteractionHand.MAIN_HAND);
    }

    /** On another player: they must sneak with a lead in hand and PvP must be on (as a link), and it takes two uses within 5 seconds. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void theSyringeOnAnotherPlayerNeedsTheirConsentAndASecondUse(GameTestHelper helper) {
        TestPlayers.TalkingDuelist user = TestPlayers.createTalkingDuelist(helper);
        TestPlayers.TalkingDuelist target = TestPlayers.createTalkingDuelist(helper);
        boolean pvp = helper.getLevel().getServer().isPvpAllowed();
        AugustineSyringeItem.forgetArmedForTest();
        try {
            vampire(helper, target);
            ItemStack stack = syringe(1);
            user.setItemInHand(InteractionHand.MAIN_HAND, stack);
            helper.getLevel().getServer().setPvpAllowed(true);
            // each part of the consent missing: the same one line, nothing armed, nothing changed (a fresh user each time: the line is read off the action bar)
            for (int missing = 0; missing < 3; missing++) {
                user = TestPlayers.createTalkingDuelist(helper);
                user.setItemInHand(InteractionHand.MAIN_HAND, stack);
                target.setShiftKeyDown(missing != 0);
                target.setItemInHand(InteractionHand.MAIN_HAND, missing == 1 ? ItemStack.EMPTY : new ItemStack(net.minecraft.world.item.Items.LEAD));
                helper.getLevel().getServer().setPvpAllowed(missing != 2);
                use(stack, user, target);
                use(stack, user, target); // a second use would confirm an arming: there must be none
                helper.assertTrue(user.receivedOnActionBar(AugustineSyringeItem.NOTHING) && !user.receivedOnActionBar(AugustineSyringeItem.ARMED), "missing " + missing + ": not the one line, or it armed");
                helper.assertTrue(BloodlineAccess.get(target).species() == Species.NONE && stack.getCount() == 1, "missing " + missing + ": an unwilling vampire was injected");
            }
            // willing: the first use arms and tells them, the second injects
            target.setShiftKeyDown(true);
            target.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.LEAD));
            helper.getLevel().getServer().setPvpAllowed(true);
            use(stack, user, target);
            helper.assertTrue(BloodlineAccess.get(target).species() == Species.NONE && stack.getCount() == 1, "the first use injected");
            helper.assertTrue(user.receivedOnActionBar(AugustineSyringeItem.ARMED) && target.receivedOnActionBar(AugustineSyringeItem.ASKED), "the first use did not arm and ask");
            // the use key held down repeats within a few ticks: that is not a second decision
            AugustineSyringeItem.skipTicksForTest(4);
            use(stack, user, target);
            helper.assertTrue(BloodlineAccess.get(target).species() == Species.NONE && stack.getCount() == 1, "a use 4 ticks later injected (the use key held down)");
            AugustineSyringeItem.skipTicksForTest(net.ofatech.supernaturalbloodlines.core.bloodline.SyringeConfirm.MIN_TICKS);
            use(stack, user, target);
            helper.assertTrue(BloodlineAccess.get(target).species() == Species.AUGUSTINE, "the second use did not inject");
            helper.assertTrue(stack.isEmpty(), "the syringe was not used up: " + stack.getCount());
            helper.assertTrue(target.received(AugustineSyringeItem.DONE), "the target was not told");
        } finally {
            helper.getLevel().getServer().setPvpAllowed(pvp);
            AugustineSyringeItem.forgetArmedForTest();
            target.setShiftKeyDown(false);
            BloodlineAccess.set(target, BloodlineState.NONE);
        }
        helper.succeed();
    }

    /** A dying (downed) player cannot use it: Downed cancels the click before the item sees it. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aDownedPlayerCannotUseTheSyringe(GameTestHelper helper) {
        ServerPlayer user = TestPlayers.createMortal(helper);
        ServerPlayer target = TestPlayers.createTalkingDuelist(helper);
        try {
            vampire(helper, target);
            BloodlineAccess.set(user, BloodlineState.NONE.toBuilder().vampireBloodUntil(helper.getLevel().getGameTime() + 100000).build());
            user.hurt(helper.getLevel().damageSources().generic(), 1000f);
            helper.assertTrue(net.ofatech.supernaturalbloodlines.parts.species.original.Downed.isDowned(user), "setup: not downed");
            user.setItemInHand(InteractionHand.MAIN_HAND, syringe(1));
            var onPlayer = new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract(user, InteractionHand.MAIN_HAND, target);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(onPlayer);
            var inAir = new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem(user, InteractionHand.MAIN_HAND);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(inAir);
            helper.assertTrue(onPlayer.isCanceled() && inAir.isCanceled(), "a downed player''s click reached the syringe: on a player " + onPlayer.isCanceled() + ", in the air " + inAir.isCanceled());
        } finally {
            BloodlineAccess.set(user, BloodlineState.NONE);
        }
        helper.succeed();
    }
    private static List<ItemStack> lineageItems() {
        return List.of(new ItemStack(item("supernaturalbloodlines:moonstone")), new ItemStack(Items.BONE, 3));
    }

    /**
     * The Rite of Lineage: Awaken changes the one on the centre: when someone else works it they must sneak, and whatever they are (an Augustine, a human, a
     * hybrid) the caster hears the same thing. The target hears the reason.
     */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void theLineageRiteTellsItsCasterTheSameWhateverStandsOnTheCircle(GameTestHelper helper) {
        if (type(helper, "rite_of_lineage_awaken") == null) {
            helper.succeed(); // needs Werewolves and the species part
            return;
        }
        for (int kind = 0; kind < 4; kind++) {
            RecordingPlayer caster = RecordingPlayer.create(helper);
            RecordingPlayer target = RecordingPlayer.create(helper);
            caster.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(centre(helper)).add(5, 1, 0));
            helper.getLevel().addNewPlayer(caster);
            place(helper, target);
            try {
                makeKind(helper, target, kind == 0 ? 5 : kind == 1 ? 8 : kind == 2 ? 2 : 9); // an Augustine, a hunter, a hybrid, a heretic
                target.setShiftKeyDown(true);
                clearDropped(helper);
                start(helper, "rite_of_lineage_awaken", caster, lineageItems());
                helper.assertTrue(caster.heard().equals(FactionRiteFactory.NO_EFFECT), "kind " + kind + ": the caster heard " + caster.heard());
                helper.assertTrue(BloodlineAccess.get(target).species() != Species.AUGUSTINE || kind == 0, "kind " + kind + ": changed species");
                helper.assertTrue(!target.heard().isEmpty(), "kind " + kind + ": the target was told nothing");
                // not sneaking: the same one line for the caster, and the target is asked
                RecordingPlayer caster2 = RecordingPlayer.create(helper);
                caster2.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(centre(helper)).add(5, 1, 0));
                helper.getLevel().addNewPlayer(caster2);
                RecordingPlayer asked = target;
                target.setShiftKeyDown(false);
                start(helper, "rite_of_lineage_awaken", caster2, lineageItems());
                helper.assertTrue(caster2.heard().equals(FactionRiteFactory.NO_EFFECT), "kind " + kind + " not sneaking: the caster heard " + caster2.heard());
                helper.assertTrue(asked.received(RiteTargets.SNEAK_TO_ACCEPT), "kind " + kind + ": the target was not asked to sneak");
                remove(helper, caster2);
            } finally {
                clearDropped(helper);
                remove(helper, caster);
                remove(helper, target);
                BloodlineAccess.set(target, BloodlineState.NONE);
            }
        }
        helper.succeed();
    }

    /** Ancestral Calling refuses a vampire or someone who already has a species: the caster hears the same line for both, the one on the circle the reason. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void ancestralCallingTellsItsCasterNothingOfWhoStandsOnTheCircle(GameTestHelper helper) {
        if (type(helper, "ancestral_calling") == null) {
            helper.succeed();
            return;
        }
        for (int kind : new int[] {5, 2, 3}) { // an Augustine, a hybrid, an Original
            RecordingPlayer caster = RecordingPlayer.create(helper);
            RecordingPlayer target = RecordingPlayer.create(helper);
            caster.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(centre(helper)).add(5, 1, 0));
            helper.getLevel().addNewPlayer(caster);
            place(helper, target);
            try {
                makeKind(helper, target, kind);
                clearDropped(helper);
                start(helper, "ancestral_calling", caster, List.of(new ItemStack(Items.BONE), new ItemStack(Items.AMETHYST_SHARD), new ItemStack(Items.LAPIS_LAZULI)));
                helper.assertTrue(caster.heard().equals(FactionRiteFactory.NO_EFFECT), "kind " + kind + ": the caster heard " + caster.heard());
                helper.assertTrue(!target.heard().isEmpty() && !target.heard().equals(FactionRiteFactory.NO_EFFECT), "kind " + kind + ": the target was not told why: " + target.heard());
            } finally {
                clearDropped(helper);
                remove(helper, caster);
                remove(helper, target);
                BloodlineAccess.set(target, BloodlineState.NONE);
            }
        }
        helper.succeed();
    }}
