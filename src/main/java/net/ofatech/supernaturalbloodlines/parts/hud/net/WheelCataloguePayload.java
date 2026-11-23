package net.ofatech.supernaturalbloodlines.parts.hud.net;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/** Server to client: every power the viewer has, with a revision that grows each time the list changes. At most {@link #MAX_ENTRIES} entries. */
public record WheelCataloguePayload(int revision, List<WheelEntry> entries) implements CustomPacketPayload {
    public static final int MAX_ENTRIES = 512;
    public static final Type<WheelCataloguePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "wheel_catalogue"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WheelCataloguePayload> STREAM_CODEC = StreamCodec.of((buf, p) -> {
        ByteBufCodecs.VAR_INT.encode(buf, p.revision);
        ByteBufCodecs.VAR_INT.encode(buf, p.entries.size());
        for (WheelEntry e : p.entries) {
            WheelEntry.STREAM_CODEC.encode(buf, e);
        }
    }, buf -> {
        int revision = ByteBufCodecs.VAR_INT.decode(buf);
        int n = ByteBufCodecs.VAR_INT.decode(buf);
        if (n < 0 || n > MAX_ENTRIES) {
            throw new io.netty.handler.codec.DecoderException("too many wheel entries " + n);
        }
        List<WheelEntry> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            list.add(WheelEntry.STREAM_CODEC.decode(buf));
        }
        return new WheelCataloguePayload(revision, list);
    });

    public WheelCataloguePayload {
        entries = List.copyOf(entries);
    }

    @Override
    public Type<WheelCataloguePayload> type() {
        return TYPE;
    }
}
