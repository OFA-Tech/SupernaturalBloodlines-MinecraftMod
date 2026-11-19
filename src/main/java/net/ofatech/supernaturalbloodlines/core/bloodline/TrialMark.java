package net.ofatech.supernaturalbloodlines.core.bloodline;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * What marks the boss of an Alpha Trial (R147): who is being tried, the pack id its beasts carry, the night it began, the phase of the fight so far
 * and how long the challenger has been away. Saved with the entity, every field optional.
 */
public record TrialMark(@Nullable UUID challenger, long packId, long night, int phase, int away) {
    public static final Codec<TrialMark> CODEC = RecordCodecBuilder.create(i -> i.group(
            BloodlineCodecs.UUID_CODEC.lenientOptionalFieldOf("challenger").forGetter(m -> Optional.ofNullable(m.challenger())),
            Codec.LONG.lenientOptionalFieldOf("pack", 0L).forGetter(TrialMark::packId),
            Codec.LONG.lenientOptionalFieldOf("night", 0L).forGetter(TrialMark::night),
            Codec.INT.lenientOptionalFieldOf("phase", 0).forGetter(TrialMark::phase),
            Codec.INT.lenientOptionalFieldOf("away", 0).forGetter(TrialMark::away)
    ).apply(i, (c, pack, night, phase, away) -> new TrialMark(c.orElse(null), pack, night, phase, away)));

    public TrialMark withPhase(int p) {
        return new TrialMark(challenger, packId, night, p, away);
    }

    public TrialMark withAway(int a) {
        return new TrialMark(challenger, packId, night, phase, a);
    }
}
