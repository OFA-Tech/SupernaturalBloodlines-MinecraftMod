package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import java.util.Locale;
import org.jetbrains.annotations.Nullable;

/** The SP and EP a theme shows as a margin note, a top-right pair or a stats line. EP is Tensura's and absent without it. */
public record Counters(int sp, @Nullable Long ep) {
    /** "6 SP". */
    public String spText() {
        return sp + " SP";
    }

    /** "1,038 EP", or null without Tensura. */
    @Nullable
    public String epText() {
        return ep == null ? null : String.format(Locale.ROOT, "%,d EP", ep);
    }
}
