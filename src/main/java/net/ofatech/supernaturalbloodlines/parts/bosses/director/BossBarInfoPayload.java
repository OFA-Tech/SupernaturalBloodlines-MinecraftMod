package net.ofatech.supernaturalbloodlines.parts.bosses.director;

import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.boss.BossBarForms;
import net.ofatech.supernaturalbloodlines.core.boss.BossId;

/**
 * Server to client: what vanilla's boss bar packet cannot say about one of our bars. The bar has a random UUID and carries only a name and a fraction, so this
 * names the boss, its form chips and the health in numbers, for the bar with the UUID {@code bar}. Sent before the bar is added, and again whenever any of it changes.
 * {@code packed}: the boss (its ordinal plus one, 0 for none) in bits 0 to 7, the row of chips in 8 to 11, the lit chip in 12 to 15 and the fallen flag in bit 16.
 */
public record BossBarInfoPayload(UUID bar, int packed, int hp, int max) implements CustomPacketPayload {
    public static final Type<BossBarInfoPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "boss_bar_info"));
    public static final StreamCodec<ByteBuf, BossBarInfoPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, BossBarInfoPayload::bar,
            ByteBufCodecs.VAR_INT, BossBarInfoPayload::packed,
            ByteBufCodecs.VAR_INT, BossBarInfoPayload::hp,
            ByteBufCodecs.VAR_INT, BossBarInfoPayload::max,
            BossBarInfoPayload::new);

    /** The most health a bar shows: a number out of range is held here, so a forged payload cannot make the client format a huge string. */
    public static final int LIMIT = 1_000_000;

    public BossBarInfoPayload {
        hp = Math.max(0, Math.min(LIMIT, hp));
        max = Math.max(1, Math.min(LIMIT, max));
    }

    public static BossBarInfoPayload of(UUID bar, BossId boss, BossBarForms.Look look, boolean fallen, int hp, int max) {
        int packed = (boss.ordinal() + 1) & 0xFF | (look.set().ordinal() & 0xF) << 8 | (look.active() & 0xF) << 12 | (fallen ? 1 << 16 : 0);
        return new BossBarInfoPayload(bar, packed, hp, max);
    }

    public Optional<BossId> boss() {
        int n = (packed & 0xFF) - 1;
        BossId[] all = BossId.values();
        return n >= 0 && n < all.length ? Optional.of(all[n]) : Optional.empty();
    }

    /** The chips, checked against the boss (none for an unknown boss or a row that is not its own). */
    public BossBarForms.Look look() {
        return boss().map(b -> BossBarForms.checked(b, packed >> 8 & 0xF, packed >> 12 & 0xF)).orElse(BossBarForms.Look.NONE);
    }

    public boolean fallen() {
        return (packed >> 16 & 1) != 0;
    }

    @Override
    public Type<BossBarInfoPayload> type() {
        return TYPE;
    }
}
