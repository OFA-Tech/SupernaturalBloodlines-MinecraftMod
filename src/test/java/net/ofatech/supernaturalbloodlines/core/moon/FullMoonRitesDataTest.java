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
    /** Full-moon rites that a super moon does not make cheaper (Severance, the Alpha Trial): the rite file says full moon, the discount list leaves them out. */
    private static final Set<String> NO_DISCOUNT_ON_PURPOSE = Set.of("supernaturalbloodlines:rite_of_severance", "supernaturalbloodlines:alpha_trial",
            "supernaturalbloodlines:rite_of_the_valet_pike", "supernaturalbloodlines:rite_of_the_frequency");

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
        Set<String> both = new java.util.HashSet<>(LunarMood.FULL_MOON_RITES);
        both.addAll(NO_DISCOUNT_ON_PURPOSE);
        assertEquals(fromData, both);
        assertEquals(Set.of(), LunarMood.FULL_MOON_RITES.stream().filter(NO_DISCOUNT_ON_PURPOSE::contains).collect(Collectors.toSet()), "a rite is discounted and not discounted");
    }
}
