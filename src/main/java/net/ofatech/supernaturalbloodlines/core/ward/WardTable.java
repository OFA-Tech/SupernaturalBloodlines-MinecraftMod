package net.ofatech.supernaturalbloodlines.core.ward;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * The wards as a model (R300): who owns which, who is invited, which have fallen. No Minecraft types (the dimension is a string), so the rules
 * run in plain JUnit; {@code WardIndex} wraps it in a SavedData.
 */
public final class WardTable {
    /** One ward. {@code invited} holds the owner and whoever they invited. */
    public static final class Ward {
        private final String id;
        private final WardRules.Kind kind;
        private final String dimension;
        private final int cx;
        private final int cy;
        private final int cz;
        private final int half;
        private final UUID owner;
        private final Set<UUID> invited = new LinkedHashSet<>();
        private boolean fallen;

        public Ward(String id, WardRules.Kind kind, String dimension, int cx, int cy, int cz, int half, UUID owner) {
            this.id = id;
            this.kind = kind;
            this.dimension = dimension;
            this.cx = cx;
            this.cy = cy;
            this.cz = cz;
            this.half = half;
            this.owner = owner;
            invited.add(owner);
        }

        public String id() {
            return id;
        }

        public WardRules.Kind kind() {
            return kind;
        }

        public String dimension() {
            return dimension;
        }

        public int cx() {
            return cx;
        }

        public int cy() {
            return cy;
        }

        public int cz() {
            return cz;
        }

        public int half() {
            return half;
        }

        public UUID owner() {
            return owner;
        }

        public Set<UUID> invited() {
            return invited;
        }

        public boolean fallen() {
            return fallen;
        }

        public void setFallen(boolean value) {
            fallen = value;
        }

        public WardRules.Box box() {
            return new WardRules.Box(cx, cy, cz, half);
        }
    }

    private final Map<String, Ward> wards = new LinkedHashMap<>();

    public List<Ward> all() {
        return new ArrayList<>(wards.values());
    }

    public Optional<Ward> get(String id) {
        return Optional.ofNullable(wards.get(id));
    }

    /** Puts a ward in as it was saved. */
    public void restore(Ward ward) {
        wards.put(ward.id(), ward);
    }

    public int countStanding(UUID owner) {
        return (int) wards.values().stream().filter(w -> w.owner().equals(owner) && !w.fallen()).count();
    }

    /**
     * Raises a ward, or empty when the owner already has {@code maxPerOwner} standing ones. {@code ids} gives candidate ids (8 hex digits);
     * one that is taken is asked for again.
     */
    public Optional<Ward> add(WardRules.Kind kind, String dimension, int x, int y, int z, int half, UUID owner, int maxPerOwner, Supplier<String> ids) {
        if (countStanding(owner) >= maxPerOwner) {
            return Optional.empty();
        }
        String id = ids.get();
        while (wards.containsKey(id)) {
            id = ids.get();
        }
        Ward ward = new Ward(id, kind, dimension, x, y, z, half, owner);
        wards.put(id, ward);
        return Optional.of(ward);
    }

    /** Removes the ward; only its owner may. */
    public boolean drop(String id, UUID requester) {
        Ward w = wards.get(id);
        if (w == null || !w.owner().equals(requester)) {
            return false;
        }
        wards.remove(id);
        return true;
    }

    public List<Ward> standingOf(UUID owner) {
        return wards.values().stream().filter(w -> w.owner().equals(owner) && !w.fallen()).toList();
    }

    public List<Ward> ownedBy(UUID owner) {
        return wards.values().stream().filter(w -> w.owner().equals(owner)).toList();
    }

    /** The standing wards of other people whose list has {@code who}. */
    public List<Ward> welcoming(UUID who) {
        return wards.values().stream().filter(w -> !w.fallen() && !w.owner().equals(who) && w.invited().contains(who)).toList();
    }

    /**
     * Invites {@code who} into every standing ward of {@code owner} (a home is one place for the owner, whichever of their wards it is).
     * {@code ADDED} when at least one list took them, {@code ALREADY} when every list had them, {@code FULL} when a list that lacked them was full.
     * Empty when the owner has no standing ward.
     */
    public Optional<WardRules.InviteResult> inviteAll(UUID owner, UUID who, int max) {
        List<Ward> mine = standingOf(owner);
        if (mine.isEmpty()) {
            return Optional.empty();
        }
        boolean added = false;
        boolean full = false;
        for (Ward w : mine) {
            WardRules.InviteResult r = WardRules.invite(w.invited(), who, max);
            added |= r == WardRules.InviteResult.ADDED;
            full |= r == WardRules.InviteResult.FULL;
        }
        return Optional.of(full ? WardRules.InviteResult.FULL : added ? WardRules.InviteResult.ADDED : WardRules.InviteResult.ALREADY);
    }

    /** Withdraws {@code who} from every ward of {@code owner}; the owner cannot be withdrawn. @return whether anyone was removed */
    public boolean uninviteAll(UUID owner, UUID who) {
        if (owner.equals(who)) {
            return false;
        }
        boolean removed = false;
        for (Ward w : ownedBy(owner)) {
            removed |= w.invited().remove(who);
        }
        return removed;
    }

    /** Every ward of {@code owner} falls (R307); idempotent. @return how many fell now */
    public int fallAllOf(UUID owner) {
        int n = 0;
        for (Ward w : ownedBy(owner)) {
            if (!w.fallen()) {
                w.setFallen(true);
                n++;
            }
        }
        return n;
    }
}
