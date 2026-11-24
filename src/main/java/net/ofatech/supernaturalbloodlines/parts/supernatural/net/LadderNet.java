package net.ofatech.supernaturalbloodlines.parts.supernatural.net;

import net.ofatech.supernaturalbloodlines.core.supernatural.SkillTable;
import net.ofatech.supernaturalbloodlines.parts.supernatural.skill.ModSkillService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubPresence;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.EnabledParts;
import net.ofatech.supernaturalbloodlines.core.supernatural.LadderTable;
import net.ofatech.supernaturalbloodlines.parts.supernatural.LadderService;
import net.ofatech.supernaturalbloodlines.parts.supernatural.skill.SkillService;
import net.neoforged.fml.ModList;
import java.util.Map;
import net.neoforged.fml.loading.FMLEnvironment;
import java.util.WeakHashMap;

/**
 * The part's packets. They are registered as optional, so a client without this part (or a server without it) still connects. The
 * claim is the only client action: the server re-checks everything. The ladder table is sent when a player joins and after every
 * data reload ({@code OnDatapackSyncEvent} fires for both), with the server's mod and part sets so the client prunes it the same way.
 */
public final class LadderNet {
    /**
     * Optional, so a client or server without this part still connects; but the channel only exists between two sides on the same version, so a
     * server and its clients on different protocol numbers cannot talk (the hub shows nothing from the server). Raise it with any payload change.
     */
    static final String PROTOCOL = "15";

    private LadderNet() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(LadderNet::onRegister);
        NeoForge.EVENT_BUS.addListener((OnDatapackSyncEvent e) -> e.getRelevantPlayers().forEach(LadderNet::sendTable));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            HubPresence.forget(e.getEntity().getUUID());
            AdminNet.forget(e.getEntity().getUUID());
        });
        // a request opened or answered reaches the player now (the toast needs it with the hub closed), not at the next hub open
        HubRequests.onChanged(LadderNet::sendRequests);
        net.ofatech.supernaturalbloodlines.shared.hub.WardHub.onChanged(LadderNet::sendWard);
    }

    private static void onRegister(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL).optional();
        registrar.playToServer(ClaimTaskPayload.TYPE, ClaimTaskPayload.STREAM_CODEC, (payload, ctx) ->
                ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof ServerPlayer p) {
                        LadderService.claim(p, payload.taskId());
                    }
                }));
        registrar.playToServer(BuySkillPayload.TYPE, BuySkillPayload.STREAM_CODEC, (payload, ctx) ->
                ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof ServerPlayer p) {
                        SkillService.buy(p, payload.skillId());
                    }
                }));
        registrar.playToServer(UnlockModSkillPayload.TYPE, UnlockModSkillPayload.STREAM_CODEC, (payload, ctx) ->
                ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof ServerPlayer p) {
                        ModSkillService.unlock(p, payload.skillId());
                    }
                }));
        registrar.playToServer(HubSnapshotRequestPayload.TYPE, HubSnapshotRequestPayload.STREAM_CODEC, (payload, ctx) ->
                ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof ServerPlayer p) {
                        HubPresence.touch(p.getUUID());
                    }
                    if (ctx.player() instanceof ServerPlayer p && snapshotDue(p)) {
                        PacketDistributor.sendToPlayer(p, HubSnapshotPayload.of(p));
                        sendRequests(p);
                        sendWard(p);
                    }
                }));
        // the client handler is only named on a client, so a dedicated server never links the client class
        registrar.playToClient(HubSnapshotPayload.TYPE, HubSnapshotPayload.STREAM_CODEC, FMLEnvironment.dist.isClient()
                ? net.ofatech.supernaturalbloodlines.parts.supernatural.client.HubClientHandlers::snapshot
                : (payload, ctx) -> { });
        registrar.playToServer(HubOpenPayload.TYPE, HubOpenPayload.STREAM_CODEC, (payload, ctx) ->
                ctx.enqueueWork(() -> HubPresence.set(ctx.player().getUUID(), payload.open())));
        registrar.playToServer(AdminSetPayload.TYPE, AdminSetPayload.STREAM_CODEC, AdminNet::onSet);
        registrar.playToServer(AdminRequestPayload.TYPE, AdminRequestPayload.STREAM_CODEC, AdminNet::onRequest);
        registrar.playToServer(HubCommandPayload.TYPE, HubCommandPayload.STREAM_CODEC, HubCommands::onCommand);
        registrar.playToClient(AdminSnapshotPayload.TYPE, AdminSnapshotPayload.STREAM_CODEC, FMLEnvironment.dist.isClient()
                ? net.ofatech.supernaturalbloodlines.parts.supernatural.client.AdminClient::snapshot
                : (payload, ctx) -> { });
        registrar.playToClient(HubCommandReplyPayload.TYPE, HubCommandReplyPayload.STREAM_CODEC, FMLEnvironment.dist.isClient()
                ? net.ofatech.supernaturalbloodlines.parts.supernatural.client.AdminClient::reply
                : (payload, ctx) -> { });
        registrar.playToClient(HubRequestsPayload.TYPE, HubRequestsPayload.STREAM_CODEC, FMLEnvironment.dist.isClient()
                ? net.ofatech.supernaturalbloodlines.parts.supernatural.client.RequestsClient::receive
                : (payload, ctx) -> { });
        registrar.playToClient(WardViewPayload.TYPE, WardViewPayload.STREAM_CODEC, FMLEnvironment.dist.isClient()
                ? net.ofatech.supernaturalbloodlines.parts.supernatural.client.WardClient::receive
                : (payload, ctx) -> { });
        registrar.playToClient(GroupViewPayload.TYPE, GroupViewPayload.STREAM_CODEC, FMLEnvironment.dist.isClient()
                ? net.ofatech.supernaturalbloodlines.parts.supernatural.client.GroupClient::receive
                : (payload, ctx) -> { });
        registrar.playToClient(net.ofatech.supernaturalbloodlines.parts.supernatural.tweaks.TweakSync.Payload.TYPE,
                net.ofatech.supernaturalbloodlines.parts.supernatural.tweaks.TweakSync.Payload.STREAM_CODEC,
                net.ofatech.supernaturalbloodlines.parts.supernatural.tweaks.TweakSync::onClient);
        registrar.playToServer(ProfileRequestPayload.TYPE, ProfileRequestPayload.STREAM_CODEC, ProfileNet::onRequest);
        registrar.playToServer(AdminProfileRequestPayload.TYPE, AdminProfileRequestPayload.STREAM_CODEC, ProfileNet::onAdminRequest);
        registrar.playToClient(ProfilePayload.TYPE, ProfilePayload.STREAM_CODEC, FMLEnvironment.dist.isClient()
                ? net.ofatech.supernaturalbloodlines.parts.supernatural.client.ProfileClient::receive
                : (payload, ctx) -> { });
        registrar.playToClient(SkillTablePayload.TYPE, SkillTablePayload.STREAM_CODEC, (payload, ctx) ->
                ctx.enqueueWork(() -> {
                    SkillTable.Parsed parsed = payload.parseWithServerSets();
                    parsed.rejected().forEach(r -> SupernaturalBloodlines.LOGGER.warn("Skill data from the server rejected: {}", r));
                    SkillTable.setClientCopy(parsed.table());
                }));
        registrar.playToClient(LadderTablePayload.TYPE, LadderTablePayload.STREAM_CODEC, (payload, ctx) ->
                ctx.enqueueWork(() -> {
                    LadderTable.Parsed parsed = payload.parseWithServerSets();
                    parsed.rejected().forEach(r -> SupernaturalBloodlines.LOGGER.warn("Ladder from the server rejected: {}", r));
                    LadderTable.setClientCopy(parsed.table());
                }));
    }

    // server thread only; a snapshot walks the indexes, so a request within a second of the last one is dropped (the client keeps the last answer)
    private static final Map<ServerPlayer, Long> LAST_SNAPSHOT = new WeakHashMap<>();
    private static final long SNAPSHOT_COOLDOWN = 20;

    static boolean snapshotDue(ServerPlayer p) {
        long now = p.serverLevel().getGameTime();
        Long last = LAST_SNAPSHOT.get(p);
        if (last != null && now - last < SNAPSHOT_COOLDOWN && now >= last) {
            return false;
        }
        LAST_SNAPSHOT.put(p, now);
        return true;
    }

    /** Sends the player their wards (R310), if their client has the channel (a fake player's connection has none). */
    public static void sendWard(ServerPlayer p) {
        if (p.connection != null && p.connection.getConnection().isConnected() && p.connection.hasChannel(WardViewPayload.TYPE)) {
            PacketDistributor.sendToPlayer(p, WardViewPayload.of(p));
        }
    }

    /** Sends the player their open requests, if their client has the channel (a fake player's connection has none). */
    public static void sendRequests(ServerPlayer p) {
        if (p.connection != null && p.connection.getConnection().isConnected() && p.connection.hasChannel(HubRequestsPayload.TYPE)) {
            PacketDistributor.sendToPlayer(p, HubRequestsPayload.of(p));
        }
        if (p.connection != null && p.connection.getConnection().isConnected() && p.connection.hasChannel(GroupViewPayload.TYPE)) {
            PacketDistributor.sendToPlayer(p, GroupViewPayload.of(p));
        }
    }

    public static void sendTable(ServerPlayer p) {
        if (p.connection != null && p.connection.hasChannel(LadderTablePayload.TYPE)) {
            PacketDistributor.sendToPlayer(p, LadderTablePayload.of(LadderTable.current().sources(), ModList.get()::isLoaded, EnabledParts::isEnabled));
        }
        if (p.connection != null && p.connection.hasChannel(SkillTablePayload.TYPE)) {
            PacketDistributor.sendToPlayer(p, SkillTablePayload.of(SkillTable.current().sources(), ModList.get()::isLoaded, EnabledParts::isEnabled));
        }
    }
}
