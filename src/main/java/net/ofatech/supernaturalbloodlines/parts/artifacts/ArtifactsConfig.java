package net.ofatech.supernaturalbloodlines.parts.artifacts;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import org.jetbrains.annotations.VisibleForTesting;

/** The artifacts part's own config file. */
public final class ArtifactsConfig {
    public static final String FILE = SupernaturalBloodlines.MODID + "-artifacts.toml";
    public static final ModConfigSpec SPEC;
    public static final int DEFAULT_COMPASS_DOPPELGANGER_RANGE = 512;

    private static final ModConfigSpec.IntValue COMPASS_DOPPELGANGER_RANGE;
    private static volatile Integer compassRangeOverride;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("artifacts");
        COMPASS_DOPPELGANGER_RANGE = b.comment("How far, in blocks, the Vampire Compass in Doppelgänger mode finds a doppelgänger villager (only villagers in loaded chunks are found). Players with the gene are found anywhere in the dimension.")
                .defineInRange("compass_doppelganger_range", DEFAULT_COMPASS_DOPPELGANGER_RANGE, 1, 30_000_000);
        b.pop();
        SPEC = b.build();
    }

    private ArtifactsConfig() {}

    /** For tests: pins {@link #compassDoppelgangerRange()}; null clears it. */
    @VisibleForTesting
    public static void overrideCompassDoppelgangerRange(Integer value) {
        compassRangeOverride = value;
    }

    public static int compassDoppelgangerRange() {
        Integer pinned = compassRangeOverride;
        if (pinned != null) {
            return pinned;
        }
        try {
            return COMPASS_DOPPELGANGER_RANGE.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_COMPASS_DOPPELGANGER_RANGE;
        }
    }
}
