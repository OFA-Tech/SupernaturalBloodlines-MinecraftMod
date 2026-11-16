package net.ofatech.supernaturalbloodlines.core.bloodline;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * What a pack asks of the Open Parties and Claims party it mirrors (plan 14), as a pure plan. Only a party the pack made
 * itself is ever touched (the glue records it when it creates one), and only the people the pack put in it: a manual
 * party, and a player already in another party, are left alone. Everyone is a plain member, so only the owner, who is the
 * leader, can remove people from it; whoever is no longer in it left it themselves (or the leader removed them), and
 * leaves the pack.
 */
public final class PartySyncRules {
    private PartySyncRules() {
    }

    /** The party as it is now: {@code members} holds every member, the owner too. */
    public record Party(UUID owner, Set<UUID> members) {
    }

    /**
     * @param forget     the recorded party is gone (its owner ended it): forget it and wait for the roster to change
     * @param abandon    the party's owner is not the leader any more: our people leave it, it is forgotten and left alone otherwise
     * @param create     the leader gets a new party
     * @param add        who joins the party (each may still be refused: another party, a limit)
     * @param removeFrom who leaves the party because they left the pack
     * @param leavePack  who leaves the pack because they left the party
     */
    public record Plan(boolean forget, boolean abandon, boolean create, Set<UUID> add, Set<UUID> removeFrom, Set<UUID> leavePack) {
        static final Plan NOTHING = new Plan(false, false, false, Set.of(), Set.of(), Set.of());
    }

    /**
     * @param leader        the pack's leader
     * @param pack          everyone in the pack, the leader too
     * @param party         the party the pack recorded, as it is now, or null when it is not there
     * @param recorded      the pack has a recorded party (so a null {@code party} means it was ended)
     * @param ours         whom the pack put in the recorded party
     * @param ended         the recorded party was ended and the roster has not changed since
     * @param rosterChanged somebody joined, left or was kicked just now
     */
    public static Plan plan(UUID leader, Set<UUID> pack, @Nullable Party party, boolean recorded, Set<UUID> ours, boolean ended, boolean rosterChanged) {
        if (recorded && party == null) {
            return new Plan(true, false, false, Set.of(), Set.of(), Set.of());
        }
        if (party == null) {
            if (ended && !rosterChanged || pack.size() < 2) {
                return Plan.NOTHING;
            }
            Set<UUID> add = new HashSet<>(pack);
            add.remove(leader);
            return new Plan(false, false, true, add, Set.of(), Set.of());
        }
        if (!party.owner().equals(leader)) {
            Set<UUID> out = new HashSet<>(ours);
            out.retainAll(party.members());
            out.remove(party.owner());
            Set<UUID> add = new HashSet<>(pack);
            add.remove(leader);
            return new Plan(false, true, pack.size() > 1, pack.size() > 1 ? add : Set.of(), out, Set.of());
        }
        Set<UUID> leavePack = new HashSet<>(ours);
        leavePack.retainAll(pack);
        leavePack.removeAll(party.members());
        leavePack.remove(party.owner());
        Set<UUID> removeFrom = new HashSet<>(ours);
        removeFrom.removeAll(pack);
        removeFrom.retainAll(party.members());
        removeFrom.remove(party.owner());
        Set<UUID> add = new HashSet<>(pack);
        add.removeAll(party.members());
        add.removeAll(ours);
        add.remove(party.owner());
        return new Plan(false, false, false, add, removeFrom, leavePack);
    }
}