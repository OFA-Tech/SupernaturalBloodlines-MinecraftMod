package net.ofatech.supernaturalbloodlines.shared.admin;

import java.util.List;
import java.util.Optional;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

/**
 * The one way a setting changes from the hub: permission, allow-list, value check, then {@code set} and {@code save}, in that order and
 * only when everything before said yes. The writer is the real config in the game and a fake in a test. Pure apart from the writer.
 */
public final class AdminSettings {
    public enum Status { OK, NOT_OP, UNKNOWN, HIDDEN, NOT_LOADED, INVALID, SAVE_FAILED }

    /** @param restartRequired true for a key read only at start (the part switches); {@code detail} is the offending line or the error text */
    public record Result(Status status, boolean restartRequired, String refusalKey, String detail) {
        static Result of(Status status) {
            return new Result(status, false, "", "");
        }

        public boolean ok() {
            return status == Status.OK;
        }
    }

    /** What the server does to the world. */
    public interface Writer {
        void set(ConfigValue<?> value, Object boxed);

        void save(ModConfigSpec spec);

        void setRule(SettingEntry rule, Object boxed);
    }

    private static final List<java.util.function.Consumer<net.minecraft.server.MinecraftServer>> CLIENT_COPY_SAVED = new java.util.concurrent.CopyOnWriteArrayList<>();

    private AdminSettings() {}

    /** A part that has COMMON keys its clients also read calls this to push them at once when the hub saves one (the file watcher would only get there later). */
    public static void onClientCopySaved(java.util.function.Consumer<net.minecraft.server.MinecraftServer> listener) {
        CLIENT_COPY_SAVED.add(listener);
    }

    /** The real handler calls it after a successful save of a key whose {@link SettingEntry#clientCopy()} is set. */
    public static void clientCopySaved(net.minecraft.server.MinecraftServer server) {
        CLIENT_COPY_SAVED.forEach(l -> l.accept(server));
    }

    public static Result apply(int permissionLevel, String id, SettingValue value, Writer writer) {
        return apply(AdminEntries.ALL, AdminEntries.RULES, permissionLevel, id, value, writer);
    }

    @SuppressWarnings("unchecked")
    public static Result apply(List<SettingEntry> keys, List<SettingEntry> rules, int permissionLevel, String id, SettingValue value, Writer writer) {
        if (!AdminAccess.allowed(permissionLevel)) {
            return Result.of(Status.NOT_OP);
        }
        Optional<SettingEntry> found = AdminEntries.find(keys, rules, id);
        if (found.isEmpty()) {
            return Result.of(Status.UNKNOWN);
        }
        SettingEntry entry = found.get();
        if (AdminSpecs.isClient(entry.file())) {
            return Result.of(Status.UNKNOWN); // a CLIENT file is the player's own, never an operator's to write
        }
        if (entry.isHidden()) {
            return Result.of(Status.HIDDEN);
        }
        if (entry.kind().isRule()) {
            Optional<SettingRules.Refusal> bad = SettingRules.checkRule(entry.kind(), value);
            if (bad.isPresent()) {
                return invalid(bad.get());
            }
            writer.setRule(entry, SettingRules.boxed(value));
            return new Result(Status.OK, false, "", "");
        }
        Optional<ConfigValue<?>> live = AdminSpecs.value(entry);
        Optional<ModConfigSpec> spec = AdminSpecs.spec(entry.file());
        if (live.isEmpty() || spec.isEmpty()) {
            return Result.of(Status.NOT_LOADED);
        }
        ConfigValue<Object> cv = (ConfigValue<Object>) live.get();
        Optional<SettingRules.Refusal> bad = SettingRules.check(cv.getSpec(), entry.kind(), value, AdminSpecs.lineCheck(entry.id()));
        if (bad.isPresent()) {
            return invalid(bad.get());
        }
        Object before = cv.getRaw();
        writer.set(cv, SettingRules.boxed(value));
        try {
            writer.save(spec.get());
        } catch (RuntimeException e) {
            // the file could not be written: the old value goes back so memory and file agree
            writer.set(cv, before);
            return new Result(Status.SAVE_FAILED, false, "", String.valueOf(e.getMessage()));
        }
        return new Result(Status.OK, entry.restart() == SettingEntry.Restart.SERVER_RESTART, "", "");
    }

    private static Result invalid(SettingRules.Refusal r) {
        return new Result(Status.INVALID, false, r.key(), r.detail());
    }
}
