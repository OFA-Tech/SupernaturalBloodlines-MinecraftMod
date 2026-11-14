package net.ofatech.supernaturalbloodlines.parts.vampirismtensura;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank;
import net.ofatech.supernaturalbloodlines.parts.vampirismtensura.werewolves.WerewolfRaceSync;
import org.junit.jupiter.api.Test;

class WerewolfRaceSyncTest {
    private static Optional<String> target(String race, Rank rank, boolean joining) {
        return WerewolfRaceSync.targetRace(race, rank, joining);
    }

    @Test
    void humanJoiningBecomesBeastfolk() {
        assertEquals(Optional.of("tensura:beastfolk"), target("tensura:human", Rank.NONE, true));
        assertEquals(Optional.of("tensura:beastfolk"), target("tensura:human", Rank.OMEGA, true));
    }

    @Test
    void humanNotJoiningIsLeftAlone() {
        assertEquals(Optional.empty(), target("tensura:human", Rank.ALPHA, false));
    }

    @Test
    void alphaOnBeastfolkBecomesBeastLord() {
        assertEquals(Optional.of("tensura:beast_lord"), target("tensura:beastfolk", Rank.ALPHA, false));
    }

    @Test
    void trueAlphaOnTheLineBecomesSpiritBeast() {
        assertEquals(Optional.of("tensura:spirit_beast"), target("tensura:beastfolk", Rank.TRUE_ALPHA, false));
        assertEquals(Optional.of("tensura:spirit_beast"), target("tensura:beast_lord", Rank.TRUE_ALPHA, false));
    }

    @Test
    void theRaceIsNeverLowered() {
        assertEquals(Optional.empty(), target("tensura:spirit_beast", Rank.ALPHA, false));
        assertEquals(Optional.empty(), target("tensura:spirit_beast", Rank.TRUE_ALPHA, false));
        assertEquals(Optional.empty(), target("tensura:beast_lord", Rank.ALPHA, false));
        assertEquals(Optional.empty(), target("tensura:beast_lord", Rank.BETA, false));
    }

    @Test
    void divineBeastStaysATensuraEvolution() {
        for (Rank r : Rank.values()) {
            assertEquals(Optional.empty(), target("tensura:divine_beast", r, true));
            assertEquals(Optional.empty(), target("tensura:divine_beast", r, false));
        }
    }

    @Test
    void otherRacesAreNeverTouched() {
        for (String race : new String[] {"tensura:elf", "tensura:vampire", "tensura:vampire_lord", "tensura:ghoul", "tensura:slime"}) {
            for (Rank r : Rank.values()) {
                assertEquals(Optional.empty(), target(race, r, true), race + " " + r + " joining");
                assertEquals(Optional.empty(), target(race, r, false), race + " " + r);
            }
        }
    }

    @Test
    void ranksBelowAlphaDoNotMoveTheLine() {
        for (Rank r : new Rank[] {Rank.NONE, Rank.OMEGA, Rank.BETA}) {
            assertEquals(Optional.empty(), target("tensura:beastfolk", r, false));
            assertEquals(Optional.empty(), target("tensura:beastfolk", r, true));
        }
    }

    @Test
    void aRejoiningTrueAlphaOnTheLineGoesStraightToItsRank() {
        assertEquals(Optional.of("tensura:spirit_beast"), target("tensura:beastfolk", Rank.TRUE_ALPHA, true));
        assertEquals(Optional.of("tensura:beast_lord"), target("tensura:beastfolk", Rank.ALPHA, true));
    }

    @Test
    void noRaceIsLeftAlone() {
        assertEquals(Optional.empty(), target(null, Rank.TRUE_ALPHA, true));
    }
}
