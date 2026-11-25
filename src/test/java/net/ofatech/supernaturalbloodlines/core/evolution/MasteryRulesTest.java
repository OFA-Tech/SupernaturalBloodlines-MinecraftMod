package net.ofatech.supernaturalbloodlines.core.evolution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.ofatech.supernaturalbloodlines.core.evolution.MasteryRules.Facts;
import net.ofatech.supernaturalbloodlines.core.evolution.MasteryRules.Reason;
import org.junit.jupiter.api.Test;

class MasteryRulesTest {
    private static Facts good(Tier tier) {
        return new Facts(true, true, true, tier == Tier.ULTIMATE, false, true, true, true, false, false, tier, tier, false, false, true, false);
    }

    @Test
    void aReadyRiteGoesAhead() {
        for (Tier t : Tier.values()) {
            assertEquals(Optional.empty(), MasteryRules.check(good(t)), t.name());
        }
    }

    @Test
    void theNightFollowsTheRiteTier() {
        Facts fullMoonOnly = new Facts(true, true, true, false, false, true, true, true, false, false, Tier.ULTIMATE, Tier.ULTIMATE, false, false, true, false);
        assertEquals(Optional.of(Reason.WRONG_NIGHT), MasteryRules.check(fullMoonOnly));
        for (Tier t : List.of(Tier.COMMON, Tier.EXTRA, Tier.UNIQUE)) {
            Facts f = new Facts(true, true, true, false, false, true, true, true, false, false, t, t, false, false, true, false);
            assertEquals(Optional.empty(), MasteryRules.check(f), t.name());
        }
    }

    @Test
    void theOrderIsFixed() {
        Tier t = Tier.UNIQUE;
        // everything wrong at once, then fixed in the order the reasons come out
        boolean[] ok = new boolean[13];
        List<Reason> seen = new ArrayList<>();
        for (int guard = 0; guard < 20; guard++) {
            Facts f = new Facts(ok[0], ok[1], ok[2], false, false, ok[3], ok[4], ok[5], !ok[6], !ok[7], ok[8] ? t : Tier.COMMON, t, !ok[9], !ok[10], ok[11], !ok[12]);
            Optional<Reason> r = MasteryRules.check(f);
            if (r.isEmpty()) {
                break;
            }
            seen.add(r.get());
            switch (r.get()) {
                case CLOSED -> ok[0] = true;
                case NOT_WITCH -> ok[1] = true;
                case WRONG_NIGHT -> ok[2] = true;
                case NO_CONSENT -> ok[3] = true;
                case NO_CHOICE -> ok[4] = true;
                case NOT_HELD -> ok[5] = true;
                case TEMPORARY -> ok[6] = true;
                case ALREADY -> ok[7] = true;
                case WRONG_TIER -> ok[8] = true;
                case SYSTEM -> ok[9] = true;
                case RETIRED -> ok[10] = true;
                case NO_EP -> ok[11] = true;
                case COOLDOWN -> ok[12] = true;
            }
        }
        assertEquals(List.of(Reason.CLOSED, Reason.NOT_WITCH, Reason.WRONG_NIGHT, Reason.NO_CONSENT, Reason.NO_CHOICE, Reason.NOT_HELD, Reason.TEMPORARY,
                Reason.ALREADY, Reason.WRONG_TIER, Reason.SYSTEM, Reason.RETIRED, Reason.NO_EP, Reason.COOLDOWN), seen);
    }

    @Test
    void aCasterWhoIsTheTargetNeedsNoConsent() {
        Facts self = new Facts(true, true, true, false, true, false, true, true, false, false, Tier.UNIQUE, Tier.UNIQUE, false, false, true, false);
        assertEquals(Optional.empty(), MasteryRules.check(self));
        Facts other = new Facts(true, true, true, false, false, false, true, true, false, false, Tier.UNIQUE, Tier.UNIQUE, false, false, true, false);
        assertEquals(Optional.of(Reason.NO_CONSENT), MasteryRules.check(other));
    }

    @Test
    void onlyTargetReasonsAreAboutTheTarget() {
        List<Reason> notAbout = List.of(Reason.CLOSED, Reason.NOT_WITCH, Reason.WRONG_NIGHT, Reason.NO_CONSENT, Reason.CIRCLE);
        for (Reason r : Reason.values()) {
            assertEquals(!notAbout.contains(r), MasteryRules.aboutTarget(r), r.name());
        }
    }

    @Test
    void keysAreOneToOne() {
        assertEquals("supernaturalbloodlines.refusal.mastery.no_ep", MasteryRules.keyOf(Reason.NO_EP, Tier.UNIQUE));
        assertEquals("supernaturalbloodlines.refusal.mastery.wrong_tier", MasteryRules.keyOf(Reason.WRONG_TIER, Tier.UNIQUE));
        assertEquals("supernaturalbloodlines.rite.needs_eclipse", MasteryRules.keyOf(Reason.WRONG_NIGHT, Tier.ULTIMATE));
        assertEquals("supernaturalbloodlines.rite.sneak_to_master", MasteryRules.keyOf(Reason.NO_CONSENT, Tier.EXTRA));
        List<String> keys = new ArrayList<>();
        for (Reason r : Reason.values()) {
            keys.add(MasteryRules.keyOf(r, Tier.UNIQUE));
        }
        assertEquals(keys.size(), keys.stream().distinct().count());
    }

    @Test
    void aCostOfZeroIsFreeAndAnyOtherCostNeedsStrictlyMore() {
        assertTrue(MasteryRules.enoughEp(0, 0), "a free rite for someone with no EP");
        assertTrue(MasteryRules.enoughEp(5, -1));
        assertTrue(MasteryRules.enoughEp(101, 100));
        assertFalse(MasteryRules.enoughEp(100, 100), "exactly the cost leaves nothing");
        assertFalse(MasteryRules.enoughEp(0, 1));
    }

    @Test
    void theMasteryPointsAreFarMoreThanAnySkillNeeds() {
        // the cap is addMasteryPoint's own: min(current + points, max); a negative start and an odd max must still land on the max
        for (double current : new double[] {-1000, -1, 0, 50, 999}) {
            for (int max : new int[] {1, 100, 1000, 100_000}) {
                assertEquals(max, Math.min(current + MasteryRules.POINTS, max), current + " / " + max);
            }
        }
    }

    @Test
    void theCooldownIsWholeDaysFromTheLastRite() {
        assertFalse(MasteryRules.onCooldown(-1, 100, 7), "never performed");
        assertTrue(MasteryRules.onCooldown(1000, 1000 + 24000L * 6, 7));
        assertFalse(MasteryRules.onCooldown(1000, 1000 + 24000L * 7, 7));
        assertEquals(1, MasteryRules.daysLeft(1000, 1000 + 24000L * 6, 7));
        assertEquals(7, MasteryRules.daysLeft(1000, 1000, 7));
    }

    @Test
    void whatCannotBeChosenAtAll() {
        assertEquals(Optional.of(Reason.TEMPORARY), MasteryRules.unchoosable(true, true, true, true));
        assertEquals(Optional.of(Reason.ALREADY), MasteryRules.unchoosable(false, true, true, true));
        assertEquals(Optional.of(Reason.SYSTEM), MasteryRules.unchoosable(false, false, true, true));
        assertEquals(Optional.of(Reason.RETIRED), MasteryRules.unchoosable(false, false, false, true));
        assertEquals(Optional.empty(), MasteryRules.unchoosable(false, false, false, false));
    }

    @Test
    void theCircleGateIsForTheUltimateOnlyAndWaivable() {
        assertFalse(MasteryRules.circleOk(Tier.ULTIMATE, 6, 7, false));
        assertTrue(MasteryRules.circleOk(Tier.ULTIMATE, 7, 7, false));
        assertTrue(MasteryRules.circleOk(Tier.ULTIMATE, 0, 7, true));
        assertTrue(MasteryRules.circleOk(Tier.ULTIMATE, 0, 0, false));
        for (Tier lower : new Tier[] {Tier.COMMON, Tier.EXTRA, Tier.UNIQUE}) {
            assertTrue(MasteryRules.circleOk(lower, 0, 7, false));
        }
    }}
