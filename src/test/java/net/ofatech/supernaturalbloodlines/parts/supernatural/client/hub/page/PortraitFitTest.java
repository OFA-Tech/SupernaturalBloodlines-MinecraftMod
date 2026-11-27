package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import net.ofatech.supernaturalbloodlines.SourceTree;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.theme.BloodMoonTheme;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.theme.GrimoireTheme;
import net.ofatech.supernaturalbloodlines.parts.supernaturaltensura.client.TensuraHubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Contrast;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.PackRing;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.PortraitGeometry;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Shapes;
import org.junit.jupiter.api.Test;

/**
 * Every player face (Home, Profile and the pack, clan, coven and brotherhood rings) goes through {@code BookKit.portrait}, which draws the square
 * frame of {@link PortraitGeometry}. The three themes draw the portrait with the same function and the same geometry (only the colours differ),
 * so the geometry checks below hold for all three.
 */
class PortraitFitTest {
    private static final String PAGES = "src/main/java/net/ofatech/supernaturalbloodlines/parts/supernatural/client/hub/page/";

    private static String portraitBody() {
        String kit = SourceTree.read(PAGES + "BookKit.java");
        int start = kit.indexOf("public static void portrait(");
        assertTrue(start >= 0, "BookKit.portrait is gone");
        return kit.substring(start, kit.indexOf("\n    }", start));
    }

    private static List<PackRing.Layout> ringLayouts() {
        List<PackRing.Layout> out = new ArrayList<>();
        PackRing.Role[] roles = PackRing.Role.values();
        for (int face : new int[] {12, 16}) {
            for (int radius : new int[] {PackRing.MIN_RADIUS, 33, PackRing.NAMED_RADIUS, 55, 70}) {
                for (int count = 1; count <= 40; count++) {
                    List<PackRing.Member> members = new ArrayList<>();
                    for (int i = 0; i < count; i++) {
                        members.add(new PackRing.Member("m" + i, roles[i % roles.length]));
                    }
                    out.add(PackRing.layout(members, 200, 150, radius, face));
                }
            }
        }
        return out;
    }

    @Test
    void thePackRingUsesTheSameFrame() {
        for (String page : List.of("BookPackPage.java", "HomePage.java", "ProfilePage.java")) {
            String src = SourceTree.read(PAGES + page);
            assertTrue(src.contains("BookKit.portrait("), page + " draws a face without BookKit.portrait");
            assertTrue(!src.contains("Shapes.Shape.ROUND"), page + " draws a round shape itself");
            assertTrue(!src.contains("PlayerFaceRenderer"), page + " draws a face itself");
        }
        assertTrue(!SourceTree.read(PAGES + "BookPackPage.java").contains("size + 4"), "BookPackPage works out a ring frame itself");
        String ring = SourceTree.read("src/main/java/net/ofatech/supernaturalbloodlines/shared/hub/kit/PackRing.java");
        assertTrue(!ring.contains("size() / 2 + 2") && ring.contains("PortraitGeometry.frame("), "PackRing works out a frame itself");
        String body = portraitBody();
        assertTrue(body.contains("PortraitGeometry.of(") && body.contains("Shapes.Shape.SOFT"), "BookKit.portrait does not draw the square frame:\n" + body);
        assertTrue(!body.contains("ROUND"), "BookKit.portrait still draws a round frame:\n" + body);
    }

    /** The face itself is drawn in the geometry's face box, not a box worked out again in the drawing code. */
    @Test
    void thePortraitDrawsTheFaceInTheGeometrysFaceBox() {
        String body = portraitBody();
        assertTrue(body.contains("face(g, name, b.face().x(), b.face().y(), b.face().w());"), "BookKit.portrait does not draw the face at b.face():\n" + body);
        assertTrue(body.contains("b.frame().x(), b.frame().y(), b.frame().w(), border") && body.contains("b.paper().x(), b.paper().y(), b.paper().w(), paper"),
                "BookKit.portrait does not draw the frame and paper at their boxes:\n" + body);
    }

    @Test
    void theFrameColoursStandOutFromThePaperInEveryTheme() {
        for (HubTheme theme : List.of(new GrimoireTheme(), new BloodMoonTheme(), new TensuraHubTheme())) {
            for (int frame : new int[] {theme.accent(), theme.muted()}) {
                double ratio = Contrast.ratio(frame, theme.paper());
                assertTrue(ratio >= 3, theme.getClass().getSimpleName() + ": a portrait frame of " + Integer.toHexString(frame) + " is " + ratio + ":1 on the paper");
            }
        }
    }

    @Test
    void ringNamesKeepFourPixelsOfAir() {
        assertTrue(BookPackPage.RING_NAME_PAD >= 4, "ring names keep " + BookPackPage.RING_NAME_PAD + " px");
    }

    @Test
    void theRingFacesFitTheirFrameAtEverySizeTheLayoutPicks() {
        int checked = 0;
        for (PackRing.Layout layout : ringLayouts()) {
            for (PackRing.Slot slot : layout.slots()) {
                PortraitGeometry.Boxes b = PortraitGeometry.of(slot.x(), slot.y(), slot.size());
                Rect face = b.face();
                for (int[] c : new int[][] {{face.x(), face.y()}, {face.right() - 1, face.y()}, {face.x(), face.bottom() - 1}, {face.right() - 1, face.bottom() - 1}}) {
                    assertTrue(Shapes.contains(Shapes.Shape.SOFT, b.frame().w(), c[0] - b.frame().x(), c[1] - b.frame().y()),
                            "a corner of a ring face of " + slot.size() + " leaves its frame");
                }
                checked++;
            }
        }
        assertTrue(checked > 1000, "too few ring faces checked: " + checked);
    }

    @Test
    void theRingFootprintIsUnchanged() {
        for (PackRing.Layout layout : ringLayouts()) {
            List<Rect> frames = new ArrayList<>();
            for (PackRing.Slot slot : layout.slots()) {
                int size = slot.size();
                Rect frame = PortraitGeometry.of(slot.x(), slot.y(), size).frame();
                assertEquals(new Rect(slot.x() - size / 2 - 2, slot.y() - size / 2 - 2, size + 4, size + 4), frame, "the footprint of a " + size + " ring face moved");
                frames.add(frame);
            }
            for (int i = 0; i < frames.size(); i++) {
                for (int j = i + 1; j < frames.size(); j++) {
                    java.util.Set<Long> shared = pixels(Shapes.Shape.SOFT, frames.get(i));
                    shared.retainAll(pixels(Shapes.Shape.SOFT, frames.get(j)));
                    assertTrue(shared.isEmpty() && !frames.get(i).overlaps(frames.get(j)), "ring frames " + i + " and " + j + " overlap: " + frames.get(i) + " " + frames.get(j)
                            + " (" + layout.slots().size() + " faces of " + layout.size() + ")");
                    Rect grown = new Rect(frames.get(i).x() - 1, frames.get(i).y() - 1, frames.get(i).w() + 2, frames.get(i).h() + 2);
                    assertTrue(!grown.overlaps(frames.get(j)), "no pixel of air between ring frames " + frames.get(i) + " and " + frames.get(j));
                }
            }
        }
    }

    private static java.util.Set<Long> pixels(Shapes.Shape shape, Rect box) {
        java.util.Set<Long> out = new java.util.HashSet<>();
        for (int y = 0; y < box.h(); y++) {
            for (int x = 0; x < box.w(); x++) {
                if (Shapes.contains(shape, box.w(), x, y)) {
                    out.add(((long) (box.x() + x) << 32) | ((box.y() + y) & 0xFFFFFFFFL));
                }
            }
        }
        return out;
    }

    @Test
    void clearOfFacesSeesTheWholeFrameAtOddSizes() {
        for (int size = PackRing.MIN_SIZE + 1; size <= 47; size += 2) {
            PackRing.Layout layout = PackRing.layout(List.of(new PackRing.Member("a", PackRing.Role.ALPHA)), 200, 150, 60, size);
            PackRing.Slot s = layout.slots().get(0);
            Rect frame = PortraitGeometry.frame(s.x(), s.y(), s.size());
            assertTrue(!PackRing.clearOfFaces(layout, frame.right() - 1, frame.bottom() - 1, 1, 1, 0), "the last pixel of a " + s.size() + " frame counts as clear");
            assertTrue(!PackRing.clearOfFaces(layout, frame.x(), frame.y(), 1, 1, 0), "the first pixel of a " + s.size() + " frame counts as clear");
            assertTrue(PackRing.clearOfFaces(layout, frame.right(), frame.y(), 1, 1, 0), "the pixel right of a " + s.size() + " frame is taken");
            assertTrue(PackRing.clearOfFaces(layout, frame.x(), frame.bottom(), 1, 1, 0), "the pixel under a " + s.size() + " frame is taken");
            assertTrue(!PackRing.clearOfFaces(layout, frame.right(), frame.y(), 1, 1, 1), "the pad is not kept right of a " + s.size() + " frame");
            assertEquals(frame.bottom() + 3, PackRing.moreLabelY(layout, 0), "the more line under a " + s.size() + " frame");
        }
    }

    @Test
    void theFrameTopStaysInItsRowAndWhatFollowsHasAGap() {
        for (String page : List.of("HomePage.java", "ProfilePage.java")) {
            String src = SourceTree.read(PAGES + page);
            assertTrue(src.contains("PortraitGeometry.cyAt(y, face)") && src.contains("PortraitGeometry.below(y, face)"), page + " places its portrait vertically itself");
        }
        for (int top : new int[] {0, 9, 57}) {
            for (int face : new int[] {27, 28, 40, 41, 48}) {
                Rect frame = PortraitGeometry.frame(PortraitGeometry.cxAt(10, face), PortraitGeometry.cyAt(top, face), face);
                assertEquals(top, frame.y(), "a " + face + " frame starts above its row at " + top);
                assertEquals(2, PortraitGeometry.below(top, face) - frame.bottom(), "the gap under a " + face + " frame");
            }
        }
    }

    @Test
    void theFrameStaysInsideItsColumn() {
        for (String page : List.of("HomePage.java", "ProfilePage.java")) {
            String src = SourceTree.read(PAGES + page);
            assertTrue(src.contains("PortraitGeometry.cxAt(r.x(), face)"), page + " does not place its portrait with PortraitGeometry.cxAt");
        }
        // the column's left edge in GUI pixels at gui scales 2 and 4 of a 1920x1080 window, for the three themes' first columns
        for (int left : new int[] {12, 19, 24, 31, 38, 45, 60, 96}) {
            for (int face : new int[] {27, 28, 40, 41, 48}) {
                Rect frame = PortraitGeometry.of(PortraitGeometry.cxAt(left, face), 40, face).frame();
                assertEquals(left, frame.x(), "a " + face + " frame starts left of its column at " + left);
            }
        }
    }

    @Test
    void theTextBesideItNeverTouchesTheFrame() {
        for (String page : List.of("HomePage.java", "ProfilePage.java")) {
            assertTrue(SourceTree.read(PAGES + page).contains("PortraitGeometry.besideX(r.x(), face)"), page + " places the text beside its portrait itself");
        }
        for (int face : new int[] {28, 40, 48}) {
            Rect frame = PortraitGeometry.of(PortraitGeometry.cxAt(10, face), 40, face).frame();
            assertEquals(4, PortraitGeometry.besideX(10, face) - frame.right(), "the text beside a " + face + " face");
        }
    }
}
