package net.ofatech.supernaturalbloodlines.parts.bosses.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.imageio.ImageIO;
import net.ofatech.supernaturalbloodlines.core.boss.BossBarForms;
import net.ofatech.supernaturalbloodlines.core.boss.BossBarForms.Look;
import net.ofatech.supernaturalbloodlines.core.boss.BossForm;
import net.ofatech.supernaturalbloodlines.core.boss.BossId;
import net.ofatech.supernaturalbloodlines.parts.bosses.client.BossPortraits.Portrait;
import org.junit.jupiter.api.Test;

class BossPortraitsTest {
    private static final List<String> THE_FIFTEEN = List.of("beast_beast", "beast_host", "beast_spirit", "deucalion_demon_wolf", "deucalion_human", "elijah", "finn",
            "klaus_human", "klaus_shifting", "klaus_wolf", "kol", "mikael", "peter_human", "peter_wolf", "rebekah");

    private static Portrait of(BossId boss, int active) {
        return BossPortraits.of(boss, BossBarForms.checked(boss, BossBarForms.setOf(boss).ordinal(), active));
    }

    @Test
    void klausHasAPortraitForEachOfHisThreeForms() {
        assertEquals("klaus_human", of(BossId.KLAUS, 0).main());
        assertEquals("klaus_shifting", of(BossId.KLAUS, 1).main());
        assertEquals("klaus_wolf", of(BossId.KLAUS, 2).main());
        for (int i = 0; i < 3; i++) {
            assertFalse(of(BossId.KLAUS, i).seam(), "Klaus's shift has a portrait of its own");
        }
    }

    @Test
    void deucalionAndPeterShowAHumanAndAWolfAndASeamBetweenWhileTheyShift() {
        assertEquals("deucalion_human", of(BossId.DEUCALION, 0).main());
        assertEquals("deucalion_demon_wolf", of(BossId.DEUCALION, 2).main());
        Portrait shifting = of(BossId.DEUCALION, 1);
        assertEquals("deucalion_human", shifting.main());
        assertEquals("deucalion_demon_wolf", shifting.wolf());
        assertTrue(shifting.seam());
        assertEquals("peter_human", of(BossId.PETER_HALE, 0).main());
        assertEquals("peter_wolf", of(BossId.PETER_HALE, 2).main());
        assertEquals("peter_human", of(BossId.PETER_HALE, 1).main());
        assertEquals("peter_wolf", of(BossId.PETER_HALE, 1).wolf());
        assertNull(of(BossId.PETER_HALE, 0).wolf());
        assertNull(of(BossId.PETER_HALE, 2).wolf());
    }

    @Test
    void theBeastHasOnePerStageAndTheOthersOneEach() {
        assertEquals("beast_host", of(BossId.BEAST_OF_GEVAUDAN, 0).main());
        assertEquals("beast_beast", of(BossId.BEAST_OF_GEVAUDAN, 1).main());
        assertEquals("beast_spirit", of(BossId.BEAST_OF_GEVAUDAN, 2).main());
        assertEquals("mikael", of(BossId.MIKAEL, 0).main());
        assertEquals("elijah", of(BossId.ELIJAH, 0).main());
        assertEquals("kol", of(BossId.KOL, 0).main());
        assertEquals("rebekah", of(BossId.REBEKAH, 0).main());
        assertEquals("finn", of(BossId.FINN, 0).main());
        assertEquals("mikael", BossPortraits.of(BossId.MIKAEL, Look.NONE).main(), "a boss without forms ignores the form");
    }

    @Test
    void allFifteenPortraitsAreReachableAndEachIsA24By24Picture() throws Exception {
        Set<String> reached = new TreeSet<>();
        for (BossId boss : BossId.values()) {
            for (int active = 0; active < 3; active++) {
                Portrait p = of(boss, active);
                reached.add(p.main());
                if (p.wolf() != null) {
                    reached.add(p.wolf());
                }
            }
        }
        assertEquals(new TreeSet<>(THE_FIFTEEN), reached, "a portrait that no bar can show, or a bar with no portrait");
        for (String name : THE_FIFTEEN) {
            try (InputStream in = BossPortraitsTest.class.getResourceAsStream("/assets/supernaturalbloodlines/textures/gui/boss/" + name + ".png")) {
                assertNotNull(in, name + ".png is missing");
                BufferedImage img = ImageIO.read(in);
                assertEquals(24, img.getWidth(), name);
                assertEquals(24, img.getHeight(), name);
            }
        }
    }

    @Test
    void theSeamRunsDownTheMiddleOfThePortrait() {
        assertEquals(12, BossPortraits.SEAM_X);
    }
}
