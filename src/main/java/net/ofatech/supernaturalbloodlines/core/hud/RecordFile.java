package net.ofatech.supernaturalbloodlines.core.hud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.stream.Stream;

/**
 * The record of the original binds ({@code config/supernaturalbloodlines-hud-keys.json}): a JSON object of key id to bind.
 * <ul>
 * <li>Written to {@code .tmp} and moved over the file in one step, so a crash never leaves half a record.</li>
 * <li>An unreadable file is never overwritten: it is set aside as {@code <file>.bad-<millis>} (a second one never replaces the first), and while
 * any {@code .bad*} file is there, {@link #blocked()} is true and nothing is written. To recover by hand, open the newest {@code .bad-*} file, copy
 * its good lines into the key options yourself, then delete the {@code .bad-*} files.</li>
 * </ul>
 */
public final class RecordFile implements CoverRun.Store {
    private static final Type TYPE = new TypeToken<LinkedHashMap<String, String>>() { }.getType();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path file;
    private final LongSupplier clock;
    private final Consumer<String> warn;
    private boolean quarantineFailed;

    public RecordFile(Path file, LongSupplier clock, Consumer<String> warn) {
        this.file = file;
        this.clock = clock;
        this.warn = warn;
    }

    private String name() {
        return file.getFileName().toString();
    }

    /** True while an unreadable record is waiting for the player (a {@code .bad} file is there, or it could not be set aside). */
    @Override
    public boolean blocked() {
        if (quarantineFailed) {
            return true;
        }
        try (Stream<Path> siblings = Files.list(file.toAbsolutePath().getParent())) {
            return siblings.anyMatch(p -> p.getFileName().toString().startsWith(name() + ".bad"));
        } catch (IOException e) {
            return true; // cannot tell: do not write
        }
    }

    @Override
    public Map<String, String> read() {
        if (!Files.isRegularFile(file)) {
            return new LinkedHashMap<>();
        }
        try (Reader in = Files.newBufferedReader(file)) {
            Map<String, String> read = GSON.fromJson(in, TYPE);
            return read == null ? new LinkedHashMap<>() : read;
        } catch (IOException | RuntimeException e) {
            setAside(e);
            return new LinkedHashMap<>();
        }
    }

    private void setAside(Exception cause) {
        long stamp = clock.getAsLong();
        Path bad = file.resolveSibling(name() + ".bad-" + stamp);
        for (int n = 1; Files.exists(bad); n++) {
            bad = file.resolveSibling(name() + ".bad-" + stamp + "-" + n);
        }
        try {
            Files.move(file, bad);
            warn.accept(file + " is unreadable (" + cause + "); kept as " + bad.getFileName() + ". Covered keys stay as they are until you deal with it (see the wiki, Key binds)");
        } catch (IOException moveFailed) {
            quarantineFailed = true;
            warn.accept(file + " is unreadable (" + cause + ") and cannot be set aside (" + moveFailed + "); covered keys stay as they are");
        }
    }

    @Override
    public boolean write(Map<String, String> record) {
        if (blocked()) {
            return false;
        }
        Path tmp = file.resolveSibling(name() + ".tmp");
        try {
            Files.writeString(tmp, GSON.toJson(record, TYPE));
            try {
                Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            warn.accept("cannot save " + file + ": " + e);
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException ignored) {
                // the next write replaces it
            }
            return false;
        }
    }
}
