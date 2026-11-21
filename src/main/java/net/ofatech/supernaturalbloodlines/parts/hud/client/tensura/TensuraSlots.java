package net.ofatech.supernaturalbloodlines.parts.hud.client.tensura;

import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.ability.TensuraSkill;
import io.github.manasmods.tensura.ability.skill.resist.ResistSkill;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ability.AbilitySlot;
import io.github.manasmods.tensura.storage.ability.IAbility;
import net.minecraft.client.player.LocalPlayer;
import net.ofatech.supernaturalbloodlines.core.hud.InputPlan;
import net.ofatech.supernaturalbloodlines.core.hud.Mastery;
import net.ofatech.supernaturalbloodlines.core.hud.PresetPair;
import net.ofatech.supernaturalbloodlines.core.hud.SkillCooldown;
import net.ofatech.supernaturalbloodlines.core.hud.SlotView;
import net.ofatech.supernaturalbloodlines.parts.hud.client.HudKeys;

/**
 * Tensura mode: slots 1 to 3 are the active preset and 4 to 6 the next one ({@link PresetPair}), read from Tensura's own storage and ManasCore's skill
 * instances (nothing is stored by us). Built once per client tick, not per frame. Linked to Tensura: reached only after {@code ModList.isLoaded("tensura")}.
 */
public final class TensuraSlots {
    public static final String GREY_MISSING = "supernaturalbloodlines.hud.slot.skill_missing";
    public static final String GREY_BLOCKED = "supernaturalbloodlines.hud.slot.cannot_use";

    /** The longest cooldown seen for each slot since it was last ready: Tensura gives the ticks left, not the total, so the fill is left/peak. */
    private static final int[] PEAK = new int[6];

    private TensuraSlots() {
    }

    public static void refresh(LocalPlayer p, SlotView[] out) {
        IAbility ab = TensuraStorages.getAbilityFrom(p);
        int presets = ab.getPresets().size();
        int active = ab.getActivePreset();
        for (int i = 0; i < out.length; i++) {
            out[i] = view(p, ab, i, InputPlan.rightColumn(i) ? PresetPair.right(active, presets) : PresetPair.left(active));
        }
    }

    /** The skill and mode a key would send right now, as {@code [skill registry id, mode]}; null skill for an empty slot. */
    static AbilitySlot slot(LocalPlayer p, int hudSlot) {
        IAbility ab = TensuraStorages.getAbilityFrom(p);
        int active = ab.getActivePreset();
        return ab.getAbilitySlot(InputPlan.rightColumn(hudSlot) ? PresetPair.right(active, ab.getPresets().size()) : PresetPair.left(active), InputPlan.row(hudSlot));
    }

    private static SlotView view(LocalPlayer p, IAbility ab, int hudSlot, int preset) {
        String key = HudKeys.SLOTS[hudSlot].getTranslatedKeyMessage().getString();
        AbilitySlot slot = ab.getAbilitySlot(preset, InputPlan.row(hudSlot));
        ManasSkill skill = slot == null || slot.isEmpty() ? null : slot.getSkill();
        if (skill == null) {
            PEAK[hudSlot] = 0;
            return SlotView.EMPTY.withKey(key);
        }
        int mode = slot.getMode();
        ManasSkillInstance inst = SkillAPI.getSkillsFrom(p).getSkill(skill).orElse(null);
        String name = skill.getName().getString();
        if (inst == null) {
            return new SlotView(skill.getSkillIcon(), key, 0f, 0, false, GREY_MISSING, name, null);
        }
        // ManasCore counts a skill's cooldown in seconds (SkillStorage lowers it by 1 every 20 server ticks): see SkillCooldown
        int left = inst.onCoolDown(mode) ? inst.getCoolDown(mode) : 0;
        PEAK[hudSlot] = left == 0 ? 0 : Math.max(PEAK[hudSlot], left);
        float fraction = SkillCooldown.fraction(left, PEAK[hudSlot]);
        String detail = skill instanceof TensuraSkill ts && inst.getModes() > 1 ? ts.getModeName(inst, mode).getString() : null;
        return new SlotView(skill.getSkillIcon(), key, fraction, SkillCooldown.seconds(left), inst.isToggled(),
                inst.canInteractSkill(p) ? null : GREY_BLOCKED, name, detail, mastery(p, skill, inst));
    }

    /** The same two numbers Tensura's own skill HUD reads on the client: {@code getMastery} against {@code getMaxMastery}, below zero while a resist skill is still being learned. */
    static Mastery mastery(LocalPlayer p, ManasSkill skill, ManasSkillInstance inst) {
        int learning = skill instanceof ResistSkill r ? r.getLearningPointRequirement() : 0;
        return Mastery.of(inst.getMastery(), inst.getMaxMastery(), learning, inst.isMastered(p));
    }
}
