package net.ofatech.supernaturalbloodlines.parts.hud.client.wheel.infinity;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Where a failure of a call into his classes goes after the probe passed (his mod changed under us, or sent something we did not expect). A failed action is logged once
 * per action; a failed read of his powers ({@link #broken}) turns the whole compat off, so his own wheel key is given back and the stones are plain Tensura skills again.
 */
public final class StoneFailures {
    public static final StoneFailures NONE = new StoneFailures(m -> { }, m -> { });

    private final Consumer<String> warn;
    private final Consumer<String> broke;
    private final Set<String> seen = new HashSet<>();

    public StoneFailures(Consumer<String> warn, Consumer<String> broke) {
        this.warn = warn;
        this.broke = broke;
    }

    /** An action (a send, a screen) failed: one warning for it, then quiet. */
    public void failed(String what, Throwable cause) {
        if (seen.add(what)) {
            warn.accept("Infinity Stones compat: " + what + " failed: " + cause);
        }
    }

    /** Reading his powers failed: the compat is off from now on ({@link InfinityProbe#broke}). */
    public void broken(String what, Throwable cause) {
        broke.accept(what + ": " + cause);
    }
}
