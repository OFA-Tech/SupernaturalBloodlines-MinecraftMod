package net.ofatech.supernaturalbloodlines.core.boss;

import net.ofatech.supernaturalbloodlines.core.bloodline.PackRules;

/**
 * R171: what the killer of Deucalion gets. His sparks go through the same ladder as a stolen spark ({@link PackRules#stealBy}): a killer who is no werewolf
 * gets nothing (the spark goes nowhere and the Roar needs the faction); a True Alpha takes nothing; a killer whose path to True Alpha is open and
 * protected has the sparks destroyed; anybody else takes them, up to {@code max_stacked_sparks}. The Demon Wolf Roar is for a werewolf.
 */
public final class DeucalionRewardRules {
    public enum Outcome { NOT_A_WEREWOLF, NEVER, PROTECTED, GIVEN }

    /** @param sparksAfter the killer's stacked sparks after it, unchanged unless {@link Outcome#GIVEN} */
    public record Grant(Outcome outcome, int sparksAfter, boolean capped) {
    }

    private DeucalionRewardRules() {
    }

    public static Grant sparks(boolean werewolf, PackRules.Steal verdict, boolean killerIsAlpha, int killerSparks, int bossSparks, int cap) {
        if (!werewolf) {
            return new Grant(Outcome.NOT_A_WEREWOLF, killerSparks, false);
        }
        if (bossSparks <= 0 || verdict == PackRules.Steal.NEVER) {
            return new Grant(Outcome.NEVER, killerSparks, false);
        }
        if (verdict == PackRules.Steal.PROTECTED) {
            return new Grant(Outcome.PROTECTED, killerSparks, false);
        }
        int held = killerIsAlpha ? Math.max(1, killerSparks) : 0;
        return new Grant(Outcome.GIVEN, PackRules.sparksAfterSteal(killerSparks, killerIsAlpha, bossSparks, cap), held + bossSparks > cap);
    }

    public static boolean givesRoar(boolean werewolf) {
        return werewolf;
    }
}
