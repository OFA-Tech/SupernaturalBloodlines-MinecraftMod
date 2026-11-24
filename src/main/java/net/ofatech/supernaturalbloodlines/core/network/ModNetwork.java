package net.ofatech.supernaturalbloodlines.core.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.ofatech.supernaturalbloodlines.client.EclipseClient;
import net.ofatech.supernaturalbloodlines.client.EyeColorClient;
import net.ofatech.supernaturalbloodlines.client.HallucinationClient;
import net.ofatech.supernaturalbloodlines.client.HunterSenseClient;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.attachment.ModAttachments;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess;

public final class ModNetwork {
    /**
     * The version of this mod's required payloads. A client of another version is refused at login, with the version mismatch screen (NeoForge compares it). Bump it
     * whenever a synced field of an entity or an attachment is added or removed too: the game's own entity-data ids are positional, so an old client that is given a
     * new id reads past the end of its table and is kicked with an obscure error instead.
     * <ul>
     *   <li>"2": the Beast of Gevaudan synced two more entity data entries (form and shift ticks) for the morph (UI overhaul phase 7).</li>
     * </ul>
     */
    public static final String PROTOCOL = "2";

    private ModNetwork() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(ModNetwork::onRegisterPayloads);
        NeoForge.EVENT_BUS.addListener(ModNetwork::onLogin);
        NeoForge.EVENT_BUS.addListener(ModNetwork::onRespawn);
        NeoForge.EVENT_BUS.addListener(ModNetwork::onChangedDimension);
        NeoForge.EVENT_BUS.addListener(ModNetwork::onStartTracking);
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> SupernaturalAccess.flush());
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL);
        registrar.playToClient(BloodlineSyncPayload.TYPE, BloodlineSyncPayload.STREAM_CODEC, (payload, ctx) ->
                ctx.enqueueWork(() -> {
                    if (ctx.player().level().getEntity(payload.entityId()) instanceof Player target) {
                        target.setData(ModAttachments.BLOODLINE, payload.state());
                    }
                }));
        registrar.playToClient(SupernaturalSyncPayload.TYPE, SupernaturalSyncPayload.STREAM_CODEC, (payload, ctx) ->
                ctx.enqueueWork(() -> ctx.player().setData(ModAttachments.SUPERNATURAL, payload.state())));
        // The handler class is client only; this lambda runs only when the packet arrives, which a dedicated server never receives.
        registrar.playToClient(HallucinationPayload.TYPE, HallucinationPayload.STREAM_CODEC, (payload, ctx) ->
                ctx.enqueueWork(() -> HallucinationClient.show(payload.kind())));
        registrar.playToClient(GlowPayload.TYPE, GlowPayload.STREAM_CODEC, (payload, ctx) ->
                ctx.enqueueWork(() -> HunterSenseClient.show(payload.ids())));
        registrar.playToClient(EyeColorPayload.TYPE, EyeColorPayload.STREAM_CODEC, (payload, ctx) ->
                ctx.enqueueWork(() -> EyeColorClient.set(payload.playerId(), payload.color())));
        registrar.playToClient(EclipsePayload.TYPE, EclipsePayload.STREAM_CODEC, (payload, ctx) ->
                ctx.enqueueWork(() -> EclipseClient.set(payload.active())));
    }

    // A new client player object (login, respawn, dimension change) starts with NONE, so push the state again.
    private static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            BloodlineAccess.syncToSelf(p);
            SupernaturalAccess.syncToSelf(p);
        }
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            BloodlineAccess.syncToSelf(p);
            SupernaturalAccess.syncToSelf(p);
        }
    }

    private static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            BloodlineAccess.syncToSelf(p);
            SupernaturalAccess.syncToSelf(p);
        }
    }

    private static void onStartTracking(PlayerEvent.StartTracking e) {
        if (e.getTarget() instanceof Player target && e.getEntity() instanceof ServerPlayer watcher) {
            BloodlineAccess.sendTo(watcher, target);
        }
    }
}
