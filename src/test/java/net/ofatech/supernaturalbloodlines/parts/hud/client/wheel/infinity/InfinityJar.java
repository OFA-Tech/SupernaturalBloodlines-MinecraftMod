package net.ofatech.supernaturalbloodlines.parts.hud.client.wheel.infinity;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import org.junit.jupiter.api.Assumptions;

/**
 * Where the tests that run against his real classes find Infinity Stones (8.0.0-alpha.6). {@code -PinfinityJar=<path>} (Gradle) says where and makes a missing file a
 * failure; without it a developer's default path is tried and a missing file skips the test, as on a CI machine that has no copy of his mod.
 */
final class InfinityJar {
    static final String DEFAULT = "C:\\Users\\caiom\\AppData\\Local\\Temp\\opencode\\compat-tensura\\jars\\infinity-stones-tensura-8.0.0-alpha.6.jar";

    private InfinityJar() {
    }

    static File get() {
        String given = System.getProperty("infinity.jar");
        File jar = new File(given != null ? given : DEFAULT);
        if (Boolean.getBoolean("infinity.jar.required")) {
            assertTrue(jar.isFile(), "-PinfinityJar points at " + jar + ", which is not a file");
        } else {
            Assumptions.assumeTrue(jar.isFile(), "no Infinity Stones jar here");
        }
        return jar;
    }
}
