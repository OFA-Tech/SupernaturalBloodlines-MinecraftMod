package net.ofatech.supernaturalbloodlines.parts.supernatural.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/**
 * Client to server: unlock a skill of the player's Vampirism or Werewolves faction tree with that mod's own points. The server asks the
 * mod's own skill handler whether it may be enabled; the id is only a name.
 */
public record UnlockModSkillPayload(String skillId) implements CustomPacketPayload {
    public static final Type<UnlockModSkillPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "unlock_mod_skill"));

    public static final StreamCodec<ByteBuf, UnlockModSkillPayload> STREAM_CODEC =
            ByteBufCodecs.stringUtf8(96).map(UnlockModSkillPayload::new, UnlockModSkillPayload::skillId);

    @Override
    public Type<UnlockModSkillPayload> type() {
        return TYPE;
    }
}