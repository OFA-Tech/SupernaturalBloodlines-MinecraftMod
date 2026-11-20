package net.ofatech.supernaturalbloodlines.core.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.ofatech.supernaturalbloodlines.core.boss.DefeatRules.Kind;
import net.ofatech.supernaturalbloodlines.core.boss.DefeatRules.Verdict;
import org.junit.jupiter.api.Test;

class DefeatRulesTest {
    @Test
    void aBlowThatDoesNotKillJustHurts() {
        assertEquals(Verdict.HURT, DefeatRules.verdict(Kind.OTHER, false, false, true));
        assertEquals(Verdict.HURT, DefeatRules.verdict(Kind.WHITE_OAK, false, true, true));
    }

    @Test
    void aLethalBlowDesiccatesAClosedOriginal() {
        assertEquals(Verdict.DESICCATE, DefeatRules.verdict(Kind.OTHER, true, false, true));
    }

    @Test
    void anOpenOriginalCannotBeFinishedByAnythingButWhiteOak() {
        assertEquals(Verdict.SPARE, DefeatRules.verdict(Kind.OTHER, true, true, true));
    }

    @Test
    void whiteOakKillsAnOpenOriginal() {
        assertEquals(Verdict.DIE, DefeatRules.verdict(Kind.WHITE_OAK, true, true, true));
    }

    @Test
    void whiteOakOnAClosedOriginalOnlyHurtsWhileTheStakeNeedsOpen() {
        assertEquals(Verdict.SPARE, DefeatRules.verdict(Kind.WHITE_OAK, true, false, true));
    }

    @Test
    void whiteOakKillsAClosedOriginalWhenTheStakeNeedsNoOpening() {
        assertEquals(Verdict.DIE, DefeatRules.verdict(Kind.WHITE_OAK, true, false, false));
    }

    @Test
    void theAshDaggerDesiccatesAClosedOriginalWhateverTheDamage() {
        assertEquals(Verdict.DESICCATE, DefeatRules.verdict(Kind.ASH_DAGGER, false, false, true));
        assertEquals(Verdict.DESICCATE, DefeatRules.verdict(Kind.ASH_DAGGER, true, false, true));
    }

    @Test
    void theAshDaggerNeverKillsAnOpenOriginal() {
        assertEquals(Verdict.SPARE, DefeatRules.verdict(Kind.ASH_DAGGER, true, true, false));
    }
}
