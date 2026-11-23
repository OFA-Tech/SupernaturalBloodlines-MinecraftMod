package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DevShotsSpecTest {
    @Test
    void aWholeBossFlagIsRead() {
        assertEquals(new DevShotsSpec.Boss("grimoire", "klaus_shifting"), DevShotsSpec.boss("boss:grimoire:klaus_shifting".split(":")));
        assertEquals(new DevShotsSpec.Boss("blood_moon", "stack3"), DevShotsSpec.boss("boss:Blood_Moon:STACK3".split(":")), "case does not matter");
    }

    @Test
    void aFlagWithoutABarOrWithTooMuchIsABadFlagThatSaysWhatIsWanted() {
        for (String flag : new String[] {"boss:grimoire", "boss", "boss:grimoire:", "boss::klaus_human", "boss:grimoire:klaus_human:extra"}) {
            IllegalArgumentException bad = assertThrows(IllegalArgumentException.class, () -> DevShotsSpec.boss(flag.split(":")), flag);
            assertTrue(bad.getMessage().contains("boss:<look>:<bar>"), "says what is wanted: " + bad.getMessage());
        }
    }

    @Test
    void aLookThatDoesNotExistIsABadFlag() {
        IllegalArgumentException bad = assertThrows(IllegalArgumentException.class, () -> DevShotsSpec.boss("boss:cartoon:mikael".split(":")));
        assertTrue(bad.getMessage().contains("cartoon"));
    }
}
