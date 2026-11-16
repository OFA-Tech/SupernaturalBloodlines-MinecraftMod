package net.ofatech.supernaturalbloodlines.shared.hud;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;

/**
 * "Track on HUD": the hub (supernatural part) keeps which task you track and says how it stands; the HUD part draws that line. They meet here so neither
 * imports the other. {@link #available()} is true only when both are installed, which is when the hub's links are on.
 */
public final class TrackerAccess {
    /** @param text the task's line ("Endure thirst of 15 or more"), @param done and @param total its progress (total 0 for a task without a count) */
    public record Line(String text, long done, long total) {
        public float fraction() {
            return total <= 0 ? 0f : Math.min(1f, done / (float) total);
        }
    }

    private static volatile Supplier<Optional<Line>> line = Optional::empty;
    private static volatile Supplier<String> tracked = () -> null;
    private static volatile Consumer<String> setter = id -> { };
    private static volatile boolean hub;
    private static volatile boolean hud;

    private TrackerAccess() {
    }

    /** The hub part installs how a tracked task reads and is kept. */
    public static void installHub(Supplier<Optional<Line>> lineOf, Supplier<String> trackedId, Consumer<String> set) {
        line = lineOf;
        tracked = trackedId;
        setter = set;
        hub = true;
    }

    /** The HUD part says it can draw the line. */
    public static void installHud() {
        hud = true;
    }

    public static boolean available() {
        return hub && hud;
    }

    public static Optional<Line> line() {
        return line.get();
    }

    @Nullable
    public static String tracked() {
        return tracked.get();
    }

    public static boolean isTracked(String id) {
        return id.equals(tracked.get());
    }

    /** Tracks {@code id}, or stops tracking it when it is the tracked one. */
    public static void toggle(String id) {
        setter.accept(isTracked(id) ? null : id);
    }
}
