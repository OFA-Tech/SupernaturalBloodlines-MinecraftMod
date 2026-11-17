package net.ofatech.supernaturalbloodlines.parts.species;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LegacyConfigTest {
    @TempDir
    Path dir;

    private Path write(String text) throws IOException {
        Path p = dir.resolve("supernaturalbloodlines-species.toml");
        Files.writeString(p, text);
        return p;
    }

    @Test
    void theOldKeyIsFoundWhateverItsValue() throws IOException {
        assertTrue(LegacyConfig.hasSirelineDeath(write("[species]\n\t#When an Original is killed\n\tsireline_death = false\n")));
        assertTrue(LegacyConfig.hasSirelineDeath(write("[species]\nsireline_death=true\n")));
    }

    @Test
    void aCommentTheNewKeyOrNoFileIsNotTheOldKey() throws IOException {
        assertFalse(LegacyConfig.hasSirelineDeath(write("[species]\n#sireline_death = false\noriginal_line_death = true\n")));
        assertFalse(LegacyConfig.hasSirelineDeath(write("[species]\nmy_sireline_death_note = 1\n")));
        assertFalse(LegacyConfig.hasSirelineDeath(dir.resolve("missing.toml")));
    }

    @Test
    void theWarningSaysWhatToDo() {
        assertTrue(LegacyConfig.WARNING.contains("original_line_death = false"));
        assertTrue(LegacyConfig.WARNING.contains("ignored"));
    }
}
