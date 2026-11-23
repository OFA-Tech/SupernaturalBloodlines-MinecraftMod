package net.ofatech.supernaturalbloodlines.core.wheel.overlap;

import static net.ofatech.supernaturalbloodlines.core.wheel.overlap.SharedCooldown.NOT_OWNED;
import static net.ofatech.supernaturalbloodlines.core.wheel.overlap.SharedCooldown.pick;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SharedCooldownTest {
    @Test
    void theMainSidesOwnCooldownWhateverSideWasUsed() {
        assertEquals(600, pick(new int[] {600, 200}, 0, 1));
        assertEquals(600, pick(new int[] {600, 200}, 0, 0));
        assertEquals(600, pick(new int[] {600, NOT_OWNED, 200}, 0, 2));
    }

    @Test
    void aMainSideWithNoCooldownMeansNoGroupCooldown() {
        assertEquals(0, pick(new int[] {0, 200}, 0, 1), "the owner's rule: the main side's own, and it has none");
    }

    @Test
    void mainNotOwnedFallsBackToTheFirstMemberTheyHaveInFileOrder() {
        assertEquals(300, pick(new int[] {NOT_OWNED, 300, 200}, 0, 2), "not the used side's 200: the first they have, like the entry the wheel shows");
        assertEquals(200, pick(new int[] {NOT_OWNED, NOT_OWNED, 200}, 0, 2));
        assertEquals(300, pick(new int[] {NOT_OWNED, 300}, 0, 1));
    }

    @Test
    void mainNotOwnedAndTheFirstOwnedHasNoneUsesTheUsedSidesOwn() {
        // the shared cooldown is never 0 while the side that was used has one
        assertEquals(200, pick(new int[] {NOT_OWNED, 0, 200}, 0, 2));
        assertEquals(0, pick(new int[] {NOT_OWNED, 0, 0}, 0, 2));
        assertEquals(0, pick(new int[] {NOT_OWNED, 0, 200}, 0, 1), "the used side has none either");
    }

    @Test
    void hybridFactionSwitch() {
        // a hybrid who is now a werewolf: Vampirism's rage (the main) is not unlocked in the current faction, the werewolf's rage is the one they have
        int[] asWerewolf = {NOT_OWNED, 400};
        assertEquals(400, pick(asWerewolf, 0, 1));
        // and switched back: the main is theirs again and speaks for the group
        int[] asVampire = {900, 400};
        assertEquals(900, pick(asVampire, 0, 1));
        assertEquals(900, pick(asVampire, 0, 0));
    }

    @Test
    void nothingOwnedFallsBackToTheUsedSide() {
        assertEquals(0, pick(new int[] {NOT_OWNED, NOT_OWNED}, 0, 1));
        assertEquals(0, pick(new int[] {}, 0, 0));
    }

    @Test
    void aTensuraSideThatIsNotTheUsedOneIsUnknownNotZero() {
        // the main side is a Tensura skill that was not the one pressed: its cooldown is not readable (NOT_OWNED), so the used side's own is what the group gets
        assertEquals(300, pick(new int[] {NOT_OWNED, 300}, 0, 1));
        // the main side is the one pressed: its own, read from the instance after the press
        assertEquals(700, pick(new int[] {700, NOT_OWNED}, 0, 0));
    }

    @Test
    void outOfRangeIndexesNeverThrow() {
        assertEquals(0, pick(new int[] {NOT_OWNED, 0}, 5, 9));
        assertEquals(50, pick(new int[] {50, 10}, 7, -1));
    }
}
