package net.ofatech.supernaturalbloodlines.core.evolution;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class TierTest {
    @Test
    void nightPerTier() {
        assertEquals(Tier.Night.ECLIPSE, Tier.ULTIMATE.night());
        assertEquals(Tier.Night.FULL_MOON, Tier.UNIQUE.night());
        assertEquals(Tier.Night.FULL_MOON, Tier.EXTRA.night());
        assertEquals(Tier.Night.FULL_MOON, Tier.COMMON.night());
    }

    @Test
    void tierOfEachSkillType() {
        assertEquals(Tier.COMMON, Tier.ofSkillType("RESISTANCE", true));
        assertEquals(Tier.COMMON, Tier.ofSkillType("INTRINSIC", true));
        assertEquals(Tier.COMMON, Tier.ofSkillType("COMMON", true));
        assertEquals(Tier.EXTRA, Tier.ofSkillType("EXTRA", true));
        assertEquals(Tier.UNIQUE, Tier.ofSkillType("UNIQUE", true));
        assertEquals(Tier.ULTIMATE, Tier.ofSkillType("ULTIMATE", true));
        assertEquals(Tier.EXTRA, Tier.ofSkillType("COMMON", false), "a magic or an art that is not a Tensura Skill counts as Extra");
    }

    @Test
    void parse() {
        assertEquals(Optional.of(Tier.UNIQUE), Tier.parse("unique"));
        assertEquals(Optional.empty(), Tier.parse("nonsense"));
    }
}
