package net.ofatech.supernaturalbloodlines.parts.species;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/**
 * Line death is on by default, so a server that had the old {@code sireline_death} key in its species config (off, or on) is told once at start
 * that the key is ignored and what replaced it. The file is read before NeoForge loads it: NeoForge drops keys the spec does not know when it
 * rewrites the file, so after loading the old key can no longer be seen.
 */
final class LegacyConfig {
    private static final Pattern OLD_KEY = Pattern.compile("^\\s*sireline_death\\s*=");
    private static volatile boolean found;

    private LegacyConfig() {
    }

    /** Whether the config file at {@code file} still has the old {@code sireline_death} key (a comment or another key does not count). */
    static boolean hasSirelineDeath(Path file) {
        try {
            return Files.isRegularFile(file) && Files.readAllLines(file).stream().anyMatch(l -> OLD_KEY.matcher(l).find());
        } catch (IOException | RuntimeException unreadable) {
            return false;
        }
    }

    /** Reads the file now (before it is loaded) and warns once when the server starts. */
    static void register() {
        found = hasSirelineDeath(FMLPaths.CONFIGDIR.get().resolve(SpeciesConfig.FILE));
        NeoForge.EVENT_BUS.addListener((ServerStartingEvent e) -> warnIfFound());
    }

    private static void warnIfFound() {
        if (found) {
            found = false;
            SupernaturalBloodlines.LOGGER.warn(WARNING);
        }
    }

    static final String WARNING = SpeciesConfig.FILE + " still has sireline_death, which is ignored now. Line death (original_line_death) is ON by default, offline players included: "
            + "set original_line_death = false to keep the old behaviour.";
}
