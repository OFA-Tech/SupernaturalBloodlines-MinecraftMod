package net.ofatech.supernaturalbloodlines.parts.evolutionrites.probe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class HooksTest {
    public static final class Fake {
        public static final Supplier<Boolean> ON = () -> true;
        public static final Supplier<Boolean> OFF = () -> false;
        public static final Supplier<Boolean> NOT_LOADED = () -> {
            throw new IllegalStateException("config not loaded");
        };
        public static final String NOT_A_VALUE = "x";
        public static int instanceLike;
    }

    private static final String FAKE = Fake.class.getName();

    @Test
    void findsAJdkMethod() {
        Hooks.Hit hit = Hooks.method("java.lang.String", "length");
        assertTrue(hit.found());
        assertEquals(Optional.empty(), hit.missing());
    }

    @Test
    void namesAMissingClass() {
        Hooks.Hit hit = Hooks.method("no.such.Clazz", "x");
        assertFalse(hit.found());
        assertTrue(hit.missing().orElseThrow().contains("no.such.Clazz"));
    }

    @Test
    void namesAMissingMethod() {
        Hooks.Hit hit = Hooks.method("java.lang.String", "noSuchMethod", int.class);
        assertFalse(hit.found());
        assertTrue(hit.missing().orElseThrow().contains("String#noSuchMethod"), hit.missing().toString());
    }

    @Test
    void readsAStaticField() {
        assertTrue(Hooks.staticField(FAKE, "ON").found());
        assertTrue(Hooks.staticField(FAKE, "nope").missing().orElseThrow().contains("Fake#nope"));
        assertTrue(Hooks.staticField("no.such.Clazz", "ON").missing().orElseThrow().contains("no.such.Clazz"));
    }

    @Test
    void theUniqueRoadProbeParsesItsId() {
        assertTrue(UniqueRoadProbe.parse("unique.road:a.B#C").isPresent());
        assertTrue(UniqueRoadProbe.parse("unique.road:a.B").isEmpty());
        assertTrue(UniqueRoadProbe.parse("unique.road:#C").isEmpty());
        assertTrue(UniqueRoadProbe.parse("own.sage_great_sage").isEmpty());
    }

    @Test
    void theUniqueRoadProbeFollowsTheAddonSwitch() {
        UniqueRoadProbe on = new UniqueRoadProbe(FAKE, "ON");
        assertEquals(Optional.empty(), on.missingHook());
        assertTrue(on.roadOpen());
        UniqueRoadProbe off = new UniqueRoadProbe(FAKE, "OFF");
        assertEquals(Optional.empty(), off.missingHook());
        assertFalse(off.roadOpen());
    }

    @Test
    void anUnloadedConfigAnswersLikeItsDefault() {
        UniqueRoadProbe p = new UniqueRoadProbe(FAKE, "NOT_LOADED");
        assertEquals(Optional.empty(), p.missingHook());
        assertTrue(p.roadOpen(), "a config that is not loaded yet is the addon's default, which is on");
    }

    @Test
    void aMissingClassFieldOrAnythingThatIsNotAConfigValueMakesTheProbeUnavailable() {
        assertTrue(new UniqueRoadProbe("no.such.Clazz", "X").missingHook().isPresent());
        assertTrue(new UniqueRoadProbe(FAKE, "NOPE").missingHook().isPresent());
        assertTrue(new UniqueRoadProbe(FAKE, "NOT_A_VALUE").missingHook().isPresent());
        assertFalse(new UniqueRoadProbe("no.such.Clazz", "X").roadOpen(), "a missing hook must never read as open");
    }

    @Test
    void anUnknownProbeIdIsNoProbe() {
        assertTrue(Probes.of("no.such").isEmpty());
        assertTrue(Probes.of(Probes.OWN_SAGE_GREAT_SAGE).isPresent());
        assertTrue(Probes.of("unique.road:" + FAKE + "#ON").orElseThrow().roadOpen());
    }
}
