package net.ofatech.supernaturalbloodlines.parts.supernatural.net;

import io.netty.buffer.ByteBuf;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Genes;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial;

/**
 * Server to client, answering {@link HubSnapshotRequestPayload}: what the hub shows about other players and the world. Only names,
 * ranks and online flags of others; the genes are the subject's own, as their client may know them (unknown until discovered).
 */
public record HubSnapshotPayload(Optional<String> sire, boolean severed, List<String> sired, List<HubSocial.Member> pack, int fiveCount,
                                 Map<String, Genes.Knowledge> genes) implements CustomPacketPayload {
    public static final Type<HubSnapshotPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "hub_snapshot"));

    public static final HubSnapshotPayload EMPTY = new HubSnapshotPayload(Optional.empty(), false, List.of(), List.of(), 0, Map.of());

    private static final int MAX_NAMES = 256;
    private static final StreamCodec<ByteBuf, String> NAME = ByteBufCodecs.stringUtf8(64);

    private static final StreamCodec<ByteBuf, HubSocial.Member> MEMBER = StreamCodec.composite(
            NAME, HubSocial.Member::name,
            ByteBufCodecs.stringUtf8(16), HubSocial.Member::rank,
            ByteBufCodecs.BOOL, HubSocial.Member::online,
            HubSocial.Member::new);

    private static final StreamCodec<ByteBuf, Genes.Knowledge> KNOWLEDGE = ByteBufCodecs.stringUtf8(16).map(
            s -> Genes.Knowledge.valueOf(s.toUpperCase(Locale.ROOT)), k -> k.name().toLowerCase(Locale.ROOT));

    public static final StreamCodec<ByteBuf, HubSnapshotPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(NAME), HubSnapshotPayload::sire,
            ByteBufCodecs.BOOL, HubSnapshotPayload::severed,
            NAME.apply(ByteBufCodecs.list(MAX_NAMES)), HubSnapshotPayload::sired,
            MEMBER.apply(ByteBufCodecs.list(MAX_NAMES)), HubSnapshotPayload::pack,
            ByteBufCodecs.VAR_INT, HubSnapshotPayload::fiveCount,
            ByteBufCodecs.map(LinkedHashMap::new, ByteBufCodecs.stringUtf8(32), KNOWLEDGE, 8), HubSnapshotPayload::genes,
            HubSnapshotPayload::new);

    public HubSnapshotPayload {
        // the codec caps a list: a sire of hundreds is cut, not a crash on the client
        sired = List.copyOf(sired.subList(0, Math.min(sired.size(), MAX_NAMES)));
        pack = List.copyOf(pack.subList(0, Math.min(pack.size(), MAX_NAMES)));
        genes = Map.copyOf(genes);
    }

    /** The snapshot for {@code p}, built only from {@code p}'s own state and the indexes' names. */
    public static HubSnapshotPayload of(ServerPlayer p) {
        BloodlineState own = BloodlineAccess.get(p);
        BloodlineState view = BloodlineAccess.clientView(own, true);
        Map<String, Genes.Knowledge> genes = new LinkedHashMap<>();
        Genes.ALL.forEach(g -> genes.put(g, Genes.ofClient(view, g)));
        HubSocial.Social social = HubSocial.of(p);
        return new HubSnapshotPayload(social.sire(), own.bondBroken(), social.sired(), social.pack(), social.fiveCount(), genes);
    }

    @Override
    public Type<HubSnapshotPayload> type() {
        return TYPE;
    }
}
