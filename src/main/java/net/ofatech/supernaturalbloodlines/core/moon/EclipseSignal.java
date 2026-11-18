package net.ofatech.supernaturalbloodlines.core.moon;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The lunar eclipse starting or ending, for a part that mirrors it elsewhere (the celestials part) without the species part importing it. */
public final class EclipseSignal {
    private static final Logger LOGGER = LoggerFactory.getLogger(EclipseSignal.class);

    public interface Listener {
        /** @return true when this listener announced the change itself, so the eclipse part skips its own chat line */
        boolean changed(MinecraftServer server, boolean on);
    }

    private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<>();

    private EclipseSignal() {
    }

    public static void add(Listener listener) {
        LISTENERS.add(listener);
    }

    /** Tells every listener; true when one of them announced it. */
    public static boolean fire(MinecraftServer server, boolean on) {
        boolean announced = false;
        for (Listener l : LISTENERS) {
            try {
                announced |= l.changed(server, on);
            } catch (RuntimeException | LinkageError e) {
                LOGGER.warn("An eclipse listener failed: {}", e.toString());
            }
        }
        return announced;
    }
}
