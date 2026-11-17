package net.ofatech.supernaturalbloodlines.parts.supernatural.net;

import io.netty.buffer.ByteBuf;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.shared.supernatural.profile.Fact;
import net.ofatech.supernaturalbloodlines.shared.supernatural.profile.ProfileService;

/**
 * Server to client: the facts of one person that this player may know, and no others. A fact that is not in the map is unknown and the client
 * draws "?"; an empty value list is an explicit "none". The map never says what was left out, and never carries the relation.
 * Kept apart from the hub snapshot, which stays names, ranks and online flags.
 */
public record ProfilePayload(UUID subject, boolean opView, Map<Fact, List<String>> facts) implements CustomPacketPayload {
    public static final Type<ProfilePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "profile"));

    private static final StreamCodec<ByteBuf, Fact> FACT = ByteBufCodecs.stringUtf8(16).map(Fact::valueOf, Fact::name);

    public static final StreamCodec<ByteBuf, ProfilePayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, ProfilePayload::subject,
            ByteBufCodecs.BOOL, ProfilePayload::opView,
            ByteBufCodecs.map(c -> new EnumMap<Fact, List<String>>(Fact.class), FACT, ByteBufCodecs.stringUtf8(128).apply(ByteBufCodecs.list(8)), Fact.values().length),
            ProfilePayload::facts,
            ProfilePayload::new);

    public ProfilePayload {
        facts = Map.copyOf(facts);
    }

    public static ProfilePayload of(ProfileService.Profile profile) {
        return new ProfilePayload(profile.subject(), profile.opView(), profile.facts());
    }

    @Override
    public Type<ProfilePayload> type() {
        return TYPE;
    }
}

