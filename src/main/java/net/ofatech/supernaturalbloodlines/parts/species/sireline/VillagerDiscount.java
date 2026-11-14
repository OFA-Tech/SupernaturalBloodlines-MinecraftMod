package net.ofatech.supernaturalbloodlines.parts.species.sireline;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

/**
 * The discount a compelled villager gives {@code player}: {@code amount} of the villager's {@code MAJOR_POSITIVE} gossip about
 * them, which is exactly what the compulsion added (the villager may have had some already) and what is taken back at {@code expires}.
 * {@code minor} is the same for {@code MINOR_POSITIVE}, where Silver Tongue (S-VAM-3) puts the part of its bigger discount that does not
 * fit into the 20 a villager keeps of {@code MAJOR_POSITIVE}.
 */
public record VillagerDiscount(UUID player, int amount, int minor, long expires) {
    public static final Codec<VillagerDiscount> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("player").forGetter(VillagerDiscount::player),
            Codec.INT.fieldOf("amount").forGetter(VillagerDiscount::amount),
            Codec.INT.optionalFieldOf("minor", 0).forGetter(VillagerDiscount::minor),
            Codec.LONG.fieldOf("expires").forGetter(VillagerDiscount::expires)
    ).apply(i, VillagerDiscount::new));

    public VillagerDiscount(UUID player, int amount, long expires) {
        this(player, amount, 0, expires);
    }

    public boolean expired(long gameTime) {
        return gameTime >= expires;
    }
}
