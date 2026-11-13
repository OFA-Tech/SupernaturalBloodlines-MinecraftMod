package net.ofatech.supernaturalbloodlines.shared.werewolves;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class MoonControlTest {
    @Test
    void aBornWolfIsSparedAboutHalfTheFullMoons() {
        int spared = 0;
        int nights = 4000;
        UUID id = UUID.fromString("00000000-0000-0000-0000-00000000b0b0");
        for (long day = 0; day < nights; day++) {
            spared += MoonControl.bornSparedOn(id, day) ? 1 : 0;
        }
        assertTrue(spared > nights * 0.45 && spared < nights * 0.55, "spared " + spared + " of " + nights);
    }

    @Test
    void theAnswerIsTheSameAllNight() {
        UUID id = UUID.randomUUID();
        for (long day = 0; day < 50; day++) {
            assertEquals(MoonControl.bornSparedOn(id, day), MoonControl.bornSparedOn(id, day));
        }
    }

    @Test
    void differentWolvesAreNotSparedTheSameNights() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        int same = 0;
        for (long day = 0; day < 400; day++) {
            same += MoonControl.bornSparedOn(a, day) == MoonControl.bornSparedOn(b, day) ? 1 : 0;
        }
        assertTrue(same < 380, "the two wolves were spared on the same nights " + same + " of 400");
    }
}
