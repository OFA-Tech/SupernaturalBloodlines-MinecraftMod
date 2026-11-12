package net.ofatech.supernaturalbloodlines.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigTest {
    @TempDir
    Path dir;

    private Path write(String content) throws IOException {
        return Files.writeString(dir.resolve("c.toml"), content);
    }

    @Test
    void missingFileIsEnabled() {
        assertTrue(Config.partEnabled(dir.resolve("none.toml"), Part.SPECIES));
    }

    @Test
    void falseSwitchDisablesOnlyThatPart() throws IOException {
        Path f = write("[parts]\nspecies = false\nartifacts = true\n");
        assertFalse(Config.partEnabled(f, Part.SPECIES));
        assertTrue(Config.partEnabled(f, Part.ARTIFACTS));
        assertTrue(Config.partEnabled(f, Part.VAMPIRISM_TENSURA));
    }

    @Test
    void nonBooleanIsEnabled() throws IOException {
        assertTrue(Config.partEnabled(write("[parts]\nspecies = \"no\"\n"), Part.SPECIES));
    }

    @Test
    void garbageIsEnabled() throws IOException {
        assertTrue(Config.partEnabled(write("[parts\n = = \u0000 {{{"), Part.SPECIES));
    }
}
