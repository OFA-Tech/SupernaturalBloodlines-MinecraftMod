package net.ofatech.supernaturalbloodlines.core.wheel;

import net.ofatech.supernaturalbloodlines.core.hud.InputPlan;
import net.ofatech.supernaturalbloodlines.core.hud.PresetPair;

/** Which Tensura preset slot a HUD slot (0 to 5) is: 0-2 the active preset, 3-5 the next one. Presets wrap. */
public final class SlotPlan {
    public record TensuraSlot(int preset, int slot) {
    }

    private SlotPlan() {
    }

    public static TensuraSlot tensura(int hudSlot, int activePreset, int presetCount) {
        if (hudSlot < 0 || hudSlot >= 6) {
            throw new IllegalArgumentException("hud slot " + hudSlot);
        }
        int active = presetCount <= 0 ? 0 : Math.floorMod(activePreset, presetCount);
        int preset = InputPlan.rightColumn(hudSlot) ? PresetPair.right(active, presetCount) : PresetPair.left(active);
        return new TensuraSlot(preset, InputPlan.row(hudSlot));
    }
}
