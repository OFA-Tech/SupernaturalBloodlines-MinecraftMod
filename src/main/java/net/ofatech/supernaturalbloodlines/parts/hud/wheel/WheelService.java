package net.ofatech.supernaturalbloodlines.parts.hud.wheel;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.hud.Loadout;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerRef;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerSource;
import net.ofatech.supernaturalbloodlines.parts.hud.HudConfig;
import net.ofatech.supernaturalbloodlines.parts.hud.action.ActionService;
import net.ofatech.supernaturalbloodlines.shared.hud.ExtraActions;
import org.jetbrains.annotations.Nullable;

/**
 * The server side of the wheel's use and mode requests. <b>The wheel only asks</b>: every use goes through the path the power already has (our and the factions' actions through
 * {@link ActionService#triggerById}, Tensura through ManasCore's own skill calls, see {@link TensuraUse}), and the server looks the power up in what the player really has, so a
 * forged or stale request gets the same refusal as the slot or key would. A refusal is one action bar line and its lang key is returned; nothing else happens.
 * Held powers (a bite, a Tensura skill, spell or art, the race ability) are remembered, so a release counts only for a press that was accepted and everything ends on logout,
 * death, a dimension change or being put out of action ({@link #forget}). Links no other mod: Vampirism and Tensura are reached through their gates.
 */
public final class WheelService {
    public static final String KEY = "supernaturalbloodlines.wheel.refused.";
    /** Vampirism's vision pseudo power; named here so this class never loads a Vampirism type. */
    public static final net.minecraft.resources.ResourceLocation VISION_ID = ResourceLocation.fromNamespaceAndPath("vampirism", "vision");

    /** Per player: the held Tensura powers (ref key to the mode it was started in). */
    private static final Map<UUID, Map<String, Integer>> HELD = new ConcurrentHashMap<>();

    private WheelService() {
    }

    /**
     * @param mode the mode to run (a Tensura skill with modes; checked against the skill's modes)
     * @return the refusal's lang key (already shown in the action bar), null when the power did something
     */
    @Nullable
    public static String use(ServerPlayer p, PowerRef ref, int mode, int targetEntityId, boolean release) {
        if (release) {
            return releasePower(p, ref);
        }
        if (!HudConfig.wheelEnabled()) {
            return refuse(p, KEY + "wheel_disabled");
        }
        if (ActionService.cannotAct(p)) {
            return refuse(p, ActionService.KEY + "cannot_act");
        }
        return switch (ref.source()) {
            case BLOODLINE, VAMPIRISM, WEREWOLVES -> useAction(p, ref, targetEntityId);
            case TENSURA -> useTensura(p, ref, mode);
            case INFINITY, GREAT_SAGE -> refuse(p, KEY + "not_server_power");
        };
    }

    @Nullable
    private static String useAction(ServerPlayer p, PowerRef ref, int targetEntityId) {
        ResourceLocation id = ref.id();
        if (ref.source() == PowerSource.BLOODLINE) {
            if (!Loadout.BITE.equals(id) && ExtraActions.get(id).isEmpty()) {
                return refuse(p, KEY + "unknown_power");
            }
            return ActionService.triggerById(p, id, targetEntityId, false);
        }
        PowerSource real = VampirismGate.sourceOf(id);
        if (real != ref.source()) {
            return refuse(p, KEY + "unknown_power");
        }
        if (id.equals(VISION_ID)) {
            String refusal = VampirismGate.cycleVision(p);
            return refusal == null ? null : refuse(p, KEY + refusal);
        }
        return ActionService.triggerById(p, id, targetEntityId, false);
    }

    @Nullable
    private static String useTensura(ServerPlayer p, PowerRef ref, int mode) {
        String key = ref.key();
        // a second press of a power that is still held would overwrite the mode its release must use: refuse it. An entry whose power ended by itself is stale and dropped.
        Map<String, Integer> held = HELD.get(p.getUUID());
        Integer heldMode = held == null ? null : held.get(key);
        if (heldMode != null) {
            if (TensuraGate.held(p, ref.id(), heldMode)) {
                return refuse(p, KEY + "already_held");
            }
            held.remove(key);
        }
        String refusal = TensuraGate.press(p, ref.id(), mode);
        if (refusal != null) {
            return refuse(p, refusal);
        }
        if (TensuraGate.held(p, ref.id(), mode)) {
            HELD.computeIfAbsent(p.getUUID(), k -> new ConcurrentHashMap<>()).put(key, mode);
        }
        return null;
    }

    @Nullable
    private static String releasePower(ServerPlayer p, PowerRef ref) {
        switch (ref.source()) {
            case BLOODLINE, VAMPIRISM, WEREWOLVES -> ActionService.triggerById(p, ref.id(), -1, true);
            case TENSURA -> {
                Map<String, Integer> held = HELD.get(p.getUUID());
                Integer mode = held == null ? null : held.remove(ref.key());
                if (mode != null) {
                    TensuraGate.release(p, ref.id(), mode);
                }
            }
            default -> {
            }
        }
        return null;
    }

    /** @return the refusal's lang key (already shown), null when the mode moved */
    @Nullable
    public static String changeMode(ServerPlayer p, PowerRef ref, int delta) {
        if (!HudConfig.wheelEnabled()) {
            return refuse(p, KEY + "wheel_disabled");
        }
        if (ActionService.cannotAct(p)) {
            return refuse(p, ActionService.KEY + "cannot_act");
        }
        if (ref.source() != PowerSource.TENSURA) {
            return refuse(p, KEY + "no_modes");
        }
        String refusal = TensuraGate.changeMode(p, ref.id(), Integer.signum(delta) < 0 ? -1 : 1);
        return refusal == null ? null : refuse(p, KEY + refusal);
    }

    /** Ends everything the wheel started for this player: the held Tensura powers, a bite, the race ability, the hold bracket and the open wheel. */
    public static void forget(ServerPlayer p) {
        Map<String, Integer> held = HELD.remove(p.getUUID());
        if (held != null) {
            for (Map.Entry<String, Integer> e : new HashMap<>(held).entrySet()) {
                PowerRef.parse(e.getKey()).ifPresent(ref -> TensuraGate.release(p, ref.id(), e.getValue()));
            }
        }
        TensuraGate.forget(p);
        if (ActionService.heldId(p, Loadout.BITE)) {
            net.ofatech.supernaturalbloodlines.parts.hud.bite.BiteService.release(p);
        }
        ActionService.forget(p);
        WheelSync.close(p);
    }

    /** Test seam: what the wheel thinks is held for this player. */
    public static boolean heldTensura(ServerPlayer p, PowerRef ref) {
        Map<String, Integer> held = HELD.get(p.getUUID());
        return held != null && held.containsKey(ref.key());
    }

    private static String refuse(ServerPlayer p, String key) {
        p.displayClientMessage(Component.translatable(key), true);
        return key;
    }
}
