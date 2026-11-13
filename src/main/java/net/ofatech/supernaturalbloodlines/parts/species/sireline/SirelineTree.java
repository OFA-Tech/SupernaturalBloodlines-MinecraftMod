package net.ofatech.supernaturalbloodlines.parts.species.sireline;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Everyone a sire sired, depth-first. A visited set ends a cycle (a sire turned by their own descendant). */
public final class SirelineTree {
    public static final int MAX_DEPTH = 10;

    public record Line(int depth, UUID id, String name, String entityType) {
    }

    private SirelineTree() {
    }

    /** The root's descendants (the root itself is not listed); the root's children are depth 1. */
    public static List<Line> below(SirelineIndex index, UUID root) {
        List<Line> out = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        seen.add(root);
        walk(index, root, 1, seen, out);
        return out;
    }

    private static void walk(SirelineIndex index, UUID sire, int depth, Set<UUID> seen, List<Line> out) {
        if (depth > MAX_DEPTH) {
            return;
        }
        for (SirelineIndex.Node child : index.childrenOf(sire)) {
            if (seen.add(child.id())) {
                out.add(new Line(depth, child.id(), child.name(), child.entityType()));
                walk(index, child.id(), depth + 1, seen, out);
            }
        }
    }
}
