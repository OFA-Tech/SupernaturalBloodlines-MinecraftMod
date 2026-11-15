package net.ofatech.supernaturalbloodlines.parts.supernatural.net;

import net.ofatech.supernaturalbloodlines.core.supernatural.SkillTable;
import net.ofatech.supernaturalbloodlines.parts.supernatural.skill.ModSkillService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
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
import java.util.WeakHashMap;

/**
 * The part's packets. They are registered as optional, so a client without this part (or a server without it) still connects. The
 * claim is the only client action: the server re-checks everything. The ladder table is sent when a player joins and after every
 * data reload ({@code OnDatapackSyncEvent} fires for both), with the server's mod and part sets so the client prunes it the same way.
 */
public final class LadderNet {
    private static final String PROTOCOL = "1";

    private LadderNet() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(LadderNet::onRegister);
        NeoForge.EVENT_BUS.addListener((OnDatapackSyncEvent e) -> e.getRelevantPlayers().forEach(LadderNet::sendTable));
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
                        PacketDistributor.sendToPlayer(p, snapshot(p));
                    }
                }));
        registrar.playToClient(HubSnapshotPayload.TYPE, HubSnapshotPayload.STREAM_CODEC, (payload, ctx) ->
                ctx.enqueueWork(() -> net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubClient.receive(payload)));
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

    private record Sent(long time, HubSnapshotPayload payload) {
    }

    // server thread only; a snapshot walks the indexes, so a client that floods requests gets the same answer again for a second
    private static final Map<ServerPlayer, Sent> LAST_SNAPSHOT = new WeakHashMap<>();
    private static final long SNAPSHOT_COOLDOWN = 20;

    private static HubSnapshotPayload snapshot(ServerPlayer p) {
        long now = p.serverLevel().getGameTime();
        Sent last = LAST_SNAPSHOT.get(p);
        if (last != null && now - last.time() < SNAPSHOT_COOLDOWN && now >= last.time()) {
            return last.payload();
        }
        HubSnapshotPayload fresh = HubSnapshotPayload.of(p);
        LAST_SNAPSHOT.put(p, new Sent(now, fresh));
        return fresh;
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
