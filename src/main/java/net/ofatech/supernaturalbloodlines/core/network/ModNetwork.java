package net.ofatech.supernaturalbloodlines.core.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.attachment.ModAttachments;

public final class ModNetwork {
    private static final String PROTOCOL = "1";

    private ModNetwork() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(ModNetwork::onRegisterPayloads);
        NeoForge.EVENT_BUS.addListener(ModNetwork::onLogin);
        NeoForge.EVENT_BUS.addListener(ModNetwork::onRespawn);
        NeoForge.EVENT_BUS.addListener(ModNetwork::onChangedDimension);
        NeoForge.EVENT_BUS.addListener(ModNetwork::onStartTracking);
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL);
        registrar.playToClient(BloodlineSyncPayload.TYPE, BloodlineSyncPayload.STREAM_CODEC, (payload, ctx) ->
                ctx.enqueueWork(() -> {
                    if (ctx.player().level().getEntity(payload.entityId()) instanceof Player target) {
                        target.setData(ModAttachments.BLOODLINE, payload.state());
                    }
                }));
    }

    // A new client player object (login, respawn, dimension change) starts with NONE, so push the state again.
    private static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            BloodlineAccess.syncToSelf(p);
        }
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            BloodlineAccess.syncToSelf(p);
        }
    }

    private static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            BloodlineAccess.syncToSelf(p);
        }
    }

    private static void onStartTracking(PlayerEvent.StartTracking e) {
        if (e.getTarget() instanceof Player target && e.getEntity() instanceof ServerPlayer watcher) {
            BloodlineAccess.sendTo(watcher, target);
        }
    }
}
