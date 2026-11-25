package net.ofatech.supernaturalbloodlines.core.group.clan;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** R271, R278: the Charter's state machine, when it is lost, who may forge or reforge, and which of several stacks is the real one. */
public final class CharterRules {
    private static final long DAY = 24L * 60 * 60 * 1000;
    public static final long REFORGE_WAIT = 24L * 60 * 60 * 1000;

    public enum Event {
        SEEN_HELD, ENTITY_DROPPED, NOT_SEEN, ENTITY_DESTROYED, FORGED, REFORGED
    }

    /** One stack of the Charter found in an inventory; {@code index} is its place in the scan. */
    public record Sighting(UUID holder, int index) {
    }

    public record Kept(Optional<Sighting> kept, List<Sighting> dust) {
    }

    private CharterRules() {
    }

    public static CharterState next(CharterState state, Event event) {
        return switch (event) {
            case SEEN_HELD, FORGED, REFORGED -> CharterState.HELD;
            case ENTITY_DROPPED -> CharterState.DROPPED;
            case ENTITY_DESTROYED -> CharterState.LOST;
            case NOT_SEEN -> state == CharterState.LOST ? CharterState.LOST : CharterState.STORED;
        };
    }

    /**
     * No sighting for {@code lostDays} with nobody on record as holding it, or the recorded holder offline for {@code absentDays}
     * ({@code holderOfflineSinceMillis} 0 when they are online; {@code absentDays} 0 means never absent).
     */
    public static boolean isLost(long lastSeenMillis, boolean hasHolder, long holderOfflineSinceMillis, long nowMillis, int lostDays, int absentDays) {
        if (hasHolder) {
            return absentDays > 0 && holderOfflineSinceMillis > 0 && nowMillis - holderOfflineSinceMillis >= absentDays * DAY;
        }
        return nowMillis - lastSeenMillis >= lostDays * DAY;
    }

    /** Empty when allowed, else {@code too_few}, {@code lost_use_reforge} or {@code already}; {@code existing} is null when the clan has no row. */
    public static Optional<String> mayForge(int members, CharterState existing, int minMembers) {
        if (members < minMembers) {
            return Optional.of("too_few");
        }
        if (existing == CharterState.LOST) {
            return Optional.of("lost_use_reforge");
        }
        return existing != null ? Optional.of("already") : Optional.empty();
    }

    /** Empty when allowed, else {@code not_lost} or {@code reforge_wait}. */
    public static Optional<String> mayReforge(CharterState state, long lostAtMillis, long nowMillis) {
        if (state != CharterState.LOST) {
            return Optional.of("not_lost");
        }
        return nowMillis - lostAtMillis >= REFORGE_WAIT ? Optional.empty() : Optional.of("reforge_wait");
    }

    /** The row's holder's first stack survives; with no such holder in the scan, the first stack in scan order. Every other stack is dust. */
    public static Kept keep(List<Sighting> sightings, Optional<UUID> rowHolder) {
        if (sightings.isEmpty()) {
            return new Kept(Optional.empty(), List.of());
        }
        Sighting survivor = rowHolder.flatMap(h -> sightings.stream().filter(s -> s.holder().equals(h)).findFirst()).orElse(sightings.get(0));
        List<Sighting> dust = new ArrayList<>(sightings);
        dust.remove(survivor);
        return new Kept(Optional.of(survivor), List.copyOf(dust));
    }

    public static boolean valid(UUID stackId, UUID rowId, UUID stackGroup, UUID rowGroup) {
        return stackId.equals(rowId) && stackGroup.equals(rowGroup);
    }
}