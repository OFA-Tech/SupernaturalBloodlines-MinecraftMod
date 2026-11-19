package net.ofatech.supernaturalbloodlines.core.bloodline;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * What marks a mob as one of ours (plan 15b): a wild NPC werewolf of a spawned pack ({@code packId} ties the wolves of one pack together, {@code night}
 * is the night it came), or a Wolf Spirit (its {@code owner} and the game time it {@code expires}). Saved with the entity, every field optional.
 */
public record NpcMark(Kind kind, long packId, long night, long expires, @Nullable UUID owner) {
    public enum Kind { WILD, SPIRIT }

    public static final Codec<NpcMark> CODEC = RecordCodecBuilder.create(i -> i.group(
            BloodlineCodecs.enumByName(Kind.class, Kind.WILD).lenientOptionalFieldOf("kind", Kind.WILD).forGetter(NpcMark::kind),
            Codec.LONG.lenientOptionalFieldOf("pack", 0L).forGetter(NpcMark::packId),
            Codec.LONG.lenientOptionalFieldOf("night", 0L).forGetter(NpcMark::night),
            Codec.LONG.lenientOptionalFieldOf("expires", 0L).forGetter(NpcMark::expires),
            BloodlineCodecs.UUID_CODEC.lenientOptionalFieldOf("owner").forGetter(m -> Optional.ofNullable(m.owner()))
    ).apply(i, (kind, pack, night, expires, owner) -> new NpcMark(kind, pack, night, expires, owner.orElse(null))));

    public static NpcMark wild(long packId, long night) {
        return new NpcMark(Kind.WILD, packId, night, 0, null);
    }

    public static NpcMark spirit(UUID owner, long expires) {
        return new NpcMark(Kind.SPIRIT, 0, 0, expires, owner);
    }
}
