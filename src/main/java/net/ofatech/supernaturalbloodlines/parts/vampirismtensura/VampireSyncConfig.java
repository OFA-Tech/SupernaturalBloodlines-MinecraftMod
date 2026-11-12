package net.ofatech.supernaturalbloodlines.parts.vampirismtensura;

import java.util.List;
import java.util.function.Supplier;
import net.neoforged.neoforge.common.ModConfigSpec;

/** The {@code vampirism_tensura} section of the mod's COMMON spec. Values are readable once configs have loaded. */
public final class VampireSyncConfig {
    private static ModConfigSpec.ConfigValue<String> cureRace;
    private static ModConfigSpec.ConfigValue<List<? extends String>> raceLevelFloor;

    private VampireSyncConfig() {}

    /** Called by {@code Config} while it builds COMMON. */
    public static void define(ModConfigSpec.Builder b) {
        b.comment("Tensura <-> Vampirism sync. Joining Vampirism never changes the Tensura race.")
                .push("vampirism_tensura");
        cureRace = b.comment("Race set when a cured player's current race is in the Tensura vampire line.")
                .define("cure_race", VampireMapping.DEFAULT_CURE_RACE);
        raceLevelFloor = b.comment("\"race=level\": a Tensura vampire-line race raises the Vampirism level to at least",
                        "level; -1 means the maximum vampire level. A race not listed does not join the faction.")
                .defineListAllowEmpty("race_level_floor", VampireMapping.DEFAULT_FLOORS, () -> "", o -> o instanceof String);
        b.pop();
    }

    /** Needs a loaded config; falls back to defaults before that. */
    public static VampireMapping mapping(int maxLevel) {
        try {
            return VampireMapping.parse(cureRace.get(), raceLevelFloor.get().stream().map(String.class::cast).toList(), maxLevel);
        } catch (IllegalStateException notLoaded) {
            return VampireMapping.defaults(maxLevel);
        }
    }
}
