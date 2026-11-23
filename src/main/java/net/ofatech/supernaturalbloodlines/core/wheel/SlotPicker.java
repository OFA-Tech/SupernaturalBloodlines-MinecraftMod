package net.ofatech.supernaturalbloodlines.core.wheel;

import java.util.ArrayList;
import java.util.List;

/**
 * The slot picker (right click): the six slots the power can go in. A Tensura power is put in the active preset (slots 1 to 3) or the next one (4 to 6) through Tensura's own
 * request; every other power goes in our Loadout. The power and mode a slot holds now are what the picker shows ("Replaces Ultraspeed"). Pure.
 */
public final class SlotPicker {
    public static final int SLOTS = 6;

    /** One slot of the picker. {@code preset} and {@code row} are Tensura's (-1 for a Loadout slot); {@code holds} is the id of what is in it now, empty for nothing. */
    public record Slot(int index, WheelRules.SlotTarget target, int preset, int row, String holds) {
    }

    /** What the click on a slot sends. */
    public sealed interface Send permits SendLoadout, SendTensura, SendNothing {
    }

    public record SendLoadout(int slot, String actionId) implements Send {
    }

    public record SendTensura(int preset, int slot, String skillId, int mode) implements Send {
    }

    public record SendNothing() implements Send {
    }

    private SlotPicker() {
    }

    /**
     * @param holds        what each of the six HUD slots holds now in the target (the ids, empty for none): the Loadout's, or the Tensura slots' (left column then right)
     * @param activePreset Tensura's active preset
     * @param presetCount  how many presets Tensura has
     */
    public static List<Slot> slotsFor(WheelRules.SlotTarget target, List<String> holds, int activePreset, int presetCount) {
        List<Slot> out = new ArrayList<>();
        if (target == WheelRules.SlotTarget.NONE) {
            return out;
        }
        for (int i = 0; i < SLOTS; i++) {
            String held = i < holds.size() ? holds.get(i) : "";
            if (target == WheelRules.SlotTarget.TENSURA_PRESET) {
                SlotPlan.TensuraSlot t = SlotPlan.tensura(i, activePreset, presetCount);
                out.add(new Slot(i, target, t.preset(), t.slot(), held));
            } else {
                out.add(new Slot(i, target, -1, -1, held));
            }
        }
        return out;
    }

    /** What to send when slot {@code index} is chosen for the power {@code id} (a skill id for Tensura) in mode {@code mode}. */
    public static Send assign(WheelRules.SlotTarget target, int index, String id, int mode, int activePreset, int presetCount) {
        if (index < 0 || index >= SLOTS) {
            return new SendNothing();
        }
        return switch (target) {
            case LOADOUT -> new SendLoadout(index, id);
            case TENSURA_PRESET -> {
                SlotPlan.TensuraSlot t = SlotPlan.tensura(index, activePreset, presetCount);
                yield new SendTensura(t.preset(), t.slot(), id, mode);
            }
            case NONE -> new SendNothing();
        };
    }
}
