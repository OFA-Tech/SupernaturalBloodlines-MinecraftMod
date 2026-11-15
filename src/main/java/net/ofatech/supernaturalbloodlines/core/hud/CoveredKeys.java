package net.ofatech.supernaturalbloodlines.core.hud;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Unbinding the keys of other mods that the HUD covers, and giving them back. Pure: a bind is its option string ({@code key.keyboard.g}, or
 * {@code key.keyboard.g:ALT} with a modifier), {@link #UNBOUND} for none. Nothing is ever deleted: a covered key is set to unbound only after
 * its current bind is in the record (the file {@code config/supernaturalbloodlines-hud-keys.json}, see {@link CoverRun}).
 * <ul>
 * <li>Cover: record on the first cover only; unbind. A key that is already unbound is recorded as unbound.</li>
 * <li>A key the player rebound while it was covered (not unbound, not its default) is left alone.</li>
 * <li>A covered key back on its default (Controls' "Reset All") is covered again.</li>
 * <li>Restore: give back the recorded bind to a key that is still unbound; a key the player rebound keeps its new bind. Either way the record is dropped.
 * A recorded id that is no longer in the covered list is restored the same way, even while covering.</li>
 * <li>An id the game does not have (its mod is not loaded) is ignored; its record is kept for when the mod is back.</li>
 * </ul>
 */
public final class CoveredKeys {
    public static final String UNBOUND = "key.keyboard.unknown";

    /** @param set the binds to apply, by key id; @param record the record to save */
    public record Plan(Map<String, String> set, Map<String, String> record) {
    }

    private CoveredKeys() {
    }

    /**
     * @param current  the bind of every key the game has, by id
     * @param defaults the mod's own default bind, by id
     * @param covered  the ids to cover
     * @param record   the saved record
     * @param hide     true to cover, false to restore
     */
    public static Plan plan(Map<String, String> current, Map<String, String> defaults, Collection<String> covered, Map<String, String> record, boolean hide) {
        Map<String, String> set = new LinkedHashMap<>();
        Map<String, String> newRecord = new LinkedHashMap<>(record);
        Set<String> coveredIds = new HashSet<>(covered);
        for (Map.Entry<String, String> saved : record.entrySet()) {
            if (!hide || !coveredIds.contains(saved.getKey())) {
                restore(saved.getKey(), saved.getValue(), current, set, newRecord);
            }
        }
        if (hide) {
            for (String id : covered) {
                String now = current.get(id);
                if (now == null) {
                    continue;
                }
                if (!newRecord.containsKey(id)) {
                    newRecord.put(id, now);
                    if (!now.equals(UNBOUND)) {
                        set.put(id, UNBOUND);
                    }
                } else if (!now.equals(UNBOUND) && now.equals(defaults.get(id))) {
                    set.put(id, UNBOUND);
                }
            }
        }
        return new Plan(set, newRecord);
    }

    private static void restore(String id, String saved, Map<String, String> current, Map<String, String> set, Map<String, String> newRecord) {
        String now = current.get(id);
        if (now == null) {
            return;
        }
        if (now.equals(UNBOUND) && !saved.equals(UNBOUND)) {
            set.put(id, saved);
        }
        newRecord.remove(id);
    }
}
