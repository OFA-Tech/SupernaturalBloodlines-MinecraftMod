package net.ofatech.supernaturalbloodlines.shared.supernatural.profile;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Which facts of a profile the server reads and sends. A fact is sent when the table says KNOWN, or when it is DISCOVERED (or an ability
 * taught it) and the observer has discovered it. The reader is called only for facts that are sent: a hidden fact is never even looked up.
 * A stranger gets no fields at all: the client already has their name and online dot from its own player list.
 */
public final class ProfileView {
    private ProfileView() {}

    public static Set<Fact> shown(Relation relation, Set<Fact> discovered) {
        Set<Fact> out = EnumSet.noneOf(Fact.class);
        if (relation == Relation.STRANGER_OFFLINE) {
            return out;
        }
        for (Fact f : Fact.values()) {
            boolean base = Knowledge.visible(relation, f) == Visibility.KNOWN;
            if (relation == Relation.STRANGER) {
                base = false;
            }
            if (base || discovered.contains(f)) {
                out.add(f);
            }
        }
        return out;
    }

    /** The facts to send, each with its value parts: an empty list is an explicit "none" (no pack, no sire), an absent fact is unknown. */
    public static Map<Fact, List<String>> build(Relation relation, Set<Fact> discovered, Function<Fact, Optional<List<String>>> read) {
        Map<Fact, List<String>> out = new EnumMap<>(Fact.class);
        for (Fact f : shown(relation, discovered)) {
            read.apply(f).ifPresent(v -> out.put(f, List.copyOf(v)));
        }
        return out;
    }
}

