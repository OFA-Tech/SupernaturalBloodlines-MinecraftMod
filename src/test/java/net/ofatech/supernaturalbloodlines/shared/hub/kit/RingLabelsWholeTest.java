package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.ofatech.supernaturalbloodlines.SourceTree;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import org.junit.jupiter.api.Test;

/** The Tensura pack ring's "Rim…" (plan 23): a name cut beside its face is drawn whole under or over it when there is room there. */
class RingLabelsWholeTest {
    private static final Rect PAGE = new Rect(0, 0, 200, 200);

    @Test
    void aCutNameMovesWhereItFitsWhole() {
        // a face at (100, 100), 16 px; a name of 50 px asked to its right, where a neighbour leaves only 20 px
        RingLabels.Request asked = new RingLabels.Request(111, 97, 50, 9, RingLabels.Anchor.LEFT);
        Rect cut = new Rect(111, 97, 20, 9);
        Rect neighbour = new Rect(133, 90, 20, 20);
        Rect face = PortraitGeometry.frame(100, 100, 16);
        Rect got = RingLabels.preferWhole(asked, cut, 100, 100, 16, List.of(face, neighbour), PAGE, 4);
        assertEquals(50, got.w(), "the name is still cut: " + got);
        assertTrue(!got.overlaps(face) && !got.overlaps(neighbour), "the whole name covers a face: " + got);
    }

    @Test
    void aWholeNameStaysWhereItIs() {
        RingLabels.Request asked = new RingLabels.Request(111, 97, 50, 9, RingLabels.Anchor.LEFT);
        Rect whole = new Rect(111, 97, 50, 9);
        assertSame(whole, RingLabels.preferWhole(asked, whole, 100, 100, 16, List.of(), PAGE, 4));
    }

    @Test
    void aCutNameWithNoWholePlaceStaysCut() {
        RingLabels.Request asked = new RingLabels.Request(111, 97, 50, 9, RingLabels.Anchor.LEFT);
        Rect cut = new Rect(111, 97, 20, 9);
        Rect above = new Rect(60, 70, 80, 20);
        Rect below = new Rect(60, 110, 80, 20);
        assertSame(cut, RingLabels.preferWhole(asked, cut, 100, 100, 16, List.of(PortraitGeometry.frame(100, 100, 16), above, below), PAGE, 4));
    }

    @Test
    void thePackPageUsesIt() {
        String src = SourceTree.read("src/main/java/net/ofatech/supernaturalbloodlines/parts/supernatural/client/hub/page/BookPackPage.java");
        assertTrue(src.contains("RingLabels.preferWhole("), "the ring keeps a cut name where it fits whole");
    }
}
