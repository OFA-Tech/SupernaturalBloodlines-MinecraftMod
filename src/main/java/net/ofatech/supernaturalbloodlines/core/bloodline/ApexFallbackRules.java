package net.ofatech.supernaturalbloodlines.core.bloodline;

import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank;
import org.jetbrains.annotations.Nullable;

/**
 * Owner rule: an {@code alpha_primeval} who loses their Alpha spark (stolen, given away, cured), or a True Alpha who goes dormant, goes back to the race they
 * had before the apex one; the race and its skills return when the spark does. Pure: the game side ({@code ApexFallback}) reads the facts and sets the race.
 */
public final class ApexFallbackRules {
    public static final String APEX = "supernaturalbloodlines:alpha_primeval";
    /** Where an apex wolf goes back to when nothing was recorded (one from before the record existed): the Beastfolk line's top, which offers the apex. */
    public static final String DEFAULT_BEFORE = "tensura:spirit_beast";

    public enum Action { NONE, REVERT, RESTORE }

    private ApexFallbackRules() {}

    /** Whether the player holds a spark now: an Alpha or True Alpha who is a werewolf (a True Alpha outside the faction is dormant). */
    public static boolean holds(Rank rank, boolean werewolf) {
        return werewolf && (rank == Rank.ALPHA || rank == Rank.TRUE_ALPHA);
    }

    /**
     * What a change of "holds a spark" does. Only a change counts: an apex wolf who never had a spark (a Beta on the apex race) is left alone, and
     * so is one whose race is not the apex (or not the one we went back to).
     */
    public static Action decide(boolean heldBefore, boolean holdsNow, @Nullable String currentRace, String preApex) {
        if (heldBefore && !holdsNow && APEX.equals(currentRace)) {
            return Action.REVERT;
        }
        if (!heldBefore && holdsNow && !preApex.isEmpty() && preApex.equals(currentRace)) {
            return Action.RESTORE;
        }
        return Action.NONE;
    }

    /** The race to go back to: the recorded one, else {@link #DEFAULT_BEFORE}. */
    public static String target(String preApex) {
        return preApex.isEmpty() ? DEFAULT_BEFORE : preApex;
    }

    /**
     * What to remember when the player's race changes by their own doing: entering the apex records the race left; any other change forgets it
     * (a player who evolved on from the race we sent them back to no longer comes back).
     */
    public static String recorded(@Nullable String oldRace, @Nullable String newRace, String preApex) {
        if (APEX.equals(newRace)) {
            return oldRace == null || oldRace.isEmpty() || APEX.equals(oldRace) ? preApex : oldRace;
        }
        return "";
    }
}
