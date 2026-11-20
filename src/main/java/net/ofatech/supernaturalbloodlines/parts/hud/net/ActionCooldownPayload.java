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

/** The cooldown an action of another part just started, so the slot can sweep and count down (the server keeps the real one; the client only draws). */
public record ActionCooldownPayload(ResourceLocation action, int ticks) implements CustomPacketPayload {
    public static final Type<ActionCooldownPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "hud_action_cooldown"));
    public static final StreamCodec<ByteBuf, ActionCooldownPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, ActionCooldownPayload::action,
            ByteBufCodecs.VAR_INT, ActionCooldownPayload::ticks,
            ActionCooldownPayload::new);

    @Override
    public Type<ActionCooldownPayload> type() {
        return TYPE;
    }

    public static void send(ServerPlayer p, ResourceLocation action, int ticks) {
        if (LoadoutService.canReceive(p) && p.connection.hasChannel(TYPE)) {
            PacketDistributor.sendToPlayer(p, new ActionCooldownPayload(action, ticks));
        }
    }
}
