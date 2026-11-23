package net.ofatech.supernaturalbloodlines.core.evolution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.ofatech.supernaturalbloodlines.core.evolution.EvolutionRules.Facts;
import net.ofatech.supernaturalbloodlines.core.evolution.EvolutionRules.Reason;
import org.junit.jupiter.api.Test;

class EvolutionRulesTest {
    private static EvolutionRecipe recipe(Tier tier) {
        return new EvolutionRecipe("x", tier, "tensura:great_sage", GrantForm.DEFAULT, List.of(), List.of(), "tensura", "own.sage_great_sage", Set.of(), 0);
    }

    /** Every check passes; a test breaks one thing at a time. */
    private static Facts good() {
        return new Facts(true, true, true, true, true, true, false, false, true, false, true, true, true, false, true);
    }

    private static Facts with(Facts f, java.util.function.UnaryOperator<Builder> change) {
        return change.apply(new Builder(f)).build();
    }

    private static final class Builder {
        boolean enabled, modLoaded, probeFound, roadOpen, casterWitch, fullMoonNight, eclipse, casterIsTarget, targetSneaking, holdsGrant, kindOk, holdsNeeds,
                probeSatisfied, ownedByOther, enoughEp;

        Builder(Facts f) {
            enabled = f.enabled(); modLoaded = f.modLoaded(); probeFound = f.probeFound(); roadOpen = f.roadOpen(); casterWitch = f.casterWitch();
            fullMoonNight = f.fullMoonNight(); eclipse = f.eclipse(); casterIsTarget = f.casterIsTarget(); targetSneaking = f.targetSneaking();
            holdsGrant = f.holdsGrant(); kindOk = f.kindOk(); holdsNeeds = f.holdsNeeds(); probeSatisfied = f.probeSatisfied();
            ownedByOther = f.ownedByOther(); enoughEp = f.enoughEp();
        }

        Facts build() {
            return new Facts(enabled, modLoaded, probeFound, roadOpen, casterWitch, fullMoonNight, eclipse, casterIsTarget, targetSneaking, holdsGrant,
                    kindOk, holdsNeeds, probeSatisfied, ownedByOther, enoughEp);
        }
    }

    @Test
    void aFullyReadyRiteGoesAhead() {
        assertEquals(Optional.empty(), EvolutionRules.check(recipe(Tier.UNIQUE), good()));
    }

    @Test
    void anUltimateNeedsTheEclipse() {
        Facts fullMoonOnly = with(good(), b -> { b.fullMoonNight = true; b.eclipse = false; return b; });
        assertEquals(Optional.of(Reason.WRONG_NIGHT), EvolutionRules.check(recipe(Tier.ULTIMATE), fullMoonOnly));
        Facts eclipse = with(good(), b -> { b.fullMoonNight = true; b.eclipse = true; return b; });
        assertEquals(Optional.empty(), EvolutionRules.check(recipe(Tier.ULTIMATE), eclipse));
    }

    @Test
    void aUniqueNeedsAFullMoonNightAndAnEclipseCounts() {
        Facts noMoon = with(good(), b -> { b.fullMoonNight = false; b.eclipse = false; return b; });
        assertEquals(Optional.of(Reason.WRONG_NIGHT), EvolutionRules.check(recipe(Tier.UNIQUE), noMoon));
        Facts eclipseOnly = with(good(), b -> { b.fullMoonNight = false; b.eclipse = true; return b; });
        assertEquals(Optional.empty(), EvolutionRules.check(recipe(Tier.UNIQUE), eclipseOnly));
    }

    @Test
    void theChecksRunInTheSpecOrder() {
        Facts all = new Facts(false, false, false, false, false, false, false, false, false, true, false, false, false, true, false);
        List<Reason> seen = new java.util.ArrayList<>();
        EvolutionRecipe r = recipe(Tier.UNIQUE);
        Optional<Reason> reason;
        Builder b = new Builder(all);
        while ((reason = EvolutionRules.check(r, b.build())).isPresent() && seen.size() < 12) {
            seen.add(reason.get());
            switch (reason.get()) {
                case CLOSED -> { b.enabled = true; b.modLoaded = true; b.probeFound = true; b.roadOpen = true; }
                case NOT_WITCH -> b.casterWitch = true;
                case WRONG_NIGHT -> b.fullMoonNight = true;
                case NO_CONSENT -> b.targetSneaking = true;
                case ALREADY -> b.holdsGrant = false;
                case NOT_READY -> { b.kindOk = true; b.holdsNeeds = true; b.probeSatisfied = true; b.ownedByOther = false; b.enoughEp = true; }
            }
        }
        assertEquals(List.of(Reason.CLOSED, Reason.NOT_WITCH, Reason.WRONG_NIGHT, Reason.NO_CONSENT, Reason.ALREADY, Reason.NOT_READY), seen);
    }

    @Test
    void aCasterWhoIsTheTargetIsNotAskedForConsent() {
        Facts self = with(good(), b -> { b.casterIsTarget = true; b.targetSneaking = false; return b; });
        assertEquals(Optional.empty(), EvolutionRules.check(recipe(Tier.UNIQUE), self));
        Facts other = with(good(), b -> { b.casterIsTarget = false; b.targetSneaking = false; return b; });
        assertEquals(Optional.of(Reason.NO_CONSENT), EvolutionRules.check(recipe(Tier.UNIQUE), other));
    }

    @Test
    void everyTargetRequirementGivesTheSameAnswer() {
        List<Facts> broken = List.of(
                with(good(), b -> { b.kindOk = false; return b; }),
                with(good(), b -> { b.holdsNeeds = false; return b; }),
                with(good(), b -> { b.probeSatisfied = false; return b; }),
                with(good(), b -> { b.ownedByOther = true; return b; }),
                with(good(), b -> { b.enoughEp = false; return b; }));
        for (Facts f : broken) {
            Reason r = EvolutionRules.check(recipe(Tier.UNIQUE), f).orElseThrow();
            assertTrue("NOT_READY".equals(r.name()), "a target requirement gave " + r + ", which tells the performer something else");
            assertTrue("supernaturalbloodlines.refusal.evolution.not_ready".equals(EvolutionRules.keyOf(r, Tier.UNIQUE)), "the key differs for " + r);
        }
    }

    @Test
    void aMissingAddonSwitchedOffOrClosedRoadIsClosed() {
        for (java.util.function.UnaryOperator<Builder> off : List.<java.util.function.UnaryOperator<Builder>>of(
                b -> { b.enabled = false; return b; }, b -> { b.modLoaded = false; return b; }, b -> { b.probeFound = false; return b; },
                b -> { b.roadOpen = false; return b; })) {
            assertEquals(Optional.of(Reason.CLOSED), EvolutionRules.check(recipe(Tier.UNIQUE), with(good(), off)));
        }
    }

    @Test
    void onlyTheTargetReasonsAreAboutTheTarget() {
        for (Reason r : Reason.values()) {
            assertEquals(r == Reason.ALREADY || r == Reason.NOT_READY, EvolutionRules.aboutTarget(r), r.name());
        }
    }

    @Test
    void theNightKeyFollowsTheTier() {
        assertEquals("supernaturalbloodlines.rite.needs_eclipse", EvolutionRules.keyOf(Reason.WRONG_NIGHT, Tier.ULTIMATE));
        assertEquals("supernaturalbloodlines.rite.needs_full_moon", EvolutionRules.keyOf(Reason.WRONG_NIGHT, Tier.UNIQUE));
    }

    @Test
    void aRecipeCopiesItsLists() {
        assertFalse(recipe(Tier.UNIQUE).consumes() == null);
        assertTrue(recipe(Tier.UNIQUE).consumes().isEmpty());
    }
}
