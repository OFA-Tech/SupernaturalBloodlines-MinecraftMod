package net.ofatech.supernaturalbloodlines.parts.evolutionrites.probe;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.evolution.EvolutionRecipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The probe of a recipe by its id. An id nobody knows is no probe (and one warning), so the recipe is unavailable. */
public final class Probes {
    public static final String OWN_SAGE_GREAT_SAGE = "own.sage_great_sage";
    private static final Logger LOGGER = LoggerFactory.getLogger("supernaturalbloodlines");

    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();
    private static final Map<String, AddonProbe> FIXED = Map.of(OWN_SAGE_GREAT_SAGE, new OwnProbe());

    private Probes() {
    }

    public static Optional<AddonProbe> of(String id) {
        AddonProbe fixed = FIXED.get(id);
        if (fixed != null) {
            return Optional.of(fixed);
        }
        Optional<AddonProbe> unique = UniqueRoadProbe.parse(id).map(p -> (AddonProbe) p);
        if (unique.isEmpty() && WARNED.add(id)) {
            LOGGER.warn("An evolution rite names the probe \"{}\", which does not exist: that recipe is unavailable.", id);
        }
        return unique;
    }

    /** A chain of ours that has no addon behind it (Sage to Great Sage): there is nothing to ask, the engine checks what the recipe needs itself. */
    private static final class OwnProbe implements AddonProbe {
        @Override
        public Optional<String> missingHook() {
            return Optional.empty();
        }

        @Override
        public boolean roadOpen() {
            return true;
        }

        @Override
        public boolean satisfied(ServerPlayer target, EvolutionRecipe recipe) {
            return true;
        }
    }
}
