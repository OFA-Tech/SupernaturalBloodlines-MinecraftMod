package net.ofatech.supernaturalbloodlines.parts.hud.action;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import net.ofatech.supernaturalbloodlines.core.hud.OfferNotice;
import net.neoforged.neoforge.network.PacketDistributor;
import net.ofatech.supernaturalbloodlines.core.attachment.ModAttachments;
import net.ofatech.supernaturalbloodlines.core.hud.Loadout;
import net.ofatech.supernaturalbloodlines.parts.hud.net.LoadoutSyncPayload;
import net.ofatech.supernaturalbloodlines.shared.hud.ExtraActions;
import org.jetbrains.annotations.Nullable;

/**
 * The server side of the six Supernatural-mode slots. The ids a player may assign are Bite, or an action the player's current faction has
 * unlocked. A loadout is never cleared when the faction changes (a cured vampire keeps the list; its slots simply cannot be used).
 */
public final class LoadoutService {
    public static final String KEY = "supernaturalbloodlines.hud.loadout.";

    private LoadoutService() {
    }

    /** Once a second per player: what a player may have is put in an empty slot, once (see {@link #offerDefaults}). */
    public static void register() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e) ->
                FULL_TOLD.removeIf(k -> k.startsWith(e.getEntity().getUUID() + "/")));
        // a player who logs in during a cooldown gets the rest of it, so the slot's sweep is right from the first frame
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                for (ExtraActions.Action a : ExtraActions.all()) {
                    int left = a.cooldownLeft(p);
                    if (left > 0) {
                        net.ofatech.supernaturalbloodlines.parts.hud.net.ActionCooldownPayload.send(p, a.id(), left);
                    }
                }
            }
        });
        ExtraActions.onCooldown(net.ofatech.supernaturalbloodlines.parts.hud.net.ActionCooldownPayload::send);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.PlayerTickEvent.Post e) -> {
            if (e.getEntity() instanceof ServerPlayer p && p.tickCount % 20 == 0 && !(p instanceof net.neoforged.neoforge.common.util.FakePlayer)) {
                offerDefaults(p);
            }
        });
    }

    /**
     * Gives a player what they are now: Bite in an empty slot 1 for a type that can bite, then every {@link ExtraActions} they may use (the Alpha Roar for an
     * Alpha) in the first empty slot, each once. A human gets nothing, a slot the player set is never overwritten, and an action the player cleared is never put back.
     */
    public static void offerDefaults(ServerPlayer p) {
        Loadout before = get(p);
        Loadout next = before;
        if (!next.biteOffered() && net.ofatech.supernaturalbloodlines.parts.hud.bite.BiteService.canBite(p)) {
            next = next.withBiteOffered();
        }
        java.util.List<Component> told = new java.util.ArrayList<>();
        for (ExtraActions.Action a : ExtraActions.all()) {
            if (!next.offered().contains(a.id()) && a.available(p)) {
                Loadout after = next.withActionOffered(a.id());
                OfferNotice notice = OfferNotice.of(next, after, a.id());
                if (notice.kind() == OfferNotice.Kind.PLACED) {
                    told.add(Component.translatable(KEY_OFFER + "placed", a.name(), notice.slot() + 1));
                } else if (notice.kind() == OfferNotice.Kind.FULL && FULL_TOLD.add(p.getUUID() + "/" + a.id())) {
                    told.add(Component.translatable(KEY_OFFER + "full", a.name()));
                }
                next = after;
            }
        }
        if (!next.equals(before)) {
            p.setData(ModAttachments.HUD_LOADOUT, next);
            sync(p);
        }
        // one line per player and action: a full loadout is checked every second, and the player must not be nagged
        told.forEach(m -> p.displayClientMessage(m, true));
    }

    private static final String KEY_OFFER = "supernaturalbloodlines.hud.offer.";
    /** "player/action" pairs already told that there was no room, for this session. */
    private static final java.util.Set<String> FULL_TOLD = java.util.concurrent.ConcurrentHashMap.newKeySet();

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
        java.util.Optional<ExtraActions.Action> extra = id == null ? java.util.Optional.empty() : ExtraActions.get(id);
        if (extra.isPresent()) {
            if (!extra.get().available(p)) {
                return KEY + "not_unlocked";
            }
        } else if (id != null && !Loadout.BITE.equals(id)) {
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
