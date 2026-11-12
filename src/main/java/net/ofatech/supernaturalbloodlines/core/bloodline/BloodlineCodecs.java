package net.ofatech.supernaturalbloodlines.core.bloodline;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.UUID;

/** Small codec helpers that use only DFU types, so the core stays free of Minecraft classes. */
final class BloodlineCodecs {
    static final Codec<UUID> UUID_CODEC = Codec.STRING.comapFlatMap(s -> {
        try {
            return DataResult.success(UUID.fromString(s));
        } catch (IllegalArgumentException ex) {
            return DataResult.error(() -> "Not a UUID: " + s);
        }
    }, UUID::toString);

    private BloodlineCodecs() {
    }

    /** Encodes by name; an unknown name decodes to {@code fallback} so old or modded saves still load. */
    static <E extends Enum<E>> Codec<E> enumByName(Class<E> type, E fallback) {
        return Codec.STRING.xmap(name -> {
            for (E value : type.getEnumConstants()) {
                if (value.name().equals(name)) {
                    return value;
                }
            }
            return fallback;
        }, Enum::name);
    }
}
