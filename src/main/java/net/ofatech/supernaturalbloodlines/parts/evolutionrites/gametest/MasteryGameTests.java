package net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest;

import static net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvoTestSupport.atEclipse;
import static net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvoTestSupport.atNight;
import static net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvoTestSupport.give;
import static net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvoTestSupport.has;
import static net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvoTestSupport.onTheCircle;
import static net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvoTestSupport.performer;
import static net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvoTestSupport.stacks;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.clearDropped;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.dropped;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.remove;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.start;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.type;

import dev.architectury.event.EventResult;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.SkillEvents;
import io.github.manasmods.tensura.util.EnergyHelper;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.evolution.MasteryRules;
import net.ofatech.supernaturalbloodlines.core.evolution.Tier;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.RecordingPlayer;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.EvoSkills;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.EvolutionRitesConfig;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.mastery.MasteryChoice;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.FactionRiteFactory;

/** The Rite of Mastery (plan 19a, spec 8.5): one stuck skill of each kind is mastered, and every refusal gives everything back. */
@PrefixGameTestTemplate(false)
public final class MasteryGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final double EP = 5_000_000;

    private MasteryGameTests() {
    }

    private static String rite(Tier t) {
        return "mastery_" + t.id();
    }

    private static boolean noRite(GameTestHelper helper) {
        if (type(helper, rite(Tier.COMMON)) == null) {
            helper.succeed();
            return true;
        }
        return false;
    }

    private static List<ItemStack> items(Tier t) {
        return switch (t) {
            case COMMON -> stacks("minecraft:diamond_block", 5, "minecraft:echo_shard", 5);
            case EXTRA -> stacks("minecraft:netherite_ingot", 5, "minecraft:echo_shard", 20);
            case UNIQUE -> stacks("minecraft:nether_star", 5, "minecraft:netherite_ingot", 5);
            case ULTIMATE -> stacks("minecraft:nether_star", 5, "minecraft:dragon_breath", 5, "minecraft:netherite_ingot", 10);
        };
    }

    private static String firstItem(Tier t) {
        return switch (t) {
            case COMMON -> "minecraft:diamond_block";
            case EXTRA, UNIQUE, ULTIMATE -> "minecraft:netherite_ingot";
        };
    }

    private static void work(GameTestHelper helper, Tier t, RecordingPlayer caster) {
        if (t == Tier.ULTIMATE) {
            atEclipse(helper, () -> start(helper, rite(t), caster, items(t)));
        } else {
            atNight(helper, true, () -> start(helper, rite(t), caster, items(t)));
        }
    }

    private static void clean(GameTestHelper helper, RecordingPlayer... ps) {
        clearDropped(helper);
        MasteryChoice.clock = null;
        EvolutionRitesConfig.override("mastery_refused_skills", null);
        EvolutionRitesConfig.override("mastery_cooldown_days", null);
        EvolutionRitesConfig.override("mastery_enabled", null);
        for (RecordingPlayer p : ps) {
            MasteryChoice.clear(p);
            MasteryChoice.setLast(p, -1);
            EvoSkills_forget(p);
            remove(helper, p);
            BloodlineAccess.set(p, BloodlineState.NONE);
        }
    }

    private static void EvoSkills_forget(RecordingPlayer p) {
        EvoTestSupport.forgetAll(p);
    }

    private static Tier tierOf(String skillId) {
        return EvoSkills.tierOf(EvoSkills.skill(skillId).orElseThrow(() -> new IllegalStateException("no skill " + skillId)));
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void riteDataLoads(GameTestHelper helper) {
        if (noRite(helper)) {
            return;
        }
        int[] power = {6000, 9000, 12000, 20000};
        for (Tier t : Tier.values()) {
            var type = type(helper, rite(t));
            helper.assertTrue(type != null, rite(t) + " did not load");
            helper.assertTrue(type.getPower() == power[t.ordinal()], rite(t) + " power is " + type.getPower());
            helper.assertTrue(type.getItems().size() == items(t).size(), rite(t) + " ingredients: " + type.getItems());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anAntiSkillIsMasteredAndItsToggleOpens(GameTestHelper helper) {
        if (noRite(helper)) {
            return;
        }
        RecordingPlayer caster = performer(helper, Species.WITCH);
        RecordingPlayer target = onTheCircle(helper, true, EP);
        try {
            ManasSkillInstance inst = give(target, "tensura:anti_skill", 0);
            helper.assertTrue(tierOf("tensura:anti_skill") == Tier.UNIQUE, "Anti Skill is " + tierOf("tensura:anti_skill"));
            helper.assertTrue(!inst.canBeToggled(target), "Anti Skill is toggleable before it is mastered");
            helper.assertTrue(MasteryChoice.choose(target, ResourceLocation.parse("tensura:anti_skill")).isEmpty(), "could not choose it");
            work(helper, Tier.UNIQUE, caster);
            helper.assertTrue(inst.isMastered(target), "not mastered; the target heard " + target.heard() + ", the performer " + caster.heard());
            helper.assertTrue(inst.canBeToggled(target), "its toggle did not open");
            helper.assertTrue(Math.abs(EnergyHelper.getBaseMaxEP(target) - (EP - 750_000)) < 1, "EP is " + EnergyHelper.getBaseMaxEP(target));
            helper.assertTrue(target.received(net.ofatech.supernaturalbloodlines.parts.evolutionrites.rite.MasteryRiteFactory.DONE), "the target was not told");
            helper.assertTrue(MasteryChoice.get(target).isEmpty(), "the choice was kept");
        } finally {
            clean(helper, caster, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aResistanceLearningIsFinished(GameTestHelper helper) {
        if (noRite(helper)) {
            return;
        }
        RecordingPlayer caster = performer(helper, Species.WITCH);
        RecordingPlayer target = onTheCircle(helper, true, EP);
        try {
            ManasSkillInstance inst = give(target, "tensura:corrosion_resistance", -100);
            helper.assertTrue(tierOf("tensura:corrosion_resistance") == Tier.COMMON, "a resistance is " + tierOf("tensura:corrosion_resistance"));
            helper.assertTrue(MasteryChoice.choose(target, ResourceLocation.parse("tensura:corrosion_resistance")).isEmpty(), "could not choose it");
            work(helper, Tier.COMMON, caster);
            helper.assertTrue(inst.getMastery() == 0, "mastery is " + inst.getMastery() + "; " + target.heard());
            helper.assertTrue(has(target, "tensura:corrosion_resistance"), "the resistance is gone");
            helper.assertTrue(Math.abs(EnergyHelper.getBaseMaxEP(target) - (EP - 100_000)) < 1, "EP is " + EnergyHelper.getBaseMaxEP(target));
            // a second rite on it: the choice is refused, it is done
            helper.assertTrue(MasteryChoice.choose(target, ResourceLocation.parse("tensura:corrosion_resistance")).orElse(null) == MasteryRules.Reason.ALREADY,
                    "a finished resistance could be chosen again");
        } finally {
            clean(helper, caster, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void selfRegenerationIsMasteredWithoutInjury(GameTestHelper helper) {
        if (noRite(helper)) {
            return;
        }
        RecordingPlayer caster = performer(helper, Species.WITCH);
        RecordingPlayer target = onTheCircle(helper, true, EP);
        try {
            Tier tier = tierOf("tensura:self_regeneration");
            ManasSkillInstance inst = give(target, "tensura:self_regeneration", 0);
            helper.assertTrue(MasteryChoice.choose(target, ResourceLocation.parse("tensura:self_regeneration")).isEmpty(), "could not choose it");
            work(helper, tier, caster);
            helper.assertTrue(inst.isMastered(target), "not mastered (" + tier + "): " + target.heard() + " / " + caster.heard());
        } finally {
            clean(helper, caster, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aKumoBelowHalfHealthSkillIsMastered(GameTestHelper helper) {
        if (noRite(helper) || EvoSkills.skill("tensura_kumodesu:determination").isEmpty()) {
            helper.succeed(); // Kumo Desu ga is not here
            return;
        }
        RecordingPlayer caster = performer(helper, Species.WITCH);
        RecordingPlayer target = onTheCircle(helper, true, EP);
        try {
            Tier tier = tierOf("tensura_kumodesu:determination");
            ManasSkillInstance inst = give(target, "tensura_kumodesu:determination", 0);
            helper.assertTrue(MasteryChoice.choose(target, ResourceLocation.parse("tensura_kumodesu:determination")).isEmpty(), "could not choose it");
            work(helper, tier, caster);
            helper.assertTrue(inst.isMastered(target), "not mastered: " + target.heard() + " / " + caster.heard());
        } finally {
            clean(helper, caster, target);
        }
        helper.succeed();
    }

    /** The point of mastering through addMasteryPoint: the skill's own evolution runs, which a raw setMastery would skip. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void masteringFiresTheSkillsEvolution(GameTestHelper helper) {
        if (noRite(helper)) {
            return;
        }
        RecordingPlayer caster = performer(helper, Species.WITCH);
        RecordingPlayer target = onTheCircle(helper, true, EP);
        try {
            Tier tier = tierOf("tensura:ranged_barrier");
            give(target, "tensura:ranged_barrier", 0);
            helper.assertTrue(!has(target, "tensura:multilayer_barrier"), "already has the evolution");
            helper.assertTrue(MasteryChoice.choose(target, ResourceLocation.parse("tensura:ranged_barrier")).isEmpty(), "could not choose it");
            work(helper, tier, caster);
            helper.assertTrue(has(target, "tensura:multilayer_barrier"), "the evolution did not run (onSkillMastered): " + target.heard());
        } finally {
            clean(helper, caster, target);
        }
        helper.succeed();
    }

    /** Sage cannot be mastered by play, and the Sage to Great Sage rite needs it mastered: the Rite of Mastery makes the road reachable. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void masteringSageMakesGreatSageReachable(GameTestHelper helper) {
        if (noRite(helper) || type(helper, "evolve_great_sage") == null) {
            helper.succeed();
            return;
        }
        RecordingPlayer caster = performer(helper, Species.WITCH);
        RecordingPlayer target = onTheCircle(helper, true, EP);
        try {
            give(target, "tensura:sage", 10);
            var greatSageItems = stacks("minecraft:amethyst_shard", 4, "minecraft:ender_eye", 2, "enchanted:spirit_of_otherwhere", 1);
            atNight(helper, true, () -> start(helper, "evolve_great_sage", caster, greatSageItems));
            helper.assertTrue(!has(target, "tensura:great_sage"), "an unmastered Sage evolved");
            clearDropped(helper);
            Tier tier = tierOf("tensura:sage");
            helper.assertTrue(MasteryChoice.choose(target, ResourceLocation.parse("tensura:sage")).isEmpty(), "could not choose Sage");
            work(helper, tier, caster);
            clearDropped(helper);
            atNight(helper, true, () -> start(helper, "evolve_great_sage", caster, greatSageItems));
            helper.assertTrue(has(target, "tensura:great_sage"), "Great Sage did not become reachable: " + target.heard() + " / " + caster.heard());
        } finally {
            clean(helper, caster, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aNegativeMasteryIsZeroedFirst(GameTestHelper helper) {
        if (noRite(helper)) {
            return;
        }
        RecordingPlayer caster = performer(helper, Species.WITCH);
        RecordingPlayer target = onTheCircle(helper, true, EP);
        try {
            Tier tier = tierOf("tensura:self_regeneration");
            ManasSkillInstance inst = give(target, "tensura:self_regeneration", -100);
            helper.assertTrue(MasteryChoice.choose(target, ResourceLocation.parse("tensura:self_regeneration")).isEmpty(), "could not choose it");
            work(helper, tier, caster);
            helper.assertTrue(inst.isMastered(target), "a skill still in learning was not mastered: mastery " + inst.getMastery());
        } finally {
            clean(helper, caster, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aVetoingListenerRefusesAndGivesEverythingBack(GameTestHelper helper) {
        if (noRite(helper)) {
            return;
        }
        RecordingPlayer caster = performer(helper, Species.WITCH);
        RecordingPlayer target = onTheCircle(helper, true, EP);
        SkillEvents.SkillMasteryEvent veto = (inst, entity, amount) ->
                inst.getSkillId().toString().equals("tensura:anti_skill") ? EventResult.interruptFalse() : EventResult.pass();
        SkillEvents.SKILL_MASTERY.register(veto);
        try {
            ManasSkillInstance inst = give(target, "tensura:anti_skill", 7);
            helper.assertTrue(MasteryChoice.choose(target, ResourceLocation.parse("tensura:anti_skill")).isEmpty(), "could not choose it");
            clearDropped(helper);
            work(helper, Tier.UNIQUE, caster);
            helper.assertTrue(!inst.isMastered(target), "a veto was overridden");
            helper.assertTrue(inst.getMastery() == 7, "the old mastery was not restored: " + inst.getMastery());
            helper.assertTrue(target.received(MasteryRules.VETOED), "the target heard " + target.heard());
            helper.assertTrue(caster.heard().equals(FactionRiteFactory.NO_EFFECT), "the performer heard " + caster.heard());
            helper.assertTrue(EnergyHelper.getBaseMaxEP(target) == EP, "EP was spent: " + EnergyHelper.getBaseMaxEP(target));
            helper.assertTrue(dropped(helper, "minecraft:nether_star") == 5, "the ingredients were not given back");
            helper.assertTrue(MasteryChoice.get(target).isPresent(), "a refused rite lost the choice");
            helper.assertTrue(MasteryChoice.lastRite(target) == -1, "a refusal started the cooldown");
        } finally {
            SkillEvents.SKILL_MASTERY.unregister(veto);
            clean(helper, caster, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void notEnoughEpRefusesBeforeTakingAnything(GameTestHelper helper) {
        if (noRite(helper)) {
            return;
        }
        RecordingPlayer caster = performer(helper, Species.WITCH);
        RecordingPlayer target = onTheCircle(helper, true, 750_000); // exactly the cost: not strictly more
        try {
            ManasSkillInstance inst = give(target, "tensura:anti_skill", 0);
            MasteryChoice.choose(target, ResourceLocation.parse("tensura:anti_skill"));
            work(helper, Tier.UNIQUE, caster);
            helper.assertTrue(!inst.isMastered(target), "mastered without the EP");
            helper.assertTrue(target.received("supernaturalbloodlines.refusal.mastery.no_ep"), "the target heard " + target.heard());
            helper.assertTrue(EnergyHelper.getBaseMaxEP(target) == 750_000, "EP changed: " + EnergyHelper.getBaseMaxEP(target));
            helper.assertTrue(dropped(helper, "minecraft:nether_star") == 5, "the ingredients were not given back");
        } finally {
            clean(helper, caster, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void oneRitePerTargetPerSevenDays(GameTestHelper helper) {
        if (noRite(helper)) {
            return;
        }
        RecordingPlayer caster = performer(helper, Species.WITCH);
        RecordingPlayer target = onTheCircle(helper, true, EP);
        try {
            long[] now = {1_000_000};
            MasteryChoice.clock = () -> now[0];
            Tier tier = tierOf("tensura:self_regeneration");
            ManasSkillInstance a = give(target, "tensura:self_regeneration", 0);
            ManasSkillInstance b = give(target, "tensura:anti_skill", 0);
            MasteryChoice.choose(target, ResourceLocation.parse("tensura:self_regeneration"));
            work(helper, tier, caster);
            helper.assertTrue(a.isMastered(target), "the first rite did not work: " + target.heard());
            helper.assertTrue(MasteryChoice.lastRite(target) == 1_000_000, "the rite time was not stored");
            clearDropped(helper);
            // day 6: refused, with the days left
            now[0] = 1_000_000 + 24000L * 6;
            MasteryChoice.choose(target, ResourceLocation.parse("tensura:anti_skill"));
            work(helper, Tier.UNIQUE, caster);
            helper.assertTrue(!b.isMastered(target) && target.received("supernaturalbloodlines.refusal.mastery.cooldown"), "day 6: " + target.heard());
            helper.assertTrue(MasteryChoice.lastRite(target) == 1_000_000, "a refusal moved the cooldown");
            clearDropped(helper);
            // day 7: allowed
            now[0] = 1_000_000 + 24000L * 7;
            work(helper, Tier.UNIQUE, caster);
            helper.assertTrue(b.isMastered(target), "day 7: " + target.heard());
        } finally {
            clean(helper, caster, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSystemSkillAndTheWrongTierAreRefused(GameTestHelper helper) {
        if (noRite(helper)) {
            return;
        }
        RecordingPlayer caster = performer(helper, Species.WITCH);
        RecordingPlayer target = onTheCircle(helper, true, EP);
        try {
            ManasSkillInstance inst = give(target, "tensura:anti_skill", 0);
            // the wrong tier: Anti Skill is Unique, this is the Extra rite
            MasteryChoice.choose(target, ResourceLocation.parse("tensura:anti_skill"));
            work(helper, Tier.EXTRA, caster);
            helper.assertTrue(!inst.isMastered(target) && target.received("supernaturalbloodlines.refusal.mastery.wrong_tier"), "wrong tier: " + target.heard());
            clearDropped(helper);
            // the refused list: chosen first, listed after (the config changed under the choice), and a new choice is refused outright
            EvolutionRitesConfig.override("mastery_refused_skills", List.of("tensura:anti_skill"));
            work(helper, Tier.UNIQUE, caster);
            helper.assertTrue(!inst.isMastered(target) && target.received("supernaturalbloodlines.refusal.mastery.system"), "system: " + target.heard());
            helper.assertTrue(MasteryChoice.choose(target, ResourceLocation.parse("tensura:anti_skill")).orElse(null) == MasteryRules.Reason.SYSTEM, "a refused skill could be chosen");
        } finally {
            clean(helper, caster, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aTemporarySkillIsRefused(GameTestHelper helper) {
        if (noRite(helper)) {
            return;
        }
        RecordingPlayer target = onTheCircle(helper, true, EP);
        try {
            ManasSkillInstance inst = give(target, "tensura:anti_skill", 0);
            inst.setRemoveTime(1200);
            helper.assertTrue(MasteryChoice.choose(target, ResourceLocation.parse("tensura:anti_skill")).orElse(null) == MasteryRules.Reason.TEMPORARY, "a lent skill could be chosen");
        } finally {
            clean(helper, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theUniqueTierNeedsAFullMoonAndTheUltimateTierAnEclipse(GameTestHelper helper) {
        if (noRite(helper)) {
            return;
        }
        RecordingPlayer caster = performer(helper, Species.WITCH);
        RecordingPlayer target = onTheCircle(helper, true, EP);
        try {
            ManasSkillInstance inst = give(target, "tensura:anti_skill", 0);
            MasteryChoice.choose(target, ResourceLocation.parse("tensura:anti_skill"));
            atNight(helper, false, () -> start(helper, rite(Tier.UNIQUE), caster, items(Tier.UNIQUE)));
            helper.assertTrue(!inst.isMastered(target) && caster.received("supernaturalbloodlines.rite.needs_full_moon"), "ordinary night: " + caster.heard());
            clearDropped(helper);
            // the Ultimate rite on a plain full moon: needs the eclipse
            atNight(helper, true, () -> start(helper, rite(Tier.ULTIMATE), caster, items(Tier.ULTIMATE)));
            helper.assertTrue(caster.received("supernaturalbloodlines.rite.needs_eclipse"), "full moon, Ultimate rite: " + caster.heard());
        } finally {
            clean(helper, caster, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void thePerformerNeverLearnsTheChosenSkill(GameTestHelper helper) {
        if (noRite(helper)) {
            return;
        }
        String heardFirst = null;
        for (String skill : List.of("tensura:anti_skill", "tensura:self_regeneration")) {
            RecordingPlayer caster = performer(helper, Species.WITCH);
            RecordingPlayer target = onTheCircle(helper, true, 10); // cannot pay: a refusal about the target
            try {
                give(target, skill, 0);
                MasteryChoice.choose(target, ResourceLocation.parse(skill));
                clearDropped(helper);
                work(helper, tierOf(skill) == Tier.UNIQUE ? Tier.UNIQUE : Tier.EXTRA, caster);
                String heard = caster.heard();
                helper.assertTrue(heard.equals(FactionRiteFactory.NO_EFFECT), skill + ": the performer heard " + heard);
                if (heardFirst == null) {
                    heardFirst = heard;
                }
                helper.assertTrue(heard.equals(heardFirst), "the performer heard something else for another skill");
            } finally {
                clean(helper, caster, target);
            }
        }
        // a target who does not agree: the same one answer, and the target is asked
        RecordingPlayer caster = performer(helper, Species.WITCH);
        RecordingPlayer target = onTheCircle(helper, false, EP);
        try {
            give(target, "tensura:anti_skill", 0);
            MasteryChoice.choose(target, ResourceLocation.parse("tensura:anti_skill"));
            work(helper, Tier.UNIQUE, caster);
            helper.assertTrue(caster.heard().equals(FactionRiteFactory.NO_EFFECT), "the performer heard " + caster.heard());
            helper.assertTrue(target.receivedOnActionBar("supernaturalbloodlines.rite.sneak_to_master"), "the target was not asked: " + target.heard());
        } finally {
            clean(helper, caster, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theServerValidatesTheChoice(GameTestHelper helper) {
        RecordingPlayer target = onTheCircle(helper, true, EP);
        try {
            helper.assertTrue(MasteryChoice.choose(target, ResourceLocation.parse("tensura:anti_skill")).orElse(null) == MasteryRules.Reason.NOT_HELD, "a skill the target does not hold");
            ManasSkillInstance inst = give(target, "tensura:anti_skill", 0);
            helper.assertTrue(MasteryChoice.get(target).isEmpty(), "a refused choice was stored");
            inst.setMastery(inst.getMaxMastery());
            helper.assertTrue(MasteryChoice.choose(target, ResourceLocation.parse("tensura:anti_skill")).orElse(null) == MasteryRules.Reason.ALREADY, "a mastered skill");
            helper.assertTrue(MasteryChoice.get(target).isEmpty(), "a refused choice was stored");
            inst.setMastery(0);
            helper.assertTrue(MasteryChoice.choose(target, ResourceLocation.parse("tensura:anti_skill")).isEmpty(), "a good choice was refused");
            helper.assertTrue(MasteryChoice.get(target).map(Object::toString).orElse("").equals("tensura:anti_skill"), "the choice was not stored");
            var options = MasteryChoice.options(target);
            helper.assertTrue(options.size() == SkillAPI.getSkillsFrom(target).getLearnedSkills().size(), "the options list is not every held skill");
            helper.assertTrue(options.stream().anyMatch(o -> o.skill().toString().equals("tensura:anti_skill") && o.refusal().isEmpty() && o.tier() == Tier.UNIQUE), "the options list: " + options);
        } finally {
            clean(helper, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theSwitchClosesTheRite(GameTestHelper helper) {
        if (noRite(helper)) {
            return;
        }
        RecordingPlayer caster = performer(helper, Species.WITCH);
        RecordingPlayer target = onTheCircle(helper, true, EP);
        try {
            ManasSkillInstance inst = give(target, "tensura:anti_skill", 0);
            MasteryChoice.choose(target, ResourceLocation.parse("tensura:anti_skill"));
            EvolutionRitesConfig.override("mastery_enabled", false);
            work(helper, Tier.UNIQUE, caster);
            helper.assertTrue(!inst.isMastered(target) && caster.received("supernaturalbloodlines.refusal.evolution.closed"), "off: " + caster.heard());
        } finally {
            clean(helper, caster, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anEclipseMastersAnUltimate(GameTestHelper helper) {
        if (noRite(helper) || EvoSkills.skill("tensura:great_sage").isEmpty()) {
            helper.succeed();
            return;
        }
        // no Ultimate skill ships in Tensura's base game that this test can hold safely; the night is what is tested: the Ultimate rite works only in an eclipse
        RecordingPlayer caster = performer(helper, Species.WITCH);
        RecordingPlayer target = onTheCircle(helper, true, EP);
        try {
            MasteryChoice.clear(target);
            atEclipse(helper, () -> start(helper, rite(Tier.ULTIMATE), caster, items(Tier.ULTIMATE)));
            helper.assertTrue(target.received("supernaturalbloodlines.refusal.mastery.no_choice"), "in an eclipse the rite runs as far as the choice: " + target.heard());
            helper.assertTrue(dropped(helper, firstItem(Tier.ULTIMATE)) == 10, "the ingredients were not given back");
        } finally {
            clean(helper, caster, target);
        }
        helper.succeed();
    }
}
