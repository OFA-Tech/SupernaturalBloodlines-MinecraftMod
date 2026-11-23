package net.ofatech.supernaturalbloodlines.parts.hud.action;

import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.ofatech.supernaturalbloodlines.core.hud.Loadout;
import net.ofatech.supernaturalbloodlines.core.wheel.ActRules;
import net.ofatech.supernaturalbloodlines.parts.hud.bite.BiteService;
import net.ofatech.supernaturalbloodlines.shared.hud.ExtraActions;
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
    /** The same by action id, for the wheel (it has no slot): ids whose press was accepted and not yet released. */
    private static final java.util.Map<java.util.UUID, java.util.Set<ResourceLocation>> HELD_IDS = new java.util.concurrent.ConcurrentHashMap<>();

    private ActionService() {
    }

    /** True while a press of this slot was accepted and not yet released. */
    public static boolean held(ServerPlayer p, int slot) {
        return (HELD.getOrDefault(p.getUUID(), 0) & (1 << slot)) != 0;
    }

    /** True while a press of this action (from a slot or the wheel) was accepted and not yet released. */
    public static boolean heldId(ServerPlayer p, ResourceLocation id) {
        java.util.Set<ResourceLocation> ids = HELD_IDS.get(p.getUUID());
        return ids != null && ids.contains(id);
    }

    /** Also for a player who logged out or died with a key down: nothing is held any more. */
    public static void forget(ServerPlayer p) {
        HELD.remove(p.getUUID());
        HELD_IDS.remove(p.getUUID());
    }

    private static void mark(ServerPlayer p, int slot, boolean down) {
        HELD.merge(p.getUUID(), down ? 1 << slot : 0, (old, bit) -> down ? old | (1 << slot) : old & ~(1 << slot));
    }

    private static void markId(ServerPlayer p, ResourceLocation id, boolean down) {
        if (down) {
            HELD_IDS.computeIfAbsent(p.getUUID(), k -> java.util.concurrent.ConcurrentHashMap.newKeySet()).add(id);
        } else {
            HELD_IDS.computeIfPresent(p.getUUID(), (k, ids) -> {
                ids.remove(id);
                return ids.isEmpty() ? null : ids;
            });
        }
    }

    /** The one rule for "cannot use anything": put out of action by a part (downed, snapped), a spectator, dead or asleep. */
    public static boolean cannotAct(ServerPlayer p) {
        return ActRules.cannotAct(ExtraActions.blocked(p), p.isSpectator(), p.isAlive(), p.isSleeping());
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
        // downed, snapped or in Vampirism's dying state: no slot does anything (Bite and the faction actions too, not only the extra ones); a release still ends a held key
        if (!release && cannotAct(p)) {
            return refuse(p, KEY + "cannot_act");
        }
        Optional<ResourceLocation> id = LoadoutService.effective(p).get(slot);
        if (id.isEmpty()) {
            return release ? null : refuse(p, KEY + "empty_slot");
        }
        return run(p, id.get(), slot, targetEntityId, release);
    }

    /**
     * The same as {@link #trigger} for an action named by id (the wheel has no slot). Every check, refusal and the held mark are the slot path's: it resolves the
     * id and calls the same body. The id must still be one the player has (the server checks, whoever asks).
     */
    @Nullable
    public static String triggerById(ServerPlayer p, ResourceLocation id, int targetEntityId, boolean release) {
        if (release) {
            if (!heldId(p, id)) {
                return null;
            }
        } else if (cannotAct(p)) {
            return refuse(p, KEY + "cannot_act");
        }
        return run(p, id, -1, targetEntityId, release);
    }

    /** The body after the action is known; {@code slot} is -1 for the wheel. */
    @Nullable
    private static String run(ServerPlayer p, ResourceLocation id, int slot, int targetEntityId, boolean release) {
        if (release) {
            markId(p, id, false);
        }
        if (Loadout.BITE.equals(id)) {
            if (release) {
                BiteService.release(p);
                return null;
            }
            String refusal = BiteService.press(p, targetEntityId).refusal();
            if (refusal == null) {
                if (slot >= 0) {
                    mark(p, slot, true);
                }
                markId(p, id, true);
            }
            return refusal;
        }
        if (release) {
            return null;
        }
        Optional<ExtraActions.Action> extra = ExtraActions.get(id);
        if (extra.isPresent()) {
            return runExtra(p, extra.get(), targetEntityId);
        }
        if (!ModList.get().isLoaded("vampirism")) {
            return refuse(p, BiteService.KEY + "needs_vampirism");
        }
        if (!FactionActions.unlocked(p, id)) {
            return refuse(p, KEY + "not_unlocked");
        }
        String perm = FactionActions.toggleFor(p, id, targetEntityId);
        return perm == null ? null : refuse(p, KEY + perm);
    }
    /** An action another part owns (the Alpha Roar): it keeps its own checks; this only asks, shows its refusal, and tells the client the cooldown it started. */
    @Nullable
    private static String runExtra(ServerPlayer p, ExtraActions.Action action, int targetEntityId) {
        if (cannotAct(p)) {
            return refuse(p, KEY + "cannot_act");
        }
        if (!action.available(p)) {
            return refuse(p, KEY + "not_unlocked");
        }
        Component error = action.trigger(p, HudTargets.resolve(p, targetEntityId, action.targetReach()).orElse(null));
        if (error != null) {
            p.displayClientMessage(error, true);
            return error.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t ? t.getKey() : KEY + "refused";
        }
        // the cooldown reaches the slot through ExtraActions.cooldownStarted, from the owner of the action, whichever path started it
        return null;
    }

    private static String refuse(ServerPlayer p, String key) {
        p.displayClientMessage(Component.translatable(key), true);
        return key;
    }
}
