package net.ofatech.supernaturalbloodlines.parts.celestials;

import net.ofatech.supernaturalbloodlines.core.moon.LunarMoods;
import net.ofatech.supernaturalbloodlines.shared.werewolves.ForcedShift;

/** A Super Blue Moon skips the forced full-moon shift. Links Werewolves: call only when it is loaded. */
final class SuperBlueMoon {
    private SuperBlueMoon() {
    }

    static void register() {
        // the spare only counts in a registered gate; species and artifacts register it, but with both off nobody does (and the at-will rule stays theirs)
        ForcedShift.registerSparesOnly();
        ForcedShift.addForcedSpare(p -> LunarMoods.sparesForcedShift(p.level()));
    }
}
