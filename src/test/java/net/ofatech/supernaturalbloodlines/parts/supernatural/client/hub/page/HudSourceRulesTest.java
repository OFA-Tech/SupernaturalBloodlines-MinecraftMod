package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import static net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.SourceRulesTest.body;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Rules about how the HUD's drawing code is wired, which no pure test sees: each reverts to a visible bug. */
class HudSourceRulesTest {
    private static String source(String file) throws IOException {
        Path at = Path.of("").toAbsolutePath();
        while (!Files.isDirectory(at.resolve("src/main/java"))) {
            at = at.getParent();
        }
        return Files.readString(at.resolve("src/main/java/net/ofatech/supernaturalbloodlines/" + file));
    }

    @Test
    void everyKitBlitTurnsBlendingOnAndSetsTheNormalBlendFunctionWhateverTheAlpha() throws IOException {
        String blend = body(source("shared/hub/kit/client/UiKit.java"), "private static void blend(");
        int gate = blend.indexOf("if (alpha < 1f)");
        assertTrue(gate > 0, "the tint is gated by the alpha");
        String before = blend.substring(0, gate);
        assertTrue(before.contains("enableBlend()") && before.contains("defaultBlendFunc()"), "but the blending is not: " + blend);
    }

    @Test
    void theHudIconsAreTheOutlinedOnesAndTheReadyGlowIsNotTheActiveFrame() throws IOException {
        String slot = source("parts/hud/client/SlotArt.java");
        assertTrue(body(slot, "private static void icon(").contains("KitSprites.HUD_ICON"));
        assertFalse(body(slot, "private static void readyGlow(").contains("framePart"), "the glow has its own look");
        assertFalse(body(slot, "static void draw(").contains("alpha * glow"), "and the active frame is not faded in as the glow");
    }

    @Test
    void theSecondsAndTheKeyAreFittedToTheLiftedBadgeAndToLongKeyNames() throws IOException {
        String slot = source("parts/hud/client/SlotArt.java");
        assertTrue(body(slot, "private static void seconds(").contains("SlotBadge.secondsScale("));
        assertTrue(body(slot, "private static void key(").contains("SlotBadge.shortKey("));
        assertTrue(body(slot, "static void draw(").contains("SlotBadge.placement("), "Blood Moon's plate leaves the icon alone in Tensura mode");
    }

    @Test
    void aPackThatResizesTheBloodRingStillGetsABar() throws IOException {
        String ring = body(source("parts/hud/client/SlotArt.java"), "private static void bloodRing(");
        assertTrue(ring.contains("FrameStrip.outlineSegments("));
        assertTrue(body(source("shared/hub/kit/client/UiKit.java"), "public static boolean frame(").contains("WARNED.add("), "and says so once");
    }

    @Test
    void theGlowIsTrackedByTheTrackerOnlyForFilledUsableSlotsAndStartsOverWhenTheHudIsHidden() throws IOException {
        String layer = source("parts/hud/client/AbilityHudLayer.java");
        assertTrue(body(layer, "private float[] glows(").contains("!v.isEmpty() && !v.greyed()"));
        assertTrue(body(layer, "public void render(").contains("glowTracker.reset()"));
    }

    @Test
    void theToastLooksForTheOtherModsOnceAndAnchorsWhereTheHudSays() throws IOException {
        String toast = body(source("parts/supernatural/client/RequestToast.java"), "private static void draw(");
        assertTrue(toast.contains("statusCorner == null"), "the mod list is asked once");
        assertTrue(toast.contains("HudPlacement.toastAnchor()") && toast.contains("HudPlacement.trackerBox()"));
    }

    @Test
    void aLabelThatDoesNotFitSendsItsColumnToThePanelAndTheModeMarkIsNamedOnce() throws IOException {
        String chrome = source("parts/hud/client/HudChrome.java");
        assertTrue(body(chrome, "static void labels(").contains("SlotLabel.fallbackColumns("));
        assertFalse(chrome.contains("\"kit/shared/mode_mark\"") || source("parts/hud/client/SlotArt.java").contains("\"kit/shared/mode_mark\""), "one constant: KitSprites.MODE_MARK");
    }
}
