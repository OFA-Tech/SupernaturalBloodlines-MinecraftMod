package net.ofatech.supernaturalbloodlines.parts.evolutionrites.probe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class AscensionProbeTest {
    /** Stands in for the addon's config: a threshold and a cooldown, or a missing field. */
    private record Fake(Optional<String> missing, double minEp, int cooldownMinutes) implements AscensionProbe.Source {
        @Override
        public Optional<String> missingHook() {
            return missing;
        }
    }

    private static final AscensionProbe.Source ASCENSION = new Fake(Optional.empty(), 5_000_000, 120);

    private static AscensionProbe.Live live(double maxEp, boolean hero, boolean demonLord, long last, long now, boolean rule) {
        return new AscensionProbe.Live(maxEp, hero, demonLord, last, now, rule);
    }

    @Test
    void aGoodTargetIsSatisfied() {
        assertTrue(AscensionProbe.check(ASCENSION, live(6_000_000, true, false, 0, 1_000_000, true)));
    }

    @Test
    void eachRequirementFailsOnItsOwn() {
        assertFalse(AscensionProbe.check(ASCENSION, live(4_999_999, true, false, 0, 1_000_000, true)), "below the EP threshold");
        assertTrue(AscensionProbe.check(ASCENSION, live(5_000_000, true, false, 0, 1_000_000, true)), "exactly at it");
        assertFalse(AscensionProbe.check(ASCENSION, live(6_000_000, false, false, 0, 1_000_000, true)), "neither status");
        assertTrue(AscensionProbe.check(ASCENSION, live(6_000_000, false, true, 0, 1_000_000, true)), "a Demon Lord");
        assertFalse(AscensionProbe.check(ASCENSION, live(6_000_000, true, false, 999_999, 1_000_000, true)), "inside the cooldown");
        assertTrue(AscensionProbe.check(ASCENSION, live(6_000_000, true, false, 1_000_000 - 144_000, 1_000_000, true)), "the cooldown is over at the tick");
        assertFalse(AscensionProbe.check(ASCENSION, live(6_000_000, true, false, 0, 1_000_000, false)), "the rule is off");
    }

    @Test
    void theThresholdAndTheCooldownAreTheAddonsLiveValues() {
        assertTrue(AscensionProbe.check(new Fake(Optional.empty(), 1000, 120), live(1000, true, false, 0, 10, true)), "a server that lowers the threshold");
        assertFalse(AscensionProbe.check(new Fake(Optional.empty(), 9_000_000, 120), live(6_000_000, true, false, 0, 10, true)), "a server that raises it");
        assertTrue(AscensionProbe.check(new Fake(Optional.empty(), 5_000_000, 0), live(6_000_000, true, false, 9, 10, true)), "a server with no cooldown");
    }

    @Test
    void theTagIsTheAltarsTag() {
        assertEquals("ascension:last_awakening_tick", AscensionProbe.LAST_AWAKENING_TAG);
    }

    @Test
    void stampingWritesTheGameTimeUnderTheAltarsTag() {
        CompoundTag tag = new CompoundTag();
        AscensionProbe.stamp(tag, 123_456L);
        assertEquals(123_456L, tag.getLong("ascension:last_awakening_tick"));
        assertTrue(AwakeningStampReadBack.coolingDown(tag, 123_457L), "the tag the rite writes is the tag the check reads: a second rite right after is refused");
    }

    @Test
    void aMissingConfigFieldIsNamedAndNothingIsGuessed() {
        Fake missing = new Fake(Optional.of("AddonConfig#AWAKENING_MIN_EP"), 0, 0);
        assertEquals(Optional.of("AddonConfig#AWAKENING_MIN_EP"), missing.missingHook());
        assertTrue(Probes.of(Probes.ASCENSION_AWAKENING).filter(p -> p instanceof AscensionProbe).isPresent(), "the id the rite file names is the Ascension probe");
        assertEquals("ascension.awakening", Probes.ASCENSION_AWAKENING);
    }

    /** Reads back what {@link AscensionProbe#stamp} wrote the way the check does, with the cooldown the addon defaults to. */
    private static final class AwakeningStampReadBack {
        static boolean coolingDown(CompoundTag tag, long now) {
            return net.ofatech.supernaturalbloodlines.core.evolution.AwakeningRules.coolingDown(now, tag.getLong(AscensionProbe.LAST_AWAKENING_TAG), 120);
        }
    }

    @Test
    void theRealJarNotBeingThereIsAMissingHookNotAnException() {
        Optional<String> missing = new AscensionProbe().missingHook();
        assertTrue(missing.isPresent(), "no Ascension on the unit test classpath");
        assertTrue(missing.get().contains("com.simplygray.tensuraaddon"), missing.get());
    }
}
