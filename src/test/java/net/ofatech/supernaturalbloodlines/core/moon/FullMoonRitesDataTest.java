package net.ofatech.supernaturalbloodlines.core.moon;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** The rites a super moon discounts are the rites whose data says {@code requires_full_moon}: a new such rite must be added to the set, or this fails. */
class FullMoonRitesDataTest {
    private static Path rites() throws Exception {
        return Path.of(FullMoonRitesDataTest.class.getResource("/data/supernaturalbloodlines/enchanted/circle_magic/rite").toURI());
    }

    @Test
    void theSetIsTheRitesThatNeedTheFullMoon() throws Exception {
        Set<String> fromData;
        try (Stream<Path> files = Files.list(rites())) {
            fromData = files.filter(p -> p.toString().endsWith(".json")).filter(p -> {
                try {
                    return Files.readString(p).replaceAll("\\s", "").contains("\"requires_full_moon\":true");
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }).map(p -> "supernaturalbloodlines:" + p.getFileName().toString().replace(".json", "")).collect(Collectors.toSet());
        }
        assertEquals(fromData, LunarMood.FULL_MOON_RITES);
    }
}
