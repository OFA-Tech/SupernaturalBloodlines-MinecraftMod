package net.ofatech.supernaturalbloodlines.core.hud;

import static net.ofatech.supernaturalbloodlines.core.hud.BiteAction.HYBRID_FEED;
import static net.ofatech.supernaturalbloodlines.core.hud.BiteAction.NONE;
import static net.ofatech.supernaturalbloodlines.core.hud.BiteAction.TURNING_BITE;
import static net.ofatech.supernaturalbloodlines.core.hud.BiteAction.VAMPIRISM_DRINK;
import static net.ofatech.supernaturalbloodlines.core.hud.BiteAction.WEREWOLF_BITE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BiteRulesTest {
    private static BiteContext none() {
        return new BiteContext(false, false, false, false, false, false);
    }

    private static BiteContext werewolf(boolean wolfForm) {
        return new BiteContext(true, wolfForm, false, false, false, false);
    }

    private static BiteContext hybrid(boolean wolfForm) {
        return new BiteContext(true, wolfForm, false, true, false, false);
    }

    @Test
    void row1WerewolfInWolfFormBitesLikeAWerewolf() {
        assertEquals(WEREWOLF_BITE, BiteRules.decide(werewolf(true)));
    }

    @Test
    void row1TribridInWolfFormBitesLikeAWerewolf() {
        assertEquals(WEREWOLF_BITE, BiteRules.decide(hybrid(true)));
    }

    @Test
    void row2VampirismVampireDrinks() {
        assertEquals(VAMPIRISM_DRINK, BiteRules.decide(new BiteContext(false, false, true, false, false, false)));
    }

    @Test
    void row2TensuraVampireThatIsAlsoAVampirismVampireDrinks() {
        // Tensura's race is not in the context at all: only the Vampirism faction decides
        assertEquals(VAMPIRISM_DRINK, BiteRules.decide(new BiteContext(false, false, true, false, false, false)));
    }

    @Test
    void row3HybridOriginalHybridAndTribridInHumanFormFeed() {
        // the three types are one flag (HybridTraits.isHybrid): werewolf faction, human form
        assertEquals(HYBRID_FEED, BiteRules.decide(hybrid(false)));
        assertEquals(HYBRID_FEED, BiteRules.decide(new BiteContext(false, false, false, true, false, false)));
    }

    @Test
    void rowTHumanInTheTurningThatWouldCompleteBites() {
        assertEquals(TURNING_BITE, BiteRules.decide(new BiteContext(false, false, false, false, true, true)));
    }

    @Test
    void rowTNeedsBothTheTurningAndAWayToComplete() {
        assertEquals(NONE, BiteRules.decide(new BiteContext(false, false, false, false, true, false)));
        assertEquals(NONE, BiteRules.decide(new BiteContext(false, false, false, false, false, true)));
    }

    @Test
    void row4WerewolfInHumanFormWithNoVampireSideStillUsesTheWerewolfBite() {
        assertEquals(WEREWOLF_BITE, BiteRules.decide(werewolf(false)));
    }

    @Test
    void row5AnyoneElseGetsNothing() {
        assertEquals(NONE, BiteRules.decide(none()));
    }

    @Test
    void row5ATensuraVampireRaceWithNoVampirismFactionGetsNothing() {
        assertEquals(NONE, BiteRules.decide(none()));
    }

    @Test
    void row5AHumanNotInTheTurningGetsNothing() {
        assertEquals(NONE, BiteRules.decide(new BiteContext(false, false, false, false, false, false)));
    }

    @Test
    void tieWolfFormBeatsTheVampireSide() {
        assertEquals(WEREWOLF_BITE, BiteRules.decide(new BiteContext(true, true, true, true, true, true)));
    }

    @Test
    void tieVampirismBeatsHybridFeedAndTheTurning() {
        assertEquals(VAMPIRISM_DRINK, BiteRules.decide(new BiteContext(false, false, true, true, true, true)));
    }

    @Test
    void tieHybridFeedBeatsTheTurning() {
        assertEquals(HYBRID_FEED, BiteRules.decide(new BiteContext(true, false, false, true, true, true)));
    }

    @Test
    void tieTurningBeatsTheWerewolfBiteInHumanForm() {
        assertEquals(TURNING_BITE, BiteRules.decide(new BiteContext(true, false, false, false, true, true)));
    }
}
