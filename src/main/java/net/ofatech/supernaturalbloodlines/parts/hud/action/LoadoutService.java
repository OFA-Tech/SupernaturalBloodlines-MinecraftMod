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

    /** Once a second per player: a player who can bite gets Bite in an empty slot 1 (see {@link #offerBite}). */
    public static void register() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.PlayerTickEvent.Post e) -> {
            if (e.getEntity() instanceof ServerPlayer p && p.tickCount % 20 == 0 && !(p instanceof net.neoforged.neoforge.common.util.FakePlayer)) {
                offerBite(p);
            }
        });
    }

    /**
     * Gives Bite to a player who becomes a type that can bite (vampire, werewolf, and the hybrid types that bite), once, in slot 1, only if that slot is
     * empty. A human gets nothing, a slot the player set is never overwritten, and a Bite the player cleared is never put back.
     */
    public static void offerBite(ServerPlayer p) {
        Loadout before = get(p);
        if (before.biteOffered() || !net.ofatech.supernaturalbloodlines.parts.hud.bite.BiteService.canBite(p)) {
            return;
        }
        p.setData(ModAttachments.HUD_LOADOUT, before.withBiteOffered());
        sync(p);
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
        if (canReceive(p)) {
            PacketDistributor.sendToPlayer(p, new LoadoutSyncPayload(get(p)));
        }
    }

    /** Fake and mock players, and clients without this mod's channel, cannot take the payload: sending would throw. */
    public static boolean canReceive(ServerPlayer p) {
        return !(p instanceof net.neoforged.neoforge.common.util.FakePlayer) && p.connection != null && p.connection.hasChannel(LoadoutSyncPayload.TYPE);
    }
}
