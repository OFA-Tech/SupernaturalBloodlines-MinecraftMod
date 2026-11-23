package net.ofatech.supernaturalbloodlines.core.wheel;

/** Small rules of the wheel that need no game. */
public final class WheelRules {
    /** Where a right click puts a power: our Loadout, Tensura's preset, or nowhere. */
    public enum SlotTarget { LOADOUT, TENSURA_PRESET, NONE }

    private WheelRules() {
    }

    public static SlotTarget slotTarget(PowerSource source, boolean slottable) {
        return switch (source) {
            case BLOODLINE, VAMPIRISM, WEREWOLVES -> SlotTarget.LOADOUT;
            case TENSURA -> slottable ? SlotTarget.TENSURA_PRESET : SlotTarget.NONE;
            case INFINITY, GREAT_SAGE -> SlotTarget.NONE;
        };
    }
}
