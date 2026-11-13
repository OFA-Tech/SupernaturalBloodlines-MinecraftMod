package net.ofatech.supernaturalbloodlines.parts.species.pack;

import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Lineage;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import net.ofatech.supernaturalbloodlines.shared.werewolves.AnchorItems;
import net.ofatech.supernaturalbloodlines.shared.werewolves.ForcedShift;
import net.ofatech.supernaturalbloodlines.shared.werewolves.MoonControl;
import net.ofatech.supernaturalbloodlines.shared.werewolves.MoonControl.Control;

/**
 * Anchors (R94, R96): a werewolf is anchored while they hold or wear an item of {@code #supernaturalbloodlines:anchor}
 * (see {@link AnchorItems}) or the player they named with {@code /pack anchor} is within 16 blocks. An anchored wolf's
 * forced full-moon shift is halved (the same per-night coin as a born wolf's); a BORN wolf that is anchored has HALF moon
 * control instead, which spares the forced shift altogether and allows the half forms at will. The two reasons for
 * "halved" do not stack: a born wolf is already halved, and an anchor on top gives control, not a second halving.
 * Links Werewolves (through {@link ForcedShift}): register only behind {@code isLoaded("werewolves")}.
 */
public final class Anchors {
    public static final double RANGE = 16;

    private Anchors() {
    }

    public static void register() {
        ForcedShift.register();
        MoonControl.addCap(p -> PackAccess.wolf(p).lineage() == Lineage.BORN && anchored(p) ? Control.HALF : Control.NONE);
        MoonControl.addForcedShiftHalver(Anchors::anchored);
    }

    public static boolean anchored(ServerPlayer p) {
        return AnchorItems.holdsOrWears(p) || anchorPlayerNear(p);
    }

    public static boolean anchorPlayerNear(ServerPlayer p) {
        return PackIndex.get(p.serverLevel()).anchorOf(p.getUUID()).map(id -> {
            ServerPlayer anchor = SireCall.player(p.server, id);
            return anchor != null && anchor.isAlive() && anchor.level() == p.level() && p.distanceToSqr(anchor) <= RANGE * RANGE;
        }).orElse(false);
    }
}
