package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** The mod icon is wired into the mod list and the store pictures are the drawn art at whole-number sizes (UI overhaul phase 8). */
class LogoAssetsTest {
    private static Path root() {
        Path base = Path.of("").toAbsolutePath();
        while (!Files.isDirectory(base.resolve("src/main/java"))) {
            base = base.getParent();
        }
        return base;
    }

    private static BufferedImage png(String path) throws Exception {
        return ImageIO.read(root().resolve(path).toFile());
    }

    private static String text(String path) throws Exception {
        return Files.readString(root().resolve(path));
    }

    @Test
    void theModsListShowsTheLogoWithHardEdges() throws Exception {
        String toml = text("src/main/templates/META-INF/neoforge.mods.toml");
        assertTrue(toml.lines().anyMatch(l -> l.startsWith("logoFile=\"logo.png\"")), "logoFile points at the icon in the jar root");
        assertTrue(toml.lines().anyMatch(l -> l.startsWith("logoBlur=false")), "pixel art must not be blurred");
        BufferedImage logo = png("src/main/resources/logo.png");
        assertEquals(64, logo.getWidth());
        assertEquals(64, logo.getHeight());
    }

    @Test
    void thePackIconIsTheSameLogo() throws Exception {
        assertArrayEquals(Files.readAllBytes(root().resolve("src/main/resources/logo.png")), Files.readAllBytes(root().resolve("src/main/resources/pack.png")));
    }

    /** Each pixel of the source is an exact scale x scale block in the export, at the given offset: nothing smoothed, nothing redrawn. */
    private static void assertWholeNumberScale(BufferedImage source, BufferedImage export, int scale, int offX, int offY, int width, int height) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int expected = source.getRGB(x, y);
                int ex = (x + offX) * scale;
                int ey = (y + offY) * scale;
                for (int d : new int[] {0, scale - 1, scale / 2}) {
                    int got = export.getRGB(ex + d, ey + d);
                    if ((expected >>> 24) == 255) {
                        assertEquals(expected, got, "texel " + x + "," + y + " (offset " + d + ")");
                    }
                }
            }
        }
    }

    @Test
    void theAvatarIsSquareAtLeast400AndTheLogoAtExactly8x() throws Exception {
        BufferedImage avatar = png("docs/store/avatar.png");
        assertEquals(avatar.getWidth(), avatar.getHeight());
        assertTrue(avatar.getWidth() >= 400, "CurseForge asks for at least 400 x 400");
        assertEquals(512, avatar.getWidth());
        assertWholeNumberScale(png("src/main/resources/logo.png"), avatar, 8, 0, 0, 64, 64);
    }

    @Test
    void theBannerAndTheReadmeHeaderAreTheLockupAtWholeNumbers() throws Exception {
        BufferedImage lockup = png("src/main/resources/assets/supernaturalbloodlines/textures/gui/sprites/kit/shared/logo_lockup.png");
        BufferedImage banner = png("docs/store/banner.png");
        assertEquals(256 * 6, banner.getWidth());
        assertEquals(72 * 6, banner.getHeight());
        assertWholeNumberScale(lockup, banner, 6, (256 - lockup.getWidth()) / 2, (72 - lockup.getHeight()) / 2, lockup.getWidth(), lockup.getHeight());
        BufferedImage header = png("docs/store/readme-header.png");
        assertEquals(224 * 3, header.getWidth());
        assertEquals(68 * 3, header.getHeight());
        assertWholeNumberScale(lockup, header, 3, (224 - lockup.getWidth()) / 2, (68 - lockup.getHeight()) / 2, lockup.getWidth(), lockup.getHeight());
    }

    @Test
    void theReadmeAndTheWikiHomeStartWithTheLogo() throws Exception {
        assertTrue(text("README.md").startsWith("<p align=\"center\"><img src=\"docs/store/readme-header.png\""));
        assertTrue(text("wiki/Home.md").contains("docs/store/readme-header.png"));
        assertTrue(Files.exists(root().resolve("docs/store/readme-header.png")));
    }
}