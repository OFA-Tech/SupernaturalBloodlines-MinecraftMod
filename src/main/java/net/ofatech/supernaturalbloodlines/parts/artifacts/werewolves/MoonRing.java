package net.ofatech.supernaturalbloodlines.parts.artifacts.werewolves;

import net.ofatech.supernaturalbloodlines.parts.artifacts.ArtifactItems;
import net.ofatech.supernaturalbloodlines.shared.werewolves.ForcedShift;
import net.ofatech.supernaturalbloodlines.shared.werewolves.MoonControl;

/** R96: the owner of a worn Moon Ring has moon control (see {@link MoonControl}). Links Werewolves: call only when it is loaded. */
public final class MoonRing {
    private MoonRing() {
    }

    public static void register() {
        ForcedShift.register();
        MoonControl.addSource(p -> ArtifactItems.isBoundWorn(p, ArtifactItems.MOON_RING.get()));
    }
}
