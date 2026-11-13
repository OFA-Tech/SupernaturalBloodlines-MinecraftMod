package net.ofatech.supernaturalbloodlines.parts.species.sireline;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

/**
 * The discount a compelled villager gives {@code player}: {@code amount} of the villager's {@code MAJOR_POSITIVE} gossip about
 * them, which is exactly what the compulsion added (the villager may have had some already) and what is taken back at {@code expires}.
 */
public record VillagerDiscount(UUID player, int amount, long expires) {
    public static final Codec<VillagerDiscount> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("player").forGetter(VillagerDiscount::player),
            Codec.INT.fieldOf("amount").forGetter(VillagerDiscount::amount),
            Codec.LONG.fieldOf("expires").forGetter(VillagerDiscount::expires)
    ).apply(i, VillagerDiscount::new));

    public boolean expired(long gameTime) {
        return gameTime >= expires;
    }
}
