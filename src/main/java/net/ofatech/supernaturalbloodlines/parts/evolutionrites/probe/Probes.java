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
    public static final String ASCENSION_AWAKENING = "ascension.awakening";
    private static final Logger LOGGER = LoggerFactory.getLogger("supernaturalbloodlines");

    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();
    public static final String GREAT_SAGE_RAPHAEL = "greatsage.raphael";
    private static final Map<String, AddonProbe> FIXED = Map.of(OWN_SAGE_GREAT_SAGE, new OwnProbe(), GREAT_SAGE_RAPHAEL, new GreatSageProbe(),
            ASCENSION_AWAKENING, new AscensionProbe());

    private Probes() {
    }

    /** A test seam: probes that stand in for an id (a fixture whose world switch is off, one that records what it was told). Empty in play. */
    public static volatile Map<String, AddonProbe> overrides = Map.of();

    public static Optional<AddonProbe> of(String id) {
        AddonProbe stand = overrides.get(id);
        if (stand != null) {
            return Optional.of(stand);
        }
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
