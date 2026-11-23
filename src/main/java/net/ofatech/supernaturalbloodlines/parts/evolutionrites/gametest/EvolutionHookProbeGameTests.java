package net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest;

import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.util.EnergyHelper;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.probe.Hooks;

/**
 * The executable form of {@code docs/superpowers/research/tensura-addons/verified-hooks.md}: every hook the part calls is looked up in the jars on the
 * run classpath, and a missing one is named (a Tensura or ManasCore version that moved).
 */
@PrefixGameTestTemplate(false)
public final class EvolutionHookProbeGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private EvolutionHookProbeGameTests() {
    }

    /** One row per hook; class names of the addons are loaded by name only. */
    private static List<Hooks.Hit> rows() {
        List<Hooks.Hit> out = new ArrayList<>();
        out.add(Hooks.method("io.github.manasmods.tensura.ability.SkillHelper", "learnSkill", LivingEntity.class, ManasSkill.class));
        out.add(Hooks.method("io.github.manasmods.tensura.ability.SkillUtils", "hasSkill", LivingEntity.class, ManasSkill.class));
        out.add(Hooks.method("io.github.manasmods.tensura.ability.SkillUtils", "isSkillMastered", LivingEntity.class, ManasSkill.class));
        out.add(Hooks.method("io.github.manasmods.tensura.util.EnergyHelper", "getBaseMaxEP", LivingEntity.class));
        out.add(Hooks.method("io.github.manasmods.tensura.util.EnergyHelper", "setBaseMaxEP", LivingEntity.class, double.class));
        out.add(Hooks.method("io.github.manasmods.tensura.ability.skill.Skill", "getType"));
        out.add(Hooks.method("io.github.manasmods.manascore.skill.api.ManasSkillInstance", "isMastered", LivingEntity.class));
        out.add(Hooks.method("io.github.manasmods.manascore.skill.api.ManasSkillInstance", "addMasteryPoint", LivingEntity.class, double.class));
        out.add(Hooks.method("io.github.manasmods.manascore.skill.api.ManasSkillInstance", "setMastery", double.class));
        out.add(Hooks.method("io.github.manasmods.manascore.skill.api.ManasSkillInstance", "onLearnSkill", LivingEntity.class));
        out.add(Hooks.method("io.github.manasmods.manascore.skill.api.SkillAPI", "getSkillsFrom", LivingEntity.class));
        return out;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void everyListedHookResolves(GameTestHelper helper) {
        List<String> missing = new ArrayList<>();
        for (Hooks.Hit hit : rows()) {
            hit.missing().ifPresent(missing::add);
        }
        helper.assertTrue(missing.isEmpty(), "hooks that are not there: " + missing);
        // the types the rites read are the ones the calls above name
        helper.assertTrue(EnergyHelper.class != null && ManasSkillInstance.class != null && SkillAPI.class != null, "unreachable");
        helper.succeed();
    }
}
