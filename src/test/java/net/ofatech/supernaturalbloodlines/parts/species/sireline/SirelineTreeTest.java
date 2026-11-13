package net.ofatech.supernaturalbloodlines.parts.species.sireline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SirelineTreeTest {
    private static final UUID ROOT = UUID.randomUUID();
    private static final UUID A = UUID.randomUUID();
    private static final UUID B = UUID.randomUUID();
    private static final UUID C = UUID.randomUUID();

    @Test
    void listsDescendantsDepthFirst() {
        SirelineIndex idx = new SirelineIndex();
        idx.add(ROOT, A, "minecraft:player", "Ann");
        idx.add(ROOT, B, "minecraft:cow", "Cow");
        idx.add(A, C, "minecraft:player", "Cy");
        assertEquals(List.of(
                new SirelineTree.Line(1, A, "Ann", "minecraft:player"),
                new SirelineTree.Line(2, C, "Cy", "minecraft:player"),
                new SirelineTree.Line(1, B, "Cow", "minecraft:cow")), SirelineTree.below(idx, ROOT));
    }

    @Test
    void nobodySiredIsEmpty() {
        assertTrue(SirelineTree.below(new SirelineIndex(), ROOT).isEmpty());
    }

    @Test
    void aCycleEndsTheTree() {
        SirelineIndex idx = new SirelineIndex();
        idx.add(ROOT, A, "minecraft:player", "Ann");
        idx.add(A, B, "minecraft:player", "Bob");
        idx.add(B, ROOT, "minecraft:player", "Root"); // Root was turned by their own descendant
        List<SirelineTree.Line> lines = SirelineTree.below(idx, ROOT);
        assertEquals(List.of(A, B), lines.stream().map(SirelineTree.Line::id).toList());
    }

    @Test
    void depthIsCapped() {
        SirelineIndex idx = new SirelineIndex();
        UUID parent = ROOT;
        Set<UUID> all = new HashSet<>();
        for (int i = 0; i < 30; i++) {
            UUID child = UUID.randomUUID();
            idx.add(parent, child, "minecraft:player", "n" + i);
            all.add(child);
            parent = child;
        }
        List<SirelineTree.Line> lines = SirelineTree.below(idx, ROOT);
        assertEquals(SirelineTree.MAX_DEPTH, lines.size());
        assertEquals(SirelineTree.MAX_DEPTH, lines.get(lines.size() - 1).depth());
    }
}
