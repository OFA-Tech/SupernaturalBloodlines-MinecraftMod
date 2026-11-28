package net.ofatech.supernaturalbloodlines.parts.speciestensura;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RaceMapperConfigTest {
    @AfterEach
    void clearOverrides() {
        RaceMapperConfig.overrideBloodfiendToVampire(null);
        RaceMapperConfig.overrideDirewolfToWerewolf(null);
        RaceMapperConfig.overrideSpiritWolfToWerewolf(null);
    }

    @Test
    void defaultsAreOn() {
        assertTrue(RaceMapperConfig.bloodfiendToVampire());
        assertTrue(RaceMapperConfig.direwolfToWerewolf());
        assertTrue(RaceMapperConfig.spiritWolfToWerewolf());
    }

    @Test
    void overridesTakePrecedence() {
        RaceMapperConfig.overrideBloodfiendToVampire(false);
        assertFalse(RaceMapperConfig.bloodfiendToVampire());
        RaceMapperConfig.overrideBloodfiendToVampire(true);
        assertTrue(RaceMapperConfig.bloodfiendToVampire());
    }

    @Test
    void bloodfiendLevelFloorsHaveFiveDefaults() {
        assertEquals(5, RaceMapperConfig.bloodfiendLevelFloors().size());
    }
}