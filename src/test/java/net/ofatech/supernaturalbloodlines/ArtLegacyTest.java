package net.ofatech.supernaturalbloodlines;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The four Werewolves-model boss textures are replaced by the GeckoLib ones (plan 17, open question 11): kept in art/legacy, not in the jar. */
class ArtLegacyTest {
    private static final List<String> SUPERSEDED = List.of("deucalion_demon_wolf", "peter_hale_wolf", "klaus_hybrid", "beast_of_gevaudan");

    private static Path root() {
        Path base = Path.of("").toAbsolutePath();
        while (!Files.isDirectory(base.resolve("src/main/java"))) {
            base = base.getParent();
        }
        return base;
    }

    @Test
    void supersededBossTexturesAreNotShipped() {
        for (String name : SUPERSEDED) {
            assertFalse(Files.exists(root().resolve("src/main/resources/assets/supernaturalbloodlines/textures/entity/boss/" + name + ".png")),
                    name + ".png is back in the jar");
        }
    }

    @Test
    void supersededBossTexturesAreKeptInArtLegacy() {
        for (String name : SUPERSEDED) {
            assertTrue(Files.exists(root().resolve("art/legacy/" + name + ".png")), "art/legacy/" + name + ".png is gone");
        }
    }
}
