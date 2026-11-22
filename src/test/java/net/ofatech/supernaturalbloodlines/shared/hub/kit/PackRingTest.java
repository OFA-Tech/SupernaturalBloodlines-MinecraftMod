package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class PackRingTest {
    private static final List<PackRing.Member> PACK = List.of(
            new PackRing.Member("slime", PackRing.Role.SUBORDINATE), new PackRing.Member("stiles", PackRing.Role.PACKMATE),
            new PackRing.Member("derek", PackRing.Role.ALPHA), new PackRing.Member("liam", PackRing.Role.BETA),
            new PackRing.Member("ashen", PackRing.Role.BROTHER));

    @Test
    void theRolesKeepTheirOrderAndTheFirstStandsAtTheTop() {
        List<PackRing.Slot> ring = PackRing.ring(PACK, 100, 100, 60, 20);
        assertEquals(List.of("derek", "liam", "stiles", "ashen", "slime"), ring.stream().map(s -> s.member().id()).toList());
        assertEquals(100, ring.get(0).x());
        assertEquals(40, ring.get(0).y());
    }

    @Test
    void subordinatesAreSmallAndFaintAtHalf() {
        for (PackRing.Slot s : PackRing.ring(PACK, 100, 100, 60, 20)) {
            if (s.member().role() == PackRing.Role.SUBORDINATE) {
                assertEquals(10, s.size());
                assertEquals(0.5f, s.alpha());
            } else {
                assertEquals(20, s.size());
                assertEquals(1f, s.alpha());
            }
        }
    }

    @Test
    void theRoleColoursAreTheDesignsAndDistinct() {
        assertEquals(0xFFE0484A, PackRing.Role.ALPHA.color());
        assertEquals(0xFFE0A83A, PackRing.Role.BETA.color());
        assertEquals(0xFF4A8AD8, PackRing.Role.PACKMATE.color());
        assertEquals(0xFF8A6A4A, PackRing.Role.BROTHER.color());
        assertEquals(PackRing.Role.values().length, java.util.Arrays.stream(PackRing.Role.values()).map(PackRing.Role::color).distinct().count());
        assertTrue(PackRing.ring(List.of(), 0, 0, 10, 10).isEmpty());
    }

    @Test
    void fiftyMembersShrinkTheFacesAndTheRestIsCountedNotDrawnOnTopOfEachOther() {
        List<PackRing.Member> many = new java.util.ArrayList<>();
        for (int i = 0; i < 50; i++) {
            many.add(new PackRing.Member("m" + i, i < 2 ? PackRing.Role.ALPHA : i < 10 ? PackRing.Role.BETA : PackRing.Role.SUBORDINATE));
        }
        PackRing.Layout layout = PackRing.layout(many, 100, 100, 60, 16);
        assertTrue(layout.size() >= PackRing.MIN_SIZE && layout.size() < 16, "the faces shrink: " + layout.size());
        assertEquals(50, layout.slots().size() + layout.hidden(), "every member is drawn or counted");
        assertTrue(layout.hidden() > 0, "50 do not fit round a 60 pixel ring");
        for (int i = 0; i < layout.slots().size(); i++) {
            for (int j = i + 1; j < layout.slots().size(); j++) {
                PackRing.Slot a = layout.slots().get(i);
                PackRing.Slot b = layout.slots().get(j);
                double d = Math.hypot(a.x() - b.x(), a.y() - b.y());
                assertTrue(d >= (a.size() + b.size()) / 2.0 - 1, "faces " + i + " and " + j + " overlap");
            }
        }
        assertTrue(layout.slots().stream().anyMatch(s -> s.member().role() == PackRing.Role.ALPHA), "the lowest roles are the ones left out");
    }

    @Test
    void theRingsRoundTwoNeighboursNeverTouchWhateverTheCountOrTheRadius() {
        for (int radius : new int[] {26, 33, 40, 55, 70}) {
            for (int face : new int[] {12, 16}) {
                for (int count = 1; count <= 24; count++) {
                    List<PackRing.Member> members = new java.util.ArrayList<>();
                    for (int i = 0; i < count; i++) {
                        members.add(new PackRing.Member("m" + i, i % 3 == 0 ? PackRing.Role.ALPHA : i % 3 == 1 ? PackRing.Role.BETA : PackRing.Role.PACKMATE));
                    }
                    List<PackRing.Slot> ring = PackRing.layout(members, 0, 0, radius, face).slots();
                    for (int i = 0; i < ring.size(); i++) {
                        for (int j = i + 1; j < ring.size(); j++) {
                            PackRing.Slot a = ring.get(i);
                            PackRing.Slot b = ring.get(j);
                            // the portrait is drawn with a ring 2 px outside its face: the two rings are at least 2 px apart
                            double clear = Math.hypot(a.x() - b.x(), a.y() - b.y()) - (a.size() + b.size()) / 2.0 - 4;
                            assertTrue(clear >= 1.5, "radius " + radius + " face " + face + " count " + count + ": heads " + i + " and " + j + " are " + clear + " px apart");
                        }
                    }
                }
            }
        }
    }

    @Test
    void thePlusMoreLineIsUnderTheLowestFaceWhateverTheCountOrTheRadius() {
        for (int radius : new int[] {26, 33, 40, 60}) {
            for (int count = 5; count <= 30; count++) {
                List<PackRing.Member> members = new java.util.ArrayList<>();
                for (int i = 0; i < count; i++) {
                    members.add(new PackRing.Member("m" + i, i < 2 ? PackRing.Role.ALPHA : PackRing.Role.BETA));
                }
                PackRing.Layout layout = PackRing.layout(members, 0, 0, radius, 12);
                int y = PackRing.moreLabelY(layout, 0);
                for (PackRing.Slot s : layout.slots()) {
                    assertTrue(y >= s.y() + s.size() / 2 + 2, "radius " + radius + " count " + count + ": the line at " + y + " is on a face at " + s.y());
                }
            }
        }
    }

    @Test
    void aLineIsClearOfTheFacesOnlyWhenNoFaceOrRingTouchesIt() {
        List<PackRing.Member> members = List.of(new PackRing.Member("a", PackRing.Role.ALPHA), new PackRing.Member("b", PackRing.Role.BETA), new PackRing.Member("c", PackRing.Role.BETA),
                new PackRing.Member("d", PackRing.Role.PACKMATE));
        PackRing.Layout layout = PackRing.layout(members, 100, 100, 60, 16);
        // a line through the middle of the ring touches no face; one across the bottom face does
        assertTrue(PackRing.clearOfFaces(layout, 90, 100, 20, 9, 2));
        PackRing.Slot bottom = layout.slots().stream().max(java.util.Comparator.comparingInt(PackRing.Slot::y)).orElseThrow();
        assertTrue(!PackRing.clearOfFaces(layout, bottom.x() - 20, bottom.y() - 4, 40, 9, 2), "a line across a face");
        assertTrue(!PackRing.clearOfFaces(layout, bottom.x() + bottom.size() / 2 + 3, bottom.y(), 20, 9, 2), "a line within the ring and the air of a face");
        assertTrue(PackRing.clearOfFaces(layout, 400, 400, 20, 9, 2), "far from every face");
    }

    @Test
    void aSmallPackKeepsTheFullSizeAndHidesNobody() {
        PackRing.Layout layout = PackRing.layout(PACK, 100, 100, 60, 20);
        assertEquals(20, layout.size());
        assertEquals(0, layout.hidden());
    }

    @Test
    void aNarrowAreaGrowsTheRingToItsWidthSoTheNamesOnTopAndBottomHaveRoom() {
        // a middle column of Tensura's hub at scale 4: 137 wide, 130 tall, faces of 12
        int radius = PackRing.radiusFor(137, 130, 12);
        assertTrue(radius >= PackRing.NAMED_RADIUS, "the ring named nobody at " + radius);
        assertTrue(radius + 12 / 2 + 2 <= 137 / 2, "the faces stay inside the area: " + radius);
    }

    @Test
    void aWideAreaKeepsRoomBesideTheSidePortraitsForTheirNames() {
        assertEquals(47, PackRing.radiusFor(400, 130, 12));
        assertTrue(PackRing.radiusFor(200, 400, 16) <= 200 / 2 - PackRing.NAME_RESERVE);
    }}
