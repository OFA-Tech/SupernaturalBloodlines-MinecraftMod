package net.ofatech.supernaturalbloodlines.shared.admin;

import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * One setting the admin path may change. {@code id} is {@code file:path} (a game rule is {@code gamerule:name}); the range and default are
 * read from the config spec, never copied here.
 *
 * @param clientCopy   clients also read this COMMON key, so the server pushes it to them
 * @param hiddenReason non-null: the key is in the table (so the coverage test knows it) but never shown or written
 */
public record SettingEntry(String id, String file, List<String> path, String section, SettingKind kind, Restart restart, boolean clientCopy,
                           @Nullable String hiddenReason) {
    public enum Restart { NONE, SERVER_RESTART }

    public static final String RULE_FILE = "gamerule";

    public SettingEntry {
        path = List.copyOf(path);
    }

    public static SettingEntry of(String file, String section, String dottedPath, SettingKind kind) {
        return new SettingEntry(file + ":" + dottedPath, file, List.of(dottedPath.split("\\.")), section, kind, Restart.NONE, false, null);
    }

    public static SettingEntry rule(String name, SettingKind kind) {
        return new SettingEntry(RULE_FILE + ":" + name, RULE_FILE, List.of(name), "gamerules", kind, Restart.NONE, false, null);
    }

    public SettingEntry needsRestart() {
        return new SettingEntry(id, file, path, section, kind, Restart.SERVER_RESTART, clientCopy, hiddenReason);
    }

    public SettingEntry withClientCopy() {
        return new SettingEntry(id, file, path, section, kind, restart, true, hiddenReason);
    }

    public SettingEntry hide(String reason) {
        return new SettingEntry(id, file, path, section, kind, restart, clientCopy, reason);
    }

    public boolean isHidden() {
        return hiddenReason != null;
    }

    public String dottedPath() {
        return String.join(".", path);
    }
}
