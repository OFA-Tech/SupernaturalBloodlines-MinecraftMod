package net.ofatech.supernaturalbloodlines.core.blood;

import static net.ofatech.supernaturalbloodlines.core.blood.ForceFeedRules.Refusal.NOT_A_PLAYER;
import static net.ofatech.supernaturalbloodlines.core.blood.ForceFeedRules.Refusal.NOT_WILLING;
import static net.ofatech.supernaturalbloodlines.core.blood.ForceFeedRules.Refusal.SELF;
import static net.ofatech.supernaturalbloodlines.core.blood.ForceFeedRules.Refusal.VERVAIN;
import static net.ofatech.supernaturalbloodlines.core.blood.ForceFeedRules.check;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import net.ofatech.supernaturalbloodlines.core.blood.BloodBottleRules.Effect;
import org.junit.jupiter.api.Test;

class ForceFeedRulesTest {
    private static final float LIMIT = 0.30f;

    @Test
    void aCreatureIsNotAPlayer() {
        assertEquals(Optional.of(NOT_A_PLAYER), check(false, false, false, true, true, 0.1f, true, LIMIT));
    }

    @Test
    void notAPlayerComesBeforeEverythingElse() {
        assertEquals(Optional.of(NOT_A_PLAYER), check(false, true, true, false, false, 1f, false, LIMIT));
    }

    @Test
    void youCannotFeedYourself() {
        assertEquals(Optional.of(SELF), check(true, true, false, true, true, 0.1f, true, LIMIT));
    }

    @Test
    void vervainBlocksAWillingTarget() {
        assertEquals(Optional.of(VERVAIN), check(true, false, true, true, true, 1f, false, LIMIT));
    }

    @Test
    void vervainBlocksAnUnwillingTargetThatCouldOtherwiseBeFed() {
        assertEquals(Optional.of(VERVAIN), check(true, false, true, false, true, 0.1f, true, LIMIT));
    }

    @Test
    void consentComesBeforeVervain() {
        // pvp off, healthy, not down, not sneaking: nobody may feed them, so the refusal must not say they carry vervain
        assertEquals(Optional.of(NOT_WILLING), check(true, false, true, false, false, 1f, false, LIMIT));
        assertEquals(Optional.of(NOT_WILLING), check(true, false, true, false, true, 1f, false, LIMIT));
        assertEquals(Optional.of(NOT_WILLING), check(true, false, true, false, false, 0.1f, true, LIMIT));
    }

    @Test
    void aWillingTargetAcceptsAtFullHealthWithPvpOff() {
        assertEquals(Optional.empty(), check(true, false, false, true, false, 1f, false, LIMIT));
    }

    @Test
    void anUnwillingDownedTargetWithPvpOffIsRefused() {
        assertEquals(Optional.of(NOT_WILLING), check(true, false, false, false, false, 1f, true, LIMIT));
    }

    @Test
    void anUnwillingHealthyTargetIsRefused() {
        assertEquals(Optional.of(NOT_WILLING), check(true, false, false, false, true, 1f, false, LIMIT));
    }

    @Test
    void anUnwillingTargetAtTheLimitExactlyIsAllowed() {
        assertEquals(Optional.empty(), check(true, false, false, false, true, 0.30f, false, LIMIT));
    }

    @Test
    void anUnwillingTargetJustAboveTheLimitIsRefused() {
        assertEquals(Optional.of(NOT_WILLING), check(true, false, false, false, true, 0.31f, false, LIMIT));
    }

    @Test
    void anUnwillingDownedTargetAtFullHealthIsAllowedWithPvpOn() {
        assertEquals(Optional.empty(), check(true, false, false, false, true, 1f, true, LIMIT));
    }

    @Test
    void aLimitOfZeroLeavesOnlyTheDowned() {
        assertEquals(Optional.of(NOT_WILLING), check(true, false, false, false, true, 0.01f, false, 0f));
        assertEquals(Optional.empty(), check(true, false, false, false, true, 0.01f, true, 0f));
    }

    @Test
    void bloodForcedOnSomeoneNeverNamesASireOrTouchesATransition() {
        assertEquals(Effect.REFUSED, ForceFeedRules.withoutConsent(Effect.NAME_SIRE));
        assertEquals(Effect.REFUSED, ForceFeedRules.withoutConsent(Effect.TRANSITION_BLOOD));
        assertEquals(Effect.MARK, ForceFeedRules.withoutConsent(Effect.MARK_AND_NAME_SIRE));
    }

    @Test
    void everyOtherEffectIsUnchangedWithoutConsent() {
        for (Effect e : Effect.values()) {
            if (e != Effect.NAME_SIRE && e != Effect.TRANSITION_BLOOD && e != Effect.MARK_AND_NAME_SIRE) {
                assertEquals(e, ForceFeedRules.withoutConsent(e), e.name());
            }
        }
    }
}
