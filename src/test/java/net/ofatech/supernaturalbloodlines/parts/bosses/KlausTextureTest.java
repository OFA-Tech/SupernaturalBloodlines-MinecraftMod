package net.ofatech.supernaturalbloodlines.parts.bosses;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** The art fix of plan 17c, pinned: the wolf's coat is not grey again, and the paint squares are not back on the man's trousers. */
class KlausTextureTest {
    private static BufferedImage read(String name) throws Exception {
        Path base = Path.of("").toAbsolutePath();
        while (!Files.isDirectory(base.resolve("src/main/java"))) {
            base = base.getParent();
        }
        return ImageIO.read(base.resolve("src/main/resources/assets/supernaturalbloodlines/textures/entity/boss/" + name + ".png").toFile());
    }

    /** Mean HSV saturation over the pixels that are drawn. */
    static double meanSaturation(BufferedImage im) {
        double sum = 0;
        long n = 0;
        for (int y = 0; y < im.getHeight(); y++) {
            for (int x = 0; x < im.getWidth(); x++) {
                int argb = im.getRGB(x, y);
                if ((argb >>> 24) == 0) {
                    continue;
                }
                int r = argb >> 16 & 255;
                int g = argb >> 8 & 255;
                int b = argb & 255;
                int max = Math.max(r, Math.max(g, b));
                int min = Math.min(r, Math.min(g, b));
                sum += max == 0 ? 0 : (max - min) / (double) max;
                n++;
            }
        }
        return n == 0 ? 0 : sum / n;
    }

    @Test
    void theWolfCoatIsBrownerThanGrey() throws Exception {
        double s = meanSaturation(read("klaus_hybrid_geo"));
        assertTrue(s >= 0.22, "the wolf coat is washed out again: mean saturation " + s);
    }

    @Test
    void theMeasureSeesAGreyCoat() {
        BufferedImage grey = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < 4; i++) {
            grey.setRGB(i % 2, i / 2, 0xFF808080);
        }
        assertTrue(meanSaturation(grey) < 0.01);
    }

    @Test
    void thePaintSquaresAreNotOnTheMansSkin() throws Exception {
        BufferedImage skin = read("klaus");
        Set<Integer> stray = Set.of(0xA02228, 0xC49234, 0x285A8C);
        for (int y = 0; y < skin.getHeight(); y++) {
            for (int x = 0; x < skin.getWidth(); x++) {
                int argb = skin.getRGB(x, y);
                assertTrue((argb >>> 24) == 0 || !stray.contains(argb & 0xFFFFFF), "a paint square is back at " + x + "," + y);
            }
        }
    }
}
