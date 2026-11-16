package net.ofatech.supernaturalbloodlines.core.bloodline;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * What a pack asks of the Open Parties and Claims party it mirrors (plan 14), as a pure plan: the pack is the party, the
 * founding leader its owner, the other Alphas its admins, everyone else a member; a player who left the party leaves the
 * pack. The party is only ever changed for the people we put in it ({@code ours}).
 */
public final class PartySyncRules {
    private PartySyncRules() {
    }

    /** The party as it is now: {@code members} holds every member, the owner too. */
    public record Party(UUID owner, Set<UUID> members) {
    }

    /**
     * @param create     the pack has no party and its leader gets one
     * @param disband    the party's owner is no longer in the pack: it ends and a new one is made for the leader
     * @param add        who joins the party, true for an admin (an Alpha)
     * @param removeFrom who leaves the party because they left the pack
     * @param leavePack  who leaves the pack because they left the party
     * @param ours       the mirror after this pass (only the leader means: the party was ended, wait for the roster to change)
     */
    public record Plan(boolean create, boolean disband, Map<UUID, Boolean> add, Set<UUID> removeFrom, Set<UUID> leavePack, Set<UUID> ours) {
    }

    /**
     * @param leader         the pack's leader
     * @param pack           everyone in the pack, the leader too
     * @param alphas         the Alphas among them
     * @param party          the party the pack mirrors, or null when there is none
     * @param ours           whom this pack put in that party before
     * @param rosterChanged  somebody joined, left or was kicked now, so a missing party is made again (a party that simply
     *                       vanished is not: its owner disbanded it)
     */
    public static Plan plan(UUID leader, Set<UUID> pack, Set<UUID> alphas, @Nullable Party party, Set<UUID> ours, boolean rosterChanged) {
        Map<UUID, Boolean> add = new LinkedHashMap<>();
        Set<UUID> removeFrom = new LinkedHashSet<>();
        Set<UUID> leavePack = new LinkedHashSet<>();
        Set<UUID> keep = new HashSet<>(ours);
        if (party == null) {
            // a party that was there and is gone was ended by its owner: do not make another until the roster changes. The
            // mirror then holds only the leader, which says "ended" and is no one we added.
            if (!ours.isEmpty() && !rosterChanged) {
                return new Plan(false, false, add, removeFrom, leavePack, new HashSet<>(Set.of(leader)));
            }
            boolean create = pack.size() > 1;
            if (create) {
                for (UUID m : pack) {
                    add.put(m, alphas.contains(m) && !m.equals(leader));
                }
            }
            return new Plan(create, false, add, removeFrom, leavePack, new HashSet<>(add.keySet()));
        }
        if (!pack.contains(party.owner())) {
            // the owner left the pack: the party goes with them and the pack gets its own
            for (UUID m : pack) {
                add.put(m, alphas.contains(m) && !m.equals(leader));
            }
            return new Plan(true, true, add, removeFrom, leavePack, new HashSet<>(add.keySet()));
        }
        for (UUID m : ours) {
            if (!party.members().contains(m) && pack.contains(m) && !m.equals(party.owner())) {
                leavePack.add(m);
                keep.remove(m);
            } else if (!pack.contains(m)) {
                if (party.members().contains(m)) {
                    removeFrom.add(m);
                }
                keep.remove(m);
            }
        }
        for (UUID m : pack) {
            if (!party.members().contains(m) && !leavePack.contains(m) && !ours.contains(m)) {
                add.put(m, alphas.contains(m));
                keep.add(m);
            }
        }
        return new Plan(false, false, add, removeFrom, leavePack, keep);
    }
}
