package net.ofatech.supernaturalbloodlines.parts.speciestensura;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/**
 * Tensura's undead checks and the evolution screen also run on each client, which reads {@code layers}, {@code undead_treatment_enabled}
 * and {@code apex_evolutions_enabled} from its own file. The server sends its three values to every client when they join and whenever the
 * server's file is saved, and the client uses them instead of its own until it leaves the world. Optional channel: a client without it keeps its file.
 */
public final class SpeciesTensuraSync {
    public record Payload(List<String> layers, boolean undead, boolean apex) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "species_tensura_sync"));
        public static final StreamCodec<ByteBuf, Payload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(512).apply(ByteBufCodecs.list(256)), Payload::layers,
                ByteBufCodecs.BOOL, Payload::undead,
                ByteBufCodecs.BOOL, Payload::apex,
                Payload::new);

        public Payload {
            layers = List.copyOf(layers);
        }

        @Override
        public Type<Payload> type() {
            return TYPE;
        }

        /** The server's own file, never what another server pushed to it. */
        public static Payload ofLocal() {
            return new Payload(SpeciesTensuraConfig.localLayers(), SpeciesTensuraConfig.localUndeadTreatment(), SpeciesTensuraConfig.localApexEnabled());
        }
    }

    private SpeciesTensuraSync() {}

    static void register(IEventBus modBus) {
        modBus.addListener((RegisterPayloadHandlersEvent e) -> e.registrar("1").optional().playToClient(Payload.TYPE, Payload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> SpeciesTensuraConfig.applyPushed(payload.layers(), payload.undead(), payload.apex()))));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                send(p);
            }
        });
        // the server's file was saved (by the hub or by hand): every client in the world gets the new values
        modBus.addListener((ModConfigEvent.Reloading e) -> {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && e.getConfig().getSpec() == SpeciesTensuraConfig.SPEC) {
                server.execute(() -> server.getPlayerList().getPlayers().forEach(SpeciesTensuraSync::send));
            }
        });
        if (FMLEnvironment.dist.isClient()) {
            SpeciesTensuraClient.register();
        }
    }

    static void send(ServerPlayer p) {
        // a fake player's connection has no channel to ask
        if (p.connection != null && p.connection.getConnection().isConnected() && p.connection.hasChannel(Payload.TYPE)) {
            PacketDistributor.sendToPlayer(p, Payload.ofLocal());
        }
    }
}
