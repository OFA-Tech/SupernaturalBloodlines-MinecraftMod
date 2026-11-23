package net.ofatech.supernaturalbloodlines.parts.hud.tensura;

import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.tensura.storage.ability.AbilityPreset;
import io.github.manasmods.tensura.storage.ability.AbilitySlot;
import java.util.List;
import java.util.UUID;
import net.ofatech.supernaturalbloodlines.core.hud.PresetPair;
import org.jetbrains.annotations.Nullable;

/**
 * The rule of reserved mixin (1): a skill in preset N+1 (the HUD's right column) also counts as "in the active preset", so Tensura accepts its
 * activation packet and keeps its passive effect. Tensura Slots' mixin does the same; this is only reached when its answer was false, so both can be present.
 * Also true for the one skill the skill wheel is starting or holding ({@link WheelActivation}), so a power in no slot can be used from the wheel.
 * Linked to Tensura; reached only from the mixin, which is applied only when Tensura is installed.
 */
public final class NextPreset {
    /** "Any" for {@code slot} and {@code mode}. */
    public static final int ANY = -1;

    private NextPreset() {
    }

    /**
     * @param slot the slot index inside the preset the key belongs to ({@link #ANY} for a passive check)
     * @param mode the skill's mode in that slot ({@link #ANY} when the caller does not ask)
     * @param owner the owner of the ability storage that is asked (the wheel's scope is only open for its own player)
     */
    public static boolean matches(List<AbilityPreset> presets, int activePreset, ManasSkill skill, int slot, int mode, @Nullable UUID owner) {
        if (WheelActivation.allows(owner, skill, slot, mode)) {
            return true;
        }
        try {
            if (presets == null || presets.isEmpty()) {
                return false;
            }
            List<AbilitySlot> next = presets.get(PresetPair.right(activePreset, presets.size())).getAbilities();
            if (next == null) {
                return false;
            }
            for (int i = 0; i < next.size(); i++) {
                AbilitySlot s = next.get(i);
                if (s != null && s.getSkill() == skill && (slot == ANY || slot == i) && (mode == ANY || mode == s.getMode())) {
                    return true;
                }
            }
        } catch (RuntimeException e) {
            // a malformed preset list must never break Tensura's own check: its answer stands
        }
        return false;
    }
}
