package net.ofatech.supernaturalbloodlines.core.bloodline;

import net.ofatech.supernaturalbloodlines.core.bloodline.Outcome.Action;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineEvent.*;
import static org.junit.jupiter.api.Assertions.*;

class BloodlineRulesTest {
    private static final UUID U = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final String REFUSAL = "supernaturalbloodlines.refusal.";

    private static BloodlineContext ctx() {
        return BloodlineContext.defaults();
    }

    private static BloodlineState st(Species species) {
        return BloodlineState.NONE.toBuilder().species(species).build();
    }

    private static BloodlineState gene(BloodlineState s) {
        return s.toBuilder().wolf(s.wolf().toBuilder().geneRolled(true).latentGene(true).build()).build();
    }

    private static Outcome run(BloodlineState s, BloodlineEvent e, BloodlineContext c) {
        return BloodlineRules.apply(s, e, c);
    }

    private static void assertRefused(Outcome o, BloodlineState unchanged, String reason) {
        assertTrue(o.refused());
        assertEquals(REFUSAL + reason, o.refusalKey());
        assertEquals(unchanged, o.state());
        assertTrue(o.actions().isEmpty());
    }

    private static void assertNoChange(Outcome o, BloodlineState s) {
        assertFalse(o.refused());
        assertEquals(s, o.state());
        assertTrue(o.actions().isEmpty());
    }

    // ---- Immortality ----

    @Test
    void immortalityMakesOriginal() {
        Outcome o = run(BloodlineState.NONE, RITE_OF_IMMORTALITY, ctx());
        assertFalse(o.refused());
        assertEquals(Species.ORIGINAL, o.state().species());
        assertEquals(List.of(Action.JOIN_VAMPIRE), o.actions());
    }

    @Test
    void refusalImmortalityNotHumanLike() {
        assertRefused(run(BloodlineState.NONE, RITE_OF_IMMORTALITY, ctx().withHumanLike(false)), BloodlineState.NONE, "not_human_like");
    }

    @Test
    void refusalImmortalityHasFaction() {
        assertRefused(run(BloodlineState.NONE, RITE_OF_IMMORTALITY, ctx().withFaction(BaseFaction.WEREWOLF)), BloodlineState.NONE, "has_faction");
    }

    @Test
    void refusalImmortalityCapReached() {
        assertRefused(run(BloodlineState.NONE, RITE_OF_IMMORTALITY, ctx().withMaxOriginals(1).withOriginalsInWorld(1)), BloodlineState.NONE, "original_cap");
    }

    @Test
    void immortalityBelowCapIsAllowed() {
        assertEquals(Species.ORIGINAL, run(BloodlineState.NONE, RITE_OF_IMMORTALITY, ctx().withMaxOriginals(2).withOriginalsInWorld(1)).state().species());
    }

    @Test
    void refusalImmortalityAlreadyHasSpecies() {
        BloodlineState witch = st(Species.WITCH);
        assertRefused(run(witch, RITE_OF_IMMORTALITY, ctx()), witch, "has_species");
    }

    // ---- Hybrid transition ----

    @Test
    void werewolfDiesWithOriginalBloodStartsTransition() {
        Outcome o = run(BloodlineState.NONE, DIED_WITH_ORIGINAL_BLOOD, ctx().withFaction(BaseFaction.WEREWOLF).withBloodSource(U));
        assertTrue(o.state().inTransition());
        assertEquals(24000, o.state().transitionTicksLeft());
        assertEquals(U, o.state().pendingSire());
        assertEquals(Species.NONE, o.state().species());
    }

    @Test
    void nonWerewolfDyingWithOriginalBloodIsUnaffected() {
        assertNoChange(run(BloodlineState.NONE, DIED_WITH_ORIGINAL_BLOOD, ctx().withBloodSource(U)), BloodlineState.NONE);
    }

    @Test
    void siphonerCannotStartHybridTransition() {
        BloodlineState s = st(Species.SIPHONER);
        assertRefused(run(s, DIED_WITH_ORIGINAL_BLOOD, ctx().withFaction(BaseFaction.WEREWOLF).withBloodSource(U)), s, "siphoner_hybrid");
    }

    @Test
    void doppelgangerBloodCompletesHybrid() {
        BloodlineState s = BloodlineState.NONE.toBuilder().inTransition(true).transitionTicksLeft(100).pendingSire(U).build();
        Outcome o = run(s, DRANK_DOPPELGANGER_BLOOD, ctx().withGameTime(900));
        assertEquals(Species.HYBRID, o.state().species());
        assertEquals(U, o.state().sire());
        assertFalse(o.state().inTransition());
        assertNull(o.state().pendingSire());
        assertEquals(0, o.state().transitionTicksLeft());
        assertFalse(o.state().hybridCurseBound());
        assertEquals(900, o.state().turnedAt());
    }

    @Test
    void witchCompletingTransitionBecomesTribrid() {
        BloodlineState s = st(Species.WITCH).toBuilder().witchLayer(true).inTransition(true).pendingSire(U).build();
        Outcome o = run(s, DRANK_DOPPELGANGER_BLOOD, ctx());
        assertEquals(Species.TRIBRID, o.state().species());
        assertTrue(o.state().witchLayer());
        assertEquals(U, o.state().sire());
    }

    @Test
    void doppelgangerBloodOutsideTransitionDoesNothing() {
        assertNoChange(run(BloodlineState.NONE, DRANK_DOPPELGANGER_BLOOD, ctx()), BloodlineState.NONE);
    }

    @Test
    void doppelgangerBloodCannotRescueUnstable() {
        BloodlineState s = BloodlineState.NONE.toBuilder().inTransition(true).unstable(true).pendingSire(U).build();
        assertNoChange(run(s, DRANK_DOPPELGANGER_BLOOD, ctx()), s);
    }

    @Test
    void otherBloodMakesUnstable() {
        BloodlineState s = BloodlineState.NONE.toBuilder().inTransition(true).pendingSire(U).build();
        Outcome o = run(s, DRANK_OTHER_BLOOD, ctx());
        assertTrue(o.state().unstable());
        assertTrue(o.state().inTransition());
    }

    @Test
    void otherBloodOutsideTransitionDoesNothing() {
        assertNoChange(run(BloodlineState.NONE, DRANK_OTHER_BLOOD, ctx()), BloodlineState.NONE);
    }

    @Test
    void unstableDeathRevertsToWerewolf() {
        BloodlineState s = st(Species.WITCH).toBuilder().witchLayer(true).inTransition(true).unstable(true)
                .transitionTicksLeft(5).pendingSire(U).build();
        Outcome o = run(s, UNSTABLE_DEATH, ctx());
        assertEquals(Species.WITCH, o.state().species());
        assertTrue(o.state().witchLayer());
        assertFalse(o.state().inTransition());
        assertFalse(o.state().unstable());
        assertNull(o.state().pendingSire());
        assertEquals(0, o.state().transitionTicksLeft());
        assertNull(o.state().sire());
    }

    @Test
    void timeoutKillsAndReverts() {
        BloodlineState s = BloodlineState.NONE.toBuilder().inTransition(true).pendingSire(U).build();
        Outcome o = run(s, TRANSITION_TIMEOUT, ctx());
        assertEquals(List.of(Action.KILL_PLAYER), o.actions());
        assertFalse(o.state().inTransition());
        assertNull(o.state().pendingSire());
        assertEquals(Species.NONE, o.state().species());
    }

    @Test
    void timeoutOutsideTransitionDoesNothing() {
        assertNoChange(run(BloodlineState.NONE, TRANSITION_TIMEOUT, ctx()), BloodlineState.NONE);
    }

    // ---- Joining vampires ----

    @Test
    void witchJoiningVampireLosesMagic() {
        BloodlineState s = st(Species.WITCH).toBuilder().witchLayer(true).build();
        Outcome o = run(s, JOINED_VAMPIRE, ctx());
        assertEquals(Species.NONE, o.state().species());
        assertFalse(o.state().witchLayer());
    }

    @Test
    void siphonerJoiningVampireBecomesHeretic() {
        assertEquals(Species.HERETIC, run(st(Species.SIPHONER), JOINED_VAMPIRE, ctx()).state().species());
    }

    @Test
    void plainHumanJoiningVampireKeepsNoSpecies() {
        assertNoChange(run(BloodlineState.NONE, JOINED_VAMPIRE, ctx()), BloodlineState.NONE);
    }

    // ---- Rite of the Moon / lineage / gene ----

    @Test
    void originalRiteOfMoonNeedsGene() {
        Outcome o = run(gene(st(Species.ORIGINAL)), RITE_OF_THE_MOON, ctx());
        assertEquals(Species.ORIGINAL_HYBRID, o.state().species());
        assertFalse(o.state().hybridCurseBound());
        assertEquals(List.of(Action.LEAVE_FACTION, Action.JOIN_WEREWOLF), o.actions());
        assertRefused(run(st(Species.ORIGINAL), RITE_OF_THE_MOON, ctx()), st(Species.ORIGINAL), "no_gene");
    }

    @Test
    void lineageAwakenOnOriginalMakesBoundOriginalHybrid() {
        Outcome o = run(st(Species.ORIGINAL), RITE_OF_LINEAGE_AWAKEN, ctx().withFaction(BaseFaction.VAMPIRE));
        assertEquals(Species.ORIGINAL_HYBRID, o.state().species());
        assertTrue(o.state().hybridCurseBound());
        assertEquals(List.of(Action.LEAVE_FACTION, Action.JOIN_WEREWOLF), o.actions());
    }

    @Test
    void lineageAwakenOnPlainVampireMakesBoundHybrid() {
        BloodlineState s = BloodlineState.NONE.toBuilder().sire(U).build();
        Outcome o = run(s, RITE_OF_LINEAGE_AWAKEN, ctx().withFaction(BaseFaction.VAMPIRE));
        assertEquals(Species.HYBRID, o.state().species());
        assertTrue(o.state().hybridCurseBound());
        assertEquals(U, o.state().sire());
        assertEquals(List.of(Action.LEAVE_FACTION, Action.JOIN_WEREWOLF), o.actions());
    }

    @Test
    void lineageAwakenWithoutFactionChangesNoSpecies() {
        assertNoChange(run(BloodlineState.NONE, RITE_OF_LINEAGE_AWAKEN, ctx()), BloodlineState.NONE);
    }

    @Test
    void vampireGeneAwakensBoundHybrid() {
        BloodlineState s = gene(BloodlineState.NONE.toBuilder().sire(U).build());
        Outcome o = run(s, GENE_AWAKENED_AS_VAMPIRE, ctx().withFaction(BaseFaction.VAMPIRE).withGameTime(77));
        assertEquals(Species.HYBRID, o.state().species());
        assertTrue(o.state().hybridCurseBound());
        assertEquals(U, o.state().sire());
        assertEquals(77, o.state().turnedAt());
        assertEquals(List.of(Action.LEAVE_FACTION, Action.JOIN_WEREWOLF), o.actions());
    }

    @Test
    void originalGeneAwakensBoundOriginalHybrid() {
        Outcome o = run(gene(st(Species.ORIGINAL)), GENE_AWAKENED_AS_VAMPIRE, ctx().withFaction(BaseFaction.VAMPIRE));
        assertEquals(Species.ORIGINAL_HYBRID, o.state().species());
        assertTrue(o.state().hybridCurseBound());
    }

    @Test
    void geneAwakeningMakesTheWolfLayerBornOmega() {
        BloodlineContext vampire = ctx().withFaction(BaseFaction.VAMPIRE);
        for (Species sp : List.of(Species.NONE, Species.ORIGINAL)) {
            WolfState w = run(gene(st(sp)), GENE_AWAKENED_AS_VAMPIRE, vampire).state().wolf();
            assertEquals(WolfState.Lineage.BORN, w.lineage(), sp.name());
            assertEquals(WolfState.Rank.OMEGA, w.rank(), sp.name());
            assertTrue(w.latentGene());
        }
    }

    @Test
    void lineageAwakeningMakesTheWolfLayerBornOmegaAndGivesTheGene() {
        WolfState w = run(st(Species.ORIGINAL), RITE_OF_LINEAGE_AWAKEN, ctx().withFaction(BaseFaction.VAMPIRE)).state().wolf();
        assertEquals(WolfState.Lineage.BORN, w.lineage());
        assertEquals(WolfState.Rank.OMEGA, w.rank());
        assertTrue(w.latentGene());
        assertTrue(w.geneRolled());
    }

    @Test
    void refusalGeneMissing() {
        assertRefused(run(BloodlineState.NONE, GENE_AWAKENED_AS_VAMPIRE, ctx().withFaction(BaseFaction.VAMPIRE)), BloodlineState.NONE, "no_gene");
        assertRefused(run(st(Species.ORIGINAL), GENE_AWAKENED_AS_VAMPIRE, ctx().withFaction(BaseFaction.VAMPIRE)), st(Species.ORIGINAL), "no_gene");
    }

    @Test
    void hereticCannotBecomeHybrid() {
        BloodlineState s = st(Species.HERETIC);
        assertRefused(run(gene(s), GENE_AWAKENED_AS_VAMPIRE, ctx().withFaction(BaseFaction.VAMPIRE)), gene(s), "siphoner_hybrid");
        assertRefused(run(s, RITE_OF_LINEAGE_AWAKEN, ctx().withFaction(BaseFaction.VAMPIRE)), s, "siphoner_hybrid");
    }

    @Test
    void geneOnHumanDoesNotMakeHybrid() {
        assertNoChange(run(gene(BloodlineState.NONE), GENE_AWAKENED_AS_VAMPIRE, ctx()), gene(BloodlineState.NONE));
    }

    @Test
    void moonRiteUnbindsCurse() {
        for (Species sp : List.of(Species.HYBRID, Species.ORIGINAL_HYBRID)) {
            BloodlineState s = st(sp).toBuilder().hybridCurseBound(true).build();
            Outcome o = run(s, RITE_OF_THE_MOON, ctx());
            assertFalse(o.state().hybridCurseBound());
            assertEquals(sp, o.state().species());
            assertFalse(o.refused());
            assertTrue(o.actions().isEmpty());
        }
    }

    @Test
    void moonRiteIsRefusedWhereItDoesNothing() {
        BloodlineState vampire = st(Species.NONE);
        assertRefused(run(vampire, RITE_OF_THE_MOON, ctx().withFaction(BaseFaction.VAMPIRE)), vampire, "no_gene");
        assertRefused(run(gene(vampire), RITE_OF_THE_MOON, ctx().withFaction(BaseFaction.VAMPIRE)), gene(vampire), "not_original");
        assertRefused(run(st(Species.HYBRID), RITE_OF_THE_MOON, ctx()), st(Species.HYBRID), "no_gene");
        assertRefused(run(gene(st(Species.HYBRID)), RITE_OF_THE_MOON, ctx()), gene(st(Species.HYBRID)), "not_original");
    }

    // ---- Cures ----

    @Test
    void cleansingHybridLeavesWerewolf() {
        BloodlineState s = st(Species.HYBRID).toBuilder().sire(U).hybridCurseBound(true).build();
        Outcome o = run(s, RITE_OF_CLEANSING_BLOOD, ctx().withFaction(BaseFaction.WEREWOLF));
        assertEquals(Species.NONE, o.state().species());
        assertNull(o.state().sire());
        assertFalse(o.state().hybridCurseBound());
        assertTrue(o.actions().isEmpty());
    }

    @Test
    void cleansingTribridKeepsWitch() {
        BloodlineState s = st(Species.TRIBRID).toBuilder().witchLayer(true).sire(U).build();
        Outcome o = run(s, RITE_OF_CLEANSING_BLOOD, ctx().withFaction(BaseFaction.WEREWOLF));
        assertEquals(Species.WITCH, o.state().species());
        assertTrue(o.state().witchLayer());
        assertNull(o.state().sire());
    }

    @Test
    void cleansingPlainVampireLeavesFaction() {
        Outcome o = run(BloodlineState.NONE, RITE_OF_CLEANSING_BLOOD, ctx().withFaction(BaseFaction.VAMPIRE));
        assertEquals(List.of(Action.LEAVE_FACTION), o.actions());
    }

    @Test
    void cleansingHereticBecomesSiphoner() {
        Outcome o = run(st(Species.HERETIC), RITE_OF_CLEANSING_BLOOD, ctx().withFaction(BaseFaction.VAMPIRE));
        assertEquals(Species.SIPHONER, o.state().species());
        assertEquals(List.of(Action.LEAVE_FACTION), o.actions());
    }

    private static final WolfState ALPHA_WOLF = new WolfState(WolfState.Lineage.BITTEN, WolfState.Rank.ALPHA, true, true,
            true, U, 400L, 55, true, java.util.Set.of("skill"));

    private static BloodlineState inTransitionWerewolf(Species sp) {
        return st(sp).toBuilder().witchLayer(sp == Species.WITCH).inTransition(true).transitionTicksLeft(500)
                .pendingSire(U).wolf(ALPHA_WOLF).build();
    }

    private static void assertWolfLayerGone(BloodlineState s) {
        assertEquals(WolfState.Lineage.NONE, s.wolf().lineage());
        assertEquals(WolfState.Rank.NONE, s.wolf().rank());
        assertNull(s.wolf().packAlpha());
        assertEquals(0, s.wolf().betaSince());
        assertEquals(0, s.wolf().defenderKills());
        assertFalse(s.wolf().stoleSpark());
        assertFalse(s.wolf().killedInnocent());
        assertTrue(s.wolf().grantedSkills().isEmpty());
        assertTrue(s.wolf().geneRolled());
        assertTrue(s.wolf().latentGene());
    }

    private static void assertTransitionGone(BloodlineState s) {
        assertFalse(s.inTransition());
        assertFalse(s.unstable());
        assertEquals(0, s.transitionTicksLeft());
        assertNull(s.pendingSire());
    }

    @Test
    void silverMoonMakesAHybridAPlainVampire() {
        BloodlineState s = inTransitionWerewolf(Species.HYBRID).toBuilder().sire(U).hybridCurseBound(true).build();
        Outcome o = run(s, RITE_OF_THE_SILVER_MOON, ctx().withFaction(BaseFaction.WEREWOLF));
        assertEquals(Species.NONE, o.state().species());
        assertEquals(U, o.state().sire());
        assertFalse(o.state().hybridCurseBound());
        assertEquals(List.of(Action.LEAVE_FACTION, Action.JOIN_VAMPIRE), o.actions());
        assertWolfLayerGone(o.state());
        assertTransitionGone(o.state());
    }

    @Test
    void silverMoonMakesATribridAPlainVampireWithoutMagic() {
        BloodlineState s = inTransitionWerewolf(Species.TRIBRID).toBuilder().witchLayer(true).sire(U).build();
        Outcome o = run(s, RITE_OF_THE_SILVER_MOON, ctx().withFaction(BaseFaction.WEREWOLF));
        assertEquals(Species.NONE, o.state().species());
        assertFalse(o.state().witchLayer());
        assertEquals(U, o.state().sire());
        assertEquals(List.of(Action.LEAVE_FACTION, Action.JOIN_VAMPIRE), o.actions());
        assertWolfLayerGone(o.state());
    }

    @Test
    void silverMoonOnWerewolfInTransitionClearsTransitionAndWolfLayer() {
        for (Species sp : List.of(Species.NONE, Species.WITCH, Species.SIPHONER)) {
            Outcome o = run(inTransitionWerewolf(sp), RITE_OF_THE_SILVER_MOON, ctx().withFaction(BaseFaction.WEREWOLF));
            assertEquals(sp, o.state().species());
            assertEquals(List.of(Action.LEAVE_FACTION), o.actions());
            assertTransitionGone(o.state());
            assertWolfLayerGone(o.state());
        }
        Outcome o = run(inTransitionWerewolf(Species.NONE), RITE_OF_THE_SILVER_MOON, ctx().withFaction(BaseFaction.WEREWOLF));
        assertNoChange(run(o.state(), DRANK_DOPPELGANGER_BLOOD, ctx()), o.state());
        assertNoChange(run(o.state(), TRANSITION_TIMEOUT, ctx()), o.state());
    }

    @Test
    void silverMoonOnPlainHumanOrVampireDoesNothing() {
        assertNoChange(run(BloodlineState.NONE, RITE_OF_THE_SILVER_MOON, ctx()), BloodlineState.NONE);
        assertNoChange(run(BloodlineState.NONE, RITE_OF_THE_SILVER_MOON, ctx().withFaction(BaseFaction.VAMPIRE)), BloodlineState.NONE);
    }

    @Test
    void refusalOriginalCannotBeCured() {
        for (Species sp : List.of(Species.ORIGINAL, Species.ORIGINAL_HYBRID)) {
            for (BloodlineEvent e : List.of(RITE_OF_CLEANSING_BLOOD, RITE_OF_THE_SILVER_MOON)) {
                assertRefused(run(st(sp), e, ctx()), st(sp), "original_cure");
            }
        }
    }

    @Test
    void secondOriginalBloodNoEffectOnHybrid() {
        for (Species sp : List.of(Species.HYBRID, Species.ORIGINAL_HYBRID, Species.TRIBRID)) {
            assertNoChange(run(st(sp), DIED_WITH_ORIGINAL_BLOOD, ctx().withFaction(BaseFaction.WEREWOLF).withBloodSource(U)), st(sp));
        }
    }

    @Test
    void cureRemovesEverythingEvenOriginal() {
        BloodlineState s = st(Species.ORIGINAL_HYBRID).toBuilder().sire(U).hybridCurseBound(true).turnedAt(5)
                .markKills(3).hunterCurse(true).witchLayer(true).build();
        Outcome o = run(s, DRANK_THE_CURE, ctx().withFaction(BaseFaction.WEREWOLF));
        assertEquals(Species.NONE, o.state().species());
        assertNull(o.state().sire());
        assertFalse(o.state().hybridCurseBound());
        assertEquals(0, o.state().turnedAt());
        assertEquals(0, o.state().markKills());
        assertFalse(o.state().hunterCurse());
        assertFalse(o.state().witchLayer());
        assertEquals(List.of(Action.LEAVE_FACTION), o.actions());
    }

    @Test
    void cureKeepsTheHiddenGene() {
        WolfState wolf = WolfState.NONE.toBuilder().geneRolled(true).latentGene(true).build();
        BloodlineState s = st(Species.HYBRID).toBuilder().wolf(wolf).build();
        assertEquals(wolf, run(s, DRANK_THE_CURE, ctx().withFaction(BaseFaction.WEREWOLF)).state().wolf());
    }

    @Test
    void cureClearsTheTeenWolfLayerButKeepsTheGene() {
        BloodlineState s = BloodlineState.NONE.toBuilder().wolf(ALPHA_WOLF).build();
        Outcome o = run(s, DRANK_THE_CURE, ctx().withFaction(BaseFaction.WEREWOLF));
        assertWolfLayerGone(o.state());
        assertEquals(List.of(Action.LEAVE_FACTION), o.actions());
    }

    @Test
    void cureClearsATransitionInProgress() {
        Outcome o = run(inTransitionWerewolf(Species.NONE), DRANK_THE_CURE, ctx().withFaction(BaseFaction.WEREWOLF));
        assertTransitionGone(o.state());
        assertNoChange(run(o.state(), DRANK_DOPPELGANGER_BLOOD, ctx()), o.state());
        assertNoChange(run(o.state(), TRANSITION_TIMEOUT, ctx()), o.state());
    }

    @Test
    void cureResetsSiphonedPowerAndThirst() {
        BloodlineState s = st(Species.HERETIC).toBuilder().siphonedPower(300).thirst(3).build();
        Outcome o = run(s, DRANK_THE_CURE, ctx().withFaction(BaseFaction.VAMPIRE));
        assertEquals(0, o.state().siphonedPower());
        assertEquals(20, o.state().thirst());
    }

    @Test
    void cureOnPlainHumanDoesNothing() {
        assertNoChange(run(BloodlineState.NONE, DRANK_THE_CURE, ctx()), BloodlineState.NONE);
    }

    @Test
    void cureOnWitchWithoutFactionKeepsFaction() {
        Outcome o = run(st(Species.WITCH).toBuilder().witchLayer(true).build(), DRANK_THE_CURE, ctx());
        assertEquals(Species.NONE, o.state().species());
        assertFalse(o.state().witchLayer());
        assertTrue(o.actions().isEmpty());
    }

    // ---- Leaving the base faction / death ----

    @Test
    void leavingBaseFactionDropsLayer() {
        assertEquals(Species.NONE, run(st(Species.HYBRID).toBuilder().sire(U).build(), LEFT_BASE_FACTION, ctx()).state().species());
        assertNull(run(st(Species.HYBRID).toBuilder().sire(U).build(), LEFT_BASE_FACTION, ctx()).state().sire());
        assertEquals(Species.SIPHONER, run(st(Species.HERETIC), LEFT_BASE_FACTION, ctx()).state().species());
        assertEquals(Species.NONE, run(st(Species.ORIGINAL), LEFT_BASE_FACTION, ctx()).state().species());
        assertEquals(Species.WITCH, run(st(Species.TRIBRID).toBuilder().witchLayer(true).build(), LEFT_BASE_FACTION, ctx()).state().species());
        assertEquals(Species.NONE, run(st(Species.HUNTER_OF_THE_FIVE).toBuilder().markKills(3).build(), LEFT_BASE_FACTION, ctx()).state().species());
    }

    @Test
    void leavingBaseFactionOnWerewolfBasedStatesClearsWolfLayerAndTransition() {
        BloodlineContext werewolf = ctx().withFaction(BaseFaction.WEREWOLF);
        for (Species sp : List.of(Species.NONE, Species.WITCH, Species.SIPHONER, Species.HYBRID, Species.ORIGINAL_HYBRID, Species.TRIBRID)) {
            Outcome o = run(inTransitionWerewolf(sp), LEFT_BASE_FACTION, werewolf);
            assertWolfLayerGone(o.state());
            assertTransitionGone(o.state());
            assertTrue(o.actions().isEmpty());
        }
        Outcome plain = run(inTransitionWerewolf(Species.NONE), LEFT_BASE_FACTION, werewolf);
        assertNoChange(run(plain.state(), DRANK_DOPPELGANGER_BLOOD, ctx()), plain.state());
        assertNoChange(run(plain.state(), TRANSITION_TIMEOUT, ctx()), plain.state());
        assertEquals(Species.WITCH, run(inTransitionWerewolf(Species.WITCH), LEFT_BASE_FACTION, werewolf).state().species());
    }

    @Test
    void leavingBaseFactionAsPlainVampireKeepsTheWolfLayer() {
        BloodlineState s = BloodlineState.NONE.toBuilder().wolf(ALPHA_WOLF).build();
        assertNoChange(run(s, LEFT_BASE_FACTION, ctx().withFaction(BaseFaction.VAMPIRE)), s);
    }

    @Test
    void leavingBaseFactionKeepsWitchAndSiphoner() {
        assertNoChange(run(st(Species.WITCH), LEFT_BASE_FACTION, ctx()), st(Species.WITCH));
        assertNoChange(run(st(Species.SIPHONER), LEFT_BASE_FACTION, ctx()), st(Species.SIPHONER));
    }

    @Test
    void deathResetsSiphonedPower() {
        Outcome o = run(st(Species.SIPHONER).toBuilder().siphonedPower(50).build(), DIED, ctx());
        assertEquals(0, o.state().siphonedPower());
        assertEquals(Species.SIPHONER, o.state().species());
    }

    @Test
    void deathKeepsEverythingElse() {
        BloodlineState s = st(Species.HYBRID).toBuilder().sire(U).inTransition(true).markKills(2).build();
        assertNoChange(run(s, DIED, ctx()), s);
    }

    // ---- Witches and siphoners ----

    @Test
    void ancestralCallingMakesWitchOrSiphoner() {
        Outcome w = run(BloodlineState.NONE, ANCESTRAL_CALLING_WITCH, ctx().withGameTime(9));
        assertEquals(Species.WITCH, w.state().species());
        assertTrue(w.state().witchLayer());
        assertEquals(9, w.state().turnedAt());
        Outcome sip = run(BloodlineState.NONE, ANCESTRAL_CALLING_SIPHONER, ctx().withFaction(BaseFaction.WEREWOLF));
        assertEquals(Species.SIPHONER, sip.state().species());
        assertFalse(sip.state().witchLayer());
    }

    @Test
    void ancestralCallingOnlyNoFactionOrWerewolf() {
        for (BloodlineEvent e : List.of(ANCESTRAL_CALLING_WITCH, ANCESTRAL_CALLING_SIPHONER)) {
            assertRefused(run(BloodlineState.NONE, e, ctx().withFaction(BaseFaction.VAMPIRE)), BloodlineState.NONE, "vampire_no_magic");
            assertRefused(run(BloodlineState.NONE, e, ctx().withFaction(BaseFaction.HUNTER)), BloodlineState.NONE, "has_faction");
            assertRefused(run(BloodlineState.NONE, e, ctx().withHumanLike(false)), BloodlineState.NONE, "not_human_like");
        }
        assertRefused(run(st(Species.HYBRID), ANCESTRAL_CALLING_WITCH, ctx().withFaction(BaseFaction.WEREWOLF)), st(Species.HYBRID), "has_species");
    }

    // ---- Hunters of the Five ----

    @Test
    void riteOfTheFiveMarksHunter() {
        Outcome o = run(BloodlineState.NONE, RITE_OF_THE_FIVE, ctx().withFaction(BaseFaction.HUNTER).withFiveCount(4).withGameTime(300));
        assertEquals(Species.HUNTER_OF_THE_FIVE, o.state().species());
        assertEquals(0, o.state().markKills());
        assertEquals(300, o.state().lastVampireKill());
    }

    @Test
    void refusalFiveAlreadyHasSpecies() {
        BloodlineContext hunter = ctx().withFaction(BaseFaction.HUNTER);
        for (Species sp : List.of(Species.HUNTER_OF_THE_FIVE, Species.POTENTIAL)) {
            BloodlineState s = st(sp).toBuilder().markKills(4).build();
            assertRefused(run(s, RITE_OF_THE_FIVE, hunter), s, "has_species");
        }
    }

    @Test
    void refusalFiveFull() {
        assertRefused(run(BloodlineState.NONE, RITE_OF_THE_FIVE, ctx().withFaction(BaseFaction.HUNTER).withFiveCount(5)), BloodlineState.NONE, "five_full");
        assertRefused(run(BloodlineState.NONE, RITE_OF_THE_FIVE, ctx().withFaction(BaseFaction.VAMPIRE).withFiveCount(1)), BloodlineState.NONE, "not_hunter");
        assertRefused(run(BloodlineState.NONE, RITE_OF_THE_FIVE, ctx().withFaction(BaseFaction.HUNTER).withHumanLike(false)), BloodlineState.NONE, "not_human_like");
    }

    @Test
    void markGrowsOnPreyKill() {
        BloodlineState s = st(Species.HUNTER_OF_THE_FIVE).toBuilder().markKills(2).build();
        Outcome o = run(s, KILLED_PREY, ctx().withGameTime(1234));
        assertEquals(3, o.state().markKills());
        assertEquals(1234, o.state().lastVampireKill());
        assertEquals(Species.HUNTER_OF_THE_FIVE, o.state().species());
    }

    @Test
    void potentialActivatesOnFirstKill() {
        Outcome o = run(st(Species.POTENTIAL), KILLED_PREY, ctx().withGameTime(50));
        assertEquals(Species.HUNTER_OF_THE_FIVE, o.state().species());
        assertEquals(1, o.state().markKills());
        assertEquals(50, o.state().lastVampireKill());
        assertEquals(50, o.state().turnedAt());
    }

    @Test
    void preyKillByUnmarkedPlayerDoesNothing() {
        assertNoChange(run(BloodlineState.NONE, KILLED_PREY, ctx()), BloodlineState.NONE);
    }

    @Test
    void markLostToPreyKill() {
        Outcome o = run(st(Species.HUNTER_OF_THE_FIVE).toBuilder().markKills(7).build(), KILLED_BY_PREY, ctx());
        assertEquals(Species.NONE, o.state().species());
        assertEquals(0, o.state().markKills());
    }

    @Test
    void naturalDeathKeepsMark() {
        BloodlineState s = st(Species.HUNTER_OF_THE_FIVE).toBuilder().markKills(7).build();
        assertNoChange(run(s, DIED, ctx()), s);
    }

    @Test
    void chosenAsPotential() {
        Outcome o = run(BloodlineState.NONE, CHOSEN_AS_POTENTIAL, ctx().withFaction(BaseFaction.HUNTER));
        assertEquals(Species.POTENTIAL, o.state().species());
        assertRefused(run(BloodlineState.NONE, CHOSEN_AS_POTENTIAL, ctx().withFaction(BaseFaction.VAMPIRE)), BloodlineState.NONE, "not_hunter");
    }

    @Test
    void preyKillingMarkedHunterIsCursed() {
        assertTrue(run(st(Species.ORIGINAL), KILLED_MARKED_HUNTER, ctx().withFaction(BaseFaction.VAMPIRE)).state().hunterCurse());
        assertTrue(run(st(Species.ORIGINAL), KILLED_MARKED_HUNTER, ctx()).state().hunterCurse());
        assertTrue(run(BloodlineState.NONE, KILLED_MARKED_HUNTER, ctx().withFaction(BaseFaction.WEREWOLF)).state().hunterCurse());
        assertTrue(run(BloodlineState.NONE, KILLED_MARKED_HUNTER, ctx().withFaction(BaseFaction.VAMPIRE)).state().hunterCurse());
        for (Species sp : List.of(Species.HYBRID, Species.ORIGINAL_HYBRID, Species.TRIBRID, Species.HERETIC)) {
            assertTrue(run(st(sp), KILLED_MARKED_HUNTER, ctx()).state().hunterCurse(), sp.name());
        }
        assertFalse(run(BloodlineState.NONE, KILLED_MARKED_HUNTER, ctx()).state().hunterCurse());
        assertFalse(run(st(Species.WITCH), KILLED_MARKED_HUNTER, ctx()).state().hunterCurse());
        assertFalse(run(st(Species.SIPHONER), KILLED_MARKED_HUNTER, ctx().withFaction(BaseFaction.WEREWOLF)).state().hunterCurse());
    }

    @Test
    void curseLifts() {
        BloodlineState s = BloodlineState.NONE.toBuilder().hunterCurse(true).build();
        assertFalse(run(s, HUNTER_CURSE_LIFTED, ctx()).state().hunterCurse());
    }

    // ---- Species change stamping ----

    @Test
    void sameSpeciesDoesNotRestampTurnedAt() {
        BloodlineState s = st(Species.HYBRID).toBuilder().turnedAt(10).hybridCurseBound(true).build();
        assertEquals(10, run(s, RITE_OF_THE_MOON, ctx().withGameTime(999)).state().turnedAt());
    }
}
