package net.ofatech.supernaturalbloodlines.parts.species.pack;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.api.Test;

class PerkSuppressorsTest {
    @Test
    void anySourceSayingNotNowSuppressesAndNoneDoesNot() {
        Predicate<ServerPlayer> no = p -> false;
        Predicate<ServerPlayer> yes = p -> true;
        assertFalse(PerkSuppressors.anyOf(List.of(), null));
        assertFalse(PerkSuppressors.anyOf(List.of(no, no), null));
        assertTrue(PerkSuppressors.anyOf(List.of(no, yes), null));
    }

    @Test
    void theRankPerksAndThePackBonusReadTheSameList() throws Exception {
        // a source scan: neither class may keep a list of its own again
        java.nio.file.Path base = java.nio.file.Path.of("").toAbsolutePath();
        while (!java.nio.file.Files.isDirectory(base.resolve("src/main/java"))) {
            base = base.getParent();
        }
        java.nio.file.Path pack = base.resolve("src/main/java/net/ofatech/supernaturalbloodlines/parts/species/pack");
        for (String name : List.of("RankEffects.java", "PackBonus.java")) {
            String src = java.nio.file.Files.readString(pack.resolve(name));
            assertTrue(src.contains("PerkSuppressors.any("), name + " does not ask the shared list");
            assertFalse(src.contains("SUPPRESSORS"), name + " has a list of its own");
        }
    }
}

