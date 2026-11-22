package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import net.ofatech.supernaturalbloodlines.shared.hub.Contrast;
import org.junit.jupiter.api.Test;

/** The outline of a card (the inset) and of a chip is told from the page behind it by 3:1 or more, in each kit, read from the pictures themselves. */
class KitEdgeContrastTest {
    /** What the cards and chips of a kit sit on: the Grimoire's page, the Blood Moon's lightest sky, Tensura's panel. */
    private static int pageOf(String kit) {
        return switch (kit) {
            case "grimoire" -> 0xFFE9DCBC;
            case "bloodmoon" -> 0xFF2A0B14;
            default -> 0xFF12293F;
        };
    }

    /** The opaque pixels that touch a transparent one (or the picture's edge): the line round the shape. */
    private static List<Integer> outline(BufferedImage img) {
        List<Integer> out = new ArrayList<>();
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if ((img.getRGB(x, y) >>> 24) < 200) {
                    continue;
                }
                boolean edge = false;
                int[][] around = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
                for (int[] o : around) {
                    int nx = x + o[0];
                    int ny = y + o[1];
                    edge |= nx < 0 || ny < 0 || nx >= img.getWidth() || ny >= img.getHeight() || (img.getRGB(nx, ny) >>> 24) < 100;
                }
                if (edge) {
                    out.add(img.getRGB(x, y) | 0xFF000000);
                }
            }
        }
        return out;
    }

    @Test
    void theOutlineOfACardAndOfAChipStandsOutOfThePageByThreeToOne() throws Exception {
        List<String> weak = new ArrayList<>();
        for (String kit : KitSprites.KITS) {
            for (KitSprites.Part part : new KitSprites.Part[] {KitSprites.CARD, KitSprites.Part.CHIP}) {
                List<Integer> line = outline(UiKitSlicesTest.png(KitSprites.of(kit, part.sprite())));
                assertTrue(!line.isEmpty(), kit + " " + part + " has no outline pixels");
                for (int colour : line) {
                    double ratio = Contrast.ratio(colour, pageOf(kit));
                    if (ratio < 3.0) {
                        weak.add(String.format("%s %s outline #%06x on #%06x is %.2f:1", kit, part, colour & 0xFFFFFF, pageOf(kit) & 0xFFFFFF, ratio));
                        break;
                    }
                }
            }
        }
        assertTrue(weak.isEmpty(), "outlines below 3:1: " + weak);
    }
}