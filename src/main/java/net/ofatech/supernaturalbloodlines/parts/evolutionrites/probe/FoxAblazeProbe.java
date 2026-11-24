package net.ofatech.supernaturalbloodlines.parts.evolutionrites.probe;

import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.unique.ITrulyUnique;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.Recipes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FoxAblaze's one-owner rule for Raphael, read the way its own fusion reads it (its check is private, so this repeats it from public parts: the game rule key and
 * Tensura's unique storage). The rule is on and, for the Raphael or the Ciel id, the storage names an owner who is not the player. Never throws: a hook that moved
 * means "not owned" and one warning, so a changed addon cannot make the merge refuse or grant by accident (the real-jar test pins the happy path).
 */
public final class FoxAblazeProbe {
    public static final String MOD = "foxablazeultimate";
    public static final ResourceLocation RAPHAEL = ResourceLocation.fromNamespaceAndPath(MOD, "raphael");
    public static final ResourceLocation CIEL = ResourceLocation.fromNamespaceAndPath(MOD, "ciel");
    private static final String RULES = "com.foxablazeultimate.world.FoxAblazeGameRules";
    private static final Logger LOGGER = LoggerFactory.getLogger("supernaturalbloodlines");
    private static volatile boolean warned;

    /** Test seams: null in play. */
    public static volatile BooleanSupplier loadedHook = null;
    public static volatile Predicate<ServerPlayer> ownedHook = null;

    private FoxAblazeProbe() {
    }

    public static boolean loaded() {
        BooleanSupplier hook = loadedHook;
        return hook != null ? hook.getAsBoolean() : Recipes.modLoaded(MOD);
    }

    /** Whether the world's single-owner rule for Raphael is on (the game rule {@code RAPHAEL_TRULY_UNIQUE}); a missing rule is off. */
    @SuppressWarnings("unchecked")
    public static boolean ruleOn(ServerPlayer player) {
        Hooks.FieldHit hit = Hooks.staticField(RULES, "RAPHAEL_TRULY_UNIQUE");
        if (hit.value().orElse(null) instanceof GameRules.Key<?> key) {
            return player.level().getGameRules().getBoolean((GameRules.Key<GameRules.BooleanValue>) key);
        }
        warnOnce(hit.missing().orElse("RAPHAEL_TRULY_UNIQUE is not a game rule key"));
        return false;
    }

    /** Owned by another player: the storage names an owner for any of these ids and it is not the asker. */
    public static boolean ownedBy(ITrulyUnique storage, UUID asker, ResourceLocation... ids) {
        for (ResourceLocation id : ids) {
            if (storage.hasSkill(id)) {
                UUID owner = storage.getOwner(id);
                if (owner != null && !owner.equals(asker)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** What FoxAblaze's own fusion asks, for the Raphael and the Ciel: the rule is on and another player owns either. */
    public static boolean ownedByOther(ServerPlayer player) {
        Predicate<ServerPlayer> hook = ownedHook;
        if (hook != null) {
            return hook.test(player);
        }
        try {
            return ruleOn(player) && ownedBy(TensuraStorages.getUniqueStorageFrom(player.server.overworld()), player.getUUID(), RAPHAEL, CIEL);
        } catch (RuntimeException | LinkageError e) {
            warnOnce(e.toString());
            return false;
        }
    }

    private static void warnOnce(String what) {
        if (!warned) {
            warned = true;
            LOGGER.warn("FoxAblaze's single-owner rule could not be read ({}): Raphael is treated as owned by nobody.", what);
        }
    }
}
