package net.ofatech.supernaturalbloodlines.parts.hud.action;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;
import net.ofatech.supernaturalbloodlines.core.attachment.ModAttachments;
import net.ofatech.supernaturalbloodlines.core.hud.Loadout;
import net.ofatech.supernaturalbloodlines.parts.hud.net.LoadoutSyncPayload;
import org.jetbrains.annotations.Nullable;

/**
 * The server side of the six Supernatural-mode slots. The ids a player may assign are Bite, or an action the player's current faction has
 * unlocked. A loadout is never cleared when the faction changes (a cured vampire keeps the list; its slots simply cannot be used).
 */
public final class LoadoutService {
    public static final String KEY = "supernaturalbloodlines.hud.loadout.";

    private LoadoutService() {
    }

    public static Loadout get(ServerPlayer p) {
        return p.getData(ModAttachments.HUD_LOADOUT);
    }

    /**
     * What the slots do now: the stored loadout, and, when the supernatural part is off (no hub to assign in), the empty slots 2 to 6 show the first
     * unlocked actions ({@link Loadout#withFallback}). Never stored.
     */
    public static Loadout effective(net.minecraft.world.entity.player.Player p) {
        Loadout stored = p.getData(ModAttachments.HUD_LOADOUT);
        return needsFallback() ? stored.withFallback(FactionActions.unlockedIds(p)) : stored;
    }

    public static boolean needsFallback() {
        return ModList.get().isLoaded("vampirism") && !net.ofatech.supernaturalbloodlines.core.EnabledParts.isEnabled("supernatural");
    }

    /** @param id the action, null to clear the slot @return the lang key of the refusal, null when the slot was set */
    @Nullable
    public static String set(ServerPlayer p, int slot, @Nullable ResourceLocation id) {
        if (!Loadout.validSlot(slot)) {
            return KEY + "bad_slot";
        }
        if (id != null && !Loadout.BITE.equals(id)) {
            if (!ModList.get().isLoaded("vampirism") || FactionActions.byId(id) == null) {
                return KEY + "unknown_action";
            }
            if (!FactionActions.unlocked(p, id)) {
                return KEY + "not_unlocked";
            }
        }
        Loadout next = get(p).with(slot, id);
        p.setData(ModAttachments.HUD_LOADOUT, next);
        sync(p);
        return null;
    }

    /** Sends the player their own loadout (login, respawn, dimension change and every change). */
    public static void sync(ServerPlayer p) {
        PacketDistributor.sendToPlayer(p, new LoadoutSyncPayload(get(p)));
    }
}
