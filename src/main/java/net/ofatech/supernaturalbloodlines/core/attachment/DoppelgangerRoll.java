package net.ofatech.supernaturalbloodlines.core.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A villager's doppelgänger roll. {@code rolled} is what keeps a reloaded villager from being rolled again; it is never false
 * once saved, so the saved value can never equal the default.
 */
public record DoppelgangerRoll(boolean doppelganger, boolean rolled) {
    public static final DoppelgangerRoll NONE = new DoppelgangerRoll(false, false);

    public static final Codec<DoppelgangerRoll> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.lenientOptionalFieldOf("doppelganger", false).forGetter(DoppelgangerRoll::doppelganger),
            Codec.BOOL.lenientOptionalFieldOf("rolled", false).forGetter(DoppelgangerRoll::rolled)
    ).apply(i, DoppelgangerRoll::new));

    /** The result of rolling {@code roll} (a value in [0, 1)) against {@code chance}. */
    public static DoppelgangerRoll of(double roll, double chance) {
        return new DoppelgangerRoll(roll < chance, true);
    }
}
