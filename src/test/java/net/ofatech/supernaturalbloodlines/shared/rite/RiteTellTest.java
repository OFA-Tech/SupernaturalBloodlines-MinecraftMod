package net.ofatech.supernaturalbloodlines.shared.rite;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.RiteTargets;
import org.junit.jupiter.api.Test;

class RiteTellTest {
    /** The generic line is the one line an oracle refusal may say (R201): anything else passed as "generic" is a leak. */
    @Test
    void theGenericRefusalIsOnlyTheNoEffectLine() {
        assertDoesNotThrow(() -> RiteTell.refuseGeneric(null, RiteTell.NO_EFFECT));
        assertThrows(IllegalArgumentException.class, () -> RiteTell.refuseGeneric(null, "supernaturalbloodlines.refusal.original_cure"));
    }

    @Test
    void aRequestToSneakIsAPromptAndARefusalIsNot() {
        for (String prompt : new String[] {RiteTargets.SNEAK_TO_GIVE_BLOOD, RiteTargets.SNEAK_TO_ACCEPT, RiteTargets.SNEAK_TO_BE_MADE_AUGUSTINE,
                "supernaturalbloodlines.rite.sneak_to_evolve", "supernaturalbloodlines.ward.sneak_to_own"}) {
            assertTrue(RiteTargets.isPrompt(prompt), prompt);
        }
        assertTrue(!RiteTargets.isPrompt("supernaturalbloodlines.refusal.original_cure"));
        assertTrue(!RiteTargets.isPrompt(RiteTell.NO_EFFECT));
    }

    @Test
    void everyNightRefusalHasItsWholeLine() {
        for (var e : RiteTell.FULL_MOON_DETAIL.entrySet()) {
            assertTrue(e.getValue().equals(e.getKey() + ".detail"), e.getKey());
        }
        for (var e : RiteTell.ECLIPSE_DETAIL.entrySet()) {
            assertTrue(e.getValue().equals(e.getKey() + ".detail"), e.getKey());
        }
    }
}
