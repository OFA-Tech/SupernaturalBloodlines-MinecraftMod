package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** The atlas {@code tools/gen_icons.py} writes and the {@link Icons} that name its cells must agree. */
class IconsTest {
    private static final String DIR = "/assets/supernaturalbloodlines/textures/gui/";

    @Test
    void theNamesInTheAtlasAreTheEnumInOrder() throws Exception {
        try (InputStream in = IconsTest.class.getResourceAsStream(DIR + "icons.txt")) {
            assertNotNull(in, "icons.txt is missing: run tools/gen_icons.py");
            List<String> names = Arrays.stream(new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\\R")).filter(s -> !s.isBlank()).toList();
            assertEquals(Arrays.stream(Icons.values()).map(Icons::id).toList(), names);
        }
    }

    @Test
    void everyNamedCellIsDrawnAndTheRestIsEmpty() throws Exception {
        try (InputStream in = IconsTest.class.getResourceAsStream(DIR + "icons.png")) {
            assertNotNull(in, "icons.png is missing: run tools/gen_icons.py");
            BufferedImage img = ImageIO.read(in);
            assertEquals(Icons.COLUMNS * Icons.CELL, img.getWidth());
            assertEquals(Icons.ROWS * Icons.CELL, img.getHeight());
            for (int i = 0; i < Icons.COLUMNS * Icons.ROWS; i++) {
                int drawn = 0;
                for (int y = 0; y < Icons.CELL; y++) {
                    for (int x = 0; x < Icons.CELL; x++) {
                        int argb = img.getRGB((i % Icons.COLUMNS) * Icons.CELL + x, (i / Icons.COLUMNS) * Icons.CELL + y);
                        if ((argb >>> 24) != 0) {
                            drawn++;
                            assertEquals(0xFFFFFF, argb & 0xFFFFFF, "a sprite is white, the theme tints it");
                        }
                    }
                }
                if (i < Icons.values().length) {
                    assertTrue(drawn >= 12, Icons.values()[i] + " is nearly empty (" + drawn + " pixels)");
                } else {
                    assertEquals(0, drawn, "cell " + i + " has no name but is drawn");
                }
            }
        }
    }

    @Test
    void theMoonOfEachVanillaPhaseIsTheRightOne() {
        assertEquals(Icons.MOON_FULL, Icons.moon(0));
        assertEquals(Icons.MOON_NEW, Icons.moon(4));
        assertEquals(Icons.MOON_FIRST_QUARTER, Icons.moon(6));
        assertEquals(Icons.MOON_LAST_QUARTER, Icons.moon(2));
        assertEquals(Icons.MOON_FULL, Icons.moon(8));
        assertEquals(8, Arrays.stream(Icons.values()).filter(i -> i.name().startsWith("MOON_")).count());
    }
}
