package net.ofatech.supernaturalbloodlines.shared.moon;

import java.util.function.Predicate;
import net.minecraft.server.MinecraftServer;

/** Whether a lunar eclipse is running: the species part installs it; with that part off there is never one. */
public final class EclipseFeed {
    /** What answers before the species part installs the real one, and when it is off: no eclipse. */
    static final Predicate<MinecraftServer> NONE = server -> false;
    private static Predicate<MinecraftServer> source = NONE;

    private EclipseFeed() {
    }

    public static void install(Predicate<MinecraftServer> eclipse) {
        source = eclipse;
    }

    public static boolean active(MinecraftServer server) {
        return server != null && source.test(server);
    }
}