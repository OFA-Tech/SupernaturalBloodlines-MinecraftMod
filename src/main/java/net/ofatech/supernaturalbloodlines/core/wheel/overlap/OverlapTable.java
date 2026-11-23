package net.ofatech.supernaturalbloodlines.core.wheel.overlap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerRef;

/**
 * The approved list of powers that several mods give, and the tiers of one skill ({@code chains}, lowest first). Pure: the file is read by the part. A change to one pair is a data
 * edit, not code. Every refusal of {@link #parse} names the group it was in.
 */
public final class OverlapTable {
    public static final OverlapTable EMPTY = new OverlapTable(List.of(), List.of());

    private final List<OverlapGroup> groups;
    private final List<List<PowerRef>> chains;
    private final Map<PowerRef, PowerRef> canonical = new HashMap<>();

    private OverlapTable(List<OverlapGroup> groups, List<List<PowerRef>> chains) {
        this.groups = List.copyOf(groups);
        this.chains = chains.stream().map(List::copyOf).toList();
        for (List<PowerRef> chain : this.chains) {
            for (PowerRef r : chain) {
                canonical.put(r, chain.get(0));
            }
        }
    }

    public static OverlapTable of(List<OverlapGroup> groups, List<List<PowerRef>> chains) {
        return new OverlapTable(groups, chains);
    }

    public List<OverlapGroup> groups() {
        return groups;
    }

    public List<List<PowerRef>> chains() {
        return chains;
    }

    /** A tier of a chain counts as the first id of its chain wherever groups are matched; any other power is itself. */
    public PowerRef canonical(PowerRef ref) {
        return canonical.getOrDefault(ref, ref);
    }

    /** The groups a power is a member of (a tier counts as its chain), in file order. */
    public List<OverlapGroup> groupsOf(PowerRef ref) {
        PowerRef c = canonical(ref);
        List<OverlapGroup> out = new ArrayList<>();
        for (OverlapGroup g : groups) {
            if (sideOf(g, c) >= 0) {
                out.add(g);
            }
        }
        return out;
    }

    /** The side of the group this power is (a tier counts as its chain), -1 when it is not a member. */
    public int sideOf(OverlapGroup g, PowerRef ref) {
        PowerRef c = canonical(ref);
        for (int i = 0; i < g.members().size(); i++) {
            if (canonical(g.members().get(i)).equals(c)) {
                return i;
            }
        }
        return -1;
    }

    public static OverlapTable parse(JsonObject root) {
        if (!root.has("format") || root.get("format").getAsInt() != 1) {
            throw new IllegalArgumentException("overlap table: format must be 1");
        }
        List<List<PowerRef>> chains = new ArrayList<>();
        Set<PowerRef> inChain = new HashSet<>();
        if (root.has("chains")) {
            for (JsonElement c : root.getAsJsonArray("chains")) {
                List<PowerRef> chain = new ArrayList<>();
                for (JsonElement m : c.getAsJsonArray()) {
                    PowerRef r = ref(m.getAsString(), "chain");
                    if (!inChain.add(r)) {
                        throw new IllegalArgumentException("overlap table: " + r.key() + " is in two chains");
                    }
                    chain.add(r);
                }
                if (chain.size() < 2) {
                    throw new IllegalArgumentException("overlap table: a chain needs at least two tiers");
                }
                chains.add(chain);
            }
        }
        OverlapTable partial = new OverlapTable(List.of(), chains);
        List<OverlapGroup> groups = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        JsonArray arr = root.has("groups") ? root.getAsJsonArray("groups") : new JsonArray();
        for (JsonElement e : arr) {
            JsonObject g = e.getAsJsonObject();
            String id = g.has("id") ? g.get("id").getAsString() : "?";
            if (!ids.add(id)) {
                throw new IllegalArgumentException("overlap group " + id + ": the id is used twice");
            }
            String confidence = g.has("confidence") ? g.get("confidence").getAsString() : "";
            if (!confidence.equals("same") && !confidence.equals("close")) {
                throw new IllegalArgumentException("overlap group " + id + ": confidence must be same or close, not '" + confidence + "' (loose matches stay separate)");
            }
            List<PowerRef> members = new ArrayList<>();
            Set<PowerRef> seen = new HashSet<>();
            if (!g.has("members")) {
                throw new IllegalArgumentException("overlap group " + id + ": no members");
            }
            for (JsonElement m : g.getAsJsonArray("members")) {
                PowerRef r;
                try {
                    r = ref(m.getAsString(), id);
                } catch (IllegalArgumentException ex) {
                    throw new IllegalArgumentException("overlap group " + id + ": " + ex.getMessage(), ex);
                }
                if (!seen.add(partial.canonical(r))) {
                    throw new IllegalArgumentException("overlap group " + id + ": " + r.key() + " is in the group twice");
                }
                members.add(r);
            }
            if (members.size() < 2) {
                throw new IllegalArgumentException("overlap group " + id + ": needs at least two members");
            }
            int main = g.has("main") ? g.get("main").getAsInt() : -1;
            if (main < 0 || main >= members.size()) {
                throw new IllegalArgumentException("overlap group " + id + ": main " + main + " is out of range");
            }
            String research = g.has("research") ? g.get("research").getAsString() : "";
            groups.add(new OverlapGroup(id, research, confidence.equals("same"), main, members));
        }
        return new OverlapTable(groups, chains);
    }

    private static PowerRef ref(String key, String where) {
        return PowerRef.parse(key).orElseThrow(() -> new IllegalArgumentException("not a power: '" + key + "' (" + where + ")"));
    }
}
