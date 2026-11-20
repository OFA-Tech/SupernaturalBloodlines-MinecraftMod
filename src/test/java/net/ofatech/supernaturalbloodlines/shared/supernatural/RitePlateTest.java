package net.ofatech.supernaturalbloodlines.shared.supernatural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** Enchanted's circle pictures are chalk on nothing: white, dark red and dark purple. Each must read (3:1 or better) on the plate we put under it, in the hub and in the book. */
class RitePlateTest {
    /** Enchanted is not on the unit tests' classpath: its jar is read from the Gradle cache (where the build got it), and the tests that need it skip without. */
    private static final java.nio.file.Path JARS = java.nio.file.Path.of(System.getProperty("user.home"), ".gradle", "caches", "modules-2", "files-2.1", "net.favouriteless.enchanted");

    private static java.io.File jar() throws Exception {
        if (!java.nio.file.Files.isDirectory(JARS)) {
            return null;
        }
        try (var walk = java.nio.file.Files.walk(JARS)) {
            return walk.filter(p -> p.getFileName().toString().matches("enchanted-neoforge-1\\.21\\.1-[\\d.]+\\.jar")).map(java.nio.file.Path::toFile).findFirst().orElse(null);
        }
    }

    private static BufferedImage picture(String path) throws Exception {
        java.io.File jar = jar();
        org.junit.jupiter.api.Assumptions.assumeTrue(jar != null, "Enchanted's jar is not in the Gradle cache");
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(jar)) {
            var entry = zip.getEntry("assets/enchanted/textures/gui/" + path);
            assertTrue(entry != null, path + " is not in Enchanted: its pictures moved");
            try (InputStream in = zip.getInputStream(entry)) {
                return ImageIO.read(in);
            }
        }
    }

    /** The lowest contrast against {@code ground} of any opaque pixel of the picture. */
    private static double worst(BufferedImage img, int ground) {
        double worst = 100;
        Set<Integer> seen = new HashSet<>();
        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                int argb = img.getRGB(x, y);
                if ((argb >>> 24) > 128 && seen.add(argb)) {
                    worst = Math.min(worst, RitePlate.contrast(argb | 0xFF000000, ground));
                }
            }
        }
        return worst;
    }

    @Test
    void everyChalkReadsOnItsPlate() throws Exception {
        for (String chalk : new String[] {"ritual_chalk", "nether_chalk", "otherwhere_chalk"}) {
            for (String shape : new String[] {"small", "medium", "large"}) {
                double c = worst(picture("circle_magic_shapes/" + shape + "_circle_" + chalk.replace("_chalk", "") + "_chalk.png"), RitePlate.plate(chalk));
                assertTrue(c >= RitePlate.MIN_CONTRAST, shape + " " + chalk + " is " + c + ":1 on its plate");
            }
        }
    }

    @Test
    void theColoursWeKeepAreTheDarkestPixelsOfTheRealPictures() throws Exception {
        for (String chalk : new String[] {"ritual_chalk", "nether_chalk", "otherwhere_chalk"}) {
            BufferedImage img = picture("circle_magic_shapes/medium_circle_" + chalk.replace("_chalk", "") + "_chalk.png");
            double ours = RitePlate.contrast(RitePlate.chalkColor(chalk), RitePlate.plate(chalk));
            double real = worst(img, RitePlate.plate(chalk));
            assertEquals(real, ours, 0.05, chalk + ": RitePlate.chalkColor is not the pixel that reads worst");
        }
    }

    @Test
    void theGoldHeartReadsOnWhatItSitsOn() throws Exception {
        BufferedImage glyph = picture("gold_glyph.png");
        for (String chalk : new String[] {"ritual_chalk", "nether_chalk", "otherwhere_chalk"}) {
            assertTrue(worst(glyph, RitePlate.glyphGround(chalk)) >= RitePlate.MIN_CONTRAST, chalk + ": the gold heart does not read");
        }
        assertTrue(RitePlate.contrast(RitePlate.GLYPH, RitePlate.LIGHT) < RitePlate.MIN_CONTRAST, "a light plate does not need the dark backing: remove it");
    }

    /** The same contrast from the colours we keep, so this holds where Enchanted's jar is not to be had. */
    @Test
    void theKeptColoursReadOnTheirPlates() {
        for (String chalk : new String[] {"ritual_chalk", "nether_chalk", "otherwhere_chalk"}) {
            assertTrue(RitePlate.contrast(RitePlate.chalkColor(chalk), RitePlate.plate(chalk)) >= RitePlate.MIN_CONTRAST, chalk);
            assertTrue(RitePlate.contrast(RitePlate.GLYPH, RitePlate.glyphGround(chalk)) >= RitePlate.MIN_CONTRAST, chalk + " glyph");
        }
    }

    @Test
    void theOldPlateWasTheProblem() {
        // the hub's dark plate under infernal chalk: 2.4:1, which is what this fixes
        assertTrue(RitePlate.contrast(RitePlate.chalkColor("nether_chalk"), RitePlate.DARK) < 3.0);
        assertTrue(RitePlate.contrast(RitePlate.chalkColor("ritual_chalk"), 0xFFD8C8A0) < 3.0, "white chalk on parchment");
    }
}
