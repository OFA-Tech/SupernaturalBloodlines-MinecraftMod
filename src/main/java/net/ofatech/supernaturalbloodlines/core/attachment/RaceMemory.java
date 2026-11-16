package net.ofatech.supernaturalbloodlines.core.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/**
 * The Tensura race a player had before this mod moved them (a werewolf's Beastfolk line), and the race this mod moved them to. On a cure the race goes
 * back only if it is still the one we set; a race the player evolved to themselves is left alone. Pure: no game classes except the codec.
 *
 * @param prior the race before our first change ("" for nothing recorded)
 * @param set   the race our last change set
 */
public record RaceMemory(String prior, String set) {
    public static final RaceMemory NONE = new RaceMemory("", "");

    public static final Codec<RaceMemory> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("prior", "").forGetter(RaceMemory::prior),
            Codec.STRING.optionalFieldOf("set", "").forGetter(RaceMemory::set)).apply(i, RaceMemory::new));

    /** What to keep after we change {@code current} to {@code target}: a chain of our own changes keeps the race from before the first one. */
    public RaceMemory afterChange(@Nullable String current, String target) {
        boolean chain = !set.isEmpty() && set.equals(current);
        return new RaceMemory(chain ? prior : current == null ? "" : current, target);
    }

    /** The race to put back when the player leaves, or empty to leave the race alone. */
    public Optional<String> restore(@Nullable String current) {
        return !set.isEmpty() && !prior.isEmpty() && set.equals(current) ? Optional.of(prior) : Optional.empty();
    }
}
