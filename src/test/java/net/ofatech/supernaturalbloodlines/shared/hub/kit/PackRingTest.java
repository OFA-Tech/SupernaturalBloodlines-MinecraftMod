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
    void aSmallPackKeepsTheFullSizeAndHidesNobody() {
        PackRing.Layout layout = PackRing.layout(PACK, 100, 100, 60, 20);
        assertEquals(20, layout.size());
        assertEquals(0, layout.hidden());
    }
}
