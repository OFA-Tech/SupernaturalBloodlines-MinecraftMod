package net.ofatech.supernaturalbloodlines.shared.supernatural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class RiteLayoutTest {
    @Test
    void oneItemSitsAboveTheCentreAndSixFillTheFirstRing() {
        assertEquals(List.of(new RiteLayout.Spot(0, -16)), RiteLayout.items(1));
        List<RiteLayout.Spot> six = RiteLayout.items(6);
        assertEquals(new RiteLayout.Spot(0, -16), six.get(0));
        assertEquals(new RiteLayout.Spot(14, -8), six.get(1)); // clockwise on a screen
        assertEquals(new RiteLayout.Spot(0, 16), six.get(3));
        assertEquals(6, new HashSet<>(six).size());
    }

    @Test
    void theSeventhItemStartsTheSecondRingAtTheTop() {
        List<RiteLayout.Spot> seven = RiteLayout.items(7);
        assertEquals(new RiteLayout.Spot(0, -32), seven.get(6));
        assertEquals(0, RiteLayout.items(0).size());
    }

    @Test
    void everyRiteWeShipFitsOnTwoRingsWithoutTwoItemsOnOneSpot() {
        for (int n = 1; n <= 18; n++) {
            assertEquals(n, new HashSet<>(RiteLayout.items(n)).size(), n + " items");
            RiteLayout.items(n).forEach(s -> assertTrue(Math.hypot(s.x(), s.y()) <= 16 * 3 + 1));
        }
    }

    @Test
    void aCircleIsReadFromItsShapeAndChalk() {
        assertEquals(new HubSocial.Circle("medium_circle", "nether_chalk"), HubSocial.Circle.parse("enchanted:medium_circle=enchanted:nether_chalk"));
        assertEquals("textures/gui/circle_magic_shapes/medium_circle_nether_chalk.png", RiteLayout.shapeTexture(HubSocial.Circle.parse("enchanted:medium_circle=enchanted:nether_chalk")));
        assertEquals(new HubSocial.Circle("small_circle", ""), HubSocial.Circle.parse("enchanted:small_circle"));
        assertEquals(List.of(new HubSocial.Circle("large_circle", "ritual_chalk")),
                new HubSocial.Rite("a:b", 1, List.of(), List.of("enchanted:large_circle=enchanted:ritual_chalk"), "f", "").circles());
    }
}
