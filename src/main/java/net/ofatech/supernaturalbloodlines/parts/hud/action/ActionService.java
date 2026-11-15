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

    /** Slots (as a bit mask) whose press was accepted and whose key is still down, per player: a release with nothing held is ignored. */
    private static final java.util.Map<java.util.UUID, Integer> HELD = new java.util.concurrent.ConcurrentHashMap<>();

    private ActionService() {
    }

    /** True while a press of this slot was accepted and not yet released. */
    public static boolean held(ServerPlayer p, int slot) {
        return (HELD.getOrDefault(p.getUUID(), 0) & (1 << slot)) != 0;
    }

    /** Also for a player who logged out or died with a key down: nothing is held any more. */
    public static void forget(ServerPlayer p) {
        HELD.remove(p.getUUID());
    }

    private static void mark(ServerPlayer p, int slot, boolean down) {
        HELD.merge(p.getUUID(), down ? 1 << slot : 0, (old, bit) -> down ? old | (1 << slot) : old & ~(1 << slot));
    }

    /** @return the refusal's lang key, null when the slot did something */
    @Nullable
    public static String trigger(ServerPlayer p, int slot, int targetEntityId, boolean release) {
        if (!Loadout.validSlot(slot)) {
            return KEY + "bad_slot";
        }
        if (release) {
            // a release counts only for a press that was accepted: nothing held, nothing to end (the rate limit does not apply to releases)
            if (!held(p, slot)) {
                return null;
            }
            mark(p, slot, false);
        }
        Optional<ResourceLocation> id = LoadoutService.effective(p).get(slot);
        if (id.isEmpty()) {
            return release ? null : refuse(p, KEY + "empty_slot");
        }
        if (Loadout.BITE.equals(id.get())) {
            if (release) {
                BiteService.release(p);
                return null;
            }
            String refusal = BiteService.press(p, targetEntityId).refusal();
            if (refusal == null) {
                mark(p, slot, true);
            }
            return refusal;
        }
        if (release) {
            return null;
        }
        if (!ModList.get().isLoaded("vampirism")) {
            return refuse(p, BiteService.KEY + "needs_vampirism");
        }
        // asleep too, like Bite: an action key does nothing from a bed
        if (p.isSpectator() || !p.isAlive() || p.isSleeping()) {
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
