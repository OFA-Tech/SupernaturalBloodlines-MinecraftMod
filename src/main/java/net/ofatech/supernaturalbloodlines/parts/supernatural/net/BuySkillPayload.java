package net.ofatech.supernaturalbloodlines.parts.supernatural.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/** Client to server: buy a skill. The server decides everything (the tree, the rung, the quest, the SP); the id is only a name. */
public record BuySkillPayload(String skillId) implements CustomPacketPayload {
    public static final Type<BuySkillPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "buy_skill"));

    public static final StreamCodec<ByteBuf, BuySkillPayload> STREAM_CODEC =
            ByteBufCodecs.stringUtf8(64).map(BuySkillPayload::new, BuySkillPayload::skillId);

    @Override
    public Type<BuySkillPayload> type() {
        return TYPE;
    }
}
