package net.ofatech.supernaturalbloodlines.parts.hud.gametest;

import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ability.IAbility;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.hud.PresetPair;
import net.ofatech.supernaturalbloodlines.parts.hud.HudConfig;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;

/**
 * 13b Task 2, the open hook: does Tensura's server refuse a skill that is in the next preset (the right column) without Tensura Slots' mixin?
 * <b>Yes</b> (javap, then these tests): Tensura's {@code AbilityHandler} answers ManasCore's {@code ACTIVATE_SKILL} event with
 * {@code interruptFalse} unless {@code isAbilityInActivePreset(keyNumber, skill, mode)}, and every overload of that method looks only at the
 * active preset. So the HUD part carries the reserved {@code AbilityStorageNextPresetMixin} (switch {@code next_preset_active}); these tests
 * were red before it. The key number is the slot index inside the preset, for both columns.
 */
@PrefixGameTestTemplate(false)
public final class TensuraPresetGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    /** Any Tensura skill that ManasCore lets a player hold when it is in the active preset (checked over the first 80 skills). */
    private static final ResourceLocation SKILL = ResourceLocation.parse("tensura:aura_slash");
    private static final ResourceLocation OTHER = ResourceLocation.parse("tensura:battlewill");

    private TensuraPresetGameTests() {
    }

    private static ManasSkill skill(ResourceLocation id) {
        return SkillAPI.getSkillRegistry().get(id);
    }

    private static ServerPlayer player(GameTestHelper helper, int active) {
        ServerPlayer p = TestPlayers.create(helper);
        SkillAPI.getSkillsFrom(p).learnSkill(skill(SKILL));
        SkillAPI.getSkillsFrom(p).learnSkill(skill(OTHER));
        TensuraStorages.getAbilityFrom(p).setActivePreset(active);
        return p;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSkillInTheActivePresetCountsAsActive(GameTestHelper helper) {
        ServerPlayer p = player(helper, 0);
        IAbility ab = TensuraStorages.getAbilityFrom(p);
        ab.setAbilitySlot(0, 1, skill(SKILL), 0);
        helper.assertTrue(ab.isAbilityInActivePreset(1, skill(SKILL), 0), "the control: slot 1 of the active preset");
        helper.assertTrue(SkillAPI.getSkillsFrom(p).startHoldSkill(SKILL, 1, 0), "the control: the activation packet's call is refused for a skill in the active preset");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSkillInTheNextPresetCountsAsActiveForTheFourChecks(GameTestHelper helper) {
        ServerPlayer p = player(helper, 0);
        IAbility ab = TensuraStorages.getAbilityFrom(p);
        ab.setAbilitySlot(PresetPair.right(0, PresetPair.TENSURA_PRESETS), 2, skill(SKILL), 0);
        ManasSkill s = skill(SKILL);
        helper.assertTrue(ab.isAbilityInActivePreset(2, s, 0), "(slot, skill, mode): what the activation event asks");
        helper.assertTrue(ab.isAbilityInActivePreset(2, s), "(slot, skill)");
        helper.assertTrue(ab.isAbilityInActivePreset(s, 0), "(skill, mode)");
        helper.assertTrue(ab.isAbilityInActivePreset(s), "(skill): what passives ask");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theActivationPacketAcceptsASkillInTheNextPreset(GameTestHelper helper) {
        ServerPlayer p = player(helper, 0);
        TensuraStorages.getAbilityFrom(p).setAbilitySlot(1, 1, skill(SKILL), 0);
        helper.assertTrue(SkillAPI.getSkillsFrom(p).startHoldSkill(SKILL, 1, 0), "Tensura refused a skill in preset N+1 (the HUD's right column)");
        helper.succeed();
    }

    /** The switch: with `next_preset_active` false the mixin steps aside and Tensura's own refusal comes back. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void withTheSwitchOffTensuraRefusesTheNextPresetAgain(GameTestHelper helper) {
        ServerPlayer p = player(helper, 0);
        IAbility ab = TensuraStorages.getAbilityFrom(p);
        ab.setAbilitySlot(PresetPair.right(0, PresetPair.TENSURA_PRESETS), 2, skill(SKILL), 0);
        HudConfig.NEXT_PRESET_ACTIVE.set(false);
        try {
            helper.assertTrue(!ab.isAbilityInActivePreset(2, skill(SKILL), 0), "the check still says yes with the switch off");
            helper.assertTrue(!ab.isAbilityInActivePreset(skill(SKILL)), "the passive check still says yes with the switch off");
            helper.assertTrue(!SkillAPI.getSkillsFrom(p).startHoldSkill(SKILL, 2, 0), "the activation was accepted with the switch off");
        } finally {
            HudConfig.NEXT_PRESET_ACTIVE.set(true);
        }
        helper.assertTrue(ab.isAbilityInActivePreset(2, skill(SKILL), 0), "the switch back on did not bring the answer back");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void thePresetAfterTheLastIsTheFirst(GameTestHelper helper) {
        ServerPlayer p = player(helper, PresetPair.TENSURA_PRESETS - 1);
        IAbility ab = TensuraStorages.getAbilityFrom(p);
        ab.setAbilitySlot(0, 0, skill(SKILL), 0);
        helper.assertTrue(ab.isAbilityInActivePreset(0, skill(SKILL), 0), "preset 9 pairs with preset 1");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void onlyTheNextPresetCountsAndOnlyAtItsSlot(GameTestHelper helper) {
        ServerPlayer p = player(helper, 0);
        IAbility ab = TensuraStorages.getAbilityFrom(p);
        ab.setAbilitySlot(1, 0, skill(SKILL), 0);
        ab.setAbilitySlot(2, 0, skill(OTHER), 0);
        helper.assertTrue(!ab.isAbilityInActivePreset(skill(OTHER)), "a skill two presets away counts as active");
        helper.assertTrue(!ab.isAbilityInActivePreset(0, skill(OTHER), 0), "a skill two presets away is allowed to activate");
        helper.assertTrue(!ab.isAbilityInActivePreset(1, skill(SKILL), 0), "the next preset's slot 1 was accepted for the skill that is in slot 0");
        helper.assertTrue(!ab.isAbilityInActivePreset(0, skill(SKILL), 3), "a wrong mode was accepted");
        helper.assertTrue(!SkillAPI.getSkillsFrom(p).startHoldSkill(OTHER, 0, 0), "Tensura activated a skill two presets away");
        helper.succeed();
    }
}
