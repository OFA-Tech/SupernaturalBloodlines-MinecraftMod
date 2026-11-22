package net.ofatech.supernaturalbloodlines.core.bloodline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AugustineBloodRulesTest {
    private static final double FROM_VAMPIRE = 3.0;
    private static final double FROM_OTHER = 1.0 / 3.0;

    @Test
    void aVampireGivesTripleWhateverTheRoll() {
        for (double roll : new double[] {0, 0.5, 0.999}) {
            assertEquals(9, AugustineBloodRules.scale(3, true, FROM_VAMPIRE, FROM_OTHER, roll));
            assertEquals(3, AugustineBloodRules.scale(1, true, FROM_VAMPIRE, FROM_OTHER, roll));
        }
    }

    @Test
    void aThirdIsRoundedAtRandomSoItIsAThirdOnAverage() {
        // 1 x 1/3: 1 in the bites whose roll is under a third, 0 in the rest
        assertEquals(1, AugustineBloodRules.scale(1, false, FROM_VAMPIRE, FROM_OTHER, 0.0));
        assertEquals(1, AugustineBloodRules.scale(1, false, FROM_VAMPIRE, FROM_OTHER, 0.33));
        assertEquals(0, AugustineBloodRules.scale(1, false, FROM_VAMPIRE, FROM_OTHER, 0.34));
        assertEquals(0, AugustineBloodRules.scale(1, false, FROM_VAMPIRE, FROM_OTHER, 0.999));
        // 3 x 1/3 is exactly 1; 10 x 1/3 is 3 or 4
        assertEquals(1, AugustineBloodRules.scale(3, false, FROM_VAMPIRE, FROM_OTHER, 0.999));
        assertEquals(3, AugustineBloodRules.scale(10, false, FROM_VAMPIRE, FROM_OTHER, 0.9));
        assertEquals(4, AugustineBloodRules.scale(10, false, FROM_VAMPIRE, FROM_OTHER, 0.1));
        // the mean over a seeded run is a third (a floor of 1 would make it 1)
        Random rng = new Random(42);
        long sum = 0;
        int bites = 30_000;
        for (int i = 0; i < bites; i++) {
            sum += AugustineBloodRules.scale(1, false, FROM_VAMPIRE, FROM_OTHER, rng.nextDouble());
        }
        assertEquals(1.0 / 3.0, sum / (double) bites, 0.01);
    }

    @Test
    void nothingStaysNothing() {
        for (double roll : new double[] {0, 0.5, 0.999}) {
            assertEquals(0, AugustineBloodRules.scale(0, true, FROM_VAMPIRE, FROM_OTHER, roll), "a refused bite stays refused");
            assertEquals(0, AugustineBloodRules.scale(0, false, FROM_VAMPIRE, FROM_OTHER, roll));
            assertEquals(0, AugustineBloodRules.scale(-4, true, FROM_VAMPIRE, FROM_OTHER, roll));
        }
    }

    @Test
    void aZeroFactorGivesNothing() {
        assertEquals(0, AugustineBloodRules.scale(5, true, 0, FROM_OTHER, 0));
        assertEquals(0, AugustineBloodRules.scale(5, false, FROM_VAMPIRE, 0, 0));
    }

    @Test
    void aHugeAmountDoesNotOverflow() {
        assertTrue(AugustineBloodRules.scale(Integer.MAX_VALUE, true, 10, FROM_OTHER, 0.5) > 0);
    }

    @Test
    void garlicIsAlwaysDeniedAndBlindnessOnlyInStrongGarlic() {
        assertTrue(AugustineBloodRules.wardDenies(true, false, false), "garlic's own effect");
        assertTrue(AugustineBloodRules.wardDenies(true, false, true));
        assertTrue(AugustineBloodRules.wardDenies(false, true, true), "blindness from strong garlic");
        assertFalse(AugustineBloodRules.wardDenies(false, true, false), "a blindness potion is not the ward's business");
        assertFalse(AugustineBloodRules.wardDenies(false, false, true), "an unrelated effect in garlic");
    }

    @Test
    void theExtraIsPaidOncePerFeedNotOncePerBite() {
        AugustineBloodRules.FeedChain chain = new AugustineBloodRules.FeedChain();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        assertTrue(chain.firstBite(a, b, 0), "the first bite starts a feed");
        for (long t = 10; t <= 400; t += 10) {
            assertFalse(chain.firstBite(a, b, t), "a bite within the window of the last one is the same feed (t=" + t + ")");
        }
        assertTrue(chain.firstBite(a, c, 405), "another victim is a feed of its own");
        assertTrue(chain.firstBite(b, a, 410), "the other way round is another pair");
        assertTrue(chain.firstBite(a, b, 400 + AugustineBloodRules.CHAIN_TICKS + 1), "a pause longer than the window starts a new feed");
    }
}
