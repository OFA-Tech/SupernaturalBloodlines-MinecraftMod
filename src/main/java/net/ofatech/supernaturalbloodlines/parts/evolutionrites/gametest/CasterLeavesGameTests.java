package net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest;

import static net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvoTestSupport.atNight;
import static net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvoTestSupport.give;
import static net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvoTestSupport.has;
import static net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvoTestSupport.onTheCircle;
import static net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvoTestSupport.stacks;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.clearDropped;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.dropped;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.remove;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.start;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.type;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.util.EnergyHelper;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.EnabledParts;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.RecordingPlayer;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.mastery.MasteryChoice;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.RiteTargets;

/**
 * The performer leaves while the rite works (logs out, walks off): the one on the circle used to stand in for them, and a rite that changes the target asks
 * the target only when performer and target differ, so an unwilling target was changed without being asked. Now nobody stands in for someone else: the
 * rite cancels, which gives the ingredients back. Tests with a rite that really would have changed the target (so a revert fails them), for the two rites of
 * this part and the Rite of Augustine as the one of the older seven; the other six are held by the source scan (CasterFallbackScanTest).
 */
@PrefixGameTestTemplate(false)
public final class CasterLeavesGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final double EP = 5_000_000;

    private CasterLeavesGameTests() {
    }

    /** A witch performer who is not in the level at all: Enchanted finds nobody for the caster id. */
    private static RecordingPlayer gone(GameTestHelper helper, Species species) {
        RecordingPlayer p = RecordingPlayer.create(helper);
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(species).build());
        return p;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theGreatSageRiteDoesNotChangeAnUnsneakingTargetWhoseCasterLeft(GameTestHelper helper) {
        if (EvoTestSupport.skipped(helper, "evolve_great_sage")) {
            return;
        }
        RecordingPlayer caster = gone(helper, Species.WITCH);
        RecordingPlayer target = onTheCircle(helper, false, EP);
        BloodlineAccess.set(target, BloodlineState.NONE.toBuilder().species(Species.WITCH).build()); // a witch too: the old stand-in would have passed the witch check
        try {
            give(target, "tensura:sage", 500);
            clearDropped(helper);
            atNight(helper, true, () -> start(helper, "evolve_great_sage", caster, stacks("minecraft:amethyst_shard", 4, "minecraft:ender_eye", 2, "enchanted:spirit_of_otherwhere", 1)));
            helper.assertTrue(!has(target, "tensura:great_sage"), "an unsneaking target was evolved after the caster left");
            helper.assertTrue(EnergyHelper.getBaseMaxEP(target) == EP, "EP was spent: " + EnergyHelper.getBaseMaxEP(target));
            helper.assertTrue(dropped(helper, "minecraft:ender_eye") == 2, "the ingredients were not given back");
        } finally {
            clearDropped(helper);
            EvoTestSupport.forgetAll(target);
            remove(helper, target);
            BloodlineAccess.set(target, BloodlineState.NONE);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theMasteryRiteDoesNotChangeAnUnsneakingTargetWhoseCasterLeft(GameTestHelper helper) {
        if (EvoTestSupport.skipped(helper, "mastery_unique")) {
            return;
        }
        RecordingPlayer caster = gone(helper, Species.WITCH);
        RecordingPlayer target = onTheCircle(helper, false, EP);
        BloodlineAccess.set(target, BloodlineState.NONE.toBuilder().species(Species.WITCH).build());
        try {
            ManasSkillInstance inst = give(target, "tensura:anti_skill", 0);
            MasteryChoice.choose(target, ResourceLocation.parse("tensura:anti_skill"));
            clearDropped(helper);
            atNight(helper, true, () -> start(helper, "mastery_unique", caster, stacks("minecraft:nether_star", 5, "minecraft:netherite_ingot", 5)));
            helper.assertTrue(!inst.isMastered(target), "an unsneaking target had a skill mastered after the caster left");
            helper.assertTrue(EnergyHelper.getBaseMaxEP(target) == EP, "EP was spent: " + EnergyHelper.getBaseMaxEP(target));
            helper.assertTrue(dropped(helper, "minecraft:nether_star") == 5, "the ingredients were not given back");
            helper.assertTrue(MasteryChoice.lastRite(target) == -1, "the cooldown was started");
        } finally {
            MasteryChoice.clear(target);
            clearDropped(helper);
            EvoTestSupport.forgetAll(target);
            remove(helper, target);
            BloodlineAccess.set(target, BloodlineState.NONE);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theAugustineRiteDoesNotChangeAnUnsneakingVampireWhoseCasterLeft(GameTestHelper helper) {
        boolean speciesOn = EnabledParts.isEnabled("species");
        helper.assertTrue(speciesOn == (type(helper, "augustine") != null), "the species part is " + (speciesOn ? "on" : "off") + " and the Augustine rite is " + (type(helper, "augustine") != null ? "loaded" : "missing"));
        if (!speciesOn) {
            helper.succeed();
            return;
        }
        RecordingPlayer caster = gone(helper, Species.NONE);
        RecordingPlayer patient = onTheCircle(helper, false, EP);
        try {
            helper.assertTrue(VampirismAPI.factionPlayerHandler(patient).setFactionAndLevel(VReference.VAMPIRE_FACTION, 3), "could not make a vampire");
            clearDropped(helper);
            start(helper, "augustine", caster, List.of(new ItemStack(net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.item("vampirism:garlic"), 4),
                    new ItemStack(net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.item("vampirism:holy_water_bottle_normal")),
                    new ItemStack(net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.item("vampirism:human_heart")), new ItemStack(Items.GOLDEN_APPLE)));
            helper.assertTrue(BloodlineAccess.get(patient).species() == Species.NONE, "an unsneaking vampire became " + BloodlineAccess.get(patient).species() + " after the caster left");
            helper.assertTrue(dropped(helper, "vampirism:garlic") == 4, "the ingredients were not given back");
        } finally {
            clearDropped(helper);
            BloodlineAccess.set(patient, BloodlineState.NONE);
            VampirismAPI.factionPlayerHandler(patient).setFactionAndLevel(null, 0);
            remove(helper, patient);
        }
        helper.succeed();
    }

    /** The one case the stand-in is right for: the caster and the target are the same person (a rite worked on oneself). */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPerformerWhoIsTheTargetStillStandsForThemselves(GameTestHelper helper) {
        RecordingPlayer self = onTheCircle(helper, false, EP);
        try {
            helper.assertTrue(RiteTargets.performer(null, self.getUUID(), self).orElse(null) == self, "the target is the caster but did not stand for themselves");
            helper.assertTrue(RiteTargets.performer(null, java.util.UUID.randomUUID(), self).isEmpty(), "somebody stood in for a performer who is somebody else");
            helper.assertTrue(RiteTargets.performer(self, java.util.UUID.randomUUID(), self).orElse(null) == self, "a performer who was found was not used");
        } finally {
            remove(helper, self);
        }
        helper.succeed();
    }
}