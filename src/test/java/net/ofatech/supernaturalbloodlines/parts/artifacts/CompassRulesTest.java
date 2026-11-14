package net.ofatech.supernaturalbloodlines.parts.artifacts;

import java.util.List;
import net.minecraft.world.phys.Vec3;
import net.ofatech.supernaturalbloodlines.parts.artifacts.item.CompassMode;
import net.ofatech.supernaturalbloodlines.parts.artifacts.item.CompassRules;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CompassRulesTest {
    private static final Vec3 HOLDER = new Vec3(0, 64, 0);

    @Test
    void picksTheNearestVillagerInRange() {
        Vec3 near = new Vec3(10, 64, 0);
        Vec3 far = new Vec3(100, 64, 0);
        assertEquals(near, CompassRules.nearestDoppelganger(HOLDER, List.of(far, near), 512, List.of()).orElseThrow());
    }

    @Test
    void ignoresVillagersOutOfRangeButNotOnTheEdge() {
        assertTrue(CompassRules.nearestDoppelganger(HOLDER, List.of(new Vec3(513, 64, 0)), 512, List.of()).isEmpty());
        assertTrue(CompassRules.nearestDoppelganger(HOLDER, List.of(new Vec3(512, 64, 0)), 512, List.of()).isPresent());
    }

    @Test
    void aPlayerIsFoundAtAnyDistance() {
        Vec3 player = new Vec3(5000, 64, 0);
        assertEquals(player, CompassRules.nearestDoppelganger(HOLDER, List.of(), 512, List.of(player)).orElseThrow());
    }

    @Test
    void theNearerOfAVillagerAndAPlayerWins() {
        Vec3 villager = new Vec3(50, 64, 0);
        Vec3 player = new Vec3(20, 64, 0);
        assertEquals(player, CompassRules.nearestDoppelganger(HOLDER, List.of(villager), 512, List.of(player)).orElseThrow());
        assertEquals(villager, CompassRules.nearestDoppelganger(HOLDER, List.of(villager), 512, List.of(new Vec3(900, 64, 0))).orElseThrow());
    }

    @Test
    void nobodyMeansEmpty() {
        assertTrue(CompassRules.nearestDoppelganger(HOLDER, List.of(), 512, List.of()).isEmpty());
    }

    @Test
    void modeTogglesAndRoundTripsItsName() {
        assertEquals(CompassMode.DOPPELGANGERS, CompassMode.VAMPIRES.toggled());
        assertEquals(CompassMode.VAMPIRES, CompassMode.DOPPELGANGERS.toggled());
        for (CompassMode m : CompassMode.values()) {
            assertEquals(m, CompassMode.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,
                    CompassMode.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, m).getOrThrow()).getOrThrow());
        }
    }
}
