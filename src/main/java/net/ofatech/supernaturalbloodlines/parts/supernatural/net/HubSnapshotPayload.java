package net.ofatech.supernaturalbloodlines.parts.supernatural.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
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
import net.ofatech.supernaturalbloodlines.core.bloodline.LineRules;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial;

/**
 * Server to client, answering {@link HubSnapshotRequestPayload}: what the hub shows about other players and the world. Only names,
 * ranks and online flags of others; the genes are the subject's own, as their client may know them (unknown until discovered).
 */
public record HubSnapshotPayload(Family family, List<HubSocial.Member> pack, int fiveCount, Map<String, Genes.Knowledge> genes,
                                 List<HubSocial.Rite> rites, List<HubSocial.Person> people) implements CustomPacketPayload {
    public HubSnapshotPayload(Family family, List<HubSocial.Member> pack, int fiveCount, Map<String, Genes.Knowledge> genes, List<HubSocial.Rite> rites) {
        this(family, pack, fiveCount, genes, rites, List.of());
    }

    /**
     * The player's line: the sire (and whether the bond is severed), the sire's sire and up, those the player sired, and everyone below.
     * Names only.
     */
    public record Family(Optional<String> sire, boolean severed, List<String> sired, List<String> ancestors, List<HubSocial.Kin> line,
                         HubSocial.Origin origin) {
        public static final Family NONE = new Family(Optional.empty(), false, List.of(), List.of(), List.of(), HubSocial.Origin.NONE);

        public Family {
            sired = cap(sired);
            ancestors = cap(ancestors);
            line = cap(line);
        }
    }

    public static final Type<HubSnapshotPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "hub_snapshot"));

    public static final HubSnapshotPayload EMPTY = new HubSnapshotPayload(Family.NONE, List.of(), 0, Map.of(), List.of());

    private static final int MAX_NAMES = 256;
    private static final StreamCodec<ByteBuf, String> NAME = ByteBufCodecs.stringUtf8(64);
    private static final StreamCodec<ByteBuf, String> ID = ByteBufCodecs.stringUtf8(128);

    private static final StreamCodec<ByteBuf, HubSocial.Person> PERSON = StreamCodec.composite(
            NAME, HubSocial.Person::name,
            UUIDUtil.STREAM_CODEC, HubSocial.Person::id,
            HubSocial.Person::new);

    private static final StreamCodec<ByteBuf, HubSocial.Member> MEMBER = StreamCodec.composite(
            NAME, HubSocial.Member::name,
            ByteBufCodecs.stringUtf8(32), HubSocial.Member::rank,
            ByteBufCodecs.BOOL, HubSocial.Member::online,
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), HubSocial.Member::id,
            HubSocial.Member::new);

    private static final StreamCodec<ByteBuf, HubSocial.Kin> KIN = StreamCodec.composite(
            NAME, HubSocial.Kin::name,
            ByteBufCodecs.VAR_INT, HubSocial.Kin::depth,
            HubSocial.Kin::new);

    private static final StreamCodec<ByteBuf, HubSocial.Rite> RITE = StreamCodec.composite(
            ID, HubSocial.Rite::id,
            ByteBufCodecs.VAR_INT, HubSocial.Rite::power,
            ID.apply(ByteBufCodecs.list(32)), HubSocial.Rite::items,
            ID.apply(ByteBufCodecs.list(8)), HubSocial.Rite::shapes,
            ID, HubSocial.Rite::factory,
            ByteBufCodecs.stringUtf8(64), HubSocial.Rite::faction,
            HubSocial.Rite::new);

    private static final StreamCodec<ByteBuf, LineRules.State> LINE_STATE = ByteBufCodecs.stringUtf8(16).map(
            s -> {
                try {
                    return LineRules.State.valueOf(s);
                } catch (IllegalArgumentException unknown) {
                    return LineRules.State.NOT_VAMPIRE;
                }
            }, LineRules.State::name);

    private static final StreamCodec<ByteBuf, HubSocial.FreedKind> FREED_KIND = ByteBufCodecs.stringUtf8(16).map(
            s -> {
                try {
                    return HubSocial.FreedKind.valueOf(s);
                } catch (IllegalArgumentException unknown) {
                    return HubSocial.FreedKind.NONE;
                }
            }, HubSocial.FreedKind::name);

    private static final StreamCodec<ByteBuf, HubSocial.Origin> ORIGIN = StreamCodec.composite(
            LINE_STATE, HubSocial.Origin::state,
            ByteBufCodecs.optional(NAME), HubSocial.Origin::root,
            ByteBufCodecs.optional(NAME), HubSocial.Origin::freedFrom,
            FREED_KIND, HubSocial.Origin::freedKind,
            ByteBufCodecs.BOOL.apply(ByteBufCodecs.list(HubSocial.MAX_ANCESTORS)), HubSocial.Origin::ancestorHidden,
            HubSocial.Origin::new);

    private static final StreamCodec<ByteBuf, Family> FAMILY = StreamCodec.composite(
            ByteBufCodecs.optional(NAME), Family::sire,
            ByteBufCodecs.BOOL, Family::severed,
            NAME.apply(ByteBufCodecs.list(MAX_NAMES)), Family::sired,
            NAME.apply(ByteBufCodecs.list(MAX_NAMES)), Family::ancestors,
            KIN.apply(ByteBufCodecs.list(MAX_NAMES)), Family::line,
            ORIGIN, Family::origin,
            Family::new);

    private static final StreamCodec<ByteBuf, Genes.Knowledge> KNOWLEDGE = ByteBufCodecs.stringUtf8(16).map(
            s -> Genes.Knowledge.valueOf(s.toUpperCase(Locale.ROOT)), k -> k.name().toLowerCase(Locale.ROOT));

    public static final StreamCodec<ByteBuf, HubSnapshotPayload> STREAM_CODEC = StreamCodec.composite(
            FAMILY, HubSnapshotPayload::family,
            MEMBER.apply(ByteBufCodecs.list(MAX_NAMES)), HubSnapshotPayload::pack,
            ByteBufCodecs.VAR_INT, HubSnapshotPayload::fiveCount,
            ByteBufCodecs.map(LinkedHashMap::new, ByteBufCodecs.stringUtf8(32), KNOWLEDGE, 8), HubSnapshotPayload::genes,
            RITE.apply(ByteBufCodecs.list(MAX_NAMES)), HubSnapshotPayload::rites,
            PERSON.apply(ByteBufCodecs.list(MAX_NAMES)), HubSnapshotPayload::people,
            HubSnapshotPayload::new);

    public HubSnapshotPayload {
        // the codec caps a list: a sire of hundreds is cut, not a crash on the client
        pack = cap(pack);
        genes = Map.copyOf(genes);
        rites = cap(rites);
        people = cap(people);
    }

    /** The id a click on this name opens a Profile with, if the server named one (the sire, someone sired, a pack member). */
    public Optional<java.util.UUID> idOf(String name) {
        return java.util.stream.Stream.concat(people.stream().map(p -> java.util.Map.entry(p.name(), p.id())),
                pack.stream().filter(m -> m.id().isPresent()).map(m -> java.util.Map.entry(m.name(), m.id().get())))
                .filter(e -> e.getKey().equals(name)).map(java.util.Map.Entry::getValue).findFirst();
    }

    private static <T> List<T> cap(List<T> l) {
        return List.copyOf(l.subList(0, Math.min(l.size(), MAX_NAMES)));
    }

    public Optional<String> sire() {
        return family.sire();
    }

    public boolean severed() {
        return family.severed();
    }

    public List<String> sired() {
        return family.sired();
    }

    /** The snapshot for {@code p}, built only from {@code p}'s own state and the indexes' names. */
    public static HubSnapshotPayload of(ServerPlayer p) {
        BloodlineState own = BloodlineAccess.get(p);
        BloodlineState view = BloodlineAccess.clientView(own, true);
        Map<String, Genes.Knowledge> genes = new LinkedHashMap<>();
        Genes.ALL.forEach(g -> genes.put(g, Genes.ofClient(view, g)));
        HubSocial.Social social = HubSocial.of(p);
        return new HubSnapshotPayload(new Family(social.sire(), own.bondBroken(), social.sired(), social.ancestors(), social.line(), social.origin()),
                social.pack(), social.fiveCount(), genes, social.rites(), social.people());
    }

    @Override
    public Type<HubSnapshotPayload> type() {
        return TYPE;
    }
}
