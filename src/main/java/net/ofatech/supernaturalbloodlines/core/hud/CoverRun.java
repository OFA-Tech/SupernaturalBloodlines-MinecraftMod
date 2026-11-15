package net.ofatech.supernaturalbloodlines.core.hud;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * One covering or restoring run over the game's keys, in the order that can never lose a bind of the player's.
 * <ul>
 * <li>Covering: the record is written <b>first</b>; if that fails, no key is touched. Only then are the keys unbound and the options saved.</li>
 * <li>Restoring: the keys are set and saved first and the record entries are dropped <b>after</b>, so a crash in between leaves a stale record
 * (harmless: that key is bound, so it is skipped next time), never an unbound key whose record is gone.</li>
 * <li>While the store is blocked (an unreadable record was set aside and is waiting for the player), nothing is covered.</li>
 * <li>A recorded bind that cannot be applied stays in the record.</li>
 * </ul>
 */
public final class CoverRun {
    public interface Keys {
        Map<String, String> current();

        Map<String, String> defaults();

        /** @return false when the bind cannot be applied (the key is left as it is) */
        boolean set(String id, String bind);

        void save();
    }

    public interface Store {
        Map<String, String> read();

        boolean write(Map<String, String> record);

        boolean blocked();
    }

    public enum Outcome { DONE, BLOCKED, RECORD_NOT_SAVED }

    public record Result(Outcome outcome, List<String> changed) {
    }

    private CoverRun() {
    }

    public static Result run(Keys keys, Store store, Collection<String> covered, boolean hide) {
        Map<String, String> record = store.read();
        if (hide && store.blocked()) {
            return new Result(Outcome.BLOCKED, List.of());
        }
        CoveredKeys.Plan plan = CoveredKeys.plan(keys.current(), keys.defaults(), covered, record, hide);
        if (hide && !plan.record().equals(record) && !store.write(plan.record())) {
            return new Result(Outcome.RECORD_NOT_SAVED, List.of());
        }
        Map<String, String> kept = new LinkedHashMap<>(plan.record());
        List<String> changed = new ArrayList<>();
        for (Map.Entry<String, String> change : plan.set().entrySet()) {
            if (keys.set(change.getKey(), change.getValue())) {
                changed.add(change.getKey());
            } else if (record.containsKey(change.getKey())) {
                kept.put(change.getKey(), record.get(change.getKey()));
            }
        }
        if (!changed.isEmpty()) {
            keys.save();
        }
        if (!hide && !kept.equals(record) && !store.write(kept)) {
            return new Result(Outcome.RECORD_NOT_SAVED, changed);
        }
        return new Result(Outcome.DONE, changed);
    }
}
