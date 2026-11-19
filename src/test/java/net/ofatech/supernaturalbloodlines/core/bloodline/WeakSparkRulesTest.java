package net.ofatech.supernaturalbloodlines.core.bloodline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import net.ofatech.supernaturalbloodlines.core.bloodline.WeakSparkRules.Grant;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Lineage;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Weak;
import org.junit.jupiter.api.Test;

/** R145, row by row: the weaker spark of an NPC Alpha. */
class WeakSparkRulesTest {
    private static final int GROW = 3 * 24000;

    private static WolfState wolf(Rank rank, boolean protect) {
        return WolfState.NONE.toBuilder().lineage(Lineage.BITTEN).rank(rank).protectPath(protect).build();
    }

    /** A Beta whose path is closed (a spark held once) and who may take one. */
    private static WolfState free() {
        return wolf(Rank.BETA, false);
    }

    @Test
    void anUnprotectedWerewolfTakesIt() {
        assertEquals(Grant.GRANTED, WeakSparkRules.grant(free(), 10, 2, 3));
        assertEquals(Grant.GRANTED, WeakSparkRules.grant(wolf(Rank.ALPHA, true), 10, 2, 3), "an Alpha's path is closed already");
        assertEquals(Grant.GRANTED, WeakSparkRules.grant(wolf(Rank.OMEGA, false), 10, 2, 3));
    }

    @Test
    void aTrueAlphaNeverTakesOne() {
        assertEquals(Grant.NEVER, WeakSparkRules.grant(wolf(Rank.TRUE_ALPHA, false), 10, 2, 3));
    }

    @Test
    void aNonWerewolfGetsNothing() {
        assertEquals(Grant.NEVER, WeakSparkRules.grant(WolfState.NONE, 10, 2, 3));
    }

    @Test
    void anOpenProtectedPathDestroysIt() {
        assertEquals(Grant.DESTROYED, WeakSparkRules.grant(wolf(Rank.BETA, true), 10, 2, 3));
        assertEquals(Grant.DESTROYED, WeakSparkRules.grant(wolf(Rank.OMEGA, true), 10, 2, 3));
    }

    @Test
    void aClosedPathIsNotProtected() {
        WolfState closed = wolf(Rank.BETA, true).toBuilder().heldSpark(true).build();
        assertEquals(Grant.GRANTED, WeakSparkRules.grant(closed, 10, 2, 3));
        WolfState blue = wolf(Rank.BETA, true).toBuilder().killedInnocent(true).build();
        assertEquals(Grant.GRANTED, WeakSparkRules.grant(blue, 10, 2, 3), "blue eyes close it too");
    }

    @Test
    void theCooldownCountsGameDays() {
        WolfState w = free().toBuilder().weak(new Weak(1, 0, 11)).build(); // given on day 10 (the day is kept as day + 1)
        assertEquals(Grant.COOLDOWN, WeakSparkRules.grant(w, 10, 5, 3), "the same day");
        assertEquals(Grant.COOLDOWN, WeakSparkRules.grant(w, 12, 5, 3), "two days later");
        assertEquals(Grant.GRANTED, WeakSparkRules.grant(w, 13, 5, 3), "three days later");
        assertEquals(Grant.GRANTED, WeakSparkRules.grant(w, 10, 5, 0), "a cooldown of 0 is none");
    }

    @Test
    void aSparkGivenOnDayZeroStillStartsTheCooldown() {
        Weak w = WeakSparkRules.given(Weak.NONE, 0, GROW);
        WolfState holder = free().toBuilder().weak(w).build();
        assertEquals(Grant.COOLDOWN, WeakSparkRules.grant(holder, 0, 5, 3));
        assertEquals(Grant.COOLDOWN, WeakSparkRules.grant(holder, 2, 5, 3));
        assertEquals(Grant.GRANTED, WeakSparkRules.grant(holder, 3, 5, 3));
        assertEquals(Grant.GRANTED, WeakSparkRules.grant(holder, 0, 5, 0), "a cooldown of 0 is none, even on the same day");
    }

    @Test
    void noCooldownBeforeTheFirstOne() {
        assertEquals(Grant.GRANTED, WeakSparkRules.grant(free(), 0, 2, 3), "day 0 with nothing given yet");
    }

    @Test
    void theMostHeldIsRespected() {
        WolfState two = free().toBuilder().weak(new Weak(2, 5, 1)).build();
        assertEquals(Grant.FULL, WeakSparkRules.grant(two, 100, 2, 3));
        assertEquals(Grant.GRANTED, WeakSparkRules.grant(two, 100, 3, 3));
    }

    @Test
    void givingOneKeepsTheOneAlreadyGrowing() {
        Weak w = WeakSparkRules.given(new Weak(1, 5000, 3, 70_000), 9, GROW);
        assertEquals(new Weak(2, 5000, 10, 70_000), w, "the day is kept as day + 1, so that 0 means never; the time of the one growing stays");
        assertEquals(new Weak(1, 0, 10, GROW), WeakSparkRules.given(Weak.NONE, 9, GROW));
        assertEquals(new Weak(1, 0, 1, GROW), WeakSparkRules.given(Weak.NONE, 0, GROW), "given on day 0 is not never");
    }

    @Test
    void growthAddsTicksAndFinishesAtTheLimit() {
        assertEquals(new WeakSparkRules.Grown(new Weak(1, 200, 4), false), WeakSparkRules.grow(new Weak(1, 0, 4), 200, GROW));
        assertEquals(new WeakSparkRules.Grown(new Weak(1, GROW - 1, 4), false), WeakSparkRules.grow(new Weak(1, GROW - 200, 4), 199, GROW));
        assertEquals(new WeakSparkRules.Grown(new Weak(0, 0, 4), true), WeakSparkRules.grow(new Weak(1, GROW - 200, 4), 200, GROW));
    }

    @Test
    void theNextSparkStartsFromNothing() {
        assertEquals(new WeakSparkRules.Grown(new Weak(1, 0, 4, GROW), true), WeakSparkRules.grow(new Weak(2, GROW - 1, 4), 5, GROW));
    }

    @Test
    void theGrowthTimeIsFixedWhenTheSparkStartsAndTheSettingIsNotGuessed() {
        Weak w = WeakSparkRules.given(Weak.NONE, 5, 2 * 24000);
        assertEquals(2 * 24000, w.need());
        // the setting went up to 5 days afterwards: this one still takes its two
        assertEquals(new WeakSparkRules.Grown(new Weak(0, 0, 6), true), WeakSparkRules.grow(new Weak(1, 2 * 24000 - 10, 6, 2 * 24000), 10, 5 * 24000));
        assertEquals(new WeakSparkRules.Grown(new Weak(1, 2 * 24000 - 11, 6, 2 * 24000), false), WeakSparkRules.grow(new Weak(1, 2 * 24000 - 12, 6, 2 * 24000), 1, 5 * 24000));
        // the next one starts with the setting of that moment
        assertEquals(5 * 24000, WeakSparkRules.grow(new Weak(2, 2 * 24000 - 1, 6, 2 * 24000), 1, 5 * 24000).weak().need());
    }

    @Test
    void theHubDaysComeFromTheSparksOwnTimeNotAGuess() {
        assertEquals(7, WeakSparkRules.daysLeft(new Weak(1, 0, 1, 7 * 24000), 3 * 24000), "a 7 day spark says 7, whatever the fallback");
        assertEquals(1, WeakSparkRules.daysLeft(new Weak(1, 0, 1, 24000), 3 * 24000));
        assertEquals(3, WeakSparkRules.daysLeft(new Weak(1, 0, 1), 3 * 24000), "no time on it: the fallback");
        assertEquals(6, WeakSparkRules.daysLeft(new Weak(1, 24000, 1, 7 * 24000), 3 * 24000));
    }

    @Test
    void nothingGrowsWithoutASpark() {
        assertEquals(new WeakSparkRules.Grown(Weak.NONE, false), WeakSparkRules.grow(Weak.NONE, 99999, GROW));
    }

    @Test
    void aGrownSparkFollowsTheRulesOfASteal() {
        assertEquals(PackRules.Steal.TAKES, WeakSparkRules.onGrown(free()));
        assertEquals(PackRules.Steal.PROTECTED, WeakSparkRules.onGrown(wolf(Rank.BETA, true)));
        assertEquals(PackRules.Steal.NEVER, WeakSparkRules.onGrown(wolf(Rank.TRUE_ALPHA, true)));
    }

    @Test
    void lostKeepsOnlyTheDay() {
        assertEquals(new Weak(0, 0, 7), WeakSparkRules.lost(new Weak(2, 999, 7, 5000)));
    }

    @Test
    void daysLeftRoundsUpAndNeverShowsZeroWhileOneGrows() {
        assertEquals(0, WeakSparkRules.daysLeft(Weak.NONE, GROW));
        assertEquals(3, WeakSparkRules.daysLeft(new Weak(1, 0, 1), GROW));
        assertEquals(1, WeakSparkRules.daysLeft(new Weak(1, GROW - 1, 1), GROW));
        assertEquals(2, WeakSparkRules.daysLeft(new Weak(1, 24000 + 1, 1), GROW));
    }

    @Test
    void theDayOfAGameTimeIsTheWholeDay() {
        assertEquals(0, WeakSparkRules.day(23999));
        assertEquals(1, WeakSparkRules.day(24000));
        assertEquals(-1, WeakSparkRules.day(-1));
    }

    // ---- what it does to the rest of the wolf layer ----

    @Test
    void itIsNotASparkAndDoesNotStack() {
        WolfState w = wolf(Rank.ALPHA, false).toBuilder().sparks(2).weak(new Weak(2, 0, 1)).build();
        assertEquals(2, PackRules.effectiveSparks(w.rank(), w.sparks(), 3, false), "the weak ones are not in the count");
        assertEquals(2, PackRules.transferableSparks(w.sparks(), false), "a thief takes the real sparks only");
        assertEquals(2, w.weakSparks());
    }

    @Test
    void anAlphaWhoLosesTheSparkLosesTheWeakOnes() {
        WolfState w = wolf(Rank.ALPHA, false).toBuilder().weak(new Weak(2, 100, 5)).build();
        WolfState after = PackRules.apply(w, PackEvent.SPARK_LOST, PackContext.defaults()).state();
        assertEquals(new Weak(0, 0, 5), after.weak());
    }

    @Test
    void aTrueAlphaEarnedHoldsNone() {
        long day30 = 30L * 24000;
        WolfState w = wolf(Rank.BETA, false).toBuilder().heldSpark(false).packAlpha(java.util.UUID.randomUUID()).betaSince(1).defenderKills(300).weak(new Weak(1, 50, 8)).build();
        PackContext c = new PackContext(java.util.UUID.randomUUID(), 10, 1 + day30, 10, day30, 300, true);
        PackOutcome o = PackRules.apply(w, PackEvent.TRUE_ALPHA_EARNED, c);
        assertFalse(o.refused(), o.refusalKey());
        assertEquals(Rank.TRUE_ALPHA, o.state().rank());
        assertEquals(new Weak(0, 0, 8), o.state().weak());
    }

    @Test
    void aCureLosesThemAndKeepsTheCooldownDay() {
        BloodlineState s = BloodlineState.NONE.toBuilder().wolf(wolf(Rank.BETA, false).toBuilder().weak(new Weak(2, 77, 6)).build()).build();
        BloodlineState cured = BloodlineRules.apply(s, BloodlineEvent.DRANK_THE_CURE, BloodlineContext.defaults()).state();
        assertEquals(new Weak(0, 0, 6), cured.wolf().weak());
    }

    @Test
    void anOldSaveWithoutTheFieldLoadsWithNone() {
        var codec = WolfState.CODEC;
        var json = codec.encodeStart(JsonOps.INSTANCE, WolfState.NONE).getOrThrow().getAsJsonObject();
        json.remove("weak");
        assertEquals(Weak.NONE, codec.parse(JsonOps.INSTANCE, json).getOrThrow().weak());
    }

    @Test
    void theWeakSparksSurviveTheCodec() {
        WolfState w = wolf(Rank.BETA, false).toBuilder().weak(new Weak(2, 1234, 9, 48000)).build();
        var json = WolfState.CODEC.encodeStart(JsonOps.INSTANCE, w).getOrThrow();
        assertEquals(new Weak(2, 1234, 9, 48000), WolfState.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow().weak());
        assertTrue(json.getAsJsonObject().has("weak"));
    }

    @Test
    void aNegativeOrOrphanedProgressIsNormalised() {
        assertEquals(new Weak(0, 0, 3), new Weak(-1, 500, 3, 99));
        assertEquals(new Weak(1, 0, 3), new Weak(1, -5, 3, -9));
    }
}