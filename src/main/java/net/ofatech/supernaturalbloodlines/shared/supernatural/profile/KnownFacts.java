package net.ofatech.supernaturalbloodlines.shared.supernatural.profile;

import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * What one observer (or one pack's pool) has discovered about other people: subject to the facts known of them. Capped at
 * {@value #MAX_SUBJECTS} subjects, the oldest dropped first, so a long-lived world cannot grow it without bound. Reading from storage is
 * lenient: a bad id, an unknown fact name or a duplicate is skipped, and an over-long list keeps its newest entries, never an error.
 */
public final class KnownFacts {
    public static final int MAX_SUBJECTS = 256;

    private final LinkedHashMap<UUID, Set<Fact>> bySubject = new LinkedHashMap<>();

    public void add(UUID subject, Collection<Fact> facts) {
        if (facts.isEmpty()) {
            return;
        }
        Set<Fact> set = bySubject.remove(subject); // re-added at the end: the newest is kept longest
        if (set == null) {
            set = EnumSet.noneOf(Fact.class);
        }
        set.addAll(facts);
        bySubject.put(subject, set);
        while (bySubject.size() > MAX_SUBJECTS) {
            bySubject.remove(bySubject.keySet().iterator().next());
        }
    }

    public Set<Fact> facts(UUID subject) {
        Set<Fact> set = bySubject.get(subject);
        return set == null ? EnumSet.noneOf(Fact.class) : EnumSet.copyOf(set);
    }

    public void mergeFrom(KnownFacts other) {
        other.bySubject.forEach(this::add);
    }

    public int size() {
        return bySubject.size();
    }

    public boolean isEmpty() {
        return bySubject.isEmpty();
    }

    /** For storage: subject id to fact names, oldest first. */
    public Map<String, List<String>> toStrings() {
        Map<String, List<String>> out = new LinkedHashMap<>();
        bySubject.forEach((id, facts) -> out.put(id.toString(), facts.stream().map(Enum::name).toList()));
        return out;
    }

    /** Lenient read of {@link #toStrings}: anything that does not parse is left out. */
    public static KnownFacts fromStrings(Map<String, ? extends Collection<String>> raw) {
        KnownFacts out = new KnownFacts();
        if (raw == null) {
            return out;
        }
        raw.forEach((id, names) -> {
            UUID subject;
            try {
                subject = UUID.fromString(id);
            } catch (RuntimeException bad) {
                return;
            }
            Set<Fact> facts = EnumSet.noneOf(Fact.class);
            if (names != null) {
                for (String name : names) {
                    try {
                        facts.add(Fact.valueOf(name));
                    } catch (RuntimeException unknown) {
                        // a fact a later version knows: skipped
                    }
                }
            }
            out.add(subject, facts);
        });
        return out;
    }
}

