package net.ofatech.supernaturalbloodlines.parts.hud.client.wheel.infinity;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.ofatech.supernaturalbloodlines.core.wheel.StoneRules;
import net.ofatech.supernaturalbloodlines.core.wheel.StoneRules.State;
import org.jetbrains.annotations.Nullable;

/**
 * Decides once whether the Infinity compat works: the mod is not there (silent), the setting is off, or his classes are not what we verified (one warning naming what, then
 * off for the session). Instances so the tests can run it with a fake mod and a fake logger; {@link InfinityCompat} holds the real one.
 */
public final class InfinityProbe {
    public interface Factory {
        StoneApi create() throws ReflectiveStoneApi.Missing;
    }

    private final BooleanSupplier modLoaded;
    private final BooleanSupplier settingOn;
    private final Factory factory;
    private final Consumer<String> warn;
    private StoneApi api;
    private boolean resolved;
    private boolean broken;

    public InfinityProbe(BooleanSupplier modLoaded, BooleanSupplier settingOn, Factory factory, Consumer<String> warn) {
        this.modLoaded = modLoaded;
        this.settingOn = settingOn;
        this.factory = factory;
        this.warn = warn;
    }

    /** The state now: the setting is read every time (a player can turn it off), the classes once. */
    public State state() {
        if (!modLoaded.getAsBoolean()) {
            return State.OFF_MISSING;
        }
        if (!settingOn.getAsBoolean()) {
            return State.OFF_SETTING;
        }
        resolve();
        return broken ? State.OFF_BROKEN : State.ACTIVE;
    }

    /** The API, only while {@link #state()} is ACTIVE. */
    @Nullable
    public StoneApi api() {
        return state() == State.ACTIVE ? api : null;
    }

    private void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            api = factory.create();
        } catch (ReflectiveStoneApi.Missing missing) {
            broken = true;
            warn.accept("Infinity Stones compat off: " + missing.getMessage());
        } catch (RuntimeException | LinkageError other) {
            broken = true;
            warn.accept("Infinity Stones compat off: " + other);
        }
    }

    /** True when the soul control entry can work (and its key may be covered). */
    public boolean soulControlCovered() {
        StoneApi a = api();
        return StoneRules.soulControlKeyCovered(state(), a != null && a.soulSupported());
    }
}
