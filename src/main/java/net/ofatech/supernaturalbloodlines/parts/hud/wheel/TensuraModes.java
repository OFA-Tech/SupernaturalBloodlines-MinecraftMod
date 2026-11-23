package net.ofatech.supernaturalbloodlines.parts.hud.wheel;

import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ability.AbilityPreset;
import io.github.manasmods.tensura.storage.ability.AbilitySlot;
import io.github.manasmods.tensura.storage.ability.IAbility;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.hud.PresetPair;

/**
 * Where a Tensura skill's mode lives. Tensura keeps it in the preset slot ({@code AbilitySlot.mode}), not on the skill, so a skill in a slot has the mode of the first slot
 * that holds it (active preset, then the next one, then any other), and a skill in no slot uses {@link WheelModes}. Linked to Tensura.
 */
public final class TensuraModes {
    /** A slot that holds a skill: where, and its mode. */
    public record Held(int preset, int slot, int mode) {
    }

    private TensuraModes() {
    }

    /** Every slot of every preset that holds the skill, the active preset first, then the next one, then the rest in order. */
    public static List<Held> slotsHolding(ServerPlayer p, ManasSkill skill) {
        IAbility ab = TensuraStorages.getAbilityFrom(p);
        List<AbilityPreset> presets = ab.getPresets();
        List<Held> out = new ArrayList<>();
        if (presets == null || presets.isEmpty()) {
            return out;
        }
        int active = Math.floorMod(ab.getActivePreset(), presets.size());
        int next = PresetPair.right(active, presets.size());
        List<Integer> order = new ArrayList<>(List.of(active));
        if (next != active) {
            order.add(next);
        }
        for (int i = 0; i < presets.size(); i++) {
            if (!order.contains(i)) {
                order.add(i);
            }
        }
        for (int preset : order) {
            List<AbilitySlot> slots = presets.get(preset).getAbilities();
            for (int s = 0; slots != null && s < slots.size(); s++) {
                AbilitySlot slot = slots.get(s);
                if (slot != null && !slot.isEmpty() && slot.getSkill() == skill) {
                    out.add(new Held(preset, s, slot.getMode()));
                }
            }
        }
        return out;
    }

    /** The mode the skill runs in now, clamped to its modes. */
    public static int current(ServerPlayer p, ManasSkillInstance inst) {
        List<Held> held = slotsHolding(p, inst.getSkill());
        int mode = held.isEmpty() ? WheelModes.get(p, inst.getSkillId()) : held.get(0).mode();
        return Math.max(0, Math.min(mode, Math.max(1, inst.getModes()) - 1));
    }
}
