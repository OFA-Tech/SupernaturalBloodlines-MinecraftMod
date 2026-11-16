package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class SkillIconsTest {
    @Test
    void everySkillWeShipHasItsOwnIcon() throws Exception {
        Path dir = Path.of(SkillIconsTest.class.getResource("/data/supernaturalbloodlines/supernatural/skill/s-vam-1.json").toURI()).getParent();
        List<String> ids;
        try (Stream<Path> files = Files.list(dir)) {
            ids = files.map(p -> p.getFileName().toString().replace(".json", "")).toList();
        }
        assertTrue(ids.size() >= 30, "the skill folder was not found");
        for (String id : ids) {
            assertTrue(SkillIcons.known(id), id + " has no icon");
        }
    }

    @Test
    void anUnknownSkillWearsASpark() {
        assertEquals(Icons.SPARK, SkillIcons.of("some-other-mod:skill"));
        assertEquals(Icons.FANG, SkillIcons.of("s-hyb-2"));
    }
}
