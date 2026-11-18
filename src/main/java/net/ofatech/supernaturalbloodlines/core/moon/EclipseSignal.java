package net.ofatech.supernaturalbloodlines.core.moon;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The lunar eclipse starting or ending, for a part that mirrors it elsewhere (the celestials part) without the species part importing it.
 * The eclipse part always sends its own chat line: whether Enhanced Celestials announces a mirrored event is not verified in a real game.
 */
public final class EclipseSignal {
    private static final Logger LOGGER = LoggerFactory.getLogger(EclipseSignal.class);

    public interface Listener {
        void changed(MinecraftServer server, boolean on);
    }

    private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<>();

    private EclipseSignal() {
    }

    public static void add(Listener listener) {
        LISTENERS.add(listener);
    }

    /** For tests in this package: forgets every listener. */
    static void clear() {
        LISTENERS.clear();
    }

    /** Tells every listener. One that fails is logged and does not stop the others or the eclipse. */
    public static void fire(MinecraftServer server, boolean on) {
        for (Listener l : LISTENERS) {
            try {
                l.changed(server, on);
            } catch (RuntimeException | LinkageError e) {
                LOGGER.warn("An eclipse listener failed: {}", e.toString());
            }
        }
    }
}
