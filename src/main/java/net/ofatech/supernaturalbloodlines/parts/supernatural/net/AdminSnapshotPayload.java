package net.ofatech.supernaturalbloodlines.parts.supernatural.net;

import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.shared.admin.AdminRows;
import net.ofatech.supernaturalbloodlines.shared.admin.AdminSettings;
import net.ofatech.supernaturalbloodlines.shared.admin.SettingKind;
import net.ofatech.supernaturalbloodlines.shared.admin.SettingValue;

/**
 * Server to client, to a permission level 2 player only: every setting that is on with its live value, range and flags, and (after an edit)
 * how the edit went. The page words the outcome from lang keys; nothing but ids and values crosses the wire.
 */
public record AdminSnapshotPayload(List<AdminRows.Row> rows, Optional<Outcome> outcome) implements CustomPacketPayload {
    /** @param status an {@link AdminSettings.Status} name; {@code key} and {@code detail} say why a value was refused */
    public record Outcome(String id, String status, String key, String detail, boolean restart) {
        public static Outcome of(String id, AdminSettings.Result r) {
            return new Outcome(id, r.status().name(), r.refusalKey(), r.detail(), r.restartRequired());
        }
    }

    public static final int MAX_ROWS = 320;
    public static final Type<AdminSnapshotPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "admin_snapshot"));

    private static final StreamCodec<ByteBuf, String> ID = ByteBufCodecs.stringUtf8(128);

    private static final StreamCodec<ByteBuf, SettingKind> KIND = ByteBufCodecs.stringUtf8(16).map(SettingKind::valueOf, SettingKind::name);

    private static final StreamCodec<ByteBuf, AdminRows.Row> ROW = new StreamCodec<>() {
        @Override
        public AdminRows.Row decode(ByteBuf b) {
            return new AdminRows.Row(ID.decode(b), KIND.decode(b), SettingValue.STREAM_CODEC.decode(b), SettingValue.STREAM_CODEC.decode(b),
                    ByteBufCodecs.BOOL.decode(b), ByteBufCodecs.DOUBLE.decode(b), ByteBufCodecs.DOUBLE.decode(b),
                    ByteBufCodecs.stringUtf8(64).apply(ByteBufCodecs.list(16)).decode(b), ByteBufCodecs.BOOL.decode(b), ByteBufCodecs.BOOL.decode(b));
        }

        @Override
        public void encode(ByteBuf b, AdminRows.Row r) {
            ID.encode(b, r.id());
            KIND.encode(b, r.kind());
            SettingValue.STREAM_CODEC.encode(b, r.current());
            SettingValue.STREAM_CODEC.encode(b, r.def());
            ByteBufCodecs.BOOL.encode(b, r.ranged());
            ByteBufCodecs.DOUBLE.encode(b, r.min());
            ByteBufCodecs.DOUBLE.encode(b, r.max());
            ByteBufCodecs.stringUtf8(64).apply(ByteBufCodecs.list(16)).encode(b, r.choices());
            ByteBufCodecs.BOOL.encode(b, r.restart());
            ByteBufCodecs.BOOL.encode(b, r.clientCopy());
        }
    };

    private static final StreamCodec<ByteBuf, Outcome> OUTCOME = StreamCodec.composite(
            ID, Outcome::id,
            ByteBufCodecs.stringUtf8(16), Outcome::status,
            ByteBufCodecs.stringUtf8(128), Outcome::key,
            ByteBufCodecs.stringUtf8(SettingValue.MAX_LINE), Outcome::detail,
            ByteBufCodecs.BOOL, Outcome::restart,
            Outcome::new);

    public static final StreamCodec<ByteBuf, AdminSnapshotPayload> STREAM_CODEC = StreamCodec.composite(
            ROW.apply(ByteBufCodecs.list(MAX_ROWS)), AdminSnapshotPayload::rows,
            ByteBufCodecs.optional(OUTCOME), AdminSnapshotPayload::outcome,
            AdminSnapshotPayload::new);

    public AdminSnapshotPayload {
        rows = List.copyOf(rows);
    }

    @Override
    public Type<AdminSnapshotPayload> type() {
        return TYPE;
    }
}
