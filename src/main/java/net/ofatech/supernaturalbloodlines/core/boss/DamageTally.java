package net.ofatech.supernaturalbloodlines.core.boss;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Who did the most to bring a boss down (R179): the damage each player dealt, and the order of the last blow of each. The most damage wins; a tie goes to whoever
 * hit last. Mobs, pets and the environment are never entered here: a pet''s damage is its owner''s, and the caller decides who is behind a blow.
 */
public final class DamageTally {
    public record Entry(float amount, long seq) {
    }

    private final Map<UUID, Entry> entries = new LinkedHashMap<>();
    private long seq;

    /** A blow of {@code amount} by {@code who}; nothing for a blow that did no damage. */
    public void add(UUID who, float amount) {
        if (!(amount > 0F)) {
            return;
        }
        Entry before = entries.get(who);
        entries.put(who, new Entry((before == null ? 0F : before.amount()) + amount, ++seq));
    }

    /** Puts back what was saved. */
    public void put(UUID who, float amount, long order) {
        entries.put(who, new Entry(amount, order));
        seq = Math.max(seq, order);
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public Map<UUID, Entry> entries() {
        return Map.copyOf(entries);
    }

    public Optional<UUID> top() {
        UUID best = null;
        Entry bestEntry = null;
        for (Map.Entry<UUID, Entry> e : entries.entrySet()) {
            Entry v = e.getValue();
            if (bestEntry == null || v.amount() > bestEntry.amount() || v.amount() == bestEntry.amount() && v.seq() > bestEntry.seq()) {
                best = e.getKey();
                bestEntry = v;
            }
        }
        return Optional.ofNullable(best);
    }
}