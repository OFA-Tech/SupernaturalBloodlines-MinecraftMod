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
    void anAddonsBeastfolkBranchIsNeverTouchedAndNeverChosen() {
        // Ascension adds a kitsune branch to Tensura's Beastfolk; the line is followed by id, never "the first next evolution"
        for (String race : new String[] {"ascension:kitsune", "tensura:kitsune"}) {
            for (Rank r : Rank.values()) {
                assertEquals(Optional.empty(), target(race, r, true), race + " " + r);
                assertEquals(Optional.empty(), target(race, r, false), race + " " + r);
            }
        }
        assertEquals(Optional.of("tensura:beast_lord"), target("tensura:beastfolk", Rank.ALPHA, false));
    }

    @Test
    void noMainCodeTakesTheFirstNextEvolution() throws java.io.IOException {
        java.util.regex.Pattern first = java.util.regex.Pattern.compile("getNextEvolutions\\([^)]*\\)\\s*\\.(get\\(0\\)|getFirst\\(|stream\\(\\)\\s*\\.findFirst)");
        java.nio.file.Path base = java.nio.file.Path.of("").toAbsolutePath();
        while (!java.nio.file.Files.isDirectory(base.resolve("src/main/java"))) {
            base = base.getParent();
        }
        try (java.util.stream.Stream<java.nio.file.Path> files = java.nio.file.Files.walk(base.resolve("src/main/java"))) {
            for (java.nio.file.Path p : files.filter(f -> f.toString().endsWith(".java") && !f.toString().contains("gametest")).toList()) {
                org.junit.jupiter.api.Assertions.assertFalse(first.matcher(java.nio.file.Files.readString(p)).find(), p + " takes the first next evolution");
            }
        }
    }

    @Test
    void noRaceIsLeftAlone() {
        assertEquals(Optional.empty(), target(null, Rank.TRUE_ALPHA, true));
    }
}
