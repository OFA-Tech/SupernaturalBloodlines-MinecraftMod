package net.ofatech.supernaturalbloodlines.parts.evolutionrites.probe;

import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.ofatech.supernaturalbloodlines.core.evolution.AwakeningRules;
import net.ofatech.supernaturalbloodlines.core.evolution.EvolutionRecipe;

/**
 * Asks Ascension what its Awakening Altar asks of a player before it grants an Ultimate (spec 8.2, E2): the rule {@code doAscensionUltimate}, the cooldown, Max EP
 * at or above {@code AddonConfig.AWAKENING_MIN_EP}, and a true Hero or true Demon Lord. Every value is read live by reflection (no compile dependency, no copied
 * number); the numbers go through {@link AwakeningRules}. The pair's own gate, the Unique held and mastered, is the recipe's {@code needs}. A completed rite
 * writes the altar's own cooldown tag, so the two roads share one cooldown.
 */
public final class AscensionProbe implements AddonProbe {
    public static final String LAST_AWAKENING_TAG = "ascension:last_awakening_tick";
    static final String CONFIG = "com.simplygray.tensuraaddon.AddonConfig";
    static final String RULES = "com.simplygray.tensuraaddon.AddonGameRules";
    static final String MIN_EP = "AWAKENING_MIN_EP";
    static final String COOLDOWN = "AWAKENING_COOLDOWN_MINUTES";
    static final String RULE = "DO_ASCENSION_ULTIMATE";

    /** The addon's config as the probe reads it: a test feeds numbers, the game feeds the jar. */
    interface Source {
        Optional<String> missingHook();

        default double minEp() {
            return 0;
        }

        default int cooldownMinutes() {
            return 0;
        }
    }

    /** What is asked of one target at one moment. */
    record Live(double maxEp, boolean hero, boolean demonLord, long last, long now, boolean ruleOn) {
    }

    private final Source source = new Reflective();

    static boolean check(Source source, Live l) {
        return AwakeningRules.satisfied(new AwakeningRules.Facts(l.ruleOn(), l.now(), l.last(), source.cooldownMinutes(), l.maxEp(), source.minEp(), l.hero(), l.demonLord()));
    }

    /** Writes the altar's cooldown tag: the game time of this awakening. */
    static void stamp(CompoundTag persistent, long gameTime) {
        persistent.putLong(LAST_AWAKENING_TAG, gameTime);
    }

    @Override
    public Optional<String> missingHook() {
        return source.missingHook();
    }

    @Override
    public boolean roadOpen() {
        return true;
    }

    @Override
    public boolean enabledIn(ServerLevel level) {
        return ruleOn(level);
    }

    @Override
    public boolean satisfied(ServerPlayer target, EvolutionRecipe recipe) {
        var existence = TensuraStorages.getExistenceFrom(target);
        boolean hero = existence != null && existence.isTrueHero();
        boolean demonLord = existence != null && existence.isTrueDemonLord();
        ServerLevel level = target.serverLevel();
        return check(source, new Live(EnergyHelper.getMaxEP(target), hero, demonLord, target.getPersistentData().getLong(LAST_AWAKENING_TAG), level.getGameTime(), ruleOn(level)));
    }

    @Override
    public void onGranted(ServerPlayer target, EvolutionRecipe recipe) {
        stamp(target.getPersistentData(), target.serverLevel().getGameTime());
    }

    @SuppressWarnings("unchecked")
    private static boolean ruleOn(ServerLevel level) {
        Hooks.FieldHit hit = Hooks.staticField(RULES, RULE);
        if (hit.value().orElse(null) instanceof GameRules.Key<?> key) {
            return level.getGameRules().getBoolean((GameRules.Key<GameRules.BooleanValue>) key);
        }
        return false;
    }

    private static final class Reflective implements Source {
        @Override
        public Optional<String> missingHook() {
            for (String[] hook : new String[][] {{CONFIG, MIN_EP}, {CONFIG, COOLDOWN}, {RULES, RULE}}) {
                Hooks.FieldHit hit = Hooks.staticField(hook[0], hook[1]);
                if (hit.missing().isPresent()) {
                    return hit.missing();
                }
            }
            return Optional.empty();
        }

        @Override
        public double minEp() {
            return number(CONFIG, MIN_EP).doubleValue();
        }

        @Override
        public int cooldownMinutes() {
            return number(CONFIG, COOLDOWN).intValue();
        }

        /** A config that is not loaded yet answers with its default, as a ModConfigSpec value does. */
        private static Number number(String className, String field) {
            Object value = Hooks.staticField(className, field).value().orElse(null);
            if (value instanceof Supplier<?> supplier) {
                try {
                    if (supplier.get() instanceof Number n) {
                        return n;
                    }
                } catch (IllegalStateException notLoaded) {
                    try {
                        if (value.getClass().getMethod("getDefault").invoke(value) instanceof Number n) {
                            return n;
                        }
                    } catch (ReflectiveOperationException e) {
                        // fall through: no value
                    }
                }
            }
            // an unreadable value must never open the road: an infinite threshold and cooldown refuse everyone
            return field.equals(MIN_EP) ? Double.MAX_VALUE : Integer.MAX_VALUE;
        }
    }
}
