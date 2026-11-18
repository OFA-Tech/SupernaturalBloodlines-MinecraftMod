package net.ofatech.supernaturalbloodlines.shared.hub.profile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Who a picker offers: the players online, nearest first, less yourself and whoever the action cannot apply to. Pure. */
public final class Pickable {
    /** @param distance blocks from the viewer, or -1 when that player is not near enough to be loaded on this client */
    public record Person(UUID id, String name, double distance) {
    }

    private Pickable() {}

    /** A picker never lists more than this many, the nearest: a server of hundreds must not make a wall of buttons. */
    public static final int MAX = 30;

    public static List<Person> of(List<Person> online, UUID self, Set<UUID> except) {
        List<Person> out = new ArrayList<>();
        for (Person p : online) {
            if (!p.id().equals(self) && !except.contains(p.id())) {
                out.add(p);
            }
        }
        // those in sight first (by distance), then everyone else by name
        out.sort(Comparator.comparing((Person p) -> p.distance() < 0).thenComparingDouble(Person::distance).thenComparing(Person::name, String.CASE_INSENSITIVE_ORDER));
        return out.size() > MAX ? List.copyOf(out.subList(0, MAX)) : out;
    }
}

