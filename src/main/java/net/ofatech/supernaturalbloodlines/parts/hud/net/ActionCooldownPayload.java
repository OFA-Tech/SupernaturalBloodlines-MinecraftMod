package net.ofatech.supernaturalbloodlines.parts.hud.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.hud.action.LoadoutService;

/** So the slot can sweep and count down (the server keeps the real cooldown; the client only draws). */
/**
 * The cooldown an action of another part started: its {@code total} length (for the sweep's fraction) and what is {@code left} of it (equal to the total for a
 * cooldown that just started, less for one a player who logs in is still in).
 */
public record ActionCooldownPayload(ResourceLocation action, int total, int left) implements CustomPacketPayload {
    public static final Type<ActionCooldownPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "hud_action_cooldown"));
    public static final StreamCodec<ByteBuf, ActionCooldownPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, ActionCooldownPayload::action,
            ByteBufCodecs.VAR_INT, ActionCooldownPayload::total,
            ByteBufCodecs.VAR_INT, ActionCooldownPayload::left,
            ActionCooldownPayload::new);

    @Override
    public Type<ActionCooldownPayload> type() {
        return TYPE;
    }

    /** A cooldown that just started. */
    public static void send(ServerPlayer p, ResourceLocation action, int total) {
        sendRemaining(p, action, total, total);
    }

    /** A cooldown of {@code total} with {@code left} to run (a login in the middle of one). */
    public static void sendRemaining(ServerPlayer p, ResourceLocation action, int total, int left) {
        if (LoadoutService.canReceive(p) && p.connection.hasChannel(TYPE)) {
            PacketDistributor.sendToPlayer(p, new ActionCooldownPayload(action, total, left));
        }
    }
}
