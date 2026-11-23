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
import net.ofatech.supernaturalbloodlines.shared.supernatural.WorldNowHook;

/**
 * Server to client, answering {@link HubSnapshotRequestPayload}: what the hub shows about other players and the world. Only names,
 * ranks and online flags of others; the genes are the subject's own, as their client may know them (unknown until discovered).
 */
public record HubSnapshotPayload(Family family, List<HubSocial.Member> pack, int fiveCount, Map<String, Genes.Knowledge> genes,
                                 List<HubSocial.Person> people, WorldNowHook.Flags world, List<HubSocial.Npc> npcs,
                                 boolean moonGift) implements CustomPacketPayload {
    public HubSnapshotPayload(Family family, List<HubSocial.Member> pack, int fiveCount, Map<String, Genes.Knowledge> genes,
                              List<HubSocial.Person> people, WorldNowHook.Flags world, List<HubSocial.Npc> npcs) {
        this(family, pack, fiveCount, genes, people, world, npcs, false);
    }

    public HubSnapshotPayload(Family family, List<HubSocial.Member> pack, int fiveCount, Map<String, Genes.Knowledge> genes,
                              List<HubSocial.Person> people, WorldNowHook.Flags world) {
        this(family, pack, fiveCount, genes, people, world, List.of());
    }

    public HubSnapshotPayload(Family family, List<HubSocial.Member> pack, int fiveCount, Map<String, Genes.Knowledge> genes) {
        this(family, pack, fiveCount, genes, List.of());
    }

    public HubSnapshotPayload(Family family, List<HubSocial.Member> pack, int fiveCount, Map<String, Genes.Knowledge> genes,
                              List<HubSocial.Person> people) {
        this(family, pack, fiveCount, genes, people, WorldNowHook.Flags.NONE);
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

    public static final HubSnapshotPayload EMPTY = new HubSnapshotPayload(Family.NONE, List.of(), 0, Map.of());

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

    private static final StreamCodec<ByteBuf, WorldNowHook.Flags> WORLD = StreamCodec.composite(
            ByteBufCodecs.BOOL, WorldNowHook.Flags::sleepRule,
            ByteBufCodecs.BOOL, WorldNowHook.Flags::awakeningPending,
            ByteBufCodecs.VAR_INT, WorldNowHook.Flags::trial,
            ByteBufCodecs.VAR_INT, WorldNowHook.Flags::bossNear,
            ByteBufCodecs.STRING_UTF8, WorldNowHook.Flags::bossName,
            WorldNowHook.Flags::new);

    private static final StreamCodec<ByteBuf, HubSocial.Npc> NPC = StreamCodec.of(
            (buf, n) -> {
                ByteBufCodecs.VAR_INT.encode(buf, n.token());
                NAME.encode(buf, n.name());
                ID.encode(buf, n.type());
                ByteBufCodecs.stringUtf8(16).encode(buf, n.source());
                ByteBufCodecs.stringUtf8(16).encode(buf, n.order());
                ByteBufCodecs.BOOL.encode(buf, n.hunt());
                ByteBufCodecs.BOOL.encode(buf, n.sort());
                ByteBufCodecs.BOOL.encode(buf, n.loaded());
                ByteBufCodecs.VAR_INT.encode(buf, n.distance());
                ByteBufCodecs.VAR_INT.encode(buf, n.health());
                ByteBufCodecs.VAR_INT.encode(buf, n.days());
                NAME.encode(buf, n.owner());
                ByteBufCodecs.BOOL.encode(buf, n.canOrder());
                ByteBufCodecs.BOOL.encode(buf, n.mine());
                ByteBufCodecs.VAR_INT.encode(buf, n.weightPercent());
            },
            buf -> new HubSocial.Npc(ByteBufCodecs.VAR_INT.decode(buf), NAME.decode(buf), ID.decode(buf), ByteBufCodecs.stringUtf8(16).decode(buf),
                    ByteBufCodecs.stringUtf8(16).decode(buf), ByteBufCodecs.BOOL.decode(buf), ByteBufCodecs.BOOL.decode(buf), ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), NAME.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf), ByteBufCodecs.BOOL.decode(buf), ByteBufCodecs.VAR_INT.decode(buf)));

    private static final StreamCodec<ByteBuf, List<HubSocial.Npc>> NPCS = NPC.apply(ByteBufCodecs.list(HubSocial.MAX_NPCS));

    private static final StreamCodec<ByteBuf, List<HubSocial.Member>> MEMBERS = MEMBER.apply(ByteBufCodecs.list(MAX_NAMES));
    private static final StreamCodec<ByteBuf, Map<String, Genes.Knowledge>> GENES = ByteBufCodecs.map(LinkedHashMap::new, ByteBufCodecs.stringUtf8(32), KNOWLEDGE, 8);
    private static final StreamCodec<ByteBuf, List<HubSocial.Person>> PEOPLE = PERSON.apply(ByteBufCodecs.list(MAX_NAMES));

    /** Eight fields: more than {@code StreamCodec.composite} takes, so written out, in this order. */
    public static final StreamCodec<ByteBuf, HubSnapshotPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                FAMILY.encode(buf, p.family());
                MEMBERS.encode(buf, p.pack());
                ByteBufCodecs.VAR_INT.encode(buf, p.fiveCount());
                GENES.encode(buf, p.genes());
                PEOPLE.encode(buf, p.people());
                WORLD.encode(buf, p.world());
                NPCS.encode(buf, p.npcs());
                ByteBufCodecs.BOOL.encode(buf, p.moonGift());
            },
            buf -> new HubSnapshotPayload(FAMILY.decode(buf), MEMBERS.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), GENES.decode(buf),
                    PEOPLE.decode(buf), WORLD.decode(buf), NPCS.decode(buf), ByteBufCodecs.BOOL.decode(buf)));

    public HubSnapshotPayload {
        // the codec caps a list: a sire of hundreds is cut, not a crash on the client
        pack = cap(pack);
        genes = Map.copyOf(genes);
        people = cap(people);
        npcs = List.copyOf(npcs.subList(0, Math.min(npcs.size(), HubSocial.MAX_NPCS)));
    }

    /** The name index of the snapshot asked last: a client holds one snapshot at a time and the pages ask per frame, so it is built once per snapshot and is no part of the record (nor of its equality or its wire format). */
    private record Indexed(HubSnapshotPayload of, Map<String, java.util.UUID> ids) {
    }

    private static volatile Indexed last;

    /** Name to id for everyone the server named. */
    public Map<String, java.util.UUID> ids() {
        Indexed i = last;
        if (i == null || i.of != this) {
            i = new Indexed(this, index(pack, people));
            last = i;
        }
        return i.ids;
    }

    /** Name to id for everyone the server named, built once (the pages ask per frame); the first of a name wins: the sire and the sired before the pack. */
    private static Map<String, java.util.UUID> index(List<HubSocial.Member> pack, List<HubSocial.Person> people) {
        Map<String, java.util.UUID> out = new java.util.HashMap<>();
        people.forEach(p -> out.putIfAbsent(p.name(), p.id()));
        pack.stream().filter(m -> m.id().isPresent()).forEach(m -> out.putIfAbsent(m.name(), m.id().get()));
        return Map.copyOf(out);
    }

    /** The id a click on this name opens a Profile with, if the server named one (the sire, someone sired, a pack member). */
    public Optional<java.util.UUID> idOf(String name) {
        return Optional.ofNullable(ids().get(name));
    }

    private static <T> List<T> cap(List<T> l) {
        return List.copyOf(l.subList(0, Math.min(l.size(), MAX_NAMES)));
    }

    public Optional<String> sire() {
        return family.sire();
    }

    /** Whether the player has a sire at all: a sire the client may not be told (a creature, an undiscovered NPC Original) is an empty name, not an absent one. */
    public boolean hasSire() {
        return family.sire().isPresent();
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
                social.pack(), social.fiveCount(), genes, social.people(), WorldNowHook.of(p), HubSocial.npcsOf(p), HubSocial.moonGiftOf(p));
    }

    @Override
    public Type<HubSnapshotPayload> type() {
        return TYPE;
    }
}
