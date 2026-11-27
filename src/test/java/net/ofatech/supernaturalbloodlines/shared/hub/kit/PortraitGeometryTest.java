package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import org.junit.jupiter.api.Test;

/** The square frame round a player's face (plan 23 Task 1, owner decision F): face {@code size}, paper {@code size + 2}, frame {@code size + 4}, all centred. */
class PortraitGeometryTest {
    private static List<int[]> corners(Rect face) {
        return List.of(new int[] {face.x(), face.y()}, new int[] {face.right() - 1, face.y()},
                new int[] {face.x(), face.bottom() - 1}, new int[] {face.right() - 1, face.bottom() - 1});
    }

    private static boolean cornersInside(Shapes.Shape shape, Rect frame, Rect face) {
        for (int[] c : corners(face)) {
            if (!Shapes.contains(shape, frame.w(), c[0] - frame.x(), c[1] - frame.y())) {
                return false;
            }
        }
        return true;
    }

    @Test
    void theFrameIsTheFacePlusFour() {
        for (int size = 6; size <= 64; size++) {
            PortraitGeometry.Boxes b = PortraitGeometry.of(100, 80, size);
            assertEquals(size, b.face().w(), "face " + size);
            assertEquals(size, b.face().h(), "face " + size);
            assertEquals(size + 4, b.frame().w(), "frame " + size);
            assertEquals(size + 4, b.frame().h(), "frame " + size);
            assertEquals(b.face().x() - 2, b.frame().x(), "frame left " + size);
            assertEquals(b.face().y() - 2, b.frame().y(), "frame top " + size);
        }
    }

    @Test
    void thePaperIsOnePixelUnderTheFrame() {
        for (int size = 6; size <= 64; size++) {
            PortraitGeometry.Boxes b = PortraitGeometry.of(37, 41, size);
            assertEquals(new Rect(b.frame().x() + 1, b.frame().y() + 1, size + 2, size + 2), b.paper(), "paper " + size);
        }
    }

    @Test
    void theFaceIsCentred() {
        for (int size = 6; size <= 64; size++) {
            PortraitGeometry.Boxes b = PortraitGeometry.of(50, 60, size);
            assertEquals(50 - size / 2, b.face().x(), "face left " + size);
            assertEquals(60 - size / 2, b.face().y(), "face top " + size);
            assertEquals(b.face().x() - b.frame().x(), b.frame().right() - b.face().right(), "same margin left and right at " + size);
        }
    }

    @Test
    void everyFaceCornerIsInsideTheFrame() {
        for (int size = 6; size <= 64; size++) {
            PortraitGeometry.Boxes b = PortraitGeometry.of(64, 64, size);
            assertTrue(cornersInside(Shapes.Shape.SOFT, b.frame(), b.face()), "a corner of a " + size + " face leaves its frame");
            assertTrue(cornersInside(Shapes.Shape.SOFT, b.paper(), b.face()), "a corner of a " + size + " face leaves its paper");
        }
    }

    private static boolean oldRoundHolds(int size) {
        int cx = 64;
        int cy = 64;
        Rect oldFrame = new Rect(cx - size / 2 - 2, cy - size / 2 - 2, size + 4, size + 4);
        int oldFace = Math.max(6, size - 2);
        return cornersInside(Shapes.Shape.ROUND, oldFrame, new Rect(cx - size / 2 + 1, cy - size / 2 + 1, oldFace, oldFace));
    }

    /**
     * The bug the owner saw, kept as a test of the old shape so nobody brings it back: the round frame with the old {@code size - 2} face loses its
     * corners at Home's and Profile's sizes. The ring's faces (8 to 16) happened to fit the round frame; they get the square one for one look everywhere.
     */
    @Test
    void theRoundFrameLosesTheCornersAtTheSizesInUse() {
        for (int size : List.of(28, 40, 48)) {
            assertTrue(!oldRoundHolds(size), "the old round frame held a " + size + " face");
        }
        for (int size = PackRing.MIN_SIZE; size <= 16; size++) {
            assertTrue(oldRoundHolds(size), "the old round frame lost a ring face of " + size);
        }
    }
    @Test
    void softKeepsTheEdgeMidpoints() {
        for (int size = 1; size <= 64; size++) {
            int mid = size / 2;
            assertTrue(Shapes.contains(Shapes.Shape.SOFT, size, mid, 0), "top middle at " + size);
            assertTrue(Shapes.contains(Shapes.Shape.SOFT, size, mid, size - 1), "bottom middle at " + size);
            assertTrue(Shapes.contains(Shapes.Shape.SOFT, size, 0, mid), "left middle at " + size);
            assertTrue(Shapes.contains(Shapes.Shape.SOFT, size, size - 1, mid), "right middle at " + size);
        }
    }

    @Test
    void softCutsExactlyFourPixels() {
        for (int size = 8; size <= 64; size++) {
            int missing = 0;
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    if (!Shapes.contains(Shapes.Shape.SOFT, size, x, y)) {
                        missing++;
                        assertTrue((x == 0 || x == size - 1) && (y == 0 || y == size - 1), "pixel " + x + "," + y + " of " + size + " is not a corner");
                    }
                }
            }
            assertEquals(4, missing, "cut pixels at " + size);
        }
    }

    @Test
    void softBelowEightIsTheSquare() {
        for (int size = 1; size < 8; size++) {
            assertEquals(List.of(Shapes.spans(Shapes.Shape.SQUARE, size)), List.of(Shapes.spans(Shapes.Shape.SOFT, size)), "size " + size);
        }
    }

    @Test
    void besideTheFaceIsFourPixelsClearOfTheFrame() {
        for (int size : new int[] {28, 40, 48}) {
            for (int left : new int[] {0, 7, 133}) {
                Rect frame = PortraitGeometry.of(PortraitGeometry.cxAt(left, size), 50, size).frame();
                assertEquals(left, frame.x(), "the frame starts at its column at " + size);
                assertEquals(4, PortraitGeometry.besideX(left, size) - frame.right(), "gap to the text at " + size);
            }
        }
    }
}
