package net.ofatech.supernaturalbloodlines.core.wheel;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import net.ofatech.supernaturalbloodlines.core.hud.RecordFile;

/**
 * The power the G tap repeats: the last one used from the wheel and the mode it was used in, kept in {@code config/supernaturalbloodlines-wheel-last.json} through the same
 * atomic, quarantining file the key record uses. An unreadable file means none (it is set aside, never a crash and never overwritten); with {@code wheel_remember_last}
 * off nothing is read or written (the last power lives in memory only).
 */
public final class LastPower {
    public record Last(PowerRef ref, int mode) {
    }

    private final RecordFile file;
    private final BooleanSupplier remember;
    private Last last;
    private boolean loaded;

    public LastPower(Path path, LongSupplier clock, Consumer<String> warn, BooleanSupplier remember) {
        this.file = new RecordFile(path, clock, warn);
        this.remember = remember;
    }

    public Optional<Last> get() {
        if (!loaded) {
            loaded = true;
            if (remember.getAsBoolean()) {
                last = read();
            }
        }
        return Optional.ofNullable(last);
    }

    public void set(PowerRef ref, int mode) {
        Last next = new Last(ref, Math.max(0, mode));
        if (loaded && next.equals(last)) {
            return; // the same power again (a held key, a tap on repeat) is not worth a file write
        }
        loaded = true;
        last = next;
        if (remember.getAsBoolean()) {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("power", ref.key());
            m.put("mode", Integer.toString(last.mode()));
            file.write(m);
        }
    }

    /** Forgets what was loaded (the next {@link #get} reads the file again). */
    public void reload() {
        loaded = false;
        last = null;
    }

    private Last read() {
        Map<String, String> m = file.read();
        Optional<PowerRef> ref = PowerRef.parse(m.get("power"));
        if (ref.isEmpty()) {
            return null;
        }
        try {
            return new Last(ref.get(), Math.max(0, Integer.parseInt(m.getOrDefault("mode", "0"))));
        } catch (NumberFormatException bad) {
            return new Last(ref.get(), 0);
        }
    }
}
