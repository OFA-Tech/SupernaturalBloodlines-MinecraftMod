package net.ofatech.supernaturalbloodlines.parts.hud.wheel;

import de.teamlapen.vampirism.api.VReference;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerSource;
import net.ofatech.supernaturalbloodlines.parts.hud.action.FactionActions;
import org.jetbrains.annotations.Nullable;

/** The only door from {@link WheelService} to Vampirism: every method checks {@code isLoaded} first, so the service loads on a server without it. */
final class VampirismGate {
    private VampirismGate() {
    }

    static boolean loaded() {
        return ModList.get().isLoaded("vampirism");
    }

    /** Which mod owns a Vampirism or Werewolves action id, null when there is no such action (or no Vampirism). The Vision pseudo action is Vampirism's. */
    @Nullable
    static PowerSource sourceOf(ResourceLocation id) {
        if (!loaded()) {
            return null;
        }
        if (VampirismVision.ID.equals(id)) {
            return PowerSource.VAMPIRISM;
        }
        var action = FactionActions.byId(id);
        return action == null ? null : FactionProvider.sourceOf(action, id);
    }

    /** @return the refusal key suffix, null when the vision cycled */
    @Nullable
    static String cycleVision(ServerPlayer p) {
        if (!loaded()) {
            return "needs_vampirism";
        }
        boolean vampire = FactionActions.current(p).map(fp -> fp.getFaction() == VReference.VAMPIRE_FACTION).orElse(false);
        if (!vampire) {
            return "not_unlocked";
        }
        return VampirismVision.cycle(p) ? null : "vision";
    }
}
