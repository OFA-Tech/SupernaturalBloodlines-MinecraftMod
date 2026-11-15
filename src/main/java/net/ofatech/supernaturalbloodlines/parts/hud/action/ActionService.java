package net.ofatech.supernaturalbloodlines.parts.hud.action;

import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.ofatech.supernaturalbloodlines.core.hud.Loadout;
import net.ofatech.supernaturalbloodlines.parts.hud.bite.BiteService;
import org.jetbrains.annotations.Nullable;

/**
 * A Supernatural-mode slot key went down or up. The action is read from the player's own {@link Loadout}, never from the packet. Bite goes to
 * {@link BiteService} (press starts, release ends Vampirism's feeding). Any other action must be unlocked in the player's current faction and is
 * toggled through Vampirism's action API exactly like its own key does ({@link FactionActions#toggleFor}, which builds the context on the server).
 * Every refusal is an action bar line. This class links no Vampirism type, so it loads on a server without Vampirism.
 */
public final class ActionService {
    public static final String KEY = "supernaturalbloodlines.hud.action.";

    private ActionService() {
    }

    /** @return the refusal's lang key, null when the slot did something */
    @Nullable
    public static String trigger(ServerPlayer p, int slot, int targetEntityId, boolean release) {
        if (!Loadout.validSlot(slot)) {
            return KEY + "bad_slot";
        }
        Optional<ResourceLocation> id = LoadoutService.get(p).get(slot);
        if (id.isEmpty()) {
            return release ? null : refuse(p, KEY + "empty_slot");
        }
        if (Loadout.BITE.equals(id.get())) {
            if (release) {
                BiteService.release(p);
                return null;
            }
            return BiteService.press(p, targetEntityId).refusal();
        }
        if (release) {
            return null;
        }
        if (!ModList.get().isLoaded("vampirism")) {
            return refuse(p, BiteService.KEY + "needs_vampirism");
        }
        if (p.isSpectator() || !p.isAlive()) {
            return refuse(p, KEY + "cannot_act");
        }
        if (!FactionActions.unlocked(p, id.get())) {
            return refuse(p, KEY + "not_unlocked");
        }
        String perm = FactionActions.toggleFor(p, id.get(), targetEntityId);
        return perm == null ? null : refuse(p, KEY + perm);
    }

    private static String refuse(ServerPlayer p, String key) {
        p.displayClientMessage(Component.translatable(key), true);
        return key;
    }
}
