package net.ofatech.supernaturalbloodlines.core.wheel;

/** Small rules of the wheel that need no game. */
public final class WheelRules {
    /** Where a right click puts a power: our Loadout, Tensura's preset, or nowhere. */
    public enum SlotTarget { LOADOUT, TENSURA_PRESET, NONE }

    private WheelRules() {
    }

    /** The powers the hub's Loadout page offers: the ones the wheel puts in our Loadout (a right click on them), in the wheel's order. */
    public static java.util.List<PowerRef> loadoutPowers(java.util.List<PowerRef> all) {
        java.util.List<PowerRef> out = new java.util.ArrayList<>();
        for (PowerRef r : all) {
            if (slotTarget(r.source(), true) == SlotTarget.LOADOUT) {
                out.add(r);
            }
        }
        return out;
    }

    public static SlotTarget slotTarget(PowerSource source, boolean slottable) {
        return switch (source) {
            case BLOODLINE, VAMPIRISM, WEREWOLVES -> SlotTarget.LOADOUT;
            case TENSURA -> slottable ? SlotTarget.TENSURA_PRESET : SlotTarget.NONE;
            case INFINITY, GREAT_SAGE -> SlotTarget.NONE;
        };
    }
}
