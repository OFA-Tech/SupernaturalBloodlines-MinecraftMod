package net.ofatech.supernaturalbloodlines.parts.species.hybrid;

import net.minecraft.world.entity.player.Player;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.shared.werewolves.ForcedShift;
import net.ofatech.supernaturalbloodlines.shared.werewolves.MoonControl;

/**
 * Hybrids shift at will, so they have moon control and the forced full-moon shift is cancelled for them. The
 * detection and the cancelling live in {@link ForcedShift}; this registers the hybrid types as a control source.
 * Links Werewolves: register it only behind {@code ModList.isLoaded("werewolves")}.
 */
public final class FormControl {
    private FormControl() {
    }

    public static void register() {
        ForcedShift.register();
        // R96: once unbound (a bound hybrid has no forms at all: ForcedShift blocks them until the Rite of the Moon)
        MoonControl.addSource(p -> HybridTraits.isHybrid(BloodlineAccess.get(p).species()) && !BloodlineAccess.get(p).hybridCurseBound());
    }

    public static void enterTick(Player p) {
        ForcedShift.enterTick(p);
    }

    public static void leaveTick(Player p) {
        ForcedShift.leaveTick(p);
    }
}
