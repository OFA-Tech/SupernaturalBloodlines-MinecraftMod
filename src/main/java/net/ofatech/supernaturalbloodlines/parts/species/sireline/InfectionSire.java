package net.ofatech.supernaturalbloodlines.parts.species.sireline;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

/**
 * Who infected a creature with sanguinare (by id and last known name), until {@code expires} (game time). Vampirism keeps no record of the infector,
 * so this is the only trace; it is the player's sire when they join the vampires, the mob's when it is converted.
 */
public record InfectionSire(UUID sire, String name, long expires) {
    /** Ticks a pending infection counts for (one day). */
    public static final long WINDOW = 24000;

    public static final Codec<InfectionSire> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("sire").forGetter(InfectionSire::sire),
            Codec.STRING.optionalFieldOf("name", "").forGetter(InfectionSire::name),
            Codec.LONG.fieldOf("expires").forGetter(InfectionSire::expires)
    ).apply(i, InfectionSire::new));

    public boolean expired(long gameTime) {
        return gameTime >= expires;
    }
}
