package net.ofatech.supernaturalbloodlines.shared.species;

import java.util.Collection;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * The doppelgänger villagers loaded in a level. The species part (which rolls villagers and keeps the index) installs the source;
 * without it nothing is found. The Vampire Compass reads this instead of importing the species part.
 */
public final class DoppelgangerIndex {
    /** The villagers of {@code level} that are doppelgängers and loaded right now. Never a world scan. */
    public interface Source {
        Collection<? extends Entity> loaded(ServerLevel level);
    }

    private static final Source NOBODY = level -> List.of();
    private static volatile Source source = NOBODY;

    private DoppelgangerIndex() {
    }

    public static void set(Source s) {
        source = s == null ? NOBODY : s;
    }

    public static Source current() {
        return source;
    }

    public static Collection<? extends Entity> villagers(ServerLevel level) {
        return source.loaded(level);
    }
}
